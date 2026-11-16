package com.pico.welcomespace.ui.shop

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.math.Vector3
import com.pico.spatial.ui.foundation.content.SpatialView
import com.pico.welcomespace.domain.model.FloorPlanRoom
import com.pico.welcomespace.domain.model.RoomSurface
import com.pico.welcomespace.domain.model.RoomSurfaceKind
import com.pico.welcomespace.domain.model.VirtualRoomPlan
import com.pico.welcomespace.domain.model.ShopCategory
import com.pico.welcomespace.domain.model.ShopProduct
import com.pico.welcomespace.domain.model.PhysicalSize
import com.pico.welcomespace.ui.planner.RoomPlannerSession
import kotlinx.coroutines.channels.Channel
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
    var active by mutableStateOf(false)
        private set
    var message by mutableStateOf("正在打开虚拟样板间…")
        private set

    fun updateAvailability(value: Boolean) {
        active = value
    }

    fun report(value: String) {
        message = value
    }

    fun start(product: ShopProduct, x: Float, y: Float, z: Float) {
        if (!active) {
            report("虚拟样板间尚未就绪，不能放置模型")
            return
        }
        gestures.trySend(PlacementGesture.Start(product, Vector3(x, y, z)))
        report("正在拖动${product.name}，会吸附到合法平面并按实物比例放置")
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
    val roomPlan = RoomPlannerSession.activePlan
    val roomRevision = RoomPlannerSession.revision
    var attached by remember { mutableStateOf(false) }
    var roomRoot by remember { mutableStateOf<Entity?>(null) }

    SpatialView(
        modifier = Modifier.size(1.dp),
        initial = { content, _ ->
            content.addEntity(root)
            attached = true
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            RoomPlacement.updateAvailability(false)
            root.destroy()
        }
    }

    LaunchedEffect(attached) {
        if (!attached) return@LaunchedEffect
        RoomPlacement.updateAvailability(true)
        root.components[TransformComponent::class.java]?.setPosition(RoomOriginOffset)
        RoomPlacement.report("虚拟样板间已就绪：拖动商品模型，松手后吸附到地面或桌面")
    }

    LaunchedEffect(attached, roomRevision) {
        if (!attached) return@LaunchedEffect
        roomRoot?.destroy()
        val nextRoomRoot = Entity()
        root.addChild(nextRoomRoot)
        buildVirtualRoom(nextRoomRoot, roomPlan)
        roomRoot = nextRoomRoot
        RoomPlacement.report("${roomPlan.name}已生成，可拖动商品模型放置")
    }

    LaunchedEffect(attached) {
        if (!attached) return@LaunchedEffect
        var preview: Entity? = null
        var selected: ShopProduct? = null
        try {
            for (gesture in RoomPlacement.gestures) {
                when (gesture) {
                    is PlacementGesture.Start -> {
                        preview?.destroy()
                        preview = null
                        selected = gesture.product
                        try {
                            val model = Entity.loadSuspend("asset://${gesture.product.modelAsset}")
                            val modelScale = model.realWorldScaleFor(gesture.product)
                            val position = root.convertPositionFrom(gesture.position, null)
                            val placement = chooseVirtualSurface(
                                gesture.product,
                                position,
                                RoomPlannerSession.activePlan
                            )
                            model.components[TransformComponent::class.java]?.apply {
                                setPosition(
                                    placement?.toModelPosition(gesture.product, modelScale)
                                        ?: Vector3(position.x, 0f, position.z)
                                )
                                setScaleVector(Vector3(modelScale, modelScale, modelScale))
                            }
                            root.addChild(model)
                            preview = model
                        } catch (error: Exception) {
                            RoomPlacement.report("模型加载失败，请重新尝试")
                        }
                    }
                    is PlacementGesture.Move -> {
                        val product = selected
                        val position = root.convertPositionFrom(gesture.position, null)
                        val placement = if (product == null) {
                            null
                        } else {
                            chooseVirtualSurface(product, position, RoomPlannerSession.activePlan)
                        }
                        preview?.components?.get(TransformComponent::class.java)
                            ?.setPosition(
                                placement?.toModelPosition(product!!, preview?.currentUniformScale() ?: 1f)
                                    ?: Vector3(position.x, 0f, position.z)
                            )
                    }
                    is PlacementGesture.Drop -> {
                        val product = selected
                        val model = preview
                        if (product != null && model != null) {
                            val position = root.convertPositionFrom(gesture.position, null)
                            val placement = chooseVirtualSurface(
                                product,
                                position,
                                RoomPlannerSession.activePlan
                            )
                            if (placement == null) {
                                model.destroy()
                                RoomPlacement.report(
                                    "未找到足够大的${surfaceName(product)}，请换一个房间区域"
                                )
                            } else {
                                // GLB coordinates are meters; the in-page preview scale is not retained.
                                val modelScale = model.currentUniformScale()
                                model.components[TransformComponent::class.java]?.apply {
                                    setPosition(placement.toModelPosition(product, modelScale))
                                    setScaleVector(Vector3(modelScale, modelScale, modelScale))
                                }
                                RoomPlacement.report(
                                    "${product.name}已按 1:1 尺寸放置在${placement.label}"
                                )
                            }
                        }
                        preview = null
                        selected = null
                    }
                    PlacementGesture.Cancel -> {
                        preview?.destroy()
                        preview = null
                        selected = null
                        RoomPlacement.report("已取消放置")
                    }
                }
            }
        } finally {
            preview?.destroy()
        }
    }
}

