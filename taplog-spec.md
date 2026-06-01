# TapLog — Project Spec
> Last updated: 2026-05-31

## Overview
Offline-first NFC/RFID asset inspection platform for regulated trades.
- **Domain:** taplog.ca
- **Tagline:** Tap. Log. Done.
- **Package:** ca.taplog.app
- **Android repo:** C:\dev\taplog
- **Backend repo:** C:\dev\taplog-api

## Android Stack
| Component | Version |
|---|---|
| Kotlin | 2.3.21 |
| Compose | latest stable |
| Room | 2.8.4 |
| KSP | 2.3.9 |
| AGP | 9.2.1 |
| DataStore | 1.1.1 |
| material-icons-extended | latest stable |
| room.generateKotlin | true |

**Dev device:** Pixel 10 Pro XL (API 36)
**Android Studio:** Panda 4 | 2025.3.4 Patch 1, Windows

## Backend Stack
| Component | Detail |
|---|---|
| Language | Python 3.13 |
| Framework | FastAPI 0.115.5 |
| Server | Uvicorn |
| Database driver | Motor 3.6.0 (async MongoDB) |
| Validation | Pydantic 2.10.3 + pydantic-settings 2.6.1 |
| HTTP client | httpx 0.28.1 |
| JWT | python-jose[cryptography] 3.3.0 |
| Password hashing | bcrypt 4.2.1 (direct — passlib removed, Python 3.13 incompatible) |
| Email | Resend (transactional email — verification codes; sender: onboarding@resend.dev until taplog.ca domain verified) |
| Deploy | Railway (auto-deploy from GitHub) |
| Database | MongoDB Atlas — farpost-dev cluster, `taplog` database |
| Config | pyproject.toml + requirements.txt (Railpack trigger) |

**Backend live URL:** https://web-production-a9fb1.up.railway.app
**GitHub:** github.com/rgsamways/taplog-api

---

