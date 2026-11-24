package com.pico.welcomespace.ui.planner.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.ButtonDefaults
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text
import com.pico.welcomespace.domain.model.VirtualRoomPlan
import com.pico.welcomespace.ui.planner.PlannerHfColors
import kotlin.math.min

@Composable
fun StandardRoomOverview(
    plan: VirtualRoomPlan,
    onEnterRoom: () -> Unit,
    modifier: Modifier = Modifier
) {
    val wallColor = PicoTheme.colorScheme.labelSecondary
    val floorColor = PicoTheme.colorScheme.fillSecondary
    val lineColor = PicoTheme.colorScheme.labelTertiary
    Row(
        modifier = modifier.fillMaxSize().padding(28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val scale = min(size.width / (plan.widthMeters + .8f),
                    size.height / (plan.depthMeters + .8f))
                val origin = Offset(size.width / 2, size.height / 2)
                val floorSize = Size(plan.widthMeters * scale, plan.depthMeters * scale)
                val topLeft = origin - Offset(floorSize.width / 2, floorSize.height / 2)
                drawRect(floorColor, topLeft, floorSize)
                // 每格 1 米，X/Z 共用同一个比例尺，保持 4:5 净空。
                for (x in 1 until plan.widthMeters.toInt()) {
                    val start = topLeft + Offset(x * scale, 0f)
                    drawLine(lineColor.copy(alpha = .3f), start, start + Offset(0f, floorSize.height))
                }
                for (z in 1 until plan.depthMeters.toInt()) {
                    val start = topLeft + Offset(0f, z * scale)
                    drawLine(lineColor.copy(alpha = .3f), start, start + Offset(floorSize.width, 0f))
                }
                plan.walls.forEach { wall ->
                    drawRect(wallColor,
                        origin + Offset((wall.centerX - wall.width / 2) * scale,
                            (wall.centerZ - wall.depth / 2) * scale),
                        Size(wall.width * scale, wall.depth * scale))
                }
                drawRect(lineColor, topLeft, floorSize, style = Stroke(1.dp.toPx()))
            }
            Text(
                "4.00 m",
                modifier = Modifier.align(Alignment.TopCenter),
                color = PicoTheme.colorScheme.labelPrimary,
                style = HfType.Note.copy(fontWeight = FontWeight.Medium)
            )
            Text(
                "5.00 m",
                modifier = Modifier.align(Alignment.CenterEnd),
                color = PicoTheme.colorScheme.labelPrimary,
                style = HfType.Note.copy(fontWeight = FontWeight.Medium)
            )
        }
        Column(Modifier.weight(.65f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "标准房间 · 平面鸟瞰",
                style = HfType.Section,
                color = PicoTheme.colorScheme.labelPrimary
            )
            Text(
                "室内净宽 4.00 m\n室内净深 5.00 m\n室内净高 2.50 m",
                style = HfType.Label,
                color = PicoTheme.colorScheme.labelPrimary
            )
            Text(
                "四面墙 + 地板 + 顶面\n鸟瞰隐藏顶面，每格 1 m\n墙地面为示范材质，非图片识别结果",
                style = HfType.Note,
                color = PicoTheme.colorScheme.labelSecondary
            )
            Button(
                onClick = onEnterRoom,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PlannerHfColors.PrimaryContainer,
                    contentColor = PlannerHfColors.TextPrimary
                ),
                modifier = Modifier.height(56.dp),
                shape = CircleShape
            ) { Text("进入房间", style = HfType.Label) }
        }
    }
}
