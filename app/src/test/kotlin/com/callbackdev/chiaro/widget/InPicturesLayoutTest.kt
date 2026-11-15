package com.callbackdev.chiaro.widget

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * «In immagini»'s eight forms at the reference grants (PLANNING, «Fase 17», la regola della
 * fedeltà, point 1): the canvas's positions at 100% of text, and at 115 and 130% what each
 * form gives up, in the order the canvas states, without a piece ever leaving the card.
 *
 * The words are the canvas's own example (Milan, 15 Nov 2026, 14:00), measured by [Roboto],
 * a table of the system face's advance widths good to a few percent: enough to wrap a
 * sentence the way the device will, which is all these budgets ask of it. The device
 * measures for real (`PicturesMeasure`).
 */
class InPicturesLayoutTest {

    private val twoByOne = DpSize(159.dp, 85.dp)
    private val fourByOne = DpSize(340.dp, 85.dp)
    private val twoByTwo = DpSize(159.dp, 189.dp)
    private val threeByTwo = DpSize(250.dp, 189.dp)
    private val fourByTwo = DpSize(340.dp, 189.dp)
    private val twoByThree = DpSize(159.dp, 293.dp)
    private val fourByThree = DpSize(340.dp, 293.dp)
    private val fourByFour = DpSize(340.dp, 397.dp)
    private val all = listOf(twoByOne, fourByOne, twoByTwo, threeByTwo, fourByTwo, twoByThree, fourByThree, fourByFour)
    private val scales = listOf(1f, 1.15f, 1.3f)

    private fun milan(scale: Float, stale: Boolean = false, warning: Boolean = false) = Roboto(
        scale,
        mapOf(
            PicturesPiece.Number to "11°",
            PicturesPiece.Sentence to "Pioggia dalle 21, smette all'alba",
            PicturesPiece.ShortSentence to "Pioggia dalle 21",
            PicturesPiece.Place to "Milano",
            PicturesPiece.PlaceAndRange to "Milano · ↑12° ↓6°",
            PicturesPiece.Range to "↑12° ↓6°"
        ) + (if (stale) mapOf(PicturesPiece.Stale to "Aggiornato alle 12:40") else emptyMap()) +
            (if (warning) mapOf(PicturesPiece.Warning to "Allerta arancione") else emptyMap())
    )

    private fun plan(
        size: DpSize,
        scale: Float = 1f,
        stale: Boolean = false,
        content: PicturesContent = PicturesContent(),
        warning: Boolean = false
    ) = picturesPlan(size, scale, milan(scale, stale, warning), content)

    private fun assertRect(what: String, expected: DpRect, actual: DpRect?) {
        assertNotNull("$what is drawn", actual)
        val a = actual!!
        listOf(
            "left" to (expected.left to a.left), "top" to (expected.top to a.top),
            "right" to (expected.right to a.right), "bottom" to (expected.bottom to a.bottom)
        ).forEach { (edge, pair) -> assertEquals("$what $edge", pair.first.value, pair.second.value, 0.05f) }
    }

    private fun rect(l: Float, t: Float, r: Float, b: Float) = DpRect(l.dp, t.dp, r.dp, b.dp)

    @Test
    fun `the eight reference grants ask for the canvas's forms`() {
        assertEquals(PicturesForm.DialRow, picturesForm(twoByOne))
        assertEquals(PicturesForm.Strip, picturesForm(fourByOne))
        assertEquals(PicturesForm.Dial, picturesForm(twoByTwo))
        assertEquals(PicturesForm.Compact, picturesForm(threeByTwo))
        assertEquals(PicturesForm.Panel, picturesForm(fourByTwo))
        assertEquals(PicturesForm.Dial, picturesForm(twoByThree))
        assertEquals(PicturesForm.Panel, picturesForm(fourByThree))
        assertEquals(PicturesForm.Panel, picturesForm(fourByFour))
    }

    @Test
    fun `at 100 percent every reference grant keeps its form and its extra`() {
        val expected = listOf(
            PicturesForm.DialRow to PicturesExtra.None,
            PicturesForm.Strip to PicturesExtra.None,
            PicturesForm.Dial to PicturesExtra.None,
            PicturesForm.Compact to PicturesExtra.None,
            PicturesForm.Panel to PicturesExtra.None,
            PicturesForm.Dial to PicturesExtra.Later,
            PicturesForm.Panel to PicturesExtra.Hours,
            PicturesForm.Panel to PicturesExtra.Days
        )
        all.zip(expected).forEach { (size, want) ->
            val p = plan(size)
            assertEquals("$size", want, p.form to p.extra)
        }
    }

