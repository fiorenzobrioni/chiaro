package com.callbackdev.chiaro.ui.today

import com.callbackdev.chiaro.domain.AlertEngine
import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.model.WeatherCondition
import com.callbackdev.chiaro.domain.sample.sampleWeatherReport
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The sentence is the most product-shaped logic in Fase 2, so it is tested the way the
 * engines are: a table of skies in, a claim out — including the claim of silence.
 */
class HeadlineEngineTest {

    private val now: LocalDateTime = LocalDateTime.of(2026, 9, 2, 14, 0)

    private val clear = WeatherCondition(0, "Clear", "☀️")
    private val rain = WeatherCondition(63, "Rainy", "🌧️")
    private val snow = WeatherCondition(73, "Snowy", "🌨️")
    private val storm = WeatherCondition(95, "Thunderstorm", "⛈️")

    /** Hours from `now`, one per entry: condition to rain chance. */
    private fun report(vararg hours: Pair<WeatherCondition, Int>) =
        sampleWeatherReport().copy(
            hourly = hours.mapIndexed { i, (condition, pct) ->
                HourlyForecast(
                    time = now.plusHours(i.toLong()),
                    tempC = 20.0,
                    condition = condition,
                    precipChancePct = pct,
                    cloudCoverPct = 50
                )
            }
        )

    @Test
    fun `a quiet day says nothing`() {
        val report = report(*Array(24) { clear to 10 })
        assertNull(HeadlineEngine.headline(report, now))
    }

    @Test
    fun `rain within six hours is the umbrella sentence with its clearing`() {
        val report = report(
            clear to 5, clear to 10, clear to 75, rain to 80, clear to 40, clear to 10
        )
        val headline = HeadlineEngine.headline(report, now) as Headline.WetSoon
        assertEquals(now.plusHours(2), headline.at)
        assertEquals(75, headline.pct)
        assertEquals(now.plusHours(4), headline.clearsAt)
        assertTrue(!headline.snow)
    }

    @Test
    fun `no clearing in sight means no clearing promised`() {
        val report = report(*Array(24) { if (it < 2) clear to 5 else rain to 90 })
        val headline = HeadlineEngine.headline(report, now) as Headline.WetSoon
        assertNull(headline.clearsAt)
    }

    @Test
    fun `rain past the six hour horizon is not news yet`() {
        val hours = Array(24) { i -> if (i >= 8) rain to 90 else clear to 10 }
        assertNull(HeadlineEngine.headline(report(*hours), now))
    }

    @Test
    fun `already raining says when it stops`() {
        val report = report(rain to 90, rain to 85, rain to 60, clear to 30, clear to 5)
        val headline = HeadlineEngine.headline(report, now) as Headline.WetNow
        assertEquals(now.plusHours(3), headline.stopsAt)
        assertTrue(!headline.snow)
    }

    @Test
    fun `raining with no end in the horizon is the honest rest-of-day`() {
        val report = report(*Array(24) { rain to 90 })
        val headline = HeadlineEngine.headline(report, now) as Headline.WetNow
        assertNull(headline.stopsAt)
    }

    @Test
    fun `snow speaks as snow`() {
        val report = report(clear to 5, snow to 80, snow to 85, clear to 20)
        val headline = HeadlineEngine.headline(report, now) as Headline.WetSoon
        assertTrue(headline.snow)
    }

    @Test
    fun `a storm outranks the umbrella`() {
        val report = report(clear to 5, rain to 80, storm to 90, clear to 10)
        val headline = HeadlineEngine.headline(report, now) as Headline.Severe
        assertEquals(AlertEngine.SevereBucket.THUNDER, headline.bucket)
        assertEquals(now.plusHours(2), headline.at)
    }

    @Test
    fun `a storm twelve hours out already leads`() {
        val hours = Array(24) { i -> if (i == 11) storm to 90 else clear to 5 }
        val headline = HeadlineEngine.headline(report(*hours), now)
        assertTrue(headline is Headline.Severe)
    }

    @Test
    fun `an empty report says nothing rather than inventing`() {
        assertNull(HeadlineEngine.headline(sampleWeatherReport().copy(hourly = emptyList()), now))
    }
}
