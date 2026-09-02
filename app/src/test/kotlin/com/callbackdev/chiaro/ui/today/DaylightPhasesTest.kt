package com.callbackdev.chiaro.ui.today

import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.sky.AstronomyEngine
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The ribbon is built from real [AstronomyEngine] days — Milan for the ordinary case,
 * Longyearbyen for the two polar ones — because the edge the mapper has to survive is
 * a SolarDay with holes in it, and the engine is where honest holes come from.
 */
class DaylightPhasesTest {

    private val milan = Coordinates(45.4643, 9.1895)
    private val milanZone = ZoneId.of("Europe/Rome")
    private val svalbard = Coordinates(78.2232, 15.6267)
    private val svalbardZone = ZoneId.of("Arctic/Longyearbyen")

    private fun assertCoversTheDay(phases: List<com.callbackdev.chiaro.ui.components.LightPhase>) {
        assertTrue("no phases", phases.isNotEmpty())
        assertEquals(0f, phases.first().start, 1e-6f)
        assertEquals(1f, phases.last().end, 1e-6f)
        phases.zipWithNext().forEach { (a, b) ->
            assertEquals("gap between ${a.end} and ${b.start}", a.end, b.start, 1e-6f)
        }
    }

    @Test
    fun `an ordinary day has the full ladder of light`() {
        val day = AstronomyEngine.solarDay(LocalDate.of(2026, 3, 20), milanZone, milan)
        val phases = DaylightPhases.phases(day, milanZone)
        assertCoversTheDay(phases)
        // night → astronomical → nautical → civil → golden → day, and back down
        assertEquals(11, phases.size)
        assertEquals(-30.0, phases.first().sunAltitudeDeg, 0.0)
        assertEquals(-30.0, phases.last().sunAltitudeDeg, 0.0)
        assertTrue("no full-day segment", phases.any { it.sunAltitudeDeg == 30.0 })
    }

    @Test
    fun `midsummer above the arctic circle is one unbroken day`() {
        val day = AstronomyEngine.solarDay(LocalDate.of(2026, 6, 21), svalbardZone, svalbard)
        val phases = DaylightPhases.phases(day, svalbardZone)
        assertEquals(listOf(com.callbackdev.chiaro.ui.components.LightPhase(0f, 1f, 30.0)), phases)
    }

    @Test
    fun `midwinter above the arctic circle never reaches daylight`() {
        val day = AstronomyEngine.solarDay(LocalDate.of(2026, 12, 21), svalbardZone, svalbard)
        val phases = DaylightPhases.phases(day, svalbardZone)
        assertCoversTheDay(phases)
        assertTrue(
            "polar night must not contain daylight or golden light",
            phases.none { it.sunAltitudeDeg >= 2.0 }
        )
    }

    @Test
    fun `a milanese june night without astronomical darkness starts in twilight`() {
        // In late June at 45° the sun never reaches −18°: the day has no astronomical
        // dawn, and the ribbon's first segment must be twilight, not black.
        val day = AstronomyEngine.solarDay(LocalDate.of(2026, 6, 21), milanZone, milan)
        val phases = DaylightPhases.phases(day, milanZone)
        assertCoversTheDay(phases)
        if (day.astronomicalDawn == null) {
            assertEquals(-15.0, phases.first().sunAltitudeDeg, 0.0)
        }
    }
}
