package com.callbackdev.chiaro.widget

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * The three forms of the Now widget, and the grant that picks one (committente, 8 set
 * 2026, with the launcher's own weather widget beside ours at four sizes for reference).
 *
 * - [NARROW] is the one-row card at two or three cells: the place over the glyph and the
 *   temperature, nothing else — there is no honest room for a sentence beside them.
 * - [WIDE] is the one-row card at four cells or more: the same column, and the day's
 *   sentence against the far edge, right-aligned and centred on the row. Both are laid
 *   out like the text card's one-row forms since 15 nov 2026 ([nowRowIsWide]).
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
    nowRowIsWide(size) -> NowLayout.WIDE
    else -> NowLayout.NARROW
}

/**
 * Whether the card carries the hours: two rows tall and three cells wide (13 ott 2026, the
 * widget review). There the two widgets that were «At a glance» and «The hours ahead» are
 * one: the first's forms below this size, the second's from it. Three cells is ~210 dp on
 * every grid measured (the arc's own threshold), where the strip still holds four hours.
 */
internal fun nowShowsHours(size: DpSize): Boolean =
    size.height >= TallMinHeight && size.width >= HoursMinWidth

internal val HoursMinWidth = 210.dp

/**
 * Two rows on any launcher grid this has been measured on: one row lands between
 * ~82 and ~101 dp depending on how the host pads its cells, two rows never under 170.
 */
internal val TallMinHeight = 150.dp

/**
 * **The one-row card is the «In words» card with the glyph in front of the number**
 * (committente, 15 nov 2026, from the home screen with the two four-by-one cards one over
 * the other: «Si potrebbe fare uguale a quello di "In parole" con in più l'icona meteo a
 * sinistra della temperatura? Lo vorrei proprio uguale nel layout, compreso la parte
 * tipografia», and then: «se usiamo lo stesso identico layout e spostiamo solo la
 * temperatura a destra per lasciare lo spazio per l'icona meteo il nome della località non
 * lo tocchiamo»).
 *
 * Until then this card drew the glyph the height of the row, the temperature at a fixed
 * 34 sp over the place, the high and low as «9° / 7°», and split the row between its two
 * text columns by measuring both. Beside the text card it read as the weaker of the two:
 * the place under the number reads as a caption, the number was small against a 66 dp
 * drawing, and the slash between two temperatures says nothing the arrows do not say
 * better. Now every number on the row is the text card's ([TextWidgetLayout]):
 *
 * - the place on top, at [TextFactSp], in the column [textLeadingColumn] sizes — the same
 *   168 dp that prints «Cavenago di Brianza» whole;
 * - under it the number, sized to the grant by the same arithmetic as the text card's
 *   ([textRowHeroSp]'s room), Bold, with the stale marker under it when the data is old;
 * - against the far edge the sentence, the warning and the day's range with its ↑ ↓,
 *   filled in the text card's order ([textRowPlan]).
 *
 * **What the glyph takes is the number's line, and nothing else.** It stands in front of
 * the number at the number's own line box (39.3 sp is 51.9 dp on the reference 85 dp row),
 * so the place keeps its whole column and the sentence keeps its whole measure; the number
 * moves right by the glyph, its inset and its gap, which the 168 dp column has room for at
 * every number this app prints (5 + 52 + 8 + 83 for «−12°» is 148). The one thing that is not the
 * text card's is the warning: here it is the coloured chip, as on every card with a drawing
 * on it, and the column pays the chip's box for it ([warningBlock]).
 *
 * The card's leading inset is the glyph's 4 dp ([WidgetCardPaddingLeading]) and every line
 * of words pays back [NowGlyphEdgeGive], so the place's first letter stands 14 dp in, as
 * the text card's words do; the glyph stands [NowGlyphShift] further in than the inset, so
 * a cloud's ink meets that same edge.
 */
