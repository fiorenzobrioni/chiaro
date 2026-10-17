package com.callbackdev.chiaro.ui.today

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.domain.NowcastEngine
import com.callbackdev.chiaro.domain.NowcastEngine.Intensity
import com.callbackdev.chiaro.domain.WmoCode
import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.model.Nowcast
import com.callbackdev.chiaro.domain.model.QuarterHour
import com.callbackdev.chiaro.domain.model.WeatherCondition
import com.callbackdev.chiaro.domain.sample.sampleWeatherReport
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The next two hours in words (17 ott 2026): the sentence at the top of Today where the
 * quarters are read, and its place on the ladder.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "it")
class NextRainTextTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val zone = ZoneId.of("Europe/Rome")
    private val fmt = DateTimeFormatter.ofPattern("HH:mm")
    private val base = sampleWeatherReport()

    private fun at(hour: Int, minute: Int = 0): Instant =
        LocalDateTime.of(2026, 10, 17, hour, minute).atZone(zone).toInstant()

    private fun row(hour: Int, code: Int = WmoCode.OVERCAST.code) = HourlyForecast(
        time = LocalDateTime.of(2026, 10, 17, hour, 0),
        at = at(hour),
        tempC = 12.0,
        condition = WeatherCondition(code),
        precipChancePct = 70,
        cloudCoverPct = 90
    )

    /** Quarters from 14:00, fetched at 14:05, read at 14:07. */
    private fun reading(vararg mm: Double, currentCode: Int = WmoCode.OVERCAST.code): NowcastEngine.Reading? {
        val report = base.copy(
            current = base.current.copy(condition = WeatherCondition(currentCode)),
            hourly = (14..19).map { row(it) },
            systemInfo = base.systemInfo.copy(lastSync = at(14, 5)),
            nowcast = Nowcast(mm.mapIndexed { i, v -> QuarterHour(at(14).plusSeconds(900L * i), v, 0.0) })
        )
        return NowcastEngine.read(report, at(14, 7))
    }

    private fun say(reading: NowcastEngine.Reading?, currentCode: Int = WmoCode.OVERCAST.code): String? =
        HeadlineEngine.nextRain(reading, zone, currentCode)?.let { HeadlineText.nextRain(context, it) { t -> t.format(fmt) } }

    private fun dry(n: Int) = DoubleArray(n) { 0.0 }

    @Test
    fun `rain on the way, said in minutes, with how long it lasts`() {
        // Starts 14:30 (23 minutes: «circa 25»), stops 15:00: thirty minutes.
        assertEquals(
            "Pioggia debole tra circa 25 minuti, per circa 30 minuti",
            say(reading(0.0, 0.0, 0.2, 0.2, *dry(12)))
        )
    }

    @Test
    fun `an end an hour or more away is said by the clock`() {
        assertEquals(
            "Pioggia moderata tra circa 25 minuti, fino alle 15:30 circa",
            say(reading(0.0, 0.0, 0.8, 0.8, 0.8, 0.8, *dry(10)))
        )
    }

    @Test
    fun `a start under ten minutes away is any minute now`() {
        // 14:15 is eight minutes from 14:07.
        assertEquals(
            "Pioggia forte a momenti, per circa 15 minuti",
            say(reading(0.0, 2.5, *dry(14)))
        )
    }

    @Test
    fun `a start an hour away or more is said by the clock, and a spell past the window says so`() {
        assertEquals(
            "Pioggia debole verso le 15:15, almeno fino alle 16:00",
            say(reading(0.0, 0.0, 0.0, 0.0, 0.0, 0.2, 0.2, 0.2, *DoubleArray(8) { 0.2 }))
        )
    }

    @Test
    fun `wet now, when it stops, and when it comes back`() {
        assertEquals(
            "La pioggia smette tra circa 25 minuti, poi riprende verso le 15:15",
            say(reading(0.3, 0.3, 0.0, 0.0, 0.0, 0.3, *dry(10)))
        )
    }

    @Test
    fun `wet throughout, it holds at least to the window's end`() {
        assertEquals(
            "Pioggia moderata almeno fino alle 16:00",
            say(reading(*DoubleArray(16) { 0.8 }))
        )
    }

    @Test
    fun `the rain the hero shows over a dry window is about to stop`() {
        val code = WmoCode.RAIN_SLIGHT.code
        assertEquals("La pioggia smette a momenti", say(reading(*dry(16), currentCode = code), code))
    }

    @Test
    fun `snow has its own words`() {
        val headline = Headline.NextRain(
            falling = Headline.Falling.SNOW,
            intensity = Intensity.MODERATE,
            wetNow = false,
            change = LocalDateTime.of(2026, 10, 17, 14, 30),
            changeInMinutes = 23,
            then = null,
            thenInMinutes = null,
            until = LocalDateTime.of(2026, 10, 17, 16, 0)
        )
        assertEquals(
            "Neve moderata tra circa 25 minuti, almeno fino alle 16:00",
            HeadlineText.nextRain(context, headline) { it.format(fmt) }
        )
    }

    @Test
    fun `a dry window under a dry sky has no sentence`() {
        assertNull(say(reading(*dry(16))))
    }

    @Test
    @Config(qualifiers = "en")
    fun `in English`() {
        assertEquals(
            "Light rain in about 25 minutes, for about 30 minutes",
            say(reading(0.0, 0.0, 0.2, 0.2, *dry(12)))
        )
        assertEquals(
            "The rain stops in about 25 minutes, then starts again around 15:15",
            say(reading(0.3, 0.3, 0.0, 0.0, 0.0, 0.3, *dry(10)))
        )
    }

    @Test
    fun `minutes are said to the nearest five, never under ten`() {
        assertEquals(10, HeadlineText.roundedMinutes(10))
        assertEquals(25, HeadlineText.roundedMinutes(23))
        assertEquals(20, HeadlineText.roundedMinutes(22))
        assertEquals(55, HeadlineText.roundedMinutes(57))
    }

    @Test
    fun `on the ladder it comes after a severe hour and before the hourly umbrella`() {
        val report = base.copy(hourly = (14..19).map { row(it, if (it == 15) WmoCode.THUNDERSTORM.code else WmoCode.RAIN_MODERATE.code) })
        val next = HeadlineEngine.nextRain(reading(0.0, 0.0, 0.2, 0.2, *dry(12)), zone, WmoCode.OVERCAST.code)!!
        val now = LocalDateTime.of(2026, 10, 17, 14, 7)
        assertTrue(HeadlineEngine.headline(report, now, nextRain = next) is Headline.Severe)
        val calm = base.copy(hourly = (14..19).map { row(it, WmoCode.RAIN_MODERATE.code) })
        assertEquals(next, HeadlineEngine.headline(calm, now, nextRain = next))
        // Without a reading — the widgets — the hourly ladder answers as before.
        assertTrue(HeadlineEngine.headline(calm, now) is Headline.WetSoon)
    }
}