    /** The anatomy board, piece by piece: the 4×2 is the reference. */
    @Test
    fun `the 4x2 is the canvas's anatomy`() {
        val p = plan(fourByTwo)
        assertRect("glyph", rect(14f, 15f, 60f, 61f), p.glyph)
        assertEquals(PanelNumberSp, p.numberSp, 0f)
        assertEquals(PicturesPiece.Sentence, p.sentence)
        assertEquals(2, p.sentenceLines)
        assertEquals(PicturesPiece.PlaceAndRange, p.placeLine)
        assertEquals(1, p.placeLines)
        // The canvas's plate is at 76: its header is 54 + 8, and its 63 dp of words end 1 dp
        // inside the plate. Siblings cannot overlap, so the plate stands at 77 and ends on the
        // card's 12 dp foot exactly (PLANNING, 17.1).
        assertRect("plate", rect(14f, 77f, 326f, 177f), p.plate)
        assertEquals(189f - PicturesPaddingBottom.value, p.plate!!.bottom.value, 0.01f)
    }

    @Test
    fun `a one-line sentence puts the plate where the canvas has it`() {
        val p = picturesPlan(fourByTwo, 1f, milan(1f).with(PicturesPiece.Sentence to "Nuvoloso"))
        assertEquals(1, p.sentenceLines)
        assertRect("plate", rect(14f, 76f, 326f, 176f), p.plate)
    }

    @Test
    fun `the 3x2 has no glyph and no place, and its plate is 226 wide`() {
        val p = plan(threeByTwo)
        assertNull(p.glyph)
        assertNull(p.placeLine)
        assertEquals(CompactNumberSp, p.numberSp, 0f)
        assertEquals(PicturesPiece.Sentence, p.sentence)
        assertEquals(2, p.sentenceLines)
        assertRect("plate", rect(12f, 76f, 238f, 176f), p.plate)
    }

    @Test
    fun `the 2x2 is the dial at 124 with the number inside`() {
        val p = plan(twoByTwo)
        assertRect("dial", rect(17.5f, 16f, 141.5f, 140f), p.dial)
        assertEquals(DialNumberSp, p.numberSp, 0f)
        assertTrue(p.dialRange)
        assertEquals(PicturesPiece.ShortSentence, p.sentence)
        assertEquals(1, p.sentenceLines)
        assertEquals(PicturesPiece.Place, p.placeLine)
    }

    @Test
    fun `the 2x3 is the dial, the whole sentence and three hours later`() {
        val p = plan(twoByThree)
        assertRect("dial", rect(17.5f, 16f, 141.5f, 140f), p.dial)
        assertEquals(PicturesPiece.Sentence, p.sentence)
        assertEquals(2, p.sentenceLines)
        assertNull(p.placeLine)
        assertEquals(3, p.laterRows)
        // 12 + 4 + 124 + (6 + 2 × 19) + 6 = 190; 6 + 3 × 24 + 6 = 84.
        assertRect("later", rect(12f, 190f, 147f, 274f), p.later)
    }

    @Test
    fun `the 4x3 and the 4x4 are the 4x2 with hours and with days`() {
        val hours = plan(fourByThree)
        assertRect("plate", rect(14f, 77f, 326f, 177f), hours.plate)
        assertEquals(PicturesHourColumns, hours.hourColumns)
        assertEquals(189f, hours.extraTop.value, 0.01f)
        assertTrue(hours.extraTop + hoursCellHeight(1f) <= fourByThree.height - PicturesPaddingBottom)

        val days = plan(fourByFour)
        assertEquals(5, days.dayRows)
        assertEquals(191f, days.extraTop.value, 0.01f)
        assertEquals(122f, dayBarWidth(fourByFour.width).value, 0.01f)
    }

    @Test
    fun `the 4x4 without its days buys the hours`() {
        val p = plan(fourByFour, content = PicturesContent(days = false))
        assertEquals(PicturesExtra.Hours, p.extra)
    }

