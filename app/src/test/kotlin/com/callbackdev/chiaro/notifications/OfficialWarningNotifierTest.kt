package com.callbackdev.chiaro.notifications

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.model.GpsCityId
import com.callbackdev.chiaro.domain.warnings.PlaceWarnings
import com.callbackdev.chiaro.domain.warnings.WarningHazard
import com.callbackdev.chiaro.domain.warnings.WarningHazard.HYDRAULIC
import com.callbackdev.chiaro.domain.warnings.WarningHazard.HYDROGEOLOGICAL
import com.callbackdev.chiaro.domain.warnings.WarningHazard.THUNDERSTORM
import com.callbackdev.chiaro.domain.warnings.WarningLevel
import com.callbackdev.chiaro.domain.warnings.WarningLevel.NONE
import com.callbackdev.chiaro.domain.warnings.WarningLevel.ORANGE
import com.callbackdev.chiaro.domain.warnings.WarningLevel.YELLOW
import com.callbackdev.chiaro.domain.warnings.WarningNotification
import com.callbackdev.chiaro.domain.warnings.WarningZone
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The warning's two bodies, on the model of [SkyNotifierTest]: collapsed, one
 * sentence; expanded, the same sentence first, then one fact per line, the source
 * last. The clock is asserted by shape — the notifier follows the device's 12/24-hour
 * setting. The last test is the no-jargon rule: zone codes and bulletin ids are code.
 */
