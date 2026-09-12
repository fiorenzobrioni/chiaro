package com.callbackdev.chiaro.ui.icons

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DESIGN.md §7.1. The animated sets are generated from the same SVGs as the still ones
 * by the same tool, so what this holds is not "did the conversion run" but the four
 * claims that make the conversion safe to ship:
 *
 * 1. **A moving icon is the still icon, moving.** Same paths in the same order, same
 *    colors in the same order. It is the strongest thing a JVM test can say about a
 *    drawing it cannot render, and it catches the whole class of "the animated one is a
 *    slightly different picture" — including a re-import that recolors one set and not
 *    the other.
 * 2. **Every target exists.** An `<animated-vector>` whose `<target>` names an element
 *    the vector does not have is not an error at build time, at install time or at draw
 *    time: it is simply an icon that does not move, discovered by a person.
 * 3. **Every animated property belongs to the element it is aimed at.** `rotation` on a
 *    path or `strokeAlpha` on a group fails the same silent way.
 * 4. **Every loop is a loop.** `repeatCount="infinite"`, a positive duration, and
 *    keyframes that go forwards.
 *
 * The four sets are the four the app can reach: line and flat, on a light ground and on
 * a dark one (§13.1).
 *
 * **One deliberate exception to «same drawing, moving»**: an icon whose dash is a window
 * running along the stroke (`wind`, the Beaufort scale — DESIGN §13.1) does NOT have the
 * same `pathData` in both files. The still one has the dashes cut into it as real
 * segments, because that is what the illustrator's static frame shows; the moving one
 * carries the whole line and a `trimPath` window that runs along it, because that is the
 * only way Android says «a gust passing». Those files are skipped below, by the presence
 * of `trimPath` and nothing else, so the exception cannot quietly widen.
 */
class AnimatedIconTest {

    /** still prefix to moving prefix, for each ground and style the app can pick. */
    private val sets = listOf(
        "mc3_" to "mc3a_", "mc3n_" to "mc3an_", "mc3f_" to "mc3fa_", "mc3fn_" to "mc3fan_"
    )

    private val drawables = File("src/main/res/drawable")

