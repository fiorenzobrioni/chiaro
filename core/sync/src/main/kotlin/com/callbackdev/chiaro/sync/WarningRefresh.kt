package com.callbackdev.chiaro.sync

import android.content.Context
import com.callbackdev.chiaro.data.ActiveSource
import com.callbackdev.chiaro.data.ServiceLocator
import com.callbackdev.chiaro.domain.model.GpsCityId
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.first

/**
 * The bulletin, fetched because a SCREEN asked — not because the periodic job woke up.
 *
 * **Why this exists** (found 11 set 2026, and it is a gap Fase 11 left). The plan for that
 * phase said the bulletin is looked for «all'apertura di Oggi… un trascinamento per
 * aggiornare lo forza», and it was never wired: [OfficialWarningsStep] ran from
 * [WeatherSyncWorker] and nowhere else, `SyncScheduler` only ever enqueued *periodic* work,
 * and Today's pull to refresh went to the repository without touching the step. On a fresh
 * install nothing official appeared until WorkManager first fired — up to an hour at the
 * default interval, and the reader had no way to ask.
 *
 * **What it is not.** It does not schedule anything and it does not fetch weather: it is the
 * one leg of the job that a screen may run on its own, and it runs **silently** — it hands
 * the step no notifiers, so a warning found here reaches the banner and the widgets but
 * posts no notification. That is the same division the rest of the app already follows:
 * opening Today fetches the weather and never posts an alert; speaking is the job's.
 * Leaving the fingerprint unburnt is deliberate too — if the reader closes the app before
 * the warning registers, the job will still say it.
 *
 * Everything expensive is already gated inside the step: it is inert when no saved place is
 * graded, its cadence decides whether the network is touched at all, and it never throws.
 *
 * **One at a time.** Today is a pager, so landing on it subscribes one flow per saved place
 * and each of them asks — and the step's cadence does NOT stop them on a fresh install,
 * where `shouldFetch` answers true for as long as there is no bulletin in hand. Without a
 * guard, five saved places would mean five simultaneous downloads of the same bulletin. The
 * first caller wins and the rest return at once rather than queueing: they all wanted the
 * same document, and the store collector hands it to every page anyway.
 */
object WarningRefresh {

    private val running = AtomicBoolean(false)

    /**
     * Runs the warnings leg for the active place, or does nothing when there is no active
     * place. Never throws: a screen that asked for a bulletin and did not get one shows the
     * bulletin it already had, which is the whole contract of that surface.
     *
     * @param force skip the fetch cadence — what a pull to refresh means. A [force] of false
     *   still respects the issuer's publication window, so landing on Today at nine in the
     *   morning costs nothing.
     */
    suspend fun run(context: Context, force: Boolean = false) {
        if (!running.compareAndSet(false, true)) return
        try {
            fetch(context, force)
        } finally {
            running.set(false)
        }
    }

    private suspend fun fetch(context: Context, force: Boolean) {
        runCatching {
            val city = when (val source = ServiceLocator.cityStore(context).activeSource.first()) {
                is ActiveSource.Saved -> source.city
                // Background location is off the table by design: last persisted fix only.
                is ActiveSource.Gps -> source.lastFix ?: return
                ActiveSource.None -> return
            }
            // Stable identity, not cacheKey — the same rule the worker follows, so the two
            // paths cannot burn different fingerprints for one place.
            val cityKey = if (city.id == GpsCityId) "gps" else city.id.toString()
            OfficialWarningsStep(context).run(
                active = city,
                cityKey = cityKey,
                others = ServiceLocator.cityStore(context).cities.first(),
                settings = ServiceLocator.settingsStore(context).settings.first().notifications,
                // No notifiers: this run fetches, it does not speak.
                notifiers = null,
                force = force
            )
        }
    }
}
