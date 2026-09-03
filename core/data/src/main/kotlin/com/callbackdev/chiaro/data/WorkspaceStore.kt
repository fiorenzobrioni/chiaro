package com.callbackdev.chiaro.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.workspaceDataStore by preferencesDataStore(name = "workspace")

/**
 * Session state — what the app remembers about itself, as opposed to what the reader
 * chose. Deliberately its own DataStore instead of a [SettingsStore] key: the Settings
 * screen's reset restores every choice to its default, and it must not also reopen
 * hints the reader already used.
 */
class WorkspaceStore(private val dataStore: DataStore<Preferences>) {

    /**
     * The one-time guide card on Today (VISION §5.7), shown until it is used or
     * dismissed. Not a settings toggle on purpose: a switch for something that
     * happens once would spend the rest of the app's life sitting on "off", and a
     * settings reset would bring the card back to someone who read the guide long
     * ago. The way to see the guide again is Settings, where it always lives.
     */
    val guideCardDismissed: Flow<Boolean> = dataStore.data
        .map { it[GuideCardDismissed] ?: false }
        .distinctUntilChanged()

    suspend fun dismissGuideCard() {
        dataStore.edit { it[GuideCardDismissed] = true }
    }

    companion object {
        private val GuideCardDismissed = booleanPreferencesKey("guide_card_dismissed")

        fun create(context: Context) = WorkspaceStore(context.workspaceDataStore)
    }
}
