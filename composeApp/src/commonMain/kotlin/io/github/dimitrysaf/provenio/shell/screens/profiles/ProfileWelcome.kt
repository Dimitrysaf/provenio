package io.github.dimitrysaf.provenio.shell.screens.profiles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.provenio.core.localsync.LocalSyncRepository
import io.github.dimitrysaf.provenio.shell.screens.settings.AppBrandWordmark
import io.github.dimitrysaf.provenio.shell.screens.settings.LocalSyncCodeDialog
import io.github.dimitrysaf.provenio.shell.screens.settings.LocalSyncFeedbackEffect
import io.github.dimitrysaf.provenio.shell.screens.settings.LocalSyncPairingDialog
import io.github.dimitrysaf.provenio.shell.screens.settings.LocalSyncProgress
import io.github.dimitrysaf.provenio.shell.screens.settings.rememberLocalSyncScanner
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.welcome_create_profile
import provenio.composeapp.generated.resources.welcome_enter_code
import provenio.composeapp.generated.resources.welcome_pairing_hint
import provenio.composeapp.generated.resources.welcome_scan_code
import provenio.composeapp.generated.resources.welcome_show_code
import provenio.composeapp.generated.resources.welcome_subtitle
import provenio.composeapp.generated.resources.welcome_title

// What a device with no profiles opens on: start fresh, or pair with a device already in use and take its profiles and data.
@Composable
internal fun ProfileWelcome(
    onCreateProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val syncState by LocalSyncRepository.uiState.collectAsStateWithLifecycle()
    var showCodeEntry by rememberSaveable { mutableStateOf(false) }
    val scan = rememberLocalSyncScanner { code -> code?.let(LocalSyncRepository::join) }
    LocalSyncFeedbackEffect(syncState.activity)

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AppBrandWordmark(modifier = Modifier.height(42.dp))
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = stringResource(Res.string.welcome_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(Res.string.welcome_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 420.dp),
        )
        Spacer(modifier = Modifier.height(32.dp))

        val buttonModifier = Modifier
            .widthIn(max = 360.dp)
            .fillMaxWidth()
        Column(
            modifier = buttonModifier,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(onClick = onCreateProfile, modifier = Modifier.fillMaxWidth()) {
                WelcomeButtonContent(icon = Icons.Rounded.PersonAdd, label = stringResource(Res.string.welcome_create_profile))
            }
            if (LocalSyncRepository.isSupported) {
                if (scan != null) {
                    FilledTonalButton(
                        onClick = scan,
                        enabled = !syncState.joining,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        WelcomeButtonContent(
                            icon = Icons.Rounded.QrCodeScanner,
                            label = stringResource(Res.string.welcome_scan_code),
                            loading = syncState.joining,
                        )
                    }
                }
                OutlinedButton(
                    onClick = LocalSyncRepository::startPairing,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    WelcomeButtonContent(icon = Icons.Rounded.QrCode, label = stringResource(Res.string.welcome_show_code))
                }
                TextButton(
                    onClick = { showCodeEntry = true },
                    enabled = !syncState.joining,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    WelcomeButtonContent(
                        icon = Icons.Rounded.Keyboard,
                        label = stringResource(Res.string.welcome_enter_code),
                        loading = syncState.joining && scan == null,
                    )
                }
            }
        }

        if (LocalSyncRepository.isSupported) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(Res.string.welcome_pairing_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 360.dp),
            )
        }
    }

    syncState.pairingCode?.let { code ->
        LocalSyncPairingDialog(
            code = code,
            qr = syncState.pairingQr,
            onDismiss = LocalSyncRepository::stopPairing,
        )
    }

    if (showCodeEntry) {
        LocalSyncCodeDialog(
            onConnect = { code ->
                showCodeEntry = false
                LocalSyncRepository.join(code)
            },
            onDismiss = { showCodeEntry = false },
        )
    }
}

@Composable
private fun WelcomeButtonContent(
    icon: ImageVector,
    label: String,
    loading: Boolean = false,
) {
    if (loading) {
        LocalSyncProgress(modifier = Modifier.size(ButtonDefaults.IconSize))
    } else {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
    }
    Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
    Text(label)
}
