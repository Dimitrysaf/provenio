package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.shell.nav.LocalNativeNavigationBarHidden
import io.github.dimitrysaf.provenio.shell.nav.LocalUseNativeNavigation
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.action_back
import org.jetbrains.compose.resources.stringResource

// A filled Material icon button holding the back arrow; callers may retint it over artwork.
@Composable
fun BackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = IconButtonDefaults.filledShape,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    buttonSize: Dp = 40.dp,
    iconSize: Dp = 24.dp,
    contentDescription: String = stringResource(Res.string.action_back),
) {
    if (LocalUseNativeNavigation.current && !LocalNativeNavigationBarHidden.current) return

    FilledIconButton(
        onClick = onClick,
        modifier = modifier.size(buttonSize),
        shape = shape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
        )
    }
}
