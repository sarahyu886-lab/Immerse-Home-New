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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pico.spatial.ui.platform.containers.OpenStageResult
import com.pico.spatial.ui.platform.containers.StageStyle
import com.pico.spatial.ui.platform.containers.openStage
import com.pico.spatial.ui.design.Text
import com.pico.welcomespace.R
import com.pico.welcomespace.domain.model.FloorPlanRoom
import com.pico.welcomespace.domain.model.VirtualRoomPlan
import com.pico.welcomespace.domain.usecase.FloorPlanInputSource
import com.pico.welcomespace.ui.common.ImmerseHomeLogo
import com.pico.welcomespace.ui.planner.components.HfDangerButton
import com.pico.welcomespace.ui.planner.components.HfDemoLink
import com.pico.welcomespace.ui.planner.components.HfGlassPanel
import com.pico.welcomespace.ui.planner.components.HfNavPill
import com.pico.welcomespace.ui.planner.components.HfNumberInput
import com.pico.welcomespace.ui.planner.components.HfPrimaryButton
import com.pico.welcomespace.ui.planner.components.HfSecondaryButton
import com.pico.welcomespace.ui.planner.components.HfToast
import com.pico.welcomespace.ui.planner.components.HfToolPanel
import com.pico.welcomespace.ui.planner.components.HfType
import com.pico.welcomespace.ui.shop.RoomPlacement
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min

@Composable
fun RoomPlannerScreen(
    viewModel: RoomPlannerViewModel = remember { RoomPlannerViewModel() },
    onNavigateToShop: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val enterRoom: () -> Unit = {
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

    // 自动流转：P03 处理中 / P12 生成中（模拟延时完成）
    LaunchedEffect(state.step) {
        when (state.step) {
            PlannerStep.PROCESSING -> {
                delay(1600)
                viewModel.onEvent(RoomPlannerEvent.ProcessingCompleted)
            }
            PlannerStep.GENERATING -> {
                delay(2200)
                viewModel.onEvent(RoomPlannerEvent.GenerationCompleted)
            }
            else -> Unit
        }
    }
    // toast 自动消失
    LaunchedEffect(state.toast) {
        if (state.toast != null) {
            delay(2800)
            viewModel.onEvent(RoomPlannerEvent.HideToast)
        }
    }

    PlannerShell(
        title = state.step.title,
        state = state,
        onEvent = viewModel::onEvent,
        onNavigateToShop = onNavigateToShop,
        onEnterRoom = enterRoom
    ) { onEvent ->
        when (state.step) {
            PlannerStep.UPLOAD -> UploadStepContent()
            PlannerStep.SELECT_SOURCE -> SelectSourceStepContent(onEvent)
            PlannerStep.PROCESSING -> ProcessingStepContent(onEvent)
            PlannerStep.CALIBRATE -> CalibrateStepContent(state, onEvent)
            PlannerStep.EDIT -> EditStepContent(state, onEvent)
            PlannerStep.GENERATING -> GeneratingStepContent(onEvent)
            PlannerStep.RESULT -> ResultStepContent(state, onEvent, enterRoom, onNavigateToShop)
            PlannerStep.ERROR -> ErrorStepContent(onEvent)
        }
    }
}

@Composable
private fun PlannerShell(
    title: String,
    state: RoomPlannerUiState,
    onEvent: (RoomPlannerEvent) -> Unit,
    onNavigateToShop: () -> Unit,
    onEnterRoom: () -> Unit,
    content: @Composable ((RoomPlannerEvent) -> Unit) -> Unit
) {
    val showActionBar = state.step in listOf(
        PlannerStep.UPLOAD,
        PlannerStep.SELECT_SOURCE,
        PlannerStep.CALIBRATE,
        PlannerStep.EDIT
    )
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f)
        ) {
            // 品牌标题衬底
            HfGlassPanel(
                fill = PlannerHfColors.HeaderFill,
                border = PlannerHfColors.GlassBorder,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.075f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ImmerseHomeLogo(modifier = Modifier.size(34.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Immerse Home  /  $title",
                        color = PlannerHfColors.TextPrimary,
                        style = HfType.Label
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "演示数据",
                        color = PlannerHfColors.AuxiliaryGray,
                        style = HfType.Note
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Glass 主窗口：内容区 → 操作行 → 底部导航行（面板最后一行，独立底栏）
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                HfGlassPanel(
                    fill = PlannerHfColors.GlassFill,
                    border = PlannerHfColors.GlassBorder,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            content(onEvent)
                            // toast（内容区左下，独立区不与导航/操作行交叠）
                            state.toast?.let { message ->
                                HfToast(
                                    message = message,
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(start = 24.dp, bottom = 12.dp)
                                )
                            }
                        }
                        // 底部操作条（导航行上方）
                        if (showActionBar) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = 24.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                HfSecondaryButton(
                                    text = "上传户型图",
                                    onClick = { onEvent(RoomPlannerEvent.OpenImportOptions) },
                                    leadingIconRes = R.drawable.ic_file_upload
                                )
                                val generateEnabled = isGenerateEnabled(state)
                                HfPrimaryButton(
                                    text = "生成模拟房间",
                                    onClick = { onEvent(RoomPlannerEvent.GenerateRoom) },
                                    enabled = generateEnabled,
                                    leadingIconRes = R.drawable.ic_house_add
                                )
                            }
                        }
                        // 底部导航 pills：面板最底一行，居中
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HfNavPill(
                                label = "我的户型",
                                iconRes = R.drawable.ic_nav_home,
                                selected = true,
                                onClick = {}
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            HfNavPill(
                                label = "选家具",
                                iconRes = R.drawable.ic_nav_couch,
                                selected = false,
                                onClick = onNavigateToShop
                            )
                        }
                    }
                }
                // 模态弹窗 P15–P19（覆盖整个面板含导航栏）
                state.dialog?.let {
                    PlannerDialogOverlay(
                        state = state,
                        onEvent = onEvent
                    )
                }
            }
        }
    }
}

