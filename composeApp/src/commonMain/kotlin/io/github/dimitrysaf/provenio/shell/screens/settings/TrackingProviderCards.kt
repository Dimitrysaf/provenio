package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.provenio.shell.components.LoadingSpinner
import io.github.dimitrysaf.provenio.shell.theme.provenio
import io.github.dimitrysaf.provenio.core.profiles.ProfileRepository
import io.github.dimitrysaf.provenio.core.tracking.simkl.SimklAuthError
import io.github.dimitrysaf.provenio.core.tracking.simkl.SimklAuthRepository
import io.github.dimitrysaf.provenio.core.tracking.simkl.SimklAuthUiState
import io.github.dimitrysaf.provenio.shell.screens.simkl.SimklBrandAsset
import io.github.dimitrysaf.provenio.core.tracking.simkl.SimklConnectionMode
import io.github.dimitrysaf.provenio.core.tracking.simkl.SimklSyncRepository
import io.github.dimitrysaf.provenio.shell.screens.simkl.simklBrandPainter
import io.github.dimitrysaf.provenio.core.tracking.TrackingProviderId
import io.github.dimitrysaf.provenio.core.tracking.TrackingRefreshIntent
import io.github.dimitrysaf.provenio.core.tracking.trakt.TraktAuthRepository
import io.github.dimitrysaf.provenio.core.tracking.trakt.TraktAuthUiState
import io.github.dimitrysaf.provenio.core.tracking.trakt.TraktBrandAsset
import io.github.dimitrysaf.provenio.core.tracking.trakt.TraktConnectionMode
import io.github.dimitrysaf.provenio.shell.screens.trakt.traktBrandPainter
import io.github.dimitrysaf.provenio.core.watch.progress.WatchProgressSourceCoordinator
import kotlinx.coroutines.launch
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.action_cancel
import provenio.composeapp.generated.resources.action_close
import provenio.composeapp.generated.resources.settings_simkl_authorization_expired
import provenio.composeapp.generated.resources.settings_simkl_authorization_revoked
import provenio.composeapp.generated.resources.settings_simkl_connect
import provenio.composeapp.generated.resources.settings_simkl_connected_as
import provenio.composeapp.generated.resources.settings_simkl_connected_description
import provenio.composeapp.generated.resources.settings_simkl_default_user
import provenio.composeapp.generated.resources.settings_simkl_disconnect
import provenio.composeapp.generated.resources.settings_simkl_disconnect_description
import provenio.composeapp.generated.resources.settings_simkl_finish_sign_in
import provenio.composeapp.generated.resources.settings_simkl_invalid_callback
import provenio.composeapp.generated.resources.settings_simkl_missing_credentials
import provenio.composeapp.generated.resources.settings_simkl_open_login
import provenio.composeapp.generated.resources.settings_simkl_sign_in_description
import provenio.composeapp.generated.resources.settings_simkl_sign_in_failed
import provenio.composeapp.generated.resources.settings_simkl_sync_info_action
import provenio.composeapp.generated.resources.settings_simkl_sync_now
import provenio.composeapp.generated.resources.settings_simkl_visit
import provenio.composeapp.generated.resources.settings_tracking_approval_redirect
import provenio.composeapp.generated.resources.settings_tracking_disconnect_description
import provenio.composeapp.generated.resources.settings_tracking_disconnect_title
import provenio.composeapp.generated.resources.settings_trakt_approval_redirect
import provenio.composeapp.generated.resources.settings_trakt_connect
import provenio.composeapp.generated.resources.settings_trakt_connected_as
import provenio.composeapp.generated.resources.settings_trakt_default_user
import provenio.composeapp.generated.resources.settings_trakt_disconnect
import provenio.composeapp.generated.resources.settings_trakt_disconnect_description
import provenio.composeapp.generated.resources.settings_trakt_failed_open_browser
import provenio.composeapp.generated.resources.settings_trakt_finish_sign_in
import provenio.composeapp.generated.resources.settings_trakt_missing_credentials
import provenio.composeapp.generated.resources.settings_trakt_open_login
import provenio.composeapp.generated.resources.settings_trakt_save_actions_description
import provenio.composeapp.generated.resources.settings_trakt_sign_in_description
import provenio.composeapp.generated.resources.tracking_feature_comments
import provenio.composeapp.generated.resources.tracking_feature_lists
import provenio.composeapp.generated.resources.tracking_feature_progress
import provenio.composeapp.generated.resources.tracking_feature_recommendations
import provenio.composeapp.generated.resources.tracking_feature_scrobbling
import provenio.composeapp.generated.resources.tracking_feature_watched
import provenio.composeapp.generated.resources.tracking_features_available
import provenio.composeapp.generated.resources.tracking_features_syncing
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import io.github.dimitrysaf.provenio.core.settings.TrackingBrand
import io.github.dimitrysaf.provenio.core.settings.TrackingConnectionCardMode
import io.github.dimitrysaf.provenio.core.settings.toTrackingConnectionCardMode

