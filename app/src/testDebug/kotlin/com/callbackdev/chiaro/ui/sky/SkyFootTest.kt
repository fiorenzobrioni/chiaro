package com.callbackdev.chiaro.ui.sky

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.readme.MilanRecording
import com.callbackdev.chiaro.ui.format.LocalClock
import com.callbackdev.chiaro.ui.shell.LocalPageFoot
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import java.time.Clock
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The foot of the Sky page (24 nov 2026, after Today's), on the README's recorded Milan: what
 * is left under the page's last row, the entry to its guide. Over the shell's bar it was
 * right, the page's margin and no more; beside a rail, where the page reaches the window's
 * foot, the row stood under the system's bar.
 */
@RunWith(RobolectricTestRunner::class)
class SkyFootTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val app: Context = ApplicationProvider.getApplicationContext()
    private val state = MilanRecording.sky()

    /** The page with [pageFoot] as the shell gives it; the px between the bottom of the last
     * row's words and the page's. */
    private fun underTheFoot(pageFoot: Dp): Float {
        compose.setContent {
            ChiaroTheme(dynamicColor = false) {
                CompositionLocalProvider(
                    LocalClock provides Clock.fixed(MilanRecording.now, state.zone),
                    LocalPageFoot provides pageFoot
                ) {
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
        val foot = hasText(app.getString(R.string.sky_guide_entry_subtitle))
        val list = compose.onAllNodes(hasScrollToIndexAction()).onFirst()
        list.performScrollToNode(foot)
        repeat(3) { list.performTouchInput { swipeUp() } }
        compose.waitForIdle()
        val line = compose.onNode(foot, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val page = compose.onRoot().fetchSemanticsNode().boundsInRoot
        return page.bottom - line.bottom
    }

    private val density get() = app.resources.displayMetrics.density

    /** The page's 16dp and the row's own 8dp under its words. */
    private val margin get() = 24 * density

    @Test
    fun `over the shell's bar the last row stands on the page's margin`() {
        assertEquals(margin, underTheFoot(0.dp), 1f)
    }

    @Test
    fun `beside a rail the last row keeps clear of the system's bar`() {
        assertEquals(margin + 48 * density, underTheFoot(48.dp), 1f)
    }
}
