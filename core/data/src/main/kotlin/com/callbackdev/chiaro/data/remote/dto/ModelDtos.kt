package com.callbackdev.chiaro.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * ICON-D2's own weather code by the hour (prova del nuovo modello, 21 nov 2026): the only
 * model on Open-Meteo over the Alps with a thunderstorm in it, read for the two hazards the
 * engine takes from the provider's code (storm, freezing rain). Nothing else of it is read.
 */
@Serializable
data class ModelCodesResponseDto(
    @SerialName("utc_offset_seconds") val utcOffsetSeconds: Int? = null,
    val hourly: ModelCodesHourlyDto
)

@Serializable
data class ModelCodesHourlyDto(
    val time: List<String>,
    @SerialName("weather_code") val weatherCode: List<Int?> = emptyList()
)

/**
 * The UV index of Copernicus (CAMS) by the hour, from Open-Meteo's air-quality service
 * (prova del nuovo modello, 21 nov 2026): computed with the clouds, the ozone and the
 * aerosols, about five days ahead. Neither ICON-CH nor ECMWF IFS serve a UV index there.
 */
@Serializable
data class UvResponseDto(
    @SerialName("utc_offset_seconds") val utcOffsetSeconds: Int? = null,
    val hourly: UvHourlyDto
)

@Serializable
data class UvHourlyDto(
    val time: List<String>,
    @SerialName("uv_index") val uvIndex: List<Double?> = emptyList()
)
