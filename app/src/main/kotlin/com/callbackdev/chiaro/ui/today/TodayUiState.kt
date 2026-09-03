package com.callbackdev.chiaro.ui.today

import com.callbackdev.chiaro.domain.WeatherFreshness
import com.callbackdev.chiaro.domain.WeatherRecency
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.DailyForecast
import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.model.WeatherReport
import com.callbackdev.chiaro.domain.sky.AstronomyEngine
import com.callbackdev.chiaro.ui.components.LightPhase
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * The states Today can be in, each one the honest name of a situation the reader is
 * actually in. There is deliberately no "Loading": before anything is known the screen
 * is [Starting] (a skeleton, §8.11), and while a refresh runs the CONTENT stays up
 * with [Content.refreshing] set — no full-screen spinner exists in this product.
 */
sealed interface TodayUiState {

    /** The stores have not answered yet. Painted as a skeleton, never as values. */
    data object Starting : TodayUiState

    /** A fresh install, or every place removed: the one action that fixes it is the
     * whole screen. */
    data object NoPlace : TodayUiState

    /** A place is set but no report for it has ever landed (or the last one no longer
     * covers the present). The skeleton, plus the fetch state and its error. */
    data class Empty(
        val city: City,
        val refreshing: Boolean,
        val error: TodayError?
    ) : TodayUiState

    /** The screen. [report] is trimmed to now — hours already over are not in it. */
    data class Content(
        val city: City,
        val report: WeatherReport,
        val zone: ZoneId,
        val now: LocalDateTime,
        val night: Boolean,
        val sky: SkySnapshot,
        val headline: Headline?,
        val strip: List<StripHour>,
        val timeline: List<TimelineItem>,
        val week: List<WeekDay>,
        val lastSync: Instant,
        val isStale: Boolean,
        val refreshing: Boolean,
        val error: TodayError?,
        /** VISION §5.2.5: the latest forecast revisions, at most three lines,
         * tapping opens the Journal. Filled by the ViewModel, not the builder —
         * it comes from the history, which the builder deliberately never reads. */
        val whatChanged: List<com.callbackdev.chiaro.ui.journal.JournalEntry.ForecastShift> =
            emptyList()
    ) : TodayUiState
}

/** Why the last refresh failed, said in words at render time. */
enum class TodayError { OFFLINE, SERVICE, UNKNOWN }

/** Everything the canvas and the ribbon need for this exact moment. */
data class SkySnapshot(
    val sunAltitudeDeg: Double,
    val cloudPct: Int,
    val precipPct: Int,
    val moonIllumination: Double,
    val moonAltitudeDeg: Double,
    val phases: List<LightPhase>,
    val nowFraction: Float
)

enum class TimelineKind {
    SUNRISE, GOLDEN_MORNING_END, GOLDEN_EVENING, SUNSET, BLUE_EVENING, DARK,
    MOONRISE, MOONSET, RAIN_START, RAIN_STOP
}

/** One row of "the rest of the day". [pct] only for the rain turns. */
data class TimelineItem(val at: LocalDateTime, val kind: TimelineKind, val pct: Int? = null)

/** One cell's worth of forecast, with its own day/night already decided — the strip
 * crosses midnight, so "is it night" is per hour, not per screen. */
data class StripHour(val hour: HourlyForecast, val night: Boolean)

/** One row of the week: the forecast, the day's light, and its hours for the
 * in-place expansion. */
data class WeekDay(
    val forecast: DailyForecast,
    val phases: List<LightPhase>,
    val hours: List<StripHour>
)

/**
 * Builds [TodayUiState.Content] from a raw report and a moment. Pure and clock-free —
 * everything time-dependent is a parameter — so the whole screen's state is
 * table-testable the way the engines are.
 */
object TodayStateBuilder {

    /** §8.3: the strip is the next 24 full hours; `hourly[0]` is the hour we are in
     * and the canvas' business, not the strip's. */
    const val STRIP_HOURS = 24

    private const val RAIN_TURN_PCT = 50

