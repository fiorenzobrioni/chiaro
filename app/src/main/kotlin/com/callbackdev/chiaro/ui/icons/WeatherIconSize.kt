package com.callbackdev.chiaro.ui.icons

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The one ladder the weather family climbs on Today (DESIGN.md §13.1). It lives here
 * because the four call sites were already quoting each other — DayRow's own comment read
 * "between the strip's 32 and the timeline's 24" — and a ladder written down four times
 * is a ladder that drifts.
 *
 * **The step up of 6 set 2026** (committente, from a device): at 24–32dp the drawings had
 * to be *examined* rather than recognised, which is the one thing an icon may not ask.
 * Every rung grows by 4dp and **nothing else moves** — no padding, no arrangement, no
 * column width — so each section keeps the rhythm it was tuned to and only the drawings
 * get bigger. The rungs stay in order, because the order is the reading order: the hour
 * strip is scanned sideways and carries the most weight, the week rows are read down, a
 * line of prose or a tile label leads with the smallest glyph of the three.
 *
 * The widgets are not on this ladder: a Glance cell sizes its icon against the cell
 * (Fase 8), and the navigation bar's silhouettes are Material's own 24dp.
 */
object WeatherIconSize {

    /** Hour strip: the largest rung, in a 56dp cell — 10dp of air each side. */
    val Strip: Dp = 36.dp

    /** Week rows: under the strip, over the prose, sized to the row it sits in. */
    val Week: Dp = 32.dp

    /** Rest of the day: a leading glyph for one line of `bodyMedium`. */
    val Timeline: Dp = 28.dp

    /**
     * Details grid: the label beside it keeps a measured budget (DESIGN.md §8.6) —
     * `(360 − 32 − 12) / 2 − 32 − 36 = 90dp` on a 360dp screen, against the widest
     * label the app ships ("Qualità aria", 77dp in Inter 14sp). The budget shrank by
     * the 4dp the icon gained and every label still holds its line.
     */
    val Tile: Dp = 28.dp
}
