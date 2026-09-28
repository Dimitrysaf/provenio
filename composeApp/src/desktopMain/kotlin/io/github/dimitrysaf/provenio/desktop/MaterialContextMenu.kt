package io.github.dimitrysaf.provenio.desktop

import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ContextMenuRepresentation
import androidx.compose.foundation.ContextMenuState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.rememberPopupPositionProviderAtPosition

internal object MaterialContextMenuRepresentation : ContextMenuRepresentation {
    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    override fun Representation(state: ContextMenuState, items: () -> List<ContextMenuItem>) {
        val status = state.status
        if (status !is ContextMenuState.Status.Open) return
        val menuItems = items()
        if (menuItems.isEmpty()) {
            SideEffect { state.status = ContextMenuState.Status.Closed }
            return
        }
        Popup(
            popupPositionProvider = rememberPopupPositionProviderAtPosition(status.rect.center),
            onDismissRequest = { state.status = ContextMenuState.Status.Closed },
            properties = PopupProperties(focusable = true),
        ) {
            Surface(
                shape = MenuDefaults.shape,
                color = MenuDefaults.containerColor,
                tonalElevation = MenuDefaults.TonalElevation,
                shadowElevation = MenuDefaults.ShadowElevation,
            ) {
                Column(
                    modifier = Modifier
                        .width(IntrinsicSize.Max)
                        .widthIn(min = 112.dp, max = 280.dp)
                        .padding(vertical = 8.dp),
                ) {
                    menuItems.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item.label) },
                            onClick = {
                                state.status = ContextMenuState.Status.Closed
                                item.onClick()
                            },
                            enabled = item.enabled,
                        )
                    }
                }
            }
        }
    }
}
