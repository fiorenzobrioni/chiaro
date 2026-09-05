package com.callbackdev.chiaro.ui.sky

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.sky.MeteorShowerTable
import com.callbackdev.chiaro.domain.sky.SkyJobCatalog
import com.callbackdev.chiaro.domain.sky.SkyJobKind
import com.callbackdev.chiaro.domain.sky.SkyJobShape
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The guide to the events. The catalog is a fixed list in the domain and the pages are
 * a map beside it in the UI, which is exactly the shape that goes quietly out of date:
 * the guard that matters is **totality**, asserted here rather than discovered by the
 * reader who taps the info button on the fifty-second event and gets a crash.
 */
@RunWith(RobolectricTestRunner::class)
class SkyGuideTest {

    private val resources: Resources
        get() = ApplicationProvider.getApplicationContext<Context>().resources

    @Test
    fun `every event in the catalog has a page`() {
        SkyJobCatalog.all.forEach { job ->
            val body = resources.getString(SkyGuide.pageRes(job.id))
            assertTrue("${job.id}'s page is empty", body.isNotBlank())
        }
    }

    /** And every event is in the index exactly once, or the guide is not a guide to
     * the catalog, it is a guide to most of it. */
    @Test
    fun `the index is the catalog, once each`() {
        val listed = SkyGuide.groups.flatMap { group -> group.jobs.map { it.id } }
        assertEquals("an event is grouped twice", listed.distinct(), listed)
        assertEquals(
            "the index and the catalog disagree",
            SkyJobCatalog.all.map { it.id }.toSet(),
            listed.toSet()
        )
    }

    /**
     * The placeholder guard: two paragraphs, a body with something in it, and no
     * paragraph that is a fragment. It deliberately does NOT ask each paragraph to be
     * long — several pages open with a one-sentence definition and explain underneath,
     * which is how this kind of writing is supposed to read, and a floor high enough to
     * forbid that would be a test with an opinion about prose rhythm.
     */
    @Test
    fun `every page is two real paragraphs and not a stub`() {
        SkyJobCatalog.all.forEach { job ->
            val body = resources.getString(SkyGuide.pageRes(job.id))
            val paragraphs = body.split("\n\n").filter { it.isNotBlank() }
            assertEquals("${job.id} is not two paragraphs", 2, paragraphs.size)
            assertTrue("${job.id}'s page is a stub: $body", body.length > 300)
            paragraphs.forEach { paragraph ->
                assertTrue(
                    "${job.id} has a fragment for a paragraph: $paragraph",
                    paragraph.trim().length > 50
                )
            }
        }
    }

    /** Everything on screen in this product is prose, so everything localizes: an
     * Italian page that is the English one is a page nobody translated. */
    @Test
    @Config(qualifiers = "it")
    fun `every page is actually translated`() {
        SkyJobCatalog.all.forEach { job ->
            val italian = resources.getString(SkyGuide.pageRes(job.id))
            assertTrue("${job.id} has no Italian page", italian.isNotBlank())
            assertNotEquals(
                "${job.id} reads the same in both languages",
                englishPage(job.id),
                italian
            )
        }
    }

    /**
     * The dot-notation identifiers stay in the code and never reach a screen
     * (CLAUDE.md, VISION §8) — a page that printed `golden_hour.pm` would be the fork
     * showing through, since these pages were ported from an edition that prints them
     * on purpose.
     */
    @Test
    fun `no page says a job id out loud`() {
        val ids = SkyJobCatalog.all.map { it.id }
        listOf(Locale.ENGLISH, Locale.ITALIAN).forEach { locale ->
            val res = localized(locale)
            SkyJobCatalog.all.forEach { job ->
                val body = res.getString(SkyGuide.pageRes(job.id))
                ids.forEach { id ->
                    assertFalse("${job.id}'s page prints $id in $locale", body.contains(id))
                }
            }
        }
    }

