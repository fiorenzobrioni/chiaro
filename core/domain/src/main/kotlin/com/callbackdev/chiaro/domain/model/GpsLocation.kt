package com.callbackdev.chiaro.domain.model

import java.util.Locale
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Reserved id for the GPS pseudo-city (`current_location.json`). GeoNames ids from
 * the geocoding API are always positive, so it can never collide with a saved city.
 */
const val GpsCityId = -1L

/**
 * How far a new fix has to be from the stored one before it counts as a new PLACE
 * (Fase 3b). [City.cacheKey] moves on a ~1.1 km grid, which is finer than any
 * forecast model resolves and far finer than "the reader has changed town": adopting
 * every cell crossing rebuilt the page from a skeleton, spent two GETs and started
 * the Journal's history over, several times on a walk across a city. Below this
 * distance the fix still updates the place's NAME — reverse geocoding gets better
 * answers as the phone settles — but not its coordinates.
 */
const val FixAdoptionMeters = 2_000.0

/**
 * One-shot device position fix. Coordinates arrive already rounded to 2 decimals
 * (~1.1 km) so they map 1:1 onto [City.cacheKey] — history and cache fragment only
 * on real movement, never on float noise. Place fields are best-effort reverse
 * geocoding and stay null when unavailable.
 */
data class GeoFix(
    val coordinates: Coordinates,
    val placeName: String?,
    val region: String?,
    val country: String?
)

/** The GPS pseudo-city rendered by the editor; never stored in the saved list. */
fun GeoFix.toGpsCity(): City = City(
    id = GpsCityId,
    name = placeName ?: coordinates.gpsLabel,
    region = region,
    country = country,
    coordinates = coordinates,
    timezone = null // the forecast API resolves timezone=auto from the coordinates
)

/** `"45.46N 9.19E"` — display name fallback when reverse geocoding fails. */
val Coordinates.gpsLabel: String
    get() = String.format(
        Locale.US,
        "%.2f%s %.2f%s",
        abs(lat), if (lat >= 0) "N" else "S",
        abs(lon), if (lon >= 0) "E" else "W"
    )

/**
 * Great-circle distance in metres (haversine on a mean-radius sphere). The error
 * against the ellipsoid is a few parts per thousand — irrelevant against
 * [FixAdoptionMeters], which is the only question this answers.
 */
fun Coordinates.distanceMetersTo(other: Coordinates): Double {
    val dLat = Math.toRadians(other.lat - lat)
    val dLon = Math.toRadians(other.lon - lon)
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(Math.toRadians(lat)) * cos(Math.toRadians(other.lat)) *
        sin(dLon / 2) * sin(dLon / 2)
    return 2.0 * EarthRadiusMeters * asin(min(1.0, sqrt(a)))
}

/** IUGG mean earth radius. */
private const val EarthRadiusMeters = 6_371_008.8
