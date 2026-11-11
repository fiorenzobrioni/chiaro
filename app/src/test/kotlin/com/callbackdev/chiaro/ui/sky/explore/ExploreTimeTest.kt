package com.callbackdev.chiaro.ui.sky.explore

import com.callbackdev.chiaro.data.AppSettings
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.sky.MoonOfTheNight
import com.callbackdev.chiaro.domain.sky.SkyOccurrence
import com.callbackdev.chiaro.ui.sky.SkyNights
import com.callbackdev.chiaro.ui.sky.SkyStateBuilder
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The explorer's time (Fase 16, step 16.7): which instant its sky is of, what an hour held
 * on one night becomes on another, which way back the page offers, where the horizon is
 * kept, and how a night that is not tonight is named. Pure functions, no screen.
 */
class ExploreTimeTest {

    private val zone = ZoneId.of("Europe/Rome")
    private val milan = City(
        id = 1L, name = "Milano", region = "Lombardia", country = "Italia",
        coordinates = Coordinates(45.4642, 9.19), timezone = zone.id
    )
    private val stockholm = City(
        id = 2L, name = "Stockholm", region = null, country = "Sverige",
        coordinates = Coordinates(59.3293, 18.0686), timezone = "Europe/Stockholm"
    )
    private val eleventh = LocalDate.of(2026, 11, 11)

    private fun at(day: Int, hour: Int, minute: Int = 0): Instant =
        LocalDateTime.of(2026, 11, day, hour, minute).atZone(zone).toInstant()

    private fun night(date: LocalDate, now: Instant, city: City = milan) =
        SkyStateBuilder.night(city, null, AppSettings(), date, now)

    private fun span(date: LocalDate, now: Instant): SkyOccurrence.At = night(date, now).night!!

    @Test
    fun `the sky is of the hour held, or of now while the night is under way, or of its middle`() {
        val evening = at(11, 23)
        val tonight = night(eleventh, evening)
        val held = at(12, 2, 40)
        assertEquals(held, exploreInstant(tonight, held, evening, true, eleventh, zone, milan.coordinates))
        // Nothing held, at 23:00 of the night itself: now.
        assertEquals(evening, exploreInstant(tonight, null, evening, true, eleventh, zone, milan.coordinates))
        // Nothing held, in the afternoon: the middle of the night, where the hero draws it.
        val afternoon = at(11, 15)
        val middle = exploreInstant(night(eleventh, afternoon), null, afternoon, true, eleventh, zone, milan.coordinates)
        val dark = span(eleventh, afternoon)
        assertEquals(dark.start.plus(Duration.between(dark.start, dark.end).dividedBy(2)), middle)
        // Another night, whatever the clock says: its own middle.
        val other = eleventh.plusDays(3)
        val its = span(other, evening)
        assertEquals(
            its.start.plus(Duration.between(its.start, its.end).dividedBy(2)),
            exploreInstant(night(other, evening), null, evening, false, other, zone, milan.coordinates)
        )
    }

    /** Stockholm on midsummer's eve has no astronomical night: there is no middle to take. */
    @Test
    fun `a date with no dark hours is now for tonight and the midnight that closes it for another`() {
        val swedish = ZoneId.of("Europe/Stockholm")
        val date = LocalDate.of(2026, 6, 23)
        val now = LocalDateTime.of(2026, 6, 23, 22, 30).atZone(swedish).toInstant()
        val white = night(date, now, stockholm)
        assertNull("no astronomical night at 59°N in June", white.night)
        assertEquals(now, exploreInstant(white, null, now, true, date, swedish, stockholm.coordinates))
        val next = date.plusDays(1)
        assertEquals(
            MoonOfTheNight.instant(next, swedish, stockholm.coordinates),
            exploreInstant(night(next, now, stockholm), null, now, false, next, swedish, stockholm.coordinates)
        )
        assertEquals(
            LocalDateTime.of(2026, 6, 25, 0, 0).atZone(swedish).toInstant(),
            MoonOfTheNight.instant(next, swedish, stockholm.coordinates)
        )
    }

    @Test
    fun `an hour held goes to the other night at the same time on the clock, if that night has it`() {
        val now = at(11, 23)
        val held = at(11, 22)
        val next = eleventh.plusDays(1)
        assertEquals(at(12, 22), carriedHour(held, eleventh, next, zone, span(next, now)))
        assertEquals(at(10, 22), carriedHour(held, eleventh, eleventh.minusDays(1), zone, span(eleventh.minusDays(1), now)))
        // After midnight the hour belongs to the morning the night runs into.
        assertEquals(at(15, 3), carriedHour(at(12, 3), eleventh, eleventh.plusDays(3), zone, span(eleventh.plusDays(3), now)))
        // A night that does not have the hour: June's begins after 23:00 in Milan.
        val june = LocalDate.of(2026, 6, 20)
        val short = span(june, now)
        assertTrue("June's night starts after 22:00: ${short.start.atZone(zone)}", short.start.atZone(zone).hour >= 23 || short.start.atZone(zone).hour < 3)
        assertNull(carriedHour(held, eleventh, june, zone, short))
        // And a date with no night has no hour to carry to.
        assertNull(carriedHour(held, eleventh, next, zone, null))
    }

