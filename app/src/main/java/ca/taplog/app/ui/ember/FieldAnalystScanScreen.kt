package ca.taplog.app.ui.ember

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ca.taplog.app.data.Asset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldAnalystScanScreen(
    siteName: String,
    assetCount: Int,
    assetsThisVisit: List<Asset>,
    inlineAsset: Asset?,
    onFinishVisit: () -> Unit,
    onAddManually: () -> Unit,
    onDismissInlineAsset: () -> Unit
) {
    val teal = MaterialTheme.colorScheme.primary
    val infiniteTransition = rememberInfiniteTransition(label = "nfc_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse_alpha"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(siteName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                        Text("$assetCount assets", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    TextButton(onClick = onFinishVisit) { Text("Finish Visit") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddManually) {
                Icon(Icons.Default.Add, contentDescription = "Add manually")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Inline asset card (shown after registered tag tap)
            if (inlineAsset != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(inlineAsset.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                            Text(inlineAsset.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Already registered", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        TextButton(onClick = onDismissInlineAsset) { Text("Dismiss") }
                    }
                }
            }

            // NFC pulse area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(160.dp)) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    for (i in 1..3) {
                        val r = (40f + i * 28f) * (size.width / 160.dp.toPx())
                        drawCircle(
                            color = teal.copy(alpha = pulseAlpha * (0.8f - i * 0.2f)),
                            radius = r,
                            center = androidx.compose.ui.geometry.Offset(cx, cy),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                    drawCircle(color = teal, radius = 24.dp.toPx(), center = androidx.compose.ui.geometry.Offset(cx, cy))
                }
            }

            Text(
                "Tap a tag to register it",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            // Asset list this visit
            if (assetsThisVisit.isNotEmpty()) {
                Text(
                    "This visit",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(assetsThisVisit) { asset ->
                        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(12.dp)) {
                                Column {
                                    Text(asset.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                                    Text(asset.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            } else {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}
