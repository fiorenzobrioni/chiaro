package com.callbackdev.chiaro.ui.sky

import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.sky.AstronomyEngine
import com.callbackdev.chiaro.domain.sky.MeteorShowerTable
import com.callbackdev.chiaro.domain.sky.MoonQuarterKind
import com.callbackdev.chiaro.domain.sky.Planet
import com.callbackdev.chiaro.domain.sky.SightSubject
import com.callbackdev.chiaro.domain.sky.SkyJob
import com.callbackdev.chiaro.domain.sky.SkyJobCatalog
import com.callbackdev.chiaro.domain.sky.SkyNotScheduled
import com.callbackdev.chiaro.domain.sky.SkyOccurrence
import com.callbackdev.chiaro.domain.sky.SkyScheduler
import com.callbackdev.chiaro.domain.sky.SkyTarget
import com.callbackdev.chiaro.domain.sky.WorthSeeing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * «See that night's sky» (Fase 16, step 16.9): which rows of the calendar ahead have a
 * night of their own, which evening it begins on, the hour the explorer opens at and the
 * thing it opens turned to.
 */
class SkyEventNightsTest {

    private val milan = Coordinates(45.4642, 9.19)
    private val zone = ZoneId.of("Europe/Rome")
    private val now = Instant.parse("2026-11-12T11:00:00Z")

    private fun event(job: SkyJob, start: String, end: String? = null, quarter: MoonQuarterKind? = null) = UpcomingEvent(
        job = job,
        occurrence = SkyOccurrence.At(job, Instant.parse(start), end?.let(Instant::parse)),
        verdict = null,
        quarter = quarter
    )

    private fun shower(id: String): SkyJob = SkyJobCatalog.byId(MeteorShowerTable.jobId(MeteorShowerTable.all.first { it.id == id }))!!

    /** The row as the screen has it: the shower's next night from today. */
    private fun next(job: SkyJob) = UpcomingEvent(
        job = job,
        occurrence = SkyScheduler.next(job, now, zone, milan, limit = 1).first(),
        verdict = null
    )

    /**
     * The Geminids of 2026 peak before dawn on 14 December: their night is the 13th's, the
     * hour is the one the list has for the radiant (when it stands highest, about two in
     * the morning, nearly overhead), and the thing is the radiant.
     */
    @Test
    fun `a shower's night is the evening of its peak night, at the radiant's best hour`() {
        val geminids = shower("geminids")
        val row = next(geminids)
        assertEquals(LocalDate.of(2026, 12, 13), SkyEventNights.dateOf(row, zone))
        val night = SkyEventNights.of(row, zone, milan, emptySet())!!
        assertEquals(LocalDate.of(2026, 12, 13), night.date)
        assertEquals("radiant:${geminids.id}", night.pick)
        val listed = WorthSeeing.of(night.date, zone, milan).first { it.subject is SightSubject.Radiant }
        assertEquals(listed.best, night.at)
        assertEquals(listed.azimuthDeg, night.facingDeg!!, 1e-9)
        val window = row.at!!
        assertTrue("inside the shower's own night", !night.at.isBefore(window.start) && !night.at.isAfter(window.end!!))
        assertTrue("high: ${listed.altitudeDeg}", listed.altitudeDeg > 70.0)
        assertTrue("in the small hours: ${night.at.atZone(zone)}", night.at.atZone(zone).hour in 1..3)
    }

    /** A weak shower nobody follows is on the calendar ahead all the same: its link has the radiant's hour. */
    @Test
    fun `a weak shower has its night and its radiant's hour whether or not it is followed`() {
        val ursids = shower("ursids")
        val row = next(ursids)
        val night = SkyEventNights.of(row, zone, milan, followed = emptySet())!!
        assertEquals("radiant:${ursids.id}", night.pick)
        val listed = WorthSeeing.of(night.date, zone, milan, setOf(ursids.id))
            .first { it.subject is SightSubject.Radiant }
        assertEquals(listed.best, night.at)
        assertNotNull(night.facingDeg)
    }

    @Test
    fun `an instant before noon is the night that began the evening before`() {
        // A full moon at 03:14 in Milan on the 24th is the night of the 23rd; at 15:53, the 24th's.
        val early = event(SkyJobCatalog.MoonFull, "2026-11-24T02:14:00Z")
        val late = event(SkyJobCatalog.MoonFull, "2026-11-24T14:53:00Z")
        assertEquals(LocalDate.of(2026, 11, 23), SkyEventNights.dateOf(early, zone))
        assertEquals(LocalDate.of(2026, 11, 24), SkyEventNights.dateOf(late, zone))
        // The calendar's own full moon, which has the phase's name and not a line of the reader's.
        val calendar = event(SkyJobCatalog.MoonFull, "2026-11-24T14:53:00Z", quarter = MoonQuarterKind.FULL_MOON)
        assertEquals(LocalDate.of(2026, 11, 24), SkyEventNights.dateOf(calendar, zone))
        assertEquals(LocalDate.of(2026, 11, 24), SkyEventNights.dateOf(event(SkyJobCatalog.MoonClosestFull, "2026-11-24T14:53:00Z"), zone))
    }

