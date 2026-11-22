package com.pico.welcomespace.domain.usecase

import com.pico.welcomespace.data.repository.FakeShopRepository
import com.pico.welcomespace.domain.model.FloorPlanRoom
import com.pico.welcomespace.domain.model.PhysicalSize
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class NormalizeFurnitureSizeUseCaseTest {
    private val normalize = NormalizeFurnitureSizeUseCase()

    @Test fun explicitMillimetersCentimetersAndMetersAgree() {
        val meter = normalize.normalize(PhysicalSize(2f, 1f, .5f), null)
        val cm = normalize.normalize(PhysicalSize(200f, 100f, 50f), null, .01f)
        val mm = normalize.normalize(PhysicalSize(2000f, 1000f, 500f), null, .001f)
        assertEquals(meter.size, cm.size)
        assertEquals(meter.size, mm.size)
        assertEquals(1f, mm.calibrationScale, .0001f)
    }

    @Test fun calibrationPreservesAspectRatioAndRecordsUnitConversionSeparately() {
        val result = normalize.normalize(PhysicalSize(100f, 50f, 40f), PhysicalSize(2f, 1f, .8f), .01f)
        assertEquals(.01f, result.unitScale, .0001f)
        assertEquals(2f, result.calibrationScale, .0001f)
        assertEquals(2f, result.size.width, .00001f)
        assertEquals(1f, result.size.height, .00001f)
        assertEquals(.8f, result.size.depth, .00001f)
    }

    @Test fun repeatedFreshLoadsProduceSameScale() {
        val raw = PhysicalSize(1f, .5f, .4f)
        val target = PhysicalSize(2f, 1f, .8f)
        assertEquals(normalize.normalize(raw, target), normalize.normalize(raw, target))
    }

    @Test(expected = IllegalArgumentException::class)
    fun incompatibleAspectRatioIsRejected() {
        normalize.normalize(PhysicalSize(1f, 1f, 1f), PhysicalSize(2f, .1f, 1f))
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidBoundsNeverSilentlyBecomeScaleOne() {
        normalize.normalize(PhysicalSize(Float.NaN, 1f, 1f), null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun unknownUnitCannotBePassedAsAGuessedConversion() {
        normalize.normalize(PhysicalSize(1f, 1f, 1f), null, Float.NaN)
    }

    @Test fun allSixProductsStayWithinWallsAtEveryTestedYaw() {
        for (product in FakeShopRepository().products()) {
            for (degrees in 0..350 step 10) {
                val angle = Math.toRadians(degrees.toDouble()).toFloat()
                val placement = FloorPlanRoom.choosePlacement(product, 100f, -100f,
                    yawRadians = angle)!!
                val size = product.physicalSize
                val halfX = (abs(cos(angle)) * size.width + abs(sin(angle)) * size.depth) / 2
                val halfZ = (abs(sin(angle)) * size.width + abs(cos(angle)) * size.depth) / 2
                assertTrue(placement.x + halfX <= 2f)
                assertTrue(placement.z - halfZ >= -2.5f)
                assertEquals(0f, placement.y, .0001f)
            }
        }
    }

    @Test fun roomRejectsTooTallFurnitureAndInvalidPointer() {
        val sofa = FakeShopRepository().products().first()
        assertNull(FloorPlanRoom.choosePlacement(sofa, Float.NaN, 0f))
        assertNull(FloorPlanRoom.choosePlacement(sofa, 0f, 0f,
            size = PhysicalSize(1f, 3f, 1f)))
    }
}
