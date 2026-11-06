package io.github.dimitrysaf.provenio.shell.screens.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.core.metadata.MetaDetails
import io.github.dimitrysaf.provenio.core.metadata.MetaDetailsRepository
import io.github.dimitrysaf.provenio.core.metadata.MetaPerson
import io.github.dimitrysaf.provenio.shell.components.FindableText
import io.github.dimitrysaf.provenio.shell.components.LoadingSpinner
import io.github.dimitrysaf.provenio.shell.components.SearchField
import io.github.dimitrysaf.provenio.shell.screens.details.components.CastRow
import io.github.dimitrysaf.provenio.shell.screens.details.components.CommentRow
import io.github.dimitrysaf.provenio.shell.screens.details.components.DetailListSkeleton
import io.github.dimitrysaf.provenio.shell.screens.details.components.detailSegmentShapes
import io.github.dimitrysaf.provenio.shell.screens.details.components.rememberResolvedCast
import io.github.dimitrysaf.provenio.shell.screens.settings.ListItemBetweenSpace
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.detail_comments_title
import provenio.composeapp.generated.resources.details_cast_search_placeholder
import provenio.composeapp.generated.resources.details_comments_search_placeholder
import provenio.composeapp.generated.resources.details_list_no_matches
import provenio.composeapp.generated.resources.settings_meta_cast

/**
 * What a list page's search keeps: text the query matches as a case-insensitive pattern, or as
 * plain text when it is not a valid pattern. Null for an empty query, which keeps everything.
 */
internal fun listSearchMatcher(query: String): ((String) -> Boolean)? {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return null
    val pattern = runCatching { Regex(trimmed, RegexOption.IGNORE_CASE) }
        .getOrElse { Regex(Regex.escape(trimmed), RegexOption.IGNORE_CASE) }
    return { text -> pattern.containsMatchIn(text) }
}

/** The whole cast of a title, searchable, laid out like the other info pages. */
@Composable
fun CastListScreen(
    type: String,
    metaId: String,
    onBack: () -> Unit,
    onCastClick: (MetaPerson, String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val meta = rememberListPageMeta(type, metaId)
    val cast = rememberResolvedCast(meta?.cast.orEmpty(), metaId, type)
    var query by rememberSaveable { mutableStateOf("") }
    val matcher = remember(query) { listSearchMatcher(query) }
    val shown = remember(cast, matcher) {
        cast.withIndex().filter { (_, person) ->
            matcher == null || matcher(person.name) || person.role?.let(matcher) == true
        }
    }
    val placeholder = stringResource(Res.string.details_cast_search_placeholder)

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        DetailPage(
            pageKey = "cast-$metaId",
            name = stringResource(Res.string.settings_meta_cast),
            subtitle = meta?.name?.takeIf(String::isNotBlank),
            images = meta.listPageImages(),
            deceased = false,
            onBack = onBack,
            infoItems = { metrics ->
                listSearchItem(metrics, query, placeholder) { query = it }
                when {
                    meta == null -> listLoadingItem()
                    shown.isEmpty() -> listNoMatchesItem(metrics)
                    else -> itemsIndexed(shown, key = { _, (index, person) -> "cast-$index-${person.name}" }) { position, (index, person) ->
                        DetailSectionContainer(
                            horizontalPadding = metrics.horizontalPadding,
                            contentMaxWidth = metrics.contentMaxWidth,
                            bottomPadding = if (position == shown.lastIndex) 20.dp else ListItemBetweenSpace,
                        ) {
                            CastRow(
                                index = index,
                                person = person,
                                shapes = detailSegmentShapes(index = position, count = shown.size),
                                onCastClick = onCastClick,
                                sharedTransitionScope = null,
                                animatedVisibilityScope = null,
                            )
                        }
                    }
                }
            },
            railItems = {},
            hasRails = false,
        )
    }
}

/**
 * The Trakt comments of a title, searchable among those loaded, laid out like the other info
 * pages. Scrolling to the end loads the next page while no search is narrowing the list.
 */
