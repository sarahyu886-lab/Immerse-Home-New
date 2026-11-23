package com.pico.welcomespace.data.importing

import com.pico.welcomespace.domain.model.AssetProfile
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
import com.pico.welcomespace.domain.model.ValidationResult
import java.io.IOException
import kotlin.math.abs
import kotlin.math.max

/**
 * §2 统一模型导入 Pipeline（六步）：
 * 模型获取 → 格式解析 → 尺寸检测 → Scale 校准 → Asset Profile 生成 → 模型校验 → 注册入库。
 *
 * 幂等与防护（§2.4 / §5）：
 * - 相同 fileRef + 相同内容 → 不重复入库，返回既有 Profile
 * - 已 unitNormalized 的资产 → 跳过单位归一，绝不重复乘 unitScaleApplied
 * - 已 realSizeCalibrated 的资产 → 跳过校准，绝不重复乘 realSizeScaleApplied
 * - 缩放只在 Pipeline 内记录一次；运行时只读 finalSize（§5.2 规则 4）
 */
class ModelImportPipeline(
    private val registry: AssetProfileRegistry,
    private val contentResolver: ModelContentResolver,
    private val gltfParser: ModelFileParser = GltfModelParser(),
    private val usdzParser: ModelFileParser = UsdzModelParser(),
    private val clock: () -> Long = System::currentTimeMillis,
    /** §13 决策 2：比例异常告警阈值 25%。 */
    private val aspectMismatchTolerance: Float = 0.25f
) {

    fun import(request: ModelImportRequest): ModelImportResult {
        val fileRef = request.fileRef
        if (fileRef.isBlank()) {
            return failed(ImportErrorKind.FILE_NOT_FOUND, "fileRef 为空，无法定位模型文件")
        }

        // ── 步骤 1：模型获取 ─────────────────────────────────────────────
        val content = try {
            contentResolver.resolve(fileRef)
        } catch (error: IOException) {
            return failed(ImportErrorKind.FILE_UNREADABLE, "文件不可读: $fileRef（${error.message}）")
        } ?: return failed(ImportErrorKind.FILE_NOT_FOUND, "模型文件不存在: $fileRef")
        val contentHash = sha256Hex(content.bytes)

        // ── §2.4 幂等：相同 fileRef + 相同内容 → 复用既有 Profile ────────
        val previous = registry.findByFileRef(fileRef)
        if (previous != null && previous.contentHash == contentHash) {
            return ModelImportResult(
                profile = previous,
                warnings = listOf(
                    ImportWarning(
                        ImportWarningKind.DUPLICATE_REUSED,
                        "资产 ${previous.assetId} 已导入过相同内容，幂等复用既有 Profile，不重复入库"
                    )
                ),
                error = null
            )
        }

        // ── 步骤 2：格式解析 ─────────────────────────────────────────────
        val format = ModelFormats.detect(fileRef)
            ?: return failed(
                ImportErrorKind.FORMAT_UNSUPPORTED,
                "格式不支持: $fileRef，当前支持 ${ModelFormats.SUPPORTED_LABEL}（§13 决策 1）"
            )
        val parser = if (format == ModelFormat.USDZ) usdzParser else gltfParser
        val parsed = try {
            parser.parse(fileRef, content)
        } catch (error: ModelParseException) {
            return failed(ImportErrorKind.PARSE_FAILED, "解析失败: ${error.message}")
        }

        val warnings = mutableListOf<ImportWarning>()
        var needsManualReview = false
        fun flagReview() {
            needsManualReview = true
        }

        val rawBounds = parsed.bounds
        if (rawBounds != null && !isValidBounds(rawBounds)) {
            return failed(ImportErrorKind.PARSE_FAILED, "模型边界无效: $rawBounds，拒绝导入")
        }
        if (rawBounds == null) {
            warnings += ImportWarning(
                ImportWarningKind.PARSE_INCOMPLETE,
                "未能解析出几何边界（${parsed.unitEvidence}），尺寸信息缺失，转人工确认"
            )
            flagReview()
        }

        // ── 步骤 3：尺寸检测（单位归一到米，§3；幂等防护 §5.2 规则 1）────
        val unitDecision = if (previous?.normalization?.unitNormalized == true) {
            warnings += ImportWarning(
                ImportWarningKind.UNIT_NORMALIZATION_SKIPPED,
                "资产已做过单位归一（unitScaleApplied=${previous.unitScaleApplied}），按 §5.2 跳过，绝不二次缩放"
            )
            UnitDecision(
                declaredUnit = previous.declaredUnit,
                detectedUnit = previous.detectedUnit,
                unitSource = previous.unitSource,
                scale = previous.unitScaleApplied
            )
        } else {
            detectUnit(request, parsed, rawBounds, warnings, ::flagReview)
        }
        val meterBounds = rawBounds?.scaledBy(unitDecision.scale)

        // ── 步骤 4：Scale 校准（真实尺寸等比校准，§4；幂等防护 §5.2 规则 2）──
        val calibration = calibrate(request, previous, meterBounds, warnings, ::flagReview)

        // ── 步骤 5：Profile 生成 ─────────────────────────────────────────
        val assetId = deriveAssetId(fileRef)
        val displayName = request.displayName
            ?: fileRef.substringAfterLast('/').substringAfterLast('\\').substringBeforeLast('.')
                .ifBlank { fileRef }
        if (assetId.isBlank() || displayName.isBlank()) {
            return failed(ImportErrorKind.PROFILE_INCOMPLETE, "Profile 必填字段缺失（assetId / displayName）")
        }
        val category = request.categoryHint ?: FurnitureCategory.OTHER
        val confidence = calibration.confidenceOverride
            ?: sizeConfidence(unitDecision.unitSource, calibration.realSize, calibration.realSizeSource)

        // ── 步骤 6：模型校验（比例/尺寸异常已转 warning，此处汇总并注册）──
        val effectiveSize = calibration.finalSize
        if (effectiveSize != null && isSizeOutOfRange(effectiveSize)) {
            warnings += ImportWarning(
                ImportWarningKind.SIZE_OUT_OF_RANGE,
                "校准后尺寸超出合理范围（单轴 > ${SIZE_LIMIT_METERS}m 或整体过小），转人工确认（§8.1 尺寸类）"
            )
            flagReview()
        }
        val profile = AssetProfile(
            assetId = assetId,
            displayName = displayName,
            category = category,
            source = request.source,
            fileRef = fileRef,
            originalFormat = format,
            declaredUnit = unitDecision.declaredUnit,
            detectedUnit = unitDecision.detectedUnit,
            unitSource = unitDecision.unitSource,
            originalBounds = rawBounds,
            meterBounds = meterBounds,
            realSize = calibration.realSize,
            realSizeSource = calibration.realSizeSource,
            finalSize = calibration.finalSize,
            upAxis = parsed.upAxis,
            pivot = PivotKind.BOTTOM_CENTER, // 运行时 wrapper 语义，与 FurnitureScene 底部中心一致
            pivotOffset = PivotOffset(
                x = 0f,
                y = parsed.boundsMinY?.times(unitDecision.scale) ?: 0f,
                z = 0f
            ),
            unitScaleApplied = unitDecision.scale,
            realSizeScaleApplied = calibration.realSizeScaleApplied,
            normalization = NormalizationState(
                unitNormalized = true,
                realSizeCalibrated = calibration.calibrationApplied,
                normalizationVersion = NormalizationState.CURRENT_VERSION
            ),
            confidence = confidence,
            materialInfo = MaterialSummary("${format.name} 原始材质引用，未改动"),
            importedAt = clock(),
            validation = ValidationResult(
                passed = true,
                needsManualReview = needsManualReview,
                ratioDeviation = calibration.ratioDeviation,
                messages = warnings.map { it.message }
            ),
            contentHash = contentHash
        )
        registry.register(profile)
        return ModelImportResult(profile = profile, warnings = warnings, error = null)
    }

    /** §3.1 检测优先级：文件元数据 > 用户声明 > 启发式 > 兜底假定米 + TO_CALIBRATE。 */
    private fun detectUnit(
        request: ModelImportRequest,
        parsed: ParsedModel,
        rawBounds: PhysicalSize?,
        warnings: MutableList<ImportWarning>,
        flagReview: () -> Unit
    ): UnitDecision {
        val declared = request.declaredUnit
        val metadata = parsed.metadataUnit
        if (metadata != null) {
            if (declared != null && declared != metadata) {
                warnings += ImportWarning(
                    ImportWarningKind.UNIT_CONFLICT,
                    "声明单位 $declared 与文件元数据 $metadata（${parsed.unitEvidence}）冲突，按 §3.1 元数据优先，需要用户确认"
                )
                flagReview()
            }
            return UnitDecision(declared, metadata, UnitDetectionSource.FILE_METADATA, metadata.metersPerUnit)
        }
        if (declared != null) {
            parsed.specDefaultUnit?.let { spec ->
                if (declared != spec) {
                    warnings += ImportWarning(
                        ImportWarningKind.UNIT_CONFLICT,
                        "声明单位 $declared 与 ${parsed.format} 规范默认 $spec 冲突，已按声明执行，需要用户确认（§13）"
                    )
                    flagReview()
                }
            }
            return UnitDecision(declared, declared, UnitDetectionSource.USER, declared.metersPerUnit)
        }
        val spec = parsed.specDefaultUnit
        if (spec != null) {
            // glTF 家族规范默认米（§13）：启发式只提示可疑，不静默重解释
            warnIfHeuristicSuspicious(parsed.format, rawBounds, request.categoryHint, spec, warnings, flagReview)
            return UnitDecision(declared, spec, UnitDetectionSource.FILE_METADATA, spec.metersPerUnit)
        }
        return inferWithoutMetadata(rawBounds, request.categoryHint, warnings, flagReview)
    }

    private fun warnIfHeuristicSuspicious(
        format: ModelFormat,
        rawBounds: PhysicalSize?,
        categoryHint: FurnitureCategory?,
        spec: LengthUnit,
        warnings: MutableList<ImportWarning>,
        flagReview: () -> Unit
    ) {
        val dominant = rawBounds?.dominant() ?: return
        val category = categoryHint ?: FurnitureCategory.OTHER
        val inferred = UnitHeuristics.infer(dominant, category) ?: return
        if (inferred != spec) {
            warnings += ImportWarning(
                ImportWarningKind.UNIT_SUSPICIOUS,
                "边界主导轴 ${dominant}按 $format 规范米不合常理，启发式怀疑是 $inferred；按 §13 不静默重解释，保持 $spec 并转人工确认"
            )
            flagReview()
        }
    }

    private fun inferWithoutMetadata(
        rawBounds: PhysicalSize?,
        categoryHint: FurnitureCategory?,
        warnings: MutableList<ImportWarning>,
        flagReview: () -> Unit
    ): UnitDecision {
        val category = categoryHint ?: FurnitureCategory.OTHER
        val inferred = rawBounds?.dominant()?.let { UnitHeuristics.infer(it, category) }
        return if (inferred != null) {
            UnitDecision(null, inferred, UnitDetectionSource.HEURISTIC, inferred.metersPerUnit)
        } else {
            warnings += ImportWarning(
                ImportWarningKind.UNIT_UNRESOLVED,
                "无单位元数据且启发式无法唯一判定，兜底假定为米并标记待校准（§3.1 优先级 4）"
            )
            flagReview()
            UnitDecision(null, null, UnitDetectionSource.UNKNOWN, 1f)
        }
    }

    /** §4 真实尺寸校准：与单位转换严格分离，只允许等比（Vector3(k,k,k)）。 */
    private fun calibrate(
        request: ModelImportRequest,
        previous: AssetProfile?,
        meterBounds: PhysicalSize?,
        warnings: MutableList<ImportWarning>,
        flagReview: () -> Unit
    ): CalibrationOutcome {
        if (previous?.normalization?.realSizeCalibrated == true) {
            warnings += ImportWarning(
                ImportWarningKind.CALIBRATION_SKIPPED,
                "资产已校准（realSizeScaleApplied=${previous.realSizeScaleApplied}），按 §5.2 跳过，绝不重复乘校准系数"
            )
            val carriedScale = previous.realSizeScaleApplied
            return CalibrationOutcome(
                realSize = previous.realSize,
                realSizeSource = previous.realSizeSource,
                realSizeScaleApplied = carriedScale,
                finalSize = meterBounds?.scaledBy(carriedScale),
                ratioDeviation = null,
                calibrationApplied = true,
                confidenceOverride = null
            )
        }

        val real = request.realSizeHint
        if (real == null) {
            warnings += ImportWarning(
                ImportWarningKind.UNCALIBRATED_SIZE,
                "未提供真实尺寸，不猜测（§8.2）；finalSize 采用米制边界并标记 TO_CALIBRATE"
            )
            return CalibrationOutcome(null, RealSizeSource.NONE, 1f, meterBounds, null, false, null)
        }
        if (meterBounds == null) {
            warnings += ImportWarning(
                ImportWarningKind.CALIBRATION_SKIPPED,
                "缺少米制边界，无法执行真实尺寸校准"
            )
            return CalibrationOutcome(real, REAL_SIZE_SOURCE_FOR_HINT, 1f, null, null, false, null)
        }

        // §4.2 主导轴：realSize 中相对最大的轴（请求结构 §2.2 未含指定轴字段）
        val dominantAxis = real.dominantAxisIndex()
        val dominantBounds = meterBounds.axis(dominantAxis)
        if (dominantBounds <= 0f) {
            warnings += ImportWarning(
                ImportWarningKind.CALIBRATION_SKIPPED,
                "米制边界主导轴长度为 0，无法计算校准系数"
            )
            return CalibrationOutcome(real, REAL_SIZE_SOURCE_FOR_HINT, 1f, meterBounds, null, false, null)
        }
        val calibrationScale = real.axis(dominantAxis) / dominantBounds

        // §4.3 比例异常检测：超过阈值 → 拒绝自动校准（不自动拉伸），转人工确认
        var deviation = 0f
        for (axis in 0 until 3) {
            val boundAxis = meterBounds.axis(axis)
            val realAxis = real.axis(axis)
            if (boundAxis <= 0f && realAxis <= 0f) continue
            if (boundAxis <= 0f || realAxis <= 0f) {
                deviation = Float.MAX_VALUE
                break
            }
            deviation = max(deviation, abs(realAxis / boundAxis / calibrationScale - 1f))
        }
        if (deviation > aspectMismatchTolerance) {
            warnings += ImportWarning(
                ImportWarningKind.ASPECT_MISMATCH,
                "真实尺寸与米制边界逐轴比例偏差超过 ${"%.0f".format(aspectMismatchTolerance * 100)}%，" +
                    "已拒绝自动校准并保持米制边界，转人工确认（§4.3）"
            )
            flagReview()
            return CalibrationOutcome(real, REAL_SIZE_SOURCE_FOR_HINT, 1f, meterBounds, null, false, SizeConfidence.TO_CALIBRATE)
        }

        val finalSize = meterBounds.scaledBy(calibrationScale)
        val ratioDeviation = (0 until 3)
            .filter { real.axis(it) > 0f }
            .map { axis -> abs(finalSize.axis(axis) / real.axis(axis) - 1f) }
            .maxOrNull() ?: 0f
        return CalibrationOutcome(real, REAL_SIZE_SOURCE_FOR_HINT, calibrationScale, finalSize, ratioDeviation, true, null)
    }

    /** §4.4 可信度判定。 */
    private fun sizeConfidence(
        unitSource: UnitDetectionSource,
        realSize: PhysicalSize?,
        realSizeSource: RealSizeSource
    ): SizeConfidence = when {
        unitSource == UnitDetectionSource.UNKNOWN -> SizeConfidence.TO_CALIBRATE
        realSize == null || realSizeSource == RealSizeSource.NONE -> SizeConfidence.TO_CALIBRATE
        unitSource == UnitDetectionSource.FILE_METADATA &&
            (realSizeSource == RealSizeSource.USER || realSizeSource == RealSizeSource.SPEC) ->
            SizeConfidence.VERIFIED
        else -> SizeConfidence.ESTIMATED
    }

    private fun deriveAssetId(fileRef: String): String {
        val base = fileRef.substringAfterLast('/').substringAfterLast('\\')
            .substringBeforeLast('.')
            .replace(Regex("[^A-Za-z0-9_-]+"), "-")
            .trim('-')
            .lowercase()
            .ifBlank { "asset" }
        val owner = registry.findByAssetId(base)
        // assetId 稳定不变（§1.2）：同 fileRef 恒同 id；不同文件撞名时追加内容摘要消歧
        return if (owner == null || owner.fileRef == fileRef) {
            base
        } else {
            base + "-" + sha256Hex(fileRef.toByteArray()).take(8)
        }
    }

    private fun failed(kind: ImportErrorKind, message: String) =
        ModelImportResult(profile = null, warnings = emptyList(), error = ImportError(kind, message))

    private class UnitDecision(
        val declaredUnit: LengthUnit?,
        val detectedUnit: LengthUnit?,
        val unitSource: UnitDetectionSource,
        val scale: Float
    )

    private class CalibrationOutcome(
        val realSize: PhysicalSize?,
        val realSizeSource: RealSizeSource,
        val realSizeScaleApplied: Float,
        val finalSize: PhysicalSize?,
        val ratioDeviation: Float?,
        val calibrationApplied: Boolean,
        val confidenceOverride: SizeConfidence?
    )

    private companion object {
        /** 请求结构（§2.2）未携带真实尺寸来源字段；有 hint 时按产品规格（SPEC）记录。 */
        val REAL_SIZE_SOURCE_FOR_HINT = RealSizeSource.SPEC

        /** §8.1 尺寸合理性：单轴上限 6 米（覆盖柜类 2.4m 上限与床类长边），下限整体 5mm。 */
        private const val SIZE_LIMIT_METERS = 6f
        private const val SIZE_FLOOR_METERS = 0.005f

        fun isValidBounds(bounds: PhysicalSize): Boolean {
            val axes = listOf(bounds.width, bounds.height, bounds.depth)
            return axes.all { it.isFinite() && it >= 0f } && axes.any { it > 0f }
        }

        fun isSizeOutOfRange(size: PhysicalSize): Boolean {
            val axes = listOf(size.width, size.height, size.depth)
            return axes.any { it > SIZE_LIMIT_METERS } || axes.all { it <= SIZE_FLOOR_METERS }
        }
    }
}

/** 主导轴下标（0=宽/x, 1=高/y, 2=深/z）；并列时取先出现的轴。 */
internal fun PhysicalSize.dominantAxisIndex(): Int {
    var index = 0
    var largest = width
    if (height > largest) {
        largest = height
        index = 1
    }
    if (depth > largest) {
        index = 2
    }
    return index
}

internal fun PhysicalSize.axis(index: Int): Float = when (index) {
    0 -> width
    1 -> height
    else -> depth
}

internal fun PhysicalSize.dominant(): Float = max(width, max(height, depth))

internal fun PhysicalSize.scaledBy(factor: Float): PhysicalSize =
    PhysicalSize(width * factor, height * factor, depth * factor)
