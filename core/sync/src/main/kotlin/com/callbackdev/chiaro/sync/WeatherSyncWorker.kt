package com.callbackdev.chiaro.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.callbackdev.chiaro.data.ActiveSource
import com.callbackdev.chiaro.data.FetchFailureReason
import com.callbackdev.chiaro.data.ServiceLocator
import com.callbackdev.chiaro.domain.AlertEngine
import com.callbackdev.chiaro.domain.WeatherException
import com.callbackdev.chiaro.domain.WeatherFreshness
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.GpsCityId
import com.callbackdev.chiaro.domain.model.WeatherReport
import com.callbackdev.chiaro.domain.rules.RuleEngine
import com.callbackdev.chiaro.domain.sky.SkyJobCatalog
import com.callbackdev.chiaro.domain.sky.SkyRunRecorder
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.first

/**
 * The single periodic background job (Fase 6, see [SyncScheduler]): fetch weather
 * for the active place, evaluate the built-in alerts and the reader's rules, record
 * what the sky did, re-arm the reminder alarm. Named "sync", not "alerts" — the
 * fetch is the reusable part, and the widgets of Fase 8 will re-render off the same
 * run. Battery: one fetch (two HTTP GETs — forecast + air quality) per period, and a
 * cache HIT is free when the reader just used the app.
 *
 * Inherited from tweather minus its widget legs; what it says when something fires
 * is [SyncNotifiers]' business, in :app.
 */
class WeatherSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val notifiers = SyncDependencies.notifiers ?: return Result.success()
        val settings = ServiceLocator.settingsStore(context).settings.first()
        val enabledRules = ServiceLocator.ruleStore(context).rules.first().filter { it.enabled }
        val alertsWanted = SyncScheduler.alertsWanted(
            settings.notifications, notifiers.notificationsEnabled(), enabledRules.isNotEmpty()
        )
        // Self-heal: nothing left to sync for (no alerts wanted AND no widget
        // placed) — cancel instead of waking up for nothing forever (the Alerts
        // screen, the widget receivers and app start re-enqueue if conditions return).
        val widgets = SyncDependencies.widgets
        if (!alertsWanted && widgets?.hasWidgets() != true) {
            SyncScheduler.cancel(context)
            return Result.success()
        }

        val city = when (val source = ServiceLocator.cityStore(context).activeSource.first()) {
            is ActiveSource.Saved -> source.city
            // Background location is off the table by design: last persisted fix only
            is ActiveSource.Gps -> source.lastFix ?: return Result.success()
            ActiveSource.None -> return Result.success()
        }

        val report = try {
            ServiceLocator.weatherRepository(context).getWeather(
                city,
                forceRefresh = false,
                ttl = Duration.ofMinutes(settings.updateFrequencyMin.toLong())
            )
        } catch (e: WeatherException.NoNetwork) {
            // The Journal is where offline honesty lives (Fase 7): a fetch that
            // could not land is an entry, not a silent gap. And this is exactly when
            // a widget must stop presenting old numbers as current (Fase 8): repaint
            // so its stale marker can appear.
            recordFailure(context, city, FetchFailureReason.OFFLINE)
            widgets?.repaintAll()
            return Result.retry() // captive portal/DNS flap; CONNECTED already gated
        } catch (e: WeatherException) {
            recordFailure(
                context, city,
                if (e is WeatherException.ApiError) FetchFailureReason.SERVICE
                else FetchFailureReason.UNKNOWN
            )
            widgets?.repaintAll()
            return Result.success() // next period is at most one interval away
        }

        // A widget-only sync fetches (the repository's commit hook repaints) but
        // must never evaluate or post alerts.
        if (!alertsWanted) return Result.success()

        // Alerts and rules run in the CITY's timezone, not the device's.
        val zone = runCatching { ZoneId.of(report.location.timezone) }
            .getOrDefault(ZoneId.systemDefault())
        val now = ZonedDateTime.now(zone).toLocalDateTime()
        // Stable identity, not cacheKey: the GPS pseudo-city's cacheKey moves with
        // the fix (~1.1 km grid) and would re-notify the same storm at every commute
        // leg; ids never move.
        val cityKey = if (city.id == GpsCityId) "gps" else city.id.toString()

        val stateStore = ServiceLocator.alertStateStore(context)
        val alerts = AlertEngine.evaluate(
            report = report,
            settings = settings.notifications,
            state = stateStore.state.first(),
            now = now,
            cityKey = cityKey
        )
        alerts.forEach { alert ->
            // Fingerprint burns only on a successful post (muted channel → retry later)
            if (notifiers.notifyAlert(alert, report, settings.units)) {
                stateStore.record(alert)
            }
        }

        // The reader's own rules: same fetch, same clock, zero extra battery.
        if (settings.notifications.userRules && enabledRules.isNotEmpty()) {
            val ruleStateStore = ServiceLocator.ruleStateStore(context)
            val evaluation = RuleEngine.evaluate(
                rules = enabledRules,
                report = report,
                state = ruleStateStore.state.first(),
                now = now,
                cityKey = cityKey
            )
            // Re-arm regardless of what posts: false is false
            ruleStateStore.unlatch(evaluation.unlatch)
            val fired = mutableListOf<String>()
            evaluation.triggers.forEach { trigger ->
                val posted = notifiers.notifyRule(
                    trigger, report.location.city, report, now, settings.units
                )
                if (posted) {
                    ruleStateStore.record(trigger)
                    fired += trigger.rule.name
                }
            }
            // The Journal's food (Fase 7): this fetch's commit lists what fired.
            if (fired.isNotEmpty()) {
                ServiceLocator.weatherRepository(context).recordFiredRules(city, fired)
            }
        }

        // Widgets pinned to another city have no other producer of history commits:
        // without this their data would only age (device review, 3 set). One extra
        // fetch per distinct pinned city per period, only while such a widget exists.
        widgets?.pinnedCities().orEmpty()
            .filter { it.cacheKey != city.cacheKey }
            .distinctBy { it.cacheKey }
            .forEach { pinned ->
                runCatching {
                    ServiceLocator.weatherRepository(context).getWeather(
                        pinned,
                        forceRefresh = false,
                        ttl = Duration.ofMinutes(settings.updateFrequencyMin.toLong())
                    )
                }
            }

        // The sky (Fase 5–7). Deliberately OUTSIDE the alerts gate in spirit —
        // recording is not notifying — but this worker only runs when alerts are
        // wanted until Fase 8 gives it a second customer.
        if (settings.skyEnabled) {
            recordSkyRuns(context, city, report, settings.updateFrequencyMin)
            // The receiver arms the next reminder when one fires; this is the safety
            // net — an alarm lost to a force-stop comes back at the next fetch.
            notifiers.rearmSkyReminders()
        }

        return Result.success()
    }

    private suspend fun recordFailure(
        context: Context,
        city: City,
        reason: FetchFailureReason
    ) {
        runCatching {
            ServiceLocator.fetchLogStore(context)
                .record(city.cacheKey, Instant.now().epochSecond, reason)
        }
    }

    /**
     * Which sky moments ran since this city's previous commit, attached to the one
     * this fetch just wrote.
     *
     * The previous commit IS the "since": it is precisely the last moment the app
     * looked at this city, so the window between them is everything that happened
     * while it was not looking. On the very first commit there is no previous one
     * and nothing to have missed, so nothing is recorded — an install does not
     * acquire a history of sunsets it was not installed for.
     */
    private suspend fun recordSkyRuns(
        context: Context,
        city: City,
        report: WeatherReport,
        updateFrequencyMin: Int
    ) {
        val subscriptions = ServiceLocator.skySubscriptionStore(context).subscriptions.first()
        val jobs = subscriptions.filter { it.enabled }.mapNotNull { SkyJobCatalog.byId(it.jobId) }
        if (jobs.isEmpty()) return
        val repository = ServiceLocator.weatherRepository(context)
        val history = repository.historyFor(city, limit = 2)
        val previous = history.getOrNull(1) ?: return
        val zone = runCatching { ZoneId.of(report.location.timezone) }
            .getOrDefault(ZoneId.systemDefault())
        val runs = SkyRunRecorder.runsSince(
            since = Instant.ofEpochSecond(previous.timestampEpochSeconds),
            now = report.systemInfo.lastSync,
            jobs = jobs,
            zone = zone,
            coordinates = city.coordinates,
            hours = report.hourly,
            dataAge = Duration.ZERO,
            staleAfter = WeatherFreshness.staleAfter(updateFrequencyMin)
        )
        repository.recordSkyRuns(city, runs)
    }
}
