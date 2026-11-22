package com.pico.welcomespace.ui.planner

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.ButtonDefaults
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
import com.pico.welcomespace.ui.planner.components.StandardRoomOverview
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

private val PlannerGlass = Color(0x94FFF8ED) // design-style: fixed-figma-color 保留现有 Figma 面板配色。
private val PlannerGlassStrong = Color(0xB3FFF8ED) // design-style: fixed-figma-color 保留现有 Figma 面板配色。
private val PlannerAccent = Color(0xE6FFBF4A) // design-style: fixed-figma-color 用户指定黄色主操作。
private val PlannerText = Color(0xFF292722) // design-style: fixed-figma-color 保留现有浅色面板前景。
private val PlannerTextMuted = Color(0xFF6E655B) // design-style: fixed-figma-color 保留现有浅色面板前景。
private val PlannerBorder = Color(0xA6FFFFFF) // design-style: fixed-figma-color 保留现有面板描边。
private val PlannerDanger = Color(0xFFAD5F4B) // design-style: fixed-figma-color 保留原设计删除提示色。

@Composable
fun RoomPlannerScreen(
    modifier: Modifier = Modifier,
    onNavigateToShop: () -> Unit = {},
    viewModel: RoomPlannerViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Box(modifier = modifier.fillMaxSize()) {
        PlannerShell(
            title = "我的户型",
            statusMessage = "",
            onNavigateToShop = onNavigateToShop,
            modifier = Modifier.fillMaxSize()
        ) {
            StandardRoomOverview(
                plan = RoomPlannerSession.activePlan,
                onEnterRoom = {
                    scope.launch {
                        runCatching {
                            if (context.openStage("FurnitureRoom", style = StageStyle.Mixed) !is OpenStageResult.Allowed) {
                                RoomPlacement.report("虚拟样板间不可用，请检查 Full Stage 能力")
                            }
                        }.onFailure {
                            RoomPlacement.report("无法打开虚拟样板间，请在支持 Stage 的环境运行")
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun PlannerShell(
    title: String,
    statusMessage: String,
    onNavigateToShop: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f)
        ) {
            GlassPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.075f)
                    .align(Alignment.TopCenter),
                radius = 18.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Immerse Home  /  $title", color = PlannerText)
                }
            }
            GlassPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.80f)
                    .align(Alignment.TopCenter)
                    .padding(top = 54.dp),
                radius = 18.dp
            ) {
                content()
            }
            InternalBottomTabs(
                onNavigateToShop = onNavigateToShop,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 56.dp)
            )
        }
    }
}

@Composable
private fun UploadStep(
    onImportClick: () -> Unit,
    onShowDialog: (PlannerDialog) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 34.dp, vertical = 30.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassPanel(
                modifier = Modifier
                    .weight(0.43f)
                    .fillMaxHeight(0.8f),
                strong = true,
                radius = 20.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    UploadIllustration()
                }
            }
            Column(
                modifier = Modifier.weight(0.57f),
                verticalArrangement = Arrangement.Center
            ) {
                Text("让家的轮廓，\n变成立体空间", color = PlannerText)
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    "上传户型图，在图上调整墙、门和窗。\n确认尺寸后，生成可以进入的模拟房间。",
                    color = PlannerText.copy(alpha = 0.88f)
                )
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    "支持 JPG / PNG · 建议使用清晰的正视平面图",
                    color = PlannerTextMuted
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "上传平面图    →    图上编辑    →    生成房间",
                color = PlannerText,
                modifier = Modifier.weight(1f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PlannerSecondaryButton(text = "上传户型图", onClick = onImportClick)
                PlannerSecondaryButton(text = "生成模拟房间", onClick = {}, enabled = false)
            }
        }
    }
}

@Composable
private fun CalibrateStep(
    plan: VirtualRoomPlan,
    onImportClick: () -> Unit,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 34.dp, vertical = 26.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("校准真实尺寸", color = PlannerText)
        FloorPlanCanvas(
            plan = plan,
            selection = null,
            pan = Offset.Zero,
            modifier = Modifier.weight(1f).fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(label = "宽度", value = String.format("%.2f m", plan.widthMeters))
                MetricCard(label = "进深", value = String.format("%.2f m", plan.depthMeters))
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PlannerSecondaryButton(text = "重新上传", onClick = onImportClick)
                PlannerPrimaryButton(text = "开始图上编辑", onClick = onContinue)
            }
        }
    }
}

