package com.callbackdev.chiaro.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.callbackdev.chiaro.domain.warnings.WarningLevel
import com.callbackdev.chiaro.ui.format.Formats
import com.callbackdev.chiaro.ui.today.TodayUiState
import com.callbackdev.chiaro.ui.warnings.WarningText
import java.time.Instant
import java.util.Locale

/**
 * The text widget (committente, 19 set 2026): the same facts the Now widget carries, with
 * nothing drawn — no weather glyph, no position pin, no chip, no mark. A card of words and
 * figures, and its whole hierarchy built out of type.
 *
 * It is not the Now widget with the icon deleted. A card that loses its drawing loses the
 * thing that made it readable across a room, so this one rebuilds that at the only place
 * left: the temperature is the drawing now, sized to the grant the way the other four size
 * their glyph, in Bold where the household writes Medium, over a place set two ranks below
 * it. [TextWidgetLayout] carries the four ranks, the three forms and every number's reason.
 *
 * **Two marks are drawn, and both were asked for** (committente, 19 set 2026, on the
 * device). The position pin comes back in front of a place the phone is standing in, where
 * the first pass spelled it out in words and the words ate the place name; and the day's
 * high and low get the up and down marks. Neither contradicts the card's premise: a mark at
 * the size of the line it belongs to, tinted with that line's ink, is punctuation, and this
 * card still has no picture on it — no weather glyph, no chip, no illustration.
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
            val skyBitmap = rememberSkyBitmap(model)
            val content = model.content
            val size = LocalSize.current
            val form = if (content == null) null else textForm(size)
            // Every edge of this card carries words, so every edge takes the words' inset
            // ([WidgetCardPaddingLeading] exists for a glyph's own margin, and there is no
            // glyph here). The exception is the top and bottom of a one-row card, where
            // the household's snug 6 is what makes the hero worth calling one: at 14 the
            // reference 85 dp row would print a 27 sp number, under this card's own floor.
            // Geometry checked against the 24 dp corner: the first cap of a 14 sp line at
            // (14, 6) sits 20.6 dp from the corner's centre, inside the radius.
            val vertical = if (form == TextForm.ROW || form == TextForm.LINE) {
                WidgetCardPaddingSnug
            } else {
                WidgetCardPadding
            }
            WidgetCard(
                model, schemes, skyBitmap,
                contentPaddingTop = vertical,
                contentPaddingBottom = vertical
            ) { palette ->
                when {
                    content == null && model.city == null -> NoPlaceContent(palette)
                    content == null -> NoDataContent(palette)
                    form == TextForm.STACK -> StackContent(content, model, palette, size)
                    form == TextForm.ROW -> RowContent(content, model, palette, size)
                    else -> LineContent(content, model, palette, size)
                }
            }
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
    size: DpSize
) {
    val context = LocalContext.current
    Column(
        verticalAlignment = Alignment.Vertical.CenterVertically,
        modifier = GlanceModifier.fillMaxSize()
    ) {
        PlaceLineText(content, model, palette)
        HeroTemperature(
            content, model, palette,
            textRowHeroSp(size, fontScale(context), content.isStale)
        )
        StaleLineText(content, palette)
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
        Column(modifier = GlanceModifier.width(textLeadingColumn(size))) {
            PlaceLineText(content, model, palette)
            HeroTemperature(
                content, model, palette, textRowHeroSp(size, scale, content.isStale)
            )
            StaleLineText(content, palette)
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
 * Two rows and up: one column against the leading edge, centred on the height it does not
 * fill. Place, number, sentence, the facts, the next hours as figures, and the age of the
 * data at the foot — the order a page is read in, each rank one step quieter than the one
 * above it.
 *
 * Seven children at most against Glance's ten, and the gaps are padding rather than
 * spacers for the same reason (Fase 11: a container that goes over the limit drops the
 * overflow without a word).
 */
@Composable
private fun StackContent(
    content: TodayUiState.Content,
    model: WidgetModel,
    palette: WidgetPalette,
    size: DpSize
) {
    val context = LocalContext.current
    val locale = Locale.getDefault()
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
    val hours = content.strip.take(textHourCells(size.width))
    val plan = textStackPlan(
        size, scale,
        stale = content.isStale,
        sentence = sentenceOn,
        warning = warning.drawn,
        range = model.look.showDayRange && today != null,
        hours = hours.isNotEmpty()
    )
    Column(
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Start,
        modifier = GlanceModifier.fillMaxSize()
    ) {
        PlaceLineText(content, model, palette)
        HeroTemperature(content, model, palette, plan.heroSp)
        if (plan.sentenceLines > 0) {
            Text(
                text = sentence(context, content, model.settings.units),
                style = textSentenceStyle(palette, TextAlign.Start),
                maxLines = plan.sentenceLines,
                modifier = GlanceModifier.fillMaxWidth()
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
        if (plan.showHours) {
            val is24h = android.text.format.DateFormat.is24HourFormat(context)
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(top = TextHoursGap)
            ) {
                hours.forEach { strip ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = GlanceModifier
                            .padding(horizontal = TextHourCellGap / 2)
                            .defaultWeight()
                    ) {
                        Text(
                            text = Formats.hourLabel(strip.hour.time, is24h, locale),
                            style = secondaryStyle(palette, TextStaleSp.sp),
                            maxLines = 1
                        )
                        Text(
                            text = Formats.temperature(
                                strip.hour.tempC, model.settings.units.temperature, locale
                            ),
                            style = TextStyle(
                                color = palette.primary,
                                fontSize = TextHourTempSp.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1,
                            modifier = GlanceModifier.padding(top = TextHoursInnerGap)
                        )
                    }
                }
            }
        }
        StaleLineText(content, palette)
    }
}

/**
 * Rank 1. Bold, where every other card writes its hero Medium: weight is one of the three
 * things a rank is made of here, and the top rank is the one that can afford all three.
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
            content.report.current.tempC, model.settings.units.temperature, Locale.getDefault()
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
    palette: WidgetPalette
) {
    PlaceLine(
        name = content.city.name,
        fromGps = model.fromGps,
        palette = palette,
        size = TextFactSp.sp
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
        text = staleText(context, content.lastSync, Instant.now()),
        style = TextStyle(color = palette.stale, fontSize = TextStaleSp.sp),
        maxLines = 1
    )
}
