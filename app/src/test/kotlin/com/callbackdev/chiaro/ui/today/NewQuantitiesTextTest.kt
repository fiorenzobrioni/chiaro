package com.callbackdev.chiaro.ui.today

import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.model.AirQuality
import com.callbackdev.chiaro.domain.model.AqiScale
import com.callbackdev.chiaro.domain.model.CloudLayers
import com.callbackdev.chiaro.domain.model.Pollutants
import com.callbackdev.chiaro.ui.format.Formats
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

/** The bands and words of the quantities the second pass put on screen (24 set 2026). */
class NewQuantitiesTextTest {

    @Test
    fun `rain bands switch at 0,2 - 2 - 10 - 30 mm`() {
        // Edges as printed: 0,1 / 0,2 mm, 1,9 / 2,0, 9,9 / 10, 29 / 30.
        assertEquals(listOf(0, 0, 1, 1, 2, 2, 3, 3, 4), listOf(0.0, 0.14, 0.2, 1.9, 2.0, 9.9, 10.0, 29.4, 30.0).map(WeatherText::rainBand))
        assertEquals(R.string.rain_meaning_none, WeatherText.rainMeaning(0.0))
        assertEquals(R.string.rain_meaning_umbrella, WeatherText.rainMeaning(5.0))
        assertEquals(R.string.rain_meaning_heavy, WeatherText.rainMeaning(45.0))
    }

    @Test
    fun `snow bands switch at 1 - 5 - 20 cm`() {
        assertEquals(listOf(0, 1, 1, 2, 3), listOf(0.5, 1.0, 4.9, 5.0, 20.0).map(WeatherText::snowBand))
        assertEquals(R.string.snow_meaning_heavy, WeatherText.snowMeaning(25.0))
    }

    @Test
    fun `the cloud's line turns on the layer as much as on the percentage`() {
        assertEquals(R.string.cloud_meaning_open, WeatherText.cloudMeaning(10, null, night = false))
        assertEquals(R.string.cloud_meaning_veiled, WeatherText.cloudMeaning(100, CloudLayers.Layer.HIGH, night = false))
        assertEquals(R.string.cloud_meaning_closed, WeatherText.cloudMeaning(100, CloudLayers.Layer.LOW, night = false))
        assertEquals(R.string.cloud_meaning_closed, WeatherText.cloudMeaning(90, null, night = false))
        assertEquals(R.string.cloud_meaning_broken, WeatherText.cloudMeaning(50, null, night = false))
        assertEquals(R.string.cloud_meaning_veiled_night, WeatherText.cloudMeaning(100, CloudLayers.Layer.HIGH, night = true))
    }

    @Test
    fun `the air is said on the scale the place reads`() {
        val none = Pollutants(null, null, null, null, null, null)
        val milan = AirQuality(aqiIndex = 69, pollutants = none, europeanAqi = 51, scale = AqiScale.EUROPEAN)
        // EEA «moderate» (40-60): nothing to change for the general public.
        assertEquals(R.string.aqi_meaning_moderate, WeatherText.airMeaning(milan))
        assertEquals(R.string.aqi_meaning_sensitive, WeatherText.airMeaning(milan.copy(europeanAqi = 70)))
        assertEquals(R.string.aqi_meaning_hazardous, WeatherText.airMeaning(milan.copy(europeanAqi = 120)))
        // The same 69 on the US scale is its own band.
        assertEquals(R.string.aqi_meaning_moderate, WeatherText.airMeaning(milan.copy(scale = AqiScale.US)))
    }

    @Test
    fun `amounts print one decimal below ten, whole above`() {
        assertEquals("1,4 mm", Formats.millimetres(1.4, Locale.ITALY))
        assertEquals("12 mm", Formats.millimetres(12.4, Locale.ITALY))
        assertEquals("9.8 cm", Formats.centimetres(9.81, Locale.US))
        assertEquals("20 cm", Formats.centimetres(19.81, Locale.US))
    }

    /** The band follows the printed number: «20 cm» is not «under 20» (Everest, 24 set). */
    @Test
    fun `the band is decided on the number the reader sees`() {
        assertEquals(3, WeatherText.snowBand(19.81))
        assertEquals(R.string.snow_meaning_heavy, WeatherText.snowMeaning(19.81))
        assertEquals(4, WeatherText.rainBand(29.6)) // prints «30 mm»
        assertEquals(1, WeatherText.rainBand(0.16)) // prints «0,2 mm»
    }
}
