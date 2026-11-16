package com.pico.welcomespace.ui.planner

import androidx.lifecycle.ViewModel
import com.pico.welcomespace.domain.model.FloorPlanRoom
import com.pico.welcomespace.domain.usecase.RecognizeFloorPlanUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class RoomPlannerViewModel(
    private val recognizeFloorPlan: RecognizeFloorPlanUseCase = RecognizeFloorPlanUseCase()
) : ViewModel() {
    private val _state = MutableStateFlow(RoomPlannerUiState())
    val state: StateFlow<RoomPlannerUiState> = _state.asStateFlow()

    fun onEvent(event: RoomPlannerEvent) {
        when (event) {
            is RoomPlannerEvent.FootprintScaleChanged -> _state.update {
                val scale = (it.footprintScale + event.delta).coerceIn(0.6f, 1.8f)
                it.copy(
                    footprintScale = scale,
                    generatedPlan = FloorPlanRoom.scaledPlan(scale, it.wallHeight)
                )
            }
            is RoomPlannerEvent.WallHeightChanged -> _state.update {
                val height = (it.wallHeight + event.delta).coerceIn(2.2f, 3.6f)
                it.copy(
                    wallHeight = height,
                    generatedPlan = FloorPlanRoom.scaledPlan(it.footprintScale, height)
                )
            }
            is RoomPlannerEvent.FloorPlanRecognized -> _state.update {
                val result = recognizeFloorPlan.recognize(event.source)
                RoomPlannerSession.applyPlan(result.plan)
                it.copy(
                    footprintScale = 1f,
                    wallHeight = result.plan.walls.firstOrNull()?.height ?: it.wallHeight,
                    generatedPlan = result.plan,
                    lastInputSource = result.source
                )
            }
            RoomPlannerEvent.ApplyPreview -> _state.update {
                val plan = FloorPlanRoom.scaledPlan(it.footprintScale, it.wallHeight)
                RoomPlannerSession.applyPlan(plan)
                it.copy(generatedPlan = plan)
            }
            RoomPlannerEvent.ResetPlan -> _state.update {
                RoomPlannerSession.applyPlan(FloorPlanRoom.demoPlan)
                RoomPlannerUiState()
            }
        }
    }
}
