# TapLog — Project Spec
> Last updated: 2026-06-01

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
│   ├── EmberModels.kt       — Organisation (+ licensedVerticals), Site, Asset, Inspection, Deficiency,
│   │                          ScanEvent, TagEvent entities + all enums (incl. TapLogVertical, RetireReason)
│   ├── VerticalModels.kt    — TriggerModel, RoleModel, FieldType, ResultAction, ResultOption,
│   │                          TriggerConfig, FormField, InspectionFormProfile, VerticalAssetType,
│   │                          VerticalConfig, VerticalRegistry singleton
│   ├── Converters.kt        — Room TypeConverters for all enums + List<String> + VerticalConfig (Gson)
│   ├── EmberDAO.kt          — OrganisationDao, SiteDao, AssetDao, InspectionDao, DeficiencyDao,
│   │                          ScanEventDao, TagEventDao
│   ├── VerticalConfigDao.kt — VerticalConfigEntity (Room entity) + VerticalConfigDao (upsert + getAll)
│   ├── AppDatabase.kt       — Room singleton, version 8, taplog_ember.db
│   │                          Migrations: 2→3, 3→4, 4→5, 5→6, 6→7, 7→8
│   ├── EmberRepository.kt   — Thin data access layer for ViewModels
│   ├── EmberVerticalConfig.kt — EmberVerticalConfig.build() — translates OFCCategory to VerticalConfig;
│   │                            static cold-start fallback; NOT deleted until backend cache proven
│   ├── InspectorPreferences.kt — DataStore: authToken, refreshToken, inspectorId, deviceId ONLY
│   │                             + InspectorClaims data class + decodeJwtClaims()
│   │                             (lives in ui/ember/ directory but package ca.taplog.app.data)
│   ├── OFCAssetTypes.kt     — OFCCategory enum + OFCAssetType (35 types, checklistItems populated)
│   │                          RETAINED as static fallback — do not delete until VerticalConfig cache proven
│   ├── SyncModels.kt        — Wire-format request/response models + toSyncRequest() extensions
│   │                          OrganisationSyncRequest now includes licensedVerticals
│   ├── SyncResult.kt        — Sealed class: Success / Conflict / Failure
│   ├── RetrofitClient.kt    — Dual Retrofit clients: authApiService (no interceptor) +
│   │                          createSyncApiService(AuthInterceptor) (Bearer injected)
│   ├── TapLogApiService.kt  — Retrofit interface: getVerticals(), getVertical(code) + all sync endpoints
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
│   │   ├── InspectionFormScreen.kt    — Field-driven via VerticalConfig: checklist card +
│   │   │                                InspectorIdentityCard + ResultOption result selector +
│   │   │                                FormField loop + conditional deficiencies section
│   │   │                                NO OFCCategory/OFCAssetType imports
│   │   ├── EntryEventScreen.kt        — Stub: "Multi-role entry form — coming soon" + back button
│   │   │                                Shown for MULTI_ROLE vertical assets (Hatch prep)
│   │   ├── OpenDeficienciesScreen.kt  — All unresolved deficiencies, mark resolved
│   │   └── SplashScreen.kt            — Accepts registryReady: Boolean — tap disabled until
│   │                                    VerticalRegistry is populated
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
├── MainActivity.kt          — NFC foreground dispatch, auth gate, AuthFlow composable,
│                              EmberScanScreen, Inspecting state routes MULTI_ROLE → EntryEventScreen
│                              or SINGLE_INSPECTOR → InspectionFormScreen
├── TapLogApplication.kt     — App singleton: database, repository, inspectorPreferences,
│                              authInterceptor, syncApiService, syncRepository, reportRepository,
│                              authViewModelFactory. onCreate(): scheduleSyncIfNeeded() +
│                              initVerticalRegistry() (fetch → cache → static fallback chain)
│                              verticalRegistryReady: StateFlow<Boolean> exposed for SplashScreen
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
│       ├── tag_events.py   — POST /api/v1/tag_events (Bearer auth, upsert)
│       └── verticals.py    — GET /api/v1/verticals, GET /api/v1/verticals/{code}
│                             (PENDING — Module 34 backend tasks 9.1–9.4)
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
    val licensedVerticals: List<String> = listOf("EMBER"),  // MIGRATION_7_8
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
```

### Site, Asset, Inspection, Deficiency, ScanEvent, TagEvent
*(unchanged from previous spec — see below)*

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
    val vertical: TapLogVertical = TapLogVertical.EMBER,
    val createdAt: Long = System.currentTimeMillis()
)
```

