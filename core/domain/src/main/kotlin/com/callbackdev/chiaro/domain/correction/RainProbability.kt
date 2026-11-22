package com.callbackdev.chiaro.domain.correction

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt

/**
 * The calibrated probability of precipitation of the first day (Fase 18, 22 nov 2026,
 * PLANNING «18.5»): what the professional services do before they print a probability, so that
 * a 70% is wet seven times in ten. `best_match`'s own was too sure at the top and too shy at
 * the bottom (Italian airports, two periods: said 99%, wet 87% of the hours; 59% → 42%;
 * 8% → 14%), and what tells the hours apart is how many models see the rain. A logistic
 * regression (the MOS of DWD's MOSMIX) on `best_match`'s probability, the share of the models
 * wet, and `best_match`'s own amount: over the Po valley its Brier score is 15–17% lower than
 * the raw probability's, it is more reliable, and the engine's rule (≥ 0.1 mm and ≥ 20%) scores
 * higher with it. It passed nowhere else, so nowhere else is it applied.
 *
 * Pure: where, the coefficients ([RainProbabilityTable], written by
 * `tools/analyze_probability.py --write-table`), the arithmetic.
 */
object RainProbability {

    /** The first day's mark in the history (`DailyForecast.correction`): lead day 1. */
    const val LEAD_DAY = 1

    /** The measured event's threshold: 0.1 mm, the provider's own and the engine's. */
    const val WET_MM = 0.1

    /** The zone of a place where the calibration applies, or null: the Po valley of 18.4. */
    fun zoneOf(countryCode: String?, latitude: Double, elevationM: Double?): BlendZone? =
        TemperatureBlend.zoneOf(countryCode, latitude, elevationM)
            ?.takeIf { it in RainProbabilityTable.COEFFICIENTS }

    /**
     * The calibrated probability, in whole percent, of an hour where `best_match` says
     * [probabilityPct] and [bestMm], and the witnesses present say [witnessMm] (at least one:
     * the regression was trained with them, and the share of one model is not the same thing).
     * Null where the zone has no coefficients or no witness is there.
     */
    fun calibrated(zone: BlendZone, probabilityPct: Int, bestMm: Double, witnessMm: List<Double>): Int? {
        val beta = RainProbabilityTable.COEFFICIENTS[zone] ?: return null
        if (witnessMm.isEmpty()) return null
        val bestWet = bestMm >= WET_MM - 1e-9
        val wet = (if (bestWet) 1 else 0) + witnessMm.count { it >= WET_MM - 1e-9 }
        val share = wet.toDouble() / (witnessMm.size + 1)
        val p = (probabilityPct / 100.0).coerceIn(0.01, 0.99)
        val z = beta[0] + beta[1] * ln(p / (1 - p)) + beta[2] * share +
            beta[3] * (if (bestWet) 1.0 else 0.0) + beta[4] * ln(1 + bestMm.coerceAtLeast(0.0))
        return (100.0 / (1 + exp(-z.coerceIn(-30.0, 30.0)))).roundToInt()
    }
}
