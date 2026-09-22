package com.callbackdev.chiaro.ui.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import kotlinx.serialization.Serializable

/*
 * The shell's destinations as Navigation 3 keys (22 set 2026). Until then the shell was
 * two enums and a `BackHandler` (Fase 4), and back swapped the screen the instant the
 * finger lifted; as keys on a back stack, `NavDisplay` can run the pop while the gesture
 * is still under the finger, which is the predictive back of DESIGN §7. Each key is
 * [Serializable] so the stacks survive a rotation and process death, as the enum did.
 */

/** The four tab roots, one per [ShellTab]. */
@Serializable
data object TodayKey : NavKey

@Serializable
data object SkyKey : NavKey

@Serializable
data object AlertsKey : NavKey

@Serializable
data object JournalKey : NavKey

/** Settings, from the gear every tab carries. It covers the bottom bar. */
@Serializable
data object SettingsKey : NavKey

/** The guide, from Settings or from Today's one-time card. It covers the bottom bar. */
@Serializable
data object GuideKey : NavKey

/**
 * The index of the sky events guide. [inTab] says which door it came through: from the
 * Sky tab the reader never left the tab, so the bottom bar stays (and the page leaves
 * room for it); from the guide it sits over a page that had already covered the bar.
 */
@Serializable
data class SkyGuideKey(val inTab: Boolean) : NavKey

/** One event's page in that guide, [inTab] inherited from the index it came from. */
@Serializable
data class SkyEventKey(val jobId: String, val inTab: Boolean) : NavKey

/** The root of [this] tab's stack. */
val ShellTab.root: NavKey
    get() = when (this) {
        ShellTab.TODAY -> TodayKey
        ShellTab.SKY -> SkyKey
        ShellTab.ALERTS -> AlertsKey
        ShellTab.JOURNAL -> JournalKey
    }

/**
 * One back stack per tab — Navigation 3's "multiple back stacks" recipe, the same shape
 * as Saldo's `SaldoNavigationState`. A tab keeps its stack, and through the per-stack
 * decorator its saved state (the scroll of Today, an open page of the sky guide), while
 * another tab is on screen.
 *
 * What is on screen is `[Today's stack] + [the selected tab's stack]` ("exit through
 * home"): back from a tab's root lands on Today, back from Today's root leaves the app.
 * That is the rule the old `BackHandler` kept (Sky → Today), now with the page underneath
 * actually there for the gesture to reveal.
 */
@Stable
class ChiaroNavigationState(
    private val selectedName: MutableState<String>,
    val backStacks: Map<ShellTab, NavBackStack<NavKey>>
) {

    /** The selected tab; saved by enum name, so it survives process death. */
    var selected: ShellTab
        get() = ShellTab.entries.firstOrNull { it.name == selectedName.value } ?: ShellTab.TODAY
        private set(value) {
            selectedName.value = value.name
        }

    private val currentStack: NavBackStack<NavKey>
        get() = backStacks.getValue(selected)

    /** The key on top of what is on screen. */
    val currentKey: NavKey?
        get() = currentStack.lastOrNull()

    /**
     * Whether the bottom bar is drawn: not once Settings or the guide is open in the
     * selected tab, nor over anything opened from them. Asked of the selected stack only,
     * because a Settings page left open under Today is not on screen while Sky is.
     */
    val showsBottomBar: Boolean
        get() = currentStack.none { it == SettingsKey || it == GuideKey }

    /** Opens [key] on top of the selected tab's stack. */
    fun navigate(key: NavKey) {
        currentStack.add(key)
    }

    /**
     * Puts [key] in place of the page on top: a related event opened from an event's
     * page takes that page's place, so back still returns to the index it came from.
     */
    fun replaceTop(key: NavKey) {
        if (currentStack.size > 1) currentStack[currentStack.lastIndex] = key else navigate(key)
    }

    /** Selects [tab], keeping every other tab's stack as it was. Reselecting is a no-op. */
    fun switchTab(tab: ShellTab) {
        selected = tab
    }

    /**
     * A tab asked for from outside the app — a widget or a notification. The answer is
     * that tab's own screen, so every stack goes back to its root: landing on Sky under an
     * open Settings page (or on Sky over one left open under Today, one back away) would be
     * the app answering the tap and hiding the answer.
     */
    fun openFromOutside(tab: ShellTab) {
        backStacks.values.forEach { stack ->
            while (stack.size > 1) stack.removeAt(stack.lastIndex)
        }
        selected = tab
    }

    /**
     * Back: pops the selected tab's stack, or from a tab's root returns to Today. At
     * Today's root there is nothing to pop, and the system's back leaves the app.
     */
    fun goBack() {
        if (currentStack.size > 1) {
            currentStack.removeAt(currentStack.lastIndex)
        } else if (selected != ShellTab.TODAY) {
            selected = ShellTab.TODAY
        }
    }

    /** The stacks on screen right now: Today's, then the selected tab's. */
    fun tabsInUse(): List<ShellTab> =
        if (selected == ShellTab.TODAY) listOf(ShellTab.TODAY) else listOf(ShellTab.TODAY, selected)
}

/**
 * The shell's navigation state, remembered. Every stack is a [rememberNavBackStack], so
 * its contents survive a configuration change and process death; the selected tab is a
 * plain saveable string, as it was when it was an enum.
 */
@Composable
fun rememberChiaroNavigationState(): ChiaroNavigationState {
    val selectedName = rememberSaveable { mutableStateOf(ShellTab.TODAY.name) }
    val backStacks = ShellTab.entries.associateWith { tab -> rememberNavBackStack(tab.root) }
    return remember { ChiaroNavigationState(selectedName, backStacks) }
}

/**
 * Decorates each tab's entries with its own saveable-state holder, then flattens the
 * stacks in use into the list `NavDisplay` draws. Per stack rather than over the visible
 * list is the point: a hidden tab's entries stay decorated, so what they saved is still
 * there when the reader comes back.
 *
 * No ViewModel decorator, deliberately: the ViewModels stay scoped to the activity, as
 * they were before the stacks. The four tabs share one `PlacesViewModel`, and the active
 * place and the subscriptions are meant to survive a tab switch in the ViewModels
 * themselves; a store per entry would have split them.
 */
@Composable
fun ChiaroNavigationState.rememberDecoratedEntries(
    entryProvider: (NavKey) -> NavEntry<NavKey>
): List<NavEntry<NavKey>> {
    val decorated = backStacks.mapValues { (_, stack) ->
        rememberDecoratedNavEntries(
            backStack = stack,
            entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator<NavKey>()),
            entryProvider = entryProvider
        )
    }
    return tabsInUse().flatMap { decorated.getValue(it) }
}
