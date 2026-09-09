package com.callbackdev.chiaro.domain.warnings

import java.time.LocalDate
import java.time.LocalDateTime

// The official warnings (9 set 2026, Fase 11). The words first: an "allerta" (EN
// *warning*) is what an authority issues; an "avviso" (EN *alert*) is what the reader
// writes or switches on, and `Alert` is already theirs — hence `warnings/`. Nothing in
// this package is Italian-specific except the three hazards the Protezione Civile
// grades; MeteoAlarm's types are added to the enum when Fase 12 needs them.

/**
 * The four levels of the Dipartimento's bulletin, in severity order, so that
 * comparisons and `maxOf` mean what they say. NONE is the green of the map: a zone
 * the bulletin does not mention.
 */
enum class WarningLevel { NONE, YELLOW, ORANGE, RED }

/**
 * The three risks the criticality bulletin grades independently. Thunderstorms have
 * no RED: the Dipartimento folds a red thunderstorm scenario into hydrogeological red
 * (its README says so), and the engine does not invent one.
 */
enum class WarningHazard {
    HYDRAULIC,
    HYDROGEOLOGICAL,
    THUNDERSTORM;

    companion object {
        /**
         * The Dipartimento's own tie-break when two hazards share a level ("Idraulico,
         * Temporali, Idrogeologico", the shapefile's README): the order the surfaces
         * list hazards of equal level in, so the banner and the notification agree.
         */
        val displayOrder: List<WarningHazard> = listOf(HYDRAULIC, THUNDERSTORM, HYDROGEOLOGICAL)
    }
}

/**
 * A warning zone as the bundled index knows it. [code] is the issuer's identifier
 * (`Lomb-09`) and is for the join with the bulletin, never for the screen; [name] is
 * what the sheet prints ("Nodo Idraulico di Milano"); [region] is what the bulletin's
 * note names when it defers to a regional bulletin.
 *
 * For thirteen zones (Basilicata's seven, Marche's six) the Region never gave the
 * zone a name and [name] IS the code: the importer counts them on every rebuild, and
 * how the sheet reads them is a decision of the surface, not of this class.
 */
data class WarningZone(
    val code: String,
    val name: String,
    val region: String
) {
    /**
     * Whether the zone has a name of its own to print. False for the thirteen the
     * Region never named, where [name] IS [code]: a surface asks this and prints the
     * region instead ("Zona di allerta in Basilicata"), because "nothing here is a zone
     * code" is a rule about the screen, not about the file (DESIGN §8.13).
     */
    val named: Boolean get() = name != code
}

/** One row of a bulletin: one level, for one hazard, in one zone, on one day. */
data class ZoneWarning(
    val zoneCode: String,
    val day: LocalDate,
    val hazard: WarningHazard,
    val level: WarningLevel
)

/**
 * A bulletin as issued: a document with an identifier, an issue time and a validity
 * that outlives any fetch — which is why it is stored as a document and not as an
 * observation (PLANNING, Fase 11).
 *
 * [warnings] lists only what is graded above NONE; every (zone, day, hazard) the list
 * does not mention is NONE, exactly as the zones the CAP leaves out are green on the
 * map. [days] are the days the bulletin covers in order (today, tomorrow), and the
 * bulletin expires at midnight closing its last day. [note] is the issuer's national
 * note, kept in the language it was written (VISION §8).
 */
data class WarningBulletin(
    val id: String,
    val issuedAt: LocalDateTime,
    val days: List<LocalDate>,
    val note: String?,
    val warnings: List<ZoneWarning>
) {
    init {
        require(days.isNotEmpty()) { "a bulletin covers at least one day" }
    }

    /** The first instant the bulletin has nothing left to say about. */
    val expiresAt: LocalDateTime
        get() = days.max().plusDays(1).atStartOfDay()

    /**
     * Every hazard's level for one zone on one day, NONE where the bulletin is
     * silent; the highest wins if the same row was graded twice.
     */
    fun levelsFor(zoneCode: String, day: LocalDate): Map<WarningHazard, WarningLevel> =
        WarningHazard.entries.associateWith { hazard ->
            warnings
                .filter { it.zoneCode == zoneCode && it.day == day && it.hazard == hazard }
                .maxOfOrNull { it.level } ?: WarningLevel.NONE
        }
}

