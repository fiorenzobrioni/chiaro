package com.callbackdev.chiaro.domain

import java.time.LocalDateTime

/**
 * The parts of a day a sentence names, on the place's own clock: the night until 6, the
 * morning until noon, the afternoon until 18, the evening until midnight. One answer for every
 * reader that says «nel pomeriggio»: the open day's sentence (31 ott 2026) and the day's
 * possible storm ([PossibleStorm], Fase 17.4), so that two lines of one page never cut the
 * day in two different places. An hour belongs to the part it starts in.
 */
enum class DayPart(val untilHour: Int) {
    NIGHT(6), MORNING(12), AFTERNOON(18), EVENING(24);

    companion object {
        fun of(time: LocalDateTime): DayPart = entries.first { time.hour < it.untilHour }

        /** The sky is the day's: the night before the morning is not part of it. */
        val SKY = listOf(MORNING, AFTERNOON, EVENING)
    }
}
