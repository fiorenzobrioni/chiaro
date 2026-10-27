package com.callbackdev.chiaro.notifications

import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.callbackdev.chiaro.MainActivity
import com.callbackdev.chiaro.ui.shell.ShellDestination
import com.callbackdev.chiaro.ui.shell.ShellTab
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.Alert
import com.callbackdev.chiaro.domain.AlertKind
import com.callbackdev.chiaro.domain.CurrentEstimate
import com.callbackdev.chiaro.domain.WeatherRecency
import com.callbackdev.chiaro.domain.model.DailyForecast
import com.callbackdev.chiaro.domain.model.WeatherReport
import com.callbackdev.chiaro.domain.settings.TemperatureUnit
import com.callbackdev.chiaro.domain.settings.UnitSettings
import com.callbackdev.chiaro.domain.warnings.PlaceWarnings
import com.callbackdev.chiaro.domain.zone
import com.callbackdev.chiaro.domain.AlertEngine
import com.callbackdev.chiaro.ui.format.Formats
import com.callbackdev.chiaro.ui.today.Headline
import com.callbackdev.chiaro.ui.today.HeadlineEngine
import com.callbackdev.chiaro.ui.today.HeadlineText
import com.callbackdev.chiaro.ui.today.WeatherText
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.absoluteValue

/**
 * Renders a built-in [Alert] as a system notification, in Chiaro's idiom: a short
 * localized title naming the place, and a body that is a sentence — the condition in
 * words, the hour, the number. tweather rendered the same alerts as JSON because its
 * reader lives in a code editor; this reader does not, so nothing here looks like a
 * file (the reskin's whole point).
 *
 * **Collapsed and expanded are two different texts** (Fase 6b, device request of
 * 4 set — until then both were the same sentence, so pulling a notification open
 * gave back exactly what it already said). Collapsed is that sentence, because the
 * system gives it one line and cuts the rest. Expanded keeps it as the headline and
 * puts the rest of the story under it, one fact per line, each with what to do about
 * it — the details grid's own rule (DESIGN §1.2). tweather folds and unfolds a JSON
 * node in the same place; this is the same idea in this product's register.
 *
 * Nothing in the expanded body is invented: the window comes from [AlertDetails] and
 * the night from [EveningDetails], both reading the very hours the engine judged, and a
 * line whose data is missing is not drawn at all (§1.1 — never a dash where a value
 * should be).
 *
 * **The two summaries lead with the day's sentence** (21 ott 2026): the one Oggi's hero
 * and the widgets say, from [HeadlineEngine] — an orange warning, a storm, the umbrella's
 * hour, frost by morning, the wind, or on a quiet day its dry spell and its warmth. Until
 * then the summary was the one surface of the app that led with the model's code and its
 * numbers («Coperto. Minima 17°, massima 23°, pioggia 8%»), against the product's first
 * rule, a sentence before any number. The sentence is offered in its longest form that
 * fits the collapsed line ([NotificationFit], the full sentence, then the widgets' brief
 * one, then a quiet day's two halves one at a time), and the numbers follow it: in the
 * opened body the picture labels the high and the low where they happen, and the long
 * text, which has no picture, says them as its first line. A day with no sentence keeps
 * the factual line — and
 * its rain clause only from the headline's own «possible» floor: «pioggia 8%» under a
 * sentence that says «asciutto» on the same screen is a number nobody acts on.
 *
 * One channel per kind — the reader can silence the morning summary and keep the
 * storm warnings — and one fixed notification id per kind, so a same-kind alert
 * overwrites instead of stacking.
 */
object AlertNotifier {

