package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.action_back
import provenio.composeapp.generated.resources.action_close

enum class SheetNavigation {
    Close,
    Back,
}

@Composable
internal fun sheetNavigationVisible(): Boolean =
    !usesNativeBottomSheet && LocalWindowBreakpoint.current.isTwoPane

@Composable
fun SheetNavigationButton(
    navigation: SheetNavigation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!sheetNavigationVisible()) return
    IconButton(onClick = onClick, modifier = modifier) {
        when (navigation) {
            SheetNavigation.Close -> Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = stringResource(Res.string.action_close),
            )
            SheetNavigation.Back -> Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(Res.string.action_back),
            )
        }
    }
}

@Composable
fun SheetHeader(
    title: String,
    navigation: SheetNavigation,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    horizontalPadding: Dp = BottomSheetBodyMargin,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val showNavigation = sheetNavigationVisible()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = if (showNavigation && navigation == SheetNavigation.Back) SheetIconEdge else horizontalPadding,
                end = if (showNavigation && navigation == SheetNavigation.Close) SheetIconEdge else horizontalPadding,
            )
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (navigation == SheetNavigation.Back) {
            SheetNavigationButton(navigation = navigation, onClick = onNavigate)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        actions()
        if (navigation == SheetNavigation.Close) {
            SheetNavigationButton(navigation = navigation, onClick = onNavigate)
        }
    }
}

private val SheetIconEdge = 4.dp
