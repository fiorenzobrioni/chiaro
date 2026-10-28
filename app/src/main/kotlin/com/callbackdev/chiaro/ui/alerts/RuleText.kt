package com.callbackdev.chiaro.ui.alerts

import android.content.res.Resources
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.rules.RuleBand
import com.callbackdev.chiaro.domain.rules.RuleCondition
import com.callbackdev.chiaro.domain.rules.RuleOp
import com.callbackdev.chiaro.domain.rules.RuleMessages
import com.callbackdev.chiaro.domain.rules.RuleVariableKind
import com.callbackdev.chiaro.domain.rules.RuleVariables
import com.callbackdev.chiaro.domain.rules.RuleWindow
import com.callbackdev.chiaro.domain.rules.RuleWindowKind
import com.callbackdev.chiaro.domain.settings.PressureUnit
import com.callbackdev.chiaro.domain.settings.TemperatureUnit
import com.callbackdev.chiaro.domain.settings.UnitSettings
import com.callbackdev.chiaro.domain.settings.WindSpeedUnit
import com.callbackdev.chiaro.ui.format.Formats
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * The rules engine in words (VISION §5.4): every variable id becomes a phrase, every
 * operator a word, every threshold a number with its unit — and the dotted ids never
 * reach a screen. tweather shows `next_6h.precip_chance_max >= 60`; Chiaro says
 * "quando la pioggia nelle prossime 6 ore è almeno 60%". Same rule, same engine.
 */
object RuleText {

    /** The variable as a phrase. Unknown ids fail loudly in debug: a variable the
     * registry knows must have words before it ships. */
    fun nameRes(variableId: String): Int = when (variableId) {
        "current.temp_c" -> R.string.var_current_temp
        "current.feels_like_c" -> R.string.var_current_feels_like
        "current.humidity_pct" -> R.string.var_current_humidity
        "current.dew_point_c" -> R.string.var_current_dew_point
        "current.uv_index" -> R.string.var_current_uv
        "current.wind.speed_kph" -> R.string.var_current_wind
        "current.wind.gust_kph" -> R.string.var_current_gust
        "current.precipitation.chance_pct" -> R.string.var_current_precip
        "current.pressure_mb" -> R.string.var_current_pressure
        "current.visibility_km" -> R.string.var_current_visibility
        "current.aqi_index" -> R.string.var_current_aqi
        "current.aqi_eu_index" -> R.string.var_current_aqi_eu
        "next_6h.gust_max_kph" -> R.string.var_next6_gust
        "next_12h.gust_max_kph" -> R.string.var_next12_gust
        "today.precip_mm" -> R.string.var_today_precip_mm
        "today.snow_cm" -> R.string.var_today_snow
        "today.gust_max_kph" -> R.string.var_today_gust
        "next_6h.precip_chance_max" -> R.string.var_next6_precip
        "next_6h.temp_c_min" -> R.string.var_next6_temp_min
        "next_6h.temp_c_max" -> R.string.var_next6_temp_max
        "next_6h.wmo_severe" -> R.string.var_next6_severe
        "next_6h.feels_like_max_c" -> R.string.var_next6_feels_like
        "next_12h.feels_like_max_c" -> R.string.var_next12_feels_like
        "next_12h.precip_chance_max" -> R.string.var_next12_precip
        "next_12h.temp_c_min" -> R.string.var_next12_temp_min
        "next_12h.temp_c_max" -> R.string.var_next12_temp_max
        "next_12h.wmo_severe" -> R.string.var_next12_severe
        "today.high_c" -> R.string.var_today_high
        "today.low_c" -> R.string.var_today_low
        "today.precip_pct" -> R.string.var_today_precip
        "today.uv_max" -> R.string.var_today_uv
        else -> error("no words for rule variable $variableId")
    }

    /** The operator as the word the sentence needs; booleans read è / non è. */
    fun opRes(op: RuleOp, boolean: Boolean): Int = when {
        boolean && op == RuleOp.NEQ -> R.string.op_is_not
        boolean -> R.string.op_is
        else -> when (op) {
            RuleOp.GT -> R.string.op_above
            RuleOp.GTE -> R.string.op_at_least
            RuleOp.LT -> R.string.op_below
            RuleOp.LTE -> R.string.op_at_most
            RuleOp.EQ -> R.string.op_equal
            RuleOp.NEQ -> R.string.op_not_equal
        }
    }

