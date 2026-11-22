package com.pico.welcomespace.domain.usecase

import com.pico.welcomespace.domain.model.PhysicalSize
import kotlin.math.abs

/** 只对新加载的原始几何执行一次；单位转换与尺寸校准分别记录。 */
class NormalizeFurnitureSizeUseCase {
    data class Result(
        val unitScale: Float,
        val calibrationScale: Float,
        val size: PhysicalSize
    ) {
        val scale: Float get() = unitScale * calibrationScale
    }

    fun normalize(
        raw: PhysicalSize,
        declared: PhysicalSize?,
        metersPerUnit: Float = 1f
    ): Result {
        val axes = listOf(raw.width, raw.height, raw.depth)
        require(axes.all { it.isFinite() && it > 0f })
        require(metersPerUnit.isFinite() && metersPerUnit > 0f)
        val meters = axes.map { it * metersPerUnit }
        require(meters.all { it.isFinite() && it > 0f })
        val targets = declared?.let { listOf(it.width, it.height, it.depth) }
        require(targets == null || targets.all { it.isFinite() && it > 0f })
        val axis = targets?.indices?.maxByOrNull { targets[it] } ?: 0
        val calibration = if (targets == null) 1f else targets[axis] / meters[axis]
        val final = meters.map { it * calibration }
        require(final.all { it.isFinite() && it > 0f })
        require(targets == null || final.indices.all { abs(final[it] / targets[it] - 1f) <= .25f }) {
            "模型比例与标称尺寸相差超过 25%，需要人工校准"
        }
        return Result(metersPerUnit, calibration, PhysicalSize(final[0], final[1], final[2]))
    }
}
