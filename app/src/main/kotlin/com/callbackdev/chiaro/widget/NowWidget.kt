package com.callbackdev.chiaro.widget

import android.content.Context
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
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.callbackdev.chiaro.ui.format.Formats
import com.callbackdev.chiaro.ui.icons.ChiaroIcons
import com.callbackdev.chiaro.ui.today.HeadlineText
import com.callbackdev.chiaro.ui.today.TodayUiState
import com.callbackdev.chiaro.ui.today.WeatherText
import java.time.Instant
import java.util.Locale

/**
 * The Now widget (VISION §5.9): icon, temperature, place — the glance in the word's
 * old sense, drawn big enough to be one (device review, 3 set) — and, where the grant
 * has room, the day's sentence. Three forms, one per kind of grant ([NowLayout]); the
 * launcher's size picks the form and nothing else does. Stale data states its age; no
 * place says so; nothing here is ever a guess.
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
            val skyBitmap = rememberSkyBitmap(model)
            val content = model.content
            val size = LocalSize.current
            val layout = if (content == null) null else nowLayout(size)
            // Each edge is inset for what sits against it — a glyph brings a margin of
            // its own, words bring none (the numbers are in [WidgetCardPaddingLeading]).
            // One-row forms: the glyph leads the row and owns the height, so 4 dp before
            // it, 6 above and below, and the words' 14 at the far edge. The tall form:
            // the glyph sits in the top trailing corner, so those two edges take the
            // glyph's numbers and the other two the words'. An empty state has no glyph,
            // only words, and words get the words' inset on every side.
            val paddingStart = when (layout) {
                NowLayout.NARROW, NowLayout.WIDE -> WidgetCardPaddingLeading
                NowLayout.TALL, null -> WidgetCardPadding
            }
            val paddingEnd = when (layout) {
                NowLayout.TALL -> WidgetCardPaddingLeading
                NowLayout.NARROW, NowLayout.WIDE, null -> WidgetCardPaddingTrailing
            }
            val paddingTop = when (layout) {
                NowLayout.NARROW, NowLayout.WIDE, NowLayout.TALL -> WidgetCardPaddingSnug
                null -> WidgetCardPadding
            }
            val paddingBottom = when (layout) {
                NowLayout.NARROW, NowLayout.WIDE -> WidgetCardPaddingSnug
                NowLayout.TALL, null -> WidgetCardPadding
            }
            WidgetCard(
                model, schemes, skyBitmap,
                contentPaddingStart = paddingStart,
                contentPaddingEnd = paddingEnd,
                contentPaddingTop = paddingTop,
                contentPaddingBottom = paddingBottom
            ) { palette ->
                when {
                    content == null && model.city == null -> NoPlaceContent(palette)
                    content == null -> NoDataContent(palette)
                    layout == NowLayout.TALL -> TallContent(content, model, palette, size)
                    else -> RowContent(
                        content, model, palette, size,
                        withSentence = layout == NowLayout.WIDE
                    )
                }
            }
        }
    }
}

/**
 * The one-row card: glyph, then the temperature over the place, then — on a card wide
 * enough ([NowLayout.WIDE]) — the day's sentence against the far edge. The two text
 * columns split the row's slack evenly ([nowSentenceColumnWidth]), so the temperature
 * block keeps its place at the glyph's side, the sentence keeps the far edge, and the
 * empty space lands in the middle, which is where the reference widget puts it too.
 */
