package com.callbackdev.chiaro.domain.pictures

import com.callbackdev.chiaro.domain.WeatherStateEngine
import java.time.Duration
import java.time.Instant

/**
 * **La lastra** (the plate): the drawn half of the 3×2, 4×2, 4×3 and 4×4 cards — the curve
 * of the temperature, the rain under it, the ribbon of light and the hours, one over the
 * other on a 100 dp panel (`docs/design/in-immagini/Anatomy.dc.html`, `chart()`).
 *
 * Every y below is measured from the plate's own top edge and every x from its own left
 * edge, in dp; the plate's width is the card's (312 on the 4×2, 226 on the 3×2), its height
 * is always [HEIGHT]. The constants are the canvas's, named for what they are.
 */
object PlateMetrics {
    /** The plate's height on every form: the canvas's `height: 100px`. */
    const val HEIGHT = 100f

    /** The plate's corner: the launcher's inner corner, 16 dp (PLANNING, «Le otto forme»). */
    const val CORNER = 16f

    /** Where the axis starts and ends: 6 dp in from each side, the canvas's `x0 = 6` and the
     * ribbon's `left: 6px; right: 6px`. The curve, the bars, the ribbon and the ticks share it,
     * so an hour has one x on all four. */
    const val INSET = 6f

    /**
     * **2.5 dp per degree, always** (committente, 15 nov 2026, after the 17.1 measurement):
     * the scale anchors to the world, not to the screen, so a flat day stays flat and a cold
     * front is a cliff. Per degree Celsius whatever unit the reader prints — the shape of a
     * day does not change with its labels.
     *
     * The canvas and the plan said 3.6, which holds 7.8° between [CURVE_TOP] and
     * [CURVE_FLOOR]; on the 26 cities of `tools/measurements` that narrowed the scale in 55%
     * of the autumn's 24-hour windows and 25% of the winter's, so «always» was true half the
     * time. 2.5 holds 11.2°: it narrows one autumn day in five (20.6%) and almost no winter
     * one (3.1%). PLANNING, 17.1, «La scala della curva, misurata».
     */
    const val DEGREE = 2.5f

    /**
     * Where the day's warmest point stands: 21 dp, so the high's label (19 dp above it, 14
     * tall) starts 2 dp inside the plate. The canvas's example puts its 12.1° at 21.08 dp
     * (`top 14 + (12.4 − 12.1) × 3.6`, inside an svg 6 dp down), with the 12.4° anchor chosen
     * by hand for that day; this is the rule that number stood for, rounded.
     */
    const val CURVE_TOP = 21f

    /**
     * The lowest the coldest point may stand: 49 dp, so the low's label (5 dp under it, 14
     * tall) ends on the rain row's baseline ([RAIN_BASELINE]) and never runs into the ribbon.
     * Between [CURVE_TOP] and here there are 28 dp, 11.2° at [DEGREE]: an excursion wider
     * than that **narrows the scale until it fits** (PLANNING, «La lastra»), and only then.
     */
    const val CURVE_FLOOR = 49f

    /** The area under the curve closes here, the bottom of the canvas's 44 dp svg drawn 6 dp
     * down: it fades from [AREA_ALPHA_TOP] at the curve to nothing at this line. */
    const val AREA_BOTTOM = 50f
    const val AREA_ALPHA_TOP = 0.28f

    /** The curve's stroke: white, 2.4 dp, round caps and joins. */
    const val STROKE = 2.4f

    /** «Adesso» on the curve: a 12 dp white disc with a 3 dp ring of the veil at 35%. */
    const val NOW_DISC = 12f
    const val NOW_RING = 3f
    const val NOW_RING_ALPHA = 0.35f

    /** The high and the low, on their own point and never in a legend: 13 sp Semibold in a
     * 28 × 14 box centred on the point, the high's box 19 dp above it, the low's 5 dp under. */
    const val LABEL_SP = 13f
    const val LABEL_WIDTH = 28f
    const val LABEL_HEIGHT = 14f
    const val HIGH_LABEL_RISE = 19f
    const val LOW_LABEL_DROP = 5f

    /** The rain's own row, 50 to 68: 18 dp is 100%, on a 1 dp baseline of white at 14% that
     * runs the axis. A bar is [BAR_SHARE] of an hour wide (never under [BAR_MIN]), its corners
     * 3 dp at the top and 1 at the foot. */
    const val RAIN_TOP = 50f
    const val RAIN_HEIGHT = 18f
    const val RAIN_BASELINE = 68f
    const val RAIN_BASELINE_ALPHA = 0.14f
    const val BAR_SHARE = 0.62f
    const val BAR_MIN = 4f
    const val BAR_CORNER_TOP = 3f
    const val BAR_CORNER_BOTTOM = 1f

