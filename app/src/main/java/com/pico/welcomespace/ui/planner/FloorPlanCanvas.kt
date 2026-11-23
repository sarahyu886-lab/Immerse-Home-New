package com.pico.welcomespace.ui.planner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.pico.welcomespace.domain.model.RoomOpening
import com.pico.welcomespace.domain.model.RoomOpeningKind
import com.pico.welcomespace.domain.model.RoomSurfaceKind
import com.pico.welcomespace.domain.model.RoomWall
import com.pico.welcomespace.domain.model.VirtualRoomPlan
import kotlin.math.max

/**
 * 户型画布（HF 视觉）：
 * 墙 #6E6E6E 厚度基准 ≈6dp；选中对象高亮 #785510 + 两端白色手柄；
 * 窗块 #D9D9D9 / #292929 圆角；门块 #F7F7F7 + 开向弧线；
 * 房间名与尺寸标注通过原生 canvas 绘制。
 */
@Composable
fun FloorPlanCanvas(
    plan: VirtualRoomPlan,
    selection: PlannerSelection?,
    pan: Offset,
    modifier: Modifier = Modifier,
    showHandles: Boolean = true,
    draftPoint: PlanPoint? = null,
    dimensionLabel: Boolean = true,
    onTap: (Float, Float) -> Unit = { _, _ -> },
    onLongPress: (Float, Float) -> Unit = { _, _ -> },
    onPan: (Offset) -> Unit = {}
) {
    val density = LocalDensity.current
    val colors = PlannerHfColors
    val textPaint = remember {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }
    val gestureState = remember { FloorPlanGestureState() }
    Canvas(
        modifier = modifier
            .floorPlanTapGestures(
                state = gestureState,
                onTap = onTap,
                onLongPress = onLongPress
            )
            .floorPlanPanGestures(onPan = onPan)
    ) {
        val bounds = plan.widthMeters + 1.1f
        val boundsZ = plan.depthMeters + 1.1f
        val scale = minOf(size.width / bounds, size.height / boundsZ)
        val origin = Offset(size.width / 2f + pan.x, size.height / 2f + pan.y)

        fun px(value: Float) = value * scale
        fun pt(point: PlanPoint) = Offset(origin.x + px(point.x), origin.y - px(point.z))
        fun rectOf(centerX: Float, centerZ: Float, width: Float, depth: Float): Rect =
            Rect(
                left = origin.x + px(centerX - width / 2f),
                top = origin.y - px(centerZ + depth / 2f),
                right = origin.x + px(centerX + width / 2f),
                bottom = origin.y - px(centerZ - depth / 2f)
            )
        gestureState.attach(scale, origin)

        // 房间地面与房间名
        val roomLabelSize = with(density) { 13.dp.toPx() }
        plan.surfaces
            .filter { it.kind == RoomSurfaceKind.FLOOR }
            .forEach { surface ->
                val rect = rectOf(surface.centerX, surface.centerZ, surface.width, surface.depth)
                drawRect(
                    color = Color.White.copy(alpha = 0.32f),
                    topLeft = rect.topLeft,
                    size = rect.size
                )
                textPaint.color = colors.TextPrimary.toArgb()
                textPaint.textSize = roomLabelSize
                textPaint.typeface = android.graphics.Typeface.create(
                    android.graphics.Typeface.DEFAULT,
                    android.graphics.Typeface.BOLD
                )
                drawContext.canvas.nativeCanvas.drawText(
                    surface.label,
                    origin.x + px(surface.centerX),
                    origin.y - px(surface.centerZ) + roomLabelSize / 3f,
                    textPaint
                )
            }

        // 外轮廓（虚线参考框）
        val outline = rectOf(0f, 0f, plan.widthMeters, plan.depthMeters)
        drawRoundRect(
            color = colors.Wall.copy(alpha = 0.45f),
            topLeft = outline.topLeft,
            size = outline.size,
            cornerRadius = CornerRadius(6f, 6f),
            style = Stroke(
                width = 1.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
            )
        )

        // 尺寸标注：与墙体外缘保持 ≥8dp 间隙，避免压线（标注按文字实测宽度内收）
        if (dimensionLabel) {
            val dimSize = with(density) { 12.dp.toPx() }
            val labelGap = with(density) { 8.dp.toPx() }
            val halfWallPx = px((plan.walls.maxOfOrNull { minOf(it.width, it.depth) } ?: 0f) / 2f)
            textPaint.typeface = android.graphics.Typeface.create(
                android.graphics.Typeface.DEFAULT,
                android.graphics.Typeface.NORMAL
            )
            textPaint.textSize = dimSize
            textPaint.color = colors.TextSecondary.toArgb()
            val widthLabel = "%.2f m".format(plan.widthMeters)
            drawContext.canvas.nativeCanvas.drawText(
                widthLabel,
                origin.x,
                outline.top - halfWallPx - labelGap,
                textPaint
            )
            val depthLabel = "%.2f m".format(plan.depthMeters)
            val depthLabelWidth = textPaint.measureText(depthLabel)
            drawContext.canvas.nativeCanvas.drawText(
                depthLabel,
                outline.right + halfWallPx + labelGap + depthLabelWidth / 2f,
                origin.y,
                textPaint
            )
        }

        // 墙体：#6E6E6E，厚度基准 ≈6dp；选中 #785510
        val minWallPx = with(density) { 6.dp.toPx() }
        plan.walls.forEach { wall ->
            val selected = selection?.id == wall.id && selection.type == PlannerSelectionType.WALL
            val rect = rectOf(wall.centerX, wall.centerZ, wall.width, wall.depth)
            drawRoundRect(
                color = if (selected) colors.SelectionHighlight else colors.Wall,
                topLeft = rect.topLeft,
                size = Size(
                    max(rect.width, if (wall.width >= wall.depth) minWallPx else rect.width),
                    max(rect.height, if (wall.width >= wall.depth) rect.height else minWallPx)
                ),
                cornerRadius = CornerRadius(3f, 3f)
            )
        }

        // 门窗块
        plan.openings.forEach { opening ->
            val selected = selection?.id == opening.id
            val rect = rectOf(opening.centerX, opening.centerZ, opening.width, opening.depth)
            when (opening.kind) {
                RoomOpeningKind.WINDOW -> {
                    drawRoundRect(
                        color = if (selected) colors.SelectionHighlight else colors.WindowPaneFill,
                        topLeft = rect.topLeft,
                        size = rect.size,
                        cornerRadius = CornerRadius(rect.minDimension / 2f, rect.minDimension / 2f)
                    )
                    drawRoundRect(
                        color = colors.WindowPaneStroke,
                        topLeft = rect.topLeft,
                        size = rect.size,
                        cornerRadius = CornerRadius(rect.minDimension / 2f, rect.minDimension / 2f),
                        style = Stroke(width = 1.8f)
                    )
                }
                RoomOpeningKind.DOOR -> {
                    drawRoundRect(
                        color = if (selected) colors.SelectionHighlight else colors.DoorLeafFill,
                        topLeft = rect.topLeft,
                        size = rect.size,
                        cornerRadius = CornerRadius(3f, 3f)
                    )
                    drawRoundRect(
                        color = colors.Wall,
                        topLeft = rect.topLeft,
                        size = rect.size,
                        cornerRadius = CornerRadius(3f, 3f),
                        style = Stroke(width = 1.6f)
                    )
                    // 开向弧线：铰链取门块底边端点，swingFlipped 翻转朝向。
                    val radius = px(if (opening.width >= opening.depth) opening.width else opening.depth)
                    val hingeY = origin.y - px(opening.centerZ - opening.depth / 2f)
                    val hingeX = origin.x + px(
                        if (opening.swingFlipped) {
                            opening.centerX - opening.width / 2f
                        } else {
                            opening.centerX + opening.width / 2f
                        }
                    )
                    drawArc(
                        color = if (selected) colors.SelectionHighlight else colors.Wall,
                        startAngle = if (opening.swingFlipped) 180f else 270f,
                        sweepAngle = 90f,
                        useCenter = false,
                        topLeft = Offset(
                            if (opening.swingFlipped) hingeX else hingeX - radius,
                            hingeY - radius
                        ),
                        size = Size(radius, radius),
                        style = Stroke(width = 1.4f, cap = StrokeCap.Round)
                    )
                }
            }
        }

        // 添加墙草稿起点
        draftPoint?.let { point ->
            val center = pt(point)
            drawCircle(
                color = colors.AmberAlt,
                radius = with(density) { 7.dp.toPx() },
                center = center
            )
            drawCircle(
                color = colors.ButtonText,
                radius = with(density) { 7.dp.toPx() },
                center = center,
                style = Stroke(width = 2f)
            )
        }

        // 选中对象两端白色手柄
        if (showHandles && selection != null) {
            val endpoints: List<PlanPoint> = when (selection.type) {
                PlannerSelectionType.WALL -> plan.walls
                    .firstOrNull { it.id == selection.id }
                    ?.let { wallEndpointsOf(it) } ?: emptyList()
                PlannerSelectionType.DOOR,
                PlannerSelectionType.WINDOW -> plan.openings
                    .firstOrNull { it.id == selection.id }
                    ?.let { openingEndpointsOf(it) } ?: emptyList()
            }
            endpoints.forEach { endpoint ->
                drawCircle(
                    color = colors.SurfaceCream,
                    radius = with(density) { 8.dp.toPx() },
                    center = pt(endpoint)
                )
                drawCircle(
                    color = colors.SelectionHighlight,
                    radius = with(density) { 8.dp.toPx() },
                    center = pt(endpoint),
                    style = Stroke(width = 2.4f)
                )
            }
        }
    }
}

