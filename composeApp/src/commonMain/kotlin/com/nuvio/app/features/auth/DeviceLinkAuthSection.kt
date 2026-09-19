package com.nuvio.app.features.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.core.auth.DeviceLinkAuthFailure
import com.nuvio.app.core.auth.DeviceLinkAuthState
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.compose_auth_link_cancel
import nuvio.composeapp.generated.resources.compose_auth_link_code_expired
import nuvio.composeapp.generated.resources.compose_auth_link_creating_code
import nuvio.composeapp.generated.resources.compose_auth_link_failed
import nuvio.composeapp.generated.resources.compose_auth_link_open
import nuvio.composeapp.generated.resources.compose_auth_link_open_failed
import nuvio.composeapp.generated.resources.compose_auth_link_sign_in
import nuvio.composeapp.generated.resources.compose_auth_link_signing_in
import nuvio.composeapp.generated.resources.compose_auth_link_try_again
import nuvio.composeapp.generated.resources.compose_auth_link_waiting
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DeviceLinkAuthSection(
    state: DeviceLinkAuthState,
    enabled: Boolean,
    onStart: () -> Unit,
    onCancel: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    var openFailed by remember(state) { mutableStateOf(false) }

    when (state) {
        DeviceLinkAuthState.Idle -> {
            FilledTonalButton(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
            ) {
                Text(stringResource(Res.string.compose_auth_link_sign_in))
            }
        }
        DeviceLinkAuthState.Starting -> {
            FilledTonalButton(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
            ) {
                Text(stringResource(Res.string.compose_auth_link_creating_code))
            }
        }
        is DeviceLinkAuthState.Waiting -> {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = state.code,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                letterSpacing = 2.5.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        openFailed = runCatching {
                            uriHandler.openUri(state.verificationUrl)
                        }.isFailure
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled && !state.isCompleting,
                ) {
                    if (state.isCompleting) {
                        LoadingIndicator(
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(stringResource(Res.string.compose_auth_link_open))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (state.isCompleting) {
                        stringResource(Res.string.compose_auth_link_signing_in)
                    } else {
                        stringResource(Res.string.compose_auth_link_waiting)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                if (openFailed) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.compose_auth_link_open_failed),
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                }

                if (!state.isCompleting) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onCancel) {
                        Text(stringResource(Res.string.compose_auth_link_cancel))
                    }
                }
            }
        }
        is DeviceLinkAuthState.Failed -> {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = when (state.reason) {
                        DeviceLinkAuthFailure.Expired -> {
                            stringResource(Res.string.compose_auth_link_code_expired)
                        }
                        DeviceLinkAuthFailure.Start,
                        DeviceLinkAuthFailure.Complete,
                        -> stringResource(Res.string.compose_auth_link_failed)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(16.dp))

                FilledTonalButton(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                ) {
                    Text(stringResource(Res.string.compose_auth_link_try_again))
                }
            }
        }
    }
}
