# OFC Checklists Spec

## Purpose

Defines how inspection checklist items are sourced and displayed for each asset type in the Ember vertical.
## Requirements
### Requirement: OFCAssetType carries checklistItems
The `VerticalAssetType` data class (replacing `OFCAssetType` as the authoritative source) SHALL include a `checklistItems: List<String>` field populated for all Ember asset types with inspection steps derived from CAN/ULC-S536:19 and applicable NFPA standards. The static `OFCAssetType.checklistItems` in `OFCAssetTypes.kt` is retained as a local fallback only.

#### Scenario: Asset type has checklist
- **WHEN** any code reads a `VerticalAssetType` from the Ember `VerticalConfig`
- **THEN** `checklistItems` is a non-empty list of plain-language inspection steps relevant to that asset type

#### Scenario: All Ember types populated in backend config
- **WHEN** the Ember `VerticalConfig` is fetched from the backend or loaded from cache
- **THEN** every `VerticalAssetType` in `assetTypeRegistry` has at least 3 `checklistItems`

### Requirement: Pre-inspection checklist displayed on InspectionFormScreen
The system SHALL display the asset type's `checklistItems` as a read-only reference section at the top of `InspectionFormScreen`, before the inspection result and notes fields. The checklist items SHALL now be sourced from `VerticalConfig.assetTypeRegistry` via `VerticalRegistry`, not from `OFCAssetTypes.kt` directly.

#### Scenario: Inspector opens inspection form
- **WHEN** the inspector taps "Inspect" from the asset detail screen
- **THEN** `InspectionFormScreen` displays a "Checklist" card containing the numbered inspection steps from the asset type's `checklistItems` (sourced from `VerticalConfig`), followed by the inspection form fields

#### Scenario: Checklist available offline
- **WHEN** the device has no network connectivity
- **THEN** the checklist is still displayed correctly (sourced from the cached `VerticalConfig` in Room, or from the static `OFCAssetTypes.kt` fallback if no cache exists)

#### Scenario: Checklist items are reference only
- **WHEN** the inspector views the checklist
- **THEN** the items are displayed as a numbered read-only list — there are no checkboxes and no per-item state is saved

