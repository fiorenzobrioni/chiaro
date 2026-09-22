package com.callbackdev.chiaro.ui.shell

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The shell's back stacks (22 set 2026, Navigation 3). The animation is a device check;
 * what can be checked here is what back DOES, which is the half a reader can lose work
 * to: the page each gesture lands on, the tab it leaves the reader in, and the stacks a
 * widget tap clears.
 */
@RunWith(RobolectricTestRunner::class)
class ChiaroNavigationStateTest {

    private fun state(): ChiaroNavigationState = ChiaroNavigationState(
        selectedName = mutableStateOf(ShellTab.TODAY.name),
        backStacks = ShellTab.entries.associateWith { NavBackStack(it.root) }
    )

    /** What `NavDisplay` is handed: the stacks in use, flattened. */
    private fun ChiaroNavigationState.onScreen(): List<NavKey> =
        tabsInUse().flatMap { backStacks.getValue(it) }

    @Test
    fun `a fresh shell shows Today alone, with the bar`() {
        val nav = state()
        assertEquals(listOf(TodayKey), nav.onScreen())
        assertTrue(nav.showsBottomBar)
    }

    @Test
    fun `a tab sits over Today, so back from its root returns to Today`() {
        val nav = state()
        nav.switchTab(ShellTab.SKY)
        assertEquals(listOf(TodayKey, SkyKey), nav.onScreen())

        nav.goBack()
        assertEquals(ShellTab.TODAY, nav.selected)
        assertEquals(listOf(TodayKey), nav.onScreen())
    }

    @Test
    fun `back at Today's root is left to the system`() {
        val nav = state()
        nav.goBack()
        assertEquals(ShellTab.TODAY, nav.selected)
        assertEquals(listOf(TodayKey), nav.onScreen())
    }

    @Test
    fun `settings covers the bar and back closes it`() {
        val nav = state()
        nav.switchTab(ShellTab.ALERTS)
        nav.navigate(SettingsKey)
        assertEquals(listOf(TodayKey, AlertsKey, SettingsKey), nav.onScreen())
        assertFalse(nav.showsBottomBar)

        nav.goBack()
        assertEquals(ShellTab.ALERTS, nav.selected)
        assertTrue(nav.showsBottomBar)
    }

    @Test
    fun `the guide returns through the door it entered`() {
        val fromSettings = state()
        fromSettings.navigate(SettingsKey)
        fromSettings.navigate(GuideKey)
        fromSettings.goBack()
        assertEquals(SettingsKey, fromSettings.currentKey)

        val fromToday = state()
        fromToday.navigate(GuideKey)
        fromToday.goBack()
        assertEquals(TodayKey, fromToday.currentKey)
    }

    @Test
    fun `the sky guide keeps the bar in the Sky tab and not over the guide`() {
        val inTab = state()
        inTab.switchTab(ShellTab.SKY)
        inTab.navigate(SkyGuideKey(inTab = true))
        inTab.navigate(SkyEventKey("golden_hour.pm", inTab = true))
        assertTrue(inTab.showsBottomBar)

        val overGuide = state()
        overGuide.navigate(GuideKey)
        overGuide.navigate(SkyGuideKey(inTab = false))
        assertFalse(overGuide.showsBottomBar)
    }

    @Test
    fun `a related event takes the page's place, so back still reaches the index`() {
        val nav = state()
        nav.switchTab(ShellTab.SKY)
        nav.navigate(SkyGuideKey(inTab = true))
        nav.navigate(SkyEventKey("golden_hour.pm", inTab = true))
        nav.replaceTop(SkyEventKey("blue_hour.pm", inTab = true))
        assertEquals(SkyEventKey("blue_hour.pm", inTab = true), nav.currentKey)

        nav.goBack()
        assertEquals(SkyGuideKey(inTab = true), nav.currentKey)
    }

    @Test
    fun `a tab keeps its open page while another tab is on screen`() {
        val nav = state()
        nav.switchTab(ShellTab.SKY)
        nav.navigate(SkyGuideKey(inTab = true))
        nav.switchTab(ShellTab.JOURNAL)
        assertEquals(listOf(TodayKey, JournalKey), nav.onScreen())

        nav.switchTab(ShellTab.SKY)
        assertEquals(SkyGuideKey(inTab = true), nav.currentKey)
    }

    @Test
    fun `a tap from outside lands on the tab's own screen, with nothing left open`() {
        val nav = state()
        nav.navigate(SettingsKey)
        nav.navigate(GuideKey)

        nav.openFromOutside(ShellTab.SKY)
        assertEquals(listOf(TodayKey, SkyKey), nav.onScreen())
        assertTrue(nav.showsBottomBar)

        // And one back later the reader is on Today, not on the Settings page the tap
        // had closed.
        nav.goBack()
        assertEquals(listOf(TodayKey), nav.onScreen())
    }

    @Test
    fun `every key survives the trip through saved state`() {
        // rememberNavBackStack saves the stacks through kotlinx.serialization: a key
        // without its serializer would crash on the first rotation, not at compile time.
        val keys: List<NavKey> = listOf(
            TodayKey, SkyKey, AlertsKey, JournalKey, SettingsKey, GuideKey,
            SkyGuideKey(inTab = true), SkyEventKey("golden_hour.pm", inTab = false)
        )
        keys.forEach { key ->
            @Suppress("UNCHECKED_CAST")
            val serializer = serializer(key::class.java) as kotlinx.serialization.KSerializer<NavKey>
            assertEquals(key, Json.decodeFromString(serializer, Json.encodeToString(serializer, key)))
        }
    }
}
