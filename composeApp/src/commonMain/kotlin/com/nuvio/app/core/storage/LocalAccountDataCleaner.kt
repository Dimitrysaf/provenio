package com.nuvio.app.core.storage

import com.nuvio.app.core.build.AppFeaturePolicy
import com.nuvio.app.core.sync.SyncManager
import com.nuvio.app.core.sync.ProfileSettingsSync
import com.nuvio.app.core.tracking.ensureTrackingProvidersRegistered
import com.nuvio.app.core.addons.AddonRepository
import com.nuvio.app.features.catalog.CatalogRepository
import com.nuvio.app.core.collection.CollectionMobileSettingsRepository
import com.nuvio.app.core.collection.CollectionRepository
import com.nuvio.app.features.details.MetaDetailsRepository
import com.nuvio.app.core.metadata.MetaScreenSettingsRepository
import com.nuvio.app.features.home.HomeCatalogSettingsRepository
import com.nuvio.app.features.home.HomeRepository
import com.nuvio.app.core.library.LibraryRepository
import com.nuvio.app.core.membership.MemberAccessRepository
import com.nuvio.app.core.library.LibraryDisplaySettingsRepository
import com.nuvio.app.core.notifications.EpisodeReleaseNotificationsRepository
import com.nuvio.app.features.player.PlayerLaunchStore
import com.nuvio.app.features.player.PlayerSettingsRepository
import com.nuvio.app.core.p2p.P2pSettingsRepository
import com.nuvio.app.core.plugins.PluginRepository
import com.nuvio.app.features.player.SubtitleRepository
import com.nuvio.app.features.profiles.ProfileRepository
import com.nuvio.app.core.profiles.MAX_PROFILES
import com.nuvio.app.features.search.SearchRepository
import com.nuvio.app.features.settings.ThemeSettingsRepository
import com.nuvio.app.core.streams.StreamContextStore
import com.nuvio.app.core.streams.StreamBadgeSettingsRepository
import com.nuvio.app.core.streams.StreamLaunchStore
import com.nuvio.app.features.streams.StreamsRepository
import com.nuvio.app.core.tracking.TrackingProviderRegistry
import com.nuvio.app.core.tracking.TrackingSettingsRepository
import com.nuvio.app.core.settings.PosterCardStyleRepository
import com.nuvio.app.core.watch.progress.ContinueWatchingPreferencesRepository
import com.nuvio.app.core.watch.progress.ContinueWatchingEnrichmentCache
import com.nuvio.app.features.watchprogress.WatchProgressRepository
import com.nuvio.app.core.watch.progress.WatchProgressSourceCoordinator
import com.nuvio.app.core.watch.watched.WatchedRepository

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
