package com.callbackdev.chiaro.ui.sky

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.sky.SkyJob
import com.callbackdev.chiaro.domain.sky.SkyJobCatalog
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import com.callbackdev.chiaro.ui.theme.GroupTop
import com.callbackdev.chiaro.ui.theme.SectionBottom

/**
 * The guide to the sky events: the index of all fifty-one, and a page for each.
 *
 * It has two doors, and neither is redundant. The **catalog sheet** carries it per
 * event, where the question actually arrives — you are about to add something called
 * a zodiacal light and you would like to know what that is — and the sheet keeps its
 * page inside itself, so reading about an event does not throw away the list you were
 * halfway down. The **index** is the door for a reader who came to learn rather than
 * to add, and it is reachable from the Sky screen and from the guide in Settings.
 *
 * A screen and not a tooltip, for the same reason `HELP.md` became a guide rather
 * than a paragraph: there is room here to explain, and an app that answers "what is
 * a blue hour" in a caption is answering a different question.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkyGuideRoute(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    initialJobId: String? = null
) {
    // Which page is open, or the index. Saved across a rotation like any other screen
    // the reader is in the middle of.
    var openId by rememberSaveable { mutableStateOf(initialJobId) }
    val job = openId?.let { SkyJobCatalog.byId(it) }
    // Back peels one layer: a page returns to the index it came from, the index closes.
    BackHandler {
        if (job != null && initialJobId == null) openId = null else onClose()
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (job == null) {
                            stringResource(R.string.sky_guide_title)
                        } else {
                            stringResource(SkyText.nameRes(job.id))
                        }
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (job != null && initialJobId == null) openId = null else onClose()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        val content = Modifier
            .fillMaxSize()
            .padding(padding)
        if (job == null) {
            SkyGuideIndex(onOpen = { openId = it }, modifier = content)
        } else {
            SkyEventPage(
                job = job,
                onOpenRelated = { openId = it },
                modifier = content
            )
        }
    }
}

/**
 * The index: every event the app can follow, in the groups the catalog uses, each
 * with the same one line the catalog shows. Grouped by what the event is about rather
 * than by when it next happens — this is a table of contents, not an agenda.
 */
@Composable
private fun SkyGuideIndex(onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier) {
        item {
            Text(
                text = stringResource(R.string.sky_guide_intro),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        SkyGuide.groups.forEach { group ->
            item {
                Text(
                    text = stringResource(group.titleRes),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(
                        start = 16.dp, top = GroupTop, bottom = SectionBottom
                    )
                )
            }
            items(group.jobs.size) { index ->
                val job = group.jobs[index]
                val name = stringResource(SkyText.nameRes(job.id))
                ListItem(
                    leadingContent = {
                        Icon(
                            imageVector = jobIcon(job),
                            contentDescription = null, // the name is right beside it
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(26.dp)
                        )
                    },
                    headlineContent = { Text(name) },
                    supportingContent = {
                        Text(stringResource(SkyText.explanationRes(job.id)))
                    },
                    modifier = Modifier.clickable { onOpen(job.id) }
                )
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

/**
 * One event's page: what it is, when it happens, and what to read next.
 *
 * [onBack] and [action] are the two things the sheet needs and the screen does not —
 * a back affordance of its own (a bottom sheet has no app bar) and the button that
 * adds the event you have just read about. Reading and adding in the same place is
 * the whole point of the catalog door: the answer to "what is this" is worth nothing
 * if you have to remember the name and go back for it.
 */
@Composable
internal fun SkyEventPage(
    job: SkyJob,
    onOpenRelated: (String) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    action: @Composable (() -> Unit)? = null
) {
    val resources = LocalContext.current.resources
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.padding(top = 4.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.action_back)
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = if (onBack == null) 8.dp else 0.dp)
        ) {
            Icon(
                imageVector = jobIcon(job),
                contentDescription = null, // the name is right beside it
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(36.dp)
            )
            Column {
                Text(
                    text = stringResource(SkyText.nameRes(job.id)),
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = stringResource(SkyText.explanationRes(job.id)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Prose is bodyLarge (DESIGN §5): 16/24, made to be read rather than scanned,
        // which is the same size the guide in Settings sets its paragraphs in.
        resources.getString(SkyGuide.pageRes(job.id))
            .split("\n\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { paragraph ->
                Text(text = paragraph, style = MaterialTheme.typography.bodyLarge)
            }

        SectionTitle(stringResource(R.string.sky_guide_section_when))
        SkyGuide.whenLines(resources, job).forEach { sentence ->
            Text(
                text = sentence,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val related = SkyGuide.seeAlso(job.id)
        if (related.isNotEmpty()) {
            SectionTitle(stringResource(R.string.sky_guide_section_related))
            RelatedEvents(related, onOpenRelated)
        }

        action?.invoke()
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = GroupTop)
    )
}

/** The neighbours, as chips: a short list of names to tap, not a second index. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RelatedEvents(ids: List<String>, onOpen: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ids.forEach { id ->
            val job = SkyJobCatalog.byId(id) ?: return@forEach
            AssistChip(
                onClick = { onOpen(id) },
                label = { Text(stringResource(SkyText.nameRes(id))) },
                leadingIcon = {
                    Icon(
                        imageVector = jobIcon(job),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SkyGuideIndexPreview() {
    ChiaroTheme(dynamicColor = false) {
        SkyGuideRoute(onClose = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun SkyEventPagePreview() {
    ChiaroTheme(dynamicColor = false) {
        SkyGuideRoute(onClose = {}, initialJobId = SkyJobCatalog.DarknessWindow.id)
    }
}
