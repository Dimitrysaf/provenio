package com.nuvio.app.shell.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.shell.components.NuvioLoadingIndicator
import com.nuvio.app.shell.theme.nuvio
import com.nuvio.app.core.profiles.ProfileRepository
import com.nuvio.app.core.tracking.simkl.SimklAuthError
import com.nuvio.app.core.tracking.simkl.SimklAuthRepository
import com.nuvio.app.core.tracking.simkl.SimklAuthUiState
import com.nuvio.app.shell.screens.simkl.SimklBrandAsset
import com.nuvio.app.core.tracking.simkl.SimklConnectionMode
import com.nuvio.app.core.tracking.simkl.SimklSyncRepository
import com.nuvio.app.shell.screens.simkl.simklBrandPainter
import com.nuvio.app.core.tracking.TrackingProviderId
import com.nuvio.app.core.tracking.TrackingRefreshIntent
import com.nuvio.app.core.tracking.trakt.TraktAuthRepository
import com.nuvio.app.core.tracking.trakt.TraktAuthUiState
import com.nuvio.app.core.tracking.trakt.TraktBrandAsset
import com.nuvio.app.core.tracking.trakt.TraktConnectionMode
import com.nuvio.app.shell.screens.trakt.traktBrandPainter
import com.nuvio.app.core.watch.progress.WatchProgressSourceCoordinator
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_cancel
import nuvio.composeapp.generated.resources.action_close
import nuvio.composeapp.generated.resources.settings_simkl_authorization_expired
import nuvio.composeapp.generated.resources.settings_simkl_authorization_revoked
import nuvio.composeapp.generated.resources.settings_simkl_connect
import nuvio.composeapp.generated.resources.settings_simkl_connected_as
import nuvio.composeapp.generated.resources.settings_simkl_connected_description
import nuvio.composeapp.generated.resources.settings_simkl_default_user
import nuvio.composeapp.generated.resources.settings_simkl_disconnect
import nuvio.composeapp.generated.resources.settings_simkl_disconnect_description
import nuvio.composeapp.generated.resources.settings_simkl_finish_sign_in
import nuvio.composeapp.generated.resources.settings_simkl_invalid_callback
import nuvio.composeapp.generated.resources.settings_simkl_missing_credentials
import nuvio.composeapp.generated.resources.settings_simkl_open_login
import nuvio.composeapp.generated.resources.settings_simkl_sign_in_description
import nuvio.composeapp.generated.resources.settings_simkl_sign_in_failed
import nuvio.composeapp.generated.resources.settings_simkl_sync_info_action
import nuvio.composeapp.generated.resources.settings_simkl_sync_now
import nuvio.composeapp.generated.resources.settings_simkl_visit
import nuvio.composeapp.generated.resources.settings_tracking_approval_redirect
import nuvio.composeapp.generated.resources.settings_tracking_disconnect_description
import nuvio.composeapp.generated.resources.settings_tracking_disconnect_title
import nuvio.composeapp.generated.resources.settings_trakt_approval_redirect
import nuvio.composeapp.generated.resources.settings_trakt_connect
import nuvio.composeapp.generated.resources.settings_trakt_connected_as
import nuvio.composeapp.generated.resources.settings_trakt_default_user
import nuvio.composeapp.generated.resources.settings_trakt_disconnect
import nuvio.composeapp.generated.resources.settings_trakt_disconnect_description
import nuvio.composeapp.generated.resources.settings_trakt_failed_open_browser
import nuvio.composeapp.generated.resources.settings_trakt_finish_sign_in
import nuvio.composeapp.generated.resources.settings_trakt_missing_credentials
import nuvio.composeapp.generated.resources.settings_trakt_open_login
import nuvio.composeapp.generated.resources.settings_trakt_save_actions_description
import nuvio.composeapp.generated.resources.settings_trakt_sign_in_description
import org.jetbrains.compose.resources.stringResource
import com.nuvio.app.core.settings.TrackingBrand
import com.nuvio.app.core.settings.TrackingConnectionCardMode
import com.nuvio.app.core.settings.toTrackingConnectionCardMode

