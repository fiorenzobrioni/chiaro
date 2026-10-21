package com.callbackdev.chiaro.notifications

import android.content.Context
import android.graphics.Bitmap
import com.callbackdev.chiaro.data.AppPalette
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.model.WeatherReport
import com.callbackdev.chiaro.widget.window.WindowPainter
import com.callbackdev.chiaro.widget.window.WindowSky
import java.time.Instant
import java.time.ZoneId

/**
 * The sky reminder's picture (21 ott 2026): the scene of the moment it is about, as the
 * window widget and Oggi's hero draw it — the gradient of that hour's light, the moon with
 * its phase and its tilt, the stars that are out, Venus and Jupiter among them, the clouds
 * the forecast gives the hour, and the names of the brightest things where they stand.
 *
 * On 23 set 2026 the sky reminder was given no picture, «a word and a number» being all a
 * verdict is. That was true of the pictures the app had then, charts of quantities; since
 * 20 ott 2026 the app's own picture of the sky exists, is the hero of the page the reminder
 * opens and the whole of the window card, and a reminder about the golden hour or a clear
 * night that showed the sky of that hour is not decorating the verdict: it is showing the
 * thing the verdict is about, computed from the same forecast the verdict read.
 *
 * One bitmap and not a pair: the scene has no day and night dress to follow the shade
 * with, it is a picture of the sky, the same on either ground. Painted for the moment
 * ([at]), not for now — the reminder arrives before its moment, and it is the moment's sky
 * the reader is being told about. Where the GPU does not answer the painter's plain
 * drawing stands in, as on the card; where the forecast has no hour for the moment there
 * is no picture, because the clouds in it would be nobody's forecast.
 */
internal object SkyPictures {

    /** The picture's size in px: the charts' width, and a panorama a notification has room for. */
    const val WIDTH = NotificationCharts.WIDTH
    const val HEIGHT = 330

    /** The dp a px stands for in a notification's picture (~292 dp of width at 1000 px). */
    private const val PictureDensity = WIDTH / 292f

    fun scene(
        context: Context,
        report: WeatherReport,
        coords: Coordinates,
        zone: ZoneId,
        at: Instant,
        palette: AppPalette,
        followed: Set<String>,
        gpu: Boolean = true
    ): Bitmap? {
        val sky = WindowSky.forHour(report, coords, zone, at, palette, followed) ?: return null
        return WindowPainter.paintSky(
            context, sky, WIDTH, HEIGHT, HEIGHT, at, PictureDensity,
            names = true,
            gpu = gpu
        ).bitmap
    }
}
