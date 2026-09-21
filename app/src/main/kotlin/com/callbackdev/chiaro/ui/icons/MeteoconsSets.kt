package com.callbackdev.chiaro.ui.icons

import androidx.annotation.DrawableRes
import com.callbackdev.chiaro.R

/**
 * Le quattro facce di ogni disegno di Meteocons, e i gemelli che si muovono.
 *
 * **File generato da `tools/import_meteocons_v3.py` — non si modifica a mano.** La lista
 * sta in `tools/shipped_icons.py`; questo e' il suo risultato, ed e' anche la ragione per
 * cui l'APK pesa quanto la lista mentre il repo tiene la famiglia intera: un drawable che
 * nessuna tabella qui nomina non e' referenziato, e `shrinkResources` lo toglie dalla
 * release.
 *
 * La chiave e' sempre l'id del set **line su fondo chiaro**: e' la cucitura su cui
 * `ChiaroIcons` fa girare stile e fondo, la stessa che aveva la v2.
 */
internal object MeteoconsSets {

    /** line, fondo scuro. */
    val lineDarkOf: Map<Int, Int> = mapOf(
        R.drawable.mc3_smoke to R.drawable.mc3n_smoke,
        R.drawable.mc3_clear_day to R.drawable.mc3n_clear_day,
        R.drawable.mc3_clear_night to R.drawable.mc3n_clear_night,
        R.drawable.mc3_partly_cloudy_day to R.drawable.mc3n_partly_cloudy_day,
        R.drawable.mc3_partly_cloudy_night to R.drawable.mc3n_partly_cloudy_night,
        R.drawable.mc3_overcast to R.drawable.mc3n_overcast,
        R.drawable.mc3_cloudy to R.drawable.mc3n_cloudy,
        R.drawable.mc3_fog_day to R.drawable.mc3n_fog_day,
        R.drawable.mc3_fog_night to R.drawable.mc3n_fog_night,
        R.drawable.mc3_overcast_drizzle to R.drawable.mc3n_overcast_drizzle,
        R.drawable.mc3_overcast_rain to R.drawable.mc3n_overcast_rain,
        R.drawable.mc3_overcast_sleet to R.drawable.mc3n_overcast_sleet,
        R.drawable.mc3_overcast_snow to R.drawable.mc3n_overcast_snow,
        R.drawable.mc3_partly_cloudy_day_rain to R.drawable.mc3n_partly_cloudy_day_rain,
        R.drawable.mc3_partly_cloudy_night_rain to R.drawable.mc3n_partly_cloudy_night_rain,
        R.drawable.mc3_partly_cloudy_day_snow to R.drawable.mc3n_partly_cloudy_day_snow,
        R.drawable.mc3_partly_cloudy_night_snow to R.drawable.mc3n_partly_cloudy_night_snow,
        R.drawable.mc3_extreme_rain to R.drawable.mc3n_extreme_rain,
        R.drawable.mc3_thunderstorms_day to R.drawable.mc3n_thunderstorms_day,
        R.drawable.mc3_thunderstorms_night to R.drawable.mc3n_thunderstorms_night,
        R.drawable.mc3_thunderstorms_day_hail to R.drawable.mc3n_thunderstorms_day_hail,
        R.drawable.mc3_thunderstorms_night_hail to R.drawable.mc3n_thunderstorms_night_hail,
        R.drawable.mc3_not_available to R.drawable.mc3n_not_available,
        R.drawable.mc3_wind to R.drawable.mc3n_wind,
        R.drawable.mc3_humidity to R.drawable.mc3n_humidity,
        R.drawable.mc3_uv_index to R.drawable.mc3n_uv_index,
        R.drawable.mc3_thermometer to R.drawable.mc3n_thermometer,
        R.drawable.mc3_barometer to R.drawable.mc3n_barometer,
        R.drawable.mc3_raindrops to R.drawable.mc3n_raindrops,
        R.drawable.mc3_mist to R.drawable.mc3n_mist,
        R.drawable.mc3_snowflake to R.drawable.mc3n_snowflake,
        R.drawable.mc3_smoke_particles to R.drawable.mc3n_smoke_particles,
        R.drawable.mc3_compass to R.drawable.mc3n_compass,
        R.drawable.mc3_pollen to R.drawable.mc3n_pollen,
        R.drawable.mc3_sunrise to R.drawable.mc3n_sunrise,
        R.drawable.mc3_sunset to R.drawable.mc3n_sunset,
        R.drawable.mc3_moonrise to R.drawable.mc3n_moonrise,
        R.drawable.mc3_moonset to R.drawable.mc3n_moonset,
        R.drawable.mc3_horizon to R.drawable.mc3n_horizon,
        R.drawable.mc3_star to R.drawable.mc3n_star,
        R.drawable.mc3_starry_night to R.drawable.mc3n_starry_night,
        R.drawable.mc3_falling_stars to R.drawable.mc3n_falling_stars,
        R.drawable.mc3_solar_eclipse to R.drawable.mc3n_solar_eclipse,
        R.drawable.mc3_moon_new to R.drawable.mc3n_moon_new,
        R.drawable.mc3_moon_waxing_crescent to R.drawable.mc3n_moon_waxing_crescent,
        R.drawable.mc3_moon_first_quarter to R.drawable.mc3n_moon_first_quarter,
        R.drawable.mc3_moon_waxing_gibbous to R.drawable.mc3n_moon_waxing_gibbous,
        R.drawable.mc3_moon_full to R.drawable.mc3n_moon_full,
        R.drawable.mc3_moon_waning_gibbous to R.drawable.mc3n_moon_waning_gibbous,
        R.drawable.mc3_moon_last_quarter to R.drawable.mc3n_moon_last_quarter,
        R.drawable.mc3_moon_waning_crescent to R.drawable.mc3n_moon_waning_crescent,
        R.drawable.mc3_rainbow to R.drawable.mc3n_rainbow,
        R.drawable.mc3_uv_index_1 to R.drawable.mc3n_uv_index_1,
        R.drawable.mc3_uv_index_2 to R.drawable.mc3n_uv_index_2,
        R.drawable.mc3_uv_index_3 to R.drawable.mc3n_uv_index_3,
        R.drawable.mc3_uv_index_4 to R.drawable.mc3n_uv_index_4,
        R.drawable.mc3_uv_index_5 to R.drawable.mc3n_uv_index_5,
        R.drawable.mc3_uv_index_6 to R.drawable.mc3n_uv_index_6,
        R.drawable.mc3_uv_index_7 to R.drawable.mc3n_uv_index_7,
        R.drawable.mc3_uv_index_8 to R.drawable.mc3n_uv_index_8,
        R.drawable.mc3_uv_index_9 to R.drawable.mc3n_uv_index_9,
        R.drawable.mc3_uv_index_10 to R.drawable.mc3n_uv_index_10,
        R.drawable.mc3_uv_index_11 to R.drawable.mc3n_uv_index_11,
        R.drawable.mc3_uv_index_11_plus to R.drawable.mc3n_uv_index_11_plus,
        R.drawable.mc3_barometer_moderate to R.drawable.mc3n_barometer_moderate,
        R.drawable.mc3_pollen_grass to R.drawable.mc3n_pollen_grass,
        R.drawable.mc3_pollen_tree to R.drawable.mc3n_pollen_tree,
        R.drawable.mc3_pollen_weed to R.drawable.mc3n_pollen_weed,
        R.drawable.mc3_pollen_grass_low to R.drawable.mc3n_pollen_grass_low,
        R.drawable.mc3_pollen_grass_moderate to R.drawable.mc3n_pollen_grass_moderate,
        R.drawable.mc3_pollen_grass_high to R.drawable.mc3n_pollen_grass_high,
        R.drawable.mc3_pollen_tree_low to R.drawable.mc3n_pollen_tree_low,
        R.drawable.mc3_pollen_tree_moderate to R.drawable.mc3n_pollen_tree_moderate,
        R.drawable.mc3_pollen_tree_high to R.drawable.mc3n_pollen_tree_high,
        R.drawable.mc3_pollen_weed_low to R.drawable.mc3n_pollen_weed_low,
        R.drawable.mc3_pollen_weed_moderate to R.drawable.mc3n_pollen_weed_moderate,
        R.drawable.mc3_pollen_weed_high to R.drawable.mc3n_pollen_weed_high,
        R.drawable.mc3_haze to R.drawable.mc3n_haze,
        R.drawable.mc3_fog to R.drawable.mc3n_fog,
    )

