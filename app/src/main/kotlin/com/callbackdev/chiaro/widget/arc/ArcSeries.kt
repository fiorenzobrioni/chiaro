package com.callbackdev.chiaro.widget.arc

import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.sky.AstronomyEngine
import com.callbackdev.chiaro.domain.sky.MoonLight
import com.callbackdev.chiaro.domain.sky.SkyVerdict
import com.callbackdev.chiaro.ui.today.TimelineItem
import com.callbackdev.chiaro.ui.today.TimelineKind
import com.callbackdev.chiaro.ui.today.TodayStateBuilder
import com.callbackdev.chiaro.ui.today.TodayUiState
import com.callbackdev.chiaro.widget.NextMoment
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * The stretch of time the arc spans, and where a moment falls in it. [fraction] is the
 * plot's x axis: 0 at the left edge, 1 at the right.
 */
internal data class ArcWindow(val start: Instant, val end: Instant) {
    val length: Duration get() = Duration.between(start, end)

    fun fraction(at: Instant): Float =
        (Duration.between(start, at).toMillis().toDouble() / length.toMillis()).toFloat()

    fun at(fraction: Float): Instant =
        start.plusMillis((length.toMillis() * fraction.toDouble()).toLong())

    operator fun contains(at: Instant): Boolean = !at.isBefore(start) && at.isBefore(end)
}

/** One forecast hour inside the window, with its place on the axis. */
internal data class ArcHour(val hour: HourlyForecast, val at: Instant, val fraction: Float)

/**
 * One row of the agenda: the Today screen's own [TimelineItem], with its instant, whether
 * it falls on a later date than the reader's (the row says «tomorrow»), and — when the
 * reader follows that moment on the Sky screen — the verdict the Sky widget would print.
 */
internal data class ArcEvent(
    val item: TimelineItem,
    val at: Instant,
    val tomorrow: Boolean,
    val verdict: SkyVerdict?
)

/**
 * Everything the arc draws, derived once from the cached report and the astronomy engine
 * and nothing else — no clock, no network, no resources — so a table can pin it
 * ([ArcSeriesTest]) and a composition can cache it.
 *
 * [sun] and [moon] are altitude samples across the window, evenly spaced, [SunSamples] and
 * [MoonSamples] of them; the painter joins them. Equality is by identity for the arrays,
 * which is what a `remember` key wants: the series is rebuilt only when its inputs change.
 */
