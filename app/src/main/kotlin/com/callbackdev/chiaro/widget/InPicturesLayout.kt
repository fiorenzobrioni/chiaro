package com.callbackdev.chiaro.widget

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.domain.pictures.DialSpec
import com.callbackdev.chiaro.domain.pictures.PlateMetrics
import com.callbackdev.chiaro.domain.pictures.StripMetrics
import com.callbackdev.chiaro.domain.pictures.WeekRange
import kotlin.math.floor
import kotlin.math.sqrt

/**
 * **«In immagini»**, the drawn twin of «In parole» (Fase 17, committente 15 nov 2026): its
 * eight forms, and what each one spends its height and width on.
 *
 * The canvas the committente approved (`docs/design/in-immagini/Forms.dc.html`) is the
 * contract (PLANNING, «Fase 17», la regola della fedeltà): every dp and sp below is the
 * canvas's, named for what it is, and `InPicturesLayoutTest` pins them at the reference
 * grants — 85, 189, 293 and 397 dp tall, 159, 250 and 340 wide — at 100, 115 and 130% of
 * text. The geometry INSIDE the drawings (the plate's curve, the dial's ring, the strip's
 * marks) is `:core:domain`'s `pictures` package; this file places the drawings on the card
 * and budgets the words around them.
 *
 * | Form | Reference | What it carries |
 * |---|---|---|
 * | [PicturesForm.DialRow] | 2×1, 159 × 85 | the number and the short sentence, the 52 dp dial beside them |
 * | [PicturesForm.Strip] | 4×1, 340 × 85 | the number; the short sentence and the place on one line, twelve hours of ribbon under them |
 * | [PicturesForm.Dial] | 2×2, 159 × 189 | the 124 dp dial with the number inside, the short sentence and the place under it |
 * | [PicturesForm.Dial] + [PicturesExtra.Later] | 2×3, 159 × 293 | the dial, the whole sentence, and «Più tardi»: three hours |
 * | [PicturesForm.Compact] | 3×2, 250 × 189 | the number and the whole sentence, the plate 226 wide |
 * | [PicturesForm.Panel] | 4×2, 340 × 189 | the reference: the hour's glyph, the number, the sentence and the place, the plate 312 wide |
 * | [PicturesForm.Panel] + [PicturesExtra.Hours] | 4×3, 340 × 293 | the 4×2, and six hours in a grid |
 * | [PicturesForm.Panel] + [PicturesExtra.Days] | 4×4, 340 × 397 | the 4×2, and five days |
 *
 * **The rules the canvas states for every size** (its «Come si adatta»):
 *
 * - *La larghezza compra parole.* When the words do not fit, the glyph goes first, then
 *   the place; the sentence wraps and is never cut, and only then becomes the short one.
 * - *L'altezza compra ore e giorni*, never a bigger ornament: the dial stops at 124 dp, the
 *   curve at 3.6 dp per degree, the rows come whole or not at all.
 * - *Quello che non ci sta non si disegna*: a form that cannot hold its drawing hands the
 *   card to a smaller form ([picturesPlan]'s fallbacks), never a clipped one.
 * - *Un dato vecchio lo dice*: past an hour, «Aggiornato alle 12:40» takes the place's line,
 *   in Caption, and it is never what gives way ([PicturesPiece.Stale]).
 *
 * **The text is measured, the boxes are the canvas's.** Glance cannot measure text, so the
 * caller does, on the device with the system face, as the text widget does for its sentence
 * ([PicturesMeasure]); everything else is arithmetic on the canvas's line boxes (its
 * `line-height`s) scaled by the reader's font size. For prose those boxes are what a
 * `TextView` draws within a few tenths of a dp (16 sp on two lines is 40 dp either way); for
 * the numbers the canvas sets tight boxes (46 sp in 48) that 17.2 has to honour, since a
 * `TextView` keeps 61.
 */
internal enum class PicturesForm { DialRow, Strip, Dial, Compact, Panel }

/** What the height buys under the drawing, whole or not at all. */
internal enum class PicturesExtra { None, Later, Hours, Days }

/** The form the grant asks for, before a word is measured. [picturesPlan] may hand the card
 * to a smaller one when the words do not leave its drawing the room it needs. */
internal fun picturesForm(size: DpSize): PicturesForm = when {
    size.height < TallMinHeight ->
        if (size.width >= PicturesWideMin) PicturesForm.Strip else PicturesForm.DialRow
    size.width < PicturesWideMin -> PicturesForm.Dial
    size.width < PicturesPanelMinWidth -> PicturesForm.Compact
    else -> PicturesForm.Panel
}

