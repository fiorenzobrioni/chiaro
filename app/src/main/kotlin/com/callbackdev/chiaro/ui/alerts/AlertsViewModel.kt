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
import com.callbackdev.chiaro.domain.WeatherException
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
import com.callbackdev.chiaro.domain.rules.RulePlace
import com.callbackdev.chiaro.domain.rules.RulePlaces
import com.callbackdev.chiaro.domain.rules.RuleWindows
import com.callbackdev.chiaro.domain.settings.NotificationSettings
import com.callbackdev.chiaro.domain.settings.UnitSettings
import com.callbackdev.chiaro.domain.sky.AstronomyEngine
import com.callbackdev.chiaro.domain.warnings.WarningLevel
import com.callbackdev.chiaro.sync.SyncScheduler
import com.callbackdev.chiaro.ui.today.KeptReport
import com.callbackdev.chiaro.ui.today.RefreshBackoff
import com.callbackdev.chiaro.ui.today.ReportKeeper
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One rule with what the screen says about it: the card of VISION §5.4. */
data class RuleCardModel(
    val rule: NotificationRule,
    /** When it last fired where it is read (the active place, or its own since 15.12), from
     * that place's history — null = not yet. */
    val lastFired: Instant?,
    /**
     * Whether it holds right now, with the number that decides (the Alerts review, point 11,
     * 14 ott 2026): the editor's dry run, on the card. Read where the rule is read: the active
     * place's report, or the cache of the saved place it is bound to (15.12). Null with no
     * forecast for that place, where the card says nothing rather than a guess.
     */
    val now: RulePreview? = null,
    /** Where it is read, when it is bound to a saved place (15.11–15.12): the card says it on a
     * line of its own. Null for a rule that follows the active place. */
    val place: RuleCardPlace? = null
) {
    /**
     * The rule is bound to a saved place the reader has removed (15.11, 29 Oct 2026;
     * [RulePlaces.isOrphan]): it is read nowhere, the job switches it off at its next run, and
     * the card says the place is gone, from the moment it is, not from that run.
     */
    val placeGone: Boolean get() = place is RuleCardPlace.Gone

    /** Whether it can ring: on, and its place still there. What the page promises for it. */
    val canRing: Boolean get() = rule.enabled && !placeGone
}

/** The saved place a rule is bound to, as its card says it (15.12, 29 Oct 2026). */
sealed interface RuleCardPlace {
    val name: String

    /**
     * Still saved, named by its record. [staleSince] is when its report was fetched, once that
     * is past the stale bar (the page's own title says the active place's age, never this
     * one's); [noForecast] when the phone has no forecast for it yet: bound a minute ago, or
     * never opened, until the job's next run or the editor fetches it.
     */
    data class Saved(
        override val name: String,
        val staleSince: Instant? = null,
        val noForecast: Boolean = false
    ) : RuleCardPlace

