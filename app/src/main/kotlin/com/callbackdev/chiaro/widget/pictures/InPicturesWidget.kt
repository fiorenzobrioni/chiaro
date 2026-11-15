package com.callbackdev.chiaro.widget.pictures

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import com.callbackdev.chiaro.widget.ChiaroWidgetReceiver
import com.callbackdev.chiaro.widget.WidgetData
import com.callbackdev.chiaro.widget.WidgetRefresh
import com.callbackdev.chiaro.widget.rememberSkyBitmap
import com.callbackdev.chiaro.widget.rememberWidgetModel
import com.callbackdev.chiaro.widget.rememberWidgetSchemes

class InPicturesWidgetReceiver : ChiaroWidgetReceiver() {
    override val glanceAppWidget = InPicturesWidget()
}

/**
 * «In immagini» (Fase 17, committente 15 nov 2026): the drawn twin of «In parole». Where that
 * card writes the day, this one draws it — the temperature as a curve, the rain as bars, the
 * light as a ribbon — with the same facts and the same sentence, on the canvas the committente
 * approved (`docs/design/in-immagini/`).
 *
 * It resizes from two cells by one to four by four and redraws itself at every step
 * (`InPicturesLayout.kt`); like every card of the family it never invents (VISION §5.9): no
 * place and no data are said, old data states its age, and what is drawn is the cached report.
 * It is repainted by the shared periodic job with the others ([com.callbackdev.chiaro.widget.ChiaroWidgets]):
 * no clock of its own.
 */
class InPicturesWidget : GlanceAppWidget() {

    /** Exact sizing: every form, every rect and every bitmap is read off the size the
     * launcher really granted. */
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = runCatching {
            GlanceAppWidgetManager(context).getAppWidgetId(id)
        }.getOrDefault(0)
        val loadedAt = WidgetRefresh.revision.value
        val initial = WidgetData.load(context, appWidgetId)
        provideContent {
            val model = rememberWidgetModel(context, appWidgetId, initial, loadedAt)
            val schemes = rememberWidgetSchemes(context, model.settings.dynamicColor, model.settings.palette)
            InPicturesContent(model, schemes, rememberSkyBitmap(model))
        }
    }
}
