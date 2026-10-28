package com.callbackdev.chiaro.data.remote

import com.callbackdev.chiaro.data.remote.dto.AirQualityResponseDto
import com.callbackdev.chiaro.data.remote.dto.ForecastResponseDto
import com.callbackdev.chiaro.data.remote.dto.GeocodingResponseDto
import com.callbackdev.chiaro.data.remote.dto.WitnessResponseDto
import java.util.Locale
import retrofit2.http.GET
import retrofit2.http.Query

// Open-Meteo (https://open-meteo.com): free, no API key. The three services live on
// separate hosts, hence three Retrofit instances sharing one OkHttp client.

interface OpenMeteoForecastApi {
    @GET("v1/forecast")
    suspend fun forecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = CURRENT_VARIABLES,
        @Query("hourly") hourly: String = HOURLY_VARIABLES,
        @Query("daily") daily: String = DAILY_VARIABLES,
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = FORECAST_DAYS,
        @Query("minutely_15") minutely15: String = MINUTELY_15_VARIABLES,
        @Query("forecast_minutely_15") forecastQuarters: Int = FORECAST_QUARTERS
    ): ForecastResponseDto

    companion object {
        const val BASE_URL = "https://api.open-meteo.com/"

        /**
         * Days of forecast requested, and therefore `× 24` hourly values returned.
         * A constant since Fase 16a because the mapper's hourly window is derived
         * from it: the two used to be a 7 here and a 25 there, which is how the app
         * ended up parsing a week of hours and keeping one day of them.
         */
        const val FORECAST_DAYS = 7

        /**
         * What this request costs of Open-Meteo's free calls (CLAUDE.md, «Free and without a
         * key is a constraint», 27 Oct 2026): 54 variables over 7 days weigh **5.4 calls**
         * (one call per ten variables, one per two weeks; `tools/probe_provider.py` prints
         * the sum; 5.2 with 52 variables until 14.8 added the snow line's two, 28 Oct 2026).
         * A variable added here is a tenth of a call more on every fetch.
         */
        const val CURRENT_VARIABLES =
            "temperature_2m,relative_humidity_2m,apparent_temperature,dew_point_2m," +
                "is_day,precipitation,weather_code,pressure_msl,wind_speed_10m," +
                "wind_direction_10m,wind_gusts_10m,visibility,cloud_cover,uv_index," +
                "cloud_cover_low,cloud_cover_mid,cloud_cover_high," +
                // 25 set 2026: the phase and the convective part, for WeatherStateEngine.
                "snowfall,showers"
        // visibility + cloud_cover are never displayed: they repair `weather_code`,
        // whose fog is unreliable in both directions (Fase 13c) — see
        // WeatherReportMapper. Hourly cloud_cover has a second reader since Fase 16a:
        // it is carried into the domain for the sky module's verdicts. Still not a
        // rendered field, and still fetched for the same price as before.
        // `precipitation` joined the hourly list in Fase 26: WeatherReportMapper's
        // daily code needs to know whether a day's precipitation is MATERIAL before
        // letting it label the day, and millimetres are the quantity that answers
        // that. Measured at **+72 bytes gzipped** on a 7-day response — 168 mostly-zero
        // values compress to almost nothing, which is why the amount was worth asking
        // for rather than inferring from the probability the app already had.
        //
        // 24 set 2026, the second pass of the engine review: the rest of what a "now" is
        // made of (apparent temperature, humidity, dew point, pressure, wind, gusts, UV),
        // so that a `current` block gone stale can be replaced by the forecast for this
        // hour instead of shown hours late, and the headline can see a gale coming; and
        // the cloud by layer, which the details and the sky's verdicts now say. Every
        // one of them has a reader — see `PLANNING.md`, «I dati nuovi a schermo».
        const val HOURLY_VARIABLES =
            "temperature_2m,weather_code,precipitation,precipitation_probability," +
                "is_day,visibility,cloud_cover," +
                "apparent_temperature,relative_humidity_2m,dew_point_2m,pressure_msl," +
                "wind_speed_10m,wind_direction_10m,wind_gusts_10m,uv_index," +
                "cloud_cover_low,cloud_cover_mid,cloud_cover_high," +
                // 25 set 2026: the snow and the convective part of each hour, which
                // WeatherStateEngine reads to decide what falls. `rain` is not asked for:
                // its meaning changes with the model (the UK's includes the showers), and
                // the liquid is the total minus the snow's water.
                "snowfall,showers," +
                // 28 Oct 2026 (Fase 14.8): the snow line of the hour, the model's own where it
                // has one and the 0 °C isotherm less 300 m otherwise (`SnowLine`). Two
                // variables, **+0.2 calls** a fetch: 5.4 for this request.
                "freezing_level_height,snowfall_height"
        /**
         * `sunrise`, `sunset` and `daylight_duration` left this list on 20 set 2026,
         * having been asked for and dropped on the floor since Fase 16e handed the
         * astronomy to [com.callbackdev.chiaro.domain.sky.AstronomyEngine]. They were
         * kept because "they cost nothing", which was true of the bytes and false of
         * everything else: they are written into every `ReportDiskCache` entry, they
         * are three more non-nullable fields a response has to satisfy for the whole
         * report to parse, and — since the provider writes them on the fixed offset
         * `utc_offset_seconds` names — a contributor who found `daily.sunrise` sitting
         * in the DTO and used it would reintroduce the hour that fix removed. The
         * response's own sunrise for Sydney on 4 Oct 2026 says 05:28 where the clock
         * there says 06:28.
         */
        /**
         * What a response was asked for, written beside it in `ReportDiskCache` (25 set
         * 2026). An entry fetched by an older app with a shorter list re-mapped as a
         * fresh HIT for up to 15 minutes after an update: Longyearbyen showed no rain on
         * a day of 3.8 mm because the response on disk did not have `rain_sum` yet.
         */
        val REQUEST: String get() =
            "$CURRENT_VARIABLES|$HOURLY_VARIABLES|$DAILY_VARIABLES|$MINUTELY_15_VARIABLES:$FORECAST_QUARTERS"

        /**
         * The quarters of the next hours (17 ott 2026), for Today's «Pioggia nelle prossime
         * due ore»: the amount and the snow, the two the state of a quarter is read from.
         * Asked everywhere, read only where `NowcastCoverage` says a model computes the
         * quarter: elsewhere the series is the hourly one cut in four, and the mapper drops
         * it. Twenty-four quarters, six hours: the section shows two, and the other four are
         * what `NowcastCoverage.looksInterpolated` reads the series' shape from.
         */
        const val MINUTELY_15_VARIABLES = "precipitation,snowfall"
        const val FORECAST_QUARTERS = 24

        const val DAILY_VARIABLES =
            "weather_code,temperature_2m_max,temperature_2m_min," +
                "precipitation_probability_max,uv_index_max," +
                // 24 set 2026: how much, not only how likely — the day's rain and snow,
                // the hours it lasts, and the strongest gust.
                "precipitation_sum,precipitation_hours,snowfall_sum,wind_gusts_10m_max," +
                // Same day, later: `precipitation_sum` is rain, showers AND the snow's
                // water together (Everest's 19.81 cm of snow arrive as 28.4 mm of it), so
                // it cannot be printed as «di pioggia». The liquid part is asked for by name.
                "rain_sum,showers_sum"
    }
}

