# TapLog — Session Handoff
> Session date: 2026-05-31
> Prepared for: next Claude session

---

## How to start the next session

1. Upload all md docs: `taplog-spec.md`, `taplog-curriculum.md`, `taplog-investor-summary.md`, `taplog-lightbulbs.md`, `taplog-md-prime-directives.md`, `taplog-pilot-tester-pool.md`, `taplog-session-handoff.md`
2. Read `taplog-md-prime-directives.md` first and confirm you've read it
3. Next module is **Module 31 — Visual Asset Identification**
4. Start with `/opsx:propose "module-31-visual-asset-identification"` in Claude Code

---

## What was accomplished this session

Session 9 was the most productive build session to date. Three modules archived in a single session.

### Module 27 — Tamper-evident scan log + tag lifecycle (COMPLETE, verified on device)

**What was built:**
- `ScanEvent` entity — insert-only, every NFC tap logged (BROWSE or INSPECTION), assetId FK, inspectorId (nullable until Module 28), isSynced
- `TagEvent` entity — insert-only, tag attach/retire lifecycle, retiredReason enum, retiredByInspectorId (nullable until Module 28), isSynced
- Room version 3 → 4 (`MIGRATION_3_4` — creates scan_events and tag_events tables)
- `ScanEventDao`, `TagEventDao` — insert + query only, no update/delete methods
- Tag replacement workflow — `ReplaceTagDialog` on AssetDetailScreen, `AwaitingReplacementTag` ScanState, atomic `withTransaction`
- `checklistItems: List<String>` added to `OFCAssetType` — all 35 types populated against CAN/ULC-S536:19
- Pre-inspection checklist card on `InspectionFormScreen` — displayed before form, fully offline
- Collapsible scan history section on `AssetDetailScreen`
- Two new backend endpoints: POST /api/v1/scan_events, POST /api/v1/tag_events
- Sync order: assets → tag_events → inspections → deficiencies → scan_events
- Back navigation audit: `BackHandler` on all screens, `imePadding()` fixed on OrganisationSetupScreen + SiteRegistrationScreen
- MIGRATION_2_3 retroactively patched to include `isSynced` on all three tables (was missing)

### Module 28 — Authenticated inspector identity (COMPLETE, verified on device)

**What was built:**

*Backend:*
- `app/auth.py` — JWT (python-jose), password hashing via direct bcrypt (passlib removed — Python 3.13 incompatible with bcrypt 4.x), Resend email dispatch with dev console fallback
- `app/routers/auth.py` — 6 endpoints: register, verify-email, login, register-device, refresh, resend-code
- `app/routers/organisations.py` + `sites.py` — Bearer-auth upsert endpoints; organisations router back-links inspector's organisationId on first org sync
- `app/dependencies.py` — `get_current_inspector` JWT Bearer dependency
- All 5 existing sync routers swapped from x-api-key to Depends(get_current_inspector)
- New MongoDB collections: `inspectors` (unique index on cert_number), `devices`, `pending_verifications` (TTL index on expires_at)
- JWT_SECRET and RESEND_API_KEY added to Railway env vars

*Android:*
- `InspectorPreferences.kt` stripped to: authToken, refreshToken, inspectorId, deviceId. Added `decodeJwtClaims()` (base64, no library), `ensureDeviceId()`, `InspectorClaims` data class
- `AuthApiService.kt` — 6 auth endpoints + request/response models
- `AuthInterceptor.kt` — runBlocking token read, Bearer injection, 401 → refresh → retry → clearAuth
- `RetrofitClient.kt` — dual clients: authApiService (no interceptor) + createSyncApiService(AuthInterceptor)
- `ui/auth/` package — AuthViewModel + LoginScreen + RegistrationScreen + EmailVerificationScreen + NewDeviceScreen
- `InspectionFormScreen` — replaced editable name/cert fields with read-only `InspectorIdentityCard` from JWT claims
- `EmberViewModel` — `inspectorClaims: StateFlow<InspectorClaims?>` from JWT, `inspectorId` on ScanEvent/TagEvent
- `MainActivity` — auth gate at startup, `AuthFlow` composable
- `SyncRepository` — org/site sync added, sync order: organisations → sites → assets → tag_events → inspections → deficiencies → scan_events
- `TapLogApplication.onCreate()` — calls `scheduleSyncIfNeeded(this)` — was missing, causing sync to never fire
- `TapLogVertical` enum added to EmberModels: EMBER, ANCHOR, HATCH, NEWEL, MAST, CRANE, SEAM, SPAN
- `Asset.vertical: TapLogVertical = TapLogVertical.EMBER` field added — Room version 5 → 6, MIGRATION_5_6
- Room version 4 → 5 (MIGRATION_4_5: organisations.isSynced)

**Bugs fixed during session:**
- passlib incompatible with Python 3.13 + bcrypt 4.x — fixed by removing passlib, using bcrypt directly
- Resend silent failure — RESEND_API_KEY set but taplog.ca domain unverified → using onboarding@resend.dev temporarily; added Resend error logging
- Sync never firing — TapLogApplication had no onCreate(), sync trigger was missing post-auth rewrite

