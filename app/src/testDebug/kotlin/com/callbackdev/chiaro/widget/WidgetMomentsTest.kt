package com.callbackdev.chiaro.widget

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.readme.MilanRecording
import java.time.Duration
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The moments every sky card reads, with old data (21 ott 2026, from the device: six hours
 * offline, «Not sure yet» on the card). A verdict the engine refused for the data's age
 * does not reach a card; the moments and their clocks still do.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rGB")
class WidgetMomentsTest {

    private val app: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun `with old data the moments come without a verdict, with fresh data with one`() {
        val fresh = MilanRecording.widget(app, WidgetKind.SKY).moments
        assertTrue("a fresh recording judges: $fresh", fresh.any { it.verdict?.isKnown == true })

        val stale = MilanRecording.widget(app, WidgetKind.SKY, MilanRecording.now.plus(Duration.ofHours(6))).moments
        assertTrue(stale.isNotEmpty())
        assertTrue("no verdict from old data: $stale", stale.all { it.verdict == null })
    }
}
