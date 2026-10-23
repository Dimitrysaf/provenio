package io.github.dimitrysaf.provenio.shell.components.markdown

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.CachePolicy
import io.github.dimitrysaf.provenio.shell.components.ImageViewerHost
import io.github.dimitrysaf.provenio.shell.components.ImageViewerRequest
import io.github.dimitrysaf.provenio.shell.components.LocalImageViewerLauncher
import io.github.dimitrysaf.provenio.shell.components.ToastController
import io.github.dimitrysaf.provenio.shell.components.WithTooltip
import io.github.dimitrysaf.provenio.shell.components.skeleton
import io.github.dimitrysaf.provenio.shell.components.viewerImageOf
import io.github.dimitrysaf.provenio.shell.screens.settings.SettingsList
import io.github.dimitrysaf.provenio.shell.theme.provenio
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.*
import coil3.size.Size as ImageSize

/**
 * Draws [document] in a column, the way GitHub draws Markdown, in the app's own type and colours.
 * Links are handed to [onLinkClick] as written; pictures open in the image viewer when tapped.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MarkdownContent(
    document: MarkdownDocument,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // A link to one of the page's own headings scrolls whatever holds the page to bring it to the top.
    val requesters = remember(document) { document.anchors.mapValues { BringIntoViewRequester() } }
    val byBlock = remember(document) { document.anchors.entries.associate { (anchor, index) -> index to requesters.getValue(anchor) } }
    val scope = rememberCoroutineScope()
    val handleLink: (String) -> Unit = { link ->
        val requester = link.takeIf { it.startsWith("#") }?.let { requesters[it.removePrefix("#")] }
        if (requester != null) scope.launch { requester.bringIntoView(TopOfView) } else onLinkClick(link)
    }
    MarkdownScope(handleLink) {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(BlockSpacing)) {
            document.blocks.forEachIndexed { index, block ->
                MarkdownBlockView(block, byBlock[index]?.let { Modifier.bringIntoViewRequester(it) } ?: Modifier)
            }
        }
    }
}

// Taller than any screen, so bringing it into view puts its top edge at the top rather than at the bottom.
private val TopOfView = Rect(0f, 0f, 1f, 100_000f)

/**
 * [document] as items of a lazy list, one per block, so a long page draws only what is on screen
 * and a link to a heading can scroll to its item with [MarkdownDocument.anchors].
 */
fun LazyListScope.markdownItems(document: MarkdownDocument, onLinkClick: (String) -> Unit) {
    itemsIndexed(document.blocks) { index, block ->
        MarkdownScope(onLinkClick) {
            MarkdownBlockView(block, Modifier.padding(top = if (index == 0) 0.dp else BlockSpacing))
        }
    }
}

@Composable
private fun MarkdownScope(onLinkClick: (String) -> Unit, content: @Composable () -> Unit) {
    val current by rememberUpdatedState(onLinkClick)
    val handler = remember { { url: String -> current(url) } }
    // Pictures open in the screen's image viewer, or in one of their own where the screen has none.
    val screenViewer = LocalImageViewerLauncher.current
    var request by remember { mutableStateOf<ImageViewerRequest?>(null) }
    val viewer = screenViewer ?: remember { { opened: ImageViewerRequest -> request = opened } }
    CompositionLocalProvider(
        LocalMarkdownLinkHandler provides handler,
        LocalImageViewerLauncher provides viewer,
        content = content,
    )
    if (screenViewer == null) ImageViewerHost(request = request, onDismiss = { request = null })
}

private val LocalMarkdownLinkHandler = staticCompositionLocalOf<(String) -> Unit> { {} }

// How deep a list sits in other lists, which picks its bullet as GitHub does: •, ◦, ▪.
private val LocalListDepth = compositionLocalOf { 0 }

