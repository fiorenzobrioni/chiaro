package com.callbackdev.chiaro.ui.icons

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Il disegno che il repo **compone** invece di importarlo (DESIGN.md §13.1, 12 set 2026).
 *
 * `sun-one-cloud` e' il sereno di Meteocons piu' una nuvoletta nell'angolo, e la ragione
 * per cui puo' esistere senza che nessuno disegni a mano e' che ogni suo pezzo resta il
 * pezzo originale: il sole e' `clear-day` con i suoi percorsi e la sua scala, la nuvola
 * e' la silhouette di `cloudy`. Lo compone `tools/compose_sun_cloud.py`, e il solo
 * percorso calcolato e' il buco della maschera.
 *
 * Quel che questo test tiene fermo e' esattamente quella promessa, perche' e' anche il
 * punto in cui la faccenda si romperebbe in silenzio: `import_meteocons_v3.py` non tocca
 * questi file, ma **puo' cambiare i disegni da cui vengono** — un aggiornamento di
 * Meteocons, un altro riancoraggio dei colori — e allora il composto resterebbe indietro
 * senza che niente diventi rosso. Un confronto alla lettera fra il composto e le sue
 * sorgenti e' l'unica cosa che se ne accorge, e dice anche cosa fare: ri-eseguire lo
 * strumento.
 *
 * Le altre quattro cose che contano sui file (le quattro facce ci sono tutte, il gemello
 * animato e' lo stesso disegno, i colori tengono il 3:1 sul loro fondo, il perno della
 * scala e' il centro) sono gia' coperte dai test che camminano i prefissi: i nomi
 * composti portano gli stessi prefissi apposta.
 */
class ComposedIconsTest {

    private val drawables = File("src/main/res/drawable")

    /** I quattro set fermi, e da quale set ognuno prende la silhouette della nuvola. */
    private val stillSets = mapOf(
        "mc3_" to "mc3f_", "mc3n_" to "mc3fn_", "mc3f_" to "mc3f_", "mc3fn_" to "mc3fn_"
    )
    private val movingSets = listOf("mc3a_", "mc3an_", "mc3fa_", "mc3fan_")

    /** I nomi composti, e il disegno del sereno da cui viene ognuno. */
    private val composed = mapOf(
        "sun_one_cloud_day" to "clear_day", "sun_one_cloud_night" to "clear_night"
    )

    private val pathData = Regex("""android:pathData="([^"]*)"""")
    private val paint = Regex("""android:(?:strokeColor|fillColor)="(#[0-9a-fA-F]{6})"""")
    private val sunGroup = Regex(
        """<group android:name="g3sun" android:scaleX="([\d.]+)" android:scaleY="([\d.]+)""""
    )
    private val cloudGroup = Regex(
        """<group android:name="g5cloud" android:scaleX="([\d.]+)" android:scaleY="([\d.]+)""""
    )
    private val strokeWidth = Regex("""android:strokeWidth="([\d.]+)"""")

    private fun text(name: String): String {
        val file = File(drawables, "$name.xml")
        assertTrue(
            "$name.xml non c'e' — esegui tools/compose_sun_cloud.py",
            file.isFile
        )
        return file.readText()
    }

    private fun paths(name: String) = pathData.findAll(text(name)).map { it.groupValues[1] }.toList()

    @Test
    fun `ogni faccia del disegno composto esiste, ferma e animata`() {
        val missing = buildList {
            composed.keys.forEach { name ->
                (stillSets.keys + movingSets).forEach { prefix ->
                    if (!File(drawables, "$prefix$name.xml").isFile) add("$prefix$name.xml")
                }
            }
        }
        assertTrue(
            "mancano delle facce — esegui tools/compose_sun_cloud.py: $missing",
            missing.isEmpty()
        )
    }

    /**
     * Il sole del composto **e' quello di sereno**, non un sereno somigliante: stessi
     * percorsi nello stesso ordine, stessa scala, stesso perno. E' la promessa che ha
     * fatto scegliere questa composizione fra le due guardate il 12 set 2026.
     */
    @Test
    fun `il sole del composto e' quello di sereno, alla lettera`() {
        composed.forEach { (name, clear) ->
            stillSets.keys.forEach { prefix ->
                val body = text("$prefix$name")
                val sun = sunGroup.find(body)
                assertNotNull("$prefix$name: nessun gruppo g3sun", sun)
                val source = text("$prefix$clear")
                val sourceScale = Regex("""<group android:name="mc3scale" android:scaleX="([\d.]+)"""")
                    .find(source)!!.groupValues[1]
                assertEquals(
                    "$prefix$name: il sole non porta la scala di $prefix$clear",
                    sourceScale,
                    sun!!.groupValues[1]
                )
                // il primo percorso e' il buco, l'ultimo la nuvola: in mezzo c'e' il sereno
                assertEquals(
                    "$prefix$name: il sole non e' piu' il disegno di $prefix$clear — " +
                        "ri-esegui tools/compose_sun_cloud.py",
                    paths(prefix + clear),
                    paths(prefix + name).drop(1).dropLast(1)
                )
            }
        }
    }

    /** La nuvola e' la silhouette di `cloudy` del suo stesso fondo, anche lei alla lettera. */
    @Test
    fun `la nuvola del composto e' la silhouette di cloudy`() {
        composed.keys.forEach { name ->
            stillSets.forEach { (prefix, flat) ->
                assertEquals(
                    "$prefix$name: la nuvola non e' piu' quella di ${flat}cloudy — " +
                        "ri-esegui tools/compose_sun_cloud.py",
                    paths("${flat}cloudy").single(),
                    paths(prefix + name).last()
                )
                assertEquals(
                    "$prefix$name: la nuvola non porta il colore di ${prefix}cloudy",
                    paint.findAll(text("${prefix}cloudy")).map { it.groupValues[1] }.single(),
                    paint.findAll(text(prefix + name)).map { it.groupValues[1] }.last()
                )
            }
        }
    }

    /**
     * **Il contorno della nuvola torna a 4 dopo la scala del gruppo.**
     *
     * E' il punto per cui la nuvoletta e' ri-tracciata invece che rimpicciolita: il
     * gruppo la porta a meno di meta' taglia, e se il tratto scendesse con lei
     * resterebbe largo 2,0 contro i 3,7 del sole, cioe' uno sbaffo. Lo strumento scrive
     * `strokeWidth` = 4 / scala apposta, e questo e' il numero che non deve tornare a 4.
     */
    @Test
    fun `il contorno della nuvola resta la linea della famiglia`() {
        composed.keys.forEach { name ->
            listOf("mc3_", "mc3n_").forEach { prefix ->
                val body = text(prefix + name)
                val scale = cloudGroup.find(body)!!.groupValues[1].toDouble()
                val stroke = strokeWidth.findAll(body).map { it.groupValues[1].toDouble() }.last()
                assertEquals(
                    "$prefix$name: la nuvola verrebbe tracciata a ${stroke * scale} " +
                        "invece che a 4 unita'",
                    4.0,
                    stroke * scale,
                    0.005
                )
            }
        }
    }

    /** Il buco e' lo stesso in tutte e quattro le facce: dipende da dove sta la nuvola,
     * non da come e' dipinta. Una faccia con un ritaglio suo taglierebbe il sole in un
     * punto diverso al cambio di stile. */
    @Test
    fun `il buco della maschera e' lo stesso in tutte le facce`() {
        composed.keys.forEach { name ->
            val holes = stillSets.keys.map { paths(it + name).first() }.toSet()
            assertEquals("$name: le facce non ritagliano lo stesso buco", 1, holes.size)
        }
    }

    /** Le tabelle composte hanno gli stessi membri, come quelle importate: una faccia
     * che manca e' un'eccezione per chi cambia stile o accende il tema scuro. */
    @Test
    fun `le tabelle composte tengono gli stessi disegni`() {
        val keys = ComposedIcons.byName.values.toSet()
        assertTrue("la tabella composta e' vuota", keys.isNotEmpty())
        assertEquals("line scuro", keys, ComposedIcons.lineDarkOf.keys)
        assertEquals("flat chiaro", keys, ComposedIcons.flatOf.keys)
        assertEquals("flat scuro", keys, ComposedIcons.flatDarkOf.keys)
        assertEquals("animati", keys, ComposedIcons.movingOf.keys)
    }

    /** Nessun nome sta in tutte e due le tabelle: se ci finisse, `styledRes` prenderebbe
     * la composta e la famiglia avrebbe una faccia che nessuno guarda piu'. */
    @Test
    fun `nessun disegno sta in tutte e due le tabelle`() {
        val both = ComposedIcons.byName.values.toSet() intersect MeteoconsSets.byName.values.toSet()
        assertTrue("questi disegni stanno sia nella famiglia sia fra i composti: $both", both.isEmpty())
    }
}
