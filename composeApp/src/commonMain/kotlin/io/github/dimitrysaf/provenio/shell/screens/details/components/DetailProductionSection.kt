package io.github.dimitrysaf.provenio.shell.screens.details.components

import io.github.dimitrysaf.provenio.shell.components.FindableText
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.core.metadata.MetaCompany
import io.github.dimitrysaf.provenio.core.metadata.MetaDetails
import provenio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/** The title's networks or production companies as a segmented list, each with its own logo. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DetailProductionSection(
    meta: MetaDetails,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
    onCompanyClick: ((MetaCompany, String) -> Unit)? = null,
) {
    val isSeriesLike = meta.type == "series" || meta.videos.any { it.season != null || it.episode != null }
    val isNetworkSource = isSeriesLike && meta.networks.isNotEmpty()
    val sourceItems = if (isSeriesLike) {
        meta.networks.ifEmpty { meta.productionCompanies }
    } else {
        meta.productionCompanies.ifEmpty { meta.networks }
    }
    val displayItems = sourceItems.take(MaxProductionItems)
    if (displayItems.isEmpty()) return

    val entityKind = if (isNetworkSource) "network" else "company"

    DetailSection(
        title = if (isSeriesLike) {
            stringResource(Res.string.details_networks)
        } else {
            stringResource(Res.string.meta_section_production_title)
        },
        modifier = modifier,
        showHeader = showHeader,
    ) {
        DetailSegmentedColumn {
            displayItems.forEachIndexed { index, item ->
                val open = if (onCompanyClick != null && item.tmdbId != null) {
                    { onCompanyClick(item, entityKind) }
                } else {
                    null
                }
                SegmentedListItem(
                    onClick = { open?.invoke() },
                    shapes = detailSegmentShapes(index = index, count = displayItems.size),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = open != null,
                    colors = ListItemDefaults.segmentedColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                    leadingContent = item.logo?.takeIf(String::isNotBlank)?.let { logo ->
                        {
                            CompanyLogo(
                                url = logo,
                                contentDescription = null,
                                modifier = Modifier.height(LogoHeight).widthIn(max = LogoMaxWidth),
                            )
                        }
                    },
                ) {
                    FindableText(text = item.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

private val LogoHeight = 28.dp
private val LogoMaxWidth = 72.dp

private const val MaxProductionItems = 8
