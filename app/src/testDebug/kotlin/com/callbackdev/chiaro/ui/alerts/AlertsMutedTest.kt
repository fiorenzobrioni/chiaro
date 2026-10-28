package com.callbackdev.chiaro.ui.alerts

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.rules.NotificationRule
import com.callbackdev.chiaro.domain.rules.RuleCondition
import com.callbackdev.chiaro.domain.rules.RuleOp
import com.callbackdev.chiaro.readme.MilanRecording
import com.callbackdev.chiaro.ui.format.LocalClock
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import java.time.Clock
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * A channel the phone has silenced is said on screen (15.3, 28 Oct 2026). With the permission
 * held and one channel of a switched-on alert at importance «none», the page draws the card's
 * second voice, naming the channel by the system's own name, with «Open settings» leading to
 * that channel's page; several muted, one card that lists them; a channel at its normal
 * importance, or never created, draws nothing; notifications off altogether draw the first
 * voice and not this one.
 *
 * In `testDebug` for the reason the Sky's sheet tests give: the Compose rule's activity comes
 * from a debug-only manifest.
 */
@RunWith(RobolectricTestRunner::class)
class AlertsMutedTest {

    @get:Rule
    val compose = createComposeRule()

    private val app: Context = ApplicationProvider.getApplicationContext()
    private val manager = app.getSystemService(NotificationManager::class.java)

    private val zone = MilanRecording.alerts(app).zone

    private val bike = NotificationRule(
        id = 1,
        name = "Bici",
        enabled = true,
        conditions = listOf(RuleCondition("current.temp_c", RuleOp.GT, 15.0)),
        message = "Si va"
    )

    private fun channel(id: String, importance: Int) {
        manager.createNotificationChannel(NotificationChannel(id, id, importance))
    }

    private fun show(state: AlertsUiState = MilanRecording.alerts(app, rules = listOf(bike))) {
        compose.setContent {
            CompositionLocalProvider(LocalClock provides Clock.fixed(MilanRecording.now, zone)) {
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
    private fun plural(id: Int, n: Int, vararg args: Any) = app.resources.getQuantityString(id, n, *args)

    @Test
    fun `a muted channel is named, and its button opens its own page`() {
        channel("user_rules", NotificationManager.IMPORTANCE_NONE)
        show()

        compose.onNodeWithText(plural(R.plurals.notifications_muted_title, 1)).assertExists()
        compose.onNodeWithText(
            plural(R.plurals.notifications_muted_body, 1, string(R.string.notifications_muted_name, string(R.string.notif_channel_rules)))
        ).assertExists()
        compose.onAllNodesWithText(string(R.string.notifications_off_title)).assertCountEquals(0)

        compose.onNodeWithText(string(R.string.notifications_off_open_settings)).performClick()
        val started = shadowOf(app as Application).nextStartedActivity
        assertEquals(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS, started.action)
        assertEquals("user_rules", started.getStringExtra(Settings.EXTRA_CHANNEL_ID))
    }

    @Test
    fun `several muted channels are one card that lists them and opens the first`() {
        channel("alert_severe", NotificationManager.IMPORTANCE_NONE)
        channel("user_rules", NotificationManager.IMPORTANCE_NONE)
        show()

        compose.onNodeWithText(plural(R.plurals.notifications_muted_title, 2)).assertExists()
        compose.onNodeWithText("This phone has silenced “Severe weather” and “Your alerts”: they cannot arrive.")
            .assertExists()

        compose.onNodeWithText(string(R.string.notifications_off_open_settings)).performClick()
        assertEquals("alert_severe", shadowOf(app as Application).nextStartedActivity.getStringExtra(Settings.EXTRA_CHANNEL_ID))
    }

    @Test
    fun `a channel at its normal importance, or never created, draws no card`() {
        channel("user_rules", NotificationManager.IMPORTANCE_DEFAULT)
        channel("alert_severe", NotificationManager.IMPORTANCE_HIGH)
        show()

        compose.onAllNodesWithText(plural(R.plurals.notifications_muted_title, 1)).assertCountEquals(0)
        compose.onAllNodesWithText(plural(R.plurals.notifications_muted_title, 2)).assertCountEquals(0)
        compose.onAllNodesWithText(string(R.string.notifications_off_open_settings)).assertCountEquals(0)
    }

    /** A muted channel of an alert that is switched off is nothing the screen promised. */
    @Test
    fun `a muted channel of a switched-off alert is not named`() {
        channel("user_rules", NotificationManager.IMPORTANCE_NONE)
        show(MilanRecording.alerts(app, rules = listOf(bike.copy(enabled = false))))

        compose.onAllNodesWithText(plural(R.plurals.notifications_muted_title, 1)).assertCountEquals(0)
    }

    @Test
    fun `with notifications off altogether the first voice speaks, not this one`() {
        channel("user_rules", NotificationManager.IMPORTANCE_NONE)
        shadowOf(manager).setNotificationsEnabled(false)
        show()

        compose.onNodeWithText(string(R.string.notifications_off_title)).assertExists()
        compose.onAllNodesWithText(plural(R.plurals.notifications_muted_title, 1)).assertCountEquals(0)
    }
}
