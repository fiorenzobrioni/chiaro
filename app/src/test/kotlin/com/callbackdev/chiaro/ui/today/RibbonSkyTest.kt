package com.callbackdev.chiaro.ui.today

import com.callbackdev.chiaro.domain.WmoCode
import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.model.WeatherCondition
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The ribbon's forecast sky (DESIGN §4, 12 ott 2026), as [DaylightPhases] builds it from a
 * report's hours: where each hour falls, which hours are wet, and the words for them.
 */
class RibbonSkyTest {

    private val zone = ZoneId.of("Europe/Rome")
    private val date = LocalDate.of(2026, 10, 13)

    private fun hour(time: LocalDateTime, code: Int = WmoCode.OVERCAST.code, cloud: Int = 90, pct: Int? = 10) =
        HourlyForecast(
            time = time,
            at = time.atZone(zone).toInstant(),
            tempC = 12.0,
            condition = WeatherCondition(code),
            precipChancePct = pct,
            cloudCoverPct = cloud
        )

    private fun day(codes: Map<Int, Int> = emptyMap(), from: Int = 0) =
        (from..23).map { h -> hour(date.atTime(h, 0), codes[h] ?: WmoCode.OVERCAST.code) }

    @Test
    fun `each hour falls where its label does, and the next midnight closes the day`() {
        val hourly = day() + hour(date.plusDays(1).atStartOfDay(), code = WmoCode.RAIN_HEAVY.code, cloud = 40)
        val sky = DaylightPhases.sky(hourly, date)
        assertEquals(25, sky.size)
        assertEquals(15 / 24f, sky[15].at, 1e-6f)
        assertEquals(1f, sky.last().at, 0f)
        assertEquals(40, sky.last().cloudPct)
        assertFalse("the closing hour belongs to tomorrow's row", sky.last().wet)
    }

    @Test
    fun `wet is something falling, and a likely hour is not`() {
        val hourly = day(mapOf(9 to WmoCode.RAIN_LIKELY.code, 15 to WmoCode.RAIN_SLIGHT.code, 20 to WmoCode.SNOW_SLIGHT.code))
        val sky = DaylightPhases.sky(hourly, date)
        assertFalse(sky[9].wet)
        assertTrue(sky[15].wet)
        assertTrue(sky[20].wet)
        assertEquals(listOf(15, 20), sky.indices.filter { sky[it].wet })
    }

    @Test
    fun `no chance forecast is no tint`() {
        val sky = DaylightPhases.sky(listOf(hour(date.atTime(12, 0), pct = null)), date)
        assertEquals(0, sky.single().precipPct)
    }

    @Test
    fun `a trimmed day starts its sky at the hour we are in`() {
        val sky = DaylightPhases.sky(day(from = 12), date)
        assertEquals(12 / 24f, sky.first().at, 1e-6f)
        assertTrue(DaylightPhases.sky(day(), date.plusDays(3)).isEmpty())
    }

    @Test
    fun `wet hours group into runs by the clock`() {
        val rain = WmoCode.RAIN_MODERATE.code
        val runs = DaylightPhases.wetRuns(day(mapOf(15 to rain, 16 to rain, 17 to rain, 20 to rain)))
        assertEquals(
            listOf(date.atTime(15, 0) to date.atTime(17, 0), date.atTime(20, 0) to date.atTime(20, 0)),
            runs
        )
        assertTrue(DaylightPhases.wetRuns(day()).isEmpty())
    }
}
