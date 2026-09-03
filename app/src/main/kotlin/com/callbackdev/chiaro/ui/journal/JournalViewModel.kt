package com.callbackdev.chiaro.ui.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.callbackdev.chiaro.data.ActiveSource
import com.callbackdev.chiaro.data.CityStore
import com.callbackdev.chiaro.data.FetchLogStore
import com.callbackdev.chiaro.data.ServiceLocator
import com.callbackdev.chiaro.data.SettingsStore
import com.callbackdev.chiaro.data.WeatherRepository
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.settings.UnitSettings
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface JournalUiState {
    data object Starting : JournalUiState
    data object NoPlace : JournalUiState
    data class Ready(val content: JournalContent) : JournalUiState
}

/**
 * The Journal's state machine (Fase 7): the active place's history commits, decoded
 * by the repository and read as prose by [JournalStateBuilder]. A minute tick keeps
 * the screen following fetches that land while it is open — the history has no flow
 * of its own per city, and polling a local table once a minute costs nothing.
 */
class JournalViewModel(
    private val repository: WeatherRepository,
    private val cityStore: CityStore,
    private val fetchLogStore: FetchLogStore,
    settingsStore: SettingsStore
) : ViewModel() {

    private val minuteTick = flow {
        while (true) {
            emit(Unit)
            kotlinx.coroutines.delay(60_000)
        }
    }

    val units: StateFlow<UnitSettings> = settingsStore.settings
        .map { it.units }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UnitSettings())

    val state: StateFlow<JournalUiState> = combine(
        cityStore.activeSource,
        fetchLogStore.failures,
        minuteTick
    ) { active, failures, _ ->
        val city = active.cityOrNull() ?: return@combine JournalUiState.NoPlace
        val rows = repository.historyFor(city, limit = HISTORY_SCAN).map { entry ->
            JournalRow(
                at = Instant.ofEpochSecond(entry.timestampEpochSeconds),
                forecast = repository.forecast(entry),
                firedRules = repository.firedRules(entry),
                skyRuns = repository.skyRuns(entry)
            )
        }
        JournalUiState.Ready(
            JournalStateBuilder.build(
                city = city,
                rows = rows,
                failures = failures.filter { it.cityKey == city.cacheKey }
            )
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JournalUiState.Starting)

    private fun ActiveSource.cityOrNull(): City? = when (this) {
        is ActiveSource.Saved -> city
        is ActiveSource.Gps -> lastFix
        ActiveSource.None -> null
    }

    companion object {
        private const val HISTORY_SCAN = 40

        val Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                JournalViewModel(
                    repository = ServiceLocator.weatherRepository(app),
                    cityStore = ServiceLocator.cityStore(app),
                    fetchLogStore = ServiceLocator.fetchLogStore(app),
                    settingsStore = ServiceLocator.settingsStore(app)
                )
            }
        }
    }
}
