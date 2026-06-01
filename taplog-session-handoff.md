# TapLog — Session Handoff
> Session date: 2026-06-01
> Prepared for: next Claude session

---

## How to start the next session

1. Upload all md docs: `taplog-spec.md`, `taplog-curriculum.md`, `taplog-investor-summary.md`, `taplog-lightbulbs.md`, `taplog-md-prime-directives.md`, `taplog-pilot-tester-pool.md`, `taplog-session-handoff.md`
2. Read `taplog-md-prime-directives.md` first and confirm you've read it
3. **Before anything else:** complete backend tasks 9.1–9.4 in `C:\dev\taplog-api` (see below)
4. Next Android module is **Module 35 — Anchor Config** (backend-only, zero Android changes) OR back to **Module 31 — Visual Asset Identification**

---

## What was accomplished this session

Session 10 was an architectural pivot session. Module 34 (vertical engine) was prioritized over Modules 31–33 because the vertical engine is the foundational layer that all future verticals and multi-role flows depend on. The Android app is now a vertical factory.

### Module 34 — Vertical Engine (COMPLETE on Android, backend pending)

**What was built:**

*Architecture:*
- `VerticalModels.kt` — full data model: `TriggerModel`, `RoleModel`, `FieldType`, `FormField`, `InspectionFormProfile`, `VerticalAssetType`, `VerticalConfig`, `VerticalRegistry` singleton
- `ResultOption` + `ResultAction` enum — replace `List<String>` for result options; downstream actions (REMOVE_FROM_SERVICE, NOTIFY_AUTHORITY, ISSUE_CERTIFICATE, DELIVER_REPORT) encoded in the config, not hardcoded
- `TriggerConfig` replaces `intervalMonths: Int?` — supports CALENDAR (months), MILEAGE, ENGINE_HOURS; `TriggerModel` gains `MILEAGE` and `ENGINE_HOURS` values
- `EmberVerticalConfig.build()` — translates all `OFCCategory` / `OFCAssetType` entries to `VerticalAssetType` instances; used as cold-start static fallback

*Room:*
- Version 6 → 7 (`MIGRATION_6_7`): adds `vertical_configs` table (JSON blob cache)
- Version 7 → 8 (`MIGRATION_7_8`): adds `organisations.licensedVerticals` TEXT column
- `VerticalConfigEntity` + `VerticalConfigDao` (upsert + getAll)

*Startup:*
- `TapLogApplication.initVerticalRegistry()` — fetch from `GET /api/v1/verticals` → upsert to Room → register in VerticalRegistry; offline fallback to Room cache; cold-start fallback to `EmberVerticalConfig.build()`
- `SplashScreen` now accepts `registryReady: Boolean` — tap-to-continue disabled until registry is populated
- `verticalRegistryReady: StateFlow<Boolean>` exposed from `TapLogApplication`

*InspectionFormScreen refactor:*
- All `OFCCategory`, `OFCAssetType`, `OFCAssetTypes` imports removed
- Checklist sourced from `VerticalRegistry.get(asset.vertical).assetTypeRegistry.find { it.code == asset.assetType }?.checklistItems`
- Result selector iterates `formProfile.resultOptions` (List<ResultOption>); displays `option.label`; maps `option.code` back to `InspectionResult.valueOf()` on submit
- Dynamic fields loop over `formProfile.fields`, rendering by `FieldType` (TEXT, NUMBER, BOOLEAN; others as text fallback)
- Deficiencies section conditional on `formProfile.deficienciesEnabled`

*Routing:*
- `EntryEventScreen.kt` — stub composable, "Multi-role entry form — coming soon" + back button
- `MainActivity` Inspecting state: checks `VerticalRegistry.get(asset.vertical).roleModel`; routes `MULTI_ROLE` → `EntryEventScreen`, `SINGLE_INSPECTOR` → `InspectionFormScreen`

*Organisation:*
- `Organisation.licensedVerticals: List<String> = listOf("EMBER")` added
- `List<String>` TypeConverter added to `Converters.kt`
- `OrganisationSyncRequest` and `toSyncRequest()` updated to include `licensedVerticals`

*API:*
- `TapLogApiService` gains `getVerticals()` and `getVertical(code)` GET endpoints

*OpenSpec:*
- Module 34 archived to `openspec/changes/archive/2026-06-01-module-34-vertical-engine/`
- 4 specs synced to `openspec/specs/`: `vertical-config` (new), `vertical-form-engine` (new), `ofc-checklists` (updated), `org-site-sync` (updated)
- `openspec/design-docs/inspection-cardinality.md` — open design doc stub; blocks Fleet, Hatch multi-asset sessions, Anchor batch pre-use

---

