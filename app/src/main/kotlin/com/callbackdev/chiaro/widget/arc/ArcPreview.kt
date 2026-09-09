package com.callbackdev.chiaro.widget.arc

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.sky.SkyVerdict
import com.callbackdev.chiaro.ui.format.Formats
import com.callbackdev.chiaro.ui.icons.ChiaroIcons
import com.callbackdev.chiaro.ui.theme.paletteFor
import com.callbackdev.chiaro.ui.today.TodayUiState
import com.callbackdev.chiaro.widget.StaleSp
import com.callbackdev.chiaro.widget.WidgetBackground
import com.callbackdev.chiaro.widget.WidgetModel
import com.callbackdev.chiaro.widget.WidgetPalette
import com.callbackdev.chiaro.widget.effectiveWidgetBackground
import com.callbackdev.chiaro.widget.fontScale
import com.callbackdev.chiaro.widget.isNight
import com.callbackdev.chiaro.widget.resolveWidgetPalette
import com.callbackdev.chiaro.widget.sentence
import com.callbackdev.chiaro.widget.skyGradientBitmap
import com.callbackdev.chiaro.widget.staleText
import com.callbackdev.chiaro.widget.verdictContainer
import com.callbackdev.chiaro.widget.verdictInk
import com.callbackdev.chiaro.widget.widgetCardFill
import com.callbackdev.chiaro.widget.widgetSchemes
import java.time.Instant
import java.util.Locale

/**
 * The grants the preview can show, at the reference device's measured sizes (a cell is
 * ~85 × 82 dp there; see `ArcLayoutTest` for where the numbers come from). Labels are
 * grid arithmetic, not prose, so they stay in the code.
 */
internal enum class ArcPreviewSize(val label: String, val size: DpSize) {
    ONE_BY_ONE("1×1", DpSize(85.dp, 82.dp)),
    TWO_BY_ONE("2×1", DpSize(159.dp, 82.dp)),
    FOUR_BY_ONE("4×1", DpSize(340.dp, 82.dp)),
    TWO_BY_TWO("2×2", DpSize(159.dp, 189.dp)),
    FOUR_BY_TWO("4×2", DpSize(340.dp, 189.dp)),
    FOUR_BY_THREE("4×3", DpSize(340.dp, 290.dp)),
    TWO_BY_FOUR("2×4", DpSize(159.dp, 390.dp)),
    FOUR_BY_FOUR("4×4", DpSize(340.dp, 390.dp))
}

/**
 * The card as the launcher would draw it at [size], on the configuration screen: the
 * same model, the same plan, the same painted arc, the same inks — laid out again in
 * Compose because a Glance composition cannot be shown inside an activity. Scaled down
 * to the width the screen has, never up.
 */
@Composable
internal fun ArcPreview(
    model: WidgetModel?,
    arc: ArcSettings?,
    size: DpSize,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val scale = minOf(1f, maxWidth / size.width)
        Box(
            modifier = Modifier.fillMaxWidth().height(size.height * scale),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .requiredSize(size.width, size.height)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
            ) {
                ArcPreviewCard(model, arc, size)
            }
        }
    }
}

@Composable
private fun ArcPreviewCard(model: WidgetModel?, arc: ArcSettings?, size: DpSize) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(24.dp)
    if (model == null || arc == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        return
    }
    val schemes = remember(model.settings.dynamicColor, model.settings.palette) {
        widgetSchemes(context, model.settings.dynamicColor, model.settings.palette)
    }
    val sky = model.content?.sky
    val skyBitmap = remember(sky, model.look, model.settings.palette) {
        sky?.takeIf { model.look.background == WidgetBackground.SKY }?.let {
            skyGradientBitmap(it, model.look.opacityPct, paletteFor(model.settings.palette).sky)
        }
    }
    val hasSky = skyBitmap != null
    val palette = remember(model.look, schemes, hasSky) {
        resolveWidgetPalette(context, model.look, schemes, hasSky)
    }
    val background = effectiveWidgetBackground(model.look, hasSky)
    Box(modifier = Modifier.fillMaxSize().clip(shape)) {
        if (skyBitmap != null && background == WidgetBackground.SKY) {
            Image(
                bitmap = skyBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        widgetCardFill(background, schemes, isNight(context), model.look.opacityPct / 100f)
                    )
            )
        }
        val content = model.content
        val city = model.city
        when {
            content == null && city == null ->
                PreviewMessage(stringResource(R.string.empty_no_place_title), palette, context)
            content == null || city == null ->
                PreviewMessage(stringResource(R.string.widget_no_data), palette, context)
            else -> {
                val series = remember(content, arc, model.moments) {
                    ArcSeries.build(content, model.moments, city.coordinates, Instant.now(), arc)
                }
                val plan = arcPlan(
                    size, fontScale(context), arc,
                    stale = content.isStale,
                    agendaAvailable = series.events.size,
                    weekAvailable = content.week.isNotEmpty()
                )
                val bitmap = remember(series, arc, plan, palette) {
                    paintArc(context, model, arc, series, plan, palette)
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = plan.paddingHorizontal, vertical = plan.paddingVertical)
                ) {
                    PreviewBody(
                        model, content, arc, series, plan,
                        PreviewInks(palette, context),
                        bitmap = bitmap
                    )
                }
            }
        }
    }
}

