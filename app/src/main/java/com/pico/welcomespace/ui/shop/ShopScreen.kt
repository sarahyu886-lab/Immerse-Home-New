package com.pico.welcomespace.ui.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pico.spatial.ui.design.Button
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
import com.pico.welcomespace.ui.shop.components.ShopHeader
import com.pico.welcomespace.ui.shop.components.ShopSidebar
import com.pico.welcomespace.ui.shop.components.ShopWelcome
import com.pico.welcomespace.ui.shop.components.ShopEmptyState

@Composable
fun ShopScreen() {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        try {
            if (context.openStage("FurnitureRoom", style = StageStyle.Mixed) !is
                OpenStageResult.Allowed
            ) {
                RoomPlacement.report("虚拟样板间不可用，请检查 Full Stage 能力")
            }
        } catch (error: Exception) {
            RoomPlacement.report("无法打开虚拟样板间，请在支持 Stage 的环境运行")
        }
    }
    val shopViewModel: ShopViewModel = viewModel(factory = ShopViewModel.Factory)
    val state by shopViewModel.state.collectAsStateWithLifecycle()
    ShopContent(state = state, onEvent = shopViewModel::onEvent)
}

@Composable
internal fun ShopContent(state: ShopUiState, onEvent: (ShopEvent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 26.dp, end = 26.dp, top = 16.dp, bottom = 24.dp)
    ) {
        ShopHeader(state = state, onEvent = onEvent)
        when {
            state.showingPayment -> PaymentContent(state, onEvent)
            state.showingCart -> CartContent(state, onEvent)
            else -> HomeContent(state, onEvent)
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
            modifier = Modifier.width(196.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            ShopWelcome(state = state, onEvent = onEvent)
            val product = state.visibleProducts.firstOrNull {
                it.id == state.selectedProductId
            }
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
        modifier = Modifier.fillMaxSize().padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { onEvent(ShopEvent.CloseCart) }) { Text("返回选购") }
            Spacer(modifier = Modifier.width(24.dp))
            Text(
                "购物车",
                style = PicoTheme.typography.headlineMedium,
                color = PicoTheme.colorScheme.labelPrimary
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                "共 ${state.cartCount} 件",
                style = PicoTheme.typography.bodyMedium,
                color = PicoTheme.colorScheme.labelSecondary
            )
        }
        val items = state.products.filter { (state.cartQuantities[it.id] ?: 0) > 0 }
        if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(PicoTheme.colorScheme.fillSecondary),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "购物车还是空的",
                        style = PicoTheme.typography.headlineMedium,
                        color = PicoTheme.colorScheme.labelPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "去挑选一件喜欢的家居好物吧",
                        style = PicoTheme.typography.bodyMedium,
                        color = PicoTheme.colorScheme.labelSecondary
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items.forEach { product ->
                    CartProductRow(
                        product = product,
                        quantity = state.cartQuantities[product.id] ?: 0,
                        onEvent = onEvent
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(PicoTheme.colorScheme.fillSecondary)
                    .padding(horizontal = 28.dp, vertical = 22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "共 ${state.cartCount} 件商品 · 本地演示，不产生订单",
                    style = PicoTheme.typography.bodyMedium,
                    color = PicoTheme.colorScheme.labelSecondary
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    "合计  ¥${state.cartTotal.shopPrice()}",
                    style = PicoTheme.typography.headlineMedium,
                    color = PicoTheme.colorScheme.interaction
                )
                Spacer(modifier = Modifier.width(20.dp))
                Button(onClick = { onEvent(ShopEvent.OpenPayment) }) {
                    Text("去支付")
                }
            }
        }
    }
}

