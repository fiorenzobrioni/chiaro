package com.callbackdev.chiaro.data

import android.content.Context
import android.os.PowerManager

/**
 * Whether the device is in the system's battery saver mode.
 *
 * Read as a function rather than a value: the reader can flip the switch (or the
 * automatic threshold can trip) while the process is alive, and the answer has to be
 * the one that holds at the moment it is asked.
 *
 * The one caller is Today's automatic fetch — the one on landing and the one the
 * minute tick makes past the provider's resolution. Both are conveniences nobody
 * asked for out loud, which is what battery saver exists to postpone. Deliberately
 * NOT consulted by a pull to refresh (an explicit request is never quietly ignored),
 * by a page that has nothing to show yet (postponing there leaves a skeleton, and a
 * skeleton is not a cheaper screen, it is an empty one), nor by the periodic job:
 * the OS already defers that under Doze and App Standby, and suppressing it here
 * would silence a severe-weather alert precisely on the phone with the least charge
 * left to spare.
 */
fun interface PowerSaveState {
    fun isOn(): Boolean

    companion object {
        fun of(context: Context): PowerSaveState {
            val manager = context.getSystemService(PowerManager::class.java)
            return PowerSaveState { manager?.isPowerSaveMode == true }
        }

        /** For tests and previews: never saving. */
        val Off = PowerSaveState { false }
    }
}
