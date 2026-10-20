package com.callbackdev.chiaro.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.roundToInt
import kotlin.math.tan

/**
 * The star field as a texture the sky's shader samples (DESIGN §3.9, step 6 of the sky review,
 * 19 ott 2026). A shader that looped over a few hundred stars for every pixel would spend
 * more on a clear night than the clouds cost; drawn once into a bitmap of the canvas' size,
 * and again only when the stars or the size change (the stars move with the minute tick), a
 * star costs the shader one texture read per pixel. Each star is a soft disc whose colour
 * channels carry data, not colour: red its brightness, green its twinkle phase, blue its
 * warmth, and the alpha the disc's own falloff; the shader reads them back and draws the
 * star in linear light, twinkling on its own phase.
 */
internal class StarMap {
    private var key: Triple<List<StarMark>, Int, Int>? = null
    private var bitmap: Bitmap? = null
    private var shader: Shader? = null
    private val empty: Shader by lazy {
        BitmapShader(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    }

    /** The texture for [stars] on a canvas of [widthPx] × [heightPx]; an empty one when there
     * are no stars, so the shader's input is always bound. */
    fun shaderFor(stars: List<StarMark>, widthPx: Int, heightPx: Int, density: Float): Shader {
        if (stars.isEmpty() || widthPx <= 0 || heightPx <= 0) return empty
        val wanted = Triple(stars, widthPx, heightPx)
        shader?.let { if (wanted == key) return it }
        val target = bitmap?.takeIf { it.width == widthPx && it.height == heightPx }
            ?: Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888).also { bitmap = it }
        target.eraseColor(0)
        drawStars(Canvas(target), stars, widthPx, heightPx, density)
        key = wanted
        return BitmapShader(target, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).also { shader = it }
    }
}

/**
 * The stars' camera (§3.9): the elevation of the canvas' top row and of its bottom row,
 * degrees. Wider than the deck's 45° to 15° (20 ott 2026): the deck's window held 15 stars on
 * an October evening in Milan, and a sky is not a keyhole. Nobody compares a cloud with a
 * star, so the two cameras may differ; the band the sun and the moon stand in (§8.1c) is a
 * third frame again, compressed, and a star's distance from the moon on the screen is not
 * the sky's. Do not «fix» one onto the other.
 */
internal const val StarCameraTopDeg = 65f
internal const val StarCameraBottomDeg = 10f

/**
 * Where a star lands on the canvas, in px, or null off it: the stars' camera, the rows
 * elevations from [StarCameraTopDeg] down to [StarCameraBottomDeg] and the focal length that
 * fits them in the height, the azimuth offset through `tan` as a pinhole does, about ±29° on
 * a phone. Facing the equator, like the deck and the bodies.
 */
internal fun starPlace(star: StarMark, widthPx: Int, heightPx: Int): Pair<Float, Float>? {
    val span = StarCameraTopDeg - StarCameraBottomDeg
    val focal = heightPx / Math.toRadians(span.toDouble())
    val x = widthPx / 2.0 + tan(Math.toRadians(star.acrossDeg.toDouble())) * focal
    val y = heightPx * ((StarCameraTopDeg - star.altitudeDeg) / span)
    if (x < -8 || x > widthPx + 8 || y < -8 || y > heightPx + 8) return null
    return x.toFloat() to y
}

/** The disc a star draws: its radius in dp by its brightness. */
internal fun starRadiusDp(brightness: Float): Float = 0.8f + 2.2f * brightness

internal fun drawStars(canvas: Canvas, stars: List<StarMark>, widthPx: Int, heightPx: Int, density: Float) {
    val soft = Paint(Paint.ANTI_ALIAS_FLAG)
    val speck = Paint(Paint.ANTI_ALIAS_FLAG)
    stars.forEach { star ->
        val (x, y) = starPlace(star, widthPx, heightPx) ?: return@forEach
        val r = starRadiusDp(star.brightness) * density
        // The three channels alone: `Color.rgb` sets the alpha to 0xFF, and an `or` over it
        // left every stop opaque (found 20 ott 2026); the alpha is the disc and is set below.
        val data = Color.rgb(
            (star.brightness * 255f).roundToInt().coerceIn(0, 255),
            (star.phase * 255f).roundToInt().coerceIn(0, 255),
            (star.warmth * 255f).roundToInt().coerceIn(0, 255)
        ) and 0x00FFFFFF
        if (star.brightness < DustBrightness) {
            // The dust: a plain speck, so a few hundred of them a minute cost nothing.
            speck.color = data or (0xB0 shl 24)
            canvas.drawCircle(x, y, r * 0.7f, speck)
        } else {
            soft.shader = RadialGradient(
                x, y, r,
                intArrayOf(data or (0xFF shl 24), data or (0xA0 shl 24), data),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(x, y, r, soft)
        }
    }
}

/** Under this brightness a star is one of the dust and drawn as a speck, not a soft disc. */
private const val DustBrightness = 0.2f
