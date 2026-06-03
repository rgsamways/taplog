# Scan Log Spec

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

### Requirement: ScanEvent is insert-only
The system SHALL provide no mechanism to update or delete a `ScanEvent` record. The `ScanEventDao` SHALL expose only `insert` and query methods.

#### Scenario: Attempt to modify scan log
- **WHEN** any code path attempts to update or delete a `ScanEvent`
- **THEN** no such method exists on `ScanEventDao` and the operation cannot be performed

### Requirement: Scan log displayed on AssetDetailScreen
The system SHALL display recent scan events for an asset in a collapsible section on the `AssetDetailScreen`.

#### Scenario: Asset has scan history
- **WHEN** the inspector views an asset's detail screen
- **THEN** a collapsible "Scan History" section shows the most recent scan events with timestamp, inspector name, and event type (INSPECTION or BROWSE)

#### Scenario: Asset has no scan history
- **WHEN** the inspector views an asset that has never been scanned
- **THEN** the "Scan History" section shows an empty state message ("No scans recorded yet")

### Requirement: ScanEvent synced to backend
The system SHALL sync unsynced `ScanEvent` records to the backend via `POST /api/v1/scan_events` after all other entity types have been synced.

#### Scenario: Successful sync
- **WHEN** the device has connectivity and `syncAll()` is called
- **THEN** all `ScanEvent` records with `isSynced = false` are POSTed to the backend and marked `isSynced = true` on 200 response

#### Scenario: Backend returns 409 conflict
- **WHEN** a `ScanEvent` POST returns HTTP 409
- **THEN** the record is marked `isSynced = true` (already exists on server — treat as success)
