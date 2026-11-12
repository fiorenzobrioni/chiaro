package com.callbackdev.chiaro.ui.sky.explore

import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.sky.AstronomyEngine
import com.callbackdev.chiaro.domain.sky.Constellations
import com.callbackdev.chiaro.domain.sky.MeteorShowerTable
import com.callbackdev.chiaro.domain.sky.Planet
import com.callbackdev.chiaro.domain.sky.SkyJobCatalog
import com.callbackdev.chiaro.domain.sky.SkyOccurrence
import com.callbackdev.chiaro.domain.sky.SkyPassage
import com.callbackdev.chiaro.domain.sky.SkySights
import com.callbackdev.chiaro.domain.sky.SkyTarget
import com.callbackdev.chiaro.domain.sky.WorthSeeing
import com.callbackdev.chiaro.widget.window.NamedStars
import java.time.Instant
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max

/**
 * What a reader pointed at in the explorer (PLANNING, Fase 16, step 16.8): the moon, one of
 * the four planets, one of the stars the app can name, a constellation's name, or a peak
 * night's radiant. Nothing else in this sky answers a finger: a star with no name has
 * nothing to say about itself, and a sheet for it would be a sheet with a blank in it.
 *
 * A value with a [key], so the choice survives a rotation: the thing is found again in the
 * new sky by what it is, never by where it was on the screen.
 */
internal sealed interface SkyPick {
    val key: String

    data object Moon : SkyPick {
        override val key = "moon"
    }

    data class Wanderer(val planet: Planet) : SkyPick {
        override val key get() = "planet:${planet.name}"
    }

    /** One of [NamedStars], by its place in the list. */
    data class Star(val index: Int) : SkyPick {
        override val key get() = "star:$index"
    }

    /** A constellation's name on the sky, by its place in [ConstellationUnits.names]. */
    data class Figure(val nameIndex: Int) : SkyPick {
        override val key get() = "figure:$nameIndex"
    }

    /** Where a shower's meteors come from, by the shower's line of the catalog. */
    data class Radiant(val jobId: String) : SkyPick {
        override val key get() = "radiant:$jobId"
    }

    companion object {
        /** The pick a [key] was made from; null for one this build does not know. */
        fun of(key: String): SkyPick? {
            val value = key.substringAfter(':', "")
            return when (key.substringBefore(':')) {
                "moon" -> Moon
                "planet" -> Planet.entries.firstOrNull { it.name == value }?.let(::Wanderer)
                "star" -> value.toIntOrNull()?.takeIf { it in NamedStars.indices }?.let(::Star)
                "figure" -> value.toIntOrNull()?.takeIf { it in ConstellationUnits.names.indices }?.let(::Figure)
                "radiant" -> value.takeIf { MeteorShowerTable.showerOf(it) != null }?.let(::Radiant)
                else -> null
            }
        }
    }
}

/**
 * One thing a finger can land on, where the frame drew it: a light at ([x], [y]) with a
 * disc of [radiusPx] round it (nothing for a point, the moon's own for the moon), or a
 * written name, which is its box.
 */
internal class PickSpot(
    val pick: SkyPick,
    val x: Float,
    val y: Float,
    val radiusPx: Float = 0f,
    val left: Float = x,
    val top: Float = y,
    val right: Float = x,
    val bottom: Float = y,
    val isBox: Boolean = false
) {
    /** How far ([px], [py]) is from this spot's edge: nothing inside the disc or the box. */
    fun distance(px: Float, py: Float): Float = if (isBox) {
        val dx = max(max(left - px, 0f), px - right)
        val dy = max(max(top - py, 0f), py - bottom)
        hypot(dx, dy)
    } else {
        max(hypot(px - x, py - y) - radiusPx, 0f)
    }

    companion object {
        fun box(pick: SkyPick, left: Float, top: Float, right: Float, bottom: Float) = PickSpot(
            pick, (left + right) / 2f, (top + bottom) / 2f, left = left, top = top, right = right, bottom = bottom, isBox = true
        )
    }
}

