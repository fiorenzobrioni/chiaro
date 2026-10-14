package com.callbackdev.chiaro.ui.icons

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * I segni del cielo composti da `tools/compose_sky_marks.py` (revisione del Cielo, punto 7,
 * 14 ott 2026): la meteora, la luna con la stella, le due stelle. Come per
 * [ComposedIconsTest], la promessa e' che i pezzi sono i disegni di Meteocons **alla
 * lettera** — percorsi e colori, faccia per faccia — e che la linea resta quella della
 * famiglia. Se l'importatore riscrive le sorgenti e lo strumento non e' stato rifatto
 * girare, questi test se ne accorgono.
 */
class SkyMarksTest {

    private val drawables = File("src/main/res/drawable")
    private val faces = listOf("mc3_", "mc3n_", "mc3f_", "mc3fn_")

    /** Ogni segno, con le sorgenti dei suoi pezzi nell'ordine in cui li disegna. */
    private val recipes = mapOf(
        "meteor" to listOf("falling_stars"),
        "moon_star" to listOf("clear_night", "star"),
        "two_stars" to listOf("star", "star")
    )

    private val pathData = Regex("""android:pathData="([^"]*)"""")
    private val paint = Regex("""android:(?:strokeColor|fillColor)="(#[0-9a-fA-F]{6})"""")
    private val strokeWidth = Regex("""android:strokeWidth="([\d.]+)"""")
    private val pieceScale = Regex("""android:name="piece\d+" [^>]*android:scaleX="([\d.]+)"""")
    private val pieceStart = Regex("""<group android:name="piece\d+" """)
    private val moonGroup = Regex("""<group android:name="g4moon".*?</group>""", RegexOption.DOT_MATCHES_ALL)

    private fun text(name: String): String {
        val file = File(drawables, "$name.xml")
        assertTrue("manca $name.xml — esegui tools/compose_sky_marks.py", file.isFile)
        return file.readText()
    }

    /** La sorgente come la usa il segno: la stella cadente senza la sua luna. */
    private fun source(prefix: String, name: String, forMark: String): String {
        val body = text("$prefix$name")
        return if (forMark == "meteor") body.replace(moonGroup, "") else body
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
        assertEquals(setOf("meteor", "moon-star", "two-stars"), SkyMarks.byName.keys)
        listOf(SkyMarks.lineDarkOf, SkyMarks.flatOf, SkyMarks.flatDarkOf).forEach { table ->
            assertEquals(SkyMarks.byName.values.toSet(), table.keys)
        }
    }

    @Test
    fun `i pezzi sono i disegni di Meteocons alla lettera, percorsi e colori`() {
        recipes.forEach { (mark, sources) ->
            faces.forEach { prefix ->
                val body = text("$prefix$mark")
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

    @Test
    fun `la meteora non porta la luna`() {
        faces.forEach { prefix ->
            val moon = moonGroup.find(text("${prefix}falling_stars"))!!.value
            val moonPath = pathData.find(moon)!!.groupValues[1]
            assertTrue("$prefix: la luna e' rimasta", moonPath !in text("${prefix}meteor"))
        }
    }

    @Test
    fun `un pezzo rimpicciolito tiene la linea della famiglia`() {
        recipes.forEach { (mark, sources) ->
            faces.forEach { prefix ->
                val body = text("$prefix$mark")
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
