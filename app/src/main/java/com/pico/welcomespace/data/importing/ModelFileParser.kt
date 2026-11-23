package com.pico.welcomespace.data.importing

import com.pico.welcomespace.domain.model.LengthUnit
import com.pico.welcomespace.domain.model.ModelFormat
import com.pico.welcomespace.domain.model.PhysicalSize
import com.pico.welcomespace.domain.model.UpAxis

/** §2.3 步骤 2：结构损坏 / 不合法内容抛出，由 Pipeline 归类为 PARSE_FAILED。 */
open class ModelParseException(message: String) : Exception(message)

/**
 * 解析结果：bounds 为原始单位下的 Bounding Box（宽= x 轴、高= y 轴、深= z 轴）。
 * complete=false 表示几何边界未能解析（如外部 bin 缺失、USDZ 二进制几何），
 * 按 §8.1 降级为 warning 而不是拒绝导入。
 */
data class ParsedModel(
    val format: ModelFormat,
    val bounds: PhysicalSize?,
    /** 原始单位下的 bbox min.y，用于记录 pivotOffset（复用 modelBaseY 语义）。 */
    val boundsMinY: Float?,
    val upAxis: UpAxis,
    /** 文件内显式单位元数据（如 EXT_model_units、usda metersPerUnit）；无则 null。 */
    val metadataUnit: LengthUnit?,
    val unitEvidence: String,
    /** 格式规范默认单位：glTF 家族为米（§13 决策 1 派生）；USDZ 无规范默认。 */
    val specDefaultUnit: LengthUnit?
) {
    val complete: Boolean get() = bounds != null
}

/** §2.3 步骤 2 的格式解析 seam。 */
fun interface ModelFileParser {
    fun parse(fileRef: String, content: ModelContent): ParsedModel
}

/** §13 决策 1：仅识别 GLB / GLTF / USDZ 扩展名。 */
object ModelFormats {
    const val SUPPORTED_LABEL = "GLB / GLTF / USDZ"

    fun detect(fileRef: String): ModelFormat? {
        val extension = fileRef.substringAfterLast('.', "").lowercase()
        return ModelFormat.entries.firstOrNull { it.fileExtension == extension }
    }
}