    /** flat, fondo chiaro. */
    val flatOf: Map<Int, Int> = mapOf(
        R.drawable.mc3_smoke to R.drawable.mc3f_smoke,
        R.drawable.mc3_clear_day to R.drawable.mc3f_clear_day,
        R.drawable.mc3_clear_night to R.drawable.mc3f_clear_night,
        R.drawable.mc3_partly_cloudy_day to R.drawable.mc3f_partly_cloudy_day,
        R.drawable.mc3_partly_cloudy_night to R.drawable.mc3f_partly_cloudy_night,
        R.drawable.mc3_overcast to R.drawable.mc3f_overcast,
        R.drawable.mc3_cloudy to R.drawable.mc3f_cloudy,
        R.drawable.mc3_fog_day to R.drawable.mc3f_fog_day,
        R.drawable.mc3_fog_night to R.drawable.mc3f_fog_night,
        R.drawable.mc3_overcast_drizzle to R.drawable.mc3f_overcast_drizzle,
        R.drawable.mc3_overcast_rain to R.drawable.mc3f_overcast_rain,
        R.drawable.mc3_overcast_sleet to R.drawable.mc3f_overcast_sleet,
        R.drawable.mc3_overcast_snow to R.drawable.mc3f_overcast_snow,
        R.drawable.mc3_partly_cloudy_day_rain to R.drawable.mc3f_partly_cloudy_day_rain,
        R.drawable.mc3_partly_cloudy_night_rain to R.drawable.mc3f_partly_cloudy_night_rain,
        R.drawable.mc3_partly_cloudy_day_snow to R.drawable.mc3f_partly_cloudy_day_snow,
        R.drawable.mc3_partly_cloudy_night_snow to R.drawable.mc3f_partly_cloudy_night_snow,
        R.drawable.mc3_extreme_rain to R.drawable.mc3f_extreme_rain,
        R.drawable.mc3_thunderstorms_day to R.drawable.mc3f_thunderstorms_day,
        R.drawable.mc3_thunderstorms_night to R.drawable.mc3f_thunderstorms_night,
        R.drawable.mc3_thunderstorms_day_hail to R.drawable.mc3f_thunderstorms_day_hail,
        R.drawable.mc3_thunderstorms_night_hail to R.drawable.mc3f_thunderstorms_night_hail,
        R.drawable.mc3_not_available to R.drawable.mc3f_not_available,
        R.drawable.mc3_wind to R.drawable.mc3f_wind,
        R.drawable.mc3_humidity to R.drawable.mc3f_humidity,
        R.drawable.mc3_uv_index to R.drawable.mc3f_uv_index,
        R.drawable.mc3_thermometer to R.drawable.mc3f_thermometer,
        R.drawable.mc3_barometer to R.drawable.mc3f_barometer,
        R.drawable.mc3_raindrops to R.drawable.mc3f_raindrops,
        R.drawable.mc3_mist to R.drawable.mc3f_mist,
        R.drawable.mc3_snowflake to R.drawable.mc3f_snowflake,
        R.drawable.mc3_smoke_particles to R.drawable.mc3f_smoke_particles,
        R.drawable.mc3_compass to R.drawable.mc3f_compass,
        R.drawable.mc3_pollen to R.drawable.mc3f_pollen,
        R.drawable.mc3_sunrise to R.drawable.mc3f_sunrise,
        R.drawable.mc3_sunset to R.drawable.mc3f_sunset,
        R.drawable.mc3_moonrise to R.drawable.mc3f_moonrise,
        R.drawable.mc3_moonset to R.drawable.mc3f_moonset,
        R.drawable.mc3_horizon to R.drawable.mc3f_horizon,
        R.drawable.mc3_star to R.drawable.mc3f_star,
        R.drawable.mc3_starry_night to R.drawable.mc3f_starry_night,
        R.drawable.mc3_falling_stars to R.drawable.mc3f_falling_stars,
        R.drawable.mc3_solar_eclipse to R.drawable.mc3f_solar_eclipse,
        R.drawable.mc3_moon_new to R.drawable.mc3f_moon_new,
        R.drawable.mc3_moon_waxing_crescent to R.drawable.mc3f_moon_waxing_crescent,
        R.drawable.mc3_moon_first_quarter to R.drawable.mc3f_moon_first_quarter,
        R.drawable.mc3_moon_waxing_gibbous to R.drawable.mc3f_moon_waxing_gibbous,
        R.drawable.mc3_moon_full to R.drawable.mc3f_moon_full,
        R.drawable.mc3_moon_waning_gibbous to R.drawable.mc3f_moon_waning_gibbous,
        R.drawable.mc3_moon_last_quarter to R.drawable.mc3f_moon_last_quarter,
        R.drawable.mc3_moon_waning_crescent to R.drawable.mc3f_moon_waning_crescent,
        R.drawable.mc3_rainbow to R.drawable.mc3f_rainbow,
        R.drawable.mc3_uv_index_1 to R.drawable.mc3f_uv_index_1,
        R.drawable.mc3_uv_index_2 to R.drawable.mc3f_uv_index_2,
        R.drawable.mc3_uv_index_3 to R.drawable.mc3f_uv_index_3,
        R.drawable.mc3_uv_index_4 to R.drawable.mc3f_uv_index_4,
        R.drawable.mc3_uv_index_5 to R.drawable.mc3f_uv_index_5,
        R.drawable.mc3_uv_index_6 to R.drawable.mc3f_uv_index_6,
        R.drawable.mc3_uv_index_7 to R.drawable.mc3f_uv_index_7,
        R.drawable.mc3_uv_index_8 to R.drawable.mc3f_uv_index_8,
        R.drawable.mc3_uv_index_9 to R.drawable.mc3f_uv_index_9,
        R.drawable.mc3_uv_index_10 to R.drawable.mc3f_uv_index_10,
        R.drawable.mc3_uv_index_11 to R.drawable.mc3f_uv_index_11,
        R.drawable.mc3_uv_index_11_plus to R.drawable.mc3f_uv_index_11_plus,
        R.drawable.mc3_barometer_moderate to R.drawable.mc3f_barometer_moderate,
        R.drawable.mc3_pollen_grass to R.drawable.mc3f_pollen_grass,
        R.drawable.mc3_pollen_tree to R.drawable.mc3f_pollen_tree,
        R.drawable.mc3_pollen_weed to R.drawable.mc3f_pollen_weed,
        R.drawable.mc3_pollen_grass_low to R.drawable.mc3f_pollen_grass_low,
        R.drawable.mc3_pollen_grass_moderate to R.drawable.mc3f_pollen_grass_moderate,
        R.drawable.mc3_pollen_grass_high to R.drawable.mc3f_pollen_grass_high,
        R.drawable.mc3_pollen_tree_low to R.drawable.mc3f_pollen_tree_low,
        R.drawable.mc3_pollen_tree_moderate to R.drawable.mc3f_pollen_tree_moderate,
        R.drawable.mc3_pollen_tree_high to R.drawable.mc3f_pollen_tree_high,
        R.drawable.mc3_pollen_weed_low to R.drawable.mc3f_pollen_weed_low,
        R.drawable.mc3_pollen_weed_moderate to R.drawable.mc3f_pollen_weed_moderate,
        R.drawable.mc3_pollen_weed_high to R.drawable.mc3f_pollen_weed_high,
        R.drawable.mc3_haze to R.drawable.mc3f_haze,
        R.drawable.mc3_fog to R.drawable.mc3f_fog,
    )

