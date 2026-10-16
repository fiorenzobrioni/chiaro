package com.callbackdev.chiaro.ui.today

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.settings.UnitSettings
import com.callbackdev.chiaro.ui.components.DaylightRibbon
import com.callbackdev.chiaro.ui.components.SkyBodies
import com.callbackdev.chiaro.ui.components.SkyBodiesLayer
import com.callbackdev.chiaro.ui.components.SkyCanvas
import com.callbackdev.chiaro.ui.components.SkySheet
import com.callbackdev.chiaro.ui.format.Formats
import com.callbackdev.chiaro.ui.shell.PageColumns
import com.callbackdev.chiaro.ui.theme.SkyGradient
import com.callbackdev.chiaro.ui.theme.SkyPalette
import com.callbackdev.chiaro.ui.theme.tabular
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * The canvas' height before the status bar's own height is added on top — a FLOOR since
 * 8 set 2026, not a size. Everything on the canvas is measured in sp (the 88sp
 * temperature, a 22sp sentence that runs to two lines in Italian) and the block was
 * measured in dp: at 100% type a two-line sentence left 2dp before the hero climbed into
 * the place row, and at 115% the two overlapped by 30dp. The canvas is now at least this
 * tall and grows with what it holds — see [CanvasHeader]. The place row is pinned over
 * the list rather than scrolling inside the canvas (18 set 2026), and the canvas holds a
 * spacer its height in its place, so this floor still measures the same block.
 *
 * 328 since 15 ott 2026: the figure went from a 68sp line to an 88sp one and the day's range
 * took a line of its own under the word, 20 + 28dp, and the floor (which the skeleton quotes)
 * grew by what the hero did.
 */
internal val CanvasBaseHeight = 328.dp

