package com.callbackdev.chiaro.domain.sky

import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.model.MoonPhase
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Which moon a day is described by (review of the Sky screen, point 6, 14 ott 2026).
 *
 * The same screen printed three moons for one evening: the tonight card measured the
 * moon at the middle of the night (99%), «today's moon» at local noon (98%, and named
 * «full» a day before the full moon), and the calendar the full moon's instant
 * (tomorrow). Each was right about its own instant, and together they read as the app
 * contradicting itself. So one instant answers for all of them: **the middle of the
 * date's astronomical night**, which is the moon a person will actually look up at, and
 * the one the card has always used. Where the date has no astronomical night (the white
 * nights, the polar day) the local midnight that closes it stands in.
 */
object MoonOfTheNight {

    fun instant(date: LocalDate, zone: ZoneId, coords: Coordinates): Instant {
        val night = SkyScheduler.darkNight(date, zone, coords).night as? SkyOccurrence.At
        val end = night?.end
        return if (night != null && end != null) {
            night.start.plus(Duration.between(night.start, end).dividedBy(2))
        } else {
            date.plusDays(1).atStartOfDay(zone).toInstant()
        }
    }

    /**
     * The exact instant of the quarter [phase] is named after, when it is one of the four
     * (new, first quarter, full, last quarter), nearest to [at]; null for the four phases
     * in between. A phase name covers about a day and three quarters either side of its
     * quarter ([MoonPhase.at]), which is why «full» needs its day and hour beside it: the
     * row that said «Luna piena» the evening before the calendar's full moon was the name
     * of a range read as the name of an instant.
     */
    fun quarterOf(phase: MoonPhase, at: Instant): MoonQuarter? {
        val kind = when (phase) {
            MoonPhase.NEW_MOON -> MoonQuarterKind.NEW_MOON
            MoonPhase.FIRST_QUARTER -> MoonQuarterKind.FIRST_QUARTER
            MoonPhase.FULL_MOON -> MoonQuarterKind.FULL_MOON
            MoonPhase.LAST_QUARTER -> MoonQuarterKind.LAST_QUARTER
            else -> return null
        }
        // The quarter that names this phase is within ±1.85 days of it; searching from
        // three days back finds it and never the one a month away.
        return AstronomyEngine.nextMoonQuarter(at.minus(SearchBack), kind)
    }

    private val SearchBack: Duration = Duration.ofDays(3)
}
