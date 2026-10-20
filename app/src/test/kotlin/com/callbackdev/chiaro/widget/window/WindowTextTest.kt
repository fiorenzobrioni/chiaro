package com.callbackdev.chiaro.widget.window

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.data.AppSettings
import com.callbackdev.chiaro.domain.sky.Planet
import com.callbackdev.chiaro.domain.sky.SkyJobCatalog
import com.callbackdev.chiaro.domain.sky.SkyVerdict
import com.callbackdev.chiaro.domain.sky.SkyVerdictKind
import com.callbackdev.chiaro.ui.sky.NightHour
import com.callbackdev.chiaro.ui.sky.SkyText
import com.callbackdev.chiaro.ui.sky.Tonight
import com.callbackdev.chiaro.widget.NextMoment
import com.callbackdev.chiaro.widget.WidgetLook
import com.callbackdev.chiaro.widget.WidgetModel
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The window's words, in English, off the app's own strings. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rGB")
class WindowTextTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val zone: ZoneId = ZoneId.of("Europe/Rome")

    private fun model(tonight: Tonight? = null, moments: List<NextMoment> = emptyList()) = WidgetModel(
        settings = AppSettings(),
        look = WidgetLook(),
        city = null,
        fromGps = false,
        content = null,
        moments = moments,
        zone = zone,
        tonight = tonight
    )

    @Test
    fun `tonight is the verdict with the numbers that decided it`() {
        val tonight = Tonight(window = null, verdict = SkyVerdict(SkyVerdictKind.PASS, cloudPct = 9), moonIlluminationPct = 42)
        val word = context.getString(SkyText.verdictWordRes(SkyVerdictKind.PASS))
        assertEquals("$word tonight · cloud 9% · moon 42%", WindowText.tonight(context, model(tonight)))
        val noMoon = Tonight(window = null, verdict = SkyVerdict(SkyVerdictKind.UNSTABLE, cloudPct = 40))
        val soso = context.getString(SkyText.verdictWordRes(SkyVerdictKind.UNSTABLE))
        assertEquals("$soso tonight · cloud 40%", WindowText.tonight(context, model(noMoon)))
        // One moon number per card: with the moon named on the disc, the sentence leaves it out.
        assertEquals("$word tonight · cloud 9%", WindowText.tonight(context, model(tonight), withMoon = false))
    }

    @Test
    fun `with no dark window the moon is the fact`() {
        val at = Instant.parse("2026-09-25T20:00:00Z")
        val tonight = Tonight(
            window = null, verdict = null, moonIlluminationPct = 99,
            hours = listOf(NightHour(at, 50), NightHour(at.plusSeconds(3600), 70))
        )
        assertEquals("Moonlit all night · moon 99% · cloud 60%", WindowText.tonight(context, model(tonight)))
        assertEquals("Moonlit all night · moon 99%", WindowText.tonight(context, model(Tonight(window = null, verdict = null, moonIlluminationPct = 99))))
        assertEquals("Moonlit all night · cloud 60%", WindowText.tonight(context, model(tonight), withMoon = false))
        assertEquals("Moonlit all night", WindowText.tonight(context, model(Tonight(window = null, verdict = null, moonIlluminationPct = 99)), withMoon = false))
        assertNull(WindowText.tonight(context, model(null)))
        assertNull(WindowText.tonight(context, model(Tonight(window = null, verdict = null))))
    }

    @Test
    fun `the next moment, its clock, its verdict, and tomorrow when it is`() {
        val job = SkyJobCatalog.defaults.first()
        val at = Instant.parse("2026-09-25T10:15:00Z")
        val start = Instant.parse("2026-09-25T16:36:00Z") // 18:36 in Rome
        val name = context.getString(SkyText.shortNameRes(job.id))
        val moment = NextMoment(job, start, null, SkyVerdict(SkyVerdictKind.PASS, cloudPct = 19), inProgress = false)
        val line = WindowText.nextMoment(context, model(moments = listOf(moment)), at)
        val great = context.getString(SkyText.verdictWordRes(SkyVerdictKind.PASS))
        assertEquals("$name at 18:36 · $great", line)
        val tomorrow = NextMoment(job, Instant.parse("2026-09-26T05:14:00Z"), null, null, inProgress = false)
        assertEquals("$name tomorrow at 07:14", WindowText.nextMoment(context, model(moments = listOf(tomorrow)), at))
        assertNull(WindowText.nextMoment(context, model(), at))
    }

    @Test
    fun `where the window looks, and the names on the sky`() {
        assertEquals("Looking south", WindowText.looking(context, 180.0))
        assertEquals("Looking south-east", WindowText.looking(context, 135.0))
        assertEquals("Looking north", WindowText.looking(context, 359.0))
        assertEquals("Moon 99%", WindowText.moonLabel(context, 99))
        assertEquals("Venus", WindowText.planetLabel(context, Planet.VENUS))
        assertEquals("Jupiter", WindowText.planetLabel(context, Planet.JUPITER))
        assertEquals("Perseids, from here", WindowText.radiantLabel(context, "meteor.perseids.peak"))
        assertTrue(context.getString(R.string.star_sirius).isNotEmpty())
    }
}
