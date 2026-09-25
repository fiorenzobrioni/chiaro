package com.callbackdev.chiaro.domain

/**
 * What an hour IS — its icon and its word — decided from the physical fields instead of
 * relayed from `weather_code` (25 set 2026, `PLANNING.md`, «Il motore degli stati»).
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
 * 5. **Fog** at ≤ [FOG_VISIBILITY_M]: kept where the provider says fog and the visibility
 *    agrees, invented only where it persists into a neighbouring hour (Fase 26); freezing
 *    fog at ≤ 0 °C.
 * 6. **Sky** from the cloud cover, on Open-Meteo's own buckets (20/50/80): the same
 *    quantity the sky drawn at the top of the page and the cloud tile already show.
 *
 * Every input may be missing — an older cache entry, a model that does not carry it — and
 * a missing input switches off only the rule that needs it: that part of the decision
 * falls back on the provider's code, as the app did before this engine.
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
        val visibilityM: Double? = null,
        /** Whether a neighbouring hour is also under [FOG_VISIBILITY_M]: the series'
         * permission to invent fog. An observation has no series and passes true. */
        val fogPersists: Boolean = false
    )

    /**
     * The state of every hour of a series, in order. [likely] switches rule 4 off for a
     * caller that is describing what is happening rather than what may.
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
        return fogOrSky(hour)
    }

    /** Rule 3 on an hour with a measurable, probable amount; null when the phase cannot be
     * told and the provider says nothing falls — then the hour is its sky. */
    private fun falling(hour: Hour, mm: Double): Int? {
        val snowWater = hour.snowfallCm?.let { it / SNOW_CM_PER_MM }
            ?: phaseFromProvider(hour, mm)
            ?: return null
        val liquid = (mm - snowWater).coerceAtLeast(0.0)
        if (measurable(liquid) && measurable(snowWater)) {
            return if (mm < RAIN_LIGHT_MAX_MM) WmoCode.RAIN_AND_SNOW_LIGHT.code else WmoCode.RAIN_AND_SNOW.code
        }
        val showers = hour.showersMm
        if (snowWater > liquid) {
            val cm = snowWater * SNOW_CM_PER_MM
            if (showers != null && showers >= snowWater / 2) {
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
        if (showers != null && minOf(showers, liquid) >= liquid / 2) {
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

    /** The phase a likely hour takes: the nearest hour within reach that has a
     * measurable amount of its own, else the temperature. */
    private fun likelySnow(hours: List<Hour>, i: Int): Boolean {
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
     * Rules 5 and 6: the fog repair of Fase 13c and 26, then the sky of the cloud cover.
     * A null visibility leaves the provider's own fog verdict standing; a null cloud cover
     * leaves the provider's sky.
     */
    private fun fogOrSky(hour: Hour): Int {
        val instant = WmoCode.of(hour.providerSkyCode)
        val visibility = hour.visibilityM
        val fog = if (visibility == null) {
            instant?.isFog == true
        } else {
            visibility <= FOG_VISIBILITY_M && (instant?.isFog == true || hour.fogPersists)
        }
        if (fog) {
            val t = hour.temperatureC
            return if (t != null && t <= 0.0) WmoCode.RIME_FOG.code else WmoCode.FOG.code
        }
        val cloud = hour.cloudCoverPct
        return when {
            cloud != null -> skyOf(cloud)
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

    /** Open-Meteo's own snowfall ladder, cm/h: 71 under 0.2, 73 under 0.8, 75 from 0.8. */
    private const val SNOW_MODERATE_CM = 0.2
    private const val SNOW_HEAVY_CM = 0.8
}
