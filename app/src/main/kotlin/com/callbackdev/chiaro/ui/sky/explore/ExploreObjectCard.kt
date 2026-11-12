package com.callbackdev.chiaro.ui.sky.explore

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.model.MoonPhase
import com.callbackdev.chiaro.domain.sky.AstronomyEngine
import com.callbackdev.chiaro.domain.sky.Planet
import com.callbackdev.chiaro.domain.sky.SkyFigureKind
import com.callbackdev.chiaro.domain.sky.SkyFigures
import com.callbackdev.chiaro.domain.sky.SkyHeight
import com.callbackdev.chiaro.domain.sky.SkyJobCatalog
import com.callbackdev.chiaro.domain.sky.SkyPassagePoint
import com.callbackdev.chiaro.domain.sky.SkyVerdict
import com.callbackdev.chiaro.domain.sky.SkyVerdictNote
import com.callbackdev.chiaro.ui.components.VerdictChip
import com.callbackdev.chiaro.ui.format.currentLocale
import com.callbackdev.chiaro.ui.sky.ChipFill
import com.callbackdev.chiaro.ui.sky.ConstellationNames
import com.callbackdev.chiaro.ui.sky.SkyText
import com.callbackdev.chiaro.ui.theme.SkyPalette
import com.callbackdev.chiaro.ui.theme.tabular
import com.callbackdev.chiaro.widget.window.NamedStars
import com.callbackdev.chiaro.widget.window.WindowText
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * The sheet about a thing a reader pointed at (PLANNING, Fase 16, step 16.8, board 4), at
 * the page's foot in the place of the time panel: while it is open the sky's hour stands
 * still, and the sheet says which hour that is. Top to bottom: the name, what it is and
 * where it stands, in words; the hour's sky as a verdict with its number; when it rises,
 * tops and sets round this night, each with the direction; until when it is worth looking,
 * and what the eye makes of it; the bell and the guide's page.
 *
 * **Nothing is drawn for what is not there.** No verdict on an hour the forecast does not
 * have; no «rises» for a thing that never sets from here; no bell where the catalog has no
 * line about the thing (the moon, a star, a constellation), and no page where the guide
 * has none (a star, a constellation). A star's sheet is short, and that is the true size
 * of what the app knows about it.
 */
