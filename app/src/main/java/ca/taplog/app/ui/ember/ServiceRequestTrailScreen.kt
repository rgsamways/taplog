package ca.taplog.app.ui.ember

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ca.taplog.app.data.Asset
import ca.taplog.app.data.ServiceRequest
import ca.taplog.app.data.ServiceRequestStatus
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceRequestTrailScreen(
    asset: Asset,
    serviceRequests: List<ServiceRequest>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Service Requests", fontWeight = FontWeight.SemiBold)
                        Text(asset.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        Toast.makeText(context, "PDF export coming soon", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF")
                    }
                }
            )
        }
    ) { padding ->
        if (serviceRequests.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No service requests recorded",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(serviceRequests) { request ->
                    ServiceRequestRow(request = request, dateFormat = dateFormat)
                }
            }
        }
    }
}

@Composable
private fun ServiceRequestRow(request: ServiceRequest, dateFormat: SimpleDateFormat) {
    val isNoResponse = request.status == ServiceRequestStatus.NO_RESPONSE
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isNoResponse)
                MaterialTheme.colorScheme.errorContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    dateFormat.format(Date(request.sentAtMs)),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isNoResponse) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                StatusBadge(status = request.status)
            }
            Text(
                request.contractorName ?: "Unknown contractor",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = if (isNoResponse) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
            )
            if (!request.contractorPhone.isNullOrBlank()) {
                Text(
                    request.contractorPhone,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isNoResponse) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!request.notes.isNullOrBlank()) {
                Text(
                    request.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isNoResponse) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isNoResponse) {
                Text(
                    "Liability transferred — contractor did not respond",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(status: ServiceRequestStatus) {
    val (label, color) = when (status) {
        ServiceRequestStatus.SENT -> "Sent" to MaterialTheme.colorScheme.secondary
        ServiceRequestStatus.ACKNOWLEDGED -> "Acknowledged" to MaterialTheme.colorScheme.primary
        ServiceRequestStatus.SCHEDULED -> "Scheduled" to MaterialTheme.colorScheme.primary
        ServiceRequestStatus.COMPLETED -> "Completed" to MaterialTheme.colorScheme.tertiary
        ServiceRequestStatus.NO_RESPONSE -> "No response" to MaterialTheme.colorScheme.error
    }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = color
        )
    }
}
