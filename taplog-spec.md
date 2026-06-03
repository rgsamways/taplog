# TapLog — Project Spec
> Last updated: 2026-06-03

## Overview
Offline-first NFC/RFID asset inspection platform for regulated trades.
- **Domain:** taplog.ca
- **Tagline:** Tap. Log. Done.
- **Package:** ca.taplog.app
- **Android repo:** C:\dev\taplog
- **Backend repo:** C:\dev\taplog-api

## Platform Context — Smallburg Family
TapLog is one of six applications in the **Smallburg** platform family (`smallburg.ca`). Smallburg is municipal-grade compliance infrastructure for small towns and rural communities.

| App | Role | Status |
|---|---|---|
| **TapLog** | NFC asset inspection — generates the compliance record | Active development |
| **Farpost** | Rural insurance dispatch — surfaces TapLog records at claim time | Live (farpost.ca) |
| **Permit** | Construction lending — verifies compliance before fund release | Concept |
| **Ledger** | Property compliance history at point of sale | Concept |
| **Signal** | Commercial insurance risk scoring from compliance data | Concept |
| **Roster** | Industrial workforce credentialing | Concept |

**The engine:** every Smallburg app runs on Ingest → Match → Surface → Act. The civic address / postal code is the universal key. TapLog generates the records; the rest consume them. TapLog → Farpost integration is live: pre-loss inspection records surface automatically in the Farpost adjuster view at claim time.

**TapLog V2 reframe:** V1 answers *was the inspection done?* V2 answers *who was responsible for what, and did they do it?* The missed inspection log with service request trail is the liability transfer instrument: when an asset goes overdue and the owner requests an inspection that goes unanswered, that non-response is a permanent, timestamped record. The owner's culpability for the gap transfers to the contractor who failed to respond.

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
| Mapbox Maps SDK | 11.12.0 |
| Mapbox Compose extension | 11.12.0 |

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
│   ├── EmberModels.kt       — Organisation (+ licensedVerticals), Site (+ latitude, longitude),
│   │                          Asset, Inspection, Deficiency,
│   │                          ScanEvent, TagEvent entities + all enums (incl. TapLogVertical, RetireReason)
│   ├── VerticalModels.kt    — TriggerModel, RoleModel, FieldType, ResultAction, ResultOption,
│   │                          TriggerConfig, FormField, InspectionFormProfile, VerticalAssetType,
│   │                          VerticalConfig, VerticalRegistry singleton
│   ├── Converters.kt        — Room TypeConverters for all enums + List<String> + VerticalConfig (Gson)
│   ├── EmberDAO.kt          — OrganisationDao, SiteDao, AssetDao, InspectionDao, DeficiencyDao,
│   │                          ScanEventDao, TagEventDao
│   ├── VerticalConfigDao.kt — VerticalConfigEntity (Room entity) + VerticalConfigDao (upsert + getAll)
│   ├── AppDatabase.kt       — Room singleton, version 9, taplog_ember.db
│   │                          Migrations: 2→3, 3→4, 4→5, 5→6, 6→7, 7→8, 8→9
│   ├── EmberRepository.kt   — Thin data access layer for ViewModels
│   ├── EmberVerticalConfig.kt — EmberVerticalConfig.build() — translates OFCCategory to VerticalConfig;
│   │                            static cold-start fallback; NOT deleted until backend cache proven
│   ├── InspectorPreferences.kt — DataStore: authToken, refreshToken, inspectorId, deviceId ONLY
│   │                             + InspectorClaims data class + decodeJwtClaims()
│   │                             (lives in ui/ember/ directory but package ca.taplog.app.data)
│   ├── OFCAssetTypes.kt     — OFCCategory enum + OFCAssetType (35 types, checklistItems populated)
│   │                          RETAINED as static fallback — do not delete until VerticalConfig cache proven
│   ├── GeocodingRepository.kt — Calls Mapbox Geocoding REST API; geocode(address, city, province)
│   │                            returns Pair<Double, Double>?; non-blocking, non-fatal
│   ├── SyncModels.kt        — Wire-format request/response models + toSyncRequest() extensions
│   │                          OrganisationSyncRequest includes licensedVerticals;
│   │                          SiteSyncRequest includes latitude, longitude
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
│   │   │                                (derived from JWT), shareReportEvent SharedFlow,
│   │   │                                geocodeUnresolvedSites()
│   │   ├── OrganisationSetupScreen.kt — First launch only — creates org record
│   │   ├── DashboardScreen.kt         — Home screen: StatsStrip, QuickActionsSection, overdue sites,
│   │   │                                embedded site preview (up to 4); ScanState.Dashboard home state
│   │   ├── SiteListScreen.kt          — Full-screen site list (reached via "See all" from Dashboard);
│   │   │                                ViewMode enum (LIST/MAP); lazy geocoding on first map open
│   │   ├── SiteMapView.kt             — MapboxMap composable: Style.DARK, ViewAnnotation pins per
│   │   │                                geocoded site, SitePin composable, unmapped count footer
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
│   │   └── SplashScreen.kt            — Option A brand lockup: Canvas-drawn TapLog icon (navy bg,
│   │                                    teal NFC arcs, phone, log lines, tap dot), animated entrance,
│   │                                    wordmark (Tap bold white / Log teal), tagline, tap-to-continue
│   │                                    hint. Accepts registryReady: Boolean — tap disabled until ready.
│   │                                    Brand colors hardcoded — renders before MaterialTheme.
│   └── theme/
│       ├── Color.kt  — Full TapLog brand palette: Navy (900→50), Teal (900→50), Gray (900→50),
│       │               semantic status (success/warning/danger/info), deficiency severity,
│       │               vertical cluster colors (life-safety coral, property purple,
│       │               infrastructure blue, industrial amber)
│       ├── Theme.kt  — TapLogTheme: Material3 light + dark color schemes using brand palette.
│       │               Light: Navy primary, Teal secondary, White surface.
│       │               Dark: Teal light primary, deep navy background.
│       └── Type.kt   — TapLogTypography bound to MaterialTheme.typography
├── MainActivity.kt          — NFC foreground dispatch, auth gate, AuthFlow composable,
│                              EmberScanScreen, Inspecting state routes MULTI_ROLE → EntryEventScreen
│                              or SINGLE_INSPECTOR → InspectionFormScreen.
│                              Crossfade(400ms) between AppScreen.Splash and AppScreen.App.
├── TapLogApplication.kt     — App singleton: database, repository, inspectorPreferences,
│                              authInterceptor, syncApiService, syncRepository, reportRepository,
│                              geocodingRepository, authViewModelFactory.
│                              onCreate(): MapboxOptions.accessToken = BuildConfig.MAPBOX_PUBLIC_TOKEN
│                              (must be set before any map code — manifest meta-data approach unreliable)
│                              + scheduleSyncIfNeeded() + initVerticalRegistry()
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
│   │                      SiteIn includes optional latitude: float and longitude: float
│   └── routers/
│       ├── __init__.py
│       ├── auth.py         — POST /api/v1/auth/register, verify-email, login,
│       │                     register-device, refresh, resend-code
│       ├── organisations.py — POST /api/v1/organisations (Bearer auth, upsert + inspector org link)
│       ├── sites.py        — POST /api/v1/sites (Bearer auth, upsert; accepts lat/lng)
│       ├── assets.py       — POST /api/v1/assets (Bearer auth, upsert)
│       ├── inspections.py  — POST /api/v1/inspections (Bearer auth, asset existence guard)
│       ├── deficiencies.py — POST /api/v1/deficiencies (Bearer auth, inspection existence guard)
│       ├── scan_events.py  — POST /api/v1/scan_events (Bearer auth, upsert)
│       ├── tag_events.py   — POST /api/v1/tag_events (Bearer auth, upsert)
│       └── verticals.py    — GET /api/v1/verticals (Bearer, filtered by org.licensed_verticals)
│                             GET /api/v1/verticals/{code} (Bearer, single config or 404)
│                             ✅ COMPLETE — Ember seed document in MongoDB verticals collection
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

