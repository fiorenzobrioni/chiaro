package com.callbackdev.chiaro.widget.window

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.widget.textLaterHeight
import com.callbackdev.chiaro.widget.textLaterRows
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
        // The hour's weather line is the look's, and needs a top: not on the strip.
        assertFalse(windowPlan(DpSize(340.dp, 189.dp), 1f, 3, warning = false).weather)
        assertTrue(windowPlan(DpSize(340.dp, 189.dp), 1f, 3, warning = false, weather = true).weather)
        assertTrue(windowPlan(DpSize(159.dp, 189.dp), 1f, 3, warning = false, weather = true).weather)
        assertFalse(windowPlan(DpSize(340.dp, 82.dp), 1f, 3, warning = false, weather = true).weather)
    }

    /** With the hour's weather on, the tall card's band is the next hours' table (20 ott 2026). */
    @Test
    fun `with the hour's weather on, the tall card's rows are the hours, and the band is the table's`() {
        val three = windowPlan(DpSize(340.dp, 293.dp), 1f, momentsAvailable = 6, warning = false, weather = true)
        assertEquals(0, three.momentRows)
        // The room under a sky of WindowSkyMin holds three of the table's rows at this size;
        // the band is cut to the table, and the sky takes what it leaves.
        val rows = textLaterRows(293.dp - WindowSkyMin - WindowHoursPadding, 1f)
        assertEquals(3, rows)
        assertEquals(rows, three.hourRows)
        assertEquals(293.dp, three.skyHeight + textLaterHeight(rows, 1f) + WindowHoursPadding)
        assertTrue(three.skyHeight >= WindowSkyMin)
        // The band is the weather's: it is there whether or not the reader follows a moment.
        assertEquals(rows, windowPlan(DpSize(340.dp, 293.dp), 1f, momentsAvailable = 0, warning = false, weather = true).hourRows)
        // Four rows at most, the table's own ceiling; the sky takes what the table leaves.
        val four = windowPlan(DpSize(340.dp, 397.dp), 1f, momentsAvailable = 6, warning = false, weather = true)
        assertEquals(4, four.hourRows)
        assertTrue(four.skyHeight > windowPlan(DpSize(340.dp, 397.dp), 1f, momentsAvailable = 6, warning = false).skyHeight)
        // No hours where there is no band, and a bigger font takes fewer rows.
        assertEquals(0, windowPlan(DpSize(340.dp, 189.dp), 1f, momentsAvailable = 6, warning = false, weather = true).hourRows)
        assertEquals(0, windowPlan(DpSize(159.dp, 397.dp), 1f, momentsAvailable = 6, warning = false, weather = true).hourRows)
        assertTrue(windowPlan(DpSize(340.dp, 293.dp), 1.6f, momentsAvailable = 6, warning = false, weather = true).hourRows <= rows)
    }
}
