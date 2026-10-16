package com.callbackdev.chiaro.ui.today

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/** The details grid on a wide page (DESIGN §8.12c): how many tiles a row. */
class DetailsPerRowTest {

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
