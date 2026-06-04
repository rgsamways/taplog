## 1. Data Model

- [x] 1.1 Add `ServiceRequestStatus` enum to `EmberModels.kt` (SENT, ACKNOWLEDGED, SCHEDULED, COMPLETED, NO_RESPONSE)
- [x] 1.2 Add `ServiceRequest` entity to `EmberModels.kt` with all fields from spec
- [x] 1.3 Add `ServiceRequestStatus` TypeConverter to `Converters.kt` (same pattern as other enums — nullable not needed, has default)

## 2. Room Migration v10 → v11

- [x] 2.1 Add `MIGRATION_10_11` object to `AppDatabase.kt` with `CREATE TABLE service_requests` SQL
- [x] 2.2 Bump `AppDatabase.VERSION` from 10 to 11
- [x] 2.3 Add `MIGRATION_10_11` to `addMigrations(...)` call
- [x] 2.4 Add `ServiceRequest::class` to `@Database(entities = [...])` list

## 3. DAO + Repository

- [x] 3.1 Add `ServiceRequestDao` interface to `EmberDAO.kt`: `insert`, `getByAsset(assetId): Flow<List<ServiceRequest>>`, `getUnsyncedSent(): List<ServiceRequest>` (status=SENT, older than 7 days), `updateStatusToNoResponse(ids: List<String>)`
- [x] 3.2 Add abstract `fun serviceRequestDao(): ServiceRequestDao` to `AppDatabase`
- [x] 3.3 Add repository methods to `EmberRepository`: `insertServiceRequest()`, `getServiceRequestsForAsset()`, `markExpiredRequestsNoResponse()`
- [x] 3.4 Wire `serviceRequestDao` into `EmberRepository` constructor and `TapLogApplication.repository`

## 4. ViewModel

- [x] 4.1 Add `ScanState.ServiceRequestForm(asset: Asset)` and `ScanState.ServiceRequestTrail(asset: Asset)` to sealed class in `EmberViewModel.kt`
- [x] 4.2 Add `private val _currentAssetServiceRequests = MutableStateFlow<List<ServiceRequest>>(emptyList())` and `val serviceRequestsForCurrentAsset: StateFlow<List<ServiceRequest>>`
- [x] 4.3 Add `fun loadServiceRequestsForAsset(assetId: String)` — collects from repository Flow
- [x] 4.4 Add `fun sendServiceRequest(asset: Asset, contractorName: String?, contractorPhone: String?, contractorEmail: String?, notes: String?)` — creates and inserts `ServiceRequest`; navigates back to `AssetFound`
- [x] 4.5 Add `fun showServiceRequestForm(asset: Asset)` — sets state to `ServiceRequestForm`
- [x] 4.6 Add `fun showServiceRequestTrail(asset: Asset)` — sets state to `ServiceRequestTrail`
- [x] 4.7 Add `fun markExpiredServiceRequestsNoResponse()` — calls `repository.markExpiredRequestsNoResponse()` in viewModelScope

## 5. Startup NO_RESPONSE Trigger

- [x] 5.1 In `TapLogApplication.onCreate()`, after `initVerticalRegistry()`, call `markExpiredServiceRequestsNoResponse()` via `appScope.launch` — but only after ViewModel is available. OR: call `repository.markExpiredRequestsNoResponse()` directly in `appScope.launch` since repository is a lazy val available at startup.
- [x] 5.2 `EmberRepository.markExpiredRequestsNoResponse()` queries all SENT requests older than 7 days and bulk-updates to NO_RESPONSE

## 6. ServiceRequestScreen

- [x] 6.1 Create `ServiceRequestScreen.kt` in `ui/ember/`
- [x] 6.2 `Scaffold` with `TopAppBar`: "Request Inspection" title + back button
- [x] 6.3 `Column` with `.verticalScroll` + `.imePadding()`: asset name + overdue indicator at top (read-only)
- [x] 6.4 Urgency `SingleChoiceSegmentedButtonRow`: Routine / Urgent / Critical
- [x] 6.5 `OutlinedTextField` for contractor name (optional)
- [x] 6.6 `OutlinedTextField` for contractor phone (optional)
- [x] 6.7 `OutlinedTextField` for contractor email (optional)
- [x] 6.8 `OutlinedTextField` for notes (multiline, optional), pre-filled with urgency prefix
- [x] 6.9 "Send Request" `Button` (always enabled — all fields optional) → calls `viewModel.sendServiceRequest(...)`, navigates back
- [x] 6.10 Wire into `MainActivity` `when (state)` block for `ScanState.ServiceRequestForm`

## 7. ServiceRequestTrailScreen

- [x] 7.1 Create `ServiceRequestTrailScreen.kt` in `ui/ember/`
- [x] 7.2 `Scaffold` with `TopAppBar`: "Service Requests" + asset name subtitle + back button + "Export PDF" action button (stub)
- [x] 7.3 `LazyColumn` of service request entries — reverse chronological (newest first)
- [x] 7.4 Each entry `Card`: date sent, contractor name ("Unknown contractor" if null), status badge, response date / "No response" label
- [x] 7.5 `NO_RESPONSE` entries: `Card` containerColor = `MaterialTheme.colorScheme.errorContainer`; "Liability transferred" label in error color
- [x] 7.6 Empty state: "No service requests recorded" centred text
- [x] 7.7 "Export PDF" action button → `Toast.makeText("PDF export coming soon").show()`
- [x] 7.8 Wire into `MainActivity` for `ScanState.ServiceRequestTrail`

## 8. AssetDetailScreen Additions

- [x] 8.1 In `AssetDetailScreen`, add `serviceRequests: List<ServiceRequest> = emptyList()` parameter
- [x] 8.2 Add `onRequestInspection: () -> Unit = {}` parameter
- [x] 8.3 Add `onViewServiceRequestTrail: () -> Unit = {}` parameter
- [x] 8.4 Add "Request Inspection" `Button` after the asset info card — shown only when `asset.nextInspectionDue != null && asset.nextInspectionDue < System.currentTimeMillis()`
- [x] 8.5 Add service request summary `Card` when `serviceRequests.isNotEmpty()`: shows count + most recent status + "View all →" `TextButton`
- [x] 8.6 In `MainActivity` `AssetFound` state: load service requests via `LaunchedEffect`, pass callbacks and list to `AssetDetailScreen`

## 9. Build Verification

- [x] 9.1 `./gradlew assembleDebug` passes — no errors
- [x] 9.2 On device: clear app storage (Room migration), verify app opens cleanly on v11
- [x] 9.3 On device: navigate to an overdue asset → "Request Inspection" button appears
- [x] 9.4 On device: fill in contractor info → "Send Request" → navigates back → service request summary appears on asset detail
- [x] 9.5 On device: "View all" → `ServiceRequestTrailScreen` shows the request; "Export PDF" → toast appears
- [ ] 9.6 NO_RESPONSE test: manually set `sentAtMs` to 8 days ago in DB Inspector → force-stop → reopen → record status flips to NO_RESPONSE → amber highlight in trail
