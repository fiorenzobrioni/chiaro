package com.callbackdev.chiaro.ui.today

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The wide page's arithmetic (DESIGN §8.12c): when Today stands in two panes, and how wide. */
class TodayColumnsTest {

    @Test
    fun `a phone is one column with nothing kept at its edges`() {
        val c = todayColumns(pageWidth = 411.dp, railInset = 0.dp)
        assertFalse(c.twoPanes)
        assertEquals(0.dp, c.start)
        assertEquals(0.dp, c.end)
        assertEquals(411.dp, c.pane)
    }

    @Test
    fun `a narrow wide window keeps one column beside the rail`() {
        // A foldable held upright: 701dp of window, 621 of paper past the rail.
        val c = todayColumns(pageWidth = 701.dp, railInset = 80.dp)
        assertFalse(c.twoPanes)
        assertEquals(80.dp, c.start)
        assertEquals(621.dp, c.pane)
    }

    @Test
    fun `an open foldable stands in two panes with Material's margins`() {
        val c = todayColumns(pageWidth = 841.dp, railInset = 80.dp)
        assertTrue(c.twoPanes)
        // 8 outside each pane plus each section's 16: 24 at the edges.
        assertEquals(88.dp, c.start)
        assertEquals(8.dp, c.end)
        assertEquals(372.5.dp, c.pane)
    }

    @Test
    fun `every pane holds the week's row on one line`() {
        // 182 of fixed columns, 72 of bars, the segment's 24, the section's 32.
        val c = todayColumns(pageWidth = TwoPanesMin, railInset = 0.dp)
        assertTrue(c.twoPanes)
        assertTrue(c.pane >= 310.dp)
    }

    @Test
    fun `past the widest the panes stand centred`() {
        val c = todayColumns(pageWidth = 1680.dp, railInset = 80.dp)
        assertTrue(c.twoPanes)
        // 1600 of paper, 1200 of panes: 200 each side.
        assertEquals(80.dp + 200.dp + PaneMargin, c.start)
        assertEquals(200.dp + PaneMargin, c.end)
        assertEquals(592.dp, c.pane)
    }

    @Test
    fun `a phone's details stay two a row`() {
        assertEquals(2, detailsPerRow(count = 9, width = 328.dp))
        assertEquals(2, detailsPerRow(count = 9, width = 379.dp))
        assertEquals(2, detailsPerRow(count = 9, width = 280.dp))
    }

    @Test
    fun `nine tiles stand three by three rather than leave one alone`() {
        assertEquals(3, detailsPerRow(count = 9, width = 713.dp))
        assertEquals(3, detailsPerRow(count = 9, width = 1152.dp))
    }

    @Test
    fun `eight tiles take four a row when four fit`() {
        assertEquals(4, detailsPerRow(count = 8, width = 713.dp))
        assertEquals(2, detailsPerRow(count = 8, width = 400.dp))
    }

    @Test
    fun `with no even split the row takes what fits`() {
        assertEquals(4, detailsPerRow(count = 7, width = 713.dp))
    }
}
