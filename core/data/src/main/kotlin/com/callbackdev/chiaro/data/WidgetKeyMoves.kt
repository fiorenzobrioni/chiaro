package com.callbackdev.chiaro.data

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences

/**
 * Moves per-widget keys from the ids a backup was made with to the ids the restore handed
 * out (18 ott 2026), for every store that files a value under an `appWidgetId`.
 *
 * Snapshot first, write last, inside one edit: the old and the new ids can overlap (1→2
 * while 2→3), so moving key by key would overwrite a value still to be moved. Every old
 * key is removed whether it held a value or not, so a restore leaves no orphan behind
 * and a widget later handed an old number inherits nothing.
 */
class WidgetKeyMoves(private val prefs: MutablePreferences) {

    private val removals = mutableListOf<() -> Unit>()
    private val writes = mutableListOf<() -> Unit>()

    fun <T> move(from: Preferences.Key<T>, to: Preferences.Key<T>) {
        prefs[from]?.let { value -> writes += { prefs[to] = value } }
        removals += { prefs.remove(from) }
    }

    /** Applies the moves: every old key out, then every value under its new key. */
    fun commit() {
        removals.forEach { it() }
        writes.forEach { it() }
    }
}

/** Pairs [oldIds] with [newIds] as `onRestored` hands them over, and moves each pair. */
fun MutablePreferences.moveWidgetKeys(
    oldIds: IntArray,
    newIds: IntArray,
    each: WidgetKeyMoves.(old: Int, new: Int) -> Unit
) {
    val moves = WidgetKeyMoves(this)
    oldIds.zip(newIds).forEach { (old, new) -> moves.each(old, new) }
    moves.commit()
}