@Composable
private fun EditStep(
    state: RoomPlannerUiState,
    onEvent: (RoomPlannerEvent) -> Unit,
    onImportClick: () -> Unit
) {
    var pan by remember(state.generatedPlan) { mutableStateOf(Offset.Zero) }
    val issues = remember(state.generatedPlan) { buildPlanIssues(state.generatedPlan) }
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FloorPlanCanvas(
            plan = state.generatedPlan,
            selection = state.selected,
            pan = pan,
            onTap = { x, z -> onEvent(RoomPlannerEvent.SelectElementAt(x, z)) },
            onLongPress = { x, z -> onEvent(RoomPlannerEvent.AddWallAt(x, z)) },
            onPan = { delta -> pan += delta },
            modifier = Modifier.weight(1f).fillMaxWidth()
        )
        Text(
            "点击墙或门窗开始编辑 · 长按空白添加墙 · 拖空白移动视图",
            color = PlannerText,
            modifier = Modifier.padding(start = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ToolSelectorRow(selectedTool = state.tool, onSelect = { onEvent(RoomPlannerEvent.SelectTool(it)) })
                PlannerSecondaryButton(
                    text = adjustmentDecreaseLabel(state.selected),
                    onClick = { onEvent(RoomPlannerEvent.AdjustSelection(-0.2f)) },
                    enabled = state.selected != null
                )
                PlannerPrimaryButton(
                    text = adjustmentIncreaseLabel(state.selected),
                    onClick = { onEvent(RoomPlannerEvent.AdjustSelection(0.2f)) },
                    enabled = state.selected != null
                )
                PlannerSecondaryButton(
                    text = "删除",
                    onClick = { onEvent(RoomPlannerEvent.RequestDeleteSelection) },
                    enabled = state.selected != null
                )
                PlannerSecondaryButton(
                    text = "撤销",
                    onClick = { onEvent(RoomPlannerEvent.UndoLastChange) },
                    enabled = state.canUndo
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "待确认项 ${issues.size} 个 · 演示数据不会调用真实识别接口",
                color = PlannerTextMuted,
                modifier = Modifier.weight(1f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PlannerSecondaryButton(text = "上传户型图", onClick = onImportClick)
                PlannerPrimaryButton(text = "生成模拟房间", onClick = { onEvent(RoomPlannerEvent.GenerateRoom) })
            }
        }
    }
}

@Composable
private fun ResultStep(
    plan: VirtualRoomPlan,
    onReturn: () -> Unit,
    onEnterRoom: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 34.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("模拟房间已就绪", color = PlannerText)
        FloorPlanCanvas(
            plan = plan,
            selection = null,
            pan = Offset.Zero,
            modifier = Modifier.weight(1f).fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(label = "墙体", value = plan.walls.size.toString())
                MetricCard(label = "门窗", value = plan.openings.size.toString())
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PlannerSecondaryButton(text = "继续编辑", onClick = onReturn)
                PlannerPrimaryButton(text = "进入房间", onClick = onEnterRoom)
            }
        }
    }
}

@Composable
private fun BusyStep(
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            LoadingRing()
            Text(title, color = PlannerText)
            Text(body, color = PlannerTextMuted)
            PlannerSecondaryButton(text = actionLabel, onClick = onAction)
        }
    }
}

@Composable
private fun InternalBottomTabs(
    onNavigateToShop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlannerPrimaryButton(text = "我的户型", onClick = {})
        PlannerSecondaryButton(text = "选家具", onClick = onNavigateToShop)
    }
}

@Composable
private fun ToolSelectorRow(
    selectedTool: PlannerTool,
    onSelect: (PlannerTool) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PlannerTool.entries.forEach { tool ->
            val selected = tool == selectedTool
            Button(
                onClick = { onSelect(tool) },
                colors = if (selected) primaryButtonColors() else secondaryButtonColors()
            ) {
                Text(tool.label, color = if (selected) PlannerText else PlannerTextMuted)
            }
        }
    }
}

@Composable
private fun EditActionRow(
    state: RoomPlannerUiState,
    onAdjust: (Float) -> Unit,
    onDelete: () -> Unit,
    onUndo: () -> Unit,
    onShowIssues: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlannerSecondaryButton(
            text = adjustmentDecreaseLabel(state.selected),
            onClick = { onAdjust(-0.2f) },
            enabled = state.selected != null
        )
        PlannerPrimaryButton(
            text = adjustmentIncreaseLabel(state.selected),
            onClick = { onAdjust(0.2f) },
            enabled = state.selected != null
        )
        PlannerSecondaryButton(
            text = "删除",
            onClick = onDelete,
            enabled = state.selected != null
        )
        PlannerSecondaryButton(
            text = "撤销",
            onClick = onUndo,
            enabled = state.canUndo
        )
        PlannerSecondaryButton(text = "检查问题", onClick = onShowIssues)
    }
}

