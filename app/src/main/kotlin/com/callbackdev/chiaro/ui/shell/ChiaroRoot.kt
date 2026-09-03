package com.callbackdev.chiaro.ui.shell

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.callbackdev.chiaro.data.CityStore
import com.callbackdev.chiaro.data.FirstRun
import com.callbackdev.chiaro.data.ServiceLocator
import com.callbackdev.chiaro.data.WeatherRepository
import com.callbackdev.chiaro.ui.firstrun.FirstRunRoute
import com.callbackdev.chiaro.ui.guide.GuideRoute
import com.callbackdev.chiaro.ui.settings.SettingsRoute
import com.callbackdev.chiaro.ui.today.TodayRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The one question the shell answers before drawing anything: does first-run still
 * owe an answer? [FirstRun.Unknown] draws a bare surface — flashing either screen at
 * the wrong person is worse than one blank frame — and the migration check that
 * resolves it runs exactly once per install (VISION §5.8).
 */
class ShellViewModel(
    repository: WeatherRepository,
    private val cityStore: CityStore
) : ViewModel() {

    init {
        viewModelScope.launch {
            cityStore.migrateFirstRun(hasHistory = repository.hasAnyHistory())
        }
    }

    val firstRun: StateFlow<FirstRun> = cityStore.firstRun
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FirstRun.Unknown)

    /** Skipping is an answer (VISION §5.8): it lands on the real "no place" state. */
    fun skip() {
        viewModelScope.launch { cityStore.markInitDone() }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                ShellViewModel(
                    repository = ServiceLocator.weatherRepository(app),
                    cityStore = ServiceLocator.cityStore(app)
                )
            }
        }
    }
}

/**
 * The screens the app bar reaches (Fase 4): Settings from the gear, the guide from
 * Settings or from the one-time card on Today. Held as plain state rather than a nav
 * graph: three destinations and two edges do not earn one, and the bottom navigation
 * of VISION §5.1 will re-pose the question when Sky arrives (Fase 5).
 */
private enum class ShellScreen { TODAY, SETTINGS, GUIDE }

@Composable
fun ChiaroRoot(shellViewModel: ShellViewModel = viewModel(factory = ShellViewModel.Factory)) {
    val firstRun by shellViewModel.firstRun.collectAsStateWithLifecycle()
    when (firstRun) {
        FirstRun.Unknown -> Surface(modifier = Modifier.fillMaxSize()) { }
        FirstRun.Pending -> FirstRunRoute(onSkip = shellViewModel::skip)
        FirstRun.Done -> MainScreens()
    }
}

@Composable
private fun MainScreens() {
    var screen by rememberSaveable { mutableStateOf(ShellScreen.TODAY) }
    // The guide has two doors (VISION §5.7); back returns through the one it entered.
    var guideOrigin by rememberSaveable { mutableStateOf(ShellScreen.TODAY) }

    BackHandler(enabled = screen != ShellScreen.TODAY) {
        screen = if (screen == ShellScreen.GUIDE) guideOrigin else ShellScreen.TODAY
    }

    when (screen) {
        ShellScreen.TODAY -> TodayRoute(
            onOpenSettings = { screen = ShellScreen.SETTINGS },
            onOpenGuide = {
                guideOrigin = ShellScreen.TODAY
                screen = ShellScreen.GUIDE
            }
        )
        ShellScreen.SETTINGS -> SettingsRoute(
            onBack = { screen = ShellScreen.TODAY },
            onOpenGuide = {
                guideOrigin = ShellScreen.SETTINGS
                screen = ShellScreen.GUIDE
            }
        )
        ShellScreen.GUIDE -> GuideRoute(onBack = { screen = guideOrigin })
    }
}
