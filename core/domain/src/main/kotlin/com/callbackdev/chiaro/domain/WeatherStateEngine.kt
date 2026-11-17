package com.callbackdev.chiaro.domain

import com.callbackdev.chiaro.domain.model.CloudLayers
import kotlin.math.roundToInt

/**
 * What an hour IS — its icon and its word — decided from the physical fields instead of
 * relayed from `weather_code` (25 set 2026). Every rule and threshold, with its reason and
 * its measurement, is in `docs/STATES.md`: a rule changes there and here together.
 *
 * **Numbers are the model's; states are the app's.** Nothing here changes an amount, a
 * probability or a temperature: the engine reads them and returns one [WmoCode] number.
 * Why it exists, measured on 26 cities and 4342 hours the morning it was written:
 *
 * - Open-Meteo writes a snow code from 0.01 cm/h and a drizzle code from 0.01 mm/h, ten
 *   times under the 0.1 mm its own probability counts: a snowflake over «0%» is the
 *   provider's arithmetic, not a forecast. 88 hours carried a precipitation code under 20%.
 * - For the ICON family the code is DWD's `ww`, relayed as it is: 19 hours said «overcast»
 *   over a measurable amount of the same response, and in Italy the sky code disagreed
 *   with the cloud cover in 10–29% of the hours (0–1% elsewhere, where Open-Meteo derives
 *   the code from that same cloud cover).
 * - Open-Meteo calls every shower under 1.3 mm/h drizzle, and 1 mm/h «dense drizzle».
 *
 * One hour's input is the hour as the rows show it: the amounts, the probability and the
 * provider's code of the interval (Open-Meteo's next slot), the temperature, cloud and
 * visibility of the instant it starts. The rules, in order — the first that answers wins:
 *
 * 1. **Thunderstorm**: the provider's 95/96/99, **at any chance of rain** (17 nov 2026:
 *    against the METARs' thunder a storm code under 20% is a storm as often as one over it,
 *    and the floor it had until then silenced one storm day in three in the Po valley). The
 *    app has no CAPE, so it neither invents a storm nor removes one; over an hour with no
 *    measurable rain it is [WmoCode.THUNDERSTORM_POSSIBLE], still a hazard.
 * 2. **Freezing rain**: the provider's 56/57/66/67, with no probability floor, as
 *    [AlertEngine] has always had it. Deducing it from liquid rain at ≤ 0 °C is decided
 *    and waits for a winter measurement: the September one never saw such an hour.
 * 3. **Measurable and probable precipitation** (≥ [MEASURABLE_MM], chance ≥
 *    [PROBABLE_PCT] or unknown; heavy snow at any chance, as [AlertEngine] has it), phase
 *    from the model's own split: rain and snow when
 *    both parts are measurable, snow or snow showers when the snow's water dominates,
 *    otherwise showers when the convective part is at least half, else drizzle, light
 *    rain, rain or heavy rain on the AMS ladder (0.5, 2.5, 7.6 mm/h).
 * 4. **Likely** (chance ≥ [LIKELY_PCT], nothing measurable): [WmoCode.RAIN_LIKELY] or
 *    [WmoCode.SNOW_LIKELY], phase from the nearest measured hour within
 *    [LIKELY_PHASE_REACH_HOURS], else snow at ≤ [RAIN_SNOW_C].
 * 5. **Fog** where **two models see it** ([fogVotes], 17 nov 2026): one vote per witness
 *    whose visibility is [FOG_VISIBILITY_M] or less, and one for the hour's own model when its
 *    air is saturated in a calm ([FOG_SATURATED_PCT], [FOG_CALM_KPH]); `best_match`'s
 *    visibility is not read, so the rule is the same before and after 30 Nov 2026, when DWD
 *    removes it from ICON. Kept where the provider says fog, invented only where the two
 *    votes persist into a neighbouring hour of the same series (Fase 26), and in both cases
 *    under air the models agree is near saturation: the mean of every model's humidity
 *    without the highest and the lowest, at [FOG_MEAN_HUMIDITY_PCT] or more. **One model's
 *    word never makes fog**, and the provider's code never does alone: measured against the
 *    METARs of 26 airports it says fog 4 361 times for 955 seen, 2 425 of them over air
 *    clear for five kilometres. Freezing fog at ≤ 0 °C.
 * 6. **Sky** from the share of the sun's light the cloud takes, layer by layer
 *    ([CloudLayers.lightTaken], 26 ott 2026), on Open-Meteo's own buckets (20/50/80): the
 *    same arithmetic the sky drawn at the top of the page lights its clouds with.
 *
 * Every input may be missing — an older cache entry, a model that does not carry it — and
 * a missing input switches off only the rule that needs it: that part of the decision
 * falls back on the provider's code, as the app did before this engine. Fog is the one
 * exception: with fewer than two votes there is no fog, whatever is missing, because the
 * code's fog is the one number measured wrong more often than right.
 */
