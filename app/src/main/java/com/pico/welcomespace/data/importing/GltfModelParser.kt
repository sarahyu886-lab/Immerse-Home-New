package com.pico.welcomespace.data.importing

import com.pico.welcomespace.domain.model.LengthUnit
import com.pico.welcomespace.domain.model.ModelFormat
import com.pico.welcomespace.domain.model.PhysicalSize
import com.pico.welcomespace.domain.model.UpAxis
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64

/**
 * glTF 家族解析器（GLB 二进制容器 + .gltf JSON 文本），纯 Kotlin/JVM 实现，无第三方依赖。
 * - GLB：读 binary chunk header（magic/version/chunk 类型），取出 JSON chunk 与可选 BIN chunk
 * - 边界：优先用 POSITION accessor 自带的 min/max；缺失时从 BIN/data-URI 缓冲读取顶点
 * - 单位元数据：识别根扩展 EXT_model_units.metersPerUnit；否则按 glTF 2.0 规范默认米（§13）
 * - upAxis：glTF 恒为 Y_UP
 */
class GltfModelParser : ModelFileParser {

    override fun parse(fileRef: String, content: ModelContent): ParsedModel {
        return if (isGlb(content.bytes)) {
            parseGlb(fileRef, content.bytes)
        } else {
            parseGltfText(fileRef, content.bytes)
        }
    }

    private fun isGlb(bytes: ByteArray): Boolean =
        bytes.size >= 12 && bytes[0] == 'g'.code.toByte() && bytes[1] == 'l'.code.toByte() &&
            bytes[2] == 'T'.code.toByte() && bytes[3] == 'F'.code.toByte()

    private fun parseGlb(fileRef: String, bytes: ByteArray): ParsedModel {
        if (bytes.size < 20) throw ModelParseException("GLB 文件过短")
        val version = readUint32(bytes, 4)
        if (version != 2) throw ModelParseException("仅支持 glTF 2.0（GLB version=$version）")
        var offset = 12
        var jsonText: String? = null
        var bin: ByteArray? = null
        while (offset + 8 <= bytes.size) {
            val chunkLength = readUint32(bytes, offset)
            val chunkType = readUint32(bytes, offset + 4)
            val start = offset + 8
            if (start + chunkLength > bytes.size) throw ModelParseException("GLB chunk 越界")
            when (chunkType) {
                CHUNK_JSON -> jsonText = String(bytes, start, chunkLength, Charsets.UTF_8)
                CHUNK_BIN -> bin = bytes.copyOfRange(start, start + chunkLength)
            }
            offset = start + chunkLength
        }
        val json = jsonText ?: throw ModelParseException("GLB 缺少 JSON chunk")
        return build(fileRef, MiniJson.parse(json), ModelFormat.GLB, bin)
    }

    private fun parseGltfText(fileRef: String, bytes: ByteArray): ParsedModel {
        val json = MiniJson.parse(String(bytes, Charsets.UTF_8))
        return build(fileRef, json, ModelFormat.GLTF, null)
    }

    private fun build(fileRef: String, root: Any?, format: ModelFormat, glbBin: ByteArray?): ParsedModel {
        val json = root as? Map<*, *> ?: throw ModelParseException("glTF JSON 结构非法: $fileRef")
        val nodes = json["nodes"] as? List<*> ?: emptyList<Any>()
        val meshes = json["meshes"] as? List<*> ?: emptyList<Any>()
        val accessors = json["accessors"] as? List<*> ?: emptyList<Any>()
        val bufferViews = json["bufferViews"] as? List<*> ?: emptyList<Any>()
        val buffers = json["buffers"] as? List<*> ?: emptyList<Any>()

        val accumulator = BoundsAccumulator()
        val visited = HashSet<Int>()
        val context = BufferContext(buffers, bufferViews, glbBin)
        for (rootIndex in sceneRoots(json, nodes.size)) {
            collectBounds(rootIndex, nodes, meshes, accessors, context, identityMatrix(), visited, accumulator)
        }

        val metadataUnit = explicitMetadataUnit(json)
        val evidence = if (metadataUnit != null) {
            "EXT_model_units.metersPerUnit"
        } else {
            "glTF 2.0 specification (meter default)"
        }
        return ParsedModel(
            format = format,
            bounds = accumulator.size(),
            boundsMinY = if (accumulator.found) accumulator.minY else null,
            upAxis = UpAxis.Y_UP,
            metadataUnit = metadataUnit,
            unitEvidence = evidence,
            specDefaultUnit = LengthUnit.METERS
        )
    }

