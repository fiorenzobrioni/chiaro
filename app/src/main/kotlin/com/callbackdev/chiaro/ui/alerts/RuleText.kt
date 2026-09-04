package com.callbackdev.chiaro.ui.alerts

import android.content.res.Resources
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.rules.RuleCondition
import com.callbackdev.chiaro.domain.rules.RuleOp
import com.callbackdev.chiaro.domain.rules.RuleVariableKind
import com.callbackdev.chiaro.domain.rules.RuleVariables
import com.callbackdev.chiaro.domain.settings.UnitSettings
import com.callbackdev.chiaro.domain.settings.WindSpeedUnit

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
        "next_6h.precip_chance_max" -> R.string.var_next6_precip
        "next_6h.temp_c_min" -> R.string.var_next6_temp_min
        "next_6h.temp_c_max" -> R.string.var_next6_temp_max
        "next_6h.wmo_severe" -> R.string.var_next6_severe
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
        return number + unitSuffix(condition.variable, kind, units)
    }

    private fun unitSuffix(variableId: String, kind: RuleVariableKind, units: UnitSettings): String =
        when {
            kind == RuleVariableKind.TEMPERATURE -> "°"
            kind == RuleVariableKind.SPEED ->
                if (units.windSpeed == WindSpeedUnit.MPH) " mph" else " km/h"
            variableId.contains("pct") || variableId.contains("chance") -> "%"
            variableId.contains("pressure") -> " hPa"
            variableId.contains("visibility") -> " km"
            else -> "" // UV and AQI are bare indexes
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

    // ------------------------------------------------------------- value ranges

    /**
     * What the value picker offers, in canonical metric units — pickers, never a
     * free-text field for a value with a range (VISION §5.4: a syntax error is not
     * writable, and neither is a nonsense threshold).
     */
    data class ValueSpec(val min: Double, val max: Double, val step: Double)

    fun valueSpec(variableId: String): ValueSpec = when {
        variableId.contains("temp") || variableId.contains("dew_point") ||
            variableId.contains("high_c") || variableId.contains("low_c") ->
            ValueSpec(-30.0, 45.0, 1.0)
        variableId.contains("chance") || variableId.contains("precip_pct") ||
            variableId.contains("humidity") -> ValueSpec(0.0, 100.0, 5.0)
        variableId.contains("uv") -> ValueSpec(0.0, 12.0, 1.0)
        variableId.contains("wind") -> ValueSpec(0.0, 120.0, 5.0)
        variableId.contains("pressure") -> ValueSpec(950.0, 1050.0, 5.0)
        variableId.contains("visibility") -> ValueSpec(0.0, 50.0, 1.0)
        variableId.contains("aqi") -> ValueSpec(0.0, 300.0, 10.0)
        else -> ValueSpec(0.0, 100.0, 1.0)
    }

    // ------------------------------------------------------------- templates

    /**
     * The five starting points (VISION §5.4): picking one creates a REAL rule with
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
        val conditions: List<RuleCondition>
    )

    val templates: List<Template> = listOf(
        Template(
            R.string.tpl_bike_title, R.string.tpl_bike_desc,
            R.string.tpl_bike_name, R.string.tpl_bike_message,
            listOf(
                RuleCondition("current.temp_c", RuleOp.GTE, 12.0),
                RuleCondition("next_6h.precip_chance_max", RuleOp.LTE, 20.0)
            )
        ),
        Template(
            R.string.tpl_ice_title, R.string.tpl_ice_desc,
            R.string.tpl_ice_name, R.string.tpl_ice_message,
            listOf(RuleCondition("next_12h.temp_c_min", RuleOp.LTE, 0.0))
        ),
        Template(
            R.string.tpl_run_title, R.string.tpl_run_desc,
            R.string.tpl_run_name, R.string.tpl_run_message,
            listOf(
                RuleCondition("next_6h.precip_chance_max", RuleOp.LTE, 20.0),
                RuleCondition("current.temp_c", RuleOp.LTE, 26.0)
            )
        ),
        Template(
            R.string.tpl_uv_title, R.string.tpl_uv_desc,
            R.string.tpl_uv_name, R.string.tpl_uv_message,
            listOf(RuleCondition("today.uv_max", RuleOp.GTE, 7.0))
        ),
        // VISION sketched "a clear night"; the registry has no cloud variable, so
        // this template speaks of rain — the only clearness it can actually verify.
        // Promising "clear" on a rain-only check would be the notification lying.
        Template(
            R.string.tpl_night_title, R.string.tpl_night_desc,
            R.string.tpl_night_name, R.string.tpl_night_message,
            listOf(RuleCondition("next_12h.precip_chance_max", RuleOp.LTE, 10.0))
        )
    )
}
