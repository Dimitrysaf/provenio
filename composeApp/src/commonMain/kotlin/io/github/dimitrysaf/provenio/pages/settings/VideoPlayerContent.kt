package io.github.dimitrysaf.provenio.pages.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.alorma.compose.settings.ui.expressive.SettingsGroup
import io.github.dimitrysaf.provenio.player.PlayerRepository
import io.github.dimitrysaf.provenio.player.availablePlayerBackends
import io.github.dimitrysaf.provenio.ui.components.SettingsTile
import io.github.dimitrysaf.provenio.ui.components.SettingsTileSpacing
import io.github.dimitrysaf.provenio.ui.components.TilePosition

@Composable
fun VideoPlayerContent() {
    val backend by PlayerRepository.backend.collectAsState()
    val available = availablePlayerBackends()
    var showBackendDialog by remember { mutableStateOf(false) }

    SettingsGroup(verticalArrangement = Arrangement.spacedBy(SettingsTileSpacing)) {
        SettingsTile(
            title = { Text("Player") },
            subtitle = {
                Text(if (available.isEmpty()) "No player available" else backend.label)
            },
            position = TilePosition.Single,
            enabled = available.isNotEmpty(),
            onClick = { showBackendDialog = true },
        )
    }

    if (showBackendDialog) {
        BackendDialog(
            options = available,
            selected = backend,
            onSelect = {
                PlayerRepository.setBackend(it)
                showBackendDialog = false
            },
            onDismiss = { showBackendDialog = false },
        )
    }
}

@Composable
private fun BackendDialog(
    options: List<io.github.dimitrysaf.provenio.player.PlayerBackend>,
    selected: io.github.dimitrysaf.provenio.player.PlayerBackend,
    onSelect: (io.github.dimitrysaf.provenio.player.PlayerBackend) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Player") },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 64.dp)
                            .selectable(
                                selected = option == selected,
                                onClick = { onSelect(option) },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = option == selected, onClick = null)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(option.label, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = option.summary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
