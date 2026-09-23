package com.callbackdev.chiaro.widget

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Today widget's budget for a given grant (committente, 8 set 2026, afternoon).
 *
 * Same reference sizes as the other two tables: a two-row card is ~189 dp tall, four
 * cells ~340 dp wide, and 250 is the provider's minimum width (three cells on the
 * reference grid). The strip's own heights: an hour line of 12 sp is 15.84 dp, the
 * glyph 32, a temperature line of 14 sp 18.48, a rain line of 11 sp 14.52, and 2 dp
 * either side of the glyph — 70.32 without rain, 84.84 with it.
 */
class TodayWidgetLayoutTest {

    private val fourByTwo = DpSize(340.dp, 189.dp)
    private val threeByTwo = DpSize(250.dp, 189.dp)

    @Test
    fun `the strip is as many cells as the width honestly holds`() {
        // (340 − 28 + 6) / 44 = 7.2 → 7; (250 − 28 + 6) / 44 = 5.2 → 5.
        assertEquals(7, todayStripCells(340.dp))
        assertEquals(5, todayStripCells(250.dp))
        // Fewer than four hours is no longer an afternoon; more than seven no longer fits.
        assertEquals(4, todayStripCells(180.dp))
        assertEquals(7, todayStripCells(600.dp))
    }

    /**
     * The hero row's two columns, the Now card's rule on this card's glyph (20 set 2026).
     * On the reference four-by-two the glyph is 76 dp, which leaves
     * 340 − 4 − 76 − 8 − 14 = 238 of words and 226 of slack once the gap is paid: 113 each
     * under the even split, which is the figure `todayIsWide` has always compared.
     */
    private val heroIcon = 76.dp

    @Test
    fun `the hero row shares its slack the way the Now card does`() {
        assertEquals(113f, heroRowEvenColumn(fourByTwo.width, heroIcon).value, 0.01f)
        // A long name beside a short sentence takes the room the sentence is not using.
        assertEquals(
            167f,
            todayWordsColumnWidth(fourByTwo, heroIcon, placeLine = 167.dp, trailingKeep = 47.dp).value,
            0.01f
        )
        // A trailing column that wants its half keeps its half, whatever the name wants:
        // on this card that tenant may be the day's high and low, which cannot wrap.
        assertEquals(
            113f,
            todayWordsColumnWidth(fourByTwo, heroIcon, placeLine = 167.dp, trailingKeep = 200.dp).value,
            0.01f
        )
        // And a short name changes nothing at all.
        assertEquals(
            113f,
            todayWordsColumnWidth(fourByTwo, heroIcon, placeLine = 68.dp, trailingKeep = 47.dp).value,
            0.01f
        )
    }

    @Test
    fun `a card too narrow for a sentence column is still the number and the place`() {
        // Three cells: 250 − 4 − 76 − 8 − 14 = 148, 68 each — under the 96 a sentence needs,
        // so the row carries no trailing column and nothing here is asked to divide it.
        assertEquals(68f, heroRowEvenColumn(threeByTwo.width, heroIcon).value, 0.01f)
        assertFalse(todayIsWide(threeByTwo, heroIcon))
        assertTrue(todayIsWide(fourByTwo, heroIcon))
    }

    @Test
    fun `the strip's height is its four lines, the rain line when drawn`() {
        assertEquals(70.32f, todayStripHeight(fontScale = 1f, rain = false).value, 0.05f)
        assertEquals(84.84f, todayStripHeight(fontScale = 1f, rain = true).value, 0.05f)
    }

    @Test
    fun `the hero's words are the taller column`() {
        // Leading: 34 × 1.32 + 16 × 1.32 + 34 × 0.24 = 74.16; trailing with the sentence:
        // 2 × 16 × 1.32 = 42.24. The leading side wins.
        assertEquals(
            74.16f,
            todayHeroTextHeight(fontScale = 1f, stale = false, sentence = true, range = false).value,
            0.05f
        )
        // A stale marker adds its 11 sp line to the leading side.
        assertEquals(
            88.68f,
            todayHeroTextHeight(fontScale = 1f, stale = true, sentence = true, range = false).value,
            0.05f
        )
        // Sentence and range together: 42.24 + 21.12 = 63.36, still under the leading side.
        assertEquals(
            74.16f,
            todayHeroTextHeight(fontScale = 1f, stale = false, sentence = true, range = true).value,
            0.05f
        )
    }

    @Test
    fun `the rain row fits on the reference card, and yields to a stale marker`() {
        // 189 − 6 − 14 − 74.16 − 8 = 86.84 ≥ 84.84.
        assertTrue(todayShowRain(fourByTwo, fontScale = 1f, stale = false, sentence = true, range = false))
        // With the marker the room is 72.32: the row stays home rather than being cut.
        assertFalse(todayShowRain(fourByTwo, fontScale = 1f, stale = true, sentence = true, range = false))
        // Room enough for everything on a taller card.
        assertTrue(todayShowRain(DpSize(340.dp, 290.dp), fontScale = 1f, stale = true, sentence = true, range = true))
    }

