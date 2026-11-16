package com.pico.welcomespace.domain.usecase

import com.pico.welcomespace.domain.model.FloorPlanRoom
import com.pico.welcomespace.domain.model.VirtualRoomPlan

enum class FloorPlanInputSource {
    UPLOAD,
    CAMERA
}

data class RecognizedFloorPlan(
    val plan: VirtualRoomPlan,
    val source: FloorPlanInputSource
)

class RecognizeFloorPlanUseCase {
    fun recognize(source: FloorPlanInputSource): RecognizedFloorPlan =
        RecognizedFloorPlan(
            plan = FloorPlanRoom.demoPlan,
            source = source
        )
}