### Site
```kotlin
@Entity(tableName = "sites")
data class Site(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val organisationId: String,
    val name: String,
    val address: String,
    val city: String,
    val province: String,
    val postalCode: String? = null,
    val clientName: String? = null,
    val clientPhone: String? = null,
    val contactName: String? = null,
    val contactPhone: String? = null,
    val notes: String? = null,
    val isSynced: Boolean = false,
    val latitude: Double? = null,   // MIGRATION_8_9
    val longitude: Double? = null   // MIGRATION_8_9
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

// --- Session 15 additions ---
enum class UserRole {
    INSPECTOR,          // certified professional, full inspection flow
    FIELD_ANALYST,      // asset capture + registration, no inspection
    OWNER_COMMERCIAL,   // business owner, web dashboard + service requests
    OWNER_RESIDENTIAL,  // homeowner, 5-tag kit, remote monitoring
    TENANT,             // renter, move-in/out records, LTB protection
    CARETAKER,          // housewatcher, check-in events, limited access
    SUPERVISOR          // pairs with inspector, inherits on records
}

enum class TagEventRole {
    OWNER,          // registered by property owner self-serve
    FIELD_ANALYST,  // registered by scout on a scouting visit
    INSPECTOR,      // registered by certified inspector during visit
    CARETAKER,      // registered during a caretaker check-in
    TENANT          // registered by tenant at move-in
}

enum class ServiceRequestStatus {
    SENT,           // sent, no response yet
    ACKNOWLEDGED,   // contractor confirmed receipt
    SCHEDULED,      // visit booked
    COMPLETED,      // visit happened, asset tapped
    NO_RESPONSE     // overdue — contractor didn't respond (liability transfer moment)
}

enum class AssetCondition {
    GOOD, FAIR, POOR, NON_FUNCTIONAL, MISSING, UNKNOWN
}
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

### Asset — additions (MIGRATION_9_10)
```kotlin
// Add to existing Asset entity:
val registeredByRole: TagEventRole = TagEventRole.OWNER
val registeredByUserId: String? = null
// Surfaces in Farpost adjuster view: who first registered this asset?
```

### TagEvent — additions (MIGRATION_9_10)
```kotlin
// Add to existing TagEvent entity:
val registeredByRole: TagEventRole = TagEventRole.OWNER
val registeredByUserId: String? = null
val registeredByName: String? = null
val registeredByCertNumber: String? = null  // null for non-inspectors
```

### CheckInEvent (Session 15 — new entity)
```kotlin
@Entity(tableName = "check_in_events")
data class CheckInEvent(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val assetId: String,
    val siteId: String,
    val checkedInById: String,          // caretaker or owner user ID
    val checkedInByName: String,
    val checkedInByRole: UserRole,
    val notes: String? = null,
    val photoUri: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timestampMs: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
```
A lightweight tap event distinct from an inspection. No form — just: I was here, this tag was present and readable, timestamp, identity. A continuous `CheckInEvent` log during a two-year owner absence is the difference between a valid insurance claim and a vacancy clause violation.

### ServiceRequest (Session 15 — new entity)
```kotlin
@Entity(tableName = "service_requests")
data class ServiceRequest(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val assetId: String,
    val siteId: String,
    val requestedById: String,
    val requestedByRole: UserRole,
    val contractorName: String? = null,
    val contractorPhone: String? = null,
    val contractorEmail: String? = null,
    val notes: String? = null,
    val status: ServiceRequestStatus = ServiceRequestStatus.SENT,
    val sentAtMs: Long = System.currentTimeMillis(),
    val respondedAtMs: Long? = null,
    val resolvedAtMs: Long? = null,
    val isSynced: Boolean = false
)
```
Each `ServiceRequest` is a timestamped, asset-linked, identity-anchored record. `NO_RESPONSE` status at `sentAtMs + 7 days` is the liability transfer event — the owner's culpability for the compliance gap transfers to the contractor who failed to respond. Contractor response history feeds directly into Farpost dispatch scoring.

### TenancyRecord (Session 15 — new entity)
```kotlin
@Entity(tableName = "tenancy_records")
data class TenancyRecord(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val siteId: String,
    val tenantId: String,
    val tenantName: String,
    val landlordName: String? = null,
    val landlordPhone: String? = null,
    val landlordEmail: String? = null,
    val moveInDateMs: Long,
    val moveOutDateMs: Long? = null,
    val moveInPhotoUris: List<String> = emptyList(),
    val moveOutPhotoUris: List<String> = emptyList(),
    val notes: String? = null,
    val isSynced: Boolean = false
)
```

### MoveInRecord (Session 15 — child of TenancyRecord)
```kotlin
@Entity(tableName = "move_in_records")
data class MoveInRecord(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val tenancyRecordId: String,
    val assetId: String,
    val condition: AssetCondition = AssetCondition.UNKNOWN,
    val notes: String? = null,
    val photoUri: String? = null,
    val recordedAtMs: Long = System.currentTimeMillis()
)
```
A snapshot of all assets at a specific date, tagged as move-in condition. This becomes the baseline for any future dispute with a landlord. At move-out, `MoveOutScreen` generates a side-by-side comparison.

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
| 9 | MIGRATION_8_9: sites.latitude REAL, sites.longitude REAL added (nullable) |
| 10 | MIGRATION_9_10: check_in_events, service_requests, tenancy_records, move_in_records tables added; assets + tag_events gain registeredByRole, registeredByUserId, registeredByName, registeredByCertNumber columns |

### MIGRATION_9_10 SQL
```sql
CREATE TABLE IF NOT EXISTS check_in_events (
    id TEXT NOT NULL PRIMARY KEY,
    assetId TEXT NOT NULL,
    siteId TEXT NOT NULL,
    checkedInById TEXT NOT NULL,
    checkedInByName TEXT NOT NULL,
    checkedInByRole TEXT NOT NULL,
    notes TEXT,
    photoUri TEXT,
    latitude REAL,
    longitude REAL,
    timestampMs INTEGER NOT NULL,
    isSynced INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS service_requests (
    id TEXT NOT NULL PRIMARY KEY,
    assetId TEXT NOT NULL,
    siteId TEXT NOT NULL,
    requestedById TEXT NOT NULL,
    requestedByRole TEXT NOT NULL,
    contractorName TEXT,
    contractorPhone TEXT,
    contractorEmail TEXT,
    notes TEXT,
    status TEXT NOT NULL DEFAULT 'SENT',
    sentAtMs INTEGER NOT NULL,
    respondedAtMs INTEGER,
    resolvedAtMs INTEGER,
    isSynced INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS tenancy_records (
    id TEXT NOT NULL PRIMARY KEY,
    siteId TEXT NOT NULL,
    tenantId TEXT NOT NULL,
    tenantName TEXT NOT NULL,
    landlordName TEXT,
    landlordPhone TEXT,
    landlordEmail TEXT,
    moveInDateMs INTEGER NOT NULL,
    moveOutDateMs INTEGER,
    moveInPhotoUris TEXT NOT NULL DEFAULT '[]',
    moveOutPhotoUris TEXT NOT NULL DEFAULT '[]',
    notes TEXT,
    isSynced INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS move_in_records (
    id TEXT NOT NULL PRIMARY KEY,
    tenancyRecordId TEXT NOT NULL,
    assetId TEXT NOT NULL,
    condition TEXT NOT NULL DEFAULT 'UNKNOWN',
    notes TEXT,
    photoUri TEXT,
    recordedAtMs INTEGER NOT NULL
);

ALTER TABLE assets ADD COLUMN registeredByRole TEXT NOT NULL DEFAULT 'OWNER';
ALTER TABLE assets ADD COLUMN registeredByUserId TEXT;

ALTER TABLE tag_events ADD COLUMN registeredByRole TEXT NOT NULL DEFAULT 'OWNER';
ALTER TABLE tag_events ADD COLUMN registeredByUserId TEXT;
ALTER TABLE tag_events ADD COLUMN registeredByName TEXT;
ALTER TABLE tag_events ADD COLUMN registeredByCertNumber TEXT;
```

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

## Mapbox Integration (Module 36)

### Token setup
- `MAPBOX_PUBLIC_TOKEN=pk.eyJ1...` in `local.properties`
- `buildConfigField("String", "MAPBOX_PUBLIC_TOKEN", ...)` in `app/build.gradle.kts`
- `MAPBOX_DOWNLOADS_TOKEN=sk.eyJ1...` in `~/.gradle/gradle.properties` (private Maven repo auth)
- **Token must be set programmatically:** `MapboxOptions.accessToken = BuildConfig.MAPBOX_PUBLIC_TOKEN` in `TapLogApplication.onCreate()` — manifest `<meta-data>` approach is unreliable in SDK v11

### Geocoding
- `GeocodingRepository.geocode(address, city, province)` — Mapbox Geocoding REST API v5, `country=CA&limit=1`
- Called from `EmberViewModel.saveSite()` in background coroutine after site saves to Room
- Called from `EmberViewModel.geocodeUnresolvedSites()` on first map open for sites with null coords
- Non-fatal: null return means site saves without coords; shows in list view only

### Map view
- `SiteMapView`: `MapboxMap` composable, `Style.DARK`, camera on site centroid (zoom 10) or Ontario (zoom 7)
- `ViewAnnotation` per geocoded site → `SitePin` composable (labelled Card + Canvas triangle)
- Tapping a pin calls `onSiteSelected(site)` — same path as list row tap

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

## User Role Model (Session 15)

Every user role puts a timestamped, tamper-evident, identity-anchored record between themselves and the moment when someone tries to say they weren't doing what they were supposed to be doing.

### Inspector (existing)
Certified trade professional. Authenticated. Taps tags, completes inspection forms, logs deficiencies, generates reports.

### Field Analyst (new — Module 43)
Robin initially, then trained scouts and subcontractors. Walks buildings systematically, tags unregistered assets, photographs everything compliance-relevant, hands the owner a leave-behind document. No certification required. No inspection form. Pure asset capture — the "birthing event" role. Every asset registered in Field Analyst mode carries `registeredByRole: FIELD_ANALYST`, which surfaces in the Farpost adjuster view.

App behaviour: Role selection screen after splash (one-time). Field Analyst mode → `FieldAnalystDashboardScreen`. Straight into asset capture: tap unregistered tag → register it → next tag. Leave-behind document generated at end of visit. Reset via Settings → "Change my role."

### Building Owner — Commercial (new — Module 44 web)
Brewery owner, restaurant owner, small business operator. Receives tags, registers them on their own assets, monitors compliance dashboard, receives overdue notifications, generates service requests. Browser-first: `taplog.ca/register?tag=TL-000001`. $25/year subscription gates dashboard access.

### Residential Owner / Renter (new — Modules 44–47)
Homeowner, tenant, person in a trailer park. Tags life safety assets. Monitors remotely. Proves compliance to insurer. Protects themselves in a landlord dispute. Five-tag starter kit: smoke detector, CO detector, electrical panel, water shut-off, heat source. Move-in condition record on day one. Highest emotional stakes in the platform.

**Renter kit:** $25/year. 5 tags. A record that starts on move-in day. Distribution partners: tenant advocacy orgs (ACTO), newcomer settlement services, university housing offices, legal aid clinics, trailer park community associations.

### Tenant (new — Module 45)
The 5 million Canadian renter households. Documents condition of assets from move-in to move-out. Builds independent, tamper-evident record for LTB disputes, damage deposit claims, insurance situations. `TenancyRecord` created on first registration. `MoveInRecord` for each asset on day one. `MoveOutRecord` at departure generates comparison. Service request log: "I reported this to my landlord on [date]" — `MaintenanceRequest` with landlord contact. Record follows the person across tenancies.

### Caretaker / Housewatcher (new — Module 46)
Someone looking after a property on behalf of an absent owner. Could be two weeks or two years. Invited via invite token (`CARETAKER` role). Limited access: can tap tags and log check-ins, cannot modify asset records. Every tap creates a `CheckInEvent`. Owner monitors remotely. Caretaker gets their own activity log — proof of responsible stewardship. A continuous `CheckInEvent` log during a two-year absence satisfies a vacancy clause; a missing log may void the homeowner's insurance claim.

### Supervisor (existing concept)
Inspector pairs with a trainee. Every record created by the trainee inherits the supervisor's identity.

---

## New Screens (Session 15 — Modules 42–50)

### `RoleSelectionScreen` (Module 43)
Shown after splash, one time only, if `userRole` not set in DataStore. Two large cards: "I'm here to inspect a site" → INSPECTOR → DashboardScreen; "I'm scoping a new building" → FIELD_ANALYST → FieldAnalystDashboardScreen. Reset via Settings → "Change my role."

### `FieldAnalystDashboardScreen` (Module 43)
Home screen when role is FIELD_ANALYST. Header: today's date + "Field Analyst Mode". Active visit card (if in progress): building name, address, asset count. "Start New Visit" → VisitSetupScreen. Recent visits list (last 5). Quick stat: total assets tagged lifetime.

### `VisitSetupScreen` (Module 43)
Begin a new Field Analyst scouting visit. Fields: building name, address/city/postal, building type dropdown (commercial / restaurant / brewery / residential / other), owner name + phone (optional, for leave-behind header), notes. "Begin Visit" → saves as Site → navigates to FieldAnalystScanScreen.

### `FieldAnalystScanScreen` (Module 43)
Main scanning loop during a visit. Persistent header: building name + asset count. Large NFC scan area with pulsing animation. Asset list (scrollable). "Add manually" button. "Finish Visit" → LeaveBehindsScreen. On unregistered tag tap → QuickRegisterSheet. On registered tag tap → shows asset detail inline.

### `QuickRegisterSheet` (Module 43 — Bottom Sheet)
Fast asset registration during a Field Analyst visit. Completable in under 30 seconds. Fields: asset type (AI-suggested or manual picker), asset name (pre-filled from type), location in building (text), condition (Good/Fair/Poor/Non-functional), photo (optional). "Register Asset" → creates Asset + TagEvent with `registeredByRole: FIELD_ANALYST`.

### `LeaveBehindsScreen` (Module 49)
End-of-visit document generation. Summary: building name, date, total assets tagged. Asset list: type, location, condition, compliance obligation. Red flags section (Poor or Non-functional assets). "Generate PDF Leave-Behind" + "Email to Owner" buttons. The PDF contains: TapLog branding, building details, every tagged asset with compliance obligations, open items, Robin's contact info, and "This record is now live at taplog.ca/register?tag=[first-tag-id]" footer.

### `UnregisteredTagScreen` (Module 43)
First-class handling when any user taps an unregistered tag. NOT an error state — it is an invitation. Large icon + "This asset isn't in TapLog yet." + "Register it now to start its compliance record." Role-aware: FIELD_ANALYST → QuickRegisterSheet → scan loop; INSPECTOR → AssetRegistrationScreen → InspectionFormScreen; TENANT/OWNER_RESIDENTIAL → web registration URL.

### `ServiceRequestScreen` (Module 42)
Owner or tenant generates a formal service request for an overdue asset. Accessed from AssetDetailScreen → "Request Inspection" button. Fields: contractor name, phone, email, notes, urgency (Routine/Urgent/Critical). On submit: creates ServiceRequest with status SENT, timestamped. Optionally sends email/SMS to contractor. After submit: asset detail shows ServiceRequest trail.

### `ServiceRequestTrailScreen` (Module 42)
Full history of service requests for an asset. Chronological list. Each entry: date sent, contractor contacted, status, response date (or "No response"). `NO_RESPONSE` entries highlighted amber/red — these are the liability transfer moments. "Export as PDF" → service request history document for LTB/insurer use.

### `TenancySetupScreen` (Module 45)
Tenant creates a tenancy record at move-in. First launch for TENANT role. Fields: property address, move-in date, landlord name/phone/email, notes. Creates TenancyRecord → navigates to "Now tag your 5 most important assets" walkthrough.

### `MoveInWalkthroughScreen` (Module 45)
Guided 5-asset move-in tagging for tenants. Step-by-step: "Find your smoke detector" → tap → register → photograph → "Good. Next." Repeat for CO detector, electrical panel, water shut-off, heat source. Photo capture encouraged but not required. `AssetCondition` recorded for each. Share link generated on completion.

### `MoveOutScreen` (Module 45)
Tenant documents condition on departure. Side-by-side: move-in condition vs current condition for each tagged asset. Photo comparison if photos taken at move-in. "Generate Move-Out Report" → PDF with both conditions side by side, timestamped. This document is the damage deposit protection.

### `RemoteMonitoringScreen` (Module 47)
Absent owner checks on their property remotely. Property address + last activity date. Asset status grid: green (checked recently), amber (due for check), red (overdue). Caretaker activity: last check-in date, name, what was tapped. Outstanding ServiceRequest records. Alert preferences: "Notify me if no check-in for X days."

### Modified: `DashboardScreen` (Inspector)
Additions: role badge in header ("Inspector — [Vertical]" or "Field Analyst"). Supervision status if paired. "Unregistered assets nearby" suggestion (future).

### Modified: `AssetDetailScreen`
Additions: birthing event provenance card ("Registered by [Name] as [Role] on [Date]"), service request trail (count + latest status → "View all"), "Request Inspection" button when overdue, check-in history (if CheckInEvent records exist), tenancy context (if viewing as tenant).

### Modified: `SplashScreen`
Addition: after `registryReady`, check DataStore for `userRole`. If not set → RoleSelectionScreen. If FIELD_ANALYST → FieldAnalystDashboardScreen. If INSPECTOR → DashboardScreen. If OWNER_* or TENANT → web dashboard ("View your dashboard at taplog.ca").

---

## Web Pages Required (taplog.ca)

### `taplog.ca/register?tag=TL-000001`
Universal landing page. Role-aware based on logged-in state: not logged in → "What is this tag on?" registration form; logged in as owner/tenant → straight to asset registration; logged in as inspector → opens inspection flow (or redirects to app).

### `taplog.ca/dashboard`
Owner/tenant dashboard. $25/year subscription gates access. All tagged assets, status indicators, service request trail, shareable record link generator, subscription management.

### `taplog.ca/record/[share-token]`
Public read-only asset record. Generated by owner/tenant for sharing with insurer, landlord, LTB adjudicator, property buyer. Shows all tagged assets, last inspection dates, service request history (no contractor personal details). Timestamped, tamper-evident.

### `taplog.ca/move-in/[tenancy-id]`
Move-in record public link. Asset conditions as of move-in date. Timestamped, tamper-evident. Filed with landlord at start of tenancy.

---

## Deep Linking — Android App Links (Module 50)

### `assetlinks.json` (host at `https://taplog.ca/.well-known/assetlinks.json`)
```json
[{
  "relation": ["delegate_permission/common.handle_all_urls"],
  "target": {
    "namespace": "android_app",
    "package_name": "ca.taplog.app",
    "sha256_cert_fingerprints": ["<SIGNING_CERT_SHA256_HERE>"]
  }
}]
```

### AndroidManifest.xml intent filter
```xml
<intent-filter android:autoVerify="true">
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="https"
          android:host="taplog.ca"
          android:pathPrefix="/register" />
</intent-filter>
```

### Intent handling in MainActivity
```kotlin
val tagId = intent?.data?.getQueryParameter("tag")
if (tagId != null) {
    val asset = repository.getAssetByTagId(tagId)
    if (asset != null) {
        viewModel.openAssetFromDeepLink(asset)
    } else {
        viewModel.showUnregisteredTag(tagId)  // → UnregisteredTagScreen
    }
}
```

**Dependency:** `assetlinks.json` requires taplog.ca live on HTTPS. Until then, NFC intent handling works without App Links — no browser intercept, but app handles NFC directly.

---

## Notification Architecture (Module 48)

### Asset overdue notifications
When `nextInspectionDue` passes without a new inspection: owner/tenant push notification via FCM — "Your [asset name] at [address] is overdue for inspection." Includes "Request Inspection" deep link → opens ServiceRequestScreen directly.

### Service request no-response alert
When a ServiceRequest reaches `sentAtMs + 7 days` with status still SENT: owner/tenant receives "No response from [contractor name] yet. Your service request has been on record for 7 days." Status automatically set to `NO_RESPONSE` — liability transfer event.

### Caretaker check-in reminder
When CheckInEvent for a property hasn't been created in `alertThresholdDays` (owner-configurable): owner — "No check-in recorded at [address] for [N] days." Caretaker (if app installed) — "Reminder: check in at [address]."

---

## Farpost Integration — Updated (Session 15)

At claim time, adjuster now sees:
- **Birthing event provenance:** who first registered this asset, in what role, on what date
- Full inspection history (existing)
- **Service request trail:** every request sent, every no-response flagged
- **Check-in events:** continuous proof of property stewardship during owner absence
- **Tenancy records:** if property was rented — who the tenant was, when, what condition assets were at move-in
- **Move-in/out comparison:** did asset condition change during the tenancy?

A contractor's TapLog service request `NO_RESPONSE` history feeds into their Farpost dispatch score. Contractors who ghost service requests sink in the candidate queue without any compliance officer in sight.

---

## Sync Architecture

### Sync order
organisations → sites → assets → tag_events → inspections → deficiencies → scan_events

### Backend API
| Endpoint | Auth | Purpose |
|---|---|---|
| `POST /api/v1/organisations` | Bearer | Upsert org (incl. licensedVerticals); links inspector's organisationId |
| `POST /api/v1/sites` | Bearer | Upsert site (incl. latitude, longitude) |
| `POST /api/v1/assets` | Bearer | Upsert asset |
| `POST /api/v1/tag_events` | Bearer | Upsert tag lifecycle events |
| `POST /api/v1/inspections` | Bearer | Upsert inspection, 409 if asset missing |
| `POST /api/v1/deficiencies` | Bearer | Upsert deficiency, 409 if inspection missing |
| `POST /api/v1/scan_events` | Bearer | Upsert scan event |
| `GET /api/v1/verticals` | Bearer | Returns VerticalConfig list for org's licensedVerticals ✅ |
| `GET /api/v1/verticals/{code}` | Bearer | Returns single VerticalConfig or 404 ✅ |

### MongoDB Structure
```
farpost-dev cluster (Atlas M0 free tier)
└── taplog
    ├── organisations
    ├── sites         (includes latitude, longitude fields)
    ├── assets
    ├── tag_events
    ├── inspections
    ├── deficiencies
    ├── scan_events
    ├── inspectors       (unique index on cert_number)
    ├── devices
    ├── pending_verifications  (TTL index on expires_at)
    └── verticals        (Ember seed document deployed ✅)
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
| module-36-site-map | 2026-05-31 |

### Active specs (`openspec/specs/`)
- `ofc-checklists` — checklist source updated to VerticalConfig.assetTypeRegistry
- `org-site-sync` — licensedVerticals + lat/lng added to Organisation/Site sync payloads
- `vertical-config` — VerticalRegistry, Room cache, backend endpoints
- `vertical-form-engine` — field-driven InspectionFormScreen, EntryEventScreen stub
- `site-map-view` — SiteMapView composable, ViewAnnotation pins, SitePin
- `site-geocoding` — GeocodingRepository, lazy geocoding, save-time geocoding
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
- Module 34 backend — verticals endpoint + Ember seed in MongoDB (tasks 9.1–9.4 complete)
- Module 35 — Brand theme: full TapLog palette, Material3 light+dark schemes, SplashScreen brand lockup, Crossfade transition
- Module 36 — Mapbox site map: Room v8→v9 (lat/lng on Site), GeocodingRepository, SiteMapView + SitePin, list/map toggle in SiteListScreen, Mapbox SDK v11.12.0
- Module 39 — Dashboard shell: `DashboardScreen` as home (`ScanState.Dashboard`); `DashboardStats` + `SiteWithOverdueCount` StateFlows; StatsStrip, QuickActionsSection, overdue section, site preview; origin-aware `SiteSelected` back navigation (`fromSiteList` flag)
- Module 40 — Dashboard intelligence: `CalendarSection` (pure-Compose month grid, urgency dots, month navigation); `TasksSection` (unified open deficiencies + overdue assets, priority sort); `ContactsSection` (aggregated site contacts, search, dial); full-screen `CalendarScreen`, `TasksScreen`, `ContactsScreen`; `AssetWithSite` DAO JOIN; `calendarEvents`, `tasks`, `allContacts` StateFlows

### Next (in order)
- [ ] **Module 37** — Anchor config: seed Anchor VerticalConfig to MongoDB (backend-only, zero Android changes)
- [ ] **Module 31** — Visual Asset Identification: photo → Claude vision → `VerticalAssetType` suggestion → pre-selects `AssetTypePickerDialog`
- [ ] **Module 32** — Pre-Inspection Checklist card: `ChecklistCard` composable in `InspectionFormScreen`; tap-to-check; `checklistItems` data already populated
- [ ] **Module 33** — AI co-pilot: floating `?` FAB on `InspectionFormScreen` → `ModalBottomSheet` chat; system prompt generated from `VerticalConfig` at runtime
- [ ] **Module 41** — Billing: Stripe web checkout, subscription gating on Organisation — before OAFC November 2026
- [ ] **Module 42** — Service request trail + missed inspection log: `ServiceRequest` entity, `ServiceRequestScreen`, `ServiceRequestTrailScreen`, `NO_RESPONSE` liability transfer logic — **high priority**
- [ ] **Module 43** — Field Analyst mode + Role Selection + Deep Linking: `RoleSelectionScreen`, `FieldAnalystDashboardScreen`, `VisitSetupScreen`, `FieldAnalystScanScreen`, `QuickRegisterSheet`, `UnregisteredTagScreen`, `UserRole`/`TagEventRole` enums, Room v9→v10 — **high priority**
- [ ] **Module 44** — Owner/Tenant web dashboard + `taplog.ca/register` landing page
- [ ] **Module 45** — Tenancy records + Move-in/out flow: `TenancyRecord`, `MoveInRecord`, `TenancySetupScreen`, `MoveInWalkthroughScreen`, `MoveOutScreen`
- [ ] **Module 46** — Caretaker role + Check-in events: `CheckInEvent`, invite token model, CARETAKER access control
- [ ] **Module 47** — Remote monitoring screen: `RemoteMonitoringScreen` (Android + web)
- [ ] **Module 48** — Notification architecture: FCM, asset overdue push, service request no-response alert, caretaker check-in reminder
- [ ] **Module 49** — Leave-behind PDF generator (Field Analyst end-of-visit document)
- [ ] **Module 50** — Deep linking: `assetlinks.json`, `taplog.ca` live on HTTPS, App Links verified

### Deferred
- [ ] **Module 38** — Hatch config + EntryEventScreen implementation
- [ ] **Onboarding v2** — Invite token model + V2 admin portal (prerequisite for Module 46 caretaker invites)

---

## Vertical Roadmap
| Vertical | Domain | Status |
|---|---|---|
| Ember | Fire safety inspection | ✅ Active — v1 complete, vertical engine + site map live |
| Anchor | Fall protection | Module 37 — backend config seed |
| Hatch | Confined space | Module 38 — deferred; EntryEventScreen + config |
| Lift | Elevators / escalators | Tier 1 |
| Newel | Home inspection (+ residential renter) | Held for pilot — Geoffrey Gordon, Citadel Home Inspections, Bancroft ON. Note: renter use case (Module 45) naturally extends Newel into residential compliance |
| Apex | HVAC | Tier 2 |
| Vault | Electrical | Tier 2 |
| Crane | Crane and hoist | Tier 2 (inspection cardinality blocker) |
| Fleet | Fleet vehicles | Tier 2 (inspection cardinality blocker) |
| Span | Bridges / structures | Tier 3 |

**Renter / residential kit cross-cut:** The TENANT and OWNER_RESIDENTIAL user roles are not vertical-specific — they run across any life safety asset already registered in TapLog. The $25/year renter starter kit (5 tags: smoke detector, CO detector, electrical panel, water shut-off, heat source) uses existing Ember/Newel asset types. No new vertical needed — `UserRole.TENANT` + `TenancyRecord` + `MoveInRecord` extend the existing engine.

---

## Vertical Bundles

A `VerticalBundle` is a named, branded collection of `TapLogVertical` enums targeted at a specific industry. Bundles are presented first during org onboarding; individual vertical selection is available as "custom" for orgs that don't fit a bundle.

**Design principle:** Bundle names use the target industry's own vocabulary, not TapLog's internal vertical names. The buyer recognizes themselves in the bundle name before reading a single feature description.

**Data model (concept):**
```
VerticalBundle(
  id: String,
  displayName: String,         // e.g. "TapLog Adit"
  industryTag: String,         // e.g. "mining"
  verticals: List<TapLogVertical>,
  billingSku: String
)
```

**Bundle catalogue:**

| Bundle | Industry | Verticals | Status |
|---|---|---|---|
| **TapLog Adit** | Mining / Underground | Ember, Anchor, Hatch | Concept |

**TapLog Adit — rationale:**
Underground mining operations require fire suppression inspection (Ember), fall protection (Anchor), and confined space entry management (Hatch) as a baseline life safety cluster. No existing tool packages these three with offline-first, NFC proof-of-presence, and mining-appropriate asset vocabulary. "Adit" is genuine mining terminology (a horizontal entry tunnel) — recognizable to the industry, no negative common-language connotation, and carries a latent product metaphor: an adit is how you get into the mine; TapLog Adit is how you get into compliance.

**Go-to-market note:** PDAC (Prospectors & Developers Association of Canada) conference is the target launch venue for TapLog Adit if the mining vertical is ready by 2027.



---

## Key Files Reference

### Android
| File | Purpose |
|---|---|
| `ca.taplog.app/MainActivity.kt` | Entry point, NFC, auth gate, Inspecting state routes by roleModel, Crossfade splash→app |
| `ca.taplog.app/TapLogApplication.kt` | App singleton, MapboxOptions token, initVerticalRegistry(), verticalRegistryReady StateFlow |
| `ca.taplog.app/SyncWorker.kt` | WorkManager CoroutineWorker — syncAll() |
| `ca.taplog.app/ConnectivityReceiver.kt` | BroadcastReceiver — sync on connectivity restore |
| `ca.taplog.app.data/EmberModels.kt` | All entities + enums incl. Organisation.licensedVerticals, Site lat/lng |
| `ca.taplog.app.data/VerticalModels.kt` | Full vertical data model + VerticalRegistry singleton |
| `ca.taplog.app.data/Converters.kt` | TypeConverters: enums + List<String> + VerticalConfig (Gson) |
| `ca.taplog.app.data/EmberDAO.kt` | All DAOs |
| `ca.taplog.app.data/VerticalConfigDao.kt` | VerticalConfigEntity + VerticalConfigDao (upsert/getAll) |
| `ca.taplog.app.data/AppDatabase.kt` | Room singleton, version 9, migrations 2→3→4→5→6→7→8→9 |
| `ca.taplog.app.data/EmberRepository.kt` | Data access layer incl. replaceTag() atomic transaction |
| `ca.taplog.app.data/EmberVerticalConfig.kt` | EmberVerticalConfig.build() — static Ember VerticalConfig fallback |
| `ca.taplog.app.data/InspectorPreferences.kt` | DataStore: auth + deviceId + decodeJwtClaims() (in ui/ember/ dir, package ca.taplog.app.data) |
| `ca.taplog.app.data/OFCAssetTypes.kt` | 35 OFC asset types — retained as static fallback |
| `ca.taplog.app.data/GeocodingRepository.kt` | Mapbox Geocoding REST API — address → lat/lng; non-blocking, non-fatal |
| `ca.taplog.app.data/SyncModels.kt` | Wire-format models + toSyncRequest() — incl. lat/lng on SiteSyncRequest |
| `ca.taplog.app.data/RetrofitClient.kt` | Dual Retrofit clients |
| `ca.taplog.app.data/TapLogApiService.kt` | Retrofit interface — verticals GETs + sync POSTs |
| `ca.taplog.app.data/AuthApiService.kt` | Auth endpoints |
| `ca.taplog.app.data/AuthInterceptor.kt` | Bearer injection, 401 refresh retry |
| `ca.taplog.app.data/PdfReportGenerator.kt` | PdfDocument canvas renderer |
| `ca.taplog.app.data/ReportRepository.kt` | generateAndGetUri() → FileProvider URI |
| `ca.taplog.app.data/SyncRepository.kt` | Batch sync, Bearer auth, full sync order |
| `ca.taplog.app.ui.auth/` | AuthViewModel + 4 auth screens |
| `ca.taplog.app.ui.ember/EmberViewModel.kt` | ScanState, DashboardStats, SiteWithOverdueCount StateFlows, inspectorClaims, shareReport, geocodeUnresolvedSites |
| `ca.taplog.app.ui.ember/DashboardScreen.kt` | Home screen — StatsStrip, QuickActionsSection, overdue sites, site preview, CalendarSection, TasksSection, ContactsSection |
| `ca.taplog.app.ui.ember/CalendarScreen.kt` | Full-screen month calendar — urgency dots, day-tap expansion, month navigation |
| `ca.taplog.app.ui.ember/TasksScreen.kt` | Unified task list — open deficiencies + overdue assets; filter chips (All/Deficiencies/Overdue) |
| `ca.taplog.app.ui.ember/ContactsScreen.kt` | Aggregated site contacts — search, tap-to-dial, row → SiteDetailScreen |
| `ca.taplog.app.ui.ember/SiteListScreen.kt` | Full-screen site list (from Dashboard "See all") with list/map ViewMode toggle |
| `ca.taplog.app.ui.ember/SiteMapView.kt` | MapboxMap + ViewAnnotation pins + SitePin composable |
| `ca.taplog.app.ui.ember/InspectionFormScreen.kt` | Field-driven: ResultOption selector + FormField loop — zero OFC imports |
| `ca.taplog.app.ui.ember/EntryEventScreen.kt` | MULTI_ROLE stub — "Multi-role entry form — coming soon" |
| `ca.taplog.app.ui.ember/SplashScreen.kt` | Brand lockup: Canvas icon, animated entrance, tap-to-continue gate |
| `ca.taplog.app.ui.ember/AssetDetailScreen.kt` | Scan history + Replace Tag + Share Report + service request trail + birthing event provenance (Session 15 additions) |
| `ca.taplog.app.ui.ember/RoleSelectionScreen.kt` | One-time role selector: Inspector vs Field Analyst — Module 43 |
| `ca.taplog.app.ui.ember/FieldAnalystDashboardScreen.kt` | Field Analyst home: visit list, start visit, asset count — Module 43 |
| `ca.taplog.app.ui.ember/VisitSetupScreen.kt` | Field Analyst visit setup: building name, address, type, owner contact — Module 43 |
| `ca.taplog.app.ui.ember/FieldAnalystScanScreen.kt` | Field Analyst scan loop: NFC + asset list + finish visit — Module 43 |
| `ca.taplog.app.ui.ember/QuickRegisterSheet.kt` | Bottom sheet: fast asset registration during Field Analyst visit — Module 43 |
| `ca.taplog.app.ui.ember/UnregisteredTagScreen.kt` | Role-aware unregistered tag handler — Module 43 |
| `ca.taplog.app.ui.ember/ServiceRequestScreen.kt` | Owner/tenant generates service request for overdue asset — Module 42 |
| `ca.taplog.app.ui.ember/ServiceRequestTrailScreen.kt` | Full service request history per asset; NO_RESPONSE liability transfer — Module 42 |
| `ca.taplog.app.ui.ember/TenancySetupScreen.kt` | Tenant creates tenancy record at move-in — Module 45 |
| `ca.taplog.app.ui.ember/MoveInWalkthroughScreen.kt` | Guided 5-asset move-in tagging walkthrough — Module 45 |
| `ca.taplog.app.ui.ember/MoveOutScreen.kt` | Side-by-side move-in vs current comparison + report — Module 45 |
| `ca.taplog.app.ui.ember/RemoteMonitoringScreen.kt` | Absent owner remote property monitoring — Module 47 |
| `ca.taplog.app.ui.ember/LeaveBehindsScreen.kt` | Field Analyst end-of-visit PDF generator — Module 49 |
| `ca.taplog.app.ui.theme/Color.kt` | Full TapLog brand palette |
| `ca.taplog.app.ui.theme/Theme.kt` | TapLogTheme: Material3 light + dark schemes |
| `ca.taplog.app.ui.theme/Type.kt` | TapLogTypography |
| `app/src/main/AndroidManifest.xml` | NFC, windowSoftInputMode, ConnectivityReceiver, FileProvider, Mapbox meta-data |
| `app/src/main/res/xml/file_paths.xml` | FileProvider path config |

### Backend
| File | Purpose |
|---|---|
| `app/main.py` | FastAPI app, all routers |
| `app/database.py` | Motor client, Settings |
| `app/auth.py` | JWT, bcrypt (direct), send_verification_email |
| `app/dependencies.py` | get_current_inspector Bearer dependency |
| `app/models.py` | All Pydantic models (SiteIn includes lat/lng) |
| `app/routers/auth.py` | 6 auth endpoints |
| `app/routers/organisations.py` | POST /api/v1/organisations |
| `app/routers/sites.py` | POST /api/v1/sites (accepts lat/lng) |
| `app/routers/verticals.py` | GET /api/v1/verticals, GET /api/v1/verticals/{code} |
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
| `openspec/module-36-context.md` | Architectural context for site map / Mapbox (reference) |
| `openspec/design-docs/inspection-cardinality.md` | Open design doc — blocks Fleet/Hatch/Anchor multi-asset |

---

## Key People
- **Ember pilot contact** — details in taplog-pilot-tester-pool.md
- **Newel pilot contact** — Geoffrey Gordon, Citadel Home Inspections, Bancroft ON
