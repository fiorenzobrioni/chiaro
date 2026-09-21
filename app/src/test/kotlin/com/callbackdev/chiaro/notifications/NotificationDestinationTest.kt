package com.callbackdev.chiaro.notifications

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.domain.Alert
import com.callbackdev.chiaro.domain.AlertKind
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.model.WeatherCondition
import com.callbackdev.chiaro.domain.rules.NotificationRule
import com.callbackdev.chiaro.domain.rules.RuleCondition
import com.callbackdev.chiaro.domain.rules.RuleOp
import com.callbackdev.chiaro.domain.rules.RuleTrigger
import com.callbackdev.chiaro.domain.sample.sampleWeatherReport
import com.callbackdev.chiaro.domain.settings.UnitSettings
import com.callbackdev.chiaro.domain.sky.SkyJobCatalog
import com.callbackdev.chiaro.domain.sky.SkyVerdict
import com.callbackdev.chiaro.domain.sky.SkyVerdictKind
import com.callbackdev.chiaro.domain.warnings.PlaceWarnings
import com.callbackdev.chiaro.domain.warnings.WarningHazard
import com.callbackdev.chiaro.domain.warnings.WarningLevel
import com.callbackdev.chiaro.domain.warnings.WarningNotification
import com.callbackdev.chiaro.domain.warnings.WarningZone
import com.callbackdev.chiaro.ui.shell.ShellDestination
import com.callbackdev.chiaro.ui.shell.ShellTab
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * Where a tapped notification lands (21 set 2026, committente), and the one thing that
 * can silently take it somewhere else.
 *
 * The rule is the widgets': **the screen the notification was about**. A sky reminder
 * is Cielo's own row, bell and verdict; a fired rule and an official warning are cards
 * on Avvisi, which is also where the warning's arithmetic opens from; the four built-in
 * alerts are the weather itself — its hours, its rain, its sentence — and that is Oggi,
 * not the screen carrying the switch that sent them.
 *
 * All four are posted into ONE manager on purpose, and the destinations are read back
 * afterwards rather than one at a time. Every door into this app is now the same intent
 * ([ShellDestination]) and `PendingIntent` keys its cache on `filterEquals`, which
 * cannot see the extra the destination rides in — so the request code is the only thing
 * telling these four apart, and with `FLAG_UPDATE_CURRENT` a shared one would have the
 * last notifier rewrite an earlier one's destination under a notification already on
 * the shade. Reading them back after all four exist is what catches that; asserting
 * each in its own test would not.
 */
