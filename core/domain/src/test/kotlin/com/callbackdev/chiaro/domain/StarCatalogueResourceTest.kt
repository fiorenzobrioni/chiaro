package com.callbackdev.chiaro.domain

import com.callbackdev.chiaro.domain.sky.BrightStars
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The star catalogue is found from a class in ANOTHER package (the night crash of 20 ott
 * 2026): the minified build's R8 repackages `BrightStars`, so a resource name resolved against
 * its package is resolved against a package that no longer exists. This test stands one
 * package up, where the relative name fails exactly as it failed on the phone, and asks for
 * the catalogue the way [BrightStars] must: by its absolute path.
 */
class StarCatalogueResourceTest {

    @Test
    fun `the catalogue is found by its absolute path from any package`() {
        assertTrue("the path must be absolute, R8 moves the class", BrightStars.RESOURCE.startsWith("/"))
        assertNotNull(javaClass.getResourceAsStream(BrightStars.RESOURCE))
    }

    @Test
    fun `the relative name is the trap, and it fails from here`() {
        assertNull(javaClass.getResourceAsStream("bright_stars.bin"))
    }
}
