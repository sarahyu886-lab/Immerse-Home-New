package com.pico.welcomespace.data.importing

import com.pico.welcomespace.domain.model.AssetProfile
import com.pico.welcomespace.domain.model.AssetSource
import com.pico.welcomespace.domain.model.FurnitureCategory
import com.pico.welcomespace.domain.model.LengthUnit
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * §9 测试矩阵的 JVM 覆盖：单位换算（#1~#3）、无单位启发式（#4）、
 * 真实尺寸校准与可信度（#5/#6）、比例超差不自动拉伸（#7）、
 * 幂等重导入（#8）、重复 Scale 防护（#9）、UpAxis/Pivot 记录（#10）、
 * 文件/格式/解析异常（#11）。
 */
class ModelImportPipelineTest {

    private class MapResolver(private val files: Map<String, ByteArray>) : ModelContentResolver {
        override fun resolve(fileRef: String): ModelContent? =
            files[fileRef]?.let { ModelContent(fileRef, it) }
    }

    private fun pipeline(
        files: Map<String, ByteArray>,
        registry: AssetProfileRegistry = InMemoryAssetProfileRegistry()
    ): ModelImportPipeline = ModelImportPipeline(
        registry = registry,
        contentResolver = MapResolver(files),
        clock = { 1234L }
    )

    private fun request(
        fileRef: String,
        declaredUnit: LengthUnit? = null,
        categoryHint: FurnitureCategory? = null,
        realSizeHint: PhysicalSize? = null,
        displayName: String? = null
    ) = ModelImportRequest(
        source = AssetSource.LOCAL_ASSET,
        fileRef = fileRef,
        declaredUnit = declaredUnit,
        categoryHint = categoryHint,
        realSizeHint = realSizeHint,
        displayName = displayName
    )

    // ── 合成模型构造（accessor 自带 min/max，无需 BIN 数据）──────────────

    private fun gltfJson(
        min: FloatArray,
        max: FloatArray,
        metersPerUnit: Float? = null,
        translation: FloatArray? = null
    ): String {
        val extensionBlock = if (metersPerUnit != null) {
            "\"extensionsUsed\":[\"EXT_model_units\"]," +
                "\"extensions\":{\"EXT_model_units\":{\"metersPerUnit\":$metersPerUnit}},"
        } else {
            ""
        }
        val translationBlock = translation?.let { "\"translation\":[${it.joinToString()}]," } ?: ""
        return "{\"asset\":{\"version\":\"2.0\"},$extensionBlock\"scene\":0," +
            "\"scenes\":[{\"nodes\":[0]}]," +
            "\"nodes\":[{\"mesh\":0,$translationBlock\"name\":\"node0\"}]," +
            "\"meshes\":[{\"primitives\":[{\"attributes\":{\"POSITION\":0}}]}]," +
            "\"accessors\":[{\"type\":\"VEC3\",\"componentType\":5126,\"count\":8," +
            "\"min\":[${min.joinToString()}],\"max\":[${max.joinToString()}]}]}"
    }

    private fun glb(json: String): ByteArray {
        val jsonBytes = json.toByteArray(Charsets.UTF_8)
        val padding = (4 - (jsonBytes.size % 4)) % 4
        val total = 12 + 8 + jsonBytes.size + padding + 8 // 头 + JSON chunk + 空 BIN chunk
        val buffer = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(0x46546C67) // "glTF"
        buffer.putInt(2)
        buffer.putInt(total)
        buffer.putInt(jsonBytes.size + padding)
        buffer.putInt(0x4E4F534A) // "JSON"
        buffer.put(jsonBytes)
        repeat(padding) { buffer.put(' '.code.toByte()) }
        buffer.putInt(0)
        buffer.putInt(0x004E4942) // "BIN\0"
        return buffer.array()
    }

    private fun glb(min: FloatArray, max: FloatArray): ByteArray = glb(gltfJson(min, max))