    private fun sceneRoots(json: Map<*, *>, nodeCount: Int): List<Int> {
        val scenes = json["scenes"] as? List<*>
        val scene = scenes?.getOrNull(((json["scene"] as? Double) ?: 0.0).toInt()) as? Map<*, *>
        val declared = (scene?.get("nodes") as? List<*>)
            ?.mapNotNull { (it as? Double)?.toInt() }
            .orEmpty()
        return declared.ifEmpty { (0 until nodeCount).toList() }
    }

    private fun explicitMetadataUnit(json: Map<*, *>): LengthUnit? {
        val extensions = json["extensions"] as? Map<*, *> ?: return null
        val unitExtension = extensions["EXT_model_units"] as? Map<*, *> ?: return null
        val metersPerUnit = (unitExtension["metersPerUnit"] as? Double)?.toFloat() ?: return null
        return LengthUnit.fromMetersPerUnit(metersPerUnit)
    }

    private fun collectBounds(
        nodeIndex: Int,
        nodes: List<*>,
        meshes: List<*>,
        accessors: List<*>,
        context: BufferContext,
        parentMatrix: FloatArray,
        visited: MutableSet<Int>,
        accumulator: BoundsAccumulator
    ) {
        if (!visited.add(nodeIndex)) return
        val node = nodes.getOrNull(nodeIndex) as? Map<*, *> ?: return
        val worldMatrix = multiply(parentMatrix, nodeMatrix(node))
        val meshIndex = (node["mesh"] as? Double)?.toInt()
        if (meshIndex != null) {
            val mesh = meshes.getOrNull(meshIndex) as? Map<*, *>
            val primitives = mesh?.get("primitives") as? List<*> ?: emptyList<Any>()
            for (primitive in primitives) {
                val attributes = (primitive as? Map<*, *>)?.get("attributes") as? Map<*, *> ?: continue
                val positionAccessorIndex = (attributes["POSITION"] as? Double)?.toInt() ?: continue
                addAccessorBounds(positionAccessorIndex, accessors, context, worldMatrix, accumulator)
            }
        }
        val children = node["children"] as? List<*> ?: emptyList<Any>()
        for (child in children) {
            val childIndex = (child as? Double)?.toInt() ?: continue
            collectBounds(childIndex, nodes, meshes, accessors, context, worldMatrix, visited, accumulator)
        }
    }

    private fun addAccessorBounds(
        accessorIndex: Int,
        accessors: List<*>,
        context: BufferContext,
        worldMatrix: FloatArray,
        accumulator: BoundsAccumulator
    ) {
        val accessor = accessors.getOrNull(accessorIndex) as? Map<*, *> ?: return
        val declaredMin = accessor.floatTriple("min")
        val declaredMax = accessor.floatTriple("max")
        if (declaredMin != null && declaredMax != null) {
            accumulator.addBox(declaredMin, declaredMax, worldMatrix)
            return
        }
        // accessor 未带 min/max：回退到直接读取 POSITION 顶点（5126 FLOAT / VEC3）
        val positions = readPositions(accessor, context) ?: return
        for (i in positions.indices step 3) {
            accumulator.addPoint(
                transformPoint(worldMatrix, positions[i], positions[i + 1], positions[i + 2])
            )
        }
    }

