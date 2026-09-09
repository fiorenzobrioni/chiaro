package com.callbackdev.chiaro.domain.warnings

import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The labels are the Dipartimento's own, copied from the bulletins of 8–9 set 2026. */
class DpcBulletinReaderTest {

    @Test
    fun `hazards are read by their keyword, whatever surrounds it`() {
        assertEquals(WarningHazard.HYDRAULIC, DpcBulletinReader.hazardOf("ORDINARIA CRITICITA' PER RISCHIO IDRAULICO / ALLERTA GIALLA:"))
        assertEquals(WarningHazard.HYDROGEOLOGICAL, DpcBulletinReader.hazardOf("MODERATA CRITICITA' PER RISCHIO IDROGEOLOGICO / ALLERTA ARANCIONE:"))
        assertEquals(WarningHazard.THUNDERSTORM, DpcBulletinReader.hazardOf("ORDINARIA CRITICITA' PER RISCHIO TEMPORALI / ALLERTA GIALLA:"))
        assertEquals(WarningHazard.THUNDERSTORM, DpcBulletinReader.hazardOf("Ordinaria per rischio temporali / ALLERTA GIALLA"))
        assertNull(DpcBulletinReader.hazardOf("Precipitazioni previste"))
    }

    @Test
    fun `levels are read by their colour word, and the green label as NONE`() {
        assertEquals(WarningLevel.YELLOW, DpcBulletinReader.levelOf("ORDINARIA CRITICITA' PER RISCHIO IDRAULICO / ALLERTA GIALLA:"))
        assertEquals(WarningLevel.ORANGE, DpcBulletinReader.levelOf("Moderata / ALLERTA ARANCIONE"))
        assertEquals(WarningLevel.RED, DpcBulletinReader.levelOf("ELEVATA CRITICITA' PER RISCHIO IDRAULICO / ALLERTA ROSSA:"))
        assertEquals(WarningLevel.NONE, DpcBulletinReader.levelOf("Assenza di fenomeni significativi prevedibili / NESSUNA ALLERTA"))
        assertNull(DpcBulletinReader.levelOf("valutazione non trasmessa"))
    }

    private fun area(code: String) = CapArea("a zone", mapOf(DpcBulletinReader.ZONE_GEOCODE to code))

    private fun info(event: String, onset: String, vararg codes: String) = CapInfo(
        event = event,
        onset = onset,
        expires = onset.substring(0, 10) + "T23:59:59+02:00",
        severity = "Moderate",
        areas = codes.map(::area)
    )

    @Test
    fun `a CAP becomes a bulletin, one row per area of every readable block`() {
        val cap = CapAlert(
            identifier = " DPC_BULLETIN_2026_09_08_6471 ",
            sent = "2026-09-08T15:19:00+02:00",
            note = "Per la giornata di oggi: Regione Lombardia.\n",
            infos = listOf(
                info("ORDINARIA CRITICITA' PER RISCHIO IDRAULICO / ALLERTA GIALLA:", "2026-09-08T12:00:00+02:00", "Cala-5", "Cala-6"),
                info("MODERATA CRITICITA' PER RISCHIO TEMPORALI / ALLERTA ARANCIONE:", "2026-09-09T00:00:00+02:00", "Lomb-09")
            )
        )
        val bulletin = DpcBulletinReader.toBulletin(cap)
        assertEquals("DPC_BULLETIN_2026_09_08_6471", bulletin.id)
        assertEquals(LocalDateTime.of(2026, 9, 8, 15, 19), bulletin.issuedAt)
        assertEquals(listOf(LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 9)), bulletin.days)
        assertEquals("Per la giornata di oggi: Regione Lombardia.", bulletin.note)
        assertEquals(
            listOf(
                ZoneWarning("Cala-5", LocalDate.of(2026, 9, 8), WarningHazard.HYDRAULIC, WarningLevel.YELLOW),
                ZoneWarning("Cala-6", LocalDate.of(2026, 9, 8), WarningHazard.HYDRAULIC, WarningLevel.YELLOW),
                ZoneWarning("Lomb-09", LocalDate.of(2026, 9, 9), WarningHazard.THUNDERSTORM, WarningLevel.ORANGE)
            ),
            bulletin.warnings
        )
    }

    @Test
    fun `a block that names no hazard or no level is skipped, not guessed`() {
        val cap = CapAlert(
            identifier = "X",
            sent = "2026-09-08T15:19:00+02:00",
            note = null,
            infos = listOf(
                info("Precipitazioni previste / Elevati", "2026-09-08T12:00:00+02:00", "Cala-5"),
                info("ORDINARIA CRITICITA' PER RISCHIO IDRAULICO", "2026-09-08T12:00:00+02:00", "Cala-5"),
                CapInfo("ORDINARIA CRITICITA' PER RISCHIO IDRAULICO / ALLERTA GIALLA:", onset = null, expires = null, severity = null, areas = listOf(area("Cala-5")))
            )
        )
        assertEquals(emptyList<ZoneWarning>(), DpcBulletinReader.toBulletin(cap).warnings)
    }

    @Test
    fun `the bulletin always covers the day it was sent and the next, even when green`() {
        val cap = CapAlert("X", "2026-09-08T15:19:00+02:00", null, infos = emptyList())
        val bulletin = DpcBulletinReader.toBulletin(cap)
        assertEquals(listOf(LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 9)), bulletin.days)
        assertNull(bulletin.note)
        assertEquals(LocalDateTime.of(2026, 9, 10, 0, 0), bulletin.expiresAt)
    }

    @Test
    fun `an area without the zone geocode contributes no row`() {
        val cap = CapAlert(
            "X", "2026-09-08T15:19:00+02:00", null,
            infos = listOf(
                CapInfo(
                    "ORDINARIA CRITICITA' PER RISCHIO IDRAULICO / ALLERTA GIALLA:",
                    onset = "2026-09-08T12:00:00+02:00", expires = null, severity = null,
                    areas = listOf(CapArea("somewhere", mapOf("id_zona" to "12")))
                )
            )
        )
        assertEquals(emptyList<ZoneWarning>(), DpcBulletinReader.toBulletin(cap).warnings)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `an unreadable sent time is refused`() {
        DpcBulletinReader.toBulletin(CapAlert("X", "yesterday", null, emptyList()))
    }
}
