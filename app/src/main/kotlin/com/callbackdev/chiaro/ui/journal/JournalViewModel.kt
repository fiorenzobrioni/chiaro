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
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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
 * by the repository and read as prose by [JournalStateBuilder].
 *
 * It **follows** the table rather than polling it. The minute tick this used to run on
 * existed to catch a fetch landing while the screen was open, but nothing here ages
 * with the clock (every time on screen is absolute), so it was rebuilding forty rows
 * of JSON and a diff engine once a minute to produce the same state — and it did it
 * under battery saver too, which Today does not. Room re-emits on write and only on
 * write, so an open screen sees the fetch land and an idle one costs nothing.
 *
 * The one thing on this screen that DOES turn with the clock is the day itself, and it
 * turns once: [dayBoundaries] wakes at the place's midnight and nowhere else.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class JournalViewModel(
    private val repository: WeatherRepository,
    private val cityStore: CityStore,
    private val fetchLogStore: FetchLogStore,
    settingsStore: SettingsStore
) : ViewModel() {

    val units: StateFlow<UnitSettings> = settingsStore.settings
        .map { it.units }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UnitSettings())

    val state: StateFlow<JournalUiState> = cityStore.activeSource
        .map { it.cityOrNull() }
        .distinctUntilChanged()
        .flatMapLatest { city ->
            if (city == null) {
                flowOf(JournalUiState.NoPlace)
            } else {
                combine(
                    repository.historyFlowFor(city, limit = HISTORY_SCAN),
                    fetchLogStore.failures,
                    dayBoundaries(JournalStateBuilder.zoneOf(city))
                ) { entries, failures, _ ->
                    val rows = entries.map { entry ->
                        JournalRow(
                            at = Instant.ofEpochSecond(entry.timestampEpochSeconds),
                            forecast = repository.forecast(entry),
                            firedRules = repository.firedRules(entry),
                            skyRuns = repository.skyRuns(entry),
                            snapshot = repository.snapshot(entry)
                        )
                    }
                    JournalUiState.Ready(
                        JournalStateBuilder.build(
                            city = city,
                            rows = rows,
                            failures = failures.filter { it.cityKey == city.cacheKey },
                            now = Instant.now()
                        )
                    )
                }
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JournalUiState.Starting)

    /**
     * One emission now, then one at every local midnight. It is the only clock event
     * this screen has: the day headings turn there ("Today" becomes "Yesterday"), and
     * so does the day a finished outcome may be judged on. A timer that fires once a
     * day is not the minute tick that used to run here — it is the one moment the
     * minute tick existed to catch and never named.
     */
    private fun dayBoundaries(zone: ZoneId): Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            val now = ZonedDateTime.now(zone)
            val midnight = now.toLocalDate().plusDays(1).atStartOfDay(zone)
            delay(Duration.between(now, midnight).toMillis().coerceAtLeast(1_000L))
        }
    }

    private fun ActiveSource.cityOrNull(): City? = when (this) {
        is ActiveSource.Saved -> city
        is ActiveSource.Gps -> lastFix
        ActiveSource.None -> null
    }

    companion object {
        /**
         * The whole per-city history. It used to be forty commits, which at the default
         * hour of cadence is under two days of diary — less than the drift strip's own
         * window. Retention is per city now (`WeatherRepository.HISTORY_RETENTION`), so
         * reading all of it is reading exactly what this place recorded.
         */
        private const val HISTORY_SCAN = WeatherRepository.HISTORY_RETENTION

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
