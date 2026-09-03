package com.callbackdev.chiaro.sync

import com.callbackdev.chiaro.domain.Alert
import com.callbackdev.chiaro.domain.model.WeatherReport
import com.callbackdev.chiaro.domain.rules.RuleTrigger
import com.callbackdev.chiaro.domain.settings.TemperatureUnit
import com.callbackdev.chiaro.domain.settings.UnitSettings
import java.time.LocalDateTime

/**
 * What the worker asks of the app (Fase 6). Notifiers are TEXT — titles, bodies,
 * channels — and text is presentation, which a `:core:*` module must not own. This
 * module decides when something is worth saying; the app decides how to say it.
 *
 * Every `notify*` returns whether the notification actually posted: a fingerprint
 * burns only on a successful post, so a muted channel never consumes the one chance
 * an alert had.
 */
interface SyncNotifiers {

    /** Whether the system will show anything at all right now. */
    fun notificationsEnabled(): Boolean

    fun notifyAlert(alert: Alert, temperature: TemperatureUnit): Boolean

    fun notifyRule(
        trigger: RuleTrigger,
        cityLabel: String,
        report: WeatherReport,
        now: LocalDateTime,
        units: UnitSettings
    ): Boolean

    /** Re-arm the sky reminder alarm: the periodic run is its safety net — an alarm
     * lost to a force-stop comes back at the next fetch instead of never. */
    suspend fun rearmSkyReminders()
}

/**
 * The widgets, as the worker sees them (Fase 8): whether any are placed — a placed
 * widget keeps the periodic job alive on its own — and how to repaint them all,
 * which the worker does after a FAILED fetch so the stale marker can appear (a
 * successful one repaints through the repository's commit hook instead).
 */
interface SyncWidgets {
    fun hasWidgets(): Boolean
    suspend fun repaintAll()
}

/**
 * The worker's one seam to the app, in the exact shape of `ServiceLocator.install`
 * (Fase 0): the library does not know the app, so the app introduces itself at
 * startup. `ChiaroApplication` calls [install] before any work can run — a worker
 * only ever executes after `Application.onCreate` in its process.
 */
object SyncDependencies {

    @Volatile
    var notifiers: SyncNotifiers? = null
        private set

    @Volatile
    var widgets: SyncWidgets? = null
        private set

    fun install(notifiers: SyncNotifiers, widgets: SyncWidgets? = null) {
        this.notifiers = notifiers
        this.widgets = widgets
    }
}
