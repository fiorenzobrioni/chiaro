package com.callbackdev.chiaro.data.mapper

import com.callbackdev.chiaro.data.remote.OpenMeteoModelForecastApi
import com.callbackdev.chiaro.data.remote.dto.CurrentDto
import com.callbackdev.chiaro.data.remote.dto.DailyDto
import com.callbackdev.chiaro.data.remote.dto.ForecastResponseDto
import com.callbackdev.chiaro.data.remote.dto.HourlyDto
import com.callbackdev.chiaro.data.remote.dto.ModelCodesHourlyDto
import com.callbackdev.chiaro.data.remote.dto.ModelCodesResponseDto
import com.callbackdev.chiaro.data.remote.dto.UvHourlyDto
import com.callbackdev.chiaro.data.remote.dto.UvResponseDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The new model's week put together (prova del nuovo modello, 21 nov 2026). */
class ForecastComposerTest {

    private val dates = listOf("2026-11-21", "2026-11-22", "2026-11-23")
    private val times = dates.flatMap { d -> (0 until 24).map { h -> "%sT%02d:00".format(d, h) } }
    private val ch = OpenMeteoModelForecastApi.METEOSWISS

    /** A response of [temp] °C every hour; [lastHour] hours have it, the rest are null. */
    private fun response(
        temp: Double,
        lastHour: Int = times.size,
        days: Int = dates.size,
        precip: (Int) -> Double = { 0.0 },
        snow: (Int) -> Double = { 0.0 },
        humidity: Int = 95,
        code: Int = 3,
        dailySnow: Double = 0.0,
        dailyRain: Double = 0.0,
        offset: Int = 3600,
        current: CurrentDto = CurrentDto(time = "2026-11-21T10:15", temperatureC = temp, humidityPct = humidity)
    ): ForecastResponseDto {
        fun <T> hourly(value: (Int) -> T): List<T?> = times.indices.map { if (it < lastHour) value(it) else null }
        fun <T> daily(value: T): List<T?> = dates.indices.map { if (it < days) value else null }
        return ForecastResponseDto(
            latitude = 45.46, longitude = 9.19, timezone = "Europe/Rome", utcOffsetSeconds = offset,
            current = current,
            hourly = HourlyDto(
                time = times,
                temperatureC = hourly { temp },
                weatherCode = hourly { code },
                precipitationMm = hourly(precip),
                precipitationProbabilityPct = hourly { 50 },
                isDay = hourly { 1 },
                visibilityM = hourly { null },
                cloudCoverPct = hourly { 80 },
                humidityPct = hourly { humidity },
                snowfallCm = hourly(snow)
            ),
            daily = DailyDto(
                time = dates,
                weatherCode = daily(code),
                temperatureMaxC = daily(temp + 1),
                temperatureMinC = daily(temp - 1),
                precipitationProbabilityMaxPct = daily(50),
                uvIndexMax = daily(null),
                snowfallSumCm = daily(dailySnow),
                rainSumMm = daily(dailyRain)
            )
        )
    }

    private val ifs = response(temp = 20.0)

    @Test
    fun `the days the regional model has whole are its own, the rest are ECMWF's`() {
        // ICON-CH stops at 13:00 of the second day: the second day is not whole.
        val regional = response(temp = 10.0, lastHour = 24 + 13, days = 2)
        val out = ForecastComposer.compose(regional, ch, ifs, null, null)!!
        assertEquals(10.0, out.hourly.temperatureC[23])
        assertEquals("a day is never half one model's", 20.0, out.hourly.temperatureC[24])
        assertEquals(20.0, out.hourly.temperatureC[30])
        assertEquals(11.0, out.daily.temperatureMaxC[0])
        assertEquals(21.0, out.daily.temperatureMaxC[1])
        assertEquals(listOf(ch, OpenMeteoModelForecastApi.ECMWF), out.composedFrom)
        assertEquals("ECMWF is the hour's model from here, never also a witness", times[24], out.globalFrom)
        assertEquals(regional.minutely15, out.minutely15)
    }

