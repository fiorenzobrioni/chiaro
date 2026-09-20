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
    @SerialName("uv_index") val uvIndex: Double
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
    @SerialName("cloud_cover") val cloudCoverPct: List<Int>
)

@Serializable
data class DailyDto(
    val time: List<String>,
    @SerialName("weather_code") val weatherCode: List<Int>,
    @SerialName("temperature_2m_max") val temperatureMaxC: List<Double>,
    @SerialName("temperature_2m_min") val temperatureMinC: List<Double>,
    val sunrise: List<String>,
    val sunset: List<String>,
    @SerialName("daylight_duration") val daylightDurationSec: List<Double>,
    @SerialName("precipitation_probability_max") val precipitationProbabilityMaxPct: List<Int?>,
    @SerialName("uv_index_max") val uvIndexMax: List<Double>
)
