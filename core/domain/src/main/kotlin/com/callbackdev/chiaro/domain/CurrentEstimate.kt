package com.callbackdev.chiaro.domain

import com.callbackdev.chiaro.domain.model.CurrentConditions
import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.model.Wind
import com.callbackdev.chiaro.domain.model.WeatherReport
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt

/**
 * The conditions "now" when the provider's own `current` block has grown too old to be
 * now (24 set 2026, suggestion 3 of the engine review).
 *
 * The block is a quarter of an hour of the model, stamped when it was fetched. A phone
 * that has been offline since the morning kept printing the morning in the hero — 22°
 * and sunny under a freshness chip saying "5 hours ago", while the hourly forecast the
 * same report carries said 15° and overcast for this very hour. The chip told the truth
 * about the data's age and the hero was still the wrong answer to "what is it like now":
 * past [MAX_AGE] the forecast for now is closer to now than an observation of then.
 *
 * So from [MAX_AGE] on the conditions are rebuilt from the hour rows around [now]:
 *
 * - **instants interpolated** between the row at or before now and the one after —
 *   temperature, feels-like, humidity, dew point, pressure, steady wind, UV, visibility,
 *   total cloud — because those are values AT an instant and now lies between two;
 * - **the wind's direction and the cloud's layers from the nearer row**: an average of
 *   350° and 10° is not 180°, and layers are three numbers that have to agree;
 * - **the sky, the gust and the chance of rain from the row now falls in**: those describe
 *   the hour under way (see the mapper's rows, P7b).
 *
 * It needs the rows to carry the rest of an hour (fetched since the same day): a report
 * whose rows lack feels-like, humidity, dew point, pressure or wind is not estimated at
 * all, rather than showing a now made of two different hours. The air and the pollen are
 * never estimated — nothing forecasts them here — and stay what was measured.
 *
 * Every estimated block says so ([CurrentConditions.estimated]), and so does every
 * surface that prints one (DESIGN §1.1: a computed value is labelled as one).
 */
object CurrentEstimate {

    /**
     * The age past which the forecast for now replaces the fetched `current` block. An
     * hour: the block describes a quarter of an hour, and by the next hour the forecast
     * row for it is the better witness. Four times the report cache's TTL, so a page the
     * reader is looking at while online never gets here.
     */
    val MAX_AGE: Duration = Duration.ofMinutes(60)

    private val HOUR: Duration = Duration.ofHours(1)

    /** [report] with its `current` replaced by the estimate for [now] when it is due and
     * possible, unchanged otherwise. */
    fun apply(report: WeatherReport, now: Instant): WeatherReport {
        if (Duration.between(report.systemInfo.lastSync, now) < MAX_AGE) return report
        val estimate = estimate(report, now) ?: return report
        return report.copy(current = estimate)
    }

    /** The conditions at [now] from the report's hours, or null when they cannot say. */
    fun estimate(report: WeatherReport, now: Instant): CurrentConditions? {
        val hours = report.hourly
        val index = hours.indexOfLast { !it.at.isAfter(now) }
        if (index < 0) return null
        val row = hours[index]
        // The row must be the hour now is in: a gap in the rows is not an hour to lean on.
        if (Duration.between(row.at, now) >= HOUR) return null
        val next = hours.getOrNull(index + 1)?.takeIf { Duration.between(row.at, it.at) == HOUR }
        val f = next?.let { Duration.between(row.at, now).seconds.toDouble() / HOUR.seconds } ?: 0.0
        val nearer = if (next != null && f >= 0.5) next else row

        fun lerp(a: Double?, b: Double?): Double? = when {
            a == null -> null
            b == null -> a
            else -> a + (b - a) * f
        }
        fun lerpOf(pick: (HourlyForecast) -> Double?) = lerp(pick(row), next?.let(pick))

        val feelsLike = lerpOf { it.feelsLikeC } ?: return null
        val humidity = lerpOf { it.humidityPct?.toDouble() } ?: return null
        val dewPoint = lerpOf { it.dewPointC } ?: return null
        val pressure = lerpOf { it.pressureMb } ?: return null
        val windKph = lerpOf { it.windKph } ?: return null
        val direction = nearer.windDirectionDeg ?: row.windDirectionDeg ?: return null

        val current = report.current
        return current.copy(
            condition = row.condition,
            tempC = lerpOf { it.tempC }!!,
            feelsLikeC = feelsLike,
            humidityPct = humidity.roundToInt(),
            dewPointC = dewPoint,
            visibilityKm = lerpOf { it.visibilityKm },
            pressureMb = pressure,
            uvIndex = lerpOf { it.uvIndex }?.roundToInt(),
            wind = Wind(
                speedKph = windKph,
                directionCompass = WeatherCodes.windCompass(direction),
                degree = direction,
                // The strongest gust of the hour under way; a row without one keeps the
                // steady wind rather than a gust nobody forecast.
                gustKph = maxOf(row.gustKph ?: windKph, windKph)
            ),
            precipitation = current.precipitation.copy(chancePct = row.precipChancePct),
            cloudCoverPct = lerpOf { it.cloudCoverPct.toDouble() }?.roundToInt(),
            cloudLayers = nearer.cloudLayers,
            estimated = true
        )
    }
}
