package com.callbackdev.chiaro.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
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
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.domain.warnings.WarningLevel
import com.callbackdev.chiaro.ui.format.Formats
import com.callbackdev.chiaro.ui.format.LocalClock
import com.callbackdev.chiaro.ui.icons.ChiaroIcons
import com.callbackdev.chiaro.ui.today.StripHour
import com.callbackdev.chiaro.ui.today.TodayUiState
import com.callbackdev.chiaro.ui.today.WeatherText
import com.callbackdev.chiaro.ui.warnings.WarningText
import java.time.Instant

/**
 * The text widget (committente, 19 set 2026): the same facts the Now widget carries, with
 * no picture drawn on it — no weather glyph, no chip, no illustration. A card of words and
 * figures, and its whole hierarchy built out of type.
 *
 * It is not the Now widget with the icon deleted. A card that loses its drawing loses the
 * thing that made it readable across a room, so this one rebuilds that at the only place
 * left: the temperature is the drawing now, sized to the grant the way the other four size
 * their glyph, in Bold, over a place set two ranks below it. [TextWidgetLayout] carries the
 * four ranks, the four forms and every number's reason. (The Bold was this card's alone
 * until 20 set 2026, when the committente asked for it on the Now and Today cards too: it
 * is the household's weight for a hero number now, and this card's rank 1 is still two
 * ranks clear of everything beside it on size and ink.)
 *
 * **Two marks are drawn, and both were asked for** (committente, 19 set 2026, on the
 * device). The position pin comes back in front of a place the phone is standing in, where
 * the first pass spelled it out in words and the words ate the place name; and the day's
 * high and low get the up and down marks. Neither contradicts the card's premise: a mark at
 * the size of the line it belongs to, tinted with that line's ink, is punctuation.
 *
 * **The weather glyph is the one picture, and it is the reader's to ask for**
 * ([WidgetLook.showIcon], off by default, committente 20 set 2026). It is drawn ONLY into
 * space the card already leaves empty — [TextWidgetLayout]'s four slot functions return a
 * size or nothing, and none of them is an input to a plan — so turning it on costs no line
 * of sentence, no warning, no range and no dp of number. Where the card has no such space,
 * it is simply not drawn, which is why three cells on one row and two cells on two stay
 * text-only whatever the switch says.
 *
 * What the words still have to say that a drawing said before:
 *
 * - **the official warning** was a chip, which carries its own measured ground precisely
 *   because a widget's ground is a scrimmed sky or a wallpaper and a bare coloured word on
 *   one of those was found unreadable (committente, 4 set, on the Sky card's verdicts).
 *   This card cannot give a chip a ground without drawing one, so it gives up the colour
 *   instead and prints the level as a word in the card's own ink — which DESIGN §2.3 makes
 *   the primary carrier anyway: a level is a word before it is a colour, and here it is
 *   only the word.
 *
 * Everything else is the household's: the model is [WidgetData]'s, the sentence is the same
 * [sentence] the Now and Today cards print, the warning's slot is [warningSlot]'s one
 * table, and the card never invents — stale data states its age, a place with no report
 * says so, and a section with no room is not drawn.
 */
class TextWidget : GlanceAppWidget() {

    /** Exact sizing so [LocalSize] is the size the launcher really granted: the form, the
     * hero's size and every line count are read off it. */
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = runCatching {
            GlanceAppWidgetManager(context).getAppWidgetId(id)
        }.getOrDefault(0)
        // The revision is read BEFORE the load, so the model composed below is only
        // re-read when something really changed after it ([WidgetRefresh]).
        val loadedAt = WidgetRefresh.revision.value
        val initial = WidgetData.load(context, appWidgetId)
        provideContent {
            val model = rememberWidgetModel(context, appWidgetId, initial, loadedAt)
            val schemes = rememberWidgetSchemes(
                context, model.settings.dynamicColor, model.settings.palette
            )
            TextWidgetContent(model, schemes, rememberSkyBitmap(model))
        }
    }
}

/**
 * The whole card for [model] at the launcher's [LocalSize], split off [TextWidget] so the
 * configuration screen's preview and the render tests draw the SAME composition the
 * launcher does rather than a lookalike.
 */
