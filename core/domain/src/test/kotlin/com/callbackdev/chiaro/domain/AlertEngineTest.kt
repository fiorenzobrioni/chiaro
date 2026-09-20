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
        severeWeatherAlerts = true, dailySummary = true, precipitationWarning = true
    )

    private fun hour(plusHours: Long, wmoCode: Int = 2, precipPct: Int = 0) = HourlyForecast(
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
        uvIndexMax = 4,
        uvDescription = "Moderate"
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
        val summary = evaluate(emptyList()).single { it.kind == AlertKind.DAILY_SUMMARY }
        assertEquals(now.toLocalDate().toString(), summary.fingerprint)
        // already sent today → silent
        assertNull(
            evaluate(emptyList(), state = AlertState(summaryDate = now.toLocalDate()))
                .find { it.kind == AlertKind.DAILY_SUMMARY }
        )
        // sent yesterday → fires again
        assertTrue(
            evaluate(emptyList(), state = AlertState(summaryDate = now.toLocalDate().minusDays(1)))
                .any { it.kind == AlertKind.DAILY_SUMMARY }
        )
    }

    @Test
    fun `summary respects the 06-12 window edges`() {
        fun at(hour: Int, minute: Int) = evaluate(
            emptyList(), at = now.withHour(hour).withMinute(minute)
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
            severeWeatherAlerts = false, dailySummary = false, precipitationWarning = false
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
}
