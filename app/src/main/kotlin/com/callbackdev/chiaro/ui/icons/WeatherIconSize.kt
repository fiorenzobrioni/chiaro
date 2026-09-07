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
 * Every rung grows by **6dp** — 4dp, then 2 more on a second look — and **nothing else
 * moves**: no padding, no arrangement, no column width, so each section keeps the rhythm
 * it was tuned to and only the drawings get bigger. The rungs stay in order, because the
 * order is the reading order: the hour strip is scanned sideways and carries the most
 * weight, the week rows are read down, a line of prose or a tile label leads with the
 * smallest glyph of the three.
 *
 * What the growth spends is the three elastic measures beside the icons, and each was
 * checked at 360dp before the second step: the hour cell is fixed at 56dp and keeps 9dp
 * of air per side, the week's temperature bar and the timeline's prose are `weight(1f)`
 * and pay 6dp each (112→106dp and 232→226dp), and the tile's label budget is the one
 * with a written contract — see [Tile].
 *
 * The widgets are not on this ladder: a Glance cell sizes its icon against the cell
 * (Fase 8), and the navigation bar's silhouettes are Material's own 24dp.
 */
object WeatherIconSize {

    /** Hour strip: the largest rung, in a 56dp cell — 9dp of air each side. The cell
     * width never moved, so the strip still fits the same hours on the same screen. */
    val Strip: Dp = 38.dp

    /** Week rows: under the strip, over the prose, sized to the row it sits in. */
    val Week: Dp = 34.dp

    /** Rest of the day: a leading glyph for one line of `bodyMedium`. */
    val Timeline: Dp = 30.dp

    /**
     * Details grid: the label beside it keeps a measured budget (DESIGN.md §8.6) —
     * `(360 − 32 − 12) / 2 − 32 − 38 = 88dp` on a 360dp screen, against the widest label
     * the app ships ("Qualità aria", 76.7dp in Inter 14sp). 11dp of margin left, which is
     * what says this rung could take the second step and where it stops: a third one
     * spends the margin, not the air.
     *
     * The contract is a **360dp** contract and always was. Below that the labels wrap and
     * keep their words — the honest failure already chosen for them — and they did so at
     * 320dp with the original 24dp icon too (74dp of budget against the same 76.7).
     */
    val Tile: Dp = 30.dp
}
