package com.callbackdev.chiaro.widget.arc

import android.content.Context
import android.graphics.Bitmap
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.ui.format.Formats
import com.callbackdev.chiaro.ui.icons.ChiaroIcons
import com.callbackdev.chiaro.ui.today.TodayUiState
import com.callbackdev.chiaro.widget.ChiaroWidgetReceiver
import com.callbackdev.chiaro.widget.DayRange
import com.callbackdev.chiaro.widget.NoDataContent
import com.callbackdev.chiaro.widget.NoPlaceContent
import com.callbackdev.chiaro.domain.sky.SkyVerdict
import com.callbackdev.chiaro.widget.PlaceLine
import com.callbackdev.chiaro.widget.StaleSp
import com.callbackdev.chiaro.widget.WarningChipGap
import com.callbackdev.chiaro.widget.WarningChipRow
import com.callbackdev.chiaro.widget.WidgetCard
import com.callbackdev.chiaro.widget.WidgetCardPadding
import com.callbackdev.chiaro.widget.WidgetData
import com.callbackdev.chiaro.widget.WidgetModel
import com.callbackdev.chiaro.widget.WidgetPalette
import com.callbackdev.chiaro.widget.WidgetRefresh
import com.callbackdev.chiaro.widget.fontScale
import com.callbackdev.chiaro.widget.rememberSkyBitmap
import com.callbackdev.chiaro.widget.rememberWidgetModel
import com.callbackdev.chiaro.widget.rememberWidgetSchemes
import com.callbackdev.chiaro.widget.secondaryStyle
import com.callbackdev.chiaro.widget.sentence
import com.callbackdev.chiaro.widget.staleText
import com.callbackdev.chiaro.widget.verdictInk
import java.time.Instant
import java.util.Locale
import kotlin.math.roundToInt

class ArcWidgetReceiver : ChiaroWidgetReceiver() {
    override val glanceAppWidget = ArcWidget()
}

/**
 * The day's arc (9 set 2026): the sun's path over the reader's place, drawn from the
 * same astronomy the app computes its sky with; the sky of every hour as bands under
 * it; the moon's path and its real phase; the rain rising from the ground; and, in
 * words, the next light moment with its time and its countdown, the agenda of what
 * follows — with the verdict the Sky screen would give, where the reader follows that
 * moment — and, on the tallest card, the week.
 *
 * It resizes from one cell to sixteen and reshapes itself at every step ([ArcLayout.kt]):
 * a dial, a strip, a card, a panel, a board. What it draws is its own settings
 * ([ArcSettings]), per instance, from the launcher's reconfigure flow. Like the other
 * three widgets it never invents: no place and no data are said, old data states its
 * age, and every number is the cached report's.
 */
class ArcWidget : GlanceAppWidget() {

