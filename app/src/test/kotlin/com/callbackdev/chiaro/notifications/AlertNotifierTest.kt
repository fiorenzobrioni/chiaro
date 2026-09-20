package com.callbackdev.chiaro.notifications

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.domain.Alert
import com.callbackdev.chiaro.domain.AlertKind
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.model.DailyForecast
import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.model.WeatherCondition
import com.callbackdev.chiaro.domain.model.WeatherReport
import com.callbackdev.chiaro.domain.sample.sampleWeatherReport
import com.callbackdev.chiaro.domain.settings.UnitSettings
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * The built-in alerts' two bodies. The rule this file exists to hold (Fase 6b, and
 * the reason the evening summary of 21 set 2026 was written this way) is that
 * **pulling a notification open has to give back something it was not already
 * saying**: collapsed is the sentence, expanded is that sentence plus the story.
 *
 * Asserted for every kind, so a fifth one cannot ship as a headline with nothing
 * under it — and so the evening summary cannot quietly become a copy of its morning
 * twin, which is the one way the pair would stop being worth having.
 *
 * The clock is asserted by shape, not by value: the notifier follows the device's
 * 12/24-hour setting, which is the reader's and not the test's to fix.
 */
@RunWith(RobolectricTestRunner::class)
class AlertNotifierTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val manager = context.getSystemService(NotificationManager::class.java)

    private val today = LocalDate.of(2026, 1, 15)
    private val tomorrow = today.plusDays(1)
    private val cloudy = WeatherCondition(3, "Overcast", "☁️")
    private val zone: ZoneId = ZoneId.of("Europe/Rome")
    private val milan = Coordinates(45.4642, 9.1900)

    /** Milan, so the sun has a sunrise to have and the night is a real one. */
    private fun report(): WeatherReport = sampleWeatherReport().let { sample ->
        sample.copy(
            // The mapper computes this block with AstronomyEngine for the report's own
            // day and place; a fixture that kept the sample's New-York-in-October
            // daylight would make the change against today a number off by an hour.
            astronomical = sample.astronomical.copy(
                daylightDuration = EveningDetails.sun(today, zone, milan, today = null)!!.daylight
            ),
            location = sample.location.copy(
                city = "Milano",
                coordinates = milan,
                timezone = zone.id,
                localTime = today.atTime(20, 0)
            ),
            // 20:00 tonight → 23:00 tomorrow. It falls to −1 at 04:00, and rains hard
            // tomorrow afternoon; this morning's hours are gone, as in a real report
            // the worker never trims.
            hourly = (0..27).map { i ->
                val time = today.atTime(20, 0).plusHours(i.toLong())
                HourlyForecast(
                    time = time,
                    at = time.atZone(zone).toInstant(),
                    tempC = nightAndDay[i],
                    condition = cloudy,
                    precipChancePct = when (time.hour) {
                        in 14..16 -> if (time.toLocalDate() == tomorrow) 80 else 10
                        else -> 10
                    },
                    cloudCoverPct = 80
                )
            },
            daily = listOf(
                DailyForecast(today, 6.0, 1.0, cloudy, 10, 1, "Low"),
                DailyForecast(tomorrow, 9.0, -1.0, cloudy, 80, 5, "Moderate")
            )
        )
    }

    /** 20:00 → 23:00 next day: down to −1 before dawn, up to 9 in the afternoon. */
    private val nightAndDay = listOf(
        4.0, 3.0, 2.0, 1.0, 0.5, 0.0, -0.5, -1.0, -0.5, 0.0, // 20:00 → 05:00
        1.0, 2.0, 4.0, 6.0, 7.0, 8.0, 9.0, 9.0, 8.0, 7.0, //    06:00 → 15:00
        6.0, 5.0, 4.0, 4.0, 3.0, 3.0, 2.0, 2.0 //                16:00 → 23:00
    )

    private fun evening(): Alert = Alert(
        kind = AlertKind.EVENING_SUMMARY,
        fingerprint = today.toString(),
        cityLabel = "Milano",
        condition = cloudy,
        precipPct = 80,
        highC = 9.0,
        lowC = -1.0,
        forDate = tomorrow
    )

    private fun post(alert: Alert, report: WeatherReport = report()): Boolean {
        manager.cancelAll()
        return AlertNotifier.notify(context, alert, report, UnitSettings())
    }

    private fun extras() = shadowOf(manager).allNotifications.last().extras
    private fun title() = extras().getString(Notification.EXTRA_TITLE).orEmpty()
    private fun collapsed() = extras().getString(Notification.EXTRA_TEXT).orEmpty()
    private fun expanded() = extras().getCharSequence(Notification.EXTRA_BIG_TEXT).toString()

    // --- the rule that holds for every kind ---

    @Test
    fun `every kind says more when it is opened than when it is not`() {
        val hour = today.atTime(22, 0)
        val kinds = listOf(
            Alert(AlertKind.SEVERE, "f", "Milano", cloudy, at = hour, precipPct = 90),
            Alert(AlertKind.PRECIPITATION, "f", "Milano", cloudy, at = hour, precipPct = 80),
            Alert(
                AlertKind.DAILY_SUMMARY, "f", "Milano", cloudy,
                precipPct = 10, highC = 6.0, lowC = 1.0, forDate = today
            ),
            evening()
        )
        for (alert in kinds) {
            assertTrue(alert.kind.name, post(alert))
            val short = collapsed()
            val long = expanded()
            assertTrue(alert.kind.name, short.isNotBlank())
            assertTrue("${alert.kind}: $long", long.startsWith(short))
            assertTrue("${alert.kind} adds nothing when opened", long.length > short.length)
            assertTrue("${alert.kind} has no second line", long.contains("\n"))
        }
    }

    @Test
    fun `every built-in sentence ends as a sentence`() {
        val hour = today.atTime(22, 0)
        val alerts = listOf(
            Alert(AlertKind.SEVERE, "f", "Milano", cloudy, at = hour, precipPct = 90),
            Alert(AlertKind.SEVERE, "f", "Milano", cloudy, at = hour, precipPct = null),
            Alert(AlertKind.PRECIPITATION, "f", "Milano", cloudy, at = hour, precipPct = 80),
            evening()
        )
        for (alert in alerts) {
            post(alert)
            assertTrue("${alert.kind}: ${collapsed()}", collapsed().endsWith("."))
        }
    }

    @Test
    fun `an alert with no hour picks a sentence that does not need one`() {
        // Unreachable from the engine, which anchors both on an hour it has read — and
        // the reason the stem-plus-fragment shape had to go: it printed "Overcast
        // around" and "rain at 0%" instead of a sentence.
        post(Alert(AlertKind.SEVERE, "f", "Milano", cloudy, at = null, precipPct = 90))
        assertEquals("Overcast on the way.", collapsed())
        post(Alert(AlertKind.PRECIPITATION, "f", "Milano", cloudy, at = null, precipPct = null))
        assertEquals("Rain likely in the next hours.", collapsed())
        assertFalse(collapsed(), collapsed().contains("0%"))
    }

    // --- the evening summary ---

    @Test
    fun `the title is the only thing separating the two summaries`() {
        post(evening())
        assertEquals("Tomorrow · Milano", title())
        val eveningLine = collapsed()

        post(
            Alert(
                AlertKind.DAILY_SUMMARY, "f", "Milano", cloudy,
                precipPct = 80, highC = 9.0, lowC = -1.0, forDate = today
            )
        )
        assertEquals("Today · Milano", title())
        // Same numbers, same sentence: the twins rhyme on purpose, and the day they
        // are about is said once, in the title.
        assertEquals(eveningLine, collapsed())
    }

    @Test
    fun `the collapsed line is tomorrow, not tonight`() {
        post(evening())
        val text = collapsed()
        assertTrue(text, text.startsWith("Overcast."))
        assertTrue(text, text.contains("Low -1°"))
        assertTrue(text, text.contains("high 9°"))
        assertTrue(text, text.contains("rain 80%"))
    }

    @Test
    fun `opened, it carries the night and tomorrow's own facts`() {
        post(evening())
        val lines = expanded().lines()
        // The night, read off tonight's hours and not this morning's
        assertTrue(expanded(), lines.any { it.startsWith("Overnight down to -1°") })
        assertTrue(expanded(), lines.any { it.endsWith("Freezing: ice on the glass by morning") })
        // Tomorrow's umbrella window, with its peak
        assertTrue(expanded(), lines.any { it.startsWith("Tomorrow rain from ") && it.endsWith("up to 80%") })
        // The daylight edition's own line: two ends and the change against today
        assertTrue(expanded(), lines.any { it.startsWith("Tomorrow sunrise ") })
        assertTrue(expanded(), lines.any { it.contains("1 minute more light than today") })
        // Tomorrow's UV, which at 5 asks something of the reader
        assertTrue(expanded(), lines.any { it.startsWith("Tomorrow's peak UV 5") })
    }

    @Test
    fun `the daylight line turns around with the year`() {
        // Same sun, a longer today: the line has to say "less", and nothing else in
        // it changes. Milan on 16 January gains a minute; a July evening loses four.
        val shrinking = report().let { base ->
            base.copy(
                astronomical = base.astronomical.copy(
                    daylightDuration = base.astronomical.daylightDuration!!.plusMinutes(4)
                )
            )
        }
        post(evening(), shrinking)
        assertTrue(expanded(), expanded().contains("2 minutes less light than today"))
    }

    @Test
    fun `a quiet night and a low sun leave their lines out rather than print nothing`() {
        // Rain the provider never forecast, a UV nobody has to act on: §1.1 — the
        // line is not drawn, and no zero stands in for it.
        val quiet = report().let { base ->
            base.copy(
                hourly = base.hourly.map { it.copy(precipChancePct = null) },
                daily = base.daily.map {
                    if (it.date == tomorrow) it.copy(uvIndexMax = 1, precipPct = null) else it
                }
            )
        }
        post(evening(), quiet)
        val text = expanded()
        assertFalse(text, text.contains("Rain overnight"))
        assertFalse(text, text.contains("Tomorrow rain"))
        assertFalse(text, text.contains("peak UV"))
        // No line under the headline invents a percentage nobody forecast. The
        // headline's own 80% comes from the alert, which the engine built while the
        // day still carried one.
        assertFalse(text, text.substringAfter("\n\n").contains("%"))
        // What is left still says more than the collapsed line
        assertTrue(text, text.contains("Overnight down to"))
        assertTrue(text, text.contains("Tomorrow sunrise"))
    }

    @Test
    fun `a night that only threatens rain says so, a dry-looking one stays quiet`() {
        fun nightAt(pct: Int): String {
            val r = report().let { base ->
                base.copy(
                    hourly = base.hourly.map {
                        if (it.time.toLocalDate() == today) it.copy(precipChancePct = pct) else it
                    }
                )
            }
            post(evening(), r)
            return expanded()
        }
        assertFalse(nightAt(49).contains("Rain overnight"))
        assertTrue(nightAt(50).contains("Rain overnight up to 50%"))
    }

    @Test
    fun `the provider's English descriptions never reach the notification`() {
        post(evening())
        // `WeatherCondition.description` is tweather's JSON vocabulary, not Chiaro's:
        // the screen says "Overcast" because a string resource does, and "☁️" is an
        // emoji, which is never iconography here (CLAUDE.md).
        assertFalse(expanded(), expanded().contains("☁️"))
        assertFalse(title(), title().contains("☁️"))
    }
}
