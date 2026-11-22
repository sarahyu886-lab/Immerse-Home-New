package com.pico.welcomespace.ui.shop.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.ButtonDefaults
import com.pico.spatial.ui.design.IconButton
import com.pico.spatial.ui.design.IconButtonDefaults
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.SearchField
import com.pico.spatial.ui.design.Text
import com.pico.welcomespace.domain.model.ShopCategory
import com.pico.welcomespace.ui.shop.ShopEvent
import com.pico.welcomespace.ui.shop.ShopUiState

@Composable
fun ShopHeader(state: ShopUiState, onEvent: (ShopEvent) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Row(
            modifier = Modifier.width(260.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ImmerseHomeLogo(modifier = Modifier.size(58.dp))
            Text(
                text = "Immerse Home",
                style = PicoTheme.typography.headlineMedium,
                color = PicoTheme.colorScheme.labelPrimary
            )
        }
        SearchField(
            value = state.query,
            onValueChange = { onEvent(ShopEvent.QueryChanged(it)) },
            onSearch = { onEvent(ShopEvent.QueryChanged(it)) },
            modifier = Modifier.width(410.dp),
            placeholder = { Text("搜索喜欢的家居好物") }
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "探索灵感 · 发现生活",
            style = PicoTheme.typography.bodySmall,
            color = PicoTheme.colorScheme.labelSecondary
        )
        Box {
            val iconColor = PicoTheme.colorScheme.labelPrimary
            IconButton(
                onClick = { onEvent(ShopEvent.OpenCart) },
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent,
                    contentColor = iconColor),
                modifier = Modifier.size(56.dp).semantics {
                    contentDescription = "打开购物车，${state.cartCount} 件商品"
                },
                shape = CircleShape
            ) {
                Canvas(modifier = Modifier.size(26.dp)) {
                    val stroke = 2.dp.toPx()
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(size.width * .13f, size.height * .2f)
                        lineTo(size.width * .27f, size.height * .2f)
                        lineTo(size.width * .37f, size.height * .7f)
                        lineTo(size.width * .79f, size.height * .7f)
                        lineTo(size.width * .88f, size.height * .34f)
                        lineTo(size.width * .3f, size.height * .34f)
                    }
                    drawPath(path, iconColor, style = Stroke(width = stroke, cap = StrokeCap.Round))
                    drawCircle(iconColor, radius = stroke, center = Offset(size.width * .4f, size.height * .85f))
                    drawCircle(iconColor, radius = stroke, center = Offset(size.width * .73f, size.height * .85f))
                }
            }
            if (state.cartCount > 0) {
                Text(
                    text = state.cartCount.toString(),
                    modifier = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp)
                        .clip(CircleShape)
                        .background(PicoTheme.colorScheme.interaction)
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                    style = PicoTheme.typography.labelSmall,
                    color = PicoTheme.colorScheme.labelPrimaryLight
                )
            }
        }
        if (state.showingCart || state.showingPayment) {
            val iconColor = PicoTheme.colorScheme.labelPrimary
            IconButton(
                onClick = { onEvent(ShopEvent.GoHome) },
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent,
                    contentColor = iconColor),
                modifier = Modifier.size(56.dp).semantics {
                    contentDescription = "返回首页"
                },
                shape = CircleShape
            ) {
                Canvas(modifier = Modifier.size(26.dp)) {
                    val stroke = 2.dp.toPx()
                    val roof = androidx.compose.ui.graphics.Path().apply {
                        moveTo(size.width * .13f, size.height * .46f)
                        lineTo(size.width * .5f, size.height * .15f)
                        lineTo(size.width * .87f, size.height * .46f)
                    }
                    val house = androidx.compose.ui.graphics.Path().apply {
                        moveTo(size.width * .25f, size.height * .43f)
                        lineTo(size.width * .25f, size.height * .85f)
                        lineTo(size.width * .75f, size.height * .85f)
                        lineTo(size.width * .75f, size.height * .43f)
                        moveTo(size.width * .43f, size.height * .85f)
                        lineTo(size.width * .43f, size.height * .62f)
                        lineTo(size.width * .57f, size.height * .62f)
                        lineTo(size.width * .57f, size.height * .85f)
                    }
                    drawPath(roof, iconColor, style = Stroke(width = stroke, cap = StrokeCap.Round))
                    drawPath(house, iconColor, style = Stroke(width = stroke, cap = StrokeCap.Round))
                }
            }
        } else {
            Spacer(modifier = Modifier.size(56.dp))
        }
    }
}

@Composable
private fun ImmerseHomeLogo(modifier: Modifier = Modifier) {
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

@Composable
fun ShopSidebar(
    state: ShopUiState,
    onEvent: (ShopEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(24.dp))
            .background(PicoTheme.colorScheme.fillSecondary)
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "探索分类",
            style = PicoTheme.typography.titleMedium,
            color = PicoTheme.colorScheme.labelPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        ShopCategory.entries.forEach { category ->
            val selected = category == state.selectedCategory
            Button(
                onClick = { onEvent(ShopEvent.CategorySelected(category)) },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selected) {
                        Color(0xE6FFBF4A) // design-style: fixed-figma-color 用户指定黄色选中态。
                    } else {
                        Color.Transparent
                    },
                    contentColor = if (selected) {
                        PicoTheme.colorScheme.labelPrimary
                    } else {
                        PicoTheme.colorScheme.labelPrimary
                    }
                )
            ) {
                Text(category.label)
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "精选设计\n让好物进入生活",
            style = PicoTheme.typography.bodyMedium,
            color = PicoTheme.colorScheme.labelSecondary
        )
    }
}
