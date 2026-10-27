package com.callbackdev.chiaro.notifications

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.AlertEngine
import com.callbackdev.chiaro.domain.AlertKind
import com.callbackdev.chiaro.domain.AlertState
import com.callbackdev.chiaro.domain.model.WeatherCondition
import com.callbackdev.chiaro.domain.model.WeatherReport
import com.callbackdev.chiaro.domain.warnings.PlaceWarnings
import com.callbackdev.chiaro.domain.warnings.WarningHazard
import com.callbackdev.chiaro.domain.warnings.WarningLevel
import com.callbackdev.chiaro.domain.warnings.WarningNotification
import com.callbackdev.chiaro.domain.warnings.WarningZone
import com.callbackdev.chiaro.readme.MilanRecording
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The expanded bodies as the shade draws them, with no ellipsis anywhere (21 ott 2026,
 * device report: «the message ends in dots, the last characters are missing»). Every kind
 * that carries a picture is posted from the recorded Milan — a dry morning and a wet one,
 * the rain and the storm, the evening with and without tomorrow's rain, an official warning
 * with and without the bulletin's note — in Italian and in English, at the widths a phone
 * has (360, 384 and 411 dp) and at three font scales, each posted and inflated on a context
 * with that configuration, as a phone would. The body is inflated through the platform's own
 * template (`recoverBuilder`), measured at the shade's width, and each of its three text views
 * must show every character it was given: the fit before the post is what makes that true.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "sw384dp-w384dp-h832dp-xhdpi")
class NotificationFitRenderTest {

    private val app: Application = ApplicationProvider.getApplicationContext()

    @Before
    fun grant() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    private class Case(val name: String, val post: (Context) -> Boolean)

    private fun cases(): List<Case> {
        val report = MilanRecording.report
        val settings = MilanRecording.settings
        val noon = report.location.localTime
        val today = noon.toLocalDate()
        fun alerts(r: WeatherReport, at: java.time.LocalDateTime) =
            AlertEngine.evaluate(r, settings.notifications, AlertState(), at, "fit")
        val wet = report.copy(hourly = report.hourly.map { h ->
            if (h.time.toLocalDate() == today && h.time.hour in 14..17) {
                h.copy(precipChancePct = 75, condition = WeatherCondition(61))
            } else h
        })
        val stormy = report.copy(hourly = report.hourly.map { h ->
            if (h.time.toLocalDate() == today && h.time.hour == 15) {
                h.copy(precipChancePct = 60, condition = WeatherCondition(95))
            } else h
        })
        val evening = report.copy(location = report.location.copy(localTime = noon.withHour(20).withMinute(0)))
        val tomorrow = today.plusDays(1)
        val wetTomorrow = evening.copy(hourly = report.hourly.map { h ->
            if (h.time.toLocalDate() == tomorrow && h.time.hour in 9..13) {
                h.copy(precipChancePct = 80, condition = WeatherCondition(61))
            } else h
        })
        // The heat (14.6): the recorded Milan's September has none, so its afternoon is
        // lifted into a Roman July — the fit is what this test measures, not the sky.
        val hot = report.copy(hourly = report.hourly.map { h ->
            if (h.time.toLocalDate() == today && h.time.hour in 11..18) {
                h.copy(feelsLikeC = 36.0 + (h.time.hour - 11) * 0.7, tempC = h.tempC + 12)
            } else h
        })
        // The gale (14.7): the recorded Milan has none either; its afternoon gets a bora's
        // gusts, for the fit of «Raffiche fino a 101 km/h verso le 16:00» and its body.
        val windy = report.copy(hourly = report.hourly.map { h ->
            if (h.time.toLocalDate() == today && h.time.hour in 13..17) {
                h.copy(gustKph = 85.0 + (h.time.hour - 13) * 4.0 + (if (h.time.hour == 16) 4.0 else 0.0), windKph = 45.0)
            } else h
        })
        val zone = WarningZone("Lomb-09", "Nodo Idraulico di Milano", "Lombardia")
        fun levels(t: WarningLevel = WarningLevel.NONE, g: WarningLevel = WarningLevel.NONE) =
            mapOf(WarningHazard.THUNDERSTORM to t, WarningHazard.HYDROGEOLOGICAL to g, WarningHazard.HYDRAULIC to WarningLevel.NONE)
        val warning = PlaceWarnings(
            zone = zone, bulletinId = "DPC_BULLETIN_FIT", issuedAt = today.atTime(15, 7),
            days = listOf(
                PlaceWarnings.DayWarnings(today, levels(t = WarningLevel.ORANGE, g = WarningLevel.YELLOW)),
                PlaceWarnings.DayWarnings(tomorrow, levels(t = WarningLevel.YELLOW))
            ),
            // A note of the Dipartimento's usual length, a sample for the probe only.
            note = "Si segnala la possibilità di rovesci e temporali anche di forte intensità, con grandine e " +
                "raffiche di vento, in estensione dai settori occidentali verso quelli orientali nel corso del pomeriggio."
        )
        fun summary(r: WeatherReport, at: java.time.LocalDateTime, kind: AlertKind) =
            alerts(r, at).first { it.kind == kind }
        return listOf(
            Case("daily") { AlertNotifier.notify(it, summary(report, noon, AlertKind.DAILY_SUMMARY), report, settings.units) },
            Case("daily-wet") { AlertNotifier.notify(it, summary(wet, noon, AlertKind.DAILY_SUMMARY), wet, settings.units) },
            Case("rain") { AlertNotifier.notify(it, summary(wet, noon, AlertKind.PRECIPITATION), wet, settings.units) },
            Case("severe") { AlertNotifier.notify(it, summary(stormy, noon, AlertKind.SEVERE), stormy, settings.units) },
            Case("heat") { AlertNotifier.notify(it, summary(hot, noon, AlertKind.HEAT), hot, settings.units) },
            Case("wind") { AlertNotifier.notify(it, summary(windy, noon, AlertKind.SEVERE), windy, settings.units) },
            Case("evening") {
                AlertNotifier.notify(it, summary(evening, evening.location.localTime, AlertKind.EVENING_SUMMARY), evening, settings.units)
            },
            Case("evening-wet") {
                AlertNotifier.notify(it, summary(wetTomorrow, wetTomorrow.location.localTime, AlertKind.EVENING_SUMMARY), wetTomorrow, settings.units)
            },
            Case("warning") {
                OfficialWarningNotifier.notify(it, WarningNotification("fit:warn", warning), MilanRecording.city, today)
            },
            Case("warning-nonote") {
                OfficialWarningNotifier.notify(it, WarningNotification("fit:warn2", warning.copy(note = null)), MilanRecording.city, today)
            }
        )
    }

