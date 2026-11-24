package com.callbackdev.chiaro.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.sky.AstronomyEngine
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.cos

/*
 * The sun and the moon on the canvas: the rules (design review, 23 set 2026; drawn by the
 * shader since 19 ott 2026, review of the sky).
 *
 * Where a body stands is arithmetic on the same `AstronomyEngine` the gradient reads, so the
 * disc and the colour behind it cannot disagree about whether the sun is up: **across** by
 * compass bearing as the reader facing the noon sun sees it ([bodyAcross], [facesNorth]: east
 * on the left for who faces south, on the right for who faces north — a winter sun stays near
 * the middle, a summer one rises and sets near the edges, which is true), **up** by altitude
 * ([bodyUp]), inside the band of sky the layout keeps between the place row and the hero
 * (`SkyColumn`, [SkyBand]). Below the horizon a body is not drawn. What veils a body is the
 * cloud's **opacity**, not its cover ([sunVeil], [moonVeil]): a sky closed by a high veil still
 * shows the sun's disc, one closed by a low deck shows neither it nor the moon.
 *
 * The review of 24 nov 2026 (the committente): the reader faces where the sun culminates, not
 * the equator ([facesNorth]), a body that passes behind the reader comes back across the
 * canvas instead of leaping from one edge to the other, and one that passes overhead is drawn
 * toward the middle ([bodyAcross]); the moon's dark part is
 * the earth's light on a thin crescent in the dark and nothing else ([earthshineShare]); a day
 * moon has no glow and pales with the daylight, not at the instant the sun crosses the horizon
 * ([moonVeil]); and a low moon in the dark is warmed by the air the low sun is reddened by
 * ([moonWarmth]).
 *
 * Until 19 ott 2026 the bodies were a Compose layer over the shader and over the scrim, veiled
 * by the cloud's global opacity alone, with a glow painted in SRC_OVER: a full disc stood on a
 * cumulus at 25% of cover, the clouds' rim light was centred 19 to 42 dp from the drawn sun
 * (the shader had its own idea of where the band was), and a sun at noon read as a pale sticker.
 * Now `SkyShaderSource` draws them behind the clouds, in linear light, from the same band and
 * the same rules below, which hide a body **pixel by pixel** under the deck: so the global veil
 * a disc wears is the opacity of what the deck does not draw, the high veil's, or the whole
 * cover's on a wet hour (`skyScene`). The moon's phase ([moonLitPath]) is one geometry in two
 * places: the shader's mask on the canvas, turned onto the bright limb's angle the place sees
 * (`AstronomyEngine.moonBrightLimbAngle`, step 6), and this path on the Sky's moon badge, upright,
 * because that card's moon stands for a whole night and not for an instant.
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
 *
 * [day] is how much daylight there is, 0 at the end of civil twilight to 1 with the sun six
 * degrees up (24 nov 2026). Until then it was a switch on the sun's horizon, and the disc went
 * from 0.95 to 0.55 in the minute of sunrise: the moon pales as the sky brightens, through the
 * twilight. And a day moon has **no glow**: the glow is the moon's light in the air round it,
 * and by day the air is lit by the sun; it was drawn at 0.55, a halo no day moon has.
 */
internal fun moonVeil(cloud: Float, day: Float): MoonVeil {
    val c = cloud.coerceIn(0f, 1f)
    val d = day.coerceIn(0f, 1f)
    return MoonVeil(
        disc = (MoonByNight + (MoonByDay - MoonByNight) * d) * (1f - c),
        glow = MoonByNight * (1f - 0.6f * c) * (1f - d),
        spread = 1f + 0.3f * c * (1f - d)
    )
}

private const val MoonByDay = 0.55f
private const val MoonByNight = 0.95f

