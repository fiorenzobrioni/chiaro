package com.callbackdev.chiaro.domain.correction

import kotlin.math.exp
import kotlin.math.ln

/** The two Italian zones the blend was measured on (PLANNING.md, «18.1» and «18.4»). */
enum class BlendZone { PO_VALLEY, CENTRE_SOUTH }

/** The weights of one zone and lead day: they sum to 1 (`tools/analyze_correction.py`). */
data class BlendWeights(val bestMatch: Double, val ifs: Double, val ukmo: Double)

/**
 * The temperature blend of days 6 and 7 (Fase 18, 21–22 nov 2026): where `best_match` in Italy
 * is ICON global, its temperature blended with ECMWF IFS's and the Met Office's is closer to
 * what the airports measured — over the Po valley 2.05 → 1.72 °C in winter and 1.65 → 1.43 in
 * October, over the centre-south 2.39 → 1.72 and 1.60 → 1.39 (mean absolute error, every
 * airport tested on a table that had seen neither it nor its days; PLANNING, «18.1 — La misura
 * di partenza: fatto»). Nowhere else and on no other day did a correction beat `best_match`
 * by the 0.2 °C the criterion asked, so nowhere else is one applied.
 *
 * Pure: which places and days, the weights, the arithmetic. The data layer lays the
 * witnesses' columns on the forecast's hours and calls it.
 */
object TemperatureBlend {

    /** Italy, and the two states inside it. */
    val COUNTRIES = setOf("IT", "SM", "VA")

    /**
     * The highest the blend goes: the airports measured are 2 to 301 m (Caselle), and where
     * nothing was measured the number is the model's. The place's elevation is the response's,
     * the one the provider corrects its temperature to.
     */
    const val MAX_ELEVATION_M = 350.0

    /** North of it the Po valley's table, south the centre-south's (Bologna 44.5°, Rome 41.8°). */
    const val PO_VALLEY_FROM_LATITUDE = 44.0

    /**
     * The days of the response it applies to, by index from today (0): the sixth and the
     * seventh, whole, with the weights of lead day 6 and 7. By calendar day, not by hours ahead
     * of the fetch: a boundary that moved every hour would cut the sixth row in two and change
     * it at every update (PLANNING, «18.4», «I giorni»).
     */
    val LEAD_DAY_OF_INDEX = mapOf(5 to 6, 6 to 7)

    /** The zone of a place, or null where the blend was not measured. */
    fun zoneOf(countryCode: String?, latitude: Double, elevationM: Double?): BlendZone? {
        if (countryCode?.uppercase() !in COUNTRIES) return null
        if (elevationM == null || elevationM > MAX_ELEVATION_M) return null
        return if (latitude >= PO_VALLEY_FROM_LATITUDE) BlendZone.PO_VALLEY else BlendZone.CENTRE_SOUTH
    }

    /** The weights for [zone] on [leadDay], or null for a day the blend does not touch. */
    fun weights(zone: BlendZone, leadDay: Int): BlendWeights? =
        TemperatureBlendTable.WEIGHTS[zone]?.get(leadDay)

    /**
     * The blended temperature: the members present, their weights renormalised, as the
     * measurement scored it. [bestMatch] alone when no witness is there.
     */
    fun blend(weights: BlendWeights, bestMatch: Double, ifs: Double?, ukmo: Double?): Double {
        var sum = weights.bestMatch * bestMatch
        var total = weights.bestMatch
        if (ifs != null) {
            sum += weights.ifs * ifs
            total += weights.ifs
        }
        if (ukmo != null) {
            sum += weights.ukmo * ukmo
            total += weights.ukmo
        }
        return if (total > 1e-9) sum / total else bestMatch
    }

    /**
     * The dew point of air at [temperatureC] and [humidityPct]: the Magnus formula with
     * Alduchov and Eskridge's coefficients (1996), good to a tenth of a degree over the range
     * weather has. The humidity stays the model's; the dew point follows the blended air.
     */
    fun dewPointC(temperatureC: Double, humidityPct: Double): Double {
        val rh = humidityPct.coerceIn(1.0, 100.0) / 100.0
        val gamma = ln(rh) + MAGNUS_A * temperatureC / (MAGNUS_B + temperatureC)
        return MAGNUS_B * gamma / (MAGNUS_A - gamma)
    }

    /** The relative humidity of air at [temperatureC] with dew point [dewPointC], for tests. */
    fun humidityPct(temperatureC: Double, dewPointC: Double): Double =
        100.0 * exp(MAGNUS_A * dewPointC / (MAGNUS_B + dewPointC)) /
            exp(MAGNUS_A * temperatureC / (MAGNUS_B + temperatureC))

    private const val MAGNUS_A = 17.625
    private const val MAGNUS_B = 243.04
}
