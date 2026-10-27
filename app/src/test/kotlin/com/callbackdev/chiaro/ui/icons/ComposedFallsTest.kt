package com.callbackdev.chiaro.ui.icons

import androidx.compose.ui.graphics.Color
import com.callbackdev.chiaro.data.WeatherIcons
import com.callbackdev.chiaro.ui.theme.ChiaroDarkScheme
import com.callbackdev.chiaro.ui.theme.ChiaroLightScheme
import com.callbackdev.chiaro.ui.theme.VividDarkScheme
import com.callbackdev.chiaro.ui.theme.VividLightScheme
import java.io.File
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Le icone che mancavano, composte da `tools/compose_falls.py` (Fase 14, step 14.13, 27 ott
 * 2026): la pioggia e la pioviggine gelata (quel che cade piu' il cristallo di `snowflake`
 * in basso, dove il ghiaccio si forma), la pioggia e la neve possibile (la nuvola di
 * `drizzle` e di `snow` con una goccia e un fiocco in meno, piu' chiari).
 *
 * Come per [ComposedIconsTest] e [SkyMarksTest], la promessa e' che i pezzi sono i disegni
 * di Meteocons **alla lettera** — percorsi e colori, faccia per faccia, meno il gruppo che
 * la ricetta toglie — che la linea di un pezzo rimpicciolito resta quella della famiglia,
 * e che le animazioni sono quelle della sorgente. Le due cose che lo strumento cambia
 * davvero sono misurate qui: la chiarezza dei disegni «possibile» tiene il 3:1 sul fondo
 * per cui la faccia e' scelta (DESIGN §10, sugli schemi veri, non sulla tabella dello
 * strumento) ed e' la piu' chiara che lo tiene; e il gemello animato porta la stessa
 * chiarezza nei fotogrammi, perche' un `objectAnimator` sostituisce l'alfa e non lo
 * somma. Se l'importatore riscrive le sorgenti e lo strumento non e' stato rifatto girare,
 * questi test se ne accorgono.
 */
class ComposedFallsTest {

    private val drawables = File("src/main/res/drawable")
    private val faces = mapOf("mc3_" to "mc3a_", "mc3n_" to "mc3an_", "mc3f_" to "mc3fa_", "mc3fn_" to "mc3fan_")

    /** Un pezzo: la sorgente, i gruppi che la ricetta toglie, se quel che cade e' schiarito. */
    private data class Piece(val source: String, val dropped: Set<String> = emptySet(), val light: Boolean = false)

    private val recipes = mapOf(
        "freezing_rain" to listOf(Piece("rain"), Piece("snowflake")),
        "freezing_drizzle" to listOf(Piece("drizzle"), Piece("snowflake")),
        "rain_possible" to listOf(Piece("drizzle", setOf("g4raindrop2"), light = true)),
        "snow_possible" to listOf(Piece("snow", setOf("g4snowflake2"), light = true))
    )

    private val codes = mapOf(
        56 to "freezing_drizzle", 57 to "freezing_drizzle", 66 to "freezing_rain", 67 to "freezing_rain",
        1061 to "rain_possible", 1071 to "snow_possible"
    )

