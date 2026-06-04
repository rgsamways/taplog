## ADDED Requirements

### Requirement: Service request can be created for any overdue asset
The system SHALL provide a "Request Inspection" button on `AssetDetailScreen` when `asset.nextInspectionDue < System.currentTimeMillis()`. Tapping it SHALL navigate to `ServiceRequestScreen`. The button SHALL NOT appear for assets that are current or have no due date.

#### Scenario: Asset is overdue — button shown
- **WHEN** the user views an asset where `nextInspectionDue` is in the past
- **THEN** a "Request Inspection" button appears on `AssetDetailScreen`

#### Scenario: Asset is current — button hidden
- **WHEN** the user views an asset where `nextInspectionDue` is in the future or null
- **THEN** no "Request Inspection" button is shown

### Requirement: ServiceRequest record created with status SENT on submission
The system SHALL create a `ServiceRequest` record with `status = SENT` and `sentAtMs = now` when the user submits `ServiceRequestScreen`. The record SHALL include the authenticated user's ID as `requestedById` and their current role as `requestedByRole`.

#### Scenario: Request submitted with contractor contact info
- **WHEN** the user fills in contractor name and phone and taps "Send Request"
- **THEN** a `ServiceRequest` is inserted into Room with `status = SENT`, `sentAtMs = now`, `contractorName` and `contractorPhone` set, and the user is navigated back to `AssetDetailScreen`

#### Scenario: Request submitted with no contractor info
- **WHEN** the user leaves all contractor fields blank and taps "Send Request"
- **THEN** a `ServiceRequest` is inserted with null contractor fields — the record documents the attempt even without contact details

### Requirement: Service requests older than 7 days with status SENT are promoted to NO_RESPONSE on app open
The system SHALL scan all `ServiceRequest` records with `status = SENT` on every app launch. Any record where `sentAtMs + 604800000` (7 days in ms) `< System.currentTimeMillis()` SHALL have its `status` updated to `NO_RESPONSE`. This is the liability transfer event.

#### Scenario: 8-day-old unanswered request on app open
- **WHEN** the app opens and a ServiceRequest has `status = SENT` and `sentAtMs` is 8 days ago
- **THEN** that record's `status` is updated to `NO_RESPONSE` before any screen renders

#### Scenario: 3-day-old request on app open
- **WHEN** the app opens and a ServiceRequest has `status = SENT` and `sentAtMs` is 3 days ago
- **THEN** that record's `status` remains `SENT` — 7 days have not elapsed

### Requirement: Service request trail displays full history per asset
`ServiceRequestTrailScreen` SHALL show all `ServiceRequest` records for a given asset in reverse chronological order. `NO_RESPONSE` entries SHALL be visually distinguished (amber/red highlight). Each entry SHALL show: date sent, contractor name (or "Unknown contractor"), status, and response date if applicable.

#### Scenario: Asset with mixed request history
- **WHEN** the user views the trail for an asset that has one COMPLETED request and one NO_RESPONSE request
- **THEN** the NO_RESPONSE entry is highlighted in amber; the COMPLETED entry is shown normally; both are listed with their sent dates

#### Scenario: Asset with no service requests
- **WHEN** the user views the trail for an asset that has no ServiceRequest records
- **THEN** an empty state is shown ("No service requests recorded")

### Requirement: AssetDetailScreen shows service request summary
When a `ServiceRequest` exists for an asset, `AssetDetailScreen` SHALL show a summary card with the count of requests and the most recent request's status. The card SHALL include a "View all" link that navigates to `ServiceRequestTrailScreen`.

#### Scenario: Asset has 2 requests, most recent is NO_RESPONSE
- **WHEN** the user views `AssetDetailScreen` for an asset with 2 service requests
- **THEN** a summary shows "2 requests · No response" with a "View all" link; tapping "View all" opens `ServiceRequestTrailScreen`

#### Scenario: Asset has no requests — summary hidden
- **WHEN** the user views `AssetDetailScreen` for an asset with zero service requests
- **THEN** no service request summary card is shown

### Requirement: PDF export in ServiceRequestTrailScreen is stubbed
The "Export as PDF" button in `ServiceRequestTrailScreen` SHALL be present but SHALL show a "PDF export coming soon" `Toast` when tapped. Full implementation is deferred to Module 49.

#### Scenario: Export button tapped
- **WHEN** the user taps "Export as PDF" on `ServiceRequestTrailScreen`
- **THEN** a `Toast` with "PDF export coming soon" appears; no file is generated