    @Test
    fun `a regional model that covers the week needs no ECMWF in the credit`() {
        val out = ForecastComposer.compose(response(temp = 10.0), ch, ifs, null, null)!!
        assertEquals(listOf(ch), out.composedFrom)
        assertNull(out.globalFrom)
        assertTrue(out.hourly.temperatureC.all { it == 10.0 })
    }

    @Test
    fun `parts that do not line up give nothing, and the caller asks best_match`() {
        val shifted = ifs.copy(hourly = ifs.hourly.copy(time = times.drop(1) + "2026-11-24T00:00"))
        assertNull(ForecastComposer.compose(response(temp = 10.0), ch, shifted, null, null))
        assertNull(ForecastComposer.compose(response(temp = 10.0), ch, response(temp = 20.0, offset = 7200), null, null))
        assertNull("no day whole", ForecastComposer.compose(response(temp = 10.0, lastHour = 20), ch, ifs, null, null))
    }

    @Test
    fun `ICON-D2's storm and freezing rain are written into the hour and its day`() {
        val d2 = ModelCodesResponseDto(
            utcOffsetSeconds = 3600,
            hourly = ModelCodesHourlyDto(
                time = times.take(48),
                weatherCode = times.take(48).indices.map { i -> when (i) { 15 -> 95; 30 -> 66; 31 -> 61; else -> 3 } }
            )
        )
        val out = ForecastComposer.compose(response(temp = 10.0), ch, ifs, d2, null)!!
        assertEquals(95, out.hourly.weatherCode[15])
        assertEquals(66, out.hourly.weatherCode[30])
        assertEquals("only the hazards are read", 3, out.hourly.weatherCode[31])
        assertEquals(95, out.daily.weatherCode[0])
        assertEquals(66, out.daily.weatherCode[1])
        assertEquals(3, out.daily.weatherCode[2])
        assertTrue(OpenMeteoModelForecastApi.HAZARD_MODEL in out.composedFrom!!)
    }

    @Test
    fun `the storm of the hour the current quarter falls in reaches the hero`() {
        val d2 = ModelCodesResponseDto(3600, ModelCodesHourlyDto(time = times.take(48), weatherCode = times.take(48).indices.map { if (it == 11) 95 else 3 }))
        // The block at 10:15 is the hour that ends at 11:00.
        val out = ForecastComposer.compose(response(temp = 10.0), ch, ifs, d2, null)!!
        assertEquals(95, out.current.weatherCode)
    }

    @Test
    fun `the thresholds are Jennings et al's, by humidity`() {
        assertEquals(0.7, ForecastComposer.snowThresholdC(95), 0.0)
        assertEquals(0.7, ForecastComposer.snowThresholdC(90), 0.0)
        assertEquals(1.4, ForecastComposer.snowThresholdC(85), 0.0)
        assertEquals(2.2, ForecastComposer.snowThresholdC(75), 0.0)
        assertEquals(2.8, ForecastComposer.snowThresholdC(65), 0.0)
        assertEquals(3.7, ForecastComposer.snowThresholdC(55), 0.0)
        assertEquals(4.5, ForecastComposer.snowThresholdC(45), 0.0)
        assertEquals("no bin under 40%: the driest stands for it", 4.5, ForecastComposer.snowThresholdC(30), 0.0)
    }

    @Test
    fun `an all-snow hour too warm for snow is rain, and its day says so`() {
        // 6 °C at 95%: the threshold is 0.7 °C. Hour 5 all snow (1 mm as 0.7 cm), hour 6 a part.
        val regional = response(
            temp = 6.0, humidity = 95,
            precip = { if (it == 5 || it == 6) 1.0 else 0.0 },
            snow = { when (it) { 5 -> 0.7; 6 -> 0.3; else -> 0.0 } },
            dailySnow = 1.0, dailyRain = 0.6
        )
        val out = ForecastComposer.compose(regional, ch, ifs, null, null)!!
        assertEquals(0.0, out.hourly.snowfallCm[5])
        assertEquals("a split is the model's own: left alone", 0.3, out.hourly.snowfallCm[6])
        assertEquals(0.3, out.daily.snowfallSumCm[0]!!, 1e-9)
        assertEquals(1.6, out.daily.rainSumMm[0]!!, 1e-9)
    }

