package com.callbackdev.chiaro.widget

import android.content.Context
import com.callbackdev.chiaro.data.ActiveSource
import com.callbackdev.chiaro.data.AppSettings
import com.callbackdev.chiaro.data.ServiceLocator
import com.callbackdev.chiaro.domain.WeatherFreshness
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.WeatherReport
import com.callbackdev.chiaro.domain.sky.SkyJob
import com.callbackdev.chiaro.domain.sky.SkyJobCatalog
import com.callbackdev.chiaro.domain.sky.SkyOccurrence
import com.callbackdev.chiaro.domain.sky.SkyScheduler
import com.callbackdev.chiaro.domain.sky.SkyVerdict
import com.callbackdev.chiaro.domain.sky.SkyVerdictEngine
import com.callbackdev.chiaro.ui.today.TodayStateBuilder
import com.callbackdev.chiaro.ui.today.TodayUiState
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.first

/**
 * What a widget knows when it draws. A widget never invents (VISION §5.9): [content]
 * is [TodayStateBuilder]'s own answer over the CACHED report — the same numbers, the
 * same staleness verdict, the same recency trim the app shows, with no network asked
 * at render time. Null city means no place configured; null content means a place
 * with no data yet. Both are said, never papered over.
 */
data class WidgetModel(
    val settings: AppSettings,
    val look: WidgetLook,
    val city: City?,
    val content: TodayUiState.Content?,
    val nextMoment: NextMoment?,
    val zone: ZoneId
)

/** The Sky widget's subject: the next subscribed moment to fire, judged. */
data class NextMoment(
    val job: SkyJob,
    val start: Instant,
    val end: Instant?,
    val verdict: SkyVerdict?
)

object WidgetData {

    /**
     * The model for ONE widget instance: its pinned city if it has one (the
     * inherited [com.callbackdev.chiaro.data.WidgetCityStore], off the bench since
     * the reconfigure flow landed), the app's active place otherwise, and its own
     * look. A pin whose city was removed falls back to the active place — the honest
     * nearest answer, and the reconfigure flow is one long-press away.
     */
    suspend fun load(context: Context, appWidgetId: Int): WidgetModel {
        val settings = ServiceLocator.settingsStore(context).settings.first()
        val look = WidgetLookStore.get(context).lookFor(appWidgetId)
        val city = pinnedCity(context, appWidgetId) ?: activeCity(context)
        val zone = city?.timezone?.let { runCatching { ZoneId.of(it) }.getOrNull() }
            ?: ZoneId.systemDefault()
        if (city == null) {
            return WidgetModel(settings, look, null, null, null, zone)
        }
        val now = Instant.now()
        val report = ServiceLocator.weatherRepository(context).cachedReport(city)
        val content = report?.let {
            TodayStateBuilder.build(
                city, it, now, settings.updateFrequencyMin,
                refreshing = false, error = null
            ) as? TodayUiState.Content
        }
        return WidgetModel(
            settings = settings,
            look = look,
            city = city,
            content = content,
            nextMoment = nextMoment(context, city, zone, now, report, settings),
            zone = zone
        )
    }

    private suspend fun pinnedCity(context: Context, appWidgetId: Int): City? {
        val cityId = ServiceLocator.widgetCityStore(context)
            .current()[appWidgetId] ?: return null
        return ServiceLocator.cityStore(context).cities.first()
            .firstOrNull { it.id == cityId }
    }

    private suspend fun activeCity(context: Context): City? =
        when (val source = ServiceLocator.cityStore(context).activeSource.first()) {
            is ActiveSource.Saved -> source.city
            is ActiveSource.Gps -> source.lastFix
            ActiveSource.None -> null
        }

    /** The first subscribed moment to fire after now, with the sky's opinion on it. */
    private suspend fun nextMoment(
        context: Context,
        city: City,
        zone: ZoneId,
        now: Instant,
        report: WeatherReport?,
        settings: AppSettings
    ): NextMoment? {
        val jobs = ServiceLocator.skySubscriptionStore(context).subscriptions.first()
            .filter { it.enabled }
            .mapNotNull { SkyJobCatalog.byId(it.jobId) }
        if (jobs.isEmpty()) return null
        val at: SkyOccurrence.At =
            SkyScheduler.nextToFire(jobs, now, zone, city.coordinates) ?: return null
        val verdict = if (at.job.observable) {
            SkyVerdictEngine.evaluate(
                job = at.job,
                start = at.start,
                end = at.end,
                hours = report?.hourly.orEmpty(),
                zone = zone,
                coordinates = city.coordinates,
                dataAge = report?.let { Duration.between(it.systemInfo.lastSync, now) },
                staleAfter = WeatherFreshness.staleAfter(settings.updateFrequencyMin)
            )
        } else {
            null
        }
        return NextMoment(at.job, at.start, at.end, verdict)
    }
}
