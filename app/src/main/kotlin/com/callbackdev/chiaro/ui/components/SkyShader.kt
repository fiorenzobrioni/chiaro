package com.callbackdev.chiaro.ui.components

import android.graphics.RuntimeShader
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import com.callbackdev.chiaro.domain.model.CloudLayers
import com.callbackdev.chiaro.ui.theme.SkyGradient
import com.callbackdev.chiaro.ui.theme.SkyPalette
import kotlin.math.abs
import kotlin.math.sin

/**
 * The sky's scene (DESIGN §3.9, second review of Today, 15 ott 2026): everything the canvas'
 * shader draws over the gradient, computed from the forecast for the hour we are in and from
 * the ephemeris, never chosen for looks. The gradient itself is the palette's, unchanged
 * (§3.2–3.4); the scene is what a gradient could not say.
 *
 * - [cover], the hour's cloud cover, 0..1, is the share of the sky the clouds take;
 * - [deck], the low and middle layers' part of it, drawn as clouds with a body, and [veil],
 *   the high layer's, drawn as cirrus behind them: streaks while it is scattered, a milky
 *   sheet as it closes ([cloudDecks]);
 * - [opacity], how much of the light that cover takes ([cloudOpacity]): a low deck hides the
 *   sun, a high veil lets it through. The bodies' veils read it too ([SkyBodies]);
 * - [lit] and [shade], the clouds' two colours, from the same light the gradient has (white
 *   by day, warm in the golden hour, the moon's grey at night), never brighter than the
 *   palette's brightest stop ([maxLuminance], what §3.6's scrim is measured against);
 * - [sun] and [moon] (19 ott 2026, §8.1c): the bodies the shader draws behind the clouds, in
 *   the band the layout keeps for them ([SkyBand]), placed by the bodies' rules (`SkyBodies.kt`)
 *   and veiled by them; null while a body is under the horizon;
 * - [light], where the light comes from, as a body's place (across by bearing, up by altitude,
 *   the frame of [SunMark.across]; up under zero is the sun below the horizon at dusk, and the
 *   clouds' undersides face it): the sun's or the moon's, whichever gives more light, the
 *   edges of the clouds that face it are the bright ones; [lightAmount], 0..1, how much
 *   light that is, which is how much the clouds are shaded at all;
 * - [stars], 0..1: a dark sky, a clear one and no moon to wash it out; and [starField], the
 *   stars that stand in the canvas' window at this instant ([StarField], step 6, 19 ott 2026),
 *   which the shader draws where they are, as bright and as warm as they are;
 * - [rain], [snow], 0..1: only while the hour is wet by the state engine, by its intensity;
 * - [drift], the clouds' speed across the canvas in dp a second, signed: the wind's, from the
 *   direction it blows toward as the reader facing the equator sees it, or as seen from where
 *   a window looks;
 * - [turn], degrees to the right: how far the camera has been turned from where the clouds'
 *   field was laid (25 ott 2026). The shader reads the veil and what falls, flat sheets, that
 *   far along, in the pixels a star moves for the same turn (the stars' camera's focal
 *   length), and turns the deck's camera by the angle that moves its centre by those pixels,
 *   so a window turned by a finger turns as one picture; zero on a canvas nobody turns. The
 *   wind's [drift] is given in the frame the field was laid in, and the turn carries it;
 * - [fog], 0..1, and [fogColor] (review of the sky, 9 nov 2026): on an hour whose state is fog
 *   the canvas draws it, a bank thicker toward the ground in the clouds' own light without
 *   their hue. Until then the scene knew rain and snow only, and under the word «Nebbia» the
 *   canvas drew whatever the cover said, often a clear sky;
 * - [storm], 0 or 1: a thunderstorm in the hour's state, and the deck flashes from inside;
 * - [lowSun], 0..1, and [sunAcross]: how much the horizon lights up toward a low sun and dims
 *   away from it, and where that is across the canvas (the sun's own place, under the
 *   horizon too): a sky at sunset is not the same from east to west;
 * - [rays], 0..1: the sun's shafts between the clouds of a broken deck;
 * - [meteors], 0..1, [radiant] and [meteorSeed]: on a shower's peak night, how well a meteor
 *   would show (the dark, the open sky, the moon), the radiant in the stars' camera (degrees
 *   across and up) its trails point away from, and a number that sets the sequence apart
 *   from another night's.
 */
@Immutable
data class SkyScene(
    val cover: Float,
    val deck: Float,
    val veil: Float,
    val opacity: Float,
    val lit: Color,
    val shade: Color,
    val light: Offset,
    val glow: Float,
    val stars: Float,
    val rain: Float,
    val snow: Float,
    val drift: Float,
    val maxLuminance: Float,
    val sun: SunMark? = null,
    val moon: MoonMark? = null,
    val lightAmount: Float = 1f,
    val starField: List<StarMark> = emptyList(),
    /** How much the planets show, 0..1: open from the sun at −3° and fully at −5°, before the
     * stars (20 ott 2026), under the open sky; a moon dims them little. */
    val planets: Float = 0f,
    /** How much of the Milky Way shows, 0..1: the dark, the open sky and the moon's absence, all three. */
    val milkyWay: Float = 0f,
    /** The galactic frame the shader draws the Milky Way in; null by day, and the band is off. */
    val galactic: SkyFrame? = null,
    /** How far the camera has been turned, degrees to the right, unwrapped: the sheets the veil
     * and the rain are read from have no period, and a wrap at north would make them jump. */
    val turn: Float = 0f,
    val fog: Float = 0f,
    val fogColor: Color = Color.Transparent,
    val storm: Float = 0f,
    val lowSun: Float = 0f,
    val sunAcross: Float = 0.5f,
    val rays: Float = 0f,
    val meteors: Float = 0f,
    val radiant: Offset = Offset.Zero,
    val meteorSeed: Float = 0f
)

/**
 * The sun as the shader draws it (19 ott 2026): where it stands ([across], [up], the bodies'
 * frame), its [color] and [warmth] (0 high, 1 at the horizon), and its veil: the [disc]'s and
 * the corona's ([glowCore]) strength under the high veil alone, because the deck hides them
 * pixel by pixel in the shader; the long tail's ([glowMid]) and its [spread] under the whole
 * opacity, because that light is scattered in front of the clouds (`sunVeil`).
 */
@Immutable
data class SunMark(
    val across: Float,
    val up: Float,
    val color: Color,
    val warmth: Float,
    val disc: Float,
    val glowCore: Float,
    val glowMid: Float,
    val spread: Float
)

/**
 * The moon as the shader draws it (19 ott 2026): its place, its [illumination] (0 new, 1 full)
 * and the angle of its bright limb ([limbAngleDeg], `AstronomyEngine.moonBrightLimbAngle`: 0
 * lit from the right, 90 from above, −90 from below, as the place sees it at this instant),
 * and its veil: the [disc] under the high veil alone, the [glow] and its [spread] under the
 * whole opacity (`moonVeil`). [northAngleDeg] is where celestial north points at the moon as
 * the place sees it (the limb's frame: 0 to the right, 90 up), which is how its face is
 * turned: the seas are drawn on it (9 nov 2026), and a moon rising in the east lies on its
 * side. The moon's own axis, up to 24° off celestial north, and its libration are not applied.
 */
@Immutable
data class MoonMark(
    val across: Float,
    val up: Float,
    val illumination: Float,
    val limbAngleDeg: Float,
    val disc: Float,
    val glow: Float,
    val spread: Float,
    val northAngleDeg: Float = 90f
)

/**
 * The band of sky the sun and the moon stand in, inside the canvas (§8.1c): its [top] from the
 * canvas' top edge and its [height]. Today's column keeps it between the place row and the
 * hero and reports it; the Settings preview declares its own. The shader needs it for two
 * things: where to place a body, and where the luminance cap may open (the band holds no text).
 */
@Immutable
data class SkyBand(val top: Dp, val height: Dp)

/** What the hour is doing, for [skyScene]: nothing falls, rain falls, or snow does. */
enum class SkyFall { NONE, RAIN, SNOW }

/**
 * The scene for one moment (pure, `SkySceneTest`). [gradient] is the canvas the palette made
 * for the same moment, so the clouds are lit by the colours the sky already has.
 */
