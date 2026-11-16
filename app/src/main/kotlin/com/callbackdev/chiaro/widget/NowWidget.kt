package com.callbackdev.chiaro.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.callbackdev.chiaro.ui.format.Formats
import com.callbackdev.chiaro.ui.format.LocalClock
import com.callbackdev.chiaro.ui.icons.ChiaroIcons
import com.callbackdev.chiaro.domain.settings.UnitSettings
import com.callbackdev.chiaro.ui.today.HeadlineText
import com.callbackdev.chiaro.ui.today.TodayUiState
import com.callbackdev.chiaro.ui.today.WeatherText
import java.time.Instant
import java.util.Locale

/**
 * The Now widget (VISION §5.9): icon, temperature, place — the glance in the word's
 * old sense, drawn big enough to be one (device review, 3 set) — and, where the grant
 * has room and the reader has not turned it off, the day's sentence. Three forms, one
 * per kind of grant ([NowLayout]): the launcher's size picks the form, the widget's own
 * settings whether the sentence is there at all. The one-row forms are the text card's
 * with the glyph in front of the number since 15 nov 2026 ([NowHeroRow]); the second
 * arrangement they had, the glyph at the far edge, went the same day (committente:
 * «toglierei l'opzione per l'icona a destra»). Stale data states its age; no place says
 * so; nothing here is ever a guess.
 */
class NowWidget : GlanceAppWidget() {

    /** Exact sizing so [LocalSize] is the size the launcher really granted: the form,
     * the glyph and the sentence's line count are all read off it. */
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = runCatching {
            GlanceAppWidgetManager(context).getAppWidgetId(id)
        }.getOrDefault(0)
        // The revision is read BEFORE the load, so the model composed below is only
        // re-read when something really changed after it (see [WidgetRefresh]: Glance
        // does not run this function again while the session is alive, which is why
        // everything the widget draws is observed from inside the composition).
        val loadedAt = WidgetRefresh.revision.value
        val initial = WidgetData.load(context, appWidgetId)
        provideContent {
            val model = rememberWidgetModel(context, appWidgetId, initial, loadedAt)
            val schemes = rememberWidgetSchemes(
                context, model.settings.dynamicColor, model.settings.palette
            )
            NowWidgetContent(model, schemes, rememberSkyBitmap(model))
        }
    }
}

/**
 * The one-row card, and the hero row of the hours' card: **the «In words» card's one-row
 * forms with the glyph in front of the number** (committente, 15 nov 2026; the arithmetic
 * and its reasons are [nowRowIsWide]'s). On the leading side the place, under it the glyph
 * and the number on one line, under them the stale marker; on a [wide] row, against the
 * far edge, the sentence, the warning's chip and the day's high and low with their marks.
 *
 * It assumes the card's leading inset is the glyph's ([WidgetCardPaddingLeading]): every
 * line of words pays [NowGlyphEdgeGive] back, so the place stands where the text card's
 * does whether the glyph is drawn or not.
 *
 * [wide] and [warning] are the hours' card's to settle when it is the caller — its budget
 * decides them for the whole card, strip included — and the row's own otherwise.
 */
