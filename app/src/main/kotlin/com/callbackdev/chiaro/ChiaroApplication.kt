package com.callbackdev.chiaro

import android.app.Application
import com.callbackdev.chiaro.data.ServiceLocator
import com.callbackdev.chiaro.notifications.SkyAlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Installs the data layer's two pieces of app knowledge (Fase 0): who the app says
 * it is to Open-Meteo, and who wants to hear that a fetch committed new data.
 *
 * `:core:data` is a library and knows neither. The widget repaint plugs into
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
        // Re-arm the sky reminder on every process start (Fase 5): a force-stop or a
        // crash clears the alarm without a boot, and this is the path that notices.
        // Milliseconds of DataStore reads, off the main thread.
        CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
            runCatching { SkyAlarmScheduler.reschedule(this@ChiaroApplication) }
        }
    }
}