@RunWith(RobolectricTestRunner::class)
class OfficialWarningNotifierTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val manager = context.getSystemService(NotificationManager::class.java)
    private val today: LocalDate = LocalDate.of(2026, 9, 9)
    private val milano = City(
        3_173_435, "Milano", "Lombardia", "Italia", Coordinates(45.4643, 9.1895), "Europe/Rome",
        countryCode = "IT", admin3 = "Comune di Milano"
    )
    private val zone = WarningZone("Lomb-09", "Nodo Idraulico di Milano", "Lombardia")

    private fun levels(
        hydraulic: WarningLevel = NONE,
        hydrogeological: WarningLevel = NONE,
        thunderstorm: WarningLevel = NONE
    ) = mapOf(HYDRAULIC to hydraulic, HYDROGEOLOGICAL to hydrogeological, THUNDERSTORM to thunderstorm)

    private fun warnings(
        todayLevels: Map<WarningHazard, WarningLevel> = levels(thunderstorm = ORANGE, hydrogeological = YELLOW),
        tomorrowLevels: Map<WarningHazard, WarningLevel> = levels(thunderstorm = YELLOW),
        note: String? = null
    ) = PlaceWarnings(
        zone = zone,
        bulletinId = "DPC_BULLETIN_2026_09_09_6506",
        issuedAt = today.atTime(15, 46),
        days = listOf(
            PlaceWarnings.DayWarnings(today, todayLevels),
            PlaceWarnings.DayWarnings(today.plusDays(1), tomorrowLevels)
        ),
        note = note
    )

    private fun post(warnings: PlaceWarnings = warnings(), city: City = milano): Boolean =
        OfficialWarningNotifier.notify(
            context, WarningNotification("3173435:warn:DPC_BULLETIN_2026_09_09_6506:ORANGE", warnings), city, today
        )

    private fun posted() = shadowOf(manager).allNotifications.single()
    private fun extras() = posted().extras
    private fun title() = extras().getString(Notification.EXTRA_TITLE)
    private fun collapsed() = extras().getString(Notification.EXTRA_TEXT).orEmpty()
    private fun expanded() = extras().getCharSequence(Notification.EXTRA_BIG_TEXT).toString()

    @Test
    fun `collapsed is the hazards, the day and the bulletin's hour on one line`() {
        assertTrue(post())
        assertEquals("Orange warning · Milano", title())
        val text = collapsed()
        assertTrue(text, text.startsWith("Thunderstorms, today until midnight · bulletin of "))
        assertEquals(OfficialWarningNotifier.CHANNEL_HIGH, posted().channelId)
    }

    @Test
    fun `expanded gives each fact a line and ends with the source`() {
        post()
        val lines = expanded().lines()
        assertEquals(lines.joinToString("\n"), 7, lines.size)
        assertEquals(collapsed(), lines[0])
        assertEquals("", lines[1])
        assertEquals("Today: thunderstorms orange · hydrogeological yellow", lines[2])
        assertEquals("Tomorrow: thunderstorms yellow", lines[3])
        assertEquals("Warning zone: Nodo Idraulico di Milano", lines[4])
        assertEquals("What it means: Fenomeni diffusi o intensi e possibili danni.", lines[5])
        assertTrue(lines[6], lines[6].startsWith("Dipartimento della Protezione Civile · bulletin of "))
    }

    @Test
    fun `the bulletin's note is quoted when there is one for this zone`() {
        post(warnings(note = "Per la giornata di oggi: Regione Lombardia: si rimanda al bollettino regionale."))
        val lines = expanded().lines()
        assertEquals(8, lines.size)
        assertEquals("Bulletin note: Per la giornata di oggi: Regione Lombardia: si rimanda al bollettino regionale.", lines[6])
        assertTrue(lines[7].startsWith("Dipartimento della Protezione Civile"))
    }

    @Test
    fun `two hazards at the top level share the sentence, in the Dipartimento's order`() {
        post(warnings(todayLevels = levels(hydrogeological = ORANGE, thunderstorm = ORANGE)))
        assertTrue(collapsed(), collapsed().startsWith("Thunderstorms and hydrogeological risk, today until midnight · "))
    }

    @Test
    fun `a warning for tomorrow alone says tomorrow, for both days says both`() {
        post(warnings(todayLevels = levels(), tomorrowLevels = levels(hydraulic = ORANGE)))
        assertTrue(collapsed(), collapsed().startsWith("Hydraulic risk, tomorrow · "))
        manager.cancelAll()
        post(warnings(todayLevels = levels(hydraulic = ORANGE), tomorrowLevels = levels(hydraulic = ORANGE)))
        assertTrue(collapsed(), collapsed().startsWith("Hydraulic risk, today and tomorrow · "))
    }

    @Test
    @Config(qualifiers = "it")
    fun `everything localizes except the Dipartimento's own words`() {
        post()
        assertEquals("Allerta arancione · Milano", title())
        assertTrue(collapsed(), collapsed().startsWith("Temporali, oggi fino a mezzanotte · bollettino delle "))
        val lines = expanded().lines()
        assertEquals("Oggi: temporali arancione · idrogeologico giallo", lines[2])
        assertEquals("Domani: temporali giallo", lines[3])
        assertEquals("Zona di allerta: Nodo Idraulico di Milano", lines[4])
        assertEquals("Cosa vuol dire: Fenomeni diffusi o intensi e possibili danni.", lines[5])
        assertTrue(lines[6], lines[6].startsWith("Dipartimento della Protezione Civile · bollettino delle "))
    }

    @Test
    fun `yellow has its own channel, and green posts nothing`() {
        post(warnings(todayLevels = levels(hydraulic = YELLOW), tomorrowLevels = levels()))
        assertEquals(OfficialWarningNotifier.CHANNEL_YELLOW, posted().channelId)
        assertEquals("Yellow warning · Milano", title())
        manager.cancelAll()
        assertFalse(post(warnings(todayLevels = levels(), tomorrowLevels = levels())))
        assertEquals(0, shadowOf(manager).allNotifications.size)
    }

    @Test
    fun `one id per place, the position with its own`() {
        assertEquals(3435, OfficialWarningNotifier.notificationId(milano))
        assertEquals(3000, OfficialWarningNotifier.notificationId(milano.copy(id = GpsCityId)))
        post()
        assertTrue(shadowOf(manager).getNotification(3435) != null)
    }

    /** No jargon (CLAUDE.md): the zone code and the bulletin id stay in the code. */
    @Test
    fun `no zone code or bulletin identifier reaches the notification`() {
        post()
        val everything = listOf(title(), collapsed(), expanded()).joinToString("\n")
        assertFalse(everything, everything.contains("Lomb-09"))
        assertFalse(everything, everything.contains("DPC_BULLETIN"))
    }
}
