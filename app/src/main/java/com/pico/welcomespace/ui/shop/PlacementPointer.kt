package com.pico.welcomespace.ui.shop

import com.pico.spatial.core.math.Vector3
import com.pico.spatial.ui.foundation.gesture.SpatialDragValue
import com.pico.spatial.ui.foundation.gesture.data.InteractionKind

/** 远射线使用与世界地面的交点，不能把控制器本体的位置当作落点。 */
internal fun SpatialDragValue.floorPoint(): Vector3? {
    val origin = inputDevicePose.rawPosition
    if (!origin.x.isFinite() || !origin.y.isFinite() || !origin.z.isFinite()) return null
    if (interactionKind == InteractionKind.DirectPinch || interactionKind == InteractionKind.Poke) {
        return Vector3(origin.x, 0f, origin.z)
    }
    val q = inputDevicePose.rawRotation.toQuaternion()
    val dx = -2f * (q.x * q.z + q.w * q.y)
    val dy = -2f * (q.y * q.z - q.w * q.x)
    val dz = -(1f - 2f * (q.x * q.x + q.y * q.y))
    if (dy >= -.02f) return null
    val distance = -origin.y / dy
    if (!distance.isFinite() || distance !in 0f..20f) return null
    return Vector3(origin.x + dx * distance, 0f, origin.z + dz * distance)
}
