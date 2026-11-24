package com.callbackdev.chiaro.ui.today

import android.content.Context
import android.view.ViewGroup
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
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.domain.settings.UnitSettings
import com.callbackdev.chiaro.readme.MilanRecording
import com.callbackdev.chiaro.ui.shell.LocalPageFoot
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The foot of Today (24 nov 2026, the committente from his phone: «uno spazio di troppo, o una
 * riga vuota, in fondo»). On the README's recorded Milan, with the system's navigation bar
 * given a height as a phone gives it: what is left under the page's last line.
 */
@RunWith(RobolectricTestRunner::class)
class TodayFootTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val app: Context = ApplicationProvider.getApplicationContext()

    /** The page with [pageFoot] as the shell gives it and a system's navigation bar
     * [navigationPx] tall; the px between the bottom of the last line and the page's. */
    private fun underTheFoot(pageFoot: Dp, navigationPx: Int): Float {
        val state = MilanRecording.today(app, at = MilanRecording.now)
        compose.setContent {
            ChiaroTheme(dynamicColor = false) {
                CompositionLocalProvider(LocalPageFoot provides pageFoot) {
                    TodayPage(
                        state = state,
                        title = state.city.name,
                        isGps = false,
                        dots = null,
                        units = UnitSettings(),
                        locating = false,
                        guideCardVisible = false,
                        onRefresh = {},
                        onOpenPlaces = {},
                        onOpenSettings = {},
                        onOpenGuide = {},
                        onDismissGuideCard = {},
                        isCurrent = true,
                        onCanvasBehindBar = {}
                    )
                }
            }
        }
        compose.runOnUiThread {
            val content = compose.activity.findViewById<ViewGroup>(android.R.id.content)
            val insets = WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, navigationPx))
                .build()
            ViewCompat.dispatchApplyWindowInsets(content.getChildAt(0), insets)
        }
        compose.waitForIdle()
        val foot = hasText("Open-Meteo", substring = true)
        val list = compose.onAllNodes(hasScrollToIndexAction()).onFirst()
        list.performScrollToNode(foot)
        repeat(3) { list.performTouchInput { swipeUp() } }
        compose.waitForIdle()
        val line = compose.onNode(foot).fetchSemanticsNode().boundsInRoot
        val page = compose.onRoot().fetchSemanticsNode().boundsInRoot
        return page.bottom - line.bottom
    }

    @Test
    fun `under the last line there is the page's margin, whatever the system's bar`() {
        val density = app.resources.displayMetrics.density
        val margin = 16 * density
        assertEquals("with no navigation bar", margin, underTheFoot(0.dp, 0), 1f)
    }

    @Test
    fun `over the shell's bar the system's navigation bar is not counted again`() {
        val density = app.resources.displayMetrics.density
        val margin = 16 * density
        // The shell has already left the bar's height, the system's inset included, free
        // under the page: 48 px of it again under the last line was a blank line and more.
        assertEquals(margin, underTheFoot(0.dp, 48), 1f)
    }

    @Test
    fun `beside a rail the page reaches the window's foot and keeps clear of the system's bar`() {
        val density = app.resources.displayMetrics.density
        val margin = 16 * density
        assertEquals(margin + 48 * density, underTheFoot(48.dp, 48), 1f)
    }
}