### VerticalConfigEntity (Room cache — Module 34)
```kotlin
@Entity(tableName = "vertical_configs")
data class VerticalConfigEntity(
    @PrimaryKey val verticalCode: String,
    val configJson: String               // full VerticalConfig serialized as JSON
)
```

### VerticalConfig (Module 34)
```kotlin
data class VerticalConfig(
    val vertical: TapLogVertical,
    val displayName: String,
    val shortName: String,
    val regulatoryFramework: String,
    val triggerModel: TriggerModel,
    val roleModel: RoleModel,
    val formProfile: InspectionFormProfile,
    val assetTypeRegistry: List<VerticalAssetType>
)

data class InspectionFormProfile(
    val resultOptions: List<ResultOption>,  // was List<String> — now carries ResultAction
    val fields: List<FormField>,
    val requiresPermit: Boolean = false,
    val requiresWitness: Boolean = false,
    val deficienciesEnabled: Boolean = true,
    val photoRequired: Boolean = false
)

data class ResultOption(
    val code: String,       // e.g. "PASS", "REMOVE_FROM_SERVICE"
    val label: String,      // display label e.g. "Pass", "Remove from service"
    val action: ResultAction
)

data class VerticalAssetType(
    val code: String,
    val label: String,
    val description: String,
    val triggerConfig: TriggerConfig,   // replaces intervalMonths: Int?
    val checklistItems: List<String>,
    val defaultFormFields: List<String> = emptyList()
)

data class TriggerConfig(
    val type: TriggerModel,
    val intervalMonths: Int? = null,
    val intervalMiles: Int? = null,
    val intervalHours: Int? = null
)

data class FormField(
    val key: String,
    val label: String,
    val type: FieldType,
    val required: Boolean,
    val applicableAssetTypes: List<String> = emptyList()
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
enum class TriggerModel { CALENDAR, PRE_USE, ENTRY_EVENT, ON_DEMAND, MILEAGE, ENGINE_HOURS }
enum class RoleModel { SINGLE_INSPECTOR, MULTI_ROLE }
enum class FieldType { TEXT, NUMBER, BOOLEAN, SINGLE_SELECT, MULTI_SELECT, DATE, PHOTO }
enum class ResultAction { NONE, REMOVE_FROM_SERVICE, NOTIFY_AUTHORITY, ISSUE_CERTIFICATE, DELIVER_REPORT }
```

**Key design principle:** `Asset.nfcTagId` is a current pointer to the active tag — not permanent identity. The asset is the permanent record. Tags are replaceable pointers. All inspections and scan events attach to `assetId`, never `tagId` directly.

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
| 7 | MIGRATION_6_7: vertical_configs table added (verticalCode PK, configJson TEXT) |
| 8 | MIGRATION_7_8: organisations.licensedVerticals TEXT NOT NULL DEFAULT '["EMBER"]' added |

---

## Vertical Engine Architecture (Module 34)

### VerticalRegistry
- Singleton object in `VerticalModels.kt`
- `register(config: VerticalConfig)` — called at startup
- `get(vertical: TapLogVertical): VerticalConfig` — throws `IllegalStateException` if vertical not registered
- `all(): List<VerticalConfig>`, `isRegistered()`, `count()`

### Startup fallback chain (`TapLogApplication.initVerticalRegistry()`)
1. Fetch `GET /api/v1/verticals` → upsert to Room → register each config
2. If fetch fails: load from `VerticalConfigDao.getAll()` → register from cache
3. If cache empty: `VerticalRegistry.register(EmberVerticalConfig.build())` — static Kotlin fallback
4. Set `_verticalRegistryReady.value = true` — unblocks SplashScreen

