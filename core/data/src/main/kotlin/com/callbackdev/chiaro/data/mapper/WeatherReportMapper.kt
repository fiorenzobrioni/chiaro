package com.callbackdev.chiaro.data.mapper

import com.callbackdev.chiaro.data.remote.OpenMeteoForecastApi
import com.callbackdev.chiaro.data.remote.dto.AirQualityCurrentDto
import com.callbackdev.chiaro.data.remote.dto.DailyDto
import com.callbackdev.chiaro.data.remote.dto.ForecastResponseDto
import com.callbackdev.chiaro.data.remote.dto.HourlyDto
import com.callbackdev.chiaro.domain.AlertEngine
import com.callbackdev.chiaro.domain.PlaceRegion
import com.callbackdev.chiaro.domain.WeatherCodes
import com.callbackdev.chiaro.domain.WeatherCodes.PollenSpecies
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
import com.callbackdev.chiaro.domain.model.PastHour
import com.callbackdev.chiaro.domain.model.PollenReport
import com.callbackdev.chiaro.domain.model.Pollutants
import com.callbackdev.chiaro.domain.model.Precipitation
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

/** WMO 45, what a fog the visibility proves is written as; 48 is only ever relayed. */
private val WMO_FOG = WmoCode.FOG.code

/** Open-Meteo's own fog threshold, `WeatherCode.swift:99`: `visibility <= 1000 → fog`. */
private const val FOG_VISIBILITY_M = 1000.0

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
 * on Longyearbyen's 2 Oct of the 25 Sep run, 19.81 cm against 28.4 mm on Everest.
 */
