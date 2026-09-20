package com.callbackdev.chiaro.ui.sky

import com.callbackdev.chiaro.data.AppSettings
import com.callbackdev.chiaro.data.SkySubscription
import com.callbackdev.chiaro.domain.WeatherFreshness
import com.callbackdev.chiaro.domain.placeZone
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.MoonPhase
import com.callbackdev.chiaro.domain.model.WeatherReport
import com.callbackdev.chiaro.domain.sky.AstronomyEngine
import com.callbackdev.chiaro.domain.sky.LunarEclipse
import com.callbackdev.chiaro.domain.sky.MeteorShowerTable
import com.callbackdev.chiaro.domain.sky.MoonQuarterKind
import com.callbackdev.chiaro.domain.sky.SkyAlmanac
import com.callbackdev.chiaro.domain.sky.SolarEclipse
import com.callbackdev.chiaro.domain.sky.SkyJob
import com.callbackdev.chiaro.domain.sky.SkyJobCatalog
import com.callbackdev.chiaro.domain.sky.SkyJobKind
import com.callbackdev.chiaro.domain.sky.SkyLead
import com.callbackdev.chiaro.domain.sky.SkyNotScheduled
import com.callbackdev.chiaro.domain.sky.SkyOccurrence
import com.callbackdev.chiaro.domain.sky.SkyScheduler
import com.callbackdev.chiaro.domain.sky.SkySights
import com.callbackdev.chiaro.domain.sky.SkyVerdict
import com.callbackdev.chiaro.domain.sky.SkyVerdictEngine
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt

/**
 * The Sky screen's state (VISION §5.3). [Starting] draws the skeleton, [NoPlace] the
 * honest empty state; [Content] is tonight, the day's moments, and the calendar
 * ahead — all recomputed on read, because a verdict is a forecast, never a promise.
 */
sealed interface SkyUiState {
    data object Starting : SkyUiState
    data object NoPlace : SkyUiState

    data class Content(
        val placeName: String,
        /** The city's zone: every time on this screen renders in it, never the phone's. */
        val zone: ZoneId,
        val tonight: Tonight,
        val moments: List<Moment>,
        val events: List<UpcomingEvent>,
        val defaultLead: SkyLead,
        val notifyOnFail: Boolean,
        /**
         * The enabled subscriptions, by job id — the store's answer, for the catalog's
         * check marks. The sheet used to rebuild this set from the rows on screen, and a
         * subscribed job with no row (an eclipse search that finds nothing ahead) then
         * showed as unsubscribed and offered to be added again (review, 8 set 2026).
         */
        val subscribedIds: Set<String> = emptySet()
    ) : SkyUiState
}

/**
 * The hero: whether the dark window is worth planning around. [window] is null when
 * the sky has no dark window tonight, and [reason] says which of the three skies that
 * is: the one that never gets fully dark, the deep polar night that never gets light
 * (8 set 2026 — the card used to say "never gets fully dark" for both, which at the
 * pole in June is the reverse of the truth), or, since Fase 27, a moon that is up for
 * every minute of an otherwise perfectly dark night.
 *
 * [night] is the astronomical night the window sits inside, and it is carried for one
 * reason: the window is the night MINUS the moon, so without the night beside it the
 * card can print a two-hour window and no account of where the other five hours went.
 * [moonIlluminationPct] is the number that account rests on.
 */
data class Tonight(
    val window: SkyOccurrence.At?,
    val verdict: SkyVerdict?,
    val reason: SkyNotScheduled? = null,
    val night: SkyOccurrence.At? = null,
    val moonIlluminationPct: Int? = null,
    /**
     * The clearest run of hours inside the window, when it is not the whole of it
     * (Fase 28). The verdict is a mean over eight to ten hours, which is the right
     * number for one word and a poor answer to "yes, but when" — and the app has the
     * hours. Null on most nights: see [SkyVerdictEngine.clearStretch].
     */
    val clearStretch: ClosedRange<Instant>? = null
) {
    /** True when the moon is what opens the window late: the night started earlier. */
    val moonHeldTheStart: Boolean
        get() = window != null && night != null && window.start.isAfter(night.start)

    /** True when the moon is what closes it early. */
    val moonTookTheEnd: Boolean
        get() = window?.end != null && night?.end != null && window.end!!.isBefore(night.end!!)
}

