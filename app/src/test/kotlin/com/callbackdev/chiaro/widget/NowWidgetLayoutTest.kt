package com.callbackdev.chiaro.widget

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Which form the Now widget takes for a given grant, and how big its glyph gets
 * (committente, 8 set 2026).
 *
 * The sizes below are the reference device's, read off its screenshot at ~2.35 px/dp
 * (1080 px wide, density ~2.75, image scaled to 923): a one-row card is ~82 dp tall,
 * two cells are ~159 dp wide, three ~250, four ~340, and a two-by-two is ~159 × 189.
 * The 101 dp row is the other figure the launcher has been seen to grant (4th device
 * pass), and 320 is four cells on a five-column grid. The form is arithmetic on dp
 * precisely so it can be pinned here rather than by one phone's idea of a cell.
 *
 * **The density was re-read on 12 set 2026** and the reference device is 2.8125 px/dp,
 * not 2.75: on the Today screenshot its 16 dp tile inset measures 45 px and its 12 dp
 * gutter 34, so the screen is 384 dp wide and the one-row card is ~85 dp rather than
 * ~82. The fixtures below keep their old figures — every one of them is on the same
 * side of every threshold at either reading — and the 85 dp row is pinned beside them.
 */
class NowWidgetLayoutTest {

    private val twoByOne = DpSize(159.dp, 82.dp)
    private val threeByOne = DpSize(250.dp, 82.dp)
    private val fourByOne = DpSize(340.dp, 82.dp)
    private val twoByTwo = DpSize(159.dp, 189.dp)

    /** From three cells by two rows the card is the hours' card (13 ott 2026). */
    @Test
    fun `the hours come with three cells and two rows, never with one of the two`() {
        assertTrue(nowShowsHours(DpSize(250.dp, 189.dp)))
        assertTrue(nowShowsHours(DpSize(340.dp, 189.dp)))
        assertTrue(nowShowsHours(DpSize(340.dp, 397.dp)))
        assertFalse(nowShowsHours(fourByOne))
        assertFalse(nowShowsHours(twoByTwo))
        assertFalse(nowShowsHours(DpSize(159.dp, 397.dp)))
    }

    @Test
    fun `one row at two or three cells is the narrow form`() {
        assertEquals(NowLayout.NARROW, nowLayout(twoByOne))
        assertEquals(NowLayout.NARROW, nowLayout(threeByOne))
        // A taller row grows the number, and its glyph with it; still narrow.
        assertEquals(NowLayout.NARROW, nowLayout(DpSize(250.dp, 101.dp)))
    }

    @Test
    fun `one row at four cells carries the sentence`() {
        assertEquals(NowLayout.WIDE, nowLayout(fourByOne))
        assertEquals(NowLayout.WIDE, nowLayout(DpSize(340.dp, 101.dp)))
        // Four cells on a five-column grid: the narrowest card that still qualifies.
        assertEquals(NowLayout.WIDE, nowLayout(DpSize(320.dp, 101.dp)))
    }

    @Test
    fun `two rows are the tall form whatever the width`() {
        assertEquals(NowLayout.TALL, nowLayout(twoByTwo))
        assertEquals(NowLayout.TALL, nowLayout(DpSize(340.dp, 189.dp)))
        assertEquals(NowLayout.TALL, nowLayout(DpSize(159.dp, 150.dp)))
        assertEquals(NowLayout.WIDE, nowLayout(DpSize(340.dp, 149.dp)))
    }

    @Test
    fun `the tall glyph is what the text block leaves, plus the shared leading band`() {
        // Text: 34 × 1.32 + 2 × 16 × 1.32 + 16 × 1.32 = 108.24. Room: 189 − 6 − 14 −
        // 108.24 + 34 × 0.24 = 68.92 — about 50 dp of drawing on the reference two-by-two.
        assertEquals(68.92f, nowTallIconSize(twoByTwo, fontScale = 1f, stale = false).value, 0.05f)
        // A stale marker under the place costs its 11 sp line: 68.92 − 14.52.
        assertEquals(54.4f, nowTallIconSize(twoByTwo, fontScale = 1f, stale = true).value, 0.05f)
        // The shorter two-row grant the launcher has also been seen to make.
        assertEquals(62.92f, nowTallIconSize(DpSize(159.dp, 183.dp), fontScale = 1f, stale = false).value, 0.05f)
    }

