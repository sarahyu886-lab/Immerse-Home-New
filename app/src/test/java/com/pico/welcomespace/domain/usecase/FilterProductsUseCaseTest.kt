package com.pico.welcomespace.domain.usecase

import com.pico.welcomespace.domain.model.ShopCategory
import com.pico.welcomespace.domain.model.ShopProduct
import com.pico.welcomespace.domain.model.PhysicalSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterProductsUseCaseTest {
    private val sofa = ShopProduct(
        id = "sofa", name = "暮色经典双人沙发", series = "LIVING ROOM",
        price = 2699, originalPrice = 3299, category = ShopCategory.SOFA,
        description = "双人位", modelAsset = "sofa.glb",
        physicalSize = PhysicalSize(1.807f, 0.709f, 0.818f)
    )
    private val light = sofa.copy(
        id = "light", name = "柔光台灯", category = ShopCategory.LIGHTING
    )
    private val filter = FilterProductsUseCase()

    @Test
    fun queryMatchesProductNameAndTrimsWhitespace() {
        assertEquals(listOf(sofa), filter(listOf(sofa, light), "  沙发  ", ShopCategory.ALL))
    }

    @Test
    fun categoryExcludesOtherProducts() {
        assertEquals(listOf(light), filter(listOf(sofa, light), "", ShopCategory.LIGHTING))
    }

    @Test
    fun unmatchedKeywordReturnsEmptyList() {
        assertTrue(filter(listOf(sofa), "餐桌", ShopCategory.ALL).isEmpty())
    }
}
