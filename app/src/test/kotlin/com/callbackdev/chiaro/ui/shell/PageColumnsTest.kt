package com.callbackdev.chiaro.ui.shell

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The wide page's arithmetic (DESIGN §8.12c): when a tab stands in two panes, and how wide. */
class PageColumnsTest {

    @Test
    fun `a phone is one column with nothing kept at its edges`() {
        val c = pageColumns(pageWidth = 411.dp, railInset = 0.dp)
        assertFalse(c.twoPanes)
        assertEquals(0.dp, c.start)
        assertEquals(0.dp, c.end)
        assertEquals(411.dp, c.pane)
    }

    @Test
    fun `a narrow wide window keeps one column beside the rail`() {
        // A foldable held upright: 701dp of window, 621 of paper past the rail.
        val c = pageColumns(pageWidth = 701.dp, railInset = 80.dp)
        assertFalse(c.twoPanes)
        assertEquals(80.dp, c.start)
        assertEquals(621.dp, c.pane)
    }

    @Test
    fun `an open foldable stands in two panes with Material's margins`() {
        val c = pageColumns(pageWidth = 841.dp, railInset = 80.dp)
        assertTrue(c.twoPanes)
        // 8 outside each pane plus each section's 16: 24 at the edges.
        assertEquals(88.dp, c.start)
        assertEquals(8.dp, c.end)
        assertEquals(372.5.dp, c.pane)
    }

    @Test
    fun `a tab beside the rail splits where Today does`() {
        // Sky and Alerts are laid out beside the rail, Today under it with the rail as its
        // inset: the same window must give the same panes, or the tabs would disagree.
        for (window in listOf(701.dp, 841.dp, 1280.dp, 1680.dp)) {
            val today = pageColumns(pageWidth = window, railInset = 80.dp)
            val tab = pageColumns(pageWidth = window - 80.dp)
            assertEquals(today.twoPanes, tab.twoPanes)
            assertEquals(today.pane, tab.pane)
            assertEquals(today.start - 80.dp, tab.start)
            assertEquals(today.end, tab.end)
        }
    }

    @Test
    fun `every pane holds the week's row on one line`() {
        // 182 of fixed columns, 72 of bars, the segment's 24, the section's 32.
        val c = pageColumns(pageWidth = TwoPanesMin, railInset = 0.dp)
        assertTrue(c.twoPanes)
        assertTrue(c.pane >= 310.dp)
    }

    @Test
    fun `past the widest the panes stand centred`() {
        val c = pageColumns(pageWidth = 1680.dp, railInset = 80.dp)
        assertTrue(c.twoPanes)
        // 1600 of paper, 1200 of panes: 200 each side.
        assertEquals(80.dp + 200.dp + PaneMargin, c.start)
        assertEquals(200.dp + PaneMargin, c.end)
        assertEquals(592.dp, c.pane)
    }
}
