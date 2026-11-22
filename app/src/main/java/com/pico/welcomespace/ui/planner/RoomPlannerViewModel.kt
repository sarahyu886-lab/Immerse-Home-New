package com.pico.welcomespace.ui.planner

import androidx.lifecycle.ViewModel
import com.pico.welcomespace.domain.model.FloorPlanRoom
import com.pico.welcomespace.domain.model.RoomOpening
import com.pico.welcomespace.domain.model.RoomOpeningKind
import com.pico.welcomespace.domain.model.RoomWall
import com.pico.welcomespace.domain.model.VirtualRoomPlan
import com.pico.welcomespace.domain.usecase.RecognizeFloorPlanUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs

class RoomPlannerViewModel(
    private val recognizeFloorPlan: RecognizeFloorPlanUseCase = RecognizeFloorPlanUseCase(),
    initialState: RoomPlannerUiState = RoomPlannerUiState()
) : ViewModel() {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<RoomPlannerUiState> = _state.asStateFlow()

    private val undoStack = ArrayDeque<VirtualRoomPlan>()
    private var customWallIndex = 0

    fun onEvent(event: RoomPlannerEvent) {
        when (event) {
            RoomPlannerEvent.OpenImportOptions -> openImportOptions()
            RoomPlannerEvent.DismissDialog -> _state.update { it.copy(dialog = null) }
            RoomPlannerEvent.ConfirmReplaceCurrentPlan -> confirmReplaceCurrentPlan()
            is RoomPlannerEvent.StartImport -> startImport(event.source)
            RoomPlannerEvent.ProcessingCompleted -> finishProcessing()
            RoomPlannerEvent.AcceptCalibration -> _state.update {
                it.copy(
                    step = PlannerStep.EDIT,
                    dialog = null,
                    selected = null,
                    statusMessage = "已进入图上编辑，可继续调整墙、门、窗。"
                )
            }
            is RoomPlannerEvent.SelectTool -> _state.update {
                it.copy(
                    tool = event.tool,
                    selected = it.selected?.takeIf { selection -> selection.matchesTool(event.tool) },
                    statusMessage = "当前编辑对象：${event.tool.label}。点击图形开始编辑。"
                )
            }
            is RoomPlannerEvent.SelectElement -> _state.update {
                it.copy(
                    selected = event.selection,
                    statusMessage = selectionStatus(event.selection, it.generatedPlan)
                )
            }
            is RoomPlannerEvent.SelectElementAt -> selectElementAt(event.x, event.z)
            is RoomPlannerEvent.AddWallAt -> addWallAt(event.x, event.z)
            is RoomPlannerEvent.AdjustSelection -> adjustSelection(event.delta)
            RoomPlannerEvent.RequestDeleteSelection -> {
                if (_state.value.selected != null) {
                    _state.update { it.copy(dialog = PlannerDialog.DELETE_SELECTION) }
                }
            }
            RoomPlannerEvent.ConfirmDeleteSelection -> deleteSelection()
            RoomPlannerEvent.UndoLastChange -> undoLastChange()
            is RoomPlannerEvent.ShowDialog -> showDialog(event.dialog)
            RoomPlannerEvent.RestoreDraft -> restoreDraft()
            RoomPlannerEvent.GenerateRoom -> _state.update {
                it.copy(
                    step = PlannerStep.GENERATING,
                    selected = null,
                    dialog = null,
                    statusMessage = "正在根据当前编辑结果生成模拟房间…"
                )
            }
            RoomPlannerEvent.GenerationCompleted -> _state.update {
                it.copy(
                    step = PlannerStep.RESULT,
                    dialog = null,
                    statusMessage = "模拟房间已就绪，可进入 3D 房间查看。"
                )
            }
            RoomPlannerEvent.CancelBusyStep -> cancelBusyStep()
            RoomPlannerEvent.ReturnToEdit -> _state.update {
                it.copy(step = PlannerStep.EDIT, statusMessage = "已返回图上编辑。")
            }
        }
    }

    private fun openImportOptions() {
        _state.update {
            it.copy(
                dialog = if (it.hasUnsavedChanges) {
                    PlannerDialog.REPLACE_CURRENT_PLAN
                } else {
                    PlannerDialog.IMPORT_OPTIONS
                }
            )
        }
    }

    private fun confirmReplaceCurrentPlan() {
        _state.update {
            it.copy(
                dialog = PlannerDialog.IMPORT_OPTIONS,
                draftPlan = if (it.hasUnsavedChanges) it.generatedPlan else it.draftPlan,
                statusMessage = "当前编辑会保留为草稿，导入新户型后仍可恢复。"
            )
        }
    }

    private fun startImport(source: com.pico.welcomespace.domain.usecase.FloorPlanInputSource) {
        val result = recognizeFloorPlan.recognize(source)
        RoomPlannerSession.applyPlan(result.plan)
        undoStack.clear()
        _state.update {
            it.copy(
                step = PlannerStep.PROCESSING,
                tool = PlannerTool.WALL,
                lastInputSource = result.source,
                generatedPlan = result.plan,
                selected = null,
                dialog = null,
                hasUnsavedChanges = false,
                canUndo = false,
                statusMessage = if (source == com.pico.welcomespace.domain.usecase.FloorPlanInputSource.CAMERA) {
                    "正在处理拍摄的户型图…"
                } else {
                    "正在处理上传的户型图…"
                }
            )
        }
    }

    private fun finishProcessing() {
        _state.update {
            it.copy(
                step = PlannerStep.CALIBRATE,
                dialog = null,
                selected = null,
                statusMessage = "识别结果已生成，请先核对尺寸后再开始图上编辑。"
            )
        }
    }

    private fun selectElementAt(x: Float, z: Float) {
        val current = _state.value
        val selection = findSelectionAt(
            plan = current.generatedPlan,
            x = x,
            z = z,
            tool = current.tool
        )
        _state.update {
            it.copy(
                selected = selection,
                statusMessage = selectionStatus(selection, it.generatedPlan)
            )
        }
    }

    private fun addWallAt(x: Float, z: Float) {
        val current = _state.value
        if (current.step != PlannerStep.EDIT || current.tool != PlannerTool.WALL) return
        val plan = current.generatedPlan
        val wall = RoomWall(
            id = "custom-wall-${customWallIndex++}",
            centerX = x.coerceIn(-plan.widthMeters / 2f + 0.8f, plan.widthMeters / 2f - 0.8f),
            centerZ = z.coerceIn(-plan.depthMeters / 2f + 0.8f, plan.depthMeters / 2f - 0.8f),
            width = 1.4f,
            depth = 0.08f,
            height = plan.walls.firstOrNull()?.height ?: 2.7f
        )
        commitPlanChange(
            nextPlan = plan.copy(walls = plan.walls + wall),
            nextSelection = PlannerSelection(wall.id, PlannerSelectionType.WALL),
            statusMessage = "已新增墙体，可继续拉长、缩短或删除。"
        )
    }

    private fun adjustSelection(delta: Float) {
        val current = _state.value
        val selected = current.selected ?: return
        val plan = current.generatedPlan
        when (selected.type) {
            PlannerSelectionType.WALL -> {
                val wall = plan.walls.firstOrNull { it.id == selected.id } ?: return
                val horizontal = wall.width >= wall.depth
                val nextPlan = plan.copy(
                    walls = plan.walls.map {
                        if (it.id != selected.id) {
                            it
                        } else if (horizontal) {
                            it.copy(width = (it.width + delta).coerceIn(0.4f, plan.widthMeters))
                        } else {
                            it.copy(depth = (it.depth + delta).coerceIn(0.4f, plan.depthMeters))
                        }
                    }
                )
                commitPlanChange(nextPlan, selected, "已更新墙体长度。")
            }
            PlannerSelectionType.DOOR,
            PlannerSelectionType.WINDOW -> {
                val opening = plan.openings.firstOrNull { it.id == selected.id } ?: return
                val horizontal = opening.width >= opening.depth
                val currentLength = if (horizontal) opening.width else opening.depth
                val nextLength = currentLength + delta
                val range = if (opening.kind == RoomOpeningKind.DOOR) 0.10f..2.40f else 0.10f..4.00f
                if (opening.kind == RoomOpeningKind.DOOR && nextLength !in range) {
                    _state.update {
                        it.copy(
                            dialog = PlannerDialog.INVALID_DOOR_SIZE,
                            statusMessage = "门宽超出可用墙段，请调整到 0.10–2.40 米。"
                        )
                    }
                    return
                }
                val safeLength = nextLength.coerceIn(range.start, range.endInclusive)
                val nextPlan = plan.copy(
                    openings = plan.openings.map {
                        if (it.id != selected.id) {
                            it
                        } else if (horizontal) {
                            it.copy(width = safeLength)
                        } else {
                            it.copy(depth = safeLength)
                        }
                    }
                )
                commitPlanChange(
                    nextPlan = nextPlan,
                    nextSelection = selected,
                    statusMessage = if (opening.kind == RoomOpeningKind.DOOR) {
                        "已更新门宽。"
                    } else {
                        "已更新窗宽。"
                    }
                )
            }
        }
    }

    private fun deleteSelection() {
        val current = _state.value
        val selected = current.selected ?: return
        val plan = current.generatedPlan
        val nextPlan = when (selected.type) {
            PlannerSelectionType.WALL -> plan.copy(walls = plan.walls.filterNot { it.id == selected.id })
            PlannerSelectionType.DOOR,
            PlannerSelectionType.WINDOW -> plan.copy(openings = plan.openings.filterNot { it.id == selected.id })
        }
        commitPlanChange(nextPlan, null, "已删除选中构件。")
        _state.update { it.copy(dialog = null) }
    }

    private fun undoLastChange() {
        if (undoStack.isEmpty()) return
        val previous = undoStack.removeLast()
        RoomPlannerSession.applyPlan(previous)
        _state.update {
            it.copy(
                generatedPlan = previous,
                selected = null,
                dialog = null,
                draftPlan = previous,
                hasUnsavedChanges = true,
                canUndo = undoStack.isNotEmpty(),
                statusMessage = "已撤销上一步修改。"
            )
        }
    }

    private fun showDialog(dialog: PlannerDialog) {
        if (dialog == PlannerDialog.RESTORE_DRAFT && _state.value.draftPlan == null) return
        _state.update { it.copy(dialog = dialog) }
    }

    private fun restoreDraft() {
        val draft = _state.value.draftPlan ?: return
        undoStack.clear()
        RoomPlannerSession.applyPlan(draft)
        _state.update {
            it.copy(
                step = PlannerStep.EDIT,
                generatedPlan = draft,
                selected = null,
                dialog = null,
                hasUnsavedChanges = true,
                canUndo = false,
                statusMessage = "已恢复上次草稿。"
            )
        }
    }

    private fun cancelBusyStep() {
        _state.update {
            when (it.step) {
                PlannerStep.PROCESSING -> it.copy(
                    step = PlannerStep.UPLOAD,
                    statusMessage = "已取消本次上传，可重新选择户型来源。"
                )
                PlannerStep.GENERATING -> it.copy(
                    step = PlannerStep.EDIT,
                    statusMessage = "已取消本次生成，你的编辑结果仍然保留。"
                )
                else -> it
            }
        }
    }

    private fun commitPlanChange(
        nextPlan: VirtualRoomPlan,
        nextSelection: PlannerSelection?,
        statusMessage: String
    ) {
        undoStack.addLast(_state.value.generatedPlan)
        RoomPlannerSession.applyPlan(nextPlan)
        _state.update {
            it.copy(
                step = PlannerStep.EDIT,
                generatedPlan = nextPlan,
                selected = nextSelection,
                dialog = null,
                draftPlan = nextPlan,
                hasUnsavedChanges = true,
                canUndo = undoStack.isNotEmpty(),
                statusMessage = statusMessage
            )
        }
    }

    private fun selectionStatus(selection: PlannerSelection?, plan: VirtualRoomPlan): String {
        if (selection == null) return "未选中构件。点击墙、门或窗开始编辑。"
        return when (selection.type) {
            PlannerSelectionType.WALL -> "已选中墙体，可拉长、缩短或删除。"
            PlannerSelectionType.DOOR -> {
                val opening = plan.openings.firstOrNull { it.id == selection.id }
                "已选中门：${opening?.label ?: "未命名门"}。可调整门宽。"
            }
            PlannerSelectionType.WINDOW -> {
                val opening = plan.openings.firstOrNull { it.id == selection.id }
                "已选中窗：${opening?.label ?: "未命名窗"}。可调整窗宽。"
            }
        }
    }

    private fun findSelectionAt(
        plan: VirtualRoomPlan,
        x: Float,
        z: Float,
        tool: PlannerTool
    ): PlannerSelection? {
        val wallHit = plan.walls
            .mapNotNull { wall ->
                if (hit(x, z, wall.centerX, wall.centerZ, wall.width, wall.depth, 0.18f)) {
                    distanceScore(x, z, wall.centerX, wall.centerZ) to PlannerSelection(wall.id, PlannerSelectionType.WALL)
                } else {
                    null
                }
            }
            .minByOrNull { it.first }
            ?.second
        val doorHit = plan.openings
            .filter { it.kind == RoomOpeningKind.DOOR }
            .mapNotNull { opening ->
                if (hit(x, z, opening.centerX, opening.centerZ, opening.width, opening.depth, 0.2f)) {
                    distanceScore(x, z, opening.centerX, opening.centerZ) to PlannerSelection(opening.id, PlannerSelectionType.DOOR)
                } else {
                    null
                }
            }
            .minByOrNull { it.first }
            ?.second
        val windowHit = plan.openings
            .filter { it.kind == RoomOpeningKind.WINDOW }
            .mapNotNull { opening ->
                if (hit(x, z, opening.centerX, opening.centerZ, opening.width, opening.depth, 0.2f)) {
                    distanceScore(x, z, opening.centerX, opening.centerZ) to PlannerSelection(opening.id, PlannerSelectionType.WINDOW)
                } else {
                    null
                }
            }
            .minByOrNull { it.first }
            ?.second
        return when (tool) {
            PlannerTool.WALL -> wallHit
            PlannerTool.DOOR -> doorHit
            PlannerTool.WINDOW -> windowHit
        }
    }

    private fun PlannerSelection.matchesTool(tool: PlannerTool): Boolean = when (tool) {
        PlannerTool.WALL -> type == PlannerSelectionType.WALL
        PlannerTool.DOOR -> type == PlannerSelectionType.DOOR
        PlannerTool.WINDOW -> type == PlannerSelectionType.WINDOW
    }

    private fun hit(
        x: Float,
        z: Float,
        centerX: Float,
        centerZ: Float,
        width: Float,
        depth: Float,
        padding: Float
    ): Boolean {
        return abs(x - centerX) <= width / 2f + padding && abs(z - centerZ) <= depth / 2f + padding
    }

    private fun distanceScore(x: Float, z: Float, centerX: Float, centerZ: Float): Float {
        val dx = x - centerX
        val dz = z - centerZ
        return dx * dx + dz * dz
    }
}