@Composable
private fun FooterActions(
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String,
    onSecondary: () -> Unit,
    secondaryEnabled: Boolean = true
) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        PlannerSecondaryButton(
            text = secondaryLabel,
            onClick = onSecondary,
            enabled = secondaryEnabled
        )
        PlannerPrimaryButton(text = primaryLabel, onClick = onPrimary)
    }
}

@Composable
private fun DemoActionsRow(actions: List<Pair<String, () -> Unit>>) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        actions.take(6).forEach { (label, onClick) ->
            PlannerSecondaryButton(text = label, onClick = onClick)
        }
    }
}

@Composable
private fun GlassPanel(
    modifier: Modifier = Modifier,
    strong: Boolean = false,
    radius: androidx.compose.ui.unit.Dp = 24.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(radius))
            .background(if (strong) PlannerGlassStrong else PlannerGlass)
            .padding(1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(radius))
                .background(Color.Transparent),
            content = content
        )
    }
}

@Composable
private fun PlannerPrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Button(onClick = onClick, enabled = enabled, colors = primaryButtonColors()) {
        Text(text, color = PlannerText)
    }
}

@Composable
private fun PlannerSecondaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Button(onClick = onClick, enabled = enabled, colors = secondaryButtonColors()) {
        Text(text, color = PlannerTextMuted)
    }
}

@Composable
private fun primaryButtonColors() = ButtonDefaults.buttonColors(
    containerColor = PlannerAccent,
    contentColor = PlannerText
)

@Composable
private fun secondaryButtonColors() = ButtonDefaults.buttonColors(
    containerColor = Color.Transparent,
    contentColor = PlannerTextMuted
)

@Composable
private fun UploadIllustration() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val stroke = 10.dp.toPx()
        val panel = Rect(
            left = size.width * 0.12f,
            top = size.height * 0.12f,
            right = size.width * 0.88f,
            bottom = size.height * 0.78f
        )
        drawRoundRect(
            color = PlannerBorder,
            topLeft = panel.topLeft,
            size = panel.size,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(26.dp.toPx(), 26.dp.toPx()),
            style = Stroke(width = stroke)
        )
        drawLine(
            color = PlannerTextMuted,
            start = Offset(panel.left + panel.width * 0.22f, panel.top + panel.height * 0.48f),
            end = Offset(panel.right - panel.width * 0.22f, panel.top + panel.height * 0.48f),
            strokeWidth = stroke * 0.8f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = PlannerTextMuted,
            start = Offset(panel.center.x, panel.top + panel.height * 0.22f),
            end = Offset(panel.center.x, panel.bottom - panel.height * 0.18f),
            strokeWidth = stroke * 0.8f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = PlannerAccent,
            start = Offset(panel.center.x, panel.top + panel.height * 0.18f),
            end = Offset(panel.center.x - 34.dp.toPx(), panel.top + panel.height * 0.32f),
            strokeWidth = stroke * 0.8f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = PlannerAccent,
            start = Offset(panel.center.x, panel.top + panel.height * 0.18f),
            end = Offset(panel.center.x + 34.dp.toPx(), panel.top + panel.height * 0.32f),
            strokeWidth = stroke * 0.8f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun MetricCard(label: String, value: String) {
    GlassPanel(radius = 18.dp) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            Text(label, color = PlannerTextMuted)
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, color = PlannerText)
        }
    }
}

