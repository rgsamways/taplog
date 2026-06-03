# TapLog — Session Handoff
> Session date: 2026-06-03 (Session 16)
> Prepared for: next Claude session

---

## How to start the next session

1. Upload all md docs: `taplog-spec.md`, `taplog-curriculum.md`, `taplog-investor-summary.md`, `taplog-lightbulbs.md`, `taplog-md-prime-directives.md`, `taplog-pilot-tester-pool.md`, `taplog-session-handoff.md`
2. Read `taplog-md-prime-directives.md` first and confirm you've read it
3. **Next module options (in priority order):**
   - **Module 42** — Service request trail (highest strategic priority — the liability transfer instrument) — write `openspec/module-42-context.md` first
   - **Module 32** — Pre-Inspection Checklist card (`openspec/module-32-context.md` ✅)
   - **Module 33** — AI co-pilot (`openspec/module-33-context.md` ✅)
   - **Module 41** — Billing (before OAFC November 2026)
4. **Spec is fully merged and current:** `taplog-spec.md` includes all Session 15–16 additions
5. **Smallburg context:** TapLog is one of six apps in the Smallburg platform family (smallburg.ca). Cross-app context in Smallburg Drive folder.
6. **AI key is in local.properties** — `ANTHROPIC_API_KEY` set for Module 31 (visual asset ID). Already wired to `BuildConfig.ANTHROPIC_API_KEY` in `app/build.gradle.kts`.

---

## What was accomplished this session (Session 16 — June 3, 2026)

Massive session. Reviewed all Drive docs (TapLog, Farpost, Smallburg), merged the Session 15 expansion spec into `taplog-spec.md`, then built three modules back to back.

### Module 37 — Anchor VerticalConfig (COMPLETE ✅)
- `scripts/seed_anchor_vertical.py` — seeds Anchor (fall protection) config to MongoDB
- 6 asset types: HAR, LAN, SRL, ANC, HLL, RKT with PRE_USE (daily) + CALENDAR (annual) triggers
- No soft middle result state — PASS or REMOVE_FROM_SERVICE only
- Seeded and verified against live Atlas cluster. Zero Android changes.

### Module 43 — Field Analyst Mode (COMPLETE ✅)
Full Field Analyst operating mode — Robin's scouting business is now live in the app.

**New enums:** `UserRole` (7 values), `TagEventRole` (5 values)

**Room v9 → v10:** `registeredByRole`, `registeredByUserId`, `registeredByName`, `registeredByCertNumber` added to `assets` and `tag_events`

**DataStore:** `userRole` persisted; `isUserRoleSet` for first-launch detection; `setUserRole()`

**6 new screens:** `RoleSelectionScreen` · `FieldAnalystDashboardScreen` · `VisitSetupScreen` · `FieldAnalystScanScreen` · `QuickRegisterSheet` · `UnregisteredTagScreen`

**Key bugs fixed during testing:** `saveOrganisation()` always routed to Dashboard regardless of role; `loadOrganisation()` race condition with DataStore; wrong navigation from recent visits list

**Verified on device:** RoleSelectionScreen ✅ · FieldAnalystDashboardScreen ✅ · role persists ✅ · VisitSetupScreen ✅ · FieldAnalystScanScreen (NFC ring + FAB) ✅

### Module 31 — Visual Asset Identification (COMPLETE ✅)
Claude vision → asset type suggestion in `QuickRegisterSheet` and `AssetRegistrationScreen`.

**New:** `AiRepository.kt` — OkHttp to Anthropic API, 1024px scaling, base64, 15s timeout, null on any error

**Modified:** `AssetTypePickerDialog` (suggestedCode + "AI suggested" chip + auto-scroll) · `AssetRegistrationScreen` (camera button) · `QuickRegisterSheet` (camera button) · `VisitSetupScreen` (`.imePadding()` fix) · `file_paths.xml` (`<cache-path>` for FileProvider)

**API key:** `ANTHROPIC_API_KEY` in `local.properties` → `BuildConfig.ANTHROPIC_API_KEY`

**Key bugs fixed:** FileProvider crash (missing `<cache-path>`); camera permission revoked by `pm clear` (added runtime request); wrong navigation from recent visits

