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
 * | 1 | the temperature | [TextHeroFloor]…[TextHeroPanelMax], scaled to the grant | Bold | strong |
 * | 2 | the day's sentence | [TextSentenceSp] | Medium | strong |
 * | 3 | the place, the day's range, the warning's word | [TextFactSp] | Regular (Medium for the figures) | quiet (strong for the figures) |
 * | 4 | the stale marker | [TextStaleSp] | Regular | the freshness ink |
 *
 * **The number is sized by the grant, the way the other cards size their glyph.** A fixed
 * hero could only ever be right on one cell size; [heroIconSize] solves that for a drawing
 * and this file solves the same problem for a figure, with the same shape of answer — what
 * the height leaves once the lines around it are paid, between a floor and a ceiling.
 *
 * Four forms, picked by the grant like [NowLayout]'s:
 *
 * - [TextForm.LINE] — one row too narrow for two columns: the place, the number, the
 *   stale marker, and nothing else.
 * - [TextForm.ROW] — one row with a second column, which is the default 4×1 placement and
 *   also what three cells buys here: the place over the number on the leading side, and
 *   against the far edge the day's sentence with the warning and the day's range under it.
 *   Three cells is one cell better than the Now widget manages, and for a plain reason —
 *   the 66 dp of glyph and its gap that card spends before the first letter, this one does
 *   not have.
 * - [TextForm.STACK] — two rows and up, under four cells wide: the place as an eyebrow at
 *   the top, and everything else against the BOTTOM edge with the air between them.
 * - [TextForm.PANEL] — two rows and up, four cells or wider (committente, 19 set 2026,
 *   second device pass): the same eyebrow, and along the bottom the number on the leading
 *   side at a size the one-row card cannot afford, with the sentence and the day's range
 *   right-aligned beside it.
 *
 * **There are no hourly temperatures on any form** (same pass: «praticamente facciamo
 * sempre senza temperature orarie»). The first draft printed the next hours as figures on
 * a tall card; on a device it read as a second widget stapled under the first, and the
 * card that exists for the hours is the Today widget. What the height buys instead is air
 * between the eyebrow and the block, which is what makes the bottom alignment read as a
 * composition rather than as a list that ran out.
 *
 * Everything here is arithmetic on dp and sp so a table can pin it
 * (`TextWidgetLayoutTest`) rather than a screenshot of one launcher's idea of a cell. The
 * reference figures below are the ones the other layouts are measured against: a one-row
 * card is ~85 dp tall (and ~101 on the other launcher seen to grant one), two cells are
 * ~159 dp wide, three ~250, four ~340, and a four-by-two is ~340 × 189.
 */
internal enum class TextForm { LINE, ROW, STACK, PANEL }

internal fun textForm(size: DpSize): TextForm = when {
    size.height >= TallMinHeight && size.width >= TextPanelMinWidth -> TextForm.PANEL
    size.height >= TallMinHeight -> TextForm.STACK
    textSentenceColumn(size) >= TextSentenceColumnMin -> TextForm.ROW
    else -> TextForm.LINE
}

/**
 * Four cells, on every grid this has been measured on: four cells are ~340 dp on the
 * reference device and ~320 on a five-column grid, three are ~250. A dp threshold and not
 * a cell count because a widget is never told how many cells it got — [TallMinHeight] is
 * the same kind of number for the same reason.
 */
internal val TextPanelMinWidth = 300.dp

/**
 * The leading column of a one-row card: **what a long place name needs, but never out of
 * the sentence's minimum.**
 *
 * It was a flat 42% of the row's slack until 19 set 2026, and on a device that column was
 * 126 dp against the 165 «⌖ Cavenago di Brianza» wants — so the card printed «Cavenago di
 * Bri…» on a row with 174 dp of white space in its other column (committente: «vorrei che
 * si riesca a vedere completamente una località lunga… lasciando così il layout e le
 * dimensioni dei vari testi»). A share cannot fix that, because the two columns do not
 * want the same thing: the leading one wants exactly as much as the name it is holding,
 * and the trailing one wants a measure.
 *
 * So every dp past [TextSentenceColumnMin] goes to the name until the name is satisfied
 * ([TextPlaceColumnIdeal]), and the floor under it is what a narrow card falls back to.
 * On the reference four-cell card that is 168 dp against 132 — where a flat share gave 126
 * against 174 — and three cells still land exactly on the sentence's minimum, so the form
 * this card gains at three cells is not given back.
 */
