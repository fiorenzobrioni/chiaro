package com.callbackdev.chiaro.widget

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.callbackdev.chiaro.ui.theme.WidgetCardColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** The per-widget looks across a backup restore, which hands every widget a new id. */
class WidgetLookStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun store() = WidgetLookStore(
        PreferenceDataStoreFactory.create(scope = scope) {
            tmp.newFile("widget-look-${System.nanoTime()}.preferences_pb")
        }
    )

    @After
    fun tearDown() {
        scope.cancel()
    }

    private val green = WidgetLook(background = WidgetBackground.COLOR, cardColor = WidgetCardColor.GREEN)
    private val dark = WidgetLook(background = WidgetBackground.DARK, opacityPct = 40, showSentence = false)

    @Test
    fun `a restore moves each look to its widget's new id, overlaps included`() = runBlocking {
        val store = store()
        store.set(1, green)
        store.set(2, dark)

        store.remap(oldIds = intArrayOf(1, 2), newIds = intArrayOf(2, 3))

        assertEquals(green, store.lookFor(2, WidgetKind.NOW))
        assertEquals(dark, store.lookFor(3, WidgetKind.NOW))
        // The old number keeps nothing: a widget handed it later starts from the defaults.
        assertEquals(WidgetLook.defaultsFor(WidgetKind.NOW), store.lookFor(1, WidgetKind.NOW))
    }

    /** The window's two switches (20 ott 2026) are a look's like the others: kept, moved, forgotten. */
    @Test
    fun `the window's switches persist and move with the look`() = runBlocking {
        val store = store()
        val window = WidgetLook(windowWeather = true, windowBareSky = true)
        store.set(5, window)
        assertEquals(window, store.lookFor(5, WidgetKind.WINDOW))
        store.remap(oldIds = intArrayOf(5), newIds = intArrayOf(6))
        assertEquals(window, store.lookFor(6, WidgetKind.WINDOW))
        assertEquals(WidgetLook.defaultsFor(WidgetKind.WINDOW), store.lookFor(5, WidgetKind.WINDOW))
        store.forget(intArrayOf(6))
        assertEquals(WidgetLook.defaultsFor(WidgetKind.WINDOW), store.lookFor(6, WidgetKind.WINDOW))
    }
}
