package com.callbackdev.chiaro.ui.alerts

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.callbackdev.chiaro.data.ActiveSource
import com.callbackdev.chiaro.data.CityStore
import com.callbackdev.chiaro.data.FiredRule
import com.callbackdev.chiaro.data.PowerSaveState
import com.callbackdev.chiaro.data.RuleStateStore
import com.callbackdev.chiaro.data.RuleStore
import com.callbackdev.chiaro.data.ServiceLocator
import com.callbackdev.chiaro.data.SettingsStore
import com.callbackdev.chiaro.data.WeatherRepository
import com.callbackdev.chiaro.data.warnings.OfficialWarningReader
import com.callbackdev.chiaro.data.warnings.PlaceWarningState
import com.callbackdev.chiaro.domain.WeatherFreshness
import com.callbackdev.chiaro.domain.WeatherNow
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.model.WeatherReport
import com.callbackdev.chiaro.domain.placeZone
import com.callbackdev.chiaro.domain.rules.MaxRules
import com.callbackdev.chiaro.domain.rules.NotificationRule
import com.callbackdev.chiaro.domain.rules.RuleCheck
import com.callbackdev.chiaro.domain.rules.RuleEngine
import com.callbackdev.chiaro.domain.rules.RuleMessages
import com.callbackdev.chiaro.domain.rules.RuleWindows
import com.callbackdev.chiaro.domain.settings.NotificationSettings
import com.callbackdev.chiaro.domain.settings.UnitSettings
import com.callbackdev.chiaro.domain.sky.AstronomyEngine
import com.callbackdev.chiaro.domain.warnings.WarningLevel
import com.callbackdev.chiaro.sync.SyncScheduler
import com.callbackdev.chiaro.ui.today.KeptReport
import com.callbackdev.chiaro.ui.today.ReportKeeper
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One rule with what the screen says about it: the card of VISION §5.4. */
data class RuleCardModel(
    val rule: NotificationRule,
    /** When it last fired for the active place, from the history — null = not yet. */
    val lastFired: Instant?,
    /**
     * Whether it holds right now, with the number that decides (the Alerts review, point 11,
     * 14 ott 2026): the editor's dry run, on the card. Null with no forecast for the place,
     * where the card says nothing rather than a guess.
     */
    val now: RulePreview? = null
)

/** What "try it now" answered (VISION §5.4: no notification is ever posted). */
sealed interface RulePreview {
    /** All conditions hold: the message as it would arrive. */
    data class WouldFire(val message: String) : RulePreview

    /** Not all of it holds: [failed] is the first condition that does not, [value] what it
     * read ([RuleCheck.Passes]). */
    data class WouldPass(
        val failed: com.callbackdev.chiaro.domain.rules.RuleCondition? = null,
        val value: Double? = null
    ) : RulePreview

    /**
     * All conditions hold, but the rule's own hours are closed (26 set 2026): it would not
     * speak now, and [next] is when it could — null when there is no hour to promise (the
     * polar night). [today] is the place's date, to say «dalle» or «da domani alle».
     */
    data class OutsideHours(
        val message: String,
        val next: java.time.LocalDateTime?,
        val today: java.time.LocalDate
    ) : RulePreview

    /** A variable has no value right now (air quality down, empty window). */
    data class Unavailable(val variableId: String) : RulePreview
    data object NoData : RulePreview
}

sealed interface AlertsUiState {
    data object Starting : AlertsUiState

    /**
     * No active place (15.2, 28 Oct 2026): the page draws the empty state Today and the Sky
     * draw, and nothing else. Until then it drew the whole page, with «disponibili per i
     * luoghi in Italia» as the head's answer about nowhere and the bulletin's hours under it.
     * The state is reached: removing the last place empties the active one.
     */
    data object NoPlace : AlertsUiState