    @Test
    fun `the tall glyph keeps its floor and its ceiling`() {
        // At the largest font the text block outgrows the room; the floor holds and
        // the glyph's own margin absorbs the few dp of overlap.
        assertEquals(52f, nowTallIconSize(twoByTwo, fontScale = 1.3f, stale = false).value, 0.01f)
        // Three rows: the ceiling stops the icon turning into a poster.
        assertEquals(104f, nowTallIconSize(DpSize(159.dp, 290.dp), fontScale = 1f, stale = false).value, 0.01f)
    }

    @Test
    fun `with the sentence turned off the tall glyph takes its two lines`() {
        // 68.92 + 2 × 16 × 1.32 = 111.16, over the ceiling.
        assertEquals(
            104f,
            nowTallIconSize(twoByTwo, fontScale = 1f, stale = false, withSentence = false).value,
            0.01f
        )
        // On the shorter grant too (62.92 + 42.24 = 105.16); with a stale marker it
        // comes back under the ceiling: 105.16 − 14.52.
        assertEquals(
            104f,
            nowTallIconSize(DpSize(159.dp, 183.dp), fontScale = 1f, stale = false, withSentence = false).value,
            0.01f
        )
        assertEquals(
            90.64f,
            nowTallIconSize(DpSize(159.dp, 183.dp), fontScale = 1f, stale = true, withSentence = false).value,
            0.05f
        )
    }

    // ------------------------------------- the one-row card (15 nov 2026): the text card's

    private val reference = DpSize(340.dp, 85.dp)

    /** «Proprio uguale nel layout»: the place's column is the text card's to the dp, so a
     * name it prints whole this card prints whole. */
    @Test
    fun `the wide row's leading column is the text card's, glyph inset lent back`() {
        assertEquals(textLeadingColumn(fourByOne), nowHeroLine(fourByOne, wide = true) - NowGlyphEdgeGive)
        assertEquals(168f, textLeadingColumn(reference).value, 0.01f)
        assertEquals(10f, NowGlyphEdgeGive.value, 0.01f)
    }

    /** The number is the text card's own on the same row: 39.3 sp on the reference 85 dp. */
    @Test
    fun `the number is the text card's, the glyph is its line box`() {
        listOf(reference, fourByOne, DpSize(340.dp, 101.dp), DpSize(320.dp, 101.dp)).forEach { size ->
            assertEquals(
                "$size",
                textRowHeroSp(size, 1f, stale = false),
                nowRowHeroSp(size, 1f, stale = false),
                0.01f
            )
        }
        assertEquals(39.30f, nowRowHeroSp(reference, 1f, stale = false), 0.01f)
        // 85 − 12 − 21.12 = 51.88: the glyph is exactly the number's line.
        assertEquals(51.88f, nowRowGlyphSize(reference, 1f, stale = false).value, 0.01f)
        // The tallest row the cap allows: 44 sp, a 58 dp glyph.
        assertEquals(44f, nowRowHeroSp(DpSize(340.dp, 101.dp), 1f, stale = false), 0.01f)
        assertEquals(58.08f, nowRowGlyphSize(DpSize(340.dp, 101.dp), 1f, stale = false).value, 0.01f)
    }

    /** The glyph and the widest number always fit the line together: 51.9 + 8 + 82.5 on the
     * reference, against the 178 the column and the lent inset give. */
    @Test
    fun `the glyph, the gap and the widest number fit the line`() {
        listOf(twoByOne, threeByOne, fourByOne, reference, DpSize(320.dp, 101.dp), DpSize(140.dp, 82.dp))
            .forEach { size ->
                listOf(1f, 1.15f, 1.3f).forEach { scale ->
                    if (!nowRowShowsIcon(size, scale)) return@forEach
                    val sp = nowRowHeroSp(size, scale, stale = false)
                    val line = nowRowGlyphSize(size, scale, stale = false) + IconTextGap +
                        (sp * TempEmWidth * scale).dp
                    val wide = nowLayout(size) == NowLayout.WIDE
                    assertTrue("$size @$scale: $line", line <= nowHeroLine(size, wide) + 0.01.dp)
                }
            }
    }

