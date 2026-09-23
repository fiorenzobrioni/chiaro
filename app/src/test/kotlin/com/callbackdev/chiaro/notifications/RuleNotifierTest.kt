package com.callbackdev.chiaro.notifications

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.domain.rules.NotificationRule
import com.callbackdev.chiaro.domain.rules.RuleCondition
import com.callbackdev.chiaro.domain.rules.RuleOp
import com.callbackdev.chiaro.domain.rules.RuleTrigger
import com.callbackdev.chiaro.domain.sample.sampleWeatherReport
import com.callbackdev.chiaro.domain.settings.UnitSettings
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * «Why it fired» (23 set 2026, notification review): each condition as a sentence that
 * starts like one, and the value that was read WITH its unit and the reader's decimal
 * mark — it printed «valore 21.4», a bare number in the code's decimal point.
 */
@RunWith(RobolectricTestRunner::class)
class RuleNotifierTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val manager = context.getSystemService(NotificationManager::class.java)

    private fun post(): String {
        manager.cancelAll()
        val report = sampleWeatherReport().let { it.copy(current = it.current.copy(tempC = 21.4)) }
        RuleNotifier.notify(
            context,
            RuleTrigger(
                rule = NotificationRule(
                    id = 7, name = "Bici",
                    conditions = listOf(RuleCondition("current.temp_c", RuleOp.GT, 15.0)),
                    message = "Si va"
                ),
                fingerprint = null, latchKey = null, value = 21.4, at = null
            ),
            cityLabel = "Milano",
            report = report,
            now = LocalDate.of(2026, 9, 23).atTime(9, 0),
            units = UnitSettings()
        )
        return shadowOf(manager).allNotifications.single()
            .extras.getCharSequence(Notification.EXTRA_BIG_TEXT).toString()
    }

    @Test
    fun `the reading has its unit`() {
        val why = post().lines().last()
        assertTrue(why, why.endsWith(" · now 21.4°"))
        assertTrue(why, why.first().isUpperCase())
    }

    @Config(qualifiers = "it")
    @Test
    fun `and the reader's decimal mark`() {
        val why = post().lines().last()
        assertTrue(why, why.endsWith(" · ora 21,4°"))
        assertEquals(why.first().uppercaseChar(), why.first())
    }
}
