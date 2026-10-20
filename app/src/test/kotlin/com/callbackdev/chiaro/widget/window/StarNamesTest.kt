package com.callbackdev.chiaro.widget.window

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The named stars: the brightest eighteen, each once, each where the catalogue has it. */
class StarNamesTest {

    @Test
    fun `eighteen names, brightest first, each on the sphere`() {
        assertEquals(18, NamedStars.size)
        assertEquals(18, NamedStars.map { it.nameRes }.toSet().size)
        NamedStars.forEach { star ->
            assertTrue(star.raDeg in 0.0..360.0)
            assertTrue(star.decDeg in -90.0..90.0)
            assertTrue("a named star is a bright one: ${star.mag}", star.mag < 1.6)
        }
        assertEquals(NamedStars.sortedBy { it.mag }, NamedStars)
        // Sirius leads, at its magnitude.
        assertEquals(-1.46, NamedStars.first().mag, 1e-9)
    }
}
