package com.callbackdev.chiaro.domain

import com.callbackdev.chiaro.domain.settings.NotificationSettings
import com.callbackdev.chiaro.domain.model.DailyForecast
import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.model.WeatherCondition
import com.callbackdev.chiaro.domain.model.WeatherReport
import com.callbackdev.chiaro.domain.sample.sampleWeatherReport
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertEngineTest {

    /** The zone the fixture hours are written on, so each carries a real instant. */
    private val rome: ZoneId = ZoneId.of("Europe/Rome")

    private val now: LocalDateTime = LocalDateTime.of(2023, 10, 27, 9, 0)
    private val cityKey = "4546:919"
    private val allOn = NotificationSettings(
        severeWeatherAlerts = true, dailySummary = true, precipitationWarning = true,
        // Named on purpose: every test below that wants the evening summary asks for
        // it by name, so this fixture keeps meaning what it meant before the default
        // flipped on 21 set 2026.
        eveningSummary = false
    )

    /**
     * A severe code gets a chance above [AlertEngine.SEVERE_MIN_CHANCE_PCT] and below
     * the umbrella's 70% unless the test names one: a storm the ensemble believes, and
     * no umbrella of its own to muddle what the test is about.
     */
    private fun hour(
        plusHours: Long,
        wmoCode: Int = 2,
        precipPct: Int? = if (wmoCode in AlertEngine.SevereCodes) 40 else 0
    ) = HourlyForecast(
        time = now.plusHours(plusHours),
        at = now.plusHours(plusHours).atZone(rome).toInstant(),
        tempC = 18.0,
        condition = WeatherCondition(wmoCode),
        precipChancePct = precipPct,
        cloudCoverPct = 50
    )

    /** The sample's own daily block starts three days out; the evening summary asks
     * for TOMORROW by date, so these tests say which days the report carries. */
    private fun day(date: LocalDate) = DailyForecast(
        date = date,
        highC = 18.0,
        lowC = 7.0,
        condition = WeatherCondition(2),
        precipPct = 20,
        uvIndexMax = 4
    )

    private fun report(
        hourly: List<HourlyForecast>,
        daily: List<DailyForecast>? = null
    ): WeatherReport = sampleWeatherReport().let { sample ->
        sample.copy(hourly = hourly, daily = daily ?: sample.daily)
    }

    private fun evaluate(
        hourly: List<HourlyForecast>,
        settings: NotificationSettings = allOn,
        state: AlertState = AlertState(),
        at: LocalDateTime = now,
        daily: List<DailyForecast>? = null
    ) = AlertEngine.evaluate(report(hourly, daily), settings, state, at, cityKey)

    // --- severe ---

    @Test
    fun `every severe code maps to a bucket and triggers`() {
        for ((code, bucket) in AlertEngine.SevereCodes) {
            val alerts = evaluate(listOf(hour(2, wmoCode = code)))
            val severe = alerts.single { it.kind == AlertKind.SEVERE }
            assertTrue(severe.fingerprint.contains(":sev:${bucket.name}:"))
        }
    }

    @Test
    fun `non-severe codes do not trigger`() {
        // rain showers 80, moderate rain 63, light snow showers 85: not severe
        val alerts = evaluate(listOf(hour(1, 80), hour(2, 63), hour(3, 85)))
        assertNull(alerts.find { it.kind == AlertKind.SEVERE })
    }

    @Test
    fun `a storm the ensemble barely believes is not severe`() {
        // 24 set 2026: a 95 at 19% saw 1 mm of rain less than half the time, 5 mm 6%
        val floor = AlertEngine.SEVERE_MIN_CHANCE_PCT
        assertNull(evaluate(listOf(hour(2, 95, floor - 1))).find { it.kind == AlertKind.SEVERE })
        assertNull(evaluate(listOf(hour(2, 82, 5))).find { it.kind == AlertKind.SEVERE })
        assertTrue(evaluate(listOf(hour(2, 95, floor))).any { it.kind == AlertKind.SEVERE })
    }

    @Test
    fun `ice, snow and a missing chance keep their code`() {
        // 67 freezing rain, 75 heavy snow: not measured, and dangerous in small amounts
        for (code in listOf(67, 75)) {
            assertTrue(evaluate(listOf(hour(2, code, 5))).any { it.kind == AlertKind.SEVERE })
        }
        // no chance served (model-dependent): absence is not evidence
        assertTrue(evaluate(listOf(hour(2, 95, null))).any { it.kind == AlertKind.SEVERE })
    }

    @Test
    fun `a storm arrives at its first probable hour`() {
        val storm = listOf(hour(2, 95, 10), hour(3, 95, 60), hour(4, 95, 70))
        val severe = evaluate(storm).single { it.kind == AlertKind.SEVERE }
        assertEquals(now.plusHours(3), severe.at)
    }

    @Test
    fun `an improbable storm does not silence the umbrella`() {
        // The 80% rain an hour after a 95 at 10% is the news; nobody else told it
        val day = listOf(hour(2, 95, 10), hour(3, precipPct = 80))
        val alerts = evaluate(day)
        assertNull(alerts.find { it.kind == AlertKind.SEVERE })
        assertTrue(alerts.any { it.kind == AlertKind.PRECIPITATION })
    }

    @Test
    fun `severe respects the 12h lookahead boundary`() {
        assertTrue(evaluate(listOf(hour(12, 95))).any { it.kind == AlertKind.SEVERE })
        assertNull(evaluate(listOf(hour(13, 95))).find { it.kind == AlertKind.SEVERE })
    }

    @Test
    fun `same storm same day is deduped, new day or new bucket re-fires`() {
        val storm = listOf(hour(2, 95))
        val fingerprint = evaluate(storm).single { it.kind == AlertKind.SEVERE }.fingerprint
        // same fingerprint already notified → silent
        assertNull(
            evaluate(storm, state = AlertState(severeFingerprints = setOf(fingerprint)))
                .find { it.kind == AlertKind.SEVERE }
        )
        // different hazard class → re-fires (75 = SNOW)
        assertTrue(
            evaluate(listOf(hour(2, 75)), state = AlertState(severeFingerprints = setOf(fingerprint)))
                .any { it.kind == AlertKind.SEVERE }
        )
        // 96 is still THUNDER on the same date → same storm, still silent
        assertNull(
            evaluate(listOf(hour(3, 96)), state = AlertState(severeFingerprints = setOf(fingerprint)))
                .find { it.kind == AlertKind.SEVERE }
        )
    }

    @Test
    fun `another city's fingerprint does not silence this one, and both stay silenced together`() {
        val storm = listOf(hour(2, 95))
        val fingerprint = evaluate(storm).single { it.kind == AlertKind.SEVERE }.fingerprint
        val otherCity = fingerprint.replace(cityKey, "other-city")
        // the state holding only the OTHER city's storm must not silence this city
        assertTrue(
            evaluate(storm, state = AlertState(severeFingerprints = setOf(otherCity)))
                .any { it.kind == AlertKind.SEVERE }
        )
        // with both recorded (the alternating-cities scenario) this city stays silent
        assertNull(
            evaluate(storm, state = AlertState(severeFingerprints = setOf(otherCity, fingerprint)))
                .find { it.kind == AlertKind.SEVERE }
        )
    }

    // --- precipitation ---

    @Test
    fun `precipitation triggers at threshold within 6h`() {
        val alerts = evaluate(listOf(hour(2, precipPct = 70)))
        val precip = alerts.single { it.kind == AlertKind.PRECIPITATION }
        assertEquals(70, precip.precipPct)
    }

    @Test
    fun `precipitation below threshold or beyond 6h is silent`() {
        assertNull(
            evaluate(listOf(hour(2, precipPct = 69)))
                .find { it.kind == AlertKind.PRECIPITATION }
        )
        assertNull(
            evaluate(listOf(hour(7, precipPct = 90)))
                .find { it.kind == AlertKind.PRECIPITATION }
        )
    }

    @Test
    fun `precipitation dedups per half-day`() {
        val rain = listOf(hour(1, precipPct = 80)) // 10:00 → AM
        val fingerprint = evaluate(rain).single { it.kind == AlertKind.PRECIPITATION }.fingerprint
        assertTrue(fingerprint.endsWith(":AM"))
        assertNull(
            evaluate(rain, state = AlertState(precipFingerprints = setOf(fingerprint)))
                .find { it.kind == AlertKind.PRECIPITATION }
        )
        // afternoon rain is a new half-day bucket → re-fires
        val pmRain = listOf(hour(4, precipPct = 80)) // 13:00 → PM
        assertTrue(
            evaluate(pmRain, state = AlertState(precipFingerprints = setOf(fingerprint)))
                .any { it.kind == AlertKind.PRECIPITATION }
        )
    }

    @Test
    fun `a severe alert suppresses the precipitation warning`() {
        val alerts = evaluate(listOf(hour(2, wmoCode = 95, precipPct = 90)))
        assertTrue(alerts.any { it.kind == AlertKind.SEVERE })
        assertNull(alerts.find { it.kind == AlertKind.PRECIPITATION })
    }

    // --- daily summary ---

    @Test
    fun `summary fires once inside the morning window`() {
        val today = listOf(day(now.toLocalDate()))
        val summary = evaluate(emptyList(), daily = today).single { it.kind == AlertKind.DAILY_SUMMARY }
        assertEquals(now.toLocalDate().toString(), summary.fingerprint)
        // already sent today → silent
        assertNull(
            evaluate(emptyList(), state = AlertState(summaryDate = now.toLocalDate()), daily = today)
                .find { it.kind == AlertKind.DAILY_SUMMARY }
        )
        // sent yesterday → fires again
        assertTrue(
            evaluate(emptyList(), state = AlertState(summaryDate = now.toLocalDate().minusDays(1)), daily = today)
                .any { it.kind == AlertKind.DAILY_SUMMARY }
        )
    }

    @Test
    fun `summary respects the 06-12 window edges`() {
        fun at(hour: Int, minute: Int) = evaluate(
            emptyList(), at = now.withHour(hour).withMinute(minute),
            daily = listOf(day(now.toLocalDate()))
        ).find { it.kind == AlertKind.DAILY_SUMMARY }
        assertNull(at(5, 59))
        assertTrue(at(6, 0) != null)
        assertTrue(at(12, 0) != null)
        assertNull(at(12, 1))
    }

    // --- evening summary ---

    private val evening: LocalDateTime = now.withHour(20)
    private val tomorrow: LocalDate = now.toLocalDate().plusDays(1)
    private val withTomorrow = listOf(day(now.toLocalDate()), day(tomorrow))

    @Test
    fun `the evening summary fires once, and it is about tomorrow`() {
        val alert = evaluate(
            emptyList(), settings = allOn.copy(eveningSummary = true),
            at = evening, daily = withTomorrow
        ).single { it.kind == AlertKind.EVENING_SUMMARY }
        // The fingerprint is the EVENING's date — "has tonight's gone out" — while the
        // numbers are tomorrow's. The two dates are deliberately different values.
        assertEquals(now.toLocalDate().toString(), alert.fingerprint)
        assertEquals(tomorrow, alert.forDate)
        assertEquals(18.0, alert.highC!!, 0.001)
        assertEquals(7.0, alert.lowC!!, 0.001)

        assertNull(
            evaluate(
                emptyList(), settings = allOn.copy(eveningSummary = true),
                state = AlertState(eveningDate = now.toLocalDate()),
                at = evening, daily = withTomorrow
            ).find { it.kind == AlertKind.EVENING_SUMMARY }
        )
    }

    @Test
    fun `the evening summary respects the 18-23 window edges`() {
        fun at(hour: Int, minute: Int) = evaluate(
            emptyList(), settings = allOn.copy(eveningSummary = true),
            at = now.withHour(hour).withMinute(minute), daily = withTomorrow
        ).find { it.kind == AlertKind.EVENING_SUMMARY }
        assertNull(at(17, 59))
        assertTrue(at(18, 0) != null)
        assertTrue(at(23, 0) != null)
        assertNull(at(23, 1))
    }

    @Test
    fun `no tomorrow in the report, no evening summary`() {
        // A cached report that has outlived its own week would otherwise send a
        // sentence made entirely of dashes (DESIGN §1.1).
        assertNull(
            evaluate(
                emptyList(), settings = allOn.copy(eveningSummary = true),
                at = evening, daily = listOf(day(now.toLocalDate()))
            ).find { it.kind == AlertKind.EVENING_SUMMARY }
        )
    }

    @Test
    fun `the two summaries keep separate dedup slots`() {
        // The morning one has already burned today's date by 20:00: a shared slot
        // would mean one summary a day, whichever got there first.
        val both = allOn.copy(dailySummary = true, eveningSummary = true)
        val alerts = evaluate(
            emptyList(), settings = both,
            state = AlertState(summaryDate = now.toLocalDate()),
            at = evening, daily = withTomorrow
        )
        assertTrue(alerts.any { it.kind == AlertKind.EVENING_SUMMARY })
        assertNull(alerts.find { it.kind == AlertKind.DAILY_SUMMARY })
    }

    @Test
    fun `the morning summary never lands in the evening window, and the reverse`() {
        val both = allOn.copy(dailySummary = true, eveningSummary = true)
        assertEquals(
            listOf(AlertKind.EVENING_SUMMARY),
            evaluate(emptyList(), settings = both, at = evening, daily = withTomorrow)
                .map { it.kind }
        )
        assertEquals(
            listOf(AlertKind.DAILY_SUMMARY),
            evaluate(emptyList(), settings = both, at = now, daily = withTomorrow)
                .map { it.kind }
        )
    }

    @Test
    fun `the evening summary has its own switch`() {
        assertNull(
            evaluate(
                emptyList(), settings = allOn.copy(eveningSummary = false),
                at = evening, daily = withTomorrow
            ).find { it.kind == AlertKind.EVENING_SUMMARY }
        )
    }

    // --- gating ---

    @Test
    fun `each toggle gates its own rule`() {
        val stormyRainyMorning = listOf(hour(2, wmoCode = 75, precipPct = 90))
        val none = NotificationSettings(
            severeWeatherAlerts = false, dailySummary = false, precipitationWarning = false,
            eveningSummary = false
        )
        assertTrue(evaluate(stormyRainyMorning, settings = none).isEmpty())

        val precipOnly = none.copy(precipitationWarning = true)
        // severe disabled → no suppression, the rain itself warns
        assertEquals(
            listOf(AlertKind.PRECIPITATION),
            evaluate(stormyRainyMorning, settings = precipOnly).map { it.kind }
        )
    }

    @Test
    fun `empty hourly list produces no severe or precipitation`() {
        val alerts = evaluate(emptyList(), settings = allOn.copy(dailySummary = false))
        assertTrue(alerts.isEmpty())
    }

    // --- arrivals, not weather already under way (23 set 2026) ---

    @Test
    fun `the morning summary needs a row for today, not just a first row`() {
        // A report whose first day is not today describes some other day: its numbers
        // under «Oggi» would be the notification lying.
        assertNull(
            evaluate(emptyList(), daily = listOf(day(now.toLocalDate().plusDays(1))))
                .find { it.kind == AlertKind.DAILY_SUMMARY }
        )
    }

    @Test
    fun `rain already falling is not announced as arriving`() {
        // 08:00 → 11:00 at 80%, polled at 09:00: it is raining, «Ombrello verso le 09»
        // is not news.
        val raining = (-1L..2L).map { hour(it, precipPct = 80) }
        assertNull(evaluate(raining).find { it.kind == AlertKind.PRECIPITATION })
    }

    @Test
    fun `one spell of rain across noon is one warning`() {
        // 10:00 → 13:00: announced at 09:00 as the AM spell…
        val spell = (1L..4L).map { hour(it, precipPct = 80) }
        val first = evaluate(spell).single { it.kind == AlertKind.PRECIPITATION }
        assertTrue(first.fingerprint.endsWith(":AM"))
        // …and at 11:30 the PM half of the SAME spell must not open a second one.
        assertNull(
            evaluate(spell, state = AlertState(precipFingerprints = setOf(first.fingerprint)), at = now.plusHours(2).plusMinutes(30))
                .find { it.kind == AlertKind.PRECIPITATION }
        )
    }

    @Test
    fun `a one-hour dip does not split a spell in two`() {
        val spell = listOf(hour(1, precipPct = 80), hour(2, precipPct = 60), hour(3, precipPct = 80))
        val first = evaluate(spell).single { it.kind == AlertKind.PRECIPITATION }
        assertNull(
            evaluate(spell, state = AlertState(precipFingerprints = setOf(first.fingerprint)), at = now.plusHours(1).plusMinutes(30))
                .find { it.kind == AlertKind.PRECIPITATION }
        )
    }

    @Test
    fun `a storm across midnight is announced once`() {
        // 22:00 → 01:00: announced at 21:00; at 00:30 the hour after midnight carries
        // tomorrow's date, which used to open a fresh fingerprint and a 00:30 heads-up.
        val storm = (13L..16L).map { hour(it, wmoCode = 95, precipPct = 80) }
        val first = evaluate(storm, at = now.plusHours(12)).single { it.kind == AlertKind.SEVERE }
        assertNull(
            evaluate(storm, state = AlertState(severeFingerprints = setOf(first.fingerprint)), at = now.plusHours(15).plusMinutes(30))
                .find { it.kind == AlertKind.SEVERE }
        )
    }

    @Test
    fun `the storm's rain stays silent after the storm has been told`() {
        val storm = listOf(hour(2, wmoCode = 95, precipPct = 90))
        val told = evaluate(storm).single { it.kind == AlertKind.SEVERE }.fingerprint
        // An hour later the storm is burnt, and its own 90% used to post «Ombrello».
        assertNull(
            evaluate(storm, state = AlertState(severeFingerprints = setOf(told)))
                .find { it.kind == AlertKind.PRECIPITATION }
        )
    }

    @Test
    fun `rain that is not the storm's still warns`() {
        // Rain at 11:00, a storm at 17:00: two different things, both worth saying.
        val day = listOf(hour(2, precipPct = 90), hour(8, wmoCode = 95, precipPct = 60))
        val alerts = evaluate(day)
        assertTrue(alerts.any { it.kind == AlertKind.SEVERE })
        // The storm fires first this run and silences precipitation for THIS run only…
        val told = alerts.single { it.kind == AlertKind.SEVERE }.fingerprint
        // …the next run, storm told, the morning rain is its own warning.
        assertTrue(
            evaluate(day, state = AlertState(severeFingerprints = setOf(told)))
                .any { it.kind == AlertKind.PRECIPITATION }
        )
    }

    // --- the heat (14.6, 27 Oct 2026), on Rome's 30 and 31 July 2026 from the archive ---

    /** Feels-like over 35 °C from 10:00 to 19:00 on the 30th, 41.3 at 13:00; the 31st alike. */
    private fun rome(): WeatherReport = report(RomeHeatWave.hours()).let { r ->
        // Rome's own zone: the twelve hours are read by their instants since 15.10, and the
        // sample's New York would put them six hours off the labels.
        r.copy(location = r.location.copy(city = "Roma", timezone = RomeHeatWave.zone.id))
    }

    private val romeMorning: LocalDateTime = LocalDateTime.of(2026, 7, 30, 7, 0)
    private val romeKey = "4190:1250"

    private fun heat(at: LocalDateTime, state: AlertState = AlertState(), report: WeatherReport = rome()) =
        AlertEngine.evaluate(report, allOn, state, at, romeKey).find { it.kind == AlertKind.HEAT }

    @Test
    fun `a heat day is announced once, anchored on its first hour, with its worst feels-like`() {
        val heat = heat(romeMorning)!!
        assertEquals(LocalDateTime.of(2026, 7, 30, 10, 0), heat.at)
        assertEquals(41.3, heat.highC!!, 0.001)
        assertEquals("$romeKey:heat:2026-07-30", heat.fingerprint)
        // Told at seven: a poll at noon, inside the heat, does not say it again…
        val told = AlertState(heatFingerprints = setOf(heat.fingerprint))
        assertNull(heat(romeMorning.withHour(12), told))
        // …and the next day is a new heat day, with a fingerprint of its own.
        val tomorrow = heat(romeMorning.plusDays(1), told)!!
        assertEquals("$romeKey:heat:2026-07-31", tomorrow.fingerprint)
        assertEquals(41.0, tomorrow.highC!!, 0.001)
    }

    @Test
    fun `one hour over the bar is not a heat day`() {
        // From 19:30 the next twelve hours hold one hour over 35, the one under way (19:00, 35.2);
        // the 31st's 10:00 is past the horizon.
        assertNull(heat(LocalDateTime.of(2026, 7, 30, 19, 30)))
        // At 23:00 the twelve hours end with the 31st's 10:00, one hot hour; at midnight they reach
        // its 11:00 too: two hours, tomorrow's day. Until 15.10 the window was thirteen hours on
        // the hour, and 23:00 already counted both.
        assertNull(heat(LocalDateTime.of(2026, 7, 30, 23, 0)))
        assertEquals("$romeKey:heat:2026-07-31", heat(LocalDateTime.of(2026, 7, 31, 0, 0))!!.fingerprint)
    }

    /** The heat counts hours, it does not announce an arrival (15.10, 29 Oct 2026). */
    @Test
    fun `the hour under way is one of the twelve`() {
        // At 18:30 the hot hours ahead are 18:00, under way, and 19:00: two, where the window that
        // left the hour under way out counted one and said nothing.
        val evening = heat(LocalDateTime.of(2026, 7, 30, 18, 30))!!
        assertEquals(LocalDateTime.of(2026, 7, 30, 18, 0), evening.at)
        assertEquals("$romeKey:heat:2026-07-30", evening.fingerprint)
    }

    @Test
    fun `a row without a feels-like is never hot`() {
        // A cache from before the rows carried one (24 set 2026): silence, not a zero.
        val bare = rome().let { r -> r.copy(hourly = r.hourly.map { it.copy(feelsLikeC = null) }) }
        assertNull(heat(romeMorning, report = bare))
    }

    @Test
    fun `the heat rides on the severe switch and silences nothing`() {
        val off = allOn.copy(severeWeatherAlerts = false)
        assertNull(AlertEngine.evaluate(rome(), off, AlertState(), romeMorning, romeKey).find { it.kind == AlertKind.HEAT })
        // A hot day with rain at nine: both are news, and the heat is not the storm's rain.
        val wet = rome().let { r ->
            r.copy(hourly = r.hourly.map { if (it.time.hour == 9 && it.time.dayOfMonth == 30) it.copy(precipChancePct = 80) else it })
        }
        val kinds = AlertEngine.evaluate(wet, allOn, AlertState(), romeMorning, romeKey).map { it.kind }
        assertTrue(kinds.contains(AlertKind.HEAT))
        assertTrue(kinds.contains(AlertKind.PRECIPITATION))
    }

    // --- the wind (14.7, 27 Oct 2026): a bora at Trieste and a libeccio at Genoa, from the archive ---

    private val triesteKey = "4565:1378"
    private val genoaKey = "4441:895"

    /** Trieste at [elevationM]: 18 m is its own; 1 700 m is the same hours in the mountains. */
    private fun trieste(elevationM: Double? = WinterGales.Trieste.elevationM): WeatherReport =
        report(WinterGales.Trieste.hours()).let { r ->
            r.copy(location = r.location.copy(city = "Trieste", elevationM = elevationM))
        }

    private fun genoa(): WeatherReport = report(WinterGales.Genoa.hours()).let { r ->
        r.copy(location = r.location.copy(city = "Genova", elevationM = WinterGales.Genoa.elevationM))
    }

    private fun severe(report: WeatherReport, at: LocalDateTime, key: String, state: AlertState = AlertState()) =
        AlertEngine.evaluate(report, allOn, state, at, key).find { it.kind == AlertKind.SEVERE }

    @Test
    fun `the gust bar follows the place's elevation, and no elevation is the lowland's`() {
        assertEquals(70.0, AlertEngine.windGustBar(null), 0.0)
        assertEquals(70.0, AlertEngine.windGustBar(18.0), 0.0)
        assertEquals(70.0, AlertEngine.windGustBar(1599.9), 0.0)
        assertEquals(100.0, AlertEngine.windGustBar(1600.0), 0.0)
        assertEquals(100.0, AlertEngine.windGustBar(2240.0), 0.0)
    }

    @Test
    fun `a bora is announced once, at the hour it reaches the bar, with its strongest gust`() {
        // The evening of the 23rd: 65.2 km/h at 23:00 is under the bar, 73.4 at midnight over it.
        val evening = LocalDateTime.of(2025, 12, 23, 18, 0)
        val bora = severe(trieste(), evening, triesteKey)!!
        assertEquals(AlertEngine.SevereBucket.WIND, bora.bucket)
        assertEquals(LocalDateTime.of(2025, 12, 24, 0, 0), bora.at)
        assertEquals(112.0, bora.gustKph!!, 0.001)
        assertEquals("$triesteKey:sev:WIND:2025-12-24", bora.fingerprint)
        // Told: a poll inside the gale does not say it again…
        val told = AlertState(severeFingerprints = setOf(bora.fingerprint))
        assertNull(severe(trieste(), LocalDateTime.of(2025, 12, 24, 8, 0), triesteKey, told))
        // …and a gale that runs into the next day is one gale, not a second arrival.
        assertNull(severe(trieste(), LocalDateTime.of(2025, 12, 25, 0, 0), triesteKey, AlertState()))
    }

    @Test
    fun `in the mountains the same bora is a hazard only from 100 km per hour`() {
        val evening = LocalDateTime.of(2025, 12, 23, 18, 0)
        // At 1 700 m the first hour over the bar is 05:00 on the 24th, 103.7 km/h.
        val high = severe(trieste(elevationM = 1700.0), evening, triesteKey)!!
        assertEquals(LocalDateTime.of(2025, 12, 24, 5, 0), high.at)
        assertEquals(112.0, high.gustKph!!, 0.001)
        // Four hours earlier the 05:00 hour is past the horizon, and nothing rings.
        assertNull(severe(trieste(elevationM = 1700.0), evening.minusHours(4), triesteKey))
        // No elevation at all is the lowland bar, never a guess.
        assertEquals(LocalDateTime.of(2025, 12, 24, 0, 0), severe(trieste(elevationM = null), evening, triesteKey)!!.at)
    }

    @Test
    fun `a row without a gust is never a gale`() {
        val bare = trieste().let { r -> r.copy(hourly = r.hourly.map { it.copy(gustKph = null) }) }
        assertNull(severe(bare, LocalDateTime.of(2025, 12, 23, 18, 0), triesteKey))
    }

    @Test
    fun `the code's hazard outranks the gust in the same hour`() {
        val squall = WinterGales.Trieste.hours()[32].copy(condition = WeatherCondition(95), precipChancePct = 60)
        assertEquals(112.0, squall.gustKph!!, 0.001)
        assertEquals(AlertEngine.SevereBucket.THUNDER, AlertEngine.severeBucket(squall, 18.0))
        assertEquals(AlertEngine.SevereBucket.WIND, AlertEngine.severeBucket(squall.copy(precipChancePct = 10), 18.0))
    }

    @Test
    fun `the libeccio rings for its one hour over the bar, after the storm's own alert`() {
        // 22 March, 20:00: the thunderstorm at 22:00 is the first arrival…
        val night = LocalDateTime.of(2025, 3, 22, 20, 0)
        val storm = severe(genoa(), night, genoaKey)!!
        assertEquals(AlertEngine.SevereBucket.THUNDER, storm.bucket)
        assertEquals(LocalDateTime.of(2025, 3, 22, 22, 0), storm.at)
        assertNull(storm.gustKph)
        // …and, that one told and its hour gone by, the libeccio at 01:00 (71.6 km/h between
        // a 59.8 and a 52.2) is the next: a told arrival still holds the horizon while it is
        // ahead, as a storm's does, so the poll at 23:00 is the one that sees the wind.
        val told = AlertState(severeFingerprints = setOf(storm.fingerprint))
        assertNull(severe(genoa(), night, genoaKey, told))
        val gale = severe(genoa(), night.plusHours(3), genoaKey, told)!!
        assertEquals(AlertEngine.SevereBucket.WIND, gale.bucket)
        assertEquals(LocalDateTime.of(2025, 3, 23, 1, 0), gale.at)
        assertEquals(71.6, gale.gustKph!!, 0.001)
        assertEquals("$genoaKey:sev:WIND:2025-03-23", gale.fingerprint)
    }

    @Test
    fun `the gale rides the severe switch and covers the rain beside it`() {
        val evening = LocalDateTime.of(2025, 12, 23, 18, 0)
        val off = allOn.copy(severeWeatherAlerts = false)
        val quiet = AlertEngine.evaluate(trieste(), off, AlertState(), evening, triesteKey)
        assertNull(quiet.find { it.kind == AlertKind.SEVERE })
        // The run that tells the gale tells nothing else, as a storm's does: a severe alert
        // covers its own run, and the rain at 19:00 (73%) waits for the next poll.
        val kinds = AlertEngine.evaluate(trieste(), allOn, AlertState(), evening, triesteKey).map { it.kind }
        assertEquals(listOf(AlertKind.SEVERE), kinds.filter { it == AlertKind.SEVERE || it == AlertKind.PRECIPITATION })
        // Inside the gale (the 24th at 08:00, rain at 98% from 09:00) the rain is the gale's,
        // as it would be a storm's: one notification per perturbation.
        val inside = AlertEngine.evaluate(
            trieste(), allOn, AlertState(severeFingerprints = setOf("$triesteKey:sev:WIND:2025-12-24")),
            LocalDateTime.of(2025, 12, 24, 8, 0), triesteKey
        )
        assertNull(inside.find { it.kind == AlertKind.PRECIPITATION })
    }
}
