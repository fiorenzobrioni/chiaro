package com.callbackdev.chiaro.widget

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * The Sky widget's forms, and the grant that picks one (committente, 8 set 2026,
 * evening: «rivedi completamente il layout, con la stessa logica di adattamento del
 * widget Ora»). Same grammar as [NowLayout], because the two cards sit on the same
 * home screen and should read as siblings:
 *
 * - **one row, narrow** (three cells): the glyph, the moment's time as the hero number
 *   over its name, and the verdict as a mark — the series' own `✓ ~ ✗ ?` in the
 *   verdict's measured container — before the name;
 * - **one row, wide** (four cells and up): the same, and against the far edge the
 *   verdict as a word chip over the number that decided it;
 * - **two rows and up**: that row as the head of a list, and under it as many further
 *   moments as the height honestly holds, each with its small glyph, name, time and
 *   verdict — the word on a wide card, the mark on a narrow one.
 *
 * What changed from the first design (Fase 8, 3 set): the hero number is the TIME.
 * The card's question is «when, and is it worth going out», and a 30 sp clock answers
 * the first half at arm's length where a 15 sp name over a 12 sp time did not; the
 * verdict answers the second, and the glyph says which moment as the Now widget's
 * glyph says which weather. Everything here is arithmetic on dp so a table can pin it
 * (`SkyWidgetLayoutTest`).
 */
internal fun skyIsTall(size: DpSize): Boolean = size.height >= TallMinHeight

/**
 * Whether the words' column can spare [SkyVerdictColumn] for the chip-and-number block
 * and still keep [SkyWordsMin]. Geometric, never a function of which moment is showing:
 * a card that changed form with the forecast could not be aimed.
 */
internal fun skyIsWide(size: DpSize): Boolean = skyWordsColumnWidth(size) >= SkyWordsMin

/** The words' column on a wide card: what the row leaves after the glyph, the two gaps,
 * the far inset and the verdict block. Negative on a narrow card; nobody draws it. */
internal fun skyWordsColumnWidth(size: DpSize): Dp =
    size.width - WidgetCardPaddingLeading - skyHeroIconSize(size) - IconTextGap -
        SentenceGap - WidgetCardPaddingTrailing - SkyVerdictColumn

/**
 * The hero glyph. On one row it fills the height like the Now widget's, with the same
 * width guard, but caps at [SkyHeroIconMax]: the card's anchor is a 30 sp clock, and a
 * sunrise drawn at 89 dp would dwarf it on the one launcher that grants 101 dp rows —
 * and would push a four-cell card back to its narrow form there, because every dp of
 * glyph is a dp the words' column loses. On a tall card it is [SkyTallHeroIcon], so
 * the list under it gets the height: the list is what a tall Sky card is for.
 */
internal fun skyHeroIconSize(size: DpSize): Dp =
    if (skyIsTall(size)) {
        SkyTallHeroIcon
    } else {
        val byHeight = size.height - WidgetCardPaddingSnug * 2
        val byWidth = size.width - WidgetCardPaddingLeading - IconTextGap -
            WidgetCardPaddingTrailing - WordsColumnMin
        heroIconSize(minOf(byHeight, byWidth), min = RowIconMin, max = SkyHeroIconMax)
    }

/**
 * How many further moments fit under the hero on a tall card. The hero row is the
 * taller of its glyph and its words — the clock's line, the mark's chip (taller than
 * the name's line, and always reserved so a card does not gain a row when a verdict
 * goes away), and the leading band [textInkBalance] pays under it — then the list gap,
 * then rows of [SkyRowHeight] with [SkyRowGap] between them, the first gap forgiven.
 * Never more than [available]: the list is not padded, inventing a moment is the one
 * thing this widget must not do.
 */
internal fun skyRows(size: DpSize, fontScale: Float, available: Int): Int {
    if (!skyIsTall(size)) return 0
    val words = textLineHeight(SkyTimeSp, fontScale) +
        maxOf(textLineHeight(SkyNameSp, fontScale), SkyMarkChip) +
        textInkBalance(SkyTimeSp, fontScale)
    val hero = maxOf(SkyTallHeroIcon, words)
    val room = size.height - WidgetCardPaddingSnug - WidgetCardPadding - hero - SkyListGap
    val row = maxOf(SkyRowHeight, textLineHeight(SkyCompactSp, fontScale))
    return ((room + SkyRowGap) / (row + SkyRowGap)).toInt().coerceIn(0, available.coerceAtLeast(0))
}

/**
 * The clock: 30 sp Medium, against the Now widget's 34. A time is a longer string than
 * a temperature — «12:05 AM» is eight glyphs to «−12°»'s four — and at 34 it would not
 * fit the 136 dp a four-cell card leaves the words (measured: 149 dp), where at 30 it
 * does (132). Five glyphs at 30 carry about the ink three carry at 34, so the two
 * numbers sit at one optical weight side by side.
 */
internal const val SkyTimeSp = 30f

/** The name under the clock: the Now widget's place line, one step down because it
 * carries a day marker before it («Domani · Sorge la luna») and the column is finite. */
internal const val SkyNameSp = 15f

/** Chip text: 11 sp, the one size at which «Presto per dirlo» (89 dp with its padding)
 * fits [SkyVerdictColumn]; 12 would not. Same size on the hero and in the rows. */
internal const val SkyChipSp = 11f

/** The mark's drawing inside its 22 dp container: 12 dp, the size the glyph had as a
 * character, and the same the arc widget's rows use. */
internal val SkyMarkGlyph = 12.dp

/** The number under the word chip: «pioggia 100%» is 83 dp at this size. */
internal const val SkyEvidenceSp = 14f

/** A compact row's two texts share one size, so their baselines meet under centre
 * alignment (device finding, 4 set). */
internal const val SkyCompactSp = 13f

internal val SkyHeroIconMax = 72.dp
internal val SkyTallHeroIcon = 60.dp

/** The verdict block's fixed width: the widest chip (89 dp) and the widest evidence
 * line (83) both clear it. Fixed rather than weighted because a chip does not wrap. */
internal val SkyVerdictColumn = 96.dp

/** The narrowest words' column worth the wide form: «7:55 PM» at 30 sp is 114 dp, and
 * the reference four-cell card gives 136. A three-cell card gives 46 and stays narrow. */
internal val SkyWordsMin = 120.dp

internal val SkyMarkChip = 22.dp
internal val SkyRowGlyph = 20.dp

/** A row is its 22 dp mark and a dp of air either side; the 13 sp texts are shorter. */
internal val SkyRowHeight = 24.dp
internal val SkyRowGap = 6.dp

/** What separates the moment in front of the reader from the ones behind it. */
internal val SkyListGap = 10.dp

/**
 * The rows' small glyphs start this far in from where the hero's does, so their ink
 * lines up under its ink rather than under its box: a 60 dp Meteocons box keeps ~8 dp
 * empty on the leading side, a 20 dp one ~3, and 6 is the difference.
 */
internal val SkyRowIndent = 6.dp
