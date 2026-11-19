package com.callbackdev.chiaro.data.mapper

import com.callbackdev.chiaro.data.remote.OpenMeteoForecastApi
import com.callbackdev.chiaro.data.remote.dto.AirQualityCurrentDto
import com.callbackdev.chiaro.data.remote.dto.AirQualityHourlyDto
import com.callbackdev.chiaro.data.remote.dto.CurrentDto
import com.callbackdev.chiaro.data.remote.dto.DailyDto
import com.callbackdev.chiaro.data.remote.dto.ForecastResponseDto
import com.callbackdev.chiaro.data.remote.dto.HourlyDto
import com.callbackdev.chiaro.data.remote.dto.Minutely15Dto
import com.callbackdev.chiaro.data.remote.dto.WitnessResponseDto
import com.callbackdev.chiaro.domain.AlertEngine
import com.callbackdev.chiaro.domain.CurrentEstimate
import com.callbackdev.chiaro.domain.DayPart
import com.callbackdev.chiaro.domain.NowcastCoverage
import com.callbackdev.chiaro.domain.PlaceRegion
import com.callbackdev.chiaro.domain.PossibleStorm
import com.callbackdev.chiaro.domain.WeatherCodes
import com.callbackdev.chiaro.domain.WeatherCodes.PollenSpecies
import com.callbackdev.chiaro.domain.WeatherStateEngine
import com.callbackdev.chiaro.domain.SnowLine
import com.callbackdev.chiaro.domain.WmoCode
import com.callbackdev.chiaro.domain.model.AirQuality
import com.callbackdev.chiaro.domain.model.AqiScale
import com.callbackdev.chiaro.domain.model.Astronomical
import com.callbackdev.chiaro.domain.model.CacheStatus
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.CloudLayers
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.model.CurrentConditions
import com.callbackdev.chiaro.domain.model.DailyForecast
import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.model.Location
import com.callbackdev.chiaro.domain.model.MoonPhase
import com.callbackdev.chiaro.domain.model.Nowcast
import com.callbackdev.chiaro.domain.model.PastHour
import com.callbackdev.chiaro.domain.model.PollenReport
import com.callbackdev.chiaro.domain.model.Precipitation
import com.callbackdev.chiaro.domain.model.QuarterHour
import com.callbackdev.chiaro.domain.model.SystemInfo
import com.callbackdev.chiaro.domain.model.WeatherCondition
import com.callbackdev.chiaro.domain.model.WeatherReport
import com.callbackdev.chiaro.domain.model.Wind
import com.callbackdev.chiaro.domain.sky.AstronomyEngine
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * Hourly slots carried in the domain: the CURRENT hour first — slot 0 feeds
 * `current_conditions`' rain chance and anchors AlertEngine/rules (Fase 11f: the
 * views drop it, it only repeats the current section) — and then every remaining
 * hour the response carries.
 *
 * `FORECAST_DAYS × 24` since upstream Fase 16a, 25 before it. The old number was one day, and
 * the other 143 hours of the response were deserialized into [HourlyDto], written to
 * `ReportDiskCache` as part of the raw DTO, and then dropped on the floor by this
 * function. So a sky verdict three days out is not a new capability that needs a new
 * request: it is data the app has been paying for and discarding. Widening costs no
 * network, no disk and no parsing — only the mapped objects, which is why the count
 * is bounded here rather than left as "whatever arrived".
 *
 * The realized count is never the full 168: the window opens at the current hour, so
 * it runs from 167 at midnight down to ~145 at 23:00 — one less than the slots since
 * 24 set 2026, because a row needs the slot after it (see [WeatherReportMapper.mapHourly]) —
 * and it is clipped to what the response actually holds.
 *
 * **The views do not follow this window.** `weather_data.json` caps its table at 24
 * rows and the README at 14; AlertEngine and RuleVariables bound themselves by time
 * (`next_6h`, `next_12h`, `now.plusHours(…)`) rather than by position. Anything new
 * reading `report.hourly` has to decide its own horizon — the list is a week now.
 */
private const val HOURLY_WINDOW = OpenMeteoForecastApi.FORECAST_DAYS * 24

/** Days of daily forecast carried — the whole response, like [HOURLY_WINDOW]. */
private const val DAILY_WINDOW = OpenMeteoForecastApi.FORECAST_DAYS

/** The `current` block's interval when a cached response does not say: 15 minutes, the
 * only value measured (26 cities, 25 set 2026). */
private const val CURRENT_INTERVAL_S = 900

/** The quarters of an hour in the provider's 15-minute series. */
private const val QUARTERS_PER_HOUR = 4L

/** The hours a day's mean pollen needs: half the day, or it is not the day's. */
private const val POLLEN_MIN_HOURS = 12

/**
 * How much has to fall before precipitation may label the whole day (Fase 26), and
 * the escape hatch for the day where little falls but it falls all afternoon.
 *
 * `dailyCode` used to let ANY hour with a code ≥ 51 claim the day. Measured 6 Sep
 * 2026 on 23 cities across five continents, 161 city-days: 45% of days came back
 * "wet", and 47% of those were wet only from drizzle codes — ten of them with under a
 * millimetre in twenty-four hours, five with a peak probability below 30%. The worst
 * was Singapore, one hour, 0.1 mm, **1% probability**, and the whole day printed
 * `Drizzle 🌦️` in the week table and in the morning summary; Milan the same with
 * `Rain Showers` for **0.0 mm**. That is the fog defect again in another column: one
 * unrepresentative hour labelling a day.
 *
 * One millimetre is the Met Office's own "wet day", and three hours is what keeps a
 * long soft drizzle — which really does look like a drizzly day — from being dropped
 * by an accumulation rule. On the same 161 days the pair moves 5 days out of 73, and
 * all five are the ones above: one or two hours, 0.0–0.4 mm. The weakest day it keeps
 * is six hours and 0.6 mm, which is a day you take a jacket for.
 */
private const val WET_DAY_MM = 1.0
private const val WET_DAY_HOURS = 3

/** Snow under this is not snow on any screen: the day's facts and the tile start at it. */
private const val TRACE_SNOW_CM = 0.1

/**
 * Open-Meteo's snowfall per millimetre of water: «7 cm snow = 10 mm precipitation water
 * equivalent» (its documentation), and its responses keep to it — 9.66 cm against 13.8 mm
 * on Longyearbyen's 2 Oct of the 25 Sep run, 19.81 cm against 28.4 mm on Everest. One
 * constant with the state engine's.
 */
private const val SNOW_CM_PER_MM = WeatherStateEngine.SNOW_CM_PER_MM