    /** flat, fondo scuro. */
    val flatDarkOf: Map<Int, Int> = mapOf(
        R.drawable.mc3_smoke to R.drawable.mc3fn_smoke,
        R.drawable.mc3_clear_day to R.drawable.mc3fn_clear_day,
        R.drawable.mc3_clear_night to R.drawable.mc3fn_clear_night,
        R.drawable.mc3_partly_cloudy_day to R.drawable.mc3fn_partly_cloudy_day,
        R.drawable.mc3_partly_cloudy_night to R.drawable.mc3fn_partly_cloudy_night,
        R.drawable.mc3_overcast to R.drawable.mc3fn_overcast,
        R.drawable.mc3_cloudy to R.drawable.mc3fn_cloudy,
        R.drawable.mc3_fog_day to R.drawable.mc3fn_fog_day,
        R.drawable.mc3_fog_night to R.drawable.mc3fn_fog_night,
        R.drawable.mc3_overcast_drizzle to R.drawable.mc3fn_overcast_drizzle,
        R.drawable.mc3_overcast_rain to R.drawable.mc3fn_overcast_rain,
        R.drawable.mc3_overcast_sleet to R.drawable.mc3fn_overcast_sleet,
        R.drawable.mc3_overcast_snow to R.drawable.mc3fn_overcast_snow,
        R.drawable.mc3_partly_cloudy_day_rain to R.drawable.mc3fn_partly_cloudy_day_rain,
        R.drawable.mc3_partly_cloudy_night_rain to R.drawable.mc3fn_partly_cloudy_night_rain,
        R.drawable.mc3_partly_cloudy_day_snow to R.drawable.mc3fn_partly_cloudy_day_snow,
        R.drawable.mc3_partly_cloudy_night_snow to R.drawable.mc3fn_partly_cloudy_night_snow,
        R.drawable.mc3_extreme_rain to R.drawable.mc3fn_extreme_rain,
        R.drawable.mc3_thunderstorms_day to R.drawable.mc3fn_thunderstorms_day,
        R.drawable.mc3_thunderstorms_night to R.drawable.mc3fn_thunderstorms_night,
        R.drawable.mc3_thunderstorms_day_hail to R.drawable.mc3fn_thunderstorms_day_hail,
        R.drawable.mc3_thunderstorms_night_hail to R.drawable.mc3fn_thunderstorms_night_hail,
        R.drawable.mc3_not_available to R.drawable.mc3fn_not_available,
        R.drawable.mc3_wind to R.drawable.mc3fn_wind,
        R.drawable.mc3_humidity to R.drawable.mc3fn_humidity,
        R.drawable.mc3_uv_index to R.drawable.mc3fn_uv_index,
        R.drawable.mc3_thermometer to R.drawable.mc3fn_thermometer,
        R.drawable.mc3_barometer to R.drawable.mc3fn_barometer,
        R.drawable.mc3_raindrops to R.drawable.mc3fn_raindrops,
        R.drawable.mc3_mist to R.drawable.mc3fn_mist,
        R.drawable.mc3_snowflake to R.drawable.mc3fn_snowflake,
        R.drawable.mc3_smoke_particles to R.drawable.mc3fn_smoke_particles,
        R.drawable.mc3_compass to R.drawable.mc3fn_compass,
        R.drawable.mc3_pollen to R.drawable.mc3fn_pollen,
        R.drawable.mc3_sunrise to R.drawable.mc3fn_sunrise,
        R.drawable.mc3_sunset to R.drawable.mc3fn_sunset,
        R.drawable.mc3_moonrise to R.drawable.mc3fn_moonrise,
        R.drawable.mc3_moonset to R.drawable.mc3fn_moonset,
        R.drawable.mc3_horizon to R.drawable.mc3fn_horizon,
        R.drawable.mc3_star to R.drawable.mc3fn_star,
        R.drawable.mc3_starry_night to R.drawable.mc3fn_starry_night,
        R.drawable.mc3_falling_stars to R.drawable.mc3fn_falling_stars,
        R.drawable.mc3_solar_eclipse to R.drawable.mc3fn_solar_eclipse,
        R.drawable.mc3_moon_new to R.drawable.mc3fn_moon_new,
        R.drawable.mc3_moon_waxing_crescent to R.drawable.mc3fn_moon_waxing_crescent,
        R.drawable.mc3_moon_first_quarter to R.drawable.mc3fn_moon_first_quarter,
        R.drawable.mc3_moon_waxing_gibbous to R.drawable.mc3fn_moon_waxing_gibbous,
        R.drawable.mc3_moon_full to R.drawable.mc3fn_moon_full,
        R.drawable.mc3_moon_waning_gibbous to R.drawable.mc3fn_moon_waning_gibbous,
        R.drawable.mc3_moon_last_quarter to R.drawable.mc3fn_moon_last_quarter,
        R.drawable.mc3_moon_waning_crescent to R.drawable.mc3fn_moon_waning_crescent,
        R.drawable.mc3_rainbow to R.drawable.mc3fn_rainbow,
        R.drawable.mc3_uv_index_1 to R.drawable.mc3fn_uv_index_1,
        R.drawable.mc3_uv_index_2 to R.drawable.mc3fn_uv_index_2,
        R.drawable.mc3_uv_index_3 to R.drawable.mc3fn_uv_index_3,
        R.drawable.mc3_uv_index_4 to R.drawable.mc3fn_uv_index_4,
        R.drawable.mc3_uv_index_5 to R.drawable.mc3fn_uv_index_5,
        R.drawable.mc3_uv_index_6 to R.drawable.mc3fn_uv_index_6,
        R.drawable.mc3_uv_index_7 to R.drawable.mc3fn_uv_index_7,
        R.drawable.mc3_uv_index_8 to R.drawable.mc3fn_uv_index_8,
        R.drawable.mc3_uv_index_9 to R.drawable.mc3fn_uv_index_9,
        R.drawable.mc3_uv_index_10 to R.drawable.mc3fn_uv_index_10,
        R.drawable.mc3_uv_index_11 to R.drawable.mc3fn_uv_index_11,
        R.drawable.mc3_uv_index_11_plus to R.drawable.mc3fn_uv_index_11_plus,
        R.drawable.mc3_barometer_moderate to R.drawable.mc3fn_barometer_moderate,
        R.drawable.mc3_pollen_grass to R.drawable.mc3fn_pollen_grass,
        R.drawable.mc3_pollen_tree to R.drawable.mc3fn_pollen_tree,
        R.drawable.mc3_pollen_weed to R.drawable.mc3fn_pollen_weed,
        R.drawable.mc3_pollen_grass_low to R.drawable.mc3fn_pollen_grass_low,
        R.drawable.mc3_pollen_grass_moderate to R.drawable.mc3fn_pollen_grass_moderate,
        R.drawable.mc3_pollen_grass_high to R.drawable.mc3fn_pollen_grass_high,
        R.drawable.mc3_pollen_tree_low to R.drawable.mc3fn_pollen_tree_low,
        R.drawable.mc3_pollen_tree_moderate to R.drawable.mc3fn_pollen_tree_moderate,
        R.drawable.mc3_pollen_tree_high to R.drawable.mc3fn_pollen_tree_high,
        R.drawable.mc3_pollen_weed_low to R.drawable.mc3fn_pollen_weed_low,
        R.drawable.mc3_pollen_weed_moderate to R.drawable.mc3fn_pollen_weed_moderate,
        R.drawable.mc3_pollen_weed_high to R.drawable.mc3fn_pollen_weed_high,
        R.drawable.mc3_haze to R.drawable.mc3fn_haze,
        R.drawable.mc3_fog to R.drawable.mc3fn_fog,
    )

