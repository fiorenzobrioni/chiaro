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
        @Query("forecast_minutely_15") forecastQuarters: Int = FORECAST_QUARTERS,
        @Query("past_minutely_15") pastQuarters: Int = PAST_QUARTERS
    ): ForecastResponseDto

    companion object {
        const val BASE_URL = "https://api.open-meteo.com/"

        /**
         * Days of forecast requested, and therefore `× 24` hourly values returned.
         * A constant since upstream Fase 16a because the mapper's hourly window is derived
         * from it: the two used to be a 7 here and a 25 there, which is how the app
         * ended up parsing a week of hours and keeping one day of them.
         */
        const val FORECAST_DAYS = 7

        /**
         * What this request costs of Open-Meteo's free calls (CLAUDE.md, «Free and without a
         * key is a constraint», 27 Oct 2026): 53 variables over 7 days weigh **5.3 calls**
         * (one call per ten variables, one per two weeks; `tools/probe_provider.py` prints
         * the sum). 5.4 with 54 until 17 nov 2026, when two left: `current.is_day`, which
         * nothing read, and `daily.precipitation_hours`, which the app counts itself from
         * the hourly amounts, to the same number; 5.2 until 19 nov 2026, when `cape` came in
         * (Fase 17.4).
         * A variable added here is a tenth of a call more on every fetch.
         */
        const val CURRENT_VARIABLES =
            "temperature_2m,relative_humidity_2m,apparent_temperature,dew_point_2m," +
                "precipitation,weather_code,pressure_msl,wind_speed_10m," +
                "wind_direction_10m,wind_gusts_10m,visibility,cloud_cover,uv_index," +
                "cloud_cover_low,cloud_cover_mid,cloud_cover_high," +
                // 25 set 2026: the phase and the convective part, for WeatherStateEngine.
                "snowfall,showers"
        // cloud_cover repairs `weather_code`'s sky (Fase 13c) — see WeatherReportMapper.
        // `visibility` is not read by the state engine since 17 nov 2026 (fog is the models'
        // votes, the witnesses' visibility and this model's humidity and wind): it is the
        // fog tile's number where it agrees with the fog, the canvas's thickness, and an
        // alert's `current.visibility_km`. Hourly cloud_cover has a second reader since upstream Fase 16a:
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
                // variables, **+0.2 calls** a fetch.
                "freezing_level_height,snowfall_height," +
                // 19 nov 2026 (Fase 17.4): the convective energy of the hour, an instant, for
                // the day's possible storm where the provider's code is silent (`PossibleStorm`),
                // and since 17.6 for whether a rain is a shower (`WeatherStateEngine`, rule 3).
                // Served whole all week by `best_match` wherever it was tried (Milan, Palermo,
                // London, Oslo, New York, Tokyo, Singapore, Sydney, 19 nov 2026). One variable,
                // **+0.1 calls**: the forecast 5.3, a fetch 7.9 (9.1 since the witnesses' cloud
                // layers, 22 nov 2026).
                "cape"
        /**
         * `sunrise`, `sunset` and `daylight_duration` left this list on 20 set 2026,
         * having been asked for and dropped on the floor since upstream Fase 16e handed the
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
            "$CURRENT_VARIABLES|$HOURLY_VARIABLES|$DAILY_VARIABLES|" +
                "$MINUTELY_15_VARIABLES:$FORECAST_QUARTERS+$PAST_QUARTERS"

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

        /**
         * The three quarters before the one the `current` block is (17 nov 2026), so that the
         * series always holds the four quarters of the hour that block falls in: the mapper
         * reads from them whether the block's amount is a quarter a model computed or the
         * hour's cut in four and rounded, where an hour of 0.1 mm is four quarters of 0.0
         * (`WeatherReportMapper`, «cutInFour»). No variable more: the request weighs the same.
         */
        const val PAST_QUARTERS = 3

        const val DAILY_VARIABLES =
            "weather_code,temperature_2m_max,temperature_2m_min," +
                "precipitation_probability_max,uv_index_max," +
                // 24 set 2026: how much, not only how likely — the day's rain and snow, and
                // the strongest gust. Not `precipitation_hours` (17 nov 2026): it is the count
                // of the hours with an amount, which the hourly series already says
                // (`WeatherReportMapper.precipHours`, equal on 616 days of 616).
                "precipitation_sum,snowfall_sum,wind_gusts_10m_max," +
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
        @Query("timezone") timezone: String = "auto",
        @Query("hourly") hourly: String = HOURLY_VARIABLES,
        @Query("forecast_days") forecastDays: Int = POLLEN_DAYS
    ): AirQualityResponseDto

    companion object {
        const val BASE_URL = "https://air-quality-api.open-meteo.com/"

        /**
         * 8 variables, the two indices of the current hour and the six pollens of the day:
         * **1 call** a fetch (CLAUDE.md, the free tier's arithmetic). The six pollutants
         * behind the indices (PM2.5, PM10, ozone, NO2, SO2, CO) left on 17 nov 2026: asked
         * for and mapped since the first release, and read by no screen, no alert and no
         * rule; with them the request weighed 1.4. A screen that wants one asks for it again,
         * with its reader.
         */
        const val CURRENT_VARIABLES = "us_aqi,european_aqi"

        /**
         * The pollens by the hour, for today (17 nov 2026): the classes the app prints are
         * written for a day's mean concentration, and until then they were read off the
         * current hour's, which moves through two classes or more on four in-season days
         * out of five (`WeatherCodes.PollenSpecies`). [POLLEN_DAYS] of them: with
         * `timezone=auto` the block is the place's own day, midnight to midnight.
         */
        const val HOURLY_VARIABLES =
            "grass_pollen,birch_pollen,alder_pollen,olive_pollen,ragweed_pollen,mugwort_pollen"
        const val POLLEN_DAYS = 1

        /**
         * The air's part of `ReportDiskCache.CURRENT_REQUEST` (19 nov 2026): an entry fetched
         * with another list here lacks what the mapper now reads, and is not a fresh hit.
         */
        val REQUEST: String get() = "$CURRENT_VARIABLES|$HOURLY_VARIABLES:$POLLEN_DAYS"
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
 * forecasts. Since 22 nov 2026 (Fase 18.3) the two global ones lend their cloud too: the hour's
 * sky is the mean of `best_match`'s and theirs (`ModelMean`), the one number of theirs that
 * reaches a screen, as a combination measured against the satellite. Every other number
 * printed stays `best_match`'s.
 *
 * Its own request, not more `models=` on the main one: Open-Meteo suffixes EVERY variable
 * of a multi-model response with its model's name, which would change the shape of the
 * report the whole app is built on. Best-effort, like the air: a failure costs the vote,
 * and with one model's vote at most there is no fog (17 nov 2026), and the sky is
 * `best_match`'s alone, as it was before the mean.
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
         * Europe where ICON's goes on 30 Nov 2026 (measured in 14.2, `docs/STATES.md` §11;
         * since 17 nov 2026 their visibility is the only one the fog rule reads, one vote each;
         * outside their domain — Palermo, Sydney — the response simply has no column of
         * theirs, verified live on 27 Oct 2026, and inside it their series runs three days).
         * NOAA's GFS left the same day: its visibility is right in 5% of the fogs seen (CSI
         * 0.03) and adding it to the four costs two points. Not ICON-EU: in Italy
         * `best_match` IS the ICON family all week (measured on 15 cities, 26 ott 2026), and
         * ICON-D2 runs inside ICON-EU. Where `best_match` is one of these four (ECMWF over
         * Sydney, the Met Office over London, DMI over Keflavik) the mapper recognises it
         * from the numbers and leaves it out: its model votes once, with its humidity and wind.
         * A change here changes [REQUEST], and the disk cache knows the entries fetched
         * with the old list are not fresh hits (`ReportDiskCache.CURRENT_REQUEST`).
         */
        const val MODELS =
            "ecmwf_ifs,ukmo_global_deterministic_10km,knmi_harmonie_arome_europe,dmi_harmonie_arome_europe"

        /**
         * What fog reads — the visibility, each witness's vote, and the humidity, for the mean
         * that filters it — and what tells a witness that is `best_match` itself: the
         * temperature and the cloud. Nothing more is asked for the rule of 17 nov 2026: the
         * wind it reads is the own model's, already in the forecast. Not the amounts: the
         * phase was measured for a vote too, and `best_match` alone called 94.3% of a
         * winter's precipitation hours right against 94.6% for the vote (26 ott 2026).
         *
         * Since 22 nov 2026 (Fase 18.3) the three layers of the cloud, which with the total
         * make the sky's mean (`ModelMean`): a witness counts in it only with all four. KNMI
         * and DMI send them too, in the same request, and the mean leaves them out
         * (`Witnesses.MEAN_MODELS`): one list for every model is what the request can say.
         *
         * Seven variables from four models over 7 days: **2.8 calls** a fetch (CLAUDE.md, the
         * free tier's arithmetic; 1.6 with four variables until 22 nov 2026, 1.2 with three
         * models until 27 Oct 2026). With the forecast's 5.3 and the air's 1, a fetch weighs
         * 9.1 of the 10 000 free calls a day per address (`tools/probe_provider.py` prints the
         * sum; 7.9 until 22 nov 2026, 8.4 until 17 nov 2026, 7.8 until 19 nov 2026).
         */
        const val HOURLY_VARIABLES =
            "temperature_2m,cloud_cover,visibility,relative_humidity_2m," +
                "cloud_cover_low,cloud_cover_mid,cloud_cover_high"

        val REQUEST: String get() = "$MODELS|$HOURLY_VARIABLES"
    }
}
