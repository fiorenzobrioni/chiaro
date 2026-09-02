package com.callbackdev.chiaro.ui.places

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.callbackdev.chiaro.data.ActiveSource
import com.callbackdev.chiaro.data.CityStore
import com.callbackdev.chiaro.data.ServiceLocator
import com.callbackdev.chiaro.data.WeatherRepository
import com.callbackdev.chiaro.domain.WeatherException
import com.callbackdev.chiaro.domain.model.City
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

/**
 * The minimum of Fase 3 that Fase 2 cannot exist without (deviation recorded in
 * PLANNING.md): a fresh install has NO city — seeding a fake one is forbidden — so the
 * empty state needs a sheet that can search, add and select a real place. The full
 * Places surface (GPS, reorder, swipe-to-remove, first run) stays Fase 3 work.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class PlacesViewModel(
    private val repository: WeatherRepository,
    private val cityStore: CityStore
) : ViewModel() {

    val cities: StateFlow<List<City>> = cityStore.cities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val active: StateFlow<ActiveSource?> = cityStore.activeSource
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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

    /** A tapped search result: saved, made active, and the first-run debt settled —
     * choosing a place IS the first-run answer, wherever it is given from. */
    fun choose(city: City) {
        viewModelScope.launch {
            cityStore.add(city)
            cityStore.markInitDone()
        }
    }

    fun select(city: City) {
        viewModelScope.launch { cityStore.setActive(city) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                PlacesViewModel(
                    repository = ServiceLocator.weatherRepository(app),
                    cityStore = ServiceLocator.cityStore(app)
                )
            }
        }
    }
}
