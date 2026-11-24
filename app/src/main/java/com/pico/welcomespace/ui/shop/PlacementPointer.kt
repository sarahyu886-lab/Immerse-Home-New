package com.pico.welcomespace.ui.shop

import com.pico.spatial.core.math.Vector3
import com.pico.spatial.ui.foundation.gesture.SpatialDragValue
import com.pico.spatial.ui.foundation.gesture.data.InteractionKind

/**
 * 取手柄射线与世界地面的交点作为落点。
 * 射线未明显指向地面时退回手柄正下方，保证任何角度抓取都能得到落点，
 * 否则会出现“抓住模型但拖不进样板间”的情况。
 */
internal fun SpatialDragValue.floorPoint(): Vector3 {
    val origin = inputDevicePose.rawPosition
    val fallback = Vector3(
        if (origin.x.isFinite()) origin.x else 0f,
        0f,
        if (origin.z.isFinite()) origin.z else 0f
    )
    if (!origin.x.isFinite() || !origin.y.isFinite() || !origin.z.isFinite()) return fallback
    if (interactionKind == InteractionKind.DirectPinch || interactionKind == InteractionKind.Poke) {
        return fallback
    }
    val q = inputDevicePose.rawRotation.toQuaternion()
    val dx = -2f * (q.x * q.z + q.w * q.y)
    val dy = -2f * (q.y * q.z - q.w * q.x)
    val dz = -(1f - 2f * (q.x * q.x + q.y * q.y))
    if (dy >= -.02f) return fallback
    val distance = -origin.y / dy
    if (!distance.isFinite() || distance !in 0f..20f) return fallback
    return Vector3(origin.x + dx * distance, 0f, origin.z + dz * distance)
}
