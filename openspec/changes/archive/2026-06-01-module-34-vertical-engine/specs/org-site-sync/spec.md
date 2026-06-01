## ADDED Requirements

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

## MODIFIED Requirements

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
