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
        // 189 − 6 − 14 − 84.84 − 8 = 76.16 with the rain row; 90.68 without.
        assertEquals(76.16f, todayHeroIconSize(fourByTwo, fontScale = 1f, rain = true).value, 0.05f)
        assertEquals(90.68f, todayHeroIconSize(fourByTwo, fontScale = 1f, rain = false).value, 0.05f)
        // Floor and ceiling: a squeezed grant and a three-row card.
        assertEquals(52f, todayHeroIconSize(DpSize(340.dp, 150.dp), fontScale = 1f, rain = true).value, 0.01f)
        assertEquals(104f, todayHeroIconSize(DpSize(340.dp, 290.dp), fontScale = 1f, rain = true).value, 0.01f)
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