/* ------------------------------- P01 上传 ------------------------------- */

@Composable
private fun UploadStepContent() {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 34.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        HfGlassPanel(
            fill = PlannerHfColors.CanvasPanel,
            border = PlannerHfColors.GlassBorder,
            modifier = Modifier
                .weight(0.42f)
                .fillMaxHeight(0.84f)
        ) {
            // 上传入口容器：内边距保证 ≥24dp 呼吸空间，卡片固定比例约束避免压边/溢出
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                UploadIllustration(
                    modifier = Modifier
                        .sizeIn(maxWidth = 240.dp, maxHeight = 240.dp)
                        .aspectRatio(1f)
                )
            }
        }
        Column(
            modifier = Modifier.weight(0.58f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "让家的轮廓，\n变成立体空间",
                color = PlannerHfColors.DeepBrown,
                style = HfType.Section
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "上传一张室内平面图，先校准真实尺寸，再直接在图上编辑墙、门、窗，最后生成 3D 模拟房间。",
                color = PlannerHfColors.DeepBrown,
                style = HfType.Note
            )
            Text(
                text = "上传完成后会自动识别墙体轮廓，你可以在图上继续调整，并进入立体空间试摆家具。",
                color = PlannerHfColors.DeepBrown,
                style = HfType.Note
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "上传平面图    →    图上编辑    →    生成房间",
                color = PlannerHfColors.TextPrimary,
                style = HfType.Note.copy(fontWeight = FontWeight.Medium)
            )
        }
    }
}

@Composable
private fun UploadIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        // 白色卡片（占满画布，比例由外层 aspectRatio(1f) 固定）
        drawRoundRect(
            color = Color.White.copy(alpha = 0.55f),
            cornerRadius = CornerRadius(26f, 26f)
        )
        drawRoundRect(
            color = PlannerHfColors.Wall.copy(alpha = 0.55f),
            cornerRadius = CornerRadius(26f, 26f),
            style = Stroke(width = 3f)
        )
        // 上传入口：圆形琥珀按钮 + 上传图标，整体在安全区内居中，
        // 与卡片边缘保持 ≥24dp 呼吸空间
        val safe = 24.dp.toPx()
        val innerMin = min(size.width, size.height) - safe * 2f
        val circleR = innerMin * 0.34f
        val cx = size.width / 2f
        val cy = size.height / 2f - circleR * 0.34f
        drawCircle(
            color = PlannerHfColors.PrimaryContainer,
            radius = circleR,
            center = Offset(cx, cy)
        )
        val r = circleR * 0.45f
        val stroke = r * 0.28f
        drawLine(
            color = PlannerHfColors.ButtonText,
            start = Offset(cx, cy + r),
            end = Offset(cx, cy - r),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = PlannerHfColors.ButtonText,
            start = Offset(cx - r * 0.62f, cy - r * 0.28f),
            end = Offset(cx, cy - r),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = PlannerHfColors.ButtonText,
            start = Offset(cx + r * 0.62f, cy - r * 0.28f),
            end = Offset(cx, cy - r),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawRoundRect(
            color = PlannerHfColors.Wall.copy(alpha = 0.45f),
            topLeft = Offset(cx - r * 1.9f, cy + circleR + r * 0.55f),
            size = Size(r * 3.8f, r * 0.5f),
            cornerRadius = CornerRadius(r * 0.25f, r * 0.25f)
        )
    }
}

