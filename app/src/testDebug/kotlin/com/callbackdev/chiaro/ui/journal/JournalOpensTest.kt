package com.callbackdev.chiaro.ui.journal

import android.content.Context
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.data.FetchFailureReason
import com.callbackdev.chiaro.domain.rules.NotificationRule
import com.callbackdev.chiaro.domain.settings.UnitSettings
import com.callbackdev.chiaro.domain.sky.SkyJobCatalog
import com.callbackdev.chiaro.domain.warnings.PlaceWarnings
import com.callbackdev.chiaro.domain.warnings.WarningHazard
import com.callbackdev.chiaro.domain.warnings.WarningLevel
import com.callbackdev.chiaro.domain.warnings.WarningZone
import com.callbackdev.chiaro.ui.sky.SkyText
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * What a line of the log opens (review of the Journal, A.6), and how TalkBack reads it
 * (A.11): a line with a page behind it is one node that says what the tap does; a line
 * with nothing behind it is one node too, and takes no tap.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rGB-w360dp-h800dp")
class JournalOpensTest {

    @get:Rule
    val compose = createComposeRule()

    private fun string(id: Int, vararg args: Any): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id, *args)

    private val zone = ZoneId.of("Europe/Rome")
    private val day = LocalDate.of(2026, 10, 14)
    private val saturday = LocalDate.of(2026, 10, 17)
    private val noon = Instant.parse("2026-10-14T10:00:00Z")
    private val drift = DriftModel(
        dates = listOf(saturday),
        columns = listOf(Instant.parse("2026-10-14T00:00:00Z"), Instant.parse("2026-10-14T06:00:00Z")),
        rain = listOf(listOf(52, 5)),
        highC = listOf(listOf(18.0, 18.0)),
        frostC = listOf(null),
        columnHours = 6
    )

    private fun show(
        vararg entries: JournalEntry,
        rules: Map<String, NotificationRule> = emptyMap(),
        warnings: PlaceWarnings? = null,
        onOpenAlert: (Long) -> Unit = {}
    ) {
        compose.setContent {
            ChiaroTheme(dynamicColor = false) {
                JournalBody(
                    content = JournalContent("Milan", zone, listOf(JournalDay(day, entries.toList())), drift),
                    units = UnitSettings(),
                    rules = rules,
                    warnings = warnings,
                    onOpenAlert = onOpenAlert
                )
            }
        }
    }

    private fun clickLabel(text: String): String? =
        compose.onNodeWithText(text).fetchSemanticsNode().config[SemanticsActions.OnClick].label

    @Test
    fun `a sky moment opens its page, and says so to TalkBack`() {
        val job = SkyJobCatalog.DarknessWindow
        show(JournalEntry.SkyObserved(noon, job.id, verdict = null, cloudPct = null))
        val name = string(SkyText.nameRes(job.id))
        compose.onNodeWithText(name).assertHasClickAction()
        assertEquals(string(R.string.sky_row_open_label), clickLabel(name))
        compose.onNodeWithText(name).performClick()
        compose.waitForIdle()
        assertTrue(compose.onAllNodesWithText(name).fetchSemanticsNodes().size >= 2)
    }

    @Test
    fun `a revision opens the story of its day`() {
        show(
            JournalEntry.ForecastShift(
                noon, saturday, better = true,
                shifts = listOf(FieldShift("precip_pct", "52", "5"))
            )
        )
        val headline = compose.onAllNodesWithText("Saturday", substring = true).fetchSemanticsNodes()
            .first { SemanticsActions.OnClick in it.config }
        assertEquals(string(R.string.journal_open_story), headline.config[SemanticsActions.OnClick].label)
        headline.config[SemanticsActions.OnClick].action!!.invoke()
        compose.waitForIdle()
        compose.onNodeWithText(string(R.string.journal_story_title, "Saturday 17")).assertIsDisplayed()
    }

    @Test
    fun `a fired alert opens it on its tab, and a deleted one stays still`() {
        var opened: Long? = null
        val bike = NotificationRule(id = 7L, name = "Bike", conditions = emptyList(), message = "")
        show(
            JournalEntry.RuleFired(noon, "Bike"),
            JournalEntry.RuleFired(noon.minusSeconds(3600), "Gone"),
            rules = mapOf("Bike" to bike),
            onOpenAlert = { opened = it }
        )
        val fired = string(R.string.journal_rule_fired, "Bike")
        assertEquals(string(R.string.journal_open_alert), clickLabel(fired))
        compose.onNodeWithText(fired).performClick()
        assertEquals(7L, opened)
        compose.onNodeWithText(string(R.string.journal_rule_fired, "Gone")).assertHasNoClickAction()
    }

    @Test
    fun `a warning opens the bulletin only while it still covers the day`() {
        val raised = JournalEntry.WarningChanged(
            noon, day, WarningHazard.THUNDERSTORM, WarningLevel.NONE, WarningLevel.YELLOW, issuedAt = null
        )
        val warnings = PlaceWarnings(
            zone = WarningZone("Lomb-09", "Milano", "Lombardia"),
            bulletinId = "b",
            issuedAt = day.atTime(15, 0),
            days = listOf(
                PlaceWarnings.DayWarnings(
                    day,
                    WarningHazard.entries.associateWith {
                        if (it == WarningHazard.THUNDERSTORM) WarningLevel.YELLOW else WarningLevel.NONE
                    }
                )
            ),
            note = null
        )
        show(raised, warnings = warnings)
        val line = compose.onAllNodesWithText(string(R.string.warning_phrase_yellow), substring = true)
            .fetchSemanticsNodes().first()
        assertEquals(string(R.string.journal_open_bulletin), line.config[SemanticsActions.OnClick].label)
    }

    @Test
    fun `a line with nothing behind it is one node, its hour included`() {
        show(JournalEntry.FetchFailed(noon, FetchFailureReason.OFFLINE))
        val node = compose.onNodeWithText(string(R.string.journal_fetch_failed))
        node.assertHasNoClickAction()
        val texts = node.fetchSemanticsNode().config[SemanticsProperties.Text].map { it.text }
        assertTrue(string(R.string.journal_fail_offline) in texts)
        assertTrue(texts.any { it.contains(":") })
    }
}