    /** Exact sizing: the form, the plot's box and the row count are all read off the
     * size the launcher really granted. */
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = runCatching {
            GlanceAppWidgetManager(context).getAppWidgetId(id)
        }.getOrDefault(0)
        // The revision is read BEFORE the loads, so what is composed below is only
        // re-read when something really changed after it (see [WidgetRefresh]).
        val loadedAt = WidgetRefresh.revision.value
        val initial = WidgetData.load(context, appWidgetId)
        val arcInitial = ArcSettingsStore.get(context).settingsFor(appWidgetId)
        provideContent {
            val model = rememberWidgetModel(context, appWidgetId, initial, loadedAt)
            val arc = rememberArcSettings(context, appWidgetId, arcInitial, loadedAt)
            val schemes = rememberWidgetSchemes(
                context, model.settings.dynamicColor, model.settings.palette
            )
            val skyBitmap = rememberSkyBitmap(model)
            val size = LocalSize.current
            val content = model.content
            val city = model.city
            val series = if (content != null && city != null) {
                remember(content, arc, model.moments) {
                    ArcSeries.build(content, model.moments, city.coordinates, Instant.now(), arc)
                }
            } else {
                null
            }
            val plan = if (content != null && series != null) {
                arcPlan(
                    size, fontScale(context), arc,
                    stale = content.isStale,
                    agendaAvailable = series.events.size,
                    weekAvailable = content.week.isNotEmpty(),
                    heroIsHeadline = heroIsHeadline(arc, series),
                    warningLevel = model.warning?.maxLevel
                )
            } else {
                null
            }
            WidgetCard(
                model, schemes, skyBitmap,
                contentPaddingStart = plan?.paddingHorizontal ?: WidgetCardPadding,
                contentPaddingEnd = plan?.paddingHorizontal ?: WidgetCardPadding,
                contentPaddingTop = plan?.paddingVertical ?: WidgetCardPadding,
                contentPaddingBottom = plan?.paddingVertical ?: WidgetCardPadding
            ) { palette ->
                when {
                    content == null && city == null -> NoPlaceContent(palette)
                    content == null || series == null || plan == null -> NoDataContent(palette)
                    else -> ArcContent(model, content, arc, series, plan, palette)
                }
            }
        }
    }
}

/** The arc's own settings, re-read inside the composition when [WidgetRefresh] ticks —
 * the same discipline as [rememberWidgetModel], for the same reason. */
@Composable
private fun rememberArcSettings(
    context: Context,
    appWidgetId: Int,
    initial: ArcSettings,
    loadedAt: Long
): ArcSettings {
    val revision by WidgetRefresh.revision.collectAsState()
    val settings by produceState(initial, revision) {
        if (revision != loadedAt) value = ArcSettingsStore.get(context).settingsFor(appWidgetId)
    }
    return settings
}

/**
 * The plot as a bitmap for this exact box, cached on everything it depends on: a
 * recomposition that changed nothing must not allocate another 850 × 330 image.
 */
@Composable
private fun rememberArcBitmap(
    model: WidgetModel,
    arc: ArcSettings,
    series: ArcSeries,
    plan: ArcPlan,
    palette: WidgetPalette
): Bitmap {
    val context = LocalContext.current
    val density = density(context)
    val fontScale = fontScale(context)
    val is24h = DateFormat.is24HourFormat(context)
    val locale = Locale.getDefault()
    return remember(series, arc, plan, palette, density, fontScale, is24h, locale, model.settings.units) {
        paintArc(context, model, arc, series, plan, palette)
    }
}

/** Read off a Context for the reason [fontScale] is: there is no `LocalDensity` on the
 * launcher's side of the fence. */
private fun density(context: Context): Float = context.resources.displayMetrics.density

/**
 * The plot for [plan]'s box, as the launcher and the configuration preview both draw
 * it — one function, so the preview cannot show a picture the widget will not. Reads
 * density, font scale and the clock format off the [context] it is given.
 */
internal fun paintArc(
    context: Context,
    model: WidgetModel,
    arc: ArcSettings,
    series: ArcSeries,
    plan: ArcPlan,
    palette: WidgetPalette
): Bitmap {
    val density = density(context)
    return ArcPainter.paint(
        series, arc,
        ArcCanvasSpec(
            widthPx = (plan.graphic.width.value * density).roundToInt().coerceAtLeast(1),
            heightPx = (plan.graphic.height.value * density).roundToInt().coerceAtLeast(1),
            density = density,
            fontScale = fontScale(context),
            textScale = plan.textScale,
            hourLabels = plan.hourLabels,
            temperatures = plan.temperatures,
            tickStepHours = plan.tickStepHours,
            is24Hour = DateFormat.is24HourFormat(context),
            locale = Locale.getDefault(),
            temperatureUnit = model.settings.units.temperature,
            zone = model.zone
        ),
        ArcInks(
            primary = palette.primary.getColor(context).toArgb(),
            secondary = palette.secondary.getColor(context).toArgb(),
            darkGround = palette.darkGround,
            sky = palette.dress.sky,
            colors = palette.colors
        )
    )
}

