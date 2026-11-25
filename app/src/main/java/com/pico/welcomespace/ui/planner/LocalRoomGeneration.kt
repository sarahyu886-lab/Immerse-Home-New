package com.pico.welcomespace.ui.planner

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.pico.welcomespace.domain.model.RoomOpening
import com.pico.welcomespace.domain.model.RoomOpeningKind
import com.pico.welcomespace.domain.model.RoomWall
import com.pico.welcomespace.domain.model.VirtualRoomPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.UUID

internal data class LocalRoomGenerationResult(
    val message: String,
    val taskId: String? = null,
    val roomEdited: Boolean = false
)

private data class SemanticRoomPlan(
    val assetPrompt: String,
    val anchor: String,
    val wallId: String?,
    val roomEdit: String?,
    val summary: String,
    val notes: List<String>
)

private const val TripoGatewayBaseUrl = "http://10.0.2.2:8787"
private const val SemanticGatewayUrl = "http://10.0.2.2:8798/plan"

internal suspend fun submitLocalRoomGeneration(
    context: Context,
    prompt: String,
    imageUri: Uri?,
    onProgress: (Int, String) -> Unit = { _, _ -> }
): LocalRoomGenerationResult = withContext(Dispatchers.IO) {
    suspend fun report(progress: Int, message: String) {
        withContext(Dispatchers.Main) {
            onProgress(progress.coerceIn(0, 100), message)
        }
    }

    val trimmedPrompt = prompt.trim()
    require(trimmedPrompt.isNotEmpty() || imageUri != null) { "请先输入描述或上传参考图" }

    report(8, "正在分析你的描述")
    val semanticPlan = runCatching {
        requestSemanticPlan(trimmedPrompt, imageUri != null)
    }.getOrElse {
        fallbackSemanticPlan(trimmedPrompt, imageUri != null)
    }

    report(28, "正在整理房间约束")
    val updatedPlan = applySemanticEdit(RoomPlannerSession.activePlan, semanticPlan)
    if (updatedPlan != null) {
        RoomPlannerSession.applyPlan(updatedPlan)
        report(44, "已更新 4×5 房间预览")
    } else {
        report(44, "当前请求不改动房间轮廓")
    }

    val taskResult = runCatching {
        if (imageUri != null) {
            report(60, "正在上传参考图")
            val fileToken = uploadReferenceImage(context, imageUri)
            report(76, "正在提交 Tripo 图生模任务")
            createTripoTask(
                body = JSONObject().apply {
                    put("mode", "image_to_model")
                    put("file_token", fileToken)
                    if (trimmedPrompt.isNotEmpty()) put("prompt", semanticPlan.assetPrompt)
                }
            )
        } else {
            report(76, "正在提交 Tripo 文生模任务")
            createTripoTask(
                body = JSONObject().apply {
                    put("mode", "text_to_model")
                    put("prompt", semanticPlan.assetPrompt.ifBlank { trimmedPrompt })
                }
            )
        }
    }

    val roomMessage = if (updatedPlan != null) {
        "已按房间约束更新左侧 4×5 预览。"
    } else {
        "当前请求未改动房间轮廓。"
    }
    val notes = semanticPlan.notes.joinToString("；")
    taskResult.fold(
        onSuccess = { taskId ->
            runCatching { withContext(Dispatchers.Main) { onProgress(100, "已提交完成") } }
            LocalRoomGenerationResult(
                message = buildString {
                    append(roomMessage)
                    append(semanticPlan.summary)
                    append("已提交 Tripo 任务：")
                    append(taskId)
                    if (notes.isNotBlank()) {
                        append("。")
                        append(notes)
                    }
                },
                taskId = taskId,
                roomEdited = updatedPlan != null
            )
        },
        onFailure = { error ->
            LocalRoomGenerationResult(
                message = buildString {
                    append(roomMessage)
                    append(semanticPlan.summary)
                    append("未能提交 Tripo 任务：")
                    append(error.message ?: "请检查本机桥接服务、令牌与网络")
                    if (notes.isNotBlank()) {
                        append("。")
                        append(notes)
                    }
                },
                roomEdited = updatedPlan != null
            )
        }
    )
}

