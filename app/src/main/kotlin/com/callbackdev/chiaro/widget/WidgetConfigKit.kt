package com.callbackdev.chiaro.widget

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.ui.theme.paletteFor
import com.callbackdev.chiaro.widget.arc.ArcForm
import com.callbackdev.chiaro.widget.arc.ArcSettings
import com.callbackdev.chiaro.widget.arc.ArcWidgetContent
import com.callbackdev.chiaro.widget.arc.arcForm
import kotlin.math.roundToInt

/*
 * The parts every widget's settings screen is built from (23 set 2026, widget review):
 * the card itself at the top, at the sizes the launcher can grant it, and the choices under
 * it on the Settings screen's rounded groups. Two activities use them — the shared one for
 * Now, Today, Sky and «In parole», and the arc widget's own — so the five screens read as
 * one app and a change to the grammar lands on all of them at once.
 */

/**
 * A launcher grant, named by its cells: the household's reference sizes, the ones every
 * `*WidgetLayoutTest` measures its budget against, so what a chip shows is what the tests
 * hold.
 */
internal enum class PreviewSize(val label: String, val size: DpSize) {
    ONE_BY_ONE("1×1", DpSize(85.dp, 85.dp)),
    TWO_BY_ONE("2×1", DpSize(159.dp, 85.dp)),
    THREE_BY_ONE("3×1", DpSize(250.dp, 85.dp)),
    FOUR_BY_ONE("4×1", DpSize(340.dp, 85.dp)),
    TWO_BY_TWO("2×2", DpSize(159.dp, 189.dp)),
    THREE_BY_TWO("3×2", DpSize(250.dp, 189.dp)),
    FOUR_BY_TWO("4×2", DpSize(340.dp, 189.dp)),
    TWO_BY_THREE("2×3", DpSize(159.dp, 293.dp)),
    THREE_BY_THREE("3×3", DpSize(250.dp, 293.dp)),
    FOUR_BY_THREE("4×3", DpSize(340.dp, 293.dp)),
    TWO_BY_FOUR("2×4", DpSize(159.dp, 397.dp)),
    FOUR_BY_FOUR("4×4", DpSize(340.dp, 397.dp))
}

/**
 * The sizes a card can really be given, from its provider's own minimum (`res/xml`): a
 * chip for a grant the launcher will refuse would be teaching a card nobody can have. Every
 * form each card has appears at least once, and the default placement is in every list.
 */
internal fun previewSizes(kind: WidgetKind?): List<PreviewSize> = when (kind) {
    WidgetKind.TODAY -> listOf(
        PreviewSize.THREE_BY_TWO, PreviewSize.FOUR_BY_TWO,
        PreviewSize.THREE_BY_THREE, PreviewSize.FOUR_BY_THREE, PreviewSize.FOUR_BY_FOUR
    )
    WidgetKind.SKY -> listOf(
        PreviewSize.THREE_BY_ONE, PreviewSize.FOUR_BY_ONE, PreviewSize.THREE_BY_TWO,
        PreviewSize.FOUR_BY_TWO, PreviewSize.THREE_BY_THREE, PreviewSize.FOUR_BY_THREE
    )
    WidgetKind.ARC -> listOf(
        PreviewSize.ONE_BY_ONE, PreviewSize.TWO_BY_ONE, PreviewSize.FOUR_BY_ONE,
        PreviewSize.TWO_BY_TWO, PreviewSize.FOUR_BY_TWO, PreviewSize.FOUR_BY_THREE,
        PreviewSize.TWO_BY_FOUR, PreviewSize.FOUR_BY_FOUR
    )
    else -> listOf(
        PreviewSize.TWO_BY_ONE, PreviewSize.THREE_BY_ONE, PreviewSize.FOUR_BY_ONE,
        PreviewSize.TWO_BY_TWO, PreviewSize.THREE_BY_TWO, PreviewSize.FOUR_BY_TWO,
        PreviewSize.TWO_BY_THREE, PreviewSize.FOUR_BY_THREE
    )
}

/** Where a card lands when it is first placed: its provider's `targetCell*`. */
internal fun defaultPreviewSize(kind: WidgetKind?): PreviewSize = when (kind) {
    WidgetKind.TODAY, WidgetKind.ARC -> PreviewSize.FOUR_BY_TWO
    else -> PreviewSize.FOUR_BY_ONE
}

/**
 * The size this widget really has on the home screen, in portrait — the launcher's own
 * `minWidth × maxHeight`, which is the pair a portrait grid grants — or null when the host
 * has not said yet (a card being placed for the first time can open its settings before
 * its first layout).
 */
