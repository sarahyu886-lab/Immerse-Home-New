package com.pico.welcomespace.ui.shop

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
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
import com.pico.welcomespace.ui.shop.components.FeaturedProduct
import com.pico.welcomespace.ui.shop.components.ShopEmptyState
import com.pico.welcomespace.ui.shop.components.ShopHeader
import com.pico.welcomespace.ui.shop.components.ShopSidebar
import com.pico.welcomespace.ui.shop.components.ShopWelcome

private val ShopGlass = Color(0x94FFF8ED) // design-style: fixed-figma-color 保留现有 Figma 面板配色。
private val ShopGlassStrong = Color(0xB3FFF8ED) // design-style: fixed-figma-color 保留现有 Figma 面板配色。
private val ShopAccent = Color(0xE6FFBF4A) // design-style: fixed-figma-color 用户指定黄色主操作。
private val ShopText = Color(0xFF292722) // design-style: fixed-figma-color 保留现有浅色面板前景。
private val ShopTextMuted = Color(0xFF6E655B) // design-style: fixed-figma-color 保留现有浅色面板前景。

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
    ShopContent(state = state, onEvent = shopViewModel::onEvent, onNavigateToPlanner = onNavigateToPlanner)
}

@Composable
internal fun ShopContent(
    state: ShopUiState,
    onEvent: (ShopEvent) -> Unit,
    onNavigateToPlanner: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f)
        ) {
            ShopGlassPanel(
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
                    Text("Immerse Home  /  选家具", color = ShopText)
                }
            }
            ShopGlassPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.80f)
                    .align(Alignment.TopCenter)
                    .padding(top = 54.dp),
                radius = 18.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ShopHeader(state = state, onEvent = onEvent)
                    when {
                        state.showingPayment -> PaymentContent(state, onEvent)
                        state.showingCart -> CartContent(state, onEvent)
                        else -> HomeContent(state, onEvent)
                    }
                }
            }
            ShopBottomTabs(
                onNavigateToPlanner = onNavigateToPlanner,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 56.dp)
            )
        }
    }
}

@Composable
private fun HomeContent(state: ShopUiState, onEvent: (ShopEvent) -> Unit) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        ShopSidebar(
            state = state,
            onEvent = onEvent,
            modifier = Modifier.width(188.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ShopWelcome(state = state, onEvent = onEvent)
            val product = state.visibleProducts.firstOrNull { it.id == state.selectedProductId }
            if (product != null) {
                FeaturedProduct(
                    product = product,
                    isFavorite = product.id in state.favorites,
                    onEvent = onEvent
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
        modifier = Modifier.fillMaxSize().padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ShopSecondaryButton(text = "返回选购", onClick = { onEvent(ShopEvent.CloseCart) })
            Spacer(modifier = Modifier.width(18.dp))
            Text("购物车", color = ShopText)
            Spacer(modifier = Modifier.weight(1f))
            Text("共 ${state.cartCount} 件", color = ShopTextMuted)
        }
        val items = state.products.filter { (state.cartQuantities[it.id] ?: 0) > 0 }
        if (items.isEmpty()) {
            ShopGlassPanel(
                modifier = Modifier.fillMaxWidth().weight(1f),
                strong = true,
                radius = 22.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("购物车还是空的", color = ShopText)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("去挑选一件喜欢的家居好物吧", color = ShopTextMuted)
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
            ShopGlassPanel(modifier = Modifier.fillMaxWidth(), radius = 20.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("共 ${state.cartCount} 件商品 · 本地演示，不产生订单", color = ShopTextMuted)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("合计  ¥${state.cartTotal.shopPrice()}", color = ShopAccent)
                    Spacer(modifier = Modifier.width(16.dp))
                    ShopPrimaryButton(text = "去支付", onClick = { onEvent(ShopEvent.OpenPayment) })
                }
            }
        }
    }
}

@Composable
internal fun PaymentContent(state: ShopUiState, onEvent: (ShopEvent) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ShopSecondaryButton(text = "返回购物车", onClick = { onEvent(ShopEvent.ClosePayment) })
            Spacer(modifier = Modifier.width(18.dp))
            Text("确认支付", color = ShopText)
        }
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ShopSectionCard(title = "商品清单") {
                state.products.filter { (state.cartQuantities[it.id] ?: 0) > 0 }.forEach { product ->
                    val quantity = state.cartQuantities[product.id] ?: 0
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${product.name}  × $quantity", modifier = Modifier.weight(1f), color = ShopText)
                        Text("¥${(product.price.toLong() * quantity).shopPrice()}", color = ShopText)
                    }
                }
            }
            ShopSectionCard(title = "支付方式") {
                Text("支付宝 · 前端演示", color = ShopAccent)
                Text("尚未接入商户服务端或支付宝 SDK，不会创建订单或扣款。", color = ShopTextMuted)
            }
        }
        ShopGlassPanel(modifier = Modifier.fillMaxWidth(), radius = 20.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("应付  ¥${state.cartTotal.shopPrice()}", color = ShopAccent)
                    if (state.paymentNoticeVisible) {
                        Text("演示模式：支付宝支付尚未接入，未发起交易。", color = ShopTextMuted)
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
    ShopGlassPanel(modifier = Modifier.fillMaxWidth(), strong = true, radius = 20.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 140.dp, height = 102.dp)
                    .clip(RoundedCornerShape(18.dp))
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
                Text(product.series, color = ShopTextMuted)
                Text(product.name, color = ShopText)
                Text(product.description, color = ShopTextMuted)
            }
            Text("¥${product.price.toLong().shopPrice()}", color = ShopAccent)
            ShopSecondaryButton(text = "−", onClick = { onEvent(ShopEvent.CartQuantityChanged(product.id, -1)) })
            Text(quantity.toString(), color = ShopText)
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
        horizontalArrangement = Arrangement.spacedBy(12.dp),
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
    ShopGlassPanel(modifier = Modifier.fillMaxWidth(), radius = 20.dp) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, color = ShopText)
            content()
        }
    }
}

@Composable
private fun ShopGlassPanel(
    modifier: Modifier = Modifier,
    strong: Boolean = false,
    radius: androidx.compose.ui.unit.Dp = 24.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(radius))
            .background(if (strong) ShopGlassStrong else ShopGlass)
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
            containerColor = ShopAccent,
            contentColor = ShopText
        )
    ) {
        Text(text, color = ShopText)
    }
}

@Composable
private fun ShopSecondaryButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = ShopTextMuted
        )
    ) {
        Text(text, color = ShopTextMuted)
    }
}

private fun Long.shopPrice(): String = java.text.NumberFormat
    .getIntegerInstance(java.util.Locale.CHINA).format(this)
