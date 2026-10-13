package com.callbackdev.chiaro.widget

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Which form the Sky widget takes for a given grant, how big its glyph gets, and how
 * many further moments a tall card holds (committente, 8 set 2026, evening).
 *
 * Same reference sizes as `NowWidgetLayoutTest`: a one-row card is ~82 dp tall, three
 * cells ~250 dp wide, four ~340, two rows ~189; 101 dp is the other row height the
 * launcher has been seen to grant, 320 is four cells on a five-column grid. The form is
 * arithmetic on dp precisely so it can be pinned here rather than by one phone's idea of
 * a cell.
 */
class SkyWidgetLayoutTest {

    private val threeByOne = DpSize(250.dp, 82.dp)
    private val fourByOne = DpSize(340.dp, 82.dp)
    private val threeByTwo = DpSize(250.dp, 189.dp)
    private val fourByTwo = DpSize(340.dp, 189.dp)

    @Test
    fun `two rows are tall, one row is not`() {
        assertFalse(skyIsTall(fourByOne))
        assertFalse(skyIsTall(DpSize(340.dp, 149.dp)))
        assertTrue(skyIsTall(DpSize(340.dp, 150.dp)))
        assertTrue(skyIsTall(fourByTwo))
    }

    @Test
    fun `the one-row glyph fills the height, capped at 72`() {
        // 82 − 6 − 6 = 70, and the width guard (250 − 4 − 8 − 14 − 84 = 140) does not bind.
        assertEquals(70f, skyHeroIconSize(threeByOne).value, 0.01f)
        assertEquals(70f, skyHeroIconSize(fourByOne).value, 0.01f)
        // A 101 dp row would give 89; the Sky glyph stops at 72 so the words keep the
        // column that decides the wide form.
        assertEquals(72f, skyHeroIconSize(DpSize(340.dp, 101.dp)).value, 0.01f)
        // A tall card draws the fixed 60: the list under it is what the height is for.
        assertEquals(60f, skyHeroIconSize(fourByTwo).value, 0.01f)
    }

    @Test
    fun `the words' column is the row less glyph, gaps, inset and verdict block`() {
        // 340 − 4 − 70 − 8 − 12 − 14 − 124 = 108 (the reference's verdict block: «Presto
        // per dirlo» with its mark, 13 ott 2026).
        assertEquals(108f, skyWordsColumnWidth(fourByOne).value, 0.01f)
        // On a tall card the glyph is 60, so the same width leaves 118.
        assertEquals(118f, skyWordsColumnWidth(fourByTwo).value, 0.01f)
        // Three cells leave 18 (250 − 4 − 70 − 8 − 12 − 14 − 124): no room for a clock
        // beside a chip.
        assertEquals(18f, skyWordsColumnWidth(threeByOne).value, 0.01f)
    }

    @Test
    fun `four cells are wide, three are not, whatever the height`() {
        assertTrue(skyIsWide(fourByOne))
        assertTrue(skyIsWide(fourByTwo))
        // The 72 dp cap is what keeps a four-cell card wide on a 101 dp launcher:
        // 340 − 4 − 72 − 8 − 12 − 14 − 124 = 106 ≥ 84.
        assertTrue(skyIsWide(DpSize(340.dp, 101.dp)))
        assertFalse(skyIsWide(threeByOne))
        assertFalse(skyIsWide(threeByTwo))
        // Four cells on a five-column grid (320) leave 88: «19:55» (80 with its slack) fits...
        assertTrue(skyIsWide(DpSize(320.dp, 82.dp)))
        // ...and a twelve-hour clock does not: English's widest chip, «Not sure yet» with
        // its mark (102), leaves 110 under the 116 «7:55 PM» needs, so that card is narrow.
        assertFalse(skyIsWide(DpSize(320.dp, 82.dp), verdictColumn = 102.dp, wordsMin = 116.dp))
        assertTrue(skyIsWide(fourByOne, verdictColumn = 102.dp, wordsMin = 116.dp))
    }

    @Test
    fun `a two-row card holds three further moments`() {
        // Hero: 30 × 1.32 (clock) + 22 (the mark, taller than the 15 sp name line) +
        // 30 × 0.24 (leading band) = 68.8, over the 60 dp glyph. Room: 189 − 6 − 14 −
        // 68.8 − 10 = 90.2; rows of 24 with 6 between, first gap forgiven: 96.2 / 30 = 3.
        assertEquals(3, skyRows(fourByTwo, fontScale = 1f, available = 5))
        assertEquals(3, skyRows(threeByTwo, fontScale = 1f, available = 5))
        // The shorter two-row grant the launcher has also been seen to make: 90.2 / 30.
        assertEquals(3, skyRows(DpSize(340.dp, 183.dp), fontScale = 1f, available = 5))
    }

    @Test
    fun `the list is never padded, and a one-row card has no list`() {
        assertEquals(2, skyRows(fourByTwo, fontScale = 1f, available = 2))
        assertEquals(0, skyRows(fourByTwo, fontScale = 1f, available = 0))
        assertEquals(0, skyRows(fourByOne, fontScale = 1f, available = 5))
        assertEquals(0, skyRows(DpSize(340.dp, 149.dp), fontScale = 1f, available = 5))
    }

    @Test
    fun `the first tall height holds one row, and a large font costs one`() {
        // 150 − 20 − 68.8 − 10 = 51.2; (51.2 + 6) / 30 = 1.9 → 1.
        assertEquals(1, skyRows(DpSize(340.dp, 150.dp), fontScale = 1f, available = 5))
        // At 1.3× the hero's words grow to 86.6 and a 13 sp line to 22.3 (still under
        // the 24 dp row): 189 − 20 − 86.6 − 10 = 72.4; 78.4 / 30 = 2.6 → 2.
        assertEquals(2, skyRows(fourByTwo, fontScale = 1.3f, available = 5))
    }
}
