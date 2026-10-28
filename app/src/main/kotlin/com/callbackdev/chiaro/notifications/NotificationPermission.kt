package com.callbackdev.chiaro.notifications

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * One honest question — *can this app show a notification?* — and the one road that
 * answers it when the runtime dialog no longer can.
 *
 * The distinction matters because the two ways of losing notifications look the same
 * from a screen and are repaired in opposite places. The permission can be **ungranted**
 * (a fresh install that was never asked, or a reader who said no), which the runtime
 * dialog fixes — once. It can also be granted and then **switched off** in the system's
 * own notification settings, where the dialog is a no-op: `launch()` returns granted
 * immediately and nothing appears, so a button wired to it is a dead button.
 *
 * [allowed] is therefore what every screen asks: the app-level state the system will
 * actually honour, not the permission flag on its own. A screen that promised an alert
 * while this is false is a screen that lies (CLAUDE.md, "the screen must not lie").
 *
 * There is a third way of losing them, one channel at a time (15.3, 28 Oct 2026): the
 * permission is held, the app's master switch is on, and the reader — or a long press on
 * one notification — set a channel's importance to «none». [allowed] stays true, every
 * `notify` on that channel returns false, and the rule's card says «never fired» for
 * life. [mutedChannels] is the question for that case, and [openChannelSettings] its
 * road: the channel's own page, where the one switch that matters is.
 */
object NotificationPermission {

    const val PERMISSION: String = Manifest.permission.POST_NOTIFICATIONS

    /** Whether a notification posted right now would be delivered. */
    fun allowed(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    /**
     * Which of [channelIds] the phone has silenced: the channel exists and its importance
     * is `IMPORTANCE_NONE`. A channel is created by the first `notify` on it, so one that
     * was never created is not muted — nothing was ever refused on it, and the card that
     * reads this must not be drawn over a channel the reader has never seen. Recreating a
     * channel can only lower its importance, never raise it (Android's rule, and
     * Robolectric's), so a notifier's `createNotificationChannel` does not un-mute it.
     */
    fun mutedChannels(context: Context, channelIds: Collection<String>): Set<String> {
        val manager = NotificationManagerCompat.from(context)
        return channelIds.filterTo(LinkedHashSet()) { id ->
            manager.getNotificationChannelCompat(id)?.importance ==
                NotificationManagerCompat.IMPORTANCE_NONE
        }
    }

    /** Whether the runtime permission itself is held — a narrower question than [allowed]. */
    fun granted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, PERMISSION) == PackageManager.PERMISSION_GRANTED

    /**
     * True when asking the system is pointless and only its settings screen can help:
     * the permission is already held (so the dialog would return at once, silently)
     * and notifications are off anyway.
     *
     * The other dead case — permission refused for good, where the system shows no
     * dialog either — cannot be told apart from "never asked" before the request is
     * made, so it is caught on the way back instead: see [deniedForGood].
     */
    fun onlySettingsCanHelp(context: Context): Boolean = granted(context) && !allowed(context)

    /**
     * Read in the request's own callback, with the result it returned: the request
     * came back refused AND the system will not offer the dialog again, which is
     * exactly the state where nothing visible happened and the reader is owed the
     * settings screen instead.
     *
     * `shouldShowRequestPermissionRationale` cannot tell "never asked" from "refused
     * for good" on its own — it is false for both — which is why this is asked after a
     * refusal rather than before the request. After a first refusal it is true, so one
     * "no" is taken as a "no" and does not throw the reader into Settings.
     */
    fun deniedForGood(activity: Activity?, granted: Boolean): Boolean =
        !granted && activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, PERMISSION)

    /**
     * The app's own page in the system notification settings — every channel of it,
     * and the master switch above them. `ACTION_APP_NOTIFICATION_SETTINGS` is the
     * documented road; the application-details page is the fallback for a device that
     * does not carry it, and a launch that fails is swallowed rather than crashing the
     * app over a settings screen.
     */
    fun openSettings(context: Context) {
        val notifications = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        val details = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(android.net.Uri.fromParts("package", context.packageName, null))
        for (intent in listOf(notifications, details)) {
            if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (runCatching { context.startActivity(intent) }.isSuccess) return
        }
    }

    /**
     * One channel's page in the system notification settings — the switch that silenced
     * it, and nothing else to find (15.3). `ACTION_CHANNEL_NOTIFICATION_SETTINGS` wants
     * the package and the channel id; a device that does not answer it gets the app's
     * page through [openSettings], where the channel is one row down.
     */
    fun openChannelSettings(context: Context, channelId: String) {
        val channel = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
        if (context !is Activity) channel.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (runCatching { context.startActivity(channel) }.isSuccess) return
        openSettings(context)
    }

    /** The Activity a composable's context is hosted by, unwrapped through the themes. */
    fun activityOf(context: Context): Activity? {
        var current = context
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return null
    }
}
