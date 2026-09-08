package io.github.dimitrysaf.provenio.pages

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.stremio.AddonRepository
import io.github.dimitrysaf.provenio.stremio.AddonResult
import io.github.dimitrysaf.provenio.stremio.InstalledAddon
import io.github.dimitrysaf.provenio.ui.components.BackTopBar
import io.github.dimitrysaf.provenio.ui.components.PageScaffold
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddonsPage(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
) {
    val collection by AddonRepository.collection.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    PageScaffold(
        modifier = modifier,
        topBar = { scrollBehavior ->
            BackTopBar(
                title = "Add-ons",
                onBack = onBack,
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add an add-on")
                    }
                },
            )
        },
    ) {
        val addons = collection.all
        if (addons.isEmpty()) {
            EmptyAddons()
        } else {
            Text(
                text = "Add-ons are asked in this order. Drag the top one higher to prefer it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            addons.forEachIndexed { index, addon ->
                AddonRow(
                    addon = addon,
                    isFirst = index == 0,
                    isLast = index == addons.lastIndex,
                )
            }
        }
    }

    if (showAddDialog) {
        AddAddonDialog(onDismiss = { showAddDialog = false })
    }
}

@Composable
private fun EmptyAddons() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.Extension,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "No add-ons yet",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Add-ons supply the catalogs, metadata and streams. " +
                "Paste an add-on's manifest URL to get started.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddonRow(
    addon: InstalledAddon,
    isFirst: Boolean,
    isLast: Boolean,
) {
    val manifest = addon.manifest
    var menuOpen by remember { mutableStateOf(false) }

    Column {
        ListItem(
            headlineContent = { Text(manifest.name) },
            supportingContent = {
                Text(
                    text = manifest.description ?: "Version ${manifest.version}",
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            leadingContent = { Icon(Icons.Outlined.Extension, contentDescription = null) },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = addon.enabled,
                        onCheckedChange = null,
                    )
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = "More options for ${manifest.name}",
                            )
                        }
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Move up") },
                                enabled = !isFirst,
                                leadingIcon = {
                                    Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = null)
                                },
                                onClick = {
                                    AddonRepository.move(manifest.id, -1)
                                    menuOpen = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Move down") },
                                enabled = !isLast,
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.KeyboardArrowDown,
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    AddonRepository.move(manifest.id, 1)
                                    menuOpen = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Remove") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Delete, contentDescription = null)
                                },
                                onClick = {
                                    AddonRepository.remove(manifest.id)
                                    menuOpen = false
                                },
                            )
                        }
                    }
                }
            },
            modifier = Modifier.toggleable(
                value = addon.enabled,
                role = Role.Switch,
                onValueChange = { AddonRepository.setEnabled(manifest.id, it) },
            ),
        )

        // What the addon actually provides, which is the thing worth seeing at a glance.
        val capabilities = manifest.resources.map { it.name } + manifest.types
        if (capabilities.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 56.dp, end = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                capabilities.distinct().forEach { capability ->
                    AssistChip(onClick = {}, label = { Text(capability) })
                }
            }
        }
    }
}

@Composable
private fun AddAddonDialog(onDismiss: () -> Unit) {
    var url by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        icon = { Icon(Icons.Outlined.Extension, contentDescription = null) },
        title = { Text("Add an add-on") },
        text = {
            Column {
                Text(
                    text = "Paste the add-on's manifest URL. A stremio:// install link " +
                        "works too.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Manifest URL") },
                    placeholder = { Text("https://example.com/manifest.json") },
                    singleLine = true,
                    enabled = !busy,
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = url.isNotBlank() && !busy,
                onClick = {
                    busy = true
                    error = null
                    scope.launch {
                        when (val result = AddonRepository.install(url.trim())) {
                            is AddonResult.Success -> onDismiss()
                            is AddonResult.HttpError -> {
                                error = "The add-on answered with ${result.code}."
                                busy = false
                            }
                            is AddonResult.ParseError -> {
                                error = "That address did not return a valid manifest."
                                busy = false
                            }
                            is AddonResult.NetworkError -> {
                                error = "Could not reach that address."
                                busy = false
                            }
                        }
                    }
                },
            ) {
                if (busy) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Add")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") }
        },
    )
}