### Module 29 — PDF inspection reports (COMPLETE, verified on device)

**What was built:**
- `PdfReportGenerator.kt` — PdfDocument canvas renderer, A4, zero dependencies; sections: org/site/client → asset (OFC label) → inspection (coloured result) → deficiencies (numbered, severity-coloured, max 10 with "…and N more") → next due date
- `ReportRepository.kt` — generates PDF to getExternalFilesDir()/TapLog/, returns FileProvider content URI
- `res/xml/file_paths.xml` — FileProvider path config
- `AndroidManifest.xml` — FileProvider registered at ca.taplog.app.fileprovider
- `DeficiencyDao.getByInspection()` — new query
- `EmberViewModel.shareReport()` + `shareReportEvent: SharedFlow<Intent>`
- "Share Report" TextButton on each InspectionHistoryCard
- `MainActivity` LaunchedEffect collecting shareReportEvent → Intent.createChooser

---

## Current state of the app

**Everything working on device (Pixel 10 Pro XL, API 36):**
- Full v1 loop verified: register → email code → verify → org setup → site creation → NFC scan → asset registration → inspection → PDF report → share sheet
- Auth gate at startup — token present skips auth flow
- ScanEvent logged on every NFC tap with inspectorId from JWT
- TagEvent on asset registration; tag replacement workflow functional
- Pre-inspection checklists on all 35 OFC types
- PDF reports generated on-device, shared via Android share sheet
- Full sync pipeline: organisations → sites → assets → tag_events → inspections → deficiencies → scan_events
- MongoDB Atlas receiving all entities with Bearer auth

**Not yet built:**
- Visual Asset Identification (Module 31) — photo → AI suggests OFC asset type; depends on Module 30 ✅
- Inspection guidance Level 2 (Module 32)
- AI co-pilot (Module 33)
- Billing (before OAFC November 2026)

---

## Next module

**Module 31 — Visual Asset Identification**

Inspector photographs an asset; AI suggests the OFC asset type. Depends on Module 30 photo capture infrastructure (complete).

Key design decisions to work through at propose time:
- Which AI API: Claude claude-haiku-4-5 (fast, cheap, multimodal) via Anthropic API
- Input: JPEG photo from camera + prompt asking for OFC type classification
- Output: ranked list of OFC type suggestions (code + label + confidence)
- Where it appears: `AssetRegistrationScreen` — after photo is taken, suggestions appear below the asset type picker
- Offline behaviour: photo saved locally; AI call is online-only; graceful fallback to manual picker if offline
- `BuildConfig.ANTHROPIC_API_KEY` sourced from `local.properties`

Start with: `/opsx:propose "module-31-visual-asset-identification"` in Claude Code

---

## Key technical context

- **Stack:** Kotlin 2.3.21 · Compose · Room 2.8.4 (version 6) · KSP 2.3.9 · AGP 9.2.1 · DataStore 1.1.1
- **Backend:** FastAPI + Motor + MongoDB Atlas, live at https://web-production-a9fb1.up.railway.app
- **Backend Python:** 3.13 · bcrypt direct (no passlib) · python-jose · httpx
- **Dev device:** Pixel 10 Pro XL (API 36)
- **Android Studio:** Panda 4 | 2025.3.4 Patch 1, Windows
- **Repos:** C:\dev\taplog (Android), C:\dev\taplog-api (backend)
- **Package:** ca.taplog.app
- **Room DB:** version 6, `taplog_ember.db`
- **MongoDB:** farpost-dev Atlas M0 cluster, `taplog` database

## Critical workflow notes
- **File replacement in Android Studio:** select-all (Ctrl+A) → paste. Never Windows Explorer copy.
- **One file at a time** — confirm paste before receiving next file.
- **End of session:** update all md docs → generate handoff → `copy taplog-md-prime-directives.md .claude\AGENTS.md`
- **OpenSpec per module:** `/opsx:propose` → review → `/opsx:apply` → `/opsx:archive`
- **Room migration crashes during dev:** clear app storage (Settings → Apps → TapLog → Storage → Clear storage)
- **Resend email:** using `onboarding@resend.dev` as sender. Verification codes arrive in real email. Switch to `noreply@taplog.ca` once domain is verified in Resend dashboard.
- **Sync not firing?** Check Background Task Inspector in Android Studio. `ExistingWorkPolicy.KEEP` means airplane mode trick only works if no work is currently enqueued.
- **`AssetDetailSource`** lives in `ca.taplog.app.data` — import from there, not `ui.ember`
- **`material-icons-extended`** required for `Icons.AutoMirrored.*` and `Icons.Default.*`
- **InspectorPreferences.kt** is physically in `ui/ember/` directory but declares `package ca.taplog.app.data` — don't move it, just import from the data package
