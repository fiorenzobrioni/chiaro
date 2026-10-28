package com.callbackdev.chiaro.ui.today

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.callbackdev.chiaro.domain.SnowLine
import com.callbackdev.chiaro.domain.model.DailyForecast
import com.callbackdev.chiaro.domain.model.WeatherCondition
import com.callbackdev.chiaro.domain.settings.UnitSettings
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import java.time.LocalDate
import java.util.Locale
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The open day's snow line (14.8): «Snow above 1,200 m», «roughly» when it is read off the
 * freezing level, with its consequence for the place's own height; drawn only when the
 * line is near enough to matter here. In `testDebug` because the Compose rule's activity
 * comes from the debug-only manifest.
 */
@RunWith(RobolectricTestRunner::class)
class DayFactsTest {

    @get:Rule
    val compose = createComposeRule()

    private fun wetDay(snowLine: SnowLine?) = DailyForecast(
        date = LocalDate.of(2026, 1, 16), highC = 4.0, lowC = -1.0, condition = WeatherCondition(61),
        precipPct = 80, uvIndexMax = 1, precipMm = 6.0, rainMm = 6.0, precipHours = 5.0, snowLine = snowLine
    )

    private fun show(day: DailyForecast, elevationM: Double?) {
        compose.setContent {
            ChiaroTheme(dynamicColor = false) {
                DayFacts(forecast = day, units = UnitSettings(), locale = Locale.ENGLISH, elevationM = elevationM)
            }
        }
    }

    @Test
    fun `an estimated line says roughly, and above the place the rain here is snow higher up`() {
        show(wetDay(SnowLine(1200, estimated = true)), elevationM = 120.0)
        compose.onNodeWithText("Snow above roughly 1,200 m").assertExists()
        compose.onNodeWithText("Rain here, snow on the roads higher up").assertExists()
    }

    @Test
    fun `the model's own line is said as it is, to the hundred metres`() {
        // Bergamo, 250 m: 1 720 m is within the 1 500 m above it that make a line its own.
        show(wetDay(SnowLine(1720, estimated = false)), elevationM = 250.0)
        compose.onNodeWithText("Snow above 1,700 m").assertExists()
        compose.onAllNodesWithText("roughly", substring = true).assertCountEquals(0)
    }

    @Test
    fun `at or below the place, the snow is the place's own`() {
        show(wetDay(SnowLine(800, estimated = false)), elevationM = 1225.0)
        compose.onNodeWithText("Snow above 800 m").assertExists()
        compose.onNodeWithText("Snow down to here, rain lower down").assertExists()
    }

    @Test
    fun `a line too far above the place is not a fact of its day`() {
        show(wetDay(SnowLine(2800, estimated = true)), elevationM = 120.0)
        compose.onAllNodesWithText("Snow above", substring = true).assertCountEquals(0)
        // The day's other facts stand as before.
        compose.onNodeWithText("6.0 mm of rain over 5 hours").assertExists()
    }
}
