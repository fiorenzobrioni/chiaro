package com.callbackdev.chiaro.domain

import com.callbackdev.chiaro.domain.model.PollenLevel

/**
 * The small conversions of raw Open-Meteo values that are not weather codes: the wind's
 * compass point and the pollen's coarse level. The codes themselves live in [WmoCode]
 * since 24 set 2026, together with everything decided from them.
 *
 * This object used to be tweather's label table — English descriptions with an emoji,
 * `"Moderate ☀️"` for a UV index, `"Good ⚪"` for an air quality — none of which ever
 * reached a screen of this app: every surface localizes through `WeatherText`.
 */
object WeatherCodes {

    private val COMPASS_POINTS = listOf(
        "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
        "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"
    )

    /** Wind direction in degrees → 16-point compass label (310 → `"NW"`). */
    fun windCompass(degree: Int): String {
        val normalized = ((degree % 360) + 360) % 360
        val index = ((normalized + 11.25) / 22.5).toInt() % COMPASS_POINTS.size
        return COMPASS_POINTS[index]
    }

    /**
     * Pollen concentration (grains/m³) → coarse level; null in, null out (Open-Meteo
     * pollen is Europe-only).
     */
    fun pollenLevel(grainsPerM3: Double?): PollenLevel? = when {
        grainsPerM3 == null -> null
        grainsPerM3 < 1.0 -> PollenLevel.NONE
        grainsPerM3 < 30.0 -> PollenLevel.LOW
        grainsPerM3 < 100.0 -> PollenLevel.MODERATE
        else -> PollenLevel.HIGH
    }
}
