package com.callbackdev.chiaro.ui.today

import com.callbackdev.chiaro.domain.sky.SolarDay
import com.callbackdev.chiaro.ui.components.LightPhase
import java.time.Instant
import java.time.LocalDateTime
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
}
