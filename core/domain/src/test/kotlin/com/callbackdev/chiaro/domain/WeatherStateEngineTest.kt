package com.callbackdev.chiaro.domain

import com.callbackdev.chiaro.domain.WeatherStateEngine.Hour
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The state engine, rule by rule (25 set 2026). The cases named after a city are hours of
 * the measurement of that morning (`tools/measurements/states-2026-09-25.json`).
 */
class WeatherStateEngineTest {

    private fun state(
        code: Int = 3,
        sky: Int = code,
        mm: Double? = 0.0,
        snow: Double? = 0.0,
        showers: Double? = 0.0,
        pct: Int? = 50,
        t: Double? = 10.0,
        cloud: Int? = 90,
        vis: Double? = 20_000.0,
        persists: Boolean = false,
        likely: Boolean = true
    ) = WeatherStateEngine.state(
        Hour(code, sky, mm, snow, showers, pct, t, cloud, vis, persists),
        likely
    )

    // ---------------------------------------------------------------- 1. thunderstorm

    @Test
    fun `a probable storm keeps the provider's code when it rains`() {
        assertEquals(95, state(code = 95, mm = 2.0, pct = 60))
        assertEquals(96, state(code = 96, mm = 0.1, pct = 20))
    }

    @Test
    fun `a storm over a dry hour is a possible storm, still a hazard`() {
        assertEquals(WmoCode.THUNDERSTORM_POSSIBLE.code, state(code = 95, mm = 0.0, pct = 40))
        assertEquals(AlertEngine.SevereBucket.THUNDER, WmoCode.THUNDERSTORM_POSSIBLE.hazard)
    }

    @Test
    fun `an improbable storm is the hour's sky, as the banner already had it`() {
        assertEquals(3, state(code = 95, mm = 1.0, pct = 10, cloud = 85))
        // No chance at all is no evidence against it.
        assertEquals(95, state(code = 95, mm = 1.0, pct = null))
    }

    // ---------------------------------------------------------------- 2. freezing rain

    @Test
    fun `the provider's freezing rain stands at any chance`() {
        assertEquals(56, state(code = 56, mm = 0.1, pct = 5))
        assertEquals(67, state(code = 67, mm = 4.0, pct = 90))
    }

    @Test
    fun `liquid rain below zero is not deduced to be freezing, not yet`() {
        assertEquals(61, state(code = 61, mm = 1.0, t = -2.0, pct = 80))
    }

    // ------------------------------------------------------- 3. measurable and probable

    @Test
    fun `a trace under twenty percent is the sky — Longyearbyen's snowflake over a low chance`() {
        // 25 Sep, 02:00: code 71, 0.1 mm, 0.07 cm, 10%.
        assertEquals(3, state(code = 71, mm = 0.1, snow = 0.07, pct = 10, cloud = 100))
        // The same hour at 20% is snow.
        assertEquals(71, state(code = 71, mm = 0.1, snow = 0.07, pct = 20))
    }

    @Test
    fun `under the measurable floor nothing falls, whatever the code says`() {
        assertEquals(3, state(code = 71, mm = 0.0, snow = 0.0, pct = 40))
        assertEquals(2, state(code = 51, mm = 0.05, pct = 50, cloud = 60))
    }

    @Test
    fun `an amount under an overcast code is rain — Bari's case B`() {
        // Bari, 25 Sep 10:00: 0.5 mm at 48% under code 3.
        assertEquals(61, state(code = 3, mm = 0.5, showers = 0.0, pct = 48))
    }

    @Test
    fun `the rain ladder is the AMS one, not the provider's drizzle up to 1_3`() {
        assertEquals(51, state(code = 51, mm = 0.1))
        assertEquals(51, state(code = 51, mm = 0.4))
        assertEquals(61, state(code = 53, mm = 0.5))   // London, 0.9 mm under «dense drizzle»
        assertEquals(61, state(code = 55, mm = 2.4))
        assertEquals(63, state(code = 61, mm = 2.5))
        assertEquals(63, state(code = 63, mm = 7.5))
        assertEquals(65, state(code = 63, mm = 7.6))
    }

    @Test
    fun `convective rain is showers at any intensity — Reykjavik's drizzle`() {
        assertEquals(80, state(code = 51, mm = 0.4, showers = 0.4))
        assertEquals(80, state(code = 51, mm = 0.4, showers = 0.2))   // half
        assertEquals(51, state(code = 51, mm = 0.4, showers = 0.1))   // under half
        assertEquals(81, state(code = 80, mm = 3.0, showers = 3.0))
        assertEquals(82, state(code = 81, mm = 9.0, showers = 9.0))
    }

    @Test
    fun `London's rain that already holds its showers is still read right`() {
        // The UK model's `rain` includes the convective part: 1.5 total, 0.6 of it showers.
        assertEquals(61, state(code = 61, mm = 1.5, showers = 0.6))
    }

    @Test
    fun `snow and its ladder come from the snowfall`() {
        assertEquals(71, state(code = 3, mm = 0.1, snow = 0.07))
        assertEquals(73, state(code = 71, mm = 0.4, snow = 0.28))
        assertEquals(75, state(code = 73, mm = 1.2, snow = 0.84))
    }

    @Test
    fun `convective snow is snow showers, even when showers count the same water`() {
        // Longyearbyen 05:00: showers 0.1 and 0.07 cm in a 0.1 mm hour, code 85.
        assertEquals(85, state(code = 85, mm = 0.1, snow = 0.07, showers = 0.1))
        assertEquals(86, state(code = 85, mm = 1.4, snow = 0.98, showers = 1.4))
    }

