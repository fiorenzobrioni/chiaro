package com.callbackdev.chiaro.ui.journal

import android.content.Context
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.R
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
 * The strip read with a finger and with TalkBack (review of the Journal, C.1): each day is
 * a control over the updates whose state is the square's reading, and moving it writes the
 * same reading over the strip (the line itself is silent: the control carries its words).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rGB-w360dp-h800dp")
class JournalStripTest {

    @get:Rule
    val compose = createComposeRule()

    private fun string(id: Int, vararg args: Any): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id, *args)

    private val zone = ZoneId.of("UTC")
    private val tuesday = LocalDate.of(2026, 10, 20)
    private val drift = DriftModel(
        dates = listOf(tuesday),
        // Wednesday 14 at 00:00, an empty slot, then 12:00.
        columns = listOf(Instant.parse("2026-10-14T00:00:00Z"), null, Instant.parse("2026-10-14T12:00:00Z")),
        rain = listOf(listOf(52, null, 5)),
        highC = listOf(listOf(18.0, null, 19.0)),
        frostC = listOf(null),
        columnHours = 6
    )

    private fun show() {
        compose.setContent {
            ChiaroTheme(dynamicColor = false) {
                JournalBody(JournalContent("Milan", zone, emptyList(), drift), UnitSettings())
            }
        }
    }

    @Test
    fun `a day is a control whose state is the newest update's reading`() {
        show()
        val day = compose.onNodeWithContentDescription("Tuesday 20").fetchSemanticsNode()
        val reading = string(
            R.string.journal_scrub_reading, "Wednesday", "12:00",
            string(R.string.journal_scrub_rain, "5%") + " · " + string(R.string.journal_scrub_high, "19°")
        )
        assertEquals(reading, day.config[SemanticsProperties.StateDescription])
        assertEquals(2f, day.config[SemanticsProperties.ProgressBarRangeInfo].current)
    }

    private fun state(): String? = compose.onNodeWithContentDescription("Tuesday 20")
        .fetchSemanticsNode().config[SemanticsProperties.StateDescription]

    @Test
    fun `moving it reads each square, an empty slot included`() {
        show()
        compose.onNodeWithContentDescription("Tuesday 20")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(0f) }
        assertEquals(
            string(
                R.string.journal_scrub_reading, "Wednesday", "00:00",
                string(R.string.journal_scrub_rain, "52%") + " · " + string(R.string.journal_scrub_high, "18°")
            ),
            state()
        )
        compose.onNodeWithContentDescription("Tuesday 20")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(1f) }
        assertEquals(string(R.string.journal_scrub_empty, "Wednesday", "06:00"), state())
    }
}
