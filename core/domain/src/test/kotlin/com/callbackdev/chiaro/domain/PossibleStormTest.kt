package com.callbackdev.chiaro.domain

import com.callbackdev.chiaro.domain.PossibleStorm.Hour
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The day's possible storm (Fase 17.4, 19 nov 2026): an hour with CAPE ≥ 500 J/kg over a
 * measurable, probable amount, said by the part of the day it starts in, only where no hour of
 * the day is a storm already. The rule as `tools/analyze_against_metar.py --hazards` scored it.
 */
class PossibleStormTest {

    private val day = LocalDateTime.parse("2026-07-14T00:00")
    private val cloudy = WmoCode.OVERCAST.code
    private val rain = WmoCode.RAIN_SLIGHT.code

    /** 24 dry hours of [day] in calm air, overcast, with [edit] applied to the hours it names. */
    private fun hours(vararg edit: Pair<Int, Hour.() -> Hour>): List<Hour> {
        val edits = edit.toMap()
        return List(24) { h ->
            val hour = Hour(day.plusHours(h.toLong()), cloudy, capeJkg = 50.0, precipitationMm = 0.0, chancePct = 10)
            edits[h]?.invoke(hour) ?: hour
        }
    }

    private val shower: Hour.() -> Hour = { copy(state = rain, capeJkg = 900.0, precipitationMm = 1.2, chancePct = 40) }

    @Test
    fun `an hour of rain in unstable air is a possible storm in its part of the day`() {
        assertEquals(listOf(DayPart.AFTERNOON), PossibleStorm.parts(hours(15 to shower)))
    }

    @Test
    fun `each part is named once, in the order of the day`() {
        assertEquals(
            listOf(DayPart.NIGHT, DayPart.AFTERNOON, DayPart.EVENING),
            PossibleStorm.parts(hours(20 to shower, 3 to shower, 14 to shower, 16 to shower))
        )
    }

    @Test
    fun `an hour belongs to the part it starts in`() {
        assertEquals(listOf(DayPart.MORNING), PossibleStorm.parts(hours(11 to shower)))
        assertEquals(listOf(DayPart.AFTERNOON), PossibleStorm.parts(hours(12 to shower)))
        assertEquals(listOf(DayPart.EVENING), PossibleStorm.parts(hours(18 to shower)))
        assertEquals(listOf(DayPart.NIGHT), PossibleStorm.parts(hours(5 to shower)))
    }

    @Test
    fun `the energy starts at 500 J per kg`() {
        assertEquals(listOf(DayPart.AFTERNOON), PossibleStorm.parts(hours(15 to { shower().copy(capeJkg = 500.0) })))
        assertEquals(emptyList<DayPart>(), PossibleStorm.parts(hours(15 to { shower().copy(capeJkg = 499.0) })))
    }

    @Test
    fun `unstable air with nothing measurable is not a storm`() {
        assertEquals(emptyList<DayPart>(), PossibleStorm.parts(hours(15 to { shower().copy(precipitationMm = 0.0) })))
        assertEquals(emptyList<DayPart>(), PossibleStorm.parts(hours(15 to { shower().copy(precipitationMm = null) })))
        // 0.1 mm is measurable, give or take the arithmetic of the provider's rounding.
        assertEquals(listOf(DayPart.AFTERNOON), PossibleStorm.parts(hours(15 to { shower().copy(precipitationMm = 0.3 - 0.2) })))
    }

    @Test
    fun `an amount the ensemble does not back is not either, and no chance at all is no veto`() {
        assertEquals(emptyList<DayPart>(), PossibleStorm.parts(hours(15 to { shower().copy(chancePct = 19) })))
        assertEquals(listOf(DayPart.AFTERNOON), PossibleStorm.parts(hours(15 to { shower().copy(chancePct = 20) })))
        assertEquals(listOf(DayPart.AFTERNOON), PossibleStorm.parts(hours(15 to { shower().copy(chancePct = null) })))
    }

    @Test
    fun `where an hour of the day has the storm code the code speaks`() {
        val stormy = hours(15 to shower, 21 to { copy(state = WmoCode.THUNDERSTORM.code) })
        assertEquals(emptyList<DayPart>(), PossibleStorm.parts(stormy))
        // «Temporali possibili» over a dry hour is the code too.
        val dryStorm = hours(15 to shower, 21 to { copy(state = WmoCode.THUNDERSTORM_POSSIBLE.code) })
        assertEquals(emptyList<DayPart>(), PossibleStorm.parts(dryStorm))
    }

    @Test
    fun `and so does a day whose own label is a storm`() {
        assertEquals(emptyList<DayPart>(), PossibleStorm.parts(hours(15 to shower), label = WmoCode.THUNDERSTORM_STRONG.code))
        assertEquals(listOf(DayPart.AFTERNOON), PossibleStorm.parts(hours(15 to shower), label = WmoCode.RAIN_MODERATE.code))
    }

    @Test
    fun `other hazards do not silence it`() {
        val icy = hours(15 to shower, 6 to { copy(state = WmoCode.FREEZING_RAIN_LIGHT.code) })
        assertEquals(listOf(DayPart.AFTERNOON), PossibleStorm.parts(icy, label = WmoCode.FREEZING_RAIN_LIGHT.code))
    }

    @Test
    fun `with no CAPE anywhere the rule cannot be read, and says nothing rather than no`() {
        val old = hours(15 to shower).map { it.copy(capeJkg = null) }
        assertNull(PossibleStorm.parts(old))
        assertNull(PossibleStorm.parts(emptyList()))
        // ...even over a storm the code names: null is «not read», not «nothing possible».
        assertNull(PossibleStorm.parts(old, label = WmoCode.THUNDERSTORM.code))
    }

    @Test
    fun `hours without CAPE do not vote, the others still do`() {
        val partial = hours(15 to shower, 9 to { shower().copy(capeJkg = null) })
        assertEquals(listOf(DayPart.AFTERNOON), PossibleStorm.parts(partial))
    }

    @Test
    fun `the parts of the day are cut where the open day's sentence cuts them`() {
        fun at(hour: Int) = DayPart.of(day.plusHours(hour.toLong()))
        assertEquals(listOf(DayPart.NIGHT, DayPart.MORNING, DayPart.AFTERNOON, DayPart.EVENING), listOf(0, 6, 12, 18).map(::at))
        assertEquals(listOf(DayPart.NIGHT, DayPart.MORNING, DayPart.AFTERNOON, DayPart.EVENING), listOf(5, 11, 17, 23).map(::at))
    }
}
