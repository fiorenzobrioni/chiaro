package com.callbackdev.chiaro.data.warnings

import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.domain.model.Coordinates
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The one thing the domain test cannot prove: that the asset ships in the library and
 * opens through the Android asset manager. The geometry is `WarningZoneIndexTest`'s.
 */
@RunWith(RobolectricTestRunner::class)
class WarningZoneAssetsTest {

    @Test
    fun `the bundled index opens and answers`() {
        val index = WarningZoneAssets.load(ApplicationProvider.getApplicationContext())
        assertEquals(187, index.size)
        assertEquals("20260908_1519", index.bulletinStamp)
        assertEquals("Lomb-09", index.locate(Coordinates(45.4643, 9.1895), "Comune di Milano")?.code)
    }
}
