package com.callbackdev.chiaro.widget

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * The text widget's forms and its type scale (committente, 19 set 2026: «un widget 4x1,
 * ridimensionabile, con informazioni solo testuali — niente icona meteo o altra grafica;
 * una tipografia ordinata e leggibile con le info principali in primo piano»).
 *
 * The other four cards are drawings with words beside them: the glyph fills the height
 * the launcher grants and the words arrange themselves around it. Take the drawing away
 * and the card has no hierarchy left — so this one builds it out of type alone, in four
 * ranks that differ by size AND weight AND ink, never by one of the three on its own:
 *
 * | rank | what | size | weight | ink |
 * |---|---|---|---|---|
 * | 1 | the temperature | [TextHeroFloor]…[TextHeroMax], scaled to the grant | Bold | strong |
 * | 2 | the day's sentence | [TextSentenceSp] | Medium | strong |
 * | 3 | the place, the day's range, the warning's word | [TextFactSp] | Regular (Medium for the warning) | quiet (strong for the warning) |
 * | 4 | the stale marker, the hour labels | [TextStaleSp] | Regular | the freshness ink for the marker, the quiet one for the labels |
 *
 * **The number is sized by the grant, the way the other cards size their glyph.** A fixed
 * hero could only ever be right on one cell size; [heroIconSize] solves that for a drawing
 * and this file solves the same problem for a figure, with the same shape of answer — what
 * the height leaves once the lines around it are paid, between a floor and a ceiling.
 *
 * Three forms, picked by the grant like [NowLayout]'s:
 *
 * - [TextForm.LINE] — one row too narrow for two columns: the place, the number, the
 *   stale marker, and nothing else.
 * - [TextForm.ROW] — one row with a second column, which is the default 4×1 placement and
 *   also what three cells buys here: the place over the number on the leading side, and
 *   against the far edge the day's sentence with the warning and the day's range under it.
 *   Three cells is one cell better than the Now widget manages, and for a plain reason —
 *   the 66 dp of glyph and its gap that card spends before the first letter, this one does
 *   not have.
 * - [TextForm.STACK] — two rows and up: one centred column, place over number over
 *   sentence over the facts, and the next hours as figures where the height holds them.
 *
 * Everything here is arithmetic on dp and sp so a table can pin it
 * (`TextWidgetLayoutTest`) rather than a screenshot of one launcher's idea of a cell. The
 * reference figures below are the ones the other layouts are measured against: a one-row
 * card is ~85 dp tall (and ~101 on the other launcher seen to grant one), two cells are
 * ~159 dp wide, three ~250, four ~340, and a four-by-two is ~340 × 189.
 */
internal enum class TextForm { LINE, ROW, STACK }

internal fun textForm(size: DpSize): TextForm = when {
    size.height >= TallMinHeight -> TextForm.STACK
    textSentenceColumn(size) >= TextSentenceColumnMin -> TextForm.ROW
    else -> TextForm.LINE
}

/**
 * The leading column of a one-row card: a share of the row's slack, and never less than
 * [TextWordsMin]. A share rather than the even split the Now widget uses, because the two
 * columns are not doing the same work here — the leading one holds a number and a name,
 * both of them short, and the trailing one holds prose, which is the thing that needs
 * measure. 0.42 leaves the reference four-cell card 126 dp against 174 (the Now widget
 * gives its two columns 118 each), and the floor is what carries the narrow cards.
 */
internal fun textLeadingColumn(size: DpSize): Dp {
    val words = size.width - WidgetCardPadding * 2 - SentenceGap
    return maxOf(words * TextLeadingShare, TextWordsMin)
}

/** What is left for the sentence once the leading column and the gap are paid. Negative
 * on a card too narrow to hold both; the caller compares, never draws. */
internal fun textSentenceColumn(size: DpSize): Dp =
    size.width - WidgetCardPadding * 2 - SentenceGap - textLeadingColumn(size)

private const val TextLeadingShare = 0.42f

/**
 * What the leading column must keep: «−12°» at the hero's floor is ~63 dp and a ten-letter
 * place at [TextFactSp] ~73, so 96 is where the column stops being able to hold the number
 * AND a name worth reading — the Now widget's [WordsColumnMin] argument, at this card's
 * sizes.
 */
