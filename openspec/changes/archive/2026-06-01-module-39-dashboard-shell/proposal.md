## Why

`SiteListScreen` is a functional list but not a professional home screen — an inspector arriving at work can't see at a glance what's overdue, what needs follow-up, or where they're going today. A proper dashboard makes TapLog feel like a platform, not a list viewer, and directly supports the sales pitch: "your morning briefing in one screen."

## What Changes

- **BREAKING** `ScanState.SiteList` renamed to `ScanState.Dashboard` — all references in `EmberViewModel` and `MainActivity` updated
- New `DashboardScreen.kt` — root home screen replacing `SiteListScreen` as the `ScanState.Dashboard` destination
- New `StatsStrip` composable — four metric cards: inspections this month, open deficiencies, overdue sites, total sites
- New `OverdueSection` composable — sites with at least one overdue asset, sorted by most-overdue first; each row shows site name, overdue count, most overdue asset name, and days overdue
- `SiteListScreen` remains a full-screen destination ("See all" from dashboard), not deleted
- `EmberViewModel` gains `dashboardStats: StateFlow<DashboardStats>` and `overdueSites: StateFlow<List<SiteWithOverdueCount>>`
- New DAO queries in `EmberDAO` for overdue sites and monthly inspection count — no Room schema changes
- Quick actions section: primary Scan button, Add Site, All Deficiencies
- Sites section in dashboard: embedded `SiteListScreen` content (list/map toggle from Module 36) with "See all" link

## Capabilities

### New Capabilities
- `dashboard-screen`: `DashboardScreen` composable — stats strip, quick actions, overdue section, embedded sites section; replaces `SiteListScreen` as home
- `dashboard-stats`: `DashboardStats` data class and ViewModel StateFlows derived from existing Room entities; `SiteWithOverdueCount` for the overdue section

### Modified Capabilities

None — `ScanState` rename is an implementation detail, not a spec-level requirement change. No existing spec requirements change behavior.

## Impact

- **Android:** `DashboardScreen.kt` (new), `EmberViewModel.kt` (new StateFlows + data classes), `EmberDAO.kt` (new queries), `MainActivity.kt` (`ScanState.Dashboard` routing), `SiteListScreen.kt` (remains, used as full-screen destination)
- **No backend changes**
- **No Room migrations** — all new queries are read-only over existing tables
- **No new dependencies**
