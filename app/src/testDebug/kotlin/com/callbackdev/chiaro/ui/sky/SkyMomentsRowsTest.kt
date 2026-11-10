package com.callbackdev.chiaro.ui.sky

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.data.SkySubscription
import com.callbackdev.chiaro.domain.sky.SkyJobCatalog
import com.callbackdev.chiaro.readme.MilanRecording
import com.callbackdev.chiaro.ui.format.LocalClock
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import java.time.Clock
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The moments as Today's agenda draws them (Fase 16.2, 10 nov 2026): the hour at the row's
 * start with a span's end under it, the first moment saying how soon in the row every other
 * moment has, a moment that is happening saying how much of it is left, the rows with no
 * hour after the hours, and «Add» in the section's title where a floating button stood over
 * the list. A tall window, so the whole page is composed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rGB-w384dp-h2600dp-xhdpi")
class SkyMomentsRowsTest {

    @get:Rule
    val compose = createComposeRule()

    private val app = ApplicationProvider.getApplicationContext<Context>()
    private val goldenHour = app.getString(SkyText.nameRes(SkyJobCatalog.GoldenPm.id))

    private fun show(at: Instant, fontScale: Float = 1f) {
        val state = SkyStateBuilder.build(
            city = MilanRecording.city,
            report = MilanRecording.report,
            subscriptions = SkyJobCatalog.defaults.map { SkySubscription(it.id) },
            settings = MilanRecording.settings,
            now = at
        )
        compose.setContent {
            ChiaroTheme(dynamicColor = false) {
                CompositionLocalProvider(
                    LocalClock provides Clock.fixed(at, state.zone),
                    LocalDensity provides Density(LocalDensity.current.density, fontScale)
                ) {
                    SkyScreen(
                        state = state,
                        actions = SkyActions({}, {}, { _, _ -> }, {}),
                        onOpenPlaces = {},
                        onOpenSettings = {},
                        onOpenGuide = {}
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    private fun bounds(text: String): Rect =
        compose.onNode(hasText(text), useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

    private fun count(text: String, substring: Boolean = false): Int =
        compose.onAllNodes(hasText(text, substring = substring), useUnmergedTree = true).fetchSemanticsNodes().size

    /** The «19:15» of the golden hour's own row: the sunset's row prints it too. */
    private fun goldenHourEnd(): Rect {
        val name = bounds(goldenHour)
        return compose.onAllNodes(hasText("19:15"), useUnmergedTree = true).fetchSemanticsNodes()
            .map { it.boundsInRoot }
            .minBy { kotlin.math.abs(it.center.y - name.center.y) }
    }

    @Test
    fun `the hour stands at the row's start, and a span's end under its start`() {
        // 12:15 in Milan on the recording's day: the evening's golden hour is 18:36 to 19:15.
        show(MilanRecording.now)
        val name = bounds(goldenHour)
        val start = bounds("18:36")
        val end = goldenHourEnd()
        assertTrue("the hour (${start.right}) before the name (${name.left})", start.right < name.left)
        assertTrue("the end (${end.top}) under the start (${start.bottom})", end.top >= start.bottom - 1f)
        assertEquals("in the same column", start.left, end.left, 1f)
        // A screen reader hears the span, not two numbers.
        compose.onNode(hasContentDescription(app.getString(R.string.sky_until, "19:15")), useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun `the first moment says how soon in a row like the others, and no card says next`() {
        show(MilanRecording.now)
        // 12:15 to 18:36: six hours and twenty minutes, the seconds dropped.
        val soon = app.getString(R.string.tl_in_hours_minutes, 6, 20).replaceFirstChar { it.uppercase() }
        assertEquals("once, on the head: $soon", 1, count(soon))
        val label = bounds(soon)
        val name = bounds(goldenHour)
        assertTrue("over the golden hour's name", label.bottom <= name.top + 1f && label.left == name.left)
        assertEquals(0, count(app.getString(R.string.sky_next_label, ""), substring = true))
        // And the row's time is the column's, not a line of its own: no «18:36 – 19:15».
        assertEquals(0, count("18:36 – 19:15"))
    }

    @Test
    fun `a moment that is happening says so where its hour would be, with how much is left and until when`() {
        // 18:50 in Milan: twenty-five minutes of golden hour left.
        show(Instant.parse("2026-09-25T16:50:00Z"))
        val left = app.getString(R.string.sky_left_minutes, 25)
        assertEquals(1, count(left))
        val name = bounds(goldenHour)
        val now = bounds(app.getString(R.string.sky_moment_now))
        assertTrue("the word where the hour would be", now.right < name.left)
        assertEquals(
            "once: the line over the name does not repeat it",
            1, count(app.getString(R.string.sky_moment_now), substring = true)
        )
        assertEquals("its start is not printed", 0, count("18:36"))
        assertTrue(count(app.getString(R.string.sky_until, "19:15"), substring = true) >= 1)
        // The moment after it is not a second head.
        assertEquals(0, count(app.getString(R.string.tl_in_minutes, 25).replaceFirstChar { it.uppercase() }))
    }

    @Test
    fun `a row with no hour stands after the hours of its day, its drawing in their column`() {
        show(MilanRecording.now)
        val moon = bounds(app.getString(SkyText.nameRes(SkyJobCatalog.MoonToday.id)))
        val sunset = bounds(app.getString(SkyText.nameRes(SkyJobCatalog.SunSet.id)))
        val sunrise = bounds(app.getString(SkyText.nameRes(SkyJobCatalog.SunRise.id)))
        assertTrue("today's moon under today's sunset", moon.top > sunset.bottom)
        assertTrue("and over tomorrow's sunrise", moon.bottom < sunrise.top)
        assertEquals("the names in one column, the clock's kept empty", sunset.left, moon.left, 1f)
    }

    @Test
    fun `past 150 percent of type the clock is a line above the name`() {
        show(MilanRecording.now, fontScale = 2f)
        val name = bounds(goldenHour)
        val start = bounds("18:36")
        val end = goldenHourEnd()
        assertTrue("the hour (${start.bottom}) over the name (${name.top})", start.bottom <= name.top)
        assertEquals("the span on one line", start.center.y, end.center.y, 2f)
        assertTrue("its end after its start", end.left > start.right)
    }

    @Test
    fun `adding is a button in the moments' title, a full target, and it opens the catalog`() {
        show(MilanRecording.now)
        val add = compose.onNode(hasClickAction() and hasContentDescription(app.getString(R.string.sky_add_moment)))
        add.assertTouchHeightIsEqualTo(48.dp).assertWidthIsAtLeast(48.dp)
        add.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        val button = add.fetchSemanticsNode().boundsInRoot
        val title = bounds(app.getString(R.string.sky_section_moments))
        assertEquals("on the title's line", title.center.y, button.center.y, 2f)
        assertTrue("after the title", button.left >= title.right)
        val word = bounds(app.getString(R.string.sky_add))
        assertTrue("one word, inside the button", word.left > button.left && word.right < button.right)
        // The button does not make the title's line taller: the day's heading starts where
        // it would under a title with no action, 8dp of its own under the title's 4.
        val day = bounds(app.getString(R.string.sky_day_today))
        val gap = with(compose.density) { (day.top - title.bottom).toDp() }
        assertTrue("the heading $gap under the title", gap <= 13.dp)
        add.performClick()
        compose.waitForIdle()
        compose.onNode(hasText(app.getString(R.string.sky_catalog_search))).assertExists()
    }

    @Test
    fun `the head is the first moment with an hour, never the moon's phase`() {
        val state = MilanRecording.sky()
        val head = momentsHead(state.moments)
        assertEquals(SkyJobCatalog.GoldenPm.id, head?.job?.id)
        assertSame(state.moments.first { it.moonPhase == null }, head)
        assertNull(momentsHead(state.moments.filter { it.moonPhase != null }))
        // The day's order keeps the hours as the state gives them and moves only the rest.
        val day = dayOrder(state.moments)
        assertEquals(state.moments.filter { it.moonPhase == null }, day.takeWhile { it.moonPhase == null })
        assertTrue(day.last().moonPhase != null)
    }
}