@Composable
internal fun TextWidgetContent(model: WidgetModel, schemes: WidgetSchemes, skyBitmap: Bitmap?) {
    val content = model.content
    val size = LocalSize.current
    val form = if (content == null) null else textForm(size)
    // Every edge of this card carries words, so every edge takes the words' inset
    // ([WidgetCardPaddingLeading] exists for a glyph's own margin, and there is no
    // glyph here). The exception is the top and bottom of a one-row card, where
    // the budget spends the household's snug 6 — that is what makes the hero worth
    // calling one: at 14 the reference 85 dp row would print a 27 sp number, under
    // this card's own floor — and the CARD lends it straight back as headroom, so a
    // column that measures a few dp taller than the estimate grows into its own air
    // instead of cutting its last line. Nothing moves either way: a one-row card
    // centres its columns, and a centred block ignores a symmetric inset. See
    // [textCardPaddingVertical] for the line that was being cut and the arithmetic.
    // Geometry checked against the 24 dp corner: at the budgeted height the block
    // is still centred where it was, so the first cap of the place's line sits at
    // (14, ~8), 17 dp from the corner's centre and well inside the radius. Only a
    // column really overrunning its budget comes nearer, and at the full 12 dp of
    // headroom the cap lands at (14, ~2) — a fraction of a dp outside the curve,
    // against a line that was being cut in half before.
    val vertical = textCardPaddingVertical(form)
    // A glyph edge takes 4 dp and a words edge 14 ([TextIconEdgeGive]). On every
    // form but ROW — where the glyph is interior, inside the name's column — the
    // drawing is what meets the trailing edge, so the card gives it the glyph's
    // inset and each text that reached that edge pays the 10 dp back. Nothing the
    // reader can read moves by a dp either way, which is why the inset and the
    // give-back are two readings of ONE condition ([textCardPaddingEnd]) rather
    // than two conditions free to disagree — see there for the day they did.
    val give = textEdgeGive(form, model.look.showIcon)
    WidgetCard(
        model, schemes, skyBitmap,
        contentPaddingEnd = textCardPaddingEnd(form, model.look.showIcon),
        contentPaddingTop = vertical,
        contentPaddingBottom = vertical
    ) { palette ->
        when {
            content == null && model.city == null -> NoPlaceContent(palette)
            content == null -> NoDataContent(palette)
            form == TextForm.PANEL ->
                PanelContent(content, model, palette, size, give)
            form == TextForm.STACK ->
                StackContent(content, model, palette, size, give)
            form == TextForm.ROW -> RowContent(content, model, palette, size)
            else -> LineContent(content, model, palette, size, give)
        }
    }
}

/**
 * The narrow one-row card: the place, the number, and the age of the data when it is old.
 * No sentence, because there is no column to put one in — three cells is where this card
 * gains one, and two is where it does without, exactly as the Now widget does without at
 * three. The block is centred on the row: with no drawing beside it there is nothing for
 * it to align to but the card.
 */
@Composable
private fun LineContent(
    content: TodayUiState.Content,
    model: WidgetModel,
    palette: WidgetPalette,
    size: DpSize,
    edgeGive: Dp
) {
    val context = LocalContext.current
    val icon = if (model.look.showIcon) textRowIconSize(size, fontScale(context)) else 0.dp
    // A Box and not a Row: in a Row the glyph would take its width off the column, and the
    // place name — the one line this card fought to print whole — would start truncating
    // again. Overlaid, the words keep every dp they had and the glyph sits in the corner
    // the number leaves empty, under the place line and beside the figure.
    Box(contentAlignment = Alignment.BottomEnd, modifier = GlanceModifier.fillMaxSize()) {
        Column(
            verticalAlignment = Alignment.Vertical.CenterVertically,
            // The words keep the words' inset while the glyph meets the card at the
            // glyph's: the card's end padding is the drawing's here, so the column that
            // holds every line pays the difference back in one place.
            modifier = GlanceModifier.fillMaxSize().padding(end = edgeGive)
        ) {
            PlaceLineText(content, model, palette)
            HeroTemperature(
                content, model, palette,
                textRowHeroSp(size, fontScale(context), content.isStale)
            )
            StaleLineText(content, palette)
        }
        // The glyph is the one thing on this form anchored to the CARD rather than to the
        // block of words, so it is also the one thing the lent-back inset would have moved
        // ([textCardPaddingVertical]): it keeps the snug 6 dp of its own, outside the size
        // modifier, because padding inside one would be drawn out of the glyph instead of
        // under it.
        if (icon > 0.dp) {
            Box(modifier = GlanceModifier.padding(bottom = WidgetCardPaddingSnug)) {
                ConditionGlyph(content, model, palette, icon)
            }
        }
    }
}

