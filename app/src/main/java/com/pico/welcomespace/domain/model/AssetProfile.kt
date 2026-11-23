package com.pico.welcomespace.domain.model

/**
 * 资产标准化层（第二阶段架构设计 §1）。所有尺寸字段统一复用 [PhysicalSize]，
 * 不新增功能重复的类型。[ShopProduct] 通过 assetId 指向本层，导入元数据不写入商品模型。
 */

/** §1.2 家具类别（沙发/座椅/桌/柜/灯具/其他）。 */
enum class FurnitureCategory(val label: String) {
    SOFA("沙发"),
    SEAT("座椅"),
    TABLE("桌"),
    CABINET("柜"),
    LIGHTING("灯具"),
    OTHER("其他")
}

/** §1.2 资产来源。 */
enum class AssetSource {
    LOCAL_ASSET,
    EXTERNAL_FILE,
    TRIPO_GENERATED
}

/** §13 决策 1：本期仅支持 GLB / GLTF / USDZ。 */
enum class ModelFormat(val fileExtension: String) {
    GLB("glb"),
    GLTF("gltf"),
    USDZ("usdz")
}

/** §3 单位标准化目标恒为米；这里只枚举检测中出现的线性单位。 */
enum class LengthUnit(val metersPerUnit: Float) {
    METERS(1f),
    CENTIMETERS(0.01f),
    MILLIMETERS(0.001f);

    companion object {
        /** USDZ `metersPerUnit` 等数值 → 单位；无法识别的数值返回 null（视为未知）。 */
        fun fromMetersPerUnit(value: Float): LengthUnit? =
            entries.firstOrNull { kotlin.math.abs(it.metersPerUnit - value) < 1e-6f }
    }
}

/** §1.2 单位来源：文件元数据 / 启发式 / 用户声明 / 未知。 */
enum class UnitDetectionSource {
    FILE_METADATA,
    HEURISTIC,
    USER,
    UNKNOWN
}

/** §1.2 真实尺寸来源，优先级：用户输入 > 产品规格 > 参考库 > 未校准（§13 决策 3）。 */
enum class RealSizeSource {
    USER,
    SPEC,
    REFERENCE,
    ESTIMATED,
    NONE
}

/** §1.2 UpAxis，仅记录不转换。 */
enum class UpAxis {
    Y_UP,
    Z_UP,
    UNKNOWN
}

/** §1.2 Pivot 语义。 */
enum class PivotKind {
    BOTTOM_CENTER,
    CENTER,
    UNKNOWN
}

/** §1.2 底座/原点偏移，复用现有 ShopProduct.modelBaseY 语义（米）。 */
data class PivotOffset(val x: Float = 0f, val y: Float = 0f, val z: Float = 0f)

/** §1.2 尺寸可信度。 */
enum class SizeConfidence {
    VERIFIED,
    ESTIMATED,
    TO_CALIBRATE
}

/**
 * §5.1 幂等标记。unitNormalized / realSizeCalibrated 置位后，
 * Pipeline 必须跳过对应步骤，绝不重复乘 scale（§5.2）。
 */
data class NormalizationState(
    val unitNormalized: Boolean,
    val realSizeCalibrated: Boolean,
    val normalizationVersion: Int
) {
    companion object {
        /** 当前标准化算法版本；格式或规则变更时递增。 */
        const val CURRENT_VERSION = 1
    }
}

/** §1.2 材质摘要：本期只记录引用，不新建材质子系统。 */
data class MaterialSummary(val note: String)

/** §7.2 校验状态：记录比例是否合理、是否需人工复核。 */
data class ValidationResult(
    val passed: Boolean,
    val needsManualReview: Boolean,
    /** 校准后 finalSize 与 realSize 的最大逐轴偏差；未校准或被拒绝时为 null。 */
    val ratioDeviation: Float?,
    val messages: List<String> = emptyList()
)

/**
 * §1.2 AssetProfile 数据结构。字段含义见设计稿；
 * [contentHash] 为 §2.4 幂等所需的文件内容摘要（sha256），存量资产沿用既有记录值。
 */
data class AssetProfile(
    val assetId: String,
    val displayName: String,
    val category: FurnitureCategory,
    val source: AssetSource,
    val fileRef: String,
    val originalFormat: ModelFormat,
    val declaredUnit: LengthUnit?,
    val detectedUnit: LengthUnit?,
    val unitSource: UnitDetectionSource,
    val originalBounds: PhysicalSize?,
    val meterBounds: PhysicalSize?,
    val realSize: PhysicalSize?,
    val realSizeSource: RealSizeSource,
    val finalSize: PhysicalSize?,
    val upAxis: UpAxis,
    val pivot: PivotKind,
    val pivotOffset: PivotOffset,
    val unitScaleApplied: Float,
    val realSizeScaleApplied: Float,
    val normalization: NormalizationState,
    val confidence: SizeConfidence,
    val previewRef: String? = null,
    val materialInfo: MaterialSummary? = null,
    val importedAt: Long,
    val validation: ValidationResult,
    val contentHash: String? = null
) {
    /** §5.2 规则 4：运行时展示只读 finalSize，禁止对资产做二次缩放。 */
    val displaySize: PhysicalSize? get() = finalSize
}
