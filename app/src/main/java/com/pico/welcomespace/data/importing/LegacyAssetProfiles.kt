package com.pico.welcomespace.data.importing

import com.pico.welcomespace.domain.model.AssetProfile
import com.pico.welcomespace.domain.model.AssetSource
import com.pico.welcomespace.domain.model.FurnitureCategory
import com.pico.welcomespace.domain.model.LengthUnit
import com.pico.welcomespace.domain.model.MaterialSummary
import com.pico.welcomespace.domain.model.ModelFormat
import com.pico.welcomespace.domain.model.NormalizationState
import com.pico.welcomespace.domain.model.PhysicalSize
import com.pico.welcomespace.domain.model.PivotKind
import com.pico.welcomespace.domain.model.PivotOffset
import com.pico.welcomespace.domain.model.RealSizeSource
import com.pico.welcomespace.domain.model.SizeConfidence
import com.pico.welcomespace.domain.model.UnitDetectionSource
import com.pico.welcomespace.domain.model.UpAxis
import com.pico.welcomespace.domain.model.ValidationResult

/**
 * §7 存量 3 个模型的校准修正：只新增 AssetProfile 附加层。
 *
 * 硬性约束与回滚（§7.3 / §7.4）：
 * - 不修改 .glb 文件本体（不改几何、不改贴图），本记录为纯附加层
 * - 不改动现有 FakeShopRepository.physicalSize 数值，realSize 沿用目录标称值
 * - 现有渲染路径（FurnitureScene/RoomStage）不读取本记录，行为保持不变
 * - 回滚 = 删除本文件（或从 Registry 移除 seed），渲染自动回退到原硬编码逻辑
 *
 * originalBounds 为构建期实测值（与 assets/profiles 目录下既有记录一致，
 * 节点变换已应用，单位米）；contentHash 与对应 .glb 的实际 sha256 一致，保证幂等复用生效。
 */
object LegacyAssetProfiles {

    fun create(): List<AssetProfile> = listOf(
        legacy(
            assetId = "sofa-02",
            displayName = "暮色经典双人沙发",
            category = FurnitureCategory.SOFA,
            fileRef = "sofa.glb",
            meterBounds = PhysicalSize(1.8071673512458801f, 0.7094889283180237f, 0.8177513778209686f),
            realSize = PhysicalSize(1.807f, 0.709f, 0.818f),
            contentHash = "2275ff550c70690f4ca73c9a7120e3ac3d8de16642260c731d7be378829a533d",
            pivotBaseY = 0f
        ),
        legacy(
            assetId = "chair-01",
            displayName = "复古雕花休闲椅",
            category = FurnitureCategory.SEAT,
            fileRef = "ArmChair_01.glb",
            meterBounds = PhysicalSize(0.8484309017658234f, 1.065087635157397f, 0.7657602727413177f),
            realSize = PhysicalSize(0.848f, 1.065f, 0.766f),
            contentHash = "6ef5283521fa9f28b7ee9d8ab4fa699054878e9ed5753545c7e3334dc91b51f5",
            pivotBaseY = 0f
        ),
        legacy(
            assetId = "lamp-01",
            displayName = "灵动关节阅读灯",
            category = FurnitureCategory.LIGHTING,
            fileRef = "desk_lamp_arm_01.glb",
            meterBounds = PhysicalSize(0.20172932744026184f, 0.8926106989383698f, 0.6138725876808167f),
            realSize = PhysicalSize(0.202f, 0.893f, 0.614f),
            contentHash = "da80cd420d47d123f297ab57944ff9ba10c9e9e3c97288b955449c5db7fd3e27",
            pivotBaseY = -0.088f
        )
    )

    val byFileRef: Map<String, AssetProfile> by lazy {
        create().associateBy { it.fileRef }
    }

    private fun legacy(
        assetId: String,
        displayName: String,
        category: FurnitureCategory,
        fileRef: String,
        meterBounds: PhysicalSize,
        realSize: PhysicalSize,
        contentHash: String,
        pivotBaseY: Float
    ): AssetProfile {
        // §4.2 等比校准：主导轴取 realSize 最大轴，Vector3(k, k, k)
        val dominantAxis = realSize.dominantAxisIndex()
        val calibration = realSize.axis(dominantAxis) / meterBounds.axis(dominantAxis)
        val finalSize = meterBounds.scaledBy(calibration)
        val ratioDeviation = (0 until 3)
            .filter { realSize.axis(it) > 0f }
            .map { axis -> kotlin.math.abs(finalSize.axis(axis) / realSize.axis(axis) - 1f) }
            .maxOrNull() ?: 0f
        return AssetProfile(
            assetId = assetId,
            displayName = displayName,
            category = category,
            source = AssetSource.LOCAL_ASSET,
            fileRef = fileRef,
            originalFormat = ModelFormat.GLB,
            declaredUnit = null, // §7.1：存量资产无单位记录
            detectedUnit = LengthUnit.METERS, // §7.2：按 §3 判定（glTF 2.0 规范米）
            unitSource = UnitDetectionSource.FILE_METADATA,
            originalBounds = meterBounds,
            meterBounds = meterBounds,
            realSize = realSize, // §7.2：现有 physicalSize 视为 SPEC 类来源
            realSizeSource = RealSizeSource.SPEC,
            finalSize = finalSize,
            upAxis = UpAxis.Y_UP,
            pivot = PivotKind.BOTTOM_CENTER, // 运行时 wrapper 底部中心，原文件不变
            pivotOffset = PivotOffset(0f, pivotBaseY, 0f), // §1.3：迁移 modelBaseY 语义
            unitScaleApplied = 1f,
            realSizeScaleApplied = calibration,
            normalization = NormalizationState(
                unitNormalized = true,
                realSizeCalibrated = true,
                normalizationVersion = NormalizationState.CURRENT_VERSION
            ),
            // §7.2：无独立规格来源的标 ESTIMATED（目录标称值，非实测规格书）
            confidence = SizeConfidence.ESTIMATED,
            materialInfo = MaterialSummary("glTF 内嵌材质与贴图引用，原样保留"),
            importedAt = 0L, // 存量资产，导入时间未知
            validation = ValidationResult(
                passed = true,
                needsManualReview = false,
                ratioDeviation = ratioDeviation,
                messages = listOf("存量资产：几何实测与既有记录一致，真实尺寸沿用目录标称值（ESTIMATED）")
            ),
            contentHash = contentHash
        )
    }
}
