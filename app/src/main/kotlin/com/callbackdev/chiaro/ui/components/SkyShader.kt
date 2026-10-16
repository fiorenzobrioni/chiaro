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
 * - [light], where the light comes from, as a fraction of the canvas: the sun's place by day,
 *   the moon's at night, the edges of the clouds that face it are the bright ones;
 * - [stars], 0..1: a dark sky, a clear one and no moon to wash it out;
 * - [rain], [snow], 0..1: only while the hour is wet by the state engine, by its intensity;
 * - [drift], the clouds' speed across the canvas in dp a second, signed: the wind's, from the
 *   direction it blows toward as the reader facing the equator sees it.
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
    val maxLuminance: Float
)

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
    southern: Boolean,
    cloudPct: Int,
    layers: CloudLayers?,
    precipPct: Int,
    fall: SkyFall,
    fallMmPerHour: Double,
    windKph: Double,
    windFromDeg: Int
): SkyScene {
    val cover = (cloudPct.coerceIn(0, 100) / 100f)
    val (deck, veil) = cloudDecks(cloudPct, layers)
    val opacity = cloudOpacity(cloudPct, layers, fall)
    val maxLum = palette.brightestBottomStop().luminance()
    val day = smoothstep(-6f, 6f, sunAltitudeDeg.toFloat())
    val moonUp = moonAltitudeDeg > 0.0
    val moonLight = if (moonUp) (moonIllumination.toFloat() * smoothstep(0f, 25f, moonAltitudeDeg.toFloat())) else 0f
    // Lit by day: a white with a breath of the sky in it. In the golden hour the light is the
    // horizon's, the gradient's own bottom stop. At night the clouds are the moon's grey, a
    // little lighter than the sky behind them under a bright moon and barely there without one.
    val dayLit = lerp(Color.White, gradient.mid, 0.12f)
    val golden = smoothstep(14f, 2f, sunAltitudeDeg.toFloat()) * day
    val warmLit = lerp(dayLit, gradient.bottom, 0.65f * golden)
    val nightLit = lerp(gradient.mid, SkyPalette.CloudNight, 0.25f + 0.45f * moonLight)
    val lit = capLuminance(lerp(nightLit, warmLit, day), maxLum)
    val wet = (precipPct.coerceIn(0, 100) / 100f)
    // A closed deck is grey (device, 16 ott 2026): the underside of a stratus is lit by the
    // cloud itself, not by the blue above it, and a shade still drawn toward the sky's top
    // stop put blue hollows into a sky the hero called overcast. The hue goes as the deck
    // closes, the luminance stays, so the deck keeps its folds and the cap still holds.
    val toned = lerp(lit, gradient.top, 0.45f + 0.25f * (1f - day)).let { scale(it, 1f - 0.30f * wet) }
    val shade = capLuminance(lerp(toned, grey(toned), DeckGrey * smoothstep(0.55f, 1f, deck)), maxLum)
    val (lightAz, lightAlt) = if (day > 0.5f || !moonUp) sunAzimuthDeg to sunAltitudeDeg else moonAzimuthDeg to moonAltitudeDeg
    val light = Offset(
        bodyAcross(lightAz, southern),
        // The bodies' band is the upper half of the canvas: high is near its top.
        (0.12f + 0.38f * (1f - bodyUp(lightAlt))).coerceIn(0f, 1f)
    )
    val glow = if (day > 0.5f) day * (1f - 0.6f * opacity) else 0.6f * moonLight * (1f - 0.6f * opacity)
    val darkness = smoothstep(-10f, -18f, sunAltitudeDeg.toFloat())
    val stars = darkness * (1f - cover) * (1f - cover) * (1f - 0.65f * moonLight)
    val intensity = (0.35f + 0.65f * (fallMmPerHour / FallFullMmPerHour).toFloat()).coerceIn(0.35f, 1f)
    // Toward where the wind blows, as seen facing the equator: in the north the right of the
    // screen is the west, in the south the east (the same frame the sun and moon stand in).
    val toward = Math.toRadians((windFromDeg + 180.0).mod(360.0))
    val across = if (southern) sin(toward) else -sin(toward)
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
        maxLuminance = maxLum
    )
}

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
 * by layer (16 ott 2026). A layer's cloud is not equally thick: a low deck (stratus,
 * stratocumulus) hides the sun, a middle one (altostratus) shows it «as through ground glass»
 * when it shows it at all, a high one (cirrus, cirrostratus) never stops it from casting
 * shadows, in the words of the WMO's cloud atlas. The README's recorded Milan says the same in
 * the provider's own numbers: UV 0.75 under 100% low cloud, 4.45 under 92% high, against 4.55
 * on the clear day before. Each layer counts by [MidOpacity] and [HighOpacity] of its share,
 * the low one in full, and they combine like the cover does. A wet hour takes the light of its
 * whole cover, because what falls comes from a thick cloud whichever layer the model files it
 * under (an anvil is high). With no layers it is the cover, which is what it was before.
 */