    /**
     * I quattro gemelli animati di un disegno che ne ha, nello stesso ordine in cui si
     * scelgono i set fermi: line chiaro, line scuro, flat chiaro, flat scuro.
     */
    class Moving(
        @DrawableRes val line: Int,
        @DrawableRes val lineDark: Int,
        @DrawableRes val flat: Int,
        @DrawableRes val flatDark: Int
    )

    /**
     * Quali disegni si muovono (DESIGN §7.1): la famiglia delle condizioni e solo quella,
     * perche' il marchio di un tile etichetta una quantita' e un barometro che gira per
     * sempre e' decorazione. `not-available` non c'e': Meteocons lo disegna fermo, ed e'
     * la quantita' di movimento giusta per «non lo sappiamo».
     */
    val movingOf: Map<Int, Moving> = mapOf(
        R.drawable.mc3_clear_day to Moving(
            R.drawable.mc3a_clear_day, R.drawable.mc3an_clear_day,
            R.drawable.mc3fa_clear_day, R.drawable.mc3fan_clear_day
        ),
        R.drawable.mc3_clear_night to Moving(
            R.drawable.mc3a_clear_night, R.drawable.mc3an_clear_night,
            R.drawable.mc3fa_clear_night, R.drawable.mc3fan_clear_night
        ),
        R.drawable.mc3_partly_cloudy_day to Moving(
            R.drawable.mc3a_partly_cloudy_day, R.drawable.mc3an_partly_cloudy_day,
            R.drawable.mc3fa_partly_cloudy_day, R.drawable.mc3fan_partly_cloudy_day
        ),
        R.drawable.mc3_partly_cloudy_night to Moving(
            R.drawable.mc3a_partly_cloudy_night, R.drawable.mc3an_partly_cloudy_night,
            R.drawable.mc3fa_partly_cloudy_night, R.drawable.mc3fan_partly_cloudy_night
        ),
        R.drawable.mc3_overcast to Moving(
            R.drawable.mc3a_overcast, R.drawable.mc3an_overcast,
            R.drawable.mc3fa_overcast, R.drawable.mc3fan_overcast
        ),
        R.drawable.mc3_cloudy to Moving(
            R.drawable.mc3a_cloudy, R.drawable.mc3an_cloudy,
            R.drawable.mc3fa_cloudy, R.drawable.mc3fan_cloudy
        ),
        R.drawable.mc3_fog_day to Moving(
            R.drawable.mc3a_fog_day, R.drawable.mc3an_fog_day,
            R.drawable.mc3fa_fog_day, R.drawable.mc3fan_fog_day
        ),
        R.drawable.mc3_fog_night to Moving(
            R.drawable.mc3a_fog_night, R.drawable.mc3an_fog_night,
            R.drawable.mc3fa_fog_night, R.drawable.mc3fan_fog_night
        ),
        R.drawable.mc3_overcast_drizzle to Moving(
            R.drawable.mc3a_overcast_drizzle, R.drawable.mc3an_overcast_drizzle,
            R.drawable.mc3fa_overcast_drizzle, R.drawable.mc3fan_overcast_drizzle
        ),
        R.drawable.mc3_overcast_rain to Moving(
            R.drawable.mc3a_overcast_rain, R.drawable.mc3an_overcast_rain,
            R.drawable.mc3fa_overcast_rain, R.drawable.mc3fan_overcast_rain
        ),
        R.drawable.mc3_overcast_sleet to Moving(
            R.drawable.mc3a_overcast_sleet, R.drawable.mc3an_overcast_sleet,
            R.drawable.mc3fa_overcast_sleet, R.drawable.mc3fan_overcast_sleet
        ),
        R.drawable.mc3_overcast_snow to Moving(
            R.drawable.mc3a_overcast_snow, R.drawable.mc3an_overcast_snow,
            R.drawable.mc3fa_overcast_snow, R.drawable.mc3fan_overcast_snow
        ),
        R.drawable.mc3_partly_cloudy_day_rain to Moving(
            R.drawable.mc3a_partly_cloudy_day_rain, R.drawable.mc3an_partly_cloudy_day_rain,
            R.drawable.mc3fa_partly_cloudy_day_rain, R.drawable.mc3fan_partly_cloudy_day_rain
        ),
        R.drawable.mc3_partly_cloudy_night_rain to Moving(
            R.drawable.mc3a_partly_cloudy_night_rain, R.drawable.mc3an_partly_cloudy_night_rain,
            R.drawable.mc3fa_partly_cloudy_night_rain, R.drawable.mc3fan_partly_cloudy_night_rain
        ),
        R.drawable.mc3_partly_cloudy_day_snow to Moving(
            R.drawable.mc3a_partly_cloudy_day_snow, R.drawable.mc3an_partly_cloudy_day_snow,
            R.drawable.mc3fa_partly_cloudy_day_snow, R.drawable.mc3fan_partly_cloudy_day_snow
        ),
        R.drawable.mc3_partly_cloudy_night_snow to Moving(
            R.drawable.mc3a_partly_cloudy_night_snow, R.drawable.mc3an_partly_cloudy_night_snow,
            R.drawable.mc3fa_partly_cloudy_night_snow, R.drawable.mc3fan_partly_cloudy_night_snow
        ),
        R.drawable.mc3_extreme_rain to Moving(
            R.drawable.mc3a_extreme_rain, R.drawable.mc3an_extreme_rain,
            R.drawable.mc3fa_extreme_rain, R.drawable.mc3fan_extreme_rain
        ),
        R.drawable.mc3_thunderstorms_day to Moving(
            R.drawable.mc3a_thunderstorms_day, R.drawable.mc3an_thunderstorms_day,
            R.drawable.mc3fa_thunderstorms_day, R.drawable.mc3fan_thunderstorms_day
        ),
        R.drawable.mc3_thunderstorms_night to Moving(
            R.drawable.mc3a_thunderstorms_night, R.drawable.mc3an_thunderstorms_night,
            R.drawable.mc3fa_thunderstorms_night, R.drawable.mc3fan_thunderstorms_night
        ),
        R.drawable.mc3_thunderstorms_day_hail to Moving(
            R.drawable.mc3a_thunderstorms_day_hail, R.drawable.mc3an_thunderstorms_day_hail,
            R.drawable.mc3fa_thunderstorms_day_hail, R.drawable.mc3fan_thunderstorms_day_hail
        ),
        R.drawable.mc3_thunderstorms_night_hail to Moving(
            R.drawable.mc3a_thunderstorms_night_hail, R.drawable.mc3an_thunderstorms_night_hail,
            R.drawable.mc3fa_thunderstorms_night_hail, R.drawable.mc3fan_thunderstorms_night_hail
        ),
    )

