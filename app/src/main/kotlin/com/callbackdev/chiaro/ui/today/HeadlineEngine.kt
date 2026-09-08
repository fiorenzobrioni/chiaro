package com.callbackdev.chiaro.ui.today

import com.callbackdev.chiaro.domain.AlertEngine
import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.model.WeatherReport
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * What the line at the top of Today says, before it says it in any language. The
 * renderer turns each case into one localized sentence; **null is a real answer** —
 * a day with nothing to warn about gets no line, never a filler ("Enjoy your day!"
 * is the exact species of invention the screen must not commit).
 *
 * The line **anticipates** (committente, 9 set 2026): what is happening now is the hero
 * right above it, so a sentence that repeated the hero would be the screen saying the
 * same thing twice. It looks for the next thing worth knowing — rain later today or
 * tomorrow, frost by morning, fog on the way, a wind already strong enough to matter —
 * and says the first one it finds, in this order.
 */
sealed interface Headline {

    /** A severe hour ahead (the same table the notifier uses — one definition of
     * "severe" in the whole app). */
    data class Severe(val bucket: AlertEngine.SevereBucket, val at: LocalDateTime) : Headline

    /** Already wet: when it should stop, or the honest "not today". */
    data class WetNow(val stopsAt: LocalDateTime?, val snow: Boolean) : Headline

    /** Dry now, likely wet later today: the umbrella sentence. [clearsAt] is the first
     * hour the chance drops back under half, when the forecast shows one. */
    data class WetSoon(
        val at: LocalDateTime,
        val pct: Int,
        val snow: Boolean,
        val clearsAt: LocalDateTime?
    ) : Headline

    /** Frost by morning: the coldest hour between now and tomorrow mid-morning is at
     * or under zero while it is not freezing now. Anticipation only — a freezing
     * afternoon is already the hero's number. */
    data class Frost(val at: LocalDateTime, val minC: Double) : Headline

    /** Fog on the way inside twelve hours, while it is not foggy now. */
    data class Fog(val at: LocalDateTime) : Headline

    /** The wind right now is strong enough to be the day's fact: the hero has no wind
     * on it, so this is the one "now" the sentence carries. */
    data class Wind(val speedKph: Double, val gustKph: Double) : Headline

    /** Rain possible today: the chance is at least half but under the umbrella bar. */
    data class WetMaybe(val at: LocalDateTime, val pct: Int, val snow: Boolean) : Headline

    /** Dry today, likely wet tomorrow: the first hour tomorrow over the umbrella bar. */
    data class WetTomorrow(val at: LocalDateTime, val pct: Int, val snow: Boolean) : Headline
}

/**
 * The sentence at the top of Today (VISION §3.3.2), computed and testable with no
 * Android in sight. The rain thresholds are [AlertEngine]'s own: the sentence and the
 * notification must never disagree about what counts as "rain coming".
 *
 * Input contract: [report] is already trimmed by `WeatherRecency`, so `hourly[0]` is
 * the hour we are in.
 *
 * The ladder, first match wins:
 *
 * 1. a severe hour inside [AlertEngine.SEVERE_LOOKAHEAD_HOURS];
 * 2. wet now — when it stops;
 * 3. rain likely later **today** (or inside six hours, past midnight) — the umbrella;
 * 4. frost by tomorrow mid-morning;
 * 5. fog inside twelve hours;
 * 6. a strong wind now;
 * 7. rain **possible** today — at least half, under the umbrella bar;
 * 8. rain likely **tomorrow**.
 *
 * Until 9 set 2026 the ladder stopped at step 3 and step 3 looked six hours out, so a
 * dry morning before a wet evening, a dry day before a wet tomorrow, a 60% afternoon
 * and a frosty night all said nothing (committente, from a device: «il widget non la
 * indica»). Silence is still the answer for a day with none of these; it is no longer
 * the answer for a day whose news is eight hours away.
 */
object HeadlineEngine {

    /** Codes where water is falling right now — drizzle through thunderstorm. */
    private val WET_CODES = setOf(
        51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 71, 73, 75, 77,
        80, 81, 82, 85, 86, 95, 96, 99
    )
    private val SNOW_CODES = setOf(71, 73, 75, 77, 85, 86)
    private val FOG_CODES = setOf(45, 48)

    /** Under half, the sky has stopped promising rain: the "clear after that" bar, and
     * — read the other way — the floor of "rain possible". */
    private const val CLEAR_BELOW_PCT = 50

    /** How far past its trigger a sentence keeps looking for the turn it promises. */
    private const val TURN_LOOKAHEAD_HOURS = 12L

    /** Frost is at or under zero, like the drift strip's mark: a marker that fires at
     * +2 °C is one the reader learns to distrust. */
    private const val FROST_C = 0.0

