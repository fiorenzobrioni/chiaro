package com.callbackdev.chiaro.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.model.FixAdoptionMeters
import com.callbackdev.chiaro.domain.model.GeoFix
import com.callbackdev.chiaro.domain.model.GpsCityId
import com.callbackdev.chiaro.domain.model.distanceMetersTo
import com.callbackdev.chiaro.domain.model.toGpsCity
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.citiesDataStore by preferencesDataStore(name = "cities")

/**
 * The GPS toggle's persisted state, the last fix it produced, and WHEN that fix was
 * taken. The instant is what makes a cold start cheap (Fase 3b): the interval used to
 * be counted in a ViewModel field, which was null again at every process start, so
 * every launch counted as due and every launch acquired a position.
 */
data class LocationSettings(
    val useGps: Boolean,
    val gpsCity: City?,
    val fixedAt: Instant?
)

/** What the main screen shows weather for: a saved city, the device position, or
 * nothing at all. */
sealed interface ActiveSource {
    data class Saved(val city: City) : ActiveSource

    /** [lastFix] is the last persisted GPS pseudo-city; null until the first fix. */
    data class Gps(val lastFix: City?) : ActiveSource

    /**
     * No location configured: a fresh install that has not answered the first-run
     * screen yet, or one whose last saved place was removed with GPS off. Inherited
     * from tweather's Fase 14b, where this state first became representable — before
     * it, an empty list fell back to a seeded Milan the user had never chosen.
     */
    data object None : ActiveSource
}

/** What the shell must know before it can draw anything — see [CityStore.firstRun]. */
enum class FirstRun {
    /** The legacy check has not run yet in this process: draw nothing — flashing
     * the first-run screen at a long-time user would be answering for them. */
    Unknown,

    /** The place question still owes an answer. */
    Pending,

    /**
     * The place is answered and the notification question is not (21 set 2026). It is
     * a step of its own and not a line on the first screen, because it can only be
     * asked honestly AFTER there is a place for an alert to be about; and it exists at
     * all because four ready-made alerts ship switched ON, so an install that is never
     * asked carries four promises the phone cannot keep.
     *
     * Skipping the place skips this too, and so does an upgrade ([migrateFirstRun]):
     * neither is a fresh reader standing in front of the first screen.
     */
    Notifications,

    /** Answered — with a city, with GPS, or by skipping — or inherited by an upgrade. */
    Done
}

/**
 * Persists the saved-places list and the active place as DataStore preferences: the
 * list as a JSON array, the selection as the city id. An empty list is a real state
 * ([ActiveSource.None]): the store never invents [DefaultCity] to have something to
 * show.
 */
