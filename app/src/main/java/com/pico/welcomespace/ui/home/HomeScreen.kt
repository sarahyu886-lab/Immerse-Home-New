package com.pico.welcomespace.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.ButtonDefaults
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text
import com.pico.welcomespace.ui.planner.RoomPlannerScreen
import com.pico.welcomespace.ui.shop.ShopScreen

@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Box(modifier = Modifier.fillMaxSize()) {
        when (state.selectedTab) {
            HomeTab.SHOP -> ShopScreen()
            HomeTab.PLANNER -> RoomPlannerScreen()
        }
        HomeBottomTabs(
            selectedTab = state.selectedTab,
            onTabSelected = { viewModel.onEvent(HomeEvent.TabSelected(it)) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun HomeBottomTabs(
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(bottom = 22.dp)
            .clip(RoundedCornerShape(46.dp))
            .background(PicoTheme.colorScheme.fillSecondary.copy(alpha = 0.92f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoundTabButton(
            tab = HomeTab.SHOP,
            selected = selectedTab == HomeTab.SHOP,
            onClick = { onTabSelected(HomeTab.SHOP) }
        )
        RoundTabButton(
            tab = HomeTab.PLANNER,
            selected = selectedTab == HomeTab.PLANNER,
            onClick = { onTabSelected(HomeTab.PLANNER) }
        )
    }
}

@Composable
private fun RoundTabButton(
    tab: HomeTab,
    selected: Boolean,
    onClick: () -> Unit
) {
    val iconColor = if (selected) {
        PicoTheme.colorScheme.labelPrimaryLight
    } else {
        PicoTheme.colorScheme.labelPrimary
    }
    Button(
        onClick = onClick,
        modifier = Modifier
            .size(72.dp)
            .semantics { contentDescription = "切换到${tab.label}页面" },
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) {
                PicoTheme.colorScheme.interaction
            } else {
                PicoTheme.colorScheme.fillTertiary
            },
            contentColor = iconColor
        )
    ) {
        if (tab == HomeTab.SHOP) {
            ShopGlyph(iconColor = iconColor)
        } else {
            PlanGlyph(iconColor = iconColor)
        }
    }
}

@Composable
private fun ShopGlyph(iconColor: androidx.compose.ui.graphics.Color) {
    Canvas(modifier = Modifier.size(30.dp)) {
        val stroke = 2.4.dp.toPx()
        val cart = androidx.compose.ui.graphics.Path().apply {
            moveTo(size.width * .12f, size.height * .22f)
            lineTo(size.width * .25f, size.height * .22f)
            lineTo(size.width * .36f, size.height * .70f)
            lineTo(size.width * .78f, size.height * .70f)
            lineTo(size.width * .88f, size.height * .36f)
            lineTo(size.width * .32f, size.height * .36f)
        }
        drawPath(cart, iconColor, style = Stroke(width = stroke, cap = StrokeCap.Round))
        drawCircle(iconColor, radius = stroke, center = Offset(size.width * .42f, size.height * .86f))
        drawCircle(iconColor, radius = stroke, center = Offset(size.width * .72f, size.height * .86f))
    }
}

@Composable
private fun PlanGlyph(iconColor: androidx.compose.ui.graphics.Color) {
    Canvas(modifier = Modifier.size(30.dp)) {
        val stroke = 2.4.dp.toPx()
        drawRect(
            color = iconColor,
            topLeft = Offset(size.width * .18f, size.height * .18f),
            size = androidx.compose.ui.geometry.Size(size.width * .64f, size.height * .64f),
            style = Stroke(width = stroke)
        )
        drawLine(
            iconColor,
            Offset(size.width * .18f, size.height * .48f),
            Offset(size.width * .58f, size.height * .48f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            iconColor,
            Offset(size.width * .58f, size.height * .18f),
            Offset(size.width * .58f, size.height * .82f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}
