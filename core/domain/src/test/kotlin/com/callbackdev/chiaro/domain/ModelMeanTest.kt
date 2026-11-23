package com.callbackdev.chiaro.domain

import com.callbackdev.chiaro.domain.ModelMean.Sky
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/** The sky of three models (Fase 18.3, `docs/STATES.md` «The sky of three models»). */
class ModelMeanTest {

    @Test
    fun `the sky is the mean of every number, at equal weight`() {
        // ICON's overcast low deck against the two globals' broken sky.
        assertEquals(
            Sky(coverPct = 70, lowPct = 57, midPct = 20, highPct = 10),
            ModelMean.sky(Sky(100, 100, 30, 0), listOf(Sky(50, 30, 20, 10), Sky(60, 40, 10, 20)))
        )
    }

    @Test
    fun `each number is rounded to the percent`() {
        assertEquals(Sky(34, 1, 0, 67), ModelMean.sky(Sky(0, 0, 0, 100), listOf(Sky(1, 2, 1, 50), Sky(100, 1, 0, 50))))
    }

    @Test
    fun `without witnesses the sky is the hour's own model's`() {
        val own = Sky(80, 10, 20, 90)
        assertSame(own, ModelMean.sky(own, emptyList()))
    }

    @Test
    fun `without the hour's own model there is no mean`() {
        assertNull(ModelMean.sky(null, listOf(Sky(50, 30, 20, 10))))
    }

    @Test
    fun `a model counts only with all four numbers`() {
        assertNull(Sky.of(50, null, 20, 10))
        assertNull(Sky.of(null, 0, 0, 0))
        assertEquals(Sky(50, 30, 20, 10), Sky.of(50, 30, 20, 10))
    }
}
