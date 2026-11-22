package com.pico.welcomespace.ui.shop

import com.pico.spatial.core.ecs.CollisionComponent
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.HoverEffectComponent
import com.pico.spatial.core.ecs.InteractableComponent
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.ecs.resource.PhysicsMaterialResource
import com.pico.spatial.core.ecs.resource.ShapeResource
import com.pico.spatial.core.math.Quat
import com.pico.spatial.core.math.Vector3
import com.pico.welcomespace.domain.model.FloorPlanRoom
import com.pico.welcomespace.domain.model.PhysicalSize
import com.pico.welcomespace.domain.model.ShopProduct
import com.pico.welcomespace.domain.usecase.NormalizeFurnitureSizeUseCase
import com.pico.welcomespace.ui.planner.RoomPlannerSession
import kotlinx.coroutines.CancellationException

internal enum class FurnitureEditMode { MOVE, ROTATE }

/** 每个实例使用独立底部中心节点，拖动/旋转不会再次改变模型缩放。 */
internal class FurnitureScene(private val root: Entity) {
    private data class Instance(
        val entity: Entity,
        val product: ShopProduct,
        val size: PhysicalSize,
        val shape: ShapeResource,
        val material: PhysicsMaterialResource,
        var yaw: Float = 0f
    )

    private val placed = mutableMapOf<Entity, Instance>()
    private var preview: Instance? = null

    fun accepts(entity: Entity): Boolean = entity in placed

    suspend fun start(product: ShopProduct, worldPosition: Vector3) {
        cancel()
        val wrapper = Entity()
        var shape: ShapeResource? = null
        var material: PhysicsMaterialResource? = null
        try {
            val mesh = Entity.loadSuspend("asset://${product.modelAsset}")
            wrapper.addChild(mesh)
            val bounds = mesh.getVisualBounds(mesh, true, true)
            val result = NormalizeFurnitureSizeUseCase().normalize(
                PhysicalSize(bounds.size.x, bounds.size.y, bounds.size.z),
                product.physicalSize
            )
            val scale = result.scale
            mesh.components[TransformComponent::class.java]?.apply {
                setScaleVector(Vector3(scale, scale, scale))
                setPosition(Vector3(
                    -bounds.center.x * scale,
                    -(bounds.center.y - bounds.size.y / 2f) * scale,
                    -bounds.center.z * scale
                ))
            }
            val size = result.size
            shape = ShapeResource.createBox(Vector3(size.width, size.height, size.depth))
                .offsetByTranslation(Vector3(0f, size.height / 2f, 0f))
            material = PhysicsMaterialResource()
            wrapper.components.set(CollisionComponent(listOf(shape), material))
            wrapper.components.set(InteractableComponent())
            wrapper.components.set(HoverEffectComponent())
            val instance = Instance(wrapper, product, size, shape, material)
            root.addChild(wrapper)
            preview = instance
            if (!position(instance, root.convertPositionFrom(worldPosition, null))) {
                cancel()
                RoomPlacement.report("模型无法放入当前房间")
            }
        } catch (cancelled: CancellationException) {
            wrapper.destroy()
            shape?.close()
            material?.close()
            throw cancelled
        } catch (error: Exception) {
            wrapper.destroy()
            shape?.close()
            material?.close()
            preview = null
            RoomPlacement.report("模型加载或尺寸校验失败，请检查模型文件")
        }
    }

    fun movePreview(worldPosition: Vector3) {
        preview?.let { position(it, root.convertPositionFrom(worldPosition, null)) }
    }

    fun drop(worldPosition: Vector3) {
        val item = preview ?: return
        if (position(item, root.convertPositionFrom(worldPosition, null))) {
            placed[item.entity] = item
            preview = null
            RoomPlacement.report("${item.product.name}已按标注尺寸放置，可继续抓取移动或切换旋转")
        } else {
            cancel()
            RoomPlacement.report("当前位置无法容纳该家具")
        }
    }

    fun edit(entity: Entity?, delta: Vector3, mode: FurnitureEditMode) {
        val item = placed[entity] ?: return
        val transform = item.entity.components[TransformComponent::class.java] ?: return
        val oldYaw = item.yaw
        if (mode == FurnitureEditMode.ROTATE) {
            item.yaw = (item.yaw + delta.x * 3f) % (2f * Math.PI.toFloat())
        }
        val next = if (mode == FurnitureEditMode.MOVE) {
            Vector3(transform.position.x + delta.x, transform.position.y, transform.position.z + delta.z)
        } else transform.position
        if (!position(item, next)) item.yaw = oldYaw
    }

    private fun position(item: Instance, point: Vector3): Boolean {
        val target = FloorPlanRoom.choosePlacement(
            item.product, point.x, point.z, RoomPlannerSession.activePlan, item.yaw, item.size
        ) ?: return false
        item.entity.components[TransformComponent::class.java]?.apply {
            setPosition(Vector3(target.x, target.y, target.z))
            setQuaternion(Quat(Vector3(0f, 1f, 0f), item.yaw))
        }
        return true
    }

    fun cancel() {
        preview?.let(::destroy)
        preview = null
    }

    private fun destroy(item: Instance) {
        item.entity.destroy()
        item.shape.close()
        item.material.close()
    }

    fun close() {
        cancel()
        placed.values.forEach(::destroy)
        placed.clear()
    }
}
