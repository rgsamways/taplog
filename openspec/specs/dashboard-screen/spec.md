## ADDED Requirements

### Requirement: DashboardScreen is the app home for authenticated users
The system SHALL replace `ScanState.SiteList` with `ScanState.Dashboard` as the home state after login. `DashboardScreen` SHALL be the composable rendered for `ScanState.Dashboard`. `SiteListScreen` SHALL remain accessible as a full-screen destination via "See all" from the dashboard. `DashboardScreen` SHALL additionally contain `CalendarSection`, `TasksSection`, and `ContactsSection` appended to the existing `LazyColumn` below the sites preview section.

#### Scenario: Authenticated user lands on dashboard
- **WHEN** an authenticated user completes login or the app starts with a stored auth token
- **THEN** `EmberViewModel.scanState` emits `ScanState.Dashboard` and `DashboardScreen` is displayed

#### Scenario: SiteListScreen still reachable
- **WHEN** the user taps "See all" in the dashboard sites section
- **THEN** `ScanState.SiteList` is emitted and `SiteListScreen` renders in full-screen mode

#### Scenario: CalendarScreen reachable from dashboard
- **WHEN** the user taps "See all" in the CalendarSection header
- **THEN** `ScanState.Calendar` is emitted and `CalendarScreen` is displayed

#### Scenario: TasksScreen reachable from dashboard
- **WHEN** the user taps "See all" in the TasksSection header
- **THEN** `ScanState.Tasks` is emitted and `TasksScreen` is displayed

#### Scenario: ContactsScreen reachable from dashboard
- **WHEN** the user taps "See all" in the ContactsSection header
- **THEN** `ScanState.Contacts` is emitted and `ContactsScreen` is displayed

### Requirement: Stats strip displays four live metrics
`DashboardScreen` SHALL display a `StatsStrip` composable showing four metrics derived from existing Room data: inspections completed this calendar month, count of open deficiencies (resolvedAt IS NULL), count of sites with at least one overdue asset, and total site count. All values SHALL update reactively as underlying data changes.

#### Scenario: Stats reflect current Room state
- **WHEN** `DashboardScreen` is displayed and the org has sites and assets
- **THEN** the stats strip shows non-null values for all four metrics matching the current Room data

#### Scenario: Zero state displays correctly
- **WHEN** the org has no sites or inspections yet
- **THEN** all four stats show "0" without crashing

### Requirement: Quick actions section provides one-tap access to primary flows
`DashboardScreen` SHALL display a quick actions section containing: a primary Scan button (transitions to `ScanState.Idle` or activates NFC scan readiness), an Add Site button (transitions to `ScanState.Idle` for site registration), and an All Deficiencies button (transitions to `ScanState.OpenDeficiencies`).

#### Scenario: Scan button is visually prominent
- **WHEN** `DashboardScreen` is displayed
- **THEN** the Scan button uses the primary brand color and is visually larger than the secondary actions

#### Scenario: Quick action navigation works
- **WHEN** user taps Add Site
- **THEN** `ScanState.Idle` is emitted and `SiteRegistrationScreen` is displayed

### Requirement: Overdue section lists sites with overdue assets sorted by urgency
`DashboardScreen` SHALL display an overdue section showing sites that have at least one asset where `nextInspectionDue < System.currentTimeMillis()` and `isActive == true`. Sites SHALL be sorted so the most-overdue site (largest `mostOverdueDays`) appears first. Each row SHALL show the site name, overdue asset count, most overdue asset name, and days overdue. Tapping a row SHALL navigate to `SiteDetailScreen` for that site.

#### Scenario: Overdue sites appear sorted
- **WHEN** the org has multiple sites with overdue assets
- **THEN** the site with the largest `mostOverdueDays` value appears first in the list

#### Scenario: Empty overdue state shows positive message
- **WHEN** no sites have overdue assets
- **THEN** the overdue section shows "All sites are current" with a visual indicator — no empty card

#### Scenario: Row tap navigates to site detail
- **WHEN** the user taps an overdue site row
- **THEN** `ScanState.SiteSelected` is emitted for that site and `SiteDetailScreen` is displayed