private fun requestSemanticPlan(
    prompt: String,
    hasImage: Boolean
): SemanticRoomPlan {
    if (prompt.isBlank()) return fallbackSemanticPlan(prompt, hasImage)
    val currentPlan = RoomPlannerSession.activePlan
    val response = postJson(
        url = SemanticGatewayUrl,
        body = JSONObject().apply {
            put("prompt", prompt)
            put("has_image", hasImage)
            put("room", JSONObject().apply {
                put("name", currentPlan.name)
                put("width_meters", currentPlan.widthMeters)
                put("depth_meters", currentPlan.depthMeters)
                put("ceiling_height", currentPlan.ceilingHeight)
            })
        }
    )
    return SemanticRoomPlan(
        assetPrompt = response.optString("asset_prompt").ifBlank { prompt },
        anchor = response.optString("anchor").ifBlank { fallbackSemanticPlan(prompt, hasImage).anchor },
        wallId = response.optString("wall_id").ifBlank { null },
        roomEdit = response.optString("room_edit").ifBlank { null },
        summary = response.optString("summary").ifBlank { fallbackSemanticPlan(prompt, hasImage).summary },
        notes = response.optJSONArray("notes").toStringList()
    )
}

private fun fallbackSemanticPlan(prompt: String, hasImage: Boolean): SemanticRoomPlan {
    val lower = prompt.lowercase(Locale.ROOT)
    val wallId = inferWallId(lower)
    return when {
        lower.contains("窗") || lower.contains("window") -> SemanticRoomPlan(
            assetPrompt = prompt.ifBlank { "simple wall window frame" },
            anchor = "wall",
            wallId = wallId,
            roomEdit = "add_window",
            summary = "窗户请求已先约束到${wallLabel(wallId)}。",
            notes = listOf("未连到语义模型时使用本地关键词兜底")
        )
        lower.contains("吊灯") || lower.contains("吸顶灯") || lower.contains("ceiling light") ||
            lower.contains("chandelier") || (lower.contains("灯") && !lower.contains("落地灯")) -> SemanticRoomPlan(
            assetPrompt = prompt.ifBlank { "ceiling mounted lamp" },
            anchor = "ceiling",
            wallId = null,
            roomEdit = null,
            summary = "灯具请求已先约束到天花板。",
            notes = listOf("未连到语义模型时使用本地关键词兜底")
        )
        hasImage -> SemanticRoomPlan(
            assetPrompt = prompt.ifBlank { "reference image guided furniture model" },
            anchor = "floor",
            wallId = null,
            roomEdit = null,
            summary = "将按参考图优先发起图生模任务。",
            notes = listOf("Tripo 公共图生模接口以图片为主，不会把自然语言直接并入同一请求")
        )
        else -> SemanticRoomPlan(
            assetPrompt = prompt,
            anchor = "floor",
            wallId = null,
            roomEdit = null,
            summary = "将按文本发起模型生成。",
            notes = listOf("更复杂的空间约束建议补充语义模型")
        )
    }
}

private fun applySemanticEdit(plan: VirtualRoomPlan, semanticPlan: SemanticRoomPlan): VirtualRoomPlan? {
    if (semanticPlan.roomEdit != "add_window") return null
    val wallId = semanticPlan.wallId ?: "north"
    val replacement = createWindowForWall(plan.walls.firstOrNull { it.id == wallId } ?: return null)
    return plan.copy(
        openings = plan.openings.filterNot { it.id == replacement.id } + replacement
    )
}

private fun createWindowForWall(wall: RoomWall): RoomOpening {
    val horizontal = wall.width >= wall.depth
    return RoomOpening(
        id = "ai-window-${wall.id}",
        label = "AI 窗户",
        kind = RoomOpeningKind.WINDOW,
        centerX = wall.centerX,
        centerZ = wall.centerZ,
        width = if (horizontal) 1.2f else wall.width,
        depth = if (horizontal) wall.depth else 1.2f,
        heightMeters = 1.2f,
        sillMeters = 0.9f
    )
}

