package com.callbackdev.chiaro.domain.warnings

import java.time.LocalDate

/**
 * Pure reading of a bulletin for one place: no clocks, no I/O, no Android — the
 * caller hands in the day (in the ISSUER's zone, Europe/Rome for the Dipartimento,
 * never the device's) and the zone the index found, so every rule here is
 * table-testable ([OfficialWarningEngineTest]).
 */
object OfficialWarningEngine {

    /**
     * What [bulletin] says about a place in [zone] as of [today], or `null` when there
     * is nothing to say: the place is in no zone (abroad, at sea), or every day the
     * bulletin covers is already over. Days already past are dropped — before the
     * afternoon's new bulletin, yesterday's "tomorrow" IS today and is the only day
     * kept. A bulletin that grades the zone green on every remaining day still comes
     * back, with every level NONE: that is Avvisi's "Nessuna allerta" and it is an
     * answer, not an absence.
     */
    fun forPlace(
        bulletin: WarningBulletin,
        zone: WarningZone?,
        today: LocalDate
    ): PlaceWarnings? {
        if (zone == null) return null
        val remaining = bulletin.days.filter { !it.isBefore(today) }.sorted()
        if (remaining.isEmpty()) return null
        return PlaceWarnings(
            zone = zone,
            bulletinId = bulletin.id,
            issuedAt = bulletin.issuedAt,
            days = remaining.map { day ->
                PlaceWarnings.DayWarnings(day, bulletin.levelsFor(zone.code, day))
            },
            note = bulletin.note?.takeIf { it.names(zone) }
        )
    }

    /**
     * The notification [warnings] earns, or `null`: nothing above NONE, a maximum
     * under the level the reader asked to be told from ([minLevel], YELLOW or ORANGE
     * in Avvisi), or a fingerprint already burnt for this bulletin at this level OR
     * HIGHER. The last clause is what keeps a level that came down from
     * re-notifying: the "Aggiornamento" that raises a bulletin from yellow to orange
     * is news, the correction that lowers it back is the Journal's.
     *
     * [notified] is the ring of burnt fingerprints the store keeps; [cityKey] is the
     * job's key for the place (`"gps"` for the position, the id otherwise), so two
     * saved places in the same zone are told separately, like every other alert.
     */
    fun notificationFor(
        warnings: PlaceWarnings,
        minLevel: WarningLevel,
        notified: Set<String>,
        cityKey: String
    ): WarningNotification? {
        val level = warnings.maxLevel
        if (level == WarningLevel.NONE || level < minLevel) return null
        val prefix = fingerprintPrefix(cityKey, warnings.bulletinId)
        val burntAtOrAbove = notified.any { burnt ->
            burnt.startsWith(prefix) &&
                WarningLevel.entries.firstOrNull { it.name == burnt.removePrefix(prefix) }
                    ?.let { it >= level } == true
        }
        if (burntAtOrAbove) return null
        return WarningNotification(fingerprint = prefix + level.name, warnings = warnings)
    }

    /** `"$cityKey:warn:$bulletinId:"` — the level goes after it. */
    fun fingerprintPrefix(cityKey: String, bulletinId: String): String = "$cityKey:warn:$bulletinId:"

    /**
     * Whether the issuer's note is about this zone: it names the zone's region ("Regione
     * Lombardia e Campania") or the zone itself (the two Trentino zones are named after
     * their autonomous province, which is how a note would refer to them). Compared
     * through [WarningZoneIndex.normalizeComune]: accents, apostrophes and the hyphen in
     * "Friuli-Venezia Giulia" are spelling, not meaning.
     */
    private fun String.names(zone: WarningZone): Boolean {
        val note = WarningZoneIndex.normalizeComune(this).replace('-', ' ')
        return listOf(zone.region, zone.name)
            .map { WarningZoneIndex.normalizeComune(it).replace('-', ' ') }
            .any { it.isNotEmpty() && it in note }
    }
}
