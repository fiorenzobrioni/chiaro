package com.callbackdev.chiaro.data

import com.callbackdev.chiaro.domain.WeatherException
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.model.isUnnamed
import com.callbackdev.chiaro.domain.model.FixAdoptionMeters
import com.callbackdev.chiaro.domain.model.GeoFix
import com.callbackdev.chiaro.domain.model.distanceMetersTo
import com.callbackdev.chiaro.domain.model.toGpsCity
import java.time.Instant
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.first

/**
 * Whether a fix moves the position page, decided by the weather and not only by the
 * distance (committente, 20 ott 2026). A fix past [FixAdoptionMeters] used to be adopted
 * on the spot, so a reader who moved while offline found a page with no weather and
 * coordinates for a title, where a moment before there had been the last forecast with
 * its age. Now the arrival's first forecast is asked for BEFORE the page moves: in hand,
 * the page moves and arrives with it; not to be had, the page stays where its weather is
 * and the arrival waits ([LocationSettings.arrival]), said on the page and the widgets.
 *
 * Every road to a fix comes through here, as it came through [CityStore.adoptGpsFix]:
 * the sheet's toggle, its row, and Today's own refix.
 */
class GpsArrival(
    private val cityStore: CityStore,
    /** Whether the place already has a report to show. */
    private val hasWeather: suspend (City) -> Boolean,
    /** Fetches and caches the place's report; throws [WeatherException] when it cannot. */
    private val fetchWeather: suspend (City) -> Unit,
    /** The geocoder alone, for a place named by its coordinates (22 ott 2026); none in a
     * caller that has no provider, and the coordinates stay. */
    private val placeOf: suspend (Coordinates) -> GeocodedPlace? = { null }
) {

    constructor(
        cityStore: CityStore,
        repository: WeatherRepository,
        locationProvider: LocationProvider? = null
    ) : this(
        cityStore,
        hasWeather = { repository.cachedReport(it) != null },
        fetchWeather = { repository.getWeather(it) },
        placeOf = { coordinates -> locationProvider?.placeOf(coordinates) }
    )

    /** Takes [fix] and answers with the place the position page now shows. */
    suspend fun take(fix: GeoFix, at: Instant): City {
        val shown = cityStore.locationSettings.first().gpsCity
        val moves = shown == null ||
            fix.coordinates.distanceMetersTo(shown.coordinates) >= FixAdoptionMeters
        // Nothing to stay on: an empty page for the new place is all there is to show,
        // and it fetches on its own once it is the page.
        if (!moves || shown == null || !hasWeather(shown)) return cityStore.adoptGpsFix(fix, at)
        return try {
            fetchWeather(fix.toGpsCity())
            cityStore.adoptGpsFix(fix, at)
        } catch (e: WeatherException) {
            cityStore.holdGpsFix(fix, at)
            shown
        }
    }

    /**
     * Asks again for the waiting arrival's weather, and moves the page there once it
     * lands. Answers whether it did. Nothing waiting, nothing asked.
     */
    suspend fun retry(): Boolean {
        val arrival = cityStore.locationSettings.first().arrival ?: return false
        // Named now if it could not be then (22 ott 2026, from the device: a move made
        // offline reached the page as «45.48N 9.30E» and kept the coordinates until the
        // next fix): the fix was taken when the geocoder could not answer, and this is
        // the first moment online.
        val named = named(arrival)
        return try {
            fetchWeather(named)
            cityStore.adoptArrival(named)
            true
        } catch (e: WeatherException) {
            false
        }
    }

    /**
     * Gives the position page its name when it still carries its coordinates (22 ott
     * 2026): the fix was adopted at once, with no place to stay on, while the geocoder
     * could not answer. Asked after a fetch that succeeded, which is the sign of being
     * online. True when the page was renamed.
     */
    suspend fun nameShown(): Boolean {
        val shown = cityStore.locationSettings.first().gpsCity ?: return false
        if (!shown.isUnnamed) return false
        val named = named(shown)
        if (named === shown) return false
        cityStore.renameGpsCity(named)
        return true
    }

    /** [city] with the geocoder's name when it has none but its coordinates, else itself. */
    private suspend fun named(city: City): City {
        if (!city.isUnnamed) return city
        val place = runCatching { placeOf(city.coordinates) }.getOrNull() ?: return city
        val name = place.name ?: return city
        return city.copy(
            name = name, region = place.region, country = place.country,
            countryCode = place.countryCode, admin3 = place.admin3
        )
    }

    companion object {
        /** Whole kilometres from the place shown to the place reached, never under the
         * two the adoption rule needs before a fix is an arrival at all. */
        fun distanceKm(shown: City, reached: City): Int =
            (shown.coordinates.distanceMetersTo(reached.coordinates) / 1_000.0).roundToInt()
                .coerceAtLeast((FixAdoptionMeters / 1_000.0).roundToInt())
    }
}