internal fun placedWidgetSize(context: Context, appWidgetId: Int): DpSize? {
    val options = runCatching {
        AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId)
    }.getOrNull() ?: return null
    val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
    val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
    return if (width > 0 && height > 0) DpSize(width.dp, height.dp) else null
}

/**
 * One sentence under the preview saying what a card of [kind] carries at [size] — read off
 * the same functions the card lays itself out with, so the sentence and the card can never
 * describe two different forms.
 */
internal fun formNote(kind: WidgetKind?, size: DpSize): Int = when (kind) {
    WidgetKind.NOW -> when (nowLayout(size)) {
        NowLayout.NARROW -> R.string.now_form_narrow
        NowLayout.WIDE -> R.string.now_form_wide
        NowLayout.TALL -> R.string.now_form_tall
    }
    WidgetKind.TODAY -> if (
        todayShowDays(size, 1f, false, sentence = true, range = false, warning = false, rain = true)
    ) {
        R.string.today_form_tall
    } else {
        R.string.today_form_two
    }
    WidgetKind.SKY -> when {
        skyIsTall(size) -> R.string.sky_form_tall
        skyIsWide(size) -> R.string.sky_form_wide
        else -> R.string.sky_form_narrow
    }
    WidgetKind.ARC -> when (arcForm(size)) {
        ArcForm.DIAL -> R.string.arc_form_dial
        ArcForm.STRIP -> R.string.arc_form_strip
        ArcForm.CARD -> R.string.arc_form_card
        ArcForm.PANEL -> R.string.arc_form_panel
        ArcForm.BOARD -> R.string.arc_form_board
    }
    else -> when (textForm(size)) {
        TextForm.LINE -> R.string.text_form_line
        TextForm.ROW -> R.string.text_form_row
        TextForm.STACK -> R.string.text_form_stack
        TextForm.PANEL -> R.string.text_form_panel
    }
}

/**
 * The card as it will look, on a ground that stands in for a wallpaper, with the sizes it
 * can be seen at underneath and one line saying what that size carries. It opens on the
 * size the card really has on the home screen when the launcher has said it, so the first
 * thing the reader sees is THEIR card; the other chips are the forms it turns into when
 * resized, which is the one thing a settings screen can teach that the home screen cannot —
 * it only ever shows one size at a time.
 */
@Composable
internal fun WidgetPreviewSection(
    appWidgetId: Int,
    kind: WidgetKind?,
    model: WidgetModel?,
    arc: ArcSettings? = null
) {
    val context = LocalContext.current
    val placed = remember(appWidgetId) { placedWidgetSize(context, appWidgetId) }
    var size by remember { mutableStateOf(placed ?: defaultPreviewSize(kind).size) }
    val colors = MaterialTheme.colorScheme
    // A wallpaper out of the reader's own scheme: two containers on a diagonal, so a
    // see-through card shows it is see-through and a solid one shows its edge.
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(listOf(colors.primaryContainer, colors.tertiaryContainer))
            )
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        WidgetPreview(kind = kind, model = model, arc = arc, size = size)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        placed?.let { here ->
            FilterChip(
                selected = size == here,
                onClick = { size = here },
                label = { Text(stringResource(R.string.widget_config_size_placed)) }
            )
        }
        previewSizes(kind).forEach { option ->
            FilterChip(
                selected = size == option.size && size != placed,
                onClick = { size = option.size },
                label = { Text(option.label) }
            )
        }
    }
    Text(
        text = stringResource(formNote(kind, size)),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp)
    )
    Text(
        text = stringResource(R.string.widget_config_resize_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp)
    )
}

/**
 * **The card itself, not a lookalike** (23 set 2026). Glance can COMPOSE a card into
 * `RemoteViews` for any size ([GlanceRemoteViews]), and `RemoteViews.apply` inflates those
 * into ordinary views — which is exactly what the launcher does with them. So this preview
 * runs the same `*WidgetContent` function the card's receiver runs, over the same model the
 * card loads, at the size the reader picked; a budget changed in a `*WidgetLayout.kt`
 * changes here with no second copy to forget. (The arc card's first preview was re-laid out
 * in Compose on the belief that a Glance composition cannot be shown in an activity; it can,
 * and that copy is gone.)
 *
 * The card opens the app when tapped, and a preview that did would throw the reader out of
 * the screen they are in the middle of, so the host view swallows every touch. It is one
 * picture to a screen reader, named as the preview it is: the choices around it say
 * everything it shows. If the composition ever fails, nothing is drawn rather than a
 * half-card.
 */