/** Three cells on every grid measured (~230–260 dp; two are ~150–180): where the drawing
 * becomes a strip or a plate instead of a dial. The household's [HoursMinWidth]. */
internal val PicturesWideMin = HoursMinWidth

/** Four cells, where the glyph and the 312 dp plate come in: the text widget's panel width. */
internal val PicturesPanelMinWidth = TextPanelMinWidth

/** The words a card can print, as the caller measures them. */
internal enum class PicturesPiece {
    /** «11°», the hour's temperature. */
    Number,

    /** The day's sentence, the same as Today's and «In parole»'s: «Pioggia dalle 21, smette
     * all'alba». */
    Sentence,

    /** Its short form, the one-line card's: «Pioggia dalle 21». */
    ShortSentence,

    /** «Milano». */
    Place,

    /** «Milano · ↑12° ↓6°», the 4×2's line. */
    PlaceAndRange,

    /** «↑12° ↓6°», inside the dial. */
    Range,

    /** «Aggiornato alle 12:40»: past an hour, in the place's slot, never given up. */
    Stale
}

/**
 * The text a card is going to print, measured by the caller with the face the launcher draws
 * it in (Glance cannot measure; the text widget measures its sentence the same way). Both
 * answers are at the reader's font scale. A piece the card has nothing for — no sentence on a
 * quiet day, no stale marker on fresh data — answers 0 dp and 0 lines, and is not drawn.
 */
internal interface PicturesMeasure {
    /** [piece] set at [sp] on one line, in dp, at the reader's scale. */
    fun width(piece: PicturesPiece, sp: Float): Dp

    /** The lines [piece] wraps to at [sp] in [width] dp, at the reader's scale. */
    fun lines(piece: PicturesPiece, sp: Float, width: Dp): Int
}

/** The reader's settings that change what a card holds (the rest change only its inks). */
internal data class PicturesContent(
    /** «Il disegno dell'ora»: the 4×2's glyph. */
    val glyph: Boolean = true,
    /** «I prossimi giorni»: the 4×4's rows. Off, the height buys the hours instead. */
    val days: Boolean = true
)

/**
 * Where everything goes on one card, in dp from the card's top left corner.
 *
 * A null rect is a piece not drawn. The words are described by what they are and how many
 * lines they take; their positions follow from the form's paddings and gaps, which are
 * the constants below.
 */
internal data class PicturesPlan(
    val form: PicturesForm,
    val extra: PicturesExtra = PicturesExtra.None,
    /** The number's size in sp, before the reader's scale: the canvas's, or smaller where its
     * box would be taller than the header (or, inside the dial, wider than the ring). */
    val numberSp: Float,
    val sentence: PicturesPiece? = null,
    val sentenceLines: Int = 0,
    /** [PicturesPiece.Place], [PicturesPiece.PlaceAndRange] or [PicturesPiece.Stale]. */
    val placeLine: PicturesPiece? = null,
    val placeLines: Int = 0,
    val glyph: DpRect? = null,
    val dial: DpRect? = null,
    /** The 2×2's range under the number inside the dial. */
    val dialRange: Boolean = false,
    val plate: DpRect? = null,
    val strip: DpRect? = null,
    /** The 2×3's «Più tardi» panel and its rows. */
    val later: DpRect? = null,
    val laterRows: Int = 0,
    /** Where the 4×3's hours or the 4×4's days begin. */
    val extraTop: Dp = 0.dp,
    val hourColumns: Int = 0,
    val dayRows: Int = 0
)

/**
 * The plan for a card of [size] at the reader's [fontScale]: the form the grant asks for if
 * its drawing fits once the words are paid, otherwise the next smaller form that holds — the
 * plate's forms fall to the strip, the strip and the dial to the 2×1, and the 2×1 always
 * answers, with as much as fits.
 */
internal fun picturesPlan(
    size: DpSize,
    fontScale: Float,
    measure: PicturesMeasure,
    content: PicturesContent = PicturesContent()
): PicturesPlan {
    val s = fontScale.coerceAtLeast(0.1f)
    val stale = measure.width(PicturesPiece.Stale, PicturesStaleSp) > 0.dp
    return when (picturesForm(size)) {
        PicturesForm.Panel -> panelPlan(size, s, measure, content, stale)
        PicturesForm.Compact -> compactPlan(size, s, measure, stale)
        PicturesForm.Strip -> null
        PicturesForm.Dial -> dialPlan(size, s, measure, stale)
        PicturesForm.DialRow -> null
    }
        ?: stripPlan(size, s, measure, stale).takeIf { size.width >= PicturesWideMin }
        ?: dialRowPlan(size, s, measure, stale)
}

