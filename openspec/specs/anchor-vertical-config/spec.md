# anchor-vertical-config Specification

## Purpose
TBD - created by syncing change module-37-anchor-config. Update Purpose after archive.
## Requirements
### Requirement: Anchor VerticalConfig document is seeded in MongoDB
The backend `verticals` collection SHALL contain a document with `vertical: "ANCHOR"` representing the complete Anchor (fall protection) vertical configuration, including all six asset types, result options, form fields, checklist items, and regulatory metadata.

#### Scenario: Anchor document exists after seeding
- **WHEN** the seed script `scripts/seed_anchor_vertical.py` is run against the MongoDB instance
- **THEN** `db.verticals.findOne({ vertical: "ANCHOR" })` returns a non-null document with `display_name: "Fall Protection"`, `trigger_model: "PRE_USE"`, and `role_model: "SINGLE_INSPECTOR"`

#### Scenario: Seed script is idempotent
- **WHEN** `scripts/seed_anchor_vertical.py` is run twice against the same database
- **THEN** exactly one ANCHOR document exists in the `verticals` collection (no duplicates)

### Requirement: Anchor VerticalConfig defines six asset types
The Anchor config SHALL define asset types HAR (Full-Body Harness), LAN (Lanyard / Energy Absorber), SRL (Self-Retracting Lifeline), ANC (Anchor Point), HLL (Horizontal Lifeline), and RKT (Rescue / Retrieval Kit), each with a non-empty `checklist_items` list and a `trigger_config` appropriate to the asset type.

#### Scenario: Pre-use assets carry PRE_USE trigger
- **WHEN** the Anchor VerticalConfig is retrieved and the HAR, LAN, or SRL asset type is read
- **THEN** the asset type's `trigger_config.type` is `"PRE_USE"` and `interval_months` is null

#### Scenario: Annual-cert assets carry CALENDAR trigger
- **WHEN** the Anchor VerticalConfig is retrieved and the ANC, HLL, or RKT asset type is read
- **THEN** the asset type's `trigger_config.type` is `"CALENDAR"` and `trigger_config.interval_months` is 12

#### Scenario: Every asset type has checklist items
- **WHEN** any asset type in the Anchor VerticalConfig is read
- **THEN** `checklist_items` is a non-empty list of plain-language inspection steps

### Requirement: Anchor result options are PASS and REMOVE_FROM_SERVICE only
The Anchor form profile SHALL define exactly two result options: `PASS` (ResultAction NONE) and `REMOVE_FROM_SERVICE` (ResultAction REMOVE_FROM_SERVICE). No intermediate result option SHALL exist.

#### Scenario: Pass result carries no action
- **WHEN** the Anchor form profile result options are read and the PASS option is selected
- **THEN** `result_action` is `"NONE"`

#### Scenario: Fail result carries remove-from-service action
- **WHEN** the Anchor form profile result options are read and the REMOVE_FROM_SERVICE option is selected
- **THEN** `result_action` is `"REMOVE_FROM_SERVICE"` and `label` is `"Remove from service"`

### Requirement: Anchor form profile includes condition notes and last cert date fields
The Anchor form profile SHALL include a `condition_notes` TEXT field and a `last_cert_date` DATE field, both optional. Deficiencies SHALL be enabled.

#### Scenario: Form fields are present and typed correctly
- **WHEN** the Anchor VerticalConfig form profile fields are read
- **THEN** a field with `key: "condition_notes"` and `type: "TEXT"` exists, and a field with `key: "last_cert_date"` and `type: "DATE"` exists
