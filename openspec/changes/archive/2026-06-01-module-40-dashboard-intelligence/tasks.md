## 1. ScanState — add Calendar, Tasks, Contacts

- [x] 1.1 In `EmberViewModel.kt`, add `object Calendar : ScanState()`, `object Tasks : ScanState()`, `object Contacts : ScanState()` to the sealed class
- [x] 1.2 Add `showCalendar()`, `showTasks()`, `showContacts()` functions to `EmberViewModel` — each emits the corresponding `ScanState`

## 2. Data classes and enums

- [x] 2.1 Add `enum class CalendarUrgency { UPCOMING, DUE_SOON, OVERDUE }` to `EmberViewModel.kt`
- [x] 2.2 Add `data class CalendarEvent(assetId, assetName, siteName, assetTypeCode, dueDate: LocalDate, urgency: CalendarUrgency)` to `EmberViewModel.kt`
- [x] 2.3 Add `enum class TaskType { OPEN_DEFICIENCY, OVERDUE_ASSET }` to `EmberViewModel.kt`
- [x] 2.4 Add `data class Task(id, type: TaskType, title, siteName, severity: DeficiencySeverity?, daysOverdue: Int?, createdAt)` to `EmberViewModel.kt`
- [x] 2.5 Add `data class SiteContact(name, phone: String?, role: String, siteName, siteId)` to `EmberViewModel.kt`

## 3. DAO — AssetWithSite JOIN query

- [x] 3.1 Check `EmberDAO.kt` for an existing asset+site JOIN result — if one exists, reuse it; if not, add `data class AssetWithSite(val asset: Asset, val siteName: String)` (non-entity) and `fun getAssetsWithDueDates(): Flow<List<AssetWithSite>>` to `AssetDao` with a JOIN on `sites.id = assets.siteId` filtered to `assets.nextInspectionDue IS NOT NULL`
- [x] 3.2 Confirm `DeficiencyWithAsset.buildingName` (the existing JOIN field) is the site name — it is: the DAO query already does `INNER JOIN sites s ON a.siteId = s.id` and maps `s.name` to `buildingName`. No new DAO query needed. Use `buildingName` as the site name in the `Task` data class mapping (do not add a `siteName` field to `DeficiencyWithAsset`)

## 4. ViewModel — StateFlows

- [x] 4.1 Add `calendarEvents: StateFlow<Map<LocalDate, List<CalendarEvent>>>` — derived from `getAssetsWithDueDates()` flow; classify each asset's urgency by comparing `nextInspectionDue` to today (OVERDUE: past, DUE_SOON: within 7 days, UPCOMING: beyond); group by `LocalDate`; `stateIn(WhileSubscribed(5000), emptyMap())`
- [x] 4.2 Add `tasks: StateFlow<List<Task>>` — combine `openDeficienciesWithAsset` and `activeAssets` flows; filter `activeAssets` to only those where `(it.nextInspectionDue ?: Long.MAX_VALUE) < System.currentTimeMillis()` before mapping to tasks (do NOT emit all active assets); map filtered overdue assets to `Task(type=OVERDUE_ASSET, daysOverdue=...)` and deficiencies to `Task(type=OPEN_DEFICIENCY, severity=...)`; apply sort order (CRITICAL → HIGH → OVERDUE_ASSET by days desc → MEDIUM → LOW); `stateIn(WhileSubscribed(5000), emptyList())`
- [x] 4.3 Add `allContacts: StateFlow<List<SiteContact>>` — derived from `sitesForOrg` flow; for each site emit up to 2 `SiteContact` entries from `clientName/clientPhone` and `contactName/contactPhone` (skip nulls, skip duplicate if contactName == clientName); deduplicate by name+phone; sort alphabetically; `stateIn(WhileSubscribed(5000), emptyList())`

## 5. CalendarSection and CalendarScreen

