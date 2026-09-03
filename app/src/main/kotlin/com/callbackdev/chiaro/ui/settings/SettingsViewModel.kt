package com.callbackdev.chiaro.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.callbackdev.chiaro.data.AppSettings
import com.callbackdev.chiaro.data.ServiceLocator
import com.callbackdev.chiaro.data.SettingsStore
import com.callbackdev.chiaro.data.ThemeMode
import com.callbackdev.chiaro.data.WeatherIcons
import com.callbackdev.chiaro.domain.settings.TemperatureUnit
import com.callbackdev.chiaro.domain.settings.WindSpeedUnit
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * A thin hand between the Settings screen and [SettingsStore]: every row edits one
 * key, and the flow below is the same one the rest of the app already reads, so a
 * change is visible everywhere the moment it lands.
 */
class SettingsViewModel(private val store: SettingsStore) : ViewModel() {

    /** Null until the store's first answer: the screen draws nothing it cannot vouch
     * for, not even a default that might be about to change (DESIGN §1.1). */
    val settings: StateFlow<AppSettings?> = store.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setTemperatureUnit(unit: TemperatureUnit) =
        viewModelScope.launch { store.setTemperatureUnit(unit) }

    fun setWindSpeedUnit(unit: WindSpeedUnit) =
        viewModelScope.launch { store.setWindSpeedUnit(unit) }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { store.setThemeMode(mode) }

    fun setDynamicColor(enabled: Boolean) =
        viewModelScope.launch { store.setDynamicColor(enabled) }

    fun setWeatherIcons(style: WeatherIcons) =
        viewModelScope.launch { store.setWeatherIcons(style) }

    fun setUpdateFrequency(minutes: Int) =
        viewModelScope.launch { store.setUpdateFrequency(minutes) }

    fun setWidgetOpacity(pct: Int) = viewModelScope.launch { store.setWidgetOpacity(pct) }

    fun resetToDefaults() = viewModelScope.launch { store.resetToDefaults() }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                SettingsViewModel(store = ServiceLocator.settingsStore(app))
            }
        }
    }
}