// ---------------------------------------------------------------------------------------
// The 4×2, 4×3 and 4×4: the panel.
// ---------------------------------------------------------------------------------------

/** The card's padding on the plate's forms: 14 at the top and the sides, 12 at the foot
 * (`padding: 14px 14px 12px`). The 3×2 keeps 12 at the sides. */
internal val PanelPaddingTop = 14.dp
internal val PanelPaddingSide = 14.dp
internal val PicturesPaddingBottom = 12.dp
internal val CompactPaddingSide = 12.dp

/** The header: 54 dp, then 8 to the plate. The words may run into those 8 (the canvas's do,
 * by 9 dp, on a two-line sentence), and past them they push the plate down: see
 * [plateTop]. */
internal val PicturesHeaderHeight = 54.dp
internal val PicturesHeaderGap = 8.dp

/** The hour's glyph: 46 dp, 1 dp down, 10 dp before the number. */
internal val PanelGlyph = 46.dp
internal val PanelGlyphTop = 1.dp
internal val PicturesHeaderGutter = 10.dp

/** The panel's number: 46 sp Bold in a 48 dp box. */
internal const val PanelNumberSp = 46f
internal const val PanelNumberLine = 48f

/** The sentence, 16 sp Medium on 20 dp lines, 4 dp down (its caps on the number's); the
 * place, 13 sp quiet on 17, 2 dp under it. */
internal const val PicturesSentenceSp = WidgetType.Body
internal const val PanelSentenceLine = 20f
internal val PanelWordsTop = 4.dp
internal val PicturesWordsGap = 2.dp
internal const val PanelPlaceSp = WidgetType.Label
internal const val PanelPlaceLine = 17f

/** The stale marker, wherever it stands: Caption, on 13 dp. */
internal const val PicturesStaleSp = WidgetType.Caption
internal const val PicturesStaleLine = 13f

/**
 * Where the plate starts: under the header and its gap — 76 dp on every plate form — unless
 * the words need more than the 62 dp those two give them, and then right under the words.
 *
 * The canvas lets its words overrun: on the 4×2 its two lines of sentence and its place line
 * are 63 dp, and the place's box ends 1 dp inside the plate (its ink does not, the line box is
 * taller than the letters). RemoteViews stacks the header and the plate as siblings, which
 * cannot overlap, so here the plate gives that dp back and stands at 77 (PLANNING, 17.1,
 * «Decisioni e deviazioni»).
 */
private fun plateTop(padTop: Dp, words: Dp): Dp =
    padTop + maxOf(PicturesHeaderHeight + PicturesHeaderGap, words)

private val PlateHeight = PlateMetrics.HEIGHT.dp

/** The number's size where its box must not be taller than the header: 46 sp up to 112% of
 * text, then the size whose box is the header's 54 dp. A bigger number would push the plate
 * off the card at 130%, and the reader's scale is for the prose. */
private fun headerNumberSp(sp: Float, line: Float, s: Float): Float =
    minOf(sp, PicturesHeaderHeight.value / (line / sp * s))

private fun box(line: Float, lines: Int, s: Float): Dp = (line * lines * s).dp

private data class Attempt(val glyph: Boolean, val sentence: PicturesPiece?, val place: PicturesPiece?)

/** The order a card gives things up in when its words do not fit: the glyph, then the place,
 * then the whole sentence for the short one, then the sentence. The stale marker never. */
private fun panelAttempts(glyph: Boolean, stale: Boolean): List<Attempt> {
    val place = if (stale) PicturesPiece.Stale else PicturesPiece.PlaceAndRange
    val dropped = if (stale) PicturesPiece.Stale else null
    return listOf(
        Attempt(glyph, PicturesPiece.Sentence, place),
        Attempt(false, PicturesPiece.Sentence, place),
        Attempt(false, PicturesPiece.Sentence, dropped),
        Attempt(false, PicturesPiece.ShortSentence, dropped),
        Attempt(false, null, dropped)
    ).distinct()
}

