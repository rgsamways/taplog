# Building a Product from Scratch Using AI
### A curriculum derived from building TapLog — a real product, built in real time

---

## Course premise

This curriculum documents the actual process of taking a software product from zero to shipped, using AI as a thinking partner, strategist, visualizer, and coding assistant. Every module reflects something that actually happened — no hypotheticals, no toy examples.

The product being built is **TapLog** (taplog.ca) — an offline-first NFC/RFID asset inspection platform for the trades, built natively for Android in Kotlin.

**Dev environment:** Android Studio Panda 4 | 2025.3.4 Patch 1, Windows
**Dev device:** Google Pixel 10 Pro XL
**Project location:** C:\dev\taplog
**Package:** ca.taplog.app
**Stack:** Kotlin 2.3.21 · Jetpack Compose · Room 2.8.4 · KSP 2.3.9 · AGP 9.2.1 · DataStore 1.1.1 · room.generateKotlin=true · material-icons-extended · Mapbox Maps SDK 11.12.0

**Backend location:** C:\dev\taplog-api
**Backend stack:** Python 3.12 · FastAPI · Motor · MongoDB Atlas · Railway

---

## How this course works

Each session with AI is documented as one or more modules. The pattern repeats:
1. You describe what you want to build or decide
2. AI thinks with you, challenges assumptions, suggests structure
3. You act (write code, configure tools, make decisions)
4. You screenshot results and share them — AI verifies and guides next steps
5. The outcome gets documented here as a module

The screenshot workflow is a core technique: AI instructs → human acts → human screenshots → AI verifies → AI instructs again. It keeps the AI grounded in what's actually on screen and means the learner never gets lost.

**File replacement workflow:** When replacing file contents in Android Studio, always use select-all (Ctrl+A) → paste inside the editor. Never copy files via Windows Explorer — it triggers refactor dialogs that append content instead of replacing it, causing redeclaration errors.

**One file at a time:** Confirm paste before receiving the next file. Keeps the workflow clean and catches errors before they compound across multiple files.

**OpenSpec workflow:** As of Session 6, OpenSpec is installed at `C:\dev\taplog`. Every module starts with `/opsx:propose` and ends with `/opsx:archive`. No code gets written until the proposal is reviewed. This is the enforcement mechanism for spec alignment across sessions.

---

## Module 1 — Ideation and market framing

**What we did:**
- Started with a platform concept (TapLog) and pressure-tested it out loud with AI
- Named the core mechanic: tap a physical asset with your phone → log an inspection record → sync when back online
- Identified the tagline: *Tap. Log. Done.*
- Established that NFC (Near Field Communication) is the right technology — mature, in every modern phone, passive tags cost pennies, proves physical presence

**Key lessons:**
- AI is most useful at ideation stage when you treat it as a thinking partner, not an answer machine
- A good tagline does strategic work — it defines the product's scope implicitly
- Start with the mechanic, not the market
- NFC has been in phones since 2011 but remains underused in industrial/inspection contexts — mature tech + underserved application = strong product position

---

## Module 2 — Platform architecture thinking

**What we did:**
- Established TapLog as a platform containing vertical-specific worlds (Ember, Newel, Anchor, etc.)
- Used the AI/machine learning analogy to clarify the containment relationship
- Visualized the concept as a nested box diagram
- Named 18 verticals across 4 clusters: Life Safety, Property, Infrastructure, Industrial

**Key lessons:**
- Drawing before building forces clarity — if you can't draw it, you don't understand it yet
- The platform model (one engine, many verticals) is a strategic moat, not just an architecture decision
- A shared data model across verticals is the technical expression of a platform moat

---

## Module 3 — Competitive landscape research

**What we did:**
- Used AI + web search to map existing tools in the inspection software space
- Categorized competition into three buckets: too broad, too deep, too enterprise
- Identified that no player is building the platform model

**Key lessons:**
- Categorizing competitors by *why they're not a threat* is more useful than listing them
- White space is most valuable when it combines: high regulatory urgency + no existing tool + offline-critical environment

---

## Module 4 — Prioritization matrix

**What we did:**
- Built a scored matrix of 15 verticals across 6 dimensions
- Set vertical build sequence: (1) Ember (2) Anchor (3) Hatch — "life safety" cluster

**Key lessons:**
- A scoring matrix forces you to make your assumptions explicit
- "Most underserved" and "highest priority" are not always the same thing

---

## Module 5 — Strategic relationship mapping (TapLog + Farpost)

**What we did:**
- Established: TapLog is a pre-loss truth machine, Farpost is where that truth gets used post-loss
- Identified the flywheel: more inspections → better Farpost signal → more adoption → more TapLog demand

**Key lessons:**
- Two products that compound each other's value are more defensible than one product alone
- The integration is the moat, not the bundle

---

## Module 6 — Visual platform documentation

**What we did:**
- Built a platform map poster (TapLog at centre, all verticals by cluster, Farpost downstream)
- Established the two-artifact system: platform map (shape) + living document (depth)

---

## Module 7 — Android project setup

**What we did:**
- Created Android project in Android Studio with package `ca.taplog.app`
- Added Room, KSP, and Compose dependencies to `build.gradle.kts` and `libs.versions.toml`
- Configured `room.generateKotlin=true`

**Key lessons:**
- Version catalog (`libs.versions.toml`) is the modern way to manage Android dependencies — one source of truth
- KSP replaces KAPT for annotation processing — faster builds, required for Room with Kotlin

---

## Module 8 — NFC reading

**What we did:**
- Added NFC permission to `AndroidManifest.xml`
- Implemented foreground dispatch in `MainActivity.kt` — app gets first priority on NFC taps while open
- Extracted tag ID as hex string from `NdefMessage` or raw tag ID bytes

**Key lessons:**
- NFC foreground dispatch requires `onResume`/`onPause` lifecycle management
- Tag ID is hardware-baked and unforgeable without physical access — this is the proof-of-presence guarantee

---

## Module 9 — Room data model

**What we did:**
- Created `EmberModels.kt` with `Asset`, `Inspection`, `Deficiency` entities and `DeficiencyWithAsset` non-entity
- Created `Converters.kt` for `InspectionResult` and `DeficiencySeverity` enums
- Created `EmberDAO.kt` with `AssetDao`, `InspectionDao`, `DeficiencyDao`
- Created `AppDatabase.kt` as Room singleton

**Key lessons:**
- Room requires TypeConverters for enums — they don't store natively
- `@PrimaryKey val id: String = UUID.randomUUID().toString()` — UUID assigned on device, never by server
- Foreign key constraints are enforced at insert time

---

## Module 10 — Repository and Application class

**What we did:**
- Created `EmberRepository.kt` as thin layer over DAOs
- Created `TapLogApplication.kt` with lazy singletons for database, repository, inspectorPreferences

**Key lessons:**
- The Application class is instantiated once at app launch — the right place for app-wide singletons
- Lazy initialization means the database isn't opened until first use

---

## Module 11 — ViewModel and ScanState machine

**What we did:**
- Created `EmberViewModel.kt` with `ScanState` sealed class
- Implemented NFC tag scan → DB lookup → state transition flow
- Added `saveAsset()` and `saveInspection()` coroutine functions

**Key lessons:**
- Sealed classes are perfect for modelling UI state machines — exhaustive `when` expressions catch missing states at compile time
- ViewModel survives screen rotation — never put UI state in an Activity

---

## Module 12 — Asset registration screen

**What we did:**
- Created `AssetRegistrationScreen.kt` for registering a new NFC tag as an asset
- Form fields: name, asset type, location, building name, install date

---

## Module 13 — Asset detail screen

**What we did:**
- Created `AssetDetailScreen.kt` showing asset info and full inspection history
- "Start Inspection" button transitions to `Inspecting` state

---

## Module 14 — Inspection form screen

**What we did:**
- Created `InspectionFormScreen.kt` with result picker, notes field, deficiency dialog
- `AddDeficiencyDialog` composable for inline deficiency entry during inspection

---

## Module 15 — Full flow working on device

**What we did:**
- Ran complete flow on Pixel 10 Pro XL: Scan → Register → Detail → Inspect → Submit
- Verified Room database writes with Compass

**Full flow confirmed working on device:**
Scan → [registered] → Asset Detail → Start Inspection → Fill Form → Add Deficiency → Submit → back to Asset Detail

---

## Module 16–19 — Bug fixes and polish

**What we did:**
- Fixed keyboard/scroll issues: `verticalScroll` + `imePadding` + `windowSoftInputMode="adjustResize"`
- Fixed deficiency `inspectionId` crash: deficiencies were inserted with blank ID before inspection was saved
- Fix: `.map { it.copy(inspectionId = inspection.id) }` before insert in `saveInspection()`

**Key lessons:**
- Room foreign key constraints are enforced at insert time — a blank foreign key will crash
- `imePadding()` + `adjustResize` is the correct Compose keyboard handling pattern

---

## Module 20 — Inspector profile (DataStore)

**What we did:**
- Added DataStore Preferences dependency
- Created `InspectorPreferences.kt` — DataStore wrapper exposing `inspectorName` and `certNumber` as `Flow<String>`
- Used `LaunchedEffect(savedName, savedCertNumber)` to handle async DataStore load timing
- Profile auto-saves silently on every inspection submit

**Key lessons:**
- DataStore is async — values aren't available instantly on first composition; use `LaunchedEffect` to fill fields once loaded
- `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")` is the correct pattern to convert a Flow to StateFlow in a ViewModel
- No separate settings screen needed — auto-save on submit is the right UX for a field tool

---

## Module 21 — Asset list screen

**What we did:**
- Created `AssetListScreen.kt` — `LazyColumn` of all active assets, tap to open asset detail
- `AssetListCard` highlights overdue assets in `errorContainer` color with an "OVERDUE" badge
- Added `AssetList` to `ScanState` sealed class

**Key lessons:**
- `key = { it.id }` on `items()` helps Compose efficiently recompose only changed items
- Overdue state is derived at render time from `asset.nextInspectionDue` vs `System.currentTimeMillis()` — no extra DB query needed

---

## Module 22 — Open deficiencies screen

**What we did:**
- Added `DeficiencyWithAsset` data class — JOIN result carrying asset name and building name
- Added three-table JOIN query to `DeficiencyDao` filtered to `resolvedAt IS NULL`, ordered by severity DESC
- Created `OpenDeficienciesScreen.kt` with severity color coding and "Mark Resolved" workflow

**Key lessons:**
- Room supports multi-table JOINs returning non-entity data classes
- "Mark Resolved" sets `resolvedAt` timestamp — soft delete pattern, preserves audit history
- Severity ordering in the DAO query means the UI never needs to sort

---

## Module 23 — FastAPI backend (taplog-api)

**What we did:**
- Created `taplog-api` GitHub repo and FastAPI project at `C:\dev\taplog-api`
- Built three sync endpoints: `POST /api/v1/assets`, `POST /api/v1/inspections`, `POST /api/v1/deficiencies`
- All endpoints use `find_one_and_replace` with `upsert=True` — idempotent on device UUID
- Dependency guards: inspections reject with 409 if asset not synced; deficiencies reject with 409 if inspection not synced
- API key auth via `x-api-key` header on all endpoints
- `synced_at` epoch millis timestamp on every backend document
- Deployed to Railway with auto-deploy from GitHub
- MongoDB writing to `taplog` database on existing `farpost-dev` Atlas M0 cluster

