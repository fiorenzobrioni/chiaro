package com.callbackdev.chiaro.widget

import com.callbackdev.chiaro.ui.theme.WidgetCardColor
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The one promise the per-widget settings make, pinned (20 set 2026, committente: «in tutti
 * i widget vorrei nelle impostazioni la voce "Un colore"» — it was already true on all five,
 * and this is what keeps it true).
 *
 * A settings screen is a composable, and no test here inflates one. What CAN be checked is
 * the two things that would make a choice go missing without anybody noticing: a row that
 * exists for some kinds of card and not others, and an enum value that has no row at all.
 * The first is structural — there is one [BackgroundSection] and both configuration
 * activities call it — so it is read off the sources; the second is data now
 * ([WidgetBackgroundChoices], [WidgetCardColorChoices]) and is simply compared.
 */
class WidgetConfigChoicesTest {

    @Test
    fun `every background a card can wear is offered, in order`() {
        assertEquals(
            WidgetBackground.entries.toList(),
            WidgetBackgroundChoices.map { it.first }
        )
        assertTrue(
            "«Un colore» is the row the reader asked for on every widget",
            WidgetBackgroundChoices.any { it.first == WidgetBackground.COLOR }
        )
    }

    @Test
    fun `every card colour is offered, in order`() {
        assertEquals(
            WidgetCardColor.entries.toList(),
            WidgetCardColorChoices.map { it.first }
        )
    }

    /**
     * Both settings screens ask the background question through the shared section, and
     * neither prints a row of its own: a second copy of the list is exactly how «Un colore»
     * would come to exist on four widgets out of five.
     */
    @Test
    fun `both configuration screens ask through the one shared section`() {
        val main = File("src/main/kotlin/com/callbackdev/chiaro/widget/WidgetConfigActivity.kt")
        val arc = File("src/main/kotlin/com/callbackdev/chiaro/widget/arc/ArcConfigActivity.kt")
        listOf(main, arc).forEach { file ->
            assertTrue("${file.name} is not where it was", file.isFile)
            assertTrue(
                "${file.name} does not call BackgroundSection",
                file.readText().contains("BackgroundSection(")
            )
        }
        // The section is called with no condition on which widget this is: the whole point
        // is that the card's dress is the same question on all five.
        val call = Regex("""(?m)^\s*BackgroundSection\(""")
        assertEquals(
            "BackgroundSection is declared once and called once per screen",
            2,
            call.findAll(main.readText() + arc.readText()).count()
        )
    }
}
