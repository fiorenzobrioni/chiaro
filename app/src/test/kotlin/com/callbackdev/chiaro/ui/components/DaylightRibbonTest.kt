package com.callbackdev.chiaro.ui.components

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    // --- The forecast sky (DESIGN §4, 12 ott 2026) ---------------------------------------

    /** A sky that writes its inputs into the color, so a stop says what it was mixed from. */
    private val probe: (Double, Int, Int) -> Color = { altitude, cloud, chance ->
        Color(red = cloud / 100f, green = chance / 100f, blue = if (altitude > 0) 1f else 0f)
    }

    private fun hour(at: Float, cloud: Int, chance: Int = 0, wet: Boolean = false) =
        RibbonHour(at, cloud, chance, wet)

    @Test
    fun `with no hours the stops are the light's own pairs`() {
        val stops = ribbonColorStops(day, emptyList(), nightFade = null, sky = probe)
        assertEquals(ribbonStops(day).flatMap { listOf(it.first, it.second) }, stops.map { it.first })
        stops.forEach { (_, color) -> assertEquals(0f, color.red, 0f) }
    }

    @Test
    fun `with hours the stops still never go backwards and keep the cores' edges`() {
        val hours = (0..23).map { hour(it / 24f, cloud = it * 4) }
        val stops = ribbonColorStops(day, hours, nightFade = null, sky = probe)
        stops.map { it.first }.zipWithNext().forEach { (a, b) -> assertTrue("$a then $b", b >= a) }
        val positions = stops.map { it.first }.toSet()
        ribbonStops(day).forEach { (a, b) -> assertTrue(a in positions && b in positions) }
        assertEquals(0f, stops.first().first, 0f)
        assertEquals(1f, stops.last().first, 0f)
    }

    @Test
    fun `a stop in daylight wears the cloud of its moment`() {
        // Clear until noon, overcast from 13:00: the afternoon is grey, the morning is not.
        val hours = listOf(hour(0f, 0), hour(12 / 24f, 0), hour(13 / 24f, 100), hour(1f, 100))
        val stops = ribbonColorStops(day, hours, nightFade = null, sky = probe)
        fun at(t: Float) = stops.minByOrNull { kotlin.math.abs(it.first - t) }!!.second
        assertEquals(0f, at(0.45f).red, 1e-3f)
        assertEquals(1f, at(0.60f).red, 1e-3f)
        assertEquals(1f, at(0.60f).blue, 1e-3f) // still the day's light under the cloud
    }

    @Test
    fun `before the forecast's first hour and before skyFrom the ribbon is light alone`() {
        val hours = listOf(hour(0.5f, 100), hour(1f, 100))
        val stops = ribbonColorStops(day, hours, nightFade = null, sky = probe)
        stops.filter { it.first < 0.5f }.forEach { (_, c) -> assertEquals(0f, c.red, 0f) }
        val fromNow = ribbonColorStops(day, listOf(hour(0f, 100), hour(1f, 100)), null, skyFrom = 0.6f, sky = probe)
        fromNow.filter { it.first < 0.6f }.forEach { (_, c) -> assertEquals(0f, c.red, 0f) }
        fromNow.filter { it.first in 0.6f..0.74f }.forEach { (_, c) -> assertEquals(1f, c.red, 1e-3f) }
    }

    @Test
    fun `the sky between two hours is a straight line, and ends an hour after the last`() {
        val hours = listOf(hour(0.5f, 0, 0), hour(0.5f + 1 / 24f, 100, 60))
        assertNull(skyAt(hours, 0.49f))
        assertEquals(50 to 30, skyAt(hours, 0.5f + 1 / 48f))
        assertEquals(100 to 60, skyAt(hours, 0.5f + 1.5f / 24f))
        assertNull(skyAt(hours, 0.5f + 2.5f / 24f))
        assertNull(skyAt(emptyList(), 0.5f))
    }

    @Test
    fun `two hours at the same moment do not divide by zero`() {
        // The night a zone falls back holds 02:00 twice.
        val hours = listOf(hour(2 / 24f, 10), hour(2 / 24f, 90), hour(3 / 24f, 50))
        assertEquals(true, skyAt(hours, 2.5f / 24f) != null)
    }

    @Test
    fun `a wet mark covers its hour around the label and stays inside the day`() {
        val (a, b) = wetMarkSpan(15 / 24f)
        assertEquals(14.5f / 24f, a, 1e-6f)
        assertEquals(15.5f / 24f, b, 1e-6f)
        assertEquals(0f to 0.5f / 24f, wetMarkSpan(0f))
    }

    @Test
    fun `in a row the spent part of today fades toward the track, on the canvas it does not`() {
        val track = Color(0.5f, 0.5f, 0.5f)
        val hours = listOf(hour(0.5f, 0), hour(1f, 0))
        val row = ribbonColorStops(day, hours, nightFade = track, skyFrom = 0.55f, sky = probe)
        val noonSpent = row.first { it.first in 0.4f..0.5f }.second
        val noonAhead = row.first { it.first in 0.6f..0.7f }.second
        // The spent daylight leans as far toward the track as the night does.
        assertEquals(androidx.compose.ui.graphics.lerp(Color(0f, 0f, 1f), track, 0.75f), noonSpent)
        assertEquals(Color(0f, 0f, 1f), noonAhead)
        val canvas = ribbonColorStops(day, hours, nightFade = null, skyFrom = 0.55f, sky = probe)
        assertEquals(Color(0f, 0f, 1f), canvas.first { it.first in 0.4f..0.5f }.second)
    }
}