private fun inferWallId(lowerPrompt: String): String = when {
    lowerPrompt.contains("东") || lowerPrompt.contains("east") || lowerPrompt.contains("右") -> "east"
    lowerPrompt.contains("西") || lowerPrompt.contains("west") || lowerPrompt.contains("左") -> "west"
    lowerPrompt.contains("南") || lowerPrompt.contains("south") || lowerPrompt.contains("前") -> "south"
    else -> "north"
}

private fun wallLabel(wallId: String?): String = when (wallId) {
    "east" -> "东墙"
    "west" -> "西墙"
    "south" -> "南墙"
    else -> "北墙"
}

private fun uploadReferenceImage(context: Context, imageUri: Uri): String {
    val fileName = queryDisplayName(context, imageUri)
    val mimeType = context.contentResolver.getType(imageUri) ?: "image/png"
    val bytes = context.contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
        ?: throw IllegalStateException("无法读取参考图")
    val boundary = "Boundary-${UUID.randomUUID()}"
    val connection = openConnection("$TripoGatewayBaseUrl/upload", "POST").apply {
        setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
    }
    connection.outputStream.use { output ->
        val head = buildString {
            append("--$boundary\r\n")
            append("Content-Disposition: form-data; name=\"file\"; filename=\"")
            append(fileName)
            append("\"\r\n")
            append("Content-Type: $mimeType\r\n\r\n")
        }.toByteArray()
        output.write(head)
        output.write(bytes)
        output.write("\r\n--$boundary--\r\n".toByteArray())
    }
    val response = readJson(connection)
    return response.optJSONObject("data")?.optString("file_token")
        ?.takeIf { it.isNotBlank() }
        ?: throw IllegalStateException("上传成功但未返回 file_token")
}

private fun createTripoTask(body: JSONObject): String {
    val response = postJson(
        url = "$TripoGatewayBaseUrl/tasks",
        body = body,
        confirmGeneration = true
    )
    return response.optJSONObject("data")?.optString("task_id")
        ?.takeIf { it.isNotBlank() }
        ?: throw IllegalStateException("任务已提交但未返回 task_id")
}

private fun postJson(
    url: String,
    body: JSONObject,
    confirmGeneration: Boolean = false
): JSONObject {
    val connection = openConnection(url, "POST").apply {
        setRequestProperty("Content-Type", "application/json")
        if (confirmGeneration) setRequestProperty("X-Confirm-Generation", "yes")
    }
    connection.outputStream.use { stream ->
        stream.write(body.toString().toByteArray())
    }
    return readJson(connection)
}

private fun openConnection(url: String, method: String): HttpURLConnection =
    (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = method
        connectTimeout = 15000
        readTimeout = 60000
        doInput = true
        doOutput = method != "GET"
        setRequestProperty("Accept", "application/json")
    }

private fun readJson(connection: HttpURLConnection): JSONObject {
    val status = connection.responseCode
    val raw = try {
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        stream?.use { it.readBytes() } ?: ByteArray(0)
    } finally {
        connection.disconnect()
    }
    val body = raw.toString(Charsets.UTF_8)
    if (status !in 200..299) {
        val serverMessage = runCatching { JSONObject(body).optString("error") }.getOrNull().orEmpty()
        throw IllegalStateException(
            listOf("HTTP $status", serverMessage)
                .filter { it.isNotBlank() }
                .joinToString(" · ")
                .ifBlank { "本机桥接服务不可用" }
        )
    }
    return JSONObject(body)
}

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return buildList(length()) {
        for (index in 0 until length()) {
            optString(index).takeIf { it.isNotBlank() }?.let(::add)
        }
    }
}

private fun queryDisplayName(context: Context, uri: Uri): String {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) {
                cursor.getString(index)?.takeIf { it.isNotBlank() }?.let { return it }
            }
        }
    }
    return uri.lastPathSegment?.substringAfterLast('/') ?: "reference.png"
}
