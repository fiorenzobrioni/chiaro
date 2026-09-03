package com.callbackdev.chiaro.ui.icons

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.model.MoonPhase

/**
 * The weather icon set, behind one lookup (DESIGN.md §4.5, §13.1): **Meteocons**
 * (github.com/basmilius/meteocons, MIT), imported as vector drawables by
 * `tools/import_meteocons.py` from the v2.0.0 line set, recolored so every stroke
 * clears 3:1 against both surfaces — the tool carries the measured table, and
 * `IconContrastTest` re-measures the emitted XML.
 *
 * The `*Res` functions are the actual mapping and stay plain functions on purpose:
 * they are unit-testable without Compose, and the Glance widgets (Fase 8) need
 * resource ids, not ImageVectors. The `@Composable` accessors are the same mapping
 * one `vectorResource` later.
 */
object ChiaroIcons {

    /**
     * The icon for a WMO weather code. [night] picks the nocturnal variant where one
     * exists — a clear night is not a sunny day, and that is the only place in the
     * mapping where the distinction changes anything.
     */
    @DrawableRes
    fun conditionRes(wmoCode: Int, night: Boolean = false): Int = when (wmoCode) {
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
    }

    @Composable
    fun condition(wmoCode: Int, night: Boolean = false): ImageVector =
        ImageVector.vectorResource(conditionRes(wmoCode, night))

    /** One drawing per [MoonPhase] name — the same classifier the report carries. */
    @DrawableRes
    fun moonPhaseRes(phase: MoonPhase): Int = when (phase) {
        MoonPhase.NEW_MOON -> R.drawable.mc_moon_new
        MoonPhase.WAXING_CRESCENT -> R.drawable.mc_moon_waxing_crescent
        MoonPhase.FIRST_QUARTER -> R.drawable.mc_moon_first_quarter
        MoonPhase.WAXING_GIBBOUS -> R.drawable.mc_moon_waxing_gibbous
        MoonPhase.FULL_MOON -> R.drawable.mc_moon_full
        MoonPhase.WANING_GIBBOUS -> R.drawable.mc_moon_waning_gibbous
        MoonPhase.LAST_QUARTER -> R.drawable.mc_moon_last_quarter
        MoonPhase.WANING_CRESCENT -> R.drawable.mc_moon_waning_crescent
    }

    @Composable
    fun moonPhase(phase: MoonPhase): ImageVector =
        ImageVector.vectorResource(moonPhaseRes(phase))

    // The details grid. Each accessor names the METRIC, not the drawing, so a better
    // drawing later is a one-line change here and nothing else.
    val wind: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.mc_wind)
    val humidity: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.mc_humidity)
    val visibility: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.mc_mist)
    val temperature: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.mc_thermometer)
    val uv: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.mc_uv_index)
    val pressure: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.mc_barometer)
    val dewPoint: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.mc_raindrop)
    val precipitation: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.mc_raindrops)
    val airQuality: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.mc_smoke_particles)

    /** Meteocons v2 has no pollen icon (v3 does, but it is a different drawing and
     * cannot be mixed in). Airborne grains are the dust icon's literal subject, so it
     * serves until the v3 family stabilizes — recorded in PLANNING.md Fase 2. */
    val pollen: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.mc_dust)

    // The day's timeline and the Sky screen.
    /** The rain-over glyph: a cloud with nothing falling out of it. */
    val cloud: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.mc_cloudy)
    val sunrise: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.mc_sunrise)
    val sunset: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.mc_sunset)
    val moonrise: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.mc_moonrise)
    val moonset: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.mc_moonset)
    val horizon: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.mc_horizon)
    val star: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.mc_star)
    val starryNight: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.mc_starry_night)
    val fallingStars: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.mc_falling_stars)

    // The navigation bar. Same glyphs as the family above, rescaled to the optical
    // size and stroke weight of the Material bell beside them: three tabs, one
    // apparent weight (device finding, 3 set).
    val tabToday: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_tab_today)
    val tabSky: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_tab_sky)
}
