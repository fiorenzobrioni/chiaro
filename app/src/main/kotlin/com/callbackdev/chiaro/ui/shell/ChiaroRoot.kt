package com.callbackdev.chiaro.ui.shell

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.callbackdev.chiaro.data.CityStore
import com.callbackdev.chiaro.data.FirstRun
import com.callbackdev.chiaro.data.ServiceLocator
import com.callbackdev.chiaro.data.WeatherRepository
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.ui.alerts.AlertsRoute
import com.callbackdev.chiaro.ui.firstrun.FirstRunNotificationsRoute
import com.callbackdev.chiaro.ui.firstrun.FirstRunRoute
import com.callbackdev.chiaro.ui.guide.GuideRoute
import com.callbackdev.chiaro.ui.icons.ChiaroIcons
import com.callbackdev.chiaro.ui.journal.JournalRoute
import com.callbackdev.chiaro.ui.settings.SettingsRoute
import com.callbackdev.chiaro.ui.sky.SkyEventRoute
import com.callbackdev.chiaro.ui.sky.SkyGuideIndexRoute
import com.callbackdev.chiaro.ui.sky.SkyRoute
import com.callbackdev.chiaro.ui.theme.ChiaroMotion
import com.callbackdev.chiaro.ui.theme.reducedMotion
import com.callbackdev.chiaro.ui.today.TodayRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The one question the shell answers before drawing anything: does first-run still
 * owe an answer? [FirstRun.Unknown] draws a bare surface — flashing either screen at
 * the wrong person is worse than one blank frame — and the migration check that
 * resolves it runs exactly once per install (VISION §5.8).
 */
class ShellViewModel(
    repository: WeatherRepository,
    private val cityStore: CityStore
) : ViewModel() {

    init {
        viewModelScope.launch {
            cityStore.migrateFirstRun(hasHistory = repository.hasAnyHistory())
        }
    }

    val firstRun: StateFlow<FirstRun> = cityStore.firstRun
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FirstRun.Unknown)

    /**
     * Skipping is an answer (VISION §5.8): it lands on the real "no place" state —
     * and it answers the notification step too, in one edit. A reader who has just
     * declined to name a place has nothing for an alert to be about yet, and stopping
     * them with a second question would make "skip" mean "skip one of two".
     */
    fun skip() {
        viewModelScope.launch { cityStore.markFirstRunSkipped() }
    }

    /** The notification step has been passed — allowed, refused, or left for later. */
    fun notificationsAnswered() {
        viewModelScope.launch { cityStore.markNotificationsAsked() }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                ShellViewModel(
                    repository = ServiceLocator.weatherRepository(app),
                    cityStore = ServiceLocator.cityStore(app)
                )
            }
        }
    }
}

@Composable
fun ChiaroRoot(
    /** The tab an outside tap asked for — a home widget's (see [ShellDestination]) —
     * or null for a plain launch, which lands wherever the reader left off. */
    requestedTab: ShellTab? = null,
    onRequestedTabHandled: () -> Unit = {},
    shellViewModel: ShellViewModel = viewModel(factory = ShellViewModel.Factory)
) {
    val firstRun by shellViewModel.firstRun.collectAsStateWithLifecycle()
    when (firstRun) {
        FirstRun.Unknown -> Surface(modifier = Modifier.fillMaxSize()) { }
        FirstRun.Pending -> FirstRunRoute(onSkip = shellViewModel::skip)
        // The one question that could not be asked on the screen before it: a
        // notification is a promise about a place, and there was no place yet.
        FirstRun.Notifications ->
            FirstRunNotificationsRoute(onDone = shellViewModel::notificationsAnswered)
        // A widget tapped before first-run is answered keeps its request rather than
        // losing it: the shell honours it on the first frame the tabs exist.
        FirstRun.Done -> MainScreens(requestedTab, onRequestedTabHandled)
    }
}

/** Height of the Material 3 navigation bar's content, without the system inset under it. */
private val BottomBarHeight = 80.dp

