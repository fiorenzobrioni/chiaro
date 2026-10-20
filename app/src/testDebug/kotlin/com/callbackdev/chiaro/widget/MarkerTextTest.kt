package com.callbackdev.chiaro.widget

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.readme.MilanRecording
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The cards' marker line says the move as well as the age (20 ott 2026): a card on the
 * position that stays on the place it has weather for must not let the pin beside the name
 * say «you are here» in silence, fresh numbers or old.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "it-rIT")
class MarkerTextTest {

    private val app: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `fresh numbers say nothing, until the reader is elsewhere`() {
        val fresh = MilanRecording.today(app)
        assertFalse(fresh.isStale)
        assertFalse(fresh.marked)

        val moved = fresh.copy(elsewhereKm = 17)
        assertTrue(moved.marked)
        assertEquals("Sei altrove", markerText(app, moved, MilanRecording.now))
    }

    @Test
    fun `old numbers elsewhere say both, the move first and short`() {
        val old = MilanRecording.today(app, MilanRecording.night)
        assertTrue(old.isStale)
        val hours = java.time.Duration.between(MilanRecording.fetchedAt, MilanRecording.night).toHours()

        assertEquals("Aggiornato $hours ore fa", markerText(app, old, MilanRecording.night))
        assertEquals("Altrove · $hours h fa", markerText(app, old.copy(elsewhereKm = 17), MilanRecording.night))
    }
}
