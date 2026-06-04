## Why

TapLog V1 flags overdue assets red. TapLog V2 makes that flag mean something legally: when an owner sends a timestamped service request that goes unanswered for 7 days, the record automatically marks `NO_RESPONSE` — and at that moment, culpability for the compliance gap transfers from the owner to the contractor who failed to show. This is the single highest-value feature remaining: without it, TapLog documents the problem but doesn't protect the person who tried to fix it.

## What Changes

- **New enum** `ServiceRequestStatus` added to `EmberModels.kt` (SENT, ACKNOWLEDGED, SCHEDULED, COMPLETED, NO_RESPONSE)
- **New entity** `ServiceRequest` added to `EmberModels.kt`
- **Room migration v10 → v11**: `service_requests` table created
- **New `ServiceRequestDao`** in `EmberDAO.kt`
- **`EmberRepository`** gains service request CRUD methods
- **`EmberViewModel`** gains `sendServiceRequest()`, service requests StateFlow, NO_RESPONSE auto-update on startup
- **New `ServiceRequestScreen`** — form: contractor name/phone/email, notes, urgency — creates `ServiceRequest` with `status = SENT`
- **New `ServiceRequestTrailScreen`** — chronological history per asset; `NO_RESPONSE` entries highlighted amber; "Export PDF" stub
- **`AssetDetailScreen` modified** — "Request Inspection" button appears when asset is overdue; service request count + status chip; "View all →" link
- **`TapLogApplication`** gains startup scan: any `ServiceRequest` with `status = SENT` older than 7 days → `NO_RESPONSE`
- **`ScanState`** gains `ServiceRequestForm(asset)` and `ServiceRequestTrail(asset)`

## Capabilities

### New Capabilities

- `service-request-trail`: Formal service request creation, history trail per asset, 7-day NO_RESPONSE auto-promotion, liability transfer record

### Modified Capabilities

- `tag-lifecycle`: No change — AssetDetailScreen additions are additive only (new section below existing content)

## Impact

- **Android:** `EmberModels.kt`, `AppDatabase.kt`, `EmberDAO.kt`, `EmberRepository.kt`, `EmberViewModel.kt`, `TapLogApplication.kt`, `AssetDetailScreen.kt`, `MainActivity.kt` (2 new screen states), 2 new screen files
- **Room:** Version 10 → 11; one new table; no changes to existing tables
- **No backend changes** — sync deferred; `isSynced` field present for future
- **No breaking changes** to existing flows