private const val SNOW_CM_PER_MM = 0.7

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
        cacheStatus: CacheStatus
    ): WeatherReport {
        val current = forecast.current
        val clock = ProviderClock(forecast.timezone, forecast.utcOffsetSeconds)

        // The provider's own labels, kept in the provider's own frame. Every
        // comparison BETWEEN response values happens here, where they all share one
        // offset and the arithmetic is exact; only the values that leave this function
        // are re-expressed on the city's clock.
        val providerNow = LocalDateTime.parse(current.time)
        val providerTimes = forecast.hourly.time.map(LocalDateTime::parse)
        val hourlyCodes = forecast.hourly.repairedCodes()
        val currentHour = providerNow.truncatedTo(ChronoUnit.HOURS)
        val currentHourIndex = providerTimes.indexOfFirst { !it.isBefore(currentHour) }
            .coerceAtLeast(0)
        val localTime = clock.localOf(providerNow)

        return WeatherReport(
            location = Location(
                city = city.name,
                region = city.region,
                country = city.country,
                coordinates = Coordinates(forecast.latitude, forecast.longitude),
                timezone = forecast.timezone,
                localTime = localTime
            ),
            current = CurrentConditions(
                condition = WeatherCondition(
                    repairFog(
                        code = current.weatherCode,
                        visibilityM = current.visibilityM,
                        cloudCoverPct = current.cloudCoverPct,
                        // An observation has no run to belong to — see repairFog.
                        fogPersists = true
                    )
                ),
                tempC = current.temperatureC,
                feelsLikeC = current.apparentTemperatureC,
                humidityPct = current.humidityPct,
                dewPointC = current.dewPointC,
                visibilityKm = current.visibilityM?.div(1000.0),
                pressureMb = current.pressureMslHpa,
                uvIndex = current.uvIndex?.roundToInt(),
                wind = Wind(
                    speedKph = current.windSpeedKph,
                    directionCompass = WeatherCodes.windCompass(current.windDirectionDeg),
                    degree = current.windDirectionDeg,
                    gustKph = current.windGustsKph
                ),
                precipitation = Precipitation(
                    lastQuarterHourMm = current.precipitationMm,
                    pastHours = pastHours(forecast.hourly, providerTimes, clock, providerNow),
                    // The slot that ENDS after now is the one describing the hour under
                    // way; `currentHourIndex`'s slot is the hour already gone.
                    chancePct = providerTimes.indexOfFirst { it.isAfter(providerNow) }
                        .takeIf { it >= 0 }
                        ?.let { forecast.hourly.precipitationProbabilityPct.getOrNull(it) },
                    quarterEndedAt = clock.instantOf(providerNow)
                ),
                cloudCoverPct = current.cloudCoverPct,
                cloudLayers = layersOf(
                    current.cloudCoverLowPct, current.cloudCoverMidPct, current.cloudCoverHighPct
                )
            ),
            airQuality = airQuality?.toAirQuality(PlaceRegion.inEurope(city.countryCode)),
            pollen = airQuality?.toPollenReport(),
            astronomical = mapAstronomical(city, clock, localTime, fetchedAt),
            hourly = mapHourly(forecast, providerTimes, clock, hourlyCodes, currentHourIndex),
            daily = mapDaily(forecast, providerTimes, hourlyCodes),
            systemInfo = SystemInfo(
                source = SOURCE,
                lastSync = fetchedAt,
                cacheStatus = cacheStatus,
                responseTimeMs = responseTimeMs
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
     * The code is both, because Open-Meteo's is: a precipitation code describes the hour
     * before its slot, a sky or fog code the instant of it. So the row shows the rain that
     * falls **during** its hour (the next slot's code, when that is precipitation) and
     * otherwise the sky it **starts** with ([skyAt]). Shifting the whole code instead was
     * the first draft, and on the Sydney response of that morning it drew the 06:00 row —
     * 320 m of visibility, fog by any reading — as clear, because the fog had lifted by 07.
     *
     * The fog repair and the day's code still read the provider's slots as they are
     * (`codes` is indexed by slot); only the rows are re-expressed. The last hour of the
     * response has no slot after it and is not a row: the week loses its final hour,
     * which is past every horizon the screens draw.
     */
    private fun mapHourly(
        forecast: ForecastResponseDto,
        times: List<LocalDateTime>,
        clock: ProviderClock,
        codes: List<Int>,
        fromIndex: Int
    ): List<HourlyForecast> {
        val hourly = forecast.hourly
        return (fromIndex until (fromIndex + HOURLY_WINDOW).coerceAtMost(times.size - 1))
            .map { i ->
                val next = i + 1
                HourlyForecast(
                    // [times] holds the provider's labels; the row carries the city's
                    // clock and the moment itself. They differ only across a DST
                    // change, which is exactly where the old code was an hour out.
                    time = clock.localOf(times[i]),
                    at = clock.instantOf(times[i]),
                    tempC = hourly.temperatureC[i],
                    condition = WeatherCondition(
                        if (WmoCode.isPrecipitation(codes[next])) codes[next] else hourly.skyAt(i, codes)
                    ),
                    precipChancePct = hourly.precipitationProbabilityPct.getOrNull(next),
                    // Read like its siblings: the parallel arrays are the same length
                    // in any response that deserialized, and `repairedCodes()` has
                    // already indexed this very column over all of them.
                    cloudCoverPct = hourly.cloudCoverPct[i],
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
     */
    private fun mapDaily(
        forecast: ForecastResponseDto,
        providerTimes: List<LocalDateTime>,
        hourlyCodes: List<Int>
    ): List<DailyForecast> {
        val daily = forecast.daily
        val hoursByDate = providerTimes.indices.groupBy { providerTimes[it].toLocalDate() }
        return daily.time.take(DAILY_WINDOW).mapIndexed { i, date ->
            val day = LocalDate.parse(date)
            // NOT `?: 0`, for the reason the line below already gives about the
            // probability: the old zero was a sun that cannot burn, put in the mouth
            // of a model that never spoke. Out of bounds and a null element collapse
            // to the same answer here, which is the right one for both.
            val uvMax = daily.uvIndexMax.getOrNull(i)?.roundToInt()
            DailyForecast(
                date = day,
                highC = daily.temperatureMaxC[i],
                lowC = daily.temperatureMinC[i],
                condition = WeatherCondition(
                    dailyCode(
                        forecast.hourly,
                        hourlyCodes,
                        hoursByDate[day].orEmpty(),
                        daily.weatherCode[i]
                    )
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
     *    the day votes with the sky its own cloud cover makes ([skyCode], the provider's
     *    buckets): a light shower that does not label the day must not label its sky
     *    either, and until 24 set 2026 two drizzle hours could win a tie against it.
     *
     * Dates the hourly run does not reach keep the provider's code: it ends with
     * `forecast_days` and the daily one can outrun it. Aggregate better, never blank a row.
     */
    private fun dailyCode(
        hourly: HourlyDto,
        codes: List<Int>,
        hours: List<Int>,
        fallback: Int
    ): Int {
        if (hours.isEmpty()) return fallback
        val chance = hourly.precipitationProbabilityPct

        val hazards = hours.filter { AlertEngine.severeBucket(codes[it], chance.getOrNull(it)) != null }
        if (hazards.isNotEmpty()) return heaviest(hazards.map { codes[it] })

        // Hazard codes that failed the chance test are not wet hours of a lesser kind:
        // they are hours the app has decided not to believe.
        val wet = hours.filter { WmoCode.of(codes[it])?.let { w -> w.isPrecipitation && w.hazard == null } == true }
        // An empty `precipitationMm` is a cache entry written before the field was
        // requested: the amount clause simply cannot speak, and the hour count answers alone.
        val amounts = hourly.precipitationMm
        fun mm(of: List<Int>) = of.sumOf { amounts.getOrElse(it) { 0.0 } }
        val wetMm = mm(wet)
        if (wet.size >= WET_DAY_HOURS || wetMm >= WET_DAY_MM) {
            val weight: (List<Int>) -> Double =
                if (wetMm > 0.0) ::mm else { phaseHours -> phaseHours.size.toDouble() }
            val dominant = wet.groupBy { WmoCode.of(codes[it])!!.phase }.values
                .maxWithOrNull(
                    compareBy(weight).thenBy { phaseHours -> phaseHours.maxOf { severity(codes[it]) } }
                )!!
            return heaviest(dominant.map { codes[it] })
        }

        val daylight = hours.filter { hourly.isDay[it] == 1 }.ifEmpty { hours }
        return daylight.map { skyOf(codes[it], hourly.cloudCoverPct[it]) }
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
     * cloud cover makes. An unknown number is not a sky either. */
    private fun skyOf(code: Int, cloudCoverPct: Int): Int =
        if (WmoCode.of(code)?.phase == WmoCode.Phase.NONE) code else skyCode(cloudCoverPct)

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
                tempC = hourly.temperatureC[i]
            )
        }
    }

    /**
     * The hourly codes with [repairFog] applied, plus the one thing a series can say
     * that a single hour cannot: **fog is not one hour long** (Fase 26).
     *
     * Writing fog into an hour whose neighbours are both clear-aired nearly doubled
     * the fog↔non-fog transitions of the week — measured 6 Sep 2026 over 23 cities and
     * 3 864 hours: the provider's own series turns 10 times, the repaired one 18.
     * Every one of those extra turns is a row that changes character for an hour and
     * a line in `history.diff` that says nothing happened twice. Requiring one
     * neighbour below the threshold takes it to 14 and costs two rewrites out of 42.
     *
     * The requirement is deliberately **one-sided**. Removing a fog code the
     * provider's own visibility contradicts stays a per-hour decision: there is no
     * "run" argument for keeping a value the data disagrees with, and 68% of the fog
     * codes served are contradicted (median 4 km, worst 16.3 km). It is only the
     * INVENTING direction that has to be patient.
     */
    private fun HourlyDto.repairedCodes(): List<Int> =
        weatherCode.indices.map { i ->
            repairFog(
                code = weatherCode[i],
                visibilityM = visibilityM.getOrNull(i),
                cloudCoverPct = cloudCoverPct[i],
                fogPersists = fogPersists(i)
            )
        }

    /** Below the fog threshold at [i] and at a neighbour: the series' test for inventing fog. */
    private fun HourlyDto.fogPersists(i: Int): Boolean {
        fun low(at: Int) = visibilityM.getOrNull(at)?.let { v -> v <= FOG_VISIBILITY_M } == true
        return low(i) && (low(i - 1) || low(i + 1))
    }

    /**
     * The sky AT slot [i], as an instant (24 set 2026, for [mapHourly]'s rows): the slot's
     * own code when that is a sky or a fog — those Open-Meteo derives from the instant's
     * cloud and visibility — and when it is precipitation, which describes the hour BEFORE
     * the slot, the sky that instant's cloud cover makes, fog included by the same rule
     * [repairFog] applies. An unknown number is left as it came.
     */
    private fun HourlyDto.skyAt(i: Int, codes: List<Int>): Int {
        val code = codes[i]
        val wmo = WmoCode.of(code) ?: return code
        if (!wmo.isPrecipitation) return code
        return if (fogPersists(i)) WMO_FOG else skyCode(cloudCoverPct[i])
    }

    /**
     * `weather_code` with its fog checked against the same hour's visibility — the only
     * rewriting the app does to a provider code, and it applies Open-Meteo's own rule
     * rather than any meteorology of ours: `WeatherCode.swift:99` derives fog from
     * `visibility <= 1000` once precipitation is ruled out, and falls back on the cloud
     * cover otherwise. The served code is categorical and interpolated differently from the
     * continuous fields, so the two drift apart and the code loses: measured 22 Aug 2026 on
     * 8 Po Valley cities, `45` arrives with 10 km of visibility (01:00 and 03:00 at
     * Cavenago) while 160 m of dense fog is served as `3`, overcast. Both directions are
     * wrong and the second is the dangerous one.
     *
     * Deliberately surgical: only the fog verdict is revisited. Re-deriving the sky from
     * `cloud_cover` too — the obvious next step — moved 308 of those 1344 hours, 23%, which
     * is no longer repairing a defect but replacing the provider's classification wholesale.
     * The fog-only rule moves 15 hours, 1.1%, every one of them with its reason legible in
     * the visibility. Precipitation (>= 51) is never touched: it is not derived from
     * visibility, and thunderstorms need CAPE fields the app does not fetch.
     *
     * A null visibility (never seen in 20 cities across 5 continents, but the field is
     * model-dependent) leaves the code exactly as the provider sent it, and so does a
     * number outside [WmoCode]'s table: the repair corrects a verdict it can read, and it
     * cannot read that one.
     *
     * [fogPersists] is the series' permission to invent fog, and the `current` block
     * always has it: that block is an observation of NOW, and if the visibility is 300
     * metres right now then it is foggy right now — there is no run to require, because
     * there is no series. Persistence is a property of a forecast, not of a measurement.
     * Callers with a series pass [HourlyDto.repairedCodes]' answer; the one caller
     * without one passes `true` and gets the plain rule.
     */
    private fun repairFog(
        code: Int,
        visibilityM: Double?,
        cloudCoverPct: Int,
        fogPersists: Boolean
    ): Int {
        val wmo = WmoCode.of(code)
        if (wmo == null || wmo.isPrecipitation || visibilityM == null) return code
        val foggy = visibilityM <= FOG_VISIBILITY_M
        return when {
            wmo.isFog && !foggy -> skyCode(cloudCoverPct)
            !wmo.isFog && foggy && fogPersists -> WMO_FOG
            else -> code
        }
    }

    /** Open-Meteo's cloud cover buckets, `WeatherCode.swift:103`. */
    private fun skyCode(cloudCoverPct: Int): Int = when {
        cloudCoverPct < 20 -> 0
        cloudCoverPct < 50 -> 1
        cloudCoverPct < 80 -> 2
        else -> 3
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
