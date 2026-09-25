package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.provenio.shell.theme.Tokens
import io.github.dimitrysaf.provenio.shell.theme.provenio
import io.github.dimitrysaf.provenio.core.profiles.ProfileRepository
import io.github.dimitrysaf.provenio.core.watch.progress.ContinueWatchingEnrichmentCache
import io.github.dimitrysaf.provenio.core.watch.progress.WatchProgressRepository
import kotlinx.coroutines.launch
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.action_cancel
import provenio.composeapp.generated.resources.settings_advanced_clear_cw_cache
import provenio.composeapp.generated.resources.settings_advanced_clear_cw_cache_done
import provenio.composeapp.generated.resources.settings_advanced_clear_cw_cache_subtitle
import provenio.composeapp.generated.resources.settings_advanced_remember_last_profile
import provenio.composeapp.generated.resources.settings_advanced_remember_last_profile_description
import provenio.composeapp.generated.resources.settings_advanced_section_cache
import provenio.composeapp.generated.resources.settings_advanced_section_diagnostics
import provenio.composeapp.generated.resources.settings_advanced_section_startup
import provenio.composeapp.generated.resources.settings_advanced_sentry_reports
import provenio.composeapp.generated.resources.settings_advanced_sentry_reports_subtitle
import provenio.composeapp.generated.resources.sentry_disable_dialog_subtitle
import provenio.composeapp.generated.resources.sentry_disable_dialog_title
import provenio.composeapp.generated.resources.sentry_enable_dialog_subtitle
import provenio.composeapp.generated.resources.sentry_enable_dialog_title
import provenio.composeapp.generated.resources.sentry_help_body
import provenio.composeapp.generated.resources.sentry_help_title
import provenio.composeapp.generated.resources.sentry_keep_enabled
import provenio.composeapp.generated.resources.sentry_not_sent_body
import provenio.composeapp.generated.resources.sentry_not_sent_title
import provenio.composeapp.generated.resources.sentry_sent_body
import provenio.composeapp.generated.resources.sentry_sent_title
import provenio.composeapp.generated.resources.sentry_turn_off
import provenio.composeapp.generated.resources.sentry_turn_on
import org.jetbrains.compose.resources.stringResource
import io.github.dimitrysaf.provenio.core.settings.SentrySettingsRepository

internal fun LazyListScope.advancedSettingsContent(
    isTablet: Boolean,
    rememberLastProfileEnabled: Boolean,
) {
    item {
        SettingsSection(
            title = stringResource(Res.string.settings_advanced_section_startup),
            isTablet = isTablet,
        ) {
            SettingsList {
                switchRow(
                    title = stringResource(Res.string.settings_advanced_remember_last_profile),
                    description = stringResource(Res.string.settings_advanced_remember_last_profile_description),
                    checked = { rememberLastProfileEnabled },
                    onCheckedChange = ProfileRepository::setRememberLastProfileEnabled,
                )
            }
        }
    }
    if (SentrySettingsRepository.isSupported) {
        item {
            val sentryEnabledFlow = remember {
                SentrySettingsRepository.ensureLoaded()
                SentrySettingsRepository.enabled
            }
            val sentryEnabled by sentryEnabledFlow.collectAsStateWithLifecycle()
            var showSentryDialog by rememberSaveable { mutableStateOf(false) }

            SettingsSection(
                title = stringResource(Res.string.settings_advanced_section_diagnostics),
                isTablet = isTablet,
            ) {
                SettingsList {
                    switchRow(
                        title = stringResource(Res.string.settings_advanced_sentry_reports),
                        description = stringResource(Res.string.settings_advanced_sentry_reports_subtitle),
                        checked = { sentryEnabled },
                        onCheckedChange = { showSentryDialog = true },
                    )
                }
            }

            if (showSentryDialog) {
                SentrySettingsDialog(
                    enabled = sentryEnabled,
                    onConfirm = {
                        SentrySettingsRepository.setEnabled(!sentryEnabled)
                    },
                    onDismiss = {
                        showSentryDialog = false
                    },
                )
            }
        }
    }
    item {
        SettingsSection(
            title = stringResource(Res.string.settings_advanced_section_cache),
            isTablet = isTablet,
        ) {
            SettingsList {
                val scope = rememberCoroutineScope()
                var cleared by rememberSaveable { mutableStateOf(false) }
                navigationRow(
                    title = stringResource(Res.string.settings_advanced_clear_cw_cache),
                    description = if (cleared) {
                        stringResource(Res.string.settings_advanced_clear_cw_cache_done)
                    } else {
                        stringResource(Res.string.settings_advanced_clear_cw_cache_subtitle)
                    },
                    onClick = {
                        if (!cleared) {
                            ContinueWatchingEnrichmentCache.clearAll(ProfileRepository.activeProfileId)
                            cleared = true
                            scope.launch {
                                WatchProgressRepository.clearLocalAndForceSnapshotRefreshFromServer(
                                    ProfileRepository.activeProfileId,
                                )
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun SentrySettingsDialog(
    enabled: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (enabled) Res.string.sentry_disable_dialog_title else Res.string.sentry_enable_dialog_title,
                ),
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Tokens.Space.s12),
            ) {
                Text(
                    stringResource(
                        if (enabled) Res.string.sentry_disable_dialog_subtitle else Res.string.sentry_enable_dialog_subtitle,
                    ),
                )
                SentryInfoSection(
                    title = stringResource(Res.string.sentry_help_title),
                    body = stringResource(Res.string.sentry_help_body),
                )
                SentryInfoSection(
                    title = stringResource(Res.string.sentry_sent_title),
                    body = stringResource(Res.string.sentry_sent_body),
                )
                SentryInfoSection(
                    title = stringResource(Res.string.sentry_not_sent_title),
                    body = stringResource(Res.string.sentry_not_sent_body),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
            ) {
                Text(stringResource(if (enabled) Res.string.sentry_turn_off else Res.string.sentry_turn_on))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(if (enabled) Res.string.sentry_keep_enabled else Res.string.action_cancel))
            }
        },
    )
}

@Composable
private fun SentryInfoSection(
    title: String,
    body: String,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Tokens.Space.s4),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