@Composable
private fun ArcContent(
    model: WidgetModel,
    content: TodayUiState.Content,
    arc: ArcSettings,
    series: ArcSeries,
    plan: ArcPlan,
    palette: WidgetPalette
) {
    val bitmap = rememberArcBitmap(model, arc, series, plan, palette)
    when (plan.form) {
        ArcForm.DIAL -> DialContent(model, content, arc, series, plan, palette, bitmap)
        ArcForm.STRIP ->
            if (plan.stacked) {
                StackedStripContent(model, content, arc, series, plan, palette, bitmap)
            } else {
                RowStripContent(model, content, arc, series, plan, palette, bitmap)
            }
        ArcForm.CARD -> CardContent(model, content, arc, series, plan, palette, bitmap)
        ArcForm.PANEL, ArcForm.BOARD -> PanelContent(model, content, arc, series, plan, palette, bitmap)
    }
}

/** The plot, at the box the plan gave it. Its description is the next moment in words:
 * the picture's text equivalent (DESIGN §9.3). */
@Composable
private fun ArcGraphic(bitmap: Bitmap, plan: ArcPlan, series: ArcSeries, model: WidgetModel) {
    val context = LocalContext.current
    Image(
        provider = ImageProvider(bitmap),
        contentDescription = ArcText.description(context, series, model.zone),
        contentScale = ContentScale.FillBounds,
        modifier = GlanceModifier.width(plan.graphic.width).height(plan.graphic.height)
    )
}

/** The one-cell card: the arc, and under it one figure — the temperature, or the next
 * moment's time. A taller one-column card adds the place and the next moment. */
@Composable
private fun DialContent(
    model: WidgetModel,
    content: TodayUiState.Content,
    arc: ArcSettings,
    series: ArcSeries,
    plan: ArcPlan,
    palette: WidgetPalette,
    bitmap: Bitmap
) {
    val context = LocalContext.current
    val next = series.nextLight
    val figure = when {
        arc.dialFigure == ArcDialFigure.NEXT_TIME && next != null ->
            ArcText.clock(context, next.at, model.zone)
        else -> temperature(content, model)
    }
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ArcGraphic(bitmap, plan, series, model)
        Spacer(modifier = GlanceModifier.height(ArcGapTight))
        Text(
            text = figure,
            style = strongStyle(palette, DialFigureSp * plan.textScale),
            maxLines = 1
        )
        StaleLine(content, palette)
        if (plan.rows >= 2) {
            PlaceLine(content.city.name, model.fromGps, palette, (PlaceSp * plan.textScale).sp)
            next?.let {
                Text(
                    text = ArcText.heroShort(context, it, model.zone),
                    style = secondaryStyle(palette, (StripLineSp * plan.textScale).sp),
                    maxLines = 1
                )
            }
        }
    }
}

/** Two cells by one: the number and the next moment's time on one line, the arc under. */
@Composable
private fun StackedStripContent(
    model: WidgetModel,
    content: TodayUiState.Content,
    arc: ArcSettings,
    series: ArcSeries,
    plan: ArcPlan,
    palette: WidgetPalette,
    bitmap: Bitmap
) {
    val context = LocalContext.current
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = GlanceModifier.fillMaxWidth()
        ) {
            Text(
                text = temperature(content, model),
                style = strongStyle(palette, StripStackedTempSp * plan.textScale),
                maxLines = 1
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            val next = series.nextLight
            when {
                content.isStale -> Text(
                    text = staleText(context, content.lastSync, Instant.now()),
                    style = TextStyle(color = palette.stale, fontSize = StaleSp.sp),
                    maxLines = 1
                )
                arc.hero != ArcHero.NONE && next != null -> {
                    Image(
                        provider = ImageProvider(
                            ArcText.rowIconRes(
                                next.item.kind, next.at, model.iconStyle, palette.darkGround,
                                model.settings.palette
                            )
                        ),
                        contentDescription = ArcText.heroLabel(context, next.item),
                        modifier = GlanceModifier.size(StripGlyph)
                    )
                    Text(
                        text = ArcText.clock(context, next.at, model.zone),
                        style = secondaryStyle(palette, (PlaceSp * plan.textScale).sp),
                        maxLines = 1,
                        modifier = GlanceModifier.padding(start = 4.dp)
                    )
                }
                else -> PlaceLine(content.city.name, model.fromGps, palette, (PlaceSp * plan.textScale).sp)
            }
        }
        Spacer(modifier = GlanceModifier.height(ArcGapTight))
        ArcGraphic(bitmap, plan, series, model)
    }
}