/** When a moment's occurrence lands — the word the row prints before the time. */
enum class MomentTiming { NOW, TODAY, TOMORROW }

/**
 * One subscribed moment, resolved and judged: the one in front of the reader right
 * now ([SkyUpcoming]), which is today's while today's is still coming and tomorrow's
 * once it is over.
 *
 * [verdict] is null for the jobs that are geometry rather than a sight (solar noon,
 * the moon's phase): a verdict on something the clouds cannot spoil would be the
 * screen inventing a stake nobody has (the catalog's own `observable` rule).
 */
data class Moment(
    val job: SkyJob,
    val occurrence: SkyOccurrence,
    val verdict: SkyVerdict?,
    /** The lead in force: the moment's own, or the default it follows. */
    val lead: SkyLead,
    val followsDefault: Boolean,
    /** Happening, today's, or tomorrow's — said in words above the verdict. */
    val timing: MomentTiming,
    /** For the moon's day-moment: the phase is the value, not a verdict. */
    val moonPhase: MoonPhase? = null,
    val moonIlluminationPct: Int? = null,
    /**
     * Which way to turn, degrees from north, on the rows where that is the half a
     * reader cannot work out from a time (Fase 28). Null on most of them, deliberately
     * — see [SkySights.bearing].
     */
    val bearingDeg: Double? = null
)

/**
 * One entry of the calendar ahead: the next meteor peaks, the next full moon, the
 * next solstice or equinox. [verdict] where the forecast reaches that far; the
 * honest "too far out" lives in the verdict's own UNKNOWN note otherwise.
 *
 * [occurrence] is a plain [SkyOccurrence] and not its `At` since Fase 27: a `∅` on a
 * line the reader subscribed to is an answer — "the Perseids peak on a night that
 * never gets dark here" — and this list used to drop it on the floor while the
 * moments list beside it printed the same kind of fact with its reason.
 */
data class UpcomingEvent(
    val job: SkyJob,
    val occurrence: SkyOccurrence,
    val verdict: SkyVerdict?,
    /** Set when this entry is the moon reaching a named quarter. */
    val quarter: MoonQuarterKind? = null,
    /** Present when the job is a subscribed line, so the row can carry its bell. */
    val lead: SkyLead? = null,
    val followsDefault: Boolean = true,
    /**
     * The eclipse behind an eclipse row. Carried as the domain's own value, not as a
     * sentence: this builder is pure and has no `Resources`, and the words for it
     * live in `SkyText` with every other word on the screen.
     */
    val lunarEclipse: LunarEclipse? = null,
    val solarEclipse: SolarEclipse? = null,
    /**
     * The other events this one shares its instant with (Fase 27). The delta
     * Aquariids and the alpha Capricornids are both pinned to solar longitude 127.0°
     * — the IMO list really does put them there — so they resolve to the same
     * instant, the same night and the same icon, and the screen printed them as two
     * rows a reader could only read as a bug. One night, one row, both names.
     */
    val sharesNightWith: List<SkyJob> = emptyList(),
    /**
     * Whether the row has to print the year (Fase 27). The list used to format every
     * date as `d MMMM`, and the aperiodic rows reach years out — `EclipseEngine`
     * searches six of them for a solar eclipse — so "2 agosto" was a row that left out
     * the one fact a reader needed to know whether to care.
     *
     * Decided here rather than in the row because the clock lives here: the builder is
     * pure and takes `now`, a composable would have to be handed one.
     */
    val showYear: Boolean = false,
    /** As [Moment.bearingDeg]: where to look, where that is worth saying. */
    val bearingDeg: Double? = null,
    /**
     * How close the pair gets, degrees — the whole content of a conjunction row.
     *
     * Measured at **this row's own instant** rather than fetched from a second search,
     * and that is not a micro-optimisation: the row's occurrence comes from
     * [SkyUpcoming], which rolls past an event that is over, while a fresh "next
     * conjunction from today" answers about the one that has just passed. Two
     * questions, two answers, one row — the defect this screen has now been bitten by
     * twice. From the instant there is only one answer.
     */
    val conjunctionSeparationDeg: Double? = null
) {
    val at: SkyOccurrence.At? get() = occurrence as? SkyOccurrence.At
}

