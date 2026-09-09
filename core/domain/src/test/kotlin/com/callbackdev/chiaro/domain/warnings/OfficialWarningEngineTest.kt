package com.callbackdev.chiaro.domain.warnings

import com.callbackdev.chiaro.domain.warnings.WarningHazard.HYDRAULIC
import com.callbackdev.chiaro.domain.warnings.WarningHazard.HYDROGEOLOGICAL
import com.callbackdev.chiaro.domain.warnings.WarningHazard.THUNDERSTORM
import com.callbackdev.chiaro.domain.warnings.WarningLevel.NONE
import com.callbackdev.chiaro.domain.warnings.WarningLevel.ORANGE
import com.callbackdev.chiaro.domain.warnings.WarningLevel.RED
import com.callbackdev.chiaro.domain.warnings.WarningLevel.YELLOW
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The bulletin of 8 set 2026 15:19, in the shape the CAP parser will hand over. */
class OfficialWarningEngineTest {

    private val sept8: LocalDate = LocalDate.of(2026, 9, 8)
    private val sept9: LocalDate = sept8.plusDays(1)
    private val issued: LocalDateTime = sept8.atTime(15, 19)

    private val milano = WarningZone("Lomb-09", "Nodo Idraulico di Milano", "Lombardia")
    private val bolzano = WarningZone("Tren-A", "Provincia Autonoma di Bolzano", "Trentino-Alto Adige")
    private val trieste = WarningZone("Friu-D", "Bacino di Levante / Carso", "Friuli-Venezia Giulia")

    private fun bulletin(
        vararg rows: ZoneWarning,
        id: String = "DPC_BULLETIN_2026_09_08_6471",
        note: String? = null,
        days: List<LocalDate> = listOf(sept8, sept9)
    ) = WarningBulletin(id = id, issuedAt = issued, days = days, note = note, warnings = rows.toList())

    private fun row(zone: WarningZone, day: LocalDate, hazard: WarningHazard, level: WarningLevel) =
        ZoneWarning(zone.code, day, hazard, level)

    private fun levels(
        hydraulic: WarningLevel = NONE,
        hydrogeological: WarningLevel = NONE,
        thunderstorm: WarningLevel = NONE
    ) = mapOf(HYDRAULIC to hydraulic, HYDROGEOLOGICAL to hydrogeological, THUNDERSTORM to thunderstorm)

    // --- forPlace -------------------------------------------------------------------

    @Test
    fun `no zone is nothing to say`() {
        assertNull(OfficialWarningEngine.forPlace(bulletin(), zone = null, today = sept8))
    }

    @Test
    fun `a bulletin whose days are over is nothing to say`() {
        assertNull(OfficialWarningEngine.forPlace(bulletin(), milano, today = sept9.plusDays(1)))
    }

    @Test
    fun `a green zone is still an answer, with every hazard at NONE on every day`() {
        val place = OfficialWarningEngine.forPlace(
            bulletin(row(trieste, sept8, THUNDERSTORM, YELLOW)), milano, today = sept8
        )
        assertNotNull(place)
        assertEquals(NONE, place!!.maxLevel)
        assertEquals(listOf(sept8, sept9), place.days.map { it.date })
        assertTrue(place.days.all { it.levels == levels() })
        assertEquals(milano, place.zone)
        assertEquals("DPC_BULLETIN_2026_09_08_6471", place.bulletinId)
        assertEquals(issued, place.issuedAt)
    }

    @Test
    fun `the zone's rows fill the days, other zones' rows do not`() {
        val place = OfficialWarningEngine.forPlace(
            bulletin(
                row(milano, sept8, THUNDERSTORM, ORANGE),
                row(milano, sept8, HYDROGEOLOGICAL, YELLOW),
                row(milano, sept9, THUNDERSTORM, YELLOW),
                row(trieste, sept9, HYDRAULIC, RED)
            ),
            milano, today = sept8
        )!!
        assertEquals(levels(thunderstorm = ORANGE, hydrogeological = YELLOW), place.days[0].levels)
        assertEquals(levels(thunderstorm = YELLOW), place.days[1].levels)
        assertEquals(ORANGE, place.maxLevel)
        assertEquals(sept9.plusDays(1).atStartOfDay(), place.expiresAt)
    }

    @Test
    fun `before the afternoon bulletin, yesterday's tomorrow is today and the only day kept`() {
        val place = OfficialWarningEngine.forPlace(
            bulletin(
                row(milano, sept8, THUNDERSTORM, RED),
                row(milano, sept9, HYDRAULIC, YELLOW)
            ),
            milano, today = sept9
        )!!
        assertEquals(listOf(sept9), place.days.map { it.date })
        assertEquals(YELLOW, place.maxLevel)
    }

    @Test
    fun `the same row graded twice keeps the higher level`() {
        val place = OfficialWarningEngine.forPlace(
            bulletin(
                row(milano, sept8, HYDRAULIC, YELLOW),
                row(milano, sept8, HYDRAULIC, ORANGE)
            ),
            milano, today = sept8
        )!!
        assertEquals(ORANGE, place.days[0].levels.getValue(HYDRAULIC))
    }