    /** The threshold with its unit, in the reader's units: "20%", "12°", "40 km/h". */
    fun value(res: Resources, condition: RuleCondition, units: UnitSettings): String {
        val kind = RuleVariables.byId(condition.variable)?.kind ?: RuleVariableKind.NUMBER
        if (kind == RuleVariableKind.BOOLEAN) {
            return res.getString(
                if (condition.threshold != 0.0) R.string.value_yes else R.string.value_no
            )
        }
        val number = RuleVariables.formatValue(kind, condition.threshold, units)
        return number + unitSuffix(condition.variable, kind, units) +
            beaufortWord(kind, condition.threshold, units, res.configuration.locales[0])
    }

    /**
     * A value that was READ, for the notification's «why it fired» (23 set 2026): the
     * threshold's own formatting and unit, with the reader's decimal mark — a reading is
     * the one number here that is not a whole step of a picker, so it is the one that
     * shows the mark at all.
     */
    fun reading(
        res: Resources,
        variableId: String,
        value: Double,
        units: UnitSettings,
        locale: java.util.Locale
    ): String {
        val kind = RuleVariables.byId(variableId)?.kind ?: RuleVariableKind.NUMBER
        if (kind == RuleVariableKind.BOOLEAN) {
            return res.getString(if (value != 0.0) R.string.value_yes else R.string.value_no)
        }
        val separator = java.text.DecimalFormatSymbols.getInstance(locale).decimalSeparator
        val number = RuleVariables.formatValue(kind, value, units).replace('.', separator)
        return number + unitSuffix(variableId, kind, units) + beaufortWord(kind, value, units, locale)
    }

    /**
     * How a rule's message writes its placeholders for the reader (23 set 2026): the value
     * with its unit and the reader's decimal mark — «21,4°», «20%», «35 km/h» — and the
     * hour on the phone's own clock. Until then `{current.temp_c}` printed «21.4»: a bare
     * number, in the code's decimal point, with the unit left to whoever wrote the message.
     *
     * A unit the author already wrote right after the placeholder is not printed twice:
     * every message written before this change, the seeds included, says
     * «{current.temp_c}°», and it keeps reading «21,4°». A yes/no value is the word.
     */
    class MessageWriter(
        private val res: Resources,
        private val units: UnitSettings,
        private val locale: java.util.Locale,
        is24h: Boolean
    ) : RuleMessages.Writer {
        private val clock = com.callbackdev.chiaro.ui.format.Formats.timeFormatter(is24h, locale)

        override fun value(variableId: String?, kind: RuleVariableKind, value: Double, following: String): String {
            if (kind == RuleVariableKind.BOOLEAN) {
                return res.getString(if (value != 0.0) R.string.value_yes else R.string.value_no)
            }
            val separator = java.text.DecimalFormatSymbols.getInstance(locale).decimalSeparator
            val number = RuleVariables.formatValue(kind, value, units).replace('.', separator)
            val suffix = unitSuffix(variableId.orEmpty(), kind, units)
            val written = suffix.isNotEmpty() && following.trimStart().startsWith(suffix.trim())
            // A force whose «Bft» the author wrote keeps the author's line: the word
            // would land after their unit, inside their sentence.
            return if (written) number else number + suffix + beaufortWord(kind, value, units, locale)
        }

        override fun time(at: java.time.LocalDateTime): String = at.format(clock)
    }

    private fun unitSuffix(variableId: String, kind: RuleVariableKind, units: UnitSettings): String =
        when {
            kind == RuleVariableKind.TEMPERATURE -> "°"
            kind == RuleVariableKind.SPEED -> when (units.windSpeed) {
                WindSpeedUnit.KMH -> " km/h"
                WindSpeedUnit.MPH -> " mph"
                WindSpeedUnit.M_S -> " m/s"
                WindSpeedUnit.KNOTS -> " kn"
                WindSpeedUnit.BEAUFORT -> " Bft"
            }
            kind == RuleVariableKind.PRESSURE -> when (units.pressure) {
                PressureUnit.HPA -> " hPa"
                PressureUnit.INHG -> " inHg"
                PressureUnit.MMHG -> " mmHg"
            }
            variableId.contains("pct") || variableId.contains("chance") -> "%"
            variableId.contains("visibility") -> " km"
            variableId.endsWith("_mm") -> " mm"
            variableId.endsWith("_cm") -> " cm"
            else -> "" // UV and AQI are bare indexes
        }