**Verified on device:** smoke alarm correctly identified by Claude ✅ · "AI suggested" chip appeared ✅

### Other
- `build.gradle.kts` — `ANTHROPIC_API_KEY` BuildConfig field; `jniLibs.useLegacyPackaging = false`
- `AndroidManifest.xml` — `android:extractNativeLibs="false"` (16KB page compatibility)
- `DataStore` 1.1.1 → 1.1.4; `.gitignore` — `local.properties` entry added
- `taplog-spec.md` fully merged with Session 15 expansion (all 7 user roles, entities, Modules 42–50 roadmap)

---

## What was accomplished this session

**Session 15 (June 3, 2026)** — Platform expansion. No code written. Deep design session.

**Session 13 (June 1, 2026)** covered Module 40 (Dashboard intelligence — complete ✅), 19 new lightbulbs (LB-014–032), vertical bundle concept (TapLog Adit), taplog.ca homepage, design system extraction, and Farpost platform family strategy.

---

### Session 15 — Platform Role Expansion (COMPLETE ✅ — spec only)

Emerged from a conversation about a trailer park in Ontario and a friend going to Ecuador for two years. The platform grew from one user type (Inspector) to seven. Key deliverables from that session:

- **`taplog-roles-screens-expansion-spec.md`** — full expansion spec (now merged into taplog-spec.md)
- **`taplog-renter-concept.md`** — renter use case with Canadian housing statistics, absent tenant, housewatcher, LTB protection narrative
- **Smallburg platform family** named and positioned — `smallburg.ca` domain registered — 6-app municipal infrastructure suite
- **TapLog V2 reframe** — V1: was the inspection done? V2: who was responsible and did they do it? (liability transfer instrument)
- **Field Analyst role** defined — Robin's scouting business as the primary GTM for building owner acquisition
- **Renter kit concept** — $25/year, 5 tags, move-in record, LTB protection for 5M Canadian renter households

**New entities designed (not yet built — Room v9→10 migration pending):**
- `UserRole` enum (7 values)
- `TagEventRole` enum (5 values)
- `CheckInEvent` — caretaker/housewatcher tap log
- `ServiceRequest` + `ServiceRequestStatus` — liability transfer instrument
- `TenancyRecord` — tenancy period wrapper
- `MoveInRecord` + `AssetCondition` — move-in condition snapshot

**New screens designed (not yet built — Modules 42–49):**
RoleSelectionScreen, FieldAnalystDashboardScreen, VisitSetupScreen, FieldAnalystScanScreen, QuickRegisterSheet, LeaveBehindsScreen, UnregisteredTagScreen, ServiceRequestScreen, ServiceRequestTrailScreen, TenancySetupScreen, MoveInWalkthroughScreen, MoveOutScreen, RemoteMonitoringScreen + modified DashboardScreen/AssetDetailScreen/SplashScreen

---

---

### Module 39 — Dashboard shell (COMPLETE ✅)
See Session 12 handoff for details.

---

### Module 40 — Dashboard intelligence (COMPLETE ✅)

Three intelligence sections added to `DashboardScreen`. All 34 tasks complete. Build passing. Full validation confirmed.

**ViewModel additions:**
- `CalendarUrgency` enum (UPCOMING/DUE_SOON/OVERDUE), `CalendarEvent`, `TaskType` enum, `Task`, `SiteContact` data classes
- `AssetWithSite` JOIN result + `getAssetsWithDueDates()` DAO query (JOIN assets+sites, `nextInspectionDue IS NOT NULL`)
- `calendarEvents: StateFlow<Map<LocalDate, List<CalendarEvent>>>` — grouped by `LocalDate`, urgency classified vs today
- `tasks: StateFlow<List<Task>>` — combines deficiencies + overdue assets (explicit `nextInspectionDue < now` filter); priority sort: CRITICAL → HIGH → OVERDUE_ASSET → MEDIUM → LOW
- `allContacts: StateFlow<List<SiteContact>>` — derived from `sitesForOrg`, deduplicated by name+phone, alphabetical
- `ScanState.Calendar`, `.Tasks`, `.Contacts`; `showCalendar()`, `showTasks()`, `showContacts()`, `selectSiteById()`

