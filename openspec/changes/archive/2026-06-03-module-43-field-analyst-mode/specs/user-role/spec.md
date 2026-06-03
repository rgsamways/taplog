## ADDED Requirements

### Requirement: UserRole enum defines all platform roles
The system SHALL define a `UserRole` enum with values: `INSPECTOR`, `FIELD_ANALYST`, `OWNER_COMMERCIAL`, `OWNER_RESIDENTIAL`, `TENANT`, `CARETAKER`, `SUPERVISOR`. Only `INSPECTOR` and `FIELD_ANALYST` are active in this module; the remaining values are reserved for future modules.

#### Scenario: Existing user with no stored role treated as INSPECTOR
- **WHEN** the app reads `userRole` from DataStore and no value is present
- **THEN** `UserRole.INSPECTOR` is returned as the default value

### Requirement: TagEventRole enum records asset registration provenance
The system SHALL define a `TagEventRole` enum with values: `OWNER`, `FIELD_ANALYST`, `INSPECTOR`, `CARETAKER`, `TENANT`. Every new `TagEvent` record SHALL carry a `registeredByRole` value reflecting who performed the registration.

#### Scenario: Asset registered by Field Analyst
- **WHEN** `QuickRegisterSheet` completes registration
- **THEN** the resulting `TagEvent.registeredByRole` equals `FIELD_ANALYST`

#### Scenario: Asset registered by Inspector (existing flow)
- **WHEN** an inspector completes `AssetRegistrationScreen`
- **THEN** the resulting `TagEvent.registeredByRole` equals `INSPECTOR` (populated from `InspectorPreferences` role context)

#### Scenario: Default for pre-existing records
- **WHEN** Room migration 9→10 runs on a device with existing `tag_events` rows
- **THEN** all existing rows receive `registeredByRole = 'OWNER'` as the default

### Requirement: Room migration v9 to v10 adds provenance columns
The system SHALL migrate the Room database from version 9 to version 10 by adding `registeredByRole` and `registeredByUserId` columns to the `assets` table, and `registeredByRole`, `registeredByUserId`, `registeredByName`, and `registeredByCertNumber` columns to the `tag_events` table. All new columns SHALL be nullable where appropriate, with `registeredByRole` defaulting to `'OWNER'`.

#### Scenario: Migration runs on existing database
- **WHEN** the app is upgraded on a device running Room v9
- **THEN** migration 9→10 runs without error; the `assets` and `tag_events` tables gain the new columns; no existing rows are deleted or modified beyond receiving default values

#### Scenario: No destructive migration fallback
- **WHEN** Room detects schema mismatch during migration
- **THEN** the migration proceeds via the declared `MIGRATION_9_10` object; `fallbackToDestructiveMigration` is NOT called

### Requirement: userRole persisted in DataStore
The system SHALL persist the user's selected role as a string in DataStore under a dedicated preferences key. The value SHALL be read as a `Flow<UserRole>` and SHALL default to `UserRole.INSPECTOR` if not set.

#### Scenario: Role written to DataStore
- **WHEN** `InspectorPreferences.setUserRole(role)` is called
- **THEN** the role's name string is written to DataStore and the `userRole` Flow emits the new value

#### Scenario: Role read from DataStore
- **WHEN** `InspectorPreferences.userRole` is collected
- **THEN** the most recently written role is emitted; if no value is stored, `UserRole.INSPECTOR` is emitted
