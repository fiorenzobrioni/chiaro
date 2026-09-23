package com.callbackdev.chiaro.widget

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.ui.theme.paletteFor

/**
 * The sizes the «In parole» preview can be seen at (23 set 2026): the launcher's reference
 * grants for each cell count, the same ones `TextWidgetLayoutTest` measures every budget
 * against, so what the chips show is what the tests hold. Each of the four forms appears at
 * least twice, which is the point — the preview is where a reader learns that the card
 * re-lays itself out as it is resized, before they have tried.
 */
internal enum class TextPreviewSize(val label: String, val size: DpSize) {
    TWO_BY_ONE("2×1", DpSize(159.dp, 85.dp)),
    THREE_BY_ONE("3×1", DpSize(250.dp, 85.dp)),
    FOUR_BY_ONE("4×1", DpSize(340.dp, 85.dp)),
    TWO_BY_TWO("2×2", DpSize(159.dp, 189.dp)),
    THREE_BY_TWO("3×2", DpSize(250.dp, 189.dp)),
    FOUR_BY_TWO("4×2", DpSize(340.dp, 189.dp)),
    TWO_BY_THREE("2×3", DpSize(159.dp, 293.dp)),
    FOUR_BY_THREE("4×3", DpSize(340.dp, 293.dp))
}

/**
 * The size this widget really has on the home screen, in portrait — the launcher's own
 * `minWidth × maxHeight`, which is the pair a portrait grid grants — or null when the host
 * has not said yet (a card being placed for the first time can open this screen before
 * its first layout).
 */
internal fun placedTextSize(context: Context, appWidgetId: Int): DpSize? {
    val options = runCatching {
        AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId)
    }.getOrNull() ?: return null
    val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
    val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
    return if (width > 0 && height > 0) DpSize(width.dp, height.dp) else null
}

/** What each form carries, said on the configuration screen under the preview. */
internal fun textFormNote(form: TextForm): Int = when (form) {
    TextForm.LINE -> R.string.text_form_line
    TextForm.ROW -> R.string.text_form_row
    TextForm.STACK -> R.string.text_form_stack
    TextForm.PANEL -> R.string.text_form_panel
}

/**
 * **The card itself, on the configuration screen** (23 set 2026). Not a lookalike: the
 * arc widget's preview is re-laid out in Compose because a Glance composition cannot be
 * shown in an activity, but Glance can COMPOSE one into `RemoteViews` for any size
 * ([GlanceRemoteViews]), and `RemoteViews.apply` inflates those into ordinary views — which
 * is exactly what the launcher does with them. So this preview is [TextWidgetContent], the
 * same function the launcher's card runs, over the same model the card loads, at the size
 * the reader picked; a budget changed in `TextWidgetLayout` changes here with no second
 * copy to forget.
 *
 * The card opens the app when tapped, and a preview that did would throw the reader out of
 * the screen they are in the middle of, so the host view swallows every touch. It is one
 * picture to a screen reader, named as the preview it is: the switches around it already
 * say everything it shows.
 */
@OptIn(ExperimentalGlanceRemoteViewsApi::class)
@Composable
internal fun TextWidgetPreview(model: WidgetModel?, size: DpSize, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var views by remember { mutableStateOf<RemoteViews?>(null) }
    LaunchedEffect(model, size) {
        if (model == null) return@LaunchedEffect
        val schemes = widgetSchemes(context, model.settings.dynamicColor, model.settings.palette)
        val sky = model.content?.sky
            ?.takeIf { model.look.background == WidgetBackground.SKY }
            ?.let { skyGradientBitmap(it, model.look.opacityPct, paletteFor(model.settings.palette).sky) }
        views = runCatching {
            GlanceRemoteViews().compose(context, size) {
                TextWidgetContent(model, schemes, sky)
            }.remoteViews
        }.getOrNull()
    }
    val description = androidx.compose.ui.res.stringResource(R.string.widget_config_preview_desc)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = description }
    ) {
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
                views?.let { remote ->
                    AndroidView(
                        factory = { TouchlessFrame(it) },
                        update = { frame ->
                            frame.removeAllViews()
                            runCatching { remote.apply(frame.context, frame) }
                                .getOrNull()
                                ?.let { frame.addView(it) }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/** The preview's host: it keeps every touch for itself, so the card's own «open the app»
 * never fires from a settings screen, and hides the inflated views from accessibility. */
@SuppressLint("ViewConstructor")
private class TouchlessFrame(context: Context) : FrameLayout(context) {
    init {
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean = true

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean = true
}