@Composable
private fun MarkdownBlockView(block: MarkdownBlock, modifier: Modifier = Modifier) {
    when (block) {
        is MarkdownBlock.Heading -> HeadingView(block, modifier)
        is MarkdownBlock.Paragraph -> ParagraphView(block.content, modifier)
        is MarkdownBlock.Code -> CodeBlockView(block, modifier)
        is MarkdownBlock.Quote -> QuoteView(block, modifier)
        is MarkdownBlock.Alert -> AlertView(block, modifier)
        is MarkdownBlock.ItemList -> ListView(block, modifier)
        is MarkdownBlock.Table -> TableView(block, modifier)
        is MarkdownBlock.Details -> DetailsView(block, modifier)
        is MarkdownBlock.Links -> LinksView(block, modifier)
        is MarkdownBlock.Images -> FlowRow(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, if (block.centered) Alignment.CenterHorizontally else Alignment.Start),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            block.images.forEach { MarkdownPicture(it, link = null) }
        }
        MarkdownBlock.Rule -> HorizontalDivider(
            modifier = modifier.padding(vertical = 8.dp),
            thickness = 2.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

@Composable
private fun BlocksColumn(blocks: List<MarkdownBlock>, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(BlockSpacing)) {
        blocks.forEach { MarkdownBlockView(it) }
    }
}

@Composable
private fun HeadingView(block: MarkdownBlock.Heading, modifier: Modifier) {
    val typography = MaterialTheme.typography
    val style = when (block.level) {
        1 -> typography.headlineMedium
        2 -> typography.headlineSmall
        3 -> typography.titleLarge
        4 -> typography.titleMedium
        else -> typography.titleSmall
    }
    // The two largest headings sit further apart from what came before.
    Text(
        text = rememberInlineText(block.content),
        style = style,
        color = if (block.level == 6) MaterialTheme.colorScheme.onSurfaceVariant else LocalContentColor.current,
        modifier = modifier.padding(top = if (block.level <= 2) 12.dp else 4.dp),
    )
}

private sealed interface ParagraphPart {
    class Words(val content: List<MarkdownInline>) : ParagraphPart
    class Pictures(val pictures: List<Pair<MarkdownInline.Image, String?>>) : ParagraphPart
}

// Text cannot hold a picture of a size not known ahead, so a paragraph's pictures, and links
// made of pictures such as badges, are drawn between its runs of text.
private fun paragraphParts(content: List<MarkdownInline>): List<ParagraphPart> {
    val parts = mutableListOf<ParagraphPart>()
    val words = mutableListOf<MarkdownInline>()
    val pictures = mutableListOf<Pair<MarkdownInline.Image, String?>>()
    fun flushWords() {
        if (words.any { it !is MarkdownInline.Text || it.text.isNotBlank() }) parts += ParagraphPart.Words(words.toList())
        words.clear()
    }
    fun flushPictures() {
        if (pictures.isNotEmpty()) parts += ParagraphPart.Pictures(pictures.toList())
        pictures.clear()
    }
    content.forEach { inline ->
        val inlinePictures = picturesOf(inline)
        when {
            inlinePictures != null -> {
                flushWords()
                pictures += inlinePictures
            }
            pictures.isNotEmpty() && (inline == MarkdownInline.LineBreak || (inline is MarkdownInline.Text && inline.text.isBlank())) -> Unit
            else -> {
                flushPictures()
                words += inline
            }
        }
    }
    flushWords()
    flushPictures()
    return parts
}

private fun picturesOf(inline: MarkdownInline): List<Pair<MarkdownInline.Image, String?>>? = when (inline) {
    is MarkdownInline.Image -> listOf(inline to null)
    is MarkdownInline.Link -> {
        val inner = inline.content.filterNot { it is MarkdownInline.Text && it.text.isBlank() }
        if (inner.isNotEmpty() && inner.all { it is MarkdownInline.Image }) {
            inner.map { (it as MarkdownInline.Image) to inline.url }
        } else {
            null
        }
    }
    else -> null
}

@Composable
private fun ParagraphView(content: List<MarkdownInline>, modifier: Modifier) {
    val parts = remember(content) { paragraphParts(content) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        parts.forEach { part ->
            when (part) {
                is ParagraphPart.Words -> Text(text = rememberInlineText(part.content), style = MaterialTheme.typography.bodyLarge)
                is ParagraphPart.Pictures -> FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    part.pictures.forEach { (image, link) -> MarkdownPicture(image, link) }
                }
            }
        }
    }
}

/**
 * Which pictures were fetched from the network in this run of the app. A picture's saved copy is
 * only its fallback, so one replaced under the same address shows as it is now; [renew] asks for
 * every picture afresh, for a refresh asked for by hand.
 */
internal object MarkdownPictures {
    private val fetched = mutableSetOf<String>()

    /** Counts the refreshes asked for, so the pictures on screen ask again when it moves. */
    var generation by mutableIntStateOf(0)
        private set

    fun isFresh(url: String): Boolean = url in fetched

    fun markFresh(url: String) {
        fetched += url
    }

    fun renew() {
        fetched.clear()
        generation++
    }
}

