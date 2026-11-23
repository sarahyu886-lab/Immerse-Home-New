package com.pico.welcomespace.ui.planner

import com.pico.welcomespace.domain.model.FloorPlanRoom
import com.pico.welcomespace.domain.model.RoomOpening
import com.pico.welcomespace.domain.model.RoomOpeningKind
import com.pico.welcomespace.domain.model.RoomWall
import com.pico.welcomespace.domain.model.VirtualRoomPlan
import com.pico.welcomespace.domain.usecase.FloorPlanInputSource
import kotlin.math.abs

/** 画布平面坐标点（户型坐标系，米）。 */
data class PlanPoint(val x: Float, val z: Float)

enum class PlannerStep(val title: String) {
    UPLOAD("上传你的户型"),
    SELECT_SOURCE("选择户型来源"),
    PROCESSING("正在处理户型"),
    CALIBRATE("校准真实尺寸"),
    EDIT("直接在图上编辑"),
    GENERATING("生成模拟房间"),
    RESULT("模拟房间已就绪"),
    ERROR("异常与恢复")
}

enum class PlannerTool(val label: String) {
    WALL("墙体"),
    DOOR("门"),
    WINDOW("窗")
}

enum class PlannerSelectionType {
    WALL,
    DOOR,
    WINDOW
}

data class PlannerSelection(
    val id: String,
    val type: PlannerSelectionType
)

/** P15–P19 模态弹窗（文案与 Figma 一致）。 */
enum class PlannerDialog {
    REPLACE_CURRENT_PLAN,
    RESTORE_DRAFT,
    SAVE_FAILED,
    OFFLINE,
    INVALID_DOOR_SIZE
}

/** P10 问题类型：墙体未闭合 / 构件重叠。 */
enum class PlannerIssueKind(val label: String) {
    OPEN_EDGE("墙体未闭合"),
    OVERLAP("门窗发生重叠")
}

data class PlanIssue(
    val kind: PlannerIssueKind,
    val message: String,
    val target: PlannerSelection?
)

data class RoomPlannerUiState(
    val step: PlannerStep = PlannerStep.UPLOAD,
    val tool: PlannerTool = PlannerTool.WALL,
    val lastInputSource: FloorPlanInputSource? = null,
    val generatedPlan: VirtualRoomPlan = FloorPlanRoom.demoPlan,
    val selected: PlannerSelection? = null,
    val dialog: PlannerDialog? = null,
    val draftPlan: VirtualRoomPlan? = null,
    val hasUnsavedChanges: Boolean = false,
    val canUndo: Boolean = false,
    val isDemoMode: Boolean = true,
    val statusMessage: String = "当前户型识别与生成仍为演示数据，请在图上确认墙、门、窗位置。",
    val calibrationConfirmed: Boolean = false,
    val calibrationLengthMeters: Float? = null,
    val isAddingWall: Boolean = false,
    val wallDraftStart: PlanPoint? = null,
    val pendingDelete: PlannerSelection? = null,
    val showIssues: Boolean = false,
    val toast: String? = null,
    val simulateOffline: Boolean = false,
    val simulateSaveFailure: Boolean = false,
    val simulateGenerationError: Boolean = false
)

