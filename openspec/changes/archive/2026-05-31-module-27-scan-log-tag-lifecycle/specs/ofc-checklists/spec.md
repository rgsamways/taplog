## ADDED Requirements

### Requirement: OFCAssetType carries checklistItems
The `OFCAssetType` data class SHALL include a `checklistItems: List<String>` field populated for all 35 asset types with inspection steps derived from CAN/ULC-S536:19 and applicable NFPA standards.

#### Scenario: Asset type has checklist
- **WHEN** any code reads an `OFCAssetType`
- **THEN** `checklistItems` is a non-empty list of plain-language inspection steps relevant to that asset type

#### Scenario: All 35 types populated
- **WHEN** the full `OFCCategory` enum is enumerated
- **THEN** every `OFCAssetType` in every category has at least 3 `checklistItems`

### Requirement: Pre-inspection checklist displayed on InspectionFormScreen
The system SHALL display the asset type's `checklistItems` as a read-only reference section at the top of `InspectionFormScreen`, before the inspection result and notes fields.

#### Scenario: Inspector opens inspection form
- **WHEN** the inspector taps "Inspect" from the asset detail screen
- **THEN** `InspectionFormScreen` displays a "Checklist" card containing the numbered inspection steps from the asset type's `checklistItems`, followed by the inspection form fields

#### Scenario: Checklist available offline
- **WHEN** the device has no network connectivity
- **THEN** the checklist is still displayed correctly (it is embedded in the app binary — no network call required)

#### Scenario: Checklist items are reference only
- **WHEN** the inspector views the checklist
- **THEN** the items are displayed as a numbered read-only list — there are no checkboxes and no per-item state is saved (this module only; per-item completion tracking is a future enhancement)
