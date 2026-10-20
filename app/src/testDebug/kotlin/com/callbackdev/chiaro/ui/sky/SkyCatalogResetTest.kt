package com.callbackdev.chiaro.ui.sky

import android.content.Context
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.data.SkySubscription
import com.callbackdev.chiaro.domain.sky.SkyJobCatalog
import com.callbackdev.chiaro.readme.MilanRecording
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The catalog's way back to the four defaults (committente, 20 ott 2026): offered only while
 * the list differs from a fresh install's, and undone in place while the sheet is open.
 */
@RunWith(RobolectricTestRunner::class)
class SkyCatalogResetTest {

    @get:Rule
    val compose = createComposeRule()

    private fun string(id: Int, vararg args: Any): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id, *args)

    private fun show(state: SkyUiState.Content, onSet: (List<SkySubscription>) -> Unit) {
        compose.setContent {
            ChiaroTheme(dynamicColor = false) {
                SkyScreen(
                    state = state,
                    actions = SkyActions(
                        addMoment = {},
                        removeMoment = {},
                        setLead = { _, _ -> },
                        setMoments = onSet
                    ),
                    onOpenPlaces = {},
                    onOpenSettings = {},
                    onOpenGuide = {}
                )
            }
        }
        compose.onNodeWithText(string(R.string.sky_add_moment), useUnmergedTree = true).performClick()
        compose.waitForIdle()
    }

    @Test
    fun `a list touched by hand has a way back, and the way back has its undo`() {
        val base = MilanRecording.sky()
        val touched = base.subscriptions + SkySubscription("meteor.perseids.peak", notifyLeadMinutes = 60)
        assertTrue(differsFromDefaults(touched))
        val set = mutableListOf<List<SkySubscription>>()
        show(base.copy(subscriptions = touched, subscribedIds = touched.map { it.jobId }.toSet()), onSet = { set += it })
        val reset = string(R.string.sky_catalog_reset)
        compose.onAllNodes(hasScrollToNodeAction()).onLast().performScrollToNode(hasText(reset))
        compose.onNodeWithText(reset).performClick()
        compose.waitForIdle()
        assertEquals(listOf(SkyJobCatalog.defaults.map { SkySubscription(it.id) }), set)
        compose.onNodeWithText(string(R.string.sky_catalog_reset_done)).assertExists()
        compose.onNodeWithText(string(R.string.action_undo)).performClick()
        compose.waitForIdle()
        assertEquals(touched, set.last())
        compose.onNodeWithText(string(R.string.sky_catalog_reset_done)).assertDoesNotExist()
    }

    @Test
    fun `a fresh install's list is not offered a way back it does not need`() {
        val base = MilanRecording.sky()
        assertFalse(differsFromDefaults(base.subscriptions))
        show(base, onSet = {})
        val list = compose.onAllNodes(hasScrollToNodeAction()).onLast()
        // The last group's last row is reachable; the reset row is not there to scroll to.
        val last = SkyJobCatalog.meteorShowers.last()
        list.performScrollToNode(hasText(string(SkyText.nameRes(last.id))))
        compose.onNodeWithText(string(R.string.sky_catalog_reset)).assertDoesNotExist()
    }
}