private fun panelPlan(
    size: DpSize,
    s: Float,
    m: PicturesMeasure,
    content: PicturesContent,
    stale: Boolean
): PicturesPlan? {
    val numberSp = headerNumberSp(PanelNumberSp, PanelNumberLine, s)
    val numberWidth = m.width(PicturesPiece.Number, numberSp)
    val attempts = panelAttempts(content.glyph, stale)
    // The glyph goes first because a wider column may wrap the sentence shorter; where it did
    // not, the attempt that fits is tried again with the glyph, and keeps it if it still fits.
    val tries = attempts.flatMap { a ->
        if (content.glyph && !a.glyph) listOf(a.copy(glyph = true), a) else listOf(a)
    }.distinct()
    for (attempt in tries) {
        val numberLeft = PanelPaddingSide + if (attempt.glyph) PanelGlyph + PicturesHeaderGutter else 0.dp
        val column = size.width - PanelPaddingSide - (numberLeft + numberWidth + PicturesHeaderGutter)
        if (column < PicturesWordsMin) continue
        val lines = attempt.sentence?.let { m.lines(it, PicturesSentenceSp, column) } ?: 0
        val placeSp = if (attempt.place == PicturesPiece.Stale) PicturesStaleSp else PanelPlaceSp
        val placeLine = if (attempt.place == PicturesPiece.Stale) PicturesStaleLine else PanelPlaceLine
        val placeLines = attempt.place?.let { m.lines(it, placeSp, column) } ?: 0
        val words = if (lines + placeLines == 0) {
            0.dp
        } else {
            PanelWordsTop + box(PanelSentenceLine, lines, s) +
                (if (lines > 0 && placeLines > 0) PicturesWordsGap else 0.dp) +
                box(placeLine, placeLines, s)
        }
        val top = plateTop(PanelPaddingTop, words)
        val plateBottom = top + PlateHeight
        if (plateBottom > size.height - PicturesPaddingBottom) continue
        val room = size.height - PicturesPaddingBottom - plateBottom
        val dayRows = if (content.days) daysThatFit(room) else 0
        val hours = dayRows == 0 && room >= PicturesHoursGap + hoursCellHeight(s)
        return PicturesPlan(
            form = PicturesForm.Panel,
            extra = when {
                dayRows > 0 -> PicturesExtra.Days
                hours -> PicturesExtra.Hours
                else -> PicturesExtra.None
            },
            numberSp = numberSp,
            sentence = attempt.sentence.takeIf { lines > 0 },
            sentenceLines = lines,
            placeLine = attempt.place.takeIf { placeLines > 0 },
            placeLines = placeLines,
            glyph = if (attempt.glyph) {
                DpRect(
                    PanelPaddingSide, PanelPaddingTop + PanelGlyphTop,
                    PanelPaddingSide + PanelGlyph, PanelPaddingTop + PanelGlyphTop + PanelGlyph
                )
            } else {
                null
            },
            plate = DpRect(PanelPaddingSide, top, size.width - PanelPaddingSide, plateBottom),
            extraTop = when {
                dayRows > 0 -> plateBottom + PicturesDaysGap
                hours -> plateBottom + PicturesHoursGap
                else -> 0.dp
            },
            hourColumns = if (hours) PicturesHourColumns else 0,
            dayRows = dayRows
        )
    }
    return null
}

/** The narrowest column worth a word beside the number: «Pioggia» at 16 sp with its air. A
 * header that leaves less has no sentence to give, and the attempt is not made. */
internal val PicturesWordsMin = 64.dp

/** The 4×3's hours: 8 dp of gap and 4 of margin under the plate, six columns 4 dp apart; a
 * cell is the hour (13 sp on 15), the glyph (28), the temperature (13 sp on 15) and the
 * chance (11 sp on 13), 3 dp between them. */
internal val PicturesHoursGap = 12.dp
internal const val PicturesHourColumns = 6
internal val PicturesHourColumnGap = 4.dp
internal val PicturesHourGlyph = 28.dp

internal fun hoursCellHeight(s: Float): Dp =
    (15f * s + 3f + PicturesHourGlyph.value + 3f + 15f * s + 3f + 13f * s).dp

/** The 4×4's days: 8 dp of gap and 6 of margin under the plate, rows of 38 — five, or as
 * many whole ones as the height holds down to three. Fewer than three are not a week, and
 * the height buys the hours instead. */
internal val PicturesDaysGap = 14.dp
internal const val PicturesDaysMin = 3

private fun daysThatFit(room: Dp): Int {
    val rows = floor((room - PicturesDaysGap).value / WeekRange.ROW_HEIGHT).toInt()
        .coerceAtMost(WeekRange.DAYS)
    return if (rows >= PicturesDaysMin) rows else 0
}

/** The 4×4's row, left to right: the day's name (40), its glyph (26), the chance (30), the
 * low (24), the bar (what is left), the high (26), 8 dp apart, inside 2 dp of padding. */