    /** No longer saved: [name] is the one the rule kept when it was chosen. */
    data class Gone(override val name: String) : RuleCardPlace
}

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

    /**
     * All conditions hold and the rule has no hours of its own, but it is a fact about a day,
     * which the engine says from a set hour (15.9, 29 Oct 2026; `RuleEngine.speaksFrom`): a rule
     * on tomorrow from noon, one on today from six. [at] is that hour today. «Adesso sì» alone
     * at nine, on «Neve domani», promised a voice that comes at noon.
     */
    data class NotYet(val message: String, val at: java.time.LocalDateTime) : RulePreview

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
        val isStale: Boolean = false,
        /** The saved places, in the reader's order (15.12): where a rule can be bound. Never
         * the position, which is the active place by definition. */
        val savedPlaces: List<City> = emptyList()
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

    /** Bumped when the editor has fetched a bound place ([boundReport]): the cards read its
     * cache again at once, not at the next minute. */
    private val boundFetched = MutableStateFlow(0)

    /** The editor's waits between failed fetches, one per bound place. */
    private val boundBackoffs = java.util.concurrent.ConcurrentHashMap<Long, RefreshBackoff>()

    private class Page(
        val city: City?,
        /** The saved list, for the rules bound to one of its places. */
        val saved: List<City>,
        val updateFrequencyMin: Int,
        /** Null with no active place: the page is then [AlertsUiState.NoPlace]. */
        val content: AlertsUiState.Content?
    )

    /** The page's own state, from the stores: what the report adds is [state]'s. */
    private val page = combine(
        settingsStore.settings,
        ruleStore.rules,
        cityStore.activeSource,
        cityStore.cities,
        warningReader?.bulletin ?: flowOf(null)
    ) { settings, rules, active, saved, bulletin ->
        val city = active.cityOrNull() ?: return@combine Page(null, saved, settings.updateFrequencyMin, null)
        val lastFired = lastFired(city, saved, rules)
        Page(
            city,
            saved,
            settings.updateFrequencyMin,
            AlertsUiState.Content(
                placeName = city.name,
                notifications = settings.notifications,
                rules = rules.map { RuleCardModel(it, lastFired[it.id], place = cardPlace(it, saved)) },
                savedPlaces = saved,
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
        keeper.follow(cityStore.activeSource.map { it.cityOrNull() }),
        boundFetched
    ) { page, kept, _ ->
        val now = clock.instant()
        val content = page.content ?: return@combine AlertsUiState.NoPlace
        val city = checkNotNull(page.city)
        latest = kept
        // A report of another place, between one place and the next: none, for a tick.
        val report = kept?.takeIf { it.city.cacheKey == city.cacheKey }?.report
        val zone = placeZone(report, city)
        val read = report?.let { WeatherNow.prepare(it, now) }
        val local = now.atZone(zone).toLocalDateTime()
        val rules = content.rules.map { card ->
            val bound = card.rule.place
            when {
                card.placeGone -> card
                RulePlaces.readsActive(card.rule, city) -> card.copy(
                    now = read?.let { ruleReading(card.rule, it, zone, local, content.units, appContext) }
                )
                // Bound to another saved place (15.12): read on that place's cache, at the
                // same minute, in that place's zone.
                else -> readElsewhere(card, page.saved.first { it.id == bound?.id }, now, page.updateFrequencyMin, content.units)
            }
        }
        if (report == null) return@combine content.copy(zone = zone, rules = rules)
        content.copy(
            zone = zone,
            daylightToday = daylightToday(report.location.coordinates, zone),
            rules = rules,
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

    /**
     * Binds [rule] to [place], or to the active place with null (15.12, 29 Oct 2026). Read from
     * the store, so nothing typed in the editor and not yet saved is written over. A rule whose
     * place was gone is switched back on: the job switched it off for the place, and the place
     * is what the reader has just given back — from the card's switch, the very gesture that
     * asked for it on, or from the editor's «Dove». Re-armed like any change of what it reads
     * ([rearms]): a latch filed under the old place would hold it silent under the new one.
     */
    fun choosePlace(rule: NotificationRule, place: City?) = mutate {
        val before = ruleStore.rules.first().firstOrNull { it.id == rule.id } ?: return@mutate
        val saved = cityStore.cities.first().map { it.id }.toSet()
        val after = before.copy(
            place = place?.let(RulePlace::of),
            enabled = before.enabled || RulePlaces.isOrphan(before, saved)
        )
        ruleStore.update(after)
        if (rearms(before, after)) ruleStateStore.clearRule(rule.id)
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
        val bound = rule.place
        if (bound != null && !RulePlaces.readsActive(rule, city)) {
            // On the place it is bound to (15.12): its cache, fetched here when missing or old,
            // since the editor is where somebody is asking. A place no longer saved has none.
            val place = cityStore.cities.first().firstOrNull { it.id == bound.id } ?: return RulePreview.NoData
            val report = boundReport(place, now)?.let { WeatherNow.prepare(it, now) } ?: return RulePreview.NoData
            val zone = placeZone(report, place)
            return ruleReading(
                rule, report, zone, now.atZone(zone).toLocalDateTime(), settingsStore.settings.first().units, appContext
            )
        }
        val raw = latest?.takeIf { it.city.cacheKey == city.cacheKey }?.report
            ?: repository.cachedReport(city)
            ?: return RulePreview.NoData
        val report = WeatherNow.prepare(raw, now) ?: return RulePreview.NoData
        val zone = placeZone(report, city)
        return ruleReading(
            rule, report, zone, now.atZone(zone).toLocalDateTime(), settingsStore.settings.first().units, appContext
        )
    }

    /**
     * [card], bound to [place], read on the cache of that place (15.12): the rule's answer and
     * the place line's state, its age once past the stale bar, or no forecast yet. Never a
     * fetch: the job keeps a bound place fresh while its rule is on, and the editor fetches when
     * somebody asks ([boundReport]). A report too old to say anything about now says its age.
     */
    private suspend fun readElsewhere(
        card: RuleCardModel,
        place: City,
        now: Instant,
        updateFrequencyMin: Int,
        units: UnitSettings
    ): RuleCardModel {
        val raw = runCatching { repository.cachedReport(place) }.getOrNull()
        val read = raw?.let { WeatherNow.prepare(it, now) }
        val zone = placeZone(raw, place)
        return card.copy(
            place = RuleCardPlace.Saved(
                name = place.name,
                staleSince = raw?.systemInfo?.lastSync?.takeIf { WeatherFreshness.isStale(it, updateFrequencyMin, now) },
                noForecast = raw == null
            ),
            now = read?.let { ruleReading(card.rule, it, zone, now.atZone(zone).toLocalDateTime(), units, appContext) }
        )
    }

    /**
     * The report of a place a rule is bound to, for the editor's answer (15.12): the cache, and
     * a silent fetch when there is none or it is past the provider's quarter of an hour, on the
     * keeper's terms ([ReportKeeper]): never under battery saver unless there is nothing at all,
     * and [RefreshBackoff] between failures, since the editor asks every minute. The fetch is
     * the place's ordinary one, 8.4 calls, and a new one lands on the cards at once.
     */
    private suspend fun boundReport(place: City, now: Instant): WeatherReport? {
        val cached = runCatching { repository.cachedReport(place) }.getOrNull()
        val age = cached?.let { Duration.between(it.systemInfo.lastSync, now) }
        if (age != null && age < WeatherFreshness.ProviderResolution) return cached
        if (cached != null && powerSave.isOn()) return cached
        val backoff = boundBackoffs.getOrPut(place.id) { RefreshBackoff() }
        if (!backoff.isDue(now)) return cached
        backoff.attempting(now)
        return try {
            repository.getWeather(place).also {
                backoff.succeeded()
                boundFetched.update { it + 1 }
            }
        } catch (_: WeatherException) {
            backoff.failed()
            cached
        }
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
    private suspend fun lastFired(city: City, saved: List<City>, rules: List<NotificationRule>): Map<Long, Instant> =
        // Each rule on the history of the place it is read for (15.12): the active place, or
        // the saved place it is bound to, whose commits the job annotates since 15.11. A rule
        // whose place is gone has no place to read.
        rules.groupBy { rule ->
            if (RulePlaces.readsActive(rule, city)) city else saved.firstOrNull { it.id == rule.place?.id }
        }.flatMap { (place, ofPlace) ->
            place?.let { lastFiredAt(it, ofPlace) }.orEmpty().toList()
        }.toMap()

    /** [lastFired] for the [rules] read on [city]. */
    private suspend fun lastFiredAt(city: City, rules: List<NotificationRule>): Map<Long, Instant> {
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
                before.place?.id != after.place?.id ||
                (!before.enabled && after.enabled)

        /** What the card says about where [rule] is read (15.12): nothing for a rule that
         * follows the active place, the saved record's name, or the name it kept once gone. */
        internal fun cardPlace(rule: NotificationRule, saved: List<City>): RuleCardPlace? =
            rule.place?.let { bound ->
                saved.firstOrNull { it.id == bound.id }
                    ?.let { RuleCardPlace.Saved(it.name) }
                    ?: RuleCardPlace.Gone(bound.name)
            }

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
            val from = RuleEngine.speaksFrom(rule)
            when {
                !RuleWindows.isOpen(rule.window, now, zone, coords) -> RulePreview.OutsideHours(
                    message, RuleWindows.nextOpening(rule.window, now, zone, coords), now.toLocalDate()
                )
                from != null && now.toLocalTime() < from ->
                    RulePreview.NotYet(message, now.toLocalDate().atTime(from))
                else -> RulePreview.WouldFire(message)
            }
        }
        is RuleCheck.Passes -> RulePreview.WouldPass(check.failed, check.value)
        is RuleCheck.Unavailable -> RulePreview.Unavailable(check.variable)
    }