### InspectionFormScreen routing
- `SINGLE_INSPECTOR` roleModel → `InspectionFormScreen` (field-driven, no OFC imports)
- `MULTI_ROLE` roleModel → `EntryEventScreen` (stub in this module)

### Ember VerticalConfig (`EmberVerticalConfig.build()`)
- `resultOptions`: PASS / REQUIRES_ATTENTION / FAIL, all with `ResultAction.NONE`
- `fields`: single TEXT field (key="notes", label="Notes", required=false)
- `deficienciesEnabled = true`, `photoRequired = false`
- `assetTypeRegistry`: all OFCCategory types mapped to VerticalAssetType with `TriggerConfig(CALENDAR, intervalMonths)`

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

---

## ViewModel — ScanState
```kotlin
sealed class ScanState {
    object OrganisationSetup : ScanState()
    object SiteList : ScanState()
    data class SiteSelected(val site: Site) : ScanState()
    object AssetList : ScanState()
    object OpenDeficiencies : ScanState()
    object Idle : ScanState()
    object Scanning : ScanState()
    data class AssetFound(val asset: Asset) : ScanState()
    data class Inspecting(val asset: Asset) : ScanState()  // routed by roleModel
    data class AwaitingReplacementTag(val asset: Asset, val reason: RetireReason) : ScanState()
    object AssetNotFound : ScanState()
    data class Error(val message: String) : ScanState()
}
```

---

## Sync Architecture

### Sync order
organisations → sites → assets → tag_events → inspections → deficiencies → scan_events

### Backend API
| Endpoint | Auth | Purpose |
|---|---|---|
| `POST /api/v1/organisations` | Bearer | Upsert org (incl. licensedVerticals); links inspector's organisationId |
| `POST /api/v1/sites` | Bearer | Upsert site |
| `POST /api/v1/assets` | Bearer | Upsert asset |
| `POST /api/v1/tag_events` | Bearer | Upsert tag lifecycle events |
| `POST /api/v1/inspections` | Bearer | Upsert inspection, 409 if asset missing |
| `POST /api/v1/deficiencies` | Bearer | Upsert deficiency, 409 if inspection missing |
| `POST /api/v1/scan_events` | Bearer | Upsert scan event |
| `GET /api/v1/verticals` | Bearer | Returns VerticalConfig list for org's licensedVerticals (PENDING) |
| `GET /api/v1/verticals/{code}` | Bearer | Returns single VerticalConfig or 404 (PENDING) |

### MongoDB Structure
```
farpost-dev cluster (Atlas M0 free tier)
└── taplog
    ├── organisations
    ├── sites
    ├── assets
    ├── tag_events
    ├── inspections
    ├── deficiencies
    ├── scan_events
    ├── inspectors       (unique index on cert_number)
    ├── devices
    ├── pending_verifications  (TTL index on expires_at)
    └── verticals        (PENDING — Ember seed document + future vertical configs)
```

---

## OpenSpec State

### Archived modules
| Archive | Date |
|---|---|
| module-27-scan-log-tag-lifecycle | 2026-05-31 |
| module-28-authenticated-identity | 2026-05-31 |
| module-29-pdf-reports | 2026-05-31 |
| module-30-photo-capture | 2026-05-31 |
| module-34-vertical-engine | 2026-06-01 |

### Active specs (`openspec/specs/`)
- `ofc-checklists` — checklist source updated to VerticalConfig.assetTypeRegistry
- `org-site-sync` — licensedVerticals added to Organisation + sync payload
- `vertical-config` — VerticalRegistry, Room cache, backend endpoints
- `vertical-form-engine` — field-driven InspectionFormScreen, EntryEventScreen stub
- `inspector-auth`, `scan-log`, `tag-lifecycle`, `inspection-report`, `deficiency-photo-capture`