    private fun readPositions(accessor: Map<*, *>, context: BufferContext): FloatArray? {
        if ((accessor["componentType"] as? Double)?.toInt() != 5126) return null
        if (accessor["type"] != "VEC3") return null
        val count = (accessor["count"] as? Double)?.toInt() ?: return null
        val viewIndex = (accessor["bufferView"] as? Double)?.toInt() ?: return null
        val view = context.bufferViews.getOrNull(viewIndex) as? Map<*, *> ?: return null
        val bufferIndex = (view["buffer"] as? Double)?.toInt() ?: return null
        val buffer = context.bufferData(bufferIndex) ?: return null
        val viewOffset = (view["byteOffset"] as? Double)?.toInt() ?: 0
        val stride = (view["byteStride"] as? Double)?.toInt() ?: ELEMENT_BYTES
        val accessorOffset = (accessor["byteOffset"] as? Double)?.toInt() ?: 0
        if (stride < ELEMENT_BYTES) return null
        val result = FloatArray(count * 3)
        for (i in 0 until count) {
            val base = viewOffset + accessorOffset + i * stride
            if (base < 0 || base + ELEMENT_BYTES > buffer.size) return null
            result[i * 3] = littleEndianFloat(buffer, base)
            result[i * 3 + 1] = littleEndianFloat(buffer, base + 4)
            result[i * 3 + 2] = littleEndianFloat(buffer, base + 8)
        }
        return result
    }

    private fun littleEndianFloat(buffer: ByteArray, offset: Int): Float =
        ByteBuffer.wrap(buffer, offset, 4).order(ByteOrder.LITTLE_ENDIAN).float

    private fun nodeMatrix(node: Map<*, *>): FloatArray {
        val matrix = node["matrix"] as? List<*>
        if (matrix != null && matrix.size == 16) {
            return FloatArray(16) { i -> (matrix[i] as? Double)?.toFloat() ?: IDENTITY[i] }
        }
        val translation = node.floatTriple("translation") ?: floatArrayOf(0f, 0f, 0f)
        val rotation = node.floatQuad("rotation") ?: floatArrayOf(0f, 0f, 0f, 1f)
        val scale = node.floatTriple("scale") ?: floatArrayOf(1f, 1f, 1f)
        return trsMatrix(translation, rotation, scale)
    }

    /** TRS → 列主序 4x4（glTF 约定：M = T · R · S，四元数为 x,y,z,w）。 */
    private fun trsMatrix(t: FloatArray, q: FloatArray, s: FloatArray): FloatArray {
        val x = q[0]; val y = q[1]; val z = q[2]; val w = q[3]
        val xx = x * x; val yy = y * y; val zz = z * z
        val xy = x * y; val xz = x * z; val yz = y * z
        val wx = w * x; val wy = w * y; val wz = w * z
        val m = FloatArray(16)
        m[0] = (1f - (yy + zz)) * s[0]
        m[1] = (xy + wz) * s[0]
        m[2] = (xz - wy) * s[0]
        m[4] = (xy - wz) * s[1]
        m[5] = (1f - (xx + zz)) * s[1]
        m[6] = (yz + wx) * s[1]
        m[8] = (xz + wy) * s[2]
        m[9] = (yz - wx) * s[2]
        m[10] = (1f - (xx + yy)) * s[2]
        m[12] = t[0]; m[13] = t[1]; m[14] = t[2]; m[15] = 1f
        return m
    }

    private fun multiply(a: FloatArray, b: FloatArray): FloatArray {
        val result = FloatArray(16)
        for (column in 0 until 4) {
            for (row in 0 until 4) {
                var sum = 0f
                for (k in 0 until 4) sum += a[k * 4 + row] * b[column * 4 + k]
                result[column * 4 + row] = sum
            }
        }
        return result
    }

    private fun transform(m: FloatArray, x: Float, y: Float, z: Float): FloatArray = transformPoint(m, x, y, z)

    private fun identityMatrix() = IDENTITY.copyOf()

    private fun readUint32(bytes: ByteArray, offset: Int): Int {
        var value = 0
        for (i in 0 until 4) value = value or ((bytes[offset + i].toInt() and 0xFF) shl (i * 8))
        return value
    }

    private fun Map<*, *>.floatTriple(key: String): FloatArray? {
        val list = this[key] as? List<*> ?: return null
        if (list.size != 3) return null
        val result = FloatArray(3)
        for (i in 0 until 3) {
            val value = list[i] as? Double ?: return null
            result[i] = value.toFloat()
        }
        return result
    }

