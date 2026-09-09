package com.callbackdev.chiaro.sync

import android.content.Context
import android.net.ConnectivityManager
import com.callbackdev.chiaro.data.ServiceLocator
import com.callbackdev.chiaro.data.local.WarningRecordEntity
import com.callbackdev.chiaro.data.local.WarningRecordKind
import com.callbackdev.chiaro.data.warnings.StoredBulletin
import com.callbackdev.chiaro.data.warnings.WarningFetchResult
import com.callbackdev.chiaro.data.warnings.WarningFetchState
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.settings.NotificationSettings
import com.callbackdev.chiaro.domain.warnings.OfficialWarningEngine
import com.callbackdev.chiaro.domain.warnings.PlaceWarnings
import com.callbackdev.chiaro.domain.warnings.WarningDiff
import com.callbackdev.chiaro.domain.warnings.WarningFetchPolicy
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.flow.first

/**
 * The official warnings' leg of the periodic job (Fase 11): fetch the issuer's
 * bulletin when the cadence says so, write the Journal's rows for the active place,
 * and hand the notifier what is worth saying. One step per device, not per place — a
 * bulletin covers everyone — and **inert** when no saved place is in a zone the issuer
 * grades, so a reader abroad never spends a byte on it.
 *
 * Everything that decides is pure and lives in the domain ([WarningFetchPolicy],
 * [OfficialWarningEngine], [WarningDiff]); this class only threads the clock, the
 * store, the network and the DAO through them. It never throws to the worker:
 * a failure keeps the previous bulletin and, at most once a day, says so in the
 * Journal — never on Oggi.
 */
class OfficialWarningsStep(private val context: Context) {

    /**
     * @param active the place the job evaluates, [cityKey] its notification key
     * @param others the other saved places, for the inertness check only
     * @param force skip the cadence (a pull to refresh)
     * @param now the instant, injectable so the cadence is testable
     */
    suspend fun run(
        active: City,
        cityKey: String,
        others: List<City>,
        settings: NotificationSettings,
        notifiers: SyncNotifiers,
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

        // What the place looked like under the bulletin in hand, before anything moves.
        val before: PlaceWarnings? = state.current?.let {
            OfficialWarningEngine.forPlace(it.bulletin, activeZone, today)
        }
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
                is WarningFetchResult.Failed -> {
                    val todaysMissing = held == null || held.bulletin.issuedAt.toLocalDate() < today
                    val dueByNow = localNow.toLocalTime() >= WarningFetchPolicy.PublicationStart
                    if (todaysMissing && dueByNow && state.failureLoggedOn != today) {
                        recordMissed(active, now, held)
                        store.markFailureLogged(source.id, today)
                    }
                }
            }
        }

        val bulletin = held?.bulletin ?: return
        val after = OfficialWarningEngine.forPlace(bulletin, activeZone, today) ?: return

        // The Journal: what moved between two consecutive bulletins, for this place.
        if (held != state.current) {
            val dao = ServiceLocator.warningRecordDao(context)
            WarningDiff.between(before, after).forEach { change ->
                dao.insert(
                    WarningRecordEntity(
                        cityKey = active.cacheKey,
                        recordedEpochSeconds = now.epochSecond,
                        kind = WarningRecordKind.LEVEL_CHANGE.name,
                        bulletinId = after.bulletinId,
                        issuedAt = after.issuedAt.toString(),
                        zoneName = after.zone.name,
                        day = change.day.toString(),
                        hazard = change.hazard.name,
                        fromLevel = change.from.name,
                        toLevel = change.to.name
                    )
                )
            }
            dao.pruneCity(active.cacheKey, RETENTION)
        }

        if (!settings.officialWarnings) return
        val notification = OfficialWarningEngine.notificationFor(
            warnings = after,
            minLevel = settings.officialWarningsFrom,
            notified = store.notified.first(),
            cityKey = cityKey
        ) ?: return
        // Burns only on a successful post: a muted channel keeps its chance.
        if (notifiers.notifyOfficialWarning(notification, active, today)) {
            store.recordNotified(notification.fingerprint)
        }
    }

    private suspend fun recordMissed(active: City, now: Instant, held: StoredBulletin?) {
        ServiceLocator.warningRecordDao(context).insert(
            WarningRecordEntity(
                cityKey = active.cacheKey,
                recordedEpochSeconds = now.epochSecond,
                kind = WarningRecordKind.BULLETIN_MISSED.name,
                bulletinId = held?.bulletin?.id,
                issuedAt = held?.bulletin?.issuedAt?.toString()
            )
        )
    }

    /** The 4.7 MB fallback archive is for Wi-Fi; a metered network gets the small files only. */
    private fun isMetered(): Boolean =
        context.getSystemService(ConnectivityManager::class.java)?.isActiveNetworkMetered ?: true

    companion object {
        /** Rows kept per place — the depth of one place's warning diary, like the history's. */
        const val RETENTION = 100
    }
}
