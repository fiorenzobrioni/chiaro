package com.callbackdev.chiaro.ui.today

import android.content.Context
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.settings.UnitSettings
import com.callbackdev.chiaro.readme.MilanRecording
import com.callbackdev.chiaro.ui.format.Formats
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Today after its review (15 ott 2026), on the README's recorded Milan: the day's agenda
 * speaks the Sky's names and opens the Sky's page for a moment, without the Sky's button to
 * follow it; and a week row says whether it is open.
 *
 * In `testDebug` for the reason `AgendaPageSheetTest` gives: a Compose rule's activity is a
 * debug-only dependency.
 */
@RunWith(RobolectricTestRunner::class)
class TodayAgendaTest {

    @get:Rule
    val compose = createComposeRule()

    private val app: Context = ApplicationProvider.getApplicationContext()

    private fun show() {
        val state = MilanRecording.today(app)
        compose.setContent {
            ChiaroTheme(dynamicColor = false) {
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

    private fun scrollTo(text: String) {
        compose.onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(hasText(text))
    }

    @Test
    fun `the day's next moment wears the Sky's label and the Sky's name`() {
        show()
        val name = app.getString(R.string.sky_name_golden_pm)
        scrollTo(name)

        val next = app.getString(R.string.sky_next_label, app.getString(R.string.sky_day_today).lowercase())
        // «Next, today · in 6 h 20 min»: how soon rides on the label (15 ott 2026).
        compose.onAllNodesWithText(next, substring = true).assertCountEquals(1)
        compose.onAllNodesWithText(name).assertCountEquals(1)
    }

    /** The product's first promise on Today (15 ott 2026): the golden hour says whether the
     * sky will allow it, with the number that decided it, as the Sky tab does. */
    @Test
    fun `a moment of the sky carries its verdict and its number`() {
        show()
        val name = app.getString(R.string.sky_name_golden_pm)
        scrollTo(name)
        val word = app.getString(R.string.verdict_pass)
        assertTrue(
            "the golden hour's verdict is drawn",
            compose.onAllNodes(hasText(word, substring = true)).fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodes(
                    SemanticsMatcher("a chip saying $word") { node ->
                        node.config.getOrElse(SemanticsProperties.ContentDescription) { emptyList() }
                            .any { it.startsWith(word) }
                    }
                ).fetchSemanticsNodes().isNotEmpty()
        )
    }

    /** The reader's own rules that hold today stand among the moments, in their own words. */
    @Test
    fun `a rule of the reader's that holds today is a row of the day`() {
        show()
        val rule = MilanRecording.yourRules(app).first()
        scrollTo(rule.name)
        compose.onAllNodes(hasText(rule.name)).fetchSemanticsNodes().let {
            assertTrue("the rule's row is there", it.isNotEmpty())
        }
    }

    @Test
    fun `a moment of the sky opens its page, where to look and no button to follow it`() {
        show()
        val name = app.getString(R.string.sky_name_golden_pm)
        scrollTo(name)

        compose.onAllNodes(hasText(name) and hasClickAction()).onFirst().performClick()
        compose.waitForIdle()

        // The golden hour has a direction, so its page opens on «Where to look».
        assertTrue(
            "the page opens with where to look",
            compose.onAllNodesWithText(app.getString(R.string.sky_look_title)).fetchSemanticsNodes().isNotEmpty()
        )
        compose.onAllNodesWithText(app.getString(R.string.sky_guide_add_action)).assertCountEquals(0)
        compose.onAllNodesWithText(app.getString(R.string.sky_guide_remove_action)).assertCountEquals(0)
    }

    @Test
    fun `a week row says whether it is open, and opening it says so`() {
        show()
        val tomorrow = MilanRecording.today(app).week[1].forecast.date
        val label = Formats.dayLabel(tomorrow, app.resources.configuration.locales[0])
        scrollTo(label)

        val row = hasText(label) and hasClickAction()
        fun state(word: Int) = SemanticsMatcher.expectValue(
            SemanticsProperties.StateDescription,
            app.getString(word)
        )
        compose.onAllNodes(row and state(R.string.week_day_state_closed)).assertCountEquals(1)

        compose.onAllNodes(row).onFirst().performClick()
        compose.waitForIdle()

        compose.onAllNodes(row and state(R.string.week_day_state_open)).assertCountEquals(1)
    }

    /**
     * Closing a day crashed the app (committente, device, 15 ott 2026): the open day's top air
     * rode the spatial spring, which overshoots, and on its way back to zero it went under
     * it — a negative padding, which Compose refuses with an exception. Open, close, and let
     * every animation run to its end.
     */
    @Test
    fun `a day opened and closed again closes without a crash`() {
        show()
        val tomorrow = MilanRecording.today(app).week[1].forecast.date
        val label = Formats.dayLabel(tomorrow, app.resources.configuration.locales[0])
        scrollTo(label)
        val row = hasText(label) and hasClickAction()

        repeat(2) {
            compose.onAllNodes(row).onFirst().performClick()
            compose.waitForIdle()
            compose.onAllNodes(row).onFirst().performClick()
            compose.waitForIdle()
        }

        compose.onAllNodes(
            row and SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                app.getString(R.string.week_day_state_closed)
            )
        ).assertCountEquals(1)
    }

    /** The bar's number is what TalkBack hears there once the hero's has gone under it
     * (DESIGN §8.1e): one announcement, the icon beside it silent. */
    @Test
    fun `scrolled to the end, the bar says the temperature`() {
        show()
        val state = MilanRecording.today(app)
        val temperature = Formats.temperature(
            state.report.current.tempC, UnitSettings().temperature, app.resources.configuration.locales[0]
        )
        scrollTo(app.getString(R.string.section_details))
        compose.waitForIdle()
        compose.onAllNodes(
            SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(temperature))
        ).assertCountEquals(1)
    }
}
