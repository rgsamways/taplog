## 1. Data Model — Enums and Entity Additions

- [x] 1.1 Add `UserRole` enum to `EmberModels.kt` (INSPECTOR, FIELD_ANALYST, OWNER_COMMERCIAL, OWNER_RESIDENTIAL, TENANT, CARETAKER, SUPERVISOR)
- [x] 1.2 Add `TagEventRole` enum to `EmberModels.kt` (OWNER, FIELD_ANALYST, INSPECTOR, CARETAKER, TENANT)
- [x] 1.3 Add `registeredByRole: TagEventRole = TagEventRole.OWNER` and `registeredByUserId: String? = null` to `Asset` entity
- [x] 1.4 Add `registeredByRole: TagEventRole = TagEventRole.OWNER`, `registeredByUserId: String? = null`, `registeredByName: String? = null`, `registeredByCertNumber: String? = null` to `TagEvent` entity
- [x] 1.5 Add `TagEventRole` to `Converters.kt` TypeConverters (same pattern as other enums)

## 2. Room Migration v9 → v10

- [x] 2.1 Add `MIGRATION_9_10` object to `AppDatabase.kt` with two `ALTER TABLE assets ADD COLUMN` and four `ALTER TABLE tag_events ADD COLUMN` statements
- [x] 2.2 Bump `AppDatabase.VERSION` from 9 to 10
- [x] 2.3 Add `MIGRATION_9_10` to `addMigrations(...)` call in `AppDatabase.getInstance(...)`
- [x] 2.4 Verify build compiles clean — Room will validate migration SQL at compile time via schema export

## 3. DataStore — UserRole Persistence

- [x] 3.1 Add `USER_ROLE_KEY = stringPreferencesKey("user_role")` to `InspectorPreferences.kt`
- [x] 3.2 Add `val userRole: Flow<UserRole>` property that reads the stored string, converts to enum, defaults to `UserRole.INSPECTOR`
- [x] 3.3 Add `suspend fun setUserRole(role: UserRole)` that writes `role.name` to DataStore

## 4. ViewModel — New States and Methods

- [x] 4.1 Add `ScanState.FieldAnalystDashboard`, `ScanState.VisitSetup`, `ScanState.FieldAnalystScanning`, `ScanState.UnregisteredTag(tagId: String)` to the sealed class in `EmberViewModel.kt`
- [x] 4.2 Add `val userRole: StateFlow<UserRole>` to `EmberViewModel` — hot flow collected from `InspectorPreferences.userRole` in init block
- [x] 4.3 Add `val activeVisitSiteId: StateFlow<String?>` — MutableStateFlow, default null
- [x] 4.4 Add `val fieldAnalystAssetCount: StateFlow<Int>` — counts assets for `activeVisitSiteId` from Room
- [x] 4.5 Add `fun setUserRole(role: UserRole)` — calls `inspectorPreferences.setUserRole(role)` in viewModelScope
- [x] 4.6 Add `fun startVisit(site: Site)` — sets `_activeVisitSiteId.value = site.id`
- [x] 4.7 Add `fun endVisit()` — sets `_activeVisitSiteId.value = null`
- [x] 4.8 Add `fun registerFieldAnalystAsset(tagId, assetType, name, location, condition, siteId)` — creates Asset + TagEvent with `registeredByRole = FIELD_ANALYST` in viewModelScope
- [x] 4.9 Add `fun showUnregisteredTag(tagId: String)` — transitions to `ScanState.UnregisteredTag(tagId)`

## 5. SplashScreen and Navigation Routing

- [x] 5.1 In `SplashScreen.kt` (or wherever post-splash navigation is decided), collect `viewModel.userRole` after `registryReady`
- [x] 5.2 Route: `userRole == null / not set` → call `onNavigateToRoleSelection()`; `FIELD_ANALYST` → `onNavigateToFieldAnalystDashboard()`; `INSPECTOR` (default) → `onNavigateToDashboard()` (existing)
- [x] 5.3 Wire new navigation lambdas in `MainActivity.kt` (pass them to SplashScreen composable)

## 6. RoleSelectionScreen

- [x] 6.1 Create `RoleSelectionScreen.kt` in `ui/ember/`
- [x] 6.2 Full-screen layout: TapLog logo, heading "How will you use TapLog?", two `Card` composables side by side
- [x] 6.3 Card 1: "Inspect assets" — inspector icon, subtext — on tap calls `viewModel.setUserRole(INSPECTOR)` then navigates to DashboardScreen
- [x] 6.4 Card 2: "Scout a building" — compass icon, subtext — on tap calls `viewModel.setUserRole(FIELD_ANALYST)` then navigates to FieldAnalystDashboardScreen
- [x] 6.5 Wire `RoleSelectionScreen` into MainActivity navigation

## 7. FieldAnalystDashboardScreen

- [x] 7.1 Create `FieldAnalystDashboardScreen.kt` in `ui/ember/`
- [x] 7.2 Header: "Field Analyst" badge chip + current date
- [x] 7.3 Active visit card (shown when `activeVisitSiteId != null`): building name, address, asset count; tap navigates to `FieldAnalystScanScreen`
- [x] 7.4 "Start New Visit" `Button` (shown when no active visit) — navigates to `VisitSetupScreen`
- [x] 7.5 Recent sites list: `LazyColumn` of last 5 sites from `EmberViewModel.sitesForOrg`, showing name, asset count, and date
- [x] 7.6 Stat chip: total assets tagged (count of all assets in Room for this org)
- [x] 7.7 Wire into MainActivity navigation

## 8. VisitSetupScreen

