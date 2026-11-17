package com.callbackdev.chiaro.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AirQualityResponseDto(
    val current: AirQualityCurrentDto,
    /** Today's pollen, hour by hour; null in an answer from before it was asked for. */
    val hourly: AirQualityHourlyDto? = null
)

// Everything nullable: coverage varies by region (pollen is Europe-only) and the
// report must degrade gracefully instead of failing to parse.
@Serializable
data class AirQualityCurrentDto(
    val time: String,
    @SerialName("us_aqi") val usAqi: Int? = null,
    /** The EEA's index (bands revised 2024), served for every place since 24 set 2026. */
    @SerialName("european_aqi") val europeanAqi: Int? = null
)

/**
 * The pollens of the place's day, grains/m³ by the hour (17 nov 2026). Every column
 * defaulted and element-nullable: coverage is Europe's alone, and outside it the columns
 * come null or not at all.
 */
@Serializable
data class AirQualityHourlyDto(
    val time: List<String> = emptyList(),
    @SerialName("grass_pollen") val grassPollen: List<Double?> = emptyList(),
    @SerialName("birch_pollen") val birchPollen: List<Double?> = emptyList(),
    @SerialName("alder_pollen") val alderPollen: List<Double?> = emptyList(),
    @SerialName("olive_pollen") val olivePollen: List<Double?> = emptyList(),
    @SerialName("ragweed_pollen") val ragweedPollen: List<Double?> = emptyList(),
    @SerialName("mugwort_pollen") val mugwortPollen: List<Double?> = emptyList()
)
