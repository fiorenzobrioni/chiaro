package com.callbackdev.chiaro.ui.today

import com.callbackdev.chiaro.domain.CurrentEstimate
import com.callbackdev.chiaro.domain.NowcastEngine
import com.callbackdev.chiaro.domain.WeatherFreshness
import com.callbackdev.chiaro.domain.WeatherRecency
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.DailyForecast
import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.model.WeatherReport
import com.callbackdev.chiaro.domain.placeZone
import com.callbackdev.chiaro.domain.rules.NotificationRule
import com.callbackdev.chiaro.domain.rules.RuleCheck
import com.callbackdev.chiaro.domain.rules.RuleEngine
import com.callbackdev.chiaro.domain.rules.RuleWindows
import com.callbackdev.chiaro.domain.sky.AstronomyEngine
import com.callbackdev.chiaro.domain.sky.SkyJob
import com.callbackdev.chiaro.domain.sky.SkyJobCatalog
import com.callbackdev.chiaro.domain.sky.SkyOccurrence
import com.callbackdev.chiaro.domain.sky.SkyScheduler
import com.callbackdev.chiaro.domain.sky.SkyVerdict
import com.callbackdev.chiaro.domain.sky.SkyVerdictEngine
import com.callbackdev.chiaro.domain.warnings.PlaceWarnings
import com.callbackdev.chiaro.domain.warnings.WarningLevel
import com.callbackdev.chiaro.ui.components.LightPhase
import com.callbackdev.chiaro.ui.components.RibbonHour
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * The states Today can be in, each one the honest name of a situation the reader is
 * actually in. There is deliberately no "Loading": before anything is known the screen
 * is [Starting] (a skeleton, §8.11), and while a refresh runs the CONTENT stays up —
 * no full-screen spinner exists in this product.
 *
 * [Content.userRefreshing] is narrower than it looks and the name is the point (Fase
 * 3b). It is true only while a refresh the READER asked for is running, because the
 * only thing it drives is the pull-to-refresh indicator, and that indicator means
 * "doing what you just asked". The automatic fetch — cold start, TTL expiry, landing
 * on a page — runs with this false and says nothing: VISION §5.2 spells it out,
 * content first, freshness stated, refresh silent. What the reader is told about an
 * automatic refresh is the freshness chip, which states the real age either way.
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
        val userRefreshing: Boolean,
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
        val userRefreshing: Boolean,
        val error: TodayError?,
        /** VISION §5.2.6: the latest forecast revisions, at most three lines.
         * Filled by the ViewModel, not the builder — it comes from the history,
         * which the builder deliberately never reads. */
        val whatChanged: List<ForecastShift> = emptyList(),
        /**
         * The official warning for this place (Fase 11), or null where there is none to
         * draw — abroad, no bulletin, or a bulletin whose days are over. Green is null
         * HERE and a value in Avvisi: on this screen an absence is not drawn (§1.1),
         * on that one it is the answer somebody came for.
         */
        val warnings: PlaceWarnings? = null,
        /**
         * The next two hours a quarter at a time (17 ott 2026), only when something falls in
         * them: the section is not drawn for a dry window, and is never drawn where the app
         * has no 15-minute forecast it stands behind ([NowcastEngine.read] is null there).
         */
        val nowcast: NowcastEngine.Reading? = null
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
    val nowFraction: Float,
    /** Where the sun and the moon stand, for the discs on the canvas (23 set 2026):
     * compass bearings, clockwise from north, and the moon's elongation for its phase. */
    val sunAzimuthDeg: Double = 180.0,
    val moonAzimuthDeg: Double = 180.0,
    val moonElongationDeg: Double = 0.0,
    val southern: Boolean = false,
    /** The rest of today's forecast sky, for the canvas' ribbon (12 ott 2026). */
    val hours: List<RibbonHour> = emptyList(),
    /** The canvas' scene (DESIGN §3.9, 15 ott 2026): the cloud layers of the hour, what
     * falls in it by the state engine and how much, and the wind that moves the clouds. */
    val layers: com.callbackdev.chiaro.domain.model.CloudLayers? = null,
    val fall: com.callbackdev.chiaro.ui.components.SkyFall = com.callbackdev.chiaro.ui.components.SkyFall.NONE,
    val fallMmPerHour: Double = 0.0,
    val windKph: Double = 0.0,
    val windFromDeg: Int = 0
)

