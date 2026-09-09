package com.callbackdev.chiaro.widget

import com.callbackdev.chiaro.domain.warnings.WarningLevel
import com.callbackdev.chiaro.domain.warnings.WarningLevel.NONE
import com.callbackdev.chiaro.domain.warnings.WarningLevel.ORANGE
import com.callbackdev.chiaro.domain.warnings.WarningLevel.RED
import com.callbackdev.chiaro.domain.warnings.WarningLevel.YELLOW
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Where an official warning lands on a home-screen card (Fase 11, fourth step). The rule
 * is one table, and the table is here rather than in four widgets: the four cards had to
 * agree about when the sentence is already saying it, and four copies of a rule are four
 * chances to disagree.
 */
class WidgetWarningTest {

    private fun slot(
        level: WarningLevel?,
        enabled: Boolean = true,
        headlineShown: Boolean = false,
        sentenceSlot: Boolean = false,
        ownRow: Boolean = false
    ) = warningSlot(level, enabled, headlineShown, sentenceSlot, ownRow)

    @Test
    fun `nothing graded draws nothing, whatever the form offers`() {
        assertEquals(
            WarningSlot.NONE,
            slot(null, sentenceSlot = true, ownRow = true)
        )
        assertEquals(
            WarningSlot.NONE,
            slot(NONE, sentenceSlot = true, ownRow = true)
        )
    }

    @Test
    fun `the switch turns it off everywhere`() {
        assertEquals(
            WarningSlot.NONE,
            slot(RED, enabled = false, sentenceSlot = true, ownRow = true)
        )
        assertEquals(
            WarningSlot.NONE,
            slot(YELLOW, enabled = false, ownRow = true)
        )
    }

    /** The sentence's brief register IS «Allerta arancione · temporali» — a chip beside
     * it would be the card saying one thing twice. */
    @Test
    fun `orange and red stay silent where the day's sentence is already showing`() {
        assertEquals(
            WarningSlot.NONE,
            slot(ORANGE, headlineShown = true, sentenceSlot = true, ownRow = true)
        )
        assertEquals(
            WarningSlot.NONE,
            slot(RED, headlineShown = true, sentenceSlot = true, ownRow = true)
        )
    }

    @Test
    fun `with the sentence off the chip takes its place, and costs the card nothing`() {
        assertEquals(WarningSlot.SENTENCE, slot(ORANGE, sentenceSlot = true, ownRow = true))
        assertEquals(WarningSlot.SENTENCE, slot(RED, sentenceSlot = true))
    }

    /** A card whose sentence slot is showing something else — the arc with its hero on
     * the next light moment — is not showing the warning, so the chip is not a repeat. */
    @Test
    fun `a sentence that is not the headline does not count as saying it`() {
        assertEquals(
            WarningSlot.SENTENCE,
            slot(ORANGE, headlineShown = false, sentenceSlot = true)
        )
    }

    @Test
    fun `orange with no sentence slot at all takes a line of its own, or nothing`() {
        assertEquals(WarningSlot.OWN_ROW, slot(RED, ownRow = true))
        assertEquals(WarningSlot.NONE, slot(RED))
    }

    /** Yellow is never in a sentence, so it only ever appears where the form has a line
     * to spare — never in the sentence's place, because the sentence is still true. */
    @Test
    fun `yellow only ever gets a line of its own`() {
        assertEquals(WarningSlot.OWN_ROW, slot(YELLOW, ownRow = true))
        assertEquals(WarningSlot.NONE, slot(YELLOW, sentenceSlot = true))
        assertEquals(
            WarningSlot.OWN_ROW,
            slot(YELLOW, headlineShown = true, sentenceSlot = true, ownRow = true)
        )
    }

    @Test
    fun `only a line of its own costs the card anything`() {
        assertTrue(WarningSlot.OWN_ROW.drawn)
        assertTrue(WarningSlot.SENTENCE.drawn)
        assertTrue(!WarningSlot.NONE.drawn)
        assertEquals(0.0f, warningBlock(1f, drawn = false).value)
        assertEquals(warningChipHeight(1f) + WarningChipGap, warningBlock(1f, drawn = true))
    }

    /** The chip's box at the default font size: the word's line, which is taller than
     * the 12 dp mark, plus 3 dp of padding either side. */
    @Test
    fun `the chip is one measured height every budget subtracts`() {
        assertEquals(20.52f, warningChipHeight(1f).value, 0.01f)
        // A reader's larger font grows it, and every budget that made room follows.
        assertTrue(warningChipHeight(1.3f) > warningChipHeight(1f))
    }
}