    /**
     * The Beaufort scale's word after a force (14.9), «6 Bft (strong breeze)»: the same
     * sentence [Formats.wind] prints, so a threshold and a reading say the force the way
     * the tile and the headline do. Empty in every other unit.
     */
    private fun beaufortWord(
        kind: RuleVariableKind,
        canonicalKph: Double,
        units: UnitSettings,
        locale: java.util.Locale
    ): String =
        if (kind == RuleVariableKind.SPEED && units.windSpeed == WindSpeedUnit.BEAUFORT) {
            " (" + Formats.beaufortWord(canonicalKph, locale) + ")"
        } else {
            ""
        }

    /** One condition as its three words: "pioggia nelle prossime 6 ore · almeno · 20%". */
    fun sentence(res: Resources, condition: RuleCondition, units: UnitSettings): String {
        val boolean = RuleVariables.byId(condition.variable)?.kind == RuleVariableKind.BOOLEAN
        return res.getString(
            R.string.rule_sentence_fragment,
            res.getString(nameRes(condition.variable)),
            res.getString(opRes(condition.op, boolean)),
            value(res, condition, units)
        )
    }

    /** A piece of a rule's sentence on its card: a threshold ([value]) or the words around it. */
    data class Part(val text: String, val value: Boolean)

    /**
     * A rule's conditions as the card's one sentence, in pieces so the card can set the
     * thresholds in full weight (the Alerts review, point 6, 14 ott 2026).
     *
     * Two conditions on the same quantity are one phrase, because a reader says it that way:
     * «at least 5°» and «at most 26°» are «between 5° and 26°», and «above 0°» and «below 5°»
     * keep their words but say the quantity once. The «A window to run» idea read «the
     * temperature now is at least 5° and the temperature now is at most 26°», six lines on a
     * 360 dp screen. Only the words change: «between» is said only for the two inclusive
     * bounds, the pair the words mean exactly, and the rule is the same rule.
     */
    fun sentenceParts(res: Resources, conditions: List<RuleCondition>, units: UnitSettings): List<Part> {
        val and = " " + res.getString(R.string.rule_and) + " "
        val used = BooleanArray(conditions.size)
        val phrases = mutableListOf<List<Part>>()
        conditions.forEachIndexed { i, condition ->
            if (used[i]) return@forEachIndexed
            used[i] = true
            val twin = (i + 1 until conditions.size)
                .firstOrNull { !used[it] && conditions[it].variable == condition.variable }
            if (twin == null) {
                phrases += fragmentParts(res, condition, units)
                return@forEachIndexed
            }
            used[twin] = true
            val other = conditions[twin]
            val pair = listOf(condition, other)
            val lower = pair.firstOrNull { it.op == RuleOp.GTE }
            val upper = pair.firstOrNull { it.op == RuleOp.LTE }
            phrases += if (lower != null && upper != null && lower.threshold <= upper.threshold) {
                withValues(
                    res.getString(R.string.rule_sentence_range, res.getString(nameRes(condition.variable)), marker(0), marker(1)),
                    listOf(value(res, lower, units), value(res, upper, units))
                )
            } else {
                val boolean = RuleVariables.byId(other.variable)?.kind == RuleVariableKind.BOOLEAN
                fragmentParts(res, condition, units) +
                    Part(and + res.getString(opRes(other.op, boolean)) + " ", value = false) +
                    Part(value(res, other, units), value = true)
            }
        }
        return phrases.flatMapIndexed { index, phrase ->
            if (index == 0) phrase else listOf(Part(and, value = false)) + phrase
        }
    }

    private fun fragmentParts(res: Resources, condition: RuleCondition, units: UnitSettings): List<Part> {
        val boolean = RuleVariables.byId(condition.variable)?.kind == RuleVariableKind.BOOLEAN
        return withValues(
            res.getString(
                R.string.rule_sentence_fragment,
                res.getString(nameRes(condition.variable)),
                res.getString(opRes(condition.op, boolean)),
                marker(0)
            ),
            listOf(value(res, condition, units))
        )
    }

