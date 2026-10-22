package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.provenio.core.stremio.StremioImport
import io.github.dimitrysaf.provenio.core.stremio.StremioImportState
import io.github.dimitrysaf.provenio.core.stremio.StremioImportStep
import io.github.dimitrysaf.provenio.shell.components.SmallLoadingSpinner
import io.github.dimitrysaf.provenio.shell.components.TextActionsMenu
import io.github.dimitrysaf.provenio.shell.components.ToastController
import io.github.dimitrysaf.provenio.shell.components.arrowKeysLeaveField
import io.github.dimitrysaf.provenio.shell.components.rememberTextActionsMenu
import io.github.dimitrysaf.provenio.shell.components.textActionsMenu
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.*

internal const val StremioName = "Stremio"

private enum class StremioImportSource {
    App,
    SignIn,
}

/**
 * Bringing a Stremio account over: from the Stremio app on this computer when it is signed in,
 * or by signing in, and once done, what came over.
 */
@Composable
internal fun StremioImportList(modifier: Modifier = Modifier) {
    LaunchedEffect(Unit) { StremioImport.detect() }
    val state by StremioImport.state.collectAsStateWithLifecycle()
    val detected by StremioImport.detected.collectAsStateWithLifecycle()
    var source by remember { mutableStateOf<StremioImportSource?>(null) }
    var showSignIn by rememberSaveable { mutableStateOf(false) }
    StremioImportFeedbackEffect(state)

    val running = state as? StremioImportState.Running
    val summary = (state as? StremioImportState.Done)?.summary
    val stepLabel = running?.step?.let { stringResource(it.labelRes()) }
    val importLabel = stringResource(Res.string.stremio_import_action)
    val appTitle = stringResource(Res.string.stremio_import_app)
    val signInTitle = stringResource(Res.string.stremio_import_sign_in)
    val addonsTitle = stringResource(Res.string.onboarding_done_addons)
    val libraryTitle = stringResource(Res.string.library_title)
    val continueWatchingTitle = stringResource(Res.string.compose_settings_page_continue_watching)
    val watchedTitle = stringResource(Res.string.tracking_feature_watched)

    SettingsList(modifier = modifier) {
        detected?.let { account ->
            val importing = running != null && source == StremioImportSource.App
            val importApp = {
                source = StremioImportSource.App
                StremioImport.importDetected()
            }
            navigationRow(
                title = appTitle,
                description = stepLabel.takeIf { importing } ?: account.email.ifBlank { null },
                icon = Icons.Rounded.Download,
                enabled = running == null,
                trailingContent = {
                    FilledTonalIconButton(onClick = importApp, enabled = running == null && summary == null) {
                        when {
                            importing -> SmallLoadingSpinner(size = 20.dp)
                            summary != null && source == StremioImportSource.App ->
                                Icon(Icons.Rounded.Check, contentDescription = null)
                            else -> Icon(Icons.Rounded.Download, contentDescription = importLabel)
                        }
                    }
                },
                onClick = importApp,
            )
        }
        val signingIn = running != null && source == StremioImportSource.SignIn
        navigationRow(
            title = signInTitle,
            description = stepLabel.takeIf { signingIn },
            icon = Icons.Rounded.AccountCircle,
            enabled = running == null,
            trailingContent = StremioImportSpinner.takeIf { signingIn },
            onClick = { showSignIn = true },
        )
        if (summary != null) {
            infoRow(title = addonsTitle, value = summary.addons.toString())
            infoRow(title = libraryTitle, value = summary.library.toString())
            infoRow(title = continueWatchingTitle, value = summary.continueWatching.toString())
            infoRow(title = watchedTitle, value = summary.watched.toString())
        }
    }

    if (showSignIn) {
        StremioSignInDialog(
            onSignIn = { email, password ->
                showSignIn = false
                source = StremioImportSource.SignIn
                StremioImport.signIn(email, password)
            },
            onDismiss = { showSignIn = false },
        )
    }
}

private val StremioImportSpinner: @Composable () -> Unit = { SmallLoadingSpinner(size = 20.dp) }

// Says how an import ended, once, when it ends while this is on screen.
@Composable
private fun StremioImportFeedbackEffect(state: StremioImportState) {
    val previous = remember { mutableStateOf(state) }
    LaunchedEffect(state) {
        val wasRunning = previous.value is StremioImportState.Running
        previous.value = state
        if (!wasRunning) return@LaunchedEffect
        when (state) {
            is StremioImportState.Done -> ToastController.show(getString(Res.string.stremio_import_done))
            is StremioImportState.Failed -> ToastController.show(
                getString(if (state.signIn) Res.string.stremio_import_sign_in_failed else Res.string.stremio_import_failed),
            )
            else -> Unit
        }
    }
}

@Composable
private fun StremioSignInDialog(
    onSignIn: (email: String, password: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val canSignIn = email.isNotBlank() && password.isNotEmpty()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.stremio_import_sign_in)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val textMenu = rememberTextActionsMenu()
                TextActionsMenu(textMenu, text = email) { pasted -> email += pasted }
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(Res.string.stremio_import_email)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth().arrowKeysLeaveField { email.isEmpty() }.textActionsMenu(textMenu),
                )
                SettingsSecretTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = stringResource(Res.string.stremio_import_password),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSignIn(email, password) }, enabled = canSignIn) {
                Text(stringResource(Res.string.stremio_import_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    )
}

private fun StremioImportStep.labelRes() = when (this) {
    StremioImportStep.SigningIn -> Res.string.stremio_import_step_signing_in
    StremioImportStep.Addons -> Res.string.stremio_import_step_addons
    StremioImportStep.Library -> Res.string.stremio_import_step_library
    StremioImportStep.Watched -> Res.string.stremio_import_step_watched
}