/**
 * The instant a window is best asked about: its middle, or the instant itself. The
 * bearing of a two-hour window taken at its start is not the bearing of the window.
 */
private val SkyOccurrence.At.middle: Instant
    get() = end?.let { start.plus(Duration.between(start, it).dividedBy(2)) } ?: start

/**
 * city + cached report + subscriptions + settings + now → the whole screen. Pure on
 * purpose, like [com.callbackdev.chiaro.ui.today.TodayStateBuilder] before it: the
 * clock is a parameter, so every claim on this screen is testable without a device.
 */
object SkyStateBuilder {

    /** How many rows the calendar ahead shows: enough to always reach past the
     * forecast's horizon, few enough that the next real event is not buried. */
    private const val EVENT_ROWS = 6

    /**
     * And how much of the shared calendar survives a reader with many subscriptions.
     * The subscribed lines are never capped — they were asked for — but a screen that
     * dropped the next meteor peak entirely because somebody follows seven eclipse-ish
     * things would have traded one hole for another.
     */
    private const val MIN_CALENDAR_ROWS = 3

    fun build(
        city: City,
        report: WeatherReport?,
        subscriptions: List<SkySubscription>,
        settings: AppSettings,
        now: Instant
    ): SkyUiState.Content {
        val zone = placeZone(report, city)
        val staleAfter = WeatherFreshness.staleAfter(settings.updateFrequencyMin)
        val dataAge = report?.let { Duration.between(it.systemInfo.lastSync, now) }

        fun judge(job: SkyJob, at: SkyOccurrence.At): SkyVerdict? {
            if (!job.observable) return null
            return SkyVerdictEngine.evaluate(
                job = job,
                start = at.start,
                end = at.end,
                hours = report?.hourly.orEmpty(),
                zone = zone,
                coordinates = city.coordinates,
                dataAge = dataAge,
                staleAfter = staleAfter
            )
        }

        return SkyUiState.Content(
            placeName = city.name,
            zone = zone,
            tonight = tonight(city, zone, now, ::judge) { at ->
                SkyVerdictEngine.clearStretch(at.start, at.end, report?.hourly.orEmpty(), zone)
            },
            moments = moments(subscriptions, settings, city, zone, now, ::judge),
            events = events(subscriptions, settings, city, zone, now, ::judge),
            defaultLead = SkyLead.ofMinutes(settings.skyNotifyDefaultMin),
            notifyOnFail = settings.skyNotifyOnFail,
            subscribedIds = subscriptions.filter { it.enabled }.map { it.jobId }.toSet()
        )
    }

    /**
     * The night in progress, or the one ahead: yesterday's window while its dawn is
     * still coming (at 03:00 "tonight" means the sky outside, not the next dusk),
     * today's otherwise. That is [SkyUpcoming]'s rule for every moment, and the card
     * reads it from there so the hero and a subscribed dark-window row cannot
     * disagree about which night they are talking about.
     */
    private fun tonight(
        city: City,
        zone: ZoneId,
        now: Instant,
        judge: (SkyJob, SkyOccurrence.At) -> SkyVerdict?,
        clearStretch: (SkyOccurrence.At) -> ClosedRange<Instant>?
    ): Tonight {
        val job = SkyJobCatalog.DarknessWindow
        val upcoming = SkyUpcoming.of(job, now, zone, city.coordinates)
        // The night around the window, asked for the same local day the window came
        // from: since Fase 27 the window is the night minus the moon, and the card
        // cannot explain a short window without the night it was cut out of.
        val night = SkyScheduler.darkNight(upcoming.date, zone, city.coordinates).night
            as? SkyOccurrence.At
        val moonPct = night?.let {
            val middle = it.start.plus(
                Duration.between(it.start, it.end ?: it.start).dividedBy(2)
            )
            (AstronomyEngine.moonIllumination(middle).illuminatedFraction * 100).roundToInt()
        }
        val at = upcoming.at ?: return Tonight(
            window = null,
            verdict = null,
            reason = (upcoming.occurrence as? SkyOccurrence.None)?.reason,
            night = night,
            moonIlluminationPct = moonPct
        )
        return Tonight(
            window = at,
            verdict = judge(job, at),
            night = night,
            moonIlluminationPct = moonPct,
            clearStretch = clearStretch(at)
        )
    }

