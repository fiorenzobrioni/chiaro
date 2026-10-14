package com.callbackdev.chiaro.ui.journal

import android.content.Context
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.data.FetchFailureReason
import com.callbackdev.chiaro.domain.settings.UnitSettings
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The Journal's hero (review of the Journal, A.1, 14 ott 2026): the answer as a word with
 * its arithmetic on a line of its own, and an answer on the first hours too, before there
 * is a strip to read.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rGB-w360dp-h800dp")
class JournalHeroTest {

    @get:Rule
    val compose = createComposeRule()

    private fun string(id: Int, vararg args: Any): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id, *args)

    private val zone = ZoneId.of("Europe/Rome")
    private val failed = JournalDay(
        LocalDate.of(2026, 10, 14),
        listOf(JournalEntry.FetchFailed(Instant.parse("2026-10-14T01:00:00Z"), FetchFailureReason.OFFLINE))
    )

    private fun show(content: JournalContent) {
        compose.setContent {
            ChiaroTheme(dynamicColor = false) { JournalBody(content, UnitSettings()) }
        }
    }

    @Test
    fun `the week's answer is a word, and its numbers stand whole on a line of their own`() {
        val drift = DriftModel(
            dates = listOf(LocalDate.of(2026, 10, 20)),
            columns = listOf(Instant.parse("2026-10-14T00:00:00Z"), Instant.parse("2026-10-14T06:00:00Z")),
            rain = listOf(listOf(52, 5)),
            highC = listOf(listOf(18.0, 18.0)),
            frostC = listOf(null),
            columnHours = 6
        )
        show(JournalContent("Milan", zone, listOf(failed), drift))
        compose.onNodeWithText(string(R.string.journal_drift_label)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.journal_drift_word_better, "Tuesday 20")).assertIsDisplayed()
        // At 360 dp the old one-line sentence wrapped inside its own number («52% →» over
        // «5%»): the arithmetic is now its own text, and it is one line.
        val evidence = compose.onNodeWithText(string(R.string.journal_field_rain, "52%", "5%"))
        evidence.assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        evidence.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action!!.invoke(layouts)
        assertEquals(1, layouts.single().lineCount)
    }

    @Test
    fun `the first hours answer too, in words`() {
        show(JournalContent("Milan", zone, listOf(failed), drift = null))
        compose.onNodeWithText(string(R.string.journal_drift_label)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.journal_drift_word_waiting)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.journal_drift_waiting)).assertIsDisplayed()
    }

    @Test
    fun `a week that held says so and points at no row`() {
        val drift = DriftModel(
            dates = listOf(LocalDate.of(2026, 10, 20)),
            columns = listOf(Instant.parse("2026-10-14T00:00:00Z"), Instant.parse("2026-10-14T06:00:00Z")),
            rain = listOf(listOf(20, 22)),
            highC = listOf(listOf(18.0, 18.2)),
            frostC = listOf(null),
            columnHours = 6
        )
        show(JournalContent("Milan", zone, listOf(failed), drift))
        compose.onNodeWithText(string(R.string.journal_drift_word_quiet)).assertIsDisplayed()
    }
}
