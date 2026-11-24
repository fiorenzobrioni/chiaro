package com.callbackdev.chiaro.ui.alerts

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.readme.MilanRecording
import com.callbackdev.chiaro.ui.shell.LocalPageFoot
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The foot of the Alerts page (24 nov 2026, with Today's and the Sky's), on the README's
 * recorded Milan: what is left under the page's lowest words, scrolled to its end. Over the
 * shell's bar it is the page's margin; beside a rail, where the page reaches the window's
 * foot, it keeps clear of the system's bar too.
 */
@RunWith(RobolectricTestRunner::class)
class AlertsFootTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val app: Context = ApplicationProvider.getApplicationContext()

    /** The px between the page's lowest words and its bottom, scrolled to the end. */
    private fun underTheFoot(): Float {
        compose.waitForIdle()
        val list = compose.onAllNodes(hasScrollToIndexAction()).onFirst()
        repeat(12) { list.performTouchInput { swipeUp() } }
        compose.waitForIdle()
        val words = hasAnyAncestor(hasScrollToIndexAction()) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Text)
        val lowest = compose.onAllNodes(words, useUnmergedTree = true).fetchSemanticsNodes().maxOf { it.boundsInRoot.bottom }
        return compose.onRoot().fetchSemanticsNode().boundsInRoot.bottom - lowest
    }

    @Test
    fun `the page ends on its margin, and beside a rail keeps clear of the system's bar`() {
        var pageFoot by mutableStateOf(0.dp)
        compose.setContent {
            ChiaroTheme(dynamicColor = false) {
                CompositionLocalProvider(LocalPageFoot provides pageFoot) {
                    AlertsScreen(
                        state = MilanRecording.alerts(app),
                        actions = AlertsActions({}, {}, {}, {}, {}, {}, {}, { _, _ -> }),
                        onEdit = {},
                        onOpenPlaces = {},
                        onOpenSettings = {}
                    )
                }
            }
        }
        val density = app.resources.displayMetrics.density
        val overTheBar = underTheFoot()
        assertTrue(
            "over the shell's bar the page ends on its margin and a row's own air: $overTheBar px",
            overTheBar in 16f * density..40f * density
        )
        pageFoot = 48.dp
        assertEquals(overTheBar + 48 * density, underTheFoot(), 1f)
    }
}
