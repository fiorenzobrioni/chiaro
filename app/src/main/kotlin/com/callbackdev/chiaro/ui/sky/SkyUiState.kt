package com.callbackdev.chiaro.ui.sky

import com.callbackdev.chiaro.data.AppSettings
import com.callbackdev.chiaro.data.SkySubscription
import com.callbackdev.chiaro.domain.WeatherFreshness
import com.callbackdev.chiaro.domain.placeZone
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.CloudLayers
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.model.MoonPhase
import com.callbackdev.chiaro.domain.model.WeatherReport
import com.callbackdev.chiaro.domain.sky.AstronomyEngine
import com.callbackdev.chiaro.domain.sky.LunarEclipse
import com.callbackdev.chiaro.domain.sky.MeteorShowerTable
import com.callbackdev.chiaro.domain.sky.MoonOfTheNight
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
import java.time.LocalDate
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
        val subscribedIds: Set<String> = emptySet(),
        /** The place's own coordinates, for the compass: where magnetic north is from
         * true north there (14 ott 2026). */
        val coordinates: Coordinates? = null,
        /** The enabled lines as the store keeps them, leads included: what «Annulla»
         * puts back after a removal (review point 9, 14 ott 2026). */
        val subscriptions: List<SkySubscription> = emptyList(),
        /** When the forecast was fetched, for the line that says how old it is when the
         * verdicts are refused for its age (22 ott 2026); null with no report. */
        val lastSync: Instant? = null,
        /** The hero's sky without the clouds, the reader's choice (22 ott 2026). */
        val bareSky: Boolean = false,
        /** The explorer's constellations, lines and names, the reader's choice (Fase 16.7). */
        val constellations: Boolean = true,
        /** The nights of this place, for the explorer to ask for one by its date (Fase 16.7);
         * null only in a value made by hand. */
        val nights: SkyNights? = null
    ) : SkyUiState
}

/**
 * The nights of one place, any of them (Fase 16, step 16.7): what the explorer asks when a
 * reader steps to the night before or picks one from a calendar. It holds what a night is
 * made from, the place, its forecast and the settings the verdict reads, and makes the
 * night on request ([SkyStateBuilder.night]): the hero's own value, for another date.
 *
 * A value and not a function, so the state it rides in stays comparable: two states with
 * the same forecast are the same state, and a page that reads them does not redraw.
 */
data class SkyNights(
    private val city: City,
    private val report: WeatherReport?,
    private val settings: AppSettings
) {
    /**
     * The night that begins on the evening of [date], as the explorer holds it
     * ([SkyStateBuilder.exploredNight]): the hero's own value, and on a date whose sky never
     * gets fully dark the same value from sunset to sunrise, so its hour can still be moved.
     */
    fun of(date: LocalDate, now: Instant): Tonight = SkyStateBuilder.exploredNight(city, report, settings, date, now)
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
    val clearStretch: ClosedRange<Instant>? = null,
    /**
     * The forecast's cloud cover for each hour of [night] (design review, 23 set 2026):
     * what the hero's night ribbon is greyed by, so "so-so" is a picture of
     * which hours before it is a word. Empty when there is no report or no night.
     */
    val hours: List<NightHour> = emptyList(),
    /** The moon's elongation at the middle of the night, `[0, 360)`: under 180 it is
     * waxing. For the phase the card draws; null with [moonIlluminationPct]. */
    val moonElongationDeg: Double? = null,
    /** South of the equator the moon's lit limb is mirrored. */
    val southern: Boolean = false,
    /**
     * Where the sun and the moon stand over the night, every [NightSampleStep] (14 ott
     * 2026): what the card paints when a finger asks for one moment of the night rather
     * than the whole of it, so the sky it shows at 02:00 is the one computed for 02:00.
     * Empty when there is no night.
     */
    val samples: List<NightSample> = emptyList(),
    /**
     * The evening the night begins on, in the place's zone (Fase 16, step 16.4): the night
     * of the 10th runs into the morning of the 11th and is still the 10th's. Carried
     * because a night with no dusk in it (a white night, the polar night) has no instant
     * to read its date off, and since this step the builder is asked for nights other
     * than the one outside. Always set by [SkyStateBuilder]; null only in a value made by
     * hand.
     */
    val date: LocalDate? = null,
    /** Where the night stands against the forecast: why [hours] are there, or are not. */
    val forecast: NightForecast = NightForecast.REACHES
) {
    /** True when the moon is what opens the window late: the night started earlier. */
    val moonHeldTheStart: Boolean
        get() = window != null && night != null && window.start.isAfter(night.start)

    /** True when the moon is what closes it early. */
    val moonTookTheEnd: Boolean
        get() = window?.end != null && night?.end != null && window.end!!.isBefore(night.end!!)
}

