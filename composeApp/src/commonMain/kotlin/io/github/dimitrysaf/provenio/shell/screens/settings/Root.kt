package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.provenio.shell.screens.updater.AppUpdaterStatus
import provenio.composeapp.generated.resources.updates_downloading_progress
import provenio.composeapp.generated.resources.updates_message_ready
import provenio.composeapp.generated.resources.updates_preparing_download
import provenio.composeapp.generated.resources.updates_title_available
import io.github.dimitrysaf.provenio.core.build.AppVersionConfig
import io.github.dimitrysaf.provenio.core.diagnostics.AppLogs
import io.github.dimitrysaf.provenio.core.updater.AppUpdaterPlatform
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.compose_about_channel_beta
import provenio.composeapp.generated.resources.compose_about_channel_release
import provenio.composeapp.generated.resources.compose_about_version_format
import provenio.composeapp.generated.resources.settings_category_profile_tracking
import provenio.composeapp.generated.resources.compose_settings_page_account
import provenio.composeapp.generated.resources.compose_settings_page_advanced
import provenio.composeapp.generated.resources.compose_settings_page_appearance
import provenio.composeapp.generated.resources.compose_settings_page_integrations
import provenio.composeapp.generated.resources.compose_settings_page_licenses_attributions
import provenio.composeapp.generated.resources.compose_settings_page_notifications
import provenio.composeapp.generated.resources.compose_settings_page_playback
import provenio.composeapp.generated.resources.compose_settings_page_privacy_policy
import provenio.composeapp.generated.resources.compose_settings_root_account_description
import provenio.composeapp.generated.resources.compose_settings_root_appearance_description
import provenio.composeapp.generated.resources.compose_settings_root_check_updates_description
import provenio.composeapp.generated.resources.compose_settings_root_check_updates_title
import provenio.composeapp.generated.resources.compose_settings_root_content_discovery_description
import provenio.composeapp.generated.resources.compose_settings_root_downloads_description
import provenio.composeapp.generated.resources.compose_settings_root_downloads_title
import provenio.composeapp.generated.resources.compose_settings_root_general_section
import provenio.composeapp.generated.resources.compose_settings_root_integrations_description
import provenio.composeapp.generated.resources.compose_settings_root_notifications_description
import provenio.composeapp.generated.resources.compose_settings_root_privacy_policy_description
import provenio.composeapp.generated.resources.compose_settings_root_switch_profile_description
import provenio.composeapp.generated.resources.compose_settings_root_switch_profile_title
import provenio.composeapp.generated.resources.compose_settings_root_tracking_description
import provenio.composeapp.generated.resources.compose_settings_root_about_section
import provenio.composeapp.generated.resources.compose_settings_root_account_section
import provenio.composeapp.generated.resources.compose_settings_root_advanced_description
import provenio.composeapp.generated.resources.compose_settings_root_advanced_section
import provenio.composeapp.generated.resources.compose_settings_page_content_discovery
import provenio.composeapp.generated.resources.compose_settings_page_tracking
import provenio.composeapp.generated.resources.settings_playback_subtitle
import provenio.composeapp.generated.resources.about_licenses_attributions_subtitle
import provenio.composeapp.generated.resources.keyboard_shortcuts_entry_description
import provenio.composeapp.generated.resources.keyboard_shortcuts_title
import androidx.compose.material.icons.rounded.CollectionsBookmark
import provenio.composeapp.generated.resources.collections_header
import provenio.composeapp.generated.resources.settings_content_discovery_collections_description
import org.jetbrains.compose.resources.stringResource

private const val PRIVACY_POLICY_URL = "https://github.com/Dimitrysaf/provenio#privacy"

