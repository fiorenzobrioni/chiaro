package com.callbackdev.chiaro.notifications

import com.callbackdev.chiaro.domain.AlertEngine
import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.model.WeatherCondition
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The window under the expanded notification: everything it prints is read off the
 * same hours the engine judged, so the arithmetic is worth a table (Fase 6b).
 */
class AlertDetailsTest {

    /** The zone the fixture hours are written on, so each carries a real instant. */
    private val rome: ZoneId = ZoneId.of("Europe/Rome")

    private val day = LocalDateTime.of(2026, 9, 4, 0, 0)

    /** [codes] is one WMO code per hour from 12:00, [rain] its chance, [temp] its degrees. */
    private fun hours(
        codes: List<Int>,
        // A severe code carries a chance the engine believes, as the provider's do
        rain: List<Int> = codes.map { if (it in AlertEngine.SevereCodes) 40 else 0 },
        temp: List<Double> = codes.map { 20.0 }
    ): List<HourlyForecast> = codes.indices.map { i ->
        HourlyForecast(
            time = day.withHour(12).plusHours(i.toLong()),
            at = day.withHour(12).plusHours(i.toLong()).atZone(rome).toInstant(),
            tempC = temp[i],
            condition = WeatherCondition(codes[i]),
            precipChancePct = rain[i],
            cloudCoverPct = 0
        )
    }

    private fun at(hour: Int) = day.withHour(hour)

    @Test
    fun `a severe run is the consecutive stretch around the alert's hour`() {
        // 12 13 14 15 16 17 → clear, clear, thunder, thunder, thunder, clear
        val window = AlertDetails.severeWindow(
            hours(
                codes = listOf(0, 0, 95, 95, 96, 0),
                rain = listOf(0, 10, 80, 90, 70, 20),
                temp = listOf(26.0, 27.0, 25.0, 22.0, 19.0, 18.0)
            ),
            at = at(14),
            elevationM = null
        )!!
        assertEquals(at(14), window.start)
        assertEquals(at(16), window.end)
        assertEquals(90, window.peakPrecipPct)
        assertEquals(at(15), window.peakPrecipAt)
        assertEquals(19.0, window.lowC, 0.001)
        assertEquals(25.0, window.highC, 0.001)
        assertFalse(window.openEnded)
        assertFalse(window.singleHour)
    }

    @Test
    fun `the run extends backwards too - the alert names its first hour, not the window's`() {
        // The engine's alert is anchored at the first severe hour it FINDS from now,
        // which can sit mid-storm when the earlier hours are already in the past.
        val window = AlertDetails.severeWindow(
            hours(codes = listOf(95, 95, 95, 0)),
            at = at(14),
            elevationM = null
        )!!
        assertEquals(at(12), window.start)
        assertEquals(at(14), window.end)
    }

    @Test
    fun `a run that reaches the end of the forecast is open-ended`() {
        val window = AlertDetails.severeWindow(hours(codes = listOf(0, 95, 95)), at = at(13), elevationM = null)!!
        assertEquals(at(13), window.start)
        assertTrue(window.openEnded)
    }

    @Test
    fun `one hour on its own is one hour, not a range`() {
        val window = AlertDetails.severeWindow(hours(codes = listOf(0, 95, 0)), at = at(13), elevationM = null)!!
        assertTrue(window.singleHour)
        assertFalse(window.openEnded)
        assertEquals(window.start, window.end)
    }

    @Test
    fun `rain uses the threshold, not the weather code`() {
        val window = AlertDetails.rainWindow(
            hours(codes = listOf(61, 61, 61, 61), rain = listOf(40, 70, 85, 30)),
            at = at(13),
            thresholdPct = 70
        )!!
        assertEquals(at(13), window.start)
        assertEquals(at(14), window.end)
        assertEquals(85, window.peakPrecipPct)
    }

    @Test
    fun `an hour the forecast no longer carries has no window`() {
        // A cached report outlives the alert built from it: the honest answer is none,
        // and the notification then prints its sentence alone.
        assertNull(AlertDetails.severeWindow(hours(codes = listOf(95, 95)), at = at(9), elevationM = null))
        assertNull(AlertDetails.severeWindow(emptyList(), at = at(12), elevationM = null))
    }

    @Test
    fun `an hour that does not hold has no window`() {
        assertNull(AlertDetails.severeWindow(hours(codes = listOf(0, 0)), at = at(12), elevationM = null))
        assertNull(
            AlertDetails.rainWindow(
                hours(codes = listOf(61, 61), rain = listOf(10, 20)),
                at = at(12)
            )
        )
    }

    @Test
    fun `the heat window is the run over the bar, with its worst feels-like and the thermometer's own range`() {
        // 12 13 14 15 16 17 → feels like 33, 36, 40.5, 41, 38, 34: over 35 from 13 to 16.
        val feels = listOf(33.0, 36.0, 40.5, 41.0, 38.0, 34.0)
        val hot = hours(codes = List(6) { 0 }, temp = listOf(30.0, 33.0, 36.0, 37.0, 36.0, 32.0))
            .mapIndexed { i, h -> h.copy(feelsLikeC = feels[i]) }
        val window = AlertDetails.heatWindow(hot, at = at(13))!!
        assertEquals(at(13), window.start)
        assertEquals(at(16), window.end)
        assertEquals(41.0, window.peakC, 0.001)
        assertEquals(at(15), window.peakAt)
        assertEquals(33.0, window.lowC, 0.001)
        assertEquals(37.0, window.highC, 0.001)
        assertFalse(window.openEnded)
        assertFalse(window.singleHour)
        // Under the bar at the anchor, or an anchor the hours do not have: no window.
        assertNull(AlertDetails.heatWindow(hot, at = at(12)))
        assertNull(AlertDetails.heatWindow(hot, at = at(20)))
        // A run that reaches the end of the data is open-ended, as the rain's is.
        assertTrue(AlertDetails.heatWindow(hot.take(4), at = at(13))!!.openEnded)
    }

    @Test
    fun `the wind window is the run over the bar, with its strongest gust and the thermometer's own range`() {
        // 12 13 14 15 16 17 → gusts 50, 72, 95, 88, 71, 40: over 70 from 13 to 16.
        val gusts = listOf(50.0, 72.0, 95.0, 88.0, 71.0, 40.0)
        val windy = hours(codes = List(6) { 0 }, temp = listOf(10.0, 9.0, 8.0, 7.0, 6.0, 5.0))
            .mapIndexed { i, h -> h.copy(gustKph = gusts[i]) }
        val window = AlertDetails.windWindow(windy, at = at(13), elevationM = 18.0)!!
        assertEquals(at(13), window.start)
        assertEquals(at(16), window.end)
        assertEquals(95.0, window.peakKph, 0.001)
        assertEquals(at(14), window.peakAt)
        assertEquals(6.0, window.lowC, 0.001)
        assertEquals(9.0, window.highC, 0.001)
        assertFalse(window.openEnded)
        // Under the bar at the anchor: no window. At 1 700 m the bar is 100 km/h: none at all.
        assertNull(AlertDetails.windWindow(windy, at = at(12), elevationM = 18.0))
        assertNull(AlertDetails.windWindow(windy, at = at(14), elevationM = 1700.0))
        // No elevation is the lowland bar, and the severe window reads the same hours.
        assertEquals(at(13), AlertDetails.windWindow(windy, at = at(14), elevationM = null)!!.start)
        assertEquals(at(16), AlertDetails.severeWindow(windy, at = at(14), elevationM = null)!!.end)
    }
}
