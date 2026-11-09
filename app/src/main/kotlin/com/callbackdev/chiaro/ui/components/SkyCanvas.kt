package com.callbackdev.chiaro.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import com.callbackdev.chiaro.ui.theme.SkyGradient
import com.callbackdev.chiaro.ui.theme.SkyPalette

/** Where the top scrim band fades out, as a fraction of the canvas height. Today's
 * screen reads it to know how far white status-bar icons stay backed by scrim before
 * the bar must return to theme ink. */
const val SkyCanvasTopScrimEnd = 0.25f

/**
 * DESIGN.md §3 and §8.1. The gradient is the sky above the active city, computed by
 * [SkyPalette]; this composable paints it and guarantees the scrim — on BOTH text
 * bands since Fase 3: the bottom one under the temperature and the sentence, and a
 * symmetric top one under the place row and the status bar icons, now that the canvas
 * reaches the top edge of the screen.
 *
 * The scrims are not decoration and not optional: the canvas is the one surface in the
 * app that does not follow the reader's theme, so white text over an unscrimmed noon
 * sky would be about 1.3:1. `ScrimContractTest` pins the alpha at the value that clears
 * 4.5:1 for every altitude the palette can produce, which is why it is a constant here
 * and not a parameter — one constant, both bands.
 *
 * The bottom edge is STRAIGHT (committente, 4 set; reconsidered and kept, 8 set): the
 * canvas carried a 28dp round on its two bottom corners until then, which read as a
 * card floating over the scroll rather than as the sky the screen opens on. The sky has
 * no corners, so neither does the block that draws it. What meets it since 23 set 2026 is
 * the page's own [SkySheet], rounded and laid over its last few dp — the page is the card
 * now, and the sky stays the ground under it.
 *
 * [minHeight] is a floor, not a size (8 set 2026): the canvas holds text measured in sp
 * — the 64sp temperature, a 22sp sentence that runs to two lines in Italian — inside a
 * height that used to be fixed in dp, and at 100% type a two-line sentence left 2dp of
 * room before the hero climbed into the place row; at 115% they overlapped by 30dp. The
 * block is now at least this tall and grows with what it holds, and the scrim bands
 * scale with it because they are fractions.
 */
@Composable
fun SkyCanvas(
    gradient: SkyGradient,
    modifier: Modifier = Modifier,
    minHeight: Dp = 280.dp,
    sheet: SkySheet? = null,
    /** The scene the shader draws over the gradient (§3.9), or null for the gradient alone. */
    scene: SkyScene? = null,
    /** Whether the scene may move now (§3.9, §7.1). */
    moving: Boolean = false,
    /** Where the sun and the moon stand inside the canvas (§8.1c), when the caller knows. */
    band: SkyBand? = null,
    /** The sky gliding from the one it was to [gradient] and [scene] (9 nov 2026), when the
     * caller keeps one; the canvas then draws what it shows, sky, scrim and sheet alike. */
    transition: SkyTransition? = null,
    /** Where the text stands on the canvas (§3.6, 9 nov 2026), when the caller reports it:
     * the scrim is then full behind the text and nowhere else. Null draws the two bands. */
    guard: SkyGuard? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
    ) {
        // The sky first, then the scrims and the sheet over it, then what stands on it: the
        // scene's clouds are under the scrim like the gradient, so §3.6 holds over them too.
        val shown: (() -> Sky)? = transition?.let { t -> { t.shown() } }
        if (scene != null) {
            SkyShaderLayer(
                gradient = gradient, scene = scene, moving = moving, modifier = Modifier.matchParentSize(),
                band = band, shown = shown
            )
        }
        Spacer(
            modifier = Modifier
                .matchParentSize()
                .drawWithCache {
                    val lip = sheet?.lip?.toPx() ?: 0f
                    // The bottom band reaches its full scrim where the text ends, not at the
                    // bottom edge: with a sheet over the canvas the last [SkySheet.lip] of
                    // sky is under the page, and the band must not lose any of its alpha to
                    // it (§3.6 is measured at full strength).
                    val full = ((size.height - lip) / size.height).coerceIn(0.46f, 1f)
                    // The scrim's shape, once per size and per place of the text: how much of
                    // its strength each point of the canvas wears ([scrimShare]), as a small
                    // picture stretched over the canvas and tinted when drawn.
                    val mask = scrimMask(size.width, size.height, full, guard, ScrimFeather.toPx(), ScrimRamp.toPx())
                    onDrawBehind {
                        val sky = shown?.invoke()
                        val now = sky?.first ?: gradient
                        if (scene == null) drawRect(Brush.verticalGradient(now.stops()))
                        // As strong as this sky needs it ([scrimNeed]): all of it by day, none
                        // of it under a night that white already stands on.
                        val strength = SkyPalette.ScrimAlpha * scrimNeed(now, sky?.second ?: scene)
                        if (strength > 0f) {
                            drawImage(
                                image = mask,
                                dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                                colorFilter = ColorFilter.tint(SkyPalette.scrimFor(now).copy(alpha = strength)),
                                filterQuality = FilterQuality.Medium
                            )
                        }
                        if (sheet != null) drawSheet(sheet, now)
                    }
                }
        )
        Box(modifier = Modifier.padding(bottom = sheet?.lip ?: 0.dp), content = content)
    }
}