/**
 * The frame Open-Meteo's timestamps are written in, and the city's real clock beside it.
 *
 * With `timezone=auto` the provider does NOT apply [zone]'s rules hour by hour: it takes
 * the offset in force when the request lands and builds the whole series as
 * `UTC + that offset`, which it reports back as `utc_offset_seconds`. Every calendar day
 * of the response therefore holds exactly 24 values, including the day a zone springs
 * forward — `2026-10-04T02:00` is in an `Australia/Sydney` response although that hour
 * never happens there. Measured 20 set 2026 against the same request on `timezone=UTC`:
 * zero mismatches in 384 hours under the fixed offset, 53 of 69 under the real clock
 * after the change. See `ForecastResponseDto.utcOffsetSeconds` for the full reading.
 *
 * So the app had a whole week of rows hanging off an assumption that is true for 363
 * days a year. Around each change — the seven days a fetch's horizon reaches across it —
 * every hour after the transition sat one row late: the temperature under «08:00» was
 * 07:00's, the sky verdict for a 02:00 meteor peak read 03:00's cloud, `WeatherRecency`
 * kept an hour that was over. The app's own sunrise was right through all of it
 * ([AstronomyEngine] never saw the provider's times), which is what made the disagreement
 * visible on the screen rather than merely wrong underneath it.
 *
 * [offsetSeconds] null is a [com.callbackdev.chiaro.data.local.ReportDiskCache] entry
 * written before the field was read. It is answered with the old behaviour and not with
 * a guess: an offline phone keeps the forecast it has, at worst as accurate as yesterday.
 */
private class ProviderClock(timezone: String, offsetSeconds: Int?) {

    /** The city's real zone, DST rules and all. Falls back to the device's, like every
     * other reader of `location.timezone` in the app. */
    val zone: ZoneId = runCatching { ZoneId.of(timezone) }.getOrDefault(ZoneId.systemDefault())

    private val offset: ZoneOffset? = offsetSeconds
        ?.let { runCatching { ZoneOffset.ofTotalSeconds(it) }.getOrNull() }

    /** The moment a provider label names. Exact: the label plus the offset it was
     * written on, with no zone rule to consult and no ambiguity to resolve. */
    fun instantOf(providerLocal: LocalDateTime): Instant =
        offset?.let { providerLocal.toInstant(it) } ?: providerLocal.atZone(zone).toInstant()

    /**
     * That same moment on the city's clock — the value every surface prints.
     *
     * Identical to its input for all but the two days a year [zone] changes offset,
     * which is why this lands as a fix and not as a week of moved numbers.
     */
    fun localOf(providerLocal: LocalDateTime): LocalDateTime =
        offset?.let { LocalDateTime.ofInstant(providerLocal.toInstant(it), zone) } ?: providerLocal
}

object WeatherReportMapper {

    const val SOURCE = "Open-Meteo API"

    fun map(
        city: City,
        forecast: ForecastResponseDto,
        airQuality: AirQualityCurrentDto?,
        fetchedAt: Instant,
        cacheStatus: CacheStatus,
        /** The other model families that vote on the states; null, and best_match is alone. */
        witnesses: WitnessResponseDto? = null,
        /** Today's pollen by the hour; null, and the report has no pollen. */
        pollenHours: AirQualityHourlyDto? = null
    ): WeatherReport {
        val current = forecast.current
        val clock = ProviderClock(forecast.timezone, forecast.utcOffsetSeconds)

        // The provider's own labels, kept in the provider's own frame. Every
        // comparison BETWEEN response values happens here, where they all share one
        // offset and the arithmetic is exact; only the values that leave this function
        // are re-expressed on the city's clock.
        val providerNow = LocalDateTime.parse(current.time)
        val providerTimes = forecast.hourly.time.map(LocalDateTime::parse)
        // What every hour IS (25 set 2026): one state per row, from WeatherStateEngine,
        // and the same states re-indexed by slot for the day's label. Null where the
        // provider gave the hour nothing to read a state from (19 ott 2026).
        val voters = Witnesses.of(witnesses, forecast.hourly)
        // Each hour with what its series decides about it (the fog's persistence, the rain's
        // episode) written in once ([seriesOf]): the strip and the present read the same answer.
        val rows = seriesOf(forecast.hourly.rows(voters))
        val rowStates = statesOf(rows)
        val slotStates = statesOf(seriesOf(forecast.hourly.slots(voters)))
        // The chance of the hour under way: the slot that ENDS after now, since
        // `currentHourIndex`'s slot is the hour already gone. The stated chance
        // (`current.precipitation.chancePct`, an alert's variable).
        val chanceNow = providerTimes.indexOfFirst { it.isAfter(providerNow) }
            .takeIf { it >= 0 }
            ?.let { forecast.hourly.precipitationProbabilityPct.getOrNull(it) }
        // The chance the hero's state is read with (17 nov 2026): that of the hour the block's
        // own quarter falls in, the slot that ends AT or after the quarter's end — as every
        // quarter of the next two hours is read with its hour's (`NowcastEngine`). The two differ
        // in the first quarter of every hour only: at 13:00 the block is 12:45-13:00, an hour
        // at 38%, and was read with 13-14's 5% (Cavenago di Brianza, 17 Nov 2026, 13:12:
        // «Coperto» over 0.4 mm/h of drizzle, and drizzle on the spot).
        val blockHour = providerTimes.indexOfFirst { !it.isBefore(providerNow) }.takeIf { it >= 0 }
        val chanceOfBlock = blockHour?.let { forecast.hourly.precipitationProbabilityPct.getOrNull(it) }
        // ...and with that hour's amounts too, where the block's quarter is the hour cut in four
        // and the app reads no quarters at the place: with a nowcast the hero keeps its own
        // quarter, the one the section's sentences are written against.
        val nowcast = mapNowcast(forecast, clock, city)
        val amountsFrom = blockHour?.takeIf { nowcast == null && cutInFour(forecast.minutely15, providerTimes[it]) }
        val currentHour = providerNow.truncatedTo(ChronoUnit.HOURS)
        val currentHourIndex = providerTimes.indexOfFirst { !it.isBefore(currentHour) }
            .coerceAtLeast(0)
        val localTime = clock.localOf(providerNow)
        // The current block's fog is the series' (26 ott 2026): the row now falls in decides
        // whether fog may be invented, so the hero never draws a fog the strip below it does
        // not. The series is the models' votes (17 nov 2026): two of them seeing fog in that
        // row and in a neighbour. Read off the row as [seriesOf] wrote it, never counted again
        // here: until 19 nov 2026 the present counted it on the rows with their holes and the
        // strip without them, and across a missing hour the two could disagree.
        val nowIsARow = providerTimes.getOrNull(currentHourIndex) == currentHour
        val nowRow = if (nowIsARow) rows.getOrNull(currentHourIndex) else null
        val fogNow = nowRow?.fogPersists == true
        // ...and so are its witnesses: the row's, read at the row's instant.
        val nowVoters = if (nowIsARow) voters.at(forecast.hourly, instant = currentHourIndex) else emptyList()
        // ...and whether its rain is a shower (19 nov 2026): the row's CAPE and the row's episode,
        // so the hero never says «Rovesci» over a strip cell that says «Pioggia», or the reverse.
        val nowEpisode = nowRow?.episodeHours

        val hourly = mapHourly(forecast, providerTimes, clock, rowStates, currentHourIndex)
        // The block's own rain, which the rows below cannot replace: the quarter's amount
        // is measured nowhere else, and the hours that closed are the series'.
        val precipitation = Precipitation(
            lastQuarterHourMm = current.precipitationMm,
            pastHours = pastHours(forecast.hourly, providerTimes, clock, providerNow),
            chancePct = chanceNow,
            quarterEndedAt = clock.instantOf(providerNow)
        )

        return WeatherReport(
            location = Location(
                city = city.name,
                region = city.region,
                country = city.country,
                coordinates = Coordinates(forecast.latitude, forecast.longitude),
                timezone = forecast.timezone,
                localTime = localTime,
                elevationM = forecast.elevation
            ),
            // The provider's block when it is whole; with a hole in it (19 ott 2026) the
            // forecast for the block's own moment, from the rows, labelled as the estimate
            // it is — exactly what an hour of age does to a whole block (CurrentEstimate).
            // Rows that cannot stand in either (a cache entry from before they carried
            // the rest of an hour) leave nothing honest to show, and the report is not
            // built: that is the one failure this mapper still has, and it is the old one.
            current = wholeCurrent(
                current, forecast.hourly, amountsFrom, chanceOfBlock, fogNow, nowVoters, precipitation,
                rowCapeJkg = nowRow?.capeJkg, rowEpisodeHours = nowEpisode
            )
                ?: CurrentEstimate.estimate(hourly, precipitation, clock.instantOf(providerNow))
                ?: error("the current block has a hole in it and the hours cannot stand in"),
            airQuality = airQuality?.toAirQuality(PlaceRegion.inEurope(city.countryCode)),
            pollen = pollenHours?.toPollenReport(),
            astronomical = mapAstronomical(city, clock, localTime, fetchedAt),
            hourly = hourly,
            daily = mapDaily(forecast, providerTimes, slotStates, rowStates, clock),
            systemInfo = SystemInfo(
                source = SOURCE,
                lastSync = fetchedAt,
                cacheStatus = cacheStatus
            ),
            nowcast = nowcast
        )
    }