/**
 * What one bulletin says about one place: its zone, the days still ahead, and a level
 * per hazard for each of them. Produced by [OfficialWarningEngine.forPlace]; all green
 * is a legitimate value (Avvisi prints "Nessuna allerta" from it), absence of a zone or
 * of a valid bulletin is `null` there, not a PlaceWarnings with nothing in it.
 */
data class PlaceWarnings(
    val zone: WarningZone,
    val bulletinId: String,
    val issuedAt: LocalDateTime,
    /** In date order, the first being today or the nearest day ahead; never empty. */
    val days: List<DayWarnings>,
    /** The bulletin's note, only when it names this zone's region or the zone itself. */
    val note: String?
) {
    init {
        require(days.isNotEmpty()) { "PlaceWarnings must cover at least one day" }
    }

    /** The highest level over every day and hazard; NONE means "nothing to warn about". */
    val maxLevel: WarningLevel
        get() = days.maxOf { it.maxLevel }

    /** Midnight closing the last day covered. */
    val expiresAt: LocalDateTime
        get() = days.maxOf { it.date }.plusDays(1).atStartOfDay()

    /**
     * The days that carry [maxLevel] — what the banner and the notification say the
     * warning is FOR ("oggi fino a mezzanotte", "oggi e domani"). One rule, so the two
     * can never name different days.
     */
    val peakDays: List<DayWarnings>
        get() = days.filter { it.maxLevel == maxLevel }

    /**
     * Every hazard graded above NONE anywhere in [days], at the highest level it reaches,
     * ordered exactly as a day's own [DayWarnings.ranked] is: level first, then the
     * Dipartimento's tie-break. This is the banner's first line ("Allerta arancione per
     * temporali, gialla per rischio idrogeologico") and the order every other surface
     * lists hazards in.
     */
    val ranked: List<Pair<WarningHazard, WarningLevel>>
        get() = WarningHazard.entries
            .map { hazard -> hazard to days.maxOf { it.levels.getValue(hazard) } }
            .filter { it.second != WarningLevel.NONE }
            .sortedWith(
                compareByDescending<Pair<WarningHazard, WarningLevel>> { it.second }
                    .thenBy { WarningHazard.displayOrder.indexOf(it.first) }
            )

    /** The levels of one day, one per hazard, always all three. */
    data class DayWarnings(
        val date: LocalDate,
        val levels: Map<WarningHazard, WarningLevel>
    ) {
        init {
            require(levels.keys == WarningHazard.entries.toSet()) {
                "a day carries a level for every hazard"
            }
        }

        val maxLevel: WarningLevel
            get() = levels.values.max()

        /**
         * The hazards graded above NONE, highest level first and the Dipartimento's
         * tie-break within a level — the order every surface prints them in.
         */
        val ranked: List<Pair<WarningHazard, WarningLevel>>
            get() = levels.entries
                .filter { it.value != WarningLevel.NONE }
                .sortedWith(
                    compareByDescending<Map.Entry<WarningHazard, WarningLevel>> { it.value }
                        .thenBy { WarningHazard.displayOrder.indexOf(it.key) }
                )
                .map { it.key to it.value }
    }
}

/**
 * A warning worth a notification, with the fingerprint the store burns once it has
 * been shown: `"$cityKey:warn:$bulletinId:$maxLevel"`. Same bulletin and a higher
 * level is an "Aggiornamento" and a new fingerprint; a level that came down is not
 * news for a notification (the Journal writes it), which is why
 * [OfficialWarningEngine.notificationFor] reads the burnt prefixes, not just the
 * exact key.
 */
data class WarningNotification(
    val fingerprint: String,
    val warnings: PlaceWarnings
)
