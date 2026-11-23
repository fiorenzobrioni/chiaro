package com.callbackdev.chiaro.ui.today

import android.content.Context
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.settings.UnitSettings
import com.callbackdev.chiaro.readme.MilanRecording
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The hero's ribbon walked by a screen reader (23 nov 2026), on the README's recorded Milan:
 * back through the hours gone by to midnight, only the moves there are on offer, and the
 * line about an estimated now out of the way under an hour that is not now.
 *
 * In `testDebug` for the reason `AgendaPageSheetTest` gives: a Compose rule's activity is a
 * debug-only dependency.
 */
@RunWith(RobolectricTestRunner::class)
class TodayRibbonTest {

    @get:Rule
    val compose = createComposeRule()

    private val app: Context = ApplicationProvider.getApplicationContext()
    private val next get() = app.getString(R.string.ribbon_action_next)
    private val previous get() = app.getString(R.string.ribbon_action_previous)

    private fun show(at: Instant) {
        val state = MilanRecording.today(app, at = at)
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

    /** The ribbon's two actions as they are offered now, by label. */
    private fun actions(): Map<String, CustomAccessibilityAction> {
        val ribbon = SemanticsMatcher("the hero's ribbon") { node ->
            node.config.getOrElse(SemanticsActions.CustomActions) { emptyList() }
                .any { it.label == next || it.label == previous }
        }
        val nodes = compose.onAllNodes(ribbon).fetchSemanticsNodes()
        if (nodes.isEmpty()) return emptyMap()
        return nodes.single().config[SemanticsActions.CustomActions].associateBy { it.label }
    }

    private fun perform(label: String): Boolean {
        val action = actions().getValue(label)
        var answer = false
        compose.runOnUiThread { answer = action.action() }
        compose.waitForIdle()
        return answer
    }

    @Test
    fun `an hour gone by says it is an estimate, and the estimated now steps aside`() {
        // Two hours after the fetch the block is replaced by the forecast, and the hero says so.
        show(MilanRecording.now.plus(Duration.ofHours(2)))
        val estimatedNow = app.getString(R.string.estimate_hero)
        compose.onAllNodesWithText(estimatedNow).assertCountEquals(1)
        assertEquals(setOf(next, previous), actions().keys)

        assertTrue("a step back moves", perform(previous))
        val estimate = app.getString(R.string.hero_estimate_at, "").trim()
        compose.onAllNodesWithText(estimate, substring = true).assertCountEquals(1)
        // The line about the now that is no longer on the canvas is neither drawn nor read.
        compose.onAllNodesWithText(estimatedNow).assertCountEquals(0)

        // Back to now: the line is there again.
        assertTrue(perform(next))
        compose.onAllNodesWithText(estimatedNow).assertCountEquals(1)
    }

    @Test
    fun `the steps back stop at midnight, and are no longer offered there`() {
        show(MilanRecording.now)
        // 09:40: nine hours behind (00:00 to 08:00; 09:00 is the hour we are in).
        var steps = 0
        while (previous in actions() && steps < 30) {
            assertTrue(perform(previous))
            steps++
        }
        assertEquals(9, steps)
        assertEquals(setOf(next), actions().keys)
    }

    @Test
    fun `at half past eleven at night only the way back is offered`() {
        val milan = ZoneId.of("Europe/Rome")
        val late = MilanRecording.now.atZone(milan).toLocalDate().atTime(23, 30).atZone(milan).toInstant()
        show(late)
        assertEquals(setOf(previous), actions().keys)
    }
}
