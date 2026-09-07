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
 * The four sets are the four the app can reach: line and fill, on a light ground and on
 * a dark one (§13.1, §2.5).
 */
class AnimatedIconTest {

    /** still prefix to moving prefix, for each ground and style the app can pick. */
    private val sets = listOf(
        "mc_" to "mca_", "mcn_" to "mcan_", "mcf_" to "mcaf_", "mcfn_" to "mcafn_"
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
        assertTrue("no animated drawables — run tools/import_meteocons.py", stems.first().isNotEmpty())
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
                    // Meteocons' motion is linear; an eased loop would pulse at the seam.
                    assertTrue(
                        "${file.name} has an animator that is not linear",
                        """android:interpolator="@android:anim/linear_interpolator"""" in block
                    )
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