/* ------------------------------ P02 选择来源 ----------------------------- */

@Composable
private fun SelectSourceStepContent(onEvent: (RoomPlannerEvent) -> Unit) {
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onEvent(RoomPlannerEvent.StartImport(FloorPlanInputSource.UPLOAD))
        }
    }
    var pan by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 34.dp, vertical = 24.dp)
    ) {
        FloorPlanCanvas(
            plan = FloorPlanRoom.simulatedSamplePlan,
            selection = null,
            pan = pan,
            showHandles = false,
            modifier = Modifier
                .fillMaxSize()
                .clipRounded(),
            onTap = { _, _ -> },
            onLongPress = { _, _ -> },
            onPan = { pan += it }
        )
        Text(
            text = "示例户型 · 模拟数据",
            color = PlannerHfColors.AuxiliaryGray,
            style = HfType.Note,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 10.dp, end = 14.dp)
        )
        HfToolPanel(
            title = "上传户型图",
            description = "选择清晰的 JPG / PNG；相机不可用时仍可选文件。",
            buttonRow = {
                HfSecondaryButton(
                    text = "选择文件",
                    onClick = { filePicker.launch(arrayOf("image/*")) },
                    leadingIconRes = R.drawable.ic_file_upload
                )
                HfSecondaryButton(
                    text = "拍照上传",
                    onClick = { onEvent(RoomPlannerEvent.ShowToast(TOAST_CAMERA_UNAVAILABLE)) }
                )
                HfPrimaryButton(
                    text = "使用示例",
                    onClick = { onEvent(RoomPlannerEvent.UseSamplePlan) }
                )
            },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 56.dp)
                .width(454.dp)
        )
    }
}

/* ------------------------------- P03 处理中 ------------------------------ */

@Composable
private fun ProcessingStepContent(onEvent: (RoomPlannerEvent) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "正在提取墙体与门窗…",
            color = PlannerHfColors.TextPrimary,
            style = HfType.Section
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "上传完成 → 提取轮廓 → 检查开口\n请稍候，完成后可以直接在图上修正。",
            color = PlannerHfColors.TextSecondary,
            textAlign = TextAlign.Center,
            style = HfType.Label
        )
        Spacer(modifier = Modifier.height(24.dp))
        HfSecondaryButton(
            text = "取消处理",
            onClick = { onEvent(RoomPlannerEvent.CancelBusyStep) },
            leadingIconRes = R.drawable.ic_arrow_left
        )
    }
}

/* -------------------------------- P04 校准 ------------------------------- */

@Composable
private fun CalibrateStepContent(
    state: RoomPlannerUiState,
    onEvent: (RoomPlannerEvent) -> Unit
) {
    var pan by remember { mutableStateOf(Offset.Zero) }
    var lengthText by remember(state.generatedPlan.id()) { mutableStateOf("6.00") }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 34.dp, vertical = 24.dp)
    ) {
        FloorPlanCanvas(
            plan = state.generatedPlan,
            selection = calibrationWallSelection(state.generatedPlan),
            pan = pan,
            showHandles = true,
            modifier = Modifier
                .fillMaxSize()
                .clipRounded(),
            onTap = { _, _ -> },
            onLongPress = { _, _ -> },
            onPan = { pan += it }
        )
        HfToolPanel(
            title = "校准真实尺寸",
            description = "输入一段你知道长度的墙（如最长外墙），用于换算比例。",
            inputRow = {
                HfNumberInput(
                    label = "已知长度（米）",
                    value = lengthText,
                    onValueChange = { lengthText = it }
                )
            },
            buttonRow = {
                HfSecondaryButton(
                    text = "取消",
                    onClick = { onEvent(RoomPlannerEvent.CancelBusyStep) },
                    leadingIconRes = R.drawable.ic_arrow_left
                )
                HfPrimaryButton(
                    text = "确认尺寸",
                    onClick = {
                        val length = lengthText.toFloatOrNull()
                        if (length != null) {
                            onEvent(RoomPlannerEvent.ConfirmCalibration(length))
                        } else {
                            onEvent(RoomPlannerEvent.ShowToast("请输入有效长度"))
                        }
                    },
                    leadingIconRes = R.drawable.ic_arrow_right
                )
            },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 56.dp)
                .width(454.dp)
        )
    }
}

