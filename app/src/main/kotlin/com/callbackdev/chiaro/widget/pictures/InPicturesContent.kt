package com.callbackdev.chiaro.widget.pictures

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider as FixedColorProvider
import com.callbackdev.chiaro.domain.model.HourlyForecast
import com.callbackdev.chiaro.domain.pictures.DialSpec
import com.callbackdev.chiaro.domain.pictures.PictureHour
import com.callbackdev.chiaro.domain.pictures.PictureWindow
import com.callbackdev.chiaro.domain.pictures.PlateMetrics
import com.callbackdev.chiaro.domain.pictures.StripMetrics
import com.callbackdev.chiaro.domain.pictures.WeekRange
import com.callbackdev.chiaro.domain.pictures.dialDrawing
import com.callbackdev.chiaro.domain.pictures.plateDrawing
import com.callbackdev.chiaro.domain.pictures.strideHours
import com.callbackdev.chiaro.domain.pictures.stripDrawing
import com.callbackdev.chiaro.domain.pictures.weekRangeBar
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.ui.format.LocalClock
import com.callbackdev.chiaro.ui.icons.ChiaroIcons
import com.callbackdev.chiaro.ui.theme.PicturesInks
import com.callbackdev.chiaro.ui.theme.paletteFor
import com.callbackdev.chiaro.ui.today.TodayUiState
import com.callbackdev.chiaro.widget.CompactNumberLine
import com.callbackdev.chiaro.widget.CompactNumberSp
import com.callbackdev.chiaro.widget.CompactPaddingSide
import com.callbackdev.chiaro.widget.CompactSentenceLine
import com.callbackdev.chiaro.widget.CompactWordsTop
import com.callbackdev.chiaro.widget.DayChanceColumn
import com.callbackdev.chiaro.widget.DayGap
import com.callbackdev.chiaro.widget.DayGlyph
import com.callbackdev.chiaro.widget.DayHighColumn
import com.callbackdev.chiaro.widget.DayLowColumn
import com.callbackdev.chiaro.widget.DayNameColumn
import com.callbackdev.chiaro.widget.DayPadding
import com.callbackdev.chiaro.widget.DialGap
import com.callbackdev.chiaro.widget.DialLongSentenceLine
import com.callbackdev.chiaro.widget.DialNumberNudge
import com.callbackdev.chiaro.widget.DialPadding
import com.callbackdev.chiaro.widget.DialPlaceGap
import com.callbackdev.chiaro.widget.DialPlaceLine
import com.callbackdev.chiaro.widget.DialPlaceSp
import com.callbackdev.chiaro.widget.DialRangeSp
import com.callbackdev.chiaro.widget.DialRowGap
import com.callbackdev.chiaro.widget.DialRowNumberLine
import com.callbackdev.chiaro.widget.DialRowNumberSp
import com.callbackdev.chiaro.widget.DialRowSentenceLine
import com.callbackdev.chiaro.widget.DialRowPaddingSide
import com.callbackdev.chiaro.widget.DialRowPaddingVertical
import com.callbackdev.chiaro.widget.DialRowSentenceSp
import com.callbackdev.chiaro.widget.DialRowWordsGap
import com.callbackdev.chiaro.widget.DialSentenceLine
import com.callbackdev.chiaro.widget.DialSentenceSp
import com.callbackdev.chiaro.widget.LaterPaddingSide
import com.callbackdev.chiaro.widget.LaterPaddingVertical
import com.callbackdev.chiaro.widget.LaterRowHeight
import com.callbackdev.chiaro.widget.NoDataContent
import com.callbackdev.chiaro.widget.NoPlaceContent
import com.callbackdev.chiaro.widget.PanelGlyph
import com.callbackdev.chiaro.widget.PanelNumberLine
import com.callbackdev.chiaro.widget.PanelNumberSp
import com.callbackdev.chiaro.widget.PanelPaddingSide
import com.callbackdev.chiaro.widget.PanelPaddingTop
import com.callbackdev.chiaro.widget.PanelPlaceLine
import com.callbackdev.chiaro.widget.PanelPlaceSp
import com.callbackdev.chiaro.widget.PanelSentenceLine
import com.callbackdev.chiaro.widget.PanelWordsTop
import com.callbackdev.chiaro.widget.PicturesContent
import com.callbackdev.chiaro.widget.PicturesExtra
import com.callbackdev.chiaro.widget.PicturesForm
import com.callbackdev.chiaro.widget.PicturesHeaderGutter
import com.callbackdev.chiaro.widget.PicturesHourColumnGap
import com.callbackdev.chiaro.widget.PicturesHourColumns
import com.callbackdev.chiaro.widget.PicturesHourGlyph
import com.callbackdev.chiaro.widget.PicturesPiece
import com.callbackdev.chiaro.widget.PicturesPlan
import com.callbackdev.chiaro.widget.PicturesSentenceSp
import com.callbackdev.chiaro.widget.PicturesStaleLine
import com.callbackdev.chiaro.widget.PicturesStaleSp
import com.callbackdev.chiaro.widget.PicturesWordsGap
import com.callbackdev.chiaro.widget.PlaceLine
import com.callbackdev.chiaro.widget.StripLineGap
import com.callbackdev.chiaro.widget.StripPaddingSide
import com.callbackdev.chiaro.widget.StripPlaceSp
import com.callbackdev.chiaro.widget.StripSentenceLine
import com.callbackdev.chiaro.widget.WarningChip
import com.callbackdev.chiaro.widget.WidgetCard
import com.callbackdev.chiaro.widget.WidgetModel
import com.callbackdev.chiaro.widget.WidgetPalette
import com.callbackdev.chiaro.widget.WidgetSchemes
import com.callbackdev.chiaro.widget.dayBarWidth
import com.callbackdev.chiaro.widget.fontScale
import com.callbackdev.chiaro.widget.picturesPlan
import java.time.Instant
import java.util.Locale

