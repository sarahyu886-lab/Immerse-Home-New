package com.pico.welcomespace.data.repository

import com.pico.welcomespace.domain.model.ShopCategory
import com.pico.welcomespace.domain.model.ShopProduct
import com.pico.welcomespace.domain.model.PhysicalSize

class FakeShopRepository : ShopRepository {
    override fun products(): List<ShopProduct> = listOf(
        ShopProduct(
            id = "sofa-02",
            name = "暮色经典双人沙发",
            series = "LIVING ROOM / 经典系列",
            price = 2699,
            originalPrice = 3299,
            category = ShopCategory.SOFA,
            description = "舒适的双人位廓形，为客厅留一处自在角落。",
            modelAsset = "sofa.glb",
            physicalSize = PhysicalSize(1.807f, 0.709f, 0.818f)
        ),
        ShopProduct(
            id = "chair-01",
            name = "复古雕花休闲椅",
            series = "LIVING ROOM / 单椅系列",
            price = 1899,
            originalPrice = 2399,
            category = ShopCategory.SOFA,
            description = "木质雕花与柔软座面，让阅读时光更从容。",
            modelAsset = "ArmChair_01.glb",
            physicalSize = PhysicalSize(0.848f, 1.065f, 0.766f)
        ),
        ShopProduct(
            id = "lamp-01",
            name = "灵动关节阅读灯",
            series = "LIGHTING / 氛围系列",
            price = 699,
            originalPrice = 899,
            category = ShopCategory.LIGHTING,
            description = "可调节的灯臂，将一束柔光留给生活的每一页。",
            modelAsset = "desk_lamp_arm_01.glb",
            physicalSize = PhysicalSize(0.202f, 0.893f, 0.614f),
            modelBaseY = -0.088f
        )
    )
}
