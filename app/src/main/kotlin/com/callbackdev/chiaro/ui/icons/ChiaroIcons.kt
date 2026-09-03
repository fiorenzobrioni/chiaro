package com.callbackdev.chiaro.ui.icons

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.data.WeatherIcons
import com.callbackdev.chiaro.domain.model.MoonPhase

/**
 * The reader's icon style, provided by `MainActivity` from the settings alongside the
 * theme. FILL is the default a fresh install sees; the accessors below read this so
 * every screen switches together, with no screen ever asked to care.
 */
val LocalWeatherIcons = staticCompositionLocalOf { WeatherIcons.FILL }

/**
 * The weather icon set, behind one lookup (DESIGN.md §4.5, §13.1): **Meteocons**
 * (github.com/basmilius/meteocons, MIT), imported as vector drawables by
 * `tools/import_meteocons.py` from the v2.0.0 LINE and FILL sets, both recolored so
 * every mark clears 3:1 against both surfaces — the tool carries the two measured
 * tables, and `IconContrastTest` re-measures the emitted XML.
 *
 * The `*Res` functions are the actual mapping and stay plain functions on purpose:
 * they are unit-testable without Compose, and the Glance widgets (Fase 8) need
 * resource ids, not ImageVectors — which is also why they take the style as a
 * parameter, while the `@Composable` accessors read [LocalWeatherIcons].
 */
object ChiaroIcons {

    /** Every line drawable and its fill sibling: one table, so a missing sibling is a
     * loud [NoSuchElementException] in tests instead of a quiet mixed family. */
    private val fillOf = mapOf(
        R.drawable.mc_clear_day to R.drawable.mcf_clear_day,
        R.drawable.mc_clear_night to R.drawable.mcf_clear_night,
        R.drawable.mc_partly_cloudy_day to R.drawable.mcf_partly_cloudy_day,
        R.drawable.mc_partly_cloudy_night to R.drawable.mcf_partly_cloudy_night,
        R.drawable.mc_overcast to R.drawable.mcf_overcast,
        R.drawable.mc_cloudy to R.drawable.mcf_cloudy,
        R.drawable.mc_fog_day to R.drawable.mcf_fog_day,
        R.drawable.mc_fog_night to R.drawable.mcf_fog_night,
        R.drawable.mc_drizzle to R.drawable.mcf_drizzle,
        R.drawable.mc_rain to R.drawable.mcf_rain,
        R.drawable.mc_sleet to R.drawable.mcf_sleet,
        R.drawable.mc_snow to R.drawable.mcf_snow,
        R.drawable.mc_partly_cloudy_day_rain to R.drawable.mcf_partly_cloudy_day_rain,
        R.drawable.mc_partly_cloudy_night_rain to R.drawable.mcf_partly_cloudy_night_rain,
        R.drawable.mc_partly_cloudy_day_snow to R.drawable.mcf_partly_cloudy_day_snow,
        R.drawable.mc_partly_cloudy_night_snow to R.drawable.mcf_partly_cloudy_night_snow,
        R.drawable.mc_thunderstorms to R.drawable.mcf_thunderstorms,
        R.drawable.mc_thunderstorms_rain to R.drawable.mcf_thunderstorms_rain,
        R.drawable.mc_not_available to R.drawable.mcf_not_available,
        R.drawable.mc_wind to R.drawable.mcf_wind,
        R.drawable.mc_humidity to R.drawable.mcf_humidity,
        R.drawable.mc_uv_index to R.drawable.mcf_uv_index,
        R.drawable.mc_thermometer to R.drawable.mcf_thermometer,
        R.drawable.mc_barometer to R.drawable.mcf_barometer,
        R.drawable.mc_raindrop to R.drawable.mcf_raindrop,
        R.drawable.mc_raindrops to R.drawable.mcf_raindrops,
        R.drawable.mc_mist to R.drawable.mcf_mist,
        R.drawable.mc_umbrella to R.drawable.mcf_umbrella,
        R.drawable.mc_snowflake to R.drawable.mcf_snowflake,
        R.drawable.mc_dust to R.drawable.mcf_dust,
        R.drawable.mc_smoke_particles to R.drawable.mcf_smoke_particles,
        R.drawable.mc_compass to R.drawable.mcf_compass,
        R.drawable.mc_sunrise to R.drawable.mcf_sunrise,
        R.drawable.mc_sunset to R.drawable.mcf_sunset,
        R.drawable.mc_horizon to R.drawable.mcf_horizon,
        R.drawable.mc_star to R.drawable.mcf_star,
        R.drawable.mc_starry_night to R.drawable.mcf_starry_night,
        R.drawable.mc_falling_stars to R.drawable.mcf_falling_stars,
        R.drawable.mc_moonrise to R.drawable.mcf_moonrise,
        R.drawable.mc_moonset to R.drawable.mcf_moonset,
        R.drawable.mc_moon_new to R.drawable.mcf_moon_new,
        R.drawable.mc_moon_waxing_crescent to R.drawable.mcf_moon_waxing_crescent,
        R.drawable.mc_moon_first_quarter to R.drawable.mcf_moon_first_quarter,
        R.drawable.mc_moon_waxing_gibbous to R.drawable.mcf_moon_waxing_gibbous,
        R.drawable.mc_moon_full to R.drawable.mcf_moon_full,
        R.drawable.mc_moon_waning_gibbous to R.drawable.mcf_moon_waning_gibbous,
        R.drawable.mc_moon_last_quarter to R.drawable.mcf_moon_last_quarter,
        R.drawable.mc_moon_waning_crescent to R.drawable.mcf_moon_waning_crescent,
    )