internal val TextWordsMin = 96.dp

/**
 * The narrowest column worth a sentence: [SentenceColumnMin] is the household's number for
 * 16 sp, and this card sets prose one point larger, so it asks for one step more room.
 * 104 dp is about eleven characters of 17 sp, and three lines of eleven hold every
 * sentence the brief register can say. The reference three-cell card gives 114 and
 * qualifies; two cells give 23 and do not.
 */
internal val TextSentenceColumnMin = 104.dp

/**
 * The hero on a one-row card: what the height leaves once the place's line and the stale
 * marker's are paid, capped at [TextHeroRowMax].
 *
 * The stale marker sits in THIS column rather than under the sentence, and pays for itself
 * out of the number: a card whose data is old says so before it says anything else loudly,
 * and on the reference 85 dp row that still leaves the floor exactly (30.3 sp). The number
 * is 41 sp on the same row when the data is fresh, against the 34 the Now widget prints
 * beside its glyph.
 *
 * The column it must not overflow is [textLeadingColumn] where there is a second column to
 * share the row with, and the whole width where there is not: the narrow form has no
 * trailing column, so pretending it did would hand the number 96 dp on a card that only has
 * 82 — and «−12°» at 41 sp is 87.
 */
internal fun textRowHeroSp(size: DpSize, fontScale: Float, stale: Boolean): Float {
    val room = size.height - WidgetCardPaddingSnug * 2 -
        textLineHeight(TextFactSp, fontScale) -
        (if (stale) textLineHeight(TextStaleSp, fontScale) else 0.dp)
    val column = if (textForm(size) == TextForm.LINE) {
        size.width - WidgetCardPadding * 2
    } else {
        textLeadingColumn(size)
    }
    return heroSp(room, column, fontScale, TextHeroRowMax)
}

/**
 * **The number never outgrows the block beside it** — the rule [RowIconMax] states for the
 * Now widget's glyph, read off this card's own anchor. The trailing column of a one-row
 * card carries two lines of sentence and a line of fact, which is 2 × 22.44 + 18.48 ≈
 * 63 dp; 44 sp has a line box of 58, and 48 would already be taller than everything it
 * stands next to. Without the cap the 101 dp row the other launcher grants would print a
 * 53 sp number beside a 17 sp sentence, which is a poster, not a hierarchy.
 */
internal const val TextHeroRowMax = 44f

/**
 * The trailing column of a one-row card, in the order it fills: one line of sentence
 * first — the column exists for the sentence, and a column that opened with a footnote
 * would be a different card — then the warning's word, then the day's range, then as many
 * more lines of sentence as what is left will hold, up to [TextRowSentenceMaxLines].
 *
 * The sentence grows LAST for a reason: the third line is a safety net for a narrow
 * column, and a net that had already taken the room a warning needs would catch nothing
 * worth catching. On the reference 85 dp row a card with neither fact reads three lines,
 * one fact reads two, and a card with both a warning and a range reads one.
 */
internal data class TextRowPlan(
    val sentenceLines: Int,
    val showWarning: Boolean,
    val showRange: Boolean
)

internal fun textRowPlan(
    size: DpSize,
    fontScale: Float,
    sentence: Boolean,
    warning: Boolean,
    range: Boolean
): TextRowPlan {
    val line = textLineHeight(TextSentenceSp, fontScale)
    val fact = textLineHeight(TextFactSp, fontScale)
    var room = size.height - WidgetCardPaddingSnug * 2
    var lines = 0
    if (sentence && room >= line) {
        room -= line
        lines = 1
    }
    val showWarning = warning && room >= fact
    if (showWarning) room -= fact
    val showRange = range && room >= fact
    if (showRange) room -= fact
    while (lines in 1 until TextRowSentenceMaxLines && room >= line) {
        room -= line
        lines++
    }
    return TextRowPlan(lines, showWarning, showRange)
}

internal const val TextRowSentenceMaxLines = 3

/**
 * Whether a one-row card has a line to spare for the warning's word — the `ownRow` half of
 * [warningSlot], asked of the card the reader would have WITHOUT the warning on it. The
 * layout depends on the budget and the budget on the layout, and one of the two has to go
 * first; the Today widget unties the same knot the same way.
 */