/** Three or four cells by one: the words in a column, the arc beside them. */
@Composable
private fun RowStripContent(
    model: WidgetModel,
    content: TodayUiState.Content,
    arc: ArcSettings,
    series: ArcSeries,
    plan: ArcPlan,
    palette: WidgetPalette,
    bitmap: Bitmap
) {
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = GlanceModifier.fillMaxSize()
    ) {
        // The words: the number, then the moment's name, then — when the height holds a
        // third line — its clock and countdown; a taller font joins the last two into
        // «Tramonto · 19:42». Stale data takes the second line instead: the age of the
        // numbers outranks the moment (VISION §5.9).
        Column(modifier = GlanceModifier.width(StripTextColumn)) {
            Text(
                text = temperature(content, model),
                style = strongStyle(palette, StripTempSp * plan.textScale),
                maxLines = 1
            )
            val next = series.nextLight
            val lineSp = (StripLineSp * plan.textScale).sp
            when {
                content.isStale -> Text(
                    text = staleText(context, content.lastSync, Instant.now()),
                    style = TextStyle(color = palette.stale, fontSize = StaleSp.sp),
                    maxLines = 1
                )
                arc.hero == ArcHero.NEXT_MOMENT && next != null && plan.heroLines >= 2 -> {
                    Text(
                        text = ArcText.heroName(context, next),
                        style = TextStyle(color = palette.primary, fontSize = lineSp, fontWeight = FontWeight.Medium),
                        maxLines = 1
                    )
                    Text(
                        text = ArcText.clockLine(context, series, next, model.zone),
                        style = secondaryStyle(palette, lineSp),
                        maxLines = 1
                    )
                }
                arc.hero == ArcHero.NEXT_MOMENT && next != null -> Text(
                    text = ArcText.heroShort(context, next, model.zone),
                    style = secondaryStyle(palette, lineSp),
                    maxLines = 1
                )
                arc.hero == ArcHero.HEADLINE -> Text(
                    text = sentence(context, content, model.settings.units),
                    style = secondaryStyle(palette, lineSp),
                    maxLines = plan.heroLines
                )
                else -> PlaceLine(content.city.name, model.fromGps, palette, lineSp)
            }
        }
        Spacer(modifier = GlanceModifier.width(StripGraphicGap))
        ArcGraphic(bitmap, plan, series, model)
    }
}