fun skyScene(
    palette: SkyPalette,
    gradient: SkyGradient,
    sunAltitudeDeg: Double,
    sunAzimuthDeg: Double,
    moonAltitudeDeg: Double,
    moonAzimuthDeg: Double,
    moonIllumination: Double,
    /** The angle of the moon's bright limb as the place sees it, degrees
     * (`AstronomyEngine.moonBrightLimbAngle`: 0 lit from the right, 90 from above). */
    moonLimbAngleDeg: Double,
    southern: Boolean,
    cloudPct: Int,
    layers: CloudLayers?,
    precipPct: Int,
    fall: SkyFall,
    fallMmPerHour: Double,
    windKph: Double,
    windFromDeg: Int,
    /** The stars in the canvas' window at this instant ([StarField.visible]); none by day. */
    starMarks: List<StarMark> = emptyList(),
    /** The galactic frame as this camera sees it ([StarField.galacticFrame]), for the Milky Way; none by day. */
    galactic: SkyFrame? = null,
    /** Where the camera looks, degrees from north, for the wind's direction on screen; null faces
     * the equator as the bodies do ([StarField.facing]). A window passes where it looks (25 ott 2026). */
    facingDeg: Double? = null,
    /** How far the camera has been turned from where the field was laid, degrees to the right ([SkyScene.turn]). */
    turnDeg: Double = 0.0,
    /** How thick the fog is, 0..1, on an hour whose state is fog ([fogDensity]); zero on any other. */
    fog: Float = 0f,
    /** A thunderstorm in the hour's state (a storm, not the possibility of one): the deck flashes. */
    thunder: Boolean = false,
    /** A peak night's radiant in the stars' camera, degrees across (x) and up (y), or null:
     * the night is no shower's, and no meteor is drawn. */
    radiant: Offset? = null,
    /** Where celestial north points at the moon, as the place sees it ([MoonMark.northAngleDeg]). */
    moonNorthAngleDeg: Double = 90.0
): SkyScene {
    val cover = (cloudPct.coerceIn(0, 100) / 100f)
    val (deck, veil) = cloudDecks(cloudPct, layers)
    val opacity = cloudOpacity(cloudPct, layers, fall)
    val maxLum = palette.brightestBottomStop().luminance()
    val day = smoothstep(-6f, 6f, sunAltitudeDeg.toFloat())
    val moonUp = moonAltitudeDeg > 0.0
    val moonLight = if (moonUp) (moonIllumination.toFloat() * smoothstep(0f, 25f, moonAltitudeDeg.toFloat())) else 0f
    // How much the sun lights the clouds (step 3, 19 ott 2026): full above 1°, gone at −9°,
    // and in between the twilight that lights their undersides from below the horizon. The
    // clouds' light is the sun's or the moon's, whichever gives more, never a switch on the
    // sun's altitude alone: at −3° with a thin crescent up, the dusk still lights them.
    val sunLight = smoothstep(-9f, 1f, sunAltitudeDeg.toFloat())
    val useSun = sunLight >= moonLight
    val lightAmount = maxOf(sunLight, moonLight)
    // Lit by day: a white with a breath of the sky in it. In the golden hour and the dusk the
    // light is the horizon's, the gradient's own bottom stop. At night the clouds are the
    // moon's grey, a little lighter than the sky behind them under a bright moon and barely
    // there without one.
    val dayLit = lerp(Color.White, gradient.mid, 0.12f)
    val golden = smoothstep(14f, 2f, sunAltitudeDeg.toFloat())
    val warmLit = lerp(dayLit, gradient.bottom, 0.65f * golden)
    val nightLit = lerp(gradient.mid, SkyPalette.CloudNight, 0.25f + 0.45f * moonLight)
    val wet = (precipPct.coerceIn(0, 100) / 100f)
    // A rain cloud is darker than the sky in its gaps, not lighter (review of the sky, 19 ott
    // 2026): the gradient dims under a likely rain (§3.3) but the lit colour did not, so a
    // 94% deck at 90% of rain drew light grey clouds with dark blue holes between them, the
    // inverse of a nimbostratus. The lit colour now dims by [WetLitDim] of the chance, under
    // the shade's own 30%, so the sky's mid stop lands between the clouds' two colours.
    // Under a thunderstorm the deck is darker still (9 nov 2026): a cumulonimbus takes the
    // light a nimbostratus leaves, and the flash needs a dark cloud to be seen against.
    val storm = if (thunder) 1f else 0f
    val mist = fog.coerceIn(0f, 1f)
    val lit = capLuminance(
        scale(lerp(nightLit, warmLit, sunLight), (1f - WetLitDim * wet) * (1f - StormDim * storm)),
        maxLum
    )
    // A closed deck is grey (device, 16 ott 2026): the underside of a stratus is lit by the
    // cloud itself, not by the blue above it, and a shade still drawn toward the sky's top
    // stop put blue hollows into a sky the hero called overcast. The hue goes as the deck
    // closes, the luminance stays, so the deck keeps its folds and the cap still holds.
    val toned = lerp(lit, gradient.top, 0.45f + 0.25f * (1f - day)).let { scale(it, 1f - 0.30f * wet) }
    val shade = capLuminance(lerp(toned, grey(toned), DeckGrey * smoothstep(0.55f, 1f, deck)), maxLum)
    // The bodies (19 ott 2026): the shader hides them under the deck pixel by pixel, so the
    // veil a disc wears globally is the opacity of what the deck does not draw: the high
    // veil's share of the light, or the whole cover's on a wet hour, when what falls comes
    // from a thick cloud whichever layer the model files it under. The glows in front of the
    // clouds read the whole opacity, as the rules were written for.
    val veilOpacity = if (fall != SkyFall.NONE) opacity else minOf(CloudLayers.HIGH_OPACITY * veil, opacity)
    // Under a thunderstorm there is no sun to draw (9 nov 2026): not the disc, not the pale
    // patch a stratus lets through. A cumulonimbus is kilometres deep.
    val sun = if (sunAltitudeDeg <= -0.5 || thunder) null else {
        // Warm near the horizon, white-gold above the golden hour.
        val warmth = (1.0 - (sunAltitudeDeg / 12.0)).coerceIn(0.0, 1.0).toFloat()
        val behind = sunVeil(veilOpacity)
        val front = sunVeil(opacity)
        // In fog the disc is all but gone and what is left is the pale patch the shader
        // draws where it stands (9 nov 2026).
        SunMark(
            across = bodyAcross(sunAzimuthDeg, southern),
            up = bodyUp(sunAltitudeDeg),
            color = lerp(SkyPalette.SunHigh, SkyPalette.SunLow, warmth),
            warmth = warmth,
            disc = behind.disc * (1f - FogDisc * mist),
            glowCore = behind.glowCore * (1f - FogGlow * mist),
            glowMid = front.glowMid * (1f - FogGlow * mist),
            spread = front.spread
        )
    }
    val moon = if (!moonUp) null else {
        val byDay = sunAltitudeDeg > 0.0
        val behind = moonVeil(veilOpacity, byDay)
        val front = moonVeil(opacity, byDay)
        MoonMark(
            across = bodyAcross(moonAzimuthDeg, southern),
            up = bodyUp(moonAltitudeDeg),
            illumination = moonIllumination.coerceIn(0.0, 1.0).toFloat(),
            limbAngleDeg = moonLimbAngleDeg.toFloat(),
            disc = behind.disc * (1f - FogDisc * mist),
            glow = front.glow * (1f - FogGlow * mist),
            spread = front.spread,
            northAngleDeg = moonNorthAngleDeg.toFloat()
        )
    }
    val (lightAz, lightAlt) = if (useSun) sunAzimuthDeg to sunAltitudeDeg else moonAzimuthDeg to moonAltitudeDeg
    // The light's place is the body's own, in the same frame and by the same arithmetic the
    // shader places the discs with: the rim light is centred on the sun, not near it. A sun
    // under the horizon goes under the band, down to −1 at −12°: the shader puts it below the
    // canvas, and the clouds' undersides are what faces it, which is the sunset.
    val lightUp = if (lightAlt >= 0.0) bodyUp(lightAlt) else (lightAlt / TwilightDepthDeg).coerceIn(-1.0, 0.0).toFloat()
    val light = Offset(bodyAcross(lightAz, southern), lightUp)
    val glow = lightAmount * (1f - 0.6f * opacity) * (if (useSun) 1f else 0.6f) * (1f - mist)
    val darkness = smoothstep(-10f, -18f, sunAltitudeDeg.toFloat())
    // Fog hides the stars as a closed sky does: what is over it is not seen through it.
    val stars = darkness * (1f - cover) * (1f - cover) * (1f - 0.65f * moonLight) * (1f - mist)
    // The planets before the stars (20 ott 2026): Venus at −4 shows against a dusk the stars
    // cannot, and a moon takes a third of it where it takes two thirds of a star.
    val planets = smoothstep(StarField.PLANETS_SUN_DEG.toFloat(), -5f, sunAltitudeDeg.toFloat()) *
        (1f - cover) * (1f - cover) * (1f - 0.3f * moonLight) * (1f - mist)
    // The Milky Way wants more dark than a star does (20 ott 2026): the cover cubed, because a
    // veil that leaves Vega takes the band, and the moon squared, because a half moon takes it
    // twice over; what a city's light takes is not modelled, as it is not for the stars.
    val milkyWay = if (galactic == null) 0f else {
        darkness * (1f - cover) * (1f - cover) * (1f - cover) * (1f - moonLight) * (1f - moonLight) * (1f - mist)
    }
    // The fog's colour (9 nov 2026): the clouds' two colours, mostly the lit one, with three
    // quarters of the hue gone, because a fog is lit from every side and has no blue of its
    // own; under the cap like every cloud.
    val body = lerp(shade, lit, FogLit)
    val fogColor = capLuminance(lerp(body, grey(body), FogGrey), maxLum)
    // The low sun's side of the sky (9 nov 2026): from 16° down the horizon lights up and
    // warms toward the sun and dims away from it, most between 3° and −2°, gone at −10°; less
    // under a cloud that takes the light, nothing in fog. A window that looks where it wants
    // draws it only while the sun's disc stands in its band, where the lobe is centred.
    val lowSun = smoothstep(16f, 3f, sunAltitudeDeg.toFloat()) * smoothstep(-10f, -2f, sunAltitudeDeg.toFloat()) *
        (1f - 0.6f * opacity) * (1f - mist) * (if (facingDeg != null && sun == null) 0f else 1f)
    // The sun's shafts (9 nov 2026): only through a broken deck, between a sky too open to
    // cast any and a deck too closed to let one through; not while something falls, not in fog.
    val broken = smoothstep(0.12f, 0.4f, deck) * (1f - smoothstep(0.72f, 0.97f, deck))
    val rays = if (sun == null || fall != SkyFall.NONE) 0f else broken * (1f - mist) * (0.6f + 0.4f * sun.warmth) * sun.disc
    // A meteor shows where a third-magnitude star would: in the dark, under an open sky, and a
    // bright moon takes half of them (the Sky's verdict says the same of a peak night).
    val meteors = if (radiant == null || radiant.y <= 0f) 0f else {
        darkness * (1f - cover) * (1f - cover) * (1f - 0.5f * moonLight) * (1f - mist)
    }
    val intensity = (0.35f + 0.65f * (fallMmPerHour / FallFullMmPerHour).toFloat()).coerceIn(0.35f, 1f)
    // Toward where the wind blows, as seen from where the camera looks: facing the equator (in
    // the north the right of the screen is the west, in the south the east, the frame the sun
    // and moon stand in) unless a window says where it looks (25 ott 2026).
    val toward = Math.toRadians((windFromDeg + 180.0).mod(360.0))
    val bearing = Math.toRadians(facingDeg ?: StarField.facing(southern))
    val across = sin(toward - bearing)
    val speed = DriftBase + DriftPerKph * windKph.toFloat()
    val drift = (if (abs(across) < 0.15) 0.15 * (if (across < 0) -1 else 1) else across).toFloat() * speed
    return SkyScene(
        cover = cover,
        deck = deck,
        veil = veil,
        opacity = opacity,
        lit = lit,
        shade = shade,
        light = light,
        glow = glow,
        stars = stars,
        rain = if (fall == SkyFall.RAIN) intensity else 0f,
        snow = if (fall == SkyFall.SNOW) intensity else 0f,
        drift = drift,
        maxLuminance = maxLum,
        sun = sun,
        moon = moon,
        lightAmount = lightAmount,
        starField = starMarks,
        planets = planets,
        milkyWay = milkyWay,
        galactic = galactic,
        turn = turnDeg.toFloat(),
        fog = mist,
        fogColor = fogColor,
        storm = storm,
        lowSun = lowSun,
        sunAcross = bodyAcross(sunAzimuthDeg, southern),
        rays = rays,
        meteors = meteors,
        radiant = radiant ?: Offset.Zero,
        meteorSeed = (radiant?.x ?: 0f) + (radiant?.y ?: 0f)
    )
}

