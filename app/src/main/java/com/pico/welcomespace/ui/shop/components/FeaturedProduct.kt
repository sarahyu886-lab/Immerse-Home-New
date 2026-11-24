package com.pico.welcomespace.ui.shop.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.ButtonDefaults
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
import com.pico.welcomespace.ui.shop.FurnitureEditMode
import com.pico.welcomespace.ui.shop.floorPoint
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

@Composable
fun ShopWelcome(state: ShopUiState, onEvent: (ShopEvent) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
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
                enabled = index > 0,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent,
                    contentColor = PicoTheme.colorScheme.labelPrimary)
            ) { Text("上一件") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { onEvent(ShopEvent.PageMoved(1)) },
                enabled = index < state.visibleProducts.lastIndex,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent,
                    contentColor = PicoTheme.colorScheme.labelPrimary)
            ) { Text("下一件") }
        }
    }
}

@Composable
fun FeaturedProduct(
    product: ShopProduct,
    isFavorite: Boolean,
    onEvent: (ShopEvent) -> Unit,
    scrollState: ScrollState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val (collection, headline) = when (product.id) {
        "chair-01" -> "THE CHAIR EDIT" to "坐下来，\n让时间慢一点。"
        "lamp-01" -> "THE LIGHT EDIT" to "一盏灯，\n点亮家的温度。"
        "floor-lamp-01" -> "THE LIGHT EDIT" to "一束暖光，\n陪伴每个夜晚。"
        "rug-01" -> "THE TEXTILE EDIT" to "柔软织纹，\n安放日常。"
        "tv-cabinet-01" -> "THE STORAGE EDIT" to "有序收纳，\n留白生活。"
        else -> "THE LIVING EDIT" to "松弛有度，\n自在入座。"
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            // 用圆角背景代替 clip：滚动时模型要移出卡片边界，不能被卡片裁掉。
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .background(PicoTheme.colorScheme.fillSecondary, RoundedCornerShape(28.dp))
                .padding(horizontal = 24.dp, vertical = 16.dp),
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
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = product.name,
                    style = PicoTheme.typography.titleLarge,
                    color = PicoTheme.colorScheme.labelPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = with(product.physicalSize) {
                        String.format(Locale.CHINA, "宽 %.3f × 深 %.3f × 高 %.3f m", width, depth, height)
                    },
                    style = PicoTheme.typography.bodyMedium,
                    color = PicoTheme.colorScheme.labelSecondary
                )
                Text(product.dimensionSource.label, style = PicoTheme.typography.bodySmall,
                    color = PicoTheme.colorScheme.labelSecondary)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (RoomPlacement.editMode == FurnitureEditMode.ROTATE)
                        "抓住房间内家具，左右拖动旋转\n尺寸保持不变"
                    else "抓住预览模型，指向地面后松手\n放下后可继续抓取移动",
                    style = PicoTheme.typography.bodySmall,
                    color = PicoTheme.colorScheme.interaction
                )
            }
            Box(
                // 抵消外层滚动位移，让悬浮模型始终停在窗口上的同一位置。
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .offset { IntOffset(0, scrollState.value) },
                contentAlignment = Alignment.Center
            ) {
                SpatialModelView(
                    source = Source.assets(product.modelAsset),
                    resizability = Resizability.FitInside,
                    modifier = Modifier
                        .size(width = 360.dp, height = 190.dp)
                        // 正值朝用户：模型浮在窗口平面前，射线才会先命中模型。
                        .zOffset { 8.dp.toPx() }
                        .scale3D(0.45f)
                        .pointerInput(product.id) {
                            var placed = false
                            detectSpatialDragGesture(
                                context = context,
                                targetedToEntity = TargetEntity.any(),
                                onDragStart = { _, value ->
                                    val point = value.floorPoint()
                                    placed = true
                                    RoomPlacement.start(product, point.x, point.y, point.z)
                                },
                                onDragEnd = { value ->
                                    val point = value.floorPoint()
                                    RoomPlacement.drop(point.x, point.y, point.z)
                                    placed = false
                                },
                                onDragCancel = { placed = false; RoomPlacement.cancel() }
                            ) { value ->
                                val point = value.floorPoint()
                                if (placed) {
                                    RoomPlacement.move(point.x, point.y, point.z)
                                } else {
                                    placed = true
                                    RoomPlacement.start(product, point.x, point.y, point.z)
                                }
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
                Text(
                    text = with(product.physicalSize) {
                        String.format(Locale.CHINA, "宽 %.3f × 深 %.3f × 高 %.3f m", width, depth, height)
                    },
                    style = PicoTheme.typography.bodyMedium,
                    color = PicoTheme.colorScheme.labelPrimary
                )
                Text(
                    text = product.dimensionSource.label,
                    style = PicoTheme.typography.bodySmall,
                    color = PicoTheme.colorScheme.labelSecondary
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