    @Test
    fun `the 2x1 is the number, the short sentence and the small dial`() {
        val p = plan(twoByOne)
        assertRect("dial", rect(95f, 16.5f, 147f, 68.5f), p.dial)
        assertEquals(DialRowNumberSp, p.numberSp, 0f)
        assertEquals(PicturesPiece.ShortSentence, p.sentence)
    }

    @Test
    fun `the 4x1 is the number and twelve hours of strip, centred on the row`() {
        val m = milan(1f)
        val p = picturesPlan(fourByOne, 1f, m)
        assertEquals(PicturesPiece.ShortSentence, p.sentence)
        assertEquals(PicturesPiece.Place, p.placeLine)
        val left = 16f + m.width(PicturesPiece.Number, StripNumberSp).value + 12f
        // The column is 20 + 7 + 18 = 45 dp, centred in 61: it starts at 20, the strip at 47.
        assertRect("strip", rect(left, 47f, 324f, 65f), p.strip)
    }

    @Test
    fun `at 115 and 130 percent nothing leaves the card`() {
        for (scale in scales) for (size in all) for (stale in listOf(false, true)) {
            val p = plan(size, scale, stale)
            val what = "$size at $scale${if (stale) ", stale" else ""}"
            listOfNotNull(p.glyph, p.dial, p.plate, p.strip, p.later).forEach { r ->
                assertTrue("$what: $r", r.left >= 0.dp && r.top >= 0.dp && r.right <= size.width && r.bottom <= size.height)
            }
            p.plate?.let { assertTrue("$what: the plate's foot", it.bottom <= size.height - PicturesPaddingBottom) }
            assertTrue("$what: the words", wordsBottom(size, scale, p) <= size.height + 0.01.dp)
        }
    }

    @Test
    fun `the stale marker is never what gives way`() {
        for (scale in scales) for (size in all) {
            val p = plan(size, scale, stale = true)
            assertEquals("$size at $scale", PicturesPiece.Stale, p.placeLine)
        }
    }

    @Test
    fun `the warning takes the place's line and never gives way, the stale marker comes first`() {
        for (scale in scales) for (size in all) {
            assertEquals("$size at $scale", PicturesPiece.Warning, plan(size, scale, warning = true).placeLine)
            assertEquals("$size at $scale", PicturesPiece.Stale, plan(size, scale, stale = true, warning = true).placeLine)
            val p = plan(size, scale, warning = true)
            listOfNotNull(p.glyph, p.dial, p.plate, p.strip, p.later).forEach { r ->
                assertTrue("$size at $scale: $r", r.right <= size.width && r.bottom <= size.height)
            }
        }
        // On the reference 4×2 the chip replaces «Milano · ↑12° ↓6°», 21.2 dp against 17: the
        // two-line sentence and the chip are 67 dp, 4 more than the card has over its plate. The
        // chip never gives way, so the glyph does, and in the wider column the sentence is one
        // line.
        val p = plan(fourByTwo, warning = true)
        assertEquals(PicturesForm.Panel, p.form)
        assertNull(p.glyph)
        assertEquals(PicturesPiece.Sentence, p.sentence)
        assertEquals(1, p.sentenceLines)
    }

    @Test
    fun `the 4x2 at 115 and 130 percent gives up the place, never the sentence`() {
        for (scale in listOf(1.15f, 1.3f)) {
            val p = plan(fourByTwo, scale)
            assertNotNull(p.plate)
            assertEquals(PicturesPiece.Sentence, p.sentence)
            assertNull("$scale: the place gives way", p.placeLine)
            // The glyph was tried first and did not shorten the sentence, so it stays.
            assertNotNull("$scale: the glyph", p.glyph)
            // The number's box never outgrows the header: 54 dp at 130% is 39.8 sp.
            assertTrue(PanelNumberLine / PanelNumberSp * p.numberSp * scale <= PicturesHeaderHeight.value + 0.01f)
        }
    }

    @Test
    fun `the glyph goes where giving it up shortens the sentence`() {
        // At 130% a sentence that wraps to three lines beside the glyph and two without it.
        val m = milan(1.3f).with(PicturesPiece.Sentence to "Pioggia dalle 21, smette verso l'alba")
        val number = m.width(PicturesPiece.Number, 54f / (PanelNumberLine / PanelNumberSp * 1.3f))
        assertEquals(3, m.lines(PicturesPiece.Sentence, 16f, 312.dp - 56.dp - number - 10.dp))
        assertEquals(2, m.lines(PicturesPiece.Sentence, 16f, 312.dp - number - 10.dp))
        val p = picturesPlan(fourByTwo, 1.3f, m)
        assertNull(p.glyph)
        assertEquals(PicturesPiece.Sentence, p.sentence)
    }

