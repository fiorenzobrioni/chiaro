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
 * Where a star lands on the canvas, in px, or null off it: the deck's camera (§3.9), the rows
 * elevations from [SkyCameraTopDeg] down to [SkyCameraBottomDeg] and the focal length that
 * fits them in the height, the azimuth offset through `tan` as a pinhole does. The same frame
 * the clouds are drawn in, so a star and a cloud sit in one sky.
 */
internal fun starPlace(star: StarMark, widthPx: Int, heightPx: Int): Pair<Float, Float>? {
    val span = SkyCameraTopDeg - SkyCameraBottomDeg
    val focal = heightPx / Math.toRadians(span.toDouble())
    val x = widthPx / 2.0 + tan(Math.toRadians(star.acrossDeg.toDouble())) * focal
    val y = heightPx * ((SkyCameraTopDeg - star.altitudeDeg) / span)
    if (x < -8 || x > widthPx + 8 || y < -8 || y > heightPx + 8) return null
    return x.toFloat() to y
}

/** The disc a star draws: its radius in dp by its brightness. */
internal fun starRadiusDp(brightness: Float): Float = 0.8f + 2.2f * brightness

internal fun drawStars(canvas: Canvas, stars: List<StarMark>, widthPx: Int, heightPx: Int, density: Float) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    stars.forEach { star ->
        val (x, y) = starPlace(star, widthPx, heightPx) ?: return@forEach
        val r = starRadiusDp(star.brightness) * density
        val data = Color.rgb(
            (star.brightness * 255f).roundToInt().coerceIn(0, 255),
            (star.phase * 255f).roundToInt().coerceIn(0, 255),
            (star.warmth * 255f).roundToInt().coerceIn(0, 255)
        )
        paint.shader = RadialGradient(
            x, y, r,
            intArrayOf(data or (0xFF shl 24), data or (0xA0 shl 24), data and 0x00FFFFFF),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(x, y, r, paint)
    }
}
