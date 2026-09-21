package com.callbackdev.chiaro.widget

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The trap that cost two device passes (20 set 2026), pinned so it cannot cost a third.
 *
 * Glance's `padding` is `RemoteViews.setViewPadding` on the SAME view every other modifier
 * in the chain lands on, and an `Image` draws its provider scaled to Fit whatever the
 * padding leaves. So `GlanceModifier.padding(start = 8.dp, end = 2.dp).size(16.dp)` is not
 * a 16 dp mark with air around it: it is a **6 dp** mark. `RangeMark` asked for that and
 * got it, which is the whole of «la freccia della minima è più piccola» — the low mark
 * carried the 8 dp that separates the two halves of the pair and was drawn at 43% of the
 * high one, at the same nominal size, in two passes where the reported symptom looked like
 * colour and was geometry.
 *
 * The house rule the repository already followed everywhere else: air around a drawing is
 * a `Spacer` or a wrapper `Box`, never the drawing's own padding — `PlaceLine`'s pin and
 * `WarningChipRow`'s chip both say so in their own words. This reads the sources and holds
 * it. `width` and `height` are deliberately not included: a padded container of a FIXED
 * width is a different thing and a correct one (`SkyWidget`'s verdict column, the hero
 * rows' words column), because a layout's padding comes out of a layout's children, not
 * out of a bitmap.
 *
 * **Verified by breaking it**: putting the padding back on `RangeMark`'s `Image` fails
 * this test. A guard of this kind that has not been seen to fail guards nothing.
 */
class WidgetGlyphBoxTest {

    private val sources = File("src/main/kotlin/com/callbackdev/chiaro/widget")

    /** A `GlanceModifier` chain: the builder and the calls hanging off it. */
    private val chain = Regex("""GlanceModifier((?:\s*\.\s*\w+\([^()]*(?:\([^()]*\))?[^()]*\))+)""")

    @Test
    fun `no drawing pays for its own air out of its own box`() {
        val offenders = sources.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file ->
                val text = file.readText()
                chain.findAll(text).mapNotNull { match ->
                    val calls = match.groupValues[1]
                    if (!calls.contains(".size(") || !calls.contains(".padding(")) return@mapNotNull null
                    "${file.name}:${text.take(match.range.first).count { it == '\n' } + 1}"
                }
            }
            .toList()
        assertEquals(
            "a sized Glance view with padding draws SMALLER, it does not sit further in: " +
                "put the air on a wrapper Box or a Spacer",
            emptyList<String>(),
            offenders
        )
    }
}
