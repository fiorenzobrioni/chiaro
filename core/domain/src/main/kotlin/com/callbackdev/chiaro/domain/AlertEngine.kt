package com.callbackdev.chiaro.domain

import com.callbackdev.chiaro.domain.settings.NotificationSettings
import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.model.WeatherCondition
import com.callbackdev.chiaro.domain.model.WeatherReport
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

enum class AlertKind { SEVERE, PRECIPITATION, DAILY_SUMMARY, EVENING_SUMMARY }

/**
 * One notification-worthy finding. Domain data stays canonical English; the
 * notifier localizes the chrome (title) at render time, per the l10n rule.
 */
data class Alert(
    val kind: AlertKind,
    /** Dedup key persisted after a successful notify (see [AlertState]). */
    val fingerprint: String,
    val cityLabel: String,
    val condition: WeatherCondition? = null,
    /** Triggering hour, in the city's timezone (SEVERE, PRECIPITATION). */
    val at: LocalDateTime? = null,
    val precipPct: Int? = null,
    val highC: Double? = null,
    val lowC: Double? = null,
    /**
     * The DAY the summaries are about, in the city's calendar: today for
     * [AlertKind.DAILY_SUMMARY], tomorrow for [AlertKind.EVENING_SUMMARY]. The
     * notifier needs it and must not derive it from the device clock, which can be
     * a day off the city's own (a place east of the date line, a phone still on
     * holiday time): the numbers above were read off that day and no other.
     */
    val forDate: LocalDate? = null
)

/**
 * Recently notified fingerprints — what keeps hourly polling from re-notifying.
 * Severe and precipitation are per-kind sets rather than single slots: their
 * fingerprints embed the city, so a single slot would be clobbered every time the
 * evaluated city changes (alternating saved cities), re-notifying events already
 * notified. The two summaries are one per date across all cities, so a bare date
 * is enough — and they keep SEPARATE slots: the morning one has already burned
 * today's date by the time the evening one is due, and sharing a slot would mean
 * one summary a day, whichever came first.
 */
data class AlertState(
    val severeFingerprints: Set<String> = emptySet(),
    val precipFingerprints: Set<String> = emptySet(),
    val summaryDate: LocalDate? = null,
    val eveningDate: LocalDate? = null
)

/**
 * Pure alert evaluation: no clocks, no Android, no I/O — everything injected so
 * the rules are table-testable. `now` must be in the report's local timezone
 * (the worker derives it from `report.location.timezone`, never the device zone).
 */
object AlertEngine {

    /**
     * Hazard classes for severe weather. The bucket doubles as the fingerprint
     * component: a 95→96 evolution is the same storm, THUNDER→SNOW is news.
     * Keyed on the WMO code — descriptions collapse distinct codes.
     */
    enum class SevereBucket { THUNDER, ICE, RAIN, SNOW }

    /** The hazard codes and their classes — read off [WmoCode], which owns the table. */
    val SevereCodes: Map<Int, SevereBucket> =
        WmoCode.entries.mapNotNull { code -> code.hazard?.let { code.code to it } }.toMap()

    /** Long enough to warn before an evening storm seen at a morning poll. */
    const val SEVERE_LOOKAHEAD_HOURS = 12L

    /**
     * The chance of rain under which a storm or downpour code is not «Maltempo»
     * (24 set 2026). The code comes from one model run; the chance from the ensemble,
     * and when they disagree the ensemble is the one that verifies. Measured over 31
     * places and 60 days (21 Jul - 18 Sep 2026, 148 severe runs checked against ERA5
     * rain within two hours of the run): runs whose highest chance was under 10% saw
     * 1 mm of rain 24% of the time, 10-19% saw it 47%, 20-29% saw it 75%, 30% and over
     * 96%. Heavy rain (5 mm) followed 6% of the runs under 20%, against 3% for any five
     * hours at all. The floor drops 36 of 148 banners (24%) and misses 2 of the 68
     * runs that brought 5 mm.
     *
     * Only for [SevereBucket.THUNDER] and [SevereBucket.RAIN], the two buckets the
     * measure saw (131 and 17 runs); a summer window has no ice or snow to measure,
     * and freezing drizzle is dangerous in amounts an ensemble may barely count. An
     * hour with no chance at all (model-dependent, §1.1) keeps its code: a missing
     * number is not evidence that the storm will not come.
     */
    const val SEVERE_MIN_CHANCE_PCT = 20

    /**
     * The bucket of a severe hour, or null when the hour is not one: the single
     * definition of «Maltempo» that the alert, the Today headline, the notification's
     * window and the rules' `wmo_severe` all read, so none of them can call a storm
     * the others dropped.
     */
    fun severeBucket(hour: HourlyForecast): SevereBucket? =
        severeBucket(hour.condition.wmoCode, hour.precipChancePct)

