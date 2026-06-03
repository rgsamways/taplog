## ADDED Requirements

### Requirement: Role selection shown on first launch
The system SHALL display `RoleSelectionScreen` after splash when no `userRole` is stored in DataStore. Once a role is selected, it SHALL be persisted and `RoleSelectionScreen` SHALL NOT be shown again on subsequent launches.

#### Scenario: New install — no role stored
- **WHEN** the app launches and no `userRole` value exists in DataStore
- **THEN** the splash screen navigates to `RoleSelectionScreen` after `registryReady` and auth are satisfied

#### Scenario: Inspector role selected
- **WHEN** the user taps "Inspect assets" on `RoleSelectionScreen`
- **THEN** `UserRole.INSPECTOR` is saved to DataStore and the app navigates to `DashboardScreen`

#### Scenario: Field Analyst role selected
- **WHEN** the user taps "Scout a building" on `RoleSelectionScreen`
- **THEN** `UserRole.FIELD_ANALYST` is saved to DataStore and the app navigates to `FieldAnalystDashboardScreen`

#### Scenario: Existing user with stored role
- **WHEN** the app launches and `userRole` is already stored in DataStore
- **THEN** `RoleSelectionScreen` is NOT shown; the app navigates directly to the appropriate home screen for that role

### Requirement: Field Analyst home screen shown for FIELD_ANALYST role
The system SHALL navigate to `FieldAnalystDashboardScreen` instead of `DashboardScreen` when the stored `userRole` is `FIELD_ANALYST`.

#### Scenario: Splash completes with FIELD_ANALYST role
- **WHEN** the app launches and DataStore contains `userRole = FIELD_ANALYST`
- **THEN** navigation goes to `FieldAnalystDashboardScreen`, not `DashboardScreen`

#### Scenario: Splash completes with INSPECTOR role
- **WHEN** the app launches and DataStore contains `userRole = INSPECTOR`
- **THEN** navigation goes to `DashboardScreen` (existing behaviour unchanged)

### Requirement: Field Analyst Dashboard shows active visit and recent history
`FieldAnalystDashboardScreen` SHALL display: a header badge showing "Field Analyst", an active visit card if a visit is in progress (building name, address, asset count), a "Start New Visit" button when no visit is active, and a list of the 5 most recently visited sites with asset counts.

#### Scenario: No active visit
- **WHEN** `FieldAnalystDashboardScreen` is shown and no active visit site ID is stored in ViewModel
- **THEN** the "Start New Visit" button is prominently displayed and no active visit card is shown

#### Scenario: Visit in progress
- **WHEN** a visit has been started and assets have been registered to the active site
- **THEN** an active visit card shows the building name, address, and number of assets registered in this session; tapping the card navigates to `FieldAnalystScanScreen`

### Requirement: Visit setup captures building information
`VisitSetupScreen` SHALL require building name, address, and city. Building type, owner name, owner phone, and notes SHALL be optional. Tapping "Begin Visit" SHALL save the site to Room and set it as the active visit.

#### Scenario: Minimum required fields completed
- **WHEN** the user fills in building name, address, and city, then taps "Begin Visit"
- **THEN** a new `Site` record is saved to Room with the provided data and the app navigates to `FieldAnalystScanScreen`

#### Scenario: Required field missing
- **WHEN** the user taps "Begin Visit" without filling in building name, address, or city
- **THEN** the missing required fields are highlighted and navigation does not proceed

### Requirement: Field Analyst scan loop handles NFC taps
`FieldAnalystScanScreen` SHALL continuously listen for NFC taps. An unregistered tag tap SHALL open `QuickRegisterSheet`. A registered tag tap SHALL show an inline asset detail card without full-screen navigation.

#### Scenario: Unregistered tag tapped in scan loop
- **WHEN** the user taps an NFC tag not in Room while `FieldAnalystScanScreen` is active
- **THEN** `QuickRegisterSheet` opens with the scanned tag ID pre-populated

#### Scenario: Registered tag tapped in scan loop
- **WHEN** the user taps an NFC tag that matches an existing asset in Room
- **THEN** an inline card shows the asset name, location, and last inspection date without navigating away from the scan screen

#### Scenario: Asset count updates after registration
- **WHEN** the user completes registration via `QuickRegisterSheet`
- **THEN** the asset count in the `FieldAnalystScanScreen` header increments immediately

### Requirement: Quick registration creates Asset and TagEvent in under 30 seconds
`QuickRegisterSheet` SHALL allow registering an asset with type, name, location, and condition. Photo SHALL be optional. Tapping "Register Asset" SHALL create an `Asset` and a `TagEvent` with `registeredByRole = FIELD_ANALYST`.

#### Scenario: Successful quick registration
- **WHEN** the user fills in asset type, name, location, and condition, then taps "Register Asset"
- **THEN** a new `Asset` record is inserted with `vertical = EMBER`, `siteId` from the active visit, and `registeredByRole = FIELD_ANALYST`; a `TagEvent` is inserted with `registeredByRole = FIELD_ANALYST` and `registeredByUserId` from `InspectorPreferences.inspectorId`; the sheet dismisses

#### Scenario: Asset type pre-fills name
- **WHEN** the user selects an asset type from the dropdown
- **THEN** the asset name field is pre-populated with the selected type's label (editable)

#### Scenario: Manual add (no physical tag)
- **WHEN** the user opens `QuickRegisterSheet` via the "Add Manually" button (no NFC scan)
- **THEN** a UUID is generated as the tag ID and the asset is registered normally; the tag ID chip shows "Manual entry"

### Requirement: Unregistered tag screen is role-aware
`UnregisteredTagScreen` SHALL be shown whenever any user taps an NFC tag not in Room. The primary action SHALL navigate to registration appropriate for the user's role.

#### Scenario: FIELD_ANALYST taps unregistered tag
- **WHEN** a user with role `FIELD_ANALYST` taps an unregistered tag from any screen
- **THEN** `UnregisteredTagScreen` is shown and "Register This Asset" opens `QuickRegisterSheet` with the tag ID

#### Scenario: INSPECTOR taps unregistered tag
- **WHEN** a user with role `INSPECTOR` taps an unregistered tag
- **THEN** `UnregisteredTagScreen` is shown and "Register This Asset" navigates to `AssetRegistrationScreen` (existing flow)

#### Scenario: User dismisses unregistered tag screen
- **WHEN** the user taps "Not Now" on `UnregisteredTagScreen`
- **THEN** the screen is dismissed and the app returns to the previous screen
