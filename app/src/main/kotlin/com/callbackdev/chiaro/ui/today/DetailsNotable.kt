package com.callbackdev.chiaro.ui.today

import com.callbackdev.chiaro.domain.model.AirQuality
import com.callbackdev.chiaro.domain.model.AqiScale
import com.callbackdev.chiaro.domain.model.PollenLevel

/** The details grid's quantities, for which of them is today's one to notice. */
internal enum class DetailKind { RAIN, SNOW, UV, WIND, CLOUD, HUMIDITY, PRESSURE, VISIBILITY, AIR, POLLEN }

/**
 * The one tile of the details grid that says something today (DESIGN §8.6, second review of
 * Today, 15 ott 2026), or null on a day none does — most days, which is the point: a grid
 * where something is always highlighted has nothing highlighted.
 *
 * Each threshold is where the tile's own meaning line turns from "nothing to do" to "do
 * something", so the emphasis never disagrees with the words under it:
 *
 * - snow from 1 cm (`snowBand` 1, «a dusting» and up);
 * - rain from 10 mm (`rainBand` 3, the WMO's heavy precipitation day: «a wet day»);
 * - wind from 39 km/h, steady or in the gusts the tile shows (`windMeaning`'s «strong»), or a
 *   day's gust from 62 (its «gale»);
 * - air from «unhealthy for the sensitive» (US above 100, EU above 60, `airMeaning`);
 * - UV from 6 now (`uvMeaning`'s «high»: it burns in under half an hour);
 * - pollen from «high» (`pollenMeaning`);
 * - visibility under 1 km (`visibilityMeaning`'s «fog»);
 * - the dew point from 21° (`dewPointMeaning`'s «muggy»).
 *
 * When two cross, the first in that order wins: first what changes the day for everyone,
 * then what a body has to mind, then comfort. Clouds and pressure are never the one: an
 * overcast sky and a falling barometer are already the sentence's and the hero's to say.
 * The grid is never reordered for it — a tile that moved would cost the reader where to look.
 */
internal fun notableDetail(
    rainMm: Double?,
    snowCm: Double?,
    windShownKph: Double,
    dayGustKph: Double?,
    air: AirQuality?,
    uvNow: Int?,
    pollen: PollenLevel?,
    visibilityKm: Double?,
    dewPointC: Double
): DetailKind? = when {
    snowCm != null && WeatherText.snowBand(snowCm) >= 1 -> DetailKind.SNOW
    rainMm != null && WeatherText.rainBand(rainMm) >= 3 -> DetailKind.RAIN
    windShownKph >= StrongWindKph || (dayGustKph ?: 0.0) >= GaleKph -> DetailKind.WIND
    air != null && airIsPoor(air) -> DetailKind.AIR
    uvNow != null && uvNow >= HighUv -> DetailKind.UV
    pollen != null && pollen.ordinal >= PollenLevel.HIGH.ordinal -> DetailKind.POLLEN
    visibilityKm != null && visibilityKm < FogKm -> DetailKind.VISIBILITY
    dewPointC >= MuggyDewC -> DetailKind.HUMIDITY
    else -> null
}

private fun airIsPoor(air: AirQuality): Boolean =
    if (air.scale == AqiScale.EUROPEAN) air.shownIndex > 60 else air.shownIndex > 100

private const val StrongWindKph = 39.0
private const val GaleKph = 62.0
private const val HighUv = 6
private const val FogKm = 1.0
private const val MuggyDewC = 21.0
