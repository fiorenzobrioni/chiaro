package com.callbackdev.chiaro.ui.alerts

import com.callbackdev.chiaro.domain.AlertKind
import com.callbackdev.chiaro.domain.rules.NotificationRule
import com.callbackdev.chiaro.domain.rules.RuleCondition
import com.callbackdev.chiaro.domain.rules.RuleOp
import com.callbackdev.chiaro.domain.settings.NotificationSettings
import com.callbackdev.chiaro.domain.settings.UnitSettings
import com.callbackdev.chiaro.domain.warnings.WarningLevel
import com.callbackdev.chiaro.notifications.AlertNotifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Which channels the screen's promises ride on (15.3, 28 Oct 2026): the switch-by-switch
 * reading of [notificationsPromised], so a muted channel can be named. The storm's switch
 * carries the heat's channel too; the warnings' carries the yellow only from yellow; the
 * reader's channel only with an enabled rule; nothing switched on, no channel at all.
 */
@RunWith(RobolectricTestRunner::class)
class PromisedChannelsTest {

    private val allOff = NotificationSettings(
        severeWeatherAlerts = false,
        dailySummary = false,
        eveningSummary = false,
        precipitationWarning = false,
        officialWarnings = false
    )

    private fun content(
        notifications: NotificationSettings = allOff,
        rules: List<NotificationRule> = emptyList()
    ) = AlertsUiState.Content(
        placeName = "Milano",
        notifications = notifications,
        rules = rules.map { RuleCardModel(it, lastFired = null) },
        canAdd = true,
        units = UnitSettings()
    )

    private fun rule(enabled: Boolean) = NotificationRule(
        id = 1,
        name = "Bici",
        enabled = enabled,
        conditions = listOf(RuleCondition("current.temp_c", RuleOp.GT, 15.0)),
        message = "Si va"
    )

    private fun ids(content: AlertsUiState.Content) = promisedChannels(content).map { it.id }

    @Test
    fun `everything off promises on no channel`() {
        assertEquals(emptyList<String>(), ids(content()))
    }

    @Test
    fun `the severe switch carries the storm's channel and the heat's`() {
        assertEquals(
            listOf("alert_severe", "alert_heat"),
            ids(content(allOff.copy(severeWeatherAlerts = true)))
        )
    }

    @Test
    fun `each summary and the rain have a channel of their own`() {
        assertEquals(listOf("alert_precip"), ids(content(allOff.copy(precipitationWarning = true))))
        assertEquals(listOf("alert_summary"), ids(content(allOff.copy(dailySummary = true))))
        assertEquals(listOf("alert_evening"), ids(content(allOff.copy(eveningSummary = true))))
    }

    @Test
    fun `the warnings carry the yellow channel only from yellow`() {
        assertEquals(
            listOf("warning_high", "warning_yellow"),
            ids(content(allOff.copy(officialWarnings = true, officialWarningsFrom = WarningLevel.YELLOW)))
        )
        assertEquals(
            listOf("warning_high"),
            ids(content(allOff.copy(officialWarnings = true, officialWarningsFrom = WarningLevel.ORANGE)))
        )
    }

    @Test
    fun `the reader's channel only with an enabled rule`() {
        assertEquals(
            listOf("user_rules"),
            ids(content(allOff, rules = listOf(rule(enabled = true))))
        )
        assertEquals(
            emptyList<String>(),
            ids(content(allOff, rules = listOf(rule(enabled = false))))
        )
    }

    /** The two readings agree: a promise is a promise on some channel, and no channel
     * is promised without a promise. */
    @Test
    fun `a promise always has a channel and a channel always a promise`() {
        val fresh = content(NotificationSettings())
        assertTrue(notificationsPromised(fresh))
        assertTrue(ids(fresh).isNotEmpty())
        val off = content()
        assertFalse(notificationsPromised(off))
        assertTrue(ids(off).isEmpty())
    }

    /** The channel's name is the one the notifier gives the system, so the card's word is
     * the word on the settings page. */
    @Test
    fun `a channel is named as the notifier names it`() {
        val severe = promisedChannels(content(allOff.copy(severeWeatherAlerts = true))).first()
        assertEquals(AlertNotifier.channelNameRes(AlertKind.SEVERE), severe.nameRes)
    }

    @Test
    fun `a row fades only when every channel it rides on is muted`() {
        assertTrue(rowMuted(setOf("alert_severe", "alert_heat"), AlertKind.SEVERE, AlertKind.HEAT))
        assertFalse(rowMuted(setOf("alert_heat"), AlertKind.SEVERE, AlertKind.HEAT))
        assertTrue(rowMuted(setOf("alert_summary"), AlertKind.DAILY_SUMMARY))

        val fromYellow = promisedChannels(content(allOff.copy(officialWarnings = true)))
        assertFalse(warningsRowMuted(fromYellow, setOf("warning_yellow")))
        assertTrue(warningsRowMuted(fromYellow, setOf("warning_yellow", "warning_high")))
        val fromOrange = promisedChannels(
            content(allOff.copy(officialWarnings = true, officialWarningsFrom = WarningLevel.ORANGE))
        )
        assertTrue(warningsRowMuted(fromOrange, setOf("warning_high")))
        assertFalse(warningsRowMuted(promisedChannels(content()), setOf("warning_high", "warning_yellow")))
    }
}