**Key lessons:**
- Device UUIDs as `_id` in MongoDB makes upserts naturally idempotent — re-sending any record is always a no-op
- 409 (Conflict) is the correct status for a dependency guard failure — it's retryable, unlike 400 or 500
- Motor is the correct MongoDB driver for FastAPI — pymongo is synchronous and blocks the async event loop
- Railpack requires `requirements.txt` to trigger pip install — `pyproject.toml` alone is not detected

---

## Module 24 — Android Retrofit HTTP client

**What we did:**
- Added Retrofit 2.11, OkHttp 4.12, Gson converter, and WorkManager 2.9.1 to `build.gradle.kts`
- Created `SyncResult.kt`, `SyncModels.kt`, `RetrofitClient.kt`, `TapLogApiService.kt`, `SyncRepository.kt`
- Added `isSynced: Boolean = false` to all three entities
- Bumped Room database version to 2 with `fallbackToDestructiveMigration()`

**Key lessons:**
- `GsonBuilder().setFieldNamingPolicy(LOWER_CASE_WITH_UNDERSCORES)` maps Kotlin camelCase to Python snake_case automatically
- `Response<T>` (not raw `T`) gives HTTP status code access — essential for 409 handling
- Keep wire-format models (`SyncModels.kt`) separate from Room entities (`EmberModels.kt`)
- `BuildConfig` fields sourced from `local.properties` is the standard Android pattern for secrets

---

## Module 25 — WorkManager SyncWorker

**What we did:**
- Created `SyncWorker.kt` — `CoroutineWorker` calling `syncRepository.syncAll()`
- Created `ConnectivityReceiver.kt` — `BroadcastReceiver` that triggers sync on connectivity restore
- Fixed Android 16 NFC foreground dispatch — explicit `Intent(this, MainActivity::class.java)` with `FLAG_IMMUTABLE`
- Verified end-to-end: NFC tap → Room → WorkManager → Retrofit → Railway → MongoDB Atlas

**Key lessons:**
- `setRequiredNetworkType(NetworkType.CONNECTED)` makes WorkManager hold the job until connected
- `ExistingWorkPolicy.KEEP` prevents duplicate sync jobs on rapid connectivity changes
- `Result.retry()` triggers WorkManager exponential backoff — correct for network failures
- Android 16 BAL restrictions block implicit intents from the NFC system process on API 36

---

## Module 26 — Ontario Fire Code asset type picker

**What we did:**
- Created `OFCAssetTypes.kt` — new file in `data/`. A Kotlin enum `OFCCategory` where each entry carries a `List<OFCAssetType>`. Each `OFCAssetType` has a `code` (stored in Room), a `label` (displayed in UI), `inspectionIntervalMonths` (drives nextInspectionDue auto-calculation), and a `description` (plain-English for novice inspectors). Six categories: Portable Extinguishers, Suppression Systems, Detection & Alarm, Emergency Lighting & Exit Signs, Fire Doors & Closers, Firefighting Equipment. 35 asset types total.
- Created `AssetTypePickerDialog.kt` — AlertDialog with search field, `FilterChip` category row (hidden while searching), and `LazyColumn` of asset types. Each row shows label, plain-English description, OFC code, and inspection interval. Search matches against label, code, AND description — a novice can type "red cylinder" or "kitchen" and get relevant results.
- Updated `AssetRegistrationScreen.kt` — replaced free-text `assetType` TextField with read-only field + invisible clickable `Box` overlay that opens the picker. Added interval hint once a type is selected.
- Updated `EmberViewModel.saveAsset()` — new `inspectionIntervalMonths` parameter; `nextInspectionDue` auto-calculated as `installDate + (intervalMonths × 30 days in millis)`.
- Updated `AssetDetailScreen.kt` — `asset.assetType` wrapped with `OFCCategory.labelForCode(code)` for display; falls back to raw string for legacy free-text values.
- Added `AssetDetailSource` enum (`FROM_SCAN` / `FROM_LIST`) as `StateFlow` in `EmberViewModel`. Back button reads "Asset List" or "Home" and routes correctly in both cases.
- Created `taplog-lightbulbs.md` — new document capturing product insight moments from the build process.

**No data model changes.** `Asset.assetType` remains `String` — now stores OFC code (e.g. `"EXT_ABC"`) instead of free text. No Room migration needed.

**Key lessons:**
- A hardcoded data hierarchy (enum + data class) is the right choice for regulated code lists — they change slowly, require no DB complexity, and are instantly queryable in-memory
- `OFCCategory.labelForCode()` with a raw-string fallback is a zero-cost compatibility shim — legacy data is never broken
- `inspectionIntervalMonths` on the asset type object is the correct place to encode regulatory knowledge — the ViewModel stays ignorant of OFC rules and just does arithmetic
- Invisible clickable `Box` overlay on a read-only `OutlinedTextField` is the standard Compose workaround for picker fields — `OutlinedTextField` has no `onClick` parameter
- Search should match against descriptions, not just labels and codes — a novice doesn't know the OFC vocabulary
- Wet chemical (Class K) and kitchen hood suppression are semi-annual (6 months) — encoding this in the data enforces correct cadence without business logic in the UI
- Navigation paths reveal your mental model of the app — where Back goes tells the user what the app thinks they were doing
- Regulatory knowledge belongs in data, not in UI — when the OFC changes an interval, update one line in `OFCAssetTypes.kt`
- `material-icons-extended` is a separate dependency — if `Icons.Filled.ArrowDropDown` fails, use a Unicode character (`▾`) rather than adding the dependency for one icon
- Always use select-all-paste in Android Studio when replacing file contents — copying files via Explorer triggers refactor dialogs that append instead of replacing

---

## Module 26b — OpenSpec setup and session workflow

**What we did:**
- Installed OpenSpec globally: `npm install -g @fission-ai/openspec@latest`
- Ran `openspec init` in `C:\dev\taplog` — selected Claude Code as the target tool
- OpenSpec created `.claude/` folder with 4 skills and 4 commands (`/opsx:propose`, `/opsx:apply`, `/opsx:archive`, `/opsx:explore`)
- Copied prime directives into `.claude/AGENTS.md` — the file Claude Code reads automatically at session start: `copy taplog-md-prime-directives.md .claude\AGENTS.md`
- Established end-of-session workflow: update docs → generate handoff → sync AGENTS.md
- Created `taplog-md-prime-directives.md` as the living source of truth for session workflow rules

**Key lessons:**
- OpenSpec solves the "how do I know the AI read the directives" problem — the spec lives in the repo, not in a chat upload
- `AGENTS.md` in `.claude/` is read automatically by Claude Code at session start — no manual upload required
- The propose → apply → archive workflow creates a verifiable checkpoint before any code is written
- Specs live in the repository alongside the code — context persists across sessions, team members, and tool switches
- `taplog-md-prime-directives.md` is the source of truth; `.claude/AGENTS.md` is the sync target — always run the copy command at end of session
- OpenSpec is tool-agnostic — if you switch from Claude Code to another AI assistant, the specs travel with you

---

## Module 27 (strategy) — Product strategy, go-to-market, and roadmap resequencing

**What we did:**
This was a full strategy session — no code written. The outputs inform the next several months of build and go-to-market decisions.

**Life safety verticals matrix built:**
Ember, Anchor, and Hatch mapped across legislation, inspection triggers, data point volume, revenue ceiling, user profile, and TapLog strategic fit. Key findings: Anchor generates the highest data volume (pre-use checks are daily, 25–250M events/year in Ontario), Hatch has the highest legal stakes per event and the strongest enterprise sale, Ember remains the correct beachhead for its structured asset types and identifiable buyer.

**Go-to-market defined:**
- Three distinct buyer types: inspection company owner (Ember), employer/safety manager (Anchor/Hatch), building owner/property manager (v2)
- Revenue model: Solo $39/mo, Team $29/mo per seat, Company $199/mo flat — all month-to-month
- Corridor-first strategy: Bancroft–Peterborough–Belleville before provincial expansion
- Key events: OAFC Annual Conference & Trade Show, Niagara Falls, November 19–21 2026 (target with paying reference customers)
- Activation insight: the tag problem — inspectors can't complete step 3 without NFC tags. Tags must be in their hands before they touch the app.

**Competitive depth mapped:**
- Inspect Point: purpose-built but annual contract only, $200–500/mo small shops, 3.2/5 value rating, slow support, reporting gaps. Attack angle: built for the office, not the field.
- FireMate/Uptick: Australian acquisition, enterprise pricing, Australian regulatory assumptions. Attack angle: doesn't know the OFC.
- SafetyCulture: generic blank canvas, no OFC knowledge, no NFC. Attack angle: you're building forms TapLog ships out of the box.
- Competitive moat: NFC proof of presence — no competitor offers this.

**Proof of presence security model defined:**
Five attack vectors identified and countered. Critical gap identified: local DataStore identity (name + cert number) is insufficient for a compliance product. Cert numbers are semi-public via OFM registry — knowing name + cert is not enough friction.

**Roadmap resequencing — critical decision:**
Module 28 promoted from PDF reports to Inspector Accounts + Authenticated Identity. Rationale: a compliance product without authenticated identity is not a compliance product. Pilot user has no existing app data — clean slate, no migration required.

**New module sequence:**
- Module 27 — Tamper-evident scan log
- Module 28 — Inspector accounts + authenticated identity (PROMOTED)
- Module 29 — PDF report generation
- Module 30 — Photo capture for deficiencies
- Module 31 — Visual Asset Identification

**Module 28 fully specced:**
Registration → email verification (6-digit code, 15-min TTL) → JWT → device registration. New device triggers email confirmation before access granted. Cert number unique at DB level. All sync endpoints require Bearer token. Resend for transactional email. New MongoDB collections: `inspectors`, `devices`, `pending_verifications`. Android: new `ui/auth/` package, AuthInterceptor, DataStore stripped to token + deviceId only. deviceId = UUID generated once at install, never rotated.