@Composable
internal fun CanvasHeader(
    content: TodayUiState.Content,
    gradient: SkyGradient,
    units: UnitSettings,
    timeFmt: DateTimeFormatter,
    locale: Locale,
    placeRowHeight: Dp,
    /** The travelling number's dock, or null where the figure stays on the sky (reduced motion). */
    dock: HeroDock? = null,
    /** How the paper under the canvas is laid out (§8.12c): the hero stands over its first pane. */
    columns: PageColumns? = null,
    /** A navigation rail over the canvas' start (§8.12c): the sky runs under it, nothing else. */
    railInset: Dp = 0.dp
) {
    // The canvas item's own coordinates, for where the number rests inside it.
    var canvasCoords by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
    val sky = content.sky
    val current = content.report.current
    // The canvas owns the top edge of the screen: its height grows by the status bar
    // so the sky sits behind the clock, over the top scrim that keeps both legible.
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val floor = CanvasBaseHeight + statusTop
    // The page laid over the sky's last few dp (DESIGN §8.1, 23 set 2026). Its lip is
    // inside the floor, not added to it: the skeleton quotes the floor, and the block
    // must still end where the skeleton's does.
    val sheet = SkySheet(surface = MaterialTheme.colorScheme.surface, start = railInset)
    // The scene over the gradient (DESIGN §3.9): the hour's clouds, its light, its stars, what
    // falls in it, moving with the wind while the weather may move — the reader's «weather that
    // moves», the system's animations, battery saver and a page at rest all have to agree.
    val palette = com.callbackdev.chiaro.ui.theme.ChiaroTheme.sky
    val scene = remember(sky, gradient, palette) {
        com.callbackdev.chiaro.ui.components.skyScene(
            palette = palette,
            gradient = gradient,
            sunAltitudeDeg = sky.sunAltitudeDeg,
            sunAzimuthDeg = sky.sunAzimuthDeg,
            moonAltitudeDeg = sky.moonAltitudeDeg,
            moonAzimuthDeg = sky.moonAzimuthDeg,
            moonIllumination = sky.moonIllumination,
            southern = sky.southern,
            cloudPct = sky.cloudPct,
            layers = sky.layers,
            precipPct = sky.precipPct,
            fall = sky.fall,
            fallMmPerHour = sky.fallMmPerHour,
            windKph = sky.windKph,
            windFromDeg = sky.windFromDeg
        )
    }
    val context = LocalContext.current
    val saving = remember(context) { com.callbackdev.chiaro.data.PowerSaveState.of(context) }
    val moving = com.callbackdev.chiaro.ui.icons.LocalAnimatedIcons.current &&
        !com.callbackdev.chiaro.ui.theme.reducedMotion() &&
        !com.callbackdev.chiaro.ui.icons.LocalMotionPaused.current &&
        !saving.isOn()
    SkyCanvas(
        gradient = gradient,
        minHeight = floor,
        sheet = sheet,
        scene = scene,
        moving = moving,
        modifier = Modifier.onGloballyPositioned { canvasCoords = it }
    ) {
        // One column with the place row's seat at the top and the hero at the bottom,
        // rather than two children aligned to opposite edges of a fixed box:
        // `SpaceBetween` on a floor keeps them apart when there is room and stacks them
        // when there is not, and a fixed box let them overlap instead.
        //
        // The seat and not the row: the row is pinned over the list now, and the sky is
        // still the thing it stands on. A spacer as tall as the real bar keeps the
        // geometry the row used to make for itself — the hero lands where it always did,
        // and it cannot climb under a bar it no longer belongs to.
        //
        // Between the two, since 23 set 2026, the sun and the moon, in a band of sky the
        // layout keeps for them ([SkyColumn]): the discs can never stand behind the place
        // row or the hero, and the band is never so thin that there is no room for one.
        Box(Modifier.padding(start = railInset)) {
        SkyColumn(seat = placeRowHeight, floor = floor - sheet.lip) {
            Box(modifier = Modifier.fillMaxSize().padding(top = 4.dp)) {
                SkyBodiesLayer(
                    SkyBodies(
                        sunAltitudeDeg = sky.sunAltitudeDeg,
                        sunAzimuthDeg = sky.sunAzimuthDeg,
                        moonAltitudeDeg = sky.moonAltitudeDeg,
                        moonAzimuthDeg = sky.moonAzimuthDeg,
                        moonIllumination = sky.moonIllumination,
                        moonElongationDeg = sky.moonElongationDeg,
                        cloudOpacity = scene.opacity,
                        southern = sky.southern
                    )
                )
            }
            // Three groups, set apart by the air between them rather than by one even step
            // (review of Today, 15 ott 2026): the NOW — the number, the sky's word, what it
            // feels like and whether it is an estimate — packed tight under the figure; the
            // DAY's sentence a step below it; the ribbon with its clock times closing the
            // canvas. With 6dp between every line the word and the sentence read as one
            // paragraph of three lines.
            //
            // Every line is [SkyPalette.Ink] at full strength (DESIGN §3.6): the secondary
            // ones are told apart by their size, never by a thinner white.
            // Over the first pane on a wide page (§8.12c), so the number, its word and the
            // day's sentence keep a line a reader can take in one look, and stand where the
            // paper's first column does.
            val heroStart = (columns?.start ?: railInset) - railInset
            val heroWidth = columns?.takeIf { it.twoPanes }?.pane ?: Dp.Infinity
            Column(
                modifier = Modifier
                    .padding(start = heroStart)
                    .widthIn(max = heroWidth)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Whole degrees (committente, 24 set 2026), like every other temperature
                // in the app. The tenths were the model's resolution, not its accuracy: a
                // 2 m temperature from a forecast model is good to a degree or two, and
                // since the same day the hero may be an estimate interpolated between two
                // hours — «20,8°» claimed a precision nobody had.
                //
                // With a [dock] the figure on screen is the travelling one (DESIGN §8.1e), drawn
                // over the page; this copy keeps the figure's place and size in the column, is
                // what TalkBack reads, and tells the dock where the figure rests.
                Text(
                    text = Formats.temperature(current.tempC, units.temperature, locale),
                    style = heroStyle(),
                    color = SkyPalette.Ink,
                    modifier = if (dock == null) {
                        Modifier
                    } else {
                        // Hidden only once the travelling figure knows where to stand, so the
                        // first frame never has no number at all.
                        Modifier
                            .alpha(if (dock.measured) 0f else 1f)
                            .onGloballyPositioned { number ->
                                val item = canvasCoords ?: return@onGloballyPositioned
                                if (!item.isAttached || !number.isAttached) return@onGloballyPositioned
                                dock.rest = item.localPositionOf(number, Offset.Zero)
                                dock.size = number.size
                            }
                    }
                )
                // The condition is `titleLarge` since the review of 23 set 2026: from the
                // hero straight down to 16sp was a cliff, and the word that says what the sky
                // is doing is the second thing a reader wants. No space over it: the figure's
                // own line already leaves air under its digits.
                Text(
                    text = stringResource(WeatherText.condition(current.condition.wmoCode)),
                    style = MaterialTheme.typography.titleLarge,
                    color = SkyPalette.Ink
                )
                // The day's range under the word (15 ott 2026), the number a reader looks for
                // right after the one above: it was only in the week's first row, two screens
                // down. The feels-like joins it only when it says something (§1.2): "21° /
                // feels like 21°" is a number with nothing to do about it, and a degree apart
                // is where a body starts to notice.
                val day = content.week.firstOrNull { it.forecast.date == content.now.toLocalDate() }?.forecast
                val figures = listOfNotNull(
                    day?.let {
                        stringResource(
                            R.string.hero_high_low,
                            Formats.temperature(it.highC, units.temperature, locale),
                            Formats.temperature(it.lowC, units.temperature, locale)
                        )
                    },
                    if (kotlin.math.abs(current.feelsLikeC - current.tempC) >= FeelsLikeMinDeltaC) {
                        stringResource(
                            R.string.feels_like,
                            Formats.temperature(current.feelsLikeC, units.temperature, locale)
                        )
                    } else {
                        null
                    }
                )
                if (figures.isNotEmpty()) {
                    Text(
                        // One sentence of figures: only its first word keeps its capital.
                        text = figures.mapIndexed { i, figure ->
                            if (i == 0) figure else figure.replaceFirstChar { it.lowercase(locale) }
                        }.joinToString(" · "),
                        style = MaterialTheme.typography.bodyLarge.tabular(),
                        color = SkyPalette.Ink,
                        modifier = Modifier.padding(top = HeroLineGap)
                    )
                }
                // A now rebuilt from the forecast says so under its own numbers
                // (DESIGN §1.1, 24 set 2026): the chip above says how old the data is,
                // this says what the hero did about it.
                if (current.estimated) {
                    Text(
                        text = stringResource(R.string.estimate_hero),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SkyPalette.Ink,
                        modifier = Modifier.padding(top = HeroLineGap)
                    )
                }
                // The sentence before the ribbon (review of 12 ott 2026): it is the second
                // thing the hero says, under the word for the sky, and the ribbon with its two
                // clock times closes the canvas as its floor line.
                //
                // Prose at `bodyLarge` since the review of 15 ott 2026. At `titleLarge` it
                // wore the sky's word's own size and weight, so two lines about the day
                // outweighed the one word about now; set as a sentence it reads as one, and
                // the order the eye takes is the order of the screen — the number, the word,
                // the day.
                headlineText(content.headline, timeFmt, units)?.let { sentence ->
                    Text(
                        text = sentence,
                        style = MaterialTheme.typography.bodyLarge,
                        color = SkyPalette.Ink,
                        modifier = Modifier.padding(top = HeroGroupGap)
                    )
                }
                DaylightRibbon(
                    phases = sky.phases,
                    nowFraction = sky.nowFraction,
                    description = ribbonDescription(content, timeFmt),
                    // What is left of today wears its forecast sky (DESIGN §4, 12 ott
                    // 2026); what is already spent stays light alone, dimmed as before.
                    hours = sky.hours,
                    skyFrom = sky.nowFraction,
                    modifier = Modifier.padding(top = HeroRibbonGap)
                )
                ribbonTimes(content, timeFmt).takeIf { it.isNotEmpty() }?.let { times ->
                    RibbonTimes(
                        times = times,
                        color = SkyPalette.Ink,
                        modifier = Modifier.padding(top = HeroLineGap)
                    )
                }
            }
        }
        }
    }
}

