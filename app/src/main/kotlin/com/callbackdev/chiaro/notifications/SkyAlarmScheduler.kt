package com.callbackdev.chiaro.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import com.callbackdev.chiaro.data.ActiveSource
import com.callbackdev.chiaro.data.ServiceLocator
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.sky.SkyJobCatalog
import com.callbackdev.chiaro.domain.sky.SkyLead
import com.callbackdev.chiaro.domain.sky.SkyReminder
import com.callbackdev.chiaro.domain.sky.SkyReminderPlanner
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.first

/**
 * Arms the next sky reminder (Fase 5). Inherited from tweather nearly verbatim,
 * because the reasoning is the product's:
 *
 * **Deliberately inexact.** `setAndAllowWhileIdle`, never `setExact*`, never
 * `SCHEDULE_EXACT_ALARM`: battery is a feature and a sunset is not an alarm clock.
 * The cost is a drift of about ten minutes, which is exactly why the shortest lead a
 * bell offers is fifteen and why the screen says the reminder is approximate.
 *
 * **One alarm at a time.** The nearest reminder is armed; when it fires,
 * [SkyAlarmReceiver] posts it (or declines to) and arms the next. A queue of alarms
 * would buy nothing — the plan is recomputed on every edit — and would cost a
 * cancel-and-rearm of the whole queue every time a bell changed.
 *
 * **Only the active place.** The reminders follow the place the app is on, with the
 * last persisted GPS fix standing in when position is the source: there is no
 * background location in this app.
 */
object SkyAlarmScheduler {

    const val ACTION_FIRE = "com.callbackdev.chiaro.SKY_REMINDER"
    const val EXTRA_JOB_ID = "job_id"
    const val EXTRA_OCCURRENCE = "occurrence_epoch_s"

    /**
     * Recomputes the plan and arms (or clears) the alarm. Called after every edit on
     * the Sky screen, when a reminder fires, on boot, and on app start.
     */
    suspend fun reschedule(context: Context) {
        val settings = ServiceLocator.settingsStore(context).settings.first()
        val alarms = context.getSystemService<AlarmManager>() ?: return
        val plan = if (settings.skyEnabled) plan(context) else null
        if (plan == null) {
            alarms.cancel(pendingIntent(context, null))
            return
        }
        // FLAG_UPDATE_CURRENT on one fixed request code: arming the next reminder
        // replaces the previous alarm rather than adding to it, so the app can never
        // accumulate a queue it forgot about.
        alarms.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            plan.fireAt.toEpochMilli(),
            pendingIntent(context, plan)
        )
    }

    /** The next reminder for the active place, or null when there is nothing to arm. */
    suspend fun plan(context: Context, now: Instant = Instant.now()): SkyReminder? {
        val city = activeCity(context) ?: return null
        val subscriptions = ServiceLocator.skySubscriptionStore(context).subscriptions.first()
        // The default lead resolved exactly as the Sky screen resolves it: the bells
        // and the alarm must not be able to disagree about which moments remind.
        val defaultLead = ServiceLocator.settingsStore(context)
            .settings.first().skyNotifyDefaultMin
        val jobs = subscriptions
            .filter { it.enabled }
            .mapNotNull { subscription ->
                SkyJobCatalog.byId(subscription.jobId)?.let {
                    it to SkyLead.ofMinutes(subscription.notifyLeadMinutes ?: defaultLead)
                }
            }
        if (jobs.isEmpty()) return null
        val zone = city.timezone
            ?.let { runCatching { ZoneId.of(it) }.getOrNull() }
            ?: ZoneId.systemDefault()
        return SkyReminderPlanner.next(jobs, now, zone, city.coordinates)
    }

    suspend fun activeCity(context: Context): City? =
        when (val source = ServiceLocator.cityStore(context).activeSource.first()) {
            is ActiveSource.Saved -> source.city
            is ActiveSource.Gps -> source.lastFix
            ActiveSource.None -> null
        }

    private fun pendingIntent(context: Context, reminder: SkyReminder?): PendingIntent {
        val intent = Intent(context, SkyAlarmReceiver::class.java).setAction(ACTION_FIRE)
        reminder?.let {
            intent.putExtra(EXTRA_JOB_ID, it.jobId)
            intent.putExtra(EXTRA_OCCURRENCE, it.occurrenceAt.epochSecond)
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** One code, because there is one alarm. */
    private const val REQUEST_CODE = 0x5C1
}