/** The card's inks as Compose colors, resolved once per palette. */
private class PreviewInks(val palette: WidgetPalette, context: Context) {
    val primary: Color = palette.primary.getColor(context)
    val secondary: Color = palette.secondary.getColor(context)
    val stale: Color = palette.stale.getColor(context)
}

@Composable
private fun PreviewMessage(text: String, palette: WidgetPalette, context: Context) {
    Box(modifier = Modifier.fillMaxSize().padding(14.dp), contentAlignment = Alignment.CenterStart) {
        Text(text = text, color = palette.secondary.getColor(context), fontSize = 12.sp)
    }
}

@Composable
private fun PreviewBody(
    model: WidgetModel,
    content: TodayUiState.Content,
    arc: ArcSettings,
    series: ArcSeries,
    plan: ArcPlan,
    inks: PreviewInks,
    bitmap: android.graphics.Bitmap
) {
    val context = LocalContext.current
    val scale = plan.textScale
    val next = series.next
    val temperature = Formats.temperature(
        content.report.current.tempC, model.settings.units.temperature, Locale.getDefault()
    )
    val graphic: @Composable () -> Unit = {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.width(plan.graphic.width).height(plan.graphic.height)
        )
    }
    when (plan.form) {
        ArcForm.DIAL -> Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            graphic()
            Spacer(modifier = Modifier.height(ArcGapTight))
            val figure = if (arc.dialFigure == ArcDialFigure.NEXT_TIME && next != null) {
                ArcText.clock(context, next.at, model.zone)
            } else {
                temperature
            }
            PText(figure, inks.primary, DialFigureSp * scale, medium = true)
            if (content.isStale) PText(staleText(context, content.lastSync, Instant.now()), inks.stale, StaleSp)
            if (plan.rows >= 2) {
                PlaceText(content.city.name, model.fromGps, inks, PlaceSp * scale)
                next?.let { PText(ArcText.heroShort(context, it, model.zone), inks.secondary, StripLineSp * scale) }
            }
        }
        ArcForm.STRIP -> if (plan.stacked) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    PText(temperature, inks.primary, StripStackedTempSp * scale, medium = true)
                    Spacer(modifier = Modifier.weight(1f))
                    when {
                        content.isStale ->
                            PText(staleText(context, content.lastSync, Instant.now()), inks.stale, StaleSp)
                        arc.hero != ArcHero.NONE && next != null -> {
                            Image(
                                painter = painterResource(
                                    ArcText.rowIconRes(
                                        next.item.kind, model.iconStyle,
                                        inks.palette.darkGround, model.settings.palette
                                    )
                                ),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            PText(ArcText.clock(context, next.at, model.zone), inks.secondary, PlaceSp * scale)
                        }
                        else -> PlaceText(content.city.name, model.fromGps, inks, PlaceSp * scale)
                    }
                }
                Spacer(modifier = Modifier.height(ArcGapTight))
                graphic()
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.width(StripTextColumn)) {
                    PText(temperature, inks.primary, StripTempSp * scale, medium = true)
                    when {
                        content.isStale ->
                            PText(staleText(context, content.lastSync, Instant.now()), inks.stale, StaleSp)
                        arc.hero == ArcHero.NEXT_MOMENT && next != null ->
                            PText(ArcText.heroShort(context, next, model.zone), inks.secondary, StripLineSp * scale)
                        arc.hero == ArcHero.HEADLINE ->
                            PText(sentence(context, content, model.settings.units), inks.secondary, StripLineSp * scale)
                        else -> PlaceText(content.city.name, model.fromGps, inks, StripLineSp * scale)
                    }
                }
                Spacer(modifier = Modifier.width(StripGraphicGap))
                graphic()
            }
        }
        ArcForm.CARD -> Column(modifier = Modifier.fillMaxSize()) {
            PText(temperature, inks.primary, CardTempSp * scale, medium = true)
            PlaceText(content.city.name, model.fromGps, inks, PlaceSp * scale)
            if (content.isStale) PText(staleText(context, content.lastSync, Instant.now()), inks.stale, StaleSp)
            if (plan.heroLines > 0) {
                PText(
                    heroText(context, model, content, arc, series),
                    inks.primary, CardHeroSp * scale, medium = true, maxLines = plan.heroLines
                )
            }
            Spacer(modifier = Modifier.height(ArcGap))
            graphic()
            PreviewAgenda(model, series, plan, inks)
        }
        ArcForm.PANEL, ArcForm.BOARD -> Column(modifier = Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                PText(temperature, inks.primary, PanelTempSp * scale, medium = true)
                Column(modifier = Modifier.padding(start = HeroGap).weight(1f)) {
                    if (plan.heroLines > 0) {
                        PText(
                            heroText(context, model, content, arc, series),
                            inks.primary, HeroSp * scale, medium = true, maxLines = plan.heroLines
                        )
                    }
                    val subSp = (if (plan.heroLines > 0) HeroSubSp else PlaceSp) * scale
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PlaceText(content.city.name, model.fromGps, inks, subSp)
                        if (plan.heroLines > 0 && arc.hero == ArcHero.NEXT_MOMENT && next != null) {
                            PText(" · " + ArcText.countdown(context, series.now, next.at), inks.secondary, subSp)
                        }
                        if (content.isStale) {
                            PText(" · " + staleText(context, content.lastSync, Instant.now()), inks.stale, subSp)
                        }
                    }
                }
                if (model.look.showDayRange && series.highC != null && series.lowC != null) {
                    Spacer(modifier = Modifier.width(HeroGap))
                    val units = model.settings.units.temperature
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PText(Formats.temperature(series.highC, units, Locale.getDefault()), inks.primary, 16f, medium = true)
                        PText(" / ", inks.secondary, 16f)
                        PText(Formats.temperature(series.lowC, units, Locale.getDefault()), inks.secondary, 16f)
                    }
                }
            }
            Spacer(modifier = Modifier.height(ArcGap))
            graphic()
            PreviewAgenda(model, series, plan, inks)
            Spacer(modifier = Modifier.weight(1f))
            if (plan.week) PreviewWeek(model, content, plan, inks)
        }
    }
}

