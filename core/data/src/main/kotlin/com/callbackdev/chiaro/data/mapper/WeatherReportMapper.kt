package com.callbackdev.chiaro.data.mapper

import com.callbackdev.chiaro.data.remote.OpenMeteoForecastApi
import com.callbackdev.chiaro.data.remote.dto.AirQualityCurrentDto
import com.callbackdev.chiaro.data.remote.dto.CurrentDto
import com.callbackdev.chiaro.data.remote.dto.DailyDto
import com.callbackdev.chiaro.data.remote.dto.ForecastResponseDto
import com.callbackdev.chiaro.data.remote.dto.HourlyDto
import com.callbackdev.chiaro.data.remote.dto.WitnessResponseDto
import com.callbackdev.chiaro.domain.AlertEngine
import com.callbackdev.chiaro.domain.CurrentEstimate
import com.callbackdev.chiaro.domain.NowcastCoverage
import com.callbackdev.chiaro.domain.PlaceRegion
import com.callbackdev.chiaro.domain.WeatherCodes
import com.callbackdev.chiaro.domain.WeatherCodes.PollenSpecies
import com.callbackdev.chiaro.domain.WeatherStateEngine
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
import com.callbackdev.chiaro.domain.model.Pollutants
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
 * `FORECAST_DAYS × 24` since Fase 16a, 25 before it. The old number was one day, and
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
        responseTimeMs: Long,
        cacheStatus: CacheStatus,
        /** The other model families that vote on the states; null, and best_match is alone. */
        witnesses: WitnessResponseDto? = null
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
        val rows = forecast.hourly.rows(voters)
        val rowStates = statesOf(rows)
        val slotStates = statesOf(forecast.hourly.slots(voters))
        // The chance of the hour under way: the slot that ENDS after now, since
        // `currentHourIndex`'s slot is the hour already gone. One value for the stated
        // chance and for the state the hero shows.
        val chanceNow = providerTimes.indexOfFirst { it.isAfter(providerNow) }
            .takeIf { it >= 0 }
            ?.let { forecast.hourly.precipitationProbabilityPct.getOrNull(it) }
        val currentHour = providerNow.truncatedTo(ChronoUnit.HOURS)
        val currentHourIndex = providerTimes.indexOfFirst { !it.isBefore(currentHour) }
            .coerceAtLeast(0)
        val localTime = clock.localOf(providerNow)
        // The current block's fog is the series' (26 ott 2026): its visibility is the
        // model's like the rows', and the row now falls in decides whether fog may be
        // invented, so the hero never draws a fog the strip below it does not. The series
        // is the one the engine reads (27 ott 2026): each row's own visibility where it
        // has one, else the witnesses' lowest.
        val nowIsARow = providerTimes.getOrNull(currentHourIndex) == currentHour
        val fogNow = nowIsARow &&
            WeatherStateEngine.fogPersists(rows.map { it?.let(WeatherStateEngine::visibilityOf) }, currentHourIndex)
        // ...and so are its witnesses: the row's, read at the row's instant.
        val nowVoters = if (nowIsARow) voters.at(forecast.hourly, instant = currentHourIndex) else emptyList()
        // ...and whether its visibility is its own: where the row's number is a witness's
        // copy (Open-Meteo's fill), the block's is the same fill, and the block reads the
        // witnesses' as the row does.
        val nowVisibilityIsOwn = !nowIsARow || voters.ownVisibilityOf(forecast.hourly, currentHourIndex) != null

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
            current = wholeCurrent(current, chanceNow, fogNow, nowVoters, nowVisibilityIsOwn, precipitation)
                ?: CurrentEstimate.estimate(hourly, precipitation, clock.instantOf(providerNow))
                ?: error("the current block has a hole in it and the hours cannot stand in"),
            airQuality = airQuality?.toAirQuality(PlaceRegion.inEurope(city.countryCode)),
            pollen = airQuality?.toPollenReport(),
            astronomical = mapAstronomical(city, clock, localTime, fetchedAt),
            hourly = hourly,
            daily = mapDaily(forecast, providerTimes, slotStates),
            systemInfo = SystemInfo(
                source = SOURCE,
                lastSync = fetchedAt,
                cacheStatus = cacheStatus,
                responseTimeMs = responseTimeMs
            ),
            nowcast = mapNowcast(forecast, clock, city)
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
     * The provider's `current` block as the conditions now, or null when it has a hole in
     * it (19 ott 2026): a value the block cannot do without — the temperature, the
     * apparent one, humidity, dew point, pressure, the wind's three — or nothing to read a
     * state from. Visibility, UV, the cloud and its layers are nullable on the other side
     * and travel as they are.
     */
    private fun wholeCurrent(
        current: CurrentDto,
        chance: Int?,
        fogPersists: Boolean,
        witnesses: List<WeatherStateEngine.Witness>,
        visibilityIsOwn: Boolean,
        precipitation: Precipitation
    ): CurrentConditions? {
        val state = currentState(current, chance, fogPersists, witnesses, visibilityIsOwn) ?: return null
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
                directionCompass = WeatherCodes.windCompass(windDirection),
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
     * [HourlyForecast.gustKph] — from the NEXT one, which is the hour 15-16: what the reader
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
                    )
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
     */
    private fun mapDaily(
        forecast: ForecastResponseDto,
        providerTimes: List<LocalDateTime>,
        slotStates: List<Int?>
    ): List<DailyForecast> {
        val daily = forecast.daily
        val hoursByDate = providerTimes.indices.groupBy { providerTimes[it].toLocalDate() }
        return daily.time.take(DAILY_WINDOW).mapIndexedNotNull { i, date ->
            val day = LocalDate.parse(date)
            val hours = hoursByDate[day].orEmpty()
            // NOT `?: 0`, for the reason the line below already gives about the
            // probability: the old zero was a sun that cannot burn, put in the mouth
            // of a model that never spoke. Out of bounds and a null element collapse
            // to the same answer here, which is the right one for both.
            val uvMax = daily.uvIndexMax.getOrNull(i)?.roundToInt()
            DailyForecast(
                date = day,
                highC = daily.temperatureMaxC.getOrNull(i)
                    ?: hourlyExtreme(forecast.hourly, hours) { a, b -> maxOf(a, b) }
                    ?: return@mapIndexedNotNull null,
                lowC = daily.temperatureMinC.getOrNull(i)
                    ?: hourlyExtreme(forecast.hourly, hours) { a, b -> minOf(a, b) }
                    ?: return@mapIndexedNotNull null,
                condition = WeatherCondition(
                    dailyCode(
                        forecast.hourly,
                        slotStates,
                        hours,
                        daily.weatherCode.getOrNull(i)
                    ) ?: return@mapIndexedNotNull null
                ),
                // NOT `?: 0`: `precipitation_probability_max` is model-dependent, and
                // the old zero was a forecast of no rain put in the mouth of a model
                // that never spoke. Null travels to the surfaces, like the hourly
                // probability beside it.
                precipPct = daily.precipitationProbabilityMaxPct.getOrNull(i),
                uvIndexMax = uvMax,
                precipMm = daily.precipitationSumMm.getOrNull(i),
                precipHours = daily.precipitationHours.getOrNull(i),
                snowCm = daily.snowfallSumCm.getOrNull(i),
                gustMaxKph = daily.windGustsMaxKph.getOrNull(i),
                rainMm = rainOf(daily, i)
            )
        }
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
     * 1. **A hazard the ensemble backs claims the day.** Freezing anything, the heavy
     *    grades, every thunderstorm — [WmoCode.hazard] — whatever else falls, because
     *    this rule may remove a distortion, never a warning (scoping it to daylight, back
     *    in 13b, turned 8 night thunderstorms out of 56 days into `Overcast`). "Backs" is
     *    [AlertEngine.severeBucket]'s own test since 24 set 2026: a storm or downpour code
     *    under a 20% chance is not «Maltempo» for the banner, the headline or the rules,
     *    and the week's row used to be the one surface still printing it. Such an hour is
     *    dropped here entirely, millimetres included — the ensemble says it will not
     *    happen, and that is the judgement the rest of the app already made.
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
     *    to the heavier one — which needs no case for fog: on a really foggy day fog is
     *    the most frequent code. An hour whose code is precipitation that did not earn
     *    the day votes with the sky its own cloud cover makes ([WeatherStateEngine.skyOf], the provider's
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
        fun mm(of: List<Int>) = of.sumOf { amounts.getOrElse(it) { 0.0 } }
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

        val daylight = hours.filter { hourly.isDay.getOrNull(it) == 1 }.ifEmpty { hours }
        return daylight.mapNotNull { skyOf(codeOf(it), hourly, it) }
            .groupingBy { it }.eachCount()
            .maxWithOrNull(compareBy({ it.value }, { it.key }))?.key ?: fallback
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
     * The engine's states for [hours], in their places: null where the hour was null —
     * the provider gave it nothing to read a state from (19 ott 2026). The engine sees
     * the hours that exist as one series, so a missing hour's neighbours are judged
     * against each other for the fog rule; the day such a response describes is already
     * one the model has holes in, and that is the smaller inaccuracy.
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
            // Fog persistence is the engine's to judge, from the series' visibility: the
            // hour's own where the number is really its own, else the witnesses' lowest.
            visibilityM = voters.ownVisibilityOf(this, instant),
            humidityPct = humidityPct.getOrNull(instant),
            witnesses = voters.at(this, instant = instant)
        )
    }

    /** The provider's code at [slot], or the sky its cloud cover makes when there is none. */
    private fun HourlyDto.codeAt(slot: Int): Int? =
        weatherCode.getOrNull(slot) ?: cloudCoverPct.getOrNull(slot)?.let(WeatherStateEngine::skyOf)

    /**
     * The `current` block through the same engine. Its amounts are the sums of its own
     * interval — 15 minutes — and are scaled to the hour the thresholds are written for;
     * its chance is the one of the hour under way. It never says rain or snow is
     * «likely», because it describes NOW: what nothing measures is not the present.
     *
     * Its fog follows the row it falls in ([fogPersists], 26 ott 2026). Until then it could
     * invent fog with no neighbour, as if its visibility were an observation: it is the
     * model's, interpolated like the rows'. Lissone, 26 Oct 2026, 09:45: 380 m under code
     * 3, from a model whose fog was one hour long (140 m at 09:00, 4 km at 08:00, 1.2 km at
     * 10:00), so the strip said «Coperto» and the hero «Nebbia» under a part-sunny sky
     * with the visibility excellent on the spot.
     *
     * Its visibility is its own only where its row's is ([visibilityIsOwn], 27 ott 2026):
     * where the row carries a witness's copy, or nothing, the block reads the witnesses'
     * lowest like the row and asks two of them, so the hero and the strip read one series.
     *
     * Null when the block has neither a code nor a cloud cover to read a state from
     * (19 ott 2026), like an hour of the series ([hourOf]).
     */
    private fun currentState(
        current: CurrentDto,
        chance: Int?,
        fogPersists: Boolean,
        witnesses: List<WeatherStateEngine.Witness>,
        visibilityIsOwn: Boolean
    ): Int? {
        val perHour = 3600.0 / (current.intervalSeconds?.takeIf { it > 0 } ?: CURRENT_INTERVAL_S)
        val code = current.weatherCode
            ?: current.cloudCoverPct?.let(WeatherStateEngine::skyOf)
            ?: return null
        return WeatherStateEngine.state(
            WeatherStateEngine.Hour(
                providerCode = code,
                precipitationMm = current.precipitationMm?.times(perHour),
                snowfallCm = current.snowfallCm?.times(perHour),
                showersMm = current.showersMm?.times(perHour),
                chancePct = chance,
                temperatureC = current.temperatureC,
                cloudCoverPct = current.cloudCoverPct,
                cloudLayers = layersOf(current.cloudCoverLowPct, current.cloudCoverMidPct, current.cloudCoverHighPct),
                visibilityM = current.visibilityM.takeIf { visibilityIsOwn },
                humidityPct = current.humidityPct,
                fogPersists = fogPersists,
                witnesses = witnesses
            ),
            likely = false
        )
    }

    /**
     * Sun and moon from [AstronomyEngine], not from the provider's daily block
     * (Fase 16e).
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
            scale = if (inEurope && europeanAqi != null) AqiScale.EUROPEAN else AqiScale.US,
            // Null for what the service did not measure — never a 0.0 of clean air.
            pollutants = Pollutants(
                pm25 = pm25,
                pm10 = pm10,
                o3 = ozone,
                no2 = no2,
                so2 = so2,
                coMg = co?.div(1000.0) // the API serves µg/m³
            )
        )
    }

    /** Each species on its own MeteoSwiss scale, then the worst per family (24 set 2026). */
    private fun AirQualityCurrentDto.toPollenReport(): PollenReport? {
        return PollenReport(
            grass = WeatherCodes.pollenFamilyLevel(PollenSpecies.GRASS to grassPollen) ?: return null,
            tree = WeatherCodes.pollenFamilyLevel(
                PollenSpecies.BIRCH to birchPollen,
                PollenSpecies.ALDER to alderPollen,
                PollenSpecies.OLIVE to olivePollen
            ) ?: return null,
            weed = WeatherCodes.pollenFamilyLevel(
                PollenSpecies.RAGWEED to ragweedPollen,
                PollenSpecies.MUGWORT to mugwortPollen
            ) ?: return null
        )
    }
}
