package io.github.dimitrysaf.provenio.pages

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Tune
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.stremio.AddonRepository
import io.github.dimitrysaf.provenio.stremio.AddonUrl
import io.github.dimitrysaf.provenio.stremio.AddonResult
import io.github.dimitrysaf.provenio.stremio.InstalledAddon
import io.github.dimitrysaf.provenio.stremio.model.Manifest
import io.github.dimitrysaf.provenio.ui.rememberUrlOpener
import com.alorma.compose.settings.ui.expressive.SettingsGroup
import io.github.dimitrysaf.provenio.ui.components.SettingsTile
import io.github.dimitrysaf.provenio.ui.components.SettingsTileSpacing
import io.github.dimitrysaf.provenio.ui.components.TilePosition
import io.github.dimitrysaf.provenio.ui.components.tilePositionOf
import kotlinx.coroutines.launch

/**
 * The Add-ons settings themselves. No top bar or scroll container of its own — SettingsPage
 * owns those — so adding an addon is an inline action rather than a top bar button that
 * would only exist in one of the two layouts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddonsContent() {
    val collection by AddonRepository.collection.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    val addons = collection.all

    Text(
        text = if (addons.isEmpty()) {
            "Add-ons supply the catalogs, metadata and streams."
        } else {
            "Add-ons are asked in this order."
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )

    // One block: the installed add-ons, with adding one as its last row rather than a
    // button floating on the page behind them.
    val rowCount = addons.size + 1
    SettingsGroup(verticalArrangement = Arrangement.spacedBy(SettingsTileSpacing)) {
        addons.forEachIndexed { index, addon ->
            AddonRow(
                addon = addon,
                isFirst = index == 0,
                isLast = index == addons.lastIndex,
                position = tilePositionOf(index, rowCount),
            )
        }
        SettingsTile(
            title = { Text("Add an add-on") },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            position = tilePositionOf(addons.size, rowCount),
            onClick = { showAddDialog = true },
        )
    }

    if (showAddDialog) {
        AddAddonDialog(onDismiss = { showAddDialog = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddonRow(
    addon: InstalledAddon,
    isFirst: Boolean,
    isLast: Boolean,
    position: TilePosition,
) {
    val manifest = addon.manifest
    var menuOpen by remember { mutableStateOf(false) }
    val openUrl = rememberUrlOpener()

    SettingsTile(
        title = { Text(manifest.name) },
        subtitle = {
            Column {
                Text(
                    text = manifest.description ?: "Version ${manifest.version}",
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                // What the addon actually provides, which is the thing worth seeing at a
                // glance when deciding its priority.
                val capabilities = (manifest.resources.map { it.name } + manifest.types).distinct()
                if (capabilities.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        capabilities.forEach { capability ->
                            AssistChip(onClick = {}, label = { Text(capability) })
                        }
                    }
                }
            }
        },
        icon = { Icon(Icons.Outlined.Extension, contentDescription = null) },
        position = position,
        action = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = addon.enabled, onCheckedChange = null)
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = "More options for ${manifest.name}",
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        if (manifest.behaviorHints?.configurable == true) {
                            DropdownMenuItem(
                                text = { Text("Configure") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Tune, contentDescription = null)
                                },
                                onClick = {
                                    openUrl(AddonUrl.configure(addon.transportUrl))
                                    menuOpen = false
                                },
                            )
                        }
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
                                Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null)
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
        onClick = { AddonRepository.setEnabled(manifest.id, !addon.enabled) },
    )
}

@Composable
private fun AddAddonDialog(onDismiss: () -> Unit) {
    var url by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    // Set when the fetched manifest says the addon cannot run until it is configured.
    var needsConfiguration by remember { mutableStateOf<Manifest?>(null) }
    val scope = rememberCoroutineScope()
    val openUrl = rememberUrlOpener()

    val pending = needsConfiguration
    if (pending != null) {
        ConfigurationRequiredDialog(
            manifest = pending,
            onConfigure = { openUrl(AddonUrl.configure(url.trim())) },
            onDismiss = onDismiss,
        )
        return
    }

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
                        when (val result = AddonRepository.inspect(url.trim())) {
                            is AddonResult.Success -> {
                                val manifest = result.value
                                if (manifest.behaviorHints?.configurationRequired == true) {
                                    needsConfiguration = manifest
                                } else {
                                    AddonRepository.add(url.trim(), manifest)
                                    onDismiss()
                                }
                            }
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

/**
 * An addon with `configurationRequired` cannot work until the user has filled in its own
 * form, and configuring it produces a different manifest URL — so it is not installed
 * here. The user is sent to the addon's page and comes back with the configured link.
 */
@Composable
private fun ConfigurationRequiredDialog(
    manifest: Manifest,
    onConfigure: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Tune, contentDescription = null) },
        title = { Text("${manifest.name} needs configuring") },
        text = {
            Text(
                text = "This add-on cannot be used until it is set up. Opening its " +
                    "configuration page will give you a personalised install link — paste " +
                    "that link here instead.",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfigure) { Text("Open configuration") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