    fun build(
        city: City,
        report: WeatherReport,
        now: Instant,
        updateFrequencyMin: Int,
        refreshing: Boolean,
        error: TodayError?
    ): TodayUiState {
        val zone = runCatching { ZoneId.of(report.location.timezone) }
            .getOrDefault(ZoneId.systemDefault())
        if (!WeatherRecency.coversNow(report, now)) {
            return TodayUiState.Empty(city, refreshing, error)
        }
        val trimmed = WeatherRecency.trim(report, now)
        val local = LocalDateTime.ofInstant(now, zone)
        val coords = report.location.coordinates
        val sunAltitude = AstronomyEngine.sunAltitude(now, coords)
        val today = AstronomyEngine.solarDay(local.toLocalDate(), zone, coords)
        val currentHour = trimmed.hourly.first()
        val moon = AstronomyEngine.moonIllumination(now)

        return TodayUiState.Content(
            city = city,
            report = trimmed,
            zone = zone,
            now = local,
            night = sunAltitude < 0.0,
            sky = SkySnapshot(
                sunAltitudeDeg = sunAltitude,
                cloudPct = currentHour.cloudCoverPct,
                precipPct = currentHour.precipChancePct,
                moonIllumination = moon.illuminatedFraction,
                moonAltitudeDeg = AstronomyEngine.moonAltitude(now, coords),
                phases = DaylightPhases.phases(today, zone),
                nowFraction = DaylightPhases.fraction(local)
            ),
            headline = HeadlineEngine.headline(trimmed, local),
            strip = trimmed.hourly.drop(1).take(STRIP_HOURS).map { stripHour(it, zone, coords) },
            timeline = timeline(trimmed, today, zone, local),
            week = week(trimmed, zone, coords),
            lastSync = report.systemInfo.lastSync,
            isStale = WeatherFreshness.isStale(report.systemInfo.lastSync, updateFrequencyMin, now),
            refreshing = refreshing,
            error = error
        )
    }

    /**
     * The merged rest of the day (VISION §5.2.4): today's remaining sun and moon
     * moments plus the hours where rain becomes likely or stops being. The reader's
     * own alerts join this list in Fase 6, through the same [TimelineItem].
     */
    private fun timeline(
        report: WeatherReport,
        today: com.callbackdev.chiaro.domain.sky.SolarDay,
        zone: ZoneId,
        now: LocalDateTime
    ): List<TimelineItem> {
        val items = mutableListOf<TimelineItem>()
        fun sun(at: Instant?, kind: TimelineKind) {
            at?.let { items += TimelineItem(LocalDateTime.ofInstant(it, zone), kind) }
        }
        sun(today.sunrise, TimelineKind.SUNRISE)
        sun(today.goldenHourMorningEnd, TimelineKind.GOLDEN_MORNING_END)
        sun(today.goldenHourEveningStart, TimelineKind.GOLDEN_EVENING)
        sun(today.sunset, TimelineKind.SUNSET)
        sun(today.blueHourEveningStart, TimelineKind.BLUE_EVENING)
        sun(today.astronomicalDusk, TimelineKind.DARK)

        val lunar = AstronomyEngine.lunarDay(now.toLocalDate(), zone, report.location.coordinates)
        sun(lunar.moonrise, TimelineKind.MOONRISE)
        sun(lunar.moonset, TimelineKind.MOONSET)

        // Rain turns: the hours where the chance crosses half, up or down, today.
        val todayHours = report.hourly.filter { it.time.toLocalDate() == now.toLocalDate() }
        todayHours.zipWithNext().forEach { (a, b) ->
            if (a.precipChancePct < RAIN_TURN_PCT && b.precipChancePct >= RAIN_TURN_PCT) {
                items += TimelineItem(b.time, TimelineKind.RAIN_START, b.precipChancePct)
            }
            if (a.precipChancePct >= RAIN_TURN_PCT && b.precipChancePct < RAIN_TURN_PCT) {
                items += TimelineItem(b.time, TimelineKind.RAIN_STOP)
            }
        }

        return items
            .filter { it.at.isAfter(now) && it.at.toLocalDate() == now.toLocalDate() }
            .sortedBy { it.at }
    }

    private fun stripHour(
        hour: HourlyForecast,
        zone: ZoneId,
        coords: com.callbackdev.chiaro.domain.model.Coordinates
    ): StripHour = StripHour(
        hour = hour,
        night = AstronomyEngine.sunAltitude(hour.time.atZone(zone).toInstant(), coords) < 0.0
    )

    /** The week's rows, each with its light and its hours for the tap-to-expand. */
    private fun week(
        report: WeatherReport,
        zone: ZoneId,
        coords: com.callbackdev.chiaro.domain.model.Coordinates
    ): List<WeekDay> = report.daily.map { day ->
        WeekDay(
            forecast = day,
            phases = DaylightPhases.phases(
                AstronomyEngine.solarDay(day.date, zone, coords), zone
            ),
            hours = report.hourly
                .filter { it.time.toLocalDate() == day.date }
                .map { stripHour(it, zone, coords) }
        )
    }
}