object WeatherStateEngine {

    /** The smallest amount a response can state (it rounds to 0.1 mm), and the one the
     * probability counts from («more than 0.1 mm»). Under it is a trace. */
    const val MEASURABLE_MM = 0.1

    /** Under this chance the ensemble does not back the model: [AlertEngine]'s floor for
     * «Maltempo», and the chance under which bulletins do not name precipitation. */
    const val PROBABLE_PCT = 20

    /** «Likely» in the National Weather Service's wording starts at 60%. */
    const val LIKELY_PCT = 60

    /** The WMO's fog, and the provider's: a witness whose visibility is this or less votes for it. */
    const val FOG_VISIBILITY_M = 1000.0

    /**
     * The hour's own model votes for fog when its air is at least this humid... (17 nov
     * 2026, `docs/STATES.md` §11): the vote it has without a visibility, the one DWD stops
     * serving for ICON on 30 Nov 2026. Saturation in a calm is how fog is diagnosed where a
     * model has no visibility (Zhou and Du, 2010, NCEP's multi-model fog). The thresholds sit
     * on a plateau (98–100%, 8–16 km/h, 93–95% for the mean: CSI 0.285–0.299), and 98 / 12 /
     * 93 is ahead of the 99 / 12 / 94 first proposed on every slice measured.
     */
    const val FOG_SATURATED_PCT = 98

    /** ...and its wind this light or lighter, km/h: about 7 knots, the National Weather
     * Service's line between radiation fog and the wind that mixes it away. */
    const val FOG_CALM_KPH = 12.0

    /** How many models must see the fog: two, never one model's word (Lissone, 26 Oct 2026:
     * 140 m from one model, three others 24–28 km away, and part-sunny on the spot). */
    const val FOG_VOTES = 2

    /**
     * The air the models agree on: the mean of every humidity of the hour, its own model's and
     * the witnesses', without the highest and the lowest (17 nov 2026, the committente's
     * idea). Under it a fog does not stand, however many vote: a filter, never a vote. As a
     * number of its own it is worse than the hour's own model (5.6 points off the METARs
     * against 5.4, and 5.7 against 4.4 in saturated air over the Po valley), so it decides
     * nothing else and is never printed.
     */
    const val FOG_MEAN_HUMIDITY_PCT = 93.0

    /** Open-Meteo's snowfall per millimetre of water: «7 cm snow = 10 mm water». */
    const val SNOW_CM_PER_MM = 0.7

    /** Mean 2 m temperature at which rain and snow are equally likely over the northern
     * hemisphere's land (Jennings et al., Nature Communications, 2018). Only used when
     * the model offers no phase of its own. */
    const val RAIN_SNOW_C = 1.0

    /** How far a likely hour looks for a measured one to borrow the phase from. */
    const val LIKELY_PHASE_REACH_HOURS = 3

