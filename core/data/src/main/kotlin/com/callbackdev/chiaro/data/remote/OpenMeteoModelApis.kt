package com.callbackdev.chiaro.data.remote

import com.callbackdev.chiaro.data.remote.dto.ForecastResponseDto
import com.callbackdev.chiaro.data.remote.dto.ModelCodesResponseDto
import com.callbackdev.chiaro.data.remote.dto.UvResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * The new forecast model (prova del nuovo modello, 21 nov 2026, branch `prova-nuovo-modello`):
 * the national services' km-scale models where they cover, ECMWF after them.
 *
 * - **Northern Italy and the Alps**: MeteoSwiss ICON-CH (`meteoswiss_icon_seamless`: CH1 at
 *   1 km for 33 hours, then CH2 at 2 km to 120 hours), then ECMWF IFS at 9 km.
 * - **The rest of Italy**: ItaliaMeteo ICON-2I (2.2 km, 72 hours), then ECMWF IFS.
 * - **Elsewhere**: `best_match`, as before ([OpenMeteoForecastApi]).
 *
 * The same variable lists as [OpenMeteoForecastApi], so a model's answer has the shape the
 * whole app reads; a model's null is a null (the DTO is nullable all the way). Outside a
 * model's domain Open-Meteo answers HTTP 400 «No data is available for this location».
 *
 * Its own interface, not a `models=` parameter on [OpenMeteoForecastApi.forecast]: the
 * current path stays byte for byte what it was, and the switch in Settings goes back to it.
 */
interface OpenMeteoModelForecastApi {
    @GET("v1/forecast")
    suspend fun forecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("models") models: String,
        @Query("current") current: String = OpenMeteoForecastApi.CURRENT_VARIABLES,
        @Query("hourly") hourly: String = OpenMeteoForecastApi.HOURLY_VARIABLES,
        @Query("daily") daily: String = OpenMeteoForecastApi.DAILY_VARIABLES,
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = OpenMeteoForecastApi.FORECAST_DAYS,
        @Query("minutely_15") minutely15: String = OpenMeteoForecastApi.MINUTELY_15_VARIABLES,
        @Query("forecast_minutely_15") forecastQuarters: Int = OpenMeteoForecastApi.FORECAST_QUARTERS,
        @Query("past_minutely_15") pastQuarters: Int = OpenMeteoForecastApi.PAST_QUARTERS
    ): ForecastResponseDto

    /** ICON-D2's code by the hour, for the storm and the freezing rain ([HAZARD_MODEL]). */
    @GET("v1/forecast")
    suspend fun codes(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("models") models: String = HAZARD_MODEL,
        @Query("hourly") hourly: String = "weather_code",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = HAZARD_DAYS
    ): ModelCodesResponseDto

    companion object {
        /** MeteoSwiss: ICON-CH1 (1 km, 33 h) then ICON-CH2 (2 km, 120 h), one series. */
        const val METEOSWISS = "meteoswiss_icon_seamless"

        /** ItaliaMeteo-ARPAE ICON-2I: 2.2 km over Italy, 72 hours, two runs a day. */
        const val ITALIA_METEO = "italia_meteo_arpae_icon_2i"

        /** ECMWF IFS HRES at 9 km: the days the regional model does not reach. */
        const val ECMWF = "ecmwf_ifs"

        /** The regional models in the order they are tried: the first that covers wins. */
        val REGIONAL = listOf(METEOSWISS, ITALIA_METEO)

        /**
         * How `composedFrom` names the new model's road outside both regional domains: Open-Meteo's
         * own choice, by design of the new model, and said as such in the footer.
         */
        const val BEST_MATCH = "best_match"

        /** DWD ICON-D2 (2 km, 48 h): the only model here with a thunderstorm in its code. */
        const val HAZARD_MODEL = "icon_d2"
        const val HAZARD_DAYS = 3

        /**
         * The new path's part of the disk cache's request (`ReportDiskCache.requestFor`): an
         * entry fetched by the other path is never a fresh hit of this one.
         */
        val REQUEST: String get() =
            "new:${REGIONAL.joinToString(",")}+$ECMWF|codes:$HAZARD_MODEL:$HAZARD_DAYS|" +
                "uv:${OpenMeteoUvApi.HOURLY}:${OpenMeteoUvApi.DAYS}"
    }
}

/**
 * The UV index of Copernicus (CAMS), on the air-quality host (prova del nuovo modello): with
 * the clouds, the ozone and the aerosols. A separate call from [OpenMeteoAirQualityApi]
 * because that one asks for one day (today's pollen) and the UV is wanted for the week.
 * One variable over [DAYS] days: **1 call**.
 */
interface OpenMeteoUvApi {
    @GET("v1/air-quality")
    suspend fun uv(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("hourly") hourly: String = HOURLY,
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = DAYS
    ): UvResponseDto

    companion object {
        const val HOURLY = "uv_index"
        const val DAYS = 7
    }
}
