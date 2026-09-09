package com.callbackdev.chiaro.domain.warnings

/**
 * A CAP 1.2 document read flat: the handful of fields an issuer's bulletin is made
 * of, as strings, exactly as the XML carried them. The pull-parser in `:core:data`
 * produces this and knows nothing about levels or hazards; [DpcBulletinReader] reads
 * those out of the strings and knows nothing about XML — the seam that lets the
 * reading be tested on a data class and the parsing on a real file.
 */
data class CapAlert(
    /** `<identifier>`, the issuer's id for this bulletin (`DPC_BULLETIN_2026_09_08_6471`). */
    val identifier: String,
    /** `<sent>`, ISO-8601 with the issuer's local offset. */
    val sent: String,
    /** `<note>`, the issuer's free text, or null. */
    val note: String?,
    val infos: List<CapInfo>
)

/** One `<info>` block: a hazard at a level, for a day, over a list of areas. */
data class CapInfo(
    val event: String,
    val onset: String?,
    val expires: String?,
    val severity: String?,
    val areas: List<CapArea>
)

/** One `<area>`: its description and its geocodes, `valueName → value`. */
data class CapArea(
    val description: String,
    val geocodes: Map<String, String>
)
