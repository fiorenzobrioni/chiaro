package com.callbackdev.chiaro.domain.correction

// Written by `python tools/analyze_correction.py --write-table` (PLANNING.md, «18.4»). Do not
// edit by hand: re-running the tool is the change, and `--check` says when this file is behind.

/**
 * The weights of the temperature blend of days 6 and 7 (Fase 18, 18.1 and 18.4): `best_match`,
 * ECMWF IFS and the Met Office's global model, per zone and lead day, at steps of 0.1, chosen
 * for the lowest mean absolute error against the METARs of every airport of the zone.
 *
 * Trained on: metar-2025-12-01-2026-01-31.json.gz, metar-2026-09-26-2026-10-25.json.gz.
 */
internal object TemperatureBlendTable {
    val WEIGHTS: Map<BlendZone, Map<Int, BlendWeights>> = mapOf(
        BlendZone.PO_VALLEY to mapOf(
            6 to BlendWeights(bestMatch = 0.3, ifs = 0.3, ukmo = 0.4),
            7 to BlendWeights(bestMatch = 0.3, ifs = 0.3, ukmo = 0.4)
        ),
        BlendZone.CENTRE_SOUTH to mapOf(
            6 to BlendWeights(bestMatch = 0.2, ifs = 0.5, ukmo = 0.3),
            7 to BlendWeights(bestMatch = 0.3, ifs = 0.4, ukmo = 0.3)
        )
    )
}
