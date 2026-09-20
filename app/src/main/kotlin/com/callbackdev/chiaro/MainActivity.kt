package com.callbackdev.chiaro

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.CompositionLocalProvider
import com.callbackdev.chiaro.data.AppFont
import com.callbackdev.chiaro.data.AppPalette
import com.callbackdev.chiaro.data.ServiceLocator
import com.callbackdev.chiaro.data.ThemeMode
import com.callbackdev.chiaro.data.WeatherIcons
import com.callbackdev.chiaro.ui.icons.LocalAnimatedIcons
import com.callbackdev.chiaro.ui.icons.LocalWeatherIcons
import com.callbackdev.chiaro.ui.shell.ChiaroRoot
import com.callbackdev.chiaro.ui.shell.ShellDestination
import com.callbackdev.chiaro.ui.shell.ShellTab
import com.callbackdev.chiaro.ui.theme.ChiaroTheme

/**
 * One destination so far: Today, behind the shell's first-run gate (Fase 3). The
 * bottom navigation of VISION §5.1 arrives with the second destination (Sky, Fase 5)
 * — a bar with three dead tabs would be the screen lying about what the app can do.
 *
 * The theme reads the reader's choices (Fase 4). Until the store's first emission the
 * defaults hold — the phone's own light or dark, the vivid dress — which are also what
 * a fresh install chose; only a reader who forced the theme against the system can see
 * one frame of the other scheme, and one frame is cheaper than holding the whole app
 * blank.
 *
 * It is also the one door the home widgets open (21 set 2026): a widget that shows the
 * Sky screen's material asks for the Sky tab by name ([ShellDestination]), and the tab
 * is state rather than a route, so the request is held here and handed to the shell.
 * [onNewIntent] carries the second and every later tap, which is the case that matters
 * most — the first one lands in [onCreate], the app then stays alive, and without this
 * every tap after it would bring back whatever tab was last on screen.
 */
class MainActivity : ComponentActivity() {

    /** The tab the intent that brought us here asked for; null once the shell has it. */
    private var requestedTab by mutableStateOf<ShellTab?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestedTab = ShellDestination.of(intent)
        val settingsStore = ServiceLocator.settingsStore(applicationContext)
        setContent {
            val settings by settingsStore.settings.collectAsStateWithLifecycle(initialValue = null)
            val darkTheme = when (settings?.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                // `null` is the store's first frame, and SYSTEM is what it will say.
                ThemeMode.SYSTEM, null -> isSystemInDarkTheme()
            }
            // The bars' ink follows the APPLIED theme, not the system's: a reader
            // who forces light against a dark phone was getting white icons over a
            // white surface (device report, 3 set). Today's canvas takes the status
            // bar over while it is behind it (TodayScreen.StatusBarIcons) and hands
            // it back when it goes.
            val view = LocalView.current
            DisposableEffect(darkTheme) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
                onDispose { }
            }
            ChiaroTheme(
                darkTheme = darkTheme,
                dynamicColor = settings?.dynamicColor ?: false,
                palette = settings?.palette ?: AppPalette.VIVID,
                font = settings?.font ?: AppFont.GOOGLE_SANS
            ) {
                CompositionLocalProvider(
                    LocalWeatherIcons provides (settings?.weatherIcons ?: WeatherIcons.LINE),
                    LocalAnimatedIcons provides (settings?.animatedIcons ?: true)
                ) {
                    ChiaroRoot(
                        requestedTab = requestedTab,
                        onRequestedTabHandled = { requestedTab = null }
                    )
                }
            }
        }
    }

    /**
     * A widget tapped while the app is already running. `CLEAR_TOP or SINGLE_TOP` on
     * the widget's intent is what routes it here instead of rebuilding the activity,
     * and `setIntent` keeps [getIntent] honest for anything that reads it later.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        ShellDestination.of(intent)?.let { requestedTab = it }
    }
}
