package com.callbackdev.chiaro.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * What Open-Meteo puts in the body of an error status (read since 19 ott 2026):
 * `{"error":true,"reason":"The service is overloaded"}`. The reason is the one thing
 * that tells its answers apart — a limit on the minute, the hour or the day, the queue
 * of one address, a node shedding load — since none of them carries a header that does
 * (no `Retry-After` anywhere in the service's code).
 */
@Serializable
data class ApiErrorDto(
    val error: Boolean = false,
    val reason: String? = null
)
