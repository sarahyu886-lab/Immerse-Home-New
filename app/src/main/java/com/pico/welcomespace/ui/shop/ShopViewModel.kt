package com.pico.welcomespace.ui.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.pico.welcomespace.data.repository.FakeShopRepository
import com.pico.welcomespace.data.repository.ShopRepository
import com.pico.welcomespace.domain.usecase.FilterProductsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ShopViewModel(
    repository: ShopRepository,
    private val filterProducts: FilterProductsUseCase = FilterProductsUseCase()
) : ViewModel() {
    private val initialProducts = repository.products()
    private val _state = MutableStateFlow(
        ShopUiState(
            products = initialProducts,
            visibleProducts = initialProducts,
            selectedProductId = initialProducts.firstOrNull()?.id
        )
    )
    val state: StateFlow<ShopUiState> = _state.asStateFlow()

    private val placementBridge = PlacementBridge()

    fun onEvent(event: ShopEvent) {
        if (event is ShopEvent.PlacementDragMoved) {
            // 跟随中的射线命中点更新直通房间 Stage；移动不改变 UI 状态，避免面板每帧重组。
            if (placementBridge.isFollowing) RoomPlacement.move(event.x, event.y, event.z)
            return
        }
        _state.update { current ->
            when (event) {
                is ShopEvent.PlacementDragStarted -> {
                    val product = current.products.firstOrNull { it.id == event.productId }
                    if (product == null || !placementBridge.begin(event.productId)) current
                    else if (RoomPlacement.start(product, event.x, event.y, event.z)) {
                        current.copy(placingProductId = event.productId)
                    } else {
                        // 房间 Stage 未就绪：起手被拒，状态机立即复位，不留脏会话。
                        placementBridge.end()
                        current
                    }
                }
                is ShopEvent.PlacementDragReleased -> {
                    if (!placementBridge.isFollowing) current
                    else {
                        RoomPlacement.drop(event.x, event.y, event.z)
                        placementBridge.end()
                        current.copy(placingProductId = null)
                    }
                }
                ShopEvent.PlacementDragCancelled -> {
                    if (!placementBridge.isFollowing) current
                    else {
                        RoomPlacement.cancel()
                        placementBridge.end()
                        current.copy(placingProductId = null)
                    }
                }
                // Moved 已在方法入口直通处理，此分支仅为 when 穷尽性保留，实际不可达。
                is ShopEvent.PlacementDragMoved -> current
                is ShopEvent.QueryChanged -> {
                    val visible = filterProducts(
                        current.products,
                        event.value,
                        current.selectedCategory
                    )
                    current.copy(
                        query = event.value,
                        visibleProducts = visible,
                        selectedProductId = visible.firstOrNull()?.id
                    )
                }
                is ShopEvent.CategorySelected -> {
                    val visible = filterProducts(current.products, current.query, event.value)
                    current.copy(
                        selectedCategory = event.value,
                        visibleProducts = visible,
                        selectedProductId = visible.firstOrNull()?.id
                    )
                }
                is ShopEvent.PageMoved -> {
                    if (current.visibleProducts.isEmpty()) current
                    else {
                        val index = current.visibleProducts.indexOfFirst {
                            it.id == current.selectedProductId
                        }
                        val next = (index + event.direction).coerceIn(
                            0, current.visibleProducts.lastIndex
                        )
                        current.copy(selectedProductId = current.visibleProducts[next].id)
                    }
                }
                is ShopEvent.FavoriteToggled -> {
                    if (current.products.none { it.id == event.productId }) current
                    else current.copy(
                        favorites = if (event.productId in current.favorites) {
                            current.favorites - event.productId
                        } else {
                            current.favorites + event.productId
                        }
                    )
                }
                is ShopEvent.AddToCart -> {
                    if (current.products.none { it.id == event.productId }) current
                    else current.copy(cartQuantities = current.cartQuantities +
                        (event.productId to ((current.cartQuantities[event.productId] ?: 0) + 1)))
                }
                is ShopEvent.CartQuantityChanged -> {
                    val quantity = current.cartQuantities[event.productId] ?: 0
                    if (quantity == 0 || event.change !in listOf(-1, 1)) current
                    else {
                        val next = quantity + event.change
                        current.copy(cartQuantities = if (next == 0) {
                            current.cartQuantities - event.productId
                        } else {
                            current.cartQuantities + (event.productId to next)
                        })
                    }
                }
                is ShopEvent.RemoveFromCart -> current.copy(
                    cartQuantities = current.cartQuantities - event.productId
                )
                ShopEvent.OpenCart -> current.copy(
                    showingCart = true, showingPayment = false, paymentNoticeVisible = false
                )
                ShopEvent.CloseCart -> current.copy(
                    showingCart = false, showingPayment = false, paymentNoticeVisible = false
                )
                ShopEvent.GoHome -> current.copy(
                    showingCart = false, showingPayment = false, paymentNoticeVisible = false
                )
                ShopEvent.OpenPayment -> if (current.showingCart && current.cartCount > 0) {
                    current.copy(showingPayment = true, paymentNoticeVisible = false)
                } else current
                ShopEvent.ClosePayment -> current.copy(
                    showingPayment = false, paymentNoticeVisible = false
                )
                ShopEvent.DemoAlipayClicked -> if (current.showingPayment) {
                    current.copy(paymentNoticeVisible = true)
                } else current
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ShopViewModel(FakeShopRepository()) as T
        }
    }
}