    /** The ribbon of light, 73 to 79, corners 3: the hero's phases over the window. */
    const val RIBBON_TOP = 73f
    const val RIBBON_HEIGHT = 6f
    const val RIBBON_CORNER = 3f

    /** The hours, at 83: 11 sp in the quiet ink, centred in a 24 dp box on their hour. */
    const val TICK_TOP = 83f
    const val TICK_WIDTH = 24f
    const val TICK_SP = 11f
    const val TICK_LINE = 13f

    /** The plate's veil on the sky: `rgba(6, 24, 52, 0.30)`, and its 1 dp inner hairline,
     * white at 10%. The other grounds' veils are the painter's (PLANNING, «Le otto forme»). */
    const val VEIL_ALPHA = 0.30f
    const val HAIRLINE_ALPHA = 0.10f

    /**
     * How far the canvas's own drawing reaches past the plate's edges: the disc of «adesso»
     * at the first hour is centred on the axis's start, so its ring ends 3 dp outside the
     * plate, and «ora» is centred there too, 2.5 dp outside. The canvas draws them over the
     * card's padding; the painter's bitmap has to keep this much room around the plate, or
     * it clips what the approved design shows.
     */
    const val OVERHANG = 6f
}

/** A point on the plate, in dp from its top left corner. */
data class PlatePoint(val x: Float, val y: Float)

/** One cubic of the curve, in the order a path draws it. */
data class PlateCubic(
    val start: PlatePoint,
    val control1: PlatePoint,
    val control2: PlatePoint,
    val end: PlatePoint
) {
    fun at(u: Float): PlatePoint {
        val v = 1f - u
        val a = v * v * v
        val b = 3f * v * v * u
        val c = 3f * v * u * u
        val d = u * u * u
        return PlatePoint(
            a * start.x + b * control1.x + c * control2.x + d * end.x,
            a * start.y + b * control1.y + c * control2.y + d * end.y
        )
    }
}

/**
 * An unbroken stretch of the curve: consecutive hours with a temperature. A missing hour
 * breaks the curve rather than being bridged — a line between two known hours across an
 * unknown one is a forecast nobody made.
 */
data class PlateRun(val points: List<PlatePoint>) {
    /**
     * The canvas's `smooth()`: Catmull-Rom at a sixth, the end points doubled, so the curve
     * passes through every hour and the drawing is the canvas's to the tenth of a dp. A run
     * of one hour has no cubic; the painter draws it as a dot of the stroke's width.
     */
    val cubics: List<PlateCubic>
        get() = (0 until points.size - 1).map { i ->
            val p0 = points.getOrElse(i - 1) { points[i] }
            val p1 = points[i]
            val p2 = points[i + 1]
            val p3 = points.getOrElse(i + 2) { p2 }
            PlateCubic(
                start = p1,
                control1 = PlatePoint(p1.x + (p2.x - p0.x) / 6f, p1.y + (p2.y - p0.y) / 6f),
                control2 = PlatePoint(p2.x - (p3.x - p1.x) / 6f, p2.y - (p3.y - p1.y) / 6f),
                end = p2
            )
        }

    /** The drawn curve's height at [x], or null outside this run: what the disc of «adesso»
     * sits on, so it stands on the line and not on a straight chord under it. */
    fun yAt(x: Float): Float? {
        if (points.isEmpty() || x < points.first().x || x > points.last().x) return null
        if (points.size == 1) return points.first().y
        val cubic = cubics.first { x <= it.end.x }
        // x(u) is monotonic on a run whose hours go forward; halve until it is the x asked.
        var lo = 0f
        var hi = 1f
        repeat(BISECTIONS) {
            val mid = (lo + hi) / 2f
            if (cubic.at(mid).x < x) lo = mid else hi = mid
        }
        return cubic.at((lo + hi) / 2f).y
    }

    private companion object {
        const val BISECTIONS = 24
    }
}

/** The high or the low, with the box its label is drawn in. */
data class PlateExtreme(
    val temperatureC: Double,
    val point: PlatePoint,
    val labelLeft: Float,
    val labelTop: Float
)