    /** [text] cut at its markers, each replaced by its value: whatever order a translation
     * puts them in, the earliest marker is the next cut. */
    private fun withValues(text: String, values: List<String>): List<Part> {
        val parts = mutableListOf<Part>()
        var rest = text
        val left = values.indices.toMutableSet()
        while (left.isNotEmpty()) {
            val next = left.mapNotNull { k -> rest.indexOf(marker(k)).takeIf { it >= 0 }?.let { k to it } }
                .minByOrNull { it.second } ?: break
            val (k, at) = next
            if (at > 0) parts += Part(rest.substring(0, at), value = false)
            parts += Part(values[k], value = true)
            rest = rest.substring(at + marker(k).length)
            left -= k
        }
        if (rest.isNotEmpty()) parts += Part(rest, value = false)
        return parts
    }

    private fun marker(k: Int) = "\u0000$k\u0000"

    // ------------------------------------------------------------- hours

    /** One band in words: «dalle 07:00 alle 09:00», on the reader's clock. */
    fun band(res: Resources, band: RuleBand, clock: java.time.format.DateTimeFormatter): String =
        res.getString(R.string.rule_band_range, band.fromTime.format(clock), band.toTime.format(clock))

    /**
     * The card's line about the hours (26 set 2026): «Solo con la luce», «Solo dalle 07:00
     * alle 09:00 e dalle 17:00 alle 19:30». Null for any hour, which is what a rule with no
     * line has always meant.
     */
    fun window(res: Resources, window: RuleWindow, clock: java.time.format.DateTimeFormatter): String? =
        when (window.kind) {
            RuleWindowKind.ALWAYS -> null
            RuleWindowKind.DAYLIGHT -> res.getString(R.string.rule_card_daylight)
            RuleWindowKind.HOURS -> window.bands.filter { it.from != it.to }
                .takeIf { it.isNotEmpty() }
                ?.joinToString(" " + res.getString(R.string.rule_and) + " ") { band(res, it, clock) }
                ?.let { res.getString(R.string.rule_card_hours, it) }
        }

    // ------------------------------------------------------------- value ranges

    /**
     * What the value picker offers, in the reader's units (15.5, 28 Oct 2026) — pickers,
     * never a free-text field for a value with a range (VISION §5.4: a syntax error is not
     * writable, and neither is a nonsense threshold). The rule keeps its °C, km/h and hPa:
     * a position goes through [RuleVariables.canonicalValue] on its way to the rule and
     * [RuleVariables.displayValue] on its way back, so the same threshold never rewrites
     * itself when the unit setting changes. Until 15.5 the spec was canonical and only the
     * label converted: in Fahrenheit the slider walked 1 °C a step and printed «53,6°»,
     * «55,4°»; in Beaufort 5 km/h a step gave several positions with one label. Now the grid
     * is the unit's own: whole where the unit is whole (a degree Fahrenheit, a mile an hour,
     * a metre a second, a knot, a force), a tenth of an inch of mercury, where that unit's
     * readers move by one (29.9, 30.0), 5 mmHg as 5 hPa. The metric grids are the ones of
     * before.
     */
    data class ValueSpec(val min: Double, val max: Double, val step: Double)

