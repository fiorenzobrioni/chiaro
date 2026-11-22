package com.callbackdev.chiaro.domain.correction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The blend of days 6 and 7 (Fase 18, PLANNING «18.4»): where it applies, the weights the tool
 * wrote, and the arithmetic of the members and the dew point.
 */
class TemperatureBlendTest {

    @Test
    fun `only Italy, only up to 350 m, two zones split at 44 degrees`() {
        assertEquals(BlendZone.PO_VALLEY, TemperatureBlend.zoneOf("IT", 45.46, 147.0)) // Milano
        assertEquals(BlendZone.PO_VALLEY, TemperatureBlend.zoneOf("it", 44.49, 54.0)) // Bologna
        assertEquals(BlendZone.CENTRE_SOUTH, TemperatureBlend.zoneOf("IT", 43.77, 50.0)) // Firenze
        assertEquals(BlendZone.CENTRE_SOUTH, TemperatureBlend.zoneOf("IT", 38.12, 21.0)) // Palermo
        assertEquals(BlendZone.CENTRE_SOUTH, TemperatureBlend.zoneOf("SM", 43.94, 300.0))
        assertNull(TemperatureBlend.zoneOf("IT", 45.74, 583.0)) // Aosta: the mountain is the model's
        assertNull(TemperatureBlend.zoneOf("CH", 47.37, 408.0)) // Zurigo: not measured for this
        assertNull(TemperatureBlend.zoneOf("FR", 43.30, 10.0))
        assertNull(TemperatureBlend.zoneOf(null, 45.46, 147.0))
        assertNull(TemperatureBlend.zoneOf("IT", 45.46, null)) // no elevation, no claim it is low
    }

    @Test
    fun `the table covers both zones on days 6 and 7, every row summing to one`() {
        for (zone in BlendZone.entries) {
            for (lead in listOf(6, 7)) {
                val w = TemperatureBlend.weights(zone, lead)
                assertNotNull(w)
                w!!
                assertEquals(1.0, w.bestMatch + w.ifs + w.ukmo, 1e-9)
            }
            assertNull(TemperatureBlend.weights(zone, 5))
        }
        assertEquals(mapOf(5 to 6, 6 to 7), TemperatureBlend.LEAD_DAY_OF_INDEX)
    }

    @Test
    fun `a missing member renormalises the others, and none leaves best_match`() {
        val w = BlendWeights(bestMatch = 0.3, ifs = 0.3, ukmo = 0.4)
        assertEquals(0.3 * 10 + 0.3 * 12 + 0.4 * 9, TemperatureBlend.blend(w, 10.0, 12.0, 9.0), 1e-9)
        assertEquals((0.3 * 10 + 0.4 * 9) / 0.7, TemperatureBlend.blend(w, 10.0, null, 9.0), 1e-9)
        assertEquals((0.3 * 10 + 0.3 * 12) / 0.6, TemperatureBlend.blend(w, 10.0, 12.0, null), 1e-9)
        assertEquals(10.0, TemperatureBlend.blend(w, 10.0, null, null), 1e-9)
    }

    @Test
    fun `the dew point follows the air at the model's humidity`() {
        // Saturated air: the dew point is the temperature.
        assertEquals(15.0, TemperatureBlend.dewPointC(15.0, 100.0), 1e-6)
        // 20 °C at 50%: 9.3 °C (Magnus, Alduchov and Eskridge).
        assertEquals(9.3, TemperatureBlend.dewPointC(20.0, 50.0), 0.05)
        // And back: the humidity of the blended air with its dew point is the model's.
        for (t in listOf(-10.0, 0.0, 12.5, 30.0)) for (rh in listOf(30.0, 65.0, 95.0)) {
            assertEquals(rh, TemperatureBlend.humidityPct(t, TemperatureBlend.dewPointC(t, rh)), 1e-6)
        }
    }
}