/**
 * «In immagini», the card (Fase 17.2): the plan of `InPicturesLayout` drawn in Glance. Split off
 * the receiver (17.3) like the family's other cards, so the configuration screen's preview and
 * the render tests draw the same composition the launcher does.
 *
 * **Every piece stands where the plan puts it.** The card is one `Box` — a `FrameLayout` on the
 * launcher's side — and each piece is a child of it moved to its rect by its padding, so the
 * canvas's absolute positions are the card's and no row or column re-flows them. A text is moved
 * by [textShift] besides, so its first baseline lands where the canvas's line box puts it; its own
 * box is the `TextView`'s, taller, and overlaps what the canvas's did not, which a `FrameLayout`
 * allows.
 */
@Composable
internal fun InPicturesContent(
    model: WidgetModel,
    schemes: WidgetSchemes,
    skyBitmap: Bitmap?,
    look: PicturesLook = PicturesLook.of(model.look)
) {
    val context = LocalContext.current
    val size = LocalSize.current
    val content = model.content
    val city = model.city
    if (content == null || city == null) {
        WidgetCard(model, schemes, skyBitmap) { palette ->
            if (city == null) NoPlaceContent(palette) else NoDataContent(palette)
        }
        return
    }
    val scale = fontScale(context)
    val density = context.resources.displayMetrics.density
    val now = Instant.now(LocalClock.current)
    val window = PictureWindow.of(look.span, now, model.zone)
    // «La giornata» draws from midnight, so it reads the day's own hours, the morning already
    // gone included (`WidgetModel.dayHours`); the next 24 hours read the report as Today does.
    val hours = remember(content, model.dayHours, look.span) {
        if (look.span == com.callbackdev.chiaro.domain.pictures.PictureSpan.DAY && model.dayHours.isNotEmpty()) {
            model.dayHours.map(PictureHour::of)
        } else {
            pictureHours(content)
        }
    }
    val texts = PicturesTexts.of(context, model, content, window, hours, look, now)
    val measure = DevicePicturesMeasure(context, texts, model.fromGps)
    val plan = picturesPlan(size, scale, measure, PicturesContent(glyph = look.glyph, days = look.days))
    val inks = picturesInks(context, model, schemes, hasSky = skyBitmap != null)
    val table = paletteFor(model.settings.palette).sky
    WidgetCard(model, schemes, skyBitmap, contentPadding = 0.dp) { palette ->
        val card = Card(context, model, content, texts, plan, inks, palette, scale, density, window, hours, look, table, now)
        Box(modifier = GlanceModifier.fillMaxSize()) {
            when (plan.form) {
                PicturesForm.Panel -> card.Panel()
                PicturesForm.Compact -> card.Compact()
                PicturesForm.Dial -> card.Dial()
                PicturesForm.DialRow -> card.DialRow()
                PicturesForm.Strip -> card.Strip()
            }
        }
    }
}

