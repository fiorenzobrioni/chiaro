package com.callbackdev.chiaro.ui.sky

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.data.SkySubscription
import com.callbackdev.chiaro.domain.sky.SkyJobCatalog
import com.callbackdev.chiaro.readme.MilanRecording
import com.callbackdev.chiaro.ui.format.LocalClock
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import java.time.Clock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * «See that night's sky» under the rows of the calendar ahead (Fase 16, step 16.9): under
 * the events that have a night of their own and under no other, and what the link asks the
 * shell for. A tall window, so the whole page is composed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rGB-w384dp-h2600dp-xhdpi")
class SkyEventNightLinkTest {

    @get:Rule
    val compose = createComposeRule()

    private val app = ApplicationProvider.getApplicationContext<Context>()
    private val asked = mutableListOf<EventNight>()

    private fun show(): SkyUiState.Content {
        val state = SkyStateBuilder.build(
            city = MilanRecording.city,
            report = MilanRecording.report,
            subscriptions = SkyJobCatalog.defaults.map { SkySubscription(it.id) },
            settings = MilanRecording.settings,
            now = MilanRecording.now
        ) as SkyUiState.Content
        compose.setContent {
            ChiaroTheme(dynamicColor = false) {
                CompositionLocalProvider(LocalClock provides Clock.fixed(MilanRecording.now, state.zone)) {
                    SkyScreen(
                        state = state,
                        actions = SkyActions({}, {}, { _, _ -> }, {}),
                        onOpenPlaces = {},
                        onOpenSettings = {},
                        onOpenGuide = {},
                        onExploreNight = { asked += it }
                    )
                }
            }
        }
        compose.waitForIdle()
        return state
    }

    @Test
    fun `the link stands under the events that have a night, and under no other`() {
        val state = show()
        val withNight = state.events.filter { SkyEventNights.dateOf(it, state.zone) != null }
        assertTrue("the recorded calendar has both kinds: ${state.events.map { it.job.id }}", withNight.isNotEmpty() && withNight.size < state.events.size)
        val links = compose.onAllNodes(hasTestTag(EventNightTag))
        assertEquals(withNight.size, links.fetchSemanticsNodes().size)
        compose.onAllNodes(hasText(app.getString(R.string.sky_event_night))).onFirst().assertHasClickAction()
    }

    @Test
    fun `the link asks for the explorer on that event's night, on its thing`() {
        val state = show()
        val first = state.events.first { SkyEventNights.dateOf(it, state.zone) != null }
        compose.onAllNodes(hasTestTag(EventNightTag)).onFirst().performClick()
        compose.waitForIdle()
        val expected = SkyEventNights.of(first, state.zone, state.coordinates!!, state.subscribedIds)!!
        assertEquals(listOf(expected), asked)
        assertEquals(SkyEventNights.dateOf(first, state.zone), expected.date)
        assertTrue("a thing to turn to: ${expected.pick}", expected.pick != null && expected.facingDeg != null)
    }
}
