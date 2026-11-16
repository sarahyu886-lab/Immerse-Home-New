package com.pico.welcomespace.ui.planner

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.platform.containers.OpenStageResult
import com.pico.spatial.ui.platform.containers.StageStyle
import com.pico.spatial.ui.platform.containers.openStage
import com.pico.welcomespace.domain.model.RoomOpeningKind
import com.pico.welcomespace.domain.model.RoomSurfaceKind
import com.pico.welcomespace.domain.model.VirtualRoomPlan
import com.pico.welcomespace.domain.usecase.FloorPlanInputSource
import com.pico.welcomespace.ui.shop.RoomPlacement
import kotlinx.coroutines.launch

@Composable
fun RoomPlannerScreen(
    modifier: Modifier = Modifier,
    viewModel: RoomPlannerViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showImportOptions by remember { mutableStateOf(false) }
    val pickFloorPlan = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.onEvent(RoomPlannerEvent.FloorPlanRecognized(FloorPlanInputSource.UPLOAD))
        }
    }
    val captureFloorPlan = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            viewModel.onEvent(RoomPlannerEvent.FloorPlanRecognized(FloorPlanInputSource.CAMERA))
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        RoomPlannerContent(
            state = state,
            onEvent = { event ->
                viewModel.onEvent(event)
                if (event is RoomPlannerEvent.ApplyPreview) {
                    scope.launch {
                        runCatching {
                            if (context.openStage("FurnitureRoom", style = StageStyle.Mixed) !is
                                OpenStageResult.Allowed
                            ) {
                                RoomPlacement.report("虚拟样板间不可用，请检查 Full Stage 能力")
                            }
                        }.onFailure {
                            RoomPlacement.report("无法打开虚拟样板间，请在支持 Stage 的环境运行")
                        }
                    }
                }
            },
            onImportClick = { showImportOptions = true },
            modifier = Modifier.fillMaxSize()
        )
        if (showImportOptions) {
            FloorPlanImportOptions(
                onUpload = {
                    showImportOptions = false
                    pickFloorPlan.launch("image/*")
                },
                onCamera = {
                    showImportOptions = false
                    captureFloorPlan.launch(null)
                },
                onDismiss = { showImportOptions = false },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
internal fun RoomPlannerContent(
    state: RoomPlannerUiState,
    onEvent: (RoomPlannerEvent) -> Unit,
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 26.dp, end = 26.dp, top = 26.dp, bottom = 118.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        PlanPreviewCard(
            plan = state.generatedPlan,
            modifier = Modifier.weight(1f),
        )
        PlannerActionBar(onImportClick = onImportClick, onEvent = onEvent)
    }
}

@Composable
private fun PlanPreviewCard(plan: VirtualRoomPlan, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(28.dp))
            .background(PicoTheme.colorScheme.fillSecondary)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        FloorPlanCanvas(plan = plan, modifier = Modifier.fillMaxWidth().weight(1f))
    }
}

@Composable
private fun FloorPlanCanvas(plan: VirtualRoomPlan, modifier: Modifier = Modifier) {
    val wallColor = PicoTheme.colorScheme.labelPrimary
    val floorColor = PicoTheme.colorScheme.interaction.copy(alpha = 0.18f)
    val tableColor = Color(0xFFD8A45D)
    val doorColor = Color(0xFFFFC16A)
    val windowColor = Color(0xFF7EC8FF)
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(PicoTheme.colorScheme.fillTertiary)
            .padding(16.dp)
    ) {
        val contentWidth = size.width - 48.dp.toPx()
        val contentHeight = size.height - 48.dp.toPx()
        val scale = minOf(
            contentWidth / plan.widthMeters.coerceAtLeast(0.1f),
            contentHeight / plan.depthMeters.coerceAtLeast(0.1f)
        )
        val origin = Offset(size.width / 2f, size.height / 2f)
        fun x(value: Float) = origin.x + value * scale
        fun z(value: Float) = origin.y + value * scale

        plan.surfaces.forEach { surface ->
            drawRoundRect(
                color = if (surface.kind == RoomSurfaceKind.TABLE) tableColor.copy(alpha = 0.42f) else floorColor,
                topLeft = Offset(
                    x(surface.centerX - surface.width / 2f),
                    z(surface.centerZ - surface.depth / 2f)
                ),
                size = Size(surface.width * scale, surface.depth * scale)
            )
        }
        plan.walls.forEach { wall ->
            drawLine(
                color = wallColor,
                start = Offset(x(wall.centerX - wall.width / 2f), z(wall.centerZ - wall.depth / 2f)),
                end = Offset(x(wall.centerX + wall.width / 2f), z(wall.centerZ + wall.depth / 2f)),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        plan.openings.forEach { opening ->
            val isHorizontal = opening.width >= opening.depth
            val start = if (isHorizontal) {
                Offset(x(opening.centerX - opening.width / 2f), z(opening.centerZ))
            } else {
                Offset(x(opening.centerX), z(opening.centerZ - opening.depth / 2f))
            }
            val end = if (isHorizontal) {
                Offset(x(opening.centerX + opening.width / 2f), z(opening.centerZ))
            } else {
                Offset(x(opening.centerX), z(opening.centerZ + opening.depth / 2f))
            }
            drawLine(
                color = if (opening.kind == RoomOpeningKind.WINDOW) windowColor else doorColor,
                start = start,
                end = end,
                strokeWidth = if (opening.kind == RoomOpeningKind.WINDOW) 3.dp.toPx() else 5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        drawRect(
            color = wallColor.copy(alpha = 0.28f),
            topLeft = Offset(
                x(-plan.widthMeters / 2f),
                z(-plan.depthMeters / 2f)
            ),
            size = Size(plan.widthMeters * scale, plan.depthMeters * scale),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

@Composable
private fun PlannerActionBar(
    onImportClick: () -> Unit,
    onEvent: (RoomPlannerEvent) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(PicoTheme.colorScheme.fillSecondary)
            .padding(horizontal = 24.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = onImportClick) {
            Text("上传或识别户型图")
        }
        Spacer(modifier = Modifier.width(14.dp))
        Button(onClick = { onEvent(RoomPlannerEvent.ApplyPreview) }) {
            Text("应用并生成房间")
        }
    }
}

@Composable
private fun FloorPlanImportOptions(
    onUpload: () -> Unit,
    onCamera: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(bottom = 194.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(PicoTheme.colorScheme.fillSecondary.copy(alpha = 0.96f))
            .padding(18.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(onClick = onUpload) {
            Text("选择户型图文件")
        }
        Button(onClick = onCamera) {
            Text("打开摄像头识别")
        }
        Button(onClick = onDismiss, modifier = Modifier.size(52.dp), shape = CircleShape) {
            Text("×")
        }
    }
}
