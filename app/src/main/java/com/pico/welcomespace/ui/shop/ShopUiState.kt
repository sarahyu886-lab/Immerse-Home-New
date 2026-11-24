package com.pico.welcomespace.ui.shop

import com.pico.welcomespace.domain.model.ShopCategory
import com.pico.welcomespace.domain.model.ShopProduct

data class ShopUiState(
    val query: String = "",
    val selectedCategory: ShopCategory = ShopCategory.ALL,
    val products: List<ShopProduct> = emptyList(),
    val visibleProducts: List<ShopProduct> = emptyList(),
    val selectedProductId: String? = null,
    val favorites: Set<String> = emptySet(),
    val cartQuantities: Map<String, Int> = emptyMap(),
    val showingCart: Boolean = false,
    val showingPayment: Boolean = false,
    val paymentNoticeVisible: Boolean = false,
    /** 拖拽桥接跟随中的商品 id；非空时选家具面板切弱档玻璃，房间内实体跟随放置射线。 */
    val placingProductId: String? = null
) {
    val cartCount: Int get() = cartQuantities.values.sum()
    val cartTotal: Long get() = products.sumOf { product ->
        product.price.toLong() * (cartQuantities[product.id] ?: 0)
    }
}

sealed interface ShopEvent {
    data class QueryChanged(val value: String) : ShopEvent
    data class CategorySelected(val value: ShopCategory) : ShopEvent
    data class PageMoved(val direction: Int) : ShopEvent
    data class FavoriteToggled(val productId: String) : ShopEvent
    data class AddToCart(val productId: String) : ShopEvent
    data class CartQuantityChanged(val productId: String, val change: Int) : ShopEvent
    data class RemoveFromCart(val productId: String) : ShopEvent
    data object OpenCart : ShopEvent
    data object CloseCart : ShopEvent
    data object GoHome : ShopEvent
    data object OpenPayment : ShopEvent
    data object ClosePayment : ShopEvent
    data object DemoAlipayClicked : ShopEvent

    /** 拖拽桥接：商品卡 3D 预览按住并拖动起手（射线命中房间地面时触发）。 */
    data class PlacementDragStarted(
        val productId: String,
        val x: Float,
        val y: Float,
        val z: Float
    ) : ShopEvent

    /** 拖拽桥接：跟随中射线命中点更新（直通房间 Stage，不产生 UI 状态变更）。 */
    data class PlacementDragMoved(val x: Float, val y: Float, val z: Float) : ShopEvent

    /** 拖拽桥接：面板侧检测到指针松手，实体落地固定，房间内抓取/移动接管。 */
    data class PlacementDragReleased(val x: Float, val y: Float, val z: Float) : ShopEvent

    /** 拖拽桥接：拖拽被系统打断（取消/面板失焦），实体回收、状态复位。 */
    data object PlacementDragCancelled : ShopEvent
}
