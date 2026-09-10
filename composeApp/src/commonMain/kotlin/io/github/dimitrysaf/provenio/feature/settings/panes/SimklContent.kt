package io.github.dimitrysaf.provenio.feature.settings.panes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alorma.compose.settings.ui.expressive.SettingsGroup
import io.github.dimitrysaf.provenio.simkl.SimklAuthState
import io.github.dimitrysaf.provenio.simkl.SimklRepository
import io.github.dimitrysaf.provenio.designsystem.components.SettingsTile
import io.github.dimitrysaf.provenio.designsystem.components.SettingsTileSpacing
import io.github.dimitrysaf.provenio.designsystem.components.TilePosition
import io.github.dimitrysaf.provenio.core.platform.rememberUrlOpener
import io.github.dimitrysaf.provenio.resources.Res
import io.github.dimitrysaf.provenio.resources.cancel
import io.github.dimitrysaf.provenio.resources.simkl_account
import io.github.dimitrysaf.provenio.resources.simkl_asking_for_code
import io.github.dimitrysaf.provenio.resources.simkl_copy_code
import io.github.dimitrysaf.provenio.resources.simkl_entered_code
import io.github.dimitrysaf.provenio.resources.simkl_expires_in
import io.github.dimitrysaf.provenio.resources.simkl_go_to_page
import io.github.dimitrysaf.provenio.resources.simkl_open
import io.github.dimitrysaf.provenio.resources.simkl_sign_in
import io.github.dimitrysaf.provenio.resources.simkl_sign_in_body
import io.github.dimitrysaf.provenio.resources.simkl_sign_out
import io.github.dimitrysaf.provenio.resources.simkl_sign_out_summary
import io.github.dimitrysaf.provenio.resources.simkl_signed_in
import io.github.dimitrysaf.provenio.resources.try_again
import org.jetbrains.compose.resources.stringResource

// LocalClipboard replaces this, but its API is suspend and takes a ClipEntry whose
// multiplatform construction differs by version. Not worth the risk for one copy button.
@Suppress("DEPRECATION")
@Composable
fun SimklContent() {
    val state by SimklRepository.authState.collectAsState()
    val openUrl = rememberUrlOpener()
    val clipboard = LocalClipboardManager.current

    when (val current = state) {
        SimklAuthState.SignedOut -> SignedOut()
        SimklAuthState.Starting -> Note(stringResource(Res.string.simkl_asking_for_code))
        is SimklAuthState.AwaitingUser -> AwaitingUser(
            code = current.userCode,
            page = current.verificationPage,
            secondsRemaining = current.secondsRemaining,
            note = current.note,
            onOpen = { openUrl(current.verificationPage) },
            onCopy = { clipboard.setText(AnnotatedString(current.userCode)) },
        )
        SimklAuthState.SignedIn -> SignedIn()
        is SimklAuthState.Error -> ErrorState(current.message)
    }
}

@Composable
private fun SignedOut() {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            text = stringResource(Res.string.simkl_sign_in_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { SimklRepository.signIn() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(Res.string.simkl_sign_in))
        }
    }
}

/**
 * The PIN step. The code is typed by hand on another device, so it is set large and the
 * link is one tap away rather than something to transcribe.
 */
@Composable
private fun AwaitingUser(
    code: String,
    page: String,
    secondsRemaining: Int,
    note: String?,
    onOpen: () -> Unit,
    onCopy: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(Res.string.simkl_go_to_page, page),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        // Selectable so the code can be long pressed and copied, and there is a copy
        // button beside it because selecting five characters on a phone is fiddly.
        SelectionContainer {
            Text(text = code, style = MaterialTheme.typography.displaySmall)
        }
        OutlinedButton(onClick = onCopy) {
            Icon(Icons.Outlined.ContentCopy, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(Res.string.simkl_copy_code))
        }
        Text(
            text = stringResource(
                Res.string.simkl_expires_in,
                secondsRemaining / 60,
                secondsRemaining % 60,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (note != null) {
            Text(
                text = note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }
        Button(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.simkl_open))
        }
        // The moment the user returns from the browser is both when they are most likely
        // authorised and when background network restrictions lift.
        OutlinedButton(
            onClick = { SimklRepository.checkNow() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(Res.string.simkl_entered_code))
        }
        TextButton(onClick = { SimklRepository.cancelSignIn() }) {
            Text(stringResource(Res.string.cancel))
        }
    }
}

@Composable
private fun SignedIn() {
    SettingsGroup(verticalArrangement = Arrangement.spacedBy(SettingsTileSpacing)) {
        SettingsTile(
            title = { Text(stringResource(Res.string.simkl_account)) },
            subtitle = { Text(stringResource(Res.string.simkl_signed_in)) },
            position = TilePosition.First,
            onClick = {},
        )
        SettingsTile(
            title = { Text(stringResource(Res.string.simkl_sign_out)) },
            subtitle = { Text(stringResource(Res.string.simkl_sign_out_summary)) },
            position = TilePosition.Last,
            onClick = { SimklRepository.signOut() },
        )
    }
}

@Composable
private fun ErrorState(message: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(text = message, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { SimklRepository.signIn() }) {
            Text(stringResource(Res.string.try_again))
        }
    }
}

@Composable
private fun Note(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}
