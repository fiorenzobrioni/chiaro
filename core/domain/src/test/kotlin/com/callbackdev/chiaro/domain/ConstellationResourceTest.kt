package com.callbackdev.chiaro.domain

import com.callbackdev.chiaro.domain.sky.BrightStars
import com.callbackdev.chiaro.domain.sky.Constellations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The constellations' file is found the way the star catalogue's is ([StarCatalogueResourceTest],
 * the night crash of 20 ott 2026): by its absolute path, from a class in another package,
 * because the minified build's R8 moves `Constellations` and leaves the resource where
 * the module put it. This test stands one package up, where the relative name fails as it
 * would on the phone.
 */
class ConstellationResourceTest {

    @Test
    fun `the file is found by its absolute path from any package`() {
        assertTrue("the path must be absolute, R8 moves the class", Constellations.RESOURCE.startsWith("/"))
        assertNotNull(javaClass.getResourceAsStream(Constellations.RESOURCE))
    }

    @Test
    fun `the relative name is the trap, and it fails from here`() {
        assertNull(javaClass.getResourceAsStream("constellations.bin"))
    }

    @Test
    fun `it sits beside the stars it joins, and is not them`() {
        assertEquals(
            BrightStars.RESOURCE.substringBeforeLast('/'),
            Constellations.RESOURCE.substringBeforeLast('/')
        )
        assertNotEquals(BrightStars.RESOURCE, Constellations.RESOURCE)
        // The magic is its own: a reader handed the wrong file says so instead of drawing it.
        val magic = javaClass.getResourceAsStream(Constellations.RESOURCE)!!.use { it.readNBytes(4) }
        assertEquals("CHCN", String(magic, Charsets.US_ASCII))
    }
}