## Android Project Structure
```
ca.taplog.app
├── data/
│   ├── EmberModels.kt       — Organisation, Site, Asset, Inspection, Deficiency, DeficiencyWithAsset,
│   │                          ScanEvent, TagEvent entities + all enums (incl. TapLogVertical, RetireReason)
│   ├── Converters.kt        — Room TypeConverters for all enums (incl. TapLogVertical)
│   ├── EmberDAO.kt          — OrganisationDao, SiteDao, AssetDao, InspectionDao, DeficiencyDao,
│   │                          ScanEventDao, TagEventDao
│   ├── AppDatabase.kt       — Room singleton, version 6, taplog_ember.db
│   │                          Migrations: 2→3, 3→4, 4→5, 5→6
│   ├── EmberRepository.kt   — Thin data access layer for ViewModels
│   ├── InspectorPreferences.kt — DataStore: authToken, refreshToken, inspectorId, deviceId ONLY
│   │                             + InspectorClaims data class + decodeJwtClaims()
│   │                             (lives in ui/ember/ directory but package ca.taplog.app.data)
│   ├── OFCAssetTypes.kt     — OFCCategory enum + OFCAssetType (code, label, description,
│   │                          intervalMonths, checklistItems — all 35 types populated vs CAN/ULC-S536:19)
│   ├── SyncModels.kt        — Wire-format request/response models + toSyncRequest() extensions
│   │                          Includes: OrganisationSyncRequest, SiteSyncRequest, AssetSyncRequest,
│   │                          TagEventSyncRequest, InspectionSyncRequest, DeficiencySyncRequest,
│   │                          ScanEventSyncRequest
│   ├── SyncResult.kt        — Sealed class: Success / Conflict / Failure
│   ├── RetrofitClient.kt    — Dual Retrofit clients: authApiService (no interceptor) +
│   │                          createSyncApiService(AuthInterceptor) (Bearer injected)
│   ├── TapLogApiService.kt  — Retrofit interface: all sync endpoints (no auth header — injected)
│   ├── AuthApiService.kt    — Retrofit interface: register, verify-email, login, register-device,
│   │                          refresh, resend-code + all request/response models
│   ├── AuthInterceptor.kt   — OkHttp interceptor: injects Bearer token, handles 401 → refresh →
│   │                          retry once → clearAuth()
│   ├── PdfReportGenerator.kt — android.graphics.pdf.PdfDocument canvas renderer (zero dependencies)
│   │                           Generates A4 single-page inspection report
│   ├── ReportRepository.kt  — generateAndGetUri(): generates PDF file, returns FileProvider URI
│   └── SyncRepository.kt    — Per-record and batch sync, 409 handling, no API key (Bearer only)
│                              Sync order: organisations → sites → assets → tag_events →
│                              inspections → deficiencies → scan_events
├── ui/
│   ├── auth/
│   │   ├── AuthViewModel.kt           — AuthState machine: Login, Registering, VerifyingEmail,
│   │   │                                LoggingIn, NewDevice, Authenticated
│   │   ├── RegistrationScreen.kt      — Name, email, cert number, password, confirm password
│   │   ├── EmailVerificationScreen.kt — 6-digit code entry + resend
│   │   ├── LoginScreen.kt             — Email + password + Create account link
│   │   └── NewDeviceScreen.kt         — New device detected, 6-digit code entry
│   ├── ember/
│   │   ├── EmberViewModel.kt          — ScanState machine, org/site/asset context, inspectorClaims
│   │   │                                (derived from JWT), shareReportEvent SharedFlow
│   │   ├── OrganisationSetupScreen.kt — First launch only — creates org record
│   │   ├── SiteListScreen.kt          — Dashboard — all sites with overdue counts
│   │   ├── SiteDetailScreen.kt        — Site info + asset list + stats chips
│   │   ├── SiteRegistrationScreen.kt  — Add new site with full client + contact fields
│   │   ├── AssetRegistrationScreen.kt — Register new NFC tag (OFC picker, auto nextInspectionDue)
│   │   ├── AssetDetailScreen.kt       — Asset info + inspection history + scan log +
│   │   │                                "Share Report" per inspection + "Replace Tag" button
│   │   ├── AssetListScreen.kt         — Browse all assets, overdue highlighting
│   │   ├── AssetTypePickerDialog.kt   — Two-step OFC picker: search + category chips + type list
│   │   ├── InspectionFormScreen.kt    — Checklist card + InspectorIdentityCard (read-only) +
│   │   │                                result picker + deficiency dialog
│   │   ├── OpenDeficienciesScreen.kt  — All unresolved deficiencies, mark resolved
│   │   └── SplashScreen.kt            — Fade in/out on launch
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
├── MainActivity.kt          — NFC foreground dispatch, auth gate (token check at startup),
│                              AuthFlow composable, EmberScanScreen, all state routing
├── TapLogApplication.kt     — App singleton: database, repository, inspectorPreferences,
│                              authInterceptor, syncApiService, syncRepository, reportRepository,
│                              authViewModelFactory. onCreate() calls scheduleSyncIfNeeded()
├── SyncWorker.kt            — WorkManager CoroutineWorker, calls syncAll(), retry on failure
└── ConnectivityReceiver.kt  — BroadcastReceiver, triggers scheduleSyncIfNeeded on restore
```

## Backend Project Structure
```
taplog-api/
├── app/
│   ├── __init__.py
│   ├── main.py          — FastAPI app, lifespan, all router registration, /health endpoint
│   ├── database.py      — Motor async client, Settings (jwt_secret, resend_api_key added)
│   ├── auth.py          — JWT creation/validation, bcrypt hashing (direct, no passlib),
│   │                      send_verification_email (Resend httpx; dev fallback: console print)
│   ├── dependencies.py  — get_current_inspector JWT dependency (OAuth2PasswordBearer)
│   ├── models.py        — All Pydantic models: sync entities, auth request/response, org/site
│   └── routers/
│       ├── __init__.py
│       ├── auth.py         — POST /api/v1/auth/register, verify-email, login,
│       │                     register-device, refresh, resend-code
│       ├── organisations.py — POST /api/v1/organisations (Bearer auth, upsert + inspector org link)
│       ├── sites.py        — POST /api/v1/sites (Bearer auth, upsert)
│       ├── assets.py       — POST /api/v1/assets (Bearer auth, upsert)
│       ├── inspections.py  — POST /api/v1/inspections (Bearer auth, asset existence guard)
│       ├── deficiencies.py — POST /api/v1/deficiencies (Bearer auth, inspection existence guard)
│       ├── scan_events.py  — POST /api/v1/scan_events (Bearer auth, upsert)
│       └── tag_events.py   — POST /api/v1/tag_events (Bearer auth, upsert)
├── .env                 — MONGODB_URL, DB_NAME=taplog, API_KEY, JWT_SECRET, RESEND_API_KEY
├── .gitignore
├── Procfile             — web: python -m uvicorn app.main:app --host 0.0.0.0 --port $PORT
├── pyproject.toml       — uv-compatible dependency spec, requires-python >=3.12
└── requirements.txt     — Railpack pip install trigger (same deps as pyproject.toml)
```

