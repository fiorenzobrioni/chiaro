package com.callbackdev.chiaro.ui.alerts

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The whole hours of Alerts' windows on the phone's clock (the Alerts review, 12 nov 2026):
 * unchanged on a twenty-four-hour one, and on a twelve-hour one the hour with its half of
 * the day, said once for a window that stays inside one half.
 */
class AlertHoursTest {

    private val us = Locale.US

    @Test
    fun `a twenty-four-hour clock prints the hours as they always were`() {
        assertEquals("0", AlertHours.label(0, true, us))
        assertEquals("6", AlertHours.label(6, true, us))
        assertEquals("18", AlertHours.label(18, true, us))
        // The strip's last mark is the day's end, not a second zero.
        assertEquals("24", AlertHours.label(24, true, us))
        assertEquals("15:00", AlertHours.clock(15, true, us))
    }

    @Test
    fun `a twelve-hour clock says the hour with its half of the day`() {
        assertEquals("12 AM", AlertHours.label(0, false, us))
        assertEquals("7 AM", AlertHours.label(7, false, us))
        assertEquals("12 PM", AlertHours.label(12, false, us))
        assertEquals("6 PM", AlertHours.label(18, false, us))
        assertEquals("10 PM", AlertHours.label(22, false, us))
        // Midnight again at the strip's end.
        assertEquals("12 AM", AlertHours.label(24, false, us))
        // In a sentence it needs no minutes: the marker already says it is an hour.
        assertEquals("3 PM", AlertHours.clock(15, false, us))
    }

    @Test
    fun `a window says its half of the day once when both ends share it`() {
        assertEquals("3" to "5 PM", AlertHours.span12(15, 17, us))
        assertEquals("6" to "11 PM", AlertHours.span12(18, 23, us))
        // The morning summary's window ends at noon, in the other half.
        assertEquals("6 AM" to "12 PM", AlertHours.span12(6, 12, us))
    }
}
