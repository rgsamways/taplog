package ca.taplog.app.ui.ember

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ca.taplog.app.data.DeficiencyWithAsset
import ca.taplog.app.data.DeficiencySeverity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenDeficienciesScreen(
    deficiencies: List<DeficiencyWithAsset>,
    onResolve: (deficiencyId: String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Open Deficiencies",
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "${deficiencies.size} unresolved",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (deficiencies.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No open deficiencies.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(deficiencies, key = { it.id }) { deficiency ->
                    DeficiencyWithAssetCard(
                        deficiency = deficiency,
                        onResolve = { onResolve(deficiency.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun DeficiencyWithAssetCard(
    deficiency: DeficiencyWithAsset,
    onResolve: () -> Unit
) {
    val severityColor = when (deficiency.severity) {
        DeficiencySeverity.CRITICAL -> MaterialTheme.colorScheme.error
        DeficiencySeverity.HIGH -> MaterialTheme.colorScheme.errorContainer
        DeficiencySeverity.MEDIUM -> MaterialTheme.colorScheme.tertiary
        DeficiencySeverity.LOW -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    var thumbnail by remember(deficiency.photoPath) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(deficiency.photoPath) {
        deficiency.photoPath?.let { path ->
            thumbnail = withContext(Dispatchers.IO) {
                val opts = BitmapFactory.Options().apply { inSampleSize = 8 }
                BitmapFactory.decodeFile(path, opts)
            }
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    deficiency.severity.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = severityColor
                )
                Text(
                    deficiency.code,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        deficiency.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "${deficiency.assetName} — ${deficiency.buildingName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                thumbnail?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Deficiency photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(
                onClick = onResolve,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Mark Resolved")
            }
        }
    }
}