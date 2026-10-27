package com.callbackdev.chiaro.data.mapper

import com.callbackdev.chiaro.data.remote.OpenMeteoWitnessApi
import com.callbackdev.chiaro.data.remote.dto.HourlyDto
import com.callbackdev.chiaro.data.remote.dto.WitnessResponseDto
import com.callbackdev.chiaro.domain.WeatherStateEngine
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The witnesses' columns laid on `best_match`'s hours (26 ott 2026), and the tests that
 * keep a model from voting twice.
 *
 * `best_match` picks its model per place and per day, and sometimes picks one of the
 * witnesses: ECMWF over Sydney all week and over the north of Europe from the fourth day,
 * the Met Office over London. A witness that is `best_match` itself agrees with it by
 * construction, and its vote would be the same model counted twice. So it is recognised
 * from the numbers, hour by hour — the temperature and the cloud equal to the decimal for
 * the hours around — and not from a table of Open-Meteo's choices, which can change
 * tomorrow (the principle of `NowcastCoverage.looksInterpolated`).
 *
 * And **per variable** (27 ott 2026, Fase 14): Open-Meteo fills a variable the chosen
 * model lacks from the largest model that carries it, so `best_match` can serve MET
 * Nordic's temperature with IFS's visibility (Oslo, Stockholm, Helsinki, every hour of
 * the two periods measured in 14.2; Marseille under AROME; Italy from 30 Nov 2026, when
 * DWD removes the visibility from ICON). A visibility equal to a witness's for the six
 * hours either side is that witness's: the hour has none of its own ([ownVisibilityOf]),
 * and the engine reads it as lent — the witnesses' lowest, that witness's among them, and
 * two of them to confirm, the lender one of the two (`WeatherStateEngine.visibilityOf`).
 * The number counts once, as the witness's; until then the vote at Oslo was IFS
 * confirming itself, the same number as the hour's own and as a witness's.
 */
internal class Witnesses private constructor(private val models: List<Series>) {

    private class Series(
        val temperatureC: List<Double?>,
        val cloudCoverPct: List<Double?>,
        val visibilityM: List<Double?>,
        val humidityPct: List<Double?>
    )

    /**
     * The witnesses of the hour that starts at [instant] (fog is read at the instant, like
     * the visibility of a row), without the ones that are `best_match` around it.
     */
    fun at(best: HourlyDto, instant: Int): List<WeatherStateEngine.Witness> =
        models.filterNot { it.copies(best, instant) }.map { w ->
            WeatherStateEngine.Witness(
                visibilityM = w.visibilityM.getOrNull(instant),
                humidityPct = w.humidityPct.getOrNull(instant)?.roundToInt()
            )
        }

    /**
     * [best]'s visibility at [instant] when it is really its own: null where the response
     * has none, or where the number is a witness's copy — then the engine reads the
     * witnesses' lowest instead, and asks two of them (`WeatherStateEngine.visibilityOf`).
     */
    fun ownVisibilityOf(best: HourlyDto, instant: Int): Double? =
        best.visibilityM.getOrNull(instant)?.takeUnless { lendsVisibility(best, instant) }

    private fun lendsVisibility(best: HourlyDto, at: Int): Boolean =
        models.any { !it.copies(best, at) && it.copiesVisibility(best, at) }

    /**
     * The same model as [best] around [at]: over the hours within [SAME_REACH_HOURS], at
     * least [SAME_MIN_HOURS] with both numbers, and the temperature and the cloud
     * equal within what a rounding leaves.
     */
    private fun Series.copies(best: HourlyDto, at: Int): Boolean =
        sameAround(at) { k ->
            val t = temperatureC.getOrNull(k) ?: return@sameAround null
            val c = cloudCoverPct.getOrNull(k) ?: return@sameAround null
            val bt = best.temperatureC.getOrNull(k) ?: return@sameAround null
            val bc = best.cloudCoverPct.getOrNull(k) ?: return@sameAround null
            abs(t - bt) <= SAME_TEMPERATURE_C && abs(c - bc) <= SAME_CLOUD_PCT
        }

    /**
     * The same visibility as [best] around [at], by the same test on that one column: the
     * fill Open-Meteo makes from this model where `best_match`'s own has none. A clear-air
     * plateau two models share for six hours (GFS and IFS both cap at 24 140 m) passes the
     * test too, and harmlessly: the fog hour itself, where the numbers differ, never does.
     */
    private fun Series.copiesVisibility(best: HourlyDto, at: Int): Boolean =
        sameAround(at) { k ->
            val v = visibilityM.getOrNull(k) ?: return@sameAround null
            val bv = best.visibilityM.getOrNull(k) ?: return@sameAround null
            abs(v - bv) < SAME_VISIBILITY_M
        }

    /** [equalAt] over the hours around [at]: null where a number is missing, else whether
     * they agree; a copy agrees in every hour that has them and has at least [SAME_MIN_HOURS]. */
    private inline fun sameAround(at: Int, equalAt: (Int) -> Boolean?): Boolean {
        var hours = 0
        for (k in at - SAME_REACH_HOURS..at + SAME_REACH_HOURS) {
            val same = equalAt(k) ?: continue
            if (!same) return false
            hours++
        }
        return hours >= SAME_MIN_HOURS
    }

    companion object {
        private val NONE = Witnesses(emptyList())

        /** Six hours either side: a model change in `best_match` falls on a day's edge. */
        private const val SAME_REACH_HOURS = 6
        private const val SAME_MIN_HOURS = 6
        private const val SAME_TEMPERATURE_C = 0.05
        private const val SAME_CLOUD_PCT = 0.5

        /** The metre a rounding may move; the measurement of 14.2 used the same half metre. */
        private const val SAME_VISIBILITY_M = 0.5

        /**
         * The witnesses of [dto] aligned on [best]'s hours by their labels, which share the
         * zone of the same `timezone=auto`. No response, or one whose hours do not match,
         * is no witness at all.
         */
        fun of(dto: WitnessResponseDto?, best: HourlyDto): Witnesses {
            dto ?: return NONE
            val index = dto.time.withIndex().associate { (i, t) -> t to i }
            if (best.time.none { it in index }) return NONE
            fun column(variable: String, model: String): List<Double?>? =
                dto.series(variable, model)?.let { values -> best.time.map { t -> index[t]?.let(values::getOrNull) } }
            val models = OpenMeteoWitnessApi.MODELS.split(',').mapNotNull { model ->
                Series(
                    temperatureC = column("temperature_2m", model) ?: return@mapNotNull null,
                    cloudCoverPct = column("cloud_cover", model) ?: return@mapNotNull null,
                    visibilityM = column("visibility", model).orEmpty(),
                    humidityPct = column("relative_humidity_2m", model).orEmpty()
                )
            }
            return Witnesses(models)
        }
    }
}