- [x] 8.1 Create `VisitSetupScreen.kt` in `ui/ember/`
- [x] 8.2 Form fields: building name (required), address (required), city (required, default "Bancroft"), postal code (optional), building type `DropdownMenu` (Commercial / Restaurant / Brewery / Residential / Other), owner name (optional), owner phone (optional), notes (optional)
- [x] 8.3 Input validation: highlight empty required fields on "Begin Visit" tap; do not navigate if validation fails
- [x] 8.4 "Begin Visit" button calls `viewModel.saveSite(...)` then `viewModel.startVisit(site)` then navigates to `FieldAnalystScanScreen`
- [x] 8.5 Wire into MainActivity navigation

## 9. FieldAnalystScanScreen

- [x] 9.1 Create `FieldAnalystScanScreen.kt` in `ui/ember/`
- [x] 9.2 Header: building name + "X assets" count (from `fieldAnalystAssetCount` StateFlow)
- [x] 9.3 Centre NFC area: large pulsing ring animation (reuse or adapt SplashScreen Canvas pattern) + "Tap a tag to register it" prompt
- [x] 9.4 Scrollable asset list below NFC area: assets registered to `activeVisitSiteId`, showing type icon + name + location
- [x] 9.5 "Add Manually" `FloatingActionButton` — opens `QuickRegisterSheet` with a generated UUID as tagId
- [x] 9.6 "Finish Visit" `Button` at bottom — calls `viewModel.endVisit()`, navigates back to `FieldAnalystDashboardScreen`
- [x] 9.7 Handle `ScanState.UnregisteredTag` from NFC: opens `QuickRegisterSheet` with scanned tagId
- [x] 9.8 Handle registered tag NFC tap: show inline `AssetCard` (reuse existing asset card composable) — no full-screen navigation
- [x] 9.9 Wire into MainActivity navigation

## 10. QuickRegisterSheet

- [x] 10.1 Create `QuickRegisterSheet.kt` in `ui/ember/` as a `ModalBottomSheet` composable
- [x] 10.2 Tag ID chip at top (read-only display, "Manual entry" if manually added)
- [x] 10.3 Asset type `DropdownMenu` populated from `VerticalRegistry.get(TapLogVertical.EMBER).assetTypeRegistry`
- [x] 10.4 Asset name `OutlinedTextField` — auto-filled from type selection, editable
- [x] 10.5 Location `OutlinedTextField` — free text, hint "e.g. Kitchen, Main lobby, Boiler room"
- [x] 10.6 Condition segmented `SingleChoiceSegmentedButtonRow`: Good / Fair / Poor / Non-functional
- [x] 10.7 Photo button (optional) — camera capture, thumbnail shown inline
- [x] 10.8 "Register Asset" `Button` — calls `viewModel.registerFieldAnalystAsset(...)`, dismisses sheet
- [x] 10.9 Pass tagId as parameter; expose via `onDismiss` and `onRegistered` callbacks

## 11. UnregisteredTagScreen

- [x] 11.1 Create `UnregisteredTagScreen.kt` in `ui/ember/`
- [x] 11.2 Large NFC ring icon (with question mark overlay) using Canvas
- [x] 11.3 Heading: "This asset isn't in TapLog yet"
- [x] 11.4 Subtext: "Register it now to start its compliance record."
- [x] 11.5 "Register This Asset" button — role-aware: FIELD_ANALYST opens `QuickRegisterSheet`; INSPECTOR navigates to `AssetRegistrationScreen`
- [x] 11.6 "Not Now" text button — navigates back
- [x] 11.7 Accepts `tagId: String` and `userRole: UserRole` as parameters

## 12. MainActivity — NFC Routing Update

- [x] 12.1 Locate the NFC tag-not-found path in `MainActivity.kt` (currently sets `ScanState.AssetNotFound`)
- [x] 12.2 Replace `viewModel.setAssetNotFound()` (or equivalent) with `viewModel.showUnregisteredTag(tagId)`
- [x] 12.3 Update all `when (state)` branches: replace `is ScanState.AssetNotFound` with `is ScanState.UnregisteredTag` — pass `state.tagId` to `UnregisteredTagScreen`
- [x] 12.4 Add navigation cases for `FieldAnalystDashboard`, `VisitSetup`, `FieldAnalystScanning` scan states

## 13. AssetDetailScreen — Provenance Card

- [x] 13.1 In `AssetDetailScreen.kt`, add a query to fetch the first `TagEvent` for the current asset (ordered by `attachedAt ASC`, limit 1)
- [x] 13.2 Add `EmberDAO` query: `getFirstTagEventForAsset(assetId: String): TagEvent?`
- [x] 13.3 Surface provenance card below asset header: "Registered by [registeredByName ?: "owner"] as [role display] on [date]"
- [x] 13.4 Only show the provenance card if a `TagEvent` exists for the asset

## 14. Build Verification

- [x] 14.1 `./gradlew assembleDebug` passes with no errors
- [x] 14.2 On device: fresh install → splash → `RoleSelectionScreen` appears
- [x] 14.3 Select "Scout a building" → `FieldAnalystDashboardScreen` shown; role persists on app restart
- [x] 14.4 Start new visit → `VisitSetupScreen` → fill in → `FieldAnalystScanScreen` with pulsing NFC area
- [ ] 14.5 Tap unregistered NFC tag → `QuickRegisterSheet` opens → register asset → count increments
- [ ] 14.6 Existing INSPECTOR flow: select "Inspect assets" on first launch → existing `DashboardScreen` and inspection flow unchanged
- [ ] 14.7 DB Inspector: `assets` and `tag_events` tables show `registeredByRole` column with `OWNER` default on pre-existing rows
