package com.callbackdev.chiaro.domain.rules

import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.sky.AstronomyEngine
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.serialization.Serializable

/**
 * When a rule may speak (26 set 2026, committente): «Bici» fired at 21:47, true about the
 * weather and useless about the hour. The conditions say whether the weather allows it;
 * this says whether the reader would do it now. Three answers, and the first is what every
 * rule saved before this had:
 *
 * - [ALWAYS] — any hour, the old behaviour;
 * - [DAYLIGHT] — while the sun is above the horizon at the place, computed on the phone
 *   by [AstronomyEngine]: it follows the season by itself (16:40 in December, 21:15 in
 *   June in Milan), where a fixed «7-21» would send a cyclist out in the dark all winter;
 * - [HOURS] — one or two bands of the reader's own ([MaxBands]), e.g. 7-9 and 17-19:30.
 */
@Serializable
enum class RuleWindowKind { ALWAYS, DAYLIGHT, HOURS }

/**
 * One band of the day in minutes from midnight on the place's clock, [from] inclusive and
 * [to] exclusive. A band whose [to] comes before its [from] runs across midnight
 * (22:00-02:00). [from] == [to] is an empty band: the editor never writes one.
 */
@Serializable
data class RuleBand(val from: Int, val to: Int) {
    val fromTime: LocalTime get() = LocalTime.of(from / 60, from % 60)
    val toTime: LocalTime get() = LocalTime.of(to / 60, to % 60)
    val wraps: Boolean get() = to < from

    companion object {
        fun of(from: LocalTime, to: LocalTime) =
            RuleBand(from.hour * 60 + from.minute, to.hour * 60 + to.minute)
    }
}

/**
 * A rule's hours. The bands are kept while [kind] is not [RuleWindowKind.HOURS], so a
 * reader who tries «Con la luce» and comes back finds their bands where they left them.
 */
@Serializable
data class RuleWindow(
    val kind: RuleWindowKind = RuleWindowKind.ALWAYS,
    val bands: List<RuleBand> = emptyList()
) {
    val always: Boolean get() = kind == RuleWindowKind.ALWAYS

    companion object {
        val Always = RuleWindow()
        val Daylight = RuleWindow(RuleWindowKind.DAYLIGHT)

        /** What «Fasce orarie» starts from: the waking day, to be narrowed. */
        val FirstBand = RuleBand(7 * 60, 21 * 60)

        /** Two: a commute is a morning and an evening, and a third band is a schedule. */
        const val MaxBands = 2
    }
}

/**
 * The window as the engine reads it: open or closed at an instant, and — while open —
 * which occurrence of it this is, the key of the rule's one voice per band.
 *
 * Pure like [RuleEngine]: the place's zone and coordinates come in, nothing reads a clock.
 */
object RuleWindows {

    /**
     * The occurrence of [window] that contains [now] (a time on the place's clock), or
     * null when the window is closed. [ALWAYS][RuleWindowKind.ALWAYS] answers an empty
     * key: it is open, and the engine keeps its old buckets for it.
     *
     * The key names the day the occurrence began and the band itself, so a band that
     * wraps midnight is one occurrence on both sides of it, and a band the reader edits
     * is a new one.
     */
    fun occurrence(
        window: RuleWindow,
        now: LocalDateTime,
        zone: ZoneId,
        coords: Coordinates
    ): String? = when (window.kind) {
        RuleWindowKind.ALWAYS -> ""
        RuleWindowKind.DAYLIGHT ->
            // The sun above the horizon at this instant, at the almanac's sunrise altitude:
            // no sunrise to look up, so the polar day is simply open and the polar night
            // closed, with no special case for either.
            if (sunUp(now, zone, coords)) "${now.toLocalDate()}:LIGHT" else null
        RuleWindowKind.HOURS -> {
            val bands = window.bands.filter { it.from != it.to }
            if (bands.isEmpty()) {
                // Never written by the editor; a hand-edited or damaged file must not
                // silence a rule for ever without a word, so it reads as any hour.
                ""
            } else {
                bands.firstNotNullOfOrNull { band -> bandOccurrence(band, now) }
            }
        }
    }

    fun isOpen(window: RuleWindow, now: LocalDateTime, zone: ZoneId, coords: Coordinates): Boolean =
        occurrence(window, now, zone, coords) != null

    /**
     * When a closed window next opens, for «Prova adesso»: the next band's start, or the
     * next sunrise within [DaylightLookAheadDays]. Null when it is open now, or when there
     * is no sunrise that close (the polar night): the screen then says «not now» without
     * inventing an hour.
     */
    fun nextOpening(
        window: RuleWindow,
        now: LocalDateTime,
        zone: ZoneId,
        coords: Coordinates
    ): LocalDateTime? {
        if (isOpen(window, now, zone, coords)) return null
        return when (window.kind) {
            RuleWindowKind.ALWAYS -> null
            RuleWindowKind.DAYLIGHT -> (0L..DaylightLookAheadDays).asSequence()
                .mapNotNull { days ->
                    val date = now.toLocalDate().plusDays(days)
                    AstronomyEngine.sunCrossing(
                        date, zone, coords, AstronomyEngine.SUNRISE_ALTITUDE, rising = true
                    )?.atZone(zone)?.toLocalDateTime()
                }
                .firstOrNull { it > now }
            RuleWindowKind.HOURS -> window.bands
                .filter { it.from != it.to }
                .flatMap { band ->
                    listOf(0L, 1L).map { now.toLocalDate().plusDays(it).atTime(band.fromTime) }
                }
                .filter { it > now }
                .minOrNull()
        }
    }

    private fun sunUp(now: LocalDateTime, zone: ZoneId, coords: Coordinates): Boolean =
        AstronomyEngine.sunAltitude(now.atZone(zone).toInstant(), coords) >
            AstronomyEngine.SUNRISE_ALTITUDE

    private fun bandOccurrence(band: RuleBand, now: LocalDateTime): String? {
        val minute = now.hour * 60 + now.minute
        val started: LocalDate? = when {
            !band.wraps -> now.toLocalDate().takeIf { minute >= band.from && minute < band.to }
            minute >= band.from -> now.toLocalDate()
            minute < band.to -> now.toLocalDate().minusDays(1)
            else -> null
        }
        return started?.let { "$it:H${band.from}-${band.to}" }
    }

    /** A week: past it, a sunrise is not an hour anybody plans a ride around. */
    private const val DaylightLookAheadDays = 7L
}
