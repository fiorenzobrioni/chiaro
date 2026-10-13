package com.callbackdev.chiaro.ui.icons

import com.callbackdev.chiaro.data.WeatherIcons
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Il disegno che il repo **compone** invece di importarlo (DESIGN.md §13.1, 12 set 2026).
 *
 * `sun-one-cloud` e' il sereno di Meteocons piu' una nuvoletta nell'angolo, `sun-half-cloud`
 * (13 ott 2026) lo stesso sereno con la nuvola al 78%, `sun-half-cloud-rain` e `-snow` (13 ott
 * 2026, gli acquazzoni) quest'ultimo con le gocce o i fiocchi di `partly-cloudy-*-rain/-snow`
 * appesi al pavimento della nuvola, e la ragione
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
        "sun_one_cloud_day" to "clear_day", "sun_one_cloud_night" to "clear_night",
        "sun_half_cloud_day" to "clear_day", "sun_half_cloud_night" to "clear_night",
        "sun_half_cloud_rain_day" to "clear_day", "sun_half_cloud_rain_night" to "clear_night",
        "sun_half_cloud_snow_day" to "clear_day", "sun_half_cloud_snow_night" to "clear_night"
    )

    /** Gli acquazzoni, e il disegno di Meteocons da cui vengono le loro gocce o i fiocchi. */
    private val falls = mapOf(
        "sun_half_cloud_rain_day" to "partly_cloudy_day_rain",
        "sun_half_cloud_rain_night" to "partly_cloudy_night_rain",
        "sun_half_cloud_snow_day" to "partly_cloudy_day_snow",
        "sun_half_cloud_snow_night" to "partly_cloudy_night_snow"
    )

    /** Quanti percorsi cadono in fondo al disegno [name]: tre gocce o tre fiocchi, o niente. */
    private fun fallCount(name: String) = falls[name]?.let { fallPaths("mc3_", name).size } ?: 0

    /** I percorsi che cadono nella sorgente: quelli con un nome, cioe' quelli che
     * l'animazione fa svanire (la nuvola e il sole non ne hanno). */
    private fun fallPaths(prefix: String, name: String): List<String> =
        Regex("""<path android:pathData="([^"]*)"\s+android:name=""")
            .findAll(text(prefix + falls.getValue(name))).map { it.groupValues[1] }.toList()

    /** La silhouette senza il tratto dritto in basso, aperta dalla fine di quel tratto:
     * la nuvola dello stile line negli acquazzoni, come quella dei disegni di pioggia di
     * Meteocons, da cui le gocce escono invece di attraversare una riga. */
    private fun floorless(silhouette: String): String {
        val tokens = silhouette.split(" ")
        val floor = tokens.indexOfFirst { it.startsWith("L") }
        val end = tokens[floor].removePrefix("L")
        return (listOf("M$end") + tokens.drop(floor + 1) + tokens.subList(1, floor)).joinToString(" ")
    }

    private val pathData = Regex("""android:pathData="([^"]*)"""")
    private val paint = Regex("""android:(?:strokeColor|fillColor)="(#[0-9a-fA-F]{6})"""")
    private val sunGroup = Regex(
        """<group android:name="g3sun" android:scaleX="([\d.]+)" android:scaleY="([\d.]+)""""
    )
    private val cloudGroup = Regex(
        """<group android:name="g6cloudplace" android:scaleX="([\d.]+)" android:scaleY="([\d.]+)""""
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
                // il primo percorso e' il buco, poi il sereno, poi la nuvola e quel che cade
                assertEquals(
                    "$prefix$name: il sole non e' piu' il disegno di $prefix$clear — " +
                        "ri-esegui tools/compose_sun_cloud.py",
                    paths(prefix + clear),
                    paths(prefix + name).drop(1).dropLast(1 + fallCount(name))
                )
            }
        }
    }

    /** La nuvola e' la silhouette di `cloudy` del suo stesso fondo, anche lei alla lettera;
     * negli acquazzoni dello stile line senza pavimento ([floorless]). */
    @Test
    fun `la nuvola del composto e' la silhouette di cloudy`() {
        composed.keys.forEach { name ->
            stillSets.forEach { (prefix, flat) ->
                val silhouette = paths("${flat}cloudy").single()
                val line = prefix == "mc3_" || prefix == "mc3n_"
                val skip = fallCount(name)
                assertEquals(
                    "$prefix$name: la nuvola non e' piu' quella di ${flat}cloudy — " +
                        "ri-esegui tools/compose_sun_cloud.py",
                    if (line && name in falls) floorless(silhouette) else silhouette,
                    paths(prefix + name).dropLast(skip).last()
                )
                assertEquals(
                    "$prefix$name: la nuvola non porta il colore di ${prefix}cloudy",
                    paint.findAll(text("${prefix}cloudy")).map { it.groupValues[1] }.single(),
                    paint.findAll(text(prefix + name)).map { it.groupValues[1] }.toList().dropLast(skip).last()
                )
            }
        }
    }

    /**
     * **Le gocce e i fiocchi sono quelli di Meteocons**, percorso per percorso e colore per
     * colore, e le gocce tornano a 4 a schermo come la nuvola: il gruppo le porta al 95%, e
     * lo strumento scrive il tratto diviso per quella scala.
     */
    @Test
    fun `quel che cade e' quello di Meteocons, alla lettera`() {
        val fallGroup = Regex(
            """<group android:name="g7fallplace" android:scaleX="([\d.]+)" android:scaleY="([\d.]+)""""
        )
        falls.forEach { (name, source) ->
            stillSets.keys.forEach { prefix ->
                val expected = fallPaths(prefix, name)
                assertTrue("$prefix$source: nessun percorso che cade", expected.size == 3)
                assertEquals(
                    "$prefix$name: quel che cade non e' piu' quello di $prefix$source — " +
                        "ri-esegui tools/compose_sun_cloud.py",
                    expected,
                    paths(prefix + name).takeLast(expected.size)
                )
                assertEquals(
                    "$prefix$name: quel che cade non porta i colori di $prefix$source",
                    paint.findAll(text(prefix + source)).map { it.groupValues[1] }.toList().takeLast(3),
                    paint.findAll(text(prefix + name)).map { it.groupValues[1] }.toList().takeLast(3)
                )
                val body = text(prefix + name)
                val scale = fallGroup.find(body)!!.groupValues[1].toDouble()
                strokeWidth.findAll(body.substringAfter("g7fallplace")).forEach {
                    assertEquals(
                        "$prefix$name: una goccia verrebbe tracciata a ${it.groupValues[1].toDouble() * scale}",
                        4.0,
                        it.groupValues[1].toDouble() * scale,
                        0.005
                    )
                }
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
     *
     * La scala sta su `g6cloudplace` e non sul guscio `g5cloud`: e' il guscio che porta
     * il dondolio, e un gruppo animato non puo' portare anche trasformazioni sue
     * (`AnimatedIconTest`).
     */
    @Test
    fun `il contorno della nuvola resta la linea della famiglia`() {
        composed.keys.forEach { name ->
            listOf("mc3_", "mc3n_").forEach { prefix ->
                val body = text(prefix + name)
                val scale = cloudGroup.find(body)!!.groupValues[1].toDouble()
                val cloud = body.substringAfter("g6cloudplace").substringBefore("g7fallplace")
                val stroke = strokeWidth.findAll(cloud).map { it.groupValues[1].toDouble() }.last()
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

    /**
     * **I codici 1 e 2 arrivano davvero al disegno composto, per tutte le strade.**
     *
     * `conditionLineRes` è solo la prima: `styledRes` deve trovare la faccia per lo stile
     * e il fondo, e `movingRes` il gemello animato. Sono tre tabelle diverse, e una
     * dimenticata qui non rompe la compilazione — lascia un'icona vuota a chi cambia
     * stile, o una che non si muove a chi accende il movimento. Il codice 2 si è aggiunto
     * il 13 ott 2026, e con lui gli acquazzoni di pioggia e di neve (80, 81, 85, 86).
     */
    @Test
    fun `quasi sereno, poco nuvoloso e acquazzoni si trovano in tutte e quattro le facce, fermi e animati`() {
        listOf(1, 2, 80, 81, 85, 86).forEach { code ->
            listOf(false, true).forEach { night ->
                val line = ChiaroIcons.conditionLineRes(code, night)
                assertTrue("il codice $code non prende il disegno composto",
                    line in ComposedIcons.byName.values)
                listOf(WeatherIcons.LINE, WeatherIcons.FILL).forEach { style ->
                    listOf(false, true).forEach { dark ->
                        assertTrue(
                            "nessuna faccia ferma per il codice $code ($style, scuro=$dark)",
                            ChiaroIcons.styledRes(line, style, dark) != 0
                        )
                        assertNotNull(
                            "il codice $code non ha il gemello animato ($style, scuro=$dark)",
                            ChiaroIcons.movingRes(line, style, dark)
                        )
                    }
                }
            }
        }
    }

    /** Nessun nome sta in tutte e due le tabelle: se ci finisse, `styledRes` prenderebbe
     * la composta e la famiglia avrebbe una faccia che nessuno guarda piu'. */
    @Test
    fun `nessun disegno sta in tutte e due le tabelle`() {
        val both = ComposedIcons.byName.values.toSet() intersect MeteoconsSets.byName.values.toSet()
        assertTrue("questi disegni stanno sia nella famiglia sia fra i composti: $both", both.isEmpty())
    }
}
