# Module 39 — Context File
> For: `/opsx:propose "module-39-dashboard-shell"`
> Written: 2026-05-31

---

## What this module is

Replace `SiteListScreen` as the app's home screen with a proper `DashboardScreen`. The dashboard is the true home — a portal to all key sections of the app. `SiteListScreen` becomes a composable embedded within the dashboard and available as a full-screen destination.

This is Module 39a of a two-part dashboard build. Module 39 is the shell: stats strip, quick actions, overdue/urgent section, and sites section. Module 40 adds calendar, tasks, and contacts.

---

## Why this matters

Right now the app drops the user into `SiteListScreen` after splash. That's functional but not aspirational. A fire safety inspector arriving at work needs to know at a glance: what's overdue, what needs follow-up, where am I going today. A list of sites doesn't answer those questions — a dashboard does. This is what makes TapLog feel like a professional platform rather than a field tool.

---

## Scope — Module 39

**In scope:**
- `DashboardScreen.kt` — new root home screen
- `StatsStrip.kt` — four metric cards composable
- `QuickActionsSection.kt` — scan button + secondary actions
- `OverdueSection.kt` — sites with overdue assets, sorted by urgency
- `SitesSectionPreview.kt` — embedded sites list/map with "See all" link
- `EmberViewModel` additions: `dashboardStats` StateFlow
- `ScanState.SiteList` renamed to `ScanState.Dashboard`
- Navigation: Dashboard replaces SiteList as root; SiteListScreen accessible full-screen via "See all"

**Out of scope (Module 40):**
- Calendar section
- Tasks section (open deficiencies + overdue assets unified)
- Contacts section

**No Room migrations** — all data is derived from existing entities.

---

## Screen layout

```
┌─────────────────────────────────┐
│  TopAppBar: TapLog + sync icon  │
├─────────────────────────────────┤
│  StatsStrip (4 cards)           │
│  Inspections | Deficiencies |   │
│  Overdue Sites | Total Sites    │
├─────────────────────────────────┤
│  QuickActions                   │
│  [SCAN — primary, large]        │
│  [Add Site]  [All Deficiencies] │
├─────────────────────────────────┤
│  "Overdue & Urgent" section     │
│  Sites with overdue assets,     │
│  sorted by days overdue         │
│  Each row: site name, overdue   │
│  count, most overdue asset      │
├─────────────────────────────────┤
│  "Sites" section                │
│  List/Map toggle (reuses        │
│  SiteListScreen composable)     │
│  "See all" → full SiteList      │
└─────────────────────────────────┘
```

---

## Stats strip — four metrics

| Metric | Source | Label |
|---|---|---|
| Inspections this month | `Inspection.createdAt` in current calendar month | "This month" |
| Open deficiencies | `Deficiency` where `resolvedAt == null` | "Open deficiencies" |
| Overdue sites | Sites with at least one asset where `nextInspectionDue < now` | "Sites overdue" |
| Total sites | `SiteDao.getAll().size` | "Sites" |

All four derived from existing Room queries — no new entities needed.

---

## Quick actions

- **Scan** — primary CTA, large teal button, triggers NFC scan (same as current scan entry point). This is always one tap away from home.
- **Add Site** → `SiteRegistrationScreen`
- **All Deficiencies** → `OpenDeficienciesScreen`

---

## Overdue & Urgent section

- Query: all sites where any asset has `nextInspectionDue < now` and `isActive == true`
- Sort: by the most-overdue asset's `nextInspectionDue` (oldest first = most urgent at top)
- Each row shows: site name, overdue asset count, name of most overdue asset, days overdue (e.g. "14 days")
- Row tap → `SiteDetailScreen`
- Empty state: "All sites are current" with a green check — positive reinforcement

---

## Sites section (preview)

- Reuse the existing `SiteListScreen` composable in embedded/preview mode
- Show the list/map toggle that was built in Module 36
- "See all" button in section header → navigates to full-screen `SiteListScreen`
- The map geocoding and pin display carry over unchanged

---

## ViewModel changes

`EmberViewModel` gains:

```kotlin
data class DashboardStats(
    val inspectionsThisMonth: Int,
    val openDeficiencies: Int,
    val overdueSiteCount: Int,
    val totalSites: Int
)

val dashboardStats: StateFlow<DashboardStats>
val overdueSites: StateFlow<List<SiteWithOverdueCount>>  // new data class
```

`SiteWithOverdueCount`:
```kotlin
data class SiteWithOverdueCount(
    val site: Site,
    val overdueCount: Int,
    val mostOverdueDays: Int,
    val mostOverdueAssetName: String
)
```

---

## Navigation change

`ScanState` enum:
- `SiteList` → rename to `Dashboard`
- All existing `ScanState.SiteList` references updated throughout ViewModel and MainActivity

`MainActivity` routing:
- `ScanState.Dashboard` → `DashboardScreen`
- `ScanState.SiteSelected` → `SiteDetailScreen` (unchanged)
- All other states unchanged

`SiteListScreen` remains a navigable destination (via "See all" from dashboard) — it is not deleted.

---

## Design notes

- Stats strip cards use `TapLogNavy700` background with teal accent numbers — prominent but not loud
- Overdue section uses `danger` semantic color for the overdue count chips (defined in `Color.kt`)
- Quick Scan button is full-width or near-full-width, `TapLogTeal400` fill — the most important action in the app should look like it
- Section headers follow a consistent pattern: label left, "See all" text button right
- All sections are always visible (no collapsing in this module — collapsible headers can come later)

---

## What does NOT change

- `SiteListScreen.kt` — not deleted, not restructured; becomes a full-screen destination
- `SiteDetailScreen.kt` and all downstream screens — unchanged
- `SiteMapView.kt` — reused as-is
- Room schema — no migrations
- Backend — no changes
- `EmberVerticalConfig`, `OFCAssetTypes`, `VerticalRegistry` — untouched
