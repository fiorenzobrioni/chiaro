package com.callbackdev.chiaro.ui.sky

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.data.SkySubscription
import com.callbackdev.chiaro.domain.sky.SkyJobCatalog
import com.callbackdev.chiaro.readme.MilanRecording
import com.callbackdev.chiaro.ui.format.LocalClock
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import java.time.Clock
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The verdicts refused for the data's age (review of the Sky screen, 22 ott 2026): the rows
 * keep the moment and its time and carry no «?», and each section says the reason once,
 * with the data's real age. A tall window, so the whole page is composed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rGB-w384dp-h2600dp-xhdpi")
class SkyStaleVerdictsTest {

    @get:Rule
    val compose = createComposeRule()

    private val app = ApplicationProvider.getApplicationContext<Context>()

    private fun show(at: Instant) {
        val state = SkyStateBuilder.build(
            city = MilanRecording.city,
            report = MilanRecording.report,
            subscriptions = SkyJobCatalog.defaults.map { SkySubscription(it.id) },
            settings = MilanRecording.settings,
            now = at
        )
        compose.setContent {
            ChiaroTheme(dynamicColor = false) {
                CompositionLocalProvider(LocalClock provides Clock.fixed(at, state.zone)) {
                    SkyScreen(
                        state = state,
                        actions = SkyActions({}, {}, { _, _ -> }, {}),
                        onOpenPlaces = {},
                        onOpenSettings = {},
                        onOpenGuide = {}
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    private fun count(text: String, substring: Boolean = false): Int =
        compose.onAllNodes(hasText(text, substring = substring), useUnmergedTree = true).fetchSemanticsNodes().size

    @Test
    fun `with an old forecast no row says «not sure yet», and each section says the age once`() {
        // The recording's evening: the forecast of 12:10 is 8 h 50 old, past the two hours
        // the default interval allows, so every verdict is refused for its age.
        show(MilanRecording.night)
        assertEquals("no «?» chip on a row", 0, count(app.getString(R.string.verdict_unknown), substring = true))
        assertEquals("no «too old» on a row", 0, count(app.getString(R.string.sky_unknown_stale), substring = true))
        val note = app.getString(
            R.string.sky_verdicts_stale_note, app.resources.getQuantityString(R.plurals.freshness_hours_ago, 8, 8)
        )
        assertEquals("once under the moments, once under the calendar: $note", 2, count(note))
        // The moments and their times are still there: the phone computes them on its own.
        assertTrue(count(app.getString(R.string.sky_name_sun_rise)) >= 1)
        assertTrue(count("07:14") >= 1)
    }

    @Test
    fun `with a fresh forecast the chips are there and no section says the age`() {
        show(MilanRecording.now)
        assertTrue(count(app.getString(R.string.verdict_pass), substring = true) >= 1)
        assertEquals(0, count(": the verdicts wait", substring = true))
        assertEquals(0, count(app.getString(R.string.sky_verdicts_no_data_note)))
    }
}
