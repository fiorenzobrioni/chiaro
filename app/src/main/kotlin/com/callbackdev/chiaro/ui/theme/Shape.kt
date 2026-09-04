package com.callbackdev.chiaro.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** DESIGN.md §6. Chips, buttons and the FAB use `CircleShape` at the call site: a
 * fully-rounded shape is not a corner size, so it has no slot here. */
val ChiaroShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/**
 * The rhythm of a scrolled list of sections (DESIGN.md §6). Written down here because
 * it grew three different values across five screens before anyone compared them: a
 * section header sat 20dp under the block above it on four screens and 12 on Today,
 * where the list's own 12dp gap made up the difference (device review, 4 set).
 *
 * The numbers are what a header COSTS, not what the eye sees: a row's own 8dp of
 * vertical padding is added on top, which is how [SectionBottom]'s 4 becomes the 12dp
 * gap the document asks for, and [SectionTop]'s 16 becomes 24.
 */
val SectionTop = 16.dp

/** A group inside a section sits closer to it than the section does to its neighbour. */
val GroupTop = 12.dp

val SectionBottom = 4.dp
