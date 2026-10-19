package com.callbackdev.chiaro.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/*
 * The sun and the moon on the canvas: the rules (design review, 23 set 2026; drawn by the
 * shader since 19 ott 2026, review of the sky).
 *
 * Where a body stands is arithmetic on the same `AstronomyEngine` the gradient reads, so the
 * disc and the colour behind it cannot disagree about whether the sun is up: **across** by
 * compass bearing as the reader facing the equator sees it ([bodyAcross]: east on the left in
 * the north, on the right in the south — a winter sun stays near the middle, a summer one rises
 * and sets near the edges, which is true), **up** by altitude ([bodyUp]), inside the band of
 * sky the layout keeps between the place row and the hero (`SkyColumn`, [SkyBand]). Below the
 * horizon a body is not drawn. What veils a body is the cloud's **opacity**, not its cover
 * ([sunVeil], [moonVeil]): a sky closed by a high veil still shows the sun's disc, one closed by
 * a low deck shows neither it nor the moon.
 *
 * Until 19 ott 2026 the bodies were a Compose layer over the shader and over the scrim, veiled
 * by the cloud's global opacity alone, with a glow painted in SRC_OVER: a full disc stood on a
 * cumulus at 25% of cover, the clouds' rim light was centred 19 to 42 dp from the drawn sun
 * (the shader had its own idea of where the band was), and a sun at noon read as a pale sticker.
 * Now `SkyShaderSource` draws them behind the clouds, in linear light, from the same band and
 * the same rules below, which hide a body **pixel by pixel** under the deck: so the global veil
 * a disc wears is the opacity of what the deck does not draw, the high veil's, or the whole
 * cover's on a wet hour (`skyScene`). The moon's phase ([moonLitPath]) is one geometry in two
 * places: the shader's mask on the canvas, this path on the tonight card.
 */

/** How the cloud draws the sun: the disc's opacity, the glow's at its core and at a third
 * of its reach, and how far the glow spreads (× the clear-sky reach). */
internal data class SunVeil(val disc: Float, val glowCore: Float, val glowMid: Float, val spread: Float)

/**
 * The sun under [cloud] (0 clear, 1 all the light taken: [cloudOpacity]). Until 24 set 2026 the cloud only faded
 * everything — disc and glow by the same 70% — so an overcast sun at noon was a crisp
 * grey-white disc at 30% with almost no glow: on a device, over a grey sky, **it read as
 * the moon** (committente, from a screenshot at 15:50 with the moon three hours from
 * rising). A sun behind a full cover has no edge; the cloud spreads its light. So the
 * disc now fades with the cloud to nothing, and the glow **keeps** most of its strength
 * and spreads a third wider: at full cover the sun is a bright, soft patch of sky, at
 * 80% a faint disc in a wide glow, at half a disc with its halo, in a clear sky the same
 * sun as before. The moon, always a crisp disc with its phase, can no longer be taken
 * for it. Simulated on the screenshot's own sky before it was written.
 *
 * Since 16 ott 2026 [cloud] is the cloud's opacity rather than its cover, and the two ends
 * are two skies: a full high veil is about 0.3, the sun's disc still there in a wide halo,
 * which is what the 24 set glow was for; only a closed low deck reaches 1, and under a
 * stratus the sun is a faint, broad brightening and no more. So the glow now dims with the
 * opacity as well (its core from 0.55 to 0.30) while it spreads wider: Lima's stratus on a
 * device kept a sun as bright as a veiled one.
 */
internal fun sunVeil(cloud: Float): SunVeil {
    val c = cloud.coerceIn(0f, 1f)
    return SunVeil(
        disc = 1f - c,
        glowCore = 0.55f - 0.25f * c,
        glowMid = 0.18f + 0.06f * c,
        spread = 1f + 0.4f * c
    )
}

/** How the cloud draws the moon: the opacity of its disc (the lit part and the earthshine)
 * and of its glow, and how far the glow spreads (× the clear-sky reach). */
internal data class MoonVeil(val disc: Float, val glow: Float, val spread: Float)

/**
 * The moon under [cloud] (its [cloudOpacity], 16 ott 2026). By day the moon is the pale thing
 * it is, and a cloud that takes the light takes it: behind a closed low deck there is no moon
 * to see, behind a high veil a paler one. At night it is the brightest mark, and a deck that
 * hides its disc still shows where it is, as a dim, wide glow. Until then it faded by 80% of
 * the cover at most, so a day moon stayed drawn behind a stratus that hides the sun.
 */
internal fun moonVeil(cloud: Float, day: Boolean): MoonVeil {
    val c = cloud.coerceIn(0f, 1f)
    return if (day) {
        MoonVeil(disc = MoonByDay * (1f - c), glow = MoonByDay * (1f - c), spread = 1f)
    } else {
        MoonVeil(disc = MoonByNight * (1f - c), glow = MoonByNight * (1f - 0.6f * c), spread = 1f + 0.3f * c)
    }
}

private const val MoonByDay = 0.55f
private const val MoonByNight = 0.95f

/** Waxing (moon − sun longitude under 180°), the lit limb faces the evening sun: west, which
 * is on the right for a reader in the north and on the left in the south. */
internal fun litOnRight(moonElongationDeg: Double, southern: Boolean): Boolean {
    val waxing = moonElongationDeg < 180.0
    return waxing != southern
}

/**
 * The lit part of a moon at [illumination] (0 new, 1 full): a half disc on the lit side,
 * closed by the terminator — an ellipse whose half-width is `r·|2k − 1|`, bulging toward
 * the lit side for a crescent and away from it for a gibbous moon. The shader's moon is the
 * same geometry as a mask (`SkyShaderSource`); this path draws it on the tonight card.
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

/** The canvas' width a body never crosses, each side, as a fraction of it: a sun on the
 * very edge would be a half sun. The shader places the bodies with the same inset. */
internal const val EdgeInset = 0.08f

/** The disc's radius; the moon's is nine tenths of it. */
internal val BodyRadius = 11.dp
