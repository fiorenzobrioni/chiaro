package com.callbackdev.chiaro.data.mapper

import com.callbackdev.chiaro.data.remote.dto.ForecastResponseDto
import com.callbackdev.chiaro.data.remote.dto.WitnessResponseDto
import com.callbackdev.chiaro.domain.correction.RainProbability
import com.callbackdev.chiaro.domain.model.City

/**
 * The calibrated probability of precipitation laid on a response (Fase 18, 22 nov 2026,
 * PLANNING «18.5»), before the mapper reads it, so the strip, the day, the engine's states and
 * the reader's rules all read the same probability.
 *
 * - Only where [RainProbability.zoneOf] has a zone (the Po valley up to 350 m) and only on the
 *   first day of the response, whole: its hours are all within 24 hours of the run, as the
 *   measured day 1 was, and a day is one answer.
 * - An hour with `best_match`'s probability and amount and at least one witness's amount takes
 *   the regression's probability; any other stays the model's.
 * - The day's highest probability is made again from its hours, as the provider makes it.
 * - Every amount stays the model's: the calibration says how likely, never how much.
 */
internal object RainProbabilityCorrection {

    /** The first day's mark in the history (`DailyForecast.correction`). */
    const val LEAD_DAY = RainProbability.LEAD_DAY

    fun apply(forecast: ForecastResponseDto, witnesses: WitnessResponseDto?, city: City): TemperatureCorrection.Result {
        val unchanged = TemperatureCorrection.Result(forecast, emptyMap())
        val zone = RainProbability.zoneOf(city.countryCode, forecast.latitude, forecast.elevation)
            ?: return unchanged
        witnesses ?: return unchanged
        val first = forecast.daily.time.firstOrNull() ?: return unchanged
        val voters = Witnesses.of(witnesses, forecast.hourly)
        val hourly = forecast.hourly
        val probability = hourly.precipitationProbabilityPct.toMutableList()
        var changed = false
        for (j in hourly.time.indices) {
            if (!hourly.time[j].startsWith(first)) continue
            val p = probability.getOrNull(j) ?: continue
            val mm = hourly.precipitationMm.getOrNull(j) ?: continue
            val q = RainProbability.calibrated(zone, p, mm, voters.precipitationAt(hourly, j)) ?: continue
            probability[j] = q
            changed = true
        }
        if (!changed) return unchanged
        val daily = forecast.daily
        val dayMax = daily.precipitationProbabilityMaxPct.toMutableList()
        if (dayMax.isNotEmpty()) {
            val values = hourly.time.indices.filter { hourly.time[it].startsWith(first) }.mapNotNull { probability[it] }
            if (values.isNotEmpty()) dayMax[0] = values.max()
        }
        return TemperatureCorrection.Result(
            forecast = forecast.copy(
                hourly = hourly.copy(precipitationProbabilityPct = probability),
                daily = daily.copy(precipitationProbabilityMaxPct = dayMax)
            ),
            leadDayOfDate = mapOf(first to LEAD_DAY)
        )
    }
}