@Composable
internal fun TrackingProviderCards(
    isTablet: Boolean,
    traktUiState: TraktAuthUiState,
    simklUiState: SimklAuthUiState,
    showTrakt: Boolean,
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

    // Simkl is the tracker the app is built around, so it always leads; Trakt joins only when asked for.
    val simklCard: @Composable (Modifier) -> Unit = { cardModifier ->
        SimklProviderCard(
            uiState = simklUiState,
            isSyncing = syncState.isLoading,
            syncErrorMessage = syncState.errorMessage,
            onSyncRequested = onSimklSyncRequested,
            onInfoRequested = { showSyncInfo = true },
            modifier = cardModifier,
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val useTwoColumns = showTrakt && maxWidth >= 600.dp
        if (useTwoColumns) {
            // Each list is as tall as its own rows, so neither is stretched to match the other.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                simklCard(Modifier.weight(1f))
                TraktProviderCard(
                    uiState = traktUiState,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(if (isTablet) 16.dp else 12.dp),
            ) {
                simklCard(Modifier.fillMaxWidth())
                if (showTrakt) {
                    TraktProviderCard(
                        uiState = traktUiState,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
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
        features = TraktFeatures,
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
        features = SimklFeatures,
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
    features: List<TrackingFeature>,
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
        shapedRow { shape ->
            TrackingFeaturesRow(
                features = features,
                active = mode == TrackingConnectionCardMode.CONNECTED,
                shape = shape,
            )
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
                            { LoadingSpinner(size = 18.dp) }
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

/** One kind of data a tracker keeps in step with the app. */
internal data class TrackingFeature(
    val icon: ImageVector,
    val label: StringResource,
)

private val SimklFeatures = listOf(
    TrackingFeature(Icons.Rounded.BookmarkBorder, Res.string.tracking_feature_lists),
    TrackingFeature(Icons.Rounded.History, Res.string.tracking_feature_watched),
    TrackingFeature(Icons.Rounded.PlayCircle, Res.string.tracking_feature_progress),
    TrackingFeature(Icons.Rounded.Sensors, Res.string.tracking_feature_scrobbling),
)

private val TraktFeatures = SimklFeatures + listOf(
    TrackingFeature(Icons.Rounded.Forum, Res.string.tracking_feature_comments),
    TrackingFeature(Icons.Rounded.AutoAwesome, Res.string.tracking_feature_recommendations),
)

/** What the tracker syncs, as tonal chips: filled in once it is connected, muted until then. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TrackingFeaturesRow(
    features: List<TrackingFeature>,
    active: Boolean,
    shape: RoundedCornerShape,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(
                    if (active) Res.string.tracking_features_syncing else Res.string.tracking_features_available,
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                features.forEach { feature ->
                    TrackingFeatureChip(feature = feature, active = active)
                }
            }
        }
    }
}

@Composable
private fun TrackingFeatureChip(
    feature: TrackingFeature,
    active: Boolean,
) {
    Surface(
        shape = CircleShape,
        color = if (active) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        contentColor = if (active) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = feature.icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(feature.label),
                style = MaterialTheme.typography.labelLarge,
            )
        }
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
                    TrackingBrand.ACCOUNT,
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
        TrackingBrand.ACCOUNT -> Icon(
            imageVector = Icons.Rounded.Sync,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = MaterialTheme.provenio.colors.accent,
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
        TrackingBrand.ACCOUNT,
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
        TrackingBrand.ACCOUNT,
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
    TrackingBrand.ACCOUNT,
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