    /**
     * The quarters ahead (17 ott 2026), only where a model computes them
     * ([NowcastCoverage.covers]) and only when the series does not read as the hourly one cut
     * in four ([NowcastCoverage.looksInterpolated]). Each slot is the quarter that ENDS at its
     * label, like the hourly sums: the quarter starts fifteen minutes earlier. A slot with no
     * amount is left out, and the gap it leaves ends the window where it is
     * ([com.callbackdev.chiaro.domain.NowcastEngine] reads only quarters without a gap).
     */
    private fun mapNowcast(forecast: ForecastResponseDto, clock: ProviderClock, city: City): Nowcast? {
        val series = forecast.minutely15 ?: return null
        if (!NowcastCoverage.covers(city.countryCode, city.coordinates)) return null
        val quarters = series.time.indices.mapNotNull { i ->
            val mm = series.precipitationMm.getOrNull(i) ?: return@mapNotNull null
            QuarterHour(
                start = clock.instantOf(LocalDateTime.parse(series.time[i])).minus(QuarterHour.LENGTH),
                precipMm = mm,
                snowCm = series.snowfallCm.getOrNull(i)
            )
        }
        if (quarters.isEmpty() || NowcastCoverage.looksInterpolated(quarters)) return null
        return Nowcast(quarters)
    }

    /**
     * Whether the quarters of the hour that ends at [hourEnd] are that hour cut in four: all
     * four in the response, and all equal (17 nov 2026, `docs/STATES.md` §5). Where no model
     * computes the quarter Open-Meteo serves the hour's amount spread evenly and rounded to
     * 0.1 mm, so an hour of 0.1 mm is four quarters of 0.0 — 22% of the wet hours measured —
     * and the `current` block, which is one of them, read «dry» over a cell that said drizzle.
     *
     * Read off the series itself, like [NowcastCoverage.looksInterpolated]: a model that
     * computes the quarter gives a wet hour a shape, and a flat hour a real model drew reads
     * the same from either number. Without all four quarters (a cache entry from before
     * [OpenMeteoForecastApi.PAST_QUARTERS], a hole in the series) it is not known, and the
     * block keeps its own amount.
     */
    private fun cutInFour(series: Minutely15Dto?, hourEnd: LocalDateTime): Boolean {
        series ?: return false
        val times = series.time.map(LocalDateTime::parse)
        val quarters = (0L until QUARTERS_PER_HOUR).map { back ->
            val i = times.indexOf(hourEnd.minusMinutes(QuarterHour.LENGTH.toMinutes() * back))
            series.precipitationMm.getOrNull(i) ?: return false
        }
        return quarters.distinct().size == 1
    }

    /**
     * The provider's `current` block as the conditions now, or null when it has a hole in
     * it (19 ott 2026): a value the block cannot do without — the temperature, the
     * apparent one, humidity, dew point, pressure, the wind's three — or nothing to read a
     * state from. Visibility, UV, the cloud and its layers are nullable on the other side
     * and travel as they are.
     */
    private fun wholeCurrent(
        current: CurrentDto,
        hourly: HourlyDto,
        amountsFrom: Int?,
        chance: Int?,
        fogPersists: Boolean,
        witnesses: List<WeatherStateEngine.Witness>,
        precipitation: Precipitation,
        rowCapeJkg: Double?,
        rowEpisodeHours: Int?
    ): CurrentConditions? {
        val state = currentState(
            current, hourly, amountsFrom, chance, fogPersists, witnesses, rowCapeJkg, rowEpisodeHours
        ) ?: return null
        val windDirection = current.windDirectionDeg ?: return null
        return CurrentConditions(
            condition = WeatherCondition(state),
            tempC = current.temperatureC ?: return null,
            feelsLikeC = current.apparentTemperatureC ?: return null,
            humidityPct = current.humidityPct ?: return null,
            dewPointC = current.dewPointC ?: return null,
            visibilityKm = current.visibilityM?.div(1000.0),
            pressureMb = current.pressureMslHpa ?: return null,
            uvIndex = current.uvIndex?.roundToInt(),
            wind = Wind(
                speedKph = current.windSpeedKph ?: return null,
                degree = windDirection,
                gustKph = current.windGustsKph ?: return null
            ),
            precipitation = precipitation,
            cloudCoverPct = current.cloudCoverPct,
            cloudLayers = layersOf(
                current.cloudCoverLowPct, current.cloudCoverMidPct, current.cloudCoverHighPct
            )
        )
    }