@OptIn(ExperimentalGlanceRemoteViewsApi::class)
@Composable
internal fun WidgetPreview(
    kind: WidgetKind?,
    model: WidgetModel?,
    arc: ArcSettings?,
    size: DpSize,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var views by remember { mutableStateOf<RemoteViews?>(null) }
    LaunchedEffect(kind, model, arc, size) {
        if (model == null) return@LaunchedEffect
        val schemes = widgetSchemes(context, model.settings.dynamicColor, model.settings.palette)
        val sky = model.content?.sky
            ?.takeIf { model.look.background == WidgetBackground.SKY }
            ?.let { skyGradientBitmap(it, model.look.opacityPct, paletteFor(model.settings.palette).sky) }
        views = runCatching {
            GlanceRemoteViews().compose(context, size) {
                when (kind) {
                    WidgetKind.NOW -> NowWidgetContent(model, schemes, sky)
                    WidgetKind.TODAY -> TodayWidgetContent(model, schemes, sky)
                    WidgetKind.SKY -> SkyWidgetContent(model, schemes, sky)
                    WidgetKind.ARC -> ArcWidgetContent(model, schemes, sky, arc ?: ArcSettings())
                    else -> TextWidgetContent(model, schemes, sky)
                }
            }.remoteViews
        }.getOrNull()
    }
    val description = stringResource(R.string.widget_config_preview_desc)
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

/** A group's title, the Settings screen's: the section's name above its card. */
@Composable
internal fun ConfigHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

/** Rows that belong together, on one rounded ground — the Settings and Alerts screens'
 * grouping, so the widgets' own settings read as part of the same app. */
@Composable
internal fun ConfigGroup(content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = ConfigGroupShape,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(ConfigGroupShape)
    ) {
        Column { content() }
    }
}

@Composable
internal fun ConfigDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

private val ConfigGroupShape = RoundedCornerShape(24.dp)

@Composable
internal fun ConfigChoiceRow(
    label: String,
    selected: Boolean,
    onPick: () -> Unit,
    inset: Dp = 16.dp,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onPick, role = Role.RadioButton)
            .padding(horizontal = inset, vertical = 10.dp)
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 12.dp)
        )
        trailing?.invoke()
    }
}

/** A toggle with the sentence that says what it does — the Settings screen's shape, so a
 * reader meets one control, not two. */
@Composable
internal fun ConfigSwitchRow(
    label: String,
    note: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, onValueChange = onToggle, role = Role.Switch)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = note,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

/** A list of switches in one group, hairlines between them. */
@Composable
internal fun ConfigSwitches(rows: List<ConfigSwitch>) {
    ConfigGroup {
        rows.forEachIndexed { index, row ->
            if (index > 0) ConfigDivider()
            ConfigSwitchRow(row.label, row.note, row.checked, row.onToggle)
        }
    }
}

internal data class ConfigSwitch(
    val label: String,
    val note: String,
    val checked: Boolean,
    val onToggle: (Boolean) -> Unit
)

/** A list of choices in one group: one answer, radio rows, no hairlines (one question). */
@Composable
internal fun <T> ConfigChoices(options: List<Pair<T, String>>, selected: T, onPick: (T) -> Unit) {
    ConfigGroup {
        options.forEach { (value, label) ->
            ConfigChoiceRow(label = label, selected = value == selected, onPick = { onPick(value) })
        }
    }
}

/** How solid the card is: the name and the value on one line, the slider under them. */
@Composable
internal fun OpacityRow(pct: Int, onChange: (Int) -> Unit, onDone: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.widget_config_opacity),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = when (pct) {
                    100 -> stringResource(R.string.settings_opacity_full)
                    0 -> stringResource(R.string.widget_opacity_transparent)
                    else -> "$pct%"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = pct.toFloat(),
            onValueChange = { raw -> onChange((raw / 5f).roundToInt() * 5) },
            onValueChangeFinished = onDone,
            valueRange = 0f..100f,
            steps = 19
        )
    }
}

/** «Fatto», full width at the foot of every widget's settings: the choices are saved as
 * they are made, so this only closes the door. */
@Composable
internal fun ConfigDoneButton(onDone: () -> Unit) {
    Button(
        onClick = onDone,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 24.dp)
    ) {
        Text(stringResource(R.string.action_done))
    }
}