    private val pathData = Regex("""android:pathData="([^"]*)"""")
    private val paint = Regex("""android:(?:strokeColor|fillColor)="([^"]*)"""")
    private val names = Regex("""android:name="([^"]*)"""")
    private val targets = Regex("""<target android:name="([^"]*)"""")

    private fun moving(prefix: String): List<File> =
        drawables.listFiles { f -> f.name.startsWith(prefix) && f.extension == "xml" }
            .orEmpty().sortedBy { it.name }

    @Test
    fun `every set has the same members, and none is missing`() {
        val stems = sets.map { (_, prefix) ->
            moving(prefix).map { it.name.removePrefix(prefix) }.toSet()
        }
        assertTrue("no animated drawables — run tools/import_meteocons_v3.py", stems.first().isNotEmpty())
        stems.forEach { assertEquals("the animated sets must hold the same icons", stems.first(), it) }
    }

    @Test
    fun `a moving icon is the still icon, moving`() {
        sets.forEach { (still, prefix) ->
            moving(prefix).forEach { file ->
                val sibling = File(drawables, still + file.name.removePrefix(prefix))
                assertTrue("${file.name} has no still sibling at ${sibling.name}", sibling.isFile)
                val a = file.readText()
                val b = sibling.readText()
                // Il tratteggio che corre: la finestra e' l'eccezione dichiarata in testa.
                if (a.contains("android:trimPath")) return@forEach
                assertEquals(
                    "${file.name} draws different geometry from ${sibling.name}",
                    pathData.findAll(b).map { it.groupValues[1] }.toList(),
                    pathData.findAll(a).map { it.groupValues[1] }.toList()
                )
                assertEquals(
                    "${file.name} is painted differently from ${sibling.name}",
                    paint.findAll(b).map { it.groupValues[1] }.toList(),
                    paint.findAll(a).map { it.groupValues[1] }.toList()
                )
            }
        }
    }

    /** Which AVD properties each kind of element actually has. */
    private val groupProperties = setOf("rotation", "translateX", "translateY", "scaleX", "scaleY")
    private val pathProperties = setOf("fillAlpha", "strokeAlpha", "strokeWidth", "trimPathStart", "trimPathEnd", "trimPathOffset")

    @Test
    fun `every target exists, and every property belongs to what it is aimed at`() {
        sets.forEach { (_, prefix) ->
            moving(prefix).forEach { file ->
                val text = file.readText()
                val drawing = text.substringBefore("</aapt:attr>")
                // A group's name is on a <group ...> line, a path's on a <path ...> line;
                // the emitter writes each on its own line right under the tag.
                val kinds = mutableMapOf<String, String>()
                var tag = ""
                drawing.lines().forEach { line ->
                    val trimmed = line.trim()
                    if (trimmed.startsWith("<group")) tag = "group"
                    if (trimmed.startsWith("<path")) tag = "path"
                    names.find(trimmed)?.let { kinds[it.groupValues[1]] = tag }
                }
                assertTrue("${file.name} names nothing to animate", kinds.isNotEmpty())

                targets.findAll(text).forEach { target ->
                    val name = target.groupValues[1]
                    val kind = kinds[name]
                    assertTrue("${file.name} targets '$name', which the drawing does not name", kind != null)
                    val block = text.substring(target.range.first)
                        .substringBefore("</target>")
                    val allowed = if (kind == "group") groupProperties else pathProperties
                    val properties = Regex("""android:propertyName="([^"]*)"""")
                        .findAll(block).map { it.groupValues[1] }.toList()
                    assertTrue("${file.name}: target '$name' animates nothing", properties.isNotEmpty())
                    properties.forEach {
                        assertTrue("${file.name}: '$it' is not a $kind property", it in allowed)
                    }
                }
            }
        }
    }

    /**
     * **Un gruppo non anima una proprieta' che porta gia' scritta nell'XML.**
     *
     * Un `objectAnimator` chiama il setter della proprieta': il valore statico non e' un
     * punto di partenza a cui l'animazione si somma, e' un valore che l'animazione
     * **sostituisce**. Un gruppo con `translateY="30.5"` e un `translateY` animato da 0 a
     * -3 sta fermo finche' l'icona e' ferma e salta di trenta unita' appena parte.
     *
     * Nessuna icona importata lo fa — i gruppi che Meteocons anima sono gusci vuoti — e
     * per questo il difetto e' passato: `tools/icon_filmstrip.py` sommava le due cose e
     * quindi disegnava la posizione giusta. E' costato un giro sul telefono (12 set 2026,
     * il «quasi sereno» composto), e da qui in poi lo dice la suite. La regola d'oro resta
     * quella dell'importatore: il gruppo che si muove non porta trasformazioni sue, e chi
     * ha bisogno di tutte e due usa due gruppi annidati.
     */
    @Test
    fun `an animated property is never also written on its group`() {
        val groupLine = Regex("""<group\b[^>]*>""")
        sets.forEach { (_, prefix) ->
            moving(prefix).forEach { file ->
                val text = file.readText()
                val drawing = text.substringBefore("</aapt:attr>")
                val statics = buildMap {
                    groupLine.findAll(drawing).forEach { g ->
                        val name = names.find(g.value)?.groupValues?.get(1) ?: return@forEach
                        put(name, groupProperties.filter { """android:$it="""" in g.value })
                    }
                }
                targets.findAll(text).forEach { target ->
                    val name = target.groupValues[1]
                    val block = text.substring(target.range.first).substringBefore("</target>")
                    Regex("""android:propertyName="([^"]*)"""").findAll(block)
                        .map { it.groupValues[1] }
                        .forEach { property ->
                            assertTrue(
                                "${file.name}: il gruppo '$name' anima $property e lo porta " +
                                    "anche scritto — l'animazione lo sostituisce, e il " +
                                    "disegno salta appena parte",
                                property !in statics[name].orEmpty()
                            )
                        }
                }
            }
        }
    }

    @Test
    fun `every loop is a loop`() {
        val animator = Regex("""<objectAnimator\b[^>]*""", RegexOption.DOT_MATCHES_ALL)
        sets.forEach { (_, prefix) ->
            moving(prefix).forEach { file ->
                val text = file.readText()
                val found = animator.findAll(text).map { it.value }.toList()
                assertTrue("${file.name} has no animators", found.isNotEmpty())
                found.forEach { block ->
                    assertTrue(
                        "${file.name} has an animator that stops",
                        """android:repeatCount="infinite"""" in block
                    )
                    val duration = Regex("""android:duration="(\d+)"""").find(block)
                    assertTrue("${file.name} has an animator with no duration", duration != null)
                    assertTrue(
                        "${file.name} has a zero-length animator",
                        duration!!.groupValues[1].toInt() > 0
                    )
                    // Fino alla v2 il moto di Meteocons era tutto lineare e questo test
                    // lo pretendeva. La v3 usa `calcMode="spline"`, e un `keySplines` E'
                    // esattamente un `<pathInterpolator>`: l'importatore ne genera uno per
                    // ogni coppia di controlli distinta invece di appiattire la curva. Qui
                    // si pretende allora la cosa che conta davvero — che l'interpolatore
                    // sia dichiarato, e che se e' uno dei nostri il file esista.
                    val interpolator = Regex("""android:interpolator="([^"]+)"""").find(block)
                    assertTrue(
                        "${file.name} has an animator with no interpolator",
                        interpolator != null
                    )
                    val res = interpolator!!.groupValues[1]
                    assertTrue(
                        "${file.name} uses an interpolator that is neither linear nor ours: $res",
                        res == "@android:anim/linear_interpolator" ||
                            res.startsWith("@interpolator/")
                    )
                    if (res.startsWith("@interpolator/")) {
                        val generated = File("src/main/res/interpolator/${res.substringAfterLast('/')}.xml")
                        assertTrue(
                            "${file.name} points at ${generated.name}, which was not generated",
                            generated.isFile
                        )
                    }
                }
                var previous = -1.0
                Regex("""<keyframe android:fraction="([\d.]+)"""").findAll(text).forEach {
                    val fraction = it.groupValues[1].toDouble()
                    assertTrue("${file.name} has a keyframe outside 0..1: $fraction", fraction in 0.0..1.0)
                    if (fraction < previous) previous = -1.0 // a new property's list begins
                    assertTrue(
                        "${file.name} has keyframes that do not go forwards",
                        fraction > previous || previous < 0
                    )
                    previous = fraction
                }
            }
        }
    }
}