    /**
     * One row per hour, **for the hour that starts at it** (24 set 2026, P7b of the review).
     *
     * Open-Meteo's instant variables (temperature, cloud, visibility, humidity, pressure,
     * wind, UV) are the value AT the label; its precipitation, probability and gusts are
     * «of the preceding hour», and its `weather_code` is derived from that preceding
     * precipitation. So the provider's 15:00 slot says what fell from 14 to 15 — and a row
     * that printed «15 · 60%» from it told the reader about an hour already over by the
     * time it began, an hour late for every «pioggia verso le 15». A row now takes its
     * instants from its own slot and its interval values — [HourlyForecast.precipChancePct],
     * [HourlyForecast.gustKph], since 15.10 [HourlyForecast.precipMm] and
     * [HourlyForecast.snowCm] — from the NEXT one, which is the hour 15-16: what the reader
     * understands the row to mean.
     *
     * The state is both, because Open-Meteo's code is: a precipitation code describes the
     * hour before its slot, a sky or fog code the instant of it. So the row shows what
     * falls **during** its hour and otherwise the sky it **starts** with. Shifting the whole
     * code instead was the first draft, and on the Sydney response of that morning it drew
     * the 06:00 row — 320 m of visibility, fog by any reading — as clear, because the fog
     * had lifted by 07. Since 25 set 2026 the decision is [WeatherStateEngine]'s, on the
     * hours [rows] builds with exactly that split; [states] holds its answer per row.
     *
     * The last hour of the response has no slot after it and is not a row: the week loses
     * its final hour, which is past every horizon the screens draw.
     *
     * Nor is an hour the provider left without a temperature, a state or a cloud cover
     * (19 ott 2026): the three a row cannot do without, and a null in one of them used to
     * fail the whole report. The hour is left out and its neighbours keep their places —
     * a row is identified by its moment, not its position, so a gap is a gap and every
     * reader that walks the list by time (the engines, the estimate, the strip) sees one.
     */
    private fun mapHourly(
        forecast: ForecastResponseDto,
        times: List<LocalDateTime>,
        clock: ProviderClock,
        states: List<Int?>,
        fromIndex: Int
    ): List<HourlyForecast> {
        val hourly = forecast.hourly
        return (fromIndex until (fromIndex + HOURLY_WINDOW).coerceAtMost(times.size - 1))
            .mapNotNull { i ->
                val next = i + 1
                HourlyForecast(
                    // [times] holds the provider's labels; the row carries the city's
                    // clock and the moment itself. They differ only across a DST
                    // change, which is exactly where the old code was an hour out.
                    time = clock.localOf(times[i]),
                    at = clock.instantOf(times[i]),
                    tempC = hourly.temperatureC.getOrNull(i) ?: return@mapNotNull null,
                    condition = WeatherCondition(states[i] ?: return@mapNotNull null),
                    precipChancePct = hourly.precipitationProbabilityPct.getOrNull(next),
                    cloudCoverPct = hourly.cloudCoverPct.getOrNull(i) ?: return@mapNotNull null,
                    feelsLikeC = hourly.apparentTemperatureC.getOrNull(i),
                    humidityPct = hourly.humidityPct.getOrNull(i),
                    dewPointC = hourly.dewPointC.getOrNull(i),
                    pressureMb = hourly.pressureMslHpa.getOrNull(i),
                    windKph = hourly.windSpeedKph.getOrNull(i),
                    windDirectionDeg = hourly.windDirectionDeg.getOrNull(i),
                    gustKph = hourly.windGustsKph.getOrNull(next),
                    uvIndex = hourly.uvIndex.getOrNull(i),
                    visibilityKm = hourly.visibilityM.getOrNull(i)?.div(1000.0),
                    cloudLayers = layersOf(
                        hourly.cloudCoverLowPct.getOrNull(i),
                        hourly.cloudCoverMidPct.getOrNull(i),
                        hourly.cloudCoverHighPct.getOrNull(i)
                    ),
                    // Instants at the slot, like the temperature (14.8).
                    freezingLevelM = hourly.freezingLevelM.getOrNull(i),
                    snowfallHeightM = hourly.snowfallHeightM.getOrNull(i),
                    // Interval values, like the chance and the gust (15.10): what falls in the
                    // hour this row starts, from the next slot. An empty list is a cache entry
                    // from before the field was asked for, and reads null.
                    precipMm = hourly.precipitationMm.getOrNull(next),
                    snowCm = hourly.snowfallCm.getOrNull(next),
                    // An instant at the slot, like the temperature (17.4).
                    capeJkg = hourly.capeJkg.getOrNull(i)
                )
            }
    }

    /** Null when the model split none of the three: an unsplit sky has no layers to name. */
    private fun layersOf(low: Int?, mid: Int?, high: Int?): CloudLayers? =
        if (low == null && mid == null && high == null) null else CloudLayers(low, mid, high)

    /**
     * [providerTimes] and not the re-expressed ones on purpose: a row of `daily` is the
     * provider's aggregate over the provider's OWN day, and [dailyCode] re-derives that
     * row's label from the hours it aggregated. Grouping by the city's clock instead
     * would hand the day a 25th hour that its max and min never saw.
     *
     * What survives of the fixed offset is therefore one hour at each end of the two
     * days a year a zone changes: the label of 26 Oct is derived from 23:00 on the 25th
     * through 22:00 on the 26th. That is the provider's own window, it is what
     * `daily.weather_code` would have said anyway, and it is two orders of magnitude
     * smaller than the error it replaces.
     *
     * A day whose high or low the provider left null takes it from its own hours (19 ott
     * 2026): the same hours its label is already read from, and what Open-Meteo itself
     * serves as the aggregate for most of its models — its EU node served `null` daily
     * extremes for a model on 12-13 Oct 2026 with the hours behind them whole. A day with
     * no hours to read either from, or no label at all, is not a row: a week with a day
     * missing is a true statement, a day with a dash in it is not.
     *
     * The day's possible storm alone reads the rows, by the city's clock ([possibleStormOf]):
     * it is said by the part of the day an hour starts in, as the open day's sentence says what
     * falls.
     */
    private fun mapDaily(
        forecast: ForecastResponseDto,
        providerTimes: List<LocalDateTime>,
        slotStates: List<Int?>,
        rowStates: List<Int?>,
        clock: ProviderClock
    ): List<DailyForecast> {
        val daily = forecast.daily
        val hoursByDate = providerTimes.indices.groupBy { providerTimes[it].toLocalDate() }
        val rowsByDate = rowStates.indices.groupBy { clock.localOf(providerTimes[it]).toLocalDate() }
        return daily.time.take(DAILY_WINDOW).mapIndexedNotNull { i, date ->
            val day = LocalDate.parse(date)
            val hours = hoursByDate[day].orEmpty()
            // NOT `?: 0`, for the reason the line below already gives about the
            // probability: the old zero was a sun that cannot burn, put in the mouth
            // of a model that never spoke. Out of bounds and a null element collapse
            // to the same answer here, which is the right one for both.
            val uvMax = daily.uvIndexMax.getOrNull(i)?.roundToInt()
            val code = dailyCode(
                forecast.hourly,
                slotStates,
                hours,
                daily.weatherCode.getOrNull(i)
            ) ?: return@mapIndexedNotNull null
            DailyForecast(
                date = day,
                highC = daily.temperatureMaxC.getOrNull(i)
                    ?: hourlyExtreme(forecast.hourly, hours) { a, b -> maxOf(a, b) }
                    ?: return@mapIndexedNotNull null,
                lowC = daily.temperatureMinC.getOrNull(i)
                    ?: hourlyExtreme(forecast.hourly, hours) { a, b -> minOf(a, b) }
                    ?: return@mapIndexedNotNull null,
                condition = WeatherCondition(code),
                // NOT `?: 0`: `precipitation_probability_max` is model-dependent, and
                // the old zero was a forecast of no rain put in the mouth of a model
                // that never spoke. Null travels to the surfaces, like the hourly
                // probability beside it.
                precipPct = daily.precipitationProbabilityMaxPct.getOrNull(i),
                uvIndexMax = uvMax,
                precipMm = daily.precipitationSumMm.getOrNull(i),
                precipHours = precipHours(forecast.hourly, slotStates, hours),
                snowCm = daily.snowfallSumCm.getOrNull(i),
                gustMaxKph = daily.windGustsMaxKph.getOrNull(i),
                rainMm = rainOf(daily, i),
                snowLine = snowLineOf(forecast.hourly, slotStates, hours, code),
                possibleStorm = possibleStormOf(forecast.hourly, rowsByDate[day].orEmpty(), rowStates, code, clock, providerTimes)
            )
        }
    }

