package com.callbackdev.chiaro.ui.today

import androidx.compose.ui.unit.em
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The hero's small tenths (23 set 2026): the text is the formatter's, whole; only the
 * part between the degrees and the degree sign is set small. */
class HeroTemperatureTextTest {

    @Test
    fun `the tenths are small and nothing else is`() {
        val text = heroTemperatureText("20,8°")
        assertEquals("20,8°", text.text)
        val span = text.spanStyles.single()
        assertEquals(",8", text.text.substring(span.start, span.end))
        assertEquals(0.55f.em, span.item.fontSize)
    }

    @Test
    fun `a negative reading keeps its sign at full size`() {
        val text = heroTemperatureText("-3.2°")
        assertEquals(".2", text.text.substring(text.spanStyles.single().start, text.spanStyles.single().end))
    }

    @Test
    fun `a whole number has nothing to shrink`() {
        assertTrue(heroTemperatureText("21°").spanStyles.isEmpty())
    }
}
