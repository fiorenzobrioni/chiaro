package com.callbackdev.chiaro.domain.sky

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The lines of the eighty-eight (PLANNING, Fase 16, step 16.5): the list is the IAU's and
 * nothing else, a line ends on a star the canvas draws, a name is written where its
 * constellation is, and the shapes a reader knows are joined the way a reader knows them.
 */
class ConstellationsTest {

    /** The IAU's eighty-eight abbreviations, in the order of their Latin names. Written
     * here and not read from the file: the list has not changed since 1922. */
    private val iau = listOf(
        "And", "Ant", "Aps", "Aqr", "Aql", "Ara", "Ari", "Aur", "Boo", "Cae", "Cam", "Cnc", "CVn",
        "CMa", "CMi", "Cap", "Car", "Cas", "Cen", "Cep", "Cet", "Cha", "Cir", "Col", "Com", "CrA",
        "CrB", "Crv", "Crt", "Cru", "Cyg", "Del", "Dor", "Dra", "Equ", "Eri", "For", "Gem", "Gru",
        "Her", "Hor", "Hya", "Hyi", "Ind", "Lac", "Leo", "LMi", "Lep", "Lib", "Lup", "Lyn", "Lyr",
        "Men", "Mic", "Mon", "Mus", "Nor", "Oct", "Oph", "Ori", "Pav", "Peg", "Per", "Phe", "Pic",
        "Psc", "PsA", "Pup", "Pyx", "Ret", "Sge", "Sgr", "Sco", "Scl", "Sct", "Ser", "Sex", "Tau",
        "Tel", "Tri", "TrA", "Tuc", "UMa", "UMi", "Vel", "Vir", "Vol", "Vul"
    )

    private class Segment(val a: SkyTarget.Fixed, val b: SkyTarget.Fixed)

    private fun segments(constellation: Constellation): List<Segment> =
        List(constellation.segmentCount) {
            val at = it * Constellation.FLOATS_A_SEGMENT
            val l = constellation.lines
            Segment(
                SkyTarget.Fixed(l[at].toDouble(), l[at + 1].toDouble()),
                SkyTarget.Fixed(l[at + 2].toDouble(), l[at + 3].toDouble())
            )
        }

    private fun separation(a: SkyTarget.Fixed, b: SkyTarget.Fixed): Double = AstronomyMath.separation(
        AstronomyMath.Equatorial(a.raDeg, a.decDeg), AstronomyMath.Equatorial(b.raDeg, b.decDeg)
    )

    /** The same star, as a table of three decimals and a file of sixteen bits both hold it. */
    private fun same(a: SkyTarget.Fixed, b: SkyTarget.Fixed): Boolean =
        abs(AstronomyMath.norm180(a.raDeg - b.raDeg)) < 0.03 && abs(a.decDeg - b.decDeg) < 0.03

    private fun ends(id: String): List<SkyTarget.Fixed> =
        segments(Constellations.byId(id)!!).flatMap { listOf(it.a, it.b) }

    private fun joined(id: String, a: SkyTarget.Fixed, b: SkyTarget.Fixed): Boolean =
        segments(Constellations.byId(id)!!).any {
            (same(it.a, a) && same(it.b, b)) || (same(it.a, b) && same(it.b, a))
        }

    @Test
    fun `the list is the IAU's eighty-eight, each once, in the order of their Latin names`() {
        assertEquals(Constellations.COUNT, Constellations.all.size)
        assertEquals(iau, Constellations.all.map { it.id })
        assertEquals(88, iau.toSet().size)
        Constellations.all.forEach { assertTrue(it === Constellations.byId(it.id)) }
        // The capitals are the IAU's, and nothing else is found.
        assertNull(Constellations.byId("uma"))
        assertNull(Constellations.byId("Ser1"))
    }

    @Test
    fun `every end of every line is a star the canvas draws, to the last bit`() {
        val drawn = HashSet<Long>(BrightStars.count * 2)
        for (i in 0 until BrightStars.count) {
            drawn += (BrightStars.raDeg[i].toRawBits().toLong() shl 32) or
                (BrightStars.decDeg[i].toRawBits().toLong() and 0xFFFFFFFFL)
        }
        var ends = 0
        Constellations.all.forEach { constellation ->
            val l = constellation.lines
            for (i in l.indices step 2) {
                val key = (l[i].toRawBits().toLong() shl 32) or (l[i + 1].toRawBits().toLong() and 0xFFFFFFFFL)
                assertTrue("${constellation.id}: an end at ${l[i]}, ${l[i + 1]} is on no star", key in drawn)
                ends++
            }
        }
        // The source's 743 strokes, every one kept: two ends each.
        assertEquals(743 * 2, ends)
        assertEquals(743, Constellations.all.sumOf { it.segmentCount })
    }

