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
import com.callbackdev.chiaro.domain.model.WeatherReport
import com.callbackdev.chiaro.domain.rules.RuleMessages
import com.callbackdev.chiaro.domain.rules.RuleTrigger
import com.callbackdev.chiaro.domain.rules.RuleVariables
import com.callbackdev.chiaro.domain.settings.UnitSettings
import com.callbackdev.chiaro.ui.alerts.RuleText
import java.time.LocalDateTime

/**
 * A fired rule of the reader's, as a notification. The body is their own message —
 * user content, in their language, never translated (VISION §8) — with the
 * `{placeholders}` interpolated in their units. The chrome names the rule and the
 * place, and that is all the chrome there is: the message is the point.
 *
 * Expanded, the message keeps its place at the top and the rule's ARITHMETIC goes
 * under it (Fase 6b): each condition as the sentence the Alerts screen shows, with
 * the value that was actually read beside it. A verdict ships with its arithmetic
 * (CLAUDE.md), and "why did this go off?" is the only question a fired rule ever
 * raises. Collapsed it stays the message alone — that is what the reader wrote it
 * for, and the system gives it one line.
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

        val message = RuleMessages.interpolate(
            trigger.rule.message, trigger, report, now, units,
            RuleText.MessageWriter(
                context.resources, units, context.resources.configuration.locales[0],
                android.text.format.DateFormat.is24HourFormat(context)
            )
        )
        val id = notificationId(trigger.rule.id)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_chiaro)
            .setColor(NotificationViews.accent(context))
            .setContentTitle(
                context.getString(R.string.notif_rule_title, trigger.rule.name, cityLabel)
            )
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(expanded(context, trigger, report, now, units, message))
            )
            .setContentIntent(openApp(context, id))
            .setAutoCancel(true)
        NotificationViews.quietAtNight(builder)
        val notification = builder.build()
        return try {
            manager.notify(id, notification)
            true
        } catch (e: SecurityException) {
            false // POST_NOTIFICATIONS revoked between the check and the post
        }
    }

    /**
     * The reader's message, then the conditions that made it fire, each with the
     * reading behind it: "rain in the next 6 hours at least 20% (now 75%)".
     *
     * A condition whose variable cannot be resolved right now (air quality down, an
     * empty window) prints its sentence WITHOUT a reading rather than a zero — the
     * engine already refuses to call missing data "false", and the notification must
     * not undo that in words (§1.1).
     */
    private fun expanded(
        context: Context,
        trigger: RuleTrigger,
        report: WeatherReport,
        now: LocalDateTime,
        units: UnitSettings,
        message: String
    ): String {
        val res = context.resources
        val locale = res.configuration.locales[0]
        val lines = trigger.rule.conditions.map { condition ->
            // A line of its own reads as a sentence, so it starts like one (23 set 2026:
            // the Alerts screen's fragment is lower-case because it follows «Quando»).
            val sentence = RuleText.sentence(res, condition, units)
                .replaceFirstChar { it.titlecase(locale) }
            val variable = RuleVariables.byId(condition.variable)
            // The reading with its unit and the reader's decimal mark: «21,4°», where it
            // printed «valore 21.4» — a number with no unit, in the code's decimal point.
            val reading = variable?.resolve?.invoke(report, now)
                ?.let { RuleText.reading(res, condition.variable, it.value, units, locale) }
            if (reading == null) {
                sentence
            } else {
                context.getString(R.string.notif_rule_condition, sentence, reading)
            }
        }
        if (lines.isEmpty()) return message
        return message + "\n\n" + context.getString(R.string.notif_rule_why) + "\n" +
            lines.joinToString("\n")
    }

    private fun openApp(context: Context, requestCode: Int): PendingIntent =
        PendingIntent.getActivity(
            context,
            requestCode,
            // Avvisi (21 set 2026): the rule that just fired is a card on that
            // screen, with the reader's own conditions on it and the hour it last
            // fired — which this notification has just changed.
            //
            // The request code is load-bearing now that the destination rides in the
            // extras: see [ShellDestination]. These are the rule ids, 2000-2999.
            ShellDestination.intent(context, MainActivity::class.java, ShellTab.ALERTS),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}
