package com.callbackdev.chiaro.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import com.callbackdev.chiaro.domain.settings.NotificationSettings
import com.callbackdev.chiaro.domain.settings.TemperatureUnit
import com.callbackdev.chiaro.domain.settings.UnitSettings
import com.callbackdev.chiaro.domain.settings.WindSpeedUnit

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/**
 * Light, dark, or whatever the phone says. Lives here and not in `:core:domain`
 * because no engine reads it: the theme decides how a number looks, never what it is.
 */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Everything the Settings screen edits. The engine inputs ([units], [notifications],
 * the sky keys) are typed in `:core:domain`; the rest is presentation and stays here.
 */
data class AppSettings(
    val units: UnitSettings = UnitSettings(),
    val notifications: NotificationSettings = NotificationSettings(),
    /** SYSTEM by default: an app that follows the phone needs no explaining. */
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /**
     * Material's wallpaper-derived scheme, on by default (DESIGN §2.1). Off gives the
     * generated Chiaro scheme — for readers who want the app to look like itself.
     */
    val dynamicColor: Boolean = true,
    /**
     * The Sky screen's master switch (its surface arrives in Fase 5). Default true:
     * the sky is the differentiator, and a feature that ships switched off is a
     * feature nobody finds.
     */
    val skyEnabled: Boolean = true,
    /**
     * The reminder lead every subscribed sky moment uses unless it carries one of its
     * own. Minutes, or null for no reminder. Off by default: turning the sky on to
     * read it must not start notifying for it.
     */
    val skyNotifyDefaultMin: Int? = null,
    /**
     * Send the reminder even when the sky will not allow the event. False by default:
     * a reminder for something you cannot see is noise.
     */
    val skyNotifyOnFail: Boolean = false,
    val updateFrequencyMin: Int = DefaultUpdateFrequencyMin,
    val widgetOpacityPct: Int = DefaultWidgetOpacityPct
)

/** Foreground cache TTL AND background polling interval (one number on purpose:
 * "how fresh should the weather be" is one question, not two). */
val UpdateFrequencies = listOf(15, 30, 60, 120)

/** 60: right default for a polling interval (decision inherited from tweather). */
const val DefaultUpdateFrequencyMin = 60

/** Home-widget background opacity: alpha on the card fill only, the border stays crisp. */
val WidgetOpacities = listOf(100, 85, 70, 50)

const val DefaultWidgetOpacityPct = 100

/** App settings persisted as DataStore preferences. */
class SettingsStore(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<AppSettings> = dataStore.data
        .map { prefs ->
            AppSettings(
                units = UnitSettings(
                    temperature = enumOrDefault(prefs[Temperature], TemperatureUnit.CELSIUS),
                    windSpeed = enumOrDefault(prefs[WindSpeed], WindSpeedUnit.KMH)
                ),
                notifications = NotificationSettings(
                    severeWeatherAlerts = prefs[SevereAlerts] ?: true,
                    dailySummary = prefs[DailySummary] ?: false,
                    precipitationWarning = prefs[PrecipWarning] ?: true,
                    userRules = prefs[UserRules] ?: true
                ),
                themeMode = enumOrDefault(prefs[Theme], ThemeMode.SYSTEM),
                dynamicColor = prefs[DynamicColor] ?: true,
                skyEnabled = prefs[SkyEnabled] ?: true,
                // 0 is how "off" is stored: an Int? preference cannot hold null, and
                // absent must read the same as explicitly switched off.
                skyNotifyDefaultMin = prefs[SkyNotifyDefault]?.takeIf { it > 0 },
                skyNotifyOnFail = prefs[SkyNotifyOnFail] ?: false,
                updateFrequencyMin = (prefs[UpdateFrequencyMin] ?: DefaultUpdateFrequencyMin)
                    .takeIf { it in UpdateFrequencies } ?: DefaultUpdateFrequencyMin,
                widgetOpacityPct = (prefs[WidgetOpacity] ?: DefaultWidgetOpacityPct)
                    .takeIf { it in WidgetOpacities } ?: DefaultWidgetOpacityPct
            )
        }
        .distinctUntilChanged()

    suspend fun setTemperatureUnit(unit: TemperatureUnit) = set(Temperature, unit.name)
    suspend fun setWindSpeedUnit(unit: WindSpeedUnit) = set(WindSpeed, unit.name)
    suspend fun setSevereWeatherAlerts(enabled: Boolean) = set(SevereAlerts, enabled)
    suspend fun setDailySummary(enabled: Boolean) = set(DailySummary, enabled)
    suspend fun setPrecipitationWarning(enabled: Boolean) = set(PrecipWarning, enabled)
    suspend fun setUserRules(enabled: Boolean) = set(UserRules, enabled)
    suspend fun setThemeMode(mode: ThemeMode) = set(Theme, mode.name)
    suspend fun setDynamicColor(enabled: Boolean) = set(DynamicColor, enabled)
    suspend fun setSkyEnabled(enabled: Boolean) = set(SkyEnabled, enabled)

    /** 0 stands for "off": DataStore has no nullable Int, and absent means default. */
    suspend fun setSkyNotifyDefault(minutes: Int?) = set(SkyNotifyDefault, minutes ?: 0)

    suspend fun setSkyNotifyOnFail(enabled: Boolean) = set(SkyNotifyOnFail, enabled)
    suspend fun setUpdateFrequency(minutes: Int) = set(UpdateFrequencyMin, minutes)
    suspend fun setWidgetOpacity(pct: Int) = set(WidgetOpacity, pct)

    /**
     * The Settings screen's reset: clears every stored preference, so everything
     * reads its default again. Places, history and the guide card are other stores
     * on purpose — resetting choices must not touch data or reopen one-time hints.
     */
    suspend fun resetToDefaults() {
        dataStore.edit { it.clear() }
    }

    private suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        dataStore.edit { it[key] = value }
    }

    private inline fun <reified E : Enum<E>> enumOrDefault(name: String?, default: E): E =
        name?.let { runCatching { enumValueOf<E>(it) }.getOrNull() } ?: default

    companion object {
        private val Temperature = stringPreferencesKey("units_temperature")
        private val WindSpeed = stringPreferencesKey("units_wind_speed")
        private val SevereAlerts = booleanPreferencesKey("notif_severe_alerts")
        private val DailySummary = booleanPreferencesKey("notif_daily_summary")
        private val PrecipWarning = booleanPreferencesKey("notif_precip_warning")
        private val UserRules = booleanPreferencesKey("notif_user_rules")
        private val Theme = stringPreferencesKey("appearance_theme_mode")
        private val DynamicColor = booleanPreferencesKey("appearance_dynamic_color")
        private val SkyEnabled = booleanPreferencesKey("sky_enabled")
        private val SkyNotifyDefault = intPreferencesKey("sky_notify_default_min")
        private val SkyNotifyOnFail = booleanPreferencesKey("sky_notify_on_fail")
        private val UpdateFrequencyMin = intPreferencesKey("sync_update_frequency_min")
        private val WidgetOpacity = intPreferencesKey("widget_bg_opacity_pct")

        fun create(context: Context) = SettingsStore(context.settingsDataStore)
    }
}
