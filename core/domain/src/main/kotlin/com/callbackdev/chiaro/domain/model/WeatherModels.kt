package com.callbackdev.chiaro.domain.model

import com.callbackdev.chiaro.domain.ConditionWord
import com.callbackdev.chiaro.domain.WmoCode
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.roundToInt
import kotlinx.serialization.Serializable

// Domain model, shaped after weather_data.json_full_sample.json. Times are java.time
// values in the location's local timezone; formatting happens at render time.

@Serializable
data class Coordinates(val lat: Double, val lon: Double)

/** A place as returned by city search; also identifies the report's subject.
 * Serializable because the saved-cities list persists as JSON in DataStore. */
@Serializable
data class City(
    val id: Long,
    val name: String,
    val region: String?,   // Open-Meteo admin1
    val country: String?,
    val coordinates: Coordinates,
    val timezone: String?,
    /**
     * ISO 3166-1 alpha-2 (`"IT"`), 9 set 2026: the official warnings are issued per
     * country and "Italia"/"Italy" in [country] is a localized name, not a
     * discriminator. Open-Meteo has always sent `country_code` and the mapper used to
     * drop it; the position path takes it from the geocoder. Nullable with a default so
     * every saved list written before this field decodes unchanged.
     */
    val countryCode: String? = null,
    /**
     * The municipality, 9 set 2026: Open-Meteo's `admin3` ("Comune di Segrate", for the
     * hamlet of Redecesio too), the geocoder's locality for the position. It is the
     * warning-zone index's fallback for a point on a zone border — never a display
     * name, which [name] already is.
     */
    val admin3: String? = null
) {
    val label: String
        get() = listOfNotNull(name, region ?: country).joinToString(", ")

    /** Stable cache/history key independent of float noise in coordinates. */
    val cacheKey: String
        get() = "${(coordinates.lat * 100).roundToInt()}:${(coordinates.lon * 100).roundToInt()}"
}

data class Location(
    val city: String,
    val region: String?,
    val country: String?,
    val coordinates: Coordinates,
    val timezone: String,
    val localTime: LocalDateTime
)

/**
 * A condition is its WMO code and nothing else (24 set 2026).
 *
 * It used to carry tweather's English description and an emoji beside the code —
 * `"Partly Cloudy ⛅"` — which no screen of this app ever showed (`WeatherText` picks
 * the word, `ChiaroIcons` the drawing) but which the history snapshots stored and the
 * Journal compared. Everything a reader or an engine needs is read off the code through
 * [WmoCode], the one table.
 */
data class WeatherCondition(val wmoCode: Int) {
    /** The table's entry, or null for a number Open-Meteo does not serve. */
    val wmo: WmoCode? get() = WmoCode.of(wmoCode)

    /** The word a screen prints for this condition. */
    val word: ConditionWord get() = ConditionWord.of(wmoCode)
}

data class Wind(
    val speedKph: Double,
    val directionCompass: String,
    val degree: Int,
    val gustKph: Double
)

/**
 * The rain around now: what fell in the provider's current quarter of an hour, what fell
 * in the hours that have just closed, and the chance for the hour under way.
 *
 * Reshaped 24 set 2026. It carried one `lastHourMm`, read off `current.precipitation`
 * and documented as "the sum of the preceding hour" — but `current` is Open-Meteo's
 * 15-minutely block (`"interval": 900`, and the docs' 15-minutely table says «Preceding
 * 15 minutes sum»). Measured that morning: Reykjavík `current` 0.4 mm, equal to the
 * 08:45 quarter, where the hour behind it held 2.7 mm; Singapore `current` 0.0 mm after
 * an hour of 0.3 mm. `ForecastOutcome` had been vouching for whole hours of dry weather
 * on a quarter of an hour of evidence. The hours now come from the hourly series, whose
 * values ARE sums of the preceding hour, each with the moment it ended.
 */
data class Precipitation(
    /** Millimetres in the 15 minutes ending at the provider's `current.time`. */
    val lastQuarterHourMm: Double,
    /**
     * The hours that have already closed, newest first, as the hourly series has them
     * (up to [PAST_HOURS]). Empty for a report built from a cache entry written before
     * the hourly amounts were fetched.
     */
    val pastHours: List<PastHour>,
    /**
     * The chance for the hour now under way, or null when the provider did not
     * forecast one — never a silent 0. Read off the slot that ENDS after now: an
     * hourly probability is Open-Meteo's «of the preceding hour», and the slot
     * labelled with the current hour describes the one already gone (24 set 2026).
     */
    val chancePct: Int?,
    /**
     * When [lastQuarterHourMm]'s quarter ended: the provider's `current.time`, which is
     * aligned to the quarter and can sit up to fifteen minutes before the fetch. Null
     * only where a report is built by hand.
     */
    val quarterEndedAt: Instant? = null
) {
    companion object {
        /** Three: enough that a reading every two hours, give or take the job's flex,
         * still leaves no hour between two readings unaccounted for. */
        const val PAST_HOURS = 3
    }
}

