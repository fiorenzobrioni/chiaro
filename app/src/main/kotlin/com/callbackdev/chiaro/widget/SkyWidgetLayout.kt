package com.callbackdev.chiaro.widget

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.domain.sky.SkyVerdictKind
import com.callbackdev.chiaro.ui.format.Formats
import com.callbackdev.chiaro.ui.sky.SkyText
import java.time.LocalTime
import java.util.Locale

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
 *   the verdict's note — the arc's agenda's own bare mark, on every width (13 ott 2026).
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
internal fun skyIsWide(
    size: DpSize,
    verdictColumn: Dp = SkyVerdictColumn,
    wordsMin: Dp = SkyWordsMin
): Boolean = skyWordsColumnWidth(size, verdictColumn) >= wordsMin

/**
 * [skyIsWide] with both widths measured in the locale and clock the card is drawn in
 * (widget review, 13 ott 2026). The chip now carries the verdict's mark before its word, at
 * the scale's 13 sp, and a column fixed for one language is a column wrong for the other:
 * «Presto per dirlo» is a 124 dp chip and «Not sure yet» a 102 dp one, «19:55» an 80 dp
 * clock and «7:55 PM» a 116 dp one. Still geometric — the widest of the four words and a
 * fixed sample time, never the moment on show — so a card does not change form with the
 * forecast.
 */
internal fun skyIsWide(size: DpSize, context: Context): Boolean =
    skyIsWide(size, skyVerdictColumn(context), skyWordsMin(context))

/** The words' column on a wide card: what the row leaves after the glyph, the two gaps,
 * the far inset and the verdict block. Negative on a narrow card; nobody draws it. */
internal fun skyWordsColumnWidth(size: DpSize, verdictColumn: Dp = SkyVerdictColumn): Dp =
    size.width - WidgetCardPaddingLeading - skyHeroIconSize(size) - IconTextGap -
        SentenceGap - WidgetCardPaddingTrailing - verdictColumn

/** The verdict block: the widest of the four chips in this language, or the evidence line's
 * floor if that is wider, and [RowFitSlack]. */
internal fun skyVerdictColumn(context: Context): Dp {
    val chip = SkyVerdictKind.entries.maxOf { kind ->
        measureWidgetText(context, context.getString(SkyText.verdictWordRes(kind)), SkyChipSp, medium = true)
    } + SkyMarkGlyph + SkyChipTextGap + SkyChipPadH * 2
    return maxOf(chip, SkyEvidenceMin) + RowFitSlack
}

/** The narrowest words' column worth the wide form: the clock at 19:55 in the reader's
 * own format, at the hero's size, and [RowFitSlack]. */
internal fun skyWordsMin(context: Context): Dp {
    val clock = LocalTime.of(19, 55).format(
        Formats.timeFormatter(DateFormat.is24HourFormat(context), Locale.getDefault())
    )
    return measureWidgetText(context, clock, SkyTimeSp, medium = true) + RowFitSlack
}

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
/** The tall card's block: the hero row, and under it [rows] moments with their gaps. */
internal fun skyTallBlockHeight(fontScale: Float, rows: Int): Dp {
    val words = textLineHeight(SkyTimeSp, fontScale) +
        maxOf(textLineHeight(SkyNameSp, fontScale), SkyMarkChip) +
        textInkBalance(SkyTimeSp, fontScale)
    val hero = maxOf(SkyTallHeroIcon, words)
    if (rows <= 0) return hero
    val row = maxOf(SkyRowHeight, textLineHeight(SkyCompactSp, fontScale))
    return hero + SkyListGap + row * rows + SkyRowGap * (rows - 1)
}

/**
 * **The arc under the moments** (widget review, 13 ott 2026): the height left on a tall
 * card once the block is paid, or null when it is less than [SkyPlotMin]. The moments card
 * centred its block in whatever it did not fill, and with the three moments a reader
 * typically follows a four-by-four was two thirds empty. Where the arc fits, the block
 * goes to the top and the day's arc takes the rest, all of it: capped at the arc card's own
 * 150 dp it only moved the air from the bottom to the middle (looked at, 13 ott 2026).
 */
internal fun skyPlotHeight(size: DpSize, fontScale: Float, rows: Int): Dp? {
    val room = size.height - WidgetCardPaddingSnug - WidgetCardPadding -
        skyTallBlockHeight(fontScale, rows) - SkyPlotGap
    return room.takeIf { it >= SkyPlotMin }
}

/** The smallest arc worth drawing under the moments: a plot of about 50 dp with its hours. */
internal val SkyPlotMin = 72.dp
internal val SkyPlotGap = 12.dp


internal const val SkyTimeSp = WidgetType.Display

/** The name under the clock: the Now widget's place line, one step down because it
 * carries a day marker before it («Domani · Sorge la luna») and the column is finite. */
internal const val SkyNameSp = WidgetType.Body

/** Chip text: the scale's [WidgetType.Label], Medium (widget review, 13 ott 2026, from
 * 11). It was 11 because «Presto per dirlo» had to fit a fixed 96 dp column; the column is
 * measured now ([skyVerdictColumn]), and the verdict — the card's answer — is no longer
 * the smallest type on it. Only the hero wears a chip: a row's verdict is a note. */
internal const val SkyChipSp = WidgetType.Label

/** The chip's sides, and the air between its mark and its word: the app's chip grammar,
 * mark then word (`VerdictChip`), at a home-screen card's size. */
internal val SkyChipPadH = 7.dp
internal val SkyChipTextGap = 4.dp

/** «Pioggia 100%», the widest evidence line, at [SkyEvidenceSp]: 78.5 dp. */
internal val SkyEvidenceMin = 80.dp

/** A row's verdict note sits in a slot this wide on every row of a card that has one,
 * so the clocks stand in a column: the arc's agenda's own 14 dp. */
internal val SkyNoteSlot = 14.dp

/** The mark's drawing inside its 22 dp container: 12 dp, the size the glyph had as a
 * character, and the same the arc widget's rows use. */
internal val SkyMarkGlyph = 12.dp

/** The number under the word chip, at the scale's [WidgetType.Label] (from 14): «Pioggia
 * 100%» is 78.5 dp at this size. */
internal const val SkyEvidenceSp = WidgetType.Label

/** A compact row's two texts share one size, so their baselines meet under centre
 * alignment (device finding, 4 set). */
internal const val SkyCompactSp = WidgetType.Label

internal val SkyHeroIconMax = 72.dp
internal val SkyTallHeroIcon = 60.dp

/** The verdict block's width where nothing has been measured — the tables' reference, in
 * the product's own language: «Presto per dirlo» with its mark, 120 dp, and the slack.
 * Fixed rather than weighted because a chip does not wrap; the card itself measures
 * ([skyVerdictColumn]). */
internal val SkyVerdictColumn = 124.dp

/** The narrowest words' column worth the wide form, in the same reference: «19:55» at
 * 30 sp is 80 dp with its slack, and the reference four-cell card gives 108. A three-cell
 * card gives 18 and stays narrow. The card measures its own clock ([skyWordsMin]). */
internal val SkyWordsMin = 84.dp

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
