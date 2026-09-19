package com.callbackdev.chiaro.widget

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The text widget's forms, its type scale and the order its budgets spend in (committente,
 * 19 set 2026, over two device passes).
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
    private val threeByTwo = DpSize(250.dp, 189.dp)
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
    fun `two rows are the panel from four cells and the stack under it`() {
        assertEquals(TextForm.PANEL, textForm(fourByTwo))
        assertEquals(TextForm.PANEL, textForm(fourByThree))
        // Four cells on a five-column grid still qualifies; three cells do not.
        assertEquals(TextForm.PANEL, textForm(DpSize(320.dp, 189.dp)))
        assertEquals(TextForm.STACK, textForm(threeByTwo))
        assertEquals(TextForm.STACK, textForm(twoByTwo))
        // The height threshold is the household's, and one dp under it is still a row.
        assertEquals(TextForm.PANEL, textForm(DpSize(340.dp, 150.dp)))
        assertEquals(TextForm.ROW, textForm(DpSize(340.dp, 149.dp)))
    }

    /**
     * The leading column is not a share of the row: it is what a long place name needs,
     * bounded below by [TextWordsMin] and above by the sentence's own minimum. The device
     * pass that asked for this printed «Cavenago di Bri…» in a 126 dp column while the
     * other column held 174 dp of white space.
     */
    @Test
    fun `the leading column is what the place name needs, not a share of the row`() {
        // 340 − 14 − 14 − 12 = 300 of words; the sentence keeps 104 and the name takes
        // what it wants, which is 168.
        assertEquals(TextPlaceColumnIdeal.value, textLeadingColumn(fourByOne).value, 0.01f)
        assertEquals(132f, textSentenceColumn(fourByOne).value, 0.01f)
        // «⌖ Cavenago di Brianza» is 145 dp of name plus the pin's box and its air.
        assertTrue(textLeadingColumn(fourByOne) >= 145.dp + TextFactSp.dp + 4.dp)
        // Three cells: 210 of words, so the name gets 106 and the sentence lands exactly
        // on its minimum — the form this card gains at three cells is not given back.
        assertEquals(106f, textLeadingColumn(threeByOne).value, 0.01f)
        assertEquals(TextSentenceColumnMin.value, textSentenceColumn(threeByOne).value, 0.01f)
        assertEquals(TextForm.ROW, textForm(threeByOne))
        // Two cells: the floor, and 23 dp left over, which is no column at all.
        assertEquals(TextWordsMin.value, textLeadingColumn(twoByOne).value, 0.01f)
        assertEquals(23f, textSentenceColumn(twoByOne).value, 0.01f)
    }

    @Test
    fun `the number fills the row the way the other cards' glyph fills it`() {
        // 85 − 6 − 6 − 21.12 (the place's line at 16 sp) = 51.88 dp of line box, 39.3 sp.
        assertEquals(39.303f, textRowHeroSp(fourByOne, 1f, stale = false), 0.01f)
        // A stale marker is never dropped, so it is paid for out of the number: 37.36 dp
        // left, 28.3 sp — over the floor, which is the whole reason the floor moved to 26
        // when rank 3 grew.
        assertEquals(28.303f, textRowHeroSp(fourByOne, 1f, stale = true), 0.01f)
        assertTrue(textRowHeroSp(fourByOne, 1f, stale = true) > TextHeroFloor)
        // The taller row the other launcher grants would print 51 sp without the cap.
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
        assertEquals(39.303f, textRowHeroSp(fourByOne, 1f, stale = false), 0.01f)
        // One cell wide, one row: no second column, so the whole 110 − 28 = 82 dp is the
        // measure, and 82 / 2.1 = 39.05 sp — just under what the height would have given.
        assertEquals(TextForm.LINE, textForm(DpSize(110.dp, 85.dp)))
        assertEquals(39.048f, textRowHeroSp(DpSize(110.dp, 85.dp), 1f, stale = false), 0.01f)
        // One cell wide and two tall: the same 82 dp, against a budget that would
        // otherwise have taken the number to its ceiling.
        val narrowStack = textStackPlan(
            DpSize(110.dp, 189.dp), 1f,
            stale = false, sentence = true, warning = false, range = false
        )
        assertEquals(39.048f, narrowStack.heroSp, 0.01f)
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

    /**
     * The panel's number stands beside the words, not under them, so it may be as tall as
     * the block beside it — two lines of sentence, the warning and the range is 89.8 dp
     * against the 84.5 a 64 sp line box occupies.
     */
    @Test
    fun `the panel's number is as tall as the column beside it`() {
        val plan = textPanelPlan(
            fourByTwo, 1f,
            stale = false, sentence = true, warning = false, range = true
        )
        assertEquals(TextHeroPanelMax, plan.heroSp, 0.01f)
        assertEquals(2, plan.sentenceLines)
        assertTrue(plan.showRange)
        // The block it must not outgrow, at its fullest.
        val block = textLineHeight(TextSentenceSp, 1f) * TextPanelSentenceMaxLines +
            textLineHeight(TextFactSp, 1f) * 2
        assertTrue(textLineHeight(TextHeroPanelMax, 1f) <= block)
        // And it is bigger than anything a one-row card can print, which is the point.
        assertTrue(plan.heroSp > TextHeroRowMax)
    }

    /** Neither of the panel's columns can overflow: the left is paid out of the height
     * without the place and the stale marker, the right without the place. */
    @Test
    fun `the panel fits at every grant that is one`() {
        listOf(
            DpSize(300.dp, 150.dp), DpSize(320.dp, 189.dp), fourByTwo, fourByThree,
            DpSize(520.dp, 400.dp)
        ).forEach { size ->
            listOf(false, true).forEach { stale ->
                listOf(1f, 1.3f).forEach { scale ->
                    val plan = textPanelPlan(
                        size, scale, stale = stale,
                        sentence = true, warning = true, range = true
                    )
                    val place = textLineHeight(TextFactSp, scale)
                    val left = textLineHeight(plan.heroSp, scale) +
                        (if (stale) textLineHeight(TextStaleSp, scale) else 0.dp)
                    val right = textLineHeight(TextSentenceSp, scale) * plan.sentenceLines +
                        (if (plan.showWarning) textLineHeight(TextFactSp, scale) else 0.dp) +
                        (if (plan.showRange) textLineHeight(TextFactSp, scale) else 0.dp)
                    val used = place + maxOf(left, right)
                    assertTrue(
                        "$size at scale $scale (stale=$stale) needs ${used.value} of " +
                            "${(size.height - WidgetCardPadding * 2).value}",
                        used <= size.height - WidgetCardPadding * 2
                    )
                }
            }
        }
    }

    /** The panel's leading column is fixed, because what stands in it is one figure: it
     * holds «−12°» at the ceiling with a couple of dp to spare, and the rest goes to the
     * sentence. */
    @Test
    fun `the panel splits its width for the figure, not for a name`() {
        assertTrue(TextPanelLeading.value >= TextHeroPanelMax * 2.1f)
        assertEquals(160f, textPanelSentenceColumn(fourByTwo).value, 0.01f)
        // The narrowest card that is a panel at all still clears the sentence's minimum.
        assertTrue(textPanelSentenceColumn(DpSize(TextPanelMinWidth, 189.dp)) >= TextSentenceColumnMin)
    }

    @Test
    fun `a narrow tall card pays the number first and the facts after`() {
        val plan = textStackPlan(
            twoByTwo, 1f,
            stale = false, sentence = true, warning = false, range = true
        )
        // 189 − 28 − 21.12 (place) = 139.88; the number reserves 52.8, two lines of
        // sentence take 47.52 and the range 21.12, and the 18.44 left over goes back to
        // the number.
        assertEquals(53.97f, plan.heroSp, 0.01f)
        assertEquals(2, plan.sentenceLines)
        assertTrue(plan.showRange)
    }

    @Test
    fun `on a narrow tall card the warning outranks the day's range`() {
        val plan = textStackPlan(
            twoByTwo, 1f,
            stale = true, sentence = true, warning = true, range = true
        )
        assertTrue(plan.showWarning)
        assertFalse(plan.showRange)
        // The stale marker and the warning both cost the number, and it stays well over
        // its floor.
        assertEquals(42.97f, plan.heroSp, 0.01f)
    }

    @Test
    fun `both tall forms ask for the warning's line the way the one-row card does`() {
        assertTrue(textStackHasFactLine(twoByTwo, 1f, stale = false, sentence = true))
        assertTrue(textPanelHasFactLine(fourByTwo, 1f, sentence = true))
    }

    /** Never three on a tall card: the Now widget's tall card made the same call for the
     * same reason, and here the third line is worth more to the number than to the prose. */
    @Test
    fun `a tall card's sentence stops at two lines`() {
        listOf(twoByTwo, DpSize(159.dp, 293.dp), DpSize(159.dp, 397.dp)).forEach { size ->
            val plan = textStackPlan(
                size, 1f, stale = false, sentence = true, warning = false, range = true
            )
            assertEquals(TextStackSentenceMaxLines, plan.sentenceLines)
        }
        listOf(fourByTwo, fourByThree, DpSize(340.dp, 397.dp)).forEach { size ->
            val plan = textPanelPlan(
                size, 1f, stale = false, sentence = true, warning = false, range = true
            )
            assertEquals(TextPanelSentenceMaxLines, plan.sentenceLines)
        }
    }

    /** The inverse the whole file rests on: a size asked for a line box, and the line box
     * asked back for the size. */
    @Test
    fun `the line box inverts`() {
        listOf(11f, 16f, 18f, 30f, 64f).forEach { sp ->
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
        assertTrue(TextHeroStackFloor in TextHeroFloor..TextHeroMax)
        assertTrue(TextHeroMax < TextHeroPanelMax)
    }
}
