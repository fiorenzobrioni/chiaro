package com.callbackdev.chiaro.ui.components

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.hypot

/**
 * A meteor's trail at one instant, in the canvas' px ([MeteorTrail.at]): the shader draws a
 * stroke from [tail] to [head], strongest at the head and thinner toward the tail, and a glow
 * round the head.
 */
internal data class MeteorStroke(
    val tail: Offset,
    val head: Offset,
    /** How strong the stroke is at its head, 0..1, before the scene's [SkyScene.meteors]. */
    val alpha: Float,
    /** Half the stroke's width at the head, px. */
    val halfWidth: Float,
    /** The head's glow: how far it reaches, px, and how strong it is, 0..1. */
    val glowRadius: Float,
    val glow: Float
)

/**
 * One meteor's whole flight (pure, `MeteorTrailTest`): where it starts and stops, in px, how
 * long it takes, and what kind of meteor it is.
 */
internal data class MeteorFlight(
    val start: Offset,
    val stop: Offset,
    /** When it lights up, seconds into its period. */
    val at: Float,
    /** Seconds from the start to the stop. */
    val duration: Float,
    /** How bright it is, 0..1: most are middling, one in a few is bright. */
    val bright: Float
) {
    val length: Float get() = (stop - start).getDistance()
}

/**
 * The meteor of a shower's peak night, placed and timed (17 nov 2026, committente: «on the
 * Sky's hero I can hardly see them: very fast, and only at the very top»). Until then the
 * shader placed it itself, from a start in the canvas' top 8–43% and running away from the
 * radiant, 0.8 seconds long, one and a half dp wide. On a peak night the radiant is low, or
 * under the camera's foot, so every trail ran **up**, toward the canvas' top edge, and the
 * Sky's canvas runs under the status bar and the place row: measured on the shader's own
 * arithmetic, with the window facing a radiant 5–25° up, 93–99% of the trails were under
 * that row at their brightest. What was left showed for half a second, in seven or eight of
 * the fifteen frames a second the night's sky is drawn at.
 *
 * So now:
 *
 * - **The whole trail is in the open sky** ([at]'s `openTop`..`openBottom`): under the place
 *   row and over what stands at the stage's foot, [EdgeMarginDp] from the sides. It still
 *   runs away from the radiant, exactly: the trail is laid on the line from the radiant
 *   through its middle, and only shortened or moved along the sky to fit, never turned.
 * - **Each trail is its own**, as meteors are: where it is, how long (shorter near the
 *   radiant, where a real trail is seen end-on and foreshortened), how fast, how bright. A
 *   bright one is wider and has a larger glow. The rules are one; the trails are not.
 * - **It lasts long enough to be seen**: 0.9 to 1.6 seconds of flight at about [SpeedDp] dp
 *   a second, shorter trails slower (near the radiant a real meteor is slower too), then an
 *   [Afterglow] in which the train shrinks into where the meteor went out. It comes on in a
 *   fifth of its flight, holds, and dims through the last quarter, so it never pops.
 * - **It is drawn smoothly**: the canvas asks for [MeteorFps] frames a second while one is
 *   in flight ([nearFlight]), and goes back to its own rate after.
 *
 * Still about one every [MeteorPeriodSeconds], at a moment of its own in each period: what a
 * reader sees of a strong shower from a town. A drawing of something that happens, not a
 * count: the app prints no rate.
 */
internal object MeteorTrail {

