# Module 43 — Context File
> For: `/opsx:propose "module-43-field-analyst-mode"`
> Written: 2026-06-03

---

## What this module is

Add **Field Analyst mode** to TapLog — a second operating mode for users who are scouting buildings and registering unregistered assets, rather than inspecting already-registered assets.

This is the GTM unlock for Robin's pre-inspection analyst business. It enables walking into a building (brewery, pub, restaurant), tagging every compliance-relevant asset, and handing the owner a leave-behind document — all from the TapLog app. No certification required. No inspection form. Pure asset capture.

---

## Scope boundary

**In scope:**
- `UserRole` enum — 2 values minimum: `INSPECTOR`, `FIELD_ANALYST` (full 7-value enum from spec is fine)
- `TagEventRole` enum — 5 values: `OWNER`, `FIELD_ANALYST`, `INSPECTOR`, `CARETAKER`, `TENANT`
- Room migration v9 → v10 — adds `registeredByRole` and `registeredByUserId` columns to `assets` and `tag_events` tables
- DataStore: store `userRole` (defaults to `INSPECTOR` for existing users)
- `RoleSelectionScreen` — shown once after splash if `userRole` not set. Two cards: Inspector / Field Analyst. No web roles (owner, tenant, caretaker) in this screen.
- `FieldAnalystDashboardScreen` — home screen when role is `FIELD_ANALYST`
- `VisitSetupScreen` — begin a new scouting visit (building name, address, type, optional owner contact)
- `FieldAnalystScanScreen` — main scanning loop during a visit
- `QuickRegisterSheet` — bottom sheet: fast asset registration (~30 seconds)
- `UnregisteredTagScreen` — role-aware handler when any user taps an unregistered tag
- `SplashScreen.kt` modification — after `registryReady`, check `userRole` from DataStore and navigate to the right home screen
- `MainActivity.kt` modification — pass `userRole` to NFC routing so unregistered tag taps go to `UnregisteredTagScreen` when appropriate
- `EmberModels.kt` modifications — add `registeredByRole: TagEventRole` and `registeredByUserId: String?` to `Asset` and `TagEvent` entities with defaults

**Out of scope (this module):**
- Leave-behind PDF generation (Module 49)
- `OWNER_COMMERCIAL`, `OWNER_RESIDENTIAL`, `TENANT`, `CARETAKER` roles — those come in Modules 44–46
- Web dashboard (Module 44)
- Tenancy records (Module 45)
- Caretaker check-in events (Module 46)
- Service requests (Module 42) — separate module, depends on UserRole being present first
- Notification architecture (Module 48)

---

## Room migration: v9 → v10

**Two tables get new columns (ALTER TABLE):**

```sql
-- assets
ALTER TABLE assets ADD COLUMN registeredByRole TEXT NOT NULL DEFAULT 'OWNER';
ALTER TABLE assets ADD COLUMN registeredByUserId TEXT;

-- tag_events
ALTER TABLE tag_events ADD COLUMN registeredByRole TEXT NOT NULL DEFAULT 'OWNER';
ALTER TABLE tag_events ADD COLUMN registeredByUserId TEXT;
ALTER TABLE tag_events ADD COLUMN registeredByName TEXT;
ALTER TABLE tag_events ADD COLUMN registeredByCertNumber TEXT;
```

No new tables in this module. New tables (`check_in_events`, `service_requests`, `tenancy_records`, `move_in_records`) come in later modules.

**AppDatabase** bumps to version 10. Add `MIGRATION_9_10` object.

---

## DataStore changes

`InspectorPreferences.kt` needs one new preference key:

```kotlin
val USER_ROLE_KEY = stringPreferencesKey("user_role")

// Read
val userRole: Flow<UserRole> = dataStore.data.map { prefs ->
    val roleStr = prefs[USER_ROLE_KEY] ?: UserRole.INSPECTOR.name
    runCatching { UserRole.valueOf(roleStr) }.getOrDefault(UserRole.INSPECTOR)
}

// Write
suspend fun setUserRole(role: UserRole) {
    dataStore.edit { it[USER_ROLE_KEY] = role.name }
}
```

