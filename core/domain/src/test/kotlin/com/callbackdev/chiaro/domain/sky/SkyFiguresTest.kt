package com.callbackdev.chiaro.domain.sky

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The figures the «worth seeing» list may name (PLANNING, Fase 16, step 16.4): each is made
 * of stars the app's own catalogue draws, bright enough for the rule the table states, and
 * its middle is where its stars are.
 */
class SkyFiguresTest {

    /** The catalogue's star at a figure's star, or null: within three hundredths of a
     * degree, which is what the binary's 16-bit fields and three decimals hold. */
    private fun catalogued(star: SkyTarget.Fixed): Int? = (0 until BrightStars.count).firstOrNull {
        abs(AstronomyMath.norm180(BrightStars.raDeg[it] - star.raDeg)) < 0.03 &&
            abs(BrightStars.decDeg[it] - star.decDeg) < 0.03
    }

    @Test
    fun `every star of every figure is a star the canvas draws, and bright enough to be found`() {
        SkyFigures.all.forEach { figure ->
            figure.stars.forEachIndexed { index, star ->
                val found = catalogued(star)
                assertNotNull("${figure.id} star $index (${star.raDeg}, ${star.decDeg}) is not in the catalogue", found)
                val mag = BrightStars.mag[found!!]
                // A shape is drawn by stars a town still shows; a cluster is fainter stars
                // close together, and the Hyades' go to 3.8.
                val faintest = if (figure.kind == SkyFigureKind.CLUSTER) 4.0f else 3.5f
                assertTrue("${figure.id} star $index is magnitude $mag", mag < faintest)
            }
        }
    }

    @Test
    fun `the table is eleven figures with ids of their own, in the order the sky turns them past`() {
        assertEquals(11, SkyFigures.all.size)
        assertEquals(SkyFigures.all.size, SkyFigures.all.map { it.id }.toSet().size)
        SkyFigures.all.forEach { assertEquals(it, SkyFigures.byId(it.id)) }
        val turn = SkyFigures.all.map { it.centre.raDeg }
        assertEquals(turn.sorted(), turn)
    }

    @Test
    fun `the middle of a figure is among its stars, across the wrap of right ascension too`() {
        SkyFigures.all.forEach { figure ->
            val centre = AstronomyMath.Equatorial(figure.centre.raDeg, figure.centre.decDeg)
            val farthest = figure.stars.maxOf {
                AstronomyMath.separation(centre, AstronomyMath.Equatorial(it.raDeg, it.decDeg))
            }
            // The widest is the Summer Triangle, whose corners are some twenty degrees
            // from its middle; a middle on the wrong side of the sky would be at ninety.
            assertTrue("${figure.id}: a star $farthest degrees from the middle", farthest < 25.0)
        }
        // The Great Square straddles 0h: two stars at 346°, two at 2° and 3°. The plain
        // mean of those is 174°, in the Lion; the middle is at 354°.
        assertEquals(354.4, SkyFigures.GreatSquare.centre.raDeg, 0.5)
        assertEquals(22.0, SkyFigures.GreatSquare.centre.decDeg, 0.5)
        // One star, and the middle is that star.
        assertEquals(SkyFigures.Pleiades.stars.single().raDeg, SkyFigures.Pleiades.centre.raDeg, 1e-6)
        assertEquals(SkyFigures.Pleiades.stars.single().decDeg, SkyFigures.Pleiades.centre.decDeg, 1e-6)
    }

    @Test
    fun `the three letters are the IAU's`() {
        val codes = SkyFigures.all.associate { it.id to it.constellation }
        assertEquals(
            mapOf(
                "cassiopeia" to "Cas", "pleiades" to "Tau", "hyades" to "Tau", "orion" to "Ori",
                "twins" to "Gem", "lion" to "Leo", "big_dipper" to "UMa", "southern_cross" to "Cru",
                "scorpion" to "Sco", "summer_triangle" to "Cyg", "great_square" to "Peg"
            ),
            codes
        )
    }
}