private suspend fun buildVirtualRoom(root: Entity, plan: VirtualRoomPlan) {
    plan.surfaces.forEach { surface ->
        val thickness = if (surface.kind == RoomSurfaceKind.TABLE) 0.08f else 0.035f
        addBox(
            root = root,
            position = Vector3(surface.centerX, surface.heightY - thickness / 2f, surface.centerZ),
            scale = Vector3(surface.width, thickness, surface.depth)
        )
        if (surface.kind == RoomSurfaceKind.TABLE) {
            addBox(
                root = root,
                position = Vector3(surface.centerX, surface.heightY / 2f, surface.centerZ),
                scale = Vector3(surface.width * 0.88f, surface.heightY, surface.depth * 0.88f)
            )
        }
    }
    plan.walls.forEach { wall ->
        addBox(
            root = root,
            position = Vector3(wall.centerX, wall.height / 2f, wall.centerZ),
            scale = Vector3(wall.width, wall.height, wall.depth)
        )
    }
}

private suspend fun addBox(root: Entity, position: Vector3, scale: Vector3) {
    val box = Entity.loadSuspend("asset://$RoomBlockAsset")
    box.components[TransformComponent::class.java]?.apply {
        setPosition(position)
        setScaleVector(scale)
    }
    root.addChild(box)
}

private fun chooseVirtualSurface(
    product: ShopProduct,
    position: Vector3,
    plan: VirtualRoomPlan
): VirtualPlacement? =
    FloorPlanRoom.choosePlacement(product, position.x, position.z, plan)
        ?.let { target ->
            VirtualPlacement(
                x = target.x,
                y = target.y,
                z = target.z,
                label = target.surface.label
            )
        }

private data class VirtualPlacement(
    val x: Float,
    val y: Float,
    val z: Float,
    val label: String
) {
    fun toModelPosition(product: ShopProduct, modelScale: Float): Vector3 =
        Vector3(x, y - product.modelBaseY * modelScale, z)
}

private fun Entity.realWorldScaleFor(product: ShopProduct): Float {
    val size = runCatching { getVisualBounds(this, true, true).size }.getOrNull()
        ?: return 1f
    if (size.x <= 0f || size.y <= 0f || size.z <= 0f) return 1f

    val target = product.physicalSize
    return when (max(target.width, max(target.height, target.depth))) {
        target.width -> target.width / size.x
        target.height -> target.height / size.y
        else -> target.depth / size.z
    }.coerceIn(MinFurnitureScale, MaxFurnitureScale)
}

private fun Entity.currentUniformScale(): Float =
    components[TransformComponent::class.java]?.scaleVector?.x ?: 1f

internal fun chooseSurface(
    product: ShopProduct,
    position: Vector3,
    surfaces: List<RoomSurface>
): RoomSurface? = FloorPlanRoom.choosePlacement(
    product = product,
    x = position.x,
    z = position.z,
    plan = VirtualRoomPlan(
        name = "test",
        widthMeters = 0f,
        depthMeters = 0f,
        walls = emptyList(),
        surfaces = surfaces
    )
)?.surface

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

private fun surfaceName(product: ShopProduct): String =
    if (product.category == ShopCategory.LIGHTING) "桌面" else "地面"

private val RoomOriginOffset = Vector3(0f, 0f, -4.5f)
private const val RoomBlockAsset = "gray_room_block.gltf"
private const val MinFurnitureScale = 0.05f
private const val MaxFurnitureScale = 20f
