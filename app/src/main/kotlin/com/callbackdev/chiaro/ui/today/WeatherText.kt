package com.callbackdev.chiaro.ui.today

import androidx.annotation.StringRes
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.ConditionWord
import com.callbackdev.chiaro.domain.model.AirQuality
import com.callbackdev.chiaro.domain.model.AqiScale
import com.callbackdev.chiaro.domain.model.CloudLayers
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
        ConditionWord.FREEZING_FOG -> R.string.cond_freezing_fog
        ConditionWord.RAIN_AND_SNOW -> R.string.cond_rain_and_snow
        ConditionWord.RAIN_LIKELY -> R.string.cond_rain_likely
        ConditionWord.SNOW_LIKELY -> R.string.cond_snow_likely
        ConditionWord.THUNDERSTORM_POSSIBLE -> R.string.cond_thunderstorm_possible
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

    /**
     * The European index (EEA, bands revised 2024: 0-20 good, 20-40 fair, 40-60 moderate,
     * 60-80 poor, 80-100 very poor, above extremely poor), said with the same six lines
     * the US scale uses, matched on the EEA's own advice for the general population:
     * nothing to change through «moderate», cut the intense effort when «poor», less time
     * outside when «very poor», indoors past 100 (24 set 2026).
     */
    @StringRes
    fun euAqiMeaning(aqi: Int): Int = when {
        aqi <= 20 -> R.string.aqi_meaning_good
        aqi <= 60 -> R.string.aqi_meaning_moderate
        aqi <= 80 -> R.string.aqi_meaning_sensitive
        aqi <= 100 -> R.string.aqi_meaning_very_unhealthy
        else -> R.string.aqi_meaning_hazardous
    }

    /** The air's line on the scale the place reads. */
    @StringRes
    fun airMeaning(air: AirQuality): Int =
        if (air.scale == AqiScale.EUROPEAN) euAqiMeaning(air.shownIndex) else aqiMeaning(air.shownIndex)

    /**
     * A day's rain in bands (24 set 2026): under 0.2 mm nothing falls that anyone notices,
     * under 2 a few drops, under 10 an umbrella, under 30 a wet day, above that abundant —
     * one millimetre is the Met Office's «wet day», ten the WMO's «heavy precipitation
     * day», thirty where Italian civil protection bulletins start talking about ground
     * effects. The index is the band, for the track's steps.
     */
    fun rainBand(mm: Double): Int = rainBandOf(asPrinted(mm))

    private fun rainBandOf(mm: Double): Int = when {
        mm < 0.2 -> 0
        mm < 2.0 -> 1
        mm < 10.0 -> 2
        mm < 30.0 -> 3
        else -> 4
    }

    const val RAIN_BANDS = 5

    /**
     * The amount as `Formats.millimetres`/`centimetres` print it — a decimal below ten,
     * whole above — so the band is decided on the number the reader sees: 19.81 cm prints
     * «20 cm», and a line for «under 20» beside it would be the tile arguing with itself.
     */
    private fun asPrinted(amount: Double): Double =
        if (amount < 10) Math.round(amount * 10) / 10.0 else Math.round(amount).toDouble()

    @StringRes
    fun rainMeaning(mm: Double): Int = when (rainBand(mm)) {
        0 -> R.string.rain_meaning_none
        1 -> R.string.rain_meaning_drops
        2 -> R.string.rain_meaning_umbrella
        3 -> R.string.rain_meaning_wet
        else -> R.string.rain_meaning_heavy
    }

    /** Snow on the ground by the day's end, in bands of what it does to a journey. */
    fun snowBand(cm: Double): Int = snowBandOf(asPrinted(cm))

    private fun snowBandOf(cm: Double): Int = when {
        cm < 1.0 -> 0
        cm < 5.0 -> 1
        cm < 20.0 -> 2
        else -> 3
    }

    const val SNOW_BANDS = 4

    @StringRes
    fun snowMeaning(cm: Double): Int = when (snowBand(cm)) {
        0 -> R.string.snow_meaning_dusting
        1 -> R.string.snow_meaning_slippery
        2 -> R.string.snow_meaning_shovel
        else -> R.string.snow_meaning_heavy
    }

    /**
     * What the cloud does to the sky, by day to the sun and by night to the stars
     * (24 set 2026). The layer is what the total cannot say: 90% of high veil still lets
     * the sun through, 90% of low cloud is a grey day.
     */
    @StringRes
    fun cloudMeaning(totalPct: Int, layer: CloudLayers.Layer?, night: Boolean): Int = when {
        totalPct < CloudLayers.MIN_PCT -> if (night) R.string.cloud_meaning_open_night else R.string.cloud_meaning_open
        layer == CloudLayers.Layer.HIGH -> if (night) R.string.cloud_meaning_veiled_night else R.string.cloud_meaning_veiled
        totalPct >= 80 -> if (night) R.string.cloud_meaning_closed_night else R.string.cloud_meaning_closed
        else -> if (night) R.string.cloud_meaning_broken_night else R.string.cloud_meaning_broken
    }

    /** The note under the cloud's value: which layer makes it, when one does. */
    @StringRes
    fun cloudLayerNote(layer: CloudLayers.Layer): Int = when (layer) {
        CloudLayers.Layer.LOW -> R.string.cloud_note_low
        CloudLayers.Layer.MID -> R.string.cloud_note_mid
        CloudLayers.Layer.HIGH -> R.string.cloud_note_high
    }

    @StringRes
    fun pollenLevel(level: PollenLevel): Int = when (level) {
        PollenLevel.NONE -> R.string.pollen_level_none
        PollenLevel.LOW -> R.string.pollen_level_low
        PollenLevel.MODERATE -> R.string.pollen_level_moderate
        PollenLevel.HIGH -> R.string.pollen_level_high
        PollenLevel.VERY_HIGH -> R.string.pollen_level_very_high
    }

    @StringRes
    fun pollenMeaning(worst: PollenLevel): Int = when (worst) {
        PollenLevel.NONE -> R.string.pollen_meaning_none
        PollenLevel.LOW -> R.string.pollen_meaning_low
        PollenLevel.MODERATE -> R.string.pollen_meaning_moderate
        PollenLevel.HIGH -> R.string.pollen_meaning_high
        PollenLevel.VERY_HIGH -> R.string.pollen_meaning_very_high
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