/**
 * How thick the canvas draws a fog, 0..1, from the visibility the hour's state was read on
 * (9 nov 2026): fog is a visibility under a kilometre, so a kilometre is the thinnest the
 * canvas draws, at [FogThin], and two hundred metres or less the thickest; an hour in fog with
 * no number (the witnesses decided it, STATES §3 rule 5) draws between the two.
 */
fun fogDensity(visibilityKm: Double?): Float {
    val km = visibilityKm ?: return (FogThin + 1f) / 2f
    val thick = 1f - ((km - 0.2) / 0.8).coerceIn(0.0, 1.0).toFloat()
    return FogThin + (1f - FogThin) * thick
}

/** The thinnest fog the canvas draws: at the kilometre that makes it a fog at all. */
internal const val FogThin = 0.6f

/** How much of the lit colour the fog's body has; the rest is the shade. */
private const val FogLit = 0.7f

/** How far the fog's colour goes toward a grey of its own luminance. */
private const val FogGrey = 0.75f

/** What a full fog takes of the sun's and the moon's disc, and of their glows. */
private const val FogDisc = 0.9f
private const val FogGlow = 0.7f

/** How much of the clouds' lit colour a thunderstorm takes, in sRGB value, over a likely rain's. */
private const val StormDim = 0.3f

/** The deck's camera (§3.9, step 4): the elevation of its top row and of its bottom row,
 * degrees. The shader writes them as radians, 0.7854 and 0.2618, and `SkyDeckTest` pins the
 * line. The stars have a wider camera of their own ([StarCameraTopDeg], 20 ott 2026), and the
 * bodies their band: three frames on purpose, see §3.9. */
internal const val SkyCameraTopDeg = 45f
internal const val SkyCameraBottomDeg = 15f

/** The sun this far under the horizon lights the clouds from as far below the canvas as the
 * shader goes; at −9° its light is gone anyway ([skyScene]'s `sunLight`). */
private const val TwilightDepthDeg = 12.0

/**
 * The low deck and the high veil, 0..1 each, for the shader (16 ott 2026). The deck is the low
 * and middle layers together, the veil the high one; each layer is a share of the whole sky and
 * they overlap at random, which is how the provider's total is made of them (measured on the
 * four recorded forecasts, Milan, Reykjavík, Everest and the README's Milan, 672 hours: the
 * total is the random overlap of its layers within 1% on average, 10 at worst). Neither is
 * ever more than the total, which is the number the hero's word comes from. With no layers,
 * or layers that explain nothing of a cloudy total, the whole cover is the deck, as it was
 * before the layers were read.
 */
internal fun cloudDecks(cloudPct: Int, layers: CloudLayers?): Pair<Float, Float> {
    val cover = cloudPct.coerceIn(0, 100) / 100f
    val l = layers?.takeIf { (it.lowPct ?: 0) + (it.midPct ?: 0) + (it.highPct ?: 0) > 0 }
        ?: return cover to 0f
    val deck = 1f - (1f - share(l.lowPct)) * (1f - share(l.midPct))
    return minOf(deck, cover) to minOf(share(l.highPct), cover)
}

/**
 * How much of the sun's or the moon's light the hour's cloud takes, 0 (none) to 1 (all of it),
 * by layer (16 ott 2026): [CloudLayers.lightTaken], the one formula the state engine's sky
 * reads too since 26 ott 2026, so the word under the hero and the light of its canvas come
 * from the same arithmetic. A wet hour takes the light of its whole cover, because what falls
 * comes from a thick cloud whichever layer the model files it under (an anvil is high).
 */
fun cloudOpacity(cloudPct: Int, layers: CloudLayers?, fall: SkyFall): Float =
    if (fall != SkyFall.NONE) cloudPct.coerceIn(0, 100) / 100f else CloudLayers.lightTaken(cloudPct, layers)

private fun share(pct: Int?): Float = (pct ?: 0).coerceIn(0, 100) / 100f

/**
 * The shader's cumulus threshold for a [deck] of 0..1, so that the clouds drawn take the
 * share of the canvas the deck says (16 ott 2026). The threshold used to run straight from
 * 0.80 to 0.16: over the shader's own noise, measured on 1.8 million points of sixty
 * 400 × 300 dp canvases along 100 000 dp of drift, that drew 2% of cloud for 20% of cover and
 * 26% for 50%, and at 100% it still left 11% of the sky to the gradient (the cloud's mean
 * alpha, 0.95, under its 0.94), the blue hollows of an overcast seen on a device. These are
 * the thresholds at which the mean alpha of the clouds is the cover, every 5% (one canvas
 * alone is a few big cells of noise and strays by a few percent either way, as one look at a
 * real sky does); the first is over the noise's ceiling, the last under its floor, so a closed
 * deck has no gap at all.
 *
 * **Measured again on 9 nov 2026**, when the border took its two fine octaves and went from
 * 0.22 to 0.14 wide: the shader's new line over four hundred canvases along 400 000 dp of
 * drift (three million points), because sixty canvases stray from each other by two hundredths
 * and the table should not carry one sample's luck. The old table still held within 0.016
 * (the border's middle did not move); the values moved by under a hundredth.
 */
internal fun deckThreshold(deck: Float): Float {
    val x = deck.coerceIn(0f, 1f) * (DeckThresholds.size - 1)
    val i = x.toInt().coerceAtMost(DeckThresholds.size - 2)
    return DeckThresholds[i] + (DeckThresholds[i + 1] - DeckThresholds[i]) * (x - i)
}

private val DeckThresholds = floatArrayOf(
    0.930f, 0.612f, 0.567f, 0.535f, 0.509f, 0.486f, 0.464f, 0.444f, 0.425f, 0.407f, 0.388f,
    0.370f, 0.351f, 0.332f, 0.312f, 0.291f, 0.268f, 0.242f, 0.210f, 0.166f, -0.250f
)

/** How far a closed deck's shade goes toward a grey of its own luminance. */
private const val DeckGrey = 0.8f

/** How much of the lit colour a sure rain takes, in sRGB value: a quarter, against the
 * shade's 30%, so the folds keep a little of their contrast under a flat wet sky. */
private const val WetLitDim = 0.25f

/** A colour no brighter than [max] in relative luminance, its hue kept. */
internal fun capLuminance(color: Color, max: Float): Color {
    val l = color.luminance()
    if (l <= max || l <= 0f) return color
    // Luminance is linear in the linear channels; scale those, then back to sRGB. Half a
    // percent under the cap, because an sRGB `Color` is stored at 8 bits a channel and the
    // rounding may go up.
    val k = max * CapMargin / l
    fun channel(c: Float): Float = toSrgb(toLinear(c) * k)
    return Color(channel(color.red), channel(color.green), channel(color.blue), color.alpha)
}

/** The grey with [color]'s relative luminance: its hue gone, its brightness kept. */
internal fun grey(color: Color): Color {
    val g = toSrgb(color.luminance())
    return Color(g, g, g, color.alpha)
}

