## Context

The service request trail completes TapLog V2's core value proposition. It's the difference between "the asset was overdue" (TapLog V1) and "the asset was overdue, the owner tried to fix it, nobody came, here's the date" (TapLog V2). The data model was designed in Session 15 and is in `taplog-spec.md`. This module implements it.

Current state: `ServiceRequestStatus` enum and `ServiceRequest` entity exist in the spec but not in code. `UserRole` (needed for `requestedByRole`) already exists in `EmberModels.kt` from Module 43. Room is at version 10.

## Goals / Non-Goals

**Goals:**
- `ServiceRequest` entity in Room with full CRUD
- "Request Inspection" button on overdue assets in `AssetDetailScreen`
- `ServiceRequestScreen` — create a request with contractor contact info
- `ServiceRequestTrailScreen` — full history per asset, NO_RESPONSE highlighted
- 7-day NO_RESPONSE auto-promotion on app startup
- Service request summary card on `AssetDetailScreen`

**Non-Goals:**
- Actually sending email/SMS to contractor — deferred; user contacts contractor manually
- Backend sync — `isSynced` field present, sync endpoint deferred
- Farpost integration of `NO_RESPONSE` into dispatch scoring — deferred
- TenancyRecord, MoveInRecord, CheckInEvent — separate modules
- PDF export — stub only ("coming soon" toast); full PDF is Module 49

## Decisions

### D1: NO_RESPONSE trigger on app startup, not a background worker
**Decision:** Scan for expired service requests in `TapLogApplication.onCreate()` via `appScope.launch`, not via WorkManager.
**Rationale:** The trigger is low-frequency (once per app launch) and lightweight (a single Room query + bulk update). WorkManager adds scheduling complexity for a job that runs fast and doesn't need exact timing. If the user doesn't open the app for 7+ days, the update happens the next time they do open it — which is the right behavior. The liability record only needs to exist before anyone looks at it.

### D2: Urgency stored as a string field in notes, not a separate column
**Decision:** The urgency selector (Routine/Urgent/Critical) prepends to the `notes` field as `[Urgent]` rather than adding a dedicated column.
**Rationale:** Urgency is display metadata — it doesn't affect any business logic in this module. Adding a column requires a wider migration and a TypeConverter. Embedding in notes avoids schema complexity for a field that may be redesigned before Module 48 (notifications) uses it properly. The trail screen can parse the prefix if needed.
**Alternative considered:** Separate `urgency TEXT` column — deferred to Module 48 when notification logic needs to act on it.

### D3: ServiceRequestTrailScreen is a separate full-screen, not an inline expansion
**Decision:** "View all service requests" opens `ServiceRequestTrailScreen` as a new full screen.
**Rationale:** The trail can be long (many requests over the asset's lifetime). An inline LazyColumn expansion inside `AssetDetailScreen` would require nested scrolling, which Compose handles awkwardly. Full-screen gives space for the NO_RESPONSE highlight, timestamps, and the future PDF export button.

### D4: `requestedByRole` uses the current `userRole` from DataStore
**Decision:** When creating a `ServiceRequest`, `requestedByRole` is populated from `inspectorPreferences.userRole.first()`.
**Rationale:** The same role that's driving the current session is the role that sent the request. An Inspector sending a request is an INSPECTOR; a Field Analyst doing so is a FIELD_ANALYST. The role context is already in DataStore from Module 43.

### D5: No optimistic NO_RESPONSE — only promote on app open
**Decision:** `NO_RESPONSE` is set when the app opens, not in a scheduled background task.
**Rationale:** Android background task scheduling (WorkManager exact timing) is unreliable on modern Android for battery-constrained exact timing. The 7-day window is approximate by design — the record reflects "this request went unanswered long enough that the owner's good faith is established," not an exact SLA. App-open trigger is deterministic and simple.

## Risks / Trade-offs

- **7-day timer is approximate** → If user opens app every day, timer is accurate. If they don't open for 2 weeks, it triggers correctly on next open. Acceptable for the use case.
- **No email/SMS** → Means the record exists but the contractor may never know. User must contact contractor manually. Mitigated by including contractor contact info fields that make the manual contact easy.
- **Nested scroll risk** → `AssetDetailScreen` already has a `LazyColumn`. The new service request summary card is a fixed-height item in that list — no nested scroll needed unless the user taps "View all" which opens a separate screen.

## Migration Plan

Room v10 → v11: one `CREATE TABLE service_requests` statement. No changes to existing tables. Safe on all devices. On crash: clear app storage (standard dev procedure).

## Open Questions

None — all decisions made above.
