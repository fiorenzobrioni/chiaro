package com.callbackdev.chiaro.ui.shell

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.callbackdev.chiaro.data.CityStore
import com.callbackdev.chiaro.data.FirstRun
import com.callbackdev.chiaro.data.ServiceLocator
import com.callbackdev.chiaro.data.WeatherRepository
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.ui.alerts.AlertsRoute
import com.callbackdev.chiaro.ui.firstrun.FirstRunRoute
import com.callbackdev.chiaro.ui.guide.GuideRoute
import com.callbackdev.chiaro.ui.icons.ChiaroIcons
import com.callbackdev.chiaro.ui.journal.JournalRoute
import com.callbackdev.chiaro.ui.settings.SettingsRoute
import com.callbackdev.chiaro.ui.sky.SkyRoute
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

    /** Skipping is an answer (VISION §5.8): it lands on the real "no place" state. */
    fun skip() {
        viewModelScope.launch { cityStore.markInitDone() }
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

/**
 * The two layers above the first-run gate (Fase 4, grown a bar in Fase 5): the
 * bottom-navigation tabs, and the screens the app bar reaches over them — Settings
 * from the gear, the guide from Settings or from the one-time card on Today. Plain
 * state rather than a nav graph: two tabs and two overlays do not earn one; Alerts
 * and the Journal (Fase 6–7) will re-pose the question.
 */
private enum class ShellTab { TODAY, SKY, ALERTS, JOURNAL }

private enum class ShellOverlay { SETTINGS, GUIDE }

@Composable
fun ChiaroRoot(shellViewModel: ShellViewModel = viewModel(factory = ShellViewModel.Factory)) {
    val firstRun by shellViewModel.firstRun.collectAsStateWithLifecycle()
    when (firstRun) {
        FirstRun.Unknown -> Surface(modifier = Modifier.fillMaxSize()) { }
        FirstRun.Pending -> FirstRunRoute(onSkip = shellViewModel::skip)
        FirstRun.Done -> MainScreens()
    }
}

@Composable
private fun MainScreens() {
    var tab by rememberSaveable { mutableStateOf(ShellTab.TODAY) }
    var overlay by rememberSaveable { mutableStateOf<ShellOverlay?>(null) }
    // The guide has two doors (VISION §5.7); back returns through the one it entered.
    var guideFromSettings by rememberSaveable { mutableStateOf(false) }

    // Back peels one layer: guide → its door, settings → the tabs, Sky → Today.
    BackHandler(enabled = overlay != null || tab != ShellTab.TODAY) {
        when {
            overlay == ShellOverlay.GUIDE ->
                overlay = if (guideFromSettings) ShellOverlay.SETTINGS else null
            overlay == ShellOverlay.SETTINGS -> overlay = null
            else -> tab = ShellTab.TODAY
        }
    }

    when (overlay) {
        ShellOverlay.SETTINGS -> SettingsRoute(
            onBack = { overlay = null },
            onOpenGuide = {
                guideFromSettings = true
                overlay = ShellOverlay.GUIDE
            }
        )
        ShellOverlay.GUIDE -> GuideRoute(
            onBack = { overlay = if (guideFromSettings) ShellOverlay.SETTINGS else null }
        )
        null -> TabScaffold(
            tab = tab,
            onSelectTab = { tab = it },
            onOpenSettings = { overlay = ShellOverlay.SETTINGS },
            onOpenGuide = {
                guideFromSettings = false
                overlay = ShellOverlay.GUIDE
            },
            onOpenJournal = { tab = ShellTab.JOURNAL }
        )
    }
}

/**
 * The bottom bar of VISION §5.1, complete since Fase 7: Today, Sky, Alerts and the
 * Journal — every tab arrived with its screen, none ever shipped dead. The tab content swaps; what must survive a switch
 * (the active place, the subscriptions) lives in the ViewModels, not in the screen.
 */
@Composable
private fun TabScaffold(
    tab: ShellTab,
    onSelectTab: (ShellTab) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenGuide: () -> Unit,
    onOpenJournal: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            when (tab) {
                ShellTab.TODAY -> TodayRoute(
                    onOpenSettings = onOpenSettings,
                    onOpenGuide = onOpenGuide,
                    onOpenJournal = onOpenJournal
                )
                ShellTab.SKY -> SkyRoute(onOpenSettings = onOpenSettings)
                ShellTab.ALERTS -> AlertsRoute(onOpenSettings = onOpenSettings)
                ShellTab.JOURNAL -> JournalRoute(onOpenSettings = onOpenSettings)
            }
        }
        NavigationBar {
            NavigationBarItem(
                selected = tab == ShellTab.TODAY,
                onClick = { onSelectTab(ShellTab.TODAY) },
                icon = {
                    Icon(
                        imageVector = ChiaroIcons.tabToday,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = { Text(stringResource(R.string.tab_today)) }
            )
            NavigationBarItem(
                selected = tab == ShellTab.SKY,
                onClick = { onSelectTab(ShellTab.SKY) },
                icon = {
                    Icon(
                        imageVector = ChiaroIcons.tabSky,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = { Text(stringResource(R.string.tab_sky)) }
            )
            NavigationBarItem(
                selected = tab == ShellTab.ALERTS,
                onClick = { onSelectTab(ShellTab.ALERTS) },
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = { Text(stringResource(R.string.tab_alerts)) }
            )
            NavigationBarItem(
                selected = tab == ShellTab.JOURNAL,
                onClick = { onSelectTab(ShellTab.JOURNAL) },
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = { Text(stringResource(R.string.tab_journal)) }
            )
        }
    }
}
