package com.callbackdev.chiaro.domain.warnings

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.Locale

/**
 * Reads the Dipartimento della Protezione Civile's criticality bulletin out of its
 * CAP, by the keywords its labels are made of — the CAP has no coded level or hazard,
 * only strings like «ORDINARIA CRITICITA' PER RISCHIO IDRAULICO / ALLERTA GIALLA:»
 * (measured on the bulletins of 8 and 9 set 2026; the same words label the shapefile,
 * «Ordinaria / ALLERTA GIALLA»). Pure: a [CapAlert] in, a [WarningBulletin] out.
 *
 * An `<info>` whose event names no hazard or no level is skipped rather than guessed:
 * an unreadable block is a block the bulletin did not say, never one it said at a
 * level this code invented.
 */
object DpcBulletinReader {

    /** The `<geocode>` that carries the zone code (`Lomb-09`) in every area. */
    const val ZONE_GEOCODE = "Zona di Allerta"

    fun hazardOf(event: String): WarningHazard? {
        val label = event.uppercase(Locale.ROOT)
        return when {
            "IDROGEOLOGIC" in label -> WarningHazard.HYDROGEOLOGICAL
            "IDRAULIC" in label -> WarningHazard.HYDRAULIC
            "TEMPORAL" in label -> WarningHazard.THUNDERSTORM
            else -> null
        }
    }

    fun levelOf(event: String): WarningLevel? {
        val label = event.uppercase(Locale.ROOT)
        return when {
            "ROSSA" in label -> WarningLevel.RED
            "ARANCIONE" in label -> WarningLevel.ORANGE
            "GIALLA" in label -> WarningLevel.YELLOW
            "NESSUNA ALLERTA" in label -> WarningLevel.NONE
            else -> null
        }
    }

    /**
     * The bulletin the CAP describes. Its days are the onsets of its blocks together
     * with the day it was sent and the one after — the bulletin always speaks of
     * "oggi" and "domani", and a day it grades green everywhere has no block to be
     * found in.
     */
    fun toBulletin(cap: CapAlert): WarningBulletin {
        val issuedAt = localDateTime(cap.sent)
            ?: throw IllegalArgumentException("unreadable <sent>: ${cap.sent}")
        val rows = mutableListOf<ZoneWarning>()
        val onsets = mutableSetOf<LocalDate>()
        for (info in cap.infos) {
            val hazard = hazardOf(info.event) ?: continue
            val level = levelOf(info.event) ?: continue
            val day = info.onset?.let(::localDate) ?: continue
            onsets += day
            if (level == WarningLevel.NONE) continue
            for (area in info.areas) {
                val code = area.geocodes[ZONE_GEOCODE]?.trim()?.takeIf { it.isNotEmpty() } ?: continue
                rows += ZoneWarning(zoneCode = code, day = day, hazard = hazard, level = level)
            }
        }
        val sentDay = issuedAt.toLocalDate()
        return WarningBulletin(
            id = cap.identifier.trim(),
            issuedAt = issuedAt,
            days = (onsets + sentDay + sentDay.plusDays(1)).sorted(),
            note = cap.note?.trim()?.takeIf { it.isNotEmpty() },
            warnings = rows
        )
    }

    /** The issuer writes its own local offset: the wall-clock time IS the local time. */
    private fun localDateTime(iso: String): LocalDateTime? =
        runCatching { OffsetDateTime.parse(iso.trim()).toLocalDateTime() }.getOrNull()

    private fun localDate(iso: String): LocalDate? = localDateTime(iso)?.toLocalDate()
}
