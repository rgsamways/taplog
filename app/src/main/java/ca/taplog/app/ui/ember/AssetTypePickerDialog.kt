package ca.taplog.app.ui.ember

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ca.taplog.app.data.OFCAssetType
import ca.taplog.app.data.OFCCategory
import kotlinx.coroutines.launch

@Composable
fun AssetTypePickerDialog(
    onTypeSelected: (OFCAssetType) -> Unit,
    onDismiss: () -> Unit,
    suggestedCode: String? = null
) {
    val initialCategory = remember(suggestedCode) {
        if (suggestedCode != null) {
            OFCCategory.entries.find { cat -> cat.types.any { it.code == suggestedCode } }
                ?: OFCCategory.entries.first()
        } else OFCCategory.entries.first()
    }
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var searchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val displayedTypes = remember(searchQuery, selectedCategory) {
        if (searchQuery.isBlank()) {
            selectedCategory.types
        } else {
            val q = searchQuery.trim().lowercase()
            OFCCategory.entries.flatMap { it.types }.filter { type ->
                type.label.lowercase().contains(q) ||
                        type.code.lowercase().contains(q) ||
                        type.description.lowercase().contains(q)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Asset Type") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {

                // ── Search field ───────────────────────────────────────────
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search") },
                    placeholder = { Text("e.g. red cylinder, kitchen, smoke…") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                // ── Category chips — hidden while searching ────────────────
                if (searchQuery.isBlank()) {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        items(OFCCategory.entries) { category ->
                            FilterChip(
                                selected = category == selectedCategory,
                                onClick = { selectedCategory = category },
                                label = {
                                    Text(
                                        category.label,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            )
                        }
                    }
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = selectedCategory.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                } else {
                    Text(
                        text = if (displayedTypes.isEmpty()) "No results"
                        else "${displayedTypes.size} result${if (displayedTypes.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                // ── Asset type list ────────────────────────────────────────
                LaunchedEffect(suggestedCode, displayedTypes) {
                    if (suggestedCode != null) {
                        val idx = displayedTypes.indexOfFirst { it.code == suggestedCode }
                        if (idx >= 0) scope.launch { listState.animateScrollToItem(idx) }
                    }
                }
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                ) {
                    items(displayedTypes) { assetType ->
                        AssetTypeRow(
                            assetType = assetType,
                            isSuggested = assetType.code == suggestedCode,
                            onClick = { onTypeSelected(assetType) }
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssetTypeRow(
    assetType: OFCAssetType,
    isSuggested: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = assetType.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (isSuggested) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("AI suggested", style = MaterialTheme.typography.labelSmall) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
            Text(
                text = assetType.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(
                text = assetType.code,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Text(
            text = intervalLabel(assetType.inspectionIntervalMonths),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp, top = 2.dp)
        )
    }
}

private fun intervalLabel(months: Int): String = when (months) {
    6    -> "6 mo"
    12   -> "Annual"
    else -> "$months mo"
}
