package com.callbackdev.chiaro.domain.sky

import com.callbackdev.chiaro.domain.model.Coordinates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * The stars the canvas draws and the moon's bright limb (step 6 of the sky review, 19 ott
 * 2026): the catalogue is the Yale one and nothing invented, a star stands where the sky puts
 * it, and the limb points at the sun.
 */
class BrightStarsTest {

    private val milan = Coordinates(45.4642, 9.19)

    @Test
    fun `the catalogue is the stars to the sixth magnitude, brightest first, Sirius at the head`() {
        val n = BrightStars.count
        assertTrue("$n stars", n in 4900..5300)
        assertEquals(n, BrightStars.raDeg.size)
        assertEquals(n, BrightStars.decDeg.size)
        assertEquals(n, BrightStars.bv.size)
        assertTrue(BrightStars.mag.all { it <= BrightStars.LIMIT_MAG })
        for (i in 1 until n) assertTrue("sorted at $i", BrightStars.mag[i] >= BrightStars.mag[i - 1])
        // Sirius: 06h 45m 08.9s, −16° 42′ 58″, −1.46; the binary's 16-bit fields hold a
        // hundredth of a degree.
        assertEquals(101.287, BrightStars.raDeg[0].toDouble(), 0.01)
        assertEquals(-16.716, BrightStars.decDeg[0].toDouble(), 0.01)
        assertEquals(-1.46, BrightStars.mag[0].toDouble(), 0.01)
        // Polaris and Vega are in it.
        assertTrue(BrightStars.decDeg.any { it > 89.2f })
        assertTrue((0 until n).any { abs(BrightStars.raDeg[it] - 279.23f) < 0.05f && abs(BrightStars.decDeg[it] - 38.78f) < 0.05f })
    }

    @Test
    fun `Polaris stands at the latitude, due north, whatever the hour`() {
        val polaris = (0 until BrightStars.count).first { BrightStars.decDeg[it] > 89.2f }
        listOf("2026-01-15T22:00:00Z", "2026-07-15T02:00:00Z", "2026-10-19T19:30:00Z").forEach { iso ->
            val h = AstronomyEngine.starHorizontal(
                BrightStars.raDeg[polaris].toDouble(), BrightStars.decDeg[polaris].toDouble(), Instant.parse(iso), milan
            )
            assertEquals(iso, milan.lat, h.altitudeDeg, 1.0)
            val fromNorth = minOf(h.azimuthDeg, 360.0 - h.azimuthDeg)
            assertTrue("$iso: azimuth ${h.azimuthDeg}", fromNorth < 1.5)
        }
    }

    @Test
    fun `Sirius never climbs over its transit height from Milan`() {
        var highest = -90.0
        var at = Instant.parse("2026-01-01T00:00:00Z")
        repeat(24 * 6) {
            highest = maxOf(highest, AstronomyEngine.starHorizontal(101.287, -16.716, at, milan).altitudeDeg)
            at = at.plusSeconds(600)
        }
        assertEquals(90.0 - milan.lat - 16.716, highest, 0.3)
    }

    @Test
    fun `the bright limb points at the sun`() {
        // The sun to the right at the same height: lit from the right (a few degrees up, because
        // a parallel of altitude bends above the tangent toward more azimuth).
        assertEquals(0.0, AstronomyEngine.brightLimbAngle(20.0, 200.0, 20.0, 180.0), 5.0)
        // The sun straight under the moon: lit from below.
        assertEquals(-90.0, AstronomyEngine.brightLimbAngle(-10.0, 180.0, 20.0, 180.0), 1.0)
        // The sun straight over it: lit from above.
        assertEquals(90.0, AstronomyEngine.brightLimbAngle(50.0, 180.0, 20.0, 180.0), 1.0)
        // An evening crescent in the west, the sun just set a little further round: the limb
        // points down and to the right, the «boat».
        val evening = AstronomyEngine.brightLimbAngle(-8.0, 262.0, 14.0, 248.0)
        assertTrue("evening $evening", evening < -20.0 && evening > -80.0)
        // Mirrored across the moon's meridian, the angle mirrors.
        val mirrored = AstronomyEngine.brightLimbAngle(-8.0, 234.0, 14.0, 248.0)
        assertEquals(180.0 - abs(evening), abs(mirrored), 2.0)
        assertTrue(mirrored < -90.0)
    }

    @Test
    fun `the limb's arithmetic agrees with the vectors it is made of`() {
        // Any two directions: the angle reconstructs the sun's tangent-plane direction.
        val (sa, sz, ma, mz) = listOf(-5.0, 240.0, 30.0, 200.0)
        val angle = Math.toRadians(AstronomyEngine.brightLimbAngle(sa, sz, ma, mz))
        val am = Math.toRadians(ma); val zm = Math.toRadians(mz)
        val east = doubleArrayOf(cos(zm), -sin(zm), 0.0)
        val up = doubleArrayOf(-sin(am) * sin(zm), -sin(am) * cos(zm), cos(am))
        val dir = DoubleArray(3) { east[it] * cos(angle) + up[it] * sin(angle) }
        val a = Math.toRadians(sa); val z = Math.toRadians(sz)
        val sun = doubleArrayOf(cos(a) * sin(z), cos(a) * cos(z), sin(a))
        val moon = doubleArrayOf(cos(am) * sin(zm), cos(am) * cos(zm), sin(am))
        val along = (0..2).sumOf { sun[it] * moon[it] }
        val tangent = DoubleArray(3) { sun[it] - along * moon[it] }
        val norm = Math.sqrt(tangent.sumOf { it * it })
        (0..2).forEach { assertEquals(tangent[it] / norm, dir[it], 1e-9) }
    }
}
