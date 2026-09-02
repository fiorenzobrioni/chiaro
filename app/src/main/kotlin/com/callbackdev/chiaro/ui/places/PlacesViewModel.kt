package com.callbackdev.chiaro.ui.places

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.callbackdev.chiaro.data.ActiveSource
import com.callbackdev.chiaro.data.CityStore
import com.callbackdev.chiaro.data.LocationProvider
import com.callbackdev.chiaro.data.LocationSettings
import com.callbackdev.chiaro.data.SearchHistoryStore
import com.callbackdev.chiaro.data.ServiceLocator
import com.callbackdev.chiaro.data.WeatherRepository
import com.callbackdev.chiaro.domain.WeatherException
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.toGpsCity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
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
    private val locationProvider: LocationProvider
) : ViewModel() {

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
                val fix = locationProvider.currentFix()
                cityStore.updateGpsCity(fix.toGpsCity())
                cityStore.setUseGps(true)
                cityStore.markInitDone()
                gpsStateFlow.value = GpsState.Idle
            } catch (e: WeatherException) {
                gpsStateFlow.value = GpsState.Error(
                    when (e) {
                        is WeatherException.LocationPermissionDenied -> GpsError.PERMISSION
                        is WeatherException.LocationDisabled -> GpsError.DISABLED
                        is WeatherException.LocationTimeout -> GpsError.TIMEOUT
                        else -> GpsError.UNAVAILABLE
                    }
                )
            }
        }
    }

    fun disableGps() {
        viewModelScope.launch { cityStore.setUseGps(false) }
        gpsStateFlow.value = GpsState.Idle
    }

    /** Tapping the enabled GPS row: select it, and quietly refresh the fix — the
     * reader who taps "my position" means where they are now, not where they were. */
    fun selectGps() {
        viewModelScope.launch {
            cityStore.setActiveGps()
            runCatching { locationProvider.currentFix() }
                .onSuccess { cityStore.updateGpsCity(it.toGpsCity()) }
            // a failed silent re-fix keeps the last one: old position, real weather
        }
    }

    fun dismissGpsError() {
        gpsStateFlow.value = GpsState.Idle
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                PlacesViewModel(
                    repository = ServiceLocator.weatherRepository(app),
                    cityStore = ServiceLocator.cityStore(app),
                    searchHistory = ServiceLocator.searchHistoryStore(app),
                    locationProvider = ServiceLocator.locationProvider(app)
                )
            }
        }
    }
}