@Composable
private fun MarkdownPicture(image: MarkdownInline.Image, link: String?, modifier: Modifier = Modifier) {
    val context = LocalPlatformContext.current
    val generation = MarkdownPictures.generation
    // The network first, once a run; the saved copy only when it cannot be reached.
    var fromCache by remember(image.url, generation) { mutableStateOf(MarkdownPictures.isFresh(image.url)) }
    var failed by remember(image.url, generation) { mutableStateOf(false) }
    val request = remember(image.url, context, generation, fromCache) {
        ImageRequest.Builder(context)
            .data(image.url)
            // Loaded at its own size, so a small picture such as a badge keeps it instead of filling the page.
            .size(ImageSize.ORIGINAL)
            .memoryCacheKey("${image.url}#$generation")
            .placeholderMemoryCacheKey("${image.url}#${generation - 1}")
            .apply { if (!fromCache) diskCachePolicy(CachePolicy.WRITE_ONLY) }
            .build()
    }
    var natural by remember(image.url) { mutableStateOf<androidx.compose.ui.geometry.Size?>(null) }
    val viewer = LocalImageViewerLauncher.current
    val openLink = LocalMarkdownLinkHandler.current
    BoxWithConstraints(modifier = modifier) {
        val loaded = natural
        // A picture's pixels are drawn as dp, the way a browser draws CSS pixels, never wider than the page.
        val width = (image.width?.dp ?: loaded?.width?.dp)?.coerceAtMost(maxWidth) ?: maxWidth
        // Until the picture says its own shape, a 16:9 stand-in holds its place, so the page does not jump.
        val aspect = loaded?.let { it.width / it.height } ?: PictureStandInAspect
        val shape = RoundedCornerShape(8.dp)
        Box(
            modifier = Modifier
                .width(width)
                .aspectRatio(aspect)
                .clip(shape)
                .clickable(enabled = loaded != null || link != null) {
                    if (link != null) {
                        openLink(link)
                    } else {
                        viewer?.invoke(ImageViewerRequest(images = listOf(viewerImageOf(image.url)), title = image.alt))
                    }
                },
        ) {
            when {
                failed -> BrokenPicture(alt = image.alt, modifier = Modifier.matchParentSize())
                loaded == null -> Box(modifier = Modifier.matchParentSize().skeleton(shape))
            }
            AsyncImage(
                model = request,
                contentDescription = image.alt.ifBlank { null },
                contentScale = ContentScale.Fit,
                onSuccess = { state ->
                    val size = state.painter.intrinsicSize
                    if (size.isSpecified && size.width > 0f && size.height > 0f) natural = size
                    if (!fromCache) MarkdownPictures.markFresh(image.url)
                    failed = false
                },
                onError = {
                    if (fromCache) failed = true else fromCache = true
                },
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

/** A picture that could not be loaded, fetched or saved: what it was meant to show, if it says. */
@Composable
private fun BrokenPicture(alt: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        Icon(
            imageVector = Icons.Rounded.BrokenImage,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (alt.isNotBlank()) {
            Text(
                text = alt,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
    }
}

private const val PictureStandInAspect = 16f / 9f

/**
 * Links on their own as rows, the way Settings lists its pages: a page of the document's own opens
 * with a chevron, and a link that leaves it says so with the open-in-browser icon.
 */
@Composable
private fun LinksView(block: MarkdownBlock.Links, modifier: Modifier) {
    val openLink = LocalMarkdownLinkHandler.current
    SettingsList(modifier = modifier) {
        block.links.forEach { link ->
            val leaves = LeavesDocument.containsMatchIn(link.url)
            navigationRow(
                title = plainText(link.content),
                opensPage = !leaves,
                trailingContent = OpensBrowserIcon.takeIf { leaves },
                onClick = { openLink(link.url) },
            )
        }
    }
}

private val OpensBrowserIcon: @Composable () -> Unit = {
    Icon(imageVector = Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null)
}

private val LeavesDocument = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:")

@Composable
private fun CodeBlockView(block: MarkdownBlock.Code, modifier: Modifier) {
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    val copyLabel = stringResource(Res.string.action_copy)
    val copied = stringResource(Res.string.text_copied)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = block.language.orEmpty(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                WithTooltip(copyLabel) {
                    IconButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(block.code))
                            ToastController.show(copied)
                        },
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = copyLabel, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Text(
                text = block.code,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                softWrap = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            )
        }
    }
}

@Composable
private fun QuoteView(block: MarkdownBlock.Quote, modifier: Modifier) {
    Row(modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
            BlocksColumn(block.blocks, Modifier.padding(start = 16.dp))
        }
    }
}

@Composable
private fun AlertView(block: MarkdownBlock.Alert, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val (color, icon, title) = when (block.kind) {
        MarkdownAlertKind.Note -> Triple(colors.primary, Icons.Rounded.Info, Res.string.markdown_alert_note)
        MarkdownAlertKind.Tip -> Triple(MaterialTheme.provenio.colors.success, Icons.Rounded.AutoAwesome, Res.string.markdown_alert_tip)
        MarkdownAlertKind.Important -> Triple(colors.tertiary, Icons.Rounded.PriorityHigh, Res.string.markdown_alert_important)
        MarkdownAlertKind.Warning -> Triple(MaterialTheme.provenio.colors.warning, Icons.Rounded.Warning, Res.string.markdown_alert_warning)
        MarkdownAlertKind.Caution -> Triple(colors.error, Icons.Rounded.ErrorOutline, Res.string.markdown_alert_caution)
    }
    AlertBox(color = color, icon = icon, title = title, modifier = modifier) {
        BlocksColumn(block.blocks)
    }
}

/** A callout in its colour, as Settings shows its warnings: a tinted card with an icon and a title. */
@Composable
private fun AlertBox(
    color: Color,
    icon: ImageVector,
    title: StringResource,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Text(text = stringResource(title), style = MaterialTheme.typography.titleSmall, color = color)
        }
        content()
    }
}

@Composable
private fun ListView(block: MarkdownBlock.ItemList, modifier: Modifier) {
    val depth = LocalListDepth.current
    val bodyStyle = MaterialTheme.typography.bodyLarge
    val markerWidth = if (block.ordered) 32.dp else 24.dp
    CompositionLocalProvider(LocalListDepth provides depth + 1) {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            block.items.forEachIndexed { index, item ->
                Row {
                    Box(modifier = Modifier.width(markerWidth), contentAlignment = Alignment.TopStart) {
                        when {
                            item.checked != null -> Checkbox(
                                checked = item.checked,
                                onCheckedChange = null,
                                modifier = Modifier.padding(top = 2.dp).size(20.dp),
                            )
                            block.ordered -> Text(text = "${block.start + index}.", style = bodyStyle)
                            else -> Text(text = Bullets[depth % Bullets.size], style = bodyStyle)
                        }
                    }
                    BlocksColumn(item.blocks, Modifier.weight(1f))
                }
            }
        }
    }
}

