package com.callbackdev.chiaro.ui.today

import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.model.WeatherCondition
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Two gales from Open-Meteo's archive of past forecasts (the `historical-forecast-api`,
 * `best_match`, read on 27 Oct 2026), the winter fixtures of Fase 14, step 14.7: the wind in
 * «Maltempo» is tested on days that happened, not on hand-written ones. One value an hour: the
 * air and apparent temperature, the code, the cloud cover, the chance of rain, the wind and the
 * strongest gust of the hour that starts then. The same tables live in `:core:domain`'s tests
 * and in `:app`'s, which cannot read each other's test sources.
 *
 * **Trieste, 23–25 December 2025** (45.6495 N 13.7768 E, 18 m, Europe/Rome): a bora, ENE
 * throughout (56–67°), rising through the evening of the 23rd — 65 km/h at 23:00, 73 at
 * midnight — to 112 km/h at 08:00 on the 24th, and still over 100 at the end of the 25th. The
 * sky is the bora's: overcast with light rain on the 24th, clearing on the 25th.
 *
 * **Genoa, 22–23 March 2025** (44.4056 N 8.9463 E, 11 m, Europe/Rome): a libeccio, the
 * strongest south-westerly of two winters in the archive — 71.6 km/h at 01:00 on the 23rd from
 * 203°, one hour over the lowland bar between a 59.8 and a 52.2 — three hours after a
 * thunderstorm at 22:00 (code 96, 98%) that is the day's own hazard.
 */
object WinterGales {

    val zone: ZoneId = ZoneId.of("Europe/Rome")

    object Trieste {
        const val elevationM = 18.0
        val start: LocalDateTime = LocalDateTime.of(2025, 12, 23, 0, 0)

