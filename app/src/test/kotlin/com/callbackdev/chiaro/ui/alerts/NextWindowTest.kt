package com.callbackdev.chiaro.ui.alerts

import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The window the head of Alerts names (the Alerts review, point 7). */
class NextWindowTest {

    // The morning summary, the bulletin, the evening summary, as the strip draws them.
    private val day = listOf(6 to 12, 15 to 17, 18 to 23)

    @Test
    fun `before dawn the morning window is next, today`() {
        assertEquals(NextWindow(0, NextWhen.TODAY), nextWindow(day, LocalTime.of(5, 59)))
    }

    @Test
    fun `inside a window it is the one open now, to its last minute`() {
        assertEquals(NextWindow(0, NextWhen.NOW), nextWindow(day, LocalTime.of(6, 0)))
        assertEquals(NextWindow(0, NextWhen.NOW), nextWindow(day, LocalTime.of(11, 59)))
        assertEquals(NextWindow(1, NextWhen.TODAY), nextWindow(day, LocalTime.of(12, 0)))
    }

    @Test
    fun `after the last window the first comes round tomorrow`() {
        assertEquals(NextWindow(0, NextWhen.TOMORROW), nextWindow(day, LocalTime.of(23, 0)))
    }

    @Test
    fun `windows switched off are simply not in the list, in any order`() {
        val evenings = listOf(18 to 23, 15 to 17)
        assertEquals(NextWindow(1, NextWhen.TODAY), nextWindow(evenings, LocalTime.of(13, 0)))
        assertEquals(NextWindow(1, NextWhen.TOMORROW), nextWindow(evenings, LocalTime.of(23, 30)))
    }

    @Test
    fun `nothing timed on, nothing named`() {
        assertNull(nextWindow(emptyList(), LocalTime.NOON))
    }

    // The bulletin's window once today's bulletin is in hand (15.2, 28 Oct 2026): spent for
    // today, skipped open or not, back tomorrow.

    @Test
    fun `a spent window is skipped before it opens and while it is open`() {
        assertEquals(NextWindow(2, NextWhen.TODAY), nextWindow(day, LocalTime.of(14, 0), spent = setOf(1)))
        assertEquals(NextWindow(2, NextWhen.TODAY), nextWindow(day, LocalTime.of(16, 0), spent = setOf(1)))
    }

    @Test
    fun `a spent window alone comes round tomorrow`() {
        val bulletin = listOf(15 to 17)
        assertEquals(NextWindow(0, NextWhen.TOMORROW), nextWindow(bulletin, LocalTime.of(14, 0), spent = setOf(0)))
        assertEquals(NextWindow(0, NextWhen.TOMORROW), nextWindow(bulletin, LocalTime.of(16, 0), spent = setOf(0)))
    }

    @Test
    fun `a spent window does not move the ones before it`() {
        assertEquals(NextWindow(0, NextWhen.NOW), nextWindow(day, LocalTime.of(9, 0), spent = setOf(1)))
    }
}