---

## Data Model

### Organisation
```kotlin
@Entity(tableName = "organisations")
data class Organisation(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val city: String? = null,
    val province: String = "ON",
    val subscriptionTier: SubscriptionTier = SubscriptionTier.SOLO,
    val subscriptionStatus: SubscriptionStatus = SubscriptionStatus.TRIAL,
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
```

### Site
```kotlin
@Entity(tableName = "sites", foreignKeys = [...], indices = [Index("organisationId")])
data class Site(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val organisationId: String,
    val name: String,
    val address: String,
    val city: String,
    val province: String = "ON",
    val postalCode: String? = null,
    val clientName: String? = null,
    val clientPhone: String? = null,
    val contactName: String? = null,
    val contactPhone: String? = null,
    val notes: String? = null,
    val isActive: Boolean = true,
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
```

### Asset
```kotlin
@Entity(tableName = "assets", foreignKeys = [...], indices = [Index("siteId")])
data class Asset(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val nfcTagId: String,
    val siteId: String,
    val name: String,
    val assetType: String,               // stores OFC code e.g. "EXT_ABC"
    val location: String,
    val installDate: Long,
    val lastInspectedAt: Long? = null,
    val nextInspectionDue: Long? = null,
    val isActive: Boolean = true,
    val isSynced: Boolean = false,
    val vertical: TapLogVertical = TapLogVertical.EMBER,  // MIGRATION_5_6
    val createdAt: Long = System.currentTimeMillis()
)
```

### Inspection
```kotlin
@Entity(tableName = "inspections", foreignKeys = [...], indices = [Index("assetId")])
data class Inspection(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val assetId: String,
    val inspectorName: String,           // from JWT claims (Module 28+)
    val inspectorCertNumber: String,     // from JWT claims (Module 28+)
    val inspectedAt: Long = System.currentTimeMillis(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val result: InspectionResult,
    val notes: String? = null,
    val isSynced: Boolean = false
)
```

### Deficiency
```kotlin
@Entity(tableName = "deficiencies", foreignKeys = [...], indices = [Index("inspectionId")])
data class Deficiency(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val inspectionId: String,
    val assetId: String,
    val code: String,
    val description: String,
    val severity: DeficiencySeverity,
    val photoPath: String? = null,       // populated in Module 30
    val resolvedAt: Long? = null,
    val isSynced: Boolean = false
)
```

### ScanEvent (Module 27)
```kotlin
@Entity(tableName = "scan_events", foreignKeys = [...], indices = [Index("assetId")])
data class ScanEvent(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val tagId: String,
    val assetId: String,                 // FK to Asset
    val inspectorId: String? = null,     // populated from JWT post Module 28
    val inspectorName: String,
    val scannedAt: Long = System.currentTimeMillis(),
    val eventType: ScanEventType,        // INSPECTION / BROWSE
    val isSynced: Boolean = false
)
```

### TagEvent (Module 27)
```kotlin
@Entity(tableName = "tag_events", foreignKeys = [...], indices = [Index("assetId")])
data class TagEvent(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val assetId: String,
    val tagId: String,
    val attachedAt: Long,
    val retiredAt: Long? = null,
    val retiredReason: RetireReason? = null,
    val retiredByInspectorId: String? = null,  // from JWT post Module 28
    val isSynced: Boolean = false
)
```

**Key design principle:** `Asset.nfcTagId` is a current pointer to the active tag — not permanent identity. The asset is the permanent record. Tags are replaceable pointers. All inspections and scan events attach to `assetId`, never `tagId` directly.

