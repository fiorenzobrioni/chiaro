package com.callbackdev.chiaro.ui.sky

import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.sky.SkyJob
import com.callbackdev.chiaro.domain.sky.SkyJobCatalog
import com.callbackdev.chiaro.domain.sky.SkyJobKind
import com.callbackdev.chiaro.domain.sky.SkyJobShape
import com.callbackdev.chiaro.domain.sky.SkyOccurrence
import com.callbackdev.chiaro.domain.sky.SkyScheduler
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Which occurrence of a subscribed job is the one in front of the reader.
 *
 * Written after a device review (committente, 3 set): at 21:19 the Sky screen listed
 * this morning's sunrise, greyed out and marked "Passed", with a "not sure yet"
 * verdict the elapsed hours could no longer support — while the Sky widget on the
 * same home screen was already showing TOMORROW's sunrise, judged clear. Two answers
 * to one question, because the window each surface looked through was never written
 * down anywhere. It is written down here, once, and the screen and the widget both
 * read it.
 *
 * The rule: a moment stops being today's business when it is over. The day's agenda
 * therefore rolls — the sunrise you are waiting for at nine in the evening is
 * tomorrow's, and the row says so — with three exceptions that are facts, not
 * conveniences:
 *
 * - **A window in progress wins.** The dark window opens at dusk and closes at dawn:
 *   at three in the morning it has not passed, it is happening (the rule the Tonight
 *   card already lived by alone).
 * - **A `∅` stays today's.** "The moon skips it today" is an answer about today, and
 *   rolling past it would turn a fact about the sky into a gap in the list — the
 *   [SkyScheduler] doctrine, kept.
 * - **The moon's day-moment never rolls.** It is a statement ABOUT today, not an
 *   appointment: printing "tomorrow" against a row named "Today's moon" would be
 *   nonsense, and "Passed" at nine in the evening already was.
 */
object SkyUpcoming {

    /** An occurrence with the two facts a row needs to introduce it honestly. */
    data class Resolved(
        val occurrence: SkyOccurrence,
        /** The local day the occurrence belongs to. */
        val date: LocalDate,
        /** A window that has already opened and has not closed. */
        val inProgress: Boolean
    ) {
        val at: SkyOccurrence.At? get() = occurrence as? SkyOccurrence.At
    }

    /** The occurrence of [job] a reader at [now] is actually waiting for. */
    fun of(job: SkyJob, now: Instant, zone: ZoneId, coords: Coordinates): Resolved {
        val today = now.atZone(zone).toLocalDate()

        if (job.id == SkyJobCatalog.MoonToday.id) {
            return Resolved(
                occurrence = SkyScheduler.resolve(job, today, zone, coords),
                date = today,
                inProgress = false
            )
        }

        // A window that opened yesterday and has not closed is the one you are in.
        // Only a daily window can cross midnight into now; an annual one (a shower's
        // peak) is caught by today's own resolution below.
        if (job.kind == SkyJobKind.DAILY && job.shape == SkyJobShape.RANGE) {
            val yesterday = SkyScheduler.resolve(job, today.minusDays(1), zone, coords)
            if (yesterday is SkyOccurrence.At && yesterday.covers(now)) {
                return Resolved(yesterday, today.minusDays(1), inProgress = true)
            }
        }

        val todays = SkyScheduler.resolve(job, today, zone, coords)
        // Today's answer stands while it is still ahead, while it is still running,
        // and whenever it is a `∅`.
        if (todays !is SkyOccurrence.At || (todays.end ?: todays.start).isAfter(now)) {
            return Resolved(todays, today, inProgress = todays.isCovering(now))
        }

        // Today's is over: the next one. `next` reports a `∅` day rather than
        // skipping it, so a polar tomorrow arrives with its reason attached.
        val next = SkyScheduler.next(job, now, zone, coords, limit = 1).firstOrNull()
            ?: return Resolved(todays, today, inProgress = false)
        val date = (next as? SkyOccurrence.At)?.start?.atZone(zone)?.toLocalDate()
            ?: today.plusDays(1)
        return Resolved(next, date, inProgress = false)
    }

    /**
     * Every one of [jobs] the sky will actually deliver, soonest first — the Sky
     * widget's whole list once it knows how tall it is (Fase 8b), and by construction
     * the scheduled part of the screen's own list in the screen's own order.
     *
     * The moon's day-moment is not a candidate: it is pinned to today by [of] and
     * never fires, so leaving it in would make it win every comparison forever.
     */
    fun allAt(
        jobs: List<SkyJob>,
        now: Instant,
        zone: ZoneId,
        coords: Coordinates
    ): List<Resolved> = jobs
        .filterNot { it.id == SkyJobCatalog.MoonToday.id }
        .map { of(it, now, zone, coords) }
        .filter { it.at != null }
        .sortedBy { it.at!!.start }

    /**
     * The first of [jobs] the sky will actually deliver — the head of [allAt], named
     * because it is also the one moment a one-cell widget has room for.
     */
    fun firstAt(
        jobs: List<SkyJob>,
        now: Instant,
        zone: ZoneId,
        coords: Coordinates
    ): Resolved? = allAt(jobs, now, zone, coords).firstOrNull()

    private fun SkyOccurrence.isCovering(at: Instant): Boolean =
        this is SkyOccurrence.At && covers(at)

    /** True for a window that has opened and not closed; an instant covers nothing. */
    private fun SkyOccurrence.At.covers(at: Instant): Boolean {
        val closes = end ?: return false
        return !start.isAfter(at) && closes.isAfter(at)
    }
}
