package com.callbackdev.chiaro.domain

/**
 * The WMO weather interpretation codes Open-Meteo serves, and everything the app
 * decides from one — the single table (24 set 2026, the engine review's suggestion 1).
 *
 * Until this file the vocabulary lived in five places that had to agree by hand: the
 * mapper's `HazardCodes` and `FogCodes`, `AlertEngine.SevereCodes`, the Today headline's
 * `WET_CODES`/`SNOW_CODES`/`FOG_CODES`, and the two `when` tables that pick a word and an
 * icon. They did agree, which is exactly why the one thing none of them said went
 * unnoticed: **the WMO number is not an order of severity**. The day's label took the
 * `max()` of its wet hours, and 80 (a slight shower) outranks 75 (heavy snow), 85 outranks
 * 82, 71 outranks 65 — a day of heavy snow with one shower at five o'clock printed
 * «Rovesci». Severity is now a column of its own, next to the phase that keeps snow and
 * rain from being compared as if they were the same thing.
 *
 * **What a code means depends on the model that wrote it**, and the response does not say
 * which model that was. For the DWD ICON family Open-Meteo relays the model's own `ww`;
 * for every other model it derives the code itself (`WeatherCode.calculate` upstream,
 * read 24 set 2026): drizzle is then any precipitation under 1.3 mm/h, 82 is ≥ 7.6 mm/h
 * — the same floor as 65 —, 96 is a thunderstorm whose score passes 85, and 99 and
 * hail are never produced at all. The words the app prints for a code (see
 * [ConditionWord]) are chosen to be true under both readings.
 *
 * **Since 25 set 2026 the table is also the vocabulary of [WeatherStateEngine]**, which
 * derives an hour's state from the physical fields instead of relaying the code. That
 * added two WMO codes Open-Meteo never serves but the engine can honestly write — 68 and
 * 69, rain and snow together (WMO 4677) — and three states that are the app's own and
 * carry numbers above 1000 so they can never be mistaken for a code a provider sent:
 * precipitation that is likely without any amount in the hour (1061, 1071), and a
 * thunderstorm with no measurable rain in its hour (1095).
 */
