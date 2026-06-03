## 1. ScanState — add Dashboard (Option A: SiteList retained)

- [x] 1.1 In `EmberViewModel.kt`, add `object Dashboard : ScanState()` to the sealed class; update `loadOrganisation()`, `resetScanState()`, and `showSiteList()` (rename to `showDashboard()`) to emit `ScanState.Dashboard` as the home state. `ScanState.SiteList` is NOT removed.
- [x] 1.2 In `MainActivity.kt`, add a new `is EmberViewModel.ScanState.Dashboard` branch that routes to `DashboardScreen`. The existing `ScanState.SiteList` branch remains unchanged as the full-screen list destination.
- [x] 1.3 Confirm the project compiles cleanly — sealed class now has both Dashboard and SiteList

## 2. ViewModel — data classes and StateFlows

- [x] 2.1 Add `DashboardStats` data class to `EmberViewModel.kt`: `inspectionsThisMonth: Int`, `openDeficiencies: Int`, `overdueSiteCount: Int`, `totalSites: Int`
- [x] 2.2 Add `SiteWithOverdueCount` data class to `EmberViewModel.kt`: `site: Site`, `overdueCount: Int`, `mostOverdueDays: Int`, `mostOverdueAssetName: String`
- [x] 2.3 Add `dashboardStats: StateFlow<DashboardStats>` — combine `getSitesForOrg()` flow, `openDeficienciesWithAsset`, and `activeAssets`; compute inspections this month by filtering `activeAssets` inspections by calendar month (use `inspectedAt` field)
- [x] 2.4 Add `overdueSites: StateFlow<List<SiteWithOverdueCount>>` — derive from `getSitesForOrg()` and `activeAssets` flows: group overdue assets by `siteId`, map to `SiteWithOverdueCount`, filter to `overdueCount > 0`, sort by `mostOverdueDays` descending
- [x] 2.5 Add `showDashboard()` function replacing `showSiteList()` — emits `ScanState.Dashboard`

## 3. DAO — monthly inspection query

- [x] 3.1 Add `getInspectionsForOrg(organisationId: String): Flow<List<Inspection>>` query to `InspectionDao` (or equivalent scoped query) — needed to compute `inspectionsThisMonth` across all sites

## 4. DashboardScreen composable

- [x] 4.1 Create `DashboardScreen.kt` in `ca.taplog.app.ui.ember/` — `LazyColumn` root with `TopAppBar` (title "TapLog", sync icon placeholder)
- [x] 4.2 Add `StatsStrip` composable — `Row` of four `StatCard` composables (`TapLogNavy700` background, teal accent number, label beneath); data from `dashboardStats`
- [x] 4.3 Add quick actions section — full-width primary Scan button (`TapLogTeal400` fill), secondary row with Add Site and All Deficiencies `OutlinedButton`s
- [x] 4.4 Add overdue section with header ("Overdue & Urgent" label + count chip); `SiteOverdueRow` composable per item showing site name, overdue count badge, most overdue asset name, days overdue; empty state "All sites are current ✓" when list is empty
- [x] 4.5 Add sites section with header ("Sites" label + "See all" `TextButton`); collects sites flow directly (avoids nested Scaffold); shows up to 4 site cards inline; "See all" emits `ScanState.SiteList`

## 5. SiteListScreen — isEmbedded parameter

- [x] 5.1 Add `isEmbedded: Boolean = false` parameter to `SiteListScreen` — when `true`, suppress the `FloatingActionButton`; added `onBack` navigation icon in TopAppBar; existing callers unaffected by defaults

## 6. MainActivity routing

- [x] 6.1 Wire `ScanState.Dashboard` in `EmberScanScreen` `when` block to call `DashboardScreen`, passing `viewModel` and callbacks: `onSiteSelected`, `onAddSite`, `onShowDeficiencies`, `onGeocodeUnresolved`, `onShowSiteList` (→ `ScanState.SiteList`)
- [x] 6.2 Confirm the existing `ScanState.SiteList` branch in `MainActivity` still renders `SiteListScreen` as a full-screen destination (unchanged functionality)

## 7. Validation

- [x] 7.1 Build project — no compilation errors
- [x] 7.2 Launch app on device — confirm `DashboardScreen` loads as home after login
- [x] 7.3 Confirm stats strip shows correct counts matching known Room data
- [ ] 7.4 Confirm overdue section shows sites with overdue assets and is empty-stated correctly when none exist (TBD — needs real overdue data in the field)
- [x] 7.5 Tap Scan → NFC scan readiness; tap Add Site → `SiteRegistrationScreen`; tap All Deficiencies → `OpenDeficienciesScreen` (All Deficiencies untested — no open deficiencies in dev data)
- [x] 7.6 Tap "See all" sites → full-screen `SiteListScreen` with back navigation returning to dashboard
- [ ] 7.7 Tap an overdue site row → `SiteDetailScreen` for correct site (TBD — needs real overdue data in the field)
