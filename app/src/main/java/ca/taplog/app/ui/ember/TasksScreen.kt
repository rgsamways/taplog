package ca.taplog.app.ui.ember

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ca.taplog.app.data.Asset
import ca.taplog.app.data.DeficiencySeverity
import ca.taplog.app.ui.ember.EmberViewModel.Task
import ca.taplog.app.ui.ember.EmberViewModel.TaskType
import ca.taplog.app.ui.theme.TapLogDanger
import ca.taplog.app.ui.theme.TapLogTeal400
import ca.taplog.app.ui.theme.TapLogWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    tasks: List<Task>,
    onAssetSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    var activeFilter by remember { mutableStateOf(TaskFilter.ALL) }

    val filteredTasks = when (activeFilter) {
        TaskFilter.ALL -> tasks
        TaskFilter.DEFICIENCIES -> tasks.filter { it.type == TaskType.OPEN_DEFICIENCY }
        TaskFilter.OVERDUE -> tasks.filter { it.type == TaskType.OVERDUE_ASSET }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Tasks", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(TaskFilter.entries) { filter ->
                        FilterChip(
                            selected = activeFilter == filter,
                            onClick = { activeFilter = filter },
                            label = { Text(filter.label) }
                        )
                    }
                }
            }

            if (filteredTasks.isEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = TapLogTeal400)
                        Text("No open tasks", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                items(filteredTasks, key = { it.id }) { task ->
                    TaskRow(
                        task = task,
                        onClick = { onAssetSelected(task.id) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

enum class TaskFilter(val label: String) {
    ALL("All"),
    DEFICIENCIES("Deficiencies"),
    OVERDUE("Overdue")
}

@Composable
fun TaskRow(
    task: Task,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (task.type == TaskType.OPEN_DEFICIENCY)
                    Icons.Default.Warning else Icons.Default.Warning,
                contentDescription = null,
                tint = when (task.type) {
                    TaskType.OPEN_DEFICIENCY -> severityColor(task.severity)
                    TaskType.OVERDUE_ASSET -> TapLogDanger
                },
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (task.siteName.isNotBlank()) {
                    Text(
                        text = task.siteName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            when (task.type) {
                TaskType.OPEN_DEFICIENCY -> task.severity?.let { sev ->
                    Badge(containerColor = severityColor(sev)) {
                        Text(sev.name.lowercase(), style = MaterialTheme.typography.labelSmall)
                    }
                }
                TaskType.OVERDUE_ASSET -> task.daysOverdue?.let { days ->
                    Badge(containerColor = TapLogDanger) {
                        Text("${days}d", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

fun severityColor(severity: DeficiencySeverity?): androidx.compose.ui.graphics.Color = when (severity) {
    DeficiencySeverity.CRITICAL -> TapLogDanger
    DeficiencySeverity.HIGH -> TapLogWarning
    DeficiencySeverity.MEDIUM -> androidx.compose.ui.graphics.Color(0xFF4A7CB5)
    DeficiencySeverity.LOW -> androidx.compose.ui.graphics.Color(0xFF888888)
    null -> TapLogWarning
}
