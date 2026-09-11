package com.callbackdev.chiaro.ui.icons

import androidx.annotation.DrawableRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.data.WeatherIcons
import com.callbackdev.chiaro.domain.model.MoonPhase
import com.callbackdev.chiaro.domain.model.PollenLevel
import com.callbackdev.chiaro.domain.model.PollenReport
import com.callbackdev.chiaro.domain.sky.SkyVerdictKind
import com.callbackdev.chiaro.ui.components.VerdictKind
import com.callbackdev.chiaro.ui.today.WeatherText

/**
 * The reader's icon style, provided by `MainActivity` from the settings alongside the
 * theme. LINE is the default a fresh install sees (6 set 2026); the accessors below read
 * this so every screen switches together, with no screen ever asked to care.
 *
 * `WeatherIcons.FILL` means Meteocons' **`flat`** since Fase 13, and the enum kept its
 * name on purpose: `flat` is what this app already shipped as its filled set — the v2
 * `fill` with every gradient flattened to its face color by the importer. Renaming the
 * constant would migrate a stored preference and rewrite a settings string, both to
 * describe the same drawing.
 */
val LocalWeatherIcons = staticCompositionLocalOf { WeatherIcons.LINE }

/**
 * The weather icon set, behind one lookup (DESIGN.md §4.5, §13.1): **Meteocons v3**
 * (github.com/basmilius/meteocons, MIT), imported as vector drawables by
 * `tools/import_meteocons_v3.py` from the `line` and `flat` styles and re-anchored by
 * `tools/reanchor.py` for the ground each set is picked for. The tables are generated
 * into [MeteoconsSets] from `tools/shipped_icons.py`; the policy — which weather code
 * gets which drawing, which metric gets which mark — is here, by hand, because it is
 * the part a person has to argue about.
 *
 * **Four sets, chosen by style and ground, and the dress no longer comes into it.**
 * Until Fase 13 the line set owed BOTH surfaces at once, which pinned every color into
 * Y ∈ [0.120, 0.283] and is why its sun was a bronze; the vivid palette got a second
 * line set to escape that ceiling on dark grounds. v3 ships a set per ground, so each
 * one only ever meets the surface it was measured against, the ceiling is gone on dark
 * (the sun is a real gold there), and `AppPalette` has nothing left to decide about an
 * icon. That is why these functions no longer take one.
 *
 * The `*Res` functions stay plain functions on purpose: they are unit-testable without
 * Compose, and the Glance widgets need resource ids, not ImageVectors — which is also
 * why they take the style as a parameter, while the `@Composable` accessors read
 * [LocalWeatherIcons].
 */
object ChiaroIcons {

    /**
     * The style applied to a line resource id: the sibling for the style the reader
     * chose and the ground the icon will sit on. [darkGround] is the applied theme in
     * the app and the card's own ground in a widget (WidgetPalette).
     *
     * `getValue` and not `get`: a drawing that reached a screen without being in the
     * shipping list is a loud failure in a test, never a silent blank.
     */
    @DrawableRes
    fun styledRes(
        @DrawableRes lineRes: Int,
        style: WeatherIcons,
        darkGround: Boolean = false
    ): Int = when {
        style == WeatherIcons.FILL && darkGround -> MeteoconsSets.flatDarkOf.getValue(lineRes)
        style == WeatherIcons.FILL -> MeteoconsSets.flatOf.getValue(lineRes)
        darkGround -> MeteoconsSets.lineDarkOf.getValue(lineRes)
        else -> lineRes
    }

    /**
     * The moving sibling of a static drawing, or **null** when this family has none —
     * which is the honest answer for the metric marks and for "not available", and the
     * signal the caller needs to fall back to the still one rather than to invent
     * motion.
     */
    @DrawableRes
    fun movingRes(
        @DrawableRes lineRes: Int,
        style: WeatherIcons,
        darkGround: Boolean = false
    ): Int? = MeteoconsSets.movingOf[lineRes]?.let {
        when {
            style == WeatherIcons.FILL && darkGround -> it.flatDark
            style == WeatherIcons.FILL -> it.flat
            darkGround -> it.lineDark
            else -> it.line
        }
    }

