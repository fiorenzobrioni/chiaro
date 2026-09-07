package com.callbackdev.chiaro.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The two rules of `TextScale.kt`, which decide whether a value at 200% keeps its line
 * or gets its own row (DESIGN.md §10).
 *
 * They are pure on purpose: "the week row reflows above 150%" is a rule about numbers,
 * and a rule about numbers should not need a device to check.
 */
class TextScaleTest {

    @Test
    fun `a text column never shrinks below its designed width`() {
        // A reader who scales type DOWN gets the layout as measured, not a tighter one:
        // the column widths were budgeted against the widest string, not against 14sp.
        assertEquals(1f, textColumnScale(0.85f), 1e-6f)
        assertEquals(1f, textColumnScale(1f), 1e-6f)
    }

    @Test
    fun `a text column grows with the reader's type`() {
        assertEquals(1.3f, textColumnScale(1.3f), 1e-6f)
        assertEquals(2f, textColumnScale(2f), 1e-6f)
    }

    @Test
    fun `growth stops where the system's own slider stops`() {
        // Past 200% the reflow carries the layout; multiplying a 44dp column by 3 would
        // spend the whole screen on the day name.
        assertEquals(TextColumnCeiling, textColumnScale(3f), 1e-6f)
        assertEquals(TextColumnCeiling, textColumnScale(10f), 1e-6f)
    }

    @Test
    fun `a row of columns reflows only once its columns stop fitting`() {
        assertFalse("the default layout is the one most readers see", reflowsForText(1f))
        assertFalse("large type still fits one line", reflowsForText(1.3f))
        assertFalse("and so does the step below the threshold", reflowsForText(1.49f))
        assertTrue("at the threshold the week row becomes two rows", reflowsForText(ReflowTextScale))
        assertTrue("and stays two at 200%", reflowsForText(2f))
    }

    @Test
    fun `the threshold is where the range bar stops being a bar`() {
        // The measurement behind ReflowTextScale, kept next to the number so moving one
        // means moving the other. A 360dp screen, 16dp of padding a side: 328dp of row.
        val screen = 360f - 32f
        val columns = (44f + 36f + 34f + 34f) * ReflowTextScale
        val icon = 34f
        val gaps = 8f * 5f
        assertTrue(
            "at ${ReflowTextScale}x the bar has ${screen - columns - icon - gaps}dp left",
            screen - columns - icon - gaps < 48f
        )
        // And one step below it, the bar is still a bar.
        val columnsBelow = (44f + 36f + 34f + 34f) * 1.3f
        assertTrue("at 1.3x the bar should survive", screen - columnsBelow - icon - gaps >= 48f)
    }
}
