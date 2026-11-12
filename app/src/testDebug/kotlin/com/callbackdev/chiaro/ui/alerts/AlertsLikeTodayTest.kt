package com.callbackdev.chiaro.ui.alerts

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.readme.MilanRecording
import com.callbackdev.chiaro.ui.format.LocalClock
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import java.time.Clock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Alerts in the shape Today and the Sky have (the Alerts review, 12 nov 2026): the place
 * leads the header with the tab's name under it, «Add» stands in the title of «Yours» where a
 * floating button stood over the switches, a title has Today's 24 over it and 12 under it,
 * and a row with a switch says whether it is on to a screen reader. A tall window, so the
 * whole page is composed.
 *
 * In `testDebug` for the reason the Sky's sheet tests give: the Compose rule's activity comes
 * from a debug-only manifest.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rGB-w384dp-h2600dp-xhdpi")
class AlertsLikeTodayTest {

    @get:Rule
    val compose = createComposeRule()

    private val app = ApplicationProvider.getApplicationContext<Context>()
    private val rules = MilanRecording.yourRules(app)
    private val morning = mutableListOf<Boolean>()

    private fun show() {
        val state = MilanRecording.alerts(app, rules)
        compose.setContent {
            CompositionLocalProvider(LocalClock provides Clock.fixed(MilanRecording.now, state.zone)) {
                ChiaroTheme(dynamicColor = false) {
                    AlertsScreen(
                        state = state,
                        actions = AlertsActions(
                            setOfficialWarnings = {},
                            setOfficialWarningsFrom = {},
                            setSevereWeather = {},
                            setPrecipitationWarning = {},
                            setDailySummary = { morning += it },
                            setEveningSummary = {},
                            update = {},
                            addFromTemplate = { _, _ -> }
                        ),
                        onEdit = {},
                        onOpenPlaces = {},
                        onOpenSettings = {}
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    private fun string(id: Int): String = app.getString(id)

    private fun bounds(text: String): Rect =
        compose.onNode(hasText(text), useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

    @Test
    fun `the place leads the header, and the tab's name stands under it`() {
        show()
        // One button to a screen reader, as on Today and on the Sky: the place, then what
        // the page is about, with «change place» as its action.
        val header = compose.onNode(
            hasContentDescription("${MilanRecording.city.name}, ${string(R.string.tab_alerts)}")
        )
        header.assertExists()
        header.assertHeightIsAtLeast(48.dp)
        // And the name is not a title of its own any more: it is the block's second line,
        // said by the block. (The merged tree: the unmerged one keeps what a block's own
        // description replaces, and the first CI found the word there.)
        compose.onAllNodes(hasText(string(R.string.tab_alerts))).assertCountEquals(0)
    }

    @Test
    fun `adding is a button in the title of Yours, a full target, and it opens the ideas`() {
        show()
        val add = compose.onNode(hasClickAction() and hasContentDescription(string(R.string.alerts_new_rule)))
        add.assertTouchHeightIsEqualTo(48.dp).assertWidthIsAtLeast(48.dp)
        add.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        val button = add.fetchSemanticsNode().boundsInRoot
        val title = bounds(string(R.string.alerts_group_yours))
        assertEquals("on the title's line", title.center.y, button.center.y, 2f)
        assertTrue("after the title", button.left >= title.right)
        val word = bounds(string(R.string.alerts_add))
        assertTrue("one word, inside the button", word.left > button.left && word.right < button.right)

        add.performClick()
        compose.waitForIdle()
        compose.onNodeWithText(string(R.string.tpl_blank_title)).assertExists()
        compose.onNodeWithText(string(R.string.alerts_templates_title)).assertExists()
    }

    @Test
    fun `a title has 24 over it and 12 under it, as on Today`() {
        show()
        val title = bounds(string(R.string.alerts_group_yours))
        // The group above ends with the evening summary's row, which fills its segment; the
        // group under begins with the first rule's card, half the segments' 2dp under the
        // title's own 12.
        val above = compose.onNode(hasText(string(R.string.alert_evening_title)) and isToggleable())
            .fetchSemanticsNode().boundsInRoot
        val under = compose.onNode(hasText(rules.first().name) and hasClickAction())
            .fetchSemanticsNode().boundsInRoot
        val over = with(compose.density) { (title.top - above.bottom).toDp() }
        val below = with(compose.density) { (under.top - title.bottom).toDp() }
        assertEquals("over the title", 24f, over.value, 1f)
        assertEquals("under the title", 13f, below.value, 1f)
    }

    @Test
    fun `a ready-made row is a switch that says whether it is on`() {
        show()
        val row = compose.onNode(hasText(string(R.string.alert_summary_title)) and isToggleable())
        row.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
        row.assertIsOn()

        row.performClick()
        assertEquals(listOf(false), morning)
    }

    @Test
    fun `a rule's switch carries the rule's name`() {
        show()
        compose.onNode(isToggleable() and hasContentDescription(rules.first().name)).assertIsOn()
    }

    /**
     * The windows' hours follow the phone's clock, as every other hour of the page does:
     * «15–17» is «3–5 PM» over the bulletin's switch and in the head's next window, and the
     * quiet hours are said in the same clock. The words are [AlertHours]' own, whose shape
     * `AlertHoursTest` pins: here, that the page asks for them.
     */
    @Test
    fun `on a twelve-hour clock the windows' hours follow it`() {
        Settings.System.putString(app.contentResolver, Settings.System.TIME_12_24, "12")
        show()
        val locale = app.resources.configuration.locales[0]
        val twelve = AlertHours.range(app.resources, 15, 17, false, locale)
        val twentyFour = app.getString(R.string.alerts_hours_range, 15, 17)
        assertTrue("the two clocks print two things", twelve != twentyFour)

        assertTrue(
            "the bulletin's window on the phone's clock",
            compose.onAllNodes(hasText(twelve), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        )
        compose.onAllNodes(hasText(twentyFour), useUnmergedTree = true).assertCountEquals(0)
        val quiet = app.getString(
            R.string.alerts_day_quiet,
            AlertHours.label(22, false, locale), AlertHours.label(7, false, locale)
        )
        compose.onNode(hasText(quiet), useUnmergedTree = true).assertExists()
    }
}
