package com.callbackdev.chiaro.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.callbackdev.chiaro.MainActivity
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.sky.SkyJobCatalog
import com.callbackdev.chiaro.domain.sky.SkyVerdict
import com.callbackdev.chiaro.domain.sky.SkyVerdictKind
import com.callbackdev.chiaro.ui.sky.SkyText
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * The sky reminder as a system notification (Fase 5). The title is the moment's own
 * name in the reader's language; the body is the lead, the time, and the verdict
 * WITH its number — a reminder that says "clear" without the figure it read is an
 * opinion, and this app does not send opinions. The dotted job ids never appear.
 *
 * One channel, one notification id per job: a second reminder for the same moment
 * replaces the first instead of stacking.
 */
object SkyNotifier {

    const val CHANNEL_ID = "sky_reminders"

    /**
     * Posts the reminder; false when notifications are off or the channel is muted —
     * the caller must then NOT burn the fingerprint, so the reminder can still fire
     * if the reader re-enables the channel.
     */
    fun notify(
        context: Context,
        jobId: String,
        occurrenceAt: Instant,
        zone: ZoneId,
        verdict: SkyVerdict?,
        now: Instant
    ): Boolean {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false
        ensureChannel(context, manager)
        if (manager.getNotificationChannelCompat(CHANNEL_ID)?.importance ==
            NotificationManagerCompat.IMPORTANCE_NONE
        ) {
            return false
        }

        val body = body(context, occurrenceAt, zone, verdict, now)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_chiaro)
            .setContentTitle(context.getString(SkyText.nameRes(jobId)))
            .setContentText(body)
            // Same text expanded: the line is short enough to fit, and BigTextStyle
            // is what stops the system from eliding the verdict on a narrow screen.
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openApp(context))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .build()
        return try {
            manager.notify(notificationId(jobId), notification)
            true
        } catch (e: SecurityException) {
            false // POST_NOTIFICATIONS revoked between the check and the post
        }
    }

    /** "Tra 30 minuti, alle 18:32 · Bello, nuvole 8%" — lead, clock time, verdict. */
    private fun body(
        context: Context,
        occurrenceAt: Instant,
        zone: ZoneId,
        verdict: SkyVerdict?,
        now: Instant
    ): String {
        val minutes = Duration.between(now, occurrenceAt).toMinutes().coerceAtLeast(0).toInt()
        val time = occurrenceAt.atZone(zone).format(clockFormat(context))
        val lead = context.resources
            .getQuantityString(R.plurals.sky_notification_in, minutes, minutes, time)
        val verdictLine = verdict?.let { verdictLine(context, it) }
        return listOfNotNull(lead, verdictLine).joinToString(" · ")
    }

    /** The verdict word and its arithmetic, or the honest reason there is none. */
    private fun verdictLine(context: Context, verdict: SkyVerdict): String {
        val word = context.getString(SkyText.verdictWordRes(verdict.kind))
        val detail = if (verdict.kind == SkyVerdictKind.UNKNOWN) {
            SkyText.unknownReason(context.resources, verdict)
        } else {
            SkyText.chipEvidence(context.resources, verdict)
        }
        return if (detail != null) "$word, $detail" else word
    }

    private fun clockFormat(context: Context): DateTimeFormatter =
        DateTimeFormatter.ofPattern(
            if (android.text.format.DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
        )

    /**
     * One id per job: a second reminder for the same job replaces the first. Derived
     * from the catalog position rather than `jobId.hashCode()` — two colliding hashes
     * would silently make one moment's reminder overwrite another's.
     */
    private fun notificationId(jobId: String): Int =
        7000 + SkyJobCatalog.all.indexOfFirst { it.id == jobId }.coerceAtLeast(0)

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun ensureChannel(context: Context, manager: NotificationManagerCompat) {
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(
                CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT
            )
                .setName(context.getString(R.string.sky_channel_name))
                .setDescription(context.getString(R.string.sky_channel_description))
                .build()
        )
    }
}