@Composable
private fun LoadingRing() {
    Canvas(modifier = Modifier.size(84.dp)) {
        val stroke = 8.dp.toPx()
        drawArc(
            color = PlannerAccent,
            startAngle = -90f,
            sweepAngle = 275f,
            useCenter = false,
            topLeft = Offset(stroke, stroke),
            size = Size(size.width - stroke * 2f, size.height - stroke * 2f),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun PlannerDialogOverlay(
    state: RoomPlannerUiState,
    onDismiss: () -> Unit,
    onUpload: () -> Unit,
    onCamera: () -> Unit,
    onConfirmReplace: () -> Unit,
    onConfirmDelete: () -> Unit,
    onRestoreDraft: () -> Unit,
    plan: VirtualRoomPlan
) {
    val dialog = state.dialog ?: return
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        GlassPanel(
            modifier = Modifier.width(620.dp),
            strong = true,
            radius = 26.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (dialog) {
                    PlannerDialog.IMPORT_OPTIONS -> {
                        DialogHeading("选择户型来源", "可以从本地图片或摄像头导入。当前识别结果仍为演示数据。")
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            PlannerPrimaryButton(text = "上传户型图", onClick = onUpload)
                            PlannerSecondaryButton(text = "打开摄像头", onClick = onCamera)
                            PlannerSecondaryButton(text = "取消", onClick = onDismiss)
                        }
                    }
                    PlannerDialog.REPLACE_CURRENT_PLAN -> {
                        DialogHeading("替换当前户型？", "当前编辑会先保存为草稿，替换后仍可在同一会话中恢复。")
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            PlannerPrimaryButton(text = "继续替换", onClick = onConfirmReplace)
                            PlannerSecondaryButton(text = "继续编辑", onClick = onDismiss)
                        }
                    }
                    PlannerDialog.DELETE_SELECTION -> {
                        DialogHeading("删除当前选中项", "删除后仍可使用撤销恢复。")
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            PlannerPrimaryButton(text = "确认删除", onClick = onConfirmDelete)
                            PlannerSecondaryButton(text = "取消", onClick = onDismiss)
                        }
                    }
                    PlannerDialog.INVALID_IMAGE -> {
                        DialogHeading("无效图片", "请上传清晰的平面户型图。当前版本不会真实识别图片内容。")
                        PlannerSecondaryButton(text = "知道了", onClick = onDismiss)
                    }
                    PlannerDialog.UPLOAD_FAILED -> {
                        DialogHeading("上传失败", "图片没有成功写入本地缓存，请重新选择文件。")
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            PlannerPrimaryButton(text = "重新上传", onClick = onDismiss)
                            PlannerSecondaryButton(text = "取消", onClick = onDismiss)
                        }
                    }
                    PlannerDialog.OFFLINE -> {
                        DialogHeading("当前已离线", "可以继续本地编辑。在线识别暂不可用，原图已保留。")
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            PlannerPrimaryButton(text = "继续编辑", onClick = onDismiss)
                            PlannerSecondaryButton(text = "重试连接", onClick = onDismiss)
                        }
                    }
                    PlannerDialog.SAVE_FAILED -> {
                        DialogHeading("保存失败", "修改仍保留在当前会话。请不要退出应用，重试保存。")
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            PlannerPrimaryButton(text = "继续编辑", onClick = onDismiss)
                            PlannerSecondaryButton(text = "稍后重试", onClick = onDismiss)
                        }
                    }
                    PlannerDialog.INVALID_DOOR_SIZE -> {
                        DialogHeading("门宽超出可用墙段", "请输入 0.10–2.40 米，或选择更长的墙。当前输入未丢失。")
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            PlannerPrimaryButton(text = "返回输入", onClick = onDismiss)
                            PlannerSecondaryButton(text = "取消修改", onClick = onDismiss)
                        }
                    }
                    PlannerDialog.GENERATE_FAILED -> {
                        DialogHeading("生成失败", "模拟房间没有生成成功，请保留当前编辑并重新尝试。")
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            PlannerPrimaryButton(text = "重试生成", onClick = onDismiss)
                            PlannerSecondaryButton(text = "继续编辑", onClick = onDismiss)
                        }
                    }
                    PlannerDialog.RESTORE_DRAFT -> {
                        DialogHeading("恢复上次编辑？", "发现上次保存的户型草稿，可以恢复墙体及门窗修改。")
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            PlannerPrimaryButton(text = "恢复草稿", onClick = onRestoreDraft)
                            PlannerSecondaryButton(text = "稍后恢复", onClick = onDismiss)
                        }
                    }
                    PlannerDialog.PLAN_ISSUES -> {
                        DialogHeading("检查户型问题", "以下项目来自当前演示户型，请在生成前人工确认。")
                        buildPlanIssues(plan).forEach { issue ->
                            Text("• $issue", color = PlannerTextMuted)
                        }
                        PlannerSecondaryButton(text = "关闭", onClick = onDismiss)
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogHeading(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = PlannerText)
        Text(body, color = PlannerTextMuted)
    }
}

