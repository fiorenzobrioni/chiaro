package com.callbackdev.chiaro.domain

import com.callbackdev.chiaro.domain.model.CloudLayers

/**
 * What a day IS, the one code its row wears (`docs/STATES.md` §6): the rules the mapper
 * applies to the provider's day, and since 23 nov 2026 the ones Today applies to the rest of
 * today ([RestOfDay]). One implementation, so the two rows of one week cannot be read by two
 * arithmetics.
 *
 * Three steps, in this order:
 *
 * 1. **A hazard claims the day** ([AlertEngine.severeBucket], the banner's own test): freezing
 *    anything, the heavy grades, every thunderstorm, whatever else falls. An hour whose hazard
 *    code fails the chance test is dropped entirely, millimetres included: the app has
 *    decided not to believe it.
 * 2. **Rain that is really there labels the day**: the other wet hours must bring [WET_DAY_MM]
 *    or run for [WET_DAY_HOURS]. The phase that brought more water (hours, without amounts)
 *    names the day; the heaviest intensity inside it picks the word.
 * 3. **Otherwise the sky, and the daylight's**: the most frequent sky of the daylight hours,
 *    ties to the heavier ([WmoCode.severity]), then to the higher number. A wet hour that did
 *    not earn the day votes with the sky its own cloud makes.
 *
 * The reasons and the measurements behind each step are in STATES §6 and in the mapper's
 * `dailyCode`, where these rules were born (Fase 26, 24 set and 17 nov 2026).
 */
object DayLabel {

    /** Precipitation that labels a day: this much over it... */
    const val WET_DAY_MM = 1.0

    /** ...or this many of its hours. */
    const val WET_DAY_HOURS = 3

    /**
     * One hour of the day as the label reads it: its [code] (the app's state, null where the
     * provider gave it nothing to read a state from, and it does not vote), what falls in it,
     * its chance, whether it is daylight, and the cloud its sky is read from when its code is
     * not a sky.
     */
    data class Hour(
        val code: Int?,
        val precipitationMm: Double?,
        val chancePct: Int?,
        val daylight: Boolean,
        val cloudCoverPct: Int?,
        val layers: CloudLayers?
    )

    /** The day's code from its [hours], or [fallback] when none has anything to read. */
    fun of(hours: List<Hour>, fallback: Int?): Int? {
        val stated = hours.filter { it.code != null }
        if (stated.isEmpty()) return fallback

        val hazards = stated.filter { AlertEngine.severeBucket(it.code!!, it.chancePct) != null }
        if (hazards.isNotEmpty()) return heaviest(hazards.map { it.code!! })

        // Hazard codes that failed the chance test are not wet hours of a lesser kind.
        val wet = stated.filter { WmoCode.of(it.code!!)?.let { w -> w.isPrecipitation && w.hazard == null } == true }
        fun mm(of: List<Hour>) = of.sumOf { it.precipitationMm ?: 0.0 }
        val wetMm = mm(wet)
        if (wet.size >= WET_DAY_HOURS || wetMm >= WET_DAY_MM) {
            // An hour with no amount (a cache entry from before they were asked for) cannot
            // weigh by millimetres: the phases then weigh by their hours.
            val weight: (List<Hour>) -> Double = if (wetMm > 0.0) ::mm else { phaseHours -> phaseHours.size.toDouble() }
            val dominant = wet.groupBy { WmoCode.of(it.code!!)!!.phase }.values
                .maxWithOrNull(compareBy(weight).thenBy { phaseHours -> phaseHours.maxOf { severity(it.code!!) } })!!
            return heaviest(dominant.map { it.code!! })
        }

        val daylight = stated.filter { it.daylight }.ifEmpty { stated }
        return daylight.mapNotNull(::skyOf)
            .groupingBy { it }.eachCount()
            .maxWithOrNull(compareBy({ it.value }, { WmoCode.of(it.key)?.severity ?: -1 }, { it.key }))?.key
            ?: fallback
    }

    /** The heaviest of [codes] by [WmoCode.severity]; among equally heavy codes, the one more
     * hours carry, then the higher number, so the answer never depends on order. */
    private fun heaviest(codes: List<Int>): Int {
        val top = codes.maxOf(::severity)
        return codes.filter { severity(it) == top }
            .groupingBy { it }.eachCount()
            .maxWithOrNull(compareBy({ it.value }, { it.key }))!!.key
    }

    /** Only ever called on codes the table knows: hazards and wet hours are filtered through it. */
    private fun severity(code: Int): Int = WmoCode.of(code)!!.severity

    /** The sky an hour shows: its own code when that is a sky or a fog, else the one its cloud
     * makes by the light its layers take (rule 6); none without a cloud cover. */
    private fun skyOf(hour: Hour): Int? {
        val code = hour.code!!
        return if (WmoCode.of(code)?.phase == WmoCode.Phase.NONE) code
        else hour.cloudCoverPct?.let { WeatherStateEngine.skyOf(it, hour.layers) }
    }
}