@RunWith(RobolectricTestRunner::class)
class NotificationDestinationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val manager = context.getSystemService(NotificationManager::class.java)
    private val zone: ZoneId = ZoneId.of("Europe/Rome")
    private val today: LocalDate = LocalDate.of(2026, 9, 21)
    private val now: Instant = Instant.parse("2026-09-21T16:51:00Z")

    private val milano = City(
        3_173_435, "Milano", "Lombardia", "Italia", Coordinates(45.4643, 9.1895), "Europe/Rome",
        countryCode = "IT", admin3 = "Comune di Milano"
    )

    private fun postSkyReminder() = SkyNotifier.notify(
        context,
        jobId = SkyJobCatalog.GoldenPm.id,
        occurrenceAt = now.plusSeconds(1800),
        zone = zone,
        verdict = SkyVerdict(SkyVerdictKind.PASS, cloudPct = 8),
        now = now
    )

    private fun postAlert() = AlertNotifier.notify(
        context,
        Alert(
            kind = AlertKind.PRECIPITATION,
            fingerprint = today.toString(),
            cityLabel = "Milano",
            condition = WeatherCondition(61, "Rain", "🌧"),
            at = today.atTime(17, 0),
            precipPct = 80
        ),
        sampleWeatherReport(),
        UnitSettings()
    )

    private fun postRule() = RuleNotifier.notify(
        context,
        RuleTrigger(
            rule = NotificationRule(
                id = 7,
                name = "Bici",
                conditions = listOf(RuleCondition("current.temp_c", RuleOp.GT, 15.0)),
                message = "Si va"
            ),
            fingerprint = null,
            latchKey = null,
            value = 18.0,
            at = null
        ),
        cityLabel = "Milano",
        report = sampleWeatherReport(),
        now = today.atTime(9, 0),
        units = UnitSettings()
    )

    private fun postWarning() = OfficialWarningNotifier.notify(
        context,
        WarningNotification(
            "3173435:warn:DPC_BULLETIN_2026_09_21_1:ORANGE",
            PlaceWarnings(
                zone = WarningZone("Lomb-09", "Nodo Idraulico di Milano", "Lombardia"),
                bulletinId = "DPC_BULLETIN_2026_09_21_1",
                issuedAt = today.atTime(15, 46),
                days = listOf(
                    // A day carries a level for EVERY hazard, green included: the model
                    // refuses a map with a hole in it, because a missing hazard would
                    // print as an empty cell where "nessuna" is the answer.
                    PlaceWarnings.DayWarnings(
                        today,
                        mapOf(
                            WarningHazard.HYDRAULIC to WarningLevel.NONE,
                            WarningHazard.HYDROGEOLOGICAL to WarningLevel.NONE,
                            WarningHazard.THUNDERSTORM to WarningLevel.ORANGE
                        )
                    )
                ),
                note = null
            )
        ),
        milano,
        today
    )

    /** The destination a posted notification would open, read off its content intent. */
    private fun destinationOf(notification: Notification): ShellTab? =
        ShellDestination.of(shadowOf(notification.contentIntent).savedIntent)

    private fun titleOf(notification: Notification): String =
        notification.extras.getString(Notification.EXTRA_TITLE).orEmpty()

    @Test
    fun `each notification opens the screen it is about, with all four on the shade`() {
        assertTrue("the sky reminder did not post", postSkyReminder())
        assertTrue("the alert did not post", postAlert())
        assertTrue("the rule did not post", postRule())
        assertTrue("the warning did not post", postWarning())

        val posted = shadowOf(manager).allNotifications
        assertEquals("four notifications, four destinations", 4, posted.size)

        // Keyed by title so a reordering of the posts cannot make this pass by accident.
        val destinations = posted.associate { titleOf(it) to destinationOf(it) }
        val byTab = destinations.values.groupingBy { it }.eachCount()
        assertEquals("the sky reminder must be the only one on Cielo", 1, byTab[ShellTab.SKY])
        assertEquals("the rule and the warning are both Avvisi", 2, byTab[ShellTab.ALERTS])
        assertEquals("the built-in alert is the weather", 1, byTab[ShellTab.TODAY])
        assertEquals("nothing may open with no screen named", null, byTab[null])
    }

    /**
     * The same four, one at a time and named: the test above proves they do not collide,
     * this one proves each one is right. Posting one alone is also the real case — a
     * shade with a single notification on it.
     */
    @Test
    fun `a sky reminder opens Cielo`() {
        assertTrue(postSkyReminder())
        assertEquals(ShellTab.SKY, destinationOf(shadowOf(manager).allNotifications.single()))
    }

    @Test
    fun `a built-in alert opens Oggi`() {
        assertTrue(postAlert())
        assertEquals(ShellTab.TODAY, destinationOf(shadowOf(manager).allNotifications.single()))
    }

    @Test
    fun `a fired rule opens Avvisi`() {
        assertTrue(postRule())
        assertEquals(ShellTab.ALERTS, destinationOf(shadowOf(manager).allNotifications.single()))
    }

    @Test
    fun `an official warning opens Avvisi`() {
        assertTrue(postWarning())
        assertEquals(ShellTab.ALERTS, destinationOf(shadowOf(manager).allNotifications.single()))
    }
}