    /**
     * The same judgement on a bare code and chance, for the one reader that has no
     * [HourlyForecast] yet: the mapper, deciding whether a hazard hour may label its
     * day (24 set 2026). Until then the day's row claimed «Temporale» for every storm
     * code this function had already dropped as improbable, so the week said storm
     * where the banner, the headline and the rules all said nothing.
     */
    fun severeBucket(wmoCode: Int, precipChancePct: Int?): SevereBucket? {
        val bucket = SevereCodes[wmoCode] ?: return null
        val probable = precipChancePct == null || precipChancePct >= SEVERE_MIN_CHANCE_PCT ||
            bucket == SevereBucket.ICE || bucket == SevereBucket.SNOW
        return bucket.takeIf { probable }
    }

    fun isSevere(hour: HourlyForecast): Boolean = severeBucket(hour) != null

    /** "Take the umbrella" horizon — actionable, not noise. */
    const val PRECIP_LOOKAHEAD_HOURS = 6L
    const val PRECIP_THRESHOLD_PCT = 70

    /** The 12:00 cap stops a "today's summary" from landing in the evening. */
    val SummaryWindowStart: LocalTime = LocalTime.of(6, 0)
    val SummaryWindowEnd: LocalTime = LocalTime.of(12, 0)

    /**
     * The evening summary's window. The 18:00 floor is where "this evening" starts
     * being true in the shortest days of the year; the 23:00 cap is what keeps the
     * word "domani" honest — past midnight it means the day after the one the
     * numbers were read for, and a summary that lands at 00:10 saying "tomorrow"
     * is off by a day. Five hours wide, so the periodic job lands inside it even
     * at the slowest polling interval the settings allow (120 min).
     */
    val EveningWindowStart: LocalTime = LocalTime.of(18, 0)
    val EveningWindowEnd: LocalTime = LocalTime.of(23, 0)

    fun evaluate(
        report: WeatherReport,
        settings: NotificationSettings,
        state: AlertState,
        now: LocalDateTime,
        cityKey: String
    ): List<Alert> = buildList {
        val severe = if (settings.severeWeatherAlerts) {
            findSevere(report, state, now, cityKey)
        } else {
            null
        }
        severe?.let(::add)
        // A severe alert already covers its own rain — don't notify twice. Not only on
        // the run that posts it (the rule until 23 set 2026): an hour later the storm's
        // fingerprint is burnt, `severe` is null, and the storm's own 90% was posting a
        // second notification, «Ombrello verso le 16», about the storm announced at 13.
        // So the rain is silenced whenever it IS the storm's rain — its first hour inside
        // a severe run, or an hour either side of one — told or not.
        if (settings.precipitationWarning && severe == null) {
            findPrecipitation(report, state, now, cityKey)
                ?.takeUnless { settings.severeWeatherAlerts && nearSevere(report, it.at) }
                ?.let(::add)
        }
        if (settings.dailySummary) {
            findDailySummary(report, state, now)?.let(::add)
        }
        if (settings.eveningSummary) {
            findEveningSummary(report, state, now)?.let(::add)
        }
    }

    /**
     * The first hour in `[now, now + lookahead]` that STARTS a run of [holds] — the
     * hour the weather arrives, not an hour of weather already under way (23 set 2026).
     *
     * Both hour-anchored alerts say «in arrivo», and until this rule they fired on the
     * first matching hour from now, whatever came before it. Three things followed from
     * that, all measured on the fixtures: rain already falling at 10 got «Ombrello verso
     * le 12» at noon, because noon opened a new half-day fingerprint; a storm running
     * past midnight was announced again at 00:30, because the hour after midnight has
     * tomorrow's date; and the first poll into a storm announced it as arriving. A run's
     * start is stable across polls, so both fingerprints now hang off an hour that does
     * not move as the clock does.
     *
     * A run survives one quiet hour ([RunGapHours]): «70, 65, 80» is one spell of rain
     * with a dip in it, and a forecast that flickers across the threshold for an hour
     * must not read as rain stopping and starting again.
     */
    private fun firstArrival(
        report: WeatherReport,
        now: LocalDateTime,
        lookaheadHours: Long,
        holds: (HourlyForecast) -> Boolean
    ): HourlyForecast? {
        val end = now.plusHours(lookaheadHours)
        val hours = report.hourly
        hours.forEachIndexed { index, hour ->
            if (hour.time.isBefore(now) || hour.time.isAfter(end) || !holds(hour)) return@forEachIndexed
            val continues = (1..RunGapHours + 1).any { back ->
                hours.getOrNull(index - back)?.let(holds) == true
            }
            if (!continues) return hour
        }
        return null
    }