/**
 * Whether the forecast has a night's clouds (Fase 16, step 16.4). The hourly forecast is a
 * week long and the builder can be asked for any night, so «no clouds on the ribbon» has
 * to say which of four things it means, or a sky drawn without clouds reads as a clear one.
 *
 * All or nothing, on purpose: a night the forecast stops inside (the seventh, which it
 * leaves at midnight) is [BEYOND] and has no hours. Half a ribbon greyed by the forecast
 * and half left as it is would draw the half nobody forecast exactly like a clear sky.
 */
enum class NightForecast {
    /** The forecast goes to the end of the night: [Tonight.hours] are its hours, or the
     * ones still to come of a night under way. */
    REACHES,

    /** The night ends past the forecast's last hour. No hours: the sky is drawn without
     * clouds, and the screen says so. */
    BEYOND,

    /** The night is over. No hours and no verdict: a verdict is a forecast, and the app
     * never saw the sky. */
    PAST,

    /** There is no forecast at all. No hours. */
    NONE
}

/** One hour of the night's ribbon: when it starts, how much of the sky is cloud, the
 * chance of precipitation where the model gives one (it is nullable all the way to the
 * screen, CLAUDE.md) and, since 28 ott 2026, the cloud by layer where the model split it,
 * for the card's picture alone: the strip and every printed number stay on the total. */
data class NightHour(
    val at: Instant,
    val cloudPct: Int,
    val precipPct: Int? = null,
    val layers: CloudLayers? = null
)

/** The sun's and the moon's altitude at one instant of the night, in degrees. */
data class NightSample(val at: Instant, val sunAltitudeDeg: Double, val moonAltitudeDeg: Double)

/** Ten minutes: a sky that changes slower than that cannot be told apart by eye, and a
 * fourteen-hour polar night is still under a hundred samples. */
