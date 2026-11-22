package com.callbackdev.chiaro.domain.correction

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The calibrated probability of the first day (Fase 18, PLANNING «18.5»): where it applies,
 * the regression the tool wrote, and the behaviour a reader can check.
 */
class RainProbabilityTest {

    @Test
    fun `only where it passed, the Po valley up to 350 m`() {
        assertEquals(BlendZone.PO_VALLEY, RainProbability.zoneOf("IT", 45.46, 147.0)) // Milano
        assertNull(RainProbability.zoneOf("IT", 43.77, 50.0)) // Firenze: the centre-south failed
        assertNull(RainProbability.zoneOf("IT", 38.12, 21.0)) // Palermo
        assertNull(RainProbability.zoneOf("IT", 45.74, 583.0)) // Aosta
        assertNull(RainProbability.zoneOf("CH", 47.37, 408.0))
        assertNull(RainProbability.calibrated(BlendZone.CENTRE_SOUTH, 50, 1.0, listOf(1.0)))
    }

    @Test
    fun `without a witness the hour stays the model's`() {
        assertNull(RainProbability.calibrated(BlendZone.PO_VALLEY, 80, 2.0, emptyList()))
    }

    @Test
    fun `the regression is the table's`() {
        val b = RainProbabilityTable.COEFFICIENTS.getValue(BlendZone.PO_VALLEY)
        val p = 0.6
        val share = 2.0 / 5
        val z = b[0] + b[1] * ln(p / (1 - p)) + b[2] * share + b[3] * 1.0 + b[4] * ln(1 + 0.4)
        val expected = (100.0 / (1 + exp(-z))).roundToInt()
        assertEquals(expected, RainProbability.calibrated(BlendZone.PO_VALLEY, 60, 0.4, listOf(0.3, 0.0, 0.0, 0.0)))
    }

    @Test
    fun `more models seeing rain, a higher probability, and the model own still counts`() {
        val zone = BlendZone.PO_VALLEY
        val alone = RainProbability.calibrated(zone, 99, 0.2, listOf(0.0, 0.0, 0.0, 0.0))!!
        val all = RainProbability.calibrated(zone, 99, 0.2, listOf(0.5, 0.3, 0.2, 0.4))!!
        // `best_match` sure of itself and alone: wet 87% of the time at 99%, and far less alone.
        assertTrue("alone $alone, all $all", alone < all)
        assertTrue(alone < 90)
        var last = -1
        for (p in 0..100 step 5) {
            val q = RainProbability.calibrated(zone, p, 0.0, listOf(0.0, 0.0, 0.0, 0.0))!!
            assertTrue("$p% gave $q after $last", q >= last)
            last = q
        }
        // A dry hour everywhere stays near the frequency the airports saw under 5%: about 2%.
        assertTrue(RainProbability.calibrated(zone, 0, 0.0, listOf(0.0, 0.0, 0.0, 0.0))!! <= 3)
    }
}
