package com.callbackdev.chiaro.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The one table of WMO codes (24 set 2026). Every set the app used to keep by hand is
 * checked against it, so moving a code in [WmoCode] is a decision a test has to agree
 * with, not a typo that five screens inherit.
 */
class WmoCodeTest {

    /** Open-Meteo's documented table, «WMO Weather interpretation codes (WW)». */
    private val served = listOf(
        0, 1, 2, 3, 45, 48, 51, 53, 55, 56, 57, 61, 63, 65, 66, 67,
        71, 73, 75, 77, 80, 81, 82, 85, 86, 95, 96, 99
    )

    /** What the state engine writes that no provider serves (25 set 2026): WMO's own 68
     * and 69, and the app's states above 1000. */
    private val engineOnly = listOf(68, 69, 1061, 1071, 1095)

    @Test
    fun `the table is the codes Open-Meteo serves, plus the engine's own`() {
        assertEquals((served + engineOnly).sorted(), WmoCode.entries.map { it.code }.sorted())
        served.forEach { assertEquals(it, WmoCode.of(it)!!.code) }
        // The app's states can never be mistaken for a WMO number.
        assertTrue(WmoCode.entries.filter { it.code > 99 }.map { it.code }.all { it > 1000 })
    }

    @Test
    fun `a number outside the table is nothing, and nothing wet`() {
        listOf(-1, 4, 42, 50, 58, 90, 100).forEach { code ->
            assertNull(WmoCode.of(code))
            assertFalse(WmoCode.isPrecipitation(code))
            assertFalse(WmoCode.isFog(code))
            assertEquals(ConditionWord.UNKNOWN, ConditionWord.of(code))
        }
    }

    @Test
    fun `precipitation, snow and fog are the sets the engines used to spell out`() {
        // HeadlineEngine's WET_CODES, SNOW_CODES and FOG_CODES as they stood.
        val wet = setOf(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 71, 73, 75, 77, 80, 81, 82, 85, 86, 95, 96, 99)
        assertEquals(wet, served.filter(WmoCode::isPrecipitation).toSet())
        assertEquals(setOf(71, 73, 75, 77, 85, 86), served.filter(WmoCode::isSnow).toSet())
        assertEquals(setOf(45, 48), served.filter(WmoCode::isFog).toSet())
        // …and the old `>= 51` boundary agrees on every served code.
        served.forEach { assertEquals(it >= 51, WmoCode.isPrecipitation(it)) }
    }

    @Test
    fun `the hazards are AlertEngine's and the mapper's, bucket for bucket`() {
        val expected = mapOf(
            95 to AlertEngine.SevereBucket.THUNDER, 96 to AlertEngine.SevereBucket.THUNDER,
            99 to AlertEngine.SevereBucket.THUNDER,
            56 to AlertEngine.SevereBucket.ICE, 57 to AlertEngine.SevereBucket.ICE,
            66 to AlertEngine.SevereBucket.ICE, 67 to AlertEngine.SevereBucket.ICE,
            65 to AlertEngine.SevereBucket.RAIN, 82 to AlertEngine.SevereBucket.RAIN,
            75 to AlertEngine.SevereBucket.SNOW, 86 to AlertEngine.SevereBucket.SNOW,
            // The engine's storm over a dry hour: a warning it may not remove (25 set 2026).
            1095 to AlertEngine.SevereBucket.THUNDER
        )
        assertEquals(expected, AlertEngine.SevereCodes)
    }

    /** The defect that opened this table: the WMO number is not an order of severity. */
    @Test
    fun `severity is not the WMO number`() {
        fun sev(code: Int) = WmoCode.of(code)!!.severity
        assertTrue("heavy snow over a slight shower", sev(75) > sev(80))
        assertTrue("heavy showers over slight snow showers", sev(82) > sev(85))
        assertTrue("heavy rain over light snow", sev(65) > sev(71))
        assertTrue("heavy rain over slight showers", sev(65) > sev(80))
        // Same intensity in two shapes weighs the same: the day's hours break the tie.
        assertEquals(sev(61), sev(80))
        assertEquals(sev(63), sev(81))
        assertEquals(sev(65), sev(82))
        assertEquals(sev(75), sev(86))
    }

