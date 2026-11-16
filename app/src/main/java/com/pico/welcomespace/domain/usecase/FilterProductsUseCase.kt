package com.pico.welcomespace.domain.usecase

import com.pico.welcomespace.domain.model.ShopCategory
import com.pico.welcomespace.domain.model.ShopProduct

class FilterProductsUseCase {
    operator fun invoke(
        products: List<ShopProduct>,
        query: String,
        category: ShopCategory
    ): List<ShopProduct> {
        val keyword = query.trim()
        return products.filter { product ->
            (category == ShopCategory.ALL || product.category == category) &&
                (keyword.isEmpty() ||
                    product.name.contains(keyword, ignoreCase = true) ||
                    product.series.contains(keyword, ignoreCase = true))
        }
    }
}
