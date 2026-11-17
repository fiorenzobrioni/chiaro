package com.callbackdev.chiaro.domain

import com.callbackdev.chiaro.domain.model.PollenLevel

/**
 * The conversion of raw Open-Meteo values that is not a weather code: the pollen's level.
 * The codes themselves live in [WmoCode] since 24 set 2026, together with everything
 * decided from them. The wind's compass point lived here until 17 nov 2026: computed for
 * every report and printed by no screen, which draw the direction from its degrees.
 *
 * This object used to be tweather's label table — English descriptions with an emoji,
 * `"Moderate ☀️"` for a UV index, `"Good ⚪"` for an air quality — none of which ever
 * reached a screen of this app: every surface localizes through `WeatherText`.
 */
object WeatherCodes {

    /**
     * The pollen species Open-Meteo serves (CAMS, Europe only), each with the lower
     * bounds of MeteoSwiss' moderate, high and very-high classes in grains/m³ («Threshold
     * values for pollen load classes of allergenic pollen types»; low starts at 1 for all).
     *
     * Per species since 24 set 2026. The old single scale (1/30/100) called 30 grains of
     * ragweed «low» where MeteoSwiss says «high» from 11, and 100 of grass «high» where
     * the scale has a class above it from 150. The olive is not in the Swiss table — it
     * does not grow there — and takes the ash's thresholds: same family, Oleaceae, and
     * cross-reactive allergens. The table is for mean DAILY concentrations, and since 17
     * nov 2026 that is what it is given: the mean of the day's hours, not the current
     * hour's count, which showed another class than its day's in one in-season hour in four.
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
