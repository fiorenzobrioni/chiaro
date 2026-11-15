package com.callbackdev.chiaro.widget

import androidx.compose.ui.unit.dp
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
     * The colour strip (15 nov 2026, the seventh colour): one row where the seven 48dp targets
     * fit, and where they do not, rows balanced so no colour sits alone under the rest. The
     * widths are the group's on real phones once the strip starts under the radio buttons:
     * 412dp (a Pixel), 384dp (most Galaxy S), 360dp (the narrowest in use).
     */
    @Test
    fun `the colour strip is one row from a 384dp phone, four over three under it`() {
        val seven = WidgetCardColorChoices.size
        assertEquals(7, seven)
        assertEquals(7, swatchesPerRow(seven, 412.dp - 32.dp - 12.dp))
        assertEquals(7, swatchesPerRow(seven, 384.dp - 32.dp - 12.dp))
        assertEquals(4, swatchesPerRow(seven, 360.dp - 32.dp - 12.dp))
        assertEquals(4, swatchesPerRow(seven, 200.dp))
        assertEquals(3, swatchesPerRow(seven, 150.dp))
        assertEquals(1, swatchesPerRow(seven, 10.dp))
    }

    /**
     * The settings screen asks the background question through the shared section, and
     * prints no row of its own: a second copy of the list is exactly how «Un colore» would
     * come to exist on one widget out of the family. (Two screens until 20 ott 2026, when
     * the arc's own went with the arc.)
     */
    @Test
    fun `the configuration screen asks through the one shared section`() {
        val main = File("src/main/kotlin/com/callbackdev/chiaro/widget/WidgetConfigActivity.kt")
        assertTrue("${main.name} is not where it was", main.isFile)
        assertTrue("${main.name} does not call BackgroundSection", main.readText().contains("BackgroundSection("))
        val call = Regex("""(?m)^\s*BackgroundSection\(""")
        assertEquals("BackgroundSection is called once", 1, call.findAll(main.readText()).count())
    }
}
