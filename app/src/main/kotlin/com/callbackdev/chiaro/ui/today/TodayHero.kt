package com.callbackdev.chiaro.ui.today

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
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
import com.callbackdev.chiaro.ui.theme.SkyGradient
import com.callbackdev.chiaro.ui.theme.SkyPalette
import com.callbackdev.chiaro.ui.theme.tabular
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * The canvas' height before the status bar's own height is added on top — a FLOOR since
 * 8 set 2026, not a size. Everything on the canvas is measured in sp (the 64sp
 * temperature, a 22sp sentence that runs to two lines in Italian) and the block was
 * measured in dp: at 100% type a two-line sentence left 2dp before the hero climbed into
 * the place row, and at 115% the two overlapped by 30dp. The canvas is now at least this
 * tall and grows with what it holds — see [CanvasHeader]. The place row is pinned over
 * the list rather than scrolling inside the canvas (18 set 2026), and the canvas holds a
 * spacer its height in its place, so this floor still measures the same block.
 */
internal val CanvasBaseHeight = 280.dp

@Composable
internal fun CanvasHeader(
    content: TodayUiState.Content,
    gradient: SkyGradient,
    units: UnitSettings,
    timeFmt: DateTimeFormatter,
    locale: Locale,
    placeRowHeight: Dp
) {
    val sky = content.sky
    val current = content.report.current
    // The canvas owns the top edge of the screen: its height grows by the status bar
    // so the sky sits behind the clock, over the top scrim that keeps both legible.
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val floor = CanvasBaseHeight + statusTop
    // The page laid over the sky's last few dp (DESIGN §8.1, 23 set 2026). Its lip is
    // inside the floor, not added to it: the skeleton quotes the floor, and the block
    // must still end where the skeleton's does.
    val sheet = SkySheet(surface = MaterialTheme.colorScheme.surface)
    SkyCanvas(
        gradient = gradient,
        minHeight = floor,
        sheet = sheet
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
                        cloudPct = sky.cloudPct,
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Whole degrees (committente, 24 set 2026), like every other temperature
                // in the app. The tenths were the model's resolution, not its accuracy: a
                // 2 m temperature from a forecast model is good to a degree or two, and
                // since the same day the hero may be an estimate interpolated between two
                // hours — «20,8°» claimed a precision nobody had.
                Text(
                    text = Formats.temperature(current.tempC, units.temperature, locale),
                    style = com.callbackdev.chiaro.ui.theme.LocalChiaroType.current.heroTemperature,
                    color = SkyPalette.Ink
                )
                // Two type sizes on one line align by BASELINE, not by top: top-aligned
                // they read as a mistake the moment the sizes differ (device check, 2 set).
                //
                // The condition is `titleLarge` since the review of 23 set 2026: from the
                // 64sp hero straight down to 16sp was a cliff, and the word that says what
                // the sky is doing is the second thing a reader wants. No space over it:
                // the figure's own 68sp line already leaves air under its digits.
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(WeatherText.condition(current.condition.wmoCode)),
                        style = MaterialTheme.typography.titleLarge,
                        color = SkyPalette.Ink,
                        modifier = Modifier.alignByBaseline()
                    )
                    // Only when it says something (§1.2): "21° / feels like 21°" is a number
                    // with nothing to do about it. A degree apart is where a body starts to
                    // notice, and a degree apart the two whole numbers always differ.
                    if (kotlin.math.abs(current.feelsLikeC - current.tempC) >= FeelsLikeMinDeltaC) {
                        Text(
                            text = stringResource(
                                R.string.feels_like,
                                Formats.temperature(current.feelsLikeC, units.temperature, locale)
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            color = SkyPalette.Ink,
                            modifier = Modifier.alignByBaseline()
                        )
                    }
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
        val hero = measurables[1].measure(
            androidx.compose.ui.unit.Constraints(minWidth = width, maxWidth = width)
        )
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