**Key lessons:**
- The activation gap kills SaaS trials in hardware-dependent products — solve the tag problem before the first pilot, not after
- Proof of presence is multi-layered: NFC tap + on-metal tag destruction + scan log + GPS + device ID + authenticated identity. Each layer raises the cost of fraud.
- "We don't make fraud impossible. We make it stupid." — the investor answer on tamper resistance
- Local device identity is not compliance-grade identity. The cert number attack vector (semi-public OFM registry) is real and closes with Module 28.
- The $39/month price point is below every meaningful competitor and above the "is this a real product" threshold. A fire safety inspector billing $75–150/hour doesn't think twice about $39/month if it saves 20 minutes per inspection.
- Inspect Point's 3.2/5 value-for-money score and annual-contract-only policy are exploitable weaknesses — TapLog's month-to-month positioning is a direct counter.
- FireMate is now Uptick (Australian acquisition) — the "doesn't know the OFC" angle is legitimate and verifiable.
- The OAFC November 2026 conference is the first real trade show target. The goal is to walk in with a paying reference customer.
- **Asset identity ≠ tag identity.** `Asset.nfcTagId` is a current pointer, not permanent identity. Tags are replaceable. History must survive tag replacement. This is a fundamental data model principle confirmed by firsthand experience with Toyota and FileTrack asset management implementations — get this right early or pay a painful migration cost later.
- Tag replacement without history loss requires decoupling asset UUID from tag UID at the schema level, a `TagEvent` lifecycle table, and a replacement workflow in the UI. All three must ship together.
- **Regulatory change is a tailwind.** The 2026 OFC amendments (O. Reg. 87/25) are the most significant overhaul of fire safety standards in two decades — more inspections, stricter documentation, new fines. Inspectors need better tools right now. When the code changes, TapLog updates a data file.
- `OFCAssetTypes.kt` needs a review pass against CAN/ULC-S536:19 before pilot — some intervals or requirements may have changed under the 2026 amendments.
- **TapLog is building the inspection layer of the built environment.** The subscription is today's revenue. The georeferenced, inspector-verified compliance dataset that accumulates with every inspection is the long-term defensibility argument — valuable to insurers (actuarial risk), regulators (OFM compliance visibility), property managers (portfolio dashboard), municipalities (verification), and researchers.
- The data flywheel: more inspectors → more records → more valuable dataset → more demand → more inspectors. Each rotation compounds automatically.
- Never hard-delete inspection records. The audit trail is the product. GPS on every record, civic address normalized from day one. These are data architecture decisions that cost nothing now and are painful to retrofit later.
- **The inspection form is a moment of professional uncertainty** — the inspector may not remember the specific OFC requirement for an unfamiliar asset. The app that resolves that uncertainty owns the relationship. Build for the basement, not the boardroom. (LB-006)
- Level 1 inspection guidance — `checklistItems` on OFCAssetType — costs almost nothing and delivers immediate value to junior inspectors. A data addition, not a feature build.
- The AI inspection co-pilot is the defensible moat. An assistant that knows the OFC and the specific asset's history cannot be replicated by a generic tool.

---

## Module 27a — Organisation + Site hierarchy

**What we did:**
This was a combination strategy and build session. The most structural change to TapLog since the initial data model — a full entity hierarchy replacing the flat `buildingName` string on Asset.

**Strategic decisions made before building:**

*Company over individual.* Research into the Ember beachhead revealed the primary customer is a multi-inspector fire safety company (3–15 inspectors, territory coverage), not a sole operator. Field tools for companies get replaced by non-field decision-makers who pick products based on browser demos. The resulting pain — no offline, no NFC, no OFC knowledge, manual data bridging between two systems — is the displacement opportunity. Billing attaches to Organisation, not Inspector. Company tier ($199/mo flat) is the primary revenue target.

*Dashboard reframe.* A field inspector's workflow starts with reviewing the work queue and site details before entry, then scanning. "Ready to Scan" as a landing screen was wrong. The SiteListScreen is the real dashboard — sites sorted by priority, overdue counts visible, site detail for pre-entry review, then scanning.

*Vertical-specific UI confirmed.* `ui/ember/` stays Ember-specific. Anchor gets `ui/anchor/`, etc. The shared engine (NFC, Room, sync, auth) is invisible to the user. Each vertical earns its own regulatory vocabulary and inspection rhythm.

**What was built:**
- `Organisation` entity — name, city, province, subscriptionTier, subscriptionStatus
- `Site` entity — organisationId FK, name, address, city, clientName, clientPhone, contactName, contactPhone, notes. Separates the inspection company (Organisation) from the building owner (clientName).
- `Asset` migrated — `buildingName` removed, `siteId` added. Room version 2 → 3.
- `OrganisationSetupScreen` — first launch only, collects company name and city, creates org record
- `SiteListScreen` — dashboard showing all active sites with overdue counts per site
- `SiteDetailScreen` — site info card, asset list scoped to site, stats chips (asset count, overdue count)
- `SiteRegistrationScreen` — full site creation form: name, address, city, client name/phone, on-site contact name/phone, notes
- `AssetRegistrationScreen` — updated, drops buildingName field entirely, site context injected from ViewModel
- `AssetDetailScreen` — updated, shows siteName in TopAppBar subtitle
- `InspectionFormScreen` — updated, shows siteName + location in TopAppBar subtitle
- `AssetListScreen` — updated, removes buildingName reference
- `EmberViewModel` — new states: OrganisationSetup, SiteList, SiteSelected; site context tracked; resetScanState() returns to site detail if site is selected
- `SplashScreen` — added, fade in/out, triggers loadOrganisation() on complete
- `TapLogApplication` — updated with OrganisationDao and SiteDao wiring
- `SyncModels` — buildingName removed, siteId added

**Dependency fix during session:**
Room bumped from 2.6.1 to 2.8.4 — required for KSP2 compatibility with Kotlin 2.3.21 (known JVM signature issue). `isSynced` field added to Asset entity — was referenced by DAO queries but missing from the class definition.

**Import fix during session:**
`AssetDetailSource` moved to `ca.taplog.app.data` — must be imported from there in all files, not from `ca.taplog.app.ui.ember`.

**Verified on device (Pixel 10 Pro XL, API 36):**
Fresh install → org setup screen → org created → site list (empty) → add site → site detail → asset registration via NFC tap → inspection form → submit. Full hierarchy working end to end. Back navigation through all levels. Overdue highlighting on site detail stats chip.

**Key lessons:**
- The real customer is often not the person using the app. The company owner decides to buy; the field inspector decides it works. Build for the field inspector but pitch to the owner.
- "Ready to Scan" is a functional idle state, not a dashboard. Inspectors arrive at a building with context — they review before they scan. The dashboard must reflect this mental model.
- Generic asset trackers fail in the field because they're bought for their browser interface. The displacement pattern: non-field decision-maker picks a tool that looks good on a laptop; field inspector discovers no offline, no NFC, no regulatory intelligence. This is TapLog's most reachable market segment.
- Orgs and sites are local-only until Module 28 — syncing without authenticated identity creates orphaned records with no server-side owner. Deferring is architecturally correct, not lazy.
- Room migration crashes during development: skip debugging the migration on dirty data — clear app storage (Settings → Apps → TapLog → Storage → Clear storage) and start fresh. This is always faster.
- `material-icons-extended` is a separate gradle dependency — `Icons.AutoMirrored.Filled.ArrowBack` and `Icons.Default.*` will fail to resolve without it.
- The `DeficiencyWithAsset` JOIN query now spans three tables (deficiencies → assets → sites) — the join chain deepens as the data model grows. Keep JOIN queries in the DAO, not in the ViewModel.
- Billing to the Organisation, not the Inspector, changes the entire sales motion. The field inspector is the champion who proves the product. The company owner is the buyer who signs the subscription. These are different conversations.

---

## Module 27 — Tamper-evident scan log + tag lifecycle + OFC checklists

**What we did:**
Full audit trail infrastructure. Three capabilities shipped together: every NFC tap creates an insert-only `ScanEvent` record; every tag attachment or retirement creates an insert-only `TagEvent` record; all 35 OFC asset types received `checklistItems` populated against CAN/ULC-S536:19.

- `ScanEvent` entity — id, tagId, assetId, inspectorId (nullable), inspectorName, scannedAt, eventType (BROWSE/INSPECTION), isSynced. Logged on every tap regardless of whether inspection is submitted
- `TagEvent` entity — id, assetId, tagId, attachedAt, retiredAt, retiredReason (DAMAGED/LOST/REPLACED/REMOVED), retiredByInspectorId, isSynced. Insert-only
- Room version 3 → 4 with `MIGRATION_3_4` adding both tables
- `checklistItems: List<String>` added to `OFCAssetType` — all 35 types populated with 5–7 plain-language steps per asset type, CAN/ULC-S536:19-aligned
- `InspectionFormScreen` — pre-inspection checklist card displayed above the form, fully offline
- `AssetDetailScreen` — collapsible "Scan History" section + "Replace Tag" button
- `ReplaceTagDialog` — reason picker (4 options) + awaiting-scan state in ScanState machine
- Tag replacement is an atomic Room transaction via `database.withTransaction`
- Two new backend endpoints: `POST /api/v1/scan_events`, `POST /api/v1/tag_events`
- Sync order: assets → tag_events → inspections → deficiencies → scan_events
- Back navigation audit: `BackHandler` added to all screens; `imePadding()` added to OrganisationSetupScreen and SiteRegistrationScreen
- `AssetRegistrationScreen` now receives `siteName` from MainActivity for context display

**Room migration note:** MIGRATION_2_3 was retroactively patched to include `isSynced` on organisations, sites, and assets. Dev workflow always clears storage; migrations are safety net for future real users.

**Key lessons:**
- Insert-only audit entities are a design pattern, not just a business requirement. The enforcement is in the DAO — no update/delete methods, full stop.
- Every physical NFC tap should be logged before deciding what to do with it. Presence proof and inspection record are separate concerns.
- Pre-inspection checklists (Level 1 guidance) are a data addition, not a feature build. Zero connectivity, zero extra code beyond populating the data structure.
- Atomic `withTransaction` for tag replacement is non-negotiable — partial writes (asset updated but TagEvent missing) would corrupt the audit trail.
- `AwaitingReplacementTag` as a ScanState state is cleaner than a ViewModel boolean flag — it participates in the back-navigation logic naturally.

---

## Module 28 — Authenticated inspector identity

**What we did:**
Full auth stack from scratch. Inspector accounts with email verification, JWT, device registration, and a new-device challenge flow. All sync endpoints switched from API key to Bearer token.

**Backend:**
- `app/auth.py` — JWT (python-jose), bcrypt password hashing (direct — passlib removed due to Python 3.13 incompatibility with bcrypt 4.x), Resend email (dev fallback: console print)
- `app/routers/auth.py` — 6 endpoints: register, verify-email, login, register-device, refresh, resend-code
- `app/routers/organisations.py` + `sites.py` — new upsert endpoints; organisations router links inspector's organisationId on first sync
- `app/dependencies.py` — `get_current_inspector` JWT dependency replacing API key auth on all sync endpoints
- New MongoDB collections: `inspectors` (unique index on cert_number), `devices`, `pending_verifications` (TTL index on expires_at)

**Android:**
- `InspectorPreferences.kt` stripped to: `authToken`, `refreshToken`, `inspectorId`, `deviceId`. `decodeJwtClaims()` decodes JWT payload via `java.util.Base64` — no library needed. `ensureDeviceId()` generates UUID once at install, never rotates
- `AuthApiService.kt` — 6 auth endpoints + all request/response models
- `AuthInterceptor.kt` — `runBlocking` token read, Bearer injection, 401 → refresh → retry once → `clearAuth()`
- `RetrofitClient.kt` — dual clients: `authApiService` (no interceptor), `createSyncApiService(interceptor)`
- `ui/auth/` — `AuthViewModel` with sealed `AuthState` + `LoginScreen`, `RegistrationScreen`, `EmailVerificationScreen`, `NewDeviceScreen`
- `InspectionFormScreen` — inspector name/cert replaced with read-only `InspectorIdentityCard` showing JWT claims
- `EmberViewModel` — `inspectorClaims: StateFlow<InspectorClaims?>` derived from authToken flow; `inspectorId` populated on every ScanEvent and TagEvent retirement
- `MainActivity` — auth gate: token check at startup → AuthFlow or main app; `AuthFlow` composable routes auth states
- `SyncRepository` — API key removed; sync order extended: organisations → sites → assets → tag_events → inspections → deficiencies → scan_events
- `TapLogApplication.onCreate()` — calls `scheduleSyncIfNeeded()` — this was missing and caused sync to never fire after the auth rewrite
- Room v4 → v5 (MIGRATION_4_5: organisations.isSynced), v5 → v6 (MIGRATION_5_6: assets.vertical)
- `TapLogVertical` enum added to EmberModels for multi-vertical architecture

