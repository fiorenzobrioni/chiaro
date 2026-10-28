package com.callbackdev.chiaro.domain

import com.callbackdev.chiaro.domain.model.WeatherReport
import java.time.Instant

/**
 * The report as a screen reads it for NOW (15.1, 28 Oct 2026): the one preparation Today
 * had made since 24 set 2026 and the Alerts cards did not.
 *
 * Two steps, in this order. [CurrentEstimate.apply] first: past an hour of age the fetched
 * `current` block is replaced by the forecast for this minute, read off the rows around now,
 * which must still be there to interpolate. [WeatherRecency.trim] then drops the hours and
 * the days already gone, so that `hourly[0]` is the hour we are in and `daily[0]` is today.
 * Null when nothing of the report is still about the present (a cache past its own
 * horizon): the caller has no «now» to read and says so, rather than reading a week that is
 * over.
 *
 * Why one function and not two calls: the rule cards on Alerts read the raw cache while
 * Today read the prepared report, and the two disagreed on the same rule and the same data —
 * «Adesso no · è 19,7°» off a `current` block three hours old, under a hero that said 15°;
 * «Adesso sì» on yesterday's high, off a cache fetched the day before. What «now» is in a
 * report is decided here, once, and every reader takes it from here.
 */
object WeatherNow {

    fun prepare(report: WeatherReport, now: Instant): WeatherReport? {
        val prepared = WeatherRecency.trim(CurrentEstimate.apply(report, now), now)
        return prepared.takeIf { it.hourly.isNotEmpty() && it.daily.isNotEmpty() }
    }
}
