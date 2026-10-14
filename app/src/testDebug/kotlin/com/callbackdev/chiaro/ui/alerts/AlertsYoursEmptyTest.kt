package com.callbackdev.chiaro.ui.alerts

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.readme.MilanRecording
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * «Nuovo avviso» is the one way in (committente, 14 ott 2026). With no alert of the
 * reader's own, «I tuoi» is a line that says what it is for, not a row of ideas: the
 * ideas are only in the FAB's sheet, so the page does not change shape at the first alert.
 *
 * In `testDebug` for the reason the Sky's sheet tests give: the Compose rule's activity
 * comes from a debug-only manifest.
 */
@RunWith(RobolectricTestRunner::class)
class AlertsYoursEmptyTest {

    @get:Rule
    val compose = createComposeRule()

    private val app: Context = ApplicationProvider.getApplicationContext()

    private fun show() {
        compose.setContent {
            ChiaroTheme(dynamicColor = false) {
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

    @Test
    fun `an empty Yours is one line, and the ideas are not on the page`() {
        show()

        val line = app.getString(R.string.alerts_yours_empty)
        // The page is a lazy list: the line is composed only once the list reaches it.
        compose.onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(hasText(line))
        compose.onNodeWithText(line).assertExists()
        compose.onAllNodesWithText(app.getString(R.string.alerts_templates_title)).assertCountEquals(0)
        compose.onAllNodesWithText(app.getString(R.string.tpl_blank_title)).assertCountEquals(0)
    }

    @Test
    fun `the ideas open from New alert`() {
        show()

        // The FAB merges its label into the button, so the label is found unmerged.
        compose.onNodeWithText(app.getString(R.string.alerts_new_rule), useUnmergedTree = true).performClick()

        compose.onNodeWithText(app.getString(R.string.tpl_blank_title)).assertExists()
        compose.onNodeWithText(app.getString(R.string.alerts_templates_title)).assertExists()
    }
}
