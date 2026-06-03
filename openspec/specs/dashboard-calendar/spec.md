## ADDED Requirements

### Requirement: CalendarSection displays current month with inspection due date dots
The system SHALL display a `CalendarSection` composable in `DashboardScreen` showing the current calendar month as a 7-column grid. Each day cell SHALL display a coloured dot at the bottom of the cell for any asset with `nextInspectionDue` falling on that date. Dot colour SHALL reflect urgency: green (`TapLogTeal400`) for due more than 7 days away, amber (`TapLogWarning`) for due within 7 days, red (`TapLogDanger`) for overdue. If more than 3 assets are due on a single day, the cell SHALL show up to 3 dots and a count label for the remainder.

#### Scenario: Day with upcoming inspection shows green dot
- **WHEN** an asset has `nextInspectionDue` set to a date more than 7 days in the future
- **THEN** the corresponding day cell in the calendar shows a green dot

#### Scenario: Day with due-soon inspection shows amber dot
- **WHEN** an asset has `nextInspectionDue` set to a date within 7 days from today (but not past)
- **THEN** the corresponding day cell shows an amber dot

#### Scenario: Day with overdue inspection shows red dot
- **WHEN** an asset has `nextInspectionDue` set to a date in the past
- **THEN** the corresponding day cell shows a red dot

#### Scenario: Day with more than 3 assets due shows overflow count
- **WHEN** 5 assets are due on the same day
- **THEN** the day cell shows 3 dots and a "+2" label

#### Scenario: Empty month shows no dots
- **WHEN** no assets have `nextInspectionDue` in the displayed month
- **THEN** all day cells are blank with no dots

### Requirement: Tapping a calendar day shows assets due that day
The system SHALL respond to a day cell tap by displaying the list of assets due on that day. Each item in the list SHALL show the asset name, site name, and asset type chip. Tapping an asset item SHALL navigate to `AssetDetailScreen` for that asset.

#### Scenario: Tap on day with assets shows list
- **WHEN** the user taps a day cell that has at least one dot
- **THEN** a list appears (inline expansion or bottom sheet) showing all assets due that day with their site names

#### Scenario: Tap on empty day does nothing
- **WHEN** the user taps a day cell with no dots
- **THEN** no action occurs

#### Scenario: Tap asset in day list navigates to detail
- **WHEN** the user taps an asset row in the day expansion
- **THEN** `ScanState.AssetFound` is emitted for that asset and `AssetDetailScreen` is displayed

### Requirement: CalendarScreen provides full-screen calendar with month navigation
The system SHALL provide a `CalendarScreen` reachable via `ScanState.Calendar`. It SHALL display the same month grid as the dashboard section but SHALL support navigating between months via left/right arrow buttons or swipe gesture. Back navigation SHALL return to `ScanState.Dashboard`.

#### Scenario: "See all" from dashboard opens CalendarScreen
- **WHEN** the user taps the "See all" action in the CalendarSection header
- **THEN** `ScanState.Calendar` is emitted and `CalendarScreen` is displayed

#### Scenario: Month navigation advances to next month
- **WHEN** the user taps the right arrow in `CalendarScreen`
- **THEN** the calendar advances to the next month and the month label updates

#### Scenario: Back from CalendarScreen returns to dashboard
- **WHEN** the user taps the back arrow in `CalendarScreen`
- **THEN** `ScanState.Dashboard` is emitted and `DashboardScreen` is displayed

### Requirement: calendarEvents StateFlow provides map of LocalDate to asset events
`EmberViewModel` SHALL expose `calendarEvents: StateFlow<Map<LocalDate, List<CalendarEvent>>>` derived from active assets joined with their site names. `CalendarEvent` SHALL carry `assetId`, `assetName`, `siteName`, `assetTypeCode`, `dueDate: LocalDate`, and `urgency: CalendarUrgency` (UPCOMING / DUE_SOON / OVERDUE). The map SHALL update reactively when asset data changes.

#### Scenario: StateFlow emits correct urgency classification
- **WHEN** `calendarEvents` is collected and an asset is due in 3 days
- **THEN** its `CalendarEvent.urgency` is `DUE_SOON`

#### Scenario: StateFlow initialises to empty map
- **WHEN** `EmberViewModel` is created before any org data is loaded
- **THEN** `calendarEvents` emits an empty map without throwing
