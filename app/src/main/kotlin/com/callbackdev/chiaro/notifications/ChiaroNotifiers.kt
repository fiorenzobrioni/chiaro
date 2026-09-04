package com.callbackdev.chiaro.notifications

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.callbackdev.chiaro.domain.Alert
import com.callbackdev.chiaro.domain.model.WeatherReport
import com.callbackdev.chiaro.domain.rules.RuleTrigger
import com.callbackdev.chiaro.domain.settings.UnitSettings
import com.callbackdev.chiaro.sync.SyncNotifiers
import java.time.LocalDateTime

/**
 * The app's answer to [SyncNotifiers] (Fase 6): the worker in `:core:sync` decides
 * when something is worth saying, these three objects decide how Chiaro says it.
 * Installed by `ChiaroApplication`, exactly as `ServiceLocator.install` hands the
 * data layer its User-Agent.
 */
class ChiaroNotifiers(private val context: Context) : SyncNotifiers {

    override fun notificationsEnabled(): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    override fun notifyAlert(
        alert: Alert,
        report: WeatherReport,
        units: UnitSettings
    ): Boolean = AlertNotifier.notify(context, alert, report, units)

    override fun notifyRule(
        trigger: RuleTrigger,
        cityLabel: String,
        report: WeatherReport,
        now: LocalDateTime,
        units: UnitSettings
    ): Boolean = RuleNotifier.notify(context, trigger, cityLabel, report, now, units)

    override suspend fun rearmSkyReminders() {
        runCatching { SkyAlarmScheduler.reschedule(context) }
    }
}