**Bugs fixed during session:**
- `passlib` incompatible with Python 3.13 + bcrypt 4.x (`__about__` attribute removed in bcrypt 4.x). Fix: remove passlib, use `bcrypt.hashpw`/`checkpw`/`gensalt` directly
- `RESEND_API_KEY` set in Railway → skipped dev fallback, attempted to send from `noreply@taplog.ca` which isn't a verified Resend domain → silent failure. Fix: use `onboarding@resend.dev` until domain verified; added error logging for Resend API responses
- Sync never fired → `TapLogApplication` had no `onCreate()`. `scheduleSyncIfNeeded()` was defined but never called at app start

**Verified on device (Pixel 10 Pro XL, API 36):**
Full v1 loop: register → email code (from Railway logs) → verify → org setup → site creation → NFC scan → asset registration → inspection → PDF report → share. Sync confirmed in MongoDB Atlas.

**Key lessons:**
- `passlib` is a convenience wrapper around bcrypt. When the wrapper breaks (Python 3.13 compat issue), the underlying API is right there and equally simple. Wrappers add fragility for minimal gain on simple use cases.
- Verified Resend domains are required for custom `from` addresses. Until verified, use `onboarding@resend.dev`. The failure is silent (httpx call succeeds with 4xx from Resend if not logged). Always log API response codes on external service calls.
- A sync trigger that is never called is the same as a sync that doesn't exist. When rewriting `TapLogApplication`, the `onCreate()` sync trigger was lost in the refactor. `ExistingWorkPolicy.KEEP` then masked the issue — the Background Task Inspector would have shown the work was never enqueued.
- Auth rewrites affect startup sequence. Any code that ran at app start before the rewrite needs to be re-verified afterward. The auth gate changed the flow in `MainActivity`; the sync scheduling in `TapLogApplication.onCreate()` needed explicit attention.

---

## Module 29 — PDF inspection reports

**What we did:**
On-demand PDF generation for every inspection record, shared via Android share sheet. Zero new dependencies.

- `PdfReportGenerator.kt` — `android.graphics.pdf.PdfDocument` canvas renderer. A4 layout (595×842 points): org name + generation date → site/client info → asset (OFC type label, location, tag ID) → inspection (date, inspector name, cert, coloured result) → deficiency list (numbered, severity-coloured, truncated at 10 with "…and N more") → footer (next due date + TapLog credit)
- `ReportRepository.kt` — generates PDF file to `getExternalFilesDir(null)/TapLog/`, wraps in `FileProvider.getUriForFile()` content URI
- `res/xml/file_paths.xml` — external-files-path configuration for FileProvider
- `AndroidManifest.xml` — `FileProvider` registered at `ca.taplog.app.fileprovider` (exported=false, grantUriPermissions=true)
- `DeficiencyDao.getByInspection()` — new query: deficiencies for a specific inspection, sorted by severity
- `EmberViewModel.shareReport()` — fetches site, org, deficiencies; calls `ReportRepository.generateAndGetUri()`; emits `Intent` on `shareReportEvent: SharedFlow<Intent>`
- `AssetDetailScreen` — "Share Report" `TextButton` on each `InspectionHistoryCard`
- `MainActivity` — `LaunchedEffect` collecting `shareReportEvent` → `Intent.createChooser` → share sheet

**Key lessons:**
- `android.graphics.pdf.PdfDocument` is sufficient for text-based reports. iText adds ~3 MB APK weight and a licensing consideration for commercial use. For v1, always default to the built-in API.
- `FileProvider` is non-negotiable for cross-app file sharing on API 24+. A `file://` URI will crash with a `FileUriExposedException`. Two files are required: `<provider>` in the manifest AND `res/xml/file_paths.xml`. Missing either causes a crash at share time — not at build time.
- `SharedFlow<Intent>` is the correct pattern for bridging a ViewModel action that needs to start an Activity. Collecting it in `LaunchedEffect(Unit)` in `EmberScanScreen` means the event is observed as long as the composable is alive.
- PDF canvas layout is manual — no auto-wrap or table engine. Plan for fixed-width columns. Long text must be explicitly truncated before drawing.

---

## Module 30 — Photo capture for deficiencies

**What we did:**
Activated `Deficiency.photoPath: String?` — the field that had been in the Room entity since the original data model. Inspectors can now photograph a deficiency at the moment of recording it. Zero new dependencies.

- `AndroidManifest.xml` — `CAMERA` permission added
- `AddDeficiencyDialog` — camera button with permission check at point of use (`ActivityResultContracts.RequestPermission()` + `ActivityResultContracts.TakePicture()`); photo written to `getExternalFilesDir()/TapLog/photos/def_<timestamp>.jpg` via existing FileProvider; 64dp thumbnail preview shown in dialog before confirming; `photoPath` set on the `Deficiency` passed to `onAdd`
- `createPhotoFile(context)` — helper creating the file and returning a `Pair<File, Uri>` (FileProvider URI)
- `DeficiencyChip` — async 48dp thumbnail via `LaunchedEffect` + `BitmapFactory.decodeFile()` with `inSampleSize = 8` on `Dispatchers.IO`
- `DeficiencyWithAssetCard` (OpenDeficienciesScreen) — same thumbnail pattern
- `DeficiencyWithAsset.photoPath` was already in the data class; `d.*` in the JOIN query already covered it — no DAO or Room changes needed
- No backend changes — `photoPath` string was already in `DeficiencySyncRequest` and `DeficiencyIn`

**Key lessons:**
- Data model decisions made early pay dividends later. `photoPath: String?` was added to `Deficiency` in the original model even before camera integration was in scope. When Module 30 arrived, the Room entity, the JOIN result, the sync request, and the backend model were all ready. The module was purely UI wiring.
- `ActivityResultContracts.TakePicture()` requires a pre-created file URI (not a directory). The pattern is: create the file first, get a FileProvider URI, pass the URI to the launcher, and on success the file contains the photo. On cancel, delete the pre-created file to avoid accumulating empty files.
- Request camera permission at the point of use — not at app launch. Users understand why the permission is needed when asked at the moment they tap the camera button.
- `BitmapFactory.Options.inSampleSize` with powers of 2 is the zero-dependency way to create thumbnails from large files. `inSampleSize = 8` reduces a 4032×3024 camera photo to ~504×378 px — well within the 48dp display budget.
- `rememberLauncherForActivityResult` works inside `AlertDialog` composables — launchers don't need to be hoisted to a screen level.

---

## Module 34 — Vertical engine + field-driven forms

**What we did:**
Architectural pivot session. Module 34 was promoted ahead of Modules 31–33 because the vertical engine is the foundational layer that all future verticals depend on. TapLog is now a vertical factory — adding a new vertical means creating a MongoDB document and a seed script, no Android code changes.

**Data model (`VerticalModels.kt`):**
- `VerticalConfig` — full per-vertical configuration: displayName, regulatoryFramework, triggerModel, roleModel, formProfile, assetTypeRegistry
- `InspectionFormProfile` — `resultOptions: List<ResultOption>` (replaced `List<String>`), `fields: List<FormField>`, `deficienciesEnabled`, `photoRequired`
- `ResultOption(code, label, action: ResultAction)` — `ResultAction` enum: NONE, REMOVE_FROM_SERVICE, NOTIFY_AUTHORITY, ISSUE_CERTIFICATE, DELIVER_REPORT. Downstream logic encoded in config, not hardcoded
- `VerticalAssetType` — `triggerConfig: TriggerConfig` (replaced `intervalMonths: Int?`); `TriggerConfig` carries type (CALENDAR/MILEAGE/ENGINE_HOURS) + appropriate interval value
- `TriggerModel` gains `MILEAGE` and `ENGINE_HOURS` values
- `VerticalRegistry` singleton — `register()`, `get()` (throws on unregistered vertical), `all()`

**Room v6 → v7 → v8:**
- `MIGRATION_6_7`: `vertical_configs` table (verticalCode PK, configJson TEXT) — stores VerticalConfig JSON blobs
- `MIGRATION_7_8`: `organisations.licensedVerticals` TEXT column (DEFAULT '["EMBER"]') — billing hook
- `VerticalConfigEntity` + `VerticalConfigDao` (upsert + getAll)

**Startup fallback chain (`TapLogApplication.initVerticalRegistry()`):**
1. Fetch `GET /api/v1/verticals` → upsert to Room → register each config
2. Fetch fails (offline) → load from `VerticalConfigDao.getAll()` → register from cache
3. Cache empty (first cold start, no connectivity) → `VerticalRegistry.register(EmberVerticalConfig.build())`
4. `verticalRegistryReady: StateFlow<Boolean>` set to true → unblocks SplashScreen

**`EmberVerticalConfig.build()`:** Translates all `OFCCategory`/`OFCAssetType` entries to `VerticalAssetType` instances with `TriggerConfig(CALENDAR, intervalMonths)`. Ember form profile: resultOptions (PASS/REQUIRES_ATTENTION/FAIL with ResultAction.NONE), single notes TEXT FormField, deficienciesEnabled=true. `OFCAssetTypes.kt` retained — not deleted until backend cache proven.

**`InspectionFormScreen` refactor:**
- All `OFCCategory`, `OFCAssetType`, `OFCAssetTypes` imports removed
- Checklist: `VerticalRegistry.get(asset.vertical).assetTypeRegistry.find { it.code == asset.assetType }?.checklistItems`
- Result selector: iterates `formProfile.resultOptions`, shows `option.label`, maps `option.code` to `InspectionResult.valueOf()` on submit
- Fields: loop over `formProfile.fields`, renders by `FieldType` (TEXT = OutlinedTextField, BOOLEAN = Checkbox, others as text fallback)
- Deficiencies: conditional on `formProfile.deficienciesEnabled`
- Ember output is identical to pre-Module-34 behaviour

**Routing:**
- `EntryEventScreen.kt` — stub: "Multi-role entry form — coming soon" + back button
- `MainActivity` `Inspecting` state: `if (roleModel == MULTI_ROLE) EntryEventScreen else InspectionFormScreen`

**Organisation model:**
- `Organisation.licensedVerticals: List<String> = listOf("EMBER")` added
- `List<String>` TypeConverter added to `Converters.kt` (Gson)
- `OrganisationSyncRequest` and `toSyncRequest()` updated to include `licensedVerticals`

**API:**
- `TapLogApiService` gains `getVerticals()` and `getVertical(code)` Retrofit declarations
- Backend implementation (tasks 9.1–9.4) completed in next session

