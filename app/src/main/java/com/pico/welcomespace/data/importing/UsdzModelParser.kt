package com.pico.welcomespace.data.importing

import com.pico.welcomespace.domain.model.LengthUnit
import com.pico.welcomespace.domain.model.ModelFormat
import com.pico.welcomespace.domain.model.UpAxis
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

/**
 * USDZ 解析器（本期范围：容器校验 + usda 层元数据 metersPerUnit / upAxis）。
 * 二进制 usdc 几何与嵌套引用不在纯 JVM 解析范围（§14 第三阶段边界），
 * bounds 返回 null → 走 TO_CALIBRATE 降级，不猜测尺寸（§8.2）。
 */
class UsdzModelParser : ModelFileParser {

    override fun parse(fileRef: String, content: ModelContent): ParsedModel {
        val bytes = content.bytes
        if (!isZipContainer(bytes)) throw ModelParseException("USDZ 不是合法的 zip 容器: $fileRef")

        var metadataUnit: LengthUnit? = null
        var upAxis = UpAxis.UNKNOWN
        var sawUsda = false
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.name.endsWith(".usda")) {
                    sawUsda = true
                    val text = zip.readBytes().toString(Charsets.UTF_8)
                    METERS_PER_UNIT.find(text)?.let { match ->
                        val value = match.groupValues[1].toFloatOrNull()
                        metadataUnit = if (value != null) LengthUnit.fromMetersPerUnit(value) else null
                    }
                    UP_AXIS.find(text)?.let { match ->
                        upAxis = when (match.groupValues[1].uppercase()) {
                            "Y" -> UpAxis.Y_UP
                            "Z" -> UpAxis.Z_UP
                            else -> UpAxis.UNKNOWN
                        }
                    }
                    break
                }
            }
        }

        return ParsedModel(
            format = ModelFormat.USDZ,
            bounds = null,
            boundsMinY = null,
            upAxis = upAxis,
            metadataUnit = metadataUnit,
            unitEvidence = if (metadataUnit != null) "usda metersPerUnit" else if (sawUsda) "usda 存在但无 metersPerUnit" else "USDZ 未找到 usda 层",
            specDefaultUnit = null
        )
    }

    private fun isZipContainer(bytes: ByteArray): Boolean =
        bytes.size >= 4 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte() &&
            bytes[2] == 3.toByte() && bytes[3] == 4.toByte()

    private companion object {
        val METERS_PER_UNIT = Regex("""metersPerUnit\s*=\s*([0-9.+\-eE]+)""")
        val UP_AXIS = Regex("""upAxis\s*=\s*"([A-Za-z]+)"""")
    }
}
