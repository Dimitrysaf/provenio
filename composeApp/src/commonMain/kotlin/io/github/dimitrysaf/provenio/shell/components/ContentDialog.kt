package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** A Material 3 dialog laid out like AlertDialog, for bodies and action rows its slots cannot hold. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ContentDialog(
    onDismissRequest: () -> Unit,
    title: String,
    buttons: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    BasicAlertDialog(onDismissRequest = onDismissRequest, modifier = modifier) {
        Surface(
            shape = AlertDialogDefaults.shape,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = AlertDialogDefaults.titleContentColor,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                CompositionLocalProvider(
                    LocalContentColor provides AlertDialogDefaults.textContentColor,
                    LocalTextStyle provides MaterialTheme.typography.bodyMedium,
                ) {
                    Column(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        content = content,
                    )
                }
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    content = { buttons() },
                )
            }
        }
    }
}
