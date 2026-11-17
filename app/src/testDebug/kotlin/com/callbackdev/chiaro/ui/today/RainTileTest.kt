package com.callbackdev.chiaro.ui.today

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.model.DailyForecast
import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.model.WeatherCondition
import com.callbackdev.chiaro.domain.sample.sampleWeatherReport
import com.callbackdev.chiaro.domain.settings.UnitSettings
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The note of the rain tile in the details grid (17 nov 2026): «Over 5 hours» beside the
 * day's millimetres, the hours the model gives an amount to, and no hours at all on a day
 * whose strip draws no wet hour. In `testDebug` because the Compose rule's activity comes
 * from the debug-only manifest.
 */
@RunWith(RobolectricTestRunner::class)
class RainTileTest {

    @get:Rule
    val compose = createComposeRule()

    private val zone = ZoneId.of("Europe/Rome")
    private val milan = City(
        id = 1L, name = "Milano", region = "Lombardia", country = "Italia",
        coordinates = Coordinates(45.4643, 9.1895), timezone = "Europe/Rome"
    )
    private val date: LocalDate = LocalDate.of(2026, 9, 2)
    private val cloudy = WeatherCondition(3)

    private fun show(rainMm: Double, precipHours: Int?) {
        val fetched = LocalDateTime.of(2026, 9, 2, 12, 0)
        val sample = sampleWeatherReport()
        val report = sample.copy(
            location = sample.location.copy(
                city = "Milano", coordinates = milan.coordinates, timezone = "Europe/Rome", localTime = fetched
            ),
            hourly = (0 until 48).map {
                HourlyForecast(
                    time = fetched.plusHours(it.toLong()),
                    at = fetched.plusHours(it.toLong()).atZone(zone).toInstant(),
                    tempC = 20.0,
                    condition = cloudy,
                    precipChancePct = 10,
                    cloudCoverPct = 90
                )
            },
            daily = (0 until 7).map {
                DailyForecast(
                    date.plusDays(it.toLong()), 24.0, 14.0, cloudy, 10, 5,
                    precipMm = rainMm, rainMm = rainMm, precipHours = precipHours.takeIf { _ -> it == 0 }
                )
            },
            systemInfo = sample.systemInfo.copy(lastSync = fetched.atZone(zone).toInstant())
        )
        val now = LocalDateTime.of(2026, 9, 2, 12, 30).atZone(zone).toInstant()
        val content = TodayStateBuilder.build(milan, report, now, 60, false, null) as TodayUiState.Content
        compose.setContent {
            ChiaroTheme(dynamicColor = false) {
                Details(content = content, units = UnitSettings(), locale = Locale.ENGLISH)
            }
        }
    }

    @Test
    fun `the rain tile says over how many hours the day's millimetres fall`() {
        show(rainMm = 6.0, precipHours = 5)
        compose.onNodeWithText("6.0 mm").assertExists()
        compose.onAllNodesWithText("Over 5 hours", substring = true).assertCountEquals(1)
    }

    @Test
    fun `on a day with no hours to say the tile keeps its millimetres and says no hours`() {
        // Turin, 22 Nov 2026: 2.4 mm over nine hours the app calls none of wet.
        show(rainMm = 2.4, precipHours = null)
        compose.onNodeWithText("2.4 mm").assertExists()
        compose.onAllNodesWithText("hours", substring = true).assertCountEquals(0)
    }
}