    /**
     * Il nome che l'icona ha a monte, per i test: una tabella che dice «questo codice
     * WMO prende questo disegno» deve poter nominare il disegno come lo nomina
     * l'illustratore, non con un id numerico che non si legge.
     */
    val byName: Map<String, Int> = mapOf(
        "smoke" to R.drawable.mc3_smoke,
        "clear-day" to R.drawable.mc3_clear_day,
        "clear-night" to R.drawable.mc3_clear_night,
        "partly-cloudy-day" to R.drawable.mc3_partly_cloudy_day,
        "partly-cloudy-night" to R.drawable.mc3_partly_cloudy_night,
        "overcast" to R.drawable.mc3_overcast,
        "cloudy" to R.drawable.mc3_cloudy,
        "fog-day" to R.drawable.mc3_fog_day,
        "fog-night" to R.drawable.mc3_fog_night,
        "overcast-drizzle" to R.drawable.mc3_overcast_drizzle,
        "overcast-rain" to R.drawable.mc3_overcast_rain,
        "overcast-sleet" to R.drawable.mc3_overcast_sleet,
        "overcast-snow" to R.drawable.mc3_overcast_snow,
        "partly-cloudy-day-rain" to R.drawable.mc3_partly_cloudy_day_rain,
        "partly-cloudy-night-rain" to R.drawable.mc3_partly_cloudy_night_rain,
        "partly-cloudy-day-snow" to R.drawable.mc3_partly_cloudy_day_snow,
        "partly-cloudy-night-snow" to R.drawable.mc3_partly_cloudy_night_snow,
        "extreme-rain" to R.drawable.mc3_extreme_rain,
        "thunderstorms-day" to R.drawable.mc3_thunderstorms_day,
        "thunderstorms-night" to R.drawable.mc3_thunderstorms_night,
        "thunderstorms-day-hail" to R.drawable.mc3_thunderstorms_day_hail,
        "thunderstorms-night-hail" to R.drawable.mc3_thunderstorms_night_hail,
        "not-available" to R.drawable.mc3_not_available,
        "wind" to R.drawable.mc3_wind,
        "humidity" to R.drawable.mc3_humidity,
        "uv-index" to R.drawable.mc3_uv_index,
        "thermometer" to R.drawable.mc3_thermometer,
        "barometer" to R.drawable.mc3_barometer,
        "raindrops" to R.drawable.mc3_raindrops,
        "mist" to R.drawable.mc3_mist,
        "snowflake" to R.drawable.mc3_snowflake,
        "smoke-particles" to R.drawable.mc3_smoke_particles,
        "compass" to R.drawable.mc3_compass,
        "pollen" to R.drawable.mc3_pollen,
        "sunrise" to R.drawable.mc3_sunrise,
        "sunset" to R.drawable.mc3_sunset,
        "moonrise" to R.drawable.mc3_moonrise,
        "moonset" to R.drawable.mc3_moonset,
        "horizon" to R.drawable.mc3_horizon,
        "star" to R.drawable.mc3_star,
        "starry-night" to R.drawable.mc3_starry_night,
        "falling-stars" to R.drawable.mc3_falling_stars,
        "solar-eclipse" to R.drawable.mc3_solar_eclipse,
        "moon-new" to R.drawable.mc3_moon_new,
        "moon-waxing-crescent" to R.drawable.mc3_moon_waxing_crescent,
        "moon-first-quarter" to R.drawable.mc3_moon_first_quarter,
        "moon-waxing-gibbous" to R.drawable.mc3_moon_waxing_gibbous,
        "moon-full" to R.drawable.mc3_moon_full,
        "moon-waning-gibbous" to R.drawable.mc3_moon_waning_gibbous,
        "moon-last-quarter" to R.drawable.mc3_moon_last_quarter,
        "moon-waning-crescent" to R.drawable.mc3_moon_waning_crescent,
        "rainbow" to R.drawable.mc3_rainbow,
        "uv-index-1" to R.drawable.mc3_uv_index_1,
        "uv-index-2" to R.drawable.mc3_uv_index_2,
        "uv-index-3" to R.drawable.mc3_uv_index_3,
        "uv-index-4" to R.drawable.mc3_uv_index_4,
        "uv-index-5" to R.drawable.mc3_uv_index_5,
        "uv-index-6" to R.drawable.mc3_uv_index_6,
        "uv-index-7" to R.drawable.mc3_uv_index_7,
        "uv-index-8" to R.drawable.mc3_uv_index_8,
        "uv-index-9" to R.drawable.mc3_uv_index_9,
        "uv-index-10" to R.drawable.mc3_uv_index_10,
        "uv-index-11" to R.drawable.mc3_uv_index_11,
        "uv-index-11-plus" to R.drawable.mc3_uv_index_11_plus,
        "barometer-moderate" to R.drawable.mc3_barometer_moderate,
        "pollen-grass" to R.drawable.mc3_pollen_grass,
        "pollen-tree" to R.drawable.mc3_pollen_tree,
        "pollen-weed" to R.drawable.mc3_pollen_weed,
        "pollen-grass-low" to R.drawable.mc3_pollen_grass_low,
        "pollen-grass-moderate" to R.drawable.mc3_pollen_grass_moderate,
        "pollen-grass-high" to R.drawable.mc3_pollen_grass_high,
        "pollen-tree-low" to R.drawable.mc3_pollen_tree_low,
        "pollen-tree-moderate" to R.drawable.mc3_pollen_tree_moderate,
        "pollen-tree-high" to R.drawable.mc3_pollen_tree_high,
        "pollen-weed-low" to R.drawable.mc3_pollen_weed_low,
        "pollen-weed-moderate" to R.drawable.mc3_pollen_weed_moderate,
        "pollen-weed-high" to R.drawable.mc3_pollen_weed_high,
        "haze" to R.drawable.mc3_haze,
        "fog" to R.drawable.mc3_fog,
    )
}
