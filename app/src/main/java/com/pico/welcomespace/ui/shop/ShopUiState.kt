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
    val paymentNoticeVisible: Boolean = false
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
}