    /** One hour, as a row shows it. Every field but [providerCode] may be unknown. */
    data class Hour(
        /** The provider's code for the interval: what falls during the hour. */
        val providerCode: Int,
        /** The provider's code at the instant the hour starts: its sky or fog. */
        val providerSkyCode: Int = providerCode,
        /** Total precipitation in the hour, mm: rain, showers and the snow's water. */
        val precipitationMm: Double? = null,
        /** Snowfall in the hour, cm. */
        val snowfallCm: Double? = null,
        /** Convective precipitation in the hour, mm, of ANY phase (see `rainOf`). */
        val showersMm: Double? = null,
        val chancePct: Int? = null,
        val temperatureC: Double? = null,
        val cloudCoverPct: Int? = null,
        /**
         * The permission to invent fog. Null — the default — lets the series decide: two
         * models must see it in a neighbouring hour too (Fase 26, [fogPersists]). An hour
         * judged on its own (the `current` block) passes the verdict of the series it
         * belongs to, [fogPersists] on the row it falls in: a fog the strip does not draw
         * is not drawn above it either (26 ott 2026, Lissone).
         */
        val fogPersists: Boolean? = null,
        /** The cover by height, at the same instant: the sky is the light they take. */
        val cloudLayers: CloudLayers? = null,
        /** Relative humidity at the instant the hour starts, %: the own model's fog vote, and
         * one of the humidities the fog's mean is made of. */
        val humidityPct: Int? = null,
        /** Wind at 10 m at the instant the hour starts, km/h: the calm the own model's fog
         * vote needs (17 nov 2026). */
        val windKph: Double? = null,
        /**
         * The same hour from the other model families (26 ott 2026), `best_match` itself
         * left out: they vote on fog ([fogVotes]) and are never printed. Empty — no
         * response, an older cache entry — and the hour has one vote at most: no fog.
         */
        val witnesses: List<Witness> = emptyList()
    )

    /**
     * Another model family's reading of the same instant: it votes for fog with its
     * visibility, adds its humidity to the fog's mean, and is never printed. `best_match`'s
     * own visibility is not read (17 nov 2026): whatever Open-Meteo serves there — ICON's
     * until 30 Nov 2026, nothing or a witness's copy after — the vote is the same.
     */
    data class Witness(
        val visibilityM: Double? = null,
        val humidityPct: Int? = null
    )

    /**
     * The models that see fog at [hour]'s instant (17 nov 2026, rule 5): one vote per witness
     * with a visibility of [FOG_VISIBILITY_M] or less, and one for the hour's own model when
     * its air is at [FOG_SATURATED_PCT] or more in a wind of [FOG_CALM_KPH] or less — a model
     * with no humidity or no wind does not vote. Fog needs [FOG_VOTES]. Public for the
     * `current` block, which judges its moment on the series of its rows ([fogPersists]).
     */
    fun fogVotes(hour: Hour): Int {
        val seen = hour.witnesses.count { w -> w.visibilityM?.let { it <= FOG_VISIBILITY_M } == true }
        val rh = hour.humidityPct
        val wind = hour.windKph
        val own = rh != null && wind != null && rh >= FOG_SATURATED_PCT && wind <= FOG_CALM_KPH
        return seen + if (own) 1 else 0
    }

    /**
     * The humidity the models agree on at [hour]'s instant (17 nov 2026): its own model's and
     * every witness's, without the highest and the lowest when there are three or more, and
     * averaged. Null when nobody has one. Read only as rule 5's filter, never printed.
     */
    fun fogMeanHumidityPct(hour: Hour): Double? {
        val all = (listOfNotNull(hour.humidityPct) + hour.witnesses.mapNotNull { it.humidityPct }).sorted()
        if (all.isEmpty()) return null
        val kept = if (all.size >= 3) all.subList(1, all.size - 1) else all
        return kept.average()
    }

    /**
     * The state of every hour of a series, in order. [likely] switches rule 4 off for a
     * caller that is describing what is happening rather than what may: rain or snow the
     * ensemble expects but nothing measures is not the present. It does not touch rule 1 —
     * a storm the provider sees over a dry quarter is «Temporali possibili» now too, and
     * the warning is the present's.
     */
    fun states(hours: List<Hour>, likely: Boolean = true): List<Int> =
        hours.indices.map { i -> stateAt(hours, i, likely) }

    /** One hour on its own: rule 4 then has no neighbour to take a phase from. */
    fun state(hour: Hour, likely: Boolean = true): Int = stateAt(listOf(hour), 0, likely)

