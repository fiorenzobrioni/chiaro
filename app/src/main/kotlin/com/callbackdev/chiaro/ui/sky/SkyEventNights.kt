package com.callbackdev.chiaro.ui.sky

import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.sky.AstronomyEngine
import com.callbackdev.chiaro.domain.sky.MeteorShowerTable
import com.callbackdev.chiaro.domain.sky.MoonQuarterKind
import com.callbackdev.chiaro.domain.sky.SkyBody
import com.callbackdev.chiaro.domain.sky.SkyJobCatalog
import com.callbackdev.chiaro.domain.sky.SkyScheduler
import com.callbackdev.chiaro.domain.sky.SkyTarget
import com.callbackdev.chiaro.domain.sky.WorthSeeing
import com.callbackdev.chiaro.ui.sky.explore.ExploreObjects
import com.callbackdev.chiaro.ui.sky.explore.SkyPick
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The night of an event of the calendar ahead, as the explorer is opened on it (PLANNING,
 * Fase 16, step 16.9, «See that night's sky»): the evening it begins on, the hour to open
 * at, what the event is about, as the explorer names a thing ([pick], a `SkyPick` key), and
 * which way that stands at that hour, for a sky that cannot ring it (a weak shower nobody
 * follows has no radiant drawn).
 */
data class EventNight(
    val date: LocalDate,
    val at: Instant,
    val pick: String?,
    val facingDeg: Double?
)

/**
 * Which rows of the calendar ahead have a night of their own, and which night.
 *
 * **A night of its own** is an event somebody goes out for after dark, about a thing the
 * explorer can turn to: a meteor shower (its radiant), an eclipse of the moon, the moon
 * beside a planet or two planets together, a full moon. A solstice, an equinox, the ends of
 * the earth's orbit, the earliest sunset and the white nights' two dates are dates, an
 * eclipse of the sun is a day's, and a new moon is a sky with nothing of its own in it:
 * none has the link, and a link under every row would be the page filling space.
 *
 * **The evening a night begins on** is the date twelve hours before the event: an instant
 * before noon belongs to the night that began the evening before, one after noon to the
 * night that begins that evening. A shower's night is its window's own evening.
 *
 * **The hour** is the event's own where it has one that is the sight (the middle of an
 * eclipse, the instant two bodies stand closest, the full moon rising at dusk), and where
 * it has not (a shower is a whole night, a full moon's instant is geometry) the hour the
 * «worth seeing» list has for the thing on that night: when it stands highest.
 */
internal object SkyEventNights {

    /** The evening [event]'s night begins on, or null for an event with no night of its own. */
    fun dateOf(event: UpcomingEvent, zone: ZoneId): LocalDate? {
        val at = event.at ?: return null
        if (kindOf(event) == null) return null
        val instant = if (kindOf(event) == Kind.ECLIPSE) at.middle else at.start
        return instant.atZone(zone).minusHours(HALF_DAY).toLocalDate()
    }

    /** [event]'s night, with the hour and the thing; null as [dateOf] is. Works the list
     * out for that night, so it is asked when the link is tapped and not for every row. */
    fun of(event: UpcomingEvent, zone: ZoneId, coords: Coordinates, followed: Set<String>): EventNight? {
        val date = dateOf(event, zone) ?: return null
        val at = event.at ?: return null
        val id = event.job.id
        fun listed(pick: SkyPick, known: Set<String> = followed): Instant? =
            WorthSeeing.of(date, zone, coords, known).firstOrNull { SkyPick.of(it.subject) == pick }?.best
        val (pick, hour) = when (kindOf(event)) {
            // The shower's own row is asked for whether or not the reader follows it: the
            // link stands under its row, and the hour is the radiant's all the same.
            Kind.SHOWER -> SkyPick.Radiant(id).let { it to (listed(it, followed + id) ?: at.middle) }
            Kind.FULL_MOON -> SkyPick.Moon to (listed(SkyPick.Moon) ?: at.start)
            Kind.ECLIPSE -> SkyPick.Moon to at.middle
            Kind.MOON_AT_DUSK -> SkyPick.Moon to at.start
            Kind.PAIR -> pairPick(id) to at.start
            null -> return null
        }
        val target: SkyTarget? = ExploreObjects.target(pick)
        return EventNight(
            date = date,
            at = hour,
            pick = pick.key,
            facingDeg = target?.let { AstronomyEngine.horizontal(it, hour, coords).azimuthDeg }
        )
    }

    private enum class Kind { SHOWER, FULL_MOON, ECLIPSE, MOON_AT_DUSK, PAIR }

    private fun kindOf(event: UpcomingEvent): Kind? {
        val id = event.job.id
        return when {
            MeteorShowerTable.showerOf(id) != null -> Kind.SHOWER
            id == SkyJobCatalog.LunarEclipse.id -> Kind.ECLIPSE
            id == SkyJobCatalog.MoonFullAtDusk.id -> Kind.MOON_AT_DUSK
            id in SkyScheduler.ConjunctionJobs -> Kind.PAIR
            event.quarter == MoonQuarterKind.FULL_MOON ||
                id == SkyJobCatalog.MoonFull.id ||
                id == SkyJobCatalog.MoonClosestFull.id -> Kind.FULL_MOON
            else -> null
        }
    }

    /** The fainter of a pair: where it is, the other is beside it, and the moon needs no finding. */
    private fun pairPick(jobId: String): SkyPick {
        val (first, second) = SkyScheduler.ConjunctionJobs.getValue(jobId)
        val body = if (first == SkyBody.MOON) second else first
        return body.planet?.let(SkyPick::Wanderer) ?: SkyPick.Moon
    }

    private const val HALF_DAY = 12L
}