**Amendments before apply:**
The propose-then-amend pattern was demonstrated this session. After `/opsx:propose` generated the full artifact set, a strategy session identified three structural gaps in the initial design: (1) `resultOptions: List<String>` loses downstream action semantics, (2) `intervalMonths: Int?` can't express Fleet's mileage-based trigger, (3) inspection cardinality is load-bearing and needs a design doc before being addressed. Amendments 1 and 2 were applied to `VerticalModels.kt` and `EmberVerticalConfig.kt` before `/opsx:apply`. Amendment 3 was deferred as `openspec/design-docs/inspection-cardinality.md`.

**OpenSpec:** Module 34 archived. 4 specs synced: `vertical-config` (new), `vertical-form-engine` (new), `ofc-checklists` (updated), `org-site-sync` (updated).

**Key lessons:**
- Ask "what's different between verticals?" before building the abstraction. Two hours cataloguing dissimilarities avoids two sprints of refactoring. Surface differences (labels, intervals) are easy to parameterize. Structural differences (inspection cardinality, downstream result logic, mileage-based triggers) require deliberate model choices.
- `resultOptions: List<String>` is a label system, not a behavior system. When a result should trigger a physical action (remove from service, notify authority, issue certificate), encode that in the data — not in screen code that appears later in a different vertical's UI.
- `intervalMonths: Int?` with null meaning "event-driven" is a semantic hack. A proper `TriggerConfig` with an explicit type enum is the same amount of code and carries meaning that `null` never can.
- Context files are the cheapest architectural documentation. Dropping `openspec/module-34-context.md` before the propose run produced the right proposal on the first attempt. Without it, the first run proposed adding a fire pump asset type.
- The propose-then-amend pattern is correct for large architectural modules. A full propose run surfaces the spec shape before any code is written. Amendments can then be applied to the spec artifacts before apply runs. No code written until the shape is right.
- `OFCAssetTypes.kt` must not be deleted until the backend vertical config cache is proven in production. The fallback is what keeps Ember working on cold starts with no connectivity. Delete it too early and you have a cold-start crash.

---

## Module 34 backend — Verticals endpoint + Ember seed

**What we did:**
Completed the four backend tasks deferred from Module 34's Android session:

- Added `licensed_verticals: List[str] = ["EMBER"]` to the Organisation Pydantic model; included in org upsert and API response
- Created `verticals` MongoDB collection; seeded with full Ember `VerticalConfig` JSON document — complete asset type registry, form profile with `ResultOption` + `TriggerConfig` structures, all 35 OFC types
- Implemented `GET /api/v1/verticals` — returns configs filtered by `org.licensed_verticals`; Bearer auth required
- Implemented `GET /api/v1/verticals/{code}` — returns single config or 404; Bearer auth required
- Deployed to Railway; `TapLogApplication.initVerticalRegistry()` now fetches live from backend on first launch

With these tasks complete, the vertical engine fallback chain is fully operational: backend fetch → Room cache → static Ember config. Cold starts on offline devices still work via the Kotlin fallback.

---

## Module 35 — Brand theme + app polish

**What we did:**
Full brand application session. TapLog now has a complete, coherent visual identity across all app states.

**Color.kt:**
Full TapLog brand palette defined as named constants:
- Navy scale: `TapLogNavy900` (0xFF040F1D) → `TapLogNavy50` (0xFFEBF2FA); `TapLogNavy800` is the brand primary background
- Teal scale: `TapLogTeal900` → `TapLogTeal50`; `TapLogTeal400` (0xFF1D9E75) is the primary action color; `TapLogTeal200` (0xFF5DCAA5) is the tap dot / accent
- Neutral gray scale: 900 → 50
- Semantic status: `TapLogSuccess` (=Teal400), `TapLogWarning` (amber), `TapLogDanger` (red), `TapLogInfo` (blue)
- Deficiency severity colors: critical (red), major (amber), minor (blue), observed (gray)
- Vertical cluster accent colors: life-safety coral, property purple, infrastructure blue, industrial amber

**Theme.kt:**
Material3 `lightColorScheme` and `darkColorScheme` populated from the brand palette:
- Light: Navy800 as primary, Teal400 as secondary, White surface
- Dark: Teal200 as primary (bright on dark), Teal400 as secondary, Navy900/800 as background/surface
- `TapLogTheme` composable wires up both schemes via `isSystemInDarkTheme()`

**Type.kt:**
`TapLogTypography` defined and bound to `MaterialTheme.typography` in `TapLogTheme`.

**SplashScreen.kt — Option A brand lockup:**
- Canvas-drawn `drawTapLogIcon()`: navy rounded-rect background, three NFC arcs (opacity 100/45/20%), teal phone body, teal-deep screen, three log lines (full/60%/35% opacity), home bar, tap dot circle
- Animated entrance: icon scale 0.85→1 + alpha 0→1 (500ms EaseOutCubic), wordmark fade 400ms, tagline fade 350ms — all staggered with `delay()`
- Wordmark: "**Tap**" bold white + "Log" normal teal, 40sp, letterSpacing -1.5sp
- Tagline: "TAP. LOG. DONE." 11sp, 2sp letterSpacing, gray muted
- Ready state: "tap to continue" + teal dot fade in once `registryReady` is true
- Brand colors hardcoded as private constants — MaterialTheme not available when splash renders

**MainActivity.kt:**
- `Crossfade(targetState = screen, animationSpec = tween(400), label = "splash_to_app")` wraps the top-level screen slot
- Eliminates the hard cut between splash and app content

**Key lessons:**
- Hardcode brand colors on the splash screen. The splash renders before `MaterialTheme` is set up — using `MaterialTheme.colorScheme.*` on the splash would require the theme to be initialized first, which defeats the purpose. Private constants at the top of the file is the correct pattern.
- `Crossfade` between top-level screen states is the right tool for a splash-to-app transition — it handles the animation automatically and the code stays readable. `tween(400)` is the right duration.
- Canvas-based icon drawing (not PNG, not Drawable, not vector XML) gives perfect density-independent sharpness and opens the door for icon animation in a future session.
- A complete color system has three layers: named brand colors (the palette), semantic tokens (success/warning/error), and theme bindings (light/dark schemes). Keep these as three separate files/sections — mixing them creates maintenance complexity.

---

## Module 36 — Site map (Mapbox)

**What we did:**
Geocoding and map view for the site dashboard. Inspectors can now toggle between list and map view of their sites. Sites are geocoded at save time; existing sites are geocoded lazily on first map open.

**Room v8 → v9:**
- `MIGRATION_8_9`: `ALTER TABLE sites ADD COLUMN latitude REAL` + `ALTER TABLE sites ADD COLUMN longitude REAL`
- `Site` entity gains `latitude: Double? = null` and `longitude: Double? = null`
- `AppDatabase` version bumped to 9

**GeocodingRepository.kt (new):**
- `geocode(address, city, province): Pair<Double, Double>?` — Mapbox Geocoding REST API v5 endpoint
- Parameters: `country=CA&limit=1&access_token=...`
- Runs on `Dispatchers.IO` via `URL(url).readText()` — no new dependencies (OkHttp already in project)
- Returns null on any error (no connectivity, API error, zero results) — non-fatal
- Token guard: skips if token is blank or placeholder value

**EmberViewModel updates:**
- `saveSite()` — after Room insert, launches background coroutine to geocode and upsert with coords if successful
- `geocodeUnresolvedSites(sites)` — filters sites with null lat, geocodes sequentially, upserts each on success
- `GeocodingRepository` injected via constructor, wired in `TapLogApplication`

**SiteMapView.kt (new):**
- `MapboxMap` composable with `MapEffect(Unit)` loading `Style.DARK`
- Camera initialized via `rememberMapViewportState`: centroid of geocoded sites at zoom 10, or Ontario fallback (`-79.3832, 43.6532`) at zoom 7
- `ViewAnnotation` per geocoded site, positioned at `Point.fromLngLat(longitude, latitude)`, `allowOverlap(false)`
- `SitePin` composable: `Card` with site name (maxLines=1, ellipsis) + `Canvas` triangle pointer; `Modifier.clickable { onSiteSelected(site) }`
- Unmapped site count shown in `Alignment.BottomCenter` when any sites lack coords

**SiteListScreen updates:**
- `ViewMode` enum: `LIST` | `MAP`
- `var viewMode by remember { mutableStateOf(ViewMode.LIST) }` — persists within session
- List/map icon toggle buttons in `TopAppBar` trailing content; active button tinted `primary`, inactive `onSurfaceVariant`
- MAP branch: `LaunchedEffect(Unit)` calls `onGeocodeUnresolved(sites)` once on first map open; renders `SiteMapView`
- FAB hidden in MAP mode

**Sync model update:**
- `SiteSyncRequest` gains `latitude: Double?` and `longitude: Double?`
- `Site.toSyncRequest()` passes coords through
- Backend `SiteIn` Pydantic model updated to accept optional lat/lng (passed through `model_dump()`)

**Mapbox SDK setup:**
- `mapbox-maps = "11.12.0"` and `mapbox-compose = "11.12.0"` in `libs.versions.toml`
- Private Mapbox Maven repo in `settings.gradle.kts` — requires `MAPBOX_DOWNLOADS_TOKEN=sk.*` in `~/.gradle/gradle.properties`
- `MAPBOX_PUBLIC_TOKEN=pk.*` in `local.properties`, injected via `buildConfigField` and `manifestPlaceholders`
- **Token fix:** manifest `<meta-data android:name="com.mapbox.token">` approach does not reliably supply the token to the Compose SDK at runtime. Fix: `MapboxOptions.accessToken = BuildConfig.MAPBOX_PUBLIC_TOKEN` in `TapLogApplication.onCreate()` before any map code runs. This is the correct pattern for all future Mapbox work.

**OpenSpec:** Module 36 archived. Specs synced: `site-map-view` (new), `site-geocoding` (new), `org-site-sync` (updated with lat/lng).

**Key lessons:**
- The Mapbox manifest `<meta-data>` token injection is unreliable with the Compose extension in SDK v11. The SDK's own error message offers two options — option 2 (`MapboxOptions.accessToken` programmatic setter in Application.onCreate) is the correct one. Don't rely on manifest injection for Mapbox.
- Geocoding should be non-blocking and non-fatal. Save the site to Room immediately; geocode in a background coroutine that optionally upserts coordinates. The inspector never waits. A failed geocode is not a failed save.
- `ViewAnnotation` inside `MapboxMap` is the correct Compose SDK pattern for rendering UI at map coordinates. It participates in Compose state naturally — the same `Site` object drives both the list view and the map pin.
- `LaunchedEffect(Unit)` for the one-time geocode trigger on first map open — not `LaunchedEffect(viewMode)` which would retrigger on every toggle back to map mode.
- The manifest approach for SDK tokens is a common gotcha across multiple SDK vendors. When an SDK throws at init with an error about a missing token, check for both the manifest and a programmatic setter option. The programmatic setter is always more reliable.

---

## Module 39 — Dashboard shell

**What we did:**
Replaced `SiteListScreen` as the home screen with a purpose-built `DashboardScreen`. The app now opens to a summary view that gives inspectors immediate situational awareness before they enter a building.

