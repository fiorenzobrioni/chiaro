package com.callbackdev.chiaro.widget

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * The Today widget's budget (committente, 8 set 2026, afternoon: «una review completa
 * sul widget Oggi»). The card is the Now widget's one-row form with the next hours under
 * it — «Today: now plus the next hours strip» is VISION §5.9's own definition, and the
 * two cards now say it with one grammar: glyph filling the hero band, the temperature
 * over the place beside it, the day's sentence against the far edge where the card is
 * wide, and the day's high and low under the sentence when the reader asked for them.
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
internal fun todayStripCells(width: Dp): Int =
    ((width - WidgetCardPadding * 2 + StripCellSpacing) / (StripCellMin + StripCellSpacing))
        .toInt()
        .coerceIn(StripCellsFloor, StripCellsCeiling)

/** The strip's height: the hour line, the glyph, the temperature line, and the rain
 * line when the row is drawn, with the 2 dp between glyph and each text. */
internal fun todayStripHeight(fontScale: Float, rain: Boolean): Dp =
    textLineHeight(StripHourSp, fontScale) + StripInnerGap + StripIconSize + StripInnerGap +
        textLineHeight(StripTempSp, fontScale) +
        (if (rain) textLineHeight(StripRainSp, fontScale) else 0.dp)

/**
 * The hero row's words, the taller of its two columns: on the leading side the
 * temperature's line, the place's, the stale marker's when there is one, and the
 * leading band [textInkBalance] pays under them; on the trailing side the sentence's
 * two lines and the range's one, each when shown.
 */
internal fun todayHeroTextHeight(
    fontScale: Float,
    stale: Boolean,
    sentence: Boolean,
    range: Boolean
): Dp {
    val leading = textLineHeight(TemperatureSp, fontScale) + textLineHeight(PlaceSp, fontScale) +
        (if (stale) textLineHeight(StaleSp, fontScale) else 0.dp) +
        textInkBalance(TemperatureSp, fontScale)
    val trailing = (if (sentence) textLineHeight(SentenceSp, fontScale) * TallSentenceMaxLines else 0.dp) +
        (if (range) textLineHeight(PlaceSp, fontScale) else 0.dp)
    return maxOf(leading, trailing)
}

/**
 * Whether the rain row fits under the hero's words. The hero row is at least the glyph's
 * floor ([RowIconMin]) and at least its words; what is left after the strip gap has to
 * hold the strip WITH its rain line. On the reference four-by-two it does (86.8 dp of
 * room against 84.8 needed) — until a stale marker takes the words' third line and the
 * room drops to 72: then the row stays home rather than being cut at the card's edge,
 * which is the honest failure — a section that does not fit is not drawn.
 */
internal fun todayShowRain(
    size: DpSize,
    fontScale: Float,
    stale: Boolean,
    sentence: Boolean,
    range: Boolean
): Boolean {
    val hero = maxOf(RowIconMin, todayHeroTextHeight(fontScale, stale, sentence, range))
    val room = size.height - WidgetCardPaddingSnug - WidgetCardPadding - hero - StripGap
    return room >= todayStripHeight(fontScale, rain = true)
}

/** The hero glyph: what the height leaves once the strip and the gap before it are
 * paid, between the family's floor and ceiling — the same numbers as the Now widget's
 * tall card, because the two glyphs sit on the same home screen. */
internal fun todayHeroIconSize(size: DpSize, fontScale: Float, rain: Boolean): Dp =
    heroIconSize(
        size.height - WidgetCardPaddingSnug - WidgetCardPadding -
            todayStripHeight(fontScale, rain) - StripGap
    )

/**
 * Whether the hero row has room for the sentence column against its far edge: the same
 * geometry as the Now widget's wide form ([nowSentenceColumnWidth]), with this card's
 * glyph. On the reference four-by-two each text column gets ~113 dp; at the provider's
 * three-cell minimum ~68, and the row is the number and the place alone.
 */
internal fun todayIsWide(size: DpSize, icon: Dp): Boolean {
    val words = size.width - WidgetCardPaddingLeading - icon - IconTextGap - WidgetCardPaddingTrailing
    return (words - SentenceGap) / 2 >= SentenceColumnMin
}

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
internal val StripCellSpacing = 6.dp
internal const val StripCellsFloor = 4
internal const val StripCellsCeiling = 7

internal val StripIconSize = 32.dp
internal val StripInnerGap = 2.dp
internal const val StripHourSp = 12f
internal const val StripTempSp = 14f
internal const val StripRainSp = 11f