internal val DayNameColumn = 40.dp
internal val DayGlyph = 26.dp
internal val DayChanceColumn = 30.dp
internal val DayLowColumn = 24.dp
internal val DayHighColumn = 26.dp
internal val DayGap = 8.dp
internal val DayPadding = 2.dp

/** The range bar's track on a card [width] wide: 122 dp on the 4×4. */
internal fun dayBarWidth(width: Dp): Dp =
    width - PanelPaddingSide * 2 - DayPadding * 2 -
        (DayNameColumn + DayGlyph + DayChanceColumn + DayLowColumn + DayHighColumn) - DayGap * 5

// ---------------------------------------------------------------------------------------
// The 3×2: the compact plate.
// ---------------------------------------------------------------------------------------

/** The 3×2's number: 44 sp in a 48 dp box; its sentence 16 sp on 19, 3 dp down. No place: on
 * 250 dp «il luogo cede il posto alla frase». */
internal const val CompactNumberSp = 44f
internal const val CompactNumberLine = 48f
internal const val CompactSentenceLine = 19f
internal val CompactWordsTop = 3.dp

private fun compactPlan(size: DpSize, s: Float, m: PicturesMeasure, stale: Boolean): PicturesPlan? {
    val numberSp = headerNumberSp(CompactNumberSp, CompactNumberLine, s)
    val left = CompactPaddingSide + m.width(PicturesPiece.Number, numberSp) + PicturesHeaderGutter
    val column = size.width - CompactPaddingSide - left
    if (column < PicturesWordsMin) return null
    val staleLines = if (stale) m.lines(PicturesPiece.Stale, PicturesStaleSp, column) else 0
    for (sentence in listOf(PicturesPiece.Sentence, PicturesPiece.ShortSentence, null)) {
        val lines = sentence?.let { m.lines(it, PicturesSentenceSp, column) } ?: 0
        val words = if (lines + staleLines == 0) {
            0.dp
        } else {
            CompactWordsTop + box(CompactSentenceLine, lines, s) +
                (if (lines > 0 && staleLines > 0) PicturesWordsGap else 0.dp) +
                box(PicturesStaleLine, staleLines, s)
        }
        val top = plateTop(PanelPaddingTop, words)
        val plateBottom = top + PlateHeight
        if (plateBottom > size.height - PicturesPaddingBottom) continue
        val room = size.height - PicturesPaddingBottom - plateBottom
        val hours = room >= PicturesHoursGap + hoursCellHeight(s)
        return PicturesPlan(
            form = PicturesForm.Compact,
            extra = if (hours) PicturesExtra.Hours else PicturesExtra.None,
            numberSp = numberSp,
            sentence = sentence.takeIf { lines > 0 },
            sentenceLines = lines,
            placeLine = PicturesPiece.Stale.takeIf { staleLines > 0 },
            placeLines = staleLines,
            plate = DpRect(CompactPaddingSide, top, size.width - CompactPaddingSide, plateBottom),
            extraTop = if (hours) plateBottom + PicturesHoursGap else 0.dp,
            hourColumns = if (hours) PicturesHourColumns else 0
        )
    }
    return null
}

// ---------------------------------------------------------------------------------------
// The 2×2 and the 2×3: the dial.
// ---------------------------------------------------------------------------------------

/** The dial's forms: 12 dp of padding, the dial 4 dp down, 6 dp between the pieces. */
internal val DialPadding = 12.dp
internal val DialTop = 4.dp
internal val DialGap = 6.dp

/** The 2×2's words under the dial: the short sentence 13 sp Medium on 16, the place 11 sp on
 * 13, 4 dp closer than the gap (`margin-top: -4px`). */
internal const val DialSentenceSp = WidgetType.Label
internal const val DialSentenceLine = 16f
internal const val DialPlaceSp = WidgetType.Caption
internal const val DialPlaceLine = 13f
internal val DialPlaceGap = 2.dp

/** The 2×3's sentence: the whole one, 16 sp on 19, centred. */
internal const val DialLongSentenceLine = 19f

/** Inside the dial: the number 38 sp in 40, 6 dp right of centre (the degree sign's own
 * weight), the range 13 sp on 16 under it. */
internal const val DialNumberSp = 38f
internal const val DialNumberLine = 40f
internal val DialNumberNudge = 6.dp
internal const val DialRangeSp = WidgetType.Label
internal const val DialRangeLine = 16f

/** Where the dial's card is tall enough to be the 2×3 (three rows are ~290 dp; two never
 * over ~200): from here it prints the whole sentence, as the 2×3 does, rows or no rows. */
internal val DialTallMinHeight = 245.dp

