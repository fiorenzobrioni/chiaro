package com.callbackdev.chiaro.ui.alerts

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.callbackdev.chiaro.data.ActiveSource
import com.callbackdev.chiaro.data.CityStore
import com.callbackdev.chiaro.data.RuleStore
import com.callbackdev.chiaro.data.ServiceLocator
import com.callbackdev.chiaro.data.SettingsStore
import com.callbackdev.chiaro.data.WeatherRepository
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.rules.MaxRules
import com.callbackdev.chiaro.domain.rules.NotificationRule
import com.callbackdev.chiaro.domain.rules.RuleCheck
import com.callbackdev.chiaro.domain.rules.RuleEngine
import com.callbackdev.chiaro.domain.rules.RuleMessages
import com.callbackdev.chiaro.domain.settings.NotificationSettings
import com.callbackdev.chiaro.domain.settings.UnitSettings
import com.callbackdev.chiaro.sync.SyncScheduler
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One rule with what the screen says about it: the card of VISION §5.4. */
data class RuleCardModel(
    val rule: NotificationRule,
    /** When it last fired for the active place, from the history — null = not yet. */
    val lastFired: Instant?
)

/** What "try it now" answered (VISION §5.4: no notification is ever posted). */
sealed interface RulePreview {
    /** All conditions hold: the message as it would arrive. */
    data class WouldFire(val message: String) : RulePreview
    data object WouldPass : RulePreview

    /** A variable has no value right now (air quality down, empty window). */
    data class Unavailable(val variableId: String) : RulePreview
    data object NoData : RulePreview
}

sealed interface AlertsUiState {
    data object Starting : AlertsUiState

    data class Content(
        val placeName: String?,
        val notifications: NotificationSettings,
        val rules: List<RuleCardModel>,
        val canAdd: Boolean,
        val units: UnitSettings
    ) : AlertsUiState
}

/**
 * The Alerts screen's state machine (Fase 6). Every edit ends by reconciling the
 * periodic job: a switch the scheduler does not know about is a notification that
 * never comes — or a phone polling for nobody.
 */
class AlertsViewModel(
    private val appContext: Context,
    private val settingsStore: SettingsStore,
    private val ruleStore: RuleStore,
    private val cityStore: CityStore,
    private val repository: WeatherRepository
) : ViewModel() {

    val state: StateFlow<AlertsUiState> = combine(
        settingsStore.settings,
        ruleStore.rules,
        cityStore.activeSource
    ) { settings, rules, active ->
        val city = active.cityOrNull()
        val lastFired = city?.let { lastFiredByName(it) } ?: emptyMap()
        AlertsUiState.Content(
            placeName = city?.name,
            notifications = settings.notifications,
            rules = rules.map { RuleCardModel(it, lastFired[it.name]) },
            canAdd = rules.size < MaxRules,
            units = settings.units
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AlertsUiState.Starting)

    // ------------------------------------------------------------- ready-made

    fun setSevereWeather(enabled: Boolean) =
        mutate { settingsStore.setSevereWeatherAlerts(enabled) }

    fun setPrecipitationWarning(enabled: Boolean) =
        mutate { settingsStore.setPrecipitationWarning(enabled) }

    fun setDailySummary(enabled: Boolean) = mutate { settingsStore.setDailySummary(enabled) }

    // ------------------------------------------------------------- the reader's

    /** Creates the template's real rule, already on, and hands it to [onCreated]. */
    fun addFromTemplate(template: RuleText.Template, onCreated: (NotificationRule) -> Unit) {
        val res = appContext.resources
        viewModelScope.launch {
            val created = ruleStore.add(
                name = res.getString(template.nameRes),
                conditions = template.conditions,
                message = res.getString(template.messageRes)
            )
            SyncScheduler.reconcile(appContext)
            created?.let(onCreated)
        }
    }

    fun update(rule: NotificationRule) = mutate { ruleStore.update(rule) }

    fun remove(id: Long) = mutate { ruleStore.remove(id) }

    /**
     * "Try it now": the stateless check against the data the app already has, in the
     * city's own clock — what the rule WOULD do, with nothing posted (VISION §5.4).
     */
    suspend fun preview(rule: NotificationRule): RulePreview {
        val city = cityStore.activeSource.first().cityOrNull() ?: return RulePreview.NoData
        val report = repository.cachedReport(city) ?: return RulePreview.NoData
        val zone = city.timezone?.let { runCatching { ZoneId.of(it) }.getOrNull() }
            ?: ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone).toLocalDateTime()
        return when (val check = RuleEngine.check(rule, report, now)) {
            is RuleCheck.Fires -> RulePreview.WouldFire(
                RuleMessages.interpolate(
                    rule.message, rule, check.value, check.at, report, now,
                    settingsStore.settings.first().units
                )
            )
            RuleCheck.Passes -> RulePreview.WouldPass
            is RuleCheck.Unavailable -> RulePreview.Unavailable(check.variable)
        }
    }

    // ------------------------------------------------------------- plumbing

    private fun mutate(edit: suspend () -> Unit) {
        viewModelScope.launch {
            edit()
            runCatching { SyncScheduler.reconcile(appContext) }
        }
    }

    /**
     * When each rule last fired for [city], read off the history commits the worker
     * annotates ([WeatherRepository.recordFiredRules]). Keyed by name because that is
     * what the commit records; a renamed rule starts its history over, which is the
     * honest reading of what the data can say.
     */
    private suspend fun lastFiredByName(city: City): Map<String, Instant> {
        val rows = runCatching { repository.historyFor(city, limit = HISTORY_SCAN) }
            .getOrDefault(emptyList())
        val result = mutableMapOf<String, Instant>()
        rows.forEach { row ->
            repository.firedRules(row).forEach { name ->
                // Rows arrive newest first: the first sighting is the latest firing.
                result.getOrPut(name) { Instant.ofEpochSecond(row.timestampEpochSeconds) }
            }
        }
        return result
    }

    private fun ActiveSource.cityOrNull(): City? = when (this) {
        is ActiveSource.Saved -> city
        is ActiveSource.Gps -> lastFix
        ActiveSource.None -> null
    }

    companion object {
        private const val HISTORY_SCAN = 50

        val Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                AlertsViewModel(
                    appContext = app.applicationContext,
                    settingsStore = ServiceLocator.settingsStore(app),
                    ruleStore = ServiceLocator.ruleStore(app),
                    cityStore = ServiceLocator.cityStore(app),
                    repository = ServiceLocator.weatherRepository(app)
                )
            }
        }
    }
}
