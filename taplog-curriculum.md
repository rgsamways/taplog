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
**Stack:** Kotlin 2.3.21 · Jetpack Compose · Room 2.8.4 · KSP 2.3.9 · AGP 9.2.1 · DataStore 1.1.1 · room.generateKotlin=true · material-icons-extended

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

## What comes next (upcoming modules)

- **Module 31** — Visual Asset Identification: photo → AI suggests OFC asset type (depends on Module 30 ✅)
- **Module 32** — Inspection guidance Level 2: collapsible contextual OFC guidance panel on InspectionFormScreen
- **Module 33** — AI inspection co-pilot: OFC-aware, asset-context-aware, offline-cached Q&A; pre-inspection building briefing
- **Billing** — Stripe web checkout, subscription gating on Organisation — before OAFC November 2026

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
| Beachhead | The first vertical you launch — proves the model before expanding |
| Corridor-first | Go-to-market strategy: own the Bancroft–Peterborough–Belleville corridor before expanding provincially |
| Activation gap | The gap between downloading the app and completing a real first inspection — for TapLog, caused by lack of NFC tags |
| OAFC | Ontario Association of Fire Chiefs — runs annual conference and trade show (November, Niagara Falls) |
| Life safety cluster | Ember + Anchor + Hatch — the three highest-urgency regulated trade verticals, built in sequence |
| Organisation | Top-level entity — the fire safety company using TapLog. Billing attaches here, not to the inspector. |
| Site | A physical location belonging to an Organisation — has its own address, client name, and on-site contact. Replaces the old buildingName string. |
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

---

## Key files reference

### Android
| File | Purpose |
|---|---|
| `ca.taplog.app/MainActivity.kt` | Entry point, NFC handling, EmberScanScreen, all state routing |
| `ca.taplog.app/TapLogApplication.kt` | App singleton — all DAOs, repository, inspectorPreferences, syncRepository |
| `ca.taplog.app/SyncWorker.kt` | WorkManager CoroutineWorker — calls syncAll(), handles retry/success |
| `ca.taplog.app/ConnectivityReceiver.kt` | BroadcastReceiver — triggers sync on connectivity restore |
| `ca.taplog.app.data/EmberModels.kt` | Organisation, Site, Asset, Inspection, Deficiency, DeficiencyWithAsset, ScanEvent, TagEvent entities + all enums |
| `ca.taplog.app.data/Converters.kt` | TypeConverters for all Room enums |
| `ca.taplog.app.data/EmberDAO.kt` | OrganisationDao, SiteDao, AssetDao, InspectionDao, DeficiencyDao |
| `ca.taplog.app.data/AppDatabase.kt` | Room singleton, version 6, migrations 2→3→4→5→6 |
| `ca.taplog.app.data/EmberRepository.kt` | Data access layer including replaceTag() atomic transaction |
| `ca.taplog.app.data/InspectorPreferences.kt` | DataStore: authToken, refreshToken, inspectorId, deviceId + decodeJwtClaims() + InspectorClaims |
| `ca.taplog.app.data/AuthApiService.kt` | Retrofit interface for 6 auth endpoints + all auth models |
| `ca.taplog.app.data/AuthInterceptor.kt` | Bearer injection + 401 refresh retry + clearAuth |
| `ca.taplog.app.data/PdfReportGenerator.kt` | PdfDocument canvas renderer — A4 inspection report |
| `ca.taplog.app.data/ReportRepository.kt` | PDF file generation + FileProvider URI |
| `ca.taplog.app.ui.auth/AuthViewModel.kt` | Auth state machine |
| `app/src/main/res/xml/file_paths.xml` | FileProvider path config — required for share sheet |
| `ca.taplog.app.data/OFCAssetTypes.kt` | OFCCategory enum + OFCAssetType data class — all 35 OFC asset types with descriptions |
| `ca.taplog.app.data/SyncModels.kt` | Wire-format request/response models + toSyncRequest() extensions |
| `ca.taplog.app.data/SyncResult.kt` | Sealed class — Success / Conflict / Failure sync result types |
| `ca.taplog.app.data/RetrofitClient.kt` | Retrofit singleton with Gson naming policy and OkHttp logging |
| `ca.taplog.app.data/TapLogApiService.kt` | Retrofit interface — three POST sync endpoints |
| `ca.taplog.app.data/SyncRepository.kt` | Per-record and batch sync methods, 409 handling |
| `ca.taplog.app.ui.ember/EmberViewModel.kt` | ScanState machine, org/site/asset context, all Ember business logic |
| `ca.taplog.app.ui.ember/OrganisationSetupScreen.kt` | First launch org creation |
| `ca.taplog.app.ui.ember/SiteListScreen.kt` | Dashboard — site list with overdue counts |
| `ca.taplog.app.ui.ember/SiteDetailScreen.kt` | Site detail + asset list + stats chips |
| `ca.taplog.app.ui.ember/SiteRegistrationScreen.kt` | Add new site |
| `ca.taplog.app.ui.ember/AssetRegistrationScreen.kt` | Register new NFC tag — OFC picker, auto inspection interval, site context |
| `ca.taplog.app.ui.ember/AssetDetailScreen.kt` | Asset info + inspection history, source-aware back navigation |
| `ca.taplog.app.ui.ember/AssetTypePickerDialog.kt` | OFC asset type picker — search, category chips, descriptions |
| `ca.taplog.app.ui.ember/AssetListScreen.kt` | Browse all assets, overdue highlighting |
| `ca.taplog.app.ui.ember/InspectionFormScreen.kt` | Inspection form + deficiency dialog |
| `ca.taplog.app.ui.ember/OpenDeficienciesScreen.kt` | All open deficiencies, mark resolved |
| `ca.taplog.app.ui.ember/SplashScreen.kt` | Launch splash — fade in/out |
| `app/src/main/AndroidManifest.xml` | NFC permissions, windowSoftInputMode, ConnectivityReceiver, app config |
| `app/build.gradle.kts` | App-level dependencies, KSP config, BuildConfig fields |
| `gradle/libs.versions.toml` | Version catalog |

### Backend
| File | Purpose |
|---|---|
| `app/main.py` | FastAPI app entry point, lifespan, router registration |
| `app/database.py` | Motor client, Settings, connect/close/get_db |
| `app/models.py` | All Pydantic request/response models and enums |
| `app/routers/assets.py` | POST /api/v1/assets |
| `app/routers/inspections.py` | POST /api/v1/inspections |
| `app/routers/deficiencies.py` | POST /api/v1/deficiencies |
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
