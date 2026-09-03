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
import com.callbackdev.chiaro.domain.settings.TemperatureUnit
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
    fun notify(context: Context, alert: Alert, temperatureUnit: TemperatureUnit): Boolean {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false
        ensureChannel(context, manager, alert.kind)
        if (manager.getNotificationChannelCompat(alert.kind.channelId)?.importance ==
            NotificationManagerCompat.IMPORTANCE_NONE
        ) {
            return false
        }

        val body = body(context, alert, temperatureUnit)
        val notification = NotificationCompat.Builder(context, alert.kind.channelId)
            .setSmallIcon(R.drawable.ic_stat_chiaro)
            .setContentTitle(context.getString(alert.kind.titleRes, alert.cityLabel))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
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