@Composable
fun CommentsListScreen(
    type: String,
    metaId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val meta = rememberListPageMeta(type, metaId)
    val comments = remember(type, metaId) { DetailCommentsState() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(meta) {
        if (meta != null && !comments.hasLoaded) comments.loadFirstPage(meta)
    }
    var query by rememberSaveable { mutableStateOf("") }
    val matcher = remember(query) { listSearchMatcher(query) }
    val shown = remember(comments.items, matcher) {
        comments.items.filter { review ->
            matcher == null || matcher(review.authorDisplayName) || matcher(review.comment)
        }
    }
    val placeholder = stringResource(Res.string.details_comments_search_placeholder)
    val loadMore: () -> Unit = {
        meta?.let { loaded -> scope.launch { comments.loadNextPage(loaded) } }
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        DetailPage(
            pageKey = "comments-$metaId",
            name = stringResource(Res.string.detail_comments_title),
            subtitle = meta?.name?.takeIf(String::isNotBlank),
            images = meta.listPageImages(),
            deceased = false,
            onBack = onBack,
            infoItems = { metrics ->
                listSearchItem(metrics, query, placeholder) { query = it }
                when {
                    meta == null || (comments.isLoading && comments.items.isEmpty()) -> item(key = "comments-loading") {
                        DetailSectionContainer(
                            horizontalPadding = metrics.horizontalPadding,
                            contentMaxWidth = metrics.contentMaxWidth,
                        ) { DetailListSkeleton() }
                    }
                    shown.isEmpty() -> listNoMatchesItem(metrics)
                    else -> {
                        itemsIndexed(shown, key = { _, review -> "comment-${review.id}" }) { position, review ->
                            DetailSectionContainer(
                                horizontalPadding = metrics.horizontalPadding,
                                contentMaxWidth = metrics.contentMaxWidth,
                                bottomPadding = if (position == shown.lastIndex) 20.dp else ListItemBetweenSpace,
                            ) {
                                CommentRow(
                                    review = review,
                                    shapes = detailSegmentShapes(index = position, count = shown.size),
                                    onClick = { comments.selected = review },
                                )
                            }
                        }
                        if (matcher == null && comments.hasMorePages) {
                            item(key = "comments-more") {
                                LaunchedEffect(comments.currentPage) { if (!comments.isLoadingMore) loadMore() }
                                ListLoadingIndicator()
                            }
                        }
                    }
                }
            },
            railItems = {},
            hasRails = false,
        )
    }
    CommentDetailHost(state = comments, onLoadMore = loadMore)
}

@Composable
private fun rememberListPageMeta(type: String, metaId: String): MetaDetails? {
    var meta by remember(type, metaId) { mutableStateOf(MetaDetailsRepository.peek(type, metaId)) }
    LaunchedEffect(type, metaId) {
        if (meta == null) MetaDetailsRepository.fetch(type, metaId)?.let { meta = it }
    }
    return meta
}

private fun MetaDetails?.listPageImages(): List<String> =
    listOfNotNull(this?.background?.takeIf(String::isNotBlank) ?: this?.poster?.takeIf(String::isNotBlank))

private fun LazyListScope.listSearchItem(
    metrics: DetailPageMetrics,
    query: String,
    placeholder: String,
    onQueryChange: (String) -> Unit,
) {
    item(key = "list-search") {
        DetailSectionContainer(
            horizontalPadding = metrics.horizontalPadding,
            contentMaxWidth = metrics.contentMaxWidth,
            bottomPadding = 12.dp,
        ) {
            SearchField(query = query, onQueryChange = onQueryChange, placeholder = placeholder)
        }
    }
}

private fun LazyListScope.listNoMatchesItem(metrics: DetailPageMetrics) {
    item(key = "list-no-matches") {
        DetailSectionContainer(
            horizontalPadding = metrics.horizontalPadding,
            contentMaxWidth = metrics.contentMaxWidth,
        ) {
            FindableText(
                text = stringResource(Res.string.details_list_no_matches),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun LazyListScope.listLoadingItem() {
    item(key = "list-loading") { ListLoadingIndicator() }
}

@Composable
private fun ListLoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) { LoadingSpinner() }
}
