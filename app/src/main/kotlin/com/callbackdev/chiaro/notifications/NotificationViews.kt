package com.callbackdev.chiaro.notifications

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.callbackdev.chiaro.R

/**
 * What every Chiaro notification wears besides its words (23 set 2026, notification
 * review): the brand's accent on the small icon and the header, and — where
 * [NotificationCharts] has something worth drawing — an expanded body with the picture in
 * it.
 *
 * The picture goes in a custom expanded view, and the [NotificationCompat.BigTextStyle]
 * stays on the notification all the same: it is what a watch, a car, a lock screen that
 * does not inflate custom views and every test read, and it keeps EVERY line, where the
 * custom body keeps the five that fit under the picture. The two say the same thing; the
 * picture only says part of it faster.
 */
internal object NotificationViews {

    /**
     * **Quiet hours** (23 set 2026, notification review): between [QuietFrom] and
     * [QuietUntil] on the phone's own clock a notification still arrives — in the shade,
     * with its whole text, ready for the morning — but without sound or vibration.
     *
     * Until then a rain warning polled at 03:00 for rain at 07:00 rang the phone at three
     * in the morning, on the default channel, for something nobody could act on before
     * getting up; so did a storm six hours out, and a rule of the reader's reading the
     * night's frost. The phone's clock and not the place's, because it is the reader who
     * sleeps: a saved place abroad has its own night, and it is not theirs.
     *
     * Two things still ring: a RED official warning ([urgent]), the one level the
     * Dipartimento means as «act now», and the sky reminders, which do not come through
     * here at all — a reader who asked to be told about the meteor peak at 02:00 asked to
     * be woken.
     */
    fun quietAtNight(
        builder: NotificationCompat.Builder,
        urgent: Boolean = false,
        now: java.time.LocalTime = java.time.LocalTime.now()
    ) {
        if (!urgent && isQuiet(now)) builder.setSilent(true)
    }

    fun isQuiet(now: java.time.LocalTime): Boolean = now >= QuietFrom || now < QuietUntil

    val QuietFrom: java.time.LocalTime = java.time.LocalTime.of(22, 0)
    val QuietUntil: java.time.LocalTime = java.time.LocalTime.of(7, 0)

    /** The brand's accent, from the icon's own ring: the vivid primary. */
    fun accent(context: Context): Int = context.getColor(R.color.notification_accent)

    /**
     * Sets the expanded body on [builder] when there is a [chart]; leaves the standard
     * big-text body alone when there is not. [description] is the picture's text
     * equivalent for a screen reader: what it shows, in one sentence.
     */
    fun expandWithChart(
        context: Context,
        builder: NotificationCompat.Builder,
        title: String,
        headline: String,
        details: List<String>,
        chart: Bitmap?,
        description: String
    ) {
        if (chart == null) return
        val views = RemoteViews(context.packageName, R.layout.notification_expanded).apply {
            setTextViewText(R.id.notif_title, title)
            setTextViewText(R.id.notif_headline, headline)
            setImageViewBitmap(R.id.notif_chart, chart)
            setContentDescription(R.id.notif_chart, description)
            if (details.isEmpty()) {
                setViewVisibility(R.id.notif_details, View.GONE)
            } else {
                setTextViewText(R.id.notif_details, details.joinToString("\n"))
            }
        }
        builder.setCustomBigContentView(views)
    }
}
