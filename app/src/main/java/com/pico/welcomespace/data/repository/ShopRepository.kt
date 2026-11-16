package com.pico.welcomespace.data.repository

import com.pico.welcomespace.domain.model.ShopProduct

interface ShopRepository {
    fun products(): List<ShopProduct>
}
