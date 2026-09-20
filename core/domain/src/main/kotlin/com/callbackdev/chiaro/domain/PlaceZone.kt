package com.callbackdev.chiaro.domain

import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.WeatherReport
import java.time.ZoneId

/**
 * The zone a place's hours, days and clock labels are read in — resolved in ONE place
 * since 20 set 2026, because six surfaces were resolving it two different ways and one
 * of them was wrong for the position.
 *
 * The order is the order of how much each source knows:
 *
 * 1. **The report's own timezone.** Open-Meteo resolves `timezone=auto` from the
 *    coordinates it was handed and names it back (`Europe/Rome`, `Etc/GMT+11`), so this
 *    is the provider's answer about the point the forecast is FOR. It is never absent
 *    in a response that parsed.
 * 2. **The city's**, from the geocoder. Present for a searched place, and **null for
 *    the position** — deliberately, since `timezone=auto` was always going to answer it
 *    ([com.callbackdev.chiaro.domain.model.toGpsCity]). Which is exactly how the
 *    position ended up on the device's zone everywhere the report was not consulted:
 *    Today read the report and the widget read the city, so a phone whose own zone was
 *    not the place's drew two different clocks for one afternoon — the disagreement the
 *    widget's own notes say it exists to prevent.
 * 3. **The device's**, which is a guess, and the only thing left when a place has never
 *    been fetched.
 *
 * A zone id that `ZoneId` cannot parse falls through to the next source rather than
 * throwing: a place is still worth drawing on the device's clock, and every caller used
 * to write that `runCatching` by hand.
 */
fun placeZone(report: WeatherReport?, city: City?): ZoneId =
    report?.location?.timezone?.toZoneOrNull()
        ?: city?.timezone?.toZoneOrNull()
        ?: ZoneId.systemDefault()

/** [placeZone] for a caller that has only a report — the common case in the engines. */
fun WeatherReport.zone(): ZoneId = placeZone(this, null)

private fun String.toZoneOrNull(): ZoneId? = runCatching { ZoneId.of(this) }.getOrNull()