    @Test
    fun `the 2x3 keeps the whole sentence when the rows do not fit`() {
        val p = plan(twoByThree, 1.3f)
        assertEquals(PicturesForm.Dial, p.form)
        assertEquals(PicturesPiece.Sentence, p.sentence)
        assertEquals(PicturesPiece.Place, p.placeLine)
        assertEquals(0, p.laterRows)
    }

    @Test
    fun `the dial shrinks before a word goes, and never under its floor`() {
        val p = plan(twoByTwo, 1.3f)
        assertEquals(PicturesForm.Dial, p.form)
        val side = p.dial!!.right - p.dial!!.left
        assertTrue("$side", side < 124.dp && side >= DialMin)
        assertEquals(PicturesPiece.Place, p.placeLine)
    }

    @Test
    fun `a card too short for its drawing hands over to a smaller form`() {
        // A 4×2 on a launcher that grants 170 dp: the plate does not fit, the strip does.
        assertEquals(PicturesForm.Strip, plan(DpSize(340.dp, 170.dp)).form)
        // A narrow card one row high is the 2×1, even on a short grant.
        assertEquals(PicturesForm.DialRow, plan(DpSize(159.dp, 120.dp)).form)
        // One cell: the number and what fits, no dial.
        val one = plan(DpSize(80.dp, 85.dp))
        assertEquals(PicturesForm.DialRow, one.form)
        assertNull(one.dial)
    }

    /** Where the last line of words ends, for the forms whose words sit under or beside a
     * drawing: the arithmetic the plan spends, re-added independently. */
    private fun wordsBottom(size: DpSize, s: Float, p: PicturesPlan): Dp = when (p.form) {
        PicturesForm.Dial -> {
            val under = p.later?.top ?: (p.dial!!.bottom +
                (if (p.sentenceLines > 0) DialGap + ((if (p.sentence == PicturesPiece.Sentence) DialLongSentenceLine else DialSentenceLine) * p.sentenceLines * s).dp else 0.dp) +
                (if (p.placeLines > 0) DialPlaceGap + (DialPlaceLine * p.placeLines * s).dp else 0.dp))
            maxOf(under, p.later?.bottom ?: 0.dp)
        }
        else -> p.plate?.bottom ?: p.strip?.bottom ?: 0.dp
    }
}

/**
 * A measure that wraps like the device, from a table of Roboto's advance widths in ems (the
 * regular face's; the bold digits and the degree sign are within 2% of them). Greedy, word by
 * word, as `StaticLayout` breaks a line.
 */
internal class Roboto(
    private val scale: Float,
    private val texts: Map<PicturesPiece, String>
) : PicturesMeasure {

    fun with(vararg pieces: Pair<PicturesPiece, String>) = Roboto(scale, texts + pieces)

    override fun width(piece: PicturesPiece, sp: Float): Dp = (em(texts[piece].orEmpty()) * sp * scale).dp

    override fun lines(piece: PicturesPiece, sp: Float, width: Dp): Int {
        val text = texts[piece].orEmpty()
        if (text.isEmpty()) return 0
        val unit = sp * scale
        val space = em(" ") * unit
        var lines = 1
        var used = 0f
        for (word in text.split(' ')) {
            val w = em(word) * unit
            used = when {
                used == 0f -> w
                used + space + w <= width.value -> used + space + w
                else -> {
                    lines++
                    w
                }
            }
        }
        return lines
    }

    private fun em(text: String): Float = text.sumOf { c ->
        when (c) {
            in '0'..'9' -> 0.562
            '°' -> 0.385
            '−', '-' -> 0.55
            ' ' -> 0.248
            '·', '.', ',', ':', '\'', '’' -> 0.22
            '↑', '↓' -> 0.6
            'i', 'l', 'j' -> 0.243
            't', 'f', 'r' -> 0.34
            'm' -> 0.876
            'w' -> 0.75
            in 'a'..'z' -> 0.55
            'M' -> 0.87
            in 'A'..'Z' -> 0.65
            else -> 0.55
        }.toDouble()
    }.toFloat()
}
