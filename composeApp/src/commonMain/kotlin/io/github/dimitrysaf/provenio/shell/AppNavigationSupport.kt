package io.github.dimitrysaf.provenio.shell

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import io.github.dimitrysaf.provenio.core.catalog.CatalogRepository
import io.github.dimitrysaf.provenio.shell.screens.collection.CollectionEditorPage
import io.github.dimitrysaf.provenio.core.collection.CollectionEditorRepository
import io.github.dimitrysaf.provenio.core.collection.FolderDetailRepository
import io.github.dimitrysaf.provenio.shell.screens.collection.disposeCollectionEditorPage
import io.github.dimitrysaf.provenio.core.home.MetaPreview
import io.github.dimitrysaf.provenio.core.library.LibraryItem
import io.github.dimitrysaf.provenio.core.playback.ExternalPlayerPlaybackRequest
import io.github.dimitrysaf.provenio.core.playback.PlayerLaunch
import io.github.dimitrysaf.provenio.core.playback.PlayerLaunchStore
import io.github.dimitrysaf.provenio.core.streams.StreamLaunchStore
import io.github.dimitrysaf.provenio.core.streams.StreamsRepository
import io.github.dimitrysaf.provenio.shell.nav.*
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

internal val navigationSavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(TabsRoute::class, TabsRoute.serializer())
            subclass(DetailRoute::class, DetailRoute.serializer())
            subclass(PersonDetailRoute::class, PersonDetailRoute.serializer())
            subclass(EntityBrowseRoute::class, EntityBrowseRoute.serializer())
            subclass(SettingsPageRoute::class, SettingsPageRoute.serializer())
            subclass(HomescreenSettingsRoute::class, HomescreenSettingsRoute.serializer())
            subclass(MetaScreenSettingsRoute::class, MetaScreenSettingsRoute.serializer())
            subclass(ContinueWatchingSettingsRoute::class, ContinueWatchingSettingsRoute.serializer())
            subclass(DownloadsSettingsRoute::class, DownloadsSettingsRoute.serializer())
            subclass(DownloadShowRoute::class, DownloadShowRoute.serializer())
            subclass(AddonsSettingsRoute::class, AddonsSettingsRoute.serializer())
            subclass(PluginsSettingsRoute::class, PluginsSettingsRoute.serializer())
            subclass(SupportersContributorsSettingsRoute::class, SupportersContributorsSettingsRoute.serializer())
            subclass(LicensesAttributionsSettingsRoute::class, LicensesAttributionsSettingsRoute.serializer())
            subclass(CollectionsRoute::class, CollectionsRoute.serializer())
            subclass(CollectionEditorRoute::class, CollectionEditorRoute.serializer())
            subclass(CollectionEditorPageRoute::class, CollectionEditorPageRoute.serializer())
            subclass(FolderDetailRoute::class, FolderDetailRoute.serializer())
            subclass(StreamRoute::class, StreamRoute.serializer())
            subclass(CatalogRoute::class, CatalogRoute.serializer())
            subclass(PlayerRoute::class, PlayerRoute.serializer())
        }
    }
}

internal fun disposeRouteResources(route: AppRoute) {
    when (route) {
        is StreamRoute -> {
            StreamsRepository.clear()
            StreamLaunchStore.remove(route.launchId)
        }

        is PlayerRoute -> {
            PlayerLaunchStore.remove(route.launchId)
        }

        is CatalogRoute -> {
            CatalogRepository.clear()
            CatalogLaunchStore.remove(route.launchId)
        }

        is CollectionEditorRoute -> CollectionEditorRepository.clear()
        is CollectionEditorPageRoute -> {
            runCatching { CollectionEditorPage.valueOf(route.pageName) }
                .getOrNull()
                ?.let(::disposeCollectionEditorPage)
        }
        is FolderDetailRoute -> FolderDetailRepository.clear()
        else -> Unit
    }
}

internal data class PosterActionTarget(
    val preview: MetaPreview,
    val libraryItem: LibraryItem? = null,
    val libraryListKey: String? = null,
)

internal fun PlayerLaunch.toExternalPlayerPlaybackRequest(): ExternalPlayerPlaybackRequest =
    ExternalPlayerPlaybackRequest(
        sourceUrl = sourceUrl,
        title = title,
        streamTitle = streamTitle,
        sourceHeaders = sourceHeaders,
        resumePositionMs = initialPositionMs,
        season = seasonNumber,
        episode = episodeNumber,
        episodeTitle = episodeTitle,
    )
