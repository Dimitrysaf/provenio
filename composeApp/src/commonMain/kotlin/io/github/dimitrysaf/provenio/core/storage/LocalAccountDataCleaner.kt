package io.github.dimitrysaf.provenio.core.storage

import io.github.dimitrysaf.provenio.core.build.AppFeaturePolicy
import io.github.dimitrysaf.provenio.core.sync.SyncManager
import io.github.dimitrysaf.provenio.core.sync.ProfileSettingsSync
import io.github.dimitrysaf.provenio.core.tracking.ensureTrackingProvidersRegistered
import io.github.dimitrysaf.provenio.core.addons.AddonRepository
import io.github.dimitrysaf.provenio.core.catalog.CatalogRepository
import io.github.dimitrysaf.provenio.core.collection.CollectionMobileSettingsRepository
import io.github.dimitrysaf.provenio.core.collection.CollectionRepository
import io.github.dimitrysaf.provenio.core.metadata.MetaDetailsRepository
import io.github.dimitrysaf.provenio.core.metadata.MetaScreenSettingsRepository
import io.github.dimitrysaf.provenio.core.home.HomeCatalogSettingsRepository
import io.github.dimitrysaf.provenio.core.home.HomeRepository
import io.github.dimitrysaf.provenio.core.library.LibraryRepository
import io.github.dimitrysaf.provenio.core.membership.MemberAccessRepository
import io.github.dimitrysaf.provenio.core.library.LibraryDisplaySettingsRepository
import io.github.dimitrysaf.provenio.core.notifications.EpisodeReleaseNotificationsRepository
import io.github.dimitrysaf.provenio.core.playback.PlayerLaunchStore
import io.github.dimitrysaf.provenio.core.playback.PlayerSettingsRepository
import io.github.dimitrysaf.provenio.core.p2p.P2pSettingsRepository
import io.github.dimitrysaf.provenio.core.plugins.PluginRepository
import io.github.dimitrysaf.provenio.core.playback.SubtitleRepository
import io.github.dimitrysaf.provenio.core.profiles.ProfileRepository
import io.github.dimitrysaf.provenio.core.profiles.MAX_PROFILES
import io.github.dimitrysaf.provenio.core.search.SearchRepository
import io.github.dimitrysaf.provenio.core.settings.ThemeSettingsRepository
import io.github.dimitrysaf.provenio.core.streams.StreamContextStore
import io.github.dimitrysaf.provenio.core.streams.StreamBadgeSettingsRepository
import io.github.dimitrysaf.provenio.core.streams.StreamLaunchStore
import io.github.dimitrysaf.provenio.core.streams.StreamsRepository
import io.github.dimitrysaf.provenio.core.tracking.TrackingProviderRegistry
import io.github.dimitrysaf.provenio.core.tracking.TrackingSettingsRepository
import io.github.dimitrysaf.provenio.core.settings.PosterCardStyleRepository
import io.github.dimitrysaf.provenio.core.watch.progress.ContinueWatchingPreferencesRepository
import io.github.dimitrysaf.provenio.core.watch.progress.ContinueWatchingEnrichmentCache
import io.github.dimitrysaf.provenio.core.watch.progress.WatchProgressRepository
import io.github.dimitrysaf.provenio.core.watch.progress.WatchProgressSourceCoordinator
import io.github.dimitrysaf.provenio.core.watch.watched.WatchedRepository

internal object LocalAccountDataCleaner {
    fun wipe() {
        ensureTrackingProvidersRegistered()
        TrackingProviderRegistry.removeStoredProfiles(1..MAX_PROFILES)
        SyncManager.cancelAccountSync()
        WatchProgressSourceCoordinator.clearLocalState()
        ProfileSettingsSync.clearAccountState()
        ContinueWatchingEnrichmentCache.clearLocalState()
        WatchProgressRepository.clearLocalState()
        WatchedRepository.clearLocalState()
        LibraryRepository.runAccountStorageWipe {
            wipePlatformStorage()
        }

        ProfileRepository.clearInMemory()
        MemberAccessRepository.clearLocalState()
        AddonRepository.clearLocalState()
        if (AppFeaturePolicy.pluginsEnabled) {
            PluginRepository.clearLocalState()
        }
        HomeRepository.clear()
        HomeCatalogSettingsRepository.clearLocalState()
        MetaScreenSettingsRepository.clearLocalState()
        LibraryRepository.clearLocalState()
        LibraryDisplaySettingsRepository.clearLocalState()
        ContinueWatchingPreferencesRepository.clearLocalState()
        EpisodeReleaseNotificationsRepository.clearLocalState()
        CollectionMobileSettingsRepository.clearLocalState()
        CollectionRepository.clearLocalState()
        ThemeSettingsRepository.clearLocalState()
        PosterCardStyleRepository.clearLocalState()
        TrackingProviderRegistry.clearLocalState()
        TrackingSettingsRepository.clearLocalState()
        PlayerSettingsRepository.clearLocalState()
        StreamBadgeSettingsRepository.clearLocalState()
        P2pSettingsRepository.clearLocalState()
        CatalogRepository.clear()
        StreamsRepository.clear()
        MetaDetailsRepository.clear()
        SearchRepository.reset()
        SubtitleRepository.clear()
        PlayerLaunchStore.clear()
        StreamLaunchStore.clear()
        StreamContextStore.clear()
    }

    internal fun wipePlatformStorage(wipeStorage: () -> Unit = PlatformLocalAccountDataCleaner::wipe) {
        try {
            wipeStorage()
        } finally {
            ContinueWatchingEnrichmentCache.clearLocalState()
        }
    }
}

internal expect object PlatformLocalAccountDataCleaner {
    fun wipe()
}
