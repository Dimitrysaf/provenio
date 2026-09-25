package io.github.dimitrysaf.provenio

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.dimitrysaf.provenio.core.addons.AddonHttpClientProvider
import io.github.dimitrysaf.provenio.core.addons.AddonStorage
import io.github.dimitrysaf.provenio.core.auth.AuthStorage
import io.github.dimitrysaf.provenio.core.collection.CollectionMobileSettingsStorage
import io.github.dimitrysaf.provenio.core.collection.CollectionStorage
import io.github.dimitrysaf.provenio.core.debrid.DebridSettingsStorage
import io.github.dimitrysaf.provenio.core.deeplink.handleAppUrl
import io.github.dimitrysaf.provenio.core.downloads.DownloadsStorage
import io.github.dimitrysaf.provenio.core.home.HomeCatalogSettingsStorage
import io.github.dimitrysaf.provenio.core.library.LibraryDisplaySettingsStorage
import io.github.dimitrysaf.provenio.core.library.LibraryStorage
import io.github.dimitrysaf.provenio.core.membership.MemberAssetStorage
import io.github.dimitrysaf.provenio.core.metadata.MetaScreenSettingsStorage
import io.github.dimitrysaf.provenio.core.metadata.SeasonViewModeStorage
import io.github.dimitrysaf.provenio.core.metadata.mdblist.MdbListSettingsStorage
import io.github.dimitrysaf.provenio.core.metadata.tmdb.TmdbSettingsStorage
import io.github.dimitrysaf.provenio.core.network.ServerConfigurationStorage
import io.github.dimitrysaf.provenio.core.notifications.EpisodeReleaseNotificationsStorage
import io.github.dimitrysaf.provenio.core.p2p.P2pSettingsStorage
import io.github.dimitrysaf.provenio.core.p2p.P2pStreamingEngine
import io.github.dimitrysaf.provenio.core.playback.PlayerSettingsStorage
import io.github.dimitrysaf.provenio.core.playback.PlayerTrackPreferenceStorage
import io.github.dimitrysaf.provenio.core.profiles.AvatarStorage
import io.github.dimitrysaf.provenio.core.profiles.ProfilePinCacheStorage
import io.github.dimitrysaf.provenio.core.profiles.ProfileStorage
import io.github.dimitrysaf.provenio.core.search.DiscoverSelectionStorage
import io.github.dimitrysaf.provenio.core.search.SearchHistoryStorage
import io.github.dimitrysaf.provenio.core.settings.PosterCardStyleStorage
import io.github.dimitrysaf.provenio.core.settings.SentrySettingsStorage
import io.github.dimitrysaf.provenio.core.settings.ThemeSettingsStorage
import io.github.dimitrysaf.provenio.core.storage.PlatformLocalAccountDataCleaner
import io.github.dimitrysaf.provenio.core.streams.BingeGroupCacheStorage
import io.github.dimitrysaf.provenio.core.streams.StreamBadgeSettingsStorage
import io.github.dimitrysaf.provenio.core.streams.StreamLinkCacheStorage
import io.github.dimitrysaf.provenio.core.sync.SyncClientIdentityStorage
import io.github.dimitrysaf.provenio.core.tracking.simkl.SimklAuthStorage
import io.github.dimitrysaf.provenio.core.tracking.simkl.SimklSyncStorage
import io.github.dimitrysaf.provenio.core.tracking.trakt.TraktAuthStorage
import io.github.dimitrysaf.provenio.core.tracking.trakt.TraktCommentsStorage
import io.github.dimitrysaf.provenio.core.tracking.trakt.TraktLibraryStorage
import io.github.dimitrysaf.provenio.core.tracking.trakt.TraktSettingsStorage
import io.github.dimitrysaf.provenio.core.watch.progress.ContinueWatchingEnrichmentStorage
import io.github.dimitrysaf.provenio.core.watch.progress.ContinueWatchingPreferencesStorage
import io.github.dimitrysaf.provenio.core.watch.progress.WatchProgressStorage
import io.github.dimitrysaf.provenio.core.watch.watched.WatchedStorage
import io.github.dimitrysaf.provenio.desktop.Context
import io.github.dimitrysaf.provenio.desktop.DesktopWindowState
import io.github.dimitrysaf.provenio.shell.App
import org.jetbrains.compose.resources.painterResource
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.app_icon_original

