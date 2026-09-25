package com.callbackdev.chiaro.readme

import android.content.Context
import com.callbackdev.chiaro.data.AppSettings
import com.callbackdev.chiaro.data.SkySubscription
import com.callbackdev.chiaro.data.mapper.WeatherReportMapper
import com.callbackdev.chiaro.data.remote.dto.AirQualityResponseDto
import com.callbackdev.chiaro.data.remote.dto.ForecastResponseDto
import com.callbackdev.chiaro.data.remote.dto.GeocodingResponseDto
import com.callbackdev.chiaro.data.warnings.CapParser
import com.callbackdev.chiaro.data.warnings.DpcBulletinSource
import com.callbackdev.chiaro.data.warnings.PlaceWarningState
import com.callbackdev.chiaro.data.warnings.WarningZoneAssets
import com.callbackdev.chiaro.domain.model.CacheStatus
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.model.WeatherReport
import com.callbackdev.chiaro.domain.placeZone
import com.callbackdev.chiaro.domain.sky.SkyJobCatalog
import com.callbackdev.chiaro.domain.warnings.DpcBulletinReader
import com.callbackdev.chiaro.domain.warnings.OfficialWarningEngine
import com.callbackdev.chiaro.domain.warnings.WarningLevel
import com.callbackdev.chiaro.domain.rules.MaxRules
import com.callbackdev.chiaro.domain.rules.NotificationRule
import com.callbackdev.chiaro.ui.alerts.AlertsUiState
import com.callbackdev.chiaro.ui.alerts.RuleCardModel
import com.callbackdev.chiaro.ui.alerts.RuleText
import com.callbackdev.chiaro.ui.sky.SkyStateBuilder
import com.callbackdev.chiaro.ui.sky.SkyUiState
import com.callbackdev.chiaro.ui.today.TodayStateBuilder
import com.callbackdev.chiaro.ui.today.TodayUiState
import com.callbackdev.chiaro.widget.WidgetData
import com.callbackdev.chiaro.widget.WidgetKind
import com.callbackdev.chiaro.widget.WidgetLook
import com.callbackdev.chiaro.widget.WidgetModel
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Milan as Open-Meteo and the Protezione Civile served it, recorded by
 * `tools/record_readme_data.py` into `src/test/resources/readme/`: the README's pictures
 * are drawn from real responses put through the app's own pipeline (the DTOs, the
 * mapper, the state builders), never from numbers made up to look good.
 *
 * The screens are drawn five minutes after the recording, as somebody opening the app
 * just after its refresh would see them. The settings are a fresh install's: the
 * pictures show the app a new reader gets, and follow its defaults when they change.
 */
internal object MilanRecording {

    private val json = Json { ignoreUnknownKeys = true }

    private fun text(name: String): String =
        checkNotNull(javaClass.getResource("/readme/$name")) {
            "src/test/resources/readme/$name is missing: run tools/record_readme_data.py"
        }.readText()

    val fetchedAt: Instant = Instant.parse(
        json.parseToJsonElement(text("recording.json")).jsonObject.getValue("fetchedAt").jsonPrimitive.content
    )

    /** The moment every screen is drawn at. */
    val now: Instant = fetchedAt.plus(Duration.ofMinutes(5))

    val settings = AppSettings()

    /** The geocoder's first answer, mapped as `WeatherRepository` maps a search result. */
    val city: City = json.decodeFromString(GeocodingResponseDto.serializer(), text("geocoding.json"))
        .results.first()
        .let { place ->
            City(
                id = place.id,
                name = place.name,
                region = place.admin1,
                country = place.country ?: place.countryCode,
                coordinates = Coordinates(place.latitude, place.longitude),
                timezone = place.timezone,
                countryCode = place.countryCode,
                admin3 = place.admin3
            )
        }

