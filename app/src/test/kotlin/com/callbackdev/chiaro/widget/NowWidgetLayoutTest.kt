package com.callbackdev.chiaro.widget

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
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
 */
class NowWidgetLayoutTest {

    private val twoByOne = DpSize(159.dp, 82.dp)
    private val threeByOne = DpSize(250.dp, 82.dp)
    private val fourByOne = DpSize(340.dp, 82.dp)
    private val twoByTwo = DpSize(159.dp, 189.dp)

    @Test
    fun `one row at two or three cells is the narrow form`() {
        assertEquals(NowLayout.NARROW, nowLayout(twoByOne))
        assertEquals(NowLayout.NARROW, nowLayout(threeByOne))
        // A taller row grows the glyph, which takes width from the words; still narrow.
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
    fun `the sentence column is the row's slack split in two`() {
        // 340 − 4 (glyph edge) − 70 (glyph) − 8 (gap) − 14 (words' edge) − 12 (sentence
        // gap) = 232, half of which is 116 for each text column.
        assertEquals(116f, nowSentenceColumnWidth(fourByOne).value, 0.01f)
        // At three cells the same arithmetic leaves 71: under the 96 a sentence needs.
        assertEquals(71f, nowSentenceColumnWidth(threeByOne).value, 0.01f)
    }

    @Test
    fun `the one-row glyph fills the height, unless the words would lose their minimum`() {
        // 82 − 6 − 6 = 70: height-bound at three and four cells.
        assertEquals(70f, nowRowIconSize(threeByOne).value, 0.01f)
        assertEquals(70f, nowRowIconSize(fourByOne).value, 0.01f)
        // At two cells the width binds — 159 − 4 − 8 − 14 − 84 = 49 — and the floor
        // wins over it: 56 is where the Meteocons art stops reading.
        assertEquals(56f, nowRowIconSize(twoByOne).value, 0.01f)
        // A wider two-cell grid (178 dp) lands between the two: 178 − 110 = 68.
        assertEquals(68f, nowRowIconSize(DpSize(178.dp, 82.dp)).value, 0.01f)
        // The ceiling holds for a tall one-row grant.
        assertEquals(104f, nowRowIconSize(DpSize(340.dp, 130.dp)).value, 0.01f)
    }

    @Test
    fun `the sentence takes the lines the row holds, up to three`() {
        // (82 − 12) / (16 × 1.32) = 3.31 → 3 at the default font size…
        assertEquals(3, nowSentenceLines(fourByOne, fontScale = 1f))
        // …and 2 at the largest system font: (70) / (16 × 1.32 × 1.3) = 2.55.
        assertEquals(2, nowSentenceLines(fourByOne, fontScale = 1.3f))
        assertEquals(2, nowSentenceLines(DpSize(340.dp, 60.dp), fontScale = 1f))
        assertEquals(1, nowSentenceLines(DpSize(340.dp, 40.dp), fontScale = 1f))
        // The cap is the sentence's: a tall row does not buy a fourth line.
        assertEquals(3, nowSentenceLines(DpSize(340.dp, 101.dp), fontScale = 1f))
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

    @Test
    fun `the other way round, the sentence gets the row less number, glyph, gaps and insets`() {
        // 340 − 14 (words' edge) − 66 (number) − 12 (gap) − 8 (gap) − 70 (glyph) − 4
        // (glyph's edge) = 166: room for two lines of 16 sp beside the number.
        assertEquals(166f, nowMirroredSentenceWidth(fourByOne).value, 0.01f)
        // Four cells on a five-column grid still qualify (146 ≥ 96)…
        assertEquals(146f, nowMirroredSentenceWidth(DpSize(320.dp, 82.dp)).value, 0.01f)
        // …and three cells do not (76): the mirrored card shows the number and the
        // place alone, exactly as the standard one does at that width.
        assertEquals(76f, nowMirroredSentenceWidth(threeByOne).value, 0.01f)
    }
}
