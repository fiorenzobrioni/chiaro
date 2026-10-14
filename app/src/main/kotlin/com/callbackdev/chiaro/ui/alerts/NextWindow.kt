package com.callbackdev.chiaro.ui.alerts

import java.time.LocalTime

/** When the window [NextWindow.index] names stands from now: open now, later today, tomorrow. */
internal enum class NextWhen { NOW, TODAY, TOMORROW }

internal data class NextWindow(val index: Int, val whenever: NextWhen)

/**
 * The window the head of Alerts names (the Alerts review, point 7, 14 ott 2026), from the
 * day's timed windows as whole hours ([hours], `from` to `to`, in any order): the one open
 * now, else the next to open today, else the first of tomorrow. Null when none is on.
 *
 * A window and not an alert: the summaries come once inside theirs, and the screen does not
 * know whether today's has already come, so it names the hours the phone may ring, which it
 * does know, and never a promise that it will.
 */
internal fun nextWindow(hours: List<Pair<Int, Int>>, now: LocalTime): NextWindow? {
    if (hours.isEmpty()) return null
    val order = hours.indices.sortedBy { hours[it].first }
    val minute = now.hour * 60 + now.minute
    order.firstOrNull { minute < hours[it].second * 60 }?.let { i ->
        val open = minute >= hours[i].first * 60
        return NextWindow(i, if (open) NextWhen.NOW else NextWhen.TODAY)
    }
    return NextWindow(order.first(), NextWhen.TOMORROW)
}