internal fun textLeadingColumn(size: DpSize): Dp {
    val words = size.width - WidgetCardPadding * 2 - SentenceGap
    return (words - TextSentenceColumnMin)
        .coerceAtMost(TextPlaceColumnIdeal)
        .coerceAtLeast(TextWordsMin)
}

/** What is left for the sentence once the leading column and the gap are paid. Negative
 * on a card too narrow to hold both; the caller compares, never draws. */
internal fun textSentenceColumn(size: DpSize): Dp =
    size.width - WidgetCardPadding * 2 - SentenceGap - textLeadingColumn(size)

/**
 * What the place column wants: «Cavenago di Brianza» is 145 dp at [TextFactSp] (measured,
 * and the same number `NowWidgetLayout` records for the same name at the same size), plus
 * the position pin's box and the air after it — 165 dp, rounded up to 168 so a name a
 * letter longer still lands inside it. Past this the column stops growing and the sentence
 * takes the rest: a column wider than the longest name it will ever hold is white space
 * taken from prose.
 */
internal val TextPlaceColumnIdeal = 168.dp

/**
 * What the leading column must keep: «−12°» at the hero's floor is ~55 dp and a ten-letter
 * place with its pin ~93, so 96 is where the column stops being able to hold the number
 * AND a name worth reading — the Now widget's [WordsColumnMin] argument, at this card's
 * sizes.
 */
internal val TextWordsMin = 96.dp

/**
 * The narrowest column worth a sentence: [SentenceColumnMin] is the household's number for
 * 16 sp, and this card sets prose two points larger, so it asks for one step more room.
 * 104 dp is about eleven characters of 18 sp, and three lines of eleven hold every sentence
 * the brief register can say. The reference three-cell card lands exactly here; two cells
 * give 23 and stay on the narrow form.
 */
internal val TextSentenceColumnMin = 104.dp

/**
 * The hero on a one-row card: what the height leaves once the place's line and the stale
 * marker's are paid, capped at [TextHeroRowMax].
 *
 * The stale marker sits in THIS column rather than under the sentence, and pays for itself
 * out of the number: a card whose data is old says so before it says anything else loudly,
 * and on the reference 85 dp row that still leaves 28.3 sp. The number is 39.3 sp on the
 * same row when the data is fresh, against the 34 the Now widget prints beside its glyph.
 *
 * The column it must not overflow is [textLeadingColumn] where there is a second column to
 * share the row with, and the whole width where there is not: the narrow form has no
 * trailing column, so pretending it did would hand the number 96 dp on a card that only
 * has 82.
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
 * card carries two lines of sentence and a line of fact, which is 2 × 23.76 + 21.12 ≈
 * 68 dp; 44 sp has a line box of 58, and 52 would already be taller than everything it
 * stands next to. Without the cap the 101 dp row the other launcher grants would print a
 * 51 sp number beside an 18 sp sentence, which is a poster, not a hierarchy.
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
): TextRowPlan = fillColumn(
    room = size.height - WidgetCardPaddingSnug * 2,
    fontScale = fontScale,
    sentence = sentence,
    warning = warning,
    range = range,
    maxLines = TextRowSentenceMaxLines
).asRowPlan()

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
 * The two-column tall card (committente, 19 set 2026: «se la dimensione poi è su 2 righe e
 * le colonne sono almeno 4 valuta di cambiare il layout con la temperatura allineata in
 * basso con font un po' più grande e testo e temperature max e min allineate a destra in
 * basso»).
 *
 * The place stays at the top as the card's eyebrow, on the full width — which is where a
 * long name finally has nothing to compete with — and everything else sits on the BOTTOM
 * edge: the number on the leading side, the sentence and the day's range right-aligned
 * beside it. The air lands in the middle, between the two, which is what makes the card
 * read as a composition instead of a column that ran out of things to say.
 *
 * The two columns are budgeted separately and neither can overflow by construction: the
 * left one is paid out of `height − place − stale` and the right one out of
 * `height − place`, so whichever is taller is still inside the card.
 */
internal data class TextPanelPlan(
    val heroSp: Float,
    val sentenceLines: Int,
    val showWarning: Boolean,
    val showRange: Boolean
)

