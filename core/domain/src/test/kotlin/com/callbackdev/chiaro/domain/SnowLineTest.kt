package com.callbackdev.chiaro.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The snow line (14.8): the model's own, or the freezing level less 300 m said to be an
 * estimate; the lowest of a day; and the two questions a place asks of it.
 */
class SnowLineTest {

    @Test
    fun `the model's own line wins, and is not an estimate`() {
        val line = SnowLine.of(snowfallHeightM = 1720.0, freezingLevelM = 2100.0)!!
        assertEquals(1720, line.metres)
        assertFalse(line.estimated)
    }

    @Test
    fun `without the model's line, the freezing level less 300 m, said to be circa`() {
        val line = SnowLine.of(snowfallHeightM = null, freezingLevelM = 1500.0)!!
        assertEquals(1200, line.metres)
        assertTrue(line.estimated)
    }

    @Test
    fun `with neither there is no line, and a line under the sea is the sea`() {
        assertNull(SnowLine.of(null, null))
        assertEquals(0, SnowLine.of(null, 120.0)!!.metres)
        assertEquals(0, SnowLine.of(-40.0, null)!!.metres)
    }

    @Test
    fun `the lowest of a day keeps its own provenance`() {
        val lowest = SnowLine.lowest(
            listOf(SnowLine(1700, estimated = true), SnowLine(1650, estimated = false), SnowLine(1800, estimated = true))
        )!!
        assertEquals(SnowLine(1650, estimated = false), lowest)
        assertNull(SnowLine.lowest(emptyList()))
    }

    @Test
    fun `a line matters within 1 500 m above the place, or anywhere below it`() {
        // Milan, 120 m: 1 200 m is the Prealps, 2 800 m is every winter rain.
        assertTrue(SnowLine(1200, true).matters(elevationM = 120.0))
        assertTrue(SnowLine(1620, true).matters(elevationM = 120.0))
        assertFalse(SnowLine(1621, true).matters(elevationM = 120.0))
        assertFalse(SnowLine(2800, true).matters(elevationM = 120.0))
        // Bormio, 1 225 m: a line at 800 m is its own snow, and still a fact.
        assertTrue(SnowLine(800, false).matters(elevationM = 1225.0))
        // No height known: the place reads as the sea.
        assertTrue(SnowLine(1500, true).matters(elevationM = null))
        assertFalse(SnowLine(1501, true).matters(elevationM = null))
    }

    @Test
    fun `the snow reaches the place at or below its height`() {
        assertTrue(SnowLine(1225, false).reaches(elevationM = 1225.0))
        assertTrue(SnowLine(800, false).reaches(elevationM = 1225.0))
        assertFalse(SnowLine(1226, false).reaches(elevationM = 1225.0))
        assertTrue(SnowLine(0, true).reaches(elevationM = null))
        assertFalse(SnowLine(1, true).reaches(elevationM = null))
    }
}
