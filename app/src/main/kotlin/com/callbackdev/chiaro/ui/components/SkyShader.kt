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
 * - [cirrus], the high layer's share of that cover, turns puffs into veils;
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
    val cirrus: Float,
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
    val shade = capLuminance(
        lerp(lit, gradient.top, 0.45f + 0.25f * (1f - day)).let { scale(it, 1f - 0.30f * wet) },
        maxLum
    )
    val (lightAz, lightAlt) = if (day > 0.5f || !moonUp) sunAzimuthDeg to sunAltitudeDeg else moonAzimuthDeg to moonAltitudeDeg
    val light = Offset(
        bodyAcross(lightAz, southern),
        // The bodies' band is the upper half of the canvas: high is near its top.
        (0.12f + 0.38f * (1f - bodyUp(lightAlt))).coerceIn(0f, 1f)
    )
    val glow = if (day > 0.5f) day * (1f - 0.6f * cover) else 0.6f * moonLight * (1f - 0.6f * cover)
    val darkness = smoothstep(-10f, -18f, sunAltitudeDeg.toFloat())
    val stars = darkness * (1f - cover) * (1f - cover) * (1f - 0.65f * moonLight)
    val layered = layers?.let { l ->
        val low = (l.lowPct ?: 0) + (l.midPct ?: 0)
        val high = l.highPct ?: 0
        if (low + high == 0) 0f else high.toFloat() / (low + high)
    } ?: 0f
    val intensity = (0.35f + 0.65f * (fallMmPerHour / FallFullMmPerHour).toFloat()).coerceIn(0.35f, 1f)
    // Toward where the wind blows, as seen facing the equator: in the north the right of the
    // screen is the west, in the south the east (the same frame the sun and moon stand in).
    val toward = Math.toRadians((windFromDeg + 180.0).mod(360.0))
    val across = if (southern) sin(toward) else -sin(toward)
    val speed = DriftBase + DriftPerKph * windKph.toFloat()
    val drift = (if (abs(across) < 0.15) 0.15 * (if (across < 0) -1 else 1) else across).toFloat() * speed
    return SkyScene(
        cover = cover,
        cirrus = layered,
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

/** A colour no brighter than [max] in relative luminance, its hue kept. */
internal fun capLuminance(color: Color, max: Float): Color {
    val l = color.luminance()
    if (l <= max || l <= 0f) return color
    // Luminance is linear in the linear channels; scale those, then back to sRGB. Half a
    // percent under the cap, because an sRGB `Color` is stored at 8 bits a channel and the
    // rounding may go up.
    val k = max * CapMargin / l
    fun channel(c: Float): Float {
        val lin = if (c <= 0.04045f) c / 12.92f else Math.pow(((c + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
        val s = lin * k
        return if (s <= 0.0031308f) s * 12.92f else (1.055 * Math.pow(s.toDouble(), 1 / 2.4) - 0.055).toFloat()
    }
    return Color(channel(color.red), channel(color.green), channel(color.blue), color.alpha)
}

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
 * order: stars, cumulus, cirrus, the fall, and last a cap on every pixel's luminance at the
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
uniform float uCover;
uniform float uCirrus;
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

float vnoise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float2 u = f * f * (3.0 - 2.0 * f);
    float a = hash12(i);
    float b = hash12(i + float2(1.0, 0.0));
    float c = hash12(i + float2(0.0, 1.0));
    float d = hash12(i + float2(1.0, 1.0));
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

    if (uCover > 0.01) {
        float2 p = float2((q.x + drift) * 0.0105, q.y * 0.0105 * persp);
        float2 w = float2(fbm2(p * 0.7 + float2(3.1, 7.7)), fbm2(p * 0.7 + float2(9.4, 1.3)));
        float2 pw = p + (w - 0.5) * 0.9;
        float n = fbm(pw);
        float th = mix(0.80, 0.16, uCover);
        float a = smoothstep(th, th + 0.22, n) * (1.0 - uCirrus * 0.75);
        if (a > 0.002) {
            float2 lightPx = uLight * uSize;
            float2 toLight = lightPx - fc;
            float len = max(length(toLight), 1.0);
            float2 dir = toLight / len;
            float n2 = fbm(pw + dir * 0.16);
            float s = clamp(0.55 + (n - n2) * 4.0, 0.0, 1.0);
            // A deck is one grey with soft folds, not a sky of lit puffs: the shading flattens
            // as the cover closes.
            s = 0.62 + (s - 0.62) * mix(1.0, 0.35, smoothstep(0.55, 1.0, uCover));
            half3 cloud = mix(uShade.rgb, uLit.rgb, half(s));
            float near = exp(-len / (uSize.x * 0.35));
            float rim = uGlow * near * (1.0 - smoothstep(0.15, 0.7, a));
            cloud = mix(cloud, uLit.rgb, half(rim * 0.6));
            col = mix(col, cloud, half(a * 0.94));
        }
    }

    if (uCirrus > 0.01 && uCover > 0.01) {
        float2 p = float2((q.x + drift * 1.8) * 0.0045, q.y * 0.034 * persp);
        float n = fbm(p + float2(0.0, vnoise(p * float2(2.0, 0.6)) * 0.8));
        float a = smoothstep(mix(0.82, 0.38, uCover), 1.0, n) * uCirrus * 0.6;
        col = mix(col, uLit.rgb, half(a));
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
    setFloatUniform("uCover", scene.cover)
    setFloatUniform("uCirrus", scene.cirrus)
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