    /**
     * The subscribed daily moments in front of the reader, in the order they happen.
     *
     * Each one is [SkyUpcoming]'s answer rather than today's calendar row: a moment
     * that is over is tomorrow's, and the row says "Tomorrow" instead of greying out
     * a sunrise nobody can attend any more. That is also what the Sky widget shows,
     * from the same rule — the two surfaces used to print two different sunrises
     * (committente, 3 set). The days the sky skips one (`∅`) keep their row with the
     * reason, sorted after the scheduled: a fact about the sky, not a gap in the list.
     */
    private fun moments(
        subscriptions: List<SkySubscription>,
        settings: AppSettings,
        city: City,
        zone: ZoneId,
        now: Instant,
        judge: (SkyJob, SkyOccurrence.At) -> SkyVerdict?
    ): List<Moment> {
        val today = now.atZone(zone).toLocalDate()
        return subscriptions
            .filter { it.enabled }
            .mapNotNull { sub -> SkyJobCatalog.byId(sub.jobId)?.let { sub to it } }
            .filter { (_, job) -> job.kind == SkyJobKind.DAILY }
            .map { (sub, job) ->
                val upcoming = SkyUpcoming.of(job, now, zone, city.coordinates)
                val at = upcoming.at
                val isMoonDay = job.id == SkyJobCatalog.MoonToday.id
                Moment(
                    job = job,
                    occurrence = upcoming.occurrence,
                    verdict = at?.let { judge(job, it) },
                    lead = SkyLead.ofMinutes(sub.notifyLeadMinutes ?: settings.skyNotifyDefaultMin),
                    followsDefault = sub.notifyLeadMinutes == null,
                    timing = when {
                        upcoming.inProgress -> MomentTiming.NOW
                        upcoming.date == today -> MomentTiming.TODAY
                        else -> MomentTiming.TOMORROW
                    },
                    moonPhase = if (isMoonDay && at != null) MoonPhase.at(at.start) else null,
                    moonIlluminationPct = if (isMoonDay && at != null) {
                        (AstronomyEngine.moonIllumination(at.start).illuminatedFraction * 100)
                            .roundToInt()
                    } else null,
                    // Measured at the middle of a window rather than at its start: the
                    // sun moves 15° an hour, so a golden hour's bearing taken at its
                    // opening points a good deal north of where it ends up.
                    bearingDeg = at?.let { SkySights.bearing(job, it.middle, city.coordinates) }
                )
            }
            .sortedWith(
                compareBy(
                    { it.occurrence !is SkyOccurrence.At },
                    { (it.occurrence as? SkyOccurrence.At)?.start },
                    { SkyJobCatalog.orderOf(it.job) }
                )
            )
    }