    fun valueSpec(variableId: String, units: UnitSettings): ValueSpec = when {
        // «feels_like» since 14.6: the feels-like now had the chance's 0–100 before, a
        // range a temperature in Fahrenheit or a winter's night did not fit.
        variableId.contains("temp") || variableId.contains("dew_point") ||
            variableId.contains("feels_like") ||
            variableId.contains("high_c") || variableId.contains("low_c") ->
            if (units.temperature == TemperatureUnit.FAHRENHEIT) {
                ValueSpec(-22.0, 113.0, 1.0)
            } else {
                ValueSpec(-30.0, 45.0, 1.0)
            }
        variableId.contains("chance") || variableId.contains("precip_pct") ||
            variableId.contains("humidity") -> ValueSpec(0.0, 100.0, 5.0)
        variableId.contains("uv") -> ValueSpec(0.0, 12.0, 1.0)
        variableId.contains("wind") -> when (units.windSpeed) {
            WindSpeedUnit.KMH -> ValueSpec(0.0, 120.0, 5.0)
            WindSpeedUnit.MPH -> ValueSpec(0.0, 75.0, 5.0)
            WindSpeedUnit.M_S -> ValueSpec(0.0, 35.0, 1.0)
            WindSpeedUnit.KNOTS -> ValueSpec(0.0, 65.0, 5.0)
            WindSpeedUnit.BEAUFORT -> ValueSpec(0.0, 12.0, 1.0)
        }
        variableId.contains("gust") -> when (units.windSpeed) {
            WindSpeedUnit.KMH -> ValueSpec(0.0, 150.0, 5.0)
            WindSpeedUnit.MPH -> ValueSpec(0.0, 95.0, 5.0)
            WindSpeedUnit.M_S -> ValueSpec(0.0, 40.0, 1.0)
            WindSpeedUnit.KNOTS -> ValueSpec(0.0, 80.0, 5.0)
            WindSpeedUnit.BEAUFORT -> ValueSpec(0.0, 12.0, 1.0)
        }
        variableId.endsWith("_mm") || variableId.endsWith("_cm") -> ValueSpec(0.0, 100.0, 1.0)
        variableId.contains("pressure") -> when (units.pressure) {
            PressureUnit.HPA -> ValueSpec(950.0, 1050.0, 5.0)
            PressureUnit.INHG -> ValueSpec(28.0, 31.0, 0.1)
            PressureUnit.MMHG -> ValueSpec(710.0, 790.0, 5.0)
        }
        variableId.contains("visibility") -> ValueSpec(0.0, 50.0, 1.0)
        variableId.contains("aqi_eu") -> ValueSpec(0.0, 150.0, 5.0)
        variableId.contains("aqi") -> ValueSpec(0.0, 300.0, 10.0)
        else -> ValueSpec(0.0, 100.0, 1.0)
    }

    /** How many steps the grid has, from [ValueSpec.min] to [ValueSpec.max]. */
    fun positions(spec: ValueSpec): Int = Math.round((spec.max - spec.min) / spec.step).toInt()

    /**
     * The tick marks the slider draws, the `steps` of Material's `Slider`: one between each
     * pair of positions up to twenty steps (humidity's twenty, UV's twelve, the Beaufort
     * forces), none past that (15.5). A temperature drew 74 marks and the millimetres 99: a
     * comb under the thumb, too dense to mark anything. Without them the slider is
     * continuous and [snap] keeps the grid by hand, so the label never shows a value the
     * thumb cannot come back to.
     */
    fun ticks(spec: ValueSpec): Int = positions(spec).let { if (it <= MaxTicks) it - 1 else 0 }

    /** A slider position pulled to the grid and kept inside the range. */
    fun snap(spec: ValueSpec, value: Double): Double =
        (Math.round((value - spec.min) / spec.step) * spec.step + spec.min)
            .coerceIn(spec.min, spec.max)

    /**
     * A canonical threshold kept when the reader's range holds it and pulled to the nearest
     * edge when it does not (the chip that changes what a condition watches, where the old
     * threshold meets a new range). Inside the range it is left exactly as it is, on the
     * grid or off it: a 72 km/h written in km/h reads «44,7 mph» in miles until the reader
     * moves it, and is never rewritten by a look (15.5).
     */
    fun inRange(variableId: String, canonical: Double, units: UnitSettings): Double {
        val kind = RuleVariables.byId(variableId)?.kind ?: RuleVariableKind.NUMBER
        val spec = valueSpec(variableId, units)
        val shown = RuleVariables.displayValue(kind, canonical, units)
        return if (shown in spec.min..spec.max) {
            canonical
        } else {
            RuleVariables.canonicalValue(kind, shown.coerceIn(spec.min, spec.max), units)
        }
    }

    private const val MaxTicks = 20

    // ------------------------------------------------------------- templates

