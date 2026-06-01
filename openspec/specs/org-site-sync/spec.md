# Org Site Sync Spec

## Purpose

Defines how Organisation, Site, and related entity records are synced from the device to the backend in the correct order to maintain referential integrity.
## Requirements
### Requirement: Organisation record synced to backend after authentication
The system SHALL sync the local Organisation record to the backend via `POST /api/v1/organisations` as the first step in every `syncAll()` call, after authentication is established. The sync payload SHALL include the `licensedVerticals` field.

#### Scenario: Organisation synced successfully
- **WHEN** device has connectivity, a valid Bearer token, and a local Organisation record with `isSynced = false`
- **THEN** the Organisation (including `licensedVerticals`) is POSTed to `/api/v1/organisations`, and `isSynced` is set to `true` on HTTP 200

#### Scenario: Organisation already synced
- **WHEN** backend returns HTTP 409 for an Organisation POST
- **THEN** the Organisation is marked `isSynced = true` (already exists on server — idempotent)

#### Scenario: Sync attempted without authentication
- **WHEN** `syncAll()` is called and no valid Bearer token exists
- **THEN** sync is aborted and no network requests are made

### Requirement: Site records synced to backend after organisation
The system SHALL sync all local Site records (where `isSynced = false`) to the backend via `POST /api/v1/sites` in `syncAll()`, after organisations have been synced and before assets.

#### Scenario: Sites synced successfully
- **WHEN** device has connectivity, a valid Bearer token, and one or more Site records with `isSynced = false`
- **THEN** each Site is POSTed to `/api/v1/sites` and marked `isSynced = true` on HTTP 200

#### Scenario: Site sync blocked by missing organisation on backend
- **WHEN** site sync is attempted but the organisation has not yet synced (e.g. org sync failed)
- **THEN** site sync returns 409 or 422 from backend; site remains `isSynced = false` for retry

### Requirement: Sync order enforces referential integrity
The system SHALL execute `syncAll()` in the order: organisations → sites → assets → tag_events → inspections → deficiencies → scan_events. No entity SHALL be synced before its parent entity.

#### Scenario: Full sync runs in correct order
- **WHEN** `syncAll()` is called with pending records across all entity types
- **THEN** records are sent to the backend in the order: organisations first, sites second, assets third, followed by tag_events, inspections, deficiencies, scan_events last

### Requirement: Site entity isSynced field persisted
The system SHALL track the `isSynced` state of each Site record to support offline-first sync. A Site created while offline SHALL be synced on next connectivity restore via WorkManager.

#### Scenario: New site created offline
- **WHEN** inspector creates a site while the device has no connectivity
- **THEN** the Site is saved locally with `isSynced = false` and synced on next successful `syncAll()`

### Requirement: Organisation model carries licensedVerticals
The `Organisation` data class SHALL include a `licensedVerticals: List<String>` field defaulting to `listOf("EMBER")`. This field SHALL be included in the Organisation POST payload sent to the backend during sync, and SHALL be populated from the backend response on receipt.

#### Scenario: New organisation defaults to EMBER
- **WHEN** a new Organisation is created locally
- **THEN** `licensedVerticals` is `["EMBER"]`

#### Scenario: licensedVerticals is included in sync payload
- **WHEN** an Organisation with `isSynced = false` is POSTed to `/api/v1/organisations`
- **THEN** the request body includes a `licensedVerticals` array

#### Scenario: Backend-provided licensedVerticals is persisted locally
- **WHEN** the backend returns an Organisation record with a `licensedVerticals` value in a sync response
- **THEN** the local `Organisation` entity is updated with the backend-provided value