/**
 * The page's edge where it meets the sky (design review, 23 set 2026).
 *
 * Until then the canvas ended on a straight cut from its darkest band — the bottom scrim
 * at full strength — straight into the near-white page: the two most distant colors on
 * the screen, touching along a ruler line. The page now starts as a **sheet laid on the
 * sky**: its top [lip] overlaps the canvas with [Corner] rounded corners, so the sky shows
 * round them and the darkest strip of scrim is under the paper. This is not the rounded
 * canvas removed on 4 set — that made the sky a card floating over the page; here the sky
 * is still the ground and the page is what lies on it.
 *
 * And the sheet **catches the light** of the sky it lies on: its first [glowSpan] are
 * tinted with the canvas' own bottom stop at [GlowStrength], fading into [surface] — warm
 * at sunset, blue at noon, barely there at night. The glow reaches past the canvas' own
 * bounds on purpose (a lazy item is not clipped to itself): the items after it are
 * transparent and stand on it. It fades out well inside the height of the pinned place
 * row, so when the canvas item leaves the list the part that goes with it is already
 * hidden under that row's surface.
 */
@Immutable
data class SkySheet(
    val surface: Color,
    val lip: Dp = 24.dp,
    val glowSpan: Dp = 120.dp,
    /** Where the paper starts: past a navigation rail on a wide window (§8.12c), so its
     * rounded corner shows beside the rail rather than under it. */
    val start: Dp = 0.dp
)

private fun DrawScope.drawSheet(sheet: SkySheet, gradient: SkyGradient) {
    val lip = sheet.lip.toPx()
    val top = size.height - lip
    val glow = lerp(sheet.surface, gradient.bottom, GlowStrength)
    val span = sheet.glowSpan.toPx()
    // Eased rather than linear: a linear fade ends on a visible line where it meets
    // the plain surface.
    val brush = Brush.verticalGradient(
        0.0f to glow,
        0.35f to lerp(sheet.surface, glow, 0.55f),
        0.7f to lerp(sheet.surface, glow, 0.15f),
        1.0f to sheet.surface,
        startY = top,
        endY = top + span
    )
    val corner = CornerRadius(Corner.toPx())
    val left = sheet.start.toPx()
    drawPath(
        Path().apply {
            addRoundRect(
                RoundRect(
                    left = left, top = top, right = size.width, bottom = size.height,
                    topLeftCornerRadius = corner, topRightCornerRadius = corner
                )
            )
        },
        brush = brush
    )
    // Below the canvas: the rest of the glow, over the list's own surface.
    drawRect(brush = brush, topLeft = Offset(left, size.height), size = Size(size.width - left, span - lip))
}

private val Corner = 28.dp

/**
 * Where the text stands on the canvas, in the canvas' own px (DESIGN §3.6, 9 nov 2026): the
 * box of the «now» ([now]: the figure, the sky's word and the lines under it, which keep to
 * the start of the canvas) and the row where the lines that run the whole width begin
 * ([proseTop]: the day's sentence, the ribbon and its clock times).
 *
 * Until then the bottom scrim was a band across the whole canvas from 45% of its height, at
 * full strength only where the text ended: more than half of the sky stood under a veil for
 * the sake of a figure that takes a third of the width, and the figure itself sat on the
 * band's ramp, at 2.30:1 on a clear noon (§3.6, the halo's measurement). The scrim now goes
 * where the text is: full behind the box and from the prose down, fading over [ScrimFeather]
 * round the box and over [ScrimRamp] above the prose, and the sky beside the figure is sky.
 */
@Immutable
data class SkyGuard(val now: Rect, val proseTop: Float)

/** How far round the «now» box the scrim fades to nothing: this far above and below it, and
 * [ScrimFeatherAcross] times as far beside it, where the sky is, so that the patch ends as a
 * long shade and never as the side of a panel. */
internal val ScrimFeather = 72.dp
internal const val ScrimFeatherAcross = 1.8f

/**
 * How much of the scrim this sky needs, 0..1 (DESIGN §3.6, 9 nov 2026; pure,
 * `ScrimContractTest`). The scrim is there so that white clears 4.5:1 over the brightest
 * thing the canvas draws under the text, and it was drawn at one strength over every sky: a
 * veil of 55% over a night that white already stands on at 12:1, where it only put a dark
 * panel into the dark. The need is read on the brightest of what this sky draws outside the
 * bodies' band: its three stops, a third more where the low sun lights the horizon, the
 * clouds' lit colour when there is a deck or a fall to wear it (six tenths of it over the sky
 * for a veil alone, as the shader mixes it), the fog's when there is fog. Under [NeedNone] of luminance white clears 7:1 with no scrim and there is none; from
 * [NeedFull] up it is all there, as it always was by day; between the two it climbs. A
 * thunderstorm has all of it whatever the hour: a flash may light the deck up to the cap.
 */
