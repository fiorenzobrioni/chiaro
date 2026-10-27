package com.callbackdev.chiaro.ui.today

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.domain.AlertEngine
import com.callbackdev.chiaro.domain.settings.UnitSettings
import com.callbackdev.chiaro.domain.settings.WindSpeedUnit
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The severe hour's sentence in words, for the one bucket that carries a number (14.7, 27 Oct
 * 2026): the gale says its strongest gust in the reader's unit and the hour it starts; the
 * storm says only its hour, as before.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "it")
class SevereTextTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val timeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val midnight: LocalDateTime = LocalDateTime.of(2025, 12, 24, 0, 0)

    private fun text(headline: Headline, units: UnitSettings = UnitSettings(), brief: Boolean = false) =
        HeadlineText.of(context, headline, timeFmt, units, brief = brief)

    @Test
    fun `the gale says its strongest gust and its hour, in the reader's unit`() {
        val bora = Headline.Severe(AlertEngine.SevereBucket.WIND, midnight, gustKph = 112.0)
        assertEquals("Raffiche fino a 112 km/h verso le 00:00", text(bora))
        assertEquals("Raffiche fino a 70 mph verso le 00:00", text(bora, UnitSettings(windSpeed = WindSpeedUnit.MPH)))
        // The widgets' brief register is the same sentence: a gale has no shorter form.
        assertEquals("Raffiche fino a 112 km/h verso le 00:00", text(bora, brief = true))
    }

    @Test
    fun `without a number the gale still names its hour`() {
        assertEquals(
            "Raffiche forti verso le 00:00",
            text(Headline.Severe(AlertEngine.SevereBucket.WIND, midnight, gustKph = null))
        )
    }

    @Test
    fun `the storm says its hour as before`() {
        assertEquals(
            "Temporale verso le 00:00",
            text(Headline.Severe(AlertEngine.SevereBucket.THUNDER, midnight))
        )
    }
}
