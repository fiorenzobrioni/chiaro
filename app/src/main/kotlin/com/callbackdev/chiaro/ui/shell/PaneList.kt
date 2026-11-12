package com.callbackdev.chiaro.ui.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A tab's list said once and laid out twice (DESIGN §8.12c, 16 ott 2026): Sky and Alerts
 * name their blocks [across] the page, in the [left] pane and in the [right] one, in reading
 * order, and [paneItems] makes of them the phone's list (every block its own item, in the
 * order across, left, right, which is the order the page always had) or the wide page's (the
 * blocks across over the two panes, the panes one item of the list beside each other). One
 * description of the page, so the two layouts cannot drift apart in what they show.
 */
internal class PaneBlocks {
    internal class Block(val key: Any?, val content: @Composable () -> Unit)

    val across = mutableListOf<Block>()
    val left = mutableListOf<Block>()
    val right = mutableListOf<Block>()

    fun across(key: Any? = null, content: @Composable () -> Unit) { across += Block(key, content) }
    fun left(key: Any? = null, content: @Composable () -> Unit) { left += Block(key, content) }
    fun right(key: Any? = null, content: @Composable () -> Unit) { right += Block(key, content) }

    // Until 12 nov 2026 the blocks also measured the left pane's first one, for the label of
    // a floating button to fold once it had scrolled away: the Sky's button went into its
    // title with Fase 16.2 and Alerts' with its review, and nothing floats over a list now.
}

/**
 * The [blocks] as the items of a lazy list, by [columns]. On a wide page the panes are ONE
 * item, as Today's are: two columns of a lazy list cannot scroll as one page, and both lists
 * hold a screen or two at that width anyway. Each pane takes [PageColumns.pane]; the page
 * keeps [PageColumns.start] and [PageColumns.end] at its edges.
 */
internal fun LazyListScope.paneItems(blocks: PaneBlocks, columns: PageColumns) {
    // The page's edges, in one column too: a page laid out under the rail (Sky, since Fase
    // 16.1) keeps its single column clear of it.
    val paper = Modifier.padding(start = columns.start, end = columns.end)
    if (!columns.twoPanes) {
        (blocks.across + blocks.left + blocks.right).forEach { block ->
            item(key = block.key) { Box(paper) { block.content() } }
        }
        return
    }
    blocks.across.forEach { block -> item(key = block.key) { Box(paper) { block.content() } } }
    item(key = PanesKey) {
        Row(paper) {
            Column(Modifier.weight(1f)) {
                blocks.left.forEach { block -> keyed(block) }
            }
            Column(Modifier.weight(1f)) {
                blocks.right.forEach { block -> keyed(block) }
            }
        }
    }
}

@Composable
private fun keyed(block: PaneBlocks.Block) {
    if (block.key != null) key(block.key) { block.content() } else block.content()
}

private const val PanesKey = "panes"

/**
 * Over the right pane's first title, where the left pane opens on a card (Alerts' answer;
 * the Sky's tonight too, until it became the page's canvas in Fase 16.1): the card's own 8dp of air, so the title starts on the card's top edge
 * rather than a section's 24 lower down.
 */
internal val PaneTitleTop: Dp = 8.dp
