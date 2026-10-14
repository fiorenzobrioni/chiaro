package com.callbackdev.chiaro.ui.sky.compass

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.ui.theme.reducedMotion
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * «Dove guardare» (14 ott 2026): the bearing a sky row prints in words — «verso ovest» —
 * turned into a needle. The dial turns with the phone so its north is the world's; the
 * event's own mark stands on the rim where it will happen; a fixed pointer at the top is
 * where the phone points; and one line says what to do, «Gira a destra» until it says
 * «Ci sei». A light confirmation under the finger the moment it does.
 *
 * Two honest admissions sit beside the needle rather than behind it. When the sensor
 * reports itself badly calibrated, a card shows the figure-eight gesture that fixes it,
 * drawn moving; and because phones report that flag unevenly (some never do), the same
 * gesture is one tap away under the dial for a reader whose needle simply looks wrong.
 * When the field is too weak or too strong to be the earth's, the card says something
 * nearby is pulling the needle, which no gesture fixes.
 *
 * No degrees anywhere: the app prints «verso ovest-nordovest», never «verso 292°», and the
 * dial is the picture of the number.
 */
@Composable
internal fun CompassView(
    name: String,
    whenLine: String,
    /** The direction in words, «ovest-nordovest». */
    direction: String,
    bearingDeg: Double,
    icon: ImageVector,
    coordinates: Coordinates?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val compass by rememberCompass(coordinates)
    val target = bearingDeg.toFloat()
    val heading = compass.heading
    var guidance by remember { mutableStateOf<Guidance?>(null) }
    val next = heading?.let { CompassMath.guidance(guidance, CompassMath.turn(it, target)) }
    val haptics = LocalHapticFeedback.current
    SideEffect {
        if (next != guidance) {
            if (next == Guidance.AHEAD) haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            guidance = next
        }
    }
    var helpOpen by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
            Column(modifier = Modifier.padding(start = 4.dp)) {
                Text(name, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = listOf(whenLine, stringResource(R.string.sky_bearing_towards, direction))
                        .filter { it.isNotEmpty() }
                        .joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (!compass.supported) {
            Text(
                text = stringResource(R.string.compass_unsupported, direction),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            return@Column
        }

        AnimatedVisibility(visible = compass.needsCalibration) {
            CalibrationCard(title = stringResource(R.string.compass_calibrate_title), body = stringResource(R.string.compass_calibrate_body))
        }
        AnimatedVisibility(visible = compass.disturbed) {
            NoticeCard(
                text = stringResource(R.string.compass_disturbed),
                container = MaterialTheme.colorScheme.tertiaryContainer,
                ink = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .widthIn(max = DialMax)
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                // Dimmed while the sensor says it cannot be trusted: the needle still moves,
                // because the gesture is easier to do watching it settle, but it does not
                // claim the confidence of a calibrated one.
                val doubtful = compass.needsCalibration || compass.disturbed
                CompassDial(
                    heading = heading,
                    target = target,
                    aligned = next == Guidance.AHEAD,
                    modifier = Modifier.graphicsLayer { alpha = if (doubtful) DoubtfulAlpha else 1f }
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null, // the title above says what it is
                    tint = Color.Unspecified,
                    modifier = Modifier.size(CentreIcon)
                )
            }
        }

        Text(
            text = stringResource(
                when (next) {
                    null -> R.string.compass_waiting
                    Guidance.AHEAD -> R.string.compass_ahead
                    Guidance.LEFT -> R.string.compass_turn_left
                    Guidance.RIGHT -> R.string.compass_turn_right
                    Guidance.BEHIND -> R.string.compass_behind
                }
            ),
            style = MaterialTheme.typography.headlineSmall,
            color = if (next == Guidance.AHEAD) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                // Said when it changes, which is when the reader needs it: the word moves
                // four times in a turn, the needle fifty times a second.
                .semantics { liveRegion = LiveRegionMode.Polite }
        )
        Text(
            text = stringResource(R.string.compass_hold),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        // The gesture on request: the flag above is only as good as the phone reporting it.
        if (!compass.needsCalibration) {
            TextButton(
                onClick = { helpOpen = !helpOpen },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(stringResource(R.string.compass_calibrate_help))
            }
            AnimatedVisibility(visible = helpOpen) {
                CalibrationCard(title = null, body = stringResource(R.string.compass_calibrate_help_body))
            }
        }
        Box(Modifier.padding(bottom = 16.dp))
    }
}

/**
 * The dial. Everything on it turns with the phone except the pointer at the top, which
 * is the phone. Before the first reading only the ring and the pointer are drawn: a dial
 * with letters on it before the sensor has spoken would be printing a north it does not
 * know.
 */
@Composable
private fun CompassDial(heading: Float?, target: Float, aligned: Boolean, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val letterStyle = MaterialTheme.typography.titleMedium
    val letters = listOf(
        stringResource(R.string.compass_letter_n),
        stringResource(R.string.compass_letter_e),
        stringResource(R.string.compass_letter_s),
        stringResource(R.string.compass_letter_w)
    )
    val ring = MaterialTheme.colorScheme.outlineVariant
    val ink = MaterialTheme.colorScheme.onSurface
    val quiet = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clearAndSetSemantics { }
    ) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension / 2f - PointerRoom.toPx()
        fun at(deg: Float, radius: Float): Offset {
            val rad = Math.toRadians(deg.toDouble())
            return Offset(c.x + radius * sin(rad).toFloat(), c.y - radius * cos(rad).toFloat())
        }
        drawCircle(ring, radius = r, center = c, style = Stroke(width = 2.dp.toPx()))

        if (heading != null) {
            for (a in 0 until 360 step 15) {
                val major = a % 90 == 0
                val half = a % 45 == 0
                val length = when {
                    major -> 12.dp
                    half -> 8.dp
                    else -> 5.dp
                }.toPx()
                val angle = a - heading
                drawLine(
                    color = if (major) ink else quiet,
                    start = at(angle, r),
                    end = at(angle, r - length),
                    strokeWidth = (if (major) 2.5.dp else 1.5.dp).toPx(),
                    cap = StrokeCap.Round
                )
            }
            letters.forEachIndexed { i, letter ->
                val text = measurer.measure(
                    letter,
                    letterStyle.copy(color = if (i == 0) accent else quiet)
                )
                val p = at(i * 90f - heading, r - LetterInset.toPx())
                drawText(text, topLeft = Offset(p.x - text.size.width / 2f, p.y - text.size.height / 2f))
            }
            // Where the event will be: a line from the centre out to its mark on the rim.
            val t = target - heading
            val markRadius = (if (aligned) 13.dp else 10.dp).toPx()
            drawLine(
                color = accent,
                start = at(t, CentreIcon.toPx() / 2f + 6.dp.toPx()),
                end = at(t, r - markRadius),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawCircle(accent, radius = markRadius, center = at(t, r))
        }
        // The phone: a fixed pointer over the top of the ring.
        val tip = Offset(c.x, c.y - r + 2.dp.toPx())
        val w = PointerWidth.toPx()
        val pointer = Path().apply {
            moveTo(tip.x, tip.y)
            lineTo(tip.x - w / 2f, tip.y - PointerHeight.toPx())
            lineTo(tip.x + w / 2f, tip.y - PointerHeight.toPx())
            close()
        }
        drawPath(pointer, if (aligned) accent else ink)
    }
}

/** The calibration card: the gesture drawn, then what to do in words. */
@Composable
private fun CalibrationCard(title: String?, body: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            FigureEight(Modifier.size(width = 96.dp, height = 64.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                title?.let { Text(it, style = MaterialTheme.typography.titleSmall) }
                Text(body, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun NoticeCard(text: String, container: Color, ink: Color) {
    Surface(
        color = container,
        contentColor = ink,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
    }
}

/**
 * The figure-eight, with a small phone travelling it and tilting as it goes, which is the
 * wrist turn that makes the gesture work. Still when the reader has asked for less
 * motion: the path and the phone at one point of it.
 */
@Composable
private fun FigureEight(modifier: Modifier) {
    val reduced = reducedMotion()
    val phase = if (reduced) {
        StillPhase
    } else {
        val transition = rememberInfiniteTransition(label = "figure-eight")
        val t by transition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * PI).toFloat(),
            animationSpec = infiniteRepeatable(tween(FigureEightMs, easing = LinearEasing), RepeatMode.Restart),
            label = "figure-eight-t"
        )
        t
    }
    val path = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.45f)
    val phone = MaterialTheme.colorScheme.onSecondaryContainer
    Canvas(modifier = modifier.clearAndSetSemantics { }) {
        val a = size.width / 2f - 8.dp.toPx()
        val c = Offset(size.width / 2f, size.height / 2f)
        fun point(t: Float): Offset {
            val s = sin(t)
            val d = 1f + s * s
            return Offset(c.x + a * cos(t) / d, c.y + a * s * cos(t) / d * 1.4f)
        }
        val curve = Path().apply {
            val steps = 64
            for (i in 0..steps) {
                val p = point((2 * PI * i / steps).toFloat())
                if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
            }
            close()
        }
        drawPath(curve, path, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        val p = point(phase)
        val ahead = point(phase + 0.05f)
        val tangent = Math.toDegrees(atan2((ahead.y - p.y).toDouble(), (ahead.x - p.x).toDouble())).toFloat()
        val w = 14.dp.toPx()
        val h = 24.dp.toPx()
        rotate(degrees = tangent * 0.5f, pivot = p) {
            drawRoundRect(
                color = phone,
                topLeft = Offset(p.x - w / 2f, p.y - h / 2f),
                size = Size(w, h),
                cornerRadius = CornerRadius(3.dp.toPx())
            )
        }
    }
}

private val DialMax = 300.dp
private val CentreIcon = 64.dp
private val PointerRoom = 14.dp
private val PointerWidth = 14.dp
private val PointerHeight = 12.dp
private val LetterInset = 30.dp
private const val FigureEightMs = 2600
private const val StillPhase = 0.6f
private const val DoubtfulAlpha = 0.45f
