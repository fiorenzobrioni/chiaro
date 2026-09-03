package com.callbackdev.chiaro.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.callbackdev.chiaro.MainActivity
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.model.WeatherReport
import com.callbackdev.chiaro.domain.rules.RuleMessages
import com.callbackdev.chiaro.domain.rules.RuleTrigger
import com.callbackdev.chiaro.domain.settings.UnitSettings
import java.time.LocalDateTime

/**
 * A fired rule of the reader's, as a notification. The body is their own message —
 * user content, in their language, never translated (VISION §8) — with the
 * `{placeholders}` interpolated in their units. The chrome names the rule and the
 * place, and that is all the chrome there is: the message is the point.
 *
 * One channel for every rule, one notification id per rule: a re-fire of the same
 * rule overwrites, different rules stack.
 */
object RuleNotifier {

    const val CHANNEL_ID = "user_rules"

    /** Fixed ids 1001–1003 belong to the built-in alerts; rules live above 2000. */
    private const val NOTIFICATION_ID_BASE = 2000

    internal fun notificationId(ruleId: Long): Int =
        NOTIFICATION_ID_BASE + (ruleId % 1000).toInt()

    /**
     * Posts the notification; false when notifications are off or the channel is
     * muted — the caller must then NOT record the trigger, so it can retry later.
     */
    fun notify(
        context: Context,
        trigger: RuleTrigger,
        cityLabel: String,
        report: WeatherReport,
        now: LocalDateTime,
        units: UnitSettings
    ): Boolean {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(
                CHANNEL_ID,
                NotificationManagerCompat.IMPORTANCE_DEFAULT
            )
                .setName(context.getString(R.string.notif_channel_rules))
                .build()
        )
        if (manager.getNotificationChannelCompat(CHANNEL_ID)?.importance ==
            NotificationManagerCompat.IMPORTANCE_NONE
        ) {
            return false
        }

        val message = RuleMessages.interpolate(trigger.rule.message, trigger, report, now, units)
        val id = notificationId(trigger.rule.id)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_chiaro)
            .setContentTitle(
                context.getString(R.string.notif_rule_title, trigger.rule.name, cityLabel)
            )
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(openApp(context, id))
            .setAutoCancel(true)
            .build()
        return try {
            manager.notify(id, notification)
            true
        } catch (e: SecurityException) {
            false // POST_NOTIFICATIONS revoked between the check and the post
        }
    }

    private fun openApp(context: Context, requestCode: Int): PendingIntent =
        PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}
