## Why

The Module 39 dashboard shell is a navigation portal — it shows summary numbers and routes to other screens. Module 40 makes it a professional briefing tool: inspectors can review what's scheduled, what's outstanding, and who to call before they set foot in a building. All three intelligence sections are derived from data already in Room; no migrations or backend changes are required.

## What Changes

- Add `CalendarSection` to `DashboardScreen` — pure-Compose month grid showing inspection due dates as colour-coded dots (green/amber/red by urgency); tap a day to see assets due; tap an asset → `AssetDetailScreen`
- Add `TasksSection` to `DashboardScreen` — unified list of open deficiencies and overdue assets, sorted by urgency; top 5 in preview
- Add `ContactsSection` to `DashboardScreen` — searchable list of client/contact names and phones aggregated from all `Site` records; tap phone → dialer; tap row → `SiteDetailScreen`
- Add three full-screen screens: `CalendarScreen`, `TasksScreen`, `ContactsScreen` — reached via "See all" from dashboard
- Add `CalendarEvent`, `Task`, `SiteContact` data classes and `CalendarUrgency`, `TaskType` enums to `EmberViewModel`
- Add `calendarEvents: StateFlow<Map<LocalDate, List<CalendarEvent>>>`, `tasks: StateFlow<List<Task>>`, `allContacts: StateFlow<List<SiteContact>>` to `EmberViewModel`
- Add three DAO read queries: assets with due dates (JOIN with site), open deficiencies with asset+site context, all sites for contact extraction
- Add three `ScanState` entries: `ScanState.Calendar`, `ScanState.Tasks`, `ScanState.Contacts`
- Wire new states in `MainActivity` `EmberScanScreen` when block

## Capabilities

### New Capabilities
- `dashboard-calendar`: Month grid calendar showing inspection due dates per asset; colour-coded urgency dots; day-tap expansion; full-screen `CalendarScreen` with month swipe
- `dashboard-tasks`: Unified task list combining open deficiencies and overdue assets; priority sort; filter chips (All / Deficiencies / Overdue); full-screen `TasksScreen`
- `dashboard-contacts`: Aggregated contact list from all site client/contact fields; client-side search; tap-to-dial; full-screen `ContactsScreen`

### Modified Capabilities
- `dashboard-screen`: Three new sections appended to existing `LazyColumn` (calendar, tasks, contacts); three new "See all" navigation targets; `DashboardScreen` composable signature extended with new callbacks

## Impact

- `EmberViewModel.kt` — new data classes, enums, StateFlows
- `EmberDAO.kt` — three new read-only queries (no schema changes)
- `DashboardScreen.kt` — three new section composables added to `LazyColumn`
- `MainActivity.kt` — three new `ScanState` branches in `EmberScanScreen`
- New files: `CalendarScreen.kt`, `TasksScreen.kt`, `ContactsScreen.kt`
- No Room migrations, no backend changes, no existing screen modifications
