package com.callbackdev.chiaro.ui.today

import com.callbackdev.chiaro.domain.ConditionWord
import com.callbackdev.chiaro.domain.DayPart
import com.callbackdev.chiaro.domain.WeatherStateEngine
import com.callbackdev.chiaro.domain.WmoCode
import com.callbackdev.chiaro.domain.model.HourlyForecast

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
 * - **Vaguer far out, as measured.** What falls was to be said by the hour («dalle 17:00
 *   alle 20:00») as far out as an hour is right two times in three, and by the part of the day
 *   beyond. Against the METARs of 26 airports in two periods the hour is right 65% of the time
 *   already tomorrow, under the bar, so no row says the hour. The part of the day («nel
 *   pomeriggio») holds the bar up to the third day and not from the fourth, where «it falls
 *   that day» still does: the last three rows say what falls without when ([TIMED_UNTIL_LEAD],
 *   the committente's decision on those numbers). Three runs or more are «a tratti».
 * - **The day's possible storm is its last clause** (Fase 17.5, 19 nov 2026): «possibile qualche
 *   temporale nel pomeriggio», from the day's own field, never from an hour's state — no hour
 *   of such a day is a storm, or the code would have spoken and the field would be empty.
 * - **Not today's row and not a closed one**: the caller's to keep (TodayWeek).
 */
object OpenDaySentence {

    /**
     * The last lead day whose falls say their part of the day (committente, 31 ott 2026, on the
     * numbers of PLANNING «La frase del giorno aperto»): «nel pomeriggio» is right 78%, 72% and
     * 68% of the time on days 1 to 3 (the third 67.0% in winter, 71.4% in October), and 64%,
     * 59% and 57% on days 4 to 6, under the two thirds the hour was held to, where «it falls
     * that day» is still right 80%, 74% and 70%. Tomorrow is lead 1.
     */
    const val TIMED_UNTIL_LEAD = 3

    /** Three runs or more are a broken day: «a tratti». */
    private const val SCATTERED_RUNS = 3

    /** Past this many clauses, what falls is one clause in the day's heaviest word: a sentence
     * never turns into a list. */
    private const val MAX_CLAUSES = 2

    /** One stretch of sky: [code]'s word over consecutive [parts] ([DayPart], on the place's
     * clock: the day's possible storm cuts the day in the same places). */
    data class Sky(val code: Int, val parts: List<DayPart>)

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
     * register's drop or flake. Untimed ([timed] false, the rows past [TIMED_UNTIL_LEAD]) it is
     * said without its parts.
     */
    data class Fall(
        val word: ConditionWord,
        val phase: WmoCode.Phase,
        val register: Register,
        val parts: List<DayPart>,
        val scattered: Boolean = false,
        val timed: Boolean = true
    ) {
        /** A fall said over the whole day, steady: the sky under it goes without saying. An
         * untimed one says no «tutto il giorno», and keeps the sky in front of it. */
        val allDay: Boolean get() = timed && !scattered && parts.size == DayPart.entries.size
    }

    /**
     * The day's possible storm (Fase 17.5): the [parts] of the day in which one is possible
     * though no hour is a storm, said with them when [timed], as a fall is.
     */
    data class Storm(val parts: List<DayPart>, val timed: Boolean)

    /** [sky] is empty when a steady fall takes the whole day; [falls] when nothing falls;
     * [storm] null when no storm is possible beyond what the hours say. */
    data class Sentence(val sky: List<Sky>, val falls: List<Fall>, val storm: Storm? = null)

    /**
     * The sentence of a day whose label is [dayCode] and whose hours are [day] (one local day,
     * in order, each with whether the sun is down), its falls with their part of the day when
     * [timed] (a lead day up to [TIMED_UNTIL_LEAD]) and without it, in one clause, otherwise.
     * [storm] is the day's possible storm ([com.callbackdev.chiaro.domain.model.DailyForecast.
     * possibleStorm]), its last clause, timed as the falls are: «nel pomeriggio» is right on 91%
     * of the days a storm comes (STATES §11), measured on the first day; up to the third it holds
     * the bar the falls were held to, and past it the clause has no when, as theirs has none.
     * Null when it has nothing true to say: no hours, no sky to read, or a label the hours
     * cannot back.
     */
    fun of(dayCode: Int, day: List<StripHour>, timed: Boolean = true, storm: List<DayPart> = emptyList()): Sentence? {
        if (day.isEmpty()) return null
        val hours = day.map { it.hour }
        val label = WmoCode.of(dayCode)
        val labelFalls = label != null && (label.isPrecipitation || label.hazard != null)

        val wetHours = hours.filter { WmoCode.isPrecipitation(it.condition.wmoCode) }
        val wet = inParts(wetHours, wet = labelFalls, scattered = runs(wetHours) >= SCATTERED_RUNS, timed)
        val possible = hours.filter { WmoCode.of(it.condition.wmoCode)?.let { w -> w.likely || w == WmoCode.THUNDERSTORM_POSSIBLE } == true }
            // A possibility of rain beside rain that falls is the same rain said twice; a
            // possible thunderstorm is a hazard of its own and is always said.
            .filter { wet.isEmpty() || it.condition.wmoCode == WmoCode.THUNDERSTORM_POSSIBLE.code }
        val falls = wet + inParts(possible, wet = null, scattered = false, timed)
        if (labelFalls && falls.isEmpty()) return null

        val sky = if (falls.any { it.register == Register.FULL && it.allDay }) emptyList() else sky(dayCode, day)
        val possibleStorm = storm.takeIf { it.isNotEmpty() }?.let { Storm(it, timed) }
        if (sky.isEmpty() && falls.isEmpty() && possibleStorm == null) return null
        return Sentence(sky, falls, possibleStorm)
    }

    /** How many runs of consecutive hours [wet] makes, by the instant as the ribbon's marks
     * are spoken ([DaylightPhases.wetRuns]): the night a zone falls back holds 02:00 twice. */
    private fun runs(wet: List<HourlyForecast>): Int =
        if (wet.isEmpty()) 0 else 1 + wet.zipWithNext().count { (a, b) -> b.at.isAfter(a.at.plusSeconds(3_600)) }

    /**
     * [hours] said by the parts of the day they fall in: each part takes the word of its
     * hours, neighbouring parts of one word are one clause, and more than [MAX_CLAUSES] are
     * one, in the day's heaviest word. Untimed, they are always one: «neve, pioggia» with no
     * when to tell them apart would be a list, «pioggia e neve» is the day. [wet] is whether
     * the label says something falls, null for the possibilities.
     */
    private fun inParts(hours: List<HourlyForecast>, wet: Boolean?, scattered: Boolean, timed: Boolean): List<Fall> {
        if (hours.isEmpty()) return emptyList()
        val byPart = hours.groupBy { DayPart.of(it.time) }.toSortedMap()
        val clauses = mutableListOf<Pair<List<HourlyForecast>, List<DayPart>>>()
        byPart.forEach { (part, inPart) ->
            val previous = clauses.lastOrNull()
            if (previous != null && previous.first.word() == inPart.word()) {
                clauses[clauses.lastIndex] = (previous.first + inPart) to (previous.second + part)
            } else {
                clauses += inPart to listOf(part)
            }
        }
        val grouped = if (clauses.size > (if (timed) MAX_CLAUSES else 1)) listOf(hours to byPart.keys.toList()) else clauses
        return grouped.map { (inClause, parts) -> inClause.fall(wet, parts, scattered).copy(timed = timed) }
    }

    /** The clause a group of hours makes. A hazard keeps its own word in every register:
     * ice and storms are never «qualche goccia». */
    private fun List<HourlyForecast>.fall(wet: Boolean?, parts: List<DayPart>, scattered: Boolean): Fall {
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
     * cloud. The most frequent look wins, ties to the lighter (fog excepted), then within it
     * the most frequent word, ties to the heavier, as the label's own tie goes.
     *
     * A sky that holds all day says the label's own word when the label is a sky, so the
     * sentence and the icon never name two skies.
     */
    private fun sky(dayCode: Int, day: List<StripHour>): List<Sky> {
        val parts = DayPart.SKY.mapNotNull { part ->
            val inPart = day.filter { DayPart.of(it.hour.time) == part }
            val voters = if (part == DayPart.EVENING) inPart else inPart.filter { !it.night }.ifEmpty { inPart }
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

    /**
     * The most frequent look; within it the most frequent word, ties to the heavier.
     *
     * Two looks that tie go to the LIGHTER since 23 nov 2026 (committente; PLANNING, «Le tre
     * cose del 23 nov 2026»): two hours of sun and cloud and two grey ones said «Overcast» over
     * a morning whose strip showed the sun, the sentence denying its own icons. Measured on the
     * daylight of the first day against the satellite (`analyze_against_metar.py --sky-ties`):
     * in the parts that tie, the satellite sees the look said in 60.5% of the hours with the
     * lighter against 25.6% with the heavier in the summer of 2026, 46.8% against 32.9% in
     * October. Fog still wins a tie: it was not measured, and it is a warning before it is a sky.
     */
    private fun mostFrequent(skies: List<Int>): Int {
        val look = skies.groupingBy(::look).eachCount()
            .maxWith(compareBy({ it.value }, { it.key == Look.FOG }, { -it.key.ordinal })).key
        return skies.filter { look(it) == look }.groupingBy { it }.eachCount()
            .maxWith(compareBy({ it.value }, { WmoCode.of(it.key)?.severity ?: -1 })).key
    }

    private enum class Look { SUN, SUN_AND_CLOUD, GREY, FOG }

    private class Stretch(val look: Look, val parts: MutableList<DayPart>, val skies: MutableList<Int>)

    private fun look(code: Int): Look = when {
        WmoCode.isFog(code) -> Look.FOG
        code == WmoCode.OVERCAST.code -> Look.GREY
        code == WmoCode.PARTLY_CLOUDY.code -> Look.SUN_AND_CLOUD
        else -> Look.SUN
    }
}