internal fun nowRowIsWide(size: DpSize): Boolean {
    if (textForm(size) != TextForm.ROW) return false
    // At the reference font scale, and fresh: the form is the grant's, never the data's
    // age or the reader's font size, so a card cannot change shape while it is looked at.
    val bare = nowRowBareSp(size, 1f, stale = false, column = textLeadingColumn(size))
    return nowGlyphLineSp(size, 1f, stale = false, wide = true) >= bare
}

/**
 * The width the number's line has: the leading column where there is a far edge to share
 * the row with, the whole row where there is not, plus what the glyph's inset gives back.
 */
internal fun nowHeroLine(size: DpSize, wide: Boolean): Dp =
    (if (wide) textLeadingColumn(size) else size.width - WidgetCardPadding * 2) + NowGlyphEdgeGive

/**
 * What a words' edge gives a glyph's: the card's leading inset is 4 dp because the
 * drawings carry 9 to 12.5 dp of margin of their own, and every line that is words pays
 * the other 10 back, so the place starts where the text card's place starts.
 */
internal val NowGlyphEdgeGive = WidgetCardPadding - WidgetCardPaddingLeading

/**
 * How far in from the card's leading inset the row's glyph stands (committente, 16 nov 2026:
 * «mi sembra un po' troppo vicina al bordo di sinistra»).
 *
 * The 4 dp inset counts on the margin a drawing keeps inside its own box, and that margin is
 * a share of the box: 7.5% for the drizzle, 8.3% for the snow, 9.2% for the cloud and the
 * rain, 15.5% for the sun (measured on the shipped drawings, drawn at 400 px). On the 66 dp
 * glyph the card had until 15 nov 2026 the sun's ink stood 14 dp in; on the 52 dp glyph the
 * row has since then a cloud's stood 8.8 dp in and the sun's 12, against the 14 dp of the
 * place over them. Five more put the cloud's ink at 13.8, the snow's at 13.3, the sun's at 17: the
 * widest drawings, the ones that read as touching the edge, land on the words' edge.
 *
 * One number for every drawing, not each drawing's own margin: the temperature must not step
 * sideways when the weather changes. Paid out of the number's line, which has it to spare
 * wherever the height sets the number: the reference row allows 48.2 sp by width and draws
 * 39.3, and two cells on a launcher's 82 dp row allow 37.4 and draw 37.0. Only a two-cell
 * card on a row taller than 82.5 dp pays, a sp and a half at most.
 */
internal val NowGlyphShift = 5.dp

/** The height the number may take: the row inside its snug insets, less the place's line
 * and the stale marker's — the text card's own budget. */
private fun nowHeroRoom(size: DpSize, fontScale: Float, stale: Boolean): Dp =
    size.height - WidgetCardPaddingSnug * 2 - textLineHeight(TextFactSp, fontScale) -
        (if (stale) textLineHeight(TextStaleSp, fontScale) else 0.dp)

/** The number alone in a column [column] wide: the text card's [textRowHeroSp]. */
private fun nowRowBareSp(size: DpSize, fontScale: Float, stale: Boolean, column: Dp): Float {
    val byWidth = column.value / (TempEmWidth * fontScale.coerceAtLeast(0.1f))
    return minOf(textSizeForLine(nowHeroRoom(size, fontScale, stale), fontScale), byWidth)
        .coerceIn(TextHeroFloor, TextHeroRowMax)
}

/**
 * The number with the glyph in front of it. The glyph is the number's line box, so a line
 * of *s* sp is `s × (TempEmWidth + 1.32) × fontScale` dp wide plus the gap; on the
 * reference four-cell card the width allows 49.7 sp and the height 39.3, which is the text
 * card's own number.
 */
private fun nowGlyphLineSp(size: DpSize, fontScale: Float, stale: Boolean, wide: Boolean): Float =
    minOf(textSizeForLine(nowHeroRoom(size, fontScale, stale), fontScale), nowGlyphLineWidthSp(size, fontScale, wide))
        .coerceIn(TextHeroFloor, TextHeroRowMax)

