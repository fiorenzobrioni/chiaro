package com.callbackdev.chiaro.sync

import android.content.Context
import android.net.ConnectivityManager
import com.callbackdev.chiaro.data.ServiceLocator
import com.callbackdev.chiaro.data.warnings.StoredBulletin
import com.callbackdev.chiaro.data.warnings.WarningFetchResult
import com.callbackdev.chiaro.data.warnings.WarningFetchState
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.settings.NotificationSettings
import com.callbackdev.chiaro.domain.warnings.OfficialWarningEngine
import com.callbackdev.chiaro.domain.warnings.WarningFetchPolicy
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.flow.first

/**
 * The official warnings' leg of the periodic job (Fase 11): fetch the issuer's
 * bulletin when the cadence says so and hand the notifier what is worth saying. One
 * step per device, not per place — a bulletin covers everyone — and **inert** when no
 * saved place is in a zone the issuer grades, so a reader abroad never spends a byte
 * on it.
 *
 * Everything that decides is pure and lives in the domain ([WarningFetchPolicy],
 * [OfficialWarningEngine]); this class only threads the clock, the store and the
 * network through them. It never throws to the worker: a failure keeps the previous
 * bulletin, whose age the banner and the card state.
 */
class OfficialWarningsStep(private val context: Context) {

    /**
     * @param active the place the job evaluates, [cityKey] its notification key
     * @param others the other saved places, for the inertness check only
     * @param force skip the cadence (a pull to refresh)
     * @param notifiers who may speak, or **null for a run that does not speak** — which is
     *   what a SCREEN asking for the bulletin passes ([WarningRefresh]). In this app the
     *   screen fetches content and the job notifies, the same division the weather itself
     *   follows: opening Today never posts an alert. A silent run also leaves the
     *   fingerprint unburnt, so the job can still say it if the reader has walked away.
     * @param now the instant, injectable so the cadence is testable
     */
    suspend fun run(
        active: City,
        cityKey: String,
        others: List<City>,
        settings: NotificationSettings,
        notifiers: SyncNotifiers?,
        force: Boolean = false,
        now: Instant = Instant.now()
    ) {
        val source = ServiceLocator.warningSource(context)
        val index = ServiceLocator.warningZoneIndex(context)
        val activeZone = index.locate(active.coordinates, active.admin3)
        val anyoneGraded = activeZone != null ||
            others.any { index.locate(it.coordinates, it.admin3) != null }
        if (!anyoneGraded) return

        val store = ServiceLocator.warningStore(context)
        val state = store.state(source.id).first()
        val localNow: LocalDateTime = now.atZone(source.zone).toLocalDateTime()
        val today: LocalDate = localNow.toLocalDate()
        val lastAttempt = state.lastAttempt?.atZone(source.zone)?.toLocalDateTime()

        var held: StoredBulletin? = state.current

        if (force || WarningFetchPolicy.shouldFetch(localNow, held?.bulletin, lastAttempt)) {
            store.markAttempt(source.id, now)
            val known = WarningFetchState(stamp = held?.stamp, feedTag = state.feedTag)
            when (val result = source.fetch(known, allowLargeDownload = !isMetered())) {
                is WarningFetchResult.Fresh -> {
                    held = StoredBulletin(source.id, result.stamp, result.bulletin)
                    store.setBulletin(held)
                    store.setFeedTag(source.id, result.feedTag)
                    // The home screen carries the level too (Fase 11, fourth step), and
                    // a bulletin can land on a run whose weather came from the cache —
                    // no commit, so the repository's own hook never fires. This is the
                    // only road from a new bulletin to a repainted card.
                    SyncDependencies.widgets?.repaintAll()
                }
                is WarningFetchResult.Unchanged -> store.setFeedTag(source.id, result.feedTag)
                // Nothing to record: the bulletin in hand stays, with its own date.
                is WarningFetchResult.Failed -> Unit
            }
        }

        val bulletin = held?.bulletin ?: return
        if (notifiers == null || !settings.officialWarnings) return
        val warnings = OfficialWarningEngine.forPlace(bulletin, activeZone, today) ?: return
        val notification = OfficialWarningEngine.notificationFor(
            warnings = warnings,
            minLevel = settings.officialWarningsFrom,
            notified = store.notified.first(),
            cityKey = cityKey
        ) ?: return
        // Burns only on a successful post: a muted channel keeps its chance.
        if (notifiers.notifyOfficialWarning(notification, active, today)) {
            store.recordNotified(notification.fingerprint)
            // What it told, cell by cell: the next bulletin repeating it stays quiet.
            notification.told.forEach { store.recordNotified(it) }
        }
    }

    /** The 4.7 MB fallback archive is for Wi-Fi; a metered network gets the small files only. */
    private fun isMetered(): Boolean =
        context.getSystemService(ConnectivityManager::class.java)?.isActiveNetworkMetered ?: true
}
