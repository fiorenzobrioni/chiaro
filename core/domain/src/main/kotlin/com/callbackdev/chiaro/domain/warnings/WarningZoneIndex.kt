package com.callbackdev.chiaro.domain.warnings

import com.callbackdev.chiaro.domain.model.Coordinates
import java.text.Normalizer
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Which warning zone a place is in — the join between a coordinate the app owns and a
 * bulletin that speaks in zone codes.
 *
 * Built once from the bundled asset `warning_zones_it.json`, which
 * `tools/build_warning_zones.py` writes from the Dipartimento's own shapefile and
 * TopoJSON (the tool's docstring is the record of what the asset is and how it was
 * measured). Pure Kotlin: the asset is a string when it gets here, and reading it off
 * the Android side is `:core:data`'s one line.
 *
 * **Geometry first, the municipality second** (PLANNING, Fase 11). The polygons are
 * simplified to [toleranceMeters] and a point that close to a border cannot be placed
 * by geometry alone — so inside that band the municipality name decides when it is
 * known, and the deepest or nearest zone when it is not. Far from every zone the name
 * is the last resort: a point can be handed in with a comune and a coordinate that
 * disagree, and the coordinate is the one the app measured.
 */
class WarningZoneIndex private constructor(
    private val zones: List<IndexedZone>,
    /** How far the simplified borders may sit from the real ones. */
    val toleranceMeters: Double,
    /** The bulletin the zones were taken from, `AAAAMMGG_HHMM`. */
    val bulletinStamp: String
) {
    private val byCode: Map<String, IndexedZone> = zones.associateBy { it.zone.code }

    val size: Int get() = zones.size

    val all: List<WarningZone> get() = zones.map { it.zone }

    fun zone(code: String): WarningZone? = byCode[code]?.zone

    /**
     * The zone [point] is in, or `null` when it is in none (abroad, at sea).
     *
     * [comune] is the municipality as the geocoder or Open-Meteo's `admin3` spells it
     * ("Comune di Segrate", "Bozen"), best effort and nullable; it is normalized here.
     *
     * The order of the decision, each step only when the one before did not decide:
     * 1. a zone that contains the point at more than [toleranceMeters] from its border
     *    is certain (two such zones would be an overlap in the source and the comune,
     *    then the deeper one, breaks the tie);
     * 2. in the border band — inside a polygon or within the tolerance of one — the
     *    comune decides if it names exactly one of the zones there, else the zone the
     *    point is deepest inside, else the nearest (a beach the simplified coast moved
     *    inland);
     * 3. clear of every polygon, the comune alone, if it belongs to exactly one zone.
     */
    fun locate(point: Coordinates, comune: String? = null): WarningZone? {
        val key = comune?.let(::normalizeComune)?.takeIf { it.isNotEmpty() }
        val candidates = zones
            .filter { it.bboxContains(point, toleranceMeters) }
            .map { zone ->
                Candidate(
                    zone = zone,
                    inside = zone.contains(point),
                    borderMeters = zone.borderDistanceMeters(point),
                    named = key != null && key in zone.comuni
                )
            }

        val certain = candidates.filter { it.inside && it.borderMeters >= toleranceMeters }
        if (certain.size == 1) return certain.single().zone.zone
        if (certain.size > 1) {
            val named = certain.filter { it.named }
            return (if (named.size == 1) named else certain).maxBy { it.borderMeters }.zone.zone
        }

        val near = candidates.filter { it.inside || it.borderMeters < toleranceMeters }
        if (near.isNotEmpty()) {
            val named = near.filter { it.named }
            if (named.size == 1) return named.single().zone.zone
            val inside = near.filter { it.inside }
            if (inside.isNotEmpty()) return inside.maxBy { it.borderMeters }.zone.zone
            return near.minBy { it.borderMeters }.zone.zone
        }

        if (key != null) {
            val byName = zones.filter { key in it.comuni }
            if (byName.size == 1) return byName.single().zone
        }
        return null
    }

    private class Candidate(
        val zone: IndexedZone,
        val inside: Boolean,
        val borderMeters: Double,
        val named: Boolean
    )

    companion object {
        private const val MetersPerDegreeLat = 111_320.0

        private val json = Json { ignoreUnknownKeys = true }

        /** Reads the asset the importer wrote; fails loudly on anything else. */
        fun decode(assetJson: String): WarningZoneIndex {
            val asset = json.decodeFromString<AssetDto>(assetJson)
            val quantum = generateSequence(1.0) { it * 10 }.elementAt(asset.precision)
            val zones = asset.zones.map { dto ->
                IndexedZone(
                    zone = WarningZone(code = dto.code, name = dto.name, region = dto.region),
                    rings = dto.rings.map { flat -> Ring.fromDeltas(flat, quantum) },
                    comuni = dto.comuni.toHashSet()
                )
            }
            return WarningZoneIndex(
                zones = zones,
                toleranceMeters = asset.toleranceM.toDouble(),
                bulletinStamp = asset.stamp
            )
        }

        /**
         * One spelling for a municipality, on both sides of the join. Mirrored in
         * `tools/build_warning_zones.py` (`normalize_comune`) — change both: NFD with the
         * combining marks dropped, lower case, curly apostrophes straightened,
         * whitespace collapsed. The lookup side alone also drops a leading "Comune di",
         * which is how Open-Meteo's `admin3` spells a municipality; the asset never
         * carries the prefix, so applying it to both is the same as applying it to one.
         */
        fun normalizeComune(name: String): String {
            val decomposed = Normalizer.normalize(name, Normalizer.Form.NFD)
            val stripped = CombiningMarks.replace(decomposed, "")
            val straight = stripped.replace('’', '\'').replace('`', '\'').replace('´', '\'')
            val collapsed = straight.lowercase().trim().split(Whitespace).joinToString(" ")
            return collapsed.removePrefix("comune di ")
        }

        private val CombiningMarks = Regex("\\p{Mn}+")
        private val Whitespace = Regex("\\s+")

        private fun metersPerDegreeLon(lat: Double): Double =
            MetersPerDegreeLat * cos(Math.toRadians(lat))
    }

    private class IndexedZone(
        val zone: WarningZone,
        val rings: List<Ring>,
        val comuni: Set<String>
    ) {
        private val minLon = rings.minOf { it.minLon }
        private val maxLon = rings.maxOf { it.maxLon }
        private val minLat = rings.minOf { it.minLat }
        private val maxLat = rings.maxOf { it.maxLat }

        fun bboxContains(point: Coordinates, paddingMeters: Double): Boolean {
            val padLat = paddingMeters / MetersPerDegreeLat
            val padLon = paddingMeters / metersPerDegreeLon(point.lat)
            return point.lon in (minLon - padLon)..(maxLon + padLon) &&
                point.lat in (minLat - padLat)..(maxLat + padLat)
        }

        /** Even-odd over every ring: holes count themselves out, orientation is moot. */
        fun contains(point: Coordinates): Boolean {
            var inside = false
            for (ring in rings) if (ring.crossings(point) % 2 == 1) inside = !inside
            return inside
        }

        fun borderDistanceMeters(point: Coordinates): Double =
            rings.minOf { it.distanceMeters(point) }
    }

    /** A closed ring stored open (the last vertex joins the first). */
    private class Ring(private val lon: DoubleArray, private val lat: DoubleArray) {
        val minLon = lon.min()
        val maxLon = lon.max()
        val minLat = lat.min()
        val maxLat = lat.max()

        /** How many edges a ray from [point] to +∞ longitude crosses. */
        fun crossings(point: Coordinates): Int {
            var count = 0
            val n = lon.size
            for (i in 0 until n) {
                val j = (i + 1) % n
                if ((lat[i] > point.lat) != (lat[j] > point.lat)) {
                    val x = lon[i] + (point.lat - lat[i]) * (lon[j] - lon[i]) / (lat[j] - lat[i])
                    if (x > point.lon) count++
                }
            }
            return count
        }

        /** Distance from [point] to the nearest edge, in a frame that is metric around it. */
        fun distanceMeters(point: Coordinates): Double {
            val mLon = metersPerDegreeLon(point.lat)
            val px = point.lon * mLon
            val py = point.lat * MetersPerDegreeLat
            var best = Double.MAX_VALUE
            val n = lon.size
            for (i in 0 until n) {
                val j = (i + 1) % n
                val ax = lon[i] * mLon
                val ay = lat[i] * MetersPerDegreeLat
                val dx = lon[j] * mLon - ax
                val dy = lat[j] * MetersPerDegreeLat - ay
                val d = if (dx == 0.0 && dy == 0.0) {
                    hypot(px - ax, py - ay)
                } else {
                    val t = max(0.0, min(1.0, ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy)))
                    hypot(px - (ax + t * dx), py - (ay + t * dy))
                }
                if (d < best) best = d
            }
            return best
        }

        companion object {
            fun fromDeltas(flat: List<Int>, quantum: Double): Ring {
                require(flat.size % 2 == 0 && flat.size >= 6) { "a ring is at least three (lon, lat) pairs" }
                val n = flat.size / 2
                val lon = DoubleArray(n)
                val lat = DoubleArray(n)
                var x = 0L
                var y = 0L
                for (i in 0 until n) {
                    x += flat[2 * i]
                    y += flat[2 * i + 1]
                    lon[i] = x / quantum
                    lat[i] = y / quantum
                }
                return Ring(lon, lat)
            }
        }
    }
}

@Serializable
private data class AssetDto(
    val source: String,
    val stamp: String,
    val license: String,
    val precision: Int,
    val toleranceM: Int,
    val zones: List<ZoneDto>
)

@Serializable
private data class ZoneDto(
    val code: String,
    val name: String,
    val region: String,
    val rings: List<List<Int>>,
    val comuni: List<String>
)