    /**
     * The day's possible storm (Fase 17.4, 19 nov 2026, [PossibleStorm]) on the day's rows: the
     * hours that START on the day by the city's clock — the strip's hours and the open day's
     * sentence's — each with the amount and the chance of the slot after it (the hour's own,
     * as every row takes them) and the higher of the CAPE at its two ends, the hour's two
     * instants. That pairing is the one the measure scored (`tools/analyze_against_metar.py
     * --hazards`, «Il temporale possibile del giorno»). The day's own [label] is asked too: it
     * reads the provider's slots, and a storm in the last slot of the day before labels this one.
     */
    private fun possibleStormOf(
        hourly: HourlyDto,
        rows: List<Int>,
        rowStates: List<Int?>,
        label: Int,
        clock: ProviderClock,
        providerTimes: List<LocalDateTime>
    ): List<DayPart>? = PossibleStorm.parts(
        rows.map { i ->
            PossibleStorm.Hour(
                start = clock.localOf(providerTimes[i]),
                state = rowStates[i],
                capeJkg = listOfNotNull(hourly.capeJkg.getOrNull(i), hourly.capeJkg.getOrNull(i + 1)).maxOrNull(),
                precipitationMm = hourly.precipitationMm.getOrNull(i + 1),
                chancePct = hourly.precipitationProbabilityPct.getOrNull(i + 1)
            )
        },
        label
    )

    /**
     * The day's snow line (14.8): the lowest [SnowLine] of its wet hours — the hours whose
     * state is precipitation, the engine's reading and not the provider's code — and only
     * when the day's own label [code] is a wet one, which is [dailyCode]'s judgement that
     * the precipitation is material (≥ 1 mm or ≥ 3 hours, hazards exempt). A day labelled
     * by its sky carries none, whatever its stray hours say. Each wet slot's line is the
     * instant at the slot beside the precipitation of the hour before it; the lowest of a
     * day does not depend on which of the two it is paired with.
     */
    private fun snowLineOf(hourly: HourlyDto, codes: List<Int?>, hours: List<Int>, code: Int): SnowLine? {
        if (WmoCode.of(code)?.isPrecipitation != true) return null
        return SnowLine.lowest(
            hours.filter { codes[it]?.let { c -> WmoCode.of(c)?.isPrecipitation } == true }
                .mapNotNull { SnowLine.of(hourly.snowfallHeightM.getOrNull(it), hourly.freezingLevelM.getOrNull(it)) }
        )
    }

    /**
     * Over how many hours the day's millimetres fall: the hours of the day the model gives
     * an amount to, the ones the day's sum is made of, so that «6 mm in 5 ore» is one
     * statement about one model's numbers (17 nov 2026, `docs/STATES.md` §7). It is
     * Open-Meteo's `precipitation_hours`, counted here from the hourly series the response
     * already carries: equal to it on every one of 616 city-days measured, and the
     * variable weighs a tenth of a call.
     *
     * Not the hours the engine calls wet. That count agrees with the strip, and beside the
     * day's total it states a rate the day does not have: Cuneo's 10 Nov 2026, 10 mm over
     * six hours of which two were wet, read «10 mm in 2 hours», and no hour of it had more
     * than 3.1. The wet hours decide one thing only: on a day that has none the count is
     * null, and the note is not written over a strip that draws no rain.
     */
    private fun precipHours(hourly: HourlyDto, states: List<Int?>, hours: List<Int>): Int? {
        if (hours.none { states[it]?.let(WmoCode::isPrecipitation) == true }) return null
        return hours.count { (hourly.precipitationMm.getOrNull(it) ?: 0.0) > 0.0 }
    }

    /** The day's extreme over [hours]' own temperatures, or null when none has one. */
    private fun hourlyExtreme(
        hourly: HourlyDto,
        hours: List<Int>,
        pick: (Double, Double) -> Double
    ): Double? = hours.mapNotNull { hourly.temperatureC.getOrNull(it) }.reduceOrNull(pick)

    /**
     * The day's rain without its snow: `rain_sum` plus `showers_sum`, **capped by what the
     * total leaves once the snow's water is taken out** (25 set 2026).
     *
     * The cap is there because `showers` is the model's convective precipitation of ANY
     * phase: measured on Longyearbyen's response of 25 Sep, the hours coded 85 (snow
     * showers) carry the same water in `showers` and in `snowfall`, and the day added up to
     * 0.8 mm against a `precipitation_sum` of 0.6. On every other day of that response the
     * identity held exactly — total = rain + showers + snowfall / 0.7 (Open-Meteo's own
     * 7 cm to 10 mm) — so the total minus the snow's water is the liquid whenever the split
     * double counts, and the split is the liquid whenever it does not. The smaller of the
     * two is right in both cases; on a day of snow alone the split is 0 and no rounding
     * residue of the subtraction ever reaches the screen.
     *
     * A response without the split — a cache entry from before it was asked for, a model
     * that does not provide it — falls back on the total only when the day has no snow the
     * screen would name ([TRACE_SNOW_CM]); otherwise the answer is null, not the total.
     * Under that floor the snow's water is under 0.15 mm, below what the rain line prints.
     */
    private fun rainOf(daily: DailyDto, i: Int): Double? {
        val rain = daily.rainSumMm.getOrNull(i)
        val showers = daily.showersSumMm.getOrNull(i)
        val total = daily.precipitationSumMm.getOrNull(i)
        val snow = daily.snowfallSumCm.getOrNull(i)
        if (rain != null || showers != null) {
            val split = (rain ?: 0.0) + (showers ?: 0.0)
            if (total == null || snow == null) return split
            return minOf(split, maxOf(0.0, total - snow / SNOW_CM_PER_MM))
        }
        if (snow == null) return null
        return if (snow < TRACE_SNOW_CM) total else null
    }

