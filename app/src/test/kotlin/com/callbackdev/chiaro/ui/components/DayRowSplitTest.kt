package com.callbackdev.chiaro.ui.components

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Where a week row turns into two (review of Today, 15 ott 2026): when what is left to the
 * range bar, beside the four text columns, the icon, the five gaps and the chevron's
 * column, is under 48dp. The widths are the row's content box — the screen less the open
 * day's 8dp inset and its 8dp of inner air on each side, the page's 16.
 */
class DayRowSplitTest {

    private val phone360 = (360 - 32).dp
    private val phone411 = (411 - 32).dp

    @Test
    fun `the row most readers see keeps one line, chevron included`() {
        // 328 − (148 + 38 + 40 + 20) = 82dp of bar.
        assertFalse(splitsRow(phone360, 1f))
        assertFalse(splitsRow(phone411, 1f))
    }

    @Test
    fun `a narrow phone splits a step sooner than the old fixed 150 percent`() {
        // At 120% the bar still has 52dp; at 125% it would have 45.
        assertFalse(splitsRow(phone360, 1.2f))
        assertTrue(splitsRow(phone360, 1.25f))
    }

    @Test
    fun `a wider phone keeps one line past it`() {
        // 379 − (222 + 98) = 59dp of bar at 150%.
        assertFalse(splitsRow(phone411, 1.5f))
        assertTrue(splitsRow(phone411, 2f))
    }

    @Test
    fun `the floor is the bar's own minimum`() {
        // Exactly the fixed part plus the floor keeps one line; a dp less does not.
        val fixed = (148 + 38 + 40 + 20).dp
        assertFalse(splitsRow(fixed + RangeBarMin, 1f))
        assertTrue(splitsRow(fixed + RangeBarMin - 1.dp, 1f))
    }
}
