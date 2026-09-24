package com.callbackdev.chiaro.domain.sample

import com.callbackdev.chiaro.domain.model.AirQuality
import com.callbackdev.chiaro.domain.model.Astronomical
import com.callbackdev.chiaro.domain.model.CacheStatus
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.model.CurrentConditions
import com.callbackdev.chiaro.domain.model.DailyForecast
import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.model.Location
import com.callbackdev.chiaro.domain.model.MoonPhase
import com.callbackdev.chiaro.domain.model.PollenLevel
import com.callbackdev.chiaro.domain.model.PollenReport
import com.callbackdev.chiaro.domain.model.Pollutants
import com.callbackdev.chiaro.domain.model.Precipitation
import com.callbackdev.chiaro.domain.model.SystemInfo
import com.callbackdev.chiaro.domain.model.WeatherCondition
import com.callbackdev.chiaro.domain.model.WeatherReport
import com.callbackdev.chiaro.domain.model.Wind
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * The PRD's `weather_data.json_full_sample.json` as a domain object, for `@Preview`s
 * that must match the mockups without touching the network.
 */
fun sampleWeatherReport(): WeatherReport {
    val partlyCloudy = WeatherCondition(2)
    val sunny = WeatherCondition(0)
    val clearNight = WeatherCondition(0)
    val baseDate = LocalDate.of(2023, 10, 27)
    return WeatherReport(
        location = Location(
            city = "New York",
            region = "NY",
            country = "USA",
            coordinates = Coordinates(40.7128, -74.0060),
            timezone = "America/New_York",
            localTime = LocalDateTime.of(2023, 10, 27, 14, 30)
        ),
        current = CurrentConditions(
            condition = partlyCloudy,
            tempC = 18.5,
            feelsLikeC = 17.2,
            humidityPct = 54,
            dewPointC = 9.0,
            visibilityKm = 16.1,
            pressureMb = 1015.2,
            uvIndex = 4,
            wind = Wind(12.5, "NW", 310, 18.0),
            precipitation = Precipitation(0.0, emptyList(), 10)
        ),
        airQuality = AirQuality(
            aqiIndex = 42,
            pollutants = Pollutants(8.2, 15.5, 35.1, 12.4, 2.1, 0.4)
        ),
        pollen = PollenReport(
            grass = PollenLevel.LOW,
            tree = PollenLevel.HIGH,
            weed = PollenLevel.MODERATE
        ),
        astronomical = Astronomical(
            sunrise = LocalTime.of(7, 12),
            sunset = LocalTime.of(18, 4),
            moonPhase = MoonPhase.WAXING_GIBBOUS,
            daylightDuration = Duration.ofHours(10).plusMinutes(52)
        ),
        // Cloud cover tracks the condition of each row (Fase 16a): the field is not
        // rendered anywhere, but a sample whose sunny hour is 90% overcast would be a
        // trap for the first sky verdict written against it.
        hourly = listOf(
            sampleHour(baseDate.atTime(15, 0), 19.0, sunny, 0, 5),
            sampleHour(baseDate.atTime(16, 0), 18.0, sunny, 0, 10),
            sampleHour(baseDate.atTime(17, 0), 17.0, partlyCloudy, 5, 45),
            sampleHour(baseDate.atTime(18, 0), 15.0, partlyCloudy, 10, 55),
            sampleHour(baseDate.atTime(19, 0), 14.0, clearNight, 0, 8)
        ),
        daily = listOf(
            DailyForecast(baseDate.plusDays(3), 20.0, 12.0, sunny, 0, 5),
            DailyForecast(baseDate.plusDays(4), 18.0, 11.0, WeatherCondition(63), 85, 2),
            DailyForecast(baseDate.plusDays(5), 16.0, 10.0, WeatherCondition(3), 20, 3),
            DailyForecast(baseDate.plusDays(6), 19.0, 13.0, partlyCloudy, 10, 6)
        ),
        systemInfo = SystemInfo(
            source = "Open-Meteo API",
            lastSync = Instant.ofEpochSecond(1_698_413_400),
            cacheStatus = CacheStatus.HIT,
            responseTimeMs = 142
        )
    )
}

/**
 * One sample hour, with its [HourlyForecast.at] derived from the sample's own zone —
 * the one thing a hand-written row cannot be allowed to make up, since the strip keys
 * its cells on it and the day/night flag is read from it.
 */
private fun sampleHour(
    time: LocalDateTime,
    tempC: Double,
    condition: WeatherCondition,
    precipChancePct: Int?,
    cloudCoverPct: Int
) = HourlyForecast(
    time = time,
    at = time.atZone(SampleZone).toInstant(),
    tempC = tempC,
    condition = condition,
    precipChancePct = precipChancePct,
    cloudCoverPct = cloudCoverPct
)

/** The sample's city, as a zone: `America/New_York`, like its `location.timezone`. */
private val SampleZone: ZoneId = ZoneId.of("America/New_York")