        val tempC = doubleArrayOf(
            10.8, 10.7, 11.1, 10.9, 10.4, 10.7, 10.6, 10.8, 11.1, 11.4, 11.8, 12.0, 11.8, 12.1, 12.4, 12.4,
            11.7, 11.0, 10.8, 10.3, 9.6, 9.3, 9.1, 8.6, 7.9, 7.5, 7.3, 7.0, 7.1, 6.8, 6.6, 6.7, 6.2, 6.2, 5.9,
            6.1, 6.5, 6.6, 6.4, 6.3, 6.0, 5.7, 5.7, 5.4, 5.3, 5.4, 5.5, 5.2, 5.2, 5.2, 5.1, 5.0, 4.9, 4.7, 4.7,
            4.9, 5.0, 5.4, 6.0, 6.3, 6.6, 6.9, 6.8, 6.5, 5.9, 5.4, 5.3, 5.2, 5.0, 4.9, 5.3, 5.2
        )
        val feelsLikeC = doubleArrayOf(
            8.0, 8.0, 8.5, 8.4, 7.8, 8.1, 8.4, 8.6, 8.8, 9.1, 9.2, 9.5, 9.4, 9.7, 10.0, 10.0, 8.7, 7.6, 7.4,
            6.3, 5.2, 4.4, 3.5, 2.0, 0.7, -0.4, -1.0, -1.6, -1.7, -2.3, -2.9, -2.8, -3.2, -2.9, -2.6, -2.4,
            -2.1, -2.3, -2.4, -3.0, -3.5, -3.8, -3.8, -4.1, -4.2, -3.9, -3.8, -4.1, -4.1, -4.5, -4.3, -4.5,
            -4.4, -4.5, -4.7, -4.0, -4.0, -3.6, -2.5, -2.3, -2.0, -1.4, -1.4, -1.9, -2.3, -3.1, -3.2, -2.9,
            -3.1, -3.1, -2.7, -2.7
        )
        val weatherCode = intArrayOf(
            3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 61, 61, 61, 61, 61, 3, 3, 3, 3, 3, 3, 3, 3, 61, 61, 61, 3, 61, 61, 3,
            3, 3, 61, 61, 61, 61, 61, 61, 61, 3, 3, 3, 3, 3, 3, 3, 2, 2, 2, 2, 1, 0, 2, 1, 1, 1, 2, 3, 2, 3, 3,
            3, 3, 2, 2, 0, 0, 1, 0, 0, 0, 0
        )
        val cloudCoverPct = intArrayOf(
            99, 97, 100, 100, 92, 95, 95, 99, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100,
            100, 100, 100, 100, 100, 100, 100, 100, 98, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100,
            100, 100, 100, 100, 100, 100, 100, 71, 83, 75, 70, 43, 0, 39, 27, 3, 18, 66, 100, 100, 100, 100,
            100, 100, 29, 100, 0, 0, 0, 0, 0, 0, 0
        )
        val precipChancePct = intArrayOf(
            43, 43, 45, 48, 53, 55, 55, 65, 45, 45, 40, 43, 53, 65, 55, 55, 45, 53, 68, 73, 58, 70, 80, 85,
            80, 75, 63, 55, 58, 38, 33, 43, 18, 68, 98, 100, 93, 78, 48, 40, 60, 75, 70, 55, 13, 8, 5, 0, 3,
            5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        )
        val windKph = doubleArrayOf(
            11.1, 11.0, 11.1, 10.3, 10.5, 10.6, 9.2, 9.9, 10.3, 10.1, 12.1, 10.9, 11.1, 10.9, 10.9, 10.9,
            13.8, 15.9, 15.8, 18.9, 20.4, 22.6, 27.1, 32.2, 36.3, 40.6, 42.8, 45.2, 46.5, 48.2, 50.4, 50.7,
            50.9, 48.3, 44.5, 44.0, 44.6, 46.4, 45.3, 48.2, 50.1, 48.9, 49.1, 49.3, 49.1, 47.5, 47.7, 47.7,
            47.5, 50.1, 48.5, 48.8, 47.5, 46.6, 47.9, 45.0, 45.0, 44.9, 42.2, 42.9, 43.0, 41.4, 40.6, 42.2,
            40.6, 42.4, 42.2, 39.0, 39.3, 39.2, 38.6, 38.3
        )
        val gustKph = doubleArrayOf(
            22.0, 20.9, 21.6, 20.5, 19.1, 20.9, 20.5, 18.4, 21.6, 19.8, 19.1, 23.0, 21.2, 20.9, 21.6, 20.9,
            24.8, 30.6, 30.2, 33.5, 39.6, 43.6, 49.0, 65.2, 73.4, 85.0, 90.0, 96.5, 99.4, 103.7, 110.2,
            110.9, 112.0, 109.4, 108.0, 97.2, 97.6, 101.5, 99.7, 105.5, 107.6, 108.0, 105.8, 110.5, 108.4,
            106.9, 103.0, 104.0, 103.3, 107.6, 108.0, 105.8, 106.6, 103.0, 105.5, 105.8, 99.7, 100.1, 99.4,
            95.8, 97.6, 95.8, 92.2, 94.3, 96.8, 97.2, 97.9, 95.4, 92.5, 90.4, 90.4, 88.6
        )

        fun hours(): List<HourlyForecast> = rows(start, tempC, feelsLikeC, weatherCode, cloudCoverPct, precipChancePct, windKph, gustKph)
    }

    object Genoa {
        const val elevationM = 11.0
        val start: LocalDateTime = LocalDateTime.of(2025, 3, 22, 0, 0)

