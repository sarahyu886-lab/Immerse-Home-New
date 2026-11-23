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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
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
import com.pico.welcomespace.ui.planner.PlannerHfColors
import com.pico.welcomespace.ui.planner.components.HfType
import com.pico.welcomespace.ui.shop.ShopEvent
import com.pico.welcomespace.ui.shop.ShopUiState
import com.pico.welcomespace.ui.shop.RoomPlacement
import com.pico.welcomespace.ui.shop.FurnitureEditMode
import com.pico.welcomespace.ui.shop.floorPoint
import com.pico.spatial.core.math.Vector3
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
        FurnitureModeButtons()
        Spacer(modifier = Modifier.weight(1f))
        if (state.visibleProducts.size > 1) {
            Text(
                text = "在文字区左右拖动翻阅",
                style = HfType.Note,
                color = PlannerHfColors.TextSecondary
            )
            Spacer(modifier = Modifier.width(12.dp))
            val index = state.visibleProducts.indexOfFirst { it.id == state.selectedProductId }
            Button(
                onClick = { onEvent(ShopEvent.PageMoved(-1)) },
                enabled = index > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PlannerHfColors.SecondaryContainer,
                    contentColor = PlannerHfColors.TextPrimary
                ),
                modifier = Modifier.height(56.dp),
                shape = CircleShape
            ) { Text("上一件", style = HfType.Label) }
            Spacer(modifier = Modifier.width(6.dp))
            Button(
                onClick = { onEvent(ShopEvent.PageMoved(1)) },
                enabled = index < state.visibleProducts.lastIndex,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PlannerHfColors.SecondaryContainer,
                    contentColor = PlannerHfColors.TextPrimary
                ),
                modifier = Modifier.height(56.dp),
                shape = CircleShape
            ) { Text("下一件", style = HfType.Label) }
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
    // 商品卡离场（切分类/翻页/面板失焦销毁）时兜底取消，避免房间留下跟随中的脏实体。
    DisposableEffect(product.id) {
        onDispose { onEvent(ShopEvent.PlacementDragCancelled) }
    }
    val (collection, headline) = when (product.id) {
        "chair-01" -> "THE CHAIR EDIT" to "坐下来，\n让时间慢一点。"
        "lamp-01" -> "THE LIGHT EDIT" to "一盏灯，\n点亮家的温度。"
        "floor-lamp-01" -> "THE LIGHT EDIT" to "一束暖光，\n陪伴每个夜晚。"
        "rug-01" -> "THE TEXTILE EDIT" to "柔软织纹，\n安放日常。"
        "tv-cabinet-01" -> "THE STORAGE EDIT" to "有序收纳，\n留白生活。"
        else -> "THE LIVING EDIT" to "松弛有度，\n自在入座。"
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(PicoTheme.colorScheme.fillSecondary)
                .padding(horizontal = 28.dp, vertical = 24.dp),
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
                    style = HfType.Note.copy(fontWeight = FontWeight.Medium),
                    color = PicoTheme.colorScheme.interaction
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = product.name,
                    style = HfType.Label,
                    color = PicoTheme.colorScheme.labelPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = with(product.physicalSize) {
                        String.format(Locale.CHINA, "宽 %.3f × 深 %.3f × 高 %.3f m", width, depth, height)
                    },
                    style = HfType.Note,
                    color = PicoTheme.colorScheme.labelSecondary
                )
                Text(product.dimensionSource.label, style = HfType.Note,
                    color = PicoTheme.colorScheme.labelSecondary)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (RoomPlacement.editMode == FurnitureEditMode.ROTATE)
                        "抓住房间内家具，左右拖动旋转\n尺寸保持不变"
                    else "按住模型拖入房间，松手放置\n进入房间后可握持微调",
                    style = HfType.Note,
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
                        .size(width = 360.dp, height = 190.dp)
                        .zOffset { (-12).dp.toPx() }
                        .scale3D(0.45f)
                        .pointerInput(product.id) {
                            var lastPoint: Vector3? = null
                            // 按住并拖动即起手（无长按延迟，真机延迟最低）；
                            // 起手/松手/取消经 ViewModel 桥接状态机路由，命中点直通房间 Stage。
                            detectSpatialDragGesture(
                                context = context,
                                targetedToEntity = TargetEntity.any(),
                                onDragStart = { _, value ->
                                    lastPoint = value.floorPoint()
                                    val point = lastPoint
                                    if (point == null) RoomPlacement.report("保持抓取，将射线移向房间地面")
                                    else onEvent(
                                        ShopEvent.PlacementDragStarted(product.id, point.x, point.y, point.z)
                                    )
                                },
                                onDragEnd = {
                                    lastPoint?.let {
                                        onEvent(ShopEvent.PlacementDragReleased(it.x, it.y, it.z))
                                    }
                                    lastPoint = null
                                },
                                onDragCancel = {
                                    lastPoint = null
                                    onEvent(ShopEvent.PlacementDragCancelled)
                                }
                            ) { value ->
                                value.floorPoint()?.let { point ->
                                    if (lastPoint == null) {
                                        onEvent(
                                            ShopEvent.PlacementDragStarted(product.id, point.x, point.y, point.z)
                                        )
                                    } else {
                                        onEvent(ShopEvent.PlacementDragMoved(point.x, point.y, point.z))
                                    }
                                    lastPoint = point
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
                .padding(horizontal = 28.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.series,
                    style = HfType.Note,
                    color = PicoTheme.colorScheme.labelTertiary
                )
                Text(
                    text = product.name,
                    style = HfType.Label,
                    color = PicoTheme.colorScheme.labelPrimary
                )
                Text(
                    text = with(product.physicalSize) {
                        String.format(Locale.CHINA, "宽 %.3f × 深 %.3f × 高 %.3f m", width, depth, height)
                    },
                    style = HfType.Note,
                    color = PicoTheme.colorScheme.labelPrimary
                )
                Text(
                    text = product.dimensionSource.label,
                    style = HfType.Note,
                    color = PicoTheme.colorScheme.labelSecondary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "¥${product.price.shopPrice()}",
                    style = HfType.Section,
                    color = PicoTheme.colorScheme.interaction
                )
                Text(
                    text = "¥${product.originalPrice.shopPrice()}",
                    style = HfType.Note.copy(
                        textDecoration = TextDecoration.LineThrough
                    ),
                    color = PicoTheme.colorScheme.labelTertiary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = { onEvent(ShopEvent.FavoriteToggled(product.id)) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PlannerHfColors.SecondaryContainer,
                    contentColor = PlannerHfColors.TextSecondary
                ),
                modifier = Modifier.height(56.dp),
                shape = CircleShape
            ) {
                Text(if (isFavorite) "已收藏" else "收藏", style = HfType.Label)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Button(
                onClick = { onEvent(ShopEvent.AddToCart(product.id)) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PlannerHfColors.PrimaryContainer,
                    contentColor = PlannerHfColors.TextPrimary
                ),
                modifier = Modifier.height(56.dp),
                shape = CircleShape
            ) {
                Text("加入购物车", style = HfType.Label)
            }
        }
        Text(
            RoomPlacement.message,
            style = HfType.Note,
            color = PlannerHfColors.TextSecondary
        )
    }
}

@Composable
private fun FurnitureModeButtons() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FurnitureEditMode.entries.forEach { mode ->
            val selected = RoomPlacement.editMode == mode
            Button(
                onClick = { RoomPlacement.editMode = mode },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selected) PlannerHfColors.PrimaryContainer else Color.Transparent,
                    contentColor = PlannerHfColors.TextPrimary
                ),
                modifier = Modifier.height(56.dp),
                shape = CircleShape
            ) {
                Text(
                    if (mode == FurnitureEditMode.MOVE) "移动家具" else "旋转家具",
                    style = HfType.Label
                )
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
            .clip(RoundedCornerShape(24.dp))
            .background(PicoTheme.colorScheme.fillSecondary)
            .padding(horizontal = 28.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "好物正在路上",
            style = HfType.Section,
            color = PicoTheme.colorScheme.labelPrimary
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = if (query.isBlank()) "这个分类暂时没有上架商品，试试沙发座椅。"
            else "没有找到“$query”，换个关键词试试。",
            style = HfType.Note,
            color = PicoTheme.colorScheme.labelSecondary
        )
    }
}

private fun Int.shopPrice(): String = NumberFormat.getIntegerInstance(Locale.CHINA).format(this)