/** A dial is never drawn smaller than this: a 96 dp ring still holds «11°» and its range
 * inside it at 100%. Under it the card is the 2×1. */
internal val DialMin = 96.dp

/** The smallest number inside the dial: under it the 2×2 is not carrying a number, it is
 * carrying a caption. */
internal const val DialNumberFloor = 24f

/** «Più tardi»: a panel 6 dp in at the top and the foot, 10 at the sides, rows of 24 three
 * hours apart — three, or two where only two fit. One is not «later». */
internal val LaterPaddingVertical = 6.dp
internal val LaterPaddingSide = 10.dp
internal val LaterRowHeight = 24.dp
internal const val LaterMaxRows = 3
internal const val LaterMinRows = 2

private fun dialPlan(size: DpSize, s: Float, m: PicturesMeasure, stale: Boolean): PicturesPlan? {
    val inner = size.width - DialPadding * 2
    val avail = size.height - DialPadding * 2
    val staleLines = if (stale) m.lines(PicturesPiece.Stale, PicturesStaleSp, inner) else 0
    val staleBlock = if (staleLines > 0) DialPlaceGap + box(PicturesStaleLine, staleLines, s) else 0.dp

    // The tall dial first: where the whole sentence fits under a full 124 dp dial, the card
    // is the 2×3 — the whole sentence, and «Più tardi» in as many whole rows as the height
    // then holds (two or three), or, where none, the place under the sentence as on the 2×2.
    // The height buys rows only once the dial and the words are paid.
    val full = minOf(DialSpec.LARGE.size.dp, inner)
    if (full >= DialSpec.LARGE.size.dp) {
        val lines = m.lines(PicturesPiece.Sentence, PicturesSentenceSp, inner)
        val words = (if (lines > 0) DialGap + box(DialLongSentenceLine, lines, s) else 0.dp) + staleBlock
        val above = DialTop + full + words + DialGap + LaterPaddingVertical * 2
        val rows = floor((avail - above).value / LaterRowHeight.value).toInt().coerceAtMost(LaterMaxRows)
        if (lines > 0 && rows >= LaterMinRows) {
            val laterTop = DialPadding + DialTop + full + words + DialGap
            return dialPlanOf(
                size, s, m, full,
                sentence = PicturesPiece.Sentence, lines = lines,
                place = PicturesPiece.Stale.takeIf { staleLines > 0 }, placeLines = staleLines
            ).copy(
                extra = PicturesExtra.Later,
                later = DpRect(
                    DialPadding, laterTop, size.width - DialPadding,
                    laterTop + LaterPaddingVertical * 2 + LaterRowHeight * rows
                ),
                laterRows = rows
            )
        }
        val placeLines = if (stale) staleLines else m.lines(PicturesPiece.Place, DialPlaceSp, inner)
        val withPlace = words + if (!stale && placeLines > 0) DialPlaceGap + box(DialPlaceLine, placeLines, s) else 0.dp
        // Taller than the 2×2 needs: a 2×2 at its own 189 dp keeps its short sentence.
        if (lines > 0 && avail - DialTop - full >= withPlace && size.height >= DialTallMinHeight) {
            return dialPlanOf(
                size, s, m, full,
                sentence = PicturesPiece.Sentence, lines = lines,
                place = (if (stale) PicturesPiece.Stale else PicturesPiece.Place).takeIf { placeLines > 0 },
                placeLines = placeLines
            )
        }
    }

    // The 2×2: the dial as large as the words leave it, down to [DialMin], then the place
    // goes, then the sentence. Never the stale marker.
    val attempts = if (stale) {
        listOf(PicturesPiece.ShortSentence to PicturesPiece.Stale, null to PicturesPiece.Stale)
    } else {
        listOf(
            PicturesPiece.ShortSentence to PicturesPiece.Place,
            PicturesPiece.ShortSentence to null,
            null to null
        )
    }
    for ((sentence, place) in attempts) {
        val lines = sentence?.let { m.lines(it, DialSentenceSp, inner) } ?: 0
        val placeSp = if (place == PicturesPiece.Stale) PicturesStaleSp else DialPlaceSp
        val placeLines = place?.let { m.lines(it, placeSp, inner) } ?: 0
        val words = (if (lines > 0) DialGap + box(DialSentenceLine, lines, s) else 0.dp) +
            (if (placeLines > 0) DialPlaceGap + box(DialPlaceLine, placeLines, s) else 0.dp)
        val side = minOf(DialSpec.LARGE.size.dp, inner, avail - DialTop - words)
        if (side < DialMin) continue
        return dialPlanOf(
            size, s, m, side,
            sentence = sentence.takeIf { lines > 0 }, lines = lines,
            place = place.takeIf { placeLines > 0 }, placeLines = placeLines
        )
    }
    return null
}