    /**
     * The flight of [cycle]'s meteor, or null where the open sky is too small to hold one.
     * [radiant] is in px, off the canvas too; [seed] sets one night's sequence apart.
     */
    fun flight(
        cycle: Long,
        seed: Int,
        radiant: Offset,
        width: Float,
        openTop: Float,
        openBottom: Float,
        density: Float
    ): MeteorFlight? {
        val margin = EdgeMarginDp * density
        val inset = InsetDp * density
        val xMin = margin
        val xMax = width - margin
        val yMin = openTop + inset
        val yMax = openBottom - inset
        if (xMax - xMin < MinRoomDp * density || yMax - yMin < MinRoomDp * density) return null
        fun h(salt: Int) = hash(cycle, seed, salt)
        // The middle first, anywhere in the open sky but its outer fifths, so a trail is not
        // all pressed against an edge.
        var mid = Offset(xMin + (0.15f + 0.7f * h(1)) * (xMax - xMin), yMin + (0.2f + 0.6f * h(2)) * (yMax - yMin))
        // Foreshortened toward the radiant: a trail near it is seen nearly end-on.
        val near = smoothstep(0f, ForeshortenDp * density, (mid - radiant).getDistance())
        var length = (MinLengthDp + (MaxLengthDp - MinLengthDp) * near) * (0.7f + 0.6f * h(3)) * density
        // Fitted, three times over: shortened to the room the direction leaves, the middle
        // moved to keep both ends in, the direction read again from where the middle now is.
        repeat(3) {
            val dir = away(mid, radiant)
            if (abs(dir.x) * length > xMax - xMin) length = (xMax - xMin) / abs(dir.x)
            if (abs(dir.y) * length > yMax - yMin) length = (yMax - yMin) / abs(dir.y)
            mid = Offset(
                mid.x.coerceIn(xMin + abs(dir.x) * length / 2f, xMax - abs(dir.x) * length / 2f),
                mid.y.coerceIn(yMin + abs(dir.y) * length / 2f, yMax - abs(dir.y) * length / 2f)
            )
        }
        // The last direction is the one drawn, exactly away from the radiant; the length is
        // what it leaves between the middle and the open sky's edges, both ways.
        val dir = away(mid, radiant)
        val half = minOf(
            length / 2f,
            reach(mid.x, dir.x, xMin, xMax),
            reach(mid.y, dir.y, yMin, yMax)
        )
        if (2f * half < MinLengthDrawnDp * density) return null
        val start = mid - dir * half
        val stop = mid + dir * half
        val speed = SpeedDp * density * (0.9f + 0.2f * h(4))
        val duration = (2f * half / speed).coerceIn(MinDuration, MaxDuration)
        val at = Lead + (MeteorPeriodSeconds - 2f * Lead - Life) * h(5)
        // Most are middling and one in a few is bright, as a shower's magnitudes go.
        val u = h(6)
        return MeteorFlight(start, stop, at, duration, u * u)
    }

    /** The stroke at [time], seconds on the canvas' clock, or null between two meteors. */
    fun at(
        time: Float,
        seed: Int,
        radiant: Offset,
        width: Float,
        openTop: Float,
        openBottom: Float,
        density: Float
    ): MeteorStroke? {
        val cycle = floor(time / MeteorPeriodSeconds).toLong()
        val into = time - cycle * MeteorPeriodSeconds
        // Cheap first: the moment is the hash's alone, and most frames are between meteors.
        val earliest = Lead + (MeteorPeriodSeconds - 2f * Lead - Life) * hash(cycle, seed, 5)
        if (into < earliest || into > earliest + Life) return null
        val f = flight(cycle, seed, radiant, width, openTop, openBottom, density) ?: return null
        val t = into - f.at
        if (t < 0f || t >= f.duration + Afterglow) return null
        val core = (0.75f + 0.45f * f.bright) * density
        val glowRadius = (2f + 2.5f * f.bright) * density
        val strength = 0.78f + 0.22f * f.bright
        val glow = 0.22f + 0.33f * f.bright
        val dir = f.stop - f.start
        return if (t < f.duration) {
            val p = t / f.duration
            // On in a fifth of the flight, held, and down to [Ember] through the last quarter.
            val env = smoothstep(0f, 0.2f, p) * (1f - (1f - Ember) * smoothstep(0.75f, 1f, p))
            MeteorStroke(
                tail = f.start + dir * maxOf(0f, p - TailShare),
                head = f.start + dir * p,
                alpha = strength * env,
                halfWidth = core,
                glowRadius = glowRadius,
                glow = glow * env * (1f - smoothstep(0.85f, 1f, p))
            )
        } else {
            // The train: what is left of the trail shrinks into where the meteor went out
            // and fades from [Ember], so it ends on a breath and not on a cut.
            val q = (t - f.duration) / Afterglow
            val left = TailShare * (1f - q * (2f - q))
            MeteorStroke(
                tail = f.stop - dir * left,
                head = f.stop,
                alpha = strength * Ember * (1f - q) * (1f - q),
                halfWidth = core,
                glowRadius = glowRadius,
                glow = 0f
            )
        }
    }

