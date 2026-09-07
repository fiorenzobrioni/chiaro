package com.callbackdev.chiaro.ui

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Everything on screen in this product is prose or data, so everything localizes
 * (CLAUDE.md, VISION §5.10): a string that exists in one language and not the other
 * is not a style choice, it is a screen that falls back to English for one reader.
 *
 * Written with the guide's rewrite (4th device pass), which added some seventy
 * strings in one sitting — exactly the kind of edit where one gets forgotten, and
 * exactly the kind of miss no reviewer catches by reading two files side by side.
 * `translatable="false"` is the one honest exception: a brand name is not prose.
 */
class StringsParityTest {

    private val english = File("src/main/res/values/strings.xml")
    private val italian = File("src/main/res/values-it/strings.xml")

    @Test
    fun `every translatable string and plural exists in both languages`() {
        assertTrue("no strings at ${english.absolutePath}", english.isFile)
        assertTrue("no strings at ${italian.absolutePath}", italian.isFile)

        val en = names(english).filterNot { it in untranslatable(english) }.toSet()
        val it = names(italian)

        assertEquals("missing from values-it/", emptySet<String>(), en - it)
        assertEquals("missing from values/", emptySet<String>(), it - en)
    }

    @Test
    fun `no name is declared twice in either language`() {
        listOf(english, italian).forEach { file ->
            val all = allNames(file)
            val twice = all.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
            assertEquals("declared twice in ${file.name}", emptySet<String>(), twice)
        }
    }

    /** The format arguments a string carries have to be the same in both languages:
     * a missing `%1$s` is a crash at the moment the sentence is needed. */
    @Test
    fun `a translated string keeps the arguments of its original`() {
        val en = bodies(english)
        val it = bodies(italian)
        val mismatched = en.keys.intersect(it.keys).filter { name ->
            arguments(en.getValue(name)) != arguments(it.getValue(name))
        }
        assertEquals("different format arguments", emptyList<String>(), mismatched)
    }

    /**
     * A plural is a translation decision, not a copy: Italian and English happen to
     * agree on `one`/`other`, and a language that needs `many` would need it in one file
     * only. What must not happen is a quantity present in one and missing in the other —
     * `getQuantityString` falls back silently and the reader gets the wrong sentence,
     * not a crash. Added on the Fase 9 IT/EN pass.
     */
    @Test
    fun `a plural offers the same quantities in both languages`() {
        val en = quantities(english)
        val it = quantities(italian)
        assertEquals("plurals declared", en.keys, it.keys)
        en.keys.forEach { name ->
            assertEquals("the quantities of $name", en.getValue(name), it.getValue(name))
        }
    }

    /**
     * A stray `%` inside a string that also carries a format argument is a crash at the
     * moment the sentence is needed: `String.format` reads it as a conversion it does
     * not know. It has to be written `%%`, and nothing in the toolchain says so.
     */
    @Test
    fun `a string that formats does not carry a stray percent`() {
        listOf(english, italian).forEach { file ->
            val stray = bodies(file).filter { (_, body) ->
                argument.containsMatchIn(body) &&
                    argument.replace(body, "").replace("%%", "").contains('%')
            }.keys
            assertEquals("an unescaped %% in ${file.name}", emptySet<String>(), stray)
        }
    }

    /** A string that exists and says nothing is a screen with a hole in it (§1.1). */
    @Test
    fun `no string is blank in either language`() {
        listOf(english, italian).forEach { file ->
            val blank = bodies(file).filterValues { it.isBlank() }.keys
            assertEquals("blank in ${file.name}", emptySet<String>(), blank)
        }
    }

    private val plural = Regex("""<plurals[^>]*name="([^"]+)"[^>]*>(.*?)</plurals>""", RegexOption.DOT_MATCHES_ALL)
    private val quantity = Regex("""quantity="(\w+)"""")

    private fun quantities(file: File): Map<String, Set<String>> =
        plural.findAll(file.readText()).associate { match ->
            match.groupValues[1] to quantity.findAll(match.groupValues[2])
                .map { it.groupValues[1] }
                .toSet()
        }

    private val entry = Regex("""<(string|item)[^>]*name="([^"]+)"""")
    private val bodyEntry = Regex("""<string[^>]*name="([^"]+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
    private val argument = Regex("""%(\d+\$)?[sdf]""")

    private fun allNames(file: File): List<String> =
        entry.findAll(file.readText()).map { it.groupValues[2] }.toList()

    private fun names(file: File): Set<String> = allNames(file).toSet()

    private fun untranslatable(file: File): Set<String> =
        Regex("""<string[^>]*name="([^"]+)"[^>]*translatable="false"""")
            .findAll(file.readText())
            .map { it.groupValues[1] }
            .toSet()

    private fun bodies(file: File): Map<String, String> =
        bodyEntry.findAll(file.readText()).associate { it.groupValues[1] to it.groupValues[2] }

    private fun arguments(body: String): Set<String> =
        argument.findAll(body).map { it.value }.toSet()
}
