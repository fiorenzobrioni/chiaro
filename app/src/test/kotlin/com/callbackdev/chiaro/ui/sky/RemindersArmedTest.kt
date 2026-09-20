package com.callbackdev.chiaro.ui.sky

import com.callbackdev.chiaro.domain.sky.SkyJobCatalog
import com.callbackdev.chiaro.domain.sky.SkyLead
import com.callbackdev.chiaro.domain.sky.SkyOccurrence
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The question the Sky screen's "notifications are off" card is drawn on (21 set 2026).
 *
 * Sky is the milder half of the same defect: no reminder ships armed, so a fresh
 * install promises nothing here and the card must stay away. But the moment a bell IS
 * set — and the permission was refused, or switched off in Settings afterwards — the
 * row shows a bell for something that can never ring, which is the same lie Avvisi was
 * telling. A subscribed moment with no lead is not a promise, and neither is the
 * default lead sitting in the settings with nothing following it.
 */
class RemindersArmedTest {

    private val zone: ZoneId = ZoneId.of("Europe/Rome")
    private val now: Instant = Instant.parse("2026-09-21T06:00:00Z")

    private fun moment(lead: SkyLead) = Moment(
        job = SkyJobCatalog.GoldenPm,
        occurrence = SkyOccurrence.At(SkyJobCatalog.GoldenPm, now.plusSeconds(3600)),
        verdict = null,
        lead = lead,
        followsDefault = false,
        timing = MomentTiming.TODAY
    )

    private fun event(lead: SkyLead?) = UpcomingEvent(
        job = SkyJobCatalog.MoonFull,
        occurrence = SkyOccurrence.At(SkyJobCatalog.MoonFull, now.plusSeconds(86_400)),
        verdict = null,
        lead = lead
    )

    private fun content(
        moments: List<Moment> = emptyList(),
        events: List<UpcomingEvent> = emptyList(),
        defaultLead: SkyLead = SkyLead.THIRTY
    ) = SkyUiState.Content(
        placeName = "Milano",
        zone = zone,
        tonight = Tonight(window = null, verdict = null),
        moments = moments,
        events = events,
        defaultLead = defaultLead,
        notifyOnFail = false
    )

    @Test
    fun `a moment with a lead is a promise`() {
        assertTrue(remindersArmed(content(moments = listOf(moment(SkyLead.THIRTY)))))
    }

    @Test
    fun `a row of the calendar ahead with a lead is a promise too`() {
        assertTrue(remindersArmed(content(events = listOf(event(SkyLead.ONE_DAY)))))
    }

    @Test
    fun `a subscribed moment with the bell off promises nothing`() {
        assertFalse(remindersArmed(content(moments = listOf(moment(SkyLead.OFF)))))
    }

    /** A calendar row carries `null` where it is not a subscribed line at all — the
     * next full moon, which is printed for everybody and has no bell to ring. */
    @Test
    fun `a calendar row nobody subscribed to promises nothing`() {
        assertFalse(remindersArmed(content(events = listOf(event(lead = null)))))
    }

    /** The default lead is what a moment ADOPTS when it is subscribed to. On its own,
     * with no row following it, it has announced nothing to anybody. */
    @Test
    fun `the default lead alone is not a promise`() {
        assertFalse(remindersArmed(content(defaultLead = SkyLead.ONE_HOUR)))
    }
}
