package com.pico.welcomespace.ui.shop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 拖拽桥接状态机流转：起手 → 跟随 → 落位/取消，均为纯 JVM 路径。 */
class PlacementBridgeTest {
    @Test
    fun idleBridgeAcceptsBeginAndTracksProduct() {
        val bridge = PlacementBridge()
        assertEquals(PlacementBridge.Phase.IDLE, bridge.phase)
        assertNull(bridge.productId)

        assertTrue(bridge.begin("sofa-02"))

        assertEquals(PlacementBridge.Phase.FOLLOWING, bridge.phase)
        assertEquals("sofa-02", bridge.productId)
        assertTrue(bridge.isFollowing)
    }

    @Test
    fun beginRejectedWhileAnotherSessionIsFollowing() {
        val bridge = PlacementBridge()
        assertTrue(bridge.begin("sofa-02"))

        assertFalse(bridge.begin("chair-01"))

        assertEquals(PlacementBridge.Phase.FOLLOWING, bridge.phase)
        assertEquals("sofa-02", bridge.productId)
    }

    @Test
    fun releasePathReturnsToIdleAndClearsProduct() {
        val bridge = PlacementBridge()
        bridge.begin("sofa-02")

        bridge.end()

        assertEquals(PlacementBridge.Phase.IDLE, bridge.phase)
        assertNull(bridge.productId)
        assertFalse(bridge.isFollowing)
    }

    @Test
    fun cancelPathReturnsToIdleAndCanStartAgain() {
        val bridge = PlacementBridge()
        bridge.begin("lamp-01")
        bridge.end()

        assertTrue(bridge.begin("lamp-01"))
        assertEquals("lamp-01", bridge.productId)
    }

    @Test
    fun endIsIdempotentWhenIdle() {
        val bridge = PlacementBridge()
        bridge.end()
        assertEquals(PlacementBridge.Phase.IDLE, bridge.phase)
        assertNull(bridge.productId)
    }
}
