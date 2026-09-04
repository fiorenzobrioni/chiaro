package com.callbackdev.chiaro.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.callbackdev.chiaro.MainActivity
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.Alert
import com.callbackdev.chiaro.domain.AlertKind
import com.callbackdev.chiaro.domain.model.WeatherReport
import com.callbackdev.chiaro.domain.settings.TemperatureUnit
import com.callbackdev.chiaro.domain.settings.UnitSettings
import com.callbackdev.chiaro.ui.format.Formats
import com.callbackdev.chiaro.ui.today.WeatherText
import java.time.format.DateTimeFormatter
import java.util.Locale

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
 * Nothing in the expanded body is invented: the window comes from [AlertDetails]
 * reading the very hours the engine judged, and a line whose data is missing is not
 * drawn at all (§1.1 — never a dash where a value should be).
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
        units: UnitSettings
    ): Boolean {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false
        ensureChannel(context, manager, alert.kind)
        if (manager.getNotificationChannelCompat(alert.kind.channelId)?.importance ==
            NotificationManagerCompat.IMPORTANCE_NONE
        ) {
            return false
        }

        val headline = body(context, alert, units.temperature)
        val notification = NotificationCompat.Builder(context, alert.kind.channelId)
            .setSmallIcon(R.drawable.ic_stat_chiaro)
            .setContentTitle(context.getString(alert.kind.titleRes, alert.cityLabel))
            .setContentText(headline)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(expanded(context, alert, report, units, headline))
            )
            .setContentIntent(openApp(context, alert.kind.notificationId))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
        return try {
            manager.notify(alert.kind.notificationId, notification)
            true
        } catch (e: SecurityException) {
            false // POST_NOTIFICATIONS revoked between the check and the post
        }
    }

    /** The sentence: condition in the reader's words, the hour, the number behind it. */
    private fun body(context: Context, alert: Alert, unit: TemperatureUnit): String {
        val locale = Locale.getDefault()
        val time = alert.at?.format(clockFormat(context))
        val condition = alert.condition
            ?.let { context.getString(WeatherText.condition(it.wmoCode)) }
        return when (alert.kind) {
            AlertKind.SEVERE -> buildString {
                append(
                    context.getString(
                        R.string.notif_severe_body,
                        condition ?: context.getString(R.string.cond_unknown),
                        time ?: ""
                    ).trimEnd()
                )
                alert.precipPct?.takeIf { it > 0 }?.let {
                    append(context.getString(R.string.notif_rain_fragment, it))
                }
            }
            AlertKind.PRECIPITATION -> context.getString(
                R.string.notif_precip_body, time ?: "", alert.precipPct ?: 0
            )
            AlertKind.DAILY_SUMMARY -> context.getString(
                R.string.notif_summary_body,
                condition ?: context.getString(R.string.cond_unknown),
                alert.lowC?.let { Formats.temperature(it, unit, locale) } ?: "–",
                alert.highC?.let { Formats.temperature(it, unit, locale) } ?: "–",
                alert.precipPct ?: 0
            )
        }
    }

    /**
     * The headline, then the rest of the story: the window the weather really covers,
     * its worst hour, what the thermometer does meanwhile, and — for the morning
     * summary — the day's own facts, each with its consequence.
     *
     * Order is worth-first, because the system cuts a long body at the bottom.
     */
    private fun expanded(
        context: Context,
        alert: Alert,
        report: WeatherReport,
        units: UnitSettings,
        headline: String
    ): String {
        val details = when (alert.kind) {
            AlertKind.SEVERE -> forecastDetails(
                context, units, report,
                alert.at?.let { AlertDetails.severeWindow(report.hourly, it) }
            )
            AlertKind.PRECIPITATION -> forecastDetails(
                context, units, report,
                alert.at?.let { AlertDetails.rainWindow(report.hourly, it) }
            )
            AlertKind.DAILY_SUMMARY -> dayDetails(context, units, report)
        }
        // Never empty: the window can be missing (a cached report whose hours have
        // elapsed) but "right now" always has a reading behind it.
        return headline + "\n\n" + details.joinToString("\n")
    }

    /** What an hour-anchored alert (storm, rain) can say beyond its first hour. */
    private fun forecastDetails(
        context: Context,
        units: UnitSettings,
        report: WeatherReport,
        window: AlertWindow?
    ): List<String> = buildList {
        val locale = Locale.getDefault()
        val clock = clockFormat(context)
        window?.let { w ->
            // An open-ended run says "from 17:00" and stops there: the forecast ran
            // out, and naming an end it never showed would be the one lie the reader
            // cannot check (§1.1).
            add(
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
            if (w.peakPrecipPct > 0) {
                add(
                    context.getString(
                        R.string.notif_detail_rain_peak,
                        w.peakPrecipPct,
                        w.peakPrecipAt.format(clock)
                    )
                )
            }
            add(
                context.getString(
                    R.string.notif_detail_temp_range,
                    Formats.temperature(w.lowC, units.temperature, locale),
                    Formats.temperature(w.highC, units.temperature, locale)
                )
            )
        }
        add(nowLine(context, units, report))
        add(windLine(context, units, report))
    }

    /** The morning summary's own block: the day, not the next few hours. */
    private fun dayDetails(
        context: Context,
        units: UnitSettings,
        report: WeatherReport
    ): List<String> = buildList {
        val clock = clockFormat(context)
        add(nowLine(context, units, report))
        val sunrise = report.astronomical.sunrise
        val sunset = report.astronomical.sunset
        // Both or neither: above the Arctic circle in June there is no sunrise to
        // have, and half a pair says less than nothing.
        if (sunrise != null && sunset != null) {
            add(
                context.getString(
                    R.string.notif_detail_sun,
                    sunrise.format(clock),
                    sunset.format(clock)
                )
            )
        }
        report.daily.firstOrNull()?.let { today ->
            add(
                context.getString(
                    R.string.notif_detail_uv,
                    today.uvIndexMax,
                    context.getString(WeatherText.uvMeaning(today.uvIndexMax))
                )
            )
        }
        add(windLine(context, units, report))
        report.airQuality?.let { air ->
            add(
                context.getString(
                    R.string.notif_detail_air,
                    air.aqiIndex,
                    context.getString(WeatherText.aqiMeaning(air.aqiIndex))
                )
            )
        }
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
    private fun windLine(context: Context, units: UnitSettings, report: WeatherReport): String =
        context.getString(
            R.string.notif_detail_wind,
            Formats.wind(report.current.wind.speedKph, units.windSpeed, Locale.getDefault()),
            context.getString(WeatherText.windMeaning(report.current.wind.speedKph))
        )

    private val AlertKind.channelId: String
        get() = when (this) {
            AlertKind.SEVERE -> "alert_severe"
            AlertKind.PRECIPITATION -> "alert_precip"
            AlertKind.DAILY_SUMMARY -> "alert_summary"
        }

    /** Fixed ids: a fresher same-kind alert replaces the old one, never stacks. */
    private val AlertKind.notificationId: Int
        get() = when (this) {
            AlertKind.SEVERE -> 1001
            AlertKind.PRECIPITATION -> 1002
            AlertKind.DAILY_SUMMARY -> 1003
        }

    private val AlertKind.titleRes: Int
        get() = when (this) {
            AlertKind.SEVERE -> R.string.notif_severe_title
            AlertKind.PRECIPITATION -> R.string.notif_precip_title
            AlertKind.DAILY_SUMMARY -> R.string.notif_summary_title
        }

    private val AlertKind.channelNameRes: Int
        get() = when (this) {
            AlertKind.SEVERE -> R.string.notif_channel_severe
            AlertKind.PRECIPITATION -> R.string.notif_channel_precip
            AlertKind.DAILY_SUMMARY -> R.string.notif_channel_summary
        }

    private fun ensureChannel(
        context: Context,
        manager: NotificationManagerCompat,
        kind: AlertKind
    ) {
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(
                kind.channelId,
                // The storm outranks the summary in the system's own vocabulary too.
                if (kind == AlertKind.SEVERE) {
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
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}
