package com.callbackdev.chiaro.ui.components

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.acos

/**
 * The meteor of a peak night, placed and timed (17 nov 2026, committente: «on the Sky's hero I
 * can hardly see them: very fast, and only at the very top»). The canvas of the Sky's hero on
 * a phone: 411 dp across, a stage 420 dp tall at density 3, the place row and the status bar
 * over its first 96 dp, «Explore» under them, the foot's line and chip at the bottom.
 */
class MeteorTrailTest {

    private val density = 3f
    private val width = 411f * density
    private val stage = 420f * density
    private val openTop = (96f + 40f) * density
    private val openBottom = stage - 60f * density

    /** Where a peak night's radiant stands in the stars' camera: low in the middle (the window
     * faces it), under the foot, off to either side (the window faces the moon), in the open. */
    private val radiants = listOf(
        "low, ahead" to Offset(width / 2f, stage * 0.95f),
        "under the foot" to Offset(width / 2f, stage * 1.2f),
        "far left, low" to Offset(-2.5f * width, stage * 0.9f),
        "right, high" to Offset(1.6f * width, stage * 0.3f),
        "in the open sky" to Offset(width * 0.4f, (openTop + openBottom) / 2f),
        "over the canvas" to Offset(width * 0.7f, -0.5f * stage)
    )

    private fun flights(radiant: Offset, seed: Int = 37, count: Int = 2_000) =
        (0L until count).mapNotNull { MeteorTrail.flight(it, seed, radiant, width, openTop, openBottom, density) }

    @Test
    fun `the whole trail is in the open sky, clear of the place row, the foot and the sides`() {
        val m = MeteorTrail.EdgeMarginDp * density
        val inset = MeteorTrail.InsetDp * density
        val eps = 0.01f
        radiants.forEach { (name, radiant) ->
            val all = flights(radiant)
            assertEquals("$name: every period has its meteor", 2_000, all.size)
            all.forEach { f ->
                listOf(f.start, f.stop).forEach { p ->
                    assertTrue("$name: $p across", p.x >= m - eps && p.x <= width - m + eps)
                    assertTrue("$name: $p up and down", p.y >= openTop + inset - eps && p.y <= openBottom - inset + eps)
                }
            }
        }
    }

    @Test
    fun `a trail runs exactly away from the radiant`() {
        radiants.forEach { (name, radiant) ->
            flights(radiant).forEach { f ->
                val along = (f.stop - f.start) / f.length
                val mid = (f.start + f.stop) / 2f
                val out = (mid - radiant) / (mid - radiant).getDistance()
                val deg = Math.toDegrees(acos((along.x * out.x + along.y * out.y).toDouble().coerceIn(-1.0, 1.0)))
                assertTrue("$name: ${"%.3f".format(deg)}° off the radiant's line", deg < 0.05)
            }
        }
    }

    @Test
    fun `under a low radiant the trails run up, and they are not pressed against the top`() {
        val low = flights(radiants.first().second)
        assertTrue(low.all { it.stop.y < it.start.y })
        // Where the old shader put nine in ten heads at their brightest, behind the place row,
        // no trail is now; and their middles spread over the open sky, not along its top.
        val middles = low.map { (it.start.y + it.stop.y) / 2f }.sorted()
        val share = (middles[middles.size / 2] - openTop) / (openBottom - openTop)
        assertTrue("the median middle at ${"%.2f".format(share)} of the open sky", share in 0.3f..0.7f)
    }

    @Test
    fun `each trail is its own, under the one rule`() {
        val all = flights(radiants[2].second)
        val lengths = all.map { it.length / density }
        val durations = all.map { it.duration }
        val bright = all.map { it.bright }
        assertTrue("lengths ${lengths.min()}..${lengths.max()} dp", lengths.max() - lengths.min() > 60f)
        assertTrue(durations.all { it in MeteorTrail.MinDuration..MeteorTrail.MaxDuration })
        assertTrue("durations ${durations.min()}..${durations.max()} s", durations.max() - durations.min() > 0.5f)
        assertTrue(bright.all { it in 0f..1f })
        // Most are middling, a few are bright.
        assertTrue(bright.count { it > 0.8f } in (all.size / 20)..(all.size / 4))
        assertTrue(bright.count { it < 0.5f } > all.size / 2)
        // Not two meteors at the same moment of their periods, nor in the same place.
        assertTrue(all.map { (it.at * 10).toInt() }.toSet().size > 300)
        assertTrue(all.map { (it.start.x / 30).toInt() to (it.start.y / 30).toInt() }.toSet().size > 100)
    }