**ViewModel additions:**
- `ScanState.Dashboard` added to sealed class; `loadOrganisation()`, `saveOrganisation()`, and `resetScanState()` all emit `Dashboard` as the home state
- `DashboardStats` data class: `inspectionsThisMonth`, `openDeficiencies`, `overdueSiteCount`, `totalSites`
- `SiteWithOverdueCount` data class: `site`, `overdueCount`, `mostOverdueDays`, `mostOverdueAssetName`
- `dashboardStats: StateFlow<DashboardStats>` — combines `sitesForOrg`, `openDeficienciesWithAsset`, `activeAssets`, and `allInspections` flows; inspections this month computed by comparing `inspectedAt` to start-of-month epoch
- `overdueSites: StateFlow<List<SiteWithOverdueCount>>` — groups overdue assets by `siteId`, builds `SiteWithOverdueCount` per site, sorted by `mostOverdueDays` descending
- `showDashboard()` replaces `showSiteList()` as the primary home navigation call (both coexist)
- `ScanState.SiteSelected` gains `fromSiteList: Boolean = false` — origin flag for back navigation

**DashboardScreen.kt (new):**
- `LazyColumn` root with `TopAppBar` showing org name
- `StatsStrip` — `Row` of four `StatCard` composables (`TapLogNavy700` background, `TapLogTeal200` accent number, label beneath)
- `QuickActionsSection` — full-width Scan `Button` (`TapLogTeal400`); secondary row with "Add Site" and "Deficiencies" `OutlinedButton`s
- Overdue section — `SectionHeader` with error badge count; `SiteOverdueRow` per item (site name, overdue count badge, most overdue asset, days overdue); "All sites are current" empty state
- Sites preview — up to 4 `SiteCard`s inline; "See all" `TextButton` navigates to full-screen `SiteListScreen`

**Navigation fixes:**
- Scan button on Dashboard routes to `SiteList` (not `resetScanState()`) — user picks a site before scanning
- `SiteDetailScreen` back navigation is origin-aware: `fromSiteList = true` → back to `SiteList`; `fromSiteList = false` (default, from Dashboard) → back to `Dashboard`

**SiteListScreen updates:**
- `isEmbedded: Boolean = false` parameter — suppresses FAB when true
- `onBack: (() -> Unit)?` — shows back arrow in TopAppBar when non-null

**Validation:** Build passing, Dashboard loads as home after login, stats strip correct, "See all" → SiteList → back → Dashboard. Overdue section and overdue site row tap TBD pending real overdue data in the field.

**Key lessons:**
- A home screen built around situational awareness (what's overdue, what's urgent) is more useful to a field inspector than a plain list. The list is still there — it's just one tap away.
- StateFlow `combine()` across multiple Room flows is the right pattern for derived dashboard stats — the result updates automatically when any source changes.
- Origin-aware back navigation (`fromSiteList` flag on `ScanState.SiteSelected`) is the minimal solution when a detail screen is reachable from multiple parents. Avoid a shared "previous state" tracker — flag the destination, not the journey.
- A Scan button that silently loops back to the same screen (because `resetScanState()` with no site = Dashboard) violates the principle of least surprise. Visible navigation beats invisible state mutation.

---

## Module 40 — Dashboard intelligence

**What we did:**
Added the three intelligence sections that turn the dashboard from a navigation portal into a professional briefing tool: a month calendar with inspection due date dots, a unified task list, and a searchable contacts list. All data derived from existing Room entities — no migrations, no backend changes.

**New data classes and enums (EmberViewModel):**
- `CalendarUrgency` enum: UPCOMING / DUE_SOON / OVERDUE
- `CalendarEvent`: assetId, assetName, siteName, assetTypeCode, dueDate: LocalDate, urgency
- `TaskType` enum: OPEN_DEFICIENCY / OVERDUE_ASSET
- `Task`: id, type, title, siteName, severity?, daysOverdue?, createdAt
- `SiteContact`: name, phone?, role, siteName, siteId

**DAO addition:**
- `AssetWithSite` JOIN result (non-entity): `@Embedded asset: Asset` + `@ColumnInfo siteName`
- `AssetDao.getAssetsWithDueDates()` — JOIN with sites, filtered to `nextInspectionDue IS NOT NULL`
- `EmberRepository.getAssetsWithDueDates()` wired through

**ViewModel StateFlows:**
- `calendarEvents: StateFlow<Map<LocalDate, List<CalendarEvent>>>` — derived from `getAssetsWithDueDates()`; urgency classified by comparing due date to today (OVERDUE: past, DUE_SOON: ≤7 days, UPCOMING: beyond); grouped by `LocalDate`
- `tasks: StateFlow<List<Task>>` — combines `openDeficienciesWithAsset` + `activeAssets`; explicit `nextInspectionDue < now` filter on assets before mapping; sorted CRITICAL → HIGH → OVERDUE_ASSET (days desc) → MEDIUM → LOW; uses `buildingName` from `DeficiencyWithAsset` as site name (pre-existing JOIN field)
- `allContacts: StateFlow<List<SiteContact>>` — derived from `sitesForOrg`; up to 2 contacts per site; deduplication by name+phone; alphabetical sort
- `ScanState.Calendar`, `ScanState.Tasks`, `ScanState.Contacts` added; `showCalendar()`, `showTasks()`, `showContacts()`, `selectSiteById()` added

**New screens:**
- `CalendarScreen.kt` — `TopAppBar` + `LazyColumn`; `MonthCalendarGrid` composable (7-column grid, pure Compose); `DayCell` with urgency dots (up to 3 + overflow count); day-tap expansion showing `CalendarEventRow` list; month navigation via arrow `IconButton`s
- `TasksScreen.kt` — `FilterChip` row (All · Deficiencies · Overdue); `TaskRow` composable (type icon, title, site, severity/days badge); "No open tasks ✓" empty state
- `ContactsScreen.kt` — search `OutlinedTextField`; `ContactRow` (name, role chip, site name, `ACTION_DIAL` phone tap); tap row → `selectSiteById()`

**DashboardScreen extensions:**
- `CalendarSection`, `TasksSection`, `ContactsSection` appended to existing `LazyColumn`
- `CalendarSection` has local `displayedMonth` + `selectedDay` state (month navigation works in preview too)
- `.imePadding()` added to dashboard `LazyColumn` — keyboard from Contacts search no longer overlaps content below

**Two bugs fixed post-build:**
1. `CalendarSection` had no-op `onPrevMonth`/`onNextMonth` — added local state so arrows work on dashboard
2. Keyboard covered task list — added `imePadding()` to dashboard `LazyColumn`

**Key lessons:**
- `DeficiencyWithAsset.buildingName` is the site name (sourced from `sites.name` via JOIN) — read existing JOIN results before adding new DAO queries. The field name is legacy but the data is correct.
- Explicit `nextInspectionDue < now` filter must be in the ViewModel, not assumed. `activeAssets` is all non-retired assets — without the filter, every asset becomes a task.
- A `Map<LocalDate, List<CalendarEvent>>` StateFlow gives O(1) day-cell lookup. Computing it once in the ViewModel and caching via `stateIn` is far better than filtering a list on every cell recomposition in a 30+ cell grid.
- Preview sections in a scroll view need their own local state if they have interactive controls (month arrows). Passing no-op lambdas renders the controls but silently breaks them.
- `imePadding()` belongs on the scrollable container, not the `Scaffold`. The `Scaffold` provides inset padding for system bars; `imePadding()` on the `LazyColumn` makes the keyboard push the scroll viewport without disrupting the `TopAppBar`.

---

## What comes next (upcoming modules)

| # | What | Notes |
|---|---|---|
| **37** | Anchor config — MongoDB seed | Backend-only; context written ✅ |
| **31** | Visual Asset Identification | Photo → Claude vision → `VerticalAssetType` suggestion → pre-selects picker; context written ✅ |
| **32** | Pre-Inspection Checklist card | `ChecklistCard` composable in `InspectionFormScreen`; tap-to-check UX; `checklistItems` data already populated; context written ✅ |
| **33** | AI co-pilot | Floating `?` FAB on `InspectionFormScreen` → `ModalBottomSheet` chat; system prompt generated from `VerticalConfig` + `VerticalAssetType` at runtime; context written ✅ |
| **41** | Billing | Stripe web checkout, subscription gating on Organisation — before OAFC November 2026 |

**Platform (deferred):**
- **Module 38** — Hatch config + EntryEventScreen implementation (deferred; Anchor first)
- **Onboarding v2** — Invite token model: join code on registration, V2 admin portal (browser-based)

---

## Running glossary

