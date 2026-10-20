package com.callbackdev.chiaro.ui.today

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.unit.Dp
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.settings.UnitSettings
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The pager follows a selection made in the sheet, and only a settle the reader made
 * writes one back (20 ott 2026). Turning the position on from the sheet inserts its page
 * before the active place; the pager kept that place by key, reported the shifted index
 * as a settle, wrote the place back over the position, and was left halfway between the
 * two when that write cut its own scroll short.
 *
 * In `testDebug` for the reason the Sky's sheet tests give: the Compose rule's activity
 * comes from a debug-only manifest.
 */
@RunWith(RobolectricTestRunner::class)
class PagedTodayFollowTest {

    @get:Rule
    val compose = createComposeRule()

    private val milano = City(1, "Milano", null, null, Coordinates(45.46, 9.19), "Europe/Rome")
    private val tokyo = City(2, "Tokyo", null, null, Coordinates(35.68, 139.69), "Asia/Tokyo")
    private val gps = PlacePage.Gps(milano)
    private val saved = PlacePage.Saved(tokyo)

    private var model by mutableStateOf(PagerModel(listOf(saved), 0))
    private val settled = mutableListOf<String>()

    private fun show() {
        compose.setContent {
            ChiaroTheme(dynamicColor = false) {
                PagedToday(
                    model = model,
                    units = UnitSettings(),
                    locating = false,
                    locationErrorMessage = null,
                    guideCardVisible = false,
                    stateFor = { MutableStateFlow(TodayUiState.Starting) },
                    onRefresh = {},
                    onSettled = { settled += it.key },
                    onResumed = {},
                    onLocationErrorShown = {},
                    onOpenPlaces = {},
                    onOpenSettings = {},
                    onOpenGuide = {},
                    onDismissGuideCard = {}
                )
            }
        }
        compose.waitForIdle()
    }

    /** Where a page's place row stands: the row speaks its name, the text is under it. */
    private fun leftOf(name: String): Dp =
        compose.onAllNodes(hasContentDescription(name, substring = true)).onFirst()
            .getBoundsInRoot().left

    @Test
    fun `turning the position on does not settle the place it pushed aside`() {
        show()
        val home = leftOf("Tokyo")

        // The stores answer one at a time: first the page appears with Tokyo still
        // active (now its second page), then the sheet's selection of the position.
        model = PagerModel(listOf(gps, saved), 1)
        compose.waitForIdle()
        model = PagerModel(listOf(gps, saved), 0)
        compose.waitForIdle()

        assertEquals(listOf(saved.key, gps.key), settled)
        assertEquals(home, leftOf("Milano"))
    }

    @Test
    fun `a selection that turns back mid-travel is brought home`() {
        model = PagerModel(listOf(gps, saved), 1)
        show()
        val home = leftOf("Tokyo")

        compose.mainClock.autoAdvance = false
        model = PagerModel(listOf(gps, saved), 0)
        compose.mainClock.advanceTimeBy(96)
        // Back to Tokyo about a third of the way over: its nearest page is already the
        // target, and the scroll that was cut must still be finished. Here the cut scroll
        // has ended before the pager follows again; on a device it may still be running,
        // which the old gate on any scroll in progress took for the reader's finger.
        model = PagerModel(listOf(gps, saved), 1)
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()

        assertEquals(home, leftOf("Tokyo"))
    }
}
