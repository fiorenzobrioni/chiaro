package com.callbackdev.chiaro.ui.places

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.callbackdev.chiaro.data.ActiveSource
import com.callbackdev.chiaro.data.CityStore
import com.callbackdev.chiaro.data.GpsArrival
import com.callbackdev.chiaro.data.GpsRefix
import com.callbackdev.chiaro.data.LocationProvider
import com.callbackdev.chiaro.data.LocationSettings
import com.callbackdev.chiaro.data.SearchHistoryStore
import com.callbackdev.chiaro.data.ServiceLocator
import com.callbackdev.chiaro.data.WeatherRepository
import com.callbackdev.chiaro.domain.WeatherException
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.sync.SyncScheduler
import java.time.Clock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the search field is currently answering. Idle draws nothing — an empty result
 * list before anybody typed is not a result. */
sealed interface SearchState {
    data object Idle : SearchState
    data object Searching : SearchState
    data class Results(val cities: List<City>) : SearchState
    data class NoResults(val query: String) : SearchState
    data object Offline : SearchState
    data object Failed : SearchState
}

/** Where the device-position row is in its life. [Error] words arrive at render. */
sealed interface GpsState {
    data object Idle : GpsState
    data object Acquiring : GpsState
    data class Error(val kind: GpsError) : GpsState
}

enum class GpsError { PERMISSION, DISABLED, TIMEOUT, UNAVAILABLE }

/** The honest word for a failed fix. Shared since Fase 3b: Today's pull has to say
 * exactly what the sheet has always said, and one mapping is how it stays that way. */
fun WeatherException.asGpsError(): GpsError = when (this) {
    is WeatherException.LocationPermissionDenied -> GpsError.PERMISSION
    is WeatherException.LocationDisabled -> GpsError.DISABLED
    is WeatherException.LocationTimeout -> GpsError.TIMEOUT
    else -> GpsError.UNAVAILABLE
}

/** One saved row: the city plus the cached temperature beside it (VISION §5.6) —
 * cached only, already formatted°-less; a place list must never spend network. */
data class SavedPlace(val city: City, val temperatureC: Double?)

/** What swipe-to-remove needs to offer its undo: the row and where it was. */
data class RemovedPlace(val city: City, val index: Int, val wasActive: Boolean)