@Composable
internal fun NowHeroRow(
    content: TodayUiState.Content,
    model: WidgetModel,
    palette: WidgetPalette,
    size: DpSize,
    modifier: GlanceModifier = GlanceModifier.fillMaxSize(),
    wide: Boolean = nowLayout(size) == NowLayout.WIDE,
    warning: WarningSlot? = null
) {
    val context = LocalContext.current
    val scale = fontScale(context)
    val heroSp = nowRowHeroSp(size, scale, content.marked)
    val glyph = nowRowGlyphSize(size, scale, content.marked)
    // Three children, against Glance's ten.
    val leading: @Composable () -> Unit = {
        PlaceLine(
            name = content.city.name,
            fromGps = model.fromGps,
            palette = palette,
            size = TextFactSp.sp,
            modifier = GlanceModifier.padding(start = NowGlyphEdgeGive)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (glyph > 0.dp) {
                Box(modifier = GlanceModifier.padding(start = NowGlyphShift)) {
                    HeroIcon(content, model, palette, glyph)
                }
            }
            Temperature(
                content, model, palette, heroSp,
                modifier = GlanceModifier.padding(
                    start = if (glyph > 0.dp) IconTextGap else NowGlyphEdgeGive
                )
            )
        }
        if (content.marked) {
            Box(modifier = GlanceModifier.padding(start = NowGlyphEdgeGive)) {
                StaleLine(content, palette)
            }
        }
    }
    if (!wide) {
        Column(verticalAlignment = Alignment.Vertical.CenterVertically, modifier = modifier) {
            leading()
        }
        return
    }
    val sentenceOn = model.look.showSentence
    // Settled on the card the reader would have WITHOUT the warning, as the text card does:
    // the layout depends on the budget and the budget on the layout, and one goes first.
    val slot = warning ?: warningSlot(
        level = model.warning?.maxLevel,
        enabled = model.look.showWarning,
        headlineShown = sentenceOn,
        sentenceSlot = true,
        ownRow = nowRowHasWarningRow(size, scale, sentenceOn)
    )
    val day = content.week.firstOrNull()?.forecast?.takeIf { model.look.showDayRange }
    val plan = nowRowPlan(size, scale, sentenceOn, slot.drawn, range = day != null)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        // A width and not a weight: the text card's column ([textLeadingColumn]), measured
        // for the longest name, plus the 10 dp the glyph's inset lent it.
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = GlanceModifier.width(nowHeroLine(size, wide = true))
        ) { leading() }
        // Three children at most, against Glance's ten.
        Column(
            horizontalAlignment = Alignment.End,
            modifier = GlanceModifier.padding(start = SentenceGap).defaultWeight()
        ) {
            if (plan.sentenceLines > 0) {
                Text(
                    text = sentence(context, content, model.settings.units),
                    style = sentenceStyle(palette, TextAlign.End),
                    maxLines = plan.sentenceLines,
                    modifier = GlanceModifier.fillMaxWidth()
                )
            }
            model.warning?.maxLevel?.takeIf { plan.showWarning }?.let { level ->
                WarningChipRow(level, palette, if (plan.sentenceLines > 0) WarningChipGap else 0.dp)
            }
            day?.takeIf { plan.showRange }?.let {
                DayRange(it.highC, it.lowC, model.settings.units, palette, size = TextFactSp.sp, marks = true)
            }
        }
    }
}

/**
 * The two-row card: the glyph alone in the top trailing corner, the words stacked
 * against the bottom leading one — temperature, sentence, place, in the order the
 * reference stacks them and the order the app's own Today reads.
 *
 * A [Box] rather than a [Column], so the glyph is sized by [nowTallIconSize] and
 * anchored to the top while the words hang from the bottom: the two may share the
 * temperature's empty leading band (the budget counts on it), which a Column that
 * stacks boxes could never let them do. Horizontally they do not meet either — the
 * glyph is on the trailing side, the number on the leading one — so the only place
 * the two could touch is a band that is empty by construction. With the sentence
 * turned off the budget gives its two lines to the glyph.
 */
@Composable
private fun TallContent(
    content: TodayUiState.Content,
    model: WidgetModel,
    palette: WidgetPalette,
    size: DpSize,
    withSentence: Boolean,
    warning: WarningSlot = WarningSlot.NONE
) {
    val context = LocalContext.current
    val scale = fontScale(context)
    val temperatureSp = nowTallTemperatureSp(
        size, scale, content.marked, withSentence, warning.drawn
    )
    Box(
        contentAlignment = Alignment.TopEnd,
        modifier = GlanceModifier.fillMaxSize()
    ) {
        HeroIcon(
            content, model, palette,
            heroIconSize(
                nowTallIconRoom(
                    size, scale, content.marked, withSentence, warning.drawn, temperatureSp
                )
            )
        )
        // The words stop at the words' inset on the trailing side too: the card's
        // end padding is the glyph's 4, so the column pays the difference itself.
        Column(
            verticalAlignment = Alignment.Bottom,
            horizontalAlignment = Alignment.Start,
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(end = WidgetCardPadding - WidgetCardPaddingLeading)
        ) {
            Temperature(content, model, palette, temperatureSp)
            if (withSentence) {
                Text(
                    text = sentence(context, content, model.settings.units),
                    style = sentenceStyle(palette, TextAlign.Start),
                    maxLines = TallSentenceMaxLines,
                    modifier = GlanceModifier.fillMaxWidth()
                )
            }
            // Five children at most in this column, against Glance's ten.
            model.warning?.maxLevel?.takeIf { warning.drawn }?.let { level ->
                WarningChipRow(level, palette, if (withSentence) WarningChipGap else 0.dp)
            }
            PlaceLine(
                name = content.city.name,
                fromGps = model.fromGps,
                palette = palette,
                size = PlaceSp.sp
            )
            StaleLine(content, palette)
        }
    }
}

/** One hour of the narrow tall card's list: the hour, its drawing, its temperature. The
 * strip's three things on a line, in the strip's sizes, the temperature against the words'
 * edge like the hero's words. */
