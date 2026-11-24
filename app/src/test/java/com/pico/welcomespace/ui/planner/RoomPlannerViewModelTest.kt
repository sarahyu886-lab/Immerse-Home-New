package com.pico.welcomespace.ui.planner

import com.pico.welcomespace.domain.model.FloorPlanRoom
import com.pico.welcomespace.domain.model.RoomOpeningKind
import com.pico.welcomespace.domain.usecase.FloorPlanInputSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

    /* ---------------------- 新流程（HF 设计稿）补充测试 ---------------------- */

    private fun enterEditStep(vm: RoomPlannerViewModel) {
        vm.onEvent(RoomPlannerEvent.StartImport(FloorPlanInputSource.UPLOAD))
        vm.onEvent(RoomPlannerEvent.ProcessingCompleted)
        vm.onEvent(RoomPlannerEvent.AcceptCalibration)
    }

    @Test
    fun samplePlanIsClosedForGeneration() {
        assertTrue(buildPlanIssues(FloorPlanRoom.simulatedSamplePlan).isEmpty())
    }

    @Test
    fun issuePlanSurfacesExactlyTwoIssues() {
        val issues = buildPlanIssues(FloorPlanRoom.simulatedIssuePlan)

        assertEquals(2, issues.size)
        assertEquals(PlannerIssueKind.OPEN_EDGE, issues[0].kind)
        assertEquals(PlannerIssueKind.OVERLAP, issues[1].kind)
    }

    @Test
    fun useSamplePlanEntersEditWithSample() {
        val vm = RoomPlannerViewModel()

        vm.onEvent(RoomPlannerEvent.UseSamplePlan)

        assertEquals(PlannerStep.EDIT, vm.state.value.step)
        assertEquals(FloorPlanRoom.simulatedSamplePlan.name, vm.state.value.generatedPlan.name)
        assertTrue(vm.state.value.calibrationConfirmed)
        assertTrue(isGenerateEnabled(vm.state.value))
    }

    @Test
    fun generateRequiresCalibrationOnCalibrateStep() {
        val vm = RoomPlannerViewModel()
        assertFalse(isGenerateEnabled(vm.state.value))

        vm.onEvent(RoomPlannerEvent.StartImport(FloorPlanInputSource.UPLOAD))
        vm.onEvent(RoomPlannerEvent.ProcessingCompleted)

        assertEquals(PlannerStep.CALIBRATE, vm.state.value.step)
        assertFalse(vm.state.value.calibrationConfirmed)
        assertFalse(isGenerateEnabled(vm.state.value))

        vm.onEvent(RoomPlannerEvent.ConfirmCalibration(6f))

        assertEquals(PlannerStep.EDIT, vm.state.value.step)
        assertTrue(vm.state.value.calibrationConfirmed)
        assertTrue(isGenerateEnabled(vm.state.value))
    }

    @Test
    fun twoPointAddWallCreatesSelectableWall() {
        val vm = RoomPlannerViewModel()
        enterEditStep(vm)
        val originalWalls = vm.state.value.generatedPlan.walls.size

        vm.onEvent(RoomPlannerEvent.BeginAddWallAt(0f, -1f))
        assertTrue(vm.state.value.isAddingWall)
        assertNotNull(vm.state.value.wallDraftStart)

        vm.onEvent(RoomPlannerEvent.PlaceWallPoint(1.5f, -1f))

        assertFalse(vm.state.value.isAddingWall)
        assertNull(vm.state.value.wallDraftStart)
        assertEquals(originalWalls + 1, vm.state.value.generatedPlan.walls.size)
        assertEquals(PlannerSelectionType.WALL, vm.state.value.selected?.type)
    }

    @Test
    fun deletingWallCreatesOpenEdgeIssueAndRoutesToIssues() {
        val vm = RoomPlannerViewModel()
        enterEditStep(vm)

        vm.onEvent(
            RoomPlannerEvent.SelectElement(PlannerSelection("sample-south", PlannerSelectionType.WALL))
        )
        vm.onEvent(RoomPlannerEvent.RequestDeleteSelection)
        assertEquals(vm.state.value.selected, vm.state.value.pendingDelete)

        vm.onEvent(RoomPlannerEvent.ConfirmDeleteSelection)

        val issues = buildPlanIssues(vm.state.value.generatedPlan)
        assertTrue(issues.any { it.kind == PlannerIssueKind.OPEN_EDGE })

        vm.onEvent(RoomPlannerEvent.GenerateRoom)

        assertEquals(PlannerStep.EDIT, vm.state.value.step)
        assertTrue(vm.state.value.showIssues)
        assertFalse(isGenerateEnabled(vm.state.value))
    }

    @Test
    fun issuePlanLocatesOverlapTarget() {
        val vm = RoomPlannerViewModel()

        vm.onEvent(RoomPlannerEvent.InjectIssuePlan)
        vm.onEvent(RoomPlannerEvent.LocateIssue(PlannerIssueKind.OVERLAP))

        assertEquals("sample-door-duplicate", vm.state.value.selected?.id)
    }

    @Test
    fun offlineDemoSwitchRoutesToOfflineDialogAndRetryConnects() {
        val vm = RoomPlannerViewModel()
        enterEditStep(vm)

        vm.onEvent(RoomPlannerEvent.ToggleOfflineSimulated)
        assertTrue(vm.state.value.simulateOffline)

        vm.onEvent(RoomPlannerEvent.GenerateRoom)

        assertEquals(PlannerDialog.OFFLINE, vm.state.value.dialog)
        assertEquals(TOAST_LOCAL_DRAFT, vm.state.value.toast)

        vm.onEvent(RoomPlannerEvent.RetryConnection)

        assertFalse(vm.state.value.simulateOffline)
        assertNull(vm.state.value.dialog)
        assertEquals(PlannerStep.GENERATING, vm.state.value.step)

        vm.onEvent(RoomPlannerEvent.GenerationCompleted)
        assertEquals(PlannerStep.RESULT, vm.state.value.step)
    }

    @Test
    fun generationErrorSwitchRoutesToErrorThenRetrySucceeds() {
        val vm = RoomPlannerViewModel()
        enterEditStep(vm)

        vm.onEvent(RoomPlannerEvent.ToggleGenerationErrorSimulated)
        vm.onEvent(RoomPlannerEvent.GenerateRoom)
        vm.onEvent(RoomPlannerEvent.GenerationCompleted)

        assertEquals(PlannerStep.ERROR, vm.state.value.step)

        vm.onEvent(RoomPlannerEvent.RetryGeneration)
        assertEquals(PlannerStep.GENERATING, vm.state.value.step)

        vm.onEvent(RoomPlannerEvent.ToggleGenerationErrorSimulated)
        vm.onEvent(RoomPlannerEvent.GenerationCompleted)
        assertEquals(PlannerStep.RESULT, vm.state.value.step)
    }

    @Test
    fun saveFailureSimulationShowsDialogAndRetrySaves() {
        val vm = RoomPlannerViewModel()
        enterEditStep(vm)

        vm.onEvent(RoomPlannerEvent.ToggleSaveFailureSimulated)
        vm.onEvent(
            RoomPlannerEvent.SelectElement(PlannerSelection("sample-north", PlannerSelectionType.WALL))
        )
        vm.onEvent(RoomPlannerEvent.AddDoorOnSelection)

        assertEquals(PlannerDialog.SAVE_FAILED, vm.state.value.dialog)
        assertEquals(TOAST_SAVE_FAILED, vm.state.value.toast)
        assertEquals(
            RoomOpeningKind.DOOR,
            vm.state.value.generatedPlan.openings.last().kind
        )

        vm.onEvent(RoomPlannerEvent.RetrySave)

        assertNull(vm.state.value.dialog)
        assertEquals(TOAST_LOCAL_SAVED, vm.state.value.toast)
    }

    @Test
    fun restoreDraftRestoresLastSnapshot() {
        val vm = RoomPlannerViewModel()
        enterEditStep(vm)
        val editedWalls = vm.state.value.generatedPlan.walls.size

        vm.onEvent(RoomPlannerEvent.AddWallAt(0f, 0f))
        vm.onEvent(RoomPlannerEvent.StartImport(FloorPlanInputSource.UPLOAD))
        assertEquals(PlannerStep.PROCESSING, vm.state.value.step)
        assertNotNull(vm.state.value.draftPlan)

        vm.onEvent(RoomPlannerEvent.ShowDialog(PlannerDialog.RESTORE_DRAFT))
        assertEquals(PlannerDialog.RESTORE_DRAFT, vm.state.value.dialog)
        assertEquals(TOAST_LOCAL_DRAFT, vm.state.value.toast)

        vm.onEvent(RoomPlannerEvent.RestoreDraft)

        assertEquals(PlannerStep.EDIT, vm.state.value.step)
        assertEquals(editedWalls + 1, vm.state.value.generatedPlan.walls.size)
    }

    @Test
    fun doorWidthBoundsFollowSpec() {
        assertEquals(0.10f, DOOR_WIDTH_MIN_METERS, 1e-4f)
        assertEquals(2.40f, DOOR_WIDTH_MAX_METERS, 1e-4f)
    }

    @Test
    fun toggleDoorSwingFlipsOpening() {
        val vm = RoomPlannerViewModel()
        enterEditStep(vm)

        vm.onEvent(
            RoomPlannerEvent.SelectElement(PlannerSelection("sample-door-entry", PlannerSelectionType.DOOR))
        )
        vm.onEvent(RoomPlannerEvent.ToggleDoorSwing)

        val door = vm.state.value.generatedPlan.openings.first { it.id == "sample-door-entry" }
        assertTrue(door.swingFlipped)
    }
}
