package com.callbackdev.chiaro.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import com.callbackdev.chiaro.data.AppFont
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The typeface is a setting since 20 set 2026, so "the app is set in one family" stopped
 * being something you can read off one file and became something to check.
 *
 * Two ways it breaks, and neither shows up as a crash:
 *
 * 1. **A role nobody copied.** `Typography()` leaves `fontFamily` null, which resolves to
 *    the platform sans — so a role left out of [typographyFor] silently sets one line of
 *    the app in a different font from the rest. The test asks every role Material has,
 *    through reflection rather than a list, so a role added by a future Material release
 *    fails here instead of shipping.
 * 2. **A style that remembers Inter.** [ChiaroType]'s two roles used to be top-level
 *    values built at class-init time; a reader on [AppFont.SYSTEM] would have gone on
 *    reading their temperature in Inter and nothing would have said so.
 */
class TypographyFamilyTest {

    @Test
    fun `every Material role is set in the family the reader chose`() {
        AppFont.entries.forEach { font ->
            val family = familyFor(font)
            roles(chiaroTypography(font)).forEach { (role, style) ->
                assertEquals("$role under $font", family, style.fontFamily)
            }
        }
    }

    @Test
    fun `the roles Material does not have follow the same choice`() {
        AppFont.entries.forEach { font ->
            val family = familyFor(font)
            val type = chiaroType(font)
            assertEquals("hero under $font", family, type.heroTemperature.fontFamily)
            assertEquals("reading under $font", family, type.readingValue.fontFamily)
        }
    }

    /** DESIGN §5: a figure that sits in a column is tabular, in either family. Whether the
     * font HAS tabular figures is the font's business — `tnum` on a face without them is
     * ignored, silently — but asking for them is ours, and it must not depend on the
     * setting. */
    @Test
    fun `the two figure roles ask for tabular digits in either family`() {
        AppFont.entries.forEach { font ->
            val type = chiaroType(font)
            assertEquals("tnum", type.heroTemperature.fontFeatureSettings)
            assertEquals("tnum", type.readingValue.fontFeatureSettings)
        }
    }

    /** The sizes are the design system's, not the family's: switching the typeface must
     * move nothing but the drawing of the letters. */
    @Test
    fun `the scale itself does not move with the family`() {
        val inter = chiaroTypography(AppFont.INTER)
        val system = chiaroTypography(AppFont.SYSTEM)
        roles(inter).forEach { (role, style) ->
            val other = roles(system).getValue(role)
            assertEquals("$role size", style.fontSize, other.fontSize)
            assertEquals("$role line", style.lineHeight, other.lineHeight)
            assertEquals("$role weight", style.fontWeight, other.fontWeight)
        }
        assertEquals(
            chiaroType(AppFont.INTER).heroTemperature.fontSize,
            chiaroType(AppFont.SYSTEM).heroTemperature.fontSize
        )
    }

    /** And the setting has to do something: the two answers are two different families,
     * and the default is the bundled one. */
    @Test
    fun `the two answers differ and Inter is the default`() {
        assertNotEquals(InterFamily, SystemFamily)
        assertEquals(FontFamily.Default, SystemFamily)
        assertEquals(InterFamily, familyFor(AppFont.INTER))
    }

    /** Reflection, so the test asks Material what its roles are instead of trusting a list
     * written the same day as the code it checks. */
    private fun roles(typography: Typography): Map<String, TextStyle> {
        val found = Typography::class.java.declaredMethods
            .filter {
                it.parameterCount == 0 &&
                    it.returnType == TextStyle::class.java &&
                    it.name.startsWith("get")
            }
            .associate { it.name.removePrefix("get") to it.invoke(typography) as TextStyle }
        assertTrue("no roles found on Typography", found.size >= 15)
        return found
    }
}
