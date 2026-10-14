package com.callbackdev.chiaro.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.callbackdev.chiaro.data.ActiveSource
import com.callbackdev.chiaro.data.ServiceLocator
import com.callbackdev.chiaro.domain.AlertEngine
import com.callbackdev.chiaro.domain.WeatherException
import com.callbackdev.chiaro.domain.zone
import com.callbackdev.chiaro.domain.model.notificationKey
import com.callbackdev.chiaro.domain.rules.RuleEngine
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.first

/**
 * The single periodic background job (Fase 6, see [SyncScheduler]): fetch weather
 * for the active place, evaluate the built-in alerts and the reader's rules, re-arm
 * the sky reminders. Named "sync", not "alerts" — the
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
            // No `ttl` of its own since 6 set 2026: the repository's
            // WeatherFreshness.ProviderResolution applies here too. Handing it
            // `update_frequency_min` meant a foreground fetch from ninety minutes ago
            // could still satisfy this run — and this run evaluates the alerts.
            ServiceLocator.weatherRepository(context).getWeather(city, forceRefresh = false)
        } catch (e: WeatherException.NoNetwork) {
            // Exactly when a widget must stop presenting old numbers as current (Fase
            // 8): repaint so its stale marker can appear.
            widgets?.repaintAll()
            return Result.retry() // captive portal/DNS flap; CONNECTED already gated
        } catch (e: WeatherException) {
            widgets?.repaintAll()
            return Result.success() // next period is at most one interval away
        }

        // Stable identity, not cacheKey: see [notificationKey].
        val cityKey = city.notificationKey

        // The official warnings (Fase 11): one step per device, after the fetch and
        // BEFORE the alerts gate — the bulletin is content for Oggi and the widgets,
        // not only a notification, and whether it speaks is its own switch inside.
        // Inert when no saved place is in a graded zone; never lets a failure out.
        runCatching {
            OfficialWarningsStep(context).run(
                active = city,
                cityKey = cityKey,
                others = ServiceLocator.cityStore(context).cities.first(),
                settings = settings.notifications,
                notifiers = notifiers
            )
        }

        // A widget-only sync fetches (the repository's commit hook repaints) but
        // must never evaluate or post alerts.
        if (!alertsWanted) return Result.success()

        // Alerts and rules run in the CITY's timezone, not the device's.
        val zone = report.zone()
        val now = ZonedDateTime.now(zone).toLocalDateTime()

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
            // This fetch's commit lists what fired: Alerts reads «last fired» off it.
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
                        forceRefresh = false
                    )
                }
            }

        // The receiver arms the next sky reminder when one fires; this is the safety
        // net — an alarm lost to a force-stop comes back at the next fetch.
        if (settings.skyEnabled) notifiers.rearmSkyReminders()

        return Result.success()
    }

}
