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
 * The rungs stayed in order, the hour strip on top, until the strip was made compact (15 ott
 * 2026): seven hours to a 360dp phone in 42dp cells leave its glyph 32dp, under the week's 38
 * and the agenda's 34. The order was the reading order and the strip still leads it — by its
 * place on the page and its count of glyphs, no longer by their size. The details grid left
 * the ladder the same day: its eyebrows are Material glyphs at the label's size (§8.6).
 *
 * The widgets are not on this ladder: a Glance cell sizes its icon against the cell
 * (Fase 8), and the navigation bar's silhouettes are Material's own 24dp.
 */
object WeatherIconSize {

    /** Hour strip: in a 42dp cell, 5dp of air each side (15 ott 2026; it was the largest rung,
     * 42dp in a 56dp cell, until the strip was made compact to show seven hours at 360dp). */
    val Strip: Dp = 32.dp

    /** A page's one glyph, beside its title (the Sky's page for a moment): the family's
     * largest rung, 42dp, which the strip held until 15 ott 2026. */
    val Page: Dp = 42.dp

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

}
