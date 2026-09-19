package com.callbackdev.chiaro.widget

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The text widget's forms, its type scale and the order its budget spends in (committente,
 * 19 set 2026).
 *
 * The card has nothing drawn on it, so there is no screenshot that says whether it is
 * right — only the arithmetic, which is exactly why the arithmetic lives in
 * `TextWidgetLayout.kt` and is pinned here. The fixtures are the household's own reference
 * sizes, the ones `NowWidgetLayoutTest` reads off the reference device: a one-row card is
 * ~85 dp tall (~101 on the other launcher seen to grant one), two cells are ~159 dp wide,
 * three ~250, four ~340, and a four-by-two is ~340 × 189.
 */
class TextWidgetLayoutTest {

    private val twoByOne = DpSize(159.dp, 85.dp)
    private val threeByOne = DpSize(250.dp, 85.dp)
    private val fourByOne = DpSize(340.dp, 85.dp)
    private val tallRow = DpSize(340.dp, 101.dp)
    private val fourByTwo = DpSize(340.dp, 189.dp)
    private val twoByTwo = DpSize(159.dp, 189.dp)
    private val fourByThree = DpSize(340.dp, 293.dp)

    @Test
    fun `three cells already carry the sentence, two do not`() {
        assertEquals(TextForm.LINE, textForm(twoByOne))
        assertEquals(TextForm.ROW, textForm(threeByOne))
        assertEquals(TextForm.ROW, textForm(fourByOne))
        assertEquals(TextForm.ROW, textForm(tallRow))
    }

    /** The one place this card beats the Now widget on the same grant, and the reason is
     * arithmetic: the 66 dp of glyph and its 8 dp gap that card spends before its first
     * letter, this one does not have. */
    @Test
    fun `the Now widget's narrow card is this card's wide one`() {
        assertEquals(NowLayout.NARROW, nowLayout(DpSize(250.dp, 82.dp)))
        assertEquals(TextForm.ROW, textForm(threeByOne))
    }

    @Test
    fun `two rows are the stack whatever the width`() {
        assertEquals(TextForm.STACK, textForm(twoByTwo))
        assertEquals(TextForm.STACK, textForm(fourByTwo))
        assertEquals(TextForm.STACK, textForm(DpSize(340.dp, 150.dp)))
        assertEquals(TextForm.ROW, textForm(DpSize(340.dp, 149.dp)))
    }

    @Test
    fun `the leading column is a share of the slack, with a floor under it`() {
        // 340 − 14 − 14 − 12 = 300, and 42% of that is 126 for the number and its place.
        assertEquals(126f, textLeadingColumn(fourByOne).value, 0.01f)
        assertEquals(174f, textSentenceColumn(fourByOne).value, 0.01f)
        // Three cells: the share would be 88.2, so the floor carries it and the sentence
        // still clears its own minimum of 104.
        assertEquals(TextWordsMin.value, textLeadingColumn(threeByOne).value, 0.01f)
        assertEquals(114f, textSentenceColumn(threeByOne).value, 0.01f)
        // Two cells: the floor again, and 23 dp left over, which is no column at all.
        assertEquals(TextWordsMin.value, textLeadingColumn(twoByOne).value, 0.01f)
        assertEquals(23f, textSentenceColumn(twoByOne).value, 0.01f)
    }

    @Test
    fun `the number fills the row the way the other cards' glyph fills it`() {
        // 85 − 6 − 6 − 18.48 (the place's line) = 54.52 dp of line box, which is 41.3 sp.
        assertEquals(41.303f, textRowHeroSp(fourByOne, 1f, stale = false), 0.01f)
        // A stale marker is never dropped, so it is paid for out of the number: 40 dp
        // left, 30.3 sp, which is where the floor sits.
        assertEquals(30.303f, textRowHeroSp(fourByOne, 1f, stale = true), 0.01f)
        assertTrue(textRowHeroSp(fourByOne, 1f, stale = true) >= TextHeroFloor)
        // The taller row the other launcher grants would print 53 sp without the cap.
        assertEquals(TextHeroRowMax, textRowHeroSp(tallRow, 1f, stale = false), 0.01f)
        // A reader's larger font scale grows the lines around it, so the number yields.
        assertTrue(
            textRowHeroSp(fourByOne, 1.3f, stale = false) <
                textRowHeroSp(fourByOne, 1f, stale = false)
        )
    }

    /** A card too narrow to hold the number it computed would clip it, so the width has a
     * guard of its own. On a card with two columns the leading one's floor (96 dp, good for
     * 45 sp) already clears the row's ceiling, so the guard bites only where there is one
     * column: the narrow one-row form and a one-cell-wide tall card. */
    @Test
    fun `the number is capped by the column it really has`() {
        // Four cells: 126 dp of leading column, and the height is what binds.
        assertEquals(41.303f, textRowHeroSp(fourByOne, 1f, stale = false), 0.01f)
        // One cell wide, one row: no second column, so the whole 110 − 28 = 82 dp is the
        // measure, and 82 / 2.1 = 39 sp — where the height alone would have said 41.3.
        assertEquals(TextForm.LINE, textForm(DpSize(110.dp, 85.dp)))
        assertEquals(39.05f, textRowHeroSp(DpSize(110.dp, 85.dp), 1f, stale = false), 0.01f)
        // One cell wide and two tall: the same 82 dp, against a budget that would
        // otherwise have taken the number to its ceiling.
        val narrowStack = textStackPlan(
            DpSize(110.dp, 189.dp), 1f,
            stale = false, sentence = true, warning = false, range = false, hours = true
        )
        assertEquals(39.05f, narrowStack.heroSp, 0.01f)
    }