@Composable
internal fun ObjectCard(
    facts: ObjectFacts,
    dome: SkyDome,
    /** The hour's sky, null where the forecast has no such hour. */
    verdict: SkyVerdict?,
    /** The hour the sky is of, as the page prints it. */
    clockText: String,
    zone: ZoneId,
    timeFmt: DateTimeFormatter,
    /** A reminder for this thing's line is set and can ring. */
    reminded: Boolean,
    notificationsAllowed: Boolean,
    onRemind: (jobId: String, on: Boolean) -> Unit,
    onOpenPage: (jobId: String) -> Unit,
    onClose: () -> Unit,
    docked: Boolean,
    maxHeight: Dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val locale = currentLocale()
    val pick = facts.pick
    val title = remember(pick, locale) { ObjectWords.title(context, pick) }
    val kind = remember(pick, facts.place, dome, locale) { ObjectWords.kindLine(context, facts, dome) }
    val shape = if (docked) RoundedCornerShape(topStart = CardCorner, topEnd = CardCorner) else RoundedCornerShape(CardCorner)
    Column(
        modifier = modifier
            .clip(shape)
            .background(SkyPalette.ScrimColor.copy(alpha = CardScrim))
            .then(if (docked) Modifier.navigationBarsPadding() else Modifier)
            .heightIn(max = maxHeight)
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 8.dp)
            .testTag(ExploreObjectTag)
    ) {
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f).padding(top = 12.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = SkyPalette.Ink,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = kind,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SkyPalette.Ink.copy(alpha = QuietInk)
                )
            }
            IconButton(
                onClick = onClose,
                colors = IconButtonDefaults.iconButtonColors(contentColor = SkyPalette.Ink)
            ) {
                Icon(imageVector = Icons.Outlined.Close, contentDescription = stringResource(R.string.explore_object_close))
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(top = 10.dp, end = 12.dp)
        ) {
            if (verdict != null) {
                val number = if (verdict.note == SkyVerdictNote.PRECIPITATION && verdict.precipPct != null) {
                    stringResource(R.string.sky_evidence_rain, verdict.precipPct!!)
                } else {
                    stringResource(R.string.sky_evidence_cloud, verdict.cloudPct ?: 0)
                }
                VerdictChip(
                    kind = SkyText.chipKind(verdict.kind),
                    label = stringResource(SkyText.verdictWordRes(verdict.kind)),
                    evidence = stringResource(R.string.explore_object_evidence, number, clockText)
                )
            }
            val passage = facts.passage
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                passage.rise?.let {
                    PassageBox(R.string.explore_box_rise, it, zone, timeFmt, withHeight = false, modifier = Modifier.weight(1f))
                }
                PassageBox(R.string.explore_box_highest, passage.highest, zone, timeFmt, withHeight = true, modifier = Modifier.weight(1f))
                passage.set?.let {
                    PassageBox(R.string.explore_box_set, it, zone, timeFmt, withHeight = false, modifier = Modifier.weight(1f))
                }
            }
            val until = facts.worthUntil
            val worth = when {
                passage.alwaysUp && !facts.lowNow -> stringResource(R.string.explore_object_always_up)
                facts.lowNow -> stringResource(R.string.explore_object_low_now)
                until != null && until.isAfter(dome.at) && facts.untilDawn -> stringResource(R.string.explore_object_until_dawn)
                // The core's stretch can end because the moon comes up, with the core still
                // high: the hour is said without a reason the sheet does not know.
                until != null && until.isAfter(dome.at) && pick == SkyPick.MilkyWay ->
                    stringResource(R.string.explore_object_until_plain, until.atZone(zone).format(timeFmt))
                until != null && until.isAfter(dome.at) ->
                    stringResource(R.string.explore_object_until, until.atZone(zone).format(timeFmt))
                else -> null
            }
            val eye = ObjectWords.eyeRes(pick)?.let { stringResource(it) }
            listOfNotNull(worth, eye).forEach { sentence ->
                Text(text = sentence, style = MaterialTheme.typography.bodyMedium, color = SkyPalette.Ink)
            }
            val bell = facts.bellJobId
            if (bell != null) {
                val peak = pick is SkyPick.Radiant
                val label = stringResource(
                    when {
                        reminded && peak -> R.string.explore_reminded_peak
                        reminded -> R.string.explore_reminded
                        peak -> R.string.explore_remind_peak
                        else -> R.string.explore_remind
                    }
                )
                val state = stringResource(if (reminded) R.string.explore_remind_state_on else R.string.explore_remind_state_off)
                FilledTonalButton(
                    onClick = { onRemind(bell, !reminded) },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = SkyPalette.Ink.copy(alpha = if (reminded) BellOnFill else ChipFill),
                        contentColor = SkyPalette.Ink
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(ExploreBellTag)
                        .semantics { stateDescription = state }
                ) {
                    Icon(
                        imageVector = if (reminded) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                        contentDescription = null, // the words beside it say it
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Text(text = label, modifier = Modifier.padding(start = ButtonDefaults.IconSpacing))
                }
                // A bell that cannot ring says so, where it was set (15.2's rule).
                if (reminded && !notificationsAllowed) {
                    Text(
                        text = stringResource(R.string.notifications_off_sky),
                        style = MaterialTheme.typography.bodySmall,
                        color = SkyPalette.Ink.copy(alpha = QuietInk)
                    )
                }
            }
            val page = facts.pageJobId
            if (page != null) {
                TextButton(
                    onClick = { onOpenPage(page) },
                    colors = ButtonDefaults.textButtonColors(contentColor = SkyPalette.Ink),
                    modifier = Modifier.testTag(ExplorePageTag)
                ) {
                    Text(text = stringResource(R.string.explore_object_page))
                }
            }
        }
    }
}

