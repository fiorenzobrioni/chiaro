package com.callbackdev.chiaro.ui.today

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatChangedBuilderTest {

    private val zone = ZoneId.of("Europe/Rome")
    private val saturday = "2026-09-05"

    private fun at(day: Int, hour: Int): Instant =
        LocalDate.of(2026, 9, day).atTime(hour, 0).atZone(zone).toInstant()

    private fun forecast(precip: Int, high: Double, low: Double = 14.0, status: String = "rain") = mapOf(
        "$saturday.status" to status,
        "$saturday.high_c" to high.toString(),
        "$saturday.low_c" to low.toString(),
        "$saturday.precip_pct" to precip.toString()
    )

    private fun commit(at: Instant, forecast: Map<String, String>) = ForecastCommit(at, forecast)

    @Test
    fun `a rain drop reads as Saturday improved, with the numbers`() {
        val shift = WhatChangedBuilder.latestShifts(
            listOf(
                commit(at(3, 7), forecast(precip = 30, high = 27.0)),
                commit(at(2, 7), forecast(precip = 70, high = 24.0))
            )
        ).single()

        assertEquals(LocalDate.parse(saturday), shift.date)
        assertEquals(true, shift.better)
        val rain = shift.shifts.first { it.field == "precip_pct" }
        assertEquals("70", rain.old)
        assertEquals("30", rain.new)
    }

    /** A day that turned from clear to storm with the same chance of rain got worse:
     * the sky decides when rain does not (24 set 2026). */
    @Test
    fun `a sky that turned stormy at the same rain reads as worse`() {
        val shift = WhatChangedBuilder.latestShifts(
            listOf(
                commit(at(3, 7), forecast(precip = 40, high = 24.0, status = "thunderstorm")),
                commit(at(2, 7), forecast(precip = 40, high = 24.0, status = "clear"))
            )
        ).single()

        assertEquals(false, shift.better)
        val sky = shift.shifts.single()
        assertEquals("status", sky.field)
        assertEquals("clear", sky.old)
        assertEquals("thunderstorm", sky.new)
    }

    @Test
    fun `rain still decides over the sky when both moved`() {
        val shift = WhatChangedBuilder.latestShifts(
            listOf(
                commit(at(3, 7), forecast(precip = 20, high = 24.0, status = "overcast")),
                commit(at(2, 7), forecast(precip = 60, high = 24.0, status = "clear"))
            )
        ).single()

        assertEquals(true, shift.better)
    }

    @Test
    fun `a high that moved alone stays neutral`() {
        val shift = WhatChangedBuilder.latestShifts(
            listOf(
                commit(at(3, 7), forecast(precip = 40, high = 27.0)),
                commit(at(2, 7), forecast(precip = 40, high = 24.0))
            )
        ).single()

        assertNull(shift.better)
        assertEquals(listOf("high_c"), shift.shifts.map { it.field })
    }

    /** The first fetch after the update rewrote every status id; the forecast did not
     * change, and the screen must not say it did. */
    @Test
    fun `an old English label and its new id are the same forecast`() {
        assertTrue(
            WhatChangedBuilder.latestShifts(
                listOf(
                    commit(at(3, 7), forecast(precip = 40, high = 24.0, status = "partly_cloudy")),
                    commit(at(2, 7), forecast(precip = 40, high = 24.0, status = "Partly Cloudy ⛅"))
                )
            ).isEmpty()
        )
    }

    /** What changed is what the NEWEST fetch changed, against the last shown value. */
    @Test
    fun `only the newest fetch speaks, against its own baseline`() {
        val latest = WhatChangedBuilder.latestShifts(
            listOf(
                commit(at(3, 9), forecast(precip = 30, high = 27.0)),
                commit(at(3, 8), forecast(precip = 60, high = 25.0)),
                commit(at(3, 7), forecast(precip = 70, high = 24.0))
            )
        ).single()

        assertEquals(at(3, 9), latest.at)
        assertEquals("60", latest.shifts.first { it.field == "precip_pct" }.old)
        assertEquals("30", latest.shifts.first { it.field == "precip_pct" }.new)
    }

    @Test
    fun `a newest fetch that moved nothing leaves the section empty`() {
        assertTrue(
            WhatChangedBuilder.latestShifts(
                listOf(
                    commit(at(3, 10), forecast(precip = 30, high = 27.0)),
                    commit(at(3, 9), forecast(precip = 30, high = 27.0)),
                    commit(at(3, 8), forecast(precip = 70, high = 24.0))
                )
            ).isEmpty()
        )
    }

    @Test
    fun `a day entering the horizon is not a revision worth a sentence`() {
        assertTrue(
            WhatChangedBuilder.latestShifts(
                listOf(commit(at(2, 7), forecast(precip = 70, high = 24.0)))
            ).isEmpty()
        )
    }

    @Test
    fun `at most three lines, the biggest rain move first`() {
        val days = (5..9).map { "2026-09-0$it" }
        fun week(precip: (Int) -> Int) = days.flatMapIndexed { i, d ->
            listOf("$d.precip_pct" to precip(i).toString(), "$d.high_c" to "20.0")
        }.toMap()
        val shifts = WhatChangedBuilder.latestShifts(
            listOf(
                commit(at(3, 8), week { 80 - it * 10 }),
                commit(at(3, 7), week { 90 })
            )
        )

        assertEquals(3, shifts.size)
        assertEquals(LocalDate.parse("2026-09-09"), shifts.first().date)
    }
}
