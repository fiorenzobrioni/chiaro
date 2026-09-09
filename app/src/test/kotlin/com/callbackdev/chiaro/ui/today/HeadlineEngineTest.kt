package com.callbackdev.chiaro.ui.today

import com.callbackdev.chiaro.domain.AlertEngine
import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.model.WeatherCondition
import com.callbackdev.chiaro.domain.model.Wind
import com.callbackdev.chiaro.domain.sample.sampleWeatherReport
import com.callbackdev.chiaro.domain.warnings.PlaceWarnings
import com.callbackdev.chiaro.domain.warnings.WarningHazard
import com.callbackdev.chiaro.domain.warnings.WarningLevel
import com.callbackdev.chiaro.domain.warnings.WarningZone
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The sentence is the most product-shaped logic in Fase 2, so it is tested the way the
 * engines are: a table of skies in, a claim out — including the claim of silence.
 *
 * Since 9 set 2026 the table is a ladder: the sentence anticipates, and the tests below
 * hold both what each rung says and which rung wins.
 */
class HeadlineEngineTest {

    private val now: LocalDateTime = LocalDateTime.of(2026, 9, 2, 14, 0)

    private val clear = WeatherCondition(0, "Clear", "☀️")
    private val fog = WeatherCondition(45, "Fog", "🌫️")
    private val rain = WeatherCondition(63, "Rainy", "🌧️")
    private val snow = WeatherCondition(73, "Snowy", "🌨️")
    private val storm = WeatherCondition(95, "Thunderstorm", "⛈️")

    /** One forecast hour: what the sky does, the chance of rain, the temperature. */
    private data class Hour(val condition: WeatherCondition, val pct: Int, val tempC: Double = 20.0)

    private infix fun WeatherCondition.at(pct: Int) = Hour(this, pct)

    /** Hours from `now`, one per entry. The sample report's own wind is a 12 km/h breeze. */
    private fun report(vararg hours: Hour) = report(hours.toList())

    private fun report(
        hours: List<Hour>,
        windKph: Double = 12.5,
        gustKph: Double = 18.0,
        start: LocalDateTime = now
    ) =
        sampleWeatherReport().let { sample ->
            sample.copy(
                current = sample.current.copy(
                    wind = Wind(speedKph = windKph, directionCompass = "NW", degree = 310, gustKph = gustKph)
                ),
                hourly = hours.mapIndexed { i, hour ->
                    HourlyForecast(
                        time = start.plusHours(i.toLong()),
                        tempC = hour.tempC,
                        condition = hour.condition,
                        precipChancePct = hour.pct,
                        cloudCoverPct = 50
                    )
                }
            )
        }

    private fun quiet(hours: Int = 36) = List(hours) { clear at 10 }

    // ------------------------------------------------------------ step zero

    private val zone = WarningZone("Lomb-09", "Nodo Idraulico di Milano", "Lombardia")

    private fun levels(
        hydraulic: WarningLevel = WarningLevel.NONE,
        hydrogeological: WarningLevel = WarningLevel.NONE,
        thunderstorm: WarningLevel = WarningLevel.NONE
    ) = mapOf(
        WarningHazard.HYDRAULIC to hydraulic,
        WarningHazard.HYDROGEOLOGICAL to hydrogeological,
        WarningHazard.THUNDERSTORM to thunderstorm
    )

    private fun warnings(
        today: Map<WarningHazard, WarningLevel> = levels(),
        tomorrow: Map<WarningHazard, WarningLevel> = levels()
    ) = PlaceWarnings(
        zone = zone,
        bulletinId = "DPC_BULLETIN_2026_09_02_1",
        issuedAt = now.toLocalDate().atTime(15, 46),
        days = listOf(
            PlaceWarnings.DayWarnings(now.toLocalDate(), today),
            PlaceWarnings.DayWarnings(now.toLocalDate().plusDays(1), tomorrow)
        ),
        note = null
    )

    @Test
    fun `an orange warning outranks everything the model has to say`() {
        // A thunderstorm inside the severe window would otherwise be rung one.
        val severe = report(List(6) { storm at 90 })
        val headline = HeadlineEngine.headline(
            severe, now, warnings(today = levels(thunderstorm = WarningLevel.ORANGE))
        )
        val official = headline as Headline.Official
        assertEquals(WarningLevel.ORANGE, official.level)
        assertEquals(listOf(WarningHazard.THUNDERSTORM), official.hazards)
        assertTrue(official.today)
    }

    @Test
    fun `yellow never takes the sentence, however the day looks`() {
        val yellow = warnings(today = levels(hydraulic = WarningLevel.YELLOW))
        assertNull(HeadlineEngine.headline(report(quiet()), now, yellow))
    }