/**
 * The default 4×1 card, and everything from three cells up that is one row high: the place
 * over the number on the leading side, and against the far edge the day's sentence with
 * the warning's word and the day's range under it.
 *
 * The leading column is a fixed width ([textLeadingColumn]) rather than half the row: the
 * two columns are not doing the same work, and the one holding prose is the one that needs
 * the measure. The stale marker sits on the leading side under the number, where it costs
 * the hero its own line and not the sentence one of hers.
 */
@Composable
private fun RowContent(
    content: TodayUiState.Content,
    model: WidgetModel,
    palette: WidgetPalette,
    size: DpSize
) {
    val context = LocalContext.current
    val scale = fontScale(context)
    val sentenceOn = model.look.showSentence
    // The warning is settled on the card the reader would have WITHOUT it — the layout
    // depends on the budget and the budget on the layout, and the Today widget unties the
    // same knot the same way. `headlineShown` is the switch rather than the resolved line
    // count for the same reason it is on that card: this form always gives the sentence
    // its first line above ~34 dp of row, which is under the provider's own floor.
    val warning = warningSlot(
        level = model.warning?.maxLevel,
        enabled = model.look.showWarning,
        headlineShown = sentenceOn,
        sentenceSlot = true,
        ownRow = textRowHasFactLine(size, scale, sentenceOn)
    )
    val plan = textRowPlan(
        size, scale,
        sentence = sentenceOn,
        warning = warning.drawn,
        range = model.look.showDayRange && content.week.firstOrNull() != null
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = GlanceModifier.fillMaxSize()
    ) {
        // Overlaid on the name's column for the reason the narrow form gives: a glyph that
        // took its width out of the column would truncate the place name. Here it sits
        // against the column's trailing edge, under the place line and beside the number,
        // which is space the widest temperature this card can print still leaves empty.
        Box(
            contentAlignment = Alignment.BottomEnd,
            modifier = GlanceModifier.width(textLeadingColumn(size))
        ) {
            // **The width and the alignment are both written out, and the width is the
            // fix** (committente, 21 set 2026, on the device: «sembra che testo e
            // temperatura siano allineati a destra e non a sinistra»). Glance has no
            // per-child `align`, so a Box's `contentAlignment` lands on EVERY child of
            // it — and this column had no width of its own, so `BottomEnd` laid the
            // words out at their own width against the column's trailing edge. The card
            // opened with an empty leading inset and «Manchester» floating in the middle
            // of it; only a name long enough to fill the column — the «Cavenago di
            // Brianza» the column was measured for — hid it, which is why it read as a
            // card that was right on one place and wrong on another.
            //
            // Filling the width puts the words back on the card's own edge and leaves
            // `BottomEnd` to the one child it was written for. That is also what keeps
            // the two apart: [textRowIconSize] sizes the glyph out of what the widest
            // number this column can print leaves, and that arithmetic starts from a
            // number at the LEADING edge — against the trailing one a short name put the
            // drawing straight over the figure.
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = GlanceModifier.fillMaxWidth()
            ) {
                PlaceLineText(content, model, palette)
                HeroTemperature(
                    content, model, palette, textRowHeroSp(size, scale, content.isStale)
                )
                StaleLineText(content, palette)
            }
            ConditionGlyph(
                content, model, palette,
                if (model.look.showIcon) textRowIconSize(size, scale) else 0.dp
            )
        }
        Column(
            horizontalAlignment = Alignment.End,
            modifier = GlanceModifier.padding(start = SentenceGap).defaultWeight()
        ) {
            if (plan.sentenceLines > 0) {
                Text(
                    text = sentence(context, content, model.settings.units),
                    style = textSentenceStyle(palette, TextAlign.End),
                    maxLines = plan.sentenceLines,
                    modifier = GlanceModifier.fillMaxWidth()
                )
            }
            model.warning?.maxLevel?.takeIf { plan.showWarning }?.let { level ->
                WarningWord(level, palette, TextAlign.End)
            }
            content.week.firstOrNull()?.forecast?.takeIf { plan.showRange }?.let { day ->
                DayRange(
                    day.highC, day.lowC, model.settings.units, palette,
                    size = TextFactSp.sp, marks = true
                )
            }
        }
    }
}

