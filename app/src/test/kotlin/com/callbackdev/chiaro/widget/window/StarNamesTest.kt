package com.callbackdev.chiaro.widget.window

import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.sky.BrightStars
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The named stars: each once, brightest first, each where the catalogue has it. */
class StarNamesTest {

    /** Whether the catalogue's star [index] stands at [star]'s place: within three hundredths
     * of a degree, which is what three decimals and sixteen bits hold. No named star is
     * near 0h, so the right ascensions are compared as they are. */
    private fun at(index: Int, star: NamedStar): Boolean =
        abs(BrightStars.raDeg[index] - star.raDeg) < 0.03 && abs(BrightStars.decDeg[index] - star.decDeg) < 0.03

    /** The magnitude of the catalogue's brightest star at [star]'s place, or null. */
    private fun catalogued(star: NamedStar): Float? =
        (0 until BrightStars.count).filter { at(it, star) }.minOfOrNull { BrightStars.mag[it] }

    @Test
    fun `thirty-one names, brightest first, each a star the canvas draws`() {
        assertEquals(31, NamedStars.size)
        assertEquals(31, NamedStars.map { it.nameRes }.toSet().size)
        NamedStars.forEach { star ->
            val mag = catalogued(star)
            assertTrue("no catalogue star at ${star.raDeg}, ${star.decDeg}", mag != null)
            // The list's magnitude is the eye's: a double's two halves together are up to
            // half a magnitude brighter than the brighter half the catalogue lists (Acrux,
            // 0.77 against 1.33; Castor, 1.58 against 1.98). The other way it is Antares,
            // which varies: 0.96 in the catalogue, 1.09 in the list the window came with.
            assertTrue("${star.mag} against the catalogue's $mag", star.mag <= mag!! + 0.15 && star.mag > mag - 0.6)
        }
        assertEquals(NamedStars.sortedBy { it.mag }, NamedStars)
        // Sirius leads, at its magnitude.
        assertEquals(-1.46, NamedStars.first().mag, 1e-9)
    }

    @Test
    fun `the thirty brightest of the sky and the Pole Star, and nothing between`() {
        // The thirtieth is Alnair; the next the catalogue has is Alioth, at 1.77.
        val thirty = NamedStars.dropLast(1)
        assertTrue(thirty.all { it.mag < 1.75 })
        // Every catalogue star brighter than that is one of them (a double's fainter half
        // is the same point of light as its brighter one, at the same place).
        for (i in 0 until BrightStars.count) {
            if (BrightStars.mag[i] >= 1.75f) continue
            assertTrue(
                "the catalogue's star at ${BrightStars.raDeg[i]}, ${BrightStars.decDeg[i]} has no name",
                thirty.any { at(i, it) }
            )
        }
        val pole = NamedStars.last()
        assertEquals(R.string.star_polaris, pole.nameRes)
        assertTrue("the Pole Star is within a degree of the pole", pole.decDeg > 89.0)
    }

    @Test
    fun `the window keeps to its eighteen`() {
        assertEquals(18, WindowStars.size)
        assertTrue(WindowStars.all { it.onWindow && it.mag < 1.6 })
        assertEquals(WindowStars, NamedStars.filter { it in WindowStars })
        assertEquals(R.string.star_sirius, WindowStars.first().nameRes)
        assertEquals(R.string.star_castor, WindowStars.last().nameRes)
        // The thirteen of step 16.5 are the explorer's.
        val explorers = NamedStars - WindowStars.toSet()
        assertEquals(13, explorers.size)
        assertFalse(explorers.any { it.onWindow })
        assertTrue(R.string.star_polaris in explorers.map { it.nameRes })
        assertTrue(R.string.star_alpha_centauri in explorers.map { it.nameRes })
    }
}
