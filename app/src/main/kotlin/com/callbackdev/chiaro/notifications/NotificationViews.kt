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
