package com.callbackdev.chiaro.ui.today

import com.callbackdev.chiaro.domain.ConditionWord
import com.callbackdev.chiaro.domain.WeatherStateEngine
import com.callbackdev.chiaro.domain.WmoCode
import com.callbackdev.chiaro.domain.model.HourlyForecast
import java.time.LocalDateTime

/**
 * The open day's sentence (committente, 31 ott 2026; PLANNING, «La frase del giorno aperto»):
 * the shape of a day of the week in words, what its ribbon draws — «Nuvoloso al mattino,
 * sereno dal pomeriggio, pioggia in serata». Language-free like [HeadlineEngine]: this decides
 * what the sentence says, [OpenDaySentenceText] spells it.
 *
 * The rules it was approved with, each kept here:
 *
 * - **No number.** The amounts, the gust and the UV are the facts under it.
 * - **The same states as the row's icon and ribbon**, so it cannot contradict them. The sky
 *   is read the way the day's label reads it (STATES §6, step 3) on each part of the day; a
 *   sky that holds all day takes the label's own word. What falls is the hours' own states,
 *   the ribbon's wet marks. A wet hour that did not earn the day (the label is a sky) is
 *   [Register.LIGHT], «qualche goccia», never «Pioggia» under an icon that says «Nuvoloso».
 *   A label that says something falls with no wet hour on the row to say it of (the label
 *   reads the provider's slots, the row its own hours: they part by an hour at the day's
 *   edge) draws no sentence rather than one that contradicts the icon.
 * - **Vaguer far out — measured: everywhere.** What falls was to be said by the hour
 *   («dalle 17:00 alle 20:00») as far out as an hour is right two times in three, and by the
 *   part of the day beyond. Against the METARs of 26 airports in two periods the hour is right
 *   65% of the time already tomorrow, under the bar, so every row says the part of the day
 *   («nel pomeriggio»), which tomorrow is right 78% of the time; three runs or more are «a
 *   tratti».
 * - **Not today's row and not a closed one**: the caller's to keep (TodayWeek).
 */
object OpenDaySentence {

    /** Three runs or more are a broken day: «a tratti». */
    private const val SCATTERED_RUNS = 3

    /** Past this many clauses, what falls is one clause in the day's heaviest word: a sentence
     * never turns into a list. */
    private const val MAX_CLAUSES = 2

    /** The parts of the day the sentence names, on the place's clock. */
    enum class Part(val untilHour: Int) {
        NIGHT(6), MORNING(12), AFTERNOON(18), EVENING(24);

        companion object {
            fun of(time: LocalDateTime): Part = entries.first { time.hour < it.untilHour }

            /** The sky is the day's: the night before the morning is not part of it. */
            val SKY = listOf(MORNING, AFTERNOON, EVENING)
        }
    }

    /** One stretch of sky: [code]'s word over consecutive [parts]. */
    data class Sky(val code: Int, val parts: List<Part>)

    enum class Register {
        /** What the hours' states call it: the day's label says something falls. */
        FULL,

        /** A wet hour under a label that is a sky: «qualche goccia», «qualche fiocco». */
        LIGHT,

        /** A possibility (1061, 1071, 1095): never wet. */
        POSSIBLE
    }

    /**
     * Something falling, or possible, over [parts] (in order, contiguous or not): [word] is the
     * heaviest of its hours' states, the one the strip's icons wear; [phase] picks the light
     * register's drop or flake.
     */
    data class Fall(
        val word: ConditionWord,
        val phase: WmoCode.Phase,
        val register: Register,
        val parts: List<Part>,
        val scattered: Boolean = false
    ) {
        /** A fall over the whole day, steady: the sky under it goes without saying. */
        val allDay: Boolean get() = !scattered && parts.size == Part.entries.size
    }

    /** [sky] is empty when a steady fall takes the whole day; [falls] when nothing falls. */
    data class Sentence(val sky: List<Sky>, val falls: List<Fall>)

