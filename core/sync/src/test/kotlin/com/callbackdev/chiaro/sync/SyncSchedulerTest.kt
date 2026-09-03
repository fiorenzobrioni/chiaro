package com.callbackdev.chiaro.sync

import com.callbackdev.chiaro.domain.settings.NotificationSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The enqueue-vs-cancel decision, pure and table-tested (inherited semantics). */
class SyncSchedulerTest {

    private val allOff = NotificationSettings(
        severeWeatherAlerts = false,
        dailySummary = false,
        precipitationWarning = false,
        userRules = false
    )

    @Test
    fun `system notifications off means nothing is wanted`() {
        assertFalse(
            SyncScheduler.alertsWanted(
                NotificationSettings(), notificationsEnabled = false, hasEnabledRules = true
            )
        )
    }

    @Test
    fun `any builtin toggle keeps the job alive`() {
        assertTrue(
            SyncScheduler.alertsWanted(
                allOff.copy(severeWeatherAlerts = true), notificationsEnabled = true
            )
        )
        assertTrue(
            SyncScheduler.alertsWanted(
                allOff.copy(dailySummary = true), notificationsEnabled = true
            )
        )
        assertTrue(
            SyncScheduler.alertsWanted(
                allOff.copy(precipitationWarning = true), notificationsEnabled = true
            )
        )
    }

    @Test
    fun `an empty rule list must not keep the phone polling`() {
        val onlyRules = allOff.copy(userRules = true)
        assertFalse(SyncScheduler.alertsWanted(onlyRules, true, hasEnabledRules = false))
        assertTrue(SyncScheduler.alertsWanted(onlyRules, true, hasEnabledRules = true))
    }

    @Test
    fun `everything off cancels`() {
        assertFalse(SyncScheduler.shouldRun(allOff, notificationsEnabled = true))
    }

    /** Fase 8: a placed widget is its own reason to keep fetching — it renders
     * instead of notifying, so even disabled notifications do not cancel it. */
    @Test
    fun `a placed widget keeps the job alive on its own`() {
        assertTrue(
            SyncScheduler.shouldRun(allOff, notificationsEnabled = false, hasWidgets = true)
        )
        assertFalse(
            SyncScheduler.shouldRun(allOff, notificationsEnabled = false, hasWidgets = false)
        )
    }
}
