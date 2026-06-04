# Module 42 — Context File
> For: `/opsx:propose "module-42-service-request-trail"`
> Written: 2026-06-03

---

## What this module is

The service request trail — the liability transfer instrument.

When an asset goes overdue, TapLog V1 just flags it red. TapLog V2 does something more
powerful: the owner (or Robin, as field analyst) can send a formal, timestamped service
request to a contractor. If the contractor doesn't respond within 7 days, the record
automatically marks `NO_RESPONSE` — and at that moment, the owner's culpability for the
compliance gap transfers to the contractor who failed to show.

The image that defines this feature: a building owner standing in front of a TSSA
inspector, pointing at a compliance gap, saying: "I know about it. I flagged it 8 months
ago. I called my contractor. No one came. Here's the record. Go ask them."

That record is `ServiceRequest`. This module builds it.

---

## What already exists (from spec merge — NOT yet implemented in code)

The data model was designed in Session 15 and lives in `taplog-spec.md`:

```kotlin
@Entity(tableName = "service_requests")
data class ServiceRequest(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val assetId: String,
    val siteId: String,
    val requestedById: String,
    val requestedByRole: UserRole,
    val contractorName: String? = null,
    val contractorPhone: String? = null,
    val contractorEmail: String? = null,
    val notes: String? = null,
    val status: ServiceRequestStatus = ServiceRequestStatus.SENT,
    val sentAtMs: Long = System.currentTimeMillis(),
    val respondedAtMs: Long? = null,
    val resolvedAtMs: Long? = null,
    val isSynced: Boolean = false
)

enum class ServiceRequestStatus {
    SENT, ACKNOWLEDGED, SCHEDULED, COMPLETED, NO_RESPONSE
}
```

`ServiceRequestStatus` and `UserRole` enums are already in `EmberModels.kt` (Module 43
added `UserRole`). `ServiceRequestStatus` is defined in the spec but NOT yet in the
Kotlin code — needs to be added.

---

## Scope

**In scope:**
- Add `ServiceRequestStatus` enum to `EmberModels.kt`
- Add `ServiceRequest` entity to `EmberModels.kt`
- Room migration v10 → v11: create `service_requests` table
- `ServiceRequestDao` in `EmberDAO.kt`
- `EmberRepository` methods for service requests
- `EmberViewModel` methods: `sendServiceRequest()`, `markNoResponse()`,
  `getServiceRequestsForAsset()`
- `ServiceRequestScreen` — form for creating a new request (contractor name, phone,
  email, notes, urgency)
- `ServiceRequestTrailScreen` — full history for an asset; `NO_RESPONSE` entries
  highlighted; "Export as PDF" button (stub — PDF generation is Module 49)
- `AssetDetailScreen` modification — "Request Inspection" button shown when
  `asset.nextInspectionDue < now` (overdue); service request count + latest status chip
  shown when any requests exist for the asset; "View all" → `ServiceRequestTrailScreen`
- Background job: at app launch, scan all `ServiceRequest` records with
  `status == SENT` and `sentAtMs + 7 days < now` → set `status = NO_RESPONSE`
  (this is the liability transfer trigger)
- `ScanState.ServiceRequestForm(asset)` and `ScanState.ServiceRequestTrail(asset)` added
  to ScanState sealed class

**Out of scope:**
- Actually sending the email/SMS to the contractor (deferred — no email client integration
  yet; the record is created and the user can contact manually)
- Sync to backend (ServiceRequest records will sync in a future session when the backend
  endpoint is added)
- Farpost integration of `NO_RESPONSE` data into dispatch scoring (deferred)
- TenancyRecord, MoveInRecord, CheckInEvent (separate modules)

---

## Room migration v10 → v11

```sql
CREATE TABLE IF NOT EXISTS service_requests (
    id TEXT NOT NULL PRIMARY KEY,
    assetId TEXT NOT NULL,
    siteId TEXT NOT NULL,
    requestedById TEXT NOT NULL,
    requestedByRole TEXT NOT NULL,
    contractorName TEXT,
    contractorPhone TEXT,
    contractorEmail TEXT,
    notes TEXT,
    status TEXT NOT NULL DEFAULT 'SENT',
    sentAtMs INTEGER NOT NULL,
    respondedAtMs INTEGER,
    resolvedAtMs INTEGER,
    isSynced INTEGER NOT NULL DEFAULT 0
);
```

---

## Screen designs

### `ServiceRequestScreen`
Accessed from `AssetDetailScreen` → "Request Inspection" button (only shown when overdue).

Fields:
- Contractor name (text, optional)
- Contractor phone (text, optional)
- Contractor email (text, optional)
- Notes / what needs to be done (multiline, optional)
- Urgency selector (Routine / Urgent / Critical — stored in notes for now)

On "Send Request": creates `ServiceRequest` with `status = SENT`, `sentAtMs = now`,
`requestedById` from `InspectorPreferences.inspectorId`, `requestedByRole = INSPECTOR`
(or FIELD_ANALYST based on current role). Navigates back to `AssetDetailScreen`.

### `ServiceRequestTrailScreen`
Accessed from `AssetDetailScreen` → "View all service requests".

Shows: chronological list of all `ServiceRequest` records for this asset.
Each entry: date sent, contractor name (or "Unknown contractor"), status badge,
response date (or "No response" if status == NO_RESPONSE).
`NO_RESPONSE` entries: amber/red highlight, "Liability transferred" label.
"Export as PDF" button → stub ("PDF export coming soon" toast for now).

### `AssetDetailScreen` additions
- **"Request Inspection" button**: shown when `asset.nextInspectionDue < now`
  (asset is overdue). Navigates to `ServiceRequestScreen`.
- **Service request summary**: shown when any ServiceRequest records exist for the
  asset. Shows count + most recent status. "View all →" navigates to
  `ServiceRequestTrailScreen`.

---

## The NO_RESPONSE trigger

At app startup (in `TapLogApplication.onCreate()` or via a one-shot `WorkManager` job),
scan all `ServiceRequest` records with `status == SENT` and
`sentAtMs + (7 * 24 * 60 * 60 * 1000L) < System.currentTimeMillis()` → update to
`status = NO_RESPONSE`.

This is the liability transfer event. In the trail view, these are highlighted.

Simple implementation: call `repository.updateOverdueServiceRequests()` from
`TapLogApplication.scheduleSyncIfNeeded()` or a dedicated startup coroutine.

---

## Key design notes

- `ServiceRequest` is insert-mostly. Status transitions are ONE-WAY: SENT →
  ACKNOWLEDGED → SCHEDULED → COMPLETED, or SENT → NO_RESPONSE. No going back.
- The contractor contact info is stored on the request at time of sending — it reflects
  who was contacted, even if that contractor's details change later.
- `requestedById` uses `InspectorPreferences.inspectorId.first()` — the authenticated
  inspector's ID. For unauthenticated users (not yet built), default to "local".
- The "Export as PDF" in `ServiceRequestTrailScreen` is a stub in this module. The full
  PDF generation (WeasyPrint-style on Android using `PdfDocument`) comes in Module 49.

---

## What does NOT change

- No backend changes (ServiceRequest sync deferred)
- No TenancyRecord, MoveInRecord, CheckInEvent (separate modules)
- `InspectionFormScreen`, `FieldAnalystScanScreen`, `QuickRegisterSheet` — untouched
- Existing inspection and deficiency flows — untouched
