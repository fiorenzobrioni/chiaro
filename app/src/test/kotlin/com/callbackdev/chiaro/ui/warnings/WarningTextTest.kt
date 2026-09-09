package com.callbackdev.chiaro.ui.warnings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.domain.settings.UnitSettings
import com.callbackdev.chiaro.domain.warnings.PlaceWarnings
import com.callbackdev.chiaro.domain.warnings.WarningHazard
import com.callbackdev.chiaro.domain.warnings.WarningHazard.HYDRAULIC
import com.callbackdev.chiaro.domain.warnings.WarningHazard.HYDROGEOLOGICAL
import com.callbackdev.chiaro.domain.warnings.WarningHazard.THUNDERSTORM
import com.callbackdev.chiaro.domain.warnings.WarningLevel
import com.callbackdev.chiaro.domain.warnings.WarningLevel.NONE
import com.callbackdev.chiaro.domain.warnings.WarningLevel.ORANGE
import com.callbackdev.chiaro.domain.warnings.WarningLevel.RED
import com.callbackdev.chiaro.domain.warnings.WarningLevel.YELLOW
import com.callbackdev.chiaro.domain.warnings.WarningZone
import com.callbackdev.chiaro.ui.today.Headline
import com.callbackdev.chiaro.ui.today.HeadlineText
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The one vocabulary every surface says a warning with (DESIGN §8.13). Two claims are
 * worth a test each: the banner's sentence groups by level in the issuer's order, and
 * **nothing in this package ever prints a zone code** — which is the same rule the sky
 * ids live under, and the only rule the thirteen unnamed zones can break.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "it")
class WarningTextTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val today: LocalDate = LocalDate.of(2026, 9, 9)
    private val milano = WarningZone("Lomb-09", "Nodo Idraulico di Milano", "Lombardia")

    /** One of the thirteen the Region never named: the file repeats the code. */
    private val unnamed = WarningZone("Basi-A1", "Basi-A1", "Basilicata")

    private fun levels(
        hydraulic: WarningLevel = NONE,
        hydrogeological: WarningLevel = NONE,
        thunderstorm: WarningLevel = NONE
    ) = mapOf(HYDRAULIC to hydraulic, HYDROGEOLOGICAL to hydrogeological, THUNDERSTORM to thunderstorm)

    private fun warnings(
        zone: WarningZone = milano,
        todayLevels: Map<WarningHazard, WarningLevel> = levels(thunderstorm = ORANGE, hydrogeological = YELLOW),
        tomorrowLevels: Map<WarningHazard, WarningLevel> = levels()
    ) = PlaceWarnings(
        zone = zone,
        bulletinId = "DPC_BULLETIN_2026_09_09_6506",
        issuedAt = today.atTime(15, 46),
        days = listOf(
            PlaceWarnings.DayWarnings(today, todayLevels),
            PlaceWarnings.DayWarnings(today.plusDays(1), tomorrowLevels)
        ),
        note = null
    )

    @Test
    fun `the sentence groups by level, worst first, in the issuer's order`() {
        assertEquals(
            "Allerta arancione per temporali, gialla per rischio idrogeologico",
            WarningText.sentence(context, warnings())
        )
    }

    @Test
    fun `two hazards at one level share a clause`() {
        val both = warnings(
            todayLevels = levels(hydraulic = RED, thunderstorm = NONE, hydrogeological = RED)
        )
        assertEquals(
            "Allerta rossa per rischio idraulico e rischio idrogeologico",
            WarningText.sentence(context, both)
        )
    }

    /** The levels of every day, not only of the first: a quiet today under a red
     * tomorrow still says red. */
    @Test
    fun `the sentence reads across the days the bulletin covers`() {
        val tomorrowOnly = warnings(
            todayLevels = levels(),
            tomorrowLevels = levels(hydraulic = RED)
        )
        assertEquals("Allerta rossa per rischio idraulico", WarningText.sentence(context, tomorrowOnly))
    }

    @Test
    fun `the days a peak covers are said in words`() {
        assertEquals(
            "oggi fino a mezzanotte",
            WarningText.days(context, listOf(today), today)
        )
        assertEquals("domani", WarningText.days(context, listOf(today.plusDays(1)), today))
        assertEquals(
            "oggi e domani",
            WarningText.days(context, listOf(today, today.plusDays(1)), today)
        )
    }

    @Test
    fun `a named zone prints its name`() {
        assertEquals("Nodo Idraulico di Milano", WarningText.zoneLabel(context, milano))
    }

    /** The thirteen without a name print their region: a code is code (DESIGN §8.13). */
    @Test
    fun `an unnamed zone prints its region instead of its code`() {
        val printed = WarningText.zoneLabel(context, unnamed)
        assertEquals("una zona della regione Basilicata", printed)
        assertFalse(printed.contains(unnamed.code))
    }

    @Test
    fun `no zone code and no bulletin id reach any sentence this object makes`() {
        val lines = listOf(
            WarningText.sentence(context, warnings()),
            WarningText.sentence(context, warnings(zone = unnamed)),
            WarningText.zoneLabel(context, milano),
            WarningText.zoneLabel(context, unnamed),
            WarningText.days(context, listOf(today), today)
        )
        lines.forEach { line ->
            assertFalse(line, line.contains("Lomb-09"))
            assertFalse(line, line.contains("Basi-A1"))
            assertFalse(line, line.contains("DPC_BULLETIN"))
        }
    }

    // ------------------------------------------------------------ the headline

    private val timeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    private fun headline(brief: Boolean, today: Boolean) = HeadlineText.of(
        context,
        Headline.Official(ORANGE, listOf(THUNDERSTORM), today),
        timeFmt,
        UnitSettings(),
        brief = brief
    )

    @Test
    fun `the headline says the level, what for, and which day`() {
        assertEquals("Allerta arancione per temporali, oggi", headline(brief = false, today = true))
        assertEquals("Allerta arancione per temporali, domani", headline(brief = false, today = false))
    }

    /** The brief register is the widgets' three lines of fourteen characters: same
     * level, same hazard, no day (the card has no room and the app screen has it). */
    @Test
    fun `the brief register keeps the level and drops the day`() {
        assertEquals("Allerta arancione · temporali", headline(brief = true, today = true))
    }

    @Test
    fun `the headline never leaks an identifier either`() {
        val line = headline(brief = false, today = true)!!
        assertTrue(line.startsWith("Allerta"))
        assertFalse(line.contains("ORANGE"))
        assertFalse(line.contains("THUNDERSTORM"))
    }
}
