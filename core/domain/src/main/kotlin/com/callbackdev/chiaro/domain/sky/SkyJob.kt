package com.callbackdev.chiaro.domain.sky

/**
 * How a job's schedule is expressed, and therefore what its cron line says (Fase
 * 16b). The taxonomy is the feature, not a workaround for the metaphor: a crontab
 * line asserts a RECURRENCE, which is true of every job here, while the instant it
 * resolves to lives in the comment channel, which is where a real crontab puts a
 * computed fact. See `VISION_SKY.md` §3.
 */
enum class SkyJobKind(val expression: String) {
    /** Once per local day; the instant is computed per day. */
    DAILY("@daily"),

    /** Once per year; the instant is computed per year. */
    ANNUAL("@yearly"),

    /**
     * Aperiodic, detected by evaluation. A half-hourly polling expression is the
     * honest way to schedule an event with no recurrence rule — you look often and
     * act when the condition holds — and it also happens to be literally what the app
     * does: it evaluates on each fetch.
     *
     * (The expression is not spelled out in this comment for a dull reason: it opens
     * with the two characters that end a block comment.)
     */
    POLLING("*/30 * * * *")
}

/** What the job resolves to: one instant, or a window with two ends. */
enum class SkyJobShape { INSTANT, RANGE }

/**
 * One line of `sky.crontab`. A value, not a class with behaviour: the catalog is a
 * fixed list of these ([SkyJobCatalog]) and [SkyScheduler] turns one plus a date
 * into an occurrence, exactly as the 22 variables of `alerts.rules` are values that
 * `RuleEngine` resolves.
 *
 * [id] is the dotted name the file shows and the store persists. It is **English and
 * never localized**: it is code, like a JSON key — see `VISION_SKY.md` §4.
 */
data class SkyJob(
    val id: String,
    val kind: SkyJobKind,
    val shape: SkyJobShape,
    /**
     * True when the job resolves to something you would go outside and LOOK at, and
     * therefore something the clouds can have an opinion about (Fase 16d).
     *
     * Almost everything in the catalog is: a sunset, a golden hour, a moonrise, a
     * shower's peak. The exceptions are the moments of pure geometry — the solstice,
     * the instant of a quarter moon, solar noon — which happen at a computed time and
     * are not a sight. A `✗ fail` on a first quarter would be the file inventing a
     * stake nobody has.
     */
    val observable: Boolean = true,
    /**
     * True when clouds decide whether the event is worth ANY of your attention.
     *
     * Not the same as [observable], and the difference is the whole of Fase 16f's
     * notification rule: a sunset is observable but happens regardless, so its
     * reminder goes out even under a `✗ fail` — you may have somewhere to be at dusk.
     * The Perseids peak is only an event if the sky is clear, so a reminder for a
     * failed one is noise and is suppressed.
     */
    val visibilityDependent: Boolean = false,
    /** True when the darkness of the sky, not just its clearness, is the point. */
    val needsDarkness: Boolean = false,
    /**
     * True when this is an event somebody would bring a camera to (Fase 28).
     *
     * Deliberately **scarce**, and that is the whole of whether it is a feature or
     * noise: on a third of the catalog it says nothing, so it is on nine of sixty —
     * the four photographer's hours, the full moon at dusk, the two earthshine windows,
     * the Milky Way's core and the LUNAR eclipse. Not on solar noon, not on an equinox,
     * not on perihelion, however much those matter otherwise; and not on the solar
     * eclipse, for a reason that is about safety rather than about the sight and is
     * written where that job is declared.
     *
     * It is also not an opinion. This app does not print "pretty"; what the flag buys
     * is a **bearing** — the row that carries it says which way to turn, because that
     * is the part a person cannot work out from a time. Golden hour without "the sun
     * sets west-north-west" is an almanac line; with it, it is a window to stand at.
     */
    val photographic: Boolean = false
) {
    val expression: String get() = kind.expression
}
