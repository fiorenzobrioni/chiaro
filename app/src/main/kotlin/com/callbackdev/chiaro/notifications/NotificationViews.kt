package com.callbackdev.chiaro.notifications

import android.content.Context
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
 * custom body keeps the lines that fit under the picture — whole lines, measured before
 * the post by [NotificationFit] (21 ott 2026), never a line cut by the view. The two say
 * the same thing; the picture only says part of it faster.
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
     * equivalent for a screen reader: what it shows, in one sentence. [details] are the
     * lines offered for the space under the picture, worth-first: the ones that fit are
     * drawn ([NotificationFit.fit]), and a line the picture already draws is not among them
     * — the notifier keeps that one for the long text alone.
     *
     * Both pictures go in: the layout shows the day one and hides the night one, and its
     * `-night` resources the reverse, so the system picks the picture for the shade it is
     * drawing on at the moment it draws — see [NotificationCharts] for why the app cannot.
     */
    fun expandWithChart(
        context: Context,
        builder: NotificationCompat.Builder,
        title: String,
        headline: String,
        /** Each line in its forms, long first: the picture's body takes the first that
         * holds one line ([NotificationFit.fitForms]). */
        details: List<List<String>>,
        chart: NotificationCharts.Themed?,
        description: String
    ) {
        if (chart == null) return
        val views = RemoteViews(context.packageName, R.layout.notification_expanded).apply {
            setTextViewText(R.id.notif_title, title)
            setTextViewText(R.id.notif_headline, headline)
            setImageViewBitmap(R.id.notif_chart_day, chart.light)
            setImageViewBitmap(R.id.notif_chart_night, chart.dark)
            setContentDescription(R.id.notif_chart_day, description)
            setContentDescription(R.id.notif_chart_night, description)
            // The lines that fit under this picture on this phone, and the view's net set to
            // the same number: a budget the view does not know is a budget it cannot keep.
            val budget = NotificationFit.detailBudget(context, headline, chart.light.height, chart.light.width)
            val shown = NotificationFit.fitForms(context, details, budget)
            if (shown.isEmpty()) {
                setViewVisibility(R.id.notif_details, View.GONE)
            } else {
                setInt(R.id.notif_details, "setMaxLines", budget)
                setTextViewText(R.id.notif_details, shown.joinToString("\n"))
            }
        }
        builder.setCustomBigContentView(views)
    }
}
