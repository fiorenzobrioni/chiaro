package com.callbackdev.chiaro.widget

import android.content.Context
import androidx.compose.runtime.Composable
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
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.callbackdev.chiaro.ui.format.Formats
import com.callbackdev.chiaro.ui.icons.ChiaroIcons
import com.callbackdev.chiaro.ui.today.TodayUiState
import com.callbackdev.chiaro.ui.today.WeatherText
import java.time.Instant
import java.util.Locale

/**
 * The Now widget (VISION §5.9): icon, temperature, place — the glance in the word's
 * old sense, drawn big enough to be one (device review, 3 set). Stale data states
 * its age; no place says so; nothing here is ever a guess.
 */
class NowWidget : GlanceAppWidget() {

    /** Exact sizing so [LocalSize] is the height the launcher really granted: the
     * icon fills it (device review, 3 set — a fixed size fits one cell only). */
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
            val schemes = rememberWidgetSchemes(context, model.settings.dynamicColor)
            val skyBitmap = rememberSkyBitmap(model)
            WidgetCard(
                model, schemes, skyBitmap,
                contentPadding = WidgetCardPaddingSnug,
                // Nothing on the leading edge: the glyph's own margin is the inset,
                // and it lands the icon where the neighbouring weather widgets put
                // theirs (measured on the device's screenshot, 4th pass).
                contentPaddingStart = 0.dp
            ) { palette ->
                when (val content = model.content) {
                    null -> if (model.city == null) {
                        NoPlaceContent(palette)
                    } else {
                        NoDataContent(palette)
                    }
                    else -> NowContent(content, model, palette)
                }
            }
        }
    }
}

/** The hero number's size, named because the ink balance below is measured off it. */
private const val TemperatureSp = 34f

/**
 * The state's size and the air around it (committente, 4 set: at the place name's
 * 15sp and 6dp away it read as an afterthought stuck to the degree sign).
 *
 * 20sp is roughly three fifths of the hero, which puts three clear steps on the card
 * — 34 for the number, 20 for what the sky is doing, 15 for where — instead of two
 * sizes competing and one of them losing. The gap is the card's own 12, and the
 * degree sign donates a little more optical space on top of it.
 */
private const val ConditionSp = 20f
private val ConditionGap = 12.dp

@Composable
private fun NowContent(
    content: TodayUiState.Content,
    model: WidgetModel,
    palette: WidgetPalette
) {
    val context = LocalContext.current
    val locale = Locale.getDefault()
    val current = content.report.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = GlanceModifier.fillMaxSize()
    ) {
        Image(
            provider = ImageProvider(
                ChiaroIcons.conditionRes(
                    current.condition.wmoCode, content.night,
                    model.settings.weatherIcons, palette.darkGround
                )
            ),
            contentDescription = null, // the temperature and place say it in words
            // One row, one hero: the icon takes the whole height the card is not
            // using, floor 56dp so a squeezed grant stays legible.
            modifier = GlanceModifier.size(
                heroIconSize(
                    LocalSize.current.height - WidgetCardPaddingSnug * 2,
                    min = 56.dp
                )
            )
        )
        // 8dp, not 12: a quarter of the glyph's box is already empty on that side.
        // The bottom balances the leading above "23°", so the words' ink and the
        // glyph's ink share a centre line instead of the two boxes sharing one
        // (committente, 5th device pass — the icon read high by exactly half that
        // band).
        Column(
            modifier = GlanceModifier
                .padding(start = 8.dp, bottom = textInkBalance(context, TemperatureSp))
                .fillMaxWidth()
        ) {
            // Icon, temperature, place — VISION §5.9's three, and only those: the
            // day's range next to the number read as clutter on the home screen
            // (committente, 3 set) and it is one tap away in the app.
            //
            // The fourth thing, the sky's state, is beside the number and OFF unless
            // the reader asked for it in the widget's own settings (committente,
            // 4 set): the standard dress is the one tuned on device, and this is the
            // room a wide widget can spend rather than a size the layout reacts to.
            // Optically centred, not bottom- or baseline-aligned (committente, 4 set
            // — bottom-aligned and small, the word read as something stuck on the
            // number rather than said with it). Glance has no baseline alignment; but
            // at these two sizes the system font puts each block's visible ink almost
            // exactly at the centre of its own box, so centring the two boxes centres
            // the two inks — which is the treatment a small label beside a big numeral
            // wants anyway.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = Formats.temperature(
                        current.tempC, model.settings.units.temperature, locale
                    ),
                    style = TextStyle(
                        color = palette.primary,
                        fontSize = TemperatureSp.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                if (model.look.showCondition) {
                    Text(
                        text = context.getString(
                            WeatherText.condition(current.condition.wmoCode)
                        ),
                        style = secondaryStyle(palette, ConditionSp.sp),
                        maxLines = 1,
                        modifier = GlanceModifier.padding(start = ConditionGap)
                    )
                }
            }
            Text(
                text = content.city.name,
                style = secondaryStyle(palette, 15.sp),
                maxLines = 1
            )
            if (content.isStale) {
                Text(
                    text = staleText(context, content.lastSync, Instant.now()),
                    style = TextStyle(color = palette.stale, fontSize = 11.sp)
                )
            }
        }
    }
}