    /**
     * The day's weather code, derived from the day's own hourly codes instead of read off
     * `daily.weather_code` — which is `max()` over all 24 of them ("the most severe weather
     * condition on a given day"), the one summary guaranteed to pick the least
     * representative hour there is. A single 3am fog code relabelled a whole clear August
     * day `Foggy 🌫️` in Prossimi giorni, in `daily_forecast` and in the morning summary
     * notification; the same `max()` prints a thunderstorm over a week for one nocturnal
     * CAPE spike. Measured 22 Aug 2026 on 8 Po Valley cities — see Fase 13b in PLANNING.md
     * for the numbers behind every choice below.
     *
     * Three steps, in this order, over the WHOLE day for the first two:
     *
     * 1. **A hazard claims the day.** Freezing anything, the heavy grades, every
     *    thunderstorm — [WmoCode.hazard] — whatever else falls, because this rule may
     *    remove a distortion, never a warning (scoping it to daylight, back in 13b, turned
     *    8 night thunderstorms out of 56 days into `Overcast`). The test is
     *    [AlertEngine.severeBucket]'s own since 24 set 2026, so the week's row says what
     *    the banner, the headline and the rules say: a downpour code under a 20% chance
     *    is not «Maltempo», and such an hour is dropped here entirely, millimetres
     *    included. A storm has no floor since 17 nov 2026 (`docs/STATES.md` §11).
     * 2. **Rain that is REALLY there labels the day** (Fase 26): the non-hazard codes
     *    have to clear [WET_DAY_MM] over the day or run for [WET_DAY_HOURS] of it — one
     *    hour of 0.1 mm at 1% probability used to print «Drizzle» over a whole day.
     *    The label is then chosen by [WmoCode.severity] within the day's dominant
     *    [WmoCode.Phase], never by the WMO number (24 set 2026): the number is not an
     *    order of severity, and `max()` over it printed «Rovesci» (80) over a day of
     *    snow (71-75) for one shower, and let slight showers outrank heavy rain. The
     *    phase that brought more water — hours, when the amounts are not there — names
     *    the day; the heaviest intensity inside it picks the word; among codes of the
     *    same intensity (rain and a shower of the same millimetres) the one more hours
     *    carry wins.
     * 3. **Otherwise the sky, and the daylight's.** The row answers "how will the day
     *    look", so a single closed hour at 4am neither darkens nor fogs a sunny day.
     *    Apple (`daytimeForecast`/`overnightForecast`) and Google (`daytimeForecast`
     *    07-19) split the day for the same reason. Most frequent daylight sky wins, ties
     *    to the heavier one by [WmoCode.severity] (17 nov 2026; the number until then) —
     *    which needs no case for fog: on a really foggy day fog is the most frequent code.
     *    An hour whose code is precipitation that did not earn the day votes with the sky its own cloud cover makes ([WeatherStateEngine.skyOf], the provider's
     *    buckets): a light shower that does not label the day must not label its sky
     *    either, and until 24 set 2026 two drizzle hours could win a tie against it.
     *
     * Dates the hourly run does not reach keep the provider's code: it ends with
     * `forecast_days` and the daily one can outrun it. Aggregate better, never blank a row.
     *
     * Null — no row — only when there is nothing to read at all (19 ott 2026): no hour of
     * the day with a state and no code from the provider either. [codes] is null where an
     * hour had nothing to read a state from; such hours do not vote.
     */
    private fun dailyCode(
        hourly: HourlyDto,
        codes: List<Int?>,
        stated: List<Int>,
        fallback: Int?
    ): Int? {
        val hours = stated.filter { codes[it] != null }
        if (hours.isEmpty()) return fallback
        fun codeOf(hour: Int): Int = codes[hour]!!
        val chance = hourly.precipitationProbabilityPct

        val hazards = hours.filter { AlertEngine.severeBucket(codeOf(it), chance.getOrNull(it)) != null }
        if (hazards.isNotEmpty()) return heaviest(hazards.map(::codeOf))

        // Hazard codes that failed the chance test are not wet hours of a lesser kind:
        // they are hours the app has decided not to believe.
        val wet = hours.filter { WmoCode.of(codeOf(it))?.let { w -> w.isPrecipitation && w.hazard == null } == true }
        // An empty `precipitationMm` is a cache entry written before the field was
        // requested: the amount clause simply cannot speak, and the hour count answers alone.
        val amounts = hourly.precipitationMm
        fun mm(of: List<Int>) = of.sumOf { amounts.getOrNull(it) ?: 0.0 }
        val wetMm = mm(wet)
        if (wet.size >= WET_DAY_HOURS || wetMm >= WET_DAY_MM) {
            val weight: (List<Int>) -> Double =
                if (wetMm > 0.0) ::mm else { phaseHours -> phaseHours.size.toDouble() }
            val dominant = wet.groupBy { WmoCode.of(codeOf(it))!!.phase }.values
                .maxWithOrNull(
                    compareBy(weight).thenBy { phaseHours -> phaseHours.maxOf { severity(codeOf(it)) } }
                )!!
            return heaviest(dominant.map(::codeOf))
        }

        // A tie goes to the heavier sky by [WmoCode.severity], then to the higher number so the
        // answer never depends on order. Until 17 nov 2026 it went to the number alone, which
        // was the ladder only while the skies were 0–3 and fog 45/48: the veiled sky is 1003,
        // and by its number it would have won every tie, against fog too.
        val daylight = hours.filter { hourly.isDay.getOrNull(it) == 1 }.ifEmpty { hours }
        return daylight.mapNotNull { skyOf(codeOf(it), hourly, it) }
            .groupingBy { it }.eachCount()
            .maxWithOrNull(compareBy({ it.value }, { WmoCode.of(it.key)?.severity ?: -1 }, { it.key }))?.key
            ?: fallback
    }

    /** The heaviest of [codes] by [WmoCode.severity]; among equally heavy codes, the one
     * more hours carry, then the higher number so the answer never depends on order. */
    private fun heaviest(codes: List<Int>): Int {
        val top = codes.maxOf(::severity)
        return codes.filter { severity(it) == top }
            .groupingBy { it }.eachCount()
            .maxWithOrNull(compareBy({ it.value }, { it.key }))!!.key
    }

    /** Only ever called on codes the table knows: hazards and wet hours are filtered through it. */
    private fun severity(code: Int): Int = WmoCode.of(code)!!.severity

    /** The sky an hour shows: its own code when that is a sky or a fog, else the one its
     * cloud makes, by the light its layers take (rule 6) — none when there is no cloud cover
     * to make it from. An unknown number is not a sky either. */
    private fun skyOf(code: Int, hourly: HourlyDto, slot: Int): Int? =
        if (WmoCode.of(code)?.phase == WmoCode.Phase.NONE) code else hourly.cloudCoverPct.getOrNull(slot)?.let { cover ->
            WeatherStateEngine.skyOf(
                cover,
                layersOf(
                    hourly.cloudCoverLowPct.getOrNull(slot),
                    hourly.cloudCoverMidPct.getOrNull(slot),
                    hourly.cloudCoverHighPct.getOrNull(slot)
                )
            )
        }

