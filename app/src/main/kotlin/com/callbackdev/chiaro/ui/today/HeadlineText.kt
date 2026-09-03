package com.callbackdev.chiaro.ui.today

import android.content.Context
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.AlertEngine
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * The headline sentence in words, on top of [HeadlineEngine]'s language-free answer.
 * A plain function on a [Context] so the Today screen and the Today widget (Fase 8)
 * speak from the same mapping — two copies of "umbrella around 17:00" would drift.
 */
object HeadlineText {

    fun of(context: Context, headline: Headline?, timeFmt: DateTimeFormatter): String? {
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
            is Headline.WetSoon -> when {
                headline.snow && headline.clearsAt != null -> context.getString(
                    R.string.headline_snow_soon_clearing, t(headline.at), t(headline.clearsAt)
                )
                headline.snow -> context.getString(R.string.headline_snow_soon, t(headline.at))
                headline.clearsAt != null -> context.getString(
                    R.string.headline_wet_soon_clearing, t(headline.at), t(headline.clearsAt)
                )
                else -> context.getString(R.string.headline_wet_soon, t(headline.at))
            }
            is Headline.WetNow -> when {
                headline.snow && headline.stopsAt != null ->
                    context.getString(R.string.headline_snow_now_stopping, t(headline.stopsAt))
                headline.snow -> context.getString(R.string.headline_snow_now)
                headline.stopsAt != null ->
                    context.getString(R.string.headline_wet_now_stopping, t(headline.stopsAt))
                else -> context.getString(R.string.headline_wet_now)
            }
        }
    }
}