    private fun stateAt(hours: List<Hour>, i: Int, likely: Boolean): Int {
        val hour = hours[i]
        val provider = WmoCode.of(hour.providerCode)
        val chance = hour.chancePct
        val probable = chance == null || chance >= PROBABLE_PCT
        val mm = hour.precipitationMm

        // 1. Thunderstorm, at any chance: the chance is of rain, not of lightning.
        if (provider?.hazard == AlertEngine.SevereBucket.THUNDER) {
            return if (mm == null || measurable(mm)) hour.providerCode
            else WmoCode.THUNDERSTORM_POSSIBLE.code
        }
        // 2. Freezing rain, from the provider.
        if (provider?.phase == WmoCode.Phase.FREEZING) return hour.providerCode

        // 3. What falls. Without an amount the provider's code is all there is.
        if (mm == null) {
            if (provider?.isPrecipitation == true) return hour.providerCode
        } else if (measurable(mm)) {
            val state = falling(hour, mm)
            // An improbable amount is a trace of the model's, and the hour is its sky —
            // except heavy snow, which [AlertEngine] warns about at any chance: the
            // engine removes a distortion, never a warning.
            if (state != null && (probable || WmoCode.of(state)?.hazard == AlertEngine.SevereBucket.SNOW)) {
                return state
            }
        }

        // 4. Likely, with nothing to measure.
        if (likely && mm != null && !measurable(mm) && chance != null && chance >= LIKELY_PCT) {
            return if (likelySnow(hours, i)) WmoCode.SNOW_LIKELY.code else WmoCode.RAIN_LIKELY.code
        }

        // 5. Fog, then 6. the sky.
        return fogOrSky(hour, hour.fogPersists ?: persists(i) { hours.getOrNull(it)?.let(::fogVotes) })
    }

    /**
     * Two models see fog here and in a neighbouring hour: the series' test for INVENTING fog
     * (Fase 26, on the votes since 17 nov 2026). Writing fog into an hour whose neighbours
     * are both clear-aired nearly doubled the fog↔non-fog transitions of the week (23
     * cities, 6 Sep 2026: 10 in the provider's series, 18 repaired, 14 with this rule).
     *
     * Public, on the bare counts — [fogVotes] of each hour of the series — for a caller that
     * judges one moment of the series on its own (the `current` block), so that the moment
     * and its row agree on what is fog.
     */
    fun fogPersists(votes: List<Int?>, i: Int): Boolean = persists(i) { votes.getOrNull(it) }

    private fun persists(i: Int, votesAt: (Int) -> Int?): Boolean {
        fun seen(at: Int) = (votesAt(at) ?: 0) >= FOG_VOTES
        return seen(i) && (seen(i - 1) || seen(i + 1))
    }

    /** Rule 3 on an hour with a measurable, probable amount; null when the phase cannot be
     * told and the provider says nothing falls — then the hour is its sky. */
    private fun falling(hour: Hour, mm: Double): Int? {
        val snowWater = hour.snowfallCm?.let { it / SNOW_CM_PER_MM }
            ?: phaseFromProvider(hour, mm)
            ?: return null
        val liquid = (mm - snowWater).coerceAtLeast(0.0)
        val showers = hour.showersMm
        // Read only when `showers` is missing: the provider's code then says whether the
        // hour is convective.
        val providerConvective = WmoCode.of(hour.providerCode) in PROVIDER_SHOWERS
        if (measurable(liquid) && measurable(snowWater)) {
            // Rain and snow together is not a hazard of its own, and must not hide one:
            // a part that is heavy on its own keeps its warning and names the hour.
            val cm = snowWater * SNOW_CM_PER_MM
            return when {
                cm >= SNOW_HEAVY_CM -> WmoCode.SNOW_HEAVY.code
                liquid >= RAIN_MODERATE_MAX_MM -> WmoCode.RAIN_HEAVY.code
                mm < RAIN_LIGHT_MAX_MM -> WmoCode.RAIN_AND_SNOW_LIGHT.code
                else -> WmoCode.RAIN_AND_SNOW.code
            }
        }
        if (snowWater > liquid) {
            val cm = snowWater * SNOW_CM_PER_MM
            if (if (showers != null) showers >= snowWater / 2 else providerConvective) {
                return if (cm >= SNOW_HEAVY_CM) WmoCode.SNOW_SHOWERS_HEAVY.code else WmoCode.SNOW_SHOWERS_SLIGHT.code
            }
            return when {
                cm < SNOW_MODERATE_CM -> WmoCode.SNOW_SLIGHT.code
                cm < SNOW_HEAVY_CM -> WmoCode.SNOW_MODERATE.code
                else -> WmoCode.SNOW_HEAVY.code
            }
        }
        // `showers` may hold convective snow too: only the part the liquid can account
        // for is a shower of rain.
        // Without `showers` (a cache entry from before it was asked for, a model that does
        // not split) the provider's own shower codes say whether the hour is convective.
        if (if (showers != null) minOf(showers, liquid) >= liquid / 2 else providerConvective) {
            return when {
                liquid < RAIN_LIGHT_MAX_MM -> WmoCode.SHOWERS_SLIGHT.code
                liquid < RAIN_MODERATE_MAX_MM -> WmoCode.SHOWERS_MODERATE.code
                else -> WmoCode.SHOWERS_HEAVY.code
            }
        }
        return when {
            liquid < DRIZZLE_MAX_MM -> WmoCode.DRIZZLE_LIGHT.code
            liquid < RAIN_LIGHT_MAX_MM -> WmoCode.RAIN_SLIGHT.code
            liquid < RAIN_MODERATE_MAX_MM -> WmoCode.RAIN_MODERATE.code
            else -> WmoCode.RAIN_HEAVY.code
        }
    }