private val Bullets = listOf("•", "◦", "▪")

@Composable
private fun TableView(block: MarkdownBlock.Table, modifier: Modifier) {
    val columns = block.alignments.size
    if (columns == 0) return
    val border = MaterialTheme.colorScheme.outlineVariant
    val headerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val shape = RoundedCornerShape(12.dp)
    val geometry = remember { TableGeometry() }
    Box(modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        Layout(
            modifier = Modifier
                .clip(shape)
                .border(1.dp, border, shape)
                .drawBehind {
                    val rows = geometry.rowEdges
                    if (rows.size > 1) drawRect(headerColor, size = size.copy(height = rows[1].toFloat()))
                    val stroke = 1.dp.toPx()
                    rows.drop(1).dropLast(1).forEach { y ->
                        drawLine(border, Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), stroke)
                    }
                    geometry.columnEdges.drop(1).dropLast(1).forEach { x ->
                        drawLine(border, Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), stroke)
                    }
                },
            content = {
                (listOf(block.header) + block.rows).forEachIndexed { rowIndex, row ->
                    row.forEachIndexed { column, cell ->
                        Box(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = when (block.alignments[column]) {
                                MarkdownAlignment.Start -> Alignment.TopStart
                                MarkdownAlignment.Center -> Alignment.TopCenter
                                MarkdownAlignment.End -> Alignment.TopEnd
                            },
                        ) {
                            Text(
                                text = rememberInlineText(cell),
                                style = MaterialTheme.typography.bodyMedium.let {
                                    if (rowIndex == 0) it.copy(fontWeight = FontWeight.SemiBold) else it
                                },
                            )
                        }
                    }
                }
            },
        ) { measurables, _ ->
            // Each column is as wide as its widest cell, up to a limit past which cells wrap.
            val maxCellWidth = MaxCellWidth.roundToPx()
            val widths = IntArray(columns)
            measurables.forEachIndexed { index, measurable ->
                val column = index % columns
                widths[column] = maxOf(widths[column], measurable.maxIntrinsicWidth(Constraints.Infinity).coerceAtMost(maxCellWidth))
            }
            val placeables = measurables.mapIndexed { index, measurable ->
                val width = widths[index % columns]
                measurable.measure(Constraints(minWidth = width, maxWidth = width))
            }
            val rowCount = placeables.size / columns
            val heights = IntArray(rowCount) { row -> (0 until columns).maxOf { placeables[row * columns + it].height } }
            geometry.columnEdges = widths.runningFold(0) { edge, width -> edge + width }.toIntArray()
            geometry.rowEdges = heights.runningFold(0) { edge, height -> edge + height }.toIntArray()
            layout(geometry.columnEdges.last(), geometry.rowEdges.last()) {
                placeables.forEachIndexed { index, placeable ->
                    placeable.place(geometry.columnEdges[index % columns], geometry.rowEdges[index / columns])
                }
            }
        }
    }
}