fun cloudOpacity(cloudPct: Int, layers: CloudLayers?, fall: SkyFall): Float {
    val cover = cloudPct.coerceIn(0, 100) / 100f
    val l = layers?.takeIf { (it.lowPct ?: 0) + (it.midPct ?: 0) + (it.highPct ?: 0) > 0 }
        ?: return cover
    val layered = 1f - (1f - share(l.lowPct)) * (1f - MidOpacity * share(l.midPct)) *
        (1f - HighOpacity * share(l.highPct))
    return if (fall != SkyFall.NONE) cover else minOf(layered, cover)
}

private fun share(pct: Int?): Float = (pct ?: 0).coerceIn(0, 100) / 100f

/** What a middle and a high layer take of the light, against a low one's whole. */
private const val MidOpacity = 0.8f
private const val HighOpacity = 0.3f

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
 */
internal fun deckThreshold(deck: Float): Float {
    val x = deck.coerceIn(0f, 1f) * (DeckThresholds.size - 1)
    val i = x.toInt().coerceAtMost(DeckThresholds.size - 2)
    return DeckThresholds[i] + (DeckThresholds[i + 1] - DeckThresholds[i]) * (x - i)
}

private val DeckThresholds = floatArrayOf(
    0.930f, 0.620f, 0.574f, 0.541f, 0.514f, 0.490f, 0.468f, 0.448f, 0.428f, 0.409f, 0.390f,
    0.371f, 0.352f, 0.332f, 0.311f, 0.290f, 0.265f, 0.238f, 0.205f, 0.159f, -0.250f
)

/** How far a closed deck's shade goes toward a grey of its own luminance. */
private const val DeckGrey = 0.8f

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

