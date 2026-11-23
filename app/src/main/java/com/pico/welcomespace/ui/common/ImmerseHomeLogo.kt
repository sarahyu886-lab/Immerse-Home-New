package com.pico.welcomespace.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

/** Immerse Home 品牌 LOGO（自绘矢量，供电商页与户型流程窗口标题衬底复用）。 */
@Composable
fun ImmerseHomeLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val dark = Color(0xFF302D2A) // design-style: fixed-figma-color reference logo charcoal
        val ivory = Color(0xFFFFFBF5) // design-style: fixed-figma-color reference logo ivory
        val radius = CornerRadius(w * .20f, h * .20f)
        drawRoundRect(color = ivory, cornerRadius = radius)
        drawRoundRect(
            color = dark,
            topLeft = Offset(w * .025f, h * .025f),
            size = Size(w * .95f, h * .95f),
            cornerRadius = CornerRadius(w * .18f, h * .18f)
        )
        val house = Path().apply {
            moveTo(w * .23f, h * .46f)
            quadraticTo(w * .23f, h * .43f, w * .26f, h * .40f)
            lineTo(w * .46f, h * .23f)
            quadraticTo(w * .50f, h * .19f, w * .54f, h * .23f)
            lineTo(w * .77f, h * .41f)
            quadraticTo(w * .80f, h * .44f, w * .80f, h * .48f)
            lineTo(w * .80f, h * .74f)
            quadraticTo(w * .80f, h * .79f, w * .75f, h * .79f)
            lineTo(w * .28f, h * .79f)
            quadraticTo(w * .23f, h * .79f, w * .23f, h * .74f)
            close()
        }
        drawPath(house, ivory)
        drawRoundRect(
            color = Color(0xFFFFB735), // design-style: fixed-figma-color reference logo amber window
            topLeft = Offset(w * .67f, h * .46f),
            size = Size(w * .064f, h * .11f),
            cornerRadius = CornerRadius(w * .032f, h * .032f)
        )
        // The dark silhouette cuts a sofa shape out of the ivory house.
        val sofa = Path().apply {
            moveTo(w * .32f, h * .80f)
            lineTo(w * .32f, h * .55f)
            quadraticTo(w * .32f, h * .50f, w * .37f, h * .50f)
            lineTo(w * .43f, h * .50f)
            quadraticTo(w * .48f, h * .50f, w * .48f, h * .55f)
            lineTo(w * .48f, h * .59f)
            lineTo(w * .69f, h * .59f)
            quadraticTo(w * .75f, h * .59f, w * .75f, h * .65f)
            lineTo(w * .75f, h * .80f)
            close()
        }
        drawPath(sofa, dark)
        drawRoundRect(
            color = ivory,
            topLeft = Offset(w * .37f, h * .70f),
            size = Size(w * .33f, h * .09f),
            cornerRadius = CornerRadius(w * .025f, h * .025f)
        )
    }
}
