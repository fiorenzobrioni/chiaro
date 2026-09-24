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
     * The pollen species Open-Meteo serves (CAMS, Europe only), each with the lower
     * bounds of MeteoSwiss' moderate, high and very-high classes in grains/m³ («Threshold
     * values for pollen load classes of allergenic pollen types»; low starts at 1 for all).
     *
     * Per species since 24 set 2026. The old single scale (1/30/100) called 30 grains of
     * ragweed «low» where MeteoSwiss says «high» from 11, and 100 of grass «high» where
     * the scale has a class above it from 150. The olive is not in the Swiss table — it
     * does not grow there — and takes the ash's thresholds: same family, Oleaceae, and
     * cross-reactive allergens. The table is for mean DAILY concentrations and the value
     * here is the current hour's: the same approximation the old scale made, now on the
     * right numbers.
     */
    enum class PollenSpecies(val moderate: Double, val high: Double, val veryHigh: Double) {
        GRASS(20.0, 50.0, 150.0),
        BIRCH(11.0, 70.0, 300.0),
        ALDER(11.0, 70.0, 250.0),
        OLIVE(11.0, 100.0, 350.0),
        RAGWEED(6.0, 11.0, 40.0),
        MUGWORT(6.0, 15.0, 50.0)
    }

    /** The class [grainsPerM3] of [species] falls in; null in, null out. */
    fun pollenLevel(species: PollenSpecies, grainsPerM3: Double?): PollenLevel? = when {
        grainsPerM3 == null -> null
        grainsPerM3 < 1.0 -> PollenLevel.NONE
        grainsPerM3 < species.moderate -> PollenLevel.LOW
        grainsPerM3 < species.high -> PollenLevel.MODERATE
        grainsPerM3 < species.veryHigh -> PollenLevel.HIGH
        else -> PollenLevel.VERY_HIGH
    }

    /**
     * A family's level: the worst of its species, each on its own scale — comparing grains
     * across species was the old scale's mistake. Null when no species of the family was
     * served (outside Europe), so the report says nothing rather than «none».
     */
    fun pollenFamilyLevel(vararg readings: Pair<PollenSpecies, Double?>): PollenLevel? =
        readings.mapNotNull { (species, grains) -> pollenLevel(species, grains) }.maxByOrNull { it.ordinal }
}
