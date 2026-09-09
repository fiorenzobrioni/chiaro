package com.callbackdev.chiaro.ui.warnings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.warnings.PlaceWarnings
import com.callbackdev.chiaro.domain.warnings.WarningLevel
import com.callbackdev.chiaro.ui.icons.ChiaroIcons
import com.callbackdev.chiaro.ui.warnings.WarningText.colors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * DESIGN.md §8.13. The official warning as one surface on Today, between the freshness
 * chip and the guide card: the chip qualifies the hero and talks about the DATA, this
 * talks about the WORLD, so it opens the content rather than annotating the sky.
 *
 * Two lines and nothing else. The first is what and how bad — "Allerta arancione per
 * temporali", with every other graded hazard after it — the second is when and who said
 * so. It is one TalkBack target with one sentence, because "warning triangle, Allerta
 * arancione per temporali, Oggi fino a mezzanotte, Protezione Civile" read out as four
 * things is not what the banner says.
 *
 * It is never drawn green: an absence is not drawn on this screen (§1.1), and Avvisi is
 * where somebody goes to ask whether there is one.
 */
@Composable
fun WarningBanner(
    warnings: PlaceWarnings,
    today: LocalDate,
    timeFmt: DateTimeFormatter,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val palette = warnings.maxLevel.colors
    val headline = WarningText.sentence(context, warnings)
    val detail = stringResource(
        R.string.warning_banner_detail,
        WarningText.days(context, warnings.peakDays.map { it.date }, today)
            .replaceFirstChar { it.titlecase(java.util.Locale.getDefault()) },
        stringResource(
            R.string.warning_banner_source,
            warnings.issuedAt.toLocalTime().format(timeFmt)
        )
    )
    Surface(
        color = palette.container,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .clearAndSetSemantics { contentDescription = "$headline. $detail" }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .defaultMinSize(minHeight = 56.dp)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Icon(
                painter = painterResource(ChiaroIcons.warningMarkRes()),
                contentDescription = null, // the surface's own semantics say it
                tint = palette.ink,
                modifier = Modifier.size(24.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(headline, style = MaterialTheme.typography.titleSmall, color = palette.ink)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = palette.ink)
            }
        }
    }
}

/**
 * The same statement where a banner does not fit: `ic_warning` at 12dp in the level's
 * ink, the level's word at 11sp, on the level's container — the `VerdictChip` grammar
 * at the size a card can afford (DESIGN §8.13). Avvisi's card leads with it, and the
 * widgets take it in the phase's fourth step.
 */
@Composable
fun WarningLevelChip(level: WarningLevel, modifier: Modifier = Modifier) {
    val palette = level.colors
    val word = stringResource(WarningText.phraseRes(level))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .background(palette.container, RoundedCornerShape(10.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
            .clearAndSetSemantics { contentDescription = word }
    ) {
        Icon(
            painter = painterResource(ChiaroIcons.warningMarkRes()),
            contentDescription = null,
            tint = palette.ink,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = word,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = palette.ink
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WarningLevelChipPreview() {
    com.callbackdev.chiaro.ui.theme.ChiaroTheme(dynamicColor = false) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            WarningLevelChip(WarningLevel.YELLOW)
            WarningLevelChip(WarningLevel.ORANGE)
            WarningLevelChip(WarningLevel.RED)
        }
    }
}