internal object ExplorePick {
    /** A finger's reach: the step's own twenty-four dp, half of the smallest target a hand hits. */
    const val ReachDp = 24f

    /**
     * The spot a tap at ([x], [y]) means: **the nearest within [reachPx]**, measured to the
     * edge of what is drawn, and null with none that near. Between two at the same distance
     * the earlier of [spots] wins, and the page lists them the way it writes their names:
     * the moon, the radiant, the planets, the stars from the brightest, the constellations.
     * So a tap on Vega is Vega and a tap on the word «LYRA» beside it is the Lyre.
     */
    fun nearest(x: Float, y: Float, spots: List<PickSpot>, reachPx: Float): SkyPick? {
        var best: PickSpot? = null
        var bestDistance = Float.MAX_VALUE
        for (spot in spots) {
            val d = spot.distance(x, y)
            if (d <= reachPx && d < bestDistance) {
                best = spot
                bestDistance = d
            }
        }
        return best?.pick
    }
}

/**
 * What the sheet says about a thing pointed at, as numbers (step 16.8, board 4): where it
 * stands at the instant, when it rises, tops and sets round this night (16.3's passage),
 * until when it is worth looking, and which line of the catalog, if any, is about it. The
 * words are the screen's; there are no degrees among them.
 */
internal class ObjectFacts(
    val pick: SkyPick,
    val target: SkyTarget,
    /** Where it stands at the sky's instant. */
    val place: DomePoint,
    val passage: SkyPassage,
    /** The floor it is worth looking over: how high it has to stand to clear the roofs. */
    val floorDeg: Double,
    /** The last instant of the night it stands over [floorDeg]; null when it never does, or
     * on a date with no night to measure against. */
    val worthUntil: Instant?,
    /** [worthUntil] is the night's own end: it outlasts the dark. */
    val untilDawn: Boolean,
    /** The catalog's line a reminder for this thing rides on; null where there is none,
     * and then the sheet has no bell. */
    val bellJobId: String?,
    /** The guide's page about it; null where there is none, and then no link. */
    val pageJobId: String?
) {
    /** Low enough now that the roofs and the haze have it. */
    val lowNow: Boolean get() = place.altitudeDeg < floorDeg
}

internal object ExploreObjects {

    /** The thing itself, as the engine follows it. */
    fun target(pick: SkyPick): SkyTarget? = when (pick) {
        SkyPick.Moon -> SkyTarget.Moon
        is SkyPick.Wanderer -> SkyTarget.Wanderer(pick.planet)
        is SkyPick.Star -> NamedStars.getOrNull(pick.index)?.let { SkyTarget.Fixed(it.raDeg, it.decDeg) }
        is SkyPick.Figure -> figurePoints.getOrNull(pick.nameIndex)
        is SkyPick.Radiant -> MeteorShowerTable.showerOf(pick.jobId)?.let { SkyTarget.Fixed(it.radiantRaDeg, it.radiantDecDeg) }
    }

    /** Where each constellation's name is written, in [ConstellationUnits.names]' order. */
    private val figurePoints: List<SkyTarget.Fixed> by lazy { Constellations.all.flatMap { it.labels } }

    /**
     * Where [pick] stands in [dome]: the dome's own place for it, so the ring is drawn on
     * the dot and not a rounding away from it. Null for a pick the dome does not hold (a
     * radiant on a night that is not its peak).
     */
    fun place(pick: SkyPick, dome: SkyDome): DomePoint? = when (pick) {
        SkyPick.Moon -> dome.moon
        is SkyPick.Wanderer -> dome.planets.firstOrNull { it.planet == pick.planet }?.place
        is SkyPick.Star -> NamedStars.getOrNull(pick.index)?.let { star ->
            dome.named.firstOrNull { it.star === star }?.place
                ?: AstronomyEngine.starHorizontal(star.raDeg, star.decDeg, dome.at, dome.coords).let { DomePoint(it.altitudeDeg, it.azimuthDeg) }
        }
        is SkyPick.Figure -> dome.constellationNamePlaces.let { places ->
            val at = 3 * pick.nameIndex
            if (at + 2 >= places.size) return null
            val up = places[at + 2].toDouble().coerceIn(-1.0, 1.0)
            DomePoint(Math.toDegrees(asin(up)), SkyCamera.norm360(Math.toDegrees(atan2(places[at].toDouble(), places[at + 1].toDouble()))))
        }
        is SkyPick.Radiant -> dome.radiant?.takeIf { it.jobId == pick.jobId }?.let { DomePoint(it.altitudeDeg, it.azimuthDeg) }
    }

