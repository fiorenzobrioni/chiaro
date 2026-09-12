package com.callbackdev.chiaro.ui.icons

import com.callbackdev.chiaro.data.WeatherIcons
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Il confine fra quel che il repo tiene e quel che l'APK spedisce.
 *
 * Il repo porta la famiglia **intera** di Meteocons v3 — 519 icone per quattro set — ma
 * le tabelle di [MeteoconsSets] nominano solo il sottoinsieme che una schermata disegna
 * (`tools/shipped_icons.py`), ed e' quello il motivo per cui l'APK pesa quanto la lista:
 * un drawable che nessuna tabella nomina non e' referenziato, e `shrinkResources` lo
 * toglie. Misurato l'11 set 2026: spedire anche le 90 gia' scelte ma non ancora cablate
 * costava 570 KB.
 *
 * Il confine pero' e' fragile in una direzione sola: aggiungere un `R.drawable.mc3_…` a
 * una schermata senza aggiungerlo alla lista compila benissimo e poi lancia in faccia al
 * lettore, perche' `styledRes` usa `getValue`. Questo test guarda i sorgenti e lo impedisce.
 */
class MeteoconsSetsTest {

    private val kotlinRef = Regex("""R\.drawable\.mc3_([a-z0-9_]+)""")
    private val layoutRef = Regex("""drawable/mc3n?_([a-z0-9_]+)""")

    private fun sources(dir: String, ext: String) =
        File(dir).walkTopDown().filter { it.isFile && it.extension == ext }

    /** Ogni disegno che il codice nomina sta nelle tabelle, in tutti e quattro i set. */
    @Test
    fun `every drawing a screen names is in the tables`() {
        val named = buildSet {
            sources("src/main/kotlin", "kt")
                .filter { it.name != "MeteoconsSets.kt" && it.name != "ComposedIcons.kt" }
                .forEach { f -> kotlinRef.findAll(f.readText()).forEach { add(it.groupValues[1]) } }
            sources("src/main/res/layout", "xml")
                .forEach { f -> layoutRef.findAll(f.readText()).forEach { add(it.groupValues[1]) } }
        }
        assertTrue("nessun riferimento a mc3_* — la famiglia si e' spostata?", named.isNotEmpty())

        val shipped = (MeteoconsSets.byName.keys + ComposedIcons.byName.keys)
            .map { it.replace("-", "_") }.toSet()
        val orphans = named - shipped
        assertTrue(
            "questi disegni sono nominati da una schermata ma non sono nella lista di " +
                "spedizione, quindi `styledRes` lancerebbe: $orphans\n" +
                "aggiungili a tools/shipped_icons.py e ri-esegui " +
                "tools/import_meteocons_v3.py (o, se sono disegni composti, " +
                "tools/compose_sun_cloud.py)",
            orphans.isEmpty()
        )
    }

    /** I quattro set hanno gli stessi membri: una faccia mancante e' un crash per chi
     * sceglie l'altro stile o accende il tema scuro, cioe' il caso che non si prova. */
    @Test
    fun `the four sets hold exactly the same drawings`() {
        val keys = MeteoconsSets.byName.values.toSet()
        assertEquals("line scuro", keys, MeteoconsSets.lineDarkOf.keys)
        assertEquals("flat chiaro", keys, MeteoconsSets.flatOf.keys)
        assertEquals("flat scuro", keys, MeteoconsSets.flatDarkOf.keys)
        keys.forEach { line ->
            WeatherIcons.entries.forEach { style ->
                listOf(false, true).forEach { dark ->
                    assertTrue(
                        "manca una faccia di $line ($style, scuro=$dark)",
                        ChiaroIcons.styledRes(line, style, dark) != 0
                    )
                }
            }
        }
    }

    /** Solo le condizioni si muovono (DESIGN §7.1), e ogni gemello e' di un disegno che
     * la lista contiene. */
    @Test
    fun `only conditions move, and each moving twin belongs to a shipped drawing`() {
        val shipped = MeteoconsSets.byName.values.toSet()
        assertTrue("un gemello animato senza il suo disegno fermo",
            MeteoconsSets.movingOf.keys.all { it in shipped })
        assertTrue("nessun disegno si muove", MeteoconsSets.movingOf.isNotEmpty())
        MeteoconsSets.movingOf.keys.forEach { line ->
            WeatherIcons.entries.forEach { style ->
                listOf(false, true).forEach { dark ->
                    assertTrue(
                        "manca un gemello di $line ($style, scuro=$dark)",
                        ChiaroIcons.movingRes(line, style, dark) != null
                    )
                }
            }
        }
    }
}