internal fun textPanelPlan(
    size: DpSize,
    fontScale: Float,
    stale: Boolean,
    sentence: Boolean,
    warning: Boolean,
    range: Boolean
): TextPanelPlan {
    val top = size.height - WidgetCardPadding * 2 - textLineHeight(TextFactSp, fontScale)
    val column = fillColumn(
        room = top,
        fontScale = fontScale,
        sentence = sentence,
        warning = warning,
        range = range,
        maxLines = TextPanelSentenceMaxLines
    )
    val heroRoom = top - (if (stale) textLineHeight(TextStaleSp, fontScale) else 0.dp)
    return TextPanelPlan(
        heroSp = heroSp(heroRoom, TextPanelLeading, fontScale, TextHeroPanelMax),
        sentenceLines = column.sentenceLines,
        showWarning = column.showWarning,
        showRange = column.showRange
    )
}

/** [textRowHasFactLine] for the panel: the same question, asked of the same card without
 * the warning on it. */
internal fun textPanelHasFactLine(
    size: DpSize,
    fontScale: Float,
    sentence: Boolean
): Boolean = textPanelPlan(
    size, fontScale, stale = false, sentence = sentence, warning = true, range = false
).showWarning

/**
 * The panel's leading column: fixed, because what stands in it is one figure and not a
 * measure. 140 dp is «−12°» at [TextHeroPanelMax] (134.4) plus a couple of dp of slack, so
 * the ceiling and not the width is what decides the number's size on every grant. The
 * sentence takes the rest — 160 dp on the reference four-by-two, 120 at the narrowest card
 * that qualifies as a panel at all.
 *
 * It is NOT [textLeadingColumn]: that column is sized for a place name, and on this form
 * the place is not in it. The name is the full-width eyebrow above both columns.
 */
internal val TextPanelLeading = 140.dp

internal fun textPanelSentenceColumn(size: DpSize): Dp =
    size.width - WidgetCardPadding * 2 - SentenceGap - TextPanelLeading

/**
 * The panel's number, and the reason it may be bigger than any other form's: it does not
 * stand under the words, it stands BESIDE them, so the rule is the one [TextHeroRowMax]
 * states — the number never outgrows the block beside it. That block at its fullest is two
 * lines of sentence, the warning's word and the day's range, 89.8 dp; 64 sp has a line box
 * of 84.5, so the number is as tall as the column beside it and no taller.
 *
 * On every grant a launcher makes, this ceiling is what decides. The height bound in
 * [textPanelPlan] is the guard behind it: a card squeezed to the two-row minimum with a
 * stale marker on it gives 65.4 sp, and anything shorter than that is not a panel.
 */
internal const val TextHeroPanelMax = 64f

/** Two lines on the panel, like the stack and for the same reason the Now widget's tall
 * card gives: the third line is worth more to the number than to the prose, and at 160 dp
 * of measure the brief register almost never asks for it. */
internal const val TextPanelSentenceMaxLines = 2

/**
 * The plan for a tall card too narrow for two columns: what the height buys, in the order
 * it buys it.
 *
 * The order IS the hierarchy, and it is this widget's whole layout argument. The number is
 * reserved first at [TextHeroStackFloor] — on a tall card it is the page's title, and a
 * title that shrank so a footnote could fit would be the card arguing with itself. Then
 * the sentence's two lines, then the warning's word, then the day's range; whatever is left
 * over grows the number, up to [TextHeroMax]. A section that does not fit is not drawn
 * (`DESIGN` §1.1) — the Today widget's own rule for its rain row.
 *
 * The place and the stale marker are never in the order, because they are never optional:
 * a card that dropped its place would be a number about nowhere, and one that dropped its
 * age would be lying about how old it is (VISION §5.9).
 *
 * The sentence's lines are RESERVED, not measured — Glance cannot measure text — so a
 * one-line sentence leaves its second line as air. Since 19 set 2026 that air lands
 * between the eyebrow and the block rather than inside the block, because the place is
 * pinned to the top of the card and everything else to the bottom.
 */
internal data class TextStackPlan(
    val heroSp: Float,
    val sentenceLines: Int,
    val showWarning: Boolean,
    val showRange: Boolean
)

internal fun textStackPlan(
    size: DpSize,
    fontScale: Float,
    stale: Boolean,
    sentence: Boolean,
    warning: Boolean,
    range: Boolean
): TextStackPlan {
    val floor = textLineHeight(TextHeroStackFloor, fontScale)
    val room = size.height - WidgetCardPadding * 2 -
        textLineHeight(TextFactSp, fontScale) -
        (if (stale) textLineHeight(TextStaleSp, fontScale) else 0.dp) -
        floor
    val column = fillColumn(
        room = room,
        fontScale = fontScale,
        sentence = sentence,
        warning = warning,
        range = range,
        maxLines = TextStackSentenceMaxLines,
        growSentenceLast = false
    )
    return TextStackPlan(
        heroSp = heroSp(
            floor + column.left.coerceAtLeast(0.dp),
            size.width - WidgetCardPadding * 2,
            fontScale,
            TextHeroMax
        ),
        sentenceLines = column.sentenceLines,
        showWarning = column.showWarning,
        showRange = column.showRange
    )
}