    val report: WeatherReport = WeatherReportMapper.map(
        city = city,
        forecast = json.decodeFromString(ForecastResponseDto.serializer(), text("forecast.json")),
        airQuality = json.decodeFromString(AirQualityResponseDto.serializer(), text("air-quality.json")).current,
        fetchedAt = fetchedAt,
        responseTimeMs = 180,
        cacheStatus = CacheStatus.MISS
    )

    /** The bulletin for Milan's zone, as `OfficialWarningReader.state` answers it. */
    fun warnings(context: Context): PlaceWarningState {
        val zone = WarningZoneAssets.load(context).locate(city.coordinates, city.admin3)
            ?: return PlaceWarningState.Unavailable
        val bulletin = DpcBulletinReader.toBulletin(CapParser.parse(text("bulletin.xml").byteInputStream()))
        val today = LocalDate.ofInstant(now, DpcBulletinSource.ZONE)
        return OfficialWarningEngine.forPlace(bulletin, zone, today)
            ?.let { PlaceWarningState.Current(it) }
            ?: PlaceWarningState.Stale(zone, bulletin.issuedAt)
    }

    fun today(context: Context): TodayUiState.Content {
        val graded = (warnings(context) as? PlaceWarningState.Current)?.warnings
            ?.takeIf { it.maxLevel != WarningLevel.NONE }
        val state = TodayStateBuilder.build(
            city = city,
            report = report,
            now = now,
            updateFrequencyMin = settings.updateFrequencyMin,
            userRefreshing = false,
            error = null,
            warnings = graded
        )
        return checkNotNull(state as? TodayUiState.Content) {
            "the recording no longer covers its own moment: re-run tools/record_readme_data.py"
        }
    }

    /** A fresh install's subscriptions: the catalog's defaults, as the store seeds them. */
    private val subscriptions = SkyJobCatalog.defaults.map { SkySubscription(it.id) }

    fun sky(): SkyUiState.Content = SkyStateBuilder.build(
        city = city,
        report = report,
        subscriptions = subscriptions,
        settings = settings,
        now = now
    )

    /**
     * What a widget of [kind] knows, as `WidgetData.load` builds it: Today's own state
     * over the report, the moments judged by `WidgetData.momentsFor`, and the look a
     * newly placed widget of that kind wears.
     */
    fun widget(context: Context, kind: WidgetKind): WidgetModel {
        val zone = placeZone(report, city)
        return WidgetModel(
            settings = settings,
            look = WidgetLook.defaultsFor(kind),
            city = city,
            fromGps = false,
            content = today(context),
            moments = WidgetData.momentsFor(
                jobs = subscriptions.mapNotNull { SkyJobCatalog.byId(it.jobId) },
                city = city,
                zone = zone,
                now = now,
                report = report,
                settings = settings
            ),
            zone = zone,
            warning = (warnings(context) as? PlaceWarningState.Current)?.warnings
                ?.takeIf { it.maxLevel != WarningLevel.NONE }
        )
    }

    /**
     * Two rules a reader makes with two taps: the Bike and the Run ideas, created exactly
     * as `AlertsViewModel.addFromTemplate` creates them (the idea's name and message, its
     * conditions, switched on). The app's own words, so the picture puts no sentence in a
     * reader's mouth; never fired yet, because nothing has run.
     */
    fun yourRules(context: Context): List<NotificationRule> =
        listOf(RuleText.templates[0], RuleText.templates[2]).mapIndexed { index, template ->
            NotificationRule(
                id = index + 1L,
                name = context.getString(template.nameRes),
                conditions = template.conditions,
                message = context.getString(template.messageRes)
            )
        }

    fun alerts(context: Context, rules: List<NotificationRule> = emptyList()): AlertsUiState.Content = AlertsUiState.Content(
        placeName = city.name,
        notifications = settings.notifications,
        rules = rules.map { RuleCardModel(it, lastFired = null) },
        canAdd = rules.size < MaxRules,
        units = settings.units,
        zone = placeZone(report, city),
        warnings = warnings(context)
    )
}