    private val pathData = Regex("""android:pathData="([^"]*)"""")
    private val paint = Regex("""android:(?:strokeColor|fillColor)="(#[0-9a-fA-F]{6})"""")
    private val strokeWidth = Regex("""android:strokeWidth="([\d.]+)"""")
    private val alphaAttr = Regex("""android:(?:strokeAlpha|fillAlpha)="([\d.]+)"""")
    private val pieceScale = Regex("""android:name="piece\d+" [^>]*android:scaleX="([\d.]+)"""")
    private val pieceStart = Regex("""<group android:name="piece\d+" """)

    private fun text(name: String): String {
        val file = File(drawables, "$name.xml")
        assertTrue("manca $name.xml — esegui tools/compose_falls.py", file.isFile)
        return file.readText()
    }

    /** La sorgente come il pezzo la usa: senza i gruppi che la ricetta toglie (ognuno un
     * gruppo con un percorso solo, senza gruppi dentro). */
    private fun source(prefix: String, piece: Piece): String =
        piece.dropped.fold(text(prefix + piece.source)) { body, group ->
            body.replace(Regex("""<group android:name="$group">.*?</group>""", RegexOption.DOT_MATCHES_ALL), "")
        }

    /** Il composto spezzato nei suoi pezzi: ognuno va dal suo gruppo al successivo. */
    private fun pieces(body: String): List<String> {
        val starts = pieceStart.findAll(body).map { it.range.first }.toList()
        return starts.mapIndexed { i, from -> body.substring(from, starts.getOrElse(i + 1) { body.length }) }
    }

    @Test
    fun `ogni disegno ha le quattro facce ferme e i quattro gemelli, e sta nelle tabelle`() {
        recipes.keys.forEach { name ->
            faces.forEach { (still, moving) ->
                text("$still$name")
                text("$moving$name")
            }
        }
        assertEquals(
            setOf("freezing-rain", "freezing-drizzle", "rain-possible", "snow-possible"),
            ComposedFalls.byName.keys
        )
        val keys = ComposedFalls.byName.values.toSet()
        assertEquals("line scuro", keys, ComposedFalls.lineDarkOf.keys)
        assertEquals("flat chiaro", keys, ComposedFalls.flatOf.keys)
        assertEquals("flat scuro", keys, ComposedFalls.flatDarkOf.keys)
        assertEquals("animati", keys, ComposedFalls.movingOf.keys)
    }

    @Test
    fun `i pezzi sono i disegni di Meteocons alla lettera, percorsi e colori, meno quel che la ricetta toglie`() {
        recipes.forEach { (name, parts) ->
            faces.keys.forEach { prefix ->
                val found = pieces(text("$prefix$name"))
                assertEquals("$prefix$name: pezzi", parts.size, found.size)
                found.zip(parts).forEach { (piece, part) ->
                    val src = source(prefix, part)
                    assertEquals(
                        "$prefix$name (${part.source}): percorsi — ri-esegui tools/compose_falls.py",
                        pathData.findAll(src).map { it.groupValues[1] }.toList(),
                        pathData.findAll(piece).map { it.groupValues[1] }.toList()
                    )
                    assertEquals(
                        "$prefix$name (${part.source}): colori",
                        paint.findAll(src).map { it.groupValues[1] }.toList(),
                        paint.findAll(piece).map { it.groupValues[1] }.toList()
                    )
                }
            }
        }
    }

    /** Il pezzo di base resta dov'e' e grande com'e': la pioggia gelata E' la pioggia di
     * Meteocons, non una pioggia somigliante. */
    @Test
    fun `il pezzo di base resta alla sua taglia e al suo posto`() {
        val base = Regex("""<group android:name="piece1" [^>]*android:scaleX="1" android:scaleY="1" android:translateX="0" android:translateY="0">""")
        recipes.keys.forEach { name ->
            faces.keys.forEach { prefix ->
                assertTrue("$prefix$name: il pezzo di base e' spostato o scalato", base.containsMatchIn(text("$prefix$name")))
            }
        }
    }

    @Test
    fun `un pezzo rimpicciolito tiene la linea della famiglia`() {
        recipes.forEach { (name, parts) ->
            faces.keys.forEach { prefix ->
                pieces(text("$prefix$name")).zip(parts).forEach { (piece, part) ->
                    val k = pieceScale.find(piece)!!.groupValues[1].toDouble()
                    val drawn = strokeWidth.findAll(piece).map { it.groupValues[1].toDouble() * k }.toList()
                    val original = strokeWidth.findAll(source(prefix, part)).map { it.groupValues[1].toDouble() }.toList()
                    assertEquals("$prefix$name (${part.source}): quanti contorni", original.size, drawn.size)
                    drawn.zip(original).forEach { (d, o) -> assertEquals("$prefix$name (${part.source})", o, d, 0.001) }
                }
            }
        }
    }

    private fun channel(c: Float) =
        if (c <= 0.03928f) c / 12.92 else ((c + 0.055) / 1.055).toDouble().pow(2.4)

    private fun luminance(color: Color) =
        0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)

    private fun contrast(a: Color, b: Color): Double {
        val (hi, lo) = listOf(luminance(a), luminance(b)).sortedDescending()
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun over(ink: Color, ground: Color, alpha: Float) = Color(
        ink.red * alpha + ground.red * (1 - alpha),
        ink.green * alpha + ground.green * (1 - alpha),
        ink.blue * alpha + ground.blue * (1 - alpha)
    )

    /** Il contrasto peggiore di [ink] al [alpha] dato sui fondi della faccia [prefix]. */
    private fun worst(prefix: String, ink: Color, alpha: Float): Double {
        val grounds = if (prefix == "mc3n_" || prefix == "mc3fn_") {
            listOf(ChiaroDarkScheme.surface, VividDarkScheme.surface)
        } else {
            listOf(ChiaroLightScheme.surface, VividLightScheme.surface)
        }
        return grounds.minOf { contrast(over(ink, it, alpha), it) }
    }

    /**
     * **I disegni «possibile» sono piu' chiari, fin dove il 3:1 regge.** Quel che cade porta
     * un alfa sotto 1; il colore che ne risulta sul fondo peggiore della faccia (gli schemi
     * veri, le due vesti) tiene il 3:1 di DESIGN §10; e un passo piu' chiaro (0,05) non lo
     * terrebbe, a meno che l'alfa sia gia' il 60% voluto dallo strumento. La nuvola non e'
     * schiarita.
     */
    @Test
    fun `quel che cade nei disegni possibile e' piu' chiaro, e tiene il 3 a 1 sul suo fondo`() {
        recipes.filterValues { parts -> parts.any { it.light } }.keys.forEach { name ->
            faces.keys.forEach { prefix ->
                val body = text("$prefix$name")
                val falling = Regex("""<path [^>]*android:name="[^"]*"[^>]*/>""").findAll(body).map { it.value }.toList()
                assertEquals("$prefix$name: quanti percorsi cadono", 2, falling.size)
                val alphas = falling.map { alphaAttr.find(it)?.groupValues?.get(1)?.toFloat() }
                assertTrue("$prefix$name: una goccia o un fiocco senza chiarezza", alphas.all { it != null })
                val alpha = alphas.first()!!
                assertTrue("$prefix$name: le due chiarezze differiscono", alphas.all { it == alpha })
                assertTrue("$prefix$name: non e' piu' chiaro ($alpha)", alpha < 1f)
                val ink = Color(0xFF000000 or paint.find(falling.first())!!.groupValues[1].drop(1).toLong(16))
                val held = worst(prefix, ink, alpha)
                assertTrue("$prefix$name: a $alpha fa %.2f:1 sul fondo peggiore".format(held), held >= 3.0)
                val lighter = worst(prefix, ink, alpha - 0.05f)
                assertTrue(
                    "$prefix$name: a ${alpha - 0.05f} farebbe ancora %.2f:1, poteva essere piu' chiaro".format(lighter),
                    alpha <= 0.6f + 1e-6f || lighter < 3.0
                )
                val cloud = body.substringBefore("<group android:name=\"g2")
                assertTrue("$prefix$name: la nuvola e' schiarita", !alphaAttr.containsMatchIn(cloud))
            }
        }
    }

    /**
     * **Il gemello animato porta la stessa chiarezza.** I fotogrammi dell'alfa che la
     * sorgente fa correre fra 0 e 1 corrono qui fra 0 e la chiarezza del disegno fermo: una
     * goccia che tornasse piena appena si muove direbbe pioggia e non possibilita'.
     */
    @Test
    fun `i fotogrammi dell'alfa del gemello animato corrono fino alla chiarezza del disegno fermo`() {
        recipes.filterValues { parts -> parts.any { it.light } }.keys.forEach { name ->
            faces.forEach { (still, moving) ->
                val alpha = alphaAttr.find(text("$still$name"))!!.groupValues[1].toDouble()
                val twin = text("$moving$name")
                val targets = Regex("""<target android:name="([^"]*)">.*?</target>""", RegexOption.DOT_MATCHES_ALL)
                    .findAll(twin).map { it.value }
                    .filter { Regex("""android:propertyName="(strokeAlpha|fillAlpha)"""").containsMatchIn(it) }
                    .toList()
                assertEquals("$moving$name: quante animazioni dell'alfa", 2, targets.size)
                targets.forEach { block ->
                    val values = Regex("""android:value="([\d.]+)"""").findAll(block).map { it.groupValues[1].toDouble() }.toList()
                    assertTrue("$moving$name: nessun fotogramma", values.isNotEmpty())
                    assertEquals("$moving$name: il fotogramma pieno non e' la chiarezza", alpha, values.max(), 0.001)
                }
            }
        }
    }

    /** Le animazioni sono della sorgente: le gocce cadono e la nuvola dondola come in
     * `rain`, il cristallo dondola come `snowflake` fa da solo (6 gradi in 3 secondi, il
     * passo della nuvola), e nient'altro. */
    @Test
    fun `nella pioggia gelata cadono le gocce e il cristallo dondola come la sua sorgente`() {
        listOf("freezing_rain", "freezing_drizzle").forEach { name ->
            faces.forEach { (still, moving) ->
                val twin = text("$moving$name")
                val targets = Regex("""<target android:name="([^"]*)">""").findAll(twin).map { it.groupValues[1] }.toList()
                assertEquals("$moving$name: le gocce e la nuvola", 7, targets.count { it.endsWith("_1") })
                assertEquals("$moving$name: il cristallo", listOf("g1snowflake_2"), targets.filter { it.endsWith("_2") })
                val crystal = twin.substringAfter("<target android:name=\"g1snowflake_2\">").substringBefore("</target>")
                val source = text("${moving}snowflake").substringAfter("<target android:name=\"g1snowflake\">").substringBefore("</target>")
                assertEquals("$moving$name: il dondolio del cristallo non e' quello di ${still}snowflake", source, crystal)
            }
        }
    }

    /**
     * I sei codici arrivano al disegno composto per tutte le strade: `conditionLineRes`,
     * `styledRes` per ogni faccia, `movingRes` per ogni gemello. Una tabella dimenticata non
     * rompe la compilazione: lascia un'icona vuota a chi cambia stile.
     */
    @Test
    fun `la pioggia gelata e la possibilita' si trovano in tutte le facce, ferme e animate`() {
        codes.forEach { (code, name) ->
            listOf(false, true).forEach { night ->
                val line = ChiaroIcons.conditionLineRes(code, night)
                assertEquals("il codice $code non prende $name", ComposedFalls.byName.getValue(name.replace("_", "-")), line)
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

    /** Nessun nome sta in due tabelle: `styledRes` prenderebbe la prima e l'altra avrebbe
     * una faccia che nessuno guarda piu'. */
    @Test
    fun `nessun disegno sta in due tabelle`() {
        val tables = listOf(ComposedFalls.byName, ComposedIcons.byName, SkyMarks.byName, MeteoconsSets.byName)
        tables.forEachIndexed { i, a ->
            tables.drop(i + 1).forEach { b ->
                val both = a.values.toSet() intersect b.values.toSet()
                assertTrue("questi disegni stanno in due tabelle: $both", both.isEmpty())
            }
        }
    }
}
