package com.callbackdev.chiaro.data.mapper

import com.callbackdev.chiaro.data.remote.dto.ForecastResponseDto
import com.callbackdev.chiaro.data.remote.dto.WitnessResponseDto
import com.callbackdev.chiaro.domain.correction.TemperatureBlend
import com.callbackdev.chiaro.domain.model.City

/**
 * The blend of days 6 and 7 laid on a response (Fase 18, 22 nov 2026, PLANNING «18.4»), before
 * the mapper reads it: what the corrected forecast is, at the level the whole mapper already
 * reads, so the states, the days and the alerts all see the same hours.
 *
 * - Only where [TemperatureBlend.zoneOf] has a zone (Italy, up to 350 m) and only on the sixth
 *   and seventh day of the response, whole.
 * - Each hour's temperature is the blend of `best_match`, ECMWF IFS and the Met Office's model
 *   from the witnesses' response the app already downloads; a witness that is `best_match`
 *   around the hour is left out ([Witnesses.temperatureOf]), a missing one renormalises the
 *   others, and with none the hour stays the model's.
 * - The humidity stays the model's and the dew point follows the blended air; the feels-like
 *   moves by the same difference; the day's high and low are made again from its hours. Every
 *   other field, the amounts first, is the model's.
 */
internal object TemperatureCorrection {

    const val IFS = "ecmwf_ifs"
    const val UKMO = "ukmo_global_deterministic_10km"

    /** The corrected response, and the lead day of the table each corrected date used. */
    data class Result(val forecast: ForecastResponseDto, val leadDayOfDate: Map<String, Int>)

    fun apply(forecast: ForecastResponseDto, witnesses: WitnessResponseDto?, city: City): Result {
        val unchanged = Result(forecast, emptyMap())
        val zone = TemperatureBlend.zoneOf(city.countryCode, forecast.latitude, forecast.elevation)
            ?: return unchanged
        witnesses ?: return unchanged
        val voters = Witnesses.of(witnesses, forecast.hourly)
        val hourly = forecast.hourly
        val dates = forecast.daily.time
        val leadOfDate = TemperatureBlend.LEAD_DAY_OF_INDEX.mapNotNull { (index, lead) ->
            dates.getOrNull(index)?.let { it to lead }
        }.toMap()

        val temperature = hourly.temperatureC.toMutableList()
        val dewPoint = hourly.dewPointC.toMutableList()
        val feelsLike = hourly.apparentTemperatureC.toMutableList()
        val blendedDates = mutableSetOf<String>()
        for (i in hourly.time.indices) {
            val date = hourly.time[i].take(DATE_LENGTH)
            val lead = leadOfDate[date] ?: continue
            val weights = TemperatureBlend.weights(zone, lead) ?: continue
            val best = hourly.temperatureC.getOrNull(i) ?: continue
            val ifs = voters.temperatureOf(IFS, hourly, i)
            val ukmo = voters.temperatureOf(UKMO, hourly, i)
            if (ifs == null && ukmo == null) continue
            val blended = TemperatureBlend.blend(weights, best, ifs, ukmo)
            val delta = blended - best
            temperature[i] = blended
            val rh = hourly.humidityPct.getOrNull(i)
            if (i < dewPoint.size) {
                dewPoint[i] = rh?.let { TemperatureBlend.dewPointC(blended, it.toDouble()) }
                    ?: dewPoint[i]?.plus(delta)
            }
            if (i < feelsLike.size) feelsLike[i] = feelsLike[i]?.plus(delta)
            blendedDates += date
        }
        if (blendedDates.isEmpty()) return unchanged

        // The day's high and low from its hours, where any of them was blended: a day's maximum
        // is the maximum of its hours, and a high left the model's over blended hours would be
        // the row contradicting its own strip.
        val daily = forecast.daily
        val high = daily.temperatureMaxC.toMutableList()
        val low = daily.temperatureMinC.toMutableList()
        for ((d, date) in dates.withIndex()) {
            if (date !in blendedDates) continue
            val values = hourly.time.indices.filter { hourly.time[it].take(DATE_LENGTH) == date }
                .mapNotNull { temperature[it] }
            if (values.isEmpty()) continue
            if (d < high.size) high[d] = values.max()
            if (d < low.size) low[d] = values.min()
        }
        return Result(
            forecast = forecast.copy(
                hourly = hourly.copy(temperatureC = temperature, dewPointC = dewPoint, apparentTemperatureC = feelsLike),
                daily = daily.copy(temperatureMaxC = high, temperatureMinC = low)
            ),
            leadDayOfDate = leadOfDate.filterKeys { it in blendedDates }
        )
    }

    private const val DATE_LENGTH = 10
}
