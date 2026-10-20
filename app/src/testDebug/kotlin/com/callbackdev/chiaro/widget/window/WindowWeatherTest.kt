package com.callbackdev.chiaro.widget.window

import android.app.Application
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.readme.MilanRecording
import com.callbackdev.chiaro.ui.format.Formats
import com.callbackdev.chiaro.ui.today.WeatherText
import com.callbackdev.chiaro.widget.laterHours
import java.util.Locale
import com.callbackdev.chiaro.widget.WidgetKind
import com.callbackdev.chiaro.widget.WidgetModel
import com.callbackdev.chiaro.widget.widgetSchemes
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The window's weather line (20 ott 2026): the hour's temperature and word, only when asked for. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rGB-w384dp-h832dp-xhdpi")
class WindowWeatherTest {

    private val app: Application = ApplicationProvider.getApplicationContext()

    @OptIn(ExperimentalGlanceRemoteViewsApi::class)
    private fun texts(model: WidgetModel, size: DpSize = DpSize(340.dp, 189.dp)): List<String> {
        val composed = runBlocking {
            GlanceRemoteViews().compose(app, size) {
                WindowWidgetContent(model, widgetSchemes(app, model.settings.dynamicColor, model.settings.palette), null)
            }
        }
        val view = composed.remoteViews.apply(app, FrameLayout(app))
        val found = ArrayList<String>()
        fun walk(v: View) {
            if (v is TextView) found += v.text.toString()
            if (v is ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i))
        }
        walk(view)
        return found
    }

    @Test
    fun `the hour's weather is the Now card's own words, and only on request`() {
        val model = MilanRecording.widget(app, WidgetKind.WINDOW)
        val line = WindowText.weatherLine(app, model.content!!, model)
        assertEquals("20° · Overcast", line)
        assertFalse(line in texts(model))
        assertTrue(line in texts(model.copy(look = model.look.copy(windowWeather = true))))
    }

    /** On the tall card the hour's weather brings the next hours under the picture, the «In
     * words» table's own rows, in place of the sky's moments (committente, 20 ott 2026). */
    @Test
    fun `with the hour's weather on, the tall card's rows are the next hours, not the moments`() {
        val model = MilanRecording.widget(app, WidgetKind.WINDOW)
        val content = checkNotNull(model.content)
        val tall = DpSize(340.dp, 293.dp)
        val hours = laterHours(content.strip, content.now)
        assertTrue(hours.size >= 3)
        val clock = Formats.timeFormatter(android.text.format.DateFormat.is24HourFormat(app), Locale.getDefault())
        val firstHour = hours.first().hour.time.format(clock)
        val firstMoment = app.getString(R.string.sky_short_golden)

        val sky = texts(model, tall)
        assertTrue("the sky's rows: $sky", firstMoment in sky)
        assertFalse(firstHour in sky)

        val weather = texts(model.copy(look = model.look.copy(windowWeather = true)), tall)
        assertTrue("the hours' rows: $weather", firstHour in weather)
        assertTrue(app.getString(WeatherText.condition(hours.first().hour.condition.wmoCode)) in weather)
        assertFalse(firstMoment in weather)
    }
}