private fun wallEndpointsOf(wall: RoomWall): List<PlanPoint> =
    if (wall.width >= wall.depth) {
        listOf(
            PlanPoint(wall.centerX - wall.width / 2f, wall.centerZ),
            PlanPoint(wall.centerX + wall.width / 2f, wall.centerZ)
        )
    } else {
        listOf(
            PlanPoint(wall.centerX, wall.centerZ - wall.depth / 2f),
            PlanPoint(wall.centerX, wall.centerZ + wall.depth / 2f)
        )
    }

private fun openingEndpointsOf(opening: RoomOpening): List<PlanPoint> =
    if (opening.width >= opening.depth) {
        listOf(
            PlanPoint(opening.centerX - opening.width / 2f, opening.centerZ),
            PlanPoint(opening.centerX + opening.width / 2f, opening.centerZ)
        )
    } else {
        listOf(
            PlanPoint(opening.centerX, opening.centerZ - opening.depth / 2f),
            PlanPoint(opening.centerX, opening.centerZ + opening.depth / 2f)
        )
    }

private fun Modifier.floorPlanTapGestures(
    state: FloorPlanGestureState,
    onTap: (Float, Float) -> Unit,
    onLongPress: (Float, Float) -> Unit
): Modifier = this.pointerInput(Unit) {
    detectTapGestures(
        onTap = { offset ->
            val point = state.toPlan(offset)
            onTap(point.x, point.z)
        },
        onLongPress = { offset ->
            val point = state.toPlan(offset)
            onLongPress(point.x, point.z)
        }
    )
}

private fun Modifier.floorPlanPanGestures(
    onPan: (Offset) -> Unit
): Modifier = this.pointerInput(Unit) {
    detectDragGestures(
        onDrag = { change, dragAmount ->
            change.consume()
            onPan(dragAmount)
        }
    )
}

/** 存储最近一次绘制时的比例与原点，供手势反算户型坐标。 */
internal class FloorPlanGestureState {
    var scale: Float = 100f
        private set
    var origin: Offset = Offset.Zero
        private set

    fun attach(scale: Float, origin: Offset) {
        this.scale = scale
        this.origin = origin
    }

    fun toPlan(offset: Offset): PlanPoint =
        PlanPoint(
            x = (offset.x - origin.x) / scale,
            z = -(offset.y - origin.y) / scale
        )
}
