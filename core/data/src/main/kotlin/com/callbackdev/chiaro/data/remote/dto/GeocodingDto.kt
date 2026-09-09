package com.callbackdev.chiaro.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeocodingResponseDto(
    val results: List<GeoResultDto> = emptyList()
)

@Serializable
data class GeoResultDto(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    @SerialName("country_code") val countryCode: String? = null,
    val admin1: String? = null,
    /** The municipality ("Comune di Segrate"; "Roma" and "Genova" come without the
     * prefix), read since 9 set 2026 for the warning-zone index's fallback. */
    val admin3: String? = null,
    val timezone: String? = null
)
