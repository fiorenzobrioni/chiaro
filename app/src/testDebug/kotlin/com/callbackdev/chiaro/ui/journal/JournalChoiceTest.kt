package com.callbackdev.chiaro.ui.journal

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.settings.UnitSettings
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Rain or highs is one choice of two (review of the Journal, A.7), and the page has a door
 * to its own chapter of the guide (A.10).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rGB-w360dp-h800dp")
class JournalChoiceTest {

    @get:Rule
    val compose = createComposeRule()

    private fun string(id: Int): String = ApplicationProvider.getApplicationContext<Context>().getString(id)

    private val drift = DriftModel(
        dates = listOf(LocalDate.of(2026, 10, 20)),
        columns = listOf(Instant.parse("2026-10-14T00:00:00Z"), Instant.parse("2026-10-14T06:00:00Z")),
        rain = listOf(listOf(52, 5)),
        highC = listOf(listOf(18.0, 18.0)),
        frostC = listOf(null),
        columnHours = 6
    )

    private fun show() {
        compose.setContent {
            ChiaroTheme(dynamicColor = false) {
                JournalBody(JournalContent("Milan", ZoneId.of("UTC"), emptyList(), drift), UnitSettings())
            }
        }
    }

    @Test
    fun `rain and highs are one choice, and choosing one leaves the other`() {
        show()
        val rain = compose.onNodeWithText(string(R.string.journal_metric_rain))
        val high = compose.onNodeWithText(string(R.string.journal_metric_high))
        rain.assertIsSelected()
        high.assertIsNotSelected()
        high.performClick()
        high.assertIsSelected()
        rain.assertIsNotSelected()
    }

    @Test
    fun `the guide's chapter opens from the page it explains`() {
        show()
        compose.onNodeWithText(string(R.string.journal_guide_entry_title)).performClick()
        compose.waitForIdle()
        compose.onNodeWithText(string(R.string.guide_journal_p1)).assertIsDisplayed()
    }
}