### OFCAssetType
```kotlin
data class OFCAssetType(
    val code: String,
    val label: String,
    val inspectionIntervalMonths: Int,
    val description: String,
    val checklistItems: List<String> = emptyList()
)
```

### InspectorClaims (not a Room entity — decoded from JWT)
```kotlin
data class InspectorClaims(
    val inspectorId: String,
    val name: String,
    val email: String,
    val certNumber: String,
    val organisationId: String
)
```

### Enums
```kotlin
enum class InspectionResult { PASS, FAIL, REQUIRES_ATTENTION }
enum class DeficiencySeverity { LOW, MEDIUM, HIGH, CRITICAL }
enum class AssetDetailSource { FROM_SCAN, FROM_LIST }
enum class SubscriptionTier { SOLO, TEAM, COMPANY }
enum class SubscriptionStatus { TRIAL, ACTIVE, PAST_DUE, CANCELLED }
enum class ScanEventType { INSPECTION, BROWSE }
enum class RetireReason { DAMAGED, LOST, REPLACED, REMOVED }
enum class TapLogVertical { EMBER, ANCHOR, HATCH, NEWEL, MAST, CRANE, SEAM, SPAN }
```

---

## Room Migration History
| Version | What changed |
|---|---|
| 1 | Initial schema: assets, inspections, deficiencies |
| 2 | (fallbackToDestructiveMigration era — pre-Module 27) |
| 3 | MIGRATION_2_3: organisations, sites added; assets rebuilt with siteId (buildingName removed) |
| 4 | MIGRATION_3_4: scan_events, tag_events tables added |
| 5 | MIGRATION_4_5: organisations.isSynced added (was missing from v3 SQL) |
| 6 | MIGRATION_5_6: assets.vertical TEXT NOT NULL DEFAULT 'EMBER' added |

---

## Authentication Model (Module 28)

### MongoDB Collections
```
inspectors
  _id, name, email, cert_number (unique index), password_hash, email_verified, organisation_id, created_at

devices
  _id, inspector_id, device_id, device_name, registered_at, last_seen_at

pending_verifications
  _id, email, code (6-digit), purpose (EMAIL_VERIFY / NEW_DEVICE), expires_at (TTL index, 15 min)
```

### JWT Payload
```json
{
  "inspectorId": "...",
  "email": "...",
  "name": "...",
  "certNumber": "...",
  "deviceId": "...",
  "organisationId": "...",
  "type": "access",
  "exp": ...
}
```

### Auth Endpoints
| Endpoint | Purpose |
|---|---|
| `POST /api/v1/auth/register` | Create account, send 6-digit email verification code |
| `POST /api/v1/auth/verify-email` | Submit code → account verified, device registered, JWT issued |
| `POST /api/v1/auth/login` | Email + password + deviceId → JWT or 403 NEW_DEVICE |
| `POST /api/v1/auth/register-device` | Submit code from new-device email → device registered, JWT issued |
| `POST /api/v1/auth/refresh` | Decode refresh token, issue new access token |
| `POST /api/v1/auth/resend-code` | Re-send verification code for EMAIL_VERIFY or NEW_DEVICE |

### Auth Flow Rules
- Cert number is unique at registration — backend rejects duplicate (409)
- Device ID: UUID generated once at app install, stored permanently in DataStore, never rotated
- New device on login → 403 + detail "NEW_DEVICE" → email code sent → `register-device` → JWT
- All sync endpoints require `Authorization: Bearer <token>`
- `AuthInterceptor` injects token on every request, handles 401 → refresh → retry once → clearAuth()
- No backward compatibility required — pilot user has no existing data
- `passlib` removed — direct `bcrypt` (Python 3.13 compat fix)
- Email sender: `onboarding@resend.dev` (temporary) → `noreply@taplog.ca` once domain verified in Resend

### Security Properties
- Knowing name + cert number is insufficient — requires password
- Knowing password is insufficient on unregistered device — requires email access
- Every inspection signed by verified inspector identity + device ID
- `inspectorId` populated on ScanEvent and TagEvent.retiredByInspectorId post Module 28

---