    @Test
    fun `a trail near the radiant is shorter and slower, as a meteor seen end-on is`() {
        val radiant = Offset(width / 2f, (openTop + openBottom) / 2f)
        val all = flights(radiant)
        fun distance(f: MeteorFlight) = ((f.start + f.stop) / 2f - radiant).getDistance()
        val sorted = all.sortedBy(::distance)
        val near = sorted.take(all.size / 5)
        val far = sorted.takeLast(all.size / 5)
        assertTrue(near.map { it.length }.average() < 0.8 * far.map { it.length }.average())
        assertTrue(near.map { it.length / it.duration }.average() < far.map { it.length / it.duration }.average())
    }

    @Test
    fun `long enough to be seen, and never a pop`() {
        val radiant = radiants.first().second
        var checked = 0
        for (cycle in 0L until 200L) {
            val f = MeteorTrail.flight(cycle, 37, radiant, width, openTop, openBottom, density) ?: continue
            val t0 = cycle * MeteorPeriodSeconds + f.at
            val step = 1f / 240f
            var t = t0 - 0.2f
            var last: MeteorStroke? = null
            var shown = 0f
            var lastHead = -1f
            while (t < t0 + f.duration + MeteorTrail.Afterglow + 0.2f) {
                val s = MeteorTrail.at(t, 37, radiant, width, openTop, openBottom, density)
                if (s != null) {
                    // Strong for most of its flight: well over half a second.
                    if (s.alpha > 0.5f * (0.78f + 0.22f * f.bright)) shown += step
                    // No jump from one frame to the next, coming on, going out or between.
                    val before = last?.alpha ?: 0f
                    assertTrue("cycle $cycle at ${t - t0}: ${s.alpha} after $before", abs(s.alpha - before) < 0.05f)
                    // The head moves one way only and stops where the meteor goes out.
                    val head = (s.head - f.start).getDistance()
                    assertTrue(head >= lastHead - 1e-3f)
                    lastHead = head
                    assertTrue(MeteorTrail.nearFlight(t, 37, lookAhead = 0f))
                } else {
                    assertTrue("cycle $cycle: nothing out of its time", t < t0 || t >= t0 + f.duration + MeteorTrail.Afterglow - step)
                    assertTrue("cycle $cycle went out from ${last?.alpha}", (last?.alpha ?: 0f) < 0.05f)
                }
                last = s
                t += step
            }
            assertTrue("cycle $cycle shown ${"%.2f".format(shown)} s", shown > 0.6f)
            checked++
        }
        assertEquals(200, checked)
    }

    @Test
    fun `between meteors nothing is drawn, and the canvas is not asked for more frames`() {
        val radiant = radiants.first().second
        val f = MeteorTrail.flight(0L, 37, radiant, width, openTop, openBottom, density)!!
        val quiet = if (f.at > 10f) f.at - 5f else f.at + 10f
        assertNull(MeteorTrail.at(quiet, 37, radiant, width, openTop, openBottom, density))
        assertFalse(MeteorTrail.nearFlight(quiet, 37, lookAhead = 0.07f))
        assertTrue(MeteorTrail.nearFlight(f.at - 0.05f, 37, lookAhead = 0.07f))
        assertNotNull(MeteorTrail.at(f.at + f.duration / 2f, 37, radiant, width, openTop, openBottom, density))
    }

    @Test
    fun `an open sky too small for a trail has none`() {
        val radiant = radiants.first().second
        val thin = (0L until 50L).mapNotNull {
            MeteorTrail.flight(it, 37, radiant, width, openTop, openTop + 40f * density, density)
        }
        assertTrue(thin.isEmpty())
    }

    @Test
    fun `another night is another sequence, the same night the same`() {
        val radiant = radiants.first().second
        val a = flights(radiant, seed = 37, count = 20)
        assertEquals(a, flights(radiant, seed = 37, count = 20))
        val b = flights(radiant, seed = 38, count = 20)
        assertTrue(a.zip(b).count { (x, y) -> x.at != y.at } > 15)
    }
}
