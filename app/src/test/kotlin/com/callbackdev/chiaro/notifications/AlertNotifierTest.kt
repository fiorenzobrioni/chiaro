package com.callbackdev.chiaro.notifications

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.Alert
import com.callbackdev.chiaro.domain.AlertEngine
import com.callbackdev.chiaro.domain.AlertKind
import com.callbackdev.chiaro.domain.DayPart
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.model.DailyForecast
import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.model.WeatherCondition
import com.callbackdev.chiaro.domain.model.WeatherReport
import com.callbackdev.chiaro.domain.sample.sampleWeatherReport
import com.callbackdev.chiaro.domain.settings.UnitSettings
import com.callbackdev.chiaro.ui.format.Formats
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
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
    private val cloudy = WeatherCondition(3)
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
                val wet = time.hour in 14..16 && time.toLocalDate() == tomorrow
                HourlyForecast(
                    time = time,
                    at = time.atZone(zone).toInstant(),
                    tempC = nightAndDay[i],
                    // The afternoon's rain is rain for the engine too: the umbrella is a wet
                    // hour at 70% or more, not a chance alone (17 nov 2026).
                    condition = if (wet) WeatherCondition(63) else cloudy,
                    precipChancePct = if (wet) 80 else 10,
                    cloudCoverPct = 80
                )
            },
            daily = listOf(
                DailyForecast(today, 6.0, 1.0, cloudy, 10, 1),
                DailyForecast(tomorrow, 9.0, -1.0, cloudy, 80, 5)
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

    // --- what falls is said (17 nov 2026): the hour's own state, not «rain» for everything ---

    @Test
    fun `rain on the way is the umbrella, snow on the way is snow`() {
        val hour = tomorrow.atTime(14, 0)
        post(Alert(AlertKind.PRECIPITATION, "f", "Milano", WeatherCondition(63), at = hour, precipPct = 80))
        assertEquals("Rain on the way · Milano", title())
        assertTrue(collapsed(), collapsed().startsWith("Umbrella around ") && collapsed().endsWith("rain at 80%."))
        // Bormio in January: «Umbrella around 2: rain at 80%» was said over an hour of snow.
        post(Alert(AlertKind.PRECIPITATION, "f", "Bormio", WeatherCondition(73), at = hour, precipPct = 80))
        assertEquals("Snow on the way · Bormio", title())
        assertTrue(collapsed(), collapsed().startsWith("Snow around ") && collapsed().endsWith(", at 80%."))
        post(Alert(AlertKind.PRECIPITATION, "f", "Bormio", WeatherCondition(69), at = hour, precipPct = 75))
        assertEquals("Rain and snow on the way · Bormio", title())
        assertTrue(collapsed(), collapsed().startsWith("Rain and snow around ") && collapsed().endsWith(", at 75%."))
        // With no hour to name the sentence still says what falls.
        post(Alert(AlertKind.PRECIPITATION, "f", "Bormio", WeatherCondition(71), at = null, precipPct = null))
        assertEquals("Snow likely in the next hours.", collapsed())
    }

    @Test
    fun `a channel made under its old name takes the new one at the next alert`() {
        // An install from before 17 nov 2026 has the channel as «Incoming rain». The notifier
        // creates the channel again at every alert, under the same id, and the platform takes
        // that as a rename: the id stays, and with it what the reader set on the channel.
        val id = AlertNotifier.channelId(AlertKind.PRECIPITATION)
        manager.createNotificationChannel(
            android.app.NotificationChannel(id, "Incoming rain", NotificationManager.IMPORTANCE_DEFAULT)
        )
        post(Alert(AlertKind.PRECIPITATION, "f", "Milano", WeatherCondition(73), at = tomorrow.atTime(14, 0), precipPct = 80))
        assertEquals("Incoming rain or snow", manager.getNotificationChannel(id).name.toString())
        assertEquals(1, manager.notificationChannels.count { it.id == id })
    }

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

    /** 17.1, 19 nov 2026: the chance printed in «Maltempo» is the rain's, and is said only
     * where the hazard is the rain. A storm rings at any chance since 17 nov, and its body
     * read «Thunderstorm around 4:00 PM, rain at 8%.»; over heavy snow it said «rain». */
    @Test
    fun `a severe alert says the chance of rain only when the hazard is the rain`() {
        val hour = tomorrow.atTime(14, 0)
        fun severe(code: Int, bucket: AlertEngine.SevereBucket, pct: Int) =
            Alert(AlertKind.SEVERE, "f", "Milano", WeatherCondition(code), at = hour, precipPct = pct, bucket = bucket)
        post(severe(95, AlertEngine.SevereBucket.THUNDER, 8))
        assertTrue(collapsed(), collapsed().startsWith("Thunderstorm around ") && !collapsed().contains("%"))
        assertTrue(expanded(), expanded().lines().none { it.startsWith("Rain up to ") })
        post(severe(75, AlertEngine.SevereBucket.SNOW, 90))
        assertTrue(collapsed(), !collapsed().contains("rain") && !collapsed().contains("%"))
        assertTrue(expanded(), expanded().lines().none { it.startsWith("Rain up to ") })
        post(severe(66, AlertEngine.SevereBucket.ICE, 40))
        assertTrue(collapsed(), !collapsed().contains("%"))
        post(severe(65, AlertEngine.SevereBucket.RAIN, 70))
        assertTrue(collapsed(), collapsed().endsWith("rain at 70%."))
    }

    @Test
    fun `an umbrella for snow does not say the chance as the rain's`() {
        post(Alert(AlertKind.PRECIPITATION, "f", "Bormio", WeatherCondition(73), at = tomorrow.atTime(14, 0), precipPct = 80))
        assertTrue(expanded(), expanded().lines().none { it.startsWith("Rain up to ") })
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
        // Same report, same sentence: the twins speak with one voice, the day's own
        // sentence (21 ott 2026), and the day they are about is said once, in the title.
        assertEquals(eveningLine, collapsed())
    }

    /** The collapsed line is the day's sentence, the one Oggi's hero says (21 ott 2026):
     * at 20:00 with −1° at 04:00 the ladder says the frost by morning, and the numbers
     * follow it, opened — the model's code and its numbers no longer lead. */
    @Test
    fun `the collapsed line is the day's sentence, and the numbers follow it`() {
        post(evening())
        val text = collapsed()
        assertTrue(text, text.startsWith("Frost around "))
        assertTrue(text, text.endsWith("low -1°."))
        assertFalse(text, text.contains("Overcast"))
        val lines = expanded().lines()
        assertEquals(text, lines[0])
        assertEquals("Low -1°, high 9°.", lines[2])
    }

    /** With no sentence to say (a day the ladder and the quiet day's own sentence have
     * nothing on), the factual line stays — and its rain clause only from the headline's
     * «possible» floor: «rain 10%» and «rain 0%» are numbers nobody acts on. */
    @Test
    fun `a day with no sentence keeps the factual line, without a rain clause under the floor`() {
        // A report whose hours have all elapsed covers no «now»: Oggi shows nothing of it,
        // and the summary falls back to its facts.
        val spent = report().let { base ->
            base.copy(location = base.location.copy(localTime = tomorrow.plusDays(2).atTime(20, 0)))
        }
        post(evening().copy(precipPct = 10), spent)
        assertEquals("Overcast. Low -1°, high 9°.", collapsed())
        post(evening().copy(precipPct = 0), spent)
        assertEquals("Overcast. Low -1°, high 9°.", collapsed())
        post(evening().copy(precipPct = 50), spent)
        assertEquals("Overcast. Low -1°, high 9°, rain 50%.", collapsed())
        post(evening().copy(precipPct = null), spent)
        assertEquals("Overcast. Low -1°, high 9°.", collapsed())
    }

    /** Opened with a picture, the sentence stands alone under the title and the picture
     * labels the low and the high where they happen; the long text, with no picture, says
     * them as its first line. */
    @Test
    fun `opened, the sentence stands alone over the picture and the range is in the long text`() {
        post(evening())
        val body = posted().bigContentView.apply(context, android.widget.FrameLayout(context))
        val headline = body.findViewById<android.widget.TextView>(com.callbackdev.chiaro.R.id.notif_headline).text.toString()
        assertEquals(collapsed(), headline)
        assertEquals("Low -1°, high 9°.", expanded().lines()[2])
        assertTrue(picturedDetails().toString(), picturedDetails().none { it.startsWith("Low ") })
    }

    @Test
    fun `opened, it carries the night and tomorrow's own facts`() {
        post(evening())
        val lines = expanded().lines()
        // The night, read off tonight's hours and not this morning's
        assertTrue(expanded(), lines.any { it.startsWith("Tonight a low of -1°") })
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
    fun `tomorrow's UV on a wet day burns in the sunny spells`() {
        // The peak of a day of rain is the hour the sun comes out (13 ott 2026): the
        // number stays the model's, the time is said for that hour.
        val wet = report().let { base ->
            base.copy(
                daily = base.daily.map {
                    if (it.date == tomorrow) it.copy(condition = WeatherCondition(61)) else it
                }
            )
        }
        post(evening(), wet)
        assertTrue(
            expanded(),
            expanded().lines().any { it == "Tomorrow's peak UV 5 · In sunny spells, burns in about 45 minutes" }
        )
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
        // alert's own 80% was built by the engine while the day still carried one, and
        // is not said: the sentence leads, and the range under it has no chance in it.
        assertFalse(text, text.substringAfter("\n\n").contains("%"))
        // What is left still says more than the collapsed line
        assertTrue(text, text.contains("Tonight a low of"))
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

    // --- the picture (23 set 2026) ---

    private fun posted() = shadowOf(manager).allNotifications.last()

    @Test
    fun `every built-in kind carries its picture and keeps its whole text`() {
        val hour = tomorrow.atTime(14, 0)
        val kinds = listOf(
            Alert(AlertKind.SEVERE, "f", "Milano", cloudy, at = hour, precipPct = 90),
            Alert(AlertKind.PRECIPITATION, "f", "Milano", cloudy, at = hour, precipPct = 80),
            evening()
        )
        for (alert in kinds) {
            post(alert)
            assertTrue("${alert.kind} has no picture", posted().bigContentView != null)
            // The big text stays, for the surfaces that never inflate a custom view.
            assertTrue(alert.kind.name, expanded().startsWith(collapsed()))
            assertEquals(
                alert.kind.name,
                com.callbackdev.chiaro.R.layout.notification_expanded,
                posted().bigContentView.layoutId
            )
        }
    }

    /** The morning's daylight line (21 ott 2026): once the sun is up, the sunset and
     * today's light against yesterday's; before sunrise, the pair. Milan in mid-January
     * gains about two minutes a day. */
    @Test
    fun `the morning says the sunset and the light gained on yesterday, or the pair before sunrise`() {
        val daily = Alert(
            AlertKind.DAILY_SUMMARY, "f", "Milano", cloudy,
            precipPct = 80, highC = 9.0, lowC = -1.0, forDate = tomorrow
        )
        val nine = report().let { base ->
            base.copy(location = base.location.copy(localTime = tomorrow.atTime(9, 0)))
        }
        post(daily, nine)
        val sunset = expanded().lines().single { it.startsWith("Sunset ") }
        assertTrue(sunset, Regex("^Sunset \\d{1,2}:\\d{2}( [AP]M)? · \\d minutes? more light than yesterday$").matches(sunset))
        assertFalse(expanded(), expanded().contains("Sunrise "))

        val six = report().let { base ->
            base.copy(location = base.location.copy(localTime = tomorrow.atTime(6, 30)))
        }
        post(daily, six)
        assertTrue(expanded(), expanded().lines().any { it.startsWith("Sunrise ") && it.contains(", sunset ") })
        assertFalse(expanded(), expanded().contains("than yesterday"))
    }

    /** When the day's sentence already says the umbrella's hour, the rain line is not
     * repeated under the picture, which labels the peak too; the long text keeps it. */
    @Test
    fun `the rain line is not repeated under the picture when the sentence says it`() {
        val morning = report().let { base ->
            base.copy(location = base.location.copy(localTime = tomorrow.atTime(7, 0)))
        }
        post(
            Alert(
                AlertKind.DAILY_SUMMARY, "f", "Milano", cloudy,
                precipPct = 80, highC = 9.0, lowC = -1.0, forDate = tomorrow
            ),
            morning
        )
        assertTrue(collapsed(), collapsed().startsWith("Umbrella around "))
        assertTrue(expanded(), expanded().lines().any { it.startsWith("Rain from ") })
        assertTrue(picturedDetails().toString(), picturedDetails().none { it.startsWith("Rain from ") })
    }

    @Test
    fun `the morning summary says when it rains, not only how likely`() {
        val morning = report().let { base ->
            base.copy(location = base.location.copy(localTime = tomorrow.atTime(7, 0)))
        }
        post(
            Alert(
                AlertKind.DAILY_SUMMARY, "f", "Milano", cloudy,
                precipPct = 80, highC = 9.0, lowC = -1.0, forDate = tomorrow
            ),
            morning
        )
        val lines = expanded().lines()
        assertTrue(expanded(), lines.any { it.startsWith("Rain from ") && it.endsWith("up to 80%") })
        // Worth-first: the rain comes straight after «right now».
        val now = lines.indexOfFirst { it.startsWith("Right now ") }
        assertEquals(expanded(), now + 1, lines.indexOfFirst { it.startsWith("Rain from ") })
        assertTrue(posted().bigContentView != null)
        // The day's sentence leads, and says the same hour the rain line does.
        assertTrue(collapsed(), collapsed().startsWith("Umbrella around "))
    }

    @Test
    fun `every notification wears the brand's accent`() {
        post(evening())
        assertEquals(context.getColor(com.callbackdev.chiaro.R.color.notification_accent), posted().color)
    }

    // --- the fit (21 ott 2026) ---

    /** The lines under the picture, as the pictured body shows them. */
    private fun picturedDetails(): List<String> {
        val body = posted().bigContentView.apply(context, android.widget.FrameLayout(context))
        val details = body.findViewById<android.widget.TextView>(com.callbackdev.chiaro.R.id.notif_details)
        return if (details.visibility == android.view.View.VISIBLE) details.text.toString().lines() else emptyList()
    }

    /** The chart labels the peak and its hour; the line that says the same stays in the
     * long text, for the surfaces without a chart, and leaves the space under it to a line
     * that says something new. */
    @Test
    fun `under the rain chart the peak line is not repeated`() {
        post(Alert(AlertKind.PRECIPITATION, "f", "Milano", cloudy, at = tomorrow.atTime(14, 0), precipPct = 80))
        assertTrue(expanded(), expanded().lines().any { it.startsWith("Rain up to ") })
        val pictured = picturedDetails()
        assertTrue(pictured.toString(), pictured.isNotEmpty())
        assertTrue(pictured.toString(), pictured.none { it.startsWith("Rain up to ") })
        assertTrue(pictured.toString(), pictured.any { it.startsWith("From ") })
    }

    /** Every line under the picture is one of the story's lines, whole: the fit leaves a
     * line out, it never cuts one. */
    @Test
    fun `the pictured body is a head of the story's lines`() {
        post(evening())
        val story = expanded().lines().drop(2)
        val pictured = picturedDetails()
        assertTrue(pictured.toString(), pictured.isNotEmpty())
        assertEquals(story.filter { it in pictured }, pictured)
    }

    // --- the day's possible storm (17.5, 19 nov 2026) ---

    /**
     * [report] with tomorrow's afternoon rain falling in unstable air (2 mm an hour, CAPE 900
     * J/kg) at [pct], and tomorrow's own field [storm]: what the mapper read on the whole day.
     */
    private fun unstable(pct: Int = 80, storm: List<DayPart>? = listOf(DayPart.AFTERNOON)) = report().let { base ->
        base.copy(
            hourly = base.hourly.map { hour ->
                val wet = hour.condition.wmoCode == 63
                hour.copy(
                    precipMm = if (wet) 2.0 else 0.0,
                    precipChancePct = if (wet) pct else hour.precipChancePct,
                    capeJkg = if (wet) 900.0 else 50.0
                )
            },
            daily = base.daily.map { if (it.date == tomorrow) it.copy(possibleStorm = storm) else it }
        )
    }

    private fun morningOf(report: WeatherReport) =
        report.copy(location = report.location.copy(localTime = tomorrow.atTime(7, 0)))

    private val summary = Alert(
        AlertKind.DAILY_SUMMARY, "f", "Milano", cloudy,
        precipPct = 80, highC = 9.0, lowC = -1.0, forDate = tomorrow
    )

    @Test
    fun `the evening says tomorrow's possible storm, and what to do tonight`() {
        post(evening(), unstable())
        assertTrue(expanded(), expanded().lines().contains("Tomorrow a chance of thunderstorms in the afternoon · Keep an eye on the sky"))
        // A day the code spoke for (empty) or a report with no CAPE (null) says nothing of it.
        for (silent in listOf<List<DayPart>?>(emptyList(), null)) {
            post(evening(), unstable(storm = silent))
            assertTrue(expanded(), expanded().lines().none { "thunderstorms" in it })
        }
    }

    @Test
    fun `the morning says it under the umbrella, and once only when the sentence takes it`() {
        // The umbrella at 80% leads; the storm is a line of the day.
        post(summary, morningOf(unstable()))
        assertTrue(collapsed(), collapsed().startsWith("Umbrella around "))
        assertTrue(expanded(), expanded().lines().contains("A chance of thunderstorms in the afternoon · Keep an eye on the sky"))
        // At 40% there is no umbrella: the day's sentence is the storm, and no line repeats it.
        post(summary, morningOf(unstable(pct = 40)))
        assertEquals("A chance of thunderstorms in the afternoon.", collapsed())
        assertTrue(expanded(), expanded().lines().drop(1).none { "thunderstorms" in it })
    }

    @Test
    fun `the morning says no storm whose hours have gone`() {
        // 17:00: the afternoon's unstable hours (14-16) are over.
        val late = unstable(pct = 40).let { it.copy(location = it.location.copy(localTime = tomorrow.atTime(17, 0))) }
        post(summary, late)
        assertTrue(expanded(), expanded().lines().none { "thunderstorms" in it })
    }

    /**
     * The heat's peak in the hour under way is said as now (15.10, 29 Oct 2026): the twelve hours
     * the alert counts begin with that hour since, and «around 14:00» at 14:30 named an hour
     * already begun. Still ahead, it keeps its hour.
     */
    @Test
    fun `a heat peak in the hour under way is said as now`() {
        fun hot(localTime: LocalDateTime) = report().copy(
            location = report().location.copy(localTime = localTime),
            hourly = (0..10).map { i ->
                val time = today.atTime(14, 0).plusHours(i.toLong())
                HourlyForecast(
                    time = time,
                    at = time.atZone(zone).toInstant(),
                    tempC = 33.0,
                    condition = WeatherCondition(0),
                    precipChancePct = 0,
                    cloudCoverPct = 0,
                    feelsLikeC = when (i) { 0 -> 39.0; 1 -> 37.0; 2 -> 36.0; else -> 30.0 }
                )
            }
        )
        val heat = Alert(AlertKind.HEAT, "f", "Milano", WeatherCondition(0), at = today.atTime(14, 0), highC = 39.0)
        val peak = Formats.temperature(39.0, UnitSettings().temperature, Locale.getDefault())
        assertTrue(post(heat, hot(today.atTime(14, 30))))
        assertEquals(context.getString(R.string.notif_heat_body_now, peak), collapsed())
        // At 13:30 the same peak is still ahead, and keeps its hour.
        assertTrue(post(heat, hot(today.atTime(13, 30))))
        assertFalse(collapsed(), collapsed() == context.getString(R.string.notif_heat_body_now, peak))
        assertTrue(collapsed(), collapsed().startsWith(context.getString(R.string.notif_heat_body_plain, peak).removeSuffix(".")))
    }
}
