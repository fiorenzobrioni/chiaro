package com.callbackdev.chiaro.ui.today

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp

/**
 * How Today's paper is laid out across the page (DESIGN §8.12c, 15 ott 2026): one column, or
 * two panes side by side. [start] and [end] are what the page keeps free at its edges before
 * the first section's own 16dp margin, the rail included; [pane] is one pane's width, its
 * sections' margins included, and the hero's text column takes that width too so the
 * number and its sentence stand over the left pane.
 */
@Immutable
internal data class TodayColumns(
    val twoPanes: Boolean,
    val start: Dp,
    val end: Dp,
    val pane: Dp
)

/**
 * The layout for a page [pageWidth] wide with a rail of [railInset] over its start (pure,
 * `TodayColumnsTest`).
 *
 * **Two panes from [TwoPanesMin] of paper**: the width at which each pane still holds the
 * week's row on one line at 100% type (§8.5: 182dp of fixed columns, 72 of bars, the
 * segment's 24 and the section's 32 make 310), with a few dp of air. Under it one column, the
 * phone's, however wide: a pane narrower than the row would break every day in two.
 *
 * **The two panes never pass [BodyMax] together**, and stand centred in what is left: past
 * it a pane is wider than anything in it wants to be (a 25-hour strip, a 7-row week, a
 * two-tile grid), and the eye would cross a desk to go from a day's label to its high.
 * Between and around the panes [PaneMargin]: with each section's 16 it makes Material's 24dp
 * margin at the page's edges and a 32dp gutter between the panes.
 */
internal fun todayColumns(pageWidth: Dp, railInset: Dp): TodayColumns {
    val paper = pageWidth - railInset
    if (paper < TwoPanesMin) return TodayColumns(twoPanes = false, start = railInset, end = 0.dp, pane = paper)
    val body = paper.coerceAtMost(BodyMax)
    val side = (paper - body) / 2
    return TodayColumns(
        twoPanes = true,
        start = railInset + side + PaneMargin,
        end = side + PaneMargin,
        pane = (body - PaneMargin * 2) / 2
    )
}

/** The paper's width from which Today stands in two panes. */
internal val TwoPanesMin: Dp = 640.dp

/** The two panes' widest, together, their margins included. */
internal val BodyMax: Dp = 1200.dp

/** Outside each pane's own 16dp: the page's edges come to 24, the gutter to 32. */
internal val PaneMargin: Dp = 8.dp