    /**
     * The sentence of a day whose label is [dayCode] and whose hours are [day] (one local day,
     * in order, each with whether the sun is down). Null when it has nothing true to say: no
     * hours, no sky to read, or a label the hours cannot back.
     */
    fun of(dayCode: Int, day: List<StripHour>): Sentence? {
        if (day.isEmpty()) return null
        val hours = day.map { it.hour }
        val label = WmoCode.of(dayCode)
        val labelFalls = label != null && (label.isPrecipitation || label.hazard != null)

        val wetHours = hours.filter { WmoCode.isPrecipitation(it.condition.wmoCode) }
        val wet = inParts(wetHours, wet = labelFalls, scattered = runs(wetHours) >= SCATTERED_RUNS)
        val possible = hours.filter { WmoCode.of(it.condition.wmoCode)?.let { w -> w.likely || w == WmoCode.THUNDERSTORM_POSSIBLE } == true }
            // A possibility of rain beside rain that falls is the same rain said twice; a
            // possible thunderstorm is a hazard of its own and is always said.
            .filter { wet.isEmpty() || it.condition.wmoCode == WmoCode.THUNDERSTORM_POSSIBLE.code }
        val falls = wet + inParts(possible, wet = null, scattered = false)
        if (labelFalls && falls.isEmpty()) return null

        val sky = if (falls.any { it.register == Register.FULL && it.allDay }) emptyList() else sky(dayCode, day)
        if (sky.isEmpty() && falls.isEmpty()) return null
        return Sentence(sky, falls)
    }

    /** How many runs of consecutive hours [wet] makes, by the instant as the ribbon's marks
     * are spoken ([DaylightPhases.wetRuns]): the night a zone falls back holds 02:00 twice. */
    private fun runs(wet: List<HourlyForecast>): Int =
        if (wet.isEmpty()) 0 else 1 + wet.zipWithNext().count { (a, b) -> b.at.isAfter(a.at.plusSeconds(3_600)) }

    /**
     * [hours] said by the parts of the day they fall in: each part takes the word of its
     * hours, neighbouring parts of one word are one clause, and more than [MAX_CLAUSES] are
     * one, in the day's heaviest word. [wet] is whether the label says something falls, null
     * for the possibilities.
     */
    private fun inParts(hours: List<HourlyForecast>, wet: Boolean?, scattered: Boolean): List<Fall> {
        if (hours.isEmpty()) return emptyList()
        val byPart = hours.groupBy { Part.of(it.time) }.toSortedMap()
        val clauses = mutableListOf<Pair<List<HourlyForecast>, List<Part>>>()
        byPart.forEach { (part, inPart) ->
            val previous = clauses.lastOrNull()
            if (previous != null && previous.first.word() == inPart.word()) {
                clauses[clauses.lastIndex] = (previous.first + inPart) to (previous.second + part)
            } else {
                clauses += inPart to listOf(part)
            }
        }
        val grouped = if (clauses.size > MAX_CLAUSES) listOf(hours to byPart.keys.toList()) else clauses
        return grouped.map { (inClause, parts) -> inClause.fall(wet, parts, scattered) }
    }

    /** The clause a group of hours makes. A hazard keeps its own word in every register:
     * ice and storms are never «qualche goccia». */
    private fun List<HourlyForecast>.fall(wet: Boolean?, parts: List<Part>, scattered: Boolean): Fall {
        val word = word()
        val hazard = WmoCode.entries.any { it.word == word && it.hazard != null }
        val register = when {
            wet == null -> Register.POSSIBLE
            wet || hazard -> Register.FULL
            else -> Register.LIGHT
        }
        return Fall(word, phase(), register, parts, scattered)
    }

    /**
     * The word a group of hours goes by: a hazard first, by severity (ice and storms are
     * named whatever else falls, as the day's label names them); rain and snow in one
     * group are «pioggia e neve»; otherwise the heaviest state's word.
     */
    private fun List<HourlyForecast>.word(): ConditionWord {
        val codes = mapNotNull { WmoCode.of(it.condition.wmoCode) }
        codes.filter { it.hazard != null }.maxByOrNull { it.severity }?.let { return it.word }
        val phases = codes.filter { it.isPrecipitation }.map { it.phase }.toSet()
        if (WmoCode.Phase.MIXED in phases || (WmoCode.Phase.FROZEN in phases && WmoCode.Phase.LIQUID in phases)) {
            return ConditionWord.RAIN_AND_SNOW
        }
        return codes.maxBy { it.severity }.word
    }