    /**
     * The catalog's line a reminder for [pick] rides on. Jupiter, Mars and Saturn have one
     * for the night; Venus has two, and the one that counts is the side of the night the
     * reader is looking at it on: the sun west of the meridian is the evening, east of it
     * the morning ([sunAzimuthDeg]). A shower's radiant has the shower's peak. **The moon,
     * a star and a constellation have none**: the catalog's moon lines are its rising, its
     * setting and its phases, none of which is «the evenings it is worth looking at».
     */
    fun bellJob(pick: SkyPick, sunAzimuthDeg: Double): String? = when (pick) {
        is SkyPick.Wanderer -> when (pick.planet) {
            Planet.VENUS -> if (sunAzimuthDeg >= 180.0) SkyJobCatalog.VenusEvening.id else SkyJobCatalog.VenusMorning.id
            Planet.JUPITER -> SkyJobCatalog.JupiterNight.id
            Planet.MARS -> SkyJobCatalog.MarsNight.id
            Planet.SATURN -> SkyJobCatalog.SaturnNight.id
        }
        is SkyPick.Radiant -> pick.jobId
        SkyPick.Moon, is SkyPick.Star, is SkyPick.Figure -> null
    }

    /** The guide's page about [pick]: its line's, and for the moon the page of the moon of the day. */
    fun pageJob(pick: SkyPick, sunAzimuthDeg: Double): String? =
        if (pick == SkyPick.Moon) SkyJobCatalog.MoonToday.id else bellJob(pick, sunAzimuthDeg)

    /** How high [pick] has to stand to be worth going out for: the floors of the «Da vedere» list. */
    fun floorDeg(pick: SkyPick): Double = when (pick) {
        SkyPick.Moon -> WorthSeeing.MOON_MIN_ALTITUDE
        is SkyPick.Wanderer -> SkySights.minAltitude(pick.planet)
        is SkyPick.Star, is SkyPick.Figure -> WorthSeeing.FIGURE_MIN_ALTITUDE
        is SkyPick.Radiant -> WorthSeeing.RADIANT_MIN_ALTITUDE
    }

    /**
     * Everything the sheet says about [pick] in [dome]'s sky, or null when the dome does
     * not hold it. [night] is the night the reader is on, dusk to dawn, null on a date that
     * has none: the passage is then the one round the instant itself, and nothing is said
     * about until when.
     */
    fun facts(pick: SkyPick, dome: SkyDome, night: SkyOccurrence.At?, coords: Coordinates): ObjectFacts? {
        val target = target(pick) ?: return null
        val place = place(pick, dome) ?: return null
        val end = night?.end
        val dark = if (night != null && end != null && end.isAfter(night.start)) night.start..end else null
        val floor = floorDeg(pick)
        val until = dark?.let { AstronomyEngine.worthUntil(target, it, coords, floor) }
        return ObjectFacts(
            pick = pick,
            target = target,
            place = place,
            passage = AstronomyEngine.passage(target, dark ?: dome.at..dome.at, coords),
            floorDeg = floor,
            worthUntil = until,
            untilDawn = until != null && dark != null && until == dark.endInclusive,
            bellJobId = bellJob(pick, dome.sun.azimuthDeg),
            pageJobId = pageJob(pick, dome.sun.azimuthDeg)
        )
    }
}
