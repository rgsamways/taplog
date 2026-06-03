## ADDED Requirements

### Requirement: DashboardStats data class is derived from existing Room flows
The system SHALL define a `DashboardStats` data class with fields `inspectionsThisMonth: Int`, `openDeficiencies: Int`, `overdueSiteCount: Int`, and `totalSites: Int`. `EmberViewModel` SHALL expose `dashboardStats: StateFlow<DashboardStats>` derived without new Room entities or migrations.

#### Scenario: DashboardStats updates reactively
- **WHEN** a new inspection is submitted
- **THEN** `dashboardStats.inspectionsThisMonth` increments if the inspection occurred in the current calendar month

#### Scenario: DashboardStats initialises to zero
- **WHEN** `EmberViewModel` is created before any org data is loaded
- **THEN** `dashboardStats` emits `DashboardStats(0, 0, 0, 0)`

### Requirement: SiteWithOverdueCount data class carries per-site overdue context
The system SHALL define a `SiteWithOverdueCount` data class with fields `site: Site`, `overdueCount: Int`, `mostOverdueDays: Int`, and `mostOverdueAssetName: String`. `EmberViewModel` SHALL expose `overdueSites: StateFlow<List<SiteWithOverdueCount>>` containing only sites with `overdueCount > 0`, sorted by `mostOverdueDays` descending.

#### Scenario: Only sites with overdue assets appear
- **WHEN** some sites have overdue assets and some do not
- **THEN** `overdueSites` contains only the sites with at least one overdue asset

#### Scenario: mostOverdueDays is accurate
- **WHEN** a site has an asset whose `nextInspectionDue` is 14 days in the past
- **THEN** the corresponding `SiteWithOverdueCount.mostOverdueDays` is 14 (or greater, rounded to whole days)
