package com.callbackdev.chiaro.domain.warnings

import com.callbackdev.chiaro.domain.model.Coordinates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Runs against the asset the app ships (`core/data/src/main/assets`, wired in as a test
 * resource of this module), so a rebuilt asset is re-measured here. Coordinates are
 * Open-Meteo's geocoding answers of 9 set 2026, fixed; the expected zones were checked
 * against the Dipartimento's own municipality lists.
 */
class WarningZoneIndexTest {

    private val index: WarningZoneIndex = WarningZoneIndex.decode(
        checkNotNull(javaClass.getResourceAsStream("/warning_zones_it.json")) {
            "warning_zones_it.json is not on the test classpath: see core/domain/build.gradle.kts"
        }.bufferedReader().use { it.readText() }
    )

    private fun locate(lat: Double, lon: Double, comune: String? = null): String? =
        index.locate(Coordinates(lat, lon), comune)?.code

    // --- the asset -----------------------------------------------------------------

    @Test
    fun `the asset carries every zone of the bulletin, each with a name and a region`() {
        assertEquals(187, index.size)
        assertEquals("20260908_1519", index.bulletinStamp)
        assertEquals(500.0, index.toleranceMeters, 0.0)
        for (zone in index.all) {
            assertTrue(zone.code, zone.code.matches(Regex("[A-Za-z]{4}-[A-Za-z0-9]{1,2}")))
            assertTrue(zone.code, zone.name.isNotBlank())
            assertTrue(zone.code, zone.region.isNotBlank())
        }
        assertEquals(
            WarningZone("Lomb-09", "Nodo Idraulico di Milano", "Lombardia"),
            index.zone("Lomb-09")
        )
        assertNull(index.zone("Lomb-99"))
    }

    @Test
    fun `thirteen zones were never named by their Region and carry their code as a name`() {
        val unnamed = index.all.filter { it.name == it.code }.map { it.code }
        assertEquals(
            listOf(
                "Basi-A1", "Basi-A2", "Basi-B", "Basi-C", "Basi-D", "Basi-E1", "Basi-E2",
                "Marc-1", "Marc-2", "Marc-3", "Marc-4", "Marc-5", "Marc-6"
            ),
            unnamed
        )
    }

    @Test
    fun `the lost apostrophe of the Valle d'Aosta names is restored`() {
        val names = index.all.filter { it.code.startsWith("VDAo") }.map { it.name }
        assertTrue(names.any { it.startsWith("Valle d'Aosta centrale") })
        assertTrue(names.none { '?' in it })
    }

    // --- twenty towns, one zone each ----------------------------------------------

    @Test
    fun `twenty known towns land in their zone by geometry alone`() {
        val table = listOf(
            Triple("Milano", 45.4643 to 9.1895, "Lomb-09"),
            Triple("Segrate", 45.4918 to 9.2981, "Lomb-09"),
            Triple("Roma", 41.8919 to 12.5113, "Lazi-D"),
            Triple("Napoli", 40.8522 to 14.2681, "Camp-1"),
            Triple("Torino", 45.0705 to 7.6868, "Piem-L"),
            Triple("Genova", 44.4048 to 8.9444, "Ligu-B"),
            Triple("Bologna", 44.4938 to 11.3387, "Emil-C2"),
            Triple("Firenze", 43.7792 to 11.2463, "Tosc-A3"),
            Triple("Venezia", 45.4371 to 12.3326, "Vene-F2"),
            Triple("Trieste", 45.6495 to 13.7768, "Friu-D"),
            Triple("Bolzano", 46.4907 to 11.3398, "Tren-A"),
            Triple("Aosta", 45.7376 to 7.3172, "VDAo-A"),
            Triple("Ancona", 43.6072 to 13.5103, "Marc-4"),
            Triple("Perugia", 43.1122 to 12.3888, "Umbr-E"),
            Triple("L'Aquila", 42.3505 to 13.3995, "Abru-B"),
            Triple("Campobasso", 41.5595 to 14.6674, "Moli-B"),
            Triple("Bari", 41.1207 to 16.8698, "Pugl-C"),
            Triple("Potenza", 40.6418 to 15.8079, "Basi-B"),
            Triple("Reggio Calabria", 38.1105 to 15.6613, "Cala-4"),
            Triple("Palermo", 38.1166 to 13.3636, "Sici-C"),
            Triple("Cagliari", 39.2305 to 9.1192, "Sard-B"),
            Triple("Cavenago d'Adda", 45.2825 to 9.5987, "Lomb-10")
        )
        for ((town, at, code) in table) {
            assertEquals(town, code, locate(at.first, at.second))
        }
    }

    @Test
    fun `a hamlet is placed by its coordinate, whatever the geocoder calls it`() {
        // Redecesio is a quarter of Segrate; Open-Meteo answers admin3 "Comune di Segrate".
        assertEquals("Lomb-09", locate(45.4819, 9.2720, "Comune di Segrate"))
    }

    @Test
    fun `three points at sea and one abroad are in no zone`() {
        assertNull("Tirreno", locate(40.0, 12.0))
        assertNull("Adriatico", locate(43.5, 15.0))
        assertNull("Ionio", locate(38.5, 17.5))
        assertNull("Lugano", locate(46.0037, 8.9511, "Lugano"))
    }

    // --- the border band ------------------------------------------------------------

    @Test
    fun `on a border the municipality decides, and without one the deeper zone does`() {
        // 172 m inside Piem-I's simplified polygon and 172 m from Lomb-12's: on the
        // Piemonte/Lombardia border, closer than the 500 m the simplification vouches for.
        val lat = 45.0353
        val lon = 8.8225
        assertEquals("Piem-I", locate(lat, lon))
        assertEquals("Piem-I", locate(lat, lon, "Agrate Conturbia"))
        assertEquals("Lomb-12", locate(lat, lon, "Alagna"))
        // A municipality neither zone lists cannot move the point.
        assertEquals("Piem-I", locate(lat, lon, "Comune di Milano"))
    }

    @Test
    fun `clear of every polygon the municipality is the last resort, and only if unambiguous`() {
        // 3 km off Genova's coast: a coastal town's geocoded point can sit in the water.
        assertEquals("Ligu-B", locate(44.3748, 8.9444, "Genova"))
        assertNull(locate(44.3748, 8.9444))
        // Roma is split across five zones: the name alone cannot choose.
        assertNull(locate(40.0, 12.0, "Comune di Roma"))
    }

    // --- one spelling for a municipality -------------------------------------------

    @Test
    fun `the municipality is matched whatever the prefix, case, accent or language`() {
        assertEquals("Emil-B1", locate(44.2227, 12.0407, "Comune di Forlì"))
        assertEquals("Emil-B1", locate(44.2227, 12.0407, "FORLI"))
        assertEquals("Tren-A", locate(46.4907, 11.3398, "Bozen"))
    }

    @Test
    fun `normalizeComune mirrors the importer`() {
        assertEquals("segrate", WarningZoneIndex.normalizeComune("Comune di Segrate"))
        assertEquals("forli", WarningZoneIndex.normalizeComune("Forlì"))
        assertEquals("sant'angelo lodigiano", WarningZoneIndex.normalizeComune("Sant’Angelo  Lodigiano "))
        assertEquals("citta sant'angelo", WarningZoneIndex.normalizeComune("Città Sant'Angelo"))
        assertEquals("", WarningZoneIndex.normalizeComune("   "))
    }
}