    /** Old data pays for its marker out of the number, as on the text card, and the glyph
     * follows the number down: 85 − 12 − 21.12 − 14.52 = 37.36. */
    @Test
    fun `a stale card's number pays for the marker and the glyph shrinks with it`() {
        assertEquals(textRowHeroSp(reference, 1f, stale = true), nowRowHeroSp(reference, 1f, stale = true), 0.01f)
        assertEquals(37.36f, nowRowGlyphSize(reference, 1f, stale = true).value, 0.01f)
        assertTrue(nowRowGlyphSize(reference, 1f, stale = true) >= NowGlyphMin)
    }

    /** At three cells the glyph's line would take the number under the size the height
     * gives it, so the row is the narrow form: the place over the glyph and the number,
     * across the whole card, the number at its full size. */
    @Test
    fun `three cells are the narrow form with the full number`() {
        assertEquals(TextForm.ROW, textForm(threeByOne))
        assertEquals(NowLayout.NARROW, nowLayout(threeByOne))
        assertEquals(textRowHeroSp(fourByOne, 1f, stale = false), nowRowHeroSp(threeByOne, 1f, stale = false), 0.01f)
        assertEquals(232f, nowHeroLine(threeByOne, wide = false).value, 0.01f)
    }

    /** The provider's 110 dp minimum cannot hold the glyph beside the widest number, and the
     * glyph goes; two cells and up keep it. */
    @Test
    fun `a row too narrow for the glyph beside the number drops the glyph`() {
        assertFalse(nowRowShowsIcon(DpSize(110.dp, 82.dp), 1f))
        assertEquals(0f, nowRowGlyphSize(DpSize(110.dp, 82.dp), 1f, stale = false).value, 0.01f)
        assertTrue(nowRowShowsIcon(DpSize(140.dp, 82.dp), 1f))
        assertTrue(nowRowShowsIcon(twoByOne, 1f))
        assertTrue(nowRowShowsIcon(threeByOne, 1.3f))
        assertTrue(nowRowShowsIcon(fourByOne, 1.3f))
        // With no glyph the number is sized to the words' column alone.
        assertTrue(nowRowHeroSp(DpSize(110.dp, 82.dp), 1f, stale = false) * TempEmWidth <= 82f + 0.01f)
    }

    // ------------------------------------------------- the warning chip (Fase 11)

    /** The chip's box at the default font size, the one number every budget below
     * subtracts: the 11 sp word's line (14.52) over the 12 dp mark, plus 3 either side,
     * and 4 dp of air above it. */
    private val chipBlock = warningChipHeight(1f) + WarningChipGap

    /** The far edge fills in the text card's order — a line of sentence, the warning, the
     * range, then more sentence — with the chip's box in the warning's place. */
    @Test
    fun `the far edge fills in the text card's order`() {
        assertEquals(TextRowPlan(3, false, false), nowRowPlan(fourByOne, 1f, true, warning = false, range = false))
        assertEquals(TextRowPlan(2, false, true), nowRowPlan(fourByOne, 1f, true, warning = false, range = true))
        assertEquals(TextRowPlan(2, true, false), nowRowPlan(fourByOne, 1f, true, warning = true, range = false))
        assertEquals(TextRowPlan(1, true, true), nowRowPlan(fourByOne, 1f, true, warning = true, range = true))
        // Never fewer than one line of sentence, whatever else the column holds.
        assertEquals(1, nowRowPlan(fourByOne, 1.3f, true, warning = true, range = true).sentenceLines)
        // The chip costs its own box, which is more than the text card's line of fact.
        assertTrue(chipBlock > textLineHeight(TextFactSp, 1f))
    }

