package com.pico.welcomespace.ui.shop

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.math.Vector3
import com.pico.spatial.tracking.hmd.HMDTrackingProvider
import com.pico.spatial.ui.foundation.content.SpatialView
import com.pico.spatial.ui.foundation.gesture.TargetEntity
import com.pico.spatial.ui.foundation.gesture.detectSpatialDragGesture
import com.pico.spatial.ui.platform.LocalPhysicalLengthConverter
import com.pico.spatial.ui.platform.LengthUnit
import com.pico.welcomespace.domain.model.FloorPlanRoom
import com.pico.welcomespace.domain.model.RoomSurfaceKind
import com.pico.welcomespace.domain.model.VirtualRoomPlan
import com.pico.welcomespace.domain.model.ShopCategory
import com.pico.welcomespace.domain.model.ShopProduct
import com.pico.welcomespace.domain.model.PhysicalSize
import com.pico.welcomespace.ui.planner.RoomPlannerSession
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

internal sealed interface PlacementGesture {
    data class Start(val product: ShopProduct, val position: Vector3) : PlacementGesture
    data class Move(val position: Vector3) : PlacementGesture
    data class Drop(val position: Vector3) : PlacementGesture
    data object Cancel : PlacementGesture
}

internal object RoomPlacement {
    val gestures = Channel<PlacementGesture>(Channel.UNLIMITED)
    var editMode by mutableStateOf(FurnitureEditMode.MOVE)
    var active by mutableStateOf(false)
        private set
    var message by mutableStateOf("正在打开虚拟样板间…")
        private set

    fun updateAvailability(value: Boolean) {
        active = value
        if (!value) {
            while (gestures.tryReceive().isSuccess) {
                // 清除关闭场景前未消费的输入，避免重开后出现旧预览。
            }
        }
    }

    fun report(value: String) {
        message = value
    }

    fun start(product: ShopProduct, x: Float, y: Float, z: Float): Boolean {
        if (!active) {
            report("虚拟样板间尚未就绪，不能放置模型")
            return false
        }
        gestures.trySend(PlacementGesture.Start(product, Vector3(x, y, z)))
        report("正在拖入${product.name}，跟随射线吸附合法平面，松手放置")
        return true
    }

    fun move(x: Float, y: Float, z: Float) {
        if (active) gestures.trySend(PlacementGesture.Move(Vector3(x, y, z)))
    }

    fun drop(x: Float, y: Float, z: Float) {
        if (active) gestures.trySend(PlacementGesture.Drop(Vector3(x, y, z)))
    }

    fun cancel() {
        if (active) gestures.trySend(PlacementGesture.Cancel)
    }
}