    /**
     * The line drawing for a WMO code, before any style or ground is applied: the seam
     * [movingRes] and [styledRes] are both keyed on.
     *
     * **Codes 1 and 2 are different drawings again** (Fase 13). They had shared one
     * since Fase 2, because Meteocons v2 had no "mostly clear" and the nearest thing was
     * the partly-cloudy sky; measured on 1 680 hours, code 1 carries a median 25% of
     * cloud and code 2 a median 64%, and the two buckets do not overlap between their
     * tenth and ninetieth percentiles. One hour in six was drawn half again cloudier
     * than forecast, under a word (`cond_mostly_clear`) that said otherwise.
     *
     * Rain, drizzle and snow take their cloud with them (`overcast-*`) rather than
     * falling out of nothing: from the same hours, when it rains the sky IS closed —
     * code 51 never below 88% of cover, code 61 never below 87%. Showers keep the
     * partly-cloudy sky, because a shower is the sky that lets the sun back between two
     * of them.
     *
     * An unrecognised code draws **`not-available`**, not a cloud. The word beside it
     * already says "unknown conditions", and a cloud there would be the screen
     * inventing weather nobody forecast.
     */
    @DrawableRes
    fun conditionLineRes(wmoCode: Int, night: Boolean = false): Int = when (wmoCode) {
        0 -> if (night) R.drawable.mc3_clear_night else R.drawable.mc3_clear_day
        1 -> if (night) R.drawable.mc3_mostly_clear_night else R.drawable.mc3_mostly_clear_day
        2 -> if (night) R.drawable.mc3_partly_cloudy_night else R.drawable.mc3_partly_cloudy_day
        3 -> R.drawable.mc3_overcast
        45, 48 -> if (night) R.drawable.mc3_fog_night else R.drawable.mc3_fog_day
        51, 53, 55 -> R.drawable.mc3_overcast_drizzle
        56, 57, 66, 67 -> R.drawable.mc3_overcast_sleet
        61, 63, 65 -> R.drawable.mc3_overcast_rain
        71, 73, 75, 77 -> R.drawable.mc3_overcast_snow
        80, 81 -> if (night) {
            R.drawable.mc3_partly_cloudy_night_rain
        } else {
            R.drawable.mc3_partly_cloudy_day_rain
        }
        82 -> R.drawable.mc3_extreme_rain
        85, 86 -> if (night) {
            R.drawable.mc3_partly_cloudy_night_snow
        } else {
            R.drawable.mc3_partly_cloudy_day_snow
        }
        95 -> if (night) R.drawable.mc3_thunderstorms_night else R.drawable.mc3_thunderstorms_day
        96, 99 -> if (night) {
            R.drawable.mc3_thunderstorms_night_hail
        } else {
            R.drawable.mc3_thunderstorms_day_hail
        }
        else -> R.drawable.mc3_not_available
    }

    /**
     * The icon for a WMO code. [night] picks the nocturnal variant where one exists — a
     * clear night is not a sunny day, and that is the only place in the mapping where
     * the distinction changes anything.
     */
    @DrawableRes
    fun conditionRes(
        wmoCode: Int,
        night: Boolean = false,
        style: WeatherIcons = WeatherIcons.LINE,
        darkGround: Boolean = false
    ): Int = styledRes(conditionLineRes(wmoCode, night), style, darkGround)

    @Composable
    fun condition(wmoCode: Int, night: Boolean = false): ImageVector =
        ImageVector.vectorResource(
            conditionRes(wmoCode, night, LocalWeatherIcons.current, darkGround())
        )

