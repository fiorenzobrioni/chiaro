package com.callbackdev.chiaro.domain

import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.model.QuarterHour
import java.time.ZoneOffset

/**
 * Where the 15-minute forecast is a forecast (17 ott 2026, `PLANNING.md`, «La pioggia
 * nelle prossime due ore»).
 *
 * Open-Meteo answers `minutely_15` everywhere, but only three of its models compute the
 * quarter: DWD's ICON-D2 over central Europe, Météo-France's AROME over France, NOAA's HRRR
 * over the United States. Everywhere else the series is **the hourly one cut in four**, each
 * hour's amount spread evenly over its quarters: a number that looks like a quarter of an
 * hour and is not one. A section that drew it would be a placeholder rendered as a value.
 *
 * Measured the day this was written, `best_match` against each model at the same point,
 * 192 quarters each:
 *
 * - **Milan, Bologna, Ancona**: identical to ICON-D2's in every quarter, and the four
 *   quarters of each hour sum to the hour the strip already shows. Milan's eight wet hours
 *   have eight different shapes (0.1 0.1 0.2 · 0.3 0.1 0.1 0.8): computed.
 * - **New York**: identical to HRRR's, its four wet hours all shaped.
 * - **London**, inside ICON-D2's grid: neither ICON-D2's (20 quarters differ) nor AROME's —
 *   `best_match` there is the UK Met Office's hourly model, and its three wet hours are
 *   0.2 0.2 0.2 0.2 each: interpolated. So the grid is not the answer; the country is.
 * - **Rome, Naples, Palermo, Bari**: no ICON-D2 at all (the key is absent from the response).
 *
 * So a place is covered when its country is one where `best_match` is one of the computing
 * models AND it stands inside that model's grid, less a margin, because the edge of a
 * limited-area model is where it is fed by a coarser one. By ISO code like
 * [PlaceRegion]: a place without one is not covered, never guessed.
 *
 * The country table can be wrong tomorrow (Open-Meteo can change what `best_match` picks),
 * and so the series is also read for what it is: [looksInterpolated] fails closed. And it
 * is re-measured monthly by the provider's probe (`tools/probe_provider.py`, Fase 14, step
 * 14.1), which re-ran the comparison above on 45 cities on 27 Oct 2026 and found France
 * computed by ICON-D2 (see [IconD2Countries]): the country table is a measurement with a
 * date, never a recollection of Open-Meteo's documentation.
 */
object NowcastCoverage {

    /** A model's grid, in degrees. */
    data class Box(val minLat: Double, val maxLat: Double, val minLon: Double, val maxLon: Double) {
        fun contains(c: Coordinates): Boolean = c.lat in minLat..maxLat && c.lon in minLon..maxLon

        /** The same box, [degrees] narrower on every side. */
        fun inset(degrees: Double) = Box(minLat + degrees, maxLat - degrees, minLon + degrees, maxLon - degrees)
    }

    /**
     * How far inside a grid's edge a place has to stand: ten ICON-D2 points, the width of
     * the band where a limited-area model is still being relaxed towards the model it is
     * nested in.
     */
    const val EDGE_MARGIN_DEG = 0.25

    /** DWD's ICON-D2 on its regular grid: 1215 × 746 points of 0.02° from 43.18° N, 3.94° W. */
    val IconD2 = Box(minLat = 43.18, maxLat = 58.08, minLon = -3.94, maxLon = 20.34)

    /** NOAA's HRRR over the contiguous United States, as a box inside its Lambert grid. */
    val Hrrr = Box(minLat = 24.5, maxLat = 49.5, minLon = -125.0, maxLon = -66.5)

    /**
     * The countries whose `best_match` is ICON-D2 for its first two days, **measured** and not
     * read off Open-Meteo's table (the provider's probe, `tools/probe_provider.py`, 27 Oct 2026,
     * `tools/measurements/probe-2026-10-27.json`): in its source the rule «anywhere ICON-D2 has
     * data» comes before the one that hands France to AROME, so Paris, Lyon, Toulouse, Bordeaux
     * and Nice take ICON-D2's quarters, computed, and the table of 17 Oct that left France out
     * «because it goes to AROME» was wrong by the source's own order. Measured computed too:
     * Zagreb, Bratislava, Wrocław. Measured NOT ICON-D2 although inside the grid: Luxembourg,
     * Amsterdam and Brussels (KNMI), London and Dublin (UKMO), Copenhagen and Odense (MET
     * Nordic), Warsaw and Budapest (DMI HARMONIE Europe: the regular box is wider than the
     * rotated grid's data at its eastern edge), Marseille (at the edge, the inset below). So
     * the box alone does not decide, and a country enters this set only with a measured city
     * in it. Italy is in it as far as the grid reaches, which is Florence and Ancona and not
     * Perugia. Hungary is out on Budapest's evidence; a country never measured is out until
     * the probe says otherwise.
     */
    private val IconD2Countries = setOf("IT", "SM", "CH", "LI", "AT", "DE", "SI", "CZ", "FR", "HR", "SK", "PL")

    private val Grids: List<Pair<Set<String>, Box>> = listOf(
        IconD2Countries to IconD2.inset(EDGE_MARGIN_DEG),
        setOf("US") to Hrrr
    )

    fun covers(countryCode: String?, coordinates: Coordinates): Boolean {
        val code = countryCode?.uppercase() ?: return false
        return Grids.any { (countries, box) -> code in countries && box.contains(coordinates) }
    }

    /**
     * The hourly series cut in four, read off the series itself: every hour with something
     * in it has four equal quarters. Open-Meteo spreads an hour evenly when it interpolates
     * (London's 0.2 0.2 0.2 0.2), and a model that computes the quarter almost never draws
     * four equal ones (none of Milan's eight wet hours, none of New York's four). A series
     * with nothing falling says nothing either way and is not suspected: dry is dry in both.
     *
     * Fails closed: an hour of steady drizzle a real model drew flat, alone in its series,
     * hides the section for that fetch. That costs a light drizzle its card; drawing an
     * interpolated hour as a computed one would cost the card its truth.
     */
    fun looksInterpolated(quarters: List<QuarterHour>): Boolean {
        val hours = quarters
            .groupBy { it.start.atOffset(ZoneOffset.UTC).let { t -> t.toLocalDate() to t.hour } }
            .values
            .filter { it.size == QUARTERS_PER_HOUR && it.any { q -> q.precipMm > 0.0 } }
        if (hours.isEmpty()) return false
        return hours.all { hour -> hour.map { it.precipMm }.distinct().size == 1 }
    }

    private const val QUARTERS_PER_HOUR = 4
}
