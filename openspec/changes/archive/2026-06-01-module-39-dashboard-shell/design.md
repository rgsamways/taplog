## Context

`SiteListScreen` is currently `ScanState.SiteList` — the app's home state after login. It shows a flat list of sites with a list/map toggle (Module 36). The screen does its job but provides no at-a-glance status. An inspector arriving at work has to mentally process the list to understand their workload. The dashboard replaces this with a structured briefing: what's urgent, quick access to the primary action (Scan), and the site list still accessible one tap away.

The ViewModel (`EmberViewModel`) already tracks `organisation`, `currentSite`, `openDeficienciesWithAsset`, and `activeAssets`. The new stats and overdue data can be derived from these + a few new DAO queries over existing tables. No Room schema changes.

## Goals / Non-Goals

**Goals:**
- `DashboardScreen` is the home — replaces `ScanState.SiteList` routing
- Stats strip gives instant numeric overview (inspections, deficiencies, overdue, sites)
- Overdue section surfaces the highest-urgency work first
- Sites section reuses existing `SiteListScreen` composable logic (list + map toggle)
- `SiteListScreen` remains fully accessible as a full-screen destination

**Non-Goals:**
- Calendar, tasks, contacts sections (Module 40)
- Any Room schema changes or migrations
- Backend changes
- Filtering or assignment (multi-inspector — deferred to Company tier UI work)

## Decisions

**Decision: `ScanState.SiteList` → `ScanState.Dashboard` (rename, not new state)**
The dashboard is the new home state — it is not a separate navigation destination alongside SiteList. Renaming the state rather than adding a new one keeps the state machine clean and avoids dual home states. All `when(scanState)` branches in `MainActivity` are updated.

**Decision: `DashboardStats` and `SiteWithOverdueCount` as ViewModel data classes, not Room entities**
These are derived views over existing data — not persisted. `DashboardStats` is computed from existing flows. `SiteWithOverdueCount` is derived by joining `getSitesForOrg()` flow with `activeAssets` flow in the ViewModel, grouping and counting overdue assets per site. This avoids a Room JOIN query returning a non-entity class (which would require a new DAO method for a relatively simple derivation).

Alternative considered: a Room DAO query with a JOIN and GROUP BY. Rejected because the asset and site data is already loaded into StateFlows — an in-memory derivation is simpler and avoids a new DB query on the hot path.

**Decision: Overdue section sorted in ViewModel, not DAO**
`overdueSites` is derived from `activeAssets` and sites flows already in memory. Sorting in the ViewModel (by `mostOverdueDays` descending) keeps DAO queries simple and the sort logic testable without a DB.

**Decision: `SiteListScreen` reused as embedded content, not duplicated**
The sites section in the dashboard uses the same `SiteListScreen` composable. A `isEmbedded: Boolean` parameter gates whether the FAB and full TopAppBar are shown. This avoids duplicating list/map toggle logic.

**Decision: "Inspections this month" uses `Inspection.inspectedAt` filtered to current calendar month**
Calendar month (not rolling 30 days) matches how inspectors think about their workload. `inspectedAt` is the correct timestamp — it's when the inspection actually occurred, not when it was synced.

## Risks / Trade-offs

[Risk] Deriving `SiteWithOverdueCount` in the ViewModel by combining two flows adds a `combine()` operator — if either flow emits frequently, the derived flow recomputes on every emission → Mitigation: Both `getSitesForOrg()` and `activeAssets` are Room flows that only emit on DB writes; recomputation is bounded to actual data changes.

[Risk] `ScanState.SiteList` rename is a BREAKING change — any reference missed in a `when()` branch fails at compile time → Mitigation: Kotlin sealed classes with exhaustive `when` expressions make this a compile error, not a silent bug. Safe to rename.

[Risk] `isEmbedded` parameter on `SiteListScreen` adds a parameter to an existing composable — callers that don't pass it default to `false` (full-screen mode), so the existing `ScanState.SiteList` call site is backward-compatible with a default.
