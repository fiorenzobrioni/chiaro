package com.callbackdev.chiaro.domain

import kotlin.math.roundToInt

/**
 * The numbers that are a combination of models (Fase 18, 22 nov 2026, `docs/STATES.md`
 * «The sky of three models»): the one rule of `CLAUDE.md` that lets a number on screen be
 * other than one model's, applied where it was measured to be closer to what happens.
 *
 * Today that is the sky alone. The hour's cloud — the total cover and its three layers — is
 * the mean of `best_match`'s and of the global witnesses' that have all four numbers, at equal
 * weight: against the satellite, in the summer of 2026 on 26 airports and 25 489 daylight
 * hours, the sky read from the mean is right in 83.6% of the hours against 76.5% for
 * `best_match` alone (Heidke 0.33 → 0.44), in the Po valley and outside it, and says «no sun»
 * under a sun the satellite saw in 0.6% of the hours against 3.3%. `best_match` over Europe is
 * the ICON family, the furthest of the models measured from the satellite. The temperature was
 * measured too and left out (Fase 18.2: the Po valley's summer low got worse).
 *
 * Pure arithmetic: which models count, and which of them is `best_match` itself, is decided
 * by the caller (the mapper's witnesses). Amounts and probabilities never come here: the mean
 * of two rains is a drizzle no model forecast.
 */
object ModelMean {

    /** One model's cloud at one moment, every number of it: a model counts only whole. */
    data class Sky(val coverPct: Int, val lowPct: Int, val midPct: Int, val highPct: Int) {
        companion object {
            /** The four numbers as one [Sky], or null when any is missing. */
            fun of(coverPct: Int?, lowPct: Int?, midPct: Int?, highPct: Int?): Sky? =
                if (coverPct == null || lowPct == null || midPct == null || highPct == null) null
                else Sky(coverPct, lowPct, midPct, highPct)
        }
    }

    /**
     * The mean of [own] and [others], each number rounded to the percent the provider serves;
     * null without [own] — the hour's model is the one the others are added to, never left
     * out — and [own] itself when there is no other (no witnesses: the sky is `best_match`'s,
     * as it was before the mean).
     */
    fun sky(own: Sky?, others: List<Sky>): Sky? {
        own ?: return null
        if (others.isEmpty()) return own
        val all = listOf(own) + others
        fun mean(pick: (Sky) -> Int): Int = (all.sumOf(pick).toDouble() / all.size).roundToInt()
        return Sky(mean(Sky::coverPct), mean(Sky::lowPct), mean(Sky::midPct), mean(Sky::highPct))
    }
}
