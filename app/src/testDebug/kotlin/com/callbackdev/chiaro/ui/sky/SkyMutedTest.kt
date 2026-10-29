package com.callbackdev.chiaro.ui.sky

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.provider.Settings
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.data.SkySubscription
import com.callbackdev.chiaro.domain.sky.SkyJobCatalog
import com.callbackdev.chiaro.notifications.SkyNotifier
import com.callbackdev.chiaro.readme.MilanRecording
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * The reminders' channel the phone has silenced is said on the Sky (15.13, 29 Oct 2026, the
 * second voice 15.3 gave Alerts). With the permission held, a bell set and the channel at
 * importance «none», the page draws the card naming the channel by the system's own name,
 * with «Open settings» leading to that channel's page; the channel at its normal importance
 * draws nothing; notifications off altogether draw the first voice and not this one.
 *
 * In `testDebug` as [SkyUndoTest]: the Compose rule's activity comes from a debug-only manifest.
 */
@RunWith(RobolectricTestRunner::class)
class SkyMutedTest {

    @get:Rule
    val compose = createComposeRule()

    private val app: Context = ApplicationProvider.getApplicationContext()
    private val manager = app.getSystemService(NotificationManager::class.java)

    private fun channel(importance: Int) {
        manager.createNotificationChannel(
            NotificationChannel(SkyNotifier.CHANNEL_ID, app.getString(R.string.sky_channel_name), importance)
        )
    }

    /** The recording's Sky with one bell really set: the sunset half an hour ahead. Built
     * from the subscriptions, not copied onto the built state: the moments' leads are read
     * at the build, and a fresh install's moments follow a default lead of «off». */
    private fun show() {
        val state = SkyStateBuilder.build(
            city = MilanRecording.city,
            report = MilanRecording.report,
            subscriptions = SkyJobCatalog.defaults.map {
                SkySubscription(it.id, notifyLeadMinutes = if (it.id == SkyJobCatalog.SunSet.id) 30 else null)
            },
            settings = MilanRecording.settings,
            now = MilanRecording.now
        )
        compose.setContent {
            ChiaroTheme(dynamicColor = false) {
                SkyScreen(
                    state = state,
                    actions = SkyActions(
                        addMoment = {},
                        removeMoment = {},
                        setLead = { _, _ -> },
                        restoreMoment = {}
                    ),
                    onOpenPlaces = {},
                    onOpenSettings = {},
                    onOpenGuide = {}
                )
            }
        }
    }

    private fun string(id: Int, vararg args: Any) = app.getString(id, *args)
    private fun plural(id: Int, n: Int, vararg args: Any) = app.resources.getQuantityString(id, n, *args)

    @Test
    fun `the muted channel is named, and its button opens its own page`() {
        channel(NotificationManager.IMPORTANCE_NONE)
        show()

        compose.onNodeWithText(plural(R.plurals.notifications_muted_title, 1)).assertExists()
        compose.onNodeWithText(
            plural(R.plurals.notifications_muted_body, 1, string(R.string.notifications_muted_name, string(R.string.sky_channel_name)))
        ).assertExists()
        compose.onAllNodesWithText(string(R.string.notifications_off_title)).assertCountEquals(0)

        compose.onNodeWithText(string(R.string.notifications_off_open_settings)).performClick()
        val started = shadowOf(app as Application).nextStartedActivity
        assertEquals(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS, started.action)
        assertEquals(SkyNotifier.CHANNEL_ID, started.getStringExtra(Settings.EXTRA_CHANNEL_ID))
    }

    @Test
    fun `the channel at its normal importance draws no card`() {
        channel(NotificationManager.IMPORTANCE_DEFAULT)
        show()

        compose.onAllNodesWithText(plural(R.plurals.notifications_muted_title, 1)).assertCountEquals(0)
        compose.onAllNodesWithText(string(R.string.notifications_off_open_settings)).assertCountEquals(0)
    }

    @Test
    fun `with notifications off altogether the first voice speaks, not this one`() {
        channel(NotificationManager.IMPORTANCE_NONE)
        shadowOf(manager).setNotificationsEnabled(false)
        show()

        compose.onNodeWithText(string(R.string.notifications_off_title)).assertExists()
        compose.onAllNodesWithText(plural(R.plurals.notifications_muted_title, 1)).assertCountEquals(0)
    }
}
