package com.callbackdev.chiaro.ui.icons

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * A strip dragged sideways inside a page holds the WHOLE page's weather still, not only its
 * own (committente, 15 ott 2026): it reports to the page's [PageScrolls] while it moves, and
 * the page lets go when the last scroller stops, or leaves the composition mid-scroll.
 */
@RunWith(RobolectricTestRunner::class)
class PageScrollsTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `the page is moving while any of its scrollers is, and only then`() {
        val page = PageScrolls()
        var first by mutableStateOf(false)
        var second by mutableStateOf(false)
        compose.setContent {
            CompositionLocalProvider(LocalPageScrolls provides page) {
                ReportScrolling(first)
                ReportScrolling(second)
            }
        }
        assertFalse(page.any)

        first = true
        compose.waitForIdle()
        assertTrue("one strip moving holds the page", page.any)

        second = true
        first = false
        compose.waitForIdle()
        assertTrue("another still moving keeps it held", page.any)

        second = false
        compose.waitForIdle()
        assertFalse("all still lets it go", page.any)
    }

    @Test
    fun `a scroller that leaves mid-scroll does not hold the page forever`() {
        val page = PageScrolls()
        var shown by mutableStateOf(true)
        compose.setContent {
            CompositionLocalProvider(LocalPageScrolls provides page) {
                // A day closed while its strip was still flinging.
                if (shown) ReportScrolling(true)
            }
        }
        assertTrue(page.any)

        shown = false
        compose.waitForIdle()
        assertFalse(page.any)
    }
}