/** Two cells wide, two or more tall: number, place, sentence, the arc, the agenda. */
@Composable
private fun CardContent(
    model: WidgetModel,
    content: TodayUiState.Content,
    arc: ArcSettings,
    series: ArcSeries,
    plan: ArcPlan,
    palette: WidgetPalette,
    bitmap: Bitmap
) {
    val context = LocalContext.current
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Text(
            text = temperature(content, model),
            style = strongStyle(palette, CardTempSp * plan.textScale),
            maxLines = 1
        )
        PlaceLine(content.city.name, model.fromGps, palette, (PlaceSp * plan.textScale).sp)
        StaleLine(content, palette)
        if (plan.heroLines > 0) {
            Text(
                text = heroText(context, model, content, arc, series),
                style = strongStyle(palette, CardHeroSp * plan.textScale),
                maxLines = plan.heroLines
            )
        }
        // Eight children at most in this column, against Glance's ten: the chip is one
        // of them, gap included (see [WarningChipRow]).
        model.warning?.maxLevel?.takeIf { plan.warning.drawn }?.let { level ->
            WarningChipRow(level, palette, if (plan.heroLines > 0) WarningChipGap else 0.dp)
        }
        Spacer(modifier = GlanceModifier.height(ArcGap))
        ArcGraphic(bitmap, plan, series, model)
        Agenda(model, series, plan, palette)
        Spacer(modifier = GlanceModifier.defaultWeight())
    }
}

/**
 * Three or four cells wide, two or more tall: the hero row — the number, then the
 * sentence with the place and the countdown under it, the day's range at the far edge
 * when asked — the arc with its hours, the agenda, and on four rows the week.
 */
@Composable
private fun PanelContent(
    model: WidgetModel,
    content: TodayUiState.Content,
    arc: ArcSettings,
    series: ArcSeries,
    plan: ArcPlan,
    palette: WidgetPalette,
    bitmap: Bitmap
) {
    val context = LocalContext.current
    val next = series.nextLight
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = GlanceModifier.fillMaxWidth()
        ) {
            Text(
                text = temperature(content, model),
                style = strongStyle(palette, PanelTempSp * plan.textScale),
                maxLines = 1
            )
            Column(modifier = GlanceModifier.padding(start = HeroGap).defaultWeight()) {
                if (plan.heroLines > 0) {
                    Text(
                        text = heroText(context, model, content, arc, series),
                        style = strongStyle(palette, HeroSp * plan.textScale),
                        maxLines = plan.heroLines
                    )
                }
                val subSp = ((if (plan.heroLines > 0) HeroSubSp else PlaceSp) * plan.textScale).sp
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlaceLine(content.city.name, model.fromGps, palette, subSp)
                    if (plan.heroLines > 0 && arc.hero == ArcHero.NEXT_MOMENT && next != null) {
                        Text(
                            text = Separator + ArcText.countdown(context, series.now, next.at),
                            style = secondaryStyle(palette, subSp),
                            maxLines = 1
                        )
                    }
                    if (content.isStale) {
                        Text(
                            text = Separator + staleText(context, content.lastSync, Instant.now()),
                            style = TextStyle(color = palette.stale, fontSize = subSp),
                            maxLines = 1
                        )
                    }
                }
            }
            if (model.look.showDayRange && series.highC != null && series.lowC != null) {
                Spacer(modifier = GlanceModifier.width(HeroGap))
                DayRange(series.highC, series.lowC, model.settings.units, palette)
            }
        }
        // A line of its own under the hero row, before the drawing. Seven children at
        // most in this column.
        model.warning?.maxLevel?.takeIf { plan.warning.drawn }?.let { level ->
            WarningChipRow(level, palette, WarningChipGap)
        }
        Spacer(modifier = GlanceModifier.height(ArcGap))
        ArcGraphic(bitmap, plan, series, model)
        Agenda(model, series, plan, palette)
        Spacer(modifier = GlanceModifier.defaultWeight())
        if (plan.week) WeekStrip(model, content, plan, palette)
    }
}