@Composable
internal fun TrackingProviderCards(
    isTablet: Boolean,
    traktUiState: TraktAuthUiState,
    simklUiState: SimklAuthUiState,
) {
    val syncState by remember {
        SimklSyncRepository.ensureLoaded()
        SimklSyncRepository.state
    }.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showSyncInfo by rememberSaveable { mutableStateOf(false) }
    val onSimklSyncRequested: () -> Unit = {
        scope.launch {
            WatchProgressSourceCoordinator.refreshProviderAndActiveSource(
                profileId = ProfileRepository.activeProfileId,
                providerId = TrackingProviderId.SIMKL,
                refreshProvider = {
                    SimklSyncRepository.refresh(TrackingRefreshIntent.USER_INITIATED)
                },
            )
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val useTwoColumns = maxWidth >= 600.dp
        if (useTwoColumns) {
            // The two are lists now, each as tall as its own rows, so neither is stretched to
            // match the other: a row with nothing in it would just be a gap.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                TraktProviderCard(
                    uiState = traktUiState,
                    modifier = Modifier.weight(1f),
                )
                SimklProviderCard(
                    uiState = simklUiState,
                    isSyncing = syncState.isLoading,
                    syncErrorMessage = syncState.errorMessage,
                    onSyncRequested = onSimklSyncRequested,
                    onInfoRequested = { showSyncInfo = true },
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(if (isTablet) 16.dp else 12.dp),
            ) {
                TraktProviderCard(
                    uiState = traktUiState,
                    modifier = Modifier.fillMaxWidth(),
                )
                SimklProviderCard(
                    uiState = simklUiState,
                    isSyncing = syncState.isLoading,
                    syncErrorMessage = syncState.errorMessage,
                    onSyncRequested = onSimklSyncRequested,
                    onInfoRequested = { showSyncInfo = true },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (showSyncInfo) {
        SimklSyncInfoDialog(onDismiss = { showSyncInfo = false })
    }
}

@Composable
private fun TraktProviderCard(
    uiState: TraktAuthUiState,
    modifier: Modifier,
) {
    TrackingProviderCard(
        brand = TrackingBrand.TRAKT,
        mode = uiState.mode.toTrackingConnectionCardMode(),
        credentialsConfigured = uiState.credentialsConfigured,
        isLoading = uiState.isLoading,
        connectedLabel = stringResource(
            Res.string.settings_trakt_connected_as,
            uiState.username ?: stringResource(Res.string.settings_trakt_default_user),
        ),
        connectedDescription = stringResource(Res.string.settings_trakt_save_actions_description),
        signInDescription = stringResource(Res.string.settings_trakt_sign_in_description),
        finishSignInLabel = stringResource(Res.string.settings_trakt_finish_sign_in),
        approvalDescription = stringResource(Res.string.settings_trakt_approval_redirect),
        connectLabel = stringResource(Res.string.settings_trakt_connect),
        openLoginLabel = stringResource(Res.string.settings_trakt_open_login),
        disconnectLabel = stringResource(Res.string.settings_trakt_disconnect),
        missingCredentialsMessage = stringResource(Res.string.settings_trakt_missing_credentials),
        statusMessage = uiState.statusMessage.takeUnless {
            uiState.mode == TraktConnectionMode.CONNECTED
        },
        errorMessage = uiState.errorMessage,
        onConnectRequested = TraktAuthRepository::onConnectRequested,
        onResumeAuthorization = {
            TraktAuthRepository.pendingAuthorizationUrl()
                ?: TraktAuthRepository.onConnectRequested()
        },
        onCancelAuthorization = TraktAuthRepository::onCancelAuthorization,
        onDisconnect = TraktAuthRepository::onDisconnectRequested,
        modifier = modifier,
    )
}

@Composable
private fun SimklProviderCard(
    uiState: SimklAuthUiState,
    isSyncing: Boolean,
    syncErrorMessage: String?,
    onSyncRequested: () -> Unit,
    onInfoRequested: () -> Unit,
    modifier: Modifier,
) {
    TrackingProviderCard(
        brand = TrackingBrand.SIMKL,
        mode = uiState.mode.toTrackingConnectionCardMode(),
        credentialsConfigured = uiState.credentialsConfigured,
        isLoading = uiState.isLoading,
        connectedLabel = stringResource(
            Res.string.settings_simkl_connected_as,
            uiState.username ?: stringResource(Res.string.settings_simkl_default_user),
        ),
        connectedDescription = stringResource(Res.string.settings_simkl_connected_description),
        signInDescription = stringResource(Res.string.settings_simkl_sign_in_description),
        finishSignInLabel = stringResource(Res.string.settings_simkl_finish_sign_in),
        approvalDescription = stringResource(Res.string.settings_tracking_approval_redirect),
        connectLabel = stringResource(Res.string.settings_simkl_connect),
        openLoginLabel = stringResource(Res.string.settings_simkl_open_login),
        disconnectLabel = stringResource(Res.string.settings_simkl_disconnect),
        syncLabel = stringResource(Res.string.settings_simkl_sync_now),
        infoLabel = stringResource(Res.string.settings_simkl_sync_info_action),
        isSyncing = isSyncing,
        missingCredentialsMessage = stringResource(Res.string.settings_simkl_missing_credentials),
        errorMessage = simklErrorMessage(uiState.error) ?: syncErrorMessage,
        websiteLabel = stringResource(Res.string.settings_simkl_visit),
        websiteUrl = SIMKL_WEBSITE_URL,
        onConnectRequested = SimklAuthRepository::onConnectRequested,
        onResumeAuthorization = {
            SimklAuthRepository.pendingAuthorizationUrl()
                ?: SimklAuthRepository.onConnectRequested()
        },
        onCancelAuthorization = SimklAuthRepository::onCancelAuthorization,
        onSyncRequested = onSyncRequested,
        onInfoRequested = onInfoRequested,
        onDisconnect = SimklAuthRepository::onDisconnectRequested,
        modifier = modifier,
    )
}

/**
 * One tracking service.
 *
 * The brand artwork is the group's first item, so the gradient reads as the head of the list
 * rather than as a card with controls painted on top of it: everything under it is an ordinary
 * settings row, in the app's own colours.
 */
@Composable
private fun TrackingProviderCard(
    brand: TrackingBrand,
    mode: TrackingConnectionCardMode,
    credentialsConfigured: Boolean,
    isLoading: Boolean,
    connectedLabel: String,
    connectedDescription: String,
    signInDescription: String,
    finishSignInLabel: String,
    approvalDescription: String,
    connectLabel: String,
    openLoginLabel: String,
    disconnectLabel: String,
    missingCredentialsMessage: String,
    modifier: Modifier = Modifier,
    syncLabel: String? = null,
    infoLabel: String? = null,
    isSyncing: Boolean = false,
    statusMessage: String? = null,
    errorMessage: String? = null,
    websiteLabel: String? = null,
    websiteUrl: String? = null,
    onConnectRequested: () -> String?,
    onResumeAuthorization: () -> String?,
    onCancelAuthorization: () -> Unit,
    onSyncRequested: (() -> Unit)? = null,
    onInfoRequested: (() -> Unit)? = null,
    onDisconnect: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val failedOpenBrowserMessage = stringResource(Res.string.settings_trakt_failed_open_browser)
    var noticeMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var awaitingResult by rememberSaveable { mutableStateOf(false) }
    var showDisconnectDialog by rememberSaveable { mutableStateOf(false) }

    // What the service has to say is a reply to pressing something, so it is held until the work
    // settles and then said once, rather than sitting under the button forever.
    LaunchedEffect(isLoading, statusMessage, errorMessage) {
        if (!awaitingResult || isLoading) return@LaunchedEffect
        val message = errorMessage?.takeIf(String::isNotBlank)
            ?: statusMessage?.takeIf(String::isNotBlank)
            ?: return@LaunchedEffect
        noticeMessage = message
        awaitingResult = false
    }

    fun openUrl(url: String?) {
        if (url.isNullOrBlank()) {
            noticeMessage = failedOpenBrowserMessage
            return
        }
        runCatching { uriHandler.openUri(url) }
            .onFailure { noticeMessage = failedOpenBrowserMessage }
    }

    SettingsList(modifier = modifier) {
        shapedRow { shape ->
            TrackingBrandBanner(brand = brand, shape = shape)
        }

        when (mode) {
            TrackingConnectionCardMode.CONNECTED -> {
                infoRow(title = connectedLabel, description = connectedDescription)
                if (syncLabel != null && onSyncRequested != null) {
                    navigationRow(
                        title = syncLabel,
                        icon = Icons.Rounded.Sync,
                        enabled = !isLoading && !isSyncing,
                        trailingContent = if (isSyncing) {
                            { NuvioLoadingIndicator(size = 18.dp) }
                        } else {
                            null
                        },
                        onClick = onSyncRequested,
                    )
                }
                if (infoLabel != null && onInfoRequested != null) {
                    navigationRow(
                        title = infoLabel,
                        icon = Icons.Rounded.Info,
                        onClick = onInfoRequested,
                    )
                }
            }

            TrackingConnectionCardMode.AWAITING_APPROVAL -> {
                infoRow(title = finishSignInLabel, description = approvalDescription)
                navigationRow(
                    title = openLoginLabel,
                    icon = Icons.Rounded.Link,
                    enabled = !isLoading,
                    trailingContent = { OpensInBrowserIcon() },
                    onClick = {
                        awaitingResult = true
                        openUrl(onResumeAuthorization())
                    },
                )
                navigationRow(
                    title = stringResource(Res.string.action_cancel),
                    icon = Icons.Rounded.Close,
                    enabled = !isLoading,
                    onClick = onCancelAuthorization,
                )
            }

            TrackingConnectionCardMode.DISCONNECTED -> {
                navigationRow(
                    title = connectLabel,
                    description = signInDescription,
                    icon = Icons.Rounded.Link,
                    enabled = !isLoading,
                    trailingContent = { OpensInBrowserIcon() },
                    onClick = {
                        // Missing keys are a reason connecting cannot run, not a state of the
                        // service, so they are said when connecting is asked for and not before.
                        if (!credentialsConfigured) {
                            noticeMessage = missingCredentialsMessage
                            return@navigationRow
                        }
                        awaitingResult = true
                        openUrl(onConnectRequested())
                    },
                )
            }
        }

        if (!websiteLabel.isNullOrBlank() && !websiteUrl.isNullOrBlank()) {
            navigationRow(
                title = websiteLabel,
                icon = Icons.Rounded.Language,
                trailingContent = { OpensInBrowserIcon() },
                onClick = { openUrl(websiteUrl) },
            )
        }

        if (mode == TrackingConnectionCardMode.CONNECTED) {
            navigationRow(
                title = disconnectLabel,
                icon = Icons.Rounded.LinkOff,
                enabled = !isLoading && !isSyncing,
                onClick = { showDisconnectDialog = true },
            )
        }
    }

    noticeMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { noticeMessage = null },
            title = { Text(brand.displayName) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { noticeMessage = null }) {
                    Text(stringResource(Res.string.action_close))
                }
            },
        )
    }

    if (showDisconnectDialog) {
        TrackingDisconnectDialog(
            brand = brand,
            onConfirm = {
                showDisconnectDialog = false
                onDisconnect()
            },
            onDismiss = { showDisconnectDialog = false },
        )
    }
}

/** The mark a row carries when pressing it hands the person over to their browser. */
@Composable
private fun OpensInBrowserIcon() {
    Icon(
        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
        contentDescription = null,
    )
}

/** The brand artwork: the gradient, the wordmark, and the glyph watermark behind it. */
@Composable
private fun TrackingBrandBanner(
    brand: TrackingBrand,
    shape: RoundedCornerShape,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(TrackingBannerHeight)
            .clip(shape)
            .background(brand.cardBrush()),
        contentAlignment = Alignment.CenterStart,
    ) {
        TrackingBrandGlyph(
            brand = brand,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
                .size(TrackingBannerGlyphSize)
                .alpha(0.12f),
        )
        TrackingBrandWordmark(
            brand = brand,
            contentDescription = brand.displayName,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
    }
}

@Composable
private fun TrackingDisconnectDialog(
    brand: TrackingBrand,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(Res.string.settings_tracking_disconnect_title, brand.displayName))
        },
        text = {
            Text(
                text = when (brand) {
                    TrackingBrand.TRAKT ->
                        stringResource(Res.string.settings_trakt_disconnect_description)
                    TrackingBrand.SIMKL ->
                        stringResource(Res.string.settings_simkl_disconnect_description)
                    TrackingBrand.NUVIO,
                    TrackingBrand.TMDB,
                    -> stringResource(
                        Res.string.settings_tracking_disconnect_description,
                        brand.displayName,
                    )
                },
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(stringResource(Res.string.settings_trakt_disconnect))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    )
}