**New screens:** `CalendarScreen.kt` (pure-Compose month grid, urgency dots, month navigation), `TasksScreen.kt` (FilterChip row, TaskRow composable), `ContactsScreen.kt` (search, tap-to-dial, ACTION_DIAL intent)

**DashboardScreen extended:** `CalendarSection` (local `displayedMonth` + `selectedDay` state), `TasksSection` (top 5 tasks), `ContactsSection` (top 4 contacts + search); `.imePadding()` on LazyColumn

**Two bugs fixed post-build:**
1. `CalendarSection` had no-op month navigation — added local `displayedMonth` + `selectedDay` state
2. Keyboard from Contacts search covered task list — added `.imePadding()` to dashboard `LazyColumn`

**Key facts confirmed during build:**
- `DeficiencyWithAsset.buildingName` is the site name — sourced from `sites.name` via existing JOIN. Check existing JOIN fields before adding new DAO queries.
- `java.time.LocalDate` / `YearMonth` — available from minSdk 26+, no desugaring needed.

---

### Module 37 — Anchor config (NOT YET BUILT)

Context file written at `openspec/module-37-context.md`. Backend-only — should be a fast session. Do this before Module 31.

---

### New lightbulbs — LB-014 through LB-032

19 new lightbulbs logged this session. Full file updated. Key ones for next session:

- **LB-014** — Vertical bundles are industry fluency. First bundle: TapLog Adit (mining, Ember + Anchor + Hatch).
- **LB-015** — Inspectors need help saying what they found. Hybrid chip row + AI polish model for notes. Likely Module 33 scope.
- **LB-016** — The PDF is the wrong deliverable. Per-inspection URL > file attachment.
- **LB-017** — "Overdue" is the wrong word. "Exposed" is the right word.
- **LB-018** — Building owner has no copy of their own compliance history.
- **LB-019** — The inspection schedule is a sales pipeline / revenue calendar.
- **LB-020** — Deficiency aging is a liability clock.
- **LB-021** — The first scan of a tag is the asset's birth certificate.
- **LB-022** — Compliance gaps cluster by building type (platform analytics tier).
- **LB-023** — The reinspection is a hidden workflow.
- **LB-024** — NFC tags on non-physical assets prove presence at a location (waypoints).
- **LB-025** — Inspector certification is a liability gap TapLog can close.
- **LB-026** — Seasonal inspection patterns are predictable load (workload smoothing).
- **LB-027** — The tag is the asset's immune system.
- **LB-028** — Building owner is paying for inspections they can't verify happened (pull GTM).
- **LB-029** — Equipment manufacturers want field performance data.
- **LB-030** — The inspection report is a marketing document (white-label opportunity).
- **LB-031** — The data layer needs a story, not a slide (TapLog × Farpost synthetic demo).
- **LB-032** — Farpost is a template, not a product. Engine: ingest → match → surface → act. Five apps: Farpost, Permit, Roster, Ledger, Signal.

---

### Vertical Bundle concept established

**`VerticalBundle`** — named, branded collection of `TapLogVertical` enums with display name, industry tag, billing SKU. Org onboarding presents bundles first; individual vertical selection is "custom."

**First bundle: TapLog Adit (mining)**
- Verticals: Ember + Anchor + Hatch
- Name: "adit" = horizontal mine entry tunnel. Industry-recognizable, no common-language baggage, latent product metaphor.
- Target venue: PDAC 2027
- Spec addendum written: paste `taplog-spec-bundles-addendum.md` into spec after vertical catalogue section.

---

### Farpost platform family

Farpost is not an insurance claims app — it's a record consumption and decision surfacing engine. The engine (ingest → match → surface → act) is a template for five applications:

| App | Industry | Trigger | Professional | Decision |
|---|---|---|---|---|
| **Farpost** | Insurance | Claim filed | Adjuster | Liability + settlement |
| **Permit** | Construction lending | Draw requested | Lender | Release funds |
| **Roster** | Industrial workforce | Worker checks in | Safety manager | Authorize site access |
| **Ledger** | Real estate | Due diligence / title transfer | Lawyer / lender | Approve transaction |
| **Signal** | Commercial insurance | Policy renewal | Underwriter | Price the risk |

TapLog generates the records. The Farpost platform family consumes them. Founder owns both ends of the pipe.

