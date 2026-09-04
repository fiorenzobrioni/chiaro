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
import com.callbackdev.chiaro.domain.sky.SkyVerdict
import com.callbackdev.chiaro.domain.sky.SkyVerdictEngine
import com.callbackdev.chiaro.ui.sky.SkyUpcoming
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
    /**
     * The subscribed moments in front of the reader, soonest first (Fase 8b). A list
     * rather than one, because the Sky widget draws as many as the launcher's grant
     * has room for; on a one-cell widget that is exactly [nextMoment] and nothing
     * changes. Capped at [WidgetData.MaxMoments] — every entry costs a verdict, and
     * no home screen is tall enough for more.
     */
    val moments: List<NextMoment>,
    val zone: ZoneId
) {
    /** The one a one-cell widget shows, and the head of every taller one. */
    val nextMoment: NextMoment? get() = moments.firstOrNull()
}

/**
 * The Sky widget's subject: the subscribed moment in front of the reader, judged.
 * [inProgress] is a window that has opened and not closed — the widget says so
 * rather than printing a start time that has been and gone.
 */
data class NextMoment(
    val job: SkyJob,
    val start: Instant,
    val end: Instant?,
    val verdict: SkyVerdict?,
    val inProgress: Boolean
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
            return WidgetModel(settings, look, null, null, emptyList(), zone)
        }
        val now = Instant.now()
        val report = ServiceLocator.weatherRepository(context).cachedReport(city)
        val content = report?.let {
            TodayStateBuilder.build(
                city, it, now, settings.updateFrequencyMin,
                userRefreshing = false, error = null
            ) as? TodayUiState.Content
        }
        return WidgetModel(
            settings = settings,
            look = look,
            city = city,
            content = content,
            moments = moments(context, city, zone, now, report, settings),
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

    /**
     * The subscribed moments in front of the reader, judged — [SkyUpcoming]'s own
     * answers, which are exactly the scheduled rows of the Sky screen's list in the
     * screen's order. The widget and the screen printed two different sunrises until
     * the rule lived in one place (committente, 3 set), and a widget that now prints
     * SEVERAL of them has that much more to disagree about.
     */
    private suspend fun moments(
        context: Context,
        city: City,
        zone: ZoneId,
        now: Instant,
        report: WeatherReport?,
        settings: AppSettings
    ): List<NextMoment> {
        val jobs = ServiceLocator.skySubscriptionStore(context).subscriptions.first()
            .filter { it.enabled }
            .mapNotNull { SkyJobCatalog.byId(it.jobId) }
        if (jobs.isEmpty()) return emptyList()
        val hours = report?.hourly.orEmpty()
        val dataAge = report?.let { Duration.between(it.systemInfo.lastSync, now) }
        val staleAfter = WeatherFreshness.staleAfter(settings.updateFrequencyMin)
        return SkyUpcoming.allAt(jobs, now, zone, city.coordinates)
            .take(MaxMoments)
            .mapNotNull { upcoming ->
                val at: SkyOccurrence.At = upcoming.at ?: return@mapNotNull null
                val verdict = if (at.job.observable) {
                    SkyVerdictEngine.evaluate(
                        job = at.job,
                        start = at.start,
                        end = at.end,
                        hours = hours,
                        zone = zone,
                        coordinates = city.coordinates,
                        dataAge = dataAge,
                        staleAfter = staleAfter
                    )
                } else {
                    null
                }
                NextMoment(at.job, at.start, at.end, verdict, upcoming.inProgress)
            }
    }

    /**
     * How many moments are worth resolving at all. Six compact rows under the hero
     * need a widget five cells tall, which no launcher grid offers; past that the
     * only thing another verdict buys is battery spent on a row nobody can see.
     */
    private const val MaxMoments = 6
}
