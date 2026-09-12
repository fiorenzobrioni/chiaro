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
     * The face of a drawing in one of the four sets: the composed drawings first, the
     * imported family after.
     *
     * Two tables and not one because only one of them is rewritten by
     * `tools/import_meteocons_v3.py` on every run; [ComposedIcons] is written by
     * `tools/compose_sun_cloud.py` and has to survive that. The order matters in one
     * direction only — no name is in both — so a composed drawing is found first and an
     * imported one falls through.
     *
     * `getValue` on the family and not `get`: a drawing that reached a screen without
     * being in the shipping list is a loud failure in a test, never a silent blank.
     */
    @DrawableRes
    private fun faceOf(
        @DrawableRes lineRes: Int,
        composed: Map<Int, Int>,
        family: Map<Int, Int>
    ): Int = composed[lineRes] ?: family.getValue(lineRes)

    /**
     * The style applied to a line resource id: the sibling for the style the reader
     * chose and the ground the icon will sit on. [darkGround] is the applied theme in
     * the app and the card's own ground in a widget (WidgetPalette).
     */
    @DrawableRes
    fun styledRes(
        @DrawableRes lineRes: Int,
        style: WeatherIcons,
        darkGround: Boolean = false
    ): Int = when {
        style == WeatherIcons.FILL && darkGround ->
            faceOf(lineRes, ComposedIcons.flatDarkOf, MeteoconsSets.flatDarkOf)
        style == WeatherIcons.FILL ->
            faceOf(lineRes, ComposedIcons.flatOf, MeteoconsSets.flatOf)
        darkGround ->
            faceOf(lineRes, ComposedIcons.lineDarkOf, MeteoconsSets.lineDarkOf)
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
    ): Int? = (ComposedIcons.movingOf[lineRes] ?: MeteoconsSets.movingOf[lineRes])?.let {
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
     * **Codes 0, 1 and 2 are three different drawings** (12 set 2026), and it took three
     * goes. They shared one until Fase 13, which was the defect that opened the phase:
     * measured on 1 680 hours, code 1 carries a median 25% of cloud against code 2's 64%,
     * two buckets that do not overlap between their tenth and ninetieth percentiles. One
     * hour in six was drawn half again cloudier than forecast, under a word
     * (`cond_mostly_clear`) that said otherwise.
     *
     * Fase 13 gave code 1 Meteocons' own `mostly-clear`, and that was wrong the other way
     * round: measured in the source its cloud is **56 units of 128 against
     * partly-cloudy's 80** — 70% of the cloud for a sky that carries 39% of the cover —
     * and its sun shrinks from a 36-unit disc to 23, while at a quarter of cover the sun
     * is fully out. So on 11 set 2026 code 1 was given the plain sun instead: honest in
     * the words, mute in the hour strip and the week row, where there are none. That cost
     * was written down rather than waved past — 0 and 1 drew the same sky for 17,2% of
     * hours — and it is the reason this came back.
     *
     * **Code 1 now takes `sun-one-cloud`, which this repo composes rather than imports**
     * (committente, 12 set 2026; the recipe and its measurements are in the head of
     * `tools/compose_sun_cloud.py`). It is `clear-day` untouched — the same paths, the
     * same 0,92 scale, the same place — plus `cloudy`'s silhouette at 48,88% in the
     * bottom-right corner, cut out of the sun by the very mask `partly-cloudy` already
     * uses. Measured: the cloud is **43,1 units against partly-cloudy's 99,2** (43%,
     * where the shelved `mostly-clear` sits at 72%), the air between cloud and rays is
     * 1,80 to 2,45 against Meteocons' own 2,48, and the sun keeps seven rays of eight —
     * the south-east one stands behind the cloud. Between 0 and 1 exactly one thing
     * changes, and it is the thing that changes in the sky.
     *
     * The imported `mostly-clear` stays in the repo, unused, for the day the choice is
     * revisited again. Most apps collapse 0 and 1 for a poorer reason, having no such
     * drawing at all: Home Assistant's Open-Meteo integration maps both to `sunny`.
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
        1 -> if (night) ComposedIcons.mostlyClearNight else ComposedIcons.mostlyClearDay
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
    /**
     * `smoke`, not `smoke-particles` (11 set 2026, committente). Meteocons draws the
     * particles alone as three specks that fill a third of their box — the smallest mark
     * in the grid, and mute beside the sun and the grass. `smoke` is the same particles
     * with the air they hang in, twice the ink, and it is what an air-quality reading is
     * about. The one cost is declared: when visibility drops into its hazy band the tile
     * beside this one draws a cloud with lines while this one draws a cloud with dots,
     * and at 34dp those are close. Below 10km, and the words differ.
     */
    val airQuality: ImageVector @Composable get() = styled(R.drawable.mc3_smoke)

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

    /**
     * The golden hour, and it is **not** the horizon glyph any more (11 set 2026, from a
     * screenshot): `sunrise`, `horizon` and `sunset` are the same drawing apart from a
     * bump in the middle of the line, **6 units in a 128 box** — 1.6dp at the agenda's
     * 34dp — so «Ora d'oro» at 19:02 and «Tramonto» at 19:41 were two rows carrying one
     * picture. Meteocons has no golden-hour drawing, but the plain sun says the thing
     * that actually separates them: in the golden hour the sun is still **above** the
     * horizon, at sunrise and sunset it is crossing it. Whoever crosses keeps the line.
     */
    val goldenHour: ImageVector @Composable get() = styled(R.drawable.mc3_clear_day)
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
