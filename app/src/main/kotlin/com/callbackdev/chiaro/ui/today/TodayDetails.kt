package com.callbackdev.chiaro.ui.today

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.BoxWithConstraints
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.model.AqiScale
import com.callbackdev.chiaro.domain.model.CloudLayers
import com.callbackdev.chiaro.domain.model.DailyForecast
import com.callbackdev.chiaro.domain.model.PollenLevel
import com.callbackdev.chiaro.domain.settings.UnitSettings
import com.callbackdev.chiaro.ui.components.MetricTile
import com.callbackdev.chiaro.ui.components.SegmentGap
import com.callbackdev.chiaro.ui.components.TrackScale
import com.callbackdev.chiaro.ui.components.airTrack
import com.callbackdev.chiaro.ui.components.euAirTrack
import com.callbackdev.chiaro.ui.components.gridSegmentShape
import com.callbackdev.chiaro.ui.components.humidityTrack
import com.callbackdev.chiaro.ui.components.pollenTrack
import com.callbackdev.chiaro.ui.components.pressureTrack
import com.callbackdev.chiaro.ui.components.rainTrack
import com.callbackdev.chiaro.ui.components.uvTrack
import com.callbackdev.chiaro.ui.format.Formats
import com.callbackdev.chiaro.ui.icons.MetricIcons
import com.callbackdev.chiaro.ui.icons.WeatherIconSize
import com.callbackdev.chiaro.ui.sky.SkyText
import com.callbackdev.chiaro.ui.theme.reflowForText
import java.util.Locale
import kotlin.math.roundToInt

/** The day's strongest gust is news from here: «strong» on the wind tile's own ladder. */
internal const val DayGustNewsKph = 39.0

/** …and when it is clearly above what the tile already prints. */
internal const val DayGustMarginKph = 5.0

/**
 * The details grid (DESIGN §8.6), reviewed card by card on 8 set 2026. What each tile
 * carries beyond its value and its meaning, and why:
 *
 * - **UV, humidity, air, pressure, pollen**: a track on the scale the world uses (0–11,
 *   0–100, 0–300, 980–1046 around 1013, four levels), each in its own hue with a disc on
 *   the value (design review, 23 set 2026), so "where on the scale" arrives before the
 *   number is read. Visibility is logarithmic and the wind's meaning is the gust, and
 *   neither gets one.
 * - **Wind**: the words for where it comes from, and the gusts on the days they matter,
 *   when the wind a body feels is the gust.
 * - **Humidity**: one tile where there were two. The dew point tile said "pleasant"
 *   under the humidity tile's "comfortable", the same fact in two cards; the dew point
 *   is the better predictor of how the air feels, so it writes the meaning line and
 *   stays on the tile as a note, and "Rugiada" — the least understood word on the
 *   screen — stops being a headline.
 * - **Air**: the index without its acronym. "AQI" was the one piece of jargon on a
 *   screen built to have none; the label already says what the number is.
 * - **Pollen**: which pollen. "High" tells an allergic reader nothing to act on,
 *   "grass" does.
 */