    /** One quiet hour inside a run does not end it; two do. */
    private const val RunGapHours = 1

    /** Whether [at] falls inside a run of severe hours, or an hour either side of one. */
    private fun nearSevere(report: WeatherReport, at: LocalDateTime?): Boolean {
        at ?: return false
        return report.hourly.any { hour ->
            isSevere(hour) &&
                !hour.time.isBefore(at.minusHours(1)) && !hour.time.isAfter(at.plusHours(1))
        }
    }

    private fun findSevere(
        report: WeatherReport,
        state: AlertState,
        now: LocalDateTime,
        cityKey: String
    ): Alert? {
        val hit = firstArrival(report, now, SEVERE_LOOKAHEAD_HOURS, ::isSevere) ?: return null
        val bucket = severeBucket(hit) ?: return null
        val fingerprint = "$cityKey:sev:${bucket.name}:${hit.time.toLocalDate()}"
        if (fingerprint in state.severeFingerprints) return null
        return Alert(
            kind = AlertKind.SEVERE,
            fingerprint = fingerprint,
            cityLabel = report.location.city,
            condition = hit.condition,
            at = hit.time,
            precipPct = hit.precipChancePct
        )
    }

    private fun findPrecipitation(
        report: WeatherReport,
        state: AlertState,
        now: LocalDateTime,
        cityKey: String
    ): Alert? {
        // A chance the provider did not forecast does not meet a threshold: "we were
        // not told" has never been a reason to warn (Fase 26).
        val hit = firstArrival(report, now, PRECIP_LOOKAHEAD_HOURS) {
            (it.precipChancePct ?: 0) >= PRECIP_THRESHOLD_PCT
        } ?: return null
        // Half-day bucket of the spell's START: at most two rain warnings per day per
        // city, and never two for one spell.
        val halfDay = if (hit.time.hour < 12) "AM" else "PM"
        val fingerprint = "$cityKey:pre:${hit.time.toLocalDate()}:$halfDay"
        if (fingerprint in state.precipFingerprints) return null
        return Alert(
            kind = AlertKind.PRECIPITATION,
            fingerprint = fingerprint,
            cityLabel = report.location.city,
            condition = hit.condition,
            at = hit.time,
            precipPct = hit.precipChancePct
        )
    }

    private fun findDailySummary(
        report: WeatherReport,
        state: AlertState,
        now: LocalDateTime
    ): Alert? {
        val time = now.toLocalTime()
        if (time < SummaryWindowStart || time > SummaryWindowEnd) return null
        if (state.summaryDate == now.toLocalDate()) return null
        // By date, like the evening's twin (23 set 2026): the first row of a report is
        // today only while the report is today's, and the numbers must be the day's
        // the summary names.
        val today = report.daily.firstOrNull { it.date == now.toLocalDate() } ?: return null
        return Alert(
            kind = AlertKind.DAILY_SUMMARY,
            // ISO date: AlertStateStore stores it straight into summaryDate
            fingerprint = now.toLocalDate().toString(),
            cityLabel = report.location.city,
            condition = today.condition,
            precipPct = today.precipPct,
            highC = today.highC,
            lowC = today.lowC,
            forDate = today.date
        )
    }

    /**
     * The evening's twin of [findDailySummary], and its subject is TOMORROW: at
     * 20:00 the day is over and the only summary left worth sending is the one you
     * can still act on — the alarm, the coat by the door, the umbrella. The night
     * itself rides along in the expanded notification, where the notifier reads it
     * off the same hours this report carries.
     *
     * It does not fire when the report has no tomorrow in it. That is not a defensive
     * null check: a cached report can outlive its own week, and an evening summary
     * whose entire collapsed sentence would be dashes is the screen lying (DESIGN
     * §1.1) in the one place the reader cannot check it.
     */
    private fun findEveningSummary(
        report: WeatherReport,
        state: AlertState,
        now: LocalDateTime
    ): Alert? {
        val time = now.toLocalTime()
        if (time < EveningWindowStart || time > EveningWindowEnd) return null
        if (state.eveningDate == now.toLocalDate()) return null
        val tomorrowDate = now.toLocalDate().plusDays(1)
        val tomorrow = report.daily.firstOrNull { it.date == tomorrowDate } ?: return null
        return Alert(
            kind = AlertKind.EVENING_SUMMARY,
            // The ISO date of the EVENING, not of the day described: the dedup slot
            // answers "has tonight's summary gone out", and tonight is today.
            fingerprint = now.toLocalDate().toString(),
            cityLabel = report.location.city,
            condition = tomorrow.condition,
            precipPct = tomorrow.precipPct,
            highC = tomorrow.highC,
            lowC = tomorrow.lowC,
            forDate = tomorrow.date
        )
    }
}
