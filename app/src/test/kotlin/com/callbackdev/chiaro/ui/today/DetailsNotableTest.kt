package com.callbackdev.chiaro.ui.today

import com.callbackdev.chiaro.domain.model.AirQuality
import com.callbackdev.chiaro.domain.model.AqiScale
import com.callbackdev.chiaro.domain.model.PollenLevel
import org.junit.Assert.assertEquals
import org.junit.Test

/** The details grid's one tile to notice (DESIGN §8.6, 15 ott 2026). */
class DetailsNotableTest {

    private val none = com.callbackdev.chiaro.domain.model.Pollutants(null, null, null, null, null, null)

    private fun air(aqiIndex: Int, europeanAqi: Int? = null, scale: AqiScale) =
        AirQuality(aqiIndex = aqiIndex, pollutants = none, europeanAqi = europeanAqi, scale = scale)

    private fun pick(
        rainMm: Double? = 0.0,
        snowCm: Double? = null,
        wind: Double = 12.0,
        dayGust: Double? = 25.0,
        air: AirQuality? = air(aqiIndex = 30, scale = AqiScale.US),
        uv: Int? = 3,
        pollen: PollenLevel? = PollenLevel.LOW,
        fog: Boolean = false,
        dew: Double = 12.0
    ) = notableDetail(rainMm, snowCm, wind, dayGust, air, uv, pollen, fog, dew)

    @Test
    fun `an ordinary day has nothing to notice`() {
        assertEquals(null, pick())
    }

    /** Each threshold is where the tile's own meaning line turns. */
    @Test
    fun `each quantity crosses where its words do`() {
        assertEquals(DetailKind.UV, pick(uv = 6))
        assertEquals(null, pick(uv = 5))
        assertEquals(DetailKind.WIND, pick(wind = 39.0))
        assertEquals(DetailKind.WIND, pick(dayGust = 62.0))
        assertEquals(null, pick(dayGust = 61.0))
        assertEquals(DetailKind.RAIN, pick(rainMm = 10.0))
        assertEquals(null, pick(rainMm = 9.94))
        assertEquals(DetailKind.SNOW, pick(snowCm = 1.0))
        assertEquals(DetailKind.POLLEN, pick(pollen = PollenLevel.HIGH))
        // The visibility tile exists only in fog, the engine's state (27 ott 2026).
        assertEquals(DetailKind.VISIBILITY, pick(fog = true))
        assertEquals(DetailKind.HUMIDITY, pick(dew = 21.0))
    }

    /** The air turns on the scale the place reads: above 100 in the US, above 60 in Europe. */
    @Test
    fun `the air turns on its own scale`() {
        assertEquals(DetailKind.AIR, pick(air = air(aqiIndex = 101, scale = AqiScale.US)))
        assertEquals(null, pick(air = air(aqiIndex = 100, scale = AqiScale.US)))
        assertEquals(DetailKind.AIR, pick(air = air(aqiIndex = 40, europeanAqi = 61, scale = AqiScale.EUROPEAN)))
        assertEquals(null, pick(air = air(aqiIndex = 120, europeanAqi = 60, scale = AqiScale.EUROPEAN)))
    }

    /** When two cross, what changes the day for everyone comes before what a body minds. */
    @Test
    fun `one tile only, in the order of what it asks`() {
        assertEquals(DetailKind.RAIN, pick(rainMm = 15.0, uv = 9, pollen = PollenLevel.VERY_HIGH))
        assertEquals(DetailKind.AIR, pick(air = air(aqiIndex = 160, scale = AqiScale.US), uv = 9))
        assertEquals(DetailKind.UV, pick(uv = 9, pollen = PollenLevel.VERY_HIGH, dew = 23.0))
    }
}