        val tempC = doubleArrayOf(
            13.1, 13.0, 12.6, 12.5, 11.8, 11.2, 10.4, 10.0, 10.3, 10.3, 11.4, 11.5, 11.7, 11.7, 12.8, 13.1,
            13.4, 13.7, 13.2, 12.5, 12.1, 12.0, 12.9, 13.0, 13.2, 13.8, 12.3, 11.6, 12.2, 12.2, 12.4, 12.7,
            13.3, 13.9, 14.3, 14.5, 14.6, 15.3, 15.5, 15.4, 15.0, 14.6, 14.1, 13.8, 12.6, 12.0, 11.9, 12.1
        )
        val feelsLikeC = doubleArrayOf(
            11.9, 11.4, 9.5, 10.0, 10.4, 9.0, 7.3, 5.7, 6.3, 6.5, 7.4, 7.5, 8.1, 7.6, 9.8, 10.7, 12.1, 12.2,
            11.2, 10.0, 9.5, 9.4, 12.5, 9.8, 9.6, 9.2, 10.9, 10.4, 10.9, 10.9, 10.7, 10.6, 11.3, 11.8, 12.4,
            12.7, 13.1, 13.7, 13.5, 13.6, 13.3, 13.5, 13.6, 13.6, 12.1, 10.8, 10.3, 10.6
        )
        val weatherCode = intArrayOf(
            61, 61, 61, 61, 61, 61, 63, 63, 3, 61, 63, 80, 3, 80, 3, 3, 61, 80, 80, 80, 61, 61, 96, 61, 61,
            80, 2, 3, 80, 3, 3, 61, 80, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 61, 61
        )
        val cloudCoverPct = intArrayOf(
            100, 100, 100, 100, 100, 100, 100, 97, 100, 100, 100, 100, 100, 100, 100, 100, 95, 100, 96,
            100, 100, 100, 100, 100, 100, 94, 59, 95, 94, 94, 100, 99, 94, 93, 100, 99, 100, 100, 100, 96,
            99, 100, 100, 84, 100, 100, 100, 100
        )
        val precipChancePct = intArrayOf(
            98, 100, 100, 100, 100, 100, 100, 100, 100, 100, 88, 80, 53, 25, 10, 15, 23, 40, 50, 73, 93,
            100, 98, 100, 100, 98, 93, 28, 40, 80, 95, 93, 93, 70, 53, 48, 55, 48, 35, 35, 48, 50, 35, 25,
            10, 20, 38, 70
        )
        val windKph = doubleArrayOf(
            7.1, 10.7, 20.2, 17.4, 8.7, 10.8, 15.5, 19.9, 19.3, 19.2, 18.9, 19.1, 16.8, 21.2, 14.3, 11.0,
            6.6, 8.6, 10.5, 13.4, 14.7, 14.5, 5.8, 26.7, 30.6, 31.8, 9.1, 7.0, 7.9, 9.0, 12.3, 14.7, 15.8,
            16.3, 15.6, 14.7, 12.8, 12.9, 14.8, 13.5, 12.2, 8.0, 4.5, 1.5, 4.4, 6.3, 7.5, 6.8
        )
        val gustKph = doubleArrayOf(
            14.0, 45.4, 42.1, 40.3, 33.8, 22.7, 33.8, 39.6, 38.2, 37.8, 36.7, 34.9, 33.5, 34.6, 37.4, 24.5,
            18.7, 15.5, 22.7, 27.4, 30.2, 28.1, 26.3, 50.0, 59.8, 71.6, 52.2, 15.1, 16.9, 24.1, 34.6, 38.5,
            34.2, 36.4, 35.3, 31.7, 35.3, 34.9, 35.3, 30.6, 24.8, 25.9, 16.6, 9.0, 8.6, 11.9, 11.9, 14.8
        )

        fun hours(): List<HourlyForecast> = rows(start, tempC, feelsLikeC, weatherCode, cloudCoverPct, precipChancePct, windKph, gustKph)
    }

    private fun rows(
        start: LocalDateTime,
        tempC: DoubleArray,
        feelsLikeC: DoubleArray,
        weatherCode: IntArray,
        cloudCoverPct: IntArray,
        precipChancePct: IntArray,
        windKph: DoubleArray,
        gustKph: DoubleArray
    ): List<HourlyForecast> = tempC.indices.map { i ->
        val time = start.plusHours(i.toLong())
        HourlyForecast(
            time = time,
            at = time.atZone(zone).toInstant(),
            tempC = tempC[i],
            condition = WeatherCondition(weatherCode[i]),
            precipChancePct = precipChancePct[i],
            cloudCoverPct = cloudCoverPct[i],
            feelsLikeC = feelsLikeC[i],
            windKph = windKph[i],
            gustKph = gustKph[i]
        )
    }
}