## Backend tasks required before Module 35 or any vertical config testing

These 4 tasks must be completed in `C:\dev\taplog-api` before the vertical engine is operational:

1. Add `licensed_verticals: List[str] = ["EMBER"]` to the Organisation Pydantic model; include in Organisation API response
2. Create `verticals` MongoDB collection; seed with Ember `VerticalConfig` JSON document (full asset type registry + form profile with `ResultOption` + `TriggerConfig` structures)
3. Implement `GET /api/v1/verticals` — returns configs filtered by `org.licensed_verticals`; requires Bearer auth
4. Implement `GET /api/v1/verticals/{code}` — returns single config or 404; requires Bearer auth

Until these are deployed, the app falls back to `EmberVerticalConfig.build()` (static Kotlin data) on every cold start. The Ember path is fully functional regardless.

---

## Current state of the app

**Android (Room v8, build passing):**
- Vertical engine implemented; Ember path fully field-driven via `VerticalConfig`
- `OFCAssetTypes.kt` retained as static fallback — not deleted
- `InspectionFormScreen` has zero OFC imports; identical behaviour for Ember
- `EntryEventScreen` stub in place for MULTI_ROLE routing
- Cold-start fallback chain working: backend fetch → Room cache → static Ember config

**Backend (pending):**
- Tasks 9.1–9.4 (see above) not yet implemented
- All existing sync endpoints unchanged and working
- Ember inspection flow end-to-end unaffected

**Not yet built:**
- Module 31 — Visual Asset Identification (deferred; Module 34 prioritized)
- Module 32 — Inspection guidance Level 2 (deferred)
- Module 33 — AI inspection co-pilot (deferred)
- Module 35 — Anchor config in backend (backend-only; zero Android changes)
- Module 36 — Hatch config + EntryEventScreen implementation
- Inspection cardinality (open design doc — blocks Fleet/Hatch multi-asset)
- Billing (before OAFC November 2026)

---

## Next module options

**Option A — Complete the vertical engine (recommended first):**
Complete backend tasks 9.1–9.4 in taplog-api, then `/opsx:propose "module-35-anchor-config"`. Module 35 is backend-only: add Anchor `VerticalConfig` document to MongoDB. Zero Android changes.

**Option B — Resume AI features:**
Go back to `/opsx:propose "module-31-visual-asset-identification"`. Vertical engine is fully backward-compatible; Module 31 can proceed without the backend tasks being done.

---

## Key technical context

- **Stack:** Kotlin 2.3.21 · Compose · Room 2.8.4 (version **8**) · KSP 2.3.9 · AGP 9.2.1 · DataStore 1.1.1
- **Backend:** FastAPI + Motor + MongoDB Atlas, live at https://web-production-a9fb1.up.railway.app
- **Backend Python:** 3.13 · bcrypt direct (no passlib) · python-jose · httpx
- **Dev device:** Pixel 10 Pro XL (API 36)
- **Android Studio:** Panda 4 | 2025.3.4 Patch 1, Windows
- **Repos:** C:\dev\taplog (Android), C:\dev\taplog-api (backend)
- **Package:** ca.taplog.app
- **Room DB:** version **8**, `taplog_ember.db`
- **MongoDB:** farpost-dev Atlas M0 cluster, `taplog` database

## Critical workflow notes
- **File replacement in Android Studio:** select-all (Ctrl+A) → paste. Never Windows Explorer copy.
- **One file at a time** — confirm paste before receiving next file.
- **End of session:** update all md docs → generate handoff → `copy taplog-md-prime-directives.md .claude\AGENTS.md`
- **OpenSpec per module:** `/opsx:propose` → review → `/opsx:apply` → `/opsx:archive`
- **Room migration crashes during dev:** clear app storage (Settings → Apps → TapLog → Storage → Clear storage)
- **Resend email:** using `onboarding@resend.dev` as sender. Switch to `noreply@taplog.ca` once domain verified in Resend dashboard.
- **VerticalRegistry must be populated before any screen that calls `VerticalRegistry.get()`** — SplashScreen gates on `registryReady` StateFlow
- **`OFCAssetTypes.kt` is intentionally retained** — it is the cold-start fallback for Ember. Do not delete until backend vertical config is proven in production.
- **Room v8** — two separate migrations: 6→7 (vertical_configs table), 7→8 (organisations.licensedVerticals column)
- **`AssetDetailSource`** lives in `ca.taplog.app.data` — import from there, not `ui.ember`
- **`material-icons-extended`** required for `Icons.AutoMirrored.*` and `Icons.Default.*`
- **InspectorPreferences.kt** is physically in `ui/ember/` directory but declares `package ca.taplog.app.data` — don't move it
