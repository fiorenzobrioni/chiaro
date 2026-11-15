package com.callbackdev.chiaro.domain.pictures

import com.callbackdev.chiaro.domain.model.HourlyForecast
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * «In immagini» (Fase 17, committente 15 nov 2026): the drawn twin of «In parole». Where the
 * text widget writes the day, this one draws it — the temperature as a curve, the rain as
 * bars, the light as a ribbon — with the same facts and the same sentence. The canvas the
 * committente approved is `docs/design/in-immagini/`, and its numbers are the contract
 * (`PLANNING.md`, «Fase 17», «La regola della fedeltà»): every measure in this package is
 * the canvas's, in dp, and a test pins it.
 *
 * This package is the geometry, and only that: where things go, from the forecast, in dp.
 * The colours, the faces and the bitmaps are the painter's (`:app`, step 17.2); the forms
 * and their budgets are `InPicturesLayout` (`:app`). Nothing here touches a resource.
 *
 * What the drawing covers, the reader's choice in the settings («Il disegno»).
 */
enum class PictureSpan {
    /** «Prossime 24 ore»: from the hour we are in to the same hour tomorrow. The default. */
    NEXT_24_HOURS,

    /** «La giornata»: from midnight to midnight, the morning already spent drawn lighter
     * and the day's own high and low labelled. */
    DAY
}

/**
 * The stretch of time a drawing spans, and where an instant falls in it.
 *
 * **Time is the axis, never a list index.** The canvas places hour *i* at `x0 + i × step`
 * because its example has 25 hours on the hour; a real report has a missing hour, a night
 * the clocks change (23 or 25 hours from midnight to midnight), and a `now` that is not on
 * the hour. Every position here is [fraction] of the real span, so a gap is a gap and the
 * disc of «adesso» stands at 14:23 when it is 14:23. On the canvas's own example the two
 * are the same numbers.
 */
data class PictureWindow(
    val start: Instant,
    val end: Instant,
    val now: Instant,
    val zone: ZoneId,
    val span: PictureSpan
) {
    init {
        require(end.isAfter(start)) { "a window must have a length: $start .. $end" }
    }

    /** The window's length in hours: 24, or 23 and 25 for [PictureSpan.DAY] on the nights the
     * clocks change. */
    val hours: Double get() = Duration.between(start, end).toMillis() / MILLIS_PER_HOUR

    /** Where [at] falls, 0 at [start] and 1 at [end]; outside the window it runs past both. */
    fun fraction(at: Instant): Double =
        Duration.between(start, at).toMillis() / Duration.between(start, end).toMillis().toDouble()

    /** Inclusive at both ends: the canvas draws 25 points for 24 hours, the last one the same
     * hour tomorrow, and so does every drawing here. */
    operator fun contains(at: Instant): Boolean = !at.isBefore(start) && !at.isAfter(end)

    /** The hour [now] is in, on the place's clock: what the hour strip calls «Ora». */
    val nowHour: Instant get() = hourOf(now, zone)

    companion object {
        /** [hours] from the hour we are in: the plate's 24 by default, the 4×1 strip's 12. */
        fun next(now: Instant, zone: ZoneId, hours: Long = 24): PictureWindow {
            val start = hourOf(now, zone)
            return PictureWindow(start, start.plus(Duration.ofHours(hours)), now, zone, PictureSpan.NEXT_24_HOURS)
        }

        /** Today, midnight to midnight on the place's clock. */
        fun day(now: Instant, zone: ZoneId): PictureWindow {
            val date = now.atZone(zone).toLocalDate()
            return PictureWindow(
                start = date.atStartOfDay(zone).toInstant(),
                end = date.plusDays(1).atStartOfDay(zone).toInstant(),
                now = now,
                zone = zone,
                span = PictureSpan.DAY
            )
        }

        /** The window the reader's [span] asks for. */
        fun of(span: PictureSpan, now: Instant, zone: ZoneId): PictureWindow = when (span) {
            PictureSpan.NEXT_24_HOURS -> next(now, zone)
            PictureSpan.DAY -> day(now, zone)
        }

        private fun hourOf(at: Instant, zone: ZoneId): Instant =
            at.atZone(zone).truncatedTo(ChronoUnit.HOURS).toInstant()

        private const val MILLIS_PER_HOUR = 3_600_000.0
    }
}

/**
 * One hour of the forecast, as the drawings read it: its instant, its temperature, its
 * chance and its amount. Each may be missing, and a missing one is drawn as nothing — a
 * break in the curve, no bar — never as a zero nobody forecast (CLAUDE.md, «the screen must
 * not lie»; the rain sparkline's own rule).
 */
data class PictureHour(
    val at: Instant,
    val temperatureC: Double?,
    val chancePct: Int?,
    /** The hour's total in mm, rain, showers and the snow's water ([HourlyForecast.precipMm]). */
    val amountMm: Double?
) {
    companion object {
        fun of(hour: HourlyForecast): PictureHour =
            PictureHour(hour.at, hour.tempC, hour.precipChancePct, hour.precipMm)
    }
}

/**
 * The hours «Più tardi» (2×3) and the 4×3's grid print: [count] of them, [strideHours]
 * apart, after the hour we are in — 17, 20, 23 at 14:00, the plate's own tick labels in the
 * «Prossime 24 ore» drawing, so the rows under the plate name the hours the plate marks (the
 * canvas's `later` and `hours`). From the hour of «adesso» and not from the window's start,
 * so «La giornata», whose window starts at midnight, still lists what is to come. Matched on
 * the instant, so a missing hour is a missing row and never a row for the wrong hour.
 */
fun strideHours(
    window: PictureWindow,
    hours: List<PictureHour>,
    count: Int,
    strideHours: Long = LATER_STRIDE_HOURS
): List<PictureHour> {
    val byInstant = hours.associateBy { it.at }
    val from = window.nowHour
    return (1..count).mapNotNull { k ->
        byInstant[from.plus(Duration.ofHours(k * strideHours))]
    }
}

/** Three hours between rows, as the plate's ticks on the 4×2 (the canvas's `later`). */
const val LATER_STRIDE_HOURS = 3L