    /** Frozen when only snow falls, for the light register's flake; otherwise what falls. */
    private fun List<HourlyForecast>.phase(): WmoCode.Phase {
        val phases = mapNotNull { WmoCode.of(it.condition.wmoCode)?.phase }.filter { it != WmoCode.Phase.NONE }.toSet()
        return when {
            phases == setOf(WmoCode.Phase.FROZEN) -> WmoCode.Phase.FROZEN
            WmoCode.Phase.FREEZING in phases -> WmoCode.Phase.FREEZING
            phases.isEmpty() -> WmoCode.Phase.NONE
            WmoCode.Phase.FROZEN in phases -> WmoCode.Phase.MIXED
            else -> phases.first()
        }
    }

    /**
     * The sky of the morning, the afternoon and the evening, and neighbouring parts of one
     * look as one stretch: the sun's (clear and mostly clear), the sun and cloud's, the
     * grey's, the fog's. Clear to mostly clear is not a change a reader looks up for.
     *
     * A part's sky is read the way the day's label reads the day's (STATES §6, step 3): **the
     * daylight's hours vote** in the morning and the afternoon, as the label counts only
     * those — the hours before sunrise are the night's, and two of them, overcast at 6 and
     * 7, made «Coperto al mattino» of a morning whose four hours of light had sun in them
     * (the README's Milan, 26 Sep 2026). The evening keeps all its hours: it is the evening
     * dark or light, and in winter it has no daylight to vote. A part with no hour of light
     * (the polar night) votes with all of them. A wet or possible hour votes with its own
     * cloud. The most frequent look wins, ties to the heavier, then within it the most
     * frequent word, ties to the heavier, as the label's own tie goes.
     *
     * A sky that holds all day says the label's own word when the label is a sky, so the
     * sentence and the icon never name two skies.
     */
    private fun sky(dayCode: Int, day: List<StripHour>): List<Sky> {
        val parts = Part.SKY.mapNotNull { part ->
            val inPart = day.filter { Part.of(it.hour.time) == part }
            val voters = if (part == Part.EVENING) inPart else inPart.filter { !it.night }.ifEmpty { inPart }
            voters.takeIf { it.isNotEmpty() }?.let { part to it.map { h -> skyOf(h.hour) } }
        }
        if (parts.isEmpty()) return emptyList()
        val stretches = mutableListOf<Stretch>()
        parts.forEach { (part, skies) ->
            val look = look(mostFrequent(skies))
            val previous = stretches.lastOrNull()
            if (previous != null && previous.look == look) {
                previous.parts += part
                previous.skies += skies
            } else {
                stretches += Stretch(look, mutableListOf(part), skies.toMutableList())
            }
        }
        if (stretches.size == 1) {
            val code = dayCode.takeIf { WmoCode.of(it)?.let { w -> w.phase == WmoCode.Phase.NONE && w.hazard == null } == true }
                ?: mostFrequent(stretches.single().skies)
            return listOf(Sky(code, stretches.single().parts))
        }
        return stretches.map { Sky(mostFrequent(it.skies), it.parts) }
    }

    /** The sky an hour shows: its own state when that is a sky or a fog, otherwise the one
     * its cloud makes by the light its layers take (rule 6), as the day's label reads it. */
    private fun skyOf(hour: HourlyForecast): Int {
        val code = WmoCode.of(hour.condition.wmoCode)
        return if (code != null && code.phase == WmoCode.Phase.NONE && code.hazard == null) code.code
        else WeatherStateEngine.skyOf(hour.cloudCoverPct, hour.cloudLayers)
    }

    /** The most frequent look, ties to the heavier; within it the most frequent word, ties to
     * the heavier. */
    private fun mostFrequent(skies: List<Int>): Int {
        val look = skies.groupingBy(::look).eachCount().maxWith(compareBy({ it.value }, { it.key.ordinal })).key
        return skies.filter { look(it) == look }.groupingBy { it }.eachCount()
            .maxWith(compareBy({ it.value }, { WmoCode.of(it.key)?.severity ?: -1 })).key
    }

    private enum class Look { SUN, SUN_AND_CLOUD, GREY, FOG }

    private class Stretch(val look: Look, val parts: MutableList<Part>, val skies: MutableList<Int>)

    private fun look(code: Int): Look = when {
        WmoCode.isFog(code) -> Look.FOG
        code == WmoCode.OVERCAST.code -> Look.GREY
        code == WmoCode.PARTLY_CLOUDY.code -> Look.SUN_AND_CLOUD
        else -> Look.SUN
    }
}