// Where the table's lines fall, found while laying it out and drawn after.
private class TableGeometry {
    var columnEdges = IntArray(0)
    var rowEdges = IntArray(0)
}

@Composable
private fun DetailsView(block: MarkdownBlock.Details, modifier: Modifier) {
    var open by rememberSaveable { mutableStateOf(false) }
    val turn by animateFloatAsState(if (open) 0f else -90f, label = "MarkdownDetails")
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { open = !open }
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(imageVector = Icons.Rounded.ExpandMore, contentDescription = null, modifier = Modifier.rotate(turn))
            Text(text = rememberInlineText(block.summary), style = MaterialTheme.typography.titleSmall)
        }
        AnimatedVisibility(visible = open) {
            BlocksColumn(block.blocks, Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp))
        }
    }
}

@Immutable
private class InlineColors(
    val link: Color,
    val codeBackground: Color,
    val highlightBackground: Color,
    val highlightText: Color,
)

@Composable
private fun rememberInlineText(content: List<MarkdownInline>): AnnotatedString {
    val scheme = MaterialTheme.colorScheme
    val colors = remember(scheme) {
        InlineColors(
            link = scheme.primary,
            codeBackground = scheme.surfaceContainerHighest,
            highlightBackground = scheme.tertiaryContainer,
            highlightText = scheme.onTertiaryContainer,
        )
    }
    val openLink = LocalMarkdownLinkHandler.current
    return remember(content, colors, openLink) {
        buildAnnotatedString { content.forEach { appendInline(it, colors, openLink) } }
    }
}

private fun AnnotatedString.Builder.appendInline(
    inline: MarkdownInline,
    colors: InlineColors,
    openLink: (String) -> Unit,
) {
    when (inline) {
        is MarkdownInline.Text -> append(inline.text)
        MarkdownInline.LineBreak -> append('\n')
        is MarkdownInline.Code -> withStyle(
            SpanStyle(fontFamily = FontFamily.Monospace, background = colors.codeBackground, fontSize = 0.9.em),
        ) { append(inline.code) }
        is MarkdownInline.Image -> append(inline.alt)
        is MarkdownInline.Link -> withLink(
            LinkAnnotation.Clickable(
                tag = inline.url,
                styles = TextLinkStyles(SpanStyle(color = colors.link, textDecoration = TextDecoration.Underline)),
                linkInteractionListener = { openLink(inline.url) },
            ),
        ) { inline.content.forEach { appendInline(it, colors, openLink) } }
        is MarkdownInline.Styled -> withStyle(spanOf(inline.style, colors)) {
            inline.content.forEach { appendInline(it, colors, openLink) }
        }
    }
}

private fun spanOf(style: MarkdownTextStyle, colors: InlineColors): SpanStyle = when (style) {
    MarkdownTextStyle.Bold -> SpanStyle(fontWeight = FontWeight.Bold)
    MarkdownTextStyle.Italic -> SpanStyle(fontStyle = FontStyle.Italic)
    MarkdownTextStyle.Strikethrough -> SpanStyle(textDecoration = TextDecoration.LineThrough)
    MarkdownTextStyle.Underline -> SpanStyle(textDecoration = TextDecoration.Underline)
    MarkdownTextStyle.Highlight -> SpanStyle(background = colors.highlightBackground, color = colors.highlightText)
    MarkdownTextStyle.Keyboard -> SpanStyle(fontFamily = FontFamily.Monospace, background = colors.codeBackground, fontSize = 0.9.em)
    MarkdownTextStyle.Subscript -> SpanStyle(baselineShift = BaselineShift.Subscript, fontSize = 0.75.em)
    MarkdownTextStyle.Superscript -> SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 0.75.em)
}

private val BlockSpacing = 12.dp
private val MaxCellWidth = 280.dp