@Composable
private fun FloorPlanCanvas(
    plan: VirtualRoomPlan,
    selection: PlannerSelection?,
    pan: Offset,
    modifier: Modifier = Modifier,
    onTap: ((Float, Float) -> Unit)? = null,
    onLongPress: ((Float, Float) -> Unit)? = null,
    onPan: ((Offset) -> Unit)? = null
) {
    val wallColor = PicoTheme.colorScheme.labelPrimary
    val floorColor = PicoTheme.colorScheme.interaction.copy(alpha = 0.14f)
    val tableColor = PicoTheme.colorScheme.fillSecondary
    val doorColor = PicoTheme.colorScheme.interaction
    val windowColor = PicoTheme.colorScheme.labelSecondary
    val selectedColor = PlannerAccent
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.36f))
            .pointerInput(plan, pan) {
                detectTapGestures(
                    onTap = { offset ->
                        onTap?.invoke(mapOffsetToPlanX(offset, size, plan, pan), mapOffsetToPlanZ(offset, size, plan, pan))
                    },
                    onLongPress = { offset ->
                        onLongPress?.invoke(mapOffsetToPlanX(offset, size, plan, pan), mapOffsetToPlanZ(offset, size, plan, pan))
                    }
                )
            }
            .pointerInput(plan, pan) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onPan?.invoke(dragAmount)
                }
            }
            .padding(16.dp)
    ) {
        val transform = rememberTransform(plan, size, pan)
        fun x(value: Float) = transform.origin.x + value * transform.scale
        fun z(value: Float) = transform.origin.y - value * transform.scale
        fun band(centerX: Float, centerZ: Float, widthPx: Float, depthPx: Float) = Rect(
            left = x(centerX) - widthPx / 2f,
            top = z(centerZ) - depthPx / 2f,
            right = x(centerX) + widthPx / 2f,
            bottom = z(centerZ) + depthPx / 2f
        )

        val minWallThickness = 2.5.dp.toPx()
        val thinStroke = 1.5.dp.toPx()
        val boldStroke = 2.5.dp.toPx()

        plan.surfaces.forEach { surface ->
            val rect = band(surface.centerX, surface.centerZ, surface.width * transform.scale, surface.depth * transform.scale)
            drawRect(
                color = if (surface.kind == RoomSurfaceKind.TABLE) tableColor.copy(alpha = 0.42f) else floorColor,
                topLeft = rect.topLeft,
                size = rect.size
            )
        }

        plan.walls.forEach { wall ->
            val isHorizontal = wall.width >= wall.depth
            val lengthPx = max(wall.width, wall.depth) * transform.scale
            val thicknessPx = max(minOf(wall.width, wall.depth) * transform.scale, minWallThickness)
            val rect = band(
                centerX = wall.centerX,
                centerZ = wall.centerZ,
                widthPx = if (isHorizontal) lengthPx else thicknessPx,
                depthPx = if (isHorizontal) thicknessPx else lengthPx
            )
            drawRect(
                color = if (selection?.id == wall.id && selection.type == PlannerSelectionType.WALL) {
                    selectedColor
                } else {
                    wallColor
                },
                topLeft = rect.topLeft,
                size = rect.size
            )
        }

        plan.openings.forEach { opening ->
            val isHorizontal = opening.width >= opening.depth
            val lengthPx = max(opening.width, opening.depth) * transform.scale
            val centerX = x(opening.centerX)
            val centerZ = z(opening.centerZ)
            val isSelected = selection?.id == opening.id
            val color = if (isSelected) selectedColor else if (opening.kind == RoomOpeningKind.WINDOW) windowColor else doorColor
            if (opening.kind == RoomOpeningKind.WINDOW) {
                val half = lengthPx / 2f
                listOf(-1f, 0f, 1f).forEach { step ->
                    val offset = step * thinStroke
                    val start = if (isHorizontal) {
                        Offset(centerX - half, centerZ + offset)
                    } else {
                        Offset(centerX + offset, centerZ - half)
                    }
                    val end = if (isHorizontal) {
                        Offset(centerX + half, centerZ + offset)
                    } else {
                        Offset(centerX + offset, centerZ + half)
                    }
                    drawLine(color, start, end, strokeWidth = if (isSelected) boldStroke else thinStroke, cap = StrokeCap.Round)
                }
            } else {
                val hinge = if (isHorizontal) {
                    Offset(centerX - lengthPx / 2f, centerZ)
                } else {
                    Offset(centerX, centerZ - lengthPx / 2f)
                }
                val tip = if (isHorizontal) {
                    Offset(hinge.x, hinge.y + lengthPx)
                } else {
                    Offset(hinge.x + lengthPx, hinge.y)
                }
                drawLine(color, hinge, tip, strokeWidth = boldStroke, cap = StrokeCap.Round)
                drawArc(
                    color = color,
                    startAngle = 0f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(hinge.x - lengthPx, hinge.y - lengthPx),
                    size = Size(lengthPx * 2f, lengthPx * 2f),
                    style = Stroke(width = if (isSelected) boldStroke + 1.dp.toPx() else boldStroke)
                )
            }
        }

        drawRect(
            color = wallColor.copy(alpha = 0.28f),
            topLeft = Offset(x(-plan.widthMeters / 2f), z(plan.depthMeters / 2f)),
            size = Size(plan.widthMeters * transform.scale, plan.depthMeters * transform.scale),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

private data class CanvasTransform(
    val scale: Float,
    val origin: Offset
)

private fun rememberTransform(plan: VirtualRoomPlan, size: Size, pan: Offset): CanvasTransform {
    val inset = 24f
    val contentWidth = size.width - inset * 2f
    val contentHeight = size.height - inset * 2f
    val scale = minOf(
        contentWidth / plan.widthMeters.coerceAtLeast(0.1f),
        contentHeight / plan.depthMeters.coerceAtLeast(0.1f)
    )
    return CanvasTransform(
        scale = scale,
        origin = Offset(size.width / 2f + pan.x, size.height / 2f + pan.y)
    )
}

private fun mapOffsetToPlanX(offset: Offset, size: IntSize, plan: VirtualRoomPlan, pan: Offset): Float {
    val transform = rememberTransform(plan, Size(size.width.toFloat(), size.height.toFloat()), pan)
    return (offset.x - transform.origin.x) / transform.scale
}

private fun mapOffsetToPlanZ(offset: Offset, size: IntSize, plan: VirtualRoomPlan, pan: Offset): Float {
    val transform = rememberTransform(plan, Size(size.width.toFloat(), size.height.toFloat()), pan)
    return (transform.origin.y - offset.y) / transform.scale
}

private fun plannerTitle(step: PlannerStep): String = when (step) {
    PlannerStep.UPLOAD -> "上传你的户型"
    PlannerStep.PROCESSING -> "正在处理户型"
    PlannerStep.CALIBRATE -> "校准真实尺寸"
    PlannerStep.EDIT -> "直接在图上编辑"
    PlannerStep.GENERATING -> "生成模拟房间"
    PlannerStep.RESULT -> "模拟房间已就绪"
}

private fun adjustmentIncreaseLabel(selection: PlannerSelection?): String = when (selection?.type) {
    PlannerSelectionType.WALL -> "拉长"
    PlannerSelectionType.DOOR -> "加宽"
    PlannerSelectionType.WINDOW -> "加宽"
    null -> "加宽"
}

private fun adjustmentDecreaseLabel(selection: PlannerSelection?): String = when (selection?.type) {
    PlannerSelectionType.WALL -> "缩短"
    PlannerSelectionType.DOOR -> "缩窄"
    PlannerSelectionType.WINDOW -> "缩窄"
    null -> "缩窄"
}

private fun buildPlanIssues(plan: VirtualRoomPlan): List<String> {
    val issues = mutableListOf<String>()
    issues += "当前结果来自本地演示数据，请人工确认墙、门、窗位置。"
    val duplicateWalls = plan.walls.groupBy { listOf(it.centerX, it.centerZ, it.width, it.depth) }.count { it.value.size > 1 }
    if (duplicateWalls > 0) {
        issues += "检测到 $duplicateWalls 组重叠墙体，生成前建议继续核对。"
    }
    val duplicateOpenings = plan.openings.groupBy { listOf(it.kind, it.centerX, it.centerZ, it.width, it.depth) }.count { it.value.size > 1 }
    if (duplicateOpenings > 0) {
        issues += "检测到 $duplicateOpenings 组重叠门窗，可能需要手动调整。"
    }
    if (plan.openings.none { it.kind == RoomOpeningKind.DOOR }) {
        issues += "当前户型没有门，请确认是否遗漏。"
    }
    if (plan.openings.none { it.kind == RoomOpeningKind.WINDOW }) {
        issues += "当前户型没有窗，请确认是否遗漏。"
    }
    return issues
}
