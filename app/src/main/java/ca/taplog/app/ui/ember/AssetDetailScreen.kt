package ca.taplog.app.ui.ember

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ca.taplog.app.data.Asset
import ca.taplog.app.data.AssetDetailSource
import ca.taplog.app.data.Inspection
import ca.taplog.app.data.InspectionResult
import ca.taplog.app.data.OFCCategory
import ca.taplog.app.data.RetireReason
import ca.taplog.app.data.ScanEvent
import ca.taplog.app.data.ScanEventType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetDetailScreen(
    asset: Asset,
    inspections: List<Inspection>,
    scanEvents: List<ScanEvent> = emptyList(),
    siteName: String = "",
    source: AssetDetailSource = AssetDetailSource.FROM_SCAN,
    onStartInspection: () -> Unit,
    onStartReplacement: (RetireReason) -> Unit = {},
    onShareReport: (Inspection) -> Unit = {},
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()) }
    var showReplaceTagDialog by remember { mutableStateOf(false) }
    var scanHistoryExpanded by remember { mutableStateOf(false) }

    if (showReplaceTagDialog) {
        ReplaceTagDialog(
            assetName = asset.name,
            onConfirm = { reason ->
                showReplaceTagDialog = false
                onStartReplacement(reason)
            },
            onDismiss = { showReplaceTagDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = asset.name,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (siteName.isNotBlank()) {
                            Text(
                                text = siteName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (source == AssetDetailSource.FROM_LIST)
                                "Back to Site" else "Home"
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Asset info card
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssetDetailRow("Type", OFCCategory.labelForCode(asset.assetType))
                        AssetDetailRow("Location", asset.location)
                        AssetDetailRow(
                            "Last inspected",
                            asset.lastInspectedAt?.let {
                                dateFormat.format(Date(it))
                            } ?: "Never"
                        )
                        AssetDetailRow(
                            "Next due",
                            asset.nextInspectionDue?.let {
                                val isOverdue = it < System.currentTimeMillis()
                                if (isOverdue) "OVERDUE" else dateFormat.format(Date(it))
                            } ?: "Not scheduled"
                        )
                        AssetDetailRow("Tag ID", asset.nfcTagId)
                    }
                }
            }

            // Action buttons
            item {
                Button(
                    onClick = onStartInspection,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("Start Inspection", style = MaterialTheme.typography.titleMedium)
                }
            }

            if (asset.isActive) {
                item {
                    OutlinedButton(
                        onClick = { showReplaceTagDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Replace Tag", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Inspection history
            item {
                Text(
                    "Inspection History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (inspections.isEmpty()) {
                item {
                    Text(
                        "No inspections recorded yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(inspections) { inspection ->
                    InspectionHistoryCard(
                        inspection = inspection,
                        dateFormat = dateFormat,
                        onShareReport = { onShareReport(inspection) }
                    )
                }
            }

            // Scan history (collapsible)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Scan History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = { scanHistoryExpanded = !scanHistoryExpanded }) {
                        Text(if (scanHistoryExpanded) "Hide" else "Show (${scanEvents.size})")
                    }
                }
            }

            if (scanHistoryExpanded) {
                if (scanEvents.isEmpty()) {
                    item {
                        Text(
                            "No scans recorded yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(scanEvents.take(20)) { event ->
                        ScanEventRow(event, dateFormat)
                    }
                }
            }
        }
    }
}

@Composable
fun ScanEventRow(event: ScanEvent, dateFormat: SimpleDateFormat) {
    val typeColor = when (event.eventType) {
        ScanEventType.INSPECTION -> MaterialTheme.colorScheme.primary
        ScanEventType.BROWSE -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                dateFormat.format(Date(event.scannedAt)),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                event.inspectorName.ifBlank { "Unknown inspector" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            event.eventType.name,
            style = MaterialTheme.typography.labelSmall,
            color = typeColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun ReplaceTagDialog(
    assetName: String,
    onConfirm: (RetireReason) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedReason by remember { mutableStateOf<RetireReason?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Replace Tag") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Why is the tag on \"$assetName\" being replaced?",
                    style = MaterialTheme.typography.bodyMedium
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    RetireReason.entries.forEach { reason ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = selectedReason == reason,
                                onClick = { selectedReason = reason }
                            )
                            Text(
                                reason.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                if (selectedReason != null) {
                    Text(
                        "After confirming, scan the new NFC tag.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedReason?.let { onConfirm(it) } },
                enabled = selectedReason != null
            ) {
                Text("Scan New Tag")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AssetDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun InspectionHistoryCard(
    inspection: Inspection,
    dateFormat: SimpleDateFormat,
    onShareReport: () -> Unit = {}
) {
    val resultColor = when (inspection.result) {
        InspectionResult.PASS -> MaterialTheme.colorScheme.primary
        InspectionResult.FAIL -> MaterialTheme.colorScheme.error
        InspectionResult.REQUIRES_ATTENTION -> MaterialTheme.colorScheme.tertiary
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    dateFormat.format(Date(inspection.inspectedAt)),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    inspection.result.name.replace("_", " "),
                    style = MaterialTheme.typography.labelMedium,
                    color = resultColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                inspection.inspectorName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            inspection.notes?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(
                onClick = onShareReport,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Share Report", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
