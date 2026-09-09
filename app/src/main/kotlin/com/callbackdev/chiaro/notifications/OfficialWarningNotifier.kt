package com.callbackdev.chiaro.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.callbackdev.chiaro.MainActivity
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.GpsCityId
import com.callbackdev.chiaro.domain.warnings.PlaceWarnings
import com.callbackdev.chiaro.domain.warnings.WarningHazard
import com.callbackdev.chiaro.domain.warnings.WarningLevel
import com.callbackdev.chiaro.domain.warnings.WarningNotification
import com.callbackdev.chiaro.ui.warnings.WarningText
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * An official warning as a system notification (Fase 11). The title is the level and
 * the place («Allerta arancione · Milano»); the collapsed body is one sentence — the
 * hazards, the day, the bulletin's hour; expanded, the same sentence leads and the
 * facts follow one per line, each present only if it has its data: the levels per
 * day, the zone, what the level means in the Dipartimento's words, the bulletin's
 * note when it concerns this zone, and the source last. Zone codes and bulletin ids
 * never appear (`OfficialWarningNotifierTest` checks).
 *
 * Two channels, because Android lets the reader silence the yellow from the system
 * without losing the orange, and the «Avvisami da» row in Avvisi does the same from
 * inside: two honest roads to one choice. One fixed id per place: a new bulletin
 * replaces the old notification instead of stacking.
 */
object OfficialWarningNotifier {

    const val CHANNEL_HIGH = "warning_high"
    const val CHANNEL_YELLOW = "warning_yellow"

    /**
     * Posts the warning; false when notifications are off or the channel is muted —
     * the caller must then NOT burn the fingerprint. [today] is the issuer's day.
     */
    fun notify(
        context: Context,
        notification: WarningNotification,
        city: City,
        today: LocalDate
    ): Boolean {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false
        val warnings = notification.warnings
        val level = warnings.maxLevel
        if (level == WarningLevel.NONE) return false
        val channel = channelFor(level)
        ensureChannels(context, manager)
        if (manager.getNotificationChannelCompat(channel)?.importance ==
            NotificationManagerCompat.IMPORTANCE_NONE
        ) {
            return false
        }

        val headline = collapsed(context, warnings, today)
        val built = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_chiaro)
            .setContentTitle(
                context.getString(
                    R.string.notif_warning_title,
                    context.getString(WarningText.phraseRes(level)),
                    city.name
                )
            )
            .setContentText(headline)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(headline + "\n\n" + expanded(context, warnings, today).joinToString("\n"))
            )
            .setContentIntent(openApp(context, notificationId(city)))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
        return try {
            manager.notify(notificationId(city), built)
            true
        } catch (e: SecurityException) {
            false // POST_NOTIFICATIONS revoked between the check and the post
        }
    }

    /**
     * «Temporali, oggi fino a mezzanotte · bollettino delle 15:19»: the hazards at the
     * highest level, on the day(s) that carry it, and when the bulletin was issued.
     */
    internal fun collapsed(context: Context, warnings: PlaceWarnings, today: LocalDate): String {
        val level = warnings.maxLevel
        val peakDays = warnings.peakDays
        val hazards = WarningHazard.displayOrder.filter { hazard ->
            peakDays.any { it.levels[hazard] == level }
        }
        val hazardWords = WarningText.hazards(context, hazards, capitalize = true)
        val dayPhrase = WarningText.days(context, peakDays.map { it.date }, today)
        return context.getString(
            R.string.notif_warning_collapsed,
            hazardWords,
            dayPhrase,
            context.getString(R.string.notif_warning_bulletin_at, issuedTime(context, warnings))
        )
    }

    /** The facts under the headline, worth-first, each only if it has its data. */
    internal fun expanded(context: Context, warnings: PlaceWarnings, today: LocalDate): List<String> =
        buildList {
            warnings.days.forEach { day ->
                val ranked = day.ranked
                if (ranked.isEmpty()) return@forEach
                val levels = ranked.joinToString(" · ") { (hazard, level) ->
                    context.getString(WarningText.shortHazardRes(hazard)) + " " + context.getString(WarningText.levelWordRes(level))
                }
                add(
                    when (day.date) {
                        today -> context.getString(R.string.notif_warning_day_today, levels)
                        today.plusDays(1) -> context.getString(R.string.notif_warning_day_tomorrow, levels)
                        else -> context.getString(
                            R.string.notif_warning_day_other,
                            day.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                            levels
                        )
                    }
                )
            }
            // The zone's own line, and only when the zone HAS a name: for the thirteen
            // the Region never named there is nothing to print here that is not a code,
            // and a line without its data is not drawn (this notifier's own rule).
            warnings.zone.takeIf { it.named }?.let {
                add(context.getString(R.string.notif_warning_zone, it.name))
            }
            add(context.getString(R.string.notif_warning_meaning, context.getString(WarningText.meaningRes(warnings.maxLevel))))
            warnings.note?.let { add(context.getString(R.string.notif_warning_note, it)) }
            add(context.getString(R.string.notif_warning_source, issuedTime(context, warnings)))
        }

    private fun issuedTime(context: Context, warnings: PlaceWarnings): String =
        warnings.issuedAt.toLocalTime().format(clockFormat(context))

    private fun channelFor(level: WarningLevel): String =
        if (level >= WarningLevel.ORANGE) CHANNEL_HIGH else CHANNEL_YELLOW

    /**
     * One id per place: `3000` for the position, `3000 + id % 1000` for a saved place —
     * a new bulletin for the same place replaces the notification, never stacks.
     */
    internal fun notificationId(city: City): Int =
        if (city.id == GpsCityId) 3000 else 3000 + (city.id % 1000).toInt()

    private fun ensureChannels(context: Context, manager: NotificationManagerCompat) {
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_HIGH, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName(context.getString(R.string.notif_channel_warning_high))
                .build()
        )
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_YELLOW, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.notif_channel_warning_yellow))
                .build()
        )
    }

    private fun clockFormat(context: Context): DateTimeFormatter =
        DateTimeFormatter.ofPattern(
            if (android.text.format.DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
        )

    /** Oggi shows the banner first under the canvas: no deep link needed (PLANNING, Fase 11). */
    private fun openApp(context: Context, requestCode: Int): PendingIntent =
        PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}
