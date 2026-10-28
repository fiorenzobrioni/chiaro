package com.callbackdev.chiaro.ui.alerts

import androidx.annotation.StringRes
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.AlertKind
import com.callbackdev.chiaro.domain.warnings.WarningLevel
import com.callbackdev.chiaro.notifications.AlertNotifier
import com.callbackdev.chiaro.notifications.OfficialWarningNotifier
import com.callbackdev.chiaro.notifications.RuleNotifier

/**
 * A notification channel one of the screen's promises rides on (15.3, 28 Oct 2026): its
 * system id and the name the system shows for it, which is the name the card uses when the
 * phone has silenced it — the reader will look for that word on the settings page.
 */
internal class PromisedChannel(val id: String, @StringRes val nameRes: Int)

/**
 * The channels the screen is promising a notification on, in the order of the page: every
 * switched-on alert's, and the reader's own channel when a rule of theirs is enabled.
 * It is [notificationsPromised] said channel by channel, and the two must agree: a promise
 * with no channel here is one the muted card can never name.
 *
 * - The severe-weather switch carries two channels, the storm's and the heat's: the heat
 *   rides that switch (14.6) but is posted on a channel of its own, so the phone can mute
 *   one and keep the other, and the card must be able to name either.
 * - The official warnings carry the orange-and-red channel always, and the yellow one only
 *   while the switch starts from yellow: from orange up the app never posts a yellow, so a
 *   muted yellow channel silences nothing the screen promised.
 * - A channel is created by the first `notify` on it, so most of these do not exist on a
 *   fresh install; [com.callbackdev.chiaro.notifications.NotificationPermission.mutedChannels]
 *   reads a channel that was never created as not muted.
 */
internal fun promisedChannels(content: AlertsUiState.Content): List<PromisedChannel> = buildList {
    with(content.notifications) {
        if (severeWeatherAlerts) {
            add(channelOf(AlertKind.SEVERE))
            add(channelOf(AlertKind.HEAT))
        }
        if (precipitationWarning) add(channelOf(AlertKind.PRECIPITATION))
        if (dailySummary) add(channelOf(AlertKind.DAILY_SUMMARY))
        if (eveningSummary) add(channelOf(AlertKind.EVENING_SUMMARY))
        if (officialWarnings) {
            add(PromisedChannel(OfficialWarningNotifier.CHANNEL_HIGH, R.string.notif_channel_warning_high))
            if (officialWarningsFrom <= WarningLevel.YELLOW) {
                add(PromisedChannel(OfficialWarningNotifier.CHANNEL_YELLOW, R.string.notif_channel_warning_yellow))
            }
        }
        if (content.rules.any { it.rule.enabled }) {
            add(PromisedChannel(RuleNotifier.CHANNEL_ID, R.string.notif_channel_rules))
        }
    }
}

/**
 * Whether a row of the page is silenced: every channel it rides on is among [muted]. A row
 * whose channels are all muted is a row that is off, whatever its switch says, and fades
 * like one. One muted channel of two (the heat's under the storm's switch, the yellow's
 * under the warnings') does not fade the row — the storm still arrives — and the card above
 * names the one that will not.
 */
internal fun rowMuted(muted: Set<String>, vararg kinds: AlertKind): Boolean =
    kinds.all { AlertNotifier.channelId(it) in muted }

/**
 * [rowMuted] for the official warnings' switch, whose channels depend on the level it
 * starts from: the ones [promisedChannels] listed for it, all muted. With the switch off
 * there are none, and the row is not muted — it is off, which the switch already says.
 */
internal fun warningsRowMuted(channels: List<PromisedChannel>, muted: Set<String>): Boolean {
    val own = channels.filter {
        it.id == OfficialWarningNotifier.CHANNEL_HIGH || it.id == OfficialWarningNotifier.CHANNEL_YELLOW
    }
    return own.isNotEmpty() && own.all { it.id in muted }
}

private fun channelOf(kind: AlertKind) =
    PromisedChannel(AlertNotifier.channelId(kind), AlertNotifier.channelNameRes(kind))
