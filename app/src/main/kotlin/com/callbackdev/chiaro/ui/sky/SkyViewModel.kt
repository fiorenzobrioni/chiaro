package com.callbackdev.chiaro.ui.sky

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.callbackdev.chiaro.data.SkySubscription
import com.callbackdev.chiaro.data.ActiveSource
import com.callbackdev.chiaro.data.CityStore
import com.callbackdev.chiaro.data.PowerSaveState
import com.callbackdev.chiaro.data.ServiceLocator
import com.callbackdev.chiaro.data.SettingsStore
import com.callbackdev.chiaro.data.SkySubscriptionStore
import com.callbackdev.chiaro.data.WeatherRepository
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.notifications.SkyAlarmScheduler
import com.callbackdev.chiaro.ui.today.ReportKeeper
import java.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The Sky screen's state machine. Everything on screen is recomputed from the same
 * four sources — active place, the place's report, subscriptions, settings — plus a
 * minute tick, because "in 40 minutes" and "passed" both move with the clock even
 * when no data does.
 *
 * The report and the tick are the [ReportKeeper]'s (28 Oct 2026, after 15.1): the cache
 * the moment the page is watched, then a silent fetch on Today's road once it is older
 * than the provider's quarter of an hour, never under battery saver. Until then the page
 * read the cache and never asked: a reader who opened the Sky without passing by Today
 * found the job's last report, up to two hours old, and past the stale bar every verdict
 * of the night was «?» for the age — when one fetch, the one Today would have made at the
 * same minute, would have let them speak. The verdicts still refuse a report past the bar
 * and the section still says the age ([SkyVerdictEngine], `DataNote`): what changed is that
 * the page no longer waits for somebody else to fetch.
 *
 * Every mutation ends by re-arming the reminder alarm: the plan and the screen must
 * never disagree about which line has a bell.
 */
class SkyViewModel(
    private val appContext: Context,
    private val repository: WeatherRepository,
    private val cityStore: CityStore,
    private val settingsStore: SettingsStore,
    private val subscriptionStore: SkySubscriptionStore,
    private val clock: Clock = Clock.systemUTC(),
    powerSave: PowerSaveState = PowerSaveState.Off
) : ViewModel() {

    private val keeper = ReportKeeper(repository, clock, powerSave)

    val state: StateFlow<SkyUiState> = combine(
        keeper.follow(cityStore.activeSource.map { it.cityOrNull() }),
        subscriptionStore.subscriptions,
        settingsStore.settings
    ) { kept, subscriptions, settings ->
        val place = kept ?: return@combine SkyUiState.NoPlace
        SkyStateBuilder.build(
            city = place.city,
            report = place.report,
            subscriptions = subscriptions,
            settings = settings,
            now = clock.instant()
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SkyUiState.Starting)

    fun addMoment(jobId: String) = mutate { subscriptionStore.add(jobId) }

    fun removeMoment(jobId: String) = mutate { subscriptionStore.remove(jobId) }

    /** «Annulla» after a removal: the line back as it was, its own lead included. */
    fun restoreMoment(line: SkySubscription) = mutate { subscriptionStore.restore(line) }

    /** The file whole: the catalog's way back to the four defaults, and its undo (20 ott 2026). */
    fun setMoments(lines: List<SkySubscription>) = mutate { subscriptionStore.replaceAll(lines) }

    /**
     * This moment's own lead: null follows the default, zero is an explicit "never"
     * (the store keeps 0, [com.callbackdev.chiaro.domain.sky.SkyLead.ofMinutes] reads
     * it as off), any other value is its own reminder.
     */
    fun setLead(jobId: String, minutes: Int?) =
        mutate { subscriptionStore.setNotifyLead(jobId, minutes) }

    /** «Behind the clouds» on the Tonight card (22 ott 2026): a look, not a plan, so no alarm follows it. */
    fun setBareSky(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setSkyBareSky(enabled) }
    }

    // The usual reminder and «even under a covered sky» are Settings' since 14 ott 2026
    // (review point 5): see SettingsViewModel.

    private fun mutate(edit: suspend () -> Unit) {
        viewModelScope.launch {
            edit()
            // The alarm follows the edit, not the other way around: a bell the plan
            // does not know about is a reminder that never comes.
            runCatching { SkyAlarmScheduler.reschedule(appContext) }
        }
    }

    private fun ActiveSource.cityOrNull(): City? = when (this) {
        is ActiveSource.Saved -> city
        is ActiveSource.Gps -> lastFix
        ActiveSource.None -> null
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                SkyViewModel(
                    appContext = app.applicationContext,
                    repository = ServiceLocator.weatherRepository(app),
                    cityStore = ServiceLocator.cityStore(app),
                    settingsStore = ServiceLocator.settingsStore(app),
                    subscriptionStore = ServiceLocator.skySubscriptionStore(app),
                    powerSave = PowerSaveState.of(app)
                )
            }
        }
    }
}
