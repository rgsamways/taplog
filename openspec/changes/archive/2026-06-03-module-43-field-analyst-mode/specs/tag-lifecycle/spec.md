## MODIFIED Requirements

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

## ADDED Requirements

### Requirement: TagEvent birthing event provenance visible on AssetDetailScreen
The system SHALL display a provenance card on `AssetDetailScreen` showing who first registered the asset: "Registered by [name] as [role] on [date]", sourced from the first `TagEvent` for that asset.

#### Scenario: Asset registered by Field Analyst
- **WHEN** an asset's detail screen is viewed and the first `TagEvent` has `registeredByRole = FIELD_ANALYST`
- **THEN** the provenance card shows "Registered by [name] as Field Analyst on [date]"

#### Scenario: Asset with pre-migration record (OWNER default)
- **WHEN** an asset's detail screen is viewed and the first `TagEvent` has `registeredByRole = OWNER`
- **THEN** the provenance card shows "Registered by owner on [date]" (registeredByName is null for pre-migration records)