enum class TimelineKind {
    SUNRISE, GOLDEN_MORNING_END, GOLDEN_EVENING, SUNSET, BLUE_EVENING, DARK,
    MOONRISE, MOONSET, RAIN_START, RAIN_STOP, RAINBOW,
    /** One of the reader's own rules that holds today (VISION §5.2.4, 15 ott 2026). */
    ALERT
}

/**
 * The Sky's moment a row of the day is (15 ott 2026), whose page the row opens, whose name it
 * prints and whose verdict it carries. The morning's golden hour ends here rather than starts,
 * so its row keeps its own words and opens the morning's page. Null for what the Sky has no
 * page for: the weather's turns, the rainbow and the reader's rules.
 */
val TimelineKind.skyJob: SkyJob?
    get() = when (this) {
        TimelineKind.SUNRISE -> SkyJobCatalog.SunRise
        TimelineKind.GOLDEN_MORNING_END -> SkyJobCatalog.GoldenAm
        TimelineKind.GOLDEN_EVENING -> SkyJobCatalog.GoldenPm
        TimelineKind.SUNSET -> SkyJobCatalog.SunSet
        TimelineKind.BLUE_EVENING -> SkyJobCatalog.BluePm
        // From the sun at −18° the sky is as dark as the sun leaves it: the Sky's «Buio pieno».
        TimelineKind.DARK -> SkyJobCatalog.DarknessWindow
        TimelineKind.MOONRISE -> SkyJobCatalog.MoonRise
        TimelineKind.MOONSET -> SkyJobCatalog.MoonSet
        TimelineKind.RAINBOW, TimelineKind.RAIN_START, TimelineKind.RAIN_STOP, TimelineKind.ALERT -> null
    }

/**
 * A rule of the reader's that holds on this report (15 ott 2026): the rule, and what its
 * first condition read and when, which is what its message's `{trigger.*}` placeholders say.
 */
data class AgendaRule(
    val rule: NotificationRule,
    val value: Double,
    val at: LocalDateTime?
)

/**
 * One row of "the rest of the day". [pct] carries the rain probability of the turns
 * and of the rainbow window; [bearingDeg] is the rainbow's only, and is where to
 * look — a rainbow is the one row on this screen that comes with a direction.
 */
data class TimelineItem(
    val at: LocalDateTime,
    val kind: TimelineKind,
    val pct: Int? = null,
    val bearingDeg: Double? = null,
    /** Where a span ends, for the rows that are spans and not moments (12 ott 2026):
     * the evening's golden hour runs to sunset, its blue hour to the sun at −6°. Null for a
     * moment, and for a span whose end the day does not have. */
    val until: LocalDateTime? = null,
    /** Will the sky allow it (15 ott 2026): the Sky tab's verdict for the same moment, from
     * the same engine on the same hours, so the two tabs cannot disagree. Null for a row
     * that is not a sight (a weather turn, a rule) and for a sight the clouds cannot spoil. */
    val verdict: SkyVerdict? = null,
    /** Why the Sky has no occurrence of this row's sight today («the moon is up all night»),
     * for a row whose clock time is the sun's and not the sky's: full darkness at 20:54 under
     * a full moon is a time the sky will not keep, and the row says so in the Sky's words. */
    val skipped: com.callbackdev.chiaro.domain.sky.SkyNotScheduled? = null,
    /** The reader's rule, on an [TimelineKind.ALERT] row. */
    val rule: AgendaRule? = null,
    /** Holding now rather than at [at]: a rule about this very moment. The row's clock column
     * says «Now», and the row stands before every moment still ahead. */
    val now: Boolean = false
)

/** One cell's worth of forecast, with its own day/night already decided — the strip
 * crosses midnight, so "is it night" is per hour, not per screen. */
data class StripHour(val hour: HourlyForecast, val night: Boolean)

/** One row of the week: the forecast, the day's light, its hours for the in-place
 * expansion, and its forecast sky for the ribbon (12 ott 2026). */