fun main(args: Array<String>) {
    initializePlatform(Context.app)
    // A provenio:// link the desktop entry was opened with.
    args.firstOrNull { it.startsWith("provenio:") || it.startsWith("stremio:") }?.let(::handleAppUrl)
    application {
        val windowState = rememberWindowState(size = DpSize(1280.dp, 800.dp))
        val fullscreen by DesktopWindowState.isFullscreen.collectAsState()
        LaunchedEffect(fullscreen) {
            windowState.placement = if (fullscreen) WindowPlacement.Fullscreen else WindowPlacement.Floating
        }
        LaunchedEffect(windowState.isMinimized) {
            DesktopWindowState.setVisible(!windowState.isMinimized)
        }
        Window(
            onCloseRequest = {
                P2pStreamingEngine.shutdown()
                exitApplication()
            },
            state = windowState,
            title = "Provenio",
            icon = painterResource(Res.drawable.app_icon_original),
            onPreviewKeyEvent = { event ->
                when {
                    event.type != KeyEventType.KeyDown -> false
                    event.key == Key.F11 -> {
                        DesktopWindowState.setFullscreen(!fullscreen)
                        true
                    }
                    event.key == Key.Escape && fullscreen -> {
                        DesktopWindowState.setFullscreen(false)
                        true
                    }
                    else -> false
                }
            },
        ) {
            App()
        }
    }
}

/** The desktop counterpart of MainActivity's storage set-up. */
private fun initializePlatform(context: Context) {
    ThemeSettingsStorage.initialize(context)
    SentrySettingsStorage.initialize(context)
    SyncClientIdentityStorage.initialize(context)
    AddonHttpClientProvider.initialize(context)
    AddonStorage.initialize(context)
    AuthStorage.initialize(context)
    ServerConfigurationStorage.initialize(context)
    LibraryStorage.initialize(context)
    WatchedStorage.initialize(context)
    MetaScreenSettingsStorage.initialize(context)
    HomeCatalogSettingsStorage.initialize(context)
    PlayerSettingsStorage.initialize(context)
    PlayerTrackPreferenceStorage.initialize(context)
    P2pSettingsStorage.initialize(context)
    P2pStreamingEngine.initialize(context)
    ProfileStorage.initialize(context)
    AvatarStorage.initialize(context)
    ProfilePinCacheStorage.initialize(context)
    MemberAssetStorage.initialize(context)
    DiscoverSelectionStorage.initialize(context)
    SearchHistoryStorage.initialize(context)
    SeasonViewModeStorage.initialize(context)
    PosterCardStyleStorage.initialize(context)
    DebridSettingsStorage.initialize(context)
    TmdbSettingsStorage.initialize(context)
    MdbListSettingsStorage.initialize(context)
    TraktAuthStorage.initialize(context)
    TraktCommentsStorage.initialize(context)
    TraktLibraryStorage.initialize(context)
    TraktSettingsStorage.initialize(context)
    SimklAuthStorage.initialize(context)
    SimklSyncStorage.initialize(context)
    LibraryDisplaySettingsStorage.initialize(context)
    ContinueWatchingPreferencesStorage.initialize(context)
    ContinueWatchingEnrichmentStorage.initialize(context)
    EpisodeReleaseNotificationsStorage.initialize(context)
    WatchProgressStorage.initialize(context)
    StreamLinkCacheStorage.initialize(context)
    StreamBadgeSettingsStorage.initialize(context)
    BingeGroupCacheStorage.initialize(context)
    CollectionMobileSettingsStorage.initialize(context)
    CollectionStorage.initialize(context)
    DownloadsStorage.initialize(context)
    PlatformLocalAccountDataCleaner.initialize(context)
}