@Composable
private fun HourListRow(
    strip: com.callbackdev.chiaro.ui.today.StripHour,
    model: WidgetModel,
    palette: WidgetPalette
) {
    val context = LocalContext.current
    val locale = glanceLocale()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(HourListRowHeight)
            .padding(end = WidgetCardPadding - WidgetCardPaddingLeading)
    ) {
        Text(
            text = Formats.hourClockLabel(
                strip.hour.time, android.text.format.DateFormat.is24HourFormat(context), locale
            ),
            style = secondaryStyle(palette, StripHourSp.sp),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight()
        )
        Image(
            provider = ImageProvider(
                ChiaroIcons.conditionRes(
                    strip.hour.condition.wmoCode, strip.night, model.iconStyle, palette.darkGround
                )
            ),
            contentDescription = null,
            modifier = GlanceModifier.size(HourListIconSize)
        )
        Text(
            text = Formats.temperature(strip.hour.tempC, model.settings.units.temperature, locale),
            style = TextStyle(
                color = palette.primary,
                fontSize = StripTempSp.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.End
            ),
            maxLines = 1,
            modifier = GlanceModifier.width(HourListTempWidth)
        )
    }
}

@Composable
private fun HeroIcon(
    content: TodayUiState.Content,
    model: WidgetModel,
    palette: WidgetPalette,
    size: androidx.compose.ui.unit.Dp
) {
    val current = content.report.current
    Image(
        provider = ImageProvider(
            ChiaroIcons.conditionRes(
                current.condition.wmoCode, content.night,
                model.iconStyle, palette.darkGround
            )
        ),
        contentDescription = null, // the temperature and the sentence say it in words
        modifier = GlanceModifier.size(size)
    )
}

/**
 * The hero number. **Bold since 20 set 2026** (committente, with the two cards side by
 * side on the home screen): it was Medium, which is the weight every fact on these cards
 * is set in, and beside the text widget's Bold hero it read as one more fact rather than
 * as the thing the card is for. The rule is now the household's and not that one card's:
 * on the three cards where the temperature IS the hero it is Bold, and the weight sorts
 * it from the sentence and the place under it the way size and ink already did.
 *
 * One line, always — a temperature that wrapped would have stopped being a number.
 */
@Composable
private fun Temperature(
    content: TodayUiState.Content,
    model: WidgetModel,
    palette: WidgetPalette,
    sizeSp: Float = TemperatureSp,
    modifier: GlanceModifier = GlanceModifier
) {
    Text(
        text = Formats.temperature(
            content.report.current.tempC, model.settings.units.temperature, glanceLocale()
        ),
        style = TextStyle(
            color = palette.primary,
            fontSize = sizeSp.sp,
            fontWeight = FontWeight.Bold
        ),
        maxLines = 1,
        modifier = modifier
    )
}

/** The stale marker (VISION §5.9): with old data the widget says how old, under the
 * place, on the tall form; the one-row forms put it beside the number or at the far edge. */
@Composable
private fun StaleLine(content: TodayUiState.Content, palette: WidgetPalette) {
    if (!content.marked) return
    val context = LocalContext.current
    Text(
        text = markerText(context, content, Instant.now(LocalClock.current)),
        style = TextStyle(color = palette.stale, fontSize = StaleSp.sp),
        maxLines = 1
    )
}

/**
 * What the sentence slot says: the day's headline when there is one — the same
 * [HeadlineText] Today opens with, in its brief register, so the widget and the screen
 * never tell two stories about the same afternoon — and the sky's present state when
 * there is nothing to warn about. The fallback is not a filler: «Poco nuvoloso» is a
 * true thing about the sky right now, which is what this card is for, and a slot that
 * went blank on every quiet day would read as a card that failed to load. Shared with
 * the Today widget, which prints the same slot for the same reason.
 */
internal fun sentence(context: Context, content: TodayUiState.Content, units: UnitSettings): String =
    headline(context, content, units)
        ?: context.getString(WeatherText.condition(content.report.current.condition.wmoCode))

/** The day's headline in its brief register, null on a quiet day: [sentence] without its
 * fallback, for a card that already prints the sky's word elsewhere and would say it twice. */
internal fun headline(context: Context, content: TodayUiState.Content, units: UnitSettings): String? {
    val timeFmt = Formats.timeFormatter(
        android.text.format.DateFormat.is24HourFormat(context), Locale.getDefault()
    )
    return HeadlineText.of(context, content.headline, timeFmt, units, brief = true)
}

/** The sentence's dress on every form: 16 sp Medium in the strong ink — the second
 * thing read after the number, and the same sentence on both widgets that print it,
 * so it wears the same clothes there and here. */
internal fun sentenceStyle(palette: WidgetPalette, align: TextAlign): TextStyle = TextStyle(
    color = palette.primary,
    fontSize = SentenceSp.sp,
    fontWeight = FontWeight.Medium,
    textAlign = align
)

/** Read off a Context rather than a composition local for the reason [isNight] is:
 * there is no `LocalConfiguration` on the launcher's side of the fence. */
