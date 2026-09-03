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
import com.callbackdev.chiaro.data.WorkspaceStore
import com.callbackdev.chiaro.domain.WeatherException
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.settings.UnitSettings
import java.time.Clock
import java.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * One page of the Today pager (VISION §5.1: swiping left/right moves between saved
 * places, with the device position as its own page while GPS is on). [key] is what
 * the pager and the per-page state cache identify a page by.
 */
sealed interface PlacePage {
    val key: String
    val city: City?

    data class Gps(val lastFix: City?) : PlacePage {
        override val key: String get() = "gps"
        override val city: City? get() = lastFix
    }

    data class Saved(override val city: City) : PlacePage {
        override val key: String get() = "city:${city.id}"
    }
}

/** The pager's whole input in one value, so pages and selection can never disagree
 * about which index is active. Null while the stores have not answered yet. */
data class PagerModel(val pages: List<PlacePage>, val activeIndex: Int)

/**
 * The Today screens' state machine — plural since Fase 3: one flow of pages, one
 * state per page, built on the same rule as ever: **cache first, network after, and
 * no page ever goes blank to wait.** A refresh is addressed to one page, because
 * pulling on Milano is not a request to spend two GETs on every neighbour the pager
 * keeps warm.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(
    private val repository: com.callbackdev.chiaro.data.WeatherRepository,
    private val cityStore: CityStore,
    private val settingsStore: SettingsStore,
    private val workspaceStore: WorkspaceStore,
    private val clock: Clock = Clock.systemUTC()
) : ViewModel() {

    /** Carries the cacheKey of the page whose reader pulled. */
    private val refreshRequests = MutableSharedFlow<String>(extraBufferCapacity = 4)

    private val pageStates = mutableMapOf<String, StateFlow<TodayUiState>>()

    val pager: StateFlow<PagerModel?> = combine(
        cityStore.locationSettings,
        cityStore.cities,
        cityStore.activeSource
    ) { location, cities, active ->
        val pages = buildList {
            if (location.useGps) add(PlacePage.Gps(location.gpsCity))
            cities.forEach { add(PlacePage.Saved(it)) }
        }
        val activeIndex = when (active) {
            is ActiveSource.Gps -> pages.indexOfFirst { it is PlacePage.Gps }
            is ActiveSource.Saved -> pages.indexOfFirst {
                it is PlacePage.Saved && it.city.id == active.city.id
            }
            ActiveSource.None -> -1
        }
        PagerModel(pages, activeIndex)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The reader's units (Fase 4). Starts on the defaults — the same values a fresh
     * install chose — and follows the store from its first answer on. */
    val units: StateFlow<UnitSettings> = settingsStore.settings
        .map { it.units }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UnitSettings())

    /** The one-time guide card (VISION §5.7). Null until the store answers: a card
     * that flashes and leaves would be shown to everyone exactly once, dismissed by
     * nobody. */
    val guideCardVisible: StateFlow<Boolean?> = workspaceStore.guideCardDismissed
        .map { dismissed -> !dismissed }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Used or waved away, the card is done: both roads end here. */
    fun dismissGuideCard() {
        viewModelScope.launch { workspaceStore.dismissGuideCard() }
    }

    /** The state of one page, created on first request and shared from then on. */
    fun stateFor(page: PlacePage): StateFlow<TodayUiState> {
        // A GPS page with no fix can only mean the enable flow was cut short: the
        // sheet's row is the way back, and "no place" is the only honest word for it.
        val city = page.city ?: return MutableStateFlow(TodayUiState.NoPlace)
        val key = if (page is PlacePage.Gps) "gps:${city.cacheKey}" else page.key
        return pageStates.getOrPut(key) {
            settingsStore.settings
                .map { it.updateFrequencyMin }
                .distinctUntilChanged()
                .flatMapLatest { cityStates(city, it) }
                .flowOn(Dispatchers.Default)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState.Starting)
        }
    }

    /** Pull-to-refresh and the freshness chip, for the page the reader is on. */
    fun refresh(page: PlacePage) {
        page.city?.let { refreshRequests.tryEmit(it.cacheKey) }
    }

    /** The pager settled on [page]: that IS the selection (VISION §5.6 — the active
     * place is a tap, the pager is a swipe; both end in the same store). */
    fun setActive(page: PlacePage) {
        viewModelScope.launch {
            when (page) {
                is PlacePage.Gps -> cityStore.setActiveGps()
                is PlacePage.Saved -> cityStore.setActive(page.city)
            }
        }
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
            launch {
                refreshRequests.filter { it == city.cacheKey }.collect { fetch(force = true) }
            }
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
                    settingsStore = ServiceLocator.settingsStore(app),
                    workspaceStore = ServiceLocator.workspaceStore(app)
                )
            }
        }
    }
}