/* ------------------------------ P05–P11 编辑 ------------------------------ */

@Composable
private fun EditStepContent(
    state: RoomPlannerUiState,
    onEvent: (RoomPlannerEvent) -> Unit
) {
    var pan by remember { mutableStateOf(Offset.Zero) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 34.dp, vertical = 24.dp)
    ) {
        // 户型图画布区：提示条移出画布，独立放在画布下方，避免与墙体线/尺寸标注叠加
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            FloorPlanCanvas(
                plan = state.generatedPlan,
                selection = state.selected,
                pan = pan,
                showHandles = state.selected != null,
                draftPoint = state.wallDraftStart,
                modifier = Modifier
                    .fillMaxSize()
                    .clipRounded(),
                onTap = { x, z ->
                    if (state.isAddingWall) {
                        onEvent(RoomPlannerEvent.PlaceWallPoint(x, z))
                    } else {
                        onEvent(RoomPlannerEvent.SelectElementAt(x, z))
                    }
                },
                onLongPress = { x, z ->
                    if (!state.isAddingWall) {
                        onEvent(RoomPlannerEvent.BeginAddWallAt(x, z))
                    }
                },
                onPan = { pan += it }
            )
            // 就近工具面板（P06–P11）
            val panelModifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 56.dp)
                .width(454.dp)
            when {
                state.pendingDelete != null ->
                    DeleteConfirmPanel(state, onEvent, panelModifier)
                state.showIssues ->
                    IssuesPanel(state, onEvent, panelModifier)
                state.isAddingWall ->
                    AddWallPanel(state, onEvent, panelModifier)
                state.selected?.type == PlannerSelectionType.WALL ->
                    WallEditorPanel(state, onEvent, panelModifier)
                state.selected?.type == PlannerSelectionType.DOOR ->
                    DoorEditorPanel(state, onEvent, panelModifier)
                state.selected?.type == PlannerSelectionType.WINDOW ->
                    WindowEditorPanel(state, onEvent, panelModifier)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        // 提示行 + 演示开关（画布下方独立区域，就近排布，保持小字号弱视觉）
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HfDemoLink(text = "演示：检查问题", onClick = { onEvent(RoomPlannerEvent.InjectIssuePlan) })
                HfDemoLink(
                    text = "演示：离线" + if (state.simulateOffline) "·开" else "",
                    onClick = { onEvent(RoomPlannerEvent.ToggleOfflineSimulated) }
                )
                HfDemoLink(
                    text = "演示：保存失败" + if (state.simulateSaveFailure) "·开" else "",
                    onClick = { onEvent(RoomPlannerEvent.ToggleSaveFailureSimulated) }
                )
                HfDemoLink(
                    text = "演示：生成异常" + if (state.simulateGenerationError) "·开" else "",
                    onClick = { onEvent(RoomPlannerEvent.ToggleGenerationErrorSimulated) }
                )
                HfDemoLink(
                    text = "演示：恢复草稿",
                    onClick = { onEvent(RoomPlannerEvent.ShowDialog(PlannerDialog.RESTORE_DRAFT)) }
                )
            }
            Text(
                text = "点击墙或门窗开始编辑 · 长按空白添加墙 · 拖空白移动视图",
                color = PlannerHfColors.TextSecondary,
                style = HfType.Note
            )
        }
    }
}

