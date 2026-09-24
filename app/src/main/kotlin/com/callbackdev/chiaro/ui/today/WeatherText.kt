package com.callbackdev.chiaro.ui.today

import androidx.annotation.StringRes
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.ConditionWord
import com.callbackdev.chiaro.domain.model.PollenLevel
import com.callbackdev.chiaro.domain.model.PollenReport

/**
 * The WMO vocabulary and the meaning lines, as string resources — which is the whole
 * localization architecture of this app: everything on screen is prose or data, so
 * everything on screen is a resource (VISION §8). The domain carries no words of its
 * own for a condition since 24 set 2026 — tweather's English descriptions went — only
 * the code and the [ConditionWord] group it belongs to.
 *
 * The meaning bands are the product's editorial voice (DESIGN §1.2): every band is an
 * honest consequence, and a metric whose value has no consequence today still gets its
 * band's line — "nothing to do about it" is also an answer.
 */
object WeatherText {

    /** One word (or two) per WMO bucket. Day/night does not change the word. */
    @StringRes
    fun condition(wmoCode: Int): Int = condition(ConditionWord.of(wmoCode))

    /**
     * The word for a [ConditionWord], which is where the grouping of codes lives
     * (24 set 2026): this table only spells each group, so it can no longer disagree
     * with the engines about which codes belong together.
     *
     * Three words changed that day, each to what the code can promise under both of
     * the ways Open-Meteo writes it (see `WmoCode`): 96/99 are «Temporale forte», not
     * «con grandine» — outside the ICON family the code means a strong thunderstorm
     * and nobody forecast hail; 82 is «Rovesci forti», not «violenti» — it starts at
     * 7.6 mm/h, the same floor as «Pioggia forte»; and code 2 is «Nuvoloso», not «Poco
     * nuvoloso» — it is 50 to 80% of the sky, where the Italian bulletins' «poco
     * nuvoloso» is the few clouds of code 1.
     */
    @StringRes
    fun condition(word: ConditionWord): Int = when (word) {
        ConditionWord.CLEAR -> R.string.cond_clear
        ConditionWord.MOSTLY_CLEAR -> R.string.cond_mostly_clear
        ConditionWord.PARTLY_CLOUDY -> R.string.cond_partly_cloudy
        ConditionWord.OVERCAST -> R.string.cond_overcast
        ConditionWord.FOG -> R.string.cond_fog
        ConditionWord.DRIZZLE -> R.string.cond_drizzle
        ConditionWord.FREEZING_DRIZZLE -> R.string.cond_freezing_drizzle
        ConditionWord.RAIN_LIGHT -> R.string.cond_rain_light
        ConditionWord.RAIN -> R.string.cond_rain
        ConditionWord.RAIN_HEAVY -> R.string.cond_rain_heavy
        ConditionWord.FREEZING_RAIN -> R.string.cond_freezing_rain
        ConditionWord.SNOW_LIGHT -> R.string.cond_snow_light
        ConditionWord.SNOW -> R.string.cond_snow
        ConditionWord.SNOW_HEAVY -> R.string.cond_snow_heavy
        ConditionWord.SNOW_GRAINS -> R.string.cond_snow_grains
        ConditionWord.SHOWERS -> R.string.cond_showers
        ConditionWord.SHOWERS_HEAVY -> R.string.cond_showers_heavy
        ConditionWord.SNOW_SHOWERS -> R.string.cond_snow_showers
        ConditionWord.THUNDERSTORM -> R.string.cond_thunderstorm
        ConditionWord.THUNDERSTORM_STRONG -> R.string.cond_thunderstorm_strong
        ConditionWord.UNKNOWN -> R.string.cond_unknown
    }

    /** Burn-time bands for unprotected fair skin — estimates, and worded as such. */
    @StringRes
    fun uvMeaning(uvIndex: Int): Int = when {
        uvIndex <= 2 -> R.string.uv_meaning_low
        uvIndex <= 5 -> R.string.uv_meaning_moderate
        uvIndex <= 7 -> R.string.uv_meaning_high
        uvIndex <= 10 -> R.string.uv_meaning_very_high
        else -> R.string.uv_meaning_extreme
    }

    /**
     * Said for the wind a body feels, which on a gusty day is the gust: the tile passes
     * the gust here when [gustsMaterial] says it counts, because "noticeable, not a
     * bother" over a line reading "gusts up to 45 km/h" is the tile arguing with itself.
     */
    @StringRes
    fun windMeaning(speedKph: Double): Int = when {
        speedKph < 5 -> R.string.wind_meaning_calm
        speedKph < 20 -> R.string.wind_meaning_light
        speedKph < 39 -> R.string.wind_meaning_moderate
        speedKph < 62 -> R.string.wind_meaning_strong
        else -> R.string.wind_meaning_gale
    }

