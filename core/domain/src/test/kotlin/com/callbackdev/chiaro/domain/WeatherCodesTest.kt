package com.callbackdev.chiaro.domain

import com.callbackdev.chiaro.domain.WeatherCodes.PollenSpecies
import com.callbackdev.chiaro.domain.model.PollenLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeatherCodesTest {

    @Test
    fun `wind degrees map to 16-point compass`() {
        assertEquals("N", WeatherCodes.windCompass(0))
        assertEquals("NW", WeatherCodes.windCompass(310)) // sample value
        assertEquals("E", WeatherCodes.windCompass(90))
        assertEquals("SSW", WeatherCodes.windCompass(202))
        assertEquals("N", WeatherCodes.windCompass(355))
        assertEquals("N", WeatherCodes.windCompass(360))
    }

    /** MeteoSwiss' classes per species (24 set 2026); null in, null out. */
    @Test
    fun `pollen grains map to the species' own classes`() {
        val s = PollenSpecies.RAGWEED
        assertNull(WeatherCodes.pollenLevel(s, null))
        assertEquals(PollenLevel.NONE, WeatherCodes.pollenLevel(s, 0.5))
        assertEquals(PollenLevel.LOW, WeatherCodes.pollenLevel(s, 5.0))
        assertEquals(PollenLevel.MODERATE, WeatherCodes.pollenLevel(s, 6.0))
        assertEquals(PollenLevel.HIGH, WeatherCodes.pollenLevel(s, 11.0))
        assertEquals(PollenLevel.VERY_HIGH, WeatherCodes.pollenLevel(s, 40.0))
        // The same 40 grains of birch are only moderate.
        assertEquals(PollenLevel.MODERATE, WeatherCodes.pollenLevel(PollenSpecies.BIRCH, 40.0))
    }

    @Test
    fun `a family is its worst species, and nothing served is null`() {
        assertEquals(
            PollenLevel.HIGH,
            WeatherCodes.pollenFamilyLevel(
                PollenSpecies.BIRCH to 5.0, PollenSpecies.ALDER to 120.0, PollenSpecies.OLIVE to null
            )
        )
        assertNull(WeatherCodes.pollenFamilyLevel(PollenSpecies.RAGWEED to null, PollenSpecies.MUGWORT to null))
    }
}
