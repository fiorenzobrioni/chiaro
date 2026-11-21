package com.callbackdev.chiaro.data.mapper

import com.callbackdev.chiaro.data.remote.OpenMeteoModelForecastApi
import com.callbackdev.chiaro.data.remote.dto.CurrentDto
import com.callbackdev.chiaro.data.remote.dto.DailyDto
import com.callbackdev.chiaro.data.remote.dto.ForecastResponseDto
import com.callbackdev.chiaro.data.remote.dto.HourlyDto
import com.callbackdev.chiaro.data.remote.dto.ModelCodesResponseDto
import com.callbackdev.chiaro.data.remote.dto.UvResponseDto
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * The new model's week, put together into the one response the app already reads
 * (prova del nuovo modello, 21 nov 2026, branch `prova-nuovo-modello`).
 *
 * - **Each day is one model's, whole.** The days the regional model (MeteoSwiss ICON-CH,
 *   ItaliaMeteo ICON-2I) covers hour by hour are its own; the rest are ECMWF IFS's. A day is
 *   never half one model and half the other, and an hour's numbers are always one model's.
 * - **The two hazards the engine reads from the provider's code** (storm 95/96/99, freezing
 *   rain 56/57/66/67) come from ICON-D2 where it has them: on Open-Meteo neither ICON-CH nor
 *   IFS carries a thunderstorm. A code is a state's input, never a number on screen; over an
 *   hour with no measurable rain the engine already says «Temporali possibili».
 * - **The snow of an hour that is all snow and too warm for it is rain.** Open-Meteo serves
 *   ICON-CH's snow all or nothing (snowfall exactly 0.7 × the precipitation, or zero), and
 *   it has served all snow at 6 °C under rain (Malpensa, 24 Dec 2025). The threshold is the
 *   air temperature at which rain and snow are equally frequent, by relative humidity, from
 *   17.8 million observations (Jennings et al. 2018, Nature Communications 9:1148, Fig. 2):
 *   above it rain is the likelier phase. Only the regional hours, only an all-snow hour.
 * - **The UV index is Copernicus's** (CAMS), with clouds, ozone and aerosols: neither model
 *   serves one. Past CAMS's horizon there is no UV, and the tile is not drawn.
 * - **The visibility is ECMWF's**, all week, where the regional model has none (neither does):
 *   a number to read, never a fog vote (the engine reads no hour's own visibility).
 * - **The quarters stay the regional model's** for the hero's «cut in four» test only: none
 *   of these models computes the quarter, so the mapper draws no «Prossime due ore» for a
 *   composed response ([ForecastResponseDto.composedFrom]).
 *
 * Pure: the repository fetches, this decides. Null when the parts do not line up (other
 * hours, another offset, no day the regional model covers whole): the caller then falls back
 * to `best_match`, the path the app had before.
 */
object ForecastComposer {

    /** The provider's codes rules 1 and 2 of the engine read: thunderstorm, freezing rain. */
    val HAZARD_CODES = setOf(95, 96, 99, 56, 57, 66, 67)

    /** Open-Meteo's snow: centimetres of fresh snow per millimetre of water. */
    const val SNOW_CM_PER_MM = 0.7

    /**
     * The air temperature (°C) at which rain and snow fall equally often, for [rhPct]:
     * Jennings et al. 2018, Fig. 2, the 50% rain–snow threshold of each humidity bin —
     * 0.7 °C at 90–100%, 1.4 at 80–90, 2.2 at 70–80, 2.8 at 60–70, 3.7 at 50–60, 4.5 at
     * 40–50%. The paper gives no bin under 40%; the driest one stands for it.
     */
    fun snowThresholdC(rhPct: Int): Double = when {
        rhPct >= 90 -> 0.7
        rhPct >= 80 -> 1.4
        rhPct >= 70 -> 2.2
        rhPct >= 60 -> 2.8
        rhPct >= 50 -> 3.7
        else -> 4.5
    }

    /** Every millimetre of the hour served as snow: how Open-Meteo serves ICON-CH's snow. */
    fun allSnow(precipMm: Double?, snowCm: Double?): Boolean =
        precipMm != null && snowCm != null && snowCm > 0.0 &&
            abs(snowCm - SNOW_CM_PER_MM * precipMm) < ALL_SNOW_TOLERANCE_CM

    /** Warmer than the humidity's threshold: rain is likelier than snow. */
    fun tooWarmForSnow(tempC: Double?, rhPct: Int?): Boolean =
        tempC != null && rhPct != null && tempC > snowThresholdC(rhPct)

    fun compose(
        regional: ForecastResponseDto,
        regionalModel: String,
        global: ForecastResponseDto,
        codes: ModelCodesResponseDto?,
        uv: UvResponseDto?
    ): ForecastResponseDto? {
        if (regional.hourly.time != global.hourly.time) return null
        if (regional.daily.time != global.daily.time) return null
        if (regional.utcOffsetSeconds != global.utcOffsetSeconds) return null
        val offset = regional.utcOffsetSeconds
        val hazards = codes?.takeIf { it.utcOffsetSeconds == offset }
            ?.let { response ->
                response.hourly.time.zip(response.hourly.weatherCode)
                    .mapNotNull { (time, code) -> code?.takeIf { it in HAZARD_CODES }?.let { time to it } }
                    .toMap()
            }
            .orEmpty()
        val uvAt = uv?.takeIf { it.utcOffsetSeconds == offset }
            ?.let { it.hourly.time.zip(it.hourly.uvIndex).toMap() }
            .orEmpty()

        val times = regional.hourly.time
        val dates = regional.daily.time
        val dateOf = times.map { it.take(DATE_LENGTH) }
        val r = regional.hourly
        val g = global.hourly

        // The leading run of days the regional model has whole: its daily block and every
        // hour of the day. ICON-CH stops at its run + 120 hours, ICON-2I at + 72, mid-day.
        val regionalDates = buildSet {
            for ((d, date) in dates.withIndex()) {
                val hours = times.indices.filter { dateOf[it] == date }
                val whole = regional.daily.temperatureMaxC.getOrNull(d) != null && hours.isNotEmpty() &&
                    hours.all { r.temperatureC.getOrNull(it) != null }
                if (!whole) break
                add(date)
            }
        }
        if (regionalDates.isEmpty()) return null
        val own = times.indices.map { dateOf[it] in regionalDates }

        fun <T> pick(a: List<T?>, b: List<T?>): List<T?> =
            times.indices.map { i -> if (own[i]) a.getOrNull(i) else b.getOrNull(i) }

        val temperature = pick(r.temperatureC, g.temperatureC)
        val humidity = pick(r.humidityPct, g.humidityPct)
        val precipitation = pick(r.precipitationMm, g.precipitationMm)
        val snowIn = pick(r.snowfallCm, g.snowfallCm)

        // The snow guard, on the regional hours. A slot's amount is of the hour that ends at it,
        // and its air is read at both ends: the snow goes only if neither end is cold enough.
        val removed = DoubleArray(times.size)
        val snowfall = snowIn.mapIndexed { i, snow ->
            val warmEnd = tooWarmForSnow(temperature[i], humidity[i])
            val warmStart = i == 0 || temperature[i - 1] == null || humidity[i - 1] == null ||
                tooWarmForSnow(temperature[i - 1], humidity[i - 1])
            if (own[i] && allSnow(precipitation[i], snow) && warmEnd && warmStart) {
                removed[i] = snow ?: 0.0
                0.0
            } else snow
        }

        val injected = mutableMapOf<String, MutableList<Int>>()
        val weatherCode = pick(r.weatherCode, g.weatherCode).mapIndexed { i, code ->
            val hazard = hazards[times[i]]
            if (hazard != null && code !in HAZARD_CODES) {
                injected.getOrPut(dateOf[i]) { mutableListOf() } += hazard
                hazard
            } else code
        }
        val uvIndex = times.map { uvAt[it] }

        val hourly = HourlyDto(
            time = times,
            temperatureC = temperature,
            weatherCode = weatherCode,
            precipitationMm = precipitation,
            precipitationProbabilityPct = pick(r.precipitationProbabilityPct, g.precipitationProbabilityPct),
            isDay = pick(r.isDay, g.isDay),
            // ICON-CH and ICON-2I serve no visibility: ECMWF's fills it, all week from one source
            // (21 nov 2026). A number to read — the fog tile's kilometres, the canvas's thickness,
            // the reader's alerts — never a vote: the engine reads no hour's own visibility, and
            // ECMWF already votes on fog as a witness.
            visibilityM = times.indices.map { i ->
                (if (own[i]) r.visibilityM.getOrNull(i) else null) ?: g.visibilityM.getOrNull(i)
            },
            cloudCoverPct = pick(r.cloudCoverPct, g.cloudCoverPct),
            apparentTemperatureC = pick(r.apparentTemperatureC, g.apparentTemperatureC),
            humidityPct = humidity,
            dewPointC = pick(r.dewPointC, g.dewPointC),
            pressureMslHpa = pick(r.pressureMslHpa, g.pressureMslHpa),
            windSpeedKph = pick(r.windSpeedKph, g.windSpeedKph),
            windDirectionDeg = pick(r.windDirectionDeg, g.windDirectionDeg),
            windGustsKph = pick(r.windGustsKph, g.windGustsKph),
            uvIndex = uvIndex,
            cloudCoverLowPct = pick(r.cloudCoverLowPct, g.cloudCoverLowPct),
            cloudCoverMidPct = pick(r.cloudCoverMidPct, g.cloudCoverMidPct),
            cloudCoverHighPct = pick(r.cloudCoverHighPct, g.cloudCoverHighPct),
            snowfallCm = snowfall,
            showersMm = pick(r.showersMm, g.showersMm),
            freezingLevelM = pick(r.freezingLevelM, g.freezingLevelM),
            snowfallHeightM = pick(r.snowfallHeightM, g.snowfallHeightM),
            capeJkg = pick(r.capeJkg, g.capeJkg)
        )

        val rd = regional.daily
        val gd = global.daily
        fun <T> day(a: List<T?>, b: List<T?>): List<T?> =
            dates.indices.map { d -> if (dates[d] in regionalDates) a.getOrNull(d) else b.getOrNull(d) }
        val removedByDate = times.indices.groupBy { dateOf[it] }
            .mapValues { (_, hours) -> hours.sumOf { removed[it] } }
        val daily = DailyDto(
            time = dates,
            weatherCode = day(rd.weatherCode, gd.weatherCode).mapIndexed { d, code ->
                val extra = injected[dates[d]]
                if (extra != null && code !in HAZARD_CODES) extra.max() else code
            },
            temperatureMaxC = day(rd.temperatureMaxC, gd.temperatureMaxC),
            temperatureMinC = day(rd.temperatureMinC, gd.temperatureMinC),
            precipitationProbabilityMaxPct = day(rd.precipitationProbabilityMaxPct, gd.precipitationProbabilityMaxPct),
            // The day's highest UV only when CAMS has all its hours: a part of a day's maximum
            // is not the day's maximum.
            uvIndexMax = dates.map { date ->
                val values = times.indices.filter { dateOf[it] == date }.map { uvAt[times[it]] }
                if (values.size == HOURS_PER_DAY && values.all { it != null }) values.maxOf { it!! } else null
            },
            precipitationSumMm = day(rd.precipitationSumMm, gd.precipitationSumMm),
            snowfallSumCm = day(rd.snowfallSumCm, gd.snowfallSumCm).mapIndexed { d, sum ->
                sum?.let { (it - (removedByDate[dates[d]] ?: 0.0)).coerceAtLeast(0.0) }
            },
            windGustsMaxKph = day(rd.windGustsMaxKph, gd.windGustsMaxKph),
            rainSumMm = day(rd.rainSumMm, gd.rainSumMm).mapIndexed { d, sum ->
                sum?.let { it + (removedByDate[dates[d]] ?: 0.0) / SNOW_CM_PER_MM }
            },
            showersSumMm = day(rd.showersSumMm, gd.showersSumMm)
        )

        val current = composeCurrent(regional.current, hazards, uvAt).let { block ->
            // ECMWF's visibility of the same instant, where the regional block has none.
            val ecmwf = global.current.visibilityM.takeIf { global.current.time == block.time }
            block.copy(visibilityM = block.visibilityM ?: ecmwf)
        }
        val usedGlobal = dates.any { it !in regionalDates }
        return ForecastResponseDto(
            latitude = regional.latitude,
            longitude = regional.longitude,
            timezone = regional.timezone,
            utcOffsetSeconds = offset,
            elevation = regional.elevation,
            current = current,
            hourly = hourly,
            daily = daily,
            minutely15 = regional.minutely15,
            globalFrom = times.indices.firstOrNull { !own[it] }?.let { times[it] },
            composedFrom = listOfNotNull(
                regionalModel,
                OpenMeteoModelForecastApi.ECMWF.takeIf { usedGlobal },
                OpenMeteoModelForecastApi.HAZARD_MODEL.takeIf { hazards.isNotEmpty() },
                UV_SOURCE.takeIf { uvAt.values.any { it != null } }
            )
        )
    }

    /**
     * The regional model's `current` block, with the hazard of the hour its quarter falls in
     * (the slot that ends at or after it, as the mapper reads the block's chance), the UV of
     * the hour it is in, and the snow guard.
     */
    private fun composeCurrent(
        current: CurrentDto,
        hazards: Map<String, Int>,
        uvAt: Map<String, Double?>
    ): CurrentDto {
        val at = runCatching { LocalDateTime.parse(current.time) }.getOrNull() ?: return current
        val hourStart = at.truncatedTo(ChronoUnit.HOURS)
        val slot = if (at == hourStart) at else hourStart.plusHours(1)
        val hazard = hazards[slot.toString()]
        val snowGone = allSnow(current.precipitationMm, current.snowfallCm) &&
            tooWarmForSnow(current.temperatureC, current.humidityPct)
        return current.copy(
            weatherCode = if (hazard != null && current.weatherCode !in HAZARD_CODES) hazard else current.weatherCode,
            uvIndex = uvAt[hourStart.toString()],
            snowfallCm = if (snowGone) 0.0 else current.snowfallCm
        )
    }

    /** How [ForecastResponseDto.composedFrom] names Copernicus's UV. */
    const val UV_SOURCE = "cams_uv"

    private const val DATE_LENGTH = 10
    private const val HOURS_PER_DAY = 24

    /** Open-Meteo rounds the snow to 0.01 cm and the precipitation to 0.1 mm. */
    private const val ALL_SNOW_TOLERANCE_CM = 0.011
}