### Open design docs (`openspec/design-docs/`)
- `inspection-cardinality.md` — Status: Open. Blocks Fleet, Hatch multi-asset sessions, Anchor batch pre-use. One NFC tap = one asset = one form is wrong for these verticals.

---

## Roadmap

### Done ✅
- NFC reading (foreground dispatch)
- Room database — full entity hierarchy
- Ember vertical — full inspection flow, org/site hierarchy
- FastAPI backend — all sync endpoints, live on Railway
- Module 27 — ScanEvent (insert-only), TagEvent (lifecycle), checklistItems on all 35 OFC types, Room v3→v4
- Module 28 — Inspector accounts: registration, email verification, JWT, AuthInterceptor, org/site sync, auth gate, Room v4→v5→v6
- Module 29 — PDF inspection reports (PdfDocument, FileProvider, share sheet)
- Module 30 — Photo capture for deficiencies (CAMERA permission, TakePicture, thumbnails)
- Module 34 — Vertical engine: VerticalConfig/VerticalRegistry, field-driven InspectionFormScreen, Room v6→v7→v8, EntryEventScreen stub, licensedVerticals on Organisation

### Pending backend (taplog-api)
- Module 34 backend tasks 9.1–9.4: Organisation.licensed_verticals, verticals MongoDB collection (Ember seed), GET /api/v1/verticals, GET /api/v1/verticals/{code}

### Next
- [ ] **Module 31** — Visual Asset Identification: photo → AI suggests OFC asset type
- [ ] **Module 32** — Inspection guidance Level 2: contextual OFC guidance panel
- [ ] **Module 33** — AI inspection co-pilot: OFC-aware, asset-context-aware Q&A
- [ ] **Module 35** — Anchor config: add Anchor VerticalConfig to MongoDB (zero Android changes)
- [ ] **Module 36** — Hatch config + EntryEventScreen implementation
- [ ] **Billing** — Stripe web checkout, subscription gating — before OAFC November 2026

---

## Vertical Roadmap
| Vertical | Domain | Status |
|---|---|---|
| Ember | Fire safety inspection | ✅ Active — v1 complete, vertical engine live |
| Anchor | Fall protection | Module 35 — backend config only |
| Hatch | Confined space | Module 36 — EntryEventScreen + config |
| Lift | Elevators / escalators | Tier 1 |
| Newel | Home inspection | Held for pilot contact |
| Apex | HVAC | Tier 2 |
| Vault | Electrical | Tier 2 |
| Crane | Crane and hoist | Tier 2 (inspection cardinality blocker) |
| Fleet | Fleet vehicles | Tier 2 (inspection cardinality blocker) |
| Span | Bridges / structures | Tier 3 |

---

## Key Files Reference

