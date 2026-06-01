# Scan Log Spec

### Requirement: ScanEvent recorded on every NFC tap
The system SHALL insert a `ScanEvent` record into the local Room database every time an NFC tag is read, regardless of whether the inspector proceeds to submit an inspection. After Module 28, the `inspectorId` field SHALL be populated from `InspectorPreferences.inspectorId` (set from JWT) for all new ScanEvent records.

#### Scenario: NFC tap opens asset detail (BROWSE)
- **WHEN** the inspector taps an NFC tag and the asset is found in the local database
- **THEN** a `ScanEvent` with `eventType = BROWSE`, the tag ID, `inspectorName` (from JWT claims), `inspectorId` (from DataStore), and current timestamp is inserted into the `scan_events` table before the `AssetDetailScreen` is shown

#### Scenario: Inspector submits inspection (INSPECTION)
- **WHEN** the inspector completes and submits an inspection form
- **THEN** a second `ScanEvent` with `eventType = INSPECTION`, the tag ID, `inspectorId` (from DataStore), and `inspectorName` (from JWT) is inserted at submission time

#### Scenario: NFC tap on unknown tag
- **WHEN** the inspector taps an NFC tag that does not match any asset in the local database
- **THEN** no `ScanEvent` is inserted (the asset is unknown; there is nothing to log against)

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