class CityStore(
    private val dataStore: DataStore<Preferences>,
    private val json: Json
) {

    val cities: Flow<List<City>> = dataStore.data.map(::decode).distinctUntilChanged()

    val locationSettings: Flow<LocationSettings> = dataStore.data
        .map { prefs ->
            LocationSettings(
                useGps = prefs[UseGps] ?: false,
                gpsCity = decodeGpsCity(prefs),
                fixedAt = prefs[GpsFixedAt]?.let(Instant::ofEpochMilli)
            )
        }
        .distinctUntilChanged()

    /** GPS is the source only while enabled AND selected (sentinel [GpsCityId]). */
    val activeSource: Flow<ActiveSource> = dataStore.data
        .map { prefs ->
            if (prefs[UseGps] == true && prefs[ActiveCityId] == GpsCityId) {
                ActiveSource.Gps(decodeGpsCity(prefs))
            } else {
                val cities = decode(prefs)
                val city = cities.firstOrNull { it.id == prefs[ActiveCityId] }
                    ?: cities.firstOrNull()
                city?.let { ActiveSource.Saved(it) } ?: ActiveSource.None
            }
        }
        .distinctUntilChanged()

    /**
     * Whether the first-run screen still owes an answer. [FirstRun.Unknown] until
     * [migrateFirstRun] has run in this install: the shell must not flash first-run
     * at someone who has been using the app for months.
     */
    val firstRun: Flow<FirstRun> = dataStore.data
        .map { prefs ->
            when {
                prefs[Migrated] != true -> FirstRun.Unknown
                prefs[InitDone] != true -> FirstRun.Pending
                prefs[NotifyAsked] != true -> FirstRun.Notifications
                else -> FirstRun.Done
            }
        }
        .distinctUntilChanged()

    /**
     * Decides once per install whether it predates the empty state, and never runs
     * again. [hasHistory] — any fetch ever recorded — is what tells a used install
     * from a fresh one: someone who never touched the place list has nothing in this
     * store either, but has been watching the seeded Milan since the day they
     * installed, and an update must not take it away. Such an install has the seed
     * written for real (it was a fallback, never a stored value) and skips first-run.
     * A genuinely fresh install writes nothing but the marker.
     */
    suspend fun migrateFirstRun(hasHistory: Boolean) {
        dataStore.edit { prefs ->
            if (prefs[Migrated] == true) {
                // An install that was migrated BEFORE the notification step existed
                // (21 set 2026) has answered the place question already, so the new
                // step is not its to be stopped by — without this it would meet a
                // full-screen question at the next launch, which is the very thing
                // this check was written to prevent. Its road to the permission is
                // the card Avvisi draws while the promise is unkept (DESIGN §8.14).
                if (prefs[InitDone] == true && prefs[NotifyAsked] == null) {
                    prefs[NotifyAsked] = true
                }
                return@edit
            }
            prefs[Migrated] = true
            val used = hasHistory ||
                prefs[CitiesJson] != null ||
                prefs[ActiveCityId] != null ||
                prefs[UseGps] != null ||
                prefs[GpsCityJson] != null
            if (!used) return@edit
            prefs[InitDone] = true
            // An upgrade inherits the notification step as answered: the reader has
            // been using the app for months and a new full-screen question at launch
            // would be the app interrupting them to ask something Avvisi can say in
            // place, where it is already true (see the card of DESIGN §8.14).
            prefs[NotifyAsked] = true
            if (prefs[CitiesJson] == null) {
                prefs[CitiesJson] = json.encodeToString(listOf(DefaultCity))
            }
        }
    }

    /** The place question has been answered — the notification step follows. */
    suspend fun markInitDone() {
        dataStore.edit { it[InitDone] = true }
    }

    /**
     * The notification step has been passed — granted, refused, or simply left for
     * later. It records that the QUESTION was put, never the answer: the permission
     * itself is the system's to hold, and the app reads it from the system every time
     * rather than remembering a yes that can be taken back from Settings.
     */
    suspend fun markNotificationsAsked() {
        dataStore.edit { it[NotifyAsked] = true }
    }

    /**
     * Skipping the first screen: one edit, both answers. Two edits would emit
     * [FirstRun.Notifications] for a frame on the way past, and a reader who just
     * declined to name a place is not the reader to stop with a second question —
     * with no place there is nothing for an alert to be about yet.
     */
    suspend fun markFirstRunSkipped() {
        dataStore.edit {
            it[InitDone] = true
            it[NotifyAsked] = true
        }
    }

    /**
     * Adds (or refreshes) [city] and makes it active — the search flow.
     *
     * A city already in the list is REPLACED, not skipped: the stored record can be
     * older than the geocoding answer the user just tapped. Inherited fix (tweather,
     * Fase 13f): a Milano searched in Italian kept rendering under its old English
     * record — same GeoNames id as the stored "Milan", so the add was a no-op. Same
     * bug for anyone who switches the phone's language and re-adds a city. Its
     * position in the list is kept: re-adding a city is not a reorder.
     */
    suspend fun add(city: City) {
        dataStore.edit { prefs ->
            val cities = decode(prefs)
            val updated = if (cities.any { it.id == city.id }) {
                cities.map { if (it.id == city.id) city else it }
            } else {
                cities + city
            }
            prefs[CitiesJson] = json.encodeToString(updated)
            prefs[ActiveCityId] = city.id
        }
    }

    suspend fun setActive(city: City) {
        dataStore.edit { it[ActiveCityId] = city.id }
    }

    /**
     * Moves [city] to [toIndex] in the saved list (Chiaro, Fase 3 — the Places sheet
     * is reorderable, VISION §5.6). Everything else about the row is untouched: a
     * reorder is not a selection and not a refresh.
     */
    suspend fun move(city: City, toIndex: Int) {
        dataStore.edit { prefs ->
            val cities = decode(prefs).toMutableList()
            val from = cities.indexOfFirst { it.id == city.id }
            if (from < 0) return@edit
            val item = cities.removeAt(from)
            cities.add(toIndex.coerceIn(0, cities.size), item)
            prefs[CitiesJson] = json.encodeToString(cities)
        }
    }

    /**
     * Re-inserts [city] at [index] without selecting it — the other half of
     * swipe-to-remove's undo (Chiaro, Fase 3): [add] would append at the end and
     * steal the selection, and an undo that does not restore the exact previous
     * state is not an undo. A city already in the list is left where it is.
     */
    suspend fun insert(city: City, index: Int) {
        dataStore.edit { prefs ->
            val cities = decode(prefs)
            if (cities.any { it.id == city.id }) return@edit
            val updated = cities.toMutableList()
            updated.add(index.coerceIn(0, cities.size), city)
            prefs[CitiesJson] = json.encodeToString(updated)
        }
    }

    /**
     * Removes [city] — the last one included. The old guard against removing the
     * last place existed only because tweather's main screen once could not survive
     * without a subject; here "no place yet" is a state the screen says out loud.
     * Removing the active city activates the first one left, or nothing.
     */
    suspend fun remove(city: City) {
        dataStore.edit { prefs ->
            val remaining = decode(prefs).filterNot { it.id == city.id }
            prefs[CitiesJson] = json.encodeToString(remaining)
            if (prefs[ActiveCityId] == city.id) {
                remaining.firstOrNull()
                    ?.let { prefs[ActiveCityId] = it.id }
                    ?: prefs.remove(ActiveCityId)
            }
        }
    }

    /**
     * Enables/disables GPS as a source in one atomic edit: on also selects it (the
     * user just asked for their position); off falls back to the first saved city,
     * or to [ActiveSource.None] when the list is empty (Fase 14b).
     */
    suspend fun setUseGps(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[UseGps] = enabled
            if (enabled) {
                prefs[ActiveCityId] = GpsCityId
            } else if (prefs[ActiveCityId] == GpsCityId) {
                decode(prefs).firstOrNull()
                    ?.let { prefs[ActiveCityId] = it.id }
                    ?: prefs.remove(ActiveCityId)
            }
        }
    }

    /** Selects GPS from the Places sheet; no-op while the toggle is off. */
    suspend fun setActiveGps() {
        dataStore.edit { prefs ->
            if (prefs[UseGps] == true) prefs[ActiveCityId] = GpsCityId
        }
    }

    /**
     * Records a fix and answers with the place the app should now be showing. Never
     * touches the saved list.
     *
     * The adoption rule is here rather than in a caller because every road to a fix
     * has to obey it (Fase 3b). Under [FixAdoptionMeters] the reader has not changed
     * town: the coordinates — and with them the cacheKey, the page, the disk cache
     * and the Journal's history — stay exactly where they were, and only what reverse
     * geocoding learned about the NAME is taken. Past it the fix is a new place and
     * replaces the old one wholesale. Read and write happen inside one edit, so two
     * fixes landing together cannot both measure themselves against the same
     * predecessor.
     *
     * The instant is written on every fix, adopted or not: it is the answer to "is
     * another acquisition due", which does not depend on whether the reader moved.
     */
    suspend fun adoptGpsFix(fix: GeoFix, at: Instant): City {
        val updated = dataStore.edit { prefs ->
            val previous = decodeGpsCity(prefs)
            val moved = previous == null ||
                fix.coordinates.distanceMetersTo(previous.coordinates) >= FixAdoptionMeters
            val adopted = if (moved) {
                fix.toGpsCity()
            } else {
                // A failed geocode must not overwrite a name that worked: its City
                // would carry the coordinate label of a position we are not adopting.
                previous.copy(
                    name = fix.placeName ?: previous.name,
                    region = fix.region ?: previous.region,
                    country = fix.country ?: previous.country,
                    countryCode = fix.countryCode ?: previous.countryCode,
                    admin3 = fix.admin3 ?: previous.admin3
                )
            }
            prefs[GpsCityJson] = json.encodeToString(adopted)
            prefs[GpsFixedAt] = at.toEpochMilli()
        }
        return decodeGpsCity(updated) ?: fix.toGpsCity()
    }

    private fun decodeGpsCity(prefs: Preferences): City? =
        prefs[GpsCityJson]?.let { runCatching { json.decodeFromString<City>(it) }.getOrNull() }

    private fun decode(prefs: Preferences): List<City> =
        prefs[CitiesJson]
            ?.let { runCatching { json.decodeFromString<List<City>>(it) }.getOrNull() }
            ?.takeIf { it.isNotEmpty() }
            ?: emptyList()

    companion object {
        private val CitiesJson = stringPreferencesKey("cities_json")
        private val ActiveCityId = longPreferencesKey("active_city_id")
        private val UseGps = booleanPreferencesKey("use_gps")

        /** This install has been checked for a history that predates first-run. */
        private val Migrated = booleanPreferencesKey("first_run_migrated")

        /** The first-run screen has been answered. */
        private val InitDone = booleanPreferencesKey("init_done")

        /** The first run's notification step has been put to the reader. */
        private val NotifyAsked = booleanPreferencesKey("notify_asked")
        private val GpsCityJson = stringPreferencesKey("gps_city_json")

        /** When [GpsCityJson] was taken, so the interval survives the process. */
        private val GpsFixedAt = longPreferencesKey("gps_fixed_at")

        /**
         * NOT seeded on a fresh install: a city the user never chose is the one
         * thing the place list must not claim. It survives as what [migrateFirstRun]
         * writes for installs that predate the empty state and have been watching it
         * all along. Milan — where the app is developed.
         */
        val DefaultCity = City(
            id = 3_173_435, // GeoNames id, as Open-Meteo geocoding would return
            name = "Milan",
            region = "Lombardy",
            country = "Italy",
            coordinates = Coordinates(45.4643, 9.1895),
            timezone = "Europe/Rome"
        )

        fun create(context: Context, json: Json) = CityStore(context.citiesDataStore, json)
    }
}
