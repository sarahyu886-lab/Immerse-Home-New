package com.pico.welcomespace.ui.planner.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.ButtonDefaults
import com.pico.spatial.ui.design.Text
import com.pico.welcomespace.ui.planner.PlannerHfColors

/**
 * IH/LF 字阶（Figma 2302:39362）：Noto Sans SC 24/18/16，行高 1.5；品牌字 Bold 40，字距 -0.02em。
 * 全应用字号层级收敛为这 4 级，不新增档位。
 */
object HfType {
    /** 品牌/展示级：Darker Grotesque Bold 40，字距 -0.02em */
    val Brand = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.02).em)

    /** Section 标题：Medium 24 / 行高 36 */
    val Section = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Medium, lineHeight = 36.sp)

    /** Label 正文 / 控件文字：Medium 18 / 行高 27 */
    val Label = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium, lineHeight = 27.sp)

    /** Note 标注 / 辅助说明：Regular 16 / 行高 24 */
    val Note = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal, lineHeight = 24.sp)
}

/**
 * HF 设计系统图标：颜色继承 tint，不写死到资源。
 */
@Composable
fun HfIcon(resId: Int, modifier: Modifier = Modifier, tint: Color, iconSize: Dp = 18.dp) {
    Image(
        painter = painterResource(id = resId),
        contentDescription = null,
        modifier = modifier.size(iconSize),
        colorFilter = ColorFilter.tint(tint)
    )
}

/** 禁用态整体 opacity 0.45（规格 1.4）。 */
private fun Modifier.hfDisabledAlpha(enabled: Boolean): Modifier =
    if (enabled) this else this.alpha(0.45f)

/** HF Primary 按钮：琥珀 90% 底 + 深棕文字/图标。 */
@Composable
fun HfPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIconRes: Int? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = PlannerHfColors.PrimaryContainer
        ),
        shape = CircleShape,
        modifier = modifier
            .hfDisabledAlpha(enabled)
            .height(56.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            leadingIconRes?.let {
                HfIcon(it, tint = PlannerHfColors.ButtonText)
            }
            Text(
                text = text,
                color = PlannerHfColors.ButtonText,
                style = HfType.Label
            )
        }
    }
}

/** HF Secondary 按钮：白 20% 玻璃底 + 1px 白描边。 */
@Composable
fun HfSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIconRes: Int? = null,
    textColor: Color = PlannerHfColors.ButtonText
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = PlannerHfColors.SecondaryContainer
        ),
        shape = CircleShape,
        modifier = modifier
            .hfDisabledAlpha(enabled)
            .height(56.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            leadingIconRes?.let {
                HfIcon(it, tint = textColor)
            }
            Text(
                text = text,
                color = textColor,
                style = HfType.Label
            )
        }
    }
}

/** HF Danger 按钮：Secondary 样式 + 删除图标。 */
@Composable
fun HfDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    deleteIconRes: Int
) {
    HfSecondaryButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        leadingIconRes = deleteIconRes
    )
}

/** 底部导航 pill（我的户型 / 选家具）。 */
@Composable
fun HfNavPill(
    label: String,
    iconRes: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selected) {
        HfPrimaryButton(
            text = label,
            onClick = onClick,
            modifier = modifier,
            leadingIconRes = iconRes
        )
    } else {
        HfSecondaryButton(
            text = label,
            onClick = onClick,
            modifier = modifier,
            leadingIconRes = iconRes,
            textColor = PlannerHfColors.NavTextLight
        )
    }
}

/** 玻璃面板（品牌衬底 / 主窗口 / 工具面板基底）。 */
@Composable
fun HfGlassPanel(
    modifier: Modifier = Modifier,
    fill: Color,
    border: Color,
    cornerRadius: Dp = 24.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(fill)
            .border(1.dp, border, RoundedCornerShape(cornerRadius))
    ) {
        content()
    }
}

/**
 * 就近工具面板（P04/P06–P11）：标题 + 可选说明 + 输入行 + 按钮行。
 */