## ViewModel — ScanState
```kotlin
sealed class ScanState {
    object OrganisationSetup : ScanState()
    object SiteList : ScanState()
    data class SiteSelected(val site: Site) : ScanState()
    object AssetList : ScanState()
    object OpenDeficiencies : ScanState()
    object Idle : ScanState()                               // routes to SiteRegistrationScreen
    object Scanning : ScanState()
    data class AssetFound(val asset: Asset) : ScanState()
    data class Inspecting(val asset: Asset) : ScanState()
    data class AwaitingReplacementTag(val asset: Asset, val reason: RetireReason) : ScanState()
    object AssetNotFound : ScanState()
    data class Error(val message: String) : ScanState()
}
```

---

## Auth Gate — First Launch Flow (post Module 28)
1. Splash screen fades in/out
2. `LaunchedEffect` reads `inspectorPreferences.getAuthToken()` — suspending DataStore read
3. Token absent → `AuthFlow` composable → Login/Registration/Verification screens
4. Token present → `LaunchedEffect` calls `viewModel.loadOrganisation()`
5. `loadOrganisation()` checks organisationDao — null → `OrganisationSetup`, else → `SiteList`
6. `TapLogApplication.onCreate()` calls `scheduleSyncIfNeeded(this)` — sync triggered on every launch

---

## NFC Flow
1. Inspector selects a site → NFC tap available
2. NFC tap → `onNfcTagScanned(tagId)` → validates site context (error if no site selected)
3. DB lookup by `nfcTagId`
4. ScanEvent logged immediately (BROWSE) — insert-only
5. **Found** → `AssetFound` → AssetDetailScreen → Start Inspection → InspectionFormScreen
6. **Not found** → `AssetNotFound` → AssetRegistrationScreen → OFC picker → save → `AssetFound`
7. On inspection submit: second ScanEvent logged (INSPECTION)

---

## Sync Architecture

### Principles
- Device UUIDs assigned at creation time — never by the backend
- `isSynced` only flips to `true` after HTTP 200 confirmation
- Backend upserts on `_id` — re-sending any record is always safe
- Sync order: organisations → sites → assets → tag_events → inspections → deficiencies → scan_events
- 409 guards on backend: inspection rejected if asset not synced; deficiency rejected if inspection not synced
- All endpoints require Bearer token (injected by AuthInterceptor — no per-method header)
- `TapLogApplication.onCreate()` calls `scheduleSyncIfNeeded()` — ensures sync triggers on every app start
- `ExistingWorkPolicy.KEEP` prevents duplicate sync jobs

### Backend API
| Endpoint | Auth | Purpose |
|---|---|---|
| `POST /api/v1/organisations` | Bearer | Upsert org; links inspector's organisationId |
| `POST /api/v1/sites` | Bearer | Upsert site |
| `POST /api/v1/assets` | Bearer | Upsert asset |
| `POST /api/v1/tag_events` | Bearer | Upsert tag lifecycle events |
| `POST /api/v1/inspections` | Bearer | Upsert inspection, 409 if asset missing |
| `POST /api/v1/deficiencies` | Bearer | Upsert deficiency, 409 if inspection missing |
| `POST /api/v1/scan_events` | Bearer | Upsert scan event |

### MongoDB Structure
```
farpost-dev cluster (Atlas M0 free tier)
├── farpost      (Farpost collections — untouched)
└── taplog       (TapLog collections)
    ├── organisations
    ├── sites
    ├── assets
    ├── tag_events
    ├── inspections
    ├── deficiencies
    ├── scan_events
    ├── inspectors       (Module 28 — unique index on cert_number)
    ├── devices          (Module 28)
    └── pending_verifications  (Module 28 — TTL index on expires_at)
```

---

## PDF Report Architecture (Module 29)

### Generation
- `PdfReportGenerator.kt` — `android.graphics.pdf.PdfDocument` canvas renderer (API 19+, zero dependencies)
- A4 page (595×842 points), sections: header → site/client → asset → inspection → deficiencies → footer
- Deficiency list truncated at 10 with "…and N more" note
- PASS/FAIL/REQUIRES ATTENTION rendered with distinct colors
- File: `getExternalFilesDir(null)/TapLog/taplog_report_<inspectionId_prefix>.pdf`

