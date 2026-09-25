package io.github.dimitrysaf.provenio

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import io.github.dimitrysaf.provenio.core.auth.AuthRepository
import io.github.dimitrysaf.provenio.core.auth.AuthState
import io.github.dimitrysaf.provenio.core.auth.AuthStorage
import io.github.dimitrysaf.provenio.core.diagnostics.SentryInitializer
import io.github.dimitrysaf.provenio.core.deeplink.handleAppUrl
import io.github.dimitrysaf.provenio.core.storage.PlatformLocalAccountDataCleaner
import io.github.dimitrysaf.provenio.core.sync.SyncClientIdentityStorage
import io.github.dimitrysaf.provenio.core.addons.AddonHttpClientProvider
import io.github.dimitrysaf.provenio.core.addons.AddonStorage
import io.github.dimitrysaf.provenio.core.collection.CollectionMobileSettingsStorage
import io.github.dimitrysaf.provenio.core.collection.CollectionStorage
import io.github.dimitrysaf.provenio.core.debrid.DebridSettingsStorage
import io.github.dimitrysaf.provenio.core.downloads.DownloadsLiveStatusPlatform
import io.github.dimitrysaf.provenio.core.downloads.DownloadsPlatformDownloader
import io.github.dimitrysaf.provenio.core.downloads.DownloadsStorage
import io.github.dimitrysaf.provenio.core.library.LibraryDisplaySettingsStorage
import io.github.dimitrysaf.provenio.core.membership.MemberAssetStorage
import io.github.dimitrysaf.provenio.core.library.LibraryStorage
import io.github.dimitrysaf.provenio.core.metadata.MetaScreenSettingsStorage
import io.github.dimitrysaf.provenio.core.home.HomeCatalogSettingsStorage
import io.github.dimitrysaf.provenio.core.metadata.mdblist.MdbListSettingsStorage
import io.github.dimitrysaf.provenio.core.notifications.EpisodeReleaseNotificationPlatform
import io.github.dimitrysaf.provenio.core.notifications.EpisodeReleaseNotificationsStorage
import io.github.dimitrysaf.provenio.core.playback.PlayerSettingsStorage
import io.github.dimitrysaf.provenio.core.playback.PlayerTrackPreferenceStorage
import io.github.dimitrysaf.provenio.core.playback.ExternalPlayerPlatform
import io.github.dimitrysaf.provenio.core.playback.SubtitleFileCache
import io.github.dimitrysaf.provenio.shell.screens.player.PlayerPictureInPictureManager
import io.github.dimitrysaf.provenio.shell.screens.player.PipRemoteActionReceiver
import io.github.dimitrysaf.provenio.core.p2p.P2pSettingsStorage
import io.github.dimitrysaf.provenio.core.p2p.P2pStreamingEngine
import io.github.dimitrysaf.provenio.core.plugins.PluginStorage
import io.github.dimitrysaf.provenio.core.profiles.AvatarStorage
import io.github.dimitrysaf.provenio.core.profiles.ProfilePinCacheStorage
import io.github.dimitrysaf.provenio.core.profiles.ProfileRepository
import io.github.dimitrysaf.provenio.core.profiles.ProfileStorage
import io.github.dimitrysaf.provenio.core.metadata.SeasonViewModeStorage
import io.github.dimitrysaf.provenio.core.search.DiscoverSelectionStorage
import io.github.dimitrysaf.provenio.core.search.SearchHistoryStorage
import io.github.dimitrysaf.provenio.core.settings.SentrySettingsStorage
import io.github.dimitrysaf.provenio.core.settings.AppIconPlatform
import io.github.dimitrysaf.provenio.core.settings.ThemeSettingsStorage
import io.github.dimitrysaf.provenio.core.tracking.trakt.TraktAuthStorage
import io.github.dimitrysaf.provenio.core.tracking.trakt.TraktCommentsStorage
import io.github.dimitrysaf.provenio.core.tracking.trakt.TraktLibraryStorage
import io.github.dimitrysaf.provenio.core.tracking.trakt.TraktSettingsStorage
import io.github.dimitrysaf.provenio.core.tracking.simkl.SimklAuthStorage
import io.github.dimitrysaf.provenio.core.tracking.simkl.SimklSyncStorage
import io.github.dimitrysaf.provenio.core.metadata.tmdb.TmdbSettingsStorage
import io.github.dimitrysaf.provenio.core.updater.AndroidAppUpdaterPlatform
import io.github.dimitrysaf.provenio.core.settings.PosterCardStyleStorage
import io.github.dimitrysaf.provenio.core.watch.watched.WatchedStorage
import io.github.dimitrysaf.provenio.core.streams.StreamLinkCacheStorage
import io.github.dimitrysaf.provenio.core.streams.StreamBadgeSettingsStorage
import io.github.dimitrysaf.provenio.core.streams.BingeGroupCacheStorage
import io.github.dimitrysaf.provenio.core.watch.progress.ContinueWatchingEnrichmentStorage
import io.github.dimitrysaf.provenio.core.watch.progress.ContinueWatchingPreferencesStorage
import io.github.dimitrysaf.provenio.core.watch.progress.WatchProgressStorage
import io.github.dimitrysaf.provenio.shell.App

