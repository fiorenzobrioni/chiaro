package com.callbackdev.chiaro.domain.sky

import com.callbackdev.chiaro.domain.model.Coordinates
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Where a rainbow stands and how high (15 ott 2026): the antisolar bearing, and the bow's
 * 42° radius less the sun's altitude. Measured against the engine's own sun, so what is
 * asserted is the geometry, not the ephemeris again.
 */
class RainbowSightTest {

    private val milan = Coordinates(45.4642, 9.1900)

    @Test
    fun `the bow stands opposite the sun`() {
        val at = Instant.parse("2026-10-15T15:00:00Z") // 17:00 in Milan, the sun low in the south-west
        val sight = RainbowWindow.sight(at, milan)
        assertNotNull(sight)
        val sun = AstronomyEngine.sunAzimuth(at, milan)
        assertEquals((sun + 180.0) % 360.0, sight!!.lookTowardsDeg, 1e-9)
        // An October afternoon sun is in the south-west: the bow is in the north-east.
        assertTrue(sight.lookTowardsDeg in 0.0..90.0)
    }

    @Test
    fun `a low sun raises a tall bow and a high one a flat one`() {
        val evening = RainbowWindow.sight(Instant.parse("2026-10-15T15:45:00Z"), milan)!!
        val afternoon = RainbowWindow.sight(Instant.parse("2026-10-15T13:30:00Z"), milan)!!
        val sunEvening = AstronomyEngine.sunAltitude(Instant.parse("2026-10-15T15:45:00Z"), milan)
        assertEquals(RainbowWindow.MAX_SUN_ALTITUDE - sunEvening, evening.topAltitudeDeg, 1e-9)
        assertTrue(evening.topAltitudeDeg > afternoon.topAltitudeDeg)
    }

    @Test
    fun `no bow with the sun down or above the bow's radius`() {
        // Night in Milan.
        assertNull(RainbowWindow.sight(Instant.parse("2026-10-15T21:00:00Z"), milan))
        // Midsummer noon: the sun near 68°, the whole bow under the horizon.
        assertNull(RainbowWindow.sight(Instant.parse("2026-06-21T11:30:00Z"), milan))
    }
}