    data class Content(
        val placeName: String?,
        val notifications: NotificationSettings,
        val rules: List<RuleCardModel>,
        val canAdd: Boolean,
        val units: UnitSettings,
        /** The active place's zone, for "last fired" — every other hour in the app is the
         * place's, and this one was the phone's (review, 8 set 2026). */
        val zone: ZoneId = ZoneId.systemDefault(),
        /**
         * The official warnings for the active place (Fase 11). Never null and never
         * hidden: this is the one screen where "there is no warning" is the answer the
         * reader came for, so all four states are drawn — which is the opposite of what
         * Today does with the same value (DESIGN §1.1).
         */
        val warnings: PlaceWarningState = PlaceWarningState.Unavailable,
        /**
         * Today's sunrise and sunset at the active place, on its clock (26 set 2026): what
         * «Con la luce» means today, said beside the choice. Null with no forecast yet or
         * on a day the sun does not both rise and set — the line then says the rule only.
         */
        val daylightToday: Pair<LocalTime, LocalTime>? = null,
        /**
         * When the report the cards are read against was fetched (15.1, 28 Oct 2026), and
         * whether that is past the stale bar of [WeatherFreshness]: the «I tuoi» title then
         * says the age in the freshness chip's words, because a card that reads «Adesso no ·
         * è 12°» off a fetch of this morning is answering about this morning, and the page
         * never said so (§1.1). Null with no report.
         */
        val lastSync: Instant? = null,
        val isStale: Boolean = false
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
    private val ruleStateStore: RuleStateStore,
    private val cityStore: CityStore,
    private val repository: WeatherRepository,
    private val warningReader: OfficialWarningReader?,
    private val clock: Clock = Clock.systemUTC(),
    private val powerSave: PowerSaveState = PowerSaveState.Off
) : ViewModel() {

    /**
     * The active place's report as this page keeps it (15.1, 28 Oct 2026), on Today's road:
     * the cache the moment the page is watched, then whatever the silent refreshes bring,
     * and the minute tick with it ([ReportKeeper], shared with the Sky since the same day).
     * Raw, as fetched: what the cards read is [WeatherNow.prepare] of it, at the minute of
     * reading.
     */
    private val keeper = ReportKeeper(repository, clock, powerSave)

    /** The last report the keeper handed [state], for the editor's dry run ([preview]). */
    @Volatile
    private var latest: KeptReport? = null

    private class Page(
        val city: City?,
        val updateFrequencyMin: Int,
        /** Null with no active place: the page is then [AlertsUiState.NoPlace]. */
        val content: AlertsUiState.Content?
    )

    /** The page's own state, from the stores: what the report adds is [state]'s. */
    private val page = combine(
        settingsStore.settings,
        ruleStore.rules,
        cityStore.activeSource,
        warningReader?.bulletin ?: flowOf(null)
    ) { settings, rules, active, bulletin ->
        val city = active.cityOrNull() ?: return@combine Page(null, settings.updateFrequencyMin, null)
        val lastFired = lastFired(city, rules)
        Page(
            city,
            settings.updateFrequencyMin,
            AlertsUiState.Content(
                placeName = city.name,
                notifications = settings.notifications,
                rules = rules.map { RuleCardModel(it, lastFired[it.id]) },
                // The place's own, until the report says better (its zone is the model's).
                zone = placeZone(null, city),
                canAdd = rules.size < MaxRules,
                units = settings.units,
                // The zone lookup decodes a 290 KB asset the first time and runs on
                // Dispatchers.Default with the rest of this block; a place outside Italy
                // costs one bounding-box test and answers Unavailable.
                warnings = warningReader?.let { reader ->
                    runCatching {
                        reader.state(reader.zoneOf(city), bulletin, reader.today())
                    }.getOrDefault(PlaceWarningState.Unavailable)
                } ?: PlaceWarningState.Unavailable
            )
        )
    }

    /**
     * Each rule read against the forecast the phone already has, again every minute the page
     * is watched (the Alerts review, point 11): `next_6h` slides with the clock and a band of
     * hours opens and closes, so an answer read once would go stale on screen. Only the
     * reading repeats, which is arithmetic on a cached report; the stores and the history are
     * read when they change, as before. Nothing runs once the page is gone (`WhileSubscribed`).
     *
     * Read on the report PREPARED for now (15.1, 28 Oct 2026), as Today reads it: the
     * forecast for this minute in place of a `current` block over an hour old, the hours and
     * the days gone dropped, so that «oggi» is today's row and «adesso» is now's number. Until
     * then the cards read the raw cache, and said the opposite of Today's agenda on the same
     * rule: «Adesso sì» on yesterday's high, «è 19,7°» off a block three hours old. A report
     * with nothing left about the present reads as none: the card says nothing.
     */
    val state: StateFlow<AlertsUiState> = combine(
        page,
        keeper.follow(cityStore.activeSource.map { it.cityOrNull() })
    ) { page, kept ->
        val now = clock.instant()
        val content = page.content ?: return@combine AlertsUiState.NoPlace
        val city = checkNotNull(page.city)
        latest = kept
        // A report of another place, between one place and the next: none, for a tick.
        val report = kept?.takeIf { it.city.cacheKey == city.cacheKey }?.report
        val zone = placeZone(report, city)
        if (report == null) return@combine content.copy(zone = zone)
        val read = WeatherNow.prepare(report, now)
        val local = now.atZone(zone).toLocalDateTime()
        content.copy(
            zone = zone,
            daylightToday = daylightToday(report.location.coordinates, zone),
            rules = content.rules.map { card ->
                card.copy(
                    now = read?.let { ruleReading(card.rule, it, zone, local, content.units, appContext) }
                )
            },
            lastSync = report.systemInfo.lastSync,
            isStale = WeatherFreshness.isStale(report.systemInfo.lastSync, page.updateFrequencyMin, now)
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

    fun setEveningSummary(enabled: Boolean) =
        mutate { settingsStore.setEveningSummary(enabled) }

    // The official warnings (Fase 11). They live here and not in Settings for Fase 6's
    // reason: a switch belongs next to what it governs.
    fun setOfficialWarnings(enabled: Boolean) =
        mutate { settingsStore.setOfficialWarnings(enabled) }

    fun setOfficialWarningsFrom(level: WarningLevel) =
        mutate { settingsStore.setOfficialWarningsFrom(level) }

    // ------------------------------------------------------------- the reader's

    /** Creates the template's real rule, already on, and hands it to [onCreated]. The name
     * is the idea's, numbered when a rule already has it (the blank idea, a second «Bici»). */
    fun addFromTemplate(template: RuleText.Template, onCreated: (NotificationRule) -> Unit) {
        val res = appContext.resources
        viewModelScope.launch {
            val created = ruleStore.add(
                name = RuleText.freeName(
                    res.getString(template.nameRes), ruleStore.rules.first().map { it.name }
                ),
                conditions = template.conditions,
                message = res.getString(template.messageRes),
                window = template.window
            )
            SyncScheduler.reconcile(appContext)
            created?.let(onCreated)
        }
    }

    /**
     * Saves [rule], and re-arms it when what it asks changed (14 ott 2026) or when it is
     * switched back on (15.4, 28 Oct 2026; [rearms]). A latched rule given a new threshold
     * stayed latched under the old one, silent until the old condition happened to read
     * false; a rule that had spoken today under its old window kept today's fingerprint and
     * could not speak under the new one. `RuleStateStore.clearRule` was written for exactly
     * this and never called. A rename leaves the state alone: nothing it reads moved.
     *
     * A deletion does not clear here, because «Annulla» brings the rule back as it
     * was; the latches of a rule that stays deleted leave at the next sync, with
     * those of removed places (`RuleStateStore.retain`).
     */
    fun update(rule: NotificationRule) = mutate {
        val before = ruleStore.rules.first().firstOrNull { it.id == rule.id }
        ruleStore.update(rule)
        if (before != null && rearms(before, rule)) {
            ruleStateStore.clearRule(rule.id)
        }
    }

    fun remove(id: Long) = mutate { ruleStore.remove(id) }

    /** The «Annulla» of a deletion: [rule] back at [index], as it was ([RuleStore.restore]). */
    fun restore(rule: NotificationRule, index: Int) = mutate { ruleStore.restore(rule, index) }

    /**
     * "Try it now": the stateless check against the data the app already has, in the
     * city's own clock — what the rule WOULD do, with nothing posted (VISION §5.4). On the
     * report the page keeps, prepared for this minute, so the editor's answer is the card's.
     */
    suspend fun preview(rule: NotificationRule): RulePreview {
        val city = cityStore.activeSource.first().cityOrNull() ?: return RulePreview.NoData
        val now = clock.instant()
        val raw = latest?.takeIf { it.city.cacheKey == city.cacheKey }?.report
            ?: repository.cachedReport(city)
            ?: return RulePreview.NoData
        val report = WeatherNow.prepare(raw, now) ?: return RulePreview.NoData
        val zone = placeZone(report, city)
        return ruleReading(
            rule, report, zone, now.atZone(zone).toLocalDateTime(), settingsStore.settings.first().units, appContext
        )
    }

    private fun daylightToday(coords: Coordinates, zone: ZoneId): Pair<LocalTime, LocalTime>? {
        val today = clock.instant().atZone(zone).toLocalDate()
        fun crossing(rising: Boolean) = AstronomyEngine.sunCrossing(
            today, zone, coords, AstronomyEngine.SUNRISE_ALTITUDE, rising
        )?.atZone(zone)?.toLocalTime()
        val rise = crossing(rising = true) ?: return null
        val set = crossing(rising = false) ?: return null
        return rise to set
    }

    // ------------------------------------------------------------- plumbing

    private fun mutate(edit: suspend () -> Unit) {
        viewModelScope.launch {
            edit()
            runCatching { SyncScheduler.reconcile(appContext) }
        }
    }

    /**
     * When each of [rules] last fired for [city], by rule id, read off the history commits
     * the worker annotates ([WeatherRepository.recordFiredRules]). A commit names a rule by
     * id and name since 15.4 (28 Oct 2026) and is read by the id, so a renamed rule keeps its
     * past; the commits written before carry the name alone and are read by it
     * ([FiredRule.isFor]), until retention takes them. Until 15.4 the reading was by name,
     * and two rules with one name shared one past.
     */
    private suspend fun lastFired(city: City, rules: List<NotificationRule>): Map<Long, Instant> {
        val rows = runCatching { repository.historyFor(city) }
            .getOrDefault(emptyList())
        val result = mutableMapOf<Long, Instant>()
        rows.forEach { row ->
            val fired = repository.firedRules(row)
            if (fired.isEmpty()) return@forEach
            // Rows arrive newest first: the first sighting is the latest firing.
            val at = Instant.ofEpochSecond(row.timestampEpochSeconds)
            rules.forEach { rule ->
                if (rule.id !in result && fired.any { it.isFor(rule) }) result[rule.id] = at
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
        /**
         * Whether saving [after] over [before] must clear the rule's latch and fingerprints
         * ([RuleStateStore.clearRule]): its conditions or its hours changed (14 ott 2026), or
         * it is switched back on (15.4, 28 Oct 2026). The worker evaluates the rules that are
         * on (`filter { it.enabled }`), so a rule switched off is never read false again and
         * its latch never discharges: «Temperatura adesso ≥ 20» true and fired, switched off,
         * the temperature down and up again, switched on — silent until the next false. The
         * switch off itself clears nothing: a rule at rest has nothing to re-arm, and the
         * state goes the moment it matters, when the rule is read again.
         */
        internal fun rearms(before: NotificationRule, after: NotificationRule): Boolean =
            before.conditions != after.conditions ||
                before.window != after.window ||
                (!before.enabled && after.enabled)

        val Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                AlertsViewModel(
                    appContext = app.applicationContext,
                    settingsStore = ServiceLocator.settingsStore(app),
                    ruleStore = ServiceLocator.ruleStore(app),
                    ruleStateStore = ServiceLocator.ruleStateStore(app),
                    cityStore = ServiceLocator.cityStore(app),
                    repository = ServiceLocator.weatherRepository(app),
                    warningReader = ServiceLocator.warningReader(app),
                    powerSave = PowerSaveState.of(app)
                )
            }
        }
    }
}

/**
 * What [rule] would do at [now], with nothing posted: the dry run, for the editor and for each
 * card (the Alerts review, points 11 and 12). Its message is written as it would arrive, in the
 * reader's units and on the phone's clock ([RuleText.MessageWriter]). [report] is the one
 * prepared for now ([WeatherNow.prepare]): the caller's duty, so that this reads what Today reads.
 */
internal fun ruleReading(
    rule: NotificationRule,
    report: WeatherReport,
    zone: ZoneId,
    now: java.time.LocalDateTime,
    units: UnitSettings,
    context: Context
): RulePreview =
    when (val check = RuleEngine.check(rule, report, now)) {
        is RuleCheck.Fires -> {
            val message = RuleMessages.interpolate(
                rule.message, rule, check.value, check.at, report, now, units,
                RuleText.MessageWriter(
                    context.resources, units,
                    context.resources.configuration.locales[0],
                    android.text.format.DateFormat.is24HourFormat(context)
                )
            )
            val coords = report.location.coordinates
            if (RuleWindows.isOpen(rule.window, now, zone, coords)) {
                RulePreview.WouldFire(message)
            } else {
                RulePreview.OutsideHours(
                    message, RuleWindows.nextOpening(rule.window, now, zone, coords), now.toLocalDate()
                )
            }
        }
        is RuleCheck.Passes -> RulePreview.WouldPass(check.failed, check.value)
        is RuleCheck.Unavailable -> RulePreview.Unavailable(check.variable)
    }
