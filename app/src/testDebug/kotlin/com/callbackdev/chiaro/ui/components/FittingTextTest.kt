package com.callbackdev.chiaro.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The place's day and hour on Today's bar (committente, 15 ott 2026): the longest form that
 * fits the line, measured. The widths are the ones the bar gives it — about 190dp beside the
 * bar's temperature on a 360dp phone, about 270 at rest — and the strings the measured worst
 * cases: «Wednesday 30 September · 12:15 PM» is 237dp in the app's face.
 */
@RunWith(RobolectricTestRunner::class)
// Real text measurement: Robolectric's default graphics measure a line as a few pixels
// whatever it says, which would make every candidate fit.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w384dp-h832dp-xhdpi")
class FittingTextTest {

    @get:Rule
    val compose = createComposeRule()

    private val long = "Wednesday 30 September · 12:15 PM"
    private val medium = "Wed 30 Sep · 12:15 PM"
    private val short = "Wed · 12:15 PM"

    private fun show(width: Dp) {
        compose.setContent {
            ChiaroTheme(dynamicColor = false) {
                Box(Modifier.width(width)) {
                    FittingText(
                        candidates = listOf(long, medium, short),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.Unspecified
                    )
                }
            }
        }
    }

    private fun shows(text: String) {
        listOf(long, medium, short).forEach {
            compose.onAllNodesWithText(it).assertCountEquals(if (it == text) 1 else 0)
        }
    }

    @Test
    fun `with the room, the long form`() {
        show(272.dp)
        shows(long)
    }

    @Test
    fun `beside the bar's temperature on a narrow phone, the medium one`() {
        show(190.dp)
        shows(medium)
    }

    @Test
    fun `where not even that fits, the short one`() {
        show(120.dp)
        shows(short)
    }

    @Test
    fun `where nothing fits, the last, which wraps rather than vanish`() {
        show(40.dp)
        shows(short)
    }
}