    /** A context configured as a phone of [widthDp] at [fontScale] in [locale]. */
    private fun phone(widthDp: Int, fontScale: Float, locale: Locale): Context =
        app.createConfigurationContext(
            Configuration(app.resources.configuration).apply {
                smallestScreenWidthDp = widthDp
                screenWidthDp = widthDp
                this.fontScale = fontScale
                setLocales(LocaleList(locale))
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_NO
            }
        )

    private class Shown(val label: String, val lines: Int, val ellipsis: Int, val hidden: Boolean)

    /** The three text views of the body, inflated and measured as the shade would. */
    private fun render(context: Context, n: Notification, widthDp: Int): List<Shown> {
        val density = context.resources.displayMetrics.density
        val widthPx = ((widthDp - ShadeSideMarginDp * 2) * density).toInt()
        val body = Notification.Builder.recoverBuilder(context, n).createBigContentView()
            .apply(context, FrameLayout(context))
        body.measure(
            View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        body.layout(0, 0, widthPx, body.measuredHeight)
        return listOf("title" to R.id.notif_title, "headline" to R.id.notif_headline, "details" to R.id.notif_details)
            .mapNotNull { (label, id) ->
                val tv = body.findViewById<TextView>(id) ?: return@mapNotNull null
                if (tv.visibility != View.VISIBLE) return@mapNotNull null
                val layout = tv.layout ?: return@mapNotNull null
                val last = tv.lineCount - 1
                // A line past maxLines is not laid out at all: the text's own line count
                // against the view's is what tells a hidden line.
                val hidden = layout.getLineEnd(last) < tv.text.length
                Shown(label, tv.lineCount, layout.getEllipsisCount(last), hidden)
            }
    }

    @Test
    fun `no body ends in an ellipsis on any phone, at any font size, in either language`() {
        val failures = ArrayList<String>()
        val defaultLocale = Locale.getDefault()
        try {
            for (locale in listOf(Locale.ITALY, Locale.UK)) {
                Locale.setDefault(locale)
                for (widthDp in listOf(360, 384, 411)) for (fontScale in listOf(1.0f, 1.15f, 1.3f)) {
                    val context = phone(widthDp, fontScale, locale)
                    val manager = context.getSystemService(NotificationManager::class.java)
                    for (case in cases()) {
                        manager.cancelAll()
                        assertTrue("${case.name} not posted", case.post(context))
                        val posted = shadowOf(manager).allNotifications.single()
                        assertTrue("${case.name} has no pictured body", posted.bigContentView != null)
                        for (shown in render(context, posted, widthDp)) {
                            if (shown.ellipsis > 0 || shown.hidden) {
                                failures += "${case.name} ${locale.language} ${widthDp}dp ×$fontScale ${shown.label}: " +
                                    "${shown.lines} lines, ellipsis ${shown.ellipsis}, hidden ${shown.hidden}"
                            }
                        }
                    }
                }
            }
        } finally {
            Locale.setDefault(defaultLocale)
        }
        assertEquals(failures.joinToString("\n"), 0, failures.size)
    }

    /** The budget holds on the stock template too: never more visual lines than the view's cap. */
    @Test
    fun `the details stay within the view's lines on the widest font`() {
        Locale.setDefault(Locale.ITALY)
        val context = phone(384, 1.3f, Locale.ITALY)
        val manager = context.getSystemService(NotificationManager::class.java)
        for (case in cases()) {
            manager.cancelAll()
            assertTrue(case.post(context))
            val details = render(context, shadowOf(manager).allNotifications.single(), 384).firstOrNull { it.label == "details" }
                ?: continue
            assertTrue("${case.name}: ${details.lines} lines", details.lines <= NotificationFit.MaxDetailLines)
        }
    }

    private companion object {
        /** The shade's margin at each side of a notification, stock Android. */
        const val ShadeSideMarginDp = 12
    }
}
