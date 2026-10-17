package com.callbackdev.chiaro.ui.today

import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.model.DailyForecast
import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.model.WeatherCondition
import com.callbackdev.chiaro.domain.sample.sampleWeatherReport
import com.callbackdev.chiaro.domain.warnings.PlaceWarnings
import com.callbackdev.chiaro.domain.warnings.WarningHazard
import com.callbackdev.chiaro.domain.warnings.WarningLevel
import com.callbackdev.chiaro.domain.warnings.WarningZone
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayStateBuilderTest {

    private val zone = ZoneId.of("Europe/Rome")
    private val milan = City(
        id = 1L, name = "Milano", region = "Lombardia", country = "Italia",
        coordinates = Coordinates(45.4643, 9.1895), timezone = "Europe/Rome"
    )
    private val date: LocalDate = LocalDate.of(2026, 9, 2)
    private val clear = WeatherCondition(0)

    private fun report(fetchedAt: LocalDateTime, hours: Int = 48) =
        sampleWeatherReport().copy(
            location = sampleWeatherReport().location.copy(
                city = "Milano",
                coordinates = milan.coordinates,
                timezone = "Europe/Rome",
                localTime = fetchedAt
            ),
            hourly = (0 until hours).map {
                HourlyForecast(
                    time = fetchedAt.withMinute(0).plusHours(it.toLong()),
                    at = fetchedAt.withMinute(0).plusHours(it.toLong()).atZone(zone).toInstant(),
                    tempC = 20.0,
                    condition = clear,
                    precipChancePct = 10,
                    cloudCoverPct = 20
                )
            },
            daily = (0 until 7).map {
                DailyForecast(date.plusDays(it.toLong()), 24.0, 14.0, clear, 10, 5)
            },
            systemInfo = sampleWeatherReport().systemInfo.copy(
                lastSync = fetchedAt.atZone(zone).toInstant()
            )
        )

    // -------------------------------------------------- the official warning (Fase 11)

    private fun warnings(level: WarningLevel) = PlaceWarnings(
        zone = WarningZone("Lomb-09", "Nodo Idraulico di Milano", "Lombardia"),
        bulletinId = "DPC_BULLETIN_2026_09_02_1",
        issuedAt = date.atTime(15, 46),
        days = listOf(
            PlaceWarnings.DayWarnings(
                date,
                mapOf(
                    WarningHazard.HYDRAULIC to WarningLevel.NONE,
                    WarningHazard.HYDROGEOLOGICAL to WarningLevel.NONE,
                    WarningHazard.THUNDERSTORM to level
                )
            )
        ),
        note = null
    )

    private fun content(warnings: PlaceWarnings?): TodayUiState.Content {
        val fetched = LocalDateTime.of(2026, 9, 2, 12, 0)
        val now = LocalDateTime.of(2026, 9, 2, 12, 30).atZone(zone).toInstant()
        return TodayStateBuilder.build(milan, report(fetched), now, 60, false, null, warnings)
            as TodayUiState.Content
    }

    /** A green bulletin is an answer in Avvisi and NOTHING on this screen: no banner,
     * no sentence, no card with a dash in it (DESIGN §1.1). */
    @Test
    fun `a bulletin with nothing above NONE draws no banner and no sentence`() {
        val built = content(warnings(WarningLevel.NONE))
        assertEquals(null, built.warnings)
        assertTrue(built.headline !is Headline.Official)
    }

    @Test
    fun `a yellow warning reaches the banner and not the sentence`() {
        val built = content(warnings(WarningLevel.YELLOW))
        assertEquals(WarningLevel.YELLOW, built.warnings?.maxLevel)
        assertTrue(built.headline !is Headline.Official)
    }

    @Test
    fun `an orange warning reaches both`() {
        val built = content(warnings(WarningLevel.ORANGE))
        assertEquals(WarningLevel.ORANGE, built.warnings?.maxLevel)
        assertEquals(WarningLevel.ORANGE, (built.headline as Headline.Official).level)
    }

    @Test
    fun `no bulletin at all leaves the screen exactly as it was`() {
        assertEquals(null, content(null).warnings)
    }

    @Test
    fun `a fresh report builds content with the strip starting at the next full hour`() {
        val fetched = LocalDateTime.of(2026, 9, 2, 12, 0)
        val now = LocalDateTime.of(2026, 9, 2, 12, 30).atZone(zone).toInstant()
        val state = TodayStateBuilder.build(milan, report(fetched), now, 60, false, null)
        val content = state as TodayUiState.Content
        assertEquals(TodayStateBuilder.STRIP_HOURS, content.strip.size)
        assertEquals(
            LocalDateTime.of(2026, 9, 2, 13, 0),
            content.strip.first().hour.time
        )
        assertTrue("noon is not night", !content.night)
        assertTrue("fresh data is not stale", !content.isStale)
        assertTrue("a clear day has sun events left at 12:30", content.timeline.isNotEmpty())
        assertEquals(7, content.week.size)
    }

    @Test
    fun `three quiet hours later the same report is content, aged and trimmed`() {
        val fetched = LocalDateTime.of(2026, 9, 2, 9, 0)
        val now = LocalDateTime.of(2026, 9, 2, 12, 10).atZone(zone).toInstant()
        val content =
            TodayStateBuilder.build(milan, report(fetched), now, 60, false, null) as TodayUiState.Content
        // hours 9, 10 and 11 have already happened and must not be in the report
        assertEquals(LocalDateTime.of(2026, 9, 2, 12, 0), content.report.hourly.first().time)
        assertTrue("3h > 2×60min: stale", content.isStale)
    }

    @Test
    fun `a report past its horizon is empty, not a lie about last week`() {
        val fetched = LocalDateTime.of(2026, 8, 24, 9, 0)
        val now = LocalDateTime.of(2026, 9, 2, 12, 0).atZone(zone).toInstant()
        val state = TodayStateBuilder.build(
            milan, report(fetched, hours = 24), now, 60, false, null
        )
        assertTrue(state is TodayUiState.Empty)
    }

    @Test
    fun `the timeline is only what is still ahead today`() {
        val fetched = LocalDateTime.of(2026, 9, 2, 12, 0)
        val now = LocalDateTime.of(2026, 9, 2, 23, 30).atZone(zone).toInstant()
        val content =
            TodayStateBuilder.build(milan, report(fetched), now, 60, false, null) as TodayUiState.Content
        val lateEvening = LocalDateTime.of(2026, 9, 2, 23, 30)
        content.timeline.forEach {
            assertTrue("${it.kind} at ${it.at} is not ahead", it.at.isAfter(lateEvening))
            assertEquals(date, it.at.toLocalDate())
        }
    }

    /** The arc widget's window (9 set 2026): the same rule, cut a day later, and the
     * timeline is exactly its first day. */
    @Test
    fun `the agenda reaches past midnight and the timeline is its first day`() {
        val fetched = LocalDateTime.of(2026, 9, 2, 12, 0)
        val nowLocal = LocalDateTime.of(2026, 9, 2, 21, 0)
        val report = report(fetched)
        val agenda = TodayStateBuilder.agenda(report, zone, nowLocal, nowLocal.plusHours(24))
        assertTrue(
            "tomorrow's sunrise is in the next twenty-four hours",
            agenda.any { it.kind == TimelineKind.SUNRISE && it.at.toLocalDate() == date.plusDays(1) }
        )
        assertTrue(agenda.all { it.at.isAfter(nowLocal) && !it.at.isAfter(nowLocal.plusHours(24)) })
        assertEquals(agenda.sortedBy { it.at }, agenda)
        val content = TodayStateBuilder.build(
            milan, report, nowLocal.atZone(zone).toInstant(), 60, false, null
        ) as TodayUiState.Content
        // Today's timeline is the agenda's first day without the moon, which stays on the
        // Sky tab and in the day's arc widget (13 ott 2026), and without the sunset the
        // golden hour now ends on (15 ott 2026); its rows carry their verdicts.
        val golden = agenda.firstOrNull { it.kind == TimelineKind.GOLDEN_EVENING }
        assertEquals(
            agenda.filter {
                it.at.toLocalDate() == date &&
                    it.kind != TimelineKind.MOONRISE && it.kind != TimelineKind.MOONSET &&
                    !(it.kind == TimelineKind.SUNSET && golden != null && it.at == golden.until)
            }.map { it.at to it.kind },
            content.timeline.map { it.at to it.kind }
        )
    }

    // ------------------------------------------- the ribbon's sky (DESIGN §4, 12 ott 2026)

    /** The report keeps no hour before the one we are in, so both of today's ribbons, the
     * canvas' and the row's, start their sky there, and every later day has its whole day
     * closed by the next midnight. */
    @Test
    fun `today's sky starts at the hour we are in and later days are whole`() {
        val built = content(null)
        assertEquals(12 / 24f, built.sky.hours.first().at, 1e-6f)
        assertEquals(12 / 24f, built.week.first().sky.first().at, 1e-6f)
        val tomorrow = built.week[1].sky
        assertEquals(0f, tomorrow.first().at, 0f)
        assertEquals(1f, tomorrow.last().at, 0f)
    }

    /** The evening's golden and blue hours are spans (review of 12 ott 2026): the golden
     * hour ends at the sunset the agenda lists, the blue hour after it begins. */
    @Test
    fun `the evening hours carry where they end`() {
        val nowLocal = LocalDateTime.of(2026, 9, 2, 12, 30)
        val agenda = TodayStateBuilder.agenda(report(LocalDateTime.of(2026, 9, 2, 12, 0)), zone, nowLocal, date.atTime(23, 59))
        val golden = agenda.single { it.kind == TimelineKind.GOLDEN_EVENING }
        val sunset = agenda.single { it.kind == TimelineKind.SUNSET }
        val blue = agenda.single { it.kind == TimelineKind.BLUE_EVENING }
        assertEquals(sunset.at, golden.until)
        assertTrue(blue.until!!.isAfter(blue.at))
        assertEquals(null, sunset.until)
    }

    // ---------------------------------------------- the day's verdicts and rules (15 ott 2026)

    /** «Ora d'oro, until 19:15» over «Tramonto 19:15» said one time twice: the sunset the
     * golden hour ends on is folded into it; the widget's agenda keeps both. */
    @Test
    fun `the sunset folds into the golden hour that ends on it`() {
        val timeline = content(null).timeline
        val golden = timeline.single { it.kind == TimelineKind.GOLDEN_EVENING }
        assertTrue(timeline.none { it.kind == TimelineKind.SUNSET && it.at == golden.until })
    }

    /** Once the golden hour has begun its row is gone, and the sunset keeps its own. */
    @Test
    fun `inside the golden hour the sunset is a row again`() {
        val fetched = LocalDateTime.of(2026, 9, 2, 12, 0)
        val agenda = TodayStateBuilder.agenda(report(fetched), zone, LocalDateTime.of(2026, 9, 2, 12, 30), date.atTime(23, 59))
        val golden = agenda.single { it.kind == TimelineKind.GOLDEN_EVENING }
        val inside = golden.at.plusMinutes(5)
        val timeline = (TodayStateBuilder.build(
            milan, report(fetched), inside.atZone(zone).toInstant(), 60, false, null
        ) as TodayUiState.Content).timeline
        assertTrue(timeline.any { it.kind == TimelineKind.SUNSET })
    }

    /** «Will the sky allow it» on Today is the Sky tab's answer: the same engine on the same
     * hours over the job's own window. A sky of 20% cloud and 10% rain is a clear golden hour. */
    @Test
    fun `a sight carries the Sky's verdict and a weather turn carries none`() {
        val timeline = content(null).timeline
        val golden = timeline.single { it.kind == TimelineKind.GOLDEN_EVENING }
        assertEquals(com.callbackdev.chiaro.domain.sky.SkyVerdictKind.PASS, golden.verdict?.kind)
        assertEquals(20, golden.verdict?.cloudPct)
        assertTrue(timeline.filter { it.kind.skyJob == null }.all { it.verdict == null })
    }

    /** Stale data does not get to hold an opinion, here as on the Sky tab. */
    @Test
    fun `a report too old to trust judges nothing`() {
        val fetched = LocalDateTime.of(2026, 9, 2, 9, 0)
        val now = LocalDateTime.of(2026, 9, 2, 12, 30).atZone(zone).toInstant()
        // Polled every hour: stale after two, and this one is three and a half.
        val timeline = (TodayStateBuilder.build(milan, report(fetched), now, 60, false, null)
            as TodayUiState.Content).timeline
        val golden = timeline.single { it.kind == TimelineKind.GOLDEN_EVENING }
        assertEquals(com.callbackdev.chiaro.domain.sky.SkyVerdictKind.UNKNOWN, golden.verdict?.kind)
    }

    private fun rule(
        id: Long,
        variable: String,
        op: com.callbackdev.chiaro.domain.rules.RuleOp,
        threshold: Double,
        window: com.callbackdev.chiaro.domain.rules.RuleWindow = com.callbackdev.chiaro.domain.rules.RuleWindow.Always,
        enabled: Boolean = true
    ) = com.callbackdev.chiaro.domain.rules.NotificationRule(
        id = id,
        name = "rule $id",
        enabled = enabled,
        conditions = listOf(com.callbackdev.chiaro.domain.rules.RuleCondition(variable, op, threshold)),
        message = "msg {trigger.value}",
        window = window
    )

    private fun timelineWith(vararg rules: com.callbackdev.chiaro.domain.rules.NotificationRule): List<TimelineItem> {
        val fetched = LocalDateTime.of(2026, 9, 2, 12, 0)
        val now = LocalDateTime.of(2026, 9, 2, 12, 30).atZone(zone).toInstant()
        return (TodayStateBuilder.build(
            milan, report(fetched), now, 60, false, null, rules = rules.toList()
        ) as TodayUiState.Content).timeline
    }

    /** A rule about this moment holds now, and stands before every moment still ahead. */
    @Test
    fun `a rule that holds now is the first row, at now`() {
        val timeline = timelineWith(rule(1, "current.temp_c", com.callbackdev.chiaro.domain.rules.RuleOp.GT, 15.0))
        val first = timeline.first()
        assertEquals(TimelineKind.ALERT, first.kind)
        assertTrue(first.now)
        assertEquals(1L, first.rule?.rule?.id)
    }

    /** A rule whose hours open later today is a row at the hour it would speak. */
    @Test
    fun `a rule outside its hours is a row when they open`() {
        val evening = com.callbackdev.chiaro.domain.rules.RuleWindow(
            com.callbackdev.chiaro.domain.rules.RuleWindowKind.HOURS,
            listOf(com.callbackdev.chiaro.domain.rules.RuleBand(17 * 60, 19 * 60))
        )
        val row = timelineWith(rule(2, "current.temp_c", com.callbackdev.chiaro.domain.rules.RuleOp.GT, 15.0, evening))
            .single { it.kind == TimelineKind.ALERT }
        assertEquals(LocalDateTime.of(2026, 9, 2, 17, 0), row.at)
        assertTrue(!row.now)
    }

    /** No row for a rule that does not hold, one that is off, or one whose hours are over. */
    @Test
    fun `a rule that does not hold, is off, or is done for the day is no row`() {
        val morning = com.callbackdev.chiaro.domain.rules.RuleWindow(
            com.callbackdev.chiaro.domain.rules.RuleWindowKind.HOURS,
            listOf(com.callbackdev.chiaro.domain.rules.RuleBand(7 * 60, 9 * 60))
        )
        val timeline = timelineWith(
            rule(3, "current.temp_c", com.callbackdev.chiaro.domain.rules.RuleOp.LT, 0.0),
            rule(4, "current.temp_c", com.callbackdev.chiaro.domain.rules.RuleOp.GT, 15.0, enabled = false),
            rule(5, "current.temp_c", com.callbackdev.chiaro.domain.rules.RuleOp.GT, 15.0, morning)
        )
        assertTrue(timeline.none { it.kind == TimelineKind.ALERT })
    }

    // ----------------------------------------------- yesterday off the history (13 ott 2026)

    private fun commit(at: LocalDateTime, high: String?) =
        at.atZone(zone).toInstant() to (high?.let { mapOf("2026-09-01.high_c" to it) } ?: emptyMap())

    private fun yesterdayHigh(vararg commits: Pair<java.time.Instant, Map<String, String>>) =
        TodayStateBuilder.yesterdayHigh(commits.toList(), { it.first }, { it.second }, date, zone)

    @Test
    fun `yesterday's high is the newest of yesterday's commits`() {
        assertEquals(
            24.5,
            yesterdayHigh(
                commit(LocalDateTime.of(2026, 9, 1, 8, 0), "26.0"),
                commit(LocalDateTime.of(2026, 9, 1, 22, 0), "24.5"),
                // Today's commits are not yesterday's, whatever they say about it.
                commit(LocalDateTime.of(2026, 9, 2, 9, 0), "30.0")
            )!!,
            0.0
        )
    }

    @Test
    fun `no commit of yesterday's, or none with its high, is no yesterday`() {
        assertEquals(null, yesterdayHigh(commit(LocalDateTime.of(2026, 9, 2, 9, 0), "30.0")))
        assertEquals(null, yesterdayHigh(commit(LocalDateTime.of(2026, 9, 1, 22, 0), null)))
        assertEquals(null, yesterdayHigh())
    }

    @Test
    fun `Today's agenda has no moon`() {
        assertTrue(content(null).timeline.none {
            it.kind == TimelineKind.MOONRISE || it.kind == TimelineKind.MOONSET
        })
    }

    // -------------------------------------------------- the next two hours (17 ott 2026)

    private fun withQuarters(vararg mm: Double): TodayUiState.Content {
        val fetched = LocalDateTime.of(2026, 9, 2, 12, 0)
        val now = LocalDateTime.of(2026, 9, 2, 12, 5).atZone(zone).toInstant()
        val wet = report(fetched).let { r ->
            r.copy(
                // The ensemble backs the hours, so a measurable quarter is a wet one.
                hourly = r.hourly.map { it.copy(precipChancePct = 70) },
                nowcast = com.callbackdev.chiaro.domain.model.Nowcast(
                    mm.mapIndexed { i, v ->
                        com.callbackdev.chiaro.domain.model.QuarterHour(
                            fetched.atZone(zone).toInstant().plusSeconds(900L * i), v, 0.0
                        )
                    }
                )
            )
        }
        return TodayStateBuilder.build(milan, wet, now, 60, false, null) as TodayUiState.Content
    }

    @Test
    fun `rain in the quarters ahead is the section and the sentence`() {
        val content = withQuarters(0.0, 0.0, 0.3, 0.3, *DoubleArray(12))
        assertTrue(content.nowcast?.wet == true)
        assertTrue(content.headline is Headline.NextRain)
    }

    @Test
    fun `a dry window draws no section`() {
        val content = withQuarters(*DoubleArray(16))
        assertEquals(null, content.nowcast)
        assertTrue(content.headline !is Headline.NextRain)
    }
}
