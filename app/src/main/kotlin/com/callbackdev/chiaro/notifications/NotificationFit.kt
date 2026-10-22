package com.callbackdev.chiaro.notifications

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.text.StaticLayout
import android.text.TextPaint
import android.util.TypedValue
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * What fits in a notification's body, decided BEFORE it is posted (21 ott 2026, device
 * report: the expanded summary ended in «…» on a Samsung S24 Ultra).
 *
 * The pictured body (`notification_expanded.xml`) gives its lines one `TextView` with
 * `maxLines` and `ellipsize="end"`, and that cap counts the VISUAL lines after wrapping.
 * A `RemoteViews` cannot measure, so when the lines ran past the cap the platform cut
 * whatever followed and replaced the end of the last visible line with an ellipsis: the
 * sixth line of a rainy morning vanished and the fifth lost its tail; the evening's two
 * long lines did it on their own; an official warning's note, a paragraph, did it every
 * time and took the licence's attribution line with it. Measured in Italian at 360, 384
 * and 411 dp and three font scales: the dry morning at the default size was the one case
 * that fitted everywhere.
 *
 * The notifier does know two things the view does not: the phone's width and the reader's
 * font scale. So the lines are flowed here with a `StaticLayout` at the notification's own
 * type size, in the width the system's template leaves a custom body, and only the whole
 * lines that fit go in — the same method the widgets use to decide what a card carries
 * (`nowRowMarkerAsideFits`, «In words»' one-line check). A line that does not fit is left
 * out whole, never cut; the ones after it are left out too, because the order is worth-first
 * and skipping ahead to a shorter line would betray it. The view keeps its `maxLines` as a
 * net under an estimate, and the long text (`BigTextStyle`) keeps every line for the
 * surfaces that do not inflate custom views.
 */
internal object NotificationFit {

    /**
     * What the decorated template takes around a custom body, in dp: the shade's side
     * margins (12 and 12) and the content's start (52) and end (16), stock Android 12 to
     * 16. Probe of 21 ott 2026: 292 dp of text at a 384 dp screen.
     */
    const val TemplateMarginsDp = 92f

    /**
     * The share of the stock width the body is flowed in. A maker's font (One UI's) is wider
     * than Roboto and its margins are not narrower, and the estimate must err on the side
     * of a line left out rather than a line cut. An assumption, said as one: a device
     * screenshot is what confirms it.
     */
    const val MakerShare = 0.92f

    /**
     * The most visual lines the details may take under a picture, whatever the room: the
     * view's own `maxLines` net (`notification_expanded`), and the ceiling's arithmetic
     * ([detailBudget]) never asks for more.
     */
    const val MaxDetailLines = 6

    /** The platform clips an expanded custom view at this height (DESIGN §8.17). */
    const val CeilingDp = 256f

    /**
     * The body around the details, in dp at font scale 1, as `notification_expanded` lays
     * it out: the title's line, one line of the headline, the air above and below the
     * picture. The same figures `NotificationChartsTest` does the ceiling's arithmetic with.
     */
    const val TitleDp = 24f
    const val HeadlineLineDp = 20f
    const val DetailLineDp = 19f
    const val PictureGapsDp = 14f

    /** The notification's text size, `TextAppearance.Compat.Notification`. */
    const val TextSp = 14f

    /** The phone's width in portrait, dp. */
    private fun screenDp(context: Context): Int {
        val config = context.resources.configuration
        return config.smallestScreenWidthDp.takeIf { it > 0 } ?: config.screenWidthDp
    }

    /** What the template leaves a custom body on this phone, dp: the picture spans it. */
    fun contentWidthDp(context: Context): Float = (screenDp(context) - TemplateMarginsDp).coerceAtLeast(1f)

    /** The width a line of the body is flowed in, for this phone and this reader. */
    fun textWidthPx(context: Context): Int {
        val dp = contentWidthDp(context) * MakerShare
        return (dp * context.resources.displayMetrics.density).roundToInt().coerceAtLeast(1)
    }

    /**
     * How many visual lines of details fit under a picture of [pictureHeightPx] by
     * [pictureWidthPx] before the body meets the platform's ceiling, for this reader's font
     * scale and this [headline]'s own lines. Until 21 ott 2026 the body took five lines
     * whatever the scale, and at 1.3 five lines and a day chart ran to about 290 dp: the
     * system clipped the last of them at the bottom, with no ellipsis to say so.
     */
    fun detailBudget(context: Context, headline: String, pictureHeightPx: Int, pictureWidthPx: Int): Int {
        val scale = context.resources.configuration.fontScale
        val pictureDp = pictureHeightPx * contentWidthDp(context) / pictureWidthPx.coerceAtLeast(1)
        val headlineLines = lines(headline, paint(context), textWidthPx(context)).coerceIn(1, 2)
        val room = CeilingDp - TitleDp * scale - HeadlineLineDp * scale * headlineLines - PictureGapsDp - pictureDp
        return floor(room / (DetailLineDp * scale)).toInt().coerceIn(0, MaxDetailLines)
    }

    /** The body's type: the notification's size at the reader's font scale, the default face. */
    fun paint(context: Context): TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, TextSp, context.resources.displayMetrics
        )
        typeface = Typeface.DEFAULT
    }

    /** The visual lines [text] takes at [widthPx] in [paint]. */
    fun lines(text: CharSequence, paint: TextPaint, widthPx: Int): Int =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, widthPx)
            .setIncludePad(true)
            .build()
            .lineCount

    /**
     * The longest head of [candidates] whose lines add up to at most [budget] visual lines:
     * whole lines, in their order, and nothing past the first that does not fit.
     */
    fun fit(context: Context, candidates: List<String>, budget: Int): List<String> =
        fitForms(context, candidates.map { listOf(it) }, budget)

    /**
     * Each candidate in the first of its forms that holds one line, the long form when
     * none does (22 ott 2026, from the device: «Tramonto 18:25 · 3 minuti di luce in meno
     * di ieri» wrapped at 384 dp and took the air's line with it). The forms are the
     * line's own words shortened, long first; the long text's body keeps the long one.
     * Then as [fit]: worth-first, the first line that does not fit closes the list.
     */
    fun fitForms(context: Context, candidates: List<List<String>>, budget: Int): List<String> {
        if (candidates.isEmpty()) return emptyList()
        val paint = paint(context)
        val width = textWidthPx(context)
        var used = 0
        val kept = ArrayList<String>(candidates.size)
        for (forms in candidates) {
            require(forms.isNotEmpty()) { "a line with no form" }
            val chosen = forms.firstOrNull { lines(it, paint, width) <= 1 } ?: forms.first()
            val lines = lines(chosen, paint, width)
            if (used + lines > budget) break
            used += lines
            kept += chosen
        }
        return kept
    }

    /**
     * The first of [candidates] that fits in [lines] visual lines, or the last one when none
     * does: a sentence is offered longest first, and the shortest form is the fallback that
     * is said whatever the width.
     */
    fun firstThatFits(context: Context, candidates: List<String>, lines: Int = 1): String {
        require(candidates.isNotEmpty()) { "nothing to fit" }
        val paint = paint(context)
        val width = textWidthPx(context)
        return candidates.firstOrNull { lines(it, paint, width) <= lines } ?: candidates.last()
    }
}
