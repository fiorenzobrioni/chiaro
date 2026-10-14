package com.callbackdev.chiaro.ui.sky

import android.content.Context
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.data.SkySubscription
import com.callbackdev.chiaro.domain.sky.SkyJobCatalog
import com.callbackdev.chiaro.readme.MilanRecording
import com.callbackdev.chiaro.ui.components.TabHeader
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The last two points of the Sky screen's review (14 ott 2026): «Annulla» after a removal
 * puts the line back as it was, its own lead included (point 9), and the place switcher in
 * the tabs' header is a 48 dp target (point 10).
 */
@RunWith(RobolectricTestRunner::class)
class SkyUndoTest {

    @get:Rule
    val compose = createComposeRule()

    private fun string(id: Int, vararg args: Any): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id, *args)

    @Test
    fun `undo after a removal from a page puts the line back with its lead`() {
        val sunset = SkyJobCatalog.SunSet.id
        val base = MilanRecording.sky()
        val state = base.copy(
            subscriptions = base.subscriptions.map {
                if (it.jobId == sunset) it.copy(notifyLeadMinutes = 30) else it
            }
        )
        val removed = mutableListOf<String>()
        val restored = mutableListOf<SkySubscription>()
        compose.setContent {
            ChiaroTheme(dynamicColor = false) {
                SkyScreen(
                    state = state,
                    actions = SkyActions(
                        addMoment = {},
                        removeMoment = { removed += it },
                        setLead = { _, _ -> },
                        restoreMoment = { restored += it }
                    ),
                    onOpenPlaces = {},
                    onOpenSettings = {},
                    onOpenGuide = {}
                )
            }
        }
        val name = string(SkyText.nameRes(sunset))
        // A lazy list composes what is on screen: scroll the list to the row first.
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(name))
        compose.onNodeWithText(name).performClick()
        compose.onNodeWithText(string(R.string.sky_guide_remove_action)).performScrollTo().performClick()
        assertEquals(listOf(sunset), removed)

        compose.onNodeWithText(string(R.string.sky_moment_removed, name)).assertExists()
        compose.onNodeWithText(string(R.string.action_undo)).performClick()
        compose.waitForIdle()
        assertEquals(listOf(SkySubscription(sunset, enabled = true, notifyLeadMinutes = 30)), restored)
    }

    @Test
    fun `the place switcher is one target of at least 48 dp`() {
        compose.setContent {
            ChiaroTheme(dynamicColor = false) {
                TabHeader(title = "Sky", placeName = "Bra", onOpenPlaces = {}, onOpenSettings = {})
            }
        }
        compose.onNode(hasClickAction() and hasText("Bra"))
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
    }

    @Test
    fun `with no place the title is not a button`() {
        compose.setContent {
            ChiaroTheme(dynamicColor = false) {
                TabHeader(title = "Sky", placeName = null, onOpenPlaces = {}, onOpenSettings = {})
            }
        }
        assertTrue(compose.onAllNodes(hasClickAction() and hasText("Sky")).fetchSemanticsNodes().isEmpty())
    }
}
