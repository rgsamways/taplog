package ca.taplog.app.ui.ember

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ca.taplog.app.data.Site
import kotlinx.coroutines.flow.Flow

private enum class ViewMode { LIST, MAP }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteListScreen(
    sitesFlow: Flow<List<Site>>?,
    organisationName: String,
    deficiencyCount: Int = 0,
    onSiteSelected: (Site) -> Unit,
    onAddSite: () -> Unit,
    onShowDeficiencies: () -> Unit,
    onGeocodeUnresolved: (List<Site>) -> Unit = {},
    onBack: (() -> Unit)? = null,
    isEmbedded: Boolean = false
) {
    val sites by (sitesFlow ?: return).collectAsState(initial = emptyList())
    var viewMode by remember { mutableStateOf(ViewMode.LIST) }
    var geocodeTriggered by remember { mutableStateOf(false) }

    val modifier = if (isEmbedded) Modifier.heightIn(max = 320.dp) else Modifier

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                title = {
                    Column {
                        Text(
                            text = organisationName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${sites.size} site${if (sites.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (deficiencyCount > 0) {
                        IconButton(onClick = onShowDeficiencies) {
                            BadgedBox(
                                badge = { Badge { Text(deficiencyCount.toString()) } }
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = "$deficiencyCount open deficiencies",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    IconButton(onClick = { viewMode = ViewMode.LIST }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ViewList,
                            contentDescription = "List view",
                            tint = if (viewMode == ViewMode.LIST)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { viewMode = ViewMode.MAP }) {
                        Icon(
                            Icons.Default.Map,
                            contentDescription = "Map view",
                            tint = if (viewMode == ViewMode.MAP)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (viewMode == ViewMode.LIST && !isEmbedded) {
                FloatingActionButton(onClick = onAddSite) {
                    Icon(Icons.Default.Add, contentDescription = "Add Site")
                }
            }
        }
    ) { padding ->
        when (viewMode) {
            ViewMode.MAP -> {
                // Trigger lazy geocoding once on first map open
                LaunchedEffect(Unit) {
                    if (!geocodeTriggered) {
                        geocodeTriggered = true
                        onGeocodeUnresolved(sites)
                    }
                }
                Box(modifier = Modifier.padding(padding)) {
                    SiteMapView(sites = sites, onSiteSelected = onSiteSelected)
                }
            }

            ViewMode.LIST -> {
                if (sites.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No sites yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap + to add your first site",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(sites, key = { it.id }) { site ->
                            SiteCard(site = site, onClick = { onSiteSelected(site) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SiteCard(site: Site, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = site.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${site.city}, ${site.province}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                site.clientName?.let {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
