package com.callbackdev.chiaro.ui.today

import com.callbackdev.chiaro.domain.AlertEngine
import com.callbackdev.chiaro.domain.model.WeatherReport
import java.time.LocalDateTime

/**
 * What the line at the top of Today says, before it says it in any language. The
 * renderer turns each case into one localized sentence; **null is a real answer** —
 * a day with nothing to warn about gets no line, never a filler ("Enjoy your day!"
 * is the exact species of invention the screen must not commit).
 */
sealed interface Headline {

    /** A severe hour ahead (the same table the notifier uses — one definition of
     * "severe" in the whole app). */
    data class Severe(val bucket: AlertEngine.SevereBucket, val at: LocalDateTime) : Headline

    /** Dry now, wet later: the umbrella sentence. [clearsAt] is the first hour the
     * chance drops back under half, when the forecast shows one. */
    data class WetSoon(
        val at: LocalDateTime,
        val pct: Int,
        val snow: Boolean,
        val clearsAt: LocalDateTime?
    ) : Headline

    /** Already wet: when it should stop, or the honest "not today". */
    data class WetNow(val stopsAt: LocalDateTime?, val snow: Boolean) : Headline
}

/**
 * The sentence at the top of Today (VISION §3.3.2), computed and testable with no
 * Android in sight. Thresholds are [AlertEngine]'s own: the sentence and the
 * notification must never disagree about what counts as "rain coming".
 *
 * Input contract: [report] is already trimmed by `WeatherRecency`, so `hourly[0]` is
 * the hour we are in.
 */
object HeadlineEngine {

    /** Codes where water is falling right now — drizzle through thunderstorm. */
    private val WET_CODES = setOf(
        51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 71, 73, 75, 77,
        80, 81, 82, 85, 86, 95, 96, 99
    )
    private val SNOW_CODES = setOf(71, 73, 75, 77, 85, 86)

    /** Under half, the sky has stopped promising rain: the "clear after that" bar. */
    private const val CLEAR_BELOW_PCT = 50

    /** How far past its trigger a sentence keeps looking for the turn it promises. */
    private const val TURN_LOOKAHEAD_HOURS = 12

    fun headline(report: WeatherReport, now: LocalDateTime): Headline? {
        val hours = report.hourly
        if (hours.isEmpty()) return null

        val severeEnd = now.plusHours(AlertEngine.SEVERE_LOOKAHEAD_HOURS)
        val severe = hours.firstOrNull {
            !it.time.isBefore(now) && !it.time.isAfter(severeEnd) &&
                it.condition.wmoCode in AlertEngine.SevereCodes
        }
        if (severe != null) {
            return Headline.Severe(
                bucket = AlertEngine.SevereCodes.getValue(severe.condition.wmoCode),
                at = severe.time
            )
        }

        val current = hours.first()
        if (current.condition.wmoCode in WET_CODES) {
            val stopsAt = hours.asSequence()
                .drop(1)
                .takeWhile { it.time.isBefore(now.plusHours(TURN_LOOKAHEAD_HOURS.toLong())) }
                .firstOrNull {
                    it.condition.wmoCode !in WET_CODES && it.precipChancePct < CLEAR_BELOW_PCT
                }
            return Headline.WetNow(
                stopsAt = stopsAt?.time,
                snow = current.condition.wmoCode in SNOW_CODES
            )
        }

        val soonEnd = now.plusHours(AlertEngine.PRECIP_LOOKAHEAD_HOURS)
        val wetHour = hours.firstOrNull {
            !it.time.isBefore(now) && !it.time.isAfter(soonEnd) &&
                it.precipChancePct >= AlertEngine.PRECIP_THRESHOLD_PCT
        } ?: return null
        val clearsAt = hours.asSequence()
            .filter { it.time.isAfter(wetHour.time) }
            .takeWhile { it.time.isBefore(wetHour.time.plusHours(TURN_LOOKAHEAD_HOURS.toLong())) }
            .firstOrNull { it.precipChancePct < CLEAR_BELOW_PCT }
        return Headline.WetSoon(
            at = wetHour.time,
            pct = wetHour.precipChancePct,
            snow = wetHour.condition.wmoCode in SNOW_CODES,
            clearsAt = clearsAt?.time
        )
    }
}