    /**
     * Whether the gusts earn a line of their own (card review, 8 set 2026): at least
     * 25 km/h, and at least half again the steady wind. Below that the two numbers say
     * the same thing and the second is noise; above it the gust is the number that
     * takes the hat, and hiding it would be the tile understating the day.
     */
    fun gustsMaterial(speedKph: Double, gustKph: Double): Boolean =
        gustKph >= 25.0 && gustKph >= speedKph * 1.5

    /**
     * The comfort scale that humidity alone cannot honestly claim — and, since the card
     * review of 8 set 2026, the meaning line of the ONE humidity tile: the dew point is
     * the better predictor of how the air feels, and two tiles saying "comfortable" and
     * "pleasant" one under the other were the same fact told twice. The dew point itself
     * stays on the tile as a printed note.
     */
    @StringRes
    fun dewPointMeaning(celsius: Double): Int = when {
        celsius < 10 -> R.string.dew_meaning_dry
        celsius < 16 -> R.string.dew_meaning_pleasant
        celsius < 21 -> R.string.dew_meaning_sticky
        celsius < 24 -> R.string.dew_meaning_muggy
        else -> R.string.dew_meaning_oppressive
    }

    /**
     * What a night's lowest hour asks of the reader, for the evening summary — the one
     * place the app talks about a temperature nobody is standing in yet.
     *
     * The bands are the decisions, not the thermometer: at or under zero there is ice
     * to scrape and plants to cover (the same 0 °C the drift strip and the headline
     * call frost — a third definition of freezing is a third answer); under 5 the cold
     * reaches what is left outside; under 13 it reaches a body that goes out; under 21
     * it is the night a window can stay open on; above it, the night that does not
     * cool down, which is the only reason the number matters at all.
     */
    @StringRes
    fun nightMeaning(lowC: Double): Int = when {
        lowC <= 0 -> R.string.night_meaning_freezing
        lowC < 5 -> R.string.night_meaning_cold
        lowC < 13 -> R.string.night_meaning_cool
        lowC < 21 -> R.string.night_meaning_mild
        else -> R.string.night_meaning_warm
    }

    /** Tendencies, not forecasts: absolute pressure only says which way to lean. */
    @StringRes
    fun pressureMeaning(mb: Double): Int = when {
        mb < 1000 -> R.string.pressure_meaning_low
        mb <= 1020 -> R.string.pressure_meaning_normal
        else -> R.string.pressure_meaning_high
    }

    @StringRes
    fun visibilityMeaning(km: Double): Int = when {
        km >= 10 -> R.string.visibility_meaning_clear
        km >= 4 -> R.string.visibility_meaning_light_haze
        km >= 1 -> R.string.visibility_meaning_haze
        else -> R.string.visibility_meaning_fog
    }

    /** US AQI bands, said as what a body can do in them (VISION §3.3.3). */
    @StringRes
    fun aqiMeaning(aqi: Int): Int = when {
        aqi <= 50 -> R.string.aqi_meaning_good
        aqi <= 100 -> R.string.aqi_meaning_moderate
        aqi <= 150 -> R.string.aqi_meaning_sensitive
        aqi <= 200 -> R.string.aqi_meaning_unhealthy
        aqi <= 300 -> R.string.aqi_meaning_very_unhealthy
        else -> R.string.aqi_meaning_hazardous
    }

    @StringRes
    fun pollenLevel(level: PollenLevel): Int = when (level) {
        PollenLevel.NONE -> R.string.pollen_level_none
        PollenLevel.LOW -> R.string.pollen_level_low
        PollenLevel.MODERATE -> R.string.pollen_level_moderate
        PollenLevel.HIGH -> R.string.pollen_level_high
    }

    @StringRes
    fun pollenMeaning(worst: PollenLevel): Int = when (worst) {
        PollenLevel.NONE -> R.string.pollen_meaning_none
        PollenLevel.LOW -> R.string.pollen_meaning_low
        PollenLevel.MODERATE -> R.string.pollen_meaning_moderate
        PollenLevel.HIGH -> R.string.pollen_meaning_high
    }

    /** The level the pollen tile prints: the worst of the three families. */
    fun pollenWorst(pollen: PollenReport): PollenLevel =
        listOf(pollen.grass, pollen.tree, pollen.weed).maxBy { it.ordinal }

    /**
     * WHICH pollen is at that level, in the catalog's order (card review, 8 set 2026):
     * "high" alone tells an allergic reader nothing they can act on, "grass" does.
     * Empty when nothing is in the air, so the tile prints no line rather than naming
     * three absent families.
     */
    @StringRes
    fun pollenFamiliesAtWorst(pollen: PollenReport): List<Int> {
        val worst = pollenWorst(pollen)
        if (worst == PollenLevel.NONE) return emptyList()
        return buildList {
            if (pollen.grass == worst) add(R.string.pollen_family_grass)
            if (pollen.tree == worst) add(R.string.pollen_family_tree)
            if (pollen.weed == worst) add(R.string.pollen_family_weed)
        }
    }
}