internal const val TextStackSentenceMaxLines = 2

/** [textRowHasFactLine]'s answer for a narrow tall card, asked the same way and for the
 * same reason: the plan without the warning says whether there is a line for one. */
internal fun textStackHasFactLine(
    size: DpSize,
    fontScale: Float,
    stale: Boolean,
    sentence: Boolean
): Boolean = textStackPlan(
    size, fontScale, stale, sentence, warning = true, range = false
).showWarning

/**
 * The one piece of arithmetic all three budgets share: fill a column of prose and facts
 * out of [room], in the order the hierarchy spends in.
 *
 * One line of sentence first, then the warning's word, then the day's range, and then —
 * where [growSentenceLast] is on — as many more lines of sentence as what is left will
 * hold. The stack turns that last step off because there the leftover belongs to the
 * number instead, which is the one thing that form does differently.
 */
private data class FilledColumn(
    val sentenceLines: Int,
    val showWarning: Boolean,
    val showRange: Boolean,
    val left: Dp
)

private fun fillColumn(
    room: Dp,
    fontScale: Float,
    sentence: Boolean,
    warning: Boolean,
    range: Boolean,
    maxLines: Int,
    growSentenceLast: Boolean = true
): FilledColumn {
    val line = textLineHeight(TextSentenceSp, fontScale)
    val fact = textLineHeight(TextFactSp, fontScale)
    var left = room
    var lines = 0
    if (sentence && left >= line) {
        left -= line
        lines = 1
    }
    if (!growSentenceLast) {
        while (sentence && lines < maxLines && left >= line) {
            left -= line
            lines++
        }
    }
    val showWarning = warning && left >= fact
    if (showWarning) left -= fact
    val showRange = range && left >= fact
    if (showRange) left -= fact
    if (growSentenceLast) {
        while (lines in 1 until maxLines && left >= line) {
            left -= line
            lines++
        }
    }
    return FilledColumn(lines, showWarning, showRange, left)
}

private fun FilledColumn.asRowPlan() = TextRowPlan(sentenceLines, showWarning, showRange)

/**
 * **Where the weather glyph goes, when the reader asks for one** (committente, 20 set 2026:
 * «per ogni layout/dimensione del widget senza cambiare assolutamente niente del testo che
 * c'è adesso… dove si potrebbe aggiungere anche l'icona del meteo attuale»).
 *
 * The condition is the whole design: **no budget above this line changes.** The glyph is
 * not a section the card makes room for — it is drawn only into space the card already
 * leaves empty, so a reader who turns it on loses no line of sentence, no warning, no
 * range, and no dp of number. Every one of the four functions below therefore returns a
 * size or nothing; none of them is an input to a plan.
 *
 * There are two slots, and which one a form uses depends on where its empty space is:
 *
 * - **Beside the number** ([textRowIconSize], [textStackIconSize]). The number is at most
 *   [TempEmWidth] ems wide («−12°»), and on a wide enough card the rest of its line is air.
 *   The glyph fills that air, against the trailing edge of the column, and never grows the
 *   line it sits on.
 * - **Over the words** ([textPanelIconSize]). The panel pins its eyebrow to the top and its
 *   block to the bottom, so the band between them is empty by construction; the glyph
 *   hangs at the end of that band, directly over the sentence.
 *
 * Both are geometric — measured against the WIDEST temperature this app can print, never
 * against the one it is printing — for the reason the Sky widget states about its own
 * forms: a card that changed shape with the forecast could not be aimed.
 */
internal fun textIconSize(slot: Dp): Dp =
    if (slot >= TextIconMin) heroIconSize(slot, min = TextIconMin) else 0.dp