internal fun LazyListScope.settingsRootContent(
    isTablet: Boolean,
    onPlaybackClick: () -> Unit,
    onAppearanceClick: () -> Unit,
    onAdvancedClick: () -> Unit,
    onKeyboardShortcutsClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onContentDiscoveryClick: () -> Unit,
    onIntegrationsClick: () -> Unit,
    onTrackingClick: () -> Unit,
    onLicensesAttributionsClick: () -> Unit,
    onCheckForUpdatesClick: (() -> Unit)? = null,
    onDownloadsClick: () -> Unit,
    onCollectionsClick: () -> Unit,
    onSwitchProfileClick: (() -> Unit)? = null,
    showAccountSection: Boolean = true,
    showGeneralSection: Boolean = true,
    showAboutSection: Boolean = true,
    showAdvancedSection: Boolean = true,
) {
    if (showAccountSection) {
        item {
            SettingsSection(
                title = stringResource(Res.string.settings_category_profile_tracking),
                isTablet = isTablet,
                showTitle = !isTablet,
            ) {
                SettingsList {
                    if (onSwitchProfileClick != null) {
                        navigationRow(
                            title = stringResource(Res.string.compose_settings_root_switch_profile_title),
                            description = stringResource(Res.string.compose_settings_root_switch_profile_description),
                            icon = Icons.Rounded.People,
                            onClick = onSwitchProfileClick,
                        )
                    }
                    navigationRow(
                        title = stringResource(Res.string.compose_settings_page_tracking),
                        description = stringResource(Res.string.compose_settings_root_tracking_description),
                        icon = Icons.Default.Sync,
                        onClick = onTrackingClick,
                    )
                }
            }
        }
    }
    if (showGeneralSection) {
        item {
            SettingsSection(
                title = stringResource(Res.string.compose_settings_root_general_section),
                isTablet = isTablet,
                showTitle = !isTablet,
            ) {
                SettingsList {
                    navigationRow(
                        title = stringResource(Res.string.compose_settings_page_appearance),
                        description = stringResource(Res.string.compose_settings_root_appearance_description),
                        icon = Icons.Rounded.Palette,
                        onClick = onAppearanceClick,
                    )
                    navigationRow(
                        title = stringResource(Res.string.compose_settings_page_content_discovery),
                        description = stringResource(Res.string.compose_settings_root_content_discovery_description),
                        icon = Icons.Rounded.Extension,
                        onClick = onContentDiscoveryClick,
                    )
                    navigationRow(
                        title = stringResource(Res.string.compose_settings_root_downloads_title),
                        description = stringResource(Res.string.compose_settings_root_downloads_description),
                        icon = Icons.Rounded.CloudDownload,
                        onClick = onDownloadsClick,
                    )
                    navigationRow(
                        title = stringResource(Res.string.collections_header),
                        description = stringResource(Res.string.settings_content_discovery_collections_description),
                        icon = Icons.Rounded.CollectionsBookmark,
                        onClick = onCollectionsClick,
                    )
                    navigationRow(
                        title = stringResource(Res.string.compose_settings_page_playback),
                        description = stringResource(Res.string.settings_playback_subtitle),
                        icon = Icons.Rounded.PlayArrow,
                        onClick = onPlaybackClick,
                    )
                    navigationRow(
                        title = stringResource(Res.string.compose_settings_page_integrations),
                        description = stringResource(Res.string.compose_settings_root_integrations_description),
                        icon = Icons.Rounded.Link,
                        onClick = onIntegrationsClick,
                    )
                    navigationRow(
                        title = stringResource(Res.string.compose_settings_page_notifications),
                        description = stringResource(Res.string.compose_settings_root_notifications_description),
                        icon = Icons.Rounded.Notifications,
                        onClick = onNotificationsClick,
                    )
                }
            }
        }
    }
    if (showAboutSection) {
        item {
            val uriHandler = LocalUriHandler.current
            SettingsSection(
                title = stringResource(Res.string.compose_settings_root_about_section),
                isTablet = isTablet,
                showTitle = !isTablet,
            ) {
                SettingsList {
                    navigationRow(
                        title = stringResource(Res.string.compose_settings_page_privacy_policy),
                        description = stringResource(Res.string.compose_settings_root_privacy_policy_description),
                        icon = Icons.Rounded.Policy,
                        onClick = { uriHandler.openUri(PRIVACY_POLICY_URL) },
                    )
                    navigationRow(
                        title = stringResource(Res.string.compose_settings_page_licenses_attributions),
                        description = stringResource(Res.string.about_licenses_attributions_subtitle),
                        icon = Icons.Rounded.Info,
                        onClick = onLicensesAttributionsClick,
                    )
                    if (onCheckForUpdatesClick != null) {
                        val updater by AppUpdaterStatus.uiState.collectAsStateWithLifecycle()
                        val downloadPercent = updater.downloadProgress?.let { (it * 100).toInt().coerceIn(0, 100) }
                        navigationRow(
                            title = stringResource(Res.string.compose_settings_root_check_updates_title),
                            description = when {
                                updater.isDownloading && downloadPercent != null ->
                                    stringResource(Res.string.updates_downloading_progress, downloadPercent)
                                updater.isDownloading -> stringResource(Res.string.updates_preparing_download)
                                updater.downloadedApkPath != null -> stringResource(Res.string.updates_message_ready)
                                updater.isUpdateAvailable -> stringResource(Res.string.updates_title_available)
                                else -> stringResource(Res.string.compose_settings_root_check_updates_description)
                            },
                            icon = Icons.Rounded.CloudDownload,
                            progress = when {
                                updater.isDownloading -> updater.downloadProgress ?: 0f
                                updater.downloadedApkPath != null -> 1f
                                else -> null
                            },
                            onClick = onCheckForUpdatesClick,
                        )
                    }
                }
            }
        }
    }
    if (showAdvancedSection) {
        item {
            val saveLogs = if (AppLogs.isSupported) rememberSaveLogsRow() else null
            SettingsSection(
                title = stringResource(Res.string.compose_settings_root_advanced_section),
                isTablet = isTablet,
                showTitle = !isTablet,
            ) {
                SettingsList {
                    navigationRow(
                        title = stringResource(Res.string.compose_settings_page_advanced),
                        description = stringResource(Res.string.compose_settings_root_advanced_description),
                        icon = Icons.Rounded.Tune,
                        onClick = onAdvancedClick,
                    )
                    navigationRow(
                        title = stringResource(Res.string.keyboard_shortcuts_title),
                        description = stringResource(Res.string.keyboard_shortcuts_entry_description),
                        icon = Icons.Rounded.Keyboard,
                        onClick = onKeyboardShortcutsClick,
                    )
                    saveLogs?.let { saveLogsRow(it) }
                }
            }
        }
    }
    item {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = if (isTablet) 20.dp else 16.dp),
        ) {
            if (showAboutSection) {
                MemberBrandWordmark(
                    height = if (isTablet) 30.dp else 26.dp,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                androidx.compose.foundation.layout.Spacer(
                    modifier = Modifier.height(if (isTablet) 10.dp else 8.dp),
                )
            }
            val channel = stringResource(
                if (AppUpdaterPlatform.isDebugBuild) Res.string.compose_about_channel_beta else Res.string.compose_about_channel_release,
            )
            Text(
                text = listOf(
                    stringResource(
                        Res.string.compose_about_version_format,
                        AppVersionConfig.VERSION_NAME,
                        AppVersionConfig.VERSION_CODE,
                    ),
                    channel,
                ).filter { it.isNotBlank() }.joinToString(" · "),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
