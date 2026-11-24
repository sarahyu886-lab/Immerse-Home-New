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
import com.pico.welcomespace.ui.common.ImmerseHomeLogo
import com.pico.welcomespace.ui.planner.PlannerHfColors
import com.pico.welcomespace.ui.planner.components.HfType
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
            modifier = Modifier.width(352.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ImmerseHomeLogo(modifier = Modifier.size(58.dp))
            Text(
                text = "Immerse Home",
                style = HfType.Brand,
                color = PlannerHfColors.TextPrimary,
                maxLines = 1,
                softWrap = false
            )
        }
        SearchField(
            value = state.query,
            onValueChange = { onEvent(ShopEvent.QueryChanged(it)) },
            onSearch = { onEvent(ShopEvent.QueryChanged(it)) },
            modifier = Modifier.width(410.dp),
            placeholder = { Text("搜索喜欢的家居好物", style = HfType.Note) }
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "探索灵感 · 发现生活",
            style = HfType.Note,
            color = PlannerHfColors.TextSecondary
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
                    style = HfType.Note,
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
            style = HfType.Label,
            color = PicoTheme.colorScheme.labelPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        ShopCategory.entries.forEach { category ->
            val selected = category == state.selectedCategory
            Button(
                onClick = { onEvent(ShopEvent.CategorySelected(category)) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selected) {
                        PlannerHfColors.PrimaryContainer
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
                Text(category.label, style = HfType.Note)
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "精选设计\n让好物进入生活",
            style = HfType.Note,
            color = PicoTheme.colorScheme.labelSecondary
        )
    }
}
