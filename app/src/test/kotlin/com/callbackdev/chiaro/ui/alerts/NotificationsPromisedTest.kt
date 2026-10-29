package com.callbackdev.chiaro.ui.alerts

import com.callbackdev.chiaro.domain.rules.NotificationRule
import com.callbackdev.chiaro.domain.rules.RuleCondition
import com.callbackdev.chiaro.domain.rules.RuleOp
import com.callbackdev.chiaro.domain.settings.NotificationSettings
import com.callbackdev.chiaro.domain.settings.UnitSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The question the "notifications are off" card is drawn on (21 set 2026).
 *
 * The first test is the bug itself, pinned: a fresh install has alerts switched on
 * before anybody has touched anything, and the permission used to be asked only by the
 * act of switching one on — which that install never does. So the screen promised
 * notifications the phone could not deliver, and said nothing about it. If the
 * defaults ever move, this test is where that shows up.
 *
 * The last one is the other half of the rule: with everything off there is no promise
 * to break, so no card. A screen that nags about a permission it needs for nothing is
 * inventing a problem, which is the same fault in the other direction.
 */
class NotificationsPromisedTest {

    private fun content(
        notifications: NotificationSettings = NotificationSettings(),
        rules: List<NotificationRule> = emptyList(),
        placeGone: Boolean = false
    ) = AlertsUiState.Content(
        placeName = "Milano",
        notifications = notifications,
        rules = rules.map { RuleCardModel(it, lastFired = null, placeGone = placeGone) },
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

    private val allOff = NotificationSettings(
        severeWeatherAlerts = false,
        dailySummary = false,
        eveningSummary = false,
        precipitationWarning = false,
        officialWarnings = false
    )

    @Test
    fun `a fresh install already promises notifications`() {
        assertTrue(notificationsPromised(content()))
    }

    @Test
    fun `each ready-made switch is a promise on its own`() {
        listOf<(NotificationSettings) -> NotificationSettings>(
            { it.copy(severeWeatherAlerts = true) },
            { it.copy(precipitationWarning = true) },
            { it.copy(dailySummary = true) },
            { it.copy(eveningSummary = true) },
            { it.copy(officialWarnings = true) }
        ).forEach { turnOn ->
            assertTrue(notificationsPromised(content(turnOn(allOff))))
        }
    }

    @Test
    fun `an enabled rule of the reader's own is a promise too`() {
        assertTrue(
            notificationsPromised(
                content(allOff, rules = listOf(rule(enabled = true)))
            )
        )
    }

    /** A rule's own switch is the one way for it to be silent (15.6, 28 Oct 2026: the
     * master switch over all of them went), and a rule that is off is no promise. */
    @Test
    fun `a rule that cannot fire promises nothing`() {
        assertFalse(
            notificationsPromised(
                content(allOff, rules = listOf(rule(enabled = false)))
            )
        )
    }

    @Test
    fun `with everything off there is nothing to warn about`() {
        assertFalse(notificationsPromised(content(allOff)))
    }

    /** A rule whose place is gone cannot ring until the reader gives it one (15.11): on until
     * the job's next run switches it off, it promises nothing in the meantime. */
    @Test
    fun `a rule whose place is gone is no promise`() {
        assertFalse(
            notificationsPromised(
                content(allOff, rules = listOf(rule(enabled = true)), placeGone = true)
            )
        )
    }
}