    /**
     * Whether a meteor is in flight at [time] or about to be, within [lookAhead] seconds: the
     * canvas then asks for [MeteorFps] frames a second. The moment alone, no geometry.
     */
    fun nearFlight(time: Float, seed: Int, lookAhead: Float): Boolean {
        val cycle = floor(time / MeteorPeriodSeconds).toLong()
        val into = time - cycle * MeteorPeriodSeconds
        val at = Lead + (MeteorPeriodSeconds - 2f * Lead - Life) * hash(cycle, seed, 5)
        return into >= at - lookAhead && into <= at + Life
    }

    /** The unit vector from the radiant through [p]; straight up where the two meet. */
    private fun away(p: Offset, radiant: Offset): Offset {
        val d = p - radiant
        val l = hypot(d.x, d.y)
        return if (l < 1f) Offset(0f, -1f) else d / l
    }

    /** How far from [c] along [d] (a direction's component) the span [lo]..[hi] reaches. */
    private fun reach(c: Float, d: Float, lo: Float, hi: Float): Float =
        if (abs(d) < 1e-4f) Float.MAX_VALUE else minOf(c - lo, hi - c).coerceAtLeast(0f) / abs(d)

    private fun smoothstep(e0: Float, e1: Float, x: Float): Float {
        val t = ((x - e0) / (e1 - e0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    /** 0..1 from a cycle, a night's seed and a salt (SplitMix64's finaliser). */
    private fun hash(cycle: Long, seed: Int, salt: Int): Float {
        var z = cycle * -0x61c8864680b583ebL + seed.toLong() * 0x632BE59BD9B4E019L + salt.toLong() * 0x2545F4914F6CDD1DL
        z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
        z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
        z = z xor (z ushr 31)
        return (z ushr 40).toFloat() / (1L shl 24).toFloat()
    }

    /** From the sides of the canvas, dp: the trail stays clear of the edges. */
    internal const val EdgeMarginDp = 20f
    /** Inside the open sky's top and bottom, dp: past the reach of a head's glow. */
    internal const val InsetDp = 10f
    /** An open sky narrower or shorter than this, dp, has no meteor. */
    internal const val MinRoomDp = 48f
    /** A trail fitted shorter than this, dp, is not drawn. */
    internal const val MinLengthDrawnDp = 36f
    /** A trail's length near the radiant and far from it, dp, before the fit. */
    internal const val MinLengthDp = 90f
    internal const val MaxLengthDp = 180f
    /** Past this distance from the radiant, dp, a trail is no longer foreshortened. */
    private const val ForeshortenDp = 320f
    /** About how fast the head moves, dp a second. */
    internal const val SpeedDp = 110f
    internal const val MinDuration = 0.9f
    internal const val MaxDuration = 1.6f
    /** The train after the head goes out, seconds. */
    internal const val Afterglow = 0.4f
    /** The whole of a meteor's life, flight and train, at most. */
    internal const val Life = MaxDuration + Afterglow
    /** How much of the path the tail trails behind the head. */
    private const val TailShare = 0.4f
    /** What is left of the trail's light when the head goes out. */
    private const val Ember = 0.35f
    /** Never in a period's first or last seconds. */
    private const val Lead = 2f
}

/** A meteor about every this many seconds on a peak night under a dark, open sky: «one every
 * so often», which is what a reader sees of a strong shower from a town, and no rate printed. */
internal const val MeteorPeriodSeconds = 45f

/** The frames a second the canvas asks for while a meteor is in flight ([MeteorTrail.nearFlight]):
 * at the night's fifteen a head moving 110 dp a second jumps 7 dp a frame, and that reads as a stutter. */
internal const val MeteorFps = 60