@Composable
private fun RowContent(
    content: TodayUiState.Content,
    model: WidgetModel,
    palette: WidgetPalette,
    size: DpSize,
    withSentence: Boolean
) {
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = GlanceModifier.fillMaxSize()
    ) {
        HeroIcon(content, model, palette, nowRowIconSize(size))
        // The bottom padding balances the leading above "23°", so the words' ink and
        // the glyph's ink share a centre line instead of the two boxes sharing one
        // (committente, 5th device pass — the icon read high by exactly half that band).
        Column(
            modifier = GlanceModifier
                .padding(start = IconTextGap, bottom = textInkBalance(context, TemperatureSp))
                .defaultWeight()
        ) {
            Temperature(content, model, palette)
            PlaceLine(
                name = content.city.name,
                fromGps = model.fromGps,
                palette = palette,
                size = PlaceSp.sp
            )
            StaleLine(content, palette)
        }
        if (withSentence) {
            // Right-aligned and centred on the row, as the reference draws it. The
            // TextView fills its column so a one-line sentence still sits at the far
            // edge rather than floating where its own width ends.
            Column(
                horizontalAlignment = Alignment.End,
                modifier = GlanceModifier
                    .padding(start = SentenceGap)
                    .defaultWeight()
            ) {
                Text(
                    text = sentence(context, content),
                    style = sentenceStyle(palette, TextAlign.End),
                    maxLines = nowSentenceLines(size, fontScale(context)),
                    modifier = GlanceModifier.fillMaxWidth()
                )
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
 * the two could touch is a band that is empty by construction.
 */
@Composable
private fun TallContent(
    content: TodayUiState.Content,
    model: WidgetModel,
    palette: WidgetPalette,
    size: DpSize
) {
    val context = LocalContext.current
    Box(
        contentAlignment = Alignment.TopEnd,
        modifier = GlanceModifier.fillMaxSize()
    ) {
        HeroIcon(
            content, model, palette,
            nowTallIconSize(size, fontScale(context), content.isStale)
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
            Temperature(content, model, palette)
            Text(
                text = sentence(context, content),
                style = sentenceStyle(palette, TextAlign.Start),
                maxLines = TallSentenceMaxLines,
                modifier = GlanceModifier.fillMaxWidth()
            )
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
                model.iconStyle, palette.darkGround,
                model.settings.palette
            )
        ),
        contentDescription = null, // the temperature and the sentence say it in words
        modifier = GlanceModifier.size(size)
    )
}

@Composable
private fun Temperature(
    content: TodayUiState.Content,
    model: WidgetModel,
    palette: WidgetPalette
) {
    Text(
        text = Formats.temperature(
            content.report.current.tempC, model.settings.units.temperature, Locale.getDefault()
        ),
        style = TextStyle(
            color = palette.primary,
            fontSize = TemperatureSp.sp,
            fontWeight = FontWeight.Medium
        ),
        maxLines = 1
    )
}

/** The stale marker (VISION §5.9): with old data the widget says how old, under the
 * place, on every form. */
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
 * What the sentence slot says: the day's headline when there is one — the same
 * [HeadlineText] Today opens with, in its brief register, so the widget and the screen
 * never tell two stories about the same afternoon — and the sky's present state when
 * there is nothing to warn about. The fallback is not a filler: «Poco nuvoloso» is a
 * true thing about the sky right now, which is what this card is for, and a slot that
 * went blank on every quiet day would read as a card that failed to load.
 */
private fun sentence(context: Context, content: TodayUiState.Content): String {
    val locale = Locale.getDefault()
    val timeFmt = Formats.timeFormatter(
        android.text.format.DateFormat.is24HourFormat(context), locale
    )
    return HeadlineText.of(context, content.headline, timeFmt, brief = true)
        ?: context.getString(WeatherText.condition(content.report.current.condition.wmoCode))
}

/** The sentence's dress on both forms: the Today widget's own 14 sp Medium in the
 * strong ink — the second thing read after the number, and the same sentence the other
 * widget prints, so it wears the same clothes there and here. */
private fun sentenceStyle(palette: WidgetPalette, align: TextAlign): TextStyle = TextStyle(
    color = palette.primary,
    fontSize = SentenceSp.sp,
    fontWeight = FontWeight.Medium,
    textAlign = align
)

/** Read off a Context rather than a composition local for the reason [isNight] is:
 * there is no `LocalConfiguration` on the launcher's side of the fence. */
private fun fontScale(context: Context): Float = context.resources.configuration.fontScale