internal data class ArcSeries(
    val window: ArcWindow,
    val now: Instant,
    /** Where the present sits on the axis, 0..1. */
    val nowFraction: Float,
    val sun: FloatArray,
    val moon: FloatArray,
    val sunNowDeg: Double,
    val moonNowDeg: Double,
    val moonLight: MoonLight,
    /** Northern-hemisphere readers see a waxing moon lit on the right; southern ones on
     * the left. The disc is drawn the way the reader would see it. */
    val southern: Boolean,
    /** The forecast hours inside the window, in order. The report is trimmed to the
     * present, so in a [ArcSpan.TODAY] window the morning has none: what is past is not
     * drawn (WeatherRecency's rule). */
    val hours: List<ArcHour>,
    /** The agenda of the next twenty-four hours, soonest first, filtered by the reader's
     * settings — whatever the window shows. */
    val events: List<ArcEvent>,
    val highC: Double?,
    val lowC: Double?
) {
    /** The moment in front of the reader; null when the agenda is empty. */
    val next: ArcEvent? get() = events.firstOrNull()

    /** The forecast hour starting exactly at [at], if the window has it. */
    fun hourAt(at: Instant): ArcHour? = hours.firstOrNull { it.at == at }

    /** The forecast hour that contains [at]: the one that started at or before it. */
    fun hourCovering(at: Instant): ArcHour? = hours.lastOrNull { !it.at.isAfter(at) }?.takeIf {
        Duration.between(it.at, at) < Duration.ofHours(1)
    }

    /**
     * The whole local hours inside the window whose hour-of-day is a multiple of
     * [stepHours]: where the plot writes its labels. The end is exclusive, so a
     * midnight-to-midnight window labels 0 and never 24.
     */
    fun ticks(zone: ZoneId, stepHours: Int): List<Pair<Instant, LocalDateTime>> {
        if (stepHours <= 0) return emptyList()
        val ticks = mutableListOf<Pair<Instant, LocalDateTime>>()
        var local = LocalDateTime.ofInstant(window.start, zone).truncatedTo(ChronoUnit.HOURS)
        var at = local.atZone(zone).toInstant()
        if (at.isBefore(window.start)) {
            local = local.plusHours(1)
            at = local.atZone(zone).toInstant()
        }
        while (at.isBefore(window.end)) {
            if (local.hour % stepHours == 0) ticks += at to local
            local = local.plusHours(1)
            at = local.atZone(zone).toInstant()
        }
        return ticks
    }

    companion object {
        /** Every fifteen minutes over a day: the sun's path is smooth at that spacing,
         * and 97 altitudes are a few milliseconds of arithmetic. */
        const val SunSamples = 97

        /** The moon moves as slowly across the sky; every half hour is plenty. */
        const val MoonSamples = 49

        /** The agenda looks this far ahead, whatever the plot spans. */
        val AgendaReach: Duration = Duration.ofHours(24)

        /** In the rolling window the present sits this far in from the left edge, so the
         * sun's disc has room to be a disc and the last half hour reads as just past. */
        val AheadLeadIn: Duration = Duration.ofMinutes(30)

        /** How close a followed moment has to be to an agenda row to lend it its verdict:
         * the two are computed by the same engine, so they agree to the minute — the
         * tolerance is for a range's edge against an instant. */
        val VerdictMatch: Duration = Duration.ofMinutes(20)

        fun build(
            content: TodayUiState.Content,
            moments: List<NextMoment>,
            coords: Coordinates,
            now: Instant,
            settings: ArcSettings
        ): ArcSeries {
            val zone = content.zone
            val window = window(settings.span, now, zone)
            val sun = FloatArray(SunSamples) { i ->
                AstronomyEngine.sunAltitude(window.at(i / (SunSamples - 1f)), coords).toFloat()
            }
            val moon = FloatArray(MoonSamples) { i ->
                AstronomyEngine.moonAltitude(window.at(i / (MoonSamples - 1f)), coords).toFloat()
            }
            val hours = content.report.hourly.mapNotNull { hour ->
                val at = hour.time.atZone(zone).toInstant()
                if (at in window) ArcHour(hour, at, window.fraction(at)) else null
            }
            val local = LocalDateTime.ofInstant(now, zone)
            val events = TodayStateBuilder
                .agenda(content.report, zone, local, local.plus(AgendaReach))
                .filter { allowed(it.kind, settings) }
                .map { item ->
                    ArcEvent(
                        item = item,
                        at = item.at.atZone(zone).toInstant(),
                        tomorrow = item.at.toLocalDate() != local.toLocalDate(),
                        verdict = if (settings.agendaVerdicts) verdictFor(item, moments, zone) else null
                    )
                }
            val today = content.week.firstOrNull()?.forecast
            return ArcSeries(
                window = window,
                now = now,
                nowFraction = window.fraction(now).coerceIn(0f, 1f),
                sun = sun,
                moon = moon,
                sunNowDeg = AstronomyEngine.sunAltitude(now, coords),
                moonNowDeg = AstronomyEngine.moonAltitude(now, coords),
                moonLight = AstronomyEngine.moonIllumination(now),
                southern = coords.lat < 0.0,
                hours = hours,
                events = events,
                highC = today?.highC,
                lowC = today?.lowC
            )
        }

        /**
         * The window for a span. [ArcSpan.TODAY] is the civil day of the place — from
         * one local midnight to the next, so a clock-change day is honestly 23 or 25
         * hours long. [ArcSpan.AHEAD] runs from half an hour ago to twenty-three and a
         * half hours from now: a day, with the present a little in from the edge.
         */
        fun window(span: ArcSpan, now: Instant, zone: ZoneId): ArcWindow = when (span) {
            ArcSpan.TODAY -> {
                val date = now.atZone(zone).toLocalDate()
                ArcWindow(
                    date.atStartOfDay(zone).toInstant(),
                    date.plusDays(1).atStartOfDay(zone).toInstant()
                )
            }
            ArcSpan.AHEAD -> ArcWindow(
                now.minus(AheadLeadIn),
                now.minus(AheadLeadIn).plus(Duration.ofHours(24))
            )
        }

        /** Which of the three families an agenda kind belongs to, and whether it is on. */
        fun allowed(kind: TimelineKind, settings: ArcSettings): Boolean = when (kind) {
            TimelineKind.SUNRISE, TimelineKind.GOLDEN_MORNING_END, TimelineKind.GOLDEN_EVENING,
            TimelineKind.SUNSET, TimelineKind.BLUE_EVENING, TimelineKind.DARK -> settings.agendaSun
            TimelineKind.MOONRISE, TimelineKind.MOONSET -> settings.agendaMoon
            TimelineKind.RAIN_START, TimelineKind.RAIN_STOP, TimelineKind.RAINBOW -> settings.agendaRain
        }

        /**
         * The Sky jobs that name the same moment as a timeline kind. The morning golden
         * hour's END is the end of the `golden_hour.am` range; full darkness is the end of
         * astronomical twilight or the start of the darkness window; the rest are instants.
         * The rain's turns and the rainbow have no job: nobody subscribes to a shower.
         */
        fun jobIdsFor(kind: TimelineKind): List<String> = when (kind) {
            TimelineKind.SUNRISE -> listOf("sun.rise")
            TimelineKind.GOLDEN_MORNING_END -> listOf("golden_hour.am")
            TimelineKind.GOLDEN_EVENING -> listOf("golden_hour.pm")
            TimelineKind.SUNSET -> listOf("sun.set")
            TimelineKind.BLUE_EVENING -> listOf("blue_hour.pm")
            TimelineKind.DARK -> listOf("twilight.astronomical.pm", "darkness.window")
            TimelineKind.MOONRISE -> listOf("moon.rise")
            TimelineKind.MOONSET -> listOf("moon.set")
            TimelineKind.RAIN_START, TimelineKind.RAIN_STOP, TimelineKind.RAINBOW -> emptyList()
        }

        /** The verdict of the followed moment that IS this row, if the reader follows one. */
        fun verdictFor(item: TimelineItem, moments: List<NextMoment>, zone: ZoneId): SkyVerdict? {
            val ids = jobIdsFor(item.kind)
            if (ids.isEmpty() || moments.isEmpty()) return null
            val at = item.at.atZone(zone).toInstant()
            return moments.firstOrNull { moment ->
                moment.job.id in ids &&
                    (near(moment.start, at) || moment.end?.let { near(it, at) } == true)
            }?.verdict
        }

        private fun near(a: Instant, b: Instant): Boolean =
            Duration.between(a, b).abs() <= VerdictMatch
    }
}