### Android
| File | Purpose |
|---|---|
| `ca.taplog.app/MainActivity.kt` | Entry point, NFC, auth gate, Inspecting state routes by roleModel |
| `ca.taplog.app/TapLogApplication.kt` | App singleton, initVerticalRegistry(), verticalRegistryReady StateFlow |
| `ca.taplog.app/SyncWorker.kt` | WorkManager CoroutineWorker — syncAll() |
| `ca.taplog.app/ConnectivityReceiver.kt` | BroadcastReceiver — sync on connectivity restore |
| `ca.taplog.app.data/EmberModels.kt` | All entities + enums incl. Organisation.licensedVerticals |
| `ca.taplog.app.data/VerticalModels.kt` | Full vertical data model + VerticalRegistry singleton |
| `ca.taplog.app.data/Converters.kt` | TypeConverters: enums + List<String> + VerticalConfig (Gson) |
| `ca.taplog.app.data/EmberDAO.kt` | All DAOs |
| `ca.taplog.app.data/VerticalConfigDao.kt` | VerticalConfigEntity + VerticalConfigDao (upsert/getAll) |
| `ca.taplog.app.data/AppDatabase.kt` | Room singleton, version 8, migrations 2→3→4→5→6→7→8 |
| `ca.taplog.app.data/EmberRepository.kt` | Data access layer incl. replaceTag() atomic transaction |
| `ca.taplog.app.data/EmberVerticalConfig.kt` | EmberVerticalConfig.build() — static Ember VerticalConfig fallback |
| `ca.taplog.app.data/InspectorPreferences.kt` | DataStore: auth + deviceId + decodeJwtClaims() (in ui/ember/ dir, package ca.taplog.app.data) |
| `ca.taplog.app.data/OFCAssetTypes.kt` | 35 OFC asset types — retained as static fallback |
| `ca.taplog.app.data/SyncModels.kt` | Wire-format models + toSyncRequest() — OrganisationSyncRequest includes licensedVerticals |
| `ca.taplog.app.data/RetrofitClient.kt` | Dual Retrofit clients |
| `ca.taplog.app.data/TapLogApiService.kt` | Retrofit interface — verticals GETs + sync POSTs |
| `ca.taplog.app.data/AuthApiService.kt` | Auth endpoints |
| `ca.taplog.app.data/AuthInterceptor.kt` | Bearer injection, 401 refresh retry |
| `ca.taplog.app.data/PdfReportGenerator.kt` | PdfDocument canvas renderer |
| `ca.taplog.app.data/ReportRepository.kt` | generateAndGetUri() → FileProvider URI |
| `ca.taplog.app.data/SyncRepository.kt` | Batch sync, Bearer auth, full sync order |
| `ca.taplog.app.ui.auth/` | AuthViewModel + 4 auth screens |
| `ca.taplog.app.ui.ember/EmberViewModel.kt` | ScanState, inspectorClaims, shareReport |
| `ca.taplog.app.ui.ember/InspectionFormScreen.kt` | Field-driven: ResultOption selector + FormField loop — zero OFC imports |
| `ca.taplog.app.ui.ember/EntryEventScreen.kt` | MULTI_ROLE stub — "Multi-role entry form — coming soon" |
| `ca.taplog.app.ui.ember/SplashScreen.kt` | Accepts registryReady: Boolean |
| `ca.taplog.app.ui.ember/AssetDetailScreen.kt` | Scan history + Replace Tag + Share Report |
| `app/src/main/AndroidManifest.xml` | NFC, windowSoftInputMode, ConnectivityReceiver, FileProvider |
| `app/src/main/res/xml/file_paths.xml` | FileProvider path config |

### Backend
| File | Purpose |
|---|---|
| `app/main.py` | FastAPI app, all routers |
| `app/database.py` | Motor client, Settings |
| `app/auth.py` | JWT, bcrypt (direct), send_verification_email |
| `app/dependencies.py` | get_current_inspector Bearer dependency |
| `app/models.py` | All Pydantic models |
| `app/routers/auth.py` | 6 auth endpoints |
| `app/routers/organisations.py` | POST /api/v1/organisations |
| `app/routers/verticals.py` | GET /api/v1/verticals, GET /api/v1/verticals/{code} (PENDING) |
| `requirements.txt` | Railpack pip install trigger |

### Project docs & tooling
| File | Purpose |
|---|---|
| `taplog-spec.md` | Living technical spec |
| `taplog-curriculum.md` | Full build log — every module documented |
| `taplog-investor-summary.md` | Pitch document |
| `taplog-lightbulbs.md` | Product insight moments |
| `taplog-pilot-tester-pool.md` | Prospective pilot users |
| `taplog-md-prime-directives.md` | Session workflow rules |
| `.claude/AGENTS.md` | Auto-read by Claude Code — copy of prime directives |
| `openspec/config.yaml` | OpenSpec configuration |
| `openspec/module-34-context.md` | Architectural context for vertical engine (reference) |
| `openspec/module-34-amendments.md` | ResultOption + TriggerConfig amendments (reference) |
| `openspec/design-docs/inspection-cardinality.md` | Open design doc — blocks Fleet/Hatch/Anchor multi-asset |

---

## Key People
- **Ember pilot contact** — details in taplog-pilot-tester-pool.md
- **Newel pilot contact** — Geoffrey Gordon, Citadel Home Inspections, Bancroft ON