/** The intensity a rain bar's tone says, on the state engine's own ladder (STATES §2 and §3, rule 3:
 * drizzle under 0.5 mm/h, light rain to 2.5, rain to 7.6, heavy beyond). Four tones of one
 * blue, lighter is stronger; the painter owns the four inks and their contrast. */
enum class RainTone {
    DRIZZLE, LIGHT, MODERATE, HEAVY;

    companion object {
        /**
         * The canvas toned its bars at 0.4 / 0.8 / 1.5 mm, which were placeholders (PLANNING,
         * «La lastra»): these are the engine's, so the bar and the hour's word cannot disagree
         * about how hard it rains. The amount is the hour's whole water, snow included — the
         * bar is the chance that something falls, and its tone how much. An hour with no
         * amount (a cache entry from before 15.10) is drawn in the quietest tone: the bar
         * still says the chance, which is known, and claims no intensity.
         */
        fun of(amountMm: Double?): RainTone = when {
            amountMm == null || amountMm < WeatherStateEngine.DRIZZLE_MAX_MM -> DRIZZLE
            amountMm < WeatherStateEngine.RAIN_LIGHT_MAX_MM -> LIGHT
            amountMm < WeatherStateEngine.RAIN_MODERATE_MAX_MM -> MODERATE
            else -> HEAVY
        }
    }
}

/** One hour's rain bar: its box on the plate and its tone. */
data class PlateBar(
    val at: Instant,
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val tone: RainTone
)

/** One hour label: centred on [centerX] in a [PlateMetrics.TICK_WIDTH] box. [isNow] prints
 * «ora» instead of the hour. */
data class PlateTick(val at: Instant, val centerX: Float, val isNow: Boolean)

/** Everything the painter needs to draw one plate, in the plate's own dp. */
data class PlateDrawing(
    val width: Float,
    val window: PictureWindow,
    /** dp per degree Celsius: [PlateMetrics.DEGREE], or less on a day whose excursion does
     * not fit between [PlateMetrics.CURVE_TOP] and [PlateMetrics.CURVE_FLOOR]. */
    val degree: Float,
    val runs: List<PlateRun>,
    /** Where «adesso» falls on the axis. In [PictureSpan.DAY] the curve before it is the
     * morning already spent, drawn lighter. */
    val nowX: Float,
    /** The disc's centre on the curve; null when no run passes over [nowX]. */
    val now: PlatePoint?,
    val high: PlateExtreme?,
    /** Null when it would be the same point as [high]: one label on a point, not two. */
    val low: PlateExtreme?,
    val bars: List<PlateBar>,
    val ticks: List<PlateTick>
) {
    /** The axis's x for [at]: what the curve, the bars, the ribbon and the ticks share. */
    fun xAt(at: Instant): Float = plateX(window, width, at)

    /** The ribbon's box: the axis, at [PlateMetrics.RIBBON_TOP]. */
    val ribbonLeft: Float get() = PlateMetrics.INSET
    val ribbonRight: Float get() = width - PlateMetrics.INSET
}

/**
 * The plate for [window] at [width] dp, from [hours] (any order, any span: what falls outside
 * the window is not drawn), with a tick every [tickEveryHours] — three on the 4×2, six on the
 * 3×2.
 */