internal fun fontScale(context: Context): Float = context.resources.configuration.fontScale

/**
 * The whole card for [model] at the launcher's [LocalSize], split off the receiver's
 * `provideGlance` (23 set 2026) so the configuration screen's preview and the render tests
 * draw the SAME composition the launcher does rather than a lookalike.
 */
@Composable
internal fun NowWidgetContent(model: WidgetModel, schemes: WidgetSchemes, skyBitmap: Bitmap?) {
    val context = LocalContext.current
    val content = model.content
    val size = LocalSize.current
    // From three cells by two rows the card is the hours' card (13 ott 2026, when «Le
    // prossime ore» joined this one and left the picker): the same hero row, and under it
    // the next hours and, taller, the next days. The tall form this card drew there had a
    // glyph in one corner, three lines in the other and the middle empty.
    if (nowShowsHours(size)) {
        TodayWidgetContent(model, schemes, skyBitmap)
        return
    }
    val layout = if (content == null) null else nowLayout(size)
    // Each edge is inset for what sits against it — a glyph brings a margin of
    // its own, words bring none (the numbers are in [WidgetCardPaddingLeading]).
    // One-row forms (15 nov 2026): the text card's insets, the leading one lent to
    // the glyph — 4 dp, with every line of words paying the 10 back
    // ([NowGlyphEdgeGive]) — and nothing above and below: the budget still spends the
    // snug 6 at each edge, and the card lends them back as headroom so a font that
    // boxes taller than the estimate grows into air instead of losing its last line
    // ([textCardPaddingVertical]'s reason, and its arithmetic). The tall form: the
    // glyph sits in the top trailing corner, so those two edges take the glyph's
    // numbers and the other two the words'. An empty state has no glyph, only words,
    // and words get the words' inset on every side.
    val oneRow = layout == NowLayout.NARROW || layout == NowLayout.WIDE
    val paddingStart = if (oneRow) WidgetCardPaddingLeading else WidgetCardPadding
    val paddingEnd =
        if (layout == NowLayout.TALL) WidgetCardPaddingLeading else WidgetCardPaddingTrailing
    val paddingTop = when {
        oneRow -> 0.dp
        layout != null -> WidgetCardPaddingSnug
        else -> WidgetCardPadding
    }
    val paddingBottom = if (oneRow) 0.dp else WidgetCardPadding
    WidgetCard(
        model, schemes, skyBitmap,
        contentPaddingStart = paddingStart,
        contentPaddingEnd = paddingEnd,
        contentPaddingTop = paddingTop,
        contentPaddingBottom = paddingBottom
    ) { palette ->
        val sentenceOn = model.look.showSentence
        val level = model.warning?.maxLevel
        val stale = content?.marked == true
        val scale = fontScale(context)
        when {
            content == null && model.city == null -> NoPlaceContent(palette)
            content == null -> NoDataContent(palette)
            layout == NowLayout.TALL -> {
                // A narrow card taller than the reference two-by-two (widget review,
                // 13 ott 2026): the two-by-two's own hero at its own height, and under it
                // the next hours, one per line. It used to be that hero stretched, a glyph
                // in one corner and the words in the other with half the card empty.
                val rows = nowTallHourRows(size, scale, content.strip.size)
                val hero = if (rows > 0) DpSize(size.width, NowTallHeroCard) else size
                val heroContent: @Composable () -> Unit = {
                    TallContent(
                        content, model, palette, hero,
                        withSentence = sentenceOn,
                        // A tall card always HAS a sentence slot, so orange and red take
                        // it whenever the reader turned the sentence off; a line of its
                        // own is the budget's answer, not the form's.
                        warning = warningSlot(
                            level = level,
                            enabled = model.look.showWarning,
                            headlineShown = sentenceOn,
                            sentenceSlot = true,
                            ownRow = nowTallHasWarningRow(hero, scale, stale, sentenceOn)
                        )
                    )
                }
                if (rows == 0) {
                    heroContent()
                } else {
                    // Ten children at most: the hero, the gap and eight hours.
                    Column(modifier = GlanceModifier.fillMaxSize()) {
                        Box(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .height(NowTallHeroCard - WidgetCardPaddingSnug - WidgetCardPadding)
                        ) { heroContent() }
                        Spacer(modifier = GlanceModifier.height(HourListGap))
                        content.strip.take(rows).forEach { HourListRow(it, model, palette) }
                    }
                }
            }
            // The narrow form has no far edge, so it has nowhere to put a sentence or a
            // warning either: two or three cells are the place, the glyph and the number,
            // and that is the whole card. The row settles both for itself.
            else -> NowHeroRow(content, model, palette, size)
        }
    }
}
