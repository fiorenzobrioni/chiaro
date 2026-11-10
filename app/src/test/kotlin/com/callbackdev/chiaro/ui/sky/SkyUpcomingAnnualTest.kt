package com.callbackdev.chiaro.ui.sky

import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.sky.SkyJobCatalog
import com.callbackdev.chiaro.widget.window.WindowPainter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * An annual moment whose answer falls in the January after its year (10 nov 2026, PLANNING
 * «Dal 1° gennaio lo sciame prossimo è quello dell'anno dopo»): the Quadrantids «of 2026»
 * peak on 3 January 2027, and from the first of January the ones in front of the reader
 * were those of 2028, on their own peak night too.
 */
class SkyUpcomingAnnualTest {

    private val milan = Coordinates(45.4642, 9.19)
    private val rome: ZoneId = ZoneId.of("Europe/Rome")
    private val quadrantids = SkyJobCatalog.byId("meteor.quadrantids.peak")!!

    private fun at(local: String): Instant = LocalDateTime.parse(local).atZone(rome).toInstant()

    @Test
    fun `in January the Quadrantids in front of the reader are this January's`() {
        val before = SkyUpcoming.of(quadrantids, at("2027-01-02T12:00"), rome, milan)
        assertEquals(LocalDate.of(2027, 1, 3), before.at!!.start.atZone(rome).toLocalDate())
        assertFalse(before.inProgress)

        // Through their night they are under way, not a year off.
        listOf("2027-01-03T23:00", "2027-01-04T03:00").forEach {
            val during = SkyUpcoming.of(quadrantids, at(it), rome, milan)
            assertEquals(it, before.at!!.start, during.at!!.start)
            assertTrue(it, during.inProgress)
            assertEquals(it, "meteor.quadrantids.peak", WindowPainter.peakRadiant(at(it), rome, milan, emptySet())?.jobId)
        }

        // Once the night is over, the next ones are a year away.
        val after = SkyUpcoming.of(quadrantids, at("2027-01-04T12:00"), rome, milan)
        assertEquals(2028, after.at!!.start.atZone(rome).year)
    }

    @Test
    fun `a shower that peaks inside its own year is not moved`() {
        val geminids = SkyJobCatalog.byId("meteor.geminids.peak")!!
        val ahead = SkyUpcoming.of(geminids, at("2026-12-13T12:00"), rome, milan)
        assertEquals(LocalDate.of(2026, 12, 13), ahead.at!!.start.atZone(rome).toLocalDate())
        val over = SkyUpcoming.of(geminids, at("2026-12-14T07:00"), rome, milan)
        assertEquals(2027, over.at!!.start.atZone(rome).year)
    }
}
