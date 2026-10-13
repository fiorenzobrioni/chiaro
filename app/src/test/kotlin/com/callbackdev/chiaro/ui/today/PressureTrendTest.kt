package com.callbackdev.chiaro.ui.today

import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.model.WeatherCondition
import com.callbackdev.chiaro.ui.today.WeatherText.PressureTrend.Direction
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The pressure tile's tendency (review of 12 ott 2026): the hour we are in against the
 * one three hours on, on the marine forecasts' bands.
 */
class PressureTrendTest {

    private val zone = ZoneId.of("Europe/Rome")
    private val start = LocalDateTime.of(2026, 9, 25, 12, 0)

    private fun hours(vararg hpa: Double?) = hpa.mapIndexed { i, p ->
        val time = start.plusHours(i.toLong())
        HourlyForecast(
            time = time,
            at = time.atZone(zone).toInstant(),
            tempC = 20.0,
            condition = WeatherCondition(3),
            precipChancePct = 0,
            cloudCoverPct = 86,
            pressureMb = p
        )
    }

    @Test
    fun `the bands`() {
        assertEquals(Direction.STEADY, WeatherText.pressureTrend(hours(1022.0, 1022.0, 1021.6, 1020.7))!!.direction)
        assertEquals(Direction.FALLING, WeatherText.pressureTrend(hours(1012.0, 0.0, 0.0, 1010.0))!!.direction)
        assertEquals(Direction.FALLING_FAST, WeatherText.pressureTrend(hours(1008.0, 0.0, 0.0, 1004.0))!!.direction)
        assertEquals(Direction.RISING, WeatherText.pressureTrend(hours(1004.0, 0.0, 0.0, 1006.5))!!.direction)
        assertEquals(-4.0, WeatherText.pressureTrend(hours(1008.0, 0.0, 0.0, 1004.0))!!.deltaHpa, 1e-9)
    }

    @Test
    fun `no pressure or no hour three on is no tendency`() {
        assertNull(WeatherText.pressureTrend(hours(null, 1.0, 1.0, 1010.0)))
        assertNull(WeatherText.pressureTrend(hours(1010.0, 1.0, 1.0, null)))
        assertNull(WeatherText.pressureTrend(hours(1010.0, 1010.0)))
        assertNull(WeatherText.pressureTrend(emptyList()))
    }

    @Test
    fun `a moving barometer speaks its way, a steady one its reading`() {
        val falling = WeatherText.PressureTrend(Direction.FALLING, -2.0)
        assertEquals(R.string.pressure_trend_falling, WeatherText.pressureMeaning(1025.0, falling))
        val steady = WeatherText.PressureTrend(Direction.STEADY, -1.3)
        assertEquals(R.string.pressure_meaning_high, WeatherText.pressureMeaning(1025.0, steady))
        assertEquals(R.string.pressure_meaning_low, WeatherText.pressureMeaning(995.0, null))
    }
}
