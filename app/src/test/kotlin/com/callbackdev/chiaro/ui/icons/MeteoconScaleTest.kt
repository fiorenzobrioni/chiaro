package com.callbackdev.chiaro.ui.icons

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La normalizzazione della taglia (DESIGN.md §13.1, 11 set 2026).
 *
 * Meteocons disegna ogni icona alla taglia che le serve: misurato sulla lista di
 * spedizione, il lato dell'inchiostro andava dal **33%** della scatola
 * (`smoke-particles`) all'**81%** (`uv-index-11-plus`), e incolonnate nel Cielo quelle
 * differenze si leggono come un difetto dell'app, non come una scelta dell'illustratore.
 * `tools/import_meteocons_v3.py` avvolge quindi ogni drawable in un gruppo `mc3scale`
 * che porta la **media geometrica** dei due lati a 0,69, sotto un tetto sull'ingombro.
 *
 * Quel che un test JVM puo' dire di un disegno che non sa disegnare non e' «e' della
 * taglia giusta» — quella la misura `tools/icon_ink.py` sulla sorgente — ma le tre cose
 * che rendono la scala sicura da spedire, e che si rompono per distrazione:
 *
 * 1. **Le quattro facce di un disegno portano la stessa scala.** Rigenerare uno stile e
 *    non l'altro compila, e poi l'icona cambia taglia quando il lettore cambia stile in
 *    Impostazioni.
 * 2. **Il gemello animato porta la scala del fermo.** Questo e' il difetto peggiore della
 *    famiglia: l'icona si disegna ferma, poi parte l'animazione e salta di taglia.
 * 3. **Il perno e' il centro della scatola**, perche' e' l'unico che non sposta il
 *    centraggio ottico su cui la striscia oraria e' stata messa a punto.
 *
 * Un'icona **senza** gruppo `mc3scale` non e' un errore: vuol dire che era gia' a misura
 * e che l'importatore non ha scritto un gruppo che non serviva. Quel che non deve
 * succedere e' che ce l'abbia una faccia sola.
 */
class MeteoconScaleTest {

    private val drawables = File("src/main/res/drawable")

    /** I quattro set fermi, e il gemello animato di ognuno. */
    private val stillSets = listOf("mc3_", "mc3n_", "mc3f_", "mc3fn_")
    private val movingOf = mapOf(
        "mc3_" to "mc3a_", "mc3n_" to "mc3an_", "mc3f_" to "mc3fa_", "mc3fn_" to "mc3fan_"
    )

    private val scaleGroup = Regex(
        """<group android:name="mc3scale"""" +
            """ android:scaleX="([\d.]+)" android:scaleY="([\d.]+)"""" +
            """ android:pivotX="([\d.]+)" android:pivotY="([\d.]+)">"""
    )
    private val viewport = Regex(
        """android:viewportWidth="([\d.]+)"\s+android:viewportHeight="([\d.]+)""""
    )

    /** La scala di un file, o null se non ne porta una (cioe' era gia' a misura). */
    private fun scaleOf(file: File): String? =
        scaleGroup.find(file.readText())?.groupValues?.get(1)

    private fun shippedStems(): List<String> =
        MeteoconsSets.byName.keys.map { it.replace("-", "_") }.sorted()

    @Test
    fun `the four faces of a drawing carry the same scale`() {
        val stems = shippedStems()
        assertTrue("le tabelle sono vuote — l'importatore ha girato?", stems.isNotEmpty())
        val disagree = stems.mapNotNull { stem ->
            val seen = stillSets
                .map { File(drawables, "$it$stem.xml") }
                .filter { it.exists() }
                .map { scaleOf(it) }
            if (seen.toSet().size > 1) "$stem: $seen" else null
        }
        assertTrue(
            "stessa icona, taglie diverse fra gli stili — rigenera TUTTI i set con " +
                "tools/import_meteocons_v3.py:\n" + disagree.joinToString("\n"),
            disagree.isEmpty()
        )
    }

    @Test
    fun `a moving twin carries the scale of its still one`() {
        val stems = shippedStems()
        val jumps = buildList {
            for (stem in stems) {
                for ((still, moving) in movingOf) {
                    val a = File(drawables, "$still$stem.xml")
                    val b = File(drawables, "$moving$stem.xml")
                    if (!a.exists() || !b.exists()) continue
                    val sa = scaleOf(a)
                    val sb = scaleOf(b)
                    if (sa != sb) add("$moving$stem: fermo $sa, animato $sb")
                }
            }
        }
        assertTrue(
            "l'icona salterebbe di taglia quando parte l'animazione:\n" +
                jumps.joinToString("\n"),
            jumps.isEmpty()
        )
    }

    @Test
    fun `the scale turns on the centre of the box`() {
        var checked = 0
        drawables.listFiles { f -> f.name.startsWith("mc3") && f.extension == "xml" }
            .orEmpty()
            .forEach { file ->
                val text = file.readText()
                val g = scaleGroup.find(text) ?: return@forEach
                val box = viewport.find(text)
                assertTrue("${file.name}: nessun viewport da cui leggere il centro", box != null)
                val (w, h) = box!!.destructured
                assertEquals(
                    "${file.name}: il perno non e' il centro della scatola",
                    listOf(w.toFloat() / 2, h.toFloat() / 2),
                    listOf(g.groupValues[3].toFloat(), g.groupValues[4].toFloat())
                )
                checked++
            }
        assertTrue("nessun gruppo mc3scale trovato — la normalizzazione e' sparita", checked > 0)
    }

    /** Il gruppo della scala avvolge **tutto**: se ne restasse fuori un pezzo, quel pezzo
     * resterebbe alla taglia dell'illustratore accanto al resto riportato a misura. */
    @Test
    fun `the scale group wraps the whole drawing`() {
        val leaks = drawables.listFiles { f -> f.name.startsWith("mc3") && f.extension == "xml" }
            .orEmpty()
            .filter { scaleGroup.containsMatchIn(it.readText()) }
            .filter { file ->
                val lines = file.readText().lines()
                val open = lines.indexOfFirst { scaleGroup.containsMatchIn(it) }
                val close = lines.indexOfLast { it.trim() == "</group>" }
                val after = lines.subList(close + 1, lines.size)
                // Fra la chiusura del gruppo e la fine del vettore non puo' esserci
                // disegno: solo la chiusura del vettore e, per gli animati, i target.
                open < 0 || after.any { l ->
                    l.trim().startsWith("<path") || l.trim().startsWith("<clip-path")
                }
            }
        assertTrue(
            "disegno fuori dal gruppo della scala: " + leaks.joinToString { it.name },
            leaks.isEmpty()
        )
    }
}
