package com.callbackdev.chiaro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.ui.theme.SkyPalette
import kotlin.math.abs

/**
 * Where the sun and the moon are, for [SkyBodiesLayer]: altitude and compass bearing from
 * the same `AstronomyEngine` the canvas' gradient reads, so the disc and the color behind
 * it cannot disagree about whether the sun is up.
 */
@Immutable
data class SkyBodies(
    val sunAltitudeDeg: Double,
    val sunAzimuthDeg: Double,
    val moonAltitudeDeg: Double,
    val moonAzimuthDeg: Double,
    /** 0 at new moon, 1 at full. */
    val moonIllumination: Double,
    /** Moon − sun longitude, `[0, 360)`: under 180 the moon is waxing. */
    val moonElongationDeg: Double,
    val cloudPct: Int,
    /** South of the equator the reader faces north to find the sun: east is on the right,
     * and the moon's lit limb is mirrored. */
    val southern: Boolean
)

/**
 * The sun and the moon, drawn where they stand (design review, 23 set 2026).
 *
 * The canvas has always been the computed sky; until now it painted the light and left the
 * two bodies that make it out. The layer fills the free band between the place row and the
 * hero and puts each body in it as the reader would see it facing the equator: **across**
 * by compass bearing (east on the left in the north, on the right in the south — a winter
 * sun stays near the middle, a summer one rises and sets near the edges, which is true),
 * **up** by altitude. Below the horizon a body is not drawn. Clouds veil the sun rather than
 * hide it, and under a full cover it loses its **edge**, not only its strength — see
 * [sunVeil]: an overcast sun is a brighter part of the sky, never a disc.
 *
 * The moon wears its phase — the lit limb on the sun's side, the terminator an ellipse —
 * and the rest of its disc is barely there, as earthshine.
 *
 * It draws inside its own box and never behind text: the band it gets is the space the
 * column leaves between the row and the hero, and when that space is too small for a disc
 * (a two-line sentence at a large font size) nothing is drawn — a sun squeezed behind the
 * temperature would be a sun that costs the number its contrast. Static: it moves as the
 * page's minute tick moves it, and costs one draw.
 */
@Composable
fun SkyBodiesLayer(bodies: SkyBodies, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val radius = BodyRadius.toPx()
        if (size.height < radius * 2 + 4.dp.toPx() || size.width <= 0f) return@Canvas
        val cloud = (bodies.cloudPct.coerceIn(0, 100) / 100f)
        if (bodies.moonAltitudeDeg > 0.0) {
            val center = place(bodies.moonAzimuthDeg, bodies.moonAltitudeDeg, bodies.southern, radius)
            // By day the moon is the pale thing it is; at night it is the brightest mark.
            val day = bodies.sunAltitudeDeg > 0.0
            moon(center, radius * 0.9f, bodies, alpha = (if (day) 0.55f else 0.95f) * (1f - 0.8f * cloud))
        }
        if (bodies.sunAltitudeDeg > -0.5) {
            val center = place(bodies.sunAzimuthDeg, bodies.sunAltitudeDeg, bodies.southern, radius)
            sun(center, radius, bodies.sunAltitudeDeg, cloud)
        }
    }
}

private fun DrawScope.place(azimuthDeg: Double, altitudeDeg: Double, southern: Boolean, radius: Float): Offset {
    val inset = size.width * EdgeInset
    val x = inset + (size.width - inset * 2) * bodyAcross(azimuthDeg, southern)
    val y = radius + (size.height - radius * 2) * (1f - bodyUp(altitudeDeg))
    return Offset(x, y)
}

private fun DrawScope.sun(center: Offset, radius: Float, altitudeDeg: Double, cloud: Float) {
    // Warm near the horizon, white-gold above the golden hour.
    val warmth = (1.0 - (altitudeDeg / 12.0)).coerceIn(0.0, 1.0).toFloat()
    val core = lerp(SkyPalette.SunHigh, SkyPalette.SunLow, warmth)
    val veil = sunVeil(cloud)
    val glow = glowRadius(center, radius * 4f * veil.spread)
    drawCircle(
        brush = Brush.radialGradient(
            0f to core.copy(alpha = veil.glowCore),
            0.35f to core.copy(alpha = veil.glowMid),
            1f to Color.Transparent,
            center = center,
            radius = glow
        ),
        radius = glow,
        center = center
    )
    if (veil.disc > 0f) drawCircle(core.copy(alpha = veil.disc), radius = radius, center = center)
}

/** How the cloud draws the sun: the disc's opacity, the glow's at its core and at a third
 * of its reach, and how far the glow spreads (× the clear-sky reach). */
internal data class SunVeil(val disc: Float, val glowCore: Float, val glowMid: Float, val spread: Float)

