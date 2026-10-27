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
import java.time.ZoneId
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

    private val clear = WeatherCondition(0)
    private val fog = WeatherCondition(45)
    private val rain = WeatherCondition(63)
    private val snow = WeatherCondition(73)
    private val storm = WeatherCondition(95)

    /** One forecast hour: what the sky does, the chance of rain, the temperature. */
    private data class Hour(val condition: WeatherCondition, val pct: Int, val tempC: Double = 20.0)

    private infix fun WeatherCondition.at(pct: Int) = Hour(this, pct)

    /** Hours from `now`, one per entry. The sample report's own wind is a 12 km/h breeze. */
    private fun report(vararg hours: Hour) = report(hours.toList())

    private fun report(
        hours: List<Hour>,
        windKph: Double = 12.5,
        gustKph: Double = 18.0,
        start: LocalDateTime = now,
        /** What is falling now, as the hero shows it; by default the first hour's sky. */
        currentCondition: WeatherCondition? = null
    ) =
        sampleWeatherReport().let { sample ->
            sample.copy(
                current = sample.current.copy(
                    condition = currentCondition ?: hours.firstOrNull()?.condition ?: sample.current.condition,
                    wind = Wind(speedKph = windKph, directionCompass = "NW", degree = 310, gustKph = gustKph)
                ),
                hourly = hours.mapIndexed { i, hour ->
                    HourlyForecast(
                        time = start.plusHours(i.toLong()),
                        at = start.plusHours(i.toLong()).atZone(rome).toInstant(),
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

    /** The fixture city's zone: the hours carry their own instant now. */
    private val rome: ZoneId = ZoneId.of("Europe/Rome")

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
        assertEquals(Headline.Falling.RAIN, headline.falling)
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

    /** 24 set 2026: the rows carry the wind, so a gale this afternoon is news this morning. */
    @Test
    fun `a strong wind later today is announced with its hour`() {
        val calmNow = report(quiet())
        val later = calmNow.copy(
            hourly = calmNow.hourly.mapIndexed { i, h ->
                h.copy(windKph = 15.0, gustKph = if (i == 4) 70.0 else 25.0)
            }
        )
        val headline = HeadlineEngine.headline(later, now) as Headline.Wind
        assertEquals(now.plusHours(4), headline.at)
        assertEquals(70.0, headline.gustKph, 0.001)
        // Now wins over later, and still says no hour.
        val gustyNow = report(quiet(), windKph = 25.0, gustKph = 65.0)
        assertNull((HeadlineEngine.headline(gustyNow, now) as Headline.Wind).at)
        // Rows without wind (a cache from before) are not calm rows: nothing is said.
        assertNull(HeadlineEngine.headline(calmNow, now))
        // Tomorrow's gale is not today's sentence.
        val tomorrow = calmNow.copy(
            hourly = calmNow.hourly.mapIndexed { i, h -> h.copy(gustKph = if (i == 20) 90.0 else 20.0) }
        )
        assertNull(HeadlineEngine.headline(tomorrow, now))
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
        assertEquals(Headline.Falling.RAIN, headline.falling)
    }

    /** Manaus, 15 ott 2026: the hour under way holds a little rain at 39%, nothing falls now,
     * and the hero says overcast. The sentence must not announce the end of a rain the screen
     * says is not there. */
    @Test
    fun `a wet hour with nothing falling now is not rain stopping`() {
        val overcast = WeatherCondition(3)
        val report = report(
            listOf(rain at 39, rain at 43, overcast at 44, overcast at 43, overcast at 39) + quiet(20),
            currentCondition = overcast
        )
        val headline = HeadlineEngine.headline(report, now)
        assertTrue("not wet now: $headline", headline !is Headline.WetNow)
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
        assertEquals(Headline.Falling.SNOW, headline.falling)
    }

    @Test
    fun `rain and snow together speak as both`() {
        // 25 set 2026: the state engine writes 68/69, and the sentence said rain alone.
        val mixed = WeatherCondition(68)
        val soon = HeadlineEngine.headline(report(clear at 5, mixed at 80, mixed at 85, clear at 20), now)
        assertEquals(Headline.Falling.MIXED, (soon as Headline.WetSoon).falling)
        val wetNow = HeadlineEngine.headline(report(List(24) { mixed at 90 }), now)
        assertEquals(Headline.Falling.MIXED, (wetNow as Headline.WetNow).falling)
    }

    @Test
    fun `likely snow speaks as snow, and is never snowing now`() {
        val likelySnow = WeatherCondition(1071)
        val headline = HeadlineEngine.headline(report(List(24) { likelySnow at 80 }), now)
        assertEquals(Headline.Falling.SNOW, (headline as Headline.WetSoon).falling)
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
    fun `a storm at 10 percent is not the headline`() {
        // The alert's own floor (AlertEngine.SEVERE_MIN_CHANCE_PCT): the banner must not
        // announce a storm the notifications dropped. The rain after it still speaks.
        val report = report(clear at 5, storm at 10, rain at 80, clear at 10)
        assertTrue(HeadlineEngine.headline(report, now) is Headline.WetSoon)
    }

    @Test
    fun `an empty report says nothing rather than inventing`() {
        assertNull(HeadlineEngine.headline(sampleWeatherReport().copy(hourly = emptyList()), now))
    }

    // ------------------------------------------- the quiet day's sentence (12 ott 2026)

    /** [days] daily rows from today, each a condition; hours from [hours], starting now. */
    private fun calmReport(hours: List<Hour>, vararg days: WeatherCondition) =
        report(hours).copy(
            daily = days.mapIndexed { i, condition ->
                com.callbackdev.chiaro.domain.model.DailyForecast(
                    now.toLocalDate().plusDays(i.toLong()), 24.0 + i, 14.0 + i, condition, 10, 5
                )
            }
        )

    /** A warm afternoon: 20° now, peaking at 23° three hours out, then cooling. */
    private val warmingDay = List(10) { i -> Hour(clear, 10, tempC = if (i == 3) 23.0 else 20.0) }

    @Test
    fun `the ladder still answers no on a quiet day, and calm answers the quiet day`() {
        val r = calmReport(warmingDay, clear, clear, clear, rain, clear, clear, clear)
        assertNull(HeadlineEngine.headline(r, now))
        val calm = HeadlineEngine.calm(r, now)!!
        val dry = calm.dry as Headline.Calm.Dry.Until
        assertEquals(now.toLocalDate().plusDays(3), dry.date)
        assertEquals(3, dry.daysAhead)
        assertEquals(Headline.Falling.RAIN, dry.falling)
        val peak = calm.warmth as Headline.Calm.Warmth.Peak
        assertEquals(now.plusHours(3), peak.at)
        assertEquals(23.0, peak.tempC, 0.0)
    }

    @Test
    fun `the peak is the first hour that prints the high`() {
        val hours = List(10) { i -> Hour(clear, 10, tempC = when (i) { 2 -> 22.7; 3 -> 22.9; else -> 20.0 }) }
        val peak = HeadlineEngine.calm(calmReport(hours, clear, clear), now)!!.warmth as Headline.Calm.Warmth.Peak
        assertEquals(now.plusHours(2), peak.at)
        assertEquals(22.7, peak.tempC, 0.0)
    }

    @Test
    fun `a wet hour on a later day ends the dry spell even under a dry label`() {
        // 36 hours from 14:00: tomorrow 20:00 rains, though tomorrow's label says clear.
        val hours = List(36) { i -> if (i == 30) rain at 30 else clear at 10 }
        val calm = HeadlineEngine.calm(calmReport(hours, clear, clear, clear, clear), now)!!
        val dry = calm.dry as Headline.Calm.Dry.Until
        assertEquals(1, dry.daysAhead)
    }

    @Test
    fun `no wet day in sight is the forecast's own length, and only from three days`() {
        val week = HeadlineEngine.calm(calmReport(warmingDay, clear, clear, clear, clear, clear, clear, clear), now)!!
        assertEquals(Headline.Calm.Dry.Days(7), week.dry)
        val short = HeadlineEngine.calm(calmReport(warmingDay, clear, clear), now)!!
        assertNull(short.dry)
    }

    @Test
    fun `a wet hour left today says nothing about a dry spell`() {
        // 20% and 0.2 mm is a wet hour to the state engine, and under every rung of the ladder.
        val hours = listOf(clear at 10, rain at 20, clear at 10)
        val calm = HeadlineEngine.calm(calmReport(hours, clear, clear, clear, clear), now)
        assertNull(calm?.dry)
    }

    @Test
    fun `snow names the next day as snow`() {
        val calm = HeadlineEngine.calm(calmReport(warmingDay, clear, clear, snow, clear), now)!!
        assertEquals(Headline.Falling.SNOW, (calm.dry as Headline.Calm.Dry.Until).falling)
    }

    @Test
    fun `past the peak, or with a peak no warmer than now, tomorrow's range is the warmth`() {
        val cooling = List(10) { i -> Hour(clear, 10, tempC = 20.0 - i) }
        val flat = List(10) { Hour(clear, 10, tempC = 20.0) }
        listOf(cooling, flat).forEach { hours ->
            val calm = HeadlineEngine.calm(calmReport(hours, clear, clear), now)!!
            assertEquals(Headline.Calm.Warmth.Tomorrow(lowC = 15.0, highC = 25.0), calm.warmth)
        }
    }

    @Test
    fun `nothing true to say is still nothing`() {
        // One day of forecast, past its peak: no spell to name and no tomorrow.
        val cooling = List(5) { i -> Hour(clear, 10, tempC = 20.0 - i) }
        assertNull(HeadlineEngine.calm(calmReport(cooling, clear), now))
        assertNull(HeadlineEngine.calm(sampleWeatherReport().copy(hourly = emptyList()), now))
    }

    // ------------------------------------------- today against yesterday (13 ott 2026)

    @Test
    fun `three degrees from yesterday's high take the hour's place`() {
        // calmReport's today is 24.0 high.
        val warmer = HeadlineEngine.calm(calmReport(warmingDay, clear, clear), now, yesterdayHighC = 20.5)!!
        assertEquals(3.5, (warmer.warmth as Headline.Calm.Warmth.Peak).vsYesterdayC!!, 1e-9)
        val cooler = HeadlineEngine.calm(calmReport(warmingDay, clear, clear), now, yesterdayHighC = 28.0)!!
        assertEquals(-4.0, (cooler.warmth as Headline.Calm.Warmth.Peak).vsYesterdayC!!, 1e-9)
    }

    @Test
    fun `under three degrees, or with no yesterday, the hour stays`() {
        val close = HeadlineEngine.calm(calmReport(warmingDay, clear, clear), now, yesterdayHighC = 22.0)!!
        assertNull((close.warmth as Headline.Calm.Warmth.Peak).vsYesterdayC)
        val none = HeadlineEngine.calm(calmReport(warmingDay, clear, clear), now)!!
        assertNull((none.warmth as Headline.Calm.Warmth.Peak).vsYesterdayC)
    }

    // ------------------------------------------------------------ the heat (14.6, 27 Oct 2026)

    /**
     * Rome, 30–31 July 2026 from the archive, as Today reads it at [at]: the hours from the
     * one under way, the hero's feels-like that hour's.
     */
    private fun rome(at: LocalDateTime, feelsLike: (HourlyForecast) -> Double? = { it.feelsLikeC }) =
        sampleWeatherReport().let { sample ->
            val hours = RomeHeatWave.hours()
                .filter { !it.time.isBefore(at.withMinute(0)) }
                .map { it.copy(feelsLikeC = feelsLike(it)) }
            sample.copy(
                current = sample.current.copy(
                    condition = hours.first().condition,
                    tempC = hours.first().tempC,
                    feelsLikeC = hours.first().feelsLikeC ?: hours.first().tempC
                ),
                hourly = hours
            )
        }

    private val romeMorning: LocalDateTime = LocalDateTime.of(2026, 7, 30, 7, 0)

    @Test
    fun `heat ahead says the day's peak and its hour`() {
        val heat = HeadlineEngine.headline(rome(romeMorning), romeMorning) as Headline.Heat
        assertEquals(false, heat.ongoing)
        assertEquals(41.3, heat.peakC!!, 0.001)
        assertEquals(LocalDateTime.of(2026, 7, 30, 13, 0), heat.peakAt)
        assertNull(heat.until)
    }

    @Test
    fun `heat under way says when it lets up, and the peak only while it is still to come`() {
        // 10:00, feels like 36.9: over the bar until 23:00 (31.8), the peak at 13:00 ahead.
        val ten = LocalDateTime.of(2026, 7, 30, 10, 0)
        val heat = HeadlineEngine.headline(rome(ten), ten) as Headline.Heat
        assertEquals(true, heat.ongoing)
        assertEquals(LocalDateTime.of(2026, 7, 30, 23, 0), heat.until)
        assertEquals(41.3, heat.peakC!!, 0.001)
        assertEquals(LocalDateTime.of(2026, 7, 30, 13, 0), heat.peakAt)
        // 15:00, feels like 37.7, every hour ahead cooler: no peak clause, the hour it lets up.
        val three = ten.withHour(15)
        val later = HeadlineEngine.headline(rome(three), three) as Headline.Heat
        assertEquals(true, later.ongoing)
        assertNull(later.peakAt)
        assertEquals(LocalDateTime.of(2026, 7, 30, 23, 0), later.until)
    }

    @Test
    fun `heat that does not let up within the day says so`() {
        // A night that does not cool: every hour of the next day over the bar.
        val ten = LocalDateTime.of(2026, 7, 30, 10, 0)
        val heat = HeadlineEngine.headline(rome(ten) { 34.0 }, ten) as Headline.Heat
        assertEquals(true, heat.ongoing)
        assertNull(heat.until)
        assertNull(heat.peakAt)
    }

    @Test
    fun `the ladder - the umbrella over the heat, the heat over the fog and the wind`() {
        val report = rome(romeMorning)
        val wet = report.copy(hourly = report.hourly.mapIndexed { i, h -> if (i == 2) h.copy(precipChancePct = 80, condition = rain) else h })
        assertTrue(HeadlineEngine.headline(wet, romeMorning) is Headline.WetSoon)
        val foggy = report.copy(hourly = report.hourly.mapIndexed { i, h -> if (i == 2) h.copy(condition = fog) else h })
        assertTrue(HeadlineEngine.headline(foggy, romeMorning) is Headline.Heat)
        val gusty = report.copy(current = report.current.copy(wind = Wind(speedKph = 25.0, directionCompass = "NW", degree = 310, gustKph = 65.0)))
        assertTrue(HeadlineEngine.headline(gusty, romeMorning) is Headline.Heat)
    }

    @Test
    fun `heat tomorrow is the last rung`() {
        // 23:00 on the 30th, feels like 31.8: nothing hot left today, the 31st peaks at 13:00.
        val night = LocalDateTime.of(2026, 7, 30, 23, 0)
        val tomorrow = HeadlineEngine.headline(rome(night), night) as Headline.HeatTomorrow
        assertEquals(41.0, tomorrow.peakC, 0.001)
        assertEquals(LocalDateTime.of(2026, 7, 31, 13, 0), tomorrow.peakAt)
        // Tomorrow's umbrella comes first.
        val wet = rome(night).let { r -> r.copy(hourly = r.hourly.map { if (it.time.hour == 9) it.copy(precipChancePct = 90, condition = rain) else it }) }
        assertTrue(HeadlineEngine.headline(wet, night) is Headline.WetTomorrow)
    }

    @Test
    fun `a row without a feels-like is never hot`() {
        // A cache from before the rows carried one: the day stays silent rather than hot.
        assertNull(HeadlineEngine.headline(rome(romeMorning) { null }, romeMorning))
    }
}