sealed interface RoomPlannerEvent {
    data object OpenImportOptions : RoomPlannerEvent
    data object DismissDialog : RoomPlannerEvent
    data object ConfirmReplaceCurrentPlan : RoomPlannerEvent
    data class StartImport(val source: FloorPlanInputSource) : RoomPlannerEvent
    data object ProcessingCompleted : RoomPlannerEvent
    data object AcceptCalibration : RoomPlannerEvent
    data class ConfirmCalibration(val knownLengthMeters: Float) : RoomPlannerEvent
    data class SelectElement(val selection: PlannerSelection?) : RoomPlannerEvent
    data class SelectElementAt(val x: Float, val z: Float) : RoomPlannerEvent
    data class AddWallAt(val x: Float, val z: Float) : RoomPlannerEvent
    data class BeginAddWallAt(val x: Float, val z: Float) : RoomPlannerEvent
    data class PlaceWallPoint(val x: Float, val z: Float) : RoomPlannerEvent
    data object FinishAddWall : RoomPlannerEvent
    data object CancelAddWall : RoomPlannerEvent
    data class AdjustSelection(val delta: Float) : RoomPlannerEvent
    data class UpdateWallGeometry(
        val lengthMeters: Float?,
        val heightMeters: Float?,
        val thicknessMeters: Float?
    ) : RoomPlannerEvent
    data object AddDoorOnSelection : RoomPlannerEvent
    data object AddWindowOnSelection : RoomPlannerEvent
    data class ApplyOpeningEdit(
        val widthMeters: Float,
        val heightMeters: Float,
        val sillMeters: Float
    ) : RoomPlannerEvent
    data object ToggleDoorSwing : RoomPlannerEvent
    data object RequestDeleteSelection : RoomPlannerEvent
    data object ConfirmDeleteSelection : RoomPlannerEvent
    data object CancelDeleteSelection : RoomPlannerEvent
    data object UndoLastChange : RoomPlannerEvent
    data class ShowDialog(val dialog: PlannerDialog) : RoomPlannerEvent
    data object RestoreDraft : RoomPlannerEvent
    data class LocateIssue(val kind: PlannerIssueKind) : RoomPlannerEvent
    data object GenerateRoom : RoomPlannerEvent
    data object GenerationCompleted : RoomPlannerEvent
    data object RetryGeneration : RoomPlannerEvent
    data object RetryConnection : RoomPlannerEvent
    data object CancelBusyStep : RoomPlannerEvent
    data object ReturnToEdit : RoomPlannerEvent
    data object RetrySave : RoomPlannerEvent
    data class ShowToast(val message: String) : RoomPlannerEvent
    data object HideToast : RoomPlannerEvent
    data object UseSamplePlan : RoomPlannerEvent
    data object InjectIssuePlan : RoomPlannerEvent
    data object ToggleOfflineSimulated : RoomPlannerEvent
    data object ToggleSaveFailureSimulated : RoomPlannerEvent
    data object ToggleGenerationErrorSimulated : RoomPlannerEvent
}

/** 门宽合法区间（P19）。 */
const val DOOR_WIDTH_MIN_METERS = 0.10f
const val DOOR_WIDTH_MAX_METERS = 2.40f

/** 汇总当前户型问题（墙体未闭合 / 构件重叠），供 P10 与生成按钮禁用逻辑使用。 */
fun buildPlanIssues(plan: VirtualRoomPlan): List<PlanIssue> {
    val issues = mutableListOf<PlanIssue>()

    // 墙体未闭合：任一墙端点没有搭在其他墙体上。
    val looseWalls = plan.walls.filter { wall ->
        wallEndpoints(wall).any { endpoint ->
            plan.walls.none { other -> other.id != wall.id && endpointInside(endpoint, other) }
        }
    }
    if (looseWalls.isNotEmpty()) {
        issues += PlanIssue(
            kind = PlannerIssueKind.OPEN_EDGE,
            message = "墙体未闭合",
            target = PlannerSelection(looseWalls.first().id, PlannerSelectionType.WALL)
        )
    }

    // 墙体互相重叠（T 型搭接不算重叠）。
    for (i in plan.walls.indices) {
        for (j in i + 1 until plan.walls.size) {
            val first = plan.walls[i]
            val second = plan.walls[j]
            if (wallsOverlap(first, second) && !isTJunction(first, second)) {
                issues += PlanIssue(
                    kind = PlannerIssueKind.OVERLAP,
                    message = "墙体发生重叠",
                    target = PlannerSelection(second.id, PlannerSelectionType.WALL)
                )
            }
        }
    }

    // 门窗互相重叠。
    for (i in plan.openings.indices) {
        for (j in i + 1 until plan.openings.size) {
            val first = plan.openings[i]
            val second = plan.openings[j]
            if (openingsOverlap(first, second)) {
                issues += PlanIssue(
                    kind = PlannerIssueKind.OVERLAP,
                    message = "门窗发生重叠",
                    target = PlannerSelection(second.id, selectionType(second.kind))
                )
            }
        }
    }
    return issues
}

/** P10 面板描述文案。 */
fun planIssueSummary(issues: List<PlanIssue>): String {
    if (issues.isEmpty()) return "未发现问题，可以生成模拟房间。"
    val labels = issues.map { it.kind.label }.distinct().joinToString("；")
    return "$labels。请先修正标记位置。"
}

