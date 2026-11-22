package com.callbackdev.chiaro.domain.correction

// Written by `python tools/analyze_probability.py --write-table` (PLANNING.md, «18.5»). Do not
// edit by hand: re-running the tool is the change, and `--check` says when this file is behind.

/**
 * The calibration of the probability of precipitation of the first day (Fase 18, 18.5): a
 * logistic regression of «something falls at the station in that hour» on the logit of
 * `best_match`'s probability (clipped to 1-99%), the share of the models with 0.1 mm or more
 * (`best_match` and the witnesses present), whether `best_match` has 0.1 mm or more, and
 * ln(1 + its amount). Only the zones where it passed the criterion.
 *
 * Trained on: metar-2025-12-01-2026-01-31.json.gz, metar-2026-09-26-2026-10-25.json.gz.
 */
internal object RainProbabilityTable {
    /** Intercept, logit(p), share of models wet, best_match wet, ln(1 + mm). */
    val COEFFICIENTS: Map<BlendZone, List<Double>> = mapOf(
        BlendZone.PO_VALLEY to listOf(-2.2536, 0.4066, 3.4192, 0.4474, 1.3692)
    )
}