/** One closed hour: what fell in it and the temperature at its end. */
data class PastHour(
    val endedAt: Instant,
    val precipMm: Double,
    val tempC: Double
)

data class CurrentConditions(
    val condition: WeatherCondition,
    val tempC: Double,
    val feelsLikeC: Double,
    val humidityPct: Int,
    val dewPointC: Double,
    /** Null when the model behind this response does not carry visibility. */
    val visibilityKm: Double?,
    val pressureMb: Double,
    /**
     * The UV index right now, **null when the model behind this response does not
     * carry one** (24 set 2026) — the same sentence [DailyForecast.uvIndexMax] has
     * carried since 20 set, found out one block later. Measured on the live endpoint:
     * `models=icon_seamless` serves `current.uv_index: null`, and the non-nullable
     * field would have failed the whole report to parse rather than cost one tile.
     * The English `uvDescription` beside it went with it: nothing read it.
     */
    val uvIndex: Int?,
    val wind: Wind,
    val precipitation: Precipitation,
    /** Total cloud cover now, 0..100 — null only in a report built by hand. */
    val cloudCoverPct: Int? = null,
    /** The same cloud by layer (24 set 2026), null when the model does not split it. */
    val cloudLayers: CloudLayers? = null,
    /**
     * True when these values are not the provider's `current` block but the forecast for
     * this hour, because the block had grown too old to show (24 set 2026, the review's
     * suggestion 3). Set by [com.callbackdev.chiaro.domain.CurrentEstimate], never by the
     * mapper; every surface that prints it says so (DESIGN §1.1).
     */
    val estimated: Boolean = false
)

/**
 * The cloud cover split by height, 0..100 each (Open-Meteo's `cloud_cover_low/mid/high`:
 * below ~2 km, ~2-6 km, above ~6 km). They do not add up to the total: layers overlap.
 *
 * Why it is carried (24 set 2026): the total says how much of the sky is covered and
 * nothing about what that sky looks like — 70% of high, thin cloud is a veiled sun and
 * often a coloured sunset, 70% of low cloud is a grey day. The details and the sky's
 * verdicts say which one it is.
 */
data class CloudLayers(val lowPct: Int?, val midPct: Int?, val highPct: Int?) {

    enum class Layer { LOW, MID, HIGH }

    /**
     * The layer that makes the sky, or null when none does. A layer "makes" it when it
     * is at least [MIN_PCT] and at least twice each of the others: 60% high over 10% low
     * is a veiled sky; 50% low under 40% high is not one layer's sky, and naming one
     * would be a guess. A missing layer counts as nothing, not as a reason to name none.
     */
    fun dominant(): Layer? {
        val values = mapOf(Layer.LOW to (lowPct ?: 0), Layer.MID to (midPct ?: 0), Layer.HIGH to (highPct ?: 0))
        val (layer, top) = values.maxBy { it.value }
        if (lowPct == null && midPct == null && highPct == null) return null
        if (top < MIN_PCT) return null
        return layer.takeIf { values.filterKeys { it != layer }.values.all { other -> top >= 2 * other } }
    }

    companion object {
        /** Below this no layer is worth naming: the sky is essentially open. */
        const val MIN_PCT = 20
    }
}

/**
 * Concentrations in µg/m³ except [coMg] (mg/m³). Each one **null when the service did
 * not measure it** (24 set 2026): the mapper used to write `0.0` in its place, a clean
 * air nobody had observed. No screen reads these yet; the first one that does must not
 * inherit a zero.
 */
data class Pollutants(
    val pm25: Double?,
    val pm10: Double?,
    val o3: Double?,
    val no2: Double?,
    val so2: Double?,
    val coMg: Double?
)

/** Which scale the air is read on: the place's own. */
enum class AqiScale { US, EUROPEAN }

/**
 * The air: the US AQI (what rules and history have always stored), the European index
 * beside it, and which of the two this place reads (24 set 2026). An Italian reader's
 * bulletins speak the EEA's scale — 0-20 good to over 100 extremely poor — and a US
 * number on a US scale was a foreign unit on the one tile that most needs no translating.
 */