    private fun Map<*, *>.floatQuad(key: String): FloatArray? {
        val list = this[key] as? List<*> ?: return null
        if (list.size != 4) return null
        val result = FloatArray(4)
        for (i in 0 until 4) {
            val value = list[i] as? Double ?: return null
            result[i] = value.toFloat()
        }
        return result
    }

    private class BufferContext(
        val buffers: List<*>,
        val bufferViews: List<*>,
        val glbBin: ByteArray?
    ) {
        private val cache = HashMap<Int, ByteArray?>()

        fun bufferData(index: Int): ByteArray? {
            if (cache.containsKey(index)) return cache[index]
            val buffer = buffers.getOrNull(index) as? Map<*, *>
            val value = when (val uri = buffer?.get("uri") as? String) {
                null -> glbBin
                else -> decodeDataUri(uri)
            }
            cache[index] = value
            return value
        }

        private fun decodeDataUri(uri: String): ByteArray? {
            val marker = "base64,"
            val position = uri.indexOf(marker)
            if (!uri.startsWith("data:") || position < 0) return null
            return try {
                Base64.getDecoder().decode(uri.substring(position + marker.length))
            } catch (_: IllegalArgumentException) {
                null
            }
        }
    }

    private class BoundsAccumulator {
        var minX = Float.MAX_VALUE
            private set
        var minY = Float.MAX_VALUE
            private set
        var minZ = Float.MAX_VALUE
            private set
        var maxX = -Float.MAX_VALUE
            private set
        var maxY = -Float.MAX_VALUE
            private set
        var maxZ = -Float.MAX_VALUE
            private set
        var found = false
            private set

        fun addBox(min: FloatArray, max: FloatArray, matrix: FloatArray) {
            val xs = floatArrayOf(min[0], max[0])
            val ys = floatArrayOf(min[1], max[1])
            val zs = floatArrayOf(min[2], max[2])
            for (x in xs) {
                for (y in ys) {
                    for (z in zs) {
                        addPoint(transformPoint(matrix, x, y, z))
                    }
                }
            }
        }

        fun addPoint(point: FloatArray) {
            found = true
            if (point[0] < minX) minX = point[0]
            if (point[1] < minY) minY = point[1]
            if (point[2] < minZ) minZ = point[2]
            if (point[0] > maxX) maxX = point[0]
            if (point[1] > maxY) maxY = point[1]
            if (point[2] > maxZ) maxZ = point[2]
        }

        fun size(): PhysicalSize? = if (!found) {
            null
        } else {
            PhysicalSize(maxX - minX, maxY - minY, maxZ - minZ)
        }
    }

    companion object {
        private const val CHUNK_JSON = 0x4E4F534A // "JSON"
        private const val CHUNK_BIN = 0x004E4942 // "BIN\0"
        private const val ELEMENT_BYTES = 12 // VEC3 FLOAT
        private val IDENTITY = floatArrayOf(
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f
        )
    }
}

/** 列主序 4x4 矩阵变换点（顶层函数：供嵌套类 BoundsAccumulator 复用）。 */
internal fun transformPoint(m: FloatArray, x: Float, y: Float, z: Float): FloatArray {
    val rx = m[0] * x + m[4] * y + m[8] * z + m[12]
    val ry = m[1] * x + m[5] * y + m[9] * z + m[13]
    val rz = m[2] * x + m[6] * y + m[10] * z + m[14]
    val rw = m[3] * x + m[7] * y + m[11] * z + m[15]
    return when {
        // 仿射矩阵 rw 恒为 1；病态输入（如 0×Inf=NaN）或 rw=0 时跳过齐次除法，
        // 让非有限值原样进入边界累积，由上游 isValidBounds 拒绝（§2.3 步骤 6）
        rw == 1f || rw.isNaN() || rw == 0f -> floatArrayOf(rx, ry, rz)
        else -> floatArrayOf(rx / rw, ry / rw, rz / rw)
    }
}
