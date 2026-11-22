package com.pico.welcomespace.ui.planner

import com.pico.welcomespace.domain.usecase.FloorPlanInputSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomPlannerViewModelTest {
    @Test
    fun startImportMovesToProcessingAndPublishesPlan() {
        val vm = RoomPlannerViewModel()

        vm.onEvent(RoomPlannerEvent.StartImport(FloorPlanInputSource.UPLOAD))

        assertEquals(PlannerStep.PROCESSING, vm.state.value.step)
        assertEquals(FloorPlanInputSource.UPLOAD, vm.state.value.lastInputSource)
        assertEquals(vm.state.value.generatedPlan.widthMeters, RoomPlannerSession.activePlan.widthMeters, 0.001f)
    }

    @Test
    fun processingThenCalibrationEntersEditStep() {
        val vm = RoomPlannerViewModel()

        vm.onEvent(RoomPlannerEvent.StartImport(FloorPlanInputSource.UPLOAD))
        vm.onEvent(RoomPlannerEvent.ProcessingCompleted)
        vm.onEvent(RoomPlannerEvent.AcceptCalibration)

        assertEquals(PlannerStep.EDIT, vm.state.value.step)
        assertEquals(PlannerTool.WALL, vm.state.value.tool)
    }

    @Test
    fun oversizeDoorShowsValidationDialog() {
        val door = com.pico.welcomespace.domain.model.RoomOpening(
            "entry-door", "测试门", com.pico.welcomespace.domain.model.RoomOpeningKind.DOOR,
            0f, 0f, .8f, .1f
        )
        val vm = RoomPlannerViewModel(initialState = RoomPlannerUiState(
            generatedPlan = com.pico.welcomespace.domain.model.FloorPlanRoom.demoPlan.copy(openings = listOf(door))
        ))

        vm.onEvent(RoomPlannerEvent.SelectElement(PlannerSelection("entry-door", PlannerSelectionType.DOOR)))
        vm.onEvent(RoomPlannerEvent.AdjustSelection(5f))

        assertEquals(PlannerDialog.INVALID_DOOR_SIZE, vm.state.value.dialog)
    }

    @Test
    fun addWallCanUndo() {
        val vm = RoomPlannerViewModel()

        vm.onEvent(RoomPlannerEvent.StartImport(FloorPlanInputSource.UPLOAD))
        vm.onEvent(RoomPlannerEvent.ProcessingCompleted)
        vm.onEvent(RoomPlannerEvent.AcceptCalibration)
        val originalWalls = vm.state.value.generatedPlan.walls.size

        vm.onEvent(RoomPlannerEvent.AddWallAt(0f, 0f))

        assertEquals(originalWalls + 1, vm.state.value.generatedPlan.walls.size)
        assertTrue(vm.state.value.canUndo)

        vm.onEvent(RoomPlannerEvent.UndoLastChange)

        assertEquals(originalWalls, vm.state.value.generatedPlan.walls.size)
    }

    @Test
    fun generationFlowMovesToResult() {
        val vm = RoomPlannerViewModel()

        vm.onEvent(RoomPlannerEvent.StartImport(FloorPlanInputSource.UPLOAD))
        vm.onEvent(RoomPlannerEvent.ProcessingCompleted)
        vm.onEvent(RoomPlannerEvent.AcceptCalibration)
        vm.onEvent(RoomPlannerEvent.GenerateRoom)
        vm.onEvent(RoomPlannerEvent.GenerationCompleted)

        assertEquals(PlannerStep.RESULT, vm.state.value.step)
    }
}
