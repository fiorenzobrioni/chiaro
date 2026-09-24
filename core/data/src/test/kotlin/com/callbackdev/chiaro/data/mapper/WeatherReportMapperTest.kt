package com.callbackdev.chiaro.data.mapper

import com.callbackdev.chiaro.data.remote.dto.AirQualityCurrentDto
import com.callbackdev.chiaro.data.remote.dto.CurrentDto
import com.callbackdev.chiaro.data.remote.dto.DailyDto
import com.callbackdev.chiaro.data.remote.dto.ForecastResponseDto
import com.callbackdev.chiaro.data.remote.dto.HourlyDto
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
            current = CurrentDto(
                time = currentTime,
                temperatureC = 22.4,
                humidityPct = 65,
                apparentTemperatureC = 24.1,
                dewPointC = 15.6,
                isDay = 1,
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
        airQuality: AirQualityCurrentDto? = null
    ) = WeatherReportMapper.map(
        city = city,
        forecast = forecast,
        airQuality = airQuality,
        fetchedAt = fetchedAt,
        responseTimeMs = 245,
        cacheStatus = CacheStatus.MISS
    )

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
    }

    @Test
    fun `current conditions convert units and derive labels`() {
        val current = map().current
        assertEquals(2, current.condition.wmoCode)
        assertEquals(22.4, current.tempC, 0.0)
        assertEquals(24.1, current.feelsLikeC, 0.0)
        assertEquals(65, current.humidityPct)
        assertEquals(16.09, current.visibilityKm!!, 1e-9)      // meters → km
        assertEquals(5, current.uvIndex)                     // 5.4 rounds down
        assertEquals("NW", current.wind.directionCompass)    // 310°
        assertEquals(310, current.wind.degree)
        // The chance of the hour under way is the slot that ends after 14:23 — 15:00,
        // which the fixture leaves without one — not 14:00's 40%, which is 13-14's.
        assertNull(current.precipitation.chancePct)
        // `current.precipitation` is the provider's quarter of an hour, named as such.
        assertEquals(0.5, current.precipitation.lastQuarterHourMm, 0.0)
    }

    @Test
    fun `hourly window starts at the current hour and keeps every slot after it`() {
        // Slot 0 is the current hour (current_conditions' rain chance, the engines'
        // anchor); the views drop it and still show a full day. Since Fase 16a the
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
                // about whether the day is wet enough to be called wet (Fase 26).
                precipitationMm = codes.map { if (it >= 51) 1.5 else 0.0 },
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

    private fun statusOfFirstDay(codes: List<Int>, providerCode: Int) =
        map(forecast = dayOf(codes, providerCode)).daily.first().condition.wmoCode

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
    fun `dense fog the provider called overcast reads as fog`() {
        // Same city, 07:00: 160 m of visibility served as code 3. The dangerous direction.
        assertEquals(45, statusOfCurrentHour(3, 160.0, 100))
    }

    @Test
    fun `fog that really is fog is left alone`() {
        assertEquals(45, statusOfCurrentHour(45, 40.0, 100))
    }

    @Test
    fun `precipitation is never rewritten by the fog repair`() {
        // Rain is not derived from visibility, and it can rain in fog.
        assertEquals(61, statusOfCurrentHour(61, 40.0, 100))
    }

    @Test
    fun `a missing visibility leaves the provider code untouched`() {
        assertEquals(45, statusOfCurrentHour(45, null, 10))
    }

    @Test
    fun `current conditions get the same repair as the hours`() {
        val base = forecast()
        val report = map(
            forecast = base.copy(
                current = base.current.copy(weatherCode = 45, visibilityM = 9760.0, cloudCoverPct = 59)
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

    // ---------------------------------------- Fase 26: fog needs a run to be invented

    @Test
    fun `a single hour below the threshold is not invented into fog`() {
        // Measured 6 Sep 2026 on 23 cities: rewriting isolated hours took the week's
        // fog transitions from 10 to 18. Fog is not one hour long at this resolution.
        assertEquals(0, statusOfCurrentHour(0, 160.0, 100, span = 1))
    }

    @Test
    fun `two hours below the threshold still are`() {
        assertEquals(45, statusOfCurrentHour(0, 160.0, 100, span = 2))
    }

    @Test
    fun `a contradicted fog code is dropped even when it stands alone`() {
        // The rule is one-sided on purpose: there is no run argument for KEEPING a
        // value the provider's own visibility disagrees with, and 68% of the fog codes
        // served are contradicted.
        assertEquals(2, statusOfCurrentHour(45, 9760.0, 59, span = 1))
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
        val codes = List(24) { if (it == 9) 95 else 0 }
        assertEquals(95, dayOfWith(codes, mm = 0.0, providerCode = 95))
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
    fun `a tie between two skies goes to the heavier one`() {
        val codes = List(24) { if (it in 6..12) 1 else 3 }
        assertEquals(3, statusOfFirstDay(codes, providerCode = 3))
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
        mm: (Int) -> Double = { if (codes[it] >= 51) 1.5 else 0.0 },
        chance: (Int) -> Int? = { null }
    ): Int {
        val base = dayOf(codes, providerCode = 3)
        return map(
            forecast = base.copy(
                hourly = base.hourly.copy(
                    precipitationMm = List(24, mm),
                    precipitationProbabilityPct = List(24, chance)
                )
            )
        ).daily.first().condition.wmoCode
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

    // ------------------------------ 24 set 2026: the banner's chance floor (P5)

    @Test
    fun `an improbable storm does not label the day`() {
        // AlertEngine drops a storm under 20%; the week's row printed it anyway.
        val codes = List(24) { if (it == 15) 95 else 0 }
        assertEquals(0, dayCode(codes, chance = { if (it == 15) 10 else 0 }))
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
    fun `hours at 0 percent do not make a day of snow`() {
        // Six hours of trace snow, all at 0%: the rows draw them as sky, and so must the day.
        val codes = List(24) { if (it in 1..6) 71 else 3 }
        assertEquals(3, dayCode(codes, mm = { if (codes[it] == 71) 0.1 else 0.0 }, chance = { 0 }))
        // At 5% they are believed, and six hours of them are the day's.
        assertEquals(71, dayCode(codes, mm = { if (codes[it] == 71) 0.1 else 0.0 }, chance = { 5 }))
    }

    @Test
    fun `precipitation that did not earn the day does not win its sky either`() {
        // Daylight 06-19, two hours apiece of every sky and two of drizzle: the old
        // tie-break handed the day to 51. Its hours now vote with their cloud (100% → 3).
        val daylight = listOf(0, 0, 1, 1, 2, 2, 3, 3, 51, 51, 45, 45, 48, 48)
        val codes = List(24) { if (it in 6..19) daylight[it - 6] else 0 }
        assertEquals(3, dayCode(codes, mm = { if (codes[it] == 51) 0.1 else 0.0 }))
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
    private fun rows(edit: (HourlyDto) -> HourlyDto) = map(forecast = forecast().let { f ->
        f.copy(hourly = edit(f.hourly))
    }).hourly

    @Test
    fun `a row carries the rain, the chance and the gust of the hour it starts`() {
        val n = forecast().hourly.time.size
        val hours = rows { h ->
            h.copy(
                weatherCode = List(n) { if (it == 15) 63 else 0 },
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

    @Test
    fun `a snowflake over 0 percent is the sky its hour starts with`() {
        // Longyearbyen, 24 set 2026: the deterministic model's trace of snow against an
        // ensemble in which no member reaches 0.1 mm drew a snowflake over «0%» in one cell.
        val n = forecast().hourly.time.size
        val hours = rows { h ->
            h.copy(
                weatherCode = List(n) { if (it in 15..17) 71 else 3 },
                precipitationProbabilityPct = List(n) { if (it == 15) 0 else if (it == 16) 1 else null },
                cloudCoverPct = List(n) { 100 }
            )
        }
        // Row 14 is slot 15 (0%): its sky. Row 15 is slot 16 (1%): the chance says it can.
        // Row 16 is slot 17, no chance at all: no evidence against the code.
        assertEquals(listOf(3, 71, 71), hours.take(3).map { it.condition.wmoCode })
        assertEquals(0, hours.first().precipChancePct)
    }

    @Test
    fun `a hazard code keeps its hour at 0 percent too`() {
        val n = forecast().hourly.time.size
        val hours = rows { h ->
            h.copy(
                weatherCode = List(n) { if (it == 15) 56 else 0 },
                precipitationProbabilityPct = List(n) { 0 }
            )
        }
        assertEquals(56, hours.first().condition.wmoCode)
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
    fun `the day carries how much falls, for how long, the snow and the strongest gust`() {
        val base = forecast()
        val day = map(
            forecast = base.copy(
                daily = base.daily.copy(
                    precipitationSumMm = List(8) { if (it == 0) 12.4 else null },
                    precipitationHours = List(8) { if (it == 0) 5.0 else null },
                    snowfallSumCm = List(8) { if (it == 0) 0.0 else null },
                    windGustsMaxKph = List(8) { if (it == 0) 61.2 else null }
                )
            )
        ).daily
        assertEquals(12.4, day[0].precipMm!!, 0.0)
        assertEquals(5.0, day[0].precipHours!!, 0.0)
        assertEquals(0.0, day[0].snowCm!!, 0.0)
        assertEquals(61.2, day[0].gustMaxKph!!, 0.0)
        assertNull(day[1].precipMm)
        // No split asked for and no snow: the total is all rain.
        assertEquals(12.4, day[0].rainMm!!, 0.0)
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
        assertEquals(0.2, day(rain = 0.2, showers = null, snow = 0.4).rainMm!!, 0.0)
        // No split, and snow in the total: which part is rain cannot be told.
        assertNull(day(rain = null, showers = null, snow = 0.4).rainMm)
        assertNull(day(rain = null, showers = null, snow = null).rainMm)
    }

    /**
     * Since Fase 16e these come from [AstronomyEngine] and not from the provider's
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
            responseTimeMs = 1,
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
        assertEquals(245, info.responseTimeMs)
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
    fun `air quality maps pollutants and converts co to mg`() {
        val report = map(
            airQuality = AirQualityCurrentDto(
                time = "2026-08-13T14:00",
                usAqi = 42,
                pm25 = 12.5, pm10 = 20.1, ozone = 45.2, no2 = 15.3, so2 = 2.1,
                co = 300.0 // µg/m³ from the API
            )
        )
        val aq = report.airQuality!!
        assertEquals(42, aq.aqiIndex)
        assertEquals(12.5, aq.pollutants.pm25!!, 0.0)
        assertEquals(0.3, aq.pollutants.coMg!!, 1e-9)          // µg → mg
        assertEquals(15.3, aq.pollutants.no2!!, 1e-9)
    }

    @Test
    fun `missing pollutants stay missing rather than clean`() {
        // They defaulted to 0.0 until 24 set 2026: clean air nobody had measured.
        val report = map(
            airQuality = AirQualityCurrentDto(time = "2026-08-13T14:00", usAqi = 66)
        )
        val pollutants = report.airQuality!!.pollutants
        assertNull(pollutants.pm25)
        assertNull(pollutants.coMg)
    }

    @Test
    fun `pollen tree level is the worst of birch alder olive, each on its own scale`() {
        val report = map(
            airQuality = AirQualityCurrentDto(
                time = "2026-08-13T14:00",
                usAqi = 42,
                grassPollen = 0.4,     // NONE
                birchPollen = 5.0,     // LOW…
                alderPollen = 120.0,   // …but alder is HIGH (70-249) → tree = HIGH
                olivePollen = 0.0,
                ragweedPollen = 35.0,  // HIGH for ragweed (11-39); it was «moderate» on the old
                mugwortPollen = 2.0    // single scale, which is the point of 24 set 2026
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
        fun grass(g: Double) = map(
            airQuality = AirQualityCurrentDto(
                time = "2026-08-13T14:00", usAqi = 42, grassPollen = g, birchPollen = 0.0,
                alderPollen = 0.0, olivePollen = 0.0, ragweedPollen = 0.0, mugwortPollen = 0.0
            )
        ).pollen!!.grass
        assertEquals(PollenLevel.LOW, grass(19.0))
        assertEquals(PollenLevel.MODERATE, grass(20.0))
        assertEquals(PollenLevel.HIGH, grass(149.0))
        assertEquals(PollenLevel.VERY_HIGH, grass(150.0))
        fun olive(o: Double) = map(
            airQuality = AirQualityCurrentDto(
                time = "2026-08-13T14:00", usAqi = 42, grassPollen = 0.0, birchPollen = 0.0,
                alderPollen = 0.0, olivePollen = o, ragweedPollen = 0.0, mugwortPollen = 0.0
            )
        ).pollen!!.tree
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
            fetchedAt = fetchedAt, responseTimeMs = 1, cacheStatus = CacheStatus.MISS
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
            fetchedAt = fetchedAt, responseTimeMs = 1, cacheStatus = CacheStatus.MISS
        ).airQuality!!
        assertEquals(AqiScale.US, usOnly.scale)
    }

    @Test
    fun `pollen report is null outside pollen coverage`() {
        // US location: air quality present, every pollen field absent.
        val report = map(
            airQuality = AirQualityCurrentDto(time = "2026-08-13T14:00", usAqi = 42)
        )
        assertNull(report.pollen)
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
            responseTimeMs = 1,
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
            responseTimeMs = 1,
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
            responseTimeMs = 1,
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
}