enum class WmoCode(
    val code: Int,
    val word: ConditionWord,
    val phase: Phase,
    /**
     * How much the code weighs when one of them has to speak for a whole day. Equal
     * values are the same intensity in two shapes — steady rain and a shower of the
     * same millimetres, snow and a snow shower — and the day's own hours break the tie.
     * Hazards first, then snow over rain of the same grade (it closes more roads), then
     * the intensity ladder, then fog, then the sky.
     */
    val severity: Int,
    /** The class of «Maltempo» this code belongs to, or null when it is not one. */
    val hazard: AlertEngine.SevereBucket?,
    /**
     * A state of possibility, not of fact: the ensemble makes precipitation likely in an
     * hour where the model has none to measure. It has a phase — the headline still says
     * snow or rain from it — but it is not [isPrecipitation]: nothing is falling that the
     * app could say is falling, so it never makes «it is raining now», never counts as a
     * wet hour for the day's label, and never carries an amount.
     */
    val likely: Boolean = false
) {
    CLEAR(0, ConditionWord.CLEAR, Phase.NONE, 0, null),
    MAINLY_CLEAR(1, ConditionWord.MOSTLY_CLEAR, Phase.NONE, 1, null),
    PARTLY_CLOUDY(2, ConditionWord.PARTLY_CLOUDY, Phase.NONE, 2, null),
    OVERCAST(3, ConditionWord.OVERCAST, Phase.NONE, 3, null),
    FOG(45, ConditionWord.FOG, Phase.NONE, 10, null),
    /**
     * Listed in Open-Meteo's enum and never derived by it; only a relayed `ww` carries it.
     * [WeatherStateEngine] writes it for fog at or below 0 °C (25 set 2026): the droplets
     * are supercooled and freeze on what they touch, which is what «nebbia gelata» means.
     */
    RIME_FOG(48, ConditionWord.FREEZING_FOG, Phase.NONE, 11, null),

    DRIZZLE_LIGHT(51, ConditionWord.DRIZZLE, Phase.LIQUID, 20, null),
    DRIZZLE_MODERATE(53, ConditionWord.DRIZZLE, Phase.LIQUID, 21, null),
    DRIZZLE_DENSE(55, ConditionWord.DRIZZLE, Phase.LIQUID, 22, null),
    FREEZING_DRIZZLE_LIGHT(56, ConditionWord.FREEZING_DRIZZLE, Phase.FREEZING, 70, AlertEngine.SevereBucket.ICE),
    FREEZING_DRIZZLE_DENSE(57, ConditionWord.FREEZING_DRIZZLE, Phase.FREEZING, 72, AlertEngine.SevereBucket.ICE),

    RAIN_SLIGHT(61, ConditionWord.RAIN_LIGHT, Phase.LIQUID, 30, null),
    RAIN_MODERATE(63, ConditionWord.RAIN, Phase.LIQUID, 40, null),
    RAIN_HEAVY(65, ConditionWord.RAIN_HEAVY, Phase.LIQUID, 60, AlertEngine.SevereBucket.RAIN),
    FREEZING_RAIN_LIGHT(66, ConditionWord.FREEZING_RAIN, Phase.FREEZING, 80, AlertEngine.SevereBucket.ICE),
    FREEZING_RAIN_HEAVY(67, ConditionWord.FREEZING_RAIN, Phase.FREEZING, 82, AlertEngine.SevereBucket.ICE),

    /** Rain and snow in the same hour, both measurable (WMO 4677: 68 slight, 69 moderate
     * or heavy). Never served by Open-Meteo; written by [WeatherStateEngine]. */
    RAIN_AND_SNOW_LIGHT(68, ConditionWord.RAIN_AND_SNOW, Phase.MIXED, 36, null),
    RAIN_AND_SNOW(69, ConditionWord.RAIN_AND_SNOW, Phase.MIXED, 52, null),

    SNOW_SLIGHT(71, ConditionWord.SNOW_LIGHT, Phase.FROZEN, 35, null),
    SNOW_MODERATE(73, ConditionWord.SNOW, Phase.FROZEN, 50, null),
    SNOW_HEAVY(75, ConditionWord.SNOW_HEAVY, Phase.FROZEN, 65, AlertEngine.SevereBucket.SNOW),
    SNOW_GRAINS(77, ConditionWord.SNOW_GRAINS, Phase.FROZEN, 33, null),

    SHOWERS_SLIGHT(80, ConditionWord.SHOWERS, Phase.LIQUID, 30, null),
    SHOWERS_MODERATE(81, ConditionWord.SHOWERS, Phase.LIQUID, 40, null),
    SHOWERS_HEAVY(82, ConditionWord.SHOWERS_HEAVY, Phase.LIQUID, 60, AlertEngine.SevereBucket.RAIN),
    SNOW_SHOWERS_SLIGHT(85, ConditionWord.SNOW_SHOWERS, Phase.FROZEN, 35, null),
    SNOW_SHOWERS_HEAVY(86, ConditionWord.SNOW_SHOWERS, Phase.FROZEN, 65, AlertEngine.SevereBucket.SNOW),

    THUNDERSTORM(95, ConditionWord.THUNDERSTORM, Phase.LIQUID, 90, AlertEngine.SevereBucket.THUNDER),
    THUNDERSTORM_STRONG(96, ConditionWord.THUNDERSTORM_STRONG, Phase.LIQUID, 95, AlertEngine.SevereBucket.THUNDER),
    THUNDERSTORM_SEVERE(99, ConditionWord.THUNDERSTORM_STRONG, Phase.LIQUID, 96, AlertEngine.SevereBucket.THUNDER),

    // The app's own states (25 set 2026, [WeatherStateEngine]). Numbers above 1000: no
    // provider writes them, and a reader of a stored code can tell the two apart.

    /** Likely rain with no amount in the hour: the ensemble at ≥ 60%, the model dry. */
    RAIN_LIKELY(1061, ConditionWord.RAIN_LIKELY, Phase.LIQUID, 15, null, likely = true),

    /** The same for snow. */
    SNOW_LIKELY(1071, ConditionWord.SNOW_LIKELY, Phase.FROZEN, 16, null, likely = true),

    /**
     * A thunderstorm code over an hour with no measurable rain: Open-Meteo derives 95 from
     * CAPE before it looks at precipitation, so a storm can be forecast in a dry hour. Still
     * a «Maltempo» hazard — the engine may remove a distortion, never a warning — but drawn
     * without rain and worded as a possibility.
     */
    THUNDERSTORM_POSSIBLE(1095, ConditionWord.THUNDERSTORM_POSSIBLE, Phase.NONE, 89, AlertEngine.SevereBucket.THUNDER);

    /**
     * What is falling, if anything. Drizzle, rain, showers and thunderstorms are
     * [LIQUID]; the freezing codes are their own phase because they are ice on the
     * ground whatever the amount; snow, snow grains and snow showers are [FROZEN]; rain
     * and snow together are [MIXED].
     */
    enum class Phase { NONE, LIQUID, FREEZING, FROZEN, MIXED }

    val isPrecipitation: Boolean get() = phase != Phase.NONE && !likely
    val isFog: Boolean get() = word == ConditionWord.FOG || word == ConditionWord.FREEZING_FOG
    val isSnow: Boolean get() = phase == Phase.FROZEN

    companion object {
        private val byCode: Map<Int, WmoCode> = entries.associateBy { it.code }

        /** The entry for [code], or null for a number Open-Meteo does not serve. */
        fun of(code: Int): WmoCode? = byCode[code]

        /**
         * Whether [code] is precipitation of any kind. A number outside the table is
         * not: nobody forecast anything falling, and "unknown" is not "wet".
         */
        fun isPrecipitation(code: Int): Boolean = of(code)?.isPrecipitation == true
        fun isFog(code: Int): Boolean = of(code)?.isFog == true
        fun isSnow(code: Int): Boolean = of(code)?.isSnow == true
    }
}

