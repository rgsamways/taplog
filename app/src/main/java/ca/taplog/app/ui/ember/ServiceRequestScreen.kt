package ca.taplog.app.ui.ember

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ca.taplog.app.data.Asset
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceRequestScreen(
    asset: Asset,
    onSend: (contractorName: String?, contractorPhone: String?, contractorEmail: String?, notes: String?) -> Unit,
    onCancel: () -> Unit
) {
    var contractorName by remember { mutableStateOf("") }
    var contractorPhone by remember { mutableStateOf("") }
    var contractorEmail by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var urgency by remember { mutableStateOf("Routine") }
    val urgencyOptions = listOf("Routine", "Urgent", "Critical")
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val overdueSince = asset.nextInspectionDue?.let { dateFormat.format(Date(it)) } ?: "unknown date"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Request Inspection", fontWeight = FontWeight.SemiBold)
                        Text(
                            asset.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // Overdue indicator
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Asset overdue since $overdueSince",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        "This request will be recorded with a timestamp. If no response is received within 7 days, the record will automatically note non-compliance by the contractor.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Urgency selector
            Text("Urgency", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                urgencyOptions.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = urgency == option,
                        onClick = { urgency = option },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = urgencyOptions.size)
                    ) { Text(option) }
                }
            }

            Text("Contractor contact (optional)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            OutlinedTextField(
                value = contractorName,
                onValueChange = { contractorName = it },
                label = { Text("Contractor name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = contractorPhone,
                onValueChange = { contractorPhone = it },
                label = { Text("Phone") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = contractorEmail,
                onValueChange = { contractorEmail = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes / what needs to be done") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    val combinedNotes = buildString {
                        append("[$urgency]")
                        if (notes.isNotBlank()) append(" $notes")
                    }
                    onSend(
                        contractorName.takeIf { it.isNotBlank() },
                        contractorPhone.takeIf { it.isNotBlank() },
                        contractorEmail.takeIf { it.isNotBlank() },
                        combinedNotes
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Send Request")
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