fun plateDrawing(
    window: PictureWindow,
    hours: List<PictureHour>,
    width: Float,
    tickEveryHours: Int
): PlateDrawing {
    require(tickEveryHours > 0)
    val inside = hours.filter { it.at in window }.sortedBy { it.at }
    val known = inside.filter { it.temperatureC != null }
    val maxC = known.maxOfOrNull { it.temperatureC!! }
    val minC = known.minOfOrNull { it.temperatureC!! }
    val degree = plateDegree(maxC, minC)
    fun y(c: Double): Float = PlateMetrics.CURVE_TOP + ((maxC!! - c) * degree).toFloat()

    val runs = mutableListOf<MutableList<PlatePoint>>()
    var last: Instant? = null
    inside.forEach { hour ->
        val c = hour.temperatureC
        if (c == null) {
            last = null
            return@forEach
        }
        val point = PlatePoint(plateX(window, width, hour.at), y(c))
        val previous = last
        if (previous == null || Duration.between(previous, hour.at) > RUN_GAP) {
            runs += mutableListOf(point)
        } else {
            runs.last() += point
        }
        last = hour.at
    }
    val plateRuns = runs.map { PlateRun(it) }

    val nowX = plateX(window, width, window.now)
        .coerceIn(PlateMetrics.INSET, width - PlateMetrics.INSET)
    val nowY = plateRuns.firstNotNullOfOrNull { it.yAt(nowX) }

    // The canvas looks for the high and the low from its second hour on when the drawing
    // starts now (`hi = 1, lo = 1`): the disc of «adesso» stands on the first, and a label
    // there would sit on it. «La giornata» labels the day's own (`hi = 0`).
    val candidates = known.filterNot {
        window.span == PictureSpan.NEXT_24_HOURS && it.at == window.start && known.size > 1
    }
    val highHour = candidates.firstOrNull()?.let { first ->
        candidates.fold(first) { best, h -> if (h.temperatureC!! > best.temperatureC!!) h else best }
    }
    val lowHour = candidates.firstOrNull()?.let { first ->
        candidates.fold(first) { best, h -> if (h.temperatureC!! < best.temperatureC!!) h else best }
    }
    fun extreme(hour: PictureHour, high: Boolean): PlateExtreme {
        val point = PlatePoint(plateX(window, width, hour.at), y(hour.temperatureC!!))
        return PlateExtreme(
            temperatureC = hour.temperatureC,
            point = point,
            // Kept inside the plate: the canvas never meets its edge, a high at the last hour
            // would put half its label over the card's corner.
            labelLeft = (point.x - PlateMetrics.LABEL_WIDTH / 2f)
                .coerceIn(0f, (width - PlateMetrics.LABEL_WIDTH).coerceAtLeast(0f)),
            labelTop = if (high) {
                point.y - PlateMetrics.HIGH_LABEL_RISE
            } else {
                point.y + PlateMetrics.LOW_LABEL_DROP
            }
        )
    }

    val hourWidth = ((width - 2f * PlateMetrics.INSET) / window.hours).toFloat()
    val barWidth = maxOf(PlateMetrics.BAR_MIN, hourWidth * PlateMetrics.BAR_SHARE)
    val bars = inside.mapNotNull { hour ->
        val chance = hour.chancePct ?: return@mapNotNull null
        if (chance < WeatherStateEngine.PROBABLE_PCT) return@mapNotNull null
        val height = chance.coerceIn(0, 100) / 100f * PlateMetrics.RAIN_HEIGHT
        PlateBar(
            at = hour.at,
            left = plateX(window, width, hour.at) - barWidth / 2f,
            top = PlateMetrics.RAIN_BASELINE - height,
            width = barWidth,
            height = height,
            tone = RainTone.of(hour.amountMm)
        )
    }

    val ticks = generateSequence(0L) { it + tickEveryHours }
        .takeWhile { it < window.hours }
        .map { k ->
            val at = window.start.plus(Duration.ofHours(k))
            PlateTick(at, plateX(window, width, at), isNow = at == window.nowHour)
        }
        .toList()

    return PlateDrawing(
        width = width,
        window = window,
        degree = degree,
        runs = plateRuns,
        nowX = nowX,
        now = nowY?.let { PlatePoint(nowX, it) },
        high = highHour?.let { extreme(it, high = true) },
        low = lowHour?.takeIf { it.at != highHour?.at }?.let { extreme(it, high = false) },
        bars = bars,
        ticks = ticks
    )
}

/** [PlateMetrics.DEGREE], unless the excursion does not fit in the curve's band: then the
 * scale that just fits it. */
fun plateDegree(maxC: Double?, minC: Double?): Float {
    if (maxC == null || minC == null) return PlateMetrics.DEGREE
    val band = PlateMetrics.CURVE_FLOOR - PlateMetrics.CURVE_TOP
    val range = (maxC - minC).toFloat()
    return if (range * PlateMetrics.DEGREE <= band) PlateMetrics.DEGREE else band / range
}

/** The widest excursion the plate draws at [PlateMetrics.DEGREE]: 11.2°. */
val PLATE_RANGE_AT_FULL_SCALE_C: Float =
    (PlateMetrics.CURVE_FLOOR - PlateMetrics.CURVE_TOP) / PlateMetrics.DEGREE

private fun plateX(window: PictureWindow, width: Float, at: Instant): Float =
    PlateMetrics.INSET + (window.fraction(at) * (width - 2f * PlateMetrics.INSET)).toFloat()

/** Two hours further apart than this are not one stretch of curve. */
private val RUN_GAP: Duration = Duration.ofMinutes(61)
