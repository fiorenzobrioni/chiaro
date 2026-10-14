package com.callbackdev.chiaro.data.local

/**
 * The lines of a forecast diff, in git style ([ForecastDiff]): a changed field emits its
 * old value as a `-` line followed by the new one as a `+` line; untouched fields are
 * context, and a day seen for the first time is all additions (git's "new file").
 */
object SnapshotDiff {

    enum class Type { CONTEXT, ADDED, REMOVED }

    data class Line(val type: Type, val key: String, val value: String)
}