@Composable
fun RoomStage() {
    val root = remember { Entity() }
    val furniture = remember(root) { FurnitureScene(root) }
    val context = LocalContext.current
    val converter = LocalPhysicalLengthConverter.current
    var attached by remember { mutableStateOf(false) }
    // 拖拽桥接跟随态：Start/Move 事件持续更新最新射线命中点，逐帧贴到预览实体。
    var following by remember { mutableStateOf(false) }
    var followPoint by remember { mutableStateOf<Vector3?>(null) }
    // 背面剔除：房间表面实体引用 + 头部追踪，HMD 退出某面墙/天花板后隐藏它。
    val roomSurfaces = remember { mutableMapOf<String, Entity>() }
    var roomReady by remember { mutableStateOf(false) }
    val hmdProvider = remember { HMDTrackingProvider() }

    SpatialView(
        modifier = Modifier.size(1.dp).pointerInput(root) {
            var target: Entity? = null
            detectSpatialDragGesture(
                context = context,
                targetedToEntity = TargetEntity.any { furniture.accepts(it) },
                onDragStart = { _, value -> target = value.targetEntity },
                onDragEnd = { target = null },
                onDragCancel = { target = null }
            ) { value ->
                // SDK 手势增量为视图像素；Stage 根无旋转，转换到米制右手坐标。
                val dx = with(density) {
                    converter.dpToLength(value.dragAmount.x.toDp(), LengthUnit.Meters)
                }
                val dz = with(density) {
                    converter.dpToLength(value.dragAmount.z.toDp(), LengthUnit.Meters)
                }
                furniture.edit(target ?: value.targetEntity, Vector3(dx, 0f, dz), RoomPlacement.editMode)
            }
        },
        initial = { content, _ ->
            content.addEntity(root)
            attached = true
        }
    )

    DisposableEffect(Unit) {
        hmdProvider.start()
        onDispose {
            hmdProvider.stop()
            RoomPlacement.updateAvailability(false)
            furniture.close()
            root.destroy()
        }
    }

    LaunchedEffect(attached) {
        if (!attached) return@LaunchedEffect
        root.components[TransformComponent::class.java]?.setPosition(RoomOriginOffset)
        try {
            val room = Entity.loadSuspend("asset://standard_room.glb")
            root.addChild(room)
            collectRoomSurfaces(room, roomSurfaces)
            roomReady = true
            RoomPlacement.updateAvailability(true)
            RoomPlacement.report("4 × 5 × 2.5 米房间已就绪，拖入模型后可继续移动或旋转")
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            RoomPlacement.report("房间模型加载失败，请重新打开")
        }
    }

    LaunchedEffect(attached) {
        if (!attached) return@LaunchedEffect
        try {
            for (gesture in RoomPlacement.gestures) {
                when (gesture) {
                    is PlacementGesture.Start -> {
                        furniture.start(gesture.product, gesture.position)
                        followPoint = gesture.position
                        following = true
                    }
                    is PlacementGesture.Move -> followPoint = gesture.position
                    is PlacementGesture.Drop -> {
                        furniture.drop(gesture.position)
                        following = false
                        followPoint = null
                    }
                    PlacementGesture.Cancel -> {
                        furniture.cancel()
                        following = false
                        followPoint = null
                    }
                }
            }
        } finally {
            furniture.cancel()
        }
    }

    // 跟随模式：每帧把预览实体贴到最新射线命中点，松手（Drop）或取消（Cancel）后退出。
    // 降级点（SDK 6.0.0）：射线源为 PlacementPointer 同款 SpatialDragValue.inputDevicePose，
    // 只在拖拽事件流中按帧携带，没有与之同坐标系的独立可轮询射线 API（tracking 模块坐标系不同）；
    // 因此跟随帧源 = 拖拽事件流的最新命中点，指针静止时实体保持在最近命中点。
    LaunchedEffect(following) {
        if (!following) return@LaunchedEffect
        while (true) {
            withFrameNanos { }
            followPoint?.let { furniture.movePreview(it) }
        }
    }

    // 背面剔除：每帧读取 HMD 并转换到房间本地坐标，
    // 退出到某面墙（或天花板）外侧时隐藏该表面，保证从室外也能看到室内陈设。
    LaunchedEffect(roomReady) {
        if (!roomReady) return@LaunchedEffect
        hmdProvider.dataFlow.collect { data ->
            val local = root.convertPositionFrom(data.hmdPose.position, null)
            updateRoomSurfaceVisibility(local, roomSurfaces)
        }
    }
}

// An inscribed circle stays on a detected horizontal plane regardless of its yaw.
internal fun fitsSurface(
    size: PhysicalSize,
    planeWidth: Float,
    planeDepth: Float,
    offsetX: Float,
    offsetZ: Float
): Boolean {
    val furnitureRadius = hypot(size.width, size.depth) / 2f
    val usableRadius = min(planeWidth, planeDepth) / 2f - furnitureRadius - 0.05f
    return usableRadius >= 0f && hypot(offsetX, offsetZ) <= usableRadius
}

private val RoomOriginOffset = Vector3(0f, 0f, -2.25f)

// standard_room.glb 的节点名（由 tools/generate_demo_assets.py 生成），背面剔除据此定位表面。
private val RoomSurfaceNames = setOf("ceiling", "wall-z--1", "wall-z-1", "wall-x--1", "wall-x-1")

private fun collectRoomSurfaces(entity: Entity, out: MutableMap<String, Entity>) {
    val name = entity.getName()
    if (name in RoomSurfaceNames) out[name] = entity
    entity.getChildren().forEach { collectRoomSurfaces(it, out) }
}

// 房间净空 4×5×2.5 m（与 FloorPlanRoom 一致）。HMD 越过边界进入某表面外侧即隐藏该表面。
private fun updateRoomSurfaceVisibility(local: Vector3, surfaces: Map<String, Entity>) {
    surfaces["wall-z--1"]?.enabled = local.z >= -RoomHalfDepth
    surfaces["wall-z-1"]?.enabled = local.z <= RoomHalfDepth
    surfaces["wall-x--1"]?.enabled = local.x >= -RoomHalfWidth
    surfaces["wall-x-1"]?.enabled = local.x <= RoomHalfWidth
    surfaces["ceiling"]?.enabled = local.y <= RoomCeilingHeight
}

private const val RoomHalfWidth = 2.0f
private const val RoomHalfDepth = 2.5f
private const val RoomCeilingHeight = 2.5f
