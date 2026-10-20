package com.callbackdev.chiaro.widget.window

import android.app.Application
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextClock
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.readme.MilanRecording
import com.callbackdev.chiaro.widget.WidgetKind
import com.callbackdev.chiaro.widget.WidgetModel
import com.callbackdev.chiaro.widget.widgetSchemes
import java.time.Instant
import java.time.ZoneId
import java.util.TimeZone
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The window's live clock (20 ott 2026): the place's own time, the platform's TextClock in the
 * place's zone, only when that zone keeps a different time from the phone's.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rGB-w384dp-h832dp-xhdpi")
class WindowClockTest {

    private val app: Application = ApplicationProvider.getApplicationContext()

    /** The card composed as the launcher receives it, applied to a view, its clocks collected. */
    @OptIn(ExperimentalGlanceRemoteViewsApi::class)
    private fun clocks(model: WidgetModel): List<TextClock> {
        val composed = runBlocking {
            GlanceRemoteViews().compose(app, DpSize(340.dp, 189.dp)) {
                WindowWidgetContent(model, widgetSchemes(app, model.settings.dynamicColor, model.settings.palette), null)
            }
        }
        val view = composed.remoteViews.apply(app, FrameLayout(app))
        val found = ArrayList<TextClock>()
        fun walk(v: View) {
            if (v is TextClock) found += v
            if (v is ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i))
        }
        walk(view)
        return found
    }

    private fun <T> inRome(block: () -> T): T {
        val before = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Rome"))
        try {
            return block()
        } finally {
            TimeZone.setDefault(before)
        }
    }

    @Test
    fun `a place that keeps another time carries a live clock in its zone, the phone's place does not`() = inRome {
        val milan = MilanRecording.widget(app, WidgetKind.WINDOW)
        assertTrue("Milan on a phone in Rome: no clock", clocks(milan).isEmpty())
        val taipei = milan.copy(zone = ZoneId.of("Asia/Taipei"))
        val clock = clocks(taipei).single()
        assertEquals("Asia/Taipei", clock.timeZone)
    }

    @Test
    fun `the clock is decided by the offset, not by the zone's name`() = inRome {
        val at = Instant.parse("2026-10-20T13:00:00Z")
        assertFalse(clockDiffers(ZoneId.of("Europe/Paris"), at))
        assertFalse(clockDiffers(ZoneId.of("Europe/Rome"), at))
        assertTrue(clockDiffers(ZoneId.of("Asia/Taipei"), at))
        assertTrue(clockDiffers(ZoneId.of("Europe/London"), at))
    }
}