    @Test
    fun `every hazard outweighs every other precipitation, and precipitation every sky`() {
        val hazards = WmoCode.entries.filter { it.hazard != null }
        val plainWet = WmoCode.entries.filter { it.isPrecipitation && it.hazard == null }
        // A possible storm is a hazard and not precipitation: it belongs with the hazards.
        val skies = WmoCode.entries.filter { !it.isPrecipitation && it.hazard == null }
        assertTrue(hazards.minOf { it.severity } > plainWet.maxOf { it.severity })
        assertTrue(plainWet.minOf { it.severity } > skies.maxOf { it.severity })
        // Fog outweighs the cloudiest sky, as the day's tie-break always had it.
        assertTrue(WmoCode.FOG.severity > WmoCode.OVERCAST.severity)
        // The thunderstorms top the ladder.
        assertEquals(WmoCode.THUNDERSTORM_SEVERE, WmoCode.entries.maxBy { it.severity })
    }

    @Test
    fun `phases keep snow, ice and rain apart`() {
        assertEquals(WmoCode.Phase.FREEZING, WmoCode.of(56)!!.phase)
        assertEquals(WmoCode.Phase.FREEZING, WmoCode.of(67)!!.phase)
        assertEquals(WmoCode.Phase.FROZEN, WmoCode.of(77)!!.phase)
        assertEquals(WmoCode.Phase.FROZEN, WmoCode.of(86)!!.phase)
        assertEquals(WmoCode.Phase.LIQUID, WmoCode.of(51)!!.phase)
        assertEquals(WmoCode.Phase.LIQUID, WmoCode.of(95)!!.phase)
        assertEquals(WmoCode.Phase.NONE, WmoCode.of(45)!!.phase)
        assertEquals(WmoCode.Phase.MIXED, WmoCode.of(68)!!.phase)
        assertEquals(WmoCode.Phase.MIXED, WmoCode.of(69)!!.phase)
    }

    @Test
    fun `codes share a word exactly where the old tables grouped them`() {
        val groups = WmoCode.entries.groupBy({ it.word }, { it.code }).values.map { it.toSet() }.toSet()
        val expected = setOf(
            setOf(0), setOf(1), setOf(2), setOf(3), setOf(45), setOf(48), setOf(51, 53, 55),
            setOf(56, 57), setOf(61), setOf(63), setOf(65), setOf(66, 67), setOf(71),
            setOf(73), setOf(75), setOf(77), setOf(80, 81), setOf(82), setOf(85, 86),
            setOf(95), setOf(96, 99), setOf(68, 69), setOf(1061), setOf(1071), setOf(1095)
        )
        assertEquals(expected, groups)
    }

    /** The ids are on disk in every history commit since 24 set 2026: frozen. */
    @Test
    fun `word ids are stable and round-trip`() {
        assertEquals(
            listOf(
                "clear", "mostly_clear", "partly_cloudy", "overcast", "fog", "drizzle",
                "freezing_drizzle", "rain_light", "rain", "rain_heavy", "freezing_rain",
                "snow_light", "snow", "snow_heavy", "snow_grains", "showers", "showers_heavy",
                "snow_showers", "thunderstorm", "thunderstorm_strong", "unknown",
                "freezing_fog", "rain_and_snow", "rain_likely", "snow_likely", "thunderstorm_possible"
            ),
            ConditionWord.entries.map { it.id }
        )
        ConditionWord.entries.forEach { assertEquals(it, ConditionWord.fromId(it.id)) }
        assertNull(ConditionWord.fromId("Partly Cloudy ⛅"))
    }

    @Test
    fun `a word weighs as its heaviest code`() {
        assertEquals(WmoCode.THUNDERSTORM_SEVERE.severity, ConditionWord.THUNDERSTORM_STRONG.severity)
        assertEquals(WmoCode.DRIZZLE_DENSE.severity, ConditionWord.DRIZZLE.severity)
        assertTrue(ConditionWord.PARTLY_CLOUDY.severity > ConditionWord.CLEAR.severity)
        assertTrue(ConditionWord.UNKNOWN.severity < ConditionWord.CLEAR.severity)
    }

    /** The gate the day's label now shares with the banner (P5, 24 set 2026). */
    @Test
    fun `an improbable storm or downpour is not severe, ice and snow always are`() {
        assertNull(AlertEngine.severeBucket(95, 10))
        assertNull(AlertEngine.severeBucket(65, 19))
        assertEquals(AlertEngine.SevereBucket.THUNDER, AlertEngine.severeBucket(95, 20))
        // A missing chance is not evidence that the storm will not come.
        assertEquals(AlertEngine.SevereBucket.THUNDER, AlertEngine.severeBucket(96, null))
        assertEquals(AlertEngine.SevereBucket.ICE, AlertEngine.severeBucket(56, 5))
        assertEquals(AlertEngine.SevereBucket.SNOW, AlertEngine.severeBucket(75, 5))
        assertNull(AlertEngine.severeBucket(63, 90))
    }
}
