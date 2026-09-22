package com.callbackdev.chiaro.widget

import androidx.compose.ui.unit.Dp
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

    /** A slot and the space it has to fit in are two routes through the same arithmetic, so
     * they land on the same dp and meet exactly. A hundredth of a dp of float slack, because
     * an estimate to that precision is not a layout claim. */
    private fun assertFits(what: String, taken: Dp, available: Dp) =
        assertTrue(
            "$what: ${taken.value} dp into ${available.value}",
            taken.value <= available.value + 0.01f
        )

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

    /**
     * The weather glyph (committente, 20 set 2026) is drawn only into space the card
     * already leaves empty, so the promise worth pinning is not where it goes but what it
     * costs: **nothing**. Every plan on this card is computed without it, and these are the
     * sizes that fall out of the air that is left.
     */
    @Test
    fun `the glyph rides in the air beside the number on a one-row card`() {
        // 340: the name's column is 168, the widest number it can print is 82.5, and the
        // number's own line box caps what is left at 51.9.
        assertEquals(51.88f, textRowIconSize(fourByOne, 1f).value, 0.01f)
        assertEquals(51.88f, textRowIconSize(DpSize(320.dp, 85.dp), 1f).value, 0.01f)
        assertEquals(51.88f, textRowIconSize(DpSize(430.dp, 85.dp), 1f).value, 0.01f)
        // The taller row the other launcher grants has a taller line to fill.
        assertEquals(58.08f, textRowIconSize(tallRow, 1f).value, 0.01f)
        // Two cells: no second column, so the whole width is the number's and there is
        // room for a glyph at the card's own trailing edge.
        assertEquals(51.88f, textRowIconSize(twoByOne, 1f).value, 0.01f)
    }

    /**
     * Option A (committente, 20 set 2026): at three cells there is no glyph, and it is the
     * form's own trade rather than a failure — that is the width this card spends on the
     * sentence, which the Now widget at three cells does not have.
     */
    @Test
    fun `three cells on one row stay words`() {
        assertEquals(TextForm.ROW, textForm(threeByOne))
        assertEquals(0f, textRowIconSize(threeByOne, 1f).value, 0.001f)
        // 272 dp: 45.5 dp of air, still under the 48 the family's smallest drawing needs.
        assertEquals(0f, textRowIconSize(DpSize(272.dp, 85.dp), 1f).value, 0.001f)
        // And it comes back as soon as the column really has the room.
        assertTrue(textRowIconSize(DpSize(300.dp, 85.dp), 1f) >= TextIconMin)
        // One cell: the number fills its own column, so there is nothing to give.
        assertEquals(0f, textRowIconSize(DpSize(110.dp, 85.dp), 1f).value, 0.001f)
        // A reader's larger font scale grows the text and shrinks the air, and the glyph
        // yields rather than the words.
        assertEquals(0f, textRowIconSize(fourByOne, 1.3f).value, 0.001f)
    }

    @Test
    fun `the narrow tall card gives the glyph the number's line, where it has one`() {
        val plan = textStackPlan(
            threeByTwo, 1f, stale = false, sentence = true, warning = false, range = true
        )
        assertEquals(71.24f, textStackIconSize(threeByTwo, 1f, plan.heroSp).value, 0.01f)
        // Two cells: the number takes every leftover dp, so there is no air to take and
        // taking it from the number would be changing the text.
        val narrow = textStackPlan(
            twoByTwo, 1f, stale = false, sentence = true, warning = false, range = true
        )
        assertEquals(0f, textStackIconSize(twoByTwo, 1f, narrow.heroSp).value, 0.001f)
    }

    @Test
    fun `the panel hangs the glyph in the band it keeps empty by construction`() {
        val plan = textPanelPlan(
            fourByTwo, 1f, stale = false, sentence = true, warning = false, range = true
        )
        assertEquals(71.24f, textPanelIconSize(fourByTwo, 1f, plan).value, 0.01f)
        // The stale marker lives in the LEADING column, under the number, so it never
        // costs the glyph a dp — which is the reason the band is measured against the
        // trailing column and not against the taller of the two.
        val stale = textPanelPlan(
            fourByTwo, 1f, stale = true, sentence = true, warning = false, range = true
        )
        assertEquals(71.24f, textPanelIconSize(fourByTwo, 1f, stale).value, 0.01f)
        // A warning's word is a line the card grew for a reason, and the drawing yields.
        val warned = textPanelPlan(
            fourByTwo, 1f, stale = false, sentence = true, warning = true, range = true
        )
        assertTrue(textPanelIconSize(fourByTwo, 1f, warned) < textPanelIconSize(fourByTwo, 1f, plan))
        // Three rows: all band, so the family's own ceiling is what stops it.
        val tall = textPanelPlan(
            fourByThree, 1f, stale = false, sentence = true, warning = false, range = true
        )
        assertEquals(104f, textPanelIconSize(fourByThree, 1f, tall).value, 0.01f)
        // The shortest card that is a panel at all has no band to spare.
        val squeezed = DpSize(300.dp, 150.dp)
        val tight = textPanelPlan(
            squeezed, 1f, stale = false, sentence = true, warning = false, range = true
        )
        assertEquals(0f, textPanelIconSize(squeezed, 1f, tight).value, 0.001f)
    }

    /** Everything a one-row card stacks in its leading column, on a stale card: the place's
     * line, the number the budget sized, and the marker that is never dropped. */
    private fun oneRowColumn(size: DpSize, scale: Float): Dp =
        textLineHeight(TextFactSp, scale) +
            textLineHeight(textRowHeroSp(size, scale, stale = true), scale) +
            textLineHeight(TextStaleSp, scale)

    /**
     * **The one-row budget spends the card's whole height, so the estimate's error has
     * nowhere to go.** [textRowHeroSp] hands the number everything the place's line and the
     * marker's leave, which means the leading column measures EXACTLY the card's own budget
     * at every grant that can pay the hero's floor — zero slack, by construction, while the
     * file that defines [textLineHeight] says the budgets resting on it are supposed to keep
     * a band.
     *
     * Pinned because it is the mechanism behind the bug, not an incidental number: a device
     * whose system font boxes taller than the estimated 1.32 em overruns, and the vertical
     * LinearLayout the column compiles to cuts its LAST child — the stale marker, drawn and
     * then sliced at the baseline (committente, 22 set 2026, «Aggiornato 2 ore fa» tagliato
     * in basso su una card 4×1).
     */
    @Test
    fun `a one-row card spends every dp of its own budget`() {
        listOf(twoByOne, threeByOne, fourByOne).forEach { size ->
            listOf(0.85f, 1f).forEach { scale ->
                assertEquals(
                    "$size/$scale: the column is the budget, to the dp",
                    (size.height - WidgetCardPaddingSnug * 2).value,
                    oneRowColumn(size, scale).value,
                    0.01f
                )
            }
        }
    }

    /**
     * The answer to it: the budget still spends the snug inset — every size and every
     * position on the card is the one it was — and the CARD lends it back, so the column
     * may overrun into its own air instead of cutting the marker.
     *
     * [TallerBox] is the shape of the error, not a device's exact number: a system font
     * whose top-to-bottom box is 4% past the estimate. On the reference grants that is
     * ~3 dp, which is what the marker was losing.
     */
    @Test
    fun `the lent-back inset carries a font box taller than the estimate`() {
        listOf(twoByOne, threeByOne, fourByOne).forEach { size ->
            listOf(0.85f, 1f, 1.15f).forEach { scale ->
                val real = Dp(oneRowColumn(size, scale).value * TallerBox)
                assertTrue(
                    "$size/$scale: the old inset had already clipped ${real.value} dp",
                    real > size.height - WidgetCardPaddingSnug * 2
                )
                assertFits(
                    "$size/$scale: the card carries it",
                    real, size.height - textCardPaddingVertical(textForm(size)) * 2
                )
            }
        }
    }

    /** A font box 4% taller than [textLineHeight]'s estimate: the shape of the error the
     * headroom absorbs, sized so it is a few dp on a one-row card rather than a claim about
     * one launcher's font. */
    private val TallerBox = 1.04f

    @Test
    fun `only the one-row forms lend their vertical inset back`() {
        // The two forms that centre what they hold, so a symmetric inset is free to go.
        assertEquals(0f, textCardPaddingVertical(TextForm.ROW).value, 0.001f)
        assertEquals(0f, textCardPaddingVertical(TextForm.LINE).value, 0.001f)
        // The stack and the panel pin an eyebrow to the top and a block to the bottom, so
        // there the inset is a position; a card with no report yet has no form at all.
        assertEquals(WidgetCardPadding, textCardPaddingVertical(TextForm.STACK))
        assertEquals(WidgetCardPadding, textCardPaddingVertical(TextForm.PANEL))
        assertEquals(WidgetCardPadding, textCardPaddingVertical(null))
        // And what the budget spends is what the card lends back, in one place.
        assertEquals(WidgetCardPaddingSnug * 2, TextRowHeadroom)
        assertTrue(textOneRow(TextForm.ROW) && textOneRow(TextForm.LINE))
        assertFalse(textOneRow(TextForm.STACK) || textOneRow(TextForm.PANEL) || textOneRow(null))
    }

    /**
     * **Where the headroom stops, on the record.** Past font scale 1.15 the column overruns
     * for a second reason: the number is already on [TextHeroFloor] and has nothing left to
     * pay the marker with, so at 1.3 an 85 dp row wants ~91 dp before the card's air is even
     * counted. Twelve dp of headroom does not reach it, and the only dp left to take are the
     * number's own — which is a different decision from this one (it would change the size of
     * the temperature on every card that falls to the floor), so it is written down rather
     * than quietly made here.
     */
    @Test
    fun `the largest font on the shortest row is past what any inset can carry`() {
        assertEquals(TextHeroFloor, textRowHeroSp(fourByOne, 1.3f, stale = true), 0.01f)
        assertTrue(
            "the 1.3 column is ${oneRowColumn(fourByOne, 1.3f).value} dp of an 85 dp card",
            oneRowColumn(fourByOne, 1.3f) > fourByOne.height
        )
        // One step down it is inside the card, which is where the headroom earns its keep.
        assertFits("scale 1.15", oneRowColumn(fourByOne, 1.15f), fourByOne.height)
    }

    /**
     * The promise the whole feature rests on, asserted rather than reviewed: **the glyph
     * fits in space the card was already leaving empty.** The failure mode is not an ugly
     * card, it is a line of sentence or a stale marker quietly clipped on somebody's home
     * screen the day they turn the switch on — which is exactly the kind of thing nobody
     * reports as a bug.
     */
    @Test
    fun `the glyph never takes a dp from anything the card already drew`() {
        val oneRow = listOf(twoByOne, threeByOne, fourByOne, tallRow, DpSize(300.dp, 85.dp))
        oneRow.forEach { size ->
            listOf(1f, 1.3f, 0.85f).forEach { scale ->
                val icon = textRowIconSize(size, scale)
                if (icon <= 0.dp) return@forEach
                val column = if (textForm(size) == TextForm.LINE) {
                    size.width - WidgetCardPadding - WidgetCardPaddingLeading
                } else {
                    textLeadingColumn(size)
                }
                listOf(false, true).forEach { stale ->
                    val hero = textRowHeroSp(size, scale, stale)
                    // Under the place's line, on a fresh card and on a stale one: the band
                    // the glyph sits in is everything the column holds below that line.
                    val band = textLineHeight(hero, scale) +
                        (if (stale) textLineHeight(TextStaleSp, scale) else 0.dp)
                    assertFits("$size/$scale stale=$stale: glyph over the place line", icon, band)
                    // And clear of the number itself, at its widest.
                    assertFits(
                        "$size/$scale stale=$stale: glyph over the number",
                        icon, column - (hero * TempEmWidth * scale).dp
                    )
                }
                // Clear of the stale marker too: six ems of its own size is «7 giorni fa»
                // with room to spare, and the glyph never comes within that of the start.
                assertTrue(
                    "$size/$scale: glyph over the stale marker",
                    column - icon >= (TextStaleSp * 6f).dp
                )
            }
        }
    }

    @Test
    fun `neither tall form grows to make room for the glyph`() {
        listOf(twoByTwo, threeByTwo, DpSize(250.dp, 293.dp)).forEach { size ->
            listOf(1f, 1.3f).forEach { scale ->
                listOf(false, true).forEach { stale ->
                    val plan = textStackPlan(
                        size, scale, stale, sentence = true, warning = true, range = true
                    )
                    val icon = textStackIconSize(size, scale, plan.heroSp)
                    // It rides the number's own line and is never taller than it, so the
                    // row it shares is the line the budget already paid for.
                    assertFits(
                        "$size/$scale: glyph taller than the number's line",
                        icon, textLineHeight(plan.heroSp, scale)
                    )
                    assertFits(
                        "$size/$scale: glyph over the number", icon,
                        size.width - WidgetCardPadding - WidgetCardPaddingLeading -
                            (plan.heroSp * TempEmWidth * scale).dp
                    )
                }
            }
        }
        listOf(fourByTwo, fourByThree, DpSize(320.dp, 189.dp), DpSize(300.dp, 150.dp)).forEach { size ->
            listOf(1f, 1.3f).forEach { scale ->
                listOf(false, true).forEach { stale ->
                    val plan = textPanelPlan(
                        size, scale, stale, sentence = true, warning = true, range = true
                    )
                    val icon = textPanelIconSize(size, scale, plan)
                    val fact = textLineHeight(TextFactSp, scale)
                    val trailing = textLineHeight(TextSentenceSp, scale) * plan.sentenceLines +
                        (if (plan.showWarning) fact else 0.dp) +
                        (if (plan.showRange) fact else 0.dp)
                    // The glyph grows the trailing column upward into the band the spacer
                    // was holding, and the eyebrow plus the whole column still fit.
                    assertFits(
                        "$size/$scale stale=$stale: the panel grew for the glyph",
                        fact + icon + trailing, size.height - WidgetCardPadding * 2
                    )
                    assertFits(
                        "$size/$scale: glyph wider than the column it sits in",
                        icon, textPanelSentenceColumn(size) + TextIconEdgeGive
                    )
                }
            }
        }
    }

    /** A glyph that met the card's edge at the words' inset would read 10 dp too far in
     * (the Now widget measured it); the card gives it the glyph's inset and every text that
     * reached that edge pays the difference back, which is what keeps the promise literal. */
    @Test
    fun `the edge the glyph meets is a glyph's edge`() {
        assertEquals(10f, TextIconEdgeGive.value, 0.001f)
        assertEquals(WidgetCardPadding, WidgetCardPaddingLeading + TextIconEdgeGive)
    }

    /** The inset and the give-back are one condition read twice, so a line of words
     * measures against [WidgetCardPadding] on every form and either side of the switch. */
    @Test
    fun `what the card keeps at its trailing edge, the words are given back`() {
        (TextForm.entries + null).forEach { form ->
            listOf(false, true).forEach { showIcon ->
                assertEquals(
                    "$form/icon=$showIcon: the words' measure moved",
                    WidgetCardPadding,
                    textCardPaddingEnd(form, showIcon) + textEdgeGive(form, showIcon)
                )
            }
        }
        // ROW is the one form whose glyph never meets the card: it is interior, inside
        // the name's own column, so that edge is never given away in the first place.
        assertEquals(WidgetCardPadding, textCardPaddingEnd(TextForm.ROW, true))
        assertEquals(0.dp, textEdgeGive(TextForm.ROW, true))
        // A card with no report yet draws no glyph, so it has none to make room for.
        assertEquals(WidgetCardPadding, textCardPaddingEnd(null, true))
    }

    /**
     * The regression that put the pair in one place (21 set 2026): the edge went to the
     * glyph on the reader's switch alone, while the words paid it back only where a glyph
     * really came out of [textIconSize] — so on every grant too small for one the card
     * moved every line 10 dp towards its 24 dp corner and drew nothing there at all.
     *
     * Both of these grants ask for the glyph and get none, and both still write at 14.
     */
    @Test
    fun `a card with no room for a glyph keeps the words' own edge`() {
        val narrow = textStackPlan(
            twoByTwo, 1f, stale = false, sentence = true, warning = false, range = true
        )
        assertEquals(0f, textStackIconSize(twoByTwo, 1f, narrow.heroSp).value, 0.001f)
        assertEquals(TextIconEdgeGive, textEdgeGive(TextForm.STACK, showIcon = true))
        // The reference panel at a reader's larger font scale: the same question, and the
        // band between the eyebrow and the block has gone to the words.
        val panel = textPanelPlan(
            fourByTwo, 1.3f, stale = false, sentence = true, warning = true, range = true
        )
        assertEquals(0f, textPanelIconSize(fourByTwo, 1.3f, panel).value, 0.001f)
        assertEquals(TextIconEdgeGive, textEdgeGive(TextForm.PANEL, showIcon = true))
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
