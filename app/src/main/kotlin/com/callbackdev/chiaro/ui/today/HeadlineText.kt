package com.callbackdev.chiaro.ui.today

import android.content.Context
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.AlertEngine
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * The headline sentence in words, on top of [HeadlineEngine]'s language-free answer.
 * A plain function on a [Context] so the Today screen and the two widgets that print
 * it (Fase 8) speak from the same mapping — two copies of "umbrella around 17:00"
 * would drift.
 *
 * [brief] is the same sentence for a card that has three lines of fourteen characters
 * (the Now widget, 8 set 2026): the umbrella keeps its hour and drops the clearing
 * clause, and "rain should stop around 17:00" becomes "rain until about 17:00". Same
 * fact, same hour, same hedge; it lives HERE rather than in the widget so the two
 * registers can never disagree about which sentence a forecast earns.
 */
object HeadlineText {

    fun of(
        context: Context,
        headline: Headline?,
        timeFmt: DateTimeFormatter,
        brief: Boolean = false
    ): String? {
        fun t(at: LocalDateTime): String = at.format(timeFmt)
        return when (headline) {
            null -> null
            is Headline.Severe -> context.getString(
                when (headline.bucket) {
                    AlertEngine.SevereBucket.THUNDER -> R.string.headline_severe_thunder
                    AlertEngine.SevereBucket.ICE -> R.string.headline_severe_ice
                    AlertEngine.SevereBucket.RAIN -> R.string.headline_severe_rain
                    AlertEngine.SevereBucket.SNOW -> R.string.headline_severe_snow
                },
                t(headline.at)
            )
            is Headline.WetSoon -> {
                val clearsAt = headline.clearsAt?.takeUnless { brief }
                when {
                    headline.snow && clearsAt != null -> context.getString(
                        R.string.headline_snow_soon_clearing, t(headline.at), t(clearsAt)
                    )
                    headline.snow -> context.getString(R.string.headline_snow_soon, t(headline.at))
                    clearsAt != null -> context.getString(
                        R.string.headline_wet_soon_clearing, t(headline.at), t(clearsAt)
                    )
                    else -> context.getString(R.string.headline_wet_soon, t(headline.at))
                }
            }
            is Headline.WetNow -> when {
                headline.snow && headline.stopsAt != null -> context.getString(
                    if (brief) R.string.headline_snow_now_stopping_brief
                    else R.string.headline_snow_now_stopping,
                    t(headline.stopsAt)
                )
                headline.snow -> context.getString(R.string.headline_snow_now)
                headline.stopsAt != null -> context.getString(
                    if (brief) R.string.headline_wet_now_stopping_brief
                    else R.string.headline_wet_now_stopping,
                    t(headline.stopsAt)
                )
                else -> context.getString(R.string.headline_wet_now)
            }
        }
    }
}
