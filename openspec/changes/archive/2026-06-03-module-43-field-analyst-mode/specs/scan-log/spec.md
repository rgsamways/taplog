## MODIFIED Requirements

### Requirement: ScanEvent recorded on every NFC tap
The system SHALL insert a `ScanEvent` record into the local Room database every time an NFC tag is read, regardless of whether the user proceeds to submit an inspection or register an asset. The `inspectorId` field SHALL be populated from `InspectorPreferences.inspectorId` for all new records.

#### Scenario: NFC tap opens asset detail — BROWSE (Inspector)
- **WHEN** an Inspector taps an NFC tag and the asset is found in Room
- **THEN** a `ScanEvent` with `eventType = BROWSE`, the tag ID, `inspectorName`, `inspectorId`, and current timestamp is inserted before `AssetDetailScreen` is shown

#### Scenario: Inspector submits inspection — INSPECTION
- **WHEN** an Inspector completes and submits an inspection form
- **THEN** a second `ScanEvent` with `eventType = INSPECTION`, the tag ID, `inspectorId`, and `inspectorName` is inserted at submission time

#### Scenario: NFC tap on unknown tag — routes to UnregisteredTagScreen
- **WHEN** any user taps an NFC tag that does not match any asset in Room
- **THEN** no `ScanEvent` is inserted (no asset to log against) and the app navigates to `UnregisteredTagScreen` with the scanned tag ID

#### Scenario: Field Analyst taps registered tag in scan loop
- **WHEN** a Field Analyst taps a registered tag in `FieldAnalystScanScreen`
- **THEN** a `ScanEvent` with `eventType = BROWSE` is inserted and an inline asset card is shown (no full-screen navigation)

## REMOVED Requirements

### Requirement: AssetNotFound state shown on unknown tag tap
**Reason**: Replaced by `UnregisteredTagScreen` which provides role-aware registration path instead of a dead-end state.
**Migration**: All `when (state) is ScanState.AssetNotFound` branches in composables and MainActivity MUST be updated to `is ScanState.UnregisteredTag`. The new state carries `tagId: String` for use in the registration flow.
