package com.callbackdev.chiaro.ui.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
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
internal class PaneBlocks(
    /** The left pane's first block's height on a wide page, measured, for [firstInView]:
     * held by the caller (`remember`), because the blocks are said again at every composition. */
    private val firstLeftHeight: MutableIntState
) {
    internal class Block(val key: Any?, val content: @Composable () -> Unit)

    val across = mutableListOf<Block>()
    val left = mutableListOf<Block>()
    val right = mutableListOf<Block>()

    fun across(key: Any? = null, content: @Composable () -> Unit) { across += Block(key, content) }
    fun left(key: Any? = null, content: @Composable () -> Unit) { left += Block(key, content) }
    fun right(key: Any? = null, content: @Composable () -> Unit) { right += Block(key, content) }

    /**
     * Whether the page's first block is still on screen: what the FAB's label follows («read
     * once, then it covers rows»). On a phone that is the list's first item. On a wide page,
     * where the panes are one item from the top of the paper to its end, it is the left pane's
     * first block, measured, unless a block across stands above it as the list's first item.
     */
    fun firstInView(state: LazyListState, twoPanes: Boolean): Boolean =
        state.firstVisibleItemIndex == 0 &&
            (!twoPanes || across.isNotEmpty() || state.firstVisibleItemScrollOffset < firstLeftHeight.intValue)

    internal fun measureFirstLeft(height: Int) {
        firstLeftHeight.intValue = height
    }
}

/**
 * The [blocks] as the items of a lazy list, by [columns]. On a wide page the panes are ONE
 * item, as Today's are: two columns of a lazy list cannot scroll as one page, and both lists
 * hold a screen or two at that width anyway. Each pane takes [PageColumns.pane]; the page
 * keeps [PageColumns.start] and [PageColumns.end] at its edges.
 */
internal fun LazyListScope.paneItems(blocks: PaneBlocks, columns: PageColumns) {
    if (!columns.twoPanes) {
        (blocks.across + blocks.left + blocks.right).forEach { block ->
            item(key = block.key) { block.content() }
        }
        return
    }
    val paper = Modifier.padding(start = columns.start, end = columns.end)
    blocks.across.forEach { block -> item(key = block.key) { Box(paper) { block.content() } } }
    item(key = PanesKey) {
        Row(paper) {
            Column(Modifier.weight(1f)) {
                blocks.left.forEachIndexed { index, block ->
                    val measured = if (index == 0) {
                        Modifier.onSizeChanged { blocks.measureFirstLeft(it.height) }
                    } else Modifier
                    Box(measured) { keyed(block) }
                }
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
 * Over the right pane's first title, where the left pane opens on a card (the Sky's tonight,
 * Alerts' answer): the card's own 8dp of air, so the title starts on the card's top edge
 * rather than a section's [com.callbackdev.chiaro.ui.theme.SectionTop] lower down.
 */
internal val PaneTitleTop: Dp = 8.dp