@Composable
internal fun PaymentContent(state: ShopUiState, onEvent: (ShopEvent) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { onEvent(ShopEvent.ClosePayment) }) { Text("返回购物车") }
            Spacer(modifier = Modifier.width(24.dp))
            Text(
                "确认支付",
                style = PicoTheme.typography.headlineMedium,
                color = PicoTheme.colorScheme.labelPrimary
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(PicoTheme.colorScheme.fillSecondary)
                    .padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "商品清单",
                    style = PicoTheme.typography.titleLarge,
                    color = PicoTheme.colorScheme.labelPrimary
                )
                state.products.filter { (state.cartQuantities[it.id] ?: 0) > 0 }
                    .forEach { product ->
                        val quantity = state.cartQuantities[product.id] ?: 0
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${product.name}  × $quantity",
                                modifier = Modifier.weight(1f),
                                style = PicoTheme.typography.bodyMedium,
                                color = PicoTheme.colorScheme.labelPrimary
                            )
                            Text(
                                "¥${(product.price.toLong() * quantity).shopPrice()}",
                                style = PicoTheme.typography.bodyMedium,
                                color = PicoTheme.colorScheme.labelPrimary
                            )
                        }
                    }
            }
            Column(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(PicoTheme.colorScheme.fillSecondary)
                    .padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "支付方式",
                    style = PicoTheme.typography.titleLarge,
                    color = PicoTheme.colorScheme.labelPrimary
                )
                Text(
                    "支付宝 · 前端演示",
                    style = PicoTheme.typography.bodyMedium,
                    color = PicoTheme.colorScheme.interaction
                )
                Text(
                    "尚未接入商户服务端或支付宝 SDK，不会创建订单或扣款。",
                    style = PicoTheme.typography.bodySmall,
                    color = PicoTheme.colorScheme.labelSecondary
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(PicoTheme.colorScheme.fillSecondary)
                .padding(horizontal = 28.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "应付  ¥${state.cartTotal.shopPrice()}",
                    style = PicoTheme.typography.headlineMedium,
                    color = PicoTheme.colorScheme.interaction
                )
                if (state.paymentNoticeVisible) {
                    Text(
                        "演示模式：支付宝支付尚未接入，未发起交易。",
                        style = PicoTheme.typography.bodySmall,
                        color = PicoTheme.colorScheme.labelSecondary
                    )
                }
            }
            Button(onClick = { onEvent(ShopEvent.DemoAlipayClicked) }) {
                Text("支付宝支付（演示）")
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
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(PicoTheme.colorScheme.fillSecondary)
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Box(
            modifier = Modifier.size(width = 150.dp, height = 112.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(PicoTheme.colorScheme.fillTertiary),
            contentAlignment = Alignment.Center
        ) {
            SpatialModelView(
                source = Source.assets(product.modelAsset),
                resizability = Resizability.FitInside,
                modifier = Modifier.size(width = 140.dp, height = 100.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                product.series,
                style = PicoTheme.typography.labelSmall,
                color = PicoTheme.colorScheme.labelTertiary
            )
            Text(
                product.name,
                style = PicoTheme.typography.titleLarge,
                color = PicoTheme.colorScheme.labelPrimary
            )
            Text(
                product.description,
                style = PicoTheme.typography.bodySmall,
                color = PicoTheme.colorScheme.labelSecondary
            )
        }
        Text(
            "¥${product.price.toLong().shopPrice()}",
            style = PicoTheme.typography.titleLarge,
            color = PicoTheme.colorScheme.interaction
        )
        Button(onClick = { onEvent(ShopEvent.CartQuantityChanged(product.id, -1)) }) {
            Text("−")
        }
        Text(
            quantity.toString(),
            style = PicoTheme.typography.titleMedium,
            color = PicoTheme.colorScheme.labelPrimary
        )
        Button(onClick = { onEvent(ShopEvent.CartQuantityChanged(product.id, 1)) }) {
            Text("+")
        }
        Button(onClick = { onEvent(ShopEvent.RemoveFromCart(product.id)) }) {
            Text("移除")
        }
    }
}

private fun Long.shopPrice(): String = java.text.NumberFormat
    .getIntegerInstance(java.util.Locale.CHINA).format(this)
