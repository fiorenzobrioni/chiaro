package com.callbackdev.chiaro.domain.warnings

import com.callbackdev.chiaro.domain.warnings.WarningHazard.HYDRAULIC
import com.callbackdev.chiaro.domain.warnings.WarningHazard.HYDROGEOLOGICAL
import com.callbackdev.chiaro.domain.warnings.WarningHazard.THUNDERSTORM
import com.callbackdev.chiaro.domain.warnings.WarningLevel.NONE
import com.callbackdev.chiaro.domain.warnings.WarningLevel.ORANGE
import com.callbackdev.chiaro.domain.warnings.WarningLevel.YELLOW
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class WarningDiffTest {

    private val sept8: LocalDate = LocalDate.of(2026, 9, 8)
    private val sept9: LocalDate = sept8.plusDays(1)
    private val sept10: LocalDate = sept8.plusDays(2)
    private val milano = WarningZone("Lomb-09", "Nodo Idraulico di Milano", "Lombardia")
    private val trieste = WarningZone("Friu-D", "Bacino di Levante / Carso", "Friuli-Venezia Giulia")

    private fun levels(
        hydraulic: WarningLevel = NONE,
        hydrogeological: WarningLevel = NONE,
        thunderstorm: WarningLevel = NONE
    ) = mapOf(HYDRAULIC to hydraulic, HYDROGEOLOGICAL to hydrogeological, THUNDERSTORM to thunderstorm)

    private fun place(zone: WarningZone, vararg days: Pair<LocalDate, Map<WarningHazard, WarningLevel>>) =
        PlaceWarnings(
            zone = zone, bulletinId = "B", issuedAt = sept8.atTime(15, 19),
            days = days.map { (date, levels) -> PlaceWarnings.DayWarnings(date, levels) },
            note = null
        )

    @Test
    fun `no previous bulletin - the first one is a state, not news`() {
        val next = place(milano, sept8 to levels(thunderstorm = ORANGE))
        assertEquals(emptyList<WarningChange>(), WarningDiff.between(null, next))
    }

    @Test
    fun `nothing moved - nothing to say`() {
        val same = place(milano, sept8 to levels(thunderstorm = YELLOW), sept9 to levels())
        assertEquals(emptyList<WarningChange>(), WarningDiff.between(same, same))
    }

    @Test
    fun `a level that appears, rises or comes back down is a change each`() {
        val before = place(milano, sept8 to levels(thunderstorm = YELLOW, hydraulic = YELLOW), sept9 to levels())
        val after = place(milano, sept8 to levels(thunderstorm = ORANGE, hydrogeological = YELLOW), sept9 to levels())
        assertEquals(
            listOf(
                WarningChange(sept8, HYDRAULIC, YELLOW, NONE),
                WarningChange(sept8, HYDROGEOLOGICAL, NONE, YELLOW),
                WarningChange(sept8, THUNDERSTORM, YELLOW, ORANGE)
            ),
            WarningDiff.between(before, after)
        )
    }

    @Test
    fun `a day the old bulletin did not cover starts from green, a day it dropped is over`() {
        val yesterdays = place(milano, sept8 to levels(thunderstorm = ORANGE), sept9 to levels(thunderstorm = YELLOW))
        val todays = place(milano, sept9 to levels(thunderstorm = YELLOW), sept10 to levels(hydraulic = YELLOW))
        assertEquals(
            listOf(WarningChange(sept10, HYDRAULIC, NONE, YELLOW)),
            WarningDiff.between(yesterdays, todays)
        )
    }

    @Test
    fun `another zone is another story - no comparison across a move`() {
        val there = place(trieste, sept8 to levels(thunderstorm = ORANGE))
        val here = place(milano, sept8 to levels(thunderstorm = YELLOW))
        assertEquals(emptyList<WarningChange>(), WarningDiff.between(there, here))
    }
}
