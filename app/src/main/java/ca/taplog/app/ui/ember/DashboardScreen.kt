package ca.taplog.app.ui.ember

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ca.taplog.app.data.Site
import ca.taplog.app.ui.ember.EmberViewModel.CalendarEvent
import ca.taplog.app.ui.ember.EmberViewModel.CalendarUrgency
import ca.taplog.app.ui.ember.EmberViewModel.DashboardStats
import ca.taplog.app.ui.ember.EmberViewModel.SiteContact
import ca.taplog.app.ui.ember.EmberViewModel.SiteWithOverdueCount
import ca.taplog.app.ui.ember.EmberViewModel.Task
import ca.taplog.app.ui.theme.TapLogNavy700
import ca.taplog.app.ui.theme.TapLogTeal200
import ca.taplog.app.ui.theme.TapLogTeal400
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    organisationName: String,
    dashboardStats: DashboardStats,
    overdueSites: List<SiteWithOverdueCount>,
    sitesFlow: Flow<List<Site>>?,
    calendarEvents: Map<LocalDate, List<CalendarEvent>>,
    tasks: List<Task>,
    contacts: List<SiteContact>,
    onScan: () -> Unit,
    onAddSite: () -> Unit,
    onShowDeficiencies: () -> Unit,
    onSiteSelected: (Site) -> Unit,
    onOverdueSiteTapped: (Site) -> Unit,
    onShowAllSites: () -> Unit,
    onGeocodeUnresolved: (List<Site>) -> Unit,
    onShowCalendar: () -> Unit,
    onShowTasks: () -> Unit,
    onShowContacts: () -> Unit,
    onAssetSelected: (String) -> Unit
) {
    // Collect sites at composable top — safe, consistent with rest of the app
    val sites by (sitesFlow ?: return).collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = organisationName.ifBlank { "TapLog" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {

            // ── Stats strip ────────────────────────────────────────────────
            item {
                StatsStrip(
                    stats = dashboardStats,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            // ── Quick actions ──────────────────────────────────────────────
            item {
                QuickActionsSection(
                    onScan = onScan,
                    onAddSite = onAddSite,
                    onShowDeficiencies = onShowDeficiencies,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // ── Overdue & Urgent ───────────────────────────────────────────
            item {
                SectionHeader(
                    title = "Overdue & Urgent",
                    badge = if (overdueSites.isNotEmpty()) overdueSites.size.toString() else null,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
                )
            }

            if (overdueSites.isEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = TapLogTeal400,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "All sites are current",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(overdueSites, key = { it.site.id }) { item ->
                    SiteOverdueRow(
                        item = item,
                        onClick = { onOverdueSiteTapped(item.site) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 3.dp)
                    )
                }
            }

            // ── Sites preview ──────────────────────────────────────────────
            item {
                SectionHeader(
                    title = "Sites",
                    actionLabel = "See all",
                    onAction = onShowAllSites,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
                )
            }

            if (sites.isEmpty()) {
                item {
                    Text(
                        text = "No sites yet — tap Add Site to get started",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            } else {
                items(sites.take(4), key = { "preview_${it.id}" }) { site ->
                    SiteCard(
                        site = site,
                        onClick = { onSiteSelected(site) }
                    )
                }
                if (sites.size > 4) {
                    item {
                        TextButton(
                            onClick = onShowAllSites,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Text("+ ${sites.size - 4} more sites")
                        }
                    }
                }
            }

            // ── Calendar section ───────────────────────────────────────────
            item {
                CalendarSection(
                    calendarEvents = calendarEvents,
                    onShowCalendar = onShowCalendar,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // ── Tasks section ──────────────────────────────────────────────
            item {
                TasksSection(
                    tasks = tasks,
                    onShowTasks = onShowTasks,
                    onAssetSelected = onAssetSelected,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // ── Contacts section ───────────────────────────────────────────
            item {
                ContactsSection(
                    contacts = contacts,
                    onShowContacts = onShowContacts,
                    onSiteSelected = { /* handled in MainActivity */ },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

// ── StatsStrip ────────────────────────────────────────────────────────────

@Composable
fun StatsStrip(stats: DashboardStats, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(value = stats.inspectionsThisMonth.toString(), label = "This month",    modifier = Modifier.weight(1f))
        StatCard(value = stats.openDeficiencies.toString(),     label = "Deficiencies",  modifier = Modifier.weight(1f))
        StatCard(value = stats.overdueSiteCount.toString(),     label = "Sites overdue", modifier = Modifier.weight(1f))
        StatCard(value = stats.totalSites.toString(),           label = "Sites",         modifier = Modifier.weight(1f))
    }
}

@Composable
fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = TapLogNavy700)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TapLogTeal200
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

// ── QuickActionsSection ───────────────────────────────────────────────────

@Composable
fun QuickActionsSection(
    onScan: () -> Unit,
    onAddSite: () -> Unit,
    onShowDeficiencies: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = onScan,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TapLogTeal400),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Scan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = onAddSite,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add Site")
            }
            OutlinedButton(
                onClick = onShowDeficiencies,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Deficiencies")
            }
        }
    }
}

// ── Section header ────────────────────────────────────────────────────────

@Composable
fun SectionHeader(
    title: String,
    badge: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        if (badge != null) {
            Badge(containerColor = MaterialTheme.colorScheme.error) {
                Text(badge, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.width(8.dp))
        }
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction, contentPadding = PaddingValues(0.dp)) {
                Text(actionLabel, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

// ── SiteOverdueRow ────────────────────────────────────────────────────────

@Composable
fun SiteOverdueRow(
    item: SiteWithOverdueCount,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.site.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = item.mostOverdueAssetName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Badge(containerColor = MaterialTheme.colorScheme.error) {
                    Text("${item.overdueCount} overdue", style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${item.mostOverdueDays}d ago",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// ── CalendarSection ───────────────────────────────────────────────────────

@Composable
fun CalendarSection(
    calendarEvents: Map<LocalDate, List<CalendarEvent>>,
    onShowCalendar: () -> Unit,
    modifier: Modifier = Modifier
) {
    var displayedMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = "Calendar",
            actionLabel = "See all",
            onAction = onShowCalendar,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
        )
        MonthCalendarGrid(
            month = displayedMonth,
            calendarEvents = calendarEvents,
            selectedDay = selectedDay,
            onDaySelected = { day -> selectedDay = if (selectedDay == day) null else day },
            onPrevMonth = { displayedMonth = displayedMonth.minusMonths(1) },
            onNextMonth = { displayedMonth = displayedMonth.plusMonths(1) }
        )
    }
}

// ── TasksSection ──────────────────────────────────────────────────────────

@Composable
fun TasksSection(
    tasks: List<Task>,
    onShowTasks: () -> Unit,
    onAssetSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = "Tasks",
            badge = if (tasks.isNotEmpty()) tasks.size.toString() else null,
            actionLabel = "See all",
            onAction = onShowTasks,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
        )
        if (tasks.isEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = TapLogTeal400,
                    modifier = Modifier.size(18.dp)
                )
                Text("No open tasks", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            tasks.take(5).forEach { task ->
                TaskRow(
                    task = task,
                    onClick = { onAssetSelected(task.id) },
                    modifier = Modifier.padding(vertical = 3.dp)
                )
            }
        }
    }
}

// ── ContactsSection ───────────────────────────────────────────────────────

@Composable
fun ContactsSection(
    contacts: List<SiteContact>,
    onShowContacts: () -> Unit,
    onSiteSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = if (searchQuery.isBlank()) contacts
    else contacts.filter { it.name.contains(searchQuery, ignoreCase = true) }

    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = "Contacts",
            actionLabel = "See all",
            onAction = onShowContacts,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
        )
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search contacts") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
        )
        if (contacts.isEmpty()) {
            Text(
                text = "Add contact info when registering sites",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        } else {
            filtered.take(4).forEach { contact ->
                ContactRow(
                    contact = contact,
                    onClick = { onSiteSelected(contact.siteId) },
                    modifier = Modifier.padding(vertical = 3.dp)
                )
            }
        }
    }
}
