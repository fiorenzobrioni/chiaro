package com.callbackdev.chiaro.domain

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

    @Test
    fun `pollen grains map to coarse levels`() {
        assertNull(WeatherCodes.pollenLevel(null))
        assertEquals(PollenLevel.NONE, WeatherCodes.pollenLevel(0.0))
        assertEquals(PollenLevel.LOW, WeatherCodes.pollenLevel(7.1))
        assertEquals(PollenLevel.MODERATE, WeatherCodes.pollenLevel(50.0))
        assertEquals(PollenLevel.HIGH, WeatherCodes.pollenLevel(250.0))
    }
}
