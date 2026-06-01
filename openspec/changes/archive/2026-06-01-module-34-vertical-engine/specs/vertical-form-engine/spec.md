## ADDED Requirements

### Requirement: InspectionFormScreen renders fields from VerticalConfig
`InspectionFormScreen` SHALL read `VerticalRegistry.get(asset.vertical).formProfile.fields` and render each `FormField` dynamically. The Ember config's `fields` list SHALL reproduce the current hardcoded Ember layout exactly, so existing Ember inspection behaviour is unchanged.

#### Scenario: Ember inspection form is unchanged
- **WHEN** an inspector opens the inspection form for an Ember (fire safety) asset
- **THEN** the rendered form is visually and functionally identical to the pre-Module-34 hardcoded form — same result options, notes field, deficiency flow, and checklist display

#### Scenario: Field-driven form renders TEXT field
- **WHEN** a `VerticalConfig.formProfile.fields` list contains a `FormField` with `type = TEXT` and `required = true`
- **THEN** `InspectionFormScreen` renders a mandatory text input for that field, and submission is blocked if it is empty

#### Scenario: Field-driven form renders BOOLEAN field
- **WHEN** a `FormField` with `type = BOOLEAN` is in the fields list
- **THEN** `InspectionFormScreen` renders a toggle or checkbox for that field

#### Scenario: Fields scoped to asset type are conditionally shown
- **WHEN** a `FormField` has a non-empty `applicableAssetTypes` list that does not include the current asset's type code
- **THEN** that field is not rendered on the inspection form for that asset

### Requirement: MULTI_ROLE assets route to EntryEventScreen stub
Assets whose `VerticalConfig.roleModel == MULTI_ROLE` SHALL be routed to a new `EntryEventScreen` composable instead of `InspectionFormScreen`. In this module, `EntryEventScreen` is a stub that displays a placeholder message and a back button.

#### Scenario: MULTI_ROLE asset routes to EntryEventScreen
- **WHEN** an inspector navigates to inspect an asset whose vertical has `roleModel = MULTI_ROLE`
- **THEN** `EntryEventScreen` is shown instead of `InspectionFormScreen`

#### Scenario: SINGLE_INSPECTOR asset routes to InspectionFormScreen
- **WHEN** an inspector navigates to inspect an asset whose vertical has `roleModel = SINGLE_INSPECTOR`
- **THEN** `InspectionFormScreen` is shown (unchanged routing for all current Ember assets)

### Requirement: InspectionFormScreen has no hardcoded Ember references after refactor
After the refactor, `InspectionFormScreen` SHALL contain no direct imports or references to `OFCAssetTypes`, `OFCCategory`, or any Ember-specific enum. All behaviour SHALL be driven by the `VerticalConfig` retrieved from `VerticalRegistry`.

#### Scenario: No Ember-specific imports in InspectionFormScreen
- **WHEN** the `InspectionFormScreen.kt` file is reviewed post-refactor
- **THEN** no import of `OFCCategory`, `OFCAssetType`, or `OFCAssetTypes` is present
