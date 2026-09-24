package com.nuvio.app

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.nuvio.app.core.auth.AuthStorage
import com.nuvio.app.core.network.ServerConfigurationStorage
import com.nuvio.app.core.diagnostics.SentryInitializer
import com.nuvio.app.core.deeplink.handleAppUrl
import com.nuvio.app.core.storage.PlatformLocalAccountDataCleaner
import com.nuvio.app.core.sync.SyncClientIdentityStorage
import com.nuvio.app.core.addons.AddonHttpClientProvider
import com.nuvio.app.core.addons.AddonStorage
import com.nuvio.app.core.collection.CollectionMobileSettingsStorage
import com.nuvio.app.core.collection.CollectionStorage
import com.nuvio.app.core.debrid.DebridSettingsStorage
import com.nuvio.app.core.downloads.DownloadsLiveStatusPlatform
import com.nuvio.app.core.downloads.DownloadsPlatformDownloader
import com.nuvio.app.core.downloads.DownloadsStorage
import com.nuvio.app.core.library.LibraryDisplaySettingsStorage
import com.nuvio.app.core.membership.MemberAssetStorage
import com.nuvio.app.core.library.LibraryStorage
import com.nuvio.app.core.metadata.MetaScreenSettingsStorage
import com.nuvio.app.core.home.HomeCatalogSettingsStorage
import com.nuvio.app.core.metadata.mdblist.MdbListSettingsStorage
import com.nuvio.app.core.notifications.EpisodeReleaseNotificationPlatform
import com.nuvio.app.core.notifications.EpisodeReleaseNotificationsStorage
import com.nuvio.app.shell.screens.player.PlayerSettingsStorage
import com.nuvio.app.core.playback.PlayerTrackPreferenceStorage
import com.nuvio.app.core.playback.ExternalPlayerPlatform
import com.nuvio.app.core.playback.SubtitleFileCache
import com.nuvio.app.shell.screens.player.PlayerPictureInPictureManager
import com.nuvio.app.shell.screens.player.PipRemoteActionReceiver
import com.nuvio.app.core.p2p.P2pSettingsStorage
import com.nuvio.app.core.p2p.P2pStreamingEngine
import com.nuvio.app.core.plugins.PluginStorage
import com.nuvio.app.core.profiles.AvatarStorage
import com.nuvio.app.core.profiles.ProfilePinCacheStorage
import com.nuvio.app.core.profiles.ProfileStorage
import com.nuvio.app.core.metadata.SeasonViewModeStorage
import com.nuvio.app.core.search.DiscoverSelectionStorage
import com.nuvio.app.core.search.SearchHistoryStorage
import com.nuvio.app.core.settings.SentrySettingsStorage
import com.nuvio.app.core.settings.AppIconPlatform
import com.nuvio.app.core.settings.ThemeSettingsStorage
import com.nuvio.app.core.tracking.trakt.TraktAuthStorage
import com.nuvio.app.core.tracking.trakt.TraktCommentsStorage
import com.nuvio.app.core.tracking.trakt.TraktLibraryStorage
import com.nuvio.app.core.tracking.trakt.TraktSettingsStorage
import com.nuvio.app.core.tracking.simkl.SimklAuthStorage
import com.nuvio.app.core.tracking.simkl.SimklSyncStorage
import com.nuvio.app.core.metadata.tmdb.TmdbSettingsStorage
import com.nuvio.app.core.updater.AndroidAppUpdaterPlatform
import com.nuvio.app.shell.components.PlatformToast
import com.nuvio.app.core.settings.PosterCardStyleStorage
import com.nuvio.app.core.watch.watched.WatchedStorage
import com.nuvio.app.core.streams.StreamLinkCacheStorage
import com.nuvio.app.core.streams.StreamBadgeSettingsStorage
import com.nuvio.app.core.streams.BingeGroupCacheStorage
import com.nuvio.app.core.watch.progress.ContinueWatchingEnrichmentStorage
import com.nuvio.app.core.watch.progress.ContinueWatchingPreferencesStorage
import com.nuvio.app.core.watch.progress.WatchProgressStorage
import com.nuvio.app.shell.App

open class MainActivity : AppCompatActivity() {
    private var pipRemoteActionReceiver: PipRemoteActionReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.dark(
                scrim = 0xFF020404.toInt(),
            ),
        )
        ThemeSettingsStorage.initialize(applicationContext)
        AppIconPlatform.initialize(applicationContext)
        PlatformToast.initialize(applicationContext)
        SentrySettingsStorage.initialize(applicationContext)
        SentryInitializer.start(application)
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(R.color.nuvio_background)
        pipRemoteActionReceiver = PipRemoteActionReceiver.register(this)
        SyncClientIdentityStorage.initialize(applicationContext)
        AddonHttpClientProvider.initialize(applicationContext)
        AddonStorage.initialize(applicationContext)
        AuthStorage.initialize(applicationContext)
        ServerConfigurationStorage.initialize(applicationContext)
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
