package com.callbackdev.chiaro.domain

import com.callbackdev.chiaro.domain.model.DailyForecast
import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.model.WeatherCondition
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/** Today's row as the rest of the day (23 nov 2026, [RestOfDay]), and the day's rules it reads by ([DayLabel]). */
class RestOfDayTest {

    private val zone = ZoneId.of("Europe/Paris")
    private val date = LocalDate.of(2026, 11, 23)

    /** Paris that morning, as the provider's day had it: 1.2 mm in the night, a rain label. */
    private val paris = DailyForecast(
        date = date, highC = 10.0, lowC = 5.0, condition = WeatherCondition(WmoCode.RAIN_SLIGHT.code),
        precipPct = 90, uvIndexMax = 1, precipMm = 1.2, precipHours = 4, snowCm = 0.0, gustMaxKph = 30.0, rainMm = 1.2
    )

    private fun hour(
        h: Int, code: Int, cloud: Int = 5, mm: Double? = 0.0, chance: Int? = 0, snow: Double? = 0.0,
        uv: Double? = null, gust: Double? = null, snowfallHeight: Double? = null
    ) = HourlyForecast(
        time = LocalDateTime.of(date, java.time.LocalTime.of(h, 0)),
        at = date.atTime(h, 0).atZone(zone).toInstant(),
        tempC = 8.0,
        condition = WeatherCondition(code),
        precipChancePct = chance,
        cloudCoverPct = cloud,
        precipMm = mm,
        snowCm = snow,
        uvIndex = uv,
        gustKph = gust,
        snowfallHeightM = snowfallHeight
    )

    private fun daylight(h: HourlyForecast) = h.time.hour in 8..16

    @Test
    fun `a night's rain does not label the rest of a sunny day`() {
        // From 09:00, the hours ahead: clear, a breeze, the sun at its winter low.
        val ahead = (9..23).map { hour(it, WmoCode.CLEAR.code, uv = if (it in 11..14) 1.4 else 0.0, gust = 12.0 + it) }
        val rest = RestOfDay.of(paris, ahead, ::daylight)
        assertEquals(WmoCode.CLEAR.code, rest.condition.wmoCode)
        assertEquals(0, rest.precipPct)
        assertEquals(0.0, rest.precipMm!!, 0.0)
        assertEquals(0.0, rest.rainMm!!, 0.0)
        assertNull("no hours of rain over a strip that draws none", rest.precipHours)
        assertNull(rest.snowLine)
        assertEquals(1, rest.uvIndexMax)
        assertEquals(35.0, rest.gustMaxKph!!, 0.0)
        // The bar keeps the whole day: «now» is drawn on its range.
        assertEquals(10.0, rest.highC, 0.0)
        assertEquals(5.0, rest.lowC, 0.0)
    }

    @Test
    fun `rain still ahead labels the rest of the day by the day's own rules`() {
        val ahead = (12..23).map { h ->
            if (h in 17..19) hour(h, WmoCode.RAIN_SLIGHT.code, cloud = 95, mm = 0.6, chance = 80, snowfallHeight = 1400.0 - h)
            else hour(h, WmoCode.OVERCAST.code, cloud = 95, chance = 30)
        }
        val rest = RestOfDay.of(paris, ahead, ::daylight)
        assertEquals(WmoCode.RAIN_SLIGHT.code, rest.condition.wmoCode)
        assertEquals(80, rest.precipPct)
        assertEquals(1.8, rest.rainMm!!, 1e-9)
        assertEquals(3, rest.precipHours)
        assertEquals(1381, rest.snowLine!!.metres)
    }

    @Test
    fun `a stray hour of drizzle does not label the rest of the day`() {
        // One hour of 0.2 mm: under 1 mm and under 3 hours, the day is its sky.
        val ahead = (12..23).map { h ->
            if (h == 15) hour(h, WmoCode.DRIZZLE_LIGHT.code, cloud = 70, mm = 0.2, chance = 40)
            else hour(h, WmoCode.PARTLY_CLOUDY.code, cloud = 70)
        }
        val rest = RestOfDay.of(paris, ahead, ::daylight)
        assertEquals(WmoCode.PARTLY_CLOUDY.code, rest.condition.wmoCode)
        assertEquals(1, rest.precipHours)
        assertNull("the label is a sky: no snow line", rest.snowLine)
    }

    @Test
    fun `an amount with no snow number is not split into rain`() {
        val ahead = (18..23).map { hour(it, WmoCode.RAIN_SLIGHT.code, cloud = 95, mm = 0.4, chance = 70, snow = null) }
        val rest = RestOfDay.of(paris, ahead, ::daylight)
        assertNull(rest.rainMm)
        assertEquals(2.4, rest.precipMm!!, 1e-9)
    }

    @Test
    fun `no hour left keeps the provider's day`() {
        assertSame(paris, RestOfDay.of(paris, emptyList(), ::daylight))
    }

    @Test
    fun `the day's rules, step by step`() {
        fun h(code: Int?, mm: Double? = 0.0, chance: Int? = 50, day: Boolean = true, cloud: Int? = 50) =
            DayLabel.Hour(code, mm, chance, day, cloud, null)
        // A hazard claims the day, whatever else falls.
        assertEquals(WmoCode.THUNDERSTORM.code, DayLabel.of(listOf(h(0), h(WmoCode.THUNDERSTORM.code), h(61, 3.0)), null))
        // Rain must be material: 1 mm or 3 hours.
        assertEquals(WmoCode.PARTLY_CLOUDY.code, DayLabel.of(listOf(h(61, 0.4), h(2), h(2)), null))
        assertEquals(61, DayLabel.of(listOf(h(61, 0.4), h(61, 0.3), h(61, 0.1)), null))
        // The sky is the daylight's: a grey night does not darken a clear day.
        assertEquals(0, DayLabel.of(listOf(h(3, day = false), h(3, day = false), h(0), h(0)), null))
        // A tie goes to the heavier sky.
        assertEquals(3, DayLabel.of(listOf(h(0), h(3)), null))
        // Nothing to read: the fallback.
        assertEquals(2, DayLabel.of(listOf(h(null)), 2))
    }
}
