package com.callbackdev.chiaro.domain.pictures

import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * The canvas's example, as its script holds it (`docs/design/in-immagini/Forms.dc.html`,
 * `T`, `P`, `A`): Milan, Sunday 15 November 2026, 25 hours from 14:00. The expected numbers in
 * the tests are what the canvas's own `chart()`, `dial()` and `strip` compute from these
 * arrays (run under node on 15 Nov 2026), so a test that passes is a drawing that lands where
 * the approved one does.
 */
internal object CanvasMilan {
    val zone: ZoneId = ZoneId.of("Europe/Rome")
    val now: Instant = LocalDateTime.parse("2026-11-15T14:00").atZone(zone).toInstant()

    val T = doubleArrayOf(
        11.2, 11.8, 11.3, 10.2, 9.4, 8.9, 8.5, 8.1, 7.8, 7.5, 7.2, 6.9, 6.6,
        6.3, 6.1, 6.0, 5.9, 6.0, 6.6, 7.9, 9.6, 10.9, 11.7, 12.1, 12.0
    )
    val P = intArrayOf(5, 5, 5, 10, 10, 15, 30, 60, 75, 80, 70, 60, 45, 30, 20, 10, 5, 5, 5, 0, 0, 0, 0, 0, 0)
    val A = doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.1, 0.6, 1.4, 1.8, 1.2, 0.7, 0.3, 0.1, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)

    val hours: List<PictureHour> = T.indices.map { i ->
        PictureHour(now.plus(Duration.ofHours(i.toLong())), T[i], P[i], A[i])
    }

    /** The canvas's 4×2 plate (`chart(312, 3)`) and 3×2 plate (`chart(226, 6)`). */
    const val PANEL_PLATE = 312f
    const val COMPACT_PLATE = 226f
}
