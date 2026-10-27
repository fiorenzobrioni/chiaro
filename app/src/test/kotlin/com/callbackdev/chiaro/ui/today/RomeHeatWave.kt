package com.callbackdev.chiaro.ui.today

import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.model.WeatherCondition
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Rome, 30 and 31 July 2026, from Open-Meteo's archive of past forecasts (the
 * `historical-forecast-api`, `best_match`, 41.9028 N 12.4964 E, Europe/Rome; read on 27 Oct
 * 2026): the hottest days of that July by feels-like, 41.3 °C at 13:00 on the 30th and 41.0 on
 * the 31st. The summer fixture of Fase 14, step 14.6: the heat alert and the heat rung of the
 * headline are tested on a day that happened, not on a hand-written one. One value an hour
 * from 00:00 on the 30th: the air temperature, the apparent temperature, the code (0–2, a
 * clear sky throughout) and the cloud cover. The same table lives in `:core:domain`'s tests
 * and in `:app`'s, which cannot read each other's test sources.
 */
object RomeHeatWave {

    val zone: ZoneId = ZoneId.of("Europe/Rome")
    val start: LocalDateTime = LocalDateTime.of(2026, 7, 30, 0, 0)

    val tempC: DoubleArray = doubleArrayOf(
        26.9, 26.7, 26.5, 26.2, 25.5, 25.1, 25.0, 26.4, 28.4, 30.9, 33.4, 35.5, 37.0, 37.6, 37.3,
        37.0, 36.5, 35.6, 34.6, 33.6, 31.8, 30.2, 28.9, 27.9, 27.3, 26.5, 26.0, 25.6, 25.0, 24.6,
        24.5, 26.3, 28.7, 31.5, 33.8, 35.9, 37.2, 37.5, 37.1, 36.3, 35.5, 35.0, 33.7, 32.0, 30.2,
        29.0, 28.1, 27.5
    )

    val feelsLikeC: DoubleArray = doubleArrayOf(
        32.5, 32.2, 31.4, 30.9, 29.8, 29.3, 29.0, 29.9, 31.5, 33.8, 36.9, 39.4, 40.7, 41.3, 39.1,
        37.7, 36.7, 35.6, 35.1, 35.2, 34.6, 33.4, 32.3, 31.8, 31.4, 30.0, 29.0, 28.4, 27.6, 27.2,
        27.0, 27.9, 30.3, 32.7, 36.2, 39.3, 40.3, 41.0, 39.3, 38.1, 36.4, 36.6, 35.6, 35.0, 33.5,
        32.7, 32.1, 32.2
    )

    val weatherCode: IntArray = intArrayOf(
        2, 2, 2, 1, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1,
        1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0
    )

    val cloudCoverPct: IntArray = intArrayOf(
        58, 54, 48, 26, 7, 4, 5, 5, 3, 0, 0, 0, 0, 3, 0, 0, 6, 0, 0, 6, 2, 0, 0, 0, 0, 0, 0, 0,
        0, 5, 17, 0, 0, 0, 0, 0, 0, 11, 0, 12, 0, 0, 0, 1, 0, 0, 0, 0
    )

    /** The 48 hours as the report carries them, the chance of rain the archive's own zero. */
    fun hours(): List<HourlyForecast> = tempC.indices.map { i ->
        val time = start.plusHours(i.toLong())
        HourlyForecast(
            time = time,
            at = time.atZone(zone).toInstant(),
            tempC = tempC[i],
            condition = WeatherCondition(weatherCode[i]),
            precipChancePct = 0,
            cloudCoverPct = cloudCoverPct[i],
            feelsLikeC = feelsLikeC[i]
        )
    }
}
