package com.callbackdev.chiaro.ui.today

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.ui.components.FittingText
import com.callbackdev.chiaro.ui.icons.ConditionGlyph
import com.callbackdev.chiaro.ui.icons.LocalMotionPaused
import com.callbackdev.chiaro.ui.theme.ChiaroMotion
import com.callbackdev.chiaro.ui.theme.reducedMotion
import com.callbackdev.chiaro.ui.theme.tabular

/**
 * The place row's vertical padding, the same on the canvas and on a plain surface: it
 * was 8dp on the sky and 12dp everywhere else, so swiping from a place with a report to
 * one without moved the title 4dp (review, 8 set 2026).
 */
internal val HeaderVerticalPadding = 8.dp

/** The air the place target's ripple has around its text, taken from [HeaderPadding]. */
internal val PlaceTargetInset = 8.dp

/** The place row's padding: the page's 16 at the end, where the gear's own 48dp box carries
 * its air, and 16 less the place target's inset at the start, so the name stands on the
 * page margin and the target's ripple reaches past it. */
internal val HeaderPadding = PaddingValues(
    start = 16.dp - PlaceTargetInset,
    end = 16.dp,
    top = HeaderVerticalPadding,
    bottom = HeaderVerticalPadding
)

/** The place target's minimum, DESIGN §6's floor for anything a finger aims at. */
internal val PlaceTargetMin = 48.dp

/** The place target's ripple: rounded like the other tabs' place targets. */
internal val PlaceTargetShape = RoundedCornerShape(12.dp)

/** The frame for the two page-less situations: not started yet, and no place at all. */
@Composable
internal fun GlobalFrame(
    onOpenPlaces: () -> Unit,
    onOpenSettings: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PlaceHeader(
                title = stringResource(R.string.app_name),
                isGps = false,
                localNow = null,
                dots = null,
                onOpenPlaces = onOpenPlaces,
                onOpenSettings = onOpenSettings,
                contentColor = MaterialTheme.colorScheme.onSurface,
                dotInactive = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(HeaderPadding)
            )
            content()
        }
    }
}

/**
 * The place switcher: name, chevron, and the pager dots when there is more than one
 * page. One composable for both grounds — white over the canvas' scrim, theme ink on
 * a plain surface — so the two can never drift apart in shape.
 *
 * [isGps] draws the position pin before the name: a saved "Cavenago" and the GPS fix
 * standing in Cavenago would otherwise be two identical pages, and where a number
 * comes from is part of its truth (device request, 2 set). The pin carries its word
 * through the row's description, never alone.
 *
 * [localNow] is the other half of that same question, and appears on exactly the pages
 * the pin does not (device request, 7 set): the day and the hour IN this place. On the
 * position page it would be the phone's own clock printed under the status bar that
 * already shows it; on any other place it is the one thing the reader cannot look up —
 * Palermo and Reykjavík are not on the same hour, and some days not on the same date.
 * It is a fact about the place, not about the data: the page's last line still says
 * when the numbers arrived, in the same timezone as this one and in its own words.
 */