internal fun textRowHasFactLine(size: DpSize, fontScale: Float, sentence: Boolean): Boolean =
    textRowPlan(size, fontScale, sentence, warning = true, range = false).showWarning

/**
 * The plan for a two-row card: what the height buys, in the order it buys it.
 *
 * The order IS the hierarchy, and it is the whole of this widget's layout argument. The
 * number is reserved first at [TextHeroStackFloor] — on a tall card it is the page's
 * title, and a title that shrank so a footnote could fit would be the card arguing with
 * itself. Then the sentence's two lines, then the warning's word, then the day's range,
 * then the hours; whatever is left over grows the number, up to [TextHeroMax]. A section
 * that does not fit is not drawn (`DESIGN` §1.1) — the Today widget's own rule for its
 * rain row, applied to four things instead of one.
 *
 * Two lines of sentence and never three, which is the Now widget's tall card's number
 * ([TallSentenceMaxLines]) and its argument too: there the third line would have cost the
 * glyph 21 dp, here it costs the number 17 sp. On the reference four-by-two it is the
 * difference between a 56 sp hero and a 43 sp one, for a line the brief register almost
 * never needs at 340 dp of measure.
 *
 * The place and the stale marker are never in the order, because they are never optional:
 * a card that dropped its place would be a number about nowhere, and one that dropped its
 * age would be lying about how old it is (VISION §5.9).
 *
 * The sentence's lines are RESERVED, not measured — Glance cannot measure text — so a
 * one-line sentence leaves its second line as air. The column is centred on the card
 * ([TextWidget]'s own composition), so that air lands evenly above and below the block
 * rather than as a hole in the middle of it, which is the Sky widget's finding about its
 * own short lists (committente, 4 set).
 */
internal data class TextStackPlan(
    val heroSp: Float,
    val sentenceLines: Int,
    val showWarning: Boolean,
    val showRange: Boolean,
    val showHours: Boolean
)

internal fun textStackPlan(
    size: DpSize,
    fontScale: Float,
    stale: Boolean,
    sentence: Boolean,
    warning: Boolean,
    range: Boolean,
    hours: Boolean
): TextStackPlan {
    val fact = textLineHeight(TextFactSp, fontScale)
    val line = textLineHeight(TextSentenceSp, fontScale)
    var room = size.height - WidgetCardPadding * 2 - fact -
        (if (stale) textLineHeight(TextStaleSp, fontScale) else 0.dp)
    val floor = textLineHeight(TextHeroStackFloor, fontScale)
    room -= floor
    var lines = 0
    while (sentence && lines < TextStackSentenceMaxLines && room >= line) {
        room -= line
        lines++
    }
    val showWarning = warning && room >= fact
    if (showWarning) room -= fact
    val showRange = range && room >= fact
    if (showRange) room -= fact
    val showHours = hours && room >= textHoursHeight(fontScale) + TextHoursGap
    if (showHours) room -= textHoursHeight(fontScale) + TextHoursGap
    return TextStackPlan(
        heroSp = heroSp(
            floor + room.coerceAtLeast(0.dp),
            size.width - WidgetCardPadding * 2,
            fontScale,
            TextHeroMax
        ),
        sentenceLines = lines,
        showWarning = showWarning,
        showRange = showRange,
        showHours = showHours
    )
}

internal const val TextStackSentenceMaxLines = 2

/** [textRowHasFactLine]'s answer for a tall card, asked the same way and for the same
 * reason: the plan without the warning says whether there is a line for one. */
internal fun textStackHasFactLine(
    size: DpSize,
    fontScale: Float,
    stale: Boolean,
    sentence: Boolean
): Boolean = textStackPlan(
    size, fontScale, stale, sentence, warning = true, range = false, hours = false
).showWarning

/**
 * The number's size, given the height it may occupy and the column it must not overflow.
 * [textSizeForLine] inverts the line box; the width guard is the other half of
 * [nowRowIconSize]'s `minOf(byHeight, byWidth)`, at this card's units — the widest
 * temperature this app prints is «−12°», about [TempEmWidth] ems of the system font at the
 * weight this card sets it in, so a column of *w* dp carries at most `w / (2.1 × fontScale)`
 * sp of it.
 */
