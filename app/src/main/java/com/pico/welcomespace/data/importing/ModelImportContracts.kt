package com.pico.welcomespace.data.importing

import com.pico.welcomespace.domain.model.AssetSource
import com.pico.welcomespace.domain.model.FurnitureCategory
import com.pico.welcomespace.domain.model.LengthUnit
import com.pico.welcomespace.domain.model.PhysicalSize

/**
 * §2.2 统一导入入口的请求/结果契约。所有来源（本地/外部文件/Tripo）只走
 * [ModelImportPipeline.import] 一条路径。
 */

/** §2.2 请求结构，字段与设计稿一一对应。 */
data class ModelImportRequest(
    val source: AssetSource,
    val fileRef: String,
    val declaredUnit: LengthUnit? = null,
    val categoryHint: FurnitureCategory? = null,
    val realSizeHint: PhysicalSize? = null,
    val displayName: String? = null
)

/** §2.2 结果结构：成功时 error 为 null；失败时 profile 为 null；不确定项一律转 warning。 */
data class ModelImportResult(
    val profile: com.pico.welcomespace.domain.model.AssetProfile?,
    val warnings: List<ImportWarning>,
    val error: ImportError?
)

/** §8.1 错误分类：文件类与格式类错误拒绝导入；单位类/尺寸类以 warning 放行。 */
enum class ImportErrorKind {
    FILE_NOT_FOUND,
    FILE_UNREADABLE,
    FORMAT_UNSUPPORTED,
    PARSE_FAILED,
    PROFILE_INCOMPLETE
}

data class ImportError(val kind: ImportErrorKind, val message: String)

/** §8.2 原则：任何推断、降级、跳过都必须写进 warning，绝不静默。 */
enum class ImportWarningKind {
    /** 重复导入：幂等复用既有 Profile，不重复入库（§2.4）。 */
    DUPLICATE_REUSED,

    /** 用户声明与文件元数据/格式规范冲突，需要用户确认（§13 决策 1 派生）。 */
    UNIT_CONFLICT,

    /** 启发式怀疑声明单位可疑，但按 §13 不静默重解释。 */
    UNIT_SUSPICIOUS,

    /** 启发式无法唯一判定单位，兜底假定为米（§3.1 优先级 4）。 */
    UNIT_UNRESOLVED,

    /** 资产已标记 unitNormalized，跳过单位归一（§5.2 规则 1）。 */
    UNIT_NORMALIZATION_SKIPPED,

    /** 无真实尺寸，标记待校准（§2.3 步骤 4）。 */
    UNCALIBRATED_SIZE,

    /** 真实尺寸与米制边界比例偏差超过 25%，拒绝自动校准，转人工（§4.3）。 */
    ASPECT_MISMATCH,

    /** 尺寸超界（单轴过大或整体过小），转人工确认（§8.1 尺寸类）。 */
    SIZE_OUT_OF_RANGE,

    /** 缺少米制边界或已校准过，校准步骤跳过（§4 / §5.2 规则 2）。 */
    CALIBRATION_SKIPPED,

    /** 解析不完整（如外部 bin 缺失、USDZ 二进制几何未解析），降级处理（§8.1）。 */
    PARSE_INCOMPLETE
}

data class ImportWarning(val kind: ImportWarningKind, val message: String)
