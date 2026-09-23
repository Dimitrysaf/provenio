package com.nuvio.app.features.details

import androidx.compose.ui.unit.dp
import com.nuvio.app.core.metadata.MetaScreenSectionKey

// Screens at least this wide split the details page into two panes.
internal val DetailTwoPaneMinWidth = 840.dp

internal const val DetailPrimaryPaneWeight = 0.56f

private val PrimaryPaneOnlySections = setOf(MetaScreenSectionKey.ACTIONS, MetaScreenSectionKey.OVERVIEW)

// Episodes always take the side pane; otherwise the lowest section with content climbs up into it.
internal fun detailSidePaneSection(sectionKeys: List<MetaScreenSectionKey>): MetaScreenSectionKey? =
    if (MetaScreenSectionKey.EPISODES in sectionKeys) {
        MetaScreenSectionKey.EPISODES
    } else {
        sectionKeys.lastOrNull { it !in PrimaryPaneOnlySections }
    }
