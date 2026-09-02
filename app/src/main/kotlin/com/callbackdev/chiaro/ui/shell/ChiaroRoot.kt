package com.callbackdev.chiaro.ui.shell

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

@Composable
fun ChiaroRoot(shellViewModel: ShellViewModel = viewModel(factory = ShellViewModel.Factory)) {
    val firstRun by shellViewModel.firstRun.collectAsStateWithLifecycle()
    when (firstRun) {
        FirstRun.Unknown -> Surface(modifier = Modifier.fillMaxSize()) { }
        FirstRun.Pending -> FirstRunRoute(onSkip = shellViewModel::skip)
        FirstRun.Done -> TodayRoute()
    }
}
