## Why

TapLog is currently built around Ember (fire safety) as a hardcoded vertical — adding any new inspection domain requires new screens, new ViewModels, new Room migrations, and a Play Store update. This module introduces a backend-driven vertical engine so that any new vertical can be added by creating one MongoDB document and one seed script, with zero Android changes.

## What Changes

- **New Android data model**: `VerticalConfig`, `VerticalAssetType`, `InspectionFormProfile`, `FormField`, `TriggerModel`, `RoleModel`, `FieldType` Kotlin data classes, and a `VerticalRegistry` singleton
- **Room v6 → v7**: new `vertical_configs` cache table to store configs fetched from the backend
- **InspectionFormScreen refactored** to be field-driven — reads `VerticalRegistry.get(asset.vertical).formProfile.fields` instead of any Ember-specific layout; all existing Ember behaviour is preserved, now driven by data
- **OFCAssetTypes.kt migrated to backend**: Ember's asset types become the first `VerticalConfig` document in MongoDB; the Kotlin file is retained as a local fallback until the cache is proven
- **New backend API**: `GET /api/v1/verticals` and `GET /api/v1/verticals/{code}` — returns only configs for the org's `licensedVerticals`
- **`licensedVerticals: List[str]`** added to the Organisation model (billing hook; defaults to `["EMBER"]`)
- **Multi-role stub**: assets with `roleModel == MULTI_ROLE` route to a new `EntryEventScreen` shell (Hatch prep); `InspectionFormScreen` is untouched for single-inspector verticals

## Capabilities

### New Capabilities
- `vertical-config`: VerticalRegistry, VerticalConfig data model, Room v7 cache table, backend endpoints, app-startup fetch-and-cache flow
- `vertical-form-engine`: InspectionFormScreen refactored to render fields from `VerticalConfig.formProfile.fields`; EntryEventScreen stub for MULTI_ROLE assets

### Modified Capabilities
- `ofc-checklists`: checklist items are now sourced from `VerticalAssetType.checklistItems` in the cached VerticalConfig rather than the static `OFCAssetType` Kotlin enum; display behaviour is unchanged
- `org-site-sync`: Organisation model gains `licensedVerticals` field, synced from the backend and used to filter the verticals API response

## Impact

- `OFCAssetTypes.kt` — retained as local fallback; superseded by Ember's VerticalConfig document
- `InspectionFormScreen.kt` — significant refactor; no new screens required for Ember path
- `AppDatabase.kt` — version bump 6 → 7, migration adds `vertical_configs` table
- `EmberModels.kt` — Organisation gains `licensedVerticals` field
- `SyncModels.kt` / `TapLogApiService.kt` — new verticals endpoints added
- `MainActivity.kt` / `TapLogApplication.kt` — VerticalRegistry populated at startup
- Backend: new `verticals` collection in MongoDB, two new REST endpoints, Organisation schema update
