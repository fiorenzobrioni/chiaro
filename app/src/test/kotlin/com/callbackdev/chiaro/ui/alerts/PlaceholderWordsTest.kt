package com.callbackdev.chiaro.ui.alerts

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The message field's placeholders as words (15.8, 28 Oct 2026): the painted text, the map
 * between the saved offsets and the painted ones in both directions, on a message with two
 * placeholders and a typo, and the backspace that takes a chip whole.
 */
class PlaceholderWordsTest {

    private val words = mapOf(
        "current.temp_c" to "la temperatura adesso",
        "next_6h.precip_chance_max" to "la pioggia nelle prossime 6 ore"
    )
    private val phrase: (String) -> String? = { words[it] }
    private val pad = PlaceholderWords.Pad

    // Si pedala: {current.temp_c} e pioggia al {next_6h.precip_chance_max} {refuso
    //            ^11             ^27           ^41                        ^68
    private val message = "Si pedala: {current.temp_c} e pioggia al {next_6h.precip_chance_max} {refuso"
    private val painted = "Si pedala: ${pad}la temperatura adesso$pad e pioggia al ${pad}la pioggia nelle prossime 6 ore$pad {refuso"
    private val transformed = PlaceholderTransformation(phrase, SpanStyle()).filter(AnnotatedString(message))

    @Test
    fun `a known placeholder is painted as its words, a typo as typed`() {
        assertEquals(painted, transformed.text.text)
    }

    @Test
    fun `saved offsets map forward onto the painted text`() {
        val map = transformed.offsetMapping
        val chip1 = 11 + pad.length + "la temperatura adesso".length + pad.length // the first chip's end
        assertEquals(0, map.originalToTransformed(0))
        assertEquals(11, map.originalToTransformed(11)) // before «{» is before the chip
        assertEquals(chip1, map.originalToTransformed(12)) // inside the id is after the chip
        assertEquals(chip1, map.originalToTransformed(20))
        assertEquals(chip1, map.originalToTransformed(27)) // after «}» is after the chip
        assertEquals(chip1 + 1, map.originalToTransformed(28))
        assertEquals(painted.length, map.originalToTransformed(message.length))
        // the typo moves with the growth of the two chips before it
        val growth = painted.length - message.length
        assertEquals(68 + growth, map.originalToTransformed(68))
        assertEquals(70 + growth, map.originalToTransformed(70))
    }

    @Test
    fun `painted offsets map back onto the saved text`() {
        val map = transformed.offsetMapping
        val chip1 = 11 + pad.length + "la temperatura adesso".length + pad.length
        assertEquals(0, map.transformedToOriginal(0))
        assertEquals(11, map.transformedToOriginal(11))
        assertEquals(27, map.transformedToOriginal(12)) // inside the chip is after the «}»
        assertEquals(27, map.transformedToOriginal(chip1 - 1))
        assertEquals(27, map.transformedToOriginal(chip1))
        assertEquals(28, map.transformedToOriginal(chip1 + 1))
        assertEquals(message.length, map.transformedToOriginal(painted.length))
        // every painted offset lands on a saved one that comes back to it, or to the end of
        // the chip it was inside: never behind it
        (0..painted.length).forEach { t ->
            val back = map.originalToTransformed(map.transformedToOriginal(t))
            assertTrue("painted $t came back as $back", back >= t)
        }
    }

    @Test
    fun `a backspace after a chip takes the placeholder whole`() {
        // The field removed the «}» at 26 and left the cursor there.
        val after = message.removeRange(26, 27)
        assertEquals(
            "Si pedala:  e pioggia al {next_6h.precip_chance_max} {refuso" to 11,
            PlaceholderWords.wholeDeletion(message, after, 26, phrase)
        )
    }

    @Test
    fun `any other edit stands as the field made it`() {
        // A letter typed, a letter of the typo deleted, the brace of the typo: none is a chip.
        assertNull(PlaceholderWords.wholeDeletion(message, message + "a", message.length + 1, phrase))
        assertNull(PlaceholderWords.wholeDeletion(message, message.removeRange(70, 71), 70, phrase))
        val closed = "$message}"
        assertNull(PlaceholderWords.wholeDeletion(closed, message, message.length, phrase))
        // A «}» deleted inside a chip's text by a selection is not a one-brace edit either.
        assertNull(PlaceholderWords.wholeDeletion(message, message.removeRange(12, 27), 12, phrase))
    }
}
