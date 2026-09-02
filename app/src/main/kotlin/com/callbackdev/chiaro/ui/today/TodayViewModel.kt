package com.callbackdev.chiaro.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.callbackdev.chiaro.data.ActiveSource
import com.callbackdev.chiaro.data.CityStore
import com.callbackdev.chiaro.data.ServiceLocator
import com.callbackdev.chiaro.data.SettingsStore
import com.callbackdev.chiaro.domain.WeatherException
import com.callbackdev.chiaro.domain.model.City
import java.time.Clock
import java.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The Today screen's state machine, and the place where the product's opening rule is
 * enforced: **cache first, network after, and the screen never goes blank to wait.**
 * On every (place, interval) change the cached report is emitted before any fetch is
 * even attempted; a refresh only ever flips [TodayUiState.Content.refreshing] on the
 * content that is already up.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(
    private val repository: com.callbackdev.chiaro.data.WeatherRepository,
    cityStore: CityStore,
    settingsStore: SettingsStore,
    private val clock: Clock = Clock.systemUTC()
) : ViewModel() {

    private val refreshRequests = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)

    val state: StateFlow<TodayUiState> = combine(
        cityStore.activeSource,
        settingsStore.settings.map { it.updateFrequencyMin }.distinctUntilChanged()
    ) { source, freq -> source to freq }
        .flatMapLatest { (source, freq) ->
            when (source) {
                ActiveSource.None -> flowOf(TodayUiState.NoPlace)
                // A GPS source with no fix yet cannot name a place; the real GPS flow
                // (permission, fix, pseudo-city) is Fase 3's. Until then it reads as
                // "no place", which is what the screen can honestly say about it.
                is ActiveSource.Gps ->
                    source.lastFix?.let { cityStates(it, freq) } ?: flowOf(TodayUiState.NoPlace)
                is ActiveSource.Saved -> cityStates(source.city, freq)
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState.Starting)

    /** Pull-to-refresh and the freshness chip. Forced: the reader asked. */
    fun refresh() {
        refreshRequests.tryEmit(true)
    }

    private fun cityStates(city: City, updateFrequencyMin: Int): Flow<TodayUiState> =
        channelFlow {
            var refreshing = false
            var error: TodayError? = null
            var report = repository.cachedReport(city)

            suspend fun push() {
                val current = report
                send(
                    if (current == null) {
                        TodayUiState.Empty(city, refreshing, error)
                    } else {
                        TodayStateBuilder.build(
                            city, current, clock.instant(), updateFrequencyMin,
                            refreshing, error
                        )
                    }
                )
            }

            suspend fun fetch(force: Boolean) {
                if (refreshing) return
                refreshing = true
                push()
                try {
                    report = repository.getWeather(
                        city,
                        forceRefresh = force,
                        ttl = Duration.ofMinutes(updateFrequencyMin.toLong())
                    )
                    error = null
                } catch (e: WeatherException) {
                    error = when (e) {
                        is WeatherException.NoNetwork -> TodayError.OFFLINE
                        is WeatherException.ApiError -> TodayError.SERVICE
                        else -> TodayError.UNKNOWN
                    }
                } finally {
                    refreshing = false
                    push()
                }
            }

            push()
            launch { fetch(force = false) }
            launch { refreshRequests.collect { force -> fetch(force) } }
            // The minute tick: the stated age, the staleness verdict and the recency
            // trim all move with the clock even when no new data does.
            launch {
                while (true) {
                    delay(60_000)
                    push()
                }
            }
            awaitClose { }
        }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                TodayViewModel(
                    repository = ServiceLocator.weatherRepository(app),
                    cityStore = ServiceLocator.cityStore(app),
                    settingsStore = ServiceLocator.settingsStore(app)
                )
            }
        }
    }
}
