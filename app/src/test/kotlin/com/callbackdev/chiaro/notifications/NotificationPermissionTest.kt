package com.callbackdev.chiaro.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * The third way of losing a notification (15.3, 28 Oct 2026): the permission is held and
 * one channel is set to «none». Three states of a channel — muted, never created, normal —
 * and only the first is muted; a channel the notifier creates again at its default
 * importance stays muted, which is the Android rule the card relies on.
 */
@RunWith(RobolectricTestRunner::class)
class NotificationPermissionTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val manager = context.getSystemService(NotificationManager::class.java)

    private fun channel(id: String, importance: Int) {
        manager.createNotificationChannel(NotificationChannel(id, id, importance))
    }

    @Test
    fun `a channel at importance none is muted`() {
        channel("user_rules", NotificationManager.IMPORTANCE_NONE)

        assertEquals(setOf("user_rules"), NotificationPermission.mutedChannels(context, listOf("user_rules")))
    }

    @Test
    fun `a channel never created is not muted`() {
        assertEquals(emptySet<String>(), NotificationPermission.mutedChannels(context, listOf("alert_severe")))
    }

    @Test
    fun `a channel at its normal importance is not muted`() {
        channel("alert_summary", NotificationManager.IMPORTANCE_DEFAULT)
        channel("alert_severe", NotificationManager.IMPORTANCE_HIGH)

        assertEquals(
            emptySet<String>(),
            NotificationPermission.mutedChannels(context, listOf("alert_summary", "alert_severe"))
        )
    }

    /** Only the muted ones, in the order asked: the card lists them in the page's order. */
    @Test
    fun `several channels answer with the muted ones in order`() {
        channel("alert_severe", NotificationManager.IMPORTANCE_NONE)
        channel("alert_precip", NotificationManager.IMPORTANCE_DEFAULT)
        channel("user_rules", NotificationManager.IMPORTANCE_NONE)

        assertEquals(
            listOf("alert_severe", "user_rules"),
            NotificationPermission.mutedChannels(
                context, listOf("alert_severe", "alert_precip", "alert_summary", "user_rules")
            ).toList()
        )
    }

    /** A notifier's `createNotificationChannel` at the default importance does not un-mute
     * a channel the reader silenced: creating again can only lower, never raise. */
    @Test
    fun `recreating a muted channel leaves it muted`() {
        channel("user_rules", NotificationManager.IMPORTANCE_NONE)
        channel("user_rules", NotificationManager.IMPORTANCE_DEFAULT)

        assertEquals(setOf("user_rules"), NotificationPermission.mutedChannels(context, listOf("user_rules")))
    }

    @Test
    fun `the channel's road is its own settings page`() {
        NotificationPermission.openChannelSettings(context, "user_rules")

        val started = shadowOf(context as android.app.Application).nextStartedActivity
        assertEquals(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS, started.action)
        assertEquals(context.packageName, started.getStringExtra(Settings.EXTRA_APP_PACKAGE))
        assertEquals("user_rules", started.getStringExtra(Settings.EXTRA_CHANNEL_ID))
    }
}