/**
 * Two rows and up, under four cells wide: one column against the leading edge, with the
 * place pinned to the TOP of the card as its eyebrow and everything else pinned to the
 * bottom (committente, 19 set 2026: «allinea in basso testo e, se impostata, temperatura
 * minima e massima»). The air lands between the two, which is what turns a short column
 * into a composition instead of a list that ran out.
 *
 * Six children at most against Glance's ten, and no spacers: the air is a weighted
 * [Spacer] between the eyebrow and the block, one child, for the reason Fase 11 records —
 * a container that goes over the limit drops the overflow without a word.
 */
@Composable
private fun StackContent(
    content: TodayUiState.Content,
    model: WidgetModel,
    palette: WidgetPalette,
    size: DpSize,
    edgeGive: Dp
) {
    val context = LocalContext.current
    val scale = fontScale(context)
    val sentenceOn = model.look.showSentence
    val today = content.week.firstOrNull()?.forecast
    // Settled without itself, as on the one-row card. A stack is at least 150 dp tall by
    // construction, which always pays for the sentence's two lines, so the switch is the
    // honest answer to `headlineShown` here too.
    val warning = warningSlot(
        level = model.warning?.maxLevel,
        enabled = model.look.showWarning,
        headlineShown = sentenceOn,
        sentenceSlot = true,
        ownRow = textStackHasFactLine(size, scale, content.isStale, sentenceOn)
    )
    val later = laterHours(content.strip, content.now)
    val plan = textStackPlan(
        size, scale,
        stale = content.isStale,
        sentence = sentenceOn,
        warning = warning.drawn,
        range = model.look.showDayRange && today != null,
        later = model.look.showLater && later.size >= TextLaterMinRows
    )
    val icon = if (model.look.showIcon) {
        textStackIconSize(size, scale, plan.heroSp)
    } else {
        0.dp
    }
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = GlanceModifier.fillMaxSize()
    ) {
        // The give is paid line by line here and not on the whole column, because the
        // glyph is INSIDE it, on the number's row: a column that paid it once would have
        // moved the drawing off the edge it was measured against. The place is the one
        // line besides the sentence long enough to reach that edge — a name at the
        // glyph's 4 dp would ellipsise 10 dp further out than the prose under it, into
        // the corner's own radius.
        PlaceLineText(content, model, palette, GlanceModifier.padding(end = edgeGive))
        Spacer(modifier = GlanceModifier.defaultWeight())
        // The glyph rides the number's own line and is never taller than it, so the row it
        // shares is exactly the line the budget already paid for.
        Row(verticalAlignment = Alignment.Bottom, modifier = GlanceModifier.fillMaxWidth()) {
            HeroTemperature(content, model, palette, plan.heroSp)
            if (icon > 0.dp) {
                Spacer(modifier = GlanceModifier.defaultWeight())
                ConditionGlyph(content, model, palette, icon)
            }
        }
        if (plan.sentenceLines > 0) {
            Text(
                text = sentence(context, content, model.settings.units),
                style = textSentenceStyle(palette, TextAlign.Start),
                maxLines = plan.sentenceLines,
                // The 10 dp the card's edge gave the glyph, given back: the sentence wraps
                // against exactly the width it wrapped against before. It is [edgeGive]
                // and not `icon > 0` — the card hands its edge over on the switch alone,
                // and on a two-cell stack there is no room for a glyph, so that test paid
                // back nothing on precisely the cards that had nothing drawn on them.
                modifier = GlanceModifier
                    .padding(end = edgeGive)
                    .fillMaxWidth()
            )
        }
        model.warning?.maxLevel?.takeIf { plan.showWarning }?.let { level ->
            WarningWord(level, palette, TextAlign.Start)
        }
        today?.takeIf { plan.showRange }?.let { day ->
            DayRange(
                day.highC, day.lowC, model.settings.units, palette,
                size = TextFactSp.sp, marks = true
            )
        }
        LaterTable(
            later.take(plan.laterRows), model, palette, scale,
            width = size.width - WidgetCardPadding * 2, edgeGive = edgeGive
        )
        StaleLineText(content, palette)
    }
}