/** One of the three boxes: what happens, when, and which way to face for it. */
@Composable
private fun PassageBox(
    labelRes: Int,
    point: SkyPassagePoint,
    zone: ZoneId,
    timeFmt: DateTimeFormatter,
    /** The highest point says how high as well: «south, high up». */
    withHeight: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val locale = currentLocale()
    val label = stringResource(labelRes)
    val time = point.at.atZone(zone).format(timeFmt)
    val where = remember(point, withHeight, locale) {
        if (withHeight) {
            ObjectWords.place(context, point.azimuthDeg, point.altitudeDeg)
        } else {
            ObjectWords.bearing(context, point.azimuthDeg)
        }
    }
    val spoken = stringResource(R.string.explore_box_spoken, label, time, where)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SkyPalette.Ink.copy(alpha = BoxFill))
            .padding(horizontal = 10.dp, vertical = 8.dp)
            // One announcement: «Rises at 15:34, in the east».
            .clearAndSetSemantics { contentDescription = spoken }
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = SkyPalette.Ink.copy(alpha = QuietInk))
        Text(text = time, style = MaterialTheme.typography.titleMedium.tabular(), color = SkyPalette.Ink)
        Text(text = where, style = MaterialTheme.typography.bodySmall, color = SkyPalette.Ink)
    }
}

/**
 * The sheet's words. From a [Context] and not composable: the sky's canvas names the same
 * things for a screen reader's actions, outside any sheet.
 */
internal object ObjectWords {

    /** The thing's name, as the sky writes it; a constellation's in ordinary letters. */
    fun title(context: Context, pick: SkyPick): String = when (pick) {
        SkyPick.Moon -> context.getString(R.string.explore_object_moon)
        is SkyPick.Wanderer -> WindowText.planetLabel(context, pick.planet)
        is SkyPick.Star -> NamedStars.getOrNull(pick.index)?.let { context.getString(it.nameRes) }.orEmpty()
        is SkyPick.Figure -> ConstellationUnits.names.getOrNull(pick.nameIndex)
            ?.let { ConstellationNames.nameRes(it.id) }
            ?.let(context::getString)
            .orEmpty()
        is SkyPick.Radiant -> context.getString(SkyText.shortNameRes(pick.jobId))
        is SkyPick.Shape -> SkyFigures.byId(pick.figureId)?.let { SightWords.figureName(context, it) }.orEmpty()
        SkyPick.MilkyWay -> context.getString(SkyText.nameRes(SkyJobCatalog.MilkyWayCore.id))
    }

    /** A screen reader's action on the sky: «Open: Jupiter». */
    fun openLabel(context: Context, pick: SkyPick): String =
        context.getString(R.string.explore_open_object, title(context, pick))

    /** «Planet · in the south, halfway up»: what it is, and where it stands in this sky. */
    fun kindLine(context: Context, facts: ObjectFacts, dome: SkyDome): String {
        val kind = when (facts.pick) {
            SkyPick.Moon -> moonKind(context, MoonPhase.at(dome.at), (dome.moonIllumination * 100).roundToInt())
            is SkyPick.Wanderer -> context.getString(R.string.explore_kind_planet)
            is SkyPick.Star -> context.getString(R.string.explore_kind_star)
            is SkyPick.Figure -> context.getString(R.string.explore_kind_constellation)
            is SkyPick.Radiant -> context.getString(R.string.explore_kind_radiant)
            is SkyPick.Shape -> context.getString(
                when (SkyFigures.byId(facts.pick.figureId)?.kind) {
                    SkyFigureKind.CLUSTER -> R.string.explore_kind_cluster
                    SkyFigureKind.SHAPE -> R.string.explore_kind_shape
                    else -> R.string.explore_kind_constellation
                }
            )
            SkyPick.MilkyWay -> context.getString(R.string.explore_kind_milky_way)
        }
        return context.getString(R.string.explore_kind_line, kind, place(context, facts.place.azimuthDeg, facts.place.altitudeDeg))
    }

