package com.callbackdev.chiaro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.ui.icons.ChiaroIcons
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import com.callbackdev.chiaro.ui.theme.VerdictColors
import com.callbackdev.chiaro.ui.theme.forText
import com.callbackdev.chiaro.ui.theme.tabular

/** The four answers the sky can give. The UI's own enum, not the domain's: the domain
 * ships English words (`pass`, `unstable`) because tweather printed them as code, and
 * in Chiaro every one of them is a localized sentence fragment. */
enum class VerdictKind { PASS, UNSTABLE, FAIL, UNKNOWN }

/**
 * DESIGN.md §8.7. A verdict is **a glyph and a word before it is a color**: green, amber
 * and red separate by ΔE 0.7 under deuteranopia (measured, §2.3), so a colored dot would
 * be telling a third of some readers nothing at all.
 *
 * The glyph is a drawing since 9 set 2026 ([ChiaroIcons.verdictMarkRes]), not the `✓ ✗`
 * characters: Roboto has neither, and the phone drew them from a symbol fallback font in
 * a hand of its own. Sized on the label's text so it grows with the reader's font, as
 * the letters beside it do.
 *
 * [evidence] is the number that decided it, and it is not optional by accident: the
 * series' rule is that a verdict always ships with its arithmetic. Pass null only where
 * there is genuinely no number — an `UNKNOWN` for data that never arrived.
 */
@Composable
fun VerdictChip(
    kind: VerdictKind,
    label: String,
    evidence: String?,
    modifier: Modifier = Modifier
) {
    val colors: VerdictColors = when (kind) {
        VerdictKind.PASS -> ChiaroTheme.colors.pass
        VerdictKind.UNSTABLE -> ChiaroTheme.colors.unstable
        VerdictKind.FAIL -> ChiaroTheme.colors.fail
        VerdictKind.UNKNOWN -> ChiaroTheme.colors.unknown
    }
    val spoken = listOfNotNull(label, evidence).joinToString(", ")
    Row(
        modifier = modifier
            .background(colors.container, CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            // One announcement, not three: a screen reader should say "great, 12% cloud",
            // never "check mark, great, 12% cloud".
            .clearAndSetSemantics { contentDescription = spoken },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(ChiaroIcons.verdictMarkRes(kind)),
            contentDescription = null, // the row's own semantics say the word
            tint = colors.ink,
            modifier = Modifier.size(VerdictMarkSize.forText())
        )
        Text(label, style = MaterialTheme.typography.labelLarge, color = colors.ink)
        if (evidence != null) {
            Text(
                text = evidence,
                style = MaterialTheme.typography.labelLarge.tabular(),
                color = colors.ink.copy(alpha = 0.8f)
            )
        }
    }
}

/** The mark's box beside a `labelLarge` (14 sp) word: 14 dp, of which the drawing fills
 * ~15/24 — the cap height of the letters next to it. */
private val VerdictMarkSize = 14.dp

@Preview(name = "Verdicts", showBackground = true)
@Composable
private fun VerdictChipPreview() {
    com.callbackdev.chiaro.ui.theme.ChiaroTheme(dynamicColor = false) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VerdictChip(VerdictKind.PASS, "Ottimo", "12% nuvole")
            VerdictChip(VerdictKind.UNSTABLE, "Così così", "61% nuvole")
            VerdictChip(VerdictKind.FAIL, "Niente da fare", "94% nuvole")
            VerdictChip(VerdictKind.UNKNOWN, "Troppo lontano", null)
        }
    }
}