/**
 * The glyph on a one-row card: the number's own line box, and whatever the column has left
 * beside the widest number it could print.
 *
 * Measured against the FRESH number on purpose. A stale card prints a smaller one and puts
 * the marker under it, and the band below the place line is the same height either way
 * (the column fills the row by construction) — so sizing on the fresh number gives a glyph
 * that neither overflows the stale card nor changes size when the data ages.
 *
 * On the reference four-cell row that is 51.9 dp, and 58.1 on the 101 dp row the other
 * launcher grants. **At three cells it is nothing** (23.5 dp of air, against 48 needed), and
 * that is the form's own trade rather than a failure: at three cells this card spends its
 * width on the sentence, which the Now widget at three cells does not have.
 */
internal fun textRowIconSize(size: DpSize, fontScale: Float): Dp {
    val hero = textRowHeroSp(size, fontScale, stale = false)
    val column = if (textForm(size) == TextForm.LINE) {
        // The narrow form has no second column, so the glyph meets the card's own edge and
        // that edge is a glyph's (4 dp), not a word's — see [TextIconEdgeGive].
        size.width - WidgetCardPadding - WidgetCardPaddingLeading
    } else {
        textLeadingColumn(size)
    }
    return textIconSize(
        minOf(textLineHeight(hero, fontScale), column - heroWidth(hero, fontScale))
    )
}

/**
 * The glyph on a narrow tall card: the same slot, beside the number, against the card's own
 * trailing edge. [heroSp] is the plan's, not the fresh card's, because here the number
 * really does shrink when the marker appears and the glyph shrinks with it — the two sit on
 * one line and the line is what the budget already paid for.
 *
 * Two cells is nothing (27.7 dp against 48 needed) and there is no honest way round it: the
 * stack spends every leftover dp on the number, so there is no air to take and taking it
 * from the number would be changing the text. Two and a half cells upwards it is 48 to 71 dp.
 */
internal fun textStackIconSize(size: DpSize, fontScale: Float, heroSp: Float): Dp {
    val column = size.width - WidgetCardPadding - WidgetCardPaddingLeading
    return textIconSize(
        minOf(textLineHeight(heroSp, fontScale), column - heroWidth(heroSp, fontScale))
    )
}

/**
 * The glyph on the panel: the band between the eyebrow and the block, at the end of it, over
 * the words.
 *
 * The band is `height − the place's line − the trailing column`, and the trailing column is
 * the only thing under the glyph — so the stale marker, which lives in the LEADING column,
 * never costs the glyph a dp. On the reference four-by-two that is 71.2 dp, and 50.1 on a
 * day with a warning, which is the one thing that does take from it: the warning's word is
 * a line the card grew for a reason, and a drawing yields to it.
 */
internal fun textPanelIconSize(size: DpSize, fontScale: Float, plan: TextPanelPlan): Dp {
    val fact = textLineHeight(TextFactSp, fontScale)
    val trailing = textLineHeight(TextSentenceSp, fontScale) * plan.sentenceLines +
        (if (plan.showWarning) fact else 0.dp) +
        (if (plan.showRange) fact else 0.dp)
    val band = size.height - WidgetCardPadding * 2 - fact - trailing
    return textIconSize(minOf(band, textPanelSentenceColumn(size) + TextIconEdgeGive))
}

/** The widest this app can print a temperature at [sp]: see [TempEmWidth]. */
private fun heroWidth(sp: Float, fontScale: Float): Dp = (sp * TempEmWidth * fontScale).dp

/**
 * The smallest glyph this card draws, one step under the family's own hero floor
 * ([heroIconSize] clamps at 52). The step is deliberate and it is not a relaxation of
 * `DESIGN` §13.1: on the other four cards the glyph IS the hero and 52 is where it stops
 * carrying the card at arm's length. Here the hero is the number and the glyph is its
 * companion, at the number's own optical size — 48 dp of box is about 33 of ink, which is
 * what a 39 sp figure puts on the same line.
 */
internal val TextIconMin = 48.dp

/**
 * What the card gives back at its trailing edge when a glyph meets it.
 *
 * A glyph edge takes [WidgetCardPaddingLeading] and a words edge [WidgetCardPadding] —
 * the Now widget's measured rule, and the drawings carry 9 to 12.5 dp of margin of their
 * own, so a glyph at the words' inset reads about 10 dp further in than it should. On the
 * three forms whose glyph meets the card (everything but [TextForm.ROW], where it is
 * interior) the card's end padding becomes the glyph's and **the text pays the difference
 * back**, so every line measures exactly as it did without the glyph. That is the whole
 * "nothing moves" promise, written as one number.
 */
internal val TextIconEdgeGive = WidgetCardPadding - WidgetCardPaddingLeading

