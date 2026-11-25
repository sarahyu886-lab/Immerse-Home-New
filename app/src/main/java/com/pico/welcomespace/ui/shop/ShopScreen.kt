package com.pico.welcomespace.ui.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.ButtonDefaults
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.foundation.content.Resizability
import com.pico.spatial.ui.foundation.content.Source
import com.pico.spatial.ui.foundation.content.SpatialModelView
import com.pico.spatial.ui.platform.containers.OpenStageResult
import com.pico.spatial.ui.platform.containers.StageStyle
import com.pico.spatial.ui.platform.containers.openStage
import com.pico.welcomespace.domain.model.ShopProduct
import com.pico.welcomespace.ui.planner.PlannerHfColors
import com.pico.welcomespace.ui.planner.components.HfType
import com.pico.welcomespace.ui.shop.components.FeaturedProduct
import com.pico.welcomespace.ui.shop.components.ShopEmptyState
import com.pico.welcomespace.ui.shop.components.ShopHeader
import com.pico.welcomespace.ui.shop.components.ShopSidebar
import com.pico.welcomespace.ui.shop.components.ShopWelcome

@Composable
fun ShopScreen(onNavigateToPlanner: () -> Unit = {}) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        try {
            if (context.openStage("FurnitureRoom", style = StageStyle.Mixed) !is OpenStageResult.Allowed) {
                RoomPlacement.report("虚拟样板间不可用，请检查 Full Stage 能力")
            }
        } catch (_: Exception) {
            RoomPlacement.report("无法打开虚拟样板间，请在支持 Stage 的环境运行")
        }
    }
    val shopViewModel: ShopViewModel = viewModel(factory = ShopViewModel.Factory)
    val state by shopViewModel.state.collectAsStateWithLifecycle()
    // 选家具面板离场（切页/销毁）时兜底取消桥接会话，房间侧实体随之回收。
    DisposableEffect(Unit) {
        onDispose { shopViewModel.onEvent(ShopEvent.PlacementDragCancelled) }
    }
    ShopContent(state = state, onEvent = shopViewModel::onEvent, onNavigateToPlanner = onNavigateToPlanner)
}

@Composable
internal fun ShopContent(
    state: ShopUiState,
    onEvent: (ShopEvent) -> Unit,
    onNavigateToPlanner: () -> Unit
) {
    // 拖拽桥接跟随中：面板切弱档玻璃（0.2 级 token），让用户看到房间内的跟随实体。
    val placing = state.placingProductId != null
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        ShopGlassPanel(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.94f),
            weak = placing
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ShopHeader(state = state, onEvent = onEvent)
                Box(modifier = Modifier.weight(1f)) {
                    when {
                        state.showingPayment -> PaymentContent(state, onEvent)
                        state.showingCart -> CartContent(state, onEvent)
                        else -> HomeContent(state, onEvent)
                    }
                }
                ShopBottomTabs(
                    onNavigateToPlanner = onNavigateToPlanner,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun HomeContent(state: ShopUiState, onEvent: (ShopEvent) -> Unit) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        ShopSidebar(
            state = state,
            onEvent = onEvent,
            modifier = Modifier.width(188.dp)
        )
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ShopWelcome(state = state, onEvent = onEvent)
            Text(
                text = RoomPlacement.message,
                style = PicoTheme.typography.bodySmall,
                color = PicoTheme.colorScheme.labelSecondary
            )
            val product = state.visibleProducts.firstOrNull { it.id == state.selectedProductId }
            if (product != null) {
                FeaturedProduct(
                    product = product,
                    isFavorite = product.id in state.favorites,
                    onEvent = onEvent,
                    scrollState = scrollState
                )
            } else {
                ShopEmptyState(query = state.query)
            }
        }
    }
}

@Composable
internal fun CartContent(state: ShopUiState, onEvent: (ShopEvent) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(top = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ShopSecondaryButton(text = "返回选购", onClick = { onEvent(ShopEvent.CloseCart) })
            Spacer(modifier = Modifier.width(12.dp))
            Text("购物车", color = PlannerHfColors.TextPrimary, style = HfType.Label)
            Spacer(modifier = Modifier.weight(1f))
            Text("共 ${state.cartCount} 件", color = PlannerHfColors.TextSecondary, style = HfType.Note)
        }
        val items = state.products.filter { (state.cartQuantities[it.id] ?: 0) > 0 }
        if (items.isEmpty()) {
            ShopGlassPanel(
                modifier = Modifier.fillMaxWidth().weight(1f),
                strong = true
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("购物车还是空的", color = PlannerHfColors.TextPrimary, style = HfType.Label)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "去挑选一件喜欢的家居好物吧",
                        color = PlannerHfColors.TextSecondary,
                        style = HfType.Note
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items.forEach { product ->
                    CartProductRow(
                        product = product,
                        quantity = state.cartQuantities[product.id] ?: 0,
                        onEvent = onEvent
                    )
                }
            }
            ShopGlassPanel(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "共 ${state.cartCount} 件商品",
                        color = PlannerHfColors.TextSecondary,
                        style = HfType.Note
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text("合计  ¥${state.cartTotal.shopPrice()}", color = PlannerHfColors.TextPrimary, style = HfType.Label)
                    Spacer(modifier = Modifier.width(12.dp))
                    ShopPrimaryButton(text = "去支付", onClick = { onEvent(ShopEvent.OpenPayment) })
                }
            }
        }
    }
}

