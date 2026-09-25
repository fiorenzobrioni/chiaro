package com.callbackdev.chiaro.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ForecastResponseDto(
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    /**
     * The offset every timestamp in this response is expressed on — and it is ONE
     * offset for the whole week, not [timezone]'s rules applied hour by hour.
     *
     * Read since 20 set 2026, and the field that repairs the app's oldest wrong
     * assumption about the provider. With `timezone=auto` Open-Meteo builds the
     * series as `UTC + utc_offset_seconds`, taking the offset in force at REQUEST
     * time and holding it: measured on a 16-day Sydney forecast across the 4 Oct
     * 2026 spring-forward, the response carries `utc_offset_seconds: 36000` and
     * `timezone_abbreviation: "GMT+10"` for every day of it, every calendar day
     * holds exactly 24 values, and `2026-10-04T02:00` — an hour that does not
     * exist in `Australia/Sydney` — is one of them. Matched hour by hour against
     * the same request with `timezone=UTC`: **0 mismatches out of 384** under a
     * fixed `+10`, **53 out of 69** under the real wall clock after the change.
     * The response's own `daily.sunrise` for 4 Oct says 05:28, where the clock in
     * Sydney says 06:28.
     *
     * So a `hourly.time` value is not a local time in [timezone]'s sense, and
     * treating it as one — which the app did until this field arrived — puts every
     * row after a DST change one hour off. [com.callbackdev.chiaro.data.mapper.WeatherReportMapper]
     * re-expresses them; see `ProviderClock` there.
     *
     * Nullable with a default because [com.callbackdev.chiaro.data.local.ReportDiskCache]
     * stores this DTO verbatim: an entry written before today must still decode, and
     * a null means "no offset was recorded", which the mapper answers with the old
     * behaviour rather than with a guess.
     */
    @SerialName("utc_offset_seconds") val utcOffsetSeconds: Int? = null,
    val current: CurrentDto,
    val hourly: HourlyDto,
    val daily: DailyDto
)

@Serializable
data class CurrentDto(
    val time: String,
    @SerialName("temperature_2m") val temperatureC: Double,
    @SerialName("relative_humidity_2m") val humidityPct: Int,
    @SerialName("apparent_temperature") val apparentTemperatureC: Double,
    @SerialName("dew_point_2m") val dewPointC: Double,
    @SerialName("is_day") val isDay: Int,
    @SerialName("precipitation") val precipitationMm: Double,
    @SerialName("weather_code") val weatherCode: Int,
    @SerialName("pressure_msl") val pressureMslHpa: Double,
    @SerialName("wind_speed_10m") val windSpeedKph: Double,
    @SerialName("wind_direction_10m") val windDirectionDeg: Int,
    @SerialName("wind_gusts_10m") val windGustsKph: Double,
    /**
     * Nullable since Fase 26, like [HourlyDto]'s. Never seen absent — probed on twelve
     * places including McMurdo, mid-Pacific, Everest and Svalbard — but `visibility`
     * is a model-dependent field, and a non-nullable one here means a model that stops
     * carrying it fails the WHOLE fetch, current block, forecast and all. The hourly
     * DTO has always tolerated it; there was no reason for the two to disagree.
     */
    @SerialName("visibility") val visibilityM: Double? = null,
    @SerialName("cloud_cover") val cloudCoverPct: Int,
    /**
     * Nullable since 24 set 2026, the last model-dependent field of this block to be
     * found out: `models=icon_seamless` serves `current.uv_index: null` on the live
     * endpoint (Milan, measured that morning), exactly as it serves `uv_index_max`
     * below. Chiaro passes no `models=`, but `best_match` picks per region and the cost
     * of meeting that null non-nullable is the whole report, not one tile.
     */
    @SerialName("uv_index") val uvIndex: Double? = null,
    // The cloud by layer (24 set 2026). Defaulted and nullable like every field added
    // after the first release: a cached response from before must still decode, and a
    // model that does not split its cloud costs the layer, not the report.
    @SerialName("cloud_cover_low") val cloudCoverLowPct: Int? = null,
    @SerialName("cloud_cover_mid") val cloudCoverMidPct: Int? = null,
    @SerialName("cloud_cover_high") val cloudCoverHighPct: Int? = null,
    /**
     * The length of the block's own interval, seconds: 900 on every response measured
     * (26 cities, 25 set 2026), which is what [precipitationMm] and the two below are the
     * sum of. Null in a cache entry from before it was read; the mapper then assumes 900.
     */
    @SerialName("interval") val intervalSeconds: Int? = null,
    // 25 set 2026, for WeatherStateEngine: the snow and the convective part of the
    // interval. Nullable and defaulted like every later field.
    @SerialName("snowfall") val snowfallCm: Double? = null,
    @SerialName("showers") val showersMm: Double? = null
)