    /**
     * The hours that have closed by [providerNow], newest first, up to
     * [Precipitation.PAST_HOURS]: each hourly value of `precipitation` is Open-Meteo's
     * «sum of the preceding hour», so the slot labelled 10:00 is the rain of 09:00-10:00
     * and closes at 10:00. The label is compared in the provider's frame and the end
     * leaves as the instant it names, like every other row of this mapper.
     *
     * Empty when the amounts are not in the response — a cache entry from before Fase 26
     * — rather than a list of zeros: no amount is not a dry hour.
     */
    private fun pastHours(
        hourly: HourlyDto,
        providerTimes: List<LocalDateTime>,
        clock: ProviderClock,
        providerNow: LocalDateTime
    ): List<PastHour> {
        if (hourly.precipitationMm.isEmpty()) return emptyList()
        val newest = providerTimes.indexOfLast { !it.isAfter(providerNow) }
        if (newest < 0) return emptyList()
        return (newest downTo maxOf(0, newest - Precipitation.PAST_HOURS + 1)).mapNotNull { i ->
            val mm = hourly.precipitationMm.getOrNull(i) ?: return@mapNotNull null
            PastHour(
                endedAt = clock.instantOf(providerTimes[i]),
                precipMm = mm,
                // An hour the provider left without a temperature is not a closed hour
                // either (19 ott 2026): left out, never a zero.
                tempC = hourly.temperatureC.getOrNull(i) ?: return@mapNotNull null
            )
        }
    }

    /**
     * The hours as the rows mean them, for [WeatherStateEngine] (25 set 2026): row i is
     * the hour that starts at slot i, so it takes the interval values — the amounts, the
     * probability, the provider's code of what fell — from slot i + 1, and the instants —
     * temperature, cloud, visibility, the provider's code of the sky — from slot i. The
     * same split [mapHourly] prints; one row fewer than there are slots.
     */
    private fun HourlyDto.rows(voters: Witnesses): List<WeatherStateEngine.Hour?> =
        (0 until time.size - 1).map { i -> hourOf(interval = i + 1, instant = i, voters) }

    /**
     * The hours as the provider's slots mean them, for the day's label ([dailyCode] groups
     * the provider's slots by the provider's day, as its aggregates do): slot k is the
     * hour that ENDS at it, with its interval values and the sky and fog of its own
     * instant — what `dailyCode` read before the engine, now decided by it.
     */
    private fun HourlyDto.slots(voters: Witnesses): List<WeatherStateEngine.Hour?> =
        time.indices.map { k -> hourOf(interval = k, instant = k, voters) }

    /**
     * [hours] with what their series decides about each hour written into it (19 nov 2026):
     * whether two models' fog persists into a neighbouring hour ([WeatherStateEngine.fogPersists],
     * rule 5) and how long the episode of its rain is ([WeatherStateEngine.episodeHours], rule
     * 3). Counted on the hours that exist, as [statesOf] hands them to the engine (a missing
     * hour's neighbours are each other's), so the states of the series are the ones the engine
     * would count by itself, to the hour.
     *
     * Counted once, here, and read twice: by the engine for every row, and by the present for
     * the row it falls in. Until 19 nov 2026 the present counted both again on the rows WITH
     * their holes, where a missing hour ends a run, and next to a missing hour the hero could
     * say «Rovesci» over a cell saying «Pioggia», or «Nebbia» over a cell saying «Coperto».
     * Null where the hour is.
     */
    private fun seriesOf(hours: List<WeatherStateEngine.Hour?>): List<WeatherStateEngine.Hour?> {
        val present = hours.filterNotNull()
        val votes = present.map(WeatherStateEngine::fogVotes)
        var k = 0
        return hours.map { hour ->
            hour?.let {
                val at = k++
                it.copy(
                    fogPersists = it.fogPersists ?: WeatherStateEngine.fogPersists(votes, at),
                    episodeHours = it.episodeHours ?: WeatherStateEngine.episodeHours(present, at)
                )
            }
        }
    }

    /**
     * The engine's states for [hours], in their places: null where the hour was null —
     * the provider gave it nothing to read a state from (19 ott 2026). The engine sees
     * the hours that exist as one series, so a missing hour's neighbours are judged
     * against each other for the fog rule; the day such a response describes is already
     * one the model has holes in, and that is the smaller inaccuracy. What the series
     * decides about each hour is already written in by [seriesOf], on that same series.
     */
    private fun statesOf(hours: List<WeatherStateEngine.Hour?>): List<Int?> {
        val present = hours.mapIndexedNotNull { i, hour -> hour?.let { i to it } }
        val states = WeatherStateEngine.states(present.map { it.second })
        val out = arrayOfNulls<Int>(hours.size)
        present.forEachIndexed { k, (i, _) -> out[i] = states[k] }
        return out.toList()
    }

    /**
     * Null when the provider gave the hour nothing to read a state from (19 ott 2026):
     * no code and no cloud cover at its instant. A missing code alone is read as the sky
     * its cloud cover makes — the provider has not said what falls, and the engine then
     * reads the amounts, or finds the hour dry, and the sky from the cloud as it always
     * does. A missing code on the interval alone claims nothing falls: the hour keeps the
     * sky it starts with.
     */
    private fun HourlyDto.hourOf(interval: Int, instant: Int, voters: Witnesses): WeatherStateEngine.Hour? {
        val sky = codeAt(instant) ?: return null
        return WeatherStateEngine.Hour(
            providerCode = codeAt(interval) ?: sky,
            providerSkyCode = sky,
            // An empty list is a cache entry from before the field was asked for: the engine
            // then falls back on the provider's code for exactly that part of the decision.
            precipitationMm = precipitationMm.getOrNull(interval),
            snowfallCm = snowfallCm.getOrNull(interval),
            showersMm = showersMm.getOrNull(interval),
            chancePct = precipitationProbabilityPct.getOrNull(interval),
            temperatureC = temperatureC.getOrNull(instant),
            cloudCoverPct = cloudCoverPct.getOrNull(instant),
            cloudLayers = layersOf(
                cloudCoverLowPct.getOrNull(instant), cloudCoverMidPct.getOrNull(instant), cloudCoverHighPct.getOrNull(instant)
            ),
            // Fog is the models' votes (17 nov 2026): the own model's from its humidity and
            // wind at the instant, the witnesses' from their visibility. `best_match`'s own
            // visibility is not read: ICON's goes on 30 Nov 2026, and the rule is the same
            // before and after.
            humidityPct = humidityPct.getOrNull(instant),
            windKph = windSpeedKph.getOrNull(instant),
            witnesses = voters.at(this, instant = instant),
            // Whether a rain is a shower (19 nov 2026): the hour's convective energy, the higher
            // of its two ends — the slot before the interval's and the interval's own, which for
            // a row are its start and its end. Empty, a cache entry from before: `showers` decides.
            capeJkg = listOfNotNull(capeJkg.getOrNull(interval - 1), capeJkg.getOrNull(interval)).maxOrNull()
        )
    }

    /** The provider's code at [slot], or the sky its cloud cover makes when there is none. */
    private fun HourlyDto.codeAt(slot: Int): Int? =
        weatherCode.getOrNull(slot) ?: cloudCoverPct.getOrNull(slot)?.let(WeatherStateEngine::skyOf)

