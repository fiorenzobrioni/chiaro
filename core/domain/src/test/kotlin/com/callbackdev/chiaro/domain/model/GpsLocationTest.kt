package com.callbackdev.chiaro.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GpsLocationTest {

    @Test
    fun `gpsLabel formats hemispheres and two decimals`() {
        assertEquals("45.46N 9.19E", Coordinates(45.46, 9.19).gpsLabel)
        assertEquals("33.87S 151.21E", Coordinates(-33.87, 151.21).gpsLabel)
        assertEquals("40.71N 74.01W", Coordinates(40.71, -74.006).gpsLabel)
        assertEquals("0.00N 0.00E", Coordinates(0.0, 0.0).gpsLabel)
    }

    @Test
    fun `toGpsCity uses reverse geocoded name when present`() {
        val city = GeoFix(Coordinates(45.46, 9.19), "Milano", "Lombardia", "Italy").toGpsCity()
        assertEquals(GpsCityId, city.id)
        assertEquals("Milano", city.name)
        assertEquals("Lombardia", city.region)
        assertEquals("Italy", city.country)
        assertNull(city.timezone)
    }

    @Test
    fun `toGpsCity falls back to coordinate label without geocoding`() {
        val city = GeoFix(Coordinates(45.46, 9.19), null, null, null).toGpsCity()
        assertEquals("45.46N 9.19E", city.name)
        assertEquals("45.46N 9.19E", city.label)
    }

    @Test
    fun `two-decimal coordinates map exactly onto cacheKey`() {
        val a = GeoFix(Coordinates(45.46, 9.19), null, null, null).toGpsCity()
        val b = GeoFix(Coordinates(45.46, 9.19), "Milano", null, null).toGpsCity()
        assertEquals(a.cacheKey, b.cacheKey)
        assertEquals("4546:919", a.cacheKey)
    }

    /** Milan to Turin, ~126 km by great circle. One per cent is plenty: the only
     * question this answers is which side of [FixAdoptionMeters] a fix is on. */
    @Test
    fun `distance is right to within a per cent over a hundred kilometres`() {
        val milan = Coordinates(45.4643, 9.1895)
        val turin = Coordinates(45.0703, 7.6869)
        val measured = milan.distanceMetersTo(turin)
        assertTrue("got $measured m", measured in 124_000.0..128_000.0)
        assertEquals(measured, turin.distanceMetersTo(milan), 0.001)
    }

    @Test
    fun `a place is zero metres from itself`() {
        val milan = Coordinates(45.4643, 9.1895)
        assertEquals(0.0, milan.distanceMetersTo(milan), 0.001)
    }

    /**
     * The cacheKey grid is ~1.1 km and the adoption distance is 2 km, so one cell
     * crossing is NOT a new place — which is the whole point of having two numbers.
     */
    @Test
    fun `one cacheKey cell apart is under the adoption distance`() {
        val here = Coordinates(45.46, 9.19)
        val nextCell = Coordinates(45.47, 9.19)
        assertTrue(here.toString(), here.distanceMetersTo(nextCell) < FixAdoptionMeters)

        val twoCellsDiagonally = Coordinates(45.48, 9.21)
        assertTrue(here.distanceMetersTo(twoCellsDiagonally) >= FixAdoptionMeters)
    }

    @Test
    fun `distance survives the antimeridian`() {
        val west = Coordinates(0.0, -179.99)
        val east = Coordinates(0.0, 179.99)
        // 0.02 degrees of longitude at the equator, not most of the way round.
        assertTrue("got ${west.distanceMetersTo(east)} m", west.distanceMetersTo(east) < 3_000.0)
    }
}