val NightSampleStep: Duration = Duration.ofMinutes(10)

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
    /** The exact instant of the quarter [moonPhase] is named after, when it is one of the
     * four: the row prints its day and hour beside the name (review point 6). */
    val moonQuarterAt: Instant? = null,
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
internal val SkyOccurrence.At.middle: Instant
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

        val moments = moments(subscriptions, settings, city, zone, now, ::judge)
        return SkyUiState.Content(
            placeName = city.name,
            zone = zone,
            tonight = tonight(city, zone, now, report?.hourly.orEmpty(), ::judge),
            moments = moments,
            events = events(
                subscriptions, settings, city, zone, now, ::judge,
                // The quarters the agenda already names (22 ott 2026): «Today's moon · Full
                // moon tomorrow at 18:49» and a «Full moon · tomorrow» row under it said one
                // thing twice, and the calendar's row is for the reader the agenda does not serve.
                agendaQuarters = moments.mapNotNull { it.moonQuarterAt }.toSet()
            ),
            defaultLead = SkyLead.ofMinutes(settings.skyNotifyDefaultMin),
            notifyOnFail = settings.skyNotifyOnFail,
            subscribedIds = subscriptions.filter { it.enabled }.map { it.jobId }.toSet(),
            coordinates = city.coordinates,
            subscriptions = subscriptions.filter { it.enabled },
            lastSync = report?.systemInfo?.lastSync,
            bareSky = settings.skyBareSky,
            constellations = settings.skyConstellations,
            nights = SkyNights(city, report, settings)
        )
    }

    /**
     * The night that begins on the evening of [date], whichever it is (Fase 16, step 16.4):
     * the same value the hero is built from, for the night a reader picks in the explorer.
     * The astronomy is computed for any date; the clouds are the forecast's, which is a
     * week long, and [Tonight.forecast] says whether it gets there. [now] is what the
     * forecast's age and «the night is over» are measured against.
     */
    fun night(
        city: City,
        report: WeatherReport?,
        settings: AppSettings,
        date: LocalDate,
        now: Instant
    ): Tonight {
        val zone = placeZone(report, city)
        val staleAfter = WeatherFreshness.staleAfter(settings.updateFrequencyMin)
        val dataAge = report?.let { Duration.between(it.systemInfo.lastSync, now) }
        val job = SkyJobCatalog.DarknessWindow
        // The job's own answer for that date: the night minus the moon ([SkyScheduler.resolve]).
        val window = SkyScheduler.darkNight(date, zone, city.coordinates).moonless
        return nightOf(date, window, city, zone, now, report?.hourly.orEmpty()) { at ->
            SkyVerdictEngine.evaluate(
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
    }

    /**
     * The night of [date] for the explorer's ribbon (11 nov 2026, the box 16.7 left open).
     * [night] itself wherever the date has an astronomical night. Where the sky never gets
     * fully dark (the white nights: London for two months round the solstice, Stockholm
     * for four) [night] has no span, and a page whose handle on the hour is the night's
     * ribbon then has no handle: a reader in London at three in the afternoon of a June
     * day was shown the afternoon's sky and could not ask for half past eleven. So there
     * the span is **sunset to sunrise**, with the sun's place along it and the forecast's
     * hours over it, and everything else is left as it was: no dark window, no verdict,
     * and the reason, which the panel still prints. The sky is not said to be dark; it is
     * given hours.
     *
     * The hero is not changed: it reads [build]'s night, and a white night there is a
     * statement with no ribbon, as before. A date with no sunset in it (the midnight sun)
     * and the deep polar night keep no span here either.
     */
    fun exploredNight(
        city: City,
        report: WeatherReport?,
        settings: AppSettings,
        date: LocalDate,
        now: Instant
    ): Tonight {
        val tonight = night(city, report, settings, date, now)
        if (tonight.night != null || tonight.reason != SkyNotScheduled.NO_DARKNESS) return tonight
        val zone = placeZone(report, city)
        val sunset = SkyAlmanac.solarDay(date, zone, city.coordinates).sunset ?: return tonight
        val sunrise = SkyAlmanac.solarDay(date.plusDays(1), zone, city.coordinates).sunrise ?: return tonight
        if (!sunrise.isAfter(sunset)) return tonight
        val span = SkyOccurrence.At(SkyJobCatalog.DarknessWindow, sunset, sunrise)
        val hourly = report?.hourly.orEmpty()
        val forecast = nightForecast(span, sunset, hourly, now)
        return tonight.copy(
            night = span,
            hours = if (forecast == NightForecast.REACHES) nightHours(span, hourly) else emptyList(),
            samples = nightSamples(span, city.coordinates),
            forecast = forecast
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
        hourly: List<com.callbackdev.chiaro.domain.model.HourlyForecast>,
        judge: (SkyJob, SkyOccurrence.At) -> SkyVerdict?
    ): Tonight {
        val job = SkyJobCatalog.DarknessWindow
        val upcoming = SkyUpcoming.of(job, now, zone, city.coordinates)
        return nightOf(upcoming.date, upcoming.occurrence, city, zone, now, hourly) { judge(job, it) }
    }

    /**
     * One night, from the evening it begins on and the dark window found for it: the one
     * place a [Tonight] is made, so the night outside and a night picked from a calendar
     * are the same kind of thing.
     */
    private fun nightOf(
        date: LocalDate,
        window: SkyOccurrence,
        city: City,
        zone: ZoneId,
        now: Instant,
        hourly: List<com.callbackdev.chiaro.domain.model.HourlyForecast>,
        judge: (SkyOccurrence.At) -> SkyVerdict?
    ): Tonight {
        // The night around the window, asked for the same local day the window came
        // from: since Fase 27 the window is the night minus the moon, and the card
        // cannot explain a short window without the night it was cut out of.
        val night = SkyScheduler.darkNight(date, zone, city.coordinates).night
            as? SkyOccurrence.At
        // The one moon of the screen ([MoonOfTheNight]): «today's moon» reads the same
        // instant, so the card and the row cannot print two percentages for one evening.
        val moonAt = MoonOfTheNight.instant(date, zone, city.coordinates)
        val moonLight = night?.let { AstronomyEngine.moonIllumination(moonAt) }
        val moonPct = moonLight?.let { (it.illuminatedFraction * 100).roundToInt() }
        val forecast = nightForecast(night, moonAt, hourly, now)
        val hours = if (forecast == NightForecast.REACHES) nightHours(night, hourly) else emptyList()
        val samples = nightSamples(night, city.coordinates)
        val at = window as? SkyOccurrence.At ?: return Tonight(
            window = null,
            verdict = null,
            reason = (window as? SkyOccurrence.None)?.reason,
            night = night,
            moonIlluminationPct = moonPct,
            hours = hours,
            moonElongationDeg = moonLight?.elongation,
            southern = city.coordinates.lat < 0.0,
            samples = samples,
            date = date,
            forecast = forecast
        )
        // A night that is over has no verdict and no clear stretch: both are forecasts.
        val ahead = forecast != NightForecast.PAST
        return Tonight(
            window = at,
            verdict = if (ahead) judge(at) else null,
            night = night,
            moonIlluminationPct = moonPct,
            clearStretch = if (ahead) SkyVerdictEngine.clearStretch(at.start, at.end, hourly, zone) else null,
            hours = hours,
            moonElongationDeg = moonLight?.elongation,
            southern = city.coordinates.lat < 0.0,
            samples = samples,
            date = date,
            forecast = forecast
        )
    }

    /**
     * Where a night stands against the forecast ([NightForecast]). The night is the
     * astronomical one; where the date has none (a white night, the polar night) the
     * instant that stands in for it is the one its moon is read at, [fallback].
     *
     * Over when its dawn is behind [now]; not reached when the last forecast hour ends
     * before its dawn. A night under way whose first hours the report no longer carries
     * ([com.callbackdev.chiaro.domain.WeatherRecency] drops what has gone by) is reached:
     * what is left of it is all there.
     */
    internal fun nightForecast(
        night: SkyOccurrence.At?,
        fallback: Instant,
        hourly: List<com.callbackdev.chiaro.domain.model.HourlyForecast>,
        now: Instant
    ): NightForecast {
        val end = night?.end ?: night?.start ?: fallback
        val last = hourly.lastOrNull() ?: return NightForecast.NONE
        return when {
            !end.isAfter(now) -> NightForecast.PAST
            last.at.plus(Duration.ofHours(1)).isBefore(end) -> NightForecast.BEYOND
            else -> NightForecast.REACHES
        }
    }

    /** The sun and the moon over [night], every [NightSampleStep] and at dawn itself. */
    internal fun nightSamples(night: SkyOccurrence.At?, coordinates: Coordinates): List<NightSample> {
        val start = night?.start ?: return emptyList()
        val end = night.end ?: return emptyList()
        val instants = generateSequence(start) { it.plus(NightSampleStep) }
            .takeWhile { it.isBefore(end) }
            .plus(end)
        return instants.map {
            NightSample(it, AstronomyEngine.sunAltitude(it, coordinates), AstronomyEngine.moonAltitude(it, coordinates))
        }.toList()
    }

    /** The forecast hours that overlap [night], each with its cloud cover: an hour that
     * starts before dusk still counts for the part of it that is night. */
    internal fun nightHours(
        night: SkyOccurrence.At?,
        hourly: List<com.callbackdev.chiaro.domain.model.HourlyForecast>
    ): List<NightHour> {
        val start = night?.start ?: return emptyList()
        val end = night.end ?: return emptyList()
        return hourly
            .filter { it.at.plus(Duration.ofHours(1)).isAfter(start) && it.at.isBefore(end) }
            .map { NightHour(it.at, it.cloudCoverPct, it.precipChancePct, it.cloudLayers) }
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
                // Read at the night's middle, not at the occurrence's noon (which stays
                // the reminder's time): [MoonOfTheNight].
                val moonAt = if (isMoonDay && at != null) {
                    MoonOfTheNight.instant(upcoming.date, zone, city.coordinates)
                } else null
                val phase = moonAt?.let { MoonPhase.at(it) }
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
                    moonPhase = phase,
                    moonIlluminationPct = moonAt?.let {
                        (AstronomyEngine.moonIllumination(it).illuminatedFraction * 100).roundToInt()
                    },
                    moonQuarterAt = if (phase != null && moonAt != null) {
                        MoonOfTheNight.quarterOf(phase, moonAt)?.at
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
        judge: (SkyJob, SkyOccurrence.At) -> SkyVerdict?,
        agendaQuarters: Set<Instant> = emptySet()
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
            // Within a minute, not equal: the two searches converge on the same quarter from
            // different starts and can differ by seconds.
            nextFullMoon(now)?.takeUnless { full ->
                agendaQuarters.any { Duration.between(it, full).abs() <= Duration.ofMinutes(1) }
            }?.let { at ->
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