data class AirQuality(
    val aqiIndex: Int,
    val pollutants: Pollutants,
    val europeanAqi: Int? = null,
    val scale: AqiScale = AqiScale.US
) {
    /** The index the place reads: the European one only where it is served. */
    val shownIndex: Int get() = if (scale == AqiScale.EUROPEAN) europeanAqi ?: aqiIndex else aqiIndex
}

/**
 * The pollen load classes, as MeteoSwiss publishes them («Threshold values for pollen
 * load classes of allergenic pollen types»): nothing, then low, moderate, high and **very
 * high** — the fifth arrived on 24 set 2026, when the thresholds became per species. Until
 * then every species shared one scale (1/30/100 grains/m³) and the top was «high» for a
 * grass count of 100 and of 1 000 alike.
 */
enum class PollenLevel(val label: String) {
    NONE("None"),
    LOW("Low"),
    MODERATE("Moderate"),
    HIGH("High"),
    VERY_HIGH("Very high")
}

data class PollenReport(
    val grass: PollenLevel,
    val tree: PollenLevel,
    val weed: PollenLevel
)

/**
 * The sun and the moon over the report's day.
 *
 * **Nullable since Fase 16e**, and not for tidiness: these values come from
 * [com.callbackdev.chiaro.domain.sky.AstronomyEngine] now rather than from the
 * provider's daily block, and above the Arctic circle in June there is no sunrise to
 * have. The old type could not say that — it could only carry some other time and
 * let the reader assume it meant something.
 */
data class Astronomical(
    val sunrise: LocalTime?,
    val sunset: LocalTime?,
    val moonPhase: MoonPhase,
    /** Sunset − sunrise; null on the days one of them does not happen. */
    val daylightDuration: Duration?
)

/**
 * `"10h 52m"` — how every surface writes a span of daylight.
 *
 * Lives in the domain rather than next to `weather_data.json`'s builder because it
 * has three readers in two layers now: the JSON, `README.md`'s `## Astronomy`, and
 * the history snapshot that `history.diff` is a diff OF. A second copy of the format
 * is a second answer to "how long was today", which is the one thing the sky module
 * was built not to have.
 */
fun Duration.hhMm(): String = "${toHours()}h ${toMinutesPart()}m"

data class HourlyForecast(
    /**
     * The hour on the city's own clock, which is what every surface prints and what
     * the engines compare against a local `now`.
     *
     * It is **not** the string the provider sent (20 set 2026). Open-Meteo builds its
     * series as `UTC + utc_offset_seconds` with ONE offset for the whole response, so
     * after a DST change its labels drift an hour from the real wall clock; the mapper
     * re-expresses them through [at]. See `ForecastResponseDto.utcOffsetSeconds`.
     *
     * Consequence worth knowing before writing anything that indexes on this: on the
     * day a zone falls back, **two consecutive hours carry the same value** (02:00
     * twice), and on the day it springs forward one is missing. It is a label, not an
     * identity — [at] is the identity.
     */
    val time: LocalDateTime,
    /**
     * The same hour as the moment it actually is (20 set 2026).
     *
     * Unique across the list where [time] is not, so it is what a `LazyRow` key and
     * any other identity must be built from; exact where `time.atZone(zone)` only
     * guesses, since that call has to pick one of the two offsets an ambiguous local
     * hour can wear. Four readers were re-deriving it and now read it: the strip's
     * day/night flag, the arc widget's series, and `RainbowWindow`'s two ends.
     */
    val at: Instant,
    val tempC: Double,
    val condition: WeatherCondition,
    /**
     * Chance of precipitation, 0..100, or **null when the provider did not forecast
     * one for this hour** (Fase 26). It used to be a non-nullable Int that the mapper
     * filled with `0` whenever the field came back null — which is a claim, not a
     * fallback: "no chance of rain" is a forecast, and "we were not told" is not.
     * Probed on twelve places across five continents and never seen null, so this
     * closes a rule rather than a bug; every reader either compares it (where null
     * simply does not meet a threshold) or prints it (where null prints as null).
     */
    val precipChancePct: Int?,
    /**
     * Total cloud cover, 0..100 (Fase 16a). Open-Meteo has been sending it since Fase
     * 13c, where it repairs `weather_code`'s unreliable fog inside the mapper, but it
     * never reached the domain: nothing rendered it and nothing reasoned about it. The
     * sky module does — whether tonight's sunset is worth walking outside for is a
     * statement about this number and almost nothing else — so it stops being a local
     * variable of the fog repair and becomes part of the hour.
     *
     * Rendered nowhere still: `weather_data.json` has no `cloud_cover` key and gains
     * none. The condition emoji already says what the sky looks like.
     *
     * Non-null, and deliberately so. The first draft made it nullable to give the sky
     * verdict's `? unknown` an input that could produce it — but the mapper cannot
     * produce an hour without a cloud cover (the fog repair reads the same column one
     * step earlier and would fail first), so the null would have been a state the type
     * allows and the data never holds. Every reader would then have written `?: 0`,
     * and 0 % cloud is "clear sky": the nullable version was the shortest route to the
     * exact lie it was meant to prevent. The absence 16d actually has to handle is a
     * missing HOUR — an event past the end of [WeatherReport.hourly] — which the list
     * expresses on its own.
     */
    val cloudCoverPct: Int,
    // 24 set 2026 — the rest of an hour, so that the forecast for THIS hour can stand in
    // for a `current` block gone stale ([com.callbackdev.chiaro.domain.CurrentEstimate]),
    // and the headline can see a gale coming. All nullable: a model may lack one, and a
    // cache entry from before carries none. Instant values at [time], except [gustKph],
    // which is the strongest gust of the hour that STARTS at [time] (see the mapper).
    val feelsLikeC: Double? = null,
    val humidityPct: Int? = null,
    val dewPointC: Double? = null,
    val pressureMb: Double? = null,
    val windKph: Double? = null,
    val windDirectionDeg: Int? = null,
    val gustKph: Double? = null,
    val uvIndex: Double? = null,
    val visibilityKm: Double? = null,
    val cloudLayers: CloudLayers? = null
)

