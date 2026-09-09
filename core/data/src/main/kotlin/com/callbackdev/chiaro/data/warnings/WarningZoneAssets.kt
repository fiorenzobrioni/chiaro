package com.callbackdev.chiaro.data.warnings

import android.content.Context
import com.callbackdev.chiaro.domain.warnings.WarningZoneIndex

/**
 * The Android side of the warning-zone index: one bundled asset, read once, handed to
 * the domain as a string. The asset is written by `tools/build_warning_zones.py` from
 * the Dipartimento della Protezione Civile's bulletin files (CC BY 4.0); its docstring
 * is the record of what it holds and how it was measured.
 */
object WarningZoneAssets {

    const val FILE_NAME = "warning_zones_it.json"

    /** Reads and decodes the bundled index; ~290 KB of JSON, so not on the main thread. */
    fun load(context: Context): WarningZoneIndex =
        context.assets.open(FILE_NAME).bufferedReader().use { reader ->
            WarningZoneIndex.decode(reader.readText())
        }
}
