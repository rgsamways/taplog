## MODIFIED Requirements

### Requirement: DashboardScreen is the app home for authenticated users
The system SHALL replace `ScanState.SiteList` with `ScanState.Dashboard` as the home state after login. `DashboardScreen` SHALL be the composable rendered for `ScanState.Dashboard`. `SiteListScreen` SHALL remain accessible as a full-screen destination via "See all" from the dashboard sites section. `DashboardScreen` SHALL additionally contain `CalendarSection`, `TasksSection`, and `ContactsSection` appended to the existing `LazyColumn` below the sites preview section.

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
