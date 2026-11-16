package com.pico.welcomespace.ui.planner

import com.pico.welcomespace.domain.model.FloorPlanRoom
import com.pico.welcomespace.domain.usecase.FloorPlanInputSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomPlannerViewModelTest {
    @Test
    fun footprintScaleRegeneratesMeterPlan() {
        val plan = FloorPlanRoom.scaledPlan(1.2f, 2.7f)

        assertEquals(13.2f, plan.widthMeters, 0.001f)
        assertEquals(14.22f, plan.depthMeters, 0.001f)
    }

    @Test
    fun wallHeightRegeneratesWallsWithoutChangingFloorHeight() {
        val plan = FloorPlanRoom.scaledPlan(1f, 3.1f)

        assertTrue(plan.walls.all { it.height == 3.1f })
        assertTrue(plan.surfaces.filter { it.id.endsWith("floor") }.all { it.heightY == 0f })
    }

    @Test
    fun applyPreviewPublishesPlanToStageSession() {
        val vm = RoomPlannerViewModel()
        vm.onEvent(RoomPlannerEvent.ApplyPreview)

        assertEquals(vm.state.value.generatedPlan.widthMeters, RoomPlannerSession.activePlan.widthMeters, 0.001f)
        assertTrue(RoomPlannerSession.revision > 0)
    }

    @Test
    fun recognizedUploadPublishesPlanToStageSession() {
        val vm = RoomPlannerViewModel()
        vm.onEvent(RoomPlannerEvent.FloorPlanRecognized(FloorPlanInputSource.UPLOAD))

        assertEquals(FloorPlanInputSource.UPLOAD, vm.state.value.lastInputSource)
        assertEquals(FloorPlanRoom.demoPlan.widthMeters, RoomPlannerSession.activePlan.widthMeters, 0.001f)
        assertEquals(FloorPlanRoom.demoPlan.openings.size, RoomPlannerSession.activePlan.openings.size)
    }
}
