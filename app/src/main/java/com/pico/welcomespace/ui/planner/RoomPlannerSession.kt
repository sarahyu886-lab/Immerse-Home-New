package com.pico.welcomespace.ui.planner

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pico.welcomespace.domain.model.FloorPlanRoom
import com.pico.welcomespace.domain.model.VirtualRoomPlan

object RoomPlannerSession {
    var activePlan: VirtualRoomPlan by mutableStateOf(FloorPlanRoom.demoPlan)
        private set

    var revision: Int by mutableIntStateOf(0)
        private set

    fun applyPlan(plan: VirtualRoomPlan) {
        activePlan = plan
        revision += 1
    }
}