@Composable
internal fun TrackingBrandGlyph(
    brand: TrackingBrand,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    when (brand) {
        TrackingBrand.TRAKT -> Image(
            painter = traktBrandPainter(TraktBrandAsset.Glyph),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Fit,
        )
        TrackingBrand.SIMKL -> Image(
            painter = simklBrandPainter(SimklBrandAsset.Glyph),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Fit,
        )
        TrackingBrand.TMDB -> Image(
            painter = integrationLogoPainter(IntegrationLogo.Tmdb),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Fit,
        )
        TrackingBrand.NUVIO -> Icon(
            imageVector = Icons.Rounded.Sync,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = MaterialTheme.nuvio.colors.accent,
        )
    }
}

@Composable
private fun TrackingBrandWordmark(
    brand: TrackingBrand,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val painter: Painter = when (brand) {
        TrackingBrand.TRAKT -> traktBrandPainter(TraktBrandAsset.Wordmark)
        TrackingBrand.SIMKL -> simklBrandPainter(SimklBrandAsset.Wordmark)
        TrackingBrand.NUVIO,
        TrackingBrand.TMDB,
        -> return
    }
    val wordmarkSize = when (brand) {
        TrackingBrand.TRAKT -> Modifier
            .width(86.dp)
            .height(38.dp)
        TrackingBrand.SIMKL -> Modifier
            .width(124.dp)
            .height(30.dp)
        TrackingBrand.NUVIO,
        TrackingBrand.TMDB,
        -> Modifier
    }
    Image(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier.then(wordmarkSize),
        contentScale = ContentScale.Fit,
        alignment = Alignment.CenterStart,
    )
}

