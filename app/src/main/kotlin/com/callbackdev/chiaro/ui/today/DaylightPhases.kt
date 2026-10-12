package com.callbackdev.chiaro.ui.today

import com.callbackdev.chiaro.domain.WmoCode
import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.sky.SolarDay
import com.callbackdev.chiaro.ui.components.LightPhase
import com.callbackdev.chiaro.ui.components.RibbonHour
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Turns one [SolarDay] into the ribbon's segments (DESIGN.md §4). The ribbon paints
 * altitudes through the same `SkyPalette` as the canvas, so all this decides is WHICH
 * altitude each stretch of the day wears — one value per phase, at the middle of the
 * band it represents, because the ribbon depicts phases, it does not re-plot the sun.
 */
object DaylightPhases {

    private const val NIGHT = -30.0
    private const val ASTRONOMICAL = -15.0
    private const val NAUTICAL = -9.0
    private const val CIVIL = -4.0
    private const val GOLDEN = 2.0
    private const val DAY = 30.0

    fun phases(day: SolarDay, zone: ZoneId): List<LightPhase> {
        if (day.sunUpAllDay) return listOf(LightPhase(0f, 1f, DAY))

        // Each boundary starts the altitude that follows it; a null boundary is a
        // phase that day does not have (mid-summer mid-latitudes lose astronomical
        // darkness first), and simply drops out of the walk.
        val boundaries = listOf(
            day.astronomicalDawn to ASTRONOMICAL,
            day.nauticalDawn to NAUTICAL,
            day.civilDawn to CIVIL,
            day.sunrise to GOLDEN,
            day.goldenHourMorningEnd to DAY,
            day.goldenHourEveningStart to GOLDEN,
            day.sunset to CIVIL,
            day.civilDusk to NAUTICAL,
            day.nauticalDusk to ASTRONOMICAL,
            day.astronomicalDusk to NIGHT
        ).mapNotNull { (at, altitude) -> at?.let { fraction(it, zone) to altitude } }
            .sortedBy { it.first }

        // Before the first boundary the sky is one step darker than what that
        // boundary starts — which on a night with no astronomical darkness is
        // astronomical twilight, not black.
        val initial = when {
            day.astronomicalDawn != null -> NIGHT
            day.nauticalDawn != null -> ASTRONOMICAL
            day.civilDawn != null -> NAUTICAL
            day.sunrise != null -> CIVIL
            else -> NIGHT
        }

        val phases = mutableListOf<LightPhase>()
        var from = 0f
        var altitude = initial
        for ((at, next) in boundaries) {
            if (at > from) {
                phases += LightPhase(from, at, altitude)
                from = at
            }
            altitude = next
        }
        if (from < 1f) phases += LightPhase(from, 1f, altitude)
        return phases
    }

    /** Where in the ribbon a moment falls: fraction of ITS local day. */
    fun fraction(at: Instant, zone: ZoneId): Float = fraction(LocalDateTime.ofInstant(at, zone))

    fun fraction(local: LocalDateTime): Float =
        (local.toLocalTime().toSecondOfDay() / 86_400f).coerceIn(0f, 1f)

    /**
     * The forecast sky of [date] for its ribbon (DESIGN.md §4, 12 ott 2026): each of the
     * day's hours where its label falls, with its cover, its chance (none forecast is no
     * tint, the canvas' own "not told is not wet") and whether the app's state of the hour
     * has something falling. A possibility (`RAIN_LIKELY`) is not wet: it darkens the band
     * through its chance and gets no mark.
     *
     * The next day's midnight hour closes the day at 1, unmarked, so the last hour of the
     * evening blends into the night that follows rather than holding flat to the edge.
     *
     * [hourly] may start late — Today's report is trimmed to the hour we are in — and then
     * so does the sky: the hours already gone are not forecast any more, and the ribbon
     * draws no sky over them.
     */
    fun sky(hourly: List<HourlyForecast>, date: LocalDate): List<RibbonHour> {
        val day = hourly.filter { it.time.toLocalDate() == date }
        if (day.isEmpty()) return emptyList()
        val closing = hourly.firstOrNull { it.time.toLocalDate().isAfter(date) }
            ?.takeIf { it.time.toLocalDate() == date.plusDays(1) && it.time.toLocalTime() == LocalTime.MIDNIGHT }
        return day.map { it.ribbonHour(fraction(it.time), wet = WmoCode.isPrecipitation(it.condition.wmoCode)) } +
            listOfNotNull(closing?.ribbonHour(1f, wet = false))
    }

    /**
     * The runs of wet hours in [hours], first and last hour of each, for the words the
     * ribbon's marks need (§4.6: every mark has its word). Contiguous means the next
     * hour on the clock; a gap of one dry hour starts a new run.
     */
    fun wetRuns(hours: List<HourlyForecast>): List<Pair<LocalDateTime, LocalDateTime>> {
        val runs = mutableListOf<Pair<LocalDateTime, LocalDateTime>>()
        var lastWet: Instant? = null
        hours.forEach { hour ->
            if (!WmoCode.isPrecipitation(hour.condition.wmoCode)) return@forEach
            val previous = lastWet
            // By the instant, not the label: the night a zone falls back holds 02:00 twice.
            if (previous != null && !hour.at.isAfter(previous.plusSeconds(3_600))) {
                runs[runs.lastIndex] = runs.last().first to hour.time
            } else {
                runs += hour.time to hour.time
            }
            lastWet = hour.at
        }
        return runs
    }

    private fun HourlyForecast.ribbonHour(at: Float, wet: Boolean) =
        RibbonHour(at = at, cloudPct = cloudCoverPct, precipPct = precipChancePct ?: 0, wet = wet)
}
