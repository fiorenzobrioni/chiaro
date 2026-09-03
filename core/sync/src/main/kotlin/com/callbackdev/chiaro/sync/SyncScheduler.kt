package com.callbackdev.chiaro.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.callbackdev.chiaro.data.ServiceLocator
import com.callbackdev.chiaro.domain.settings.NotificationSettings
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

/**
 * Desired-state reconciliation for the single periodic background job (Fase 6),
 * inherited from tweather with its battery choices intact: no flex window (default =
 * whole period = maximum OS batching freedom), only a CONNECTED constraint, no
 * battery-not-low (it would suppress severe alerts exactly when they matter; flip
 * here if ever reconsidered), backoff only for the no-network edge. Survives reboots
 * via WorkManager itself.
 *
 * Called after anything that changes the answer: an alert toggle, a rule edit, the
 * update frequency, app start.
 */
object SyncScheduler {

    const val UNIQUE_NAME = "weather-sync"

    /**
     * Whether the job would post anything: gates the alert evaluation in the worker.
     * User rules count only while some exist and their master switch is on — an
     * empty rule list must not keep the phone polling.
     */
    fun alertsWanted(
        settings: NotificationSettings,
        notificationsEnabled: Boolean,
        hasEnabledRules: Boolean = false
    ): Boolean =
        notificationsEnabled &&
            (
                settings.severeWeatherAlerts || settings.dailySummary ||
                    settings.precipitationWarning || (settings.userRules && hasEnabledRules)
                )

    /**
     * Split out pure so the enqueue-vs-cancel decision is unit-testable. The widgets
     * of Fase 8 will add their own reason to keep the job alive; until then alerts
     * are the only customer.
     */
    fun shouldRun(
        settings: NotificationSettings,
        notificationsEnabled: Boolean,
        hasEnabledRules: Boolean = false
    ): Boolean = alertsWanted(settings, notificationsEnabled, hasEnabledRules)

    suspend fun reconcile(context: Context) {
        val settings = ServiceLocator.settingsStore(context).settings.first()
        val hasEnabledRules =
            ServiceLocator.ruleStore(context).rules.first().any { it.enabled }
        val notificationsEnabled =
            SyncDependencies.notifiers?.notificationsEnabled() ?: false
        if (shouldRun(settings.notifications, notificationsEnabled, hasEnabledRules)) {
            val request = PeriodicWorkRequestBuilder<WeatherSyncWorker>(
                settings.updateFrequencyMin.coerceAtLeast(15).toLong(), TimeUnit.MINUTES
            )
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()
            // UPDATE keeps the periodic cycle on frequency changes; idempotent
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(UNIQUE_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        } else {
            cancel(context)
        }
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME)
    }
}
