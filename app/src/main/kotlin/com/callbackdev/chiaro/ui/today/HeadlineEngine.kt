package com.callbackdev.chiaro.ui.today

import com.callbackdev.chiaro.domain.AlertEngine
import com.callbackdev.chiaro.domain.WmoCode
import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.model.WeatherReport
import com.callbackdev.chiaro.domain.warnings.PlaceWarnings
import com.callbackdev.chiaro.domain.warnings.WarningHazard
import com.callbackdev.chiaro.domain.warnings.WarningLevel
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

    /**
     * An official warning at orange or red for the place (Fase 11). Step zero of the
     * ladder: an authority grading the day outranks anything the model says about it.
     *
     * Yellow deliberately never gets here. In an Italian autumn yellow is a frequent
     * state, and a sentence that says the same thing one day in three stops being read;
     * the banner under the hero carries yellow, where it is one line among the day's
     * facts rather than the day's headline.
     */
    data class Official(
        val level: WarningLevel,
        /** At [level], in the issuer's own order — the banner and the notification agree. */
        val hazards: List<WarningHazard>,
        /** Whether the day carrying [level] is the one the reader is in. */
        val today: Boolean
    ) : Headline

    /** A severe hour ahead (the same table the notifier uses — one definition of
     * "severe" in the whole app). */
    data class Severe(val bucket: AlertEngine.SevereBucket, val at: LocalDateTime) : Headline

    /** Already wet: when it should stop, or the honest "not today". */
    data class WetNow(val stopsAt: LocalDateTime?, val falling: Falling) : Headline

    /** Dry now, likely wet later today: the umbrella sentence. [clearsAt] is the first
     * hour the chance drops back under half, when the forecast shows one. */
    data class WetSoon(
        val at: LocalDateTime,
        val pct: Int,
        val falling: Falling,
        val clearsAt: LocalDateTime?
    ) : Headline

    /** Frost by morning: the coldest hour between now and tomorrow mid-morning is at
     * or under zero while it is not freezing now. Anticipation only — a freezing
     * afternoon is already the hero's number. */
    data class Frost(val at: LocalDateTime, val minC: Double) : Headline

    /** Fog on the way inside twelve hours, while it is not foggy now. */
    data class Fog(val at: LocalDateTime) : Headline

    /** The wind is strong enough to be the day's fact: the hero has no wind on it.
     * [at] null is now; otherwise the first hour of today (or of the next six) that
     * reaches it, which the rows can say since they carry the wind (24 set 2026). */
    data class Wind(val speedKph: Double, val gustKph: Double, val at: LocalDateTime? = null) : Headline

    /** Rain possible today: the chance is at least half but under the umbrella bar. */
    data class WetMaybe(val at: LocalDateTime, val pct: Int, val falling: Falling) : Headline

    /** Dry today, likely wet tomorrow: the first hour tomorrow over the umbrella bar. */
    data class WetTomorrow(val at: LocalDateTime, val pct: Int, val falling: Falling) : Headline

    /**
     * What the sentence says is falling (25 set 2026). It was a snow-or-rain Boolean until
     * the state engine started writing rain and snow together (68/69): the icon, the word
     * and the chart's caption said both while the sentence said rain.
     */
    enum class Falling {
        RAIN, SNOW, MIXED;

        companion object {
            fun of(wmoCode: Int): Falling = when {
                WmoCode.of(wmoCode)?.phase == WmoCode.Phase.MIXED -> MIXED
                WmoCode.isSnow(wmoCode) -> SNOW
                else -> RAIN
            }
        }
    }
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
 * 0. an official warning at **orange or red** for the place (Fase 11) — an authority
 *    grading the day outranks a model's reading of it;
 * 1. a severe hour inside [AlertEngine.SEVERE_LOOKAHEAD_HOURS];
 * 2. wet now — when it stops;
 * 3. rain likely later **today** (or inside six hours, past midnight) — the umbrella;
 * 4. frost by tomorrow mid-morning;
 * 5. fog inside twelve hours;
 * 6. a strong wind now, or later today (24 set 2026, when the rows gained the wind);
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

    /** Under half, the sky has stopped promising rain: the "clear after that" bar, and
     * — read the other way — the floor of "rain possible". Internal since the evening
     * summary reads it too (21 set 2026): the app holds two bars for rain, the umbrella
     * one and this one, and a notification inventing a third would be a third opinion
     * on the same sky. */
    internal const val CLEAR_BELOW_PCT = 50

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

    /**
     * [warnings] is what the official-warnings step last wrote for this place, or null
     * where there is nothing (abroad, no bulletin, a bulletin whose days are over). It
     * is a parameter with a default so the widgets that call this can pick it up one
     * step at a time; the ladder below is the same either way.
     */
    fun headline(
        report: WeatherReport,
        now: LocalDateTime,
        warnings: PlaceWarnings? = null
    ): Headline? {
        official(warnings, now)?.let { return it }

        val hours = report.hourly
        if (hours.isEmpty()) return null
        val ahead = hours.filter { !it.time.isBefore(now) }

        val severeEnd = now.plusHours(AlertEngine.SEVERE_LOOKAHEAD_HOURS)
        ahead.asSequence()
            .takeWhile { !it.time.isAfter(severeEnd) }
            .firstNotNullOfOrNull { hour -> AlertEngine.severeBucket(hour)?.let { hour to it } }
            ?.let { (hour, bucket) -> return Headline.Severe(bucket = bucket, at = hour.time) }

        val current = hours.first()
        if (WmoCode.isPrecipitation(current.condition.wmoCode)) {
            val stopsAt = hours.asSequence()
                .drop(1)
                .takeWhile { it.time.isBefore(now.plusHours(TURN_LOOKAHEAD_HOURS)) }
                .firstOrNull {
                    // An hour with no forecast chance is not evidence that it clears:
                    // the sentence waits for an hour that actually says so (Fase 26).
                    !WmoCode.isPrecipitation(it.condition.wmoCode) &&
                        (it.precipChancePct ?: return@firstOrNull false) < CLEAR_BELOW_PCT
                }
            return Headline.WetNow(
                stopsAt = stopsAt?.time,
                falling = Headline.Falling.of(current.condition.wmoCode)
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
                falling = Headline.Falling.of(wetHour.condition.wmoCode),
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
        if (!WmoCode.isFog(current.condition.wmoCode)) {
            val fogEnd = now.plusHours(FOG_LOOKAHEAD_HOURS)
            ahead.firstOrNull { !it.time.isAfter(fogEnd) && WmoCode.isFog(it.condition.wmoCode) }
                ?.let { return Headline.Fog(at = it.time) }
        }

        // The hero has no wind on it, and a gale is the day's fact whatever the sky is
        // doing. Now first; then, since the rows carry the wind (24 set 2026), the first
        // hour of the umbrella's horizon that reaches the same bar — until then this was
        // the one step of the ladder that could not look ahead, and a gale at four was
        // news only at four. A row with no wind at all is not a calm one: it is skipped.
        val wind = report.current.wind
        if (wind.speedKph >= WIND_STRONG_KPH || wind.gustKph >= GUST_STRONG_KPH) {
            return Headline.Wind(speedKph = wind.speedKph, gustKph = wind.gustKph)
        }
        todays.firstOrNull {
            (it.windKph ?: 0.0) >= WIND_STRONG_KPH || (it.gustKph ?: 0.0) >= GUST_STRONG_KPH
        }?.let { windy ->
            return Headline.Wind(
                speedKph = windy.windKph ?: 0.0,
                gustKph = windy.gustKph ?: 0.0,
                at = windy.time
            )
        }

        todays.firstOrNull { it.chance >= CLEAR_BELOW_PCT }?.let { maybe ->
            return Headline.WetMaybe(
                at = maybe.time,
                pct = maybe.chance,
                falling = Headline.Falling.of(maybe.condition.wmoCode)
            )
        }

        val tomorrow = today.plusDays(1)
        ahead.firstOrNull { it.time.toLocalDate() == tomorrow && it.chance >= AlertEngine.PRECIP_THRESHOLD_PCT }
            ?.let { wet ->
                return Headline.WetTomorrow(
                    at = wet.time,
                    pct = wet.chance,
                    falling = Headline.Falling.of(wet.condition.wmoCode)
                )
            }

        return null
    }

    /**
     * Step zero. Today before tomorrow when both carry the peak — a sentence about now
     * beats a sentence about later — and the hazards are the ones AT the peak on the
     * day named, not every hazard the day carries: "orange for thunderstorms" must not
     * become "orange for thunderstorms and hydrogeological risk" because the second was
     * yellow.
     */
    private fun official(warnings: PlaceWarnings?, now: LocalDateTime): Headline.Official? {
        if (warnings == null) return null
        val level = warnings.maxLevel
        if (level < WarningLevel.ORANGE) return null
        val today = now.toLocalDate()
        val day = warnings.peakDays.firstOrNull { it.date == today } ?: warnings.peakDays.first()
        val hazards = WarningHazard.displayOrder.filter { day.levels[it] == level }
        if (hazards.isEmpty()) return null
        return Headline.Official(level = level, hazards = hazards, today = day.date == today)
    }

    /** A missing chance meets no threshold (§1.1): it is not a zero, it is silence. */
    private val HourlyForecast.chance: Int get() = precipChancePct ?: -1
}