    /** The style applied to a line resource id: identity for LINE, sibling for FILL. */
    @DrawableRes
    fun styledRes(@DrawableRes lineRes: Int, style: WeatherIcons): Int =
        if (style == WeatherIcons.FILL) fillOf.getValue(lineRes) else lineRes

    /**
     * The icon for a WMO weather code. [night] picks the nocturnal variant where one
     * exists — a clear night is not a sunny day, and that is the only place in the
     * mapping where the distinction changes anything.
     */
    @DrawableRes
    fun conditionRes(
        wmoCode: Int,
        night: Boolean = false,
        style: WeatherIcons = WeatherIcons.FILL
    ): Int = styledRes(
        when (wmoCode) {
            0 -> if (night) R.drawable.mc_clear_night else R.drawable.mc_clear_day
            1, 2 -> if (night) R.drawable.mc_partly_cloudy_night else R.drawable.mc_partly_cloudy_day
            3 -> R.drawable.mc_overcast
            45, 48 -> if (night) R.drawable.mc_fog_night else R.drawable.mc_fog_day
            51, 53, 55 -> R.drawable.mc_drizzle
            56, 57, 66, 67 -> R.drawable.mc_sleet
            61, 63, 65, 82 -> R.drawable.mc_rain
            71, 73, 75, 77 -> R.drawable.mc_snow
            80, 81 -> if (night) {
                R.drawable.mc_partly_cloudy_night_rain
            } else {
                R.drawable.mc_partly_cloudy_day_rain
            }
            85, 86 -> if (night) {
                R.drawable.mc_partly_cloudy_night_snow
            } else {
                R.drawable.mc_partly_cloudy_day_snow
            }
            95 -> R.drawable.mc_thunderstorms
            96, 99 -> R.drawable.mc_thunderstorms_rain
            else -> R.drawable.mc_cloudy
        },
        style
    )

    @Composable
    fun condition(wmoCode: Int, night: Boolean = false): ImageVector =
        ImageVector.vectorResource(conditionRes(wmoCode, night, LocalWeatherIcons.current))