### Sharing
- `ReportRepository.kt` — calls PdfReportGenerator, wraps file in FileProvider URI
- FileProvider authority: `ca.taplog.app.fileprovider`
- `res/xml/file_paths.xml` — external-files-path for `TapLog/` subdirectory
- `FileProvider` registered in `AndroidManifest.xml` (exported=false, grantUriPermissions=true)
- Share intent via `Intent.ACTION_SEND` → `Intent.createChooser` → Android share sheet
- `EmberViewModel.shareReportEvent: SharedFlow<Intent>` — `MainActivity` `LaunchedEffect` collects and starts chooser

---

## Key Implementation Notes

### Tag Lifecycle
- `Asset.nfcTagId` is a current pointer, not permanent identity
- `TagEvent` is insert-only — DAMAGED/LOST/REPLACED/REMOVED retire reasons
- Tag replacement is an atomic Room transaction: retire old TagEvent + update Asset.nfcTagId + insert new TagEvent
- `retiredByInspectorId` populated from `InspectorPreferences.inspectorId` (JWT claim)

### Inspector Identity (post Module 28)
- `InspectorPreferences.kt` stores: `authToken`, `refreshToken`, `inspectorId`, `deviceId` only
- `decodeJwtClaims(token)` — base64 decode JWT payload, returns `InspectorClaims`, no library needed
- `EmberViewModel.inspectorClaims: StateFlow<InspectorClaims?>` derived from authToken flow
- `InspectionFormScreen` shows `InspectorIdentityCard` (read-only) — no editable name/cert fields
- `inspectorId` populated on every ScanEvent and TagEvent retirement (post Module 28)

### Sync Trigger
- `TapLogApplication.onCreate()` calls `scheduleSyncIfNeeded(this)` — critical for fresh install
- `ConnectivityReceiver` fires on network change — also calls `scheduleSyncIfNeeded`
- `ExistingWorkPolicy.KEEP` — if `taplog_sync` work already queued, new requests are ignored
- To force sync in dev: Background Task Inspector in Android Studio → cancel + re-enqueue

### Vertical Architecture
- `TapLogVertical` enum: EMBER, ANCHOR, HATCH, NEWEL, MAST, CRANE, SEAM, SPAN
- `Asset.vertical` field added in MIGRATION_5_6 — default EMBER
- Future screens in `ui/anchor/`, `ui/hatch/`, etc. — same shared engine

### Back Navigation
- `BackHandler` added to all navigable screens — system back gesture mirrors in-app back button
- `AssetDetailSource` enum lives in `ca.taplog.app.data` (not `ui.ember`)

### Scroll / Keyboard
- All form screens: `verticalScroll(rememberScrollState())` + `imePadding()`
- `AndroidManifest.xml` activity: `windowSoftInputMode="adjustResize"`

### Development Workflow
- **File replacement in Android Studio:** select-all (Ctrl+A) → paste. Never Windows Explorer copy.
- **Room migration crashes during dev:** clear app storage — faster than debugging
- **`AssetDetailSource`** lives in `ca.taplog.app.data`
- **`material-icons-extended`** required for `Icons.AutoMirrored.*` and `Icons.Default.*`
- **Resend dev mode:** if `RESEND_API_KEY` is empty, verification codes print to console as `[DEV] code`
- **passlib is removed** — use `bcrypt` directly (Python 3.13 compat)

### Railway / Railpack
- `requirements.txt` must be present for Railpack pip install trigger
- Environment variables: `MONGODB_URL`, `DB_NAME`, `API_KEY`, `JWT_SECRET`, `RESEND_API_KEY`
- TTL index on `pending_verifications.expires_at` must be created manually in Atlas
- Unique index on `inspectors.cert_number` must be created manually in Atlas

---

## Roadmap