/** As many agenda rows as the plan found room for, soonest first. */
@Composable
private fun Agenda(model: WidgetModel, series: ArcSeries, plan: ArcPlan, palette: WidgetPalette) {
    if (plan.agendaRows <= 0) return
    val context = LocalContext.current
    val rowHeight = arcAgendaRowHeight(fontScale(context), plan.textScale)
    val rows = series.events.take(plan.agendaRows)
    // A slot for the mark on every row as soon as one row has a verdict, so the clocks
    // stand in a column (device report, 9 set 2026: «19:07» sat left of the «19:00»
    // above it, pushed by its own mark). The mark itself is quiet — see [AgendaMark].
    val marks = rows.any { it.verdict != null }
    // ONE child of the card's column whatever the row count, and the gaps between rows
    // as padding rather than spacers: Glance draws at most ten children per container
    // and drops the rest without a word. The first four-by-four on a device (9 set 2026)
    // showed three rows and no week — the header, the arc and six spacers had used the
    // ten, and the fourth row, the last spacer and the week were simply not there. The
    // Compose preview has no such limit, which is why it showed them.
    Column(modifier = GlanceModifier.fillMaxWidth().padding(top = ArcGap)) {
        rows.forEachIndexed { index, event ->
            val gap = if (index > 0) ArcRowGap else 0.dp
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(rowHeight + gap)
                    .padding(top = gap)
            ) {
                Image(
                    provider = ImageProvider(
                        ArcText.rowIconRes(
                            event.item.kind, event.at, model.iconStyle, palette.darkGround,
                            model.settings.palette
                        )
                    ),
                    contentDescription = null, // the words beside it say it
                    modifier = GlanceModifier.size(AgendaGlyph)
                )
                val name = ArcText.rowLabel(context, event.item)
                Text(
                    text = if (event.tomorrow) context.getString(R.string.arc_tomorrow_name, name) else name,
                    style = TextStyle(color = palette.primary, fontSize = (AgendaSp * plan.textScale).sp),
                    maxLines = 1,
                    modifier = GlanceModifier.padding(start = 8.dp).defaultWeight()
                )
                Text(
                    text = ArcText.clock(context, event.at, model.zone),
                    style = secondaryStyle(palette, (AgendaSp * plan.textScale).sp),
                    maxLines = 1,
                    modifier = GlanceModifier.padding(start = 8.dp)
                )
                if (marks) {
                    Spacer(modifier = GlanceModifier.width(MarkGap))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = GlanceModifier.width(AgendaMarkSlot)
                    ) {
                        event.verdict?.let { AgendaMark(it, palette) }
                    }
                }
            }
        }
    }
}

/**
 * The verdict beside an agenda row: the series' own mark — `✓ ~ ✗ ?`, a shape before it is
 * a color (DESIGN §2.3) — drawn ([ArcText.markRes]) in the verdict's ink, at the words'
 * x-height, and nothing else. The Sky widget's filled pill was here first and came back
 * from the device as «a punch in the eye» (committente, 9 set 2026): on that card the
 * verdict is the hero, on this one it is a note beside a time, and a note is set like the
 * words around it. The character came next and came back as «handwriting»: it was a
 * fallback font's, not Roboto's, hence the drawings ([ChiaroIcons.verdictMarkRes]), which
 * the Sky widget and the app's chip now share.
 *
 * The ink is picked for the CARD'S ground, not the phone's theme: the pill's pale container
 * on a dark card was the light theme's pair, chosen by `isNight`, on a card that is dark
 * whatever the phone is doing (the sky under its scrim, or a dark card). The dark set's
 * inks are the ones measured against a dark surface, so the mark is readable where it
 * sits — and a bare glyph in the wrong set was exactly what made the Sky widget grow its
 * container in the first place (4 set).
 */
@Composable
private fun AgendaMark(verdict: SkyVerdict, palette: WidgetPalette) {
    val context = LocalContext.current
    Image(
        provider = ImageProvider(ChiaroIcons.verdictMarkRes(verdict.kind)),
        // The word for it, so the row reads «Golden hour, 19:07, fail».
        contentDescription = context.getString(
            com.callbackdev.chiaro.ui.sky.SkyText.verdictWordRes(verdict.kind)
        ),
        colorFilter = ColorFilter.tint(verdictInk(verdict.kind, palette.darkGround, palette.dress)),
        modifier = GlanceModifier.size(AgendaMarkGlyph)
    )
}