    /**
     * The snow's water when the response has no `snowfall`: all of it when the provider's
     * code is snow, none when it is rain; with no precipitation code either, the 2 m
     * temperature decides. Null only when not even that is known.
     */
    private fun phaseFromProvider(hour: Hour, mm: Double): Double? {
        val provider = WmoCode.of(hour.providerCode)
        return when {
            provider?.isSnow == true -> mm
            provider?.isPrecipitation == true -> 0.0
            hour.temperatureC != null -> if (hour.temperatureC <= RAIN_SNOW_C) mm else 0.0
            else -> null
        }
    }

    /**
     * The phase a likely hour takes: its own, when the model gave it one — a trace of snow
     * or rain, or a precipitation code, is the model's phase for this very hour —, else
     * the nearest hour within reach that has a measurable amount, else the temperature.
     */
    private fun likelySnow(hours: List<Hour>, i: Int): Boolean {
        val own = hours[i]
        val ownMm = own.precipitationMm ?: 0.0
        val ownSnow = own.snowfallCm
        if (ownMm > 0.0 && ownSnow != null) return ownSnow / SNOW_CM_PER_MM > ownMm / 2
        WmoCode.of(own.providerCode)?.takeIf { it.isPrecipitation }?.let { return it.isSnow }
        for (distance in 1..LIKELY_PHASE_REACH_HOURS) {
            for (j in intArrayOf(i - distance, i + distance)) {
                val near = hours.getOrNull(j) ?: continue
                val mm = near.precipitationMm ?: continue
                if (!measurable(mm)) continue
                val snowWater = near.snowfallCm?.let { it / SNOW_CM_PER_MM }
                    ?: phaseFromProvider(near, mm)
                    ?: continue
                return snowWater > mm - snowWater
            }
        }
        val t = hours[i].temperatureC ?: return false
        return t <= RAIN_SNOW_C
    }