data class WeekDay(
    val forecast: DailyForecast,
    val phases: List<LightPhase>,
    val hours: List<StripHour>,
    val sky: List<RibbonHour> = emptyList()
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

    /**
     * [warnings] is what the official-warnings step last wrote for this place. It reaches
     * the builder rather than being read here for the reason `whatChanged` does not:
     * this object never touches a store. A warning with nothing above NONE is dropped on
     * the way in — the banner is not drawn for a green day, and neither is the sentence.
     */
    fun build(
        city: City,
        report: WeatherReport,
        now: Instant,
        updateFrequencyMin: Int,
        userRefreshing: Boolean,
        error: TodayError?,
        warnings: PlaceWarnings? = null,
        /** Yesterday's high as the history last wrote it ([yesterdayHigh]), for the
         * quiet day's sentence; null where the history has no yesterday. */
        yesterdayHighC: Double? = null,
        /** The reader's rules, for the rest of the day's rows (15 ott 2026). Empty when the
         * reader has switched their own alerts off. */
        rules: List<NotificationRule> = emptyList(),
        /**
         * Whether to read the quarters at all (17 ott 2026). The page does; a widget must not
         * (`WidgetData` passes false): a card repainted once a sync cannot keep a sentence
         * said in minutes, and «La pioggia smette tra circa 45 minuti» stood on a home screen
         * long after it stopped being true. Off, the state has no section and the sentence is
         * the hourly ladder's, as it was before the quarters.
         */
        readQuarters: Boolean = true
    ): TodayUiState {
        val zone = placeZone(report, city)
        if (!WeatherRecency.coversNow(report, now)) {
            return TodayUiState.Empty(city, userRefreshing, error)
        }
        // The forecast for now stands in for a `current` block over an hour old (24 set
        // 2026, `CurrentEstimate`), before the trim so the rows around now are still there
        // to interpolate. The widgets build through here too, and inherit it.
        val trimmed = WeatherRecency.trim(CurrentEstimate.apply(report, now), now)
        val local = LocalDateTime.ofInstant(now, zone)
        val coords = report.location.coordinates
        val sunAltitude = AstronomyEngine.sunAltitude(now, coords)
        val today = AstronomyEngine.solarDay(local.toLocalDate(), zone, coords)
        val currentHour = trimmed.hourly.first()
        val moon = AstronomyEngine.moonIllumination(now)
        val graded = warnings?.takeIf { it.maxLevel != WarningLevel.NONE }
        // The quarters ahead (17 ott 2026): one reading for the sentence and the section.
        val nowcast = if (readQuarters) NowcastEngine.read(trimmed, now) else null

        return TodayUiState.Content(
            city = city,
            report = trimmed,
            zone = zone,
            now = local,
            night = sunAltitude < 0.0,
            sky = SkySnapshot(
                sunAltitudeDeg = sunAltitude,
                cloudPct = currentHour.cloudCoverPct,
                // A tint, never a printed number: an hour with no chance draws the
                // sky undimmed, the same "not told is not wet" the verdicts apply.
                precipPct = currentHour.precipChancePct ?: 0,
                moonIllumination = moon.illuminatedFraction,
                moonAltitudeDeg = AstronomyEngine.moonAltitude(now, coords),
                phases = DaylightPhases.phases(today, zone),
                nowFraction = DaylightPhases.fraction(local),
                sunAzimuthDeg = AstronomyEngine.sunAzimuth(now, coords),
                moonAzimuthDeg = AstronomyEngine.bodyAzimuth(
                    com.callbackdev.chiaro.domain.sky.SkyBody.MOON, now, coords
                ),
                moonElongationDeg = moon.elongation,
                southern = coords.lat < 0.0,
                hours = DaylightPhases.sky(trimmed.hourly, local.toLocalDate()),
                layers = trimmed.current.cloudLayers,
                fall = fallOf(trimmed.current.condition.wmoCode),
                // The quarter hour just measured, as a rate: the hourly series carries no amounts.
                fallMmPerHour = trimmed.current.precipitation.lastQuarterHourMm * 4.0,
                windKph = trimmed.current.wind.speedKph,
                windFromDeg = trimmed.current.wind.degree
            ),
            // The ladder first; on a day it has nothing to warn about, the quiet day's own
            // sentence (12 ott 2026) — still the model's facts, never a filler.
            headline = HeadlineEngine.headline(
                trimmed, local, graded,
                nextRain = HeadlineEngine.nextRain(nowcast, zone, trimmed.current.condition.wmoCode)
            ) ?: HeadlineEngine.calm(trimmed, local, yesterdayHighC),
            strip = trimmed.hourly.drop(1).take(STRIP_HOURS).map { stripHour(it, zone, coords) },
            timeline = timeline(
                trimmed, today, zone, local,
                dataAge = Duration.between(report.systemInfo.lastSync, now),
                staleAfter = WeatherFreshness.staleAfter(updateFrequencyMin),
                rules = rules
            ),
            week = week(trimmed, zone, coords),
            lastSync = report.systemInfo.lastSync,
            isStale = WeatherFreshness.isStale(report.systemInfo.lastSync, updateFrequencyMin, now),
            userRefreshing = userRefreshing,
            error = error,
            warnings = graded,
            nowcast = nowcast?.takeIf { it.wet }
        )
    }

    /** What falls now, by the hour's state (the state engine's, not the provider's code):
     * snow when its phase is frozen, rain for any other falling phase. */
    internal fun fallOf(wmoCode: Int): com.callbackdev.chiaro.ui.components.SkyFall {
        if (!com.callbackdev.chiaro.domain.WmoCode.isPrecipitation(wmoCode)) return com.callbackdev.chiaro.ui.components.SkyFall.NONE
        return if (com.callbackdev.chiaro.domain.WmoCode.of(wmoCode)?.phase == com.callbackdev.chiaro.domain.WmoCode.Phase.FROZEN) {
            com.callbackdev.chiaro.ui.components.SkyFall.SNOW
        } else {
            com.callbackdev.chiaro.ui.components.SkyFall.RAIN
        }
    }

    /**
     * The merged rest of the day (VISION §5.2.4): today's remaining sun moments, the hours
     * where rain becomes likely or stops being, and the reader's own rules that hold today.
     * The cut at midnight of [agenda], plus three things the day's list does and the widget's
     * does not (15 ott 2026):
     *
     * - **a verdict on every sight** ([judged]): «will the sky allow it», the product's first
     *   promise, was the Sky tab's alone, and Today listed a golden hour with no answer;
     * - **the reader's rules** ([ruleRows]), which this list had promised since its first
     *   draft and never drawn;
     * - **the sunset folded into the golden hour** that ends with it ([foldSunset]): «Ora d'oro,
     *   until 19:15» over «Tramonto 19:15» said one time twice.
     */
    private fun timeline(
        report: WeatherReport,
        today: com.callbackdev.chiaro.domain.sky.SolarDay,
        zone: ZoneId,
        now: LocalDateTime,
        dataAge: Duration,
        staleAfter: Duration,
        rules: List<NotificationRule>
    ): List<TimelineItem> {
        val sky = agenda(report, zone, now, until = now.toLocalDate().atTime(LocalTime.MAX), today = today)
            // The moon stays on the Sky tab (committente, 13 ott 2026): on a dry day
            // Today's agenda was the Sky tab again, led by a moonrise nobody plans an
            // afternoon by. The day's arc widget keeps the whole agenda, moon included.
            .filterNot { it.kind == TimelineKind.MOONRISE || it.kind == TimelineKind.MOONSET }
        return (foldSunset(sky).map { judged(it, report, zone, dataAge, staleAfter) } + ruleRows(rules, report, zone, now))
            .sortedWith(compareBy<TimelineItem>({ !it.now }, { it.at }))
    }

    /**
     * The evening's golden hour runs until sunset, and its row says so («until sunset at
     * 19:15»): the sunset's own row, at that same minute, is folded into it. Only when the
     * golden hour is still ahead; once it has begun its row is gone and the sunset keeps its own.
     */
    internal fun foldSunset(items: List<TimelineItem>): List<TimelineItem> {
        val golden = items.firstOrNull { it.kind == TimelineKind.GOLDEN_EVENING } ?: return items
        return items.filterNot { it.kind == TimelineKind.SUNSET && it.at == golden.until }
    }

    /**
     * A sight's verdict, from [SkyVerdictEngine] on the job's own occurrence that day — the
     * window the Sky tab judges, not the row's minute — so the two tabs give one answer.
     */
    private fun judged(
        item: TimelineItem,
        report: WeatherReport,
        zone: ZoneId,
        dataAge: Duration,
        staleAfter: Duration
    ): TimelineItem {
        val job = item.kind.skyJob?.takeIf { it.observable } ?: return item
        val coords = report.location.coordinates
        val at = when (val occurrence = SkyScheduler.resolve(job, item.at.toLocalDate(), zone, coords)) {
            is SkyOccurrence.At -> occurrence
            is SkyOccurrence.None -> return item.copy(skipped = occurrence.reason)
        }
        return item.copy(
            verdict = SkyVerdictEngine.evaluate(
                job = job,
                start = at.start,
                end = at.end,
                hours = report.hourly,
                zone = zone,
                coordinates = coords,
                dataAge = dataAge,
                staleAfter = staleAfter
            )
        )
    }

    /**
     * The reader's rules that hold on this report, as rows of the day (15 ott 2026). What the
     * row says is what the rule's card on Alerts says — [RuleEngine.check], no latch, no
     * fingerprint — so it is a reading of the forecast in the reader's own words, never a
     * promise that the phone will ring: whether it already has today is the job's to know.
     *
     * When: the hour the rule's subject points at, if it is still ahead today (the rain at
     * 17:00 a «rain in the next 6 hours» rule found); no earlier than its window opens, if its
     * window is closed now; and «now» for a rule about this moment. A rule whose window does
     * not open again today is not a row of today.
     */
    internal fun ruleRows(
        rules: List<NotificationRule>,
        report: WeatherReport,
        zone: ZoneId,
        now: LocalDateTime
    ): List<TimelineItem> {
        val coords = report.location.coordinates
        val endOfDay = now.toLocalDate().atTime(LocalTime.MAX)
        return rules.filter { it.enabled }.mapNotNull { rule ->
            val fires = RuleEngine.check(rule, report, now) as? RuleCheck.Fires ?: return@mapNotNull null
            val open = RuleWindows.isOpen(rule.window, now, zone, coords)
            val opening = if (open) {
                null
            } else {
                RuleWindows.nextOpening(rule.window, now, zone, coords)
                    ?.takeIf { !it.isAfter(endOfDay) }
                    ?: return@mapNotNull null
            }
            val ahead = fires.at?.takeIf { it.isAfter(now) && !it.isAfter(endOfDay) }
            val at = listOfNotNull(ahead, opening).maxOrNull()
            TimelineItem(
                at = at ?: now,
                kind = TimelineKind.ALERT,
                rule = AgendaRule(rule, fires.value, fires.at),
                now = at == null
            )
        }
    }

    /**
     * The merged agenda of everything still ahead between [now] and [until]: the sun's
     * moments and the moon's of every date the window touches, the rainbow windows, and
     * the hours where the rain chance crosses half either way. Public and windowed since
     * the day's arc widget (9 set 2026), which lists the next twenty-four hours rather than
     * the rest of today — the widget and the screen printed two different sunrises once
     * before, and the cure was one rule in one place. [today] is the solar day the caller
     * has already computed for [now]'s date, if it has; the others are computed here.
     */
    fun agenda(
        report: WeatherReport,
        zone: ZoneId,
        now: LocalDateTime,
        until: LocalDateTime,
        today: com.callbackdev.chiaro.domain.sky.SolarDay? = null
    ): List<TimelineItem> {
        val coords = report.location.coordinates
        val items = mutableListOf<TimelineItem>()
        fun sun(at: Instant?, kind: TimelineKind, until: Instant? = null) {
            at?.let {
                items += TimelineItem(
                    LocalDateTime.ofInstant(it, zone),
                    kind,
                    until = until?.let { end -> LocalDateTime.ofInstant(end, zone) }
                )
            }
        }
        val firstDate = now.toLocalDate()
        val lastDate = until.toLocalDate()
        var date = firstDate
        while (!date.isAfter(lastDate)) {
            val solar = if (date == firstDate && today != null) {
                today
            } else {
                AstronomyEngine.solarDay(date, zone, coords)
            }
            sun(solar.sunrise, TimelineKind.SUNRISE)
            sun(solar.goldenHourMorningEnd, TimelineKind.GOLDEN_MORNING_END)
            // The two evening hours are spans, and the agenda printed them as moments
            // (review of 12 ott 2026): «Ora d'oro 18:36» left the reader to find out
            // that it lasts until sunset.
            sun(solar.goldenHourEveningStart, TimelineKind.GOLDEN_EVENING, until = solar.sunset)
            sun(solar.sunset, TimelineKind.SUNSET)
            sun(solar.blueHourEveningStart, TimelineKind.BLUE_EVENING, until = solar.blueHourEveningEnd)
            sun(solar.astronomicalDusk, TimelineKind.DARK)

            val lunar = AstronomyEngine.lunarDay(date, zone, coords)
            sun(lunar.moonrise, TimelineKind.MOONRISE)
            sun(lunar.moonset, TimelineKind.MOONSET)
            date = date.plusDays(1)
        }

        // The rainbow windows of the window: geometry (the sun under 42°) and weather
        // (rain likely, sky not shut) at the same time. A possibility, printed with the
        // number it rests on — never a promise (RainbowWindow's own rule).
        com.callbackdev.chiaro.domain.sky.RainbowWindow
            .windows(report.hourly, coords)
            .forEach { rainbow ->
                items += TimelineItem(
                    at = LocalDateTime.ofInstant(rainbow.start, zone),
                    kind = TimelineKind.RAINBOW,
                    pct = rainbow.precipChancePct,
                    bearingDeg = rainbow.lookTowardsDeg,
                    // A window, so the row says until when, as the golden hour's does, and
                    // its page knows whether the bow is now or still ahead.
                    until = LocalDateTime.ofInstant(rainbow.end, zone)
                )
            }

        // Rain turns: the hours where the chance crosses half, up or down.
        val hours = report.hourly.filter {
            val day = it.time.toLocalDate()
            !day.isBefore(firstDate) && !day.isAfter(lastDate)
        }
        hours.zipWithNext().forEach { (a, b) ->
            // A turn needs two hours that both said something: an hour with no
            // forecast chance cannot start the rain and cannot stop it (Fase 26).
            val from = a.precipChancePct ?: return@forEach
            val to = b.precipChancePct ?: return@forEach
            if (from < RAIN_TURN_PCT && to >= RAIN_TURN_PCT) {
                items += TimelineItem(b.time, TimelineKind.RAIN_START, to)
            }
            if (from >= RAIN_TURN_PCT && to < RAIN_TURN_PCT) {
                items += TimelineItem(b.time, TimelineKind.RAIN_STOP)
            }
        }

        return items
            .filter { it.at.isAfter(now) && !it.at.isAfter(until) }
            .sortedBy { it.at }
    }

    /**
     * Yesterday's high, read off the history (13 ott 2026): the newest commit written on
     * yesterday's date in the place's zone, and the `<date>.high_c` it flattened for that
     * date — the forecast for yesterday as it stood at its last fetch, which by then is as
     * close to the day as the app ever gets. No network and no new field: every fetch has
     * been writing it since today joined the horizon (8 set 2026). Null when no commit of
     * yesterday's is there (a new install, a phone that never fetched), or it carries no
     * high for the date. [commits] are in any order; [forecast] is called once, on the
     * commit chosen.
     */
    fun <T> yesterdayHigh(
        commits: List<T>,
        at: (T) -> Instant,
        forecast: (T) -> Map<String, String>,
        today: java.time.LocalDate,
        zone: ZoneId
    ): Double? {
        val yesterday = today.minusDays(1)
        // The date first, the JSON last: one commit is decoded, not the hundred.
        return commits
            .filter { at(it).atZone(zone).toLocalDate() == yesterday }
            .maxByOrNull { at(it) }
            ?.let(forecast)
            ?.get("$yesterday.high_c")
            ?.toDoubleOrNull()
    }

    private fun stripHour(
        hour: HourlyForecast,
        zone: ZoneId,
        coords: com.callbackdev.chiaro.domain.model.Coordinates
    ): StripHour = StripHour(
        hour = hour,
        // The hour's own instant, not its label put back through the zone: on the day
        // a zone falls back the label is ambiguous and `atZone` has to pick one of the
        // two offsets, which is a guess the mapper already answered exactly.
        night = AstronomyEngine.sunAltitude(hour.at, coords) < 0.0
    )

    /** The week's rows, each with its light, its hours for the tap-to-expand and its sky. */
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
                .map { stripHour(it, zone, coords) },
            sky = DaylightPhases.sky(report.hourly, day.date)
        )
    }
}
