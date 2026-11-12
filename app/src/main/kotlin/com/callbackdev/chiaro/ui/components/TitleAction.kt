package com.callbackdev.chiaro.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A section's own action, at the end of its title: «+ Aggiungi» on the Sky's moments (Fase
 * 16.2, 10 nov 2026) and on Alerts' «I tuoi» (the Alerts review, 12 nov 2026). One word and a
 * plus, the same on the two tabs, because it is the same gesture in the same place: adding to
 * the list the title names. [said] is the whole of it for a screen reader, which hears the
 * button without the title beside it («Aggiungi un momento», «Nuovo avviso»).
 *
 * One composable where there would have been two copies: the Sky drew it first, as a private
 * function of its screen.
 */
@Composable
fun TitleAction(label: String, said: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onClick,
        contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
        modifier = modifier.semantics { contentDescription = said }
    ) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize)
        )
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text(label)
    }
}

/**
 * Lays its content out whole and reports [line] for its height, centred on it.
 *
 * **A title with an action stands where a title without one does.** The button's box is 48dp
 * and the title's line 24: laid out as it measures, it would push the title 12dp down and the
 * list 12dp further, and on a wide page two panes' first titles, one with the button and one
 * without, would stand on two lines. So the button is placed on the title's line and counts
 * for the line's height only: its box hangs 12dp over the air above and below, which is air,
 * and the finger still finds all 48.
 */
fun Modifier.onTheLine(line: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minHeight = 0))
    val height = line.roundToPx().coerceAtMost(placeable.height)
    layout(placeable.width, height) { placeable.place(0, (height - placeable.height) / 2) }
}

/** `titleMedium`'s line, which a title's action may not make taller. Not scaled with the
 * reader's type: at a larger one the line is taller than this and the button hangs less. */
val TitleLine: Dp = 24.dp

/** A text button's own 16dp of padding after its word, and 4: the word ends 20dp from the
 * edge, a little inside the segments' own. */
val TitleActionEnd: Dp = 4.dp
