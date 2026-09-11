package com.callbackdev.chiaro.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.ui.icons.ChiaroIcons
import com.callbackdev.chiaro.ui.theme.ChiaroTheme

/**
 * Where the air is going, as the weather family's own needle (Fase 13).
 *
 * Meteorology names a wind by where it comes FROM — "vento da nord-est" — and an arrow is
 * read as movement, so the glyph points the other way, where the air is heading, and the
 * words beside it say the source. The two agree the way a weather map's arrows agree with
 * its captions, and a reader who only glances gets the direction without decoding a
 * compass abbreviation.
 *
 * **It is one drawing turned, not one of eight** (11 set 2026, after the committente asked
 * whether to use Meteocons' `wind-direction-*` set). Three reasons, all of them in the
 * drawing rather than in taste:
 *
 * 1. those glyphs come in **eight** fixed points, and this arrow has always shown the
 *    exact bearing — going to eight would round the forecast into 45° buckets, which is
 *    losing precision the app already has, not gaining a picture;
 * 2. each of them carries the letters **N E S W drawn as paths**, and in Italian the west
 *    is **O**: that is English text inside an image, and in this product everything on a
 *    screen localizes;
 * 3. Meteocons' needle points where the wind comes FROM. Importing it whole would have
 *    silently reversed the decision the paragraph above records.
 *
 * So the importer keeps `wind-direction-n`'s `Pointer` group and drops its `Letters` one
 * (`PARTIALS` in `tools/import_meteocons_v3.py`), and the needle is turned here by the
 * real degrees. It is drawn untinted, like every other mark of the family: a flat tint
 * would make a silhouette of a drawing that has a shaft and a hub (DESIGN §13.1).
 */
@Composable
fun WindArrow(fromDegrees: Int, modifier: Modifier = Modifier) {
    // Zero is "from the north", which blows south: the needle, drawn pointing north,
    // turns half a turn and then follows the wind round the compass.
    Icon(
        imageVector = ChiaroIcons.windNeedle,
        contentDescription = null, // the words right beside it carry the direction (§10)
        tint = Color.Unspecified, // Meteocons carry their own measured colors
        modifier = modifier
            .size(WindArrowSize)
            .rotate((((fromDegrees % 360) + 360) % 360 + 180).toFloat())
    )
}

/**
 * 16dp, the size the hand-drawn arrow had, so no measure in the tile moves.
 *
 * The needle is imported cropped to a 48-unit window centred on its hub (`PARTIALS` in
 * `tools/import_meteocons_v3.py`) rather than left in the compass rose's 128-unit box:
 * uncropped it is thirteen units of ink in a hundred and twenty-eight, and at 16dp that
 * is a splinter — looked at, not assumed (`tools/icon_filmstrip.py`, 11 set 2026).
 * Cropped, the shaft and the head read at 16dp; the hub dot does not, at this size or
 * at 20, and that is fine — it is a joint, not information.
 */
private val WindArrowSize = 16.dp

@Preview(showBackground = true)
@Composable
private fun WindArrowPreview() {
    ChiaroTheme(dynamicColor = false) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(8.dp)
        ) {
            listOf(0, 45, 90, 180, 270, 315).forEach { WindArrow(fromDegrees = it) }
        }
    }
}