internal fun scrimNeed(gradient: SkyGradient, scene: SkyScene?): Float {
    fun lum(c: Color): Float {
        fun lin(v: Float) = if (v <= 0.04045f) v / 12.92f else Math.pow(((v + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
        return 0.2126f * lin(c.red) + 0.7152f * lin(c.green) + 0.0722f * lin(c.blue)
    }
    if (scene != null && scene.storm > 0f) return 1f
    var brightest = gradient.stops().maxOf { lum(it) }
    if (scene != null) {
        if (scene.lowSun > 0f) brightest *= 1f + 0.35f * scene.lowSun
        // A deck and what falls wear the lit colour whole; a veil alone is at most six tenths
        // of it over the sky (the shader's own mix), so a wisp of cirrus under a full moon
        // does not call for a scrim the night does not need.
        val lit = lum(scene.lit)
        if (scene.deck > 0.01f || scene.rain > 0f || scene.snow > 0f) {
            brightest = maxOf(brightest, lit)
        } else if (scene.veil > 0.01f) {
            brightest = maxOf(brightest, 0.4f * brightest + 0.6f * lit)
        }
        if (scene.fog > 0f) brightest = maxOf(brightest, lum(scene.fogColor))
    }
    val t = ((brightest - NeedNone) / (NeedFull - NeedNone)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/** The luminance under which a sky needs no scrim, and the one from which it needs all of it. */
internal const val NeedNone = 0.06f
internal const val NeedFull = 0.18f

/** How far above the prose the scrim climbs to nothing. */
internal val ScrimRamp = 40.dp

/** Where the top band fades out and where the bottom one starts with no [SkyGuard], as
 * fractions of the canvas' height. */
private const val BottomScrimStart = 0.45f

/**
 * How much of the scrim's strength the point ([x], [y]) of a canvas [width] by [height] wears,
 * 0..1 (pure, `ScrimContractTest`). The top band always: full at the top edge, nothing at
 * [SkyCanvasTopScrimEnd]. Then, with a [guard], whichever is more of the prose's band (full
 * from [SkyGuard.proseTop] down, climbing to nothing [ramp] above it) and the box's patch
 * (full inside [SkyGuard.now], fading to nothing [feather] away from it); without one, the
 * band the canvas had, from 45% of the height to full at [full].
 */
internal fun scrimShare(
    x: Float, y: Float, width: Float, height: Float, full: Float, guard: SkyGuard?, feather: Float, ramp: Float
): Float {
    fun ease(t: Float): Float = t.coerceIn(0f, 1f).let { it * it * (3f - 2f * it) }
    val top = (1f - y / (height * SkyCanvasTopScrimEnd)).coerceIn(0f, 1f)
    if (guard == null) {
        val bottom = ((y / height - BottomScrimStart) / (full - BottomScrimStart)).coerceIn(0f, 1f)
        return maxOf(top, bottom)
    }
    val band = ease((y - (guard.proseTop - ramp)) / ramp)
    val dx = maxOf(guard.now.left - x, 0f, x - guard.now.right) / ScrimFeatherAcross
    val dy = maxOf(guard.now.top - y, 0f, y - guard.now.bottom)
    val patch = 1f - ease(kotlin.math.hypot(dx, dy) / feather)
    return maxOf(top, band, patch).coerceIn(0f, 1f)
}

/** The scrim's shape as a picture: [scrimShare] in the alpha of [MaskWidth] columns. */
private fun scrimMask(width: Float, height: Float, full: Float, guard: SkyGuard?, feather: Float, ramp: Float): ImageBitmap {
    val w = MaskWidth
    val h = (MaskWidth * height / width.coerceAtLeast(1f)).toInt().coerceIn(8, 4 * MaskWidth)
    val pixels = IntArray(w * h)
    for (j in 0 until h) {
        val y = (j + 0.5f) / h * height
        for (i in 0 until w) {
            val x = (i + 0.5f) / w * width
            val a = (scrimShare(x, y, width, height, full, guard, feather, ramp) * 255f + 0.5f).toInt().coerceIn(0, 255)
            pixels[j * w + i] = a shl 24
        }
    }
    return android.graphics.Bitmap.createBitmap(pixels, w, h, android.graphics.Bitmap.Config.ARGB_8888).asImageBitmap()
}

/** The mask's width in pixels: every shape in it is a ramp tens of dp long, and 96 columns
 * stretched with a filter draw it as smoothly as the brush drew the bands. */
private const val MaskWidth = 96

/** How much of the sky's bottom stop the sheet's first pixel carries. */
private const val GlowStrength = 0.16f

@Preview(showBackground = true, heightDp = 440)
@Composable
private fun SkyCanvasPreview() {
    ChiaroTheme(dynamicColor = false) {
        androidx.compose.foundation.layout.Column {
            listOf(50.0 to "mezzogiorno", 3.0 to "ora d'oro", -4.0 to "ora blu", -30.0 to "notte")
                .forEach { (altitude, label) ->
                    SkyCanvas(gradient = SkyPalette.Paper.gradient(altitude), minHeight = 100.dp) {
                        androidx.compose.material3.Text(
                            text = label,
                            color = Color.White,
                            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
                        )
                    }
                }
        }
    }
}