    @Test
    fun `rain and snow are both there only when both are measurable`() {
        // 0.3 mm of which 0.14 cm snow = 0.2 mm water, 0.1 liquid.
        assertEquals(68, state(code = 71, mm = 0.3, snow = 0.14))
        assertEquals(69, state(code = 73, mm = 3.0, snow = 1.05))
        // 0.07 cm (0.1 mm water) in a 0.1 mm hour: no liquid left, snow.
        assertEquals(71, state(code = 71, mm = 0.1, snow = 0.07))
    }

    @Test
    fun `heavy snow is a warning at any chance, like the banner`() {
        assertEquals(75, state(code = 75, mm = 1.4, snow = 0.98, pct = 5))
        // …while an improbable heavy downpour is not, like the banner.
        assertEquals(3, state(code = 65, mm = 9.0, pct = 15))
    }

    // --------------------------------------------------------------------- 4. likely

    @Test
    fun `a dry hour at sixty percent is likely rain — Tokyo's case C`() {
        assertEquals(WmoCode.RAIN_LIKELY.code, state(code = 2, mm = 0.0, pct = 90, cloud = 60))
        assertEquals(WmoCode.RAIN_LIKELY.code, state(code = 3, mm = 0.0, pct = 60))
        assertEquals(3, state(code = 3, mm = 0.0, pct = 59))
    }

    @Test
    fun `likely is never a fact`() {
        assertEquals(false, WmoCode.RAIN_LIKELY.isPrecipitation)
        assertEquals(true, WmoCode.SNOW_LIKELY.isSnow)
    }

    @Test
    fun `an observation is never likely`() {
        assertEquals(3, state(code = 3, mm = 0.0, pct = 90, likely = false))
    }

    @Test
    fun `a likely hour takes the phase of the nearest measured one`() {
        fun hour(mm: Double, snow: Double, pct: Int, t: Double) =
            Hour(3, 3, mm, snow, 0.0, pct, t, 90, 20_000.0, false)
        // Snow two hours later, rain three hours earlier: the nearer one wins.
        val series = listOf(
            hour(0.5, 0.0, 80, 3.0), hour(0.0, 0.0, 10, 3.0), hour(0.0, 0.0, 10, 3.0),
            hour(0.0, 0.0, 70, 3.0),
            hour(0.0, 0.0, 10, 3.0), hour(0.7, 0.49, 80, 3.0)
        )
        assertEquals(WmoCode.SNOW_LIKELY.code, WeatherStateEngine.states(series)[3])
        // Nothing measured within three hours: the temperature decides, 1 °C.
        assertEquals(WmoCode.SNOW_LIKELY.code, state(code = 3, mm = 0.0, pct = 63, t = 1.0))
        assertEquals(WmoCode.RAIN_LIKELY.code, state(code = 3, mm = 0.0, pct = 63, t = 1.1))
    }

    // ------------------------------------------------------------------------ 5. fog

    @Test
    fun `fog the visibility agrees with stays, fog it contradicts goes`() {
        assertEquals(45, state(code = 45, vis = 400.0))
        assertEquals(2, state(code = 45, vis = 4_000.0, cloud = 70))
    }

    @Test
    fun `fog is invented only where it persists`() {
        assertEquals(3, state(code = 3, vis = 300.0, persists = false))
        assertEquals(45, state(code = 3, vis = 300.0, persists = true))
    }

    @Test
    fun `fog at or below zero is freezing fog — Longyearbyen at minus four`() {
        assertEquals(48, state(code = 45, vis = 200.0, t = -4.2))
        assertEquals(48, state(code = 45, vis = 200.0, t = 0.0))
        assertEquals(45, state(code = 45, vis = 200.0, t = 0.1))
    }

    @Test
    fun `falling precipitation comes before fog`() {
        assertEquals(61, state(code = 61, mm = 1.0, vis = 300.0, persists = true))
    }

    @Test
    fun `a trace under fog is fog, not drizzle — Singapore`() {
        assertEquals(45, state(code = 51, sky = 45, mm = 0.1, pct = 18, vis = 800.0, persists = true))
    }

    // ------------------------------------------------------------------------ 6. sky

    @Test
    fun `the sky is the cloud cover's, on the provider's buckets`() {
        assertEquals(0, state(code = 1, cloud = 19))
        assertEquals(1, state(code = 0, cloud = 20))
        assertEquals(1, state(code = 2, cloud = 49))
        assertEquals(2, state(code = 3, cloud = 50))
        assertEquals(2, state(code = 3, cloud = 79))
        assertEquals(3, state(code = 2, cloud = 80))
    }

    // -------------------------------------------------------------------- fallbacks

    @Test
    fun `without amounts the provider's precipitation code stands, as before the engine`() {
        assertEquals(63, state(code = 63, mm = null, snow = null, showers = null))
        assertEquals(71, state(code = 71, mm = null, snow = null, showers = null, pct = 5))
    }

    @Test
    fun `without snowfall the phase comes from the code, then the temperature`() {
        assertEquals(71, state(code = 73, mm = 0.1, snow = null))
        assertEquals(61, state(code = 61, mm = 1.0, snow = null, showers = null))
        assertEquals(71, state(code = 3, mm = 0.1, snow = null, t = 0.5))
        assertEquals(51, state(code = 3, mm = 0.1, snow = null, t = 5.0))
    }

    @Test
    fun `without cloud cover or visibility the provider's sky and fog stand`() {
        assertEquals(1, state(code = 1, cloud = null))
        assertEquals(45, state(code = 45, vis = null))
        assertEquals(3, state(code = 61, sky = 61, mm = 0.0, cloud = null))
    }

    @Test
    fun `a number outside the table is not a sky, and not wet`() {
        assertEquals(2, state(code = 42, cloud = 60))
        assertEquals(3, state(code = 42, sky = 42, mm = null, cloud = null))
    }
}
