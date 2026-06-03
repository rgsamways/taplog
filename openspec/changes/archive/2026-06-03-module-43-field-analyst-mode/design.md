## Context

TapLog currently has a single operating mode: an authenticated inspector taps a tag and enters an inspection form. The entire navigation stack (SplashScreen → DashboardScreen → SiteDetailScreen → InspectionFormScreen) assumes this role.

The Field Analyst mode introduces a second identity: a non-inspector who scouts buildings and registers unregistered assets. These two roles share the same NFC infrastructure and Room database but diverge immediately after the splash screen. The app must route them to different home screens, different NFC tap responses, and different post-scan flows.

Current state: Room v9, one role implied but never modelled, unregistered tag taps produce a bare `AssetNotFound` state with no registration path.

## Goals / Non-Goals

**Goals:**
- Add `UserRole` + `TagEventRole` enums to the data model
- Persist `userRole` in DataStore; route navigation from SplashScreen accordingly
- Show `RoleSelectionScreen` exactly once (on first launch or when no role is stored)
- Give Field Analyst users a dedicated home, visit setup, scanning loop, and quick registration sheet
- Replace the bare AssetNotFound path with a role-aware `UnregisteredTagScreen`
- Record `registeredByRole` + `registeredByUserId` on every new Asset and TagEvent
- Room v9 → v10 migration (two ALTER TABLE, no destructive changes)

**Non-Goals:**
- Leave-behind PDF (Module 49)
- Owner, Tenant, Caretaker roles (Modules 44–46)
- Service request trail (Module 42) — depends on UserRole being present first
- Sync payload extension for new columns — deferred; columns are local-only this module
- Multi-vertical asset type picker in QuickRegisterSheet — Ember only for now

## Decisions

### D1: UserRole stored in DataStore, not Room
**Decision:** Persist `userRole` as a string in DataStore (existing `InspectorPreferences` DataStore).
**Rationale:** UserRole is a device-level identity setting, not a per-record value. It belongs alongside `authToken` and `deviceId`. Storing it in Room would require a separate table and a DAO query on every navigation decision. DataStore is already the right place; reading it as a `Flow<UserRole>` integrates cleanly with the existing ViewModel startup pattern.
**Alternative considered:** Store in Room `organisations` table — rejected because role is per-device, not per-org. An inspector and a field analyst could share the same org.

### D2: RoleSelectionScreen shown on first launch only, reset via Settings
**Decision:** `RoleSelectionScreen` is shown if and only if `userRole` is not set in DataStore. Once set, it is never shown again automatically. A "Change mode" option in Settings resets it.
**Rationale:** Asking every time is friction. A field analyst doesn't want to confirm their role on every launch. An inspector especially doesn't — they already have a full dashboard waiting.
**Alternative considered:** Show role badge in header with tap-to-switch — deferred to a future UX pass; adds complexity without clear need today.

### D3: UnregisteredTagScreen replaces AssetNotFound entirely
**Decision:** Remove the bare `ScanState.AssetNotFound` path and replace it with `ScanState.UnregisteredTag(tagId)` that routes to `UnregisteredTagScreen`.
**Rationale:** `AssetNotFound` was always a dead end — it told the user nothing useful and offered no action. Every unregistered tag is an opportunity to register an asset. Role-aware routing in `UnregisteredTagScreen` (Field Analyst → QuickRegisterSheet, Inspector → full AssetRegistrationScreen) gives both roles a productive response to an unknown tag.
**Impact:** Any code that currently checks `is ScanState.AssetNotFound` must be updated to `is ScanState.UnregisteredTag`.

### D4: QuickRegisterSheet uses Ember asset type list only
**Decision:** For this module, the asset type dropdown in `QuickRegisterSheet` pulls from `VerticalRegistry.get(TapLogVertical.EMBER).assetTypeRegistry` only.
**Rationale:** Field Analyst mode is being built for Robin's fire safety scouting use case first. Multi-vertical support in the quick-register flow is a future concern. Hardcoding Ember now avoids a cross-vertical picker design that isn't needed yet.
**Alternative considered:** Show all registered verticals — deferred; the picker UX for multi-vertical selection belongs in a dedicated design session.

### D5: registeredByRole defaults to OWNER for existing records
**Decision:** The Room v10 migration adds `registeredByRole TEXT NOT NULL DEFAULT 'OWNER'` to both `assets` and `tag_events`. Existing records get `OWNER` as their provenance.
**Rationale:** Existing records were created by the building owner via self-serve (or effectively so — no inspector role was tracked). `OWNER` is the safest conservative default: it doesn't claim inspector or field analyst provenance for records we have no provenance for.

### D6: FieldAnalystScanScreen owns the active visit in ViewModel, not a separate Visit entity
**Decision:** The "active visit" state in this module is a `String? activeVisitSiteId` StateFlow in EmberViewModel. No new Room entity for visits this module.
**Rationale:** Visit entity design (with end-of-visit summary, leave-behind PDF, visit history) belongs with Module 49 (leave-behind PDF generator). For now, a visit is just "which site am I currently adding assets to." A StateFlow in the ViewModel is sufficient. When Module 49 adds the `Visit` entity and migration, it can backfill from existing site creation timestamps.

## Risks / Trade-offs

- **`ScanState.AssetNotFound` removal** → Any future code that adds a case for `AssetNotFound` will fail to compile (good — forces update). Existing screens that handled it must be audited. Check: `MainActivity.kt`, any `when (state)` block in composable screens.
- **DataStore read latency on splash** → Reading `userRole` is async. SplashScreen already gates on `registryReady` StateFlow; `userRole` can be collected in the same coroutine scope. No visible delay expected.
- **Room migration on device with existing data** → ALTER TABLE with a DEFAULT is safe and non-destructive. No data loss. If migration fails for any reason, the existing dev practice is: Settings → Apps → TapLog → Clear Storage. Document in session notes.
- **QuickRegisterSheet asset type list depends on VerticalRegistry being ready** → SplashScreen already gates on `registryReady` before navigating anywhere. Field Analyst Dashboard can't be reached until the registry is populated. No race condition.

## Migration Plan

1. Bump `AppDatabase.VERSION` to 10
2. Add `MIGRATION_9_10` object with two ALTER TABLE statements (assets + tag_events)
3. Add `MIGRATION_9_10` to `addMigrations(...)` in `AppDatabase.getInstance(...)`
4. On device: if migration crashes, clear app storage (Settings → Apps → TapLog → Clear Storage)
5. No backend migration needed — new columns are not yet synced

## Open Questions

- Should "Change mode" in Settings be a dedicated screen or an AlertDialog? (Recommendation: AlertDialog for now — same two-card choice — no new screen needed)
- Should `FieldAnalystScanScreen` show a persistent "Finish Visit" button or require the back gesture? (Decision: explicit "Finish Visit" button — Field Analyst flow should always feel intentional, not accidental)
