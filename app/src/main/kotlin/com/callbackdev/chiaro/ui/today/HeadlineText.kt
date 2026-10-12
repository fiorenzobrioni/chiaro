package com.callbackdev.chiaro.ui.today

import android.content.Context
import androidx.annotation.StringRes
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.AlertEngine
import com.callbackdev.chiaro.domain.settings.UnitSettings
import com.callbackdev.chiaro.ui.format.Formats
import com.callbackdev.chiaro.ui.warnings.WarningText
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

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
 *
 * [units] arrived with the frost and the wind (9 set 2026): a minimum and a gust are
 * numbers with a unit, and the unit is the reader's.
 */
object HeadlineText {

    fun of(
        context: Context,
        headline: Headline?,
        timeFmt: DateTimeFormatter,
        units: UnitSettings,
        brief: Boolean = false
    ): String? {
        val locale = Locale.getDefault()
        fun t(at: LocalDateTime): String = at.format(timeFmt)
        return when (headline) {
            null -> null
            // Step zero (Fase 11): an authority's orange or red, in the same shape in
            // both registers — the brief one drops the day, which the widget's own row
            // does not have room for, and keeps the level and what it is for.
            is Headline.Official -> {
                val phrase = context.getString(WarningText.phraseRes(headline.level))
                val hazards = WarningText.hazards(context, headline.hazards)
                if (brief) {
                    context.getString(R.string.headline_warning_brief, phrase, hazards)
                } else {
                    context.getString(
                        if (headline.today) R.string.headline_warning_today
                        else R.string.headline_warning_tomorrow,
                        phrase,
                        hazards
                    )
                }
            }
            is Headline.Severe -> context.getString(
                when (headline.bucket) {
                    AlertEngine.SevereBucket.THUNDER -> R.string.headline_severe_thunder
                    AlertEngine.SevereBucket.ICE -> R.string.headline_severe_ice
                    AlertEngine.SevereBucket.RAIN -> R.string.headline_severe_rain
                    AlertEngine.SevereBucket.SNOW -> R.string.headline_severe_snow
                },
                t(headline.at)
            )
            is Headline.WetNow -> {
                val falling = headline.falling
                val stopsAt = headline.stopsAt
                if (stopsAt != null) {
                    context.getString(
                        if (brief) {
                            falling.pick(
                                R.string.headline_wet_now_stopping_brief,
                                R.string.headline_snow_now_stopping_brief,
                                R.string.headline_mixed_now_stopping_brief
                            )
                        } else {
                            falling.pick(
                                R.string.headline_wet_now_stopping,
                                R.string.headline_snow_now_stopping,
                                R.string.headline_mixed_now_stopping
                            )
                        },
                        t(stopsAt)
                    )
                } else {
                    context.getString(
                        if (brief) {
                            falling.pick(
                                R.string.headline_wet_now_brief,
                                R.string.headline_snow_now_brief,
                                R.string.headline_mixed_now_brief
                            )
                        } else {
                            falling.pick(R.string.headline_wet_now, R.string.headline_snow_now, R.string.headline_mixed_now)
                        }
                    )
                }
            }
            is Headline.WetSoon -> {
                val clearsAt = headline.clearsAt?.takeUnless { brief }
                if (clearsAt != null) {
                    context.getString(
                        headline.falling.pick(
                            R.string.headline_wet_soon_clearing,
                            R.string.headline_snow_soon_clearing,
                            R.string.headline_mixed_soon_clearing
                        ),
                        t(headline.at), t(clearsAt)
                    )
                } else {
                    context.getString(
                        headline.falling.pick(
                            R.string.headline_wet_soon, R.string.headline_snow_soon, R.string.headline_mixed_soon
                        ),
                        t(headline.at)
                    )
                }
            }
            is Headline.Frost -> {
                val low = Formats.temperature(headline.minC, units.temperature, locale)
                if (brief) context.getString(R.string.headline_frost_brief, t(headline.at))
                else context.getString(R.string.headline_frost, t(headline.at), low)
            }
            is Headline.Fog -> context.getString(R.string.headline_fog, t(headline.at))
            is Headline.Wind -> {
                // The bigger of the two is the number that matters; a gust under the
                // steady wind is not a gust worth naming.
                val top = Formats.wind(maxOf(headline.speedKph, headline.gustKph), units.windSpeed, locale)
                val at = headline.at
                if (at == null) {
                    context.getString(
                        if (brief) R.string.headline_wind_brief else R.string.headline_wind, top
                    )
                } else {
                    context.getString(
                        if (brief) R.string.headline_wind_later_brief else R.string.headline_wind_later,
                        top, t(at)
                    )
                }
            }
            is Headline.WetMaybe -> context.getString(
                headline.falling.pick(
                    R.string.headline_wet_maybe, R.string.headline_snow_maybe, R.string.headline_mixed_maybe
                ),
                t(headline.at)
            )
            is Headline.WetTomorrow -> context.getString(
                headline.falling.pick(
                    R.string.headline_wet_tomorrow, R.string.headline_snow_tomorrow, R.string.headline_mixed_tomorrow
                ),
                t(headline.at)
            )
            // A quiet day's sentence (12 ott 2026) is the screen's, not the cards': the
            // brief register keeps its own fallback, the sky right now, which is what a
            // card with three short lines is for.
            is Headline.Calm -> if (brief) null else calm(context, headline, ::t, units, locale)
        }
    }

    /** «Prossima pioggia domenica, massima 23° verso le 16:00»: the dry spell, then the
     * warmth, either alone when the other is missing. */
    private fun calm(
        context: Context,
        headline: Headline.Calm,
        t: (LocalDateTime) -> String,
        units: UnitSettings,
        locale: Locale
    ): String? {
        val dry = when (val d = headline.dry) {
            null -> null
            is Headline.Calm.Dry.Until -> context.getString(
                d.falling.pick(R.string.headline_next_rain, R.string.headline_next_snow, R.string.headline_next_mixed),
                if (d.daysAhead == 1) {
                    context.getString(R.string.headline_next_tomorrow)
                } else {
                    d.date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
                }
            )
            is Headline.Calm.Dry.Days ->
                context.resources.getQuantityString(R.plurals.headline_dry_days, d.days, d.days)
        }
        fun deg(c: Double) = Formats.temperature(c, units.temperature, locale)
        val warmth = when (val w = headline.warmth) {
            null -> null
            is Headline.Calm.Warmth.Peak -> context.getString(R.string.headline_peak, deg(w.tempC), t(w.at))
            is Headline.Calm.Warmth.Tomorrow ->
                context.getString(R.string.headline_tomorrow_range, deg(w.lowC), deg(w.highC))
        }
        return when {
            dry != null && warmth != null -> context.getString(
                R.string.headline_calm_pair,
                dry,
                warmth.replaceFirstChar { it.lowercase(locale) }
            )
            else -> dry ?: warmth
        }
    }

    /** The sentence for what falls: rain, snow, or both (25 set 2026). */
    @StringRes
    private fun Headline.Falling.pick(@StringRes rain: Int, @StringRes snow: Int, @StringRes mixed: Int): Int =
        when (this) {
            Headline.Falling.RAIN -> rain
            Headline.Falling.SNOW -> snow
            Headline.Falling.MIXED -> mixed
        }
}
