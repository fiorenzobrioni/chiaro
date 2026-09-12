package com.callbackdev.chiaro.widget

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * The three forms of the Now widget, and the grant that picks one (committente, 8 set
 * 2026, with the launcher's own weather widget beside ours at four sizes for reference).
 *
 * - [NARROW] is the one-row card at two or three cells: the glyph, the temperature and
 *   the place, nothing else — there is no honest room for a sentence beside them.
 * - [WIDE] is the one-row card at four cells or more: the same row, and the day's
 *   sentence against the far edge, right-aligned and centred on the row.
 * - [TALL] is any card two rows high: the glyph alone in the top corner, and under it
 *   the temperature, the sentence and the place stacked against the leading edge.
 *
 * This reverses a decision of 4 set: the sky's state used to be a per-widget switch
 * precisely so a card would not rewrite itself while its handles were being dragged.
 * Living with it beside a widget that reflows showed the opposite is what a reader
 * expects — a card that shows what fits at the size it was given is the launcher's own
 * grammar — so the choice moved from a setting to the grant, and the setting went.
 *
 * Everything here is arithmetic on dp so it can be pinned by a table
 * (`NowWidgetLayoutTest`) rather than by a screenshot of one phone's idea of a cell.
 */
internal enum class NowLayout { NARROW, WIDE, TALL }

internal fun nowLayout(size: DpSize): NowLayout = when {
    size.height >= TallMinHeight -> NowLayout.TALL
    nowSentenceColumnWidth(size) >= SentenceColumnMin -> NowLayout.WIDE
    else -> NowLayout.NARROW
}

/**
 * Two rows on any launcher grid this has been measured on: one row lands between
 * ~82 and ~101 dp depending on how the host pads its cells, two rows never under 170.
 */
internal val TallMinHeight = 150.dp

/**
 * The hero glyph of a one-row card: it fills the height the launcher granted, as it
 * always has — and, since 8 set, never more of the width than leaves the words their
 * minimum. On a two-cell card (~159 dp on the reference device) a height-sized glyph
 * left the place name ~65 dp, which is «Dergan…»; the words' column now keeps
 * [WordsColumnMin] where it can, and the glyph takes what is left down to its own
 * floor — 56 dp on that card, 68 on a 178 dp two-cell grid, against the ~70 the height
 * alone would give. Three cells and up were height-bound until [RowIconMax].
 */
internal fun nowRowIconSize(size: DpSize): Dp {
    val byHeight = size.height - WidgetCardPaddingSnug * 2
    val byWidth = size.width - WidgetCardPaddingLeading - IconTextGap -
        WidgetCardPaddingTrailing - WordsColumnMin
    return heroIconSize(minOf(byHeight, byWidth), min = RowIconMin, max = RowIconMax)
}

/**
 * What the words' column of a one-row card must keep: «−12°» at 34 sp Medium is ~72 dp
 * and a ten-letter place with its position pin ~84, so 84 is where a name stops being
 * readable rather than where it stops being complete.
 */
internal val WordsColumnMin = 84.dp

/** A squeezed grant stays legible: below this the Meteocons art loses its detail. */
internal val RowIconMin = 56.dp

/**
 * **The glyph never outgrows the block of words beside it** (committente, 12 set 2026,
 * from a device: "the widget's icon is a bit big"). The block is the temperature's line
 * over the place's, and it is arithmetic already in this file:
 * `34 × 1.32 + 16 × 1.32 = 66` ([textLineHeight] of [TemperatureSp] and [PlaceSp]).
 *
 * Until here the one-row glyph was bound only by the height, and a launcher that grants
 * a taller row made it bigger with nothing beside it growing: on the reference device's
 * four-cell card (measured off the screenshot at 2.8125 px/dp — 1080 px over a 384 dp
 * screen, 16 dp of tile inset reading 45 px and the 12 dp gutter 34) the row is ~85 dp,
 * so the glyph was **73 dp** against a 66 dp block of words, and 89 dp on the 101 dp row
 * the other launcher grants. The cap takes the reference card to 66 — about 10% off the
 * drawing, 50.5 → 45.5 dp of ink on the family's 0.69 geometric mean — and makes every
 * one-row card from ~78 dp of height upwards draw the same glyph, which is the property
 * the height alone never had.
 *
 * The Sky widget capped its own row glyph for the same reason on 8 set
 * ([com.callbackdev.chiaro.widget.SkyHeroIconMax], 72 dp against a 30 sp clock); this is
 * the Now card's version of that number, read off its own anchor.
 *
 * A plain constant and not a function of the reader's font scale: a larger scale grows
 * the words, but the card has no more height to give the glyph anyway — `byHeight` is
 * already the binding constraint there — so following the scale would only move a number
 * that nothing reads. Narrow cards are untouched: at two cells the width binds first.
 */
internal val RowIconMax = 66.dp

/**
 * The width each of the two text columns gets on a [NowLayout.WIDE] card: the row's
 * slack after the glyph, split evenly between the temperature block and the sentence.
 * Even rather than measured because Glance cannot measure text, and even is what the
 * reference widget does — its description block is about as wide as its number block,
 * and the empty space lands in the middle where the eye expects it.
 *
 * Negative when the card is too narrow to hold both; the caller compares, never draws.
 */
internal fun nowSentenceColumnWidth(size: DpSize): Dp {
    val words = size.width - WidgetCardPaddingLeading - nowRowIconSize(size) -
        IconTextGap - WidgetCardPaddingTrailing
    return (words - SentenceGap) / 2
}

/**
 * The sentence's room on a one-row card laid the other way round
 * ([WidgetArrangement.ICON_END]): the glyph in the trailing corner, and on the leading
 * side the number with the sentence at its shoulder — the tall card's composition
 * pressed into one row (committente, 8 set 2026, afternoon). The words take the words'
 * inset on the leading edge and the glyph the glyph's on the trailing one, the same two
 * numbers as the standard row on swapped edges; the sentence gets what is left after
 * the number's own column ([TemperatureColumnMin]) and the gap between them. Compared
 * against [SentenceColumnMin] like the standard row's column: 170 dp on the reference
 * four-cell card, 150 on a five-column grid's four cells, 80 on three cells — where the
 * sentence stays home, exactly as it does the other way round.
 */
internal fun nowMirroredSentenceWidth(size: DpSize): Dp =
    size.width - WidgetCardPaddingTrailing - TemperatureColumnMin - SentenceGap -
        IconTextGap - nowRowIconSize(size) - WidgetCardPaddingLeading

/** What the number needs beside a sentence: «−12°» at 34 sp Medium is ~62 dp. */
internal val TemperatureColumnMin = 66.dp

/**
 * The narrowest column worth a sentence: 96 dp is about twelve characters of 16 sp,
 * and three lines of twelve hold every sentence the widget can say in its brief
 * register (measured with the system font: «Pioggia gelata verso le 15:00» wraps to
 * three at 96 dp and fits). On the reference device a four-cell card (~340 dp) gives
 * each column ~118 dp — where every brief sentence takes two lines — and a three-cell
 * card (~250 dp) ~73, so the threshold sits well clear of both.
 */
internal val SentenceColumnMin = 96.dp

/**
 * How many lines the sentence may take on a one-row card: as many 16 sp lines as the
 * height really holds, capped at three. The third line is the safety net for the
 * narrowest column that qualifies, and the row has the height for it — three lines are
 * ~63 dp against the ~70 the card leaves — so the cap is the sentence's, not the card's.
 * A reader's larger font scale lowers the count rather than overflowing.
 */
internal fun nowSentenceLines(
    size: DpSize,
    fontScale: Float,
    withWarning: Boolean = false
): Int {
    val room = size.height - WidgetCardPaddingSnug * 2 - warningBlock(fontScale, withWarning)
    return (room / textLineHeight(SentenceSp, fontScale)).toInt().coerceIn(1, RowSentenceMaxLines)
}

/** What a drawn chip costs a budget: its own box and the air above it, or nothing. */
internal fun warningBlock(fontScale: Float, drawn: Boolean): Dp =
    if (drawn) warningChipHeight(fontScale) + WarningChipGap else 0.dp

/**
 * Whether a one-row card's sentence column has room for the warning chip on a line of
 * its OWN — a line more than it is already using, which is what
 * [com.callbackdev.chiaro.widget.WarningSlot.OWN_ROW] means. The column must still hold
 * a line of sentence where one is drawn: a chip that pushed the sentence out would not
 * be an extra line, it would be a swap the reader did not ask for. On the reference
 * 82 dp row that leaves 70 dp against the 45.6 a line and a chip need, so a wide card
 * carries both; the narrow form has no column at all and never gets here.
 */
internal fun nowRowHasWarningRow(size: DpSize, fontScale: Float, withSentence: Boolean): Boolean {
    val room = size.height - WidgetCardPaddingSnug * 2
    val needed = (if (withSentence) textLineHeight(SentenceSp, fontScale) else 0.dp) +
        warningChipHeight(fontScale) + WarningChipGap
    return room >= needed
}

internal const val RowSentenceMaxLines = 3

/** On a tall card the sentence takes two lines, as the reference does: the third line
 * would cost the glyph 21 dp, and at the ~131 dp a two-cell card leaves the words every
 * brief sentence fits two lines of 16 sp (measured — which is why «per il resto della
 * giornata» has a brief form, «del giorno»: the full one needed three). */
internal const val TallSentenceMaxLines = 2

/**
 * The hero glyph of a [NowLayout.TALL] card: what the height leaves once the text
 * block under it is paid for, plus the one band the two may share.
 *
 * The text block is the temperature's line, the sentence's two, the place's, and the
 * stale marker's when there is one — each line the box its TextView really occupies
 * ([textLineHeight]). The shared band is [textInkBalance]: the system font leaves about
 * a quarter of an em empty above the temperature's capitals, and the glyph leaves 9 to
 * 12 dp of its own box empty at the bottom (measured over the family, see
 * [WidgetCardPaddingLeading]), so the glyph's box may sink into that leading without
 * any ink meeting. On the reference device's two-by-two (~159 × 189 dp) this puts the
 * glyph at ~69 dp — about 50 dp of drawing against the neighbour's 64 — where a plain
 * stack would have left 61. (It was ~75 at 14 and 15 sp; the two extra points of text
 * the committente asked for on 8 set are paid here, and knowingly.)
 */
internal fun nowTallIconSize(
    size: DpSize,
    fontScale: Float,
    stale: Boolean,
    withSentence: Boolean = true,
    withWarning: Boolean = false
): Dp = heroIconSize(nowTallIconRoom(size, fontScale, stale, withSentence, withWarning))

/**
 * The same arithmetic before the family's floor and ceiling are applied. The RAW number
 * is what [nowTallHasWarningRow] has to see: `heroIconSize` clamps at 52 dp, so a card
 * that has already run out of room and one that has 20 dp to spare come back identical.
 */
internal fun nowTallIconRoom(
    size: DpSize,
    fontScale: Float,
    stale: Boolean,
    withSentence: Boolean,
    withWarning: Boolean
): Dp {
    val text = textLineHeight(TemperatureSp, fontScale) +
        (if (withSentence) textLineHeight(SentenceSp, fontScale) * TallSentenceMaxLines else 0.dp) +
        textLineHeight(PlaceSp, fontScale) +
        (if (stale) textLineHeight(StaleSp, fontScale) else 0.dp) +
        warningBlock(fontScale, withWarning)
    return size.height - WidgetCardPaddingSnug - WidgetCardPadding - text +
        textInkBalance(TemperatureSp, fontScale)
}

/**
 * Whether a tall card can give the chip a line of its own: what the budget leaves once
 * the chip is paid for, against the glyph's own floor. On the reference two-by-two
 * (159 × 189 dp) with the sentence on it does NOT — 68.96 dp of room minus the chip's
 * 24.5 is 44.5, under the family's 52 — and the yellow chip stays home rather than
 * pushing the glyph below the size at which Meteocons stops being legible. Turn the
 * sentence off on that same card and the two lines it gives back pay for the chip twice
 * over. A section that does not fit is not drawn: the Today widget's own rule.
 */
internal fun nowTallHasWarningRow(
    size: DpSize,
    fontScale: Float,
    stale: Boolean,
    withSentence: Boolean
): Boolean =
    nowTallIconRoom(size, fontScale, stale, withSentence, withWarning = true) >= HeroIconFloor

/** The floor `heroIconSize` clamps to, named here so the budget above can compare
 * against it rather than against a number it cannot see. */
internal val HeroIconFloor = 52.dp

/** The hero number's size, named because two budgets are measured off it. */
internal const val TemperatureSp = 34f

/**
 * The sentence and the place share one size, 16 sp, and differ by weight and ink: the
 * sentence Medium in the strong ink (the second thing read after the number), the place
 * Regular in the quiet one. **16 since 8 set 2026** (committente, with the screenshot
 * beside the launcher's own widget, whose description and place both sit at ~17 sp
 * under a ~30 sp number): they were 14 and 15, and read small. Sixteen under a 34 sp
 * hero keeps the reference's ratio, and it is the size at which every brief sentence
 * still takes two lines in a four-cell column and «Cavenago di Brianza» (145 dp,
 * measured) still fits the 154 dp a three-cell card leaves the words.
 */
internal const val SentenceSp = 16f
internal const val PlaceSp = 16f
internal const val StaleSp = 11f

/**
 * 8 dp between glyph and words, not 12: the glyph leaves 9 to 12.5 dp of its own box
 * empty on that side too, so the gap the eye reads is 17 to 20 (5th device pass).
 */
internal val IconTextGap = 8.dp

/** The air between the temperature block and the sentence: the card's own 12. */
internal val SentenceGap = 12.dp
