package com.callbackdev.chiaro.widget.window

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The window's forms and plans, pinned by the table of grants a launcher really gives. */
class WindowLayoutTest {

    @Test
    fun `the grant picks the form`() {
        assertEquals(WindowForm.STRIP, windowForm(DpSize(340.dp, 82.dp)))
        assertEquals(WindowForm.STRIP, windowForm(DpSize(159.dp, 101.dp)))
        assertEquals(WindowForm.NARROW, windowForm(DpSize(159.dp, 189.dp)))
        assertEquals(WindowForm.NARROW, windowForm(DpSize(159.dp, 397.dp)))
        assertEquals(WindowForm.WINDOW, windowForm(DpSize(250.dp, 189.dp)))
        assertEquals(WindowForm.WINDOW, windowForm(DpSize(340.dp, 189.dp)))
        assertEquals(WindowForm.TALL, windowForm(DpSize(250.dp, 293.dp)))
        assertEquals(WindowForm.TALL, windowForm(DpSize(340.dp, 397.dp)))
    }

    @Test
    fun `the tall card gives the rows what the sky leaves, and never a row that does not exist`() {
        val three = windowPlan(DpSize(340.dp, 293.dp), 1f, momentsAvailable = 6, warning = false)
        assertEquals(WindowForm.TALL, three.form)
        assertEquals(3, three.momentRows)
        assertTrue(three.skyHeight >= WindowSkyMin)
        assertEquals(293.dp, three.skyHeight + windowRowHeight(1f) * 3 + WindowRowsPadding)

        val four = windowPlan(DpSize(340.dp, 397.dp), 1f, momentsAvailable = 6, warning = false)
        assertEquals(6, four.momentRows)
        assertTrue(four.skyHeight >= WindowSkyMin)

        val few = windowPlan(DpSize(340.dp, 397.dp), 1f, momentsAvailable = 2, warning = false)
        assertEquals(2, few.momentRows)
        assertTrue("the sky takes the room the rows leave", few.skyHeight > four.skyHeight)

        val none = windowPlan(DpSize(340.dp, 397.dp), 1f, momentsAvailable = 0, warning = false)
        assertEquals(0, none.momentRows)
        assertEquals(397.dp, none.skyHeight)

        // A bigger font takes taller rows, so fewer of them.
        val big = windowPlan(DpSize(340.dp, 293.dp), 1.6f, momentsAvailable = 6, warning = false)
        assertTrue(big.momentRows <= three.momentRows)
        assertTrue(windowRowHeight(1.6f) >= windowRowHeight(1f))
    }

    @Test
    fun `no rows under the picture on the forms that have no room for a list`() {
        listOf(DpSize(340.dp, 82.dp), DpSize(159.dp, 397.dp), DpSize(340.dp, 189.dp)).forEach { size ->
            val plan = windowPlan(size, 1f, momentsAvailable = 6, warning = true)
            assertEquals("$size", 0, plan.momentRows)
            assertEquals(size.height, plan.skyHeight)
        }
    }

    @Test
    fun `what the caption carries, by form`() {
        val strip = windowPlan(DpSize(340.dp, 82.dp), 1f, 3, warning = true)
        assertFalse(strip.labels)
        assertFalse(strip.compass)
        assertFalse(strip.warning)
        assertEquals(1, strip.captionLines)

        val narrow = windowPlan(DpSize(159.dp, 189.dp), 1f, 3, warning = true)
        assertTrue(narrow.labels)
        assertFalse(narrow.compass)
        assertFalse(narrow.warning)
        assertEquals(2, narrow.captionLines)

        val window = windowPlan(DpSize(340.dp, 189.dp), 1f, 3, warning = true)
        assertTrue(window.labels)
        assertTrue(window.compass)
        assertTrue(window.warning)
        assertEquals(2, window.captionLines)

        // The chip is drawn only when there is a warning to carry.
        assertFalse(windowPlan(DpSize(340.dp, 189.dp), 1f, 3, warning = false).warning)
        val tall = windowPlan(DpSize(340.dp, 293.dp), 1f, 3, warning = true)
        assertTrue(tall.labels && tall.compass && tall.warning)
    }
}
