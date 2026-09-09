package com.callbackdev.chiaro.ui.warnings

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.warnings.PlaceWarnings
import com.callbackdev.chiaro.domain.warnings.WarningHazard
import com.callbackdev.chiaro.domain.warnings.WarningLevel
import com.callbackdev.chiaro.ui.warnings.WarningText.colors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * DESIGN.md §8.13. The arithmetic behind the banner: the zone, a grid of every hazard
 * against every day the bulletin still covers, what the level means in the issuer's own
 * words, the bulletin's note when it concerns this zone, and the attribution the licence
 * requires — with the way back to the bulletin itself.
 *
 * The grid never has an empty cell and never a colour alone: a hazard nobody graded says
 * "nessuna" in the `unknown` grey, which is the same rule the verdict chip follows and
 * the reason §2.3's deuteranopia table is not a problem here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarningSheet(
    warnings: PlaceWarnings,
    today: LocalDate,
    timeFmt: DateTimeFormatter,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val locale = Locale.getDefault()
    val dateFmt = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
        ) {
            Text(
                text = stringResource(
                    R.string.warning_sheet_title,
                    WarningText.zoneLabel(context, warnings.zone)
                ),
                style = MaterialTheme.typography.titleLarge
            )
            // The region under the title, unless the title already had to name it: an
            // unnamed zone reads "una zona della regione Marche" and repeating it would
            // be the screen saying one thing twice.
            if (warnings.zone.named) {
                Text(
                    text = warnings.zone.region,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LevelGrid(warnings, today)

            Section(
                title = stringResource(R.string.warning_sheet_meaning_title),
                body = stringResource(WarningText.meaningRes(warnings.maxLevel))
            )
            // Quoted, never translated (VISION §8): it is the issuer speaking, and the
            // label is what says so.
            warnings.note?.let {
                Section(title = stringResource(R.string.warning_sheet_note_title), body = it)
            }

            HorizontalDivider()
            Text(
                text = stringResource(
                    R.string.warning_sheet_source,
                    warnings.issuedAt.toLocalDate().format(dateFmt),
                    warnings.issuedAt.toLocalTime().format(timeFmt)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(
                onClick = {
                    // A phone with no browser answers with nothing happening, which is
                    // better than a crash for a link that states its destination anyway.
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, BulletinUrl.toUri()))
                    }
                },
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(stringResource(R.string.warning_sheet_open))
            }
        }
    }
}

/** The Dipartimento's own page for the criticality bulletin, checked 9 set 2026. */
private const val BulletinUrl =
    "https://mappe.protezionecivile.gov.it/it/mappe-rischi/bollettino-di-criticita/"

/**
 * One row per hazard, one column per day, the WORD of the level in its own container.
 * Two days at most in practice (today and tomorrow), so a fixed-width column beats a
 * table that reflows: the reader compares down a column, not across a wrap.
 */
@Composable
private fun LevelGrid(warnings: PlaceWarnings, today: LocalDate) {
    val locale = Locale.getDefault()
    val dayFmt = DateTimeFormatter.ofPattern("EEE d", locale)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HeaderCell("", Modifier.width(HazardColumn))
            warnings.days.forEach { day ->
                HeaderCell(
                    text = when (day.date) {
                        today -> stringResource(R.string.week_today)
                        today.plusDays(1) -> stringResource(R.string.warning_sheet_tomorrow)
                        else -> day.date.format(dayFmt)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        WarningHazard.displayOrder.forEach { hazard ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(WarningText.hazardRes(hazard))
                        .replaceFirstChar { it.titlecase(locale) },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(HazardColumn)
                )
                warnings.days.forEach { day ->
                    LevelCell(day.levels.getValue(hazard), Modifier.weight(1f))
                }
            }
        }
    }
}

private val HazardColumn = 128.dp

@Composable
private fun HeaderCell(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier
    )
}

@Composable
private fun LevelCell(level: WarningLevel, modifier: Modifier = Modifier) {
    val palette = level.colors
    Text(
        text = stringResource(WarningText.levelWordRes(level)),
        style = MaterialTheme.typography.labelLarge,
        color = palette.ink,
        textAlign = TextAlign.Center,
        modifier = modifier
            .background(palette.container, RoundedCornerShape(8.dp))
            .padding(vertical = 6.dp, horizontal = 4.dp)
    )
}

@Composable
private fun Section(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(text = body, style = MaterialTheme.typography.bodyMedium)
    }
}
