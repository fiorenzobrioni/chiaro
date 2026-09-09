package com.callbackdev.chiaro.notifications

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.domain.sky.SkyJobCatalog
import com.callbackdev.chiaro.domain.sky.SkyVerdict
import com.callbackdev.chiaro.domain.sky.SkyVerdictKind
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The reminder's two bodies (Fase 6b): collapsed, the lead and the verdict share one
 * line; expanded, they take a line each and the catalog's own sentence on what the
 * moment IS comes last. The structure is tweather's `SkyNotifierTest` (ported 9 set
 * 2026, `UPSTREAM.md`); the words are this app's, and so is the last test — the
 * dotted job ids are code and never reach a screen here, a notification included.
 *
 * The clock is asserted by shape, not by value: `SkyNotifier` follows the device's
 * 12/24-hour setting, which is the reader's and not the test's to fix.
 */
@RunWith(RobolectricTestRunner::class)
class SkyNotifierTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val manager = context.getSystemService(NotificationManager::class.java)
    private val zone: ZoneId = ZoneId.of("Europe/Rome")
    private val now: Instant = Instant.parse("2026-09-06T16:51:00Z")
    private val at: Instant = now.plusSeconds(30 * 60)

    private fun post(jobId: String = SkyJobCatalog.GoldenPm.id) = SkyNotifier.notify(
        context,
        jobId = jobId,
        occurrenceAt = at,
        zone = zone,
        verdict = SkyVerdict(SkyVerdictKind.PASS, cloudPct = 8),
        now = now
    )

    private fun extras() = shadowOf(manager).allNotifications.single().extras

    private fun title() = extras().getString(Notification.EXTRA_TITLE)
    private fun collapsed() = extras().getString(Notification.EXTRA_TEXT).orEmpty()
    private fun expanded() = extras().getCharSequence(Notification.EXTRA_BIG_TEXT).toString()

    @Test
    fun `collapsed is the lead and the verdict with its number on one line`() {
        assertTrue(post())
        val text = collapsed()
        assertTrue(text, text.startsWith("In 30 minutes, at "))
        assertTrue(text, text.endsWith(" · Great, cloud 8%"))
        assertEquals("Golden hour, evening", title())
    }

    @Test
    fun `expanded gives each fact a line and ends with what the moment is`() {
        post()
        val lines = expanded().lines()
        assertEquals(3, lines.size)
        assertTrue(lines[0], lines[0].startsWith("In 30 minutes, at "))
        assertEquals("Great, cloud 8%", lines[1])
        assertEquals("The last light before sunset, warm and low.", lines[2])
        // The two bodies are the same words: the collapsed line is the first two
        // stacked lines joined, so they cannot drift.
        assertEquals(lines[0] + " · " + lines[1], collapsed())
    }

    @Test
    @Config(qualifiers = "it")
    fun `everything localizes, the verdict word and its evidence included`() {
        post()
        assertEquals("Ora dorata, sera", title())
        val text = collapsed()
        assertTrue(text, text.startsWith("Tra 30 minuti, alle "))
        assertTrue(text, text.endsWith(" · Bello, nuvole 8%"))
        assertTrue(expanded(), expanded().endsWith("L'ultima luce prima del tramonto, calda e radente."))
    }

    /** No jargon (CLAUDE.md): the id stays in the code, and so does upstream's `man`. */
    @Test
    fun `the dotted job id never reaches the notification`() {
        post()
        val everything = listOf(title(), collapsed(), expanded()).joinToString("\n")
        assertFalse(everything, everything.contains(SkyJobCatalog.GoldenPm.id))
        assertFalse(everything, everything.contains("$ man"))
    }
}