interface OpenMeteoAirQualityApi {
    @GET("v1/air-quality")
    suspend fun current(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = CURRENT_VARIABLES,
        @Query("timezone") timezone: String = "auto"
    ): AirQualityResponseDto

    companion object {
        const val BASE_URL = "https://air-quality-api.open-meteo.com/"

        /** 14 variables of the current hour: **1.4 calls** a fetch (CLAUDE.md, the free tier's
         * arithmetic). */
        const val CURRENT_VARIABLES =
            "us_aqi,european_aqi,pm2_5,pm10,ozone,nitrogen_dioxide,sulphur_dioxide,carbon_monoxide," +
                "grass_pollen,birch_pollen,alder_pollen,olive_pollen,ragweed_pollen," +
                "mugwort_pollen"
    }
}

interface OpenMeteoGeocodingApi {
    /**
     * [language] is not a display setting: it also picks the index the query is matched
     * against, so it decides what the user can even find. Hardcoded to `en` until Fase
     * 13f, which is why an Italian phone had to spell its own cities in English —
     * "Firenze" returned only the hamlet Firenze Nova and "Napoli" five places that are
     * not Naples. It is passed per call by `WeatherRepository.searchCities` and has no
     * default here on purpose: a caller cannot fall back to English by distraction.
     */
    @GET("v1/search")
    suspend fun search(
        @Query("name") name: String,
        @Query("language") language: String,
        @Query("count") count: Int = 10,
        @Query("format") format: String = "json"
    ): GeocodingResponseDto