/**
 * Two rows and up, four cells or wider (committente, 19 set 2026, second device pass): the
 * place as a full-width eyebrow at the top, and along the bottom the number on the leading
 * side with the sentence and the day's range right-aligned beside it.
 *
 * It is the one-row card's composition given a second dimension, and the reason to have it
 * is the number: standing BESIDE the words rather than under them, it can be as tall as
 * the whole block beside it ([TextHeroPanelMax], 64 sp against the row form's 44) without
 * becoming an ornament. The eyebrow moving to the full width is the other half of the
 * trade — «Cavenago di Brianza» has nothing to compete with up there.
 *
 * The two columns are bottom-aligned, not centred, and then lifted onto one baseline
 * ([textPanelBaselineLift]): the digits of the number and of the range stand on one line,
 * which is the whole point of «allineate a destra in basso».
 */
@Composable
private fun PanelContent(
    content: TodayUiState.Content,
    model: WidgetModel,
    palette: WidgetPalette,
    size: DpSize,
    edgeGive: Dp
) {
    val context = LocalContext.current
    val scale = fontScale(context)
    val sentenceOn = model.look.showSentence
    val today = content.week.firstOrNull()?.forecast
    val warning = warningSlot(
        level = model.warning?.maxLevel,
        enabled = model.look.showWarning,
        headlineShown = sentenceOn,
        sentenceSlot = true,
        ownRow = textPanelHasFactLine(size, scale, sentenceOn)
    )
    val later = laterHours(content.strip, content.now)
    val sentenceText = sentence(context, content, model.settings.units)
    val plan = textPanelPlan(
        size, scale,
        stale = content.isStale,
        sentence = sentenceOn,
        warning = warning.drawn,
        range = model.look.showDayRange && today != null,
        later = model.look.showLater && later.size >= TextLaterMinRows,
        sentenceFitsOneLine = measureWidgetText(context, sentenceText, TextSentenceSp, medium = true) +
            RowFitSlack <= textPanelSentenceColumn(size),
        descentEm = widgetTextDescentEm()
    )
    val icon = if (model.look.showIcon) textPanelIconSize(size, scale, plan) else 0.dp
    Column(modifier = GlanceModifier.fillMaxSize()) {
        // The eyebrow runs the full width, so it is one of the lines that reaches the
        // trailing edge and it pays the glyph's 10 dp back like the prose below it —
        // and here the edge it would otherwise meet is the top corner's.
        PlaceLineText(content, model, palette, GlanceModifier.padding(end = edgeGive))
        Spacer(modifier = GlanceModifier.defaultWeight())
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = GlanceModifier.fillMaxWidth()
        ) {
            Column(modifier = GlanceModifier.width(TextPanelLeading)) {
                HeroTemperature(content, model, palette, plan.heroSp)
                StaleLineText(content, palette)
            }
            // The glyph goes INSIDE the trailing column, over the words, and not beside
            // this row as its own line: a line of its own would be height the card has to
            // find, and the whole promise is that it finds none. Here the column grows
            // upward into the band the weighted spacer was holding, the row's bottom
            // alignment keeps the number exactly where it was, and
            // [textPanelIconSize] is precisely the height that band had.
            Column(
                horizontalAlignment = Alignment.End,
                modifier = GlanceModifier.padding(start = SentenceGap).defaultWeight()
            ) {
                ConditionGlyph(content, model, palette, icon)
                Column(
                    horizontalAlignment = Alignment.End,
                    // The 10 dp the card's edge gave the glyph, given back to the words:
                    // the column they wrap against is the column it always was. Off
                    // [edgeGive] for the reason the stack gives — the reference panel at
                    // a 1.3 font scale asks for a glyph and has no band to draw one in.
                    // [TextPanelPlan.baselineLift] under the words puts their last line on
                    // the number's baseline, not on the bottom of its line box.
                    modifier = GlanceModifier.padding(end = edgeGive, bottom = plan.baselineLift)
                ) {
                    if (plan.sentenceLines > 0) {
                        Text(
                            text = sentenceText,
                            style = textSentenceStyle(palette, TextAlign.End),
                            maxLines = plan.sentenceLines,
                            modifier = GlanceModifier.fillMaxWidth()
                        )
                    }
                    model.warning?.maxLevel?.takeIf { plan.showWarning }?.let { level ->
                        WarningWord(level, palette, TextAlign.End)
                    }
                    today?.takeIf { plan.showRange }?.let { day ->
                        DayRange(
                            day.highC, day.lowC, model.settings.units, palette,
                            size = TextFactSp.sp, marks = true
                        )
                    }
                }
            }
        }
        // Under the whole block and across the full width: the table is the card's
        // last paragraph, not a third column, and at the width of the card its words
        // («Pioggia leggera», «Poco nuvoloso») print whole.
        LaterTable(
            later.take(plan.laterRows), model, palette, scale,
            width = size.width - WidgetCardPadding * 2, edgeGive = edgeGive
        )
    }
}

