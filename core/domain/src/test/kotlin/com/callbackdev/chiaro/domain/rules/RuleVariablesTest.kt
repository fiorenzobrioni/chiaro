package com.callbackdev.chiaro.domain.rules

import com.callbackdev.chiaro.domain.settings.PressureUnit
import com.callbackdev.chiaro.domain.settings.TemperatureUnit
import com.callbackdev.chiaro.domain.settings.UnitSettings
import com.callbackdev.chiaro.domain.settings.WindSpeedUnit
import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.model.WeatherCondition
import com.callbackdev.chiaro.domain.sample.sampleWeatherReport
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleVariablesTest {

    // The sample's hourly rows run 15:00–19:00 on 2023-10-27
    private val now: LocalDateTime = LocalDateTime.of(2023, 10, 27, 14, 30)

    // The sample's days start three days after its own date: re-dated so that the first is
    // today's, the row the `today.*` variables read since 15.1 (28 Oct 2026).
    private val report = sampleWeatherReport().let { sample ->
        sample.copy(
            daily = sample.daily.mapIndexed { i, day -> day.copy(date = now.toLocalDate().plusDays(i.toLong())) }
        )
    }
    private val metric = UnitSettings()
    private val imperial = UnitSettings(
        temperature = TemperatureUnit.FAHRENHEIT,
        windSpeed = WindSpeedUnit.MPH
    )

    private fun resolve(id: String) = RuleVariables.byId(id)!!.resolve(report, now)

    @Test
    fun `current variables mirror the report`() {
        assertEquals(18.5, resolve("current.temp_c")!!.value, 0.0)
        assertEquals(4.0, resolve("current.uv_index")!!.value, 0.0)
        assertEquals(18.0, resolve("current.wind.gust_kph")!!.value, 0.0)
        assertEquals(10.0, resolve("current.precipitation.chance_pct")!!.value, 0.0)
        assertEquals(42.0, resolve("current.aqi_index")!!.value, 0.0)
    }

    @Test
    fun `aqi resolves null when air quality is unavailable`() {
        val noAq = report.copy(airQuality = null)
        assertNull(RuleVariables.byId("current.aqi_index")!!.resolve(noAq, now))
    }

    @Test
    fun `window aggregates pick value and hour`() {
        // max precip chance in the next 6h is 10% at 18:00
        val precip = resolve("next_6h.precip_chance_max")!!
        assertEquals(10.0, precip.value, 0.0)
        assertEquals(LocalDateTime.of(2023, 10, 27, 18, 0), precip.at)
        // min temperature is 14.0 at 19:00
        val tempMin = resolve("next_6h.temp_c_min")!!
        assertEquals(14.0, tempMin.value, 0.0)
        assertEquals(LocalDateTime.of(2023, 10, 27, 19, 0), tempMin.at)
        assertEquals(19.0, resolve("next_6h.temp_c_max")!!.value, 0.0)
    }

    @Test
    fun `window boundary is inclusive and an empty window resolves null`() {
        // 19:00 row sits exactly at now+6h when now is 13:00
        val at13 = LocalDateTime.of(2023, 10, 27, 13, 0)
        val variable = RuleVariables.byId("next_6h.temp_c_min")!!
        assertEquals(14.0, variable.resolve(report, at13)!!.value, 0.0)
        // all rows are in the past relative to a much later now
        assertNull(variable.resolve(report, LocalDateTime.of(2023, 10, 28, 9, 0)))
    }

    @Test
    fun `wmo_severe is a boolean with the triggering hour`() {
        assertEquals(0.0, resolve("next_12h.wmo_severe")!!.value, 0.0)
        val thunder = report.copy(
            hourly = report.hourly + HourlyForecast(
                time = LocalDateTime.of(2023, 10, 27, 20, 0),
                at = LocalDateTime.of(2023, 10, 27, 20, 0)
                    .atZone(ZoneId.of("America/New_York")).toInstant(),
                tempC = 13.0,
                condition = WeatherCondition(95),
                precipChancePct = 90,
                cloudCoverPct = 100
            )
        )
        val severe = RuleVariables.byId("next_12h.wmo_severe")!!.resolve(thunder, now)!!
        assertEquals(1.0, severe.value, 0.0)
        assertEquals(LocalDateTime.of(2023, 10, 27, 20, 0), severe.at)
    }

    @Test
    fun `wmo_severe keeps the alert's chance floor`() {
        val unlikely = report.copy(
            hourly = report.hourly + HourlyForecast(
                time = LocalDateTime.of(2023, 10, 27, 20, 0),
                at = LocalDateTime.of(2023, 10, 27, 20, 0)
                    .atZone(ZoneId.of("America/New_York")).toInstant(),
                tempC = 13.0,
                condition = WeatherCondition(95),
                precipChancePct = 10,
                cloudCoverPct = 100
            )
        )
        val severe = RuleVariables.byId("next_12h.wmo_severe")!!.resolve(unlikely, now)!!
        assertEquals(0.0, severe.value, 0.0)
    }

    @Test
    fun `wmo_severe sees the gale at the place's own bar`() {
        // 14.7: a gust of 75 km/h under a clear sky at 20:00 is «Maltempo» at the sea…
        val gale = report.copy(
            hourly = report.hourly + HourlyForecast(
                time = LocalDateTime.of(2023, 10, 27, 20, 0),
                at = LocalDateTime.of(2023, 10, 27, 20, 0)
                    .atZone(ZoneId.of("America/New_York")).toInstant(),
                tempC = 13.0,
                condition = WeatherCondition(0),
                precipChancePct = 0,
                cloudCoverPct = 0,
                gustKph = 75.0
            )
        )
        val severe = RuleVariables.byId("next_12h.wmo_severe")!!.resolve(gale, now)!!
        assertEquals(1.0, severe.value, 0.0)
        assertEquals(LocalDateTime.of(2023, 10, 27, 20, 0), severe.at)
        // …and not at 1 700 m, where the bar is 100.
        val high = gale.copy(location = gale.location.copy(elevationM = 1700.0))
        assertEquals(0.0, RuleVariables.byId("next_12h.wmo_severe")!!.resolve(high, now)!!.value, 0.0)
    }

    @Test
    fun `today variables read the row dated today, never the first`() {
        assertEquals(20.0, resolve("today.high_c")!!.value, 0.0)
        assertEquals(12.0, resolve("today.low_c")!!.value, 0.0)
        assertEquals(0.0, resolve("today.precip_pct")!!.value, 0.0)
        assertEquals(5.0, resolve("today.uv_max")!!.value, 0.0)  // peak, not current.uv_index (4)
        assertNull(RuleVariables.byId("today.high_c")!!.resolve(report.copy(daily = emptyList()), now))
        // A cache fetched yesterday still leads with yesterday's row (15.1, 28 Oct 2026): «the
        // high of today» is the row dated today, and the card read the first one as today's.
        val yesterday = report.daily.first().copy(date = now.toLocalDate().minusDays(1), highC = 31.0)
        val fromYesterday = report.copy(daily = listOf(yesterday) + report.daily)
        assertEquals(20.0, RuleVariables.byId("today.high_c")!!.resolve(fromYesterday, now)!!.value, 0.0)
        // And a report whose days start tomorrow has no today to read: the rule skips.
        assertNull(RuleVariables.byId("today.high_c")!!.resolve(report.copy(daily = report.daily.drop(1)), now))
    }

    /** 24 set 2026: how much, the strongest gust, the European air. Absent reads null. */
    @Test
    fun `the new quantities resolve, and skip when the data does not carry them`() {
        val today = report.daily.first().copy(precipMm = 16.7, rainMm = 12.4, snowCm = 3.0, gustMaxKph = 61.0)
        val rich = report.copy(
            daily = listOf(today) + report.daily.drop(1),
            hourly = report.hourly.mapIndexed { i, h -> h.copy(gustKph = 20.0 + i * 10) },
            airQuality = report.airQuality!!.copy(europeanAqi = 33)
        )
        fun of(id: String) = RuleVariables.byId(id)!!.resolve(rich, now)
        // The rain alone: the 16.7 mm total carries the 3 cm of snow's water too.
        assertEquals(12.4, of("today.precip_mm")!!.value, 0.0)
        assertEquals(3.0, of("today.snow_cm")!!.value, 0.0)
        assertEquals(61.0, of("today.gust_max_kph")!!.value, 0.0)
        assertEquals(33.0, of("current.aqi_eu_index")!!.value, 0.0)
        val gust = of("next_6h.gust_max_kph")!!
        assertEquals(rich.hourly.last().gustKph!!, gust.value, 0.0)
        assertEquals(rich.hourly.last().time, gust.at)
        // A report that carries none of them: every one skips rather than reading zero.
        val bare = report.copy(
            daily = report.daily.map { it.copy(precipMm = null, rainMm = null, snowCm = null, gustMaxKph = null) },
            hourly = report.hourly.map { it.copy(gustKph = null) },
            airQuality = report.airQuality!!.copy(europeanAqi = null)
        )
        listOf("today.precip_mm", "today.snow_cm", "today.gust_max_kph", "current.aqi_eu_index", "next_6h.gust_max_kph")
            .forEach { assertNull(it, RuleVariables.byId(it)!!.resolve(bare, now)) }
        // Gusts are speeds: they follow the reader's unit, name and value.
        assertEquals(RuleVariableKind.SPEED, RuleVariables.byId("today.gust_max_kph")!!.kind)
    }

    @Test
    fun `display names follow the units setting`() {
        val temp = RuleVariables.byId("current.temp_c")!!
        assertEquals("current.temp_c", RuleVariables.displayId(temp, metric))
        assertEquals("current.temp_f", RuleVariables.displayId(temp, imperial))
        val tempMin = RuleVariables.byId("next_6h.temp_c_min")!!
        assertEquals("next_6h.temp_f_min", RuleVariables.displayId(tempMin, imperial))
        val gust = RuleVariables.byId("current.wind.gust_kph")!!
        assertEquals("current.wind.gust_mph", RuleVariables.displayId(gust, imperial))
        // unit-less numbers never change name
        val uv = RuleVariables.byId("current.uv_index")!!
        assertEquals("current.uv_index", RuleVariables.displayId(uv, imperial))
    }

    @Test
    fun `displayed names map back to canonical ids`() {
        assertEquals("current.temp_c", RuleVariables.canonicalId("current.temp_f"))
        assertEquals("next_6h.temp_c_min", RuleVariables.canonicalId("next_6h.temp_f_min"))
        assertEquals("current.wind.speed_kph", RuleVariables.canonicalId("current.wind.speed_mph"))
        assertEquals("current.uv_index", RuleVariables.canonicalId("current.uv_index"))
        assertNull(RuleVariables.canonicalId("current.nonsense"))
    }

    /**
     * The units of 14.9: every speed unit has its displayed token, every pressure unit
     * too, and each maps back to the one canonical id (`_kph`, `_mb`), so a rule written
     * in knots and read in km/h is the same rule.
     */
    @Test
    fun `the speed and pressure units each have a displayed name over one canonical id`() {
        val gust = RuleVariables.byId("today.gust_max_kph")!!
        val speed = RuleVariables.byId("current.wind.speed_kph")!!
        val pressure = RuleVariables.byId("current.pressure_mb")!!
        assertEquals(RuleVariableKind.PRESSURE, pressure.kind)
        mapOf(
            WindSpeedUnit.KMH to "today.gust_max_kph",
            WindSpeedUnit.MPH to "today.gust_max_mph",
            WindSpeedUnit.M_S to "today.gust_max_ms",
            WindSpeedUnit.KNOTS to "today.gust_max_kn",
            WindSpeedUnit.BEAUFORT to "today.gust_max_bft"
        ).forEach { (unit, shown) ->
            val units = UnitSettings(windSpeed = unit)
            assertEquals(shown, RuleVariables.displayId(gust, units))
            assertEquals("today.gust_max_kph", RuleVariables.canonicalId(shown))
            assertEquals("current.wind.speed_kph", RuleVariables.canonicalId(RuleVariables.displayId(speed, units)))
            // A speed's unit never touches a pressure's name, nor the other way round.
            assertEquals("current.pressure_mb", RuleVariables.displayId(pressure, units))
        }
        mapOf(
            PressureUnit.HPA to "current.pressure_mb",
            PressureUnit.INHG to "current.pressure_inhg",
            PressureUnit.MMHG to "current.pressure_mmhg"
        ).forEach { (unit, shown) ->
            val units = UnitSettings(pressure = unit)
            assertEquals(shown, RuleVariables.displayId(pressure, units))
            assertEquals("current.pressure_mb", RuleVariables.canonicalId(shown))
            assertEquals("today.gust_max_kph", RuleVariables.displayId(gust, units))
        }
        assertNull(RuleVariables.canonicalId("current.pressure_psi"))
        assertNull(RuleVariables.canonicalId("today.gust_max_furlongs"))
    }

    /** Each unit's value converts and comes back; a Beaufort force is its band's lower
     * bound in km/h, so «at least 6» is «at least 39 km/h» and a reading of 45 is 6. */
    @Test
    fun `speeds and pressures convert and round-trip in every unit`() {
        val speed = RuleVariableKind.SPEED
        assertEquals(5.0, RuleVariables.displayValue(speed, 18.0, UnitSettings(windSpeed = WindSpeedUnit.M_S)), 1e-9)
        assertEquals(10.0, RuleVariables.displayValue(speed, 18.52, UnitSettings(windSpeed = WindSpeedUnit.KNOTS)), 1e-9)
        listOf(WindSpeedUnit.MPH, WindSpeedUnit.M_S, WindSpeedUnit.KNOTS).forEach { unit ->
            val units = UnitSettings(windSpeed = unit)
            assertEquals(unit.name, 50.0, RuleVariables.canonicalValue(speed, RuleVariables.displayValue(speed, 50.0, units), units), 1e-9)
        }
        val beaufort = UnitSettings(windSpeed = WindSpeedUnit.BEAUFORT)
        assertEquals(6.0, RuleVariables.displayValue(speed, 45.0, beaufort), 0.0)
        assertEquals(39.0, RuleVariables.canonicalValue(speed, 6.0, beaufort), 0.0)
        assertEquals(0.0, RuleVariables.canonicalValue(speed, 0.0, beaufort), 0.0)
        assertEquals(118.0, RuleVariables.canonicalValue(speed, 12.0, beaufort), 0.0)
        assertEquals(118.0, RuleVariables.canonicalValue(speed, 14.0, beaufort), 0.0)

        val pressure = RuleVariableKind.PRESSURE
        assertEquals(29.92, RuleVariables.displayValue(pressure, 1013.25, UnitSettings(pressure = PressureUnit.INHG)), 0.005)
        assertEquals(760.0, RuleVariables.displayValue(pressure, 1013.25, UnitSettings(pressure = PressureUnit.MMHG)), 0.05)
        assertEquals(1013.25, RuleVariables.displayValue(pressure, 1013.25, metric), 0.0)
        listOf(PressureUnit.INHG, PressureUnit.MMHG).forEach { unit ->
            val units = UnitSettings(pressure = unit)
            assertEquals(unit.name, 1013.25, RuleVariables.canonicalValue(pressure, RuleVariables.displayValue(pressure, 1013.25, units), units), 1e-9)
        }
    }

    /** The decimals follow the unit: a force is whole, inches of mercury keep two. */
    @Test
    fun `formatValue writes a force whole and the inches to the hundredth`() {
        assertEquals("6", RuleVariables.formatValue(RuleVariableKind.SPEED, 45.0, UnitSettings(windSpeed = WindSpeedUnit.BEAUFORT)))
        assertEquals("12.5", RuleVariables.formatValue(RuleVariableKind.SPEED, 45.0, UnitSettings(windSpeed = WindSpeedUnit.M_S)))
        assertEquals("24.3", RuleVariables.formatValue(RuleVariableKind.SPEED, 45.0, UnitSettings(windSpeed = WindSpeedUnit.KNOTS)))
        assertEquals("29.92", RuleVariables.formatValue(RuleVariableKind.PRESSURE, 1013.25, UnitSettings(pressure = PressureUnit.INHG)))
        assertEquals("30", RuleVariables.formatValue(RuleVariableKind.PRESSURE, 1015.92, UnitSettings(pressure = PressureUnit.INHG)))
        assertEquals("760", RuleVariables.formatValue(RuleVariableKind.PRESSURE, 1013.25, UnitSettings(pressure = PressureUnit.MMHG)))
        assertEquals("1013.3", RuleVariables.formatValue(RuleVariableKind.PRESSURE, 1013.25, metric))
    }

    @Test
    fun `values convert and round-trip between units`() {
        val kind = RuleVariableKind.TEMPERATURE
        assertEquals(41.0, RuleVariables.displayValue(kind, 5.0, imperial), 1e-9)
        assertEquals(5.0, RuleVariables.canonicalValue(kind, 41.0, imperial), 1e-9)
        assertEquals(5.0, RuleVariables.displayValue(kind, 5.0, metric), 0.0)
        val speed = RuleVariableKind.SPEED
        assertEquals(10.0, RuleVariables.canonicalValue(
            speed, RuleVariables.displayValue(speed, 10.0, imperial), imperial
        ), 1e-9)
    }

    @Test
    fun `formatValue renders integers bare and booleans as words`() {
        assertEquals("19", RuleVariables.formatValue(RuleVariableKind.NUMBER, 19.0, metric))
        assertEquals("19.5", RuleVariables.formatValue(RuleVariableKind.NUMBER, 19.5, metric))
        assertEquals("65.3", RuleVariables.formatValue(RuleVariableKind.TEMPERATURE, 18.5, imperial))
        assertEquals("true", RuleVariables.formatValue(RuleVariableKind.BOOLEAN, 1.0, metric))
        assertEquals("false", RuleVariables.formatValue(RuleVariableKind.BOOLEAN, 0.0, metric))
    }

    @Test
    fun `every registry id resolves against the sample or declares unavailability`() {
        // The sample's rows carry no feels-like (14.6): the rows are given the air's, so the
        // feels-like aggregates resolve like the rest and a row without one is its own test.
        val rich = report.copy(hourly = report.hourly.map { it.copy(feelsLikeC = it.tempC) })
        RuleVariables.all.forEach { variable ->
            // must not throw; today/current/aggregates all resolve on the sample
            assertTrue(variable.id, variable.resolve(rich, now) != null)
        }
    }

    /** The feels-like high ahead (14.6): the rows' own number, the hours without one left out. */
    @Test
    fun `the feels-like high ahead reads the rows and skips the ones without one`() {
        val hot = report.copy(hourly = report.hourly.mapIndexed { i, h -> h.copy(feelsLikeC = if (i == 2) 31.5 else 20.0 + i) })
        val feels = RuleVariables.byId("next_6h.feels_like_max_c")!!.resolve(hot, now)!!
        assertEquals(31.5, feels.value, 0.0)
        assertEquals(hot.hourly[2].time, feels.at)
        // The sample's rows carry none: the variable does not resolve, the rule skips.
        assertNull(resolve("next_12h.feels_like_max_c"))
        // Said in the reader's unit, like every other temperature.
        val variable = RuleVariables.byId("next_6h.feels_like_max_c")!!
        assertEquals("next_6h.feels_like_max_f", RuleVariables.displayId(variable, imperial))
        assertEquals("next_6h.feels_like_max_c", RuleVariables.canonicalId("next_6h.feels_like_max_f"))
    }
}
