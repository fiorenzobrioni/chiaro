package com.callbackdev.chiaro.domain.sky

import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.model.MoonPhase
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The screen's one moon (review of the Sky screen, point 6, 14 ott 2026). */
class MoonOfTheNightTest {

    private val milan = Coordinates(45.4642, 9.19)
    private val rome = ZoneId.of("Europe/Rome")
    private val tromso = Coordinates(69.6492, 18.9553)
    private val oslo = ZoneId.of("Europe/Oslo")

    @Test
    fun `the moon of a day is read at the middle of its astronomical night`() {
        val date = LocalDate.of(2026, 9, 25)
        val night = SkyScheduler.darkNight(date, rome, milan).night as SkyOccurrence.At
        val middle = night.start.plus(Duration.between(night.start, night.end!!).dividedBy(2))
        assertEquals(middle, MoonOfTheNight.instant(date, rome, milan))
    }

    @Test
    fun `a day with no astronomical night is read at the midnight that closes it`() {
        val midsummer = LocalDate.of(2026, 6, 21)
        assertEquals(
            midsummer.plusDays(1).atStartOfDay(oslo).toInstant(),
            MoonOfTheNight.instant(midsummer, oslo, tromso)
        )
    }

    @Test
    fun `a principal phase carries its own quarter, within two days of it`() {
        val at = MoonOfTheNight.instant(LocalDate.of(2026, 9, 25), rome, milan)
        assertEquals(MoonPhase.FULL_MOON, MoonPhase.at(at))
        val quarter = MoonOfTheNight.quarterOf(MoonPhase.FULL_MOON, at)!!
        assertEquals(MoonQuarterKind.FULL_MOON, quarter.kind)
        assertTrue(Duration.between(at, quarter.at).abs() < Duration.ofDays(2))
        // The calendar's full moon is the same instant.
        assertEquals(AstronomyEngine.nextMoonQuarter(at.minus(Duration.ofDays(3)), MoonQuarterKind.FULL_MOON).at, quarter.at)
    }

    @Test
    fun `the phases between the quarters have no quarter to name`() {
        val at = MoonOfTheNight.instant(LocalDate.of(2026, 9, 25), rome, milan)
        assertNull(MoonOfTheNight.quarterOf(MoonPhase.WAXING_GIBBOUS, at))
        assertNull(MoonOfTheNight.quarterOf(MoonPhase.WANING_CRESCENT, at))
    }
}