/**
 * «Più tardi»: the next hours as lines of type, three hours apart ([laterHours]) — the time
 * in the quiet ink, the temperature at rank 3 in the strong one, the chance of rain in the
 * rain ramp's own ink when it is [TextLaterRainFloorPct] or more, and the sky in a word.
 *
 * It is the Today card's strip said in words, which is the whole of this card's premise:
 * the same hours, the same numbers, and a word where that card draws a glyph. Columns are
 * fixed widths so the figures line up down the table the way a timetable's do; the rain
 * column exists only when some row has a figure for it, and the word only where the card
 * leaves it [TextLaterWordMin] — a two-cell card prints «Poc…» otherwise, and a clipped
 * word is worse than a table of figures. Every row is one Glance `Row` of at most four
 * children, inside a column of at most four rows.
 */
@Composable
private fun LaterTable(
    rows: List<StripHour>,
    model: WidgetModel,
    palette: WidgetPalette,
    scale: Float,
    width: Dp,
    edgeGive: Dp
) {
    if (rows.size < TextLaterMinRows) return
    val context = LocalContext.current
    val locale = glanceLocale()
    val is24h = android.text.format.DateFormat.is24HourFormat(context)
    val clock = Formats.timeFormatter(is24h, locale)
    val columns = laterColumns(
        width - edgeGive, scale, is24h,
        rain = rows.any { (it.hour.precipChancePct ?: 0) >= TextLaterRainFloorPct }
    )
    Column(
        modifier = GlanceModifier.fillMaxWidth().padding(top = TextLaterGap, end = edgeGive)
    ) {
        rows.forEach { row ->
            val hour = row.hour
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = GlanceModifier.fillMaxWidth()
            ) {
                Text(
                    text = hour.time.format(clock),
                    style = secondaryStyle(palette, TextLaterSp.sp),
                    maxLines = 1,
                    modifier = GlanceModifier.width(columns.time)
                )
                Text(
                    text = Formats.temperature(
                        hour.tempC, model.settings.units.temperature, locale
                    ),
                    style = TextStyle(
                        color = palette.primary,
                        fontSize = TextFactSp.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    modifier = GlanceModifier.width(columns.temperature)
                )
                if (columns.rain > 0.dp) {
                    // An empty cell, not a «0%», where this hour's chance is under the
                    // floor or was never forecast (§1.1): the column stays one column.
                    val pct = hour.precipChancePct?.takeIf { it >= TextLaterRainFloorPct }
                    Text(
                        text = pct?.let { Formats.percent(it, locale) } ?: "",
                        style = TextStyle(
                            color = rainInk(pct ?: 0, palette),
                            fontSize = TextLaterSp.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        modifier = GlanceModifier.width(columns.rain)
                    )
                }
                if (columns.word) {
                    Text(
                        text = context.getString(WeatherText.condition(hour.condition.wmoCode)),
                        style = secondaryStyle(palette, TextLaterSp.sp),
                        maxLines = 1,
                        modifier = GlanceModifier.defaultWeight()
                    )
                }
            }
        }
    }
}

/**
 * The one picture on the card, and only where the card had room for it already
 * ([TextWidgetLayout]'s slot functions, which answer 0 dp when it does not). It is the
 * household's own drawing — the same family, the same day/night and ground resolution the
 * other four cards use through [WidgetModel.iconStyle] — so five widgets on one home screen
 * cannot draw two different skies.
 *
 * It says nothing to a screen reader while the sentence is drawn, because the sentence says
 * it in words and saying it twice is worse than not saying it. With the sentence turned off
 * the glyph is the only thing on the card that names the sky, so it takes the condition's
 * own word: a drawing with no name is the one thing this card must not become.
 */
@Composable
private fun ConditionGlyph(
    content: TodayUiState.Content,
    model: WidgetModel,
    palette: WidgetPalette,
    size: Dp
) {
    if (size <= 0.dp) return
    val context = LocalContext.current
    val current = content.report.current
    Image(
        provider = ImageProvider(
            ChiaroIcons.conditionRes(
                current.condition.wmoCode, content.night,
                model.iconStyle, palette.darkGround
            )
        ),
        contentDescription = if (model.look.showSentence) {
            null
        } else {
            context.getString(WeatherText.condition(current.condition.wmoCode))
        },
        modifier = GlanceModifier.size(size)
    )
}

/**
 * Rank 1. Bold: weight is one of the three things a rank is made of here, and the top rank
 * is the one that can afford all three. Since 20 set 2026 the Now and Today cards set their
 * hero the same way, which costs this card nothing — there the number is 34 sp against a
 * 16 sp fact, here it is 26 to 64 against the same 16, and the rank is the distance.
 * One line, always — a temperature that wrapped would have stopped being a number.
 */
@Composable
private fun HeroTemperature(
    content: TodayUiState.Content,
    model: WidgetModel,
    palette: WidgetPalette,
    sizeSp: Float
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
        maxLines = 1
    )
}