/**
 * The Places surface (VISION §5.6): the device position pinned on top, the saved
 * list with a cached temperature beside each, search-as-you-type with the recent
 * searches, reorder, and remove-with-undo. Fase 3 makes this the real thing the
 * Fase 2 sheet was a down payment on.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class PlacesViewModel(
    private val repository: WeatherRepository,
    private val cityStore: CityStore,
    private val searchHistory: SearchHistoryStore,
    private val locationProvider: LocationProvider,
    private val clock: Clock = Clock.systemUTC(),
    /** The background job's desired state, read again after a place is gone for good
     * ([settleRemoval]): [com.callbackdev.chiaro.sync.SyncScheduler.reconcile] in the app. */
    private val reconcileSync: suspend () -> Unit = {},
    /** The silent road to the position, the one Today's pager takes when it settles on
     * that page: the app's single instance in the factory (30 ott 2026). */
    private val gpsRefix: GpsRefix = GpsRefix(
        cityStore, locationProvider, GpsArrival(cityStore, repository, locationProvider), clock
    )
) : ViewModel() {

    /** Whether a fix moves the position page, decided with its weather (20 ott 2026). */
    private val arrival = GpsArrival(cityStore, repository, locationProvider)

    val places: StateFlow<List<SavedPlace>> = cityStore.cities
        .mapLatest { cities ->
            cities.map { SavedPlace(it, repository.cachedReport(it)?.current?.tempC) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val active: StateFlow<ActiveSource?> = cityStore.activeSource
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val location: StateFlow<LocationSettings?> = cityStore.locationSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val recentSearches: StateFlow<List<String>> = searchHistory.recentSearches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val gpsStateFlow = MutableStateFlow<GpsState>(GpsState.Idle)
    val gpsState: StateFlow<GpsState> = gpsStateFlow.asStateFlow()

    /**
     * A tap on the position row has selected the page — the sheet's cue to close, which
     * it deliberately does not do on the tap itself (Fase 3b). The row is the only
     * surface that can say why the position cannot be had, and "I am looking" when there
     * is no place to show yet ([selectGps]), and a sheet dismissed a frame after the tap
     * said neither.
     */
    private val gpsSelectedFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val gpsSelected: SharedFlow<Unit> = gpsSelectedFlow.asSharedFlow()

    private val queryFlow = MutableStateFlow("")
    val query: StateFlow<String> = queryFlow.asStateFlow()

    val search: StateFlow<SearchState> = queryFlow
        .debounce(350)
        .mapLatest { q ->
            val trimmed = q.trim()
            if (trimmed.length < 2) return@mapLatest SearchState.Idle
            try {
                SearchState.Results(repository.searchCities(trimmed))
            } catch (e: WeatherException.CityNotFound) {
                SearchState.NoResults(trimmed)
            } catch (e: WeatherException.NoNetwork) {
                SearchState.Offline
            } catch (e: WeatherException) {
                SearchState.Failed
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchState.Idle)

    fun setQuery(value: String) {
        queryFlow.value = value
    }

    /**
     * Forgets one remembered search. The sheet passes the list it was showing to
     * [restoreRecents] if the reader undoes it: searching for the term again would
     * file it as the newest one, and the order of that list means something.
     */
    fun removeRecent(term: String) {
        viewModelScope.launch { searchHistory.remove(term) }
    }

    /** Forgets every remembered search, with the same undo as [removeRecent]. */
    fun clearRecents() {
        viewModelScope.launch { searchHistory.clear() }
    }

    fun restoreRecents(terms: List<String>) {
        viewModelScope.launch { searchHistory.restore(terms) }
    }

    /** A tapped search result: saved, made active, remembered as a search, and the
     * first-run debt settled — choosing a place IS the first-run answer, wherever it
     * is given from. */
    fun choose(city: City) {
        val term = queryFlow.value.trim()
        viewModelScope.launch {
            cityStore.add(city)
            cityStore.markInitDone()
            if (term.isNotEmpty()) searchHistory.add(term)
        }
        queryFlow.value = ""
    }

    fun select(city: City) {
        viewModelScope.launch { cityStore.setActive(city) }
    }

    /** Reorder, from the sheet's drag: [city] lands at [toIndex] among the saved. */
    fun move(city: City, toIndex: Int) {
        viewModelScope.launch { cityStore.move(city, toIndex) }
    }

    /** Swipe-to-remove. The returned memo is what [undoRemove] restores. */
    fun remove(place: RemovedPlace) {
        viewModelScope.launch { cityStore.remove(place.city) }
    }

    fun undoRemove(place: RemovedPlace) {
        viewModelScope.launch {
            cityStore.insert(place.city, place.index)
            if (place.wasActive) cityStore.setActive(place.city)
        }
    }

    /**
     * The «Annulla» of a removal has passed unused (15.13, 29 Oct 2026): the place is gone
     * for good, and the alerts bound to it go off now, job or no job, through the same
     * reconciliation every edit of Alerts makes. Not at the removal itself: a place put back
     * by «Annulla» has never lost its alerts.
     */
    fun settleRemoval() {
        viewModelScope.launch { runCatching { reconcileSync() } }
    }

    /**
     * The device-position flow, called only AFTER the permission is granted (the UI
     * owns the permission dialog; this owns everything behind it). Fix first, then
     * the toggle: enabling a source that cannot name a place yet would flash "no
     * place" at whoever is watching.
     */
    fun enableGps() {
        if (gpsStateFlow.value == GpsState.Acquiring) return
        gpsStateFlow.value = GpsState.Acquiring
        viewModelScope.launch {
            try {
                // Now, not "recent enough": the reader has just asked for their
                // position, and the page settle this enable causes will read the
                // provider's memo rather than pay for a second acquisition.
                val fix = locationProvider.currentFix(maxAge = LocationProvider.Now)
                arrival.take(fix, clock.instant())
                cityStore.setUseGps(true)
                cityStore.markInitDone()
                gpsStateFlow.value = GpsState.Idle
            } catch (e: WeatherException) {
                gpsStateFlow.value = GpsState.Error(e.asGpsError())
            }
        }
    }

    fun disableGps() {
        viewModelScope.launch { cityStore.setUseGps(false) }
        gpsStateFlow.value = GpsState.Idle
    }

    /**
     * Tapping the enabled position row: what swiping onto its page on Today does
     * (committente, 30 ott 2026). The page is selected and the sheet closes at once, on
     * the place the last fix found and its weather; behind it [GpsRefix] re-takes the
     * position, silently and only when the last fix is older than five minutes, and the
     * page moves only if the reader has, with the new place's weather in hand.
     *
     * Until then the tap asked for a new fix whatever the age of the last one and held
     * the sheet open on it for up to fifteen seconds (Fase 3b), over a page that had
     * already switched behind the sheet. "Where I am now" was the reason, and the
     * arrival rule of 20 ott 2026 keeps it on the silent road with nobody waiting.
     *
     * Two things of Fase 3b stay. What the row can see without taking a fix (the
     * permission revoked, the location switched off) keeps the sheet open and says so,
     * because the fix that follows is silent and a revoked permission must still be said
     * somewhere. And with no place found yet there is nothing to show while looking:
     * that tap is an ask out loud, [locate]. The last position is kept either way: old
     * place, real weather.
     */
    fun selectGps() {
        if (gpsStateFlow.value == GpsState.Acquiring) return
        viewModelScope.launch {
            cityStore.setActiveGps()
            if (cityStore.locationSettings.first().gpsCity == null) {
                locate()
                return@launch
            }
            locationProvider.obstacle()?.let { obstacle ->
                gpsStateFlow.value = GpsState.Error(obstacle.asGpsError())
                return@launch
            }
            gpsStateFlow.value = GpsState.Idle
            gpsSelectedFlow.tryEmit(Unit)
            gpsRefix.run()
        }
    }

    /**
     * The position asked for out loud from the row, when there is no place to show yet.
     * The row goes to [GpsState.Acquiring] while it runs, because a tap that takes fifteen
     * seconds and shows nothing reads as a dead row; the sheet closes when the position
     * answers, and stays with the reason when it does not.
     */
    private suspend fun locate() {
        if (gpsStateFlow.value == GpsState.Acquiring) return
        gpsStateFlow.value = GpsState.Acquiring
        try {
            val fix = locationProvider.currentFix(maxAge = LocationProvider.Now)
            arrival.take(fix, clock.instant())
            gpsStateFlow.value = GpsState.Idle
            gpsSelectedFlow.tryEmit(Unit)
        } catch (e: WeatherException) {
            gpsStateFlow.value = GpsState.Error(e.asGpsError())
        }
    }

    /**
     * Clears a failure that has been read. Only a failure: an acquisition in flight is
     * not something to dismiss, and since Fase 3b both the toggle and the row can be
     * the thing in flight.
     */
    fun dismissGpsError() {
        if (gpsStateFlow.value is GpsState.Error) gpsStateFlow.value = GpsState.Idle
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                PlacesViewModel(
                    repository = ServiceLocator.weatherRepository(app),
                    cityStore = ServiceLocator.cityStore(app),
                    searchHistory = ServiceLocator.searchHistoryStore(app),
                    locationProvider = ServiceLocator.locationProvider(app),
                    reconcileSync = { SyncScheduler.reconcile(app) },
                    gpsRefix = ServiceLocator.gpsRefix(app)
                )
            }
        }
    }
}