@Composable
fun HfToolPanel(
    title: String,
    buttonRow: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    description: String? = null,
    inputRow: (@Composable () -> Unit)? = null,
    showToolTag: Boolean = true
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(PlannerHfColors.ToolPanel)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (showToolTag) {
            Text(
                text = "就近工具",
                color = PlannerHfColors.TextSecondary,
                style = HfType.Note
            )
        }
        Text(
            text = title,
            color = PlannerHfColors.TextPrimary,
            style = HfType.Label
        )
        subtitle?.let {
            Text(
                text = it,
                color = PlannerHfColors.TextSecondary,
                style = HfType.Note
            )
        }
        description?.let {
            Text(
                text = it,
                color = PlannerHfColors.TextSecondary,
                style = HfType.Note
            )
        }
        inputRow?.invoke()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            content = buttonRow
        )
    }
}

/**
 * HF 数字输入框（125.82 × 39.14 → 126×39dp，圆角 9dp）。
 * 仅允许数字与一个小数点。
 */
@Composable
fun HfNumberInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            color = PlannerHfColors.TextPrimary,
            style = HfType.Note
        )
        Box(
            modifier = Modifier
                .width(126.dp)
                .height(39.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(PlannerHfColors.SurfaceCream)
                .border(1.dp, PlannerHfColors.InputBorder, RoundedCornerShape(9.dp))
                .padding(horizontal = 11.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = { raw ->
                    val filtered = raw.filter { it.isDigit() || it == '.' }.let {
                        val firstDot = it.indexOf('.')
                        if (firstDot >= 0) {
                            it.substring(0, firstDot + 1) + it.substring(firstDot + 1).filter { c -> c != '.' }
                        } else {
                            it
                        }
                    }
                    onValueChange(filtered)
                },
                enabled = enabled,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    color = PlannerHfColors.TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun HfTextInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = false,
    minLines: Int = 1,
    enabled: Boolean = true
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            color = PlannerHfColors.TextPrimary,
            style = HfType.Note
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(PlannerHfColors.SurfaceCream)
                .border(1.dp, PlannerHfColors.InputBorder, RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            if (value.isEmpty() && placeholder.isNotEmpty()) {
                Text(
                    text = placeholder,
                    color = PlannerHfColors.AuxiliaryGray,
                    style = HfType.Note
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = singleLine,
                minLines = minLines,
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = PlannerHfColors.TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun HfPlusButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = PlannerHfColors.PrimaryContainer),
        shape = CircleShape,
        modifier = modifier
            .hfDisabledAlpha(enabled)
            .size(56.dp)
    ) {
        Text(
            text = "+",
            color = PlannerHfColors.ButtonText,
            style = HfType.Section
        )
    }
}

@Composable
fun HfProgressRing(
    progress: Int,
    modifier: Modifier = Modifier,
    label: String? = null
) {
    val safeProgress = progress.coerceIn(0, 100)
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(96.dp)) {
                val stroke = 10.dp.toPx()
                drawCircle(
                    color = PlannerHfColors.InputBorder,
                    style = Stroke(width = stroke)
                )
                drawArc(
                    color = PlannerHfColors.PrimaryContainer,
                    startAngle = -90f,
                    sweepAngle = 360f * safeProgress / 100f,
                    useCenter = false,
                    topLeft = Offset(stroke / 2f, stroke / 2f),
                    size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
            Text(
                text = "$safeProgress%",
                color = PlannerHfColors.TextPrimary,
                style = HfType.Label
            )
        }
        label?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                color = PlannerHfColors.TextSecondary,
                style = HfType.Note
            )
        }
    }
}

/** 演示开关小链接（仅演示用途，置于提示行末端）。 */
@Composable
fun HfDemoLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = PlannerHfColors.TextSecondary,
        style = HfType.Note,
        modifier = modifier.clickable(onClick = onClick)
    )
}

/** toast 文案样式。 */
@Composable
fun HfToast(message: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        color = PlannerHfColors.ToastText,
        style = HfType.Note,
        modifier = modifier
    )
}

/** 弹窗主按钮文字行内间距工具（无逻辑，保持 API 一致）。 */
@Composable
fun HfSpacer(width: Dp) {
    Spacer(modifier = Modifier.width(width))
}
