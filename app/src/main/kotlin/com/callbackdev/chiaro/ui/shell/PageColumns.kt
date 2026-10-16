package com.callbackdev.chiaro.ui.shell

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp

/**
 * How a tab's paper is laid out across the page (DESIGN §8.12c): one column, or two panes
 * side by side. Today's since 15 ott 2026, Sky's and Alerts' since 16 ott 2026, one rule for
 * the three so that a device which shows Today in two panes shows every tab in two. [start]
 * and [end] are what the page keeps free at its edges before the first section's own 16dp
 * margin, the rail included; [pane] is one pane's width, its sections' margins included, and
 * Today's hero takes that width too so the number and its sentence stand over the left pane.
 */
@Immutable
internal data class PageColumns(
    val twoPanes: Boolean,
    val start: Dp,
    val end: Dp,
    val pane: Dp
)

/**
 * The layout for a page [pageWidth] wide with a rail of [railInset] over its start (pure,
 * `PageColumnsTest`). Today is laid out the full window and passes the rail; Sky and Alerts
 * are laid out beside it and pass zero.
 *
 * **Two panes from [TwoPanesMin] of paper**: the width at which each pane still holds the
 * week's row on one line at 100% type (§8.5: 182dp of fixed columns, 72 of bars, the
 * segment's 24 and the section's 32 make 310), with a few dp of air. Under it one column, the
 * phone's, however wide: a pane narrower than the row would break every day in two.
 *
 * **The two panes never pass [BodyMax] together**, and stand centred in what is left: past
 * it a pane is wider than anything in it wants to be (a 25-hour strip, a 7-row week, a
 * two-tile grid, a row of the Sky's calendar), and the eye would cross a desk to go from a
 * day's label to its high. Between and around the panes [PaneMargin]: with each section's 16
 * it makes Material's 24dp margin at the page's edges and a 32dp gutter between the panes.
 */
internal fun pageColumns(pageWidth: Dp, railInset: Dp = 0.dp): PageColumns {
    val paper = pageWidth - railInset
    if (paper < TwoPanesMin) return PageColumns(twoPanes = false, start = railInset, end = 0.dp, pane = paper)
    val body = paper.coerceAtMost(BodyMax)
    val side = (paper - body) / 2
    return PageColumns(
        twoPanes = true,
        start = railInset + side + PaneMargin,
        end = side + PaneMargin,
        pane = (body - PaneMargin * 2) / 2
    )
}

/** The paper's width from which a tab stands in two panes. */
internal val TwoPanesMin: Dp = 640.dp

/** The two panes' widest, together, their margins included. */
internal val BodyMax: Dp = 1200.dp

/** Outside each pane's own 16dp: the page's edges come to 24, the gutter to 32. */
internal val PaneMargin: Dp = 8.dp
