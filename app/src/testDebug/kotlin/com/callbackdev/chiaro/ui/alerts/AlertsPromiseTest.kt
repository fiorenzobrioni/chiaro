package com.callbackdev.chiaro.ui.alerts

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.data.warnings.PlaceWarningState
import com.callbackdev.chiaro.readme.MilanRecording
import com.callbackdev.chiaro.ui.format.LocalClock
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import java.time.Clock
import java.time.LocalTime
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The screen does not promise a sound that cannot come (15.2, 28 Oct 2026). The bulletin's
 * window is the place's, not the switch's: a place no bulletin grades has no «15–17» anywhere,
 * a bulletin already out today is not the next thing to ring, and with no place at all the page
 * is the empty state and nothing else.
 *
 * In `testDebug` for the reason the Sky's sheet tests give: the Compose rule's activity comes
 * from a debug-only manifest.
 */
@RunWith(RobolectricTestRunner::class)
class AlertsPromiseTest {

    @get:Rule
    val compose = createComposeRule()

    private val app: Context = ApplicationProvider.getApplicationContext()

    private val zone = MilanRecording.alerts(app).zone

    private fun show(state: AlertsUiState, at: LocalTime? = null) {
        val clock = at?.let {
            Clock.fixed(MilanRecording.now.atZone(zone).with(it).toInstant(), zone)
        } ?: Clock.fixed(MilanRecording.now, zone)
        compose.setContent {
            CompositionLocalProvider(LocalClock provides clock) {
                ChiaroTheme(dynamicColor = false) {
                    AlertsScreen(
                        state = state,
                        actions = AlertsActions({}, {}, {}, {}, {}, {}, {}, { _, _ -> }),
                        onEdit = {},
                        onOpenPlaces = {},
                        onOpenSettings = {}
                    )
                }
            }
        }
    }

    private fun string(id: Int, vararg args: Any) = app.getString(id, *args)

    private fun scrollTo(text: String) {
        compose.onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(hasText(text))
    }

    @Test
    fun `a place outside every zone has no bulletin hours anywhere`() {
        show(MilanRecording.alerts(app).copy(placeName = "Lyon", warnings = PlaceWarningState.Unavailable))

        // The head's answer merges its lines into one sentence for TalkBack: found unmerged.
        compose.onNodeWithText(string(R.string.warning_card_unavailable), useUnmergedTree = true).assertExists()
        // The switch stays: a setting is not hidden behind today's choice of city.
        scrollTo(string(R.string.warning_switch_title))
        compose.onNodeWithText(string(R.string.warning_switch_title)).assertExists()
        compose.onAllNodesWithText(string(R.string.alerts_hours_range, 15, 17)).assertCountEquals(0)
        compose.onAllNodesWithText(string(R.string.alerts_window_bulletin)).assertCountEquals(0)
        // The morning summary's window is still the day's: 12:15 at the recording, so the
        // next to ring is the evening's.
        compose.onNodeWithText(string(R.string.alerts_next_today)).assertExists()
    }

    @Test
    fun `a bulletin already out today is not the next thing to ring`() {
        val milan = MilanRecording.alerts(app)
        val current = milan.warnings as PlaceWarningState.Current
        val today = MilanRecording.now.atZone(zone).toLocalDate()
        val issuedToday = current.copy(
            warnings = current.warnings.copy(issuedAt = today.atTime(14, 34))
        )
        show(milan.copy(warnings = issuedToday), at = LocalTime.of(16, 0))

        // Four o'clock, inside 15–17, and the bulletin came at 14:34: the window is spent,
        // the evening summary is next.
        compose.onAllNodesWithText(string(R.string.alerts_next_now)).assertCountEquals(0)
        compose.onAllNodesWithText(string(R.string.alerts_window_bulletin)).assertCountEquals(0)
        compose.onNodeWithText(string(R.string.alerts_next_today)).assertExists()
    }

    @Test
    fun `yesterday's bulletin leaves today's window open`() {
        // The recording's own bulletin is yesterday's: at four the bulletin's window is open now.
        show(MilanRecording.alerts(app), at = LocalTime.of(16, 0))

        compose.onNodeWithText(string(R.string.alerts_next_now)).assertExists()
        compose.onNodeWithText(string(R.string.alerts_window_bulletin)).assertExists()
    }

    @Test
    fun `no place is the empty state and nothing else`() {
        show(AlertsUiState.NoPlace)

        compose.onNodeWithText(string(R.string.empty_no_place_title)).assertExists()
        compose.onNodeWithText(string(R.string.alerts_no_place_body)).assertExists()
        compose.onNodeWithText(string(R.string.empty_no_place_action)).assertExists()
        compose.onAllNodesWithText(string(R.string.warning_card_unavailable)).assertCountEquals(0)
        compose.onAllNodesWithText(string(R.string.alerts_group_warnings)).assertCountEquals(0)
        compose.onAllNodesWithText(string(R.string.alerts_group_ready)).assertCountEquals(0)
        compose.onAllNodesWithText(string(R.string.alerts_hours_range, 15, 17)).assertCountEquals(0)
    }
}