    @Test
    fun `the trailing column spends on the sentence first and grows it last`() {
        // Nothing else to say: three lines of prose on the reference four-cell row.
        assertEquals(
            TextRowPlan(sentenceLines = 3, showWarning = false, showRange = false),
            textRowPlan(fourByOne, 1f, sentence = true, warning = false, range = false)
        )
        // A warning takes one of those lines, never the first.
        assertEquals(
            TextRowPlan(sentenceLines = 2, showWarning = true, showRange = false),
            textRowPlan(fourByOne, 1f, sentence = true, warning = true, range = false)
        )
        // Both facts, and the sentence is down to the line it is guaranteed.
        assertEquals(
            TextRowPlan(sentenceLines = 1, showWarning = true, showRange = true),
            textRowPlan(fourByOne, 1f, sentence = true, warning = true, range = true)
        )
        // With the sentence switched off the column is facts alone, and nothing pads it.
        assertEquals(
            TextRowPlan(sentenceLines = 0, showWarning = true, showRange = true),
            textRowPlan(fourByOne, 1f, sentence = false, warning = true, range = true)
        )
    }

    @Test
    fun `a one-row card has a line for the warning, a squeezed one does not`() {
        assertTrue(textRowHasFactLine(fourByOne, 1f, sentence = true))
        assertTrue(textRowHasFactLine(threeByOne, 1f, sentence = true))
        // 50 dp of row: one line of sentence and nothing after it. A section that does
        // not fit is not drawn.
        assertFalse(textRowHasFactLine(DpSize(340.dp, 50.dp), 1f, sentence = true))
    }

    @Test
    fun `a tall card pays the number first and the hours last`() {
        val plan = textStackPlan(
            fourByTwo, 1f,
            stale = false, sentence = true, warning = false, range = true, hours = true
        )
        // 189 − 28 − 18.48 (place) = 142.52; the number reserves 52.8, two lines of
        // sentence take 44.88 and the range 18.48, which leaves 26.36 — under the 46.32
        // the hours block and its air need, so the hours stay home and the leftover goes
        // back to the number, which hits its ceiling.
        assertEquals(TextHeroMax, plan.heroSp, 0.01f)
        assertEquals(2, plan.sentenceLines)
        assertTrue(plan.showRange)
        assertFalse(plan.showHours)

        // Three rows buy the hours, and the number is still at its ceiling.
        val taller = textStackPlan(
            fourByThree, 1f,
            stale = false, sentence = true, warning = false, range = true, hours = true
        )
        assertEquals(TextHeroMax, taller.heroSp, 0.01f)
        assertTrue(taller.showHours)

        // Turn the sentence off on the four-by-two and its two lines pay for the hours.
        val bare = textStackPlan(
            fourByTwo, 1f,
            stale = false, sentence = false, warning = false, range = false, hours = true
        )
        assertEquals(0, bare.sentenceLines)
        assertTrue(bare.showHours)
    }

    @Test
    fun `on a tall card the warning outranks the day's range`() {
        val plan = textStackPlan(
            fourByTwo, 1f,
            stale = true, sentence = true, warning = true, range = true, hours = true
        )
        assertTrue(plan.showWarning)
        assertFalse(plan.showRange)
        assertFalse(plan.showHours)
        // The stale marker and the warning both cost the number, and it stays well over
        // its floor: 48.97 sp.
        assertEquals(48.97f, plan.heroSp, 0.01f)
    }

    @Test
    fun `the tall card asks for the warning's line the way the one-row card does`() {
        assertTrue(textStackHasFactLine(fourByTwo, 1f, stale = false, sentence = true))
        assertTrue(textStackHasFactLine(twoByTwo, 1f, stale = false, sentence = true))
    }

    /** Never three: the Now widget's tall card made the same call for the same reason,
     * and here the third line is worth 13 sp of hero on the reference four-by-two. */
    @Test
    fun `the tall card's sentence stops at two lines`() {
        listOf(fourByTwo, fourByThree, DpSize(340.dp, 397.dp)).forEach { size ->
            val plan = textStackPlan(
                size, 1f,
                stale = false, sentence = true, warning = false, range = true, hours = true
            )
            assertEquals(TextStackSentenceMaxLines, plan.sentenceLines)
        }
    }

    @Test
    fun `the hours strip takes what the width honestly holds`() {
        assertEquals(TextHourCellsFloor, textHourCells(159.dp))
        assertEquals(5, textHourCells(250.dp))
        assertEquals(TextHourCellsCeiling, textHourCells(340.dp))
        // Glance drops the eleventh child of a container without a word, so the ceiling
        // holds however wide the grant gets.
        assertEquals(TextHourCellsCeiling, textHourCells(720.dp))
    }

    /** The inverse the whole file rests on: a size asked for a line box, and the line box
     * asked back for the size. */
    @Test
    fun `the line box inverts`() {
        listOf(11f, 14f, 17f, 30f, 56f).forEach { sp ->
            listOf(1f, 1.3f, 0.85f).forEach { scale ->
                assertEquals(sp, textSizeForLine(textLineHeight(sp, scale), scale), 0.001f)
            }
        }
        assertEquals(0f, textSizeForLine((-10).dp, 1f), 0.001f)
    }

    /** The four ranks are separated by size as well as by weight and ink, and in that
     * order: a rank that only differed by one of the three would not sort. */
    @Test
    fun `the ranks are ordered`() {
        assertTrue(TextHeroFloor > TextSentenceSp)
        assertTrue(TextSentenceSp > TextFactSp)
        assertTrue(TextFactSp > TextStaleSp)
        // The hours' own figures are rank 3 too, and not a fifth size.
        assertEquals(TextFactSp, TextHourTempSp, 0.001f)
        assertTrue(TextHeroStackFloor in TextHeroFloor..TextHeroMax)
    }
}