/**
 * The canvas' column (23 set 2026): the place row's seat, the band of sky the sun and the
 * moon are drawn in, and the hero, top to bottom. It was a `Column` with `SpaceBetween`,
 * which left the sky whatever the floor had over — 14dp on a page with the place's own
 * hour, which is no room for a sun. Now the band is **at least [BodiesBand]**, more when
 * the floor leaves more, and the canvas grows by what that costs: the sky is the hero of
 * this screen, and a hero with no sky in it is a temperature on a gradient.
 *
 * Children: the band's content, then the hero.
 */
@Composable
internal fun SkyColumn(seat: Dp, floor: Dp, content: @Composable () -> Unit) {
    androidx.compose.ui.layout.Layout(content = content) { measurables, constraints ->
        val width = constraints.maxWidth
        // Up to the full width and not exactly it: the hero fills it itself, and a wide page
        // narrows it to its first pane (§8.12c).
        val hero = measurables[1].measure(androidx.compose.ui.unit.Constraints(maxWidth = width))
        val seatPx = seat.roundToPx()
        val band = maxOf(BodiesBand.roundToPx(), floor.roundToPx() - seatPx - hero.height)
        val bodies = measurables[0].measure(androidx.compose.ui.unit.Constraints.fixed(width, band))
        layout(width, seatPx + band + hero.height) {
            bodies.place(0, seatPx)
            hero.place(0, seatPx + band)
        }
    }
}

