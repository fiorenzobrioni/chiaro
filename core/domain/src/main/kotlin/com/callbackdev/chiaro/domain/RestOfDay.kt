package com.callbackdev.chiaro.domain

import com.callbackdev.chiaro.domain.model.DailyForecast
import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.model.WeatherCondition
import kotlin.math.roundToInt

/**
 * Today's row as the rest of the day (committente, 23 nov 2026, `docs/STATES.md` §6): what
 * the row says — its icon and word, its chance, and the facts it opens on — is read from the
 * hours still ahead, by the day's own rules ([DayLabel]); its high and low stay the whole
 * day's, because its bar wears «now» on that range.
 *
 * Why: Paris, 23 nov 2026 at 09:30. It rained from midnight to six, 1.2 mm, and from there the
 * day opened into sun. The row said «Pioggia» over a ribbon of sun drawn from now on: an icon
 * about hours nobody can still go out in, under a band about the ones they can. Apple's
 * forecast data carry the same split (a day's forecast and the rest of today's, WeatherKit's
 * `restOfDayForecast`), and the same complaint is made of its app's daily icon: rain at
 * night, sun all day, a rain icon.
 *
 * Read when the page is looked at, never when the forecast is fetched: [hours] are the
 * report's hours as `WeatherNow` prepared them, the first being the one now is in. The day in
 * the report stays the provider's whole day, which is what «What changed», the reader's rules
 * and the summaries compare and read.
 */
object RestOfDay {

    /**
     * [day] as its remaining [hours] say it (in order, the day's own, the first the one now
     * is in), [daylight] telling which of them have the sun up. [day] itself when no hour is
     * left to read: a row is never emptied of what the provider said.
     */
    fun of(day: DailyForecast, hours: List<HourlyForecast>, daylight: (HourlyForecast) -> Boolean): DailyForecast {
        if (hours.isEmpty()) return day
        val code = DayLabel.of(
            hours.map {
                DayLabel.Hour(
                    code = it.condition.wmoCode,
                    precipitationMm = it.precipMm,
                    chancePct = it.precipChancePct,
                    daylight = daylight(it),
                    cloudCoverPct = it.cloudCoverPct,
                    layers = it.cloudLayers
                )
            },
            fallback = day.condition.wmoCode
        ) ?: day.condition.wmoCode
        val wet = hours.filter { WmoCode.isPrecipitation(it.condition.wmoCode) }
        val amounts = hours.mapNotNull { it.precipMm }
        val snow = hours.mapNotNull { it.snowCm }
        // The rain alone, as the day's own line prints it: each hour's total less its snow's
        // water. An hour that has an amount and no snow number cannot be split, and the rain is
        // then not said rather than guessed.
        val unsplit = hours.any { (it.precipMm ?: 0.0) > 0.0 && it.snowCm == null }
        val rain = if (amounts.isEmpty() || unsplit) null else hours.sumOf { hour ->
            val mm = hour.precipMm ?: 0.0
            (mm - (hour.snowCm ?: 0.0) / WeatherStateEngine.SNOW_CM_PER_MM).coerceAtLeast(0.0)
        }
        return day.copy(
            condition = WeatherCondition(code),
            precipPct = hours.mapNotNull { it.precipChancePct }.maxOrNull(),
            uvIndexMax = hours.mapNotNull { it.uvIndex }.maxOrNull()?.roundToInt(),
            precipMm = amounts.takeIf { it.isNotEmpty() }?.sum(),
            // Over how many hours the amount falls, and only on a rest of the day the strip
            // draws rain in (the mapper's own rule for the whole day).
            precipHours = if (wet.isEmpty()) null else hours.count { (it.precipMm ?: 0.0) > 0.0 },
            snowCm = snow.takeIf { it.isNotEmpty() }?.sum(),
            gustMaxKph = hours.mapNotNull { it.gustKph }.maxOrNull(),
            rainMm = rain,
            // The lowest line of the wet hours, and only under a label that says something falls.
            snowLine = if (WmoCode.of(code)?.isPrecipitation != true) null else SnowLine.lowest(
                wet.mapNotNull { SnowLine.of(it.snowfallHeightM, it.freezingLevelM) }
            )
        )
    }
}
