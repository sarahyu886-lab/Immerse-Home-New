package com.pico.welcomespace.ui.planner

import com.pico.welcomespace.domain.model.FloorPlanRoom
import com.pico.welcomespace.domain.model.VirtualRoomPlan
import com.pico.welcomespace.domain.usecase.FloorPlanInputSource

enum class PlannerStep {
    UPLOAD,
    PROCESSING,
    CALIBRATE,
    EDIT,
    GENERATING,
    RESULT
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

enum class PlannerDialog {
    IMPORT_OPTIONS,
    REPLACE_CURRENT_PLAN,
    DELETE_SELECTION,
    INVALID_IMAGE,
    UPLOAD_FAILED,
    OFFLINE,
    SAVE_FAILED,
    INVALID_DOOR_SIZE,
    GENERATE_FAILED,
    RESTORE_DRAFT,
    PLAN_ISSUES
}

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
    val statusMessage: String = "当前户型识别与生成仍为演示数据，请在图上确认墙、门、窗位置。"
)

sealed interface RoomPlannerEvent {
    data object OpenImportOptions : RoomPlannerEvent
    data object DismissDialog : RoomPlannerEvent
    data object ConfirmReplaceCurrentPlan : RoomPlannerEvent
    data class StartImport(val source: FloorPlanInputSource) : RoomPlannerEvent
    data object ProcessingCompleted : RoomPlannerEvent
    data object AcceptCalibration : RoomPlannerEvent
    data class SelectTool(val tool: PlannerTool) : RoomPlannerEvent
    data class SelectElement(val selection: PlannerSelection?) : RoomPlannerEvent
    data class SelectElementAt(val x: Float, val z: Float) : RoomPlannerEvent
    data class AddWallAt(val x: Float, val z: Float) : RoomPlannerEvent
    data class AdjustSelection(val delta: Float) : RoomPlannerEvent
    data object RequestDeleteSelection : RoomPlannerEvent
    data object ConfirmDeleteSelection : RoomPlannerEvent
    data object UndoLastChange : RoomPlannerEvent
    data class ShowDialog(val dialog: PlannerDialog) : RoomPlannerEvent
    data object RestoreDraft : RoomPlannerEvent
    data object GenerateRoom : RoomPlannerEvent
    data object GenerationCompleted : RoomPlannerEvent
    data object CancelBusyStep : RoomPlannerEvent
    data object ReturnToEdit : RoomPlannerEvent
}