@Composable
internal fun PlaceHeader(
    title: String,
    isGps: Boolean,
    /** The place's day and hour, longest form first ([FittingText] draws the longest
     * that fits). */
    localNow: List<String>?,
    dots: Pair<Int, Int>?,
    onOpenPlaces: () -> Unit,
    onOpenSettings: () -> Unit,
    contentColor: Color,
    dotInactive: Color,
    modifier: Modifier = Modifier,
    compactVisible: Boolean = false,
    compact: (@Composable () -> Unit)? = null
) {
    val spoken = if (isGps) stringResource(R.string.header_gps_desc, title) else title
    val switcher = stringResource(R.string.place_switcher_action)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f)) {
            // The target is the whole block, name and hour together, as on the other tabs'
            // `TabHeader` (review of Today, 15 ott 2026). It was the name's row alone: a
            // `titleLarge` line beside a 24dp chevron, about 28dp tall, under the 48 DESIGN
            // §6 asks of every target, with a ripple that was not clipped and so drew a
            // rectangle over the sky. Name and hour are 28 + 20 = 48dp, which is also where
            // a finger aims for "this place"; without the hour the block keeps 48 by its
            // minimum. The 8dp it is inset by are the ripple's, taken from the row's 16, so
            // the text stays where it was. The ripple takes the row's ink: white over the
            // sky, the theme's ink on a plain surface.
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(PlaceTargetShape)
                        .clickable(onClickLabel = switcher, role = Role.Button, onClick = onOpenPlaces)
                        // One button to a screen reader: the place (and the position pin's
                        // word) and the place's own hour, with «change place» as its action.
                        // The chevron says nothing of its own.
                        .clearAndSetSemantics {
                            // The longest form, whichever one the line can draw.
                            contentDescription = listOfNotNull(spoken, localNow?.firstOrNull())
                                .joinToString(", ")
                        }
                        .defaultMinSize(minWidth = PlaceTargetMin, minHeight = PlaceTargetMin)
                        .padding(horizontal = PlaceTargetInset)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isGps) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            title,
                            style = MaterialTheme.typography.titleLarge,
                            color = contentColor,
                            // One line when the bar also carries the temperature: a name
                            // that wrapped under it would push the bar taller at every scroll.
                            maxLines = if (compact != null) 1 else Int.MAX_VALUE,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowDown,
                            contentDescription = null, // the block's action says it
                            tint = contentColor
                        )
                    }
                    // Full strength rather than a faded white: this sits over the sky,
                    // where §3.6's scrim is measured against ink at full opacity, and the
                    // size is what makes it secondary.
                    //
                    // The longest form that fits (committente, 15 ott 2026): «Venerdì 25
                    // settembre · 12:15» while it has the room, «Ven 25 set · 12:15» when the
                    // bar's temperature takes it, «Ven · 12:15» at a type size where neither
                    // fits. Measured, not decided by the scroll: on a wide phone the long one
                    // stays all the way down, and the line never wraps, which would make the
                    // bar taller under the reader's finger.
                    localNow?.let {
                        FittingText(
                            candidates = it,
                            style = MaterialTheme.typography.labelLarge,
                            color = contentColor
                        )
                    }
                }
            }
            // Outside the target: where this page stands among the others is a fact,
            // not a way in.
            dots?.let { (selected, count) ->
                PageDots(
                    selected = selected,
                    count = count,
                    active = contentColor,
                    inactive = dotInactive,
                    modifier = Modifier.padding(start = PlaceTargetInset)
                )
            }
        }
        compact?.let { now ->
            AnimatedVisibility(
                visible = compactVisible,
                enter = fadeIn(ChiaroMotion.effects(reducedMotion())) +
                    slideInVertically(ChiaroMotion.spatial(reducedMotion())) { it / 2 },
                exit = fadeOut(ChiaroMotion.effects(reducedMotion())) +
                    slideOutVertically(ChiaroMotion.spatial(reducedMotion())) { it / 2 }
            ) { now() }
        }
        // The gear of VISION §5.1: settings are visited once a month, so they get an
        // icon by the place row, not a tab. Same two grounds as the row itself.
        IconButton(onClick = onOpenSettings) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = stringResource(R.string.settings_title),
                tint = contentColor
            )
        }
    }
}

/**
 * The hero in the bar (DESIGN §8.1b, 23 set 2026): the sky's icon and the temperature,
 * shown once the canvas has scrolled away. One announcement, the same number the hero
 * says; the icon is the still drawing, because the bar is where the eye rests while the
 * page moves under it.
 */
@Composable
internal fun CompactNow(temperature: String, glyph: ConditionGlyph, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(start = 8.dp)
    ) {
        CompositionLocalProvider(LocalMotionPaused provides true) {
            com.callbackdev.chiaro.ui.icons.ConditionIcon(glyph = glyph, modifier = Modifier.size(32.dp))
        }
        Text(
            text = temperature,
            style = MaterialTheme.typography.titleLarge.tabular(),
            color = color
        )
    }
}

/**
 * How long the place row takes to turn from the sky's to the page's (committente, 15 ott
 * 2026): longer than §7's effects spring, about a tenth of a second, which on the device read
 * as a snap; short enough that the grey midway is gone before it is read. Reduced motion
 * keeps §7's 100 ms fade.
 */
internal const val BarCrossfadeMillis = 300

internal fun <T> barCrossfade(reduced: Boolean): androidx.compose.animation.core.FiniteAnimationSpec<T> =
    if (reduced) ChiaroMotion.fade() else androidx.compose.animation.core.tween(
        BarCrossfadeMillis,
        easing = androidx.compose.animation.core.FastOutSlowInEasing
    )

/** How far above the bar's bottom edge the canvas' bottom may climb before the hero
 * counts as gone: the lip, the padding and the ribbon, up to the temperature's baseline. */
internal val HeroReach = 120.dp

/** The place dots of VISION §5.1: position among the pages, at a glance. */
@Composable
internal fun PageDots(
    selected: Int,
    count: Int,
    active: Color,
    inactive: Color,
    modifier: Modifier = Modifier
) {
    val description = stringResource(R.string.pager_dots_desc, selected + 1, count)
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .padding(top = 4.dp)
            .semantics { contentDescription = description }
    ) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (index == selected) active else inactive)
            )
        }
    }
}
