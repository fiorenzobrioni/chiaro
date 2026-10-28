package com.callbackdev.chiaro.domain.settings

/**
 * The arithmetic of the units (14.9, 28 Oct 2026), in one place: the engines keep km/h
 * and hPa, and every surface that prints a speed or a pressure, or reads a threshold the
 * reader picked in their unit, converts here. Pure, so `Formats` and the rules' registry
 * share the same factors and the same Beaufort bands.
 */
object Units {

    private const val KM_PER_MILE = 1.609344
    private const val KM_PER_NAUTICAL_MILE = 1.852
    private const val INHG_PER_HPA = 0.029529983
    private const val MMHG_PER_HPA = 0.750061683

    /**
     * The lower bound in km/h of each Beaufort force, 0 to 12: the WMO scale as the
     * services print it in km/h (calm under 1; 1–5, 6–11, 12–19, 20–28, 29–38, 39–49,
     * 50–61, 62–74, 75–88, 89–102, 103–117, 118 and up). The app's own wind ladder
     * already sits on two of its edges: «tieni il cappello» from 39 (force 6), a day's
     * gust worth naming from 62 (force 8).
     */
    val BeaufortLowerKph: List<Double> =
        listOf(0.0, 1.0, 6.0, 12.0, 20.0, 29.0, 39.0, 50.0, 62.0, 75.0, 89.0, 103.0, 118.0)

    /** The Beaufort force the speed falls in, 0 to 12. */
    fun beaufort(kph: Double): Int =
        BeaufortLowerKph.indexOfLast { kph >= it }.coerceAtLeast(0)

    /**
     * A speed in km/h as the reader's unit counts it. Beaufort is the force, a whole
     * number: a band, not a scaled speed.
     */
    fun speedFromKph(kph: Double, unit: WindSpeedUnit): Double = when (unit) {
        WindSpeedUnit.KMH -> kph
        WindSpeedUnit.MPH -> kph / KM_PER_MILE
        WindSpeedUnit.M_S -> kph / 3.6
        WindSpeedUnit.KNOTS -> kph / KM_PER_NAUTICAL_MILE
        WindSpeedUnit.BEAUFORT -> beaufort(kph).toDouble()
    }

    /**
     * A speed the reader wrote in their unit, back to km/h. A Beaufort force becomes the
     * band's lower bound, so «at least force 6» is «at least 39 km/h», the threshold the
     * word means; a force past 12 or under 0 is clamped to the scale.
     */
    fun speedToKph(value: Double, unit: WindSpeedUnit): Double = when (unit) {
        WindSpeedUnit.KMH -> value
        WindSpeedUnit.MPH -> value * KM_PER_MILE
        WindSpeedUnit.M_S -> value * 3.6
        WindSpeedUnit.KNOTS -> value * KM_PER_NAUTICAL_MILE
        WindSpeedUnit.BEAUFORT ->
            BeaufortLowerKph[Math.round(value).toInt().coerceIn(0, BeaufortLowerKph.lastIndex)]
    }

    /** A pressure in hPa as the reader's unit counts it. */
    fun pressureFromHpa(hpa: Double, unit: PressureUnit): Double = when (unit) {
        PressureUnit.HPA -> hpa
        PressureUnit.INHG -> hpa * INHG_PER_HPA
        PressureUnit.MMHG -> hpa * MMHG_PER_HPA
    }

    /** A pressure the reader wrote in their unit, back to hPa. */
    fun pressureToHpa(value: Double, unit: PressureUnit): Double = when (unit) {
        PressureUnit.HPA -> value
        PressureUnit.INHG -> value / INHG_PER_HPA
        PressureUnit.MMHG -> value / MMHG_PER_HPA
    }

    /**
     * The decimals a value in this unit is written with when it is a reading or a
     * threshold (the rules): none for a Beaufort force, two for inches of mercury (29.92
     * is how that unit's readers write it: one decimal is 3.4 hPa, coarser than the
     * everyday swing), one for everything else.
     */
    fun speedDecimals(unit: WindSpeedUnit): Int = if (unit == WindSpeedUnit.BEAUFORT) 0 else 1

    fun pressureDecimals(unit: PressureUnit): Int = if (unit == PressureUnit.INHG) 2 else 1
}
