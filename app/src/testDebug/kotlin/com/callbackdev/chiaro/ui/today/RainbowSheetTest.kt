package com.callbackdev.chiaro.ui.today

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertCountEquals
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.sky.RainbowWindow
import com.callbackdev.chiaro.ui.sky.SkyText
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The rainbow's page (15 ott 2026): what it is, how high, where to look, and that it is a
 * possibility. An October afternoon in Milan, the window 16:00–18:00, with the sun low in the
 * south-west and the bow in the north-east.
 *
 * In `testDebug` for the reason `AgendaPageSheetTest` gives.
 */
@RunWith(RobolectricTestRunner::class)
class RainbowSheetTest {

    @get:Rule
    val compose = createComposeRule()

    private val app: Context = ApplicationProvider.getApplicationContext()
    private val milan = Coordinates(45.4642, 9.1900)
    private val rome = ZoneId.of("Europe/Rome")
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

    private val window = TimelineItem(
        at = LocalDateTime.of(2026, 10, 15, 16, 0),
        kind = TimelineKind.RAINBOW,
        pct = 60,
        bearingDeg = 45.0,
        until = LocalDateTime.of(2026, 10, 15, 18, 0)
    )

    private fun show(now: LocalDateTime) {
        compose.setContent {
            ChiaroTheme(dynamicColor = false) {
                RainbowSheet(window, now, rome, milan, timeFmt, onDismiss = {})
            }
        }
        compose.waitForIdle()
    }

    private fun shown(text: String) = compose.onAllNodesWithText(text, substring = true).assertCountEquals(1)

    @Test
    fun `ahead of the window the page says what it is, where, how high and that it may not happen`() {
        show(now = LocalDateTime.of(2026, 10, 15, 12, 0))
        shown(app.getString(R.string.rainbow_title))
        shown(app.getString(R.string.rainbow_when, "16:00", "18:00", 60))
        shown(app.getString(R.string.sky_look_title))
        // Ahead of the window, the row's own bearing: north-east, as the row said.
        shown(app.getString(R.string.sky_look_towards, app.getString(SkyText.bearingRes(45.0))))
        shown(app.getString(R.string.rainbow_about))
        shown(app.getString(R.string.rainbow_honest))
        // The height at the window's middle, 17:00, in words.
        val top = RainbowWindow.sight(LocalDateTime.of(2026, 10, 15, 17, 0).atZone(rome).toInstant(), milan)!!
        shown(app.getString(rainbowHeightRes(top.topAltitudeDeg)))
    }

    @Test
    fun `inside the window the bow is where it stands now`() {
        val now = LocalDateTime.of(2026, 10, 15, 17, 40)
        show(now)
        val sight = RainbowWindow.sight(now.atZone(rome).toInstant(), milan)!!
        shown(app.getString(R.string.sky_look_towards, app.getString(SkyText.bearingRes(sight.lookTowardsDeg))))
    }

    @Test
    fun `the bow's height is said in words, from a band on the horizon to most of a half circle`() {
        assertEquals(R.string.rainbow_height_low, rainbowHeightRes(5.0))
        assertEquals(R.string.rainbow_height_mid, rainbowHeightRes(LowBowDeg))
        assertEquals(R.string.rainbow_height_mid, rainbowHeightRes(20.0))
        assertEquals(R.string.rainbow_height_tall, rainbowHeightRes(TallBowDeg))
        assertEquals(R.string.rainbow_height_tall, rainbowHeightRes(40.0))
    }
}