/**
 * How much of the earth's light shows on the moon's dark part, 0..1 (24 nov 2026, pure,
 * `SkyBodiesTest`): all of it on a crescent of [EarthshineThin] or less, none from
 * [EarthshineGone] of the disc lit, and only in the dark, from the sun on the horizon (none) to
 * the end of civil twilight (all). The Sky's own event says when it is seen: a crescent between
 * 1% and 18%, in the dusk or before dawn.
 *
 * Until then the dark part was drawn at 14% of the face at every phase and every hour: at 3%
 * of phase a grey ball with a rim of light, at half a coin of two tones, and by day a pale disc
 * on the blue, so that a new moon beside the sun was a ghost nobody has ever seen. The dark
 * part of a half moon is the night sky, and of a day moon the day's. Whoever draws the moon
 * multiplies this by the strength its own paint needs (the shader mixes light, a canvas paint).
 */
internal fun earthshineShare(illumination: Float, sunAltitudeDeg: Double): Float =
    smoothstep(EarthshineGone, EarthshineThin, illumination) * smoothstep(0f, -6f, sunAltitudeDeg.toFloat())

private const val EarthshineThin = 0.08f
private const val EarthshineGone = 0.35f

/** The earth's light on the dark part as the shader mixes it over the sky, in linear light:
 * half of the 14% every phase had, on the crescents that have it. */
internal const val EarthshineLight = 0.07f

/**
 * How much of the horizon's colour a body wears, 0..1: none from twelve degrees up, all on the
 * horizon. The air that reddens the low sun warms the low moon (24 nov 2026): one rule for the
 * two, where the moon kept its blue-white down to the horizon and a full moon rose, an hour
 * after an amber sun had set, the colour it has overhead.
 */
internal fun horizonWarmth(altitudeDeg: Double): Float =
    (1.0 - altitudeDeg / 12.0).coerceIn(0.0, 1.0).toFloat()

/** How much daylight there is, 0..1: none at the end of civil twilight, all with the sun six
 * degrees up. What the scene's clouds, the moon's veil and its warmth are read on. */
internal fun daylight(sunAltitudeDeg: Double): Float = smoothstep(-6f, 6f, sunAltitudeDeg.toFloat())

/**
 * How warm the moon's face is, 0..1: the horizon's colour, in the dark only (the control review
 * of 24 nov 2026). The air reddens the moon by day as by night, but by day the air between is
 * lit and washes it out, and the first cut warmed it at every hour: drawn by the shader, an
 * amber moon rising at sunset was lost in the amber sky it rises in, and a low day moon was a
 * grey-beige disc on the blue. So the warmth goes with the daylight: all of it from the end of
 * civil twilight, none with the sun six degrees up, where the moon is the white it was.
 */
internal fun moonWarmth(moonAltitudeDeg: Double, sunAltitudeDeg: Double): Float =
    horizonWarmth(moonAltitudeDeg) * (1f - daylight(sunAltitudeDeg))

/**
 * Whether the canvas' reader faces north (24 nov 2026): where the sun culminates, which is
 * north for a place south of the latitude the sun stands over ([sunDeclinationDeg]). Outside
 * the tropics it is the hemisphere, every day of the year, and nothing changed there. Inside
 * them the sun passes on the pole's side for part of the year (Huaraz, 9.5° south, from
 * mid-October to late February; Singapore from March to September), and a reader made to face
 * the equator had it behind them: pinned to one edge of the canvas all morning, across the
 * whole width in the minutes of noon, pinned to the other all afternoon (measured for Huaraz
 * on 24 nov 2026: 1.00 at 11:40, 0.00 at 12:00). Facing the sun, it rises on one side, passes
 * the middle at noon and sets on the other, as it does everywhere else.
 */
internal fun facesNorth(latitudeDeg: Double, sunDeclinationDeg: Double): Boolean =
    latitudeDeg < sunDeclinationDeg

/** [facesNorth] at [coords] on the local day [date]: the declination read at that day's noon,
 * so that a day has one frame and the hours of it cannot disagree. */
internal fun facesNorth(coords: Coordinates, date: LocalDate, zone: ZoneId): Boolean =
    facesNorth(coords.lat, AstronomyEngine.sunDeclination(date.atTime(12, 0).atZone(zone).toInstant()))

