package com.callbackdev.chiaro.domain.sky

import com.callbackdev.chiaro.domain.model.Coordinates
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The window a reminder is about, found again from the instant it starts at (10 nov 2026,
 * PLANNING «Il verdetto del promemoria di una finestra è della sua prima ora»).
 */
class SkyOccurrenceAtTest {

    private val milan = Coordinates(45.4642, 9.19)
    private val rome: ZoneId = ZoneId.of("Europe/Rome")

    /** The start as an alarm carries it: whole seconds. */
    private fun alarm(occurrence: SkyOccurrence.At): Instant = Instant.ofEpochSecond(occurrence.start.epochSecond)

    @Test
    fun `a window is found whole from its start`() {
        // A daily window: the dark window of one evening.
        val dark = SkyScheduler.resolve(SkyJobCatalog.DarknessWindow, LocalDate.of(2026, 9, 12), rome, milan) as SkyOccurrence.At
        assertEquals(dark, SkyScheduler.occurrenceAt(SkyJobCatalog.DarknessWindow, alarm(dark), rome, milan))

        // An annual one: the Geminids' night.
        val geminids = SkyJobCatalog.byId("meteor.geminids.peak")!!
        val night = SkyScheduler.resolve(geminids, LocalDate.of(2026, 6, 1), rome, milan) as SkyOccurrence.At
        assertEquals(night, SkyScheduler.occurrenceAt(geminids, alarm(night), rome, milan))

        // And one that falls in the January after its year: the Quadrantids of 2026.
        val quadrantids = SkyJobCatalog.byId("meteor.quadrantids.peak")!!
        val january = SkyScheduler.resolve(quadrantids, LocalDate.of(2026, 6, 1), rome, milan) as SkyOccurrence.At
        assertEquals(2027, january.start.atZone(rome).year)
        assertEquals(january, SkyScheduler.occurrenceAt(quadrantids, alarm(january), rome, milan))
    }

    @Test
    fun `an instant that starts nothing finds nothing`() {
        val geminids = SkyJobCatalog.byId("meteor.geminids.peak")!!
        val night = SkyScheduler.resolve(geminids, LocalDate.of(2026, 6, 1), rome, milan) as SkyOccurrence.At
        assertNull(SkyScheduler.occurrenceAt(geminids, night.start.plus(Duration.ofHours(2)), rome, milan))
    }
}