    @Test
    fun `the hero glyph is what the strip leaves`() {
        // 189 − 6 − 14 − 84.84 − 8 = 76.16 with the rain row; 90.68 without, which the
        // card's own ceiling takes down to 80 (23 set 2026: the glyph sits beside the words
        // and every dp it grows is a dp the sentence loses).
        assertEquals(76.16f, todayHeroIconSize(fourByTwo, fontScale = 1f, rain = true).value, 0.05f)
        assertEquals(80f, todayHeroIconSize(fourByTwo, fontScale = 1f, rain = false).value, 0.05f)
        // Floor and ceiling: a squeezed grant and a three-row card.
        assertEquals(52f, todayHeroIconSize(DpSize(340.dp, 150.dp), fontScale = 1f, rain = true).value, 0.01f)
        assertEquals(80f, todayHeroIconSize(DpSize(340.dp, 290.dp), fontScale = 1f, rain = true).value, 0.01f)
    }

    /** The days' row (23 set 2026): absent on the reference two-row cards whatever they
     * carry, present on every three-row card from the provider's three-cell minimum up,
     * and never at the cost of the rain line or the hero — it is paid last. */
    @Test
    fun `the days come with the third row and never before`() {
        val flags = listOf(false, true)
        flags.forEach { stale -> flags.forEach { range -> flags.forEach { rain ->
            assertFalse(
                "4x2 stale=$stale range=$range rain=$rain",
                todayShowDays(fourByTwo, 1f, stale, sentence = true, range = range, warning = false, rain = rain)
            )
            assertFalse(todayShowDays(threeByTwo, 1f, stale, true, range, false, rain))
            listOf(DpSize(250.dp, 293.dp), DpSize(340.dp, 293.dp)).forEach { size ->
                assertTrue(
                    "$size stale=$stale range=$range rain=$rain",
                    todayShowDays(size, 1f, stale, sentence = true, range = range, warning = true, rain = rain)
                )
            }
        } } }
        // And what it draws fits: hero, gap, strip with its rain line, gap, days.
        val size = DpSize(340.dp, 293.dp)
        val used = WidgetCardPaddingSnug + WidgetCardPadding +
            maxOf(todayHeroIconSize(size, 1f, true), todayHeroTextHeight(1f, true, true, true, true)) +
            StripGap + todayStripHeight(1f, true) + DaysGap + todayDaysHeight(1f)
        assertTrue("${used.value}", used.value <= size.height.value)
    }

    @Test
    fun `four cells carry the sentence column, three do not`() {
        // (340 − 4 − 76 − 8 − 14 − 12) / 2 = 113 ≥ 96.
        assertTrue(todayIsWide(fourByTwo, icon = 76.dp))
        // (250 − 4 − 76 − 8 − 14 − 12) / 2 = 68.
        assertFalse(todayIsWide(threeByTwo, icon = 76.dp))
    }

    // ------------------------------------------------- the warning chip (Fase 11)

    /**
     * On the reference four-by-two the chip is FREE: the hero's leading column (the
     * 34 sp number, the place and the leading band, 74.16 dp) is taller than its
     * trailing one even once the chip has joined it (42.24 + 24.52 = 66.76), so the row
     * does not grow and nothing else on the card moves.
     */
    @Test
    fun `the chip is free where the number's column is the taller one`() {
        val without = todayHeroTextHeight(1f, stale = false, sentence = true, range = false)
        val with = todayHeroTextHeight(1f, stale = false, sentence = true, range = false, warning = true)
        assertEquals(74.16f, without.value, 0.01f)
        assertEquals(without.value, with.value, 0.01f)
    }

    /** With the day's range as well the trailing column wins, and the row grows to
     * 87.88 — which the card still holds, without the rain line. */
    @Test
    fun `with the range too the row grows and the strip still fits`() {
        assertEquals(
            87.88f,
            todayHeroTextHeight(1f, stale = false, sentence = true, range = true, warning = true).value,
            0.01f
        )
        assertTrue(todayHasWarningRow(fourByTwo, 1f, stale = false, sentence = true, range = true))
    }

    /** The rain row is the one that yields: it is the last thing the budget pays for,
     * and a section that does not fit is not drawn. */
    @Test
    fun `the rain row yields to the chip before the strip does`() {
        assertTrue(todayShowRain(fourByTwo, 1f, stale = false, sentence = true, range = false, warning = true))
        assertTrue(todayShowRain(fourByTwo, 1f, stale = false, sentence = true, range = true, warning = false))
        assertFalse(todayShowRain(fourByTwo, 1f, stale = false, sentence = true, range = true, warning = true))
    }

    /** A card too short for the strip without its rain line has no line to spare. */
    @Test
    fun `a card with no room left carries no chip`() {
        assertFalse(
            todayHasWarningRow(DpSize(340.dp, 150.dp), 1f, stale = true, sentence = true, range = true)
        )
    }
}
