package com.callbackdev.chiaro.ui.sky

import com.callbackdev.chiaro.domain.sky.SkyJobCatalog
import com.callbackdev.chiaro.domain.sky.SkyNotScheduled
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * The vocabulary has to be TOTAL over the catalog, and a test is the only thing that
 * can say so: `SkyText` answers with `error(...)` for an id it does not know, so a job
 * shipped without words is not a missing label, it is a crash on the screen that
 * happens to be rare — the catalog row only has to be tapped.
 *
 * Written when Fase 19 added nineteen jobs at once, which is exactly the edit where
 * one gets forgotten.
 */
class SkyTextTest {

    @Test
    fun `every job in the catalog has a name and an explanation`() {
        SkyJobCatalog.all.forEach { job ->
            assertNotEquals("no name for ${job.id}", 0, SkyText.nameRes(job.id))
            assertNotEquals("no explanation for ${job.id}", 0, SkyText.explanationRes(job.id))
        }
    }

    @Test
    fun `every reason the sky skips a job has words`() {
        SkyNotScheduled.entries.forEach { reason ->
            assertNotEquals("no words for $reason", 0, SkyText.notScheduledRes(reason))
        }
    }

    /**
     * The bearing sentence is prose ("look east"), so the eight points have to be
     * eight strings and the arithmetic has to put north where north is: 0° and 359°
     * are both north, and 90° is east.
     */
    @Test
    fun `a bearing lands on the compass point it belongs to`() {
        val north = SkyText.bearingRes(0.0)
        assertNotEquals(0, north)
        assertNotEquals(north, SkyText.bearingRes(90.0))
        assertNotEquals(north, SkyText.bearingRes(180.0))
        assertNotEquals(north, SkyText.bearingRes(270.0))
        // The wrap-around: 359° is still north, and so is 361°.
        org.junit.Assert.assertEquals(north, SkyText.bearingRes(359.0))
        org.junit.Assert.assertEquals(north, SkyText.bearingRes(361.0))
        org.junit.Assert.assertEquals(SkyText.bearingRes(90.0), SkyText.bearingRes(-270.0))
    }
}