/** One card's pieces, with what every form shares: the texts, the inks, the scale. */
private class Card(
    val context: android.content.Context,
    val model: WidgetModel,
    val content: TodayUiState.Content,
    val texts: PicturesTexts,
    val plan: PicturesPlan,
    val inks: PicturesInks,
    val palette: WidgetPalette,
    val s: Float,
    val density: Float,
    val window: PictureWindow,
    val hours: List<PictureHour>,
    val look: PicturesLook,
    val table: com.callbackdev.chiaro.ui.theme.SkyPalette,
    val now: Instant
) {
    val unit = model.settings.units.temperature

    // ------------------------------------------------------------------ the 4×2 family

    @Composable
    fun Panel() {
        val numberLeft = PanelPaddingSide + if (plan.glyph != null) PanelGlyph + PicturesHeaderGutter else 0.dp
        plan.glyph?.let { Glyph(it) }
        Number(numberLeft, PanelPaddingTop, PanelNumberSp, PanelNumberLine)
        val numberWidth = DevicePicturesMeasure(context, texts, model.fromGps).width(PicturesPiece.Number, plan.numberSp)
        val left = numberLeft + numberWidth + PicturesHeaderGutter
        val plate = checkNotNull(plan.plate)
        Words(
            left = left,
            top = PanelPaddingTop + PanelWordsTop,
            width = plate.right - left,
            sentenceSp = PicturesSentenceSp,
            sentenceLine = PanelSentenceLine,
            placeSp = PanelPlaceSp,
            placeLine = PanelPlaceLine,
            align = TextAlign.Start
        )
        Plate(plate, tickEvery = 3)
        when (plan.extra) {
            PicturesExtra.Hours -> Hours(plate.left, plan.extraTop, plate.right - plate.left)
            PicturesExtra.Days -> Days(plate.left, plan.extraTop, plate.right - plate.left)
            else -> Unit
        }
    }

    @Composable
    fun Compact() {
        Number(CompactPaddingSide, PanelPaddingTop, CompactNumberSp, CompactNumberLine)
        val numberWidth = DevicePicturesMeasure(context, texts, model.fromGps).width(PicturesPiece.Number, plan.numberSp)
        val left = CompactPaddingSide + numberWidth + PicturesHeaderGutter
        val plate = checkNotNull(plan.plate)
        Words(
            left = left,
            top = PanelPaddingTop + CompactWordsTop,
            width = plate.right - left,
            sentenceSp = PicturesSentenceSp,
            sentenceLine = CompactSentenceLine,
            placeSp = PicturesStaleSp,
            placeLine = PicturesStaleLine,
            align = TextAlign.Start
        )
        Plate(plate, tickEvery = 6)
        if (plan.extra == PicturesExtra.Hours) Hours(plate.left, plan.extraTop, plate.right - plate.left)
    }

    // ------------------------------------------------------------------ the dial family

    @Composable
    fun Dial() {
        val dial = checkNotNull(plan.dial)
        val side = dial.right - dial.left
        DialPicture(dial, DialSpec.LARGE.at(side.value))
        // The number and the range, centred inside the ring as one block (the canvas's flex
        // column), the number 6 dp right of centre for its degree sign.
        At(dial.left, dial.top) {
            Box(modifier = GlanceModifier.size(side, side), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = texts[PicturesPiece.Number],
                        style = TextStyle(color = palette.primary, fontSize = plan.numberSp.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        modifier = GlanceModifier.padding(start = DialNumberNudge)
                    )
                    if (plan.dialRange) {
                        Text(
                            text = texts.range,
                            style = TextStyle(color = palette.secondary, fontSize = DialRangeSp.sp),
                            maxLines = 1
                        )
                    }
                }
            }
        }
        val long = plan.sentence == PicturesPiece.Sentence
        Words(
            left = DialPadding,
            top = dial.bottom + DialGap,
            width = width(),
            sentenceSp = if (long) PicturesSentenceSp else DialSentenceSp,
            sentenceLine = if (long) DialLongSentenceLine else DialSentenceLine,
            placeSp = DialPlaceSp,
            placeLine = DialPlaceLine,
            align = TextAlign.Center,
            placeGap = DialPlaceGap
        )
        plan.later?.let { Later(it) }
    }

    @Composable
    fun DialRow() {
        val size = LocalSize.current
        val dial = plan.dial
        if (dial != null) DialPicture(dial, DialSpec.SMALL)
        val words = size.width - DialRowPaddingSide * 2 - if (dial != null) DialSpec.SMALL.size.dp + DialRowGap else 0.dp
        // The words' block centred on the card (the canvas's `align-items: center`), each line
        // where the canvas's box puts it: a column of `TextView`s would stack their own boxes,
        // taller than the canvas's, and push the last line off a one-row card.
        val numberLine = (DialRowNumberLine / DialRowNumberSp * plan.numberSp * s).dp
        val sentenceLine = (DialRowSentenceLine * s).dp
        val slotLine = when (plan.placeLine) {
            PicturesPiece.Warning -> com.callbackdev.chiaro.widget.warningChipHeight(s)
            null -> 0.dp
            else -> (PicturesStaleLine * plan.placeLines * s).dp
        }
        val block = numberLine +
            (if (plan.sentence != null) DialRowWordsGap + sentenceLine * plan.sentenceLines else 0.dp) +
            (if (plan.placeLine != null) DialRowWordsGap + slotLine else 0.dp)
        var top: Dp = (size.height - block) / 2
        At(DialRowPaddingSide, top + textShift(context, plan.numberSp, numberLine, bold = true)) {
            Text(
                text = texts[PicturesPiece.Number],
                style = TextStyle(color = palette.primary, fontSize = plan.numberSp.sp, fontWeight = FontWeight.Bold),
                maxLines = 1
            )
        }
        top += numberLine
        if (plan.sentence != null) {
            top += DialRowWordsGap
            At(DialRowPaddingSide, top + textShift(context, DialRowSentenceSp, sentenceLine)) {
                Text(
                    text = texts[plan.sentence],
                    style = TextStyle(color = palette.primary, fontSize = DialRowSentenceSp.sp, fontWeight = FontWeight.Medium),
                    maxLines = plan.sentenceLines,
                    modifier = GlanceModifier.width(words)
                )
            }
            top += sentenceLine * plan.sentenceLines
        }
        if (plan.placeLine != null) {
            top += DialRowWordsGap
            val shift = if (plan.placeLine == PicturesPiece.Warning) 0.dp else textShift(context, PicturesStaleSp, (PicturesStaleLine * s).dp)
            At(DialRowPaddingSide, top + shift) {
                Column(modifier = GlanceModifier.width(words)) { Slot(TextAlign.Start, 0.dp) }
            }
        }
    }

    // ------------------------------------------------------------------ the 4×1

    @Composable
    fun Strip() {
        val strip = checkNotNull(plan.strip)
        Box(
            modifier = GlanceModifier.fillMaxSize().padding(start = StripPaddingSide),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = texts[PicturesPiece.Number],
                style = TextStyle(color = palette.primary, fontSize = plan.numberSp.sp, fontWeight = FontWeight.Bold),
                maxLines = 1
            )
        }
        val line = if (plan.sentence != null || plan.placeLine != null) {
            maxOf(
                if (plan.sentence != null) (StripSentenceLine * s).dp else 0.dp,
                if (plan.placeLine != null) (if (plan.placeLine == PicturesPiece.Place) 16f * s else PicturesStaleLine * s).dp else 0.dp
            )
        } else {
            0.dp
        }
        if (line > 0.dp) {
            val top = strip.top - StripLineGap - line
            At(strip.left, top + textShift(context, PicturesSentenceSp * 1f, line)) {
                Row(
                    modifier = GlanceModifier.width(strip.right - strip.left),
                    verticalAlignment = Alignment.Bottom
                ) {
                    if (plan.sentence != null) {
                        Text(
                            text = texts[plan.sentence],
                            style = TextStyle(color = palette.primary, fontSize = PicturesSentenceSp.sp, fontWeight = FontWeight.Medium),
                            maxLines = 1,
                            modifier = GlanceModifier.defaultWeight()
                        )
                    }
                    when (plan.placeLine) {
                        PicturesPiece.Place -> PlaceLine(texts[PicturesPiece.Place], model.fromGps, palette, StripPlaceSp.sp)
                        PicturesPiece.Stale -> Text(texts[PicturesPiece.Stale], style = TextStyle(color = palette.stale, fontSize = PicturesStaleSp.sp), maxLines = 1)
                        PicturesPiece.Warning -> model.warning?.let { WarningChip(it.maxLevel, palette) }
                        else -> Unit
                    }
                }
            }
        }
        val drawing = stripDrawing(
            PictureWindow.next(now, model.zone, StripMetrics.HOURS), hours, (strip.right - strip.left).value
        )
        val picture = PicturesPainter.strip(
            drawing,
            windowRibbon(drawing.window, content.week, table, model.zone),
            inks,
            drawing.ticks.map { tickLabel(context, it.at, model.zone, it.isNow) },
            density, s, rain = look.rain
        )
        val o = PicturesPainter.StripOverhang.dp
        At(strip.left - o, strip.top - o) {
            Image(
                provider = ImageProvider(picture),
                contentDescription = context.getString(
                    R.string.pictures_desc_strip,
                    pictureDescription(context, drawing.window, hours, unit, texts[PicturesPiece.Number], look.rain)
                ),
                modifier = GlanceModifier.size(drawing.width.dp + o * 2, (picture.height / density).dp)
            )
        }
    }

    // ------------------------------------------------------------------ shared pieces

    @Composable
    fun Glyph(rect: DpRect) {
        At(rect.left, rect.top) {
            Image(
                provider = ImageProvider(
                    ChiaroIcons.conditionRes(content.report.current.condition.wmoCode, content.night, model.iconStyle, palette.darkGround)
                ),
                contentDescription = null, // the number and the sentence say it in words
                modifier = GlanceModifier.size(rect.right - rect.left, rect.bottom - rect.top)
            )
        }
    }

    /** The number, its first baseline where the canvas's [line]-tall box puts it. */
    @Composable
    fun Number(left: Dp, top: Dp, canvasSp: Float, canvasLine: Float) {
        val line = (canvasLine / canvasSp * plan.numberSp * s).dp
        At(left, top + textShift(context, plan.numberSp, line, bold = true)) {
            Text(
                text = texts[PicturesPiece.Number],
                style = TextStyle(color = palette.primary, fontSize = plan.numberSp.sp, fontWeight = FontWeight.Bold),
                maxLines = 1
            )
        }
    }

    /** The sentence and the place's slot under it, a column of [width] at ([left], [top]). */
    @Composable
    fun Words(
        left: Dp,
        top: Dp,
        width: Dp,
        sentenceSp: Float,
        sentenceLine: Float,
        placeSp: Float,
        placeLine: Float,
        align: TextAlign,
        placeGap: Dp = PicturesWordsGap
    ) {
        if (plan.sentence == null && plan.placeLine == null) return
        val firstSp = if (plan.sentence != null) sentenceSp else placeSp
        val firstLine = ((if (plan.sentence != null) sentenceLine else placeLine) * s).dp
        At(left, top + textShift(context, firstSp, firstLine)) {
            Column(
                modifier = GlanceModifier.width(width),
                horizontalAlignment = if (align == TextAlign.Center) Alignment.CenterHorizontally else Alignment.Start
            ) {
                if (plan.sentence != null) {
                    Text(
                        text = texts[plan.sentence],
                        style = TextStyle(
                            color = palette.primary, fontSize = sentenceSp.sp, fontWeight = FontWeight.Medium, textAlign = align
                        ),
                        maxLines = plan.sentenceLines,
                        modifier = GlanceModifier.width(width)
                    )
                }
                val gap = if (plan.sentence != null) placeGap else 0.dp
                when (plan.placeLine) {
                    PicturesPiece.Place, PicturesPiece.PlaceAndRange -> Row(
                        modifier = GlanceModifier.padding(top = gap),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlaceLine(texts[PicturesPiece.Place], model.fromGps, palette, placeSp.sp)
                        if (plan.placeLine == PicturesPiece.PlaceAndRange && texts.range.isNotEmpty()) {
                            Text(
                                text = context.getString(R.string.pictures_place_and_range, "", texts.range),
                                style = TextStyle(color = palette.secondary, fontSize = placeSp.sp),
                                maxLines = 1
                            )
                        }
                    }
                    else -> Slot(align, gap)
                }
            }
        }
    }

    /** The slot's fixed pieces: the stale marker, or the warning's chip. */
    @Composable
    fun Slot(align: TextAlign, gap: Dp) {
        when (plan.placeLine) {
            PicturesPiece.Stale -> Text(
                text = texts[PicturesPiece.Stale],
                style = TextStyle(color = palette.stale, fontSize = PicturesStaleSp.sp, textAlign = align),
                maxLines = plan.placeLines,
                modifier = GlanceModifier.padding(top = gap)
            )
            PicturesPiece.Warning -> model.warning?.let { warning ->
                Box(modifier = GlanceModifier.padding(top = gap)) { WarningChip(warning.maxLevel, palette) }
            }
            else -> Unit
        }
    }

    @Composable
    fun Plate(rect: DpRect, tickEvery: Int) {
        val w = rect.right - rect.left
        val drawing = plateDrawing(window, hours, w.value, tickEvery)
        val picture = PicturesPainter.plate(
            drawing,
            windowRibbon(window, content.week, table, model.zone),
            inks,
            PicturesPainter.PlateText(
                high = drawing.high?.let { degrees(it.temperatureC, unit) },
                low = drawing.low?.let { degrees(it.temperatureC, unit) },
                ticks = drawing.ticks.map { tickLabel(context, it.at, model.zone, it.isNow) }
            ),
            density, s,
            PicturesPainter.PlateShow(rain = look.rain, ribbon = look.ribbon)
        )
        val o = PlateMetrics.OVERHANG.dp
        At(rect.left - o, rect.top - o) {
            Image(
                provider = ImageProvider(picture),
                contentDescription = pictureDescription(context, window, hours, unit, texts[PicturesPiece.Number], look.rain),
                modifier = GlanceModifier.size(w + o * 2, PlateMetrics.HEIGHT.dp + o * 2)
            )
        }
    }

    @Composable
    fun DialPicture(rect: DpRect, spec: DialSpec) {
        val drawing = dialDrawing(spec, now, model.zone, hours)
        val picture = PicturesPainter.dial(drawing, dayRing(content.week.firstOrNull(), table), inks, density, rain = look.rain)
        val o = PicturesPainter.DialOverhang.dp
        val next = PictureWindow.next(now, model.zone)
        At(rect.left - o, rect.top - o) {
            Image(
                provider = ImageProvider(picture),
                contentDescription = context.getString(
                    R.string.pictures_desc_dial,
                    pictureDescription(context, next, hours, unit, texts[PicturesPiece.Number], look.rain)
                ),
                modifier = GlanceModifier.size(spec.size.dp + o * 2, spec.size.dp + o * 2)
            )
        }
    }

    /** «Più tardi» on the 2×3: three hours under the dial, on a plate of their own. */
    @Composable
    fun Later(rect: DpRect) {
        val w = rect.right - rect.left
        val h = rect.bottom - rect.top
        val rows = strideHours(window, hours, plan.laterRows)
        // The hour's column: the canvas's 20 dp, or the widest label where the reader's clock or
        // scale needs more («5 PM»), so no hour is cut.
        val time = rows.maxOfOrNull {
            com.callbackdev.chiaro.widget.measureWidgetText(context, tickLabel(context, it.at, model.zone, false), 13f, medium = false)
        }?.let { maxOf(it, LaterTimeColumn) } ?: LaterTimeColumn
        At(rect.left, rect.top) {
            Box(modifier = GlanceModifier.size(w, h)) {
                Image(
                    provider = ImageProvider(PicturesPainter.panel(w.value, h.value, inks, density)),
                    contentDescription = null,
                    modifier = GlanceModifier.size(w, h)
                )
                Column(
                    modifier = GlanceModifier.fillMaxSize().padding(
                        horizontal = LaterPaddingSide, vertical = LaterPaddingVertical
                    )
                ) {
                    rows.forEach { hour -> LaterRow(hour, time) }
                }
            }
        }
    }

    @Composable
    fun LaterRow(hour: PictureHour, time: Dp) {
        val forecast = hourly(hour)
        Row(modifier = GlanceModifier.fillMaxWidth().height(LaterRowHeight), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = tickLabel(context, hour.at, model.zone, isNow = false),
                style = TextStyle(color = palette.secondary, fontSize = 13.sp),
                maxLines = 1,
                modifier = GlanceModifier.width(time)
            )
            forecast?.let { HourIcon(it, LaterGlyph, GlanceModifier.padding(horizontal = LaterGap)) }
            Text(
                text = hour.temperatureC?.let { degrees(it, unit) }.orEmpty(),
                style = TextStyle(color = palette.primary, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight()
            )
            chanceLabel(hour.chancePct)?.let { Chance(it) }
        }
    }

    /** The 4×3's six hours, three apart, in a grid of six columns. */
    @Composable
    fun Hours(left: Dp, top: Dp, width: Dp) {
        val cells = strideHours(window, hours, PicturesHourColumns)
        val column = (width - PicturesHourColumnGap * (PicturesHourColumns - 1)) / PicturesHourColumns
        At(left, top) {
            Row(modifier = GlanceModifier.width(width)) {
                cells.forEachIndexed { i, hour ->
                    Column(
                        modifier = GlanceModifier.width(column).padding(start = if (i > 0) PicturesHourColumnGap else 0.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = tickLabel(context, hour.at, model.zone, isNow = false),
                            style = TextStyle(color = palette.secondary, fontSize = 13.sp, textAlign = TextAlign.Center),
                            maxLines = 1
                        )
                        hourly(hour)?.let { HourIcon(it, PicturesHourGlyph, GlanceModifier.padding(vertical = 3.dp)) }
                        Text(
                            text = hour.temperatureC?.let { degrees(it, unit) }.orEmpty(),
                            style = TextStyle(color = palette.primary, fontSize = 13.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center),
                            maxLines = 1
                        )
                        Chance(chanceLabel(hour.chancePct) ?: " ")
                    }
                }
            }
        }
    }

    /** The 4×4's days: today first, a hairline over every row but the first. */
    @Composable
    fun Days(left: Dp, top: Dp, width: Dp) {
        val days = content.week.take(plan.dayRows)
        val grow = s.coerceAtLeast(1f)
        val bar = dayBarWidth(width + PanelPaddingSide * 2, s)
        val colors = palette.colors
        val track = inks.ink.copy(alpha = WeekRange.TRACK_ALPHA)
        val locale = com.callbackdev.chiaro.widget.glanceLocale()
        At(left + DayPadding, top) {
            Column(modifier = GlanceModifier.width(width - DayPadding * 2)) {
                days.forEachIndexed { i, day ->
                    val f = day.forecast
                    Column(modifier = GlanceModifier.fillMaxWidth()) {
                        if (i > 0) {
                            Box(
                                modifier = GlanceModifier.fillMaxWidth().height(1.dp)
                                    .background(FixedColorProvider(inks.ink.copy(alpha = WeekRange.HAIRLINE_ALPHA)))
                            ) {}
                        }
                        Row(
                            modifier = GlanceModifier.fillMaxWidth().height(WeekRange.ROW_HEIGHT.dp - if (i > 0) 1.dp else 0.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (i == 0) context.getString(R.string.week_today) else com.callbackdev.chiaro.ui.format.Formats.dayLabel(f.date, locale),
                                style = TextStyle(color = palette.primary, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                                maxLines = 1,
                                modifier = GlanceModifier.width(DayNameColumn * grow + DayGap)
                            )
                            Image(
                                provider = ImageProvider(ChiaroIcons.conditionRes(f.condition.wmoCode, false, model.iconStyle, palette.darkGround)),
                                contentDescription = null,
                                modifier = GlanceModifier.size(DayGlyph)
                            )
                            Box(modifier = GlanceModifier.width(DayGap + DayChanceColumn * grow).padding(start = DayGap)) {
                                chanceLabel(f.precipPct)?.let { Chance(it) }
                            }
                            Text(
                                text = degrees(f.lowC, unit),
                                style = TextStyle(color = palette.secondary, fontSize = 13.sp, textAlign = TextAlign.End),
                                maxLines = 1,
                                modifier = GlanceModifier.width(DayLowColumn * grow + DayGap).padding(end = DayGap)
                            )
                            weekRangeBar(f.lowC, f.highC)?.let { range ->
                                Image(
                                    provider = ImageProvider(
                                        PicturesPainter.rangeBar(
                                            range, bar.value, colors.temperatureAt(f.lowC), colors.temperatureAt(f.highC), track, density
                                        )
                                    ),
                                    contentDescription = null, // the two figures beside it say it
                                    modifier = GlanceModifier.size(bar, WeekRange.BAR_HEIGHT.dp)
                                )
                            }
                            Text(
                                text = degrees(f.highC, unit),
                                style = TextStyle(color = palette.primary, fontSize = 13.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.End),
                                maxLines = 1,
                                modifier = GlanceModifier.width(DayHighColumn * grow + DayGap).padding(start = DayGap)
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun Chance(text: String) {
        Text(
            text = text,
            style = TextStyle(color = FixedColorProvider(inks.chance), fontSize = 11.sp, fontWeight = FontWeight.Medium),
            maxLines = 1
        )
    }

    /** The hour's glyph at [size], with [air] around it on a wrapper: a sized Glance view with
     * padding draws smaller, it does not sit further in (`WidgetGlyphBoxTest`). */
    @Composable
    fun HourIcon(hour: HourlyForecast, size: Dp, air: GlanceModifier = GlanceModifier) {
        val night = content.strip.firstOrNull { it.hour.at == hour.at }?.night ?: false
        Box(modifier = air) {
            Image(
                provider = ImageProvider(ChiaroIcons.conditionRes(hour.condition.wmoCode, night, model.iconStyle, palette.darkGround)),
                contentDescription = null,
                modifier = GlanceModifier.size(size)
            )
        }
    }

    fun hourly(hour: PictureHour): HourlyForecast? = content.report.hourly.firstOrNull { it.at == hour.at }

    /** The card's inner width on the dial's forms. */
    @Composable
    fun width(): Dp = LocalSize.current.width - DialPadding * 2
}

/** A piece moved to ([left], [top]) of the card. */
@Composable
private fun At(left: Dp, top: Dp, content: @Composable () -> Unit) {
    Box(modifier = GlanceModifier.padding(start = left.coerceAtLeast(0.dp), top = top.coerceAtLeast(0.dp))) {
        content()
    }
}

private fun Dp.coerceAtLeast(min: Dp): Dp = if (this < min) min else this

/** «Più tardi»'s row: the hour in 20 dp, the glyph 22 with 6 dp either side, the temperature,
 * the chance (the canvas's 2×3). */
private val LaterTimeColumn = 20.dp
private val LaterGlyph = 22.dp
private val LaterGap = 6.dp
