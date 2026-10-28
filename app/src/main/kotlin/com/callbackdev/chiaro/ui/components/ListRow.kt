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
 * The arithmetic is at `SkyRow`: inside a group's 16 dp margins the row spends 12 at its
 * start and 12 after its drawing, and the text column is 201 dp at 360 with the Sky's 51 dp
 * drawing, 216 with Alerts' 36. The drawing's size is each list's own (`WeatherIconSize.Sky`
 * on the Sky; 36 dp on Alerts, where a thermometer at 51 would be a poster and a sunrise a
 * landscape): the measures around it are not.
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