/**
 * The shell (Fase 4, bottom bar since Fase 5, Navigation 3 since 22 set 2026): the four
 * tabs of VISION §5.1, the pages opened over them — Settings from the gear, the guide
 * from Settings or from the one-time card on Today, the sky events guide from Sky or
 * from the guide — and the bottom bar over all of it.
 *
 * The bar is drawn OVER the display rather than beside it, as in Saldo: a bar in the
 * layout would reshape the area every page is laid out in each time it came or went, and
 * a page would jump at the end of its own transition. Over it, every page is laid out at
 * its final size from the first frame — the tab pages leave the bar's height free at the
 * bottom, which is exactly the area the old `Column` gave them — and only the bar moves.
 */
@Composable
private fun MainScreens(requestedTab: ShellTab?, onRequestedTabHandled: () -> Unit) {
    val nav = rememberChiaroNavigationState()

    // A destination asked for from outside wins over what was on screen, and takes the
    // pages opened over the tabs down with it ([ChiaroNavigationState.openFromOutside]).
    LaunchedEffect(requestedTab) {
        if (requestedTab != null) {
            nav.openFromOutside(requestedTab)
            onRequestedTabHandled()
        }
    }

    // The bar's full height, system inset included: what the old `Column` took from the
    // tab pages, so they are laid out to the pixel where they were. Plain padding and not
    // an insets modifier, so nothing is consumed and every page still reads the
    // navigation-bar inset it read before.
    val barHeight = BottomBarHeight +
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val tabPage = Modifier
        .fillMaxSize()
        .padding(bottom = barHeight)

    val provider = entryProvider<NavKey> {
        entry<TodayKey> {
            Box(tabPage) {
                TodayRoute(
                    onOpenSettings = { nav.navigate(SettingsKey) },
                    onOpenGuide = { nav.navigate(GuideKey) },
                    onOpenJournal = { nav.switchTab(ShellTab.JOURNAL) }
                )
            }
        }
        entry<SkyKey> {
            Box(tabPage) {
                SkyRoute(
                    onOpenSettings = { nav.navigate(SettingsKey) },
                    onOpenGuide = { nav.navigate(SkyGuideKey(inTab = true)) }
                )
            }
        }
        entry<AlertsKey> {
            Box(tabPage) { AlertsRoute(onOpenSettings = { nav.navigate(SettingsKey) }) }
        }
        entry<JournalKey> {
            Box(tabPage) { JournalRoute(onOpenSettings = { nav.navigate(SettingsKey) }) }
        }
        entry<SettingsKey> {
            SettingsRoute(
                onBack = { nav.goBack() },
                onOpenGuide = { nav.navigate(GuideKey) }
            )
        }
        // The guide has two doors (VISION §5.7); back returns through the one it entered,
        // because the door is simply the page under it on the stack.
        entry<GuideKey> {
            GuideRoute(
                onBack = { nav.goBack() },
                onOpenSkyGuide = { nav.navigate(SkyGuideKey(inTab = false)) }
            )
        }
        entry<SkyGuideKey> { key ->
            SkyGuideIndexRoute(
                onBack = { nav.goBack() },
                onOpen = { id -> nav.navigate(SkyEventKey(id, key.inTab)) },
                modifier = if (key.inTab) tabPage else Modifier.fillMaxSize()
            )
        }
        entry<SkyEventKey> { key ->
            SkyEventRoute(
                jobId = key.jobId,
                onBack = { nav.goBack() },
                onOpenRelated = { id -> nav.replaceTop(SkyEventKey(id, key.inTab)) },
                modifier = if (key.inTab) tabPage else Modifier.fillMaxSize()
            )
        }
    }

    val reduced = reducedMotion()
    Box(modifier = Modifier.fillMaxSize()) {
        NavDisplay(
            entries = nav.rememberDecoratedEntries(provider),
            modifier = Modifier.fillMaxSize(),
            onBack = { nav.goBack() },
            transitionSpec = { forwardTransition(reduced) },
            popTransitionSpec = { backwardTransition(reduced) },
            predictivePopTransitionSpec = { backwardTransition(reduced) }
        )

        AnimatedVisibility(
            visible = nav.showsBottomBar,
            enter = if (reduced) {
                fadeIn(ChiaroMotion.fade())
            } else {
                slideInVertically(tween(NAV_TRANSITION_MS)) { it }
            },
            exit = if (reduced) {
                fadeOut(ChiaroMotion.fade())
            } else {
                slideOutVertically(tween(NAV_TRANSITION_MS)) { it }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            ChiaroBottomBar(selected = nav.selected, onSelect = nav::switchTab)
        }
    }
}

/*
 * The page transitions are Saldo's, value for value (the committente's request, 22 set
 * 2026): 300 ms, the incoming page sliding a sixth of the width and fading in, the
 * outgoing one sliding a sixth the other way and fading out. A tween and not one of
 * DESIGN §7's springs, because a predictive back SEEKS this transition with the finger,
 * and a seek needs a curve with a known length to put the finger's progress on.
 * Reduced motion collapses both to §7's 100 ms fade, like everything else that moves.
 */

/** Duration of the page and bottom-bar transitions. */
private const val NAV_TRANSITION_MS = 300

/** How far the incoming and outgoing pages slide, as a fraction (1/N) of their width. */
private const val SLIDE_DIVISOR = 6

/** Push: the incoming page slides in from the end and fades in. Also a tab switch away
 * from Today, or between two other tabs, which `NavDisplay` reads as a push. */
private fun forwardTransition(reduced: Boolean): ContentTransform {
    if (reduced) return fadeIn(ChiaroMotion.fade()) togetherWith fadeOut(ChiaroMotion.fade())
    val enter = fadeIn(tween(NAV_TRANSITION_MS, easing = FastOutSlowInEasing)) +
        slideInHorizontally(tween(NAV_TRANSITION_MS, easing = FastOutSlowInEasing)) { it / SLIDE_DIVISOR }
    val exit = fadeOut(tween(NAV_TRANSITION_MS, easing = FastOutSlowInEasing)) +
        slideOutHorizontally(tween(NAV_TRANSITION_MS, easing = FastOutSlowInEasing)) { -it / SLIDE_DIVISOR }
    return enter togetherWith exit
}

/** Pop, and the predictive back that seeks it: the reverse of [forwardTransition]. */
private fun backwardTransition(reduced: Boolean): ContentTransform {
    if (reduced) return fadeIn(ChiaroMotion.fade()) togetherWith fadeOut(ChiaroMotion.fade())
    val enter = fadeIn(tween(NAV_TRANSITION_MS, easing = FastOutSlowInEasing)) +
        slideInHorizontally(tween(NAV_TRANSITION_MS, easing = FastOutSlowInEasing)) { -it / SLIDE_DIVISOR }
    val exit = fadeOut(tween(NAV_TRANSITION_MS, easing = FastOutSlowInEasing)) +
        slideOutHorizontally(tween(NAV_TRANSITION_MS, easing = FastOutSlowInEasing)) { it / SLIDE_DIVISOR }
    return enter togetherWith exit
}

/**
 * The bottom bar of VISION §5.1, complete since Fase 7: Today, Sky, Alerts and the
 * Journal — every tab arrived with its screen, none ever shipped dead.
 */
@Composable
private fun ChiaroBottomBar(selected: ShellTab, onSelect: (ShellTab) -> Unit) {
    NavigationBar {
        TabItem(ShellTab.TODAY, selected, onSelect, ChiaroIcons.tabToday, R.string.tab_today)
        TabItem(ShellTab.SKY, selected, onSelect, ChiaroIcons.tabSky, R.string.tab_sky)
        TabItem(ShellTab.ALERTS, selected, onSelect, Icons.Outlined.Notifications, R.string.tab_alerts)
        TabItem(ShellTab.JOURNAL, selected, onSelect, Icons.Outlined.DateRange, R.string.tab_journal)
    }
}

@Composable
private fun RowScope.TabItem(
    tab: ShellTab,
    selected: ShellTab,
    onSelect: (ShellTab) -> Unit,
    icon: ImageVector,
    @StringRes label: Int
) {
    NavigationBarItem(
        selected = selected == tab,
        onClick = { onSelect(tab) },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        },
        label = { Text(stringResource(label)) }
    )
}
