package com.pico.welcomespace.ui.planner

import androidx.lifecycle.ViewModel
import com.pico.welcomespace.domain.model.FloorPlanRoom
import com.pico.welcomespace.domain.model.RoomOpening
import com.pico.welcomespace.domain.model.RoomOpeningKind
import com.pico.welcomespace.domain.model.RoomWall
import com.pico.welcomespace.domain.model.VirtualRoomPlan
import com.pico.welcomespace.domain.usecase.FloorPlanInputSource
import com.pico.welcomespace.domain.usecase.RecognizeFloorPlanUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs

/** 演示流程 toast 文案（P05/P15–P19）。 */
const val TOAST_LOCAL_SAVED = "已保存到本机"
const val TOAST_LOCAL_DRAFT = "本机会话草稿"
const val TOAST_SAVE_FAILED = "尚未保存，请重试"
const val TOAST_CAMERA_UNAVAILABLE = "相机不可用"

class RoomPlannerViewModel(
    private val recognizeFloorPlan: RecognizeFloorPlanUseCase = RecognizeFloorPlanUseCase(),
    initialState: RoomPlannerUiState = RoomPlannerUiState()
) : ViewModel() {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<RoomPlannerUiState> = _state.asStateFlow()

    private val undoStack = ArrayDeque<VirtualRoomPlan>()
    private var customWallIndex = 0
    private var customOpeningIndex = 0

    fun onEvent(event: RoomPlannerEvent) {
        when (event) {
            RoomPlannerEvent.OpenImportOptions -> openImportOptions()
            RoomPlannerEvent.DismissDialog -> _state.update { it.copy(dialog = null) }
            RoomPlannerEvent.ConfirmReplaceCurrentPlan -> confirmReplaceCurrentPlan()
            is RoomPlannerEvent.StartImport -> startImport(event.source)
            RoomPlannerEvent.ProcessingCompleted -> finishProcessing()
            RoomPlannerEvent.AcceptCalibration -> acceptCalibration()
            is RoomPlannerEvent.ConfirmCalibration -> confirmCalibration(event.knownLengthMeters)
            is RoomPlannerEvent.SelectElement -> _state.update {
                it.copy(
                    selected = event.selection,
                    statusMessage = selectionStatus(event.selection, it.generatedPlan)
                )
            }
            is RoomPlannerEvent.SelectElementAt -> selectElementAt(event.x, event.z)
            is RoomPlannerEvent.AddWallAt -> addWallAt(event.x, event.z)
            is RoomPlannerEvent.BeginAddWallAt -> beginAddWallAt(event.x, event.z)
            is RoomPlannerEvent.PlaceWallPoint -> placeWallPoint(event.x, event.z)
            RoomPlannerEvent.FinishAddWall -> _state.update {
                it.copy(
                    isAddingWall = false,
                    wallDraftStart = null,
                    statusMessage = "已完成墙体绘制，可继续编辑其他构件。"
                )
            }
            RoomPlannerEvent.CancelAddWall -> _state.update {
                it.copy(
                    isAddingWall = false,
                    wallDraftStart = null,
                    selected = null,
                    statusMessage = "已取消添加墙体。"
                )
            }
            is RoomPlannerEvent.AdjustSelection -> adjustSelection(event.delta)
            is RoomPlannerEvent.UpdateWallGeometry -> updateWallGeometry(event)
            RoomPlannerEvent.AddDoorOnSelection -> addOpeningOnSelection(RoomOpeningKind.DOOR)
            RoomPlannerEvent.AddWindowOnSelection -> addOpeningOnSelection(RoomOpeningKind.WINDOW)
            is RoomPlannerEvent.ApplyOpeningEdit -> applyOpeningEdit(event)
            RoomPlannerEvent.ToggleDoorSwing -> toggleDoorSwing()
            RoomPlannerEvent.RequestDeleteSelection -> {
                if (_state.value.selected != null) {
                    _state.update { it.copy(pendingDelete = it.selected) }
                }
            }
            RoomPlannerEvent.ConfirmDeleteSelection -> deleteSelection()
            RoomPlannerEvent.CancelDeleteSelection -> _state.update { it.copy(pendingDelete = null) }
            RoomPlannerEvent.UndoLastChange -> undoLastChange()
            is RoomPlannerEvent.ShowDialog -> showDialog(event.dialog)
            RoomPlannerEvent.RestoreDraft -> restoreDraft()
            is RoomPlannerEvent.LocateIssue -> locateIssue(event.kind)
            RoomPlannerEvent.GenerateRoom -> generateRoom()
            RoomPlannerEvent.GenerationCompleted -> generationCompleted()
            RoomPlannerEvent.RetryGeneration -> _state.update {
                it.copy(step = PlannerStep.GENERATING, statusMessage = "正在重新生成模拟房间…")
            }
            RoomPlannerEvent.RetryConnection -> retryConnection()
            RoomPlannerEvent.CancelBusyStep -> cancelBusyStep()
            RoomPlannerEvent.ReturnToEdit -> _state.update {
                it.copy(
                    step = PlannerStep.EDIT,
                    selected = null,
                    pendingDelete = null,
                    isAddingWall = false,
                    wallDraftStart = null,
                    showIssues = false,
                    toast = null,
                    statusMessage = "已返回图上编辑。"
                )
            }
            RoomPlannerEvent.RetrySave -> _state.update {
                it.copy(dialog = null, toast = TOAST_LOCAL_SAVED, statusMessage = "已重新保存到本机（模拟）。")
            }
            is RoomPlannerEvent.ShowToast -> _state.update { it.copy(toast = event.message) }
            RoomPlannerEvent.HideToast -> _state.update { it.copy(toast = null) }
            RoomPlannerEvent.UseSamplePlan -> useSamplePlan()
            RoomPlannerEvent.InjectIssuePlan -> injectIssuePlan()
            RoomPlannerEvent.ToggleOfflineSimulated -> toggleDemoFlag("离线", { it.simulateOffline }) { it.copy(simulateOffline = !it.simulateOffline) }
            RoomPlannerEvent.ToggleSaveFailureSimulated -> toggleDemoFlag("保存失败", { it.simulateSaveFailure }) { it.copy(simulateSaveFailure = !it.simulateSaveFailure) }
            RoomPlannerEvent.ToggleGenerationErrorSimulated -> toggleDemoFlag("生成异常", { it.simulateGenerationError }) { it.copy(simulateGenerationError = !it.simulateGenerationError) }
        }
    }

    private fun toggleDemoFlag(
        label: String,
        flag: (RoomPlannerUiState) -> Boolean,
        change: (RoomPlannerUiState) -> RoomPlannerUiState
    ) {
        val next = change(_state.value)
        _state.update {
            next.copy(toast = "已${if (flag(next)) "开启" else "关闭"}${label}模拟（演示）")
        }
    }

    private fun openImportOptions() {
        val current = _state.value
        when {
            current.step == PlannerStep.UPLOAD ->
                _state.update { it.copy(step = PlannerStep.SELECT_SOURCE, toast = null) }
            current.hasUnsavedChanges -> showDialog(PlannerDialog.REPLACE_CURRENT_PLAN)
            else -> _state.update { it.copy(step = PlannerStep.SELECT_SOURCE, toast = null) }
        }
    }

    private fun confirmReplaceCurrentPlan() {
        _state.update {
            it.copy(
                step = PlannerStep.SELECT_SOURCE,
                dialog = null,
                draftPlan = if (it.hasUnsavedChanges) it.generatedPlan else it.draftPlan,
                selected = null,
                pendingDelete = null,
                isAddingWall = false,
                wallDraftStart = null,
                showIssues = false,
                statusMessage = "当前编辑已保留为本机草稿（模拟），请选择新的户型来源。"
            )
        }
    }

    private fun startImport(source: FloorPlanInputSource) {
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
                pendingDelete = null,
                isAddingWall = false,
                wallDraftStart = null,
                showIssues = false,
                hasUnsavedChanges = false,
                canUndo = false,
                calibrationConfirmed = false,
                calibrationLengthMeters = null,
                toast = null,
                statusMessage = if (source == FloorPlanInputSource.CAMERA) {
                    "正在处理拍摄的户型图…（演示数据）"
                } else {
                    "正在处理上传的户型图…（演示数据）"
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
                statusMessage = "提取结果已生成（模拟），请先确认一段已知长度。"
            )
        }
    }

    private fun acceptCalibration() {
        _state.update {
            it.copy(
                step = PlannerStep.EDIT,
                calibrationConfirmed = true,
                dialog = null,
                selected = null,
                statusMessage = "已进入图上编辑，可继续调整墙、门、窗。"
            )
        }
    }

    private fun confirmCalibration(knownLengthMeters: Float) {
        if (!knownLengthMeters.isFinite() || knownLengthMeters < 0.5f || knownLengthMeters > 20f) {
            _state.update { it.copy(toast = "请输入 0.5–20 米之间的已知长度") }
            return
        }
        _state.update {
            it.copy(
                step = PlannerStep.EDIT,
                calibrationConfirmed = true,
                calibrationLengthMeters = knownLengthMeters,
                selected = null,
                toast = TOAST_LOCAL_SAVED,
                statusMessage = "尺寸已校准（模拟），可开始图上编辑。"
            )
        }
    }

    private fun useSamplePlan() {
        val plan = FloorPlanRoom.simulatedSamplePlan
        RoomPlannerSession.applyPlan(plan)
        undoStack.clear()
        _state.update {
            it.copy(
                step = PlannerStep.EDIT,
                tool = PlannerTool.WALL,
                generatedPlan = plan,
                selected = null,
                dialog = null,
                pendingDelete = null,
                isAddingWall = false,
                wallDraftStart = null,
                showIssues = false,
                hasUnsavedChanges = false,
                canUndo = false,
                calibrationConfirmed = true,
                toast = null,
                statusMessage = "已载入示例户型（模拟数据），可直接在图上编辑。"
            )
        }
    }

    private fun injectIssuePlan() {
        val plan = FloorPlanRoom.simulatedIssuePlan
        RoomPlannerSession.applyPlan(plan)
        undoStack.clear()
        _state.update {
            it.copy(
                step = PlannerStep.EDIT,
                generatedPlan = plan,
                selected = null,
                dialog = null,
                pendingDelete = null,
                isAddingWall = false,
                wallDraftStart = null,
                showIssues = true,
                hasUnsavedChanges = true,
                canUndo = false,
                draftPlan = plan,
                toast = null,
                statusMessage = "已载入含问题的示例数据（模拟），请检查标记位置。"
            )
        }
    }

    private fun selectElementAt(x: Float, z: Float) {
        val current = _state.value
        if (current.isAddingWall) return
        val selection = findSelectionAt(current.generatedPlan, x, z)
        _state.update {
            it.copy(
                selected = selection,
                statusMessage = selectionStatus(selection, it.generatedPlan)
            )
        }
    }

    private fun beginAddWallAt(x: Float, z: Float) {
        val current = _state.value
        if (current.step != PlannerStep.EDIT || current.isAddingWall) return
        _state.update {
            it.copy(
                isAddingWall = true,
                wallDraftStart = PlanPoint(x, z),
                selected = null,
                pendingDelete = null,
                showIssues = false,
                statusMessage = "已放置起点，请点击终点完成新墙。"
            )
        }
    }

    private fun placeWallPoint(x: Float, z: Float) {
        val current = _state.value
        val start = current.wallDraftStart ?: return
        val plan = current.generatedPlan
        val dx = abs(x - start.x)
        val dz = abs(z - start.z)
        if (dx < 0.25f && dz < 0.25f) {
            _state.update { it.copy(toast = "墙段太短，请重新选择终点") }
            return
        }
        val thickness = 0.12f
        val wall = if (dx >= dz) {
            RoomWall(
                id = "custom-wall-${customWallIndex++}",
                centerX = (start.x + x) / 2f,
                centerZ = (start.z + z) / 2f,
                width = maxOf(dx, thickness),
                depth = thickness,
                height = plan.walls.firstOrNull()?.height ?: 2.7f
            )
        } else {
            RoomWall(
                id = "custom-wall-${customWallIndex++}",
                centerX = (start.x + x) / 2f,
                centerZ = (start.z + z) / 2f,
                width = thickness,
                depth = maxOf(dz, thickness),
                height = plan.walls.firstOrNull()?.height ?: 2.7f
            )
        }
        commitPlanChange(
            nextPlan = plan.copy(walls = plan.walls + wall),
            nextSelection = PlannerSelection(wall.id, PlannerSelectionType.WALL),
            statusMessage = "已添加墙体，可在面板中继续调整。"
        )
        _state.update {
            it.copy(isAddingWall = false, wallDraftStart = null)
        }
    }

    private fun addWallAt(x: Float, z: Float) {
        val current = _state.value
        if (current.step != PlannerStep.EDIT || current.isAddingWall) return
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

    private fun updateWallGeometry(event: RoomPlannerEvent.UpdateWallGeometry) {
        val current = _state.value
        val selected = current.selected ?: return
        if (selected.type != PlannerSelectionType.WALL) return
        val plan = current.generatedPlan
        val wall = plan.walls.firstOrNull { it.id == selected.id } ?: return
        val horizontal = wall.width >= wall.depth
        val thickness = (event.thicknessMeters ?: wallThicknessOf(wall)).coerceIn(0.08f, 0.6f)
        val nextWall = if (horizontal) {
            wall.copy(
                width = (event.lengthMeters ?: wall.width).coerceIn(0.4f, plan.widthMeters + 0.5f),
                depth = thickness,
                height = (event.heightMeters ?: wall.height).coerceIn(2f, 3.6f)
            )
        } else {
            wall.copy(
                depth = (event.lengthMeters ?: wall.depth).coerceIn(0.4f, plan.depthMeters + 0.5f),
                width = thickness,
                height = (event.heightMeters ?: wall.height).coerceIn(2f, 3.6f)
            )
        }
        commitPlanChange(
            nextPlan = plan.copy(walls = plan.walls.map { if (it.id == wall.id) nextWall else it }),
            nextSelection = selected,
            statusMessage = "已更新墙体参数。",
            pushUndo = false
        )
    }

    private fun wallThicknessOf(wall: RoomWall): Float = minOf(wall.width, wall.depth)

    private fun addOpeningOnSelection(kind: RoomOpeningKind) {
        val current = _state.value
        val selected = current.selected ?: return
        if (selected.type != PlannerSelectionType.WALL) return
        val plan = current.generatedPlan
        val wall = plan.walls.firstOrNull { it.id == selected.id } ?: return
        val horizontal = wall.width >= wall.depth
        val opening = if (kind == RoomOpeningKind.DOOR) {
            RoomOpening(
                id = "custom-door-${customOpeningIndex++}",
                label = "门",
                kind = RoomOpeningKind.DOOR,
                centerX = wall.centerX,
                centerZ = wall.centerZ,
                width = if (horizontal) 0.9f else 0.12f,
                depth = if (horizontal) 0.12f else 0.9f,
                heightMeters = 2.1f
            )
        } else {
            RoomOpening(
                id = "custom-window-${customOpeningIndex++}",
                label = "窗",
                kind = RoomOpeningKind.WINDOW,
                centerX = wall.centerX,
                centerZ = wall.centerZ,
                width = if (horizontal) 1.5f else 0.12f,
                depth = if (horizontal) 0.12f else 1.5f,
                heightMeters = 1.2f,
                sillMeters = 0.9f
            )
        }
        commitPlanChange(
            nextPlan = plan.copy(openings = plan.openings + opening),
            nextSelection = PlannerSelection(opening.id, selectionType(kind)),
            statusMessage = if (kind == RoomOpeningKind.DOOR) "已在该墙添加门，可调整宽度与开向。" else "已在该墙添加窗，可调整尺寸。"
        )
    }

    private fun applyOpeningEdit(event: RoomPlannerEvent.ApplyOpeningEdit) {
        val current = _state.value
        val selected = current.selected ?: return
        if (selected.type == PlannerSelectionType.WALL) return
        val plan = current.generatedPlan
        val opening = plan.openings.firstOrNull { it.id == selected.id } ?: return
        val horizontal = opening.width >= opening.depth
        if (opening.kind == RoomOpeningKind.DOOR &&
            (event.widthMeters < DOOR_WIDTH_MIN_METERS || event.widthMeters > DOOR_WIDTH_MAX_METERS)
        ) {
            _state.update {
                it.copy(
                    dialog = PlannerDialog.INVALID_DOOR_SIZE,
                    toast = TOAST_LOCAL_DRAFT,
                    statusMessage = "门宽超出可用墙段，请输入 0.10–2.40 米。"
                )
            }
            return
        }
        val widthRange = if (opening.kind == RoomOpeningKind.DOOR) {
            DOOR_WIDTH_MIN_METERS..DOOR_WIDTH_MAX_METERS
        } else {
            0.10f..4.00f
        }
        val safeWidth = event.widthMeters.coerceIn(widthRange.start, widthRange.endInclusive)
        val nextOpening = opening.copy(
            width = if (horizontal) safeWidth else opening.width,
            depth = if (horizontal) opening.depth else safeWidth,
            heightMeters = event.heightMeters.coerceIn(0.5f, plan.ceilingHeight),
            sillMeters = if (opening.kind == RoomOpeningKind.WINDOW) {
                event.sillMeters.coerceIn(0f, plan.ceilingHeight - event.heightMeters.coerceIn(0.5f, plan.ceilingHeight))
            } else {
                0f
            }
        )
        commitPlanChange(
            nextPlan = plan.copy(openings = plan.openings.map { if (it.id == opening.id) nextOpening else it }),
            nextSelection = selected,
            statusMessage = if (opening.kind == RoomOpeningKind.DOOR) "已更新门尺寸。" else "已更新窗尺寸。"
        )
    }

    private fun toggleDoorSwing() {
        val current = _state.value
        val selected = current.selected ?: return
        if (selected.type != PlannerSelectionType.DOOR) return
        val plan = current.generatedPlan
        val opening = plan.openings.firstOrNull { it.id == selected.id } ?: return
        commitPlanChange(
            nextPlan = plan.copy(
                openings = plan.openings.map {
                    if (it.id == opening.id) it.copy(swingFlipped = !it.swingFlipped) else it
                }
            ),
            nextSelection = selected,
            statusMessage = "已切换门开向。",
            pushUndo = false
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
                val range = if (opening.kind == RoomOpeningKind.DOOR) {
                    DOOR_WIDTH_MIN_METERS..DOOR_WIDTH_MAX_METERS
                } else {
                    0.10f..4.00f
                }
                if (opening.kind == RoomOpeningKind.DOOR && nextLength !in range) {
                    _state.update {
                        it.copy(
                            dialog = PlannerDialog.INVALID_DOOR_SIZE,
                            toast = TOAST_LOCAL_DRAFT,
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
        val target = current.pendingDelete ?: current.selected ?: return
        val plan = current.generatedPlan
        when (target.type) {
            PlannerSelectionType.WALL -> {
                val wall = plan.walls.firstOrNull { it.id == target.id }
                val nextOpenings = if (wall != null) {
                    plan.openings.filterNot { openingInsideWall(it, wall) }
                } else {
                    plan.openings
                }
                val removedDoors = if (wall != null) {
                    plan.openings.count { it.kind == RoomOpeningKind.DOOR && openingInsideWall(it, wall) }
                } else {
                    0
                }
                commitPlanChange(
                    nextPlan = plan.copy(
                        walls = plan.walls.filterNot { it.id == target.id },
                        openings = nextOpenings
                    ),
                    nextSelection = null,
                    statusMessage = if (removedDoors > 0) {
                        "已删除墙体及其上的 $removedDoors 扇门，可撤销。"
                    } else {
                        "已删除墙体，可撤销。"
                    }
                )
            }
            PlannerSelectionType.DOOR,
            PlannerSelectionType.WINDOW ->
                commitPlanChange(
                    nextPlan = plan.copy(openings = plan.openings.filterNot { it.id == target.id }),
                    nextSelection = null,
                    statusMessage = "已删除选中门窗，可撤销。"
                )
        }
        _state.update { it.copy(pendingDelete = null, dialog = null) }
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
                pendingDelete = null,
                draftPlan = previous,
                hasUnsavedChanges = true,
                canUndo = undoStack.isNotEmpty(),
                showIssues = buildPlanIssues(previous).isNotEmpty() && it.showIssues,
                statusMessage = "已撤销上一步修改。"
            )
        }
    }

    private fun showDialog(dialog: PlannerDialog) {
        if (dialog == PlannerDialog.RESTORE_DRAFT && _state.value.draftPlan == null) return
        _state.update {
            it.copy(
                dialog = dialog,
                toast = when (dialog) {
                    PlannerDialog.SAVE_FAILED -> TOAST_SAVE_FAILED
                    else -> TOAST_LOCAL_DRAFT
                }
            )
        }
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
                pendingDelete = null,
                isAddingWall = false,
                wallDraftStart = null,
                hasUnsavedChanges = true,
                canUndo = false,
                calibrationConfirmed = true,
                toast = TOAST_LOCAL_SAVED,
                statusMessage = "已恢复上次草稿（本机模拟）。"
            )
        }
    }

    private fun locateIssue(kind: PlannerIssueKind) {
        val current = _state.value
        val issue = buildPlanIssues(current.generatedPlan).firstOrNull { it.kind == kind }
        if (issue == null) {
            _state.update { it.copy(toast = "当前没有${kind.label}的问题") }
            return
        }
        _state.update {
            it.copy(
                selected = issue.target,
                statusMessage = "已定位到问题位置，请修正后再生成。"
            )
        }
    }

    private fun generateRoom() {
        val current = _state.value
        when {
            current.simulateOffline -> showDialog(PlannerDialog.OFFLINE)
            buildPlanIssues(current.generatedPlan).isNotEmpty() -> _state.update {
                it.copy(
                    showIssues = true,
                    selected = null,
                    pendingDelete = null,
                    statusMessage = "检查到待修正问题，请先处理标记位置。"
                )
            }
            else -> _state.update {
                it.copy(
                    step = PlannerStep.GENERATING,
                    selected = null,
                    pendingDelete = null,
                    dialog = null,
                    isAddingWall = false,
                    wallDraftStart = null,
                    showIssues = false,
                    statusMessage = "正在搭建你的房间…"
                )
            }
        }
    }

    private fun generationCompleted() {
        if (_state.value.simulateGenerationError) {
            _state.update {
                it.copy(
                    step = PlannerStep.ERROR,
                    statusMessage = "生成失败（演示），当前编辑已保留。"
                )
            }
        } else {
            _state.update {
                it.copy(
                    step = PlannerStep.RESULT,
                    dialog = null,
                    statusMessage = "模拟房间已就绪，可进入 3D 房间查看。"
                )
            }
        }
    }

    private fun retryConnection() {
        _state.update { it.copy(simulateOffline = false, dialog = null, toast = null) }
        generateRoom()
    }

    private fun cancelBusyStep() {
        _state.update {
            when (it.step) {
                PlannerStep.PROCESSING, PlannerStep.CALIBRATE -> it.copy(
                    step = PlannerStep.SELECT_SOURCE,
                    statusMessage = "已取消本次处理，可重新选择户型来源。"
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
        statusMessage: String,
        pushUndo: Boolean = true
    ) {
        if (pushUndo) undoStack.addLast(_state.value.generatedPlan)
        RoomPlannerSession.applyPlan(nextPlan)
        val saveFailed = _state.value.simulateSaveFailure
        _state.update {
            it.copy(
                step = PlannerStep.EDIT,
                generatedPlan = nextPlan,
                selected = nextSelection,
                dialog = null,
                pendingDelete = null,
                isAddingWall = false,
                wallDraftStart = null,
                showIssues = it.showIssues && buildPlanIssues(nextPlan).isNotEmpty(),
                draftPlan = nextPlan,
                hasUnsavedChanges = true,
                canUndo = undoStack.isNotEmpty(),
                statusMessage = statusMessage
            )
        }
        if (saveFailed) {
            _state.update {
                it.copy(dialog = PlannerDialog.SAVE_FAILED, toast = TOAST_SAVE_FAILED)
            }
        } else {
            _state.update { it.copy(toast = TOAST_LOCAL_SAVED) }
        }
    }

    private fun selectionStatus(selection: PlannerSelection?, plan: VirtualRoomPlan): String {
        if (selection == null) return "未选中构件。点击墙、门或窗开始编辑。"
        return when (selection.type) {
            PlannerSelectionType.WALL -> "已选中墙体，可调整长宽高或删除。"
            PlannerSelectionType.DOOR -> {
                val opening = plan.openings.firstOrNull { it.id == selection.id }
                "已选中门：${opening?.label ?: "未命名门"}。可调整宽度、高度与开向。"
            }
            PlannerSelectionType.WINDOW -> {
                val opening = plan.openings.firstOrNull { it.id == selection.id }
                "已选中窗：${opening?.label ?: "未命名窗"}。可调整宽度、高度与窗台高。"
            }
        }
    }

    private fun findSelectionAt(plan: VirtualRoomPlan, x: Float, z: Float): PlannerSelection? {
        val doorHit = plan.openings
            .filter { it.kind == RoomOpeningKind.DOOR }
            .mapNotNull { opening ->
                if (hit(x, z, opening.centerX, opening.centerZ, opening.width, opening.depth, 0.25f)) {
                    distanceScore(x, z, opening.centerX, opening.centerZ) to
                        PlannerSelection(opening.id, PlannerSelectionType.DOOR)
                } else {
                    null
                }
            }
            .minByOrNull { it.first }
            ?.second
        if (doorHit != null) return doorHit
        val windowHit = plan.openings
            .filter { it.kind == RoomOpeningKind.WINDOW }
            .mapNotNull { opening ->
                if (hit(x, z, opening.centerX, opening.centerZ, opening.width, opening.depth, 0.25f)) {
                    distanceScore(x, z, opening.centerX, opening.centerZ) to
                        PlannerSelection(opening.id, PlannerSelectionType.WINDOW)
                } else {
                    null
                }
            }
            .minByOrNull { it.first }
            ?.second
        if (windowHit != null) return windowHit
        return plan.walls
            .mapNotNull { wall ->
                if (hit(x, z, wall.centerX, wall.centerZ, wall.width, wall.depth, 0.18f)) {
                    distanceScore(x, z, wall.centerX, wall.centerZ) to
                        PlannerSelection(wall.id, PlannerSelectionType.WALL)
                } else {
                    null
                }
            }
            .minByOrNull { it.first }
            ?.second
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