    /** A page that points nowhere is a dead end; one that points at itself is a bug. */
    @Test
    fun `see also only ever points at real pages, never at itself`() {
        SkyJobCatalog.all.forEach { job ->
            val targets = SkyGuide.seeAlso(job.id)
            assertEquals("${job.id} lists an event twice", targets.distinct(), targets)
            targets.forEach { target ->
                assertNotEquals("${job.id} points at itself", job.id, target)
                assertTrue(
                    "${job.id} points at $target, which is not in the catalog",
                    SkyJobCatalog.byId(target) != null
                )
            }
        }
    }

    /**
     * Symmetry, with the one exception the map documents: every shower points at full
     * darkness and it does not point back, because a page whose "see also" listed
     * thirteen showers is a page nobody finishes.
     */
    @Test
    fun `see also is mutual except for the showers' shared link`() {
        val showers = MeteorShowerTable.all.map { MeteorShowerTable.jobId(it) }.toSet()
        SkyJobCatalog.all.forEach { job ->
            SkyGuide.seeAlso(job.id).forEach { target ->
                val oneWay = job.id in showers && target == SkyJobCatalog.DarknessWindow.id
                if (!oneWay) {
                    assertTrue(
                        "${job.id} points at $target and $target does not point back",
                        job.id in SkyGuide.seeAlso(target)
                    )
                }
            }
        }
    }

    /** Every shower hands the reader to full darkness, since that is what decides it. */
    @Test
    fun `every shower points at the darkness it needs`() {
        MeteorShowerTable.all.forEach { shower ->
            val id = MeteorShowerTable.jobId(shower)
            assertTrue(
                "$id does not point at full darkness",
                SkyJobCatalog.DarknessWindow.id in SkyGuide.seeAlso(id)
            )
        }
    }

    /**
     * "When it happens" is read off the job and never written down twice — this is the
     * test that says so, by checking the sentences track the fields rather than a
     * hand-kept list. An event whose cadence changed and whose page still claimed the
     * old one is exactly the drift the generation exists to prevent.
     */
    @Test
    fun `when it happens is the job's own definition, said in words`() {
        val daily = resources.getString(R.string.sky_guide_when_daily)
        val annual = resources.getString(R.string.sky_guide_when_annual)
        val polling = resources.getString(R.string.sky_guide_when_polling)
        val range = resources.getString(R.string.sky_guide_when_range)
        val geometry = resources.getString(R.string.sky_guide_when_geometry)
        val darkness = resources.getString(R.string.sky_guide_when_darkness)

        SkyJobCatalog.all.forEach { job ->
            val lines = SkyGuide.whenLines(resources, job)
            val cadence = when (job.kind) {
                SkyJobKind.DAILY -> daily
                SkyJobKind.ANNUAL -> annual
                SkyJobKind.POLLING -> polling
            }
            assertEquals("${job.id} says the wrong cadence", cadence, lines.first())
            assertEquals(
                "${job.id} says the wrong shape",
                job.shape == SkyJobShape.RANGE,
                lines.contains(range)
            )
            assertEquals(
                "${job.id} is wrong about being a sight",
                !job.observable,
                lines.contains(geometry)
            )
            assertEquals(
                "${job.id} is wrong about needing a dark sky",
                job.needsDarkness,
                lines.contains(darkness)
            )
        }
    }

    /** A moment of geometry carries no verdict, so its page must never blame the
     * clouds: the screen and the page have to agree about what the app knows. */
    @Test
    fun `an event the clouds cannot spoil never says they can`() {
        val visibility = resources.getString(R.string.sky_guide_when_visibility)
        SkyJobCatalog.all.filter { !it.observable }.forEach { job ->
            assertFalse(
                "${job.id} is not observable but blames the clouds",
                SkyGuide.whenLines(resources, job).contains(visibility)
            )
        }
    }

    private fun englishPage(jobId: String): String =
        localized(Locale.ENGLISH).getString(SkyGuide.pageRes(jobId))

    private fun localized(locale: Locale): Resources {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config).resources
    }
}
