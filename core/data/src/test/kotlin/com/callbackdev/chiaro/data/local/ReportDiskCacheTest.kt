package com.callbackdev.chiaro.data.local

import com.callbackdev.chiaro.data.remote.OpenMeteoForecastApi
import com.callbackdev.chiaro.data.remote.dto.ForecastResponseDto
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        cache.write("78.22:15.63", ReportDiskCache.Entry(0L, 100L, forecast, null, OpenMeteoForecastApi.REQUEST))
        val current = cache.read("78.22:15.63")!!
        assertEquals(OpenMeteoForecastApi.REQUEST, current.request)
        assertTrue(current.isCurrentRequest)

        // Written before the field existed: the JSON has no `request` at all.
        tmp.root.resolve("45.46_9.19.json").writeText(
            json.encodeToString(ReportDiskCache.Entry.serializer(), current).replace(Regex(""","request":"[^"]*""""), "")
        )
        val older = cache.read("45.46:9.19")!!
        assertEquals("", older.request)
        assertFalse(older.isCurrentRequest)
    }
}