    @Test
    fun `a green bulletin leaves the ladder exactly as it was`() {
        assertNull(HeadlineEngine.headline(report(quiet()), now, warnings()))
    }

    /** Today before tomorrow when both carry the peak: a sentence about now beats one
     * about later, and only the day named decides which hazards are printed. */
    @Test
    fun `today wins the day and only the peak hazards are named`() {
        val both = warnings(
            today = levels(thunderstorm = WarningLevel.RED, hydrogeological = WarningLevel.YELLOW),
            tomorrow = levels(hydraulic = WarningLevel.RED)
        )
        val official = HeadlineEngine.headline(report(quiet()), now, both) as Headline.Official
        assertTrue(official.today)
        assertEquals(listOf(WarningHazard.THUNDERSTORM), official.hazards)
    }

    @Test
    fun `a peak that falls only tomorrow says so`() {
        val tomorrow = warnings(tomorrow = levels(hydraulic = WarningLevel.ORANGE))
        val official = HeadlineEngine.headline(report(quiet()), now, tomorrow) as Headline.Official
        assertEquals(false, official.today)
        assertEquals(listOf(WarningHazard.HYDRAULIC), official.hazards)
    }

    /** Two hazards at the same level print in the issuer's own tie-break, not in
     * declaration order: hydraulic, thunderstorms, hydrogeological. */
    @Test
    fun `hazards sharing the peak keep the issuer's order`() {
        val two = warnings(
            today = levels(hydrogeological = WarningLevel.ORANGE, hydraulic = WarningLevel.ORANGE)
        )
        val official = HeadlineEngine.headline(report(quiet()), now, two) as Headline.Official
        assertEquals(
            listOf(WarningHazard.HYDRAULIC, WarningHazard.HYDROGEOLOGICAL), official.hazards
        )
    }

    @Test
    fun `a quiet day says nothing`() {
        assertNull(HeadlineEngine.headline(report(quiet()), now))
    }

    @Test
    fun `rain within six hours is the umbrella sentence with its clearing`() {
        val report = report(
            clear at 5, clear at 10, clear at 75, rain at 80, clear at 40, clear at 10
        )
        val headline = HeadlineEngine.headline(report, now) as Headline.WetSoon
        assertEquals(now.plusHours(2), headline.at)
        assertEquals(75, headline.pct)
        assertEquals(now.plusHours(4), headline.clearsAt)
        assertTrue(!headline.snow)
    }

    @Test
    fun `no clearing in sight means no clearing promised`() {
        val report = report(List(24) { if (it < 2) clear at 5 else rain at 90 })
        val headline = HeadlineEngine.headline(report, now) as Headline.WetSoon
        assertNull(headline.clearsAt)
    }

    /**
     * Until 9 set 2026 the umbrella looked six hours out and this was a test of
     * silence. A dry afternoon before a wet evening is exactly the day a reader wants
     * told about at two o'clock, so the horizon is the rest of the local day.
     */
    @Test
    fun `rain later today is the umbrella sentence too`() {
        val hours = List(24) { i -> if (i >= 8) rain at 90 else clear at 10 } // 22:00 tonight
        val headline = HeadlineEngine.headline(report(hours), now) as Headline.WetSoon
        assertEquals(now.plusHours(8), headline.at)
    }

    @Test
    fun `rain after midnight is tomorrow's sentence, unless it is within six hours`() {
        // Seen at 14:00, rain from 03:00: tomorrow's business, said as tomorrow.
        val late = List(30) { i -> if (i >= 13) rain at 90 else clear at 10 }
        val tomorrow = HeadlineEngine.headline(report(late), now) as Headline.WetTomorrow
        assertEquals(now.plusHours(13), tomorrow.at)

        // Seen at 22:00, rain from 03:00: still tonight, the notifier's six hours.
        val evening = LocalDateTime.of(2026, 9, 2, 22, 0)
        val eveningReport = report(
            List(12) { i -> if (i >= 5) rain at 90 else clear at 10 },
            start = evening
        )
        val soon = HeadlineEngine.headline(eveningReport, evening) as Headline.WetSoon
        assertEquals(evening.plusHours(5), soon.at)
    }

    @Test
    fun `a chance of at least half is rain possible, not an umbrella`() {
        val report = report(clear at 10, clear at 30, clear at 55, clear at 60, clear at 20)
        val headline = HeadlineEngine.headline(report, now) as Headline.WetMaybe
        assertEquals(now.plusHours(2), headline.at)
        assertEquals(55, headline.pct)
    }

    @Test
    fun `under half is nothing to say`() {
        assertNull(HeadlineEngine.headline(report(clear at 10, clear at 45, clear at 49), now))
    }

