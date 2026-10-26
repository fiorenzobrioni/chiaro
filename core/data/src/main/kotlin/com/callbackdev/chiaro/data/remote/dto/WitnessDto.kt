package com.callbackdev.chiaro.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull

/**
 * The witnesses' response ([com.callbackdev.chiaro.data.remote.OpenMeteoWitnessApi]).
 * A multi-model response names every column `<variable>_<model>`, so the block is kept as
 * it came and read by name: a model the provider does not serve at the place, or a
 * variable it does not carry, is simply a missing column.
 */
@Serializable
data class WitnessResponseDto(val hourly: JsonObject) {

    val time: List<String>
        get() = (hourly["time"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.content }.orEmpty()

    /** The column of [variable] for [model], or null when the response has none. */
    fun series(variable: String, model: String): List<Double?>? =
        (hourly["${variable}_$model"] as? JsonArray)?.map { (it as? JsonPrimitive)?.doubleOrNull }
}
