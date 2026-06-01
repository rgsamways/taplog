## ADDED Requirements

### Requirement: VerticalConfig data model is defined
The system SHALL define `VerticalConfig`, `VerticalAssetType`, `InspectionFormProfile`, `FormField`, `TriggerModel`, `RoleModel`, and `FieldType` as Kotlin data classes and enums representing the complete configuration for a single TapLog vertical.

#### Scenario: VerticalConfig fully describes a vertical
- **WHEN** a `VerticalConfig` is constructed for the Ember vertical
- **THEN** it carries `vertical = TapLogVertical.EMBER`, a non-empty `assetTypeRegistry`, and a `formProfile` with a non-empty `fields` list

#### Scenario: VerticalAssetType carries checklist items
- **WHEN** any `VerticalAssetType` in a `VerticalConfig.assetTypeRegistry` is read
- **THEN** it has a non-empty `checklistItems` list and a non-null `intervalMonths` (for calendar-triggered verticals)

### Requirement: VerticalRegistry registers and retrieves configs
The `VerticalRegistry` singleton SHALL accept `register(config: VerticalConfig)` calls and return a config via `get(vertical: TapLogVertical)`. Calling `get()` for an unregistered vertical SHALL throw an `IllegalStateException`.

#### Scenario: Registered vertical is retrievable
- **WHEN** `VerticalRegistry.register(config)` is called followed by `VerticalRegistry.get(config.vertical)`
- **THEN** the returned config is identical to the one registered

#### Scenario: Unregistered vertical throws
- **WHEN** `VerticalRegistry.get(TapLogVertical.ANCHOR)` is called before any ANCHOR config is registered
- **THEN** an `IllegalStateException` is thrown with a message identifying the unregistered vertical

### Requirement: VerticalConfig is fetched from backend at startup and cached in Room
The system SHALL fetch `VerticalConfig` for all of the org's licensed verticals from `GET /api/v1/verticals` on every app startup when network is available, store the result in the Room `vertical_configs` table (Room version 7), and populate `VerticalRegistry` from the cached data.

#### Scenario: Successful fetch populates registry
- **WHEN** the app starts with network available and `GET /api/v1/verticals` returns a valid config list
- **THEN** each config is stored in `vertical_configs`, `VerticalRegistry` is populated, and the app proceeds past the SplashScreen

#### Scenario: Offline startup uses cached config
- **WHEN** the app starts with no network and a previously cached config exists in `vertical_configs`
- **THEN** `VerticalRegistry` is populated from the cached Room data and the app proceeds normally

#### Scenario: No network and no cache
- **WHEN** the app starts with no network and `vertical_configs` is empty
- **THEN** the app falls back to loading `VerticalConfig` from the static `OFCAssetTypes.kt` data for EMBER and proceeds

### Requirement: Backend exposes verticals endpoints gated by licensedVerticals
The backend SHALL expose `GET /api/v1/verticals` returning the list of `VerticalConfig` documents for the authenticated org's `licensedVerticals`, and `GET /api/v1/verticals/{code}` returning a single config.

#### Scenario: Org with default license gets Ember config
- **WHEN** an authenticated request is made to `GET /api/v1/verticals` for an org with `licensedVerticals = ["EMBER"]`
- **THEN** the response contains exactly one VerticalConfig with `vertical = "EMBER"`

#### Scenario: Unknown vertical code returns 404
- **WHEN** a request is made to `GET /api/v1/verticals/UNKNOWN`
- **THEN** the backend returns HTTP 404

### Requirement: Room database migrated to version 7 with vertical_configs table
The Room database SHALL be incremented from version 6 to version 7. The migration SHALL add a `vertical_configs` table with columns `vertical_code TEXT PRIMARY KEY` and `config_json TEXT NOT NULL`. No existing tables or columns SHALL be modified.

#### Scenario: Migration 6 to 7 is additive
- **WHEN** the app is updated on a device running Room version 6
- **THEN** the database is migrated to version 7 with the new `vertical_configs` table; all existing data is preserved

#### Scenario: VerticalConfig round-trips through Room
- **WHEN** a `VerticalConfig` is serialized to JSON and stored in `vertical_configs`, then read back and deserialized
- **THEN** the deserialized object is equal to the original
