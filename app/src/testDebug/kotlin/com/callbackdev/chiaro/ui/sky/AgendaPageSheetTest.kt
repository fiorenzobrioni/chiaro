package com.callbackdev.chiaro.ui.sky

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.sky.SkyJobCatalog
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The page a row of the agenda opens (23 set 2026). What is worth holding still is the
 * one button: a subscribed moment's page removes it, a row the reader never picked adds
 * it, and either way the sheet closes, because the agenda under it is the receipt.
 *
 * In `testDebug` and not `test`: the activity a Compose rule hosts its content in is
 * declared by `ui-test-manifest`, which is a debug-only dependency so that it never
 * reaches a release APK — and the release unit tests therefore have no activity to
 * launch.
 */
@RunWith(RobolectricTestRunner::class)
class AgendaPageSheetTest {

    @get:Rule
    val compose = createComposeRule()

    private fun string(id: Int): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id)

    private val added = mutableListOf<String>()
    private val removed = mutableListOf<String>()
    private var dismissed = 0

    private fun show(jobId: String, subscribed: Boolean) {
        compose.setContent {
            ChiaroTheme(dynamicColor = false) {
                AgendaPageSheet(
                    jobId = jobId,
                    subscribed = subscribed,
                    onOpenRelated = {},
                    onAdd = { added += it },
                    onRemove = { removed += it },
                    onDismiss = { dismissed++ }
                )
            }
        }
    }

    @Test
    fun `a subscribed moment's page takes it off the list and closes`() {
        val job = SkyJobCatalog.BluePm
        show(job.id, subscribed = true)

        compose.onNodeWithText(string(R.string.sky_guide_remove_action))
            .performScrollTo()
            .performClick()

        assertEquals(listOf(job.id), removed)
        assertTrue("nothing may be added from a remove", added.isEmpty())
        assertEquals(1, dismissed)
    }

    @Test
    fun `a row the reader never picked offers to add it`() {
        val job = SkyJobCatalog.EquinoxAutumn
        show(job.id, subscribed = false)

        compose.onNodeWithText(string(R.string.sky_guide_add_action))
            .performScrollTo()
            .performClick()

        assertEquals(listOf(job.id), added)
        assertTrue("nothing may be removed from an add", removed.isEmpty())
        assertEquals(1, dismissed)
    }

    @Test
    fun `an id the catalog no longer carries closes instead of drawing an empty page`() {
        show("gone.from.the.catalog", subscribed = true)
        compose.waitForIdle()

        assertEquals(1, dismissed)
    }
}
