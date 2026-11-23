package com.pico.welcomespace.data.importing

import com.pico.welcomespace.data.repository.FakeShopRepository
import com.pico.welcomespace.domain.model.FurnitureCategory
import com.pico.welcomespace.domain.model.PhysicalSize
import com.pico.welcomespace.domain.model.PivotOffset
import com.pico.welcomespace.domain.model.RealSizeSource
import com.pico.welcomespace.domain.model.SizeConfidence
import com.pico.welcomespace.domain.model.UnitDetectionSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * §7 存量 3 个模型的校准修正 + §9 #12「历史模型与场景兼容」：
 * profile 的实测边界与真实 .glb 几何一致、contentHash 与真实文件一致、
 * realSize 与 FakeShopRepository.physicalSize 逐字节一致（不改存量数值）、
 * 幂等重导入复用 seed 记录。
 */
class LegacyAssetProfilesTest {

    private class DirectoryResolver(private val root: Path) : ModelContentResolver {
        override fun resolve(fileRef: String): ModelContent? {
            val path = root.resolve(fileRef)
            if (!Files.isRegularFile(path)) return null
            return ModelContent(fileRef, Files.readAllBytes(path))
        }
    }

    private fun assetsDir(): Path {
        var dir: Path? = Paths.get(System.getProperty("user.dir")).toAbsolutePath()
        repeat(5) {
            val current = dir ?: return@repeat
            listOf(current.resolve("src/main/assets"), current.resolve("app/src/main/assets"))
                .firstOrNull { Files.exists(it.resolve("sofa.glb")) }
                ?.let { return it }
            dir = current.parent
        }
        throw IllegalStateException("未找到 src/main/assets（user.dir=${System.getProperty("user.dir")}）")
    }

    private val legacy = LegacyAssetProfiles.create()

    @Test
    fun legacyProfilesCoverTheThreeCatalogAssets() {
        val products = FakeShopRepository().products()
        val expected = mapOf(
            "sofa-02" to "sofa.glb",
            "chair-01" to "ArmChair_01.glb",
            "lamp-01" to "desk_lamp_arm_01.glb"
        )
        assertEquals(3, legacy.size)
        legacy.forEach { profile ->
            val product = products.first { it.id == profile.assetId }
            assertEquals(expected[profile.assetId], profile.fileRef)
            assertEquals(product.modelAsset, profile.fileRef)
            // §7.3：不动存量数值 —— realSize 与现有 physicalSize 完全一致
            assertEquals(product.physicalSize, profile.realSize)
        }
        // 单椅按 §1.2 记录为座椅类（商品目录中的 ShopCategory.SOFA 语义不变）
        assertEquals(FurnitureCategory.SEAT, legacy.first { it.assetId == "chair-01" }.category)
    }

    @Test
    fun meterBoundsMatchRealGlbGeometry() {
        val parser = GltfModelParser()
        val resolver = DirectoryResolver(assetsDir())
        legacy.forEach { profile ->
            val content = resolver.resolve(profile.fileRef)!!
            val parsed = parser.parse(profile.fileRef, content)
            assertTrue("解析应完整: ${profile.fileRef}", parsed.complete)
            val bounds = parsed.bounds!!
            assertEquals(profile.meterBounds!!.width, bounds.width, 1e-4f)
            assertEquals(profile.meterBounds!!.height, bounds.height, 1e-4f)
            assertEquals(profile.meterBounds!!.depth, bounds.depth, 1e-4f)
        }
    }

    @Test
    fun contentHashMatchesActualAssetBytes() {
        val resolver = DirectoryResolver(assetsDir())
        legacy.forEach { profile ->
            val bytes = resolver.resolve(profile.fileRef)!!.bytes
            assertEquals(profile.contentHash, sha256Hex(bytes))
        }
    }

    @Test
    fun pivotOffsetCarriesLegacyModelBaseY() {
        val byId = legacy.associateBy { it.assetId }
        assertEquals(PivotOffset(0f, 0f, 0f), byId.getValue("sofa-02").pivotOffset)
        assertEquals(PivotOffset(0f, 0f, 0f), byId.getValue("chair-01").pivotOffset)
        // 灯具原文件 min.y ≈ -0.088，对应原 ShopProduct.modelBaseY = -0.088f（§1.3 迁移）
        assertEquals(PivotOffset(0f, -0.088f, 0f), byId.getValue("lamp-01").pivotOffset)
    }

