package com.callbackdev.chiaro.ui.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The measures of a row in a grouped list: the Sky's since its review (point 2, 14 ott
 * 2026), and Alerts' since 15.7 (28 Oct 2026). Until then `AlertRow` and `RuleCard` spent
 * 16 at the start, 14 between the drawing and the text and 14 above and below, and the two
 * tabs' twin lists had two densities (the Alerts review, point 10). One object, not two
 * copies, so they cannot drift apart again.
 *
 * Inside a group's 16 dp margins a row spends 12 at its start and 12 after its drawing, and
 * Alerts' text column is 216 dp at 360 with its 36 dp drawing. The drawing's size is each
 * list's own (36 dp on Alerts, where a thermometer at 51 would be a poster and a sunrise a
 * landscape): the measures around it are not.
 *
 * Since Fase 16.2 (10 nov 2026) the Sky's rows are the agenda's ([TimelineRow]), which takes
 * its [Start] and its [Gap] from here and keeps its own height: 12 above and below and 64 at
 * least, where a row begins with a clock column and often ends with a verdict's line.
 * [Vertical] and [MinHeight] are Alerts'.
 */
object ListRow {
    /** From the row's edge to its drawing. */
    val Start: Dp = 12.dp

    /** Between the drawing and the text. */
    val Gap: Dp = 12.dp

    /** Above and below the content; [MinHeight] centres a short row. */
    val Vertical: Dp = 8.dp

    /** A row is never shorter than this: a one-line row is still a row a thumb finds. */
    val MinHeight: Dp = 72.dp
}
