package com.callbackdev.chiaro.ui.sky

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.callbackdev.chiaro.data.ActiveSource
import com.callbackdev.chiaro.data.CityStore
import com.callbackdev.chiaro.data.ServiceLocator
import com.callbackdev.chiaro.data.SettingsStore
import com.callbackdev.chiaro.data.SkySubscriptionStore
import com.callbackdev.chiaro.data.WeatherRepository
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.notifications.SkyAlarmScheduler
import java.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The Sky screen's state machine. Everything on screen is recomputed from the same
 * four sources — active place, cached report, subscriptions, settings — plus a
 * minute tick, because "in 40 minutes" and "passed" both move with the clock even
 * when no data does. A fetch on Today lands here within the tick: the report is
 * re-read on every rebuild, never held.
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
    private val clock: Clock = Clock.systemUTC()
) : ViewModel() {

    private val minuteTick = flow {
        while (true) {
            emit(Unit)
            kotlinx.coroutines.delay(60_000)
        }
    }

    val state: StateFlow<SkyUiState> = combine(
        cityStore.activeSource,
        subscriptionStore.subscriptions,
        settingsStore.settings,
        minuteTick
    ) { active, subscriptions, settings, _ ->
        val city = active.cityOrNull() ?: return@combine SkyUiState.NoPlace
        SkyStateBuilder.build(
            city = city,
            report = repository.cachedReport(city),
            subscriptions = subscriptions,
            settings = settings,
            now = clock.instant()
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SkyUiState.Starting)

    fun addMoment(jobId: String) = mutate { subscriptionStore.add(jobId) }

    fun removeMoment(jobId: String) = mutate { subscriptionStore.remove(jobId) }

    /**
     * This moment's own lead: null follows the default, zero is an explicit "never"
     * (the store keeps 0, [com.callbackdev.chiaro.domain.sky.SkyLead.ofMinutes] reads
     * it as off), any other value is its own reminder.
     */
    fun setLead(jobId: String, minutes: Int?) =
        mutate { subscriptionStore.setNotifyLead(jobId, minutes) }

    fun setDefaultLead(minutes: Int?) = mutate { settingsStore.setSkyNotifyDefault(minutes) }

    fun setNotifyOnFail(enabled: Boolean) = mutate { settingsStore.setSkyNotifyOnFail(enabled) }

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
                    subscriptionStore = ServiceLocator.skySubscriptionStore(app)
                )
            }
        }
    }
}
