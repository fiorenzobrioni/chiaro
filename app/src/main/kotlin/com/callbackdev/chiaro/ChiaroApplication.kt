package com.callbackdev.chiaro

import android.app.Application
import com.callbackdev.chiaro.data.ServiceLocator
import com.callbackdev.chiaro.notifications.ChiaroNotifiers
import com.callbackdev.chiaro.notifications.SkyAlarmScheduler
import com.callbackdev.chiaro.sync.SyncDependencies
import com.callbackdev.chiaro.sync.SyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Installs what the libraries cannot know about the app: who it says it is to
 * Open-Meteo (Fase 0), and how it speaks when something fires — the notifiers behind
 * `:core:sync`'s interface (Fase 6). The widget repaint plugs into
 * [ServiceLocator.install]'s callback from Fase 8; until then nothing listens, which
 * is why the parameter has a default and this call still says everything it needs to.
 */
class ChiaroApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        ServiceLocator.install(
            userAgent = "chiaro/${BuildConfig.VERSION_NAME} " +
                "(+https://github.com/fiorenzobrioni/chiaro)"
        )
        SyncDependencies.install(ChiaroNotifiers(this))
        // Two safety nets on every process start, milliseconds of DataStore reads
        // off the main thread: the sky alarm (a force-stop clears it without a boot,
        // Fase 5) and the periodic job's desired state (Fase 6).
        CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
            runCatching { SkyAlarmScheduler.reschedule(this@ChiaroApplication) }
            runCatching { SyncScheduler.reconcile(this@ChiaroApplication) }
        }
    }
}