    companion object {
        const val BASE_URL = "https://geocoding-api.open-meteo.com/"

        /** What a locale with no language of its own searches in. */
        const val DEFAULT_LANGUAGE = "en"

        /**
         * The ISO code [search] wants for [locale]. Handed over as it is: Open-Meteo
         * supports nine languages and falls back to English server-side for anything
         * else, so filtering here would only date the app the day it supports a tenth.
         */
        fun languageOf(locale: Locale): String =
            locale.language.lowercase(Locale.ROOT).ifBlank { DEFAULT_LANGUAGE }
    }
}

/**
 * The witnesses (26 ott 2026, `PLANNING.md`, «Il motore dopo la revisione»): the same
 * hours from other model families, which vote on what the state engine cannot read from
 * one model alone: fog, the state whose input, the visibility, is the weakest number a model
 * forecasts. Their numbers never reach a
 * screen: what is printed stays `best_match`'s.
 *
 * Its own request, not more `models=` on the main one: Open-Meteo suffixes EVERY variable
 * of a multi-model response with its model's name, which would change the shape of the
 * report the whole app is built on. Best-effort, like the air: a failure costs the vote,
 * and the engine then reads `best_match` alone, as it did before.
 */
interface OpenMeteoWitnessApi {
    @GET("v1/forecast")
    suspend fun witnesses(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("hourly") hourly: String = HOURLY_VARIABLES,
        @Query("models") models: String = MODELS,
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = OpenMeteoForecastApi.FORECAST_DAYS
    ): WitnessResponseDto

    companion object {
        /**
         * One model per family that `best_match` does not always use: ECMWF's IFS at 9 km
         * (every variable the engine reads), the Met Office's global 10 km, and the two
         * HARMONIE-AROME Europe of KNMI and DMI at 5.5 km, which carry a visibility over
         * Europe where ICON's goes on 30 Nov 2026 (measured in 14.2, `docs/STATES.md` §11:
         * the four lend the fog rule the same score it has with the hour's own visibility;
         * outside their domain — Palermo, Sydney — the response simply has no column of
         * theirs, verified live on 27 Oct 2026, and inside it their series runs three days).
         * NOAA's GFS left the same day: its visibility is right in 5% of the fogs seen (CSI
         * 0.03) and adding it to the four costs two points. Not ICON-EU: in Italy
         * `best_match` IS the ICON family all week (measured on 15 cities, 26 ott 2026), and
         * ICON-D2 runs inside ICON-EU. Where `best_match` is one of these four (ECMWF over
         * Sydney, the Met Office over London, DMI over Keflavik) the mapper recognises it
         * from the numbers and leaves it out; where only one variable is theirs (IFS's
         * visibility under MET Nordic's temperature at Oslo) it leaves that variable out.
         * A change here changes [REQUEST], and the disk cache knows the entries fetched
         * with the old list are not fresh hits (`ReportDiskCache.CURRENT_REQUEST`).
         */
        const val MODELS =
            "ecmwf_ifs,ukmo_global_deterministic_10km,knmi_harmonie_arome_europe,dmi_harmonie_arome_europe"

        /**
         * What the vote reads — visibility and humidity, for fog — and what tells a witness
         * that is `best_match` itself: the temperature and the cloud. Not the amounts: the
         * phase was measured for a vote too, and `best_match` alone called 94.3% of a
         * winter's precipitation hours right against 94.6% for the vote (26 ott 2026).
         *
         * Four variables from four models over 7 days: **1.6 calls** a fetch (CLAUDE.md, the
         * free tier's arithmetic; 1.2 with three models until 27 Oct 2026). With the
         * forecast's 5.4 and the air's 1.4, a fetch weighs 8.4 of the 10 000 free calls a
         * day per address (`tools/probe_provider.py` prints the sum).
         */
        const val HOURLY_VARIABLES = "temperature_2m,cloud_cover,visibility,relative_humidity_2m"

        val REQUEST: String get() = "$MODELS|$HOURLY_VARIABLES"
    }
}