/** An sRGB channel as linear light, by the exact transfer curve (the one the shader inverts). */
internal fun toLinear(c: Float): Float =
    if (c <= 0.04045f) c / 12.92f else Math.pow(((c + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()

/** Linear light as an sRGB channel: the shader's `encode`, in Kotlin. */
internal fun toSrgb(s: Float): Float =
    if (s <= 0.0031308f) s * 12.92f else (1.055 * Math.pow(s.toDouble(), 1 / 2.4) - 0.055).toFloat()

private const val CapMargin = 0.995f

private fun scale(c: Color, k: Float) = Color(c.red * k, c.green * k, c.blue * k, c.alpha)

private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
    val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}


/** A fall this heavy (mm an hour) draws at full strength; lighter draws from a third. */
private const val FallFullMmPerHour = 4.0

/** The clouds' drift: a floor so a still day still breathes, and the wind's share. */
private const val DriftBase = 1.5f
private const val DriftPerKph = 0.3f

/**
 * The canvas' shader (AGSL). The gradient is drawn here from the palette's three stops, then
 * the scene over it, in this order: stars, cirrus, cumulus, the fall, and last a cap on every
 * pixel's luminance at the palette's brightest stop, which is what keeps §3.6's scrim contract
 * true whatever is drawn.
 *
 * **It works in linear light** (review of the sky, 19 ott 2026): every colour uniform arrives
 * as linear RGB ([bindSky] converts the palette's sRGB stops by the exact transfer curve),
 * every `mix` adds light as light adds, and the pixel is encoded back to sRGB at the very end,
 * with half a level of dither. Mixed as sRGB values, as it was, the middle of a cloud's edge
 * came out darker than the light makes it (luminance 0.537 against 0.558 at half mix over the
 * day band's mid stop): the grey rim of clouds composed in gamma space. And without dither the
 * night band, seventeen levels of red over the whole height, drew in bands up to 110 px wide.
 * The luminance cap is exact here, a dot product on linear values, no longer a 2.2 power.
 *
 * The noise is value noise over a hash, four octaves; the cumulus is domain-warped once so it
 * billows rather than blots, and flattens toward the horizon (the bottom of the canvas) as a
 * cloud deck does in perspective. Coordinates are in dp, so the sky has the same grain on
 * every screen.
 */
internal const val SkyShaderSource = """
uniform float2 uSize;
uniform float uPx;
uniform float uTime;
uniform half4 uTop;
uniform half4 uMid;
uniform half4 uBottom;
uniform float uDeck;
uniform float uDeckTh;
uniform float uVeil;
uniform half4 uLit;
uniform half4 uShade;
uniform float2 uLight;
uniform float uGlow;
uniform float uStars;
uniform float uPlanets;
uniform float uRain;
uniform float uSnow;
uniform float uDrift;
uniform float uTurn;
uniform float uMaxLum;
uniform float2 uBand;
uniform float2 uBody;
uniform float2 uSun;
uniform float uSunOn;
uniform half4 uSunColor;
uniform float4 uSunVeil;
uniform float2 uMoon;
uniform float uMoonOn;
uniform half4 uMoonFace;
uniform float4 uMoonVeil;
uniform float uMoonLimb;
uniform float uLightAmount;
uniform shader uStarMap;
uniform float3 uGalPole;
uniform float3 uGalCentre;
uniform float uMilkyWay;
uniform float2 uStarCam;
uniform float uFog;
uniform half4 uFogColor;
uniform float uStorm;
uniform float2 uLowSun;
uniform float uRays;
uniform float4 uMeteor;
uniform float uMeteorSeed;
uniform float uMoonNorth;

float hash12(float2 p) {
    float3 p3 = fract(float3(p.x, p.y, p.x) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

// Every corner is hashed from its own whole number, taken through floor: the same numbers as
// i + (1, 0) and the rest, but a value the compiler cannot open. Written as i + (1, 0), a GPU
// compiler may turn (i.x + 1) * 0.1031 into i.x * 0.1031 + 0.1031, rounded twice; the cell next
// door hashes the same corner from its own floor, one bit apart, and the hash multiplies that
// bit until, once in a few thousand corners, its last fract falls on the other side of a whole
// number. The corner then has two values and the clouds a crease along the cells' edges: the
// wedge seen over the hero on a device (15 ott 2026), drifting away with the clouds.
float vnoise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float2 u = f * f * (3.0 - 2.0 * f);
    float a = hash12(i);
    float b = hash12(floor(i + float2(1.5, 0.5)));
    float c = hash12(floor(i + float2(0.5, 1.5)));
    float d = hash12(floor(i + float2(1.5, 1.5)));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

// Each octave turned by ~37° so the value noise's grid never lines up from one octave to the
// next: unturned, its axis-aligned cells showed as V-shapes and diagonal streaks.
float fbm(float2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 4; i++) {
        v += a * vnoise(p);
        p = float2(0.8 * p.x - 0.6 * p.y, 0.6 * p.x + 0.8 * p.y) * 2.03 + float2(11.7, 5.3);
        a *= 0.5;
    }
    return v / 0.9375;
}

// Two octaves: the warp only bends the clouds, it carries no detail of its own.
float fbm2(float2 p) {
    return (0.5 * vnoise(p) + 0.25 * vnoise(float2(0.8 * p.x - 0.6 * p.y, 0.6 * p.x + 0.8 * p.y) * 2.03 + float2(11.7, 5.3))) / 0.75;
}

// The same four octaves as fbm, handing back the two- and three-octave sums on the way (each
// normalised like fbm): the deck's light reads the coarse octaves of the field it is drawn
// from, and this costs no sample the deck does not already take.
float fbmParts(float2 p, out float two, out float three) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 4; i++) {
        v += a * vnoise(p);
        if (i == 1) two = v / 0.75;
        if (i == 2) three = v / 0.875;
        p = float2(0.8 * p.x - 0.6 * p.y, 0.6 * p.x + 0.8 * p.y) * 2.03 + float2(11.7, 5.3);
        a *= 0.5;
    }
    return v / 0.9375;
}

// Three octaves, for the sample toward the light.
float fbm3(float2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 3; i++) {
        v += a * vnoise(p);
        p = float2(0.8 * p.x - 0.6 * p.y, 0.6 * p.x + 0.8 * p.y) * 2.03 + float2(11.7, 5.3);
        a *= 0.5;
    }
    return v / 0.875;
}

// One octave on: the turn and the doubling fbm applies between two of its octaves.
float2 oct(float2 p) {
    return float2(0.8 * p.x - 0.6 * p.y, 0.6 * p.x + 0.8 * p.y) * 2.03 + float2(11.7, 5.3);
}

// A sea of the moon: a soft dark patch at c, of radius rad, in the disc's own frame.
float mare(float2 p, float2 c, float rad) {
    float2 d = (p - c) / rad;
    return exp(-dot(d, d));
}

// The deck's shape at any point of the canvas, on its two coarse octaves and unwarped: what the
// sun's shafts are read on (9 nov 2026). The same plane and the same camera as the deck in
// main, turn and drift included; without the warp a cloud's shadow stands up to a third of a
// cell from its cloud, which a shaft averaged over four steps toward the sun does not show,
// and it costs two samples a step where the whole field would cost eight.
float deckCoarse(float2 f) {
    float elev = mix(0.7854, 0.2618, clamp(f.y / uSize.y, -0.3, 1.0));
    float dist = 1.0 / tan(elev);
    float focal = (uSize.y / uPx) / (0.7854 - 0.2618);
    float theta = uTurn / focal;
    float2 cam = float2((f.x / uPx - 0.5 * uSize.x / uPx) * dist / focal, dist);
    float2 ground = float2(cam.x * cos(theta) + cam.y * sin(theta) + uTime * uDrift * 1.732 / focal, cam.y * cos(theta) - cam.x * sin(theta));
    return smoothstep(uDeckTh, uDeckTh + 0.22, fbm2(ground * (focal / (1.732 * 95.0))));
}

// Where a body stands, in px (§8.1c, the arithmetic of SkyBodies.kt): across by bearing inside
// the canvas' width less the inset each side, up by altitude inside the band less the disc.
float2 place(float2 body) {
    float inset = uSize.x * uBody.x;
    float r = uBody.y;
    return float2(inset + (uSize.x - 2.0 * inset) * body.x, uBand.x + r + (uBand.y - 2.0 * r) * (1.0 - body.y));
}

// Where the light stands, in px: a body's place while it is up; below the horizon (up under
// zero, the sun at dusk) from the band's foot down to 40 dp under the canvas, so that the
// clouds' undersides are what faces it.
float2 placeLight(float2 light) {
    if (light.y >= 0.0) return place(light);
    float foot = uBand.x + uBand.y;
    return float2(place(float2(light.x, 0.0)).x, mix(foot, uSize.y + 40.0 * uPx, -light.y));
}

// Linear light back to sRGB, by the exact piecewise curve and not a 2.2 power: at the night
// band's darkest stop (13 of 255) the power curve would print 21, and the night would pale.
half3 encode(half3 c) {
    half3 lo = c * 12.92;
    half3 hi = 1.055 * pow(max(c, half3(0.0)), half3(1.0 / 2.4)) - 0.055;
    return mix(lo, hi, step(half3(0.0031308), c));
}

half4 main(float2 fc) {
    float2 uv = fc / uSize;
    half3 col = uv.y < 0.5
        ? mix(uTop.rgb, uMid.rgb, half(uv.y * 2.0))
        : mix(uMid.rgb, uBottom.rgb, half(uv.y * 2.0 - 1.0));
    float2 q = fc / uPx;
    float persp = mix(1.0, 1.5, uv.y);
    float drift = uTime * uDrift;

    // The low sun's side of the sky (9 nov 2026). A gradient of three stops down the canvas
    // is the same from east to west, and a sky at sunset is not: toward the sun the horizon
    // lights up and warms, away from it it dims. uLowSun is where the sun stands across the
    // canvas (under the horizon too) and how much of this there is: most with the sun between
    // 3° and −2°, nothing over 16° or under −10°. It costs no sample, and the cap at the end
    // holds it like everything else.
    if (uLowSun.y > 0.003) {
        float ax = (fc.x - place(float2(uLowSun.x, 0.0)).x) / uSize.x;
        float lobe = exp(-ax * ax * 2.6);
        float hz = smoothstep(0.2, 1.0, uv.y);
        col *= half(1.0 + uLowSun.y * hz * (lobe * 0.6 - 0.25));
        col = mix(col, col * half3(1.12, 0.97, 0.82), half(uLowSun.y * hz * lobe * 0.6));
    }
    // The turn (25 ott 2026): where the window has been turned by a finger, the veil and what
    // falls, flat sheets, are read uTurn dp further along, the pixels a star moves for the
    // same turn, and the deck, a plane under a camera, is seen by that camera turned (below),
    // so the picture turns as one: a camera turning has no parallax, near or far. The deck's
    // own parallax is the wind's, not the turn's.

    // The Milky Way (20 ott 2026): a band along the galactic plane, where the plane is for this
    // place and hour. The pixel's direction in the stars' camera; its galactic latitude from the
    // pole and its longitude from the centre (two vectors the engine computes once a minute); a
    // Gaussian across the plane, wider and brighter toward the centre (Sagittarius) and narrow
    // and faint toward the anticentre (Auriga), mottled by a noise fixed to the sky, thinner
    // toward the horizon. A drawing of a real thing at its real place, with a model's profile:
    // the rift and the clouds of the real band are not in it. Faint by design, under the stars.
    if (uMilkyWay > 0.003) {
        float alt = uStarCam.x - uv.y * (uStarCam.x - uStarCam.y);
        float focal = uSize.y / (uStarCam.x - uStarCam.y);
        float across = atan((fc.x - 0.5 * uSize.x) / focal);
        float3 dir = float3(sin(across) * cos(alt), sin(alt), cos(across) * cos(alt));
        float b = asin(clamp(dot(dir, uGalPole), -1.0, 1.0));
        float3 gy = cross(uGalPole, uGalCentre);
        float l = atan(dot(dir, gy), dot(dir, uGalCentre));
        float toward = 0.5 + 0.5 * cos(l);
        float sigma = 0.10 + 0.07 * toward;
        float band = exp(-b * b / (2.0 * sigma * sigma));
        float along = 0.3 + 0.7 * pow(toward, 1.5);
        float mottle = 0.8 + 0.4 * fbm2(float2(l, b) * 9.0);
        float low = 1.0 - 0.6 * smoothstep(0.55, 1.0, uv.y);
        float mw = uMilkyWay * band * along * mottle * low;
        col += half3(0.74, 0.80, 0.92) * half(mw * 0.05);
    }

    // The stars that are there (step 6 of the sky review, 19 ott 2026): the texture StarMap
    // draws from the catalogue for this instant and place, one read per pixel; its channels
    // are the star's brightness, its twinkle phase and its warmth, premultiplied by the disc's
    // alpha. Until then the stars were a hash: one in a few cells of 13 dp, all alike.
    if (uStars > 0.003 || uPlanets > 0.003) {
        half4 st = uStarMap.eval(fc);
        float cover = float(st.a);
        if (cover > 0.004) {
            float3 d = float3(st.rgb) / cover;
            // A phase at the top of the channel is a planet (StarField.STEADY_PHASE, 20 ott
            // 2026): a steady light, which is how the eye tells Venus from a star.
            float planet = step(0.985, d.y);
            // A star low in the sky twinkles more than one overhead (9 nov 2026): its light
            // crosses more air. From 14% of its light at the top of the canvas to 40% at the foot.
            float amp = mix(0.14, 0.40, smoothstep(0.35, 1.0, uv.y));
            float twinkle = planet > 0.5 ? 1.0 : 1.0 - amp + amp * sin(uTime * (0.8 + d.y * 1.7) + d.y * 40.0);
            // A planet shows by its own amount: open in the dusk, where the stars are not yet.
            float amount = mix(uStars, uPlanets, planet);
            // Thinner toward the horizon: the air a low star shines through.
            float fade = 1.0 - 0.5 * smoothstep(0.55, 1.0, uv.y);
            // Blue-white to orange, in linear light, by the star's own colour.
            half3 tint = mix(half3(0.72, 0.82, 1.0), half3(1.0, 0.86, 0.62), half(d.z));
            col = mix(col, tint, half(cover * d.x * twinkle * amount * fade));
        }
    }

    // A meteor, on a shower's peak night only (9 nov 2026): uMeteor is the radiant's place in
    // the stars' camera, in px, the seconds between one and the next, and how well it would
    // show (the dark, the open sky, the moon). One trail a period, at a moment of its own in
    // it, starting somewhere in the upper sky and running away from the radiant, which is what
    // makes a shower a shower; 0.8 seconds, a head and a tail that fades. Behind the clouds,
    // like the stars. A drawing of something that happens, not a count: the app prints no rate.
    if (uMeteor.w > 0.003) {
        float period = uMeteor.z;
        float cyc = floor(uTime / period);
        float t0 = 3.0 + (period - 6.0) * hash12(float2(cyc + uMeteorSeed, 5.5));
        float prog = (uTime - cyc * period - t0) / 0.8;
        if (prog > 0.0 && prog < 1.0) {
            float h1 = hash12(float2(cyc + uMeteorSeed, 3.1));
            float h2 = hash12(float2(cyc + uMeteorSeed, 9.7));
            float2 start = float2((0.15 + 0.7 * h1) * uSize.x, (0.08 + 0.35 * h2) * uSize.y);
            float2 away = start - uMeteor.xy;
            float al = length(away);
            float2 along = al > 1.0 ? away / al : float2(0.0, 1.0);
            float2 stop = start + along * (60.0 + 80.0 * h2) * uPx;
            float2 head = mix(start, stop, prog);
            float2 tail = mix(start, stop, max(prog - 0.35, 0.0));
            float2 pa = fc - tail;
            float2 ba = head - tail;
            float hh = clamp(dot(pa, ba) / max(dot(ba, ba), 1.0), 0.0, 1.0);
            float streak = (1.0 - smoothstep(0.0, 1.4 * uPx, length(pa - ba * hh))) * hh * sin(3.14159 * prog);
            col = mix(col, half3(0.9, 0.95, 1.0), half(streak * 0.85 * uMeteor.w));
        }
    }

    // The deck's shape before its colour (19 ott 2026), so that what stands behind it, the sun,
    // the moon, the halo, can be hidden by it pixel by pixel.
    float deckA = 0.0;
    float deck2 = 0.0;
    float deck3 = 0.0;
    float deckN = 0.0;
    float2 deckP = float2(0.0, 0.0);
    // The deck's camera's turn, as a rotation: the light's direction is turned by it below.
    float deckC = 1.0;
    float deckS = 0.0;
    if (uDeck > 0.01) {
        // The deck as a plane at one height seen from below (step 4 of the sky review, 19 ott
        // 2026). The canvas' rows are elevations, 45° at the top down to 15° at the bottom, and
        // a row's ray meets the plane at 1 / tan(elevation) of its height: overhead the clouds
        // are near and big, toward the horizon far, small and flat, together, and they drift
        // across the screen 1.7 times faster at the top than at the bottom, the parallax a
        // deck has. Until then the field was only squashed in height toward the bottom, by
        // half, and the clouds were a wall: the same width at the top and at the bottom. The
        // elevations span the canvas whatever its height, so a taller canvas sees the same sky
        // with a touch wider cells and never a horizon low enough to alias the noise; the
        // scale is the old one at the middle row, 95 dp a cell, so deckThreshold's table holds
        // (measured on 24 canvases: the mean alpha is the deck within 0.021, as before).
        float elev = mix(0.7854, 0.2618, uv.y);
        float dist = 1.0 / tan(elev);
        float focal = (uSize.y / uPx) / (0.7854 - 0.2618);
        // The turn (25 ott 2026, the second cut): the deck's camera turns, by the angle that
        // moves its centre by uTurn dp, the pixels a star moves (its focal length is 1.8 times
        // the stars', 30° on the height against 55°, so the angle is 1.8 times smaller), and
        // the ray's foot on the plane turns with it: a rotation, which stretches nothing and
        // comes back on itself. The first cut read the still picture uTurn dp further along
        // instead, and a plane seen 60° off a pinhole's axis is smeared along the rows: by the
        // second circle the deck was horizontal lines (committente, from the device). The
        // drift is the wind's in the frame the field was laid in, and turns with the plane.
        float theta = uTurn / focal;
        deckC = cos(theta);
        deckS = sin(theta);
        float2 cam = float2((q.x - 0.5 * uSize.x / uPx) * dist / focal, dist);
        float2 ground = float2(cam.x * deckC + cam.y * deckS + drift * 1.732 / focal, cam.y * deckC - cam.x * deckS);
        float2 p = ground * (focal / (1.732 * 95.0));
        // The clouds evolve, they do not only drift (9 nov 2026): the field that bends them
        // walks in time, a hundredth of its cell a second, the two axes opposite ways, so a
        // cloud watched for a minute changes shape as one does. No sample more.
        float2 ev = float2(uTime * 0.010, -uTime * 0.007);
        float2 w = float2(fbm2(p * 0.7 + float2(3.1, 7.7) + ev), fbm2(p * 0.7 + float2(9.4, 1.3) - ev));
        // 0.6 and not more: at 0.9 the warp folded the plane over itself here and there
        // (its Jacobian went negative), and every fold drew a crease drifting with the clouds.
        float2 pw = p + (w - 0.5) * 0.6;
        float n = fbmParts(pw, deck2, deck3);
        // The edge (9 nov 2026). Four octaves stop at a grain of 12 dp and the border was a
        // ramp 0.22 wide: clouds out of focus. Two finer octaves, 5.6 and 2.8 dp, now eat at
        // the border alone (the body keeps its soft folds), by as much as 0.055 either way, and
        // the ramp is 0.14 wide round the same middle, so the share of sky stays the deck's
        // (deckThreshold, measured again on this line). The fine octaves fade with distance:
        // toward the horizon the plane's rows pack them under a pixel, where they would alias.
        float2 p5 = oct(oct(oct(oct(pw))));
        float det = 0.6 * vnoise(p5 + ev * 8.0) + 0.4 * vnoise(oct(p5) - ev * 8.0);
        float fine = clamp(1.6 / dist - 0.4, 0.0, 1.0);
        float border = 1.0 - smoothstep(uDeckTh + 0.04, uDeckTh + 0.34, n);
        // The threshold is Kotlin's (deckThreshold), measured so the clouds take the deck's share.
        float a = smoothstep(uDeckTh + 0.04, uDeckTh + 0.18, n + (det - 0.5) * 0.11 * border * fine);
        deckA = a;
        deckN = n;
        deckP = pw;
    }

    // The band the bodies stand in (§8.1c). Inside it the cap of §3.6 opens from the palette's
    // brightest stop to white, because the band holds no text by construction (SkyColumn): a
    // sun may burn there and a moon be brighter than any sky. A body's glow is a circle round
    // it, fading from its centre to nothing 8 dp past the band's nearer edge (what glowRadius
    // did for the old layer; past the edge it is a fifth of itself at most, under the cap),
    // never a window cut out of the band: cut in height alone, the tail of a noon sun drew a
    // bright stripe across the whole canvas (the shader's first render, 19 ott 2026).
    float bandTop = uBand.x;
    float bandEnd = uBand.x + uBand.y;
    float edge6 = 6.0 * uPx;
    float spill = 8.0 * uPx;
    float open = smoothstep(bandTop - edge6, bandTop + edge6, fc.y) * smoothstep(bandEnd + edge6, bandEnd - edge6, fc.y);
    half3 front = half3(0.0);

    // The lightning (9 nov 2026), on an hour whose state is a thunderstorm: the deck is lit
    // from inside at a place of its own, two pulses a quarter of a second apart and a weaker
    // one three seconds on, in a cycle of six and a half; never three flashes in a second
    // (WCAG 2.3.1), and under the cap like every pixel. No bolt is drawn: where a real one
    // falls is not something the forecast knows.
    float flash = 0.0;
    float2 flashAt = float2(0.0, 0.0);
    if (uStorm > 0.01) {
        float cyc = floor(uTime / 6.5);
        float tt = uTime - cyc * 6.5;
        float late = hash12(float2(cyc, 2.9));
        float f = (tt > 0.6 ? exp(-(tt - 0.6) / 0.10) : 0.0)
            + (tt > 0.86 ? 0.65 * exp(-(tt - 0.86) / 0.16) : 0.0)
            + (tt > 3.9 && late > 0.35 ? 0.5 * exp(-(tt - 3.9) / 0.12) : 0.0);
        flash = min(f, 1.0) * uStorm;
        flashAt = float2((0.15 + 0.7 * hash12(float2(cyc + (tt > 3.5 ? 0.5 : 0.0), 7.3))) * uSize.x, 0.3 * uSize.y);
    }

    // The sun, behind the clouds: a disc at three times the cap, which burns to white, the
    // corona the air scatters round it and the long tail of its aureole, all gone where the
    // deck stands (the veil's share of the light is in uSunVeil, the deck's is this pixel's
    // alpha; a thinner cloud blurs the disc's edge before it hides it). In front of the clouds,
    // added further down: the share of the tail the air between us and the clouds scatters,
    // and the broad, faint patch a cloud lets through, which is all of the sun a closed stratus
    // shows (sunVeil).
    if (uSunOn > 0.5) {
        float2 sp = place(uSun);
        float dSun = length(fc - sp);
        float r = uBody.y;
        float warm = uSunColor.a;
        float room = min(sp.y - bandTop, bandEnd - sp.y) + spill;
        float reach = smoothstep(room, 0.0, dSun);
        float blur = 0.75 * uPx + 5.0 * uPx * deckA;
        float disc = smoothstep(r + blur, r - blur, dSun) * uSunVeil.x;
        float corona = exp(-pow(dSun / (r * (2.0 + 1.5 * warm)), 2.0)) * uSunVeil.y * 1.6;
        float tail = 1.0 / (1.0 + pow(dSun / (r * (6.0 + 4.0 * warm) * uSunVeil.w), 2.0)) * uSunVeil.z;
        col += uSunColor.rgb * half((disc * 3.0 + (corona + tail * 1.5) * reach) * (1.0 - deckA));
        float through = exp(-pow(dSun / (r * 3.0 * uSunVeil.w), 2.0)) * 0.12 * deckA;
        front += uSunColor.rgb * half((tail * 0.6 + through) * reach);
    }

    // The moon, behind the clouds, as a colour over the sky and not light added to it: it
    // reflects. Its phase is moonLitPath's geometry as a mask: the half toward the sun, closed
    // by the terminator, an ellipse of half-width r·|2k − 1| that bulges toward the lit side
    // for a crescent and away from it for a gibbous moon; the dark part at 14% as earthshine.
    // The mask is turned onto the bright limb's angle (step 6, 19 ott 2026): the lit side
    // points at the sun as the place sees it, down toward a sun just set, up toward one not
    // yet risen, the «boat» of a spring dusk; until then the terminator stood vertical.
    if (uMoonOn > 0.5) {
        float2 mp = place(uMoon);
        float2 dm0 = fc - mp;
        float d = length(dm0);
        float r = uBody.y * 0.9;
        float edge = 0.75 * uPx;
        float inside = smoothstep(r + edge, r - edge, d);
        float k = uMoonVeil.w;
        // The screen's y points down, so the lit direction of angle θ is (cos θ, −sin θ).
        float ca = cos(uMoonLimb);
        float sa = sin(uMoonLimb);
        float2 dm = float2(dm0.x * ca - dm0.y * sa, dm0.x * sa + dm0.y * ca);
        float halfLit = smoothstep(-edge, edge, dm.x);
        float rx = max(r * abs(2.0 * k - 1.0), 0.001);
        float e = length(float2(dm.x / rx, dm.y / r));
        float ell = smoothstep(1.0 + edge / r, 1.0 - edge / r, e);
        float lit = k >= 0.5 ? max(halfLit, ell) : halfLit * (1.0 - ell);
        float a = inside * (0.14 + 0.86 * lit) * uMoonVeil.x * (1.0 - deckA);
        // The seas and the limb (9 nov 2026): the disc was one flat tone, a button. Nine soft
        // patches where the seas are with north up (Imbrium, Serenitatis, Tranquillitatis,
        // Crisium, Fecunditatis, Nectaris, Procellarum, Nubium, Humorum, the large ones
        // weighing more), a quarter darker at most, and a limb a sixth darker than the centre; the face turned by where celestial
        // north points at the moon for this place and hour (uMoonNorth), so a moon rising in
        // the east lies on its side as it does. Arithmetic only, and only inside the disc.
        half3 face = uMoonFace.rgb;
        if (inside > 0.0) {
            float2 md = float2(dm0.x, -dm0.y) / r;
            float cn = cos(uMoonNorth);
            float sn = sin(uMoonNorth);
            float2 m = float2(md.x * sn - md.y * cn, md.x * cn + md.y * sn);
            float seas = 1.2 * mare(m, float2(-0.25, 0.50), 0.30) + mare(m, float2(0.28, 0.42), 0.20)
                + 1.1 * mare(m, float2(0.42, 0.12), 0.22) + 0.7 * mare(m, float2(0.78, 0.28), 0.11)
                + 0.7 * mare(m, float2(0.66, -0.10), 0.15) + 0.5 * mare(m, float2(0.46, -0.26), 0.10)
                + 1.1 * mare(m, float2(-0.62, 0.18), 0.34) + 0.8 * mare(m, float2(-0.18, -0.38), 0.22)
                + 0.6 * mare(m, float2(-0.50, -0.38), 0.12);
            float limb = sqrt(max(1.0 - dot(md, md), 0.0));
            face *= half((1.0 - 0.24 * min(seas, 1.0)) * (0.84 + 0.16 * limb));
        }
        col = mix(col, face, half(a));
        float room = min(mp.y - bandTop, bandEnd - mp.y) + spill;
        float reach = smoothstep(room, 0.0, d);
        float glow = exp(-pow(d / (r * 3.0 * uMoonVeil.z), 2.0)) * 0.22 * uMoonVeil.y * k;
        front += uMoonFace.rgb * half(glow * 0.6 * reach);
    }

    // The high layer next, behind the deck: from the ground a cirrus shows through the gaps
    // of the clouds below it, never over them. Streaks stretched 7.5 to 1 while it is
    // scattered; as it closes (cirrostratus) the stretch relaxes to 4 to 3 and the streaks give
    // way to a milky sheet with a soft unevenness, which is what a closed high veil looks like.
    // It drifts at half the deck's speed (19 ott 2026): a cirrus is five to eight times higher
    // than a cumulus and its wind two or three times stronger, so across the sky it moves
    // slower, not faster. At 1.8 the far layer overtook the near one, a parallax inside out.
    if (uVeil > 0.01) {
        float closed = smoothstep(0.6, 1.0, uVeil);
        float2 p = float2((q.x + uTurn + drift * 0.5) * mix(0.0045, 0.009, closed), q.y * mix(0.034, 0.012, closed) * persp);
        float n = fbm(p + float2(0.0, vnoise(p * float2(2.0, 0.6)) * 0.8));
        float streak = smoothstep(mix(0.82, 0.38, uVeil), 1.0, n);
        float sheet = clamp(0.75 + (n - 0.5) * 0.6, 0.0, 1.0);
        float a = mix(streak, sheet, closed) * 0.6;
        col = mix(col, uLit.rgb, half(a));
    }

    if (deckA > 0.002) {
        float2 lightPx = placeLight(uLight);
        float len = max(length(lightPx - fc), 1.0);
        // One direction for the whole canvas, from its middle toward the light. Taken per
        // pixel it turned a full circle round the light, and the lit edge turned with it:
        // rings and arcs drawn into the clouds 150dp around the moon (device, 15 ott 2026).
        float2 across = lightPx - uSize * 0.5;
        float2 dir = length(across) > 1.0 ? normalize(across) : float2(0.0, -1.0);
        // The light of the deck (19 ott 2026, step 3 of the sky review). The edge that faces
        // the light is where the field thins toward it, read on the first three octaves and
        // 0.22 of a cell away: read on all four at 0.16 with a gain of 4, as it was, the fourth
        // octave's lattice combed the clouds along the light. The self-shadow is a cloud
        // thicker toward the light than here, read on the two coarse octaves twice as far:
        // the undersides and the hollows go into shade, which is what gives a cumulus its
        // body. Without a light (a moonless night) the shading flattens to one tone; a deck
        // that closes flattens it further, to soft folds.
        // In the field's own frame: the deck's camera is turned (deckC, deckS), the light's
        // direction on screen is turned with it, or a turned deck would be lit from the side.
        float2 toward = float2(dir.x * deckC + dir.y * deckS, dir.y * deckC - dir.x * deckS) * 0.22;
        float edge = clamp(0.5 + (deck3 - fbm3(deckP + toward)) * 3.0, 0.0, 1.0);
        float occl = max(fbm2(deckP + 2.0 * toward) - deck2, 0.0);
        float shadow = 1.0 - exp(-occl * 7.0);
        float s = edge * (1.0 - 0.55 * shadow);
        s = mix(0.55, s, uLightAmount);
        s = 0.62 + (s - 0.62) * mix(1.0, 0.35, smoothstep(0.55, 1.0, uDeck));
        half3 cloud = mix(uShade.rgb, uLit.rgb, half(s));
        float near = exp(-len / (uSize.x * 0.35));
        float rim = uGlow * near * (1.0 - smoothstep(0.15, 0.7, deckA));
        cloud = mix(cloud, uLit.rgb, half(rim * 0.6));
        // The thick of a cloud and its thin border (9 nov 2026). Where the field stands well
        // over the threshold the cloud is deep and less light comes through, a fifth less at
        // most, which gives a cumulus a dark heart under a lit crown; a closed deck has none,
        // it is one grey already. And the border, where the cloud is half there, lets the sun
        // behind it through: the silver lining, near the sun only and while its disc shows.
        float thick = smoothstep(uDeckTh + 0.10, uDeckTh + 0.55, deckN);
        cloud *= half(1.0 - 0.20 * thick * uLightAmount * (1.0 - smoothstep(0.7, 1.0, uDeck)));
        float thin = deckA * (1.0 - deckA) * 4.0;
        cloud += uSunColor.rgb * half(thin * exp(-len / (uSize.x * 0.30)) * uSunOn * uSunVeil.x * 0.30);
        // The flash, from inside: a patch wider than tall, as a cloud bank lit from within is,
        // fading from its place, and uneven by the deck's own coarse folds, so that it is the
        // cloud that is seen and not a lamp behind it.
        float2 fd = (fc - flashAt) / float2(uSize.x * 0.55, uSize.x * 0.30);
        cloud += half3(0.72, 0.80, 1.0) * half(flash * exp(-length(fd)) * clamp(0.2 + 1.6 * (deck2 - 0.25), 0.0, 1.2) * 0.9);
        col = mix(col, cloud, half(deckA * 0.94));
    }

    // What shines in front of the clouds: the sun's long tail, the patch a thin cloud lets
    // through, the moon's glow where a deck hides its disc.
    col += front;
    // The flash on the air itself, a breath of it over the whole canvas.
    col += half3(0.55, 0.62, 0.85) * half(flash * 0.06);

    // The sun's shafts (9 nov 2026), through a broken deck. From the sun toward this pixel,
    // the deck's coarse shape (deckCoarse) is read at four distances from the sun, 40 to 190
    // dp, or as far as the pixel if it is nearer: what stands round the sun decides the light
    // of the whole line behind it, which is what makes a shaft a line from the sun and not a
    // stain. Where the way is open the air is lit, where a cloud stands on it the air is in
    // its shadow; strongest near the sun, fading away from it. Eight samples, and only on the
    // skies that have shafts (uRays).
    if (uRays > 0.01) {
        float2 sp = place(uSun);
        float2 outward = fc - sp;
        float away = max(length(outward), 1.0);
        float2 ray = outward / away;
        float way = 0.0;
        for (int i = 1; i <= 4; i++) {
            float k = float(i);
            float reachPx = (2.0 + 35.0 * k + 3.0 * k * k) * uPx;
            way += 1.0 - deckCoarse(sp + ray * min(reachPx, away * k / 5.0));
        }
        float shaft = (way * 0.25 - 0.5) * exp(-away / (uSize.x * 0.6)) * uRays * (1.0 - 0.6 * deckA);
        col += uSunColor.rgb * half(max(shaft, 0.0) * 0.40);
        col *= half(1.0 - max(-shaft, 0.0) * 0.35);
    }

    // The fog (9 nov 2026), on an hour whose state is fog. A bank thicker toward the ground,
    // from half its strength at the top of the canvas to all of it four fifths of the way
    // down, uneven by a slow field of its own that drifts and breathes, in the clouds' light
    // without their hue (uFogColor, Kotlin's). Over the sky, the bodies and the deck: fog is
    // the nearest thing there is. Where the sun stands, a pale patch is all that is left of it.
    if (uFog > 0.01) {
        float fn = fbm3(float2((q.x + uTurn) * 0.006 + uTime * 0.012, q.y * 0.011 - uTime * 0.004));
        float depth = smoothstep(-0.2, 0.8, uv.y);
        float fa = uFog * clamp(0.5 + 0.5 * depth + (fn - 0.5) * 0.55, 0.0, 1.0);
        col = mix(col, uFogColor.rgb, half(fa));
        if (uSunOn > 0.5) {
            float dFog = length(fc - place(uSun));
            col += uSunColor.rgb * half(exp(-pow(dFog / (uBody.y * 5.0), 2.0)) * 0.22 * uFog);
        }
    }

    // What falls, on three planes (step 5 of the sky review, 19 ott 2026): near, middle and
    // far, each with its own size, speed and strength, the near one big and fast and the far
    // one small, slow and faint, so the fall has depth. Until then there was one plane: the
    // rain thin scratches crawling at 68 dp a second, the snow one flake a cell of 22 dp,
    // never two close together, a lattice of dots.
    if (uRain > 0.01) {
        // The streaks lean with the wind (a drop goes where the air goes while it falls) and
        // slide along their own line: moving the field down r.y is moving it along the streak.
        float slant = clamp(uDrift * 0.03, -0.35, 0.35);
        float lean = q.x + uTurn - slant * q.y;
        col = mix(col, uShade.rgb, half(uRain * 0.14));
        half3 drop = mix(uLit.rgb, uShade.rgb, 0.3);
        // Rain comes in gusts (9 nov 2026): a slow field, wide as the canvas, by which every
        // plane is stronger here and weaker there, from half to nearly one and a half, moving
        // with the fall; and a mist of it toward the ground. The drops catch the flash.
        float gust = 0.55 + 0.9 * fbm2(float2(lean * 0.005 + uTime * 0.22, q.y * 0.0025 + uTime * 0.08));
        drop += half3(0.6, 0.7, 1.0) * half(flash * 0.5);
        col = mix(col, drop, half(clamp(uRain * 0.10 * gust * smoothstep(0.25, 1.0, uv.y), 0.0, 1.0)));
        // Near: 1.8 dp wide, 40 dp long, 160 dp a second, the fewest.
        float2 r0 = float2(lean * 0.55 + 3.0, q.y * 0.025 - uTime * 4.0);
        float s0 = smoothstep(0.72, 1.0, vnoise(r0)) * smoothstep(0.5, 1.0, vnoise(r0 * 0.37 + 4.0));
        // Middle: what the one plane was, 1.2 dp wide, 28 dp long, 90 dp a second.
        float2 r1 = float2(lean * 0.85, q.y * 0.035 - uTime * 3.2);
        float s1 = smoothstep(0.66, 1.0, vnoise(r1)) * smoothstep(0.45, 1.0, vnoise(r1 * 0.37 + 4.0));
        // Far: 0.8 dp wide, 18 dp long, 55 dp a second, the faintest.
        float2 r2 = float2(lean * 1.3 + 11.0, q.y * 0.055 - uTime * 3.0);
        float s2 = smoothstep(0.62, 1.0, vnoise(r2)) * smoothstep(0.45, 1.0, vnoise(r2 * 0.37 + 4.0));
        col = mix(col, drop, half(clamp(s2 * uRain * 0.45 * gust, 0.0, 1.0)));
        col = mix(col, drop, half(clamp(s1 * uRain * 0.7 * gust, 0.0, 1.0)));
        col = mix(col, drop, half(clamp(s0 * uRain * 0.85 * gust, 0.0, 1.0)));
    }

    if (uSnow > 0.01) {
        // Each plane: cells of its own size with a flake in about two thirds of them, jittered
        // inside the cell, of a size of its own, swaying, falling at its speed and carried
        // sideways by the wind in its share; the far plane first, the near one over it.
        for (int k = 2; k >= 0; k--) {
            float near = 1.0 - float(k) * 0.5;                      // 1 near, 0.5 middle, 0 far
            float cellSize = mix(14.0, 34.0, near);
            float fall = mix(8.0, 22.0, near);
            float size = mix(1.1, 2.6, near);
            float2 sq = float2(q.x + uTurn - uTime * uDrift * mix(0.3, 0.8, near) + float(k) * 7.0, q.y - uTime * fall);
            float2 cell = floor(sq / cellSize);
            float h = hash12(cell + float(k) * 13.1);
            float2 pos = (cell + float2(0.2 + 0.6 * hash12(cell + 2.7), 0.2 + 0.6 * hash12(cell + 9.1))) * cellSize;
            pos.x += sin(uTime * (0.7 + near * 0.5) + h * 30.0) * mix(2.0, 5.0, near);
            float r = size * (0.7 + 0.6 * fract(h * 7.3));
            float flake = 1.0 - smoothstep(r * 0.35, r, length(sq - pos));
            col = mix(col, uLit.rgb, half(flake * uSnow * mix(0.5, 0.9, near) * step(0.33, h)));
        }
        // A fourth plane, right in front of the glass (9 nov 2026): cells of 90 dp with a flake
        // in about half of them, 5 to 9 dp wide and out of focus, falling at 46 dp a second.
        // Few, faint and big, they put the three planes behind them at a distance.
        float2 nq = float2(q.x + uTurn - uTime * uDrift * 1.2 + 31.0, q.y - uTime * 46.0);
        float2 ncell = floor(nq / 90.0);
        float nh = hash12(ncell + 53.7);
        float2 npos = (ncell + float2(0.25 + 0.5 * hash12(ncell + 2.7), 0.25 + 0.5 * hash12(ncell + 9.1))) * 90.0;
        npos.x += sin(uTime * 1.1 + nh * 30.0) * 9.0;
        float nr = 5.0 + 4.0 * fract(nh * 7.3);
        float soft = 1.0 - smoothstep(nr * 0.15, nr, length(nq - npos));
        col = mix(col, uLit.rgb, half(soft * uSnow * 0.30 * step(0.45, nh)));
    }

    // The cap (§3.6), exact: luminance is linear in linear light. Open to white inside the
    // bodies' band, the palette's brightest stop everywhere else; and far over it (a sun's
    // disc, three times the cap) the colour burns to a white of the cap's luminance, as it
    // does in a lens, instead of staying a yellow held down to the cap.
    float capY = mix(uMaxLum, 1.0, open);
    float L = dot(col, half3(0.2126, 0.7152, 0.0722));
    if (L > capY) {
        half3 held = col * half(capY / L);
        col = mix(held, half3(half(capY)), half(smoothstep(1.0, 3.5, L / capY)));
    }
    // Encoded, then dithered by half a level: a fixed pattern per pixel, so it never shimmers,
    // and within what the scrim contract has to spare (5.27:1 measured against 4.5 needed).
    half3 srgb = encode(col) + half((hash12(fc) - 0.5) / 255.0);
    return half4(clamp(srgb, 0.0, 1.0), 1.0);
}
"""

/**
 * The sky, drawn by [SkyShaderSource] (DESIGN §3.9). Where the canvas is not hardware
 * accelerated (a software snapshot) a `RuntimeShader` cannot be drawn at all, and the layer
 * draws the palette's gradient alone, which is what the sky was before the scene.
 *
 * It moves while it may ([moving]): the clouds drift with the wind, the stars twinkle, the
 * rain falls; at 15 frames a second for a sky that only drifts and 30 for a falling one, the
 * rates their own speeds need. When it may not, it is one still frame.
 */
@Composable
fun SkyShaderLayer(
    gradient: SkyGradient,
    scene: SkyScene,
    moving: Boolean,
    modifier: Modifier = Modifier,
    /** Where the sun and the moon stand (§8.1c); null places them in the canvas' upper half,
     * where the scene's light used to be before the band was reported. */
    band: SkyBand? = null,
    /** The sky to draw instead of [gradient] and [scene], asked for while drawing: a canvas
     * that glides from one sky to the next ([SkyTransition]); null draws the two as given. */
    shown: (() -> Sky)? = null
) {
    val shader = remember { RuntimeShader(SkyShaderSource) }
    val brush = remember(shader) { ShaderBrush(shader) }
    val starMap = remember { StarMap() }
    var time by remember { mutableFloatStateOf(0f) }
    // A flash lasts a tenth of a second: a storm draws at the falling rate, rain or not.
    val falling = scene.rain > 0f || scene.snow > 0f || scene.storm > 0f
    val stepMillis = if (falling) 1_000L / FallingFps else 1_000L / DriftFps
    LaunchedEffect(moving, stepMillis) {
        if (!moving) return@LaunchedEffect
        // Where the clock stood when it last stopped, so a scroll that held the sky still does
        // not make it jump when it lets go.
        var origin = 0L
        while (true) {
            withFrameNanos { now ->
                if (origin == 0L) origin = now - (time * 1e9).toLong()
                // Wrapped, so the floats the shader receives keep their precision on a page
                // left open for hours; the wrap is longer than any drift a reader watches.
                time = (((now - origin) / 1e9) % TimeWrapSeconds).toFloat()
            }
            // One frame asked for per step, not one per vsync: between steps the screen is
            // not woken for the sky at all.
            delay(stepMillis)
        }
    }
    Spacer(
        modifier = modifier.drawBehind {
            if (!drawContext.canvas.nativeCanvas.isHardwareAccelerated) {
                drawRect(Brush.verticalGradient((shown?.invoke()?.first ?: gradient).stops()))
                return@drawBehind
            }
            val (gradientNow, sceneNow) = shown?.invoke() ?: (gradient to scene)
            // A sky held still holds no flash and no trail: the clock stands wherever the
            // scroll caught it, and a deck frozen white or a meteor hung in the air is not a
            // still sky. Both come back with the clock.
            val drawn = if (moving) sceneNow else sceneNow.copy(storm = 0f, meteors = 0f)
            val r = BodyRadius.toPx()
            val top = band?.top?.toPx() ?: (size.height * 0.12f - r)
            val height = band?.height?.toPx() ?: (size.height * 0.38f + 2f * r)
            val stars = starMap.shaderFor(drawn.starField, size.width.toInt(), size.height.toInt(), density)
            shader.bindSky(size.width, size.height, density, time, gradientNow, drawn, top, height, stars)
            drawRect(brush)
        }
    )
}

/**
 * Every uniform of [SkyShaderSource], from the canvas' size and density, the scene's clock,
 * the gradient and the scene. One function, so the shader's compile test sets exactly what the
 * screen sets: a uniform renamed in the source and not here fails the test, not the reader.
 */
internal fun RuntimeShader.bindSky(
    width: Float,
    height: Float,
    density: Float,
    time: Float,
    gradient: SkyGradient,
    scene: SkyScene,
    /** The bodies' band, in px from the canvas' top: where it starts and how tall it is. */
    bandTop: Float,
    bandHeight: Float,
    /** The star field as [StarMap] draws it for this canvas, or its empty texture. */
    starMap: android.graphics.Shader
) {
    setFloatUniform("uSize", width, height)
    setFloatUniform("uPx", density)
    setFloatUniform("uTime", time)
    setLinearColor("uTop", gradient.top)
    setLinearColor("uMid", gradient.mid)
    setLinearColor("uBottom", gradient.bottom)
    setFloatUniform("uDeck", scene.deck)
    setFloatUniform("uDeckTh", deckThreshold(scene.deck))
    setFloatUniform("uVeil", scene.veil)
    setLinearColor("uLit", scene.lit)
    setLinearColor("uShade", scene.shade)
    setFloatUniform("uLight", scene.light.x, scene.light.y)
    setFloatUniform("uGlow", scene.glow)
    setFloatUniform("uStars", scene.stars)
    setFloatUniform("uPlanets", scene.planets)
    setFloatUniform("uRain", scene.rain)
    setFloatUniform("uSnow", scene.snow)
    setFloatUniform("uDrift", scene.drift)
    // The turn in dp: through the stars' camera's focal length (`starPlace`), so a turned sheet
    // moves by the pixels the stars move, and the deck's camera by the angle worth them.
    val focal = height / Math.toRadians((StarCameraTopDeg - StarCameraBottomDeg).toDouble())
    setFloatUniform("uTurn", (Math.toRadians(scene.turn.toDouble()) * focal / density).toFloat())
    setFloatUniform("uMaxLum", scene.maxLuminance)
    setFloatUniform("uBand", bandTop, bandHeight)
    setFloatUniform("uBody", EdgeInset, BodyRadius.value * density)
    val sun = scene.sun
    setFloatUniform("uSunOn", if (sun != null) 1f else 0f)
    setFloatUniform("uSun", sun?.across ?: 0f, sun?.up ?: 0f)
    val sunColor = sun?.color ?: Color.Black
    setFloatUniform("uSunColor", toLinear(sunColor.red), toLinear(sunColor.green), toLinear(sunColor.blue), sun?.warmth ?: 0f)
    setFloatUniform("uSunVeil", sun?.disc ?: 0f, sun?.glowCore ?: 0f, sun?.glowMid ?: 0f, sun?.spread ?: 1f)
    val moon = scene.moon
    setFloatUniform("uMoonOn", if (moon != null) 1f else 0f)
    setFloatUniform("uMoon", moon?.across ?: 0f, moon?.up ?: 0f)
    setLinearColor("uMoonFace", SkyPalette.MoonFace)
    setFloatUniform("uMoonVeil", moon?.disc ?: 0f, moon?.glow ?: 0f, moon?.spread ?: 1f, moon?.illumination ?: 0f)
    setFloatUniform("uMoonLimb", Math.toRadians((moon?.limbAngleDeg ?: 0f).toDouble()).toFloat())
    setFloatUniform("uLightAmount", scene.lightAmount)
    setInputShader("uStarMap", starMap)
    val frame = scene.galactic
    setFloatUniform("uGalPole", frame?.poleX ?: 0f, frame?.poleY ?: 1f, frame?.poleZ ?: 0f)
    setFloatUniform("uGalCentre", frame?.centreX ?: 0f, frame?.centreY ?: 0f, frame?.centreZ ?: 1f)
    setFloatUniform("uMilkyWay", if (frame != null) scene.milkyWay else 0f)
    setFloatUniform(
        "uStarCam",
        Math.toRadians(StarCameraTopDeg.toDouble()).toFloat(),
        Math.toRadians(StarCameraBottomDeg.toDouble()).toFloat()
    )
    setFloatUniform("uFog", scene.fog)
    setLinearColor("uFogColor", scene.fogColor)
    setFloatUniform("uStorm", scene.storm)
    setFloatUniform("uLowSun", scene.sunAcross, scene.lowSun)
    setFloatUniform("uRays", scene.rays)
    // The radiant in the stars' camera, in px, off the canvas too: a trail runs away from it
    // wherever it stands. Beside the camera the tangent runs off, so the bearing is held at 80°.
    val across = Math.toRadians(scene.radiant.x.toDouble().coerceIn(-80.0, 80.0))
    val span = StarCameraTopDeg - StarCameraBottomDeg
    setFloatUniform(
        "uMeteor",
        (width / 2.0 + Math.tan(across) * focal).toFloat(),
        height * ((StarCameraTopDeg - scene.radiant.y) / span),
        MeteorPeriodSeconds,
        scene.meteors
    )
    setFloatUniform("uMeteorSeed", scene.meteorSeed)
    setFloatUniform("uMoonNorth", Math.toRadians((moon?.northAngleDeg ?: 90f).toDouble()).toFloat())
}

/** A meteor about every this many seconds on a peak night under a dark, open sky: «one every
 * so often», which is what a reader sees of a strong shower from a town, and no rate printed. */
internal const val MeteorPeriodSeconds = 45f

/** A colour as a plain float4 uniform in linear light: the shader mixes linear values and
 * encodes the pixel itself, so the colour pipeline never touches these. */
private fun RuntimeShader.setLinearColor(name: String, c: Color) =
    setFloatUniform(name, toLinear(c.red), toLinear(c.green), toLinear(c.blue), c.alpha)

private const val DriftFps = 15
private const val FallingFps = 30
private const val TimeWrapSeconds = 3_600.0
