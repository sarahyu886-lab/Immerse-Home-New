package com.pico.welcomespace.ui.shop.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.foundation.content.Resizability
import com.pico.spatial.ui.foundation.content.Source
import com.pico.spatial.ui.foundation.content.SpatialModelView
import com.pico.spatial.ui.foundation.effect3d.scale3D
import com.pico.spatial.ui.foundation.gesture.TargetEntity
import com.pico.spatial.ui.foundation.gesture.detectHorizontalDragGestures
import com.pico.spatial.ui.foundation.gesture.detectSpatialDragGesture
import com.pico.spatial.ui.foundation.layout.zOffset
import com.pico.welcomespace.domain.model.ShopProduct
import com.pico.welcomespace.ui.shop.ShopEvent
import com.pico.welcomespace.ui.shop.ShopUiState
import com.pico.welcomespace.ui.shop.RoomPlacement
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

@Composable
fun ShopWelcome(state: ShopUiState, onEvent: (ShopEvent) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Spacer(modifier = Modifier.weight(1f))
        if (state.visibleProducts.size > 1) {
            Text(
                text = "在文字区左右拖动翻阅",
                style = PicoTheme.typography.bodySmall,
                color = PicoTheme.colorScheme.labelSecondary
            )
            Spacer(modifier = Modifier.width(14.dp))
            val index = state.visibleProducts.indexOfFirst { it.id == state.selectedProductId }
            Button(
                onClick = { onEvent(ShopEvent.PageMoved(-1)) },
                enabled = index > 0
            ) { Text("上一件") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { onEvent(ShopEvent.PageMoved(1)) },
                enabled = index < state.visibleProducts.lastIndex
            ) { Text("下一件") }
        }
    }
}

@Composable
fun FeaturedProduct(
    product: ShopProduct,
    isFavorite: Boolean,
    onEvent: (ShopEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val (collection, headline) = when (product.id) {
        "chair-01" -> "THE CHAIR EDIT" to "坐下来，\n让时间慢一点。"
        "lamp-01" -> "THE LIGHT EDIT" to "一盏灯，\n点亮家的温度。"
        else -> "THE LIVING EDIT" to "松弛有度，\n自在入座。"
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(315.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(PicoTheme.colorScheme.fillSecondary)
                .padding(horizontal = 32.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.width(350.dp).pointerInput(product.id) {
                    var dragged = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { dragged = 0f },
                        onDragEnd = {
                            if (abs(dragged) > 72f) {
                                onEvent(ShopEvent.PageMoved(if (dragged < 0) 1 else -1))
                            }
                        },
                        context = context,
                        targetedToEntity = TargetEntity.any(),
                        onHorizontalDrag = { change, amount ->
                            dragged += amount
                            change.consume()
                        }
                    )
                }
            ) {
                Text(
                    text = collection,
                    style = PicoTheme.typography.labelMedium,
                    color = PicoTheme.colorScheme.interaction
                )
                Spacer(modifier = Modifier.height(22.dp))
                Text(
                    text = headline,
                    style = PicoTheme.typography.displaySmall,
                    color = PicoTheme.colorScheme.labelPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = product.description,
                    style = PicoTheme.typography.bodyMedium,
                    color = PicoTheme.colorScheme.labelSecondary
                )
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "文字区滑动翻页 · 抓住模型放入样板间",
                    style = PicoTheme.typography.labelLarge,
                    color = PicoTheme.colorScheme.interaction
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                SpatialModelView(
                    source = Source.assets(product.modelAsset),
                    resizability = Resizability.FitInside,
                    modifier = Modifier
                        .size(width = 440.dp, height = 260.dp)
                        .zOffset { (-12).dp.toPx() }
                        .scale3D(0.45f)
                        .pointerInput(product.id) {
                            detectSpatialDragGesture(
                                context = context,
                                targetedToEntity = TargetEntity.any(),
                                onDragStart = { _, value ->
                                    val pose = value.inputDevicePose.rawPosition
                                    RoomPlacement.start(product, pose.x, pose.y, pose.z)
                                },
                                onDragEnd = { value ->
                                    val pose = value.inputDevicePose.rawPosition
                                    RoomPlacement.drop(pose.x, pose.y, pose.z)
                                },
                                onDragCancel = { RoomPlacement.cancel() }
                            ) { value ->
                                val pose = value.inputDevicePose.rawPosition
                                RoomPlacement.move(pose.x, pose.y, pose.z)
                            }
                        }
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(PicoTheme.colorScheme.fillSecondary)
                .padding(horizontal = 26.dp, vertical = 19.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.series,
                    style = PicoTheme.typography.labelSmall,
                    color = PicoTheme.colorScheme.labelTertiary
                )
                Text(
                    text = product.name,
                    style = PicoTheme.typography.titleLarge,
                    color = PicoTheme.colorScheme.labelPrimary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "¥${product.price.shopPrice()}",
                    style = PicoTheme.typography.headlineMedium,
                    color = PicoTheme.colorScheme.interaction
                )
                Text(
                    text = "¥${product.originalPrice.shopPrice()}",
                    style = PicoTheme.typography.bodySmall.copy(
                        textDecoration = TextDecoration.LineThrough
                    ),
                    color = PicoTheme.colorScheme.labelTertiary
                )
            }
            Spacer(modifier = Modifier.width(18.dp))
            Button(onClick = { onEvent(ShopEvent.FavoriteToggled(product.id)) }) {
                Text(if (isFavorite) "已收藏" else "收藏")
            }
            Spacer(modifier = Modifier.width(10.dp))
            Button(onClick = { onEvent(ShopEvent.AddToCart(product.id)) }) {
                Text("加入购物车")
            }
        }
        Text(
            text = RoomPlacement.message,
            style = PicoTheme.typography.bodySmall,
            color = PicoTheme.colorScheme.labelSecondary
        )
    }
}

@Composable
fun ShopEmptyState(query: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(390.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(PicoTheme.colorScheme.fillSecondary)
            .padding(40.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "好物正在路上",
            style = PicoTheme.typography.headlineMedium,
            color = PicoTheme.colorScheme.labelPrimary
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = if (query.isBlank()) "这个分类暂时没有上架商品，试试沙发座椅。"
            else "没有找到“$query”，换个关键词试试。",
            style = PicoTheme.typography.bodyLarge,
            color = PicoTheme.colorScheme.labelSecondary
        )
    }
}

private fun Int.shopPrice(): String = NumberFormat.getIntegerInstance(Locale.CHINA).format(this)