    @Test
    fun `snow in air cold enough for it stays snow`() {
        val regional = response(temp = 0.5, humidity = 95, precip = { if (it == 5) 1.0 else 0.0 }, snow = { if (it == 5) 0.7 else 0.0 })
        val out = ForecastComposer.compose(regional, ch, ifs, null, null)!!
        assertEquals(0.7, out.hourly.snowfallCm[5])
    }

    @Test
    fun `the snow goes only when neither end of the hour is cold enough`() {
        val base = response(temp = 6.0, humidity = 95, precip = { if (it == 5) 1.0 else 0.0 }, snow = { if (it == 5) 0.7 else 0.0 })
        // The hour starts at 0 °C and ends at 6 °C.
        val cold = base.copy(hourly = base.hourly.copy(temperatureC = base.hourly.temperatureC.mapIndexed { i, t -> if (i == 4) 0.0 else t }))
        val out = ForecastComposer.compose(cold, ch, ifs, null, null)!!
        assertEquals(0.7, out.hourly.snowfallCm[5])
    }

    @Test
    fun `ECMWF's days keep their snow as it is`() {
        val regional = response(temp = 6.0, lastHour = 24, days = 1)
        val global = response(temp = 6.0, precip = { if (it == 40) 1.0 else 0.0 }, snow = { if (it == 40) 0.7 else 0.0 })
        val out = ForecastComposer.compose(regional, ch, global, null, null)!!
        assertEquals(0.7, out.hourly.snowfallCm[40])
    }

    @Test
    fun `the current block's snow follows the same rule`() {
        val regional = response(
            temp = 6.0,
            current = CurrentDto(time = "2026-11-21T10:15", temperatureC = 6.0, humidityPct = 95, precipitationMm = 0.2, snowfallCm = 0.14)
        )
        val out = ForecastComposer.compose(regional, ch, ifs, null, null)!!
        assertEquals(0.0, out.current.snowfallCm)
    }

    @Test
    fun `the visibility the regional model lacks is ECMWF's, hours and block`() {
        val global = ifs.copy(
            current = ifs.current.copy(visibilityM = 800.0),
            hourly = ifs.hourly.copy(visibilityM = times.indices.map { 10_000.0 + it })
        )
        val out = ForecastComposer.compose(response(temp = 10.0, lastHour = 24, days = 1), ch, global, null, null)!!
        assertEquals(10_005.0, out.hourly.visibilityM[5])
        assertEquals(10_040.0, out.hourly.visibilityM[40])
        assertEquals(800.0, out.current.visibilityM)
    }

    @Test
    fun `the UV is Copernicus's, and a day's maximum only from all its hours`() {
        val cams = UvResponseDto(
            utcOffsetSeconds = 3600,
            hourly = UvHourlyDto(time = times.take(40), uvIndex = times.take(40).indices.map { i -> (i % 24) / 10.0 })
        )
        val out = ForecastComposer.compose(response(temp = 10.0), ch, ifs, null, cams)!!
        assertEquals(1.2, out.hourly.uvIndex[12])
        assertNull(out.hourly.uvIndex[45])
        assertEquals(2.3, out.daily.uvIndexMax[0]!!, 1e-9)
        assertNull("a part of a day is not its maximum", out.daily.uvIndexMax[1])
        assertEquals("the hour the block is in", 1.0, out.current.uvIndex)
        assertTrue(ForecastComposer.UV_SOURCE in out.composedFrom!!)
    }
}