---

## New enums (EmberModels.kt)

```kotlin
enum class UserRole {
    INSPECTOR,
    FIELD_ANALYST,
    OWNER_COMMERCIAL,
    OWNER_RESIDENTIAL,
    TENANT,
    CARETAKER,
    SUPERVISOR
}

enum class TagEventRole {
    OWNER,
    FIELD_ANALYST,
    INSPECTOR,
    CARETAKER,
    TENANT
}
```

---

## ScanState additions (EmberViewModel.kt)

```kotlin
// New states
object FieldAnalystDashboard : ScanState()
data class VisitSetup(val visitInProgress: Boolean) : ScanState()
object FieldAnalystScanning : ScanState()
data class UnregisteredTag(val tagId: String) : ScanState()
```

New ViewModel properties:
- `userRole: StateFlow<UserRole>` — hot, read from DataStore on init
- `activeVisitSiteId: StateFlow<String?>` — the site ID of the current Field Analyst visit (null = no active visit)
- `fieldAnalystAssetCount: StateFlow<Int>` — asset count for the active visit site

New ViewModel methods:
- `setUserRole(role: UserRole)` — saves to DataStore
- `startVisit(site: Site)` — saves site, sets activeVisitSiteId
- `endVisit()` — clears activeVisitSiteId
- `registerFieldAnalystAsset(tagId, type, name, location, condition, siteId)` — creates Asset + TagEvent with `registeredByRole = FIELD_ANALYST`
- `showUnregisteredTag(tagId: String)` — transitions to `ScanState.UnregisteredTag`

---

## Screen designs

### `RoleSelectionScreen`
- Full-screen, navy background
- TapLog logo at top
- Heading: "How will you use TapLog?"
- Two large tappable cards side by side:
  - **"Inspect assets"** — inspector icon, subtext: "I'm a certified inspector completing inspections" → sets INSPECTOR, navigates to DashboardScreen
  - **"Scout a building"** — compass/map icon, subtext: "I'm registering assets in a new building" → sets FIELD_ANALYST, navigates to FieldAnalystDashboardScreen
- Shown once only. Reset: Settings → "Change mode."
- No "owner" / "tenant" / "caretaker" options here — those are web-only for this module

### `FieldAnalystDashboardScreen`
- Header: "Field Analyst" badge + date
- Active visit card (teal background if visit in progress): building name, address, number of assets tagged today. Tap → `FieldAnalystScanScreen`
- "Start New Visit" large button (shown when no active visit) → `VisitSetupScreen`
- Recent visits list: last 5 sites with asset counts and date. Tap → site detail.
- Total stat chip: "X assets tagged lifetime"

### `VisitSetupScreen`
- Title: "New Visit"
- Fields:
  - Building name (required)
  - Address (required)
  - City (required, default "Bancroft")
  - Postal code (optional)
  - Building type (DropdownMenu: Commercial / Restaurant/Food Service / Brewery / Residential / Other)
  - Owner name (optional)
  - Owner phone (optional)
  - Notes (optional)
- "Begin Visit" button → calls `viewModel.saveSite(...)` then `viewModel.startVisit(site)` → navigates to `FieldAnalystScanScreen`

### `FieldAnalystScanScreen`
- Header: building name + "X assets tagged" count
- Large centre area: pulsing NFC ring animation + "Tap a tag to register it"
- Below NFC area: scrollable list of assets registered this visit (icon + name + location)
- "Add Manually" floating button → opens `QuickRegisterSheet` without a tag scan (tagId = generated UUID, flag as manual)
- "Finish Visit" button at bottom → navigates back to `FieldAnalystDashboardScreen` (leave-behind generation is Module 49)
- **On NFC tap of unregistered tag:** → `QuickRegisterSheet(tagId = scannedTagId)`
- **On NFC tap of registered tag:** → inline asset detail card (not full screen), shows name/location/last inspected

