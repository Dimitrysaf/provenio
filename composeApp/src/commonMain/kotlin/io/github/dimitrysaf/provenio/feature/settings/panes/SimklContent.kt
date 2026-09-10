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
        SimklAuthState.Starting -> Note("Asking Simkl for a code...")
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
            text = "Sign in to sync your watchlist, progress and ratings with Simkl.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { SimklRepository.signIn() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Sign in with Simkl")
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
            text = "Go to $page and enter this code.",
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
            Text("Copy code")
        }
        Text(
            text = "Expires in ${secondsRemaining / 60}m ${secondsRemaining % 60}s",
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
            Text("Open Simkl")
        }
        // The moment the user returns from the browser is both when they are most likely
        // authorised and when background network restrictions lift.
        OutlinedButton(
            onClick = { SimklRepository.checkNow() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("I entered the code")
        }
        TextButton(onClick = { SimklRepository.cancelSignIn() }) { Text("Cancel") }
    }
}

@Composable
private fun SignedIn() {
    SettingsGroup(verticalArrangement = Arrangement.spacedBy(SettingsTileSpacing)) {
        SettingsTile(
            title = { Text("Account") },
            subtitle = { Text("Signed in") },
            position = TilePosition.First,
            onClick = {},
        )
        SettingsTile(
            title = { Text("Sign out") },
            subtitle = { Text("Removes the token from this device") },
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
        OutlinedButton(onClick = { SimklRepository.signIn() }) { Text("Try again") }
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
