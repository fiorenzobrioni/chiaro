package com.callbackdev.chiaro.data.local

import com.callbackdev.chiaro.data.remote.OpenMeteoForecastApi
import com.callbackdev.chiaro.data.remote.dto.ForecastResponseDto
import com.callbackdev.chiaro.data.remote.dto.WitnessResponseDto
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ReportDiskCacheTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val json = Json { ignoreUnknownKeys = true }

    private val forecast: ForecastResponseDto = json.decodeFromString(
        ForecastResponseDto.serializer(),
        javaClass.getResource("/openmeteo/milan-full-2026-09-24.json")!!.readText()
    )

    /**
     * 25 set 2026, Longyearbyen after an update: the entry an older app had written a
     * few minutes before was read back as fresh, without the `rain_sum` the new one asks
     * for, and a day of 3.8 mm of rain showed none.
     */
    @Test
    fun `an entry remembers what it was asked for, and an older one is not the current request`() = runBlocking {
        val cache = ReportDiskCache(tmp.root, json)
        cache.write("78.22:15.63", ReportDiskCache.Entry(0L, 100L, forecast, null, ReportDiskCache.CURRENT_REQUEST))
        val current = cache.read("78.22:15.63")!!
        assertEquals(ReportDiskCache.CURRENT_REQUEST, current.request)
        assertTrue(current.isCurrentRequest)
        // 27 ott 2026: the witnesses' request is part of it, so an entry fetched with the
        // forecast's request alone — three witness models, GFS among them — is not a hit.
        assertFalse(current.copy(request = OpenMeteoForecastApi.REQUEST).isCurrentRequest)
        assertTrue(ReportDiskCache.CURRENT_REQUEST.contains("knmi_harmonie_arome_europe"))

        // Written before the field existed: the JSON has no `request` at all.
        tmp.root.resolve("45.46_9.19.json").writeText(
            json.encodeToString(ReportDiskCache.Entry.serializer(), current).replace(Regex(""","request":"[^"]*""""), "")
        )
        val older = cache.read("45.46:9.19")!!
        assertEquals("", older.request)
        assertFalse(older.isCurrentRequest)
    }

    /**
     * 26 ott 2026: the witnesses travel with the forecast, so that a report re-mapped from
     * disk votes as the one first mapped from the network did; an entry from before has none.
     */
    @Test
    fun `the witnesses are kept beside the forecast, and an older entry has none`() = runBlocking {
        val cache = ReportDiskCache(tmp.root, json)
        val witnesses = json.decodeFromString(
            WitnessResponseDto.serializer(),
            """{"hourly":{"time":["2026-10-26T09:00"],"visibility_ecmwf_ifs":[14160.0]}}"""
        )
        cache.write("45.61:9.24", ReportDiskCache.Entry(0L, 100L, forecast, null, OpenMeteoForecastApi.REQUEST, witnesses))
        val read = cache.read("45.61:9.24")!!
        assertEquals(listOf(14160.0), read.witnesses!!.series("visibility", "ecmwf_ifs"))

        tmp.root.resolve("45.46_9.19.json").writeText(
            json.encodeToString(ReportDiskCache.Entry.serializer(), read).replace(Regex(""","witnesses":\{.*\}\}"""), "}")
        )
        assertNull(cache.read("45.46:9.19")!!.witnesses)
    }
}