/** The largest number whose line, glyph in front, the width holds. */
private fun nowGlyphLineWidthSp(size: DpSize, fontScale: Float, wide: Boolean): Float {
    val scale = fontScale.coerceAtLeast(0.1f)
    val perSp = TempEmWidth * scale + textLineHeight(1f, scale).value
    return (nowHeroLine(size, wide) - NowGlyphShift - IconTextGap).value / perSp
}

/**
 * Whether the row draws its glyph: where the glyph at the fresh number's size is at least
 * [NowGlyphMin] and the line still holds the widest number beside it. Every one-row grant
 * from two cells up does (51 dp at 159 dp of width); the provider's 110 dp minimum does
 * not, and there the card is the place and the number — the number is what the card is
 * for, and the place is what makes a position card honest.
 */
internal fun nowRowShowsIcon(size: DpSize, fontScale: Float): Boolean {
    val wide = nowLayout(size) == NowLayout.WIDE
    val sp = nowGlyphLineSp(size, fontScale, stale = false, wide = wide)
    // The line holds the glyph and the widest number exactly when the width alone would
    // not push the number under its floor: asked that way, so a line that fits to the dp
    // is not lost to a rounding.
    return textLineHeight(sp, fontScale) >= NowGlyphMin &&
        nowGlyphLineWidthSp(size, fontScale, wide) >= TextHeroFloor
}

/**
 * The smallest drawing the row puts in front of its number: the hour strip's own glyph,
 * the smallest sky these cards draw. A fresh card never comes near it; a stale one on the
 * reference row draws 37 dp, the stale number's own line.
 */
internal val NowGlyphMin = 32.dp

/** The number on a one-row card at the reader's [fontScale], old data's marker paid. */
internal fun nowRowHeroSp(size: DpSize, fontScale: Float, stale: Boolean): Float {
    val wide = nowLayout(size) == NowLayout.WIDE
    return if (nowRowShowsIcon(size, fontScale)) {
        nowGlyphLineSp(size, fontScale, stale, wide)
    } else {
        nowRowBareSp(size, fontScale, stale, nowHeroLine(size, wide) - NowGlyphEdgeGive)
    }
}

/** The glyph in front of the number: the number's own line box, or nothing. */
internal fun nowRowGlyphSize(size: DpSize, fontScale: Float, stale: Boolean): Dp =
    if (nowRowShowsIcon(size, fontScale)) {
        textLineHeight(nowRowHeroSp(size, fontScale, stale), fontScale)
    } else {
        0.dp
    }

/**
 * The far edge of a wide row, filled in the text card's order — a line of sentence, the
 * warning, the day's range, then more sentence — with the chip's box where that card has
 * a line of fact.
 */
internal fun nowRowPlan(
    size: DpSize,
    fontScale: Float,
    sentence: Boolean,
    warning: Boolean,
    range: Boolean
): TextRowPlan = textRowPlan(
    size, fontScale, sentence, warning, range,
    warningLine = warningBlock(fontScale, drawn = true)
)

/** Whether the far edge has a line for the chip of its own: the `ownRow` half of
 * [warningSlot], asked of the card the reader would have without the warning. */
internal fun nowRowHasWarningRow(size: DpSize, fontScale: Float, sentence: Boolean): Boolean =
    nowRowPlan(size, fontScale, sentence, warning = true, range = false).showWarning

/** What a drawn chip costs a budget: its own box and the air above it, or nothing. */
internal fun warningBlock(fontScale: Float, drawn: Boolean): Dp =
    if (drawn) warningChipHeight(fontScale) + WarningChipGap else 0.dp

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
    withWarning: Boolean,
    temperatureSp: Float = TemperatureSp
): Dp {
    val text = textLineHeight(temperatureSp, fontScale) +
        (if (withSentence) textLineHeight(SentenceSp, fontScale) * TallSentenceMaxLines else 0.dp) +
        textLineHeight(PlaceSp, fontScale) +
        (if (stale) textLineHeight(StaleSp, fontScale) else 0.dp) +
        warningBlock(fontScale, withWarning)
    return size.height - WidgetCardPaddingSnug - WidgetCardPadding - text +
        textInkBalance(temperatureSp, fontScale)
}

