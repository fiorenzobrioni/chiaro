package com.callbackdev.chiaro.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DESIGN §4 since the review of 23 set 2026: the ribbon is one gradient, and a gradient's
 * stops are arithmetic. What must hold is that the stops never go backwards (the platform
 * would draw garbage), that the long phases keep a solid core (night stays night), and
 * that the compact rows' fade leaves the lit part of the day alone.
 */
class DaylightRibbonTest {

    private val day = listOf(
        LightPhase(0f, 0.20f, -30.0),
        LightPhase(0.20f, 0.24f, -12.0),
        LightPhase(0.24f, 0.27f, -6.0),
        LightPhase(0.27f, 0.31f, 2.0),
        LightPhase(0.31f, 0.76f, 45.0),
        LightPhase(0.76f, 0.80f, 2.0),
        LightPhase(0.80f, 0.84f, -6.0),
        LightPhase(0.84f, 1f, -30.0)
    )

    @Test
    fun `the stops never go backwards`() {
        val flat = ribbonStops(day).flatMap { listOf(it.first, it.second) }
        flat.zipWithNext().forEach { (a, b) -> assertTrue("$a then $b", b >= a) }
        assertEquals(0f, flat.first(), 0f)
        assertEquals(1f, flat.last(), 0f)
    }

    @Test
    fun `a long phase keeps its core and a short one blends over most of itself`() {
        val stops = ribbonStops(day)
        // Daylight, 0.31..0.76: the blend is capped, so the core is nearly all of it.
        val (a, b) = stops[4]
        assertEquals(0.325f, a, 1e-5f)
        assertEquals(0.745f, b, 1e-5f)
        // Civil twilight, 0.24..0.27: 30% of its width on each side.
        val (c, d) = stops[2]
        assertEquals(0.249f, c, 1e-5f)
        assertEquals(0.261f, d, 1e-5f)
    }

    @Test
    fun `overlapping or unsorted input still yields monotonic stops`() {
        val odd = listOf(LightPhase(0f, 0.5f, -30.0), LightPhase(0.4f, 1f, 45.0))
        val flat = ribbonStops(odd).flatMap { listOf(it.first, it.second) }
        flat.zipWithNext().forEach { (a, b) -> assertTrue("$a then $b", b >= a) }
    }

    @Test
    fun `the week's fade leaves the lit day alone and takes most of the night`() {
        assertEquals(0f, nightFadeAmount(45.0), 0f)
        assertEquals(0f, nightFadeAmount(-6.0), 0f)
        assertEquals(0.375f, nightFadeAmount(-12.0), 1e-5f)
        assertEquals(0.75f, nightFadeAmount(-30.0), 1e-5f)
    }
}
