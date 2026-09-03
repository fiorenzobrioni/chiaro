package com.callbackdev.chiaro.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.callbackdev.chiaro.domain.settings.TemperatureUnit
import com.callbackdev.chiaro.domain.settings.WindSpeedUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * New with Fase 4 (the store predates it, its first screen does not): the defaults a
 * fresh install reads, the round-trips the Settings screen performs, and the rule
 * that a stored value the app no longer understands falls back instead of crashing.
 */
class SettingsStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun dataStore(): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = scope) { tmp.newFile("s.preferences_pb") }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `a fresh install reads every default`() = runBlocking {
        val settings = SettingsStore(dataStore()).settings.first()

        assertEquals(TemperatureUnit.CELSIUS, settings.units.temperature)
        assertEquals(WindSpeedUnit.KMH, settings.units.windSpeed)
        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertEquals(true, settings.dynamicColor)
        assertEquals(WeatherIcons.FILL, settings.weatherIcons)
        assertEquals(true, settings.skyEnabled)
        assertNull(settings.skyNotifyDefaultMin)
        assertEquals(false, settings.skyNotifyOnFail)
        assertEquals(DefaultUpdateFrequencyMin, settings.updateFrequencyMin)
        assertEquals(DefaultWidgetOpacityPct, settings.widgetOpacityPct)
    }

    @Test
    fun `choices round-trip`() = runBlocking {
        val store = SettingsStore(dataStore())

        store.setTemperatureUnit(TemperatureUnit.FAHRENHEIT)
        store.setWindSpeedUnit(WindSpeedUnit.MPH)
        store.setThemeMode(ThemeMode.DARK)
        store.setDynamicColor(false)
        store.setWeatherIcons(WeatherIcons.LINE)
        store.setUpdateFrequency(30)

        val settings = store.settings.first()
        assertEquals(TemperatureUnit.FAHRENHEIT, settings.units.temperature)
        assertEquals(WindSpeedUnit.MPH, settings.units.windSpeed)
        assertEquals(ThemeMode.DARK, settings.themeMode)
        assertEquals(false, settings.dynamicColor)
        assertEquals(WeatherIcons.LINE, settings.weatherIcons)
        assertEquals(30, settings.updateFrequencyMin)
    }

    /** A stored name this version no longer ships must read as the default, never throw. */
    @Test
    fun `an unrecognized stored enum falls back to its default`() = runBlocking {
        val ds = dataStore()
        ds.edit {
            it[stringPreferencesKey("units_temperature")] = "KELVIN"
            it[stringPreferencesKey("appearance_theme_mode")] = "OBSIDIAN"
        }

        val settings = SettingsStore(ds).settings.first()
        assertEquals(TemperatureUnit.CELSIUS, settings.units.temperature)
        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
    }

    /** An interval outside the offered set reads as the default, same rule as the enums. */
    @Test
    fun `an update frequency the app does not offer falls back`() = runBlocking {
        val ds = dataStore()
        ds.edit { it[intPreferencesKey("sync_update_frequency_min")] = 7 }

        assertEquals(DefaultUpdateFrequencyMin, SettingsStore(ds).settings.first().updateFrequencyMin)
    }

    @Test
    fun `reset returns every choice to its default`() = runBlocking {
        val store = SettingsStore(dataStore())
        store.setThemeMode(ThemeMode.LIGHT)
        store.setTemperatureUnit(TemperatureUnit.FAHRENHEIT)
        store.setUpdateFrequency(15)

        store.resetToDefaults()

        val settings = store.settings.first()
        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertEquals(TemperatureUnit.CELSIUS, settings.units.temperature)
        assertEquals(DefaultUpdateFrequencyMin, settings.updateFrequencyMin)
    }
}
