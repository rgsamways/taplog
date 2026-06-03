## Context

Module 39 delivered `DashboardScreen` as the home screen with a stats strip, quick actions, overdue section, and a 4-site preview. The screen is a `LazyColumn` with a clear section-based structure. Module 40 extends that column with three new intelligence sections. All data required already exists in Room v9 — `Asset.nextInspectionDue`, `Deficiency.resolvedAt`, and `Site.clientName/clientPhone/contactName/contactPhone`. No migrations are needed.

The app uses `StateFlow` + `combine()` in `EmberViewModel` for reactive data, and `ScanState` sealed class for screen routing in `MainActivity`. Both patterns are well-established and this module follows them without deviation.

## Goals / Non-Goals

**Goals:**
- Add Calendar, Tasks, and Contacts sections to `DashboardScreen`, each with a preview and "See all" link
- Provide full-screen versions of all three (`CalendarScreen`, `TasksScreen`, `ContactsScreen`)
- All data derived reactively from existing Room entities — no user-visible loading states
- Navigation consistent with Module 39 patterns (origin-aware back, `ScanState` routing)

**Non-Goals:**
- Room schema changes or migrations
- Backend changes of any kind
- Replacing `OpenDeficienciesScreen` — tasks section is a read/navigate view; resolve action stays on existing screens
- Calendar editing or appointment creation
- Push notifications for due dates
- Bottom navigation bar (deferred)

## Decisions

### D1: Pure-Compose calendar grid (no library)

**Decision:** Build the month calendar grid from scratch using `LazyVerticalGrid` or manual `Row`/`Column` layout rather than adding a third-party calendar library.

**Rationale:** The calendar is a display-only component — it shows dots on days, not a full scheduling UI. A library adds APK weight, a new dependency to maintain, and opinionated styling that fights Material3. A pure-Compose grid is ~100 lines and gives full control over dot rendering, day cell sizing, and colour.

**Alternative considered:** `kizitonwose/calendar-compose` — well-maintained but 200KB+ and overkill for read-only dot indicators.

---

### D2: `calendarEvents` as `Map<LocalDate, List<CalendarEvent>>` StateFlow

**Decision:** Expose calendar data as a map keyed by `LocalDate`, computed in the ViewModel from the `activeAssets` flow (already subscribed) joined with site names via a new DAO query.

**Rationale:** The composable needs O(1) lookup per day cell. A list would require filtering on every recomposition of each cell. The map is computed once when the flow emits and cached in `stateIn`. `LocalDate` requires API 26+ — TapLog's `minSdk` is already above this.

**Alternative considered:** Passing a flat list and filtering in the composable — correct but inefficient for a 30+ cell grid.

---

### D3: Unified `Task` model for deficiencies and overdue assets

**Decision:** Define a single `Task` sealed-class-like data class with a `TaskType` enum (`OPEN_DEFICIENCY`, `OVERDUE_ASSET`) rather than two separate lists.

**Rationale:** The `TasksSection` and `TasksScreen` render both types in a single sorted list. A unified model lets the sort logic live in the ViewModel (`combine()` of two flows) and the UI stay simple — one `LazyColumn` items block, one row composable with conditional rendering based on `type`.

**Sort order:** CRITICAL deficiencies → HIGH deficiencies → OVERDUE_ASSET (by `mostOverdueDays` desc) → MEDIUM deficiencies → LOW deficiencies.

---

### D4: Contacts derived in ViewModel, not DAO

**Decision:** `allContacts: StateFlow<List<SiteContact>>` is derived from `sitesForOrg` flow already in the ViewModel. Each site emits 0–2 `SiteContact` entries. Deduplication (same name+phone across sites) and search filtering both happen in-memory.

**Rationale:** No new DAO query needed — `getSitesForOrganisation()` already returns the full `Site` objects including all contact fields. The contact list is small (bounded by site count) so in-memory derivation is correct. A separate DAO query adds complexity with no benefit.

---

### D5: Three new `ScanState` entries for full-screen screens

**Decision:** Add `ScanState.Calendar`, `ScanState.Tasks`, `ScanState.Contacts` to the sealed class. Each full-screen screen is a top-level destination consistent with how `ScanState.SiteList` and `ScanState.OpenDeficiencies` work.

**Rationale:** Consistent with the existing routing pattern. Back navigation from each returns to `ScanState.Dashboard` via `showDashboard()`. No new navigation infrastructure needed.

---

### D6: `AssetWithSite` JOIN result for calendar data

**Decision:** Add a new `data class AssetWithSite(val asset: Asset, val siteName: String)` and a DAO query `getAssetsWithDueDates(): Flow<List<AssetWithSite>>` joining `assets` and `sites` tables. Check first whether an equivalent JOIN result already exists in `EmberDAO` — reuse if so.

**Rationale:** The calendar needs both `asset.nextInspectionDue` and `siteName` for the day-tap expansion list. Fetching sites separately and joining in the ViewModel is messier than a single JOIN query.

## Risks / Trade-offs

**[Risk] Calendar recomposes on every asset change** — `calendarEvents` StateFlow recomputes the full map whenever any asset changes. With hundreds of assets this could be expensive.
→ Mitigation: Use `stateIn(SharingStarted.WhileSubscribed(5000))` so the flow is only active when the screen is visible. The map computation is pure Kotlin (no DB call) and fast in practice for the expected data volumes (< 500 assets).

**[Risk] `LocalDate` API 26 requirement** — `java.time.LocalDate` requires API 26.
→ Mitigation: TapLog's `minSdk` is already API 26+. No issue.

**[Risk] Contact deduplication is name+phone exact match** — the same person listed with a slightly different name on two sites won't be deduplicated.
→ Mitigation: Acceptable for v1. Fuzzy matching is a future enhancement. The list is short and inspectors will recognise duplicates.

**[Risk] `DashboardScreen` LazyColumn grows long** — adding three sections makes the dashboard scroll-heavy.
→ Mitigation: Each section shows a preview (current month only for calendar, top 5 for tasks, top 4–5 for contacts). The sections are collapsed by default; "See all" expands to full screen. This is the established pattern from Module 39.

## Open Questions

None — context file is complete and all decisions are resolved above.
