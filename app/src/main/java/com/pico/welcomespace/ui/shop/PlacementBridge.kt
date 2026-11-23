package com.pico.welcomespace.ui.shop

/**
 * 拖拽桥接状态机（纯 JVM 可测）：商品卡 3D 预览与 FurnitureRoom Stage 之间的放置会话。
 *
 * IDLE --begin(productId)--> FOLLOWING --end()--> IDLE
 *
 * FOLLOWING 期间：房间 Stage 的预览实体每帧跟随放置射线命中点，选家具面板切弱档玻璃。
 * end() 同时表示"松手落位"与"取消回收"两种出口，具体动作（drop/cancel）由调用方触发。
 */
internal class PlacementBridge {
    enum class Phase { IDLE, FOLLOWING }

    var phase: Phase = Phase.IDLE
        private set
    var productId: String? = null
        private set

    val isFollowing: Boolean get() = phase == Phase.FOLLOWING

    /** 起手：仅 IDLE 可进入跟随；已有会话未结束时拒绝新的起手。 */
    fun begin(productId: String): Boolean {
        if (phase == Phase.FOLLOWING) return false
        phase = Phase.FOLLOWING
        this.productId = productId
        return true
    }

    /** 结束当前会话（落位或取消后调用），回到 IDLE；IDLE 下幂等。 */
    fun end() {
        phase = Phase.IDLE
        productId = null
    }
}