    /**
     * The six starting points (VISION §5.4): picking one creates a REAL rule with
     * sensible thresholds, already on — the builder is for adjusting it, not for
     * building from nothing. Name and message become user content at creation, in
     * the reader's language, and are never translated again.
     *
     * The seed names are CAPITALIZED (committente, 4 set). They arrived lowercase
     * from tweather, where `alerts.rules` is a configuration file and a lowercase
     * identifier is the code register — the one register this product deliberately
     * does not have (CLAUDE.md). Here the name is a proper name in a title's place:
     * the rule's card, and the notification's own `“Bike” · Milan`. Only the SEED
     * changes: a rule already saved keeps whatever the reader called it, because
     * that is their text and not ours to correct.
     */
    data class Template(
        val titleRes: Int,
        val descriptionRes: Int,
        val nameRes: Int,
        val messageRes: Int,
        val conditions: List<RuleCondition>,
        /** The hours it starts with (26 set 2026): the outdoor ideas are born «Con la
         * luce», because an invitation to ride or run is about the daylight too — the
         * Bike idea's rule fired at 21:47. The rest keep any hour: «Ghiaccio domattina»
         * is useful precisely the evening before. */
        val window: RuleWindow = RuleWindow.Always
    )

    /**
     * The blank idea (26 set 2026, committente): the first card of the row, for a rule the
     * ideas do not cover. Not empty — the builder cannot hold a rule with no condition, and
     * a working sentence to change beats a skeleton to fill — but neutral: the condition the
     * «and another condition» button adds, a message that fits whatever the reader makes of
     * it, any hour. Kept out of [templates]: it is never «already added», and the ideas'
     * order is what the README's picture and its tests name.
     */
    val blank = Template(
        R.string.tpl_blank_title, R.string.tpl_blank_desc,
        R.string.tpl_blank_name, R.string.tpl_blank_message,
        listOf(RuleCondition("current.temp_c", RuleOp.GTE, 20.0))
    )

    /**
     * [base], or «[base] 2», «[base] 3»… — the first that no rule in [taken] carries. A name
     * is unique (15.4, 28 Oct 2026): it is the card's title and the notification's, and two
     * «Bici» confuse before any history does.
     */
    fun freeName(base: String, taken: Collection<String>): String =
        generateSequence(1) { it + 1 }
            .map { if (it == 1) base else "$base $it" }
            .first { !isTaken(it, taken) }

    /**
     * Whether [name] is already one of [taken], the other rules' names (15.4, 28 Oct 2026):
     * the editor's field says so while the reader types. Case and the blanks around do not
     * make a second name: «bici» after «Bici» is the same word on two cards.
     */
    fun isTaken(name: String, taken: Collection<String>): Boolean =
        name.trim().let { typed -> taken.any { it.trim().equals(typed, ignoreCase = true) } }

    /**
     * The name the editor keeps when it closes (15.4, 28 Oct 2026): [typed], trimmed, unless it
     * is empty or already another rule's ([taken]), when the rule keeps [current], the name it
     * had. The field said so while it was typed; the close does not argue.
     */
    fun keptName(typed: String, current: String, taken: Collection<String>): String =
        typed.trim().let { if (it.isEmpty() || isTaken(it, taken)) current else it }

    /**
     * When a rule last fired, in the bulletin's grammar (15.7, 28 Oct 2026; the Alerts
     * review's point 11): «oggi alle 7:12», «ieri alle 7:12», and beyond yesterday the day,
     * «il giorno 25 ott alle 7:12» — the day only when it is not today, as `WarningText.issued`
     * says a bulletin's hour, so the two lines of one card do not say a time two ways.
     * [dateFmt] is day and month: the history keeps three days, so the year is never in doubt.
     */
    fun firedAt(
        res: Resources,
        at: LocalDateTime,
        today: LocalDate,
        timeFmt: DateTimeFormatter,
        dateFmt: DateTimeFormatter
    ): String {
        val time = at.toLocalTime().format(timeFmt)
        return when (val day = at.toLocalDate()) {
            today -> res.getString(R.string.rule_fired_today, time)
            today.minusDays(1) -> res.getString(R.string.rule_fired_yesterday, time)
            else -> res.getString(R.string.rule_fired_dated, day.format(dateFmt), time)
        }
    }

    /** A group of «Cosa guardare» (15.7, 28 Oct 2026): its heading and its variables' ids. */
    class VariableGroup(val titleRes: Int, val ids: List<String>)

