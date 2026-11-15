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
        // A label wider than the floor takes a cell from the strip rather than being cut:
        // «13:00» at 13 sp is 33 dp, and at a large font size the cell it needs grows.
        assertEquals(7, todayStripCells(340.dp, stripCellFor(33.dp)))
        assertEquals(6, todayStripCells(340.dp, stripCellFor(43.dp)))
    }

    /** The hero row is the four-by-one's (15 nov 2026), and so is its gate: four cells
     * have the far edge, three are the place and the number. */
    @Test
    fun `a card too narrow for a sentence column is still the number and the place`() {
        assertTrue(todayIsWide(fourByTwo.width))
        assertFalse(todayIsWide(threeByTwo.width))
        assertEquals(nowLayout(DpSize(340.dp, 85.dp)) == NowLayout.WIDE, todayIsWide(340.dp))
    }

    @Test
    fun `the strip's height is its four lines, the rain line when drawn`() {
        assertEquals(70.32f, todayStripHeight(fontScale = 1f, rain = false).value, 0.05f)
        assertEquals(84.84f, todayStripHeight(fontScale = 1f, rain = true).value, 0.05f)
    }

    @Test
    fun `the hero's words are the taller column`() {
        // Leading: the place's 16 × 1.32 over the kept 34 sp number's 34 × 1.32 = 66.0;
        // trailing with the sentence: 2 × 16 × 1.32 = 42.24. The leading side wins. The
        // glyph stands on the number's line and adds nothing.
        assertEquals(
            66.0f,
            todayHeroTextHeight(fontScale = 1f, stale = false, sentence = true, range = false).value,
            0.05f
        )
        // A stale marker adds its 11 sp line to the leading side.
        assertEquals(
            80.52f,
            todayHeroTextHeight(fontScale = 1f, stale = true, sentence = true, range = false).value,
            0.05f
        )
        // Sentence and range together: 42.24 + 21.12 = 63.36, still under the leading side.
        assertEquals(
            66.0f,
            todayHeroTextHeight(fontScale = 1f, stale = false, sentence = true, range = true).value,
            0.05f
        )
    }

    @Test
    fun `the rain row fits on the reference card, and yields to a stale marker`() {
        // 189 − 6 − 14 − 66 − 8 = 95 ≥ 84.84.
        assertTrue(todayShowRain(fourByTwo, fontScale = 1f, stale = false, sentence = true, range = false))
        // With the marker the room is 80.48: the row stays home rather than being cut.
        assertFalse(todayShowRain(fourByTwo, fontScale = 1f, stale = true, sentence = true, range = false))
        // Room enough for everything on a taller card.
        assertTrue(todayShowRain(DpSize(340.dp, 290.dp), fontScale = 1f, stale = true, sentence = true, range = true))
    }

    /** The band the hero lays itself out in: what the strip leaves, up to the reference
     * four-by-one's 73 dp — so from the four-by-two up the hero IS the four-by-one's. */
    @Test
    fun `the hero band is what the strip leaves, up to the one-row card's`() {
        // 189 − 6 − 14 − 84.84 − 8 = 76.16 with the rain row, 90.68 without: both capped.
        assertEquals(73f, todayHeroBand(fourByTwo, fontScale = 1f, rain = true).value, 0.05f)
        assertEquals(73f, todayHeroBand(fourByTwo, fontScale = 1f, rain = false).value, 0.05f)
        assertEquals(73f, todayHeroBand(DpSize(340.dp, 397.dp), fontScale = 1f, rain = true).value, 0.05f)
        // A squeezed grant gives the hero less, and the hero yields: 175 − 20 − 84.84 − 8.
        assertEquals(62.16f, todayHeroBand(DpSize(340.dp, 175.dp), fontScale = 1f, rain = true).value, 0.05f)
        // The hero row at the band is the reference one-row card's, number and glyph.
        val hero = todayHeroSize(fourByTwo, 1f, rain = true)
        assertEquals(85f, hero.height.value, 0.05f)
        assertEquals(39.30f, nowRowHeroSp(hero, 1f, stale = false), 0.01f)
        assertEquals(51.88f, nowRowGlyphSize(hero, 1f, stale = false).value, 0.05f)
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
            maxOf(todayHeroBand(size, 1f, true), todayHeroTextHeight(1f, true, true, true, true)) +
            StripGap + todayStripHeight(1f, true) + DaysGap + todayDaysHeight(1f)
        assertTrue("${used.value}", used.value <= size.height.value)
    }

    // ------------------------------------------------- the warning chip (Fase 11)

    /**
     * On the reference four-by-two the chip costs the row almost nothing: the hero's
     * leading column (the place over the 34 sp number, 66 dp) is nearly as tall as the
     * trailing one once the chip has joined the sentence's two lines.
     */
    @Test
    fun `the chip costs the row only what its column outgrows the number's`() {
        val without = todayHeroTextHeight(1f, stale = false, sentence = true, range = false)
        val with = todayHeroTextHeight(1f, stale = false, sentence = true, range = false, warning = true)
        assertEquals(66.0f, without.value, 0.01f)
        assertEquals(42.24f + warningBlock(1f, drawn = true).value, with.value, 0.01f)
    }

    /** With the day's range as well the trailing column wins, and the row grows — which
     * the card still holds, without the rain line. */
    @Test
    fun `with the range too the row grows and the strip still fits`() {
        assertEquals(
            42.24f + 21.12f + warningBlock(1f, drawn = true).value,
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

    /** The second row of hours (widget review, 13 ott 2026): on the four-by-four, under
     * everything else and with its rain line; never on the four-by-three. */
    @Test
    fun `only the tallest card carries a second row of hours`() {
        fun more(h: Int) = todayShowMoreHours(
            DpSize(340.dp, h.dp), 1f, stale = false, sentence = true, range = false,
            warning = false, rain = true
        )
        assertTrue(more(397))
        assertFalse(more(293))
        assertFalse(
            todayShowMoreHours(
                DpSize(340.dp, 397.dp), 2f, stale = false, sentence = true, range = false,
                warning = false, rain = true
            )
        )
    }
}
