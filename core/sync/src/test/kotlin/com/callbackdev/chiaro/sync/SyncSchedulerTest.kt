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
        // Named rather than left to the default, which is what «allOff» means: the
        // two summaries went ON by default on 21 set 2026 and this fixture would have
        // quietly stopped being all off.
        eveningSummary = false,
        precipitationWarning = false,
        officialWarnings = false
    )

    /** Fase 11: a reader who wants nothing but the Protezione Civile's bulletin keeps
     * the job alive with that one switch. */
    @Test
    fun `the official warnings keep the job alive on their own`() {
        assertTrue(
            SyncScheduler.alertsWanted(
                allOff.copy(officialWarnings = true), notificationsEnabled = true
            )
        )
        assertFalse(
            SyncScheduler.alertsWanted(
                allOff.copy(officialWarnings = true), notificationsEnabled = false
            )
        )
    }

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

    /** An enabled rule of the reader's own keeps the job alive by itself (15.6, 28 Oct
     * 2026: no master switch over the rules any more); none, and the phone does not poll. */
    @Test
    fun `an empty rule list must not keep the phone polling`() {
        assertFalse(SyncScheduler.alertsWanted(allOff, true, hasEnabledRules = false))
        assertTrue(SyncScheduler.alertsWanted(allOff, true, hasEnabledRules = true))
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
