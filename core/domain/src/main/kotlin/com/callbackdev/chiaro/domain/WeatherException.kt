package com.callbackdev.chiaro.domain

/**
 * Data-layer failures, each carrying a terminal-style message ready for the editor
 * UI (rendered later as `// ERROR: ...` comment lines).
 */
sealed class WeatherException(
    val terminalMessage: String,
    cause: Throwable? = null
) : Exception(terminalMessage, cause) {

    class NoNetwork(cause: Throwable? = null) : WeatherException(
        "net::ERR_INTERNET_DISCONNECTED — check your connection", cause
    )

    /**
     * The phone is online and the service did not answer: a connection refused or
     * reset, a TLS handshake that stalled, a timeout (17 ott 2026). It used to be
     * [NoNetwork], and Open-Meteo dropping one handshake in two told a reader on a
     * working Wi-Fi that they were offline.
     */
    class Unreachable(cause: Throwable? = null) : WeatherException(
        "net::ERR_CONNECTION_FAILED — Open-Meteo did not answer", cause
    )

    class CityNotFound(query: String) : WeatherException(
        "404: location \"$query\" not found in geocoding index"
    )

    /**
     * The service answered with an error status. [reason] is Open-Meteo's own sentence
     * from the body (`{"error":true,"reason":"…"}`, read since 19 ott 2026), null when
     * the body was empty or not that JSON: it is the one thing that tells a limit on
     * the minute from a queue on the address from a node shedding load, since none of
     * them carries a header that does.
     */
    class ApiError(
        val code: Int,
        cause: Throwable? = null,
        val reason: String? = null
    ) : WeatherException(
        "http::$code — ${reason ?: "Open-Meteo request failed"}", cause
    ) {
        /**
         * The service's own passing state, worth another try LATER (19 ott 2026): a
         * 5xx — Open-Meteo sheds load with a 503 «The service is overloaded» at the peak
         * of every full hour — or a 429, whose limit a later try is past. Not the
         * request's fault: any other 4xx will fail the same way again.
         */
        val isTransient: Boolean get() = code in 500..599 || code == 429
    }

    class Unknown(cause: Throwable) : WeatherException(
        "panic: unexpected error — ${cause.message ?: cause::class.simpleName}", cause
    )

    class LocationPermissionDenied : WeatherException(
        "gps::PERMISSION_DENIED — grant location access"
    )

    class LocationDisabled : WeatherException(
        "gps::PROVIDER_DISABLED — turn on location services"
    )

    class LocationTimeout : WeatherException(
        "gps::TIMEOUT — no position fix acquired"
    )

    class LocationUnavailable(cause: Throwable? = null) : WeatherException(
        "gps::NO_FIX — position unavailable", cause
    )
}
