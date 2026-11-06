package io.github.dimitrysaf.provenio.shell.screens.details.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.dimitrysaf.provenio.core.metadata.MetaKeyword
import io.github.dimitrysaf.provenio.shell.components.FindableText
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.meta_section_keywords_title

/** TMDB's tags for the title, as a segmented list; tapping one searches for titles with it. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DetailKeywordsSection(
    keywords: List<MetaKeyword>,
    onKeywordClick: ((MetaKeyword) -> Unit)?,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
) {
    if (keywords.isEmpty()) return
    var showAll by rememberSaveable { mutableStateOf(false) }
    DetailSection(
        title = stringResource(Res.string.meta_section_keywords_title),
        modifier = modifier,
        showHeader = showHeader,
    ) {
        DetailSegmentedList(
            items = keywords,
            previewCount = DetailListPreviewCount,
            showingAll = showAll,
            onViewAll = { showAll = true },
            viewAllExpands = true,
        ) { keyword, shapes ->
            SegmentedListItem(
                onClick = { onKeywordClick?.invoke(keyword) },
                shapes = shapes,
                modifier = Modifier.fillMaxWidth(),
                enabled = onKeywordClick != null,
                colors = ListItemDefaults.segmentedColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            ) {
                FindableText(text = keyword.name, maxLines = 1)
            }
        }
    }
}