/** The week at the foot of a four-row card: seven columns of day, glyph, high, low. */
@Composable
private fun WeekStrip(
    model: WidgetModel,
    content: TodayUiState.Content,
    plan: ArcPlan,
    palette: WidgetPalette
) {
    val context = LocalContext.current
    val locale = Locale.getDefault()
    val today = content.now.toLocalDate()
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        content.week.take(7).forEach { day ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = GlanceModifier.defaultWeight()
            ) {
                Text(
                    text = ArcText.dayLabel(context, day.forecast.date, today),
                    style = secondaryStyle(palette, (WeekDaySp * plan.textScale).sp),
                    maxLines = 1
                )
                Spacer(modifier = GlanceModifier.height(WeekInnerGap))
                Image(
                    provider = ImageProvider(
                        ChiaroIcons.conditionRes(
                            day.forecast.condition.wmoCode, false,
                            model.iconStyle, palette.darkGround, model.settings.palette
                        )
                    ),
                    contentDescription = null,
                    modifier = GlanceModifier.size(WeekIcon)
                )
                Spacer(modifier = GlanceModifier.height(WeekInnerGap))
                Text(
                    text = Formats.temperature(day.forecast.highC, model.settings.units.temperature, locale),
                    style = TextStyle(
                        color = palette.primary,
                        fontSize = (WeekHighSp * plan.textScale).sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1
                )
                Text(
                    text = Formats.temperature(day.forecast.lowC, model.settings.units.temperature, locale),
                    style = secondaryStyle(palette, (WeekLowSp * plan.textScale).sp),
                    maxLines = 1
                )
            }
        }
    }
}

/** The stale marker, where a form has a line for it (VISION §5.9). */
@Composable
private fun StaleLine(content: TodayUiState.Content, palette: WidgetPalette) {
    if (!content.isStale) return
    val context = LocalContext.current
    Text(
        text = staleText(context, content.lastSync, Instant.now()),
        style = TextStyle(color = palette.stale, fontSize = StaleSp.sp),
        maxLines = 1
    )
}

/**
 * The header's sentence: the next light moment with its time, or the day's headline
 * (the Now widget's own sentence, in its brief register). When the agenda is empty the
 * next-moment hero falls back to the headline rather than to a blank line.
 */
/**
 * Whether that sentence is the day's HEADLINE rather than the next light moment — which
 * for an orange or a red warning means the card is already saying it (Fase 11). It is
 * not simply the reader's choice: a card set to the next moment with no moment left
 * falls back to the headline, and a chip beside it would be the same thing twice.
 */
internal fun heroIsHeadline(arc: ArcSettings, series: ArcSeries): Boolean =
    arc.hero != ArcHero.NONE && !(arc.hero == ArcHero.NEXT_MOMENT && series.nextLight != null)

internal fun heroText(
    context: Context,
    model: WidgetModel,
    content: TodayUiState.Content,
    arc: ArcSettings,
    series: ArcSeries
): String {
    val next = series.nextLight
    return if (arc.hero == ArcHero.NEXT_MOMENT && next != null) {
        ArcText.heroSentence(context, next, model.zone)
    } else {
        sentence(context, content, model.settings.units)
    }
}

private fun temperature(content: TodayUiState.Content, model: WidgetModel): String =
    Formats.temperature(
        content.report.current.tempC, model.settings.units.temperature, Locale.getDefault()
    )

private fun strongStyle(palette: WidgetPalette, sizeSp: Float): TextStyle = TextStyle(
    color = palette.primary,
    fontSize = sizeSp.sp,
    fontWeight = FontWeight.Medium
)

/** Punctuation, not prose: the same dot the Sky widget joins its day marker with. */
private const val Separator = " · "

private val StripGlyph = 16.dp

/** The air before a row's verdict mark: the Sky widget's own. */
private val MarkGap = 6.dp