    /**
     * Posts the notification; false when notifications are off or the kind's channel
     * is muted — the caller must then NOT burn the alert's fingerprint, so the alert
     * can still fire if the reader re-enables the channel.
     */
    fun notify(
        context: Context,
        alert: Alert,
        report: WeatherReport,
        units: UnitSettings,
        warnings: PlaceWarnings? = null
    ): Boolean {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false
        ensureChannel(context, manager, alert.kind)
        if (manager.getNotificationChannelCompat(alert.kind.channelId)?.importance ==
            NotificationManagerCompat.IMPORTANCE_NONE
        ) {
            return false
        }

        val day = daySentence(context, alert, report, units, warnings)
        val facts = body(context, alert, report, units)
        val headline = day?.collapsed ?: facts
        val title = context.getString(alert.kind.titleRes, alert.cityLabel)
        val story = details(
            context, alert, report, units,
            rainSaid = day?.saysRain == true, heatSaid = day?.saysHeat == true
        )
        // With a sentence leading, the numbers come right under it in the long text, as its
        // first line; under the picture they are the picture's own labels, and a line that
        // said them again would take the place of one that says something new.
        val range = day?.let { rangeLine(context, alert, units.temperature) }
        val details = if (range != null) listOf(Line(range, pictured = false)) + story else story
        val builder = NotificationCompat.Builder(context, alert.kind.channelId)
            .setSmallIcon(R.drawable.ic_stat_chiaro)
            .setColor(NotificationViews.accent(context))
            .setContentTitle(title)
            .setContentText(headline)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(expanded(headline, details))
            )
            .setContentIntent(openApp(context, alert.kind.notificationId))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
        NotificationViews.quietAtNight(builder)
        // The picture (23 set 2026): the rain's hours under a rain or storm alert, the
        // day under a summary — see [NotificationCharts] for why these and no others.
        // Painted only when there is something to paint; the big text above stays
        // either way, for every surface that does not inflate a custom view.
        runCatching { chart(context, alert, report, units) }.getOrNull()?.let { (bitmap, description) ->
            NotificationViews.expandWithChart(
                context, builder, title, headline,
                details.filter { it.pictured }.map { it.forms },
                bitmap, description
            )
        }
        val notification = builder.build()
        return try {
            manager.notify(alert.kind.notificationId, notification)
            true
        } catch (e: SecurityException) {
            false // POST_NOTIFICATIONS revoked between the check and the post
        }
    }

    /** The day's sentence for a summary, in the form that fits the collapsed line. */
    private class DaySentence(val collapsed: String, val saysRain: Boolean, val saysHeat: Boolean)

    /**
     * The day's sentence as Oggi says it, for the two summaries: the ladder first
     * ([HeadlineEngine.headline], with the official warnings at its top), the quiet day's
     * own sentence after it ([HeadlineEngine.calm]), null when neither has anything true
     * to say — or when the report does not cover its own now, which is the one case Oggi
     * shows nothing either. The report is read as the page reads it: the forecast hour
     * standing in for a stale `current`, the hours already gone trimmed off.
     *
     * The forms, longest first, for [NotificationFit.firstThatFits]: the full sentence, the
     * brief one the widgets print, and on a quiet day each half of the pair on its own —
     * «Asciutto per i prossimi 7 giorni, massima 23° verso le 16:00» does not fit one line
     * of a phone, and the system's own ellipsis is the thing this rule exists to avoid. No
     * quarter-hour reading: a notification is not rebuilt by the minute. The hero prints
     * its sentence without a full stop; a notification's line is a sentence and gets one.
     */
    private fun daySentence(
        context: Context,
        alert: Alert,
        report: WeatherReport,
        units: UnitSettings,
        warnings: PlaceWarnings?
    ): DaySentence? {
        if (alert.kind != AlertKind.DAILY_SUMMARY && alert.kind != AlertKind.EVENING_SUMMARY) return null
        val now = report.location.localTime
        val instant = now.atZone(report.zone()).toInstant()
        if (!WeatherRecency.coversNow(report, instant)) return null
        val read = WeatherRecency.trim(CurrentEstimate.apply(report, instant), instant)
        val timeFmt = clockFormat(context)
        fun say(headline: Headline, brief: Boolean = false): String? =
            HeadlineText.of(context, headline, timeFmt, units, brief)?.let(::sentence)
        val ladder = HeadlineEngine.headline(read, now, warnings)
        val forms: List<String> = if (ladder != null) {
            listOfNotNull(say(ladder), say(ladder, brief = true))
        } else {
            val calm = HeadlineEngine.calm(read, now) ?: return null
            listOfNotNull(
                say(calm),
                calm.dry?.let { say(Headline.Calm(it, null)) },
                calm.warmth?.let { say(Headline.Calm(null, it)) }
            )
        }.distinct()
        if (forms.isEmpty()) return null
        val saysRain = ladder is Headline.WetNow || ladder is Headline.WetSoon ||
            ladder is Headline.WetMaybe || ladder is Headline.NextRain
        val saysHeat = ladder is Headline.Heat || ladder is Headline.HeatTomorrow
        return DaySentence(NotificationFit.firstThatFits(context, forms), saysRain, saysHeat)
    }

    /** A line of the shade is a sentence: a full stop where the hero's line has none. */
    private fun sentence(text: String): String =
        if (text.endsWith(".") || text.endsWith("!") || text.endsWith("?")) text else "$text."

    /** «Minima 17°, massima 23°.»: the summary's numbers, under the day's sentence. */
    private fun rangeLine(context: Context, alert: Alert, unit: TemperatureUnit): String {
        val locale = Locale.getDefault()
        val low = alert.lowC?.let { Formats.temperature(it, unit, locale) } ?: "–"
        val high = alert.highC?.let { Formats.temperature(it, unit, locale) } ?: "–"
        return context.getString(R.string.notif_summary_range, low, high)
    }

    /** The sentence: condition in the reader's words, the hour, the number behind it. */
    private fun body(context: Context, alert: Alert, report: WeatherReport, units: UnitSettings): String {
        val unit = units.temperature
        val locale = Locale.getDefault()
        val time = alert.at?.format(clockFormat(context))
        val condition = alert.condition
            ?.let { context.getString(WeatherText.condition(it.wmoCode)) }
        return when (alert.kind) {
            // Whole sentences, one per shape, since the review of 21 set 2026. They
            // used to be a stem plus an optional ", rain at 90%" fragment, which cost
            // the sentence its full stop (the only built-in alert without one) and
            // fixed the clause order in English for every language. An absent hour or
            // an absent chance now picks a sentence that does not need it: the old
            // `?: ""` and `?: 0` printed "Thunderstorm around" and "rain at 0%",
            // which is the screen breaking and the screen lying (§1.1) — unreachable
            // today, because the engine anchors both alerts on an hour it has read.
            // The gale (14.7): the run's strongest gust and its hour, read off the window the
            // notifier draws from the very hours the engine judged, like the heat's; the
            // alert's own number when those hours have elapsed under a cached report.
            AlertKind.SEVERE -> if (alert.bucket == AlertEngine.SevereBucket.WIND) {
                val window = alert.at?.let { AlertDetails.windWindow(report.hourly, it, report.location.elevationM) }
                val peak = (window?.peakKph ?: alert.gustKph)?.let { Formats.wind(it, units.windSpeed, locale) }
                when {
                    window != null -> context.getString(
                        R.string.notif_severe_body_wind, peak, window.peakAt.format(clockFormat(context))
                    )
                    peak != null && time != null -> context.getString(R.string.notif_severe_body_wind, peak, time)
                    else -> context.getString(R.string.notif_severe_body_wind_soon)
                }
            } else {
                val sky = condition ?: context.getString(R.string.cond_unknown)
                val pct = alert.precipPct?.takeIf { it > 0 }
                when {
                    time == null -> context.getString(R.string.notif_severe_body_soon, sky)
                    pct == null -> context.getString(R.string.notif_severe_body, sky, time)
                    else -> context.getString(R.string.notif_severe_body_rain, sky, time, pct)
                }
            }
            // The heat (14.6): the worst feels-like and its hour, read off the window the
            // notifier draws from the very hours the engine judged; the alert's own number
            // when those hours have elapsed under a cached report; nothing invented when
            // neither is there.
            AlertKind.HEAT -> {
                val window = alert.at?.let { AlertDetails.heatWindow(report.hourly, it) }
                val peak = (window?.peakC ?: alert.highC)?.let { Formats.temperature(it, unit, locale) }
                when {
                    window != null -> context.getString(
                        R.string.notif_heat_body, peak, window.peakAt.format(clockFormat(context))
                    )
                    peak != null -> context.getString(R.string.notif_heat_body_plain, peak)
                    else -> context.getString(R.string.notif_heat_body_soon)
                }
            }
            AlertKind.PRECIPITATION -> {
                val pct = alert.precipPct
                if (time == null || pct == null) {
                    context.getString(R.string.notif_precip_body_plain)
                } else {
                    context.getString(R.string.notif_precip_body, time, pct)
                }
            }
            // The two summaries share one sentence and differ in their TITLE ("Oggi"
            // against "Domani"): they are twins on purpose, and a reader who has both
            // on should be able to read either without learning a second shape.
            //
            // Two forms: the day may carry no probability at all (§1.1), and the
            // summary then drops the clause rather than announcing a 0% nobody
            // forecast. The temperatures keep their dash — an absent reading in a
            // list of readings is a dash; an absent clause is no clause.
            //
            // And the clause only from the headline's «possible» floor (21 ott 2026):
            // «pioggia 8%» and «pioggia 0%» were printed under a hero that said
            // «asciutto», two opinions on one sky. The number stays the model's; whether
            // it is worth a word is the app's, and the app has one bar for that.
            AlertKind.DAILY_SUMMARY, AlertKind.EVENING_SUMMARY -> {
                val low = alert.lowC?.let { Formats.temperature(it, unit, locale) } ?: "–"
                val high = alert.highC?.let { Formats.temperature(it, unit, locale) } ?: "–"
                val sky = condition ?: context.getString(R.string.cond_unknown)
                alert.precipPct?.takeIf { it >= HeadlineEngine.CLEAR_BELOW_PCT }?.let {
                    context.getString(R.string.notif_summary_body, sky, low, high, it)
                } ?: context.getString(R.string.notif_summary_body_no_rain, sky, low, high)
            }
        }
    }

    /**
     * The rest of the story under the headline: the window the weather really covers,
     * its worst hour, what the thermometer does meanwhile; for the morning summary the
     * day's own facts; for the evening one the night ahead and what tomorrow asks
     * tonight. Each with its consequence.
     *
     * Order is worth-first, because the system cuts a long body at the bottom — and the
     * pictured body keeps the lines that fit under the picture, measured before the post
     * ([NotificationFit], 21 ott 2026), the first ones first.
     */
    private fun details(
        context: Context,
        alert: Alert,
        report: WeatherReport,
        units: UnitSettings,
        /** Whether the day's sentence already says when it rains today: then the rain
         * line under the picture would say it twice, and stays in the long text. */
        rainSaid: Boolean,
        /** The same for the heat (14.6): a sentence that says the peak needs no heat line. */
        heatSaid: Boolean
    ): List<Line> = when (alert.kind) {
        AlertKind.SEVERE -> if (alert.bucket == AlertEngine.SevereBucket.WIND) {
            windDetails(
                context, units, report,
                alert.at?.let { AlertDetails.windWindow(report.hourly, it, report.location.elevationM) }
            )
        } else {
            forecastDetails(
                context, units, report,
                alert.at?.let { AlertDetails.severeWindow(report.hourly, it, report.location.elevationM) }
            )
        }
        AlertKind.PRECIPITATION -> forecastDetails(
            context, units, report,
            alert.at?.let { AlertDetails.rainWindow(report.hourly, it) }
        )
        AlertKind.HEAT -> heatDetails(
            context, units, report,
            alert.at?.let { AlertDetails.heatWindow(report.hourly, it) }
        )
        AlertKind.DAILY_SUMMARY -> dayDetails(context, units, report, rainSaid, heatSaid)
        AlertKind.EVENING_SUMMARY -> eveningDetails(context, units, report, alert, heatSaid)
    }

    /**
     * The headline and its details as one text, for the big-text body. The morning kinds
     * are never empty ("right now" always has a reading behind it); the evening one can
     * be, on a cached report whose hours have all elapsed and a place with no sunrise.
     * Then the expanded body IS the headline — which is honest, and still better than a
     * blank line under it.
     */
    private fun expanded(headline: String, details: List<Line>): String =
        if (details.isEmpty()) headline else headline + "\n\n" + details.joinToString("\n") { it.text }

    /**
     * A line of the story, and whether it also goes under the picture. A fact the picture
     * already draws — the rain's peak and its hour, labelled on the chart — is in the long
     * text for the surfaces without one, and not under the chart, where it would say the
     * same thing twice and take a line from one that says something new.
     */
    private class Line(
        val text: String,
        val pictured: Boolean = true,
        /** The line's shorter forms, for the picture's body when the long one would wrap
         * (22 ott 2026); [text] is always the first. */
        shorter: List<String> = emptyList()
    ) {
        val forms: List<String> = listOf(text) + shorter
    }

    private class Story : ArrayList<Line>() {
        fun line(text: String, vararg shorter: String) { add(Line(text, shorter = shorter.toList())) }
        fun aside(text: String) { add(Line(text, pictured = false)) }
    }

    private inline fun story(block: Story.() -> Unit): List<Line> = Story().apply(block)

    /**
     * The picture for [alert] and its text equivalent, or null when there is nothing to
     * draw. Storms and rain get the next twelve hours of rain with the alert's window lit;
     * the summaries get their day — what is left of today in the morning, the whole of
     * tomorrow in the evening — with its temperature, its rain and its night. The heat
     * (14.6) gets the morning's picture: the rest of today's temperature over its light,
     * which is the heat's own shape.
     */
    private fun chart(
        context: Context,
        alert: Alert,
        report: WeatherReport,
        units: UnitSettings
    ): Pair<NotificationCharts.Themed, String>? {
        val locale = Locale.getDefault()
        val is24h = android.text.format.DateFormat.is24HourFormat(context)
        val now = report.location.localTime
        return when (alert.kind) {
            AlertKind.SEVERE, AlertKind.PRECIPITATION -> {
                val severe = alert.kind == AlertKind.SEVERE
                // The gale's picture (14.7) is its gusts, not the rain's chance: the same
                // twelve hours, the same window, the quantity the alert is about.
                if (alert.bucket == AlertEngine.SevereBucket.WIND) {
                    val elevation = report.location.elevationM
                    val window = alert.at?.let { AlertDetails.windWindow(report.hourly, it, elevation) }
                    val bitmap = NotificationCharts.themed(context) { inks ->
                        NotificationCharts.gustHours(
                            report.hourly, now, window, AlertEngine.windGustBar(elevation),
                            units.windSpeed, inks, is24h, locale
                        )
                    } ?: return null
                    val shown = report.hourly.filter { !it.time.isBefore(now.withMinute(0)) }
                        .take(NotificationCharts.RainHours)
                    val peak = shown.filter { it.gustKph != null }.maxByOrNull { it.gustKph!! }
                    val description = if (peak == null) {
                        context.getString(R.string.notif_chart_gust_desc_plain, shown.size)
                    } else {
                        context.getString(
                            R.string.notif_chart_gust_desc, shown.size,
                            Formats.wind(peak.gustKph!!, units.windSpeed, locale), peak.time.format(clockFormat(context))
                        )
                    }
                    return bitmap to description
                }
                val window = alert.at?.let {
                    if (severe) AlertDetails.severeWindow(report.hourly, it, report.location.elevationM)
                    else AlertDetails.rainWindow(report.hourly, it)
                }
                val bitmap = NotificationCharts.themed(context) { inks ->
                    NotificationCharts.rainHours(report.hourly, now, window, severe, inks, is24h, locale)
                } ?: return null
                val shown = report.hourly.filter { !it.time.isBefore(now.withMinute(0)) }
                    .take(NotificationCharts.RainHours)
                val peak = shown.filter { it.precipChancePct != null }.maxByOrNull { it.precipChancePct!! }
                val description = if (peak == null) {
                    context.getString(R.string.notif_chart_rain_desc_plain, shown.size)
                } else {
                    context.getString(
                        R.string.notif_chart_rain_desc, shown.size,
                        peak.precipChancePct!!, peak.time.format(clockFormat(context))
                    )
                }
                bitmap to description
            }
            AlertKind.DAILY_SUMMARY, AlertKind.EVENING_SUMMARY, AlertKind.HEAT -> {
                val date = if (alert.kind != AlertKind.EVENING_SUMMARY) {
                    now.toLocalDate()
                } else {
                    alert.forDate ?: now.toLocalDate().plusDays(1)
                }
                val hours = report.hourly.filter {
                    it.time.toLocalDate() == date && !it.time.isBefore(now.withMinute(0))
                }
                val bitmap = NotificationCharts.themed(context) { inks ->
                    NotificationCharts.day(
                        hours, date, report.zone(), report.location.coordinates,
                        units.temperature, inks, is24h, locale
                    )
                } ?: return null
                val low = Formats.temperature(hours.minOf { it.tempC }, units.temperature, locale)
                val high = Formats.temperature(hours.maxOf { it.tempC }, units.temperature, locale)
                val rain = hours.mapNotNull { it.precipChancePct }.maxOrNull()?.takeIf { it > 0 }
                val description = if (rain == null) {
                    context.getString(R.string.notif_chart_day_desc_dry, low, high)
                } else {
                    context.getString(R.string.notif_chart_day_desc, low, high, rain)
                }
                bitmap to description
            }
        }
    }

    /** What an hour-anchored alert (storm, rain) can say beyond its first hour. */
    private fun forecastDetails(
        context: Context,
        units: UnitSettings,
        report: WeatherReport,
        window: AlertWindow?
    ): List<Line> = story {
        val locale = Locale.getDefault()
        val clock = clockFormat(context)
        window?.let { w ->
            // An open-ended run says "from 17:00" and stops there: the forecast ran
            // out, and naming an end it never showed would be the one lie the reader
            // cannot check (§1.1).
            line(
                when {
                    w.openEnded ->
                        context.getString(R.string.notif_detail_from, w.start.format(clock))
                    w.singleHour ->
                        context.getString(R.string.notif_detail_hour, w.start.format(clock))
                    else -> context.getString(
                        R.string.notif_detail_window,
                        w.start.format(clock),
                        w.end.format(clock)
                    )
                }
            )
            // The peak and its hour are labelled on the chart: under it this line would say
            // the same thing twice and take a line from one that says something new (21 ott
            // 2026, the fit); it stays in the long text, which has no chart.
            if (w.peakPrecipPct > 0) {
                aside(
                    context.getString(
                        R.string.notif_detail_rain_peak,
                        w.peakPrecipPct,
                        w.peakPrecipAt.format(clock)
                    )
                )
            }
            line(
                context.getString(
                    R.string.notif_detail_temp_range,
                    Formats.temperature(w.lowC, units.temperature, locale),
                    Formats.temperature(w.highC, units.temperature, locale)
                )
            )
        }
        line(nowLine(context, units, report))
        line(windLine(context, units, report), windLine(context, units, report, short = true))
    }

    /**
     * The gale's story (14.7): its hours, the thermometer's range meanwhile, where the
     * reader stands now. The strongest gust and its hour are the collapsed line's and not
     * said again; the wind of now closes it, as under a storm, because the gale's own
     * numbers are the rows' and the reading of the moment is a different fact.
     */
    private fun windDetails(
        context: Context,
        units: UnitSettings,
        report: WeatherReport,
        window: WindWindow?
    ): List<Line> = story {
        val locale = Locale.getDefault()
        val clock = clockFormat(context)
        window?.let { w ->
            line(
                when {
                    w.openEnded ->
                        context.getString(R.string.notif_detail_from, w.start.format(clock))
                    w.singleHour ->
                        context.getString(R.string.notif_detail_hour, w.start.format(clock))
                    else -> context.getString(
                        R.string.notif_detail_window, w.start.format(clock), w.end.format(clock)
                    )
                }
            )
            // The gale's notification silences the umbrella's for its run (one per
            // perturbation), so it carries the umbrella's fact: the rain's peak in the
            // window, from the floor at which the page calls rain possible.
            report.hourly
                .filter { !it.time.isBefore(w.start) && !it.time.isAfter(w.end) && it.precipChancePct != null }
                .maxByOrNull { it.precipChancePct!! }
                ?.takeIf { it.precipChancePct!! >= HeadlineEngine.CLEAR_BELOW_PCT }
                ?.let { wet ->
                    aside(context.getString(R.string.notif_detail_rain_peak, wet.precipChancePct!!, wet.time.format(clock)))
                }
            line(
                context.getString(
                    R.string.notif_detail_temp_range,
                    Formats.temperature(w.lowC, units.temperature, locale),
                    Formats.temperature(w.highC, units.temperature, locale)
                )
            )
        }
        line(nowLine(context, units, report))
        line(windLine(context, units, report), windLine(context, units, report, short = true))
    }

    /**
     * The heat's story (14.6): its hours with what to do in them, what the thermometer
     * itself does meanwhile, where the reader stands now, the day's UV — the sun is the half
     * of the heat a reader can do something about. The peak and its hour are the collapsed
     * line's and the picture's label: not said again under it.
     */
    private fun heatDetails(
        context: Context,
        units: UnitSettings,
        report: WeatherReport,
        window: HeatWindow?
    ): List<Line> = story {
        val locale = Locale.getDefault()
        val clock = clockFormat(context)
        window?.let { w ->
            val meaning = context.getString(WeatherText.heatMeaning(w.peakC))
            line(
                when {
                    w.openEnded ->
                        context.getString(R.string.notif_detail_heat_from, w.start.format(clock), meaning)
                    w.singleHour ->
                        context.getString(R.string.notif_detail_heat_hour, w.start.format(clock), meaning)
                    else -> context.getString(
                        R.string.notif_detail_heat_window, w.start.format(clock), w.end.format(clock), meaning
                    )
                },
                meaning
            )
            line(
                context.getString(
                    R.string.notif_detail_temp_range,
                    Formats.temperature(w.lowC, units.temperature, locale),
                    Formats.temperature(w.highC, units.temperature, locale)
                )
            )
        }
        line(nowLine(context, units, report))
        report.daily.firstOrNull()?.let { uvLine(context, it) }
    }

    /** The morning summary's own block: the day, not the next few hours. */
    private fun dayDetails(
        context: Context,
        units: UnitSettings,
        report: WeatherReport,
        rainSaid: Boolean,
        heatSaid: Boolean
    ): List<Line> = story {
        val clock = clockFormat(context)
        line(nowLine(context, units, report))
        // When it rains today (23 set 2026, notification review): the summary said how
        // likely and never WHEN, which is the half of the question a reader acts on. The
        // evening summary's own rule, read over what is left of today: the window where
        // it clears the umbrella bar, the peak where it only gets to «possible».
        val now = report.location.localTime
        val today = now.toLocalDate()
        val ahead = report.hourly.filter { !it.time.isBefore(now.withMinute(0)) }
        val window = EveningDetails.tomorrowRain(ahead, today)
        // Said by the day's sentence already (21 ott 2026)? Then not again under the
        // picture, which labels the peak too; the long text, with no picture, keeps it.
        val rain: (String) -> Unit = if (rainSaid) ::aside else ::line
        if (window != null) {
            rain(
                when {
                    window.openEnded -> context.getString(
                        R.string.notif_detail_today_rain_from,
                        window.start.format(clock), window.peakPrecipPct
                    )
                    window.singleHour -> context.getString(
                        R.string.notif_detail_today_rain_hour,
                        window.start.format(clock), window.peakPrecipPct
                    )
                    else -> context.getString(
                        R.string.notif_detail_today_rain,
                        window.start.format(clock), window.end.format(clock), window.peakPrecipPct
                    )
                }
            )
        } else {
            EveningDetails.tomorrowRainPeak(ahead, today)
                ?.takeIf { it.pct >= HeadlineEngine.CLEAR_BELOW_PCT }
                ?.let {
                    rain(
                        context.getString(
                            R.string.notif_detail_today_rain_maybe, it.pct, it.at.format(clock)
                        )
                    )
                }
        }
        // The heat (14.6), when the day's sentence did not take it (an umbrella outranks
        // it): today's worst feels-like, its hour and what to do about it, from the
        // sentence's own bar.
        if (!heatSaid) {
            EveningDetails.heatPeak(ahead, today)?.let { heat ->
                val peak = Formats.temperature(heat.feelsLikeC, units.temperature, Locale.getDefault())
                line(
                    context.getString(
                        R.string.notif_detail_today_heat, peak, heat.at.format(clock),
                        context.getString(WeatherText.heatMeaning(heat.feelsLikeC))
                    ),
                    context.getString(R.string.notif_detail_today_heat_short, peak, heat.at.format(clock))
                )
            }
        }
        // No index, no line: a model that does not carry UV gets silence rather than
        // a "nessuna protezione necessaria" nobody forecast (§1.1).
        report.daily.firstOrNull()?.let { uvLine(context, it) }
        line(windLine(context, units, report), windLine(context, units, report, short = true))
        // The daylight edition's own line (21 ott 2026), the one the evening already had:
        // once the sun is up, the sunset and today's light against yesterday's, both ends
        // from the engine so the two days are measured alike; a summary that arrives before
        // sunrise keeps the pair, because then the sunrise is still ahead. Both ends or
        // neither: above the Arctic circle in June there is no sunrise to have, and half a
        // pair says less than nothing. After the wind since 23 set 2026: the picture draws
        // the night, so this line is the fifth thing to read. The delta is dropped inside a
        // minute of it, as the evening's is: around a solstice the sun stands still.
        val zone = report.zone()
        val coords = report.location.coordinates
        val yesterday = EveningDetails.sun(today.minusDays(1), zone, coords, today = null)?.daylight
        EveningDetails.sun(today, zone, coords, today = yesterday)?.let { sun ->
            if (now.toLocalTime().isBefore(sun.sunrise)) {
                line(context.getString(R.string.notif_detail_sun, sun.sunrise.format(clock), sun.sunset.format(clock)))
                return@let
            }
            val minutes = sun.daylightDelta?.toMinutes() ?: 0L
            val delta = if (minutes == 0L) {
                null
            } else {
                context.resources.getQuantityString(
                    if (minutes > 0) R.plurals.notif_detail_daylight_more_yesterday
                    else R.plurals.notif_detail_daylight_less_yesterday,
                    minutes.absoluteValue.toInt(),
                    minutes.absoluteValue.toInt()
                )
            }
            if (delta == null) {
                line(context.getString(R.string.notif_detail_sunset, sun.sunset.format(clock)))
            } else {
                line(
                    context.getString(R.string.notif_detail_sunset_delta, sun.sunset.format(clock), delta),
                    context.getString(R.string.notif_detail_sunset_delta, sun.sunset.format(clock), daylightShort(context, minutes))
                )
            }
        }
        report.airQuality?.let { air ->
            val meaning = context.getString(WeatherText.aqiMeaning(air.aqiIndex))
            line(
                context.getString(R.string.notif_detail_air, air.aqiIndex, meaning),
                context.getString(R.string.notif_detail_air_short, air.aqiIndex, meaning)
            )
        }
    }

    /**
     * The evening summary's own block: the night between here and tomorrow, then the
     * parts of tomorrow that ask something TONIGHT — an umbrella by the door, an alarm
     * set to the light. Nothing about today: by 20:00 today is not a decision.
     *
     * Worth-first, and every line conditional on having its data. The rain lines use
     * the app's two existing bars and no third one: the umbrella threshold the rain
     * warning fires at, and the "possible" floor the Today headline uses.
     */
    private fun eveningDetails(
        context: Context,
        units: UnitSettings,
        report: WeatherReport,
        alert: Alert,
        heatSaid: Boolean
    ): List<Line> = story {
        val locale = Locale.getDefault()
        val clock = clockFormat(context)
        val zone = report.zone()
        val now = report.location.localTime
        val tomorrow = alert.forDate ?: now.toLocalDate().plusDays(1)
        val coords = report.location.coordinates

        // The night: how cold, when, and what that asks of the reader.
        val nightEnd = EveningDetails.nightEnd(tomorrow, zone, coords)
        EveningDetails.night(report.hourly, from = now, until = nightEnd)?.let { night ->
            line(
                context.getString(
                    R.string.notif_detail_night,
                    Formats.temperature(night.lowC, units.temperature, locale),
                    night.lowAt.format(clock),
                    context.getString(WeatherText.nightMeaning(night.lowC))
                )
            )
            // Only once the night has stopped promising a dry one: under half, the
            // number would be a line that says "probably not", which is not news.
            val pct = night.peakPrecipPct
            if (pct != null && pct >= HeadlineEngine.CLEAR_BELOW_PCT && night.peakPrecipAt != null) {
                line(
                    context.getString(
                        R.string.notif_detail_night_rain,
                        pct,
                        night.peakPrecipAt.format(clock)
                    )
                )
            }
        }

        // Tomorrow's rain: the window when it clears the umbrella bar, the peak when
        // it only gets as far as "possible", nothing at all when it is a dry day.
        val window = EveningDetails.tomorrowRain(report.hourly, tomorrow)
        if (window != null) {
            line(
                when {
                    window.openEnded -> context.getString(
                        R.string.notif_detail_tomorrow_rain_from,
                        window.start.format(clock),
                        window.peakPrecipPct
                    )
                    window.singleHour -> context.getString(
                        R.string.notif_detail_tomorrow_rain_hour,
                        window.start.format(clock),
                        window.peakPrecipPct
                    )
                    else -> context.getString(
                        R.string.notif_detail_tomorrow_rain,
                        window.start.format(clock),
                        window.end.format(clock),
                        window.peakPrecipPct
                    )
                }
            )
        } else {
            EveningDetails.tomorrowRainPeak(report.hourly, tomorrow)
                ?.takeIf { it.pct >= HeadlineEngine.CLEAR_BELOW_PCT }
                ?.let {
                    line(
                        context.getString(
                            R.string.notif_detail_tomorrow_rain_maybe,
                            it.pct,
                            it.at.format(clock)
                        )
                    )
                }
        }

        // Tomorrow's heat (14.6), when the sentence did not take it: the worst feels-like
        // and its hour, with what to do about it — where tomorrow's walk goes is decided
        // tonight.
        if (!heatSaid) {
            EveningDetails.heatPeak(report.hourly, tomorrow)?.let { heat ->
                val peak = Formats.temperature(heat.feelsLikeC, units.temperature, locale)
                line(
                    context.getString(
                        R.string.notif_detail_tomorrow_heat, peak, heat.at.format(clock),
                        context.getString(WeatherText.heatMeaning(heat.feelsLikeC))
                    ),
                    context.getString(R.string.notif_detail_tomorrow_heat_short, peak, heat.at.format(clock))
                )
            }
        }

        // The daylight edition's own line: the two ends of tomorrow, and how much
        // light that is against today. The delta is dropped inside a minute of it —
        // around a solstice the sun stands still, and "0 minutes less" is noise.
        EveningDetails.sun(tomorrow, zone, coords, report.astronomical.daylightDuration)
            ?.let { sun ->
                val minutes = sun.daylightDelta?.toMinutes() ?: 0L
                val delta = if (minutes == 0L) {
                    null
                } else {
                    context.resources.getQuantityString(
                        if (minutes > 0) R.plurals.notif_detail_daylight_more
                        else R.plurals.notif_detail_daylight_less,
                        minutes.absoluteValue.toInt(),
                        minutes.absoluteValue.toInt()
                    )
                }
                if (delta == null) {
                    line(
                        context.getString(
                            R.string.notif_detail_sun_tomorrow,
                            sun.sunrise.format(clock),
                            sun.sunset.format(clock)
                        )
                    )
                } else {
                    line(
                        context.getString(
                            R.string.notif_detail_sun_tomorrow_delta,
                            sun.sunrise.format(clock),
                            sun.sunset.format(clock),
                            delta
                        ),
                        context.getString(
                            R.string.notif_detail_sun_tomorrow_delta,
                            sun.sunrise.format(clock),
                            sun.sunset.format(clock),
                            daylightShort(context, minutes)
                        )
                    )
                }
            }

        // Tomorrow's UV, only once there is something to do about it. The morning
        // summary prints every band because at 8:00 "no protection needed" settles
        // the question the reader opened the app with; at 21:00 nobody is asking.
        report.daily.firstOrNull { it.date == tomorrow }
            ?.let { day ->
                val uv = day.uvIndexMax?.takeIf { it >= UV_WORTH_SAYING } ?: return@let
                val meaning = context.getString(WeatherText.uvPeakMeaning(uv, day.condition))
                line(
                    context.getString(R.string.notif_detail_uv_tomorrow, uv, meaning),
                    context.getString(R.string.notif_detail_uv_tomorrow_short, uv, meaning)
                )
            }
    }

    /** The first UV band that asks for anything ([WeatherText.uvMeaning]'s "moderate"). */
    private const val UV_WORTH_SAYING = 3

    /** The day's peak UV with its meaning; no index, no line (§1.1). */
    private fun Story.uvLine(context: Context, day: DailyForecast) {
        val uv = day.uvIndexMax ?: return
        val meaning = context.getString(WeatherText.uvPeakMeaning(uv, day.condition))
        line(
            context.getString(R.string.notif_detail_uv, uv, meaning),
            context.getString(R.string.notif_detail_uv_short, uv, meaning)
        )
    }

    /** Where the reader is standing when the notification arrives. */
    private fun nowLine(context: Context, units: UnitSettings, report: WeatherReport): String =
        context.getString(
            R.string.notif_detail_now,
            Formats.temperature(
                report.current.tempC, units.temperature, Locale.getDefault()
            ),
            context.getString(WeatherText.condition(report.current.condition.wmoCode))
        )

    /**
     * The wind with what it feels like: a number nobody can act on is not a line.
     *
     * It says "right now" in words, and has to: the hourly forecast carries no wind,
     * so this is the reading at the moment the notification is posted — printed bare
     * under a storm window it would read as the storm's wind, which is the kind of
     * quiet lie §1.1 exists to forbid.
     */
    private fun windLine(context: Context, units: UnitSettings, report: WeatherReport, short: Boolean = false): String =
        context.getString(
            if (short) R.string.notif_detail_wind_short else R.string.notif_detail_wind,
            Formats.wind(report.current.wind.speedKph, units.windSpeed, Locale.getDefault()),
            context.getString(WeatherText.windMeaning(report.current.wind.speedKph))
        )

    /** The daylight's change in its short form, «3 min less light», for a line that would
     * wrap with the long one (22 ott 2026); [minutes] signed, never zero here. */
    private fun daylightShort(context: Context, minutes: Long): String =
        context.getString(
            if (minutes > 0) R.string.notif_detail_daylight_more_short else R.string.notif_detail_daylight_less_short,
            minutes.absoluteValue.toInt()
        )

    private val AlertKind.channelId: String
        get() = when (this) {
            AlertKind.SEVERE -> "alert_severe"
            AlertKind.PRECIPITATION -> "alert_precip"
            AlertKind.DAILY_SUMMARY -> "alert_summary"
            AlertKind.EVENING_SUMMARY -> "alert_evening"
            AlertKind.HEAT -> "alert_heat"
        }

    /** Fixed ids: a fresher same-kind alert replaces the old one, never stacks. */
    private val AlertKind.notificationId: Int
        get() = when (this) {
            AlertKind.SEVERE -> 1001
            AlertKind.PRECIPITATION -> 1002
            AlertKind.DAILY_SUMMARY -> 1003
            AlertKind.EVENING_SUMMARY -> 1004
            AlertKind.HEAT -> 1005
        }

    private val AlertKind.titleRes: Int
        get() = when (this) {
            AlertKind.SEVERE -> R.string.notif_severe_title
            AlertKind.PRECIPITATION -> R.string.notif_precip_title
            AlertKind.DAILY_SUMMARY -> R.string.notif_summary_title
            AlertKind.EVENING_SUMMARY -> R.string.notif_evening_title
            AlertKind.HEAT -> R.string.notif_heat_title
        }

    private val AlertKind.channelNameRes: Int
        get() = when (this) {
            AlertKind.SEVERE -> R.string.notif_channel_severe
            AlertKind.PRECIPITATION -> R.string.notif_channel_precip
            AlertKind.DAILY_SUMMARY -> R.string.notif_channel_summary
            AlertKind.EVENING_SUMMARY -> R.string.notif_channel_evening
            AlertKind.HEAT -> R.string.notif_channel_heat
        }

    private fun ensureChannel(
        context: Context,
        manager: NotificationManagerCompat,
        kind: AlertKind
    ) {
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(
                kind.channelId,
                // The storm, and the heat (14.6), outrank the summary in the system's own
                // vocabulary too.
                if (kind == AlertKind.SEVERE || kind == AlertKind.HEAT) {
                    NotificationManagerCompat.IMPORTANCE_HIGH
                } else {
                    NotificationManagerCompat.IMPORTANCE_DEFAULT
                }
            )
                .setName(context.getString(kind.channelNameRes))
                .build()
        )
    }

    private fun clockFormat(context: Context): DateTimeFormatter =
        DateTimeFormatter.ofPattern(
            if (android.text.format.DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
        )

    private fun openApp(context: Context, requestCode: Int): PendingIntent =
        PendingIntent.getActivity(
            context,
            requestCode,
            // The same door the widgets use, and the screen these four are about
            // (21 set 2026): storms, the rain in the next hours and both summaries are
            // the weather, and the weather is Oggi — its hours, its rain, its sentence.
            // Avvisi is where the SWITCH that sent this lives, which is not what the
            // reader who tapped «Pioggia alle 17» came for.
            //
            // The request code is load-bearing now that the destination rides in the
            // extras: see [ShellDestination]. These are the alert ids, 1001-1005.
            ShellDestination.intent(context, MainActivity::class.java, ShellTab.TODAY),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}
