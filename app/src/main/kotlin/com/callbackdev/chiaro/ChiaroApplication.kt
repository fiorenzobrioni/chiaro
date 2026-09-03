package com.callbackdev.chiaro

import android.app.Application
import com.callbackdev.chiaro.data.ServiceLocator
import com.callbackdev.chiaro.notifications.ChiaroNotifiers
import com.callbackdev.chiaro.notifications.SkyAlarmScheduler
import com.callbackdev.chiaro.sync.SyncDependencies
import com.callbackdev.chiaro.sync.SyncScheduler
import com.callbackdev.chiaro.widget.ChiaroWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Installs what the libraries cannot know about the app: who it says it is to
 * Open-Meteo (Fase 0), how it speaks when something fires — the notifiers behind
 * `:core:sync`'s interface (Fase 6) — and, since Fase 8, who wants to hear that a
 * fetch committed new data: the widgets, which repaint off the same commit the app
 * reads, so home screen and app can never show two different afternoons.
 */
class ChiaroApplication : Application() {

    private val appScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        ServiceLocator.install(
            userAgent = "chiaro/${BuildConfig.VERSION_NAME} " +
                "(+https://github.com/fiorenzobrioni/chiaro)",
            onHistoryCommitted = {
                runCatching { ChiaroWidgets.updateAll(this@ChiaroApplication) }
            }
        )
        SyncDependencies.install(
            notifiers = ChiaroNotifiers(this),
            widgets = ChiaroWidgets.bridge(this)
        )
        // Two safety nets on every process start, milliseconds of DataStore reads
        // off the main thread: the sky alarm (a force-stop clears it without a boot,
        // Fase 5) and the periodic job's desired state (Fase 6).
        appScope.launch {
            runCatching { SkyAlarmScheduler.reschedule(this@ChiaroApplication) }
            runCatching { SyncScheduler.reconcile(this@ChiaroApplication) }
        }
        // The widgets follow the reader everywhere the data does not: a new active
        // place, a unit change, the icon style, the opacity. One process-lifetime
        // collector covers every path that edits either store (Fase 8).
        appScope.launch {
            combine(
                ServiceLocator.cityStore(this@ChiaroApplication).activeSource,
                ServiceLocator.settingsStore(this@ChiaroApplication).settings
            ) { active, settings -> active to settings }
                .drop(1) // the first emission is startup, not a change
                .collect {
                    runCatching { ChiaroWidgets.updateAll(this@ChiaroApplication) }
                }
        }
    }
}
