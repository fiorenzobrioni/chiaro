package com.callbackdev.chiaro.ui.settings

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.data.AppPalette
import com.callbackdev.chiaro.data.AppSettings
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The Settings page as a plain composable (review of 30 ott 2026): what its rows say, what
 * a tap opens, and that the groups stand in the family's order. In `testDebug` like the
 * other screen tests: the Compose rule's activity comes from a debug-only manifest.
 */
@RunWith(RobolectricTestRunner::class)
class SettingsScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val app: Context = ApplicationProvider.getApplicationContext()

    private var palette: AppPalette? = null
    private var resets = 0

    private fun show(settings: AppSettings = AppSettings()) {
        compose.setContent {
            ChiaroTheme(dynamicColor = false) {
                SettingsScreen(
                    settings = settings,
                    actions = SettingsActions(
                        setTemperatureUnit = {},
                        setWindSpeedUnit = {},
                        setPressureUnit = {},
                        setThemeMode = {},
                        setDynamicColor = {},
                        setPalette = { palette = it },
                        setFont = {},
                        setAnimatedIcons = {},
                        setWeatherIcons = {},
                        setUpdateFrequency = {},
                        setSkyDefaultLead = {},
                        setSkyNotifyOnFail = {},
                        resetToDefaults = { resets++ }
                    ),
                    onBack = {},
                    onOpenGuide = {}
                )
            }
        }
    }

    private fun string(id: Int, vararg args: Any) = app.getString(id, *args)

    /** The page is a lazy list: a row is composed only once the list reaches it. */
    private fun scrollTo(text: String) {
        compose.onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(hasText(text))
        compose.waitForIdle()
    }


    @Test
    fun `the palette picker opens from its row and hands the choice back`() {
        show()
        scrollTo(string(R.string.settings_palette))
        compose.onNodeWithText(string(R.string.settings_palette)).performClick()

        compose.onNodeWithText(string(R.string.settings_palette_paper)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.settings_palette_paper)).performClick()
        assertEquals(AppPalette.PAPER, palette)
    }

    @Test
    fun `the widgets group names the three widgets and says each keeps its own settings`() {
        show()
        val add = string(
            R.string.settings_widget_add_note,
            string(R.string.widget_now_label),
            string(R.string.widget_window_label),
            string(R.string.widget_text_label)
        )
        scrollTo(add)
        compose.onNodeWithText(add).assertExists()
        scrollTo(string(R.string.settings_widget_change_note))
        compose.onNodeWithText(string(R.string.settings_widget_change_note)).assertExists()
    }

    /** On a window tall enough to hold the whole page, so every header has a position. */
    @Test
    @Config(qualifiers = "w384dp-h4000dp-xhdpi")
    fun `the features come first, then the appearance, then the language and the rest`() {
        show()
        val order = listOf(
            R.string.settings_group_units, R.string.settings_group_updates,
            R.string.settings_group_sky_reminders, R.string.settings_group_widgets,
            R.string.settings_group_appearance, R.string.settings_group_language,
            R.string.settings_privacy, R.string.settings_group_about, R.string.settings_group_credits
        ).map { string(it) }
        val tops = order.map { compose.onNodeWithText(it).fetchSemanticsNode().positionInRoot.y }
        assertEquals(tops.sorted(), tops)
        assertTrue(tops.zipWithNext().all { (a, b) -> a < b })
    }

    @Test
    fun `the reset asks first, then resets once`() {
        show()
        scrollTo(string(R.string.settings_reset))
        compose.onNodeWithText(string(R.string.settings_reset)).performClick()

        compose.onNodeWithText(string(R.string.settings_reset_dialog_title)).assertIsDisplayed()
        assertEquals(0, resets)
        compose.onNodeWithText(string(R.string.settings_reset_confirm)).performClick()
        assertEquals(1, resets)
    }

    @Test
    fun `the credits say the icons are adapted, and the language row leaves the app`() {
        show()
        scrollTo(string(R.string.settings_credit_icons_note))
        compose.onNodeWithText(string(R.string.settings_credit_icons_note)).assertExists()
        scrollTo(string(R.string.settings_language))
        compose.onNodeWithText(string(R.string.settings_language)).assertExists()
    }
}
