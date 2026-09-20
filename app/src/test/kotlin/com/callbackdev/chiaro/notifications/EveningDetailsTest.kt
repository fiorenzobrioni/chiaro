package com.callbackdev.chiaro.notifications

import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.model.WeatherCondition
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The evening summary's arithmetic: the night it reports and the tomorrow it looks
 * at are read off the same report the engine judged, so both are worth a table.
 */
class EveningDetailsTest {

    private val today = LocalDate.of(2026, 1, 15)
    private val tomorrow = today.plusDays(1)
    private val zone: ZoneId = ZoneId.of("Europe/Rome")
    private val milan = Coordinates(45.4642, 9.1900)

    /** Hours from [start], one per entry: [temp] degrees, [rain] chance (null = absent). */
    private fun hours(
        start: LocalDateTime,
        temp: List<Double>,
        rain: List<Int?> = temp.map { 0 }
    ): List<HourlyForecast> = temp.indices.map { i ->
        HourlyForecast(
            time = start.plusHours(i.toLong()),
            at = start.plusHours(i.toLong()).atZone(zone).toInstant(),
            tempC = temp[i],
            condition = WeatherCondition(2, "x", "x"),
            precipChancePct = rain[i],
            cloudCoverPct = 40
        )
    }

    // --- the night ---

    @Test
    fun `the night is the coldest hour between now and sunrise, with its hour`() {
        // 20:00 → 08:00, falling to -2 at 06:00 then climbing: the 09:00 hour is
        // warmer AND past sunrise, and must not be the one reported either way.
        val night = EveningDetails.night(
            hours(today.atTime(20, 0), listOf(6.0, 4.0, 3.0, 1.0, 0.0, -1.0, -2.0, -2.5, 1.0, 5.0, 9.0)),
            from = today.atTime(20, 0),
            until = tomorrow.atTime(7, 0)
        )!!
        assertEquals(-2.5, night.lowC, 0.001)
        assertEquals(tomorrow.atTime(3, 0), night.lowAt)
    }

    @Test
    fun `hours that have already happened are not part of tonight`() {
        // The worker's report is NOT trimmed: this morning's 05:00 is still in it,
        // and it was colder than anything tonight will be.
        val list = hours(today.atTime(5, 0), List(20) { 15.0 }).toMutableList()
        list[0] = list[0].copy(tempC = -8.0)
        val night = EveningDetails.night(
            list, from = today.atTime(20, 0), until = tomorrow.atTime(7, 0)
        )!!
        assertEquals(15.0, night.lowC, 0.001)
    }

    @Test
    fun `a night nobody forecast a chance for reports no chance, never a zero`() {
        val night = EveningDetails.night(
            hours(today.atTime(20, 0), List(6) { 5.0 }, rain = List(6) { null }),
            from = today.atTime(20, 0),
            until = tomorrow.atTime(7, 0)
        )!!
        assertNull(night.peakPrecipPct)
        assertNull(night.peakPrecipAt)
    }

    @Test
    fun `the night's wettest hour is the peak and its time`() {
        val night = EveningDetails.night(
            hours(today.atTime(20, 0), List(6) { 5.0 }, rain = listOf(10, 30, 80, 65, null, 20)),
            from = today.atTime(20, 0),
            until = tomorrow.atTime(7, 0)
        )!!
        assertEquals(80, night.peakPrecipPct)
        assertEquals(today.atTime(22, 0), night.peakPrecipAt)
    }

    @Test
    fun `a report whose hours have all elapsed has no night to report`() {
        assertNull(
            EveningDetails.night(
                hours(today.atTime(5, 0), List(6) { 5.0 }),
                from = today.atTime(20, 0),
                until = tomorrow.atTime(7, 0)
            )
        )
    }

    // --- when the night ends ---

    @Test
    fun `the night ends at tomorrow's sunrise, in the city's own zone`() {
        // Milan, 16 January: sunrise is around 08:00 local. The point of the test is
        // that it is tomorrow's, local, and in the morning — not the exact minute.
        val end = EveningDetails.nightEnd(tomorrow, zone, milan)
        assertEquals(tomorrow, end.toLocalDate())
        assertTrue(end.hour in 7..8)
    }

    @Test
    fun `inside the polar night it falls back to six in the morning`() {
        // Longyearbyen in January: the sun does not come up at all, and a night with
        // no end would run to the following evening.
        val end = EveningDetails.nightEnd(tomorrow, ZoneId.of("Arctic/Longyearbyen"), Coordinates(78.22, 15.63))
        assertEquals(tomorrow.atTime(6, 0), end)
    }

    // --- tomorrow ---

    @Test
    fun `tomorrow's rain window is tomorrow's alone`() {
        // Rain from 23:00 tonight through 02:00, then again 14:00-16:00 tomorrow. The
        // line says "tomorrow", so the run it reports has to start inside tomorrow.
        val temp = List(20) { 10.0 }
        val rain = MutableList<Int?>(20) { 0 }
        rain[0] = 90 // 23:00 today
        rain[1] = 90 // 00:00 tomorrow
        rain[2] = 90 // 01:00
        rain[15] = 75 // 14:00
        rain[16] = 80 // 15:00
        val window = EveningDetails.tomorrowRain(hours(today.atTime(23, 0), temp, rain), tomorrow)!!
        assertEquals(tomorrow.atTime(0, 0), window.start)
        assertEquals(tomorrow.atTime(1, 0), window.end)
        assertFalse(window.openEnded)
    }

    @Test
    fun `a dry tomorrow has no window and a quiet one has no peak worth printing`() {
        val hours = hours(today.atTime(20, 0), List(30) { 10.0 }, rain = MutableList(30) { 15 })
        assertNull(EveningDetails.tomorrowRain(hours, tomorrow))
        assertEquals(15, EveningDetails.tomorrowRainPeak(hours, tomorrow)!!.pct)
    }

    @Test
    fun `the peak of a tomorrow nobody forecast is absent, not zero`() {
        val hours = hours(today.atTime(20, 0), List(30) { 10.0 }, rain = MutableList(30) { null })
        assertNull(EveningDetails.tomorrowRainPeak(hours, tomorrow))
    }

    // --- the sun ---

    @Test
    fun `tomorrow's sun carries the change in daylight against today`() {
        // Milan a month past the solstice is gaining light: the SIGN is the fact the
        // line prints, and a couple of minutes a day is the size of it.
        val todayDaylight = EveningDetails.sun(today, zone, milan, today = null)!!.daylight
        val sun = EveningDetails.sun(tomorrow, zone, milan, todayDaylight)!!
        assertTrue(sun.sunrise.isBefore(sun.sunset))
        assertEquals(sun.daylight, Duration.between(sun.sunrise, sun.sunset))
        assertEquals(sun.daylight.minus(todayDaylight), sun.daylightDelta)
        assertTrue(sun.daylightDelta!!.toMinutes() in 1..4)
    }

    @Test
    fun `without today's daylight there is no change to state`() {
        assertNull(EveningDetails.sun(tomorrow, zone, milan, today = null)!!.daylightDelta)
    }

    @Test
    fun `a day with no sunrise has no sun line at all`() {
        assertNull(
            EveningDetails.sun(
                tomorrow, ZoneId.of("Arctic/Longyearbyen"), Coordinates(78.22, 15.63), null
            )
        )
    }
}