    /** One drawing per [MoonPhase] name — the same classifier the report carries. */
    @DrawableRes
    fun moonPhaseRes(phase: MoonPhase, style: WeatherIcons = WeatherIcons.FILL): Int = styledRes(
        when (phase) {
            MoonPhase.NEW_MOON -> R.drawable.mc_moon_new
            MoonPhase.WAXING_CRESCENT -> R.drawable.mc_moon_waxing_crescent
            MoonPhase.FIRST_QUARTER -> R.drawable.mc_moon_first_quarter
            MoonPhase.WAXING_GIBBOUS -> R.drawable.mc_moon_waxing_gibbous
            MoonPhase.FULL_MOON -> R.drawable.mc_moon_full
            MoonPhase.WANING_GIBBOUS -> R.drawable.mc_moon_waning_gibbous
            MoonPhase.LAST_QUARTER -> R.drawable.mc_moon_last_quarter
            MoonPhase.WANING_CRESCENT -> R.drawable.mc_moon_waning_crescent
        },
        style
    )

    @Composable
    fun moonPhase(phase: MoonPhase): ImageVector =
        ImageVector.vectorResource(moonPhaseRes(phase, LocalWeatherIcons.current))

    /** The styled vector for a line id: the one seam every accessor below shares. */
    @Composable
    private fun styled(@DrawableRes lineRes: Int): ImageVector =
        ImageVector.vectorResource(styledRes(lineRes, LocalWeatherIcons.current))

    // The details grid. Each accessor names the METRIC, not the drawing, so a better
    // drawing later is a one-line change here and nothing else.
    val wind: ImageVector @Composable get() = styled(R.drawable.mc_wind)
    val humidity: ImageVector @Composable get() = styled(R.drawable.mc_humidity)
    val visibility: ImageVector @Composable get() = styled(R.drawable.mc_mist)
    val temperature: ImageVector @Composable get() = styled(R.drawable.mc_thermometer)
    val uv: ImageVector @Composable get() = styled(R.drawable.mc_uv_index)
    val pressure: ImageVector @Composable get() = styled(R.drawable.mc_barometer)
    val dewPoint: ImageVector @Composable get() = styled(R.drawable.mc_raindrop)
    val precipitation: ImageVector @Composable get() = styled(R.drawable.mc_raindrops)
    val airQuality: ImageVector @Composable get() = styled(R.drawable.mc_smoke_particles)

    /** Meteocons v2 has no pollen icon (v3 does, but it is a different drawing and
     * cannot be mixed in). Airborne grains are the dust icon's literal subject, so it
     * serves until the v3 family stabilizes — recorded in PLANNING.md Fase 2. */
    val pollen: ImageVector @Composable get() = styled(R.drawable.mc_dust)

    // The day's timeline and the Sky screen.
    /** The rain-over glyph: a cloud with nothing falling out of it. */
    val cloud: ImageVector @Composable get() = styled(R.drawable.mc_cloudy)
    val sunrise: ImageVector @Composable get() = styled(R.drawable.mc_sunrise)
    val sunset: ImageVector @Composable get() = styled(R.drawable.mc_sunset)
    val moonrise: ImageVector @Composable get() = styled(R.drawable.mc_moonrise)
    val moonset: ImageVector @Composable get() = styled(R.drawable.mc_moonset)
    val horizon: ImageVector @Composable get() = styled(R.drawable.mc_horizon)
    val star: ImageVector @Composable get() = styled(R.drawable.mc_star)
    val starryNight: ImageVector @Composable get() = styled(R.drawable.mc_starry_night)
    val fallingStars: ImageVector @Composable get() = styled(R.drawable.mc_falling_stars)

    // The navigation bar. Deliberately NOT styled (decision, 3 set): these are
    // silhouettes the bar tints to one color, so fill-vs-line would change nothing
    // visible, and the pair is calibrated to the Material bell beside them.
    val tabToday: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_tab_today)
    val tabSky: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_tab_sky)
}
