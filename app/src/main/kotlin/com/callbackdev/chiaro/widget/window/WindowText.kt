package com.callbackdev.chiaro.widget.window

import android.content.Context
import android.text.format.DateFormat
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.sky.Planet
import com.callbackdev.chiaro.ui.format.Formats
import com.callbackdev.chiaro.ui.sky.SkyText
import com.callbackdev.chiaro.widget.WidgetModel
import com.callbackdev.chiaro.widget.sentence
import java.time.Instant
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Every word the window prints, built off a [Context] so the card and the configuration
 * screen's preview say the same things. One sentence under the sky: at night the Sky
 * tab's own verdict for tonight with the numbers that decided it, or, when a bright moon
 * leaves no dark window, the fact of the moon; by day the next moment of light with its
 * verdict, as the Sky card names it; and, with no moment ahead, the day's sentence.
 */
internal object WindowText {

    /** The sentence at the foot of the picture. */
    fun caption(context: Context, model: WidgetModel, night: Boolean, at: Instant): String {
        val content = model.content ?: return ""
        if (night) tonight(context, model)?.let { return it }
        nextMoment(context, model, at)?.let { return it }
        return sentence(context, content, model.settings.units)
    }

    /** Tonight, as the Sky tab judges it: null when the state has nothing to say about it. */
    fun tonight(context: Context, model: WidgetModel): String? {
        val tonight = model.tonight ?: return null
        val moon = tonight.moonIlluminationPct
        val verdict = tonight.verdict
        if (verdict != null) {
            val word = context.getString(SkyText.verdictWordRes(verdict.kind))
            val cloud = verdict.cloudPct
            return when {
                cloud != null && moon != null -> context.getString(R.string.window_tonight, word, cloud, moon)
                cloud != null -> context.getString(R.string.window_tonight_cloud, word, cloud)
                else -> context.getString(R.string.window_tonight_word, word)
            }
        }
        // No dark window tonight: the moon takes it. The facts, then.
        if (moon != null) {
            val cloud = tonight.hours.takeIf { it.isNotEmpty() }?.map { it.cloudPct }?.average()?.roundToInt()
            return if (cloud != null) {
                context.getString(R.string.window_moonlit_cloud, moon, cloud)
            } else {
                context.getString(R.string.window_moonlit, moon)
            }
        }
        return null
    }

    /** «Golden hour at 18:36 · Great», or «Sunrise tomorrow at 07:14» past midnight. */
    fun nextMoment(context: Context, model: WidgetModel, at: Instant): String? {
        val next = model.moments.firstOrNull() ?: return null
        val zone = model.zone
        val name = context.getString(SkyText.shortNameRes(next.job.id))
        val clock = next.start.atZone(zone).format(
            Formats.timeFormatter(DateFormat.is24HourFormat(context), Locale.getDefault())
        )
        val tomorrow = next.start.atZone(zone).toLocalDate() != at.atZone(zone).toLocalDate()
        val line = context.getString(
            if (tomorrow) R.string.window_next_tomorrow_at else R.string.window_next_at, name, clock
        )
        val verdict = next.verdict?.let { context.getString(SkyText.verdictWordRes(it.kind)) }
        return if (verdict != null) context.getString(R.string.window_next_verdict, line, verdict) else line
    }

    /** «Looking SE»: where the window faces, so the stars can be checked against the sky. */
    fun looking(context: Context, facingDeg: Double): String =
        context.getString(R.string.window_looking, context.getString(SkyText.bearingRes(facingDeg)))

    /** The moon's name on the sky, with its phase: «Moon 99%». */
    fun moonLabel(context: Context, illuminationPct: Int): String =
        context.getString(R.string.window_label_moon, illuminationPct)

    fun planetLabel(context: Context, planet: Planet): String = context.getString(
        when (planet) {
            Planet.VENUS -> R.string.window_label_venus
            Planet.JUPITER -> R.string.window_label_jupiter
        }
    )
}