@Serializable
data class HourlyDto(
    val time: List<String>,
    @SerialName("temperature_2m") val temperatureC: List<Double>,
    @SerialName("weather_code") val weatherCode: List<Int>,
    /**
     * Millimetres in the hour. Defaulted so a `ReportDiskCache` entry written before
     * Fase 26 still deserializes: an offline phone must not lose its week of forecast
     * to an app update, and the daily code degrades to its hour-count rule when the
     * amounts are not there.
     */
    @SerialName("precipitation") val precipitationMm: List<Double> = emptyList(),
    @SerialName("precipitation_probability") val precipitationProbabilityPct: List<Int?>,
    @SerialName("is_day") val isDay: List<Int>,
    @SerialName("visibility") val visibilityM: List<Double?>,
    @SerialName("cloud_cover") val cloudCoverPct: List<Int>,
    // 24 set 2026, see OpenMeteoForecastApi.HOURLY_VARIABLES. All defaulted to empty (a
    // cache entry from before) and all element-nullable (a model that lacks one):
    // readers go through `getOrNull`, and an absent value is an absent value.
    @SerialName("apparent_temperature") val apparentTemperatureC: List<Double?> = emptyList(),
    @SerialName("relative_humidity_2m") val humidityPct: List<Int?> = emptyList(),
    @SerialName("dew_point_2m") val dewPointC: List<Double?> = emptyList(),
    @SerialName("pressure_msl") val pressureMslHpa: List<Double?> = emptyList(),
    @SerialName("wind_speed_10m") val windSpeedKph: List<Double?> = emptyList(),
    @SerialName("wind_direction_10m") val windDirectionDeg: List<Int?> = emptyList(),
    @SerialName("wind_gusts_10m") val windGustsKph: List<Double?> = emptyList(),
    @SerialName("uv_index") val uvIndex: List<Double?> = emptyList(),
    @SerialName("cloud_cover_low") val cloudCoverLowPct: List<Int?> = emptyList(),
    @SerialName("cloud_cover_mid") val cloudCoverMidPct: List<Int?> = emptyList(),
    @SerialName("cloud_cover_high") val cloudCoverHighPct: List<Int?> = emptyList(),
    // 25 set 2026, for WeatherStateEngine: snowfall (cm) and convective precipitation of
    // ANY phase (mm) in the hour. Defaulted for the cache, element-nullable for the model.
    @SerialName("snowfall") val snowfallCm: List<Double?> = emptyList(),
    @SerialName("showers") val showersMm: List<Double?> = emptyList()
)

@Serializable
data class DailyDto(
    val time: List<String>,
    @SerialName("weather_code") val weatherCode: List<Int>,
    @SerialName("temperature_2m_max") val temperatureMaxC: List<Double>,
    @SerialName("temperature_2m_min") val temperatureMinC: List<Double>,
    @SerialName("precipitation_probability_max") val precipitationProbabilityMaxPct: List<Int?>,
    /**
     * Nullable elements since 20 set 2026, like `precipitation_probability_max` beside
     * it and `visibility` in the hourly block — the third model-dependent field to be
     * found out, and the first to be found out BEFORE it broke something.
     *
     * Verified on the live endpoint: `uv_index_max` comes back `[null, null, null]`
     * under `models=icon_seamless` and `models=jma_seamless`, and for past dates even
     * under `best_match` (Milan, 1-3 July). Chiaro passes neither `models=` nor
     * `past_days`, so it has never met one — but a non-nullable `List<Double>` here
     * does not degrade a tile when it does, it throws inside the deserializer and
     * takes the WHOLE report with it: the week of forecast, the current block, the
     * hours. `best_match` picks its model per region and Open-Meteo changes those
     * picks; the cost of being wrong is the app showing nothing.
     */
    @SerialName("uv_index_max") val uvIndexMax: List<Double?>,
    // 24 set 2026: how much falls, for how long, and the strongest gust. Defaulted for
    // the cache, element-nullable for the model, like `uv_index_max` above.
    @SerialName("precipitation_sum") val precipitationSumMm: List<Double?> = emptyList(),
    @SerialName("precipitation_hours") val precipitationHours: List<Double?> = emptyList(),
    @SerialName("snowfall_sum") val snowfallSumCm: List<Double?> = emptyList(),
    @SerialName("wind_gusts_10m_max") val windGustsMaxKph: List<Double?> = emptyList(),
    // The liquid part of `precipitation_sum`, which also carries the snow's water: see
    // OpenMeteoForecastApi.DAILY_VARIABLES. Defaulted and nullable like the fields above.
    @SerialName("rain_sum") val rainSumMm: List<Double?> = emptyList(),
    @SerialName("showers_sum") val showersSumMm: List<Double?> = emptyList()
)
