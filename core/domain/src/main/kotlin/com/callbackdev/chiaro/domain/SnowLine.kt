package com.callbackdev.chiaro.domain

import kotlin.math.roundToInt

/**
 * The snow line: the height above which what falls is snow (Fase 14.8, 28 Oct 2026).
 *
 * An hour's line is the model's own `snowfall_height` where the model carries one, and
 * otherwise the freezing level less [FREEZING_LEVEL_OFFSET_M], **said to be an estimate**:
 * a flake keeps falling below the 0 °C isotherm while it melts, and the snow line sits
 * 200 to 400 m under the freezing level (MeteoSwiss's own rule for the forecasts it
 * prints, «Schneefallgrenze»: «in der Regel 200 bis 400 Meter unterhalb der Nullgradgrenze»,
 * the middle of the band taken here). The number is one model's, like every other
 * (CLAUDE.md): what the app adds is the subtraction, and the line says so with «circa».
 *
 * A day's line is the lowest of its wet hours' (`WeatherReportMapper`), and the day
 * carries one only when its precipitation is material enough to label it: a line over a
 * day with one hour of 0.1 mm would be a fact about a rain the app has already decided
 * not to believe.
 */
data class SnowLine(
    /** Metres above sea level. */
    val metres: Int,
    /** True when read off the freezing level, false when the model forecast the line itself. */
    val estimated: Boolean
) {
    /**
     * Whether the line is worth a line at a place [elevationM] metres up: within
     * [NEARBY_M] above it, or anywhere below it. Above that the snow is somebody else's
     * weather — a line at 2 800 m over Milan is every winter rain — and below the place the
     * snow is the place's own, which the day's state already says, and the line adds where
     * it turns to rain. A place whose height the response did not carry reads as sea
     * level: the row then appears only for a line within [NEARBY_M] of the sea, which is
     * never a line too far to matter.
     */
    fun matters(elevationM: Double?): Boolean = metres <= (elevationM ?: 0.0) + NEARBY_M

    /** Whether the snow reaches the place itself, or stays above it. */
    fun reaches(elevationM: Double?): Boolean = metres <= (elevationM ?: 0.0)

    companion object {
        /** What the snow line sits under the freezing level, metres: the middle of MeteoSwiss's 200–400. */
        const val FREEZING_LEVEL_OFFSET_M = 300

        /** How far above the place a line still matters to it, metres. */
        const val NEARBY_M = 1500

        /**
         * An hour's line from what the model sent: its own [snowfallHeightM] first, the
         * [freezingLevelM] less [FREEZING_LEVEL_OFFSET_M] otherwise, null with neither. A
         * line under the sea is the sea's: snow down to the ground is «0 m», not a minus.
         */
        fun of(snowfallHeightM: Double?, freezingLevelM: Double?): SnowLine? = when {
            snowfallHeightM != null -> SnowLine(snowfallHeightM.roundToInt().coerceAtLeast(0), estimated = false)
            freezingLevelM != null ->
                SnowLine((freezingLevelM - FREEZING_LEVEL_OFFSET_M).roundToInt().coerceAtLeast(0), estimated = true)
            else -> null
        }

        /** The lowest of [lines], with its own provenance; null when there is none. */
        fun lowest(lines: Iterable<SnowLine>): SnowLine? = lines.minByOrNull { it.metres }
    }
}
