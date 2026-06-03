# Tag Lifecycle Spec

### Requirement: TagEvent recorded on asset registration
The system SHALL insert a `TagEvent` record when a new NFC tag is attached to an asset during registration. The `TagEvent` SHALL include `registeredByRole` (the role of the actor who performed registration) and `registeredByUserId` (the authenticated user's ID, if available).

#### Scenario: New asset registered by Inspector
- **WHEN** an inspector completes asset registration via `AssetRegistrationScreen` and the asset is saved
- **THEN** a `TagEvent` with `assetId`, `tagId` (= `Asset.nfcTagId`), `attachedAt = now`, `registeredByRole = INSPECTOR`, and `registeredByUserId` from `InspectorPreferences.inspectorId` is inserted into `tag_events`

#### Scenario: New asset registered by Field Analyst via QuickRegisterSheet
- **WHEN** a Field Analyst completes registration via `QuickRegisterSheet` and the asset is saved
- **THEN** a `TagEvent` with `registeredByRole = FIELD_ANALYST` and `registeredByUserId` from `InspectorPreferences.inspectorId` is inserted into `tag_events`

#### Scenario: Existing records after migration
- **WHEN** Room migration 9→10 runs
- **THEN** all existing `tag_events` rows have `registeredByRole = 'OWNER'` (default); no existing rows are modified beyond this default

### Requirement: TagEvent is insert-only
The system SHALL provide no mechanism to update or delete a `TagEvent` record.

#### Scenario: Attempt to modify tag history
- **WHEN** any code path attempts to update or delete a `TagEvent`
- **THEN** no such method exists on `TagEventDao` and the operation cannot be performed

### Requirement: Tag replacement workflow available on AssetDetailScreen
The system SHALL provide a "Replace Tag" action on `AssetDetailScreen` that retires the current tag and attaches a new one as an atomic operation.

#### Scenario: Inspector initiates tag replacement
- **WHEN** the inspector taps "Replace Tag" on the asset detail screen
- **THEN** a dialog appears requesting a retire reason (DAMAGED / LOST / REPLACED / REMOVED) and prompts the inspector to scan the new NFC tag

#### Scenario: New tag scanned during replacement flow
- **WHEN** the inspector selects a retire reason and scans a new NFC tag during the replacement dialog
- **THEN** the system atomically: (1) inserts a `TagEvent` retiring the old tag with the selected reason, current timestamp, and `retiredByInspectorId` from `InspectorPreferences.inspectorId`; (2) updates `Asset.nfcTagId` to the new tag ID; and (3) inserts a new `TagEvent` attaching the new tag — all within a single Room transaction

#### Scenario: Tag replacement cancelled
- **WHEN** the inspector dismisses the replacement dialog without scanning a new tag
- **THEN** no `TagEvent` is inserted and `Asset.nfcTagId` is unchanged

#### Scenario: New tag already registered to another asset
- **WHEN** the scanned new tag ID matches an existing asset's `nfcTagId`
- **THEN** the replacement is rejected with an error message ("This tag is already registered to another asset") and no writes are made

### Requirement: TagEvent synced to backend
The system SHALL sync unsynced `TagEvent` records to the backend via `POST /api/v1/tag_events` after assets have been synced and before inspections.

#### Scenario: Successful sync
- **WHEN** the device has connectivity and `syncAll()` is called
- **THEN** all `TagEvent` records with `isSynced = false` are POSTed to the backend and marked `isSynced = true` on 200 response

#### Scenario: Backend returns 409 conflict
- **WHEN** a `TagEvent` POST returns HTTP 409
- **THEN** the record is marked `isSynced = true`

### Requirement: TagEvent birthing event provenance visible on AssetDetailScreen
The system SHALL display a provenance card on `AssetDetailScreen` showing who first registered the asset: "Registered by [name] as [role] on [date]", sourced from the first `TagEvent` for that asset.

#### Scenario: Asset registered by Field Analyst
- **WHEN** an asset's detail screen is viewed and the first `TagEvent` has `registeredByRole = FIELD_ANALYST`
- **THEN** the provenance card shows "Registered by [name] as Field Analyst on [date]"

#### Scenario: Asset with pre-migration record (OWNER default)
- **WHEN** an asset's detail screen is viewed and the first `TagEvent` has `registeredByRole = OWNER`
- **THEN** the provenance card shows "Registered by owner on [date]" (registeredByName is null for pre-migration records)