/**
 * The lit part of a moon at [illumination] (0 new, 1 full): a half disc on the lit side,
 * closed by the terminator — an ellipse whose half-width is `r·|2k − 1|`, bulging toward
 * the lit side for a crescent and away from it for a gibbous moon. The shader's moon is the
 * same geometry as a mask (`SkyShaderSource`); this path draws it on the Sky's moon badge.
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
 * Across, 0..1: the compass bearing seen by a reader who faces south, or north when
 * [southern] (the frame of a place in the south; Today's canvas passes [facesNorth], the side
 * the sun culminates on). Facing south, south is the middle, east (90°) toward the left and
 * west (270°) toward the right; the scale runs from 30° to 330° so that a midsummer sunrise
 * in the north-east still lands on the screen. Facing north the bearing turns half a circle
 * first.
 *
 * **Behind the reader a body comes back across** (the control review of 24 nov 2026): in the
 * sixty degrees the scale does not hold, [BehindDeg] either side of straight behind, it goes
 * from its edge through the middle to the other edge. Until then it stood on the edge and, at
 * the bearing straight behind, leapt to the other: the midnight sun north of the polar circle
 * every night, and in the tropics whichever of the sun and the moon culminates on the side the
 * reader does not face, which with the reader facing the noon sun is the moon on the nights
 * round the full one (measured at Huaraz, 10 nov to 9 dec 2026: eleven nights of twenty-seven).
 * A body that goes right round the sky has to cross a flat canvas twice; it crosses it, it
 * does not jump. Up to 150° from where the reader looks nothing changed.
 */
internal fun bodyAcross(azimuthDeg: Double, southern: Boolean): Float {
    val facing = if (southern) (azimuthDeg + 180.0).mod(360.0) else azimuthDeg.mod(360.0)
    // Degrees from straight ahead, −180..180, to the right positive.
    val off = facing - 180.0
    val away = abs(off)
    val fromMiddle = if (away <= 180.0 - BehindDeg) {
        off / AcrossSpanDeg
    } else {
        (if (off < 0.0) -0.5 else 0.5) * (180.0 - away) / BehindDeg
    }
    return (0.5 + fromMiddle).toFloat()
}

/** The bearings the canvas holds from edge to edge, and the ones either side of straight
 * behind the reader, where a body comes back across it. */
private const val AcrossSpanDeg = 300.0
private const val BehindDeg = 30.0

/**
 * Across for a body at [altitudeDeg]: its bearing's place ([bodyAcross]), drawn toward the
 * middle as it nears the zenith (the control review of 24 nov 2026). A body overhead is
 * neither east nor west, and its bearing turns half a circle in the minutes it passes: by its
 * bearing alone the disc crossed most of the canvas in one of them (counted over 2026, facing
 * the noon sun and with the fold: the moon on 22 to 28 days a year in the tropics, up to 0.74
 * of the width in a minute at Mumbai; the sun on its 10 to 15 days overhead, up to 0.38). From
 * [ZenithFromDeg] up, the distance from the middle shrinks as the body's distance from the
 * zenith does, `cos(altitude)`, to nothing straight overhead: counted again over the same year
 * at Huaraz, Singapore and Mumbai, the largest step in a minute is 0.018 of the width for the
 * moon and 0.005 for the sun. Under 75° it is the bearing's place, to the last bit: north of
 * about 44° of latitude no body climbs that high, so at Milan nothing moved; at Rome the moon
 * by 0.004 at most, at Palermo by 0.026, and the sun at neither.
 */
internal fun bodyAcross(azimuthDeg: Double, altitudeDeg: Double, southern: Boolean): Float {
    val byBearing = bodyAcross(azimuthDeg, southern)
    if (altitudeDeg <= ZenithFromDeg) return byBearing
    val share = cos(Math.toRadians(altitudeDeg.coerceAtMost(90.0))) / cos(Math.toRadians(ZenithFromDeg))
    return (0.5 + (byBearing - 0.5) * share).toFloat()
}

/** The altitude from which a body's place across is drawn toward the middle. */
private const val ZenithFromDeg = 75.0

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