open class MainActivity : AppCompatActivity() {
    private var pipRemoteActionReceiver: PipRemoteActionReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // The splash stays until sign-in resolves, unless cached profiles let the gate open sooner.
        installSplashScreen().setKeepOnScreenCondition {
            AuthRepository.state.value is AuthState.Loading &&
                ProfileRepository.state.value.profiles.isEmpty()
        }
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.dark(
                scrim = 0xFF020404.toInt(),
            ),
        )
        ThemeSettingsStorage.initialize(applicationContext)
        AppIconPlatform.initialize(applicationContext)
        SentrySettingsStorage.initialize(applicationContext)
        SentryInitializer.start(application)
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(R.color.provenio_background)
        pipRemoteActionReceiver = PipRemoteActionReceiver.register(this)
        SyncClientIdentityStorage.initialize(applicationContext)
        AddonHttpClientProvider.initialize(applicationContext)
        AddonStorage.initialize(applicationContext)
        AuthStorage.initialize(applicationContext)
        LibraryStorage.initialize(applicationContext)
        WatchedStorage.initialize(applicationContext)
        MetaScreenSettingsStorage.initialize(applicationContext)
        HomeCatalogSettingsStorage.initialize(applicationContext)
        PlayerSettingsStorage.initialize(applicationContext)
        PlayerTrackPreferenceStorage.initialize(applicationContext)
        P2pSettingsStorage.initialize(applicationContext)
        P2pStreamingEngine.initialize(applicationContext)
        ExternalPlayerPlatform.initialize(applicationContext)
        SubtitleFileCache.initialize(applicationContext)
        ProfileStorage.initialize(applicationContext)
        AvatarStorage.initialize(applicationContext)
        ProfilePinCacheStorage.initialize(applicationContext)
        MemberAssetStorage.initialize(applicationContext)
        DiscoverSelectionStorage.initialize(applicationContext)
        SearchHistoryStorage.initialize(applicationContext)
        SeasonViewModeStorage.initialize(applicationContext)
        PosterCardStyleStorage.initialize(applicationContext)
        DebridSettingsStorage.initialize(applicationContext)
        TmdbSettingsStorage.initialize(applicationContext)
        MdbListSettingsStorage.initialize(applicationContext)
        TraktAuthStorage.initialize(applicationContext)
        TraktCommentsStorage.initialize(applicationContext)
        TraktLibraryStorage.initialize(applicationContext)
        TraktSettingsStorage.initialize(applicationContext)
        SimklAuthStorage.initialize(applicationContext)
        SimklSyncStorage.initialize(applicationContext)
        LibraryDisplaySettingsStorage.initialize(applicationContext)
        ContinueWatchingPreferencesStorage.initialize(applicationContext)
        ContinueWatchingEnrichmentStorage.initialize(applicationContext)
        EpisodeReleaseNotificationsStorage.initialize(applicationContext)
        WatchProgressStorage.initialize(applicationContext)
        StreamLinkCacheStorage.initialize(applicationContext)
        StreamBadgeSettingsStorage.initialize(applicationContext)
        BingeGroupCacheStorage.initialize(applicationContext)
        PluginStorage.initialize(applicationContext)
        CollectionMobileSettingsStorage.initialize(applicationContext)
        CollectionStorage.initialize(applicationContext)
        DownloadsStorage.initialize(applicationContext)
        DownloadsPlatformDownloader.initialize(applicationContext)
        DownloadsLiveStatusPlatform.initialize(applicationContext)
        AndroidAppUpdaterPlatform.initialize(applicationContext)
        PlatformLocalAccountDataCleaner.initialize(applicationContext)
        EpisodeReleaseNotificationPlatform.initialize(applicationContext)
        EpisodeReleaseNotificationPlatform.bindActivity(this)
        handleIncomingAppIntent(intent)

        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingAppIntent(intent)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        PlayerPictureInPictureManager.onUserLeaveHint(this)
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        PlayerPictureInPictureManager.onPictureInPictureModeChanged(this, isInPictureInPictureMode)
    }

    override fun onDestroy() {
        EpisodeReleaseNotificationPlatform.unbindActivity(this)
        val receiver = pipRemoteActionReceiver
        if (receiver != null) {
            runCatching { unregisterReceiver(receiver) }
            pipRemoteActionReceiver = null
        }
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        if (EpisodeReleaseNotificationPlatform.handlePermissionRequestResult(requestCode, grantResults)) {
            return
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun handleIncomingAppIntent(intent: Intent?) {
        val appUrl = intent?.dataString?.trim().orEmpty()
        if (appUrl.isBlank()) return
        handleAppUrl(appUrl)
    }
}
