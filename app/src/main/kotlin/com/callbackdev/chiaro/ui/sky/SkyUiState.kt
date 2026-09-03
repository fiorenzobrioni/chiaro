package com.callbackdev.chiaro.ui.sky

import com.callbackdev.chiaro.data.AppSettings
import com.callbackdev.chiaro.data.SkySubscription
import com.callbackdev.chiaro.domain.WeatherFreshness
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.MoonPhase
import com.callbackdev.chiaro.domain.model.WeatherReport
import com.callbackdev.chiaro.domain.sky.AstronomyEngine
import com.callbackdev.chiaro.domain.sky.MoonQuarterKind
import com.callbackdev.chiaro.domain.sky.SkyJob
import com.callbackdev.chiaro.domain.sky.SkyJobCatalog
import com.callbackdev.chiaro.domain.sky.SkyJobKind
import com.callbackdev.chiaro.domain.sky.SkyLead
import com.callbackdev.chiaro.domain.sky.SkyOccurrence
import com.callbackdev.chiaro.domain.sky.SkyScheduler
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
        val notifyOnFail: Boolean
    ) : SkyUiState
}

/**
 * The hero: whether the dark window is worth planning around. [window] is null when
 * the sky never gets fully dark tonight — a fact about the latitude, stated as such.
 */
data class Tonight(
    val window: SkyOccurrence.At?,
    val verdict: SkyVerdict?
)

/**
 * One subscribed moment of today, resolved and judged.
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
    /** Fully over at build time: rendered quieter, never dropped — it was today's. */
    val past: Boolean,
    /** For the moon's day-moment: the phase is the value, not a verdict. */
    val moonPhase: MoonPhase? = null,
    val moonIlluminationPct: Int? = null
)

/**
 * One entry of the calendar ahead: the next meteor peaks, the next full moon, the
 * next solstice or equinox. [verdict] where the forecast reaches that far; the
 * honest "too far out" lives in the verdict's own UNKNOWN note otherwise.
 */
data class UpcomingEvent(
    val job: SkyJob,
    val occurrence: SkyOccurrence.At,
    val verdict: SkyVerdict?,
    /** Set when this entry is the moon reaching a named quarter. */
    val quarter: MoonQuarterKind? = null,
    /** Present when the job is a subscribed line, so the row can carry its bell. */
    val lead: SkyLead? = null,
    val followsDefault: Boolean = true
)

/**
 * city + cached report + subscriptions + settings + now → the whole screen. Pure on
 * purpose, like [com.callbackdev.chiaro.ui.today.TodayStateBuilder] before it: the
 * clock is a parameter, so every claim on this screen is testable without a device.
 */
object SkyStateBuilder {

    /** How many rows the calendar ahead shows: enough to always reach past the
     * forecast's horizon, few enough that the next real event is not buried. */
    private const val EVENT_ROWS = 6

    fun build(
        city: City,
        report: WeatherReport?,
        subscriptions: List<SkySubscription>,
        settings: AppSettings,
        now: Instant
    ): SkyUiState.Content {
        val zone = city.timezone?.let { runCatching { ZoneId.of(it) }.getOrNull() }
            ?: ZoneId.systemDefault()
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
            tonight = tonight(city, zone, now, ::judge),
            moments = moments(subscriptions, settings, city, zone, now, ::judge),
            events = events(subscriptions, settings, city, zone, now, ::judge),
            defaultLead = SkyLead.ofMinutes(settings.skyNotifyDefaultMin),
            notifyOnFail = settings.skyNotifyOnFail
        )
    }

    /**
     * The night in progress, or the one ahead: yesterday's window while its dawn is
     * still coming (at 03:00 "tonight" means the sky outside, not the next dusk),
     * today's otherwise.
     */
    private fun tonight(
        city: City,
        zone: ZoneId,
        now: Instant,
        judge: (SkyJob, SkyOccurrence.At) -> SkyVerdict?
    ): Tonight {
        val job = SkyJobCatalog.DarknessWindow
        val today = now.atZone(zone).toLocalDate()
        val current = SkyScheduler.resolve(job, today.minusDays(1), zone, city.coordinates)
        val occurrence = when {
            current is SkyOccurrence.At && current.end?.isAfter(now) == true -> current
            else -> SkyScheduler.resolve(job, today, zone, city.coordinates)
        }
        val at = occurrence as? SkyOccurrence.At ?: return Tonight(window = null, verdict = null)
        return Tonight(window = at, verdict = judge(job, at))
    }

    /**
     * Today's subscribed daily moments, in the order they happen; the days the sky
     * skips one (`∅`) keep their row with the reason, sorted after the scheduled —
     * a fact about the sky, not a gap in the list.
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
                val occurrence = SkyScheduler.resolve(job, today, zone, city.coordinates)
                val at = occurrence as? SkyOccurrence.At
                val isMoonDay = job.id == SkyJobCatalog.MoonToday.id
                Moment(
                    job = job,
                    occurrence = occurrence,
                    verdict = at?.let { judge(job, it) },
                    lead = SkyLead.ofMinutes(sub.notifyLeadMinutes ?: settings.skyNotifyDefaultMin),
                    followsDefault = sub.notifyLeadMinutes == null,
                    past = at != null && (at.end ?: at.start).isBefore(now),
                    moonPhase = if (isMoonDay && at != null) MoonPhase.at(at.start) else null,
                    moonIlluminationPct = if (isMoonDay && at != null) {
                        (AstronomyEngine.moonIllumination(at.start).illuminatedFraction * 100)
                            .roundToInt()
                    } else null
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
     * The calendar ahead (VISION §5.3): every annual job's next occurrence plus the
     * next full moon, nearest first. Catalog-wide, not subscription-bound — a Perseid
     * peak is coming whether or not it is a line of yours; the bell rides only the
     * subscribed rows.
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
        val annual = SkyJobCatalog.all
            .filter { it.kind == SkyJobKind.ANNUAL }
            .mapNotNull { job ->
                SkyScheduler.next(job, now, zone, city.coordinates, limit = 1)
                    .filterIsInstance<SkyOccurrence.At>()
                    .firstOrNull()
                    ?.let { at ->
                        val sub = subscribed[job.id]
                        UpcomingEvent(
                            job = job,
                            occurrence = at,
                            verdict = judge(job, at),
                            lead = sub?.let {
                                SkyLead.ofMinutes(it.notifyLeadMinutes ?: settings.skyNotifyDefaultMin)
                            },
                            followsDefault = sub?.notifyLeadMinutes == null
                        )
                    }
            }
        val fullMoon = nextFullMoon(now)?.let { at ->
            UpcomingEvent(
                job = SkyJobCatalog.MoonPhase,
                occurrence = SkyOccurrence.At(SkyJobCatalog.MoonPhase, at),
                verdict = null, // a phase is a fact about the day, not a sight to judge
                quarter = MoonQuarterKind.FULL_MOON
            )
        }
        return (annual + listOfNotNull(fullMoon))
            .sortedBy { it.occurrence.start }
            .take(EVENT_ROWS)
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
