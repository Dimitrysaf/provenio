package io.github.dimitrysaf.provenio.shell.screens.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.dimitrysaf.provenio.core.metadata.MetaDetails
import io.github.dimitrysaf.provenio.core.tracking.trakt.TraktCommentReview
import io.github.dimitrysaf.provenio.core.tracking.trakt.TraktCommentsRepository
import io.github.dimitrysaf.provenio.shell.screens.details.components.CommentDetailSheet
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.details_comments_load_failed
import org.jetbrains.compose.resources.getString

// The Trakt comments of one title, loaded a page at a time.
@Stable
internal class DetailCommentsState {
    var items by mutableStateOf<List<TraktCommentReview>>(emptyList())
        private set
    var currentPage by mutableIntStateOf(0)
        private set
    var pageCount by mutableIntStateOf(0)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var isLoadingMore by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var selected by mutableStateOf<TraktCommentReview?>(null)

    val hasMorePages: Boolean
        get() = currentPage < pageCount

    fun clear() {
        items = emptyList()
        currentPage = 0
        pageCount = 0
        error = null
    }

    suspend fun loadFirstPage(meta: MetaDetails, forceRefresh: Boolean = false) {
        isLoading = true
        error = null
        try {
            val result = TraktCommentsRepository.getCommentsPage(meta, page = 1, forceRefresh = forceRefresh)
            items = result.items
            currentPage = result.currentPage
            pageCount = result.pageCount
        } catch (e: Exception) {
            error = e.message ?: getString(Res.string.details_comments_load_failed)
        }
        isLoading = false
    }

    suspend fun loadNextPage(meta: MetaDetails) {
        isLoadingMore = true
        try {
            val result = TraktCommentsRepository.getCommentsPage(meta, page = currentPage + 1)
            val existingIds = items.map { it.id }.toSet()
            items = items + result.items.filter { it.id !in existingIds }
            currentPage = result.currentPage
            pageCount = result.pageCount
        } catch (_: Exception) {
        }
        isLoadingMore = false
    }
}

// The full comment sheet, paging through the loaded comments and fetching more near the end.
@Composable
internal fun CommentDetailHost(
    state: DetailCommentsState,
    onLoadMore: () -> Unit,
) {
    val comment = state.selected ?: return
    val comments = state.items
    val commentIndex = comments.indexOfFirst { it.id == comment.id }.coerceAtLeast(0)
    CommentDetailSheet(
        comment = comment,
        currentIndex = commentIndex,
        totalCount = comments.size,
        canGoBack = commentIndex > 0,
        canGoForward = commentIndex < comments.size - 1,
        onPrevious = {
            if (commentIndex > 0) {
                state.selected = comments[commentIndex - 1]
            }
        },
        onNext = {
            val nextIndex = commentIndex + 1
            if (nextIndex < comments.size) {
                state.selected = comments[nextIndex]
            }
            if (nextIndex >= comments.size - 3 && state.hasMorePages) {
                onLoadMore()
            }
        },
        onDismiss = { state.selected = null },
    )
}
