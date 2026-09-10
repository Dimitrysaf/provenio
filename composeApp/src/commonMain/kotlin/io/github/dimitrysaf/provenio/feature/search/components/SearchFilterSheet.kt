package io.github.dimitrysaf.provenio.feature.search.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.navigation.SearchFilter

/** A release year is four digits or it is not a year. */
private const val YearLength = 4

/**
 * What a typed query is narrowed by.
 *
 * The year is applied to what comes back rather than sent with the request: the addon
 * protocol's `search` extra carries the query and nothing else, so no addon can be asked
 * for one year in particular.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchFilterSheet(
    selectedType: SearchFilter,
    onSelectType: (SearchFilter) -> Unit,
    year: String,
    onYearChange: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Text(text = "Filters", style = MaterialTheme.typography.titleLarge)

            Text(
                text = "Type",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SearchFilter.entries.forEach { option ->
                    FilterChip(
                        selected = option == selectedType,
                        onClick = { onSelectType(option) },
                        label = { Text(option.label) },
                    )
                }
            }

            Text(
                text = "Release year",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
            )
            OutlinedTextField(
                value = year,
                // Anything that is not a digit cannot be part of a year, so it never
                // reaches the field rather than being rejected after the fact.
                onValueChange = { typed ->
                    onYearChange(typed.filter { it.isDigit() }.take(YearLength))
                },
                placeholder = { Text("Any year") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = {
                        onSelectType(SearchFilter.All)
                        onYearChange("")
                    },
                ) {
                    Text("Clear")
                }
                TextButton(onClick = onDismiss) { Text("Done") }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
