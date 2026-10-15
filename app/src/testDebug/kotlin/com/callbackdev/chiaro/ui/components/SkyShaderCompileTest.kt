package com.callbackdev.chiaro.ui.components

import android.graphics.RuntimeShader
import com.callbackdev.chiaro.ui.theme.SkyPalette
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * The sky's AGSL compiles, and every uniform the screen sets exists in it (DESIGN §3.9): a
 * `RuntimeShader` compiles its source in its constructor and refuses a uniform it does not
 * declare, so a slip in the shader fails here and not on the reader's phone. In `testDebug`
 * with native graphics, where Robolectric carries the real shader compiler.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SkyShaderCompileTest {

    @Test
    fun `the sky's shader compiles and takes every uniform`() {
        val palette = SkyPalette.Paper
        val gradient = palette.gradient(10.0, 60, 70)
        val scene = skyScene(
            palette, gradient, 10.0, 200.0, 20.0, 120.0, 0.6, false, 60,
            null, 70, SkyFall.RAIN, 1.5, 20.0, 250
        )
        RuntimeShader(SkyShaderSource).bindSky(400f, 600f, 2.5f, 12f, gradient, scene)
    }
}