    @Test
    fun `a one-row card has room for a line of sentence and the chip`() {
        assertTrue(nowRowHasWarningRow(fourByOne, 1f, sentence = true))
        assertTrue(nowRowHasWarningRow(fourByOne, 1f, sentence = false))
        // A row the launcher granted 45 dp of is not a row with a line to spare.
        assertFalse(nowRowHasWarningRow(DpSize(340.dp, 45.dp), 1f, sentence = true))
    }
    /**
     * The two-by-two is the card the chip does NOT fit on with the sentence up: 68.9 dp
     * of room for the glyph, 24.5 of which the chip would want, leaves 44.4 against the
     * family's 52 dp floor. Turn the sentence off and its two lines pay for the chip
     * twice over.
     */
    @Test
    fun `a tall card gives the chip a line only when the glyph can spare it`() {
        assertFalse(nowTallHasWarningRow(twoByTwo, 1f, stale = false, withSentence = true))
        assertTrue(nowTallHasWarningRow(twoByTwo, 1f, stale = false, withSentence = false))
        // Three rows: room for everything.
        assertTrue(nowTallHasWarningRow(DpSize(159.dp, 290.dp), 1f, false, withSentence = true))
    }

    @Test
    fun `a drawn chip costs the tall glyph exactly its own block`() {
        val without = nowTallIconRoom(DpSize(159.dp, 290.dp), 1f, false, withSentence = true, withWarning = false)
        val with = nowTallIconRoom(DpSize(159.dp, 290.dp), 1f, false, withSentence = true, withWarning = true)
        assertEquals(chipBlock.value, (without - with).value, 0.01f)
    }

    /** The tall card's number (23 set 2026): 34 sp until the glyph reaches its ceiling —
     * so the reference two-by-two does not move — then whatever the glyph cannot use, up
     * to the text card's 56, and never wider than the words' column holds. */
    @Test
    fun `the tall number grows only with the glyph's surplus`() {
        assertEquals(TemperatureSp, nowTallTemperatureSp(twoByTwo, 1f, false, true, false), 0.001f)
        assertEquals(TemperatureSp, nowTallTemperatureSp(DpSize(340.dp, 189.dp), 1f, true, true, true), 0.001f)
        val threeRows = listOf(DpSize(159.dp, 293.dp), DpSize(250.dp, 293.dp), DpSize(340.dp, 293.dp))
        threeRows.forEach { size ->
            val sp = nowTallTemperatureSp(size, 1f, false, true, false)
            assertTrue("$size: $sp", sp > TemperatureSp && sp <= TallTemperatureMax)
            // The glyph keeps its ceiling with the bigger number in the budget.
            assertEquals(
                HeroIconMax.value,
                heroIconSize(nowTallIconRoom(size, 1f, false, true, false, sp)).value,
                0.01f
            )
        }
        // The width guard: a narrow tall card never gets a number wider than its column.
        val narrow = DpSize(110.dp, 400.dp)
        val sp = nowTallTemperatureSp(narrow, 1f, false, true, false)
        assertTrue(sp * TempEmWidth <= (narrow.width - WidgetCardPadding * 2).value + 0.01f)
    }

    /** The narrow tall card lists the next hours under the two-by-two's hero (widget
     * review, 13 ott 2026): three on the reference two-by-three, six on the two-by-four,
     * none on the two-by-two itself, and fewer lines as the text grows. */
    @Test
    fun `a narrow tall card lists the next hours under its hero`() {
        assertEquals(0, nowTallHourRows(DpSize(159.dp, 189.dp), 1f, available = 24))
        assertEquals(3, nowTallHourRows(DpSize(159.dp, 293.dp), 1f, available = 24))
        assertEquals(6, nowTallHourRows(DpSize(159.dp, 397.dp), 1f, available = 24))
        assertEquals(2, nowTallHourRows(DpSize(159.dp, 293.dp), 2f, available = 24))
        // Never more than the strip carries, and one line alone is no list.
        assertEquals(4, nowTallHourRows(DpSize(159.dp, 397.dp), 1f, available = 4))
        assertEquals(0, nowTallHourRows(DpSize(159.dp, 397.dp), 1f, available = 1))
    }
}