| Term | Plain meaning |
|---|---|
| NFC | Near Field Communication — tap your phone to a sticker-sized chip to exchange data |
| RFID | Radio Frequency Identification — broader family; NFC is the short-range tap variant |
| Room | Android's local database library — stores inspection records offline on-device |
| SQLite | The actual database engine Room sits on top of — built into every Android device |
| KSP | Kotlin Symbol Processing — generates Room boilerplate code at compile time |
| DataStore | Android's modern key-value storage library — replaces SharedPreferences |
| Retrofit | Android HTTP client library — turns an interface into API calls |
| WorkManager | Android library for reliable background tasks — handles connectivity constraints and retry backoff |
| Coroutines | Kotlin's way of doing background work without freezing the UI |
| ViewModel | Holds UI state so it survives screen rotations and lifecycle events |
| Composable | A Kotlin function annotated with @Composable that describes a piece of UI |
| Entity | A Room-annotated data class that maps to a database table |
| DAO | Data Access Object — an interface where you define your database queries |
| TypeConverter | A function that maps a Kotlin type (like an enum) to something Room can store |
| StateFlow | A Kotlin Flow that always holds a current value — used to drive Compose UI state |
| Sealed class | A Kotlin class with a fixed set of subclasses — perfect for modelling UI state machines |
| LazyColumn | Compose's efficient scrollable list — only renders visible items |
| LazyRow | Compose's efficient horizontal scrollable list — used for category chip rows |
| LaunchedEffect | A Compose side-effect that runs a coroutine when its key changes |
| FilterChip | A Compose component for selectable options — used for category selection in the OFC picker |
| imePadding | Compose modifier that adds padding equal to the keyboard height — prevents keyboard covering form fields |
| Repository | A class that abstracts data access — ViewModels talk to it, not directly to DAOs |
| Application class | Android class instantiated once at app launch — holds app-wide singletons |
| Vertical | A trade-specific version of TapLog (Ember = fire safety, Newel = home inspection, etc.) |
| Beachhead | The first vertical you launch — proves the model before expanding |
| Offline-first | The app works fully without internet; syncs when connection is available |
| UUID | Unique identifier — used as primary key, safe for distributed/offline systems |
| Pre-loss | Inspection records created before a damage or insurance event occurs |
| isSynced | Boolean flag on records — false means queued for upload |
| Foreground dispatch | NFC mode where your app gets first priority on tag taps while open |
| ScanState | Sealed class representing every possible state of the NFC scan flow |
| AssetDetailSource | Enum tracking how user arrived at AssetDetailScreen — FROM_SCAN or FROM_LIST — lives in ca.taplog.app.data |
| DeficiencyWithAsset | Non-entity data class — JOIN result combining deficiency fields with asset name and site name |
| OFCCategory | Enum of Ontario Fire Code asset categories — each carries a list of OFCAssetType |
| OFCAssetType | Data class: code (stored in Room), label, description, inspectionIntervalMonths, checklistItems |
| Motor | Async Python MongoDB driver — correct choice for FastAPI (pymongo is sync) |
| Upsert | Insert if not exists, replace if exists — idempotent database write |
| Idempotent | An operation that produces the same result whether run once or many times |
| 409 Conflict | HTTP status meaning "dependency not met, retry later" — used for sync order guards |
| Railpack | Railway's build system — requires requirements.txt to trigger pip install |
| pyproject.toml | Modern Python dependency spec — used with uv for local development |
| M0 | MongoDB Atlas free tier — 512 MB storage, no charge |
| synced_at | Epoch millis timestamp set by backend on upsert — used for provisioning queries in v2 |
| OkHttp | HTTP engine underneath Retrofit — handles connections, logging, interceptors |
| BuildConfig | Generated class holding compile-time constants — used to store API keys from local.properties |
| VerticalConfig | Full configuration for one TapLog vertical — asset types, form profile, trigger model, role model |
| VerticalRegistry | Singleton that holds registered VerticalConfig instances; populated at startup; throws on unregistered vertical |
| VerticalAssetType | A VerticalConfig's version of OFCAssetType — includes TriggerConfig instead of intervalMonths |
| TriggerConfig | Encodes how inspections are triggered: CALENDAR (months), MILEAGE, ENGINE_HOURS, or event-driven |
| InspectionFormProfile | Per-vertical form definition: resultOptions, fields, deficienciesEnabled, photoRequired |
| ResultOption | A result choice with code, display label, and ResultAction — replaces plain string result options |
| ResultAction | Enum encoding downstream behavior after a result: NONE, REMOVE_FROM_SERVICE, NOTIFY_AUTHORITY, ISSUE_CERTIFICATE, DELIVER_REPORT |
| FormField | A dynamic form input definition: key, label, FieldType, required, applicableAssetTypes |
| VerticalConfigEntity | Room entity caching a VerticalConfig as a JSON blob in the vertical_configs table |
| EmberVerticalConfig | Kotlin object with build() function — translates OFCCategory to VerticalConfig; used as cold-start fallback |
| licensedVerticals | List<String> on Organisation — billing hook controlling which vertical configs the backend returns |
| EntryEventScreen | Stub composable for MULTI_ROLE assets — placeholder for Hatch entry event flow (Module 38) |
| Inspection cardinality | The assumption that one NFC tap = one asset = one inspection form — wrong for Fleet, Hatch, Anchor batch. Open design doc. |
| local.properties | Gitignored file at project root — safe place for secrets like API keys |
| SyncResult | Sealed class with Success / Conflict / Failure — clean return type for sync operations |
| SyncWorker | WorkManager CoroutineWorker that calls syncAll() when connectivity is available |
| BAL | Background Activity Launch — Android 16 security restriction limiting which processes can launch activities |
| FLAG_IMMUTABLE | PendingIntent flag required on API 34+ for security — intent cannot be modified after creation |
| TagEvent | Room entity recording tag lifecycle — attach date, retire date, reason, inspector. Insert-only. |
| TagRetiredReason | Enum: DAMAGED / LOST / REPLACED / REMOVED — reason a tag was retired from an asset |
| Tag lifecycle | The principle that tags are replaceable pointers to assets, not permanent identities — history survives replacement |
| checklistItems | List of plain-language inspection steps on OFCAssetType — displayed before inspection form, offline, no build cost |
| AI co-pilot | Planned in-app AI assistant: OFC-aware, asset-context-aware, offline-cached Q&A — the defensible moat for inspection guidance |
| Pre-inspection briefing | Building-level summary generated before a walkthrough: overdue assets, open deficiencies, OFC changes since last visit |
| Data asset | The georeferenced, inspector-verified compliance dataset that accumulates with every inspection — valuable to insurers, regulators, property managers, and researchers |
| Data flywheel | More inspectors → more records → more valuable dataset → more demand → more inspectors — compounds automatically |
| CAN/ULC-S536:19 | Updated Ontario fire alarm inspection standard effective January 2026 — stricter, more documentation, no more sampling |
| O. Reg. 87/25 | Ontario regulation enacting the 2026 OFC amendments — most significant overhaul in two decades |
| AMP | Administrative Monetary Penalty — new enforcement tool giving municipalities power to issue on-the-spot fines for OFC non-compliance |
| OFM | Office of the Fire Marshal — Ontario body that licenses fire safety inspectors and enforces the Fire Code |
| OpenSpec | Spec-driven development framework — propose → apply → archive workflow for AI coding sessions |
| AGENTS.md | File in `.claude/` read automatically by Claude Code at session start — contains prime directives |
| SDD | Spec-Driven Development — agree on what to build before any code is written |
| ScanEvent | Insert-only Room entity recording every NFC tap — independent proof-of-presence audit trail |
| ScanEventType | Enum: INSPECTION (tap led to inspection) / BROWSE (tap without inspection submitted) |
| JWT | JSON Web Token — signed token carrying inspector identity, issued on login, sent on every API request |
| AuthInterceptor | OkHttp interceptor that injects Bearer token on every request and handles 401 → refresh → retry |
| deviceId | UUID generated once at app install, stored permanently in DataStore — identifies the physical device |
| pending_verifications | MongoDB collection storing 6-digit email codes with 15-minute TTL — used for account and device verification |
| Resend | Transactional email service used for verification codes — generous free tier, simple Python SDK |
| Bearer token | Authorization header format — `Authorization: Bearer <JWT>` — replaces API key on sync endpoints post-Module 28 |
| Corridor-first | Go-to-market strategy: own the Bancroft–Peterborough–Belleville corridor before expanding provincially |
| Activation gap | The gap between downloading the app and completing a real first inspection — for TapLog, caused by lack of NFC tags |
| OAFC | Ontario Association of Fire Chiefs — runs annual conference and trade show (November, Niagara Falls) |
| Life safety cluster | Ember + Anchor + Hatch — the three highest-urgency regulated trade verticals, built in sequence |
| Organisation | Top-level entity — the fire safety company using TapLog. Billing attaches here, not to the inspector. |
| Site | A physical location belonging to an Organisation — has its own address, client name, on-site contact, and now lat/lng coords. |
| Generic tracker displacement | The pattern where a field tool is replaced by a cloud-only generic asset tracker by non-field decision-makers — no offline, no NFC, no regulatory knowledge. TapLog's most reachable competitive target. |
| Company tier | TapLog's $199/mo flat subscription — up to 15 inspectors, reporting dashboard. Primary revenue target for multi-inspector fire safety companies. |
| InspectorClaims | Data class decoded from JWT payload on-device — inspectorId, name, email, certNumber, organisationId. Not stored in DataStore. |
| AuthState | Sealed class in AuthViewModel — Login, Registering, VerifyingEmail, LoggingIn, NewDevice, Authenticated |
| AuthFlow | Composable in MainActivity that routes between auth screens based on AuthState |
| RetireReason | Enum: DAMAGED / LOST / REPLACED / REMOVED — reason a tag was retired from an asset |
| TapLogVertical | Enum: EMBER, ANCHOR, HATCH, NEWEL, MAST, CRANE, SEAM, SPAN — vertical stored on Asset for multi-vertical routing |
| PdfDocument | Android built-in (API 19+) PDF generation API — canvas-based, no external dependencies needed |
| FileProvider | AndroidX component for sharing app-private files with other apps via content:// URI — requires manifest registration + res/xml/file_paths.xml |
| SharedFlow | Kotlin Flow variant for one-shot events — used for shareReportEvent in EmberViewModel to bridge ViewModel → Activity |
| InspectorIdentityCard | Read-only composable in InspectionFormScreen showing inspector name + cert from JWT claims — replaces editable fields post-Module 28 |
| ExistingWorkPolicy.KEEP | WorkManager policy — if a named work task is already queued, new enqueue requests are ignored. Can mask "sync never fires" bugs if the trigger was only called once. |
| ActivityResultContracts.TakePicture | Compose/Activity result contract for camera capture — takes a pre-created FileProvider URI, writes the photo there, returns true/false |
| ActivityResultContracts.RequestPermission | Result contract for requesting a single runtime permission — returns true if granted |
| BitmapFactory.Options.inSampleSize | Power-of-2 downsampling factor for BitmapFactory.decodeFile — use 8 for 48dp thumbnails from camera photos |
| photoPath | String? field on Deficiency — local file path to a captured photo. Path string syncs to backend; binary upload is a future enhancement |
| Mapbox | Cloud mapping platform — provides maps, geocoding REST API, and the Android Compose SDK used for TapLog's site map view |
| GeocodingRepository | Calls Mapbox Geocoding REST API v5 to resolve a site address to lat/lng coordinates; non-blocking, non-fatal |
| SiteMapView | Compose composable rendering a MapboxMap with ViewAnnotation pins per geocoded site; includes SitePin label + triangle |
| ViewAnnotation | Mapbox Compose SDK component for rendering Compose UI at a specific map coordinate — used for site name pins |
| MapEffect | Mapbox Compose SDK side-effect for imperative map operations (e.g., loading a style on the MapView) |
| MapboxOptions.accessToken | Programmatic Mapbox token setter — must be called in Application.onCreate() before any map code runs; manifest meta-data approach unreliable in SDK v11 |
| ViewMode | Enum in SiteListScreen — LIST or MAP; controls which view is displayed; persists within the session |
| MAPBOX_DOWNLOADS_TOKEN | Secret Mapbox token (sk.*) for downloading SDK from the private Mapbox Maven repo; set in ~/.gradle/gradle.properties, never committed |
| TapLog brand palette | Navy (primary background/text), Teal (primary action/accent), Gray (neutral), semantic status, deficiency severity, vertical cluster colors — defined in Color.kt |
| TapLogTheme | Material3 theme composable — applies brand palette to light and dark color schemes; wired in MainActivity |
| Crossfade | Compose animation composable — used for 400ms fade between AppScreen.Splash and AppScreen.App in MainActivity |
| Invite token model | Planned onboarding mechanism: org owner generates a join code, inspector uses it on registration to join the correct org account |

---

## Key files reference