/**
 * **The number on a tall card grows with the card** (23 set 2026, widget review). Up to
 * two rows the tall form spends its height on the glyph, and the glyph stops at the
 * family's ceiling ([HeroIconMax], 104 dp) — which is where a three-row card used to stop
 * spending: a 34 sp number and a 104 dp drawing in a card with a hundred dp of nothing
 * between them. What the glyph cannot use now goes to the number, up to [TallTemperatureMax],
 * and never past what the words' column can hold at the widest temperature this app prints.
 *
 * Nothing changes where the glyph has not reached its ceiling: the reference two-by-two
 * (~69 dp of glyph) prints the same 34 sp it always did. The glyph never shrinks for it
 * either — the number takes only the surplus, and a larger number also lends the glyph a
 * larger band of leading ([textInkBalance]), so the recomputed room only grows.
 */
internal fun nowTallTemperatureSp(
    size: DpSize,
    fontScale: Float,
    stale: Boolean,
    withSentence: Boolean,
    withWarning: Boolean
): Float {
    val surplus = nowTallIconRoom(size, fontScale, stale, withSentence, withWarning) - HeroIconMax
    if (surplus <= 0.dp) return TemperatureSp
    val byHeight = textSizeForLine(textLineHeight(TemperatureSp, fontScale) + surplus, fontScale)
    val byWidth = (size.width - WidgetCardPadding * 2).value /
        (TempEmWidth * fontScale.coerceAtLeast(0.1f))
    return minOf(byHeight, byWidth, TallTemperatureMax).coerceAtLeast(TemperatureSp)
}

/**
 * The tall card's ceiling: the text widget's own for a number that stands over its
 * sentence ([TextHeroMax]) — past it a temperature stops being read as a number and starts
 * being read as an ornament, and two cards on one home screen should agree about where.
 */
internal const val TallTemperatureMax = TextHeroMax

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

/**
 * **The next hours on a narrow tall card** (widget review, 13 ott 2026): how many lines of
 * hours fit under the reference two-by-two's hero ([NowTallHeroCard]), at most eight
 * (Glance's ten children, less the hero and its gap) and never more than the strip
 * carries. Zero below two lines: one hour under a hero is a footnote, not a list, and the
 * card keeps the hero it had. On the reference two-by-three it is three, on the two-by-four
 * six.
 */
internal fun nowTallHourRows(size: DpSize, fontScale: Float, available: Int): Int {
    val row = maxOf(HourListRowHeight, textLineHeight(StripHourSp, fontScale))
    val room = size.height - NowTallHeroCard - HourListGap
    val rows = (room / row).toInt().coerceIn(0, minOf(HourListMax, available))
    return if (rows < 2) 0 else rows
}

/** The hero's height when the narrow tall card lists hours under it: the reference
 * two-by-two's, so the two cards' heroes are the same drawing at the same size. */
internal val NowTallHeroCard = 189.dp

internal val HourListGap = 8.dp
internal val HourListRowHeight = 30.dp
internal val HourListIconSize = 24.dp

/** Wide enough for «-10°», measured at 13 sp Medium with the family's slack. */
internal val HourListTempWidth = 40.dp
internal const val HourListMax = 8

/** The hero number's size, named because two budgets are measured off it. */
internal const val TemperatureSp = WidgetType.Hero

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
internal const val SentenceSp = WidgetType.Body
internal const val PlaceSp = WidgetType.Body
internal const val StaleSp = WidgetType.Caption

/**
 * 8 dp between glyph and words, not 12: the glyph leaves 9 to 12.5 dp of its own box
 * empty on that side too, so the gap the eye reads is 17 to 20 (5th device pass).
 */
internal val IconTextGap = 8.dp

/** The air between the temperature block and the sentence: the card's own 12. */
internal val SentenceGap = 12.dp