private fun TrackingBrand.cardBrush(): Brush = when (this) {
    TrackingBrand.TRAKT -> Brush.linearGradient(
        colors = listOf(Color(0xFF7D279B), Color(0xFFD61F56), Color(0xFFF22125)),
    )
    TrackingBrand.SIMKL -> Brush.linearGradient(
        colors = listOf(Color(0xFF050505), Color(0xFF292929), Color(0xFF111111)),
    )
    TrackingBrand.NUVIO,
    TrackingBrand.TMDB,
    -> Brush.linearGradient(colors = listOf(Color(0xFF242424), Color(0xFF111111)))
}

@Composable
private fun simklErrorMessage(error: SimklAuthError?): String? = when (error) {
    null, SimklAuthError.MISSING_CLIENT_ID -> null
    SimklAuthError.INVALID_CALLBACK,
    SimklAuthError.INVALID_CALLBACK_STATE,
    -> stringResource(Res.string.settings_simkl_invalid_callback)
    SimklAuthError.AUTHORIZATION_EXPIRED ->
        stringResource(Res.string.settings_simkl_authorization_expired)
    SimklAuthError.TOKEN_EXCHANGE_FAILED,
    SimklAuthError.INVALID_TOKEN_RESPONSE,
    -> stringResource(Res.string.settings_simkl_sign_in_failed)
    SimklAuthError.AUTHORIZATION_REVOKED ->
        stringResource(Res.string.settings_simkl_authorization_revoked)
}

/** Enough room for the taller of the two wordmarks, plus the air the artwork needs. */
private val TrackingBannerHeight = 104.dp

/** The watermark runs past the banner's edges; the banner's own clip trims it. */
private val TrackingBannerGlyphSize = 132.dp

private const val SIMKL_WEBSITE_URL = "https://simkl.com"