    /** One drawing per [MoonPhase] name — the same classifier the report carries. */
    @DrawableRes
    fun moonPhaseRes(
        phase: MoonPhase,
        style: WeatherIcons = WeatherIcons.LINE,
        darkGround: Boolean = false
    ): Int = styledRes(
        when (phase) {
            MoonPhase.NEW_MOON -> R.drawable.mc3_moon_new
            MoonPhase.WAXING_CRESCENT -> R.drawable.mc3_moon_waxing_crescent
            MoonPhase.FIRST_QUARTER -> R.drawable.mc3_moon_first_quarter
            MoonPhase.WAXING_GIBBOUS -> R.drawable.mc3_moon_waxing_gibbous
            MoonPhase.FULL_MOON -> R.drawable.mc3_moon_full
            MoonPhase.WANING_GIBBOUS -> R.drawable.mc3_moon_waning_gibbous
            MoonPhase.LAST_QUARTER -> R.drawable.mc3_moon_last_quarter
            MoonPhase.WANING_CRESCENT -> R.drawable.mc3_moon_waning_crescent
        },
        style,
        darkGround
    )

    @Composable
    fun moonPhase(phase: MoonPhase): ImageVector =
        ImageVector.vectorResource(
            moonPhaseRes(phase, LocalWeatherIcons.current, darkGround())
        )

    /**
     * The verdict's mark as a drawing (9 set 2026): the series' `✓ ~ ✗ ?`, one path each,
     * at one line weight, tinted with the verdict's ink wherever it is shown. They were
     * characters until a device pass read the X as handwriting — U+2713 and U+2717 are not
     * in Roboto, and the phone draws them from a symbol fallback font in a hand of its own
     * (calligraphic on One UI, another on a Pixel). A drawing is the same shape on every
     * phone, and the shape is the point: a verdict is a glyph and a word before it is a
     * color (DESIGN §2.3, §8.7). One drawing per verdict, never two on one shape.
     */
    @DrawableRes
    fun verdictMarkRes(kind: VerdictKind): Int = when (kind) {
        VerdictKind.PASS -> R.drawable.ic_verdict_pass
        VerdictKind.UNSTABLE -> R.drawable.ic_verdict_unstable
        VerdictKind.FAIL -> R.drawable.ic_verdict_fail
        VerdictKind.UNKNOWN -> R.drawable.ic_verdict_unknown
    }

    /** The same, for the domain's own kind — the widgets hold verdicts, not chip kinds. */
    @DrawableRes
    fun verdictMarkRes(kind: SkyVerdictKind): Int = verdictMarkRes(
        when (kind) {
            SkyVerdictKind.PASS -> VerdictKind.PASS
            SkyVerdictKind.UNSTABLE -> VerdictKind.UNSTABLE
            SkyVerdictKind.FAIL -> VerdictKind.FAIL
            SkyVerdictKind.UNKNOWN -> VerdictKind.UNKNOWN
        }
    )

    /**
     * The official warning's mark (Fase 11, DESIGN §8.13): one drawing, at the verdict
     * marks' weight, for every surface that carries a level — the banner, the sheet, the
     * card in Avvisi, the Journal's line and the widgets' chip. It is tinted with the
     * level's ink and never recoloured, and it is a drawing for the reason the verdict
     * marks are: ⚠ is not in the app's face.
     *
     * Meteocons v3 HAS `code-yellow`/`code-orange`/`code-red`, and they are deliberately
     * not used here: they are full-colour illustrations, and this slot wants a mark at
     * the verdict weight that takes the level's ink. The family's `*-alert` drawings are
     * imported for the **kind** of hazard instead, which is a different question.
     */
    @DrawableRes
    fun warningMarkRes(): Int = R.drawable.ic_warning

