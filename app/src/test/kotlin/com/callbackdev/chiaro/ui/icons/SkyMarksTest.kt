package com.callbackdev.chiaro.ui.icons

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * I segni del cielo composti da `tools/compose_sky_marks.py` (revisione del Cielo, punto 7,
 * 14 ott 2026; rivisti il 9 nov 2026): la meteora, la luna con la stella, le due stelle, il
 * sole sotto l'orizzonte con le prime stelle, le tre stelle, la luna piena che sorge,
 * l'eclissi di luna e la luce zodiacale. Come per [ComposedIconsTest], la promessa e' che i
 * pezzi sono i disegni di Meteocons **alla lettera** — percorsi e colori, faccia per faccia —
 * e che la linea resta quella della famiglia. Dal 9 nov 2026 lo strumento disegna anche
 * qualche tratto (le scie, l'orizzonte, il sole tratteggiato, il cono, l'ombra): la promessa,
 * per quelli, e' che i colori sono presi dai disegni della famiglia nella stessa faccia e che
 * un tratto ha la linea della famiglia. Se l'importatore riscrive le sorgenti e lo strumento
 * non e' stato rifatto girare, questi test se ne accorgono.
 */
class SkyMarksTest {

    private val drawables = File("src/main/res/drawable")
    private val faces = listOf("mc3_", "mc3n_", "mc3f_", "mc3fn_")

    /** Ogni segno, con le sorgenti dei suoi pezzi nell'ordine in cui li disegna. */
    private val recipes = mapOf(
        "meteor" to listOf("star"),
        "moon_star" to listOf("clear_night", "star"),
        "two_stars" to listOf("star", "star"),
        "horizon_star" to listOf("star", "star"),
        "three_stars" to listOf("star", "star", "star"),
        "full_moonrise" to listOf("moonrise"),
        "lunar_eclipse" to listOf("moon_full"),
        "zodiacal_light" to listOf("star", "star")
    )

    /** I tratti disegnati di ogni segno, nell'ordine: da quale disegno della famiglia prende
     * il colore (il suo n-esimo colore nel file della stessa faccia), e se e' un tratto nella
     * linea della famiglia (`true`) o una forma piena (`false`). */
    private val drawn = mapOf(
        "meteor" to List(3) { Triple("star", 0, true) },
        "horizon_star" to listOf(Triple("horizon", 0, true)) + List(5) { Triple("horizon", 1, true) },
        "lunar_eclipse" to listOf(Triple("code_red", 0, false)),
        "zodiacal_light" to listOf(Triple("horizon", 1, false), Triple("horizon", 0, true))
    )

