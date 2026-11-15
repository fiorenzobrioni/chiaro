package com.callbackdev.chiaro.widget

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * The Today widget's budget (committente, 8 set 2026, afternoon: «una review completa
 * sul widget Oggi»). The card is the Now widget's one-row form with the next hours under
 * it — «Today: now plus the next hours strip» is VISION §5.9's own definition, and the
 * two cards say it with one grammar: since 15 nov 2026 literally one composition
 * ([NowHeroRow]), the place over the glyph and the number, the day's sentence against the
 * far edge where the card is wide, and the day's high and low under the sentence with
 * their marks when the reader asked for them (committente: «portiamo lo stesso layout
 * anche lì», the hours' card being the four-by-one grown a row).
 *
 * What changed from the first design (Fase 8): the sentence left its own line under the
 * hero row and joined the hero's far edge, which is where the Now widget prints it and
 * where a reader who has both cards on one screen already looks for it. The line it
 * gave back goes to the glyph: on the reference four-by-two (~340 × 189 dp) the hero
 * glyph grows from ~45 dp to ~76, the size the Now widget draws beside it. The strip
 * keeps its rule — as many hours as the width honestly holds, the rain row when any of
 * them has rain to report — and gains one: the rain row also has to FIT, and yields to
 * the hero's words when a stale marker takes their third line. Everything here is
 * arithmetic on dp so a table can pin it (`TodayWidgetLayoutTest`).
 */
internal fun todayStripCells(width: Dp, cellMin: Dp = StripCellMin): Int =
    ((width - WidgetCardPadding * 2 + StripCellSpacing) / (maxOf(cellMin, StripCellMin) + StripCellSpacing))
        .toInt()
        .coerceIn(StripCellsFloor, StripCellsCeiling)

/** The strip's height: the hour line, the glyph, the temperature line, and the rain
 * line when the row is drawn, with the 2 dp between glyph and each text. */
internal fun todayStripHeight(fontScale: Float, rain: Boolean): Dp =
    textLineHeight(StripHourSp, fontScale) + StripInnerGap + StripIconSize + StripInnerGap +
        textLineHeight(StripTempSp, fontScale) +
        (if (rain) textLineHeight(StripRainSp, fontScale) else 0.dp)

/**
 * What the hero row must keep, the taller of its two columns: on the leading side the
 * place's line, the number's at [TodayHeroKeepSp] and the stale marker's when there is
 * one; on the trailing side the sentence's two lines, the chip and the range's one, each
 * when shown. The glyph stands on the number's line and costs the row no height of its own.
 */
internal fun todayHeroTextHeight(
    fontScale: Float,
    stale: Boolean,
    sentence: Boolean,
    range: Boolean,
    warning: Boolean = false
): Dp {
    val fact = textLineHeight(TextFactSp, fontScale)
    val leading = fact + textLineHeight(TodayHeroKeepSp, fontScale) +
        (if (stale) textLineHeight(TextStaleSp, fontScale) else 0.dp)
    val trailing = (if (sentence) textLineHeight(SentenceSp, fontScale) * TallSentenceMaxLines else 0.dp) +
        warningBlock(fontScale, warning) +
        (if (range) fact else 0.dp)
    return maxOf(leading, trailing)
}

/**
 * The number the hours' card keeps before a strip line is paid: the 34 sp this card always
 * printed. The row may draw it larger, up to the four-by-one's own number, out of whatever
 * the strip leaves ([todayHeroBand]); it never squeezes the hours to get there.
 */
internal const val TodayHeroKeepSp = TemperatureSp

/**
 * Whether the warning chip fits on a line of its own under the sentence (Fase 11). It
 * grows the hero's trailing column, and what it must not do is push the strip off the
 * card — the strip IS this widget. A section that does not fit is not drawn.
 */
internal fun todayHasWarningRow(
    size: DpSize,
    fontScale: Float,
    stale: Boolean,
    sentence: Boolean,
    range: Boolean
): Boolean {
    val hero = todayHeroTextHeight(fontScale, stale, sentence, range, warning = true)
    val room = size.height - WidgetCardPaddingSnug - WidgetCardPadding - hero - StripGap
    return room >= todayStripHeight(fontScale, rain = false)
}

/**
 * Whether the rain row fits under the hero's words: what is left after the hero and the
 * strip gap has to hold the strip WITH its rain line. On the reference four-by-two it does
 * (76.2 dp of room against the 66 the number and the place keep) — until a stale marker
 * takes a third line; then the row stays home rather than being cut at the card's edge,
 * which is the honest failure: a section that does not fit is not drawn.
 */
internal fun todayShowRain(
    size: DpSize,
    fontScale: Float,
    stale: Boolean,
    sentence: Boolean,
    range: Boolean,
    warning: Boolean = false
): Boolean {
    val hero = todayHeroTextHeight(fontScale, stale, sentence, range, warning)
    val room = size.height - WidgetCardPaddingSnug - WidgetCardPadding - hero - StripGap
    return room >= todayStripHeight(fontScale, rain = true)
}

/**
 * The hero row's band: what the height leaves once the strip and the gap before it are
 * paid, up to [TodayHeroBandMax]. The row sizes its number and its glyph to it as a
 * four-by-one sizes them to its height, so on every card from the reference four-by-two up
 * the hero is the four-by-one's, to the dp.
 */
internal fun todayHeroBand(size: DpSize, fontScale: Float, rain: Boolean): Dp =
    (size.height - WidgetCardPaddingSnug - WidgetCardPadding -
        todayStripHeight(fontScale, rain) - StripGap).coerceAtMost(TodayHeroBandMax)

/**
 * **The band's ceiling: the reference four-by-one's, 85 dp less its snug insets.** The
 * hours' card grown a row is that card with the hours under it, and a taller grant buys
 * the hours more room, never a bigger hero: a 44 sp number over a strip of 13 sp hours
 * was measured as a poster with a footnote. The air a tall card has left goes between the
 * hero and the hours, where it was before.
 */
internal val TodayHeroBandMax = 85.dp - WidgetCardPaddingSnug * 2

/** The size the hero row lays itself out for: the card's width, and the band inside the
 * snug insets its arithmetic budgets out of. */
internal fun todayHeroSize(size: DpSize, fontScale: Float, rain: Boolean): DpSize =
    DpSize(size.width, todayHeroBand(size, fontScale, rain) + WidgetCardPaddingSnug * 2)

/**
 * Whether the hero row has a far edge: the four-by-one's own gate at the band's ceiling,
 * so the form is the width's and never the strip's. Three cells are the place and the
 * number, as they are on one row.
 */
internal fun todayIsWide(width: Dp): Boolean =
    nowRowIsWide(DpSize(width, TodayHeroBandMax + WidgetCardPaddingSnug * 2))

/**
 * **The next days on a tall card** (23 set 2026, widget review). From about three rows the
 * card had a band of nothing between its hero and its hours; it now carries the days —
 * the day's name, its drawing, its high and its low — under the hours, so the card reads
 * top to bottom as now, the next hours, the next days. It starts with today, in as many
 * cells as the hours have, so the two rows are one grid.
 *
 * Budgeted last: the hero row and the strip with its rain line are paid first, and the row
 * is drawn only when all of it fits, never squeezed. On the reference four-by-two it does
 * not (the card is unchanged), on every three-row card from three cells up it does.
 */
internal fun todayShowDays(
    size: DpSize,
    fontScale: Float,
    stale: Boolean,
    sentence: Boolean,
    range: Boolean,
    warning: Boolean,
    rain: Boolean
): Boolean {
    val hero = maxOf(
        todayHeroBand(size, fontScale, rain),
        todayHeroTextHeight(fontScale, stale, sentence, range, warning)
    )
    val room = size.height - WidgetCardPaddingSnug - WidgetCardPadding - hero - StripGap -
        todayStripHeight(fontScale, rain) - DaysGap
    return room >= todayDaysHeight(fontScale)
}

/**
 * **A second row of hours on the tallest card** (widget review, 13 ott 2026). At four by
 * four the card held its hero, one row of hours and the days, and a third of it was air
 * between the hero and the hours. The air now holds the hours after the first row's, from
 * the strip the app already carries (the next 24): seven hours become fourteen, the same
 * grid, and nothing on the card is drawn that the forecast did not say.
 *
 * Budgeted after everything else and with its rain line, so it never squeezes what was
 * there: on the reference four-by-three it does not fit and the card is unchanged.
 */
internal fun todayShowMoreHours(
    size: DpSize,
    fontScale: Float,
    stale: Boolean,
    sentence: Boolean,
    range: Boolean,
    warning: Boolean,
    rain: Boolean
): Boolean {
    val hero = maxOf(
        todayHeroBand(size, fontScale, rain),
        todayHeroTextHeight(fontScale, stale, sentence, range, warning)
    )
    val room = size.height - WidgetCardPaddingSnug - WidgetCardPadding - hero - StripGap -
        todayStripHeight(fontScale, rain) - DaysGap - todayDaysHeight(fontScale) -
        MoreHoursGap - todayStripHeight(fontScale, rain = true)
    return room >= 0.dp
}

/** The air between the two rows of hours: the hours' own gap, because they are one list. */
internal val MoreHoursGap = 8.dp

/** The days' row: the day's name, the glyph, the high and the low, stacked like an hour. */
internal fun todayDaysHeight(fontScale: Float): Dp =
    textLineHeight(StripHourSp, fontScale) + StripInnerGap + DaysIconSize + StripInnerGap +
        textLineHeight(StripTempSp, fontScale) + textLineHeight(StripRainSp, fontScale)

/** A day's drawing: a step under the hour's, because a day is a summary and the hours
 * above it are the card's subject. */
internal val DaysIconSize = 28.dp

/** The air between the hours and the days: a little more than between the hero and the
 * hours, so the two rows read as two things. */
internal val DaysGap = 10.dp

/** The strip's texts start at the words' inset, not the glyph's: the card's start
 * padding is the glyph's 4 dp, so the strip pays the other 10 itself. */
internal val StripStartInset = WidgetCardPadding - WidgetCardPaddingLeading

/** The air between the hero row and the strip: the card's sub-unit, twice. */
internal val StripGap = 8.dp

/**
 * The strip sizes itself to the width the launcher actually granted: a cell under
 * [StripCellMin] squeezes its numbers, and fewer than four hours is no longer an
 * afternoon. At the reference four-cell width this lands on seven cells.
 */
internal val StripCellMin = 38.dp

/**
 * The cell an hour's label really needs: the widest clock time the strip can print,
 * measured by the caller at the label's size and the reader's font scale, plus
 * [RowFitSlack]. «13:00» (widget review, 13 ott 2026: a bare «13» over a row of days read
 * as a date) is ~33 dp at 13 sp and fits the 38 dp floor at the default font size; at a
 * larger one the strip shows an hour fewer rather than cutting «13:…».
 */
internal fun stripCellFor(labelWidth: Dp): Dp = labelWidth + RowFitSlack
internal val StripCellSpacing = 6.dp
internal const val StripCellsFloor = 4
internal const val StripCellsCeiling = 7

internal val StripIconSize = 32.dp
internal val StripInnerGap = 2.dp
internal const val StripHourSp = WidgetType.Label
internal const val StripTempSp = WidgetType.Label
internal const val StripRainSp = WidgetType.Caption