### Done ✅
- NFC reading (foreground dispatch)
- Room database — full entity hierarchy (Organisation, Site, Asset, Inspection, Deficiency)
- EmberRepository, EmberViewModel with full ScanState machine
- SplashScreen, OrganisationSetupScreen, SiteListScreen, SiteDetailScreen, SiteRegistrationScreen
- AssetRegistrationScreen, AssetDetailScreen, AssetListScreen
- InspectionFormScreen + AddDeficiencyDialog
- OpenDeficienciesScreen — mark resolved
- FastAPI backend — sync endpoints, live on Railway
- MongoDB Atlas — taplog database
- Retrofit HTTP client + SyncRepository + WorkManager SyncWorker
- End-to-end sync verified on device
- Android 16 NFC BAL fix
- OFC asset type picker — 6 categories, 35 types, search, plain-English descriptions
- Back navigation — source-aware, hierarchy-aware, BackHandler on all screens
- Room v2→v3 migration (organisations + sites + asset restructure)
- **Module 27** — ScanEvent (insert-only, every NFC tap), TagEvent (tag lifecycle), tag replacement workflow, checklistItems on all 35 OFC types, Room v3→v4, two new sync endpoints
- **Module 28** — Inspector accounts: registration, email verification (6-digit, Resend), device registration, JWT, AuthInterceptor (401 refresh retry), org/site sync to backend, auth gate in MainActivity, read-only inspector identity in InspectionFormScreen, Room v4→v5→v6, TapLogVertical enum
- **Module 29** — PDF inspection reports: PdfDocument (zero dependencies), FileProvider, share sheet, "Share Report" per inspection history card
- **Module 30** — Photo capture for deficiencies: CAMERA permission, `ActivityResultContracts.TakePicture()` in `AddDeficiencyDialog`, `BitmapFactory` thumbnails in `DeficiencyChip` and `DeficiencyWithAssetCard`, zero new dependencies

### Next
- [ ] **Module 31** — Visual Asset Identification: photo → AI suggests OFC asset type (depends on Module 30 ✅)
- [ ] **Module 31** — Visual Asset Identification — photo → AI suggests OFC asset type (depends on Module 30)
- [ ] **Module 32** — Inspection guidance Level 2 — contextual OFC guidance panel on inspection form
- [ ] **Module 33** — AI inspection co-pilot — OFC-aware, asset-context-aware, offline-cached Q&A
- [ ] **Billing** — Stripe web checkout, subscription gating on Organisation — before OAFC November 2026

---

## Vertical Roadmap
| Vertical | Domain | Status |
|---|---|---|
| Ember | Fire safety inspection | 🔨 Active build — v1 nearly complete |
| Anchor | Fall protection | Specced, next after Ember |
| Hatch | Confined space | Specced, tier 1 underserved |
| Mast | Scaffolding | Tier 2 |
| Crane | Crane/hoist | Tier 2 |
| Seam | Welding/pressure | Tier 2 |
| Newel | Home inspection | Held for pilot contact |
| Span | Bridges/infrastructure | Tier 3 |

---

## Key Files Reference