/**
 * The words the reader sees for a condition, one per string resource: several codes
 * share a word (51/53/55 are all «Pioviggine»), and the Journal compares days by word
 * because a change the reader cannot see is not a change worth a line.
 *
 * [id] is written into history snapshots and must never change once shipped; it is the
 * key the Journal and the widgets read back. [UNKNOWN] is what a number outside the table
 * maps to, never a guess at the nearest code.
 */
enum class ConditionWord(val id: String) {
    CLEAR("clear"),
    MOSTLY_CLEAR("mostly_clear"),
    PARTLY_CLOUDY("partly_cloudy"),
    OVERCAST("overcast"),
    FOG("fog"),
    DRIZZLE("drizzle"),
    FREEZING_DRIZZLE("freezing_drizzle"),
    RAIN_LIGHT("rain_light"),
    RAIN("rain"),
    RAIN_HEAVY("rain_heavy"),
    FREEZING_RAIN("freezing_rain"),
    SNOW_LIGHT("snow_light"),
    SNOW("snow"),
    SNOW_HEAVY("snow_heavy"),
    SNOW_GRAINS("snow_grains"),
    SHOWERS("showers"),
    SHOWERS_HEAVY("showers_heavy"),
    SNOW_SHOWERS("snow_showers"),
    THUNDERSTORM("thunderstorm"),
    /** 96 and 99. Not «with hail»: see [WmoCode] — outside the ICON family no model says so. */
    THUNDERSTORM_STRONG("thunderstorm_strong"),
    UNKNOWN("unknown"),
    // 25 set 2026, the states of [WeatherStateEngine]. Appended, so every id above keeps
    // its place: they are on disk in the history.
    FREEZING_FOG("freezing_fog"),
    RAIN_AND_SNOW("rain_and_snow"),
    RAIN_LIKELY("rain_likely"),
    SNOW_LIKELY("snow_likely"),
    THUNDERSTORM_POSSIBLE("thunderstorm_possible");

    /** The heaviest code that prints this word; what the Journal ranks two days by. */
    val severity: Int get() = WmoCode.entries.filter { it.word == this }.maxOfOrNull { it.severity } ?: -1

    companion object {
        private val byId: Map<String, ConditionWord> = entries.associateBy { it.id }

        fun of(code: Int): ConditionWord = WmoCode.of(code)?.word ?: UNKNOWN
        fun fromId(id: String): ConditionWord? = byId[id]
    }
}
