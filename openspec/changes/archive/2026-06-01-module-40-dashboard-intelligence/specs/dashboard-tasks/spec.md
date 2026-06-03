## ADDED Requirements

### Requirement: TasksSection displays unified list of open deficiencies and overdue assets
The system SHALL display a `TasksSection` composable in `DashboardScreen` showing the top 5 items from a combined, sorted list of open deficiencies (`resolvedAt == null`) and overdue assets (`nextInspectionDue < now`). Items SHALL be sorted: CRITICAL deficiencies first, HIGH deficiencies second, OVERDUE_ASSET items (sorted by most overdue days descending), MEDIUM deficiencies, LOW deficiencies. Each row SHALL show a type icon, title, site name, and a severity chip (for deficiencies) or days-overdue chip (for overdue assets).

#### Scenario: CRITICAL deficiency appears before overdue asset
- **WHEN** there is one CRITICAL deficiency and one overdue asset in the task list
- **THEN** the CRITICAL deficiency row appears first

#### Scenario: Preview shows at most 5 items
- **WHEN** there are 10 tasks in total
- **THEN** `TasksSection` displays exactly 5 rows

#### Scenario: Empty state shows positive message
- **WHEN** there are no open deficiencies and no overdue assets
- **THEN** `TasksSection` displays "No open tasks" with a checkmark icon

### Requirement: Tapping a task row navigates to the relevant detail screen
The system SHALL navigate to `AssetDetailScreen` when the user taps any task row, passing the asset associated with the deficiency or overdue asset. The mark-resolved action SHALL remain on `AssetDetailScreen` and `OpenDeficienciesScreen` — the tasks section does not duplicate it.

#### Scenario: Tap deficiency task navigates to asset
- **WHEN** the user taps an OPEN_DEFICIENCY task row
- **THEN** `ScanState.AssetFound` is emitted for the asset associated with that deficiency

#### Scenario: Tap overdue asset task navigates to asset
- **WHEN** the user taps an OVERDUE_ASSET task row
- **THEN** `ScanState.AssetFound` is emitted for that asset

### Requirement: TasksScreen provides full-screen task list with filter chips
The system SHALL provide a `TasksScreen` reachable via `ScanState.Tasks`. It SHALL display all tasks (no 5-item cap) with filter chips: All · Deficiencies · Overdue. The active filter chip SHALL be visually highlighted. Back navigation SHALL return to `ScanState.Dashboard`.

#### Scenario: "See all" from dashboard opens TasksScreen
- **WHEN** the user taps the "See all" action in the TasksSection header
- **THEN** `ScanState.Tasks` is emitted and `TasksScreen` is displayed

#### Scenario: Deficiencies filter shows only deficiency tasks
- **WHEN** the user selects the "Deficiencies" filter chip in `TasksScreen`
- **THEN** only OPEN_DEFICIENCY tasks are shown; OVERDUE_ASSET tasks are hidden

#### Scenario: Overdue filter shows only overdue asset tasks
- **WHEN** the user selects the "Overdue" filter chip in `TasksScreen`
- **THEN** only OVERDUE_ASSET tasks are shown; OPEN_DEFICIENCY tasks are hidden

#### Scenario: Back from TasksScreen returns to dashboard
- **WHEN** the user taps the back arrow in `TasksScreen`
- **THEN** `ScanState.Dashboard` is emitted and `DashboardScreen` is displayed

### Requirement: tasks StateFlow provides sorted unified task list
`EmberViewModel` SHALL expose `tasks: StateFlow<List<Task>>` derived by combining `openDeficienciesWithAsset` and `activeAssets` flows. `Task` SHALL carry `id`, `type: TaskType`, `title`, `siteName`, `severity: DeficiencySeverity?`, `daysOverdue: Int?`, and `createdAt`. The list SHALL be sorted per the priority order defined above and SHALL update reactively.

#### Scenario: tasks StateFlow initialises to empty list
- **WHEN** `EmberViewModel` is created before any data is loaded
- **THEN** `tasks` emits an empty list without throwing

#### Scenario: Resolving a deficiency removes it from tasks
- **WHEN** a deficiency is marked resolved via `resolveDeficiency()`
- **THEN** the corresponding OPEN_DEFICIENCY task disappears from `tasks` flow