/**
 * **The card's trailing inset and the give-back that answers it, settled together** —
 * one condition, because the two drifted the moment they were two (21 set 2026).
 *
 * The edge went to the glyph on the strength of the reader's switch alone, while each
 * text paid the 10 dp back only where a glyph really came out of [textIconSize] — and
 * that function answers 0 dp on every grant too small for one: a two-cell stack at any
 * font scale, the reference panel at 1.3, a narrow one-row card at 1.3. On exactly those
 * cards turning the glyph ON moved every line 10 dp towards a 24 dp corner and drew
 * nothing there at all, which is the reverse of what the switch promises.
 *
 * So the pair is decided off the FORM and the switch and nothing else. Whether this grant
 * has room for the drawing is the drawing's business; the inset's business is that the
 * words measure at [WidgetCardPadding] on every card, with a glyph and without one.
 */
internal fun textCardPaddingEnd(form: TextForm?, showIcon: Boolean): Dp =
    if (textGlyphMeetsEdge(form, showIcon)) WidgetCardPaddingLeading else WidgetCardPadding

/** What a text that reaches the trailing edge pays back so that its measure is the one it
 * had without the glyph: [WidgetCardPadding] less whatever the card kept for itself. */
internal fun textEdgeGive(form: TextForm?, showIcon: Boolean): Dp =
    WidgetCardPadding - textCardPaddingEnd(form, showIcon)

/** [TextForm.ROW] keeps the words' edge whatever the switch says: there the glyph is
 * interior, inside the name's own column, and never touches the card. A null form is a
 * card with no report yet, which has no glyph to make room for either. */
private fun textGlyphMeetsEdge(form: TextForm?, showIcon: Boolean): Boolean =
    showIcon && form != null && form != TextForm.ROW

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
internal const val TempEmWidth = 2.1f

/**
 * Below this a temperature stops being a hero and becomes just another line.
 *
 * **26 since 19 set 2026**, from 30, and the reason is arithmetic rather than taste: rank 3
 * went from 14 sp to 16, so the place's line grew by 2.6 dp and the stale marker's budget
 * with it. On the reference 85 dp row a stale card now leaves 37.4 dp, which is 28.3 sp —
 * under the old floor, and a floor that cannot be paid is not a floor, it is a clipped
 * line. At 26 the same card keeps its real number and the floor goes back to being what it
 * is meant to be: the point below which a squeezed card stops shrinking the figure, not a
 * size any measured grant reaches. Fresh data on that row still reads 39.3 sp.
 */
internal const val TextHeroFloor = 26f

/** A narrow tall card reserves this much for its number before anything optional is paid:
 * the rank-1 line of a page, not what is left after the footnotes. */
internal const val TextHeroStackFloor = 40f

/** The narrow tall card's ceiling: past this a temperature set OVER its own sentence stops
 * being read as a number and starts being read as an ornament. The panel's is higher
 * ([TextHeroPanelMax]), because there the number stands beside the words instead. */
internal const val TextHeroMax = 56f

/**
 * Rank 2, the day's sentence: 18 sp Medium in the strong ink, two points above the
 * household's 16. The other cards put the sentence beside a drawing, and the drawing is
 * what carries the card at arm's length; here the sentence IS the second thing to read, so
 * it takes the step the glyph's absence pays for.
 *
 * **18 since 19 set 2026** (committente, on the device), from 17: rank 3 went up to the
 * household's 16 in the same pass and 17 over 16 is not a rank, it is a rounding error. Two
 * points, with Medium against Regular and the strong ink against the quiet one, is the
 * smallest gap that still sorts at arm's length. At 18 the reference four-cell card's
 * trailing column still holds «Pioggia gelata verso le 15:00» in two lines, which is the
 * measurement that stopped it going to 19.
 */
internal const val TextSentenceSp = 18f

/**
 * Rank 3, the facts: the place, the day's high and low, the warning's word. **16 sp since
 * 19 set 2026** (committente, on the device: «la località un pochino più grande», «max e
 * min un po' più grandi»), from 14 — which lands it on the household's own 16, the size
 * every other card prints a place and a range at, so the five widgets now agree about what
 * rank a fact is.
 *
 * Inside the rank the other two axes still do the sorting: the place is Regular in the
 * quiet ink, the figures Medium in the strong one, and the range carries its two marks.
 */
internal const val TextFactSp = 16f

/** Rank 4, the footnote: the stale marker, at the household's own stale size and in the
 * freshness ink it wears on every other card. */
internal const val TextStaleSp = 11f
