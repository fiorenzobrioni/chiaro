package com.callbackdev.chiaro.ui.today

import androidx.annotation.StringRes
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.model.PollenLevel

/**
 * The WMO vocabulary and the meaning lines, as string resources — which is the whole
 * localization architecture of this app: everything on screen is prose or data, so
 * everything on screen is a resource (VISION §8). The domain's own English
 * `WeatherCondition.description` never reaches a screen; it is tweather's JSON
 * vocabulary, not Chiaro's.
 *
 * The meaning bands are the product's editorial voice (DESIGN §1.2): every band is an
 * honest consequence, and a metric whose value has no consequence today still gets its
 * band's line — "nothing to do about it" is also an answer.
 */
object WeatherText {

    /** One word (or two) per WMO bucket. Day/night does not change the word. */
    @StringRes
    fun condition(wmoCode: Int): Int = when (wmoCode) {
        0 -> R.string.cond_clear
        1 -> R.string.cond_mostly_clear
        2 -> R.string.cond_partly_cloudy
        3 -> R.string.cond_overcast
        45, 48 -> R.string.cond_fog
        51, 53, 55 -> R.string.cond_drizzle
        56, 57 -> R.string.cond_freezing_drizzle
        61 -> R.string.cond_rain_light
        63 -> R.string.cond_rain
        65 -> R.string.cond_rain_heavy
        66, 67 -> R.string.cond_freezing_rain
        71 -> R.string.cond_snow_light
        73 -> R.string.cond_snow
        75 -> R.string.cond_snow_heavy
        77 -> R.string.cond_snow_grains
        80, 81 -> R.string.cond_showers
        82 -> R.string.cond_showers_violent
        85, 86 -> R.string.cond_snow_showers
        95 -> R.string.cond_thunderstorm
        96, 99 -> R.string.cond_thunderstorm_hail
        else -> R.string.cond_unknown
    }

    /** Burn-time bands for unprotected fair skin — estimates, and worded as such. */
    @StringRes
    fun uvMeaning(uvIndex: Int): Int = when {
        uvIndex <= 2 -> R.string.uv_meaning_low
        uvIndex <= 5 -> R.string.uv_meaning_moderate
        uvIndex <= 7 -> R.string.uv_meaning_high
        uvIndex <= 10 -> R.string.uv_meaning_very_high
        else -> R.string.uv_meaning_extreme
    }

    @StringRes
    fun windMeaning(speedKph: Double): Int = when {
        speedKph < 5 -> R.string.wind_meaning_calm
        speedKph < 20 -> R.string.wind_meaning_light
        speedKph < 39 -> R.string.wind_meaning_moderate
        speedKph < 62 -> R.string.wind_meaning_strong
        else -> R.string.wind_meaning_gale
    }

    @StringRes
    fun humidityMeaning(pct: Int): Int = when {
        pct < 30 -> R.string.humidity_meaning_dry
        pct <= 60 -> R.string.humidity_meaning_comfortable
        pct <= 80 -> R.string.humidity_meaning_humid
        else -> R.string.humidity_meaning_oppressive
    }

    /** The comfort scale that humidity alone cannot honestly claim. */
    @StringRes
    fun dewPointMeaning(celsius: Double): Int = when {
        celsius < 10 -> R.string.dew_meaning_dry
        celsius < 16 -> R.string.dew_meaning_pleasant
        celsius < 21 -> R.string.dew_meaning_sticky
        celsius < 24 -> R.string.dew_meaning_muggy
        else -> R.string.dew_meaning_oppressive
    }

    /** Tendencies, not forecasts: absolute pressure only says which way to lean. */
    @StringRes
    fun pressureMeaning(mb: Double): Int = when {
        mb < 1000 -> R.string.pressure_meaning_low
        mb <= 1020 -> R.string.pressure_meaning_normal
        else -> R.string.pressure_meaning_high
    }

    @StringRes
    fun visibilityMeaning(km: Double): Int = when {
        km >= 10 -> R.string.visibility_meaning_clear
        km >= 4 -> R.string.visibility_meaning_light_haze
        km >= 1 -> R.string.visibility_meaning_haze
        else -> R.string.visibility_meaning_fog
    }

    /** US AQI bands, said as what a body can do in them (VISION §3.3.3). */
    @StringRes
    fun aqiMeaning(aqi: Int): Int = when {
        aqi <= 50 -> R.string.aqi_meaning_good
        aqi <= 100 -> R.string.aqi_meaning_moderate
        aqi <= 150 -> R.string.aqi_meaning_sensitive
        aqi <= 200 -> R.string.aqi_meaning_unhealthy
        aqi <= 300 -> R.string.aqi_meaning_very_unhealthy
        else -> R.string.aqi_meaning_hazardous
    }

    @StringRes
    fun pollenLevel(level: PollenLevel): Int = when (level) {
        PollenLevel.NONE -> R.string.pollen_level_none
        PollenLevel.LOW -> R.string.pollen_level_low
        PollenLevel.MODERATE -> R.string.pollen_level_moderate
        PollenLevel.HIGH -> R.string.pollen_level_high
    }

    @StringRes
    fun pollenMeaning(worst: PollenLevel): Int = when (worst) {
        PollenLevel.NONE -> R.string.pollen_meaning_none
        PollenLevel.LOW -> R.string.pollen_meaning_low
        PollenLevel.MODERATE -> R.string.pollen_meaning_moderate
        PollenLevel.HIGH -> R.string.pollen_meaning_high
    }
}