### Android
| File | Purpose |
|---|---|
| `ca.taplog.app/MainActivity.kt` | Entry point, NFC handling, auth gate, AuthFlow, EmberScanScreen, all state routing |
| `ca.taplog.app/TapLogApplication.kt` | App singleton — all DAOs, repos, interceptor, authViewModelFactory. onCreate() schedules sync |
| `ca.taplog.app/SyncWorker.kt` | WorkManager CoroutineWorker — calls syncAll(), handles retry/success |
| `ca.taplog.app/ConnectivityReceiver.kt` | BroadcastReceiver — triggers sync on connectivity restore |
| `ca.taplog.app.data/EmberModels.kt` | All entities + enums (incl. TapLogVertical, RetireReason, InspectorClaims is here too via InspectorPreferences.kt) |
| `ca.taplog.app.data/Converters.kt` | TypeConverters for all Room enums incl. TapLogVertical |
| `ca.taplog.app.data/EmberDAO.kt` | All DAOs + getByInspection on DeficiencyDao |
| `ca.taplog.app.data/AppDatabase.kt` | Room singleton, version 6, migrations 2→3→4→5→6 |
| `ca.taplog.app.data/EmberRepository.kt` | Data access layer — includes replaceTag() atomic transaction |
| `ca.taplog.app.data/InspectorPreferences.kt` | DataStore: auth fields + deviceId + decodeJwtClaims() + InspectorClaims (file in ui/ember/ dir, package ca.taplog.app.data) |
| `ca.taplog.app.data/OFCAssetTypes.kt` | 35 OFC asset types with checklistItems |
| `ca.taplog.app.data/SyncModels.kt` | Wire-format models for all 7 entity types + toSyncRequest() extensions |
| `ca.taplog.app.data/RetrofitClient.kt` | authApiService (no interceptor) + createSyncApiService(AuthInterceptor) |
| `ca.taplog.app.data/TapLogApiService.kt` | Retrofit interface — 7 sync endpoints, no auth header (injected) |
| `ca.taplog.app.data/AuthApiService.kt` | Retrofit interface — 6 auth endpoints + all request/response models |
| `ca.taplog.app.data/AuthInterceptor.kt` | Bearer injection, 401 → refresh → retry → clearAuth |
| `ca.taplog.app.data/PdfReportGenerator.kt` | PdfDocument canvas renderer — A4 inspection report |
| `ca.taplog.app.data/ReportRepository.kt` | generateAndGetUri() — PDF file → FileProvider content URI |
| `ca.taplog.app.data/SyncRepository.kt` | Batch sync, updated order, Bearer auth, org/site included |
| `ca.taplog.app.ui.auth/AuthViewModel.kt` | AuthState machine + register/login/verify/registerDevice/resend |
| `ca.taplog.app.ui.auth/LoginScreen.kt` | Email + password |
| `ca.taplog.app.ui.auth/RegistrationScreen.kt` | Name, email, cert, password |
| `ca.taplog.app.ui.auth/EmailVerificationScreen.kt` | 6-digit code + resend |
| `ca.taplog.app.ui.auth/NewDeviceScreen.kt` | New device detected, 6-digit code |
| `ca.taplog.app.ui.ember/EmberViewModel.kt` | ScanState, inspectorClaims, shareReport, shareReportEvent |
| `ca.taplog.app.ui.ember/InspectionFormScreen.kt` | Pre-inspection checklist + InspectorIdentityCard + form |
| `ca.taplog.app.ui.ember/AssetDetailScreen.kt` | Scan history + Replace Tag + Share Report per inspection |
| `app/src/main/AndroidManifest.xml` | NFC, windowSoftInputMode, ConnectivityReceiver, FileProvider |
| `app/src/main/res/xml/file_paths.xml` | FileProvider path config for TapLog/ external dir |

### Backend
| File | Purpose |
|---|---|
| `app/main.py` | FastAPI app, all routers registered |
| `app/database.py` | Motor client, Settings incl. jwt_secret + resend_api_key |
| `app/auth.py` | JWT, bcrypt (direct), send_verification_email |
| `app/dependencies.py` | get_current_inspector Bearer dependency |
| `app/models.py` | All Pydantic models (sync entities + auth + org/site) |
| `app/routers/auth.py` | All 6 auth endpoints |
| `app/routers/organisations.py` | POST /api/v1/organisations |
| `app/routers/sites.py` | POST /api/v1/sites |
| `app/routers/assets.py` | POST /api/v1/assets |
| `app/routers/inspections.py` | POST /api/v1/inspections |
| `app/routers/deficiencies.py` | POST /api/v1/deficiencies |
| `app/routers/scan_events.py` | POST /api/v1/scan_events |
| `app/routers/tag_events.py` | POST /api/v1/tag_events |
| `requirements.txt` | Railpack pip install trigger |

### Project docs & tooling
| File | Purpose |
|---|---|
| `taplog-spec.md` | Living technical spec |
| `taplog-curriculum.md` | Full build log — every module documented |
| `taplog-investor-summary.md` | Pitch document |
| `taplog-lightbulbs.md` | Product insight moments |
| `taplog-pilot-tester-pool.md` | Prospective pilot users |
| `taplog-md-prime-directives.md` | Session workflow rules — source of truth for AGENTS.md |
| `.claude/AGENTS.md` | Auto-read by Claude Code — copy of prime directives |
| `openspec/config.yaml` | OpenSpec configuration |

---

## Key People
- **Ember pilot contact** — details in taplog-pilot-tester-pool.md
- **Newel pilot contact** — Geoffrey Gordon, Citadel Home Inspections, Bancroft ON
