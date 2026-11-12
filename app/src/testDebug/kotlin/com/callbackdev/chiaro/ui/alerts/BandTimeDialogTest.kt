package com.callbackdev.chiaro.ui.alerts

import android.content.Context
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * One end of a band of hours, in Material's own dialog for a time (the Alerts review, 12 nov
 * 2026): the dial where the window has room for it, with the button that turns it into two
 * fields; only the fields where it has not (a phone on its side, half a split screen), where
 * the dial in a plain dialog was cut; and «OK» refused on the other end's own time.
 *
 * In `testDebug` for the reason the Sky's sheet tests give: the Compose rule's activity comes
 * from a debug-only manifest.
 */
@RunWith(RobolectricTestRunner::class)
class BandTimeDialogTest {

    @get:Rule
    val compose = createComposeRule()

    private val app = ApplicationProvider.getApplicationContext<Context>()
    private val picked = mutableListOf<LocalTime>()
    private val ok = app.getString(android.R.string.ok)

    private fun show(initial: LocalTime = LocalTime.of(7, 0), other: LocalTime = LocalTime.of(9, 0)) {
        compose.setContent {
            ChiaroTheme(dynamicColor = false) {
                BandTimeDialog(
                    title = "From",
                    initial = initial,
                    other = other,
                    is24h = true,
                    onPick = { picked += it },
                    onDismiss = {}
                )
            }
        }
        compose.waitForIdle()
    }

    /** The two fields of the typed hour are the dialog's only editable text. */
    private fun fields(): Int = compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size

    @Test
    @Config(qualifiers = "en-rGB-w384dp-h800dp-xhdpi")
    fun `a tall window opens on the dial, and a button turns it into fields`() {
        show()
        assertEquals("the dial has nothing to type in", 0, fields())

        // Material's toggle is the one button of the dialog with no word on it.
        val toggle = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button) and
            !hasText(ok) and !hasText(app.getString(R.string.action_cancel))
        compose.onAllNodes(toggle).onFirst().performClick()
        compose.waitForIdle()
        assertTrue("the hour can be typed", fields() > 0)

        compose.onNodeWithText(ok).performClick()
        assertEquals(listOf(LocalTime.of(7, 0)), picked)
    }

    @Test
    @Config(qualifiers = "en-rGB-w640dp-h280dp-land-xhdpi")
    fun `a window too low for the dial has only the fields, and its buttons are on screen`() {
        show()
        assertTrue("the hour can be typed", fields() > 0)
        compose.onNodeWithText(ok).assertIsDisplayed()
        compose.onNodeWithText(app.getString(R.string.action_cancel)).assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "en-rGB-w384dp-h800dp-xhdpi")
    fun `the other end's own time is refused, and the dialog says why`() {
        show(initial = LocalTime.of(9, 0), other = LocalTime.of(9, 0))
        compose.onNodeWithText(ok).assertIsNotEnabled()
        compose.onNodeWithText(app.getString(R.string.rule_band_same)).assertExists()
    }
}
