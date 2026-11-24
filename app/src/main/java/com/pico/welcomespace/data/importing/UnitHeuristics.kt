package com.pico.welcomespace.data.importing

import com.pico.welcomespace.domain.model.FurnitureCategory
import com.pico.welcomespace.domain.model.LengthUnit

/**
 * §3.2 启发式：以米为基准，用常见家具高度区间判断数量级（÷1000 判 mm、÷100 判 cm、÷1 判 m）。
 * 命中唯一解才采纳；命中多个解或全不命中 → null（UNKNOWN，调用方标 TO_CALIBRATE）。
 */
object UnitHeuristics {

    private val heightRangeMeters = mapOf(
        FurnitureCategory.SOFA to 0.6f..1.2f,
        FurnitureCategory.SEAT to 0.6f..1.2f,
        FurnitureCategory.TABLE to 0.7f..0.8f,
        FurnitureCategory.CABINET to 0.6f..2.4f,
        FurnitureCategory.LIGHTING to 0.3f..1.8f,
        FurnitureCategory.OTHER to 0.2f..2.5f
    )

    fun infer(dominantAxisLength: Float, category: FurnitureCategory): LengthUnit? {
        if (!dominantAxisLength.isFinite() || dominantAxisLength <= 0f) return null
        val range = heightRangeMeters.getValue(category)
        // 原始长度 × metersPerUnit = 米制长度；命中唯一解才采纳
        val candidates = listOf(LengthUnit.MILLIMETERS, LengthUnit.CENTIMETERS, LengthUnit.METERS)
            .filter { dominantAxisLength * it.metersPerUnit in range }
        return candidates.singleOrNull()
    }
}