    @Test
    fun `frost by morning is said while it is not freezing yet`() {
        val hours = List(20) { i ->
            Hour(clear, 5, tempC = when {
                i == 0 -> 6.0
                i == 15 -> -1.5 // 05:00
                else -> 3.0
            })
        }
        val headline = HeadlineEngine.headline(report(hours), now) as Headline.Frost
        assertEquals(now.plusHours(15), headline.at)
        assertEquals(-1.5, headline.minC, 0.001)
    }

    @Test
    fun `freezing now is the hero's number, not the sentence's`() {
        val hours = List(20) { Hour(clear, 5, tempC = -2.0) }
        assertNull(HeadlineEngine.headline(report(hours), now))
    }

    @Test
    fun `a frost past mid-morning tomorrow is not by morning`() {
        // The only sub-zero hour is at 11:00 tomorrow: outside the night's window.
        val hours = List(24) { i -> Hour(clear, 5, tempC = if (i == 21) -1.0 else 4.0) }
        assertNull(HeadlineEngine.headline(report(hours), now))
    }

    @Test
    fun `fog on the way is said, fog now is not`() {
        val ahead = report(clear at 5, clear at 5, clear at 5, fog at 5, fog at 5, clear at 5)
        assertEquals(now.plusHours(3), (HeadlineEngine.headline(ahead, now) as Headline.Fog).at)

        val already = report(fog at 5, fog at 5, clear at 5)
        assertNull(HeadlineEngine.headline(already, now))
    }

    @Test
    fun `a strong wind now is the day's fact`() {
        val gusty = report(quiet(), windKph = 25.0, gustKph = 65.0)
        val headline = HeadlineEngine.headline(gusty, now) as Headline.Wind
        assertEquals(65.0, headline.gustKph, 0.001)

        val steady = report(quiet(), windKph = 42.0, gustKph = 45.0)
        assertTrue(HeadlineEngine.headline(steady, now) is Headline.Wind)

        val breeze = report(quiet(), windKph = 30.0, gustKph = 50.0)
        assertNull(HeadlineEngine.headline(breeze, now))
    }

    @Test
    fun `the ladder - today's umbrella over frost, frost over maybe, maybe over tomorrow`() {
        val frostyMaybe = List(30) { i ->
            Hour(clear, if (i == 3) 55 else 5, tempC = if (i == 15) -1.0 else 4.0)
        }
        assertTrue(HeadlineEngine.headline(report(frostyMaybe), now) is Headline.Frost)

        val frostyUmbrella = frostyMaybe.mapIndexed { i, h -> if (i == 3) h.copy(pct = 80) else h }
        assertTrue(HeadlineEngine.headline(report(frostyUmbrella), now) is Headline.WetSoon)

        val maybeThenTomorrow = List(30) { i ->
            when {
                i == 3 -> clear at 55
                i >= 20 -> rain at 90 // 10:00 tomorrow
                else -> clear at 5
            }
        }
        assertTrue(HeadlineEngine.headline(report(maybeThenTomorrow), now) is Headline.WetMaybe)
    }

    @Test
    fun `already raining says when it stops`() {
        val report = report(rain at 90, rain at 85, rain at 60, clear at 30, clear at 5)
        val headline = HeadlineEngine.headline(report, now) as Headline.WetNow
        assertEquals(now.plusHours(3), headline.stopsAt)
        assertTrue(!headline.snow)
    }

    @Test
    fun `raining with no end in the horizon is the honest rest-of-day`() {
        val report = report(List(24) { rain at 90 })
        val headline = HeadlineEngine.headline(report, now) as Headline.WetNow
        assertNull(headline.stopsAt)
    }

    @Test
    fun `snow speaks as snow`() {
        val report = report(clear at 5, snow at 80, snow at 85, clear at 20)
        val headline = HeadlineEngine.headline(report, now) as Headline.WetSoon
        assertTrue(headline.snow)
    }

    @Test
    fun `a storm outranks the umbrella`() {
        val report = report(clear at 5, rain at 80, storm at 90, clear at 10)
        val headline = HeadlineEngine.headline(report, now) as Headline.Severe
        assertEquals(AlertEngine.SevereBucket.THUNDER, headline.bucket)
        assertEquals(now.plusHours(2), headline.at)
    }

    @Test
    fun `a storm twelve hours out already leads`() {
        val hours = List(24) { i -> if (i == 11) storm at 90 else clear at 5 }
        val headline = HeadlineEngine.headline(report(hours), now)
        assertTrue(headline is Headline.Severe)
    }

    @Test
    fun `an empty report says nothing rather than inventing`() {
        assertNull(HeadlineEngine.headline(sampleWeatherReport().copy(hourly = emptyList()), now))
    }
}
