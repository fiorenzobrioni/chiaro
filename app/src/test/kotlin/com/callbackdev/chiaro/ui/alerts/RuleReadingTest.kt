package com.callbackdev.chiaro.ui.alerts

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.domain.WeatherNow
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.model.DailyForecast
import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.model.WeatherCondition
import com.callbackdev.chiaro.domain.rules.NotificationRule
import com.callbackdev.chiaro.domain.rules.RuleCondition
import com.callbackdev.chiaro.domain.rules.RuleOp
import com.callbackdev.chiaro.domain.sample.sampleWeatherReport
import com.callbackdev.chiaro.domain.settings.UnitSettings
import com.callbackdev.chiaro.ui.today.TimelineKind
import com.callbackdev.chiaro.ui.today.TodayStateBuilder
import com.callbackdev.chiaro.ui.today.TodayUiState
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The Alerts card and Today's agenda row say the same thing about the same rule on the same
 * report (15.1, 28 Oct 2026). The case is the one that showed they did not: a cache fetched
 * yesterday at 10:00, read today at 10:30, whose `current` block is yesterday's morning and
 * whose first daily row is yesterday.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "it")
class RuleReadingTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val zone = ZoneId.of("Europe/Rome")
    private val clear = WeatherCondition(0)
    private val yesterday = LocalDate.of(2026, 10, 27)
    private val today = yesterday.plusDays(1)
    private val milan = City(
        id = 1L, name = "Milano", region = "Lombardia", country = "Italia",
        coordinates = Coordinates(45.4643, 9.1895), timezone = "Europe/Rome"
    )
    private val fetchedAt: Instant = yesterday.atTime(10, 0).atZone(zone).toInstant()
    private val now: Instant = today.atTime(10, 30).atZone(zone).toInstant()
    private val local: LocalDateTime = now.atZone(zone).toLocalDateTime()

    private fun row(start: LocalDateTime, temp: Double) = HourlyForecast(
        time = start,
        at = start.atZone(zone).toInstant(),
        tempC = temp,
        condition = clear,
        precipChancePct = 10,
        cloudCoverPct = 20,
        feelsLikeC = temp - 1,
        humidityPct = 60,
        dewPointC = 8.0,
        pressureMb = 1015.0,
        windKph = 10.0,
        windDirectionDeg = 90,
        gustKph = 20.0
    )

    /** 25° at the fetch and a high of 30 that day; today's rows say 12° and a high of 20. */
    private val report = sampleWeatherReport().let { sample ->
        sample.copy(
            location = sample.location.copy(
                city = "Milano", coordinates = milan.coordinates, timezone = "Europe/Rome",
                localTime = yesterday.atTime(10, 0)
            ),
            current = sample.current.copy(tempC = 25.0),
            hourly = (0 until 48).map { h ->
                val start = yesterday.atTime(10, 0).plusHours(h.toLong())
                row(start, if (start.toLocalDate() == today) 12.0 else 25.0)
            },
            daily = listOf(
                DailyForecast(yesterday, 30.0, 18.0, clear, 0, 5),
                DailyForecast(today, 20.0, 9.0, clear, 10, 3),
                DailyForecast(today.plusDays(1), 18.0, 8.0, clear, 20, 3)
            ),
            systemInfo = sample.systemInfo.copy(lastSync = fetchedAt)
        )
    }

    private fun rule(id: Long, variable: String, threshold: Double) = NotificationRule(
        id = id, name = "rule_$id", conditions = listOf(RuleCondition(variable, RuleOp.GTE, threshold)), message = "msg"
    )

    /** What the card says: the dry run on the report prepared for now, as the page reads it. */
    private fun card(rule: NotificationRule): RulePreview =
        ruleReading(rule, WeatherNow.prepare(report, now)!!, zone, local, UnitSettings(), context)

    /** Whether Today's agenda has a row for the rule, on the same raw report at the same minute. */
    private fun agendaHolds(rule: NotificationRule): Boolean {
        val state = TodayStateBuilder.build(
            city = milan, report = report, now = now, updateFrequencyMin = 60,
            userRefreshing = false, error = null, rules = listOf(rule), readQuarters = false
        ) as TodayUiState.Content
        return state.timeline.any { it.kind == TimelineKind.ALERT && it.rule?.rule?.id == rule.id }
    }

    @Test
    fun `the high of today is today's row, on the card as in the agenda`() {
        // Yesterday's high (30) passes the bar, today's (20) does not: the card used to say
        // «Adesso sì» off the row the cache leads with, while Today's agenda had no row.
        val hot = rule(1, "today.high_c", 25.0)
        val now = card(hot)
        assertTrue(now is RulePreview.WouldPass)
        assertEquals(20.0, (now as RulePreview.WouldPass).value!!, 1e-9)
        assertEquals(false, agendaHolds(hot))
        // And a bar today's high clears reads as a yes on both.
        val mild = rule(2, "today.high_c", 15.0)
        assertTrue(card(mild) is RulePreview.WouldFire)
        assertEquals(true, agendaHolds(mild))
    }

    @Test
    fun `the temperature now is the estimate for now, on the card as in the agenda`() {
        // The fetched block says 25°; the forecast for this hour says 12°, and so does the hero.
        val warm = rule(3, "current.temp_c", 20.0)
        val now = card(warm)
        assertTrue(now is RulePreview.WouldPass)
        assertEquals(12.0, (now as RulePreview.WouldPass).value!!, 1e-9)
        assertEquals(false, agendaHolds(warm))
        val cool = rule(4, "current.temp_c", 10.0)
        assertTrue(card(cool) is RulePreview.WouldFire)
        assertEquals(true, agendaHolds(cool))
    }
}