private fun heroSp(room: Dp, column: Dp, fontScale: Float, max: Float): Float {
    val byWidth = column.value / (TempEmWidth * fontScale.coerceAtLeast(0.1f))
    return minOf(textSizeForLine(room, fontScale), byWidth)
        .coerceIn(TextHeroFloor, max)
}

/** «−12°»: a minus (~0.55 em), two digits (~0.57 each) and a degree sign (~0.4) of Roboto
 * Bold, rounded up so the guard errs towards the smaller number. */
private const val TempEmWidth = 2.1f

/** Below this a temperature stops being a hero and becomes just another line; the
 * reference 85 dp row with a stale marker on it lands at 30.3, so the floor is what a
 * squeezed one-row card falls back to rather than a number anything usually hits. */
internal const val TextHeroFloor = 30f

/** A tall card reserves this much for its number before anything optional is paid: the
 * rank-1 line of a page, not what is left after the footnotes. */
internal const val TextHeroStackFloor = 40f

/** The ceiling, and the reason there is one: past this a temperature stops being read as
 * a number and starts being read as an ornament, and the card is meant to be typography,
 * not a poster. 56 sp is four lines of [TextFactSp] tall. */
internal const val TextHeroMax = 56f

/**
 * Rank 2, the day's sentence: 17 sp Medium in the strong ink, one point above the
 * household's 16. The other cards put the sentence beside a drawing, and the drawing is
 * what carries the card at arm's length; here the sentence IS the second thing to read,
 * so it takes the step the glyph's absence pays for. One point and not two: at 18 the
 * reference four-cell card's 174 dp column stops holding «Pioggia gelata verso le 15:00»
 * in two lines.
 */
internal const val TextSentenceSp = 17f

/** Rank 3, the facts: the place, the day's high and low, the warning's word and the hours'
 * own temperatures. 14 sp — the app's own `bodyMedium`, and far enough under 17 that the
 * eye sorts the two without having to compare them. Inside the rank the ink and the weight
 * still do their work: the place is Regular in the quiet ink, the figures Medium in the
 * strong one. */
internal const val TextFactSp = 14f

/** Rank 4, the footnotes: the stale marker and the hour labels, at the household's own
 * stale size. The marker keeps the freshness ink it wears on every other card; the labels
 * take the quiet one, like the Today widget's strip. */
internal const val TextStaleSp = 11f

/** The hours' temperatures are rank 3, not a fifth size: they are the thing the row is
 * for, and a row of figures set smaller than the label over them would read as a legend
 * rather than as a forecast. Named separately only so the budget below can say which line
 * it is measuring. */
internal const val TextHourTempSp = TextFactSp

/** How many hours a tall card prints, by the width it has. A cell is «−12°» at
 * [TextHourTempSp] (~29 dp) with air either side; six is the ceiling because the strip is
 * one Glance container and Glance draws at most ten children per container (the Today
 * widget lost the last two hours of its own strip to that rule, Fase 11), and three is the
 * floor because fewer is no longer a stretch of the day. */
internal fun textHourCells(width: Dp): Int =
    ((width - WidgetCardPadding * 2 + TextHourCellGap) / (TextHourCellMin + TextHourCellGap))
        .toInt()
        .coerceIn(TextHourCellsFloor, TextHourCellsCeiling)

internal val TextHourCellMin = 36.dp
internal val TextHourCellGap = 6.dp
internal const val TextHourCellsFloor = 3
internal const val TextHourCellsCeiling = 6

/** The hours block: the label's line over the temperature's, with the hairline of air
 * between them that keeps the two from reading as one word. */
internal fun textHoursHeight(fontScale: Float): Dp =
    textLineHeight(TextStaleSp, fontScale) + TextHoursInnerGap +
        textLineHeight(TextHourTempSp, fontScale)

internal val TextHoursInnerGap = 2.dp

/** The air between the block of prose and the figures under it: enough that the hours
 * read as a second thing, not as the sentence's last line. */
internal val TextHoursGap = 10.dp
