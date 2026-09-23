package com.nuvio.app.features.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nuvio.app.shell.components.BottomSheetBodyMargin
import com.nuvio.app.shell.components.NuvioLoadingIndicator
import com.nuvio.app.shell.components.NuvioModalBottomSheet
import com.nuvio.app.shell.components.dismissNuvioBottomSheet
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_cancel
import nuvio.composeapp.generated.resources.action_ok
import nuvio.composeapp.generated.resources.settings_appearance_app_icon_android_confirmation_message
import nuvio.composeapp.generated.resources.settings_appearance_app_icon_android_confirmation_title
import nuvio.composeapp.generated.resources.settings_appearance_app_icon_change_failed
import nuvio.composeapp.generated.resources.settings_appearance_app_icon_sheet_title
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.nuvio.app.core.settings.AppIconOption
import com.nuvio.app.core.settings.AppIconPlatform
import com.nuvio.app.core.settings.AppIconSettingsState
import com.nuvio.app.core.settings.labelResource
import com.nuvio.app.core.settings.previewResource

@Composable
internal fun AppIconPicker(
    state: AppIconSettingsState,
    onSelected: (AppIconOption) -> Unit,
    onDismiss: () -> Unit,
) {
    var confirmationIcon by remember { mutableStateOf<AppIconOption?>(null) }
    val requestSelection: (AppIconOption) -> Unit = { icon ->
        if (icon != state.selected) {
            if (AppIconPlatform.requiresCloseConfirmation) {
                confirmationIcon = icon
            } else {
                onSelected(icon)
            }
        }
    }

    AppIconPickerBottomSheet(
        state = state,
        onSelected = requestSelection,
        onDismiss = onDismiss,
    )

    confirmationIcon?.let { icon ->
        // Swapping the icon on Android restarts the app, so the choice is confirmed first. A
        // question that interrupts and expects an answer is what a dialog is for, at every size.
        AlertDialog(
            onDismissRequest = { confirmationIcon = null },
            title = {
                Text(stringResource(Res.string.settings_appearance_app_icon_android_confirmation_title))
            },
            text = {
                Text(
                    stringResource(
                        Res.string.settings_appearance_app_icon_android_confirmation_message,
                        stringResource(icon.labelResource),
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmationIcon = null
                        onSelected(icon)
                    },
                ) {
                    Text(stringResource(Res.string.action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmationIcon = null }) {
                    Text(stringResource(Res.string.action_cancel))
                }
            },
        )
    }
}

/**
 * The icon choices, in a modal bottom sheet at every window size.
 *
 * m3.material.io/components/bottom-sheets/specs
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppIconPickerBottomSheet(
    state: AppIconSettingsState,
    onSelected: (AppIconOption) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    fun dismiss() {
        scope.launch {
            dismissNuvioBottomSheet(sheetState = sheetState, onDismiss = onDismiss)
        }
    }

    NuvioModalBottomSheet(
        onDismissRequest = ::dismiss,
        sheetState = sheetState,
    ) {
        AppIconPickerContent(
            state = state,
            onSelected = onSelected,
            modifier = Modifier.navigationBarsPadding(),
        )
    }
}

@Composable
private fun AppIconPickerContent(
    state: AppIconSettingsState,
    onSelected: (AppIconOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // A sheet stops growing at 640dp, so it is either a phone-width sheet or that maximum.
        // Three tiles fit comfortably at the wide end, two at the narrow one.
        val columns = if (maxWidth >= 480.dp) 3 else 2

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 720.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = BottomSheetBodyMargin)
                .padding(bottom = BottomSheetBodyMargin),
        ) {
            Text(
                text = stringResource(Res.string.settings_appearance_app_icon_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = BottomSheetBodyMargin),
            )

            if (state.changeFailed) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ) {
                    Text(
                        text = stringResource(Res.string.settings_appearance_app_icon_change_failed),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(modifier = Modifier.height(BottomSheetBodyMargin))
            }

            Column(verticalArrangement = Arrangement.spacedBy(AppIconTileSpacing)) {
                AppIconOption.entries.chunked(columns).forEach { rowIcons ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppIconTileSpacing),
                    ) {
                        rowIcons.forEach { icon ->
                            AppIconChoice(
                                icon = icon,
                                selected = state.selected == icon,
                                pending = state.pending == icon,
                                enabled = state.pending == null,
                                onClick = { onSelected(icon) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        // Keep a short last row's tiles the same width as every other row's.
                        repeat(columns - rowIcons.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

private val AppIconTileSpacing = 12.dp

/**
 * One icon choice: the artwork, its name, and a badge marking the active one.
 *
 * The tile is a filled card on the sheet's surface, and the chosen one takes the primary container
 * pair — the same selection treatment the expressive list spec uses for a selected row.
 *
 * m3.material.io/components/cards/specs
 */
@Composable
private fun AppIconChoice(
    icon: AppIconOption,
    selected: Boolean,
    pending: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.semantics { this.selected = selected },
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box {
                AppIconThumbnail(
                    icon = icon,
                    modifier = Modifier.size(78.dp),
                )
                if (selected || pending) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(24.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (pending) {
                                NuvioLoadingIndicator(
                                    size = 20.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(icon.labelResource),
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
internal fun AppIconThumbnail(
    icon: AppIconOption,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    cornerRadius: Dp = 18.dp,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Image(
        painter = painterResource(icon.previewResource),
        contentDescription = contentDescription,
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = shape,
            ),
        contentScale = ContentScale.Fit,
    )
}