data class DailyForecast(
    val date: LocalDate,
    val highC: Double,
    val lowC: Double,
    val condition: WeatherCondition,
    /**
     * The day's peak probability of precipitation, **null when the model behind this
     * response does not carry one** — the hourly field has travelled that way since
     * the 6 set 2026 review of the provider's codes, and the daily one used to be
     * coerced to `0`, which is the one value a probability must never be invented as:
     * a zero is a forecast of no rain, and "we were not told" is not that. Null goes
     * all the way to the surfaces, which print absence rather than a number nobody
     * forecast, and the snapshot leaves the key out.
     */
    val precipPct: Int?,
    /**
     * The day's PEAK UV (Open-Meteo `uv_index_max`) — never the instant reading
     * [CurrentConditions.uvIndex]: under a "Today" heading only the maximum says
     * anything, since at 23:52 the current index is 0 whatever the day was (which is
     * exactly what the README used to print). On Today's own details grid the current
     * reading is the value and this rides as a note, which is a different question and
     * `TodayScreen.Details` answers it there.
     *
     * **Null when the model behind this response does not carry one** (20 set 2026).
     * It was coerced to `0` — and a zero UV is not an absence, it is a forecast of a
     * sun that cannot burn, printed under "Nessuna protezione necessaria". The same
     * sentence [precipPct] carries, for the same reason, about the field beside it in
     * the same block.
     *
     * It used to travel with a `uvDescription` label. That was tweather's JSON
     * vocabulary — English, never localized, and read by nothing in this app but the
     * test that asserted the mapper had written it, the way `WeatherCondition.description`
     * is (see `WeatherText`'s own note). A nullable index forced the question of what
     * its label should say when there is no index, and the honest answer was that
     * nobody was asking.
     */
    val uvIndexMax: Int?,
    /**
     * How much falls (mm, rain and melted snow together) and over how many hours; the
     * snow on its own in cm; the strongest gust in km/h (24 set 2026). Null when the
     * model or the cached response does not carry it — never a zero, which would be a
     * dry, calm day nobody forecast.
     */
    val precipMm: Double? = null,
    val precipHours: Double? = null,
    val snowCm: Double? = null,
    val gustMaxKph: Double? = null,
    /**
     * The rain alone, in mm: rain and showers, without the snow's water that [precipMm]
     * also carries. What every line that says «di pioggia» prints — 0.4 cm of snow is
     * 0.6 mm of [precipMm], and «0,6 mm di pioggia» over a day of snow was a rain nobody
     * forecast. Null when the response does not split it and the day has snow in it: the
     * total can then not be told apart, and a guess is not a forecast.
     */
    val rainMm: Double? = null
)

enum class CacheStatus { HIT, MISS }

data class SystemInfo(
    val source: String,
    val lastSync: Instant,
    val cacheStatus: CacheStatus,
    val responseTimeMs: Long
)

data class WeatherReport(
    val location: Location,
    val current: CurrentConditions,
    val airQuality: AirQuality?,   // air quality API can fail independently of forecast
    val pollen: PollenReport?,     // Open-Meteo pollen coverage is Europe-only
    val astronomical: Astronomical,
    val hourly: List<HourlyForecast>,
    val daily: List<DailyForecast>,
    val systemInfo: SystemInfo
)
