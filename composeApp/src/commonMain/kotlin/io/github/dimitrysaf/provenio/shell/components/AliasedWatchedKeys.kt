package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import io.github.dimitrysaf.provenio.core.home.MetaPreview
import io.github.dimitrysaf.provenio.core.watch.watching.application.PosterImdbAliases

/**
 * [keys] that also match the TMDB-only posters among [items], whose IMDb id is looked up as they
 * show. TMDB-built pages (a network's, a person's, More like this) carry "tmdb:" ids while
 * watched titles are kept by IMDb id, so without this their posters never show as watched.
 */
@Composable
internal fun rememberWatchedKeysWithAliases(keys: Set<String>, items: List<MetaPreview>): Set<String> {
    LaunchedEffect(items) { PosterImdbAliases.resolve(items) }
    val aliases by PosterImdbAliases.aliases.collectAsState()
    return remember(keys, aliases) { PosterImdbAliases.withAliases(keys, aliases) }
}
