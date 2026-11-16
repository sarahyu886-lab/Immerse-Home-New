package com.pico.welcomespace.domain.model

enum class ShopCategory(val label: String) {
    ALL("全部好物"),
    SOFA("沙发座椅"),
    LIGHTING("灯具照明")
}

data class ShopProduct(
    val id: String,
    val name: String,
    val series: String,
    val price: Int,
    val originalPrice: Int,
    val category: ShopCategory,
    val description: String,
    val modelAsset: String,
    val physicalSize: PhysicalSize,
    val modelBaseY: Float = 0f
)

data class PhysicalSize(val width: Float, val height: Float, val depth: Float)