private fun toLinear(c: Float): Float =
    if (c <= 0.04045f) c / 12.92f else Math.pow(((c + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()

private fun toSrgb(s: Float): Float =
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
 * The canvas' shader (AGSL). The gradient is drawn here exactly as the palette's three stops
 * (the same interpolation `Brush.verticalGradient` uses), then the scene over it, in this
 * order: stars, cirrus, cumulus, the fall, and last a cap on every pixel's luminance at the
 * palette's brightest stop, which is what keeps §3.6's scrim contract true whatever is drawn.
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
uniform float uRain;
uniform float uSnow;
uniform float uDrift;
uniform float uMaxLum;

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

float lum(half3 c) {
    half3 l = pow(max(c, half3(0.0)), half3(2.2));
    return dot(l, half3(0.2126, 0.7152, 0.0722));
}

half4 main(float2 fc) {
    float2 uv = fc / uSize;
    half3 col = uv.y < 0.5
        ? mix(uTop.rgb, uMid.rgb, half(uv.y * 2.0))
        : mix(uMid.rgb, uBottom.rgb, half(uv.y * 2.0 - 1.0));
    float2 q = fc / uPx;
    float persp = mix(1.0, 1.5, uv.y);
    float drift = uTime * uDrift;

    if (uStars > 0.003) {
        float cellSize = 13.0;
        float2 cell = floor(q / cellSize);
        float h = hash12(cell);
        if (h > 0.88) {
            float2 pos = (cell + float2(hash12(cell + 7.1), hash12(cell + 3.3))) * cellSize;
            float dist = length(q - pos);
            float twinkle = 0.75 + 0.25 * sin(uTime * (0.8 + h * 1.7) + h * 40.0);
            float size = 0.55 + (h - 0.88) * 6.0;
            float b = (1.0 - smoothstep(0.0, size, dist)) * twinkle;
            float fade = 1.0 - smoothstep(0.30, 0.80, uv.y);
            col = mix(col, half3(0.93, 0.95, 1.0), half(b * uStars * fade));
        }
    }

    // The high layer first, behind the deck: from the ground a cirrus shows through the gaps
    // of the clouds below it, never over them. Streaks stretched 7.5 to 1 while it is
    // scattered; as it closes (cirrostratus) the stretch relaxes to 4 to 3 and the streaks give
    // way to a milky sheet with a soft unevenness, which is what a closed high veil looks like.
    if (uVeil > 0.01) {
        float closed = smoothstep(0.6, 1.0, uVeil);
        float2 p = float2((q.x + drift * 1.8) * mix(0.0045, 0.009, closed), q.y * mix(0.034, 0.012, closed) * persp);
        float n = fbm(p + float2(0.0, vnoise(p * float2(2.0, 0.6)) * 0.8));
        float streak = smoothstep(mix(0.82, 0.38, uVeil), 1.0, n);
        float sheet = clamp(0.75 + (n - 0.5) * 0.6, 0.0, 1.0);
        float a = mix(streak, sheet, closed) * 0.6;
        col = mix(col, uLit.rgb, half(a));
    }

    if (uDeck > 0.01) {
        float2 p = float2((q.x + drift) * 0.0105, q.y * 0.0105 * persp);
        float2 w = float2(fbm2(p * 0.7 + float2(3.1, 7.7)), fbm2(p * 0.7 + float2(9.4, 1.3)));
        // 0.6 and not more: at 0.9 the warp folded the plane over itself here and there
        // (its Jacobian went negative), and every fold drew a crease drifting with the clouds.
        float2 pw = p + (w - 0.5) * 0.6;
        float n = fbm(pw);
        // The threshold is Kotlin's (deckThreshold), measured so the clouds take the deck's share.
        float a = smoothstep(uDeckTh, uDeckTh + 0.22, n);
        if (a > 0.002) {
            float2 lightPx = uLight * uSize;
            float len = max(length(lightPx - fc), 1.0);
            // One direction for the whole canvas, from its middle toward the light. Taken per
            // pixel it turned a full circle round the light, and the lit edge turned with it:
            // rings and arcs drawn into the clouds 150dp around the moon (device, 15 ott 2026).
            float2 across = lightPx - uSize * 0.5;
            float2 dir = length(across) > 1.0 ? normalize(across) : float2(0.0, -1.0);
            float n2 = fbm(pw + dir * 0.16);
            float s = clamp(0.55 + (n - n2) * 4.0, 0.0, 1.0);
            // A deck is one grey with soft folds, not a sky of lit puffs: the shading flattens
            // as the cover closes.
            s = 0.62 + (s - 0.62) * mix(1.0, 0.35, smoothstep(0.55, 1.0, uDeck));
            half3 cloud = mix(uShade.rgb, uLit.rgb, half(s));
            float near = exp(-len / (uSize.x * 0.35));
            float rim = uGlow * near * (1.0 - smoothstep(0.15, 0.7, a));
            cloud = mix(cloud, uLit.rgb, half(rim * 0.6));
            col = mix(col, cloud, half(a * 0.94));
        }
    }

    if (uRain > 0.01) {
        float2 r = float2((q.x + q.y * 0.17) * 0.85, q.y * 0.035 - uTime * 2.4);
        float streak = smoothstep(0.66, 1.0, vnoise(r)) * smoothstep(0.45, 1.0, vnoise(r * 0.37 + 4.0));
        col = mix(col, uShade.rgb, half(uRain * 0.14));
        col = mix(col, mix(uLit.rgb, uShade.rgb, 0.3), half(streak * uRain * 0.75));
    }

    if (uSnow > 0.01) {
        float cellSize = 22.0;
        float2 sq = float2(q.x, q.y - uTime * 11.0);
        float2 cell = floor(sq / cellSize);
        float h = hash12(cell);
        float2 pos = (cell + float2(0.2 + 0.6 * hash12(cell + 2.7), 0.2 + 0.6 * hash12(cell + 9.1))) * cellSize;
        pos.x += sin(uTime * 0.9 + h * 30.0) * 3.0;
        float flake = 1.0 - smoothstep(0.6, 1.8, length(sq - pos));
        col = mix(col, uLit.rgb, half(flake * uSnow * 0.85 * step(0.25, h)));
    }

    float L = lum(col);
    if (L > uMaxLum) {
        col = col * half(pow(uMaxLum / L, 1.0 / 2.2));
    }
    return half4(col, 1.0);
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
    modifier: Modifier = Modifier
) {
    val shader = remember { RuntimeShader(SkyShaderSource) }
    val brush = remember(shader) { ShaderBrush(shader) }
    var time by remember { mutableFloatStateOf(0f) }
    val falling = scene.rain > 0f || scene.snow > 0f
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
                drawRect(Brush.verticalGradient(gradient.stops()))
                return@drawBehind
            }
            shader.bindSky(size.width, size.height, density, time, gradient, scene)
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
    scene: SkyScene
) {
    setFloatUniform("uSize", width, height)
    setFloatUniform("uPx", density)
    setFloatUniform("uTime", time)
    setColor("uTop", gradient.top)
    setColor("uMid", gradient.mid)
    setColor("uBottom", gradient.bottom)
    setFloatUniform("uDeck", scene.deck)
    setFloatUniform("uDeckTh", deckThreshold(scene.deck))
    setFloatUniform("uVeil", scene.veil)
    setColor("uLit", scene.lit)
    setColor("uShade", scene.shade)
    setFloatUniform("uLight", scene.light.x, scene.light.y)
    setFloatUniform("uGlow", scene.glow)
    setFloatUniform("uStars", scene.stars)
    setFloatUniform("uRain", scene.rain)
    setFloatUniform("uSnow", scene.snow)
    setFloatUniform("uDrift", scene.drift)
    setFloatUniform("uMaxLum", scene.maxLuminance)
}

/** A colour as a plain float4 uniform: the shader's colours are sRGB values, like the
 * gradient brush's, never converted by the colour pipeline. */
private fun RuntimeShader.setColor(name: String, c: Color) =
    setFloatUniform(name, c.red, c.green, c.blue, c.alpha)

private const val DriftFps = 15
private const val FallingFps = 30
private const val TimeWrapSeconds = 3_600.0