    /** "By morning" ends here, local time: the coldest hour of a night is before it,
     * and past it the sun has had its say. */
    private val FROST_UNTIL = LocalTime.of(10, 0)

    private const val FOG_LOOKAHEAD_HOURS = 12L

    /** The "hold on to your hat" band of the wind tile's own scale, and the gust that
     * matters on its own whatever the steady wind is doing. */
    private const val WIND_STRONG_KPH = 39.0
    private const val GUST_STRONG_KPH = 60.0

    fun headline(report: WeatherReport, now: LocalDateTime): Headline? {
        val hours = report.hourly
        if (hours.isEmpty()) return null
        val ahead = hours.filter { !it.time.isBefore(now) }

        val severeEnd = now.plusHours(AlertEngine.SEVERE_LOOKAHEAD_HOURS)
        val severe = ahead.firstOrNull {
            !it.time.isAfter(severeEnd) && it.condition.wmoCode in AlertEngine.SevereCodes
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
                .takeWhile { it.time.isBefore(now.plusHours(TURN_LOOKAHEAD_HOURS)) }
                .firstOrNull {
                    // An hour with no forecast chance is not evidence that it clears:
                    // the sentence waits for an hour that actually says so (Fase 26).
                    it.condition.wmoCode !in WET_CODES &&
                        (it.precipChancePct ?: return@firstOrNull false) < CLEAR_BELOW_PCT
                }
            return Headline.WetNow(
                stopsAt = stopsAt?.time,
                snow = current.condition.wmoCode in SNOW_CODES
            )
        }

        // "Today" for the umbrella: the rest of the local day, and never less than the
        // notifier's six hours — at 22:00 the rain at 03:00 is still tonight's business.
        val today = now.toLocalDate()
        val soonEnd = now.plusHours(AlertEngine.PRECIP_LOOKAHEAD_HOURS)
        val todays = ahead.filter { it.time.toLocalDate() == today || !it.time.isAfter(soonEnd) }
        todays.firstOrNull { it.chance >= AlertEngine.PRECIP_THRESHOLD_PCT }?.let { wetHour ->
            val clearsAt = hours.asSequence()
                .filter { it.time.isAfter(wetHour.time) }
                .takeWhile { it.time.isBefore(wetHour.time.plusHours(TURN_LOOKAHEAD_HOURS)) }
                .firstOrNull { (it.precipChancePct ?: return@firstOrNull false) < CLEAR_BELOW_PCT }
            return Headline.WetSoon(
                at = wetHour.time,
                pct = wetHour.chance,
                snow = wetHour.condition.wmoCode in SNOW_CODES,
                clearsAt = clearsAt?.time
            )
        }

        // Frost by morning, only as a forecast: if it is freezing now the hero says so.
        if (current.tempC > FROST_C) {
            val morning = today.plusDays(1).atTime(FROST_UNTIL)
            val coldest = ahead
                .filter { !it.time.isAfter(morning) }
                .minByOrNull { it.tempC }
            if (coldest != null && coldest.tempC <= FROST_C) {
                return Headline.Frost(at = coldest.time, minC = coldest.tempC)
            }
        }

        // Fog on the way, only while it is not foggy now: "Fog" is the hero's own word
        // for the present, and the sentence is for what comes next.
        if (current.condition.wmoCode !in FOG_CODES) {
            val fogEnd = now.plusHours(FOG_LOOKAHEAD_HOURS)
            ahead.firstOrNull { !it.time.isAfter(fogEnd) && it.condition.wmoCode in FOG_CODES }
                ?.let { return Headline.Fog(at = it.time) }
        }

        // The one "now" the sentence carries: the hero has no wind on it, and a gale is
        // the day's fact whatever the sky is doing. The model carries no hourly wind, so
        // this cannot look ahead the way the others do.
        val wind = report.current.wind
        if (wind.speedKph >= WIND_STRONG_KPH || wind.gustKph >= GUST_STRONG_KPH) {
            return Headline.Wind(speedKph = wind.speedKph, gustKph = wind.gustKph)
        }

        todays.firstOrNull { it.chance >= CLEAR_BELOW_PCT }?.let { maybe ->
            return Headline.WetMaybe(
                at = maybe.time,
                pct = maybe.chance,
                snow = maybe.condition.wmoCode in SNOW_CODES
            )
        }

        val tomorrow = today.plusDays(1)
        ahead.firstOrNull { it.time.toLocalDate() == tomorrow && it.chance >= AlertEngine.PRECIP_THRESHOLD_PCT }
            ?.let { wet ->
                return Headline.WetTomorrow(
                    at = wet.time,
                    pct = wet.chance,
                    snow = wet.condition.wmoCode in SNOW_CODES
                )
            }

        return null
    }

    /** A missing chance meets no threshold (§1.1): it is not a zero, it is silence. */
    private val HourlyForecast.chance: Int get() = precipChancePct ?: -1
}