/** Rank 2's dress: 17 sp Medium in the strong ink. Written here rather than reusing
 * [sentenceStyle] because that one is the household's 16, and this card's ranks are its
 * own — see [TextSentenceSp] for the point it is worth. */
private fun textSentenceStyle(palette: WidgetPalette, align: TextAlign): TextStyle = TextStyle(
    color = palette.primary,
    fontSize = TextSentenceSp.sp,
    fontWeight = FontWeight.Medium,
    textAlign = align
)

/**
 * Rank 3, the place, with the position pin in front of it when the place is the phone's own
 * — the household's own [PlaceLine], at this card's fact size.
 *
 * It is the eyebrow of the card rather than a line under the number: with nothing drawn,
 * the reader needs to know what the big figure is ABOUT before reading it, and a name set
 * two ranks down cannot compete with it for the eye.
 *
 * **The pin, from 19 set 2026** (committente, on the device). The first pass said «la mia
 * posizione» in words, on the argument that a card with nothing drawn should say what the
 * other cards mark — and on the device the words were the whole line: «Ornago · la mia
 * posizi…», a place name truncated by its own footnote. The mark is 16 dp of ink that says
 * the same thing and leaves the name whole, and it is the same drawing the app screen and
 * the other four cards put there, so the five cards cannot disagree about what "my
 * position" looks like.
 */
@Composable
private fun PlaceLineText(
    content: TodayUiState.Content,
    model: WidgetModel,
    palette: WidgetPalette,
    /** What this line owes the card's trailing edge, where the glyph took it
     * ([textEdgeGive]). Nothing on the forms whose edge is still the words'. */
    modifier: GlanceModifier = GlanceModifier
) {
    PlaceLine(
        name = content.city.name,
        fromGps = model.fromGps,
        palette = palette,
        size = TextFactSp.sp,
        modifier = modifier
    )
}

/**
 * Rank 3, the official warning: the level as a phrase, in the card's own strong ink and
 * Medium weight. No chip and no level colour — see this file's header for why a card with
 * nothing drawn on it cannot carry either honestly. Which days and which hazard are in the
 * app, one tap away, exactly as they are from every other card's chip.
 */
@Composable
private fun WarningWord(level: WarningLevel, palette: WidgetPalette, align: TextAlign) {
    val context = LocalContext.current
    Text(
        text = context.getString(WarningText.phraseRes(level)),
        style = TextStyle(
            color = palette.primary,
            fontSize = TextFactSp.sp,
            fontWeight = FontWeight.Medium,
            textAlign = align
        ),
        maxLines = 1
    )
}

/** Rank 4: with old data the card says how old, on every form (VISION §5.9). Never
 * optional, and never one of the things a budget may drop. */
@Composable
private fun StaleLineText(content: TodayUiState.Content, palette: WidgetPalette) {
    if (!content.isStale) return
    val context = LocalContext.current
    Text(
        text = staleText(context, content.lastSync, Instant.now(LocalClock.current)),
        style = TextStyle(color = palette.stale, fontSize = TextStaleSp.sp),
        maxLines = 1
    )
}
