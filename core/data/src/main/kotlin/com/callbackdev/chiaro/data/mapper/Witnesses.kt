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
 * `best_match`'s own visibility is not read since 17 nov 2026 (`WeatherStateEngine.fogVotes`):
 * its model votes on fog with its humidity and wind, the witnesses with their visibility. So
 * the copy Open-Meteo makes of a variable the chosen model lacks — IFS's visibility under MET
 * Nordic's temperature at Oslo, under ICON's in Italy from 30 Nov 2026 — can no longer count
 * twice, and needs no test of its own (it had one, per variable, from 27 Oct 2026).
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

        /**
         * The witnesses of [dto] aligned on [best]'s hours by their labels, which share the
         * zone of the same `timezone=auto`. No response, or one whose hours do not match,
         * is no witness at all.
         */
        fun of(
            dto: WitnessResponseDto?,
            best: HourlyDto,
            /**
             * A witness that IS the hour's model from a label on (prova del nuovo modello, 21 nov
             * 2026): ECMWF from the first day `ForecastComposer` gives it. The copy test above
             * cannot see it in the six hours after the seam, half of whose window is still the
             * regional model's, and ECMWF would vote twice — its visibility as a witness, its
             * humidity as the hour's model. So it is no witness from that label on.
             */
            ownFrom: Pair<String, String>? = null
        ): Witnesses {
            dto ?: return NONE
            val index = dto.time.withIndex().associate { (i, t) -> t to i }
            if (best.time.none { it in index }) return NONE
            fun column(variable: String, model: String): List<Double?>? =
                dto.series(variable, model)?.let { values ->
                    best.time.map { t ->
                        if (ownFrom != null && model == ownFrom.first && t >= ownFrom.second) null
                        else index[t]?.let(values::getOrNull)
                    }
                }
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
