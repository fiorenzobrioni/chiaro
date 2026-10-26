package com.callbackdev.chiaro.data.local

import com.callbackdev.chiaro.data.remote.dto.AirQualityCurrentDto
import com.callbackdev.chiaro.data.remote.OpenMeteoForecastApi
import com.callbackdev.chiaro.data.remote.dto.ForecastResponseDto
import com.callbackdev.chiaro.data.remote.dto.WitnessResponseDto
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Last successful fetch per city, persisted as the raw API DTOs plus fetch
 * metadata. The repository's in-memory TTL cache dies with the process; this copy
 * survives it, so a cold start inside the TTL re-maps the disk entry instead of
 * re-spending the two HTTP GETs a worker (or a previous process) already paid for.
 * Not Room: the history table stores flattened diff snapshots, not enough to
 * rebuild a [com.callbackdev.chiaro.domain.model.WeatherReport]. Best-effort by
 * design — any I/O or parse failure just falls through to the network.
 *
 * Since Fase 17 it is also **what the app falls back to when a fetch fails**, and
 * that changed what "dead weight" means here: an entry past the TTL is no longer
 * useless, it is the only forecast an offline phone has. See [prune].
 */
class ReportDiskCache(private val dir: File, private val json: Json) {

    @Serializable
    data class Entry(
        val fetchedAtEpochMs: Long,
        val responseTimeMs: Long,
        val forecast: ForecastResponseDto,
        val airQuality: AirQualityCurrentDto?,
        /**
         * [OpenMeteoForecastApi.REQUEST] at the time of the fetch; empty for an entry
         * written before it was recorded. A different one is never a fresh hit — the
         * app now asks for more than the entry has — but it is still the forecast an
         * offline phone falls back on.
         */
        val request: String = "",
        /**
         * The witnesses fetched with [forecast] (26 ott 2026), null when there were none —
         * an entry from before, a request that failed. Kept beside the forecast because a
         * report re-mapped from here must say the same states as the one first mapped from
         * the network: without them the vote would change its mind on a cold start.
         */
        val witnesses: WitnessResponseDto? = null
    ) {
        val isCurrentRequest: Boolean get() = request == OpenMeteoForecastApi.REQUEST
    }

    suspend fun read(cacheKey: String): Entry? = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(dir, fileName(cacheKey))
            if (file.isFile) json.decodeFromString<Entry>(file.readText()) else null
        }.getOrNull()
    }

    suspend fun write(cacheKey: String, entry: Entry) {
        withContext(Dispatchers.IO) {
            runCatching {
                dir.mkdirs()
                File(dir, fileName(cacheKey)).writeText(json.encodeToString(entry))
                prune()
            }
        }
    }

    /**
     * The age cutoff, on every write.
     *
     * It used to be four hours — "twice the largest TTL, so it can never be read back
     * as a hit". True while a hit was the only thing an entry could ever be; since Fase
     * 17 an entry is also the document the app shows when it cannot refresh, and the
     * response holds a **week** of forecast, so it stays useful for as long as that
     * forecast still reaches the present ([com.callbackdev.chiaro.domain.WeatherRecency]
     * decides that, from the data rather than from a constant). Past the horizon it says
     * nothing about any hour still to come, and goes.
     *
     * There was a count cutoff here too, sixteen files, newest kept. Here it could not
     * tell a followed place from a GPS cell driven through yesterday, so a reader with
     * more than sixteen places lost the offline forecast of the least recently fetched
     * of them. The count moved to [forgetForeign], which knows which is which
     * (14 ott 2026).
     */
    private fun prune() {
        val cutoff = System.currentTimeMillis() - MAX_AGE_MS
        dir.listFiles()?.forEach { file ->
            if (file.lastModified() < cutoff) file.delete()
        }
    }

    /**
     * Files belonging to places the app no longer follows — the disk-cache half of
     * `StoredDataSweep`. Two rules, and neither ever touches a followed place:
     *
     * - past [cutoffEpochMs], a foreign file goes (the same age test [prune] applies,
     *   so an entry the offline fallback could still use is never the one taken away);
     * - inside it, at most [MAX_FOREIGN] foreign files stay, the newest: the GPS
     *   pseudo-city mints a cacheKey per ~1.1 km cell, and a week of commuting would
     *   otherwise leave a response behind for every place the reader has been.
     */
    suspend fun forgetForeign(liveKeys: Set<String>, cutoffEpochMs: Long) {
        withContext(Dispatchers.IO) {
            runCatching {
                val live = liveKeys.map(::fileName).toSet()
                dir.listFiles()
                    ?.filter { it.name !in live }
                    ?.sortedByDescending { it.lastModified() }
                    ?.forEachIndexed { index, file ->
                        if (index >= MAX_FOREIGN || file.lastModified() < cutoffEpochMs) file.delete()
                    }
            }
        }
    }

    /** cacheKey is `lat:lon` in hundredths — ':' is not filesystem-safe everywhere. */
    private fun fileName(cacheKey: String) = cacheKey.replace(':', '_') + ".json"

    companion object {
        /**
         * The forecast horizon: past it the response says nothing about any hour that
         * has not already happened, so the file is finally dead weight for real.
         */
        private const val MAX_AGE_MS = OpenMeteoForecastApi.FORECAST_DAYS * 24 * 60 * 60 * 1000L

        /**
         * Foreign responses kept: a handful of GPS cells, enough for a place passed
         * through this morning to come back without a fetch. An evicted one costs a
         * fetch when there is a network, and nothing a reader follows.
         */
        const val MAX_FOREIGN = 8
    }
}