    /**
     * The registry's variables in the four groups a reader thinks in — now, the next six
     * hours, the next twelve, today — each in the registry's own order (15.7, 28 Oct 2026).
     * The id's first word is the group: every variable has one, and one only
     * (`VariableGroupsTest`), and the dotted id itself never reaches the sheet.
     */
    val variableGroups: List<VariableGroup> = listOf(
        "current." to R.string.rule_var_group_now,
        "next_6h." to R.string.rule_var_group_next6,
        "next_12h." to R.string.rule_var_group_next12,
        "today." to R.string.rule_var_group_today
    ).map { (prefix, titleRes) ->
        VariableGroup(titleRes, RuleVariables.all.map { it.id }.filter { it.startsWith(prefix) })
    }

    /** Where the plain run ends and the cold one begins: the practical line of an
     * amateur runner, under which gloves, layers and a longer warm-up come out. */
    private const val RunColdBelowC = 5.0

    val templates: List<Template> = listOf(
        Template(
            R.string.tpl_bike_title, R.string.tpl_bike_desc,
            R.string.tpl_bike_name, R.string.tpl_bike_message,
            listOf(
                RuleCondition("current.temp_c", RuleOp.GTE, 12.0),
                RuleCondition("next_6h.precip_chance_max", RuleOp.LTE, 20.0)
            ),
            RuleWindow.Daylight
        ),
        Template(
            R.string.tpl_ice_title, R.string.tpl_ice_desc,
            R.string.tpl_ice_name, R.string.tpl_ice_message,
            listOf(RuleCondition("next_12h.temp_c_min", RuleOp.LTE, 0.0))
        ),
        // The run in two bands (26 set 2026, committente): from 5° it is the plain
        // invitation; between 0 and 5 it still runs, with the cold's precautions; at 0 and
        // under there is no idea at all, because an invitation to run over ice is the one
        // risk the app cannot see (ice on the asphalt is the wet ground's, not the air's)
        // and «Ghiaccio domattina» already speaks at that same 0°. The bands meet without
        // a gap or an overlap: 5° is the plain run's, 0° is neither's.
        Template(
            R.string.tpl_run_title, R.string.tpl_run_desc,
            R.string.tpl_run_name, R.string.tpl_run_message,
            listOf(
                RuleCondition("next_6h.precip_chance_max", RuleOp.LTE, 20.0),
                RuleCondition("current.temp_c", RuleOp.GTE, RunColdBelowC),
                RuleCondition("current.temp_c", RuleOp.LTE, 26.0)
            ),
            RuleWindow.Daylight
        ),
        Template(
            R.string.tpl_cold_run_title, R.string.tpl_cold_run_desc,
            R.string.tpl_cold_run_name, R.string.tpl_cold_run_message,
            listOf(
                RuleCondition("next_6h.precip_chance_max", RuleOp.LTE, 20.0),
                RuleCondition("current.temp_c", RuleOp.GT, 0.0),
                RuleCondition("current.temp_c", RuleOp.LT, RunColdBelowC)
            ),
            RuleWindow.Daylight
        ),
        Template(
            R.string.tpl_uv_title, R.string.tpl_uv_desc,
            R.string.tpl_uv_name, R.string.tpl_uv_message,
            listOf(RuleCondition("today.uv_max", RuleOp.GTE, 7.0))
        ),
        // The heat's twin of the frost above (23 set 2026, notification review): a
        // summer in the Po valley reaches 33° for weeks, and it is the one day-shaped
        // risk the ideas did not cover. On today's high, so it speaks once a day, in
        // the morning, when the day can still be planned around it.
        Template(
            R.string.tpl_heat_title, R.string.tpl_heat_desc,
            R.string.tpl_heat_name, R.string.tpl_heat_message,
            listOf(RuleCondition("today.high_c", RuleOp.GTE, 33.0))
        )
        // No «Una notte serena» (15.6, 28 Oct 2026). VISION sketched a clear night; the
        // registry has no cloud variable and no hour variable, so the idea became «Dodici
        // ore asciutte» (rain under 10% in the next twelve hours), which fired at nine in
        // the morning, up to twice a day, and said nothing about any night. It left the
        // ideas; the rules a reader made from it stay, as their own text. It comes back as
        // the clear night the day the registry has a cloud variable and an hour, not before.
    )
}