### `QuickRegisterSheet` (ModalBottomSheet)
- Purpose: register a new asset in under 30 seconds
- Title: "Register Asset" + tag ID chip
- Fields:
  - Asset type — DropdownMenu from `VerticalRegistry.get(EMBER).assetTypeRegistry` labels (or cross-vertical picker if multiple verticals available)
  - Asset name — pre-filled from type selection, editable
  - Location in building — free text ("Kitchen", "Main lobby", "Boiler room")
  - Condition — segmented button row: Good / Fair / Poor / Non-functional
  - Photo — optional camera capture button
- "Register Asset" primary button → creates `Asset` + `TagEvent` with `registeredByRole = FIELD_ANALYST`, `registeredByUserId = inspectorId from JWT`
- Sheet dismisses, `FieldAnalystScanScreen` updates count

### `UnregisteredTagScreen`
- Shown when any user taps an NFC tag not in Room (replaces the current `ScanState.AssetNotFound` path)
- Icon: large NFC ring with question mark
- Heading: "This asset isn't in TapLog yet"
- Subtext: "Register it now to start its compliance record."
- Two buttons:
  - **"Register This Asset"** — role-aware:
    - `FIELD_ANALYST` → `QuickRegisterSheet`
    - `INSPECTOR` → `AssetRegistrationScreen` (existing flow) then `InspectionFormScreen`
  - **"Not Now"** → back
- tagId is passed through and used in registration

---

## MainActivity.kt changes

Current flow on NFC tap:
- Tag in Room → `ScanState.AssetFound` → `AssetDetailScreen` or `Inspecting`
- Tag not in Room → `ScanState.AssetNotFound` → "Asset Not Found" screen

New flow:
- Tag in Room → same as current
- Tag not in Room → `viewModel.showUnregisteredTag(tagId)` → `ScanState.UnregisteredTag` → `UnregisteredTagScreen`

The `userRole` must be observable in `MainActivity` to route `UnregisteredTagScreen` actions correctly (FIELD_ANALYST gets sheet, INSPECTOR gets full registration).

---

## SplashScreen.kt / navigation changes

After `registryReady` and auth check:
1. Read `userRole` from DataStore (via ViewModel StateFlow)
2. If `userRole == FIELD_ANALYST` → navigate to `FieldAnalystDashboardScreen`
3. If `userRole == INSPECTOR` (default) → navigate to `DashboardScreen` (existing)
4. If `userRole` not set (new install) → navigate to `RoleSelectionScreen`

`RoleSelectionScreen` replaces the first-launch check for `userRole` not set.

---

## What does NOT change

- Ember inspection flow — untouched
- `InspectionFormScreen`, `DashboardScreen`, `SiteListScreen` — untouched
- PDF report generation — untouched
- Auth flow — untouched
- All existing `ScanState` values — new states added, none removed
- Sync architecture — `registeredByRole`/`registeredByUserId` fields get default values on existing records; sync models need to include them on new records

---

## Verification

1. Fresh install → splash → `RoleSelectionScreen` → select "Scout a building" → `FieldAnalystDashboardScreen`
2. `FieldAnalystDashboardScreen` → "Start New Visit" → `VisitSetupScreen` → fill in building → "Begin Visit" → `FieldAnalystScanScreen`
3. In `FieldAnalystScanScreen`, tap an unregistered NFC tag → `QuickRegisterSheet` opens → fill in type, location, condition → "Register Asset" → sheet closes, count increments
4. Tap a registered tag in `FieldAnalystScanScreen` → inline asset card shows (no full navigation)
5. Existing INSPECTOR users: `userRole` not set in DataStore → `RoleSelectionScreen` → select "Inspect assets" → existing `DashboardScreen` unchanged
6. DB Inspector confirms Room v10: `assets` and `tag_events` tables have `registeredByRole` column with default 'OWNER' on pre-existing rows