@Composable
internal fun Details(content: TodayUiState.Content, units: UnitSettings, locale: Locale) {
    val report = content.report
    val current = report.current
    val today = report.daily.firstOrNull()
    val tiles = buildList {
        // The day's rain first (24 set 2026): how much, over how long, and — while the
        // report is fresh — what fell in the hour just closed. The one number on this grid
        // that answers "do I need the umbrella" rather than "what is it like".
        //
        // The rain ALONE (24 set 2026), like the week's open day: the total carries the
        // snow's water too. On a day with snow in it the hours and the hour just closed
        // stay off this tile, because both count the snow as well and it has its own.
        val snowy = (today?.snowCm ?: 0.0) >= 0.1
        today?.rainMm?.let { mm ->
            val lastHour = current.precipitation.pastHours.firstOrNull()
                ?.takeIf { !current.estimated && !snowy && it.precipMm > 0.0 }
            val hours = today.precipHours?.takeIf { !snowy && mm >= 0.2 && it >= 1.0 }
            add(
                Tile(
                    kind = DetailKind.RAIN,
                    icon = { MetricIcons.rain },
                    label = R.string.metric_rain,
                    value = Formats.millimetres(mm, locale),
                    meaning = WeatherText.rainMeaning(mm),
                    track = rainTrack(WeatherText.rainBand(mm), WeatherText.RAIN_BANDS),
                    note = listOfNotNull(
                        hours?.let {
                            pluralStringResource(
                                R.plurals.rain_hours_note, it.roundToInt(), Formats.hours(it, locale)
                            )
                        },
                        lastHour?.let {
                            stringResource(R.string.rain_last_hour, Formats.millimetres(it.precipMm, locale))
                        }
                    ).joinToString(" · ").ifEmpty { null }
                        ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
                )
            )
        }
        // Snow only on a day that has some: a «0 cm» every day of the year would be the
        // tile saying nothing about nothing.
        today?.snowCm?.takeIf { it >= 0.1 }?.let { cm ->
            add(
                Tile(
                    kind = DetailKind.SNOW,
                    icon = { MetricIcons.snow },
                    label = R.string.metric_snow,
                    value = Formats.centimetres(cm, locale),
                    meaning = WeatherText.snowMeaning(cm),
                    track = rainTrack(WeatherText.snowBand(cm), WeatherText.SNOW_BANDS)
                )
            )
        }
        // The UV tile says NOW, like every other tile in this grid (20 set 2026).
        //
        // It printed `uv_index_max` — the day's PEAK — under the bare label "UV" and
        // under a meaning line written for the present: at 23:00 on a July day it read
        // «UV 8 — Scotta in circa 25 minuti, copriti», advice about a sun that had set
        // four hours earlier. `DailyForecast.uvIndexMax` defends the peak "under a Today
        // heading" and it is right to; this grid is not that heading, it is the wind,
        // the humidity and the pressure of this minute.
        //
        // So the value is the current index — fetched since the first commit and
        // rendered nowhere until now — and the peak rides along as a note, the way the
        // wind tile carries its gusts: the planning number kept, the consequence line
        // no longer wearing it.
        //
        // The tile is still drawn at night, deliberately. §1.2: a metric whose value
        // asks nothing of the reader today still gets its band's line, because
        // "nothing to do about it" is also an answer — and «UV 0, nessuna protezione
        // necessaria» at midnight is exactly that answer, where the old line was a
        // different one about a different hour.
        // No tile when the model carries no index (24 set 2026): the screen draws no
        // section it has no data for, and a «UV 0» would be a sun that cannot burn.
        current.uvIndex?.let { uvNow ->
            add(
                Tile(
                    kind = DetailKind.UV,
                    icon = { MetricIcons.uv },
                    label = R.string.metric_uv,
                    value = uvNow.toString(),
                    meaning = WeatherText.uvMeaning(uvNow),
                    track = uvTrack(uvNow),
                    // Only while it is news: a peak that equals the reading is the tile
                    // printing its own number twice.
                    note = today?.uvIndexMax?.takeIf { it > uvNow }
                        ?.let { stringResource(R.string.uv_peak_today, it) }
                )
            )
        }
        val wind = current.wind
        val gusty = WeatherText.gustsMaterial(wind.speedKph, wind.gustKph)
        add(
            Tile(
                kind = DetailKind.WIND,
                icon = { MetricIcons.wind },
                label = R.string.metric_wind,
                value = Formats.wind(wind.speedKph, units.windSpeed, locale),
                // On a gusty day the wind a body feels is the gust, and the advice
                // follows it: "hold on to your hat" over a steady 20 km/h is the gust
                // talking, and it is right.
                meaning = WeatherText.windMeaning(if (gusty) wind.gustKph else wind.speedKph),
                detail = { WindDirection(fromDegrees = wind.degree) },
                // The day's strongest gust joins the note (24 set 2026) when it is news:
                // strong on the tile's own ladder (39 km/h, «tieni il cappello») and
                // clearly above what the tile already shows. A calm morning before a
                // windy afternoon said nothing about the afternoon until then.
                note = run {
                    val shownNow = if (gusty) wind.gustKph else wind.speedKph
                    val dayMax = today?.gustMaxKph
                        ?.takeIf { it >= DayGustNewsKph && it >= shownNow + DayGustMarginKph }
                    val max = dayMax?.let { Formats.wind(it, units.windSpeed, locale) }
                    val now = Formats.wind(wind.gustKph, units.windSpeed, locale)
                    when {
                        gusty && max != null -> stringResource(R.string.wind_gusts_and_max, now, max)
                        gusty -> stringResource(R.string.wind_gusts, now)
                        max != null -> stringResource(R.string.wind_gust_max_today, max)
                        else -> null
                    }
                }
            )
        )
        // The sky by layer (24 set 2026): the total says how much is covered, the layer
        // says what that looks like — 90% of high veil still lets the sun through. No
        // track: the meaning turns on the layer as much as on the percentage, and a
        // 0–100 line would draw a veiled sun and a grey day as the same place.
        current.cloudCoverPct?.let { total ->
            val layer = current.cloudLayers?.dominant()
            add(
                Tile(
                    kind = DetailKind.CLOUD,
                    icon = { MetricIcons.cloud },
                    label = R.string.metric_cloud,
                    value = Formats.percent(total, locale),
                    meaning = WeatherText.cloudMeaning(total, layer, content.night),
                    note = layer?.takeIf { total >= CloudLayers.MIN_PCT }
                        ?.let { stringResource(WeatherText.cloudLayerNote(it)) }
                )
            )
        }
        add(
            Tile(
                kind = DetailKind.HUMIDITY,
                icon = { MetricIcons.humidity },
                label = R.string.metric_humidity,
                value = Formats.percent(current.humidityPct, locale),
                meaning = WeatherText.dewPointMeaning(current.dewPointC),
                track = humidityTrack(current.humidityPct),
                note = stringResource(
                    R.string.humidity_dew_note,
                    Formats.temperature(current.dewPointC, units.temperature, locale)
                )
            )
        )
        // Where the barometer is going, when it is going somewhere (12 ott 2026): the
        // meaning follows the tendency and the note carries its arithmetic; a steady
        // barometer keeps the absolute reading's meaning and no note.
        val trend = WeatherText.pressureTrend(report.hourly)
        add(
            Tile(
                kind = DetailKind.PRESSURE,
                icon = { MetricIcons.pressure },
                label = R.string.metric_pressure,
                value = Formats.pressure(current.pressureMb, locale),
                meaning = WeatherText.pressureMeaning(current.pressureMb, trend),
                track = pressureTrack(current.pressureMb),
                note = trend?.takeIf { it.direction != WeatherText.PressureTrend.Direction.STEADY }
                    ?.let { stringResource(R.string.pressure_trend_note, String.format(locale, "%+.1f", it.deltaHpa)) }
            )
        )
        // §1.1 again (Fase 26): `visibility` is a model-dependent field, and a model
        // that does not carry it gets no tile rather than a tile reading 0 km.
        current.visibilityKm?.let { km ->
            add(
                Tile(
                    kind = DetailKind.VISIBILITY,
                    icon = { MetricIcons.visibility },
                    label = R.string.metric_visibility,
                    value = Formats.kilometers(km, locale),
                    meaning = WeatherText.visibilityMeaning(km)
                )
            )
        }
        // §1.1: data the provider does not have for here is not drawn — no dashes.
        // On the scale the place reads (24 set 2026): the EEA's in Europe, the US one
        // elsewhere — each with its own bands and track.
        report.airQuality?.let { air ->
            add(
                Tile(
                    kind = DetailKind.AIR,
                    icon = { MetricIcons.air },
                    label = R.string.metric_air,
                    value = air.shownIndex.toString(),
                    meaning = WeatherText.airMeaning(air),
                    track = if (air.scale == AqiScale.EUROPEAN) euAirTrack(air.shownIndex) else airTrack(air.shownIndex)
                )
            )
        }
        report.pollen?.let { pollen ->
            val worst = WeatherText.pollenWorst(pollen)
            val families = WeatherText.pollenFamiliesAtWorst(pollen).map { stringResource(it) }
            add(
                Tile(
                    kind = DetailKind.POLLEN,
                    icon = { MetricIcons.pollen },
                    label = R.string.metric_pollen,
                    value = stringResource(WeatherText.pollenLevel(worst)),
                    meaning = WeatherText.pollenMeaning(worst),
                    track = pollenTrack(worst.ordinal, PollenLevel.entries.size),
                    // The families are stored lowercase so they can be listed; the
                    // line then starts like a sentence.
                    note = when (families.size) {
                        0 -> null
                        1 -> families[0]
                        2 -> stringResource(R.string.list_two, families[0], families[1])
                        else -> stringResource(
                            R.string.list_three, families[0], families[1], families[2]
                        )
                    }?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
                )
            )
        }
    }
    // Today's one tile to notice, if any (`notableDetail`): the grid keeps its order.
    val notable = notableDetail(
        rainMm = today?.rainMm,
        snowCm = today?.snowCm,
        windShownKph = current.wind.let {
            if (WeatherText.gustsMaterial(it.speedKph, it.gustKph)) it.gustKph else it.speedKph
        },
        dayGustKph = today?.gustMaxKph,
        air = report.airQuality,
        uvNow = current.uvIndex,
        pollen = report.pollen?.let { WeatherText.pollenWorst(it) },
        visibilityKm = current.visibilityKm,
        dewPointC = current.dewPointC
    )
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        // An estimated now says so where its numbers are (DESIGN §1.1, 24 set 2026): the
        // tiles below are the forecast for this hour, and the air and pollen still what
        // was measured when the data arrived.
        if (current.estimated) {
            Text(
                text = stringResource(R.string.estimate_details, freshnessAge(content.lastSync)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
        // §10: two columns is a budget, not a layout. At 150% the label beside the
        // 34dp icon has 84dp for a word that wants 115, and the tile becomes three
        // wrapped lines of two words. One column keeps the pair rule (a row of one is
        // still a row) and gives the label the whole width it needed all along.
        // Wider than a phone, as many tiles a row as the width holds at a phone's tile
        // (DESIGN §8.12c): the grid spans a wide page under its two panes.
        val reflow = reflowForText()
        BoxWithConstraints {
        val perRow = if (reflow) 1 else detailsPerRow(tiles.size, maxWidth)
        val rows = tiles.chunked(perRow)
        // One group, as the Sky's and Alerts' rows are (review of Today, 15 ott 2026,
        // committente): the tiles are segments of a grid 2dp apart, the grid's four outer
        // corners round and every corner where two tiles meet tight, so the details read
        // as one block of facts rather than nine loose cards 12dp apart.
        Column(verticalArrangement = Arrangement.spacedBy(SegmentGap)) {
            rows.forEachIndexed { rowIndex, rowTiles ->
                // The pair shares one height: two cards whose bottoms disagree read as a
                // misalignment, not as content of different lengths (device check, 2 set).
                Row(
                    horizontalArrangement = Arrangement.spacedBy(SegmentGap),
                    modifier = Modifier.height(IntrinsicSize.Max)
                ) {
                    // An odd tile out takes the whole row (review of 12 ott 2026): it used
                    // to sit beside an empty box, half a row of nothing at the foot of the
                    // grid, which read as a tile that had failed to load.
                    rowTiles.forEachIndexed { colIndex, tile ->
                        MetricTile(
                            icon = tile.icon(),
                            label = stringResource(tile.label),
                            value = tile.value,
                            meaning = stringResource(tile.meaning),
                            track = tile.track,
                            detail = tile.detail,
                            note = tile.note,
                            notable = tile.kind == notable,
                            shape = gridSegmentShape(rowIndex, rows.size, colIndex, rowTiles.size),
                            modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = DetailTileMinHeight)
                        )
                    }
                }
            }
        }
        }
    }
}

/**
 * How many tiles a row of the details holds in [width] (pure, `DetailsPerRowTest`): as many as
 * fit at [DetailTileMinWidth], a phone's tile, between two and [DetailsMaxPerRow]. Of the
 * counts that fit, the largest that leaves **no short last row**, so nine tiles stand three by
 * three rather than four, four and one, whose odd tile would run the whole width of a tablet;
 * when none divides the tiles evenly, the most that fit. A phone always gets its two.
 */
internal fun detailsPerRow(count: Int, width: Dp): Int {
    val fit = (width / DetailTileMinWidth).toInt().coerceIn(2, DetailsMaxPerRow)
    return (fit downTo 2).firstOrNull { count % it == 0 } ?: fit
}

/** A detail tile's narrowest on a wide page: about a phone's, (360 - 32 - 2) / 2. */
internal val DetailTileMinWidth: Dp = 160.dp

/** Past four the tiles of one row are too far apart to compare. */
internal const val DetailsMaxPerRow = 4

/**
 * Where the wind comes from, in words alone (DESIGN §8.6; committente, 11 set 2026). The
 * eighth of the compass is `SkyText`'s vocabulary — the same "north-east" the Sky screen
 * says for a rainbow — because a general audience reads "from the north-east" and decodes
 * "NNE"; the 16-point label the model carries stays in the data.
 *
 * There was an arrow beside it, hand-drawn and then Meteocons' needle. It turned by the
 * true degrees, so it said the direction more precisely than the words; it went because at
 * 16 to 18dp it was not good to look at, and in a tile where everything else is a line of
 * text it was the one thing noticed for the wrong reason. The direction was already
 * written, so only a drawing was lost.
 */
@Composable
internal fun WindDirection(fromDegrees: Int) {
    Text(
        text = stringResource(
            R.string.wind_from,
            stringResource(SkyText.bearingRes(fromDegrees.toDouble()))
        ),
        style = MaterialTheme.typography.bodyMedium
    )
}

internal data class Tile(
    /** Which quantity, for which tile is today's to notice. */
    val kind: DetailKind,
    /** The eyebrow's glyph ([MetricIcons]). */
    val icon: @Composable () -> androidx.compose.ui.graphics.vector.ImageVector,
    val label: Int,
    val value: String,
    val meaning: Int,
    /** The metric's world scale, when it has one (DESIGN §8.6). */
    val track: TrackScale? = null,
    /** A composed fact about the value (where the wind comes from). */
    val detail: (@Composable () -> Unit)? = null,
    /** A printed fact about the value (gusts, dew point, which pollen). */
    val note: String? = null
)

/**
 * Every row of the grid at least this tall (second review of Today, 15 ott 2026): the tallest
 * tile a day commonly draws — the eyebrow, the reading, a track, a note and the meaning, 158dp
 * at 100% type — so a row of two short tiles stands as tall as a row with a scale in it, and
 * the grid reads as a grid. A tile with more to say grows past it.
 */
private val DetailTileMinHeight = 160.dp