    @Test
    fun `ranked lists the graded hazards highest first, then in the Dipartimento's order`() {
        val day = PlaceWarnings.DayWarnings(
            sept8, levels(hydraulic = YELLOW, hydrogeological = ORANGE, thunderstorm = YELLOW)
        )
        assertEquals(
            listOf(HYDROGEOLOGICAL to ORANGE, HYDRAULIC to YELLOW, THUNDERSTORM to YELLOW),
            day.ranked
        )
        assertEquals(emptyList<Pair<WarningHazard, WarningLevel>>(), PlaceWarnings.DayWarnings(sept8, levels()).ranked)
    }

    // --- the note -------------------------------------------------------------------

    private val note = "Per la giornata di oggi: Regione Lombardia e Campania: per la validità temporale " +
        "delle criticità si rimanda al bollettino regionale.\nPer la giornata di domani: Regione " +
        "Friuli Venezia Giulia e Provincia Autonoma di Bolzano: si rimanda al bollettino regionale."

    @Test
    fun `the note travels only to the zones whose region or name it mentions`() {
        fun noteFor(zone: WarningZone) =
            OfficialWarningEngine.forPlace(bulletin(note = note), zone, today = sept8)!!.note

        assertEquals(note, noteFor(milano))
        // The hyphen in the region's name is spelling: the note writes it without one.
        assertEquals(note, noteFor(trieste))
        // Named by its autonomous province, which is the zone's name.
        assertEquals(note, noteFor(bolzano))
        assertNull(noteFor(WarningZone("Sard-B", "Campidano", "Sardegna")))
    }

    @Test
    fun `no note in the bulletin, no note for the place`() {
        assertNull(OfficialWarningEngine.forPlace(bulletin(), milano, today = sept8)!!.note)
    }

    // --- notificationFor -------------------------------------------------------------

    private fun place(vararg rows: ZoneWarning, id: String = "DPC_BULLETIN_2026_09_08_6471") =
        OfficialWarningEngine.forPlace(bulletin(*rows, id = id), milano, today = sept8)!!

    private fun notify(
        place: PlaceWarnings,
        minLevel: WarningLevel = YELLOW,
        notified: Set<String> = emptySet(),
        cityKey: String = "3173435"
    ) = OfficialWarningEngine.notificationFor(place, minLevel, notified, cityKey)

    @Test
    fun `the fingerprint names the place, the bulletin and the level`() {
        val notification = notify(place(row(milano, sept9, THUNDERSTORM, ORANGE)))!!
        assertEquals("3173435:warn:DPC_BULLETIN_2026_09_08_6471:ORANGE", notification.fingerprint)
        assertEquals(ORANGE, notification.warnings.maxLevel)
    }

    @Test
    fun `green never notifies, whatever the threshold`() {
        assertNull(notify(place(), minLevel = YELLOW))
        assertNull(notify(place(), minLevel = NONE))
    }

    @Test
    fun `a level under the reader's threshold stays silent`() {
        val yellow = place(row(milano, sept8, HYDRAULIC, YELLOW))
        assertNotNull(notify(yellow, minLevel = YELLOW))
        assertNull(notify(yellow, minLevel = ORANGE))
        assertNotNull(notify(place(row(milano, sept8, HYDRAULIC, ORANGE)), minLevel = ORANGE))
    }

    @Test
    fun `a burnt fingerprint is not notified twice`() {
        val orange = place(row(milano, sept8, THUNDERSTORM, ORANGE))
        val burnt = notify(orange)!!.fingerprint
        assertNull(notify(orange, notified = setOf(burnt)))
    }

    @Test
    fun `an update that raises the level is news, one that lowers it is not`() {
        val yellow = place(row(milano, sept8, THUNDERSTORM, YELLOW))
        val orange = place(row(milano, sept8, THUNDERSTORM, ORANGE))
        val afterYellow = setOf(notify(yellow)!!.fingerprint)
        assertNotNull(notify(orange, notified = afterYellow))
        val afterOrange = afterYellow + notify(orange, notified = afterYellow)!!.fingerprint
        assertNull(notify(yellow, notified = afterOrange))
    }

    @Test
    fun `a new bulletin at the same level is a new fingerprint`() {
        val today = place(row(milano, sept8, THUNDERSTORM, YELLOW))
        val burnt = setOf(notify(today)!!.fingerprint)
        val tomorrowsBulletin = place(row(milano, sept9, THUNDERSTORM, YELLOW), id = "DPC_BULLETIN_2026_09_09_6472")
        assertEquals(
            "3173435:warn:DPC_BULLETIN_2026_09_09_6472:YELLOW",
            notify(tomorrowsBulletin, notified = burnt)!!.fingerprint
        )
    }

    @Test
    fun `two places in the same zone are told separately`() {
        val orange = place(row(milano, sept8, THUNDERSTORM, ORANGE))
        val burnt = setOf(notify(orange, cityKey = "3173435")!!.fingerprint)
        assertEquals("gps:warn:DPC_BULLETIN_2026_09_08_6471:ORANGE", notify(orange, notified = burnt, cityKey = "gps")!!.fingerprint)
    }

    // --- the model's own guards -----------------------------------------------------

    @Test(expected = IllegalArgumentException::class)
    fun `a day without every hazard is refused`() {
        PlaceWarnings.DayWarnings(sept8, mapOf(HYDRAULIC to YELLOW))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a bulletin covering no day is refused`() {
        bulletin(days = emptyList())
    }
}