    private fun usdz(usda: String): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("model.usda"))
            zip.write(usda.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return output.toByteArray()
    }

    private fun seedProfile(
        fileRef: String,
        contentHash: String,
        unitScale: Float,
        detectedUnit: LengthUnit?,
        unitSource: UnitDetectionSource,
        unitNormalized: Boolean,
        realSizeCalibrated: Boolean,
        realSizeScale: Float,
        realSize: PhysicalSize? = null
    ): AssetProfile = AssetProfile(
        assetId = fileRef.substringBeforeLast('.'),
        displayName = fileRef,
        category = FurnitureCategory.OTHER,
        source = AssetSource.LOCAL_ASSET,
        fileRef = fileRef,
        originalFormat = ModelFormat.GLB,
        declaredUnit = null,
        detectedUnit = detectedUnit,
        unitSource = unitSource,
        originalBounds = null,
        meterBounds = null,
        realSize = realSize,
        realSizeSource = if (realSize == null) RealSizeSource.NONE else RealSizeSource.SPEC,
        finalSize = null,
        upAxis = UpAxis.Y_UP,
        pivot = PivotKind.BOTTOM_CENTER,
        pivotOffset = PivotOffset(),
        unitScaleApplied = unitScale,
        realSizeScaleApplied = realSizeScale,
        normalization = NormalizationState(
            unitNormalized = unitNormalized,
            realSizeCalibrated = realSizeCalibrated,
            normalizationVersion = NormalizationState.CURRENT_VERSION
        ),
        confidence = SizeConfidence.TO_CALIBRATE,
        importedAt = 0L,
        validation = ValidationResult(passed = true, needsManualReview = false, ratioDeviation = null),
        contentHash = contentHash
    )

    private fun warningsOf(result: ModelImportResult) = result.warnings.map { it.kind }

    // ── #1~#3 单位换算 ─────────────────────────────────────────────────

    @Test
    fun millimeterMetadataConvertsToMetersAndVerifies() {
        val pipeline = pipeline(mapOf("mm.gltf" to gltfJson(
            floatArrayOf(0f, 0f, 0f), floatArrayOf(2000f, 1000f, 500f), metersPerUnit = 0.001f
        ).toByteArray(Charsets.UTF_8)))
        val result = pipeline.import(request("mm.gltf", realSizeHint = PhysicalSize(2f, 1f, 0.5f)))

        assertNull(result.error)
        val profile = result.profile!!
        assertEquals(LengthUnit.MILLIMETERS, profile.detectedUnit)
        assertEquals(UnitDetectionSource.FILE_METADATA, profile.unitSource)
        assertEquals(0.001f, profile.unitScaleApplied, 1e-6f)
        assertEquals(2f, profile.meterBounds!!.width, 1e-4f)
        assertEquals(1f, profile.meterBounds!!.height, 1e-4f)
        assertEquals(0.5f, profile.meterBounds!!.depth, 1e-4f)
        assertEquals(1f, profile.realSizeScaleApplied, 1e-5f)
        assertEquals(SizeConfidence.VERIFIED, profile.confidence)
        assertTrue(profile.normalization.unitNormalized)
        assertTrue(profile.normalization.realSizeCalibrated)
    }

    @Test
    fun centimeterMetadataConvertsToMeters() {
        val pipeline = pipeline(mapOf("cm.gltf" to gltfJson(
            floatArrayOf(0f, 0f, 0f), floatArrayOf(200f, 100f, 50f), metersPerUnit = 0.01f
        ).toByteArray(Charsets.UTF_8)))
        val result = pipeline.import(request("cm.gltf", realSizeHint = PhysicalSize(2f, 1f, 0.5f)))

        assertEquals(0.01f, result.profile!!.unitScaleApplied, 1e-6f)
        assertEquals(2f, result.profile!!.finalSize!!.width, 1e-4f)
        assertEquals(SizeConfidence.VERIFIED, result.profile!!.confidence)
    }

    @Test
    fun meterMetadataKeepsUnitScaleOne() {
        val pipeline = pipeline(mapOf("m.gltf" to gltfJson(
            floatArrayOf(0f, 0f, 0f), floatArrayOf(2f, 1f, 0.5f), metersPerUnit = 1f
        ).toByteArray(Charsets.UTF_8)))
        val result = pipeline.import(request("m.gltf"))

        assertEquals(1f, result.profile!!.unitScaleApplied, 1e-6f)
        assertEquals(2f, result.profile!!.meterBounds!!.width, 1e-5f)
    }

    // ── #4 无单位信息 → 规范默认 + 启发式提示 ─────────────────────────

    @Test
    fun glbWithoutUnitMetadataFallsBackToSpecMeter() {
        val pipeline = pipeline(mapOf("plain.glb" to glb(
            floatArrayOf(0f, 0f, 0f), floatArrayOf(2f, 1f, 0.5f)
        )))
        val result = pipeline.import(request("plain.glb", realSizeHint = PhysicalSize(2f, 1f, 0.5f)))

        assertNull(result.error)
        val profile = result.profile!!
        assertEquals(LengthUnit.METERS, profile.detectedUnit)
        assertEquals(UnitDetectionSource.FILE_METADATA, profile.unitSource)
        assertEquals(1f, profile.unitScaleApplied, 1e-6f)
        assertEquals(SizeConfidence.VERIFIED, profile.confidence)
    }

    @Test
    fun heuristicFlagsSuspiciousMillimeterChairWithoutSilentReinterpretation() {
        // 850 × 900 × 600 按米不合常理；按 cm 的 0.85m 命中座椅区间 → 仅告警，不重解释（§13）
        val pipeline = pipeline(mapOf("chair.glb" to glb(
            floatArrayOf(0f, 0f, 0f), floatArrayOf(850f, 900f, 600f)
        )))
        val result = pipeline.import(request("chair.glb", categoryHint = FurnitureCategory.SEAT))

        val profile = result.profile!!
        assertTrue(ImportWarningKind.UNIT_SUSPICIOUS in warningsOf(result))
        assertEquals(LengthUnit.METERS, profile.detectedUnit)
        assertEquals(1f, profile.unitScaleApplied, 1e-6f)
        assertEquals(850f, profile.meterBounds!!.width, 1e-4f)
        assertTrue(profile.validation.needsManualReview)
        assertEquals(SizeConfidence.TO_CALIBRATE, profile.confidence)
    }

    @Test
    fun unitHeuristicsResolveUniqueQuantitiesOnly() {
        assertEquals(LengthUnit.MILLIMETERS,
            UnitHeuristics.infer(850f, FurnitureCategory.SEAT))
        assertEquals(LengthUnit.CENTIMETERS,
            UnitHeuristics.infer(85f, FurnitureCategory.SEAT))
        assertEquals(LengthUnit.METERS,
            UnitHeuristics.infer(0.85f, FurnitureCategory.SEAT))
        // 2.0m 的灯具既不命中 m（上限 1.8）也不命中 cm/mm → 多解/无解返回 null
        assertNull(UnitHeuristics.infer(2.0f, FurnitureCategory.LIGHTING))
        assertNull(UnitHeuristics.infer(0f, FurnitureCategory.SEAT))
        assertNull(UnitHeuristics.infer(Float.NaN, FurnitureCategory.SEAT))
    }

    // ── #5/#6 真实尺寸校准与可信度 ────────────────────────────────────

    @Test
    fun knownRealSizeCalibratesUniformlyWithVerifiedConfidence() {
        val pipeline = pipeline(mapOf("desk.glb" to glb(
            floatArrayOf(0f, 0f, 0f), floatArrayOf(1f, 0.5f, 0.4f)
        )))
        val result = pipeline.import(request("desk.glb", realSizeHint = PhysicalSize(2f, 1f, 0.8f)))

        assertNull(result.error)
        val profile = result.profile!!
        assertEquals(2f, profile.realSizeScaleApplied, 1e-5f)
        assertEquals(2f, profile.finalSize!!.width, 1e-4f)
        assertEquals(1f, profile.finalSize!!.height, 1e-4f)
        assertEquals(0.8f, profile.finalSize!!.depth, 1e-4f)
        assertEquals(PhysicalSize(2f, 1f, 0.8f), profile.realSize)
        assertEquals(SizeConfidence.VERIFIED, profile.confidence)
        assertTrue(profile.ratioDeviationSafe())
        assertTrue(warningsOf(result).isEmpty())
    }

    @Test
    fun unknownRealSizeIsNeverGuessedAndMarksToCalibrate() {
        val pipeline = pipeline(mapOf("plain.glb" to glb(
            floatArrayOf(0f, 0f, 0f), floatArrayOf(2f, 1f, 0.5f)
        )))
        val result = pipeline.import(request("plain.glb"))

        val profile = result.profile!!
        assertNull(profile.realSize)
        assertEquals(RealSizeSource.NONE, profile.realSizeSource)
        assertEquals(1f, profile.realSizeScaleApplied, 1e-6f)
        assertEquals(profile.meterBounds, profile.finalSize)
        assertEquals(SizeConfidence.TO_CALIBRATE, profile.confidence)
        assertTrue(ImportWarningKind.UNCALIBRATED_SIZE in warningsOf(result))
    }

    // ── #7 比例异常：拒绝自动校准，不自动拉伸 ─────────────────────────

    @Test
    fun aspectMismatchOver25PercentIsRejectedWithoutStretching() {
        val pipeline = pipeline(mapOf("weird.glb" to glb(
            floatArrayOf(0f, 0f, 0f), floatArrayOf(1f, 1f, 1f)
        )))
        val result = pipeline.import(request("weird.glb", realSizeHint = PhysicalSize(2f, 2f, 0.5f)))

        val profile = result.profile!!
        assertTrue(ImportWarningKind.ASPECT_MISMATCH in warningsOf(result))
        assertEquals(1f, profile.realSizeScaleApplied, 1e-6f)
        // 未被拉伸：finalSize 保持米制边界而非 (2,2,0.5)
        assertEquals(1f, profile.finalSize!!.height, 1e-5f)
        assertEquals(1f, profile.finalSize!!.depth, 1e-5f)
        assertEquals(SizeConfidence.TO_CALIBRATE, profile.confidence)
        assertTrue(profile.validation.needsManualReview)
        assertEquals(false, profile.normalization.realSizeCalibrated)
    }

    @Test
    fun sizeOutOfRangeIsReportedButDoesNotBlockImport() {
        // 等比 5 倍放大到 10m 宽：比例无异常，但超出合理尺寸 → warning 转人工（§8.1）
        val pipeline = pipeline(mapOf("hall.glb" to glb(
            floatArrayOf(0f, 0f, 0f), floatArrayOf(2f, 1f, 0.5f)
        )))
        val result = pipeline.import(request("hall.glb", realSizeHint = PhysicalSize(10f, 5f, 2.5f)))

        val profile = result.profile!!
        assertTrue(ImportWarningKind.SIZE_OUT_OF_RANGE in warningsOf(result))
        assertEquals(10f, profile.finalSize!!.width, 1e-4f)
        assertEquals(5f, profile.realSizeScaleApplied, 1e-4f)
        assertTrue(profile.validation.needsManualReview)
        assertNull(result.error)
    }

    // ── #8 幂等：重复导入同一模型 ─────────────────────────────────────

    @Test
    fun duplicateImportReusesProfileWithoutReregistering() {
        val files = mapOf("dup.glb" to glb(floatArrayOf(0f, 0f, 0f), floatArrayOf(2f, 1f, 0.5f)))
        val registry = InMemoryAssetProfileRegistry()
        val pipeline = pipeline(files, registry)

        val first = pipeline.import(request("dup.glb"))
        val second = pipeline.import(request("dup.glb"))

        assertNull(first.error)
        assertNull(second.error)
        assertSame(first.profile, second.profile)
        assertEquals(1, registry.all().size)
        assertTrue(ImportWarningKind.DUPLICATE_REUSED in warningsOf(second))
        assertEquals("dup", second.profile!!.assetId)
    }

    // ── #9 重复 Scale 防护：unitNormalized / realSizeCalibrated 跳过 ──

    @Test
    fun unitNormalizedAssetSkipsUnitConversionOnReimport() {
        val registry = InMemoryAssetProfileRegistry(listOf(seedProfile(
            fileRef = "asset.glb",
            contentHash = "outdated-hash",
            unitScale = 0.01f,
            detectedUnit = LengthUnit.CENTIMETERS,
            unitSource = UnitDetectionSource.USER,
            unitNormalized = true,
            realSizeCalibrated = false,
            realSizeScale = 1f
        )))
        val pipeline = pipeline(mapOf("asset.glb" to gltfJson(
            floatArrayOf(0f, 0f, 0f), floatArrayOf(100f, 50f, 25f), metersPerUnit = 0.001f
        ).toByteArray(Charsets.UTF_8)), registry)

        val result = pipeline.import(request("asset.glb"))

        assertNull(result.error)
        val profile = result.profile!!
        assertTrue(ImportWarningKind.UNIT_NORMALIZATION_SKIPPED in warningsOf(result))
        // 不采用文件元数据的 0.001，也不叠加：保持既有 0.01
        assertEquals(0.01f, profile.unitScaleApplied, 1e-6f)
        assertEquals(1f, profile.meterBounds!!.width, 1e-4f) // 100 × 0.01
        assertEquals(0.5f, profile.meterBounds!!.height, 1e-4f)
    }

    @Test
    fun realSizeCalibratedAssetSkipsRecalibrationOnReimport() {
        val registry = InMemoryAssetProfileRegistry(listOf(seedProfile(
            fileRef = "asset.glb",
            contentHash = "outdated-hash",
            unitScale = 1f,
            detectedUnit = LengthUnit.METERS,
            unitSource = UnitDetectionSource.FILE_METADATA,
            unitNormalized = true,
            realSizeCalibrated = true,
            realSizeScale = 2f,
            realSize = PhysicalSize(2f, 1f, 0.8f)
        )))
        val pipeline = pipeline(mapOf("asset.glb" to glb(
            floatArrayOf(0f, 0f, 0f), floatArrayOf(1f, 0.5f, 0.4f)
        )), registry)

        val result = pipeline.import(request("asset.glb", realSizeHint = PhysicalSize(3f, 1.5f, 1.2f)))

        val profile = result.profile!!
        assertTrue(ImportWarningKind.CALIBRATION_SKIPPED in warningsOf(result))
        assertTrue(ImportWarningKind.UNIT_NORMALIZATION_SKIPPED in warningsOf(result))
        assertEquals(2f, profile.realSizeScaleApplied, 1e-6f)
        assertEquals(PhysicalSize(2f, 1f, 0.8f), profile.realSize)
        assertEquals(2f, profile.finalSize!!.width, 1e-5f)
        assertEquals(1f, profile.finalSize!!.height, 1e-5f)
    }

    // ── #10 UpAxis / Pivot 记录 ────────────────────────────────────────

    @Test
    fun upAxisAndPivotOffsetAreRecordedFromGeometry() {
        val pipeline = pipeline(mapOf("lamp.glb" to glb(gltfJson(
            floatArrayOf(0f, 0f, 0f), floatArrayOf(0.2f, 0.9f, 0.6f),
            translation = floatArrayOf(0f, -0.088f, 0f)
        ))))
        val result = pipeline.import(request("lamp.glb", categoryHint = FurnitureCategory.LIGHTING))

        val profile = result.profile!!
        assertEquals(UpAxis.Y_UP, profile.upAxis)
        assertEquals(PivotKind.BOTTOM_CENTER, profile.pivot)
        assertEquals(-0.088f, profile.pivotOffset.y, 1e-4f)
        assertEquals(0.9f, profile.meterBounds!!.height, 1e-4f)
    }

    @Test
    fun usdzMetadataUnitAndUpAxisAreReadFromUsda() {
        val pipeline = pipeline(mapOf("box.usdz" to usdz(
            "#usda 1.0\n(\n    metersPerUnit = 0.01\n    upAxis = \"Z\"\n)\n"
        )))
        val result = pipeline.import(request("box.usdz"))

        val profile = result.profile!!
        assertEquals(LengthUnit.CENTIMETERS, profile.detectedUnit)
        assertEquals(0.01f, profile.unitScaleApplied, 1e-6f)
        assertEquals(UpAxis.Z_UP, profile.upAxis)
        // 几何边界未解析 → 降级 + 待校准，不猜测（§8.2）
        assertTrue(ImportWarningKind.PARSE_INCOMPLETE in warningsOf(result))
        assertNull(profile.meterBounds)
        assertNull(profile.finalSize)
        assertEquals(SizeConfidence.TO_CALIBRATE, profile.confidence)
    }

    @Test
    fun usdzWithoutMetadataAssumesMetersAndMarksToCalibrate() {
        val pipeline = pipeline(mapOf("box.usdz" to usdz(
            "#usda 1.0\n(\n    upAxis = \"Y\"\n)\n"
        )))
        val result = pipeline.import(request("box.usdz"))

        val profile = result.profile!!
        assertNull(profile.detectedUnit)
        assertEquals(UnitDetectionSource.UNKNOWN, profile.unitSource)
        assertEquals(1f, profile.unitScaleApplied, 1e-6f)
        assertTrue(ImportWarningKind.UNIT_UNRESOLVED in warningsOf(result))
        assertTrue(ImportWarningKind.PARSE_INCOMPLETE in warningsOf(result))
        assertEquals(SizeConfidence.TO_CALIBRATE, profile.confidence)
    }

    // ── #11 文件 / 格式 / 解析异常 ────────────────────────────────────

    @Test
    fun missingFileReportsFileNotFound() {
        val result = pipeline(emptyMap()).import(request("gone.glb"))

        assertNull(result.profile)
        assertEquals(ImportErrorKind.FILE_NOT_FOUND, result.error!!.kind)
    }

    @Test
    fun unsupportedFormatListsSupportedOnesAndIsRejected() {
        val result = pipeline(mapOf("chair.obj" to "solid".toByteArray()))
            .import(request("chair.obj"))

        assertNull(result.profile)
        assertEquals(ImportErrorKind.FORMAT_UNSUPPORTED, result.error!!.kind)
        assertTrue(ModelFormats.SUPPORTED_LABEL in result.error!!.message)
    }

    @Test
    fun corruptGlbIsRejectedAsParseFailed() {
        val result = pipeline(mapOf("broken.glb" to "junk-not-a-model".toByteArray()))
            .import(request("broken.glb"))

        assertNull(result.profile)
        assertEquals(ImportErrorKind.PARSE_FAILED, result.error!!.kind)
    }

    @Test
    fun invalidBoundsAreRejectedInsteadOfSilentScaleOne() {
        // 1e999 → Infinity；JSON 中表达非法巨大边界
        val hugeJson = gltfJson(
            floatArrayOf(0f, 0f, 0f), floatArrayOf(1f, 1f, 1f)
        ).replace("\"max\":[1.0, 1.0, 1.0]", "\"max\":[1e999, 1.0, 1.0]")
        val files = mapOf(
            "huge.gltf" to hugeJson.toByteArray(Charsets.UTF_8),
            "degenerate.gltf" to gltfJson(
                floatArrayOf(1f, 1f, 1f), floatArrayOf(1f, 1f, 1f)
            ).toByteArray(Charsets.UTF_8)
        )
        val pipeline = pipeline(files)

        assertEquals(ImportErrorKind.PARSE_FAILED, pipeline.import(request("huge.gltf")).error!!.kind)
        assertEquals(ImportErrorKind.PARSE_FAILED, pipeline.import(request("degenerate.gltf")).error!!.kind)
    }

    // ── 声明冲突（§13：需要用户确认）─────────────────────────────────

    @Test
    fun declaredUnitConflictingWithSpecDefaultIsExecutedButFlagged() {
        val pipeline = pipeline(mapOf("cm.glb" to glb(
            floatArrayOf(0f, 0f, 0f), floatArrayOf(200f, 100f, 50f)
        )))
        val result = pipeline.import(
            request("cm.glb", declaredUnit = LengthUnit.CENTIMETERS, realSizeHint = PhysicalSize(2f, 1f, 0.5f))
        )

        val profile = result.profile!!
        assertTrue(ImportWarningKind.UNIT_CONFLICT in warningsOf(result))
        assertEquals(0.01f, profile.unitScaleApplied, 1e-6f)
        assertEquals(LengthUnit.CENTIMETERS, profile.detectedUnit)
        assertEquals(UnitDetectionSource.USER, profile.unitSource)
        // 用户声明单位不属于文件元数据 → 不能 VERIFIED（§4.4）
        assertEquals(SizeConfidence.ESTIMATED, profile.confidence)
    }

    @Test
    fun declaredUnitLosesAgainstFileMetadataWithConflictWarning() {
        val pipeline = pipeline(mapOf("mm.gltf" to gltfJson(
            floatArrayOf(0f, 0f, 0f), floatArrayOf(2000f, 1000f, 500f), metersPerUnit = 0.001f
        ).toByteArray(Charsets.UTF_8)))
        val result = pipeline.import(request("mm.gltf", declaredUnit = LengthUnit.CENTIMETERS))

        val profile = result.profile!!
        assertTrue(ImportWarningKind.UNIT_CONFLICT in warningsOf(result))
        assertEquals(0.001f, profile.unitScaleApplied, 1e-6f)
        assertEquals(LengthUnit.MILLIMETERS, profile.detectedUnit)
        assertEquals(LengthUnit.CENTIMETERS, profile.declaredUnit)
        assertTrue(profile.validation.needsManualReview)
    }

    private fun AssetProfile.ratioDeviationSafe(): Boolean =
        validation.ratioDeviation != null && validation.ratioDeviation < 0.25f
}