private fun dialPlanOf(
    size: DpSize,
    s: Float,
    m: PicturesMeasure,
    side: Dp,
    sentence: PicturesPiece?,
    lines: Int,
    place: PicturesPiece?,
    placeLines: Int
): PicturesPlan {
    val (numberSp, range) = dialNumber(side, s, m)
    val left = (size.width - side) / 2
    return PicturesPlan(
        form = PicturesForm.Dial,
        numberSp = numberSp,
        sentence = sentence,
        sentenceLines = lines,
        placeLine = place,
        placeLines = placeLines,
        dial = DpRect(left, DialPadding + DialTop, left + side, DialPadding + DialTop + side),
        dialRange = range
    )
}

/**
 * The number inside a dial of [side] dp, and whether the range fits under it: the canvas's
 * 38 sp, smaller only where the number (with its 6 dp nudge) would be wider than the ring's
 * inside at the height it stands at — the chord of the inner circle at the block's top edge.
 * At 100% on the 124 dp dial «11°» has 90 dp of chord for its 55.
 */
internal fun dialNumber(side: Dp, s: Float, m: PicturesMeasure): Pair<Float, Boolean> {
    val radius = side.value / 2f - DialSpec.LARGE.ring
    fun block(sp: Float) = DialNumberLine / DialNumberSp * sp * s + DialRangeLine * s
    fun chord(sp: Float): Float {
        val half = block(sp) / 2f
        return if (half >= radius) 0f else 2f * sqrt(radius * radius - half * half)
    }
    var sp = DialNumberSp
    repeat(DIAL_FIT_PASSES) {
        val wide = m.width(PicturesPiece.Number, sp).value + DialNumberNudge.value
        val room = chord(sp)
        if (wide > room && wide > 0f) sp = (sp * room / wide).coerceAtLeast(DialNumberFloor)
    }
    val range = m.width(PicturesPiece.Range, DialRangeSp).value
    return sp to (range > 0f && range <= chord(sp))
}

private const val DIAL_FIT_PASSES = 4

// ---------------------------------------------------------------------------------------
// The 2×1: the number and the small dial.
// ---------------------------------------------------------------------------------------

/** The 2×1: 10 dp of padding at the top and foot, 12 at the sides; the number 30 sp in 32,
 * 1 dp over the short sentence 13 sp Medium on 15; the 52 dp dial 8 dp after the words. */
internal val DialRowPaddingVertical = 10.dp
internal val DialRowPaddingSide = 12.dp
internal const val DialRowNumberSp = WidgetType.Display
internal const val DialRowNumberLine = 32f
internal val DialRowWordsGap = 1.dp
internal const val DialRowSentenceSp = WidgetType.Label
internal const val DialRowSentenceLine = 15f
internal val DialRowGap = 8.dp

private fun dialRowPlan(size: DpSize, s: Float, m: PicturesMeasure, stale: Boolean): PicturesPlan {
    val avail = size.height - DialRowPaddingVertical * 2
    val numberSp = minOf(DialRowNumberSp, avail.value / (DialRowNumberLine / DialRowNumberSp * s))
        .coerceAtLeast(DialNumberFloor)
    val numberBox = (DialRowNumberLine / DialRowNumberSp * numberSp * s).dp
    val small = DialSpec.SMALL.size.dp
    val attempts = listOf(
        true to PicturesPiece.ShortSentence,
        false to PicturesPiece.ShortSentence,
        false to null
    )
    var last: PicturesPlan? = null
    for ((withDial, sentence) in attempts) {
        if (withDial && avail < small) continue
        val words = size.width - DialRowPaddingSide * 2 - if (withDial) small + DialRowGap else 0.dp
        if (words < m.width(PicturesPiece.Number, numberSp)) continue
        val lines = sentence?.let { m.lines(it, DialRowSentenceSp, words) } ?: 0
        val staleLines = if (stale) m.lines(PicturesPiece.Stale, PicturesStaleSp, words) else 0
        val height = numberBox +
            (if (lines > 0) DialRowWordsGap + box(DialRowSentenceLine, lines, s) else 0.dp) +
            (if (staleLines > 0) DialRowWordsGap + box(PicturesStaleLine, staleLines, s) else 0.dp)
        val plan = PicturesPlan(
            form = PicturesForm.DialRow,
            numberSp = numberSp,
            sentence = sentence.takeIf { lines > 0 },
            sentenceLines = lines,
            placeLine = PicturesPiece.Stale.takeIf { staleLines > 0 },
            placeLines = staleLines,
            dial = if (withDial) {
                val top = (size.height - small) / 2
                val left = size.width - DialRowPaddingSide - small
                DpRect(left, top, left + small, top + small)
            } else {
                null
            }
        )
        last = plan
        if (height <= avail) return plan
    }
    // Not even the number and the marker fit: the card still says them, a smaller launcher
    // cell clips nothing the reader can be misled by.
    return last ?: PicturesPlan(form = PicturesForm.DialRow, numberSp = numberSp)
}

