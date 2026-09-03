package com.callbackdev.chiaro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.CompositionLocalProvider
import com.callbackdev.chiaro.data.ServiceLocator
import com.callbackdev.chiaro.data.ThemeMode
import com.callbackdev.chiaro.data.WeatherIcons
import com.callbackdev.chiaro.ui.icons.LocalWeatherIcons
import com.callbackdev.chiaro.ui.shell.ChiaroRoot
import com.callbackdev.chiaro.ui.theme.ChiaroTheme

/**
 * One destination so far: Today, behind the shell's first-run gate (Fase 3). The
 * bottom navigation of VISION §5.1 arrives with the second destination (Sky, Fase 5)
 * — a bar with three dead tabs would be the screen lying about what the app can do.
 *
 * The theme reads the reader's choices (Fase 4). Until the store's first emission the
 * defaults hold — system dark, dynamic color — which are also what a fresh install
 * chose; only a reader who forced the theme against the system can see one frame of
 * the other scheme, and one frame is cheaper than holding the whole app blank.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settingsStore = ServiceLocator.settingsStore(applicationContext)
        setContent {
            val settings by settingsStore.settings.collectAsStateWithLifecycle(initialValue = null)
            val darkTheme = when (settings?.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM, null -> isSystemInDarkTheme()
            }
            ChiaroTheme(
                darkTheme = darkTheme,
                dynamicColor = settings?.dynamicColor ?: true
            ) {
                CompositionLocalProvider(
                    LocalWeatherIcons provides (settings?.weatherIcons ?: WeatherIcons.FILL)
                ) {
                    ChiaroRoot()
                }
            }
        }
    }
}
