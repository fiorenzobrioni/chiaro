package com.callbackdev.chiaro.data.warnings

import com.callbackdev.chiaro.domain.warnings.DpcBulletinReader
import java.io.ByteArrayInputStream
import java.io.IOException
import java.time.ZoneId
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

/**
 * The Dipartimento della Protezione Civile's criticality bulletin, from its GitHub
 * repository (CC BY 4.0). Everything here was measured live on 8–9 set 2026 and is
 * recorded in PLANNING (Fase 11, «Le misure che hanno deciso» and the second PR).
 *
 * **Discovery goes through the site, never the API**: the REST API allows sixty
 * requests an hour per IP and mobile operators share one IP among thousands of
 * phones. The commits Atom (18 KB, or a 304 with no body when it carries the ETag
 * of the last look) names the newest commits; the `.diff` of one names, on its
 * first line, a file whose name carries the bulletin's stamp
 * (`files/preview/20260909_1546_domani.png`). Only that first line is read and the
 * connection dropped: the pipeline publishes a bulletin in a dozen commits and the
 * diff of a mid-pipeline one (a GeoJSON) weighs 5.5 MB.
 *
 * With the stamp, `files/xml/<stamp>.zip` (7 KB) holds `Cap_<stamp>.xml`. A 404
 * there means the pipeline has not reached that file yet: nothing is kept and the
 * next run asks again. `files/all/latest_all.zip` (4.7 MB, the shapefile and a PDF
 * inside next to the same CAP) is the fallback when discovery itself fails, and only
 * on an unmetered network.
 */
class DpcBulletinSource(
    private val client: OkHttpClient,
    private val siteBase: String = SITE_BASE,
    private val rawBase: String = RAW_BASE
) : WarningSource {

    override val id: String = ID

    override val zone: ZoneId = ZoneId.of("Europe/Rome")

    override suspend fun fetch(
        known: WarningFetchState,
        allowLargeDownload: Boolean
    ): WarningFetchResult = withContext(Dispatchers.IO) {
        try {
            when (val found = discover(known.feedTag)) {
                is Discovery.NotModified -> WarningFetchResult.Unchanged(known.feedTag)
                is Discovery.Failed ->
                    if (allowLargeDownload) fallback() else WarningFetchResult.Failed(found.reason, found.cause)
                is Discovery.Stamp ->
                    if (found.stamp == known.stamp) {
                        WarningFetchResult.Unchanged(found.feedTag)
                    } else {
                        download(found.stamp, found.feedTag)
                    }
            }
        } catch (e: IOException) {
            WarningFetchResult.Failed(WarningFetchFailure.OFFLINE, e)
        }
    }

    private sealed interface Discovery {
        object NotModified : Discovery
        data class Stamp(val stamp: String, val feedTag: String?) : Discovery
        data class Failed(val reason: WarningFetchFailure, val cause: Throwable? = null) : Discovery
    }

    /** The newest bulletin's stamp, from the feed and the first line of a diff. */
    private fun discover(feedTag: String?): Discovery {
        val feed = get("$siteBase/commits/master.atom", ifNoneMatch = feedTag)
        feed.use { response ->
            if (response.code == 304) return Discovery.NotModified
            if (!response.isSuccessful) return Discovery.Failed(WarningFetchFailure.SERVICE)
            val tag = response.header("ETag")
            val shas = CommitId.findAll(response.body?.string().orEmpty())
                .map { it.groupValues[1] }
                .take(COMMITS_TO_TRY)
                .toList()
            if (shas.isEmpty()) return Discovery.Failed(WarningFetchFailure.MALFORMED)
            for (sha in shas) {
                val stamp = stampOf(sha) ?: continue
                return Discovery.Stamp(stamp, tag)
            }
            return Discovery.Failed(WarningFetchFailure.MALFORMED)
        }
    }

    /** The stamp on the first line of the commit's diff, reading no more than that. */
    private fun stampOf(sha: String): String? =
        get("$siteBase/commit/$sha.diff").use { response ->
            if (!response.isSuccessful) return null
            val firstLine = response.body?.source()?.readUtf8Line() ?: return null
            Stamp.find(firstLine)?.value
        }

    private fun download(stamp: String, feedTag: String?): WarningFetchResult =
        get("$rawBase/xml/$stamp.zip").use { response ->
            when {
                response.code == 404 -> WarningFetchResult.Unchanged(feedTag = null)
                !response.isSuccessful -> WarningFetchResult.Failed(WarningFetchFailure.SERVICE)
                else -> readCapZip(response, stamp, feedTag)
            }
        }

    private fun fallback(): WarningFetchResult =
        get("$rawBase/all/latest_all.zip").use { response ->
            if (!response.isSuccessful) return WarningFetchResult.Failed(WarningFetchFailure.SERVICE)
            readCapZip(response, stamp = null, feedTag = null)
        }

    /** The `Cap_<stamp>.xml` inside the zip, read into a bulletin. */
    private fun readCapZip(response: Response, stamp: String?, feedTag: String?): WarningFetchResult {
        val body = response.body ?: return WarningFetchResult.Failed(WarningFetchFailure.MALFORMED)
        ZipInputStream(body.byteStream()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = entry.name.substringAfterLast('/')
                if (!name.startsWith("Cap_") || !name.endsWith(".xml")) continue
                val bytes = zip.readBytes()
                val stampInName = Stamp.find(name)?.value
                return try {
                    val cap = CapParser.parse(ByteArrayInputStream(bytes))
                    WarningFetchResult.Fresh(
                        bulletin = DpcBulletinReader.toBulletin(cap),
                        stamp = stamp ?: stampInName ?: cap.identifier,
                        feedTag = feedTag
                    )
                } catch (e: IOException) {
                    throw e
                } catch (e: Exception) {
                    WarningFetchResult.Failed(WarningFetchFailure.MALFORMED, e)
                }
            }
        }
        return WarningFetchResult.Failed(WarningFetchFailure.MALFORMED)
    }

    private fun get(url: String, ifNoneMatch: String? = null): Response {
        val request = Request.Builder().url(url).apply {
            if (ifNoneMatch != null) header("If-None-Match", ifNoneMatch)
        }.build()
        return client.newCall(request).execute()
    }

    companion object {
        const val ID = "dpc"
        const val REPO = "pcm-dpc/DPC-Bollettini-Criticita-Idrogeologica-Idraulica"
        const val SITE_BASE = "https://github.com/$REPO"
        const val RAW_BASE = "https://raw.githubusercontent.com/$REPO/master/files"

        /** A bulletin publishes as ~12 commits; the newest few are enough to find a stamp. */
        private const val COMMITS_TO_TRY = 3

        private val CommitId = Regex("Grit::Commit/([0-9a-f]{40})")
        private val Stamp = Regex("\\d{8}_\\d{4}")

        /** The bulletin's stamp inside a file name or a diff line, `AAAAMMGG_HHMM`. */
        fun stampIn(text: String): String? = Stamp.find(text)?.value
    }
}
