package com.callbackdev.chiaro.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.callbackdev.chiaro.data.ActiveSource
import com.callbackdev.chiaro.data.CityStore
import com.callbackdev.chiaro.data.FetchFailureReason
import com.callbackdev.chiaro.data.FetchLogStore
import com.callbackdev.chiaro.data.LocationProvider
import com.callbackdev.chiaro.data.ServiceLocator
import com.callbackdev.chiaro.data.SettingsStore
import com.callbackdev.chiaro.data.WorkspaceStore
import com.callbackdev.chiaro.domain.WeatherException
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.settings.UnitSettings
import com.callbackdev.chiaro.ui.journal.JournalEntry
import com.callbackdev.chiaro.ui.journal.JournalRow
import com.callbackdev.chiaro.ui.journal.JournalStateBuilder
import com.callbackdev.chiaro.ui.places.GpsError
import com.callbackdev.chiaro.ui.places.asGpsError
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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
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
    private val fetchLogStore: FetchLogStore,
    private val locationProvider: LocationProvider,
    private val clock: Clock = Clock.systemUTC()
) : ViewModel() {

    /** Carries the cacheKey of the page whose reader pulled. */
    private val refreshRequests = MutableSharedFlow<String>(extraBufferCapacity = 4)

    private val pageStates = mutableMapOf<String, StateFlow<TodayUiState>>()

    /**
     * True while a position acquisition **the reader asked for** is in flight, so the
     * GPS page's pull keeps spinning: a fix can take up to fifteen seconds, and a
     * gesture that answers with nothing reads as a broken one.
     *
     * Deliberately not set by the automatic paths (Fase 3b). It used to be, and since
     * landing on the page re-fixes and landing happens at every launch, the reader met
     * a spinning pull indicator on every single app start — for work nobody had asked
     * for, over content that was already on screen and already right.
     */
    private val locatingFlow = MutableStateFlow(false)
    val locating: StateFlow<Boolean> = locatingFlow.asStateFlow()

    /**
     * Why an explicitly requested fix failed, said in words once (Fase 3b). An
     * automatic fix that fails stays silent — the last position with real weather
     * beats an error over numbers that are still true — but a pull that silently
     * changes nothing is how a revoked permission stayed invisible forever.
     */
    private val locationErrorFlow = MutableStateFlow<GpsError?>(null)
    val locationError: StateFlow<GpsError?> = locationErrorFlow.asStateFlow()

    /** One acquisition at a time in this ViewModel; the provider coalesces across
     * the other one. */
    private var acquiring = false

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

    /**
     * Pull-to-refresh and the freshness chip, for the page the reader is on.
     *
     * On the device-position page the refresh takes the POSITION again first (device
     * report, 4 set): the fix was only ever re-taken when the source was enabled or
     * its row tapped in the sheet, so someone who drove to the next town and pulled
     * got fresh numbers for the town they had left. "My position" means where the
     * reader is now — refreshing it is refreshing the place, not only its weather.
     */
    fun refresh(page: PlacePage) {
        when (page) {
            is PlacePage.Saved -> refreshRequests.tryEmit(page.city.cacheKey)
            is PlacePage.Gps -> viewModelScope.launch { refix(page, explicit = true) }
        }
    }

    /** The error banner's own dismissal, and what the snackbar calls once it has been
     * read: an error is worth saying once, not until the reader taps it away. */
    fun dismissLocationError() {
        locationErrorFlow.value = null
    }

    /**
     * The app came back to the front (Fase 3b). The pager settles once and does not
     * settle again on a warm resume, so before this the position page could sit on a
     * fix taken before a three-hour drive while its weather refreshed happily beside
     * it — the position was re-taken at every cold start, which is when it was least
     * likely to be wrong, and never when it was most likely to be. Silent, and gated
     * by the same interval as everything else, so on the ordinary resume it does
     * nothing at all.
     */
    fun resumed(page: PlacePage) {
        if (page !is PlacePage.Gps) return
        viewModelScope.launch { refix(page, explicit = false) }
    }

    /** The pager settled on [page]: that IS the selection (VISION §5.6 — the active
     * place is a tap, the pager is a swipe; both end in the same store). Swiping onto
     * the position page also re-takes the fix, exactly as tapping its row in the sheet
     * does — the two roads to the same page cannot answer with two different towns. */
    fun setActive(page: PlacePage) {
        viewModelScope.launch {
            when (page) {
                is PlacePage.Gps -> {
                    cityStore.setActiveGps()
                    refix(page, explicit = false)
                }
                is PlacePage.Saved -> cityStore.setActive(page.city)
            }
        }
    }

    /**
     * Re-takes the device position. [explicit] is the reader asking out loud — the
     * pull — which is the only thing that shows a spinner, bypasses every interval and
     * reports a failure.
     *
     * Nothing is cancelled: the page keeps whatever it was showing until the new fix
     * lands, and an automatic fix that fails leaves no trace at all.
     */
    private suspend fun refix(page: PlacePage.Gps, explicit: Boolean) {
        val previous = page.lastFix
        // A moved fix rebuilds the page under a new cacheKey and the state for that
        // key fetches on its own: asking for a refresh here as well would spend two
        // GETs on the same arrival.
        if (acquire(previous, explicit)) return
        // Same place (or no fix at all): the reader still asked for new numbers.
        if (explicit) previous?.let { refreshRequests.tryEmit(it.cacheKey) }
    }

    /**
     * Takes the position when one is due, hands it to the store to adopt, and answers
     * whether the PLACE changed.
     *
     * Two gates, and they guard different things. The persisted instant is what makes
     * a launch free: it survives the process, which the old in-memory counter did not,
     * so a cold start behind a fix taken minutes ago now asks for nothing — while
     * before it counted every launch as due and acquired a position every time. The
     * [acquiring] flag is only about this object doing one thing at a time; the
     * provider's own throttle is what keeps the Places sheet and this ViewModel from
     * paying twice for one gesture.
     *
     * The maxAge handed down is the same interval, so the layer that knows how to be
     * cheap gets to be cheap: below it the platform answers from a position it already
     * holds and powers nothing up at all.
     */
    private suspend fun acquire(previous: City?, explicit: Boolean): Boolean {
        if (acquiring) return false
        if (!explicit && previous != null && !fixIsDue()) return false

        acquiring = true
        if (explicit) locatingFlow.value = true
        val fix = try {
            locationProvider.currentFix(
                maxAge = if (explicit) LocationProvider.Now else GpsRefixInterval,
                timeout = if (explicit) {
                    LocationProvider.DefaultTimeout
                } else {
                    LocationProvider.SilentTimeout
                }
            )
        } catch (e: WeatherException) {
            if (explicit) locationErrorFlow.value = e.asGpsError()
            null
        } finally {
            acquiring = false
            locatingFlow.value = false
        }
        if (fix == null) return false

        // The adoption rule lives in the store, so every road to a fix obeys it: under
        // two kilometres this keeps the place and takes only its better name.
        val adopted = cityStore.adoptGpsFix(fix, clock.instant())
        return adopted.cacheKey != previous?.cacheKey
    }

    /** Whether the persisted fix has aged past the interval. No fix ever taken (or a
     * clock that moved backwards under us) counts as due. */
    private suspend fun fixIsDue(): Boolean {
        val fixedAt = cityStore.locationSettings.first().fixedAt ?: return true
        val age = Duration.between(fixedAt, clock.instant())
        return age.isNegative || age >= GpsRefixInterval
    }

    private fun cityStates(city: City, updateFrequencyMin: Int): Flow<TodayUiState> =
        channelFlow {
            // Two flags, not one (Fase 3b): [inFlight] is the guard against two
            // fetches racing, [userRefreshing] is the only thing the pull indicator
            // is allowed to believe. They used to be the same variable, which is how
            // an automatic fetch came to spin a gesture's spinner.
            var inFlight = false
            var userRefreshing = false
            var error: TodayError? = null
            var report = repository.cachedReport(city)
            var whatChanged: List<JournalEntry.ForecastShift> = emptyList()

            suspend fun push() {
                val current = report
                send(
                    if (current == null) {
                        TodayUiState.Empty(city, userRefreshing, error)
                    } else {
                        val built = TodayStateBuilder.build(
                            city, current, clock.instant(), updateFrequencyMin,
                            userRefreshing, error
                        )
                        if (built is TodayUiState.Content) {
                            built.copy(whatChanged = whatChanged)
                        } else built
                    }
                )
            }

            // VISION §5.2.5: the latest revisions, read off the history the fetches
            // write. Recomputed when the data moves, never on the minute tick.
            suspend fun refreshChanged() {
                whatChanged = runCatching {
                    JournalStateBuilder.latestShifts(
                        repository.historyFor(city, limit = 12).map { entry ->
                            JournalRow(
                                at = java.time.Instant.ofEpochSecond(entry.timestampEpochSeconds),
                                forecast = repository.forecast(entry),
                                firedRules = emptyList(),
                                skyRuns = emptyList()
                            )
                        }
                    )
                }.getOrDefault(emptyList())
            }

            suspend fun fetch(userAsked: Boolean) {
                if (inFlight) return
                inFlight = true
                userRefreshing = userAsked
                // An automatic fetch has nothing to announce, so it does not even
                // spend a frame saying it started.
                if (userAsked) push()
                try {
                    report = repository.getWeather(
                        city,
                        forceRefresh = userAsked,
                        ttl = Duration.ofMinutes(updateFrequencyMin.toLong())
                    )
                    error = null
                    refreshChanged()
                } catch (e: WeatherException) {
                    error = when (e) {
                        is WeatherException.NoNetwork -> TodayError.OFFLINE
                        is WeatherException.ApiError -> TodayError.SERVICE
                        else -> TodayError.UNKNOWN
                    }
                    // The Journal is where offline honesty lives (Fase 7): the
                    // failure becomes an entry, not a silent gap between commits.
                    runCatching {
                        fetchLogStore.record(
                            city.cacheKey, clock.instant().epochSecond,
                            when (error) {
                                TodayError.OFFLINE -> FetchFailureReason.OFFLINE
                                TodayError.SERVICE -> FetchFailureReason.SERVICE
                                else -> FetchFailureReason.UNKNOWN
                            }
                        )
                    }
                } finally {
                    inFlight = false
                    userRefreshing = false
                    push()
                }
            }

            // The cached report goes out FIRST (Fase 3b). The revisions need a Room
            // query and a dozen JSON decodes, and they feed a section far down the
            // page: waiting for them held the whole screen on its skeleton for work
            // nothing visible was waiting on.
            push()
            refreshChanged()
            push()
            launch { fetch(userAsked = false) }
            launch {
                refreshRequests.filter { it == city.cacheKey }.collect { fetch(userAsked = true) }
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
        /**
         * How stale a fix has to be before an automatic path takes another one, and
         * also the maxAge handed to the provider. Battery is a feature (CLAUDE.md):
         * a one-shot coarse fix is cheap, a fix per swipe is not, and a fix per
         * launch is the one that adds up.
         */
        private val GpsRefixInterval: Duration = LocationProvider.SilentMaxAge

        val Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                TodayViewModel(
                    repository = ServiceLocator.weatherRepository(app),
                    cityStore = ServiceLocator.cityStore(app),
                    settingsStore = ServiceLocator.settingsStore(app),
                    workspaceStore = ServiceLocator.workspaceStore(app),
                    fetchLogStore = ServiceLocator.fetchLogStore(app),
                    locationProvider = ServiceLocator.locationProvider(app)
                )
            }
        }
    }
}
