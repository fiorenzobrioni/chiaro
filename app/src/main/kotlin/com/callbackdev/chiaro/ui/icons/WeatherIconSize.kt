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
 * Every rung grew by **6dp** — 4dp, then 2 more on a second look — and **nothing else
 * moved**: no padding, no arrangement, no column width, so each section kept the rhythm
 * it was tuned to and only the drawings got bigger.
 *
 * **The third step, 8 set 2026** (committente: "slightly bigger, without touching the
 * spacing"), +4dp on every rung, argued with a measurement first. The question was
 * whether the drawings could simply fill more of their own box instead: the ink of the
 * 18 condition icons was measured over the whole animation loop, and it uses the box —
 * x 6.0–61.6 and y 8.0–60.0 of 64 (drops fall to y 60, the drifting overcast reaches
 * x 61.6, the sun starts at y 8). A uniform crop could take 2.4 units, under 4%, and
 * would move the optical centres; so the size moves and the viewport does not. The
 * plain cloud is why they read small: it is 30 units tall in a 64 box, 18dp of drawing
 * in a 38dp icon with 10dp of air above and below it.
 *
 * What the third step spends, each measured at 360dp:
 * - the **strip** pays nothing: the icon takes over the 2dp of vertical padding it used
 *   to sit in, so the cell stays 112dp tall, and the 56dp cell keeps 7dp of air per side
 *   instead of 9;
 * - the **week** row grows 4dp (its height is the icon's) and the temperature bar, a
 *   `weight(1f)`, goes 106→102dp;
 * - the **timeline** row grows 4dp and its prose goes 226→222dp;
 * - the **tile**'s header row grows 4dp and its label budget goes 88→84dp against the
 *   widest label the app ships, 76.7dp — see [Tile] for where that contract stops.
 *
 * The rungs stay in order, because the order is the reading order: the hour strip is
 * scanned sideways and carries the most weight, the week rows are read down, a line of
 * prose or a tile label leads with the smallest glyph of the three.
 *
 * The widgets are not on this ladder: a Glance cell sizes its icon against the cell
 * (Fase 8), and the navigation bar's silhouettes are Material's own 24dp.
 */
object WeatherIconSize {

    /** Hour strip: the largest rung, in a 56dp cell — 7dp of air each side. The cell
     * width never moved, so the strip still fits the same hours on the same screen. */
    val Strip: Dp = 42.dp

    /** Week rows: under the strip, over the prose, sized to the row it sits in. */
    val Week: Dp = 38.dp

    /** Rest of the day: a leading glyph for one line of `bodyMedium`. */
    val Timeline: Dp = 34.dp

    /**
     * The Sky's own rung, **51dp since 11 set 2026** (committente: "even 1.5x if there
     * is room"). It is the one place the ladder is broken on purpose, and the reason is
     * that the Sky is the only surface where icons from different families stand in one
     * column — a sunrise over a moon over a meteor shower — so it is the only place a
     * reader compares them at all.
     *
     * **Read it together with the normalisation of the same day**: every drawing now
     * fills 0.75 of its box, so this rung carries **38.3dp of ink**, against the hour
     * strip's 31.5dp at its 42dp box. The Sky therefore has the largest drawings in the
     * app, which inverts the reading order the rungs above are sorted by. That is the
     * committente's call and it is written here rather than smoothed over; 44dp (33dp of
     * ink) is the value that would keep the strip in front, and it is one number away.
     *
     * The room was measured at 360dp before it was taken. The text budget of a moment
     * row is `360 − 16 − icon − 16 − 48 (the bell) − 16`, so it goes from **230dp to
     * 213dp**; an event row with no bell goes from 278 to 261. The rows are already
     * three-line (name, time, chip), so their height does not move: a 51dp leading slot
     * fits inside Material's 88dp three-line row with 18dp of air each side, and inside
     * a two-line 72dp row with 10.5dp.
     *
     * What it costs is the long supporting lines: "8 Ottobre · La previsione non arriva
     * ancora così lontano" already wraps to two lines at 230dp, and 7% less width can
     * push a row like it to three.
     */
    val Sky: Dp = 51.dp

    /**
     * Details grid: the label beside it keeps a measured budget (DESIGN.md §8.6) —
     * `(360 − 32 − 12) / 2 − 32 − (icon + 8)` on a 360dp screen, which is half the row
     * minus the tile's padding minus the icon and the 8dp beside it, so **the budget is
     * `118 − icon`**. Against the widest label the app ships ("Qualità aria", 76.7dp in
     * Inter 14sp) that puts the ceiling at **41.3dp**.
     *
     * **38dp since 11 set 2026** (committente, from a device: v3's drawings are worth
     * looking at and were asking to be bigger). It leaves 80dp of label, 3.3dp of margin,
     * and it is the last step with a margin worth the name: 40dp leaves 1.3dp and 42dp
     * wraps. The note that used to stand here said a fourth step would wrap the label; the
     * arithmetic above says one step fits, and the arithmetic is right.
     *
     * What it costs is the ladder's strict order: this rung now **equals** [Week] instead
     * of sitting under it. The order it must keep is the reading order — the strip carries
     * the most weight and still leads — and a tile label beside a week row is not a
     * comparison a reader ever makes.
     *
     * The contract is a **360dp** contract and always was. Below that the labels wrap and
     * keep their words — the honest failure already chosen for them — and they did so at
     * 320dp with the original 24dp icon too.
     */
    val Tile: Dp = 38.dp
}
