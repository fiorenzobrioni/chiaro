package com.callbackdev.chiaro.data.mapper

import com.callbackdev.chiaro.data.remote.dto.HourlyDto
import com.callbackdev.chiaro.data.remote.dto.WitnessResponseDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The witnesses laid on `best_match`'s hours (26 ott 2026): by their labels, without the
 * one that is `best_match` itself, and without the one variable `best_match` carries as a
 * witness's copy (27 ott 2026).
 */
class WitnessesTest {

    private val hours = 24
    private val times = List(hours) { "2026-10-26T%02d:00".format(it) }

    private fun best(
        temperature: (Int) -> Double = { 10.0 + it * 0.1 },
        cloud: (Int) -> Int = { 50 },
        visibility: (Int) -> Double? = { 20_000.0 }
    ) = HourlyDto(
        time = times,
        temperatureC = List(hours, temperature),
        weatherCode = List(hours) { 3 },
        precipitationProbabilityPct = List(hours) { null },
        isDay = List(hours) { 1 },
        visibilityM = List(hours, visibility),
        cloudCoverPct = List(hours, cloud)
    )

    private val ukmo = "ukmo_global_deterministic_10km"
    private val knmi = "knmi_harmonie_arome_europe"

    /** A response with one column per (variable, model) as Open-Meteo writes them. */
    private fun response(time: List<String> = times, columns: Map<String, List<Number?>>): WitnessResponseDto {
        val body = buildString {
            append("""{"hourly":{"time":[""")
            append(time.joinToString(",") { "\"$it\"" })
            append("]")
            columns.forEach { (name, values) -> append(""","$name":[${values.joinToString(",") { it?.toString() ?: "null" }}]""") }
            append("}}")
        }
        val hourly = Json.parseToJsonElement(body).jsonObject["hourly"] as JsonObject
        return WitnessResponseDto(hourly)
    }

    private fun witness(
        model: String,
        temperature: (Int) -> Double,
        visibility: (Int) -> Double = { 300.0 }
    ): Map<String, List<Number?>> = mapOf(
        "temperature_2m_$model" to List(hours, temperature),
        "cloud_cover_$model" to List(hours) { 50 },
        "visibility_$model" to List(hours, visibility),
        "relative_humidity_2m_$model" to List(hours) { 98 }
    )

    @Test
    fun `no response is no witness`() {
        assertTrue(Witnesses.of(null, best()).at(best(), instant = 5).isEmpty())
    }

    @Test
    fun `each family that is not best_match votes, with its own numbers`() {
        val r = response(columns = witness("ecmwf_ifs", { 7.0 }) + witness(ukmo, { 12.0 }, { 24_000.0 }))
        val at = Witnesses.of(r, best()).at(best(), instant = 10)
        assertEquals(listOf(300.0, 24_000.0), at.map { it.visibilityM })
        assertEquals(98, at.first().humidityPct)
    }

    @Test
    fun `a witness that is best_match itself is left out`() {
        // ECMWF over Sydney all week: the same temperature and cloud, hour by hour.
        val r = response(columns = witness("ecmwf_ifs", { 10.0 + it * 0.1 }) + witness(ukmo, { 12.0 }))
        val at = Witnesses.of(r, best()).at(best(), instant = 10)
        assertEquals(1, at.size)
        assertEquals(300.0, at.single().visibilityM)
    }

    @Test
    fun `a visibility equal to a witness's is the witness's, and the hour has none of its own`() {
        // Oslo, every hour of both periods measured in 14.2: MET Nordic's temperature, no
        // visibility of its own, and Open-Meteo's fill from IFS. Until 27 ott 2026 IFS then
        // confirmed itself.
        val fill = best(visibility = { 300.0 + it })
        val r = response(columns = witness("ecmwf_ifs", { 7.0 }, { 300.0 + it }) + witness(ukmo, { 12.0 }, { 24_000.0 }))
        val voters = Witnesses.of(r, fill)
        assertNull(voters.ownVisibilityOf(fill, instant = 10))
        val at = voters.at(fill, instant = 10)
        assertEquals(2, at.size)
        // IFS still votes, and lends, with its own number — which now counts once.
        assertEquals(listOf(310.0, 24_000.0), at.map { it.visibilityM })
        assertEquals(listOf(98, 98), at.map { it.humidityPct })
        // The same numbers under a `best_match` that is not the fill: its own.
        assertEquals(20_000.0, Witnesses.of(r, best()).ownVisibilityOf(best(), instant = 10))
        assertEquals(listOf(310.0, 24_000.0), Witnesses.of(r, best()).at(best(), instant = 10).map { it.visibilityM })
    }

    @Test
    fun `a witness that is best_match whole leaves the hour's visibility its own`() {
        // Longyearbyen: IFS is `best_match` whole, so its visibility is the hour's, not a copy.
        val r = response(columns = witness("ecmwf_ifs", { 10.0 + it * 0.1 }, { 20_000.0 }) + witness(ukmo, { 12.0 }))
        val voters = Witnesses.of(r, best())
        assertEquals(20_000.0, voters.ownVisibilityOf(best(), instant = 10))
        assertEquals(1, voters.at(best(), instant = 10).size)
    }

    @Test
    fun `a copy is judged on the six hours around, not on one equal number`() {
        // The two agree at hour 10 alone: chance, not a fill.
        val r = response(columns = witness("ecmwf_ifs", { 7.0 }, { if (it == 10) 20_000.0 else 300.0 }))
        val voters = Witnesses.of(r, best())
        assertEquals(20_000.0, voters.ownVisibilityOf(best(), instant = 10))
        assertEquals(20_000.0, voters.at(best(), instant = 10).single().visibilityM)
    }

    @Test
    fun `an hour with no visibility of its own has none, witnesses or not`() {
        val none = best(visibility = { null })
        val r = response(columns = witness("ecmwf_ifs", { 7.0 }) + witness(knmi, { 9.0 }, { 600.0 }))
        val voters = Witnesses.of(r, none)
        assertNull(voters.ownVisibilityOf(none, instant = 10))
        assertEquals(listOf(300.0, 600.0), voters.at(none, instant = 10).map { it.visibilityM })
    }

    @Test
    fun `a witness that becomes best_match later in the week is left out only there`() {
        // ECMWF over the north of Europe from the fourth day: a model change on the edge of a day.
        val r = response(columns = witness("ecmwf_ifs", { if (it < 12) 5.0 else 10.0 + it * 0.1 }))
        val voters = Witnesses.of(r, best())
        assertEquals(1, voters.at(best(), instant = 2).size)
        assertTrue(voters.at(best(), instant = 20).isEmpty())
    }

    @Test
    fun `hours are matched by their labels, and an hour the witness lacks is a missing value`() {
        val r = response(time = times.drop(2), columns = witness("ecmwf_ifs", { 5.0 }).mapValues { it.value.drop(2) })
        val voters = Witnesses.of(r, best())
        assertNull(voters.at(best(), instant = 1).single().visibilityM)
        assertEquals(300.0, voters.at(best(), instant = 5).single().visibilityM)
    }

    @Test
    fun `a model the provider does not serve at the place is no witness`() {
        val r = response(columns = mapOf<String, List<Number?>>("visibility_ecmwf_ifs" to List(hours) { 300.0 }))
        assertTrue(Witnesses.of(r, best()).at(best(), instant = 5).isEmpty())
    }
}
