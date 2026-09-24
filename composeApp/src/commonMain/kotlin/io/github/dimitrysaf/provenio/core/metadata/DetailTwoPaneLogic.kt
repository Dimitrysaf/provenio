package io.github.dimitrysaf.provenio.core.metadata


private val PrimaryPaneOnlySections = setOf(MetaScreenSectionKey.ACTIONS, MetaScreenSectionKey.OVERVIEW)

// Episodes always take the side pane; otherwise the lowest section with content climbs up into it.
internal fun detailSidePaneSection(sectionKeys: List<MetaScreenSectionKey>): MetaScreenSectionKey? =
    if (MetaScreenSectionKey.EPISODES in sectionKeys) {
        MetaScreenSectionKey.EPISODES
    } else {
        sectionKeys.lastOrNull { it !in PrimaryPaneOnlySections }
    }
