package com.pico.welcomespace.ui.shop

import com.pico.welcomespace.data.repository.FakeShopRepository
import com.pico.welcomespace.domain.model.FloorPlanRoom
import com.pico.welcomespace.domain.model.RoomOpeningKind
import com.pico.welcomespace.domain.model.ShopCategory
import com.pico.welcomespace.domain.model.RoomSurfaceKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShopViewModelTest {
    private fun viewModel() = ShopViewModel(FakeShopRepository())

    @Test
    fun initialStateShowsSofa() {
        val state = viewModel().state.value
        assertEquals(3, state.visibleProducts.size)
        assertEquals("sofa-02", state.selectedProductId)
    }

    @Test
    fun modelDimensionsAreDeclaredInMetersForRoomPlacement() {
        val products = viewModel().state.value.products
        assertEquals(1.807f, products[0].physicalSize.width, 0.001f)
        assertEquals(0.848f, products[1].physicalSize.width, 0.001f)
        assertEquals(0.893f, products[2].physicalSize.height, 0.001f)
        assertTrue(products.all {
            it.physicalSize.width > 0f &&
                it.physicalSize.height > 0f &&
                it.physicalSize.depth > 0f
        })
    }

    @Test
    fun roomPlacementKeepsFurnitureWithinDetectedPlane() {
        val sofa = viewModel().state.value.products.first().physicalSize
        assertTrue(fitsSurface(sofa, 4f, 4f, 0f, 0f))
        assertFalse(fitsSurface(sofa, 1.5f, 1.5f, 0f, 0f))
        assertFalse(fitsSurface(sofa, 4f, 4f, 1.5f, 0f))
    }

    @Test
    fun floorPlanRoomUsesMeterScaleFromReferenceImage() {
        val plan = FloorPlanRoom.demoPlan
        assertEquals(11f, plan.widthMeters, 0.001f)
        assertEquals(11.85f, plan.depthMeters, 0.001f)
        assertTrue(plan.walls.size >= 40)
        assertTrue(plan.surfaces.any { it.label == "起居室地面" })
        assertTrue(plan.surfaces.any { it.label == "书房地面" })
        assertTrue(plan.openings.count { it.kind == RoomOpeningKind.DOOR } >= 8)
        assertTrue(plan.openings.count { it.kind == RoomOpeningKind.WINDOW } >= 8)
    }

    @Test
    fun virtualRoomSnapsSeatingToFloorAndLightingToTable() {
        val products = viewModel().state.value.products
        val sofa = products.first { it.id == "sofa-02" }
        val lamp = products.first { it.id == "lamp-01" }

        val sofaTarget = FloorPlanRoom.choosePlacement(sofa, x = -0.4f, z = 1.2f)
        val lampTarget = FloorPlanRoom.choosePlacement(lamp, x = -1.6f, z = 0.75f)

        assertEquals(RoomSurfaceKind.FLOOR, sofaTarget?.surface?.kind)
        assertEquals(RoomSurfaceKind.TABLE, lampTarget?.surface?.kind)
        assertEquals(0f, sofaTarget?.y ?: -1f, 0.001f)
        assertTrue((lampTarget?.y ?: 0f) > 0.5f)
    }

    @Test
    fun virtualRoomClampsDraggedFurnitureInsideLegalSurface() {
        val sofa = viewModel().state.value.products.first { it.id == "sofa-02" }
        val target = FloorPlanRoom.choosePlacement(sofa, x = 20f, z = 20f)

        assertEquals(RoomSurfaceKind.FLOOR, target?.surface?.kind)
        assertTrue((target?.x ?: 20f) < 5f)
        assertTrue((target?.z ?: 20f) < 6f)
    }

    @Test
    fun searchingKnownProductKeepsItVisible() {
        val vm = viewModel()
        vm.onEvent(ShopEvent.QueryChanged("沙发"))
        assertEquals(1, vm.state.value.visibleProducts.size)
    }

    @Test
    fun searchingUnknownProductShowsEmptyState() {
        val vm = viewModel()
        vm.onEvent(ShopEvent.QueryChanged("餐桌"))
        assertTrue(vm.state.value.visibleProducts.isEmpty())
        assertEquals(null, vm.state.value.selectedProductId)
    }

    @Test
    fun categoryChangeFiltersAndCanRestoreAll() {
        val vm = viewModel()
        vm.onEvent(ShopEvent.CategorySelected(ShopCategory.LIGHTING))
        assertEquals("lamp-01", vm.state.value.selectedProductId)
        assertEquals(1, vm.state.value.visibleProducts.size)
        vm.onEvent(ShopEvent.CategorySelected(ShopCategory.ALL))
        assertEquals(3, vm.state.value.visibleProducts.size)
        assertEquals("sofa-02", vm.state.value.selectedProductId)
    }

    @Test
    fun swipingPagesStaysWithinTheVisibleCategory() {
        val vm = viewModel()
        vm.onEvent(ShopEvent.PageMoved(1))
        assertEquals("chair-01", vm.state.value.selectedProductId)
        vm.onEvent(ShopEvent.CategorySelected(ShopCategory.SOFA))
        assertEquals("sofa-02", vm.state.value.selectedProductId)
        vm.onEvent(ShopEvent.PageMoved(1))
        assertEquals("chair-01", vm.state.value.selectedProductId)
        vm.onEvent(ShopEvent.PageMoved(1))
        assertEquals("chair-01", vm.state.value.selectedProductId)
        vm.onEvent(ShopEvent.PageMoved(-1))
        assertEquals("sofa-02", vm.state.value.selectedProductId)
    }

    @Test
    fun emptySearchIgnoresPageGesturesAndNewCategoryResetsSelection() {
        val vm = viewModel()
        vm.onEvent(ShopEvent.QueryChanged("餐桌"))
        vm.onEvent(ShopEvent.PageMoved(1))
        assertEquals(null, vm.state.value.selectedProductId)
        vm.onEvent(ShopEvent.QueryChanged(""))
        vm.onEvent(ShopEvent.CategorySelected(ShopCategory.LIGHTING))
        assertEquals("lamp-01", vm.state.value.selectedProductId)
    }

    @Test
    fun favoriteTogglesAndUnknownCartItemIsIgnored() {
        val vm = viewModel()
        vm.onEvent(ShopEvent.FavoriteToggled("sofa-02"))
        assertTrue("sofa-02" in vm.state.value.favorites)
        vm.onEvent(ShopEvent.FavoriteToggled("sofa-02"))
        assertFalse("sofa-02" in vm.state.value.favorites)
        vm.onEvent(ShopEvent.AddToCart("missing"))
        assertEquals(0, vm.state.value.cartCount)
    }

    @Test
    fun addToCartUpdatesCount() {
        val vm = viewModel()
        vm.onEvent(ShopEvent.AddToCart("sofa-02"))
        assertEquals(1, vm.state.value.cartCount)
        assertEquals(1, vm.state.value.cartQuantities["sofa-02"])
        assertEquals(2699L, vm.state.value.cartTotal)
    }

    @Test
    fun cartShowsOnlyAddedProductsAndPreservesSelectionOnReturn() {
        val vm = viewModel()
        vm.onEvent(ShopEvent.PageMoved(1))
        vm.onEvent(ShopEvent.AddToCart("chair-01"))
        vm.onEvent(ShopEvent.AddToCart("sofa-02"))
        vm.onEvent(ShopEvent.AddToCart("sofa-02"))
        vm.onEvent(ShopEvent.OpenCart)
        assertTrue(vm.state.value.showingCart)
        assertEquals(mapOf("chair-01" to 1, "sofa-02" to 2), vm.state.value.cartQuantities)
        assertEquals(7297L, vm.state.value.cartTotal)
        vm.onEvent(ShopEvent.CloseCart)
        assertFalse(vm.state.value.showingCart)
        assertEquals("chair-01", vm.state.value.selectedProductId)
        assertEquals(3, vm.state.value.cartCount)
    }

    @Test
    fun cartQuantityCannotGoNegativeAndRemoveClearsItem() {
        val vm = viewModel()
        vm.onEvent(ShopEvent.CartQuantityChanged("sofa-02", 1))
        assertEquals(0, vm.state.value.cartCount)
        vm.onEvent(ShopEvent.AddToCart("sofa-02"))
        vm.onEvent(ShopEvent.CartQuantityChanged("sofa-02", -1))
        assertTrue(vm.state.value.cartQuantities.isEmpty())
        vm.onEvent(ShopEvent.AddToCart("lamp-01"))
        vm.onEvent(ShopEvent.CartQuantityChanged("lamp-01", 1))
        assertEquals(2, vm.state.value.cartQuantities["lamp-01"])
        vm.onEvent(ShopEvent.RemoveFromCart("lamp-01"))
        assertEquals(0L, vm.state.value.cartTotal)
        assertTrue(vm.state.value.cartQuantities.isEmpty())
    }

    @Test
    fun paymentRequiresNonEmptyCartAndReturnsToCart() {
        val vm = viewModel()
        vm.onEvent(ShopEvent.OpenPayment)
        assertFalse(vm.state.value.showingPayment)
        vm.onEvent(ShopEvent.OpenCart)
        vm.onEvent(ShopEvent.OpenPayment)
        assertFalse(vm.state.value.showingPayment)
        vm.onEvent(ShopEvent.AddToCart("sofa-02"))
        vm.onEvent(ShopEvent.OpenPayment)
        assertTrue(vm.state.value.showingPayment)
        assertEquals(2699L, vm.state.value.cartTotal)
        vm.onEvent(ShopEvent.ClosePayment)
        assertFalse(vm.state.value.showingPayment)
        assertTrue(vm.state.value.showingCart)
        assertEquals(1, vm.state.value.cartCount)
    }

    @Test
    fun demoPaymentNeverChangesCartOrCreatesPaymentResult() {
        val vm = viewModel()
        vm.onEvent(ShopEvent.AddToCart("lamp-01"))
        vm.onEvent(ShopEvent.OpenCart)
        vm.onEvent(ShopEvent.OpenPayment)
        vm.onEvent(ShopEvent.DemoAlipayClicked)
        assertTrue(vm.state.value.paymentNoticeVisible)
        assertEquals(699L, vm.state.value.cartTotal)
        assertEquals(mapOf("lamp-01" to 1), vm.state.value.cartQuantities)
        vm.onEvent(ShopEvent.ClosePayment)
        assertFalse(vm.state.value.paymentNoticeVisible)
    }

    @Test
    fun homeFromCartKeepsItemsAndSelectedProduct() {
        val vm = viewModel()
        vm.onEvent(ShopEvent.PageMoved(1))
        vm.onEvent(ShopEvent.AddToCart("chair-01"))
        vm.onEvent(ShopEvent.OpenCart)
        vm.onEvent(ShopEvent.GoHome)
        assertFalse(vm.state.value.showingCart)
        assertFalse(vm.state.value.showingPayment)
        assertEquals("chair-01", vm.state.value.selectedProductId)
        assertEquals(mapOf("chair-01" to 1), vm.state.value.cartQuantities)
    }

    @Test
    fun navigationFromPaymentPreservesCartAndClearsDemoNotice() {
        val vm = viewModel()
        vm.onEvent(ShopEvent.AddToCart("sofa-02"))
        vm.onEvent(ShopEvent.OpenCart)
        vm.onEvent(ShopEvent.OpenPayment)
        vm.onEvent(ShopEvent.DemoAlipayClicked)
        vm.onEvent(ShopEvent.OpenCart)
        assertTrue(vm.state.value.showingCart)
        assertFalse(vm.state.value.showingPayment)
        assertFalse(vm.state.value.paymentNoticeVisible)
        vm.onEvent(ShopEvent.OpenPayment)
        vm.onEvent(ShopEvent.GoHome)
        assertFalse(vm.state.value.showingCart)
        assertFalse(vm.state.value.showingPayment)
        assertEquals(2699L, vm.state.value.cartTotal)
    }
}