@Composable
private fun PreviewAgenda(model: WidgetModel, series: ArcSeries, plan: ArcPlan, inks: PreviewInks) {
    if (plan.agendaRows <= 0) return
    val context = LocalContext.current
    val rowHeight = arcAgendaRowHeight(fontScale(context), plan.textScale)
    Spacer(modifier = Modifier.height(ArcGap))
    series.events.take(plan.agendaRows).forEachIndexed { index, event ->
        if (index > 0) Spacer(modifier = Modifier.height(ArcRowGap))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().height(rowHeight)
        ) {
            Image(
                painter = painterResource(
                    ArcText.rowIconRes(
                        event.item.kind, model.iconStyle, inks.palette.darkGround, model.settings.palette
                    )
                ),
                contentDescription = null,
                modifier = Modifier.size(AgendaGlyph)
            )
            val name = ArcText.rowLabel(context, event.item)
            Text(
                text = if (event.tomorrow) context.getString(R.string.arc_tomorrow_name, name) else name,
                color = inks.primary,
                fontSize = (AgendaSp * plan.textScale).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 8.dp).weight(1f)
            )
            PText(ArcText.clock(context, event.at, model.zone), inks.secondary, AgendaSp * plan.textScale)
            event.verdict?.let { verdict ->
                Spacer(modifier = Modifier.width(6.dp))
                PreviewVerdictMark(verdict, inks)
            }
        }
    }
}

@Composable
private fun PreviewWeek(
    model: WidgetModel,
    content: TodayUiState.Content,
    plan: ArcPlan,
    inks: PreviewInks
) {
    val context = LocalContext.current
    val locale = Locale.getDefault()
    val today = content.now.toLocalDate()
    Row(modifier = Modifier.fillMaxWidth()) {
        content.week.take(7).forEach { day ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                PText(ArcText.dayLabel(context, day.forecast.date, today), inks.secondary, WeekDaySp * plan.textScale)
                Spacer(modifier = Modifier.height(WeekInnerGap))
                Image(
                    painter = painterResource(
                        ChiaroIcons.conditionRes(
                            day.forecast.condition.wmoCode, false,
                            model.iconStyle, inks.palette.darkGround, model.settings.palette
                        )
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(WeekIcon)
                )
                Spacer(modifier = Modifier.height(WeekInnerGap))
                val units = model.settings.units.temperature
                PText(Formats.temperature(day.forecast.highC, units, locale), inks.primary, WeekHighSp * plan.textScale, medium = true)
                PText(Formats.temperature(day.forecast.lowC, units, locale), inks.secondary, WeekLowSp * plan.textScale)
            }
        }
    }
}

/** The Sky widget's mark, in Compose: the verdict's glyph in its own container. */
@Composable
private fun PreviewVerdictMark(verdict: SkyVerdict, inks: PreviewInks) {
    val context = LocalContext.current
    val night = isNight(context)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(22.dp)
            .background(verdictContainer(verdict.kind, night, inks.palette.dress).getColor(context), CircleShape)
    ) {
        Text(
            text = verdict.kind.glyph,
            color = verdictInk(verdict.kind, night, inks.palette.dress).getColor(context),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun PlaceText(name: String, fromGps: Boolean, inks: PreviewInks, sizeSp: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (fromGps) {
            Image(
                painter = painterResource(R.drawable.ic_place_pin),
                contentDescription = stringResource(R.string.places_gps_title),
                colorFilter = ColorFilter.tint(inks.secondary),
                modifier = Modifier.size(sizeSp.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        PText(name, inks.secondary, sizeSp)
    }
}

@Composable
private fun PText(
    text: String,
    color: Color,
    sizeSp: Float,
    medium: Boolean = false,
    maxLines: Int = 1
) {
    Text(
        text = text,
        color = color,
        fontSize = sizeSp.sp,
        fontWeight = if (medium) FontWeight.Medium else FontWeight.Normal,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}