---

### taplog.ca — Homepage complete

**File:** `taplog-index.html` — single HTML file, deploy to Cloudflare Pages.

**Design system:** Extracted from Color.kt / Theme.kt / Type.kt. CSS token set is now the canonical source of truth for web, admin portal, and PDF reports. Dark-first. Inter font. All hex values exact matches to Color.kt.

**Homepage sections:** Nav · Hero (phone mockframe) · Problem strip · How it works · Proof strip · Audience split · Verticals · Data layer · Pilot CTA · Pricing · Footer

**To go live:**
1. Set up `hello@taplog.ca` or replace with real contact address
2. Export splash icon as SVG, replace CSS logo mark placeholder
3. Point taplog.ca DNS to Cloudflare Pages
4. Add working contact/waitlist form (Formspree or FastAPI endpoint)

**Remaining pages to build:**
- `/how-it-works`, `/pilot`, `/about`, `/contact`, `/verticals`, `/pricing` (pricing already on homepage, may be redundant)
- `/data` — TapLog × Farpost data layer story (LB-031 demo scenario) — investor + insurer page
- `/clients` — building owner audience (LB-028 pull strategy)

---

## Current state of the app

**Android (Room v10, build passing):**
- **Field Analyst mode** live — RoleSelectionScreen → FieldAnalystDashboard → VisitSetup → FieldAnalystScanScreen + QuickRegisterSheet + UnregisteredTagScreen
- **Visual Asset Identification** live — camera icon in QuickRegisterSheet and AssetRegistrationScreen → Claude vision → "AI suggested" chip in AssetTypePickerDialog
- `DashboardScreen` (Inspector home) — stats strip, quick actions, calendar, tasks, contacts, overdue section
- `CalendarScreen`, `TasksScreen`, `ContactsScreen` reachable from "See all" links
- Full Ember inspection flow end-to-end unchanged and working
- Anchor VerticalConfig seeded to MongoDB — app fetches it on startup
- `AssetDetailScreen` — birthing event provenance card showing registered-by info
- Brand theme, SplashScreen, Mapbox site map all working

**Backend (all endpoints live):**
- All auth, sync, and vertical endpoints live at https://web-production-a9fb1.up.railway.app

**taplog.ca:**
- Domain registered, not yet live
- Homepage built: `taplog-index.html`
- Design system tokens established

**Not yet built (in priority order):**
- Module 42 — Service request trail + missed inspection log ← **highest priority**
- Module 32 — Pre-Inspection Checklist card
- Module 33 — AI co-pilot
- Module 41 — Billing (before OAFC November 2026)
- Module 44 — Owner/tenant web dashboard (taplog.ca)
- Module 45 — Tenancy records + Move-in/out flow
- Module 46 — Caretaker role + Check-in events
- Module 47 — Remote monitoring screen
- Module 48 — Notification architecture (FCM)
- Module 49 — Leave-behind PDF generator
- Module 50 — Deep linking + taplog.ca live
- taplog.ca remaining pages

**Deferred:**
- Module 38 — Hatch config + EntryEventScreen
- Inspection cardinality open design doc
- Invite token model + V2 admin portal
- VerticalBundle data model + onboarding UI
- LB-015 — notes chip row + AI polish (Module 33 scope)
- LB-016 — per-inspection shareable URL
- LB-023 — reinspection workflow
- LB-024 — location waypoint tags

---

## Updated module sequence

| # | Module | Type | Context file | Status |
|---|---|---|---|---|
| **37** | Anchor config — MongoDB | Backend-only | `module-37-context.md` ✅ | ✅ Complete |
| **39** | Dashboard shell | Android | — | ✅ Complete |
| **40** | Dashboard intelligence | Android | `module-40-context.md` ✅ | ✅ Complete |
| **43** | Field Analyst mode + Role Selection | Android | `module-43-context.md` ✅ | ✅ Complete |
| **31** | Visual Asset Identification | Android + Anthropic API | `module-31-context.md` ✅ | ✅ Complete |
| **42** | Service request trail | Android | Write context first | **Next** |
| **32** | Pre-Inspection Checklist card | Android UI | `module-32-context.md` ✅ | — |
| **33** | AI co-pilot | Android + Anthropic API | `module-33-context.md` ✅ | — |
| **41** | Billing | Web + Android | No context yet | Before OAFC Nov 2026 |
| **44** | Owner/tenant web dashboard | Web | No context yet | — |
| **45** | Tenancy records + Move-in/out | Android + Web | No context yet | — |
| **46** | Caretaker role + Check-in events | Android | No context yet | — |
| **47** | Remote monitoring screen | Android + Web | No context yet | — |
| **48** | Notification architecture | Android + Backend | No context yet | — |
| **49** | Leave-behind PDF generator | Android | No context yet | — |
| **50** | Deep linking + taplog.ca live | Web + Android | No context yet | Depends on taplog.ca deployment |