    @Test
    fun calibrationIsUniformAndDominantAxisMatchesRealSize() {
        val expectedScales = mapOf(
            "sofa-02" to 0.9999074f,
            "chair-01" to 0.9999177f,
            "lamp-01" to 1.0004361f
        )
        legacy.forEach { profile ->
            val scale = profile.realSizeScaleApplied
            assertEquals(expectedScales.getValue(profile.assetId), scale, 1e-5f)
            val final = profile.finalSize!!
            val real = profile.realSize!!
            // 主导轴（realSize 最大轴）精确对齐，其余轴等比
            val dominant = real.dominantAxisIndex()
            assertEquals(real.axis(dominant), final.axis(dominant), 1e-5f)
            assertTrue("比例偏差应远小于 25%", profile.validation.ratioDeviation!! < 0.01f)
            assertTrue(profile.validation.passed)
            // §7.2：存量资产无独立规格来源 → ESTIMATED；unitNormalized/realSizeCalibrated 均置位
            assertEquals(SizeConfidence.ESTIMATED, profile.confidence)
            assertEquals(UnitDetectionSource.FILE_METADATA, profile.unitSource)
            assertEquals(RealSizeSource.SPEC, profile.realSizeSource)
            assertTrue(profile.normalization.unitNormalized)
            assertTrue(profile.normalization.realSizeCalibrated)
        }
    }

    @Test
    fun pipelineReusesLegacySeedForIdenticalContent() {
        val registry = InMemoryAssetProfileRegistry(legacy)
        val pipeline = ModelImportPipeline(
            registry = registry,
            contentResolver = DirectoryResolver(assetsDir()),
            clock = { 1234L }
        )
        val result = pipeline.import(
            ModelImportRequest(source = com.pico.welcomespace.domain.model.AssetSource.LOCAL_ASSET, fileRef = "sofa.glb")
        )

        assertNull(result.error)
        // 与注册进 Registry 的必须是同一实例（create() 每次产生新对象）
        val seeded = legacy.first { it.fileRef == "sofa.glb" }
        assertSame(seeded, result.profile)
        assertTrue(ImportWarningKind.DUPLICATE_REUSED in result.warnings.map { it.kind })
        assertEquals(3, registry.all().size)
    }

    @Test
    fun freshImportOfLegacyAssetsReproducesRecordedBounds() {
        val registry = InMemoryAssetProfileRegistry()
        val pipeline = ModelImportPipeline(
            registry = registry,
            contentResolver = DirectoryResolver(assetsDir()),
            clock = { 1234L }
        )
        val products = FakeShopRepository().products()
        val byAssetId = legacy.associateBy { it.assetId }

        listOf("sofa-02", "chair-01", "lamp-01").forEach { assetId ->
            val profile = byAssetId.getValue(assetId)
            val product = products.first { it.id == assetId }
            val result = pipeline.import(
                ModelImportRequest(
                    source = com.pico.welcomespace.domain.model.AssetSource.LOCAL_ASSET,
                    fileRef = profile.fileRef,
                    categoryHint = profile.category,
                    realSizeHint = product.physicalSize
                )
            )
            assertNull("导入失败: ${result.error}", result.error)
            val fresh = result.profile!!
            assertEquals(SizeConfidence.VERIFIED, fresh.confidence)
            assertEquals(1f, fresh.unitScaleApplied, 1e-6f)
            assertEquals(profile.meterBounds!!.width, fresh.meterBounds!!.width, 1e-4f)
            assertEquals(profile.meterBounds!!.height, fresh.meterBounds!!.height, 1e-4f)
            assertEquals(profile.meterBounds!!.depth, fresh.meterBounds!!.depth, 1e-4f)
            assertEquals(profile.realSizeScaleApplied, fresh.realSizeScaleApplied, 1e-5f)
            assertEquals(profile.finalSize!!.width, fresh.finalSize!!.width, 1e-4f)
            assertEquals(profile.finalSize!!.height, fresh.finalSize!!.height, 1e-4f)
            assertEquals(profile.finalSize!!.depth, fresh.finalSize!!.depth, 1e-4f)
        }
    }
}
