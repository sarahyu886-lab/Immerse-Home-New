package com.pico.welcomespace.data.importing

import com.pico.welcomespace.domain.model.AssetProfile

/** §2.3 步骤 6 的「注册 Profile」seam；also §10.1 导入前先查重。 */
interface AssetProfileRegistry {
    fun findByFileRef(fileRef: String): AssetProfile?
    fun findByAssetId(assetId: String): AssetProfile?
    fun register(profile: AssetProfile)
    fun all(): List<AssetProfile>
}

/** 内存实现：本期 Phase 3 的库即进程内注册表，seed 来自 [LegacyAssetProfiles]（§7）。 */
class InMemoryAssetProfileRegistry(initial: List<AssetProfile> = emptyList()) : AssetProfileRegistry {
    private val profiles = LinkedHashMap<String, AssetProfile>()

    init {
        initial.forEach { profiles[it.assetId] = it }
    }

    override fun findByFileRef(fileRef: String): AssetProfile? =
        profiles.values.firstOrNull { it.fileRef == fileRef }

    override fun findByAssetId(assetId: String): AssetProfile? = profiles[assetId]

    override fun register(profile: AssetProfile) {
        profiles[profile.assetId] = profile
    }

    override fun all(): List<AssetProfile> = profiles.values.toList()
}
