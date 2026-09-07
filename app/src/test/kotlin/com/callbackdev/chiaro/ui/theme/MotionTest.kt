package com.callbackdev.chiaro.ui.theme

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DESIGN.md §7 says reduced motion collapses every spec to a 100 ms fade. The constant
 * for it existed from Fase 1 and nothing consulted it until Fase 9, which is the kind of
 * rule that is true in a document and false in an APK. This is the part CI can hold: the
 * specs themselves.
 */
class MotionTest {

    @Test
    fun `the document's springs are the springs`() {
        val spatial = ChiaroMotion.spatial<Float>() as SpringSpec
        assertEquals(0.8f, spatial.dampingRatio, 1e-6f)
        assertEquals(380f, spatial.stiffness, 1e-6f)

        val fast = ChiaroMotion.spatialFast<Float>() as SpringSpec
        assertEquals(0.9f, fast.dampingRatio, 1e-6f)
        assertEquals(800f, fast.stiffness, 1e-6f)

        val effects = ChiaroMotion.effects<Float>() as SpringSpec
        assertEquals(1f, effects.dampingRatio, 1e-6f)
        assertEquals(1600f, effects.stiffness, 1e-6f)
    }

    @Test
    fun `every spec collapses to the same fade when motion is reduced`() {
        listOf(
            ChiaroMotion.spatial<Float>(reduced = true),
            ChiaroMotion.spatialFast<Float>(reduced = true),
            ChiaroMotion.effects<Float>(reduced = true)
        ).forEach { spec ->
            val tween = spec as TweenSpec
            assertEquals(ChiaroMotion.reducedMotionFadeMillis, tween.durationMillis)
            assertEquals("a fade does not wait to start", 0, tween.delay)
        }
    }

    @Test
    fun `the fade is the hundred milliseconds the document promises`() {
        assertEquals(100, ChiaroMotion.reducedMotionFadeMillis)
    }

    @Test
    fun `an opening section keeps its fade and loses its measure`() {
        // Not an equality check — EnterTransition has no public shape — but the two must
        // differ, or "reduced" is a parameter nobody reads.
        assertTrue(
            "reduced motion must change the transition",
            ChiaroMotion.enter(reduced = true) != ChiaroMotion.enter(reduced = false)
        )
        assertTrue(
            "reduced motion must change the exit too",
            ChiaroMotion.exit(reduced = true) != ChiaroMotion.exit(reduced = false)
        )
    }
}
