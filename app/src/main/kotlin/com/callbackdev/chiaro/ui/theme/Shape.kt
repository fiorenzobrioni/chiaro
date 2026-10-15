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
 * The numbers are what a header COSTS, not what the eye sees: the neighbour adds its
 * own. Beside one of the app's own rows (8dp of vertical padding) [SectionTop]'s 16
 * becomes the 24dp the document asks for and [SectionBottom]'s 4 becomes 12. Beside a
 * Material `ListItem` — the Sky moments, the Alerts switches, the Settings rows — the
 * gap is Material's own and comes out a little larger, which is left alone on purpose:
 * a list built out of `ListItem` reads right because it follows the platform, and
 * trimming a component to hit a number in a document is the design system arguing with
 * Material over something nobody can see.
 */
val SectionTop = 16.dp

/** A group inside a section sits closer to it than the section does to its neighbour. */
val GroupTop = 12.dp

val SectionBottom = 4.dp

/**
 * The app's two card corners beyond Material's scale (DESIGN.md §6, review of Today,
 * 15 ott 2026). They grew up as private constants of the screens that drew them first —
 * the Sky's 28 for its tonight and next-moment cards, the segments' 20 and 4 — while
 * Today kept Material's 12 for its tiles and banners, so two tabs and the third looked
 * cut from different paper. Written down here so every screen asks for the same three.
 *
 * [CardCorner]: a card that stands alone and carries a screen's answer or a notice (the
 * tonight card, the next moment, Alerts' answer, the guide's and the notifications' cards).
 * It is Material's `extraLarge`, named for what it is used for.
 */
val CardCorner = 28.dp

/**
 * [GroupCorner]: the outer corner of a group's segments, and the corner of a smaller card
 * that a 28 would turn into a pill — a metric tile, a banner of one statement, an open day
 * of the week. [GroupInnerCorner]: where two segments of a group meet.
 */
val GroupCorner = 20.dp

val GroupInnerCorner = 4.dp