@Composable
private fun WallEditorPanel(
    state: RoomPlannerUiState,
    onEvent: (RoomPlannerEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val wall = state.generatedPlan.walls
        .firstOrNull { it.id == state.selected?.id } ?: return
    val horizontal = wall.width >= wall.depth
    val length = if (horizontal) wall.width else wall.depth
    val thickness = minOf(wall.width, wall.depth)
    var lengthText by remember(wall.id, wall.width, wall.depth, wall.height) {
        mutableStateOf("%.2f".format(length))
    }
    var heightText by remember(wall.id, wall.width, wall.depth, wall.height) {
        mutableStateOf("%.2f".format(wall.height))
    }
    var thicknessText by remember(wall.id, wall.width, wall.depth, wall.height) {
        mutableStateOf("%.2f".format(thickness))
    }
    HfToolPanel(
        title = "编辑墙体",
        subtitle = "墙体已选中 · 长 %.2f m".format(length),
        inputRow = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HfNumberInput(
                    label = "长度（米）",
                    value = lengthText,
                    onValueChange = { text ->
                        lengthText = text
                        text.toFloatOrNull()?.let {
                            onEvent(RoomPlannerEvent.UpdateWallGeometry(it, null, null))
                        }
                    }
                )
                HfNumberInput(
                    label = "墙高（米）",
                    value = heightText,
                    onValueChange = { text ->
                        heightText = text
                        text.toFloatOrNull()?.let {
                            onEvent(RoomPlannerEvent.UpdateWallGeometry(null, it, null))
                        }
                    }
                )
                HfNumberInput(
                    label = "厚度（米）",
                    value = thicknessText,
                    onValueChange = { text ->
                        thicknessText = text
                        text.toFloatOrNull()?.let {
                            onEvent(RoomPlannerEvent.UpdateWallGeometry(null, null, it))
                        }
                    }
                )
            }
        },
        buttonRow = {
            HfSecondaryButton(
                text = "添加门",
                onClick = { onEvent(RoomPlannerEvent.AddDoorOnSelection) }
            )
            HfSecondaryButton(
                text = "添加窗",
                onClick = { onEvent(RoomPlannerEvent.AddWindowOnSelection) }
            )
            HfDangerButton(
                text = "删除墙",
                onClick = { onEvent(RoomPlannerEvent.RequestDeleteSelection) },
                deleteIconRes = R.drawable.ic_delete
            )
        },
        modifier = modifier
    )
}

@Composable
private fun DoorEditorPanel(
    state: RoomPlannerUiState,
    onEvent: (RoomPlannerEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val opening = state.generatedPlan.openings
        .firstOrNull { it.id == state.selected?.id } ?: return
    val width = if (opening.width >= opening.depth) opening.width else opening.depth
    var widthText by remember(opening.id, opening.width, opening.depth) {
        mutableStateOf("%.2f".format(width))
    }
    var heightText by remember(opening.id, opening.heightMeters) {
        mutableStateOf("%.2f".format(opening.heightMeters))
    }
    HfToolPanel(
        title = "编辑门",
        subtitle = "门 · 宽 %.2f m / 高 %.2f m".format(width, opening.heightMeters),
        inputRow = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HfNumberInput(
                    label = "宽度（米）",
                    value = widthText,
                    onValueChange = { widthText = it }
                )
                HfNumberInput(
                    label = "高度（米）",
                    value = heightText,
                    onValueChange = { heightText = it }
                )
            }
        },
        buttonRow = {
            HfSecondaryButton(
                text = "切换开向",
                onClick = { onEvent(RoomPlannerEvent.ToggleDoorSwing) }
            )
            HfPrimaryButton(
                text = "确认修改",
                onClick = {
                    val w = widthText.toFloatOrNull() ?: width
                    val h = heightText.toFloatOrNull() ?: opening.heightMeters
                    onEvent(RoomPlannerEvent.ApplyOpeningEdit(w, h, opening.sillMeters))
                }
            )
            HfDangerButton(
                text = "删除门",
                onClick = { onEvent(RoomPlannerEvent.RequestDeleteSelection) },
                deleteIconRes = R.drawable.ic_delete
            )
        },
        modifier = modifier
    )
}

