package com.callbackdev.chiaro.data.mapper

import com.callbackdev.chiaro.data.remote.dto.HourlyDto
import com.callbackdev.chiaro.data.remote.dto.WitnessResponseDto
import com.callbackdev.chiaro.domain.ModelMean
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The witnesses laid on `best_match`'s hours (26 ott 2026): by their labels, and without the
 * one that is `best_match` itself.
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
        assertTrue(Witnesses.of(null, best()).at(instant = 5).isEmpty())
    }

    @Test
    fun `each family that is not best_match votes, with its own numbers`() {
        val r = response(columns = witness("ecmwf_ifs", { 7.0 }) + witness(ukmo, { 12.0 }, { 24_000.0 }))
        val at = Witnesses.of(r, best()).at(instant = 10)
        assertEquals(listOf(300.0, 24_000.0), at.map { it.visibilityM })
        assertEquals(98, at.first().humidityPct)
    }

    @Test
    fun `a witness that is best_match itself is left out`() {
        // ECMWF over Sydney all week: the same temperature and cloud, hour by hour.
        val r = response(columns = witness("ecmwf_ifs", { 10.0 + it * 0.1 }) + witness(ukmo, { 12.0 }))
        val at = Witnesses.of(r, best()).at(instant = 10)
        assertEquals(1, at.size)
        assertEquals(300.0, at.single().visibilityM)
    }

    @Test
    fun `the witnesses vote with their own numbers, whatever best_match's visibility is`() {
        // Oslo, every hour of both periods measured in 14.2: MET Nordic's temperature with
        // Open-Meteo's fill of IFS's visibility. Since 17 nov 2026 `best_match`'s visibility is
        // no vote at all, so the fill cannot count twice: IFS votes once, with its own number,
        // under a fill, under a visibility of `best_match`'s own and under none (Italy from
        // 30 Nov 2026).
        val r = response(columns = witness("ecmwf_ifs", { 7.0 }, { 300.0 + it }) + witness(ukmo, { 12.0 }, { 24_000.0 }))
        for (b in listOf(best(visibility = { 300.0 + it }), best(), best(visibility = { null }))) {
            val at = Witnesses.of(r, b).at(instant = 10)
            assertEquals(listOf(310.0, 24_000.0), at.map { it.visibilityM })
            assertEquals(listOf(98, 98), at.map { it.humidityPct })
        }
    }

    @Test
    fun `a witness that becomes best_match later in the week is left out only there`() {
        // ECMWF over the north of Europe from the fourth day: a model change on the edge of a day.
        val r = response(columns = witness("ecmwf_ifs", { if (it < 12) 5.0 else 10.0 + it * 0.1 }))
        val voters = Witnesses.of(r, best())
        assertEquals(1, voters.at(instant = 2).size)
        assertTrue(voters.at(instant = 20).isEmpty())
    }

    @Test
    fun `hours are matched by their labels, and an hour the witness lacks is a missing value`() {
        val r = response(time = times.drop(2), columns = witness("ecmwf_ifs", { 5.0 }).mapValues { it.value.drop(2) })
        val voters = Witnesses.of(r, best())
        assertNull(voters.at(instant = 1).single().visibilityM)
        assertEquals(300.0, voters.at(instant = 5).single().visibilityM)
    }

    @Test
    fun `a model the provider does not serve at the place is no witness`() {
        val r = response(columns = mapOf<String, List<Number?>>("visibility_ecmwf_ifs" to List(hours) { 300.0 }))
        assertTrue(Witnesses.of(r, best()).at(instant = 5).isEmpty())
    }

    /** A witness's cloud, the total and the three layers, the same every hour. */
    private fun cloud(model: String, cover: Int, low: Int?, mid: Int, high: Int): Map<String, List<Number?>> = mapOf(
        "cloud_cover_$model" to List(hours) { cover },
        "cloud_cover_low_$model" to List(hours) { low },
        "cloud_cover_mid_$model" to List(hours) { mid },
        "cloud_cover_high_$model" to List(hours) { high }
    )

    @Test
    fun `the sky's witnesses are the two global ones, each with its four numbers`() {
        // Fase 18.3: IFS and UKMO lend their cloud; KNMI is fog's witness only.
        val r = response(
            columns = witness("ecmwf_ifs", { 7.0 }) + cloud("ecmwf_ifs", 20, 10, 5, 0) +
                witness(ukmo, { 12.0 }) + cloud(ukmo, 90, 80, 30, 10) +
                witness(knmi, { 9.0 }) + cloud(knmi, 100, 100, 100, 100)
        )
        val sky = Witnesses.of(r, best()).skyAt(10)
        assertEquals(
            listOf(ModelMean.Sky(20, 10, 5, 0), ModelMean.Sky(90, 80, 30, 10)),
            sky
        )
    }

    @Test
    fun `a witness without a layer, or one that is best_match itself, lends no sky`() {
        val r = response(
            columns = witness("ecmwf_ifs", { 10.0 + it * 0.1 }) + cloud("ecmwf_ifs", 50, 40, 10, 0) +
                witness(ukmo, { 12.0 }) + cloud(ukmo, 90, null, 30, 10)
        )
        assertTrue(Witnesses.of(r, best()).skyAt(10).isEmpty())
    }

    @Test
    fun `a witness from before the layers were asked for lends no sky, and still votes on fog`() {
        // A cache entry written before 22 nov 2026: the four fog columns and nothing else.
        val voters = Witnesses.of(response(columns = witness("ecmwf_ifs", { 7.0 })), best())
        assertTrue(voters.skyAt(10).isEmpty())
        assertEquals(1, voters.at(instant = 10).size)
    }
}