/** The least sky the canvas keeps between the place row and the hero: a 22dp disc and
 * room for it to climb. */
internal val BodiesBand = 56.dp

/** Between two lines of one group on the canvas: the estimate's note under the word, the
 * clock times under the ribbon. */
internal val HeroLineGap = 4.dp

/** Between the now and the day's sentence: a step, so the two read as two things. */
internal val HeroGroupGap = 12.dp

/** Over the ribbon: the canvas' floor line stands clear of the prose above it. */
internal val HeroRibbonGap = 16.dp

/**
 * The hero's style at the reader's type scale (15 ott 2026): the figure grows with the reader's
 * type up to [HeroScaleCeiling] and no further. At 88sp a 200% scale would set «−12°» some 400dp
 * wide on a 360dp phone; the figure is already the largest thing on the screen, and what a
 * larger type is for — the words — still grows in full beside it.
 */
@Composable
internal fun heroStyle(): TextStyle {
    val base = com.callbackdev.chiaro.ui.theme.LocalChiaroType.current.heroTemperature
    val scale = LocalDensity.current.fontScale
    val capped = heroFontScale(scale)
    if (capped == scale) return base
    return base.copy(
        fontSize = (base.fontSize.value * capped / scale).sp,
        lineHeight = (base.lineHeight.value * capped / scale).sp
    )
}

/** The scale the hero figure actually grows by: the reader's, up to [HeroScaleCeiling]. */
internal fun heroFontScale(fontScale: Float): Float = fontScale.coerceAtMost(HeroScaleCeiling)

internal const val HeroScaleCeiling = 1.3f

/** Below this the feels-like line is not drawn: the air feels like what it is. */
internal const val FeelsLikeMinDeltaC = 1.0

/** The sentence, or nothing: [HeadlineText]'s mapping, shared with the Today widget. */
@Composable
internal fun headlineText(
    headline: Headline?,
    timeFmt: DateTimeFormatter,
    units: UnitSettings
): String? = HeadlineText.of(LocalContext.current, headline, timeFmt, units)

@Composable
internal fun ribbonDescription(
    content: TodayUiState.Content,
    timeFmt: DateTimeFormatter
): String {
    val astro = content.report.astronomical
    return when {
        astro.sunrise != null && astro.sunset != null -> stringResource(
            R.string.ribbon_desc, astro.sunrise!!.format(timeFmt), astro.sunset!!.format(timeFmt)
        )
        astro.daylightDuration == null && astro.sunrise == null && astro.sunset == null ->
            stringResource(R.string.ribbon_desc_all_night)
        else -> stringResource(R.string.ribbon_desc_all_day)
    }
}

/**
 * The ribbon's two clock times (review of 12 ott 2026): sunrise and sunset, each under the
 * point of the band where it falls. Until then the band was a strip of colors that could
 * be read only by somebody who already knew it: the blue in the middle is the day, the
 * amber flecks are the sun's ends. The same two times the ribbon's description reads, so
 * TalkBack hears them once, from the ribbon, and the labels are cleared from semantics.
 * A day with no sunrise or no sunset draws neither: one time on its own is half a span.
 */
@Composable
internal fun ribbonTimes(
    content: TodayUiState.Content,
    timeFmt: DateTimeFormatter
): List<Pair<Float, String>> {
    val astro = content.report.astronomical
    val rise = astro.sunrise ?: return emptyList()
    val set = astro.sunset ?: return emptyList()
    fun at(time: java.time.LocalTime) = time.toSecondOfDay() / 86_400f
    return listOf(at(rise) to rise.format(timeFmt), at(set) to set.format(timeFmt))
}

/** Labels placed under the fractions of the width they name, centred there and kept
 * inside the row. */
@Composable
internal fun RibbonTimes(times: List<Pair<Float, String>>, color: Color, modifier: Modifier = Modifier) {
    androidx.compose.ui.layout.Layout(
        content = {
            times.forEach { (_, text) ->
                Text(text = text, style = MaterialTheme.typography.labelSmall.tabular(), color = color)
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { }
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0)) }
        val width = constraints.maxWidth
        layout(width, placeables.maxOfOrNull { it.height } ?: 0) {
            placeables.forEachIndexed { i, label ->
                val x = (times[i].first * width - label.width / 2f).roundToInt()
                    .coerceIn(0, (width - label.width).coerceAtLeast(0))
                label.place(x, 0)
            }
        }
    }
}