    /** A full moon's instant is geometry: the hour is the one the moon stands highest that night. */
    @Test
    fun `a full moon opens on the moon at its highest that night`() {
        val night = SkyEventNights.of(event(SkyJobCatalog.MoonFull, "2026-11-24T14:53:00Z"), zone, milan, emptySet())!!
        assertEquals("moon", night.pick)
        val listed = WorthSeeing.of(LocalDate.of(2026, 11, 24), zone, milan).first { it.subject is SightSubject.Moon }
        assertEquals(listed.best, night.at)
        assertTrue(AstronomyEngine.horizontal(SkyTarget.Moon, night.at, milan).altitudeDeg > 40.0)
    }

    @Test
    fun `an eclipse of the moon opens at its middle, and the pairs at their instant, on the fainter of the two`() {
        val eclipse = event(SkyJobCatalog.LunarEclipse, "2028-12-31T15:08:00Z", "2028-12-31T18:36:00Z")
        val night = SkyEventNights.of(eclipse, zone, milan, emptySet())!!
        assertEquals(LocalDate.of(2028, 12, 31), night.date)
        assertEquals(Instant.parse("2028-12-31T16:52:00Z"), night.at)
        assertEquals("moon", night.pick)

        val moonVenus = SkyEventNights.of(event(SkyJobCatalog.MoonVenus, "2026-12-07T05:30:00Z"), zone, milan, emptySet())!!
        // Half past six in the morning: the night that began on the 6th.
        assertEquals(LocalDate.of(2026, 12, 6), moonVenus.date)
        assertEquals(Instant.parse("2026-12-07T05:30:00Z"), moonVenus.at)
        assertEquals("planet:${Planet.VENUS.name}", moonVenus.pick)
        assertEquals(
            AstronomyEngine.horizontal(SkyTarget.Wanderer(Planet.VENUS), moonVenus.at, milan).azimuthDeg,
            moonVenus.facingDeg!!,
            1e-9
        )
        assertEquals("planet:${Planet.JUPITER.name}", SkyEventNights.of(event(SkyJobCatalog.MoonJupiter, "2026-12-07T05:30:00Z"), zone, milan, emptySet())!!.pick)
        assertEquals("planet:${Planet.VENUS.name}", SkyEventNights.of(event(SkyJobCatalog.VenusJupiter, "2026-12-07T05:30:00Z"), zone, milan, emptySet())!!.pick)
        assertEquals("moon", SkyEventNights.of(event(SkyJobCatalog.MoonFullAtDusk, "2026-11-24T16:00:00Z"), zone, milan, emptySet())!!.pick)
    }

    /** A date, a day's event and a sky with nothing of its own in it have no night to open. */
    @Test
    fun `what has no night of its own has no link`() {
        val none = listOf(
            SkyJobCatalog.SolsticeWinter, SkyJobCatalog.EquinoxSpring, SkyJobCatalog.Perihelion,
            SkyJobCatalog.EarliestSunset, SkyJobCatalog.LatestSunrise, SkyJobCatalog.WhiteNightsStart,
            SkyJobCatalog.SolarEclipse, SkyJobCatalog.MoonNew, SkyJobCatalog.MoonFirstQuarter, SkyJobCatalog.MoonPhase
        )
        none.forEach { job ->
            val row = event(job, "2026-12-21T20:00:00Z")
            assertNull(job.id, SkyEventNights.dateOf(row, zone))
            assertNull(job.id, SkyEventNights.of(row, zone, milan, emptySet()))
        }
        // And a row that is a reason and not a date: the Perseids on a night that never gets dark.
        val perseids = shower("perseids")
        val undated = UpcomingEvent(perseids, SkyOccurrence.None(perseids, SkyNotScheduled.NO_DARKNESS), verdict = null)
        assertNull(SkyEventNights.dateOf(undated, zone))
    }

    /** Every row the calendar ahead really builds either has a night inside the next year or has none. */
    @Test
    fun `every shower of the table has its night on the evening its window opens`() {
        MeteorShowerTable.all.forEach { table ->
            val job = SkyJobCatalog.byId(MeteorShowerTable.jobId(table))!!
            val row = next(job)
            val at = row.at ?: return@forEach
            val date = SkyEventNights.dateOf(row, zone)!!
            assertEquals(table.id, at.start.atZone(zone).toLocalDate(), date)
            val dark = SkyScheduler.darkNight(date, zone, milan).night as SkyOccurrence.At
            assertTrue(table.id, !at.start.isAfter(dark.end!!) && !at.end!!.isBefore(dark.start))
        }
    }
}
