package com.callbackdev.chiaro.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

/**
 * DESIGN.md §10: at 200% type, layouts **wrap and reflow, they do not clip or ellipsize
 * a value**.
 *
 * The app never clips — there is not one `maxLines` in `ui/`, which is deliberate — but
 * "does not clip" was doing all the work, and it is only half the rule. Four components
 * put text in a column measured in dp: the week row's day, rain and two temperatures,
 * the hour cell, the timeline's clock, the drift strip's date. A dp is a fixed distance
 * and an sp is not, so at 200% every one of those columns held text twice as wide as
 * itself. Nothing was cut off — it wrapped, mid-value, into a second line the row had no
 * height for, and the week stopped being a column of anything.
 *
 * Two rules, both measured against a 360dp screen:
 *
 * 1. **A column that holds text is measured in text.** [forText] grows it with the
 *    reader's scale, so «Mer» and «-10°» keep the one line they were budgeted.
 * 2. **Past [ReflowTextScale] a row of columns stops being a row.** Growing them all is
 *    only affordable while there is something to grow into; beyond 150% the week row's
 *    four columns and its bar want more than a phone is wide, and the honest answer is
 *    two lines, not a thinner bar. That threshold is where the numbers stop fitting, not
 *    a round number: 44+36+34+34 dp of columns at 1.5 is 222dp, which with the icon and
 *    the gaps leaves the range bar 40dp — under the 48dp that makes it a bar rather than
 *    a smudge.
 *
 * The ceiling on growth is 2.0 because the system's own slider stops at 200%; past that
 * the reflow is what carries the layout, not the multiplier.
 */
const val ReflowTextScale = 1.5f

/** The largest multiplier a fixed column is grown by. */
const val TextColumnCeiling = 2.0f

/** Pure, so the two rules above are testable without a device. */
internal fun textColumnScale(fontScale: Float): Float = fontScale.coerceIn(1f, TextColumnCeiling)

internal fun reflowsForText(fontScale: Float): Boolean = fontScale >= ReflowTextScale

/** A fixed column that holds text, in the reader's text. */
@Composable
@ReadOnlyComposable
fun Dp.forText(): Dp = this * textColumnScale(LocalDensity.current.fontScale)

/** Whether a row of columns has to become two rows to keep its values whole. */
@Composable
@ReadOnlyComposable
fun reflowForText(): Boolean = reflowsForText(LocalDensity.current.fontScale)
