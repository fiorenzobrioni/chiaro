package com.callbackdev.chiaro.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.callbackdev.chiaro.data.ActiveSource
import com.callbackdev.chiaro.data.AppSettings
import com.callbackdev.chiaro.data.GpsArrival
import com.callbackdev.chiaro.data.ServiceLocator
import com.callbackdev.chiaro.domain.AlertEngine
import com.callbackdev.chiaro.domain.WeatherException
import com.callbackdev.chiaro.domain.zone
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.WeatherReport
import com.callbackdev.chiaro.domain.model.notificationKey
import com.callbackdev.chiaro.domain.rules.NotificationRule
import com.callbackdev.chiaro.domain.rules.RuleEngine
import com.callbackdev.chiaro.domain.rules.RulePlaces
import com.callbackdev.chiaro.domain.settings.UnitSettings
import java.time.LocalDateTime
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.first

/**
 * The single periodic background job (Fase 6, see [SyncScheduler]): fetch weather
 * for the active place, evaluate the built-in alerts and the reader's rules, re-arm
 * the sky reminders. Named "sync", not "alerts" — the
 * fetch is the reusable part, and the widgets of Fase 8 will re-render off the same
 * run. Battery: one fetch (two HTTP GETs — forecast + air quality) per period, and a
 * cache HIT is free when the reader just used the app; one more per saved place a rule
 * is bound to (15.11) and per city pinned to a widget, only while there is one.
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
        val cityStore = ServiceLocator.cityStore(context)
        val saved = cityStore.cities.first()
        val ruleStore = ServiceLocator.ruleStore(context)
        // A rule bound to a place the reader has removed does not move to another place in
        // silence (15.11, 29 Oct 2026): it goes off here, at the first run after the removal,
        // and its card says the place is gone. Before the gate, so that a rule which can no
        // longer speak stops keeping the job alive this very run.
        ruleStore.switchOffOrphans(saved.map { it.id }.toSet())
        val enabledRules = ruleStore.rules.first().filter { it.enabled }
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

        // The active place, or none (15.13, 29 Oct 2026): the position switched on with no
        // fix saved yet (just enabled, the service off, the permission taken away), or
        // nothing chosen at all. Its leg used to end the run here, before the places the
        // other rules are bound to and the widgets' cities, which are other places' and
        // have nothing to wait for: a «Snow at Bormio» stayed mute for as long as the
        // position did not arrive, and nothing said so.
        val city: City? = when (val source = cityStore.activeSource.first()) {
            is ActiveSource.Saved -> source.city
            // Background location is off the table by design: last persisted fix only.
            // A fix the app took and could not move to for want of its weather is one
            // of those (20 ott 2026): its weather is asked for here as well, with the
            // coordinates already in hand, so the move happens at the first run back
            // online and not only when the reader next opens the app.
            is ActiveSource.Gps -> {
                GpsArrival(
                    cityStore, ServiceLocator.weatherRepository(context), ServiceLocator.locationProvider(context)
                ).also { it.retry(); it.nameShown() }
                cityStore.locationSettings.first().gpsCity
            }
            ActiveSource.None -> null
        }

        val report: WeatherReport? = if (city == null) {
            null
        } else {
            try {
                // No `ttl` of its own since 6 set 2026: the repository's
                // WeatherFreshness.ProviderResolution applies here too. Handing it
                // `update_frequency_min` meant a foreground fetch from ninety minutes ago
                // could still satisfy this run — and this run evaluates the alerts.
                ServiceLocator.weatherRepository(context).getWeather(city, forceRefresh = false)
            } catch (e: WeatherException) {
                // Exactly when a widget must stop presenting old numbers as current (Fase
                // 8): repaint so its stale marker can appear.
                widgets?.repaintAll()
                // A failure that passes — a captive portal or a DNS flap (CONNECTED already
                // gated), the service dropping connections (17 ott 2026), a 503 at the peak
                // of the full hour, a 429 on a shared address (19 ott 2026) — ends the run
                // and asks for the retry: WorkManager's backoff tries the whole round again
                // well before the next period, and until then the one failure that waited
                // out the whole period was up to two hours of alerts and widgets on old
                // numbers. The request's own fault, any other 4xx, still waits for the next
                // period — it would fail the same way again — but only for this place: the
                // bound places and the widgets' cities go on below (15.13).
                if (e.passes) return Result.retry()
                null
            }
        }

        var retry = false
        if (city != null && report != null) {
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
                    others = saved,
                    settings = settings.notifications,
                    notifiers = notifiers
                )
            }

            // A widget-only sync fetches (the repository's commit hook repaints) but
            // must never evaluate or post alerts.
            if (alertsWanted) {
                // Alerts and rules run in the CITY's timezone, not the device's.
                val now = ZonedDateTime.now(report.zone()).toLocalDateTime()
                postAlerts(city, cityKey, report, now, settings, notifiers)
                // The reader's own rules: same fetch, same clock, zero extra battery.
                readRules(
                    enabledRules.filter { RulePlaces.readsActive(it, city) },
                    city, report, now, settings.units, notifiers
                )
            }
        }
        if (alertsWanted) {
            // The saved places the other rules are bound to (15.11, 29 Oct 2026): one fetch
            // each per period, 8.4 calls (PLANNING, «15.11»), each place's rules read on its
            // own forecast and in its own timezone. A place that fails is that place's alone:
            // a passing failure asks for the retry the active place's does (a rule is an
            // alert, and an hour late is late), any other waits for the next period. With no
            // active place every bound rule is read here, on its own (15.13).
            RulePlaces.elsewhere(enabledRules, city, saved).forEach { (place, rules) ->
                val placeReport = try {
                    ServiceLocator.weatherRepository(context).getWeather(place, forceRefresh = false)
                } catch (e: WeatherException) {
                    if (e.passes) retry = true
                    return@forEach
                }
                val placeNow = ZonedDateTime.now(placeReport.zone()).toLocalDateTime()
                readRules(rules, place, placeReport, placeNow, settings.units, notifiers)
            }
        }

        // Widgets pinned to another city have no other producer of history commits:
        // without this their data would only age (device review, 3 set). One extra
        // fetch per distinct pinned city per period, only while such a widget exists;
        // a city a rule is bound to was fetched just above and is a cache hit here. Run
        // on a widget-only sync too (15.11): the early return of the alerts gate used to
        // come first, and a pinned city was never fetched for a reader with no alerts.
        widgets?.pinnedCities().orEmpty()
            .filter { it.cacheKey != city?.cacheKey }
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
        // net — an alarm lost to a force-stop comes back at the next fetch of the active
        // place, whose sky the reminders are about.
        if (alertsWanted && report != null) notifiers.rearmSkyReminders()

        return if (retry) Result.retry() else Result.success()
    }

    /** The built-in alerts, for the active place only: they are the app's, not the reader's. */
    private suspend fun postAlerts(
        city: City,
        cityKey: String,
        report: WeatherReport,
        now: LocalDateTime,
        settings: AppSettings,
        notifiers: SyncNotifiers
    ) {
        val context = applicationContext
        val stateStore = ServiceLocator.alertStateStore(context)
        val alerts = AlertEngine.evaluate(
            report = report,
            settings = settings.notifications,
            state = stateStore.state.first(),
            now = now,
            cityKey = cityKey
        )
        // The warnings the step above left for this place (21 ott 2026): the summaries
        // lead with the day's sentence, whose first step they are. Read as the widgets
        // read them; a store that cannot be read is a sentence one step shorter, never a
        // summary not sent.
        val graded = if (alerts.isEmpty()) {
            null
        } else {
            runCatching { ServiceLocator.warningReader(context).graded(city) }.getOrNull()
        }
        alerts.forEach { alert ->
            // Fingerprint burns only on a successful post (muted channel → retry later)
            if (notifiers.notifyAlert(alert, report, settings.units, graded)) {
                stateStore.record(alert)
            }
        }
    }

    /**
     * [rules] read on [city]'s forecast and clock: the active place's own, or those of one saved
     * place they are bound to (15.11). The latches and fingerprints are filed under the place
     * ([notificationKey]), the notification names it (its own name: a cache hit can hand back
     * a report mapped for another record of the same cell), and what fired goes on the place's own
     * history commit, which Alerts reads «last fired» from.
     */
    private suspend fun readRules(
        rules: List<NotificationRule>,
        city: City,
        report: WeatherReport,
        now: LocalDateTime,
        units: UnitSettings,
        notifiers: SyncNotifiers
    ) {
        if (rules.isEmpty()) return
        val context = applicationContext
        val ruleStateStore = ServiceLocator.ruleStateStore(context)
        val evaluation = RuleEngine.evaluate(
            rules = rules,
            report = report,
            state = ruleStateStore.state.first(),
            now = now,
            cityKey = city.notificationKey
        )
        // Re-arm regardless of what posts: false is false
        ruleStateStore.unlatch(evaluation.unlatch)
        val fired = mutableListOf<NotificationRule>()
        evaluation.triggers.forEach { trigger ->
            val posted = notifiers.notifyRule(trigger, city.name, report, now, units)
            if (posted) {
                ruleStateStore.record(trigger)
                fired += trigger.rule
            }
        }
        // This fetch's commit lists what fired, by id and by name: Alerts reads «last
        // fired» off it.
        if (fired.isNotEmpty()) {
            ServiceLocator.weatherRepository(context).recordFiredRules(city, fired)
        }
    }

    /** A failure that passes (a dropped connection, a 503, a 429): the active place's leg
     * retries on these, and so does a bound place's. */
    private val WeatherException.passes: Boolean
        get() = this is WeatherException.NoNetwork ||
            this is WeatherException.Unreachable ||
            (this is WeatherException.ApiError && isTransient)
}
