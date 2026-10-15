package com.callbackdev.chiaro.ui.components

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Where a week row turns into two (second review of Today, 15 ott 2026): when what is left to
 * the block of the two tracks, beside the three text columns, the icon and the four gaps, is
 * under 72dp. The widths are the row's content box — the screen less the page's 16 and the
 * segment's 12 of inner air on each side.
 */
class DayRowSplitTest {

    private val phone360 = (360 - 56).dp
    private val phone411 = (411 - 56).dp

    @Test
    fun `the row most readers see keeps one line`() {
        // 304 − (112 + 38 + 32) = 122dp of tracks.
        assertFalse(splitsRow(phone360, 1f))
        assertFalse(splitsRow(phone411, 1f))
    }

    @Test
    fun `a narrow phone splits from about 145 percent`() {
        // 304 − (112 × 1.4 + 70) = 77dp of tracks at 140%; at 150%, 304 − (168 + 70) = 66.
        assertFalse(splitsRow(phone360, 1.4f))
        assertTrue(splitsRow(phone360, 1.5f))
    }

    @Test
    fun `a wider phone keeps one line further`() {
        // 355 − (168 + 70) = 117dp at 150%; at 200% 355 − (224 + 70) = 61.
        assertFalse(splitsRow(phone411, 1.5f))
        assertTrue(splitsRow(phone411, 2f))
    }

    @Test
    fun `the floor is the block's own minimum`() {
        val fixed = (112 + 38 + 32).dp
        assertFalse(splitsRow(fixed + TracksMin, 1f))
        assertTrue(splitsRow(fixed + TracksMin - 1.dp, 1f))
    }
}
