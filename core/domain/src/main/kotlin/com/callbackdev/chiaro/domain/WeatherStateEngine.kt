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
 * 1. **Thunderstorm**: the provider's 95/96/99 when probable. The app has no CAPE, so it
 *    neither invents a storm nor removes one; over an hour with no measurable rain it is
 *    [WmoCode.THUNDERSTORM_POSSIBLE], still a hazard.
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
 * 5. **Fog** at ≤ [FOG_VISIBILITY_M] on the visibility the response has ([visibilityOf]):
 *    the hour's own where it carries one, otherwise the lowest the witnesses forecast
 *    (27 ott 2026, Fase 14: DWD removes the visibility from ICON on 30 Nov 2026). Kept where
 *    the provider says fog and that visibility agrees, invented only where it persists into
 *    a neighbouring hour of the same series (Fase 26), and in both cases confirmed by the
 *    hour's own humidity and by the witness models: one that sees it when the visibility
 *    is the hour's own, two when it is lent (26 ott 2026, 14.2). **The provider's code alone
 *    never makes fog**: measured against the METARs of 24 airports it says fog 4 361 times
 *    for 955 seen, 2 425 of them over air clear for five kilometres. Freezing fog at ≤ 0 °C.
 * 6. **Sky** from the share of the sun's light the cloud takes, layer by layer
 *    ([CloudLayers.lightTaken], 26 ott 2026), on Open-Meteo's own buckets (20/50/80): the
 *    same arithmetic the sky drawn at the top of the page lights its clouds with.
 *
 * Every input may be missing — an older cache entry, a model that does not carry it — and
 * a missing input switches off only the rule that needs it: that part of the decision
 * falls back on the provider's code, as the app did before this engine. Fog is the one
 * exception: with no visibility of any kind, own or lent, there is no fog, because the
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

    const val FOG_VISIBILITY_M = 1000.0

    /** A witness that sees the air this close to saturation sees fog, whatever its visibility
     * diagnostic says (26 ott 2026, measured against METARs: `docs/STATES.md` §11). */
    const val FOG_WITNESS_HUMIDITY_PCT = 97

    /** The hour's own humidity under which its fog does not stand, witnesses or not. */
    const val FOG_OWN_HUMIDITY_PCT = 95

    /**
     * How many witnesses must see the fog: one when the visibility is the hour's own, two
     * when it is lent by the witnesses themselves, the lender being one of them (27 ott
     * 2026, measured in `docs/STATES.md` §11: a CSI of 0.28 either way, and the second
     * witness takes 78 false fogs off the lent series, 73 of them over clear air).
     */
    const val FOG_WITNESSES_OWN = 1
    const val FOG_WITNESSES_LENT = 2

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
         * The hour's OWN visibility, m: null where the response has none, or where the
         * number it carries is a witness's copy (Open-Meteo fills a variable the model
         * lacks from the largest model that has it; the mapper recognises the copy per
         * variable and passes null here, so that the number counts once, as the witness's,
         * and a model never confirms itself). The visibility the rules read is
         * [visibilityOf]: this one, else the witnesses' lowest.
         */
        val visibilityM: Double? = null,
        /**
         * The permission to invent fog. Null — the default — lets the series decide: a
         * neighbouring hour must also be under [FOG_VISIBILITY_M] (Fase 26). An hour
         * judged on its own (the `current` block) passes the verdict of the series it
         * belongs to, [fogPersists] on the row it falls in: its visibility is the
         * model's, not an observation, and a fog the strip does not draw is not drawn
         * above it either (26 ott 2026, Lissone).
         */
        val fogPersists: Boolean? = null,
        /** The cover by height, at the same instant: the sky is the light they take. */
        val cloudLayers: CloudLayers? = null,
        /** Relative humidity at the instant the hour starts, %. */
        val humidityPct: Int? = null,
        /**
         * The same hour from the other model families (26 ott 2026), `best_match` itself
         * left out: they vote on fog ([fogConfirmed]) and are never printed. Empty — no
         * response, an older cache entry — and the engine reads this hour alone.
         */
        val witnesses: List<Witness> = emptyList()
    )

    /**
     * Another model family's reading of the same instant: it votes on fog, it lends its
     * visibility where the hour has none of its own, and it is never printed. A null
     * [visibilityM] is a model without one: it then votes with its humidity alone.
     */
    data class Witness(
        val visibilityM: Double? = null,
        val humidityPct: Int? = null
    )

    /**
     * The visibility the rules read for [hour] (27 ott 2026): its own where the response
     * carries one of its own, otherwise the lowest the witnesses forecast for the same
     * instant — the series rule 5 persists on, and the one the `current` block must read
     * too. Null when nobody has one, and then there is no fog.
     */
    fun visibilityOf(hour: Hour): Double? =
        hour.visibilityM ?: hour.witnesses.mapNotNull { it.visibilityM }.minOrNull()

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

        // 1. Thunderstorm.
        if (provider?.hazard == AlertEngine.SevereBucket.THUNDER && probable) {
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
        return fogOrSky(hour, hour.fogPersists ?: persists(i) { hours.getOrNull(it)?.let(::visibilityOf) })
    }

    /**
     * Below the fog threshold here and in a neighbouring hour: the series' test for
     * INVENTING fog (Fase 26). Writing fog into an hour whose neighbours are both
     * clear-aired nearly doubled the fog↔non-fog transitions of the week (23 cities, 6 Sep
     * 2026: 10 in the provider's series, 18 repaired, 14 with this rule). Removing a fog
     * code the visibility contradicts needs no such patience.
     *
     * Public, on the bare visibilities — the series' as [visibilityOf] reads it, own or
     * lent — for a caller that judges one moment of the series on its own (the `current`
     * block), so that the moment and its row agree on what is fog.
     */
    fun fogPersists(visibilityM: List<Double?>, i: Int): Boolean = persists(i) { visibilityM.getOrNull(it) }

    private fun persists(i: Int, visibilityAt: (Int) -> Double?): Boolean {
        fun low(at: Int) = visibilityAt(at)?.let { it <= FOG_VISIBILITY_M } == true
        return low(i) && (low(i - 1) || low(i + 1))
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
     * Rules 5 and 6: the fog repair of Fase 13c and 26 on the visibility the response has,
     * then the sky of the cloud cover. With no visibility of any kind there is no fog: the
     * provider's fog code never decides alone (27 ott 2026; until then a null visibility
     * left its verdict standing, and that verdict contradicts the same response's visibility
     * 68% of the times it says fog). A null cloud cover leaves the provider's sky.
     */
    private fun fogOrSky(hour: Hour, persists: Boolean): Int {
        val instant = WmoCode.of(hour.providerSkyCode)
        val visibility = visibilityOf(hour)
        val fog = visibility != null && visibility <= FOG_VISIBILITY_M &&
            (instant?.isFog == true || persists) &&
            fogConfirmed(hour, lent = hour.visibilityM == null)
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
     * The two confirmations fog needs (26 ott 2026). One model's visibility is the weakest number
     * it forecasts, and one model alone saw the fog over Lissone that morning, so:
     *
     * - **its own air near saturation**, [FOG_OWN_HUMIDITY_PCT] or more where the hour has a
     *   humidity: Lissone's block said 140 m at 94%;
     * - **other models that see it too**, where there are witnesses: under [FOG_VISIBILITY_M],
     *   or at [FOG_WITNESS_HUMIDITY_PCT] where their visibility diagnostic does not say it.
     *   [FOG_WITNESSES_OWN] of them when the visibility is the hour's own; [FOG_WITNESSES_LENT]
     *   when it is [lent] by the witnesses themselves (27 ott 2026), the lender counting as
     *   one — a lent visibility under a kilometre is already one witness's word, and one word
     *   is what Lissone had.
     *
     * Measured against the METARs of 24 airports over October 2026 and December–January
     * (`docs/STATES.md` §11): the false fogs fall by 41%, the fogs invented over air clear for
     * five kilometres by half, and 85% of the real fogs the engine caught are still caught;
     * without the hour's own visibility the lent series with two witnesses holds the same
     * score. Asking most of the witnesses rather than one caught 607 of a winter's 1 257 fog
     * hours against 671, in the season fog is a hazard on the road. With no humidity and no
     * witness, nothing contradicts the hour's own visibility.
     */
    private fun fogConfirmed(hour: Hour, lent: Boolean): Boolean {
        val rh = hour.humidityPct
        if (rh != null && rh < FOG_OWN_HUMIDITY_PCT) return false
        val voters = hour.witnesses.filter { it.visibilityM != null || it.humidityPct != null }
        if (voters.isEmpty()) return !lent
        val seeing = voters.count { w ->
            (w.visibilityM?.let { it <= FOG_VISIBILITY_M } == true) ||
                (w.humidityPct?.let { it >= FOG_WITNESS_HUMIDITY_PCT } == true)
        }
        return seeing >= if (lent) FOG_WITNESSES_LENT else FOG_WITNESSES_OWN
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
