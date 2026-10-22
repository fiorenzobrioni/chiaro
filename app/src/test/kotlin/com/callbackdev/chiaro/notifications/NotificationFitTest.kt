package com.callbackdev.chiaro.notifications

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * What the pictured body carries is decided before the post (21 ott 2026): whole lines,
 * in their order, as many as fit the budget of visual lines at the phone's width and the
 * reader's font scale. The properties held here do not depend on a font's metrics — the
 * device's are not the sandbox's — only on [NotificationFit.lines] being the same measure
 * the fit uses. `NotificationFitRenderTest` (testDebug) then inflates the real bodies in
 * the platform's template and asks for no ellipsis anywhere.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "sw384dp-w384dp-h832dp-xhdpi")
class NotificationFitTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val short = "Adesso 20° · Coperto"
    private val long = "Stanotte minima 16° verso le 04:00 · Notte mite, si può lasciare la finestra aperta, " +
        "e domani alba alle 07:15 con tre minuti di luce in meno di oggi"

    private fun linesOf(text: String) =
        NotificationFit.lines(text, NotificationFit.paint(context), NotificationFit.textWidthPx(context))

    /** The stock template leaves 292 dp at 384 dp; the body is flowed in 92% of it. */
    @Test
    fun `the width is the template's, less a maker's share`() {
        val density = context.resources.displayMetrics.density
        val expectedDp = (384 - NotificationFit.TemplateMarginsDp) * NotificationFit.MakerShare
        assertEquals(expectedDp, NotificationFit.textWidthPx(context) / density, 0.5f)
    }

    @Test
    fun `a sentence longer than the width wraps, a short line does not`() {
        assertEquals(1, linesOf(short))
        assertTrue(linesOf(long) >= 2)
    }

    /** The budget follows the ceiling: a one-line headline and the default font leave room
     * for more than five lines under the day chart, a font at 1.3 for fewer, and never
     * does title, headline, picture and lines add up to more than the ceiling. */
    @Test
    fun `the budget is what the ceiling leaves, and shrinks with the font`() {
        val day = NotificationCharts.DayChartHeight
        val width = NotificationCharts.WIDTH
        val atNormal = NotificationFit.detailBudget(context, short, day, width)
        assertTrue("$atNormal", atNormal >= 5)
        val big = context.createConfigurationContext(
            android.content.res.Configuration(context.resources.configuration).apply { fontScale = 1.3f }
        )
        val atBig = NotificationFit.detailBudget(big, short, day, width)
        assertTrue("$atBig", atBig in 1 until atNormal)
        for ((ctx, budget) in listOf(context to atNormal, big to atBig)) {
            val scale = ctx.resources.configuration.fontScale
            val pictureDp = day * NotificationFit.contentWidthDp(ctx) / width
            val total = NotificationFit.TitleDp * scale + NotificationFit.HeadlineLineDp * scale +
                NotificationFit.PictureGapsDp + pictureDp + budget * NotificationFit.DetailLineDp * scale
            assertTrue("$total dp at $scale", total <= NotificationFit.CeilingDp)
        }
        // A headline that wraps takes a line from the details.
        assertTrue(NotificationFit.detailBudget(context, long, day, width) < atNormal)
        assertTrue(NotificationFit.detailBudget(context, short, day, width) <= NotificationFit.MaxDetailLines)
    }

    @Test
    fun `the lines kept are a head of the candidates, and never more than the budget`() {
        val candidates = listOf(short, long, short, long, short, short)
        val kept = NotificationFit.fit(context, candidates, budget = 5)
        assertEquals(candidates.take(kept.size), kept)
        assertTrue(kept.sumOf { linesOf(it) } <= 5)
        // Adding the next one would have gone over: nothing that fitted was left out.
        val next = candidates.getOrNull(kept.size)
        if (next != null) assertTrue(kept.sumOf { linesOf(it) } + linesOf(next) > 5)
    }

    /** Worth-first: the first line that does not fit closes the list, even when a shorter
     * one after it would have fitted — a body that skipped a line would reorder the story. */
    @Test
    fun `the first line that does not fit closes the list`() {
        val longLines = linesOf(long)
        val kept = NotificationFit.fit(context, listOf(long, long, long, short), budget = longLines * 2)
        assertEquals(listOf(long, long), kept)
    }

    @Test
    fun `a budget nothing fits in gives an empty body, and an empty offer stays empty`() {
        assertEquals(emptyList<String>(), NotificationFit.fit(context, listOf(long), budget = 1))
        assertEquals(emptyList<String>(), NotificationFit.fit(context, emptyList(), budget = 5))
    }

    /** From the device (22 ott 2026): the sunset line wrapped and took the air's line with
     * it. A line in forms takes the first that holds one line, the long one when none does. */
    @Test
    fun `a line's shorter form is taken when its long one would wrap, and the long one when none fits`() {
        val sunset = "Tramonto 18:25 · 3 minuti di luce in meno di ieri"
        val sunsetShort = "Tramonto 18:25 · 3 min di luce in meno"
        assertTrue(linesOf(sunset) > 1)
        assertEquals(1, linesOf(sunsetShort))
        val kept = NotificationFit.fitForms(
            context,
            listOf(listOf(short), listOf(sunset, sunsetShort), listOf(long, long), listOf(short)),
            budget = 3 + linesOf(long)
        )
        assertEquals(listOf(short, sunsetShort, long, short), kept)
        // The plain fit is the same with one form a line.
        assertEquals(NotificationFit.fit(context, listOf(short, long), 5), NotificationFit.fitForms(context, listOf(listOf(short), listOf(long)), 5))
    }

    @Test
    fun `the first form that fits one line is chosen, the last form is the fallback`() {
        assertEquals(short, NotificationFit.firstThatFits(context, listOf(long, short)))
        assertEquals(short, NotificationFit.firstThatFits(context, listOf(short, long)))
        assertEquals(long, NotificationFit.firstThatFits(context, listOf(long, long)))
    }

    /** A larger font takes more lines of the same width: the fit follows the reader's scale. */
    @Test
    @Config(qualifiers = "sw384dp-w384dp-h832dp-xhdpi", fontScale = 1.3f)
    fun `a larger font scale is measured as such`() {
        val big: Context = ApplicationProvider.getApplicationContext()
        val base = android.content.res.Configuration(big.resources.configuration).apply { fontScale = 1.0f }
        val normal = big.createConfigurationContext(base)
        val atNormal = NotificationFit.lines(long, NotificationFit.paint(normal), NotificationFit.textWidthPx(normal))
        val atBig = NotificationFit.lines(long, NotificationFit.paint(big), NotificationFit.textWidthPx(big))
        assertEquals(1, NotificationFit.lines(short, NotificationFit.paint(big), NotificationFit.textWidthPx(big)))
        assertTrue("$atBig lines at 1.3 against $atNormal", atBig > atNormal)
        assertTrue(NotificationFit.paint(big).textSize > NotificationFit.paint(normal).textSize)
    }
}
