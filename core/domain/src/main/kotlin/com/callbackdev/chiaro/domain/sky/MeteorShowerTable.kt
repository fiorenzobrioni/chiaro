package com.callbackdev.chiaro.domain.sky

/**
 * The major annual meteor showers, each pinned to the SOLAR LONGITUDE of its peak
 * rather than to a calendar date (Fase 16b, `VISION_SKY.md` §6).
 *
 * Solar longitude is the earth's position in its own orbit, so it names the same
 * point of the debris stream every year: the table cannot go stale, cannot need an
 * update shipped with the app, and gives the right instant for a year nobody thought
 * about when this was written. A hard-coded "August 12" would be wrong by a day in
 * some years and wrong forever once the app stops being updated.
 *
 * Two honesty rules follow the table into the renderer:
 *
 * 1. **A peak is a night, not an instant.** The resolved instant is the centre of the
 *    maximum, and `sky.crontab` renders the local night it falls in — printing
 *    `03:00` alone would promise a precision nobody has.
 * 2. **Moonlight is part of the verdict** (16d). A Geminid peak under a full moon is
 *    a failed build under a perfectly clear sky, and the verdict has to say which of
 *    the two conditions failed.
 *
 * ZHR is deliberately absent. A predicted rate is a modelled number the app cannot
 * verify, and this series does not print numbers it cannot stand behind.
 */
object MeteorShowerTable {

    /**
     * [solarLongitudeDeg] is the apparent longitude of the sun at maximum (J2000
     * values from the IMO working list, rounded to the tenth of a degree — the
     * stream's own width is a degree or more, so the tenth is already generous).
     *
     * Three showers joined the ten in Fase 19, all from the same list: the **alpha
     * Capricornids**, which are slow and make fireballs and share a night with the
     * delta Aquariids, and the two **Taurids**, the autumn fireball streams. The delta
     * Aquariids moved with them, from 125.0° to the 127.0° the working list now gives
     * — two days, and the file cites that list rather than a memory of it.
     */
    data class MeteorShower(
        val id: String,
        val solarLongitudeDeg: Double,
        /**
         * The radiant at maximum, J2000, to the degree (the IMO working list's own
         * rounding; 20 ott 2026): the point the shower's meteors trace back to. A radiant
         * drifts about a degree a day along the stream, so these hold on the peak night
         * and are not asked about on any other.
         */
        val radiantRaDeg: Double,
        val radiantDecDeg: Double
    )

    val all: List<MeteorShower> = listOf(
        MeteorShower("quadrantids", 283.2, 230.0, 49.0),
        MeteorShower("lyrids", 32.3, 271.0, 34.0),
        MeteorShower("eta_aquariids", 45.5, 338.0, -1.0),
        MeteorShower("delta_aquariids", 127.0, 340.0, -16.0),
        MeteorShower("alpha_capricornids", 127.0, 307.0, -10.0),
        MeteorShower("perseids", 140.0, 48.0, 58.0),
        MeteorShower("draconids", 195.4, 262.0, 54.0),
        MeteorShower("southern_taurids", 197.0, 32.0, 9.0),
        MeteorShower("orionids", 208.0, 95.0, 16.0),
        MeteorShower("northern_taurids", 230.0, 58.0, 22.0),
        MeteorShower("leonids", 235.3, 152.0, 22.0),
        MeteorShower("geminids", 262.2, 112.0, 33.0),
        MeteorShower("ursids", 270.7, 217.0, 76.0)
    )

    fun byId(id: String): MeteorShower? = all.firstOrNull { it.id == id }

    /** The catalog job id of a shower, e.g. `meteor.perseids.peak`. */
    fun jobId(shower: MeteorShower): String = "meteor.${shower.id}.peak"

    /** The shower behind a catalog job id, or null when the id is not a shower's. */
    fun showerOf(jobId: String): MeteorShower? =
        jobId.removeSurrounding("meteor.", ".peak")
            .takeIf { it != jobId }
            ?.let(::byId)
}