// ---------------------------------------------------------------------------------------
// The 4×1: the number and the strip.
// ---------------------------------------------------------------------------------------

/** The 4×1: 12 dp of padding at the top and foot, 16 at the sides; the number 36 sp in 40,
 * 12 dp before the column; in the column the short sentence (16 sp Medium on 20) and the place
 * (13 sp quiet on 16) on one line, 8 dp apart at least, then 7 dp, then the strip. */
internal val StripPaddingVertical = 12.dp
internal val StripPaddingSide = 16.dp
internal const val StripNumberSp = 36f
internal const val StripNumberLine = 40f
internal val StripGutter = 12.dp
internal const val StripSentenceLine = 20f
internal const val StripPlaceSp = WidgetType.Label
internal const val StripPlaceLine = 16f
internal val StripPlaceGap = 8.dp
internal val StripLineGap = 7.dp

/** Under this the twelve hours' five labels («ora» and four clock hours at 11 sp) collide. */
internal val StripMinWidth = 120.dp

/**
 * The strip's box at [s]: the canvas's 18 dp, grown by what the labels' line grows. The
 * canvas sets its labels 9 dp down on a 12 dp line, so they hang 3 dp under the box at every
 * size; the box, and not the labels, is what the column centres, as there.
 */
internal fun stripHeight(s: Float): Dp =
    (StripMetrics.HEIGHT + StripMetrics.LABEL_LINE * (s - 1f).coerceAtLeast(0f)).dp

private fun stripPlan(size: DpSize, s: Float, m: PicturesMeasure, stale: Boolean): PicturesPlan? {
    val avail = size.height - StripPaddingVertical * 2
    val numberSp = minOf(StripNumberSp, avail.value / (StripNumberLine / StripNumberSp * s))
    val left = StripPaddingSide + m.width(PicturesPiece.Number, numberSp) + StripGutter
    val column = size.width - StripPaddingSide - left
    if (column < StripMinWidth) return null
    val stripBox = stripHeight(s)
    if (stripBox > avail) return null
    val place = if (stale) PicturesPiece.Stale else PicturesPiece.Place
    val sentenceWidth = m.width(PicturesPiece.ShortSentence, PicturesSentenceSp)
    val placeWidth = m.width(place, if (stale) PicturesStaleSp else StripPlaceSp)
    val hasSentence = sentenceWidth > 0.dp
    val hasPlace = placeWidth > 0.dp
    // One line, never wrapped (`white-space: nowrap`): the place goes first, then the sentence;
    // the stale marker stays even alone.
    val (sentence, placeLine) = when {
        hasSentence && hasPlace && sentenceWidth + StripPlaceGap + placeWidth <= column ->
            PicturesPiece.ShortSentence to place
        stale && hasPlace -> null to place
        hasSentence && sentenceWidth <= column -> PicturesPiece.ShortSentence to null
        hasPlace && placeWidth <= column -> null to place
        else -> null to null
    }
    var line = maxOf(
        if (sentence != null) StripSentenceLine * s else 0f,
        if (placeLine != null) (if (stale) PicturesStaleLine else StripPlaceLine) * s else 0f
    ).dp
    var keepSentence = sentence
    var keepPlace = placeLine
    if (line + StripLineGap + stripBox > avail && !stale) {
        line = 0.dp
        keepSentence = null
        keepPlace = null
    }
    val columnHeight = (if (line > 0.dp) line + StripLineGap else 0.dp) + stripBox
    val stripTop = StripPaddingVertical + (avail - columnHeight) / 2 + (columnHeight - stripBox)
    return PicturesPlan(
        form = PicturesForm.Strip,
        numberSp = numberSp,
        sentence = keepSentence,
        sentenceLines = if (keepSentence != null) 1 else 0,
        placeLine = keepPlace,
        placeLines = if (keepPlace != null) 1 else 0,
        strip = DpRect(left, stripTop, size.width - StripPaddingSide, stripTop + stripBox)
    )
}
