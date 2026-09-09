package com.callbackdev.chiaro.data.warnings

import com.callbackdev.chiaro.domain.warnings.DpcBulletinReader
import com.callbackdev.chiaro.domain.warnings.WarningHazard
import com.callbackdev.chiaro.domain.warnings.WarningLevel
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Against the real `Cap_20260908_1519.xml` of the Dipartimento (8 set 2026, 15:19):
 * the counts asserted here were measured on the file the day it was published — 8
 * blocks, 209 areas, two days, one national note.
 */
@RunWith(RobolectricTestRunner::class)
class CapParserTest {

    private fun cap() = checkNotNull(javaClass.getResourceAsStream("/dpc/Cap_20260908_1519.xml")) {
        "fixture missing: core/data/src/test/resources/dpc/Cap_20260908_1519.xml"
    }.use(CapParser::parse)

    @Test
    fun `the head of the document is read as it is`() {
        val alert = cap()
        assertEquals("DPC_BULLETIN_2026_09_08_6471", alert.identifier)
        assertEquals("2026-09-08T15:19:00+02:00", alert.sent)
        assertTrue(alert.note.orEmpty(), alert.note!!.startsWith("Per la giornata di oggi: Regione Lombardia e Campania"))
        assertTrue(alert.note!!.contains("Regione Liguria"))
    }

    @Test
    fun `eight blocks, two hundred and nine areas, every area with its zone code`() {
        val alert = cap()
        assertEquals(8, alert.infos.size)
        assertEquals(209, alert.infos.sumOf { it.areas.size })
        assertTrue(alert.infos.flatMap { it.areas }.all { DpcBulletinReader.ZONE_GEOCODE in it.geocodes })

        val first = alert.infos.first()
        assertEquals("ORDINARIA CRITICITA' PER RISCHIO IDRAULICO / ALLERTA GIALLA:", first.event)
        assertEquals("2026-09-08T12:00:00+02:00", first.onset)
        assertEquals("2026-09-08T23:59:59+02:00", first.expires)
        assertEquals("Moderate", first.severity)
        assertEquals(
            listOf("Versante Jonico Settentrionale" to "Cala-5", "Versante Jonico Centro-settentrionale" to "Cala-6"),
            first.areas.map { it.description to it.geocodes.getValue(DpcBulletinReader.ZONE_GEOCODE) }
        )
    }

    @Test
    fun `read into a bulletin, the file says what the map said that day`() {
        val bulletin = DpcBulletinReader.toBulletin(cap())
        assertEquals("DPC_BULLETIN_2026_09_08_6471", bulletin.id)
        assertEquals(LocalDateTime.of(2026, 9, 8, 15, 19), bulletin.issuedAt)
        assertEquals(listOf(LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 9)), bulletin.days)
        assertEquals(209, bulletin.warnings.size)
        // 33 + 10 orange blocks for tomorrow, the rest yellow; thunderstorms never red.
        assertEquals(43, bulletin.warnings.count { it.level == WarningLevel.ORANGE })
        assertEquals(166, bulletin.warnings.count { it.level == WarningLevel.YELLOW })
        assertEquals(0, bulletin.warnings.count { it.level == WarningLevel.RED })
        // The Versante Jonico Settentrionale was yellow on all three risks that day —
        // it is in the hydraulic block (2 areas), the thunderstorm one (50) and the
        // hydrogeological one (5) — and a zone the CAP never names is green throughout.
        assertEquals(
            WarningHazard.entries.associateWith { WarningLevel.YELLOW },
            bulletin.levelsFor("Cala-5", LocalDate.of(2026, 9, 8))
        )
        assertEquals(
            WarningHazard.entries.associateWith { WarningLevel.NONE },
            bulletin.levelsFor("Xxxx-0", LocalDate.of(2026, 9, 8))
        )
        assertEquals(LocalDateTime.of(2026, 9, 10, 0, 0), bulletin.expiresAt)
    }
}