    @Test
    fun `a line joins two different stars, once, and none runs across the sky`() {
        Constellations.all.forEach { constellation ->
            val lines = segments(constellation)
            assertTrue("${constellation.id} has no lines", lines.isNotEmpty())
            assertEquals(0, constellation.lines.size % Constellation.FLOATS_A_SEGMENT)
            val seen = HashSet<Set<SkyTarget.Fixed>>()
            lines.forEach { line ->
                val length = separation(line.a, line.b)
                assertTrue("${constellation.id}: a line of no length", length > 0.0)
                // The longest is in the Keel, 25.6 degrees from Canopus down the hull; a
                // line with an end decoded on the wrong side of the sky would be over ninety.
                assertTrue("${constellation.id}: a line $length degrees long", length < 26.0)
                assertTrue("${constellation.id}: the same line twice", seen.add(setOf(line.a, line.b)))
            }
        }
        // The fewest: the Little Dog and the Hunting Dogs are two stars and one line each.
        assertEquals(1, Constellations.byId("CMi")!!.segmentCount)
        assertEquals(1, Constellations.byId("CVn")!!.segmentCount)
    }

    @Test
    fun `the rank is one to three, and the ones everybody looks for are in the first`() {
        Constellations.all.forEach { assertTrue("${it.id}: rank ${it.rank}", it.rank in 1..3) }
        assertEquals(
            mapOf(1 to 22, 2 to 24, 3 to 42),
            Constellations.all.groupingBy { it.rank }.eachCount().toSortedMap()
        )
        listOf("Ori", "UMa", "Cas", "Sco", "Leo", "Cyg", "Tau", "Gem").forEach {
            assertEquals(it, 1, Constellations.byId(it)!!.rank)
        }
        listOf("Lyn", "Men", "Mic", "Sex").forEach { assertEquals(it, 3, Constellations.byId(it)!!.rank) }
    }

    @Test
    fun `a name is written where its constellation is, and the Serpent is named on both pieces`() {
        Constellations.all.forEach { constellation ->
            assertEquals(constellation.id, if (constellation.id == "Ser") 2 else 1, constellation.labels.size)
            val stars = ends(constellation.id)
            constellation.labels.forEach { label ->
                assertTrue(label.raDeg in 0.0..360.0 && label.decDeg in -90.0..90.0)
                val nearest = stars.minOf { separation(label, it) }
                // The farthest is the Stern's, 7.9 degrees from its nearest star: the name
                // of a large constellation sits in the middle of it, between its lines.
                assertTrue("${constellation.id}: its name is $nearest degrees from its lines", nearest < 8.0)
            }
        }
        // The head west of the Serpent Bearer, the tail east of him, 48 degrees apart.
        val serpent = Constellations.byId("Ser")!!.labels
        assertEquals(232.5, serpent[0].raDeg, 0.01)
        assertEquals(280.5, serpent[1].raDeg, 0.01)
        // And each piece's name is nearer its own lines than the other's name is.
        assertTrue(separation(serpent[0], serpent[1]) > 40.0)
    }

    @Test
    fun `the shapes a reader knows are joined the way a reader knows them`() {
        // The Plough: bowl to handle, one star to the next, and the bowl closed.
        val plough = SkyFigures.BigDipper.stars
        for (i in 0 until plough.lastIndex) {
            assertTrue("the Plough, star $i to ${i + 1}", joined("UMa", plough[i], plough[i + 1]))
        }
        assertTrue("the bowl is closed, Megrez to Dubhe", joined("UMa", plough[3], plough[0]))
        // The W, corner to corner.
        val w = SkyFigures.Cassiopeia.stars
        for (i in 0 until w.lastIndex) assertTrue("the W, star $i", joined("Cas", w[i], w[i + 1]))
        // Orion's belt: Alnitak, Alnilam, Mintaka.
        val orion = SkyFigures.Orion.stars
        assertTrue(joined("Ori", orion[2], orion[3]))
        assertTrue(joined("Ori", orion[3], orion[4]))
        // The Cross is two lines that cross and share no star: Acrux to Gacrux, Mimosa to Imai.
        val cross = SkyFigures.SouthernCross.stars
        assertEquals(2, Constellations.byId("Cru")!!.segmentCount)
        assertTrue(joined("Cru", cross[0], cross[2]))
        assertTrue(joined("Cru", cross[1], cross[3]))
        // The Little Bear's tail ends on the Pole Star.
        assertTrue(ends("UMi").any { same(it, SkyTarget.Fixed(37.955, 89.264)) })
        // Alpheratz is Andromeda's head and the fourth corner of the Great Square: both draw to it.
        val alpheratz = SkyFigures.GreatSquare.stars[3]
        assertTrue(ends("And").any { same(it, alpheratz) })
        assertTrue(ends("Peg").any { same(it, alpheratz) })
    }

    @Test
    fun `a figure of the worth seeing list is filed under a constellation that is drawn`() {
        SkyFigures.all.forEach { figure ->
            assertNotNull("${figure.id} is filed under ${figure.constellation}", Constellations.byId(figure.constellation))
            // A figure that is a constellation whole is made of stars its lines pass
            // through: the list sends a reader out for the shape the explorer draws.
            if (figure.kind == SkyFigureKind.CONSTELLATION) {
                val stars = ends(figure.constellation)
                figure.stars.forEachIndexed { index, star ->
                    assertTrue("${figure.id} star $index is on no line of ${figure.constellation}", stars.any { same(it, star) })
                }
            }
        }
    }
}