@Composable
private fun WindowEditorPanel(
    state: RoomPlannerUiState,
    onEvent: (RoomPlannerEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val opening = state.generatedPlan.openings
        .firstOrNull { it.id == state.selected?.id } ?: return
    val width = if (opening.width >= opening.depth) opening.width else opening.depth
    var widthText by remember(opening.id, opening.width, opening.depth) {
        mutableStateOf("%.2f".format(width))
    }
    var heightText by remember(opening.id, opening.heightMeters) {
        mutableStateOf("%.2f".format(opening.heightMeters))
    }
    var sillText by remember(opening.id, opening.sillMeters) {
        mutableStateOf("%.2f".format(opening.sillMeters))
    }
    HfToolPanel(
        title = "编辑窗",
        subtitle = "窗 · 宽 %.2f m / 高 %.2f m".format(width, opening.heightMeters),
        inputRow = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HfNumberInput(
                    label = "宽度（米）",
                    value = widthText,
                    onValueChange = { widthText = it }
                )
                HfNumberInput(
                    label = "高度（米）",
                    value = heightText,
                    onValueChange = { heightText = it }
                )
                HfNumberInput(
                    label = "窗台高（米）",
                    value = sillText,
                    onValueChange = { sillText = it }
                )
            }
        },
        buttonRow = {
            HfPrimaryButton(
                text = "确认修改",
                onClick = {
                    val w = widthText.toFloatOrNull() ?: width
                    val h = heightText.toFloatOrNull() ?: opening.heightMeters
                    val s = sillText.toFloatOrNull() ?: opening.sillMeters
                    onEvent(RoomPlannerEvent.ApplyOpeningEdit(w, h, s))
                }
            )
            HfDangerButton(
                text = "删除窗",
                onClick = { onEvent(RoomPlannerEvent.RequestDeleteSelection) },
                deleteIconRes = R.drawable.ic_delete
            )
        },
        modifier = modifier
    )
}

@Composable
private fun AddWallPanel(
    state: RoomPlannerUiState,
    onEvent: (RoomPlannerEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    HfToolPanel(
        title = "添加墙体",
        subtitle = if (state.wallDraftStart == null) {
            "请长按画布空白处放置起点。"
        } else {
            "已放置起点，请点击终点完成新墙。"
        },
        description = "点起点，再点终点；连接已有端点可闭合房间。",
        buttonRow = {
            HfPrimaryButton(
                text = "完成绘制",
                onClick = { onEvent(RoomPlannerEvent.FinishAddWall) }
            )
            HfSecondaryButton(
                text = "取消",
                onClick = { onEvent(RoomPlannerEvent.CancelAddWall) },
                leadingIconRes = R.drawable.ic_arrow_left
            )
        },
        modifier = modifier
    )
}

@Composable
private fun IssuesPanel(
    state: RoomPlannerUiState,
    onEvent: (RoomPlannerEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val issues = remember(state.generatedPlan) { buildPlanIssues(state.generatedPlan) }
    HfToolPanel(
        title = "有 ${issues.size} 处需要修正",
        subtitle = "问题位置已在画布上标记，修正一处即少一处。",
        description = planIssueSummary(issues),
        buttonRow = {
            HfSecondaryButton(
                text = "定位断点",
                onClick = { onEvent(RoomPlannerEvent.LocateIssue(PlannerIssueKind.OPEN_EDGE)) }
            )
            HfSecondaryButton(
                text = "定位重叠",
                onClick = { onEvent(RoomPlannerEvent.LocateIssue(PlannerIssueKind.OVERLAP)) }
            )
        },
        modifier = modifier
    )
}

@Composable
private fun DeleteConfirmPanel(
    state: RoomPlannerUiState,
    onEvent: (RoomPlannerEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val target = state.pendingDelete ?: return
    val plan = state.generatedPlan
    val title = when (target.type) {
        PlannerSelectionType.WALL -> "删除这面墙？"
        PlannerSelectionType.DOOR -> "删除这扇门？"
        PlannerSelectionType.WINDOW -> "删除这扇窗？"
    }
    val description = when (target.type) {
        PlannerSelectionType.WALL -> {
            val doors = doorsAttachedToWall(plan, target.id)
            if (doors > 0) {
                "这面墙上的 $doors 扇门也会移除。删除后可撤销。"
            } else {
                "墙体删除后可撤销，同时会移除墙上的门窗。"
            }
        }
        else -> "删除后可撤销。"
    }
    HfToolPanel(
        title = title,
        description = description,
        buttonRow = {
            HfSecondaryButton(
                text = "取消",
                onClick = { onEvent(RoomPlannerEvent.CancelDeleteSelection) },
                leadingIconRes = R.drawable.ic_arrow_left
            )
            HfDangerButton(
                text = "确认删除",
                onClick = { onEvent(RoomPlannerEvent.ConfirmDeleteSelection) },
                deleteIconRes = R.drawable.ic_delete
            )
        },
        modifier = modifier
    )
}

/* ------------------------------- P12 生成中 ------------------------------ */

@Composable
private fun GeneratingStepContent(onEvent: (RoomPlannerEvent) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "正在搭建你的房间",
            color = PlannerHfColors.TextPrimary,
            style = HfType.Section
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "检查结构  ✓    构建墙体与开口  …    准备空间\n生成期间保留当前编辑结果。",
            color = PlannerHfColors.TextSecondary,
            textAlign = TextAlign.Center,
            style = HfType.Label
        )
        Spacer(modifier = Modifier.height(24.dp))
        HfSecondaryButton(
            text = "取消生成",
            onClick = { onEvent(RoomPlannerEvent.CancelBusyStep) },
            leadingIconRes = R.drawable.ic_house_add
        )
    }
}

/* ------------------------------- P13 已就绪 ------------------------------ */

@Composable
private fun ResultStepContent(
    state: RoomPlannerUiState,
    onEvent: (RoomPlannerEvent) -> Unit,
    onEnterRoom: () -> Unit,
    onNavigateToShop: () -> Unit
) {
    var pan by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 34.dp, vertical = 24.dp)
    ) {
        FloorPlanCanvas(
            plan = state.generatedPlan,
            selection = null,
            pan = pan,
            showHandles = false,
            modifier = Modifier
                .fillMaxSize()
                .clipRounded(),
            onTap = { _, _ -> },
            onLongPress = { _, _ -> },
            onPan = { pan += it }
        )
        HfToolPanel(
            title = "房间已生成",
            description = "尺寸已校准 · 可以进入空间并开始选家具。",
            buttonRow = {
                HfSecondaryButton(
                    text = "返回编辑",
                    onClick = { onEvent(RoomPlannerEvent.ReturnToEdit) },
                    leadingIconRes = R.drawable.ic_arrow_left
                )
                HfPrimaryButton(
                    text = "进入房间",
                    onClick = onEnterRoom,
                    leadingIconRes = R.drawable.ic_house_add
                )
                HfSecondaryButton(
                    text = "去选家具",
                    onClick = onNavigateToShop,
                    leadingIconRes = R.drawable.ic_nav_couch,
                    textColor = PlannerHfColors.NavTextLight
                )
            },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 56.dp)
                .width(454.dp)
        )
    }
}

