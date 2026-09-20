package com.callbackdev.chiaro.ui

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **No Italian article may sit immediately before a number the app substitutes.**
 *
 * Italian elides the singular article before a vowel, and whether a number begins
 * with one is a fact about how it is *said*: «al 70%» but «all'80%», because seventy
 * is *settanta* and eighty is *ottanta*. Inside 0–100 the vowel-initial numbers are
 * exactly 1, 8, 11 and 80–89 — so «pioggia al %1$d%%» was right nine times out of ten
 * and wrong the tenth, silently, in a notification nobody could correct.
 *
 * Three ways out were weighed on 22 set 2026, and this is why it is a *rule* and not
 * a helper:
 *
 * 1. **A `Formats` function that knows which numbers elide.** Rejected: that is a fact
 *    about Italian living in Kotlin, applied to, or silently skipped for, every other
 *    language the app is ever translated into. It is also the shape that rots — the
 *    next translator has no way to see it, let alone change it.
 * 2. **Two variants per sentence, chosen by a locale-owned list of eliding numbers.**
 *    Rejected for a sharper reason: composing a sentence from an article fragment and
 *    a number is exactly the shape the severe-weather alert was rewritten *out of* one
 *    commit earlier, because a clause glued on in one language is not a clause every
 *    language puts in that place. Buying elision with it would be undoing that.
 * 3. **Write the sentence so no article touches the number.** Chosen. It needs no code,
 *    cannot be wrong in any language present or future, and it is what the app's own
 *    `notif_summary_body` has said since the day it shipped: «pioggia 80%». The others
 *    were the odd ones out, not this.
 *
 * A test is the right home for the one piece of Italian grammar that remains, because
 * it ships in no APK, reaches no other locale, and states the rule where somebody
 * adding a string will meet it.
 *
 * **What this test cannot check**: a `%s` argument, whose value is a string the app
 * formatted — a date, a city, a condition. Six of those carried a digit-leading value
 * and were fixed by hand in the same pass: `journal_outcome_dry` and
 * `journal_outcome_rained` (a percentage), and `warning_issued_dated`,
 * `warning_sheet_source`, `warning_card_stale_detail` and `journal_warning_missed_held`
 * (a date — «del 8 set» wanted «dell'8 set», so the article was given the word *giorno*
 * to agree with instead, which never changes). Whether a `%s` leads with a digit is a
 * property of the call site, so it stays a reading and not an assertion.
 */
class ItalianArticleTest {

    private val italian = File("src/main/res/values-it/strings.xml")

    /**
     * The singular articles and prepositional contractions that elide. The plurals are
     * deliberately absent and are safe: «alle 8», «delle 8», «dei 90%» keep their vowel
     * — Italian elides *il/lo/la* and what they contract with, never *i/gli/le*. So are
     * the bare prepositions: «fino a 80%», «di 80%», «da 80%» need no apostrophe, which
     * is precisely what makes them the way out.
     */
    private val elides = setOf(
        "il", "lo", "la", "un", "una",
        "al", "allo", "alla",
        "del", "dello", "della",
        "dal", "dallo", "dalla",
        "nel", "nello", "nella",
        "sul", "sullo", "sulla",
        "col",
        "quel", "quello", "quella"
    )

    /** `%d`, `%1$d` — a number, straight from the app, with nothing between. */
    private val articleThenNumber = Regex(
        """\b(${elides.joinToString("|")})\s+%(\d+\$)?d""",
        RegexOption.IGNORE_CASE
    )

    private val named = Regex("""<string[^>]*name="([^"]+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)

    @Test
    fun `no Italian string puts an article straight before a number`() {
        assertTrue("no strings at ${italian.absolutePath}", italian.isFile)
        val offenders = named.findAll(italian.readText())
            .filter { articleThenNumber.containsMatchIn(it.groupValues[2]) }
            .map { "${it.groupValues[1]}: ${it.groupValues[2]}" }
            .toList()
        assertEquals(
            "an article the number may force to elide (see this class's KDoc)",
            emptyList<String>(), offenders
        )
    }

    /** The same rule inside a plural's items, which carry `%d` without a position. */
    @Test
    fun `no plural item puts an article straight before a number`() {
        val items = Regex("""<item[^>]*>(.*?)</item>""", RegexOption.DOT_MATCHES_ALL)
        val offenders = items.findAll(italian.readText())
            .map { it.groupValues[1] }
            .filter { articleThenNumber.containsMatchIn(it) }
            .toList()
        assertEquals("an article a quantity may force to elide", emptyList<String>(), offenders)
    }

    /** The regex has to catch the thing it was written for, or it guards nothing. */
    @Test
    fun `the rule catches what it is for and lets the way out through`() {
        listOf(
            "pioggia al %1\$d%%",
            "illuminata al %1\$d%%",
            "Parziale: il %1\$d%% della Luna",
            "Tra un %d minuto"
        ).forEach { assertTrue(it, articleThenNumber.containsMatchIn(it)) }

        listOf(
            "pioggia %1\$d%%",
            "Pioggia fino a %1\$d%%, il peggio verso le %2\$s",
            "Domani UV massimo %1\$d · %2\$s",
            "Tra %1\$d minuti, alle %2\$s",
            "Una colonna ogni %1\$d ore",
            "%1\$d%% illuminata",
            // Plural contractions do not elide: these are correct Italian as they stand
            "dalle %1\$s alle %2\$s",
            "il massimo di avvisi che l'app tiene, %1\$d"
        ).forEach { assertTrue(it, !articleThenNumber.containsMatchIn(it)) }
    }
}