    /** The clocks go back on 25 Oct 2026: the same time on the clock is an hour more of real time away. */
    @Test
    fun `the hour is carried on the clock across the change of the clocks`() {
        val before = LocalDateTime.of(2026, 10, 24, 22, 0).atZone(zone).toInstant()
        val after = LocalDateTime.of(2026, 10, 25, 22, 0).atZone(zone).toInstant()
        assertEquals(Duration.ofHours(25), Duration.between(before, after))
        val to = LocalDate.of(2026, 10, 25)
        assertEquals(after, carriedHour(before, LocalDate.of(2026, 10, 24), to, zone, span(to, before)))
    }

    @Test
    fun `the way back is now while the night is under way, tonight before dusk, and none where the page already is`() {
        val evening = at(11, 23)
        val outside = night(eleventh, evening)
        assertNull("on now, nothing to go back to", exploreReturn(outside, evening, isTonight = true, hourHeld = false))
        assertEquals(ExploreReturn.NOW, exploreReturn(outside, evening, isTonight = true, hourHeld = true))
        assertEquals(ExploreReturn.NOW, exploreReturn(outside, evening, isTonight = false, hourHeld = false))
        assertEquals(ExploreReturn.NOW, exploreReturn(outside, evening, isTonight = false, hourHeld = true))

        val afternoon = at(11, 15)
        val ahead = night(eleventh, afternoon)
        assertNull(exploreReturn(ahead, afternoon, isTonight = true, hourHeld = false))
        assertNull("an hour of tonight held by day: the ribbon is all there is", exploreReturn(ahead, afternoon, isTonight = true, hourHeld = true))
        assertEquals(ExploreReturn.TONIGHT, exploreReturn(ahead, afternoon, isTonight = false, hourHeld = false))
        assertEquals(ExploreReturn.TONIGHT, exploreReturn(ahead, afternoon, isTonight = false, hourHeld = true))
    }

    @Test
    fun `where there is no night at all, now is the way back`() {
        val swedish = ZoneId.of("Europe/Stockholm")
        val date = LocalDate.of(2026, 6, 23)
        val now = LocalDateTime.of(2026, 6, 23, 15, 0).atZone(swedish).toInstant()
        val white = night(date, now, stockholm)
        assertNull(exploreReturn(white, now, isTonight = true, hourHeld = false))
        assertEquals(ExploreReturn.NOW, exploreReturn(white, now, isTonight = false, hourHeld = false))
    }

    @Test
    fun `the horizon is kept a lip over the foot, between the middle of the screen and the camera's own floor`() {
        val h = 2000f
        assertEquals(0.7f, horizonShare(floorPx = 1424f, lipPx = 24f, heightPx = h), 1e-6f)
        // A short foot: the camera's own floor, no lower.
        assertEquals(SkyCamera.HorizonLine, horizonShare(1900f, 24f, h), 0f)
        // A foot taller than half the screen: never over the middle.
        assertEquals(SkyCamera.HighestHorizon, horizonShare(800f, 24f, h), 0f)
        // Nothing across the foot (a wide window), or nothing measured yet.
        assertEquals(SkyCamera.HorizonLine, horizonShare(Float.POSITIVE_INFINITY, 24f, h), 0f)
        assertEquals(SkyCamera.HorizonLine, horizonShare(1424f, 24f, 0f), 0f)
    }

    @Test
    fun `another night is named by its evening, with the year when it is not this one`() {
        assertEquals(
            listOf("Thursday 12 November", "Thu 12 Nov", "12 Nov"),
            otherNightForms(LocalDate.of(2026, 11, 12), 2026, Locale.UK)
        )
        assertEquals(
            listOf("Friday 1 January 2027", "Fri 1 Jan 2027", "1 Jan 2027"),
            otherNightForms(LocalDate.of(2027, 1, 1), 2026, Locale.UK)
        )
        assertEquals("giovedì 12 novembre", otherNightForms(LocalDate.of(2026, 11, 12), 2026, Locale.ITALY).first())
    }

    @Test
    fun `a wide field names the constellations a reader looks for first, a close one all of them`() {
        assertTrue(ExploreNames.figureNamed(1, SkyCamera.MaxFieldDeg))
        assertTrue(!ExploreNames.figureNamed(2, SkyCamera.MaxFieldDeg))
        assertTrue(ExploreNames.figureNamed(2, SkyCamera.RestingFieldDeg))
        assertTrue(!ExploreNames.figureNamed(3, SkyCamera.RestingFieldDeg))
        assertTrue(ExploreNames.figureNamed(3, 45.0))
        assertTrue(ExploreNames.figureNamed(3, SkyCamera.MinFieldDeg))
    }

    /** The state hands the explorer the nights of its place: the hero's own value, for another date. */
    @Test
    fun `the nights of a place are the builder's, for any date`() {
        val now = at(11, 15)
        val nights = SkyNights(milan, null, AppSettings())
        assertEquals(SkyStateBuilder.night(milan, null, AppSettings(), eleventh, now), nights.of(eleventh, now))
        assertEquals(eleventh.plusDays(40), nights.of(eleventh.plusDays(40), now).date)
        // And the state carries them, with the reader's choice of the constellations.
        val content = SkyStateBuilder.build(milan, null, emptyList(), AppSettings(skyConstellations = false), now)
        assertEquals(nights.of(eleventh, now), content.nights!!.of(eleventh, now))
        assertEquals(false, content.constellations)
        assertEquals(true, SkyStateBuilder.build(milan, null, emptyList(), AppSettings(), now).constellations)
    }
}