    /**
     * The `current` block through the same engine. Its amounts are the sums of its own
     * interval — 15 minutes — and are scaled to the hour the thresholds are written for;
     * its chance is the one of the hour that interval falls in (17 nov 2026; the hour under
     * way until then, the next hour's in the first quarter of every hour). It never says rain or snow is
     * «likely», because it describes NOW: what nothing measures is not the present.
     *
     * Its fog follows the row it falls in ([fogPersists], 26 ott 2026). Until then it could
     * invent fog with no neighbour, as if its numbers were an observation: they are the
     * model's, interpolated like the rows'. Lissone, 26 Oct 2026, 09:45: 380 m under code
     * 3, from a model whose fog was one hour long (140 m at 09:00, 4 km at 08:00, 1.2 km at
     * 10:00), so the strip said «Coperto» and the hero «Nebbia» under a part-sunny sky
     * with the visibility excellent on the spot.
     *
     * Its votes (17 nov 2026) are the row's witnesses and its own model's, from the block's
     * own humidity and wind: the quarter's air, the hour's other models.
     *
     * Its amounts are the hour's own, [amountsFrom] being that hour's slot, where the quarter
     * is the hour cut in four ([cutInFour], 17 nov 2026): the numbers the strip reads, so the
     * hero and the cell under it say the same thing. An hour the response left without an
     * amount keeps the block's.
     *
     * Whether its rain is a shower is the row's too (19 nov 2026): the row's CAPE ([rowCapeJkg],
     * the higher of its two ends) and the episode the row belongs to ([rowEpisodeHours]); the
     * block has no CAPE of its own, and alone it has no episode.
     *
     * Null when the block has neither a code nor a cloud cover to read a state from
     * (19 ott 2026), like an hour of the series ([hourOf]).
     */
    private fun currentState(
        current: CurrentDto,
        hourly: HourlyDto,
        amountsFrom: Int?,
        chance: Int?,
        fogPersists: Boolean,
        witnesses: List<WeatherStateEngine.Witness>,
        rowCapeJkg: Double?,
        rowEpisodeHours: Int?
    ): Int? {
        val perHour = 3600.0 / (current.intervalSeconds?.takeIf { it > 0 } ?: CURRENT_INTERVAL_S)
        val code = current.weatherCode
            ?: current.cloudCoverPct?.let(WeatherStateEngine::skyOf)
            ?: return null
        val ofHour = amountsFrom?.takeIf { hourly.precipitationMm.getOrNull(it) != null }
        fun amount(ofTheHour: List<Double?>, ofTheBlock: Double?): Double? =
            if (ofHour != null) ofTheHour.getOrNull(ofHour) else ofTheBlock?.times(perHour)
        return WeatherStateEngine.state(
            WeatherStateEngine.Hour(
                providerCode = code,
                precipitationMm = amount(hourly.precipitationMm, current.precipitationMm),
                snowfallCm = amount(hourly.snowfallCm, current.snowfallCm),
                showersMm = amount(hourly.showersMm, current.showersMm),
                chancePct = chance,
                temperatureC = current.temperatureC,
                cloudCoverPct = current.cloudCoverPct,
                cloudLayers = layersOf(current.cloudCoverLowPct, current.cloudCoverMidPct, current.cloudCoverHighPct),
                humidityPct = current.humidityPct,
                windKph = current.windSpeedKph,
                fogPersists = fogPersists,
                witnesses = witnesses,
                capeJkg = rowCapeJkg,
                episodeHours = rowEpisodeHours
            ),
            likely = false
        )
    }

    /**
     * Sun and moon from [AstronomyEngine], not from the provider's daily block
     * (upstream Fase 16e).
     *
     * The provider's values are not even fetched any more (20 set 2026): they fed
     * nothing, and a `daily.sunrise` left lying in the DTO is now a trap, since the
     * provider writes it on the response's fixed offset and it drifts an hour from the
     * clock after a DST change. See `OpenMeteoForecastApi.DAILY_VARIABLES`. The engine
     * wins for the reason `VISION_SKY.md` §9.2 gives: the same figure appears in the JSON
     * tab, in the README and on a `sky.crontab` line that is computed anyway, and a
     * document showing 06:31 in one tab and 06:32 in another because one of them
     * waited for the network is exactly what "one engine is the source of truth" was
     * written against. It also means these times are right offline and past the
     * seven-day horizon, which the provider's cannot be.
     *
     * Nulls are real answers here: above the Arctic circle in June there is no
     * sunrise, and the old code could only put some other time in its place.
     */
    private fun mapAstronomical(
        city: City,
        clock: ProviderClock,
        localTime: LocalDateTime,
        fetchedAt: Instant
    ): Astronomical {
        val zone = clock.zone
        val day = AstronomyEngine.solarDay(localTime.toLocalDate(), zone, city.coordinates)
        // Truncated to the minute, and not for tidiness. Every surface renders these
        // as `HH:mm`, but `WeatherSnapshots.flatten` writes `sunrise.toString()` into
        // the history — so a value carrying seconds would put a fresh
        // `astronomical.sunrise` line in `history.diff` on EVERY fetch, since
        // the engine's answer moves by a fraction of a second between two of them.
        // The provider's values were minute-precise and nothing noticed until they
        // stopped being the source.
        fun clock(at: Instant?) =
            at?.atZone(zone)?.toLocalTime()?.truncatedTo(ChronoUnit.MINUTES)
        return Astronomical(
            sunrise = clock(day.sunrise),
            sunset = clock(day.sunset),
            moonPhase = MoonPhase.at(fetchedAt),
            // Derived from this engine's own two ends rather than read off
            // `daily.daylight_duration`: three numbers that must agree are better as
            // two numbers and a subtraction.
            daylightDuration = day.daylight
        )
    }

    /**
     * [inEurope] picks the scale the place reads (24 set 2026): the EEA's where the place is
     * European and the index was served, the US one otherwise — never a European label on
     * a missing European number.
     */
    private fun AirQualityCurrentDto.toAirQuality(inEurope: Boolean): AirQuality? {
        val aqi = usAqi ?: return null
        return AirQuality(
            aqiIndex = aqi,
            europeanAqi = europeanAqi,
            scale = if (inEurope && europeanAqi != null) AqiScale.EUROPEAN else AqiScale.US
        )
    }

    /**
     * Each species on its own MeteoSwiss scale, then the worst per family (24 set 2026),
     * **from the day's mean** (17 nov 2026): the classes are written for a mean daily
     * concentration, and read off the current hour's the level changed with the hour the
     * reader opened the app (`PLANNING.md`, «La review del motore, punto 5»). A species with fewer than
     * [POLLEN_MIN_HOURS] of the day has no mean, and no level.
     */
    private fun AirQualityHourlyDto.toPollenReport(): PollenReport? {
        fun mean(hours: List<Double?>): Double? =
            hours.filterNotNull().takeIf { it.size >= POLLEN_MIN_HOURS }?.average()
        return PollenReport(
            grass = WeatherCodes.pollenFamilyLevel(PollenSpecies.GRASS to mean(grassPollen)) ?: return null,
            tree = WeatherCodes.pollenFamilyLevel(
                PollenSpecies.BIRCH to mean(birchPollen),
                PollenSpecies.ALDER to mean(alderPollen),
                PollenSpecies.OLIVE to mean(olivePollen)
            ) ?: return null,
            weed = WeatherCodes.pollenFamilyLevel(
                PollenSpecies.RAGWEED to mean(ragweedPollen),
                PollenSpecies.MUGWORT to mean(mugwortPollen)
            ) ?: return null
        )
    }
}
