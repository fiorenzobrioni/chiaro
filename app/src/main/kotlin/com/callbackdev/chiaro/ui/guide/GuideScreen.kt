package com.callbackdev.chiaro.ui.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.ui.components.VerdictChip
import com.callbackdev.chiaro.ui.components.VerdictKind
import com.callbackdev.chiaro.ui.icons.ChiaroIcons
import com.callbackdev.chiaro.ui.theme.ChiaroTheme

/**
 * The guide (VISION §5.7): short, illustrated, and re-openable forever from Settings
 * — a definition offered before you have seen the thing it defines does not stick,
 * and a screen shown once cannot be consulted the day the question actually arrives.
 *
 * It answers the questions a new reader brings, never an interface element: an
 * interface element that needs explaining is a bug in this edition. The alerts
 * chapter joins in Fase 6, together with the screen it will be about.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideRoute(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.guide_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        GuideContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        )
    }
}

@Composable
private fun GuideContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Paragraph(stringResource(R.string.guide_intro))

        SectionTitle(ChiaroIcons.cloud, stringResource(R.string.guide_data_title))
        Paragraph(stringResource(R.string.guide_data_p1))
        Paragraph(stringResource(R.string.guide_data_p2))
        Paragraph(stringResource(R.string.guide_data_p3))

        SectionTitle(ChiaroIcons.starryNight, stringResource(R.string.guide_verdicts_title))
        Paragraph(stringResource(R.string.guide_verdicts_p1))
        VerdictSampler()
        Text(
            text = stringResource(R.string.guide_verdicts_caption),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Paragraph(stringResource(R.string.guide_verdicts_p2))

        SectionTitle(ChiaroIcons.precipitation, stringResource(R.string.guide_radar_title))
        Paragraph(stringResource(R.string.guide_radar_p1))
        Paragraph(stringResource(R.string.guide_radar_p2))
    }
}

@Composable
private fun SectionTitle(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(top = 16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null, // decoration: the title right here says it all
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
        Text(text, style = MaterialTheme.typography.titleLarge)
    }
}

/** Guide prose is bodyLarge (DESIGN §5): 16/24, made to be read, not scanned. */
@Composable
private fun Paragraph(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyLarge)
}

/**
 * The four answers, shown as themselves: real [VerdictChip]s, introduced by the
 * sentence above as an example — teaching by showing the actual thing, with the
 * caption saying what the sample numbers stand for.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VerdictSampler() {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        VerdictChip(
            kind = VerdictKind.PASS,
            label = stringResource(R.string.verdict_pass),
            evidence = stringResource(R.string.guide_verdict_evidence_pass)
        )
        VerdictChip(
            kind = VerdictKind.UNSTABLE,
            label = stringResource(R.string.verdict_unstable),
            evidence = stringResource(R.string.guide_verdict_evidence_unstable)
        )
        VerdictChip(
            kind = VerdictKind.FAIL,
            label = stringResource(R.string.verdict_fail),
            evidence = stringResource(R.string.guide_verdict_evidence_fail)
        )
        // The one chip with no number, exactly as in the app: not knowing has no
        // arithmetic to show.
        VerdictChip(
            kind = VerdictKind.UNKNOWN,
            label = stringResource(R.string.verdict_unknown),
            evidence = null
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GuidePreview() {
    ChiaroTheme(dynamicColor = false) {
        GuideRoute(onBack = {})
    }
}
