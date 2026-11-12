package com.callbackdev.chiaro.ui.firstrun

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.ui.places.GpsError
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The first run (VISION §5.8, 12 nov 2026): three pages in one frame, the welcome first; the
 * buttons that answer each; the way back from the place to the welcome; and no word alone at
 * the end of a centred line, measured with the app's face on a 360dp phone, in both languages.
 *
 * In `testDebug` for the reason the Sky's sheet tests give: the Compose rule's activity comes
 * from a debug-only manifest. Each page is saved as a PNG under `CHIARO_RENDER_DIR` when that
 * is set, to be looked at.
 */
@RunWith(RobolectricTestRunner::class)
// Real text measurement: Robolectric's default graphics measure a line as a few pixels
// whatever it says, and every text would fit on one line.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w360dp-h800dp-xhdpi")
class FirstRunScreenTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val app = ApplicationProvider.getApplicationContext<Context>()
    private val taps = mutableListOf<String>()

    private fun string(id: Int, vararg args: Any): String = app.getString(id, *args)

    // What the frame is drawn with: set once, then changed, since a rule draws once per test.
    private var page by mutableStateOf(FirstRunPage.WELCOME)
    private var place by mutableStateOf(PlaceAnswer())
    private var settingsOnly by mutableStateOf(false)
    private var dark by mutableStateOf(false)
    private var drawn = false

    /** The run as the route drives it: the welcome moves to the place and back; the rest is the store's. */
    private fun run(
        start: FirstRunPage = FirstRunPage.WELCOME,
        place: PlaceAnswer = PlaceAnswer(),
        notificationsSettingsOnly: Boolean = false,
        dark: Boolean = false
    ) {
        val set = {
            this.page = start
            this.place = place
            this.settingsOnly = notificationsSettingsOnly
            this.dark = dark
        }
        if (drawn) {
            compose.runOnIdle { set() }
        } else {
            set()
            drawn = true
            compose.setContent {
                ChiaroTheme(darkTheme = this.dark, dynamicColor = false) {
                    FirstRunScreen(
                        page = this.page,
                        place = this.place,
                        notificationsSettingsOnly = settingsOnly,
                        actions = FirstRunActions(
                            start = { this.page = FirstRunPage.PLACE },
                            back = { this.page = FirstRunPage.WELCOME },
                            useLocation = { taps += "location" },
                            search = { taps += "search" },
                            skipPlace = { taps += "skip" },
                            allowNotifications = { taps += "allow" },
                            notificationsLater = { taps += "later" }
                        )
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    /** The page's heading, which says where the run stands to a screen reader. */
    private fun assertHeading(text: String, step: Int) {
        compose.onNode(hasText(text) and isHeading()).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, string(R.string.first_run_step, step, 3))
        )
    }

    private fun primary() = compose.onNodeWithTag(FirstRunTags.PRIMARY, useUnmergedTree = false)

    @Test
    fun `the run opens on what Chiaro is, the name, its sentence and three promises`() {
        run()
        assertHeading(string(R.string.first_run_title), 1)
        listOf(
            R.string.first_run_tagline,
            R.string.first_run_reading_title, R.string.first_run_reading_body,
            R.string.first_run_sky_title, R.string.first_run_sky_body,
            R.string.first_run_free_title, R.string.first_run_free_body
        ).forEach { compose.onNodeWithText(string(it)).assertExists() }
        primary().assertTextEquals(string(R.string.first_run_start))
        // It asks nothing, so there is nothing to put off.
        compose.onAllNodesWithTag(FirstRunTags.LATER).assertCountEquals(0)
        compose.onAllNodesWithTag(FirstRunTags.SECONDARY).assertCountEquals(0)
        save("welcome")
    }

    @Test
    fun `Get started moves to the place, and back returns to the welcome`() {
        run()
        primary().performClick()
        compose.waitForIdle()

        assertHeading(string(R.string.first_run_place_title), 2)
        compose.onNodeWithText(string(R.string.first_run_place_body)).assertExists()
        primary().assertTextEquals(string(R.string.first_run_use_location))
        compose.onNodeWithText(string(R.string.first_run_location_why)).assertExists()
        compose.onNodeWithTag(FirstRunTags.SECONDARY).assertTextEquals(string(R.string.first_run_search))
        compose.onNodeWithTag(FirstRunTags.LATER).assertTextEquals(string(R.string.first_run_later))
        save("place")

        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        assertHeading(string(R.string.first_run_title), 1)
    }

    @Test
    fun `the place is answered by its buttons, and Later skips it`() {
        run(start = FirstRunPage.PLACE)
        primary().performClick()
        compose.onNodeWithTag(FirstRunTags.SECONDARY).performClick()
        compose.onNodeWithTag(FirstRunTags.LATER).performClick()
        assertEquals(listOf("location", "search", "skip"), taps)
    }

    @Test
    fun `a refusal is said under the page's words, with the way on, and for good it opens the settings`() {
        run(start = FirstRunPage.PLACE, place = PlaceAnswer(settingsOnly = true, error = GpsError.PERMISSION))
        compose.onNodeWithTag(FirstRunTags.LOCATION_ERROR).assertTextEquals(string(R.string.first_run_location_refused))
        primary().assertTextEquals(string(R.string.first_run_open_settings))
        save("place-refused")
    }

    @Test
    fun `while a fix is looked for, the button waits and says so`() {
        run(start = FirstRunPage.PLACE, place = PlaceAnswer(acquiring = true))
        primary().assertIsNotEnabled()
        compose.onNode(hasContentDescription(string(R.string.places_gps_acquiring)), useUnmergedTree = true).assertExists()
    }

    @Test
    fun `the notifications are the third step, asked in words, and Later answers them`() {
        run(start = FirstRunPage.NOTIFICATIONS)
        assertHeading(string(R.string.first_run_notify_title), 3)
        compose.onNodeWithText(string(R.string.first_run_notify_body)).assertExists()
        compose.onNodeWithText(string(R.string.first_run_notify_where)).assertExists()
        primary().assertTextEquals(string(R.string.notifications_off_allow))
        save("notifications")

        primary().performClick()
        compose.onNodeWithTag(FirstRunTags.LATER).performClick()
        assertEquals(listOf("allow", "later"), taps)
        // The notifications follow a place already answered: there is no way back to it here.
        compose.runOnUiThread { assertTrue(!compose.activity.onBackPressedDispatcher.hasEnabledCallbacks()) }
    }

    @Test
    fun `where only the settings can help, the notifications' button opens them`() {
        run(start = FirstRunPage.NOTIFICATIONS, notificationsSettingsOnly = true)
        primary().assertTextEquals(string(R.string.notifications_off_open_settings))
    }

    @Test
    fun `no word stands alone at the end of a line, in English`() = assertNoWordAlone()

    @Test
    @Config(qualifiers = "it-rIT-w360dp-h800dp-xhdpi")
    fun `no word stands alone at the end of a line, in Italian`() = assertNoWordAlone()

    @Test
    fun `the pages in the dark`() {
        // Only drawn, to be looked at: the colours are the theme's roles (NoRawColorTest).
        run(dark = true)
        save("welcome-dark")
        run(start = FirstRunPage.NOTIFICATIONS, dark = true)
        save("notifications-dark")
    }

    /**
     * Every text of every page laid out at a phone's narrowest common width: a text on more
     * than one line ends on a line of more than one word («…mostrare una / notifica.» was the
     * slip this guards).
     */
    private fun assertNoWordAlone() {
        val pages = listOf(
            FirstRunPage.WELCOME to PlaceAnswer(),
            FirstRunPage.PLACE to PlaceAnswer(error = GpsError.PERMISSION),
            FirstRunPage.NOTIFICATIONS to PlaceAnswer()
        )
        val alone = mutableListOf<String>()
        var measured = 0
        for ((page, place) in pages) {
            run(start = page, place = place)
            val nodes = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult), useUnmergedTree = true)
                .fetchSemanticsNodes()
            for (node in nodes) {
                val results = mutableListOf<TextLayoutResult>()
                node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
                val layout = results.firstOrNull() ?: continue
                if (layout.lineCount < 2) continue
                measured++
                val text = layout.layoutInput.text.text
                val last = text.substring(layout.getLineStart(layout.lineCount - 1), layout.getLineEnd(layout.lineCount - 1)).trim()
                if (!last.contains(' ')) alone += "$page: «$last» ends «${text.take(40)}…»"
            }
            save("lines-${page.name.lowercase()}")
        }
        assertTrue("some texts wrap, or this measures nothing", measured >= 5)
        assertTrue("a word alone on a last line:\n" + alone.joinToString("\n"), alone.isEmpty())
    }

    private fun save(name: String) {
        val dir = System.getenv("CHIARO_RENDER_DIR") ?: return
        compose.waitForIdle()
        val view = compose.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        compose.runOnUiThread { view.draw(Canvas(bitmap)) }
        val locale = app.resources.configuration.locales[0].language
        File(dir).apply { mkdirs() }.resolve("first-run-$name-$locale.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
