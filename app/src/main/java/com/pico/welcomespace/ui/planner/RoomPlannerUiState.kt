package com.pico.welcomespace.ui.planner

import com.pico.welcomespace.domain.model.FloorPlanRoom
import com.pico.welcomespace.domain.model.VirtualRoomPlan
import com.pico.welcomespace.domain.usecase.FloorPlanInputSource

data class RoomPlannerUiState(
    val footprintScale: Float = 1f,
    val wallHeight: Float = 2.7f,
    val activeRoomName: String = "四房两厅",
    val status: String = "",
    val lastInputSource: FloorPlanInputSource? = null,
    val generatedPlan: VirtualRoomPlan = FloorPlanRoom.demoPlan
)

sealed interface RoomPlannerEvent {
    data class FootprintScaleChanged(val delta: Float) : RoomPlannerEvent
    data class WallHeightChanged(val delta: Float) : RoomPlannerEvent
    data class FloorPlanRecognized(val source: FloorPlanInputSource) : RoomPlannerEvent
    data object ApplyPreview : RoomPlannerEvent
    data object ResetPlan : RoomPlannerEvent
}
