## Why

TapLog's current single-mode design assumes every user is a certified inspector running a full inspection flow. The GTM breakthrough for the platform is a pre-inspection analyst role: Robin (and eventually trained scouts) walks into a building, tags every compliance-relevant asset, and hands the owner a leave-behind — no certification required, no inspection form, pure asset capture. This unlocks a direct revenue stream today (scouting service) and seeds the building-owner subscription at the same time.

## What Changes

- New `UserRole` enum (INSPECTOR, FIELD_ANALYST + 5 reserved for future modules)
- New `TagEventRole` enum (OWNER, FIELD_ANALYST, INSPECTOR, CARETAKER, TENANT) — tracks who first registered each asset
- Room v9 → v10 migration: `registeredByRole` + `registeredByUserId` columns added to `assets` and `tag_events`
- `userRole` persisted in DataStore; defaults to INSPECTOR for existing users
- `RoleSelectionScreen` — shown once on first launch if no role set; two options (Inspector / Scout a building)
- `FieldAnalystDashboardScreen` — replaces DashboardScreen as home when role is FIELD_ANALYST
- `VisitSetupScreen` — begin a scouting visit (building name, address, type, optional owner contact)
- `FieldAnalystScanScreen` — main NFC scanning loop during a visit
- `QuickRegisterSheet` — bottom sheet for fast asset registration (~30 seconds per asset)
- `UnregisteredTagScreen` — role-aware handler replacing current AssetNotFound path
- SplashScreen navigation updated: post-auth, routes to RoleSelectionScreen / FieldAnalystDashboardScreen / DashboardScreen based on stored role
- MainActivity NFC routing updated: unregistered tag taps go to UnregisteredTagScreen

## Capabilities

### New Capabilities

- `field-analyst-mode`: Role selection, Field Analyst dashboard, visit setup and scanning loop, quick asset registration, and role-aware unregistered tag handling
- `user-role`: UserRole + TagEventRole enums, DataStore persistence of userRole, Room v9→v10 migration adding registeredByRole/registeredByUserId to assets and tag_events

### Modified Capabilities

- `tag-lifecycle`: TagEvent gains `registeredByRole`, `registeredByUserId`, `registeredByName`, `registeredByCertNumber` fields — the "birthing event" provenance record
- `scan-log`: Unregistered tag path now routes to `UnregisteredTagScreen` instead of a bare AssetNotFound state; role-aware continuation after the screen

## Impact

- **Android:** EmberModels.kt (new enums + entity columns), AppDatabase.kt (v10 migration), InspectorPreferences.kt (userRole DataStore key), EmberViewModel.kt (new states + methods), MainActivity.kt (NFC routing), SplashScreen.kt (navigation routing), 5 new screen files
- **Room DB:** Version bump 9 → 10; two ALTER TABLE statements on `assets` and `tag_events`; existing rows get DEFAULT 'OWNER' for registeredByRole
- **No backend changes** — new columns are local-only for this module; sync payload extension deferred
- **No breaking changes** to existing Inspector flow