@Composable
internal fun PaymentContent(state: ShopUiState, onEvent: (ShopEvent) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(top = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ShopSecondaryButton(text = "返回购物车", onClick = { onEvent(ShopEvent.ClosePayment) })
            Spacer(modifier = Modifier.width(12.dp))
            Text("确认支付", color = PlannerHfColors.TextPrimary, style = HfType.Label)
        }
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ShopSectionCard(title = "商品清单") {
                state.products.filter { (state.cartQuantities[it.id] ?: 0) > 0 }.forEach { product ->
                    val quantity = state.cartQuantities[product.id] ?: 0
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${product.name}  × $quantity",
                            modifier = Modifier.weight(1f),
                            color = PlannerHfColors.TextPrimary,
                            style = HfType.Note
                        )
                        Text("¥${(product.price.toLong() * quantity).shopPrice()}", color = PlannerHfColors.TextPrimary, style = HfType.Note)
                    }
                }
            }
            ShopSectionCard(title = "支付方式") {
                Text("支付宝 · 前端演示", color = PlannerHfColors.TextPrimary, style = HfType.Label)
                Text(
                    "尚未接入商户服务端或支付宝 SDK，不会创建订单或扣款。",
                    color = PlannerHfColors.TextSecondary,
                    style = HfType.Note
                )
            }
        }
        ShopGlassPanel(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("应付  ¥${state.cartTotal.shopPrice()}", color = PlannerHfColors.TextPrimary, style = HfType.Label)
                    if (state.paymentNoticeVisible) {
                        Text(
                            "演示模式：支付宝支付尚未接入，未发起交易。",
                            color = PlannerHfColors.TextSecondary,
                            style = HfType.Note
                        )
                    }
                }
                ShopPrimaryButton(text = "支付宝支付（演示）", onClick = { onEvent(ShopEvent.DemoAlipayClicked) })
            }
        }
    }
}

@Composable
private fun CartProductRow(
    product: ShopProduct,
    quantity: Int,
    onEvent: (ShopEvent) -> Unit
) {
    ShopGlassPanel(modifier = Modifier.fillMaxWidth(), strong = true) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 140.dp, height = 102.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.28f)),
                contentAlignment = Alignment.Center
            ) {
                SpatialModelView(
                    source = Source.assets(product.modelAsset),
                    resizability = Resizability.FitInside,
                    modifier = Modifier.size(width = 130.dp, height = 92.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(product.series, color = PlannerHfColors.TextSecondary, style = HfType.Note)
                Text(product.name, color = PlannerHfColors.TextPrimary, style = HfType.Label)
                Text(product.description, color = PlannerHfColors.TextSecondary, style = HfType.Note)
            }
            Text("¥${product.price.toLong().shopPrice()}", color = PlannerHfColors.TextPrimary, style = HfType.Label)
            ShopSecondaryButton(text = "−", onClick = { onEvent(ShopEvent.CartQuantityChanged(product.id, -1)) })
            Text(quantity.toString(), color = PlannerHfColors.TextPrimary, style = HfType.Note)
            ShopSecondaryButton(text = "+", onClick = { onEvent(ShopEvent.CartQuantityChanged(product.id, 1)) })
            ShopSecondaryButton(text = "移除", onClick = { onEvent(ShopEvent.RemoveFromCart(product.id)) })
        }
    }
}

@Composable
private fun ShopBottomTabs(
    onNavigateToPlanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShopSecondaryButton(text = "我的户型", onClick = onNavigateToPlanner)
        ShopPrimaryButton(text = "选家具", onClick = {})
    }
}

@Composable
private fun ShopSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    ShopGlassPanel(modifier = Modifier.fillMaxWidth(), radius = 24.dp) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, color = PlannerHfColors.TextPrimary, style = HfType.Label)
            content()
        }
    }
}

@Composable
private fun ShopGlassPanel(
    modifier: Modifier = Modifier,
    strong: Boolean = false,
    weak: Boolean = false,
    radius: androidx.compose.ui.unit.Dp = 24.dp,
    content: @Composable BoxScope.() -> Unit
) {
    // 玻璃档位沿用 PlannerHfColors tokens：强档 0.75（默认/strong），弱档 0.2（拖拽桥接跟随中）。
    val fill = when {
        weak -> PlannerHfColors.GlassWeak
        strong -> PlannerHfColors.GlassStrong
        else -> PlannerHfColors.GlassFill
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(radius))
            .background(fill)
            .border(1.dp, PlannerHfColors.GlassBorder, RoundedCornerShape(radius))
    ) {
        Box(modifier = Modifier.fillMaxSize(), content = content)
    }
}

@Composable
private fun ShopPrimaryButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = PlannerHfColors.PrimaryContainer,
            contentColor = PlannerHfColors.TextPrimary
        ),
        modifier = Modifier.height(56.dp),
        shape = CircleShape
    ) {
        Text(text, color = PlannerHfColors.TextPrimary, style = HfType.Label)
    }
}

@Composable
private fun ShopSecondaryButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = PlannerHfColors.SecondaryContainer,
            contentColor = PlannerHfColors.TextSecondary
        ),
        modifier = Modifier.height(56.dp),
        shape = CircleShape
    ) {
        Text(text, color = PlannerHfColors.TextSecondary, style = HfType.Label)
    }
}

private fun Long.shopPrice(): String = java.text.NumberFormat
    .getIntegerInstance(java.util.Locale.CHINA).format(this)