- [x] 5.1 Create `CalendarSection` composable in `DashboardScreen.kt` — `SectionHeader` with "See all" → `onShowCalendar`; current-month grid (7 columns, standard calendar layout); day cells with urgency dots (up to 3 + overflow count); tap day → inline expansion showing `CalendarEvent` list for that day
- [x] 5.2 Add `AssetDetailScreen` navigation from the day-expansion asset row — calls `onAssetSelected(asset)` which routes via `viewModel.selectAsset(asset)` (sets `AssetDetailSource.FROM_LIST` then emits `ScanState.AssetFound`); do NOT emit `ScanState.AssetFound` directly or set `assetDetailSource` manually
- [x] 5.3 Create `CalendarScreen.kt` — full-screen with `TopAppBar` (back → dashboard); same month grid as section; month navigation via left/right arrow `IconButton`s; `var displayedMonth by remember { mutableStateOf(YearMonth.now()) }`

## 6. TasksSection and TasksScreen

- [x] 6.1 Create `TasksSection` composable in `DashboardScreen.kt` — `SectionHeader` with count chip and "See all" → `onShowTasks`; `LazyColumn` of top 5 tasks; `TaskRow` composable (type icon, title, site name, severity/days chip); empty state "No open tasks ✓"; tap → `onAssetSelected`
- [x] 6.2 Create `TasksScreen.kt` — full-screen with `TopAppBar` (back → dashboard); `FilterChip` row (All · Deficiencies · Overdue); full sorted task list; same `TaskRow` composable; tap → `onAssetSelected`

## 7. ContactsSection and ContactsScreen

- [x] 7.1 Create `ContactsSection` composable in `DashboardScreen.kt` — `SectionHeader` with "See all" → `onShowContacts`; search `OutlinedTextField`; up to 4 `ContactRow` composables filtered by search query; empty state "Add contact info when registering sites"; `ContactRow`: name, role chip, site name, tappable phone number (`ACTION_DIAL` intent)
- [x] 7.2 Create `ContactsScreen.kt` — full-screen with `TopAppBar` (back → dashboard); full contact list with search bar; same `ContactRow` composable; tap row → `onSiteSelected(siteId)`

## 8. DashboardScreen — wire new sections

- [x] 8.1 Add `onShowCalendar`, `onShowTasks`, `onShowContacts`, `onAssetSelected` parameters to `DashboardScreen` composable signature
- [x] 8.2 Append `CalendarSection`, `TasksSection`, `ContactsSection` items to the existing `LazyColumn` in `DashboardScreen`, below the sites preview section
- [x] 8.3 Pass `calendarEvents`, `tasks`, `allContacts` StateFlows (collected in `EmberScanScreen`) as parameters to `DashboardScreen`

## 9. MainActivity routing

- [x] 9.1 Add `ScanState.Calendar` branch in `EmberScanScreen` → renders `CalendarScreen(calendarEvents, onAssetSelected, onBack = showDashboard)`
- [x] 9.2 Add `ScanState.Tasks` branch → renders `TasksScreen(tasks, onAssetSelected, onBack = showDashboard)`
- [x] 9.3 Add `ScanState.Contacts` branch → renders `ContactsScreen(allContacts, onSiteSelected, onBack = showDashboard)`
- [x] 9.4 Update `ScanState.Dashboard` branch to pass new callbacks: `onShowCalendar`, `onShowTasks`, `onShowContacts`, `onAssetSelected`

## 10. Validation

- [x] 10.1 Build project — no compilation errors
- [x] 10.2 Launch app — confirm all three new sections appear on dashboard below site preview
- [x] 10.3 Confirm calendar month grid renders; dots appear on days with assets due; tap a day with a dot shows asset list
- [x] 10.4 Confirm "See all" from CalendarSection opens CalendarScreen; month navigation works; back returns to dashboard
- [x] 10.5 Confirm tasks section shows correct items sorted correctly; empty state shows when no tasks
- [x] 10.6 Confirm "See all" from TasksSection opens TasksScreen; filter chips work; tap navigates to asset
- [x] 10.7 Confirm contacts section shows names from site records; search filters correctly; tap phone opens dialer; tap row → SiteDetailScreen
- [x] 10.8 Confirm "See all" from ContactsSection opens ContactsScreen; back returns to dashboard
