package com.callbackdev.chiaro.ui.sky

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performSemanticsAction
import androidx.core.view.WindowCompat
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.readme.MilanRecording
import com.callbackdev.chiaro.ui.format.LocalClock
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import com.callbackdev.chiaro.ui.today.StatusBarIcons
import java.time.Clock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The Sky page as Today (Fase 16.1, 10 nov 2026): the place and the night's date stand on the
 * sky and stay pinned over the list, the tab's own title is gone, the bar takes the verdict
 * once the hero's has scrolled under it, and the status bar's icons follow the page that is
 * on screen now that two pages draw a sky behind them.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rGB-w384dp-h832dp-xhdpi")
class SkyHeroPageTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val app: Context = ApplicationProvider.getApplicationContext()
    private val state = MilanRecording.sky()

    private fun show() {
        compose.setContent {
            ChiaroTheme(dynamicColor = false) {
                CompositionLocalProvider(LocalClock provides Clock.fixed(MilanRecording.now, state.zone)) {
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

    /** What the bar says of the night: the recording's is the moon's, a word and no verdict. */
    private val inBar = hasContentDescription(
        app.getString(R.string.sky_bar_tonight, app.getString(R.string.sky_tonight_headline_moon))
    )

    @Test
    fun `the place and the night's date are one target on the sky, and the tab's title is gone`() {
        show()
        // 25 September 2026, the recording's day, is a Friday.
        val place = compose.onNode(hasClickAction() and hasContentDescription("Milan, Tonight, Friday 25 September"))
        place.assertExists()
        val rest = place.fetchSemanticsNode().boundsInRoot
        val stage = compose.onNode(
            SemanticsMatcher("the stage") { node ->
                node.config.getOrNull(SemanticsProperties.ContentDescription)?.any { it.startsWith("Looking ") } == true
            }
        ).fetchSemanticsNode().boundsInRoot
        assertTrue("the row ($rest) stands on the stage ($stage)", rest.top >= stage.top && rest.bottom <= stage.bottom)
        compose.onAllNodes(hasText(app.getString(R.string.tab_sky))).assertCountEquals(0)
        // At rest the hero says the night; the bar does not say it twice.
        compose.onAllNodes(inBar).assertCountEquals(0)
    }

    @Test
    fun `the bar takes the night's word once the hero's has scrolled under it, and the place stays`() {
        show()
        compose.onNode(hasScrollToNodeAction()).performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 1400f) }
        compose.waitForIdle()
        compose.onNode(inBar).assertExists()
        compose.onNode(hasClickAction() and hasContentDescription("Milan, Tonight, Friday 25 September")).assertExists()
    }

    /** Today and Sky both stand a sky behind the status bar, and a tab switch has both on screen
     * for its length: the page that leaves must not hand the bar to the theme over the page that
     * arrived. In the light theme, where the theme's icons are dark. */
    @Test
    fun `the status bar follows the page that arrived last, and the theme when none is left`() {
        var today by mutableStateOf(true)
        var sky by mutableStateOf(false)
        compose.setContent {
            ChiaroTheme(darkTheme = false, dynamicColor = false) {
                if (today) StatusBarIcons(overCanvas = true)
                if (sky) StatusBarIcons(overCanvas = true)
            }
        }
        val window = compose.activity.window
        fun darkIcons() = WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars
        compose.waitForIdle()
        assertFalse("white icons over Today's sky", darkIcons())
        sky = true
        compose.waitForIdle()
        today = false
        compose.waitForIdle()
        assertFalse("still white: the Sky's canvas is behind the bar", darkIcons())
        sky = false
        compose.waitForIdle()
        assertEquals("the theme's own once no sky is behind it", true, darkIcons())
    }
}