    /**
     * Rules 5 and 6: fog where two models see it (17 nov 2026), then the sky of the cloud
     * cover. A null cloud cover leaves the provider's sky.
     *
     * Fog is [FOG_VOTES] votes ([fogVotes]) at the hour, kept by the provider's fog code or
     * persisting into a neighbouring hour of the series ([persists]), under air whose mean
     * humidity ([fogMeanHumidityPct]) is [FOG_MEAN_HUMIDITY_PCT] or more. One model's
     * visibility is the weakest number it forecasts, and one model alone saw the fog over
     * Lissone on 26 Oct 2026 and over Cavenago on 17 Nov 2026 (ICON-D2 240 m; the four
     * witnesses 1.4–18 km; the airports around 2–7 km in haze; outside, the next village in
     * sight). The provider's code never decides alone: it contradicts the same response's
     * visibility 68% of the times it says fog.
     *
     * Measured against the METARs of 26 airports over October 2026 and December–January
     * (`docs/STATES.md` §11, 1 442 fogs): CSI 0.299 against 0.283 for the rule it replaces as
     * that would run from December (0.285 while ICON's visibility lasts); against it, 16% fewer
     * false fogs and 17% fewer over air clear for five kilometres, 2% fewer real ones caught,
     * and ahead in the Po valley (0.254 against 0.248), elsewhere (0.336 against 0.310) and in
     * both periods.
     */
    private fun fogOrSky(hour: Hour, persists: Boolean): Int {
        val instant = WmoCode.of(hour.providerSkyCode)
        val fog = fogVotes(hour) >= FOG_VOTES &&
            (instant?.isFog == true || persists) &&
            (fogMeanHumidityPct(hour)?.let { it >= FOG_MEAN_HUMIDITY_PCT } == true)
        if (fog) {
            val t = hour.temperatureC
            return if (t != null && t <= 0.0) WmoCode.RIME_FOG.code else WmoCode.FOG.code
        }
        val cloud = hour.cloudCoverPct
        return when {
            cloud != null -> skyOf(cloud, hour.cloudLayers)
            instant != null && instant.phase == WmoCode.Phase.NONE && !instant.isFog &&
                instant.hazard == null -> hour.providerSkyCode
            else -> WmoCode.OVERCAST.code
        }
    }


    /**
     * At least [MEASURABLE_MM], give or take the arithmetic: the parts of an hour are
     * differences of numbers rounded to 0.1, and 0.3 − 0.2 is 0.0999… in a Double.
     */
    private fun measurable(mm: Double): Boolean = mm >= MEASURABLE_MM - 1e-9

    /**
     * Rule 6: the sky a person sees, the share of the sun's light the cloud takes
     * ([CloudLayers.lightTaken]) on the buckets below, rather than the share of the sky the
     * cloud covers (26 ott 2026). 90% of high veil casts shadows, and the total called it
     * «overcast»: measured against the satellite on 4 380 daylight hours of 23 airports in
     * October 2026, the total said overcast under a visible sun in 17.5% of them, the light
     * in 5.8%.
     */
    fun skyOf(cloudCoverPct: Int, layers: CloudLayers?): Int =
        skyOf((CloudLayers.lightTaken(cloudCoverPct, layers) * 100).roundToInt())

    /** Open-Meteo's cloud cover buckets (`WeatherCode.swift`). */
    fun skyOf(cloudCoverPct: Int): Int = when {
        cloudCoverPct < 20 -> WmoCode.CLEAR.code
        cloudCoverPct < 50 -> WmoCode.MAINLY_CLEAR.code
        cloudCoverPct < 80 -> WmoCode.PARTLY_CLOUDY.code
        else -> WmoCode.OVERCAST.code
    }

    /** Under this, rain is drizzle: the «a few drops» end of light rain. */
    private const val DRIZZLE_MAX_MM = 0.5

    /** The AMS Glossary's rain intensities: light under 2.5 mm/h, heavy from 7.6 — the
     * same upper floors Open-Meteo uses for 61/63/65 and 80/81/82. */
    private const val RAIN_LIGHT_MAX_MM = 2.5
    private const val RAIN_MODERATE_MAX_MM = 7.6

    /** The provider's convective codes, rain and snow: the phase of an hour's showers
     * when the response does not carry the amount. */
    private val PROVIDER_SHOWERS = setOf(
        WmoCode.SHOWERS_SLIGHT, WmoCode.SHOWERS_MODERATE, WmoCode.SHOWERS_HEAVY,
        WmoCode.SNOW_SHOWERS_SLIGHT, WmoCode.SNOW_SHOWERS_HEAVY
    )

    /** Open-Meteo's own snowfall ladder, cm/h: 71 under 0.2, 73 under 0.8, 75 from 0.8. */
    private const val SNOW_MODERATE_CM = 0.2
    private const val SNOW_HEAVY_CM = 0.8
}
