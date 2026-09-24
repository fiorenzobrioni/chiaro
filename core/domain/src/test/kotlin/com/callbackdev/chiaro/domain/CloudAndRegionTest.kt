package com.callbackdev.chiaro.domain

import com.callbackdev.chiaro.domain.model.CloudLayers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The two small judgements the second pass of the engine review added (24 set 2026). */
class CloudAndRegionTest {

    @Test
    fun `a layer makes the sky when it is twice every other`() {
        assertEquals(CloudLayers.Layer.HIGH, CloudLayers(10, 5, 60).dominant())
        assertEquals(CloudLayers.Layer.LOW, CloudLayers(80, 40, 0).dominant())
        assertEquals(CloudLayers.Layer.MID, CloudLayers(null, 50, null).dominant())
    }

    @Test
    fun `no layer is named for a mixed or open sky`() {
        assertNull(CloudLayers(50, 10, 40).dominant()) // two layers of a sky
        assertNull(CloudLayers(15, 5, 0).dominant())  // too little to name
        assertNull(CloudLayers(null, null, null).dominant())
    }

    @Test
    fun `Europe is a list of countries, not a box`() {
        assertTrue(PlaceRegion.inEurope("IT"))
        assertTrue(PlaceRegion.inEurope("ch"))
        assertTrue(PlaceRegion.inEurope("GB"))
        assertFalse(PlaceRegion.inEurope("TN")) // inside any box that holds Sicily
        assertFalse(PlaceRegion.inEurope("US"))
        assertFalse(PlaceRegion.inEurope(null))
    }
}
