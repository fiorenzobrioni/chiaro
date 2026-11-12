package com.callbackdev.chiaro.ui.alerts

import android.content.res.Resources
import com.callbackdev.chiaro.R
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The whole hours of Alerts' windows, as the phone's clock says them (the Alerts review, 12
 * nov 2026). The page printed «6–12», «15–17», «18–23», the strip's 0 · 6 · 12 · 18 · 24 and
 * «Dalle 22 alle 7» as hours of a twenty-four-hour day whatever the phone's setting, beside a
 * bulletin «di ieri alle 2:34 PM» and a rule's own hours, which followed it: one page on two
 * clocks.
 *
 * On a twenty-four-hour clock nothing changes, to the character, but one Italian sentence:
 * the bulletin's switch said «tra le 15 e le 17» where English said «between 15:00 and
 * 17:00», and now both are given the hour as a clock time ([clock]). On a twelve-hour one an
 * hour is «6 PM», in the pattern the hour strip uses for its own (`Formats.hourLabel`), and a
 * window says its period once when both ends share it: «3–5 PM», «6–11 PM», but «6 AM–12 PM».
 * The marker is the locale's, never a string of this app.
 */
internal object AlertHours {

    /** One whole hour, bare: «18» or «6 PM». 24 is the day's end, midnight again. */
    fun label(hour: Int, is24h: Boolean, locale: Locale): String =
        if (is24h) hour.toString() else twelve(hour, locale)

    /** One whole hour as a clock time, for a sentence: «18:00» or «6 PM». A twelve-hour
     * clock already says it is an hour with its marker, so only the other gains minutes. */
    fun clock(hour: Int, is24h: Boolean, locale: Locale): String =
        if (is24h) String.format(locale, "%d:00", hour) else twelve(hour, locale)

    /**
     * The two ends of a window on a twelve-hour clock: «3» and «5 PM» when both are in one
     * half of the day, «6 AM» and «12 PM» when they are not.
     */
    fun span12(from: Int, to: Int, locale: Locale): Pair<String, String> {
        val onePeriod = (from % 24 < 12) == (to % 24 < 12)
        val start = if (onePeriod) {
            LocalTime.of(from % 24, 0).format(DateTimeFormatter.ofPattern("h", locale))
        } else {
            twelve(from, locale)
        }
        return start to twelve(to, locale)
    }

    /** A window as the page prints it: «15–17», or «3–5 PM». */
    fun range(res: Resources, from: Int, to: Int, is24h: Boolean, locale: Locale): String =
        if (is24h) {
            res.getString(R.string.alerts_hours_range, from, to)
        } else {
            val (start, end) = span12(from, to, locale)
            res.getString(R.string.alerts_hours_span, start, end)
        }

    private fun twelve(hour: Int, locale: Locale): String =
        LocalTime.of(hour % 24, 0).format(DateTimeFormatter.ofPattern("h a", locale))
}
