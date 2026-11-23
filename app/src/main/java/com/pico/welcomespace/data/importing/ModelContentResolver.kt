package com.pico.welcomespace.data.importing

import android.content.res.AssetManager
import java.io.FileNotFoundException
import java.io.IOException
import java.security.MessageDigest

/** §2.3 步骤 1「模型获取」的文件读取 seam：返回 null 表示文件不存在，抛 IOException 表示不可读。 */
fun interface ModelContentResolver {
    fun resolve(fileRef: String): ModelContent?
}

class ModelContent(val fileRef: String, val bytes: ByteArray)

/** 生产实现：从 APK assets 读取。 */
class AssetModelContentResolver(private val assets: AssetManager) : ModelContentResolver {
    override fun resolve(fileRef: String): ModelContent? = try {
        assets.open(fileRef).use { stream ->
            ModelContent(fileRef, stream.readBytes())
        }
    } catch (_: FileNotFoundException) {
        null
    }
}

/** §2.4 内容摘要：相同 fileRef + 相同内容 → 不重复入库。 */
internal fun sha256Hex(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { "%02x".format(it) }