    /**
     * The calendar ahead (VISION §5.3), in two tiers — and the tiers are the fix for
     * a hole this screen had from the day it shipped (Fase 27).
     *
     * **Tier one is the reader's own lines**: every subscribed job that is not daily,
     * always, with no competition for the slot. Before this the list was six rows
     * chosen by date across the whole catalog, so a subscribed line only appeared if
     * it happened to be among the six nearest events in the world. Measured at Milan
     * on 20 set 2026 the six were an equinox, a full moon and four meteor showers,
     * covering seven weeks — and a reader who had subscribed to `eclipse.solar` (next
     * one here: 2 ago 2027) got a check mark in the catalog, a reminder that fired,
     * and no row anywhere. The bell and the screen disagreed about what the reader
     * was following, which is the very defect [SkyUpcoming] was written to close.
     *
     * A `∅` stays in this tier with its reason, exactly as it does in [moments]: the
     * Perseids at Stockholm peak on a night with no astronomical darkness, and
     * "the sky never gets fully dark" is the answer, not a row to delete.
     *
     * **Tier two is the calendar everyone gets**: the nearest annual events that are
     * not already tier one, so a reader who has subscribed to nothing still opens the
     * screen on the next meteor peak and the next solstice. Only dated rows here — an
     * unsubscribed `∅` is somebody else's fact, and printing thirteen of them at a
     * white-night latitude would bury the list it is meant to fill.
     *
     * The **aperiodic** half stays subscription-bound, as it always was: an eclipse
     * search walks years of new moons, and running two of them for a reader who never
     * asked would be a battery cost with nothing on the screen to show for it.
     */
    private fun events(
        subscriptions: List<SkySubscription>,
        settings: AppSettings,
        city: City,
        zone: ZoneId,
        now: Instant,
        judge: (SkyJob, SkyOccurrence.At) -> SkyVerdict?
    ): List<UpcomingEvent> {
        val subscribed = subscriptions.filter { it.enabled }.associateBy { it.jobId }
        val today = now.atZone(zone).toLocalDate()

        fun entry(job: SkyJob, occurrence: SkyOccurrence): UpcomingEvent {
            val sub = subscribed[job.id]
            val at = occurrence as? SkyOccurrence.At
            return UpcomingEvent(
                job = job,
                occurrence = occurrence,
                showYear = at != null &&
                    at.start.atZone(zone).toLocalDate().year != today.year,
                verdict = at?.let { judge(job, it) },
                lead = sub?.let {
                    SkyLead.ofMinutes(it.notifyLeadMinutes ?: settings.skyNotifyDefaultMin)
                },
                followsDefault = sub?.notifyLeadMinutes == null,
                lunarEclipse = if (at != null && job.id == SkyJobCatalog.LunarEclipse.id) {
                    SkyAlmanac.nextLunarEclipse(today, zone, city.coordinates)?.eclipse
                } else {
                    null
                },
                solarEclipse = if (at != null && job.id == SkyJobCatalog.SolarEclipse.id) {
                    SkyAlmanac.nextSolarEclipse(today, zone, city.coordinates)
                } else {
                    null
                },
                bearingDeg = at?.let { SkySights.bearing(job, it.middle, city.coordinates) },
                conjunctionSeparationDeg = if (at != null && job.id in SkyScheduler.ConjunctionJobs) {
                    val (first, second) = SkyScheduler.ConjunctionJobs.getValue(job.id)
                    AstronomyEngine.separation(first, second, at.start)
                } else {
                    null
                }
            )
        }

        /** The occurrence this list should show for [job]: annual walks, polling steps. */
        fun nextOf(job: SkyJob): SkyOccurrence? = when (job.kind) {
            SkyJobKind.ANNUAL ->
                SkyScheduler.next(job, now, zone, city.coordinates, limit = 1).firstOrNull()
            SkyJobKind.POLLING -> SkyUpcoming.of(job, now, zone, city.coordinates).occurrence
            // The daily ones are the moments list; they are not events ahead.
            SkyJobKind.DAILY -> null
        }

        val mine = subscriptions
            .filter { it.enabled }
            .mapNotNull { SkyJobCatalog.byId(it.jobId) }
            .filter { it.kind != SkyJobKind.DAILY }
            .mapNotNull { job -> nextOf(job)?.let { entry(job, it) } }
        val mineIds = mine.map { it.job.id }.toSet()

        // The next full moon, for everyone. It is the one moment of the moon's cycle
        // people ask about by name, so the calendar carries it whether or not it is a
        // subscribed line — unless it IS one, and then the subscribed row wins,
        // because that one has a bell.
        val fullMoon = if (SkyJobCatalog.MoonFull.id in mineIds) {
            null
        } else {
            nextFullMoon(now)?.let { at ->
                UpcomingEvent(
                    job = SkyJobCatalog.MoonFull,
                    occurrence = SkyOccurrence.At(SkyJobCatalog.MoonFull, at),
                    verdict = null, // a phase is a fact about the day, not a sight to judge
                    quarter = MoonQuarterKind.FULL_MOON,
                    showYear = at.atZone(zone).toLocalDate().year != today.year
                )
            }
        }

        // The shared calendar fills what is left, the full-moon row included in the
        // count: the cap is on the screen, not on one of its two sources.
        val everyones = SkyJobCatalog.all
            .filter { it.kind == SkyJobKind.ANNUAL && it.id !in mineIds }
            .mapNotNull { job ->
                SkyScheduler.next(job, now, zone, city.coordinates, limit = 1)
                    .filterIsInstance<SkyOccurrence.At>()
                    .firstOrNull()
                    ?.let { entry(job, it) }
            }
            .sortedBy { it.at!!.start }
            .take(
                (EVENT_ROWS - mine.size - (if (fullMoon != null) 1 else 0))
                    .coerceAtLeast(MIN_CALENDAR_ROWS)
            )

        val dated = (mine + everyones + listOfNotNull(fullMoon)).filter { it.at != null }
        // Only tier one can be undated: the shared calendar took its `At`s only.
        val undated = mine.filter { it.at == null }
        return collapseSameInstant(dated).sortedBy { it.at!!.start } +
            undated.sortedBy { SkyJobCatalog.orderOf(it.job) }
    }