    private val pathData = Regex("""android:pathData="([^"]*)"""")
    private val paint = Regex("""android:(?:strokeColor|fillColor)="(#[0-9a-fA-F]{6})"""")
    private val strokeWidth = Regex("""android:strokeWidth="([\d.]+)"""")
    private val pieceScale = Regex("""android:name="piece\d+" [^>]*android:scaleX="([\d.]+)"""")
    private val pieceStart = Regex("""<group android:name="piece\d+" """)
    private val drawnGroup = Regex("""<group android:name="drawn\d+">.*?</group>""", RegexOption.DOT_MATCHES_ALL)
    private val drawnPath = Regex("""<path android:name="drawn\d+p"[^>]*/>""")
    private val moonGroup = Regex("""<group android:name="g4moon".*?</group>""", RegexOption.DOT_MATCHES_ALL)
    private val crescent = Regex("""<group android:name="g1moon".*?</group>""", RegexOption.DOT_MATCHES_ALL)

    private fun text(name: String): String {
        val file = File(drawables, "$name.xml")
        assertTrue("manca $name.xml — esegui tools/compose_sky_marks.py", file.isFile)
        return file.readText()
    }

    /** Il segno senza i suoi tratti disegnati: i soli pezzi. */
    private fun pieces(body: String) = body.replace(drawnGroup, "")

    /** La sorgente come la usa il segno: la luna che sorge con il disco pieno al posto della
     * falce. */
    private fun source(prefix: String, name: String, forMark: String): String {
        val body = text("$prefix$name")
        return when {
            forMark == "full_moonrise" && name == "moonrise" -> {
                val disc = crescent.find(text("${prefix}moon_full"))!!.value
                crescent.replace(body) { disc }
            }
            else -> body
        }
    }

    @Test
    fun `ogni segno ha le quattro facce ferme e sta nelle tabelle, senza gemelli animati`() {
        recipes.keys.forEach { name ->
            faces.forEach { prefix -> text("$prefix$name") }
            listOf("mc3a_", "mc3an_", "mc3fa_", "mc3fan_").forEach { prefix ->
                assertTrue("$prefix$name non deve esistere: nel cielo non si muove niente",
                    !File(drawables, "$prefix$name.xml").exists())
            }
        }
        assertEquals(
            setOf(
                "meteor", "moon-star", "two-stars", "horizon-star", "three-stars", "full-moonrise",
                "lunar-eclipse", "zodiacal-light"
            ),
            SkyMarks.byName.keys
        )
        listOf(SkyMarks.lineDarkOf, SkyMarks.flatOf, SkyMarks.flatDarkOf).forEach { table ->
            assertEquals(SkyMarks.byName.values.toSet(), table.keys)
        }
    }

    @Test
    fun `i pezzi sono i disegni di Meteocons alla lettera, percorsi e colori`() {
        recipes.forEach { (mark, sources) ->
            faces.forEach { prefix ->
                val body = pieces(text("$prefix$mark"))
                val expectedPaths = sources.flatMap { src ->
                    pathData.findAll(source(prefix, src, mark)).map { it.groupValues[1] }.toList()
                }
                val expectedPaint = sources.flatMap { src ->
                    paint.findAll(source(prefix, src, mark)).map { it.groupValues[1] }.toList()
                }
                assertEquals("$prefix$mark: percorsi", expectedPaths, pathData.findAll(body).map { it.groupValues[1] }.toList())
                assertEquals("$prefix$mark: colori", expectedPaint, paint.findAll(body).map { it.groupValues[1] }.toList())
            }
        }
    }

    /** Un tratto disegnato non inventa un colore: lo prende dal disegno della famiglia che la
     * ricetta nomina, nella stessa faccia; e un tratto ha la linea della famiglia, 4 unita'. */
    @Test
    fun `i tratti disegnati hanno i colori della famiglia e la sua linea`() {
        faces.forEach { prefix ->
            recipes.keys.forEach { mark ->
                val expected = drawn[mark].orEmpty()
                val found = drawnPath.findAll(text("$prefix$mark")).map { it.value }.toList()
                assertEquals("$prefix$mark: quanti tratti", expected.size, found.size)
                found.zip(expected).forEach { (path, rule) ->
                    val (src, index, isStroke) = rule
                    val colour = paint.findAll(text("$prefix$src")).map { it.groupValues[1] }.toList()[index]
                    assertTrue("$prefix$mark: il colore viene da $src", path.contains("=\"$colour\""))
                    if (isStroke) {
                        assertTrue("$prefix$mark: la linea della famiglia", path.contains("android:strokeWidth=\"4\""))
                        assertTrue("$prefix$mark: un tratto, non una forma piena", !path.contains("fillColor"))
                    }
                }
            }
        }
    }

    /** L'ora blu: il sole sotto l'orizzonte, tratteggiato, e le prime stelle; niente cupola
     * sopra la linea, che era il sole ancora sopra l'orizzonte. */
    @Test
    fun `il sole dell'ora blu sta sotto l'orizzonte, tratteggiato, sotto due stelle`() {
        val arc = Regex("""android:pathData="M[\d.]+,([\d.]+) A17,17,0,0,1,[\d.]+,([\d.]+)"""")
        faces.forEach { prefix ->
            val body = text("${prefix}horizon_star")
            assertTrue("$prefix: la linea dell'orizzonte", body.contains("M26,78 L102,78"))
            val dashes = arc.findAll(body).toList()
            assertEquals("$prefix: i trattini del sole", 5, dashes.size)
            dashes.forEach { dash ->
                dash.groupValues.drop(1).forEach { y -> assertTrue("$prefix: un trattino sopra la linea", y.toDouble() > 78) }
            }
            assertTrue("$prefix: la cupola di horizon", !body.contains("M46,82 A18,18"))
            assertEquals("$prefix: le stelle", 2, pieceStart.findAll(body).count())
        }
    }

    @Test
    fun `la meteora e' una stella con tre scie, senza la luna`() {
        faces.forEach { prefix ->
            val body = text("${prefix}meteor")
            val moon = moonGroup.find(text("${prefix}falling_stars"))!!.value
            val moonPath = pathData.find(moon)!!.groupValues[1]
            assertTrue("$prefix: la luna e' rimasta", moonPath !in body)
            assertEquals("$prefix: la stella", 1, pieceStart.findAll(body).count())
            assertEquals("$prefix: le scie", 3, drawnPath.findAll(body).count())
        }
    }

    /** L'eclissi e la luna piena non sono piu' lo stesso disegno: l'ombra rossa sta solo
     * dentro la luna. */
    @Test
    fun `l'ombra dell'eclissi e' rossa e ritagliata dentro la luna`() {
        faces.forEach { prefix ->
            val body = text("${prefix}lunar_eclipse")
            assertTrue("$prefix: il ritaglio della luna", body.contains("<clip-path android:pathData=\"M21.91,64"))
            assertTrue("$prefix: l'ombra e' diversa dalla luna piena", body != text("${prefix}moon_full"))
        }
    }

    @Test
    fun `un pezzo rimpicciolito tiene la linea della famiglia`() {
        recipes.forEach { (mark, sources) ->
            faces.forEach { prefix ->
                val body = pieces(text("$prefix$mark"))
                // Each piece runs from its own group to the next one's: its scale group and
                // its paths come with it.
                val starts = pieceStart.findAll(body).map { it.range.first }.toList()
                val pieces = starts.mapIndexed { i, from -> body.substring(from, starts.getOrElse(i + 1) { body.length }) }
                assertEquals("$prefix$mark: pezzi", sources.size, pieces.size)
                pieces.zip(sources).forEach { (piece, src) ->
                    val k = pieceScale.find(piece)!!.groupValues[1].toDouble()
                    val drawn = strokeWidth.findAll(piece).map { it.groupValues[1].toDouble() * k }.toList()
                    val original = strokeWidth.findAll(source(prefix, src, mark)).map { it.groupValues[1].toDouble() }.toList()
                    assertEquals("$prefix$mark ($src): quanti contorni", original.size, drawn.size)
                    drawn.zip(original).forEach { (d, o) -> assertEquals("$prefix$mark ($src)", o, d, 0.001) }
                }
            }
        }
    }
}
