## 1. Android — Vertical data model

- [x] 1.1 Create `TriggerModel`, `RoleModel`, and `FieldType` enums in a new `VerticalModels.kt` file
- [x] 1.2 Create `FormField`, `InspectionFormProfile`, `VerticalAssetType`, and `VerticalConfig` data classes in `VerticalModels.kt`
- [x] 1.3 Implement `VerticalRegistry` singleton with `register()`, `get()`, and `all()` methods; `get()` throws `IllegalStateException` for unregistered verticals

## 2. Android — Room v7 migration

- [x] 2.1 Add `VerticalConfigEntity` Room entity (`vertical_code TEXT PRIMARY KEY`, `config_json TEXT NOT NULL`) to `AppDatabase.kt`
- [x] 2.2 Write Room migration 6→7 that adds the `vertical_configs` table with no changes to existing tables
- [x] 2.3 Bump `AppDatabase` version to 7 and wire in the migration
- [x] 2.4 Add `VerticalConfigDao` with `upsert(entity)` and `getAll()` methods

## 3. Android — Backend API client

- [x] 3.1 Add `GET /api/v1/verticals` and `GET /api/v1/verticals/{code}` to `TapLogApiService.kt` returning `List<VerticalConfig>` and `VerticalConfig` respectively
- [x] 3.2 Add JSON TypeConverters for `VerticalConfig` serialization/deserialization (using Gson or existing converter strategy)

## 4. Android — Startup fetch and registry population

- [x] 4.1 In `TapLogApplication.onCreate()`, launch a coroutine that fetches configs from `GET /api/v1/verticals`, upserts each into `vertical_configs` via `VerticalConfigDao`, and calls `VerticalRegistry.register()` for each
- [x] 4.2 Add offline fallback: if fetch fails, load from `VerticalConfigDao.getAll()` and register from cache
- [x] 4.3 Add static fallback: if cache is empty, build and register Ember's `VerticalConfig` from `OFCAssetTypes.kt` data
- [x] 4.4 Make `SplashScreen` wait until `VerticalRegistry` has at least one entry registered before navigating to the main app

## 5. Android — Ember VerticalConfig definition

- [x] 5.1 Author the Ember `VerticalConfig` as a Kotlin function (e.g. `EmberVerticalConfig.build()`) that translates all `OFCCategory` / `OFCAssetType` entries into `VerticalAssetType` instances — used as the static fallback
- [x] 5.2 Author the Ember `InspectionFormProfile` fields list to reproduce the current `InspectionFormScreen` layout exactly: result selector (PASS/FAIL/REQUIRES_ATTENTION), notes text field, deficiency list — `deficienciesEnabled = true`, `photoRequired = false`

## 6. Android — InspectionFormScreen refactor

- [x] 6.1 Remove all direct references to `OFCCategory`, `OFCAssetType`, and `OFCAssetTypes` from `InspectionFormScreen.kt`
- [x] 6.2 Replace the hardcoded checklist section with a lookup: `VerticalRegistry.get(asset.vertical).assetTypeRegistry.find { it.code == asset.ofcTypeCode }?.checklistItems`
- [x] 6.3 Replace hardcoded form fields with a loop over `VerticalRegistry.get(asset.vertical).formProfile.fields`, rendering each `FormField` by its `FieldType`
- [x] 6.4 Verify the refactored Ember inspection form is visually and functionally identical to the pre-Module-34 version

## 7. Android — EntryEventScreen stub and routing

- [x] 7.1 Create `EntryEventScreen.kt` composable with a placeholder message ("Multi-role entry form — coming soon") and a back button
- [x] 7.2 In the navigation graph, route assets with `VerticalConfig.roleModel == MULTI_ROLE` to `EntryEventScreen` instead of `InspectionFormScreen`

## 8. Android — Organisation model update

- [x] 8.1 Add `licensedVerticals: List<String> = listOf("EMBER")` field to the `Organisation` data class in `EmberModels.kt`
- [x] 8.2 Add a Room TypeConverter for `List<String>` if not already present
- [x] 8.3 Update `SyncModels.kt` Organisation DTO to include `licensedVerticals` in the POST payload and response deserialization

## 9. Backend — Verticals collection and endpoints

- [ ] 9.1 Create the `verticals` MongoDB collection with an Ember seed document matching the `VerticalConfig` JSON schema
- [ ] 9.2 Implement `GET /api/v1/verticals` — returns configs filtered by the authenticated org's `licensed_verticals`
- [ ] 9.3 Implement `GET /api/v1/verticals/{code}` — returns a single config or 404
- [ ] 9.4 Add `licensed_verticals: List[str] = ["EMBER"]` to the Organisation model and ensure it is included in the Organisation API response
<!-- NOTE: Backend repo not present in this workspace — tasks 9.1–9.4 require implementation in the backend service repo -->

## 10. Validation

- [x] 10.1 Confirm `OFCCategory.findByCode()` still resolves correctly via the static fallback path
- [x] 10.2 Confirm Room migration 6→7 runs cleanly on a device with existing data
- [x] 10.3 Confirm the Ember inspection flow end-to-end: scan NFC → asset detail → inspect → checklist shown → submit → inspection record saved
- [x] 10.4 Confirm `VerticalRegistry.get(ANCHOR)` throws before ANCHOR is registered
- [x] 10.5 Build the project with no compilation errors