### Android
| File | Purpose |
|---|---|
| `ca.taplog.app/MainActivity.kt` | Entry point, NFC handling, auth gate, EmberScanScreen, Crossfade splash→app |
| `ca.taplog.app/TapLogApplication.kt` | App singleton — all DAOs, repository, inspectorPreferences, syncRepository, geocodingRepository; MapboxOptions token set here |
| `ca.taplog.app/SyncWorker.kt` | WorkManager CoroutineWorker — calls syncAll(), handles retry/success |
| `ca.taplog.app/ConnectivityReceiver.kt` | BroadcastReceiver — triggers sync on connectivity restore |
| `ca.taplog.app.data/EmberModels.kt` | Organisation, Site (+ lat/lng), Asset, Inspection, Deficiency, DeficiencyWithAsset, ScanEvent, TagEvent entities + all enums |
| `ca.taplog.app.data/Converters.kt` | TypeConverters for all Room enums + List<String> |
| `ca.taplog.app.data/EmberDAO.kt` | OrganisationDao, SiteDao, AssetDao, InspectionDao, DeficiencyDao, ScanEventDao, TagEventDao |
| `ca.taplog.app.data/VerticalModels.kt` | Full vertical data model + VerticalRegistry singleton |
| `ca.taplog.app.data/VerticalConfigDao.kt` | VerticalConfigEntity + VerticalConfigDao |
| `ca.taplog.app.data/AppDatabase.kt` | Room singleton, version 9, migrations 2→3→4→5→6→7→8→9 |
| `ca.taplog.app.data/EmberRepository.kt` | Data access layer including replaceTag() atomic transaction |
| `ca.taplog.app.data/InspectorPreferences.kt` | DataStore: authToken, refreshToken, inspectorId, deviceId + decodeJwtClaims() + InspectorClaims |
| `ca.taplog.app.data/AuthApiService.kt` | Retrofit interface for 6 auth endpoints + all auth models |
| `ca.taplog.app.data/AuthInterceptor.kt` | Bearer injection + 401 refresh retry + clearAuth |
| `ca.taplog.app.data/GeocodingRepository.kt` | Mapbox Geocoding REST API — address → lat/lng; non-blocking, non-fatal |
| `ca.taplog.app.data/PdfReportGenerator.kt` | PdfDocument canvas renderer — A4 inspection report |
| `ca.taplog.app.data/ReportRepository.kt` | PDF file generation + FileProvider URI |
| `ca.taplog.app.ui.auth/AuthViewModel.kt` | Auth state machine |
| `app/src/main/res/xml/file_paths.xml` | FileProvider path config — required for share sheet |
| `ca.taplog.app.data/OFCAssetTypes.kt` | OFCCategory enum + OFCAssetType data class — all 35 OFC asset types with descriptions |
| `ca.taplog.app.data/SyncModels.kt` | Wire-format request/response models + toSyncRequest() extensions (incl. lat/lng on SiteSyncRequest) |
| `ca.taplog.app.data/SyncResult.kt` | Sealed class — Success / Conflict / Failure sync result types |
| `ca.taplog.app.data/RetrofitClient.kt` | Retrofit singleton with Gson naming policy and OkHttp logging |
| `ca.taplog.app.data/TapLogApiService.kt` | Retrofit interface — verticals GETs + sync POSTs |
| `ca.taplog.app.data/SyncRepository.kt` | Per-record and batch sync methods, 409 handling |
| `ca.taplog.app.ui.ember/EmberViewModel.kt` | ScanState machine, DashboardStats + SiteWithOverdueCount StateFlows, org/site/asset context, all Ember business logic, geocodeUnresolvedSites() |
| `ca.taplog.app.ui.ember/DashboardScreen.kt` | Home screen — StatsStrip, QuickActionsSection, overdue sites, site preview, CalendarSection, TasksSection, ContactsSection |
| `ca.taplog.app.ui.ember/CalendarScreen.kt` | Full-screen month calendar — urgency dots, day-tap expansion, month navigation |
| `ca.taplog.app.ui.ember/TasksScreen.kt` | Unified task list — open deficiencies + overdue assets; FilterChip row |
| `ca.taplog.app.ui.ember/ContactsScreen.kt` | Aggregated site contacts — search, tap-to-dial, row → SiteDetailScreen |
| `ca.taplog.app.ui.ember/OrganisationSetupScreen.kt` | First launch org creation |
| `ca.taplog.app.ui.ember/SiteListScreen.kt` | Full-screen site list (from Dashboard "See all") — list/map toggle, lazy geocoding |
| `ca.taplog.app.ui.ember/SiteMapView.kt` | MapboxMap composable + ViewAnnotation site pins + SitePin |
| `ca.taplog.app.ui.ember/SiteDetailScreen.kt` | Site detail + asset list + stats chips |
| `ca.taplog.app.ui.ember/SiteRegistrationScreen.kt` | Add new site |
| `ca.taplog.app.ui.ember/AssetRegistrationScreen.kt` | Register new NFC tag — OFC picker, auto inspection interval, site context |
| `ca.taplog.app.ui.ember/AssetDetailScreen.kt` | Asset info + inspection history, source-aware back navigation |
| `ca.taplog.app.ui.ember/AssetTypePickerDialog.kt` | OFC asset type picker — search, category chips, descriptions |
| `ca.taplog.app.ui.ember/AssetListScreen.kt` | Browse all assets, overdue highlighting |
| `ca.taplog.app.ui.ember/InspectionFormScreen.kt` | Inspection form + deficiency dialog — field-driven via VerticalConfig |
| `ca.taplog.app.ui.ember/OpenDeficienciesScreen.kt` | All open deficiencies, mark resolved |
| `ca.taplog.app.ui.ember/SplashScreen.kt` | Option A brand lockup: Canvas icon, animated entrance, tap-to-continue gate |
| `ca.taplog.app.ui.theme/Color.kt` | Full TapLog brand palette |
| `ca.taplog.app.ui.theme/Theme.kt` | TapLogTheme: Material3 light + dark color schemes |
| `ca.taplog.app.ui.theme/Type.kt` | TapLogTypography |
| `app/src/main/AndroidManifest.xml` | NFC permissions, windowSoftInputMode, ConnectivityReceiver, app config |
| `app/build.gradle.kts` | App-level dependencies, KSP config, BuildConfig fields |
| `gradle/libs.versions.toml` | Version catalog |

### Backend
| File | Purpose |
|---|---|
| `app/main.py` | FastAPI app entry point, lifespan, router registration |
| `app/database.py` | Motor client, Settings, connect/close/get_db |
| `app/models.py` | All Pydantic request/response models and enums (SiteIn includes lat/lng) |
| `app/routers/assets.py` | POST /api/v1/assets |
| `app/routers/inspections.py` | POST /api/v1/inspections |
| `app/routers/deficiencies.py` | POST /api/v1/deficiencies |
| `app/routers/sites.py` | POST /api/v1/sites (accepts lat/lng) |
| `app/routers/organisations.py` | POST /api/v1/organisations |
| `app/routers/verticals.py` | GET /api/v1/verticals, GET /api/v1/verticals/{code} |
| `Procfile` | Railway start command |
| `pyproject.toml` | uv-compatible dependency spec |
| `requirements.txt` | Railpack pip install trigger |

### Project docs & tooling
| File | Purpose |
|---|---|
| `taplog-spec.md` | Living technical spec — stack, data model, file map, roadmap |
| `taplog-curriculum.md` | Full build log — every module documented with lessons |
| `taplog-investor-summary.md` | Pitch document — traction section updated each session |
| `taplog-lightbulbs.md` | Product insight moments captured during the build |
| `taplog-pilot-tester-pool.md` | Prospective pilot users — real relationships, handle with care |
| `taplog-md-prime-directives.md` | Session workflow rules — source of truth for AGENTS.md |
| `.claude/AGENTS.md` | Auto-read by Claude Code — copy of prime directives |
| `openspec/config.yaml` | OpenSpec configuration |
| `openspec/module-34-context.md` | Architectural context for vertical engine (reference) |
| `openspec/module-36-context.md` | Architectural context for site map / Mapbox (reference) |

---

*Session 1 complete — NFC reading working, Ember data model defined, Room dependencies added*
*Session 2 complete — Full scan → register → detail → inspect → submit flow working on device*
*Session 3 complete — Inspector profile (DataStore), asset list screen, open deficiencies screen, deficiency crash fix, keyboard/scroll fixes*
*Session 4 complete — FastAPI backend live on Railway, MongoDB Atlas connected, three sync endpoints verified end-to-end*
*Session 5 complete — Retrofit HTTP client, SyncRepository, WorkManager SyncWorker, end-to-end sync verified on device, Android 16 NFC BAL fix*
*Session 6 complete — OFC asset type picker (35 types, search, plain-English descriptions), auto inspection intervals, source-aware back navigation, lightbulbs document, OpenSpec installed and configured*
*Session 7 complete — Full strategy session: life safety verticals matrix, go-to-market, pricing, competitive depth, proof of presence security model, tag economics, tag lifecycle (LB-005), regulatory tailwind (2026 OFC amendments), data asset framing, inspection guidance roadmap / AI co-pilot concept (LB-006), Module 28 promoted to authenticated identity, pilot user added as Ember champion*
*Session 8 complete — Module 27a: Organisation + Site entity hierarchy, Room v2→v3 migration, SiteListScreen dashboard, SiteDetailScreen, SiteRegistrationScreen, OrganisationSetupScreen, SplashScreen, AssetRegistrationScreen/AssetDetailScreen/InspectionFormScreen updated, full hierarchy verified on device. Strategy: company-over-individual, dashboard reframe, generic tracker displacement pattern identified, vertical-specific UI confirmed*
*Session 9 complete — Modules 27, 28, 29, 30: tamper-evident scan log + tag lifecycle + OFC checklists (Room v4), full authenticated identity with JWT + device registration + org/site sync (Room v5→v6, TapLogVertical), PDF report generation with FileProvider share sheet, photo capture for deficiencies. Full Ember v1 loop verified end-to-end on device. bcrypt/passlib fix, Resend domain workaround, sync trigger fix (TapLogApplication.onCreate). Next: Module 31 — Visual Asset Identification*
*Session 10 complete — Module 34: vertical engine (VerticalConfig/VerticalRegistry, field-driven InspectionFormScreen, Room v6→v7→v8, EntryEventScreen stub, licensedVerticals, EmberVerticalConfig cold-start fallback). Propose-then-amend pattern. ResultOption + TriggerConfig amendments before apply. inspection-cardinality.md open design doc.*
*Session 11 complete — Module 34 backend (tasks 9.1–9.4: verticals endpoint + Ember seed in MongoDB). Module 35: full brand palette, Material3 light+dark themes, SplashScreen Option A brand lockup, Crossfade transition. Module 36: Mapbox site map — Room v8→v9 (lat/lng on Site), GeocodingRepository, SiteMapView + ViewAnnotation pins + SitePin, list/map toggle in SiteListScreen, Mapbox SDK v11.12.0, programmatic token fix. Onboarding governance discussion: invite token model, V2 admin portal. LB-012 (registration entry points), LB-013 (org membership count drives UI complexity). Next: Module 37 — Anchor config*
*Session 12 complete — Module 39: DashboardScreen as home (ScanState.Dashboard), DashboardStats + SiteWithOverdueCount StateFlows, StatsStrip, QuickActionsSection, overdue section, site preview, origin-aware SiteSelected back navigation (fromSiteList flag), two UX bugs fixed (Scan button routing, Go Back navigation). Build passing, core validation complete. Next: Module 40 — Dashboard intelligence*
*Session 13 complete — Module 40: CalendarSection (pure-Compose month grid, urgency dots, local state for month navigation), TasksSection (unified deficiencies + overdue assets, explicit nextInspectionDue < now filter, priority sort), ContactsSection (site contacts aggregated in ViewModel, search, tap-to-dial), three full-screen screens (CalendarScreen, TasksScreen, ContactsScreen), AssetWithSite DAO JOIN, imePadding() fix on dashboard LazyColumn. All validation passing. New tangent next.*
