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
     * **Read it together with the normalisation of the same day**: every drawing is taken
     * to a geometric mean of 0.69 of its box, so this rung carries **35.2dp of ink** on a
     * square drawing, against the hour strip's 29dp at its 42dp box. The Sky therefore has
     * the largest drawings in the app, which inverts the reading order the rungs above are
     * sorted by. That is the committente's call and it is written here rather than smoothed
     * over; 44dp (30dp of ink) is the value that would put the strip back in front, and it
     * is one number away.
     *
     * The room was measured at 360dp before it was taken. **The figure for a row with a
     * bell was one 16dp inset short and is corrected here (12 set 2026)**: Material puts
     * a gap between the text column and the trailing slot as well as one after the
     * leading slot, so the budget is `360 − 16 − icon − 16 − 16 − 48 (the bell) − 16`
     * and it went from **214dp to 197dp**, not 230 to 213. (Read off the device's own
     * screenshot: the chip ends at 303.6dp and the bell's 48dp box is centred at 344 on
     * a 384dp screen.) An event row with no bell has no such gap and its figures stood:
     * 278 to 261. The rows were three-line (name, time, chip) when this was taken, so
     * their height did not move — a 51dp leading slot fits inside Material's 88dp
     * three-line row with 18dp of air each side, and inside a two-line 72dp row with
     * 10.5dp, which is what they are since the chip took a line of its own (§8.8).
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

    /**
     * **The UV tile's own rung, 55dp since 12 set 2026** (committente, from a device:
     * the UV glyph should grow "until the number it shows is the same size as the one
     * on the pollen icon"). It is the one rung aimed at a detail inside a drawing rather
     * than at the drawing, and that is the whole reason it exists.
     *
     * Both families draw the same badge: a 30-unit rounded square in a 128 box, with the
     * value in it (`M88,79 … A9,9` on `uv-index-*`, `M80,71 … A9,9` on `pollen-*-low`).
     * What differs is the `mc3scale` the importer cooks in to bring every drawing to the
     * same ink (§13.1): the UV sun spreads its rays to the corners so it is already big
     * and is scaled **0.92**, the pollen sprig is compact and is scaled **1.3382** — so
     * at one box size the pollen badge is 1.45× the UV one. Measured on the device's own
     * screenshot at 2.8125 px/dp: 33 px of UV badge against 40 of pollen.
     *
     * The lever left at the call site is the box, and this is it:
     * `38 × 1.3382 / 0.92 = 55.3`. At 55dp the UV badge is **11.85dp** against the grass
     * and weed badge's 11.92 at [Tile] — 0.6% apart, which no eye separates. The tree
     * pollens are scaled 1.3109, so their badge is 11.68 and the UV one lands 1.5% over
     * instead of under; the grass badge is the one the request was measured against.
     *
     * What it costs, and it is not nothing: this rung is 45% over the ladder's [Tile]
     * step, so the UV glyph is plainly the biggest thing in the details grid, and the
     * header row it sits in grows 17dp — which its neighbour in the same row pays too,
     * because a pair of tiles shares one height. The label budget is not what it costs:
     * `(384 − 32 − 12) / 2 − 32 − (55 + 8)` leaves 75dp on this device and 63 at 360dp
     * for a label that is the two letters «UV», the shortest the app ships.
     */
    val TileUv: Dp = 55.dp
}