    /**
     * «Waxing gibbous, 97% lit»: the moon's phase and how much of it is lit. The phase's
     * name is the day's, and «new moon» owns a day and three quarters either side of the
     * instant: inside it a moon that is up and lit, the hair of a crescent in the dusk or
     * the dawn, is a thing a reader can see, and a sheet that called it new would name the
     * one moon nobody sees. From one part in a hundred lit, the floor the «worth seeing»
     * list has for the moon, it is a thin crescent.
     */
    fun moonKind(context: Context, phase: MoonPhase, litPct: Int): String =
        if (isThinCrescent(phase, litPct)) {
            context.getString(R.string.explore_kind_moon_thin, litPct)
        } else {
            context.getString(R.string.explore_kind_moon, context.getString(SkyText.phaseRes(phase)), litPct)
        }

    /** A moon the phase calls new and the eye can see: lit by one part in a hundred or more. */
    fun isThinCrescent(phase: MoonPhase, litPct: Int): Boolean = phase == MoonPhase.NEW_MOON && litPct >= 1

    /**
     * The phase a row draws the moon as at [at]: its own, and for a thin crescent
     * ([isThinCrescent]) the crescent of its side of the new moon, waxing after it and
     * waning before, where the new moon's own drawing is a dashed circle with nothing lit.
     */
    fun moonDrawn(phase: MoonPhase, litPct: Int, at: Instant): MoonPhase = when {
        !isThinCrescent(phase, litPct) -> phase
        AstronomyEngine.moonIllumination(at).elongation < 180.0 -> MoonPhase.WAXING_CRESCENT
        else -> MoonPhase.WANING_CRESCENT
    }

    /**
     * Where a thing stands, with no degrees in it (PLANNING, Fase 16, «Niente gergo»): the
     * direction to the eighth of the compass and the height as a third of the way up
     * ([SkyHeight]); straight up, the height alone, because up there a direction is a
     * tilt of the head.
     */
    fun place(context: Context, azimuthDeg: Double, altitudeDeg: Double): String {
        val height = SkyHeight.of(altitudeDeg)
        if (height == SkyHeight.OVERHEAD) return context.getString(R.string.explore_height_overhead)
        return context.getString(
            R.string.explore_place,
            context.getString(SkyText.bearingRes(azimuthDeg)),
            context.getString(
                when (height) {
                    SkyHeight.LOW -> R.string.explore_height_low
                    SkyHeight.HALFWAY -> R.string.explore_height_halfway
                    else -> R.string.explore_height_high
                }
            )
        )
    }

    /** Which way to face, alone: «in the east». */
    fun bearing(context: Context, azimuthDeg: Double): String =
        context.getString(R.string.explore_box_bearing, context.getString(SkyText.bearingRes(azimuthDeg)))

    /** What the eye makes of it; null for a star, a constellation and a figure of the list,
     * which have no sentence of their own. */
    fun eyeRes(pick: SkyPick): Int? = when (pick) {
        SkyPick.Moon -> R.string.explore_eye_moon
        is SkyPick.Wanderer -> when (pick.planet) {
            Planet.VENUS -> R.string.explore_eye_venus
            Planet.JUPITER -> R.string.explore_eye_jupiter
            Planet.MARS -> R.string.explore_eye_mars
            Planet.SATURN -> R.string.explore_eye_saturn
        }
        is SkyPick.Radiant -> R.string.explore_eye_radiant
        SkyPick.MilkyWay -> R.string.explore_eye_milky_way
        is SkyPick.Star, is SkyPick.Figure, is SkyPick.Shape -> null
    }
}

/** The sheet, its bell and its link, for a test to find. */
internal const val ExploreObjectTag = "explore-object"
internal const val ExploreBellTag = "explore-bell"
internal const val ExplorePageTag = "explore-page"

private val CardCorner = 20.dp

/** The time panel's own ground: nearly all of the canvas' dark, so no star stands in a word. */
private const val CardScrim = 0.9f
private const val QuietInk = 0.72f
private const val BoxFill = 0.08f
private const val BellOnFill = 0.3f
