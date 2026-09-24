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
        condition = WeatherCondition(wmoCode, "desc-$wmoCode", "⛅"),
        precipChancePct = precipPct,
        cloudCoverPct = 50
    )

    /** The sample's own daily block starts three days out; the evening summary asks
     * for TOMORROW by date, so these tests say which days the report carries. */
    private fun day(date: LocalDate) = DailyForecast(
        date = date,
        highC = 18.0,
        lowC = 7.0,
        condition = WeatherCondition(2, "Partly Cloudy", "⛅"),
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
}