    /**
     * One instant, one row (Fase 27).
     *
     * Two showers really can peak on the same night — the delta Aquariids and the
     * alpha Capricornids share solar longitude 127.0° — and two rows with the same
     * date, the same window and the same icon read as a defect however true they are.
     * Those merge and keep both names. Everything else that collides is the same
     * event asked for twice: `moon.phase` resolving to a full moon the reader also
     * follows by name, or the year's closest full moon landing on the plain full-moon
     * row. There the more specific line wins, and a line with a bell beats one without
     * — dropping the row the reader subscribed to would take its bell off the screen.
     */
    private fun collapseSameInstant(events: List<UpcomingEvent>): List<UpcomingEvent> =
        events.groupBy { it.at!!.start }.values.map { group ->
            when {
                group.size == 1 -> group.single()
                group.all { MeteorShowerTable.showerOf(it.job.id) != null } -> {
                    // The row that survives is the one with the bell, not whichever
                    // came first: when only one of the pair is subscribed, keeping the
                    // other would take the reader's own reminder off the screen.
                    val kept = group.maxByOrNull { if (it.lead != null) 1 else 0 }!!
                    kept.copy(sharesNightWith = group.filter { it !== kept }.map { it.job })
                }
                else -> group.maxWith(
                    compareBy({ if (it.lead != null) 1 else 0 }, ::specificity)
                )
            }
        }

    /**
     * How much a row says, for [collapseSameInstant]. `moon.phase` names no phase at
     * all ("the next quarter, whichever it is"), the appended full-moon row has a name
     * and no bell, and anything else is a catalog line the reader asked for by name —
     * with the year's closest full moon above the plain one, because "the biggest full
     * moon of the year" is the whole of the other row plus a reason.
     */
    private fun specificity(event: UpcomingEvent): Int = when {
        event.job.id == SkyJobCatalog.MoonPhase.id -> 0
        event.quarter != null -> 1
        event.job.id == SkyJobCatalog.MoonFull.id -> 2
        else -> 3
    }

    /** Walks the quarter series to the next full moon: at most four steps away. */
    private fun nextFullMoon(now: Instant): Instant? {
        var at = now
        repeat(4) {
            val quarter = AstronomyEngine.nextMoonQuarter(at)
            if (quarter.kind == MoonQuarterKind.FULL_MOON) return quarter.at
            at = quarter.at
        }
        return null
    }
}