    /** The same mark as a vector, for the app's own screens. */
    val warning: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_warning)

    /** The styled vector for a line id: the one seam every accessor below shares. */
    @Composable
    private fun styled(@DrawableRes lineRes: Int): ImageVector =
        ImageVector.vectorResource(styledRes(lineRes, LocalWeatherIcons.current, darkGround()))

    /** The ground the icon is about to sit on: the APPLIED theme's surface, read off
     * the scheme itself — a reader can force the theme against the system, and the
     * icon must follow the choice the way the status bar does. */
    @Composable
    private fun darkGround(): Boolean =
        MaterialTheme.colorScheme.surface.luminance() < 0.5f

    // The details grid. Each accessor names the METRIC, not the drawing, so a better
    // drawing later is a one-line change here and nothing else.
    val wind: ImageVector @Composable get() = styled(R.drawable.mc3_wind)
    val humidity: ImageVector @Composable get() = styled(R.drawable.mc3_humidity)
    val visibility: ImageVector @Composable get() = styled(R.drawable.mc3_mist)
    val uv: ImageVector @Composable get() = styled(R.drawable.mc3_uv_index)

    /**
     * The graded marks (Fase 13). Meteocons v3 draws these metrics at their own levels,
     * and the rule for using one is the same rule a second verdict has to pass
     * (DESIGN §1.2): **a glyph may only say a level the tile already computes and says in
     * words.** Where the family's grades and the app's bands do not line up, the generic
     * mark stays — that is why the wind tile below is not graded, and why `very-high`
     * and `extreme` are imported but not shipped: `pressureMeaning` has three bands and
     * `pollenLevel` has three levels above nothing, not five and four.
     */
    @Composable
    fun uv(index: Int): ImageVector = styled(
        when {
            index <= 0 -> R.drawable.mc3_uv_index
            index >= 12 -> R.drawable.mc3_uv_index_11_plus
            else -> UV_GRADES[index - 1]
        }
    )

    /**
     * The UV mark carries the number, so it may be graded per unit rather than per band:
     * the tile prints "7" and the drawing says 7. A band-graded glyph beside a printed
     * integer would be the picture and the number disagreeing about how precise the
     * forecast is.
     */
    private val UV_GRADES = intArrayOf(
        R.drawable.mc3_uv_index_1, R.drawable.mc3_uv_index_2, R.drawable.mc3_uv_index_3,
        R.drawable.mc3_uv_index_4, R.drawable.mc3_uv_index_5, R.drawable.mc3_uv_index_6,
        R.drawable.mc3_uv_index_7, R.drawable.mc3_uv_index_8, R.drawable.mc3_uv_index_9,
        R.drawable.mc3_uv_index_10, R.drawable.mc3_uv_index_11
    )

    /**
     * The pressure mark is **not** graded, and the reason is the rule's missing half
     * (11 set 2026, from a device report: «non ha indicazione»).
     *
     * Meteocons draws five barometers and the app has three bands, so the arithmetic
     * lined up — but the thing that grades them is a needle **2 units wide in a 128-unit
     * box**, which at the tile's 34dp is half a device-independent pixel. The grade was
     * arithmetically right and optically absent: a dial that looks like it should be
     * pointing at something and is not. §1.2's rule needed the other half, now written
     * down: a glyph may only say a level the tile already says in words **and that a
     * reader can actually see**. The band is in «Nella norma», where it reads.
     */
    val pressure: ImageVector @Composable get() = styled(R.drawable.mc3_barometer)

    /**
     * The air between the reader and the horizon, at the strength the tile names.
     * `visibilityMeaning`'s two hazy bands share one drawing on purpose: they are the
     * same phenomenon at two strengths and the words already tell them apart, and a
     * glyph that under-claims is honest where one that over-claims is not.
     */
    @Composable
    fun visibility(kilometres: Double): ImageVector = styled(
        when {
            kilometres >= 10 -> R.drawable.mc3_mist
            kilometres >= 1 -> R.drawable.mc3_haze
            else -> R.drawable.mc3_fog
        }
    )

    /** The thermometer, not the raindrop (4 set 2026). Meteocons draws `humidity` as
     * `raindrop` with a % laid over it, so the two tiles that sit one under the other
     * in the details grid were the same drawing twice — told apart only by a white
     * glyph the reader has to look for, and not at all once anything tints them flat.
     * A dew point is a temperature, so it gets the instrument that reads one; the
     * drop stays the humidity mark alone. This is what §13.1's «the accessor names
     * the metric, not the drawing» is for. */
    val dewPoint: ImageVector @Composable get() = styled(R.drawable.mc3_thermometer)

    val precipitation: ImageVector @Composable get() = styled(R.drawable.mc3_raindrops)

    /** Freezing, not snow: the Journal's drift strip marks the days whose forecast
     * minimum is at or below zero, and the question there is ice, not precipitation.
     * The accessor names the metric (§13.1), which is why it is not called
     * `snowflake`. */
    val frost: ImageVector @Composable get() = styled(R.drawable.mc3_snowflake)
    val airQuality: ImageVector @Composable get() = styled(R.drawable.mc3_smoke_particles)

    /**
     * A real pollen drawing at last (Fase 13). It was `dust` from Fase 2 to here, with
     * a note saying so: Meteocons v2 had no pollen icon and airborne grains are the
     * dust icon's literal subject, which made it an honest stand-in rather than a lie.
     * v3 has the family, in three plants and four levels, and `WeatherText.pollenWorst`
     * already computes exactly that — so the tile can name the plant instead of the
     * dust in the air.
     */
    val pollen: ImageVector @Composable get() = styled(R.drawable.mc3_pollen)

    /**
     * The plant, at its level. The tile already computes both — `pollenWorst` for the
     * level and `pollenFamiliesAtWorst` for who is at it — and already prints them, so
     * the drawing is allowed to say the same thing.
     *
     * Ties go to the catalogue's order, the same order the note lists them in, so the
     * glyph names the first plant the sentence names. `NONE` keeps the generic mark:
     * there is no "no pollen" drawing, and inventing one would be a level the tile does
     * not have.
     */
    @Composable
    fun pollen(report: PollenReport): ImageVector {
        val worst = WeatherText.pollenWorst(report)
        val family = when (worst) {
            PollenLevel.NONE -> null
            report.grass -> Plant.GRASS
            report.tree -> Plant.TREE
            else -> Plant.WEED
        }
        return styled(
            when {
                family == null -> R.drawable.mc3_pollen
                worst == PollenLevel.LOW -> family.low
                worst == PollenLevel.MODERATE -> family.moderate
                else -> family.high
            }
        )
    }

    private enum class Plant(
        @DrawableRes val low: Int,
        @DrawableRes val moderate: Int,
        @DrawableRes val high: Int
    ) {
        GRASS(
            R.drawable.mc3_pollen_grass_low, R.drawable.mc3_pollen_grass_moderate,
            R.drawable.mc3_pollen_grass_high
        ),
        TREE(
            R.drawable.mc3_pollen_tree_low, R.drawable.mc3_pollen_tree_moderate,
            R.drawable.mc3_pollen_tree_high
        ),
        WEED(
            R.drawable.mc3_pollen_weed_low, R.drawable.mc3_pollen_weed_moderate,
            R.drawable.mc3_pollen_weed_high
        )
    }

    // The day's timeline and the Sky screen.
    /** The rain-over glyph: a cloud with nothing falling out of it. */
    val cloud: ImageVector @Composable get() = styled(R.drawable.mc3_cloudy)
    val sunrise: ImageVector @Composable get() = styled(R.drawable.mc3_sunrise)
    val sunset: ImageVector @Composable get() = styled(R.drawable.mc3_sunset)
    val moonrise: ImageVector @Composable get() = styled(R.drawable.mc3_moonrise)
    val moonset: ImageVector @Composable get() = styled(R.drawable.mc3_moonset)
    val horizon: ImageVector @Composable get() = styled(R.drawable.mc3_horizon)
    val star: ImageVector @Composable get() = styled(R.drawable.mc3_star)
    val starryNight: ImageVector @Composable get() = styled(R.drawable.mc3_starry_night)
    val fallingStars: ImageVector @Composable get() = styled(R.drawable.mc3_falling_stars)

    /** The moon over the sun: the one eclipse drawing the family has (Fase 19). */
    val solarEclipse: ImageVector @Composable get() = styled(R.drawable.mc3_solar_eclipse)

    /**
     * An actual rainbow (Fase 13). Until v3 this was `partly-cloudy-day-rain` under a
     * note admitting it was not a substitute — it is literally the weather a rainbow is
     * made of, which is what the row it marks says, but it is not the thing.
     */
    val rainbow: ImageVector @Composable get() = styled(R.drawable.mc3_rainbow)

    // The navigation bar. Deliberately NOT styled (decision, 3 set): these are
    // silhouettes the bar tints to one color, so fill-vs-line would change nothing
    // visible, and the pair is calibrated to the Material bell beside them.
    val tabToday: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_tab_today)
    val tabSky: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_tab_sky)
}
