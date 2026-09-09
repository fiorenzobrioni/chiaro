package com.callbackdev.chiaro.data.warnings

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The discovery chain against the files GitHub really served on 9 set 2026 (the
 * commits Atom, the first commit's diff — both fixtures under `src/test/resources/dpc`)
 * and a zip built around the real CAP. The transport is an OkHttp interceptor, so
 * every request the source makes is on the record: what it asks for is the test.
 */
@RunWith(RobolectricTestRunner::class)
class DpcBulletinSourceTest {

    private val transport = FakeTransport()
    private val source = DpcBulletinSource(OkHttpClient.Builder().addInterceptor(transport).build())

    private val firstSha = "da0f9c6ad75b8e463e5bbdb9b9977b9ed9945012"
    private val secondSha = "e01eb4c84ef7bcdd91244b61ecd268a44dd84de1"
    private val etag = "\"13c215c8f17a39f410e48444632c4836\""

    private fun resource(name: String): ByteArray =
        checkNotNull(javaClass.getResourceAsStream("/dpc/$name")) { "fixture missing: $name" }.use { it.readBytes() }

    private fun zip(vararg entries: Pair<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            entries.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    private fun serveTheNinth() {
        transport.route("/commits/master.atom") { request ->
            if (request.header("If-None-Match") == etag) {
                transport.respond(request, 304, ByteArray(0))
            } else {
                transport.respond(request, 200, resource("commits_20260909.atom"), mapOf("ETag" to etag))
            }
        }
        transport.route("/commit/$firstSha.diff") { transport.respond(it, 200, resource("commit_da0f9c6a.diff")) }
        transport.route("/xml/20260909_1546.zip") {
            transport.respond(it, 200, zip("Cap_20260909_1546.xml" to resource("Cap_20260908_1519.xml")))
        }
    }

    private fun fetch(known: WarningFetchState = WarningFetchState(), large: Boolean = false) =
        runBlocking { source.fetch(known, allowLargeDownload = large) }

    /** The paths asked for, after the repository name: the site's and the raw host's. */
    private fun asked() = transport.requests.map { it.url.encodedPath.substringAfter(DpcBulletinSource.REPO) }

    @Test
    fun `a new bulletin - the feed, one diff line, the small zip, and nothing else`() {
        serveTheNinth()
        val result = fetch() as WarningFetchResult.Fresh
        assertEquals("20260909_1546", result.stamp)
        assertEquals(etag, result.feedTag)
        assertEquals("DPC_BULLETIN_2026_09_08_6471", result.bulletin.id)
        assertEquals(209, result.bulletin.warnings.size)
        assertEquals(
            listOf("/commits/master.atom", "/commit/$firstSha.diff", "/master/files/xml/20260909_1546.zip"),
            asked()
        )
    }

    @Test
    fun `the feed's tag makes a re-check free - one request, no body`() {
        serveTheNinth()
        val result = fetch(WarningFetchState(stamp = "20260909_1546", feedTag = etag))
        assertEquals(WarningFetchResult.Unchanged(etag), result)
        assertEquals(1, transport.requests.size)
    }

    @Test
    fun `the stamp already in hand stops before the zip`() {
        serveTheNinth()
        val result = fetch(WarningFetchState(stamp = "20260909_1546"))
        assertEquals(WarningFetchResult.Unchanged(etag), result)
        assertEquals(listOf("/commits/master.atom", "/commit/$firstSha.diff"), asked())
    }

    @Test
    fun `a zip the pipeline has not uploaded yet keeps the old bulletin and forgets the tag`() {
        serveTheNinth()
        transport.route("/xml/20260909_1546.zip") { transport.respond(it, 404, ByteArray(0)) }
        assertEquals(WarningFetchResult.Unchanged(feedTag = null), fetch())
    }

    @Test
    fun `only the first line of a diff is read - a mid-pipeline commit weighs megabytes`() {
        serveTheNinth()
        val huge = ("diff --git a/files/geojson/20260909_1546_today.json b/files/geojson/20260909_1546_today.json\n" +
            "x".repeat(5 * 1024 * 1024)).toByteArray()
        transport.route("/commit/$firstSha.diff") { transport.respond(it, 200, huge) }
        val result = fetch() as WarningFetchResult.Fresh
        assertEquals("20260909_1546", result.stamp)
    }

    @Test
    fun `a commit without a stamp on its first line is skipped for the next one`() {
        serveTheNinth()
        transport.route("/commit/$firstSha.diff") {
            transport.respond(it, 200, "diff --git a/README.md b/README.md\n+hello\n".toByteArray())
        }
        transport.route("/commit/$secondSha.diff") { transport.respond(it, 200, resource("commit_da0f9c6a.diff")) }
        val result = fetch() as WarningFetchResult.Fresh
        assertEquals("20260909_1546", result.stamp)
        assertTrue(asked().contains("/commit/$secondSha.diff"))
    }

    @Test
    fun `no network is a failure that says so`() {
        transport.offline = true
        val result = fetch() as WarningFetchResult.Failed
        assertEquals(WarningFetchFailure.OFFLINE, result.reason)
    }

    @Test
    fun `when discovery fails the big archive is taken only on an unmetered network`() {
        transport.route("/commits/master.atom") { transport.respond(it, 500, ByteArray(0)) }
        transport.route("/all/latest_all.zip") {
            transport.respond(
                it, 200,
                zip("README.txt" to "readme".toByteArray(), "Cap_20260909_1546.xml" to resource("Cap_20260908_1519.xml"))
            )
        }
        val metered = fetch(large = false) as WarningFetchResult.Failed
        assertEquals(WarningFetchFailure.SERVICE, metered.reason)
        assertTrue(asked().none { it.endsWith("latest_all.zip") })

        val unmetered = fetch(large = true) as WarningFetchResult.Fresh
        assertEquals("20260909_1546", unmetered.stamp)
        assertEquals(null, unmetered.feedTag)
        assertTrue(asked().any { it.endsWith("latest_all.zip") })
    }

    @Test
    fun `a zip without a readable CAP is malformed, not a bulletin`() {
        serveTheNinth()
        transport.route("/xml/20260909_1546.zip") {
            transport.respond(it, 200, zip("Cap_20260909_1546.xml" to "<alert><identifier/></alert>".toByteArray()))
        }
        val result = fetch() as WarningFetchResult.Failed
        assertEquals(WarningFetchFailure.MALFORMED, result.reason)
    }

    /** Canned responses by URL suffix; every request is kept for the assertions. */
    private class FakeTransport : Interceptor {
        private val routes = linkedMapOf<String, (Request) -> Response>()
        val requests = mutableListOf<Request>()
        var offline = false

        fun route(suffix: String, handler: (Request) -> Response) {
            routes[suffix] = handler
        }

        fun respond(request: Request, code: Int, body: ByteArray, headers: Map<String, String> = emptyMap()) =
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message(if (code == 200) "OK" else "")
                .body(body.toResponseBody(null))
                .apply { headers.forEach { (name, value) -> header(name, value) } }
                .build()

        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            requests += request
            if (offline) throw IOException("offline")
            val handler = routes.entries.firstOrNull { request.url.toString().endsWith(it.key) }?.value
            return handler?.invoke(request) ?: respond(request, 404, ByteArray(0))
        }
    }
}
