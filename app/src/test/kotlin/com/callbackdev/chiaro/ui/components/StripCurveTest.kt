package com.callbackdev.chiaro.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The temperature curve through the hour strip (23 set 2026): the slope is the world's,
 * not the day's, and the dot sits on the line the cells draw together. */
class StripCurveTest {

    @Test
    fun `an ordinary day keeps the fixed rate, so a degree is always the same height`() {
        // 24dp of travel, 2dp a degree: a 6° day fits at the rate, spanning 12 of 24.
        val curve = StripCurve.of(listOf(20.0, 17.0, 14.0), travelDp = 24f, dpPerDegree = 2f)!!
        assertEquals(0.25f, curve.level(0)!!, 1e-5f)
        assertEquals(0.5f, curve.level(1)!!, 1e-5f)
        assertEquals(0.75f, curve.level(2)!!, 1e-5f)
    }

    @Test
    fun `a day too wide for the band is compressed to fit it, ends on the ends`() {
        val curve = StripCurve.of(listOf(30.0, 10.0), travelDp = 24f, dpPerDegree = 2f)!!
        assertEquals(0f, curve.level(0)!!, 1e-5f)
        assertEquals(1f, curve.level(1)!!, 1e-5f)
    }

    @Test
    fun `fewer than two temperatures draw no curve`() {
        assertNull(StripCurve.of(listOf(20.0), 24f, 2f))
        assertNull(StripCurve.of(listOf(null, null), 24f, 2f))
    }

    @Test
    fun `the dot is where the smoothed line passes, and a flat run is flat`() {
        assertEquals(0.5f, curveDotY(0.5f, 0.5f, 0.5f), 1e-5f)
        // A peak is softened toward its neighbours: a quarter of the way.
        assertEquals(0.125f, curveDotY(0.5f, 0f, 0.5f), 1e-5f)
        // The ends of the strip have one neighbour.
        assertEquals(0.125f, curveDotY(null, 0f, 1f), 1e-5f)
    }
}