/* ------------------------------ P14 异常恢复 ----------------------------- */

@Composable
private fun ErrorStepContent(onEvent: (RoomPlannerEvent) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "暂时无法生成房间",
            color = PlannerHfColors.TextPrimary,
            style = HfType.Section
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "当前编辑已保留，请重试或返回检查户型。\n\n" +
                "上传失败：格式不支持 → 重新选择\n" +
                "提取失败：图片不清晰 → 重传 / 使用示例\n" +
                "空间不可用：返回平面预览 → 稍后重试",
            color = PlannerHfColors.TextSecondary,
            textAlign = TextAlign.Center,
            style = HfType.Label
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HfSecondaryButton(
                text = "返回编辑",
                onClick = { onEvent(RoomPlannerEvent.ReturnToEdit) },
                leadingIconRes = R.drawable.ic_arrow_left
            )
            HfPrimaryButton(
                text = "重新尝试",
                onClick = { onEvent(RoomPlannerEvent.RetryGeneration) },
                leadingIconRes = R.drawable.ic_arrow_right
            )
        }
    }
}

/* ------------------------------ P15–P19 弹窗 ------------------------------ */

@Composable
private fun PlannerDialogOverlay(
    state: RoomPlannerUiState,
    onEvent: (RoomPlannerEvent) -> Unit
) {
    val dialog = state.dialog ?: return
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        HfGlassPanel(
            fill = PlannerHfColors.ToolPanel,
            border = PlannerHfColors.DialogStroke,
            modifier = Modifier.width(504.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = dialogTitle(dialog),
                    color = PlannerHfColors.ToastText,
                    style = HfType.Section
                )
                Text(
                    text = dialogBody(dialog),
                    color = PlannerHfColors.ToastText,
                    style = HfType.Note
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HfSecondaryButton(
                        text = dialogDismissLabel(dialog),
                        onClick = {
                            when (dialog) {
                                PlannerDialog.OFFLINE -> onEvent(RoomPlannerEvent.UseSamplePlan)
                                else -> onEvent(RoomPlannerEvent.DismissDialog)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    HfPrimaryButton(
                        text = dialogConfirmLabel(dialog),
                        onClick = {
                            when (dialog) {
                                PlannerDialog.REPLACE_CURRENT_PLAN ->
                                    onEvent(RoomPlannerEvent.ConfirmReplaceCurrentPlan)
                                PlannerDialog.RESTORE_DRAFT ->
                                    onEvent(RoomPlannerEvent.RestoreDraft)
                                PlannerDialog.SAVE_FAILED ->
                                    onEvent(RoomPlannerEvent.RetrySave)
                                PlannerDialog.OFFLINE ->
                                    onEvent(RoomPlannerEvent.RetryConnection)
                                PlannerDialog.INVALID_DOOR_SIZE ->
                                    onEvent(RoomPlannerEvent.DismissDialog)
                            }
                        },
                        leadingIconRes = dialogConfirmIcon(dialog)
                    )
                }
            }
        }
    }
}

private fun dialogTitle(dialog: PlannerDialog): String = when (dialog) {
    PlannerDialog.REPLACE_CURRENT_PLAN -> "替换当前户型？"
    PlannerDialog.RESTORE_DRAFT -> "恢复上次编辑？"
    PlannerDialog.SAVE_FAILED -> "保存失败"
    PlannerDialog.OFFLINE -> "当前已离线"
    PlannerDialog.INVALID_DOOR_SIZE -> "门宽超出可用墙段"
}

private fun dialogBody(dialog: PlannerDialog): String = when (dialog) {
    PlannerDialog.REPLACE_CURRENT_PLAN -> "当前编辑将保留为本机草稿。上传新图会替换当前画布。"
    PlannerDialog.RESTORE_DRAFT -> "发现上次保存的户型草稿，可以恢复墙体及门窗修改。"
    PlannerDialog.SAVE_FAILED -> "修改仍保留在当前会话。请不要退出应用，重试保存。"
    PlannerDialog.OFFLINE -> "可以继续本地编辑。在线识别暂不可用，原图已保留。"
    PlannerDialog.INVALID_DOOR_SIZE -> "请输入 0.10–2.40 米，或选择更长的墙。当前输入未丢失。"
}

private fun dialogDismissLabel(dialog: PlannerDialog): String = when (dialog) {
    PlannerDialog.REPLACE_CURRENT_PLAN -> "保留当前"
    PlannerDialog.RESTORE_DRAFT -> "稍后恢复"
    PlannerDialog.SAVE_FAILED -> "继续编辑"
    PlannerDialog.OFFLINE -> "使用示例"
    PlannerDialog.INVALID_DOOR_SIZE -> "取消修改"
}

private fun dialogConfirmLabel(dialog: PlannerDialog): String = when (dialog) {
    PlannerDialog.REPLACE_CURRENT_PLAN -> "上传新图"
    PlannerDialog.RESTORE_DRAFT -> "恢复草稿"
    PlannerDialog.SAVE_FAILED -> "重试保存"
    PlannerDialog.OFFLINE -> "重试连接"
    PlannerDialog.INVALID_DOOR_SIZE -> "返回输入"
}

private fun dialogConfirmIcon(dialog: PlannerDialog): Int? = when (dialog) {
    PlannerDialog.REPLACE_CURRENT_PLAN -> R.drawable.ic_file_upload
    PlannerDialog.RESTORE_DRAFT -> R.drawable.ic_arrow_right
    PlannerDialog.SAVE_FAILED -> R.drawable.ic_arrow_down
    PlannerDialog.OFFLINE -> R.drawable.ic_arrow_down
    PlannerDialog.INVALID_DOOR_SIZE -> R.drawable.ic_arrow_left
}

/* --------------------------------- 工具 --------------------------------- */

private fun VirtualRoomPlan.id(): String = name

private fun Modifier.clipRounded(): Modifier =
    this.then(
        Modifier.background(
            color = PlannerHfColors.CanvasPanel,
            shape = RoundedCornerShape(12.dp)
        )
    )
