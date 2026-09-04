package com.callbackdev.chiaro.widget

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * The tick the widgets' compositions listen to when something they draw has changed
 * (Fase 8, device report of 4 set — a manual refresh, a morning sync and a just-confirmed
 * look all left the home screen showing the previous answer).
 *
 * Glance runs `provideGlance` **once per session** and then keeps the composition alive
 * for about forty-five seconds. Inside that window `update()` does not start the function
 * again: it only wakes the composition that is already there, so anything read BEFORE
 * `provideContent` — which for these widgets is the whole model — stays exactly as it was
 * when the session opened. AndroidX says so in `GlanceAppWidget.provideGlance`'s own
 * documentation: *observe your sources of data within the composition*. That window is
 * also why the symptom looked intermittent: outside it the session had already timed out,
 * `update()` started a new one, and the widget repainted correctly.
 *
 * A plain in-process counter is enough, and deliberately not a persisted one: a session
 * cannot outlive the process that runs it, so when the process dies the stale composition
 * dies with it and the next `update()` reads everything again from scratch.
 */
object WidgetRefresh {

    private val ticks = MutableStateFlow(0L)

    /** Read by [rememberWidgetModel] inside every widget's composition. */
    val revision: StateFlow<Long> = ticks.asStateFlow()

    /**
     * Something a widget draws has changed. Called from [ChiaroWidgets] right before the
     * repaint, so both roads are covered by one call: a live session reloads its model,
     * and a session that has to be started reads the new data on its way up.
     */
    fun invalidate() {
        ticks.update { it + 1 }
    }
}
