package io.github.dimitrysaf.provenio.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.provenio.core.p2p.P2pPlaybackOwner
import io.github.dimitrysaf.provenio.core.playback.PlayerLaunch
import io.github.dimitrysaf.provenio.core.playback.PlayerLaunchStore
import io.github.dimitrysaf.provenio.shell.nav.Navigator
import io.github.dimitrysaf.provenio.shell.nav.PlayerRoute
import io.github.dimitrysaf.provenio.shell.nav.StreamRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal object DetachedPlayer {
    private val _route = MutableStateFlow<PlayerRoute?>(null)
    val route: StateFlow<PlayerRoute?> = _route.asStateFlow()

    fun popOut(launch: PlayerLaunch) {
        P2pPlaybackOwner.claim()
        _route.value = PlayerRoute(launchId = PlayerLaunchStore.put(launch), title = launch.title)
    }

    fun close(route: PlayerRoute) {
        if (_route.value == route) _route.value = null
    }

    fun closeAny() {
        _route.value = null
    }
}

internal fun Navigator.openPlayer(route: PlayerRoute, replaceStreamRoute: Boolean = false) {
    DetachedPlayer.closeAny()
    navigate(route) {
        if (replaceStreamRoute) {
            popUpTo<StreamRoute> { inclusive = true }
        }
    }
}

internal fun Navigator.moveDetachedPlayerToMainWindow(launch: PlayerLaunch) {
    P2pPlaybackOwner.claim()
    openPlayer(PlayerRoute(launchId = PlayerLaunchStore.put(launch), title = launch.title))
}

@Composable
internal fun DetachedPlayerHost(
    navController: Navigator,
    playback: AppPlayback,
    externalPlayerId: String?,
    profileId: Int,
) {
    var lastProfileId by remember { mutableStateOf(profileId) }
    LaunchedEffect(profileId) {
        if (profileId != lastProfileId) {
            lastProfileId = profileId
            DetachedPlayer.closeAny()
        }
    }
    val detachedRoute by DetachedPlayer.route.collectAsStateWithLifecycle()
    val route = detachedRoute ?: return
    key(route.launchId) {
        DetachedPlayerWindow(
            title = route.title,
            onCloseRequest = { DetachedPlayer.close(route) },
        ) {
            DisposableEffect(route) {
                onDispose { disposeRouteResources(route) }
            }
            PlayerDestination(
                route = route,
                navController = navController,
                externalPlayerId = externalPlayerId,
                externalPlayerNotConfiguredText = playback.strings.externalPlayerNotConfigured,
                externalPlayerFailedText = playback.strings.externalPlayerFailed,
                onExternalPlayerLaunch = playback.recordExternalLaunch,
                launchExternalPlayer = playback.launchExternalPlayer,
                openExternalStreamUrl = playback::openExternalStreamUrl,
                onClose = { DetachedPlayer.close(route) },
                onMoveWindow = navController::moveDetachedPlayerToMainWindow,
                inSeparateWindow = true,
            )
        }
    }
}

internal expect val playerWindowSupported: Boolean

@Composable
internal expect fun DetachedPlayerWindow(
    title: String,
    onCloseRequest: () -> Unit,
    content: @Composable () -> Unit,
)
