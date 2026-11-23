package com.callbackdev.chiaro.data.mapper

import com.callbackdev.chiaro.data.remote.dto.AirQualityCurrentDto
import com.callbackdev.chiaro.data.remote.dto.AirQualityHourlyDto
import com.callbackdev.chiaro.data.remote.dto.CurrentDto
import com.callbackdev.chiaro.data.remote.dto.DailyDto
import com.callbackdev.chiaro.data.remote.dto.ForecastResponseDto
import com.callbackdev.chiaro.data.remote.dto.HourlyDto
import com.callbackdev.chiaro.data.remote.dto.Minutely15Dto
import com.callbackdev.chiaro.data.remote.dto.WitnessResponseDto
import com.callbackdev.chiaro.domain.DayPart
import com.callbackdev.chiaro.domain.SnowLine
import com.callbackdev.chiaro.domain.WmoCode
import com.callbackdev.chiaro.domain.model.AqiScale
import com.callbackdev.chiaro.domain.model.CacheStatus
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.CloudLayers
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.model.PollenLevel
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherReportMapperTest {

    private val city = City(
        id = 5128581,
        name = "New York",
        region = "New York",
        country = "United States",
        coordinates = Coordinates(40.7128, -74.0060),
        timezone = "America/New_York"
    )

    private val fetchedAt = Instant.parse("2026-08-13T18:23:00Z")

    /** 48 hourly slots starting at local midnight; current time 14:23 → window starts at 14:00. */
    private fun forecast(
        currentTime: String = "2026-08-13T14:23",
        hourlyCount: Int = 48,
        dailyCount: Int = 8
    ): ForecastResponseDto {
        val midnight = LocalDateTime.parse("2026-08-13T00:00")
        return ForecastResponseDto(
            latitude = 40.71,
            longitude = -74.01,
            timezone = "America/New_York",
            elevation = 10.0,
            current = CurrentDto(
                time = currentTime,
                temperatureC = 22.4,
                humidityPct = 65,
                apparentTemperatureC = 24.1,
                dewPointC = 15.6,
                precipitationMm = 0.5,
                weatherCode = 2,
                pressureMslHpa = 1013.2,
                windSpeedKph = 12.5,
                windDirectionDeg = 310,
                windGustsKph = 18.3,
                visibilityM = 16090.0,
                cloudCoverPct = 60,
                uvIndex = 5.4
            ),
            hourly = HourlyDto(
                time = List(hourlyCount) { midnight.plusHours(it.toLong()).toString() },
                temperatureC = List(hourlyCount) { 10.0 + it },
                weatherCode = List(hourlyCount) { if (it == 14) 61 else 0 },
                // 2 mm clears the materiality gate of Fase 26, so the aggregation
                // tests below keep testing aggregation and not the gate.
                precipitationMm = List(hourlyCount) { if (it == 14) 2.0 else 0.0 },
                precipitationProbabilityPct = List(hourlyCount) { if (it == 14) 40 else null },
                isDay = List(hourlyCount) { if (it % 24 in 6..19) 1 else 0 },
                visibilityM = List(hourlyCount) { 20_000.0 },
                cloudCoverPct = List(hourlyCount) { 0 }
            ),
            daily = DailyDto(
                time = List(dailyCount) { LocalDate.parse("2026-08-13").plusDays(it.toLong()).toString() },
                weatherCode = List(dailyCount) { 3 },
                temperatureMaxC = List(dailyCount) { 28.0 + it },
                temperatureMinC = List(dailyCount) { 18.0 + it },
                precipitationProbabilityMaxPct = List(dailyCount) { if (it == 0) 55 else null },
                uvIndexMax = List(dailyCount) { 6.0 }
            )
        )
    }

    private fun map(
        forecast: ForecastResponseDto = forecast(),
        airQuality: AirQualityCurrentDto? = null,
        witnesses: WitnessResponseDto? = null,
        pollenHours: AirQualityHourlyDto? = null
    ) = WeatherReportMapper.map(
        city = city,
        forecast = forecast,
        airQuality = airQuality,
        pollenHours = pollenHours,
        fetchedAt = fetchedAt,
        cacheStatus = CacheStatus.MISS,
        witnesses = witnesses
    )

    /**
     * Other models' columns for [forecast]'s hours, as Open-Meteo writes them: for each, the
     * visibility and the humidity of every slot, and a temperature and a cloud of its own,
     * so that none is `best_match`.
     */
    private fun witnessesOf(
        forecast: ForecastResponseDto,
        vararg models: Pair<String, (Int) -> Pair<Double?, Int?>>
    ): WitnessResponseDto {
        val n = forecast.hourly.time.size
        val columns = models.toList().flatMapIndexed { k, (model, at) ->
            val values = List(n, at)
            listOf(
                "\"temperature_2m_$model\":[${List(n) { 5.0 + k }.joinToString(",")}]",
                "\"cloud_cover_$model\":[${List(n) { 40 }.joinToString(",")}]",
                "\"visibility_$model\":[${values.joinToString(",") { it.first?.toString() ?: "null" }}]",
                "\"relative_humidity_2m_$model\":[${values.joinToString(",") { it.second?.toString() ?: "null" }}]"
            )
        }
        return Json.decodeFromString(
            WitnessResponseDto.serializer(),
            """{"hourly":{"time":[${forecast.hourly.time.joinToString(",") { "\"$it\"" }}],${columns.joinToString(",")}}}"""
        )
    }

    /**
     * Two witnesses that see what [forecast]'s own visibility says, hour by hour: for the tests
     * about something else (the day's label, a row's instant), where the fixture's fog is meant
     * to be a fog. Since 17 nov 2026 one model's visibility is not one.
     */
    private fun agreeing(forecast: ForecastResponseDto): WitnessResponseDto {
        val vis = forecast.hourly.visibilityM
        val see: (Int) -> Pair<Double?, Int?> = { i ->
            if ((vis.getOrNull(i) ?: 20_000.0) <= 1_000.0) 200.0 to 99 else 20_000.0 to 90
        }
        return witnessesOf(forecast, "ecmwf_ifs" to see, "knmi_harmonie_arome_europe" to see)
    }

    @Test
    fun `location comes from city and forecast coordinates`() {
        val location = map().location
        assertEquals("New York", location.city)
        assertEquals("New York", location.region)
        assertEquals("United States", location.country)
        // Coordinates are the ones echoed by the forecast API, not the city's.
        assertEquals(40.71, location.coordinates.lat, 0.0)
        assertEquals(-74.01, location.coordinates.lon, 0.0)
        assertEquals("America/New_York", location.timezone)
        assertEquals(LocalDateTime.parse("2026-08-13T14:23"), location.localTime)
        // The provider's elevation (14.7), and null where a response — or a cache entry
        // from before the field was read — has none: never a guess.
        assertEquals(10.0, location.elevationM!!, 0.0)
        assertNull(map(forecast().copy(elevation = null)).location.elevationM)
    }

    @Test
    fun `current conditions convert units and derive labels`() {
        val current = map().current
        // The fixture's quarter holds 0.5 mm under code 2: 2 mm/h, light rain, whatever
        // the code says (WeatherStateEngine, 25 set 2026 — case B for the present).
        assertEquals(61, current.condition.wmoCode)
        assertEquals(22.4, current.tempC, 0.0)
        assertEquals(24.1, current.feelsLikeC, 0.0)
        assertEquals(65, current.humidityPct)
        assertEquals(16.09, current.visibilityKm!!, 1e-9)      // meters → km
        assertEquals(5, current.uvIndex)                     // 5.4 rounds down
        assertEquals(310, current.wind.degree)
        // The chance of the hour under way is the slot that ends after 14:23 — 15:00,
        // which the fixture leaves without one — not 14:00's 40%, which is 13-14's.
        assertNull(current.precipitation.chancePct)
        // `current.precipitation` is the provider's quarter of an hour, named as such.
        assertEquals(0.5, current.precipitation.lastQuarterHourMm!!, 0.0)
    }

    @Test
    fun `hourly window starts at the current hour and keeps every slot after it`() {
        // Slot 0 is the current hour (current_conditions' rain chance, the engines'
        // anchor); the views drop it and still show a full day. Since upstream Fase 16a the
        // window runs to the END of the response instead of stopping after 25: the
        // fixture carries 48 slots and the current hour is index 14, so 34 remain.
        // Since 24 set 2026 a row needs the slot after it (P7b), so the last slot of
        // the response is not a row: 33.
        val hourly = map().hourly
        assertEquals(33, hourly.size)
        assertEquals(LocalDateTime.parse("2026-08-13T14:00"), hourly.first().time)
        assertEquals(LocalDateTime.parse("2026-08-14T22:00"), hourly.last().time)
        assertEquals(24.0, hourly.first().tempC, 0.0)        // 10.0 + index 14
        // The fixture's rain sits in the 14:00 slot, which Open-Meteo means as 13-14:
        // the hour before the one this row describes. The row reads the sky it starts
        // with and the chance of 14-15, which the fixture leaves out.
        assertEquals(0, hourly.first().condition.wmoCode)
        assertNull(hourly.first().precipChancePct)
        // Fase 26: a chance the provider did not forecast stays null. It used to be
        // mapped to 0, which is a forecast of its own and was never made.
        assertNull(hourly[1].precipChancePct)
        // The day still knows about the rain: the daily code reads the slots.
        assertEquals(61, map().daily.first().condition.wmoCode)
        // Night is not the condition's business any more: the slot carries its code
        // and the icon picks the nocturnal drawing from the hour.
        val night = hourly.first { it.time == LocalDateTime.parse("2026-08-14T03:00") }
        assertEquals(0, night.condition.wmoCode)
    }

    @Test
    fun `hourly window spans the whole response when it starts at midnight`() {
        // The upper bound, and the one the constant is named after: at 00:00 nothing
        // is behind the current hour, so a full week of hourly values survives the
        // mapping instead of the single day the app used to keep.
        val report = map(forecast = forecast(currentTime = "2026-08-13T00:12", hourlyCount = 24 * 7))
        assertEquals(24 * 7 - 1, report.hourly.size)
    }

    @Test
    fun `hourly window is clipped when the API returns fewer slots`() {
        // 20 slots total, current hour at index 14 → 6 remain, and the last is no row.
        val report = map(forecast = forecast(hourlyCount = 20))
        assertEquals(5, report.hourly.size)
    }

    @Test
    fun `today's hours before the current one are kept apart, with the engine's states`() {
        // 23 nov 2026: the response starts at the place's midnight, and the ribbon walks back
        // through what the models said for the hours gone by. Same rows as the hours ahead:
        // 13:00 takes the rain of 13-14 from the 14:00 slot, and the engine reads it as rain.
        val report = map()
        val soFar = report.hoursSoFar
        assertEquals((0..13).toList(), soFar.map { it.time.hour })
        assertTrue(soFar.all { it.time.toLocalDate() == LocalDate.parse("2026-08-13") })
        assertEquals(LocalDateTime.parse("2026-08-13T14:00"), report.hourly.first().time)
        val rain = soFar.last()
        assertEquals(2.0, rain.precipMm!!, 0.0)
        assertEquals(40, rain.precipChancePct)
        assertTrue(com.callbackdev.chiaro.domain.WmoCode.isPrecipitation(rain.condition.wmoCode))
        assertEquals(10.0, soFar.first().tempC, 0.0)
        // At midnight nothing of today is over yet.
        assertTrue(map(forecast = forecast(currentTime = "2026-08-13T00:12")).hoursSoFar.isEmpty())
    }

    @Test
    fun `hourly slots carry the cloud cover`() {
        // Fetched since Fase 13c for the fog repair, carried into the domain since
        // 16a for the sky module's verdicts. Same request, same bytes: the mapper
        // simply stopped throwing the column away.
        val base = forecast()
        val n = base.hourly.time.size
        val report = map(
            forecast = base.copy(hourly = base.hourly.copy(cloudCoverPct = List(n) { it }))
        )
        assertEquals(14, report.hourly.first().cloudCoverPct)   // current hour = index 14
        assertEquals(15, report.hourly[1].cloudCoverPct)
    }

    @Test
    fun `daily takes at most 7 days`() {
        val daily = map().daily
        assertEquals(7, daily.size)
        assertEquals(LocalDate.parse("2026-08-13"), daily.first().date)
        assertEquals(28.0, daily.first().highC, 0.0)
        assertEquals(18.0, daily.first().lowC, 0.0)
        // Derived from the day's hours, not from daily.weather_code (3): it rains at hour
        // 14, and rain outranks any sky code.
        assertEquals(61, daily.first().condition.wmoCode)
        assertEquals(55, daily.first().precipPct)
        // The model carried no probability, so neither does the day.
        assertNull(daily[1].precipPct)
        // uv_index_max was fetched and parsed all along but never mapped, so the
        // README's "Today" section fell back to the instant reading (Aug 2026 fix)
        assertEquals(6, daily.first().uvIndexMax)
    }

    /**
     * One local day of hourly codes (index = hour of 2026-08-13), daylight 06:00-19:00
     * like the default fixture, and the code `daily.weather_code` would have carried.
     */
    private fun dayOf(codes: List<Int>, providerCode: Int): ForecastResponseDto {
        val base = forecast()
        val midnight = LocalDateTime.parse("2026-08-13T00:00")
        return base.copy(
            hourly = base.hourly.copy(
                time = List(24) { midnight.plusHours(it.toLong()).toString() },
                temperatureC = List(24) { 20.0 },
                weatherCode = codes,
                // Material by default: these tests are about which hour wins, not
                // about whether the day is wet enough to be called wet (Fase 26). Since
                // 25 set 2026 the amounts also have to BE the code, snow and showers
                // included: WeatherStateEngine reads them, not the number.
                precipitationMm = codes.map { amountOf(it) },
                snowfallCm = codes.map { snowOf(it, amountOf(it)) },
                showersMm = codes.map { showersOf(it, amountOf(it)) },
                precipitationProbabilityPct = List(24) { null },
                isDay = List(24) { if (it in 6..19) 1 else 0 },
                // Kept coherent with the codes, or the fog repair would rewrite them: the
                // aggregation is what these tests are about.
                visibilityM = codes.map { if (it in setOf(45, 48)) 200.0 else 20_000.0 },
                cloudCoverPct = codes.map { if (it >= 45) 100 else it * 30 }
            ),
            daily = base.daily.copy(weatherCode = List(8) { providerCode })
        )
    }

    /** An amount per hour that the state engine reads back as [code]. */
    private fun amountOf(code: Int): Double = when (code) {
        51, 53, 55 -> 0.3
        63, 81 -> 4.0
        65, 82 -> 9.0
        71, 85 -> 0.1
        73 -> 0.5
        75, 86 -> 1.4
        in 56..99 -> 1.5
        else -> 0.0
    }

    private fun snowOf(code: Int, mm: Double): Double =
        if (code in setOf(71, 73, 75, 77, 85, 86)) mm * 0.7 else 0.0

    private fun showersOf(code: Int, mm: Double): Double =
        if (code in setOf(80, 81, 82, 85, 86)) mm else 0.0

    private fun statusOfFirstDay(codes: List<Int>, providerCode: Int) =
        dayOf(codes, providerCode).let { map(forecast = it, witnesses = agreeing(it)) }.daily.first().condition.wmoCode

    /**
     * The default fixture with [span] hours from the CURRENT one (index 14,
     * `hourly.first()`) overridden. Two is the default because Fase 26 will only
     * INVENT fog for an hour whose neighbour is also below the threshold, and almost
     * every case here is about what the repair does with a real run.
     */
    private fun hourAt(
        code: Int,
        visibilityM: Double?,
        cloudPct: Int,
        span: Int = 2
    ): ForecastResponseDto {
        val base = forecast()
        val n = base.hourly.time.size
        val run = 14 until (14 + span)
        return base.copy(
            hourly = base.hourly.copy(
                weatherCode = List(n) { if (it in run) code else 0 },
                // The amount that makes the code true (25 set 2026: the engine reads it).
                precipitationMm = List(n) { if (it in run) amountOf(code) else 0.0 },
                visibilityM = List(n) { if (it in run) visibilityM else 20_000.0 },
                cloudCoverPct = List(n) { if (it in run) cloudPct else 0 }
            )
        )
    }

    private fun statusOfCurrentHour(code: Int, visibilityM: Double?, cloudPct: Int, span: Int = 2) =
        map(forecast = hourAt(code, visibilityM, cloudPct, span)).hourly.first().condition.wmoCode

    @Test
    fun `fog reported with kilometres of visibility falls back on the cloud cover`() {
        // Cavenago, 22 Aug 2026, 01:00: weather_code 45 with 9.76 km of visibility.
        assertEquals(2, statusOfCurrentHour(45, 9760.0, 59))
    }

    @Test
    fun `without other models there is no fog — one model's word`() {
        // 40 m under the provider's own fog code for two hours, and 160 m served as code 3:
        // until 17 nov 2026 both were fog. With no witness to see it too, each is its sky.
        assertEquals(3, statusOfCurrentHour(45, 40.0, 100))
        assertEquals(3, statusOfCurrentHour(3, 160.0, 100))
    }

    @Test
    fun `precipitation is never rewritten by the fog repair`() {
        // Rain is not derived from visibility, and it can rain in fog.
        assertEquals(61, statusOfCurrentHour(61, 40.0, 100))
    }

    @Test
    fun `the provider's fog code alone is the hour's sky`() {
        // The one number measured wrong more often than right (STATES §11).
        assertEquals(0, statusOfCurrentHour(45, null, 10))
    }

    @Test
    fun `current conditions get the same repair as the hours`() {
        val base = forecast()
        val report = map(
            forecast = base.copy(
                current = base.current.copy(
                    weatherCode = 45, visibilityM = 9760.0, cloudCoverPct = 59, precipitationMm = 0.0
                )
            )
        )
        // The JSON printed "Foggy" right above a 9.76 km visibility of its own.
        assertEquals(2, report.current.condition.wmoCode)
        assertEquals(9.76, report.current.visibilityKm!!, 1e-9)
    }

    @Test
    fun `night fog does not label a clear day`() {
        // The reported case: Open-Meteo's daily code is max() over 24 hours, so the fog
        // hours of a Po Valley night outrank 14 hours of sun. Only the daylight votes.
        val codes = List(24) { if (it in 6..19) 0 else 45 }
        assertEquals(0, statusOfFirstDay(codes, providerCode = 45))
    }

    @Test
    fun `rain in daylight outranks the sky`() {
        val codes = List(24) { if (it == 15) 61 else 0 }
        assertEquals(61, statusOfFirstDay(codes, providerCode = 61))
    }

    // ------------------------------------- 17 nov 2026: fog is two models that see it

    /**
     * The default fixture under a closed sky, dry, the provider's [code] everywhere, its own
     * model at [rh] % in [wind] km/h with [ownVis] m of visibility, in the rows and in the
     * current block at 14:23 (row 14) alike.
     */
    private fun closedSky(rh: Int = 95, wind: Double = 3.0, ownVis: Double? = 20_000.0, code: Int = 3): ForecastResponseDto {
        val base = forecast()
        val n = base.hourly.time.size
        return base.copy(
            current = base.current.copy(
                weatherCode = code, visibilityM = ownVis, cloudCoverPct = 98, precipitationMm = 0.0,
                humidityPct = rh, windSpeedKph = wind
            ),
            hourly = base.hourly.copy(
                weatherCode = List(n) { code },
                precipitationMm = List(n) { 0.0 },
                precipitationProbabilityPct = List(n) { 0 },
                visibilityM = List(n) { ownVis },
                cloudCoverPct = List(n) { 98 },
                humidityPct = List(n) { rh },
                windSpeedKph = List(n) { wind }
            )
        )
    }

    /** A witness that sees [vis] m at [rh] % in the slots of [during], 20 km at 90% outside. */
    private fun sees(vis: Double, rh: Int, during: IntRange = 13..15): (Int) -> Pair<Double?, Int?> =
        { i -> if (i in during) vis to rh else 20_000.0 to 90 }

    @Test
    fun `one model's visibility is not a fog, however low — Cavenago, 17 Nov 2026`() {
        // 10:00: ICON-D2 240 m under its own fog code, at 98% in a 3 km/h breeze; IFS 1.4 km,
        // UKMO 18 km, KNMI 5 km, DMI 2.4 km. The airports around saw 2 to 7 km of haze, and
        // the committente, outside, the next village. The own model's vote is one.
        val f = closedSky(rh = 98, wind = 3.0, ownVis = 240.0, code = 45)
        val w = witnessesOf(
            f, "ecmwf_ifs" to sees(1_440.0, 92), "ukmo_global_deterministic_10km" to sees(17_780.0, 94),
            "knmi_harmonie_arome_europe" to sees(5_080.0, 95), "dmi_harmonie_arome_europe" to sees(2_440.0, 97)
        )
        val report = map(forecast = f, witnesses = w)
        assertEquals(3, report.current.condition.wmoCode)
        assertEquals(3, report.hourly.first().condition.wmoCode)
        // The number is the model's all the same; out of fog the tile is not drawn.
        assertEquals(0.24, report.current.visibilityKm!!, 1e-9)
    }

    @Test
    fun `two models are a fog, in the rows and in the present`() {
        // The same morning at 07:00: KNMI 160 m and DMI 220 m, and the own model saturated.
        val f = closedSky(rh = 98, wind = 3.0, ownVis = 80.0, code = 45)
        val w = witnessesOf(
            f, "ecmwf_ifs" to sees(5_280.0, 94), "knmi_harmonie_arome_europe" to sees(160.0, 99),
            "dmi_harmonie_arome_europe" to sees(220.0, 100)
        )
        val report = map(forecast = f, witnesses = w)
        assertEquals(45, report.current.condition.wmoCode)
        // Rows 14 and 15 are in it; row 16 has the own model's vote alone.
        assertEquals(listOf(45, 45, 3), report.hourly.take(3).map { it.condition.wmoCode })
        assertEquals(0.08, report.current.visibilityKm!!, 1e-9)
    }

    @Test
    fun `best_match's visibility is no vote — the same fog with ICON's, without it, or with a witness's copy`() {
        // Before 30 Nov 2026, after it, at Oslo, and under a model fog everywhere: the votes
        // are the witnesses' and the own model's air, so the states are the same.
        fun states(ownVis: (Int) -> Double?): List<Int> {
            val base = closedSky(rh = 95)
            val n = base.hourly.time.size
            val f = base.copy(
                current = base.current.copy(visibilityM = ownVis(14)),
                hourly = base.hourly.copy(visibilityM = List(n, ownVis))
            )
            val w = witnessesOf(f, "ecmwf_ifs" to sees(300.0, 97), "ukmo_global_deterministic_10km" to sees(600.0, 97))
            val r = map(forecast = f, witnesses = w)
            return listOf(r.current.condition.wmoCode) + r.hourly.take(3).map { it.condition.wmoCode }
        }
        val icon = states { 20_000.0 }
        assertEquals(listOf(45, 45, 45, 3), icon)
        assertEquals(icon, states { null })
        assertEquals(icon, states { if (it in 13..15) 300.0 else 20_000.0 })
        assertEquals(icon, states { 100.0 })
    }

    @Test
    fun `two votes in one hour alone are not invented into fog, and the code keeps them`() {
        // Measured 6 Sep 2026 on 23 cities: rewriting isolated hours took the week's fog
        // transitions from 10 to 18. Fog is not one hour long at this resolution.
        fun row14(code14: Int): Int {
            val base = closedSky()
            val f = base.copy(hourly = base.hourly.copy(weatherCode = List(base.hourly.time.size) { if (it == 14) code14 else 3 }))
            val w = witnessesOf(f, "ecmwf_ifs" to sees(300.0, 97, 14..14), "ukmo_global_deterministic_10km" to sees(600.0, 97, 14..14))
            return map(forecast = f, witnesses = w).hourly.first().condition.wmoCode
        }
        assertEquals(3, row14(3))
        assertEquals(45, row14(45))
    }

    @Test
    fun `two votes in two hours in a row are a fog`() {
        val f = closedSky()
        val w = witnessesOf(f, "ecmwf_ifs" to sees(300.0, 97, 14..15), "ukmo_global_deterministic_10km" to sees(600.0, 97, 14..15))
        assertEquals(45, map(forecast = f, witnesses = w).hourly.first().condition.wmoCode)
    }

    @Test
    fun `the present does not invent a fog its own row does not have`() {
        // Lissone, 26 Oct 2026, 09:45: a model fog one hour long, under code 3, and the hero
        // said «Nebbia» over a strip saying «Coperto», under a part-sunny sky with the
        // visibility excellent on the spot. Two votes in row 14 alone: no fog, above or below.
        val f = closedSky()
        val w = witnessesOf(f, "ecmwf_ifs" to sees(300.0, 97, 14..14), "ukmo_global_deterministic_10km" to sees(600.0, 97, 14..14))
        val report = map(forecast = f, witnesses = w)
        assertEquals(3, report.current.condition.wmoCode)
        assertEquals(3, report.hourly.first().condition.wmoCode)
    }

    @Test
    fun `the present's fog persists as its row's does, across a missing hour`() {
        // Two witnesses in fog at 14:00 and 16:00, and the 15:00 slot with no code and no cloud:
        // row 15 is missing. The series is the hours that exist, so row 14's neighbour is row 16,
        // in fog too, and the strip draws it. Until 19 nov 2026 the present counted the votes on
        // the rows with the hole, found no neighbour, and said «Coperto» over a cell saying «Nebbia».
        val base = closedSky()
        val n = base.hourly.time.size
        val f = base.copy(hourly = base.hourly.copy(
            weatherCode = List(n) { if (it == 15) null else 3 },
            cloudCoverPct = List(n) { if (it == 15) null else 98 }
        ))
        val w = witnessesOf(f, "ecmwf_ifs" to sees(300.0, 99, 14..16), "ukmo_global_deterministic_10km" to sees(600.0, 99, 14..16))
        val report = map(forecast = f, witnesses = w)
        assertEquals(45, report.hourly.first().condition.wmoCode)
        assertEquals(45, report.current.condition.wmoCode)
    }

    @Test
    fun `the present's own model votes with the quarter's air`() {
        // One witness in fog from 13:00 to 15:00 and the own model saturated in a calm: two
        // votes, a fog in the strip and above it.
        val f = closedSky(rh = 99, wind = 4.0)
        val w = witnessesOf(f, "knmi_harmonie_arome_europe" to sees(200.0, 99))
        assertEquals(45, map(forecast = f, witnesses = w).current.condition.wmoCode)
        // A quarter in a 20 km/h wind loses its model's vote: the hero is one model's word,
        // and says the sky, while the row keeps its fog.
        val windy = f.copy(current = f.current.copy(windSpeedKph = 20.0))
        val report = map(forecast = windy, witnesses = w)
        assertEquals(3, report.current.condition.wmoCode)
        assertEquals(45, report.hourly.first().condition.wmoCode)
    }

    @Test
    fun `a fog the models agree is short of saturation does not stand`() {
        // Two witnesses under a kilometre, but the air the three models agree on (95, 85 and
        // 84 without the highest and the lowest: 85%) is not a fog's.
        val f = closedSky(rh = 95)
        val w = witnessesOf(f, "ecmwf_ifs" to sees(300.0, 85), "ukmo_global_deterministic_10km" to sees(600.0, 84))
        val report = map(forecast = f, witnesses = w)
        assertEquals(3, report.current.condition.wmoCode)
        assertEquals(3, report.hourly.first().condition.wmoCode)
    }

    // ------------------------------- Fase 26: the day is claimed by material rain only

    /** [mm] per precipitation hour; the codes place them. */
    private fun dayOfWith(codes: List<Int>, mm: Double, providerCode: Int): Int {
        val base = dayOf(codes, providerCode)
        return map(
            forecast = base.copy(
                hourly = base.hourly.copy(
                    precipitationMm = codes.map { if (it >= 51) mm else 0.0 }
                )
            )
        ).daily.first().condition.wmoCode
    }

    @Test
    fun `one hour of drizzle does not label the whole day`() {
        // Singapore, 6 Sep 2026: one hour, 0.1 mm, 1% probability, and the week table
        // said Drizzle. Milan the same day said Rain Showers for 0.0 mm.
        val codes = List(24) { if (it == 9) 51 else 0 }
        assertEquals(0, dayOfWith(codes, mm = 0.1, providerCode = 51))
    }

    @Test
    fun `a drizzle that lasts the afternoon does`() {
        // 0.4 mm in total, under the millimetre — but four hours of it, which is what
        // a drizzly day looks like. The hour count is the escape hatch.
        val codes = List(24) { if (it in 13..16) 51 else 0 }
        assertEquals(51, dayOfWith(codes, mm = 0.1, providerCode = 51))
    }

    @Test
    fun `a single hour that really rains does`() {
        val codes = List(24) { if (it == 9) 61 else 0 }
        assertEquals(61, dayOfWith(codes, mm = 1.2, providerCode = 61))
    }

    @Test
    fun `a hazard claims the day whatever falls`() {
        // The gate may remove a distortion, never a warning: a dry thunderstorm is
        // still a thunderstorm, and it does not have to clear a millimetre to say so.
        // Since 25 set 2026 a storm over a dry hour is «Temporali possibili» — still the
        // hazard, still the day's label.
        val codes = List(24) { if (it == 9) 95 else 0 }
        assertEquals(WmoCode.THUNDERSTORM_POSSIBLE.code, dayOfWith(codes, mm = 0.0, providerCode = 95))
    }

    @Test
    fun `a cache entry with no amounts falls back on the hour count`() {
        // An entry written before `precipitation` was requested: the amount clause
        // cannot speak, and the day is judged on how long the rain runs.
        val base = dayOf(List(24) { if (it in 13..16) 51 else 0 }, providerCode = 51)
        val noAmounts = base.copy(hourly = base.hourly.copy(precipitationMm = emptyList()))
        assertEquals(51, map(forecast = noAmounts).daily.first().condition.wmoCode)
    }

    @Test
    fun `rain that only falls at night still labels the day`() {
        // Only the sky is the daylight's. Scoping the rain to it too silently turned 8
        // nocturnal thunderstorms into "Overcast" across the 8 cities measured — the rule
        // may remove a distortion, never a warning.
        val codes = List(24) { if (it in 6..19) 3 else 95 }
        assertEquals(95, statusOfFirstDay(codes, providerCode = 95))
    }

    @Test
    fun `a day that really is foggy still reads foggy`() {
        // No special case for 45: it wins on its own when it is the day's usual sky.
        val codes = List(24) { if (it in 6..15) 45 else 3 }
        assertEquals(45, statusOfFirstDay(codes, providerCode = 45))
    }

    @Test
    fun `a tie between two skies goes to the lighter one`() {
        // 23 nov 2026 (DayLabel): measured against the satellite, the lighter is the sky seen.
        val codes = List(24) { if (it in 6..12) 1 else 3 }
        assertEquals(1, statusOfFirstDay(codes, providerCode = 3))
    }

    @Test
    fun `days the hourly run does not reach keep the provider code`() {
        // 48 hourly slots cover two days; the rest of the week falls back on daily.
        val daily = map().daily
        assertEquals(0, daily[1].condition.wmoCode)     // derived: clear all day
        assertEquals(3, daily[2].condition.wmoCode)  // provider's code 3
    }

    // ------------------------ 24 set 2026: severity and phase, not the WMO number (P2)

    /** The first day's code for [codes], with [mm] and [chance] per hour of the day. */
    private fun dayCode(
        codes: List<Int>,
        mm: (Int) -> Double = { amountOf(codes[it]) },
        chance: (Int) -> Int? = { null }
    ): Int {
        val base = dayOf(codes, providerCode = 3)
        val f = base.copy(
                hourly = base.hourly.copy(
                    precipitationMm = List(24, mm),
                    snowfallCm = List(24) { snowOf(codes[it], mm(it)) },
                    showersMm = List(24) { showersOf(codes[it], mm(it)) },
                    precipitationProbabilityPct = List(24, chance)
                )
            )
        return map(forecast = f, witnesses = agreeing(f)).daily.first().condition.wmoCode
    }

    @Test
    fun `a snowy day with one shower is snow, not showers`() {
        // The number put 80 above 73: max() printed «Rovesci» over a day of snow.
        val codes = List(24) { if (it in 8..15) 73 else if (it == 17) 80 else 3 }
        assertEquals(73, dayCode(codes))
    }

    @Test
    fun `heavy snow is not outranked by a slight shower`() {
        val codes = List(24) { if (it in 8..10) 75 else if (it == 17) 80 else 3 }
        assertEquals(75, dayCode(codes))
    }

    @Test
    fun `heavy rain is not outranked by slight showers`() {
        val codes = List(24) { if (it in 8..10) 65 else if (it in 14..15) 80 else 3 }
        assertEquals(65, dayCode(codes))
    }

    @Test
    fun `rain and showers of the same grade go to the one more hours carry`() {
        assertEquals(80, dayCode(List(24) { if (it in 8..9) 61 else if (it in 12..15) 80 else 3 }))
        assertEquals(61, dayCode(List(24) { if (it in 8..11) 61 else if (it in 14..15) 80 else 3 }))
    }

    @Test
    fun `the phase that brought more water names the day`() {
        // Four hours of light snow, 0.2 mm each, against two of moderate rain at 3 mm:
        // it was a day of rain with some snow in it, not the other way round.
        val codes = List(24) { if (it in 6..9) 71 else if (it in 14..15) 63 else 3 }
        assertEquals(63, dayCode(codes, mm = { if (it in 6..9) 0.2 else if (it in 14..15) 3.0 else 0.0 }))
        // Without amounts the hours decide: four of snow beat two of rain.
        val base = dayOf(codes, providerCode = 3)
        val noAmounts = base.copy(hourly = base.hourly.copy(precipitationMm = emptyList()))
        assertEquals(71, map(forecast = noAmounts).daily.first().condition.wmoCode)
    }

    // ------------------------------------------- 17 nov 2026: the veiled sky

    /** The first day's label for a dry day whose every hour has [cover]% of cloud, of which
     * [low]% low and [high]% high, hour by hour. */
    private fun skyDay(cover: (Int) -> Int, low: (Int) -> Int, high: (Int) -> Int): Int {
        val base = dayOf(List(24) { 3 }, providerCode = 3)
        val f = base.copy(
            hourly = base.hourly.copy(
                cloudCoverPct = List(24, cover),
                cloudCoverLowPct = List(24, low),
                cloudCoverMidPct = List(24) { 0 },
                cloudCoverHighPct = List(24, high)
            )
        )
        return map(forecast = f, witnesses = agreeing(f)).daily.first().condition.wmoCode
    }

    @Test
    fun `a day of high veil is a veiled day, not a mostly clear one`() {
        assertEquals(WmoCode.VEILED.code, skyDay(cover = { 100 }, low = { 0 }, high = { 100 }))
        // A low deck the whole day takes the light: overcast, as before.
        assertEquals(WmoCode.OVERCAST.code, skyDay(cover = { 100 }, low = { 90 }, high = { 100 }))
    }

    @Test
    fun `a tie between two skies goes by their weight, not by their number`() {
        // Daylight is 06-19, fourteen hours: seven veiled, seven under a low deck. By its
        // number (1003) the veil would win any tie; by weight it is the lighter of the two, and
        // the tie goes to the lighter since 23 nov 2026.
        val deck = { h: Int -> if (h in 13..19) 90 else 0 }
        assertEquals(WmoCode.VEILED.code, skyDay(cover = { 100 }, low = deck, high = { 100 }))
        // Against seven hours of mostly clear sky (75% of high cloud, 22% of the light) the
        // veil is the heavier, and mostly clear takes the tie.
        val veilFrom13 = { h: Int -> if (h in 13..19) 100 else 75 }
        assertEquals(WmoCode.MAINLY_CLEAR.code, skyDay(cover = veilFrom13, low = { 0 }, high = veilFrom13))
    }

    // ------------------------------ 24 set 2026: the banner's chance floor (P5)

    @Test
    fun `a storm labels its day at any chance of rain`() {
        // Until 17 nov 2026 a storm under 20% was dropped, by the banner and by the week:
        // against the airports' thunder it is a storm as often as one over 20%.
        val codes = List(24) { if (it == 15) 95 else 0 }
        assertEquals(95, dayCode(codes, chance = { if (it == 15) 10 else 0 }))
        assertEquals(95, dayCode(codes, chance = { if (it == 15) 25 else 0 }))
        // No chance at all is not evidence against it (§1.1): the storm stands.
        assertEquals(95, dayCode(codes, chance = { null }))
    }

    @Test
    fun `an improbable downpour's millimetres do not make the day wet`() {
        val codes = List(24) { if (it == 15) 65 else 0 }
        assertEquals(0, dayCode(codes, mm = { if (it == 15) 9.0 else 0.0 }, chance = { 5 }))
    }

    @Test
    fun `ice needs no chance at all`() {
        val codes = List(24) { if (it == 7) 56 else 0 }
        assertEquals(56, dayCode(codes, mm = { 0.0 }, chance = { 2 }))
    }

    @Test
    fun `precipitation that did not earn the day does not win its sky either`() {
        // Daylight 06-19, two hours apiece of every sky and two of drizzle: the old
        // tie-break handed the day to 51. Its hours now vote with their cloud (100% → 3).
        // (48 left the list on 25 set 2026: it is the engine's freezing fog now, which
        // needs ≤ 0 °C, and at this fixture's 20 °C two more hours of it are plain fog.)
        val daylight = listOf(0, 0, 1, 1, 2, 2, 3, 3, 51, 51, 45, 45, 0, 0)
        val codes = List(24) { if (it in 6..19) daylight[it - 6] else 0 }
        // Four clear hours tie with four overcast ones (the drizzle's two among them), and the
        // tie goes to the lighter since 23 nov 2026: the drizzle wins nothing either way.
        assertEquals(0, dayCode(codes, mm = { if (codes[it] == 51) 0.1 else 0.0 }))
    }

    // ------------------------------------------- 24 set 2026: the rain around now (P1, P7)

    @Test
    fun `the closed hours come from the hourly series, each with its end`() {
        // Fixture: current 14:23 New York (EDT, UTC-4); slot 14 holds 2 mm, the rest 0.
        val rain = map().current.precipitation
        assertEquals(
            listOf("2026-08-13T18:00:00Z", "2026-08-13T17:00:00Z", "2026-08-13T16:00:00Z"),
            rain.pastHours.map { it.endedAt.toString() }
        )
        assertEquals(listOf(2.0, 0.0, 0.0), rain.pastHours.map { it.precipMm })
        assertEquals(listOf(24.0, 23.0, 22.0), rain.pastHours.map { it.tempC })
    }

    @Test
    fun `no amounts in the response, no closed hours - not three dry ones`() {
        val base = forecast()
        val report = map(forecast = base.copy(hourly = base.hourly.copy(precipitationMm = emptyList())))
        assertEquals(emptyList<Any>(), report.current.precipitation.pastHours)
    }

    @Test
    fun `just after midnight only the hours the response holds are closed`() {
        val rain = map(forecast = forecast(currentTime = "2026-08-13T00:20")).current.precipitation
        assertEquals(1, rain.pastHours.size) // 23:00-00:00, labelled 00:00
    }

    @Test
    fun `the chance now is the hour under way, not the one gone`() {
        val base = forecast()
        val n = base.hourly.time.size
        val chances = base.copy(hourly = base.hourly.copy(precipitationProbabilityPct = List(n) { it * 2 }))
        // 14:23 → the slot labelled 15:00 (14-15), index 15.
        assertEquals(30, map(forecast = chances).current.precipitation.chancePct)
        // On the hour the one ahead too: at 14:00 the hour 13-14 is over.
        assertEquals(30, map(forecast = chances.copy(current = chances.current.copy(time = "2026-08-13T14:00")))
            .current.precipitation.chancePct)
    }

    @Test
    fun `the hero's quarter is read with the chance of its own hour`() {
        // Cavenago di Brianza, 17 Nov 2026, 13:12: the block is 12:45-13:00, 0.1 mm (0.4 mm/h),
        // in an hour at 38%; the hour under way, 13-14, is at 5%. Read with the 5% the hero
        // said «Coperto» over drizzle, and it was drizzling.
        val base = forecast()
        val n = base.hourly.time.size
        val f = base.copy(
            current = base.current.copy(time = "2026-08-13T15:00", precipitationMm = 0.1, weatherCode = 61, cloudCoverPct = 100),
            hourly = base.hourly.copy(precipitationProbabilityPct = List(n) { if (it == 15) 38 else if (it == 16) 5 else null })
        )
        val report = map(forecast = f)
        assertEquals(51, report.current.condition.wmoCode)
        // The stated chance is still the hour under way's.
        assertEquals(5, report.current.precipitation.chancePct)
        // Past the first quarter the block's hour is the one under way: the same 5%, a trace.
        val later = f.copy(current = f.current.copy(time = "2026-08-13T15:15"))
        assertEquals(3, map(forecast = later).current.condition.wmoCode)
    }

    /**
     * The block 14:15–14:30 at 0.0 mm, inside the hour 14–15: 0.1 mm at 47%, slot 15. The
     * quarters from 14:15 are [quarters]', the hour's four first.
     */
    private fun lightHour(vararg quarterMm: Double, blockMm: Double = 0.0, time: String = "2026-08-13T14:30"): ForecastResponseDto {
        val base = forecast()
        val n = base.hourly.time.size
        return base.copy(
            current = base.current.copy(time = time, precipitationMm = blockMm, weatherCode = 3, cloudCoverPct = 100),
            hourly = base.hourly.copy(
                weatherCode = List(n) { 3 },
                cloudCoverPct = List(n) { 100 },
                precipitationMm = List(n) { if (it == 15) 0.1 else 0.0 },
                precipitationProbabilityPct = List(n) { if (it == 15) 47 else null }
            ),
            minutely15 = quarters(*quarterMm, *DoubleArray(12))
        )
    }

    @Test
    fun `where the quarter is the hour cut in four the hero reads the hour, as the strip does`() {
        // Singapore, 17 Nov 2026: an hour of 0.1 mm at 47% served as four quarters of 0.0, the
        // hour spread evenly and rounded. The block is one of them, and the hero said the sky
        // over a cell that said drizzle.
        val report = map(forecast = lightHour(0.0, 0.0, 0.0, 0.0))
        assertNull(report.nowcast)
        assertEquals(51, report.current.condition.wmoCode)
        assertEquals(51, report.hourly.first().condition.wmoCode)
        // The amount the page prints for the quarter stays the block's own.
        assertEquals(0.0, report.current.precipitation.lastQuarterHourMm!!, 0.0)
    }

    @Test
    fun `where a model computes the quarter the hero keeps its own`() {
        // The same 0.1 mm with a shape, 14:30–14:45: the quarter before it is dry, and is now.
        assertEquals(3, map(forecast = lightHour(0.0, 0.0, 0.1, 0.0)).current.condition.wmoCode)
        // ...and the wet one is wet, at the quarter's own rate: 0.4 mm/h of drizzle.
        val wet = lightHour(0.0, 0.0, 0.1, 0.0, blockMm = 0.1, time = "2026-08-13T14:45")
        assertEquals(51, map(forecast = wet).current.condition.wmoCode)
    }

    @Test
    fun `where the app reads the quarters the hero keeps its own, flat hour or not`() {
        // A covered place whose quarters all round to nothing: the section stands on those
        // quarters, and a wet hero over them would be «the rain stops in a moment» all hour.
        val covered = WeatherReportMapper.map(
            city = city.copy(countryCode = "US"),
            forecast = lightHour(0.0, 0.0, 0.0, 0.0),
            airQuality = null,
            fetchedAt = fetchedAt,
            cacheStatus = CacheStatus.MISS
        )
        assertEquals(16, covered.nowcast!!.quarters.size)
        assertEquals(3, covered.current.condition.wmoCode)
    }

    @Test
    fun `without the four quarters of its hour the block keeps its own amount`() {
        // 14:00 is the last quarter of 13–14, whose first three the series does not hold.
        val f = lightHour(0.0, 0.0, 0.0, 0.0, time = "2026-08-13T14:00")
        val n = f.hourly.time.size
        val hour13 = f.copy(hourly = f.hourly.copy(
            precipitationMm = List(n) { if (it == 14) 0.1 else 0.0 },
            precipitationProbabilityPct = List(n) { if (it == 14) 47 else null }
        ))
        assertEquals(3, map(forecast = hour13).current.condition.wmoCode)
    }

    // ---------------------------------------------------------- 24 set 2026: P6

    @Test
    fun `a model with no current uv costs the index, not the report`() {
        val base = forecast()
        val report = map(forecast = base.copy(current = base.current.copy(uvIndex = null)))
        assertNull(report.current.uvIndex)
        assertEquals(33, report.hourly.size)
    }

    // ------------------------- 24 set 2026: a row is the hour it starts (P7b) and all of it

    /** The default fixture with the slots from 14 on overridden by [edit]. */
    private fun rows(edit: (HourlyDto) -> HourlyDto) = forecast().let { f ->
        f.copy(hourly = edit(f.hourly))
    }.let { map(forecast = it, witnesses = agreeing(it)) }.hourly

    @Test
    fun `a row carries the rain, the chance and the gust of the hour it starts`() {
        val n = forecast().hourly.time.size
        val hours = rows { h ->
            h.copy(
                weatherCode = List(n) { if (it == 15) 63 else 0 },
                precipitationMm = List(n) { if (it == 15) 4.0 else 0.0 },
                precipitationProbabilityPct = List(n) { if (it == 15) 70 else 10 },
                windGustsKph = List(n) { if (it == 15) 50.0 else 20.0 },
                cloudCoverPct = List(n) { if (it == 15) 100 else 0 }
            )
        }
        // Row 14:00 is the hour 14-15, which Open-Meteo writes in its 15:00 slot.
        val row = hours.first()
        assertEquals(LocalDateTime.parse("2026-08-13T14:00"), row.time)
        assertEquals(63, row.condition.wmoCode)
        assertEquals(70, row.precipChancePct)
        assertEquals(50.0, row.gustKph!!, 0.0)
        // …while its instants are its own: 14:00's temperature and cloud.
        assertEquals(24.0, row.tempC, 0.0)
        assertEquals(0, row.cloudCoverPct)
        // Row 15:00 is dry, and starts with the overcast sky the rain left.
        assertEquals(3, hours[1].condition.wmoCode)
        assertEquals(10, hours[1].precipChancePct)
    }

    // ------------------------------------------ 25 set 2026: WeatherStateEngine in the mapper

    @Test
    fun `a row's showers and snowfall reach the engine`() {
        val n = forecast().hourly.time.size
        val hours = rows { h ->
            h.copy(
                weatherCode = List(n) { if (it in 15..16) 51 else 0 },
                precipitationMm = List(n) { if (it in 15..16) 0.4 else 0.0 },
                // Slot 15 convective (Reykjavik's «drizzle»), slot 16 snow.
                showersMm = List(n) { if (it == 15) 0.4 else 0.0 },
                snowfallCm = List(n) { if (it == 16) 0.28 else 0.0 },
                precipitationProbabilityPct = List(n) { 50 }
            )
        }
        assertEquals(listOf(80, 73), hours.take(2).map { it.condition.wmoCode })
    }

    /** 15.10: the amounts reach the row, for the rules' sums, from the slot after it. */
    @Test
    fun `a row carries the total and the snowfall of the hour it starts`() {
        val n = forecast().hourly.time.size
        val hours = rows { h ->
            h.copy(
                precipitationMm = List(n) { if (it == 15) 1.4 else 0.0 },
                snowfallCm = List(n) { if (it == 15) 0.7 else 0.0 }
            )
        }
        // Row 14:00 is the hour 14-15, in the 15:00 slot.
        assertEquals(1.4, hours[0].precipMm!!, 0.0)
        assertEquals(0.7, hours[0].snowCm!!, 0.0)
        assertEquals(0.0, hours[1].precipMm!!, 0.0)
        // A cache entry from before the fields were asked for carries none: null, not zero.
        val old = rows { h -> h.copy(precipitationMm = emptyList(), snowfallCm = emptyList()) }
        assertNull(old[0].precipMm)
        assertNull(old[0].snowCm)
    }

    @Test
    fun `the current quarter is scaled to the hour the thresholds speak`() {
        val base = forecast()
        fun now(mm: Double, snow: Double?) = map(
            forecast = base.copy(
                current = base.current.copy(
                    weatherCode = 71, precipitationMm = mm, snowfallCm = snow, showersMm = 0.0,
                    intervalSeconds = 900
                )
            )
        ).current.condition.wmoCode
        // 0.1 mm and 0.07 cm in fifteen minutes is 0.4 mm and 0.28 cm an hour: moderate snow.
        assertEquals(73, now(0.1, 0.07))
        // Nothing in the quarter is the sky, whatever the code says.
        assertEquals(2, now(0.0, 0.0))
    }

    @Test
    fun `the present is never merely likely`() {
        val base = forecast()
        val n = base.hourly.time.size
        val report = map(
            forecast = base.copy(
                current = base.current.copy(precipitationMm = 0.0, cloudCoverPct = 90),
                // The hour under way (the slot ending after 14:23) at 90%.
                hourly = base.hourly.copy(precipitationProbabilityPct = List(n) { if (it == 15) 90 else null })
            )
        )
        assertEquals(3, report.current.condition.wmoCode)
        // …while the row for that same hour, a forecast, says it.
        assertEquals(WmoCode.RAIN_LIKELY.code, report.hourly.first().condition.wmoCode)
    }

    @Test
    fun `a row starts with its own sky, not the one the hour ends with`() {
        val n = forecast().hourly.time.size
        // Fog at 14 and 15, gone by 16: rows 14 and 15 start in it, row 16 does not.
        val hours = rows { h ->
            h.copy(
                weatherCode = List(n) { if (it in 14..15) 45 else 1 },
                visibilityM = List(n) { if (it in 14..15) 200.0 else 20_000.0 },
                cloudCoverPct = List(n) { if (it in 14..15) 100 else 30 }
            )
        }
        assertEquals(listOf(45, 45, 1), hours.take(3).map { it.condition.wmoCode })
    }

    @Test
    fun `an hour after rain starts with the sky its own cloud makes`() {
        val n = forecast().hourly.time.size
        // Slot 14 is rain (13-14), slot 15 dry: row 14 must not show the rain that is
        // over, and it has no sky code of its own — its 90% cloud is overcast.
        val hours = rows { h ->
            h.copy(
                weatherCode = List(n) { if (it == 14) 61 else 0 },
                cloudCoverPct = List(n) { if (it == 14) 90 else 0 }
            )
        }
        assertEquals(3, hours.first().condition.wmoCode)
    }

    @Test
    fun `the rest of an hour is carried, instants from its own slot`() {
        val n = forecast().hourly.time.size
        val row = rows { h ->
            h.copy(
                apparentTemperatureC = List(n) { 30.0 + it },
                humidityPct = List(n) { 40 + it },
                dewPointC = List(n) { 10.0 + it },
                pressureMslHpa = List(n) { 1000.0 + it },
                windSpeedKph = List(n) { 5.0 + it },
                windDirectionDeg = List(n) { it * 10 },
                uvIndex = List(n) { it / 2.0 },
                cloudCoverLowPct = List(n) { 70 },
                cloudCoverMidPct = List(n) { 10 },
                cloudCoverHighPct = List(n) { 5 }
            )
        }.first()
        assertEquals(44.0, row.feelsLikeC!!, 0.0)
        assertEquals(54, row.humidityPct)
        assertEquals(24.0, row.dewPointC!!, 0.0)
        assertEquals(1014.0, row.pressureMb!!, 0.0)
        assertEquals(19.0, row.windKph!!, 0.0)
        assertEquals(140, row.windDirectionDeg)
        assertEquals(7.0, row.uvIndex!!, 0.0)
        assertEquals(20.0, row.visibilityKm!!, 0.0)
        assertEquals(CloudLayers(70, 10, 5), row.cloudLayers)
        assertEquals(CloudLayers.Layer.LOW, row.cloudLayers!!.dominant())
    }

    @Test
    fun `a response from before these fields carries none of them, not zeros`() {
        val row = map().hourly.first()
        assertNull(row.feelsLikeC)
        assertNull(row.gustKph)
        assertNull(row.cloudLayers)
        val day = map().daily.first()
        assertNull(day.precipMm)
        assertNull(day.rainMm)
        assertNull(day.snowCm)
        assertNull(day.gustMaxKph)
    }

    @Test
    fun `the day carries how much falls, the snow and the strongest gust`() {
        val base = forecast()
        val day = map(
            forecast = base.copy(
                daily = base.daily.copy(
                    precipitationSumMm = List(8) { if (it == 0) 12.4 else null },
                    snowfallSumCm = List(8) { if (it == 0) 0.0 else null },
                    windGustsMaxKph = List(8) { if (it == 0) 61.2 else null }
                )
            )
        ).daily
        assertEquals(12.4, day[0].precipMm!!, 0.0)
        assertEquals(0.0, day[0].snowCm!!, 0.0)
        assertEquals(61.2, day[0].gustMaxKph!!, 0.0)
        assertNull(day[1].precipMm)
        // No split asked for and no snow: the total is all rain.
        assertEquals(12.4, day[0].rainMm!!, 0.0)
    }

    @Test
    fun `showers that are snow showers are not rain`() {
        // Longyearbyen, 25 Sep 2026 run, two days of the same response. The 25th: the
        // hours coded 85 carry their water in `showers` AND in `snowfall`, so the split
        // says 0.3 mm where the total leaves 0.6 - 0.35 / 0.7 = 0.1. The 28th: the showers
        // were rain, the identity holds, and the two answers are the same 3.4.
        val base = forecast()
        val days = map(
            forecast = base.copy(
                daily = base.daily.copy(
                    precipitationSumMm = List(8) { if (it == 0) 0.6 else 4.1 },
                    rainSumMm = List(8) { if (it == 0) 0.1 else 2.8 },
                    showersSumMm = List(8) { if (it == 0) 0.2 else 0.6 },
                    snowfallSumCm = List(8) { if (it == 0) 0.35 else 0.49 }
                )
            )
        ).daily
        assertEquals(0.1, days[0].rainMm!!, 1e-9)
        assertEquals(3.4, days[1].rainMm!!, 1e-9)
    }

    @Test
    fun `a day of snow alone has no rain, whatever the subtraction leaves`() {
        // Everest's 19.81 cm against 28.4 mm: the total minus the snow's water is 0.1, a
        // rounding residue. The split says 0, and the smaller answer wins.
        val base = forecast()
        val day = map(
            forecast = base.copy(
                daily = base.daily.copy(
                    precipitationSumMm = List(8) { 28.4 },
                    rainSumMm = List(8) { 0.0 },
                    showersSumMm = List(8) { 0.0 },
                    snowfallSumCm = List(8) { 19.81 }
                )
            )
        ).daily.first()
        assertEquals(0.0, day.rainMm!!, 0.0)
    }

    @Test
    fun `the day's rain is the rain alone, not the snow's water`() {
        // Friday at Longyearbyen: 0.6 mm in all, which was 0.4 cm of snow.
        val base = forecast()
        fun day(rain: Double?, showers: Double?, snow: Double?) = map(
            forecast = base.copy(
                daily = base.daily.copy(
                    precipitationSumMm = List(8) { 0.6 },
                    snowfallSumCm = List(8) { snow },
                    rainSumMm = if (rain == null) emptyList() else List(8) { rain },
                    showersSumMm = if (showers == null) emptyList() else List(8) { showers }
                )
            )
        ).daily.first()
        assertEquals(0.0, day(rain = 0.0, showers = 0.0, snow = 0.4).rainMm!!, 0.0)
        assertEquals(0.5, day(rain = 0.2, showers = 0.3, snow = 0.0).rainMm!!, 1e-9)
        // A model that does not split showers still has its rain.
        assertEquals(0.2, day(rain = 0.2, showers = null, snow = 0.2).rainMm!!, 0.0)
        // No split, and snow in the total: which part is rain cannot be told.
        assertNull(day(rain = null, showers = null, snow = 0.4).rainMm)
        // …unless the snow is a trace no screen names: its water is under what prints.
        assertEquals(0.6, day(rain = null, showers = null, snow = 0.07).rainMm!!, 0.0)
        assertNull(day(rain = null, showers = null, snow = null).rainMm)
    }

    /**
     * Since upstream Fase 16e these come from [AstronomyEngine] and not from the provider's
     * daily block — one engine answers for the JSON tab, the README and
     * `sky.crontab`, so the app cannot show two sunrises for one city. The fixture's
     * coordinates are New York's, and the values are the engine's own.
     */
    @Test
    fun `astronomical is computed rather than read off the provider`() {
        // New York on 13 Aug 2026. The provider's sunrise is not even fetched any
        // more (20 set 2026) — it fed nothing, and its fixed offset made it a trap —
        // so there is no second opinion left to disagree with: 06:04/19:56 is what
        // the engine says the sky does there that day.
        val astro = map().astronomical
        assertEquals(LocalTime.of(6, 4), astro.sunrise)
        assertEquals(LocalTime.of(19, 56), astro.sunset)
        assertEquals(
            Duration.between(astro.sunrise, astro.sunset),
            astro.daylightDuration?.truncatedTo(ChronoUnit.MINUTES)
        )
    }

    /**
     * Truncated to the minute, and not for tidiness: `WeatherSnapshots.flatten`
     * writes `sunrise.toString()` into the history, so a value carrying seconds would
     * put a fresh `astronomical.sunrise` line in `history.diff` on every
     * single fetch. The provider's values were minute-precise and nothing noticed
     * until they stopped being the source.
     */
    @Test
    fun `astronomical times carry no seconds into the history`() {
        val astro = map().astronomical
        assertEquals(0, astro.sunrise!!.second)
        assertEquals(0, astro.sunrise!!.nano)
        assertEquals(0, astro.sunset!!.second)
    }

    /**
     * Above the Arctic circle in June there is no sunrise, and the model can now say
     * so. The old type could only carry some other time and let the reader assume it
     * meant something.
     */
    @Test
    fun `a polar day maps to no sunrise rather than to a fabricated one`() {
        val tromso = City(
            id = 3133880, name = "Tromso", region = "Troms", country = "Norway",
            coordinates = Coordinates(69.6492, 18.9553), timezone = "Europe/Oslo"
        )
        val astro = WeatherReportMapper.map(
            city = tromso,
            forecast = forecast(currentTime = "2026-06-21T12:00"),
            airQuality = null,
            fetchedAt = fetchedAt,
            cacheStatus = CacheStatus.MISS
        ).astronomical
        assertNull(astro.sunrise)
        assertNull(astro.sunset)
        assertNull(astro.daylightDuration)
    }

    @Test
    fun `system info carries source sync time and cache status`() {
        val info = map().systemInfo
        assertEquals("Open-Meteo API", info.source)
        assertEquals(fetchedAt, info.lastSync)
        assertEquals(CacheStatus.MISS, info.cacheStatus)
    }

    @Test
    fun `missing air quality response leaves both sections null`() {
        val report = map(airQuality = null)
        assertNull(report.airQuality)
        assertNull(report.pollen)
    }

    @Test
    fun `air quality without us aqi maps to null`() {
        val report = map(airQuality = AirQualityCurrentDto(time = "2026-08-13T14:00"))
        assertNull(report.airQuality)
    }

    @Test
    fun `the day's rain hours are the hours with an amount, as its millimetres are`() {
        val base = forecast()
        val n = base.hourly.time.size
        fun day(mm: (Int) -> Double, chance: (Int) -> Int?) = map(
            forecast = base.copy(
                hourly = base.hourly.copy(
                    weatherCode = List(n) { 3 },
                    precipitationMm = List(n, mm),
                    precipitationProbabilityPct = List(n, chance)
                )
            )
        ).daily
        // Slots 9–11: amounts the ensemble does not back. 14: rain. 16: an amount with no
        // chance stated, which counts as probable. Two hours are wet for the strip, and the
        // day's millimetres fall over five: «in 5 hours», or the sentence would read as a
        // rate the day does not have (Cuneo, 10 Nov 2026: 10 mm over six hours, two of them
        // wet, was «10 mm in 2 hours» with no hour over 3.1).
        val mixed = day(
            mm = { if (it in 9..11) 0.3 else if (it == 14) 2.0 else if (it == 16) 0.5 else 0.0 },
            chance = { if (it in 9..11) 10 else if (it == 14) 40 else null }
        )
        assertEquals(5, mixed[0].precipHours)
        // Turin, 22 Nov 2026: 2.4 mm over nine hours, every one under 20%. The strip draws
        // no wet hour, and the hours are not said over it: the millimetres stand alone.
        val unbacked = day(mm = { if (it in 6..14) 0.3 else 0.0 }, chance = { 10 })
        assertNull(unbacked[0].precipHours)
        // A dry day has none to say either, nor has a day the hours do not reach.
        assertNull(mixed[1].precipHours)
        assertNull(map(forecast = forecast(hourlyCount = 24)).daily[1].precipHours)
    }

    /** A day of pollen by the hour: each species at one value all day unless a test says. */
    private fun pollenDay(
        grass: Double? = 0.0, birch: Double? = 0.0, alder: Double? = 0.0, olive: Double? = 0.0,
        ragweed: Double? = 0.0, mugwort: Double? = 0.0
    ) = AirQualityHourlyDto(
        time = List(24) { LocalDateTime.parse("2026-08-13T00:00").plusHours(it.toLong()).toString() },
        grassPollen = List(24) { grass }, birchPollen = List(24) { birch }, alderPollen = List(24) { alder },
        olivePollen = List(24) { olive }, ragweedPollen = List(24) { ragweed }, mugwortPollen = List(24) { mugwort }
    )

    @Test
    fun `the pollen's level is the day's mean, not the hour the page is opened in`() {
        // Milan, 2 April 2026, birch by the hour: 8 grains at four in the afternoon, 104 at
        // seven in the evening. Read off the hour, the tile said «low» and «high» in one
        // afternoon; the classes are written for the day's mean, which is 28: moderate.
        val birch = listOf(
            38.8, 23.0, 20.4, 16.8, 16.2, 14.2, 13.3, 11.5, 15.3, 21.1, 15.4, 12.7,
            11.0, 9.6, 9.3, 8.7, 8.4, 20.5, 66.0, 104.4, 93.2, 51.8, 40.7, 36.1
        )
        assertEquals(PollenLevel.MODERATE, map(pollenHours = pollenDay().copy(birchPollen = birch)).pollen!!.tree)
        // Half a day of hours is still a day; fewer are not, and the report says nothing.
        fun hours(n: Int) = List(24) { if (it < n) 30.0 else null }
        fun day(n: Int) = AirQualityHourlyDto(
            grassPollen = hours(n), birchPollen = hours(n), alderPollen = hours(n),
            olivePollen = hours(n), ragweedPollen = hours(n), mugwortPollen = hours(n)
        )
        assertEquals(PollenLevel.MODERATE, map(pollenHours = day(12)).pollen!!.grass)
        assertNull(map(pollenHours = day(11)).pollen)
    }

    @Test
    fun `pollen tree level is the worst of birch alder olive, each on its own scale`() {
        val report = map(
            pollenHours = pollenDay(
                grass = 0.4,      // NONE
                birch = 5.0,      // LOW…
                alder = 120.0,    // …but alder is HIGH (70-249) → tree = HIGH
                ragweed = 35.0,   // HIGH for ragweed (11-39); it was «moderate» on the old
                mugwort = 2.0     // single scale, which is the point of 24 set 2026
            )
        )
        val pollen = report.pollen!!
        assertEquals(PollenLevel.NONE, pollen.grass)
        assertEquals(PollenLevel.HIGH, pollen.tree)
        assertEquals(PollenLevel.HIGH, pollen.weed)
    }

    /** MeteoSwiss' classes, species by species (24 set 2026). */
    @Test
    fun `pollen classes follow the species`() {
        fun grass(g: Double) = map(pollenHours = pollenDay(grass = g)).pollen!!.grass
        assertEquals(PollenLevel.LOW, grass(19.0))
        assertEquals(PollenLevel.MODERATE, grass(20.0))
        assertEquals(PollenLevel.HIGH, grass(149.0))
        assertEquals(PollenLevel.VERY_HIGH, grass(150.0))
        fun olive(o: Double) = map(pollenHours = pollenDay(olive = o)).pollen!!.tree
        // Olive takes the ash's classes (same family): 11 / 100 / 350.
        assertEquals(PollenLevel.MODERATE, olive(99.0))
        assertEquals(PollenLevel.HIGH, olive(100.0))
        assertEquals(PollenLevel.VERY_HIGH, olive(350.0))
    }

    @Test
    fun `a European place reads the European air index, anywhere else the US one`() {
        val air = AirQualityCurrentDto(time = "2026-08-13T14:00", usAqi = 69, europeanAqi = 51)
        fun mapAt(code: String?) = WeatherReportMapper.map(
            city = city.copy(countryCode = code), forecast = forecast(), airQuality = air,
            fetchedAt = fetchedAt, cacheStatus = CacheStatus.MISS
        ).airQuality!!
        assertEquals(AqiScale.EUROPEAN, mapAt("IT").scale)
        assertEquals(51, mapAt("IT").shownIndex)
        assertEquals(69, mapAt("IT").aqiIndex) // what rules and history keep reading
        assertEquals(AqiScale.US, mapAt("US").scale)
        assertEquals(69, mapAt("US").shownIndex)
        // No code, no guess.
        assertEquals(AqiScale.US, mapAt(null).scale)
        // A European place whose European index was not served keeps the US scale.
        val usOnly = WeatherReportMapper.map(
            city = city.copy(countryCode = "IT"), forecast = forecast(),
            airQuality = air.copy(europeanAqi = null),
            fetchedAt = fetchedAt, cacheStatus = CacheStatus.MISS
        ).airQuality!!
        assertEquals(AqiScale.US, usOnly.scale)
    }

    @Test
    fun `pollen report is null outside pollen coverage`() {
        // US location: air quality present, every pollen column null, or not there at all.
        val air = AirQualityCurrentDto(time = "2026-08-13T14:00", usAqi = 42)
        assertNull(map(airQuality = air, pollenHours = pollenDay(null, null, null, null, null, null)).pollen)
        assertNull(map(airQuality = air, pollenHours = AirQualityHourlyDto()).pollen)
        assertNull(map(airQuality = air).pollen)
    }

    // --- the provider's fixed offset (20 set 2026) -------------------------------

    /**
     * A response shaped like the real one Open-Meteo serves across a DST change:
     * ONE `utc_offset_seconds` for the whole week, and a local-time series that is
     * simply `UTC + that offset`. See `ForecastResponseDto.utcOffsetSeconds` for the
     * measurement these fixtures reproduce.
     */
    private fun shifted(
        timezone: String,
        offsetSeconds: Int,
        firstHour: String,
        currentTime: String,
        hourlyCount: Int = 48
    ): ForecastResponseDto {
        val base = forecast()
        val first = LocalDateTime.parse(firstHour)
        return base.copy(
            timezone = timezone,
            utcOffsetSeconds = offsetSeconds,
            current = base.current.copy(time = currentTime),
            hourly = base.hourly.copy(
                time = List(hourlyCount) { first.plusHours(it.toLong()).toString() },
                temperatureC = List(hourlyCount) { 10.0 + it },
                weatherCode = List(hourlyCount) { 0 },
                precipitationMm = List(hourlyCount) { 0.0 },
                precipitationProbabilityPct = List(hourlyCount) { 10 },
                isDay = List(hourlyCount) { 1 },
                visibilityM = List(hourlyCount) { 20_000.0 },
                cloudCoverPct = List(hourlyCount) { 0 }
            ),
            daily = base.daily.copy(
                time = List(3) { first.toLocalDate().plusDays(it.toLong()).toString() },
                weatherCode = List(3) { 3 },
                temperatureMaxC = List(3) { 28.0 },
                temperatureMinC = List(3) { 18.0 },
                precipitationProbabilityMaxPct = List(3) { 10 },
                uvIndexMax = List(3) { 6.0 }
            )
        )
    }

    /**
     * Sydney, 4 Oct 2026: the clocks go forward at 02:00 AEST, and the provider keeps
     * writing the whole week on `+10`. The label `08:00` therefore names 22:00 UTC of
     * the 3rd, which in `Australia/Sydney` is **09:00**. Reading that label as a local
     * time — which the app did until this field was read — put the row an hour early.
     */
    @Test
    fun `hours after a spring forward land on the city's clock, not on the label`() {
        val report = WeatherReportMapper.map(
            city = city.copy(timezone = "Australia/Sydney"),
            forecast = shifted(
                timezone = "Australia/Sydney",
                offsetSeconds = 36_000,
                firstHour = "2026-10-03T00:00",
                currentTime = "2026-10-03T00:00",
                hourlyCount = 72
            ),
            airQuality = null,
            fetchedAt = fetchedAt,
            cacheStatus = CacheStatus.MISS
        )
        // Before the change the two frames agree, and nothing moves.
        val before = report.hourly.first { it.at == Instant.parse("2026-10-03T14:00:00Z") }
        assertEquals(LocalDateTime.parse("2026-10-04T00:00"), before.time)
        // After it they do not.
        val after = report.hourly.first { it.at == Instant.parse("2026-10-03T22:00:00Z") }
        assertEquals(LocalDateTime.parse("2026-10-04T09:00"), after.time)
        // And the hour that does not exist in Sydney is not drawn as though it did:
        // the provider sent a `2026-10-04T02:00` label, the city's clock never says it.
        assertNull(report.hourly.firstOrNull { it.time == LocalDateTime.parse("2026-10-04T02:00") })
    }

    /** The label names a moment, and [HourlyForecast.at] is that moment exactly: the
     * label plus the offset it was written on, with no zone rule in the way. */
    @Test
    fun `an hour carries the instant its label names`() {
        val report = WeatherReportMapper.map(
            city = city.copy(timezone = "Australia/Sydney"),
            forecast = shifted(
                timezone = "Australia/Sydney",
                offsetSeconds = 36_000,
                firstHour = "2026-10-03T00:00",
                currentTime = "2026-10-03T00:00"
            ),
            airQuality = null,
            fetchedAt = fetchedAt,
            cacheStatus = CacheStatus.MISS
        )
        // First label is 2026-10-03T00:00 on +10 → 2026-10-02T14:00Z.
        assertEquals(Instant.parse("2026-10-02T14:00:00Z"), report.hourly.first().at)
        // The series is strictly increasing by an hour, whatever the labels do.
        report.hourly.zipWithNext().forEach { (a, b) ->
            assertEquals(Duration.ofHours(1), Duration.between(a.at, b.at))
        }
    }

    /**
     * Rome, 25 Oct 2026: the clocks go back at 03:00 CEST, so the city really does
     * live 02:00 twice — and the two rows are two different hours of weather.
     *
     * This is the case that makes [HourlyForecast.at] load-bearing rather than tidy:
     * `time` is no longer unique, and the hour strip keys its cells on identity.
     */
    @Test
    fun `the day a zone falls back holds one label twice and two moments`() {
        val report = WeatherReportMapper.map(
            city = city.copy(timezone = "Europe/Rome"),
            forecast = shifted(
                timezone = "Europe/Rome",
                offsetSeconds = 7_200,
                firstHour = "2026-10-25T00:00",
                currentTime = "2026-10-25T00:00"
            ),
            airQuality = null,
            fetchedAt = fetchedAt,
            cacheStatus = CacheStatus.MISS
        )
        val twice = report.hourly.filter { it.time == LocalDateTime.parse("2026-10-25T02:00") }
        assertEquals(2, twice.size)
        assertEquals(Instant.parse("2026-10-25T00:00:00Z"), twice[0].at)
        assertEquals(Instant.parse("2026-10-25T01:00:00Z"), twice[1].at)
        // Two rows, two temperatures: dropping one would lose an hour of forecast.
        assertEquals(2, twice.map { it.tempC }.distinct().size)
        // Whatever the labels repeat, the identities do not.
        assertEquals(report.hourly.size, report.hourly.map { it.at }.distinct().size)
    }

    /**
     * A `ReportDiskCache` entry written before the field existed carries no offset,
     * and gets the behaviour it was written under — not a guess at what the offset
     * might have been. An offline phone keeps the week it has.
     */
    @Test
    fun `a response with no offset is read exactly as it used to be`() {
        val report = map(forecast().copy(utcOffsetSeconds = null))
        assertEquals(LocalDateTime.parse("2026-08-13T14:00"), report.hourly.first().time)
        assertEquals(
            report.hourly.first().time.atZone(java.time.ZoneId.of("America/New_York")).toInstant(),
            report.hourly.first().at
        )
    }

    /**
     * A model that carries no UV must cost the reader a tile, not the whole week.
     *
     * Reproduces what the live endpoint serves under `models=icon_seamless` and
     * `models=jma_seamless`: `uv_index_max` all null. Before 20 set 2026 the DTO
     * declared `List<Double>` and this response did not fail to map — it failed to
     * **deserialize**, which takes the current block, the hours and the seven days
     * with it. Now the day maps, and says it does not know.
     */
    @Test
    fun `a model with no uv index costs a tile, not the report`() {
        val base = forecast()
        val report = map(
            base.copy(daily = base.daily.copy(uvIndexMax = base.daily.time.map { null }))
        )
        assertEquals(7, report.daily.size)
        assertNull(report.daily.first().uvIndexMax)
        // Everything else in the response is untouched: the failure mode this replaces
        // was total, so the test says so.
        assertEquals(22.4, report.current.tempC, 0.001)
        assertEquals(48 - 14 - 1, report.hourly.size) // the last slot is no row (P7b)
    }

    /** A day past the end of a short `uv_index_max` is the same answer as a null in
     * it: out of bounds and "the model did not say" are both absence, never a zero. */
    @Test
    fun `a day the uv list does not reach has no index either`() {
        val base = forecast()
        val report = map(base.copy(daily = base.daily.copy(uvIndexMax = listOf(6.0))))
        assertEquals(6, report.daily.first().uvIndexMax)
        assertNull(report.daily[1].uvIndexMax)
    }

    // --- The quarters (17 ott 2026) ---------------------------------------------------

    /** Sixteen slots from 14:15, local, the amounts given; the quarter ENDS at its label. */
    private fun quarters(vararg mm: Double) = Minutely15Dto(
        time = mm.indices.map { LocalDateTime.parse("2026-08-13T14:15").plusMinutes(15L * it).toString() },
        precipitationMm = mm.toList(),
        snowfallCm = mm.map { 0.0 }
    )

    private fun mapAt(place: City, minutely: Minutely15Dto?) = WeatherReportMapper.map(
        city = place,
        forecast = forecast().copy(minutely15 = minutely),
        airQuality = null,
        fetchedAt = fetchedAt,
        cacheStatus = CacheStatus.MISS
    )

    private val shaped = doubleArrayOf(0.0, 0.1, 0.3, 0.2, 0.0, 0.0, 0.0, 0.0, 0.1, 0.4, 0.1, 0.0, 0.0, 0.0, 0.0, 0.0)

    @Test
    fun `a covered place keeps its quarters, each starting fifteen minutes before its label`() {
        val nowcast = mapAt(city.copy(countryCode = "US"), quarters(*shaped)).nowcast!!
        assertEquals(16, nowcast.quarters.size)
        // The 14:15 slot is 14:00–14:15, on New York's clock (UTC-4 in August).
        assertEquals(Instant.parse("2026-08-13T18:00:00Z"), nowcast.quarters.first().start)
        assertEquals(0.3, nowcast.quarters[2].precipMm, 0.0)
        assertEquals(0.0, nowcast.quarters[2].snowCm!!, 0.0)
    }

    @Test
    fun `outside the coverage the series is not read`() {
        // No country code: never guessed into the table.
        assertNull(mapAt(city, quarters(*shaped)).nowcast)
        assertNull(mapAt(city.copy(countryCode = "CA"), quarters(*shaped)).nowcast)
    }

    @Test
    fun `a series that reads as the hourly one cut in four is dropped`() {
        // 14:00–15:00 in four equal parts, the hour's own 0.8 mm spread evenly.
        val flat = doubleArrayOf(0.2, 0.2, 0.2, 0.2, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        assertNull(mapAt(city.copy(countryCode = "US"), quarters(*flat)).nowcast)
    }

    @Test
    fun `a cache entry from before the quarters has no nowcast, and still maps`() {
        val report = mapAt(city.copy(countryCode = "US"), null)
        assertNull(report.nowcast)
        assertEquals(33, report.hourly.size)
    }

    @Test
    fun `a slot with no amount leaves a gap rather than a zero`() {
        val series = quarters(*shaped).let { it.copy(precipitationMm = it.precipitationMm.mapIndexed { i, v -> if (i == 4) null else v }) }
        val nowcast = mapAt(city.copy(countryCode = "US"), series).nowcast!!
        assertEquals(15, nowcast.quarters.size)
        assertEquals(Duration.ofMinutes(30), Duration.between(nowcast.quarters[3].start, nowcast.quarters[4].start))
    }

    // ---- 19 ott 2026: a null from the model costs the hour, the day's aggregate or the
    // block it sits in — never the report.

    private fun <T> List<T>.withNullAt(vararg indices: Int): List<T?> =
        mapIndexed { i, v -> if (i in indices) null else v }

    @Test
    fun `an hour without a temperature is not a row, and its neighbours keep their places`() {
        val base = forecast()
        val holed = base.copy(hourly = base.hourly.copy(temperatureC = base.hourly.temperatureC.withNullAt(16)))
        val hourly = map(holed).hourly
        assertEquals(32, hourly.size)
        assertEquals(listOf(14, 15, 17), hourly.take(3).map { it.time.hour })
        assertEquals(27.0, hourly[2].tempC, 0.0) // 10 + 17: the row after the gap is its own
        // The hour that closed without a temperature is not a closed hour either.
        val past = map(base.copy(hourly = base.hourly.copy(temperatureC = base.hourly.temperatureC.withNullAt(13))))
            .current.precipitation.pastHours
        assertEquals(
            listOf("2026-08-13T18:00:00Z", "2026-08-13T16:00:00Z"),
            past.map { it.endedAt.toString() }
        )
    }

    @Test
    fun `an hour without an amount costs its amount, not the report`() {
        val base = forecast()
        // Slot 14 is the wet one: 2 mm under code 61, the hour 13–14.
        val holed = base.copy(hourly = base.hourly.copy(precipitationMm = base.hourly.precipitationMm.withNullAt(14, 15)))
        // The failure this replaces was in the decoder, and total: through it and back.
        val served = Json.decodeFromString(
            ForecastResponseDto.serializer(), Json.encodeToString(ForecastResponseDto.serializer(), holed)
        )
        val report = map(served)
        assertEquals(33, report.hourly.size)
        assertNull(report.hourly.first().precipMm)
        // Not a closed hour either, and never a dry one: the two before it remain.
        assertEquals(2, report.current.precipitation.pastHours.size)
        // The hour keeps the provider's word for what falls, and adds no millimetres to its
        // day: one wet hour with no amount does not label it.
        assertFalse(WmoCode.isPrecipitation(report.daily.first().condition.wmoCode))
    }

    @Test
    fun `an hour without a code reads its sky from its cloud, and without either is not a row`() {
        val base = forecast()
        val h = base.hourly
        // Slot 16 loses its code and is overcast by its cloud; slot 18 loses both and is gone.
        val holed = base.copy(
            hourly = h.copy(
                weatherCode = h.weatherCode.withNullAt(16, 18),
                cloudCoverPct = h.cloudCoverPct.mapIndexed { i, c -> if (i == 16) 95 else c }.withNullAt(18)
            )
        )
        val hourly = map(holed).hourly
        val at16 = hourly.first { it.time.hour == 16 }
        assertEquals(3, at16.condition.wmoCode)
        assertEquals(95, at16.cloudCoverPct)
        assertEquals(listOf(15, 16, 17, 19), hourly.drop(1).take(4).map { it.time.hour })
        // Row 17 takes its interval from slot 18, which says nothing: nothing falls, and
        // the row keeps the sky it starts with.
        assertEquals(0, hourly.first { it.time.hour == 17 }.condition.wmoCode)
        // A slot with a cloud but no code is a row; one with a code but no cloud is not.
        val cloudless = base.copy(hourly = h.copy(cloudCoverPct = h.cloudCoverPct.withNullAt(20)))
        assertTrue(map(cloudless).hourly.none { it.time.hour == 20 && it.time.dayOfMonth == 13 })
    }

    @Test
    fun `a day whose extremes the provider left null takes them from its own hours`() {
        val base = forecast()
        val holed = base.copy(
            daily = base.daily.copy(
                temperatureMaxC = base.daily.temperatureMaxC.withNullAt(1),
                temperatureMinC = base.daily.temperatureMinC.withNullAt(1)
            )
        )
        val daily = map(holed).daily
        assertEquals(7, daily.size) // the week the mapper keeps of the fixture's eight days
        // 14 Aug is the fixture's hours 24..47 at 10 + index: its own 34 and 57.
        assertEquals(57.0, daily[1].highC, 0.0)
        assertEquals(34.0, daily[1].lowC, 0.0)
        // The provider's aggregates where it gave them.
        assertEquals(28.0, daily[0].highC, 0.0)
        assertEquals(18.0, daily[0].lowC, 0.0)
    }

    @Test
    fun `a day with no hours to read from either is not a row`() {
        val base = forecast()
        // 18 Aug lies past the fixture's 48 hours: without its aggregate it has nothing.
        val noHigh = base.copy(daily = base.daily.copy(temperatureMaxC = base.daily.temperatureMaxC.withNullAt(5)))
        val days = map(noHigh).daily
        assertEquals(6, days.size)
        assertTrue(days.none { it.date == LocalDate.parse("2026-08-18") })
        // The same for a label: past the hours the provider's code is all there is.
        val noCode = base.copy(daily = base.daily.copy(weatherCode = base.daily.weatherCode.withNullAt(6)))
        assertTrue(map(noCode).daily.none { it.date == LocalDate.parse("2026-08-19") })
        // Inside the hours a null code changes nothing: the day's label was theirs already.
        val noCodeToday = base.copy(daily = base.daily.copy(weatherCode = base.daily.weatherCode.withNullAt(0)))
        assertEquals(61, map(noCodeToday).daily.first().condition.wmoCode)
    }

    @Test
    fun `a current block with a hole in it is the forecast for its own moment, and says so`() {
        val base = forecast()
        // Rows that carry the rest of an hour, as every fetch since 24 set 2026 does.
        val whole = base.hourly.copy(
            apparentTemperatureC = base.hourly.temperatureC.map { it?.plus(1.0) },
            humidityPct = List(48) { 60 },
            dewPointC = List(48) { 12.0 },
            pressureMslHpa = List(48) { 1010.0 },
            windSpeedKph = List(48) { 8.0 },
            windDirectionDeg = List(48) { 270 }
        )
        val holed = base.copy(hourly = whole, current = base.current.copy(temperatureC = null))
        val current = map(holed).current
        assertTrue(current.estimated)
        // 14:23 between the 14:00 row (24.0) and the 15:00 row (25.0).
        assertEquals(24.0 + 23.0 / 60.0, current.tempC, 1e-9)
        assertEquals(60, current.humidityPct)
        // What the rows cannot replace stays the block's: the quarter's own amount.
        assertEquals(0.5, current.precipitation.lastQuarterHourMm!!, 0.0)
        // A whole block is never an estimate, and a block without a code reads its sky
        // from its cloud like an hour does.
        assertFalse(map(base.copy(hourly = whole)).current.estimated)
        val noCode = map(base.copy(hourly = whole, current = base.current.copy(weatherCode = null, precipitationMm = 0.0)))
        assertFalse(noCode.current.estimated)
        assertEquals(2, noCode.current.condition.wmoCode) // 60% cloud
        // Rows from before they carried the rest of an hour cannot stand in: the report
        // is not built, which is the old failure and the only one left.
        assertThrows(IllegalStateException::class.java) { map(base.copy(current = base.current.copy(temperatureC = null))) }
    }

    // ------------------------------------------------------- 28 ott 2026: the snow line (14.8)

    @Test
    fun `a row carries the freezing level and the snow line of its own slot`() {
        val n = forecast().hourly.time.size
        val hours = rows { h ->
            h.copy(
                freezingLevelM = List(n) { 2000.0 + it },
                snowfallHeightM = List(n) { if (it == 15) 1650.0 else null }
            )
        }
        // Row 14:00: the isotherm at 14:00, no line of the model's → the estimate.
        assertEquals(2014.0, hours[0].freezingLevelM!!, 0.0)
        assertNull(hours[0].snowfallHeightM)
        assertEquals(SnowLine(1714, estimated = true), hours[0].snowLine)
        // Row 15:00: the model's own line wins over the isotherm beside it.
        assertEquals(1650.0, hours[1].snowfallHeightM!!, 0.0)
        assertEquals(SnowLine(1650, estimated = false), hours[1].snowLine)
    }

    @Test
    fun `a wet day's snow line is the lowest of its wet hours, with its provenance`() {
        val n = forecast().hourly.time.size
        val day = map(forecast = forecast().let { f ->
            f.copy(hourly = f.hourly.copy(
                weatherCode = List(n) { if (it in 14..16) 61 else 0 },
                precipitationMm = List(n) { if (it in 14..16) 2.0 else 0.0 },
                precipitationProbabilityPct = List(n) { if (it in 14..16) 60 else 5 },
                cloudCoverPct = List(n) { if (it in 14..16) 100 else 0 },
                // The lowest isotherm of the day is a DRY hour's: it does not count.
                freezingLevelM = List(n) { when (it) { 3 -> 500.0; 14 -> 2000.0; 15 -> 1800.0; 16 -> 2100.0; else -> 2500.0 } },
                snowfallHeightM = List(n) { if (it == 15) 1650.0 else null }
            ))
        }).daily.first()
        // 14: 2 000 − 300 = 1 700 circa; 15: the model's 1 650; 16: 1 800 circa.
        assertEquals(SnowLine(1650, estimated = false), day.snowLine)
    }

    @Test
    fun `a day the rain does not label carries no snow line, whatever its hours say`() {
        val n = forecast().hourly.time.size
        val days = map(forecast = forecast().let { f ->
            f.copy(hourly = f.hourly.copy(
                // Day 0: one hour of 0.3 mm, under the day's bar; day 1: dry.
                precipitationMm = List(n) { if (it == 14) 0.3 else 0.0 },
                freezingLevelM = List(n) { 1000.0 }
            ))
        }).daily
        assertFalse(WmoCode.of(days[0].condition.wmoCode)!!.isPrecipitation)
        assertNull(days[0].snowLine)
        assertNull(days[1].snowLine)
    }

    @Test
    fun `a response without the two columns leaves the line null`() {
        val report = map()
        assertNull(report.hourly.first().snowLine)
        assertNull(report.daily.first().snowLine)
    }

    // ------------------------------- 19 nov 2026: a shower from the air and the shape (17.6)

    /** Rain at the slots of [wet], 1 mm at 60% (5% elsewhere), the model's [showers] all of it,
     * in air of [cape] everywhere; an empty [cape] is a cache entry from before it. */
    private fun rainy(wet: IntRange, cape: List<Double?>, showers: Double = 1.0) = rows { h ->
        val n = h.time.size
        h.copy(
            weatherCode = List(n) { if (it in wet) 61 else 0 },
            precipitationMm = List(n) { if (it in wet) 1.0 else 0.0 },
            precipitationProbabilityPct = List(n) { if (it in wet) 60 else 5 },
            showersMm = List(n) { if (it in wet) showers else 0.0 },
            cloudCoverPct = List(n) { if (it in wet) 100 else 0 },
            capeJkg = cape
        )
    }

    @Test
    fun `a row's rain is a shower from its air, not from the model's showers`() {
        val n = forecast().hourly.time.size
        // Rows 14-16 rain (slots 15-17): three hours in stable air are rain, though the model
        // calls every drop of them convective; in unstable air they are showers.
        assertEquals(listOf(61, 61, 61), rainy(15..17, List(n) { 50.0 }).take(3).map { it.condition.wmoCode })
        assertEquals(listOf(80, 80, 80), rainy(15..17, List(n) { 300.0 }).take(3).map { it.condition.wmoCode })
    }

    @Test
    fun `a short episode in some energy is a shower, and with no CAPE the model's showers decide`() {
        val n = forecast().hourly.time.size
        // One hour of rain at 20 J/kg, with no convective part in the model: a shower by its shape.
        assertEquals(80, rainy(15..15, List(n) { 20.0 }, showers = 0.0).first().condition.wmoCode)
        // The same in stable air: rain.
        assertEquals(61, rainy(15..15, List(n) { 5.0 }, showers = 0.0).first().condition.wmoCode)
        // A cache entry from before CAPE: the model's `showers` decides, as it did.
        assertEquals(80, rainy(15..15, emptyList(), showers = 1.0).first().condition.wmoCode)
        assertEquals(61, rainy(15..15, emptyList(), showers = 0.0).first().condition.wmoCode)
    }

    @Test
    fun `the present says the shower its row says`() {
        val n = forecast().hourly.time.size
        // 14:23: the block falls in row 14, the hour 14-15, raining 2 mm in air of 400 J/kg.
        val f = forecast().let {
            it.copy(
                current = it.current.copy(weatherCode = 61, precipitationMm = 0.5),
                hourly = it.hourly.copy(
                    weatherCode = List(n) { i -> if (i == 15) 61 else 0 },
                    precipitationMm = List(n) { i -> if (i == 15) 2.0 else 0.0 },
                    precipitationProbabilityPct = List(n) { i -> if (i == 15) 60 else 5 },
                    cloudCoverPct = List(n) { i -> if (i == 15) 100 else 0 },
                    capeJkg = List(n) { 400.0 }
                )
            )
        }
        val report = map(forecast = f, witnesses = agreeing(f))
        assertEquals(80, report.hourly.first().condition.wmoCode)
        assertEquals(80, report.current.condition.wmoCode)
    }

    @Test
    fun `in the first quarter of an hour the present's shower is the hour that ends`() {
        // 20 nov 2026, the review of PR #185. One hour of rain, 14-15 (slot 15), in air of
        // 50 J/kg: an episode of one hour with some energy, a shower; 15-16 is dry. At 15:00 the
        // block is 14:45-15:00, its rain and chance the hour 14-15's; read with row 15's air and
        // episode (none) it was «Pioggia» for the quarter of an hour after «Rovesci».
        val n = forecast().hourly.time.size
        for (time in listOf("2026-08-13T14:45", "2026-08-13T15:00")) {
            val f = forecast().let {
                it.copy(
                    current = it.current.copy(time = time, weatherCode = 61, precipitationMm = 0.5),
                    hourly = it.hourly.copy(
                        weatherCode = List(n) { i -> if (i == 15) 61 else 0 },
                        precipitationMm = List(n) { i -> if (i == 15) 1.0 else 0.0 },
                        precipitationProbabilityPct = List(n) { i -> if (i == 15) 60 else 5 },
                        cloudCoverPct = List(n) { i -> if (i == 15) 100 else 0 },
                        capeJkg = List(n) { 50.0 }
                    )
                )
            }
            assertEquals(time, 80, map(forecast = f, witnesses = agreeing(f)).current.condition.wmoCode)
        }
    }

    @Test
    fun `the present counts its row's episode as the strip does, across a missing hour`() {
        val n = forecast().hourly.time.size
        // Row 14 rains (slot 15), row 15 is missing (slot 15 has no code and no cloud), rows 16
        // and 17 rain (slots 17-18), all in air of 20 J/kg. The series is the hours that exist:
        // rows 14, 16 and 17 are one episode three hours long, rain and not a shower. Until
        // 19 nov 2026 the present counted it on the rows with the hole, one hour long, and said
        // «Rovesci» over a cell saying «Pioggia».
        val wet = setOf(15, 17, 18)
        val f = forecast().let {
            it.copy(
                current = it.current.copy(weatherCode = 61, precipitationMm = 0.5),
                hourly = it.hourly.copy(
                    weatherCode = List(n) { i -> if (i == 15) null else if (i in wet) 61 else 0 },
                    precipitationMm = List(n) { i -> if (i in wet) 1.0 else 0.0 },
                    precipitationProbabilityPct = List(n) { i -> if (i in wet) 60 else 5 },
                    cloudCoverPct = List(n) { i -> if (i == 15) null else if (i in wet) 100 else 0 },
                    capeJkg = List(n) { 20.0 }
                )
            )
        }
        val report = map(forecast = f, witnesses = agreeing(f))
        assertEquals(61, report.hourly.first().condition.wmoCode)
        assertEquals(61, report.current.condition.wmoCode)
    }

    // ------------------------------------------- 19 nov 2026: the day's possible storm (17.4)

    /**
     * The default fixture in calm air (CAPE 50 everywhere), with the slots of [wet] raining
     * 1 mm at 40% under code 61 and the CAPE of [cape]'s slots set. Slot k is the hour that
     * ENDS at k; row k − 1 is the hour that starts there.
     */
    private fun unstable(
        wet: Set<Int>,
        cape: Map<Int, Double>,
        codes: Map<Int, Int> = emptyMap()
    ): ForecastResponseDto = forecast().let { f ->
        val n = f.hourly.time.size
        f.copy(hourly = f.hourly.copy(
            weatherCode = List(n) { codes[it] ?: if (it in wet) 61 else 0 },
            precipitationMm = List(n) { if (it in wet || it in codes) 1.0 else 0.0 },
            precipitationProbabilityPct = List(n) { if (it in wet || it in codes) 40 else 5 },
            cloudCoverPct = List(n) { if (it in wet || it in codes) 100 else 20 },
            capeJkg = List(n) { cape[it] ?: 50.0 }
        ))
    }

    @Test
    fun `a row carries the CAPE of its own slot, an instant`() {
        val n = forecast().hourly.time.size
        val hours = rows { h -> h.copy(capeJkg = List(n) { 100.0 * it }) }
        // Row 14:00 is the instant at 14:00, not the 15:00 slot its rain comes from.
        assertEquals(1400.0, hours[0].capeJkg!!, 0.0)
        assertEquals(1500.0, hours[1].capeJkg!!, 0.0)
    }

    @Test
    fun `an hour of rain in unstable air makes its part of the day a possible storm`() {
        // Row 15:00 rains in the 16:00 slot; the air is unstable at its end, not at its start:
        // the higher of the two instants is the hour's. Row 19:00 the other way round.
        val days = map(unstable(wet = setOf(16, 20), cape = mapOf(15 to 100.0, 16 to 800.0, 19 to 700.0, 20 to 80.0))).daily
        assertEquals(listOf(DayPart.AFTERNOON, DayPart.EVENING), days[0].possibleStorm)
        // The label is what falls — in that air a shower (17.6) — never a storm: the possibility
        // is the day's, never its icon.
        assertEquals(WmoCode.SHOWERS_SLIGHT.code, days[0].condition.wmoCode)
        // The next day has CAPE and no hour of rain in it: nothing is possible, and it says so.
        assertEquals(emptyList<DayPart>(), days[1].possibleStorm)
    }

    @Test
    fun `wet air without the energy, or energy without the rain, is no storm`() {
        val days = map(unstable(wet = setOf(16), cape = mapOf(10 to 2000.0, 11 to 2000.0))).daily
        assertEquals(emptyList<DayPart>(), days[0].possibleStorm)
    }

    @Test
    fun `where an hour of the day has the storm code, the code speaks`() {
        // The 15:00 row would be possible; the 20:00 row is a storm the provider names.
        val report = map(unstable(wet = setOf(16), cape = mapOf(16 to 900.0), codes = mapOf(21 to 95)))
        assertEquals(WmoCode.THUNDERSTORM.code, report.daily[0].condition.wmoCode)
        assertEquals(emptyList<DayPart>(), report.daily[0].possibleStorm)
    }

    @Test
    fun `a storm in the last hour of a day silences it, and the next day its label claims`() {
        // Slot 24 (14 Aug, 00:00) is the hour 23-24 of the 13th: the 13th's last row, and in the
        // 14th's label, which the provider's slots make. Each day's possible storm stays silent.
        val days = map(
            unstable(wet = setOf(16, 40), cape = mapOf(16 to 900.0, 40 to 900.0), codes = mapOf(24 to 95))
        ).daily
        assertEquals(emptyList<DayPart>(), days[0].possibleStorm)
        assertEquals(WmoCode.THUNDERSTORM.code, days[1].condition.wmoCode)
        assertEquals(emptyList<DayPart>(), days[1].possibleStorm)
    }

    @Test
    fun `a response without CAPE leaves the day's possible storm unsaid, not no`() {
        val report = map()
        assertNull(report.hourly.first().capeJkg)
        // A cache entry from before the variable: every day null, never an empty «no storm».
        assertTrue(report.daily.all { it.possibleStorm == null })
        // Days the hourly run does not reach have no CAPE to read either.
        val days = map(unstable(wet = setOf(16), cape = mapOf(16 to 900.0))).daily
        assertNull(days[2].possibleStorm)
    }

    // ------------------------------------------------------------- the sky of three models

    /** [forecast] under a low overcast deck, every hour and in the current block. */
    private fun overcast(): ForecastResponseDto {
        val f = forecast()
        val n = f.hourly.time.size
        return f.copy(
            current = f.current.copy(cloudCoverPct = 100, cloudCoverLowPct = 100, cloudCoverMidPct = 0, cloudCoverHighPct = 0),
            hourly = f.hourly.copy(
                cloudCoverPct = List(n) { 100 },
                cloudCoverLowPct = List(n) { 100 },
                cloudCoverMidPct = List(n) { 0 },
                cloudCoverHighPct = List(n) { 0 }
            )
        )
    }

    /**
     * Witnesses with a cloud of their own, the same every hour: (model, temperature offset, the
     * four numbers). A temperature offset of 0 and best_match's own cloud make a copy.
     */
    private fun skyWitnesses(
        forecast: ForecastResponseDto,
        vararg models: Triple<String, Double, List<Int?>>
    ): WitnessResponseDto {
        val n = forecast.hourly.time.size
        fun col(name: String, v: (Int) -> Any?) = "\"$name\":[${List(n, v).joinToString(",") { it?.toString() ?: "null" }}]"
        val columns = models.flatMap { (model, offset, sky) ->
            listOf(
                col("temperature_2m_$model") { forecast.hourly.temperatureC[it]!! + offset },
                col("cloud_cover_$model") { sky[0] },
                col("cloud_cover_low_$model") { sky[1] },
                col("cloud_cover_mid_$model") { sky[2] },
                col("cloud_cover_high_$model") { sky[3] },
                col("visibility_$model") { 20_000.0 },
                col("relative_humidity_2m_$model") { 70 }
            )
        }
        return Json.decodeFromString(
            WitnessResponseDto.serializer(),
            """{"hourly":{"time":[${forecast.hourly.time.joinToString(",") { "\"$it\"" }}],${columns.joinToString(",")}}}"""
        )
    }

    @Test
    fun `the hour's cloud is the mean of best_match and the two global witnesses`() {
        // Fase 18.3: ICON's low overcast against two globals' broken sky. KNMI's cloud is fog's
        // witness only, and stays out of the mean.
        val f = overcast()
        val w = skyWitnesses(
            f,
            Triple("ecmwf_ifs", -3.0, listOf(20, 10, 5, 0)),
            Triple("ukmo_global_deterministic_10km", 2.0, listOf(30, 20, 0, 10)),
            Triple("knmi_harmonie_arome_europe", 1.0, listOf(100, 100, 100, 100))
        )
        val report = map(forecast = f, witnesses = w)
        val hour = report.hourly[1]
        assertEquals(50, hour.cloudCoverPct)
        assertEquals(CloudLayers(43, 2, 3), hour.cloudLayers)
        // The state reads the same cloud: no longer the overcast the hour's own model says.
        assertFalse(hour.condition.wmoCode == 3)
        assertEquals(3, map(forecast = f).hourly[1].condition.wmoCode)
        // The block takes the witnesses of the row it falls in.
        assertEquals(50, report.current.cloudCoverPct)
        assertEquals(CloudLayers(43, 2, 3), report.current.cloudLayers)
        // ...and the report says so, for the page's footer.
        assertTrue(report.systemInfo.cloudsFromModels)
    }

    @Test
    fun `without witnesses, or with witnesses lacking a layer, the sky is best_match's`() {
        val f = overcast()
        assertEquals(100, map(forecast = f).hourly[1].cloudCoverPct)
        val lacking = skyWitnesses(f, Triple("ecmwf_ifs", -3.0, listOf(20, null, 5, 0)))
        val report = map(forecast = f, witnesses = lacking)
        assertEquals(100, report.hourly[1].cloudCoverPct)
        assertEquals(CloudLayers(100, 0, 0), report.hourly[1].cloudLayers)
        assertEquals(100, report.current.cloudCoverPct)
        assertFalse(report.systemInfo.cloudsFromModels)
        assertFalse(map(forecast = f).systemInfo.cloudsFromModels)
    }

    @Test
    fun `a witness that is best_match itself does not count twice`() {
        // ECMWF over Sydney: the same temperature and the same cloud as best_match.
        val f = overcast()
        val w = skyWitnesses(
            f,
            Triple("ecmwf_ifs", 0.0, listOf(100, 100, 0, 0)),
            Triple("ukmo_global_deterministic_10km", 2.0, listOf(0, 0, 0, 0))
        )
        assertEquals(50, map(forecast = f, witnesses = w).hourly[1].cloudCoverPct)
    }
}