---

## Key technical context

- **Stack:** Kotlin 2.3.21 · Compose · Room 2.8.4 (version **10**) · KSP 2.3.9 · AGP 9.2.1 · DataStore 1.1.4 · Mapbox Maps SDK 11.12.0
- **Backend:** FastAPI + Motor + MongoDB Atlas, live at https://web-production-a9fb1.up.railway.app
- **Backend Python:** 3.13 · bcrypt direct (no passlib) · python-jose · httpx
- **Dev device:** Pixel 10 Pro XL (API 36)
- **Android Studio:** Panda 4 | 2025.3.4 Patch 1, Windows
- **Repos:** C:\dev\taplog (Android), C:\dev\taplog-api (backend)
- **Package:** ca.taplog.app
- **Room DB:** version **10**, `taplog_ember.db`
- **MongoDB:** farpost-dev Atlas M0 cluster, `taplog` database

## Critical workflow notes
- **File replacement in Android Studio:** select-all (Ctrl+A) → paste. Never Windows Explorer copy.
- **One file at a time** — confirm paste before receiving next file.
- **End of session:** update all md docs → generate handoff → `copy taplog-md-prime-directives.md .claude\AGENTS.md`
- **OpenSpec per module:** `/opsx:propose` → review → `/opsx:apply` → `/opsx:archive`
- **Context file first:** write `openspec/module-N-context.md` before running `/opsx:propose` for any architectural or new-vertical module
- **Room migration crashes during dev:** clear app storage (Settings → Apps → TapLog → Storage → Clear storage)
- **Mapbox token:** use `MapboxOptions.accessToken = BuildConfig.MAPBOX_PUBLIC_TOKEN` in `TapLogApplication.onCreate()` — manifest meta-data approach unreliable in SDK v11
- **Resend email:** using `onboarding@resend.dev` as sender. Switch to `noreply@taplog.ca` once domain verified in Resend dashboard.
- **VerticalRegistry must be populated before any screen that calls `VerticalRegistry.get()`** — SplashScreen gates on `registryReady` StateFlow
- **`OFCAssetTypes.kt` is intentionally retained** — cold-start fallback for Ember. Do not delete until backend vertical config is proven in production.
- **Room v9** — added via `MIGRATION_8_9`: `ALTER TABLE sites ADD COLUMN latitude REAL` + `ALTER TABLE sites ADD COLUMN longitude REAL`
- **`AssetDetailSource`** lives in `ca.taplog.app.data` — import from there, not `ui.ember`
- **`material-icons-extended`** required for `Icons.AutoMirrored.*` and `Icons.Default.*`
- **InspectorPreferences.kt** is physically in `ui/ember/` directory but declares `package ca.taplog.app.data` — don't move it
- **Brand colors on SplashScreen** must be hardcoded private constants — `MaterialTheme` is not available when splash renders
- **`imePadding()`** belongs on the scrollable container (`LazyColumn`), not the `Scaffold`
- **`DeficiencyWithAsset.buildingName`** is the site name (from `sites.name` via existing JOIN) — check existing JOIN fields before adding new DAO queries
- **`java.time.LocalDate` / `YearMonth`** — available from minSdk 26+, no desugaring needed
- **CLI context budget:** do not upload `taplog-curriculum.md` or `taplog-lightbulbs.md` to CLI sessions — chat/strategy docs only. CLI needs spec + handoff + context file only.
- **CLI PowerShell confirmation prompt:** type `1` to select "Yes, and don't ask again" when CLI asks about expandable string commands.