/** 「生成模拟房间」可用性（规格第 2 节全局交互规则）。 */
fun isGenerateEnabled(state: RoomPlannerUiState): Boolean = when (state.step) {
    PlannerStep.UPLOAD -> false
    PlannerStep.CALIBRATE -> state.calibrationConfirmed
    else -> !state.showIssues
}

/** P04 校准：选中画布最上方（屏幕顶部）的横向外墙。 */
fun calibrationWallSelection(plan: VirtualRoomPlan): PlannerSelection? =
    plan.walls
        .filter { it.width >= it.depth }
        .maxByOrNull { it.centerZ }
        ?.let { PlannerSelection(it.id, PlannerSelectionType.WALL) }

/** P11 删除面板：统计将被一并移除的门数量。 */
fun doorsAttachedToWall(plan: VirtualRoomPlan, wallId: String): Int {
    val wall = plan.walls.firstOrNull { it.id == wallId } ?: return 0
    return plan.openings.count { opening ->
        opening.kind == RoomOpeningKind.DOOR && openingInsideWall(opening, wall)
    }
}

fun selectionType(kind: RoomOpeningKind): PlannerSelectionType = when (kind) {
    RoomOpeningKind.DOOR -> PlannerSelectionType.DOOR
    RoomOpeningKind.WINDOW -> PlannerSelectionType.WINDOW
}

private fun wallEndpoints(wall: RoomWall): List<PlanPoint> =
    if (wall.width >= wall.depth) {
        listOf(
            PlanPoint(wall.centerX - wall.width / 2f, wall.centerZ),
            PlanPoint(wall.centerX + wall.width / 2f, wall.centerZ)
        )
    } else {
        listOf(
            PlanPoint(wall.centerX, wall.centerZ - wall.depth / 2f),
            PlanPoint(wall.centerX, wall.centerZ + wall.depth / 2f)
        )
    }

private const val ENDPOINT_TOLERANCE_METERS = 0.15f
private const val OVERLAP_EPSILON_METERS = 0.02f

private fun endpointInside(endpoint: PlanPoint, wall: RoomWall): Boolean =
    endpoint.x >= wall.centerX - wall.width / 2f - ENDPOINT_TOLERANCE_METERS &&
        endpoint.x <= wall.centerX + wall.width / 2f + ENDPOINT_TOLERANCE_METERS &&
        endpoint.z >= wall.centerZ - wall.depth / 2f - ENDPOINT_TOLERANCE_METERS &&
        endpoint.z <= wall.centerZ + wall.depth / 2f + ENDPOINT_TOLERANCE_METERS

internal fun openingInsideWall(opening: RoomOpening, wall: RoomWall): Boolean =
    abs(opening.centerX - wall.centerX) <= wall.width / 2f + 0.05f &&
        abs(opening.centerZ - wall.centerZ) <= wall.depth / 2f + 0.05f

private fun spanOverlap(aCenter: Float, aSize: Float, bCenter: Float, bSize: Float): Float =
    minOf(aCenter + aSize / 2f, bCenter + bSize / 2f) -
        maxOf(aCenter - aSize / 2f, bCenter - bSize / 2f)

private fun wallsOverlap(first: RoomWall, second: RoomWall): Boolean =
    spanOverlap(first.centerX, first.width, second.centerX, second.width) > OVERLAP_EPSILON_METERS &&
        spanOverlap(first.centerZ, first.depth, second.centerZ, second.depth) > OVERLAP_EPSILON_METERS

/** T 型搭接：任一墙的端点落在另一墙体内，属于正常连接而非重叠。 */
private fun isTJunction(first: RoomWall, second: RoomWall): Boolean =
    wallEndpoints(first).any { endpointInside(it, second) } ||
        wallEndpoints(second).any { endpointInside(it, first) }

private fun openingsOverlap(first: RoomOpening, second: RoomOpening): Boolean =
    spanOverlap(first.centerX, first.width, second.centerX, second.width) > OVERLAP_EPSILON_METERS &&
        spanOverlap(first.centerZ, first.depth, second.centerZ, second.depth) > OVERLAP_EPSILON_METERS