/**
 * The sun under [cloud] (0 clear, 1 full cover). Until 24 set 2026 the cloud only faded
 * everything — disc and glow by the same 70% — so an overcast sun at noon was a crisp
 * grey-white disc at 30% with almost no glow: on a device, over a grey sky, **it read as
 * the moon** (committente, from a screenshot at 15:50 with the moon three hours from
 * rising). A sun behind a full cover has no edge; the cloud spreads its light. So the
 * disc now fades with the cloud to nothing, and the glow **keeps** most of its strength
 * and spreads a third wider: at full cover the sun is a bright, soft patch of sky, at
 * 80% a faint disc in a wide glow, at half a disc with its halo, in a clear sky the same
 * sun as before. The moon, always a crisp disc with its phase, can no longer be taken
 * for it. Simulated on the screenshot's own sky before it was written.
 */
internal fun sunVeil(cloud: Float): SunVeil {
    val c = cloud.coerceIn(0f, 1f)
    return SunVeil(
        disc = 1f - c,
        glowCore = 0.55f - 0.15f * c,
        glowMid = 0.18f + 0.10f * c,
        spread = 1f + 0.3f * c
    )
}

private fun DrawScope.moon(center: Offset, radius: Float, bodies: SkyBodies, alpha: Float) {
    val face = SkyPalette.MoonFace
    val glow = glowRadius(center, radius * 3f)
    drawCircle(
        brush = Brush.radialGradient(
            0f to face.copy(alpha = 0.22f * alpha * bodies.moonIllumination.toFloat()),
            1f to Color.Transparent,
            center = center,
            radius = glow
        ),
        radius = glow,
        center = center
    )
    // Earthshine: the unlit disc, just there.
    drawCircle(face.copy(alpha = 0.14f * alpha), radius = radius, center = center)
    val lit = moonLitPath(center, radius, bodies.moonIllumination.toFloat(), litOnRight(bodies))
    drawPath(lit, face.copy(alpha = alpha))
}

/** The glow may reach a little past the band — its tail there is a few percent of white,
 * which the scrim over the text bands absorbs — but never far: a halo that washed over
 * the temperature would cost the number its contrast. */
private fun DrawScope.glowRadius(center: Offset, wanted: Float): Float {
    val room = minOf(center.y, size.height - center.y) + GlowSpill.toPx()
    return minOf(wanted, room).coerceAtLeast(1f)
}

private val GlowSpill = 14.dp

/** Waxing, the lit limb faces the evening sun: west, which is on the right for a reader
 * in the north and on the left in the south. */
internal fun litOnRight(bodies: SkyBodies): Boolean {
    val waxing = bodies.moonElongationDeg < 180.0
    return waxing != bodies.southern
}

/**
 * The lit part of a moon at [illumination] (0 new, 1 full): a half disc on the lit side,
 * closed by the terminator — an ellipse whose half-width is `r·|2k − 1|`, bulging toward
 * the lit side for a crescent and away from it for a gibbous moon.
 */
internal fun moonLitPath(center: Offset, radius: Float, illumination: Float, litRight: Boolean): Path {
    val k = illumination.coerceIn(0f, 1f)
    val s = if (litRight) 1f else -1f
    val rx = radius * abs(2f * k - 1f)
    val bulge = if (k < 0.5f) s else -s
    return Path().apply {
        moveTo(center.x, center.y - radius)
        arcTo(
            Rect(center, radius),
            startAngleDegrees = -90f,
            sweepAngleDegrees = 180f * s,
            forceMoveTo = false
        )
        arcTo(
            Rect(center.x - rx, center.y - radius, center.x + rx, center.y + radius),
            startAngleDegrees = 90f,
            sweepAngleDegrees = -180f * bulge,
            forceMoveTo = false
        )
        close()
    }
}

/**
 * Across, 0..1: the compass bearing seen facing the equator. In the north south is the
 * middle, east (90°) toward the left and west (270°) toward the right; the scale runs from
 * 30° to 330° so that a midsummer sunrise in the north-east still lands on the screen. In
 * the south the reader faces north: the bearing turns half a circle first.
 */
internal fun bodyAcross(azimuthDeg: Double, southern: Boolean): Float {
    val facing = if (southern) (azimuthDeg + 180.0).mod(360.0) else azimuthDeg.mod(360.0)
    return ((facing - 30.0) / 300.0).coerceIn(0.0, 1.0).toFloat()
}

/** Up, 0..1: altitude above the horizon, full at [TopAltitude] — a sun that high is the
 * top of the sky for this band, and the band is not tall enough to spend on the rest. */
internal fun bodyUp(altitudeDeg: Double): Float =
    (altitudeDeg / TopAltitude).coerceIn(0.0, 1.0).toFloat()

private const val TopAltitude = 60.0
private const val EdgeInset = 0.08f
private val BodyRadius = 11.dp
